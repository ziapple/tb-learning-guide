package com.ziapple.rpc.server;

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
    private DiscoveryService discoveryService;
    private MqttServer mqttServer;

    public void init(){
        this.discoveryService = new DummyDiscoveryService();
        discoveryService.init();
        this.routingServer = new RoutingServerImpl(discoveryService);
        // 启动RPCServer集群，模拟两台RPC服务器
        RpcService gRpcService1 = new GRpcService(routingServer.getCurrentServer());
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
