package com.heypixel.heypixelmod.obsoverlay;

import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;

public class Version {
    public static String getVersion() {
        String username = IRCLoginManager.getUsername();
            return "Release 1.2-Pre1.5";
        }
    }