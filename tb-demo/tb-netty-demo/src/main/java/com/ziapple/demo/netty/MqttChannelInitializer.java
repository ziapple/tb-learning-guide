package com.ziapple.demo.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;

/**
 * 一个Socket连接对应一个Channel
 * 当接受了一个新的连接后就需要实例化一个ChannelHandler供后续调用，同时也需要一个Handler来处理消息
 */
public class MqttChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 添加处理Mqtt协议消息的Handler
        // 对Mqtt传过来的数据进行解码，把消息转化成MqttMessage对象
        pipeline.addLast("decoder", new MqttDecoder(1024));
        // 对Mqtt的数据进行编码
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);

        // Mqtt消息的真正处理类,处理连接、发布、订阅等请求
        MqttTransportHandler handler = new MqttTransportHandler();
        pipeline.addLast(handler);

        // ChannelPipe关闭时执行
        ch.closeFuture().addListener(handler);
    }
}
