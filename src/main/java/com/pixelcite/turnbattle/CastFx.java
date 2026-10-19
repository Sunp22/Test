package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
final class CastFx {
   static final float PI = (float) Math.PI;
   static final float TAU = (float) (Math.PI * 2);
   private static final String[] HEART = new String[]{".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."};

   private CastFx() {
   }

   static float sin(float a) {
      return (float)Math.sin((double)a);
   }

   static float cos(float a) {
      return (float)Math.cos((double)a);
   }

   static float bell(float q) {
      return !(q <= 0.0F) && !(q >= 1.0F) ? sin((float) Math.PI * q) : 0.0F;
   }

   static void ellipse(DrawContext ctx, float cx, float cy, float rx, float ry, float rot, float th, int argb, int n) {
      if (!(rx < 0.4F) && (argb >>> 24 & 0xFF) != 0) {
         float cr = cos(rot);
         float sr = sin(rot);
         float px = 0.0F;
         float py = 0.0F;

         for (int i = 0; i <= n; i++) {
            float a = (float) (Math.PI * 2) * (float)i / (float)n;
            float ex = cos(a) * rx;
            float ey = sin(a) * ry;
            float x = cx + ex * cr - ey * sr;
            float y = cy + ex * sr + ey * cr;
            if (i > 0) {
               CloneAnimator.line(ctx, px, py, x, y, th, argb);
            }

            px = x;
            py = y;
         }
      }
   }

   static void groundRing(CastAnims.Fx f, float cx, float r, float th, int argb, float rot, boolean dashed) {
      CloneAnimator.ringHalf(f.ctx, cx, f.fy0, r, r * 0.3F, th, argb, rot, dashed, f.front);
   }

   static void polygon(DrawContext ctx, float cx, float cy, float r, int n, float rot, float th, int argb) {
      if (!(r < 0.4F)) {
         for (int i = 0; i < n; i++) {
            float a0 = rot + (float) (Math.PI * 2) * (float)i / (float)n;
            float a1 = rot + (float) (Math.PI * 2) * (float)(i + 1) / (float)n;
            CloneAnimator.line(ctx, cx + cos(a0) * r, cy + sin(a0) * r, cx + cos(a1) * r, cy + sin(a1) * r, th, argb);
         }
      }
   }

   static void snowflake(DrawContext ctx, float cx, float cy, float r, float rot, float th, int argb) {
      if (!(r < 0.6F)) {
         for (int i = 0; i < 6; i++) {
            float a = rot + (float)i * (float) Math.PI / 3.0F;
            float ex = cx + cos(a) * r;
            float ey = cy + sin(a) * r;
            CloneAnimator.line(ctx, cx, cy, ex, ey, th, argb);

            for (int b = 1; b <= 2; b++) {
               float f = b == 1 ? 0.45F : 0.72F;
               float bl = r * (b == 1 ? 0.3F : 0.2F);
               float bx = cx + cos(a) * r * f;
               float by = cy + sin(a) * r * f;
               CloneAnimator.line(ctx, bx, by, bx + cos(a + 0.7F) * bl, by + sin(a + 0.7F) * bl, th * 0.8F, argb);
               CloneAnimator.line(ctx, bx, by, bx + cos(a - 0.7F) * bl, by + sin(a - 0.7F) * bl, th * 0.8F, argb);
            }
         }

         CloneAnimator.diamond(ctx, cx, cy, r * 0.16F, argb);
      }
   }

   static void heart(DrawContext ctx, float cx, float cy, float px, int rgb, float a) {
      if (!(a <= 0.01F) && !(px <= 0.05F)) {
         for (int pass = 0; pass < 2; pass++) {
            for (int r = 0; r < 6; r++) {
               for (int c = 0; c < 7; c++) {
                  if (HEART[r].charAt(c) == '#') {
                     float x = cx + ((float)c - 3.0F) * px;
                     float y = cy + ((float)r - 2.5F) * px;
                     if (pass == 0) {
                        CloneAnimator.quad(ctx, x, y, px * 1.9F, px * 1.9F, 0.0F, CloneAnimator.argb(0, 0.45F * a));
                     } else {
                        CloneAnimator.quad(ctx, x, y, px * 1.02F, px * 1.02F, 0.0F, CloneAnimator.argb(rgb, a));
                     }
                  }
               }
            }
         }

         CloneAnimator.quad(ctx, cx - 1.5F * px, cy - 1.5F * px, px, px, 0.0F, CloneAnimator.argb(16777215, 0.85F * a));
      }
   }

   static void note(DrawContext ctx, float x, float y, float s, int argb) {
      CloneAnimator.quad(ctx, x, y, s * 1.5F, s * 1.1F, -0.35F, argb);
      CloneAnimator.line(ctx, x + s * 0.6F, y - 0.2F * s, x + s * 0.6F, y - s * 3.0F, Math.max(0.6F, s * 0.35F), argb);
      CloneAnimator.line(ctx, x + s * 0.6F, y - s * 3.0F, x + s * 1.6F, y - s * 2.2F, Math.max(0.6F, s * 0.35F), argb);
   }

   static void zee(DrawContext ctx, float x, float y, float s, float th, int argb) {
      CloneAnimator.line(ctx, x - s, y - s, x + s, y - s, th, argb);
      CloneAnimator.line(ctx, x + s, y - s, x - s, y + s, th, argb);
      CloneAnimator.line(ctx, x - s, y + s, x + s, y + s, th, argb);
   }

   static void crescent(DrawContext ctx, float cx, float cy, float r, float thick, float ang, float span, int argb) {
      int n = 14;

      for (int i = 0; i < n; i++) {
         float s0 = (float)i / (float)n;
         float s1 = (float)(i + 1) / (float)n;
         float a0 = ang - span / 2.0F + span * s0;
         float a1 = ang - span / 2.0F + span * s1;
         float w = thick * sin((float) Math.PI * (s0 + s1) / 2.0F) + 0.5F;
         CloneAnimator.line(ctx, cx + cos(a0) * r, cy + sin(a0) * r, cx + cos(a1) * r, cy + sin(a1) * r, w, argb);
      }
   }

   static void zigRing(DrawContext ctx, float cx, float cy, float r, float jag, int n, float rot, float th, int argb) {
      float px = 0.0F;
      float py = 0.0F;

      for (int i = 0; i <= n * 2; i++) {
         float a = rot + (float) (Math.PI * 2) * (float)i / (float)(n * 2);
         float rr = r + (i % 2 == 0 ? jag : -jag);
         float x = cx + cos(a) * rr;
         float y = cy + sin(a) * rr;
         if (i > 0) {
            CloneAnimator.line(ctx, px, py, x, y, th, argb);
         }

         px = x;
         py = y;
      }
   }

   static int hue(float h, float s, float a) {
      return VfxBatch.hsv(h, s, 1.0F, CloneAnimator.clamp01(a));
   }

   static void trail(CastAnims.Fx f, boolean right, float ax, float ay, float az, float span, float from, float to, float width, int rgb, int core, float alpha) {
      if (!(f.p <= from) && !(f.p >= to + span)) {
         int n = 10;
         float[] prev = null;

         for (int i = 0; i <= n; i++) {
            float pp = Math.max(from, Math.min(to, f.p - span + span * (float)i / (float)n));
            float[] pt = f.armAt(pp, right, ax, ay, az);
            if (prev != null) {
               float k = (float)i / (float)n;
               float a = k * k * alpha * (1.0F - CloneAnimator.seg(f.p, to, to + span));
               CloneAnimator.line(f.ctx, prev[0], prev[1], pt[0], pt[1], width * (0.4F + 0.6F * k) + 1.5F, CloneAnimator.argb(rgb, a * 0.55F));
               CloneAnimator.line(f.ctx, prev[0], prev[1], pt[0], pt[1], width * 0.45F * k + 0.6F, CloneAnimator.argb(core, a));
            }

            prev = pt;
         }
      }
   }

   static void converge(CastAnims.Fx f, float x, float y, float rad, int n, float a, float b, int rgb, int salt) {
      float w = CastAnims.win(f.p, a, b, 0.05F);
      if (!(w <= 0.0F)) {
         for (int i = 0; i < n; i++) {
            float ph = (f.t * 0.0016F + CloneAnimator.rnd(salt + i * 7)) % 1.0F;
            float r = rad * (1.0F - ph) * (1.0F - ph);
            float ang = CloneAnimator.rnd(salt + i * 3) * (float) (Math.PI * 2) + ph * 2.4F;
            CloneAnimator.quad(f.ctx, x + cos(ang) * r, y + sin(ang) * r * 0.85F, 1.6F, 1.6F, 0.0F, CloneAnimator.argb(rgb, ph * 0.95F * w));
         }
      }
   }

   static void rudeBuster(CastAnims.Fx f) {
      int col = (f.fr.color & 16777215) == 16720418 ? 16726586 : 11824383;
      float h = f.h;
      float[] tip = f.arm(true, -1.0F, 11.0F, -14.5F);
      if (!f.front) {
         float q = f.q(0.48F, 0.66F);
         if (q > 0.0F && q < 1.0F) {
            for (int i = 0; i < 4; i++) {
               float y = f.by() - h * (0.22F + 0.18F * (float)i);
               float len = h * (0.4F + 0.12F * (float)(i % 2)) * (1.0F - q) + 2.0F;
               float x1 = f.bx() - (float)f.dir * h * 0.15F;
               CloneAnimator.line(f.ctx, x1 - (float)f.dir * len, y, x1, y, 1.0F, CloneAnimator.argb(16777215, 0.5F * (1.0F - q)));
            }
         }

         float g = CastAnims.win(f.p, 0.16F, 0.5F, 0.06F);
         if (g > 0.0F) {
            CloneAnimator.glow(f.ctx, tip[0], tip[1], h * 0.32F * (0.8F + 0.2F * sin(f.t * 0.03F)), f.t * 0.002F, col, 0.07F * g);
         }

         groundRing(
            f,
            f.bx(),
            h * (0.2F + 0.6F * CloneAnimator.easeOutCubic(f.q(0.48F, 0.72F))),
            1.3F,
            CloneAnimator.argb(col, 0.8F * (1.0F - f.q(0.48F, 0.72F)) * (f.p > 0.48F ? 1.0F : 0.0F)),
            0.0F,
            false
         );
      } else {
         groundRing(
            f,
            f.bx(),
            h * (0.2F + 0.6F * CloneAnimator.easeOutCubic(f.q(0.48F, 0.72F))),
            1.3F,
            CloneAnimator.argb(col, 0.8F * (1.0F - f.q(0.48F, 0.72F)) * (f.p > 0.48F ? 1.0F : 0.0F)),
            0.0F,
            false
         );
         converge(f, tip[0], tip[1], h * 0.5F, 10, 0.14F, 0.5F, col, 11);
         float ch = CastAnims.win(f.p, 0.16F, 0.5F, 0.05F);
         if (ch > 0.0F) {
            float pulse = 0.8F + 0.2F * sin(f.t * 0.04F);
            CloneAnimator.star4(f.ctx, tip[0], tip[1], (3.0F + 3.0F * f.q(0.16F, 0.46F)) * pulse * ch, CloneAnimator.argb(16777215, ch));

            for (int i = 0; i < 3; i++) {
               float a = CloneAnimator.rnd((int)(f.t / 60.0F) * 7 + i) * (float) (Math.PI * 2);
               float r = h * 0.12F * (0.6F + CloneAnimator.rnd(i * 5 + (int)(f.t / 60.0F)));
               CloneAnimator.line(f.ctx, tip[0], tip[1], tip[0] + cos(a) * r, tip[1] + sin(a) * r, 1.0F, CloneAnimator.argb(col, 0.8F * ch));
            }
         }

         trail(f, true, -1.0F, 11.0F, -14.5F, 0.12F, 0.4F, 0.54F, 5.0F, col, 16777215, 1.0F);
         float wq = f.q(0.5F, 0.82F);
         if (wq > 0.0F && wq < 1.0F) {
            float[] rel = f.armAt(0.5F, true, -1.0F, 11.0F, -14.5F);
            float sx0 = rel[0];
            float sy0 = rel[1];

            for (int g2 = 3; g2 >= 0; g2--) {
               float qq = Math.max(0.0F, wq - (float)g2 * 0.06F);
               float x = sx0 + (float)f.dir * CloneAnimator.easeOutCubic(qq) * h * 2.8F;
               float y = CloneAnimator.mix(sy0, f.by() - h * 0.5F, CloneAnimator.easeOutCubic(qq));
               float a = (1.0F - wq) * (g2 == 0 ? 1.0F : 0.35F / (float)g2);
               float ang = f.dir > 0 ? 0.0F : (float) Math.PI;
               crescent(f.ctx, x - (float)f.dir * h * 0.2F, y, h * 0.42F, h * 0.18F, ang, 2.2F, CloneAnimator.argb(col, a * 0.75F));
               crescent(f.ctx, x - (float)f.dir * h * 0.2F, y, h * 0.42F, h * 0.07F, ang, 1.9F, CloneAnimator.argb(16777215, a));
            }
         }

         float bq = f.q(0.48F, 0.66F);
         if (bq > 0.0F && bq < 1.0F) {
            float[] rel = f.armAt(0.5F, true, -1.0F, 11.0F, -14.5F);
            CloneAnimator.star4(f.ctx, rel[0], rel[1], 7.0F * (1.0F - bq) + 1.0F, CloneAnimator.argb(16777215, 1.0F - bq));
         }
      }
   }

   static void iceShock(CastAnims.Fx f) {
      float h = f.h;
      float fr = f.q(0.08F, 0.5F);
      groundRing(
         f,
         f.bx(),
         h * (0.15F + 0.3F * CloneAnimator.easeOutCubic(fr)),
         1.1F,
         CloneAnimator.argb(11070719, 0.6F * CastAnims.win(f.p, 0.08F, 0.9F, 0.1F)),
         f.t * 0.001F,
         true
      );
      if (f.front) {
         float[] hd = f.handsMid();
         float form = CloneAnimator.easeOutBack(f.q(0.1F, 0.42F));
         if (f.p < 0.55F && form > 0.0F) {
            float cx = hd[0];
            float cy = hd[1] - h * 0.14F;
            CloneAnimator.glow(f.ctx, cx, cy, h * 0.22F * form, f.t * 0.002F, 5811967, 0.05F * form);
            snowflake(f.ctx, cx, cy, h * 0.16F * form, f.t * 0.003F, 1.3F, CloneAnimator.argb(11070719, 0.95F));
            snowflake(f.ctx, cx, cy, h * 0.16F * form, f.t * 0.003F, 0.6F, CloneAnimator.argb(16777215, 0.9F));
            converge(f, cx, cy, h * 0.45F, 9, 0.05F, 0.5F, 16777215, 23);
         }

         float sq = f.q(0.53F, 0.86F);
         if (sq > 0.0F && sq < 1.0F) {
            float[] rel = handsMidAt(f, 0.55F);
            float x = rel[0] + (float)f.dir * CloneAnimator.easeOutCubic(sq) * h * 2.6F;
            float y = rel[1] - h * 0.05F;
            float a = 1.0F - sq * sq;
            snowflake(f.ctx, x, y, h * 0.14F, f.t * 0.02F, 1.3F, CloneAnimator.argb(11070719, a));
            snowflake(f.ctx, x, y, h * 0.14F, f.t * 0.02F, 0.6F, CloneAnimator.argb(16777215, a));

            for (int i = 0; i < 8; i++) {
               float k = (float)i / 8.0F;
               float tx = CloneAnimator.mix(rel[0], x, k) + (CloneAnimator.rnd(i * 13) - 0.5F) * 6.0F;
               float ty = CloneAnimator.mix(rel[1], y, k) + (CloneAnimator.rnd(i * 7) - 0.5F) * 8.0F;
               CloneAnimator.star4(f.ctx, tx, ty, 1.6F * k, CloneAnimator.argb(i % 2 == 0 ? 11070719 : 16777215, a * k));
            }
         }
      }
   }

   static void glitterBomb(CastAnims.Fx f) {
      if (f.front) {
         float h = f.h;
         float r = h * 0.07F;
         if (f.p < 0.43F) {
            float[] hd = f.hand(true);
            float a = f.q(0.02F, 0.1F);
            bomb(f, hd[0], hd[1], r, a);
         }

         float fq = f.q(0.42F, 0.86F);
         if (fq > 0.0F && fq < 1.0F) {
            float[] rel = f.handAt(0.42F, true);

            for (int i = 14; i >= 0; i--) {
               float qq = fq - (float)i * 0.025F;
               if (!(qq < 0.0F)) {
                  float[] pt = arc(f, rel, qq);
                  float a = (1.0F - (float)i / 15.0F) * (1.0F - fq * 0.5F);
                  int c = hue(CloneAnimator.rnd(i * 31 + (int)(f.t / 90.0F)), 0.5F, a);
                  CloneAnimator.quad(
                     f.ctx,
                     pt[0] + (CloneAnimator.rnd(i * 3) - 0.5F) * 5.0F,
                     pt[1] + (CloneAnimator.rnd(i * 5) - 0.5F) * 5.0F + (float)i * 0.6F,
                     1.6F,
                     1.6F,
                     f.t * 0.01F + (float)i,
                     c
                  );
               }
            }

            float[] pt = arc(f, rel, fq);
            bomb(f, pt[0], pt[1], r, 1.0F - CloneAnimator.seg(fq, 0.85F, 1.0F));
         }

         float bq = f.q(0.4F, 0.56F);
         if (bq > 0.0F && bq < 1.0F) {
            float[] rel = f.handAt(0.42F, true);

            for (int ix = 0; ix < 6; ix++) {
               float a = (float)ix * (float) (Math.PI * 2) / 6.0F;
               float d = 2.0F + CloneAnimator.easeOutCubic(bq) * h * 0.18F;
               CloneAnimator.star4(f.ctx, rel[0] + cos(a) * d, rel[1] + sin(a) * d, 2.2F * (1.0F - bq), CloneAnimator.argb(16767082, 1.0F - bq));
            }
         }
      }
   }

   private static float[] arc(CastAnims.Fx f, float[] from, float q) {
      float x = from[0] + (float)f.dir * q * f.h * 2.8F;
      float y = CloneAnimator.mix(from[1], f.by() - f.h * 0.45F, q) - sin((float) Math.PI * q) * f.h * 0.75F;
      return new float[]{x, y};
   }

   private static void bomb(CastAnims.Fx f, float x, float y, float r, float a) {
      if (!(a <= 0.01F)) {
         CloneAnimator.glow(f.ctx, x, y, r * 2.4F, f.t * 0.003F, 16757986, 0.08F * a);
         CloneAnimator.quad(f.ctx, x, y, r * 2.1F, r * 2.1F, f.t * 0.004F, CloneAnimator.argb(3810384, 0.95F * a));
         CloneAnimator.quad(f.ctx, x, y, r * 1.6F, r * 1.6F, f.t * 0.004F + 0.78F, CloneAnimator.argb(16743115, 0.95F * a));
         CloneAnimator.quad(f.ctx, x - r * 0.35F, y - r * 0.35F, r * 0.6F, r * 0.6F, 0.0F, CloneAnimator.argb(16777215, 0.9F * a));

         for (int i = 0; i < 4; i++) {
            float ang = f.t * 0.006F + (float)i * (float) (Math.PI * 2) / 4.0F;
            float tw = 0.5F + 0.5F * sin(f.t * 0.02F + (float)i * 1.7F);
            CloneAnimator.star4(
               f.ctx,
               x + cos(ang) * r * 1.6F,
               y + sin(ang) * r * 1.6F,
               1.8F * tw,
               CloneAnimator.argb(hue((float)i * 0.25F + f.t * 4.0E-4F, 0.4F, 1.0F) & 16777215, a)
            );
         }

         CloneAnimator.star4(f.ctx, x + r * 0.4F, y - r * 1.4F, 1.6F + sin(f.t * 0.05F), CloneAnimator.argb(16767082, a));
      }
   }

   static void snowgrave(CastAnims.Fx f) {
      float h = f.h;
      float bx = f.bx();
      float by = f.by();
      float dark = CloneAnimator.easeInOutSine(f.q(0.04F, 0.5F)) * (1.0F - f.q(0.9F, 1.0F));
      if (!f.front) {
         if (dark > 0.0F) {
            CloneAnimator.glow(f.ctx, bx, by - h * 0.55F, h * 1.3F, f.t * 3.0E-4F, 660528, 0.1F * dark);
            CloneAnimator.glow(f.ctx, bx, by - h * 0.55F, h * 0.8F, -f.t * 4.0E-4F, 1849958, 0.07F * dark);
         }

         groundRing(f, bx, h * 0.55F * dark, 1.2F, CloneAnimator.argb(5811967, 0.55F * dark), -f.t * 6.0E-4F, true);
      } else {
         groundRing(f, bx, h * 0.55F * dark, 1.2F, CloneAnimator.argb(5811967, 0.55F * dark), -f.t * 6.0E-4F, true);
         float snow = CloneAnimator.easeInOutSine(f.q(0.05F, 0.35F)) * (1.0F - f.q(0.92F, 1.0F));

         for (int i = 0; i < 18; i++) {
            float ph = (f.t * 2.8E-4F * (0.7F + 0.6F * CloneAnimator.rnd(i)) + CloneAnimator.rnd(i * 17)) % 1.0F;
            float x = bx + (CloneAnimator.rnd(i * 5) - 0.5F) * h * 2.4F + sin(f.t * 0.0013F + (float)i) * 5.0F - (float)f.dir * ph * h * 0.4F;
            float y = by - h * 1.6F + ph * h * 1.7F;
            float s = 1.2F + CloneAnimator.rnd(i * 9);
            CloneAnimator.quad(f.ctx, x, y, s, s, 0.0F, CloneAnimator.argb(16777215, 0.8F * snow * bell(ph)));
         }

         for (int i = 0; i < 5; i++) {
            float ph = (f.t * 0.0011F + CloneAnimator.rnd(i * 23)) % 1.0F;
            float y = by - h * (0.15F + 0.95F * CloneAnimator.rnd(i * 41));
            float x = bx + (float)f.dir * (-1.2F + 2.4F * ph) * h;
            CloneAnimator.line(f.ctx, x, y, x - (float)f.dir * h * 0.35F, y + 2.0F, 0.8F, CloneAnimator.argb(11070719, 0.4F * snow * bell(ph)));
         }

         float eyes = CastAnims.win(f.p, 0.62F, 0.9F, 0.06F);
         if (eyes > 0.0F) {
            for (int e = -1; e <= 1; e += 2) {
               float[] ep = f.head((float)e * 2.0F, -4.5F, -4.2F);
               CloneAnimator.quad(f.ctx, ep[0], ep[1], 1.6F, 1.2F, 0.0F, CloneAnimator.argb(15269119, eyes));
               CloneAnimator.glow(f.ctx, ep[0], ep[1], 3.0F, 0.0F, 11070719, 0.12F * eyes);
            }
         }

         float form = CloneAnimator.easeInOutSine(f.q(0.35F, 0.82F));
         if (f.p < 0.88F && form > 0.0F) {
            float[] hd = f.handsMid();
            float cx = hd[0];
            float cy = hd[1] - h * 0.32F * form;
            float r = h * 0.34F * form;
            CloneAnimator.glow(f.ctx, cx, cy, r * 1.5F, f.t * 0.001F, 5811967, 0.06F * form);
            snowflake(f.ctx, cx, cy, r, f.t * 7.0E-4F, 1.8F, CloneAnimator.argb(11070719, 0.9F * form));
            snowflake(f.ctx, cx, cy, r * 0.6F, -f.t * 0.0011F + 0.5F, 1.1F, CloneAnimator.argb(16777215, 0.85F * form));
            ellipse(f.ctx, cx, cy, r * 1.15F, r * 1.15F, 0.0F, 0.8F, CloneAnimator.argb(11070719, 0.5F * form), 28);
            converge(f, cx, cy, h * 0.9F, 14, 0.35F, 0.86F, 16777215, 31);
         }

         float sq = f.q(0.87F, 1.0F);
         if (sq > 0.0F && sq < 1.0F) {
            float[] rel = handsMidAt(f, 0.86F);
            float x = rel[0] + (float)f.dir * CloneAnimator.easeOutCubic(sq) * h * 3.0F;
            float y = rel[1] - h * 0.32F + sq * h * 0.2F;
            float r = h * 0.34F * (1.0F - 0.5F * sq);
            snowflake(f.ctx, x, y, r, f.t * 0.01F, 1.8F, CloneAnimator.argb(11070719, 1.0F - sq));
            snowflake(f.ctx, x, y, r * 0.6F, -f.t * 0.01F, 1.1F, CloneAnimator.argb(16777215, 1.0F - sq));

            for (int i = 0; i < 6; i++) {
               CloneAnimator.line(
                  f.ctx,
                  x - (float)f.dir * (8.0F + (float)i * 6.0F),
                  y + ((float)i - 2.5F) * 3.0F,
                  x - (float)f.dir * (8.0F + (float)i * 6.0F + 14.0F),
                  y + ((float)i - 2.5F) * 3.0F,
                  0.8F,
                  CloneAnimator.argb(11070719, 0.6F * (1.0F - sq))
               );
            }
         }
      }
   }

   static void xSlash(CastAnims.Fx f) {
      float h = f.h;
      if (!f.front) {
         float q = f.q(0.15F, 0.4F);
         if (q > 0.0F && q < 1.0F) {
            for (int i = 0; i < 4; i++) {
               float y = f.by() - h * (0.2F + 0.2F * (float)i);
               float len = h * 0.5F * (1.0F - q) + 2.0F;
               float x1 = f.bx() - (float)f.dir * h * 0.1F;
               CloneAnimator.line(f.ctx, x1 - (float)f.dir * len, y, x1, y, 1.0F, CloneAnimator.argb(16777215, 0.55F * (1.0F - q)));
            }
         }
      } else {
         int edge = f.fr.color & 16777215;
         trail(f, true, -1.0F, 11.0F, -14.5F, 0.09F, 0.18F, 0.31F, 4.0F, edge, 16777215, 1.0F);
         trail(f, true, -1.0F, 11.0F, -14.5F, 0.09F, 0.42F, 0.53F, 4.0F, edge, 16777215, 1.0F);
         float[] c0 = f.armAt(0.3F, true, -1.0F, 11.0F, -7.0F);
         float cx = c0[0] + (float)f.dir * h * 0.25F;
         float cy = f.fy0 - h * 0.55F;
         float L = h * 0.48F;
         float a1 = f.q(0.27F, 0.32F);
         float a2 = f.q(0.49F, 0.54F);
         float fade = 1.0F - f.q(0.68F, 0.86F);
         float flash = bell(f.q(0.52F, 0.66F));
         if (a1 > 0.0F && fade > 0.0F) {
            float ex = cx - (float)f.dir * L * CloneAnimator.mix(-1.0F, 1.0F, a1);
            float ey = cy + L * CloneAnimator.mix(-1.0F, 1.0F, a1);
            CloneAnimator.line(f.ctx, cx + (float)f.dir * L, cy - L, ex, ey, 3.2F + flash * 2.0F, CloneAnimator.argb(edge, 0.55F * fade));
            CloneAnimator.line(f.ctx, cx + (float)f.dir * L, cy - L, ex, ey, 1.3F + flash, CloneAnimator.argb(16777215, fade));
         }

         if (a2 > 0.0F && fade > 0.0F) {
            float ex = cx + (float)f.dir * L * CloneAnimator.mix(-1.0F, 1.0F, a2);
            float ey = cy + L * CloneAnimator.mix(-1.0F, 1.0F, a2);
            CloneAnimator.line(f.ctx, cx - (float)f.dir * L, cy - L, ex, ey, 3.2F + flash * 2.0F, CloneAnimator.argb(edge, 0.55F * fade));
            CloneAnimator.line(f.ctx, cx - (float)f.dir * L, cy - L, ex, ey, 1.3F + flash, CloneAnimator.argb(16777215, fade));
         }

         if (flash > 0.0F) {
            CloneAnimator.star4(f.ctx, cx, cy, 9.0F * flash, CloneAnimator.argb(16777215, flash));
         }

         float sh = f.q(0.66F, 0.95F);
         if (sh > 0.0F && sh < 1.0F) {
            for (int i = 0; i < 10; i++) {
               float a = CloneAnimator.rnd(i * 19) * (float) (Math.PI * 2);
               float d = CloneAnimator.easeOutCubic(sh) * h * (0.3F + 0.35F * CloneAnimator.rnd(i));
               CloneAnimator.quad(
                  f.ctx,
                  cx + cos(a) * d,
                  cy + sin(a) * d + sh * sh * 8.0F,
                  1.6F,
                  1.6F,
                  sh * 5.0F + (float)i,
                  CloneAnimator.argb(i % 2 == 0 ? 16777215 : edge, 1.0F - sh)
               );
            }
         }
      }
   }

   static void gigaflare(CastAnims.Fx f) {
      float h = f.h;
      float circ = CloneAnimator.easeOutBack(f.q(0.06F, 0.24F)) * (1.0F - f.q(0.84F, 0.96F));
      if (circ > 0.0F) {
         groundRing(f, f.bx(), h * 0.5F * circ, 1.3F, CloneAnimator.argb(16767082, 0.8F * circ), f.t * 0.0015F, false);
         groundRing(f, f.bx(), h * 0.36F * circ, 1.0F, CloneAnimator.argb(16777215, 0.6F * circ), -f.t * 0.002F, true);
      }

      float grow = CloneAnimator.easeOutCubic(f.q(0.12F, 0.7F));
      float[] mid = f.handsMid();
      float r = h * (0.05F + 0.16F * grow);
      float cx = mid[0];
      float cy = mid[1] - h * 0.06F - r * 1.15F;
      boolean held = f.p < 0.74F;
      if (!f.front) {
         if (held && grow > 0.0F) {
            for (int i = 0; i < 12; i++) {
               float a = f.t * 0.0012F + (float)i * (float) (Math.PI * 2) / 12.0F;
               float len = r * (1.8F + 0.7F * (float)(i % 2)) + h * 0.2F * grow;
               CloneAnimator.line(
                  f.ctx, cx + cos(a) * r, cy + sin(a) * r, cx + cos(a) * len, cy + sin(a) * len, 1.2F, CloneAnimator.argb(16767082, 0.45F * grow)
               );
            }
         }
      } else {
         if (held && grow > 0.0F) {
            float tr = 1.0F + 0.06F * sin(f.t * 0.05F);
            CloneAnimator.glow(f.ctx, cx, cy, r * 2.2F * tr, f.t * 0.002F, 16767082, 0.08F * grow);
            CloneAnimator.quad(f.ctx, cx, cy, r * 1.7F * tr, r * 1.7F * tr, f.t * 0.003F, CloneAnimator.argb(16771488, 0.9F));
            CloneAnimator.quad(f.ctx, cx, cy, r * 1.7F * tr, r * 1.7F * tr, f.t * 0.003F + 0.785F, CloneAnimator.argb(16771488, 0.9F));
            CloneAnimator.quad(f.ctx, cx, cy, r * 1.05F, r * 1.05F, -f.t * 0.004F, CloneAnimator.argb(16777215, 1.0F));
            ellipse(f.ctx, cx, cy, r * 2.2F, r * 0.7F, f.t * 0.0025F, 1.0F, CloneAnimator.argb(16767082, 0.8F * grow), 24);
            ellipse(f.ctx, cx, cy, r * 2.0F, r * 0.6F, -f.t * 0.003F + 1.1F, 1.0F, CloneAnimator.argb(16777215, 0.6F * grow), 24);
            converge(f, cx, cy, h * 0.9F, 14, 0.15F, 0.72F, 16767082, 41);
         }

         float lq = f.q(0.73F, 0.95F);
         if (lq > 0.0F && lq < 1.0F) {
            float[] m0 = handsMidAt(f, 0.72F);
            float sx = m0[0];
            float sy = m0[1] - h * 0.06F - h * 0.21F * 1.15F;
            float x = sx + (float)f.dir * CloneAnimator.easeOutCubic(lq) * h * 3.0F;
            float y = CloneAnimator.mix(sy, f.by() - h * 0.6F, lq);
            float rr = h * 0.2F * (1.0F - 0.4F * lq);
            float a = 1.0F - lq;

            for (int i = 1; i <= 4; i++) {
               CloneAnimator.line(
                  f.ctx,
                  x - (float)(f.dir * i) * rr * 0.9F,
                  y,
                  x - (float)(f.dir * (i + 1)) * rr * 0.9F,
                  y,
                  rr * (1.3F - (float)i * 0.25F),
                  CloneAnimator.argb(16767082, 0.4F * a / (float)i)
               );
            }

            CloneAnimator.glow(f.ctx, x, y, rr * 2.0F, f.t * 0.004F, 16767082, 0.09F * a);
            CloneAnimator.quad(f.ctx, x, y, rr * 1.3F, rr * 1.3F, f.t * 0.006F, CloneAnimator.argb(16771488, 0.9F * a));
            CloneAnimator.quad(f.ctx, x, y, rr * 1.3F, rr * 1.3F, f.t * 0.006F + 0.785F, CloneAnimator.argb(16771488, 0.9F * a));
            CloneAnimator.quad(f.ctx, x, y, rr * 0.8F, rr * 0.8F, -f.t * 0.008F, CloneAnimator.argb(16777215, a));
            CloneAnimator.star4(f.ctx, x, y, rr * 1.2F, CloneAnimator.argb(16777215, a));
         }

         float fl = f.q(0.72F, 0.84F);
         if (fl > 0.0F && fl < 1.0F) {
            CloneAnimator.star4(f.ctx, cx, cy, 14.0F * (1.0F - fl), CloneAnimator.argb(16777215, 1.0F - fl));
         }
      }
   }

   private static float[] handsMidAt(CastAnims.Fx f, float pp) {
      float[] a = f.handAt(pp, true);
      float[] b = f.handAt(pp, false);
      return new float[]{(a[0] + b[0]) / 2.0F, (a[1] + b[1]) / 2.0F};
   }

   static void healPrayer(CastAnims.Fx f) {
      prayerGlow(f, 0.2F, 0.82F);
      float h = f.h;
      float rq = f.q(0.78F, 1.0F);
      if (rq > 0.0F && rq < 1.0F) {
         groundRing(f, f.bx(), h * (0.2F + 0.45F * CloneAnimator.easeOutCubic(rq)), 1.3F, CloneAnimator.argb(6750088, 0.8F * (1.0F - rq)), 0.0F, false);
      }

      if (f.front) {
         healSparkles(f, 0.22F, 0.98F, 12);
         float pq = f.q(0.8F, 1.0F);
         if (pq > 0.0F && pq < 1.0F) {
            float[] top = f.head(0.0F, -11.0F, 0.0F);
            CloneAnimator.plus(f.ctx, top[0], top[1] - pq * 8.0F, 4.0F * (1.0F - pq * 0.4F), CloneAnimator.argb(6750088, 1.0F - pq));
            CloneAnimator.quad(f.ctx, top[0], top[1] - pq * 8.0F, 1.6F, 1.6F, 0.0F, CloneAnimator.argb(16777215, 1.0F - pq));
         }
      }
   }

   private static void prayerGlow(CastAnims.Fx f, float a, float b) {
      float g = CastAnims.win(f.p, a, b, 0.08F);
      if (!(g <= 0.0F)) {
         float[] m = f.handsMid();
         float pulse = 0.85F + 0.15F * sin(f.t * 0.008F);
         if (!f.front) {
            CloneAnimator.glow(f.ctx, f.bx(), f.by() - f.h * 0.55F, f.h * 0.55F * pulse, f.t * 5.0E-4F, 6750088, 0.035F * g);
         } else {
            CloneAnimator.glow(f.ctx, m[0], m[1], f.h * 0.14F * pulse, f.t * 0.002F, 6750088, 0.1F * g);
            CloneAnimator.star4(f.ctx, m[0], m[1], 2.6F * pulse * g, CloneAnimator.argb(16777215, g));
         }
      }
   }

   private static void healSparkles(CastAnims.Fx f, float a, float b, int n) {
      for (int i = 0; i < n; i++) {
         float start = a + CloneAnimator.rnd(i * 7) * (b - a) * 0.6F;
         float q = CloneAnimator.seg(f.p, start, start + 0.35F);
         if (!(q <= 0.0F) && !(q >= 1.0F)) {
            float ang = CloneAnimator.rnd(i * 3) * (float) (Math.PI * 2) + q * 3.0F;
            float x = f.bx() + cos(ang) * f.h * 0.3F;
            float y = f.by() - f.h * (0.1F + 0.2F * CloneAnimator.rnd(i * 11)) - q * f.h * 0.75F;
            float al = bell(q) * (sin(ang) > -0.2F ? 1.0F : 0.55F);
            if (i % 3 == 0) {
               CloneAnimator.plus(f.ctx, x, y, 2.2F, CloneAnimator.argb(6750088, al));
            } else {
               CloneAnimator.star4(f.ctx, x, y, 2.0F, CloneAnimator.argb(i % 3 == 1 ? 16777215 : 6750088, al));
            }
         }
      }
   }

   static void dualHeal(CastAnims.Fx f) {
      prayerGlow(f, 0.2F, 0.68F);
      float h = f.h;
      float wq = f.q(0.66F, 0.98F);
      float rq = f.q(0.66F, 0.95F);
      if (rq > 0.0F && rq < 1.0F) {
         groundRing(f, f.bx(), h * (0.25F + 0.9F * CloneAnimator.easeOutCubic(rq)), 1.5F, CloneAnimator.argb(6750088, 0.8F * (1.0F - rq)), 0.0F, false);
      }

      if (f.front) {
         healSparkles(f, 0.2F, 0.7F, 8);
         if (wq > 0.0F && wq < 1.0F) {
            float cy = f.by() - h * 0.55F;

            for (int s = -1; s <= 1; s += 2) {
               float x = f.bx() + (float)s * CloneAnimator.easeOutCubic(wq) * h * 1.6F;
               float ang = s > 0 ? 0.0F : (float) Math.PI;
               crescent(f.ctx, x - (float)s * h * 0.3F, cy, h * 0.5F, h * 0.12F, ang, 1.6F, CloneAnimator.argb(6750088, 0.6F * (1.0F - wq)));
               crescent(f.ctx, x - (float)s * h * 0.3F, cy, h * 0.5F, h * 0.04F, ang, 1.4F, CloneAnimator.argb(16777215, 1.0F - wq));

               for (int i = 0; i < 4; i++) {
                  CloneAnimator.plus(
                     f.ctx,
                     x - (float)(s * i) * 7.0F,
                     cy + (CloneAnimator.rnd(i * 7 + s) - 0.5F) * h * 0.6F,
                     2.0F,
                     CloneAnimator.argb(6750088, (1.0F - wq) * (1.0F - (float)i / 4.0F))
                  );
               }
            }
         }
      }
   }

   static void angelicHalo(CastAnims.Fx f) {
      float h = f.h;
      float hal = CloneAnimator.easeOutBack(f.q(0.12F, 0.34F)) * (1.0F - f.q(0.9F, 1.0F));
      float[] top = f.head(0.0F, -10.5F, 0.0F);
      float rx = h * 0.17F * hal;
      float ry = h * 0.05F * hal;
      if (hal > 0.0F) {
         CloneAnimator.ringHalf(f.ctx, top[0], top[1], rx, ry, 1.6F, CloneAnimator.argb(16767082, 0.95F), f.t * 0.002F, false, f.front);
      }

      if (!f.front) {
         if (hal > 0.0F) {
            CloneAnimator.glow(f.ctx, top[0], top[1], h * 0.16F * hal, 0.0F, 16767082, 0.06F * hal);
         }

         float wing = CloneAnimator.easeOutBack(f.q(0.18F, 0.46F)) * (1.0F - f.q(0.88F, 1.0F));
         if (wing > 0.0F) {
            float[] root = f.head(0.0F, 3.0F, 2.5F);
            float flap = sin(f.t * 0.006F) * 0.1F;

            for (int s2 = -1; s2 <= 1; s2 += 2) {
               CloneAnimator.glow(f.ctx, root[0] + (float)s2 * h * 0.32F * wing, root[1] - h * 0.12F, h * 0.3F * wing, 0.0F, 16773312, 0.035F * wing);

               for (int i = 0; i < 8; i++) {
                  float a = (float) (-Math.PI / 2) + (float)s2 * (0.5F + (float)i * 0.17F + flap) * wing;
                  float len = h * (0.62F - (float)i * 0.045F) * wing;
                  float ex = root[0] + cos(a) * len;
                  float ey = root[1] + sin(a) * len;
                  float sx = root[0] + cos(a) * len * 0.15F;
                  float sy = root[1] + sin(a) * len * 0.15F;
                  CloneAnimator.line(f.ctx, sx, sy, ex, ey, 3.4F - (float)i * 0.25F, CloneAnimator.argb(16770984, 0.45F * wing));
                  CloneAnimator.line(f.ctx, sx, sy, ex, ey, 1.1F, CloneAnimator.argb(16777215, 0.75F * wing));
                  CloneAnimator.quad(f.ctx, ex, ey, 2.0F, 2.0F, a, CloneAnimator.argb(16777215, 0.8F * wing));
               }
            }
         }
      } else {
         for (int i = 0; i < 7; i++) {
            float start = 0.3F + CloneAnimator.rnd(i * 13) * 0.35F;
            float q = CloneAnimator.seg(f.p, start, start + 0.45F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float x = f.bx() + (CloneAnimator.rnd(i * 5) - 0.5F) * h * 1.1F + sin(f.t * 0.004F + (float)i) * 4.0F;
               float y = f.by() - h * (1.1F - 0.9F * q);
               CloneAnimator.quad(f.ctx, x, y, 3.2F, 1.4F, sin(f.t * 0.004F + (float)i) * 0.6F, CloneAnimator.argb(16777215, 0.85F * bell(q)));
            }
         }

         if (hal > 0.8F) {
            CloneAnimator.star4(f.ctx, top[0] + cos(f.t * 0.004F) * rx, top[1] + sin(f.t * 0.004F) * ry, 2.0F, CloneAnimator.argb(16777215, 0.9F));
         }
      }
   }

   static void ward(CastAnims.Fx f) {
      if (!f.front) {
         float g = CastAnims.win(f.p, 0.2F, 0.9F, 0.08F);
         groundRing(f, f.bx(), f.h * 0.4F * g, 1.0F, CloneAnimator.argb(8830719, 0.55F * g), f.t * 0.001F, true);
      } else {
         float g = CastAnims.win(f.p, 0.2F, 0.9F, 0.08F);
         groundRing(f, f.bx(), f.h * 0.4F * g, 1.0F, CloneAnimator.argb(8830719, 0.55F * g), f.t * 0.001F, true);
         float fade = 1.0F - f.q(0.84F, 1.0F);
         float push = CloneAnimator.easeOutCubic(f.q(0.6F, 0.72F));
         float dx = (float)f.dir * push * f.h * 0.35F;
         float grow = 1.0F + 0.5F * push;
         float trace = CloneAnimator.easeInOutSine(f.q(0.2F, 0.6F));
         if (f.p > 0.2F) {
            float[] c0 = f.handAt(0.2F, true);
            float rr = f.h * 0.2F * grow;
            float cx = c0[0] + (float)f.dir * f.h * 0.12F + dx;
            float cy = c0[1] + f.h * 0.04F;
            float a0 = (float) (-Math.PI / 2);
            float sweep = (float) (Math.PI * 2) * trace;
            int n = 36;
            float px = 0.0F;
            float py = 0.0F;

            for (int i = 0; i <= n; i++) {
               float a = a0 + (float)f.dir * sweep * (float)i / (float)n;
               float x = cx + cos(a) * rr * 0.8F;
               float y = cy + sin(a) * rr;
               if (i > 0) {
                  CloneAnimator.line(f.ctx, px, py, x, y, 3.0F, CloneAnimator.argb(8830719, 0.5F * fade));
                  CloneAnimator.line(f.ctx, px, py, x, y, 1.1F, CloneAnimator.argb(16777215, 0.95F * fade));
               }

               px = x;
               py = y;
            }

            if (trace < 1.0F) {
               CloneAnimator.star4(f.ctx, px, py, 3.0F, CloneAnimator.argb(16777215, 1.0F));
            }

            float ru = f.q(0.55F, 0.66F) * fade;
            if (ru > 0.0F) {
               polygon(f.ctx, cx, cy, rr * 0.72F, 6, f.t * 0.002F, 1.0F, CloneAnimator.argb(8830719, 0.8F * ru));
               polygon(f.ctx, cx, cy, rr * 0.5F, 6, -f.t * 0.003F + (float) (Math.PI / 6), 1.0F, CloneAnimator.argb(16777215, 0.7F * ru));

               for (int i = 0; i < 8; i++) {
                  float a = f.t * 0.002F + (float)i * (float) (Math.PI * 2) / 8.0F;
                  CloneAnimator.quad(f.ctx, cx + cos(a) * rr * 0.8F * 0.88F, cy + sin(a) * rr * 0.88F, 1.6F, 1.6F, a, CloneAnimator.argb(8830719, ru));
               }

               CloneAnimator.glow(f.ctx, cx, cy, rr * 0.9F, f.t * 0.001F, 8830719, 0.05F * ru);
            }

            float sh = f.q(0.68F, 0.9F);
            if (sh > 0.0F && sh < 1.0F) {
               ellipse(f.ctx, cx, cy, rr * 0.8F * (1.1F + sh * 0.5F), rr * (1.1F + sh * 0.5F), 0.0F, 1.2F, CloneAnimator.argb(8830719, 0.7F * (1.0F - sh)), 28);
            }
         }
      }
   }

   static void greatShield(CastAnims.Fx f) {
      float h = f.h;
      float sq = f.q(0.4F, 0.7F);
      if (sq > 0.0F && sq < 1.0F) {
         groundRing(f, f.bx(), h * (0.2F + 0.9F * CloneAnimator.easeOutCubic(sq)), 1.8F, CloneAnimator.argb(11845844, 0.85F * (1.0F - sq)), 0.0F, false);
      }

      if (f.front) {
         float dq = f.q(0.4F, 0.75F);
         if (dq > 0.0F && dq < 1.0F) {
            for (int i = 0; i < 8; i++) {
               float s = i % 2 == 0 ? 1.0F : -1.0F;
               float x = f.bx() + s * (4.0F + CloneAnimator.easeOutCubic(dq) * h * (0.3F + 0.1F * (float)(i / 2)));
               float y = f.fy0 - 1.5F - dq * (2.0F + 3.0F * CloneAnimator.rnd(i));
               float sz = 2.0F + dq * (2.5F + (float)i * 0.3F);
               CloneAnimator.quad(f.ctx, x, y, sz, sz, dq + (float)i, CloneAnimator.argb(13617340, 0.6F * (1.0F - dq)));
            }

            for (int i = 0; i < 5; i++) {
               float a = (float) Math.PI + (float)(i - 2) * 0.35F;
               float l = h * 0.3F * CloneAnimator.easeOutCubic(f.q(0.4F, 0.5F));
               float x0 = f.bx() + (float)f.dir * h * 0.2F;
               CloneAnimator.line(
                  f.ctx,
                  x0,
                  f.fy0,
                  x0 + (float)f.dir * cos(a + (float) Math.PI) * l,
                  f.fy0 + sin(a) * l * 0.18F,
                  1.0F,
                  CloneAnimator.argb(3814464, 0.8F * (1.0F - dq))
               );
            }
         }

         float wall = 1.0F - f.q(0.84F, 1.0F);
         float x0 = f.bx() + (float)f.dir * h * 0.48F;
         float hr = h * 0.13F;
         int k = 0;

         for (int row = 0; row < 3; row++) {
            for (int col = -1; col <= 1; k++) {
               if (row != 2 || col == 0) {
                  float rise = CloneAnimator.easeOutBack(f.q(0.42F + (float)k * 0.03F, 0.58F + (float)k * 0.03F));
                  if (!(rise <= 0.0F)) {
                     float px = x0 + (float)col * hr * 1.6F * 0.6F + (float)(row % 2) * hr * 0.45F * (float)f.dir;
                     float py = f.fy0 - hr * (0.9F + (float)row * 1.5F) - (1.0F - rise) * -hr * 2.0F;
                     float a = Math.min(1.0F, rise) * wall;
                     polygonFill(f, px, py, hr * 0.92F, a);
                     polygon(f.ctx, px, py, hr, 6, (float) (Math.PI / 6), 1.3F, CloneAnimator.argb(15134463, 0.9F * a));
                     CloneAnimator.quad(f.ctx, px - hr * 0.4F, py - hr * 0.3F, 1.1F, 1.1F, 0.0F, CloneAnimator.argb(5923960, a));
                     CloneAnimator.quad(f.ctx, px + hr * 0.4F, py + hr * 0.3F, 1.1F, 1.1F, 0.0F, CloneAnimator.argb(5923960, a));
                  }
               }

               col++;
            }
         }

         float shine = f.q(0.6F, 0.82F);
         if (shine > 0.0F && shine < 1.0F) {
            float y = f.fy0 - h * 0.65F * shine;
            CloneAnimator.line(f.ctx, x0 - h * 0.18F, y, x0 + h * 0.18F, y - 4.0F, 1.4F, CloneAnimator.argb(16777215, 0.8F * bell(shine) * wall));
         }
      }
   }

   private static void polygonFill(CastAnims.Fx f, float cx, float cy, float r, float a) {
      CloneAnimator.quad(f.ctx, cx, cy, r * 1.6F, r * 1.0F, 0.0F, CloneAnimator.argb(11845844, 0.75F * a));
      CloneAnimator.quad(f.ctx, cx, cy, r * 1.0F, r * 1.75F, 0.0F, CloneAnimator.argb(11845844, 0.75F * a));
      CloneAnimator.quad(f.ctx, cx - r * 0.2F, cy - r * 0.2F, r * 0.7F, r * 0.7F, 0.0F, CloneAnimator.argb(14081772, 0.6F * a));
   }

   static void lullaby(CastAnims.Fx f) {
      if (!f.front) {
         float g = CastAnims.win(f.p, 0.15F, 0.92F, 0.1F);
         if (g > 0.0F) {
            CloneAnimator.glow(f.ctx, f.bx(), f.by() - f.h * 0.6F, f.h * 0.5F, f.t * 4.0E-4F, 13480191, 0.03F * g);
         }
      } else {
         float[] mouth = f.head(0.0F, -2.5F, -5.0F);
         int[] cols = new int[]{13480191, 16757986, 11851519};

         for (int i = 0; i < 9; i++) {
            float start = 0.18F + (float)i * 0.075F;
            float q = CloneAnimator.seg(f.p, start, start + 0.42F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float x = mouth[0] + (float)f.dir * q * f.h * (0.9F + 0.3F * CloneAnimator.rnd(i));
               float y = mouth[1] - q * f.h * 0.5F + sin(q * 9.0F + (float)i) * 3.0F;
               float a = bell(q);
               note(f.ctx, x, y, 1.5F, CloneAnimator.argb(cols[i % 3], a));
               if (i % 2 == 0) {
                  CloneAnimator.star4(f.ctx, x - (float)f.dir * 4.0F, y + 3.0F, 1.4F, CloneAnimator.argb(16777215, 0.7F * a));
               }
            }
         }
      }
   }

   static void shiningSoul(CastAnims.Fx f) {
      int col = f.fr.color & 16777215;
      float h = f.h;
      float app = f.q(0.12F, 0.26F);
      float[] m;
      if (f.p < 0.68F) {
         m = f.handsMid();
         m[1] -= h * 0.06F;
      } else {
         float[] m0 = handsMidAt(f, 0.68F);
         float q = f.q(0.68F, 1.0F);
         m = new float[]{m0[0] + (float)f.dir * q * h * 0.4F, m0[1] - h * 0.06F - CloneAnimator.easeOutCubic(q) * h * 0.25F};
      }

      float shine = f.q(0.5F, 0.7F);
      float fade = 1.0F - f.q(0.88F, 1.0F);
      float pulse = 1.0F + 0.12F * sin(f.t * 0.012F) + 0.5F * bell(f.q(0.66F, 0.8F));
      if (f.front) {
         if (app > 0.0F) {
            heart(f.ctx, m[0], m[1], 1.15F * CloneAnimator.easeOutBack(app) * pulse, col, fade);
         }

         for (int i = 0; i < 8; i++) {
            float start = 0.5F + CloneAnimator.rnd(i * 3) * 0.3F;
            float q = CloneAnimator.seg(f.p, start, start + 0.3F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float a = CloneAnimator.rnd(i * 11) * (float) (Math.PI * 2);
               float d = h * 0.1F + CloneAnimator.easeOutCubic(q) * h * 0.35F;
               CloneAnimator.star4(f.ctx, m[0] + cos(a) * d, m[1] + sin(a) * d, 2.2F * (1.0F - q), CloneAnimator.argb(i % 2 == 0 ? 16777215 : col, 1.0F - q));
            }
         }
      } else {
         if (shine > 0.0F) {
            for (int ix = 0; ix < 10; ix++) {
               float a = f.t * 0.0015F + (float)ix * (float) (Math.PI * 2) / 10.0F;
               float len = h * (0.35F + 0.15F * (float)(ix % 2)) * shine * pulse;
               CloneAnimator.line(f.ctx, m[0], m[1], m[0] + cos(a) * len, m[1] + sin(a) * len, 1.6F, CloneAnimator.argb(col, 0.5F * shine * fade));
            }

            CloneAnimator.glow(f.ctx, m[0], m[1], h * 0.3F * pulse, 0.0F, col, 0.06F * shine * fade);
         }
      }
   }

   static void pacify(CastAnims.Fx f) {
      if (f.front) {
         float h = f.h;
         float g = CastAnims.win(f.p, 0.22F, 0.9F, 0.08F);
         float[] hd = f.arm(true, -1.0F, 11.5F, -1.5F);
         if (g > 0.0F) {
            CloneAnimator.glow(f.ctx, hd[0], hd[1], h * 0.12F, f.t * 0.002F, 16757986, 0.08F * g);
            CloneAnimator.star4(f.ctx, hd[0], hd[1], 2.4F * g, CloneAnimator.argb(16777215, g));
         }

         for (int i = 0; i < 12; i++) {
            float start = 0.25F + (float)i * 0.045F;
            float q = CloneAnimator.seg(f.p, start, start + 0.4F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float x = hd[0] + (float)f.dir * q * h * 1.8F;
               float y = hd[1] + sin(q * 7.0F + (float)i * 0.9F) * h * 0.1F - q * h * 0.08F;
               int c = i % 2 == 0 ? 16757986 : 11851519;
               float a = bell(q);
               CloneAnimator.star4(f.ctx, x, y, 2.2F * (1.0F - q * 0.3F), CloneAnimator.argb(c, a));
               CloneAnimator.quad(f.ctx, x, y, 1.0F, 1.0F, 0.0F, CloneAnimator.argb(16777215, a));
            }
         }
      }
   }

   static void sleepMist(CastAnims.Fx f) {
      float h = f.h;
      if (f.front) {
         float[] r = f.hand(true);
         float[] l = f.hand(false);

         for (int i = 0; i < 14; i++) {
            float start = 0.2F + (float)(i % 7) * 0.07F;
            float q = CloneAnimator.seg(f.p, start, start + 0.45F + (f.p > 0.75F ? 0.0F : 0.0F));
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float[] src = i < 7 ? r : l;
               float push = f.p > 0.78F ? (f.p - 0.78F) * 2.5F : 0.0F;
               float x = src[0] + (float)f.dir * (q * h * (0.6F + 0.4F * CloneAnimator.rnd(i)) + push * h * 0.6F);
               float y = src[1] - q * h * 0.18F + sin(q * 6.0F + (float)i) * 4.0F;
               float s = 3.0F + q * h * 0.16F;
               float a = bell(q);
               CloneAnimator.glow(f.ctx, x, y, s, f.t * 0.001F + (float)i, 14477055, 0.06F * a);
               CloneAnimator.glow(f.ctx, x + 2.0F, y - 1.0F, s * 0.7F, -f.t * 0.0012F + (float)i, 13480191, 0.06F * a);
            }
         }

         for (int ix = 0; ix < 4; ix++) {
            float start = 0.4F + (float)ix * 0.12F;
            float q = CloneAnimator.seg(f.p, start, start + 0.4F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float x = f.bx() + (float)f.dir * h * (0.5F + 0.25F * (float)ix) + sin(q * 5.0F + (float)ix) * 3.0F;
               float y = f.by() - h * (0.7F + 0.5F * q);
               zee(f.ctx, x, y, 2.0F + q * 1.5F, 0.9F, CloneAnimator.argb(16777215, 0.9F * bell(q)));
            }
         }
      }
   }

   private static int pc(CastAnims.Fx f, float base, float range, float off, float a) {
      float hh = base + range * sin(f.t * 0.004F + off * (float) (Math.PI * 2));
      return hue(hh, 0.6F, a);
   }

   private static void psiRings(CastAnims.Fx f, float x, float y, float r, float w, float base, float range) {
      if (!(w <= 0.0F)) {
         for (int i = 0; i < 3; i++) {
            float ph = (f.t * 0.0018F + (float)i / 3.0F) % 1.0F;
            float rr = r * (1.0F - ph);
            ellipse(f.ctx, x, y, rr, rr, 0.0F, 1.0F, pc(f, base, range, (float)i * 0.2F, w * bell(ph)), 18);
         }
      }
   }

   static void lifeup(CastAnims.Fx f) {
      float h = f.h;
      float w = CastAnims.win(f.p, 0.22F, 0.96F, 0.08F);
      float rq = f.q(0.5F, 0.85F);
      if (rq > 0.0F && rq < 1.0F) {
         groundRing(f, f.bx(), h * (0.2F + 0.5F * CloneAnimator.easeOutCubic(rq)), 1.3F, pc(f, 0.38F, 0.07F, 0.0F, 0.8F * (1.0F - rq)), 0.0F, false);
      }

      for (int i = 0; i < 16; i++) {
         float ph = (f.t * 9.0E-4F + (float)i / 16.0F) % 1.0F;
         float ang = ph * (float) (Math.PI * 2) * 1.5F + (float)(i % 2) * (float) Math.PI;
         boolean frontSide = sin(ang) >= 0.0F;
         if (frontSide == f.front) {
            float x = f.bx() + cos(ang) * h * 0.32F;
            float y = f.by() - ph * h * 1.05F + sin(ang) * h * 0.05F;
            CloneAnimator.diamond(f.ctx, x, y, 2.2F * (0.6F + 0.4F * sin(ang)), pc(f, 0.38F, 0.07F, (float)i * 0.06F, w * bell(ph)));
         }
      }

      if (f.front) {
         float g = CastAnims.win(f.p, 0.22F, 0.9F, 0.06F);
         if (g > 0.0F) {
            float[] r = f.hand(true);
            float[] l = f.hand(false);

            for (float[] hd : new float[][]{r, l}) {
               polygon(f.ctx, hd[0], hd[1], 3.2F + sin(f.t * 0.01F), 4, f.t * 0.004F, 0.9F, pc(f, 0.38F, 0.07F, 0.3F, g));
               CloneAnimator.quad(f.ctx, hd[0], hd[1], 1.4F, 1.4F, 0.0F, CloneAnimator.argb(16777215, g));
            }

            float pq = f.q(0.55F, 0.9F);
            if (pq > 0.0F && pq < 1.0F) {
               float[] top = f.head(0.0F, -11.0F, 0.0F);
               CloneAnimator.plus(f.ctx, top[0], top[1] - pq * 10.0F, 3.6F, pc(f, 0.38F, 0.07F, 0.0F, 1.0F - pq));
            }
         }
      }
   }

   static void psiFire(CastAnims.Fx f) {
      if (f.front) {
         float h = f.h;
         float[] tmp = f.head(f.dir > 0 ? -3.0F : 3.0F, -5.0F, -2.5F);
         psiRings(f, tmp[0], tmp[1], h * 0.16F, CastAnims.win(f.p, 0.12F, 0.4F, 0.04F), 0.98F, 0.08F);
         float bq = f.q(0.38F, 0.72F);
         if (bq > 0.0F && bq < 1.0F) {
            float[] tip = f.handAt(0.4F, true);
            float x = tip[0] + (float)f.dir * CloneAnimator.easeOutCubic(bq) * h * 2.6F;
            float y = tip[1];
            float s = h * 0.1F;

            for (int i = 4; i >= 0; i--) {
               float qq = Math.max(0.0F, bq - (float)i * 0.04F);
               float xx = tip[0] + (float)f.dir * CloneAnimator.easeOutCubic(qq) * h * 2.6F;
               int c = pc(f, 0.98F, 0.08F, (float)i * 0.15F, (1.0F - bq) * (i == 0 ? 1.0F : 0.5F / (float)i));
               CloneAnimator.line(f.ctx, xx - (float)f.dir * s, y - s * 0.7F, xx + (float)f.dir * s, y, 1.6F, c);
               CloneAnimator.line(f.ctx, xx + (float)f.dir * s, y, xx - (float)f.dir * s, y + s * 0.7F, 1.6F, c);
            }

            CloneAnimator.star4(f.ctx, x, y, 3.0F, CloneAnimator.argb(16777215, 1.0F - bq));

            for (int i = 0; i < 5; i++) {
               float k = ((float)i + 0.5F) / 5.0F;
               float tx = CloneAnimator.mix(tip[0], x, k);
               float ty = y + sin(f.t * 0.02F + (float)i) * 4.0F;
               polygon(f.ctx, tx, ty, 2.2F, 3, f.t * 0.01F + (float)i, 0.8F, pc(f, 0.98F, 0.08F, k, 0.9F * (1.0F - bq) * k));
            }
         }

         float fq = f.q(0.38F, 0.5F);
         if (fq > 0.0F && fq < 1.0F) {
            float[] tip = f.handAt(0.4F, true);
            polygon(f.ctx, tip[0], tip[1], 2.0F + fq * 8.0F, 3, (float) (-Math.PI / 2), 1.2F, pc(f, 0.98F, 0.08F, 0.0F, 1.0F - fq));
         }
      }
   }

   static void psiFreeze(CastAnims.Fx f) {
      if (f.front) {
         float h = f.h;
         float form = CloneAnimator.easeOutBack(f.q(0.12F, 0.6F));
         if (f.p < 0.72F && form > 0.0F) {
            float[] m = f.handsMid();
            float cx = m[0] + (float)f.dir * h * 0.04F;
            float cy = m[1] - h * 0.04F;
            hexCrystal(f, cx, cy, h * 0.12F * form, 1.0F);

            for (int i = 0; i < 4; i++) {
               float a = f.t * 0.004F + (float)i * (float) (Math.PI * 2) / 4.0F;
               CloneAnimator.quad(
                  f.ctx, cx + cos(a) * h * 0.18F * form, cy + sin(a) * h * 0.08F * form, 1.8F, 1.8F, a, pc(f, 0.6F, 0.1F, (float)i * 0.25F, form)
               );
            }
         }

         float sq = f.q(0.68F, 0.95F);
         if (sq > 0.0F && sq < 1.0F) {
            float[] m0 = handsMidAt(f, 0.7F);
            float x = m0[0] + (float)f.dir * CloneAnimator.easeOutCubic(sq) * h * 2.6F;
            float y = m0[1] - h * 0.04F;
            hexCrystal(f, x, y, h * 0.12F * (1.0F - 0.4F * sq), 1.0F - sq);

            for (int i = 1; i <= 3; i++) {
               hexCrystal(f, x - (float)(f.dir * i) * 8.0F, y, h * 0.1F, (1.0F - sq) * 0.25F / (float)i);
            }
         }
      }
   }

   private static void hexCrystal(CastAnims.Fx f, float x, float y, float r, float a) {
      if (!(a <= 0.01F)) {
         polygon(f.ctx, x, y, r, 6, f.t * 0.003F, 1.4F, pc(f, 0.6F, 0.1F, 0.0F, a));
         polygon(f.ctx, x, y, r * 0.62F, 6, -f.t * 0.004F + 0.5F, 1.1F, pc(f, 0.6F, 0.1F, 0.33F, a));
         polygon(f.ctx, x, y, r * 0.3F, 6, f.t * 0.006F, 1.0F, CloneAnimator.argb(16777215, a));

         for (int i = 0; i < 6; i++) {
            float an = f.t * 0.003F + (float)i * (float) (Math.PI * 2) / 6.0F;
            CloneAnimator.line(f.ctx, x + cos(an) * r * 0.3F, y + sin(an) * r * 0.3F, x + cos(an) * r, y + sin(an) * r, 0.7F, pc(f, 0.6F, 0.1F, 0.6F, 0.6F * a));
         }
      }
   }

   static void psiThunder(CastAnims.Fx f) {
      float h = f.h;
      float[] head = f.head(0.0F, -4.0F, 0.0F);
      float w = CastAnims.win(f.p, 0.15F, 0.82F, 0.06F);
      int n = 3;
      if (f.p < 0.82F && w > 0.0F) {
         for (int i = 0; i < n; i++) {
            for (int g = 5; g >= 0; g--) {
               float tt = f.t - (float)g * 30.0F;
               float a = tt * 0.0055F + (float)i * (float) (Math.PI * 2) / (float)n;
               float wob = sin(tt * 0.017F + (float)i * 2.0F) * h * 0.04F;
               boolean frontSide = sin(a) >= 0.0F;
               if (frontSide == f.front) {
                  float x = head[0] + cos(a) * (h * 0.36F + wob);
                  float y = head[1] - h * 0.05F + sin(a) * h * 0.12F + wob;
                  float al = w * (g == 0 ? 1.0F : 0.5F * (1.0F - (float)g / 6.0F));
                  if (g == 0) {
                     ellipse(f.ctx, x, y, 3.2F, 3.2F, 0.0F, 1.1F, pc(f, 0.17F, 0.07F, (float)i * 0.3F, al), 12);
                     CloneAnimator.quad(f.ctx, x, y, 2.2F, 2.2F, tt * 0.01F, CloneAnimator.argb(16777215, al));
                  } else {
                     CloneAnimator.quad(f.ctx, x, y, 1.6F, 1.6F, 0.0F, pc(f, 0.17F, 0.07F, (float)i * 0.3F, al));
                  }
               }
            }
         }
      }

      if (f.front) {
         float[] tmp = f.head(f.dir > 0 ? -3.0F : 3.0F, -5.0F, -2.5F);
         psiRings(f, tmp[0], tmp[1], h * 0.15F, CastAnims.win(f.p, 0.22F, 0.78F, 0.05F), 0.17F, 0.07F);
         float fq = f.q(0.78F, 1.0F);
         if (fq > 0.0F && fq < 1.0F) {
            for (int i = 0; i < n; i++) {
               for (int gx = 4; gx >= 0; gx--) {
                  float qq = Math.max(0.0F, fq - (float)gx * 0.03F);
                  float x = head[0] + (float)f.dir * CloneAnimator.easeOutCubic(qq) * h * 2.6F;
                  float y = head[1] - h * 0.05F + (float)(i - 1) * h * 0.12F * (1.0F - qq) + sin(qq * 14.0F + (float)i * 2.0F) * h * 0.08F;
                  float al = (1.0F - fq) * (gx == 0 ? 1.0F : 0.4F / (float)gx);
                  if (gx == 0) {
                     ellipse(f.ctx, x, y, 3.2F, 3.2F, 0.0F, 1.1F, pc(f, 0.17F, 0.07F, (float)i * 0.3F, al), 12);
                     CloneAnimator.quad(f.ctx, x, y, 2.2F, 2.2F, f.t * 0.01F, CloneAnimator.argb(16777215, al));
                  } else {
                     CloneAnimator.quad(f.ctx, x, y, 1.6F, 1.6F, 0.0F, pc(f, 0.17F, 0.07F, (float)i * 0.3F, al));
                  }
               }
            }
         }
      }
   }

   static void psiShield(CastAnims.Fx f) {
      float h = f.h;
      if (f.front) {
         float cw = CastAnims.win(f.p, 0.15F, 0.46F, 0.05F);
         if (cw > 0.0F) {
            float[] m = f.handsMid();
            polygon(f.ctx, m[0], m[1], 5.0F * cw, 6, f.t * 0.004F, 1.1F, pc(f, 0.68F, 0.08F, 0.0F, cw));
            polygon(f.ctx, m[0], m[1], 3.0F * cw, 6, -f.t * 0.004F + (float) (Math.PI / 6), 1.1F, pc(f, 0.68F, 0.08F, 0.5F, cw));
         }
      }

      float b = CloneAnimator.easeOutBack(f.q(0.44F, 0.6F));
      float fade = 1.0F - f.q(0.82F, 1.0F);
      if (!(b <= 0.0F) && !(fade <= 0.0F)) {
         float cx = f.bx();
         float cy = f.by() - h * 0.52F;
         float rx = h * 0.5F * b;
         float ry = h * 0.6F * b;
         int n = 12;

         for (int i = 0; i < n; i++) {
            float a = (float)i * (float) (Math.PI * 2) / (float)n + f.t * 8.0E-4F;
            boolean frontSide = cos(a) * (float)f.dir >= -0.15F;
            if (frontSide == f.front) {
               float x = cx + sin(a) * rx;
               float y = cy + cos(a) * ry * 0.95F;
               float shim = 0.6F + 0.4F * sin(f.t * 0.008F + (float)i);
               polygon(f.ctx, x, y, h * 0.09F * b, 6, (float) (Math.PI / 6), 1.1F, pc(f, 0.68F, 0.08F, (float)i / (float)n, fade * shim));
            }
         }

         if (f.front) {
            ellipse(f.ctx, cx, cy, rx * 1.05F, ry * 1.05F, 0.0F, 0.8F, pc(f, 0.68F, 0.08F, 0.2F, 0.5F * fade), 32);
         }

         float fl = f.q(0.44F, 0.56F);
         if (f.front && fl > 0.0F && fl < 1.0F) {
            CloneAnimator.glow(f.ctx, cx, cy, h * 0.5F, 0.0F, 16777215, 0.07F * (1.0F - fl));
         }
      }
   }

   static void hypnosis(CastAnims.Fx f) {
      if (f.front) {
         float h = f.h;
         float w = CastAnims.win(f.p, 0.22F, 0.9F, 0.06F);
         if (!(w <= 0.0F)) {
            float[] tip = f.arm(true, -1.0F, 12.0F, -1.0F);
            float[] prev = null;

            for (int i = 0; i <= 26; i++) {
               float k = (float)i / 26.0F;
               float a = -f.t * 0.012F + k * (float) (Math.PI * 2) * 2.2F;
               float r = 1.0F + k * h * 0.13F;
               float[] pt = new float[]{tip[0] + cos(a) * r, tip[1] + sin(a) * r};
               if (prev != null) {
                  CloneAnimator.line(f.ctx, prev[0], prev[1], pt[0], pt[1], 1.0F, pc(f, 0.82F, 0.08F, k, w * (1.0F - k * 0.5F)));
               }

               prev = pt;
            }

            for (int i = 0; i < 6; i++) {
               float ph = (f.t * 0.0011F + (float)i / 6.0F) % 1.0F;
               float x = tip[0] + (float)f.dir * ph * h * 2.0F;
               float r = 3.0F + ph * h * 0.25F;
               ellipse(f.ctx, x, tip[1], r * 0.45F, r, 0.0F, 1.0F, pc(f, 0.82F, 0.08F, (float)i * 0.17F, w * bell(ph) * 0.8F), 16);
            }
         }
      }
   }

   static void psiMagnet(CastAnims.Fx f) {
      float h = f.h;
      float w = CastAnims.win(f.p, 0.22F, 0.88F, 0.06F);
      float[] m = f.handsMid();
      if (!f.front) {
         if (w > 0.0F) {
            CloneAnimator.glow(f.ctx, m[0], m[1], h * 0.18F, 0.0F, 8191944, 0.05F * w);
         }
      } else {
         if (w > 0.0F) {
            for (int i = 0; i < 14; i++) {
               float ph = (f.t * 0.0012F + CloneAnimator.rnd(i * 7)) % 1.0F;
               float sx = m[0] + (float)f.dir * h * (1.6F + CloneAnimator.rnd(i * 3));
               float sy = m[1] + (CloneAnimator.rnd(i * 5) - 0.5F) * h * 1.1F;
               float e = ph * ph;
               float x = CloneAnimator.mix(sx, m[0], e);
               float y = CloneAnimator.mix(sy, m[1], e) + sin(ph * (float) Math.PI) * (CloneAnimator.rnd(i) - 0.5F) * h * 0.3F;
               float px = CloneAnimator.mix(sx, m[0], Math.max(0.0F, e - 0.06F));
               float py = CloneAnimator.mix(sy, m[1], Math.max(0.0F, e - 0.06F));
               CloneAnimator.line(f.ctx, px, py, x, y, 1.0F, pc(f, 0.42F, 0.06F, (float)i * 0.07F, w * ph));
               CloneAnimator.quad(f.ctx, x, y, 1.6F, 1.6F, 0.0F, CloneAnimator.argb(16777215, w * ph));
            }

            for (int s = 0; s < 2; s++) {
               ellipse(
                  f.ctx,
                  m[0],
                  m[1],
                  h * (0.1F + 0.04F * (float)s),
                  h * 0.05F,
                  f.t * 0.004F * (s == 0 ? 1.0F : -1.3F),
                  0.9F,
                  pc(f, 0.42F, 0.06F, (float)s * 0.5F, 0.7F * w),
                  18
               );
            }
         }

         float aq = f.q(0.84F, 1.0F);
         if (aq > 0.0F && aq < 1.0F) {
            CloneAnimator.star4(f.ctx, m[0], m[1], 6.0F * (1.0F - aq) + 1.0F, CloneAnimator.argb(16777215, 1.0F - aq));
         }
      }
   }

   static void brainshock(CastAnims.Fx f) {
      if (f.front) {
         float h = f.h;
         float[] hd = f.head(0.0F, -4.0F, 0.0F);
         float c = CastAnims.win(f.p, 0.06F, 0.48F, 0.05F);
         if (c > 0.0F) {
            for (int i = 0; i < 2; i++) {
               float ph = (f.t * 0.0022F + (float)i * 0.5F) % 1.0F;
               zigRing(f.ctx, hd[0], hd[1], h * 0.32F * (1.0F - ph) + 3.0F, 2.0F, 9, f.t * 0.003F, 1.0F, pc(f, 0.88F, 0.06F, (float)i * 0.5F, c * bell(ph)));
            }
         }

         float bq = f.q(0.46F, 0.8F);
         if (bq > 0.0F && bq < 1.0F) {
            for (int i = 0; i < 3; i++) {
               float qq = CloneAnimator.seg(bq, (float)i * 0.12F, 1.0F);
               if (!(qq <= 0.0F) && !(qq >= 1.0F)) {
                  zigRing(
                     f.ctx,
                     hd[0],
                     hd[1],
                     4.0F + CloneAnimator.easeOutCubic(qq) * h * 0.7F,
                     3.0F,
                     12,
                     (float)i * 0.4F,
                     1.2F,
                     pc(f, 0.88F, 0.06F, (float)i * 0.3F, 1.0F - qq)
                  );
               }
            }

            for (int ix = 0; ix < 3; ix++) {
               float y = hd[1] + (float)(ix - 1) * h * 0.14F;
               float len = CloneAnimator.easeOutCubic(bq) * h * 2.2F;
               float px = hd[0];
               float py = y;

               for (int s = 1; s <= 8; s++) {
                  float x = hd[0] + (float)f.dir * len * (float)s / 8.0F;
                  float yy = y + (s % 2 == 0 ? -3.0F : 3.0F);
                  CloneAnimator.line(f.ctx, px, py, x, yy, 1.0F, pc(f, 0.88F, 0.06F, (float)ix * 0.3F + (float)s * 0.05F, 1.0F - bq));
                  px = x;
                  py = yy;
               }
            }
         }
      }
   }

   static void psiRockin(CastAnims.Fx f) {
      float h = f.h;
      float[] beats = new float[]{0.3F, 0.42F, 0.62F};

      for (int bi = 0; bi < beats.length; bi++) {
         float q = f.q(beats[bi], beats[bi] + 0.2F);
         if (!(q <= 0.0F) && !(q >= 1.0F)) {
            groundRing(f, f.bx(), h * (0.15F + 0.7F * CloneAnimator.easeOutCubic(q)), 1.4F, hue((float)bi * 0.33F + q * 0.5F, 0.6F, 1.0F - q), 0.0F, false);
         }
      }

      if (f.front) {
         float jq = f.q(0.28F, 0.6F);
         if (jq > 0.0F && jq < 1.0F) {
            for (int i = 0; i < 10; i++) {
               float a = (float)i * (float) (Math.PI * 2) / 10.0F + 0.3F;
               float d = 4.0F + CloneAnimator.easeOutCubic(jq) * h * (0.45F + 0.15F * (float)(i % 2));
               float x = f.bx() + cos(a) * d;
               float y = f.by() - h * 0.55F + sin(a) * d;
               CloneAnimator.star4(f.ctx, x, y, 3.0F * (1.0F - jq) + 0.6F, hue((float)i / 10.0F + f.t * 6.0E-4F, 0.55F, 1.0F - jq));
            }
         }

         for (int i = 0; i < 6; i++) {
            float start = 0.32F + (float)i * 0.08F;
            float q = CloneAnimator.seg(f.p, start, start + 0.35F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float s = i % 2 == 0 ? 1.0F : -1.0F;
               float x = f.bx() + s * q * h * 0.9F;
               float y = f.by() - h * (0.8F + 0.5F * q) + sin(q * 8.0F) * 3.0F;
               note(f.ctx, x, y, 1.5F, hue((float)i / 6.0F + f.t * 5.0E-4F, 0.55F, bell(q)));
            }
         }

         float fq = f.q(0.82F, 1.0F);
         if (fq > 0.0F && fq < 1.0F) {
            float[] fist = f.hand(true);
            float a = bell(CloneAnimator.seg(fq, 0.0F, 1.0F));
            CloneAnimator.star4(f.ctx, fist[0], fist[1] - 5.0F, 8.0F * a, hue(f.t * 0.001F, 0.5F, a));
            polygon(f.ctx, fist[0], fist[1] - 5.0F, 6.0F + fq * 10.0F, 5, (float) (-Math.PI / 2) + fq, 1.1F, hue(0.15F + f.t * 0.001F, 0.6F, 1.0F - fq));
         }
      }
   }

   static void starstorm(CastAnims.Fx f) {
      float h = f.h;
      float night = CloneAnimator.easeInOutSine(f.q(0.05F, 0.4F)) * (1.0F - f.q(0.9F, 1.0F));
      if (!f.front) {
         if (night > 0.0F) {
            CloneAnimator.glow(f.ctx, f.bx(), f.by() - h * 0.9F, h * 1.4F, 0.0F, 657962, 0.09F * night);
            CloneAnimator.glow(f.ctx, f.bx(), f.by() - h * 1.1F, h * 0.9F, f.t * 4.0E-4F, 2760800, 0.05F * night);
         }
      } else {
         float[] r = f.hand(true);
         float[] l = f.hand(false);
         float up = CastAnims.win(f.p, 0.25F, 0.86F, 0.05F);

         for (int i = 0; i < 16; i++) {
            float ph = (f.t * 0.0012F + CloneAnimator.rnd(i * 13)) % 1.0F;
            float[] src = i % 2 == 0 ? r : l;
            float x = src[0] + (CloneAnimator.rnd(i * 7) - 0.5F) * h * 0.4F * ph;
            float y = src[1] - ph * h * 1.3F;
            float tw = 0.6F + 0.4F * sin(f.t * 0.02F + (float)i * 2.0F);
            CloneAnimator.star4(f.ctx, x, y, (1.6F + 1.4F * (1.0F - ph)) * tw, CloneAnimator.argb(i % 3 == 0 ? 16767082 : 16777215, up * bell(ph)));
         }

         float cons = f.q(0.5F, 0.75F) * (1.0F - f.q(0.9F, 1.0F));
         if (cons > 0.0F) {
            float cx = f.bx();
            float cy = f.by() - h * 1.65F;
            float[][] pts = new float[][]{{-0.5F, 0.1F}, {-0.2F, -0.12F}, {0.1F, 0.02F}, {0.35F, -0.2F}, {0.55F, 0.05F}};

            for (int i = 0; i < pts.length; i++) {
               float x = cx + pts[i][0] * h;
               float y = cy + pts[i][1] * h;
               if (i > 0) {
                  float lx = cx + pts[i - 1][0] * h;
                  float ly = cy + pts[i - 1][1] * h;
                  float k = CloneAnimator.clamp01(cons * (float)pts.length - (float)(i - 1));
                  CloneAnimator.line(f.ctx, lx, ly, CloneAnimator.mix(lx, x, k), CloneAnimator.mix(ly, y, k), 0.8F, CloneAnimator.argb(12571903, 0.7F * cons));
               }

               CloneAnimator.star4(f.ctx, x, y, 2.6F + sin(f.t * 0.01F + (float)i), CloneAnimator.argb(16777215, cons));
            }
         }

         float fq = f.q(0.84F, 1.0F);
         if (fq > 0.0F && fq < 1.0F) {
            float cx = f.bx();
            float cy = f.by() - h * 1.65F;
            CloneAnimator.star4(f.ctx, cx, cy, 14.0F * bell(fq), CloneAnimator.argb(16777215, 1.0F - fq));

            for (int i = 0; i < 5; i++) {
               float x = cx + (float)f.dir * fq * h * (1.2F + (float)i * 0.3F);
               float y = cy + fq * h * (0.6F + 0.15F * (float)i);
               CloneAnimator.line(f.ctx, x, y, x - (float)f.dir * 10.0F, y - 6.0F, 1.2F, CloneAnimator.argb(16767082, 1.0F - fq));
               CloneAnimator.star4(f.ctx, x, y, 2.0F, CloneAnimator.argb(16777215, 1.0F - fq));
            }
         }
      }
   }

   static void offenseUp(CastAnims.Fx f) {
      float h = f.h;
      float w = CastAnims.win(f.p, 0.36F, 0.96F, 0.06F);
      float bq = f.q(0.42F, 0.66F);
      if (bq > 0.0F && bq < 1.0F) {
         groundRing(f, f.bx(), h * (0.2F + 0.6F * CloneAnimator.easeOutCubic(bq)), 1.5F, pc(f, 0.03F, 0.04F, 0.0F, 1.0F - bq), 0.0F, false);
      }

      if (!f.front) {
         if (w > 0.0F) {
            for (int i = 0; i < 8; i++) {
               float x = f.bx() + ((float)i - 3.5F) * h * 0.09F;
               float len = h * (0.5F + 0.4F * CloneAnimator.rnd(i * 3 + (int)(f.t / 70.0F)));
               CloneAnimator.line(f.ctx, x, f.fy0, x, f.fy0 - len, 1.4F, pc(f, 0.03F, 0.04F, (float)i * 0.1F, 0.35F * w));
            }
         }
      } else {
         for (int i = 0; i < 9; i++) {
            float ph = (f.t * 0.0012F + (float)i / 9.0F) % 1.0F;
            float x = f.bx() + (CloneAnimator.rnd(i * 5) - 0.5F) * h * 0.85F;
            float y = f.fy0 - ph * h * 1.2F;
            float s = 3.0F;
            int c = pc(f, 0.03F, 0.04F, (float)i * 0.1F, w * bell(ph));
            CloneAnimator.line(f.ctx, x - s, y + s * 0.6F, x, y - s * 0.4F, 1.4F, c);
            CloneAnimator.line(f.ctx, x, y - s * 0.4F, x + s, y + s * 0.6F, 1.4F, c);
         }

         float fq = f.q(0.42F, 0.58F);
         if (fq > 0.0F && fq < 1.0F) {
            CloneAnimator.star4(f.ctx, f.bx(), f.by() - h * 0.55F, 10.0F * (1.0F - fq), CloneAnimator.argb(16777215, 1.0F - fq));
         }
      }
   }

   static void spy(CastAnims.Fx f) {
      if (f.front) {
         float h = f.h;
         float w = CastAnims.win(f.p, 0.28F, 0.92F, 0.06F);
         if (!(w <= 0.0F)) {
            float[] eye = f.head(f.dir > 0 ? -2.0F : 2.0F, -4.5F, -4.3F);
            float gl = (float)Math.pow((double)Math.max(0.0F, sin(f.t * 0.006F)), 6.0);
            CloneAnimator.star4(f.ctx, eye[0], eye[1], 1.0F + 3.0F * gl, CloneAnimator.argb(16777215, w * (0.4F + 0.6F * gl)));
            float tx = f.fx0 + (float)f.dir * h * 2.2F;
            float ty = f.fy0 - h * 0.55F;
            float lock = CloneAnimator.easeOutCubic(f.q(0.3F, 0.7F));
            float s = h * (0.5F - 0.22F * lock);
            int c = pc(f, 0.46F, 0.05F, 0.0F, w);

            for (int sx = -1; sx <= 1; sx += 2) {
               for (int sy = -1; sy <= 1; sy += 2) {
                  float x = tx + (float)sx * s;
                  float y = ty + (float)sy * s;
                  CloneAnimator.line(f.ctx, x, y, x - (float)sx * s * 0.4F, y, 1.2F, c);
                  CloneAnimator.line(f.ctx, x, y, x, y - (float)sy * s * 0.4F, 1.2F, c);
               }
            }

            CloneAnimator.line(f.ctx, tx - 3.0F, ty, tx + 3.0F, ty, 0.8F, c);
            CloneAnimator.line(f.ctx, tx, ty - 3.0F, tx, ty + 3.0F, 0.8F, c);
            float sc = f.t * 0.0012F % 1.0F;
            CloneAnimator.line(f.ctx, tx - s, ty - s + sc * 2.0F * s, tx + s, ty - s + sc * 2.0F * s, 0.8F, CloneAnimator.argb(16777215, 0.6F * w));

            for (int i = 0; i < 6; i++) {
               float k = ((float)i + 0.5F) / 6.0F;
               CloneAnimator.quad(
                  f.ctx, CloneAnimator.mix(eye[0], tx, k), CloneAnimator.mix(eye[1], ty, k), 1.0F, 1.0F, 0.0F, CloneAnimator.argb(16777215, 0.4F * w)
               );
            }

            float lk = f.q(0.7F, 0.82F);
            if (lk > 0.0F && lk < 1.0F) {
               ellipse(f.ctx, tx, ty, s * (1.0F + lk), s * (1.0F + lk), 0.0F, 1.0F, CloneAnimator.argb(16777215, 1.0F - lk), 24);
            }
         }
      }
   }
}
