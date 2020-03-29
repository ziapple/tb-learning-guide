package com.ziapple.demo.grpc.cash;

import com.ziapple.demo.grpc.CashProto;
import com.ziapple.demo.grpc.CashServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;

public class CashClient{
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 50051;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        CashClient client = new CashClient();
        client.start();
    }

    public void start() throws InterruptedException {
        new RPCListener().run();
        // 等待子线程执行完
        countDownLatch.await();
    }

    public class RPCListener implements Runnable{
        @Override
        public void run() {
            ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(DEFAULT_HOST, DEFAULT_PORT).usePlaintext().build();
            CashServiceGrpc.CashServiceStub stub = CashServiceGrpc.newStub(managedChannel);
            // 声明一个异步响应服务CashReply的客户端请求对象CashRequest
            StreamObserver<CashProto.CashRequest> cash = stub.dealCash(new StreamObserver<CashProto.CashReply>() {
                @Override
                public void onNext(CashProto.CashReply cashReply) {
                    System.out.println("收到服务响应：" + cashReply.getStatus());
                }

                @Override
                public void onError(Throwable throwable) {
                    countDownLatch.countDown();
                    throwable.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    countDownLatch.countDown();
                    System.out.println("与服务器连接断开");
                }
            });

            // 发送消息
            System.out.println("发送给服务器消息user:ziapple,money:1");
            cash.onNext(CashProto.CashRequest.newBuilder().setUser("ziapple").setMoney(1).build());
            System.out.println("发送给服务器消息user:leilei,money:2");
            cash.onNext(CashProto.CashRequest.newBuilder().setUser("leilei").setMoney(2).build());
            // onNext是异步的，通知服务端断开连接，有可能onComplete执行比onNext早
            // cash.onCompleted();
        }
    }
}
