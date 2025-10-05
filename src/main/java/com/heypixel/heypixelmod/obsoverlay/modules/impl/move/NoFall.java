package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@ModuleInfo(
        name = "NoFall",
        category = Category.MOVEMENT,
        description = "Prevent fall damage"
)
public class NoFall extends Module {
    FloatValue distance = ValueBuilder.create(this, "Fall Distance").setDefaultFloatValue(3.0F).setFloatStep(0.1F).setMinFloatValue(3.0F).setMaxFloatValue(15.0F).build().getFloatValue();
    BooleanValue liquidCheck = ValueBuilder.create(this, "Liquid Check").setDefaultBooleanValue(true).build().getBooleanValue();
    public double preFallDistance;
    private boolean receivedServerLagback = false;
    public static boolean isSpoofing = false;
    private boolean sentSpoofPacket = false;
    public static boolean forceJump = false;
    private int delayTicks = 0;

    public NoFall() {
    }

    public void onEnable() {
        this.reset();
    }

    public void onDisable() {
        this.reset();
    }

    private void reset() {
        this.receivedServerLagback = false;
        isSpoofing = false;
        this.sentSpoofPacket = false;
        forceJump = false;
        this.delayTicks = 0;
    }

    private boolean shouldBlockJump() {
        return isSpoofing || forceJump;
    }

    private boolean isPlayerInLiquid() {
        if (mc.player == null) return false;
        return mc.player.isInWater() || mc.player.isInLava();
    }

    private boolean shouldSkipNoFall() {
        return liquidCheck.getCurrentValue() && isPlayerInLiquid();
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.POST && mc.player != null) {
            if (shouldSkipNoFall()) {
                return;
            }

            if (delayTicks > 0) {
                delayTicks--;
            }

            if (this.shouldBlockJump()) {
                mc.options.keyJump.setDown(false);
            }

            if (mc.player.onGround()) {
                this.preFallDistance = 0.0;
            } else {
                this.preFallDistance = mc.player.fallDistance;
            }

            if (this.receivedServerLagback && isSpoofing) {
                forceJump = true;
                isSpoofing = false;
                this.receivedServerLagback = false;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(EventUpdate event) {
        if (shouldSkipNoFall()) {
            return;
        }
        
        if (this.shouldBlockJump() && mc.options != null) {
            mc.options.keyJump.setDown(false);
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (shouldSkipNoFall()) {
            return;
        }
        
        if (mc.player.onGround() && forceJump) {
            forceJump = false;
            delayTicks = 2;
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (shouldSkipNoFall()) {
            return;
        }
        
        if (this.shouldBlockJump()) {
            event.setJump(false);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.POST) {
            if (shouldSkipNoFall()) {
                return;
            }
            
            if (!isSpoofing && mc.player.fallDistance > this.distance.getCurrentValue() && !event.isOnGround()) {
                isSpoofing = true;
                this.receivedServerLagback = false;
                this.sentSpoofPacket = false;
            }

            if (isSpoofing && mc.player.fallDistance < 3.0F) {
                event.setOnGround(false);
                if (!this.sentSpoofPacket) {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(event.getX() - 1000.0, event.getY(), event.getZ(), false));
                    this.sentSpoofPacket = true;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (shouldSkipNoFall()) {
            return;
        }
        
        if (event.getType() == EventType.SEND) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket && ((isSpoofing && this.sentSpoofPacket && !this.receivedServerLagback) || delayTicks > 0)) {
                event.setCancelled(true);
            }
        } else if (isSpoofing && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.receivedServerLagback = true;
        }
    }
}