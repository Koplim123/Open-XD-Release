package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.FontLoader;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

@ModuleInfo(
        name = "FontSelect",
        description = "Select custom font for UI rendering",
        category = Category.MISC
)
public class FontSelect extends Module {


    private final ModeValue fontOption = ValueBuilder.create(this, "Font")
            .setModes(FontLoader.getAvailableFonts())
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();
    
    private final BooleanValue otherCJKFontsRender = ValueBuilder.create(this, "OtherCJKFontsRender")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    
    private final ModeValue cjkFontOption = ValueBuilder.create(this, "CJK Font")
            .setModes(FontLoader.getAvailableFonts())
            .setDefaultModeIndex(0)
            .setVisibility(() -> otherCJKFontsRender.getCurrentValue())
            .build()
            .getModeValue();

    public FontSelect() {
        super("FontSelect", "Select custom font for UI rendering", Category.MISC);

        if (this.isEnabled()) {
            updateFont();
        }
    }
    @Override
    public void onEnable() {
        updateFont();
    }
    @Override
    public void onDisable() {
        try {
            Fonts.reloadFonts("opensans");
        } catch (Exception e) {
            System.err.println("Error resetting fonts to default");
            e.printStackTrace();
        }
    }
    public void onValueChange() {
        if (this.isEnabled()) {
            updateFont();
        }
    }
    
    // 将此方法改为public以便从外部调用
    public void updateFont() {
        try {
            if (fontOption != null) {
                String selectedFont = fontOption.getCurrentMode();
                if (selectedFont != null && !selectedFont.isEmpty()) {

                    if (isChineseFont(selectedFont)) {
                        Fonts.reloadFonts(selectedFont);
                        if (Fonts.harmony != null) {
                            Fonts.harmony = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer(
                                selectedFont, 32, 0, 65535, 16384);
                        }
                    } else {
                        Fonts.reloadFonts(selectedFont);
                    }
                } else {
                    Fonts.reloadFonts("opensans");
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating font");
            e.printStackTrace();

            try {
                Fonts.reloadFonts("opensans");
            } catch (Exception ex) {
                System.err.println("Error resetting to default font");
                ex.printStackTrace();
            }
        }
    }
    
    private boolean isChineseFont(String fontName) {
        // 如果开启了OtherCJKFontsRender选项，则使用CJK字体选项的值
        if (otherCJKFontsRender.getCurrentValue() && cjkFontOption != null) {
            return cjkFontOption.getCurrentMode().equals(fontName);
        }
        
        // 否则检查是否是中文字体（基于字体名称）
        return fontName.contains("中") || fontName.contains("汉") || 
               "HYWenHei 85W".equals(fontName) || "harmony".equals(fontName);
    }
    
    public String getSelectedFont() {
        if (fontOption != null) {
            return fontOption.getCurrentMode();
        }
        return "opensans";
    }
    
    public boolean isOtherCJKFontsRenderEnabled() {
        return otherCJKFontsRender.getCurrentValue();
    }
    
    public String getSelectedCJKFont() {
        if (cjkFontOption != null) {
            return cjkFontOption.getCurrentMode();
        }
        // 默认使用主字体选项中的字体
        return getSelectedFont();
    }
}