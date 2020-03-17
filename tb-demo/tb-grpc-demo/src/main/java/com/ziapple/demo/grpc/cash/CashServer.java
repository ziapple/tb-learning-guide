package com.ziapple.demo.grpc.cash;

import com.ziapple.demo.grpc.CashReply;
import com.ziapple.demo.grpc.CashRequest;
import com.ziapple.demo.grpc.CashServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class CashServer extends CashServiceGrpc.CashServiceImplBase {
    private int port = 50051;
    private Server server;

    public StreamObserver<CashRequest> dealCash(StreamObserver<CashReply> responseObserver) {
        return new StreamObserver<CashRequest>() {
            // 接受客户端消息
            @Override
            public void onNext(CashRequest cashRequest) {
                System.out.println("收到客户端消息：user->" + cashRequest.getUser() + ", money" + cashRequest.getMoney());
                CashReply reply = CashReply.newBuilder().setStatus("1").build();
                responseObserver.onNext(reply);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                // 告诉客户端关闭连接
                responseObserver.onCompleted();
                System.out.println("服务器断开oncompelete");
            }
        };
    }

    private void start() throws IOException {
        System.out.println("start rpc server:" + this.port);
        server = ServerBuilder.forPort(port)
                .addService(this)
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(){

            @Override
            public void run(){
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                CashServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop(){
        if (server != null){
            server.shutdown();
        }
    }

    // block 一直到退出程序
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null){
            server.awaitTermination();
        }
    }


    public  static  void main(String[] args) throws IOException, InterruptedException {
        final CashServer server = new CashServer();
        server.start();
        server.blockUntilShutdown();
    }
}
