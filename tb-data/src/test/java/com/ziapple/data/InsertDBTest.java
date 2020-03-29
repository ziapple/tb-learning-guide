package com.ziapple.data;

import jdk.nashorn.internal.codegen.CompilerConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

@Slf4j
public class InsertDBTest {
    // 并发线程数
    private static final int threads = 10;
    // 每个线程的数据规模
    private static final int nums = 100000;
    // 计时器
    private static CountDownLatch countDownLatch = new CountDownLatch(threads);
    private static ExecutorService executorService = Executors.newFixedThreadPool(threads);
    private static Timer timer = new Timer();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        InsertDBTest dbTest = new InsertDBTest();
        dbTest.run(true);
        dbTest.watch();
    }

    public void run(boolean isAsync){
        for(int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                for(int j=0; j<nums; j++) {
                    if(isAsync){
                        HADao.writeAsync("消息" + j, () -> {
                            return null;
                        }, executorService);
                    }else{
                        HADao.write("消息" + j);
                    }
                }
                countDownLatch.countDown();
            });
        }
    }


    public void watch(){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                HADao.printStats();
            }
        }, 1000, 1000);
    }
}
