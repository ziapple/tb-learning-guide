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
package com.ziapple.server.cluster.actor;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.ziapple.server.cluster.ClusterRoutingService;
import com.ziapple.server.cluster.DiscoveryService;
import com.ziapple.server.cluster.rpc.ClusterRpcService;
import com.ziapple.server.cluster.rpc.DataDecodingEncodingService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Actor系统上下文
 * 1. 包含租户、设备、资产等Service
 * 2. 保存Node节点Debug调试事件
 * 3. 定期打印JsInvoke请求数量、成功和失败数量
 */
@Slf4j
@Component
public class ActorSystemContext {
    private static final String AKKA_CONF_FILE_NAME = "actor-system.conf";

    //Json转化器
    protected final ObjectMapper mapper = new ObjectMapper();

    @Getter
    @Setter
    private ActorService actorService;

    @Autowired
    @Getter
    private DiscoveryService discoveryService;

    @Autowired
    @Getter
    private ClusterRoutingService routingService;

    @Autowired
    @Getter
    private ClusterRpcService rpcService;

    @Autowired
    @Getter
    private DataDecodingEncodingService encodingService;

    @Getter
    private final Config config;

    @Setter
    private ActorSystem actorSystem;

    public ActorSystemContext() {
        config = ConfigFactory.parseResources(AKKA_CONF_FILE_NAME).withFallback(ConfigFactory.load());
    }

    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }
}
