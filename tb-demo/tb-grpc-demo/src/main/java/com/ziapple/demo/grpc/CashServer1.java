package com.ziapple.demo.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @Author zsunny
 * @Date 2018/7/15 16:28
 * @Mail zsunny@yeah.net
 */
public class CashServer1 {

    private static final Logger logger = Logger.getLogger(CashServer1.class.getName());

    private static final int DEFAULT_PORT = 50051;

    private int port;//服务端口号

    private Server server;

    public CashServer1(int port) {
        this(port, ServerBuilder.forPort(port));
    }

    public CashServer1(int port, ServerBuilder<?> serverBuilder) {
        this.port = port;

        //构造服务器，添加我们实际的服务
        server = serverBuilder.addService(new CashServiceImpl()).build();
    }

    private void start() throws IOException {
        server.start();
        logger.info("Server has started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {

                CashServer1.this.stop();

            }
        });

    }

    private void stop() {

        if(server != null)
            server.shutdown();

    }

    //阻塞到应用停止
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) {

        CashServer1 addtionServer;

        if(args.length > 0){
            addtionServer = new CashServer1(Integer.parseInt(args[0]));
        }else{
            addtionServer = new CashServer1(DEFAULT_PORT);
        }

        try {
            addtionServer.start();
            addtionServer.blockUntilShutdown();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}