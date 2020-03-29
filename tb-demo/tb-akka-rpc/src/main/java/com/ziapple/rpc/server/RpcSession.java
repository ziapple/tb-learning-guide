package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

/**
 * RPC服务器会话
 */
public interface RpcSession {
    // 发送消息
    public void tell(ClusterAPIProtos.ClusterMessage clusterMessage);

    // 建立会话
    public void initSession();

    // 关闭会话
    public void closeSession();

    // 检查会话是否存在
    public boolean checkSession();
}
