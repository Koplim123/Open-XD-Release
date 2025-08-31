package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.events.BackTrackPacketEventManager;
import com.heypixel.heypixelmod.obsoverlay.events.api.events.JNICObf;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.IntValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
@JNICObf
@ModuleInfo(
        name = "BetaBackTrack",          // 模块名称
        description = "Advanced backtrack module based on LiquidBounce",  // 模块描述
        category = Category.COMBAT       // 模块分类为战斗
)
public class BetaBackTrack extends Module {
    
    // 配置选项 - 定义模块的各种可配置参数
    private final FloatValue range = ValueBuilder.create(this, "Range")  // 跟踪范围
            .setDefaultFloatValue(3.0f)  // 默认值3.0
            .setFloatStep(0.5f)          // 步进值0.5
            .setMinFloatValue(1.0f)      // 最小值1.0
            .setMaxFloatValue(10.0f)     // 最大值10.0
            .build()
            .getFloatValue();
            
    private final IntValue delay = ValueBuilder.create(this, "Delay")    // 延迟设置
            .setDefaultIntValue(100)     // 默认值100ms
            .setIntStep(10)              // 步进值10ms
            .setMinIntValue(0)           // 最小值0ms
            .setMaxIntValue(1000)        // 最大值1000ms
            .build()
            .getIntValue();
            
    private final IntValue nextBacktrackDelay = ValueBuilder.create(this, "NextBacktrackDelay")  // 下次回溯延迟
            .setDefaultIntValue(0)
            .setIntStep(10)
            .setMinIntValue(0)
            .setMaxIntValue(2000)
            .build()      // 默认值0ms
            .getIntValue();              // 步进值10ms
                       // 最小值0ms
    private final IntValue trackingBuffer = ValueBuilder.create(this, "TrackingBuffer")        // 最大值2000ms
            .setDefaultIntValue(500)
            .setIntStep(50)
            .setMinIntValue(0)
            .setMaxIntValue(2000)  // 跟踪缓冲区
            .build()    // 默认值500ms
            .getIntValue();              // 步进值50ms
                       // 最小值0ms
    private final FloatValue chance = ValueBuilder.create(this, "Chance")        // 最大值2000ms
            .setDefaultFloatValue(50.0f)
            .setFloatStep(5.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(100.0f)  // 触发几率
            .build() // 默认值50%
            .getFloatValue();          // 步进值5%
                  // 最小值0%
    private final BooleanValue pauseOnHurtTime = ValueBuilder.create(this, "PauseOnHurtTime")    // 最大值100%
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
              // 受伤时暂停
    private final IntValue hurtTime = ValueBuilder.create(this, "HurtTime")  // 默认不暂停
            .setVisibility(this.pauseOnHurtTime::getCurrentValue)
            .setDefaultIntValue(3)
            .setIntStep(1)
            .setMinIntValue(0)  // 受伤时间
            .setMaxIntValue(10)  // 仅在启用受伤暂停时可见
            .build()      // 默认值3
            .getIntValue();              // 步进值1
                      // 最小值0
    private final ModeValue targetMode = ValueBuilder.create(this, "TargetMode")         // 最大值10
            .setDefaultModeIndex(0)
            .setModes("Attack", "Range")
            .build()
            .getModeValue();  // 目标模式
                // 默认第一个模式
    private final IntValue lastAttackTimeToWork = ValueBuilder.create(this, "LastAttackTimeToWork") // 可选模式：攻击、范围
            .setDefaultIntValue(1000)
            .setIntStep(100)
            .setMinIntValue(0)
            .setMaxIntValue(5000)  // 攻击后生效时间
            .build()  // 默认值1000ms
            .getIntValue();           // 步进值100ms
                     // 最小值0ms
    private final ModeValue espMode = ValueBuilder.create(this, "EspMode")      // 最大值5000ms
            .setDefaultModeIndex(0)
            .setModes("Box", "Model", "Wireframe", "None")
            .build()
            .getModeValue();  // ESP模式（透视）
        // 默认第一个模式
    // 数据包管理器 // 可选模式：方框、模型、线框、无
    private final BackTrackPacketEventManager packetManager = BackTrackPacketEventManager.getInstance();
    
    // 状态变量
    private Entity target = null;
    private Vec3 targetPosition = null;
    private float currentChance = new Random().nextFloat() * 100;
    private int currentDelay = 100;
    private boolean shouldPause = false;              // 当前目标
    private long lastAttackTime = 0;        // 目标位置
    private long trackingBufferStartTime = 0;  // 当前触发几率
    private long chronometerStartTime = 0;            // 当前延迟
           // 是否应该暂停
    private final Minecraft mc = Minecraft.getInstance();           // 最后一次攻击时间
      // 跟踪缓冲区开始时间
    public BetaBackTrack() {     // 计时器开始时间
        currentDelay = getRandomDelay();
    }  // Minecraft实例
    
    private int getRandomDelay() {  // 构造函数
        return delay.getCurrentValue() + new Random().nextInt(delay.getCurrentValue() / 2);
    }
    
    @Override  // 获取随机延迟
    public void onEnable() {
        clear(false);
        packetManager.register();
    }
      // 模块启用时调用
    @Override
    public void onDisable() {
        clear(true);
        packetManager.unregister();
    }
      // 模块禁用时调用
    @EventTarget
    public void onPacket(EventPacket event) {
        // 数据包处理由BackTrackPacketEventManager负责
        if (isEnabled() && shouldCancelPackets()) {
            packetManager.setShouldCancelPackets(true);
            packetManager.setCurrentDelay(currentDelay);  // 数据包事件处理
        } else {
            packetManager.setShouldCancelPackets(false);
        }
    }
    
    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null) {
            clear(true);
            return;
        }  // 游戏tick事件处理
        
        // 更新目标
        updateTarget();
        
        // 设置包管理器状态
        if (shouldCancelPackets()) {
            packetManager.setShouldCancelPackets(true);
            packetManager.setCurrentDelay(currentDelay);
        } else {
            packetManager.setShouldCancelPackets(false);
        }
        
        // 更新延迟时间
        if (!packetManager.isProcessingPackets()) {
            currentDelay = getRandomDelay();
        }
        
        // 更新几率
        currentChance = new Random().nextFloat() * 100;
    }
    
    private void updateTarget() {
        // 查找最近的玩家作为目标
        target = null;
        targetPosition = null;
          // 更新目标
        if (mc.level != null && mc.player != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity livingEntity && 
                    livingEntity != mc.player && 
                    livingEntity.isAlive() &&
                    mc.player.distanceTo(livingEntity) <= range.getCurrentValue()) {
                    target = livingEntity;
                    targetPosition = livingEntity.position();
                    break;
                }
            }
        }
    }
    
    private boolean arePacketQueuesEmpty() {
        return !packetManager.isProcessingPackets();
    }
    
    public void clear(boolean handlePackets, boolean clearOnly, boolean resetChronometer) {  // 检查数据包队列是否为空
        if (handlePackets) {
            packetManager.clear();
        }
          // 清理方法
        if (target != null && resetChronometer) {
            chronometerStartTime = System.currentTimeMillis();
        }
        
        target = null;
        targetPosition = null;
    }
    
    public void clear(boolean handlePackets) {
        clear(handlePackets, false, true);
    }
    
    public void clear() {  // 清理方法重载
        clear(true, false, true);
    }
    
    private boolean shouldCancelPackets() {  // 清理方法重载
        return target != null && target.isAlive() && shouldBacktrack(target);
    }
    
    private boolean shouldBacktrack(Entity target) {  // 判断是否应该取消数据包
        boolean inRange = mc.player.distanceTo(target) <= range.getCurrentValue();
        
        if (inRange) {
            trackingBufferStartTime = System.currentTimeMillis();  // 判断是否应该回溯
        }
        
        return (inRange || !hasTrackingBufferElapsed()) &&
               shouldBeAttacked(target) &&
               mc.player.tickCount > 10 &&
               currentChance < chance.getCurrentValue() &&
               hasChronometerElapsed() &&
               !shouldPause() &&
               !hasAttackTimeElapsed();
    }
    
    private boolean hasTrackingBufferElapsed() {
        return System.currentTimeMillis() - trackingBufferStartTime > trackingBuffer.getCurrentValue();
    }
    
    private boolean hasChronometerElapsed() {  // 判断跟踪缓冲区是否已过期
        return System.currentTimeMillis() - chronometerStartTime >= nextBacktrackDelay.getCurrentValue();
    }
    
    private boolean hasAttackTimeElapsed() {  // 判断计时器是否已过期
        return System.currentTimeMillis() - lastAttackTime > lastAttackTimeToWork.getCurrentValue();
    }
    
    private boolean shouldPause() {  // 判断攻击时间是否已过期
        return pauseOnHurtTime.getCurrentValue() && shouldPause;
    }
    
    private boolean shouldBeAttacked(Entity entity) {  // 判断是否应该暂停
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player) return false;
        return entity.isAlive();
    }  // 判断是否应该攻击目标
    
    public boolean isLagging() {
        return isEnabled() && packetManager.isProcessingPackets();
    }
    
    // 获取包管理器实例  // 判断是否处于延迟状态
    public BackTrackPacketEventManager getPacketManager() {
        return packetManager;
    }
    
    // 获取当前目标
    public Entity getTarget() {
        return target;
    }
    
    // 获取目标位置
    public Vec3 getTargetPosition() {
        return targetPosition;
    }
    
    // 是否正在处理数据包
    public boolean isProcessingPackets() {
        return packetManager.isProcessingPackets();
    }
}