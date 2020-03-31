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


import com.ziapple.common.data.id.EntityId;

import java.util.Optional;

/**
 * 集群路由服务
 * 1. 获取本地服务器getCurrentServer，每一台集群的服务器都要知道自己
 * 2. 通过hash环，根据实体Id分配对应的集群内要处理服务器
 * @author Andrew Shvayka
 */
public interface ClusterRoutingService extends DiscoveryServiceListener {

    ServerAddress getCurrentServer();

    Optional<ServerAddress> resolveById(EntityId entityId);

}
