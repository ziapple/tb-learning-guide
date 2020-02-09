package com.ziapple.transport.api; /**
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

import com.ziapple.transport.TransportProtos;
import com.ziapple.transport.TransportProtos.*;

/**
 * Created by ashvayka on 04.10.18.
 * 通过mvn compile命令自动会在target/generated-sources下面生成TransportProtos对象，并且加入classpath
 */
public interface TransportService {

    void process(ValidateDeviceTokenRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponseMsg> callback);

    void process(ValidateDeviceX509CertRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponseMsg> callback);

    void process(TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg msg,
                 TransportServiceCallback<TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg> callback);

    boolean checkLimits(SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, SessionEventMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscriptionInfoProto msg, TransportServiceCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, ClaimDeviceMsg msg, TransportServiceCallback<Void> callback);

    void registerAsyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener);

    void registerSyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout);

    void reportActivity(SessionInfoProto sessionInfo);

    void deregisterSession(SessionInfoProto sessionInfo);

}
