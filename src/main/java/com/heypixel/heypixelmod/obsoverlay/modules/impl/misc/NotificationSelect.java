package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationMode;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

@ModuleInfo(
        name = "NotificationSelect",
        description = "Select Notification Style",
        category = Category.MISC
)
public class NotificationSelect extends Module {
    
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("SouthSide", "Naven")
            .setDefaultModeIndex(0) // 默认SouthSide
            .setOnUpdate(this::onModeChange)
            .build()
            .getModeValue();

    @Override
    public void onEnable() {
        updateNotificationMode();
        // 设置模块后缀显示当前模式
        setSuffix(getCurrentMode());
    }

    @Override
    public void onDisable() {
        // 禁用时恢复到默认SouthSide模式
        NotificationMode.setCurrentMode(NotificationMode.SOUTHSIDE);
        setSuffix(null);
    }

    private void onModeChange(Value value) {
        updateNotificationMode();
        setSuffix(getCurrentMode());
    }

    private void updateNotificationMode() {
        String currentMode = mode.getCurrentMode();
        switch (currentMode) {
            case "Naven":
                NotificationMode.setCurrentMode(NotificationMode.NAVEN);
                break;
            case "SouthSide":
            default:
                NotificationMode.setCurrentMode(NotificationMode.SOUTHSIDE);
                break;
        }
    }

    public String getCurrentMode() {
        return mode.getCurrentMode();
    }
}
