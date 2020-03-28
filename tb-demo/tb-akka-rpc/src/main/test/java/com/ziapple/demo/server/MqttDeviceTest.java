package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 模拟发送设备
 */
public class MqttDeviceTest {
    private static int MSG_NUM = 0;
    private static Logger logger = LoggerFactory.getLogger(MqttDeviceTest.class);
    private RoutingServer routingServer;
    private MqttServer mqttServer;

    public void init(){
        this.routingServer = new RoutingServerImpl();
        // 启动RPCServer集群，模拟两台RPC服务器
        RpcService gRpcService1 = new GRpcService();
        gRpcService1.start(8080);
        routingServer.regist(new ClusterAPIProtos.ServerAddress().newBuilder().setHost("localhost").setPort(8080).build());
        RpcService gRpcService2 = new GRpcService();
        gRpcService2.start(8081);
        routingServer.regist(new ClusterAPIProtos.ServerAddress().newBuilder().setHost("127.0.0.1").setPort(8081).build());

        this.mqttServer = new MqttServerImpl(routingServer);
    }

    /**
     * 模拟设备发送数据,每一秒发送数据
     */
    public void sendMsg(){
        new Timer("SendMsgTimer").schedule(new TimerTask() {
            @Override
            public void run() {
                Random random = new Random();
                String msg = "发送消息:" + random.nextInt();
                logger.info("消息{}, {}", MSG_NUM, msg);
                MSG_NUM++;
                mqttServer.process(MSG_NUM, msg);
            }
        }, 1000, 1000);
    }

    public static void main(String[] args) {
        MqttDeviceTest mqttDeviceTest = new MqttDeviceTest();
        mqttDeviceTest.init();
        mqttDeviceTest.sendMsg();
    }
}
