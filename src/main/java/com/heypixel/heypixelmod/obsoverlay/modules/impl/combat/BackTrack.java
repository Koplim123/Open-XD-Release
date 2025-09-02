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
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
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

/**
 * BetaBackTrack模块 - 用于拦截和延迟网络数据包以获得战斗优势
 * 该模块通过拦截击退数据包和其他网络包来实现"回溯"效果
 */
@ModuleInfo(
        name = "BackTrack",
        description = "Stuck Network,but adversaries",
        category = Category.COMBAT
)
public class BackTrack extends Module {
    // 日志开关 - 控制是否在聊天栏显示模块操作日志
    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
            
    // 着陆停止功能 - 当达到最大包数量时是否等待玩家着陆再释放包
    public BooleanValue OnGroundStop = ValueBuilder.create(this, "OnGroundStop")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
            
    // 最大包数量 - 控制拦截的最大数据包数量
    public FloatValue maxpacket = ValueBuilder.create(this, "Max Packet number")
            .setDefaultFloatValue(45F)
            .setFloatStep(5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(450F)
            .build()
            .getFloatValue();
            
    // 范围设置 - 控制检测附近玩家的距离范围
    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(3F)
            .setFloatStep(0.5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(6F)
            .build()
            .getFloatValue();
            
    // 延迟设置 - 控制释放包后的延迟时间(以tick为单位)
    FloatValue delay = ValueBuilder.create(this, "Delay(Tick)")
            .setDefaultFloatValue(20F)
            .setFloatStep(1F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(200F)
            .build()
            .getFloatValue();
            
    // 渲染开关 - 控制是否显示可视化界面
    public BooleanValue btrender = ValueBuilder.create(this, "Render")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
            
    // 渲染模式 - 提供不同的可视化界面选项
    public ModeValue btrendermode = ValueBuilder.create(this, "Render Mode")
            .setVisibility(this.btrender::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Normal", "LingDong")
            .build()
            .getModeValue();

    // 模块工作状态标志
    public boolean btwork = false;
    
    // 空中击退包队列 - 存储被拦截的数据包
    private final LinkedBlockingDeque<Packet<?>> airKBQueue = new LinkedBlockingDeque<>();
    
    // 击退位置列表 - 记录击退包在队列中的位置
    private final List<Integer> knockbackPositions = new ArrayList<>();
    
    // 空中击退拦截状态 - 标记是否正在拦截空中击退
    private boolean isInterceptingAirKB = false;
    
    // 已拦截包计数 - 统计当前已拦截的数据包数量
    private int interceptedPacketCount = 0;
    
    // 延迟计时器 - 控制释放包后的延迟时间
    private int delayTicks = 0;
    
    // 地面检测标志 - 标记是否需要等待玩家着陆
    private boolean shouldCheckGround = false;

    // 进度条设置
    private static final float PROGRESS_BAR_WIDTH = 200.0f;  // 进度条总宽度
    private static final float PROGRESS_BAR_HEIGHT = 10.0f;  // 进度条高度
    private static final float PROGRESS_BAR_Y_OFFSET = 65.0f; // 在准星下方65px处的偏移量
    private static final int BACKGROUND_COLOR = 0x80000000;   // 背景颜色 (半透明黑色)
    private static final int PROGRESS_COLOR = 0xFF66CCFF;    // 进度颜色 (天蓝色)
    private static final int OVERFLOW_COLOR = 0xFFFF6B6B;    // 溢出部分颜色 (红色)
    private static final float CORNER_RADIUS = 5.0f;         // 圆角半径
    private static final int BLUR_STRENGTH = 3;              // 高斯模糊强度

    /**
     * 模块启用时调用 - 重置模块状态
     */
    @Override
    public void onEnable() {
        reset();
    }

    /**
     * 模块禁用时调用 - 重置模块状态
     */
    @Override
    public void onDisable() {
        releaseAllPacketQueue();
        reset();
    }

    /**
     * 释放所有拦截的数据包
     * 此方法会处理并发送队列中所有被拦截的数据包
     */
    public void releaseAllPacketQueue() {
        releaseAirKBQueue();
    }

    /**
     * 获取当前拦截的数据包数量
     * @return 当前队列中的数据包数量
     */
    public int getPacketCount() {
        return airKBQueue.size();
    }

    /**
     * 重置模块所有状态
     */
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

    /**
     * 释放所有拦截的空中击退包
     */
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
            log("Release " + packetCount + " Packets");
        }

        // 重置计数器
        interceptedPacketCount = 0;
        knockbackPositions.clear();
    }

    /**
     * 检查附近是否有玩家在指定范围内
     * @param range 检测范围
     * @return 如果有附近玩家返回true，否则返回false
     */
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

    /**
     * 记录日志信息（如果日志功能已启用）
     * @param message 要记录的消息
     */
    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("[Backtrack] " + message);
        }
    }

    /**
     * 游戏每tick调用 - 处理模块逻辑更新
     * @param event tick事件
     */
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
            log("Checked nearby players, start intercepting packets");
        }

        // 处理达到最大包数量的情况
        if (isInterceptingAirKB && interceptedPacketCount >= maxpacket.getCurrentValue()) {
            if (OnGroundStop.getCurrentValue()) {
                shouldCheckGround = true;
                log("Max Packet number reached, waiting to land before releasing packets");
            } else {
                log("Release Packet");
                releaseAirKBQueue();
                resetAfterRelease();
            }
        }

        // 检查是否需要释放包（当需要等待落地时）
        if (shouldCheckGround && mc.player.onGround()) {
            log("Release Packet");
            releaseAirKBQueue();
            resetAfterRelease();
        }
    }

    /**
     * 2D渲染事件处理 - 渲染可视化界面
     * @param event 渲染事件
     */
    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled()) {
            // 渲染进度条和文本
            this.render(event.getGuiGraphics());
        }
    }

    /**
     * 释放包后的重置操作
     */
    private void resetAfterRelease() {
        isInterceptingAirKB = false;
        shouldCheckGround = false;
        delayTicks = (int) delay.getCurrentValue();
        log("Delay: " + delayTicks + " ticks");
    }

    /**
     * 数据包事件处理 - 拦截和处理网络数据包
     * @param event 数据包事件
     */
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
            log("Received S2C Packet, releasing all packets");
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
                log("Intercepted Packet #" + interceptedPacketCount);
            }
        }
        // 拦截其他所有包
        else {
            event.setCancelled(true);
            airKBQueue.add(packet);
            interceptedPacketCount++;
            log("Intercepted Packet #" + interceptedPacketCount);
        }
    }

    /**
     * 渲染可视化界面
     * @param guiGraphics GUI图形上下文
     */
    public void render(GuiGraphics guiGraphics) {
        // 如果未处于拦截状态则不渲染
        if (!isInterceptingAirKB && !shouldCheckGround) return;

        // 检查渲染模式和渲染开关
        if (!btrendermode.isCurrentMode("Normal")){
            return;
        }
        if (!btrender.getCurrentValue()){
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 计算屏幕中心位置
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float x = (screenWidth - PROGRESS_BAR_WIDTH) / 2.0f;
        float y = screenHeight / 2.0f + PROGRESS_BAR_Y_OFFSET;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 计算进度条显示参数
        float maxPacketValue = Math.max(1.0f, maxpacket.getCurrentValue());
        float progress = Math.min(1.0f, interceptedPacketCount / maxPacketValue);
        float progressWidth = PROGRESS_BAR_WIDTH * progress;

        // 绘制毛玻璃高斯模糊背景
        RenderUtils.drawStencilRoundedRect(guiGraphics, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, BLUR_STRENGTH, BACKGROUND_COLOR);

        // 绘制进度条已完成部分
        if (progressWidth > 0) {
            RenderUtils.drawRoundedRect(poseStack, x, y, progressWidth, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, PROGRESS_COLOR);
        }

        // 如果启用了着陆停止且已超过最大包数量，绘制溢出部分
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

        // 渲染"Tracking..."文本（在进度条内部）
        String trackingText = "Tracking...";
        float textScale = 0.35f;
        float textWidth = Fonts.harmony.getWidth(trackingText, textScale);
        float textX = x + (PROGRESS_BAR_WIDTH - textWidth) / 2.0f;
        float textY = y + (PROGRESS_BAR_HEIGHT - (float)Fonts.harmony.getHeight(false, textScale)) / 2.0f;

        // 使用自定义字体渲染文本
        Fonts.harmony.render(
                poseStack,
                trackingText,
                (double) textX,
                (double) textY,
                Color.WHITE, // 使用白色文本
                false,        // 启用阴影
                textScale
        );

        poseStack.popPose();
    }
}