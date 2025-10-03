package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
        name = "AutoPlay",
        description = "Automatically plays the next game.",
        category = Category.MISC
)
public class AutoPlay extends Module {

    private final FloatValue delay = ValueBuilder.create(this, "Delay (s)")
            .setDefaultFloatValue(0.5F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5.0F)
            .build()
            .getFloatValue();

    private final TickTimeHelper timer = new TickTimeHelper();

    private boolean hasPlayed = false;
    private boolean waitingForDelay = false;

    @Override
    public void onEnable() {
        resetState();
    }

    private void resetState() {
        this.hasPlayed = false;
        this.waitingForDelay = false;
        this.timer.reset();
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        

        if (mc.player == null) {
            return;
        }

        Inventory inventory = mc.player.getInventory();
        int targetSlot = 4;
        ItemStack itemStack = inventory.getItem(targetSlot);

        if (waitingForDelay) {
            if (timer.delay((long)(delay.getCurrentValue() * 1000))) {
                mc.player.connection.sendCommand("again");
                this.hasPlayed = true;
                this.waitingForDelay = false;
            }
            return;
        }

        if (hasPlayed) {
            if (!isPlayAgainItem(itemStack)) {
                resetState();
            }
            return;
        }

        if (isPlayAgainItem(itemStack)) {
            this.timer.reset();
            this.waitingForDelay = true;
        }
    }

    private boolean isPlayAgainItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        boolean isEmerald = stack.is(Items.EMERALD);
        boolean hasCorrectName = stack.getHoverName().getString().contains("再来一局");

        return isEmerald && hasCorrectName;
    }
}