package com.ziapple.rpc.server;

import com.ziapple.server.gen.cluster.ClusterAPIProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 集群服务
 */
public class ClusterService {
    private static Logger logger = LoggerFactory.getLogger(ClusterService.class);
    private DiscoveryService discoveryService;
    private RoutingServer routingServer;
    private RpcService rpcService;
    private ServerInstance currentServerInstance;
    private Map<ServerAddress, RpcSession> rpcSessions = new HashMap<>();

    public void init(){
        // 注册当前服务器
        DiscoveryService discoveryService = new DummyDiscoveryService();
        this.discoveryService = discoveryService;
        discoveryService.init();
        currentServerInstance = discoveryService.getCurrentServer();
        routingServer = new RoutingServerImpl(discoveryService);

        // 启动RPC服务
        rpcService = new GRpcService(currentServerInstance);
        rpcService.start();
    }

    /**
     * 初始化本地服务器和其他服务器Session
     */
    public void initSession(){
        // 初始化Session
        discoveryService.getOtherServers().stream()
                .filter(otherServer -> otherServer.getServerAddress().compareTo(currentServerInstance.getServerAddress()) > 0)
                .forEach(otherServer -> createSession(UUID.randomUUID(), otherServer.getServerAddress()));
    }

    /**
     * 建立本地服务器和其他服务器的Session通道
     */
    public void createSession(UUID sessionId, ServerAddress serverAddress){
        RpcSessionImpl rpcSession = new RpcSessionImpl(sessionId, serverAddress);
        rpcSession.initSession();
        rpcSessions.put(serverAddress, rpcSession);
    }

    public void tell(int entityId){
        Optional<ServerAddress> result = routingServer.resolveById(entityId);
        ClusterAPIProtos.ClusterMessage clusterMessage = ClusterAPIProtos.ClusterMessage.newBuilder()
                .setMessageType(ClusterAPIProtos.MessageType.CLUSTER_TRANSFER_MESSAGE)
                .setPayload("######" + entityId)
                .build();
        if(result.isPresent()){
            logger.info("给远程发送消息{}", result.get());
            RpcSession session = rpcSessions.get(result.get());
            session.tell(clusterMessage);
        }else{
            logger.info("给本地发送消息。。。");
        }
    }
}
