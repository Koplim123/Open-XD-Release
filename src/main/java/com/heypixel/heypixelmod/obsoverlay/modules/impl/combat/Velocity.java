package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@ModuleInfo(
        name = "Velocity",
        description = "Grim Reduce",
        category = Category.COMBAT
)
public class Velocity extends Module {

    private final FloatValue attackTimes = ValueBuilder.create(this, "AttackTimes")
            .setDefaultFloatValue(4.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();

    private final FloatValue chance = ValueBuilder.create(this, "Chance")
            .setDefaultFloatValue(50.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(100.0F)
            .build()
            .getFloatValue();

    private boolean velocityInput = false;
    private boolean attacked = false;
    private Entity target = null;

    @Override
    public void onEnable() {
        velocityInput = false;
        attacked = false;
        target = null;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (mc.player.hurtTime == 0) {
            velocityInput = false;
            attacked = false;
        }

        if (velocityInput && attacked) {
            if (canApplyVelocity()) {
                applyVelocityReduction();
            }
            attacked = false;
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.level == null) return;

        Packet<?> packet = event.getPacket();
        target = getKillAuraTarget();

        if (isVelocityPacket(packet) && shouldProcessVelocity()) {
            processVelocityPacket();
        }
    }
    private Entity getKillAuraTarget() {
        return null;
    }
    
    private boolean isInFluid(Player player) {
        return player.isInWater() || player.isInLava();
    }
    
    private int random(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }
    
    private boolean canApplyVelocity() {
        return target != null
                && mc.player.isAlive()
                && !mc.player.isSpectator()
                && !mc.player.getAbilities().flying
                && !isInFluid(mc.player)
                && !mc.player.onClimbable()
                && !mc.player.isOnFire()
                && !mc.player.isUsingItem();
    }
    
    private void applyVelocityReduction() {
        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x * 0.07776,
                mc.player.getDeltaMovement().y,
                mc.player.getDeltaMovement().z * 0.07776
        );
    }
    
    private boolean isVelocityPacket(Packet<?> packet) {
        return packet instanceof ClientboundSetEntityMotionPacket velocityPacket
                && velocityPacket.getId() == mc.player.getId();
    }
    
    private boolean shouldProcessVelocity() {
        return random(1, 100) <= (int)chance.getCurrentValue();
    }
    
    private void processVelocityPacket() {
        velocityInput = true;
        boolean wasSprinting = mc.player.isSprinting();
        
        if (!wasSprinting) {
            startSprinting();
        }
        
        performAttackSequence();
        attacked = true;
        
        if (!wasSprinting) {
            stopSprinting();
        }
    }
    
    private void startSprinting() {
        mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround()));
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_SPRINTING));

    }
    
    private void stopSprinting() {
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
    }
    
    private void performAttackSequence() {
        for (int i = 0; i < (int)attackTimes.getCurrentValue(); i++) {
            if (target != null) {
                mc.player.connection.send(ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
            }
            mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }
}