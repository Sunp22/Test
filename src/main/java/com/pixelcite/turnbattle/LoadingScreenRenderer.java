package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class LoadingScreenRenderer {
   private static final Identifier FONT = Identifier.of("turnbattle", "damage_numbers");
   private static final Identifier HEART = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private static final Identifier GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   private static final Identifier GRAZE = Identifier.of("turnbattle", "textures/gui/graze_ring.png");
   private static final String[] TIPS = new String[]{
      "Graze bullets with your SOUL to build up TP.",
      "When an enemy's name turns yellow, it can be SPARED.",
      "Hold Back while dodging to slow your SOUL down.",
      "A Dark Fountain opens a Dark World. Seal it, and it closes.",
      "Better materials make better weapons. Netherite hits far harder than wood.",
      "Your SOUL's trait decides your passive.",
      "How battles look can be changed on the Settings page of the menu.",
      "Higher level mobs carry far more HP. Bring a better weapon."
   };
   private static long lastFrame = 0L;
   private static long sessionStart = 0L;
   private static String message = "";
   private static long messageStart = 0L;
   private static float shownProgress = 0.0F;
   private static boolean hadProgress = false;
   private static int tip = 0;
   private static long tipStart = 0L;

   private LoadingScreenRenderer() {
   }

   private static Text det(String s) {
      return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT));
   }

   private static int argb(int rgb, float a) {
      int al = Math.max(0, Math.min(255, Math.round(a * 255.0F)));
      return al << 24 | rgb & 16777215;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }

   private static void tex(DrawContext ctx, Identifier id, int nat, float cx, float cy, float size, int rgb, float a) {
      if (!(a <= 0.003F) && !(size < 1.0F)) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F, Math.min(1.0F, a));
         int s = Math.round(size);
         ctx.drawTexture(id, Math.round(cx - (float)s / 2.0F), Math.round(cy - (float)s / 2.0F), s, s, 0.0F, 0.0F, nat, nat, nat, nat);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public static void render(DrawContext ctx, int w, int h, Text status, float progress) {
      long now = System.nanoTime();
      boolean fresh = now - lastFrame > 1500000000L;
      if (fresh) {
         sessionStart = now;
         shownProgress = 0.0F;
         hadProgress = false;
         tip = (int)(now / 1000000L % (long)TIPS.length);
         tipStart = now;
         message = "";
      }

      float dt = fresh ? 0.0F : Math.min(0.1F, (float)(now - lastFrame) / 1.0E9F);
      lastFrame = now;
      float t = (float)(now - sessionStart) / 1.0E9F;
      float clock = MenuBackdrop.time();
      String msg = status == null ? "" : status.getString().trim();
      if (!msg.equals(message)) {
         message = msg;
         messageStart = now;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      TextRenderer tr = mc.textRenderer;
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 400.0F);
      SoulType soul = TurnBattleClientMod.clientSoul;
      int soulRgb = soul != null ? soul.getColor() & 16777215 : 16711680;
      ctx.fill(0, 0, w, h, -16777216);
      float floorY = (float)h * 1.02F;
      float pool = (float)Math.max(w, h) * 1.1F;
      tex(ctx, GLOW, 64, (float)w / 2.0F, floorY, pool, soulRgb, 0.1F + 0.03F * (float)Math.sin((double)(clock * 0.8F)));
      tex(ctx, GLOW, 64, (float)w / 2.0F, floorY, pool * 0.45F, 16777215, 0.05F);

      for (int i = 0; i < 70; i++) {
         float rx = CloneAnimator.rnd(i * 3 + 1);
         float speed = 0.025F + CloneAnimator.rnd(i * 7) * 0.04F;
         float q = (CloneAnimator.rnd(i * 3 + 2) + clock * speed) % 1.0F;
         float sway = (float)Math.sin((double)(clock * (0.5F + CloneAnimator.rnd(i) * 0.7F) + (float)i)) * 5.0F;
         float x = rx * (float)w + sway;
         float y = (float)h - q * (float)(h + 10);
         float life = (float)Math.sin(Math.PI * (double)q);
         float a = (0.1F + 0.45F * life) * (0.35F + CloneAnimator.rnd(i * 5) * 0.65F);
         int px = i % 6 == 0 ? 2 : 1;
         ctx.fill((int)x, (int)y, (int)x + px, (int)y + px, argb(i % 3 == 0 ? soulRgb : 16777215, a));
      }

      int scale = h >= 420 ? 3 : 2;
      int heart = 16 * scale;
      float hx = (float)w / 2.0F;
      float hy = (float)h * 0.4F + (float)Math.sin((double)(clock * 2.1F)) * (float)scale;
      float breath = 0.5F + 0.5F * (float)Math.sin((double)(clock * 2.1F));
      tex(ctx, GLOW, 64, hx, hy, (float)heart * (3.2F + 0.4F * breath), soulRgb, 0.22F + 0.1F * breath);
      tex(ctx, GLOW, 64, hx, hy, (float)heart * 1.6F, 16777215, 0.06F + 0.04F * breath);
      float RING_EVERY = 1.6F;

      for (int k = 0; k < 2; k++) {
         float q = (clock / 1.6F + (float)k * 0.5F) % 1.0F;
         float e = 1.0F - (1.0F - q) * (1.0F - q);
         tex(ctx, GRAZE, 50, hx, hy, (float)heart * (0.9F + e * 2.4F), 16777215, 0.55F * (1.0F - q) * (1.0F - q));
      }

      RenderSystem.enableBlend();
      Identifier soulTex = soul != null ? soul.getTexture() : HEART;
      ctx.drawTexture(soulTex, Math.round(hx - (float)heart / 2.0F), Math.round(hy - (float)heart / 2.0F), heart, heart, 0.0F, 0.0F, 16, 16, 16, 16);
      String word = "LOADING";
      float ts = scale == 3 ? 2.0F : 1.5F;
      int wordW = tr.getWidth(det(word));
      int dotsW = tr.getWidth(det("..."));
      float wx = (float)w / 2.0F - (float)(wordW + dotsW) * ts / 2.0F;
      float wy = hy + (float)heart / 2.0F + (float)(14 * scale) / 2.0F;
      m.push();
      m.translate(wx, wy, 0.0F);
      m.scale(ts, ts, 1.0F);
      int cx = 0;

      for (int i = 0; i < word.length(); i++) {
         String c = word.substring(i, i + 1);
         float bob = (float)Math.sin((double)(clock * 4.0F - (float)i * 0.55F)) * 1.2F;
         ctx.drawText(tr, det(c), cx, Math.round(bob), -1, true);
         cx += tr.getWidth(det(c));
      }

      int dots = (int)(clock * 2.5F) % 4;
      ctx.drawText(tr, det(".".repeat(dots)), cx, 0, -1, true);
      m.pop();
      float sy = wy + 9.0F * ts + 10.0F;
      if (!message.isEmpty()) {
         String line = "* " + message;
         int typed = Math.min(line.length(), 2 + (int)((float)(now - messageStart) / 1.0E9F * 45.0F));
         String shown = line.substring(0, typed);
         int lw = tr.getWidth(det(line));
         int maxW = w - 24;
         float ls = lw > maxW ? (float)maxW / (float)lw : 1.0F;
         m.push();
         m.translate((float)w / 2.0F - (float)lw * ls / 2.0F, sy, 0.0F);
         m.scale(ls, ls, 1.0F);
         ctx.drawText(tr, det(shown), 0, 0, -3618606, true);
         m.pop();
      }

      int bw = Math.min(220, (int)((float)w * 0.5F));
      int bh = 3 * scale;
      int bx = w / 2 - bw / 2;
      int by = Math.round(sy + 18.0F);
      boolean known = progress >= 0.0F;
      if (known) {
         hadProgress = true;
      }

      if (known || hadProgress || t > 0.6F) {
         float barIn = clamp01((t - (known ? 0.0F : 0.6F)) / 0.4F);
         ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, argb(16777215, 0.85F * barIn));
         ctx.fill(bx, by, bx + bw, by + bh, argb(4194304, barIn));
         if (!known && !hadProgress) {
            float q = (float)(0.5 - 0.5 * Math.cos((double)(clock * 2.2F)));
            int sw = Math.max(12, bw / 5);
            int sx = bx + Math.round((float)(bw - sw) * q);
            ctx.fill(sx, by, sx + sw, by + bh, argb(16776960, 0.85F * barIn));
            ctx.fill(sx - 3, by, sx, by + bh, argb(16776960, 0.35F * barIn));
            ctx.fill(sx + sw, by, sx + sw + 3, by + bh, argb(16776960, 0.35F * barIn));
         } else {
            float target = known ? clamp01(progress) : shownProgress;
            shownProgress = shownProgress + (target - shownProgress) * clamp01(dt * 6.0F);
            if (target < shownProgress - 0.5F) {
               shownProgress = target;
            }

            int fill = Math.round((float)bw * shownProgress);
            if (fill > 0) {
               ctx.fill(bx, by, bx + fill, by + bh, argb(16752640, barIn));
               ctx.fill(bx, by, bx + fill, by + Math.max(1, bh - scale), argb(16776960, barIn));
               float g = clock * 0.6F % 1.4F;
               int gx = bx + Math.round((float)fill * g) - 6;

               for (int k = 0; k < 6; k++) {
                  int x = gx + k;
                  if (x >= bx && x < bx + fill) {
                     ctx.fill(x, by, x + 1, by + bh, argb(16777215, 0.45F * (1.0F - (float)Math.abs(k - 3) / 3.5F) * barIn));
                  }
               }
            }

            String pct = Math.round(shownProgress * 100.0F) + "%";
            ctx.drawText(tr, det(pct), bx + bw + 6, by + bh / 2 - 4, argb(16777215, barIn), true);
         }
      }

      float tipAge = (float)(now - tipStart) / 1.0E9F;
      float TIP_FOR = 7.0F;
      if (tipAge > 7.0F) {
         tip = (tip + 1) % TIPS.length;
         tipStart = now;
         tipAge = 0.0F;
      }

      float tipA = clamp01((t - 0.8F) / 0.6F) * Math.min(clamp01(tipAge / 0.5F), clamp01((7.0F - tipAge) / 0.5F));
      if (tipA > 0.01F) {
         String tipLine = TIPS[tip];
         int tw = tr.getWidth(tipLine);
         String label = "TIP";
         int lw = tr.getWidth(det(label));
         int total = lw + 6 + tw;
         float tsc = total > w - 20 ? (float)(w - 20) / (float)total : 1.0F;
         m.push();
         m.translate((float)w / 2.0F - (float)total * tsc / 2.0F, (float)(h - 16), 0.0F);
         m.scale(tsc, tsc, 1.0F);
         ctx.drawText(tr, det(label), 0, 0, argb(16776960, tipA), true);
         ctx.drawText(tr, tipLine, lw + 6, 1, argb(10133688, tipA), true);
         m.pop();
      }

      float fade = 1.0F - clamp01(t / 0.35F);
      if (fade > 0.0F) {
         ctx.fill(0, 0, w, h, argb(0, fade));
      }

      m.pop();
   }
}
