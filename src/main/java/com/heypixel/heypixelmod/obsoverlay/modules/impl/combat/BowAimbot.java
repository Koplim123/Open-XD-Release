package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;

@ModuleInfo(
        name = "BowAimbot",
        description = "Automatically aims with a bow at an entity",
        category = Category.COMBAT
)
public class BowAimbot extends Module {

    // 配置选项
    public BooleanValue enabled = ValueBuilder.create(this, "Enabled").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue onlyWhenUsingBow = ValueBuilder.create(this, "Only When Using Bow").setDefaultBooleanValue(true).build().getBooleanValue();
    public FloatValue fov = ValueBuilder.create(this, "FOV").setDefaultFloatValue(90.0F).setMinFloatValue(1.0F).setMaxFloatValue(360.0F).setFloatStep(1.0F).build().getFloatValue();
    public FloatValue aimRange = ValueBuilder.create(this, "Aim Range").setDefaultFloatValue(50.0F).setMinFloatValue(1.0F).setMaxFloatValue(100.0F).setFloatStep(1.0F).build().getFloatValue();
    public FloatValue yawSpeed = ValueBuilder.create(this, "Yaw Speed").setDefaultFloatValue(10.0F).setMinFloatValue(1.0F).setMaxFloatValue(180.0F).setFloatStep(1.0F).build().getFloatValue();
    public FloatValue pitchSpeed = ValueBuilder.create(this, "Pitch Speed").setDefaultFloatValue(10.0F).setMinFloatValue(1.0F).setMaxFloatValue(180.0F).setFloatStep(1.0F).build().getFloatValue();

    private LivingEntity target = null;
    private Vector2f currentRotations = new Vector2f(0.0F, 0.0F);

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            currentRotations.set(mc.player.getYRot(), mc.player.getXRot());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
    }

    @EventTarget
    public void onUpdate(EventRunTicks e) {
        if (e.getType() != EventType.PRE || mc.player == null) {
            return;
        }

        if (onlyWhenUsingBow.getCurrentValue() && (!(mc.player.getInventory().getSelected().getItem() instanceof BowItem) || !mc.player.isUsingItem())) {
            target = null;
            return;
        }

        this.target = findTarget();
        if (this.target != null) {
            Vector2f rotations = getRotationsToTarget(this.target);
            updateRotations(rotations);
            RotationManager.rotations.set(currentRotations);
        }
    }

    /**
     * Finds the closest valid target for the aimbot.
     *
     * @return The closest LivingEntity, or null if no valid target is found.
     */
    private LivingEntity findTarget() {
        if (mc.level == null || mc.player == null) {
            return null;
        }

        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                if (isValidTarget(livingEntity)) {
                    potentialTargets.add(livingEntity);
                }
            }
        }

        potentialTargets.sort(Comparator.comparingDouble(mc.player::distanceTo));
        return potentialTargets.isEmpty() ? null : potentialTargets.get(0);
    }

    /**
     * Checks if the entity is a valid target based on configured settings.
     *
     * @param entity The entity to check.
     * @return True if the entity is a valid target, false otherwise.
     */
    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive() || entity.isDeadOrDying()) return false;
        if (entity.distanceTo(mc.player) > aimRange.getCurrentValue()) return false;

        // 使用射线追踪来代替 hasLineOfSightTo，兼容性更好
        if (!hasLineOfSightTo(entity)) return false;

        float yawDifference = Math.abs(RotationUtils.getAngleDifference(RotationUtils.getRotations(entity).getX(), mc.player.getYRot()));
        if (yawDifference > fov.getCurrentValue() / 2) return false;

        return true;
    }

    /**
     * Uses ray tracing to check for obstacles between the player and the entity.
     *
     * @param entity The target entity.
     * @return True if there are no obstacles, false otherwise.
     */
    private boolean hasLineOfSightTo(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        Vec3 playerEyePos = mc.player.getEyePosition();
        Vec3 targetEyePos = entity.getEyePosition();

        return mc.level.clip(new ClipContext(
                playerEyePos,
                targetEyePos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        )).getType() == HitResult.Type.MISS;
    }

    /**
     * Updates player rotations with linear interpolation.
     *
     * @param targetRotations The target yaw and pitch.
     */
    private void updateRotations(Vector2f targetRotations) {
        float targetYaw = RotationUtils.normalizeAngle(targetRotations.x);
        float targetPitch = RotationUtils.normalizeAngle(targetRotations.y);

        float deltaYaw = RotationUtils.normalizeAngle(targetYaw - currentRotations.x);
        float deltaPitch = RotationUtils.normalizeAngle(targetPitch - currentRotations.y);

        float yawStep = Math.min(yawSpeed.getCurrentValue(), Math.abs(deltaYaw));
        float pitchStep = Math.min(pitchSpeed.getCurrentValue(), Math.abs(deltaPitch));

        float newYaw = this.currentRotations.x + yawStep * Math.signum(deltaYaw);
        float newPitch = this.currentRotations.y + pitchStep * Math.signum(deltaPitch);

        this.currentRotations.set(newYaw, newPitch);
    }

    /**
     * Calculates the rotations needed to aim at the target.
     *
     * @param target The LivingEntity to aim at.
     * @return A Vector2f containing the required yaw and pitch.
     */
    private Vector2f getRotationsToTarget(LivingEntity target) {
        if (target == null) {
            return new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        }

        double x = target.getX() - mc.player.getX();
        double y = target.getEyeY() - mc.player.getEyeY();
        double z = target.getZ() - mc.player.getZ();

        double horizontalDistance = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, horizontalDistance));

        return new Vector2f(yaw, pitch);
    }
}