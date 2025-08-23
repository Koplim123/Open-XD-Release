package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

@ModuleInfo(name = "FlagCheck", description = "Check Flag and alert.", category = Category.MISC)
public class FlagCheck extends Module {

    private int flagCount = 0;
    private float lastYaw;
    private float lastPitch;

    private void log(String message) {
        ChatUtils.addChatMessage("[FlagCheck] " + message);
    }

    BooleanValue notification = ValueBuilder.create(this, "Notification")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    BooleanValue chat = ValueBuilder.create(this, "Chat")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    @Override
    public void onEnable() {
        flagCount = 0;
        // 记录玩家当前朝向
        lastYaw = mc.player.getYRot();
        lastPitch = mc.player.getXRot();
    }

    @Override
    public void onDisable() {
        flagCount = 0;
    }

    @EventTarget
    public void onPacketReceive(EventHandlePacket event) { // 使用正确的事件类
        if (mc.player == null || mc.player.tickCount <= 25) {
            return;
        }

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) { // getPacket() 方法存在于 EventHandlePacket 中
            flagCount++;
            float serverYaw = packet.getYRot();
            float serverPitch = packet.getXRot();
            float deltaYaw = calculateAngleDelta(serverYaw, lastYaw);
            float deltaPitch = calculateAngleDelta(serverPitch, lastPitch);
            if (deltaYaw >= 90 || deltaPitch >= 90) {
                alert("强制旋转", String.format("(%.1f° | %.1f°)", deltaYaw, deltaPitch));
            } else {
                alert("回弹", "");
            }
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        }
    }

    private float calculateAngleDelta(float newAngle, float oldAngle) {
        float delta = newAngle - oldAngle;
        if (delta > 180) {
            delta -= 360;
        }
        if (delta < -180) {
            delta += 360;
        }
        return Math.abs(delta);
    }


    private void alert(String reason, String extra) {
        if(chat.getCurrentValue()){
            String message;
            if (extra.isEmpty()) {
                message = String.format("§f服务器检测到 §c%s§f，总计 §c%d§f 次。", reason, flagCount);
            } else {
                message = String.format("§f服务器检测到 §c%s§f %s，总计 §c%d§f 次。", reason, extra, flagCount);
            }

            log(message);
        }
        if(notification.getCurrentValue()){
            String message;
            if (extra.isEmpty()) {
                message = String.format("§f服务器检测到 §c%s§f，总计 §c%d§f 次。", reason, flagCount);
            } else {
                message = String.format("§f服务器检测到 §c%s§f %s，总计 §c%d§f 次。", reason, extra, flagCount);
            }

            Naven.getInstance().getNotificationManager().addNotification(
                    new Notification(NotificationLevel.WARNING, message, 5000L)
            );
        }

    }
}