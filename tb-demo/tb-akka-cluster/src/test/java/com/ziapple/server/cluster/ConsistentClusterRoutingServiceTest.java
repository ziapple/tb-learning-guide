package com.ziapple.server.cluster;

import org.junit.Test;

import java.util.UUID;

/**
 * 集群服务一致性Hash测试
 */
public class ConsistentClusterRoutingServiceTest {
    ConsistentClusterRoutingService consistentClusterRoutingService;
    @Test
    public void init(){
        MockDiscoveryService mockDiscoveryService = new MockDiscoveryService();
        consistentClusterRoutingService = new ConsistentClusterRoutingService();
        // 模拟10个服务器
        consistentClusterRoutingService.setDiscoveryService(mockDiscoveryService);

        // 设置Hash函数、虚拟节点数
        consistentClusterRoutingService.setHashFunctionName("murmur3_128");
        consistentClusterRoutingService.setVirtualNodesSize(2);

        // 初始化本地服务器
        consistentClusterRoutingService.init();
    }
}
