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

    private final BooleanValue otherCJKFontsRender = 
            ValueBuilder.create(this, "OtherCJKFontsRender")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final ModeValue cjkFontOption = ValueBuilder.create(this, "CJK Font")
            .setVisibility(otherCJKFontsRender::getCurrentValue)
            .setModes(FontLoader.getAvailableFonts())
            .setDefaultModeIndex(0)
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

            // 处理CJK字体渲染选项
            if (otherCJKFontsRender != null && otherCJKFontsRender.getCurrentValue()) {
                if (cjkFontOption != null) {
                    String selectedCJKFont = cjkFontOption.getCurrentMode();
                    if (selectedCJKFont != null && !selectedCJKFont.isEmpty()) {
                        // 使用用户选择的CJK字体来重新加载harmony字体
                        if (Fonts.harmony != null) {
                            Fonts.harmony = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer(
                                    selectedCJKFont, 32, 0, 65535, 16384);
                        }
                    }
                }
            } else {
                // 如果没有开启OtherCJKFontsRender，使用默认的HYWenHei 85W字体
                if (Fonts.harmony != null && !isChineseFont(fontOption.getCurrentMode())) {
                    Fonts.harmony = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer(
                            "HYWenHei 85W", 32, 0, 65535, 16384);
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

        return "HYWenHei 85W".equals(fontName) || "harmony".equals(fontName) || fontName.contains("中") || fontName.contains("汉");
    }
    public String getSelectedFont() {
        if (fontOption != null) {
            return fontOption.getCurrentMode();
        }
        return "opensans";
    }

    public String getSelectedCJKFont() {
        if (otherCJKFontsRender != null && otherCJKFontsRender.getCurrentValue()) {
            if (cjkFontOption != null) {
                return cjkFontOption.getCurrentMode();
            }
        }
        return "HYWenHei 85W";
    }

    public boolean isOtherCJKFontsRenderEnabled() {
        return otherCJKFontsRender != null && otherCJKFontsRender.getCurrentValue();
    }
}