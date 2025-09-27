package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import net.minecraft.client.Minecraft;

public class SetTitle {
    private static boolean registered = false;
    private static String lastTitle = null;

    public static void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        String version = Version.getVersion();
        String username = IRCLoginManager.getUsername();

        if (username == null || username.isEmpty()) {
            System.exit(0);
            return;
        }

        String title = "Naven-XD | " + version + " | Welcome " + username;
        if (!title.equals(lastTitle)) {
            mc.getWindow().setTitle(title);
            lastTitle = title;
        }
    }
}