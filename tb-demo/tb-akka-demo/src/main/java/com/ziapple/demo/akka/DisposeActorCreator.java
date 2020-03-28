package com.ziapple.demo.akka;

import akka.japi.Creator;

//定义角色 ProduceMsgActor  产生消息
public class DisposeActorCreator implements Creator {
    @Override
    public DisposeActor create() throws Exception {
        return new DisposeActor();
    }
}