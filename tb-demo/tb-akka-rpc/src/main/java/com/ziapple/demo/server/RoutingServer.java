package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;

/**
 * 模拟Cluster路由服务
 */
public interface RoutingServer {
    /**
     * 根据业务Id获取对应处理的服务器
     * @param entityId
     * @return
     */
    ClusterAPIProtos.ServerAddress getRoutingServer(String entityId);
}
