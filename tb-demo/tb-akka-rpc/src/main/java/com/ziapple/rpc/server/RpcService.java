package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

public interface RpcService {
    /**
     * 启动本地RPC服务
     */
    void start();

    /**
     * 发送集群消息
     * @param clusterMessage
     */
    void onSendMsg(ServerAddress serverAddress, ClusterAPIProtos.ClusterMessage clusterMessage);
}
