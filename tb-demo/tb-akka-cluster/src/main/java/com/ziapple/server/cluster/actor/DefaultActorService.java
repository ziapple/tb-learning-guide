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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import com.google.protobuf.ByteString;
import com.ziapple.server.cluster.DiscoveryService;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.ServerInstance;
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.cluster.rpc.ClusterRpcService;
import com.ziapple.server.cluster.rpc.RpcBroadcastMsg;
import com.ziapple.server.cluster.rpc.RpcSessionCreateRequestMsg;
import com.ziapple.server.data.Device;
import com.ziapple.server.data.id.DeviceId;
import com.ziapple.server.data.id.EntityId;
import com.ziapple.server.data.id.TenantId;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;


@Service
@Slf4j
public class DefaultActorService implements ActorService {
    // Actor system 名称
    private static final String ACTOR_SYSTEM_NAME = "Akka";

    // APP Actor的线程池调度名称
    public static final String APP_DISPATCHER_NAME = "app-dispatcher";
    // 权限Actor
    public static final String CORE_DISPATCHER_NAME = "core-dispatcher";
    // 系统规则引擎
    public static final String SYSTEM_RULE_DISPATCHER_NAME = "system-rule-dispatcher";
    // 租户规则引擎
    public static final String TENANT_RULE_DISPATCHER_NAME = "rule-dispatcher";
    // RPC Actor
    public static final String RPC_DISPATCHER_NAME = "rpc-dispatcher";

    // actor上下文，封装了actor用到的Service
    @Autowired
    private ActorSystemContext actorContext;

    @Autowired
    private ClusterRpcService rpcService;

    @Autowired
    private DiscoveryService discoveryService;

    private ActorSystem system;

    private ActorRef appActor;

    private ActorRef rpcManagerActor;

    /**
     * 初始化ActorSystem
     * 1. 创建AppActor、RPCMangerActor、StatsActor三个核心的Actor
     * 2. 启动RPC服务，调用{@code ClusterGrpcService}
     */
    @PostConstruct
    public void initActorSystem() {
        log.info("Initializing Actor system.");
        actorContext.setActorService(this);
        system = ActorSystem.create(ACTOR_SYSTEM_NAME, actorContext.getConfig());
        actorContext.setActorSystem(system);



        rpcService.init(this);
        log.info("Actor system initialized.");
    }
}
