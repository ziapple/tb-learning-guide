package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

/**
 * RPC服务器会话
 */
public interface RpcSession {
    void checkSession(ClusterAPIProtos.ServerAddress serverAddress);


}
