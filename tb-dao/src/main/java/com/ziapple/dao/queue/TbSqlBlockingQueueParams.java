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
package com.ziapple.dao.queue;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
public class TbSqlBlockingQueueParams {
    // 数据库并发监控的线程名称
    private final String logName;
    /**
     * 数据库一次批处理最大记录，需要提前压测数据库的最大批处理能力db_total_batchSize
     * batchSize = db_total_batchSize / 节点数
      */
    private final int batchSize;
    /**
     * 一次批处理最大时间，如果数据库批处理时间currentTs < maxDelay，客户端会休眠maxDelay-currentTs，防止给数据库的负载过大
     * 如果currentTs > maxDelay, 意味着数据库批处理的能力 < 客户端当前并发量，缓存队列会越来越大，这种情况下需要优化数据库！！！
     */
    private final long maxDelay;
    // 监控队列时间间隔，建议设置1000
    private final long statsPrintIntervalMs;
}
