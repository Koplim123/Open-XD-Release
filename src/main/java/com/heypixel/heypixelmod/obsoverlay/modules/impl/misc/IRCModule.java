package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.IRC.IRCUsernameManager;
import com.heypixel.heypixelmod.obsoverlay.IRC.IRC;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URISyntaxException;

@ModuleInfo(
        name = "IRC",
        description = "IRC Module and Player Name Render",
        category = Category.MISC
)
public class IRCModule extends Module {

    @Override
    public void onEnable() {
        // 检查用户是否已经登录
        String username = IRCLoginManager.getUsername();
        if (username == null || username.isEmpty()) {
            ChatUtils.addChatMessage("§c[IRC] You must login before connecting to IRC server");
            this.setEnabled(false);
            return;
        }

        // 设置IRC用户名
        IRCUsernameManager.getInstance().setIRCUsername(username);
        
        // 连接到IRC服务器
        try {
            IRC.getInstance().connect(username);
        } catch (Exception e) {
            ChatUtils.addChatMessage("§c[IRC] Failed to connect to server: " + e.getMessage());
            this.setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        // 断开IRC服务器连接
        try {
            IRC.getInstance().disconnect();
        } catch (Exception e) {
            // 忽略断开连接时的异常
        }
    }
}