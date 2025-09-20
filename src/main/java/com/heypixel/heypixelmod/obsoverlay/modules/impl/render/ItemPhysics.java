package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.IntValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
    name = "ItemPhysics",
    description = "Advanced physics simulation for dropped items with realistic motion",
    category = Category.RENDER
)
public class ItemPhysics extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final Map<Integer, ItemPhysicsData> physicsData = new ConcurrentHashMap<>();
    
    // Main toggle
    private final BooleanValue enabled = ValueBuilder.create(this, "Enabled")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    
    // Physics modes
    private final ModeValue physicsMode = ValueBuilder.create(this, "Physics Mode")
            .setModes("Realistic", "Simple", "Bouncy", "Floating")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();
            
    // Gravity settings
    private final FloatValue gravity = ValueBuilder.create(this, "Gravity")
            .setDefaultFloatValue(0.04f)
            .setMinFloatValue(0.001f)
            .setMaxFloatValue(0.5f)
            .setFloatStep(0.001f)
            .build()
            .getFloatValue();
            
    // Air resistance
    private final FloatValue airResistance = ValueBuilder.create(this, "Air Resistance")
            .setDefaultFloatValue(0.98f)
            .setMinFloatValue(0.5f)
            .setMaxFloatValue(0.999f)
            .setFloatStep(0.001f)
            .build()
            .getFloatValue();
            
    // Ground friction
    private final FloatValue groundFriction = ValueBuilder.create(this, "Ground Friction")
            .setDefaultFloatValue(0.8f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(0.99f)
            .setFloatStep(0.01f)
            .build()
            .getFloatValue();
            
    // Bounce factor
    private final FloatValue bounciness = ValueBuilder.create(this, "Bounciness")
            .setDefaultFloatValue(0.4f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(1.5f)
            .setFloatStep(0.05f)
            .build()
            .getFloatValue();
    
    // Rotation settings
    private final BooleanValue enableRotation = ValueBuilder.create(this, "Enable Rotation")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
            
    private final FloatValue rotationSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(2.0f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(10.0f)
            .setFloatStep(0.1f)
            .build()
            .getFloatValue();
    
    // Water physics
    private final BooleanValue waterPhysics = ValueBuilder.create(this, "Water Physics")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
            
    private final FloatValue waterDrag = ValueBuilder.create(this, "Water Drag")
            .setDefaultFloatValue(0.6f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(0.95f)
            .setFloatStep(0.05f)
            .build()
            .getFloatValue();
    
    // Performance settings
    private final IntValue maxItems = ValueBuilder.create(this, "Max Items")
            .setDefaultIntValue(500)
            .setMinIntValue(50)
            .setMaxIntValue(2000)
            .setIntStep(50)
            .build()
            .getIntValue();
            
    private final IntValue maxAge = ValueBuilder.create(this, "Max Age (ticks)")
            .setDefaultIntValue(6000)
            .setMinIntValue(1000)
            .setMaxIntValue(20000)
            .setIntStep(500)
            .build()
            .getIntValue();
    
    // Wind effect
    private final BooleanValue windEffect = ValueBuilder.create(this, "Wind Effect")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
            
    private final FloatValue windStrength = ValueBuilder.create(this, "Wind Strength")
            .setDefaultFloatValue(0.02f)
            .setMinFloatValue(0.001f)
            .setMaxFloatValue(0.1f)
            .setFloatStep(0.001f)
            .build()
            .getFloatValue();

    public void onTick() {
        if (!enabled.getCurrentValue() || mc.level == null) return;
        
        cleanupOldData();
        
        if (physicsData.size() >= maxItems.getCurrentValue()) {
            // Remove oldest entries when limit is reached
            physicsData.entrySet().removeIf(entry -> 
                entry.getValue().age > maxAge.getCurrentValue() * 0.8f);
        }
        
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity) || !entity.isAlive()) continue;
            
            ItemEntity itemEntity = (ItemEntity) entity;
            processItemPhysics(itemEntity);
        }
    }
    
    private void processItemPhysics(ItemEntity itemEntity) {
        int entityId = itemEntity.getId();
        ItemPhysicsData data = physicsData.computeIfAbsent(entityId, 
            id -> new ItemPhysicsData(itemEntity.position()));
        
        String mode = physicsMode.getCurrentMode();
        
        switch (mode) {
            case "Realistic":
                applyRealisticPhysics(itemEntity, data);
                break;
            case "Simple":
                applySimplePhysics(itemEntity, data);
                break;
            case "Bouncy":
                applyBouncyPhysics(itemEntity, data);
                break;
            case "Floating":
                applyFloatingPhysics(itemEntity, data);
                break;
        }
        
        // Apply collision detection
        handleCollisions(itemEntity, data);
        
        // Apply rotation
        if (enableRotation.getCurrentValue()) {
            updateRotation(itemEntity, data);
        }
        
        // Apply wind effect
        if (windEffect.getCurrentValue()) {
            applyWindEffect(data);
        }
        
        data.age++;
    }
    
    private void applyRealisticPhysics(ItemEntity entity, ItemPhysicsData data) {
        boolean onGround = entity.onGround();
        boolean inWater = entity.isInWater() && waterPhysics.getCurrentValue();
        
        if (onGround) {
            // Ground friction
            data.velocity = data.velocity.multiply(groundFriction.getCurrentValue(), 1.0, groundFriction.getCurrentValue());
            
            // Stop very slow movement
            if (data.velocity.horizontalDistance() < 0.01) {
                data.velocity = new Vec3(0, data.velocity.y, 0);
            }
        } else {
            // Apply gravity
            double gravityForce = gravity.getCurrentValue();
            if (inWater) {
                gravityForce *= 0.3; // Reduced gravity in water
            }
            data.velocity = data.velocity.add(0, -gravityForce, 0);
            
            // Air resistance
            double resistance = inWater ? waterDrag.getCurrentValue() : airResistance.getCurrentValue();
            data.velocity = data.velocity.multiply(resistance, resistance, resistance);
        }
        
        // Terminal velocity
        double maxVelocity = 2.0;
        if (data.velocity.length() > maxVelocity) {
            data.velocity = data.velocity.normalize().scale(maxVelocity);
        }
    }
    
    private void applySimplePhysics(ItemEntity entity, ItemPhysicsData data) {
        if (entity.onGround()) {
            data.velocity = data.velocity.multiply(0.7, 1.0, 0.7);
        } else {
            data.velocity = data.velocity.add(0, -gravity.getCurrentValue() * 0.5, 0);
        }
    }
    
    private void applyBouncyPhysics(ItemEntity entity, ItemPhysicsData data) {
        if (!entity.onGround()) {
            data.velocity = data.velocity.add(0, -gravity.getCurrentValue() * 0.8, 0);
        }
        
        // Extra bouncy behavior will be handled in collision detection
    }
    
    private void applyFloatingPhysics(ItemEntity entity, ItemPhysicsData data) {
        // Floating motion with gentle oscillation
        double time = (System.currentTimeMillis() % 10000) / 1000.0;
        double floatOffset = Math.sin(time + entity.getId()) * 0.05;
        
        Vec3 targetPos = data.originalPosition.add(0, floatOffset + 0.5, 0);
        Vec3 currentPos = entity.position();
        
        data.velocity = targetPos.subtract(currentPos).scale(0.1);
    }
    
    private void handleCollisions(ItemEntity entity, ItemPhysicsData data) {
        Level level = entity.level();
        Vec3 nextPos = entity.position().add(data.velocity);
        
        BlockPos blockPos = new BlockPos(Mth.floor(nextPos.x), Mth.floor(nextPos.y), Mth.floor(nextPos.z));
        BlockState blockState = level.getBlockState(blockPos);
        
        if (!blockState.isAir()) {
            VoxelShape shape = blockState.getCollisionShape(level, blockPos);
            
            if (!shape.isEmpty()) {
                // Simple collision response - bounce
                double bounceForce = bounciness.getCurrentValue();
                
                if ("Bouncy".equals(physicsMode.getCurrentMode())) {
                    bounceForce *= 1.5; // Extra bouncy
                }
                
                // Check which axis hit the block
                if (data.velocity.y < 0 && nextPos.y <= blockPos.getY() + 1) {
                    data.velocity = new Vec3(data.velocity.x, Math.abs(data.velocity.y) * bounceForce, data.velocity.z);
                }
                
                if (Math.abs(data.velocity.x) > 0.01) {
                    data.velocity = new Vec3(-data.velocity.x * bounceForce * 0.5, data.velocity.y, data.velocity.z);
                }
                
                if (Math.abs(data.velocity.z) > 0.01) {
                    data.velocity = new Vec3(data.velocity.x, data.velocity.y, -data.velocity.z * bounceForce * 0.5);
                }
            }
        }
        
        // Water collision
        if (entity.isInWater() && waterPhysics.getCurrentValue()) {
            if (data.velocity.y < -0.1) {
                data.velocity = new Vec3(data.velocity.x, -0.1, data.velocity.z);
            }
        }
    }
    
    private void updateRotation(ItemEntity entity, ItemPhysicsData data) {
        float speed = rotationSpeed.getCurrentValue();
        float velocityMagnitude = (float) data.velocity.horizontalDistance();
        
        data.rotationX += velocityMagnitude * speed;
        data.rotationY += velocityMagnitude * speed * 0.7f;
        data.rotationZ += velocityMagnitude * speed * 0.5f;
        
        // Keep rotations in reasonable bounds
        data.rotationX = data.rotationX % 360;
        data.rotationY = data.rotationY % 360;
        data.rotationZ = data.rotationZ % 360;
    }
    
    private void applyWindEffect(ItemPhysicsData data) {
        double time = System.currentTimeMillis() / 1000.0;
        double windX = Math.sin(time * 0.5) * windStrength.getCurrentValue();
        double windZ = Math.cos(time * 0.3) * windStrength.getCurrentValue();
        
        data.velocity = data.velocity.add(windX, 0, windZ);
    }

    public void onRender(float partialTicks) {
        if (!enabled.getCurrentValue()) return;
        
        for (Map.Entry<Integer, ItemPhysicsData> entry : physicsData.entrySet()) {
            ItemEntity entity = getEntityById(entry.getKey());
            if (entity != null && entity.isAlive()) {
                ItemPhysicsData data = entry.getValue();
                
                // Smooth interpolated position
                Vec3 currentPos = entity.position();
                Vec3 newPos = currentPos.add(data.velocity.scale(partialTicks));
                
                // Update entity position
                entity.setPos(newPos.x, newPos.y, newPos.z);
                
                // Update stored position for next frame
                data.lastPosition = newPos;
            }
        }
    }
    
    private void cleanupOldData() {
        physicsData.entrySet().removeIf(entry -> {
            ItemEntity entity = getEntityById(entry.getKey());
            return entity == null || !entity.isAlive() || 
                   entry.getValue().age > maxAge.getCurrentValue();
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
        // Reset all item positions to their original state
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
        Vec3 lastPosition;
        int age;
        float rotationX, rotationY, rotationZ;
        
        public ItemPhysicsData(Vec3 initialPosition) {
            this.velocity = Vec3.ZERO;
            this.originalPosition = initialPosition;
            this.lastPosition = initialPosition;
            this.age = 0;
            this.rotationX = 0;
            this.rotationY = 0;
            this.rotationZ = 0;
        }
    }
}