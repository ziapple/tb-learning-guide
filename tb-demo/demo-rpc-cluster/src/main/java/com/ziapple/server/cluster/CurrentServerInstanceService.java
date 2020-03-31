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
package com.ziapple.server.cluster;

import lombok.extern.slf4j.Slf4j;
import java.util.Random;


/**
 * 当前RPC服务实例
 * 默认为localhost:9001
 * @author Andrew Shvayka
 */
@Slf4j
public class CurrentServerInstanceService implements ServerInstanceService {
    private String rpcHost = "127.0.0.1";
    private Integer rpcPort = new Random().nextInt(8000);

    private ServerInstance self;

    public CurrentServerInstanceService(){
        init();
    }

    public void init() {
        self = new ServerInstance(new ServerAddress(rpcHost, rpcPort));
    }

    @Override
    public ServerInstance getSelf() {
        return self;
    }
}
