package com.ziapple.test.cluster;

import com.ziapple.rpc.server.*;
import com.ziapple.server.cluster.DummyDiscoveryService;
import com.ziapple.server.cluster.GRpcService;
import com.ziapple.server.cluster.RpcService;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ClusterTest {
    private AtomicInteger totalMsg = new AtomicInteger(0);
    private RpcService rpcService1;

    public void init(){
        //清空Cluster
        DummyDiscoveryService.clear();
        rpcService1 = new GRpcService();
        rpcService1.init();

        for(int i=0; i<10; i++) {// 初始化10个集群服务器
            RpcService rpcService = new GRpcService();
            rpcService.init();
        }
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
                int i = totalMsg.getAndIncrement();
                log.info("生成消息{}", i);
                ClusterTest.this.tell(i);
            }
        }, 1000, 1000);
    }

    public void tell(int entityId){
        GRpcService gRpcService = (GRpcService)rpcService1;
        Optional<ServerAddress> result = gRpcService.getRoutingServer().resolveById(entityId, 11);
        ClusterAPIProtos.ClusterMessage clusterMessage = ClusterAPIProtos.ClusterMessage.newBuilder()
                .setMessageType(ClusterAPIProtos.MessageType.CLUSTER_TRANSFER_MESSAGE)
                .setPayload("[" + entityId + "]")
                .build();
        if(result.isPresent()){
            log.debug("路由[远程]{}", result.get());
            clusterMessage = ClusterAPIProtos.ClusterMessage.newBuilder()
                    .setMessageType(ClusterAPIProtos.MessageType.CLUSTER_TRANSFER_MESSAGE)
                    .setServerAddress(ClusterAPIProtos.ServerAddress.newBuilder()
                            .setHost(result.get().getHost())
                            .setPort(result.get().getPort()).build())
                    .setPayload("[" + entityId + "]")
                    .build();
            gRpcService.onSendMsg(clusterMessage);
        }else{
            log.debug("路由[本地]...");
            gRpcService.onReceiveMsg(clusterMessage);
        }
    }
}
