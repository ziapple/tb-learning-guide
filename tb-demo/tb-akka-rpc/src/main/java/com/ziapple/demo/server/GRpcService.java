package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import com.ziapple.server.gen.cluster.ClusterRpcServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC集群服务，用于接受和发送RPC消息
 */
public class GRpcService extends ClusterRpcServiceGrpc.ClusterRpcServiceImplBase implements RpcService{
    // RPC的Server对象
    private Server server;
    // RPC通讯会话Map
    private Map<ClusterAPIProtos.ServerAddress, RpcSession> rpcSessions = new ConcurrentHashMap<>();

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
     * @param port
     */
    @Override
    public void start(int port) {
        server = ServerBuilder.forPort(port).addService(this).build();
        System.out.println("启动本地服务:" + port);
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
    public void onSendMsg(ClusterAPIProtos.ServerAddress serverAddress, ClusterAPIProtos.ClusterMessage clusterMessage) {
        // 判断与服务器是否建立Session会话，没有则创建
        checkSession(serverAddress);
        RpcSession rpcSession = rpcSessions.get(serverAddress);
        rpcSession.tell(clusterMessage);
    }

    public void checkSession(ClusterAPIProtos.ServerAddress serverAddress){
        RpcSession rpcSession = rpcSessions.get(serverAddress);
        if(rpcSession == null){
            rpcSession = new RpcSessionImpl(serverAddress);
            rpcSession.initSession();
            rpcSessions.put(serverAddress, rpcSession);
        }else{
            if(!rpcSession.checkSession()){// 如果连接断开，重新建立连接
                rpcSession.initSession();
            }
        }
    }

}
