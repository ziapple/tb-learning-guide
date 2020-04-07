package com.ziapple;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SettableFutureTest {
    // 队列，每个QueueElement包含一个写入对象和一个判断是否写入成功的Future
    private BlockingQueue<QueueElement> blockingQueue = new LinkedBlockingQueue<>();
    // 每次批处理的数量
    private int batchSize = 100;
    // 新增的记录
    AtomicInteger addedCount = new AtomicInteger(0);
    // 保存的记录
    AtomicInteger savedCount = new AtomicInteger(0);
    // 失败的记录
    AtomicInteger failedCount = new AtomicInteger(0);
    // 记录批次
    AtomicInteger batchCount = new AtomicInteger(0);
    // 单批次记录
    AtomicInteger tempCount = new AtomicInteger(0);

    public static void main(String[] args) {
        SettableFutureTest test = new SettableFutureTest();
        test.init();
    }


    public void init(){
        // 往队列里面写数据
        produce();
        // 开启扫描队列线程
        consume((elements) -> {
            // 批处理写入数据库, 随机模拟抛出异常，FutureCallback的onFailure捕获
            if(new Random().nextInt(100) < 50) {
                throw new RuntimeException("写入数据库异常");
            }
        });
        // 监控队列
        watch();
    }

    /**
     * 模拟并发写入数据
     */
    public void produce(){
        new Thread(() -> {
            while (!Thread.interrupted()) {
                if (tempCount.getAndIncrement() < batchSize) {
                    ListenableFuture<Element> future = createAndSave(tempCount.get(), batchCount.get() + "-" + tempCount.get());
                    // 可以异步获取每个保存记录的future返回结果，如果不追踪每条记录状态，可以不写
                    Futures.addCallback(future, new FutureCallback<Element>() {
                        @Override
                        public void onSuccess(Element el) {
                            System.out.println(el.getName() + "保存成功");
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            System.out.println("记录保存失败");
                        }
                    });
                } else {
                    try {
                        batchCount.getAndIncrement();
                        tempCount.set(0);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 消费数据，写入数据库
     */
    public void consume(Consumer<List<Element>> saveFunction){
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.submit(() -> {
            while(!Thread.interrupted()){
                List<QueueElement> elements = new ArrayList<>(batchSize);
                try {
                    blockingQueue.drainTo(elements, batchSize);
                    savedCount.addAndGet(elements.size());
                    // 写入数据库
                    saveFunction.accept(elements.stream().map(QueueElement::getElement).collect(Collectors.toList()));
                    // 设置成功返回值,调用callback
                    elements.forEach((e) -> e.future.set(e.element));
                }catch(Exception e){
                    // 告诉Future任务立即返回
                    elements.forEach(el -> el.getFuture().setException(e));
                    failedCount.addAndGet(elements.size());
                }
            }
        });
    }

    /**
     * 模拟保存对象
     */
    public ListenableFuture<Element> createAndSave(Integer id, String name){
        Element el = new Element(id, name);
        return add(el);
    }

    /**
     * 往队列里面写入对象
     * @param element
     * @return
     */
    public ListenableFuture<Element> add(Element element){
        SettableFuture<Element> future = SettableFuture.create();
        QueueElement queueElement = new QueueElement(element, future);
        blockingQueue.add(queueElement);
        addedCount.incrementAndGet();
        return future;
    }

    /**
     * 日志监控
     */
    public void watch(){
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(()->{
            System.out.println("addedCount:" + addedCount.getAndSet(0) +
                    ",savedCount:" + savedCount.getAndSet(0) +
                    ",failedCount:" + failedCount.getAndSet(0));
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 队列元素，每个对象关联一个SettableFuture，用来判断单个对象是否写入成功
     */
    public class QueueElement{
        @Getter
        private Element element;
        @Getter
        private SettableFuture<Element> future;

        public QueueElement(Element element, SettableFuture future){
            this.element = element;
            this.future =future;
        }
    }

    /**
     * 要写入的对象
     */
    @Data
    public class Element{
        private Integer id;
        private String name;

        public Element(Integer id, String name){
            this.id = id;
            this.name = name;
        }
    }
}
