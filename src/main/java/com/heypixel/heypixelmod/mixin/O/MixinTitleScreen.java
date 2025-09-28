package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.ui.MainUI.MainUI;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Minecraft.getInstance().execute(() -> {
            if (IRCLoginManager.userId != -1 && Minecraft.getInstance().screen instanceof TitleScreen) {
                Minecraft.getInstance().setScreen(new MainUI());
            }
        });
    }
}
