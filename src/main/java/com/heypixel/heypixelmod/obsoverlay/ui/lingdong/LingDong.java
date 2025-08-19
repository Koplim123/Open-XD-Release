package com.heypixel.heypixelmod.obsoverlay.ui.lingdong;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ChestStealer;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NameProtect;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class LingDong {
    private static final float MIN_WIDTH = 250.0F; // 最小宽度
    private static final float NORMAL_HEIGHT = 60.0F; // 正常高度
    private static final float MODULE_HEIGHT = 200.0F; // 模块模式高度
    private static final float PADDING = 6.0F; // 内边距
    private static final float MAX_RADIUS = 10.0F; // 最大圆角半径
    private static final float ITEM_SIZE = 20.0F; // 物品格子大小
    private static final float ITEM_PADDING = 4.0F; // 物品格子之间的间距

    // 动画状态变量
    private static boolean isAnimating = false;
    private static long animationStartTime = 0;
    private static final long ANIMATION_DURATION = 100; // 2个游戏刻 = 100毫秒

    private static float currentPillWidth = 0.0F;
    private static float currentPillHeight = 0.0F;

    private static float startWidth;
    private static float endWidth;
    private static float startHeight;
    private static float endHeight;

    private static boolean wasScaffoldActive = false;
    private static boolean wasBacktrackActive = false;
    private static boolean wasTabActive = false;
    private static boolean wasChestStealerActive = false;

    // 为Scaffold进度条添加缓动效果的实例
    private static ProgressBarAnimator scaffoldProgressAnimator = null;

    // 为ChestStealer进度条添加缓动效果的实例
    private static ProgressBarAnimator chestStealerProgressAnimator = null;

    public static void render(
            GuiGraphics guiGraphics,
            CustomTextRenderer font,
            String version,
            String fps,
            String playerName,
            float watermarkSize,
            ModuleManager moduleManager
    ) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 获取模块状态
        Scaffold scaffoldModule = null;
        ChestStealer chestStealerModule = null;
        HUD hudModule = null;

        boolean scaffoldActive = false;
        boolean backtrackActive = false;
        boolean chestStealerActive = false;
        boolean betterTabEnabled = false;

        if (moduleManager != null) {
            scaffoldModule = (Scaffold) moduleManager.getModule(Scaffold.class);
            scaffoldActive = scaffoldActive = scaffoldModule != null && scaffoldModule.isEnabled() && scaffoldModule.scaffoldblocksstyles.isCurrentMode("LingDong") && scaffoldModule.scaffoldblocks.getCurrentValue();;
            chestStealerModule = (ChestStealer) moduleManager.getModule(ChestStealer.class);
            if (chestStealerModule != null) {
                chestStealerActive = chestStealerModule.isEnabled() &&
                        chestStealerModule.CSRender.getCurrentValue() &&
                        chestStealerModule.CSRenderMode.isCurrentMode("LingDong") &&
                        chestStealerModule.isCSWorking();
            }
            hudModule = (HUD) moduleManager.getModule(HUD.class);
            betterTabEnabled = hudModule != null && hudModule.isEnabled() && hudModule.bettertab.getCurrentValue();
        }

        boolean isWaterMarkLingDong = true;
        boolean isTabActive = mc.options.keyPlayerList.isDown() && betterTabEnabled;

        // 计算目标尺寸
        float textScale = 1.3f * watermarkSize;
        String normalText = "Naven | " + Version.getVersion() + " | FPS: " + fps + " | " + playerName;
        float normalTextWidth = font.getWidth(normalText, textScale);
        float normalTotalWidth = Math.max(MIN_WIDTH * watermarkSize, normalTextWidth + PADDING * 3);

        float targetWidth;
        float targetHeight;

        if (isTabActive) {
            targetHeight = calculateTabListHeight(font, textScale, watermarkSize);
            targetWidth = calculateTabListWidth(font, textScale, watermarkSize);
        } else if (scaffoldActive) {
            targetHeight = NORMAL_HEIGHT * 1.2f * watermarkSize;
            String scaffoldText = "Scaffold Blocks: " + (scaffoldModule != null ? scaffoldModule.getTotalBlocksInHotbar() : 0);
            float scaffoldTextWidth = font.getWidth(scaffoldText, textScale);
            float progressBarWidth = 150f * watermarkSize;
            float neededWidth = scaffoldTextWidth + progressBarWidth + PADDING * 6 * watermarkSize;
            targetWidth = Math.max(neededWidth, normalTotalWidth * 1.5f);
        } else if (backtrackActive) {
            targetHeight = (float) (font.getHeight(true, textScale) * 3 + PADDING * 3 * watermarkSize + 4.0f);
            targetWidth = normalTotalWidth;
        } else if (chestStealerActive) {
            // 根据公式动态计算总高
            int cols = 9;
            int maxRows = 6;
            float scaledPadding = PADDING * watermarkSize;
            float totalGridWidth = normalTotalWidth;
            float itemScaledSize = (totalGridWidth - scaledPadding * 2.0f - (cols - 1) * ITEM_PADDING * watermarkSize) / cols;
            float itemScaledPadding = ITEM_PADDING * watermarkSize;

            float gridHeight = (maxRows * itemScaledSize) + ((maxRows - 1) * itemScaledPadding);
            float textHeight = (float) (font.getHeight(true, textScale) * 2 + scaledPadding);
            float progressBarHeight = 4.0f;

            // 总高 = 顶部文本总高 + 文本与网格的间隔 + 网格总高 + 网格与进度条的间隔 + 进度条高度 + 底部边距
            targetHeight = textHeight + scaledPadding + gridHeight + scaledPadding + progressBarHeight + scaledPadding;
            targetWidth = normalTotalWidth;
        } else {
            targetHeight = NORMAL_HEIGHT * watermarkSize;
            targetWidth = normalTotalWidth;
        }

        // 检测并启动动画
        if (isTabActive != wasTabActive || scaffoldActive != wasScaffoldActive || backtrackActive != wasBacktrackActive || chestStealerActive != wasChestStealerActive) {
            isAnimating = true;
            animationStartTime = System.currentTimeMillis();
            startWidth = currentPillWidth;
            startHeight = currentPillHeight;
            endWidth = targetWidth;
            endHeight = targetHeight;
            wasScaffoldActive = scaffoldActive;
            wasBacktrackActive = backtrackActive;
            wasTabActive = isTabActive;
            wasChestStealerActive = chestStealerActive;
        }

        // 计算当前动画进度
        if (isAnimating) {
            long elapsedTime = System.currentTimeMillis() - animationStartTime;
            float progress = (float) elapsedTime / ANIMATION_DURATION;
            if (progress >= 1.0f) {
                progress = 1.0f;
                isAnimating = false;
            }

            // 线性插值
            currentPillWidth = startWidth + (endWidth - startWidth) * progress;
            currentPillHeight = startHeight + (endHeight - startHeight) * progress;
        } else {
            // 动画结束或没有动画，直接使用目标尺寸
            currentPillWidth = targetWidth;
            currentPillHeight = targetHeight;
        }

        // 首次渲染初始化尺寸
        if (currentPillWidth == 0.0F) {
            currentPillWidth = targetWidth;
            currentPillHeight = targetHeight;
        }

        // 位置：水平居中，垂直位置在屏幕上方8%处
        float pillX = (screenWidth - currentPillWidth) / 2.0F;
        float pillY = screenHeight * 0.08f;

        // 动态计算安全的圆角半径
        float maxPossibleRadius = currentPillHeight / 2;
        float safeRadius = Math.min(MAX_RADIUS * watermarkSize, maxPossibleRadius);

        // 使用 GuiGraphics 进行渲染
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(guiGraphics.pose(), pillX, pillY, currentPillWidth, currentPillHeight, safeRadius, 0x80000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(guiGraphics.pose(), pillX, pillY, currentPillWidth, currentPillHeight, 0x80000000);
        StencilUtils.dispose();

        // 只有动画结束时才渲染内容
        if (!isAnimating) {
            if (isTabActive) {
                renderPlayerListView(guiGraphics, font, pillX, pillY, currentPillWidth, currentPillHeight, textScale, watermarkSize);
            } else if (!scaffoldActive && !backtrackActive && !chestStealerActive) {
                // 修复：将文本水平居中
                float textX = pillX + (currentPillWidth - normalTextWidth) / 2;
                float textY = (float) (pillY + (currentPillHeight - font.getHeight(true, textScale)) / 2);
                font.render(guiGraphics.pose(), normalText, textX, textY, Color.WHITE, true, textScale);
            } else if (scaffoldActive) {
                renderScaffoldView(guiGraphics, font, pillX, pillY, currentPillWidth, currentPillHeight,
                        textScale, watermarkSize, scaffoldModule);
            } else if (chestStealerActive) {
                renderChestStealerView(guiGraphics, font, pillX, pillY, currentPillWidth, currentPillHeight,
                        textScale, watermarkSize, chestStealerModule);
            }
        }
    }

    private static float calculateTabListHeight(CustomTextRenderer font, float textScale, float watermarkSize) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener cpl = mc.getConnection();
        if (cpl == null) return NORMAL_HEIGHT * watermarkSize;
        int playerCount = cpl.getOnlinePlayers().size();
        // 每列最大玩家数量
        int maxPlayersPerColumn = 10;
        int rows = Math.min(playerCount, maxPlayersPerColumn);
        return (float) (rows * (font.getHeight(true, textScale) + 2) + PADDING * 2 * watermarkSize);
    }

    private static float calculateTabListWidth(CustomTextRenderer font, float textScale, float watermarkSize) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener cpl = mc.getConnection();
        if (cpl == null) return MIN_WIDTH * watermarkSize;
        String localPlayerName = mc.player != null ? mc.player.getGameProfile().getName() : "";
        NameProtect nameProtectModule = (NameProtect) Naven.getInstance().getModuleManager().getModule(NameProtect.class);
        boolean nameProtectEnabled = nameProtectModule != null && nameProtectModule.isEnabled();

        List<PlayerInfo> players = cpl.getOnlinePlayers().stream().collect(Collectors.toList());

        // 每列最大玩家数量
        int maxPlayersPerColumn = 10;
        int columns = (int) Math.ceil((float) players.size() / maxPlayersPerColumn);
        if (columns == 0) columns = 1;

        float scaledPadding = PADDING * watermarkSize;
        float additionalPaddingWidth = font.getWidth("aaaa", textScale);
        float totalWidth = 0.0F;

        // 为每一列计算单独的宽度
        for (int col = 0; col < columns; col++) {
            float maxNameWidthInColumn = 0.0F;
            int startIndex = col * maxPlayersPerColumn;
            int endIndex = Math.min((col + 1) * maxPlayersPerColumn, players.size());

            for (int i = startIndex; i < endIndex; i++) {
                PlayerInfo playerInfo = players.get(i);
                String name = playerInfo.getTabListDisplayName() != null ? playerInfo.getTabListDisplayName().getString() : playerInfo.getProfile().getName();

                // 应用名称保护
                if (nameProtectEnabled && playerInfo.getProfile().getName().equals(localPlayerName)) {
                    name = nameProtectModule.getName();
                }

                float width = getApproximateTextWidth(name, font, textScale);
                if (width > maxNameWidthInColumn) {
                    maxNameWidthInColumn = width;
                }
            }
            float columnWidth = maxNameWidthInColumn + additionalPaddingWidth;
            totalWidth += columnWidth;
        }

        // 计算总宽度时加上列之间的间距
        if (columns > 1) {
            totalWidth += scaledPadding * (columns - 1);
        }

        return Math.max(MIN_WIDTH * watermarkSize, totalWidth + scaledPadding * 2);
    }

    private static float getApproximateTextWidth(String text, CustomTextRenderer font, float scale) {
        float totalWidth = 0.0f;
        // 获取两个小写字母的宽度作为中文字符的基准宽度
        float twoEnglishCharsWidth = font.getWidth("aa", scale);
        float upperCaseWidth = font.getWidth("A", scale);

        for (char c : text.toCharArray()) {
            // 检查字符是否在中文的Unicode范围内
            if (c >= '\u4e00' && c <= '\u9fa5') {
                totalWidth += twoEnglishCharsWidth; // 中文字符按两个英文字符的宽度计算
            } else if (Character.isUpperCase(c)) {
                totalWidth += upperCaseWidth; // 大写字母按其真实宽度计算
            } else {
                totalWidth += font.getWidth(String.valueOf(c), scale); // 其他字符按实际宽度计算
            }
        }
        return totalWidth;
    }

    private static void renderPlayerListView(GuiGraphics guiGraphics, CustomTextRenderer font, float pillX, float pillY, float totalWidth, float pillHeight, float textScale, float watermarkSize) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener cpl = mc.getConnection();
        if (cpl == null) return;
        String localPlayerName = mc.player != null ? mc.player.getGameProfile().getName() : "";
        NameProtect nameProtectModule = (NameProtect) Naven.getInstance().getModuleManager().getModule(NameProtect.class);
        boolean nameProtectEnabled = nameProtectModule != null && nameProtectModule.isEnabled();

        List<PlayerInfo> players = cpl.getOnlinePlayers().stream().collect(Collectors.toList());
        int maxPlayersPerColumn = 10;
        float scaledPadding = PADDING * watermarkSize;
        float fontHeight = (float) font.getHeight(true, textScale);

        float currentX = pillX + scaledPadding;

        // 逐列计算宽度并渲染
        int columns = (int) Math.ceil((float) players.size() / maxPlayersPerColumn);
        if (columns == 0) columns = 1;

        for (int col = 0; col < columns; col++) {
            float maxNameWidthInColumn = 0.0F;
            int startIndex = col * maxPlayersPerColumn;
            int endIndex = Math.min((col + 1) * maxPlayersPerColumn, players.size());

            // 找到本列最长名字的宽度
            for (int i = startIndex; i < endIndex; i++) {
                PlayerInfo playerInfo = players.get(i);
                String name = playerInfo.getTabListDisplayName() != null ? playerInfo.getTabListDisplayName().getString() : playerInfo.getProfile().getName();

                // 应用名称保护
                if (nameProtectEnabled && playerInfo.getProfile().getName().equals(localPlayerName)) {
                    name = nameProtectModule.getName();
                }

                float width = getApproximateTextWidth(name, font, textScale);
                if (width > maxNameWidthInColumn) {
                    maxNameWidthInColumn = width;
                }
            }

            // 计算本列的渲染宽度
            float columnWidth = maxNameWidthInColumn + font.getWidth("aaaa", textScale);

            // 渲染本列的所有玩家
            for (int i = startIndex; i < endIndex; i++) {
                PlayerInfo playerInfo = players.get(i);
                String name = playerInfo.getTabListDisplayName() != null ? playerInfo.getTabListDisplayName().getString() : playerInfo.getProfile().getName();
                int row = i % maxPlayersPerColumn;

                // 应用名称保护
                if (nameProtectEnabled && playerInfo.getProfile().getName().equals(localPlayerName)) {
                    name = nameProtectModule.getName();
                }

                float y = pillY + scaledPadding + row * (fontHeight + 2);
                Fonts.harmony.render(guiGraphics.pose(), name, currentX, y, Color.WHITE, true, textScale);
            }
            // 移动到下一列的起始位置
            currentX += columnWidth + scaledPadding;
        }
    }

    private static void renderChestStealerView(GuiGraphics guiGraphics, CustomTextRenderer font,
                                               float pillX, float pillY, float totalWidth, float pillHeight,
                                               float textScale, float watermarkSize,
                                               ChestStealer chestStealerModule) {
        float scaledPadding = PADDING * watermarkSize;

        // 渲染顶部文本
        String title = "ChestStealer";
        float titleX = pillX + scaledPadding;
        float titleY = pillY + scaledPadding;
        font.render(guiGraphics.pose(), title, titleX, titleY, Color.WHITE, true, textScale);

        int stolenItems = chestStealerModule.stolenItems;
        int totalItems = chestStealerModule.totalItemsToSteal;
        String progressText = "Stealing: " + stolenItems + " / " + totalItems;
        float progressTextY = (float) (titleY + font.getHeight(true, textScale) + scaledPadding / 2.0f);
        font.render(guiGraphics.pose(), progressText, titleX, progressTextY, Color.WHITE, true, textScale);

        // 动态计算物品格子大小
        int cols = 9;
        int maxRows = 6;
        float totalGridWidth = totalWidth - scaledPadding * 2.0f;
        float itemScaledSize = (totalGridWidth - (cols - 1) * ITEM_PADDING * watermarkSize) / cols;
        float itemScaledPadding = ITEM_PADDING * watermarkSize;

        // 渲染物品格子
        List<ItemStack> chestContents = chestStealerModule.getChestInventory();

        float gridStartX = pillX + scaledPadding;
        float gridStartY = (float) (progressTextY + font.getHeight(true, textScale) + scaledPadding);

        // 计算物品居中偏移量
        float itemCenterOffset = (itemScaledSize - 16.0f) / 2.0f;

        for (int i = 0; i < chestContents.size(); i++) {
            ItemStack stack = chestContents.get(i);
            int row = i / cols;
            int col = i % cols;

            // 如果超过6行，则不渲染，避免溢出
            if (row >= maxRows) {
                break;
            }

            float itemX = gridStartX + col * (itemScaledSize + itemScaledPadding);
            float itemY = gridStartY + row * (itemScaledSize + itemScaledPadding);

            // 绘制物品背景格子
            RenderUtils.fillBound(guiGraphics.pose(), itemX, itemY, itemScaledSize, itemScaledSize, 0x40FFFFFF);

            // 如果物品不为空，则绘制物品
            if (!stack.isEmpty()) {
                // 绘制物品时加入偏移量以实现居中
                guiGraphics.renderItem(stack, (int) (itemX + itemCenterOffset), (int) (itemY + itemCenterOffset));
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, (int) (itemX + itemCenterOffset), (int) (itemY + itemCenterOffset));
            }
        }

        // === 关键修复：重置OpenGL状态 ===
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 渲染底部进度条
        float gridHeight = (maxRows * itemScaledSize) + ((maxRows - 1) * itemScaledPadding);

        float progressBarWidth = totalGridWidth;
        float progressBarHeight = 4.0f;
        float progressBarX = pillX + scaledPadding;
        float progressBarY = gridStartY + gridHeight + scaledPadding;
        float progressBarRadius = progressBarHeight / 2.0f;

        RenderUtils.drawRoundedRect(guiGraphics.pose(), progressBarX, progressBarY,
                progressBarWidth, progressBarHeight, progressBarRadius, 0x80000000);

        // 实例化进度条动画
        if (chestStealerProgressAnimator == null) {
            chestStealerProgressAnimator = new ProgressBarAnimator(0.0f, 2.0f);
        }

        // 更新进度条动画，这里使用已偷取物品数作为目标值
        chestStealerProgressAnimator.update((float) stolenItems);
        float animatedStolenItems = chestStealerProgressAnimator.getDisplayedHealth();

        // 使用动画后的值来计算进度
        float progress = totalItems > 0 ? Math.min(animatedStolenItems / (float) totalItems, 1.0f) : 0.0f;
        int barColor = 0xFFFFFFFF;
        float foregroundWidth = Math.max(progressBarWidth * progress, progressBarRadius * 2.0f);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(guiGraphics.pose(), progressBarX, progressBarY,
                foregroundWidth, progressBarHeight, progressBarRadius, barColor);
        StencilUtils.erase(true);
        RenderUtils.fillBound(guiGraphics.pose(), progressBarX, progressBarY,
                foregroundWidth, progressBarHeight, barColor);
        StencilUtils.dispose();
    }

    private static void renderScaffoldView(GuiGraphics guiGraphics, CustomTextRenderer font,
                                           float pillX, float pillY, float totalWidth, float pillHeight,
                                           float textScale, float watermarkSize,
                                           Scaffold scaffoldModule) {
        int blocks = scaffoldModule != null ? scaffoldModule.getTotalBlocksInHotbar() : 0;

        if (scaffoldProgressAnimator == null) {
            scaffoldProgressAnimator = new ProgressBarAnimator((float) blocks, 2.0f);
        }
        scaffoldProgressAnimator.update((float) blocks);

        float animatedBlocks = scaffoldProgressAnimator.getDisplayedHealth();
        float scaledPadding = PADDING * watermarkSize;

        String scaffoldText = "Scaffold   Blocks: " + blocks;
        float scaffoldTextWidth = font.getWidth(scaffoldText, textScale);
        float progressBarHeight = 4.0f;
        float sidePadding = scaledPadding * 3.0f;
        float progressBarWidth = totalWidth - sidePadding * 2 - scaffoldTextWidth - scaledPadding;

        float textX = pillX + sidePadding;
        float textY = (float) (pillY + (pillHeight - font.getHeight(true, textScale)) / 2);
        font.render(guiGraphics.pose(), scaffoldText, textX, textY, Color.WHITE, true, textScale);

        float progressBarX = textX + scaffoldTextWidth + scaledPadding;
        float progressBarY = (float) (pillY + (pillHeight - progressBarHeight) / 2);
        float progressBarRadius = progressBarHeight / 2.0f;

        RenderUtils.drawRoundedRect(guiGraphics.pose(), progressBarX, progressBarY,
                progressBarWidth, progressBarHeight, progressBarRadius, 0x80000000);

        float progress = Math.min(animatedBlocks / 64.0f, 1.0f);
        int barColor = 0xFFFFFFFF;
        float foregroundWidth = Math.max(progressBarWidth * progress, progressBarRadius * 2.0f);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(guiGraphics.pose(), progressBarX, progressBarY,
                foregroundWidth, progressBarHeight, progressBarRadius, barColor);
        StencilUtils.erase(true);
        RenderUtils.fillBound(guiGraphics.pose(), progressBarX, progressBarY,
                foregroundWidth, progressBarHeight, barColor);
        StencilUtils.dispose();
    }

    public static class ProgressBarAnimator {
        private float displayedProgress;
        private long lastUpdateTime;
        private final float animationSpeed;

        public ProgressBarAnimator(float initialProgress, float animationSpeed) {
            this.displayedProgress = initialProgress;
            this.lastUpdateTime = System.currentTimeMillis();
            this.animationSpeed = animationSpeed;
        }

        public void update(float targetProgress) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - this.lastUpdateTime;
            this.lastUpdateTime = currentTime;

            if (Math.abs(targetProgress - this.displayedProgress) < 0.1f) {
                this.displayedProgress = targetProgress;
                return;
            }

            float change = (targetProgress - this.displayedProgress) * (deltaTime / 1000.0f) * this.animationSpeed;
            this.displayedProgress += change;

            if (targetProgress > this.displayedProgress && change < 0) {
                this.displayedProgress = targetProgress;
            } else if (targetProgress < this.displayedProgress && change > 0) {
                this.displayedProgress = targetProgress;
            }
        }

        public void reset(float newProgress) {
            this.displayedProgress = newProgress;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public float getDisplayedHealth() {
            return displayedProgress;
        }
    }
}