package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ModuleInfo(
        name = "BanWarn",
        description = "banwarn",
        category = Category.MISC
)
public class BanWarn extends Module {
    private static final Notification banwarngg = new Notification(NotificationLevel.WARNING, "有一位Hacker被办了啊!", 2000L);
    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this); // 注册Forge事件监听
    }

    @Override
    public void onDisable() {
        if (!this.isEnabled()) {
            this.setEnabled(true);
        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        String rawMessage = event.getMessage().getString();
        if (rawMessage.contains("一名违规玩家")) {
            onViolationDetected();
        }
    }

    private void onViolationDetected() {
        Naven.getInstance().getNotificationManager().addNotification(banwarngg);
    }
}