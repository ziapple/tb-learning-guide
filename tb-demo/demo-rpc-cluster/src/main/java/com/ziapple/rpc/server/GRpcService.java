package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import com.ziapple.server.gen.cluster.ClusterRpcServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC集群服务，用于接受和发送RPC消息
 */
@Slf4j
public class GRpcService extends ClusterRpcServiceGrpc.ClusterRpcServiceImplBase implements RpcService, RpcSessionListener{
    // RPC的Server对象
    private Server server;
    // 本地服务实例
    @Getter
    private ServerInstance currentServer;
    // RPC通讯会话Map
    @Getter
    private Map<ServerAddress, RpcSession> rpcSessions = new ConcurrentHashMap<>();
    // Rpc服务发现着
    @Getter
    private DiscoveryService discoveryService;
    // Rpc路由者
    @Getter
    private RoutingServer routingServer;
    // SessionListener
    private RpcSessionListener listener;

    public void init(){
        // 注册当前服务器
        this.discoveryService = new DummyDiscoveryService();
        this.discoveryService.init();
        this.currentServer = discoveryService.getCurrentServer();
        this.routingServer = new DummyRoutingServer(discoveryService);
        this.listener = new GRpcSessionLitener(this);

        // 启动RPC服务
        this.start();
        initClusterSessions();
    }

    // 服务端收到消息
    public StreamObserver<ClusterAPIProtos.ClusterMessage> handleMsgs(StreamObserver<ClusterAPIProtos.ClusterMessage> responseObserver) {
        return new StreamObserver<ClusterAPIProtos.ClusterMessage>() {
            public void onNext(ClusterAPIProtos.ClusterMessage msg) {
                GRpcService.this.onReceiveMsg(msg);
            }
            public void onError(Throwable throwable) {
            }
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    public void start() {
        server = ServerBuilder.forPort(this.currentServer.getPort()).addService(this).build();
        log.info("启动本地服务:{}", this.currentServer.getPort());
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start RPC server!");
        }
    }

    @Override
    public void onSendMsg(ClusterAPIProtos.ClusterMessage msg) {
        if(msg.hasServerAddress()){
            RpcSession session = rpcSessions.get(new ServerAddress(msg.getServerAddress().getHost(), msg.getServerAddress().getPort()));
            session.tell(msg);
        }else{
            listener.onReceiveMsg(msg);
        }
    }

    @Override
    public void onReceiveMsg(ClusterAPIProtos.ClusterMessage msg) {
        listener.onReceiveMsg(msg);
    }

    // 初始化本地服务器和其他服务器Session
    public void initClusterSessions(){
        // 初始化Session
        discoveryService.getOtherServers().stream()
                .filter(otherServer -> otherServer.getServerAddress().compareTo(currentServer.getServerAddress()) != 0)
                .forEach(otherServer -> createSession(UUID.randomUUID(), otherServer.getServerAddress(), true));
    }

    // 建立本地服务器和其他服务器的Session通道
    public void createSession(UUID sessionId, ServerAddress remoteServerAddress, boolean isClient){
        if(rpcSessions.get(remoteServerAddress) == null) {
            GRpcSession rpcSession = new GRpcSession(sessionId, remoteServerAddress);
            log.debug("初始化{}到{}的Session[{}]", discoveryService.getCurrentServer().getServerAddress(), remoteServerAddress, sessionId);
            rpcSession.initSession();
            rpcSessions.put(remoteServerAddress, rpcSession);
            // 发消息告诉服务器，建立双向Session
            ClusterAPIProtos.ClusterMessage msg = ClusterAPIProtos.ClusterMessage.newBuilder()
                    .setMessageType(ClusterAPIProtos.MessageType.CLUSTER_SESSION_REQUEST)
                    .setServerAddress(ClusterAPIProtos.ServerAddress.newBuilder().setHost(currentServer.getHost()).setPort(currentServer.getPort()))
                    .build();
            if(isClient)// Server
                rpcSession.tell(msg);
        }
    }
}
