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
package com.ziapple.transport.api.service;


import com.ziapple.server.kafka.TbKafkaEncoder;
import com.ziapple.transport.TransportProtos;

/**
 * Created by ashvayka on 05.10.18.
 */
public class ToRuleEngineMsgEncoder implements TbKafkaEncoder<TransportProtos.ToRuleEngineMsg> {
    @Override
    public byte[] encode(TransportProtos.ToRuleEngineMsg value) {
        return value.toByteArray();
    }
}
