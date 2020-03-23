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
import com.ziapple.server.cluster.DiscoveryService;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.rpc.ClusterRpcService;
import com.ziapple.server.cluster.rpc.RpcBroadcastMsg;
import com.ziapple.server.cluster.rpc.RpcManagerActor;
import com.ziapple.server.cluster.rpc.RpcSessionCreateRequestMsg;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${cluster.stats.enabled:false}")
    private boolean statsEnabled;
    private final AtomicInteger sentClusterMsgs = new AtomicInteger(0);
    private final AtomicInteger receivedClusterMsgs = new AtomicInteger(0);

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
        appActor = system.actorOf(Props.create(new AppActor.ActorCreator(actorContext)).withDispatcher(APP_DISPATCHER_NAME), "appActor");
        actorContext.setAppActor(appActor);
        rpcService.init(this);
        rpcManagerActor = system.actorOf(Props.create(new RpcManagerActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME), "rpcManagerActor");
        log.info("Actor system initialized.");
    }

    /**
     * 关闭ActorSystem
     */
    @PreDestroy
    public void stopActorSystem() {
        Future<Terminated> status = system.terminate();
        try {
            Terminated terminated = Await.result(status, Duration.Inf());
            log.info("Actor system terminated: {}", terminated);
        } catch (Exception e) {
            log.error("Failed to terminate actor system.", e);
        }
    }

    /**
     * 定期打印收到和发送给Cluster的消息
     */
    @Scheduled(fixedDelayString = "${cluster.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            int sent = sentClusterMsgs.getAndSet(0);
            int received = receivedClusterMsgs.getAndSet(0);
            if (sent > 0 || received > 0) {
                log.info("Cluster msgs sent [{}] received [{}]", sent, received);
            }
        }
    }

    @Override
    public void onReceivedMsg(ServerAddress source, ClusterAPIProtos.ClusterMessage msg) {
        if (statsEnabled) {
            receivedClusterMsgs.incrementAndGet();
        }
        ServerAddress serverAddress = new ServerAddress(source.getHost(), source.getPort(), source.getServerType());
    }

    @Override
    public void onSendMsg(ClusterAPIProtos.ClusterMessage msg) {
        if (statsEnabled) {
            sentClusterMsgs.incrementAndGet();
        }
        rpcManagerActor.tell(msg, ActorRef.noSender());
    }

    @Override
    public void onRpcSessionCreateRequestMsg(RpcSessionCreateRequestMsg msg) {

    }

    @Override
    public void onBroadcastMsg(RpcBroadcastMsg msg) {

    }
}
