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
package com.ziapple.test.service.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.ziapple.common.data.Tenant;
import com.ziapple.common.data.id.DeviceId;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.kv.BasicTsKvEntry;
import com.ziapple.common.data.kv.KvEntry;
import com.ziapple.common.data.kv.LongDataEntry;
import com.ziapple.common.data.kv.TsKvEntry;
import com.ziapple.test.dao.DaoNoSqlTest;
import com.ziapple.test.service.AbstractServiceTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL数据库压力测试
 * 批处理的前提是具有同样的Statement（往同一张表），不同的Statement放一起批处理提交效率会很低
 * 经测试，采用异步提交可以提高请求量，批量提交效果不明显
 * jpa没有异步提交，有批量提交功能，所有采用jdbcTemplate.batchUpdate方式，效果显著
 * cassandra有异步提交模式，也有批量提交功能，批量提交效果不明显
 */
@DaoNoSqlTest
@Slf4j
public class TimeseriesNoSqlPerformanceTest extends AbstractServiceTest {
    // 线程数
    private static int threads = 10;
    // 总的写入数量
    private static int total_tps = 10000;
    // 模拟租户
    private TenantId tenantId;
    // 监控器
    private ScheduledExecutorService logExecutor;
    // 记录并发数
    private AtomicInteger nTps = new AtomicInteger(0);
    private CountDownLatch countDownLatch = new CountDownLatch(threads);

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("DB Performance");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
        logExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private static TsKvEntry toTsEntry(long ts, KvEntry entry) {
        return new BasicTsKvEntry(ts, entry);
    }

    @Test
    public void testAsyncDBPerformance() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        for(int i=0; i<threads; i++){
            new Thread(asyncWriteTask(total_tps/threads)).start();
        }

        // 打印并发数
        logExecutor.scheduleAtFixedRate(()->{
            log.info("并发写入数{}", nTps.getAndSet(0));
        },0,1000, TimeUnit.MILLISECONDS);

        countDownLatch.await();
        long endTime = System.currentTimeMillis();
        log.info("总数{}，线程{}，所需时间{}秒", total_tps, threads, (endTime-startTime)/1000);
    }

    public Runnable asyncWriteTask(int records_per_thread){
        Runnable task = new Runnable(){
            DeviceId deviceId = new DeviceId(UUIDs.timeBased());
            Random random = new Random();

            public void run() {
                for(int i=0; i<records_per_thread; i++) {
                    // 模拟温度
                    KvEntry longKvEntry = new LongDataEntry("temperature", random.nextInt(40) + 0L);
                    // 异步写入，忽略写入时间
                    try {
                        tsService.save(tenantId, deviceId, toTsEntry(System.currentTimeMillis(), longKvEntry)).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    nTps.incrementAndGet();
                }
                countDownLatch.countDown();
            }
        };
        return task;
    }
}
