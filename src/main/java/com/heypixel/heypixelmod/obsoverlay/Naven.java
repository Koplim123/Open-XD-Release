package com.heypixel.heypixelmod.obsoverlay;

import com.google.gson.JsonObject;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandManager;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventManager;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShutdown;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.IRCModules.AutoConnectListener;
import com.heypixel.heypixelmod.obsoverlay.IRCModules.ConnectAndReveives;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.obsoverlay.ui.IRCLoginScreen;
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
   private static boolean ircScreenDisplayed = false; // 添加一个标志，防止重复显示
   private static ConnectAndReveives ircClient; // IRC客户端实例
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
      this.fileManager.load();
      
      // 在加载配置文件后加载用户选择的字体
      LoadFontOnStart.loadUserSelectedFont();
      
      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(new AutoConnectListener()); // 注册IRC自动连接监听器
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
      if (!ircScreenDisplayed && Minecraft.getInstance().screen instanceof TitleScreen) {
         if (com.heypixel.heypixelmod.obsoverlay.utils.IRCLoginManager.userId == -1) {
            Minecraft.getInstance().setScreen(new IRCLoginScreen());
            ircScreenDisplayed = true;
         } else if (!ircConnected) {
            // 玩家已登录IRC但未连接IRC服务器，自动连接
            connectToIRC();
            ircConnected = true;
         }
      }
   }
   
   /**
    * 连接到IRC服务器喵~
    */
   private static void connectToIRC() {
      try {
         System.out.println("正在自动连接到IRC服务器... 喵~");
         ircClient = new ConnectAndReveives();
         
         // 设置消息处理器
         ircClient.setMessageHandler(new ConnectAndReveives.MessageHandler() {
            @Override
            public void onMessage(String type, JsonObject data) {
               // 处理接收到的消息
               System.out.println("收到IRC消息: " + type + " - " + data + " 喵~");
            }
            
            @Override
            public void onConnected() {
               System.out.println("IRC连接成功喵~");
               // 连接成功后进行认证
               ircClient.authenticate();
            }
            
            @Override
            public void onDisconnected() {
               System.out.println("IRC连接断开喵~");
               ircConnected = false;
            }
            
            @Override
            public void onError(String error) {
               System.err.println("IRC连接错误: " + error + " 喵~");
               ircConnected = false;
            }
         });
         
         // 连接到服务器
         ircClient.connect();
         
      } catch (Exception e) {
         System.err.println("连接IRC服务器时出错: " + e.getMessage() + " 喵~");
         ircConnected = false;
      }
   }
   
   /**
    * 获取IRC客户端实例喵~
    */
   public static ConnectAndReveives getIrcClient() {
      return ircClient;
   }
   
   /**
    * 检查IRC是否已连接喵~
    */
   public static boolean isIrcConnected() {
      return ircConnected && ircClient != null;
   }

   @EventTarget
   public void onShutdown(EventShutdown e) {
      // 关闭时断开IRC连接
      if (ircClient != null) {
         ircClient.disconnect();
         System.out.println("IRC连接已关闭喵~");
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