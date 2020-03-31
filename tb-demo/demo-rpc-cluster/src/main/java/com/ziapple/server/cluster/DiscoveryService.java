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

import java.util.List;

/**
 * 发现服务，把本地服务器注册到集群上，并且获取集群上的其他服务器
 * @author Andrew Shvayka
 */
public interface DiscoveryService {
    void init();

    void publishCurrentServer();

    void unpublishCurrentServer();

    ServerInstance getCurrentServer();

    List<ServerInstance> getOtherServers();
}
