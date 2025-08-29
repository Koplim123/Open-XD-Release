package com.heypixel.heypixelmod.obsoverlay.IRC;


import java.net.URISyntaxException;

public class IRC {
    private static IRC instance;
    private IRCWebSocketConnectAPI webSocketAPI;
    private String username;
    
    private IRC() {
    }
    
    public static IRC getInstance() {
        if (instance == null) {
            instance = new IRC();
        }
        return instance;
    }
    
    public void connect(String username) {
        this.username = username;
        try {
            // 使用WebSocketChat文件夹中指定的服务器地址
            webSocketAPI = new IRCWebSocketConnectAPI("ws://156.238.232.145:8080", username);
            webSocketAPI.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            // 可以添加额外的错误处理逻辑
        }
    }
    
/**
 * 断开WebSocket连接的方法
 * 当webSocketAPI不为null时，调用其disconnect方法断开连接
 */
    public void disconnect() {
    // 检查webSocketAPI是否已初始化
        if (webSocketAPI != null) {
        // 调用WebSocket API的断开连接方法
            webSocketAPI.disconnect();
        }
    }
    
    public void sendMessage(String message) {
        if (webSocketAPI != null && webSocketAPI.isConnected()) {
            SendMessage.sendPublicMessage(webSocketAPI, message);
        } else {
            throw new IllegalStateException("Not connected to IRC server");
        }
    }
    
    public void sendPrivateMessage(String target, String message) {
        if (webSocketAPI != null && webSocketAPI.isConnected()) {
            SendMessage.sendPrivateMessage(webSocketAPI, target, message);
        } else {
            throw new IllegalStateException("Not connected to IRC server");
        }
    }
    
    public void sendCommand(String command) {
        if (webSocketAPI != null && webSocketAPI.isConnected()) {
            SendMessage.sendCommand(webSocketAPI, command);
        } else {
            throw new IllegalStateException("Not connected to IRC server");
        }
    }
    
    public boolean isConnected() {
        return webSocketAPI != null && webSocketAPI.isConnected();
    }
    
    public String getUsername() {
        return username;
    }
}