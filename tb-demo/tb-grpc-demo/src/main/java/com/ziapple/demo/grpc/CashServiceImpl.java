package com.ziapple.demo.grpc;

import io.grpc.stub.StreamObserver;

import java.util.logging.Logger;

/**
 * @Author zsunny
 * @Date 2018/7/15 16:28
 * @Mail zsunny@yeah.net
 */
public class CashServiceImpl extends CashServiceGrpc.CashServiceImplBase{

    private Logger log = Logger.getLogger("CaclulateImp");

    @Override
    public StreamObserver<CashProto.CashRequest> dealCash(StreamObserver<CashProto.CashReply> responseObserver) {
        return new StreamObserver<CashProto.CashRequest>() {

            private int sum = 0;
            private int cnt = 0;
            private double avg;

            public void onNext(CashProto.CashRequest request) {
                log.info("接收到消息为:" + request.getUser());
                sum += request.getMoney();
                cnt++;
                avg = 1.0*sum/cnt;
                //返回当前统计结果
                CashProto.CashReply reply = CashProto.CashReply.newBuilder().setStatus("success").build();
                log.info("返回消息为:" + reply);
                responseObserver.onNext(reply);
            }

            public void onError(Throwable throwable) {
                log.info("调用出错:" + throwable.getMessage());
            }

            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}