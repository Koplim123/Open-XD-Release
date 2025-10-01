package com.heypixel.heypixelmod.obsoverlay.ui.MainUI;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class MainUI extends Screen {
	private static final Minecraft mc = Minecraft.getInstance();
	private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("heypixel", "textures/images/background.png");
	
	private static final int BUTTON_WIDTH = 240;
	private static final int BUTTON_HEIGHT = 40;
	private static final int BUTTON_SPACING = 12;
	private static final float CORNER_RADIUS = 10.0f;
	
	private boolean textureLoaded = false;
	private Button[] buttons;
	
	public MainUI() {
		super(Component.literal("Naven-XD"));
	}

	@Override
	protected void init() {
		super.init();
		textureLoaded = checkTextureLoaded();
		
		int centerX = this.width / 2;
		int startY = this.height / 2 - 80;
		
		buttons = new Button[] {
			new Button(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT, "SinglePlayer", this::openSingleplayer),
			new Button(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT, "Multiplayer", this::openMultiplayer),
			new Button(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, BUTTON_WIDTH, BUTTON_HEIGHT, "Settings", this::openSettings),
			new Button(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3, BUTTON_WIDTH, BUTTON_HEIGHT, "Exit", this::quit, true)
		};
	}

	private boolean checkTextureLoaded() {
		try {
			mc.getResourceManager().getResourceOrThrow(BACKGROUND_TEXTURE);
			return true;
		} catch (Exception e) {
			System.err.println("Failed to load background texture: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		drawBackground(guiGraphics);
		renderTitle(guiGraphics);
		renderButtonBackground(guiGraphics);
		
		for (Button button : buttons) {
			button.render(guiGraphics, mouseX, mouseY, partialTick);
		}
		
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}
	
	private void renderButtonBackground(GuiGraphics guiGraphics) {
		if (buttons == null || buttons.length == 0) return;
		
		int padding = 20;
		int minY = buttons[0].y - padding;
		int maxY = buttons[buttons.length - 1].y + buttons[buttons.length - 1].height + padding;
		int centerX = this.width / 2;
		int panelWidth = BUTTON_WIDTH + padding * 2;
		int panelHeight = maxY - minY;
		
		float panelX = centerX - panelWidth / 2.0f;
		float panelY = minY;
		
		drawSmoothRoundedRect(guiGraphics, panelX, panelY, panelWidth, panelHeight, 12.0f, rgba(0, 0, 0, 120));
	}
	
	private void drawSmoothRoundedRect(GuiGraphics guiGraphics, float x, float y, float width, float height, float radius, int color) {
		radius = Math.min(radius, Math.min(width, height) / 2.0f);
		int xi = (int)x;
		int yi = (int)y;
		int ri = (int)radius;
		int wi = (int)width;
		int hi = (int)height;
		
		guiGraphics.fill(xi + ri, yi, xi + wi - ri, yi + hi, color);
		guiGraphics.fill(xi, yi + ri, xi + ri, yi + hi - ri, color);
		guiGraphics.fill(xi + wi - ri, yi + ri, xi + wi, yi + hi - ri, color);
		
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		
		com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
		com.mojang.blaze3d.vertex.BufferBuilder buffer = tesselator.getBuilder();
		org.joml.Matrix4f matrix = guiGraphics.pose().last().pose();
		
		float a = ((color >> 24) & 0xFF) / 255.0f;
		float r = ((color >> 16) & 0xFF) / 255.0f;
		float g = ((color >> 8) & 0xFF) / 255.0f;
		float b = (color & 0xFF) / 255.0f;
		
		int segments = 16;
		
		drawCircleCorner(buffer, tesselator, matrix, x + radius, y + radius, radius, 180, 270, segments, r, g, b, a);
		drawCircleCorner(buffer, tesselator, matrix, x + width - radius, y + radius, radius, 270, 360, segments, r, g, b, a);
		drawCircleCorner(buffer, tesselator, matrix, x + width - radius, y + height - radius, radius, 0, 90, segments, r, g, b, a);
		drawCircleCorner(buffer, tesselator, matrix, x + radius, y + height - radius, radius, 90, 180, segments, r, g, b, a);
		
		RenderSystem.disableBlend();
	}
	
	private void drawCircleCorner(com.mojang.blaze3d.vertex.BufferBuilder buffer, com.mojang.blaze3d.vertex.Tesselator tesselator,
								   org.joml.Matrix4f matrix, float cx, float cy, float radius, 
								   float startAngle, float endAngle, int segments, float r, float g, float b, float a) {
		buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_FAN, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a).endVertex();
		
		for (int i = 0; i <= segments; i++) {
			double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / (double)segments);
			float px = cx + (float)Math.cos(angle) * radius;
			float py = cy + (float)Math.sin(angle) * radius;
			buffer.vertex(matrix, px, py, 0).color(r, g, b, a).endVertex();
		}
		
		tesselator.end();
	}
	
	private int rgba(int r, int g, int b, int a) {
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private void drawBackground(@Nonnull GuiGraphics guiGraphics) {
		guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
		
		if (textureLoaded) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
			RenderSystem.disableBlend();
		}
		
		guiGraphics.fill(0, 0, this.width, this.height, 0x50000000);
	}

	private void renderTitle(GuiGraphics guiGraphics) {
		String title = "Naven-XD";
		int titleWidth = this.font.width(title) * 2;
		int titleX = this.width / 2 - titleWidth / 2;
		int titleY = this.height / 2 - 160;
		
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
		guiGraphics.drawString(this.font, title, titleX / 2 + 2, titleY / 2 + 2, 0x40000000, false);
		guiGraphics.drawString(this.font, title, titleX / 2, titleY / 2, 0xFFFFFFFF, false);
		guiGraphics.pose().popPose();
	}

	private void openSingleplayer() {
		mc.setScreen(new SelectWorldScreen(this));
	}

	private void openMultiplayer() {
		mc.setScreen(new JoinMultiplayerScreen(this));
	}

	private void openSettings() {
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
				System.err.println("Failed to open options screen");
			}
		}
	}

	private void quit() {
		mc.stop();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (Button btn : buttons) {
				if (btn.isHovered((int)mouseX, (int)mouseY)) {
					btn.onClick();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private static class Button {
		private final int x, y, width, height;
		private final String text;
		private final Runnable action;
		private final boolean isDanger;
		private float hoverProgress = 0.0f;
		
		public Button(int x, int y, int width, int height, String text, Runnable action) {
			this(x, y, width, height, text, action, false);
		}
		
		public Button(int x, int y, int width, int height, String text, Runnable action, boolean isDanger) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.text = text;
			this.action = action;
			this.isDanger = isDanger;
		}
		
		public boolean isHovered(int mouseX, int mouseY) {
			return RenderUtils.isHoveringBound(mouseX, mouseY, x, y, width, height);
		}
		
		public void onClick() {
			action.run();
		}
		
		public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			boolean hovered = isHovered(mouseX, mouseY);
			
			hoverProgress += (hovered ? 0.15f : -0.15f);
			hoverProgress = Math.max(0.0f, Math.min(1.0f, hoverProgress));
			
			int baseColor;
			int hoverColor;
			int textColor;
			
			if (isDanger) {
				baseColor = rgba(220, 38, 38, 100);
				hoverColor = rgba(220, 38, 38, 150);
				textColor = 0xFFFFFFFF;
			} else {
				baseColor = rgba(255, 255, 255, 60);
				hoverColor = rgba(255, 255, 255, 100);
				textColor = hovered ? 0xFF000000 : rgba(230, 236, 245, 240);
			}
			
			int color = interpolateColor(baseColor, hoverColor, hoverProgress);
			
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			
			RenderUtils.drawRoundedRect(guiGraphics.pose(), x, y, width, height, CORNER_RADIUS, color);
			
			if (hoverProgress > 0.01f) {
				int overlayColor = rgba(255, 255, 255, (int)(30 * hoverProgress));
				RenderUtils.drawRoundedRect(guiGraphics.pose(), x, y, width, height, CORNER_RADIUS, overlayColor);
			}
			
			Minecraft mc = Minecraft.getInstance();
			int textWidth = mc.font.width(text);
			int textX = x + (width - textWidth) / 2;
			int textY = y + (height - 8) / 2;
			
			guiGraphics.drawString(mc.font, text, textX, textY, textColor, false);
			
			RenderSystem.disableBlend();
		}
		
		private int rgba(int r, int g, int b, int a) {
			return (a << 24) | (r << 16) | (g << 8) | b;
		}
		
		private int interpolateColor(int color1, int color2, float progress) {
			int a1 = (color1 >> 24) & 0xFF;
			int r1 = (color1 >> 16) & 0xFF;
			int g1 = (color1 >> 8) & 0xFF;
			int b1 = color1 & 0xFF;
			
			int a2 = (color2 >> 24) & 0xFF;
			int r2 = (color2 >> 16) & 0xFF;
			int g2 = (color2 >> 8) & 0xFF;
			int b2 = color2 & 0xFF;
			
			int a = (int)(a1 + (a2 - a1) * progress);
			int r = (int)(r1 + (r2 - r1) * progress);
			int g = (int)(g1 + (g2 - g1) * progress);
			int b = (int)(b1 + (b2 - b1) * progress);
			
			return rgba(r, g, b, a);
		}
	}
}
