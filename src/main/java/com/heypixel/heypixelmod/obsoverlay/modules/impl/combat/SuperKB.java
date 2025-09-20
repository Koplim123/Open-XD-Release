package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.IntValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(
        name = "SuperKB",
        description = "A big Dog Shit Knockback module.",
        category = Category.COMBAT
)
public class SuperKB extends Module {
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Normal", "Packet", "SprintReset")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private final IntValue chance = ValueBuilder.create(this, "Chance")
            .setDefaultIntValue(100)
            .setMinIntValue(1)
            .setMaxIntValue(100)
            .build()
            .getIntValue();

    private final IntValue hurtTime = ValueBuilder.create(this, "HurtTime")
            .setDefaultIntValue(10)
            .setMinIntValue(0)
            .setMaxIntValue(10)
            .build()
            .getIntValue();

    private final BooleanValue onlyOnGround = ValueBuilder.create(this, "OnlyOnGround")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue onlyOnMove = ValueBuilder.create(this, "OnlyOnMove")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final IntValue delay = ValueBuilder.create(this, "Delay")
            .setDefaultIntValue(0)
            .setMinIntValue(0)
            .setMaxIntValue(5)
            .build()
            .getIntValue();

    private int ticksSinceAttack = 0;
    private boolean shouldAttack = false;

    @EventTarget
    public void onClick(EventClick event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        // 检查是否有有效的攻击目标
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            return;
        }

        Entity target = ((EntityHitResult) hitResult).getEntity();
        if (target == null || !(target instanceof LivingEntity)) {
            return;
        }

        if (onlyOnGround.getCurrentValue() && !mc.player.onGround()) {
            return;
        }

        if (onlyOnMove.getCurrentValue() && 
            mc.player.zza == 0.0F && 
            mc.player.xxa == 0.0F) {
            return;
        }

        LivingEntity livingTarget = (LivingEntity) target;
        if (livingTarget.hurtTime > hurtTime.getCurrentValue()) {
            return;
        }

        if (Math.random() * 100 > chance.getCurrentValue()) {
            return;
        }

        ticksSinceAttack = 0;
        shouldAttack = true;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!shouldAttack) {
            return;
        }

        if (ticksSinceAttack >= 0) {
            ticksSinceAttack++;

            if (ticksSinceAttack <= delay.getCurrentValue()) {
                return;
            }

            if (ticksSinceAttack > delay.getCurrentValue() + 1) {
                ticksSinceAttack = -1;
                shouldAttack = false;
                return;
            }
        } else {
            return;
        }

        switch (mode.getCurrentMode()) {
            case "Normal":
                doNormalKB();
                break;
            case "Packet":
                doPacketKB();
                break;
            case "SprintReset":
                doSprintResetKB();
                break;
        }
        
        shouldAttack = false;
    }

    private void doNormalKB() {
        if (mc.player == null) return;
        
        mc.player.setSprinting(false);
        mc.player.setSprinting(true);
    }

    private void doPacketKB() {
        if (mc.player == null || mc.getConnection() == null) return;
        
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
    }

    private void doSprintResetKB() {
        if (mc.player == null || mc.getConnection() == null) return;
        
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        
        mc.player.setSprinting(false);
        mc.player.setSprinting(true);
    }
}