package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
        name = "KillAura",
        description = "Automatically attacks entities",
        category = Category.COMBAT
)
public class Aura extends Module {
    private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
    private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
    public static Entity target;
    public static Entity aimingTarget;
    public static List<Entity> targets = new ArrayList<>();
    public static Vector2f rotation;
    BooleanValue targetHud = ValueBuilder.create(this, "Target HUD").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue preferBaby = ValueBuilder.create(this, "Prefer Baby").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue moreParticles = ValueBuilder.create(this, "More Particles").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue keepSprint = ValueBuilder.create(this, "KeepSprint").setDefaultBooleanValue(true).build().getBooleanValue();
    // 新增转头模式，包含 None, Linear, Sigmoid 和 Accelerated
    ModeValue rotationType = ValueBuilder.create(this, "Rotations Type").setModes("None", "Linear", "Sigmoid", "Accelerated").build().getModeValue();
    // 新增真人转头速度控制，最大值改为180
    FloatValue turnSpeedX = ValueBuilder.create(this, "Turn Speed X")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(180.0F)
            .setVisibility(() -> !this.rotationType.isCurrentMode("None"))
            .build()
            .getFloatValue();
    FloatValue turnSpeedY = ValueBuilder.create(this, "Turn Speed Y")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(180.0F)
            .setVisibility(() -> !this.rotationType.isCurrentMode("None"))
            .build()
            .getFloatValue();
    // 新增瞄准才攻击开关
    BooleanValue aimOnlyAttack = ValueBuilder.create(this, "Aim Only Attack").setDefaultBooleanValue(false).build().getBooleanValue();
    // 移除抖动控制
    FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();
    FloatValue aps = ValueBuilder.create(this, "Attack Per Second")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> !this.infSwitch.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    FloatValue fov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(360.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(10.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();
    FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();
    // 加速度参数只在Accelerated模式下可见
    FloatValue acceleration = ValueBuilder.create(this, "Acceleration")
            .setDefaultFloatValue(20.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(100.0F)
            .setVisibility(() -> this.rotationType.isCurrentMode("Accelerated"))
            .build()
            .getFloatValue();

    // 新增平滑度参数
    FloatValue sigmoidSmoothness = ValueBuilder.create(this, "Sigmoid Smoothness")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setVisibility(() -> this.rotationType.isCurrentMode("Sigmoid"))
            .build()
            .getFloatValue();

    RotationUtils.Data lastRotationData;
    RotationUtils.Data rotationData;
    int attackTimes = 0;
    float attacks = 0.0F;
    private int index;
    private Vector4f blurMatrix;
    private Vector2f currentRotation = new Vector2f(0.0F, 0.0F);
    private Vector2f currentSpeed = new Vector2f(0.0F, 0.0F);

    @EventTarget
    public void onShader(EventShader e) {
        if (this.blurMatrix != null && this.targetHud.getCurrentValue()) {
            RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrix = null;

        // 绘制目标HUD
        if (target instanceof LivingEntity && this.targetHud.getCurrentValue()) {
            LivingEntity living = (LivingEntity) target;
            e.getStack().pushPose();
            float x = (float) mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
            float y = (float) mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
            String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
            float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
            this.blurMatrix = new Vector4f(x, y, width, 30.0F);
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, width, 30.0F, 5.0F, HUD.headerColor);
            StencilUtils.erase(true);
            RenderUtils.fillBound(e.getStack(), x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F, HUD.headerColor);
            StencilUtils.dispose();
            Fonts.harmony.render(e.getStack(), targetName, (double) (x + 5.0F), (double) (y + 6.0F), Color.WHITE, true, 0.35F);
            Fonts.harmony
                    .render(
                            e.getStack(),
                            "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""),
                            (double) (x + 5.0F),
                            (double) (y + 17.0F),
                            Color.WHITE,
                            true,
                            0.35F
                    );
            e.getStack().popPose();
        }

        // 绘制瞄准点
        if (!this.rotationType.isCurrentMode("None") && aimingTarget != null) {
            e.getStack().pushPose();
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderUtils.drawCircle(
                    (float) mc.getWindow().getGuiScaledWidth() / 2.0F,
                    (float) mc.getWindow().getGuiScaledHeight() / 2.0F,
                    2.0F,
                    10,
                    new Color(255, 0, 0, 255).getRGB()
            );
            RenderSystem.disableBlend();
            e.getStack().popPose();
        }
    }

    @EventTarget
    public void onRender(EventRender e) {
        if (this.targetEsp.getCurrentValue()) {
            PoseStack stack = e.getPMatrixStack();
            float partialTicks = e.getRenderPartialTicks();
            stack.pushPose();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderUtils.applyRegionalRenderOffset(stack);

            for (Entity entity : targets) {
                if (entity instanceof LivingEntity living) {
                    float[] color = target == living ? targetColorRed : targetColorGreen;
                    stack.pushPose();
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                    double motionX = entity.getX() - entity.xo;
                    double motionY = entity.getY() - entity.yo;
                    double motionZ = entity.getZ() - entity.zo;
                    AABB boundingBox = entity.getBoundingBox()
                            .move(-motionX, -motionY, -motionZ)
                            .move((double) partialTicks * motionX, (double) partialTicks * motionY, (double) partialTicks * motionZ);
                    RenderUtils.drawSolidBox(boundingBox, stack);
                    stack.popPose();
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            stack.popPose();
        }
    }

    @Override
    public void onEnable() {
        rotation = null;
        this.index = 0;
        target = null;
        aimingTarget = null;
        targets.clear();
        // 初始化当前旋转和速度，避免残留值
        this.currentRotation.x = mc.player.getYRot();
        this.currentRotation.y = mc.player.getXRot();
        this.currentSpeed.x = 0.0F;
        this.currentSpeed.y = 0.0F;
    }

    @Override
    public void onDisable() {
        target = null;
        aimingTarget = null;
        // 重置旋转和速度
        this.currentRotation.x = 0.0F;
        this.currentRotation.y = 0.0F;
        this.currentSpeed.x = 0.0F;
        this.currentSpeed.y = 0.0F;
        super.onDisable();
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        target = null;
        aimingTarget = null;
        this.toggle();
    }

    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown e) {
        // 确保 KeepSprint 能正确控制
        if (this.keepSprint.getCurrentValue()) {
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        if (event.getType() == EventType.PRE && mc.player != null) {
            if (mc.screen instanceof AbstractContainerScreen
                    || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
                    || InventoryUtils.shouldDisableFeatures()) {
                target = null;
                aimingTarget = null;
                this.rotationData = null;
                rotation = null;
                this.lastRotationData = null;
                targets.clear();
                // 禁用时重置状态
                this.currentRotation.x = mc.player.getYRot();
                this.currentRotation.y = mc.player.getXRot();
                this.currentSpeed.x = 0.0F;
                this.currentSpeed.y = 0.0F;
                return;
            }

            boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
            this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
            this.updateAttackTargets();

            // 如果已经有目标，并且它仍然有效，就保持锁定
            if (target != null && isValidTarget(target)) {
                aimingTarget = target;
            } else {
                // 否则，选择一个新的目标
                aimingTarget = this.shouldPreAim();
            }

            this.lastRotationData = this.rotationData;
            this.rotationData = null;

            if (aimingTarget != null) {
                this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
                if (this.rotationData.getRotation() != null) {
                    if (this.rotationType.isCurrentMode("Linear")) {
                        this.updateLinearRotations(this.rotationData);
                    } else if (this.rotationType.isCurrentMode("Sigmoid")) {
                        this.updateSigmoidRotations(this.rotationData);
                    } else if (this.rotationType.isCurrentMode("Accelerated")) {
                        this.updateAcceleratedRotations(this.rotationData);
                    } else {
                        // None模式下，不进行任何旋转
                        rotation = null;
                        RotationManager.rotations.x = mc.player.getYRot();
                        RotationManager.rotations.y = mc.player.getXRot();
                        this.currentSpeed.x = 0.0F;
                        this.currentSpeed.y = 0.0F;
                    }
                } else {
                    rotation = null;
                }
            } else {
                rotation = null;
                this.currentSpeed.x = 0.0F;
                this.currentSpeed.y = 0.0F;
            }

            if (targets.isEmpty()) {
                target = null;
                return;
            }

            if (this.index > targets.size() - 1) {
                this.index = 0;
            }

            if (targets.size() > 1
                    && ((float) this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
                this.attackTimes = 0;

                for (int i = 0; i < targets.size(); i++) {
                    this.index++;
                    if (this.index > targets.size() - 1) {
                        this.index = 0;
                    }

                    Entity nextTarget = targets.get(this.index);
                    RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
                    if (data.getDistance() < 3.0) {
                        break;
                    }
                }
            }

            if (this.index > targets.size() - 1 || !isSwitch) {
                this.index = 0;
            }

            target = targets.get(this.index);
            this.attacks = this.attacks + this.aps.getCurrentValue() / 20.0F;
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        if (mc.player.getUseItem().isEmpty()
                && mc.screen == null
                && Naven.skipTasks.isEmpty()
                && !NetworkUtils.isServerLag()
                && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
            while (this.attacks >= 1.0F) {
                this.doAttack();
                this.attacks--;
            }
        }
    }

    public Entity shouldPreAim() {
        Entity target = Aura.target;
        if (target == null) {
            List<Entity> aimTargets = this.getTargets();
            if (!aimTargets.isEmpty()) {
                target = aimTargets.get(0);
            }
        }

        return target;
    }

    public void doAttack() {
        if (!targets.isEmpty()) {
            HitResult hitResult = mc.hitResult;

            // 如果开启了 "Aim Only Attack"，则检查是否实际瞄准了目标
            if (this.aimOnlyAttack.getCurrentValue()) {
                if (hitResult.getType() != Type.ENTITY || ((EntityHitResult) hitResult).getEntity() != aimingTarget) {
                    return; // 如果没有瞄准到aimingTarget，则不攻击
                }
            }

            if (hitResult.getType() == Type.ENTITY) {
                EntityHitResult result = (EntityHitResult) hitResult;
                if (AntiBots.isBot(result.getEntity())) {
                    ChatUtils.addChatMessage("Attacking Bot!");
                    return;
                }
            }

            if (this.multi.getCurrentValue()) {
                int attacked = 0;

                for (Entity entity : targets) {
                    if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                        this.attackEntity(entity);
                        if (++attacked >= 2) {
                            break;
                        }
                    }
                }
            } else if (hitResult.getType() == Type.ENTITY) {
                EntityHitResult result = (EntityHitResult) hitResult;
                this.attackEntity(result.getEntity());
            }
        }
    }

    private void updateLinearRotations(RotationUtils.Data data) {
        float targetYaw = data.getRotation().x;
        float targetPitch = data.getRotation().y;
        float maxSpeedX = this.turnSpeedX.getCurrentValue();
        float maxSpeedY = this.turnSpeedY.getCurrentValue();

        float deltaYaw = RotationUtils.normalizeAngle(targetYaw - this.currentRotation.x);
        float deltaPitch = RotationUtils.normalizeAngle(targetPitch - this.currentRotation.y);

        // 核心修复: 预测减速，避免转圈圈
        float yawSpeed = Math.min(Math.abs(deltaYaw), maxSpeedX) * Math.signum(deltaYaw);
        float pitchSpeed = Math.min(Math.abs(deltaPitch), maxSpeedY) * Math.signum(deltaPitch);

        if (Math.abs(deltaYaw) < Math.abs(yawSpeed)) {
            yawSpeed = deltaYaw;
        }
        if (Math.abs(deltaPitch) < Math.abs(pitchSpeed)) {
            pitchSpeed = deltaPitch;
        }

        this.currentRotation.x += yawSpeed;
        this.currentRotation.y += pitchSpeed;

        this.currentRotation.x = RotationUtils.normalizeAngle(this.currentRotation.x);
        this.currentRotation.y = Math.min(90.0F, Math.max(-90.0F, this.currentRotation.y));

        RotationManager.rotations.x = this.currentRotation.x;
        RotationManager.rotations.y = this.currentRotation.y;
        rotation = this.currentRotation;
    }

    private void updateSigmoidRotations(RotationUtils.Data data) {
        float targetYaw = data.getRotation().x;
        float targetPitch = data.getRotation().y;
        float maxSpeedX = this.turnSpeedX.getCurrentValue();
        float maxSpeedY = this.turnSpeedY.getCurrentValue();
        float smoothness = this.sigmoidSmoothness.getCurrentValue();

        float deltaYaw = RotationUtils.normalizeAngle(targetYaw - this.currentRotation.x);
        float deltaPitch = RotationUtils.normalizeAngle(targetPitch - this.currentRotation.y);

        // 使用Sigmoid函数计算移动比例，并用可调的“平滑度”参数来控制曲线
        // 修复：确保 sigmoid 函数的输入值在合理范围内
        float sigmoidYawFactor = (float) (2.0F / (1.0F + Math.exp(-deltaYaw / smoothness)) - 1.0F);
        float sigmoidPitchFactor = (float) (2.0F / (1.0F + Math.exp(-deltaPitch / smoothness)) - 1.0F);

        float newYawSpeed = sigmoidYawFactor * maxSpeedX;
        float newPitchSpeed = sigmoidPitchFactor * maxSpeedY;

        // 核心修复: 预测减速，避免转圈圈和超调
        // 如果剩余角度差小于当前速度，直接锁定目标
        if (Math.abs(deltaYaw) < Math.abs(newYawSpeed)) {
            newYawSpeed = deltaYaw;
        }
        if (Math.abs(deltaPitch) < Math.abs(newPitchSpeed)) {
            newPitchSpeed = deltaPitch;
        }

        this.currentRotation.x += newYawSpeed;
        this.currentRotation.y += newPitchSpeed;

        this.currentRotation.x = RotationUtils.normalizeAngle(this.currentRotation.x);
        this.currentRotation.y = Math.min(90.0F, Math.max(-90.0F, this.currentRotation.y));

        RotationManager.rotations.x = this.currentRotation.x;
        RotationManager.rotations.y = this.currentRotation.y;
        rotation = this.currentRotation;
    }

    private void updateAcceleratedRotations(RotationUtils.Data data) {
        float targetYaw = data.getRotation().x;
        float targetPitch = data.getRotation().y;
        float maxSpeedX = this.turnSpeedX.getCurrentValue();
        float maxSpeedY = this.turnSpeedY.getCurrentValue();
        float accel = this.acceleration.getCurrentValue();

        float deltaYaw = RotationUtils.normalizeAngle(targetYaw - this.currentRotation.x);
        float deltaPitch = RotationUtils.normalizeAngle(targetPitch - this.currentRotation.y);

        // 使用加速度计算新的速度
        float newSpeedX = this.currentSpeed.x + accel * Math.signum(deltaYaw);
        float newSpeedY = this.currentSpeed.y + accel * Math.signum(deltaPitch);

        // 核心修复: 预测减速，避免转圈圈
        if (Math.abs(deltaYaw) < Math.abs(newSpeedX)) {
            newSpeedX = deltaYaw;
            this.currentSpeed.x = 0.0f; // 停止加速度，防止超调
        } else {
            newSpeedX = Math.min(Math.abs(newSpeedX), maxSpeedX) * Math.signum(deltaYaw);
        }

        if (Math.abs(deltaPitch) < Math.abs(newSpeedY)) {
            newSpeedY = deltaPitch;
            this.currentSpeed.y = 0.0f; // 停止加速度，防止超调
        } else {
            newSpeedY = Math.min(Math.abs(newSpeedY), maxSpeedY) * Math.signum(deltaPitch);
        }

        this.currentRotation.x += newSpeedX;
        this.currentRotation.y += newSpeedY;

        this.currentSpeed.x = newSpeedX;
        this.currentSpeed.y = newSpeedY;

        this.currentRotation.x = RotationUtils.normalizeAngle(this.currentRotation.x);
        this.currentRotation.y = Math.min(90.0F, Math.max(-90.0F, this.currentRotation.y));

        RotationManager.rotations.x = this.currentRotation.x;
        RotationManager.rotations.y = this.currentRotation.y;
        rotation = this.currentRotation;
    }

    public void updateAttackTargets() {
        targets = this.getTargets();
    }

    public boolean isValidTarget(Entity entity) {
        if (entity == mc.player) {
            return false;
        } else if (entity instanceof LivingEntity living) {
            if (living instanceof BlinkingPlayer) {
                return false;
            } else {
                AntiBots module = (AntiBots) Naven.getInstance().getModuleManager().getModule(AntiBots.class);
                if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
                    if (Teams.isSameTeam(living)) {
                        return false;
                    } else if (FriendManager.isFriend(living)) {
                        return false;
                    } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                        return false;
                    } else if (entity instanceof ArmorStand) {
                        return false;
                    } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                        return false;
                    } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                        return false;
                    } else if (!(entity instanceof Player) || !((double) entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                        if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                                && !this.attackMobs.getCurrentValue()) {
                            return false;
                        } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                            return false;
                        } else {
                            return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean isValidAttack(Entity entity) {
        if (!this.isValidTarget(entity)) {
            return false;
        } else if (entity instanceof LivingEntity && (float) ((LivingEntity) entity).hurtTime > this.hurtTime.getCurrentValue()) {
            return false;
        } else {
            Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
            return closestPoint.distanceTo(mc.player.getEyePosition()) > (double) this.aimRange.getCurrentValue()
                    ? false
                    : RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
        }
    }

    public void attackEntity(Entity entity) {
        this.attackTimes++;
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        mc.player.setYRot(RotationManager.rotations.x);
        mc.player.setXRot(RotationManager.rotations.y);
        if (entity instanceof Player && !AntiBots.isBot(entity)) {
            KillSay.attackedPlayers.add(entity.getName().getString());
        }

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
        if (this.moreParticles.getCurrentValue()) {
            mc.player.magicCrit(entity);
            mc.player.crit(entity);
        }

        mc.player.setYRot(currentYaw);
        mc.player.setXRot(currentPitch);
    }

    private List<Entity> getTargets() {
        Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
                .filter(entity -> entity instanceof Entity)
                .filter(this::isValidAttack);
        List<Entity> possibleTargets = stream.collect(Collectors.toList());
        if (this.priority.isCurrentMode("Range")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> (double) o.distanceTo(mc.player)));
        } else if (this.priority.isCurrentMode("FoV")) {
            possibleTargets.sort(
                    Comparator.comparingDouble(o -> (double) RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
            );
        } else if (this.priority.isCurrentMode("Health")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double) living.getHealth() : 0.0));
        }

        if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity) entity).isBaby())) {
            possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity) entity).isBaby());
        }

        possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
        return this.infSwitch.getCurrentValue()
                ? possibleTargets
                : possibleTargets.subList(0, (int) Math.min((float) possibleTargets.size(), this.switchSize.getCurrentValue()));
    }
}