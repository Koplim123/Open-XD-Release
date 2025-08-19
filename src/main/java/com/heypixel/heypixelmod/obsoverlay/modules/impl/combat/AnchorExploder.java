package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(
        name = "AnchorExploder",
        category = Category.COMBAT,
        description = "Explodes anchors with glowstone in them when looking at them."
)
public class AnchorExploder extends Module {

    private FloatValue delay;
    private FloatValue switchTo;
    private long lastActionTime;

    public AnchorExploder() {
        delay = ValueBuilder.create(this, "Delay")
                .setDefaultFloatValue(40.0f)
                .setFloatStep(1.0f)
                .setMinFloatValue(0.0f)
                .setMaxFloatValue(200.0f)
                .setFloatStep(1.0f)
                .build()
                .getFloatValue();

        switchTo = ValueBuilder.create(this, "Switch To")
                .setDefaultFloatValue(1.0f)
                .setMinFloatValue(1.0f)
                .setMaxFloatValue(9.0f)
                .setFloatStep(1.0f)
                .build()
                .getFloatValue();

        lastActionTime = System.currentTimeMillis();
    }

    @Override
    public void onEnable() {
        lastActionTime = System.currentTimeMillis();
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (mc.screen != null || !mc.isWindowActive() || mc.player.isUsingItem() && mc.player.getUseItem().getItem() == Items.SHIELD) {
                return;
            }

            if (System.currentTimeMillis() - lastActionTime < delay.getCurrentValue()) {
                return;
            }

            HitResult result = mc.hitResult;
            if (result != null && result.getType() == Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) result;
                BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());

                if (blockState.getBlock() == Blocks.RESPAWN_ANCHOR && blockState.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) > 0) {
                    if (mc.player.getInventory().getItem(mc.player.getInventory().selected).getItem() == Items.GLOWSTONE) {
                        mc.player.getInventory().selected = (int) switchTo.getCurrentValue() - 1;
                        lastActionTime = System.currentTimeMillis();
                        return;
                    }

                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    lastActionTime = System.currentTimeMillis();
                }
            } else {
                lastActionTime = System.currentTimeMillis();
            }
        }
    }
}
