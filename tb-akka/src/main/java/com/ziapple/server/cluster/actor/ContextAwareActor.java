/**
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
package com.ziapple.server.cluster.actor;

import akka.actor.Terminated;
import akka.actor.UntypedActor;
import com.ziapple.server.cluster.msg.TbActorMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 包含了Actor上下文的抽象类，所有用户的Actor继承此类
 * Actor子类必须实现process方法，对Actor消息进行处理
 */
public abstract class ContextAwareActor extends UntypedActor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final int ENTITY_PACK_LIMIT = 1024;

    protected final ActorSystemContext systemContext;

    public ContextAwareActor(ActorSystemContext systemContext) {
        super();
        this.systemContext = systemContext;
    }

    /**
     * Actor消息处理入口
     * @param msg
     */
    @Override
    public void onReceive(Object msg) {
        if (log.isDebugEnabled()) {
            log.debug("Processing msg: {}", msg);
        }
        if (msg instanceof TbActorMsg) {
            try {
                if (!process((TbActorMsg) msg)) {
                    log.warn("Unknown message: {}!", msg);
                }
            } catch (Exception e) {
                throw e;
            }
        } else if (msg instanceof Terminated) {
            processTermination((Terminated) msg);
        } else {
            log.warn("Unknown message: {}!", msg);
        }
    }

    protected void processTermination(Terminated msg) {
    }

    protected abstract boolean process(TbActorMsg msg);
}
