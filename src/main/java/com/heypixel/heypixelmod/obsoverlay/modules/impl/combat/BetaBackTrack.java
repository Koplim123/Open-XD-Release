package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(
        name = "BetaBackTrack",
        description = "Stuck Network,but adversaries",
        category = Category.COMBAT
)
public class BetaBackTrack extends Module {
    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue OnGroundStop = ValueBuilder.create(this, "OnGroundStop")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public FloatValue maxpacket = ValueBuilder.create(this, "Max Packet number")
            .setDefaultFloatValue(1000F)
            .setFloatStep(5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(5000F)
            .build()
            .getFloatValue();
    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(3F)
            .setFloatStep(0.5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(6F)
            .build()
            .getFloatValue();
    FloatValue delay = ValueBuilder.create(this, "Delay(Tick)")
            .setDefaultFloatValue(20F)
            .setFloatStep(1F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(200F)
            .build()
            .getFloatValue();
    public BooleanValue btrender = ValueBuilder.create(this, "Render")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public ModeValue btrendermode = ValueBuilder.create(this, "Render Mode")
            .setVisibility(this.btrender::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Normal", "LingDong")
            .build()
            .getModeValue();

    // 拦截队列和状态变量
    public boolean btwork = false;
    private final LinkedBlockingDeque<Packet<?>> airKBQueue = new LinkedBlockingDeque<>();
    private final List<Integer> knockbackPositions = new ArrayList<>();
    private boolean isInterceptingAirKB = false;
    private int interceptedPacketCount = 0;
    private int delayTicks = 0;
    private boolean shouldCheckGround = false;

    // 进度条设置
    private static final float PROGRESS_BAR_WIDTH = 200.0f;  // 进度条总宽度
    private static final float PROGRESS_BAR_HEIGHT = 10.0f;  // 进度条高度
    private static final float PROGRESS_BAR_Y_OFFSET = 100.0f; // 在屏幕中心下方的偏移量
    private static final int BACKGROUND_COLOR = 0x80FFFFFF;  // 背景颜色 (半透明白色)
    private static final int PROGRESS_COLOR = 0xFF00FF00;    // 进度颜色 (绿色)
    private static final int OVERFLOW_COLOR = 0xFF00FF00;    // 溢出部分颜色 (绿色)
    private static final float CORNER_RADIUS = 3.0f;         // 圆角半径

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    public int getPacketCount() {
        return airKBQueue.size();
    }

    public void reset() {
        // 释放所有拦截的包
        releaseAirKBQueue();

        // 重置状态
        isInterceptingAirKB = false;
        interceptedPacketCount = 0;
        delayTicks = 0;
        shouldCheckGround = false;
        btwork = false;
        knockbackPositions.clear();
    }

    private void releaseAirKBQueue() {
        int packetCount = airKBQueue.size();
        while (!this.airKBQueue.isEmpty()) {
            try {
                Packet<?> packet = this.airKBQueue.poll();
                if (packet != null && mc.getConnection() != null) {
                    ((Packet<ClientGamePacketListener>) packet).handle(mc.getConnection());
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }

        // 记录日志
        if (packetCount > 0) {
            log("释放了 " + packetCount + " 个拦截的包");
        }

        // 重置计数器
        interceptedPacketCount = 0;
        knockbackPositions.clear();
    }

    private boolean hasNearbyPlayers(float range) {
        if (mc.level == null || mc.player == null) return false;

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue; // 跳过自己
            if (player.isAlive() && mc.player.distanceTo(player) <= range) {
                return true;
            }
        }
        return false;
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("[Backtrack] " + message);
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null) return;

        // 更新工作状态
        btwork = isInterceptingAirKB || shouldCheckGround;

        // 处理冷却延迟
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        // 检查是否应该开始拦截
        if (!isInterceptingAirKB && hasNearbyPlayers(range.getCurrentValue())) {
            isInterceptingAirKB = true;
            shouldCheckGround = false;
            interceptedPacketCount = 0;
            airKBQueue.clear();
            knockbackPositions.clear();
            log("检测到附近玩家，开始拦截包");
        }

        // 处理达到最大包数量的情况
        if (isInterceptingAirKB && interceptedPacketCount >= maxpacket.getCurrentValue()) {
            if (OnGroundStop.getCurrentValue()) {
                shouldCheckGround = true;
                log("达到最大包数量，等待玩家落地");
            } else {
                log("达到最大包数量，立即释放包");
                releaseAirKBQueue();
                resetAfterRelease();
            }
        }

        // 检查是否需要释放包（当需要等待落地时）
        if (shouldCheckGround && mc.player.onGround()) {
            log("玩家已落地，释放拦截的包");
            releaseAirKBQueue();
            resetAfterRelease();
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled()) {
            // 渲染进度条和文本
            this.render(event.getGuiGraphics());
        }
    }

    private void resetAfterRelease() {
        isInterceptingAirKB = false;
        shouldCheckGround = false;
        delayTicks = (int) delay.getCurrentValue();
        log("进入冷却延迟: " + delayTicks + " ticks");
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.getConnection() == null || !isInterceptingAirKB) {
            return;
        }

        // 只处理接收包
        if (event.getType() != EventType.RECEIVE) {
            return;
        }

        Packet<?> packet = event.getPacket();

        // 位置包会触发停止拦截并释放所有包
        if (packet instanceof ClientboundPlayerPositionPacket) {
            event.setCancelled(true);
            isInterceptingAirKB = false;
            shouldCheckGround = false;
            log("收到位置包，停止拦截并释放所有包");
            releaseAirKBQueue();
            resetAfterRelease();
        }
        // 处理击退包
        else if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() == mc.player.getId()) {
                event.setCancelled(true);
                airKBQueue.add(packet);
                interceptedPacketCount++;
                knockbackPositions.add(airKBQueue.size() - 1);
                log("拦截击退包 #" + interceptedPacketCount);
            }
        }
        // 拦截其他所有包
        else {
            event.setCancelled(true);
            airKBQueue.add(packet);
            interceptedPacketCount++;
            log("拦截普通包 #" + interceptedPacketCount);
        }
    }


    public void render(GuiGraphics guiGraphics) {
        if (!isInterceptingAirKB && !shouldCheckGround) return;

        if (!btrendermode.isCurrentMode("Normal")){
            return;
        }
        if (!btrender.getCurrentValue()){
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float x = (screenWidth - PROGRESS_BAR_WIDTH) / 2.0f;
        float y = screenHeight / 2.0f + PROGRESS_BAR_Y_OFFSET;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float maxPacketValue = Math.max(1.0f, maxpacket.getCurrentValue());
        float progress = Math.min(1.0f, interceptedPacketCount / maxPacketValue);
        float progressWidth = PROGRESS_BAR_WIDTH * progress;

        RenderUtils.drawRoundedRect(poseStack, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, BACKGROUND_COLOR);

        if (progressWidth > 0) {
            RenderUtils.drawRoundedRect(poseStack, x, y, progressWidth, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, PROGRESS_COLOR);
        }

        if (OnGroundStop.getCurrentValue() && interceptedPacketCount > maxpacket.getCurrentValue()) {
            float overflowProgress = (interceptedPacketCount - maxpacket.getCurrentValue()) / maxPacketValue;
            float overflowWidth = Math.min(PROGRESS_BAR_WIDTH * overflowProgress, PROGRESS_BAR_WIDTH);
            RenderUtils.drawRoundedRect(poseStack,
                    x + PROGRESS_BAR_WIDTH - overflowWidth,
                    y,
                    overflowWidth,
                    PROGRESS_BAR_HEIGHT,
                    CORNER_RADIUS,
                    OVERFLOW_COLOR);
        }

        // 使用NewNotification的字体和样式渲染文本
        String trackingText = "Tracking...";
        float textScale = 0.35f;
        float textWidth = Fonts.harmony.getWidth(trackingText, textScale);
        float textX = (screenWidth - textWidth) / 2.0f;
        float textY = y - 25f; // 稍微上移避免重叠

        // 使用NewNotification的渲染方式（黑色文字带阴影）
        Fonts.harmony.render(
                poseStack,
                trackingText,
                (double) textX,
                (double) textY,
                Color.WHITE, // 使用黑色文本
                false,        // 启用阴影
                textScale
        );

        poseStack.popPose();
    }
}