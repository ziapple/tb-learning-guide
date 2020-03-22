package com.ziapple.demo.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.calculate.CalculateProto;
import io.grpc.examples.calculate.CalculateServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;

public class CaculatorClient1 {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8088;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        CaculatorClient1 client = new CaculatorClient1();
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
            CalculateServiceGrpc.CalculateServiceStub stub = CalculateServiceGrpc.newStub(managedChannel);
            // 声明一个异步响应服务的客户端回调对象
            StreamObserver<CalculateProto.Value> requestStub = stub.getResult(new StreamObserver<CalculateProto.Result>() {
                @Override
                public void onNext(CalculateProto.Result result) {
                    System.out.println("收到服务响应：" + result.getSum());
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
            requestStub.onNext(CalculateProto.Value.newBuilder().setValue(1).build());
            System.out.println("发送给服务器消息user:leilei,money:2");
            requestStub.onNext(CalculateProto.Value.newBuilder().setValue(2).build());
            // onNext是异步的，通知服务端断开连接，有可能onComplete执行比onNext早
            // cash.onCompleted();
        }
    }
}
