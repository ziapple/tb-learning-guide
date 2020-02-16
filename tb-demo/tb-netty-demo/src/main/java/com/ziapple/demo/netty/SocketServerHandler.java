package com.ziapple.demo.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocketServerHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            printMsg(msg);
            //ctx不能关闭，关闭handler无法接受到消息
            //ctx.close();
        } finally {
            //buf释放，否则不会处理第二条消息
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    public void printMsg(Object msg){
        ByteBuf buf = (ByteBuf)msg;
        byte[] barray = new byte[buf.readableBytes()];
        buf.getBytes(0, barray);
        String str = new String(barray);
        log.trace("receive msg: {}", str);
    }

    /**
     * socket连接关闭时，线程释放，调用此方法
     * @param future
     * @throws Exception
     */
    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        log.trace("message tread done");
    }
}
