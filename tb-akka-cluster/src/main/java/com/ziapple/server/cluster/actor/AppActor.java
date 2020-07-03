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
import akka.actor.Props;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ziapple.common.data.id.DeviceId;
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.cluster.msg.TransportToDeviceActorMsg;

/**
 * thingsboard主Actor，接收设备端发来的消息
 * 1. 从{@code LocalTranportService}接收到的Mqtt消息全部交给AppActor处理
 * AppActor->DeviceActor.pushToRuleEngine->AppActor.getOrCreateTenantActor->TenantActor
 * ->RuleChainManagerActor->RootChainActor.MessageProcess.DeviceActorToRuleEngineMsg
 * ->pushMsgToNode(RuleChainToRuleNodeMsg)->RuleNodeActor->RuleNodeActorMessageProcessor
 */
public class AppActor extends ContextAwareActor {
    private final BiMap<DeviceId, ActorRef> deviceActors;

    public AppActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.deviceActors = HashBiMap.create();
    }

    public static class ActorCreator extends ContextBasedCreator<AppActor> {
        private static final long serialVersionUID = 1L;
        public ActorCreator(ActorSystemContext context) {
            super(context);
        }
        @Override
        public AppActor create() {
            return new AppActor(context);
        }
    }

    /**
     * 消息处理
     * @param msg
     */
    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case APP_INIT_MSG:  // APP初始化
                break;
            case TRANSPORT_TO_DEVICE_MSG:   // 发送给集群消息
                onToDeviceActorMsg((TransportToDeviceActorMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 设备AppActor转发给TenantActor
     * MqttTransportHandler -> LocalTransportService.process -> AppActor -> TenantActor
     * @param msg
     */
    private void onToDeviceActorMsg(TransportToDeviceActorMsg msg) {
        getOrCreateDeviceActor((DeviceId) msg.getEntityId()).tell(msg, ActorRef.noSender());
    }

    /**
     * 获得设备actor，一个设备(deviceId)对应一个DeviceActor
     * @param deviceId
     * @return
     */
    private ActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return deviceActors.computeIfAbsent(deviceId, k -> {
            log.debug("[{}]Creating device actor.", deviceId);
            ActorRef deviceActor = context().actorOf(Props.create(new DeviceActor.DeviceActorCreator(systemContext, deviceId))
                            .withDispatcher(DefaultActorService.CORE_DISPATCHER_NAME)
                    , deviceId.toString());
            context().watch(deviceActor);
            log.debug("[{}] Created device actor: {}.", deviceId, deviceActor);
            return deviceActor;
        });
    }
}
