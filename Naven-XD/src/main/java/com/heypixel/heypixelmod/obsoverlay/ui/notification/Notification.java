package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Notification {
    public static byte[] authTokens;
    private NotificationLevel level;
    private String title, description;
    private long maxAge;
    private long createTime = System.currentTimeMillis();
    private SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0.0F);
    private SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0.0F);
    private TimeHelper timerUtil = new TimeHelper();

    // 图标资源路径
    private static final ResourceLocation ICON_TRUE = new ResourceLocation("heypixel", "textures/gui/true.png");
    private static final ResourceLocation ICON_CANCEL = new ResourceLocation("heypixel", "textures/gui/cancel.png");

    // 毛玻璃背景纹理
    private DynamicTexture glassBackground;
    private NativeImage glassImage;

    private SmoothAnimationTimer animationTimer = new SmoothAnimationTimer(0.0F);
    private boolean isEntering = true;
    private static final float ANIMATION_DURATION = 50.0F; // 动画持续时间（毫秒）

    public Notification(NotificationLevel level, String message, long age) {
        this.level = level;
        if (message.contains(":")) {
            String[] parts = message.split(":", 2);
            this.title = parts[0].trim();
            this.description = parts[1].trim();
        } else {
            this.title = "Notification";
            this.description = message;
        }
        this.maxAge = age;
        initializeGlassBackground();
        resetAnimation();
    }

    public Notification(NotificationLevel level, String title, String description, long age) {
        this.level = level;
        this.title = title;
        this.description = description;
        this.maxAge = age;
        initializeGlassBackground();
        resetAnimation();
    }

    private void resetAnimation() {
        animationTimer.reset();
        isEntering = true;
    }

    public void startEnterAnimation() {
        resetAnimation();
        isEntering = true;
    }

    public void startExitAnimation() {
        resetAnimation();
        isEntering = false;
    }

    public boolean isAnimating() {
        return animationTimer.getProgress() < 1.0F;
    }

    public float getAnimationProgress() {
        return animationTimer.getProgress();
    }

    public void renderShader(PoseStack stack, float x, float y) {
        int screenWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        float notificationX = screenWidth - getWidth() - 10;
        float notificationY = screenHeight - getHeight() - 10;

        // 计算动画位置
        float animX = isEntering ? 
            screenWidth + getWidth() + 10 : 
            screenWidth - getWidth() - 10;
        float animY = notificationY;

        // 更新动画进度
        animationTimer.update(System.currentTimeMillis());

        // 应用非线性动画（使用 easeInOutQuad）
        float progress = getAnimationProgress();
        float easedProgress = easeInOutQuad(progress);

        // 计算当前动画位置
        float currentX = lerp(animX, notificationX, easedProgress);
        float currentY = lerp(animY, notificationY, easedProgress);

        // 渲染毛玻璃背景
        RenderUtils.drawRoundedRect(stack, currentX + 2.0F, currentY + 4.0F, getWidth(), getHeight(), 5.0F, level.getColor());

        // 应用高斯模糊效果
        applyGaussianBlur(stack, currentX, currentY);

        // 渲染标题和描述
        renderText(stack, currentX + 20.0F, currentY + 10.0F, title, 0.35F);
        renderText(stack, currentX + 20.0F, currentY + 30.0F, description, 0.3F);

        // 渲染图标
        renderIcon(stack, currentX + getWidth() - 90.0F, currentY + 10.0F, isPositive());
    }

    private float easeInOutQuad(float t) {
        return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void applyGaussianBlur(PoseStack stack, float x, float y) {
        StencilUtils.pushMask(stack, x, y, getWidth(), getHeight());
        RenderUtils.drawRoundedRect(stack, x + 2.0F, y + 4.0F, getWidth(), getHeight(), 5.0F, new Color(0, 0, 0, 128));
        StencilUtils.popMask(stack);
    }

    private void renderText(PoseStack stack, float x, float y, String text, float scale) {
        Fonts.harmony.render(stack, text, x, y, 0xFFFFFFFF, scale);
    }

    private void renderIcon(PoseStack stack, float x, float y, boolean positive) {
        ResourceLocation icon = positive ? ICON_TRUE : ICON_CANCEL;
        Minecraft.getInstance().getTextureManager().bindForSetup(icon);
        RenderUtils.drawTexturedModalRect(stack, x, y, 80, 80, 80, 80);
    }

    private boolean isPositive() {
        return level == NotificationLevel.SUCCESS;
    }

    private void initializeGlassBackground() {
        try {
            int width = 400;
            int height = 80;
            glassImage = new NativeImage(width, height, true);
            glassImage.clear(0xFF000000);
            glassBackground = new DynamicTexture(glassImage);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize glass background", e);
        }
    }

    public float getWidth() {
        float titleWidth = Fonts.harmony.getWidth(title, 0.35F);
        float descWidth = Fonts.harmony.getWidth(description, 0.3F);
        float iconWidth = 80.0F;

        return Math.max(titleWidth, descWidth) + iconWidth + 20.0F;
    }

    public float getHeight() {
        return 60.0F;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public long getCreateTime() {
        return createTime;
    }

    public SmoothAnimationTimer getWidthTimer() {
        return widthTimer;
    }

    public SmoothAnimationTimer getHeightTimer() {
        return heightTimer;
    }

    public TimeHelper getTimerUtil() {
        return timerUtil;
    }

    public void setLevel(NotificationLevel level) {
        this.level = level;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setWidthTimer(SmoothAnimationTimer widthTimer) {
        this.widthTimer = widthTimer;
    }

    public void setHeightTimer(SmoothAnimationTimer heightTimer) {
        this.heightTimer = heightTimer;
    }

    public void setTimerUtil(TimeHelper timerUtil) {
        this.timerUtil = timerUtil;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Notification other)) {
            return false;
        } else if (!other.canEqual(this)) {
            return false;
        } else if (this.getMaxAge() != other.getMaxAge()) {
            return false;
        } else if (this.getCreateTime() != other.getCreateTime()) {
            return false;
        } else {
            Object this$level = this.getLevel();
            Object other$level = other.getLevel();
            if (this$level == null ? other$level == null : this$level.equals(other$level)) {
                Object this$title = this.getTitle();
                Object other$title = other.getTitle();
                if (this$title == null ? other$title == null : this$title.equals(other$title)) {
                    Object this$description = this.getDescription();
                    Object other$description = other.getDescription();
                    if (this$description == null ? other$description == null : this$description.equals(other$description)) {
                        Object this$widthTimer = this.getWidthTimer();
                        Object other$widthTimer = other.getWidthTimer();
                        if (this$widthTimer == null ? other$widthTimer == null : this$widthTimer.equals(other$widthTimer)) {
                            Object this$heightTimer = this.getHeightTimer();
                            Object other$heightTimer = other.getHeightTimer();
                            return this$heightTimer == null ? other$heightTimer == null : this$heightTimer.equals(other$heightTimer);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Notification;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $maxAge = this.getMaxAge();
        result = result * 59 + (int)($maxAge >>> 32 ^ $maxAge);
        long $createTime = this.getCreateTime();
        result = result * 59 + (int)($createTime >>> 32 ^ $createTime);
        Object $level = this.getLevel();
        result = result * 59 + ($level == null ? 43 : $level.hashCode());
        Object $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        Object $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        Object $widthTimer = this.getWidthTimer();
        result = result * 59 + ($widthTimer == null ? 43 : $widthTimer.hashCode());
        Object $heightTimer = this.getHeightTimer();
        return result * 59 + ($heightTimer == null ? 43 : $heightTimer.hashCode());
    }

    @Override
    public String toString() {
        return "Notification(level="
                + this.getLevel()
                + ", title="
                + this.getTitle()
                + ", description="
                + this.getDescription()
                + ", maxAge="
                + this.getMaxAge()
                + ", createTime="
                + this.getCreateTime()
                + ", widthTimer="
                + this.getWidthTimer()
                + ", heightTimer="
                + this.getHeightTimer()
                + ")";
    }
}