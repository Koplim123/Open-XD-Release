package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
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
        name = "FreeCam",
        description = "Allows you to move out of your body",
        category = Category.MISC
)
public class Freecam extends Module {

    private final FloatValue speed = ValueBuilder.create(this, "Speed")
            .setDefaultFloatValue(1.0f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(2.0f)
            .setFloatStep(0.1f)
            .build()
            .getFloatValue();

    private final BooleanValue allowCameraInteract = ValueBuilder.create(this, "AllowCameraInteract")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue lookAt = ValueBuilder.create(this, "LookAt")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private Vec3 cameraPos;
    private Vec3 lastCameraPos;
    private float originalYaw;
    private float originalPitch;

    @Override
    public void onEnable() {
        LocalPlayer player = mc.player;
        if (player != null) {
            cameraPos = player.getEyePosition();
            lastCameraPos = cameraPos;
            originalYaw = player.getYRot();
            originalPitch = player.getXRot();
        }
    }

    @Override
    public void onDisable() {
        cameraPos = null;
        lastCameraPos = null;
        
        // Reset player rotation
        LocalPlayer player = mc.player;
        if (player != null) {
            player.setYRot(originalYaw);
            player.setXRot(originalPitch);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) return;
        
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Cancel player movement
        event.setX(player.getX());
        event.setY(player.getY());
        event.setZ(player.getZ());
        event.setYaw(originalYaw);
        event.setPitch(originalPitch);
        event.setOnGround(true);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Update camera position based on input
        updateCameraPosition();
    }

    @EventTarget
    public void onRender(EventRender event) {
        // Camera position is handled by the mixin
    }
    
    public Vec3 getCameraPos() {
        return cameraPos;
    }
    
    public Vec3 getLastCameraPos() {
        return lastCameraPos;
    }

    private void updateCameraPosition() {
        LocalPlayer player = mc.player;
        if (player == null || cameraPos == null) return;

        lastCameraPos = cameraPos;
        Vec3 movement = getMovementVector();
        cameraPos = cameraPos.add(movement);
    }

    private Vec3 getMovementVector() {
        LocalPlayer player = mc.player;
        if (player == null) return Vec3.ZERO;

        float yaw = player.getYRot();
        double forward = 0;
        double strafe = 0;
        double vertical = 0;

        // Handle keyboard input
        if (mc.options.keyUp.isDown()) {
            forward--;
        }
        if (mc.options.keyDown.isDown()) {
            forward++;
        }
        if (mc.options.keyLeft.isDown()) {
            strafe--;
        }
        if (mc.options.keyRight.isDown()) {
            strafe++;
        }
        if (mc.options.keyJump.isDown()) {
            vertical++;
        }
        if (mc.options.keyShift.isDown()) {
            vertical--;
        }

        double speedValue = speed.getCurrentValue();

        // Calculate movement vector
        double x = 0;
        double y = 0;
        double z = 0;

        if (forward != 0 || strafe != 0) {
            // Horizontal movement
            double diagonal = Math.sqrt(forward * forward + strafe * strafe);
            forward /= diagonal;
            strafe /= diagonal;

            double cosYaw = Math.cos(Math.toRadians(yaw + 90));
            double sinYaw = Math.sin(Math.toRadians(yaw + 90));

            x = (forward * cosYaw + strafe * sinYaw) * speedValue;
            z = (forward * sinYaw - strafe * cosYaw) * speedValue;
        }

        y = vertical * speedValue;

        return new Vec3(x, y, z);
    }

    public boolean shouldRenderPlayerFromAllPerspectives(LivingEntity entity) {
        if (!isEnabled() || entity != mc.player) {
            return entity.isSleeping();
        }

        return entity.isSleeping() || mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK;
    }

    public Vec3 modifyRaycast(Vec3 original, Entity entity, float tickDelta) {
        if (!isEnabled() || entity != mc.player || !allowCameraInteract.getCurrentValue()) {
            return original;
        }

        if (cameraPos != null && lastCameraPos != null) {
            // Interpolate camera position
            Vec3 interpolated = lastCameraPos.lerp(cameraPos, tickDelta);
            return interpolated;
        }
        
        return original;
    }
}