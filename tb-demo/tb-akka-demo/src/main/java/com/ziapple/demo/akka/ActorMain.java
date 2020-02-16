package com.ziapple.demo.akka;

import akka.actor.*;
import akka.routing.BalancingPool;


/**
 * Actor能够做到：
 * 对并发/并行程序的简单的、高级别的抽象。
 * 异步、非阻塞、高性能的事件驱动编程模型。
 * 非常轻量的事件驱动处理（1G内存可容纳约270万个actors）。
 *
 * 举个例子
 * 汽车生产分为底盘、发动机、电器设备等工艺，正常情况下一条生产线只能生产一个型号
 * 如果需要利用一条生产线生成多种汽车型号，并行生产，利用Actoriu可以，不同的底盘Actor、发动机Actor等可以生产出不同型号的底盘、发动机
 */
public class ActorMain {

    public static void main(String[] args) {
        //生成角色系统
        ActorSystem system = ActorSystem.create("msgSystem");

        //生成角色 ProduceMsgActor
        ActorRef produceMsgActor = system.actorOf(new BalancingPool(3).props(Props.create(ProduceMsgActor.class)), "ProduceMsgActor");

        //生成角色 DisposeMsgActor
        ActorRef disposeMsgActor = system.actorOf(new BalancingPool(2).props(Props.create(DisposeMsgActor.class)), "DisposeMsgActor");

        //给produceMsgActor发消息请求,noSender表示没有发送者，属于系统发送的消息
        produceMsgActor.tell("please produce msg1", ActorRef.noSender());

    }
}