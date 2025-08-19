package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;
import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "AutoCrystal",
        category = Category.COMBAT,
        description = "Automatically places and explodes crystals."
)
public class AutoCrystal extends Module {

    private FloatValue delay;
    private BooleanValue silentSwap;
    private BooleanValue headBob;
    private BooleanValue inAir;
    private BooleanValue switchSetting;
    private BooleanValue damageTick;
    private BooleanValue pauseOnKill;
    private long lastActionTime;
    private BlockPos posToPlace;

    public AutoCrystal() {
        delay = ValueBuilder.create(this, "Delay")
                .setDefaultFloatValue(25.0f)
                .setFloatStep(1.0f)
                .setMinFloatValue(0.0f)
                .setMaxFloatValue(200.0f)
                .setFloatStep(1.0f)
                .build()
                .getFloatValue();
        silentSwap = ValueBuilder.create(this, "Silent Swap")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();
        headBob = ValueBuilder.create(this, "Head Bob")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();
        inAir = ValueBuilder.create(this, "In Air")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();
        switchSetting = ValueBuilder.create(this, "Switch")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();
        damageTick = ValueBuilder.create(this, "Damage Tick")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();
        pauseOnKill = ValueBuilder.create(this, "Pause On Kill")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();
    }

    @Override
    public void onEnable() {
        if (switchSetting.getCurrentValue()) {
            int slot = findItemInHotbar(Items.END_CRYSTAL);
            if (slot != -1) {
                mc.player.getInventory().selected = slot;
            }
        }
        lastActionTime = System.currentTimeMillis();
        posToPlace = null;
    }

    @Override
    public void onDisable() {
        posToPlace = null;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (mc.screen != null || !mc.isWindowActive() || (pauseOnKill.getCurrentValue() && isInvalidPlayer())) {
                return;
            }

            if (System.currentTimeMillis() - lastActionTime < delay.getCurrentValue()) {
                return;
            }

            if (!mc.player.onGround() && !inAir.getCurrentValue()) {
                return;
            }

            HitResult hitResult = mc.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                Entity entity = ((EntityHitResult) hitResult).getEntity();
                if (entity instanceof EndCrystal || entity instanceof MagmaCube || entity instanceof Slime) {
                    if (!damageTick.getCurrentValue() || (entity instanceof Player && ((Player) entity).hurtTime > 0)) {
                        int originalSlot = mc.player.getInventory().selected;
                        int crystalSlot = findItemInHotbar(Items.END_CRYSTAL);

                        if (silentSwap.getCurrentValue() && crystalSlot != -1) {
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(crystalSlot));
                        } else if (crystalSlot != mc.player.getInventory().selected) {
                            return;
                        }

                        mc.gameMode.attack(mc.player, entity);
                        mc.player.swing(InteractionHand.MAIN_HAND);

                        lastActionTime = System.currentTimeMillis();

                        if (silentSwap.getCurrentValue() && crystalSlot != -1) {
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                        }

                        return;
                    }
                }
            }

            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                BlockPos blockPos = blockHitResult.getBlockPos();
                if (mc.level.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || mc.level.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) {
                    BlockPos placePos = blockPos.above();
                    if (!isCollidesWithEntity(placePos)) {
                        int originalSlot = mc.player.getInventory().selected;
                        int crystalSlot = findItemInHotbar(Items.END_CRYSTAL);

                        if (silentSwap.getCurrentValue() && crystalSlot != -1) {
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(crystalSlot));
                        } else if (crystalSlot != mc.player.getInventory().selected) {
                            return;
                        }

                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                        lastActionTime = System.currentTimeMillis();

                        if (silentSwap.getCurrentValue() && crystalSlot != -1) {
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                        }

                        return;
                    }
                }
            }

            if (headBob.getCurrentValue() && (hitResult == null || hitResult.getType() == HitResult.Type.MISS)) {
                Optional<EndCrystal> nearestCrystal = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                        .filter(entityx -> entityx instanceof EndCrystal)
                        .map(entityx -> (EndCrystal)entityx)
                        .min((c1, c2) -> Float.compare(mc.player.distanceTo(c1), mc.player.distanceTo(c2)));

                nearestCrystal.ifPresent(crystal -> {
                    if (mc.player.distanceTo(crystal) <= 4.5f) {
                        int originalSlot = mc.player.getInventory().selected;
                        int crystalSlot = findItemInHotbar(Items.END_CRYSTAL);

                        if (silentSwap.getCurrentValue() && crystalSlot != -1) {
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(crystalSlot));
                        } else if (crystalSlot != mc.player.getInventory().selected) {
                            return;
                        }

                        mc.gameMode.attack(mc.player, crystal);
                        mc.player.swing(InteractionHand.MAIN_HAND);

                        lastActionTime = System.currentTimeMillis();

                        if (silentSwap.getCurrentValue() && crystalSlot != -1) {
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                        }
                    }
                });
            }
        }
    }

    private boolean isHoldingOrSilentSwapping(net.minecraft.world.item.Item item) {
        return findItemInHotbar(item) != -1 && silentSwap.getCurrentValue();
    }

    private void silentSwap(int slot) {
    }

    private int findItemInHotbar(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getItem(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean isCollidesWithEntity(BlockPos pos) {
        AABB boundingBox = new AABB(pos);
        for (Entity entity : mc.level.getEntities(null, boundingBox)) {
            if (entity instanceof EndCrystal || entity instanceof Player) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidPlayer() {
        for (Player player : mc.level.players()) {
            if (player != mc.player && player.isAlive()) {
                return false;
            }
        }
        return true;
    }
}
