package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.ui.Watermark.Watermark;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector4f;

@ModuleInfo(
        name = "HUD",
        description = "Displays information on your screen",
        category = Category.RENDER
)
public class HUD extends Module {
    public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
    public static final int backgroundColor = new Color(0, 0, 0, 40).getRGB();
    public BooleanValue waterMark = ValueBuilder.create(this, "Water Mark").setDefaultBooleanValue(true).build().getBooleanValue();

    public ModeValue watermarkStyle = ValueBuilder.create(this, "Watermark Style")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Rainbow", "Classic", "Capsule")
            .build()
            .getModeValue();

    public FloatValue watermarkSize = ValueBuilder.create(this, "Watermark Size")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public FloatValue watermarkCornerRadius = ValueBuilder.create(this, "Watermark Corner Radius")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    // 新增：水印垂直内边距调整
    public FloatValue watermarkVPadding = ValueBuilder.create(this, "Watermark V-Padding")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultFloatValue(4.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue notification = ValueBuilder.create(this, "Notification").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue arrayList = ValueBuilder.create(this, "Array List").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue prettyModuleName = ValueBuilder.create(this, "Pretty Module Name")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue hideRenderModules = ValueBuilder.create(this, "Hide Render Modules")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue rainbow = ValueBuilder.create(this, "Rainbow")
            .setDefaultBooleanValue(true)
            .setVisibility(this.arrayList::getCurrentValue)
            .build()
            .getBooleanValue();
    public FloatValue rainbowSpeed = ValueBuilder.create(this, "Rainbow Speed")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue rainbowOffset = ValueBuilder.create(this, "Rainbow Offset")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public ModeValue arrayListDirection = ValueBuilder.create(this, "ArrayList Direction")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Right", "Left")
            .build()
            .getModeValue();
    public FloatValue xOffset = ValueBuilder.create(this, "X Offset")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(-100.0F)
            .setMaxFloatValue(100.0F)
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue yOffset = ValueBuilder.create(this, "Y Offset")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(100.0F)
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue arrayListSize = ValueBuilder.create(this, "ArrayList Size")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    // 新增：ArrayList中模块之间的垂直间距
    public FloatValue arrayListSpacing = ValueBuilder.create(this, "ArrayList Spacing")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(2.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    List<Module> renderModules;
    List<Vector4f> blurMatrices = new ArrayList<>();

    public String getModuleDisplayName(Module module) {
        String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }

    @EventTarget
    public void notification(EventRender2D e) {
        if (this.notification.getCurrentValue()) {
            Naven.getInstance().getNotificationManager().onRender(e);
        }
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (this.notification.getCurrentValue() && e.getType() == EventType.SHADOW) {
            Naven.getInstance().getNotificationManager().onRenderShadow(e);
        }

        if (this.waterMark.getCurrentValue()) {
            Watermark.onShader(e, this.watermarkStyle.getCurrentMode(), this.watermarkCornerRadius.getCurrentValue(), this.watermarkSize.getCurrentValue(), this.watermarkVPadding.getCurrentValue());
        }

        // 仅在 BLUR 通道为ArrayList背景板写入模糊蒙版
        if (this.arrayList.getCurrentValue() && e.getType() == EventType.BLUR) {
            for (Vector4f blurMatrix : this.blurMatrices) {
                RenderUtils.drawRoundedRect(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 3.0F, Integer.MIN_VALUE);
            }
        }
    }

    /**
     * 绘制一个垂直的、颜色渐变的动态彩虹条，用于模块列表的装饰胶囊。
     * 颜色基于其Y坐标，以实现模块间的平滑过渡。
     */
    private void drawVerticalAnimatedRainbowBar(com.mojang.blaze3d.vertex.PoseStack stack, float x, float y, float width, float height, float rainbowSpeed, float rainbowOffset) {
        // 分段绘制，降低 draw call 与循环次数
        int segments = Math.max(4, Math.min(12, (int)(height / 2.0F)));
        float segmentHeight = height / (float)segments;
        for (int s = 0; s < segments; s++) {
            float segY0 = y + s * segmentHeight;
            float segY1 = (s == segments - 1) ? (y + height) : (segY0 + segmentHeight);
            float sampleY = (segY0 + segY1) * 0.5F;
            int color = RenderUtils.getRainbowOpaque(
                    (int)(-sampleY * rainbowOffset),
                    1.0F, 1.0F, (21.0F - rainbowSpeed) * 1000.0F
            );
            RenderUtils.fill(stack, x, segY0, x + width, segY1, color);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (this.waterMark.getCurrentValue()) {
            // 传递彩虹效果和新的padding相关参数到Watermark
            Watermark.onRender(e, this.watermarkSize.getCurrentValue(), this.watermarkStyle.getCurrentMode(), this.rainbow.getCurrentValue(), this.rainbowSpeed.getCurrentValue(), this.rainbowOffset.getCurrentValue(), this.watermarkCornerRadius.getCurrentValue(), this.watermarkVPadding.getCurrentValue());
        }

        this.blurMatrices.clear();
        if (this.arrayList.getCurrentValue()) {
            CustomTextRenderer font = Fonts.opensans;
            e.getStack().pushPose();
            ModuleManager moduleManager = Naven.getInstance().getModuleManager();
            if (update || this.renderModules == null) {
                this.renderModules = new ArrayList<>(moduleManager.getModules());
                if (this.hideRenderModules.getCurrentValue()) {
                    this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
                }

                this.renderModules.sort((o1, o2) -> {
                    float o1Width = font.getWidth(this.getModuleDisplayName(o1), (double)this.arrayListSize.getCurrentValue());
                    float o2Width = font.getWidth(this.getModuleDisplayName(o2), (double)this.arrayListSize.getCurrentValue());
                    return Float.compare(o2Width, o1Width);
                });
            }

            // 计算最大可能的宽度（包括所有模块，无论是否启用）
            float maxWidth = 0.0F;
            for (Module module : this.renderModules) {
                float moduleWidth = font.getWidth(this.getModuleDisplayName(module), (double)this.arrayListSize.getCurrentValue());
                if (moduleWidth > maxWidth) {
                    maxWidth = moduleWidth;
                }
            }

            if (maxWidth < 50.0F) {
                maxWidth = 100.0F;
            }

            com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor.HUDElement arrayListElement =
                    com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor.getInstance().getHUDElement("arraylist");

            float arrayListX, arrayListY;
            if (arrayListElement != null) {
                if (this.arrayListDirection.isCurrentMode("Right")) {
                    arrayListX = (float)arrayListElement.x;
                } else {
                    arrayListX = (float)arrayListElement.x;
                }
                arrayListY = (float)arrayListElement.y;
            } else {
                arrayListX = this.arrayListDirection.isCurrentMode("Right")
                        ? (float)mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F + this.xOffset.getCurrentValue()
                        : 3.0F + this.xOffset.getCurrentValue();
                arrayListY = this.yOffset.getCurrentValue();
            }
            float height = 0.0F;
            double fontHeight = font.getHeight(true, (double)this.arrayListSize.getCurrentValue());

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            for (Module module : this.renderModules) {
                SmoothAnimationTimer animation = module.getAnimation();
                if (module.isEnabled()) {
                    animation.target = 100.0F;
                } else {
                    animation.target = 0.0F;
                }

                animation.update(true);
                if (animation.value > 0.0F) {
                    String displayName = this.getModuleDisplayName(module);
                    float stringWidth = font.getWidth(displayName, (double)this.arrayListSize.getCurrentValue());
                    float left = -stringWidth * (1.0F - animation.value / 100.0F);
                    float right = maxWidth - stringWidth * (animation.value / 100.0F);
                    float innerX = this.arrayListDirection.isCurrentMode("Left") ? left : right;
                    float moduleHeight = (float)((double)(animation.value / 100.0F) * fontHeight);
                    float moduleX = arrayListX + innerX;
                    float moduleY = arrayListY + height + 2.0F;
                    float moduleWidth = stringWidth + 3.0F;

                    // 步骤 1: 绘制模块的深色背景
                    RenderUtils.drawRoundedRect(
                            e.getStack(),
                            moduleX,
                            moduleY,
                            moduleWidth,
                            moduleHeight,
                            3.0F,
                            backgroundColor
                    );
                    this.blurMatrices.add(new Vector4f(moduleX, moduleY, moduleWidth, moduleHeight));

                    // 步骤 2: 绘制模块名称文本
                    int color = -1; // 默认白色
                    if (this.rainbow.getCurrentValue()) {
                        // 如果彩虹效果开启，文本也使用彩虹色
                        color = RenderUtils.getRainbowOpaque(
                                (int)(-height * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                        );
                    }

                    float alpha = animation.value / 100.0F;
                    font.setAlpha(alpha);
                    font.render(
                            e.getStack(),
                            displayName,
                            (double)(moduleX + 1.5F),
                            (double)(arrayListY + height + 1.0F),
                            new Color(color),
                            true,
                            (double)this.arrayListSize.getCurrentValue()
                    );

                    // 步骤 3: 彩虹装饰条（低开销版本，避免频繁模板切换与逐像素填充）
                    if (this.rainbow.getCurrentValue()) {
                        float capsuleWidth = 2.0f;
                        float capsulePadding = 1.5f;
                        float capsuleX = this.arrayListDirection.isCurrentMode("Left")
                                ? (moduleX - capsuleWidth - capsulePadding)
                                : (moduleX + moduleWidth + capsulePadding);
                        int barColor = RenderUtils.getRainbowOpaque(
                                (int)(-moduleY * this.rainbowOffset.getCurrentValue()),
                                1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                        );
                        RenderUtils.fill(e.getStack(), capsuleX, moduleY, capsuleX + capsuleWidth, moduleY + moduleHeight, barColor);
                    }

                    // 使用 arrayListSpacing 调整模块之间的垂直间距
                    height += (float)((double)(animation.value / 100.0F) * (fontHeight + this.arrayListSpacing.getCurrentValue()));
                }
            }

            if (arrayListElement != null) {
                arrayListElement.width = Math.max(maxWidth, 100.0F);
                arrayListElement.height = Math.max(height, 50.0F);
            }

            font.setAlpha(1.0F);
            e.getStack().popPose();
        }
    }
}