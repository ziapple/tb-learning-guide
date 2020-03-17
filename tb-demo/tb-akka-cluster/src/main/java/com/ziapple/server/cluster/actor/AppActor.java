/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ziapple.server.cluster.actor;

import akka.actor.ActorRef;
import akka.actor.SupervisorStrategy;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.msg.SendToClusterMsg;
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.data.id.TenantId;

import java.util.Optional;
import java.util.UUID;

/**
 * thingsboard主Actor，接收设备端发来的消息
 * 1. 从{@code LocalTranportService}接收到的Mqtt消息全部交给AppActor处理
 */
public class AppActor extends ContextAwareActor {
    public AppActor(ActorSystemContext systemContext) {
        super(systemContext);
    }

    /**
     * 消息处理
     * @param msg
     * @return
     */
    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case APP_INIT_MSG:  // APP初始化
                break;
            case SEND_TO_CLUSTER_MSG:   // 发送给集群消息
                onPossibleClusterMsg((SendToClusterMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }


    /**
     * 发送集群消息
     * @param msg
     */
    private void onPossibleClusterMsg(SendToClusterMsg msg) {
        Optional<ServerAddress> address = systemContext.getRoutingService().resolveById(msg.getEntityId());
        if (address.isPresent()) { // 获取集群节点，交给集群节点处理
            systemContext.getRpcService().tell(
                    systemContext.getEncodingService().convertToProtoDataMessage(address.get(), msg.getMsg()));
        } else {    // 如果集群节点是自己，交给本地处理
            self().tell(msg.getMsg(), ActorRef.noSender());
        }
    }
}
