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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(
        name = "AnchorPlacer",
        category = Category.COMBAT,
        description = "Places an anchor on the block your looking at and glowstones it."
)
public class AnchorPlacer extends Module {

    private ModeValue mode;
    private FloatValue delay;
    private BooleanValue glowstone;
    private BooleanValue detonate;
    private BooleanValue switchBack;
    private FloatValue switchSlot;

    private long lastActionTime;
    private int progress;
    private int originalSlot;

    public AnchorPlacer() {
        mode = ValueBuilder.create(this, "Mode")
                .setModes("Normal", "Glowstone")
                .setDefaultModeIndex(0)
                .build()
                .getModeValue();
        delay = ValueBuilder.create(this, "Delay")
                .setDefaultFloatValue(30.0f)
                .setFloatStep(1.0f)
                .setMinFloatValue(0.0f)
                .setMaxFloatValue(200.0f)
                .build()
                .getFloatValue();
        glowstone = ValueBuilder.create(this, "Glowstone")
                .setDefaultBooleanValue(true)
                .setVisibility(() -> mode.isCurrentMode("Normal"))
                .build()
                .getBooleanValue();
        detonate = ValueBuilder.create(this, "Detonate")
                .setDefaultBooleanValue(false)
                .setVisibility(() -> mode.isCurrentMode("Normal") && glowstone.getCurrentValue())
                .build()
                .getBooleanValue();
        switchBack = ValueBuilder.create(this, "Switch Back")
                .setDefaultBooleanValue(true)
                .build()
                .getBooleanValue();
        switchSlot = ValueBuilder.create(this, "Switch Slot")
                .setDefaultFloatValue(1.0f)
                .setFloatStep(1.0f)
                .setMinFloatValue(1.0f)
                .setMaxFloatValue(9.0f)
                .setVisibility(() -> switchBack.getCurrentValue())
                .build()
                .getFloatValue();

        lastActionTime = 0;
        progress = 0;
        originalSlot = 0;
    }

    @Override
    public void onEnable() {
        this.progress = 0;
        this.lastActionTime = System.currentTimeMillis();
        this.originalSlot = mc.player.getInventory().selected;
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.player.getInventory() != null && originalSlot != mc.player.getInventory().selected) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
        }
        this.progress = 0;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        if (mc.screen != null || !mc.isWindowActive() || System.currentTimeMillis() - lastActionTime < delay.getCurrentValue()) {
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        if (mode.isCurrentMode("Normal")) {
            switch (progress) {
                case 0: { // Step 0: Find and switch to a Respawn Anchor.
                    int anchorSlot = findBlockInHotbar(Blocks.RESPAWN_ANCHOR);
                    if (anchorSlot != -1) {
                        setSlot(anchorSlot);
                        setProgress();
                    } else {
                        toggle();
                    }
                    break;
                }
                case 1: { // Step 1: Place the Respawn Anchor.
                    BlockPos pos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                    if (!isCollidesWithEntity(pos)) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        setProgress();
                    }
                    break;
                }
                case 2: { // Step 2: If 'glowstone' is enabled, find and switch to Glowstone.
                    if (glowstone.getCurrentValue()) {
                        int glowstoneSlot = findItemInHotbar(Items.GLOWSTONE);
                        if (glowstoneSlot != -1) {
                            setSlot(glowstoneSlot);
                            setProgress();
                        } else {
                            toggle();
                        }
                    } else {
                        // If glowstone is not enabled, skip to the final steps.
                        toggle();
                    }
                    break;
                }
                case 3: { // Step 3: Use Glowstone to charge the Anchor.
                    if (mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() == Blocks.RESPAWN_ANCHOR) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        if(detonate.getCurrentValue()) {
                            // If detonate is enabled, move to the next step (switch for detonate).
                            setProgress();
                        } else {
                            // If detonate is not enabled, move to the final step (switch back).
                            if (switchBack.getCurrentValue()) {
                                setProgress();
                            } else {
                                toggle();
                            }
                        }
                    }
                    break;
                }
                case 4: { // Step 4: Switch to the user-specified slot for detonation.
                    if (detonate.getCurrentValue()) {
                        setSlot((int) switchSlot.getCurrentValue() - 1);
                        setProgress();
                    } else {
                        // If detonate is not enabled, skip this step.
                        setProgress();
                    }
                    break;
                }
                case 5: { // Step 5: Detonate the charged Respawn Anchor.
                    if (detonate.getCurrentValue()) {
                        BlockPos blockPos = blockHitResult.getBlockPos();
                        if (mc.level.getBlockState(blockPos).getBlock() == Blocks.RESPAWN_ANCHOR && mc.level.getBlockState(blockPos).getValue(RespawnAnchorBlock.CHARGE) > 0) {
                            // The explosion is caused by a player hitting a charged anchor
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            setProgress();
                        }
                    } else {
                        // If detonate is not enabled, skip this step.
                        setProgress();
                    }
                    break;
                }
                case 6: { // Step 6: Switch back to the original slot and toggle the module off.
                    if (switchBack.getCurrentValue()) {
                        setSlot(originalSlot);
                    }
                    toggle();
                    break;
                }
            }
        }
        else if (mode.isCurrentMode("Glowstone")) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (mc.level.getBlockState(blockPos).getBlock() != Blocks.RESPAWN_ANCHOR) {
                return;
            }

            int charges = mc.level.getBlockState(blockPos).getValue(RespawnAnchorBlock.CHARGE);
            if (charges > 0) {
                return;
            }

            switch (progress) {
                case 0: {
                    int glowstoneSlot = findItemInHotbar(Items.GLOWSTONE);
                    if (glowstoneSlot != -1) {
                        setSlot(glowstoneSlot);
                        setProgress();
                    } else {
                        toggle();
                    }
                    break;
                }
                case 1: {
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    setProgress();
                    break;
                }
                case 2: {
                    if (switchBack.getCurrentValue()) {
                        setSlot((int) switchSlot.getCurrentValue() - 1);
                    }
                    toggle();
                    break;
                }
            }
        }
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getItem(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findBlockInHotbar(net.minecraft.world.level.block.Block block) {
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getItem(i).getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
                return i;
            }
        }
        return -1;
    }

    private int findToolInHotbar() {
        for (int i = 0; i < 9; ++i) {
            Item item = mc.player.getInventory().getItem(i).getItem();
            if (item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE) {
                return i;
            }
        }
        return -1;
    }

    private void setSlot(int slot) {
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }

    private boolean isCollidesWithEntity(BlockPos pos) {
        AABB boundingBox = new AABB(pos);
        for (Entity entity : mc.level.getEntities(null, boundingBox)) {
            if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                return true;
            }
        }
        return false;
    }

    private void setProgress() {
        progress++;
        lastActionTime = System.currentTimeMillis();
    }
}
