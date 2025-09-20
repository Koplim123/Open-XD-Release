package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obfuscation.JNICObf;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

@JNICObf
@ModuleInfo(
        name = "NoXZ",
        description = "Reduces Knock Back.",
        category = Category.COMBAT
)
public class NoXZ extends Module {
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("NoXZ")
            .build()
            .getModeValue();

    private final ModeValue noXZMode = ValueBuilder.create(this, "NoXZ Mode")
            .setDefaultModeIndex(0)
            .setModes("OneTime", "PerTick")
            .setVisibility(() -> mode.isCurrentMode("NoXZ"))
            .build()
            .getModeValue();

    private final FloatValue attacks = ValueBuilder.create(this, "Attack Count")
            .setDefaultFloatValue(2.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.isCurrentMode("NoXZ"))
              .build()
              .getFloatValue();

    private final BooleanValue Logging = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue AirDelay = ValueBuilder.create(this, "AirDelay")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final FloatValue ReleaseMsWhenAttackOver = ValueBuilder.create(this, "ReleaseMsWhenAttackOver")
            .setDefaultFloatValue(1000.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5000.0F)
            .setFloatStep(100.0F)
            .setVisibility(() -> AirDelay.getCurrentValue())
            .build()
            .getFloatValue();

    private Entity targetEntity;
    private boolean velocityInput = false;
    private boolean attacked = false;

    private double currentKnockbackSpeed = 0.0;
    private int attackQueue = 0;
    private boolean receiveDamage = false;
    private boolean airDelayActive = false;
    private long airDelayStartTime = 0;
    private ClientboundSetEntityMotionPacket delayedVelocityPacket = null;

    @Override
    public void onDisable() {
        this.velocityInput = false;
        this.attacked = false;

        this.targetEntity = null;
        this.currentKnockbackSpeed = 0.0;
        this.attackQueue = 0;
        this.receiveDamage = false;
        this.airDelayActive = false;
        this.airDelayStartTime = 0;
        this.delayedVelocityPacket = null;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.level == null || mc.player == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundDamageEventPacket) {
            ClientboundDamageEventPacket damagePacket = (ClientboundDamageEventPacket)packet;
            if (damagePacket.entityId() == mc.player.getId()) {
                this.receiveDamage = true;
            }
        }

        if (packet instanceof ClientboundSetEntityMotionPacket) {
            ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket)packet;
            if (velocityPacket.getId() != mc.player.getId()) {
                return;
            }

            // AirDelay logic
            if (this.AirDelay.getCurrentValue() && !mc.player.onGround() && this.receiveDamage) {
                this.airDelayActive = true;
                this.airDelayStartTime = System.currentTimeMillis();
                this.delayedVelocityPacket = velocityPacket;
                event.setCancelled(true);
                
                if (this.Logging.getCurrentValue()) {
                    ChatUtils.addChatMessage("AirDelay activated - KB delayed until landing");
                }
                return;
            }

            this.velocityInput = true;
            this.targetEntity = Aura.target;

            if (this.mode.isCurrentMode("NoXZ")) {
                if (this.receiveDamage) {
                    this.receiveDamage = false;
                    this.attackQueue = (int)this.attacks.getCurrentValue();

                    if (this.Logging.getCurrentValue()) {
                        ChatUtils.addChatMessage("NoXZ Queue set: " + this.attackQueue + " attacks");
                    }
                }
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

        // AirDelay release logic
        if (this.airDelayActive && this.delayedVelocityPacket != null) {
            long currentTime = System.currentTimeMillis();
            boolean shouldRelease = false;
            
            // Release when on ground or after delay time
            if (mc.player.onGround()) {
                shouldRelease = true;
                if (this.Logging.getCurrentValue()) {
                    ChatUtils.addChatMessage("AirDelay released - player landed");
                }
            } else if (currentTime - this.airDelayStartTime >= this.ReleaseMsWhenAttackOver.getCurrentValue()) {
                shouldRelease = true;
                if (this.Logging.getCurrentValue()) {
                    ChatUtils.addChatMessage("AirDelay released - timeout after " + this.ReleaseMsWhenAttackOver.getCurrentValue() + "ms");
                }
            }
            
            if (shouldRelease) {
                // Apply the delayed velocity
                this.velocityInput = true;
                this.targetEntity = Aura.target;
                
                if (this.mode.isCurrentMode("NoXZ")) {
                    if (this.receiveDamage) {
                        this.receiveDamage = false;
                        this.attackQueue = (int)this.attacks.getCurrentValue();

                        if (this.Logging.getCurrentValue()) {
                            ChatUtils.addChatMessage("NoXZ Queue set: " + this.attackQueue + " attacks");
                        }
                    }
                }
                
                this.airDelayActive = false;
                this.airDelayStartTime = 0;
                this.delayedVelocityPacket = null;
            }
        }

        if (this.mode.isCurrentMode("NoXZ") && this.targetEntity != null && this.attackQueue > 0) {
            if (this.noXZMode.isCurrentMode("OneTime")) {
                for (; this.attackQueue >= 1; this.attackQueue--) {
                    mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.targetEntity, false));
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                    mc.player.setSprinting(false);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
                if (this.Logging.getCurrentValue()) {
                    ChatUtils.addChatMessage("NoXZ OneTime attacks executed");
                }
            } else if (this.noXZMode.isCurrentMode("PerTick")) {
                if (this.attackQueue >= 1) {
                    mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.targetEntity, false));
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                    mc.player.setSprinting(false);
                    mc.player.swing(InteractionHand.MAIN_HAND);

                    if (this.Logging.getCurrentValue()) {
                        ChatUtils.addChatMessage("NoXZ PerTick attack executed, remaining: " + (this.attackQueue - 1));
                    }
                }
                this.attackQueue--;
            }
        }
    }


}