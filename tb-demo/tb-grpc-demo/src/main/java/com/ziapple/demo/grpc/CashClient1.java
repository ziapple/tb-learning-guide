package com.ziapple.demo.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @Author zsunny
 * @Date 2018/7/15 16:28
 * @Mail zsunny@yeah.net
 */
public class CashClient1 {

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = 50051;

    private static final int VALUE_NUM = 10;

    private static final int VALUE_UPPER_BOUND = 10;

    private static final Logger log = Logger.getLogger("CaculateClient");

    //这里用异步请求存根
    private CashServiceGrpc.CashServiceStub cashServiceStub;

    public CashClient1(String host, int port) {

        //使用明文通讯，这里简单化，实际生产环境需要通讯加密
        this(ManagedChannelBuilder.forAddress(host,port).usePlaintext().build());

    }

    public CashClient1(ManagedChannel managedChannel) {
        this.cashServiceStub = CashServiceGrpc.newStub(managedChannel);
    }

    /**
     * 实际调用部分
     * @param nums 传到服务端的数据流
     */
    public void getResult( List<Integer> nums){

        //判断调用状态。在内部类中被访问，需要加final修饰
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        StreamObserver<CashProto.CashReply> responseObserver = new StreamObserver<CashProto.CashReply>() {
            private int cnt = 0;
            public void onNext(CashProto.CashReply result) {
                //此处直接打印结果，其他也可用回调进行复杂处理
                log.info("第" + (++cnt) + "次调用得到结果为:" + result);
            }

            public void onError(Throwable throwable) {
                log.info("调用出错:{}" + throwable.getMessage());
                countDownLatch.countDown();
            }

            public void onCompleted() {
                log.info("调用完成");
                countDownLatch.countDown();
            }

        };

        StreamObserver<CashProto.CashRequest> requestObserver = cashServiceStub.dealCash(responseObserver);

        for(int num: nums){
            CashProto.CashRequest value = CashProto.CashRequest.newBuilder().setUser("ziapple").setMoney(num).build();
            requestObserver.onNext(value);

            //判断调用结束状态。如果整个调用已经结束，继续发送数据不会报错，但是会被舍弃
            if(countDownLatch.getCount() == 0){
                return;
            }
        }
        //异步请求，无法确保onNext与onComplete的完成先后顺序
        requestObserver.onCompleted();

        try {
            //如果在规定时间内没有请求完，则让程序停止
            if(!countDownLatch.await(5, TimeUnit.MINUTES)){
                log.info("未在规定时间内完成调用");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


    public static void main(String[] args) {

        CashClient1 additionClient = new CashClient1(DEFAULT_HOST,DEFAULT_PORT);

        //生成value值
        List<Integer> list = new ArrayList<Integer>();
        Random random = new Random();

        for(int i=0; i<VALUE_NUM; i++){
            //随机数符合 0-VALUE_UPPER_BOUND 均匀分布
            int value = random.nextInt(VALUE_UPPER_BOUND);

//            System.out.println(i + ":" + value);

            list.add(value);
        }

        System.out.println("*************************getting result from server***************************");
        System.out.println();

        additionClient.getResult(list);

    }
}