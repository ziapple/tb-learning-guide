package com.ziapple.server.transport;

import akka.actor.ActorRef;
import com.ziapple.common.data.id.DeviceId;
import com.ziapple.server.cluster.ClusterRoutingService;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.actor.ActorSystemContext;
import com.ziapple.server.cluster.msg.TransportToDeviceActorMsg;
import com.ziapple.server.cluster.rpc.ClusterRpcService;
import com.ziapple.server.cluster.rpc.DataDecodingEncodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 模拟处理Mqtt消息的后端服务
 */
@Slf4j
@Service
public class LocalTransportService {
    @Autowired
    private ClusterRoutingService routingService;
    @Autowired
    private ClusterRpcService rpcService;
    @Autowired
    private ActorSystemContext actorContext;
    @Autowired
    private DataDecodingEncodingService encodingService;

    /**
     * 处理Mqtt消息
     * @param deviceId
     * @param msg
     */
    public void doProcess(DeviceId deviceId, String msg) {
        TransportToDeviceActorMsg transportToDeviceActorMsg = new TransportToDeviceActorMsg(deviceId, msg);
        Optional<ServerAddress> address = routingService.resolveById(deviceId);
        if (address.isPresent()) {// 远程调用
            rpcService.tell(encodingService.convertToProtoDataMessage(address.get(), transportToDeviceActorMsg));
        } else {// 运行本地Actor
            actorContext.getAppActor().tell(transportToDeviceActorMsg, ActorRef.noSender());
        }
    }
}
