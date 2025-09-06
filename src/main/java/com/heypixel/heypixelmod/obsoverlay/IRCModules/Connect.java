package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.google.gson.Gson;
import com.heypixel.heypixelmod.obsoverlay.utils.WebSocketClientWrapper;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Connect {
    private WebSocketClientWrapper webSocketClient;
    private boolean isConnected = false;
    private String ircNick;
    private String mcName;
    private final Gson gson = new Gson();
    
    /**
     * 连接到IRC服务器喵~
     */
    public void connect(String serverUrl, String ircNick, String mcName) {
        this.ircNick = ircNick;
        this.mcName = mcName;
        
        try {
            URI serverUri = new URI(serverUrl);
            webSocketClient = new WebSocketClientWrapper(serverUri) {
                @Override
                public void onOpen(org.java_websocket.handshake.ServerHandshake handshake) {
                    isConnected = true;
                    sendAuthMessage();
                    ChatUtils.addChatMessage("§a已连接到IRC服务器喵~");
                }
                
                @Override
                public void onMessage(String message) {
                    // 消息接收由Receive类处理喵~
                    Receive.getInstance().processMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    ChatUtils.addChatMessage("§cIRC连接已断开喵~: " + reason);
                }
                
                @Override
                public void onError(Exception ex) {
                    ChatUtils.addChatMessage("§cIRC连接错误喵~: " + ex.getMessage());
                }
            };
            
            webSocketClient.connect();
        } catch (Exception e) {
            ChatUtils.addChatMessage("§c连接IRC服务器失败喵~: " + e.getMessage());
        }
    }
    
    /**
     * 发送认证消息喵~
     */
    private void sendAuthMessage() {
        Map<String, String> authData = new HashMap<>();
        authData.put("action", "auth");
        authData.put("irc_nick", ircNick);
        authData.put("mc_name", mcName);
        
        String authJson = gson.toJson(authData);
        sendRawMessage(authJson);
    }
    
    /**
     * 发送公共消息喵~
     */
    public void sendMessage(String message) {
        Map<String, String> messageData = new HashMap<>();
        messageData.put("action", "public_message");
        messageData.put("message", message);
        messageData.put("irc_nick", ircNick);
        
        String messageJson = gson.toJson(messageData);
        sendRawMessage(messageJson);
    }
    
    /**
     * 发送私聊消息喵~
     */
    public void sendPrivateMessage(String target, String message) {
        Map<String, String> messageData = new HashMap<>();
        messageData.put("action", "private_message");
        messageData.put("target", target);
        messageData.put("message", message);
        messageData.put("irc_nick", ircNick);
        
        String messageJson = gson.toJson(messageData);
        sendRawMessage(messageJson);
    }
    
    /**
     * 发送原始JSON消息喵~
     */
    private void sendRawMessage(String jsonMessage) {
        if (webSocketClient != null && isConnected) {
            webSocketClient.send(jsonMessage);
        }
    }
    
    /**
     * 断开连接喵~
     */
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
            isConnected = false;
        }
    }
    
    /**
     * 检查是否已连接喵~
     */
    public boolean isConnected() {
        return isConnected;
    }
}