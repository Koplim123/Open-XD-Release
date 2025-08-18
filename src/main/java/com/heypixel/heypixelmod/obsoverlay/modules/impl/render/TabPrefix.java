package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@ModuleInfo(
        name = "TabPrefix",
        description = "在Tab列表中显示Naven-XD前缀和Rank信息",
        category = Category.RENDER
)
public class TabPrefix extends Module {
    
    public TabPrefix() {
        // 设置模块为不可禁用
        this.setCannotDisable(true);
    }

    @EventTarget
    public void onRenderTab(EventRenderTabOverlay e) {
        // 只处理名字类型事件，并确保是玩家自己
        if (e.getType() == com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType.NAME && 
            mc.player != null && 
            e.getComponent().getString().equals(mc.player.getName().getString())) {
            
            // 构建前缀: [§aNaven-XD§r] [Rank] 玩家名
            MutableComponent newComponent = Component.literal("[§aNaven-XD§r] ");
            
            // 添加Rank信息（如果存在）
            if (IRCLoginManager.rank != null && !IRCLoginManager.rank.isEmpty()) {
                newComponent = newComponent.append("[" + IRCLoginManager.rank + "] ");
            }
            
            // 添加原始玩家名
            newComponent = newComponent.append(e.getComponent());
            
            // 设置修改后的组件
            e.setComponent(newComponent);
        }
    }
}