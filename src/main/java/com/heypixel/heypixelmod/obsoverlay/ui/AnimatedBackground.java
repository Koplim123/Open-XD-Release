package com.heypixel.heypixelmod.obsoverlay.ui;

import net.minecraft.resources.ResourceLocation;

public class AnimatedBackground {
    private static final String FRAME_LOCATION_PATTERN = "heypixel:textures/ui/background/frame_%04d.jpg";
    private final int frameCount;
    private int currentFrame = 1;
    private int tickCounter = 0;
    private final int ticksPerFrame;

    /**
     * @param frameCount 动画的总帧数
     * @param fps 动画的帧率 (每秒播放多少帧)
     */
    public AnimatedBackground(int frameCount, int fps) {
        this.frameCount = frameCount;
        // Minecraft 每秒 20 tick, 计算每隔多少 tick 更新一帧
        this.ticksPerFrame = Math.max(1, 10 / fps);
    }

    public void tick() {
        tickCounter++;
        if (tickCounter >= ticksPerFrame) {
            tickCounter = 0;
            currentFrame++;
            if (currentFrame > frameCount) {
                currentFrame = 1; // 循环播放
            }
        }
    }

    public ResourceLocation getCurrentFrameLocation() {
        String framePath = String.format(FRAME_LOCATION_PATTERN, currentFrame);
        return new ResourceLocation(framePath);
    }
}
