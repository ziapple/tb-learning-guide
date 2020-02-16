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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslHandler;

/**
 * @author Andrew Shvayka
 * Mqtt服务端ChannlePipe初始化
 * - 增加消息解码MqttDecoder的Handler，把消息转化成MqttMessage
 *   - 客户端发给服务端消息，会首先调用MqttDecoder对消息进行解码
 *   - MqttDecoder继承了ChannelInboundHandlerAdapter，实现了ChannelInboundHandler接口
 * - 增加消息编码的MqttEncoder的Handler，把MqttMessage转换成网络上的消息
 *   - 当服务端给客户端发消息，即调用ctx.write(msg)的时候，把MqttMessage转化成消息数据
 *   - MqttEncoder继承了ChannelOutboundHandlerAdapter，实现了ChannelOutboundHandler接口
 * - 增加消息处理的真正处理类，{@link MqttTransportHandler}
 *
 * Mqtt协议
 * - Mqtt协议按照fixedHeader固定头（一个字节），variableHeader可变头（0~10个字节），payload消息负载（消息内容）组成
 * - fixedHeader控制指令，例如连接请求CONN，连接请求反馈CONNBACK，发送消息PUBLISH等10个指令组成
 * - variableHeader是对固定头的扩展，用于描述消息内容，包括一些Qos、消息标识符（Message Identitier），是否含有用户名，密码等
 * - payload消息体
 */
public class MqttTransportServerInitializer extends ChannelInitializer<SocketChannel> {

    // Mqtt服务上下文，一个连接会创建一个Channel，对应一个上下文
    private final MqttTransportContext context;

    public MqttTransportServerInitializer(MqttTransportContext context) {
        this.context = context;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // 支持ssl数字证书协议
        if (context.getSslHandlerProvider() != null) {
            SslHandler sslHandler = context.getSslHandlerProvider().getSslHandler();
            pipeline.addLast(sslHandler);
            context.setSslHandler(sslHandler);
        }
        // 对Mqtt传过来的数据进行解码，把消息转化成MqttMessage对象
        pipeline.addLast("decoder", new MqttDecoder(context.getMaxPayloadSize()));
        // 对Mqtt的数据进行编码
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);

        // Mqtt消息的真正处理类,处理连接、发布、订阅等请求
        MqttTransportHandler handler = new MqttTransportHandler(context);
        pipeline.addLast(handler);

        // ChannelPipe关闭时执行
        ch.closeFuture().addListener(handler);
    }

}
