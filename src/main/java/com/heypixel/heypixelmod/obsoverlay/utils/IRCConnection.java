package com.heypixel.heypixelmod.obsoverlay.utils;

import com.google.gson.JsonObject;

/**
 * IRC连接类，用于处理与IRC服务器的连接和通信
 */
public class IRCConnection {
    private MessageHandler messageHandler;
    private boolean connected = false;

    public IRCConnection() {
        // 构造函数
    }

    /**
     * 连接到IRC服务器
     */
    public void connect() {
        // 实现连接逻辑
        System.out.println("Connecting to IRC server...");
        // 连接成功后触发回调
        if (messageHandler != null) {
            messageHandler.onConnected();
        }
    }

    /**
     * 断开与IRC服务器的连接
     */
    public void disconnect() {
        // 实现断开连接逻辑
        System.out.println("Disconnecting from IRC server...");
        connected = false;
        if (messageHandler != null) {
            messageHandler.onDisconnected();
        }
    }

    /**
     * 设置消息处理器
     *
     * @param handler 消息处理器
     */
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    /**
     * 消息处理器接口
     */
    public interface MessageHandler {
        /**
         * 当收到消息时调用
         *
         * @param type 消息类型
         * @param data 消息数据
         */
        void onMessage(String type, JsonObject data);

        /**
         * 当连接成功时调用
         */
        void onConnected();

        /**
         * 当连接断开时调用
         */
        void onDisconnected();

        /**
         * 当发生错误时调用
         *
         * @param error 错误信息
         */
        void onError(String error);
    }
}