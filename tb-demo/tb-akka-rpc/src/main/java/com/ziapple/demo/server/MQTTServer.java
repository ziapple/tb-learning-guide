package com.ziapple.demo.server;

/**
 * 模拟MqttServer发送给Cluster集群消息
 */
public interface MqttServer {
    /**
     * 将接受到的消息发送给Cluster处理
     * @param mqttMsg
     */
    public void process(int entityId, String mqttMsg);
}
