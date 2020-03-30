package com.ziapple.demo.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Andrew Shvayka
 * 一、Netty服务创建步骤：
 * 1. 创建ServerBootstrap实例来引导绑定和启动服务器；
 * 2. 创建NioEventLoopGroup对象来处理事件，如接受新连接、接受数据、写数据等等；
 * 3. 指定InetSocketAddress，服务器监听此端口；
 * 4. 设置childHandler执行所有的连接请求；
 * 5. 都设置完毕了，最后低啊用ServerBootstrap。bind()方法来绑定服务器
 *
 * 二、Netty三大对象ChannelPipeline | ChannelHandler | ChannelHandlerContext
 * 1. 一个Socket连接，会创建一个ChannelPipeline
 * 2. 一个ChannelPipeline通过addLast添加多个ChannelHandler
 * 3. 一个ChannelHandler对应一个ChannelHandlerContext
 * 4. ChannelPipeLine
 *    # 当一个请求进来的时候，会进入 Socket 对应的 pipeline，并流经 pipeline 所有的 handler, 跟ServletFilter采用的模式一样，Filter模式
 *    # 可以想象ChannelPipeline是一个ChannelHandler的一个LinkedList列表
 * 5. ChannelHandler
 *    # IO 事件由 ChannelInboundHandler，用来read管道IO数据 或者 ChannelOutboundHandler 处理，用来write管道数据
 *    # ChannelInboundHandler接口最主要的方法是重写channelRead,当pipe有数据的时候会通知（event事件机制）该方法
 *    # ChannelOutboundHandler类似
 * 6. ChannelHandlerContext
 *    # ChannelHandlerContext类似于ServletContext，记录对应的比如 channel，executor，handler ，pipeline
 *    * 不同的是Context还承担了Filter功能，如果执行下一个Handler，调用ctx.fireChannelRead(),类似于Filter.doFilter();
 * 三、Handler调用顺序
 * 1. 在netty 的 DefaultChannelPipeline中是用了双向链表放置 ，这些 handler不管是什么handler, Inbound, outBound, Duplex双向的，
 * 2. 都是放置在AbstractChannelHandlerContext的双向链表(DefaultChannelPipeline持有head, tail).
 * 3. handler添加顺序就是链表里顺序
 * 3. (head)InboundsHandler1()<--->InboundsHandler2()<--->outboundsHandler1()<--->outboundsHandler2() (tail)
 * 4. pipeline有read到数据后，是从head往后查找 有in性质的handler，
 * 5. pipeline有write数据是，是从 tail往前查找 有out性质的handler
 *
 */
@Slf4j
public class NettyMqttServer {
    public static void main(String[] args) throws InterruptedException {
        Channel serverChannel;
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;

        //设置服务端Netty内存读写泄漏级别，默认为disabled
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf("DISABLED"));

        log.info("Starting MQTT transport...");
        //主线程组，接收网络请求
        bossGroup = new NioEventLoopGroup(10);
        //worker线程组，对接收到的请求进行读写处理
        workerGroup = new NioEventLoopGroup(10);
        //启动服务的启动类（辅助类）
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)  // 添加主线程组和worker线程组
                .channel(NioServerSocketChannel.class) //设置channel为服务端NioServerSocketChannel
                // 当接受了一个新的连接后就需要实例化一个ChannelHandler供后续调用，同时也会创建一个子通道。这里会使用ChannelInitializer
                // ChannelHandler核心对象之一初始化,只能有一个
                .childHandler(new MqttChannelInitializer())
                .childOption(ChannelOption.SO_KEEPALIVE, false);  //是否保持连接,默认为false

        //绑定端口，同步等待成功
        serverChannel = b.bind("127.0.0.1",  1884).sync().channel();
        log.info("Mqtt transport started!");
    }
}
