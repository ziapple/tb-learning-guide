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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 模拟的服务调度器
 */
@Slf4j
public class DummyDiscoveryService implements DiscoveryService {
    private static String FILE_PATH = "/cluster";
    private ServerInstanceService serverInstance;

    public static void clear(){
        // check file exit
        String path = DummyDiscoveryService.class.getResource("/").getPath();
        path = path.substring(0, path.indexOf("/target")) + FILE_PATH;
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void init() {
        serverInstance = new CurrentServerInstanceService();
        log.info("正在注册当前服务到集群...{}", serverInstance.getSelf().getServerAddress().toString());

        // check file exit
        String path = DummyDiscoveryService.class.getResource("/").getPath();
        path = path.substring(0, path.indexOf("/target")) + FILE_PATH;
        File file = new File(path);
        try{
            if (!file.exists()) {
                file.createNewFile();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        publishCurrentServer();
    }

    /**
     * 发布当前服务到文件
     */
    @Override
    public void publishCurrentServer() {
        try {
            String path = DummyDiscoveryService.class.getResource("/").getPath();
            path = path.substring(0, path.indexOf("/target")) + FILE_PATH;
            BufferedWriter out = new BufferedWriter(new FileWriter(path, true));
            out.append(serverInstance.getSelf().getHost() + ":" + serverInstance.getSelf().getPort() + "\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unpublishCurrentServer() {
        //Do nothing
    }

    @Override
    public ServerInstance getCurrentServer() {
        return serverInstance.getSelf();
    }

    /**
     * 读取集群服务器
     * @return
     */
    @Override
    public List<ServerInstance> getOtherServers() {
        List<ServerInstance> list = new ArrayList<>();
        try {
            String path = DummyDiscoveryService.class.getResource("/").getPath();
            path = path.substring(0, path.indexOf("/target")) + FILE_PATH;
            BufferedReader in = new BufferedReader(new FileReader(path));
            String str = null;
            while((str = in.readLine()) != null){//按行读取
                list.add(new ServerInstance(new ServerAddress(
                        str.substring(0, str.indexOf(":")),
                        Integer.valueOf(str.substring(str.indexOf(":") + 1)))));
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }
}
