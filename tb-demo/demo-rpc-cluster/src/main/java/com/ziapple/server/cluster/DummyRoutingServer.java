package com.ziapple.server.cluster;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Cluster路由服务
 */
@Slf4j
public class DummyRoutingServer implements RoutingServer{
    private DiscoveryService discoveryService;
    private ServerInstance currentServer;

    public DummyRoutingServer(DiscoveryService discoveryService){
        this.discoveryService = discoveryService;
        this.currentServer = discoveryService.getCurrentServer();
    }

    @Override
    public ServerInstance getCurrentServer() {
        return currentServer;
    }

    @Override
    public Optional<ServerAddress> resolveById(Integer entityId, int len) {
        List<ServerInstance> otherServers = discoveryService.getOtherServers();
        ServerInstance result = otherServers.get(entityId % len);
        if (!currentServer.equals(result)) {
            return Optional.of(result.getServerAddress());
        } else {
            return Optional.empty();
        }
    }
}
