package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(
        name = "LegitKillAura",
        description = "From Old Loratadine's Killaura",
        category = Category.COMBAT
)
public class LegitKillAura extends Module {
    private final FloatValue cpsValue = ValueBuilder.create(this, "CPS")
            .setDefaultFloatValue(11.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    private final FloatValue rangeValue = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(2.65F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    private final FloatValue smoothnessValue = ValueBuilder.create(this, "Smoothness")
            .setDefaultFloatValue(20.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    private final BooleanValue playersValue = ValueBuilder.create(this, "Players")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue mobsValue = ValueBuilder.create(this, "Mobs")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue animalsValue = ValueBuilder.create(this, "Animals")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue deadValue = ValueBuilder.create(this, "Dead")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue invisibleValue = ValueBuilder.create(this, "Invisible")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private static LivingEntity target;
    private final List<LivingEntity> targets = new ArrayList<>();
    private long lastAttackTime = 0;
    private float[] rotations = new float[]{0.0F, 0.0F};

    @Override
    public void onEnable() {
        if (mc == null || mc.player == null) {
            return;
        }

        this.targets.clear();
        target = null;
        rotations = new float[]{mc.player.getYRot(), mc.player.getXRot()};
    }

    @Override
    public void onDisable() {
        this.targets.clear();
        target = null;
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                if (this.filter(livingEntity)) {
                    targets.add(livingEntity);
                }
            }
        }

        targets.sort(new Comparator<LivingEntity>() {
            public int compare(LivingEntity e1, LivingEntity e2) {
                return (int) (getDistanceToEntityBox(e1) - getDistanceToEntityBox(e2));
            }
        });

        target = null;
        if (!targets.isEmpty()) {
            target = targets.get(0);
        }

        if (target != null) {
            float[] targetRotations = getSimpleRotations(target);

            rotations[0] = RotationUtils.updateRotation(rotations[0], targetRotations[0], smoothnessValue.getCurrentValue());
            rotations[1] = RotationUtils.updateRotation(rotations[1], targetRotations[1], smoothnessValue.getCurrentValue());

            mc.player.setYRot(rotations[0]);
            mc.player.setXRot(rotations[1]);
        }
    }

    @EventTarget
    public void onMotionUpdate(EventRunTicks event) {
        if (mc == null || mc.level == null || mc.player == null || target == null) {
            return;
        }

        if (mc.player.connection == null) {
            return;
        }

        if (target != null && canAttack()) {
            mc.player.connection.send(ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            lastAttackTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc == null || mc.level == null || mc.player == null || target == null) {
            return;
        }

        if (target != null) {
            renderTargetLabel(event.getStack(), target);
        }
    }

    private void renderTargetLabel(PoseStack stack, LivingEntity entity) {
        Vec3 entityPos = entity.position();
        entityPos = entityPos.add(0, entity.getBoundingBox().getYsize() + 0.5, 0);

        Vector2f screenPos = ProjectionUtils.project(entityPos.x, entityPos.y, entityPos.z, 1.0F);
        if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {
            String text = "AuraTarget";
            float x = screenPos.x - Fonts.harmony.getWidth(text, 0.5) / 2;
            float y = screenPos.y;

            Fonts.harmony.render(stack, text, x, y, Color.RED, true, 0.5);
        }
    }

    private boolean canAttack() {
        if (mc == null || mc.player == null) {
            return false;
        }

        long delay = (long) (1000 / cpsValue.getCurrentValue());
        return (System.currentTimeMillis() - lastAttackTime) >= delay;
    }

    public boolean filter(LivingEntity entity) {
        if (mc == null || mc.player == null) {
            return false;
        }

        if (getDistanceToEntityBox(entity) > rangeValue.getCurrentValue() || !isSelected(entity)) {
            return false;
        } else {
            return mc.player.hasLineOfSight(entity) && !entity.isDeadOrDying() && !(entity.getHealth() <= 0.0F);
        }
    }

    public boolean isSelected(LivingEntity entity) {
        if (mc == null || mc.player == null) {
            return false;
        }

        boolean isTargetType = (entity instanceof Player && playersValue.getCurrentValue()) ||
                (entity instanceof Monster && mobsValue.getCurrentValue()) ||
                (entity instanceof Animal && animalsValue.getCurrentValue());


        boolean isDead = entity.isDeadOrDying() || entity.getHealth() <= 0.0F;
        boolean isInvisible = !entity.isInvisible() || invisibleValue.getCurrentValue();
        boolean isActuallyDead = (!isDead || deadValue.getCurrentValue());

        return isTargetType && isActuallyDead && isInvisible && entity != mc.player;
    }

    public double getDistanceToEntityBox(LivingEntity entity) {
        if (mc == null || mc.player == null) {
            return Double.MAX_VALUE;
        }

        Vec3 playerPos = mc.player.position().add(0, mc.player.getEyeHeight(), 0);
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);
        return playerPos.distanceTo(entityPos);
    }

    public float[] getSimpleRotations(LivingEntity entity) {
        if (mc == null || mc.player == null) {
            return new float[]{0.0F, 0.0F};
        }

        Vec3 playerPos = mc.player.position().add(0, mc.player.getEyeHeight(), 0);
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);

        double d0 = entityPos.x - playerPos.x;
        double d1 = entityPos.y - playerPos.y;
        double d2 = entityPos.z - playerPos.z;

        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        float yaw = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(d1, d3) * 180.0D / Math.PI);

        return new float[]{yaw, pitch};
    }

    public static LivingEntity getTarget() {
        return target;
    }

    public List<LivingEntity> getTargets() {
        return this.targets;
    }
}