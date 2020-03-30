package com.ziapple.demo;

import java.io.*;
import java.net.Socket;

/**
 * Socket连接Netty服务器测试
 * 开启{@link com.ziapple.demo.netty.NettySocketServer}
 * 消息处理的Handler见{@link com.ziapple.demo.netty.MqttTransportHandler}
 */
public class SocketClientTest
{
    public static void main( String[] args )
    {
       String serverIP = "127.0.0.1";
       final int port = 1884;
       
      try {
          Socket client = new Socket(serverIP, port);
          System.out.println( "connected to "+serverIP+"on port "+port);
          OutputStream outToServer= client.getOutputStream();
          DataOutputStream out = new DataOutputStream(outToServer);
          BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
          System.out.println("Please enter message:");
          //给服务端发消息
          String str;
          while((str = reader.readLine()) != null){
              out.writeUTF(str);
          }
          client.close();
      } catch(IOException ex){
          ex.printStackTrace();
      }
   }
}