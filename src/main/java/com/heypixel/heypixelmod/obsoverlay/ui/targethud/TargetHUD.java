package com.heypixel.heypixelmod.obsoverlay.ui.targethud;

import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.HealthBarAnimator;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.HealthParticle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TargetHUD {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Map<UUID, HealthBarAnimator> healthAnimators = new HashMap<>();
    private static final Map<UUID, List<HealthParticle>> playerParticles = new HashMap<>();
    private static final Map<UUID, Float> lastHealth = new HashMap<>();
    private static final Random random = new Random();

    public static Vector4f render(GuiGraphics graphics, LivingEntity living, String style, float x, float y) {
        if ("Naven".equals(style)) {
            return renderNavenStyle(graphics, living, x, y);
        } else if ("New".equals(style)) {
            return renderNewStyle(graphics, living, x, y);
        } else if ("MoonLight".equals(style)) {
            return renderMoonLightV2Style(graphics, living, x, y);
        } else if ("Rise".equals(style)) {
            return renderRise(graphics, living, x, y);
        }
        return null;
    }

    private static Vector4f renderNavenStyle(GuiGraphics graphics, LivingEntity living, float x, float y) {
        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
        Vector4f blurMatrix = new Vector4f(x, y, width, 30.0F);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, width, 30.0F, 5.0F, HUD.headerColor);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, width, 30.0F, HUD.bodyColor);
        RenderUtils.fillBound(graphics.pose(), x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F, HUD.headerColor);
        StencilUtils.dispose();

        Fonts.harmony.render(graphics.pose(), targetName, (double)(x + 5.0F), (double)(y + 6.0F), Color.WHITE, true, 0.35F);
        Fonts.harmony.render(graphics.pose(), "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""), (double)(x + 5.0F), (double)(y + 17.0F), Color.WHITE, true, 0.35F);

        return blurMatrix;
    }

    private static Vector4f renderNewStyle(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float hudWidth = 140.0F;
        float hudHeight = 50.0F;
        Vector4f blurMatrix = new Vector4f(x, y, hudWidth, hudHeight);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, hudWidth, hudHeight, 8.0F, 0x80000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, hudWidth, hudHeight, 0x80000000);
        StencilUtils.dispose();

        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float nameX = x + 10.0F;
        float nameY = y + 8.0F;
        Fonts.harmony.render(graphics.pose(), "Name: " + targetName, (double)nameX, (double)nameY, Color.WHITE, true, 0.30F);

        String healthText = "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "");
        float healthTextX = x + 10.0F;
        float healthTextY = y + 20.0F;
        Fonts.harmony.render(graphics.pose(), healthText, (double)healthTextX, (double)healthTextY, Color.WHITE, true, 0.30F);

        float healthBarWidth = 120.0F;
        float healthBarHeight = 6.0F;
        float healthBarX = x + 10.0F;
        float healthBarY = y + 36.0F;

        if (healthBarX + healthBarWidth > x + hudWidth) {
            healthBarWidth = hudWidth - 20.0F;
        }

        RenderUtils.drawRoundedRect(graphics.pose(), healthBarX, healthBarY, healthBarWidth, healthBarHeight, 4.0F, 0x80FFFFFF);

        float healthRatio = living.getHealth() / living.getMaxHealth();
        if (healthRatio > 1.0F) healthRatio = 1.0F;
        float currentHealthWidth = healthBarWidth * healthRatio;

        if (currentHealthWidth > 0) {
            RenderUtils.fillBound(graphics.pose(), healthBarX, healthBarY, currentHealthWidth, healthBarHeight, 0xFFFFFFFF);
        }

        return blurMatrix;
    }

    private static Vector4f renderMoonLightV2Style(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float mlHudWidth = 150.0F;
        float mlHudHeight = 35.0F;
        Vector4f blurMatrix = new Vector4f(x, y, mlHudWidth, mlHudHeight);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, mlHudWidth, mlHudHeight, 4.0F, 0x80000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, mlHudWidth, mlHudHeight, 0x80000000);
        StencilUtils.dispose();

        String mlTargetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float mlNameX = x + 8.0F;
        float mlNameY = y + 8.0F;
        Fonts.harmony.render(graphics.pose(), mlTargetName, (double) mlNameX, (double) mlNameY, Color.WHITE, true, 0.30F);

        String mlHealthText = Math.round(living.getHealth()) + "/" + Math.round(living.getMaxHealth());
        float mlHealthTextX = x + 8.0F;
        float mlHealthTextY = y + 20.0F;
        Fonts.harmony.render(graphics.pose(), mlHealthText, (double) mlHealthTextX, (double) mlHealthTextY, Color.WHITE, true, 0.30F);

        float mlCircleX = x + mlHudWidth - 20.0F;
        float mlCircleY = y + mlHudHeight / 2.0F;
        float mlCircleRadius = 10.0F;
        float mlHealthPercent = Math.min(1.0f, Math.max(0.0f, living.getHealth() / living.getMaxHealth()));

        RenderUtils.drawHealthRing(
                graphics.pose(),
                mlCircleX,
                mlCircleY,
                mlCircleRadius,
                2.5F,
                mlHealthPercent
        );

        return blurMatrix;
    }

    private static Vector4f renderRise(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float hudWidth = 160.0F;
        float hudHeight = 45.0F;
        float avatarSize = 32.0F;
        float padding = 4.0F;

        Vector4f blurMatrix = new Vector4f(x, y, hudWidth, hudHeight);


        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, hudWidth, hudHeight, 6.0F, 0x70000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, hudWidth, hudHeight, 0x70000000);
        StencilUtils.dispose();


        float currentTotalHealth = living.getHealth() + living.getAbsorptionAmount();
        float previousHealth = lastHealth.getOrDefault(living.getUUID(), currentTotalHealth);

        if (currentTotalHealth < previousHealth) {
            int particleCount = random.nextInt(6) + 8;
            float avatarX = x + padding;
            float avatarY = y + (hudHeight - avatarSize) / 2;

            List<HealthParticle> particles = playerParticles.computeIfAbsent(living.getUUID(), k -> new CopyOnWriteArrayList<>());
            for (int i = 0; i < particleCount; i++) {
                particles.add(new HealthParticle(avatarX + avatarSize / 2, avatarY + avatarSize / 2));
            }
        }

        lastHealth.put(living.getUUID(), currentTotalHealth);

        List<HealthParticle> particles = playerParticles.get(living.getUUID());
        if (particles != null) {
            for (HealthParticle particle : particles) {
                particle.update();
                particle.render(graphics);
            }
            particles.removeIf(HealthParticle::isDead);
        }


        float avatarX = x + padding;
        float avatarY = y + (hudHeight - avatarSize) / 2;
        RenderUtils.drawRoundedRect(graphics.pose(), avatarX, avatarY, avatarSize, avatarSize, 4.0F, Color.WHITE.getRGB());

        ResourceLocation skinLocation = null;
        if (living instanceof Player player) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
                skinLocation = playerInfo.getSkinLocation();
            }
        }

        if (skinLocation != null) {
            if (living instanceof Player player) {
                graphics.blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 8, 8, 8, 8, 64, 64);
                graphics.blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 40, 8, 8, 8, 64, 64);
            } else {
                graphics.blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 0, 0, 16, 16, 16, 16);
            }
        } else {

            String noneText = "NONE";
            float noneTextWidth = Fonts.harmony.getWidth(noneText, 0.30F);
            float noneTextHeight = (float) Fonts.harmony.getHeight(true, 0.30F);
            float noneTextX = avatarX + (avatarSize - noneTextWidth) / 2.0F;
            float noneTextY = avatarY + (avatarSize - noneTextHeight) / 2.0F;
            Fonts.harmony.render(graphics.pose(), noneText, (double) noneTextX, (double) noneTextY, Color.WHITE, true, 0.30F);
        }


        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float textX = x + avatarSize + padding * 2;
        float textY = y + padding + 2;
        Fonts.harmony.render(graphics.pose(), "Name: " + targetName, (double) textX, (double) textY, Color.WHITE, true, 0.30F);

        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();
        float absorption = living.getAbsorptionAmount();

        HealthBarAnimator animator = healthAnimators.computeIfAbsent(living.getUUID(), k -> new HealthBarAnimator(health + absorption, 4.0F));
        animator.update(health + absorption);
        float animatedHealth = animator.getDisplayedHealth();

        String healthText = "HP: " + String.format("%.0f", animatedHealth) + " / " + String.format("%.0f", maxHealth);
        float healthTextY = (float) (textY + Fonts.harmony.getHeight(true, 0.30F) + 2.0F);
        Fonts.harmony.render(graphics.pose(), healthText, (double) textX, (double) healthTextY, Color.WHITE, true, 0.30F);


        float healthBarX = x + avatarSize + padding * 2;
        float healthBarY = y + hudHeight - padding - 8;
        float healthBarWidth = hudWidth - (healthBarX - x) - padding;
        float healthBarHeight = 6.0F;
        float cornerRadius = 4.0F;

        float healthRatio = animatedHealth / maxHealth;
        if (healthRatio > 1.0F) healthRatio = 1.0F;
        float currentHealthWidth = healthBarWidth * healthRatio;


        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);


        RenderUtils.drawRoundedRect(graphics.pose(), healthBarX, healthBarY, healthBarWidth, healthBarHeight, cornerRadius, 0x80404040);


        if (currentHealthWidth > 0) {

            float foregroundRadius = Math.min(cornerRadius, currentHealthWidth / 2);


            RenderUtils.drawRoundedRect(
                    graphics.pose(),
                    healthBarX,
                    healthBarY,
                    currentHealthWidth,
                    healthBarHeight,
                    foregroundRadius,
                    0xFF66CCFF
            );
        }
        return blurMatrix;
    }
}