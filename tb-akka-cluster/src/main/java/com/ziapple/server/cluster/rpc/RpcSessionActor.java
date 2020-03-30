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
import com.ziapple.server.cluster.ServerInstance;
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
    private ServerInstance currentServer;

    private RpcSessionActor(ActorSystemContext systemContext, UUID sessionId) {
        super(systemContext);
        this.sessionId = sessionId;
        this.currentServer = systemContext.getDiscoveryService().getCurrentServer();
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
        ServerAddress remoteServer = msg.getRemoteAddress();
        listener = new BasicRpcSessionListener(systemContext, context().parent(), context().self());
        if (msg.getRemoteAddress() == null) {// 建立服务端与客户端的双向Session，相对客户端来说，此时的Server相当于Client
            // Server session
            session = new GrpcSession(listener);
            // 直接用ClusterGrpcService的handleMsg的responseObserver参数
            session.setOutputStream(msg.getResponseObserver());
            // 服务端初始化客户端的输入流（读入流）
            session.initInputStream();
            // 服务端初始化发送消息的输出流（写入流）
            session.initOutputStream();
            systemContext.getRpcService().onSessionCreated(msg.getMsgUid(), session.getInputStream());
        } else {// 建立客户端与服务端的Session
            // Client session
            ManagedChannel channel = ManagedChannelBuilder.forAddress(remoteServer.getHost(), remoteServer.getPort()).usePlaintext().build();
            session = new GrpcSession(remoteServer, listener, channel);
            // 初始化输入流,用于接收服务器消息
            session.initInputStream();

            ClusterRpcServiceGrpc.ClusterRpcServiceStub stub = ClusterRpcServiceGrpc.newStub(channel);
            StreamObserver<ClusterAPIProtos.ClusterMessage> outputStream = stub.handleMsgs(session.getInputStream());

            // 初始化输出流，用于给服务器发送消息
            session.setOutputStream(outputStream);
            session.initOutputStream();
            log.info("客户端{}给{}发送Session连接请求", currentServer.getServerAddress(), msg.getRemoteAddress());
            // 给服务器发送消息：CONNECT_RPC_MESSAGE连接请求
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
