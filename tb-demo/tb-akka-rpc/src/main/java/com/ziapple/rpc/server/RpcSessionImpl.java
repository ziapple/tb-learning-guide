package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import com.ziapple.server.gen.cluster.ClusterRpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RPC服务器会话
 */
public class RpcSessionImpl implements RpcSession{
    private boolean isSessionOpen = false;
    private ServerAddress serverAddress;
    private ClusterRpcServiceGrpc.ClusterRpcServiceStub clusterRpcServiceStub;
    private StreamObserver<ClusterAPIProtos.ClusterMessage> requestObserver;
    private StreamObserver<ClusterAPIProtos.ClusterMessage> responseObserver;
    private UUID sessionId;

    RpcSessionImpl(UUID sessionId, ServerAddress serverAddress){
        this.serverAddress = serverAddress;
        this.sessionId = sessionId;
    }

    // 发送消息
    public void tell(ClusterAPIProtos.ClusterMessage clusterMessage){
        requestObserver.onNext(clusterMessage);
    }

    // 建立会话
    @Override
    public void initSession(){
        // 初始化channel
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(serverAddress.getHost(),serverAddress.getPort()).usePlaintext().build();
        clusterRpcServiceStub = ClusterRpcServiceGrpc.newStub(managedChannel);
        initResponseObserver();
        initRequestObserver();
        isSessionOpen = true;
    }

    public void initRequestObserver(){
        this.requestObserver = clusterRpcServiceStub.handleMsgs(this.responseObserver);
    }

    public void initResponseObserver(){
        StreamObserver<ClusterAPIProtos.ClusterMessage> responseObserver = new StreamObserver<ClusterAPIProtos.ClusterMessage>() {
            @Override
            public void onNext(ClusterAPIProtos.ClusterMessage clusterMessage) {
                System.out.println("收到消息" + clusterMessage);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        };

        this.responseObserver = responseObserver;
    }

    // 关闭会话
    @Override
    public void closeSession(){
        isSessionOpen = false;
        ManagedChannel managedChannel = (ManagedChannel)clusterRpcServiceStub.getChannel();
        try {
            managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 检查会话是否存在
    public boolean checkSession(){
        return isSessionOpen;
    }
}
