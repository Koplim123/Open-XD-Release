package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "Freecam",
        description = "自由相机 - 让你的相机自由移动，身体保持在原地",
        category = Category.MISC
)
public class Freecam extends Module {
    
    private final Minecraft mc = Minecraft.getInstance();
    
    // 相机状态
    private Vec3 cameraPosition = Vec3.ZERO;
    private float cameraYaw = 0.0f;
    private float cameraPitch = 0.0f;
    private boolean isFreecamActive = false;
    
    // 玩家原始状态
    private Vec3 originalPlayerPosition = Vec3.ZERO;
    private float originalPlayerYaw = 0.0f;
    private float originalPlayerPitch = 0.0f;
    private boolean originalNoClip = false;
    private boolean originalInvulnerable = false;
    private Entity originalRidingEntity = null;
    private CameraType originalCameraType = CameraType.FIRST_PERSON;
    
    // 鼠标状态
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean firstMouse = true;
    
    // 设置项
    public FloatValue speed = ValueBuilder.create(this, "移动速度")
            .setDefaultFloatValue(1.0f)
            .setFloatStep(0.1f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(5.0f)
            .build()
            .getFloatValue();
            
    public BooleanValue noClip = ValueBuilder.create(this, "穿墙模式")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
            
    public FloatValue mouseSensitivity = ValueBuilder.create(this, "鼠标灵敏度")
            .setDefaultFloatValue(1.0f)
            .setFloatStep(0.1f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(3.0f)
            .build()
            .getFloatValue();
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        
        // 保存玩家原始状态
        saveOriginalPlayerState();
        
        // 初始化相机位置为玩家当前位置
        cameraPosition = mc.player.position();
        cameraYaw = mc.player.getYRot();
        cameraPitch = mc.player.getXRot();
        
        // 重置鼠标状态
        firstMouse = true;
        
        // 激活自由相机
        isFreecamActive = true;
        
        // 冻结玩家
        freezePlayer();
    }
    
    @Override
    public void onDisable() {
        if (mc.player == null) return;
        
        // 关闭自由相机
        isFreecamActive = false;
        
        // 恢复玩家原始状态
        restoreOriginalPlayerState();
    }
    
    @EventTarget
    public void onMotion(EventMotion event) {
        if (!isFreecamActive || event.getType() != EventType.PRE || mc.player == null) return;
        
        // 取消移动事件，保持玩家在原位
        event.setCancelled(true);
        
        // 保持玩家在原始位置
        event.setX(originalPlayerPosition.x);
        event.setY(originalPlayerPosition.y);
        event.setZ(originalPlayerPosition.z);
        event.setYaw(originalPlayerYaw);
        event.setPitch(originalPlayerPitch);
    }
    
    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (!isFreecamActive || mc.player == null) return;
        
        // 处理相机移动
        handleCameraMovement();
        
        // 取消所有移动输入
        event.setForward(0.0f);
        event.setStrafe(0.0f);
        event.setJump(false);
        event.setSneak(false);
    }
    
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!isFreecamActive || mc.player == null) return;
        
        // 处理鼠标视角
        handleMouseInput();
        
        // 确保玩家保持在原位并且受保护
        maintainPlayerState();
    }
    
    /**
     * 保存玩家原始状态
     */
    private void saveOriginalPlayerState() {
        if (mc.player == null) return;
        
        originalPlayerPosition = mc.player.position();
        originalPlayerYaw = mc.player.getYRot();
        originalPlayerPitch = mc.player.getXRot();
        originalNoClip = mc.player.noPhysics;
        originalInvulnerable = mc.player.isInvulnerable();
        originalRidingEntity = mc.player.getVehicle();
        originalCameraType = mc.options.getCameraType();
    }
    
    /**
     * 恢复玩家原始状态
     */
    private void restoreOriginalPlayerState() {
        if (mc.player == null) return;
        
        // 恢复位置和视角
        mc.player.setPos(originalPlayerPosition.x, originalPlayerPosition.y, originalPlayerPosition.z);
        mc.player.setYRot(originalPlayerYaw);
        mc.player.setXRot(originalPlayerPitch);
        
        // 恢复物理状态
        mc.player.noPhysics = originalNoClip;
        mc.player.setInvulnerable(originalInvulnerable);
        mc.player.setNoGravity(false);
        
        // 恢复骑乘状态
        if (originalRidingEntity != null && originalRidingEntity.isAlive()) {
            mc.player.startRiding(originalRidingEntity, true);
        }
        
        // 恢复相机类型
        mc.options.setCameraType(originalCameraType);
    }
    
    /**
     * 冻结玩家
     */
    private void freezePlayer() {
        if (mc.player == null) return;
        
        mc.player.setNoGravity(true);
        mc.player.setInvulnerable(true);
        
        if (noClip.getCurrentValue()) {
            mc.player.noPhysics = true;
        }
        
        // 如果在骑乘，先下车
        if (mc.player.getVehicle() != null) {
            mc.player.stopRiding();
        }
    }
    
    /**
     * 维护玩家状态
     */
    private void maintainPlayerState() {
        if (mc.player == null) return;
        
        // 确保玩家保持在原始位置
        if (mc.player.position().distanceTo(originalPlayerPosition) > 0.1) {
            mc.player.setPos(originalPlayerPosition.x, originalPlayerPosition.y, originalPlayerPosition.z);
        }
        
        // 维护保护状态
        mc.player.setNoGravity(true);
        mc.player.setInvulnerable(true);
        
        if (noClip.getCurrentValue()) {
            mc.player.noPhysics = true;
        }
        
        // 防止重新骑乘
        if (mc.player.getVehicle() != null) {
            mc.player.stopRiding();
        }
    }
    
    /**
     * 处理鼠标输入
     */
    private void handleMouseInput() {
        if (!mc.mouseHandler.isMouseGrabbed()) return;
        
        // 获取鼠标移动增量 (这需要通过mixin或反射来获取真正的鼠标增量)
        // 这里使用一个简化的方法，在实际使用中可能需要mixin支持
        float sensitivity = mouseSensitivity.getCurrentValue() * 0.6f;
        
        // 模拟鼠标增量获取 (实际实现可能需要mixin)
        double currentMouseX = mc.mouseHandler.xpos();
        double currentMouseY = mc.mouseHandler.ypos();
        
        if (!firstMouse) {
            double deltaX = (currentMouseX - lastMouseX) * sensitivity;
            double deltaY = (currentMouseY - lastMouseY) * sensitivity;
            
            cameraYaw += deltaX;
            cameraPitch -= deltaY;
            
            // 限制俯仰角度
            cameraPitch = Math.max(-90.0f, Math.min(90.0f, cameraPitch));
        } else {
            firstMouse = false;
        }
        
        lastMouseX = currentMouseX;
        lastMouseY = currentMouseY;
    }
    
    /**
     * 处理相机移动
     */
    private void handleCameraMovement() {
        float moveSpeed = speed.getCurrentValue() * 0.05f; // 调整移动速度倍数
        
        double motionX = 0;
        double motionY = 0;
        double motionZ = 0;
        
        // 前后移动
        if (mc.options.keyUp.isDown()) {
            motionX -= Math.sin(Math.toRadians(cameraYaw)) * moveSpeed;
            motionZ += Math.cos(Math.toRadians(cameraYaw)) * moveSpeed;
        }
        if (mc.options.keyDown.isDown()) {
            motionX += Math.sin(Math.toRadians(cameraYaw)) * moveSpeed;
            motionZ -= Math.cos(Math.toRadians(cameraYaw)) * moveSpeed;
        }
        
        // 左右移动
        if (mc.options.keyLeft.isDown()) {
            motionX -= Math.cos(Math.toRadians(cameraYaw)) * moveSpeed;
            motionZ -= Math.sin(Math.toRadians(cameraYaw)) * moveSpeed;
        }
        if (mc.options.keyRight.isDown()) {
            motionX += Math.cos(Math.toRadians(cameraYaw)) * moveSpeed;
            motionZ += Math.sin(Math.toRadians(cameraYaw)) * moveSpeed;
        }
        
        // 上下移动
        if (mc.options.keyJump.isDown()) {
            motionY += moveSpeed;
        }
        if (mc.options.keyShift.isDown()) {
            motionY -= moveSpeed;
        }
        
        // 更新相机位置
        cameraPosition = cameraPosition.add(motionX, motionY, motionZ);
    }
    
    // 获取相机状态的公共方法 (供mixin使用)
    public Vec3 getCameraPosition() {
        return isFreecamActive ? cameraPosition : null;
    }
    
    public float getCameraYaw() {
        return isFreecamActive ? cameraYaw : 0;
    }
    
    public float getCameraPitch() {
        return isFreecamActive ? cameraPitch : 0;
    }
    
    public boolean isFreecamActive() {
        return isFreecamActive;
    }
}