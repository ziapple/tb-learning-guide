package com.ziapple.demo;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

/**
 * 先启动{@link com.ziapple.demo.netty.NettyMqttServer}
 */
public class MqttClientTest {
    public static MqttClient mqttClient = null;
    private static MemoryPersistence memoryPersistence = null;
    private static MqttConnectOptions mqttConnectOptions = null;


    public static void main(String[] args) {
        //初始化连接设置对象
        mqttConnectOptions = new MqttConnectOptions();
        // 设置用户名
        mqttConnectOptions.setUserName("userToken");
        MqttClientTest mqttClientTest = new MqttClientTest();
        mqttClientTest.init("mqttClient-1");
        mqttClientTest.publishMessage("topic1", "Hello world", 1);
    }

    public void init(String clientId) {
        //初始化MqttClient
        if(null != mqttConnectOptions) {
            //true可以安全地使用内存持久性作为客户端断开连接时清除的所有状态
            mqttConnectOptions.setCleanSession(true);
            //设置连接超时
            mqttConnectOptions.setConnectionTimeout(30);
            //设置持久化方式
            memoryPersistence = new MemoryPersistence();
            if(null != memoryPersistence && null != clientId) {
                try {
                    mqttClient = new MqttClient("tcp://127.0.0.1:1884", clientId, memoryPersistence);
                    //客户端添加回调函数
                    mqttClient.setCallback(new MqttRecieveCallback());
                    //创建连接
                    mqttClient.connect(mqttConnectOptions);
                    System.out.println("连接成功");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }else {
            System.out.println("mqttConnectOptions对象为空");
        }
    }

    //关闭连接
    public void closeConnect() {
        //关闭存储方式
        if(null != memoryPersistence) {
            try {
                memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            System.out.println("memoryPersistence is null");
        }

        //关闭连接
        if(null != mqttClient) {
            if(mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else {
                System.out.println("mqttClient is not connect");
            }
        }else {
            System.out.println("mqttClient is null");
        }
    }

    //	发布消息
    public void publishMessage(String pubTopic,String message,int qos) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());

            MqttTopic topic = mqttClient.getTopic(pubTopic);

            if(null != topic) {
                try {
                    MqttDeliveryToken publish = topic.publish(mqttMessage);
                    if(!publish.isComplete()) {
                        System.out.println("消息发布成功" + message);
                    }
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }else {
            reConnect();
        }

    }
    //	重新连接
    public void reConnect() {
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                if(null != mqttConnectOptions) {
                    try {
                        mqttClient.connect(mqttConnectOptions);
                    } catch (MqttException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }else {
                    System.out.println("mqttConnectOptions is null");
                }
            }else {
                System.out.println("mqttClient is null or connect");
            }
        }else {
            init("123");
        }

    }

    //订阅主题
    public void subTopic(String topic) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(topic, 1);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            System.out.println("mqttClient is error");
        }
    }


    //清空主题
    public void cleanTopic(String topic) {
        if(null != mqttClient&& !mqttClient.isConnected()) {
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            System.out.println("mqttClient is error");
        }
    }
}
