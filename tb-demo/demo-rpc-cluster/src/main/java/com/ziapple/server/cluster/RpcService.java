package com.ziapple.server.cluster;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

public interface RpcService {
    // 初始化服务
    void init();

    // 发送集群消息
    void onSendMsg(ClusterAPIProtos.ClusterMessage msg);

    // 接收消息
    void onReceiveMsg(ClusterAPIProtos.ClusterMessage msg);
}
