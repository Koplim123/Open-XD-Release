package com.heypixel.heypixelmod.obsoverlay;

import com.google.gson.JsonObject;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandManager;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventManager;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShutdown;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.obsoverlay.ui.Welcome;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.PostProcessRenderer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Shaders;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.HasValueManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.awt.*;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Naven {
   public static final String CLIENT_NAME = "Naven-XD";
   public static final String CLIENT_DISPLAY_NAME = "Naven";
   private static Naven instance;
   private final EventManager eventManager;
   private final EventWrapper eventWrapper;
   private final ValueManager valueManager;
   private final HasValueManager hasValueManager;
   private final RotationManager rotationManager;
   public final ModuleManager moduleManager;
   private final CommandManager commandManager;
   private final FileManager fileManager;
   private final NotificationManager notificationManager;
   public static float TICK_TIMER = 1.0F;
   public static Queue<Runnable> skipTasks = new ConcurrentLinkedQueue<>();
   private static boolean ircScreenDisplayed = false; 
   private static IRCConnection ircClient; // IRC客户端实例
   private static boolean ircConnected = false; // IRC连接状态标志

   private Naven() {
      System.out.println("Naven Init");
      instance = this;
      this.eventManager = new EventManager();
      Shaders.init();
      PostProcessRenderer.init();

      try {
         Fonts.loadFonts();
         // 确保图标字体被正确初始化
         if (Fonts.icons == null) {
             System.err.println("Icons font failed to load, attempting to reload");
             Fonts.icons = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer("PublicSans-Bold", 32, 59648, 59652, 512);
         }
      } catch (IOException var2) {
         System.err.println("Failed to load fonts due to IOException");
         var2.printStackTrace();
         // 确保即使出错也有默认字体
         ensureFontsLoaded();
      } catch (FontFormatException var3) {
         System.err.println("Failed to load fonts due to FontFormatException");
         var3.printStackTrace();
         // 确保即使出错也有默认字体
         ensureFontsLoaded();
      } catch (Exception var4) {
         System.err.println("Failed to load fonts due to unexpected error");
         var4.printStackTrace();
         // 确保即使出错也有默认字体
         ensureFontsLoaded();
      }

      // 验证字体是否正确加载
      if (Fonts.opensans == null) {
          System.err.println("Warning: opensans font is null after initialization");
      }
      
      if (Fonts.harmony == null) {
          System.err.println("Warning: harmony font is null after initialization");
      }
      
      if (Fonts.icons == null) {
          System.err.println("Warning: icons font is null after initialization");
      }
      this.eventWrapper = new EventWrapper();
      this.valueManager = new ValueManager();
      this.hasValueManager = new HasValueManager();
      this.moduleManager = new ModuleManager();
      this.rotationManager = new RotationManager();
      this.commandManager = new CommandManager();
      this.fileManager = new FileManager();
      this.notificationManager = new NotificationManager();
      
      com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor.getInstance();
      this.fileManager.load();
      
      LoadFontOnStart.loadUserSelectedFont();
      
      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(new AutoConnectListener());
      // this.eventManager.register(com.heypixel.heypixelmod.obsoverlay.IRCModules.IRCTabDecorator.getInstance()); // 注释掉不存在的类
      this.eventManager.register(new ServerUtils());
      this.eventManager.register(new EntityWatcher());
      this.eventManager.register(this.notificationManager);
      MinecraftForge.EVENT_BUS.register(this.eventWrapper);
      MinecraftForge.EVENT_BUS.register(this);
   }

   public static void modRegister() {
      try {
         new Naven();
      } catch (Exception var1) {
         System.err.println("Failed to load client");
         var1.printStackTrace(System.err);
      }
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      // 直接进入Welcome界面，不再显示IRC登录界面
      if (!ircScreenDisplayed && Minecraft.getInstance().screen instanceof TitleScreen) {
         Minecraft.getInstance().setScreen(new Welcome());
         ircScreenDisplayed = true;
      }
   }

   /**
    * 登录后连接到IRC服务器
    * 这个方法应该在登录成功后调用
    */
   public static void connectToIRCAfterLogin() {
      if (!ircConnected && ircClient == null) {
         new Thread(() -> {
            try {
               Thread.sleep(1000); // 等待1秒，确保登录状态已保存
               connectToIRC();
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }).start();
      }
   }

   private static void connectToIRC() {
      try {
         System.out.println("正在自动连接到IRC服务器...");
         com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§e[IRC] 正在连接到服务器...");
         ircClient = new IRCConnection();
         
         // 设置消息处理器
         ircClient.setMessageHandler(new IRCConnection.MessageHandler() {
            @Override
            public void onMessage(String type, JsonObject data) {
               // 处理接收到的消息
               handleIRCMessage(type, data);
            }
            
            @Override
            public void onConnected() {
               ircConnected = true;
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§a[IRC] 连接成功");
            }
            
            @Override
            public void onDisconnected() {
               ircConnected = false;
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§c[IRC] 连接断开");
            }
            
            @Override
            public void onError(String error) {
               ircConnected = false;
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§c[IRC] 错误: " + error);
            }
         });
         
         // 连接到服务器
         ircClient.connect();
         
      } catch (Exception e) {
         System.err.println("连接IRC服务器时出错: " + e.getMessage());
         com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§c[IRC] 连接失败: " + e.getMessage());
         ircConnected = false;
      }
   }
   
   private static void handleIRCMessage(String type, JsonObject data) {
      try {
         switch (type) {
            case "auth_success":
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§a[IRC] 认证成功");
               break;
               
            case "auth_failed":
               String errorMsg = data.has("message") ? data.get("message").getAsString() : "认证失败";
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§c[IRC] 认证失败: " + errorMsg);
               break;
               
            case "welcome":
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§a[IRC] 欢迎来到IRC服务器");
               break;
               
            case "user_joined":
               String username = data.get("username").getAsString();
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§e[IRC] 用户 " + username + " 加入了聊天");
               break;
               
            case "user_left":
               String leftUser = data.get("username").getAsString();
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§e[IRC] 用户 " + leftUser + " 离开了聊天");
               break;
               
            case "message":
               String sender = data.get("username").getAsString();
               String message = data.get("message").getAsString();
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§7[IRC] §b" + sender + "§7: §f" + message);
               break;
               
            case "private_message":
               String fromUser = data.get("from").getAsString();
               String privateMsg = data.get("message").getAsString();
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§d[IRC-私聊] §b" + fromUser + "§d: §f" + privateMsg);
               break;
               
            case "private_message_sent":
               String toUser = data.get("to").getAsString();
               String sentMsg = data.get("message").getAsString();
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§d[IRC] 私聊消息已发送给 " + toUser + ": " + sentMsg);
               break;
               
            case "minecraft_command":
               String commandUser = data.get("username").getAsString();
               String command = data.get("command").getAsString();
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§6[IRC-命令] §b" + commandUser + "§6: §f" + command);
               break;
               
            case "user_list":
               if (data.has("users")) {
                  com.google.gson.JsonArray users = data.getAsJsonArray("users");
                  com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§a[IRC] 在线用户列表:");
                  for (int i = 0; i < users.size(); i++) {
                     String user = users.get(i).getAsString();
                     com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§7  - §b" + user);
                  }
                  com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§a[IRC] 总计 " + users.size() + " 名用户在线");
               }
               break;
               
            case "error":
               String error = data.has("message") ? data.get("message").getAsString() : "未知错误";
               com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils.addChatMessage("§c[IRC] 服务器错误: " + error);
               break;
         }
      } catch (Exception e) {
         System.err.println("处理IRC消息时出错: " + e.getMessage());
         e.printStackTrace();
      }
   }

   public static IRCConnection getIrcClient() {
      return ircClient;
   }
   

   public static boolean isIrcConnected() {
      return ircConnected && ircClient != null;
   }

   @EventTarget
   public void onShutdown(EventShutdown e) {
      // 关闭时断开IRC连接
      if (ircClient != null) {
         ircClient.disconnect();
         System.out.println("IRC连接已关闭");
      }
      this.fileManager.save();
      LogUtils.close();
   }

   @EventTarget(0)
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE) {
         TickTimeHelper.update();
      }
   }

   public static Naven getInstance() {
      return instance;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public EventWrapper getEventWrapper() {
      return this.eventWrapper;
   }

   public ValueManager getValueManager() {
      return this.valueManager;
   }

   public HasValueManager getHasValueManager() {
      return this.hasValueManager;
   }

   public RotationManager getRotationManager() {
      return this.rotationManager;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public FileManager getFileManager() {
      return this.fileManager;
   }

   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }

    /**
     * 确保字体被正确加载的备用方法
     */
    private void ensureFontsLoaded() {
        try {
            if (Fonts.opensans == null) {
                Fonts.opensans = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer("opensans", 32, 0, 255, 512);
            }
            
            if (Fonts.harmony == null) {
                Fonts.harmony = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer("harmony", 32, 0, 65535, 16384);
            }
            
            if (Fonts.icons == null) {
                Fonts.icons = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer("PublicSans-Bold", 32, 59648, 59652, 512);
            }
        } catch (Exception e) {
            System.err.println("Failed to load fallback fonts");
            e.printStackTrace();
        }
    }
}