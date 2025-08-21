package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "GrimReduce",
        description = "Hylex-specific velocity reduction with timed multipliers",
        category = Category.COMBAT
)
public class GrimReduce extends Module {

    private final BooleanValue debugMessages = ValueBuilder.create(this, "Debug Messages")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    // 状态跟踪
    private boolean velocityTriggered = false;
    private int lastHurtTime = 0;
    private final Minecraft mc = Minecraft.getInstance();

    @EventTarget
    public void onVelocityPacket(ClientboundSetEntityMotionPacket event) {
        LocalPlayer player = mc.player;
        if (player == null || event.getId() != player.getId()) return;

        velocityTriggered = true;
        sendDebugMessage("击退包接收，准备应用Hylex规则");
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 只在受伤状态处理
        if (player.hurtTime == 0) {
            velocityTriggered = false;
            return;
        }

        // 跳跃逻辑 (原版Hylex)
        if (player.hurtTime > 5 && player.hurtTime != lastHurtTime && player.onGround()) {
            player.jumpFromGround();
            sendDebugMessage("执行Hylex跳跃重置 hurtTime=" + player.hurtTime);
        }

        // 速度减少逻辑 (原版Hylex乘数)
        if (velocityTriggered && player.hurtTime != lastHurtTime) {
            Vec3 velocity = player.getDeltaMovement();

            switch (player.hurtTime) {
                case 9:
                    player.setDeltaMovement(velocity.multiply(0.8, 1.0, 0.8));
                    sendDebugMessage("应用hurtTime=9乘数 (0.8)");
                    break;
                case 8:
                    player.setDeltaMovement(velocity.multiply(0.11, 1.0, 0.11));
                    sendDebugMessage("应用hurtTime=8乘数 (0.11)");
                    break;
                case 7:
                    player.setDeltaMovement(velocity.multiply(0.4, 1.0, 0.4));
                    sendDebugMessage("应用hurtTime=7乘数 (0.4)");
                    break;
                case 4:
                    player.setDeltaMovement(velocity.multiply(0.37, 1.0, 0.37));
                    sendDebugMessage("应用hurtTime=4乘数 (0.37)");
                    break;
            }
        }

        lastHurtTime = player.hurtTime;
    }

    private void sendDebugMessage(String message) {
        // 使用getCurrentValue()替代get()
        if (debugMessages.getCurrentValue() && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§b[VH]§f " + message));
        }
    }
}