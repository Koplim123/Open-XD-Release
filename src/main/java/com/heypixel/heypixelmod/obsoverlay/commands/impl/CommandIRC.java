package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.IRCModules.AutoConnectListener;
import com.heypixel.heypixelmod.obsoverlay.IRCModules.ConnectAndReveivesExample;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;


@CommandInfo(
    name = "irc",
    description = "Chat in IRC!",
    aliases = {"irc"}
)
public class CommandIRC extends Command {
    
    public CommandIRC() {
        // 使用AutoConnectListener的IRC客户端实例
    }
    
    private ConnectAndReveivesExample getIrcClient() {
        return AutoConnectListener.getIrcClient();
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
                
            case "list":
                handleUserList();
                break;
                
            default:
                String message = String.join(" ", args);
                handleMessage(message);
                break;
        }
    }
    

    private void handleReconnect() {
        ConnectAndReveivesExample ircClient = getIrcClient();
        if (ircClient == null) {
            ChatUtils.addChatMessage("§c[IRC] IRC客户端未初始化");
            return;
        }
        
        ChatUtils.addChatMessage("§e[IRC] 正在重连服务器...");
        

        if (ircClient.isConnected()) {
            ircClient.disconnect();
        }
        

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                ircClient.connect();
            } catch (InterruptedException e) {
                ChatUtils.addChatMessage("§c[IRC] 重连失败: " + e.getMessage() + "");
            }
        }).start();
    }
    
    /**
     * 处理用户列表指令
     */
    private void handleUserList() {
        ConnectAndReveivesExample ircClient = getIrcClient();
        if (ircClient == null) {
            ChatUtils.addChatMessage("§c[IRC] IRC客户端未初始化");
            return;
        }
        
        if (!ircClient.isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器，请先使用 .irc reconnect 连接");
            return;
        }
        
        if (!ircClient.isAuthenticated()) {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法获取用户列表");
            return;
        }
        
        ChatUtils.addChatMessage("§e[IRC] 正在获取在线用户列表...");
        ircClient.requestUserList();
    }
    
    /**
     * 处理消息发送
     */
    private void handleMessage(String message) {
        if (message.trim().isEmpty()) {
            showHelp();
            return;
        }
        

        if (message.startsWith("/msg ") || message.startsWith("/tell ") || message.startsWith("/w ")) {
            handlePrivateMessage(message);
        } 

        else if (message.startsWith("/")) {
            handleCommand(message);
        }
        else {
            handlePublicMessage(message);
        }
    }
    
    /**
     * 处理公共消息
     */
    private void handlePublicMessage(String message) {
        ConnectAndReveivesExample ircClient = getIrcClient();
        if (ircClient == null) {
            ChatUtils.addChatMessage("§c[IRC] IRC客户端未初始化");
            return;
        }
        
        if (!ircClient.isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器，请先使用 .irc reconnect 连接");
            return;
        }
        
        if (!ircClient.isAuthenticated()) {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送消息");
            return;
        }
        
        ircClient.sendPublicMessage(message);
    }
    
    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(String message) {
        ConnectAndReveivesExample ircClient = getIrcClient();
        if (ircClient == null) {
            ChatUtils.addChatMessage("§c[IRC] IRC客户端未初始化");
            return;
        }
        
        if (!ircClient.isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器，请先使用 .irc reconnect 连接");
            return;
        }
        
        if (!ircClient.isAuthenticated()) {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送私聊消息");
            return;
        }
        
        String[] parts = message.split(" ", 3);
        if (parts.length < 3) {
            ChatUtils.addChatMessage("§c[IRC] 私聊格式错误，正确格式: /msg <用户名> <消息>");
            return;
        }
        
        String targetUser = parts[1];
        String privateMessage = parts[2];
        
        ircClient.sendPrivateMessage(targetUser, privateMessage);
    }
    
    /**
     * 处理命令
     */
    private void handleCommand(String command) {
        ConnectAndReveivesExample ircClient = getIrcClient();
        if (ircClient == null) {
            ChatUtils.addChatMessage("§c[IRC] IRC客户端未初始化");
            return;
        }
        
        if (!ircClient.isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] 未连接到服务器，请先使用 .irc reconnect 连接");
            return;
        }
        
        if (!ircClient.isAuthenticated()) {
            ChatUtils.addChatMessage("§c[IRC] 未认证，无法发送命令");
            return;
        }
        
        ircClient.sendCommand(command.substring(1)); // 去掉开头的/
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp() {
        ChatUtils.addChatMessage("§b[IRC] 指令帮助");
        ChatUtils.addChatMessage("§e.irc <message> §7- 发送公共消息");
        ChatUtils.addChatMessage("§e.irc reconnect §7- 重连服务器");
        ChatUtils.addChatMessage("§e.irc list §7- 显示在线用户列表");
    }
    
    @Override
    public String[] onTab(String[] args) {
        // 提供基本的tab补全建议
        if (args.length == 1) {
            return new String[]{"reconnect", "list", "/msg ", "/tell ", "/w ", "/help"};
        }
        return new String[0];
    }
}