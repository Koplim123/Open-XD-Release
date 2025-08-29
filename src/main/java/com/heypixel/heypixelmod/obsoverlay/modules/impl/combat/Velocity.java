package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.StringValue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

@ModuleInfo(
        name = "Velocity",
        description = "It allow you to fuck YaoMao ass.(By BMWClient)",
        category = Category.COMBAT
)
public class Velocity extends Module {

    public static int ticksSinceVelocity = 0;
    
    private final FloatValue attackCountValue = new FloatValue(this, "AttackCount", 6, 0, 20, 1, null, null);
    private final BooleanValue jumpResetValue = new BooleanValue(this, "JumpReset", true, null, null);
    private final FloatValue chanceValue = new FloatValue(this, "Chance", 100, 0, 100, 1, null, null);
    private final BooleanValue debugValue = new BooleanValue(this, "Debug", false, null, null);

    // Cooldown settings
    private final BooleanValue cooldownEnabledValue = new BooleanValue(this, "Cooldown", true, null, null);
    private final FloatValue maxAttackCountValue = new FloatValue(this, "MaxAttackCount", 20, 0, 100, 1, null, null);
    private final FloatValue cooldownTicksValue = new FloatValue(this, "CooldownTicks", 10, 0, 100, 1, null, null);
    private final BooleanValue byHighCPSWarningValue = new BooleanValue(this, "ByHighCPSWarning", true, null, null);

    private boolean canReduce = false;
    private Entity target = null;
    private int totalAttackCount = 0;
    private int cooldownTicks = 0;
    private boolean jump = false;
    private final Random random = new Random();

    private void reset() {
        canReduce = false;
        target = null;
    }

    private Entity findTarget() {
        // Raytrace for entity
        Rotation rotation = new Rotation(mc.player.getYRot(), mc.player.getXRot());
        Vec3 lookVec = RotationUtils.getVectorForRotation(rotation);
        Vec3 startPos = mc.player.getEyePosition(1.0F);
        Vec3 endPos = startPos.add(lookVec.scale(6.0)); // Use default range

        // Simple entity detection in the direction the player is looking
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && entity != mc.player && !entity.isRemoved()) {
                Vec3 entityPos = entity.position();
                if (startPos.distanceTo(entityPos) < 6.0) {
                    return entity;
                }
            }
        }

        return null;
    }

    private void cooldown(String reason) {
        totalAttackCount = 0;
        cooldownTicks = (int) cooldownTicksValue.getCurrentValue();
        if (debugValue.getCurrentValue()) {
            System.out.println("[Velocity] Cooldown " + (int)cooldownTicksValue.getCurrentValue() + " ticks for " + reason);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (!isInCombat()) totalAttackCount = 0;

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (mc.player.hurtTime == 0) {
            reset();
        }

        if (canReduce) {
            if (target != null
                    && mc.player.isAlive()
                    && !mc.player.getAbilities().flying
                    && !mc.player.isInWater()
                    && !mc.player.isInLava()
                    && !mc.player.onClimbable()
                    && !mc.player.isPassenger()
            ) {
                mc.player.setDeltaMovement(
                        mc.player.getDeltaMovement().x * 0.07776,
                        mc.player.getDeltaMovement().y,
                        mc.player.getDeltaMovement().z * 0.07776
                );
            }
            reset();
        }
        
        // 更新ticksSinceVelocity
        ticksSinceVelocity++;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
                && packet.getId() == mc.player.getId()
                && random.nextInt(100) + 1 <= (int) chanceValue.getCurrentValue()
        ) {
            target = findTarget();
            if (target == null) return;

            if (cooldownTicks == 0) {
                boolean sprinting = mc.player.isSprinting();

                if (!sprinting) {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
                            mc.player.getYRot(),
                            mc.player.getXRot(),
                            mc.player.onGround()));
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(
                            mc.player,
                            ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                }

                if (cooldownEnabledValue.getCurrentValue() &&
                        totalAttackCount + (int)attackCountValue.getCurrentValue() > (int)maxAttackCountValue.getCurrentValue()) {
                    cooldown("max attack count");
                } else {
                    boolean attacked = true;

                    for (int i = 0; i < attackCountValue.getCurrentValue(); i++) {
                        // Simple attack without raytracing for now

                        mc.player.connection.send(ServerboundInteractPacket.createAttackPacket(target, false));
                        mc.player.connection.send(new ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND));
                        totalAttackCount++;
                    }

                    this.canReduce = attacked;
                }

                if (!sprinting) {
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(
                            mc.player,
                            ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                }
            }

            if (jumpResetValue.getCurrentValue()) jump = true;
            
            // 重置ticksSinceVelocity计数
            ticksSinceVelocity = 0;
        }

    }

    @EventTarget
    public void onMotionInput(EventMotion event) {
        if (jumpResetValue.getCurrentValue() && jump) {
            // Check if inventory is not open
            if (mc.screen == null) {
                event.setOnGround(false);
                jump = false;
            }
        }
    }

    @Override
    public void onEnable() {
        reset();
        totalAttackCount = 0;
        cooldownTicks = 0;
        jump = false;
        ticksSinceVelocity = 0;
    }

    private boolean isInCombat() {
        // Simple combat detection
        return mc.player.hurtTime > 0 || (target != null && mc.player.distanceTo(target) < 6.0f);
    }
}