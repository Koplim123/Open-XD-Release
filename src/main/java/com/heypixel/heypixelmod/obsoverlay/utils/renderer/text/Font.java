package com.heypixel.heypixelmod.obsoverlay.utils.renderer.text;

import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Mesh;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTTPackedchar.Buffer;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

public class Font {
   public AbstractTexture texture;
   private final int height;
   private final float scale;
   private final float ascent;
   private final CharData[] charData;
   private final int from;
   private final HashMap<String, Double> widthCache = new HashMap<>();
   // 添加后备字体
   private Font fallbackFont;

   public Font(ByteBuffer buffer, int height, int charRangeFrom, int charRangeTo, int textureSize) {
      this.height = height;
      this.from = charRangeFrom;
      STBTTFontinfo fontInfo = STBTTFontinfo.create();
      STBTruetype.stbtt_InitFont(fontInfo, buffer);
      this.charData = new CharData[charRangeTo + 1 - charRangeFrom];
      Buffer cdata = STBTTPackedchar.create(this.charData.length);
      ByteBuffer bitmap = BufferUtils.createByteBuffer(textureSize * textureSize);
      STBTTPackContext packContext = STBTTPackContext.create();
      STBTruetype.stbtt_PackBegin(packContext, bitmap, textureSize, textureSize, 0, 1);
      STBTruetype.stbtt_PackSetOversampling(packContext, 2, 2);
      STBTruetype.stbtt_PackFontRange(packContext, buffer, 0, (float)height, this.from, cdata);
      STBTruetype.stbtt_PackEnd(packContext);
      this.texture = new BufferedTexture(
         textureSize, textureSize, bitmap, BufferedTexture.Format.A, BufferedTexture.Filter.Linear, BufferedTexture.Filter.Linear
      );
      this.scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, (float)height);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer ascent = stack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
         this.ascent = (float)ascent.get(0);
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }

      for (int i = 0; i < this.charData.length; i++) {
         STBTTPackedchar packedChar = (STBTTPackedchar)cdata.get(i);
         float ipw = 1.0F / (float)textureSize;
         float iph = 1.0F / (float)textureSize;
         this.charData[i] = new CharData(
            packedChar.xoff(),
            packedChar.yoff(),
            packedChar.xoff2(),
            packedChar.yoff2(),
            (float)packedChar.x0() * ipw,
            (float)packedChar.y0() * iph,
            (float)packedChar.x1() * ipw,
            (float)packedChar.y1() * iph,
            packedChar.xadvance()
         );
      }
   }

   public double getWidth(String string) {
      if (this.widthCache.containsKey(string)) {
         return this.widthCache.get(string);
      } else {
         double width = 0.0;

         for (int i = 0; i < string.length(); i++) {
            int cp = string.charAt(i) - this.from;
            if (cp == 167 && i + 1 < string.length()) {
               i++;
            } else {
               if (cp >= this.charData.length || cp < 0) {
                  // 使用后备字体处理未知字符
                  if (this.fallbackFont != null) {
                     width += this.fallbackFont.getWidth(String.valueOf(string.charAt(i)));
                  } else {
                     // 如果没有后备字体，尝试使用默认字符
                     cp = 0;
                  }
               }
               
               // 只有当cp在有效范围内时才计算宽度
               if (cp >= 0 && cp < this.charData.length) {
                  CharData c = this.charData[cp];
                  width += (double)c.xAdvance;
               }
            }
         }

         this.widthCache.put(string, width);
         return width;
      }
   }

   public double getHeight() {
      return (double)this.height;
   }

   public double render(Mesh mesh, String string, double x, double y, Color color, double scale, boolean shadow) {
      Color currentColor = color;
      // 调整y坐标以适应字体基线
      y += (double)(this.ascent * this.scale) * scale;

      for (int i = 0; i < string.length(); i++) {
         int cp = string.charAt(i) - this.from;
         if (cp == 167 && i + 1 < string.length()) {
            char ctrl = string.charAt(i + 1);
            ChatFormatting byCode = ChatFormatting.getByCode(ctrl);
            if (byCode != null && byCode.isColor() && !shadow) {
               currentColor = new Color(byCode.getColor());
            }

            i++;
         } else {
            // 检查字符是否超出范围
            boolean renderedWithFallback = false;
            if (cp >= this.charData.length || cp < 0) {
               // 使用后备字体渲染未知字符
               if (this.fallbackFont != null) {
                  // 使用后备字体渲染字符并更新x坐标
                  x = this.fallbackFont.render(mesh, String.valueOf(string.charAt(i)), x, y - (double)(this.ascent * this.scale) * scale, color, scale, shadow);
                  renderedWithFallback = true;
               } else {
                  // 如果没有后备字体，使用默认字符
                  cp = 0;
               }
            }
            
            // 只有当没有使用后备字体渲染且cp在有效范围内时才渲染
            if (!renderedWithFallback && cp >= 0 && cp < this.charData.length) {
               CharData c = this.charData[cp];
               mesh.quad(
                  mesh.vec2(x + (double)c.x0 * scale, y + (double)c.y0 * scale).vec2((double)c.u0, (double)c.v0).color(currentColor).next(),
                  mesh.vec2(x + (double)c.x0 * scale, y + (double)c.y1 * scale).vec2((double)c.u0, (double)c.v1).color(currentColor).next(),
                  mesh.vec2(x + (double)c.x1 * scale, y + (double)c.y1 * scale).vec2((double)c.u1, (double)c.v1).color(currentColor).next(),
                  mesh.vec2(x + (double)c.x1 * scale, y + (double)c.y0 * scale).vec2((double)c.u1, (double)c.v0).color(currentColor).next()
               );
               x += (double)c.xAdvance * scale;
            }
         }
      }

      return x;
   }
   
   // 设置后备字体的方法
   public void setFallbackFont(Font fallbackFont) {
      this.fallbackFont = fallbackFont;
   }
}