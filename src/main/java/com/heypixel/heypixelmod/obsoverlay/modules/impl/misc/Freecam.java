package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "Freecam",
        description = "Simple freecam with through walls",
        category = Category.MISC
)
public class Freecam extends Module {
    
    private final Minecraft mc = Minecraft.getInstance();
    
    // 玩家原始状态
    private boolean originalNoClip = false;
    private boolean originalInvulnerable = false;
    private Entity originalRidingEntity = null;
    
    @Override
    public void onEnable() {
        var player = mc.player;
        if (player == null) return;
        
        // 保存玩家原始状态
        saveOriginalPlayerState();
        
        // 启用穿墙和无敌
        player.noPhysics = true;
        player.setInvulnerable(true);
        player.setNoGravity(true);
        
        // 下车
        if (player.getVehicle() != null) {
            player.stopRiding();
        }
    }
    
    @Override
    public void onDisable() {
        if (mc.player == null) return;
        
        // 恢复玩家原始状态
        restoreOriginalPlayerState();
    }
    
    
    /**
     * 保存玩家原始状态
     */
    private void saveOriginalPlayerState() {
        var player = mc.player;
        if (player == null) return;
        
        originalNoClip = player.noPhysics;
        originalInvulnerable = player.isInvulnerable();
        originalRidingEntity = player.getVehicle();
    }
    
    /**
     * 恢复玩家原始状态
     */
    private void restoreOriginalPlayerState() {
        var player = mc.player;
        if (player == null) return;
        
        // 恢复物理状态
        player.noPhysics = originalNoClip;
        player.setInvulnerable(originalInvulnerable);
        player.setNoGravity(false);
        
        // 恢复骑乘状态
        if (originalRidingEntity != null && originalRidingEntity.isAlive()) {
            player.startRiding(originalRidingEntity, true);
        }
    }
    public boolean isFreecamActive() {
        return this.isEnabled();
    }
    public Vec3 getCameraPosition() {
        return null;
    }

    public float getCameraYaw() {
        return mc.player != null ? mc.player.getYRot() : 0.0f;
    }

    public float getCameraPitch() {
        return mc.player != null ? mc.player.getXRot() : 0.0f;
    }
}