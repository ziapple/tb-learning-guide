package com.ziapple.server.cluster;

import java.util.Optional;

/**
 * 模拟Cluster路由服务
 */
public interface RoutingServer {
    ServerInstance getCurrentServer();

    Optional<ServerAddress> resolveById(Integer entityId, int len);
}
