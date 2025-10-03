package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

@ModuleInfo(
   name = "Scoreboard",
   description = "Modifies the scoreboard",
   category = Category.RENDER
)
public class Scoreboard extends Module {
   public BooleanValue hideScore = ValueBuilder.create(this, "Hide Red Score").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue down = ValueBuilder.create(this, "Down")
      .setDefaultFloatValue(120.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(300.0F)
      .build()
      .getFloatValue();
   
   public BooleanValue blur = ValueBuilder.create(this, "Blur Background")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   
   public BooleanValue roundedCorner = ValueBuilder.create(this, "Rounded Corner")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   
   public FloatValue cornerRadius = ValueBuilder.create(this, "Corner Radius")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.5F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(15.0F)
      .build()
      .getFloatValue();
   
   private float scoreboardX = 0;
   private float scoreboardY = 0;
   private float scoreboardWidth = 0;
   private float scoreboardHeight = 0;
   private boolean hasScoreboardData = false;
   private int colorFrom = 0;
   private int colorTo = 0;
   
   public void setScoreboardBounds(float x, float y, float width, float height, int colorFrom, int colorTo) {
      this.scoreboardX = x;
      this.scoreboardY = y;
      this.scoreboardWidth = width;
      this.scoreboardHeight = height;
      this.colorFrom = colorFrom;
      this.colorTo = colorTo;
      this.hasScoreboardData = true;
   }
   
   public boolean shouldRenderRoundedBackground() {
      return this.isEnabled() && this.roundedCorner.getCurrentValue() && this.hasScoreboardData;
   }
   
   public void clearScoreboardData() {
      this.hasScoreboardData = false;
   }
   
   @EventTarget
   public void onShader(EventShader e) {
      if (!this.hasScoreboardData) {
         return;
      }
      
      if (e.getType() == EventType.BLUR && this.blur.getCurrentValue()) {
         float radius = this.roundedCorner.getCurrentValue() ? this.cornerRadius.getCurrentValue() : 0.0F;
         RenderUtils.drawRoundedRect(
            e.getStack(), 
            scoreboardX, 
            scoreboardY, 
            scoreboardWidth, 
            scoreboardHeight, 
            radius, 
            Integer.MIN_VALUE
         );
      }
   }
   
   public void renderRoundedBackground(com.mojang.blaze3d.vertex.PoseStack poseStack) {
      if (!this.hasScoreboardData || !this.roundedCorner.getCurrentValue()) {
         return;
      }
      
      float radius = this.cornerRadius.getCurrentValue();
      // 使用单一颜色绘制圆角矩形背景
      RenderUtils.drawRoundedRect(
         poseStack,
         scoreboardX,
         scoreboardY,
         scoreboardWidth,
         scoreboardHeight,
         radius,
         colorFrom
      );
   }
}
