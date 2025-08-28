package com.heypixel.heypixelmod.obsoverlay;

import by.radioegor146.nativeobfuscator.Native;
import com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager;

public class Version {
   @Native
   public static String getVersion() {
      String username = IRCLoginManager.getUsername();

      if ("Noa1337".equals(username)) {
         return "Developer Build";
      } else {
         return "Release 1.0";
      }
   }
}