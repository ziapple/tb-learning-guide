package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import com.ziapple.server.gen.cluster.ClusterRpcServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC集群服务，用于接受和发送RPC消息
 */
@Slf4j
public class GRpcService extends ClusterRpcServiceGrpc.ClusterRpcServiceImplBase implements RpcService{
    // RPC的Server对象
    private Server server;
    private ServerInstance currentServer;
    // RPC通讯会话Map
    private Map<ServerAddress, RpcSession> rpcSessions = new ConcurrentHashMap<>();

    public GRpcService(ServerInstance serverInstance){
        this.currentServer = serverInstance;
    }

    /**
     * 服务器端接受消息
     */
    public StreamObserver<ClusterAPIProtos.ClusterMessage> handleMsgs(
            StreamObserver<ClusterAPIProtos.ClusterMessage> responseObserver) {
        return new StreamObserver<ClusterAPIProtos.ClusterMessage>() {
            /**
             * 接受客户端消息，交给本地处理
             * @param msg
             */
            public void onNext(ClusterAPIProtos.ClusterMessage msg) {
                System.out.println("服务器接受到一个消息" + msg.getPayload());
            }

            public void onError(Throwable throwable) {

            }

            /**
             * 与客户端断开连接
             */
            public void onCompleted() {
                // 通知客户端
                responseObserver.onNext(ClusterAPIProtos.ClusterMessage.newBuilder().build());
                // 通知客户端断开
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * 启动本地群服务
     */
    @Override
    public void start() {
        server = ServerBuilder.forPort(this.currentServer.getPort()).addService(this).build();
        log.info("启动本地服务:{}", this.currentServer.getPort());
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start RPC server!");
        }
    }

    /**
     * 给集群发送消息
     * @param serverAddress
     * @param clusterMessage
     */
    @Override
    public void onSendMsg(ServerAddress serverAddress, ClusterAPIProtos.ClusterMessage clusterMessage) {
        // 判断与服务器是否建立Session会话，没有则创建
        checkSession(serverAddress);
        RpcSession rpcSession = rpcSessions.get(serverAddress);
        rpcSession.tell(clusterMessage);
    }

    public void checkSession(ServerAddress serverAddress){
        RpcSession rpcSession = rpcSessions.get(serverAddress);
        if(rpcSession == null){
            rpcSession = new RpcSessionImpl(UUID.randomUUID(), serverAddress);
            rpcSession.initSession();
            rpcSessions.put(serverAddress, rpcSession);
        }else{
            if(!rpcSession.checkSession()){// 如果连接断开，重新建立连接
                rpcSession.initSession();
            }
        }
    }

}
