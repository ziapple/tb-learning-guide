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
package com.ziapple.server.cluster.rpc;

import akka.actor.*;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.ServerInstance;
import com.ziapple.server.cluster.ServerType;
import com.ziapple.server.cluster.actor.ActorSystemContext;
import com.ziapple.server.cluster.actor.ContextAwareActor;
import com.ziapple.server.cluster.actor.ContextBasedCreator;
import com.ziapple.server.cluster.actor.DefaultActorService;
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import scala.concurrent.duration.Duration;

import java.util.*;

/**
 * 管理SessionActor的Actor，用于创建，关闭，断开，检查SessionActor
 * @author Andrew Shvayka
 */
public class RpcManagerActor extends ContextAwareActor {
    private final Map<ServerAddress, SessionActorInfo> sessionActors;
    private final Map<ServerAddress, Queue<ClusterAPIProtos.ClusterMessage>> pendingMsgs;
    private final ServerAddress instance;

    private RpcManagerActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.sessionActors = new HashMap<>();
        this.pendingMsgs = new HashMap<>();
        this.instance = systemContext.getDiscoveryService().getCurrentServer().getServerAddress();

        // 初始化RPC连接
        systemContext.getDiscoveryService().getOtherServers().stream()
                .filter(otherServer -> otherServer.getServerAddress().compareTo(instance) > 0)
                .forEach(otherServer -> onCreateSessionRequest(
                        new RpcSessionCreateRequestMsg(UUID.randomUUID(), otherServer.getServerAddress(), null)));
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        //TODO Move everything here, to work with TbActorMsg
        return false;
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof ClusterAPIProtos.ClusterMessage) {
            onMsg((ClusterAPIProtos.ClusterMessage) msg);
        } else if (msg instanceof RpcSessionCreateRequestMsg) {
            onCreateSessionRequest((RpcSessionCreateRequestMsg) msg);
        } else if (msg instanceof RpcSessionConnectedMsg) {
            onSessionConnected((RpcSessionConnectedMsg) msg);
        } else if (msg instanceof RpcSessionDisconnectedMsg) {
            onSessionDisconnected((RpcSessionDisconnectedMsg) msg);
        } else if (msg instanceof RpcSessionClosedMsg) {
            onSessionClosed((RpcSessionClosedMsg) msg);
        }
    }

    private void onMsg(ClusterAPIProtos.ClusterMessage msg) {
        if (msg.hasServerAddress()) {
            ServerAddress address = new ServerAddress(msg.getServerAddress().getHost(), msg.getServerAddress().getPort(), ServerType.CORE);
            SessionActorInfo session = sessionActors.get(address);
            if (session != null) {
                log.debug("{} Forwarding msg to session actor: {}", address, msg);
                session.getActor().tell(msg, ActorRef.noSender());
            } else {//会话不存在，暂存在队列中
                log.debug("{} Storing msg to pending queue: {}", address, msg);
                Queue<ClusterAPIProtos.ClusterMessage> queue = pendingMsgs.get(address);
                if (queue == null) {
                    queue = new LinkedList<>();
                    pendingMsgs.put(new ServerAddress(
                            msg.getServerAddress().getHost(), msg.getServerAddress().getPort(), ServerType.CORE), queue);
                }
                queue.add(msg);
            }
        } else {
            log.warn("Cluster msg doesn't have server address [{}]", msg);
        }
    }

    @Override
    public void postStop() {
        sessionActors.clear();
        pendingMsgs.clear();
    }

    // 服务器反向建立Session连接成功
    private void onSessionConnected(RpcSessionConnectedMsg msg) {
        log.info("服务端{}建立与客户端{}的反向连接成功，保存Session[{}]", instance, msg.getRemoteAddress(), msg.getId());
        register(msg.getRemoteAddress(), msg.getId(), context().sender());
    }

    private void onSessionDisconnected(RpcSessionDisconnectedMsg msg) {
        boolean reconnect = msg.isClient() && isRegistered(msg.getRemoteAddress());
        onSessionClose(reconnect, msg.getRemoteAddress());
    }

    private void onSessionClosed(RpcSessionClosedMsg msg) {
        boolean reconnect = msg.isClient() && isRegistered(msg.getRemoteAddress());
        onSessionClose(reconnect, msg.getRemoteAddress());
    }

    private boolean isRegistered(ServerAddress address) {
        for (ServerInstance server : systemContext.getDiscoveryService().getOtherServers()) {
            if (server.getServerAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从sessionMap里面移除session
     * @param reconnect
     * @param remoteAddress
     */
    private void onSessionClose(boolean reconnect, ServerAddress remoteAddress) {
        log.info("[{}] session closed. Should reconnect: {}", remoteAddress, reconnect);
        SessionActorInfo sessionRef = sessionActors.get(remoteAddress);
        if (sessionRef != null && context().sender() != null && context().sender().equals(sessionRef.actor)) {
            context().stop(sessionRef.actor);
            sessionActors.remove(remoteAddress);
            pendingMsgs.remove(remoteAddress);
            if (reconnect) {
                onCreateSessionRequest(new RpcSessionCreateRequestMsg(sessionRef.sessionId, remoteAddress, null));
            }
        }
    }


    /**
     * 发送Session创建请求，一个remoteAddress，一个sessionId对应一个SessionActor
     * 只是创建Session，还没有真正建立连接
     * @param msg
     */
    private void onCreateSessionRequest(RpcSessionCreateRequestMsg msg) {
        if (msg.getRemoteAddress() != null) {// 客户端创建SessionActor
            log.info("客户端正在建立{}到{}的Session...", instance, msg.getRemoteAddress());
            if (!sessionActors.containsKey(msg.getRemoteAddress())) {
                ActorRef actorRef = createSessionActor(msg);
                log.info("客户端保存{}到{}的会话", instance, msg.getRemoteAddress());
                register(msg.getRemoteAddress(), msg.getMsgUid(), actorRef);
            }
        } else {
            // 服务端创建SessionActor
            createSessionActor(msg);
        }
    }


    /**
     * session放进map里，一个sessionId对应一个sessionActor
     * @param remoteAddress
     * @param uuid
     * @param sender
     */
    private void register(ServerAddress remoteAddress, UUID uuid, ActorRef sender) {
        sessionActors.put(remoteAddress, new SessionActorInfo(uuid, sender));
        Queue<ClusterAPIProtos.ClusterMessage> data = pendingMsgs.remove(remoteAddress);
        if (data != null) {
            data.forEach(msg -> sender.tell(msg, ActorRef.noSender()));
        } else {
        }
    }


    /**
     * 初始化RPCSessionActor
     * @param msg
     * @return
     */
    private ActorRef createSessionActor(RpcSessionCreateRequestMsg msg) {
        ActorRef actor = context().actorOf(
                Props.create(new RpcSessionActor.ActorCreator(systemContext, msg.getMsgUid()))
                        .withDispatcher(DefaultActorService.RPC_DISPATCHER_NAME));
        actor.tell(msg, context().self());
        return actor;
    }

    public static class ActorCreator extends ContextBasedCreator<RpcManagerActor> {
        private static final long serialVersionUID = 1L;

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public RpcManagerActor create() {
            return new RpcManagerActor(context);
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), t -> {
        log.warn("Unknown failure", t);
        return SupervisorStrategy.resume();
    });
}
