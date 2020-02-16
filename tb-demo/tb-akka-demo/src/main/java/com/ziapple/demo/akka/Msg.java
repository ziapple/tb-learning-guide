package com.ziapple.demo.akka;

//定义消息
public  class Msg {
    private String  content = "apple";

    public Msg(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}