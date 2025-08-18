//前提提要 这是一个狗屎代码 Full打勾是全反 Jump Reset打勾是跳跃重置 其他的都不要打,因为都是空壳
package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.LocalPlayerAccessor;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStrafe;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FletchingTableBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;




@ModuleInfo(
        name = "BetaVelocity",
        description = "Reduces knockback.",
        category = Category.MOVEMENT
)

public class BetaVelocity extends Module {

    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Cancel", "Air", "Horizontal", "Watchdog", "Boost", "Jump Reset", "GrimAC", "Reduce", "Full")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private final ModeValue jumpResetMode = ValueBuilder.create(this, "Jump Reset Mode")
            .setModes("Hurt Time", "Packet")
            .setDefaultModeIndex(1)
            .setVisibility(() -> this.mode.isCurrentMode("Jump Reset"))
            .build()
            .getModeValue();

    private final FloatValue jumpResetHurtTime = ValueBuilder.create(this, "Jump Reset Hurt Time")
            .setDefaultFloatValue(9.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> this.mode.isCurrentMode("Jump Reset") && this.jumpResetMode.isCurrentMode("Hurt Time"))
            .build()
            .getFloatValue();

    public BooleanValue log = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue test = ValueBuilder.create(this, "Full").setDefaultBooleanValue(false).build().getBooleanValue();
    private final FloatValue attacks = ValueBuilder.create(this, "Attacks")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .build()
            .getFloatValue();
    private final FloatValue skips = ValueBuilder.create(this, "Ticks")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(2.0F)
            .setMaxFloatValue(10.0F)
            .setVisibility(this.test::getCurrentValue)
            .build()
            .getFloatValue();
    private boolean velocity = false;
    private boolean sprint = false;
    private final TickTimeHelper timer = new TickTimeHelper();
    private int delayVelocity = 0;
    private int velocityTicks = 0;
    LinkedBlockingDeque<Packet<ClientGamePacketListener>> inBound = new LinkedBlockingDeque<>();
    BlockHitResult result = null;
    private boolean nextMovement;
    private int slowdownTicks = 0;
    private int direction = 1;
    private int preC0f = 0;
    private int grimAction = 0;
    private LivingEntity entity;
    private boolean veloPacket = false;

    private Optional<LivingEntity> findEntity() {

        if (mc == null || mc.player == null || mc.hitResult == null) {
            return Optional.empty();
        }

        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == Type.ENTITY) {
            Entity entity = ((EntityHitResult)hitResult).getEntity();
            if (entity instanceof Player) {
                return Optional.of((LivingEntity)entity);
            }
        }

        return Optional.empty();
    }

    @EventTarget
    public void onWorld(EventRespawn eventRespawn) {

        if (mc == null || mc.player == null) {
            return;
        }

        this.delayVelocity = 0;
        this.velocity = false;
        this.result = null;
        this.veloPacket = false;
    }

    @EventTarget
    public void onMotion(EventMotion eventMotion) {

        if (mc == null || mc.player == null) {
            return;
        }

        if (eventMotion.getType() == EventType.PRE) {
            this.slowdownTicks--;
            if (this.slowdownTicks == 0) {
                Naven.TICK_TIMER = 1.0F;
            } else if (this.slowdownTicks > 0) {
                ChatUtils.addChatMessage("Slowdown Ticks: " + this.slowdownTicks);
                Naven.TICK_TIMER = 1.0F / this.slowdownTicks;
            }


            if (this.mode.isCurrentMode("Jump Reset")) {
                if (this.jumpResetMode.isCurrentMode("Packet") && this.veloPacket && mc.player.onGround() && !mc.options.keyJump.isDown()) {
                    if (this.checks()) {
                        return;
                    }

                    mc.player.jumpFromGround();
                    this.veloPacket = false;
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {

        if (mc == null || mc.player == null) {
            return;
        }


        if (this.mode.isCurrentMode("Jump Reset") && this.jumpResetMode.isCurrentMode("Hurt Time")) {
            if (mc.player.hurtTime >= this.jumpResetHurtTime.getCurrentValue() && mc.player.onGround() && !mc.options.keyJump.isDown()) {
                if (this.checks()) {
                    return;
                }

                mc.player.jumpFromGround();
                this.veloPacket = false;
            }
        }
    }

    @EventTarget
    public void onMotion(EventClick e) {

        if (mc == null || mc.player == null) {
            return;
        }

        if (this.velocity && !this.test.getCurrentValue()) {
            if (this.sprint && !((LocalPlayerAccessor)mc.player).isWasSprinting()) {
                this.velocity = false;
                return;
            }

            this.velocity = false;
            if (this.sprint) {
                float currentYaw = mc.player.getYRot();
                float currentPitch = mc.player.getXRot();
                mc.player.setYRot(RotationManager.rotations.x);
                mc.player.setXRot(RotationManager.rotations.y);

                for (int i = 0; i < this.attacks.getCurrentValue(); i++) {
                    mc.gameMode.attack(mc.player, this.entity);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }

                mc.player.setYRot(currentYaw);
                mc.player.setXRot(currentPitch);
            }

            if (!this.sprint) {
            }
        }
    }

    @EventTarget(0)
    public void onPacket(EventHandlePacket e) {
        try {

            if (mc == null || mc.player == null || e.isCancelled()) {
                return;
            }

            if (mc.player != null && !e.isCancelled()) {
                if (e.getPacket() instanceof ClientboundSetEntityMotionPacket && this.timer.delay(3)) {
                    ClientboundSetEntityMotionPacket packet = (ClientboundSetEntityMotionPacket)e.getPacket();
                    if (mc.player.getId() == packet.getId()) {
                        double x = packet.getXa() / 8000.0;
                        double z = packet.getZa() / 8000.0;
                        double speed = Math.sqrt(x * x + z * z);
                        Optional<LivingEntity> targetEntity = this.findEntity();


                        if (this.mode.isCurrentMode("Jump Reset") && this.jumpResetMode.isCurrentMode("Packet") && packet.getYa() > 0) {
                            this.veloPacket = true;
                        }

                        if (!this.test.getCurrentValue()
                                && !Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
                                && mc.player.getUseItem().isEmpty()
                                && mc.screen == null
                                && targetEntity.isPresent()) {
                            this.entity = targetEntity.get();
                            this.sprint = ((LocalPlayerAccessor)mc.player).isWasSprinting();
                            if (this.sprint) {
                                e.setCancelled(true);
                                if (this.log.getCurrentValue()) {
                                    ChatUtils.addChatMessage("Speed: " + (float)Math.round(speed * 100.0) / 100.0F);
                                }

                                x *= Math.pow(0.6, this.attacks.getCurrentValue());
                                z *= Math.pow(0.6, this.attacks.getCurrentValue());
                                mc.player.setDeltaMovement(x, packet.getYa() / 8000.0, z);
                                this.velocity = true;
                                this.timer.reset();
                            }
                        }

                        if (this.test.getCurrentValue() && mc.player.tickCount > 120) {
                            double horizontalStrength = new Vector2d(packet.getXa(), packet.getZa()).length();
                            if (horizontalStrength <= 1000.0) {
                                return;
                            }

                            if (packet.getYa() < 0) {
                                return;
                            }

                            this.delayVelocity = (int)this.skips.getCurrentValue();
                            this.velocityTicks = 0;
                            this.velocity = true;
                            e.setCancelled(true);
                        }
                    }
                }

                if (this.test.getCurrentValue() && mc.player.tickCount > 120) {
                    Packet<?> packet = e.getPacket();
                    if (packet instanceof ClientboundPlayerPositionPacket wrapped) {
                        while (!this.inBound.isEmpty()) {
                            this.inBound.poll().handle(mc.player.connection);
                        }
                    }

                    if (packet instanceof ClientboundPingPacket pingPacket) {
                        if (Math.abs(this.preC0f - pingPacket.getId()) == 1) {
                            this.grimAction = pingPacket.getId();
                        }

                        this.preC0f = pingPacket.getId();
                        if (this.grimAction != pingPacket.getId() && Math.abs(this.grimAction - pingPacket.getId()) > 10 && mc.player.hurtTime > 0) {
                            mc.player.hurtTime = 0;
                            e.setCancelled(true);
                            return;
                        }
                    }

                    if (this.delayVelocity > 0 && this.velocity) {
                        if (packet instanceof ClientboundSystemChatPacket) {
                            return;
                        }

                        if (e.getPacket().getClass().getSimpleName().startsWith("C")
                                && !(packet instanceof ClientboundSetEntityMotionPacket)
                                && !(packet instanceof ClientboundExplodePacket)
                                && !(packet instanceof ClientboundSetTimePacket)
                                && !(packet instanceof ClientboundMoveEntityPacket)
                                && !(packet instanceof ClientboundTeleportEntityPacket)
                                && !(packet instanceof ClientboundSoundPacket)
                                && !(packet instanceof ClientboundSetHealthPacket)
                                && !(packet instanceof ClientboundPlayerPositionPacket)
                                && !(packet instanceof ClientboundSystemChatPacket)) {
                            e.setCancelled(true);
                            this.inBound.add((Packet<ClientGamePacketListener>)packet);
                            if (packet instanceof ClientboundPingPacket) {
                                this.delayVelocity--;
                                if (this.delayVelocity == 0) {
                                    this.delayVelocity++;
                                    BlockHitResult blockRayTraceResult = (BlockHitResult)PlayerUtils.pickCustom(4.5, mc.player.getYRot(), 90.0F);
                                    if (blockRayTraceResult == null) {
                                        return;
                                    }

                                    if (BlockUtils.isAirBlock(blockRayTraceResult.getBlockPos())) {
                                        return;
                                    }

                                    AABB aabb = new AABB(blockRayTraceResult.getBlockPos().above());
                                    if (!mc.player.getBoundingBox().intersects(aabb)) {
                                        return;
                                    }

                                    this.delayVelocity--;

                                    while (!this.inBound.isEmpty()) {
                                        this.inBound.poll().handle(mc.player.connection);
                                    }

                                    this.result = new BlockHitResult(
                                            blockRayTraceResult.getLocation(), blockRayTraceResult.getDirection(), blockRayTraceResult.getBlockPos(), false
                                    );
                                    this.direction = (int)(this.direction * -0.1);
                                    float pitch = (float)(MathUtils.getRandomDoubleInRange(89.1F, 90.0) - MathUtils.getRandomDoubleInRange(0.002F, 1.0E-14));
                                    float yaw = (float)(((LocalPlayerAccessor)mc.player).getYRotLast() - MathUtils.getRandomDoubleInRange(0.002F, 0.004F));
                                    ((LocalPlayerAccessor)mc.player).setYRotLast(yaw);
                                    ((LocalPlayerAccessor)mc.player).setXRotLast(pitch);
                                    float currentYaw = mc.player.getYRot();
                                    mc.player.connection.send(new Rot(yaw, pitch, mc.player.onGround()));
                                    mc.player
                                            .connection
                                            .send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, this.result, MathUtils.getRandomIntInRange(0, 2)));
                                    if (this.log.getCurrentValue()) {
                                        ChatUtils.addChatMessage("Send");
                                    }


                                    mc.hitResult = this.result;
                                    this.delayVelocity = 0;
                                    this.result = null;
                                    this.velocity = false;
                                    this.nextMovement = true;
                                }
                            }
                        }
                    }

                    if (packet instanceof ServerboundMovePlayerPacket
                            && !(packet instanceof Pos)
                            && !(packet instanceof Rot)
                            && !(packet instanceof PosRot)
                            && this.nextMovement) {
                        mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.onGround()));
                        e.setCancelled(true);
                        this.nextMovement = false;
                    }
                }
            }
        } catch (Exception var12) {
            var12.printStackTrace();
        }
    }

    private boolean checks() {

        if (mc == null || mc.player == null) {
            return true;
        }

        return mc.player.isInWater() || mc.player.isInLava() || mc.player.isPassenger() || mc.player.isFallFlying();
    }
}