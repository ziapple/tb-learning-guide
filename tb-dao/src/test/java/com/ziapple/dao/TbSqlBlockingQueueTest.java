package com.ziapple.dao;

import com.ziapple.common.data.Device;
import com.ziapple.dao.sql.ScheduledLogExecutorComponent;
import com.ziapple.dao.sql.TbSqlBlockingQueue;
import com.ziapple.dao.sql.TbSqlBlockingQueueParams;
import lombok.extern.slf4j.Slf4j;

import javax.swing.text.html.parser.Entity;
import java.util.List;
import java.util.function.Consumer;

/**
 * 并发写入队列测试
 */
@Slf4j
public class TbSqlBlockingQueueTest<T extends Entity> {
    ScheduledLogExecutorComponent logExecutor;
    Consumer<List<T>> saveFunction;
    TbSqlBlockingQueueParams tbSqlBlockingQueueParams;
    TbSqlBlockingQueue tbSqlBlockingQueue;

    public void init(){
        logExecutor = new ScheduledLogExecutorComponent();
        logExecutor.init();
        saveFunction = ts -> ts.forEach(t -> {
            log.info("save device", t.getName());
        });
        tbSqlBlockingQueueParams = new TbSqlBlockingQueueParams("sql-test", 1000, 10, 5);
        tbSqlBlockingQueue = new TbSqlBlockingQueue<Device>(tbSqlBlockingQueueParams);
    }

    public void run(){
        tbSqlBlockingQueue.init(logExecutor, saveFunction);
    }

    public static void main(String[] args) {
        TbSqlBlockingQueueTest test = new TbSqlBlockingQueueTest();
        test.init();
        test.run();
    }
}
