package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
final class OverheadBars {
   private static final int W = 50;
   private static final int H = 6;
   private final Map<Integer, OverheadBars.Anim> anims = new HashMap<>();
   private final Random rng = new Random();

   private static float secs(long since) {
      return (float)(System.nanoTime() - since) / 1.0E9F;
   }

   private static float clamp01(float t) {
      return Math.max(0.0F, Math.min(1.0F, t));
   }

   private static float easeOutCubic(float t) {
      t = clamp01(t);
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   private static float easeOutBack(float t) {
      t = clamp01(t);
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow((double)(t - 1.0F), 3.0) + c1 * (float)Math.pow((double)(t - 1.0F), 2.0);
   }

   private static int argb(int rgb, float a) {
      return Math.round(clamp01(a) * 255.0F) << 24 | rgb & 16777215;
   }

   private static int lerpRgb(int a, int b, float t) {
      t = clamp01(t);
      int r = (int)((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int g = (int)((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = (int)((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | g << 8 | bl;
   }

   private static int hpColor(float f) {
      return f > 0.5F ? lerpRgb(16751134, 16765470, (f - 0.5F) / 0.5F) : lerpRgb(16722474, 16751134, f / 0.5F);
   }

   void forget(int key) {
      this.anims.remove(key);
   }

   boolean recentlyHit(int key) {
      OverheadBars.Anim a = this.anims.get(key);
      return a != null && a.hitAt > 0L && secs(a.hitAt) < 0.9F;
   }

   void draw(DrawContext ctx, TextRenderer tr, int key, int cx, int top, float hp, float maxHp, int lives, int maxLives, List<Object[]> statuses) {
      OverheadBars.Anim a = this.anims.computeIfAbsent(key, k -> new OverheadBars.Anim());
      long now = System.nanoTime();
      float dt = Math.min(0.1F, (float)(now - a.lastNs) / 1.0E9F);
      a.lastNs = now;
      float frac = maxHp > 0.0F ? clamp01(hp / maxHp) : 0.0F;
      if (a.target < 0.0F) {
         a.target = frac;
         a.shown = frac;
      }

      if (frac < a.target - 5.0E-4F) {
         a.ghostFrom = !a.ghostShattered && a.ghostAt > 0L ? Math.max(a.ghostFrom, a.target) : Math.max(a.target, a.shown);
         a.ghostTo = frac;
         a.ghostAt = now;
         a.ghostShattered = false;
         a.hitAt = now;
         a.target = frac;
         a.shown = frac;
      } else if (frac > a.target + 5.0E-4F) {
         a.healFrom = a.shown;
         a.healTo = frac;
         a.healAt = now;
         a.target = frac;

         for (int i = 0; i < 6; i++) {
            OverheadBars.Shard s = new OverheadBars.Shard();
            s.x = (float)cx - 25.0F + 50.0F * (a.healFrom + (a.healTo - a.healFrom) * this.rng.nextFloat());
            s.y = 0.0F;
            s.vx = (this.rng.nextFloat() - 0.5F) * 10.0F;
            s.vy = -18.0F - this.rng.nextFloat() * 20.0F;
            s.size = 2.2F;
            s.life = 0.7F + this.rng.nextFloat() * 0.3F;
            s.rgb = 6750088;
            s.diamond = true;
            s.vrot = 0.0F;
            a.shards.add(s);
         }
      }

      a.shown = a.shown + (a.target - a.shown) * (1.0F - (float)Math.exp((double)(-dt * 9.0F)));
      if (a.lives >= 0 && lives < a.lives && maxLives > 0) {
         a.lifeLostAt = now;
         a.lifeLostIdx = lives;
         a.lives = lives;
         a.pendingLifeShatter = true;
      }

      a.lives = lives;
      float intro = easeOutBack(secs(a.born) / 0.35F);
      float alpha = clamp01(secs(a.born) / 0.25F);
      if (frac <= 0.0F && a.hitAt > 0L) {
         alpha *= 1.0F - clamp01((secs(a.hitAt) - 0.35F) / 0.5F);
      }

      float tHit = a.hitAt > 0L ? secs(a.hitAt) : 9.0F;
      float shake = tHit < 0.32F ? (float)Math.sin((double)(tHit * 70.0F)) * 2.4F * (1.0F - tHit / 0.32F) : 0.0F;
      float pop = tHit < 0.22F ? 1.0F + 0.16F * (1.0F - easeOutCubic(tHit / 0.22F)) : 1.0F;
      float ss = 0.6F;
      int tagsH = statuses.isEmpty() ? 0 : 10;
      int barY = top - 10 - tagsH;
      float bcx = (float)cx + shake;
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(bcx, (float)barY + 3.0F, 0.0F);
      m.scale(pop * intro, pop, 1.0F);
      m.translate(-25.0F, -3.0F, 0.0F);
      boolean low = a.target < 0.25F && a.target > 0.0F;
      int edge = 15790320;
      if (low) {
         edge = lerpRgb(15790320, 16724016, 0.5F + 0.5F * (float)Math.sin((double)(secs(a.born) * 9.0F)));
      }

      if (tHit < 0.12F) {
         edge = 16777215;
      }

      ctx.fill(-2, -1, 52, 9, argb(0, alpha * 0.35F));
      ctx.fill(-2, -2, 52, 8, argb(0, alpha));
      ctx.fill(-1, -1, 51, 7, argb(edge, alpha));
      ctx.fillGradient(0, 0, 50, 6, argb(2754054, alpha), argb(1311234, alpha));
      if (!a.ghostShattered) {
         float tg = secs(a.ghostAt);
         if (tg >= 0.16F) {
            this.shatter(a, bcx, (float)barY);
         } else {
            int gx0 = Math.round(50.0F * a.ghostTo);
            int gx1 = Math.round(50.0F * a.ghostFrom);
            boolean on = tg < 0.09F || (int)(tg * 60.0F) % 2 == 0;
            if (gx1 > gx0) {
               ctx.fill(gx0, 0, gx1, 6, argb(on ? 16777215 : 16763080, alpha));
            }
         }
      }

      int fill = Math.round(50.0F * clamp01(a.shown));
      int col = hpColor(a.shown);
      if (fill > 0) {
         ctx.fillGradient(0, 0, fill, 6, argb(lerpRgb(col, 16777215, 0.25F), alpha), argb(lerpRgb(col, 0, 0.3F), alpha));
         ctx.fill(0, 0, fill, 1, argb(lerpRgb(col, 16777215, 0.6F), alpha));
         ctx.fill(0, 5, fill, 6, argb(lerpRgb(col, 0, 0.45F), alpha));
         float ts = secs(a.born) % 3.2F / 0.7F;
         if (ts < 1.0F) {
            int sx = Math.round(-8.0F + (float)(fill + 8) * easeOutCubic(ts));

            for (int i = 0; i < 4; i++) {
               int x0 = sx + i;
               int x1 = x0 + 1;
               if (x1 > 0 && x0 < fill) {
                  ctx.fill(Math.max(0, x0), 0, Math.min(fill, x1), 6, argb(16777215, alpha * (0.45F - (float)i * 0.1F)));
               }
            }
         }
      }

      if (a.healAt > 0L) {
         float th = secs(a.healAt);
         if (th < 0.6F) {
            int h0 = Math.round(50.0F * a.healFrom);
            int h1 = Math.round(50.0F * Math.min(a.healTo, a.shown + 0.02F));
            if (h1 > h0) {
               ctx.fill(h0, 0, h1, 6, argb(8191898, alpha * (1.0F - th / 0.6F) * 0.85F));
            }
         }
      }

      for (int q = 1; q < 4; q++) {
         int nx = 50 * q / 4;
         ctx.fill(nx, 0, nx + 1, 6, argb(0, alpha * 0.45F));
      }

      m.pop();
      Iterator it = a.shards.iterator();

      while (it.hasNext()) {
         OverheadBars.Shard s = it.next();
         s.age += dt;
         if (s.age >= s.life) {
            it.remove();
         } else {
            s.vy = s.vy + (s.diamond && s.rgb == 6750088 ? -4.0F : 300.0F) * dt;
            s.vx = s.vx * (float)Math.exp((double)(-dt * 1.5F));
            s.x = s.x + s.vx * dt;
            s.y = s.y + s.vy * dt;
            s.rot = s.rot + s.vrot * dt;
            float t = s.age / s.life;
            float sa = alpha * (1.0F - t * t);
            float sx = s.x;
            float sy = (float)(s.rgb == 6750088 ? barY : 0) + s.y;
            if (s.diamond) {
               CloneAnimator.diamond(ctx, sx, sy, s.size * (1.0F - 0.4F * t), argb(s.rgb, sa));
            } else {
               CloneAnimator.quad(ctx, sx + 0.6F, sy + 0.6F, s.size, s.size * 0.8F, s.rot, argb(0, sa * 0.5F));
               CloneAnimator.quad(ctx, sx, sy, s.size, s.size * 0.8F, s.rot, argb(s.rgb, sa));
            }
         }
      }

      if (maxLives > 0) {
         float dx = 8.0F;
         float x0 = bcx - (float)(maxLives - 1) * dx / 2.0F;
         float ly = (float)barY - 7.0F;

         for (int ix = 0; ix < maxLives; ix++) {
            float bob = (float)Math.sin((double)(secs(a.born) * 3.0F + (float)ix * 0.9F)) * 0.8F;
            boolean has = ix < lives;
            if (!has && ix == a.lifeLostIdx && a.pendingLifeShatter) {
               a.pendingLifeShatter = false;

               for (int k = 0; k < 8; k++) {
                  OverheadBars.Shard s = new OverheadBars.Shard();
                  double ang = (double)k * Math.PI / 4.0 + (double)this.rng.nextFloat() * 0.4;
                  float sp = 30.0F + this.rng.nextFloat() * 35.0F;
                  s.x = x0 + (float)ix * dx;
                  s.y = ly;
                  s.vx = (float)Math.cos(ang) * sp;
                  s.vy = (float)Math.sin(ang) * sp - 25.0F;
                  s.size = 1.8F + this.rng.nextFloat();
                  s.life = 0.55F + this.rng.nextFloat() * 0.25F;
                  s.rgb = k % 2 == 0 ? 16734810 : 16777215;
                  s.rot = this.rng.nextFloat() * 6.0F;
                  s.vrot = (this.rng.nextFloat() - 0.5F) * 20.0F;
                  a.shards.add(s);
               }
            }

            float lx = x0 + (float)ix * dx;
            float lyy = ly + bob;
            if (ix == a.lifeLostIdx && a.lifeLostAt > 0L) {
               float tl = secs(a.lifeLostAt);
               if (tl < 0.45F) {
                  CloneAnimator.diamond(ctx, lx, lyy, 3.0F + tl * 18.0F, argb(16777215, alpha * (1.0F - tl / 0.45F) * 0.5F));
               }
            }

            CloneAnimator.diamond(ctx, lx, lyy, 3.4F, argb(0, alpha));
            CloneAnimator.diamond(ctx, lx, lyy, 2.6F, argb(has ? 16730698 : 3809312, alpha));
            if (has) {
               CloneAnimator.diamond(ctx, lx - 0.6F, lyy - 0.6F, 1.0F, argb(16761024, alpha));
            }
         }
      }

      if (!statuses.isEmpty()) {
         Set present = new HashSet<>();
         int rw = 0;

         for (Object[] t : statuses) {
            rw += Math.round((float)tr.getWidth((String)t[0]) * ss) + 6;
         }

         float tx = (float)cx - (float)rw / 2.0F;
         int ty = barY + 6 + 4;

         for (Object[] t : statuses) {
            String label = (String)t[0];
            int c = (Integer)t[1] & 16777215;
            String kind = label.split(" ")[0];
            present.add(kind);
            long born = a.tagBorn.computeIfAbsent(kind, k -> System.nanoTime());
            float tb = secs(born);
            float sc = tb < 0.25F ? 1.0F + 0.35F * (1.0F - easeOutCubic(tb / 0.25F)) : 1.0F;
            float ta = alpha * clamp01(tb / 0.12F);
            int tw = Math.round((float)tr.getWidth(label) * ss) + 4;
            m.push();
            m.translate(tx + (float)tw / 2.0F, (float)ty + 3.5F, 0.0F);
            m.scale(sc, sc, 1.0F);
            m.translate((float)(-tw) / 2.0F, -3.5F, 0.0F);
            ctx.fill(0, 0, tw, 7, argb(0, ta * 0.85F));
            ctx.fill(0, 0, tw, 1, argb(c, ta));
            ctx.fill(0, 6, tw, 7, argb(lerpRgb(c, 0, 0.5F), ta));
            m.push();
            m.translate(2.0F, 1.5F, 0.0F);
            m.scale(ss, ss, 1.0F);
            ctx.drawText(tr, Text.literal(label), 0, 0, argb(c, ta), true);
            m.pop();
            m.pop();
            tx += (float)(tw + 2);
         }

         a.tagBorn.keySet().retainAll(present);
      } else {
         a.tagBorn.clear();
      }
   }

   private void shatter(OverheadBars.Anim a, float bcx, float barY) {
      a.ghostShattered = true;
      float gx0 = bcx - 25.0F + 50.0F * a.ghostTo;
      float gx1 = bcx - 25.0F + 50.0F * a.ghostFrom;
      float w = Math.max(1.0F, gx1 - gx0);
      int n = Math.max(3, Math.min(18, Math.round(w / 2.4F)));
      float mid = (gx0 + gx1) / 2.0F;

      for (int i = 0; i < n; i++) {
         OverheadBars.Shard s = new OverheadBars.Shard();
         s.x = gx0 + w * ((float)i + 0.5F) / (float)n;
         s.y = barY + 1.0F + this.rng.nextFloat() * 4.0F;
         s.vx = (s.x - mid) * 2.2F + (this.rng.nextFloat() - 0.3F) * 46.0F;
         s.vy = -40.0F - this.rng.nextFloat() * 80.0F;
         s.size = 1.8F + this.rng.nextFloat() * 1.8F;
         s.rot = this.rng.nextFloat() * 6.0F;
         s.vrot = (this.rng.nextFloat() - 0.5F) * 26.0F;
         s.life = 0.5F + this.rng.nextFloat() * 0.35F;

         s.rgb = switch (i % 3) {
            case 0 -> 16777215;
            case 1 -> 16765136;
            default -> hpColor(a.ghostFrom);
         };
         a.shards.add(s);
      }
   }

   private static final class Anim {
      float shown = -1.0F;
      float target = -1.0F;
      float ghostFrom;
      float ghostTo;
      long ghostAt = -1L;
      boolean ghostShattered = true;
      long hitAt = -1L;
      float healFrom;
      float healTo;
      long healAt = -1L;
      long born = System.nanoTime();
      long lastNs = System.nanoTime();
      int lives = -1;
      long lifeLostAt = -1L;
      int lifeLostIdx = -1;
      boolean pendingLifeShatter = false;
      final List<OverheadBars.Shard> shards = new ArrayList<>();
      final Map<String, Long> tagBorn = new HashMap<>();
   }

   private static final class Shard {
      float x;
      float y;
      float vx;
      float vy;
      float rot;
      float vrot;
      float size;
      float life;
      float age;
      int rgb;
      boolean diamond;
   }
}
