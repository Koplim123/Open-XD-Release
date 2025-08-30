// 文件路径: com/heypixel/heypixelmod/obsoverlay/modules/impl/misc/AutoPlay.java

package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

@ModuleInfo(
        name = "AutoPlay",
        description = "Automatically joins the next game after a delay.",
        category = Category.MISC
)
public class AutoPlay extends Module {

    private final FloatValue delay = ValueBuilder.create(this, "Delay (Seconds)")
            .setDefaultFloatValue(2.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    private long scheduledTime = 0;
    // [新增] 记录开始等待的时间和总等待时间
    private long waitStartTime = 0;
    private long totalWaitTime = 0;

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || !this.isEnabled()) return;

        if (event.getPacket() instanceof ClientboundSystemChatPacket) {
            String message = ((ClientboundSystemChatPacket) event.getPacket()).content().getString();

            if (message.contains("游戏结束，请对")) {
                ChatUtils.addChatMessage("§b[AutoPlay] §fWait §e" + delay + " §fsecond to next game.");
                long delayMillis = (long) (delay.getCurrentValue() * 1000L);
                this.scheduledTime = System.currentTimeMillis() + delayMillis;
                // [新增] 记录计时器信息
                this.waitStartTime = System.currentTimeMillis();
                this.totalWaitTime = delayMillis;
            }

            if (message.contains("正在为您匹配可用的游戏服务器")) {
                if (this.scheduledTime > 0) {
                    resetTimer();
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || !this.isEnabled()) return;

        if (this.scheduledTime > 0 && System.currentTimeMillis() >= this.scheduledTime) {
            mc.player.connection.sendCommand("again");
            ChatUtils.addChatMessage("§b[AutoPlay] §fEntering the next game.");
            resetTimer();
        }
    }

    private void resetTimer() {
        this.scheduledTime = 0;
        this.waitStartTime = 0;
        this.totalWaitTime = 0;
    }

    @Override
    public void onEnable() {
        resetTimer();
    }

    @Override
    public void onDisable() {
        resetTimer();
    }
}