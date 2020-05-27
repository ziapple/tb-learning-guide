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
package com.ziapple.dao.util;

import com.datastax.driver.core.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.ziapple.common.concurrent.TbRateLimits;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.util.ThingsBoardThreadFactory;
import com.ziapple.dao.nosql.CassandraStatementTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * 异步任务AsyncTask执行器，所有查询和写入任务添加到BlockingQueue阻塞队列
 * 和TbSqlBlockingQueue不同
 * - 增加了每个租户的并发请求限制perTenantLimits
 * - 读写操作全部通过Queue来操作，TbSqlBlockingQueue只负责写入
 * - 写操作不是批量操作，会影响性能
 */
@Slf4j
public abstract class AbstractBufferedRateBatchExecutor<T extends AsyncTask, F extends ListenableFuture<V>, V>  implements BufferedRateExecutor<T, F> {
    // 任务最大等待时间，超时的任务不会执行，默认为20000ms
    protected final long maxWaitTime;
    // 当前并发大于最大并发数，任务处理线程会暂停pollMs时间，直到当前并发小于设置的并发数
    protected final long pollMs;
    protected final BlockingQueue<AsyncTaskContext<T, V>> queue;
    protected final ExecutorService dispatcherExecutor;
    protected final ExecutorService callbackExecutor;
    protected final ScheduledExecutorService timeoutExecutor;
    // 当前并发限制
    protected final int concurrencyLimit;
    // 打印频率
    protected final int printQueriesFreq;
    // 记录打印频率
    protected final AtomicInteger printQueriesIdx = new AtomicInteger();
    // 开启租户限制
    protected final boolean perTenantLimitsEnabled;
    // 租户并发限制配置
    protected final String perTenantLimitsConfiguration;
    // 每个租户的并发限制，保护queue的写入数量最大值，超过最大值写入totalRejected
    protected final ConcurrentMap<TenantId, TbRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    // 记录每个租户的并发数,默认为5000:1,100000:60
    protected final ConcurrentMap<TenantId, AtomicInteger> rateLimitedTenants = new ConcurrentHashMap<>();
    // 最大当前处理的任务并发数量，超过则让线程等待pollMs毫秒
    protected final AtomicInteger concurrencyLevel = new AtomicInteger();
    // 当前队列已添加数量
    protected final AtomicInteger totalAdded = new AtomicInteger();
    // 当前队列待处理的数量
    protected final AtomicInteger totalLaunched = new AtomicInteger();
    // 当前队列记录成功写入的数量，理论上totalLaunched=totalReleased
    protected final AtomicInteger totalReleased = new AtomicInteger();
    // 当前队列失败的数量
    protected final AtomicInteger totalFailed = new AtomicInteger();
    // 当前队列超时的任务数量
    protected final AtomicInteger totalExpired = new AtomicInteger();
    // 超过租户限制会被拒绝
    protected final AtomicInteger totalRejected = new AtomicInteger();
    // 记录总的任务提交数
    protected final AtomicInteger totalRateLimited = new AtomicInteger();

    // 单条任务最大处理时间
    @Value("${cassandra.ts.max_delay}")
    protected int maxDelay;

    // 每批次处理最大记录数
    @Value("${cassandra.ts.batch_size}")
    protected int batchSize;

    public AbstractBufferedRateBatchExecutor(int queueLimit, int concurrencyLimit, long maxWaitTime, int dispatcherThreads, int callbackThreads, long pollMs,
                                             boolean perTenantLimitsEnabled, String perTenantLimitsConfiguration, int printQueriesFreq) {
        this.maxWaitTime = maxWaitTime;
        this.pollMs = pollMs;
        this.concurrencyLimit = concurrencyLimit;
        this.printQueriesFreq = printQueriesFreq;
        this.queue = new LinkedBlockingDeque<>(queueLimit);
        this.dispatcherExecutor = Executors.newFixedThreadPool(dispatcherThreads, ThingsBoardThreadFactory.forName("nosql-batch-dispatcher"));
        this.callbackExecutor = Executors.newWorkStealingPool(callbackThreads);
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("nosql-batch-timeout"));
        this.perTenantLimitsEnabled = perTenantLimitsEnabled;
        this.perTenantLimitsConfiguration = perTenantLimitsConfiguration;
        // 任务处理线程
        for (int i = 0; i < dispatcherThreads; i++) {
            dispatcherExecutor.submit(this::dispatch);
        }
    }

    protected abstract SettableFuture<V> create();
    protected abstract F wrap(T task, SettableFuture<V> future);
    protected abstract ResultSetFuture executeBatch(List<AsyncTaskContext<T, V>> tasks);

    /**
     * 提交任务
     * 1. 检查是否超过租户并发限制，超过则写入totalRejected
     * 2. 记录totalAdded，写入queue队列
     * @param task
     * @return
     */
    @Override
    public F submit(T task) {
        SettableFuture<V> settableFuture = create();
        F result = wrap(task, settableFuture);
        boolean perTenantLimitReached = false;
        if (perTenantLimitsEnabled) {
            if (task.getTenantId() == null) {
                log.info("Invalid task received: {}", task);
            } else if (!task.getTenantId().isNullUid()) {
                TbRateLimits rateLimits = perTenantLimits.computeIfAbsent(task.getTenantId(), id -> new TbRateLimits(perTenantLimitsConfiguration));
                if (!rateLimits.tryConsume()) {// 超过并发限制
                    rateLimitedTenants.computeIfAbsent(task.getTenantId(), tId -> new AtomicInteger(0)).incrementAndGet();
                    totalRateLimited.incrementAndGet();
                    settableFuture.setException(new TenantRateLimitException());
                    perTenantLimitReached = true;
                }
            }
        }
        if (!perTenantLimitReached) {// 每个租户还未达到并发限制，添加到Queue队列，每个任务配套一个SettableFuture
            try {
                totalAdded.incrementAndGet();
                queue.add(new AsyncTaskContext<>(UUID.randomUUID(), task, settableFuture, System.currentTimeMillis()));
            } catch (IllegalStateException e) {
                // 记录因并发限制而拒绝的请求
                totalRejected.incrementAndGet();
                settableFuture.setException(e);
            }
        }
        return result;
    }

    public void stop() {
        if (dispatcherExecutor != null) {
            dispatcherExecutor.shutdownNow();
        }
        if (callbackExecutor != null) {
            callbackExecutor.shutdownNow();
        }
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    /**
     * 任务执行线程，dispatcherExecutor
     */
    private void dispatch() {
        log.info("Buffered rate executor thread started");
        while (!Thread.interrupted()) {
            int curLvl = concurrencyLevel.get();
            // 批量取数
            List<AsyncTaskContext<T, V>> tasks = new ArrayList<>();
            try {
                if (curLvl <= concurrencyLimit) {
                    // 获取队列第一个任务
                    AsyncTaskContext<T, V> headBatchTaskCtx  = queue.poll(maxDelay, TimeUnit.MILLISECONDS);
                    if(headBatchTaskCtx == null)
                        continue;
                    else
                        tasks.add(headBatchTaskCtx);

                    queue.drainTo(tasks, batchSize - 1);
                    if (printQueriesFreq > 0) {
                        if (printQueriesIdx.addAndGet(tasks.size()) >= printQueriesFreq) {
                            printQueriesIdx.set(0);
                            // 打印每批次第一条
                            String query = queryToString(headBatchTaskCtx);
                            log.info("[{}] Cassandra query: {}", headBatchTaskCtx.getId(), query);
                        }
                    }
                    logTask("Processing batch ", headBatchTaskCtx);
                    concurrencyLevel.addAndGet(tasks.size());

                    // 任务最大等待时间，超过maxWaitTime的批任务不会被执行
                    long timeout = headBatchTaskCtx.getCreateTime() + maxWaitTime - System.currentTimeMillis();
                    if (timeout > 0) {
                        // 当前预处理的任务数量
                        totalLaunched.addAndGet(tasks.size());
                        // 批量执行任务
                        ListenableFuture batchResult = executeBatch(tasks);
                        Futures.withTimeout(batchResult, timeout, TimeUnit.MILLISECONDS, timeoutExecutor);
                        Futures.addCallback(batchResult, new FutureCallback<ResultSet>() {
                            @Override
                            public void onSuccess(@Nullable ResultSet result) {
                                logTask("Releasing", headBatchTaskCtx);
                                totalReleased.addAndGet(tasks.size());
                                concurrencyLevel.addAndGet(-1 * tasks.size());
                                // 设置返回值，表明执行成功
                                tasks.forEach(task -> task.getFuture().set(null));
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (t instanceof TimeoutException) {
                                    logTask("Expired During Execution", headBatchTaskCtx);
                                } else {
                                    logTask("Failed", headBatchTaskCtx);
                                }
                                // 全部批处理任务失败
                                totalFailed.addAndGet(tasks.size());
                                concurrencyLevel.addAndGet(-1 * tasks.size());
                                headBatchTaskCtx.getFuture().setException(t);
                                log.debug("[{}] Failed to execute task: {}", headBatchTaskCtx.getId(), headBatchTaskCtx.getTask(), t);
                            }
                        }, callbackExecutor);
                    } else {
                        logTask("Expired Before Execution", headBatchTaskCtx);
                        totalExpired.addAndGet(tasks.size());
                        concurrencyLevel.addAndGet(-1 * tasks.size());
                        tasks.forEach(task -> task.getFuture().setException(new TimeoutException()));
                    }
                } else {
                    Thread.sleep(pollMs);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (tasks != null) {
                    log.debug("Failed to execute task: {}", tasks.size(), e);
                    totalFailed.addAndGet(tasks.size());
                    concurrencyLevel.addAndGet(-1 * tasks.size());
                } else {
                    log.debug("Failed to queue task:", e);
                }
            }
        }
        log.info("Buffered rate executor thread stopped");
    }

    private void logTask(String action, AsyncTaskContext<T, V> taskCtx) {
        if (log.isTraceEnabled()) {
            if (taskCtx.getTask() instanceof CassandraStatementTask) {
                String query = queryToString(taskCtx);
                log.trace("[{}] {} task: {}, BoundStatement query: {}", taskCtx.getId(), action, taskCtx, query);
            } else {
                log.trace("[{}] {} task: {}", taskCtx.getId(), action, taskCtx);
            }
        } else {
            log.debug("[{}] {} task", taskCtx.getId(), action);
        }
    }

    // 打印任务的Sql语句
    private String queryToString(AsyncTaskContext<T, V> taskCtx) {
        CassandraStatementTask cassStmtTask = (CassandraStatementTask) taskCtx.getTask();
        if (cassStmtTask.getStatement() instanceof BoundStatement) {
            BoundStatement stmt = (BoundStatement) cassStmtTask.getStatement();
            String query = stmt.preparedStatement().getQueryString();
            try {
                query = toStringWithValues(stmt, ProtocolVersion.V5);
            } catch (Exception e) {
                log.warn("Can't convert to query with values", e);
            }
            return query;
        } else {
            return "Not Cassandra Statement Task";
        }
    }

    // 打印Sql语句的参数
    private static String toStringWithValues(BoundStatement boundStatement, ProtocolVersion protocolVersion) {
        CodecRegistry codecRegistry = boundStatement.preparedStatement().getCodecRegistry();
        PreparedStatement preparedStatement = boundStatement.preparedStatement();
        String query = preparedStatement.getQueryString();
        ColumnDefinitions defs = preparedStatement.getVariables();
        int index = 0;
        for (ColumnDefinitions.Definition def : defs) {
            DataType type = def.getType();
            TypeCodec<Object> codec = codecRegistry.codecFor(type);
            if (boundStatement.getBytesUnsafe(index) != null) {
                Object value = codec.deserialize(boundStatement.getBytesUnsafe(index), protocolVersion);
                String replacement = Matcher.quoteReplacement(codec.format(value));
                query = query.replaceFirst("\\?", replacement);
            }
            index++;
        }
        return query;
    }

    protected int getQueueSize() {
        return queue.size();
    }
}
