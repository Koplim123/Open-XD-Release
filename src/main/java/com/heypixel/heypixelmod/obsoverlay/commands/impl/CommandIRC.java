package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.IRCModules.IRC;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;

@CommandInfo(
        name = "irc",
        description = "IRC Chat",
        aliases = {"irc"}
)
public class CommandIRC extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            // 显示帮助信息喵~
            ChatUtils.addChatMessage("§6=== IRC命令帮助 ===");
            ChatUtils.addChatMessage("§e.irc <消息> §f- 发送公共消息");
            ChatUtils.addChatMessage("§e.irc <用户名> <消息> §f- 发送私聊消息");
            ChatUtils.addChatMessage("§e.irc status §f- 查看连接状态");
            ChatUtils.addChatMessage("§e.irc reconnect §f- 重新连接到IRC服务器");
            return;
        }

        IRC irc = IRC.getInstance();

        if (args[0].equalsIgnoreCase("status")) {
            // 显示连接状态喵~
            String status = irc.isConnected() ? "§a已连接" : "§c未连接";
            ChatUtils.addChatMessage("§6IRC状态: " + status);
            return;
        }
        
        if (args[0].equalsIgnoreCase("reconnect")) {
            // 重新连接IRC服务器喵~
            irc.reconnectToServer();
            return;
        }

        if (args.length == 1) {
            // 发送公共消息喵~
            if (!irc.isConnected()) {
                ChatUtils.addChatMessage("§cIRC未连接，无法发送消息喵~");
                return;
            }

            String message = args[0];
            irc.sendMessage(message);
            ChatUtils.addChatMessage("§7[IRC] 消息已发送喵~");

        } else if (args.length >= 2) {
            // 发送私聊消息喵~
            if (!irc.isConnected()) {
                ChatUtils.addChatMessage("§cIRC未连接，无法发送私聊消息喵~");
                return;
            }

            String target = args[0];
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            String message = messageBuilder.toString().trim();

            irc.sendPrivateMessage(target, message);
            ChatUtils.addChatMessage("§7[IRC] 私聊消息已发送给 §b" + target + "§7 喵~");
        }
    }

    @Override
    public String[] onTab(String[] args) {
        // 选项卡补全功能喵~
        if (args.length == 1) {
            String[] options = {"status", "reconnect"};
            java.util.List<String> result = new java.util.ArrayList<>();
            for (String option : options) {
                if (option.startsWith(args[0].toLowerCase())) {
                    result.add(option);
                }
            }
            return result.toArray(new String[0]);
        }
        return new String[0];
    }
}