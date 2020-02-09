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
package com.ziapple.transport.api.session;

import com.ziapple.server.session.SessionContext;
import com.ziapple.transport.TransportProtos.DeviceInfoProto;
import lombok.Data;
import lombok.Getter;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 *
 */
@Data
public abstract class DeviceAwareSessionContext implements SessionContext {

    @Getter
    protected final UUID sessionId;
    @Getter
    private volatile Long deviceId;
    @Getter
    private volatile DeviceInfoProto deviceInfo;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceInfo(DeviceInfoProto deviceInfo) {
        this.deviceInfo = deviceInfo;
        // 因为UUID太长，用高位和低位数值组合方式来拼接成UUID的设备ID
        // this.deviceId = new DeviceId(new UUID(deviceInfo.getDeviceIdMSB(), deviceInfo.getDeviceIdLSB()));
        // 此处做了简化，deviceId用高位的数值取代
        this.deviceId = deviceInfo.getDeviceIdMSB();
    }

    public boolean isConnected() {
        return deviceInfo != null;
    }
}
