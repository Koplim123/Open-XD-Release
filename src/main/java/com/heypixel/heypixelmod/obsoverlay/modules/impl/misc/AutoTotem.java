package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

@ModuleInfo(
        name = "AutoTotem",
        category = Category.MISC,
        description = "Automatically re-totems when popped."
)
public class AutoTotem extends Module {

    private FloatValue delay;
    private BooleanValue silentSwitch;
    private long lastActionTime;
    private boolean totemPopped;

    public AutoTotem() {
        delay = ValueBuilder.create(this, "Delay")
                .setDefaultFloatValue(40.0f)
                .setFloatStep(1.0f)
                .setMinFloatValue(0.0f)
                .setMaxFloatValue(200.0f)
                .setFloatStep(1.0f)
                .build()
                .getFloatValue();

        silentSwitch = ValueBuilder.create(this, "Silent Switch")
                .setDefaultBooleanValue(false)
                .build()
                .getBooleanValue();

        lastActionTime = System.currentTimeMillis();
        totemPopped = false;
    }

    @Override
    public void onEnable() {
        lastActionTime = System.currentTimeMillis();
        totemPopped = false;
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof ClientboundEntityEventPacket) {
            ClientboundEntityEventPacket packet = (ClientboundEntityEventPacket) e.getPacket();
            // Added null check for mc.level to prevent NullPointerException
            if (mc.level != null) {
                Entity entity = packet.getEntity(mc.level);
                if (entity != null && entity.equals(mc.player) && packet.getEventId() == 35) {
                    totemPopped = true;
                    lastActionTime = System.currentTimeMillis();
                }
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (!totemPopped || System.currentTimeMillis() - lastActionTime < delay.getCurrentValue()) {
                return;
            }

            if (mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
                totemPopped = false;
                return;
            }

            int totemSlot = findItemSlot(Items.TOTEM_OF_UNDYING);
            if (totemSlot != -1) {
                int originalSlot = mc.player.getInventory().selected;

                if (silentSwitch.getCurrentValue()) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(totemSlot));
                } else {
                    mc.player.getInventory().selected = totemSlot;
                }

                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, totemSlot < 9 ? totemSlot + 36 : totemSlot, 40, ClickType.SWAP, mc.player);

                if (silentSwitch.getCurrentValue()) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                }

                totemPopped = false;
                lastActionTime = System.currentTimeMillis();
            }
        }
    }

    private int findItemSlot(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getItem(i).getItem() == item) {
                return i;
            }
        }
        for (int i = 9; i < 36; ++i) {
            if (mc.player.getInventory().getItem(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
