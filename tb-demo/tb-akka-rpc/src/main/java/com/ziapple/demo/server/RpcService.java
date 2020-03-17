package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

/**
 * RPC通信服务
 */
public interface RpcService {
    /**
     * 启动本地RPC服务
     */
    void start(int port);

    /**
     * 发送集群消息
     * @param clusterMessage
     */
    void onSendMsg(ClusterAPIProtos.ServerAddress serverAddress, ClusterAPIProtos.ClusterMessage clusterMessage);
}
