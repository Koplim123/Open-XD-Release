package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class Notification {
    private static final ResourceLocation TRUE_ICON = ResourceLocation.parse("heypixel:textures/icons/true.png");
    private static final ResourceLocation CANCEL_ICON = ResourceLocation.parse("heypixel:textures/icons/cancel.png");

    private final String text;
    private final boolean enabled;
    private final long createTime;
    private final int maxAge;
    
    private final SmoothAnimationTimer widthTimer;
    private final SmoothAnimationTimer heightTimer;
    
    private float width;
    private float height;

    public Notification(String text, boolean enabled) {
        this.text = text;
        this.enabled = enabled;
        this.createTime = System.currentTimeMillis();
        this.maxAge = 1500;
        
        this.widthTimer = new SmoothAnimationTimer(0f, 0f, 0.8f);
        this.heightTimer = new SmoothAnimationTimer(0f, 0f, 0.8f);
        

        this.width = 30f + Minecraft.getInstance().font.width(text) + 30f;
        this.height = 30f;
    }

    public void renderShader(PoseStack poseStack, float x, float y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GuiGraphics guiGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        guiGraphics.pose().last().pose().set(poseStack.last().pose());
        


        guiGraphics.fill((int)x, (int)y, (int)(x + width), (int)(y + height), 0xAA000000);
        

        if (enabled) {
            guiGraphics.fill((int)x, (int)y, (int)(x + width), (int)y + 2, 0xFF00FF00);
            guiGraphics.fill((int)x, (int)y, (int)x + 2, (int)(y + height), 0xFF00FF00);
            guiGraphics.fill((int)(x + width - 2), (int)y, (int)(x + width), (int)(y + height), 0xFF00FF00);
            guiGraphics.fill((int)x, (int)(y + height - 2), (int)(x + width), (int)(y + height), 0xFF00FF00);
        } else {
            guiGraphics.fill((int)x, (int)y, (int)(x + width), (int)y + 2, 0xFFFF0000);
            guiGraphics.fill((int)x, (int)y, (int)x + 2, (int)(y + height), 0xFFFF0000);
            guiGraphics.fill((int)(x + width - 2), (int)y, (int)(x + width), (int)(y + height), 0xFFFF0000);
            guiGraphics.fill((int)x, (int)(y + height - 2), (int)(x + width), (int)(y + height), 0xFFFF0000);
        }


        guiGraphics.drawString(
            Minecraft.getInstance().font,
            text,
            (int)(x + 20),
            (int)(y + (height - 8) / 2),
            enabled ? 0x00FF00 : 0xFF0000,
            false
        );

        ResourceLocation icon = enabled ? TRUE_ICON : CANCEL_ICON;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(icon, (int)(x + width - 25), (int)(y + (height - 16) / 2), 0, 0, 16, 16, 16, 16);
        
        RenderSystem.disableBlend();
    }

    public SmoothAnimationTimer getWidthTimer() {
        return widthTimer;
    }

    public SmoothAnimationTimer getHeightTimer() {
        return heightTimer;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public long getCreateTime() {
        return createTime;
    }

    public int getMaxAge() {
        return maxAge;
    }
}