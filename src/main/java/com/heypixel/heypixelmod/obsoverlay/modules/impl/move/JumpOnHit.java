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
        name = "JumpOnHit",
        description = "Automatically jumps when hit by another player",
        category = Category.COMBAT
)
public class JumpOnHit extends Module {

    private long lastJumpTime = 0L;
    private static final long JUMP_COOLDOWN = 500L;

    // 存储玩家上一帧的生命值
    private float lastHealth = -1.0F;

    @Override
    public void onEnable() {
        this.lastJumpTime = 0L;
        // 确保玩家对象存在时再初始化生命值
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

            // 检查玩家生命值是否比上一帧低
            if (this.lastHealth != -1.0F && currentHealth < this.lastHealth) {
                // 如果生命值减少，立即检查攻击来源
                DamageSource lastDamageSource = mc.player.getLastDamageSource();

                // 确认攻击者是另一个玩家，并且不是玩家自己
                if (lastDamageSource != null && lastDamageSource.getEntity() instanceof Player && !lastDamageSource.getEntity().equals(mc.player)) {
                    // 只有在地面上且冷却已过才跳跃
                    if (mc.player.onGround() && System.currentTimeMillis() - this.lastJumpTime > JUMP_COOLDOWN) {
                        mc.player.jumpFromGround();
                        this.lastJumpTime = System.currentTimeMillis();
                    }
                }
            }

            // 在每一帧末尾更新生命值，为下一帧做准备
            this.lastHealth = currentHealth;
        }
    }
}