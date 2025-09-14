package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obfuscation.JNICObf;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCCredentialManager;
import com.heypixel.heypixelmod.obsoverlay.utils.HWIDUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Properties;
@JNICObf
public class IRCLoginScreen extends Screen {
    private EditBox usernameField;
    private EditBox passwordField;
    private Button loginButton;
    private Button registerButton;
    private String hwidDisplayText;
    private int hwidX;
    private int hwidY;
    private Component copySuccessText = Component.empty();
    private long copySuccessTime;
    private Component errorText = Component.empty();
    private Component hwidErrorText = Component.empty();
    private boolean loggingIn = false;
    private String hwid;

    public IRCLoginScreen() {
        super(Component.literal("IRC Login"));
    }

    @Override
    protected void init() {
        try {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            // 用户名输入框
            this.usernameField = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.literal(""));
            this.usernameField.setMaxLength(32);
            this.addRenderableWidget(this.usernameField);

            // 密码输入框
            this.passwordField = new EditBox(this.font, centerX - 100, centerY, 200, 20, Component.literal(""));
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

            // 获取HWID
            try {
                this.hwid = HWIDUtils.getHWID();
            } catch (Exception e) {
                this.hwidErrorText = Component.literal("HWID Error: " + e.getMessage());
                System.err.println("Error getting HWID: " + e.getMessage());
                e.printStackTrace();
            }

            // 异步加载已保存的凭据
            loadSavedCredentialsAsync();
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
            
            // 检查复制成功提示是否需要清除 (3秒后清除)
            if (System.currentTimeMillis() - this.copySuccessTime > 3000) {
                this.copySuccessText = Component.empty();
            }
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
                                // 登录成功，保存凭据
                                new Thread(() -> IRCCredentialManager.saveCredentials(username, password)).start();
                                // 登录成功，跳转到Welcome界面
                                this.minecraft.execute(() -> {
                                    this.minecraft.setScreen(new Welcome());
                                });
                            } else {
                                // 登录失败，显示错误信息
                                this.errorText = Component.literal("Login failed. Please check your username and or HWID.");
                                this.loggingIn = false;
                                this.loginButton.active = true;
                                //this.onClose();
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

            // 绘制HWID错误信息
            if (!this.hwidErrorText.getString().isEmpty()) {
                guiGraphics.drawCenteredString(this.font, this.hwidErrorText, this.width / 2, this.height / 2 + 80, 0xFF5555);
            }
            
            // 绘制HWID
            if (this.hwid != null && !this.hwid.isEmpty()) {
                String hwidText = "HWID: " + this.hwid;
                // 如果HWID太长，截取部分显示
                if (hwidText.length() > 40) {
                    hwidText = hwidText.substring(0, 37) + "...";
                }
                this.hwidDisplayText = hwidText;
                
                // 记录HWID文本的位置和宽度，用于点击检测
                int hwidWidth = this.font.width(this.hwidDisplayText);
                this.hwidX = this.width / 2 - hwidWidth / 2;
                this.hwidY = this.height / 2 + (this.hwidErrorText.getString().isEmpty() ? 100 : 120);
                
                // 绘制HWID文本
                guiGraphics.drawCenteredString(this.font, this.hwidDisplayText, this.width / 2, this.hwidErrorText.getString().isEmpty() ? this.height / 2 + 100 : this.height / 2 + 120, 0xAAAAAA);
            }

            // 绘制复制成功提示
            if (!this.copySuccessText.getString().isEmpty()) {
                guiGraphics.drawCenteredString(this.font, this.copySuccessText, this.width / 2, this.height / 2 + 115, 0x55FF55);
            }

            String originalPassword = this.passwordField.getValue();
            if (!originalPassword.isEmpty()) {
                StringBuilder maskedPassword = new StringBuilder();
                for (int i = 0; i < originalPassword.length(); i++) {
                    maskedPassword.append("*");
                }
                this.passwordField.setValue(maskedPassword.toString());
            }

            super.render(guiGraphics, mouseX, mouseY, partialTicks);

            this.passwordField.setValue(originalPassword);

            final int HINT_COLOR = 0xFFFFFFFF;

            // 用户名输入框
            if (this.usernameField.getValue().isEmpty() && !this.usernameField.isFocused()) {
                guiGraphics.drawString(this.font, Component.literal("Username"), this.usernameField.getX() + 4, this.usernameField.getY() + 6, HINT_COLOR, false);
            }

            // 密码输入框
            if (this.passwordField.getValue().isEmpty() && !this.passwordField.isFocused()) {
                guiGraphics.drawString(this.font, Component.literal("Password"), this.passwordField.getX() + 4, this.passwordField.getY() + 6, HINT_COLOR, false);
            }

        } catch (Exception e) {
            System.err.println("Error rendering IRC login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            boolean usernameClicked = this.usernameField.mouseClicked(mouseX, mouseY, button);
            boolean passwordClicked = this.passwordField.mouseClicked(mouseX, mouseY, button);

            if (usernameClicked) {
                this.passwordField.setFocused(false);
            } else if (passwordClicked) {
                this.usernameField.setFocused(false);
            }

            // 检查是否点击了HWID文本
            if (isMouseOverHWID(mouseX, mouseY) && button == 0) {
                copyHWIDToClipboard();
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        } catch (Exception e) {
            System.err.println("Error handling mouse click in IRC login screen: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        try {
            if (keyCode == 258) { // Tab key
                if (this.usernameField.isFocused()) {
                    this.usernameField.setFocused(false);
                    this.passwordField.setFocused(true);
                } else if (this.passwordField.isFocused()) {
                    this.passwordField.setFocused(false);
                    this.usernameField.setFocused(true);
                }
                return true;
            }

            if (keyCode == 257 || keyCode == 335) { // Enter key
                if (this.loginButton.active) {
                    attemptLogin();
                    return true;
                }
            }

            // 阻止ESC键直接关闭屏幕
            if (keyCode == 256) { // ESC key
                return true;
            }

            if (this.usernameField.isFocused()) {
                this.usernameField.keyPressed(keyCode, scanCode, modifiers);
            } else if (this.passwordField.isFocused()) {
                this.passwordField.keyPressed(keyCode, scanCode, modifiers);
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

    /**
     * 异步加载已保存的凭据到输入框，避免阻塞UI线程
     */
    private void loadSavedCredentialsAsync() {
        new Thread(() -> {
            try {
                Properties credentials = IRCCredentialManager.loadCredentials();
                if (credentials != null) {
                    String username = credentials.getProperty("username", "");
                    String password = credentials.getProperty("password", "");

                    // 在主线程中更新UI
                    this.minecraft.execute(() -> {
                        try {
                            if (!username.isEmpty()) {
                                this.usernameField.setValue(username);
                            }

                            if (!password.isEmpty()) {
                                this.passwordField.setValue(password);
                            }
                        } catch (Exception e) {
                            System.err.println("Error setting credentials in UI: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading saved credentials: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 将HWID复制到剪贴板
     */
    private void copyHWIDToClipboard() {
        try {
            if (this.hwid != null && !this.hwid.isEmpty()) {
                this.minecraft.keyboardHandler.setClipboard(this.hwid);
                this.copySuccessText = Component.literal("HWID Copied!");
                this.copySuccessTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            System.err.println("Error copying HWID to clipboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查鼠标是否在HWID文本上
     */
    private boolean isMouseOverHWID(double mouseX, double mouseY) {
        if (this.hwidDisplayText == null) {
            return false;
        }
        
        int hwidWidth = this.font.width(this.hwidDisplayText);
        int hwidHeight = this.font.lineHeight;
        
        return mouseX >= this.hwidX && mouseX <= this.hwidX + hwidWidth && 
               mouseY >= this.hwidY && mouseY <= this.hwidY + hwidHeight;
    }
}