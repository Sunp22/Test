package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class DarkBox {
   private static final Identifier TEX = Identifier.of("turnbattle", "textures/gui/dark_box.png");
   private static final int K = 14;
   public static final int REACH = 4;
   public static final int BORDER = 3;

   private DarkBox() {
   }

   public static boolean inDarkWorld() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.world != null && mc.world.getRegistryKey().getValue().toString().startsWith("turnbattle:dark_world_");
   }

   public static void draw(DrawContext ctx, int x0, int y0, int x1, int y1) {
      draw(ctx, x0, y0, x1, y1, 1.0F);
   }

   public static void draw(DrawContext ctx, int x0, int y0, int x1, int y1, float alpha) {
      if (!(alpha <= 0.004F)) {
         boolean fade = alpha < 0.999F;
         if (fade) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         }

         int K = Math.max(4, Math.min(14, Math.min(x1 - x0, y1 - y0) / 2));
         int reach = Math.round((float)(4 * K) / 14.0F);
         int X = x0 - reach;
         int Y = y0 - reach;
         int W = x1 - x0 + 2 * reach;
         int H = y1 - y0 + 2 * reach;
         int mw = Math.max(0, W - 2 * K);
         int mh = Math.max(0, H - 2 * K);
         ctx.drawTexture(TEX, X, Y, K, K, 0.0F, 0.0F, 28, 28, 57, 57);
         ctx.drawTexture(TEX, X + W - K, Y, K, K, 29.0F, 0.0F, 28, 28, 57, 57);
         ctx.drawTexture(TEX, X, Y + H - K, K, K, 0.0F, 29.0F, 28, 28, 57, 57);
         ctx.drawTexture(TEX, X + W - K, Y + H - K, K, K, 29.0F, 29.0F, 28, 28, 57, 57);
         if (mw > 0) {
            ctx.drawTexture(TEX, X + K, Y, mw, K, 28.0F, 0.0F, 1, 28, 57, 57);
            ctx.drawTexture(TEX, X + K, Y + H - K, mw, K, 28.0F, 29.0F, 1, 28, 57, 57);
         }

         if (mh > 0) {
            ctx.drawTexture(TEX, X, Y + K, K, mh, 0.0F, 28.0F, 28, 1, 57, 57);
            ctx.drawTexture(TEX, X + W - K, Y + K, K, mh, 29.0F, 28.0F, 28, 1, 57, 57);
         }

         if (mw > 0 && mh > 0) {
            ctx.drawTexture(TEX, X + K, Y + K, mw, mh, 28.0F, 28.0F, 1, 1, 57, 57);
         }

         if (fade) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }
}
