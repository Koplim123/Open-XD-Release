package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventJoinWorld;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventLeaveWorld;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;

public class AutoConnectListener {
    private static ConnectAndReveivesExample ircClient;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 100;

    public AutoConnectListener() {
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
        if (ircClient != null && ircClient.isConnected()) {
            ircClient.disconnect();
        }
    }
    
    @EventTarget
    public void onTick(EventRunTicks event) {
        tickCounter++;
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            if (ircClient != null) {
                ircClient.checkAndReconnectIfNeeded();
            }
        }
    }
    
    public static ConnectAndReveivesExample getIrcClient() {
        return ircClient;
    }
}