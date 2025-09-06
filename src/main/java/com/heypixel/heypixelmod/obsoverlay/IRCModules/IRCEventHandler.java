package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventJoinWorld;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventLeaveWorld;

public class IRCEventHandler {
    
    @EventTarget(1)
    public void onPlayerJoinWorld(EventJoinWorld event) {
        if (event.getType() == (byte) EventType.POST.ordinal()) {
            // 玩家进入世界后连接IRC喵~
            IRC.getInstance().onPlayerJoinWorld();
        }
    }
    
    @EventTarget(1)
    public void onPlayerLeaveWorld(EventLeaveWorld event) {
        if (event.getType() == (byte) EventType.POST.ordinal()) {
            // 玩家离开世界时断开IRC连接喵~
            IRC.getInstance().onPlayerLeaveWorld();
        }
    }
}