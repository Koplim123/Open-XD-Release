package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

@ModuleInfo(
    name = "Notification Style",
    description = "Select notification style.",
    category = Category.RENDER
)
public class NotificationSelect extends Module {
    private final ModeValue notificationType = new ModeValue(
        this, 
        "Style",
        new String[]{"Normal", "SouthSide"},
        0, 
        null, 
        null
    );
    
    public NotificationSelect() {
        super();
        this.setCannotDisable(true); // 默认开启且无法关闭
    }
    
    public String getSelectedNotificationType() {
        return notificationType.getCurrentMode();
    }
}