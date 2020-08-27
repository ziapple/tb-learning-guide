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
import com.ziapple.server.cluster.ServerType;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 * RPC之间的会话
 * GrpcSession作为客户端，负责维护和服务器的输入和输出流
 * 通过{@code RpcSessionActor}建立Session
 */
@Data
@Slf4j
public final class GrpcSession implements Closeable {
    private final UUID sessionId;
    private final boolean client;
    private final GrpcSessionListener listener;
    private final ManagedChannel channel;
    // 接收服务端反馈的输入流（读入流），onNext方法用于接收消息
    private StreamObserver<ClusterAPIProtos.ClusterMessage> inputStream;
    // 给服务端发消息的输出流（写入流）,onNext方法用于发送消息
    private StreamObserver<ClusterAPIProtos.ClusterMessage> outputStream;

    private boolean connected;
    private ServerAddress remoteServer;

    public GrpcSession(GrpcSessionListener listener) {
        this(null, listener, null);
    }

    public GrpcSession(ServerAddress remoteServer, GrpcSessionListener listener, ManagedChannel channel) {
        this.sessionId = UUID.randomUUID();
        this.listener = listener;
        if (remoteServer != null) {
            this.client = true;
            this.connected = true;
            this.remoteServer = remoteServer;
        } else {
            this.client = false;
        }
        this.channel = channel;
    }

    // 读取服务端响应的输入流
    public void initInputStream() {
        this.inputStream = new StreamObserver<ClusterAPIProtos.ClusterMessage>() {
            @Override
            public void onNext(ClusterAPIProtos.ClusterMessage clusterMessage) {
                // 服务器反向建立连接的消息
                if (!connected && clusterMessage.getMessageType() == ClusterAPIProtos.MessageType.CONNECT_RPC_MESSAGE) {
                    connected = true;
                    ServerAddress rpcAddress = new ServerAddress(clusterMessage.getServerAddress().getHost(), clusterMessage.getServerAddress().getPort(), ServerType.CORE);
                    remoteServer = new ServerAddress(rpcAddress.getHost(), rpcAddress.getPort(), ServerType.CORE);
                    // 告诉监听者反向连接成功，保存服务端和客户端之间的Session
                    listener.onConnected(GrpcSession.this);
                }
                if (connected) {// 如果连接成功，交给监听者接收服务器消息
                    listener.onReceiveClusterGrpcMsg(GrpcSession.this, clusterMessage);
                }
            }

            @Override
            public void onError(Throwable t) {
                listener.onError(GrpcSession.this, t);
            }

            @Override
            public void onCompleted() {
                outputStream.onCompleted();
                listener.onDisconnected(GrpcSession.this);
            }
        };
    }

    // 初始化输出流，告诉对方（服务端），连接成功
    public void initOutputStream() {
        if (client) {
            listener.onConnected(GrpcSession.this);
        }
    }

    // 给服务器发消息，调用outputStream.onNext方法
    public void sendMsg(ClusterAPIProtos.ClusterMessage msg) {
        if (connected) {
            try {
                outputStream.onNext(msg);
            } catch (Throwable t) {
                try {
                    outputStream.onError(t);
                } catch (Throwable t2) {
                }
                listener.onError(GrpcSession.this, t);
            }
        } else {
            log.warn("[{}] Failed to send message due to closed session!", sessionId);
        }
    }

    // 与服务器断开连接
    @Override
    public void close() {
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (IllegalStateException e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
