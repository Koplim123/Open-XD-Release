package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventJoinWorld;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventLeaveWorld;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.Naven;

/**
 * 自动连接监听器
 * 监听游戏事件，管理IRC连接状态
 */
public class AutoConnectListener {
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 100;
    private static boolean hasJoinedWorld = false;

    public AutoConnectListener() {
        // 不再在这里创建IRC客户端实例，使用Naven中的统一实例
    }
    
    @EventTarget
    public void onPlayerJoinWorld(EventJoinWorld event) {
        hasJoinedWorld = true;
        // 检查用户是否已登录IRC
        String ircUsername = IRCLoginManager.getUsername();
        if (ircUsername != null && !ircUsername.isEmpty()) {
            ConnectAndReveives ircClient = Naven.getIrcClient();
            // 如果IRC尚未连接，则自动连接
            if (ircClient == null || !ircClient.isConnected()) {
                ChatUtils.addChatMessage("§e[IRC] 检测到进入世界，正在连接到IRC服务器...");
                // 触发连接
                Naven.connectToIRCAfterLogin();
            } else {
                ChatUtils.addChatMessage("§a[IRC] 已连接到IRC服务器");
            }
        }
    }
    
    @EventTarget
    public void onPlayerLeaveWorld(EventLeaveWorld event) {
        hasJoinedWorld = false;
        // 不断开连接，保持IRC连接活跃
        // 用户可以在不同世界之间切换而不断开IRC
    }
    
    @EventTarget
    public void onTick(EventRunTicks event) {
        tickCounter++;
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            // 使用Naven中的IRC客户端实例
            ConnectAndReveives ircClient = Naven.getIrcClient();
            if (ircClient != null) {
                ircClient.checkAndReconnectIfNeeded();
            }
        }
    }
    
    /**
     * 获取IRC客户端实例
     * @deprecated 使用 Naven.getIrcClient() 代替
     */
    @Deprecated
    public static ConnectAndReveives getIrcClient() {
        return Naven.getIrcClient();
    }
}