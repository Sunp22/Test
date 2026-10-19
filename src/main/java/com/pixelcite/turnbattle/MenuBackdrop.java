package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class MenuBackdrop {
   private static final Identifier TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   private static final Identifier TEX_RING = Identifier.of("turnbattle", "textures/effects/ritual_ring.png");
   private static final long START = System.nanoTime();

   private MenuBackdrop() {
   }

   public static float time() {
      return (float)(System.nanoTime() - START) / 1.0E9F;
   }

   public static void render(DrawContext ctx, int w, int h) {
      render(ctx, w, h, 7321855, 1.0F);
   }

   public static void render(DrawContext ctx, int w, int h, int tint, float strength) {
      float t = time();
      ctx.fillGradient(0, 0, w, h, -16645366, -16381412);
      float cx = (float)w / 2.0F;
      float floorY = (float)h * 0.92F;
      float s = Math.max(0.0F, Math.min(1.0F, strength));
      float breath = 0.85F + 0.15F * (float)Math.sin((double)(t * 0.9F));
      int shaftW = (int)(Math.min((float)w, (float)h * 1.6F) * 0.34F);

      for (int i = 0; i < 7; i++) {
         float sw = (float)shaftW * (1.0F - (float)i * 0.13F) * (1.0F + 0.04F * (float)Math.sin((double)(t * 0.7F + (float)i)));
         int x0 = (int)(cx - sw / 2.0F);
         int x1 = (int)(cx + sw / 2.0F);
         ctx.fillGradient(x0, 0, x1, (int)floorY, argb(tint, 0.0F), argb(tint, 0.03F * breath * s));
      }

      int coreW = Math.max(2, shaftW / 9);
      ctx.fillGradient((int)(cx - (float)coreW), 0, (int)(cx + (float)coreW), (int)floorY, argb(16777215, 0.0F), argb(14544639, 0.1F * breath * s));
      float pool = Math.min((float)w, (float)h * 1.6F) * 1.1F;
      tex(ctx, TEX_GLOW, cx, floorY, pool, pool * 0.16F, tint, 0.55F * s);
      tex(ctx, TEX_GLOW, cx, floorY, pool * 0.4F, pool * 0.06F, 16777215, 0.35F * s);

      for (int r = 0; r < 4; r++) {
         float q = (t * 0.09F + (float)r / 4.0F) % 1.0F;
         float rs = pool * (0.12F + q * 0.9F);
         tex(ctx, TEX_RING, cx, floorY, rs, rs * 0.16F, tint, 0.32F * (1.0F - q) * s);
      }

      for (int i = 0; i < 90; i++) {
         float rx = CloneAnimator.rnd(i * 3 + 1) - 0.5F;
         float spread = i % 3 == 0 ? 1.0F : 0.32F;
         float speed = 0.035F + CloneAnimator.rnd(i * 7) * 0.05F;
         float q = (CloneAnimator.rnd(i * 3 + 2) + t * speed) % 1.0F;
         float sway = (float)Math.sin((double)(t * (0.6F + CloneAnimator.rnd(i) * 0.8F) + (float)i)) * 6.0F;
         float x = cx + rx * (float)w * spread + sway * q;
         float y = floorY - q * floorY * 1.05F;
         float life = (float)Math.sin(Math.PI * (double)q);
         float a = 0.65F * life * s;
         int px = 1 + (i % 7 == 0 ? 1 : 0);
         ctx.fill((int)x, (int)y, (int)x + px, (int)y + px, argb(i % 4 == 0 ? 16777215 : tint, a));
      }

      int e = Math.max(20, h / 4);
      ctx.fillGradient(0, 0, w, e, -805306368, 0);
      ctx.fillGradient(0, h - e / 2, w, h, 0, -1879048192);
   }

   static void tex(DrawContext ctx, Identifier id, float cx, float cy, float w, float h, int rgb, float a) {
      if (!(a <= 0.003F) && !(w < 1.0F) && !(h < 1.0F)) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F, Math.min(1.0F, a));
         ctx.drawTexture(id, Math.round(cx - w / 2.0F), Math.round(cy - h / 2.0F), Math.round(w), Math.round(h), 0.0F, 0.0F, 64, 64, 64, 64);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   static int argb(int rgb, float a) {
      int al = Math.max(0, Math.min(255, Math.round(a * 255.0F)));
      return al << 24 | rgb & 16777215;
   }
}
