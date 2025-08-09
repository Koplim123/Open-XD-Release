package com.heypixel.heypixelmod.obsoverlay.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

   public static int reAlpha(int color, float alpha) {
      int col = MathUtils.clamp((int)(alpha * 255.0F), 0, 255) << 24;
      col |= MathUtils.clamp(color >> 16 & 0xFF, 0, 255) << 16;
      col |= MathUtils.clamp(color >> 8 & 0xFF, 0, 255) << 8;
      return col | MathUtils.clamp(color & 0xFF, 0, 255);
   }

   public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glEnable(2848);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f matrix = poseStack.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
      Tesselator.getInstance().end();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(2848);
   }

   public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
      float hue = (float)((System.currentTimeMillis() + (long)index) % (long)((int)speed)) / speed;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public static BlockPos getCameraBlockPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getBlockPosition();
   }

   public static Vec3 getCameraPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getPosition();
   }

   public static RegionPos getCameraRegion() {
      return RegionPos.of(getCameraBlockPos());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack) {
      applyRegionalRenderOffset(matrixStack, getCameraRegion());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
      Vec3 offset = region.toVec3().subtract(getCameraPos());
      matrixStack.translate(offset.x, offset.y, offset.z);
   }

   public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
   }

   private static void innerFill(Matrix4f pMatrix, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      if (pMinX < pMaxX) {
         float i = pMinX;
         pMinX = pMaxX;
         pMaxX = i;
      }

      if (pMinY < pMaxY) {
         float j = pMinY;
         pMinY = pMaxY;
         pMaxY = j;
      }

      float f3 = (float)(pColor >> 24 & 0xFF) / 255.0F;
      float f = (float)(pColor >> 16 & 0xFF) / 255.0F;
      float f1 = (float)(pColor >> 8 & 0xFF) / 255.0F;
      float f2 = (float)(pColor & 0xFF) / 255.0F;
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      Tesselator.getInstance().end();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x + width, y, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
      tesselator.end();
   }

   private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
   }

   public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
      if (color == 16777215) {
         color = ARGB32.color(255, 255, 255, 255);
      }

      if (edgeRadius < 0.0F) {
         edgeRadius = 0.0F;
      }

      if (edgeRadius > width / 2.0F) {
         edgeRadius = width / 2.0F;
      }

      if (edgeRadius > height / 2.0F) {
         edgeRadius = height / 2.0F;
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.lineWidth(1.0F);
      drawRectBound(poseStack, x + edgeRadius, y + edgeRadius, width - edgeRadius * 2.0F, height - edgeRadius * 2.0F, color);
      drawRectBound(poseStack, x + edgeRadius, y, width - edgeRadius * 2.0F, edgeRadius, color);
      drawRectBound(poseStack, x + edgeRadius, y + height - edgeRadius, width - edgeRadius * 2.0F, edgeRadius, color);
      drawRectBound(poseStack, x, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
      drawRectBound(poseStack, x + width - edgeRadius, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      float centerX = x + edgeRadius;
      float centerY = y + edgeRadius;
      int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 180) / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 90) / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 270) / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)i / (double)(vertices * 4);
         color(
            buffer,
            matrix,
            (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
            (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
            color
         );
      }

      tesselator.end();
      RenderSystem.disableBlend();
   }

   public static void drawSolidBox(PoseStack matrixStack) {
      drawSolidBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
      Tesselator tessellator = RenderSystem.renderThreadTesselator();
      BufferBuilder bufferBuilder = tessellator.getBuilder();
      Matrix4f matrix = matrixStack.last().pose();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawOutlinedBox(PoseStack matrixStack) {
      drawOutlinedBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
      Matrix4f matrix = matrixStack.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      drawSolidBox(bb, bufferBuilder);
      BufferUploader.reset();
      vertexBuffer.bind();
      RenderedBuffer buffer = bufferBuilder.end();
      vertexBuffer.upload(buffer);
      VertexBuffer.unbind();
   }

   public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      drawOutlinedBox(bb, bufferBuilder);
      vertexBuffer.upload(bufferBuilder.end());
   }

   public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
      return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
   }

   public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
      return (float)mouseX > xLeft && (float)mouseX < xLeft + width && (float)mouseY > yUp && (float)mouseY < yUp + height;
   }

   public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
      float right = left + width;
      float bottom = top + height;
      fill(stack, left, top, right, bottom, color);
   }

   public static void 装女人(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }
}
