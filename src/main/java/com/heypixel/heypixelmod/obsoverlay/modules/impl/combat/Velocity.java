package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

import java.util.ArrayList;

@ModuleInfo(
        name = "Velocity",
        description = "Reduces knockback.",
        category = Category.MOVEMENT
)
public class Velocity extends Module {
   public static int ticksSinceVelocity = Integer.MAX_VALUE;
   public static Velocity instance;
   private boolean isknockbacked;
   private int offGroundTicks = 0;
   private final ArrayList<Packet<?>> packets = new ArrayList();

   public Velocity() {
      instance = this;
   }

   public void onEnable() {
      this.setSuffix("GrimReduce");
   }

   public void onDisable() {
      this.packets.clear();
   }

   @EventTarget
   public void onLivingUpdate(EventRunTicks event) {
      if (event.getType() != EventType.POST && mc.player != null) {
         if (mc.player.onGround() && this.isknockbacked || this.isknockbacked && this.offGroundTicks > 10) {
            this.isknockbacked = false;
            for(Packet<?> p : this.packets) {
               @SuppressWarnings("unchecked")
               Packet<ClientPacketListener> packet = (Packet<ClientPacketListener>) p;
               packet.handle(mc.player.connection);
            }
            this.packets.clear();
         }
      }
   }

   @EventTarget
   public void onPre(EventMotion eventMotion) {
      if (eventMotion.getType() != EventType.POST) {
         if (getTicksSinceVelocity() <= 14 && mc.player.onGround()) {
            mc.player.jumpFromGround();
         }
      }
   }

   @EventTarget
   public void onTick(EventRunTicks eventRunTicks) {
      if (mc.player == null) return;

      if (mc.player.onGround()) {
         this.offGroundTicks = 0;
      } else {
         ++this.offGroundTicks;
      }

      if (ticksSinceVelocity < Integer.MAX_VALUE) {
         ++ticksSinceVelocity;
      }
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (mc.player == null) {
         return;
      }

      if (e.getPacket() instanceof ClientboundSetEntityMotionPacket) {
         ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) e.getPacket();
         if (motionPacket.getId() == mc.player.getId()) {
            ticksSinceVelocity = 0;
            if (!mc.player.onGround()) {
               this.packets.add(e.getPacket());
               this.isknockbacked = true;
               e.setCancelled(true);
            }
         }
      }

      if (e.getPacket() instanceof ClientboundPingPacket && this.isknockbacked) {
         this.packets.add(e.getPacket());
         e.setCancelled(true);
      }
   }

   public static int getTicksSinceVelocity() {
      return ticksSinceVelocity;
   }
}