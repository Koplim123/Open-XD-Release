package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.AnimationUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 改良版Hotbar渲染器
 * 特性：
 * 1. 整个hotbar渲染blur模糊效果
 * 2. hotbar边框改为圆角
 * 3. 去掉hotbar原先的background
 * 4. 去掉hotbar原先的纹理和边框
 * 5. 在当前手持物品的格子边框渲染白色窄边框
 * 6. 切换物品时白色窄边框有左右滑动动画
 */
public class BetterHotBar {
    
    private static final Minecraft mc = Minecraft.getInstance();
    
    // 动画相关
    private static float currentSlotAnimation = 0.0f; // 当前选中槽位的动画值
    private static int lastSelectedSlot = 0; // 上一次选中的槽位
    
    // hotbar位置缓存（用于blur渲染）
    private static float lastHotbarX = 0;
    private static float lastHotbarY = 0;
    private static float lastHotbarWidth = 0;
    private static float lastHotbarHeight = 0;
    
    // 样式常量 - 参考原版hotbar尺寸
    private static final float HOTBAR_CORNER_RADIUS = 6.0f; // 圆角半径
    private static final int SLOT_SIZE = 20; // 每个槽位的大小（和原版相同）
    private static final int SLOT_SPACING = 0; // 槽位之间的间距（与原版一致）
    private static final int HOTBAR_PADDING_X = 4; // hotbar左右内边距
    private static final int HOTBAR_PADDING_Y = 2; // hotbar上下内边距（更小，贴近底部）
    private static final int SELECTED_BORDER_WIDTH = 1; // 选中边框宽度
    private static final int SELECTED_BORDER_COLOR = 0xFFFFFFFF; // 白色边框
    
    /**
     * 渲染自定义hotbar
     * @param guiGraphics GUI图形上下文
     * @param partialTick 部分tick
     */
    public static void renderCustomHotbar(GuiGraphics guiGraphics, float partialTick) {
        LocalPlayer player = mc.player;
        if (player == null) return;
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // 计算hotbar的位置和大小（参考原版hotbar）
        int totalSlots = 9;
        int hotbarWidth = totalSlots * SLOT_SIZE + (totalSlots - 1) * SLOT_SPACING + HOTBAR_PADDING_X * 2;
        int hotbarHeight = SLOT_SIZE + HOTBAR_PADDING_Y * 2;
        int hotbarX = (screenWidth - hotbarWidth) / 2;
        
        // 计算Y坐标：紧贴底部（不与经验条重合）
        int hotbarY = screenHeight - hotbarHeight;
        
        // 缓存位置用于blur渲染
        lastHotbarX = hotbarX;
        lastHotbarY = hotbarY;
        lastHotbarWidth = hotbarWidth;
        lastHotbarHeight = hotbarHeight;
        
        // 更新选中槽位动画
        updateSlotAnimation(player.getInventory().selected);
        
        // 1. 渲染选中槽位的白色边框（带动画）
        renderSelectedSlotBorder(poseStack, hotbarX, hotbarY);
        
        // 2. 渲染物品
        renderItems(guiGraphics, player, hotbarX, hotbarY);
        
        poseStack.popPose();
    }
    
    /**
     * 更新选中槽位的动画
     */
    private static void updateSlotAnimation(int selectedSlot) {
        float targetPosition = selectedSlot;
        
        // 如果槽位发生变化，重置动画
        if (selectedSlot != lastSelectedSlot) {
            lastSelectedSlot = selectedSlot;
        }
        
        // 平滑动画到目标位置
        currentSlotAnimation = AnimationUtils.getAnimationState(
                currentSlotAnimation, 
                targetPosition, 
                20.0f // 动画速度
        );
    }
    
    /**
     * 处理shader事件（用于blur渲染）
     * 这个方法需要通过EventShader事件调用
     */
    public static void onShader(EventShader e) {
        if (e.getType() == EventType.BLUR && shouldRenderCustomHotbar()) {
            // 渲染blur区域
            RenderUtils.drawRoundedRect(e.getStack(), lastHotbarX, lastHotbarY, 
                    lastHotbarWidth, lastHotbarHeight, HOTBAR_CORNER_RADIUS, Integer.MIN_VALUE);
        }
    }
    
    /**
     * 渲染选中槽位的白色边框（带动画）
     */
    private static void renderSelectedSlotBorder(PoseStack poseStack, int hotbarX, int hotbarY) {
        float slotX = hotbarX + HOTBAR_PADDING_X + currentSlotAnimation * (SLOT_SIZE + SLOT_SPACING);
        float slotY = hotbarY + HOTBAR_PADDING_Y;
        
        // 绘制白色圆角边框 - 使用RenderUtils的drawStencilRoundedRect或简单绘制边框
        float borderX = slotX - SELECTED_BORDER_WIDTH / 2.0f;
        float borderY = slotY - SELECTED_BORDER_WIDTH / 2.0f;
        float borderWidth = SLOT_SIZE + SELECTED_BORDER_WIDTH;
        float borderHeight = SLOT_SIZE + SELECTED_BORDER_WIDTH;
        
        // 绘制4条边来形成空心边框
        float cornerRadius = 4.0f;
        
        // 顶边
        RenderUtils.drawRoundedRectCustom(poseStack, borderX, borderY, borderWidth, SELECTED_BORDER_WIDTH,
                cornerRadius, cornerRadius, 0, 0, SELECTED_BORDER_COLOR);
        
        // 底边
        RenderUtils.drawRoundedRectCustom(poseStack, borderX, borderY + borderHeight - SELECTED_BORDER_WIDTH, 
                borderWidth, SELECTED_BORDER_WIDTH,
                0, 0, cornerRadius, cornerRadius, SELECTED_BORDER_COLOR);
        
        // 左边
        RenderUtils.drawRoundedRect(poseStack, borderX, borderY + SELECTED_BORDER_WIDTH, 
                SELECTED_BORDER_WIDTH, borderHeight - SELECTED_BORDER_WIDTH * 2, 0, SELECTED_BORDER_COLOR);
        
        // 右边
        RenderUtils.drawRoundedRect(poseStack, borderX + borderWidth - SELECTED_BORDER_WIDTH, borderY + SELECTED_BORDER_WIDTH, 
                SELECTED_BORDER_WIDTH, borderHeight - SELECTED_BORDER_WIDTH * 2, 0, SELECTED_BORDER_COLOR);
    }
    
    /**
     * 渲染所有物品
     */
    private static void renderItems(GuiGraphics guiGraphics, Player player, int hotbarX, int hotbarY) {
        for (int i = 0; i < 9; i++) {
            int slotX = hotbarX + HOTBAR_PADDING_X + i * (SLOT_SIZE + SLOT_SPACING);
            int slotY = hotbarY + HOTBAR_PADDING_Y;
            
            renderSlot(guiGraphics, slotX, slotY, i, player);
        }
    }
    
    /**
     * 渲染单个槽位的物品
     */
    private static void renderSlot(GuiGraphics guiGraphics, int x, int y, int slot, Player player) {
        ItemStack itemStack = player.getInventory().items.get(slot);
        
        if (!itemStack.isEmpty()) {
            // 渲染物品图标（物品是16x16，槽位是20x20，所以偏移2像素使其居中）
            guiGraphics.renderItem(itemStack, x + 2, y + 2);
            
            // 渲染物品数量
            guiGraphics.renderItemDecorations(mc.font, itemStack, x + 2, y + 2);
        }
    }
    
    /**
     * 检查是否应该渲染自定义hotbar
     */
    public static boolean shouldRenderCustomHotbar() {
        return mc.player != null && !mc.options.hideGui;
    }
}
