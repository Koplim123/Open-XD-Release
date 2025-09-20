package com.heypixel.heypixelmod.obsoverlay.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.BlurUtils;

public class Welcome extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("heypixel", "textures/images/background.jpg");

    private int fadeInStage = 0;
    private int fadeAlpha = 0;
    private static final int FADE_IN_DURATION = 30;
    private static final int FADE_OUT_DURATION = 30;
    private static final int MAX_ALPHA = 255;
    private boolean textureLoaded = false;

    public Welcome() {
        super(Component.literal("Welcome"));
    }

    @Override
    protected void init() {
        super.init();
        textureLoaded = checkTextureLoaded();
    }

    private boolean checkTextureLoaded() {
        try {
            Minecraft.getInstance().getResourceManager().getResource(BACKGROUND_TEXTURE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void tick() {
        switch (fadeInStage) {
            case 0:
                fadeAlpha += (MAX_ALPHA / FADE_IN_DURATION);
                if (fadeAlpha >= MAX_ALPHA) {
                    fadeAlpha = MAX_ALPHA;
                    fadeInStage = 1;
                }
                break;
            case 1:
                break;
            case 2:
                fadeAlpha -= (MAX_ALPHA / FADE_OUT_DURATION);
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    fadeInStage = 3;
                    this.minecraft.setScreen(null);
                }
                break;
            case 3:
                break;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        renderAdvancedBlurredBackground(guiGraphics);
        renderText(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderBlurredBackground(GuiGraphics guiGraphics) {
        try {
            Window window = Minecraft.getInstance().getWindow();
            int width = window.getGuiScaledWidth();
            int height = window.getGuiScaledHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (fadeAlpha * 0.7f) / 255.0F);
            
            if (textureLoaded) {
                guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, width, height, width, height);
            } else {
                guiGraphics.fillGradient(0, 0, width, height, 0x80000000 | (fadeAlpha << 24), 0x80000000 | (fadeAlpha << 24));
            }
            
            RenderSystem.disableBlend();
        } catch (Exception e) {
            System.err.println("Error rendering blurred background: " + e.getMessage());
        }
    }

    private void renderTrueBlurredBackground(GuiGraphics guiGraphics) {
        try {
            Window window = Minecraft.getInstance().getWindow();
            int width = window.getGuiScaledWidth();
            int height = window.getGuiScaledHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            
            int baseAlpha = (int)(fadeAlpha * 0.8f);
            
            guiGraphics.fill(0, 0, width, height, (0x15 << 24) | (baseAlpha << 24));
            
            int steps = 5;
            for (int i = 1; i <= steps; i++) {
                int blurAlpha = baseAlpha / (i + 1);
                int offset = i * 3;
                int color = (0x08 << 24) | (blurAlpha << 24);
                
                guiGraphics.fill(-offset, -offset, width + offset, height + offset, color);
                guiGraphics.fill(offset, offset, width - offset, height - offset, color);
            }
            
            if (textureLoaded) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, baseAlpha / 255.0F);
                guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, width, height, width, height);
            }
            
            RenderSystem.disableBlend();
        } catch (Exception e) {
            System.err.println("Error rendering true blurred background: " + e.getMessage());
            renderBlurredBackground(guiGraphics);
        }
    }

    private void renderAdvancedBlurredBackground(GuiGraphics guiGraphics) {
        try {
            Window window = Minecraft.getInstance().getWindow();
            int width = window.getGuiScaledWidth();
            int height = window.getGuiScaledHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            
            int alpha = (int)(fadeAlpha * 0.85f);
            
            guiGraphics.fill(0, 0, width, height, (0x1A << 24) | (alpha << 24));
            
            int[] offsets = {1, 2, 3, 4, 6};
            int[] alphas = {alpha / 3, alpha / 4, alpha / 6, alpha / 8, alpha / 10};
            
            for (int i = 0; i < offsets.length; i++) {
                int offset = offsets[i];
                int blurColor = (0x05 << 24) | (alphas[i] << 24);
                
                guiGraphics.fill(-offset, -offset, width + offset, height + offset, blurColor);
                guiGraphics.fill(offset, offset, width - offset, height - offset, blurColor);
                
                guiGraphics.fill(-offset, offset, width + offset, height - offset, blurColor);
                guiGraphics.fill(offset, -offset, width - offset, height + offset, blurColor);
            }
            
            if (textureLoaded) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
                guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, width, height, width, height);
            }
            
            RenderSystem.disableBlend();
        } catch (Exception e) {
            System.err.println("Error rendering advanced blurred background: " + e.getMessage());
            renderTrueBlurredBackground(guiGraphics);
        }
    }



    private void renderText(GuiGraphics guiGraphics) {
        int textAlpha = Math.min(fadeAlpha + 80, 255);
        int shadowAlpha = textAlpha / 2;
        int textColor = 0xFFFFFF | (textAlpha << 24);
        int shadowColor = 0x000000 | (shadowAlpha << 24);
        
        String title = "Welcome to Naven-XD Client";
        String subtitle = "Press any key to continue";
        
        int titleY = this.height / 2 - 10;
        int subtitleY = this.height / 2 + 10;
        
        guiGraphics.drawCenteredString(this.font, title, this.width / 2 + 1, titleY + 1, shadowColor);
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, titleY, textColor);
        
        guiGraphics.drawCenteredString(this.font, subtitle, this.width / 2 + 1, subtitleY + 1, shadowColor);
        guiGraphics.drawCenteredString(this.font, subtitle, this.width / 2, subtitleY, textColor);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fadeInStage == 1) {
            fadeInStage = 2;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fadeInStage == 1) {
            fadeInStage = 2;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
    }
}