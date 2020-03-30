package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

public interface RpcSessionListener {
    void onReceiveMsg(ClusterAPIProtos.ClusterMessage clusterMessage);
}
