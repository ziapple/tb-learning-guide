package com.ziapple.rpc.server;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Cluster路由服务
 */
@Slf4j
public class RoutingServerImpl implements RoutingServer{
    private DiscoveryService discoveryService;
    private ServerInstance currentServer;

    public RoutingServerImpl(DiscoveryService discoveryService){
        this.discoveryService = discoveryService;
        this.currentServer = discoveryService.getCurrentServer();
    }

    @Override
    public ServerInstance getCurrentServer() {
        return currentServer;
    }

    @Override
    public Optional<ServerAddress> resolveById(Integer entityId) {
        List<ServerInstance> otherServers = discoveryService.getOtherServers();
        ServerInstance result = otherServers.get(entityId % 2);
        if (!currentServer.equals(result)) {
            return Optional.of(result.getServerAddress());
        } else {
            return Optional.empty();
        }
    }
}
