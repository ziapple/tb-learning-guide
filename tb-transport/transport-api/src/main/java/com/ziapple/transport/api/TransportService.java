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
 * 1. 处理Mqtt消息的服务Service，交给不同的Akka的Actor进行处理
 * 2. 只有一个实现类{@link com.ziapple.transport.api.service.RemoteTransportService}
 * 3. 所有process方法都是通过{@link com.ziapple.server.kafka.AsyncCallbackTemplate}模板来启动线程来处理，里面固定了线程执行完成的回调函数
 */
public interface TransportService {

    /**
     * 验证设备连接，token方法
     * @param msg 只有一个字段，token，代表用户名
     * @param callback
     */
    void process(ValidateDeviceTokenRequestMsg msg, TransportServiceCallback<ValidateDeviceCredentialsResponseMsg> callback);

    /**
     * 验证设备的数字证书
     * @param msg 只有一个字段，hash，代表加密后的内容
     * @param callback
     */
    void process(ValidateDeviceX509CertRequestMsg msg, TransportServiceCallback<ValidateDeviceCredentialsResponseMsg> callback);

    /**
     * 创建或者获取设备
     * @param msg 网关高位、低位占位符，设备名称、设备类型
     *   int64 gatewayIdMSB = 1;
     *   int64 gatewayIdLSB = 2;
     *   string deviceName = 3;
     *   string deviceType = 4;
     * @param callback
     */
    void process(TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg msg, TransportServiceCallback<TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg> callback);

    boolean checkLimits(SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<Void> callback);

    /**
     * 会话消息
     * @param sessionInfo，服务端节点Id、sessionId、tenantId、deviceId
     *   string nodeId = 1;
     *   int64 sessionIdMSB = 2;
     *   int64 sessionIdLSB = 3;
     *   int64 tenantIdMSB = 4;
     *   int64 tenantIdLSB = 5;
     *   int64 deviceIdMSB = 6;
     *   int64 deviceIdLSB = 7;
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, SessionEventMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 处理设备时序数据
     * @param sessionInfo
     * @param msg {ts(时间戳), List<KeyValueProto}
     *   KeyValueProto {
     *   string key = 1;
     *   KeyValueType type = 2;
     *   bool bool_v = 3;
     *   int64 long_v = 4;
     *   double double_v = 5;
     *   string string_v = 6;
     * }
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 处理设备属性信息
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 获取设备属性信息
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 订阅设备属性变化
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 订阅RPC消息
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback);

    /**
     * RPC消息反馈
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback);

    /**
     * RPC消息请求
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 给TB-NODE报告设备状态，在缓存中存储状态
     * Used to report session state to tb-node and persist this state in the cache on the tb-node level.
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscriptionInfoProto msg, TransportServiceCallback<Void> callback);

    /**
     * 设备声明
     * @param sessionInfo
     * @param msg
     * @param callback
     */
    void process(SessionInfoProto sessionInfo, ClaimDeviceMsg msg, TransportServiceCallback<Void> callback);

    void registerAsyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener);

    void registerSyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout);

    /**
     * 报告设备状态
     * @param sessionInfo
     */
    void reportActivity(SessionInfoProto sessionInfo);

    void deregisterSession(SessionInfoProto sessionInfo);

}
