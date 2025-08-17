package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

@ModuleInfo(
        name = "JumpReset",
        description = "Automatically jumps when hit by another player",
        category = Category.MOVEMENT
)
public class JumpReset extends Module {

    private long lastJumpTime = 0L;
    private static final long JUMP_COOLDOWN = 500L;

    private float lastHealth = -1.0F;

    @Override
    public void onEnable() {
        this.lastJumpTime = 0L;
        if (mc.player != null) {
            this.lastHealth = mc.player.getHealth();
        }
    }

    @Override
    public void onDisable() {
        this.lastHealth = -1.0F;
    }

    @EventTarget
    public void onEventRunTicks(EventRunTicks event) {
        if (event.getType() == EventType.PRE && mc.player != null) {
            float currentHealth = mc.player.getHealth();

            if (this.lastHealth != -1.0F && currentHealth < this.lastHealth) {
                DamageSource lastDamageSource = mc.player.getLastDamageSource();

                if (lastDamageSource != null && lastDamageSource.getEntity() instanceof Player && !lastDamageSource.getEntity().equals(mc.player)) {
                    if (mc.player.onGround() && System.currentTimeMillis() - this.lastJumpTime > JUMP_COOLDOWN) {
                        mc.player.jumpFromGround();
                        this.lastJumpTime = System.currentTimeMillis();
                    }
                }
            }

            this.lastHealth = currentHealth;
        }
    }
}