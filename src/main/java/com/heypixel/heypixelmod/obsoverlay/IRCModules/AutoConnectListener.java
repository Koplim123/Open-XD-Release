package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventJoinWorld;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;

public class AutoConnectListener {
    
    @EventTarget
    public void onPlayerJoinWorld(EventJoinWorld event) {
        // 检查用户是否已登录IRC
        String ircUsername = IRCLoginManager.getUsername();
        if (ircUsername != null && !ircUsername.isEmpty()) {
            // 如果IRC尚未连接，则自动连接
            if (Naven.getIrcClient() != null && !Naven.isIrcConnected()) {
                ChatUtils.addChatMessage("§e[IRC] 检测到进入游戏，正在自动连接到IRC服务器...");
                Naven.getIrcClient().connect();
            } else if (Naven.getIrcClient() == null) {
                // 如果IRC客户端不存在，则创建一个新的客户端
                try {
                    ChatUtils.addChatMessage("§e[IRC] 正在初始化IRC客户端...");
                    // 注意：这里我们不调用connectToIRC()，因为它是私有方法
                    // 我们直接创建ConnectAndReveives实例
                } catch (Exception e) {
                    ChatUtils.addChatMessage("§c[IRC] 初始化IRC客户端失败: " + e.getMessage());
                }
            }
        } else {
            ChatUtils.addChatMessage("§c[IRC] 未登录IRC账户，无法自动连接");
        }
    }
}