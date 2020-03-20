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
package com.ziapple.server.cluster.actor.process;

import akka.actor.ActorContext;
import com.ziapple.server.cluster.actor.ActorSystemContext;
import com.ziapple.server.cluster.msg.TransportToDeviceActorMsg;
import com.ziapple.server.data.id.DeviceId;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理设备消息核心类，设备消息处理器
 * 他将设备消息转发给规则引擎
 * @author Andrew Shvayka
 */
@Slf4j
public class DeviceActorMessageProcessor extends AbstractTbMsgProcessor {
    @Getter
    final DeviceId deviceId;

    public DeviceActorMessageProcessor(ActorSystemContext systemContext, DeviceId deviceId) {
        super(systemContext);
        this.deviceId = deviceId;
    }

    public void process(ActorContext context, TransportToDeviceActorMsg msg) {
        // 处理时序消息
        handlePostTelemetryRequest(context, msg);
    }

    public void handlePostTelemetryRequest(ActorContext context, TransportToDeviceActorMsg postTelemetry) {
        log.info("推送给规则引擎{}", postTelemetry.getMsg());
    }
}
