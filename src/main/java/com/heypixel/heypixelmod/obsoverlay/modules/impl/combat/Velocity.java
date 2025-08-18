package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

@ModuleInfo(
        name = "Velocity",
        description = "Reduces Knock Back.",
        category = Category.MOVEMENT
)
public class Velocity extends Module {
   private final BooleanValue NoXZ = ValueBuilder.create(this, "NoXZ")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();
   private final FloatValue attacks = ValueBuilder.create(this, "NoXZ Count")
           .setDefaultFloatValue(4.0F)
           .setMinFloatValue(4.0F)
           .setMaxFloatValue(11.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   private final BooleanValue jumpReset = ValueBuilder.create(this, "Jump Reset")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   private final FloatValue jumpTick = ValueBuilder.create(this, "JumpResetTick")
           .setDefaultFloatValue(0.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(9.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();

   private final BooleanValue debugMessageValue = ValueBuilder.create(this, "Debug Message")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private final FloatValue fovLimitValue = ValueBuilder.create(this, "FOV Limit")
           .setDefaultFloatValue(45.0F)
           .setMinFloatValue(30.0F)  // 最小值设为30
           .setMaxFloatValue(180.0F) // 最大值设为180
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();

   private final FloatValue speedThresholdValue = ValueBuilder.create(this, "Speed Threshold")
           .setDefaultFloatValue(0.60F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(1.0F)
           .setFloatStep(0.01F)
           .build()
           .getFloatValue();

   private Entity targetEntity;
   private boolean velocityInput = false;
   private boolean attacked = false;
   private int jumpResetTicks = 0;
   private double currentKnockbackSpeed = 0.0;

   @Override
   public void onDisable() {
      this.velocityInput = false;
      this.attacked = false;
      this.jumpResetTicks = 0;
      this.targetEntity = null;
      this.currentKnockbackSpeed = 0.0;
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (mc.level == null || mc.player == null) return;

      Packet<?> packet = event.getPacket();
      if (packet instanceof ClientboundSetEntityMotionPacket) {
         ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket)packet;
         if (velocityPacket.getId() != mc.player.getId()) {
            return;
         }

         double x = (double)velocityPacket.getXa() / 8000.0;
         double z = (double)velocityPacket.getZa() / 8000.0;
         double speed = Math.sqrt(x * x + z * z);
         this.currentKnockbackSpeed = speed;

         float currentSpeedThreshold = speedThresholdValue.getCurrentValue();
         if (speed < currentSpeedThreshold) {
            if (this.debugMessageValue.getCurrentValue()) {
               ChatUtils.addChatMessage("Knockback too weak: " + String.format("%.2f", speed) + " < " + String.format("%.2f", currentSpeedThreshold));
            }
            return;
         }

         this.velocityInput = true;
         this.targetEntity = Aura.target;

         boolean inFOV = false;
         if (this.targetEntity != null) {
            Vec3 playerLookVec = mc.player.getLookAngle();

            Vec3 toTargetVec = new Vec3(
                    targetEntity.getX() - mc.player.getX(),
                    0,
                    targetEntity.getZ() - mc.player.getZ()
            ).normalize();

            double dot = playerLookVec.x * toTargetVec.x + playerLookVec.z * toTargetVec.z;

            double angleRad = Math.acos(Mth.clamp(dot, -1.0, 1.0));
            double angleDeg = Math.toDegrees(angleRad);

            float currentFovLimit = fovLimitValue.getCurrentValue();
            inFOV = angleDeg <= currentFovLimit;

            if (this.debugMessageValue.getCurrentValue()) {
               ChatUtils.addChatMessage("FOV Check: " + String.format("%.1f°", angleDeg) +
                       (inFOV ? " <= " + String.format("%.1f°", currentFovLimit) : " > " + String.format("%.1f°", currentFovLimit)));
            }
         }

         if (this.NoXZ.getCurrentValue() && this.targetEntity != null && inFOV) {
            boolean wasSprintingBefore = mc.player.isSprinting();

            if (!wasSprintingBefore) {
               mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround()));
               mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_SPRINTING));
            }

            int attackCount = (int)this.attacks.getCurrentValue();
            for (int i = -1; i < attackCount; i++) {
               if (mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
                  mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.targetEntity, false));
                  mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
               }
            }

            this.attacked = true;

            if (!wasSprintingBefore) {
               mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
            }

            if (this.debugMessageValue.getCurrentValue()) {
               ChatUtils.addChatMessage("Reduce: " + String.format("%.2f", speed));
            }
         }

         if (this.jumpReset.getCurrentValue()) {
            this.jumpResetTicks = (int)this.jumpTick.getCurrentValue();
         }
      }
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (mc.player == null) return;

      if (mc.player.hurtTime == 0) {
         this.velocityInput = false;
         this.currentKnockbackSpeed = 0.0;
      }

      if (this.jumpResetTicks > 0) {
         this.jumpResetTicks--;
      }

      if (this.velocityInput && this.attacked) {
         if (this.targetEntity != null && mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
            mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x * 0.07776,
                    mc.player.getDeltaMovement().y,
                    mc.player.getDeltaMovement().z * 0.07776
            );

            if (this.debugMessageValue.getCurrentValue()) {
               ChatUtils.addChatMessage("Applied velocity reduction");
            }
         }
         this.attacked = false;
      }
   }

   @EventTarget
   public void onMoveInput(EventMoveInput event) {
      if (mc.player != null && this.jumpReset.getCurrentValue() && mc.player.onGround() && this.jumpResetTicks == 1) {
         event.setJump(true);
         this.jumpResetTicks = 0;
         if (this.debugMessageValue.getCurrentValue()) {
            ChatUtils.addChatMessage("Jump reset activated");
         }
      }
   }
}