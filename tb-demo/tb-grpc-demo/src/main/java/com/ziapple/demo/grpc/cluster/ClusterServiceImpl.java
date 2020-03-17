package com.ziapple.demo.grpc.cluster;

import com.ziapple.server.rpc.ClusterProto;
import com.ziapple.server.rpc.ClusterServiceGrpc;
import io.grpc.examples.calculate.CalculateProto;
import io.grpc.examples.calculate.CalculateServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.logging.Logger;

/**
 * @Author zsunny
 * @Date 2018/7/15 16:28
 * @Mail zsunny@yeah.net
 */
public class ClusterServiceImpl extends ClusterServiceGrpc.ClusterServiceImplBase {

    private Logger log = Logger.getLogger("ClusterServiceImp");

    @Override
    public StreamObserver<ClusterProto.MsgRequest> handleMsg(
            StreamObserver<ClusterProto.MsgResponse> responseObserver) {
        return new StreamObserver<ClusterProto.MsgRequest>() {
            public void onNext(ClusterProto.MsgRequest request) {
                log.info("接收到消息为:" + request.getMsgType());
                //返回当前统计结果
                ClusterProto.MsgResponse response = ClusterProto.MsgResponse.newBuilder().setStatus("success").build();
                log.info("返回消息为:" + response);
                responseObserver.onNext(response);
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