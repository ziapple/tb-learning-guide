package com.ziapple.test;

import com.ziapple.server.cluster.DummyDiscoveryService;
import com.ziapple.server.data.id.DeviceId;
import com.ziapple.server.transport.LocalTransportService;
import com.ziapple.server.util.SpringApplicationHolder;
import lombok.extern.slf4j.Slf4j;
import org.omg.CosNaming.BindingIterator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class AkkaClusterServerTest {
    private LocalTransportService transportService;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        AkkaClusterServerTest akkaClusterServerTest = new AkkaClusterServerTest();
        akkaClusterServerTest.init(args);
        akkaClusterServerTest.sendMsg();
    }

    /**
     * 初始化集群
     */
    public void init(String[] args){
        DummyDiscoveryService.clear();

        // 启动Server1
        new SpringApplicationBuilder(AkkaClusterServer1.class)
                .web(WebApplicationType.NONE)
                .run(AkkaClusterServer1.updateArguments(args));

        // 启动Server2
        new SpringApplicationBuilder(AkkaClusterServer2.class)
                .web(WebApplicationType.NONE)
                .run(AkkaClusterServer2.updateArguments(args));

        transportService = SpringApplicationHolder.getBean(LocalTransportService.class);
    }

    public void sendMsg(){
        //模拟设备发送数据,每一秒发送数据
        new Timer("SendMsgTimer").schedule(new TimerTask() {
            @Override
            public void run() {
                Random random = new Random();
                String msg = "消息:" + random.nextInt();
                DeviceId deviceId = new DeviceId(UUID.randomUUID());
                transportService.doProcess(deviceId, msg);
            }
        }, 500, 1000);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
