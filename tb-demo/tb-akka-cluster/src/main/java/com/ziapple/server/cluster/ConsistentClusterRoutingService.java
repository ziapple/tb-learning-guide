/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ziapple.server.cluster;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.ziapple.server.data.id.EntityId;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Cluster service implementation based on consistent hash ring
 * 一致性hash环的具体实现
 * 继承了{@link DiscoveryServiceListener}，具备动态添加和删除Server的功能
 */

@Service
@Slf4j
public class ConsistentClusterRoutingService implements ClusterRoutingService {

    @Autowired
    @Setter
    private DiscoveryService discoveryService;

    @Value("${cluster.hash_function_name}")
    @Setter
    private String hashFunctionName;
    @Value("${cluster.vitrual_nodes_size}")
    @Setter
    private Integer virtualNodesSize;

    private ServerInstance currentServer;

    private HashFunction hashFunction;

    private ConsistentHashCircle[] circles;
    private ConsistentHashCircle rootCircle;

    @PostConstruct
    public void init() {
        log.info("Initializing Cluster routing service!");
        this.hashFunction = MiscUtils.forName(hashFunctionName);
        this.currentServer = discoveryService.getCurrentServer();
        this.circles = new ConsistentHashCircle[ServerType.values().length];
        for (ServerType serverType : ServerType.values()) {
            circles[serverType.ordinal()] = new ConsistentHashCircle();
        }
        rootCircle = circles[ServerType.CORE.ordinal()];
        addNode(discoveryService.getCurrentServer());
        for (ServerInstance instance : discoveryService.getOtherServers()) {
            addNode(instance);
        }
        logCircle();
        log.info("Cluster routing service initialized!");
    }

    @Override
    public ServerAddress getCurrentServer() {
        return discoveryService.getCurrentServer().getServerAddress();
    }

    @Override
    public Optional<ServerAddress> resolveById(EntityId entityId) {
        return resolveByUuid(rootCircle, entityId);
    }

    /**
     * 返回调度服务器
     * 1. 当前服务器与计算要调度的服务器不一致，返回调度服务器
     * 2. 一致，返回空
     * @param circle
     * @param uuid
     * @return
     */
    private Optional<ServerAddress> resolveByUuid(ConsistentHashCircle circle, EntityId entityId) {
        Assert.notNull(entityId);
        if (circle.isEmpty()) {
            return Optional.empty();
        }
        Long hash = hashFunction.newHasher().putLong(entityId.getId().getMostSignificantBits())
                .putLong(entityId.getId().getLeastSignificantBits()).hash().asLong();
        if (!circle.containsKey(hash)) {
            ConcurrentNavigableMap<Long, ServerInstance> tailMap =
                    circle.tailMap(hash);
            hash = tailMap.isEmpty() ?
                    circle.firstKey() : tailMap.firstKey();
        }
        ServerInstance result = circle.get(hash);
        if (!currentServer.equals(result)) {
            return Optional.of(result.getServerAddress());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void onServerAdded(ServerInstance server) {
        log.info("On server added event: {}", server);
        addNode(server);
        logCircle();
    }

    @Override
    public void onServerUpdated(ServerInstance server) {
        log.debug("Ignoring server onUpdate event: {}", server);
    }

    @Override
    public void onServerRemoved(ServerInstance server) {
        log.info("On server removed event: {}", server);
        removeNode(server);
        logCircle();
    }

    private void addNode(ServerInstance instance) {
        for (int i = 0; i < virtualNodesSize; i++) {
            circles[instance.getServerAddress().getServerType().ordinal()].put(hash(instance, i).asLong(), instance);
        }
    }

    private void removeNode(ServerInstance instance) {
        for (int i = 0; i < virtualNodesSize; i++) {
            circles[instance.getServerAddress().getServerType().ordinal()].remove(hash(instance, i).asLong());
        }
    }

    private HashCode hash(ServerInstance instance, int i) {
        return hashFunction.newHasher().putString(instance.getHost(), MiscUtils.UTF8).putInt(instance.getPort()).putInt(i).hash();
    }

    private void logCircle() {
        log.trace("Consistent Hash Circle Start");
        Arrays.asList(circles).forEach(ConsistentHashCircle::log);
        log.trace("Consistent Hash Circle End");
    }

}
