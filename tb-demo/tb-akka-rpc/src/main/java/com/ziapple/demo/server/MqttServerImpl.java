package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模拟MqttServer发送给Cluster集群消息
 */
public class MqttServerImpl implements MqttServer{
    private Logger logger = LoggerFactory.getLogger(MqttServerImpl.class);

    private RoutingServer routingServer;
    private RpcService rpcService;

    public MqttServerImpl(RoutingServer routingServer){
        this.routingServer = routingServer;
        this.rpcService = new GRpcService();
    }

    /**
     * 将接受到的消息发送给Cluster处理
     * @param mqttMsg
     */
    public void process(int entityId, String mqttMsg){
        ClusterAPIProtos.ServerAddress serverAddress = routingServer.getRPCServer(entityId);
        ClusterAPIProtos.ClusterMessage clusterMsg = ClusterAPIProtos.ClusterMessage.newBuilder()
                .setMessageType(ClusterAPIProtos.MessageType.CLUSTER_TRANSFER_MESSAGE)
                .setPayload(mqttMsg).build();
        if(serverAddress.getHost().equals("localhost")){//交给本地处理
            logger.info("本地Service调用");
        }else{// 远程RPC调用
            logger.info("远程Service调用");
            rpcService.onSendMsg(serverAddress, clusterMsg);
        }
    }
}
