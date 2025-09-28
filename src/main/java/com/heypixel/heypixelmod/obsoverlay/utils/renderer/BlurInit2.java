package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BlurInit2 {
    private static boolean initialized = false;
    
    public static void init() {
        if (!initialized) {
            initialized = true;
        }
    }
    
    public static void renderBlur(GuiGraphics guiGraphics, int blurStrength) {
        if (blurStrength <= 0) return;
        
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int layers = Math.min(blurStrength, 5);
        float baseAlpha = 0.08f;
        
        for (int layer = 0; layer < layers; layer++) {
            int offset = (layer + 1) * 2;
            float alpha = baseAlpha * (1.0f - layer * 0.15f);
            int color = ((int)(alpha * 255) << 24) | 0x101010;
            
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            
            guiGraphics.fill(-offset, -offset, width + offset, height + offset, color);
            guiGraphics.fill(offset, -offset, width - offset, height + offset, color);
            guiGraphics.fill(-offset, offset, width + offset, height - offset, color);
            guiGraphics.fill(offset, offset, width - offset, height - offset, color);
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    
    public static void cleanup() {
        initialized = false;
    }
}