package com.heypixel.heypixelmod.obsoverlay.ui.MainUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;

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

	private final List<MenuButton> buttons = new ArrayList<>();
	private int hoveredIndex = -1;

	public MainUI() {
		super(Component.literal("Naven-XD Client"));
	}

	@Override
	protected void init() {
		buttons.clear();

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
		g.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
	}

	private void renderTitle(PoseStack stack) {
		CustomTextRenderer font = Fonts.opensans;
		if (font == null) return;

		font.setAlpha(1.0F);
		String title = "Naven-XD Client";
		double scale = 1.2;
		float textWidth = font.getWidth(title, scale);
		float x = (this.width - textWidth) / 2.0F;
		float y = this.height / 2.0F - 110.0F;
		font.render(stack, title, x, y, java.awt.Color.WHITE, true, scale);
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
		RenderUtils.drawRoundedRect(stack, x, y, width, h, PANEL_RADIUS, panelColor);
	}

	private void renderButton(PoseStack stack, MenuButton button, boolean hovered) {
		int baseAlpha = hovered ? 90 : 45; // 透明度在悬停时增强
		int bg = Colors.getColor(255, 255, 255, baseAlpha);
		RenderUtils.drawRoundedRect(stack, button.x, button.y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, bg);

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