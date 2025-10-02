package com.heypixel.heypixelmod.obsoverlay.IRCModules;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class IRCTabDecorator {
    private static final IRCTabDecorator INSTANCE = new IRCTabDecorator();
    
    private IRCTabDecorator() {}
    
    public static IRCTabDecorator getInstance() {
        return INSTANCE;
    }
    
    @EventTarget
    public void onRenderTab(EventRenderTabOverlay event) {
        if (event.getType() != com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType.NAME) {
            return;
        }
        
        String displayName = event.getComponent().getString();
        String mcName = extractMcName(displayName);
        
        if (mcName != null && !mcName.isEmpty()) {
            IRCPlayerManager.IRCPlayer ircPlayer = IRCPlayerManager.getInstance().getPlayer(mcName);
            if (ircPlayer != null) {
                String prefix = ircPlayer.getTabPrefix();
                String newDisplayName = prefix + displayName;
                event.setComponent(Component.literal(newDisplayName));
            }
        }
    }
    
    private String extractMcName(String displayName) {
        if (displayName == null) {
            return null;
        }
        
        displayName = displayName.replaceAll("§.", "");
        displayName = displayName.replaceAll("\\[.*?\\]", "").trim();
        
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getConnection() != null) {
            for (PlayerInfo playerInfo : mc.getConnection().getOnlinePlayers()) {
                String playerName = playerInfo.getProfile().getName();
                if (displayName.contains(playerName)) {
                    return playerName;
                }
            }
        }
        
        return displayName;
    }
}

