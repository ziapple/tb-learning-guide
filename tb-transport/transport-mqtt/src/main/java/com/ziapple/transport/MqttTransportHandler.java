package com.ziapple.transport; /**
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

import com.fasterxml.jackson.databind.JsonNode;
import com.ziapple.transport.TransportProtos.*;
import com.ziapple.transport.adaptors.MqttTransportAdaptor;
import com.ziapple.transport.api.SessionMsgListener;
import com.ziapple.transport.api.TransportService;
import com.ziapple.transport.api.TransportServiceCallback;
import com.ziapple.transport.api.service.AbstractTransportService;
import com.ziapple.transport.session.DeviceSessionCtx;
import com.ziapple.transport.session.GatewaySessionHandler;
import com.ziapple.transport.session.MqttTopicMatcher;
import com.ziapple.transport.util.EncryptionUtil;
import com.ziapple.transport.util.SslUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static io.netty.handler.codec.mqtt.MqttMessageType.*;
import static io.netty.handler.codec.mqtt.MqttQoS.*;

/**
 * MqttTransportHandler本身是一个多线程的，一个Socket连接会创建一个Handler线程来处理连接
 * @author Andrew Shvayka
 */
@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>>, SessionMsgListener {

    private static final MqttQoS MAX_SUPPORTED_QOS_LVL = AT_LEAST_ONCE;

    // 一个连接生成一个随机的会话Id
    private final UUID sessionId;
    // Mqtt上下文，包装了协议适配器，Mqtt消息转化器（转化成Akka格式）
    private final MqttTransportContext context;
    // 消息转换器，将Mqtt消息转化成Akka的Proto格式
    private final MqttTransportAdaptor adaptor;
    // 处理Mqtt消息的后端服务，CONNECT、PUBLISH、SUBCRIBE这些消息都是由transportService来做的
    private final TransportService transportService;
    // SSL协议解析器
    private final SslHandler sslHandler;
    // mqtt的Qos服务水平Map，一个Topic对应一个Qos
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;
    // 连接会话信息，含session、tenant、device的最高位、最低位位置,volatile表明在主存中读取，不从线程副本中读取,biao
    private volatile SessionInfoProto sessionInfo;
    // 记录Socket主机的hostname、ip、port
    private volatile InetSocketAddress address;
    // 设备会话上下文，一个sessionId会new一个设备会话Context
    private volatile DeviceSessionCtx deviceSessionCtx;
    // 网关的会话Handler
    private volatile GatewaySessionHandler gatewaySessionHandler;

    MqttTransportHandler(MqttTransportContext context) {
        this.sessionId = UUID.randomUUID();
        this.context = context;
        this.transportService = context.getTransportService();
        this.adaptor = context.getAdaptor();
        this.sslHandler = context.getSslHandler();
        this.mqttQoSMap = new ConcurrentHashMap<>();
        this.deviceSessionCtx = new DeviceSessionCtx(sessionId, mqttQoSMap);
    }

    /**
     * 消息接收会触发channelRead方法，一条消息触发一次processMqttMsg方法
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        try {
            if (msg instanceof MqttMessage) {
                processMqttMsg(ctx, (MqttMessage) msg);
            } else {
                ctx.close();
            }
        } finally {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    /**
     * 处理Mqtt消息
     * @param ctx
     * @param msg
     */
    private void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        // 获取远程客户端的地址
        address = (InetSocketAddress) ctx.channel().remoteAddress();
        // 报文固定头为空，直接断开连接
        if (msg.fixedHeader() == null) {
            log.info("[{}:{}] Invalid message received", address.getHostName(), address.getPort());
            processDisconnect(ctx);
            return;
        }
        // 把Channel放进去设备会话
        deviceSessionCtx.setChannel(ctx);
        switch (msg.fixedHeader().messageType()) {
            case CONNECT:  // Mqtt连接
                processConnect(ctx, (MqttConnectMessage) msg);
                break;
            case PUBLISH:  // 消息发布
                processPublish(ctx, (MqttPublishMessage) msg);
                break;
            case SUBSCRIBE: // 消息订阅
                processSubscribe(ctx, (MqttSubscribeMessage) msg);
                break;
            case UNSUBSCRIBE: // 取消消息订阅
                processUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
                break;
            case PINGREQ:  // PING请求
                if (checkConnected(ctx, msg)) {
                    ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
                    transportService.reportActivity(sessionInfo);
                    if (gatewaySessionHandler != null) {
                        gatewaySessionHandler.reportActivity();
                    }
                }
                break;
            case DISCONNECT:  //断开连接
                if (checkConnected(ctx, msg)) {
                    processDisconnect(ctx);
                }
                break;
            default:
                break;
        }

    }

    /**
     * 处理发布请求
     * @param ctx
     * @param mqttMsg
     */
    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            return;
        }
        String topicName = mqttMsg.variableHeader().topicName();
        int msgId = mqttMsg.variableHeader().packetId();
        log.trace("[{}][{}] Processing publish msg [{}][{}]!", sessionId, deviceSessionCtx.getDeviceId(), topicName, msgId);

        if (topicName.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC)) {
            if (gatewaySessionHandler != null) {
                handleGatewayPublishMsg(topicName, msgId, mqttMsg);
            }
        } else {
            processDevicePublish(ctx, mqttMsg, topicName, msgId);
        }
    }

    /**
     * 处理来自网关消息发送请求
     * @param topicName  主题名称
     * @param msgId      消息ID
     * @param mqttMsg    消息体
     */
    private void handleGatewayPublishMsg(String topicName, int msgId, MqttPublishMessage mqttMsg) {
        try {
            switch (topicName) {
                case MqttTopics.GATEWAY_TELEMETRY_TOPIC:  //时序数据
                    gatewaySessionHandler.onDeviceTelemetry(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_CLAIM_TOPIC:     //
                    gatewaySessionHandler.onDeviceClaim(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                    gatewaySessionHandler.onDeviceAttributes(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC:
                    gatewaySessionHandler.onDeviceAttributesRequest(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_RPC_TOPIC:
                    gatewaySessionHandler.onDeviceRpcResponse(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_CONNECT_TOPIC:
                    gatewaySessionHandler.onDeviceConnect(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_DISCONNECT_TOPIC:
                    gatewaySessionHandler.onDeviceDisconnect(mqttMsg);
                    break;
            }
        } catch (RuntimeException | adaptor.AdaptorException e) {
            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
        }
    }

    private void processDevicePublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, String topicName, int msgId) {
        try {
            if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = adaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(sessionInfo, postTelemetryMsg, getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = adaptor.convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(sessionInfo, postAttributeMsg, getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = adaptor.convertToGetAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(sessionInfo, getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = adaptor.convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg);
                transportService.process(sessionInfo, rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = adaptor.convertToServerRpcRequest(deviceSessionCtx, mqttMsg);
                transportService.process(sessionInfo, rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_CLAIM_TOPIC)) {
                TransportProtos.ClaimDeviceMsg claimDeviceMsg = adaptor.convertToClaimDevice(deviceSessionCtx, mqttMsg);
                transportService.process(sessionInfo, claimDeviceMsg, getPubAckCallback(ctx, msgId, claimDeviceMsg));
            }
        } catch (adaptor.AdaptorException e) {
            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            ctx.close();
        }
    }

    private <T> TransportServiceCallback<Void> getPubAckCallback(final ChannelHandlerContext ctx, final int msgId, final T msg) {
        return new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}] Published msg: {}", sessionId, msg);
                if (msgId > 0) {
                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
                }
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);
                processDisconnect(ctx);
            }
        };
    }

    private void processSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            return;
        }
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        List<Integer> grantedQoSList = new ArrayList<>();
        for (MqttTopicSubscription subscription : mqttMsg.payload().topicSubscriptions()) {
            String topic = subscription.topicName();
            MqttQoS reqQoS = subscription.qualityOfService();
            try {
                switch (topic) {
                    case MqttTopics.DEVICE_ATTRIBUTES_TOPIC: {
                        transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().build(), null);
                        registerSubQoS(topic, grantedQoSList, reqQoS);
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC: {
                        transportService.process(sessionInfo, TransportProtos.SubscribeToRPCMsg.newBuilder().build(), null);
                        registerSubQoS(topic, grantedQoSList, reqQoS);
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC:
                    case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                    case MqttTopics.GATEWAY_RPC_TOPIC:
                    case MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC:
                    case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC:
                        registerSubQoS(topic, grantedQoSList, reqQoS);
                        break;
                    default:
                        log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS);
                        grantedQoSList.add(FAILURE.value());
                        break;
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS);
                grantedQoSList.add(FAILURE.value());
            }
        }
        ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), grantedQoSList));
    }

    private void registerSubQoS(String topic, List<Integer> grantedQoSList, MqttQoS reqQoS) {
        grantedQoSList.add(getMinSupportedQos(reqQoS));
        mqttQoSMap.put(new MqttTopicMatcher(topic), getMinSupportedQos(reqQoS));
    }

    private void processUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            return;
        }
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        for (String topicName : mqttMsg.payload().topics()) {
            mqttQoSMap.remove(new MqttTopicMatcher(topicName));
            try {
                switch (topicName) {
                    case MqttTopics.DEVICE_ATTRIBUTES_TOPIC: {
                        transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setUnsubscribe(true).build(), null);
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC: {
                        transportService.process(sessionInfo, TransportProtos.SubscribeToRPCMsg.newBuilder().setUnsubscribe(true).build(), null);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
            }
        }
        ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId()));
    }

    private MqttMessage createUnSubAckMessage(int msgId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(UNSUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        return new MqttMessage(mqttFixedHeader, mqttMessageIdVariableHeader);
    }

    /**
     * 处理连接请求
     * @param ctx
     * @param msg
     */
    private void processConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        log.info("[{}] Processing connect msg for client: {}!", sessionId, msg.payload().clientIdentifier());
        X509Certificate cert;
        if (sslHandler != null && (cert = getX509Certificate()) != null) {
            // 数字证书请求
            processX509CertConnect(ctx, cert);
        } else {
            // token请求
            processAuthTokenConnect(ctx, msg);
        }
    }

    /**
     * 处理token请求
     * @param ctx
     * @param msg
     */
    private void processAuthTokenConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        String userName = msg.payload().userName();
        log.info("[{}] Processing connect msg for client with user name: {}!", sessionId, userName);
        if (StringUtils.isEmpty(userName)) {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD));
            ctx.close();
        } else {
            transportService.process(ValidateDeviceTokenRequestMsg.newBuilder().setToken(userName).build(),
                    new TransportServiceCallback<ValidateDeviceCredentialsResponseMsg>() {
                        @Override
                        public void onSuccess(ValidateDeviceCredentialsResponseMsg msg) {
                            onValidateDeviceResponse(msg, ctx);
                        }

                        @Override
                        public void onError(Throwable e) {
                            log.trace("[{}] Failed to process credentials: {}", address, userName, e);
                            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE));
                            ctx.close();
                        }
                    });
        }
    }

    private void processX509CertConnect(ChannelHandlerContext ctx, X509Certificate cert) {
        try {
            String strCert = SslUtil.getX509CertificateString(cert);
            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
            transportService.process(ValidateDeviceX509CertRequestMsg.newBuilder().setHash(sha3Hash).build(),
                    new TransportServiceCallback<ValidateDeviceCredentialsResponseMsg>() {
                        @Override
                        public void onSuccess(ValidateDeviceCredentialsResponseMsg msg) {
                            onValidateDeviceResponse(msg, ctx);
                        }

                        @Override
                        public void onError(Throwable e) {
                            log.trace("[{}] Failed to process credentials: {}", address, sha3Hash, e);
                            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE));
                            ctx.close();
                        }
                    });
        } catch (Exception e) {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
            ctx.close();
        }
    }

    private X509Certificate getX509Certificate() {
        try {
            X509Certificate[] certChain = sslHandler.engine().getSession().getPeerCertificateChain();
            if (certChain.length > 0) {
                return certChain[0];
            }
        } catch (SSLPeerUnverifiedException e) {
            log.warn(e.getMessage());
            return null;
        }
        return null;
    }

    private void processDisconnect(ChannelHandlerContext ctx) {
        ctx.close();
        log.info("[{}] Client disconnected!", sessionId);
        if (deviceSessionCtx.isConnected()) {
            transportService.process(sessionInfo, AbstractTransportService.getSessionEventMsg(SessionEvent.CLOSED), null);
            transportService.deregisterSession(sessionInfo);
            if (gatewaySessionHandler != null) {
                gatewaySessionHandler.onGatewayDisconnect();
            }
        }
    }

    /**
     * 反馈给客户端Mqtt连接消息：接收或者拒绝
     * @param returnCode
     * @return
     */
    private MqttConnAckMessage createMqttConnAckMsg(MqttConnectReturnCode returnCode) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(CONNACK, false, AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(returnCode, true);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[{}] Unexpected Exception", sessionId, cause);
        ctx.close();
    }

    private static MqttSubAckMessage createSubAckMessage(Integer msgId, List<Integer> grantedQoSList) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(SUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(grantedQoSList);
        return new MqttSubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
    }

    private static int getMinSupportedQos(MqttQoS reqQoS) {
        return Math.min(reqQoS.value(), MAX_SUPPORTED_QOS_LVL.value());
    }

    public static MqttPubAckMessage createMqttPubAckMsg(int requestId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(PUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMsgIdVariableHeader =
                MqttMessageIdVariableHeader.from(requestId);
        return new MqttPubAckMessage(mqttFixedHeader, mqttMsgIdVariableHeader);
    }

    private boolean checkConnected(ChannelHandlerContext ctx, MqttMessage msg) {
        if (deviceSessionCtx.isConnected()) {
            return true;
        } else {
            log.info("[{}] Closing current session due to invalid msg order: {}", sessionId, msg);
            ctx.close();
            return false;
        }
    }

    private void checkGatewaySession() {
        DeviceInfoProto device = deviceSessionCtx.getDeviceInfo();
        try {
            JsonNode infoNode = context.getMapper().readTree(device.getAdditionalInfo());
            if (infoNode != null) {
                JsonNode gatewayNode = infoNode.get("gateway");
                if (gatewayNode != null && gatewayNode.asBoolean()) {
                    gatewaySessionHandler = new GatewaySessionHandler(context, deviceSessionCtx, sessionId);
                }
            }
        } catch (IOException e) {
            log.trace("[{}][{}] Failed to fetch device additional info", sessionId, device.getDeviceName(), e);
        }
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        if (deviceSessionCtx.isConnected()) {
            transportService.process(sessionInfo, AbstractTransportService.getSessionEventMsg(SessionEvent.CLOSED), null);
            transportService.deregisterSession(sessionInfo);
        }
    }

    private void onValidateDeviceResponse(ValidateDeviceCredentialsResponseMsg msg, ChannelHandlerContext ctx) {
        if (!msg.hasDeviceInfo()) {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
            ctx.close();
        } else {
            deviceSessionCtx.setDeviceInfo(msg.getDeviceInfo());
            sessionInfo = SessionInfoProto.newBuilder()
                    .setNodeId(context.getNodeId())
                    .setSessionIdMSB(sessionId.getMostSignificantBits())
                    .setSessionIdLSB(sessionId.getLeastSignificantBits())
                    .setDeviceIdMSB(msg.getDeviceInfo().getDeviceIdMSB())
                    .setDeviceIdLSB(msg.getDeviceInfo().getDeviceIdLSB())
                    .setTenantIdMSB(msg.getDeviceInfo().getTenantIdMSB())
                    .setTenantIdLSB(msg.getDeviceInfo().getTenantIdLSB())
                    .build();
            transportService.process(sessionInfo, AbstractTransportService.getSessionEventMsg(SessionEvent.OPEN), null);
            transportService.registerAsyncSession(sessionInfo, this);
            checkGatewaySession();
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_ACCEPTED));
            log.info("[{}] Client connected!", sessionId);
        }
    }

    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg response) {
        try {
            adaptor.convertToPublish(deviceSessionCtx, response).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg notification) {
        try {
            adaptor.convertToPublish(deviceSessionCtx, notification).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes update to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session", sessionId);
        processDisconnect(deviceSessionCtx.getChannel());
    }

    @Override
    public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}] Received RPC command to device", sessionId);
        try {
            adaptor.convertToPublish(deviceSessionCtx, rpcRequest).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device RPC commandto MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg rpcResponse) {
        log.trace("[{}] Received RPC command to device", sessionId);
        try {
            adaptor.convertToPublish(deviceSessionCtx, rpcResponse).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device RPC commandto MQTT msg", sessionId, e);
        }
    }
}
