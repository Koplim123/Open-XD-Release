package com.heypixel.heypixelmod.obsoverlay.IRC;

import com.google.gson.JsonObject;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

public class Receive {
    
    /**
     * 处理从服务器接收到的消息
     * @param data 从服务器接收到的JSON数据
     */
    public static void handleMessage(JsonObject data) {
        try {
            String type = data.has("type") ? data.get("type").getAsString() : "";
            
            switch (type) {
                case "welcome":
                    handleWelcomeMessage(data);
                    break;
                case "message":
                    handlePublicMessage(data);
                    break;
                case "user_joined":
                    handleUserJoined(data);
                    break;
                case "user_left":
                    handleUserLeft(data);
                    break;
                case "private_message":
                    handlePrivateMessage(data);
                    break;
                case "private_message_sent":
                    handlePrivateMessageSent(data);
                    break;
                case "minecraft_command":
                    handleMinecraftCommand(data);
                    break;
                case "error":
                    handleError(data);
                    break;
                default:
                    ChatUtils.addChatMessage("§c[IRC] Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            ChatUtils.addChatMessage("§c[IRC] Error handling message: " + e.getMessage());
        }
    }
    
    /**
     * 处理欢迎消息
     * @param data 消息数据
     */
    private static void handleWelcomeMessage(JsonObject data) {
        String message = data.has("message") ? data.get("message").getAsString() : "";
        ChatUtils.addChatMessage("§a[IRC] " + message);
        
        // 如果有用户列表，也可以显示
        if (data.has("users")) {
            ChatUtils.addChatMessage("§7[IRC] Online users: " + data.get("users").toString());
        }
    }
    
    /**
     * 处理公共消息
     * @param data 消息数据
     */
    private static void handlePublicMessage(JsonObject data) {
        String username = data.has("username") ? data.get("username").getAsString() : "Unknown";
        String message = data.has("message") ? data.get("message").getAsString() : "";
        String timestamp = data.has("timestamp") ? data.get("timestamp").getAsString() : "";
        
        ChatUtils.addChatMessage(String.format("§7[§bIRC§7]§7[§f%s§7] §b%s§f: %s", timestamp, username, message));
    }
    
    /**
     * 处理用户加入通知
     * @param data 消息数据
     */
    private static void handleUserJoined(JsonObject data) {
        String username = data.has("username") ? data.get("username").getAsString() : "Unknown";
        String timestamp = data.has("timestamp") ? data.get("timestamp").getAsString() : "";
        
        ChatUtils.addChatMessage(String.format("§7[§bIRC§7]§7[§f%s§7] §e%s joined the chat", timestamp, username));
    }
    
    /**
     * 处理用户离开通知
     * @param data 消息数据
     */
    private static void handleUserLeft(JsonObject data) {
        String username = data.has("username") ? data.get("username").getAsString() : "Unknown";
        String timestamp = data.has("timestamp") ? data.get("timestamp").getAsString() : "";
        
        ChatUtils.addChatMessage(String.format("§7[§bIRC§7]§7[§f%s§7] §e%s left the chat", timestamp, username));
    }
    
    /**
     * 处理私聊消息
     * @param data 消息数据
     */
    private static void handlePrivateMessage(JsonObject data) {
        String from = data.has("from") ? data.get("from").getAsString() : "Unknown";
        String message = data.has("message") ? data.get("message").getAsString() : "";
        String timestamp = data.has("timestamp") ? data.get("timestamp").getAsString() : "";
        
        ChatUtils.addChatMessage(String.format("§7[§bIRC§7]§d[PM]§7[§f%s§7] §d%s: %s", timestamp, from, message));
    }
    
    /**
     * 处理私聊消息发送确认
     * @param data 消息数据
     */
    private static void handlePrivateMessageSent(JsonObject data) {
        String to = data.has("to") ? data.get("to").getAsString() : "Unknown";
        String message = data.has("message") ? data.get("message").getAsString() : "";
        
        ChatUtils.addChatMessage(String.format("§7[§bIRC§7]§d[PM Sent] To %s: %s", to, message));
    }
    
    /**
     * 处理Minecraft命令
     * @param data 消息数据
     */
    private static void handleMinecraftCommand(JsonObject data) {
        String username = data.has("username") ? data.get("username").getAsString() : "Unknown";
        String command = data.has("command") ? data.get("command").getAsString() : "";
        String timestamp = data.has("timestamp") ? data.get("timestamp").getAsString() : "";
        
        ChatUtils.addChatMessage(String.format("§7[§bIRC§7]§6[CMD]§7[§f%s§7] §6%s: %s", timestamp, username, command));
        
        // 在实际应用中，这里可以执行Minecraft命令
        // 例如: Minecraft.getInstance().player.chat(command);
    }
    
    /**
     * 处理错误消息
     * @param data 消息数据
     */
    private static void handleError(JsonObject data) {
        String message = data.has("message") ? data.get("message").getAsString() : "Unknown error";
        ChatUtils.addChatMessage("§c[IRC] Error: " + message);
    }
}