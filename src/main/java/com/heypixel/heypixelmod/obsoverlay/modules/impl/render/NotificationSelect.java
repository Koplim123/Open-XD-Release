package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.Naven;

@ModuleInfo(
    name = "NotificationSelect",
    description = "Select notification type.",
    category = Category.RENDER
)
public class NotificationSelect extends Module {
    public final ModeValue notificationType = new ModeValue(
        this, 
        "NotificationType",
        new String[]{"Normal", "SouthSide"},
        0, 
        null, 
        null
    );
    
    public NotificationSelect() {
        super();
        this.setCannotDisable(true); // 默认开启且无法关闭
        Naven.getInstance().getValueManager().addValue(notificationType);
    }
    
    public String getSelectedNotificationType() {
        return notificationType.getCurrentMode();
    }
}