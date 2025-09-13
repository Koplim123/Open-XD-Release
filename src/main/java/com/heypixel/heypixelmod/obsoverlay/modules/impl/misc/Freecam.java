package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "Freecam",
        description = "Allows you to move your camera freely while your body stays in place",
        category = Category.MISC
)
public class Freecam extends Module {
    
    private final Minecraft mc = Minecraft.getInstance();
    private Vec3 cameraPosition = Vec3.ZERO;
    private float cameraYaw = 0.0f;
    private float cameraPitch = 0.0f;
    private Entity originalRiddenEntity = null;
    private CameraType originalCameraType = CameraType.FIRST_PERSON;
    
    public FloatValue speed = ValueBuilder.create(this, "Speed")
            .setDefaultFloatValue(2.0f)
            .setFloatStep(0.1f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(10.0f)
            .build()
            .getFloatValue();
            
    public BooleanValue showPlayer = ValueBuilder.create(this, "Show Player")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        
        // 保存原始位置和视角
        cameraPosition = mc.player.position();
        cameraYaw = mc.player.getYRot();
        cameraPitch = mc.player.getXRot();
        
        // 保存原始相机类型和骑乘实体
        originalCameraType = mc.options.getCameraType();
        originalRiddenEntity = mc.player.getVehicle();
        
        // 设置为第三人称视角以更好地看到效果
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }
    
    @Override
    public void onDisable() {
        if (mc.player == null) return;
        
        // 恢复原始相机类型
        mc.options.setCameraType(originalCameraType);
        
        // 恢复骑乘实体
        if (originalRiddenEntity != null) {
            mc.player.startRiding(originalRiddenEntity, true);
        }
        
        // 恢复玩家状态
        mc.player.setNoGravity(false);
        mc.player.setInvulnerable(false);
    }
    
    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE || mc.player == null) return;
        
        // 取消事件以防止正常移动
        event.setCancelled(true);
        
        // 更新玩家位置为相机位置（用于渲染）
        mc.player.setPos(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        mc.player.setYRot(cameraYaw);
        mc.player.setXRot(cameraPitch);
    }
    
    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null) return;
        
        // 处理相机移动
        handleCameraMovement();
        
        // 重置移动输入以防止正常移动
        event.setForward(0.0f);
        event.setStrafe(0.0f);
        event.setJump(false);
        event.setSneak(false);
    }
    
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        
        // 确保玩家不会受到伤害或其他影响
        mc.player.setNoGravity(true);
        mc.player.setInvulnerable(true);
        
        // 如果玩家在骑乘实体，取消骑乘
        if (mc.player.getVehicle() != null) {
            mc.player.stopRiding();
        }
    }
    
    @EventTarget
    public void onRender(EventRender event) {
        if (mc.player == null) return;
        
        // 在渲染时更新相机位置
        if (mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
            // 这里可以通过mixin来修改相机位置，但需要额外的mixin支持
        }
    }
    
    private void handleCameraMovement() {
        float moveSpeed = speed.getCurrentValue();
        
        // 处理前后移动
        if (mc.options.keyUp.isDown()) {
            float moveX = (float) (-Math.sin(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            float moveZ = (float) (Math.cos(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            cameraPosition = cameraPosition.add(moveX, 0, moveZ);
        }
        if (mc.options.keyDown.isDown()) {
            float moveX = (float) (Math.sin(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            float moveZ = (float) (-Math.cos(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            cameraPosition = cameraPosition.add(moveX, 0, moveZ);
        }
        
        // 处理左右移动
        if (mc.options.keyLeft.isDown()) {
            float moveX = (float) (-Math.cos(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            float moveZ = (float) (-Math.sin(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            cameraPosition = cameraPosition.add(moveX, 0, moveZ);
        }
        if (mc.options.keyRight.isDown()) {
            float moveX = (float) (Math.cos(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            float moveZ = (float) (Math.sin(Math.toRadians(cameraYaw)) * moveSpeed * 0.1);
            cameraPosition = cameraPosition.add(moveX, 0, moveZ);
        }
        
        // 处理上下移动
        if (mc.options.keyJump.isDown()) {
            cameraPosition = cameraPosition.add(0, moveSpeed * 0.1, 0);
        }
        if (mc.options.keyShift.isDown()) {
            cameraPosition = cameraPosition.add(0, -moveSpeed * 0.1, 0);
        }
        
        // 处理鼠标视角 - 使用更准确的方式
        if (mc.mouseHandler.isMouseGrabbed()) {
            float mouseX = (float) mc.mouseHandler.xpos() * 0.15f;
            float mouseY = (float) mc.mouseHandler.ypos() * 0.15f;
            
            cameraYaw += mouseX;
            cameraPitch -= mouseY;
            
            // 重置鼠标位置以防止无限旋转
            mc.mouseHandler.grabMouse();
        }
        
        // 限制俯仰角
        cameraPitch = Math.max(-90, Math.min(90, cameraPitch));
    }
}