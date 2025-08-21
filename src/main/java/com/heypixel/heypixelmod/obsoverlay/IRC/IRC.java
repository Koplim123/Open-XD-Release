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
    
    public void connect(String username) throws URISyntaxException {
        this.username = username;
        // 使用WebSocketChat文件夹中指定的服务器地址
        webSocketAPI = new IRCWebSocketConnectAPI("ws://156.238.232.145:8080", username);
        webSocketAPI.connect();
    }
    
    public void disconnect() {
        if (webSocketAPI != null) {
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