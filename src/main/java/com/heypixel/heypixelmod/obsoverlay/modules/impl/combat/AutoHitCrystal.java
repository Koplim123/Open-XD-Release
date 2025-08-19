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
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "AutoHitCrystal",
        category = Category.COMBAT,
        description = "Automatically hits crystals by syncing with enemy damage ticks."
)
public class AutoHitCrystal extends Module {

    private FloatValue delay;
    private ModeValue mode;
    private BooleanValue silent;
    private BooleanValue perfectTiming;
    private BooleanValue pauseOnKill;

    private long lastActionTime;
    private int progress;
    private Player target;

    public AutoHitCrystal() {
        delay = ValueBuilder.create(this, "Delay")
                .setDefaultFloatValue(30.0f)
                .setFloatStep(1.0f)
                .setMinFloatValue(0.0f)
                .setMaxFloatValue(200.0f)
                .build()
                .getFloatValue();
        mode = ValueBuilder.create(this, "Crystal")
                .setModes("None", "Single Tap", "Double Tap")
                .setDefaultModeIndex(1)
                .build()
                .getModeValue();
        silent = ValueBuilder.create(this, "Silent")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();
        perfectTiming = ValueBuilder.create(this, "Perfect Timing")
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
        this.target = null;
        this.progress = 0;
        this.lastActionTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.progress = 0;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() == EventType.PRE) {
            if (mc.screen != null || !mc.isWindowActive() || (pauseOnKill.getCurrentValue() && isInvalidPlayer())) {
                return;
            }

            if (System.currentTimeMillis() - lastActionTime < delay.getCurrentValue()) {
                return;
            }

            this.target = findClosestPlayer();
            if (this.target != null && mc.player.distanceTo(this.target) > 10.0f) {
                this.target = null;
            }

            switch (progress) {
                case 0: {
                    int obsidianSlot = findItemInHotbar(Items.OBSIDIAN);
                    if (obsidianSlot != -1) {
                        setSlot(obsidianSlot);
                        setProgress();
                    }
                    break;
                }
                case 1: {
                    HitResult hitResult = mc.hitResult;
                    if (hitResult instanceof BlockHitResult blockHitResult && mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() != Blocks.AIR) {
                        BlockPos blockPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                        if (!isCollidesWithEntity(blockPos)) {
                            if (mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() == Blocks.OBSIDIAN) {
                                setProgress();
                                break;
                            }
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            setProgress();
                        }
                    }
                    break;
                }
                case 2: {
                    int crystalSlot = findItemInHotbar(Items.END_CRYSTAL);
                    if (crystalSlot != -1) {
                        setSlot(crystalSlot);
                        setProgress();
                    }
                    break;
                }
                case 3: {
                    HitResult hitResult = mc.hitResult;
                    if (hitResult instanceof BlockHitResult blockHitResult && mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() == Blocks.OBSIDIAN && !isCollidesWithEntity(blockHitResult.getBlockPos().above())) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        setProgress();
                    }
                    break;
                }
                case 4: {
                    if (mode.isCurrentMode("None")) {
                        toggle();
                        break;
                    }
                    if (perfectTiming.getCurrentValue() && target != null && target.hurtTime != 0) {
                        break;
                    }

                    EndCrystal nearestCrystal = findNearestCrystal();
                    if (nearestCrystal != null) {
                        attackCrystal(nearestCrystal);
                        if (mode.isCurrentMode("Double Tap")) {
                            setProgress();
                        } else {
                            toggle();
                        }
                    }
                    break;
                }
                case 5: {
                    HitResult hitResult = mc.hitResult;
                    if (hitResult instanceof BlockHitResult blockHitResult && mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() == Blocks.OBSIDIAN && !isCollidesWithEntity(blockHitResult.getBlockPos().above())) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        setProgress();
                    }
                    break;
                }
                case 6: {
                    if (perfectTiming.getCurrentValue() && target != null && target.hurtTime != 0) {
                        break;
                    }

                    EndCrystal nearestCrystal = findNearestCrystal();
                    if (nearestCrystal != null) {
                        attackCrystal(nearestCrystal);
                        toggle();
                    }
                    break;
                }
            }
        }
    }

    private Player findClosestPlayer() {
        Player closest = null;
        double minDistance = Double.MAX_VALUE;
        for (Player player : mc.level.players()) {
            if (player != mc.player && player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                double distance = mc.player.distanceToSqr(player);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = player;
                }
            }
        }
        return closest;
    }

    private EndCrystal findNearestCrystal() {
        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(entity -> entity instanceof EndCrystal && mc.player.distanceTo(entity) <= 4.5f)
                .map(entity -> (EndCrystal) entity)
                .min((c1, c2) -> Float.compare(mc.player.distanceTo(c1), mc.player.distanceTo(c2)))
                .orElse(null);
    }

    private void attackCrystal(EndCrystal crystal) {
        int originalSlot = mc.player.getInventory().selected;
        if (silent.getCurrentValue()) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
        }

        mc.gameMode.attack(mc.player, crystal);
        mc.player.swing(InteractionHand.MAIN_HAND);

        if (silent.getCurrentValue()) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
        }
    }

    private void setSlot(int slot) {
        int originalSlot = mc.player.getInventory().selected;
        if (silent.getCurrentValue()) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        } else {
            mc.player.getInventory().selected = slot;
        }
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
            if (player != mc.player && player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                return false;
            }
        }
        return true;
    }

    private void setProgress() {
        progress += 1;
        lastActionTime = System.currentTimeMillis();
    }
}
