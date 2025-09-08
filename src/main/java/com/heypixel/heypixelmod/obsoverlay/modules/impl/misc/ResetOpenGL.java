package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.GL;
import com.mojang.blaze3d.systems.RenderSystem;

@ModuleInfo(
        name = "ResetOpenGL",
        category = Category.MISC,
        description = "Resets OpenGL state when enabled"
)
public class ResetOpenGL extends Module {
    @Override
    public void onEnable() {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}