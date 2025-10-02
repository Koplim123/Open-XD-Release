package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCCredentialManager;
import com.heypixel.heypixelmod.obsoverlay.utils.HWIDUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SetTitle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Properties;
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
            this.usernameField = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.literal(""));
            this.usernameField.setMaxLength(32);
            this.addRenderableWidget(this.usernameField);

            this.passwordField = new EditBox(this.font, centerX - 100, centerY, 200, 20, Component.literal(""));
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
            try {
                this.hwid = HWIDUtils.getHWID();
            } catch (Exception e) {
                this.hwidErrorText = Component.literal("HWID Error: " + e.getMessage());
                System.err.println("Error getting HWID: " + e.getMessage());
                e.printStackTrace();
            }
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
                                new Thread(() -> IRCCredentialManager.saveCredentials(username, password)).start();
                                SetTitle.apply();
                                this.minecraft.setScreen(new Welcome());
                            } else {
                                if ("HWID_ERROR".equals(IRCLoginManager.lastError)) {
                                    this.errorText = Component.literal("Login failed. Please check your HWID.");
                                } else if ("USERNAME_PASSWORD_ERROR".equals(IRCLoginManager.lastError)) {
                                    this.errorText = Component.literal("Login failed. Please check your username and password.");
                                } else {
                                    this.errorText = Component.literal("Login failed. Fatal Error.");
                                }
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

            guiGraphics.drawCenteredString(this.font, "IRC Login", this.width / 2, this.height / 2 - 60, 0xFFFFFF);

            if (!this.errorText.getString().isEmpty()) {
                guiGraphics.drawCenteredString(this.font, this.errorText, this.width / 2, this.height / 2 + 90, 0xFF5555);
            }

            if (this.loggingIn) {
                guiGraphics.drawCenteredString(this.font, "Logging in...", this.width / 2, this.height / 2 + 90, 0x55FF55);
            }
            if (!this.hwidErrorText.getString().isEmpty()) {
                guiGraphics.drawCenteredString(this.font, this.hwidErrorText, this.width / 2, this.height / 2 + 80, 0xFF5555);
            }
            
            if (this.hwid != null && !this.hwid.isEmpty()) {
                String hwidText = "HWID: " + this.hwid;
                if (hwidText.length() > 40) {
                    hwidText = hwidText.substring(0, 37) + "...";
                }
                this.hwidDisplayText = hwidText;
                
                int hwidWidth = this.font.width(this.hwidDisplayText);
                this.hwidX = this.width / 2 - hwidWidth / 2;
                this.hwidY = this.height / 2 + (this.hwidErrorText.getString().isEmpty() ? 100 : 120);
                guiGraphics.drawCenteredString(this.font, this.hwidDisplayText, this.width / 2, this.hwidErrorText.getString().isEmpty() ? this.height / 2 + 100 : this.height / 2 + 120, 0xAAAAAA);
            }

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

            if (this.usernameField.getValue().isEmpty() && !this.usernameField.isFocused()) {
                guiGraphics.drawString(this.font, Component.literal("Username"), this.usernameField.getX() + 4, this.usernameField.getY() + 6, HINT_COLOR, false);
            }

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

            if (keyCode == 256) { 
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

    private void loadSavedCredentialsAsync() {
        new Thread(() -> {
            try {
                Properties credentials = IRCCredentialManager.loadCredentials();
                if (credentials != null) {
                    String username = credentials.getProperty("username", "");
                    String password = credentials.getProperty("password", "");

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