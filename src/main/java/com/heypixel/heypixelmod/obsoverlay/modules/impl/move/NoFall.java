package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
// 写法是对的为什么无法使用啊// 写法是对的为什么无法使用啊
// 写法是对的为什么无法使用啊// 写法是对的为什么无法使用啊
// 写法是对的为什么无法使用啊// 写法是对的为什么无法使用啊
// 写法是对的为什么无法使用啊// 写法是对的为什么无法使用啊
// 写法是对的为什么无法使用啊// 写法是对的为什么无法使用啊


@ModuleInfo(
        name = "摔落无伤",
        description = "写法是对的为什么无法使用啊",
        category = Category.MOVEMENT
)
public class NoFall extends Module {
    private boolean lastGround = false;
    private float lastFallDis = 0F;

    @Override
    public void onEnable() {
        lastGround = false;
        lastFallDis = 0F;
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() == EventType.SEND && !e.isCancelled()) {
            if (e.getPacket() instanceof ServerboundMovePlayerPacket packet) {
                // 获取原始数据包属性
                double x = packet.getX(0.0);
                double y = packet.getY(0.0);
                double z = packet.getZ(0.0);
                float yRot = packet.getYRot(0.0F);
                float xRot = packet.getXRot(0.0F);
                boolean onGround = packet.isOnGround();

                // 检测反作弊触发条件
                if (onGround && !lastGround && lastFallDis >= 2.5F) {
                    // 创建修改后的数据包（完全按照原始逻辑）
                    ServerboundMovePlayerPacket modifiedPacket = new ServerboundMovePlayerPacket.PosRot(
                            x + 1000.0,  // X 坐标偏移
                            y,
                            z + 1337.0,  // Z 坐标偏移
                            yRot,
                            xRot,
                            false        // 取消地面状态
                    );

                    // 替换原始数据包
                    e.setCancelled(true);
                    e.setPacket(modifiedPacket);

                    // 触发着陆效果（1.20.1 等效方法）
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.resetFallDistance();
                    }
                }

                // 更新状态记录（完全按照原始逻辑）
                lastGround = onGround;
                if (Minecraft.getInstance().player != null) {
                    lastFallDis = Minecraft.getInstance().player.fallDistance;
                }
            }
        }
    }
}