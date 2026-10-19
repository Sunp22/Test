package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class EarthboundBackdrop {
   private static final Identifier ID = Identifier.of("turnbattle", "dynamic/earthbound_bg");
   private static NativeImageBackedTexture tex;
   private static int texW;
   private static int texH;
   private static final int[][] SCHEMES = new int[][]{
      {14702592, 16295936, 16310272},
      {5777544, 10506440, 14715120},
      {551000, 1091728, 7923912},
      {8914984, 14698608, 16294072},
      {1058936, 2644176, 7387384},
      {2121736, 5812256, 11593824},
      {4200544, 13647936, 16298048}
   };

   private EarthboundBackdrop() {
   }

   private static int abgr(int rgb) {
      return 0xFF000000 | (rgb & 0xFF) << 16 | rgb & 0xFF00 | rgb >> 16 & 0xFF;
   }

   private static int lerp(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round((float)ar + (float)(br - ar) * t) << 16
         | Math.round((float)ag + (float)(bg - ag) * t) << 8
         | Math.round((float)ab + (float)(bb - ab) * t);
   }

   private static void ensure(int w, int h) {
      if (tex == null || texW != w || texH != h) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (tex != null) {
            mc.getTextureManager().destroyTexture(ID);
         }

         tex = new NativeImageBackedTexture(w, h, false);
         tex.setFilter(false, false);
         mc.getTextureManager().registerTexture(ID, tex);
         texW = w;
         texH = h;
      }
   }

   public static void render(DrawContext ctx, int sw, int sh, int seed, float alpha) {
      if (!(alpha <= 0.003F) && sw > 0 && sh > 0) {
         int w = 192;
         int h = Math.max(72, Math.min(192, Math.round((float)(w * sh) / (float)sw)));
         ensure(w, h);
         NativeImage img = tex.getImage();
         if (img != null) {
            int s = Math.abs(seed);
            int[] pal = SCHEMES[s % SCHEMES.length];
            int[] pal2 = SCHEMES[(s / SCHEMES.length + 3) % SCHEMES.length];
            int patA = s % 3;
            int patB = (s / 3 + 1) % 3;
            float t = MenuBackdrop.time();
            float cyc = t * 0.9F;

            for (int y = 0; y < h; y++) {
               float wob = (float)Math.sin((double)((float)y * 0.085F + t * 2.2F)) * 7.0F;
               float dx1 = (y % 2 == 0 ? wob : -wob) + (float)Math.sin((double)((float)y * 0.021F - t * 0.6F)) * 10.0F;
               float dx2 = (float)Math.sin((double)((float)y * 0.05F + t * 1.1F)) * 12.0F;
               float sq = 1.0F + 0.25F * (float)Math.sin((double)(t * 0.8F));
               float y2 = ((float)y - (float)h / 2.0F) * sq;

               for (int x = 0; x < w; x++) {
                  float v1 = pattern(patA, (float)x + dx1, (float)y, w, h, t);
                  float v2 = pattern(patB, (float)x + dx2, y2 + (float)h / 2.0F, w, h, -t * 0.7F);
                  int c1 = band(pal, v1 + cyc);
                  int c2 = band(pal2, v2 - cyc * 0.6F);
                  int c = lerp(c1, c2, 0.35F);
                  img.setColor(x, y, abgr(c));
               }
            }

            tex.upload();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
            ctx.drawTexture(ID, 0, 0, sw, sh, 0.0F, 0.0F, w, h, w, h);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private static float pattern(int kind, float x, float y, int w, int h, float t) {
      switch (kind) {
         case 0:
            return (x + y * 0.9F) / 9.0F + t * 0.6F;
         case 1:
            float d = Math.abs(x - (float)w / 2.0F) + Math.abs(y - (float)h / 2.0F);
            return d / 8.0F - t * 1.2F;
         default:
            float dx = x - (float)w / 2.0F;
            float dy = (y - (float)h / 2.0F) * 1.2F;
            return (float)Math.sqrt((double)(dx * dx + dy * dy)) / 7.0F - t;
      }
   }

   private static int band(int[] pal, float v) {
      float m = v - (float)Math.floor((double)(v / 3.0F)) * 3.0F;
      int i = (int)m;
      float f = m - (float)i;
      int a = pal[i % 3];
      int b = pal[(i + 1) % 3];
      return f > 0.85F ? lerp(a, b, (f - 0.85F) / 0.15F * 0.5F) : a;
   }
}
