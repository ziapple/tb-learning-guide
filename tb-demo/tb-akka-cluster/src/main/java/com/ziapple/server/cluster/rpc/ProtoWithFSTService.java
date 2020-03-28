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
import com.ziapple.server.cluster.msg.TbActorMsg;
import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.ziapple.server.gen.cluster.ClusterAPIProtos.MessageType.CLUSTER_ACTOR_MESSAGE;


@Slf4j
@Service
public class ProtoWithFSTService implements DataDecodingEncodingService {
    private final FSTConfiguration config = FSTConfiguration.createDefaultConfiguration();

    /**
     * 字节码转换成TbActorMsg
     * @param byteArray
     * @return
     */
    @Override
    public Optional<TbActorMsg> decode(byte[] byteArray) {
        try {
            TbActorMsg msg = (TbActorMsg) config.asObject(byteArray);
            return Optional.of(msg);

        } catch (IllegalArgumentException e) {
            log.error("Error during deserialization message, [{}]", e.getMessage());
           return Optional.empty();
        }
    }

    /**
     * TbActoMsg转换成字节码
     * @param msq
     * @return
     */
    @Override
    public byte[] encode(TbActorMsg msq) {
        return config.asByteArray(msq);
    }

    /**
     * TbActorMsg转换为ClusterMessage用于集群通讯
     * @param serverAddress
     * @param msg
     * @return
     */
    @Override
    public ClusterAPIProtos.ClusterMessage convertToProtoDataMessage(ServerAddress serverAddress,
                                                                     TbActorMsg msg) {
        return ClusterAPIProtos.ClusterMessage
                .newBuilder()
                .setServerAddress(ClusterAPIProtos.ServerAddress
                        .newBuilder()
                        .setHost(serverAddress.getHost())
                        .setPort(serverAddress.getPort())
                        .build())
                .setMessageType(CLUSTER_ACTOR_MESSAGE)
                .setPayload(ByteString.copyFrom(encode(msg))).build();

    }
}
