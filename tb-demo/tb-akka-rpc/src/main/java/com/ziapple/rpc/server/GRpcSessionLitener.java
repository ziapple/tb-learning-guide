package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class GRpcSessionLitener implements RpcSessionListener{
    private GRpcService rpcService;

    public GRpcSessionLitener(GRpcService rpcService){
        this.rpcService = rpcService;
    }

    @Override
    public void onReceiveMsg(ClusterAPIProtos.ClusterMessage clusterMessage) {
        switch (clusterMessage.getMessageType()){
            case CLUSTER_TRANSFER_MESSAGE:
                log.debug("接收到消息{}", clusterMessage.getPayload());
                break;
            case CLUSTER_SESSION_REQUEST:
                regist(clusterMessage);
                break;
        }
    }

    public void regist(ClusterAPIProtos.ClusterMessage msg){
        log.debug("反向注册Session从{}到服务器{}", rpcService.getCurrentServer().getServerAddress(), msg.getServerAddress());
        rpcService.createSession(UUID.randomUUID(),
                new ServerAddress(msg.getServerAddress().getHost(), msg.getServerAddress().getPort()), false);
    }
}
