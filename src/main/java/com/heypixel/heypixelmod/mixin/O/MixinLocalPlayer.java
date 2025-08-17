package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
   @Shadow
   private boolean wasSprinting;
   @Shadow
   @Final
   public ClientPacketListener connection;
   @Shadow
   private boolean wasShiftKeyDown;
   @Shadow
   private double xLast;
   @Shadow
   private double yLast1;
   @Shadow
   private double zLast;
   @Shadow
   private float yRotLast;
   @Shadow
   private float xRotLast;
   @Shadow
   private int positionReminder;
   @Shadow
   private boolean lastOnGround;
   @Shadow
   private boolean autoJumpEnabled;
   @Shadow
   @Final
   protected Minecraft minecraft;

   @Shadow
   protected abstract boolean isControlledCamera();

   @Shadow
   protected abstract void sendIsSprintingIfNeeded();

   public MixinLocalPlayer(ClientLevel pClientLevel, GameProfile pGameProfile) {
      super(pClientLevel, pGameProfile);
   }

   @Inject(
           method = {"tick"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
                   shift = Shift.BEFORE
           )}
   )
   public void injectUpdateEvent(CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventUpdate());
   }

   /**
    * @author b
    * @reason b
    */
   @Inject(
           method = {"sendPosition"},
           at = @At(value = "HEAD"),
           cancellable = true
   )
   public void onSendPositionPre(CallbackInfo ci) {
      EventMotion eventPre = new EventMotion(EventType.PRE, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround());
      Naven.getInstance().getEventManager().call(eventPre);
      if (eventPre.isCancelled()) {
         Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, eventPre.getYaw(), eventPre.getPitch()));
         ci.cancel();
      }
   }

   @Inject(
           method = {"sendPosition"},
           at = @At(value = "RETURN")
   )
   public void onSendPositionPost(CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, this.yRotLast, this.xRotLast));
   }

   @Redirect(
           method = {"aiStep"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
                   ordinal = 0
           )
   )
   public boolean onSlowdown(LocalPlayer localPlayer) {
      EventSlowdown event = new EventSlowdown(localPlayer.isUsingItem());
      Naven.getInstance().getEventManager().call(event);
      return event.isSlowdown();
   }
}