package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class IRCLoginScreen extends Screen {
    private EditBox usernameField;
    private EditBox passwordField;
    private Button loginButton;
    private Button registerButton;
    private Component errorText = Component.empty();
    private boolean loggingIn = false;
    private boolean loggedInSuccess = false; // Add: Flag to check if login was successful
    private long loginSuccessTime = 0;       // Add: Timestamp for login success

    public IRCLoginScreen() {
        super(Component.literal("IRC Login"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.usernameField = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.literal("Username"));
        this.usernameField.setMaxLength(32);
        this.addRenderableWidget(this.usernameField);

        this.passwordField = new EditBox(this.font, centerX - 100, centerY, 200, 20, Component.literal("Password"));
        this.passwordField.setMaxLength(32);
        this.passwordField.setHint(Component.literal("Password"));
        this.passwordField.setBordered(true);
        this.addRenderableWidget(this.passwordField);

        this.loginButton = Button.builder(Component.literal("Login"), (p_290168_) -> {
            attemptLogin();
        }).bounds(centerX - 100, centerY + 30, 200, 20).build();
        this.addRenderableWidget(this.loginButton);

        this.registerButton = Button.builder(Component.literal("Register"), (p_290168_) -> {
            IRCLoginManager.openRegisterPage();
        }).bounds(centerX - 100, centerY + 60, 200, 20).build();
        this.addRenderableWidget(this.registerButton);

        this.usernameField.setFocused(true);
    }

    @Override
    public void tick() {
        if (this.loggedInSuccess) {
            if (System.currentTimeMillis() - this.loginSuccessTime >= 1000) {
                this.onClose();
            }
        }

        if (!this.loggedInSuccess) {
            this.usernameField.tick();
            this.passwordField.tick();
            this.loginButton.active = !this.loggingIn && !this.usernameField.getValue().isEmpty() && !this.passwordField.getValue().isEmpty();
        }
    }

    private void attemptLogin() {
        this.loggingIn = true;
        this.errorText = Component.empty();
        this.loginButton.active = false;

        String username = this.usernameField.getValue();
        String password = this.passwordField.getValue();

        new Thread(() -> {
            boolean success = IRCLoginManager.login(username, password);
            this.minecraft.execute(() -> {
                if (success) {
                    this.loggedInSuccess = true;
                    this.loginSuccessTime = System.currentTimeMillis();
                } else {
                    this.errorText = Component.literal("登陆失败，请检查账户名或密码");
                    this.loggingIn = false;
                    this.loginButton.active = true;
                }
            });
        }).start();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // No background image rendering, we will just fill with a solid color in the render method.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Step 1: Fill the entire screen with a solid white color.
        guiGraphics.fill(0, 0, this.width, this.height, 0xFFFFFFFF);

        // Step 2: Ensure the screen and its render states are reset.
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Render text and loading status, which should be on top of the UI components
        if (this.loggedInSuccess) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(2.0f, 2.0f, 2.0f);
            // If logged in, only render the success text
            String successText = "登陆成功";
            int successTextWidth = this.font.width(successText);
            int successTextX = (int) ((this.width / 2.0f - successTextWidth) / 2.0f);
            int successTextY = (int) ((this.height / 2.0f - this.font.lineHeight / 2.0f) / 2.0f);
            guiGraphics.drawString(this.font, successText, successTextX, successTextY, Color.GREEN.getRGB(), false);

            // Hide the widgets
            this.usernameField.visible = false;
            this.passwordField.visible = false;
            this.loginButton.visible = false;
            this.registerButton.visible = false;
        } else {
            // If not logged in, render all UI components
            super.render(guiGraphics, mouseX, mouseY, partialTicks);

            // Render the "IRC Login" title
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(2.0f, 2.0f, 2.0f);
            String titleText = "IRC Login";
            int titleTextWidth = this.font.width(titleText);
            int titleTextX = (int) ((this.width / 2.0f - titleTextWidth) / 2.0f);
            int titleTextY = (int) ((this.height / 2.0f - 60) / 2.0f - this.font.lineHeight / 2.0f);
            guiGraphics.drawString(this.font, titleText, titleTextX, titleTextY, Color.BLACK.getRGB(), false);
            guiGraphics.pose().popPose();

            // Render status messages
            String statusText = "";
            int statusColor = Color.BLACK.getRGB();

            if (this.loggingIn) {
                statusText = "登陆中...";
                statusColor = Color.CYAN.getRGB();
            } else if (!this.errorText.getString().isEmpty()) {
                statusText = this.errorText.getString();
                statusColor = Color.RED.getRGB();
            }

            if (!statusText.isEmpty()) {
                int statusTextWidth = this.font.width(statusText);
                int statusTextX = (this.width - statusTextWidth) / 2;
                int statusTextY = this.height / 2 + 90 - this.font.lineHeight / 2;
                guiGraphics.drawString(this.font, statusText, statusTextX, statusTextY, statusColor, true);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.loggedInSuccess) {
            return true;
        }

        if (keyCode == 256) {
            return true;
        }

        if (keyCode == 257 || keyCode == 335) {
            if (this.loginButton.active) {
                attemptLogin();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
