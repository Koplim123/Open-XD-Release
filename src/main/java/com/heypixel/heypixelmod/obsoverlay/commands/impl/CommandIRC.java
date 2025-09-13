package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.IRCModules.ConnectAndReveives;
import com.heypixel.heypixelmod.obsoverlay.IRCModules.ConnectAndReveivesExample;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.google.gson.JsonObject;

/**
 * IRC指令处理类喵~
 * 支持.irc <message>收发消息和.irc reconnect重连服务器喵~
 */
@CommandInfo(
    name = "irc",
    description = "Chat in IRC!",
    aliases = {"irc"}
)
public class CommandIRC extends Command {
    private ConnectAndReveivesExample ircClient;
    
    public CommandIRC() {
        this.ircClient = new ConnectAndReveivesExample();
    }
    
    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reconnect":
                handleReconnect();
                break;
                
            default:
                // 默认情况下，将第一个参数之后的所有内容作为消息发送喵~
                String message = String.join(" ", args);
                handleMessage(message);
                break;
        }
    }
    
    /**
     * 处理重连指令喵~
     */
    private void handleReconnect() {
        ChatUtils.addChatMessage("§e[IRC] 正在重连服务器... 喵~");
        
        // 如果已经连接，先断开喵~
        if (ircClient.isConnected()) {
            ircClient.disconnect();
        }
        
        // 等待一小段时间再重新连接喵~
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                ircClient.connect();
            } catch (InterruptedException e) {
                ChatUtils.addChatMessage("§c[IRC] 重连失败: " + e.getMessage() + " 喵~");
            }
        }).start();
    }
    
    /**
     * 处理消息发送喵~
     */
    private void handleMessage(String message) {
        if (message.trim().isEmpty()) {
            showHelp();
            return;
        }
        
        // 检查是否是私聊消息喵~
        if (message.startsWith("/msg ") || message.startsWith("/tell ") || message.startsWith("/w ")) {
            handlePrivateMessage(message);
        } 
        // 检查是否是命令喵~
        else if (message.startsWith("/")) {
            handleCommand(message);
        }
        // 普通消息喵~
        else {
            handlePublicMessage(message);
        }
    }
    
    /**
     * 处理公共消息喵~
     */
    private void handlePublicMessage(String message) {
        if (!ircClient.isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器，请先使用 .irc reconnect 连接喵~");
            return;
        }
        
        if (!ircClient.isAuthenticated()) {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送消息喵~");
            return;
        }
        
        ircClient.sendPublicMessage(message);
    }
    
    /**
     * 处理私聊消息喵~
     */
    private void handlePrivateMessage(String message) {
        if (!ircClient.isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器，请先使用 .irc reconnect 连接喵~");
            return;
        }
        
        if (!ircClient.isAuthenticated()) {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送私聊消息喵~");
            return;
        }
        
        String[] parts = message.split(" ", 3);
        if (parts.length < 3) {
            ChatUtils.addChatMessage("§c[IRC] 私聊格式错误，正确格式: /msg <用户名> <消息> 喵~");
            return;
        }
        
        String targetUser = parts[1];
        String privateMessage = parts[2];
        
        ircClient.sendPrivateMessage(targetUser, privateMessage);
    }
    
    /**
     * 处理命令喵~
     */
    private void handleCommand(String command) {
        if (!ircClient.isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器，请先使用 .irc reconnect 连接喵~");
            return;
        }
        
        if (!ircClient.isAuthenticated()) {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送命令喵~");
            return;
        }
        
        ircClient.sendCommand(command.substring(1)); // 去掉开头的/
    }
    
    /**
     * 显示帮助信息喵~
     */
    private void showHelp() {
        ChatUtils.addChatMessage("§b[IRC] 指令帮助");
        ChatUtils.addChatMessage("§e.irc <message> §7- 发送公共消息");
        ChatUtils.addChatMessage("§e.irc reconnect §7- 重连服务器");
    }
    
    @Override
    public String[] onTab(String[] args) {
        // 提供基本的tab补全建议喵~
        if (args.length == 1) {
            return new String[]{"reconnect", "/msg ", "/tell ", "/w ", "/help"};
        }
        return new String[0];
    }
}