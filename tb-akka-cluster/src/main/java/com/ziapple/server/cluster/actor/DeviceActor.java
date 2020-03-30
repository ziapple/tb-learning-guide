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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ziapple.server.cluster.actor.process.DeviceActorMessageProcessor;
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.cluster.msg.TransportToDeviceActorMsg;
import com.ziapple.server.data.id.DeviceId;

/**
 * thingsboard主Actor，接收设备端发来的消息
 * 1. 从{@code LocalTranportService}接收到的Mqtt消息全部交给AppActor处理
 */
public class DeviceActor extends ContextAwareActor {
    private final BiMap<DeviceId, ActorRef> deviceActors;
    private final DeviceActorMessageProcessor processor;

    public static class DeviceActorCreator extends ContextBasedCreator<DeviceActor> {
        private static final long serialVersionUID = 1L;

        private final DeviceId deviceId;

        public DeviceActorCreator(ActorSystemContext context, DeviceId deviceId) {
            super(context);
            this.deviceId = deviceId;
        }

        @Override
        public DeviceActor create() {
            return new DeviceActor(context, deviceId);
        }
    }

    private DeviceActor(ActorSystemContext systemContext, DeviceId deviceId) {
        super(systemContext);
        this.deviceActors = HashBiMap.create();
        this.processor = new DeviceActorMessageProcessor(systemContext, deviceId);
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case TRANSPORT_TO_DEVICE_MSG:
                processor.process(context(), (TransportToDeviceActorMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    public void preStart () {
        log.debug("[{}] Starting device actor.", processor.getDeviceId());
    }
}
