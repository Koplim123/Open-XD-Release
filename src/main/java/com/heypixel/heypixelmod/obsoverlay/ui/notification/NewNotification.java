package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;

public class NewNotification extends Notification {
    private static final int BACKGROUND_COLOR = new Color(21, 22, 25, 220).getRGB();
    private static final int PROGRESS_BAR_COLOR = new Color(50, 100, 255).getRGB();
    private static final float CORNER_RADIUS = 6.0F; // 适中的圆角半径

    // --- [修改] 使用固定宽度和独立的边距常量 ---

    /** 控制通知的固定总宽度 */
    private static final float FIXED_WIDTH = 180.0F;
    private static final float LEFT_PADDING = 10.0F;
    private static final float VERTICAL_PADDING = 10.0F;

    private static final float PROGRESS_BAR_HEIGHT = 2.0F;
    private static final float HEIGHT = 50.0F;

    private final String title;

    public NewNotification(NotificationLevel level, String message, long age) {
        super(level, message, age);
        this.title = "Module";
    }

    @Override
    public void renderShader(PoseStack stack, float x, float y) {
        RenderUtils.fill(stack, x, y, x + this.getWidth(), y + this.getHeight(), BACKGROUND_COLOR);
    }

    @Override
    public void render(PoseStack stack, float x, float y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        GuiGraphics guiGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        guiGraphics.pose().last().pose().mul(stack.last().pose());

        RenderUtils.fill(stack, x, y, x + this.getWidth(), y + this.getHeight(), BACKGROUND_COLOR);

        // 使用 LEFT_PADDING 和 VERTICAL_PADDING 来定位文字
        float textX = x + LEFT_PADDING;
        float textY = y + VERTICAL_PADDING;

        Fonts.harmony.render(guiGraphics.pose(), this.title, textX, textY, Color.WHITE, true, 0.35f);
        Fonts.harmony.render(guiGraphics.pose(), "Module " + this.getMessage(), textX, textY + Fonts.harmony.getHeight(true, 0.35f) + 4, new Color(180, 180, 180), true, 0.3f);

        float lifeTime = (float)(System.currentTimeMillis() - this.getCreateTime());
        float progress = Math.min(lifeTime / (float)this.getMaxAge(), 1.0F);
        float progressBarWidth = this.getWidth() * progress;
        RenderUtils.fill(stack, x, y + this.getHeight() - PROGRESS_BAR_HEIGHT, x + progressBarWidth, y + this.getHeight(), PROGRESS_BAR_COLOR);
    }

    @Override
    public float getWidth() {
        // [修复] 使用固定宽度，但如果文本太长，则自动扩展以防止溢出

        // 计算文本实际需要的宽度
        float titleWidth = Fonts.harmony.getWidth(this.title, 0.35f);
        float messageWidth = Fonts.harmony.getWidth("Module " + this.getMessage(), 0.3f);
        float textWidth = Math.max(titleWidth, messageWidth);
        // 左右都使用 LEFT_PADDING，确保长文本也能有对称的边距
        float requiredContentWidth = LEFT_PADDING + textWidth + LEFT_PADDING;

        // 返回固定宽度和实际需要宽度中的较大值
        return Math.max(FIXED_WIDTH, requiredContentWidth);
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }
}