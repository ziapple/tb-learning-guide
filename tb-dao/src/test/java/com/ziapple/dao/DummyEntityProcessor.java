package com.ziapple.dao;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.ziapple.dao.data.TsKvEntity;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟多线程（多客户端）往队列写入数据
 */
public class DummyEntityProcessor {
    // 并发写入线程，模拟客户端节点
    private int thread_size = 1;
    // 每个线程（节点）每秒最大写入量, 建议小于 （batchSize / thread_size） *  0.6
    private int tps_per_thread = 600;
    private DummyEntityService dummyEntityService;

    public DummyEntityProcessor(int thread_size, int tps_per_thread, DummyEntityService dummyEntityService){
        this.thread_size = thread_size;
        this.tps_per_thread = tps_per_thread;
        this.dummyEntityService = dummyEntityService;
    }

    public void init(){
        for(int i=0; i<thread_size; i++) {
            new Thread(() -> {
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                        new DummySingleProcessor(), 0, 1, TimeUnit.SECONDS
                );
            }).run();
        }
    }

    // 模拟客户端多并发提交
    public class DummySingleProcessor implements Runnable{
        AtomicInteger totalAdded = new AtomicInteger(0);
        Random random = new Random();

        @Override
        public void run() {
            while(!Thread.interrupted()){
                if(totalAdded.incrementAndGet() < tps_per_thread){
                    // 异步提交，立即返回
                    Futures.addCallback(DummyEntityProcessor.this.dummyEntityService.save(
                            Collections.singletonList(
                                    TsKvEntity.builder()
                                            .id(totalAdded.get())
                                            .key("temperature")
                                            .value(random.nextDouble())
                                            .ts(new Timestamp(new Date().getTime())).build())),
                            new FutureCallback<List<Void>>() {
                                @Override
                                public void onSuccess(List<Void> voids) {
                                    // 记录写入成功
                                }

                                @Override
                                public void onFailure(Throwable throwable) {

                                }
                            });
                }else{
                    totalAdded.set(0);
                    return;
                }
            }
        }
    }
}
