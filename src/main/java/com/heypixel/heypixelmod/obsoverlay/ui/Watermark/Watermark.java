package com.heypixel.heypixelmod.obsoverlay.ui.Watermark;

import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.StringUtils;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Watermark {
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
    public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
    public static final int backgroundColor = new Color(25, 25, 25, 130).getRGB();
    private static float width;
    private static float watermarkHeight;

    public static void onShader(EventShader e, String style, float cornerRadius, float watermarkSize, float vPadding, boolean renderBlackBackground, boolean blackFont) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // 在 BLUR 通道写入胶囊蒙版，供后处理模糊使用
        if ("Capsule".equals(style) && e.getType() == EventType.BLUR) {
            CustomTextRenderer font = Fonts.opensans;
            Minecraft mc = Minecraft.getInstance();
            String clientName = "Naven-XD";
            String otherInfo = Version.getVersion() + " | " + IRCLoginManager.getUsername() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());

            float clientNameWidth = font.getWidth(clientName, (double)watermarkSize);
            float otherInfoWidth = font.getWidth(otherInfo, (double)watermarkSize);
            float height = (float)font.getHeight(true, (double)watermarkSize);

            float x = 5.0f, y = 5.0f;
            float hPadding = 7.0f;
            float spacing = 5.0f;
            float capsule_height = height + vPadding * 2;

            float capsule1_width = clientNameWidth + hPadding * 2;
            float capsule2_x = x + capsule1_width + spacing;
            float capsule2_width = otherInfoWidth + hPadding * 2;

            RenderUtils.drawRoundedRect(e.getStack(), x, y, capsule1_width, capsule_height, cornerRadius, Integer.MIN_VALUE);
            RenderUtils.drawRoundedRect(e.getStack(), capsule2_x, y, capsule2_width, capsule_height, cornerRadius, Integer.MIN_VALUE);
        }
    }

    public static void onRender(EventRender2D e, float watermarkSize, String style, boolean rainbow, float rainbowSpeed, float rainbowOffset, float cornerRadius, float vPadding, boolean renderBlackBackground, boolean blackFont) {
        if ("Classic".equals(style)) {
            renderClassic(e, watermarkSize, cornerRadius, vPadding);
        } else if ("Capsule".equals(style)) {
            renderCapsule(e, watermarkSize, cornerRadius, vPadding, renderBlackBackground, blackFont);
        } else if ("exhibition".equals(style)) {
            renderExhibition(e, watermarkSize, rainbow, rainbowSpeed, rainbowOffset);
        } else if ("skeet".equals(style)) {
            renderSkeet(e, watermarkSize, rainbow, rainbowSpeed, rainbowOffset, vPadding);
        } else {
            renderRainbow(e, watermarkSize, rainbow, rainbowSpeed, rainbowOffset, cornerRadius, vPadding);
        }
    }

    /**
     * 绘制静态的彩虹条
     */
    private static void drawRainbowBar(PoseStack stack, float x, float y, float width, float height) {
        for (float i = 0; i < width; i++) {
            float hue = i / width;
            int color = Color.HSBtoRGB(hue, 0.8f, 1.0f);
            RenderUtils.fill(stack, x + i, y, x + i + 1, y + height, color);
        }
    }

    /**
     * 绘制动态的、与ArrayList同步的彩虹条
     */
    private static void drawAnimatedRainbowBar(PoseStack stack, float x, float y, float width, float height, float rainbowSpeed, float rainbowOffset) {
        for (float i = 0; i < width; i++) {
            int color = RenderUtils.getRainbowOpaque(
                    (int)(i * -rainbowOffset), 1.0F, 1.0F, (21.0F - rainbowSpeed) * 1000.0F
            );
            RenderUtils.fill(stack, x + i, y, x + i + 1, y + height, color);
        }
    }

    /**
     * 渲染 "Rainbow" 样式的Watermark
     */
    private static void renderRainbow(EventRender2D e, float watermarkSize, boolean rainbow, float rainbowSpeed, float rainbowOffset, float cornerRadius, float vPadding) {
        CustomTextRenderer font = Fonts.opensans;
        Minecraft mc = Minecraft.getInstance();
        e.getStack().pushPose();

        String clientName = "Naven-XD";
        String separator = " | ";
        String otherInfo = Version.getVersion() + " | " + IRCLoginManager.getUsername() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());
        String fullText = clientName + separator + otherInfo;

        width = font.getWidth(fullText, (double)watermarkSize) + 14.0F;
        watermarkHeight = (float)font.getHeight(true, (double)watermarkSize);
        float x = 5.0f, y = 5.0f;
        float textX = x + 7.0f;
        float textY = y + vPadding;
        float totalHeight = watermarkHeight + vPadding * 2;

        // 步骤 1: 绘制带圆角的背景 (使用模板缓冲修复毛刺问题)
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, width, totalHeight, cornerRadius, Integer.MIN_VALUE);
        StencilUtils.erase(true);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, width, totalHeight, cornerRadius, backgroundColor);

        // 步骤 2: 绘制顶部的彩虹条
        if (rainbow) {
            drawAnimatedRainbowBar(e.getStack(), x, y, width, 2.0F, rainbowSpeed, rainbowOffset);
        } else {
            drawRainbowBar(e.getStack(), x, y, width, 2.0F);
        }

        // 步骤 3: 渲染文本
        if (rainbow) {
            float clientNameWidth = font.getWidth(clientName, (double)watermarkSize);
            float currentX = textX;
            for (char c : clientName.toCharArray()) {
                String character = String.valueOf(c);
                int color = RenderUtils.getRainbowOpaque(
                        (int)(currentX * -rainbowOffset / 5), 1.0F, 1.0F, (21.0F - rainbowSpeed) * 1000.0F
                );
                font.render(e.getStack(), character, currentX, textY, new Color(color), true, (double)watermarkSize);
                currentX += font.getWidth(character, (double)watermarkSize);
            }
            font.render(e.getStack(), separator + otherInfo, textX + clientNameWidth, textY, Color.WHITE, true, (double)watermarkSize);
        } else {
            float clientNameWidth = font.getWidth(clientName, (double)watermarkSize);
            int clientNameColor = new Color(110, 255, 110).getRGB();
            font.render(e.getStack(), clientName, textX, textY, new Color(clientNameColor), true, (double)watermarkSize);
            font.render(e.getStack(), separator + otherInfo, textX + clientNameWidth, textY, Color.WHITE, true, (double)watermarkSize);
        }

        StencilUtils.dispose();
        e.getStack().popPose();
    }

    /**
     * 渲染 "exhibition" 样式的Watermark
     */
    private static void renderExhibition(EventRender2D e, float watermarkSize, boolean rainbow, float rainbowSpeed, float rainbowOffset) {
        CustomTextRenderer font = Fonts.opensans;
        Minecraft mc = Minecraft.getInstance();
        e.getStack().pushPose();

        String clientName = "Naven-XD";
        String separator = " [";
        String otherInfo = Version.getVersion() + "] [" + IRCLoginManager.getUsername() + "] [" + StringUtils.split(mc.fpsString, " ")[0] + " FPS] [" + format.format(new Date()) + "]";

        float x = 5.0f;
        float y = 5.0f;

        float currentX = x;

        String firstChar = String.valueOf(clientName.charAt(0));
        String restOfClientName = clientName.substring(1);

        // 渲染 'N'
        if (rainbow) {
            // 动态彩虹 'N'
            int color = RenderUtils.getRainbowOpaque(
                    (int)(currentX * -rainbowOffset / 5), 1.0F, 1.0F, (21.0F - rainbowSpeed) * 1000.0F
            );
            font.render(e.getStack(), firstChar, currentX, y, new Color(color), true, (double)watermarkSize);
        } else {
            // 静态彩虹 'N' (使用色相环的第一个颜色，红色)
            font.render(e.getStack(), firstChar, currentX, y, new Color(Color.HSBtoRGB(0f, 0.8f, 1f)), true, (double)watermarkSize);
        }

        currentX += font.getWidth(firstChar, (double)watermarkSize);

        // 渲染其余白色文本
        String restOfText = restOfClientName + separator + otherInfo;
        font.render(e.getStack(), restOfText, currentX, y, Color.WHITE, true, (double)watermarkSize);

        e.getStack().popPose();
    }

    /**
     * 渲染 "skeet" 样式的Watermark
     */
    private static void renderSkeet(EventRender2D e, float watermarkSize, boolean rainbow, float rainbowSpeed, float rainbowOffset, float vPadding) {
        CustomTextRenderer font = Fonts.opensans;
        Minecraft mc = Minecraft.getInstance();
        e.getStack().pushPose();

        String text = "Naven-XD | " + Version.getVersion() + " | " + IRCLoginManager.getUsername() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());

        float textWidth = font.getWidth(text, (double)watermarkSize);
        width = textWidth + 14.0F; // 7px padding on each side
        watermarkHeight = (float)font.getHeight(true, (double)watermarkSize);
        float borderWidth = 2.0f;
        float rainbowHeight = 1.0f;
        float topSectionHeight = borderWidth + rainbowHeight; // 顶部边框 + 彩虹条
        float totalHeight = topSectionHeight + watermarkHeight + vPadding * 2 + borderWidth; // 顶部区域 + 内容 + 底部边框

        float x = 5.0f;
        float y = 5.0f;

        int skeetBorderColor = new Color(45, 45, 45).getRGB();
        int skeetBackgroundColor = new Color(35, 35, 35).getRGB();

        // 绘制背景
        RenderUtils.fill(e.getStack(), x + borderWidth, y + topSectionHeight, x + width - borderWidth, y + totalHeight - borderWidth, skeetBackgroundColor);

        // 绘制边框
        // 顶部边框
        RenderUtils.fill(e.getStack(), x, y, x + width, y + borderWidth, skeetBorderColor);
        // 彩虹条
        if (rainbow) {
            drawAnimatedRainbowBar(e.getStack(), x, y + borderWidth, width, rainbowHeight, rainbowSpeed, rainbowOffset);
        } else {
            drawRainbowBar(e.getStack(), x, y + borderWidth, width, rainbowHeight);
        }
        // 左边框
        RenderUtils.fill(e.getStack(), x, y + borderWidth, x + borderWidth, y + totalHeight, skeetBorderColor);
        // 右边框
        RenderUtils.fill(e.getStack(), x + width - borderWidth, y + borderWidth, x + width, y + totalHeight, skeetBorderColor);
        // 底边框
        RenderUtils.fill(e.getStack(), x, y + totalHeight - borderWidth, x + width, y + totalHeight, skeetBorderColor);


        // 渲染文本
        float textX = x + 7.0f;
        float textY = y + topSectionHeight + vPadding;
        font.render(e.getStack(), text, textX, textY, Color.WHITE, true, (double)watermarkSize);

        e.getStack().popPose();
    }

    /**
     * 渲染 "Classic" 样式的Watermark
     */
    private static void renderClassic(EventRender2D e, float watermarkSize, float cornerRadius, float vPadding) {
        CustomTextRenderer font = Fonts.opensans;
        Minecraft mc = Minecraft.getInstance();
        e.getStack().pushPose();

        String text = "Naven-XD | " + Version.getVersion() + " | " + IRCLoginManager.getUsername() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());

        width = font.getWidth(text, (double)watermarkSize) + 14.0F;
        watermarkHeight = (float)font.getHeight(true, (double)watermarkSize);
        float totalHeight = 3.0f + watermarkHeight + vPadding * 2; // 3px 顶部栏 + 文本高度 + 上下边距

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), 5.0F, 5.0F, width, totalHeight, cornerRadius, Integer.MIN_VALUE);
        StencilUtils.erase(true);
        RenderUtils.fill(e.getStack(), 5.0F, 5.0F, 5.0F + width, 8.0F, headerColor);
        RenderUtils.fill(e.getStack(), 5.0F, 8.0F, 5.0F + width, 5.0F + totalHeight, bodyColor);
        font.render(e.getStack(), text, 12.0, 8.0F + vPadding, Color.WHITE, true, (double)watermarkSize);
        StencilUtils.dispose();
        e.getStack().popPose();
    }

    /**
     * 渲染 "Capsule" 样式的Watermark
     */
    private static void renderCapsule(EventRender2D e, float watermarkSize, float cornerRadius, float vPadding, boolean renderBlackBackground, boolean blackFont) {
        CustomTextRenderer font = Fonts.opensans;
        Minecraft mc = Minecraft.getInstance();
        e.getStack().pushPose();

        String clientName = "Naven-XD";
        String otherInfo = Version.getVersion() + " | " + IRCLoginManager.getUsername() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());

        float clientNameWidth = font.getWidth(clientName, (double)watermarkSize);
        float otherInfoWidth = font.getWidth(otherInfo, (double)watermarkSize);
        float height = (float)font.getHeight(true, (double)watermarkSize);

        float x = 5.0f, y = 5.0f;
        float hPadding = 7.0f;
        float spacing = 5.0f;
        float capsule_height = height + vPadding * 2;

        float capsule1_width = clientNameWidth + hPadding * 2;
        float capsule2_x = x + capsule1_width + spacing;
        float capsule2_width = otherInfoWidth + hPadding * 2;

        // 使用模板缓冲来获得更平滑的圆角
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, capsule1_width, capsule_height, cornerRadius, Integer.MIN_VALUE);
        RenderUtils.drawRoundedRect(e.getStack(), capsule2_x, y, capsule2_width, capsule_height, cornerRadius, Integer.MIN_VALUE);
        StencilUtils.erase(true);

        // 根据 renderBlackBackground 参数决定是否渲染黑色背景
        if (renderBlackBackground) {
            // 使用半透明背景色而不是纯白色，以便呈现 blur 背景
            RenderUtils.drawRoundedRect(e.getStack(), x, y, capsule1_width, capsule_height, cornerRadius, backgroundColor);
            RenderUtils.drawRoundedRect(e.getStack(), capsule2_x, y, capsule2_width, capsule_height, cornerRadius, backgroundColor);
        }

        // 渲染文本 - 根据 blackFont 参数选择颜色
        Color textColor = blackFont ? Color.BLACK : Color.WHITE;
        font.render(e.getStack(), clientName, x + hPadding, y + vPadding, textColor, true, (double)watermarkSize);
        font.render(e.getStack(), otherInfo, capsule2_x + hPadding, y + vPadding, textColor, true, (double)watermarkSize);

        StencilUtils.dispose();
        e.getStack().popPose();
    }
}