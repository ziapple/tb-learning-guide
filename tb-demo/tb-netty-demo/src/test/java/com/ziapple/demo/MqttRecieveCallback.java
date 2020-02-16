package com.ziapple.demo;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class MqttRecieveCallback implements MqttCallback {
 
	@Override
	public void connectionLost(Throwable cause) {
		
	}

	@Override
	public void messageArrived(MqttTopic mqttTopic, MqttMessage mqttMessage) throws Exception {
		System.out.println("Client 接收消息主题 : " + mqttTopic);
		System.out.println("Client 接收消息Qos : " + mqttMessage.getQos());
		System.out.println("Client 接收消息内容 : " + new String(mqttMessage.getPayload()));
	}

	@Override
	public void deliveryComplete(MqttDeliveryToken mqttDeliveryToken) {

	}
}