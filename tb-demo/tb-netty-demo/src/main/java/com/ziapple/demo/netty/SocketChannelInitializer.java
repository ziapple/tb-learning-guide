package com.ziapple.demo.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * 一个Socket连接对应一个Channel
 * 当接受了一个新的连接后就需要实例化一个ChannelHandler供后续调用，同时也需要一个Handler来处理消息
 */
public class SocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 添加处理原始Socket消息的Handler
        SocketServerHandler handler = new SocketServerHandler();
        pipeline.addLast(handler);

        // ChannelPipe关闭时执行
        ch.closeFuture().addListener(handler);
    }
}
