package com.ziapple.demo.grpc;

import com.ziapple.demo.grpc.helloworld.GreeterGrpc;
import com.ziapple.demo.grpc.helloworld.HelloReply;
import com.ziapple.demo.grpc.helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloWorldClient {

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    public HelloWorldClient(String host,int port){ 
        channel = ManagedChannelBuilder.forAddress(host,port)
                .usePlaintext(true) 
                .build();

        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }


    public void shutdown() throws InterruptedException { 
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public  void greet(String name){ 
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try{ 
            response = blockingStub.sayHello(request); 
        } catch (StatusRuntimeException e)
        { 
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return; 
        } 
        logger.info("Greeting: "+response.getMessage()); 
    }

    public static void main(String[] args) throws InterruptedException { 
        HelloWorldClient client = new HelloWorldClient("127.0.0.1",50052);
        try{ 
            String user = "world"; 
            if (args.length > 0){ 
                user = args[0]; 
            } 
            client.greet(user); 
        }finally { 
            client.shutdown(); 
        } 
    } 
}