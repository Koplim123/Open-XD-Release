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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HUDEditor {
    private static final Minecraft mc = Minecraft.getInstance();
    private static HUDEditor instance;
    // 配置文件路径
    private final File configFile = new File(mc.gameDirectory, "Naven/hud.cfg");

    // 存储所有HUD元素
    private final Map<String, HUDElement> hudElements = new HashMap<>();

    // 拖拽操作所需变量
    private HUDElement draggingElement = null; // 当前正在拖拽的元素
    private double dragStartX = 0; // 拖拽开始时鼠标的X坐标
    private double dragStartY = 0; // 拖拽开始时鼠标的Y坐标
    private double elementStartX = 0; // 拖拽开始时元素的X坐标
    private double elementStartY = 0; // 拖拽开始时元素的Y坐标

    // 是否处于编辑模式
    private boolean editMode = false;

    public HUDEditor() {
        instance = this;
        initializeHUDElements(); // 初始化所有HUD元素
        loadHUDConfig(); // 从配置文件加载HUD元素位置
        Naven.getInstance().getEventManager().register(this); // 注册事件监听器
    }

    public static HUDEditor getInstance() {
        if (instance == null) {
            instance = new HUDEditor();
        }
        return instance;
    }

    /**
     * 初始化所有HUD元素及其默认位置和大小
     */
    private void initializeHUDElements() {
        // 添加 "watermark" 元素
        hudElements.put("watermark", new HUDElement("watermark", "Watermark", 5, 5, 200, 25));

        // 添加 "arraylist" 元素，默认在屏幕右上角
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        hudElements.put("arraylist", new HUDElement("arraylist", "ArrayList",
                screenWidth - 250, 1, 250, 300));

        // 添加 "targethud" 元素，默认在屏幕中心偏右下
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        hudElements.put("targethud", new HUDElement("targethud", "TargetHUD",
                screenWidth / 2.0F + 10.0F, screenHeight / 2.0F + 10.0F, 160, 50));
        
        // 添加 "itemscounter" 元素
        hudElements.put("itemscounter", new HUDElement("itemscounter", "Items Counter", 10, 10, 100, 80));
    }

    /**
     * 从 "Naven/hud.cfg" 文件加载HUD元素的位置配置
     */
    private void loadHUDConfig() {
        Properties properties = new Properties();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);
                // 遍历所有HUD元素，并从配置文件中读取它们的位置
                for (Map.Entry<String, HUDElement> entry : hudElements.entrySet()) {
                    String name = entry.getKey();
                    HUDElement element = entry.getValue();
                    element.x = Double.parseDouble(properties.getProperty(name + ".x", String.valueOf(element.x)));
                    element.y = Double.parseDouble(properties.getProperty(name + ".y", String.valueOf(element.y)));
                }
            } catch (IOException e) {
                // 如果加载失败，打印错误信息
                e.printStackTrace();
            }
        }
    }

    /**
     * 将当前HUD元素的位置保存到 "Naven/hud.cfg" 文件
     */
    private void saveHUDConfig() {
        Properties properties = new Properties();
        // 将每个元素的位置信息存入Properties对象
        for (HUDElement element : hudElements.values()) {
            properties.setProperty(element.name + ".x", String.valueOf(element.x));
            properties.setProperty(element.name + ".y", String.valueOf(element.y));
        }
        try {
            // 确保父目录存在
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            // 将配置写入文件
            try (FileWriter writer = new FileWriter(configFile)) {
                properties.store(writer, "HUD Elements Positions");
            }
        } catch (IOException e) {
            // 如果保存失败，打印错误信息
            e.printStackTrace();
        }
    }

    /**
     * 在每帧渲染时调用，用于检查是否进入或退出编辑模式
     */
    @EventTarget
    public void onRender2D(EventRender2D event) {
        // 当玩家打开聊天界面时，进入编辑模式
        boolean shouldEdit = mc.screen instanceof ChatScreen;

        if (shouldEdit != editMode) {
            editMode = shouldEdit;
        }

        // 如果在编辑模式下，则渲染编辑界面
        if (editMode) {
            renderEditMode(event);
        }
    }

    /**
     * 当鼠标点击时，开始拖拽选中的HUD元素
     */
    private void startDragging(double mouseX, double mouseY) {
        // 遍历所有元素，检查鼠标是否悬停在某个元素上
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
     * 当鼠标释放时，停止拖拽并保存配置
     */
    private void stopDragging() {
        if (draggingElement != null) {
            draggingElement = null;
            saveHUDConfig(); // 保存配置
        }
    }

    /**
     * 在拖拽过程中，实时更新元素的位置
     */
    private void updateDragging() {
        if (draggingElement != null) {
            // 获取当前鼠标在GUI中的缩放坐标
            double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

            // 根据鼠标移动的距离更新元素位置
            draggingElement.x = elementStartX + (mouseX - dragStartX);
            draggingElement.y = elementStartY + (mouseY - dragStartY);
        }
    }

    /**
     * 渲染编辑模式的UI，包括元素边框和提示信息
     */
    private void renderEditMode(EventRender2D event) {
        updateDragging(); // 更新拖拽位置

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        handleMouseInput(mouseX, mouseY); // 处理鼠标输入

        CustomTextRenderer font = Fonts.opensans;

        // 为所有HUD元素绘制边框和名称标签
        for (HUDElement element : hudElements.values()) {
            boolean hovering = element.isHovering(mouseX, mouseY);
            boolean shouldDrawBorder = hovering || element == draggingElement;

            if (shouldDrawBorder) {
                // 正在拖拽的元素边框为红色，悬停的为黄色
                int borderColor = element == draggingElement ? Color.RED.getRGB() : Color.YELLOW.getRGB();
                drawElementBorder(event.getStack(), element, borderColor);

                // 在元素左上角绘制其名称
                font.render(event.getStack(), element.displayName,
                        element.x + 2, element.y - 12, Color.WHITE, true, 0.3);
            }
        }

        // 在屏幕左下角渲染提示文本
        font.render(event.getStack(), "HUD Edit Mode - Drag elements to reposition",
                10, mc.getWindow().getGuiScaledHeight() - 20, Color.YELLOW, true, 0.3);
    }

    /**
     * 处理鼠标的按下和释放事件
     */
    private void handleMouseInput(double mouseX, double mouseY) {
        // 检测鼠标左键是否被按下
        boolean mousePressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (mousePressed && draggingElement == null) {
            startDragging(mouseX, mouseY); // 如果没有在拖拽，则开始拖拽
        } else if (!mousePressed && draggingElement != null) {
            stopDragging(); // 如果松开鼠标，则停止拖拽
        }
    }

    /**
     * 绘制指定元素的边框
     */
    private void drawElementBorder(com.mojang.blaze3d.vertex.PoseStack poseStack, HUDElement element, int color) {
        // 绘制四条线组成一个矩形边框
        RenderUtils.fill(poseStack, (float)element.x, (float)element.y, (float)(element.x + element.width), (float)(element.y + 1), color);
        RenderUtils.fill(poseStack, (float)element.x, (float)(element.y + element.height - 1), (float)(element.x + element.width), (float)(element.y + element.height), color);
        RenderUtils.fill(poseStack, (float)element.x, (float)element.y, (float)(element.x + 1), (float)(element.y + element.height), color);
        RenderUtils.fill(poseStack, (float)(element.x + element.width - 1), (float)element.y, (float)(element.x + element.width), (float)(element.y + element.height), color);
    }

    /**
     * 根据名称获取HUD元素
     */
    public HUDElement getHUDElement(String name) {
        return hudElements.get(name);
    }

    /**
     * 获取所有HUD元素的集合
     */
    public java.util.Collection<HUDElement> getAllElements() {
        return hudElements.values();
    }

    /**
     * 检查当前是否处于编辑模式
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * 代表一个HUD元素的内部类
     */
    public static class HUDElement {
        public String name; // 唯一名称
        public String displayName; // 显示名称
        public double x, y; // 位置
        public double width, height; // 尺寸

        public HUDElement(String name, String displayName, double x, double y, double width, double height) {
            this.name = name;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        /**
         * 检查鼠标是否悬停在此元素上
         */
        public boolean isHovering(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}