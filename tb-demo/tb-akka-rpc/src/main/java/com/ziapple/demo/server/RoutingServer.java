package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

/**
 * 模拟Cluster路由服务
 */
public interface RoutingServer {
    // 根据业务Id获取对应处理的服务器
    public ClusterAPIProtos.ServerAddress getRPCServer(int entityId);

    // 注册服务
    public void regist(ClusterAPIProtos.ServerAddress serverAddress);
}
