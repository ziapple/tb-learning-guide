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
import akka.actor.Scheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.ziapple.server.cluster.ClusterRoutingService;
import com.ziapple.server.cluster.DiscoveryService;
import com.ziapple.server.cluster.rpc.ClusterRpcService;
import com.ziapple.server.cluster.rpc.DataDecodingEncodingService;
import com.ziapple.server.data.id.TenantId;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateExecutor;
import org.thingsboard.server.kafka.TbNodeIdProvider;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    //租户请求的可以调试的节点并发限制
    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

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
    private final AtomicInteger jsInvokeRequestsCount = new AtomicInteger(0);
    @Getter
    private final AtomicInteger jsInvokeResponsesCount = new AtomicInteger(0);
    @Getter
    private final AtomicInteger jsInvokeFailuresCount = new AtomicInteger(0);

    @Scheduled(fixedDelayString = "${js.remote.stats.print_interval_ms}")
    public void printStats() {
        if (statisticsEnabled) {
            if (jsInvokeRequestsCount.get() > 0 || jsInvokeResponsesCount.get() > 0 || jsInvokeFailuresCount.get() > 0) {
                log.info("Rule Engine JS Invoke Stats: requests [{}] responses [{}] failures [{}]",
                        jsInvokeRequestsCount.getAndSet(0), jsInvokeResponsesCount.getAndSet(0), jsInvokeFailuresCount.getAndSet(0));
            }
        }
    }

    public ActorSystemContext() {
        config = ConfigFactory.parseResources(AKKA_CONF_FILE_NAME).withFallback(ConfigFactory.load());
    }

    public Scheduler getScheduler() {
        return actorSystem.scheduler();
    }


    /**
     * 检查规则链所有的Node的Debug下的并发限制
     * @param tenantId
     * @param tbMsg
     * @param error
     * @return
     */
    private boolean checkLimits(TenantId tenantId, TbMsg tbMsg, Throwable error) {
        if (debugPerTenantEnabled) {
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugPerTenantLimitsConfiguration), false));

            // 达到并发限制
            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {// 只保存一次告警
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 达到并发限制，保存告警事件
     * @param tenantId 租户Id
     * @param entityId 实体Id
     * @param error
     */
    private void persistRuleChainDebugModeEvent(TenantId tenantId, EntityId entityId, Throwable error) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.DEBUG_RULE_CHAIN);

        ObjectNode node = mapper.createObjectNode()
                //todo: what fields are needed here?
                .put("server", getServerAddress())
                .put("message", "Reached debug mode rate limit!");

        if (error != null) {
            node = node.put("error", toString(error));
        }

        event.setBody(node);
        ListenableFuture<Event> future = eventService.saveAsync(event);
        Futures.addCallback(future, new FutureCallback<Event>() {
            @Override
            public void onSuccess(@Nullable Event event) {

            }

            @Override
            public void onFailure(Throwable th) {
                log.error("Could not save debug Event for Rule Chain", th);
            }
        });
    }

    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }

}
