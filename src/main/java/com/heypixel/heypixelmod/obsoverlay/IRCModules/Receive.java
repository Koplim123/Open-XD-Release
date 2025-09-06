package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;

import java.util.Map;

public class Receive {
    private static Receive instance;
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();
    private boolean isReceiving = false;
    
    public Receive() {
        instance = this;
    }
    
    public static Receive getInstance() {
        if (instance == null) {
            instance = new Receive();
        }
        return instance;
    }
    
    /**
     * 处理接收到的消息喵~
     */
    public void processMessage(String message) {
        try {
            JsonObject jsonObject = jsonParser.parse(message).getAsJsonObject();
            String action = jsonObject.get("action").getAsString();
            
            switch (action) {
                case "user_joined":
                    handleUserJoined(jsonObject);
                    break;
                case "private_message":
                    handlePrivateMessage(jsonObject);
                    break;
                case "public_message":
                    handlePublicMessage(jsonObject);
                    break;
                case "minecraft_command":
                    handleMinecraftCommand(jsonObject);
                    break;
                case "error":
                    handleError(jsonObject);
                    break;
                default:
                    ChatUtils.addChatMessage("§7[§bIRC§7] 未知消息类型: " + action);
                    break;
            }
        } catch (Exception e) {
            ChatUtils.addChatMessage("§c处理IRC消息错误喵~: " + e.getMessage());
        }
    }
    
    /**
     * 处理用户加入消息喵~
     */
    private void handleUserJoined(JsonObject jsonObject) {
        String username = jsonObject.get("username").getAsString();
        ChatUtils.addChatMessage("§e用户 §a" + username + "§e 加入了IRC聊天喵~");
    }
    
    /**
     * 处理私聊消息喵~
     */
    private void handlePrivateMessage(JsonObject jsonObject) {
        String from = jsonObject.get("from").getAsString();
        String message = jsonObject.get("message").getAsString();
        
        ChatUtils.addChatMessage("§d[私聊来自 §b" + from + "§d] §f" + message);
    }
    
    /**
     * 处理公共消息喵~
     */
    private void handlePublicMessage(JsonObject jsonObject) {
        String from = jsonObject.get("from").getAsString();
        String message = jsonObject.get("message").getAsString();
        
        ChatUtils.addChatMessage("§9[IRC] §a" + from + "§f: " + message);
    }
    
    /**
     * 处理Minecraft命令喵~
     */
    private void handleMinecraftCommand(JsonObject jsonObject) {
        String command = jsonObject.get("command").getAsString();
        
        // 在Minecraft中执行命令喵~
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().sendCommand(command);
        }
    }
    
    /**
     * 处理错误消息喵~
     */
    private void handleError(JsonObject jsonObject) {
        String error = jsonObject.get("error").getAsString();
        ChatUtils.addChatMessage("§c[IRC错误] " + error);
    }
    
    /**
     * 开始接收消息喵~
     */
    public void startReceiving() {
        isReceiving = true;
    }
    
    /**
     * 停止接收消息喵~
     */
    public void stopReceiving() {
        isReceiving = false;
    }
    
    /**
     * 检查是否正在接收消息喵~
     */
    public boolean isReceiving() {
        return isReceiving;
    }
}