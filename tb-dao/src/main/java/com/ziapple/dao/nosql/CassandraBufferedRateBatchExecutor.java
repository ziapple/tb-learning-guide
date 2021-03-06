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
package com.ziapple.dao.nosql;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.SettableFuture;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.dao.util.AbstractBufferedRateBatchExecutor;
import com.ziapple.dao.util.AsyncTaskContext;
import com.ziapple.dao.util.NoSqlAnyDao;
import com.ziapple.service.EntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量提交
 */
@Component
@Slf4j
@NoSqlAnyDao
public class CassandraBufferedRateBatchExecutor extends AbstractBufferedRateBatchExecutor<CassandraStatementTask, ResultSetFuture, ResultSet> {

    @Autowired
    private EntityService entityService;
    private Map<TenantId, String> tenantNamesCache = new HashMap<>();

    private boolean printTenantNames;

    public CassandraBufferedRateBatchExecutor(
            @Value("${cassandra.query.buffer_size}") int queueLimit,
            @Value("${cassandra.query.concurrent_limit}") int concurrencyLimit,
            @Value("${cassandra.query.permit_max_wait_time}") long maxWaitTime,
            @Value("${cassandra.query.dispatcher_threads:2}") int dispatcherThreads,
            @Value("${cassandra.query.callback_threads:4}") int callbackThreads,
            @Value("${cassandra.query.poll_ms:50}") long pollMs,
            @Value("${cassandra.query.tenant_rate_limits.enabled}") boolean tenantRateLimitsEnabled,
            @Value("${cassandra.query.tenant_rate_limits.configuration}") String tenantRateLimitsConfiguration,
            @Value("${cassandra.query.tenant_rate_limits.print_tenant_names}") boolean printTenantNames,
            @Value("${cassandra.query.print_queries_freq:0}") int printQueriesFreq) {
        super(queueLimit, concurrencyLimit, maxWaitTime, dispatcherThreads, callbackThreads, pollMs, tenantRateLimitsEnabled, tenantRateLimitsConfiguration, printQueriesFreq);
        this.printTenantNames = printTenantNames;
    }

    @Scheduled(fixedDelayString = "${cassandra.query.rate_limit_print_interval_ms}")
    public void printStats() {
        int queueSize = getQueueSize();
        int totalAddedValue = totalAdded.getAndSet(0);
        int totalLaunchedValue = totalLaunched.getAndSet(0);
        int totalReleasedValue = totalReleased.getAndSet(0);
        int totalFailedValue = totalFailed.getAndSet(0);
        int totalExpiredValue = totalExpired.getAndSet(0);
        int totalRejectedValue = totalRejected.getAndSet(0);
        int totalRateLimitedValue = totalRateLimited.getAndSet(0);
        int rateLimitedTenantsValue = rateLimitedTenants.size();
        int concurrencyLevelValue = concurrencyLevel.get();
        if (queueSize > 0 || totalAddedValue > 0 || totalLaunchedValue > 0 || totalReleasedValue > 0 ||
                totalFailedValue > 0 || totalExpiredValue > 0 || totalRejectedValue > 0 || totalRateLimitedValue > 0 || rateLimitedTenantsValue > 0
                || concurrencyLevelValue > 0) {
            log.info("Permits queueSize [{}] totalAdded [{}] totalLaunched [{}] totalReleased [{}] totalFailed [{}] totalExpired [{}] totalRejected [{}] " +
                            "totalRateLimited [{}] totalRateLimitedTenants [{}] currBuffer [{}] ",
                    queueSize, totalAddedValue, totalLaunchedValue, totalReleasedValue,
                    totalFailedValue, totalExpiredValue, totalRejectedValue, totalRateLimitedValue, rateLimitedTenantsValue, concurrencyLevelValue);
        }

        rateLimitedTenants.forEach(((tenantId, counter) -> {
            if (printTenantNames) {
                String name = tenantNamesCache.computeIfAbsent(tenantId, tId -> {
                    try {
                        return entityService.fetchEntityNameAsync(TenantId.SYS_TENANT_ID, tenantId).get();
                    } catch (Exception e) {
                        log.error("[{}] Failed to get tenant name", tenantId, e);
                        return "N/A";
                    }
                });
                log.info("[{}][{}] Rate limited requests: {}", tenantId, name, counter);
            } else {
                log.info("[{}] Rate limited requests: {}", tenantId, counter);
            }
        }));
        rateLimitedTenants.clear();
    }

    @PreDestroy
    public void stop() {
        super.stop();
    }

    @Override
    protected SettableFuture<ResultSet> create() {
        return SettableFuture.create();
    }

    @Override
    protected ResultSetFuture wrap(CassandraStatementTask task, SettableFuture<ResultSet> future) {
        return new TbResultSetFuture(future);
    }

    // 批量执行statement
    protected ResultSetFuture executeBatch(List<AsyncTaskContext<CassandraStatementTask, ResultSet>> tasks) {
        Session session = tasks.get(0).getTask().getSession();
        BatchStatement batch = new BatchStatement();
        tasks.forEach(task -> batch.add(task.getTask().getStatement()));
        return session.executeAsync(batch);
    }

}
