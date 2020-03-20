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

import com.google.protobuf.ByteString;
import com.ziapple.server.cluster.ServerAddress;
import com.ziapple.server.cluster.ServerInstance;
import com.ziapple.server.cluster.ServerInstanceService;
import com.ziapple.server.cluster.actor.DefaultActorService;
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import com.ziapple.server.gen.cluster.ClusterRpcServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 启动RPC服务
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class ClusterGrpcService extends ClusterRpcServiceGrpc.ClusterRpcServiceImplBase implements ClusterRpcService {
    // 本地服务实例
    @Autowired
    private ServerInstanceService instanceService;

    // 消息payload字节码编码服务
    @Autowired
    private DataDecodingEncodingService encodingService;

    // RPC消息监听器，用于接收发消息
    private RpcMsgListener listener;

    // RPC的Server对象
    private Server server;

    // 本地服务器实例
    private ServerInstance instance;

    //
    private ConcurrentMap<UUID, BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>>> pendingSessionMap =
            new ConcurrentHashMap<>();

    /**
     * 初始化本地RPC服务
     * @param listener
     */
    public void init(RpcMsgListener listener) {
        this.listener = listener;
        log.info("Initializing RPC service!");
        instance = instanceService.getSelf();
        server = ServerBuilder.forPort(instance.getPort()).addService(this).build();
        log.info("Going to start RPC server using port: {}", instance.getPort());
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start RPC server!", e);
            throw new RuntimeException("Failed to start RPC server!");
        }
        log.info("RPC service initialized!");
    }

    @Override
    public void onSessionCreated(UUID msgUid, StreamObserver<ClusterAPIProtos.ClusterMessage> inputStream) {
        BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>> queue = pendingSessionMap.remove(msgUid);
        if (queue != null) {
            try {
                queue.put(inputStream);
            } catch (InterruptedException e) {
                log.warn("Failed to report created session!");
                Thread.currentThread().interrupt();
            }
        } else {
            log.warn("Failed to lookup pending session!");
        }
    }

    /**
     * RPC消息处理方法，在cluster.proto中定义，所有集群消息的处理方法入口！
     * 1. 所有消息初始化一个消息唯一Id
     * 2. 创建Session会话
     * @param responseObserver 消息流
     * @return
     */
    @Override
    public StreamObserver<ClusterAPIProtos.ClusterMessage> handleMsgs(StreamObserver<ClusterAPIProtos.ClusterMessage> responseObserver) {
        log.info("Processing new session.");
        return createSession(new RpcSessionCreateRequestMsg(UUID.randomUUID(), null, responseObserver));
    }


    @PreDestroy
    public void stop() {
        if (server != null) {
            log.info("Going to onStop RPC server");
            server.shutdownNow();
            try {
                server.awaitTermination();
                log.info("RPC server stopped!");
            } catch (InterruptedException e) {
                log.warn("Failed to onStop RPC server!");
                Thread.currentThread().interrupt();
            }
        }
    }


    @Override
    public void broadcast(RpcBroadcastMsg msg) {
        listener.onBroadcastMsg(msg);
    }

    /**
     * 接受消息
     * @param msg  消息体
     * @return
     */
    private StreamObserver<ClusterAPIProtos.ClusterMessage> createSession(RpcSessionCreateRequestMsg msg) {
        BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>> queue = new ArrayBlockingQueue<>(1);
        pendingSessionMap.put(msg.getMsgUid(), queue);
        listener.onRpcSessionCreateRequestMsg(msg);
        try {
            StreamObserver<ClusterAPIProtos.ClusterMessage> observer = queue.take();
            log.info("Processed new session.");
            return observer;
        } catch (Exception e) {
            log.info("Failed to process session.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送消息
     * @param message 消息
     */
    @Override
    public void tell(ClusterAPIProtos.ClusterMessage message) {
        listener.onSendMsg(message);
    }

    /**
     *
     * @param serverAddress 目标地址
     * @param actorMsg      Actor消息
     */
    @Override
    public void tell(ServerAddress serverAddress, TbActorMsg actorMsg) {
        listener.onSendMsg(encodingService.convertToProtoDataMessage(serverAddress, actorMsg));
    }

    /**
     * 发送消息
     * @param serverAddress   目标地址
     * @param msgType         消息类型
     * @param data            消息字节码
     */
    @Override
    public void tell(ServerAddress serverAddress, ClusterAPIProtos.MessageType msgType, byte[] data) {
        ClusterAPIProtos.ClusterMessage msg = ClusterAPIProtos.ClusterMessage
                .newBuilder()
                .setServerAddress(com.ziapple.server.gen.cluster.ClusterAPIProtos.ServerAddress
                        .newBuilder()
                        .setHost(serverAddress.getHost())
                        .setPort(serverAddress.getPort())
                        .build())
                .setMessageType(msgType)
                .setPayload(ByteString.copyFrom(data)).build();
        listener.onSendMsg(msg);
    }
}
