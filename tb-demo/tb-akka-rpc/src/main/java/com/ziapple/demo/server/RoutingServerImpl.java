package com.ziapple.demo.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟Cluster路由服务
 */
public class RoutingServerImpl implements RoutingServer{
    private Logger logger = LoggerFactory.getLogger(RoutingServerImpl.class);
    // 集群列表
    public List<ClusterAPIProtos.ServerAddress> serverAdresses = new ArrayList<>();

    /**
     * 根据业务Id获取对应处理的服务器
     * @param entityId
     * @return
     */
    public ClusterAPIProtos.ServerAddress getRPCServer(int entityId){
        // 模拟负载均衡算法，根据实体Id分配服务器，采用轮询的方法
        ClusterAPIProtos.ServerAddress serverAddress =  serverAdresses.get(entityId % 2);
        //logger.info("选择服务器为{}", serverAddress);
        return serverAddress;
    }

    /**
     * 注册服务器
     * @param serverAddress
     */
    @Override
    public void regist(ClusterAPIProtos.ServerAddress serverAddress) {
        serverAdresses.add(serverAddress);
    }
}
