package com.ziapple.test.dao;

import com.ziapple.test.dao.data.TsKvEntity;
import com.ziapple.test.dao.sql.ScheduledLogExecutorComponent;
import com.ziapple.test.dao.sql.TbSqlBlockingQueue;
import com.ziapple.test.dao.sql.TbSqlBlockingQueueParams;
import lombok.extern.slf4j.Slf4j;

/**
 * 并发写入队列测试
 */
@Slf4j
public class TbSqlBlockingQueueTest{
    // 监控器
    private ScheduledLogExecutorComponent logExecutor;
    // 队列并发写入参数
    private TbSqlBlockingQueueParams tbSqlBlockingQueueParams;
    // 并发缓存队列
    private TbSqlBlockingQueue<TsKvEntity> tbSqlBlockingQueue;
    // 模拟数据库客户端
    private DummySqlClient dummySqlClient;
    // 模拟Dao层
    private DummyEntityDao<TsKvEntity> dummyEntityDao;
    // 模拟Service
    private DummyEntityService dummyEntityService;
    // 模拟数据库的最大并发能力, 建议(thread_size * max_tps_thread) * 0.8 = max_db_tps
    private static int max_db_tps = 10000;
    // 模拟客户端数量, 建议(thread_size * max_tps_thread) * 0.8 = max_db_tps
    private static int thread_size = 8;
    // 模拟每个客户端并发数量, 建议(thread_size * max_tps_thread) * 0.8 = max_db_tps
    private static int max_tps_tread = 1000;

    public void init(){
        DummySqlClient.max_tps = max_db_tps;
        dummySqlClient = new DummySqlClient();
        logExecutor = new ScheduledLogExecutorComponent();
        logExecutor.init();
        // 单客户端节点批处理大小和时间设置，batchSize < 数据库的最大并发数 / 节点数，maxDelay < 数据库批处理时间, 此处batchSize = max_db_tps
        tbSqlBlockingQueueParams = TbSqlBlockingQueueParams.builder()
                .logName("KvEntity TimeScale")
                .batchSize(max_db_tps)
                .maxDelay(1000)
                .statsPrintIntervalMs(1000).build();
        tbSqlBlockingQueue = new TbSqlBlockingQueue<>(tbSqlBlockingQueueParams);
        dummyEntityDao = new DummyEntityDao<>(tbSqlBlockingQueue);
        dummyEntityService = new DummyEntityService(dummyEntityDao);
    }

    public void run(){
        tbSqlBlockingQueue.init(logExecutor, ts -> dummySqlClient.batch(ts));
    }


    public static void main(String[] args) {
        TbSqlBlockingQueueTest test = new TbSqlBlockingQueueTest();
        test.init();
        DummyEntityProcessor dummyEntityProcessor = new DummyEntityProcessor(thread_size, max_tps_tread, test.dummyEntityService);
        dummyEntityProcessor.init();
        test.run();
    }
}
