package com.ziapple.test;

import com.ziapple.server.data.id.DeviceId;
import com.ziapple.server.transport.LocalTransportService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class MqttMsgTest extends AbstractAppTest {
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Autowired
    private LocalTransportService transportService;

    @Test
    public void testMqttSendPubMsg(){
        //模拟设备发送数据,每一秒发送数据
        new Timer("SendMsgTimer").schedule(new TimerTask() {
            @Override
            public void run() {
                Random random = new Random();
                String msg = "发送消息:" + random.nextInt();
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
