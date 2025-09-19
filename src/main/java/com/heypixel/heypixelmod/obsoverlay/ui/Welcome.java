package com.heypixel.heypixelmod.obsoverlay.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Welcome extends Screen {
    // 淡入阶段: 0=初始黑色, 1=显示文字, 2=淡出, 3=完成
    private int fadeInStage = 0;
    private int fadeAlpha = 0; // 当前透明度 (0-255)
    // Minecraft每秒运行约20 tick，所以2.75秒 = 55 ticks
    private static final int FADE_IN_DURATION = 27;
    private static final int FADE_OUT_DURATION = 27;
    private static final int MAX_ALPHA = 255;        // 最大透明度

    // 动态背景
    // 假设视频被转换为300帧 (15秒 @ 20fps)
    private AnimatedBackground animatedBackground = new AnimatedBackground(300, 20);

    public Welcome() {
        super(Component.literal("Welcome"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void tick() {
        // 更新动态背景
        animatedBackground.tick();

        switch (fadeInStage) {
            case 0: // 淡入阶段，同时显示背景和文字
                fadeAlpha += (MAX_ALPHA / FADE_IN_DURATION);
                if (fadeAlpha >= MAX_ALPHA) {
                    fadeAlpha = MAX_ALPHA;
                    fadeInStage = 1;
                }
                break;
            case 1: // 显示文字阶段，等待用户输入
                // 等待用户按键或点击
                break;
            case 2: // 淡出阶段
                fadeAlpha -= (MAX_ALPHA / FADE_OUT_DURATION);
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    fadeInStage = 3;
                    // 淡出完成后进入客户端
                    this.minecraft.setScreen(null);
                }
                break;
            case 3: // 完成
                break;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 绘制动态背景 (带透明度)
        float alpha = fadeAlpha / 255.0F;
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        ResourceLocation currentFrame = animatedBackground.getCurrentFrameLocation();
        RenderSystem.setShaderTexture(0, currentFrame);
        guiGraphics.blit(currentFrame, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        // 绘制半透明黑色叠加层以模拟亚克力模糊效果
        int overlayColor = ((int)(alpha * 128)) << 24; // 50% 透明度的黑色
        guiGraphics.fill(0, 0, this.width, this.height, overlayColor);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制文字 (带透明度)
        int textColor = (fadeAlpha << 24) | 0xFFFFFF;
        guiGraphics.drawCenteredString(this.font, "Welcome to Naven-XD Client", this.width / 2, this.height / 2 - 10, textColor);
        guiGraphics.drawCenteredString(this.font, "Press any key into Client", this.width / 2, this.height / 2 + 10, textColor);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 按任意键进入下一阶段
        if (fadeInStage == 1) {
            fadeInStage = 2; // 开始淡出
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击鼠标进入下一阶段
        if (fadeInStage == 1) {
            fadeInStage = 2; // 开始淡出
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}