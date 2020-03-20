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
package com.ziapple.test.cluster;

import com.ziapple.server.cluster.DiscoveryService;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.ServerInstance;
import com.ziapple.server.cluster.ServerType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟服务注册和发现，模拟10个ServerInstance
 */
@Slf4j
public class MockDiscoveryService implements DiscoveryService {
    ServerInstance currentServer;

    public void init() {
        log.info("初始化集群服务器。。。");
        currentServer = new ServerInstance(new ServerAddress("192.168.56.0",8080, ServerType.CORE));
    }

    @Override
    public void publishCurrentServer() {
        log.info("注册本地服务器到集群。。。");
    }

    @Override
    public void unpublishCurrentServer() {
        log.info("从集群注销本地服务器。。。");
    }

    /**
     * 将192.168.56.0设置为本地服务器
     * @return
     */
    @Override
    public ServerInstance getCurrentServer() {
        return currentServer;
    }

    /**
     * 模拟10个集群服务器
     * @return
     */
    @Override
    public List<ServerInstance> getOtherServers() {
        List<ServerInstance> serverInstances = new ArrayList<>();
        for(int i=1; i<10; i++){
            serverInstances.add(new ServerInstance(new ServerAddress("192.168.56." + i, 8080, ServerType.CORE)));
        }
        return serverInstances;
    }
}
