package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.ui.MainUI.MainUI;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.BlurInit2;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import javax.annotation.Nonnull;

public class Welcome extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("heypixel", "textures/images/background.png");

    private int fadeInStage = 0;
    private int fadeAlpha = 0;
    private static final int FADE_IN_DURATION = 30;
    private static final int FADE_OUT_DURATION = 30;
    private static final int MAX_ALPHA = 255;
    private boolean textureLoaded = false;
    private int blurStrength = 3;

    public Welcome() {
        super(Component.literal("Welcome"));
    }

    @Override
    protected void init() {
        super.init();
        textureLoaded = checkTextureLoaded();
        BlurInit2.init();
    }

    private boolean checkTextureLoaded() {
        try {
            Minecraft.getInstance().getResourceManager().getResourceOrThrow(BACKGROUND_TEXTURE);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load background texture: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void tick() {
        switch (fadeInStage) {
            case 0: // Fading in
                fadeAlpha += (MAX_ALPHA / FADE_IN_DURATION);
                if (fadeAlpha >= MAX_ALPHA) {
                    fadeAlpha = MAX_ALPHA;
                    fadeInStage = 1; // Waiting for input
                }
                break;
            case 1: // Waiting for user input
                break;
            case 2: // Fading out
                fadeAlpha -= (MAX_ALPHA / FADE_OUT_DURATION);
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    fadeInStage = 3; // Done
                    if (this.minecraft != null) {
                        // IRC登录完成后直接跳转到MainUI
                        this.minecraft.setScreen(new MainUI());
                    }
                }
                break;
            case 3: // Screen is done
                break;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderBlur(guiGraphics);
        renderText(guiGraphics);
    }

    private void renderBlur(GuiGraphics guiGraphics) {
        if (fadeAlpha > 0 && fadeInStage < 3) {
            float blurOpacity = fadeAlpha / 255.0f;
            int dynamicBlurStrength = (int)(blurStrength * blurOpacity);
            BlurInit2.renderBlur(guiGraphics, dynamicBlurStrength);
        }
    }

    public void renderBackground(@Nonnull GuiGraphics guiGraphics) {
        Window window = Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        // Render solid black background as a fallback
        guiGraphics.fill(0, 0, width, height, 0xFF000000);

        if (textureLoaded) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // The image is rendered with the fade-in alpha
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha / 255.0F);
            guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, width, height, width, height);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset shader color
        }

        // Render a darkening overlay to improve text contrast
        int overlayAlpha = (int)((fadeAlpha / 255.0f) * 150); // ~60% black overlay at full alpha
        int overlayColor = (overlayAlpha << 24); // Black color

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(0, 0, width, height, overlayColor);
        RenderSystem.disableBlend();
    }

    private void renderText(GuiGraphics guiGraphics) {
        int textAlpha = fadeAlpha;
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
            fadeInStage = 2; // Start fading out
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fadeInStage == 1) {
            fadeInStage = 2; // Start fading out
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}