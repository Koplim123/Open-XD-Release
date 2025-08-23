package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;

import java.util.Random;

@ModuleInfo(
        name = "BetaSuperKB",
        description = "Increases knockback dealt to other entities",
        category = Category.COMBAT
)
public class BetaSuperKB extends Module {

    private ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("Packet", "SprintTap", "WTap")
            .build()
            .getModeValue();

    private FloatValue hurtTime = ValueBuilder.create(this, "HurtTime")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    private FloatValue chance = ValueBuilder.create(this, "Chance")
            .setDefaultFloatValue(100.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(100.0F)
            .build()
            .getFloatValue();

    private BooleanValue onlyOnMove = ValueBuilder.create(this, "OnlyOnMove")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private BooleanValue onlyForward = ValueBuilder.create(this, "OnlyForward")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private BooleanValue notInWater = ValueBuilder.create(this, "NotInWater")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private BooleanValue onlyOnGround = ValueBuilder.create(this, "OnlyOnGround")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private BooleanValue onlyFacing = ValueBuilder.create(this, "OnlyFacing")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    // For SprintTap mode
    private boolean cancelSprint = false;
    private int sprintCooldown = 0;

    // For WTap mode
    private boolean inSequence = false;
    private boolean cancelMovement = false;
    private int sequenceTicks = 0;

    private Random random = new Random();

    @Override
    public void onEnable() {
        cancelSprint = false;
        inSequence = false;
        cancelMovement = false;
        sequenceTicks = 0;
        sprintCooldown = 0;
    }

    @Override
    public void onDisable() {
        cancelSprint = false;
        inSequence = false;
        cancelMovement = false;
    }

    // Since EventAttackEntity doesn't exist, we'll need to find another way to trigger the knockback
    // For now, we'll comment out this method and you'll need to implement the attack trigger differently
    /*
    @EventTarget
    public void onAttack(EventAttackEntity event) {
        if (event.isCancelled()) return;

        Entity target = event.getTarget();
        if (target == null || !shouldOperate(target)) return;

        if (target instanceof LivingEntity) {
            LivingEntity enemy = (LivingEntity) target;

            if (enemy.hurtTime <= hurtTime.getCurrentValue() &&
                    random.nextInt(100) < chance.getCurrentValue()) {

                String currentMode = mode.getCurrentMode();
                if ("Packet".equals(currentMode)) {
                    handlePacketMode();
                } else if ("SprintTap".equals(currentMode)) {
                    handleSprintTapMode();
                } else if ("WTap".equals(currentMode)) {
                    handleWTapMode();
                }
            }
        }
    }
    */

    @EventTarget
    public void onMotion(EventMotion event) {
        // Handle SprintTap mode cooldown
        if (sprintCooldown > 0) {
            sprintCooldown--;
            if (sprintCooldown <= 0) {
                cancelSprint = false;
                if (mc.player != null) {
                    mc.player.setSprinting(true);
                }
            }
        }

        // Handle WTap mode sequence
        if (inSequence) {
            sequenceTicks++;

            if (sequenceTicks >= 2) { // Default 1-2 ticks
                cancelMovement = false;
                inSequence = false;
                sequenceTicks = 0;

                if (mc.player != null) {
                    mc.player.setSprinting(true);
                }
            }
        }

        // Apply movement cancellation for WTap mode
        if (cancelMovement && mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    private void handlePacketMode() {
        if (mc.player == null || mc.getConnection() == null) return;

        if (mc.player.isSprinting()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(
                    mc.player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY
            ));
        }

        // Send sprint packets to increase knockback
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING
        ));
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY
        ));
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING
        ));

        // Set client-side sprinting state
        mc.player.setSprinting(true);
    }

    private void handleSprintTapMode() {
        if (cancelSprint || mc.player == null) return;

        cancelSprint = true;
        sprintCooldown = 2; // 2 tick cooldown
        mc.player.setSprinting(false);
    }

    private void handleWTapMode() {
        if (inSequence || mc.player == null) return;

        inSequence = true;
        sequenceTicks = 0;
        cancelMovement = true;
        mc.player.setSprinting(false);
    }

    private boolean shouldOperate(Entity target) {
        if (mc.player == null) return false;

        // Check movement conditions
        if (onlyOnMove.getCurrentValue()) {
            boolean isMoving = mc.player.xxa != 0.0F || mc.player.zza != 0.0F;
            boolean isMovingSideways = mc.player.xxa != 0.0F;

            if (!isMoving || (onlyForward.getCurrentValue() && isMovingSideways)) {
                return false;
            }
        }

        // Check environmental conditions
        if (notInWater.getCurrentValue() && mc.player.isInWater()) {
            return false;
        }

        if (onlyOnGround.getCurrentValue() && !mc.player.onGround()) {
            return false;
        }

        if (onlyFacing.getCurrentValue()) {
            // Simple facing check - target should be in front of player
            double dot = mc.player.getLookAngle().dot(
                    target.position().subtract(mc.player.position()).normalize()
            );
            if (dot < 0) {
                return false;
            }
        }

        return true;
    }

    public String getMode() {
        return mode.getCurrentMode();
    }

    public boolean isActive() {
        return isEnabled() && (cancelSprint || inSequence);
    }
}