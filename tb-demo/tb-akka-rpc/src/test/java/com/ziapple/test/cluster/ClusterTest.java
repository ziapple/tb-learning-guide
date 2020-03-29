package com.ziapple.test.cluster;

import com.ziapple.rpc.server.ClusterService;
import com.ziapple.rpc.server.DummyDiscoveryService;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ClusterTest {
    private AtomicInteger totalMsg = new AtomicInteger(0);
    private ClusterService clusterService1;
    private ClusterService clusterService2;

    public void init(){
        //清空Cluster
        DummyDiscoveryService.clear();
        clusterService1 = new ClusterService();
        clusterService1.init();

        clusterService2 = new ClusterService();
        clusterService2.init();

        clusterService1.initSession();
        clusterService2.initSession();

        watch();
    }

    public static void main(String[] args) {
        ClusterTest test = new ClusterTest();
        test.init();
    }

    public void watch(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                clusterService1.tell(new Random().nextInt(1000));
                log.info("已发送消息{}", totalMsg.getAndIncrement());
            }
        }, 1000, 1000);
    }
}
