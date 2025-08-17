package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

@ModuleInfo(
        name = "TNTWarning",
        description = "Displays countdown timer on primed TNT",
        category = Category.MISC
)
public class TNTWarning extends Module {

    @EventTarget
    public void onRender(EventRender2D event) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof PrimedTnt tntEntity) {

                int fuse = tntEntity.getFuse();


                Vec3 tntPos = tntEntity.position();
                tntPos = tntPos.add(0, tntEntity.getBoundingBox().getYsize() + 0.5, 0);

                Vector2f screenPos = ProjectionUtils.project(tntPos.x, tntPos.y, tntPos.z, 1.0F);
                if (screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {
                    String text = String.format("%.1f", fuse / 20.0f);
                    float x = screenPos.x - Fonts.harmony.getWidth(text, 0.5) / 2;
                    float y = screenPos.y;

                    Color color = Color.RED;
                    if (fuse > 40) { // 大于2秒
                        color = Color.YELLOW;
                    } else if (fuse > 20) { // 大于1秒
                        color = Color.ORANGE;
                    }

                    Fonts.harmony.render(event.getStack(), text, x, y, color, true, 0.5);
                }
            }
        }
    }
}