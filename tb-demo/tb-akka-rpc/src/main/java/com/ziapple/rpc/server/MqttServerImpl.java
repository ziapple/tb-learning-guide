package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 模拟MqttServer发送给Cluster集群消息
 */
public class MqttServerImpl implements MqttServer{
    private Logger logger = LoggerFactory.getLogger(MqttServerImpl.class);
    private DiscoveryService discoveryService;
    private RoutingServer routingServer;
    private RpcService rpcService;

    public MqttServerImpl(RoutingServer routingServer){
        this.routingServer = routingServer;
        this.rpcService = new GRpcService(routingServer.getCurrentServer());
    }

    /**
     * 将接受到的消息发送给Cluster处理
     * @param mqttMsg
     */
    public void process(int entityId, String mqttMsg){
        Optional<ServerAddress> serverAddress = routingServer.resolveById(entityId);
        ClusterAPIProtos.ClusterMessage clusterMsg = ClusterAPIProtos.ClusterMessage.newBuilder()
                .setMessageType(ClusterAPIProtos.MessageType.CLUSTER_TRANSFER_MESSAGE)
                .setPayload(mqttMsg).build();
        if(serverAddress.isPresent()) {
            logger.info("远程Service调用");
            rpcService.onSendMsg(serverAddress.get(), clusterMsg);
        }else{
            logger.info("本地Service调用");
        }
    }
}
