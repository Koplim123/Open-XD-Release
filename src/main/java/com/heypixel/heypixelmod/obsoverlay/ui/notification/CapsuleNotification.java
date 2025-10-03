package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;

/**
 * Capsule风格通知 - 使用模糊背景
 */
public class CapsuleNotification extends Notification {
    private static final float CORNER_RADIUS = 6.0F;

    /** 控制通知的固定总宽度 */
    private static final float FIXED_WIDTH = 180.0F;
    private static final float LEFT_PADDING = 10.0F;
    private static final float VERTICAL_PADDING = 10.0F;
    private static final float HEIGHT = 50.0F;

    private final String title;

    public CapsuleNotification(NotificationLevel level, String message, long age) {
        super(level, message, age);
        this.title = "Module";
    }

    @Override
    public void renderShader(PoseStack stack, float x, float y) {
        // 在Shader通道中渲染blur蒙版
        // 使用Integer.MIN_VALUE作为颜色来标记需要模糊的区域
        RenderUtils.drawRoundedRect(stack, x, y, this.getWidth(), this.getHeight(), CORNER_RADIUS, Integer.MIN_VALUE);
    }

    @Override
    public void render(PoseStack stack, float x, float y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        GuiGraphics guiGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        guiGraphics.pose().last().pose().mul(stack.last().pose());

        // blur效果会由shader系统提供，这里只渲染文字
        
        // 使用 LEFT_PADDING 和 VERTICAL_PADDING 来定位文字
        float textX = x + LEFT_PADDING;
        float textY = y + VERTICAL_PADDING;

        Fonts.harmony.render(guiGraphics.pose(), this.title, textX, textY, Color.WHITE, true, 0.35f);
        Fonts.harmony.render(guiGraphics.pose(), "Module " + this.getMessage(), textX, textY + Fonts.harmony.getHeight(true, 0.35f) + 4, new Color(180, 180, 180), true, 0.3f);
    }

    @Override
    public float getWidth() {
        // 计算文本实际需要的宽度
        float titleWidth = Fonts.harmony.getWidth(this.title, 0.35f);
        float messageWidth = Fonts.harmony.getWidth("Module " + this.getMessage(), 0.3f);
        float textWidth = Math.max(titleWidth, messageWidth);
        float requiredContentWidth = LEFT_PADDING + textWidth + LEFT_PADDING;

        // 返回固定宽度和实际需要宽度中的较大值
        return Math.max(FIXED_WIDTH, requiredContentWidth);
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }
}
