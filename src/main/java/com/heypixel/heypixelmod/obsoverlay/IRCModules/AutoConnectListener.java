package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventJoinWorld;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventLeaveWorld;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;

public class AutoConnectListener {
    private static ConnectAndReveivesExample ircClient;

    public AutoConnectListener() {
        // 初始化IRC客户端
        if (ircClient == null) {
            ircClient = new ConnectAndReveivesExample();
        }
    }
    
    @EventTarget
    public void onPlayerJoinWorld(EventJoinWorld event) {
        // 检查用户是否已登录IRC
        String ircUsername = IRCLoginManager.getUsername();
        if (ircUsername != null && !ircUsername.isEmpty()) {
            // 如果IRC尚未连接，则自动连接
            if (!ircClient.isConnected()) {
                ChatUtils.addChatMessage("§e[IRC] 正在自动连接到IRC服务器...");
                
                // 在新线程中连接，避免阻塞游戏
                new Thread(() -> {
                    try {
                        Thread.sleep(2000); // 等待2秒，确保世界加载完成
                        ircClient.connect();
                    } catch (InterruptedException e) {
                        ChatUtils.addChatMessage("§c[IRC] 自动连接失败: " + e.getMessage());
                    }
                }).start();
            } else {
                ChatUtils.addChatMessage("§a[IRC] 已连接到IRC服务器");
            }
        } else {
            ChatUtils.addChatMessage("§c[IRC] 未登录IRC账户，无法自动连接");
        }
    }
    
    @EventTarget
    public void onPlayerLeaveWorld(EventLeaveWorld event) {
        // 当玩家离开世界时断开IRC连接
        if (ircClient != null && ircClient.isConnected()) {
            ircClient.disconnect();
        }
    }
    
    /**
     * 获取IRC客户端实例
     */
    public static ConnectAndReveivesExample getIrcClient() {
        return ircClient;
    }
}