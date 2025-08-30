package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NotificationSelect;
import com.heypixel.heypixelmod.obsoverlay.utils.AnimationUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public void addNotification(Notification notification) {
      if (!this.notifications.contains(notification)) {
         this.notifications.add(notification);
      }
   }


   public void addNotification(NotificationLevel level, String title, String message, long age) {
       NotificationSelect notificationSelect = (NotificationSelect) Naven.getInstance().getModuleManager().getModule(NotificationSelect.class);

       Notification notification;
       if (notificationSelect != null) {
           String selectedType = notificationSelect.getSelectedNotificationType();
           if ("SouthSide".equals(selectedType)) {
               notification = new SouthSideNotification(level, title, message, age);
           } else {

               boolean enabled = (level == NotificationLevel.SUCCESS || level == NotificationLevel.INFO);
               notification = new Notification(title + ": " + message, enabled);
           }
       } else {

           boolean enabled = (level == NotificationLevel.SUCCESS || level == NotificationLevel.INFO);
           notification = new Notification(title + ": " + message, enabled);
       }
       
      if (!this.notifications.contains(notification)) {
         this.notifications.add(notification);
      }
   }

public void onRenderShadow(EventShader e) {
   float height = 5.0F;
   for (Notification notification : this.notifications) {
      SmoothAnimationTimer widthTimer = notification.getWidthTimer();
      SmoothAnimationTimer heightTimer = notification.getHeightTimer();
      height += notification.getHeight() + 5.0F;
      Window window = Minecraft.getInstance().getWindow();
      

      widthTimer.speed = 1.5F; 
      heightTimer.speed = 1.5F;
      

      float x = (float)window.getGuiScaledWidth() - notification.getWidth() + widthTimer.value;
      

      if (notification instanceof SouthSideNotification) {
          ((SouthSideNotification) notification).render(e.getStack(), x, (float)window.getGuiScaledHeight() - height);
      } else {
          notification.renderShader(e.getStack(), x, (float)window.getGuiScaledHeight() - height);
      }
      

      widthTimer.update(true);
      heightTimer.update(true);
   }
}

public void onRender(EventRender2D e) {
   float height = 5.0F;

   for (Notification notification : this.notifications) {
      e.getStack().pushPose();
      float width = notification.getWidth();
      height += notification.getHeight() + 5.0F;
      SmoothAnimationTimer widthTimer = notification.getWidthTimer();
      SmoothAnimationTimer heightTimer = notification.getHeightTimer();
      float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
      

      widthTimer.speed = 1.5F; 
      heightTimer.speed = 1.5F;
      
      if (lifeTime > (float)notification.getMaxAge()) {

         widthTimer.target = width + 20.0F;
         heightTimer.target = 0.0F;
         if (widthTimer.isAnimationDone(true)) {
            this.notifications.remove(notification);
         }
      } else {

         widthTimer.target = 0.0F;
         heightTimer.target = 0.0F;
      }

      widthTimer.update(true);
      heightTimer.update(true);
      Window window = Minecraft.getInstance().getWindow();
      

      float x = (float)window.getGuiScaledWidth() - width + widthTimer.value;
      

      if (notification instanceof SouthSideNotification) {
          ((SouthSideNotification) notification).render(e.getStack(), x, (float)window.getGuiScaledHeight() - height);
      } else {
          notification.renderShader(e.getStack(), x, (float)window.getGuiScaledHeight() - height);
      }
      
      e.getStack().popPose();
   }
}

   private float easeInOutQuad(float t) {
       return t < 0.5 ? 2 * t * t : (float) (1 - Math.pow(-2 * t + 2, 2) / 2);
   }
}