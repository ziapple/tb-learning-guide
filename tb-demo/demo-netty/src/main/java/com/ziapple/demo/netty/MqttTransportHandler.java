package com.ziapple.demo.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.UUID;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
import static io.netty.handler.codec.mqtt.MqttMessageType.CONNACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.PUBACK;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;


@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {
    // 一个连接生成一个随机的会话Id
    private UUID sessionId;

    MqttTransportHandler(){
        this.sessionId = UUID.randomUUID();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        log.info("[{}:{}] remote message received", address.getHostName(), address.getPort());
        try {
            if (msg instanceof MqttMessage) {
                MqttMessage mqttMessage = (MqttMessage) msg;
                log.trace("receive mqtt msg [{}]:{}", sessionId, mqttMessage.fixedHeader().messageType());
                processMqttMsg(ctx, mqttMessage);
            }else{
                ctx.close();
            }
        } finally {
            //buf释放，否则不会处理第二条消息
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        log.trace("message tread done");
    }

    /**
     * 处理Mqtt消息
     * @param ctx
     * @param msg
     */
    private void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        // 报文固定头为空，直接断开连接
        if (msg.fixedHeader() == null) {
            processDisconnect(ctx);
            return;
        }
        switch (msg.fixedHeader().messageType()) {
            case CONNECT:  //处理连接请求
                processConnect(ctx, (MqttConnectMessage) msg);
                break;
            case PUBLISH:  //处理发布请求
                processPublish(ctx, (MqttPublishMessage) msg);
                break;
            default:
                break;
        }
    }

    /**
     * 处理Mqtt连接请求
     * @param ctx
     * @param msg
     */
    private void processConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        String userName = msg.payload().userName();
        log.info("[{}] Processing connect msg for client with user name: {}!", sessionId, userName);
        // 用户名为空（token字符串，含用户名），拒绝连接
        if (userName == null) {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD));
        } else {// 接受连接请求
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_ACCEPTED));
        }
    }

    /**
     * 处理消息发布请求
     * @param ctx
     * @param mqttMsg
     */
    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            return;
        }
        // 从消息可变头中获取消息主题
        String topicName = mqttMsg.variableHeader().topicName();
        int msgId = mqttMsg.variableHeader().packetId();
        log.trace("[{}][{}] Processing publish msg [{}][{}]!", sessionId, topicName, msgId);
        // 处理过程，此处简化，Thingsboard会交给Akka处理
        System.out.println("接受到发布消息，开始处理 + " + mqttMsg.payload());
        // 消息处理完后返回回执
        ctx.writeAndFlush(createMqttPubAckMsg(msgId));
    }

    /**
     * 检查是否已连接
     * @param ctx
     * @param msg
     * @return
     */
    private boolean checkConnected(ChannelHandlerContext ctx, MqttMessage msg) {
        if (sessionId != null) {
            return true;
        } else {
            log.info("[{}] Closing current session due to invalid msg order: {}", sessionId, msg);
            ctx.close();
            return false;
        }
    }

    /**
     * 客户端Mqtt连接（CONNECT）回执：接收或者拒绝
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

    /**
     * Mqtt发布请求（PUBLISH）回执
     * @param requestId
     * @return
     */
    public static MqttPubAckMessage createMqttPubAckMsg(int requestId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(PUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader =
                MqttMessageIdVariableHeader.from(requestId);
        return new MqttPubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader);
    }

    /**
     * 断开连接
     * @param ctx
     */
    private void processDisconnect(ChannelHandlerContext ctx) {
        sessionId = null;
        ctx.close();
        log.info("[{}] Client disconnected!", sessionId);
    }
}
