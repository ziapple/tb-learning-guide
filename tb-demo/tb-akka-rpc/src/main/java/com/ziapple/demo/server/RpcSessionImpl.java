package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import com.ziapple.server.gen.cluster.ClusterRpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

/**
 * RPC服务器会话
 */
public class RpcSessionImpl implements RpcSession{
    private boolean isSessionOpen = false;
    private ClusterAPIProtos.ServerAddress serverAddress;
    private ClusterRpcServiceGrpc.ClusterRpcServiceStub clusterRpcServiceStub;

    RpcSessionImpl(ClusterAPIProtos.ServerAddress serverAddress){
        this.serverAddress = serverAddress;
    }

    // 发送消息
    public void tell(ClusterAPIProtos.ClusterMessage clusterMessage){
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

        // 给远端RPC发送消息
        StreamObserver<ClusterAPIProtos.ClusterMessage> requestObserver = clusterRpcServiceStub.handleMsgs(responseObserver);
        requestObserver.onNext(clusterMessage);
    }

    // 建立会话
    public void initSession(){
        // 初始化channel
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(serverAddress.getHost(),serverAddress.getPort()).usePlaintext().build();
        clusterRpcServiceStub = ClusterRpcServiceGrpc.newStub(managedChannel);

        isSessionOpen = true;
    }

    // 关闭会话
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
