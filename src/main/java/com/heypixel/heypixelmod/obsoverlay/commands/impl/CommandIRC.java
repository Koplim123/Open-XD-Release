package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.IRC.IRC;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.IRCModule;
import com.heypixel.heypixelmod.obsoverlay.Naven;

@CommandInfo(
   name = "irc",
   description = "Send a message to the IRC server",
   aliases = {}
)
public class CommandIRC extends Command {
   @Override
   public void onCommand(String[] args) {
      try {
         // 检查IRC模块是否启用
         IRCModule ircModule = (IRCModule) Naven.getInstance().getModuleManager().getModule("IRC");
         if (ircModule == null || !ircModule.isEnabled()) {
            ChatUtils.addChatMessage("§c[IRC] IRC module is not enabled. Please enable it first.");
            return;
         }

         // 检查IRC是否已连接
         if (!IRC.getInstance().isConnected()) {
            ChatUtils.addChatMessage("§c[IRC] Not connected to IRC server.");
            return;
         }

         // 检查是否有消息内容
         if (args.length == 0) {
            ChatUtils.addChatMessage("§c[IRC] Please provide a message to send.");
            return;
         }

         // 将参数组合成完整消息
         StringBuilder messageBuilder = new StringBuilder();
         for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
               messageBuilder.append(" ");
            }
         }
         
         String message = messageBuilder.toString();
         
         // 发送消息到IRC服务器
         try {
             IRC.getInstance().sendMessage(message);
             ChatUtils.addChatMessage("§7[§bIRC§7] §fMessage sent: " + message);
         } catch (IllegalStateException e) {
             ChatUtils.addChatMessage("§c[IRC] " + e.getMessage());
         }
      } catch (Exception e) {
         ChatUtils.addChatMessage("§c[IRC] Failed to send message: " + e.getMessage());
         e.printStackTrace();
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0]; // 没有特定的补全建议
   }
}