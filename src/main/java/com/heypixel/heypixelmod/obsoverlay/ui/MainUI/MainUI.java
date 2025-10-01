package com.heypixel.heypixelmod.obsoverlay.ui.MainUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.BlurInit2;

import java.util.ArrayList;
import java.util.List;

public class MainUI extends Screen {
	private static final Minecraft mc = Minecraft.getInstance();
	private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("heypixel", "textures/images/background.png");

	private static final int BUTTON_WIDTH = 240;
	private static final int BUTTON_HEIGHT = 32;
	private static final int BUTTON_GAP = 10;
	private static final float BUTTON_RADIUS = 10.0F;
	private static final float PANEL_RADIUS = 12.0F;
	private static final int PANEL_PADDING_X = 16;
	private static final int PANEL_PADDING_Y = 16;
	private static final int BLUR_STRENGTH = 8;

	private final List<MenuButton> buttons = new ArrayList<>();
	private int hoveredIndex = -1;

	public MainUI() {
		super(Component.literal("Naven-XD"));
	}

	@Override
	protected void init() {
		buttons.clear();
		BlurInit2.init();

		int buttonCount = 4;
		int totalHeight = buttonCount * BUTTON_HEIGHT + (buttonCount - 1) * BUTTON_GAP;
		int startY = (this.height - totalHeight) / 2;
		int x = (this.width - BUTTON_WIDTH) / 2;

		buttons.add(new MenuButton(0, "Singleplayer", x, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 0));
		buttons.add(new MenuButton(1, "Multiplayer", x, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 1));
		buttons.add(new MenuButton(2, "Settings", x, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 2));
		buttons.add(new MenuButton(3, "Quit", x, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 3));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		PoseStack stack = guiGraphics.pose();

		renderBackgroundTexture(guiGraphics);
		renderBlur(guiGraphics);
		renderTitle(stack);
		renderButtonsPanel(stack);

		hoveredIndex = -1;
		for (int i = 0; i < buttons.size(); i++) {
			MenuButton button = buttons.get(i);
			boolean hovered = isInside(mouseX, mouseY, button.x, button.y, BUTTON_WIDTH, BUTTON_HEIGHT);
			if (hovered) hoveredIndex = i;
			renderButton(stack, button, hovered);
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void renderBackgroundTexture(GuiGraphics g) {
		// 渲染原始背景
		g.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
	}

	private void renderBlur(GuiGraphics guiGraphics) {
		// 应用真正的模糊效果
		BlurInit2.renderBlur(guiGraphics, BLUR_STRENGTH);
	}

	private void renderTitle(PoseStack stack) {
		CustomTextRenderer font = Fonts.opensans;
		if (font == null) return;

		font.setAlpha(1.0F);
		String title = "Naven-XD";
		// 使用更大的缩放比例，在更高分辨率下渲染后再缩小，以获得更锐利的边缘
		double scale = 2.0;
		float textWidth = font.getWidth(title, scale);
		float x = (this.width - textWidth) / 2.0F;
		float y = 16.0F;

		// 直接渲染文本，不使用任何模糊效果（第5个参数设为false）
		font.render(stack, title, x, y, java.awt.Color.WHITE, false, scale);
	}

	private void renderButtonsPanel(PoseStack stack) {
		if (buttons.isEmpty()) return;
		float top = buttons.get(0).y;
		float bottom = buttons.get(buttons.size() - 1).y + BUTTON_HEIGHT;
		float height = bottom - top;
		float width = BUTTON_WIDTH + PANEL_PADDING_X * 2.0F;
		float x = (this.width - BUTTON_WIDTH) / 2.0F - PANEL_PADDING_X;
		float y = top - PANEL_PADDING_Y;
		float h = height + PANEL_PADDING_Y * 2.0F;

		int panelColor = Colors.getColor(255, 255, 255, 60); // 半透明白色
		drawPureRoundedRect(stack, x, y, width, h, PANEL_RADIUS, panelColor);
	}

	private void renderButton(PoseStack stack, MenuButton button, boolean hovered) {
		int baseAlpha = hovered ? 90 : 45; // 透明度在悬停时增强
		int bg = Colors.getColor(255, 255, 255, baseAlpha);
		drawPureRoundedRect(stack, button.x, button.y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, bg);

		CustomTextRenderer font = Fonts.opensans;
		if (font != null) {
			font.setAlpha(1.0F);
			java.awt.Color color = hovered ? new java.awt.Color(20, 24, 30) : new java.awt.Color(230, 236, 245);
			double scale = 0.7;
			float tw = font.getWidth(button.label, scale);
			float th = (float) font.getHeight(false, scale);
			float tx = button.x + (BUTTON_WIDTH - tw) / 2.0F;
			float ty = button.y + (BUTTON_HEIGHT - th) / 2.0F;
			font.render(stack, button.label, tx, ty, color, false, scale);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (MenuButton b : buttons) {
				if (isInside((int) mouseX, (int) mouseY, b.x, b.y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
					handleClick(b.id);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean isInside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void handleClick(int id) {
		switch (id) {
			case 0 -> mc.setScreen(new SelectWorldScreen(this));
			case 1 -> mc.setScreen(new JoinMultiplayerScreen(this));
			case 2 -> openVanillaOptions();
			case 3 -> mc.stop();
		}
	}

	private void openVanillaOptions() {
		try {
			Options options = mc.options;
			Class<?> clazz = Class.forName("net.minecraft.client.gui.screens.options.OptionsScreen");
			java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(Screen.class, Options.class);
			Screen screen = (Screen) ctor.newInstance(this, options);
			mc.setScreen(screen);
		} catch (Throwable t) {
			try {
				Class<?> clazz = Class.forName("net.minecraft.client.gui.screens.OptionsScreen");
				java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(Screen.class, Options.class);
				Screen screen = (Screen) ctor.newInstance(this, mc.options);
				mc.setScreen(screen);
			} catch (Throwable ignored) {
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawPureRoundedRect(PoseStack stack, float x, float y, float width, float height, float radius, int color) {
		if (radius <= 0) {
			RenderUtils.fill(stack, x, y, x + width, y + height, color);
			return;
		}

		radius = Math.min(radius, Math.min(width, height) / 2.0F);

		Matrix4f matrix = stack.last().pose();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
		float red = (float)(color >> 16 & 0xFF) / 255.0F;
		float green = (float)(color >> 8 & 0xFF) / 255.0F;
		float blue = (float)(color & 0xFF) / 255.0F;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(matrix, x + radius, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + radius, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width - radius, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width - radius, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		tesselator.end();

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(matrix, x + radius, y, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + radius, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width - radius, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width - radius, y, 0.0F).color(red, green, blue, alpha).endVertex();
		tesselator.end();

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(matrix, x + radius, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + radius, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width - radius, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width - radius, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		tesselator.end();

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(matrix, x, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + radius, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + radius, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		tesselator.end();

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(matrix, x + width - radius, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width - radius, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width, y + height - radius, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x + width, y + radius, 0.0F).color(red, green, blue, alpha).endVertex();
		tesselator.end();

		// 计算圆角的顶点数量（根据半径调整精度）
		int segments = (int)Math.min(Math.max(radius, 8.0F), 32.0F);

		// 绘制左上角圆角
		drawCorner(matrix, x + radius, y + radius, radius, 180, 270, segments, red, green, blue, alpha);

		// 绘制右上角圆角
		drawCorner(matrix, x + width - radius, y + radius, radius, 270, 360, segments, red, green, blue, alpha);

		// 绘制左下角圆角
		drawCorner(matrix, x + radius, y + height - radius, radius, 90, 180, segments, red, green, blue, alpha);

		// 绘制右下角圆角
		drawCorner(matrix, x + width - radius, y + height - radius, radius, 0, 90, segments, red, green, blue, alpha);

		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private void drawCorner(Matrix4f matrix, float centerX, float centerY, float radius,
							int startAngle, int endAngle, int segments,
							float red, float green, float blue, float alpha) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		// 中心点
		buffer.vertex(matrix, centerX, centerY, 0.0F).color(red, green, blue, alpha).endVertex();

		// 圆弧上的点
		for (int i = startAngle; i <= endAngle; i += (endAngle - startAngle) / segments) {
			double angle = Math.toRadians(i);
			float px = centerX + (float)(Math.cos(angle) * radius);
			float py = centerY + (float)(Math.sin(angle) * radius);
			buffer.vertex(matrix, px, py, 0.0F).color(red, green, blue, alpha).endVertex();
		}

		// 确保闭合
		double endAngleRad = Math.toRadians(endAngle);
		float endX = centerX + (float)(Math.cos(endAngleRad) * radius);
		float endY = centerY + (float)(Math.sin(endAngleRad) * radius);
		buffer.vertex(matrix, endX, endY, 0.0F).color(red, green, blue, alpha).endVertex();

		tesselator.end();
	}

	private static final class MenuButton {
		final int id;
		final String label;
		final int x;
		final int y;

		MenuButton(int id, String label, int x, int y) {
			this.id = id;
			this.label = label;
			this.x = x;
			this.y = y;
		}
	}
}