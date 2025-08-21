package com.heypixel.heypixelmod.obsoverlay.IRC;

import com.google.gson.JsonObject;

public class SendMessage {
    
    /**
     * 发送公共消息
     * @param webSocketAPI WebSocket连接实例
     * @param message 消息内容
     */
    public static void sendPublicMessage(IRCWebSocketConnectAPI webSocketAPI, String message) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "message");
        msg.addProperty("message", message);
        webSocketAPI.sendMessage(msg.toString());
    }
    
    /**
     * 发送私聊消息
     * @param webSocketAPI WebSocket连接实例
     * @param target 目标用户名
     * @param message 消息内容
     */
    public static void sendPrivateMessage(IRCWebSocketConnectAPI webSocketAPI, String target, String message) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "private_message");
        msg.addProperty("target", target);
        msg.addProperty("message", message);
        webSocketAPI.sendMessage(msg.toString());
    }
    
    /**
     * 发送Minecraft命令
     * @param webSocketAPI WebSocket连接实例
     * @param command 命令内容
     */
    public static void sendCommand(IRCWebSocketConnectAPI webSocketAPI, String command) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "minecraft_command");
        msg.addProperty("command", command);
        webSocketAPI.sendMessage(msg.toString());
    }
}