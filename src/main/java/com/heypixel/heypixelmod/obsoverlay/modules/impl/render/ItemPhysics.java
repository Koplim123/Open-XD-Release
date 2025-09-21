package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@ModuleInfo(
    name = "ItemPhysics",
    description = "让掉落物品拥有真实的物理效果",
    category = Category.RENDER
)
public class ItemPhysics extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final Map<Integer, ItemPhysicsData> physicsData = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    // 物理常量 - 经过优化的固定值
    private static final double GRAVITY = 0.04;
    private static final double BOUNCE_FACTOR = 0.3;
    private static final double AIR_RESISTANCE = 0.98;
    private static final double GROUND_FRICTION = 0.8;
    private static final double ROTATION_SPEED = 2.0;
    private static final int MAX_AGE = 6000; // 5分钟

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.level == null || mc.player == null) return;
        
        // 清理过期数据
        cleanupOldData();
        
        // 处理所有掉落物        
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity) || !entity.isAlive()) continue;
            
            ItemEntity itemEntity = (ItemEntity) entity;
            processItemPhysics(itemEntity);
        }
    }
    
    private void processItemPhysics(ItemEntity itemEntity) {
        int entityId = itemEntity.getId();
        ItemPhysicsData data = physicsData.get(entityId);
        if (data == null) {
            data = new ItemPhysicsData(itemEntity.position());
            physicsData.put(entityId, data);
        }
        
        // 应用重力和阻力
        applyPhysics(itemEntity, data);
        
        // 处理碰撞
        handleCollisions(itemEntity, data);
        
        // 更新旋转
        updateRotation(data);
        
        data.age++;
    }
    
    private void applyPhysics(ItemEntity entity, ItemPhysicsData data) {
        boolean onGround = entity.onGround();
        boolean inWater = entity.isInWater();
        
        if (onGround) {
            // 地面摩擦力
            data.velocity = data.velocity.multiply(GROUND_FRICTION, 1.0, GROUND_FRICTION);
            
            // 停止微小的移动
            if (data.velocity.horizontalDistance() < 0.01) {
                data.velocity = new Vec3(0, data.velocity.y, 0);
            }
        } else {
            // 应用重力
            double gravityForce = GRAVITY;
            if (inWater) {
                gravityForce *= 0.3; // 水中重力减小
            }
            data.velocity = data.velocity.add(0, -gravityForce, 0);
            
            // 空气阻力
            double resistance = inWater ? 0.6 : AIR_RESISTANCE;
            data.velocity = data.velocity.multiply(resistance, resistance, resistance);
        }
        
        // 限制最大速度
        double maxVelocity = 2.0;
        if (data.velocity.length() > maxVelocity) {
            data.velocity = data.velocity.normalize().scale(maxVelocity);
        }
        
        // 如果是新创建的物品，添加一些随机初始速度
        if (data.age == 0 && !data.hasInitialVelocity) {
            double randomX = (random.nextDouble() - 0.5) * 0.1;
            double randomZ = (random.nextDouble() - 0.5) * 0.1;
            data.velocity = data.velocity.add(randomX, 0, randomZ);
            data.hasInitialVelocity = true;
        }
    }
    
    private void handleCollisions(ItemEntity entity, ItemPhysicsData data) {
        Level level = entity.level();
        if (level == null) return;
        
        Vec3 currentPos = entity.position();
        Vec3 nextPos = currentPos.add(data.velocity);
        
        // 检查垂直碰撞
        BlockPos blockBelowPos = new BlockPos(Mth.floor(currentPos.x), Mth.floor(currentPos.y - 0.1), Mth.floor(currentPos.z));
        BlockState blockBelowState = level.getBlockState(blockBelowPos);
        
        if (!blockBelowState.isAir() && data.velocity.y < 0) {
            // 垂直反弹
            data.velocity = new Vec3(
                data.velocity.x, 
                Math.abs(data.velocity.y) * BOUNCE_FACTOR, 
                data.velocity.z
            );
        }
        
        // 检查水平碰撞
        BlockPos blockNextPos = new BlockPos(Mth.floor(nextPos.x), Mth.floor(nextPos.y), Mth.floor(nextPos.z));
        BlockState blockNextState = level.getBlockState(blockNextPos);
        
        if (!blockNextState.isAir()) {
            double bounceForce = BOUNCE_FACTOR * 0.5;
            
            // X轴碰撞
            if (Math.abs(data.velocity.x) > 0.01) {
                data.velocity = new Vec3(-data.velocity.x * bounceForce, data.velocity.y, data.velocity.z);
            }
            
            // Z轴碰撞
            if (Math.abs(data.velocity.z) > 0.01) {
                data.velocity = new Vec3(data.velocity.x, data.velocity.y, -data.velocity.z * bounceForce);
            }
        }
        
        // 水中特殊处理
        if (entity.isInWater()) {
            if (data.velocity.y < -0.1) {
                data.velocity = new Vec3(data.velocity.x, -0.1, data.velocity.z);
            }
        }
        
        // 应用位置更新
        Vec3 newPos = currentPos.add(data.velocity);
        entity.setPos(newPos.x, newPos.y, newPos.z);
    }
    
    private void updateRotation(ItemPhysicsData data) {
        float velocityMagnitude = (float) data.velocity.horizontalDistance();
        
        data.rotationX += velocityMagnitude * ROTATION_SPEED;
        data.rotationY += velocityMagnitude * ROTATION_SPEED * 0.7f;
        data.rotationZ += velocityMagnitude * ROTATION_SPEED * 0.5f;
        
        // 保持旋转角度在合理范围内
        data.rotationX = data.rotationX % 360;
        data.rotationY = data.rotationY % 360;
        data.rotationZ = data.rotationZ % 360;
    }

    // 删除了onRender方法，因为我们在handleCollisions中直接更新位置
    
    private void cleanupOldData() {
        physicsData.entrySet().removeIf(entry -> {
            ItemEntity entity = getEntityById(entry.getKey());
            return entity == null || !entity.isAlive() || entry.getValue().age > MAX_AGE;
        });
    }
    
    private ItemEntity getEntityById(int entityId) {
        if (mc.level == null) return null;
        
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity && entity.getId() == entityId) {
                return (ItemEntity) entity;
            }
        }
        return null;
    }
    
    @Override
    public void onEnable() {
        physicsData.clear();
    }
    
    @Override
    public void onDisable() {
        // 重置所有物品位置
        for (Map.Entry<Integer, ItemPhysicsData> entry : physicsData.entrySet()) {
            ItemEntity entity = getEntityById(entry.getKey());
            if (entity != null) {
                Vec3 originalPos = entry.getValue().originalPosition;
                entity.setPos(originalPos.x, originalPos.y, originalPos.z);
            }
        }
        physicsData.clear();
    }
    
    private static class ItemPhysicsData {
        Vec3 velocity;
        Vec3 originalPosition;
        int age;
        float rotationX, rotationY, rotationZ;
        boolean hasInitialVelocity;
        
        public ItemPhysicsData(Vec3 initialPosition) {
            this.velocity = Vec3.ZERO;
            this.originalPosition = initialPosition;
            this.age = 0;
            this.rotationX = 0;
            this.rotationY = 0;
            this.rotationZ = 0;
            this.hasInitialVelocity = false;
        }
    }
}