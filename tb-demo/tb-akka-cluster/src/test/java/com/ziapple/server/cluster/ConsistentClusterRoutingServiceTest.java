package com.ziapple.server.cluster;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 集群服务一致性Hash测试
 */
public class ConsistentClusterRoutingServiceTest {
    ConsistentClusterRoutingService consistentClusterRoutingService;
    @Before
    public void init(){
        MockDiscoveryService mockDiscoveryService = new MockDiscoveryService();
        mockDiscoveryService.init();
        consistentClusterRoutingService = new ConsistentClusterRoutingService();
        // 模拟10个服务器
        consistentClusterRoutingService.setDiscoveryService(mockDiscoveryService);

        // 设置Hash函数、虚拟节点数
        consistentClusterRoutingService.setHashFunctionName("murmur3_128");
        consistentClusterRoutingService.setVirtualNodesSize(2);

        // 初始化本地服务器
        consistentClusterRoutingService.init();
    }

    /**
     * 测试随机性
     */
    @Test
    public void testGetServerInstance(){
        List<UUID> devices = new ArrayList<>();
        for(int i=0; i<100; i++){
            devices.add(UUID.randomUUID());
        }
        UUID fixedUUID = UUID.randomUUID();
        devices.forEach((uuid) -> {
            Optional<ServerAddress> serverAddress = consistentClusterRoutingService.resolveById(uuid);
            if(serverAddress.isPresent()){
                System.out.println(uuid + "->" + serverAddress.get().toString());
            }
            // 测试固定性
            System.out.println(fixedUUID + "->" + consistentClusterRoutingService.resolveById(fixedUUID));
        }); }
}
