package com.ziapple.demo.server;

/**
 * 模拟MqttServer发送给Cluster集群消息
 */
public interface MQTTServer {
    /**
     * 将接受到的消息发送给Cluster处理
     * @param mqttMsg
     */
    void process(String mqttMsg);
}
