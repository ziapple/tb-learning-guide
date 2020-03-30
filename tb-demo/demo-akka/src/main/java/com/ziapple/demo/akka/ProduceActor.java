package com.ziapple.demo.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;

//定义角色 ProduceMsgActor  产生消息
public class ProduceActor extends UntypedActor {
    @Override
    public void onReceive(Object o) throws Exception {
        //收到消息
        System.out.println(self() + "  receive msg  from " + sender() + ": " + o);

        //发送消息请求
        Msg msg = new Msg("haha");
        //根据路径查找下一个处理者
        ActorSelection nextDisposeRefs = getContext().actorSelection("/user/DisposeMsgActor");
        //将消息发给下一个处理者DisposeMsgActor
        nextDisposeRefs.tell(msg, self());

        ActorRef ref = context().actorOf(Props.create(DisposeActor.class));
        ref.tell(new Msg("hello parent"), self());
    }
}