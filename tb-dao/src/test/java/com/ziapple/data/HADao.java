package com.ziapple.data;

import com.ziapple.common.concurrent.TbRateLimits;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高并发数据库写入
 * 假设数据库最大处理能力是10万qps，通过压力测试压测数据库最大性能<100k/qps
 */
@Slf4j
public class HADao{
    public static AtomicInteger insertedPQS = new AtomicInteger(0);
    // 最高10万并发，其他会被丢弃
    private static TbRateLimits tbRateLimits = new TbRateLimits("100000:1");

    // 异步写
    public static void writeAsync(String msg, Callable callback, ExecutorService executorService){
        if(tbRateLimits.tryConsume()) {
            try {
                //模拟写数据库, 执行异步操作
                FutureTask<Void> task = new FutureTask(new Callable() {
                    @Override
                    public Void call() throws Exception {
                        insertedPQS.incrementAndGet();
                        // 假设每条记录写入数据库1ms，单连接最大1000并发
                        Thread.sleep(1);
                        callback.call();
                        return null;
                    }
                });
                executorService.submit(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 同步写
    public static void write(String msg){
        if(tbRateLimits.tryConsume()) {
            try {
                insertedPQS.incrementAndGet();
                // 假设每条记录写入数据库1ms，单连接最大1000并发
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void printStats() {
        int inserted = insertedPQS.getAndSet(0);
        log.info("inserted values [{}]", inserted);
    }
}
