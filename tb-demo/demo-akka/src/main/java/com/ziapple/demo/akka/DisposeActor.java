package com.ziapple.demo.akka;

import akka.actor.UntypedActor;

//定义角色 DisposeMsgActor 消费消息
public class DisposeActor extends UntypedActor {
    public DisposeActor(){
        super();
    }

    @Override
    public void onReceive(Object o) throws Exception {
        //收到消息
        if(o instanceof Msg){
            Msg t = (Msg)o;
            System.out.println(self() + "  receive msg  from " + sender() + ": " + t.getContent());
            System.out.println(self() + " dispose msg : " + t.getContent());
        }

    }
}