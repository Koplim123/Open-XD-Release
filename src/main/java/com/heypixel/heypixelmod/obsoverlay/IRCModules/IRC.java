package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;

public class IRC {
    private static IRC instance;
    private Connect connect;
    private Receive receive;
    private boolean connected = false;
    
    public IRC() {
        instance = this;
        this.connect = new Connect();
        this.receive = new Receive();
    }
    
    public static IRC getInstance() {
        if (instance == null) {
            instance = new IRC();
        }
        return instance;
    }
    
    /**
     * 玩家进入世界时自动连接IRC服务器喵~
     */
    public void onPlayerJoinWorld() {
        if (!connected && IRCLoginManager.getUsername() != null && !IRCLoginManager.getUsername().isEmpty()) {
            connectToServer();
        } else if (IRCLoginManager.getUsername() == null || IRCLoginManager.getUsername().isEmpty()) {
            ChatUtils.addChatMessage("§c[IRC] 未登录，无法自动连接到IRC服务器");
        }
    }
    
    /**
     * 玩家退出世界时自动断开IRC连接喵~
     */
    public void onPlayerLeaveWorld() {
        if (connected) {
            disconnectFromServer();
        }
    }
    
    /**
     * 连接到IRC服务器喵~
     */
    public void connectToServer() {
        String username = IRCLoginManager.getUsername();
        String mcName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() : "Unknown";
        if (username != null && !username.isEmpty()) {
            ChatUtils.addChatMessage("§e[IRC] 正在连接到服务器...");
            connect.connect("ws://45.192.105.145:8080", username, mcName);
            connected = true;
        } else {
            ChatUtils.addChatMessage("§c[IRC] 连接失败");
        }
    }
    
    /**
     * 断开IRC连接喵~
     */
    public void disconnectFromServer() {
        ChatUtils.addChatMessage("§e[IRC] 正在断开连接...");
        connect.disconnect();
        receive.stopReceiving();
        connected = false;
        ChatUtils.addChatMessage("§a[IRC] 已断开连接");
    }
    
    /**
     * 重新连接到IRC服务器喵~
     */
    public void reconnectToServer() {
        ChatUtils.addChatMessage("§e[IRC] 正在重新连接...");
        disconnectFromServer();
        connectToServer();
    }
    
    /**
     * 发送公共消息喵~
     */
    public void sendMessage(String message) {
        if (connected) {
            connect.sendMessage(message);
        } else {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器");
        }
    }
    
    /**
     * 发送私聊消息喵~
     */
    public void sendPrivateMessage(String target, String message) {
        if (connected) {
            connect.sendPrivateMessage(target, message);
        } else {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器");
        }
    }
    
    /**
     * 检查是否已连接喵~
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * 获取连接状态喵~
     */
    public Connect getConnect() {
        return connect;
    }
    
    /**
     * 获取接收器喵~
     */
    public Receive getReceive() {
        return receive;
    }
}