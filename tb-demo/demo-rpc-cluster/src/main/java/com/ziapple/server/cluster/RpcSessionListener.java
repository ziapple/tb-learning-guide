package com.ziapple.server.cluster;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

public interface RpcSessionListener {
    void onReceiveMsg(ClusterAPIProtos.ClusterMessage clusterMessage);
}
