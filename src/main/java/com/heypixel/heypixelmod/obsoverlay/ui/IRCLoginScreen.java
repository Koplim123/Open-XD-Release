package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//谁在给我动就死妈了 这些代码能正常跑就行了 狗屎代码
public class IRCLoginScreen extends Screen {
    private EditBox usernameField;
    private EditBox passwordField;
    private Button loginButton;
    private Button registerButton;
    private Component errorText = Component.empty();
    private boolean loggingIn = false;

    public IRCLoginScreen() {
        super(Component.literal("IRC Login"));
    }

    @Override
    protected void init() {
        try {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            // 用户名输入框
            this.usernameField = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.literal("Username"));
            this.usernameField.setMaxLength(32);
            this.addRenderableWidget(this.usernameField);

            // 密码输入框
            this.passwordField = new EditBox(this.font, centerX - 100, centerY, 200, 20, Component.literal("Password"));
            this.passwordField.setMaxLength(32);
            this.passwordField.setHint(Component.literal("Password"));
            this.passwordField.setBordered(true);
            this.addRenderableWidget(this.passwordField);

            // 登录按钮
            this.loginButton = Button.builder(Component.literal("Login"), (p_290168_) -> {
                attemptLogin();
            }).bounds(centerX - 100, centerY + 30, 200, 20).build();
            this.addRenderableWidget(this.loginButton);

            // 注册按钮
            this.registerButton = Button.builder(Component.literal("Register"), (p_290168_) -> {
                IRCLoginManager.openRegisterPage();
            }).bounds(centerX - 100, centerY + 60, 200, 20).build();
            this.addRenderableWidget(this.registerButton);

            this.usernameField.setFocused(true);
        } catch (Exception e) {
            System.err.println("Error initializing IRC login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        try {
            this.usernameField.tick();
            this.passwordField.tick();
            this.loginButton.active = !this.loggingIn && !this.usernameField.getValue().isEmpty() && !this.passwordField.getValue().isEmpty();
        } catch (Exception e) {
            System.err.println("Error in IRC login screen tick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void attemptLogin() {
        try {
            this.loggingIn = true;
            this.errorText = Component.empty();
            this.loginButton.active = false;

            String username = this.usernameField.getValue();
            String password = this.passwordField.getValue();

            new Thread(() -> {
                try {
                    boolean success = IRCLoginManager.login(username, password);
                    this.minecraft.execute(() -> {
                        try {
                            if (success) {
                                // 登录成功，关闭当前屏幕
                                this.onClose();
                            } else {
                                // 登录失败，显示错误信息
                                this.errorText = Component.literal("Login failed. Please check your username and password.");
                                this.loggingIn = false;
                                this.loginButton.active = true;
                            }
                        } catch (Exception e) {
                            System.err.println("Error in login callback: " + e.getMessage());
                            e.printStackTrace();
                            this.loggingIn = false;
                            this.loginButton.active = true;
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error in login thread: " + e.getMessage());
                    e.printStackTrace();
                    this.minecraft.execute(() -> {
                        try {
                            this.errorText = Component.literal("Login error: " + e.getMessage());
                            this.loggingIn = false;
                            this.loginButton.active = true;
                        } catch (Exception ex) {
                            System.err.println("Error updating UI after login error: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    });
                }
            }).start();
        } catch (Exception e) {
            System.err.println("Error initiating login attempt: " + e.getMessage());
            e.printStackTrace();
            this.loggingIn = false;
            this.loginButton.active = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        try {
            this.renderBackground(guiGraphics);
            PoseStack poseStack = guiGraphics.pose();

            // 绘制标题
            guiGraphics.drawCenteredString(this.font, "IRC Login", this.width / 2, this.height / 2 - 60, 0xFFFFFF);

            // 绘制错误信息
            if (!this.errorText.getString().isEmpty()) {
                guiGraphics.drawCenteredString(this.font, this.errorText, this.width / 2, this.height / 2 + 90, 0xFF5555);
            }

            // 绘制加载状态
            if (this.loggingIn) {
                guiGraphics.drawCenteredString(this.font, "Logging in...", this.width / 2, this.height / 2 + 90, 0x55FF55);
            }

            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        } catch (Exception e) {
            System.err.println("Error rendering IRC login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        try {
            if (keyCode == 257 || keyCode == 335) { // Enter key
                if (this.loginButton.active) {
                    attemptLogin();
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        } catch (Exception e) {
            System.err.println("Error handling key press in IRC login screen: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onClose() {
        try {
            super.onClose();
        } catch (Exception e) {
            System.err.println("Error closing IRC login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}