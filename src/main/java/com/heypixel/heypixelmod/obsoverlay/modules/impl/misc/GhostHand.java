package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMouseClick;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

@ModuleInfo(
        name = "GhostHand",
        description = "Interact with entities and chests from a distance",
        category = Category.MISC
)
public class GhostHand extends Module {

    private final BooleanValue chests = ValueBuilder.create(this, "Chests")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue enderChests = ValueBuilder.create(this, "EnderChests")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue villagers = ValueBuilder.create(this, "Villagers")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue armorStands = ValueBuilder.create(this, "ArmorStands")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue namedEntities = ValueBuilder.create(this, "Named Entities")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final Minecraft mc = Minecraft.getInstance();

    @EventTarget
    public void onMouseClick(EventMouseClick event) {
        if (event.getKey() == 1 && event.isState()) {
            LocalPlayer player = mc.player;
            if (player != null) {
                double defaultRange = mc.gameMode.getPickRange();
                BlockPos targetPos = null;

                if (chests.getCurrentValue()) {
                    targetPos = findChestAtCrosshair(player, player.level(), defaultRange);
                }

                if (targetPos == null && enderChests.getCurrentValue()) {
                    targetPos = findEnderChestAtCrosshair(player, player.level(), defaultRange);
                }

                if (targetPos != null) {
                    openChest(targetPos);
                } else {
                    Entity targetEntity = findEntityAtCrosshair(player, defaultRange);
                    if (targetEntity != null) {
                        interactWithEntity(targetEntity);
                    }
                }
            }
        }
    }

    private BlockPos findChestAtCrosshair(LocalPlayer player, Level level, double range) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition(1.0F);

        for (double i = 0.5; i <= range; i += 0.01) {
            Vec3 currentPos = startPos.add(lookVec.scale(i));
            BlockPos blockPos = BlockPos.containing(currentPos);
            if (level.getBlockState(blockPos).getBlock() instanceof ChestBlock) {
                return blockPos;
            }
        }
        return null;
    }

    private BlockPos findEnderChestAtCrosshair(LocalPlayer player, Level level, double range) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition(1.0F);

        for (double i = 0.5; i <= range; i += 0.01) {
            Vec3 currentPos = startPos.add(lookVec.scale(i));
            BlockPos blockPos = BlockPos.containing(currentPos);
            if (level.getBlockState(blockPos).getBlock() instanceof EnderChestBlock) {
                return blockPos;
            }
        }
        return null;
    }

    private Entity findEntityAtCrosshair(LocalPlayer player, double range) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 maxRangeVec = eyePosition.add(lookVec.scale(range));

        Entity pointedEntity = null;
        double closestDistance = range;

        for (Entity entity : mc.level.getEntities(player, player.getBoundingBox().inflate(range))) {
            if (isTarget(entity)) {
                Optional<Vec3> rayTraceResult = entity.getBoundingBox().inflate(entity.getBbWidth() / 2.0).clip(eyePosition, maxRangeVec);

                if (rayTraceResult.isPresent()) {
                    double distanceToEntity = eyePosition.distanceTo(rayTraceResult.get());
                    if (distanceToEntity < closestDistance || closestDistance == 0.0) {
                        pointedEntity = entity;
                        closestDistance = distanceToEntity;
                    }
                }
            }
        }
        return pointedEntity;
    }

    private boolean isTarget(Entity entity) {
        if (villagers.getCurrentValue() && entity instanceof Villager) {
            return true;
        }
        if (armorStands.getCurrentValue() && entity instanceof ArmorStand && entity.hasCustomName()) {
            return true;
        }
        if (namedEntities.getCurrentValue() && entity.hasCustomName()) {
            String name = entity.getDisplayName().getString().toUpperCase();
            if (name.contains("SHOP") || name.contains("CLICK") || name.contains("UPGRADES") || name.contains("QUEST")) {
                return true;
            }
        }
        return false;
    }

    private void interactWithEntity(Entity entity) {
        mc.getConnection().send(ServerboundInteractPacket.createInteractionPacket(entity, false, InteractionHand.MAIN_HAND));
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void openChest(BlockPos pos) {
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                Direction.UP,
                pos,
                false
        );
        mc.getConnection().send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                hitResult,
                0
        ));
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
}