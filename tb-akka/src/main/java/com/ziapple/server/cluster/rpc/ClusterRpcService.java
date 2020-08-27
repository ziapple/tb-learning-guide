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
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
public interface ClusterRpcService {
    // 初始化RPC集群
    void init(RpcMsgListener listener);

    // 集群广播消息
    void broadcast(RpcBroadcastMsg msg);

    // 服务器间建立会话
    void onSessionCreated(UUID msgUid, StreamObserver<ClusterAPIProtos.ClusterMessage> inputStream);

    // 服务器通讯，集群消息
    void tell(ClusterAPIProtos.ClusterMessage message);

    // 服务器通讯，Actor消息，转化成集群消息后发送
    void tell(ServerAddress serverAddress, TbActorMsg actorMsg);

    // 服务器通讯，底层消息发送
    void tell(ServerAddress serverAddress, ClusterAPIProtos.MessageType msgType, byte[] data);
}
