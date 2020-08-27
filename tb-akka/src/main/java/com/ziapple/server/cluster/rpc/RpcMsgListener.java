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
import com.ziapple.server.gen.cluster.ClusterAPIProtos;

/**
 * 整个RPC集群消息监听器，监听收发消息
 * @author Andrew Shvayka
 */

public interface RpcMsgListener {
    // 收到消息
    void onReceivedMsg(ServerAddress remoteServer, ClusterAPIProtos.ClusterMessage msg);

    // 发送消息
    void onSendMsg(ClusterAPIProtos.ClusterMessage msg);

    // RPC会话请求消息
    void onRpcSessionCreateRequestMsg(RpcSessionCreateRequestMsg msg);

    // 广播消息
    void onBroadcastMsg(RpcBroadcastMsg msg);
}
