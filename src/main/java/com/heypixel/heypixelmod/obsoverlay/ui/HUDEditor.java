package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class HUDEditor {
    private static final Minecraft mc = Minecraft.getInstance();
    private static HUDEditor instance;
    
    // HUD元素的位置数据
    private final Map<String, HUDElement> hudElements = new HashMap<>();
    
    // 拖拽相关变量
    private HUDElement draggingElement = null;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double elementStartX = 0;
    private double elementStartY = 0;
    
    // 是否在编辑模式
    private boolean editMode = false;
    
    public HUDEditor() {
        instance = this;
        initializeHUDElements();
        Naven.getInstance().getEventManager().register(this);
    }
    
    public static HUDEditor getInstance() {
        if (instance == null) {
            instance = new HUDEditor();
        }
        return instance;
    }
    
    /**
     * 初始化HUD元素
     */
    private void initializeHUDElements() {
        // 水印
        hudElements.put("watermark", new HUDElement("watermark", "Watermark", 5, 5, 200, 25));
        
        // 模块列表 - 默认右上角，碰撞箱最右边对齐屏幕右边
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        hudElements.put("arraylist", new HUDElement("arraylist", "ArrayList", 
            screenWidth - 250, 1, 250, 300));
            
        // TargetHUD - 默认屏幕中心偏右下
        hudElements.put("targethud", new HUDElement("targethud", "TargetHUD", 
            screenWidth / 2.0F + 10.0F, screenHeight / 2.0F + 10.0F, 160, 50));
    }
    
    /**
     * 检查是否应该启用编辑模式
     */
    @EventTarget
    public void onRender2D(EventRender2D event) {
        // 检查聊天界面是否打开
        boolean shouldEdit = mc.screen instanceof ChatScreen;
        
        if (shouldEdit != editMode) {
            editMode = shouldEdit;
        }
        
        if (editMode) {
            renderEditMode(event);
        }
    }
    
    /**
     * 开始拖拽
     */
    private void startDragging(double mouseX, double mouseY) {
        for (HUDElement element : hudElements.values()) {
            if (element.isHovering(mouseX, mouseY)) {
                draggingElement = element;
                dragStartX = mouseX;
                dragStartY = mouseY;
                elementStartX = element.x;
                elementStartY = element.y;
                break;
            }
        }
    }
    
    /**
     * 停止拖拽
     */
    private void stopDragging() {
        draggingElement = null;
    }
    
    /**
     * 更新拖拽位置
     */
    private void updateDragging() {
        if (draggingElement != null) {
            double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
            
            draggingElement.x = elementStartX + (mouseX - dragStartX);
            draggingElement.y = elementStartY + (mouseY - dragStartY);
            
            // 移除边界检查，允许拖拽出屏幕
        }
    }
    
    /**
     * 渲染编辑模式
     */
    private void renderEditMode(EventRender2D event) {
        // 更新拖拽中的元素位置
        updateDragging();
        
        // 获取当前鼠标位置
        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        
        // 处理鼠标点击
        handleMouseInput(mouseX, mouseY);
        
        // 渲染HUD元素的边框和标签
        CustomTextRenderer font = Fonts.opensans;
        
        for (HUDElement element : hudElements.values()) {
            // 只有在鼠标悬浮或正在拖拽时才绘制边框
            boolean hovering = element.isHovering(mouseX, mouseY);
            boolean shouldDrawBorder = hovering || element == draggingElement;
            
            if (shouldDrawBorder) {
                // 绘制边框
                int borderColor = element == draggingElement ? Color.RED.getRGB() : Color.YELLOW.getRGB();
                drawElementBorder(event.getStack(), element, borderColor);
                
                // 绘制标签
                font.render(event.getStack(), element.displayName, 
                    element.x + 2, element.y - 12, Color.WHITE, true, 0.3);
            }
        }
        
        // 渲染提示文本
        font.render(event.getStack(), "HUD Edit Mode - Drag elements to reposition", 
            10, mc.getWindow().getGuiScaledHeight() - 20, Color.YELLOW, true, 0.3);
    }
    
    /**
     * 处理鼠标输入
     */
    private void handleMouseInput(double mouseX, double mouseY) {
        boolean mousePressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        
        if (mousePressed && draggingElement == null) {
            // 开始拖拽
            startDragging(mouseX, mouseY);
        } else if (!mousePressed && draggingElement != null) {
            // 停止拖拽
            stopDragging();
        }
    }
    
    /**
     * 绘制元素边框
     */
    private void drawElementBorder(com.mojang.blaze3d.vertex.PoseStack poseStack, HUDElement element, int color) {
        // 绘制边框
        RenderUtils.fill(poseStack, 
            (float)element.x, (float)element.y, 
            (float)(element.x + element.width), (float)(element.y + 1), color);
        RenderUtils.fill(poseStack, 
            (float)element.x, (float)(element.y + element.height - 1), 
            (float)(element.x + element.width), (float)(element.y + element.height), color);
        RenderUtils.fill(poseStack, 
            (float)element.x, (float)element.y, 
            (float)(element.x + 1), (float)(element.y + element.height), color);
        RenderUtils.fill(poseStack, 
            (float)(element.x + element.width - 1), (float)element.y, 
            (float)(element.x + element.width), (float)(element.y + element.height), color);
    }
    
    /**
     * 获取HUD元素位置
     */
    public HUDElement getHUDElement(String name) {
        return hudElements.get(name);
    }
    
    /**
     * 获取所有HUD元素
     */
    public java.util.Collection<HUDElement> getAllElements() {
        return hudElements.values();
    }
    
    /**
     * 是否在编辑模式
     */
    public boolean isEditMode() {
        return editMode;
    }
    
    /**
     * HUD元素类
     */
    public static class HUDElement {
        public String name;
        public String displayName;
        public double x, y;
        public double width, height;
        
        public HUDElement(String name, String displayName, double x, double y, double width, double height) {
            this.name = name;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public boolean isHovering(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}