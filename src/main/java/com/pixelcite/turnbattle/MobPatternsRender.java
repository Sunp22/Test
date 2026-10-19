package com.pixelcite.turnbattle;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
final class MobPatternsRender {
   private static final float PI = (float) Math.PI;
   private static final float TAU = (float) (Math.PI * 2);
   private static final int CELL = 1;
   private static final int MARGIN = 14;
   private static final float[] BAYER = new float[]{
      0.0F, 0.5F, 0.125F, 0.625F, 0.75F, 0.25F, 0.875F, 0.375F, 0.1875F, 0.6875F, 0.0625F, 0.5625F, 0.9375F, 0.4375F, 0.8125F, 0.3125F
   };

   private MobPatternsRender() {
   }

   private static int op(int rgb) {
      return 0xFF000000 | rgb & 16777215;
   }

   private static int lighten(int rgb, float t) {
      return VfxBatch.lerp(op(rgb), -1, t);
   }

   private static int darken(int rgb, float t) {
      return VfxBatch.lerp(op(rgb), -16777216, t);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float wrap(float a) {
      while (a > (float) Math.PI) {
         a -= (float) (Math.PI * 2);
      }

      while (a < (float) -Math.PI) {
         a += (float) (Math.PI * 2);
      }

      return a;
   }

   private static int seed(Bullet b) {
      return System.identityHashCode(b);
   }

   private static float ix(Bullet b, float d) {
      return b.prevX + (b.x - b.prevX) * d;
   }

   private static float iy(Bullet b, float d) {
      return b.prevY + (b.y - b.prevY) * d;
   }

   private static float heading(Bullet b) {
      return b.vx * b.vx + b.vy * b.vy < 4.0E-4F ? (float) (Math.PI / 2) : (float)Math.atan2((double)b.vy, (double)b.vx);
   }

   static void renderBack(DrawContext ctx, float delta, int bx, int by, int bw, int bh, List<Bullet> bullets, int tick) {
      VfxBatch.draw(ctx, false, 1, v -> {
         for (Bullet b : bullets) {
            if (b.kind != 0 && b.active && b.kind == 21) {
               cloud(v, b, ix(b, delta), iy(b, delta), tick);
            }
         }

         for (Bullet bxx : bullets) {
            if (bxx.kind != 0) {
               if (bxx.pendingTicks > 0) {
                  telegraph(v, bxx, bx, by, bw, bh, tick, false);
               } else if (bxx.active) {
                  activeTelegraph(v, bxx, delta, bx, by, bw, bh, tick, false);
               }
            }
         }

         if (MobPatterns.activeBase == 270 || MobPatterns.activeBase == 271) {
            witherHeads(v, bx, by, bw, tick, false);
         }
      });
      VfxBatch.draw(ctx, true, 1, v -> {
         for (Bullet b : bullets) {
            if (b.kind != 0) {
               if (b.pendingTicks > 0) {
                  telegraph(v, b, bx, by, bw, bh, tick, true);
               } else if (b.active) {
                  activeTelegraph(v, b, delta, bx, by, bw, bh, tick, true);
               }
            }
         }

         if (MobPatterns.activeBase == 270 || MobPatterns.activeBase == 271) {
            witherHeads(v, bx, by, bw, tick, true);
         }

         if (MobPatterns.activeBase == 291) {
            endCrystals(v, bx, by, bw, bh, tick);
         }

         if (MobPatterns.activeBase == 304) {
            float px = (float)bx + (float)bw / 2.0F;
            float py = (float)by + 14.0F;
            float spin = (float)tick * (tick / 60 % 2 == 0 ? 0.19F : -0.19F);
            v.glow(px, py, 9.0F, VfxBatch.col(9226495, 0.35F));
            v.star(px, py, 6.0F, 4, spin, VfxBatch.col(14217471, 0.75F));
            v.ring(px, py, 7.5F, 1.0F, VfxBatch.col(9226495, 0.5F));
         }
      });
   }

   private static void cloud(VfxBatch v, Bullet b, float x, float y, int tick) {
      float fade = Math.min(1.0F, (float)b.age / 6.0F) * Math.min(1.0F, (float)b.timer / 12.0F);
      float a = 0.42F * fade;
      int s = seed(b);

      for (int k = 0; k < 5; k++) {
         float ang = (float)b.age * 0.045F * (float)(k % 2 == 0 ? 1 : -1) + (float)k * (float) (Math.PI * 2) / 5.0F + VfxBatch.hash01(s + k) * 2.0F;
         float d = b.size * (0.25F + 0.2F * VfxBatch.hash01(s * 3 + k));
         v.glow(x + (float)Math.cos((double)ang) * d, y + (float)Math.sin((double)ang) * d, b.size * 0.8F, VfxBatch.col(b.color, a));
      }

      v.glow(x, y, b.size * 0.75F, VfxBatch.col(lighten(b.color, 0.25F), a * 0.9F));
   }

   private static void telegraph(VfxBatch v, Bullet b, int bx, int by, int bw, int bh, int tick, boolean light) {
      int p = b.pendingTicks;
      boolean flash = p / 3 % 2 == 0;
      float a = heading(b);
      float ux = (float)Math.cos((double)a);
      float uy = (float)Math.sin((double)a);
      float cx = Math.max((float)(bx + 2), Math.min((float)(bx + bw - 2), b.x));
      float cy = Math.max((float)(by + 2), Math.min((float)(by + bh - 2), b.y));
      switch (b.kind) {
         case 6:
            float kx = clamp01(1.0F - (float)p / 16.0F);
            float r = 2.2F + 3.5F * kx;
            if (light) {
               v.ringArc(b.x, b.y, r, r, 1.6F, VfxBatch.col(b.color, 0.75F), (float)tick * 0.3F, (float)tick * 0.3F + 4.08407F);
               v.ringArc(b.x, b.y, r * 0.6F, r * 0.6F, 1.3F, VfxBatch.col(14725375, 0.6F), (float)(-tick) * 0.4F, (float)(-tick) * 0.4F + (float) Math.PI);
               v.glow(b.x, b.y, r, VfxBatch.col(b.color, 0.3F));
            } else {
               dashes(v, b.x, b.y, a, 5.0F, 16.0F, 3.0F, VfxBatch.col(14199039, flash ? 0.6F : 0.3F), 0.8F);
            }
            break;
         case 7:
         case 8:
         case 9:
         case 10:
         case 12:
         case 14:
         case 16:
         case 17:
         case 18:
         case 20:
         case 21:
         case 22:
         case 23:
         case 24:
         case 26:
         default:
            float[] seg = clip(b.x, b.y, b.x + ux * 500.0F, b.y + uy * 500.0F, bx, by, bw, bh);
            if (seg == null) {
               if (!light && flash) {
                  v.glow(cx, cy, 3.0F, VfxBatch.col(16728128, 0.7F));
               }

               return;
            }

            if (light) {
               if (b.kind == 2) {
                  v.glow(seg[0], seg[1], 4.0F, VfxBatch.col(16742928, 0.5F));
               }

               return;
            }

            float len = Math.min(110.0F, (float)Math.hypot((double)(seg[2] - seg[0]), (double)(seg[3] - seg[1])));
            int tc = b.hue == 1 ? 2803967 : (b.hue == 2 ? 16752680 : 16730184);
            if (b.hue != 0) {
               len = Math.min(len, 18.0F);
            }

            dashes(v, seg[0], seg[1], a, len, 5.0F, 3.0F, VfxBatch.col(tc, flash ? 0.55F : 0.28F), 1.0F);
            arrowHead(v, seg[0] + ux * 3.0F, seg[1] + uy * 3.0F, a, 3.6F, 1.2F, VfxBatch.col(b.hue != 0 ? tc : 16732240, flash ? 0.95F : 0.6F));
            break;
         case 11:
            float kxx = clamp01(1.0F - (float)p / 28.0F);
            float rx = b.size * 1.5F;
            int c = VfxBatch.col(light ? 12098144 : 14207136, (light ? 0.35F : 0.55F) * (0.4F + 0.6F * kxx));
            if (light) {
               v.glow(b.x, b.y, rx * 1.4F, VfxBatch.col(12098144, 0.25F * kxx));
               return;
            }

            v.polyLine(b.x, b.y, rx, 6, (float)tick * 0.08F, 0.8F, c, 1.0F);
            v.polyLine(b.x, b.y, rx * 0.62F, 6, (float)(-tick) * 0.08F + (float) (Math.PI / 6), 0.6F, VfxBatch.mulA(c, 0.7F), 1.0F);
            break;
         case 13:
            if (!light) {
               return;
            }

            float kx = clamp01(1.0F - (float)p / 16.0F);
            v.star(b.x, b.y, 2.0F + 2.5F * kx, 4, (float)tick * 0.15F, VfxBatch.col(b.color, 0.7F));
            v.ring(b.x, b.y, 3.0F + 2.0F * kx, 1.2F, VfxBatch.col(b.color, 0.4F));
            break;
         case 15:
            if (light) {
               return;
            }

            boolean vertical = Math.abs(uy) > Math.abs(ux);
            float ex = vertical ? cx : (float)(ux > 0.0F ? bx + 1 : bx + bw - 1);
            float ey = vertical ? (float)(uy > 0.0F ? by + 1 : by + bh - 1) : cy;
            v.ellipse(ex, ey, vertical ? 5.0F : 2.0F, vertical ? 2.0F : 5.0F, 0.0F, VfxBatch.col(2760210, 0.6F), VfxBatch.col(2760210, 0.3F));

            for (int kxx = -1; kxx <= 1; kxx += 2) {
               float j = VfxBatch.hash01(seed(b) + kxx) * 2.0F - 1.0F;
               float sx = ex + (vertical ? (float)kxx * 2.5F : 0.0F);
               float sy = ey + (vertical ? 0.0F : (float)kxx * 2.5F);
               v.line(
                  sx,
                  sy,
                  sx + ux * (2.5F + j) + (vertical ? j : 0.0F),
                  sy + uy * (2.5F + j) + (vertical ? 0.0F : j),
                  0.7F,
                  VfxBatch.col(920070, 0.6F),
                  VfxBatch.col(920070, 0.1F)
               );
            }

            if (p <= 6) {
               arrowHead(v, ex + ux * 5.0F, ey + uy * 5.0F, a, 2.8F, 1.0F, VfxBatch.col(16732240, 0.5F));
            }
            break;
         case 19:
            if (b.color != 10479359) {
               return;
            }

            float k = clamp01(1.0F - (float)p / 16.0F);
            if (light) {
               v.glow(b.x, b.y, 3.0F + 7.0F * k, VfxBatch.col(10479359, 0.18F + 0.3F * k));
               return;
            }

            v.star(b.x, b.y, 2.0F + 4.0F * k, 6, k * 0.8F, VfxBatch.col(14219007, 0.35F + 0.5F * k));
            if (flash) {
               v.ring(b.x, b.y, 7.0F - 3.0F * k, 1.0F, VfxBatch.col(10479359, 0.6F));
            }
            break;
         case 25:
            if (!light) {
               return;
            }

            float kx = clamp01(1.0F - (float)p / 16.0F);
            v.ringArc(
               b.x, b.y, 4.0F, 4.0F, 1.6F, VfxBatch.col(16751136, 0.7F * kx + 0.2F), (float)tick * 0.35F, (float)tick * 0.35F + (float) (Math.PI * 3.0 / 2.0)
            );
            v.glow(b.x, b.y, 3.0F + 3.0F * kx, VfxBatch.col(16742928, 0.35F));
            break;
         case 27:
            if (light) {
               return;
            }

            float[] segx = clip(b.x, b.y, b.x + ux * 500.0F, b.y + uy * 500.0F, bx, by, bw, bh);
            if (segx == null) {
               return;
            }

            float w = b.size * 2.0F + 2.0F;
            int band = VfxBatch.col(16722474, flash ? 0.3F : 0.16F);
            v.line(segx[0], segx[1], segx[2], segx[3], w, band, band);
            int edge = VfxBatch.col(16732240, flash ? 0.85F : 0.5F);
            float nx = -uy;
            float hw = w / 2.0F;
            v.line(segx[0] + nx * hw, segx[1] + ux * hw, segx[2] + nx * hw, segx[3] + ux * hw, 1.0F, edge, edge);
            v.line(segx[0] - nx * hw, segx[1] - ux * hw, segx[2] - nx * hw, segx[3] - ux * hw, 1.0F, edge, edge);
            float len = (float)Math.hypot((double)(segx[2] - segx[0]), (double)(segx[3] - segx[1]));
            float off = (float)tick * 1.5F % 16.0F;

            for (float d = off + 6.0F; d < len - 3.0F; d += 16.0F) {
               float px = segx[0] + ux * d;
               float py = segx[1] + uy * d;
               arrowHead(v, px, py, a, 3.4F, 1.0F, VfxBatch.col(16761024, flash ? 0.75F : 0.45F));
            }
      }
   }

   private static void activeTelegraph(VfxBatch v, Bullet b, float delta, int bx, int by, int bw, int bh, int tick, boolean light) {
      float x = ix(b, delta);
      float y = iy(b, delta);
      switch (b.behavior) {
         case 2:
            if (b.p3 >= 0.0F || b.kind != 20) {
               return;
            }

            float pulse = 0.5F + 0.5F * (float)Math.sin((double)((float)tick * 0.5F));
            if (light) {
               v.glow(b.tx, b.ty, 6.0F, VfxBatch.col(12599551, 0.2F + 0.2F * pulse));
               return;
            }

            int c = VfxBatch.col(13660415, 0.55F + 0.3F * pulse);
            v.ring(b.tx, b.ty, 5.0F, 1.2F, c);

            for (int k = 0; k < 4; k++) {
               float ang = (float)k * (float) Math.PI / 2.0F + (float)tick * 0.05F;
               float cs = (float)Math.cos((double)ang);
               float sn = (float)Math.sin((double)ang);
               v.line(b.tx + cs * 4.0F, b.ty + sn * 4.0F, b.tx + cs * 8.0F, b.ty + sn * 8.0F, 0.9F, c, VfxBatch.mulA(c, 0.2F));
            }
         case 3:
         case 5:
         case 7:
         default:
            break;
         case 4:
            float k = clamp01(1.0F - (float)b.timer / 32.0F);
            if (light) {
               if (b.timer < 10 && b.timer / 2 % 2 == 0) {
                  v.glow(x, y, b.p1 * 0.6F, VfxBatch.col(16777215, 0.08F));
               }

               return;
            }

            int c = VfxBatch.col(16728112, 0.25F + 0.45F * k);
            int n = 18;
            float rot = (float)tick * 0.03F;

            for (int i = 0; i < n; i++) {
               float a0 = rot + (float)i * (float) (Math.PI * 2) / (float)n;
               float a1 = a0 + (float) (Math.PI * 2) / (float)n * (0.35F + 0.4F * k);
               v.line(
                  x + (float)Math.cos((double)a0) * b.p1,
                  y + (float)Math.sin((double)a0) * b.p1,
                  x + (float)Math.cos((double)a1) * b.p1,
                  y + (float)Math.sin((double)a1) * b.p1,
                  1.0F,
                  c,
                  c
               );
            }
            break;
         case 6:
            if (b.kind != 7 && b.kind != 38) {
               return;
            }

            float dy = Math.max(0.0F, b.ty - y);
            float r = 3.0F + Math.min(10.0F, dy * 0.12F);
            if (light) {
               v.glow(b.tx, b.ty, r, VfxBatch.col(b.color, 0.25F));
               return;
            }

            int c = VfxBatch.col(lighten(b.color, 0.3F), 0.75F);
            v.ring(b.tx, b.ty, r, 1.4F, c);
            v.line(b.tx - 2.0F, b.ty, b.tx + 2.0F, b.ty, 0.8F, c, c);
            v.line(b.tx, b.ty - 2.0F, b.tx, b.ty + 2.0F, 0.8F, c, c);
            break;
         case 8:
            if (light) {
               return;
            }

            boolean locked = b.timer <= 7;
            float a = (float)Math.atan2((double)(b.ty - y), (double)(b.tx - x));
            float[] seg = clip(x, y, x + (float)Math.cos((double)a) * 500.0F, y + (float)Math.sin((double)a) * 500.0F, bx, by, bw, bh);
            if (seg == null) {
               return;
            }

            float len = Math.min(locked ? 34.0F : 18.0F, (float)Math.hypot((double)(seg[2] - seg[0]), (double)(seg[3] - seg[1])));
            int c = locked ? VfxBatch.col(16732240, 0.5F) : VfxBatch.col(16740464, 0.2F);
            dashes(v, seg[0], seg[1], a, len, 4.0F, 3.0F, c, 0.8F);
            if (locked) {
               v.ring(b.tx, b.ty, 3.0F, 0.8F, VfxBatch.col(16732240, 0.3F));
            }
      }
   }

   static void renderBullets(DrawContext ctx, float delta, int bx, int by, int bw, int bh, List<Bullet> bullets, int tick) {
      ctx.enableScissor(bx - 14, by - 14, bx + bw + 14, by + bh + 14);
      VfxBatch.draw(ctx, false, 1, v -> {
         for (Bullet b : bullets) {
            if (b.active && b.kind != 0 && b.kind != 21) {
               body(v, b, ix(b, delta), iy(b, delta), tick);
            }
         }
      });
      VfxBatch.draw(ctx, true, 1, v -> {
         for (Bullet b : bullets) {
            if (b.active && b.kind != 0) {
               light(v, b, ix(b, delta), iy(b, delta), tick);
            }
         }
      });
      ctx.disableScissor();
   }

   private static void body(VfxBatch v, Bullet b, float x, float y, int tick) {
      float s = b.size;
      float a = heading(b);
      float ux = (float)Math.cos((double)a);
      float uy = (float)Math.sin((double)a);
      float nx = -uy;
      float ny = ux;
      int sd = seed(b);
      switch (b.kind) {
         case 1:
         case 31: {
            boolean bolt = b.kind == 31;
            float back = bolt ? 4.5F : 5.5F;
            float fwd = bolt ? 3.0F : 3.5F;
            float tx = x - ux * back;
            float ty = y - uy * back;
            v.line(tx - ux * 5.0F, ty - uy * 5.0F, tx, ty, 1.0F, VfxBatch.col(b.color, 0.0F), VfxBatch.col(b.color, 0.3F));
            int shaft = bolt ? -10861008 : VfxBatch.lerp(op(9071172), op(b.color), 0.35F);
            v.line(tx, ty, x + ux * fwd, y + uy * fwd, 1.0F, shaft, shaft);
            int fl = bolt ? -1515312 : op(b.color);
            v.tri(tx, ty, fl, tx + ux * 2.2F + nx * 1.8F, ty + uy * 2.2F + ux * 1.8F, fl, tx + ux * 2.4F, ty + uy * 2.4F, fl);
            v.tri(tx, ty, fl, tx + ux * 2.2F - nx * 1.8F, ty + uy * 2.2F - ux * 1.8F, fl, tx + ux * 2.4F, ty + uy * 2.4F, fl);
            float hx = x + ux * fwd;
            float hy = y + uy * fwd;
            int head = bolt ? -3618094 : -2302748;
            v.tri(hx + ux * 2.6F, hy + uy * 2.6F, -1, hx + nx * 1.5F, hy + ux * 1.5F, head, hx - nx * 1.5F, hy - ux * 1.5F, darken(head, 0.3F));
         }
         case 2:
         case 5:
         case 20:
         case 21:
         case 22:
         case 24:
         case 30:
         default:
            break;
         case 3:
         case 4: {
            boolean blue = b.kind == 4;
            int base = blue ? -9528065 : -12961212;
            int top = blue ? -4665089 : -10592662;
            v.line(
               x - ux * s * 2.4F,
               y - uy * s * 2.4F,
               x,
               y,
               s * 1.2F,
               VfxBatch.col(blue ? 5934335 : 2105380, 0.0F),
               VfxBatch.col(blue ? 5934335 : 2105380, 0.35F)
            );
            float wx = s * 1.9F;
            float h = s * 1.75F;
            v.rect(x, y, wx + 2.0F, h + 2.0F, 0.0F, blue ? -2562305 : -6646104);
            v.rect(x, y, wx, h, 0.0F, base);
            v.rect(x, y - h * 0.32F, wx, h * 0.3F, 0.0F, top);
            int hole = blue ? -15062426 : -16448250;
            v.rect(x - wx * 0.24F, y - h * 0.02F, Math.max(1.0F, wx * 0.24F), Math.max(1.0F, h * 0.22F), 0.0F, hole);
            v.rect(x + wx * 0.24F, y - h * 0.02F, Math.max(1.0F, wx * 0.24F), Math.max(1.0F, h * 0.22F), 0.0F, hole);
            v.rect(x, y + h * 0.36F, wx * 0.55F, 1.0F, 0.0F, hole);
            break;
         }
         case 6:
            v.disc(x, y, s, -12673912, -15844814);
            v.rect(x - s * 0.35F, y - s * 0.35F, 1.0F, 1.0F, 0.0F, -4653072);
            break;
         case 7:
            float spin = (float)b.age * 0.28F + VfxBatch.hash01(sd) * (float) (Math.PI * 2);
            float cs = (float)Math.cos((double)spin);
            float sn = (float)Math.sin((double)spin);
            float bx0 = x + -sn * -0.6F;
            float by0 = y + cs * 0.6F;
            v.disc(bx0, by0, s * 0.95F, op(lighten(b.color, 0.35F)), op(b.color));
            float nkx = x + sn * s * 0.95F;
            float nky = y - cs * s * 0.95F;
            v.rect(nkx, nky, 1.6F, 2.4F, spin, -4667176);
            v.rect(nkx + sn * 1.5F, nky - cs * 1.5F, 1.6F, 1.0F, spin, -7710160);
            v.rect(bx0 - s * 0.35F, by0 - s * 0.3F, 1.0F, 1.0F, 0.0F, -1);
            break;
         case 8:
         case 9: {
            boolean magma = b.kind == 9;
            float kxx = Math.min(0.22F, Math.abs(b.vy) * 0.05F);
            float wxx = s * 2.0F * (1.0F - kxx);
            float h = s * 2.0F * (1.0F + kxx);
            y -= (h - s * 2.0F) / 2.0F;
            if (magma) {
               v.rect(x, y, wxx, h, 0.0F, -12971512);

               for (int r = 0; r < 3; r++) {
                  float yy = y - h * 0.3F + (float)r * h * 0.3F;
                  float shift = (float)((b.age / 3 + r) % 3 - 1) * wxx * 0.12F;
                  v.rect(x + shift, yy, wxx * 0.82F, 1.0F, 0.0F, r == 1 ? -20416 : -2072550);
               }

               v.rect(x - wxx * 0.22F, y - h * 0.08F, 1.4F, 1.4F, 0.0F, -8080);
               v.rect(x + wxx * 0.22F, y - h * 0.08F, 1.4F, 1.4F, 0.0F, -8080);
            } else {
               v.rect(x, y, wxx, h, 0.0F, VfxBatch.col(b.color, 0.7F));
               v.rect(x, y + h * 0.06F, wxx * 0.58F, h * 0.58F, 0.0F, op(darken(b.color, 0.25F)));
               v.rect(x - wxx * 0.2F, y - h * 0.1F, Math.max(1.0F, wxx * 0.14F), Math.max(1.0F, h * 0.14F), 0.0F, -15720436);
               v.rect(x + wxx * 0.2F, y - h * 0.1F, Math.max(1.0F, wxx * 0.14F), Math.max(1.0F, h * 0.14F), 0.0F, -15720436);
               v.rect(x + wxx * 0.1F, y + h * 0.2F, Math.max(1.0F, wxx * 0.12F), 1.0F, 0.0F, -15720436);
               v.rect(x - wxx * 0.36F, y - h * 0.36F, Math.max(1.0F, wxx * 0.14F), Math.max(1.0F, h * 0.1F), 0.0F, -857145384);
            }
            break;
         }
         case 10: {
            float rot = (float)b.age * 0.02F + VfxBatch.hash01(sd);
            int c = VfxBatch.col(15592941, 0.85F);

            for (int i = 0; i < 4; i++) {
               float ang = rot + (float)i * (float) Math.PI / 4.0F;
               float dx = (float)Math.cos((double)ang) * s * 1.3F;
               float dy = (float)Math.sin((double)ang) * s * 1.3F;
               v.line(x - dx, y - dy, x + dx, y + dy, 0.8F, c, c);
            }

            v.polyLine(x, y, s * 0.6F, 8, rot, 0.7F, c, 1.0F);
            v.polyLine(x, y, s * 1.1F, 8, rot, 0.6F, VfxBatch.col(15592941, 0.55F), 1.0F);
            break;
         }
         case 11:
            float life = clamp01(1.0F - (float)b.timer / 9.0F);
            float rise = (float)Math.sin((double)(Math.min(1.0F, life * 1.6F) * (float) Math.PI * 0.5F)) * (1.0F - Math.max(0.0F, life - 0.75F) * 3.0F);
            float baseY = y + s * 0.9F;
            v.ellipse(x, baseY, s * 1.3F, 1.4F, 0.0F, -869654504, 1714036760);
            float open = 0.55F * (1.0F - life);

            for (int side = -1; side <= 1; side += 2) {
               float ang = (float) (-Math.PI / 2) + (float)side * open;
               float jx = x + (float)side * s * 0.45F;
               v.spike(jx, baseY, s * 2.1F * rise, s * 0.9F, ang, -1515320, -7699856);
               v.spike(jx - (float)side * 0.9F, baseY, s * 1.4F * rise, s * 0.55F, ang - (float)side * 0.2F, -724764, -6647168);
            }
            break;
         case 12: {
            float flap = (float)Math.sin((double)((float)b.age * 0.9F + (float)sd)) * 0.5F + 0.5F;
            int wing = VfxBatch.col(14215416, 0.55F);
            v.tri(x, y - 0.5F, wing, x - 3.5F, y - 2.5F - flap * 1.5F, wing, x - 2.5F, y + 1.0F, wing);
            v.tri(x, y - 0.5F, wing, x + 3.5F, y - 2.5F - flap * 1.5F, wing, x + 2.5F, y + 1.0F, wing);
            v.rect(x, y + 0.6F, 1.8F, 2.6F, 0.0F, -6637352);
            v.disc(x, y - 1.6F, 1.3F, -2561793, -5715736);
            v.line(x + ux * 1.0F, y + uy * 1.0F, x + ux * 4.0F, y + uy * 4.0F, 0.8F, -4670264, -1);
            break;
         }
         case 13:
            v.rect(x, y, s * 1.2F, s * 1.2F, (float)b.age * 0.2F, op(lighten(b.color, 0.5F)));
            break;
         case 14: {
            int shaft = -12677242;
            int tine = -9447736;
            v.line(x - ux * 7.0F, y - uy * 7.0F, x + ux * 3.0F, y + uy * 3.0F, 1.2F, darken(4099974, 0.3F), shaft);
            float hx = x + ux * 3.0F;
            float hy = y + uy * 3.0F;
            v.line(hx - nx * 2.0F, hy - ux * 2.0F, hx + nx * 2.0F, hy + ux * 2.0F, 1.0F, tine, tine);
            v.line(hx, hy, hx + ux * 4.5F, hy + uy * 4.5F, 1.0F, tine, -2031622);
            v.line(hx + nx * 2.0F, hy + ux * 2.0F, hx + nx * 2.0F + ux * 3.5F, hy + ux * 2.0F + uy * 3.5F, 1.0F, tine, -2031622);
            v.line(hx - nx * 2.0F, hy - ux * 2.0F, hx - nx * 2.0F + ux * 3.5F, hy - ux * 2.0F + uy * 3.5F, 1.0F, tine, -2031622);
            break;
         }
         case 15:
            int skin = op(b.color);
            int sleeve = b.color == 13218426 ? -7570870 : (b.color == 15180960 ? -9807296 : -13726580);
            v.line(x - ux * 8.0F, y - uy * 8.0F, x - ux * 2.5F, y - uy * 2.5F, 3.4F, darken(sleeve, 0.25F), sleeve);
            v.line(x - ux * 2.5F, y - uy * 2.5F, x + ux * 1.5F, y + uy * 1.5F, 3.0F, skin, skin);
            float wig = (float)Math.sin((double)((float)b.age * 0.7F + (float)sd)) * 0.5F;

            for (int fxx = -1; fxx <= 1; fxx++) {
               float fxxx = x + ux * 1.5F + nx * (float)fxx * 1.1F;
               float fyx = y + uy * 1.5F + ny * (float)fxx * 1.1F;
               float fax = a + (float)fxx * 0.25F + wig * 0.3F;
               v.line(fxxx, fyx, fxxx + (float)Math.cos((double)fax) * 2.4F, fyx + (float)Math.sin((double)fax) * 2.4F, 0.9F, skin, darken(b.color, 0.2F));
            }
            break;
         case 16:
            int c = op(b.color);
            int d = darken(b.color, 0.35F);
            float legs = b.age % 4 < 2 ? 1.0F : -1.0F;

            for (int kxx = 0; kxx < 3; kxx++) {
               float px = x - ux * (float)kxx * 1.9F;
               float py = y - uy * (float)kxx * 1.9F;
               v.line(px + nx * 1.6F, py + ny * 1.6F, px + nx * 2.3F + ux * legs * 0.6F, py + ny * 2.3F + uy * legs * 0.6F, 0.7F, d, d);
               v.line(px - nx * 1.6F, py - ny * 1.6F, px - nx * 2.3F - ux * legs * 0.6F, py - ny * 2.3F - uy * legs * 0.6F, 0.7F, d, d);
            }

            v.ellipse(x - ux * 3.8F, y - uy * 3.8F, 1.3F, 1.0F, a, d, d);
            v.ellipse(x - ux * 1.9F, y - uy * 1.9F, 1.9F, 1.5F, a, c, d);
            v.ellipse(x, y, 1.9F, 1.6F, a, lighten(b.color, 0.2F), c);
            v.line(x + ux * 1.6F, y + uy * 1.6F, x + ux * 3.0F + nx * 1.2F, y + uy * 3.0F + ny * 1.2F, 0.6F, d, d);
            v.line(x + ux * 1.6F, y + uy * 1.6F, x + ux * 3.0F - nx * 1.2F, y + uy * 3.0F - ny * 1.2F, 0.6F, d, d);
            break;
         case 17:
            v.disc(x, y, s * 0.8F, VfxBatch.col(16055551, 0.4F), VfxBatch.col(14218495, 0.15F));
            break;
         case 18:
            float k = clamp01(1.0F - (float)b.timer / 32.0F);
            float sc = 1.0F + k * 0.25F;
            float w = s * 2.0F * sc;
            v.rect(x, y, w, w, 0.0F, -11550918);
            v.rect(x - w * 0.28F, y + w * 0.3F, w * 0.22F, w * 0.18F, 0.0F, -12936662);
            v.rect(x + w * 0.3F, y - w * 0.32F, w * 0.2F, w * 0.16F, 0.0F, -9777072);
            int f = -15985654;
            v.rect(x - w * 0.22F, y - w * 0.18F, Math.max(1.0F, w * 0.2F), Math.max(1.0F, w * 0.2F), 0.0F, f);
            v.rect(x + w * 0.22F, y - w * 0.18F, Math.max(1.0F, w * 0.2F), Math.max(1.0F, w * 0.2F), 0.0F, f);
            v.rect(x, y + w * 0.06F, Math.max(1.0F, w * 0.2F), Math.max(1.0F, w * 0.2F), 0.0F, f);
            v.rect(x - w * 0.11F, y + w * 0.24F, Math.max(1.0F, w * 0.12F), Math.max(1.0F, w * 0.18F), 0.0F, f);
            v.rect(x + w * 0.11F, y + w * 0.24F, Math.max(1.0F, w * 0.12F), Math.max(1.0F, w * 0.18F), 0.0F, f);
            int period = b.timer > 20 ? 10 : (b.timer > 10 ? 6 : 3);
            if (b.timer < 30 && b.timer % period < period / 2) {
               v.rect(x, y, w, w, 0.0F, VfxBatch.col(16777215, 0.25F + 0.35F * k));
            }
            break;
         case 19:
            v.shard(x, y, s * 2.6F, s * 1.15F, a, op(lighten(b.color, 0.45F)), op(darken(b.color, 0.35F)));
            break;
         case 23: {
            float flap = (float)Math.sin((double)((float)b.age * 0.45F + (float)sd));
            int body = -11903860;
            int wing = -13286292;
            int mem = -7692088;
            float span = s * (1.4F + 0.5F * flap);

            for (int side = -1; side <= 1; side += 2) {
               float wxx = x - ux * 1.2F + nx * (float)side * span;
               float wy = y - uy * 1.2F + ny * (float)side * span;
               v.tri(x + ux * 1.4F, y + uy * 1.4F, wing, wxx, wy, mem, x - ux * s * 0.9F, y - uy * s * 0.9F, wing);
            }

            v.ellipse(x, y, s * 0.95F, s * 0.5F, a, -10850656, body);
            v.tri(
               x - ux * s * 0.8F,
               y - uy * s * 0.8F,
               body,
               x - ux * s * 1.9F + nx * 1.4F,
               y - uy * s * 1.9F + ny * 1.4F,
               wing,
               x - ux * s * 1.9F - nx * 1.4F,
               y - uy * s * 1.9F - ny * 1.4F,
               wing
            );
            v.rect(x + ux * s * 0.6F + nx * 0.9F, y + uy * s * 0.6F + ny * 0.9F, 1.0F, 1.0F, 0.0F, -7667872);
            v.rect(x + ux * s * 0.6F - nx * 0.9F, y + uy * s * 0.6F - ny * 0.9F, 1.0F, 1.0F, 0.0F, -7667872);
            break;
         }
         case 25: {
            float rot = (float)b.age * 0.09F;
            v.rect(x, y, 2.6F, s * 2.7F, rot, -7712752);
            v.rect(x, y, 1.4F, s * 2.4F, rot, -12208);
            break;
         }
         case 26: {
            float face = a;
            if (b.behavior == 8) {
               face = (float)Math.atan2((double)(b.ty - y), (double)(b.tx - x));
            }

            float fx = (float)Math.cos((double)face);
            float fy = (float)Math.sin((double)face);
            float gx = -fy;
            float gy = fx;
            float jit = b.behavior == 8 && b.timer <= 7 ? ((float)(b.age % 2) - 0.5F) * 0.8F : 0.0F;
            x += gx * jit;
            y += fx * jit;
            int leg = op(darken(b.color, 0.25F));

            for (int side = -1; side <= 1; side += 2) {
               for (int kxx = 0; kxx < 4; kxx++) {
                  float sw = (float)Math.sin((double)((float)b.age * 0.8F + (float)kxx * 1.7F + (float)side)) * 0.5F;
                  float off = ((float)kxx - 1.5F) * 1.1F;
                  float kxxx = x + fx * off + gx * (float)side * 2.6F;
                  float kyx = y + fy * off + gy * (float)side * 2.6F;
                  float ex = x + fx * (off * 1.8F + sw) + gx * (float)side * 4.4F;
                  float ey = y + fy * (off * 1.8F + sw) + gy * (float)side * 4.4F;
                  v.line(x + fx * off * 0.4F, y + fy * off * 0.4F, kxxx, kyx - 0.6F, 0.8F, leg, leg);
                  v.line(kxxx, kyx - 0.6F, ex, ey, 0.7F, leg, leg);
               }
            }

            v.ellipse(x - fx * 1.6F, y - fy * 1.6F, s * 0.75F, s * 0.62F, face, op(lighten(b.color, 0.12F)), op(b.color));
            v.disc(x + fx * 1.3F, y + fy * 1.3F, s * 0.42F, op(darken(b.color, 0.2F)), op(darken(b.color, 0.4F)));
            v.rect(x + fx * 2.1F + gx * 0.8F, y + fy * 2.1F + gy * 0.8F, 1.0F, 1.0F, 0.0F, -55256);
            v.rect(x + fx * 2.1F - gx * 0.8F, y + fy * 2.1F - gy * 0.8F, 1.0F, 1.0F, 0.0F, -55256);
            break;
         }
         case 27:
            int sdk = sd;

            for (int kxx = 1; kxx <= 4; kxx++) {
               float dd = s + 2.0F + (float)kxx * 3.2F;
               float jx = (VfxBatch.hash01(sdk + kxx + b.age) - 0.5F) * 4.0F;
               v.glow(x - ux * dd + nx * jx, y - uy * dd + ny * jx, 2.6F + (float)kxx * 0.4F, VfxBatch.col(12101776, 0.4F - (float)kxx * 0.08F));
            }

            v.rect(x, y, s * 2.3F, s * 1.9F, a, -7706024);
            v.rect(x - ux * s * 0.2F, y - uy * s * 0.2F, s * 1.5F, s * 1.9F, a, -9546684);
            float sx = x + ux * s * 1.05F;
            float sy = y + uy * s * 1.05F;
            v.rect(sx, sy, s * 0.7F, s * 1.3F, a, -11650514);

            for (int side = -1; side <= 1; side += 2) {
               v.spike(sx + nx * (float)side * s * 0.55F, sy + ny * (float)side * s * 0.55F, s * 1.0F, 1.6F, a + (float)side * 0.55F, -725284, -4673376);
            }

            v.rect(x + ux * s * 0.35F + nx * s * 0.55F, y + uy * s * 0.35F + ny * s * 0.55F, 1.2F, 1.2F, 0.0F, -15463416);
            v.rect(x + ux * s * 0.35F - nx * s * 0.55F, y + uy * s * 0.35F - ny * s * 0.55F, 1.2F, 1.2F, 0.0F, -15463416);
            break;
         case 28:
            v.ring(x, y, s * 0.85F, 1.1F, VfxBatch.col(12118271, 0.9F));
            v.rect(x - s * 0.35F, y - s * 0.35F, 1.0F, 1.0F, 0.0F, -285212673);
            break;
         case 29:
            float ta = a + (float) Math.PI;
            v.tri(
               x + (float)Math.cos((double)ta) * s * 1.9F,
               y + (float)Math.sin((double)ta) * s * 1.9F,
               op(b.color),
               x + nx * s * 0.7F,
               y + ux * s * 0.7F,
               op(b.color),
               x - nx * s * 0.7F,
               y - ux * s * 0.7F,
               op(b.color)
            );
            v.disc(x, y, s * 0.8F, op(lighten(b.color, 0.4F)), op(b.color));
            break;
         case 32:
            v.line(x - ux * 7.0F, y - uy * 7.0F, x - ux * 3.0F, y - uy * 3.0F, 0.8F, -9811414, -7706054);
            v.rect(x, y, s * 2.2F, s * 1.15F, a, op(b.color));
            v.rect(x - ux * s * 0.3F, y - uy * s * 0.3F, 1.0F, s * 1.15F, a, -988968);
            v.tri(
               x + ux * s * 1.1F + nx * s * 0.6F,
               y + uy * s * 1.1F + ux * s * 0.6F,
               op(darken(b.color, 0.2F)),
               x + ux * s * 1.1F - nx * s * 0.6F,
               y + uy * s * 1.1F - ux * s * 0.6F,
               op(darken(b.color, 0.2F)),
               x + ux * s * 1.9F,
               y + uy * s * 1.9F,
               -1
            );
            break;
         case 33:
            v.shard(x, y, s * 2.8F, s * 0.95F, a, -3607320, -10576756);
            v.tri(
               x - ux * s * 0.9F + nx * s * 0.9F,
               y - uy * s * 0.9F + ux * s * 0.9F,
               -1533360,
               x - ux * s * 0.2F,
               y - uy * s * 0.2F,
               -1533360,
               x - ux * s * 0.9F - nx * s * 0.9F,
               y - uy * s * 0.9F - ux * s * 0.9F,
               -4691408
            );
            break;
         case 34:
            float ka = b.p1 != 0.0F ? (float)b.age * b.p1 + VfxBatch.hash01(sd) * (float) (Math.PI * 2) : a;
            float kx = (float)Math.cos((double)ka);
            float ky = (float)Math.sin((double)ka);
            float kn = -ky;
            boolean cyan = b.hue == 1;
            int lit = cyan ? lighten(b.color, 0.45F) : -460548;
            int sh = cyan ? darken(b.color, 0.25F) : -5723464;
            v.spike(x - kx * s * 0.4F, y - ky * s * 0.4F, s * 2.4F, s * 1.0F, ka, lit, sh);
            v.rect(x - kx * s * 0.4F, y - ky * s * 0.4F, 1.0F, s * 1.3F, ka, cyan ? darken(b.color, 0.45F) : -8769990);
            int hnd = cyan ? darken(b.color, 0.1F) : -1554342;
            v.line(x - kx * s * 0.5F, y - ky * s * 0.5F, x - kx * s * 1.9F, y - ky * s * 1.9F, s * 0.75F, hnd, darken(hnd & 16777215, 0.25F));
            v.rect(x - kx * s * 1.1F + kn * s * 0.15F, y - ky * s * 1.1F + kx * s * 0.15F, 1.0F, 1.0F, 0.0F, -1996488705);
            break;
         case 35: {
            float fa = b.behavior == 12 && b.timer <= 0 ? a + (float) Math.PI : a;
            float fx = (float)Math.cos((double)fa);
            float fy = (float)Math.sin((double)fa);
            float gx = -fy;
            int glove = op(b.color);
            int dk = darken(b.color, 0.3F);
            int hl = lighten(b.color, 0.35F);
            v.line(x - fx * s * 2.6F, y - fy * s * 2.6F, x - fx * s * 1.0F, y - fy * s * 1.0F, s * 0.95F, -2567480, -856346);
            v.line(
               x - fx * s * 1.55F + gx * s * 0.42F,
               y - fy * s * 1.55F + fx * s * 0.42F,
               x - fx * s * 1.55F - gx * s * 0.42F,
               y - fy * s * 1.55F - fx * s * 0.42F,
               1.0F,
               -2084822,
               -2084822
            );
            v.ellipse(x, y, s * 1.0F, s * 0.9F, fa, hl, dk);
            v.ellipse(x - fx * s * 0.3F + gx * s * 0.75F, y - fy * s * 0.3F + fx * s * 0.75F, s * 0.42F, s * 0.32F, fa, glove, dk);
            v.line(
               x + fx * s * 0.45F + gx * s * 0.5F,
               y + fy * s * 0.45F + fx * s * 0.5F,
               x + fx * s * 0.45F - gx * s * 0.6F,
               y + fy * s * 0.45F - fx * s * 0.6F,
               0.8F,
               dk,
               dk
            );
            v.rect(x + fx * s * 0.2F - gx * s * 0.35F, y + fy * s * 0.2F - fx * s * 0.35F, 1.4F, 1.4F, 0.0F, -855638017);
            break;
         }
         case 36:
            float sa = b.vy > 3.0F && Math.abs(b.vx) < 0.5F ? (float) (Math.PI / 2) : (float)b.age * 0.22F + VfxBatch.hash01(sd) * (float) (Math.PI * 2);
            float qx = (float)Math.cos((double)sa);
            float qy = (float)Math.sin((double)sa);
            v.ellipse(x, y, s * 1.25F, s * 0.6F, sa, lighten(b.color, 0.35F), op(b.color));
            v.ellipse(x - qx * s * 0.25F, y - qy * s * 0.25F, s * 0.55F, s * 0.28F, sa, darken(b.color, 0.35F), darken(b.color, 0.45F));
            v.rect(x + qx * s * 1.05F, y + qy * s * 1.05F, 1.6F, 1.6F, sa, lighten(b.color, 0.6F));
            int rib = darken(b.color, 0.2F);
            float wob = (float)Math.sin((double)((float)b.age * 0.4F + (float)sd)) * 1.5F;
            v.line(x - qx * s * 0.6F, y - qy * s * 0.6F, x - qx * s * 1.9F - qy * wob, y - qy * s * 1.9F + qx * wob, 0.7F, rib, VfxBatch.mulA(rib, 0.3F));
            break;
         case 37: {
            float pa = (float)Math.sin((double)((float)b.age * 0.12F + (float)sd)) * 0.5F;
            float wxx = s * 1.9F;
            float h = s * 2.4F;
            if (b.child) {
               wxx *= 0.75F;
               h *= 0.6F;
               pa += (float)b.age * 0.2F;
            }

            v.rect(x, y, wxx, h, pa, op(b.color));
            float px = (float)Math.cos((double)pa);
            float py = (float)Math.sin((double)pa);
            int ln = -6642488;

            for (int r = -1; r <= 1; r++) {
               float oy = (float)r * h * 0.24F;
               v.line(
                  x - px * wxx * 0.35F - py * oy, y - py * wxx * 0.35F + px * oy, x + px * wxx * 0.35F - py * oy, y + py * wxx * 0.35F + px * oy, 0.6F, ln, ln
               );
            }

            v.rect(x - px * wxx * 0.32F, y - py * wxx * 0.32F, 0.7F, h * 0.9F, pa, -2065782);
            break;
         }
         case 38:
            float ea = (float)b.age * 0.2F + VfxBatch.hash01(sd) * (float) (Math.PI * 2);
            v.ellipse(x, y, s * 0.95F, s * 1.2F, ea, -1, -1515316);
            v.rect(x - s * 0.3F, y - s * 0.4F, 1.0F, 1.0F, 0.0F, -1);
            break;
         case 39:
            v.line(x - ux * 8.0F, y - uy * 8.0F, x - ux * 2.0F, y - uy * 2.0F, 1.0F, VfxBatch.col(16773280, 0.0F), VfxBatch.col(16773280, 0.45F));
            v.rect(x, y, s * 2.2F, s * 1.1F, a, op(b.color));
            v.tri(
               x + ux * s * 1.1F + nx * s * 0.55F,
               y + uy * s * 1.1F + ux * s * 0.55F,
               -2568000,
               x + ux * s * 1.1F - nx * s * 0.55F,
               y + uy * s * 1.1F - ux * s * 0.55F,
               -4673376,
               x + ux * s * 1.9F,
               y + uy * s * 1.9F,
               -1
            );
            break;
         case 40:
            v.spike(x - ux * s * 0.5F, y - uy * s * 0.5F, s * 2.8F, s * 1.0F, a, -1, -7433050);
            v.rect(x - ux * s * 0.5F, y - uy * s * 0.5F, 1.2F, s * 1.3F, a, -3628976);
            v.line(x - ux * s * 0.7F, y - uy * s * 0.7F, x - ux * s * 2.1F, y - uy * s * 2.1F, s * 0.75F, -12966880, -14412262);
      }
   }

   private static void light(VfxBatch v, Bullet b, float x, float y, int tick) {
      float s = b.size;
      float a = heading(b);
      float ux = (float)Math.cos((double)a);
      float uy = (float)Math.sin((double)a);
      int sd = seed(b);
      switch (b.kind) {
         case 1:
            if (b.color == 10479359) {
               v.glow(x, y, 4.0F, VfxBatch.col(10479359, 0.25F));
            }
            break;
         case 2:
         case 20:
            boolean dragon = b.kind == 20;
            int hot = dragon ? 15778047 : 16769152;
            int mid = dragon ? 12599551 : 16742928;
            v.glow(x, y, s * 2.3F, VfxBatch.col(mid, 0.5F));
            v.softTaper(x, y, x - ux * s * 3.4F, y - uy * s * 3.4F, s * 1.9F, 0.5F, VfxBatch.col(mid, 0.8F), VfxBatch.col(mid, 0.0F));
            v.disc(x, y, s * 1.05F, VfxBatch.col(16777204, 1.0F), VfxBatch.col(hot, 0.9F));

            for (int k = 0; k < 3; k++) {
               float h = VfxBatch.hash01(sd + k * 7 + tick);
               float ang = a + (float) Math.PI + (h - 0.5F) * 1.6F;
               float d = s * (1.2F + 1.6F * VfxBatch.hash01(sd + k + tick * 3));
               v.rect(x + (float)Math.cos((double)ang) * d, y + (float)Math.sin((double)ang) * d, 1.0F, 1.0F, 0.0F, VfxBatch.col(hot, 0.85F));
            }
         case 3:
         case 5:
         case 10:
         case 11:
         case 15:
         case 16:
         case 27:
         case 31:
         case 35:
         case 37:
         case 38:
         default:
            break;
         case 4:
            v.glow(x, y, s * 2.4F, VfxBatch.col(5934335, 0.55F));
            break;
         case 6:
            v.glow(x, y, s * 2.1F, VfxBatch.col(b.color, 0.42F));

            for (int k = 0; k < 2; k++) {
               float ang = (float)tick * 0.25F + (float)k * (float) Math.PI + VfxBatch.hash01(sd) * (float) (Math.PI * 2);
               v.rect(x + (float)Math.cos((double)ang) * s * 1.7F, y + (float)Math.sin((double)ang) * s * 1.7F, 1.0F, 1.0F, 0.0F, VfxBatch.col(14721279, 0.9F));
            }
            break;
         case 7:
            v.glow(x, y, s * 1.6F, VfxBatch.col(b.color, 0.25F));
            break;
         case 8:
            v.glow(x, y, s * 1.4F, VfxBatch.col(b.color, 0.12F));
            break;
         case 9:
            v.glow(x, y, s * 2.0F, VfxBatch.col(16734736, 0.35F));
            break;
         case 12:
            v.glow(x, y, s * 2.2F, VfxBatch.col(11061480, 0.35F));
            break;
         case 13:
            v.glow(x, y, s * 2.2F, VfxBatch.col(b.color, 0.55F));
            v.star(x, y, s * 1.2F, 4, (float)b.age * 0.25F, VfxBatch.col(16777200, 0.8F));
            break;
         case 14:
            v.glow(x + ux * 4.0F, y + uy * 4.0F, 4.0F, VfxBatch.col(7329480, 0.3F));
            break;
         case 17:
            float rot = (float)b.age * 0.35F + VfxBatch.hash01(sd) * (float) (Math.PI * 2);
            v.ringArc(x, y, s * 0.95F, s * 0.95F, 1.4F, VfxBatch.col(15268607, 0.85F), rot, rot + 3.7699115F);
            v.ringArc(x, y, s * 1.55F, s * 1.55F, 1.2F, VfxBatch.col(12118271, 0.6F), -rot * 0.8F, -rot * 0.8F + (float) Math.PI);
            v.glow(x, y, s * 1.3F, VfxBatch.col(14218495, 0.3F));
            break;
         case 18:
            if (b.timer < 12) {
               v.glow(x, y, s * 2.4F, VfxBatch.col(16777215, 0.25F * (1.0F - (float)b.timer / 12.0F)));
            }
            break;
         case 19:
            v.glow(x, y, s * 1.4F, VfxBatch.col(b.color, (b.color & 16777215) < 4210752 ? 0.06F : 0.22F));
            break;
         case 21:
            float fade = Math.min(1.0F, (float)b.age / 6.0F) * Math.min(1.0F, (float)b.timer / 12.0F);

            for (int k = 0; k < 3; k++) {
               float h = VfxBatch.hash01(sd + k * 13);
               float py = y + b.size * 0.6F - ((float)b.age * 0.6F + h * 30.0F) % (b.size * 1.4F);
               float px = x + (h - 0.5F) * b.size * 1.4F;
               v.rect(px, py, 1.0F, 1.0F, 0.0F, VfxBatch.col(lighten(b.color, 0.4F), 0.7F * fade));
            }
            break;
         case 22:
            v.glow(x, y, s * 2.0F, VfxBatch.col(b.color, 0.5F));
            v.disc(x, y, s * 0.8F, -1, VfxBatch.col(b.color, 0.9F));
            break;
         case 23:
            v.glow(x + ux * s * 0.6F, y + uy * s * 0.6F, 2.0F, VfxBatch.col(9109344, 0.35F));
            break;
         case 24:
            v.glow(x, y, s * 2.3F, VfxBatch.col(3135184, 0.38F));
            v.poly(x, y, s * 1.1F, 4, (float) (Math.PI / 4) + (float)b.age * 0.06F, VfxBatch.col(13172730, 0.95F), VfxBatch.col(3135184, 0.75F));
            break;
         case 25:
            v.glow(x, y, s * 2.1F, VfxBatch.col(16752672, 0.45F));

            for (int k = 0; k < 3; k++) {
               float ang = (float)tick * 0.2F + (float)k * (float) (Math.PI * 2) / 3.0F;
               float fy = y + (float)((tick + k * 5) % 12) * -0.5F + 2.0F;
               v.rect(x + (float)Math.cos((double)ang) * 3.0F, fy, 1.0F, 1.0F, 0.0F, VfxBatch.col(16760896, 0.8F));
            }
            break;
         case 26:
            v.glow(x + ux * 2.0F, y + uy * 2.0F, 2.0F, VfxBatch.col(16719904, 0.3F));
            break;
         case 28:
            v.glow(x, y, s, VfxBatch.col(12118271, 0.12F));
            break;
         case 29:
            v.glow(x, y, s * 1.3F, VfxBatch.col(b.color, 0.2F));
            break;
         case 30:
            float fl = 0.7F + 0.3F * VfxBatch.hash01(sd + tick);
            v.glow(x, y, s * 1.9F, VfxBatch.col(16738832, 0.45F * fl));
            v.rect(x, y, s * 1.1F, s * 1.1F, (float)b.age * 0.15F, VfxBatch.col(16760928, 0.95F * fl));
            break;
         case 32:
            for (int k = 0; k < 4; k++) {
               float d = 7.0F + (float)k * 2.2F + VfxBatch.hash01(sd + tick * 5 + k) * 2.0F;
               float j = (VfxBatch.hash01(sd + tick + k * 3) - 0.5F) * 2.4F;
               v.rect(x - ux * d - uy * j, y - uy * d + ux * j, 1.0F, 1.0F, 0.0F, VfxBatch.col(k == 0 ? 16777184 : 16760896, 0.9F - (float)k * 0.18F));
            }
            break;
         case 33:
            v.glow(x, y, s * 1.4F, VfxBatch.col(10473664, 0.18F));
            break;
         case 34:
            if (b.hue == 1) {
               v.glow(x, y, s * 2.2F, VfxBatch.col(2803967, 0.35F));
            }
            break;
         case 36:
            v.glow(x, y, s * 1.6F, VfxBatch.col(b.color, 0.15F));
            break;
         case 39:
            v.glow(x, y, s * 2.2F, VfxBatch.col(16771200, 0.35F));
            break;
         case 40:
            v.glow(x + ux * s, y + uy * s, s * 1.2F, VfxBatch.col(16724016, 0.22F));
      }
   }

   static void renderDarkness(DrawContext ctx, float delta, int bx, int by, int bw, int bh, float scx, float scy) {
      float d = MobPatterns.prevDarkness + (MobPatterns.darkness - MobPatterns.prevDarkness) * delta;
      if (!(d < 0.02F)) {
         float r0 = 22.0F + (1.0F - d) * 30.0F;
         float r1 = r0 + 26.0F;
         int C = 2;

         for (int j = 0; j < bh / 2; j++) {
            int y = by + j * 2;
            float py = (float)y + 1.0F - scy;
            int runStart = -1;
            int runCol = 0;

            for (int i = 0; i <= bw / 2; i++) {
               int c = 0;
               if (i < bw / 2) {
                  float px = (float)(bx + i * 2) + 1.0F - scx;
                  float dist = (float)Math.sqrt((double)(px * px + py * py));
                  float t = clamp01((dist - r0) / (r1 - r0));
                  float a = d * t * t * (3.0F - 2.0F * t);
                  float q = (float)Math.floor((double)(a * 4.0F + BAYER[(j & 3) * 4 + (i & 3)])) / 4.0F;
                  if (q > 0.0F) {
                     boolean rim = t < 0.45F && d > 0.35F;
                     c = rim ? (int)(Math.min(1.0F, q) * 110.0F) << 24 | 936520 : (int)(Math.min(1.0F, q) * 240.0F) << 24 | 66826;
                  }
               }

               if (runStart >= 0 && c != runCol) {
                  ctx.fill(bx + runStart * 2, y, bx + i * 2, y + 2, runCol);
                  runStart = -1;
               }

               if (c != 0 && runStart < 0) {
                  runStart = i;
                  runCol = c;
               }
            }
         }
      }
   }

   static void renderHazards(DrawContext ctx, float delta, int bx, int by, int bw, int bh, int tick) {
      if (!MobPatterns.BEAMS.isEmpty() || !MobPatterns.RINGS.isEmpty()) {
         ctx.enableScissor(bx, by, bx + bw, by + bh);
         VfxBatch.draw(ctx, false, 1, v -> {
            for (MobPatterns.Beam b : MobPatterns.BEAMS) {
               beam(v, b, delta, tick, false);
            }

            for (MobPatterns.Ring r : MobPatterns.RINGS) {
               ring(v, r, delta, tick, false);
            }
         });
         VfxBatch.draw(ctx, true, 1, v -> {
            for (MobPatterns.Beam b : MobPatterns.BEAMS) {
               beam(v, b, delta, tick, true);
            }

            for (MobPatterns.Ring r : MobPatterns.RINGS) {
               ring(v, r, delta, tick, true);
            }
         });
         ctx.disableScissor();
      }
   }

   private static int beamColor(int style) {
      return switch (style) {
         case 1 -> 3135184;
         case 2 -> 16771168;
         default -> 16728128;
         case 4 -> 12607743;
         case 5 -> 16747224;
         case 6 -> 12599551;
         case 13 -> 16771168;
         case 14 -> 10510576;
         case 15 -> 16752680;
         case 16 -> 16738848;
      };
   }

   private static void beam(VfxBatch v, MobPatterns.Beam b, float delta, int tick, boolean light) {
      float age = (float)(b.age - 1) + delta;
      float ang = b.prevAng + wrap(b.ang - b.prevAng) * delta;
      float ux = (float)Math.cos((double)ang);
      float uy = (float)Math.sin((double)ang);
      float nx = -uy;
      float ny = ux;
      float ex = b.ox + ux * b.len;
      float ey = b.oy + uy * b.len;
      int c = beamColor(b.style);
      if (age < (float)b.warn) {
         float q = clamp01(age / (float)b.warn);
         boolean locked = b.track == 0 || b.age >= b.warn - b.track;
         boolean flash = b.age / 2 % 2 == 0;
         if (b.style == 2) {
            c = VfxBatch.lerp(op(9059583), op(16771168), q) & 16777215;
         }

         if (b.sweep) {
            if (light) {
               float bxx = b.ox + (float)Math.cos((double)b.a0) * b.len;
               float byy = b.oy + (float)Math.sin((double)b.a0) * b.len;
               v.line(b.ox, b.oy, bxx, byy, 1.0F, VfxBatch.col(16777215, 0.25F + 0.4F * q), VfxBatch.col(16777215, 0.1F));
            } else {
               int n = 14;
               int fill = VfxBatch.col(16724016, (flash ? 0.2F : 0.12F) * (0.5F + 0.5F * q));

               for (int i = 0; i < n; i++) {
                  float a0 = b.a0 + (b.a1 - b.a0) * (float)i / (float)n;
                  float a1 = b.a0 + (b.a1 - b.a0) * (float)(i + 1) / (float)n;
                  v.tri(
                     b.ox,
                     b.oy,
                     fill,
                     b.ox + (float)Math.cos((double)a0) * b.len,
                     b.oy + (float)Math.sin((double)a0) * b.len,
                     fill,
                     b.ox + (float)Math.cos((double)a1) * b.len,
                     b.oy + (float)Math.sin((double)a1) * b.len,
                     fill
                  );
                  int rim = VfxBatch.col(16732240, 0.5F + 0.3F * q);
                  v.line(
                     b.ox + (float)Math.cos((double)a0) * b.len,
                     b.oy + (float)Math.sin((double)a0) * b.len,
                     b.ox + (float)Math.cos((double)a1) * b.len,
                     b.oy + (float)Math.sin((double)a1) * b.len,
                     1.0F,
                     rim,
                     rim
                  );
               }
            }
         } else if (b.style == 6) {
            if (!light) {
               int band = VfxBatch.col(c, (flash ? 0.26F : 0.14F) * (0.4F + 0.6F * q));
               v.line(b.ox, b.oy, ex, ey, b.half * 2.0F, band, band);
               int edge = VfxBatch.col(14713087, 0.4F + 0.4F * q);
               v.line(b.ox + nx * b.half, b.oy + ux * b.half, ex + nx * b.half, ey + ux * b.half, 1.0F, edge, edge);
               v.line(b.ox - nx * b.half, b.oy - ux * b.half, ex - nx * b.half, ey - ux * b.half, 1.0F, edge, edge);
            }
         } else if (!light) {
            if (locked && q > 0.3F) {
               int edge = VfxBatch.col(c, flash ? 0.55F : 0.3F);
               dashesLine(v, b.ox + nx * b.half, b.oy + ux * b.half, ux, uy, b.len, 4.0F, 2.0F, edge, 1.0F, tick);
               dashesLine(v, b.ox - nx * b.half, b.oy - ux * b.half, ux, uy, b.len, 4.0F, 2.0F, edge, 1.0F, tick);
            }
         } else {
            float al = 0.25F + 0.55F * q;
            if (locked && flash) {
               al = Math.min(1.0F, al + 0.3F);
            }

            if (b.style == 1 && !locked) {
               float off = age * 5.0F % 12.0F;

               for (float d = off; d < b.len; d += 12.0F) {
                  v.line(b.ox + ux * d, b.oy + uy * d, b.ox + ux * (d + 5.0F), b.oy + uy * (d + 5.0F), 1.0F, VfxBatch.col(c, al), VfxBatch.col(c, al * 0.4F));
               }
            } else {
               v.line(b.ox, b.oy, ex, ey, locked ? 1.4F : 1.0F, VfxBatch.col(locked ? lighten(c, 0.4F) : op(c), al), VfxBatch.col(c, al * 0.6F));
            }
         }
      } else {
         float fadeK = age < (float)(b.warn + b.fire) ? 1.0F : clamp01(1.0F - (age - (float)b.warn - (float)b.fire) / (float)b.fade);
         float f = clamp01((age - (float)b.warn) / (float)b.fire);
         float w = b.half * 2.0F;
         switch (b.style) {
            case 1:
               if (!light) {
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 10.0F, VfxBatch.col(c, 0.5F * fadeK), VfxBatch.col(c, 0.5F * fadeK));
               v.line(b.ox, b.oy, ex, ey, w * 0.7F * (0.6F + 0.4F * fadeK), VfxBatch.col(9109496, 0.85F * fadeK), VfxBatch.col(9109496, 0.85F * fadeK));
               v.line(b.ox, b.oy, ex, ey, 2.0F, VfxBatch.col(16777215, fadeK), VfxBatch.col(16777215, fadeK));
               float off = age * 6.0F % 13.0F;

               for (float d = off; d < b.len; d += 13.0F) {
                  float px = b.ox + ux * d;
                  float py = b.oy + uy * d;
                  ellipseLine(v, px, py, 2.4F, b.half + 4.0F, ang, 1.1F, VfxBatch.col(13172732, 0.8F * fadeK));
               }
               break;
            case 2:
               if (!light) {
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 7.0F, VfxBatch.col(11559167, 0.45F * fadeK), VfxBatch.col(11559167, 0.45F * fadeK));
               v.line(b.ox, b.oy, ex, ey, w * 0.75F, VfxBatch.col(16771168, 0.9F * fadeK), VfxBatch.col(16771168, 0.9F * fadeK));
               v.line(b.ox, b.oy, ex, ey, 1.6F, VfxBatch.col(16777200, fadeK), VfxBatch.col(16777200, fadeK));

               for (float d = age * 4.0F % 6.0F; d < b.len; d += 6.0F) {
                  float sw = (float)Math.sin((double)(d * 0.35F + age * 0.8F)) * b.half * 0.8F;
                  v.rect(b.ox + ux * d + nx * sw, b.oy + uy * d + ny * sw, 1.0F, 1.0F, 0.0F, VfxBatch.col(16777215, 0.8F * fadeK));
               }
               break;
            case 3:
               if (b.sweep) {
                  float lag = wrap(ang - b.a0) * 0.35F;
                  if (light) {
                     int n = 8;

                     for (int i = 0; i < n; i++) {
                        float a0 = ang - lag * (float)i / (float)n;
                        float a1 = ang - lag * (float)(i + 1) / (float)n;
                        float alx = 0.5F * (1.0F - (float)i / (float)n) * fadeK;
                        v.tri(
                           b.ox,
                           b.oy,
                           VfxBatch.col(16777215, 0.0F),
                           b.ox + (float)Math.cos((double)a0) * b.len,
                           b.oy + (float)Math.sin((double)a0) * b.len,
                           VfxBatch.col(16769248, alx),
                           b.ox + (float)Math.cos((double)a1) * b.len,
                           b.oy + (float)Math.sin((double)a1) * b.len,
                           VfxBatch.col(16769248, alx * 0.6F)
                        );
                     }

                     v.line(
                        b.ox + nx * b.half * 0.6F,
                        b.oy + ux * b.half * 0.6F,
                        ex,
                        ey,
                        1.0F,
                        VfxBatch.col(16777215, 0.3F * fadeK),
                        VfxBatch.col(16777215, 0.9F * fadeK)
                     );
                     return;
                  }

                  boolean axe = MobPatterns.activeBase == 201;
                  int blade = axe ? -4669752 : -13882316;
                  int edge = axe ? -1117448 : -8750456;
                  int a8 = (int)(255.0F * fadeK) << 24;
                  blade = blade & 16777215 | a8;
                  edge = edge & 16777215 | a8;
                  float hilt = b.len * 0.22F;
                  v.line(b.ox, b.oy, b.ox + ux * hilt, b.oy + uy * hilt, 2.0F, VfxBatch.col(5913120, fadeK), VfxBatch.col(5913120, fadeK));
                  v.line(
                     b.ox + ux * hilt - nx * 3.0F,
                     b.oy + uy * hilt - ux * 3.0F,
                     b.ox + ux * hilt + nx * 3.0F,
                     b.oy + uy * hilt + ux * 3.0F,
                     1.6F,
                     VfxBatch.col(3816000, fadeK),
                     VfxBatch.col(3816000, fadeK)
                  );
                  if (axe) {
                     v.line(b.ox + ux * hilt, b.oy + uy * hilt, ex, ey, 2.0F, VfxBatch.col(6965802, fadeK), VfxBatch.col(6965802, fadeK));
                     float hx = ex - ux * 9.0F;
                     float hy = ey - uy * 9.0F;
                     v.quad(
                        hx,
                        hy,
                        blade,
                        ex,
                        ey,
                        blade,
                        ex + nx * b.half * 1.6F,
                        ey + ux * b.half * 1.6F,
                        edge,
                        hx + nx * b.half * 1.3F,
                        hy + ux * b.half * 1.3F,
                        edge
                     );
                  } else {
                     v.taper(b.ox + ux * hilt, b.oy + uy * hilt, ex, ey, w * 0.85F, 1.0F, blade, blade);
                     v.line(b.ox + ux * hilt + nx * w * 0.3F, b.oy + uy * hilt + ux * w * 0.3F, ex, ey, 1.0F, edge, edge);
                  }
               } else {
                  float thin = 1.0F - f * 0.6F;
                  if (light) {
                     v.soft(b.ox, b.oy, ex, ey, w + 8.0F, VfxBatch.col(16719904, 0.55F * fadeK), VfxBatch.col(16719904, 0.55F * fadeK));
                     v.line(b.ox, b.oy, ex, ey, w * 0.6F * thin, VfxBatch.col(16777215, 0.95F * fadeK), VfxBatch.col(16777215, 0.95F * fadeK));
                  } else {
                     v.line(
                        b.ox + nx * w * 0.45F,
                        b.oy + ux * w * 0.45F,
                        ex + nx * w * 0.45F,
                        ey + ux * w * 0.45F,
                        1.0F,
                        VfxBatch.col(2097160, 0.7F * fadeK),
                        VfxBatch.col(2097160, 0.7F * fadeK)
                     );
                     v.line(
                        b.ox - nx * w * 0.45F,
                        b.oy - ux * w * 0.45F,
                        ex - nx * w * 0.45F,
                        ey - ux * w * 0.45F,
                        1.0F,
                        VfxBatch.col(2097160, 0.7F * fadeK),
                        VfxBatch.col(2097160, 0.7F * fadeK)
                     );
                  }
               }
               break;
            case 4:
               if (!light) {
                  v.line(b.ox, b.oy, ex, ey, w * 0.5F, VfxBatch.col(1310750, 0.6F * fadeK), VfxBatch.col(1310750, 0.6F * fadeK));
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 8.0F, VfxBatch.col(10502399, 0.55F * fadeK), VfxBatch.col(10502399, 0.55F * fadeK));
               v.line(
                  b.ox + nx * b.half * 0.5F,
                  b.oy + ux * b.half * 0.5F,
                  ex + nx * b.half * 0.5F,
                  ey + ux * b.half * 0.5F,
                  1.2F,
                  VfxBatch.col(14725375, fadeK),
                  VfxBatch.col(14725375, fadeK)
               );
               v.line(
                  b.ox - nx * b.half * 0.5F,
                  b.oy - ux * b.half * 0.5F,
                  ex - nx * b.half * 0.5F,
                  ey - ux * b.half * 0.5F,
                  1.2F,
                  VfxBatch.col(14725375, fadeK),
                  VfxBatch.col(14725375, fadeK)
               );

               for (int k = 0; k < 10; k++) {
                  float d = VfxBatch.hash01(k * 31 + b.age) * b.len;
                  float j = (VfxBatch.hash01(k * 7 + b.age * 3) - 0.5F) * w * 1.6F;
                  v.rect(b.ox + ux * d + nx * j, b.oy + uy * d + ny * j, 1.0F, 1.0F, 0.0F, VfxBatch.col(13794559, 0.9F * fadeK));
               }
               break;
            case 5:
               if (!light) {
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 7.0F, VfxBatch.col(16736464, 0.45F * fadeK), VfxBatch.col(16736464, 0.45F * fadeK));
               v.line(b.ox, b.oy, ex, ey, w * 0.7F, VfxBatch.col(16765172, 0.85F * fadeK), VfxBatch.col(16765172, 0.85F * fadeK));
               v.line(b.ox, b.oy, ex, ey, 1.4F, VfxBatch.col(16777215, fadeK), VfxBatch.col(16777215, fadeK));
               break;
            case 6:
               if (!light) {
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 10.0F, VfxBatch.col(10494176, 0.55F * fadeK), VfxBatch.col(10494176, 0.55F * fadeK));
               v.line(b.ox, b.oy, ex, ey, w * 0.85F, VfxBatch.col(13660415, 0.7F * fadeK), VfxBatch.col(13660415, 0.7F * fadeK));

               for (float d = age * 7.0F % 9.0F; d < b.len; d += 9.0F) {
                  float j = (VfxBatch.hash01((int)d * 13 + b.age) - 0.5F) * w * 0.8F;
                  v.glow(b.ox + ux * d + nx * j, b.oy + uy * d + ny * j, b.half * 0.9F, VfxBatch.col(16042239, 0.45F * fadeK));
               }
               break;
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            default:
               if (light) {
                  v.line(b.ox, b.oy, ex, ey, w, VfxBatch.col(c, fadeK), VfxBatch.col(c, fadeK));
               }
               break;
            case 13:
               if (!light) {
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 6.0F, VfxBatch.col(16764992, 0.5F * fadeK), VfxBatch.col(16764992, 0.5F * fadeK));
               v.line(b.ox, b.oy, ex, ey, w * 0.6F, VfxBatch.col(16774336, fadeK), VfxBatch.col(16774336, fadeK));
               v.line(b.ox, b.oy, ex, ey, 1.0F, VfxBatch.col(16777215, fadeK), VfxBatch.col(16777215, fadeK));
               v.glow(b.ox + ux * 6.0F, b.oy + uy * 6.0F, 7.0F * fadeK, VfxBatch.col(16773280, 0.7F * fadeK));
               break;
            case 14:
               if (!light) {
                  v.line(b.ox, b.oy, ex, ey, w, VfxBatch.col(3807834, 0.35F * fadeK), VfxBatch.col(3807834, 0.35F * fadeK));
                  return;
               }

               v.line(b.ox, b.oy, ex, ey, w * 0.9F, VfxBatch.col(9063128, 0.55F * fadeK), VfxBatch.col(9063128, 0.55F * fadeK));
               v.line(b.ox, b.oy, ex, ey, 1.4F, VfxBatch.col(15259903, fadeK), VfxBatch.col(15259903, fadeK));
               break;
            case 15:
               if (!light) {
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 6.0F, VfxBatch.col(16752680, 0.45F * fadeK), VfxBatch.col(16752680, 0.45F * fadeK));
               v.line(b.ox, b.oy, ex, ey, w * 0.7F, VfxBatch.col(16760928, 0.75F * fadeK), VfxBatch.col(16760928, 0.75F * fadeK));

               for (float d = age * 5.0F % 10.0F; d < b.len; d += 10.0F) {
                  v.line(
                     b.ox + ux * d - nx * b.half,
                     b.oy + uy * d - ny * b.half,
                     b.ox + ux * (d + 3.0F) + nx * b.half,
                     b.oy + uy * (d + 3.0F) + ny * b.half,
                     1.0F,
                     VfxBatch.col(16773328, 0.8F * fadeK),
                     VfxBatch.col(16773328, 0.8F * fadeK)
                  );
               }
               break;
            case 16:
               if (!light) {
                  return;
               }

               v.soft(b.ox, b.oy, ex, ey, w + 8.0F, VfxBatch.col(16726544, 0.5F * fadeK), VfxBatch.col(16726544, 0.5F * fadeK));
               v.line(b.ox, b.oy, ex, ey, w * 0.75F, VfxBatch.col(16747056, 0.75F * fadeK), VfxBatch.col(16760928, 0.85F * fadeK));

               for (float d = age * 6.0F % 9.0F; d < b.len; d += 9.0F) {
                  float j = (VfxBatch.hash01((int)d * 7 + b.age) - 0.5F) * w * 0.7F;
                  v.glow(b.ox + ux * d + nx * j, b.oy + uy * d + ny * j, b.half * 0.8F, VfxBatch.col(16769184, 0.5F * fadeK));
               }
         }
      }
   }

   private static void ring(VfxBatch v, MobPatterns.Ring r, float delta, int tick, boolean light) {
      float rad = r.prevR + (r.r - r.prevR) * delta;
      switch (r.style) {
         case 10:
            if (!light) {
               v.ring(r.cx, r.cy, rad * 0.7F, r.thick * 1.6F, VfxBatch.col(3815994, 0.35F * (1.0F - rad / r.maxR)));
               return;
            }

            float k = clamp01(rad / r.maxR);
            v.disc(r.cx, r.cy, rad * 0.85F, VfxBatch.col(16773312, 0.45F * (1.0F - k)), VfxBatch.col(16744496, 0.2F * (1.0F - k)));
            v.ring(r.cx, r.cy, rad, r.thick * 1.5F, VfxBatch.col(16769184, 0.95F * (1.0F - 0.5F * k)));
            break;
         case 11:
            float ga = r.gapHalf * Math.max(1.0F, 30.0F / Math.max(10.0F, rad));
            if (!r.live()) {
               float q = clamp01((float)r.age / (float)r.delay);
               if (!light) {
                  float gx = (float)Math.cos((double)r.gapAng);
                  float gy = (float)Math.sin((double)r.gapAng);
                  int gc = VfxBatch.col(16777215, 0.35F + 0.5F * q);
                  v.line(r.cx + gx * 5.0F, r.cy + gy * 5.0F, r.cx + gx * (9.0F + 5.0F * q), r.cy + gy * (9.0F + 5.0F * q), 1.0F, gc, VfxBatch.mulA(gc, 0.2F));
                  return;
               }

               float rr = 3.0F + (1.0F - q) * 16.0F;
               float ga0 = r.gapHalf * Math.max(1.0F, 30.0F / Math.max(10.0F, rr));
               v.ringArc(r.cx, r.cy, rr, rr, 1.6F, VfxBatch.col(3135184, 0.3F + 0.5F * q), r.gapAng + ga0, r.gapAng + (float) (Math.PI * 2) - ga0);
               v.glow(r.cx, r.cy, 3.0F + 3.0F * q, VfxBatch.col(9109496, 0.4F + 0.4F * q));
               return;
            }

            if (!light) {
               return;
            }

            v.ringArc(r.cx, r.cy, rad, rad, r.thick * 1.7F, VfxBatch.col(3135184, 0.95F), r.gapAng + ga, r.gapAng + (float) (Math.PI * 2) - ga);
            v.ringArc(r.cx, r.cy, rad, rad, 1.2F, VfxBatch.col(14745596, 0.95F), r.gapAng + ga, r.gapAng + (float) (Math.PI * 2) - ga);
            float inner = rad - 7.0F;
            if (inner > 4.0F) {
               float gi = r.gapHalf * Math.max(1.0F, 30.0F / Math.max(10.0F, inner));
               v.ringArc(r.cx, r.cy, inner, inner, 1.4F, VfxBatch.col(3135184, 0.3F), r.gapAng + gi, r.gapAng + (float) (Math.PI * 2) - gi);
            }
            break;
         case 12:
            float k = clamp01(((float)(r.age - 1) + delta) / 16.0F);
            if (light) {
               v.glow(r.cx, r.cy, 5.0F + 6.0F * k, VfxBatch.col(6295584, 0.3F * k));
               return;
            }

            int petal = -10875362;
            int inner = -6281158;
            float pr = 1.5F + 4.5F * k;

            for (int i = 0; i < 5; i++) {
               float ang = (float)i * (float) (Math.PI * 2) / 5.0F + k * 0.8F;
               v.ellipse(
                  r.cx + (float)Math.cos((double)ang) * pr * 0.7F, r.cy + (float)Math.sin((double)ang) * pr * 0.7F, pr * 0.75F, pr * 0.45F, ang, inner, petal
               );
            }

            v.disc(r.cx, r.cy, 1.4F + k, -15465464, -14022126);
            if (r.age / 2 % 2 == 0) {
               v.ring(r.cx, r.cy, pr + 3.0F, 1.2F, VfxBatch.col(16724032, 0.75F));
            }
      }
   }

   private static void witherHeads(VfxBatch v, int bx, int by, int bw, int tick, boolean light) {
      float[][] h = MobPatterns.witherHeads(bx, by, bw);

      for (int i = 0; i < 3; i++) {
         float x = h[i][0];
         float y = h[i][1] + (float)Math.sin((double)((float)tick * 0.12F + (float)i * 2.1F)) * 1.5F;
         float s = i == 1 ? 1.25F : 1.0F;
         boolean fired = MobPatterns.activeBase == 270 && (tick - 2) % 9 < 3 && (tick - 2) / 9 % 3 == i && tick < 200;
         if (light) {
            v.glow(x, y, 7.0F * s, VfxBatch.col(fired ? 16777215 : 6324479, fired ? 0.35F : 0.12F));
         } else {
            float w = 8.0F * s;
            float hh = 7.0F * s;
            v.rect(x, y, w + 2.0F, hh + 2.0F, 0.0F, -7698792);
            v.rect(x, y, w, hh, 0.0F, -14013902);
            v.rect(x, y - hh * 0.32F, w, hh * 0.3F, 0.0F, -11908522);
            int eye = fired ? -1 : -16448250;
            v.rect(x - w * 0.24F, y, Math.max(1.0F, w * 0.22F), Math.max(1.0F, hh * 0.2F), 0.0F, eye);
            v.rect(x + w * 0.24F, y, Math.max(1.0F, w * 0.22F), Math.max(1.0F, hh * 0.2F), 0.0F, eye);
            v.rect(x, y + hh * 0.36F, w * 0.6F, 1.0F, 0.0F, -16448250);
         }
      }
   }

   private static void endCrystals(VfxBatch v, int bx, int by, int bw, int bh, int tick) {
      for (int corner = 0; corner < 4; corner++) {
         float x = corner % 2 == 0 ? (float)(bx + 6) : (float)(bx + bw - 6);
         float y = corner < 2 ? (float)(by + 6) : (float)(by + bh - 6);
         boolean active = false;

         for (MobPatterns.Beam b : MobPatterns.BEAMS) {
            if (b.style == 5 && Math.abs(b.ox - x) < 1.0F && Math.abs(b.oy - y) < 1.0F) {
               active = true;
            }
         }

         float rot = (float)tick * 0.07F + (float)corner;
         float bob = (float)Math.sin((double)((float)tick * 0.1F + (float)corner)) * 1.0F;
         v.glow(x, y + bob, active ? 9.0F : 6.0F, VfxBatch.col(16736464, active ? 0.55F : 0.25F));
         v.polyLine(x, y + bob, 4.2F, 4, rot, 1.0F, VfxBatch.col(16756976, active ? 0.95F : 0.55F), 0.8F);
         v.polyLine(x, y + bob, 2.6F, 4, -rot * 1.4F, 1.0F, VfxBatch.col(16777215, active ? 0.9F : 0.45F), 1.0F);
      }
   }

   private static void arrowHead(VfxBatch v, float x, float y, float ang, float size, float th, int c) {
      float ux = (float)Math.cos((double)ang);
      float uy = (float)Math.sin((double)ang);
      float nx = -uy;
      v.line(x - ux * size + nx * size * 0.8F, y - uy * size + ux * size * 0.8F, x, y, th, c, c);
      v.line(x - ux * size - nx * size * 0.8F, y - uy * size - ux * size * 0.8F, x, y, th, c, c);
   }

   private static void dashes(VfxBatch v, float x, float y, float ang, float len, float dash, float gap, int c, float w) {
      float ux = (float)Math.cos((double)ang);
      float uy = (float)Math.sin((double)ang);
      float d = 0.0F;

      while (d < len) {
         float e = Math.min(len, d + dash);
         v.line(x + ux * d, y + uy * d, x + ux * e, y + uy * e, w, c, c);
         d += dash + gap;
      }
   }

   private static void dashesLine(VfxBatch v, float x, float y, float ux, float uy, float len, float dash, float gap, int c, float w, int tick) {
      float off = (float)tick * 1.2F % (dash + gap);

      for (float d = off - dash; d < len; d += dash + gap) {
         float s = Math.max(0.0F, d);
         float e = Math.min(len, d + dash);
         if (!(e <= s)) {
            v.line(x + ux * s, y + uy * s, x + ux * e, y + uy * e, w, c, c);
         }
      }
   }

   private static void ellipseLine(VfxBatch v, float cx, float cy, float rx, float ry, float rot, float w, int c) {
      float cs = (float)Math.cos((double)rot);
      float sn = (float)Math.sin((double)rot);
      int n = 14;
      float px = 0.0F;
      float py = 0.0F;

      for (int i = 0; i <= n; i++) {
         float t = (float) (Math.PI * 2) * (float)i / (float)n;
         float ex = (float)Math.cos((double)t) * rx;
         float ey = (float)Math.sin((double)t) * ry;
         float x = cx + ex * cs - ey * sn;
         float y = cy + ex * sn + ey * cs;
         if (i > 0) {
            v.line(px, py, x, y, w, c, c);
         }

         px = x;
         py = y;
      }
   }

   static float[] clip(float x0, float y0, float x1, float y1, int bx, int by, int bw, int bh) {
      float t0 = 0.0F;
      float t1 = 1.0F;
      float dx = x1 - x0;
      float dy = y1 - y0;
      float[] p = new float[]{-dx, dx, -dy, dy};
      float[] q = new float[]{x0 - (float)bx, (float)(bx + bw) - x0, y0 - (float)by, (float)(by + bh) - y0};

      for (int i = 0; i < 4; i++) {
         if (p[i] == 0.0F) {
            if (q[i] < 0.0F) {
               return null;
            }
         } else {
            float t = q[i] / p[i];
            if (p[i] < 0.0F) {
               if (t > t1) {
                  return null;
               }

               if (t > t0) {
                  t0 = t;
               }
            } else {
               if (t < t0) {
                  return null;
               }

               if (t < t1) {
                  t1 = t;
               }
            }
         }
      }

      return new float[]{x0 + dx * t0, y0 + dy * t0, x0 + dx * t1, y0 + dy * t1};
   }
}
