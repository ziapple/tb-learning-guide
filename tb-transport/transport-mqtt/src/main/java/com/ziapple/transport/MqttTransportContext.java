package com.ziapple.transport; /**
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

import com.ziapple.transport.adaptors.MqttTransportAdaptor;
import com.ziapple.transport.api.TransportContext;
import io.netty.handler.ssl.SslHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;


/**
 * Created by ashvayka on 04.10.18.Ø
 * 一个Socket连接对应一个Mqtt服务上下文
 * 1. 包装SSL协议提供器MqttSslHandlerProvider{@link com.ziapple.transport.MqttSslHandlerProvider}
 * 2. 包装Mqtt消息转化成Akka格式的适配器{@link com.ziapple.transport.adaptors.MqttTransportAdaptor }，从Json->MqttMessage->Akka的Proto格式
 * 3. 限制了最大消息内容大小maxPayloadSize,默认为65536Bytes=64KB
 * 4. 包装了由MqttSslHandlerProvider提供的sslHandler
 */
@Slf4j
@ConditionalOnExpression("'${transport.type:null}'=='null' || ('${transport.type}'=='local' && '${transport.mqtt.enabled}'=='true')")
@Component
public class MqttTransportContext extends TransportContext {

    @Getter
    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    @Getter
    @Autowired
    private MqttTransportAdaptor adaptor;

    @Getter
    @Value("${transport.mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;

    @Getter
    @Setter
    private SslHandler sslHandler;

}
