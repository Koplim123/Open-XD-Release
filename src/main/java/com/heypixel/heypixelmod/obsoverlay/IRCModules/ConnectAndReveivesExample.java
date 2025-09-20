package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.google.gson.JsonObject;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

/**
 * ConnectAndReveives使用示例喵~
 * 这个类展示了如何使用ConnectAndReveives来连接WebSocket IRC服务器
 */
public class ConnectAndReveivesExample {
    private ConnectAndReveives ircClient;
    
    public ConnectAndReveivesExample() {
        // 创建IRC客户端实例喵~
        ircClient = new ConnectAndReveives();
        
        // 设置消息处理器喵~
        ircClient.setMessageHandler(new ConnectAndReveives.MessageHandler() {
            @Override
            public void onMessage(String type, JsonObject data) {
                handleMessage(type, data);
            }
            
/**
 * 重写onConnected方法，用于处理WebSocket连接成功后的回调
 * 当WebSocket连接成功建立时，会调用此方法
 */
            @Override
            public void onConnected() { // 重写父类或接口中的onConnected方法
                ChatUtils.addChatMessage("§a[IRC] WebSocket连接已建立喵~"); // 添加一条绿色的聊天消息，提示WebSocket连接已成功建立
            }
            
            @Override
            public void onDisconnected() {
                ChatUtils.addChatMessage("§c[IRC] WebSocket连接已断开喵~");
            }
            
            @Override
            public void onError(String error) {
                ChatUtils.addChatMessage("§c[IRC] 错误: " + error + " 喵~");
            }
        });
    }
    
    /**
     * 连接到IRC服务器喵~
     */
    public void connect() {
        ChatUtils.addChatMessage("§e[IRC] 正在连接到服务器... 喵~");
        ircClient.connect();
    }
    
    /**
     * 断开连接喵~
     */
    public void disconnect() {
        ChatUtils.addChatMessage("§e[IRC] 正在断开连接... 喵~");
        ircClient.disconnect();
    }
    
    /**
     * 发送公共消息喵~
     */
    public void sendPublicMessage(String message) {
        if (ircClient.isAuthenticated()) {
            ircClient.sendMessage(message);
            ChatUtils.addChatMessage("§b[IRC] 发送消息: " + message);
        } else {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送消息喵~");
        }
    }
    
    /**
     * 发送私聊消息喵~
     */
    public void sendPrivateMessage(String targetUser, String message) {
        if (ircClient.isAuthenticated()) {
            ircClient.sendPrivateMessage(targetUser, message);
            ChatUtils.addChatMessage("§b[IRC] 发送私聊给 " + targetUser + ": " + message + " 喵~");
        } else {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送私聊消息喵~");
        }
    }
    
    /**
     * 发送Minecraft命令喵~
     */
    public void sendCommand(String command) {
        if (ircClient.isAuthenticated()) {
            ircClient.sendMinecraftCommand(command);
            ChatUtils.addChatMessage("§b[IRC] 发送命令: " + command + " 喵~");
        } else {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送命令喵~");
        }
    }
    
    /**
     * 获取连接状态喵~
     */
    public boolean isConnected() {
        return ircClient.isConnected();
    }
    
    /**
     * 获取认证状态喵~
     */
    public boolean isAuthenticated() {
        return ircClient.isAuthenticated();
    }
    
    /**
     * 处理接收到的消息喵~
     */
    private void handleMessage(String type, JsonObject data) {
        switch (type) {
            case "auth_success":
                ChatUtils.addChatMessage("§a[IRC] 认证成功喵~");
                break;
                
            case "auth_failed":
                String errorMsg = data.has("message") ? data.get("message").getAsString() : "认证失败";
                ChatUtils.addChatMessage("§c[IRC] 认证失败: " + errorMsg + " 喵~");
                break;
                
            case "welcome":
                ChatUtils.addChatMessage("§a[IRC] 欢迎来到IRC服务器喵~");
                break;
                
            case "user_joined":
                String username = data.get("username").getAsString();
                ChatUtils.addChatMessage("§e[IRC] 用户 " + username + " 加入了聊天喵~");
                break;
                
            case "user_left":
                String leftUser = data.get("username").getAsString();
                ChatUtils.addChatMessage("§e[IRC] 用户 " + leftUser + " 离开了聊天喵~");
                break;
                
            case "message":
                String sender = data.get("username").getAsString();
                String message = data.get("message").getAsString();
                ChatUtils.addChatMessage("§7[IRC] §b" + sender + "§7: §f" + message + " 喵~");
                break;
                
            case "private_message":
                String fromUser = data.get("from").getAsString();
                String privateMsg = data.get("message").getAsString();
                ChatUtils.addChatMessage("§d[IRC-私聊] §b" + fromUser + "§d: §f" + privateMsg + " 喵~");
                break;
                
            case "minecraft_command":
                String commandUser = data.get("username").getAsString();
                String command = data.get("command").getAsString();
                ChatUtils.addChatMessage("§6[IRC-命令] §b" + commandUser + "§6: §f" + command + " 喵~");
                break;
                
            case "error":
                String error = data.has("message") ? data.get("message").getAsString() : "未知错误";
                ChatUtils.addChatMessage("§c[IRC] 服务器错误: " + error + " 喵~");
                break;
        }
    }
    
    /**
     * 获取服务器地址喵~
     */
    public String getServerAddress() {
        return ircClient.getServerAddress();
    }
}