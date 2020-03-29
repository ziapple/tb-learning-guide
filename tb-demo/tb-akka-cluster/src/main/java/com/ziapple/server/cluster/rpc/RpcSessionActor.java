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

import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.actor.ActorSystemContext;
import com.ziapple.server.cluster.actor.ContextAwareActor;
import com.ziapple.server.cluster.actor.ContextBasedCreator;
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import com.ziapple.server.gen.cluster.ClusterRpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import static com.ziapple.server.gen.cluster.ClusterAPIProtos.MessageType.CONNECT_RPC_MESSAGE;

/**
 * @author Andrew Shvayka
 * 初始化RPC的Session连接
 */
@Slf4j
public class RpcSessionActor extends ContextAwareActor {
    private final UUID sessionId;
    private GrpcSession session;
    private GrpcSessionListener listener;

    private RpcSessionActor(ActorSystemContext systemContext, UUID sessionId) {
        super(systemContext);
        this.sessionId = sessionId;
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        //TODO Move everything here, to work with TbActorMsg
        return false;
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof ClusterAPIProtos.ClusterMessage) {
            tell((ClusterAPIProtos.ClusterMessage) msg);
        } else if (msg instanceof RpcSessionCreateRequestMsg) {
            initSession((RpcSessionCreateRequestMsg) msg);
        }
    }

    private void tell(ClusterAPIProtos.ClusterMessage msg) {
        if (session != null) {
            session.sendMsg(msg);
        } else {
            log.trace("Failed to send message due to missing session!");
        }
    }

    @Override
    public void postStop() {
        if (session != null) {
            log.info("Closing session -> {}", session.getRemoteServer());
            try {
                session.close();
            } catch (RuntimeException e) {
                log.trace("Failed to close session!", e);
            }
        }
    }

    private void initSession(RpcSessionCreateRequestMsg msg) {
        log.info("[{}] Initializing session", context().self());
        ServerAddress remoteServer = msg.getRemoteAddress();
        listener = new BasicRpcSessionListener(systemContext, context().parent(), context().self());
        if (msg.getRemoteAddress() == null) {
            // Server session
            session = new GrpcSession(listener);
            session.setOutputStream(msg.getResponseObserver());
            session.initInputStream();
            session.initOutputStream();
            systemContext.getRpcService().onSessionCreated(msg.getMsgUid(), session.getInputStream());
        } else {
            // Client session
            ManagedChannel channel = ManagedChannelBuilder.forAddress(remoteServer.getHost(), remoteServer.getPort()).usePlaintext().build();
            session = new GrpcSession(remoteServer, listener, channel);
            session.initInputStream();

            ClusterRpcServiceGrpc.ClusterRpcServiceStub stub = ClusterRpcServiceGrpc.newStub(channel);
            StreamObserver<ClusterAPIProtos.ClusterMessage> outputStream = stub.handleMsgs(session.getInputStream());

            session.setOutputStream(outputStream);
            session.initOutputStream();
            // 给服务端发送建立连接的请求消息，调用ClusterGrpService.handleMsg方法
            outputStream.onNext(toConnectMsg());
        }
    }

    public static class ActorCreator extends ContextBasedCreator<RpcSessionActor> {
        private static final long serialVersionUID = 1L;

        private final UUID sessionId;

        public ActorCreator(ActorSystemContext context, UUID sessionId) {
            super(context);
            this.sessionId = sessionId;
        }

        @Override
        public RpcSessionActor create() {
            return new RpcSessionActor(context, sessionId);
        }
    }

    private ClusterAPIProtos.ClusterMessage toConnectMsg() {
        ServerAddress instance = systemContext.getDiscoveryService().getCurrentServer().getServerAddress();
        return ClusterAPIProtos.ClusterMessage.newBuilder().setMessageType(CONNECT_RPC_MESSAGE).setServerAddress(
                ClusterAPIProtos.ServerAddress.newBuilder().setHost(instance.getHost())
                        .setPort(instance.getPort()).build()).build();
    }
}
