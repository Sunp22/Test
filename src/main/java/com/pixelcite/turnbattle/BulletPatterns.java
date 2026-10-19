package com.pixelcite.turnbattle;

import java.util.List;
import java.util.Random;

public class BulletPatterns {
   public static final Random RNG = new Random();

   public static int getPatternForElement(Element element, Random rng) {
      int[] patterns = switch (element) {
         case FIRE -> new int[]{1, 5, 10, 18, 19, 20, 21};
         case ICE -> new int[]{0, 2, 7, 8, 13, 22, 23};
         case NATURE -> new int[]{3, 4, 14, 16, 17, 24, 25};
         case DARK -> new int[]{6, 9, 11, 15, 19, 26, 27};
         case LIGHT -> new int[]{2, 8, 12, 13, 17, 28, 29};
         case NONE -> new int[]{0, 1, 3, 4, 14, 30, 31};
      };
      return patterns[rng.nextInt(patterns.length)];
   }

   public static int patternFor(String typeKey, Element element, Random rng) {
      int[] own = MobPatterns.patternsFor(typeKey);
      return own.length > 0 ? own[rng.nextInt(own.length)] : getPatternForElement(element, rng);
   }

   public static void spawn(int patternIndex, int tick, int boxX, int boxY, int boxW, int boxH, float soulX, float soulY, List<Bullet> out, float speedMult) {
      int sizeBefore = out.size();
      float cx = (float)boxX + (float)boxW / 2.0F;
      float cy = (float)boxY + (float)boxH / 2.0F;
      float scx = soulX + 4.5F;
      float scy = soulY + 4.5F;
      if (patternIndex >= 100) {
         MobPatterns.spawn(patternIndex, tick, boxX, boxY, boxW, boxH, scx, scy, out);

         for (int i = sizeBefore; i < out.size(); i++) {
            Bullet b = out.get(i);
            MobPatterns.applySpeed(b, speedMult);
            if (b.pendingTicks > 0) {
               b.active = false;
            }
         }
      } else {
         switch (patternIndex) {
            case 0:
               rainfall(tick, boxX, boxY, boxW, out);
               break;
            case 1:
               aimedBurst(tick, boxX, boxY, boxW, boxH, scx, scy, out);
               break;
            case 2:
               crossSweep(tick, boxX, boxY, boxW, boxH, cx, cy, out);
               break;
            case 3:
               waveLeft(tick, boxX, boxY, boxH, out);
               break;
            case 4:
               waveRight(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 5:
               cornerScatter(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 6:
               closingRing(tick, boxX, boxY, boxW, boxH, cx, cy, out);
               break;
            case 7:
               heavyRain(tick, boxX, boxY, boxW, out);
               break;
            case 8:
               alternatingRows(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 9:
               diagonalSweep(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 10:
               centerBurst(tick, cx, cy, out);
               break;
            case 11:
               gravityBalls(tick, boxX, boxY, boxW, out);
               break;
            case 12:
               sideWalls(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 13:
               topBottom(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 14:
               zigzagRain(tick, boxX, boxY, boxW, out);
               break;
            case 15:
               funnel(tick, boxX, boxY, boxW, scx, out);
               break;
            case 16:
               arcing(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 17:
               doubleSpiral(tick, cx, cy, out);
               break;
            case 18:
               randomBurst(tick, boxX, boxY, boxW, boxH, out);
               break;
            case 19:
               chaos(tick, boxX, boxY, boxW, boxH, scx, scy, out);
               break;
            case 20:
               fireRings(tick, cx, cy, out);
               break;
            case 21:
               flameWave(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, out);
               break;
            case 22:
               iciclesFall(tick, (float)boxX, (float)boxY, (float)boxW, out);
               break;
            case 23:
               frozenGrid(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, out);
               break;
            case 24:
               vineGrowth(tick, (float)boxX, (float)boxY, (float)boxH, out);
               break;
            case 25:
               petalSpiral(tick, cx, cy, out);
               break;
            case 26:
               voidPulse(tick, cx, cy, out);
               break;
            case 27:
               shadowSnake(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, out);
               break;
            case 28:
               lightBeams(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, out);
               break;
            case 29:
               holyCircle(tick, cx, cy, scx, scy, out);
               break;
            case 30:
               simpleX(tick, cx, cy, out);
               break;
            case 31:
               basicPulse(tick, cx, cy, out);
               break;
            case 32:
               antonioPincer(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, scx, scy, out);
               break;
            case 33:
               antonioMarch(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, out);
               break;
            case 34:
               antonioTantrum(tick, cx, cy, scx, scy, out);
               break;
            case 35:
               antonioStomp(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, out);
               break;
            case 36:
               krisSlashFull(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, scx, scy, out);
               break;
            case 37:
               krisSlashH(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, scx, scy, out);
               break;
            case 38:
               krisSlashV(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, scx, scy, out);
               break;
            case 39:
               krisSlashDiag(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, scx, scy, out);
               break;
            case 40:
               krisSlashX(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, scx, scy, out);
               break;
            case 41:
               krisSlashSweep(tick, (float)boxX, (float)boxY, (float)boxW, (float)boxH, scx, scy, out);
         }

         if (speedMult != 1.0F) {
            for (int ix = sizeBefore; ix < out.size(); ix++) {
               Bullet b = out.get(ix);
               if (!(b.size >= 990.0F)) {
                  b.vx *= speedMult;
                  b.vy *= speedMult;
                  b.ax *= speedMult;
                  b.ay *= speedMult;
               }
            }
         }
      }
   }

   private static Bullet mk(float x, float y, float vx, float vy, float sz) {
      Bullet b = new Bullet();
      b.x = x;
      b.y = y;
      b.vx = vx;
      b.vy = vy;
      b.size = sz;
      b.prevX = x;
      b.prevY = y;
      return b;
   }

   private static Bullet aimed(float fx, float fy, float tx, float ty, float spd, float sz) {
      float dx = tx - fx;
      float dy = ty - fy;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (len < 0.001F) {
         len = 1.0F;
      }

      return mk(fx, fy, dx / len * spd, dy / len * spd, sz);
   }

   private static void ring(float cx, float cy, float r, int n, float inward, float sz, List<Bullet> out) {
      for (int i = 0; i < n; i++) {
         double a = (Math.PI * 2) * (double)i / (double)n;
         float bx = cx + r * (float)Math.cos(a);
         float by = cy + r * (float)Math.sin(a);
         out.add(mk(bx, by, (float)Math.cos(a) * inward, (float)Math.sin(a) * inward, sz));
      }
   }

   private static float[] randomEdgePoint(int bx, int by, int bw, int bh) {
      int edge = RNG.nextInt(4);
      switch (edge) {
         case 0:
            return new float[]{(float)bx + RNG.nextFloat() * (float)bw, (float)(by - 5)};
         case 1:
            return new float[]{(float)bx + RNG.nextFloat() * (float)bw, (float)(by + bh + 5)};
         case 2:
            return new float[]{(float)(bx - 5), (float)by + RNG.nextFloat() * (float)bh};
         default:
            return new float[]{(float)(bx + bw + 5), (float)by + RNG.nextFloat() * (float)bh};
      }
   }

   private static Bullet spawnRandomEdgeBullet(int bx, int by, int bw, int bh, float speed, float size, List<Bullet> out) {
      int edge = RNG.nextInt(4);
      float x;
      float y;
      float baseAngle;
      switch (edge) {
         case 0:
            x = (float)bx + RNG.nextFloat() * (float)bw;
            y = (float)(by - 5);
            baseAngle = (float) (Math.PI / 2);
            break;
         case 1:
            x = (float)bx + RNG.nextFloat() * (float)bw;
            y = (float)(by + bh + 5);
            baseAngle = (float) (-Math.PI / 2);
            break;
         case 2:
            x = (float)(bx - 5);
            y = (float)by + RNG.nextFloat() * (float)bh;
            baseAngle = 0.0F;
            break;
         default:
            x = (float)(bx + bw + 5);
            y = (float)by + RNG.nextFloat() * (float)bh;
            baseAngle = (float) Math.PI;
      }

      float angle = baseAngle + (RNG.nextFloat() - 0.5F) * (float)Math.toRadians(100.0);
      Bullet b = mk(x, y, (float)Math.cos((double)angle) * speed, (float)Math.sin((double)angle) * speed, size);
      out.add(b);
      return b;
   }

   private static void rainfall(int t, int bx, int by, int bw, List<Bullet> out) {
      if (t % (2 + RNG.nextInt(3)) == 0) {
         spawnRandomEdgeBullet(bx, by, bw, bw, 1.6F + RNG.nextFloat() * 0.8F, 4.0F, out);
      }
   }

   private static void aimedBurst(int t, int bx, int by, int bw, int bh, float sx, float sy, List<Bullet> out) {
      if (t % 10 == 0) {
         float[] origin = randomEdgePoint(bx, by, bw, bh);
         float spd = 1.8F + RNG.nextFloat() * 0.6F;
         float jitterSx = sx + (RNG.nextFloat() - 0.5F) * 16.0F;
         float jitterSy = sy + (RNG.nextFloat() - 0.5F) * 16.0F;
         out.add(aimed(origin[0], origin[1], jitterSx, jitterSy, spd, 4.0F));

         for (int s : new int[]{-1, 1}) {
            double a = Math.atan2((double)(jitterSy - origin[1]), (double)(jitterSx - origin[0]))
               + (double)s * Math.toRadians((double)(12.0F + RNG.nextFloat() * 10.0F));
            out.add(mk(origin[0], origin[1], (float)Math.cos(a) * spd, (float)Math.sin(a) * spd, 4.0F));
         }
      }
   }

   private static void crossSweep(int t, int bx, int by, int bw, int bh, float cx, float cy, List<Bullet> out) {
      if (t % (12 + RNG.nextInt(8)) == 0) {
         float spd = 2.0F + RNG.nextFloat() * 0.6F;
         out.add(aimed((float)bx + RNG.nextFloat() * (float)bw, (float)(by - 5), cx, cy, spd, 5.0F));
         out.add(aimed((float)bx + RNG.nextFloat() * (float)bw, (float)(by + bh + 5), cx, cy, spd, 5.0F));
         out.add(aimed((float)(bx - 5), (float)by + RNG.nextFloat() * (float)bh, cx, cy, spd, 5.0F));
         out.add(aimed((float)(bx + bw + 5), (float)by + RNG.nextFloat() * (float)bh, cx, cy, spd, 5.0F));
      }
   }

   private static void waveLeft(int t, int bx, int by, int bh, List<Bullet> out) {
      if (t % (3 + RNG.nextInt(3)) == 0) {
         out.add(mk((float)(bx - 5), (float)by + RNG.nextFloat() * (float)bh, 1.8F + RNG.nextFloat() * 0.8F, (RNG.nextFloat() - 0.5F) * 3.0F, 4.0F));
      }
   }

   private static void waveRight(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (3 + RNG.nextInt(3)) == 0) {
         out.add(mk((float)(bx + bw + 5), (float)by + RNG.nextFloat() * (float)bh, -(1.8F + RNG.nextFloat() * 0.8F), (RNG.nextFloat() - 0.5F) * 3.0F, 4.0F));
      }
   }

   private static void cornerScatter(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (16 + RNG.nextInt(10)) == 0) {
         float[][] corners = new float[][]{
            {(float)bx, (float)by}, {(float)(bx + bw), (float)by}, {(float)bx, (float)(by + bh)}, {(float)(bx + bw), (float)(by + bh)}
         };
         float[] c = corners[RNG.nextInt(4)];
         float tx = (float)bx + RNG.nextFloat() * (float)bw;
         float ty = (float)by + RNG.nextFloat() * (float)bh;
         double baseA = Math.atan2((double)(ty - c[1]), (double)(tx - c[0]));

         for (int s = -1; s <= 1; s++) {
            double a = baseA + (double)s * Math.toRadians((double)(15.0F + RNG.nextFloat() * 15.0F));
            out.add(mk(c[0], c[1], (float)Math.cos(a) * 2.0F, (float)Math.sin(a) * 2.0F, 4.0F));
         }
      }
   }

   private static void closingRing(int t, int bx, int by, int bw, int bh, float cx, float cy, List<Bullet> out) {
      if (t % (24 + RNG.nextInt(10)) == 0) {
         float r = (float)Math.min(bw, bh) * (0.35F + RNG.nextFloat() * 0.15F);
         float rotOffset = RNG.nextFloat() * (float) (Math.PI * 2);
         int n = 8;

         for (int i = 0; i < n; i++) {
            double a = (Math.PI * 2) * (double)i / (double)n + (double)rotOffset;
            float bxp = cx + r * (float)Math.cos(a);
            float byp = cy + r * (float)Math.sin(a);
            out.add(mk(bxp, byp, (float)Math.cos(a) * -1.3F, (float)Math.sin(a) * -1.3F, 4.0F));
         }
      }
   }

   private static void heavyRain(int t, int bx, int by, int bw, List<Bullet> out) {
      if (t % (1 + RNG.nextInt(2)) == 0) {
         spawnRandomEdgeBullet(bx, by, bw, bw, 1.8F + RNG.nextFloat() * 0.6F, 5.0F, out);
      }
   }

   private static void alternatingRows(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (6 + RNG.nextInt(6)) == 0) {
         float[] origin = randomEdgePoint(bx, by, bw, bh);
         float cx2 = (float)bx + (float)bw / 2.0F + (RNG.nextFloat() - 0.5F) * (float)bw * 0.6F;
         float cy2 = (float)by + (float)bh / 2.0F + (RNG.nextFloat() - 0.5F) * (float)bh * 0.6F;
         out.add(aimed(origin[0], origin[1], cx2, cy2, 1.8F + RNG.nextFloat() * 0.5F, 4.0F));
      }
   }

   private static void diagonalSweep(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (4 + RNG.nextInt(3)) == 0) {
         float[] origin = randomEdgePoint(bx, by, bw, bh);
         double ang = RNG.nextDouble() * 2.0 * Math.PI;
         float spd = 1.8F + RNG.nextFloat() * 0.7F;
         out.add(mk(origin[0], origin[1], (float)Math.cos(ang) * spd, (float)Math.sin(ang) * spd, 4.0F));
      }
   }

   private static void centerBurst(int t, float cx, float cy, List<Bullet> out) {
      if (t % (12 + RNG.nextInt(8)) == 0) {
         float ox = cx + (RNG.nextFloat() - 0.5F) * 30.0F;
         float oy = cy + (RNG.nextFloat() - 0.5F) * 30.0F;
         float rot = RNG.nextFloat() * (float) (Math.PI * 2);
         int n = 8;

         for (int i = 0; i < n; i++) {
            double a = (Math.PI * 2) * (double)i / (double)n + (double)rot;
            out.add(mk(ox, oy, (float)Math.cos(a) * 2.3F, (float)Math.sin(a) * 2.3F, 4.0F));
         }
      }
   }

   private static void gravityBalls(int t, int bx, int by, int bw, List<Bullet> out) {
      if (t % (4 + RNG.nextInt(3)) == 0) {
         Bullet b = spawnRandomEdgeBullet(bx, by, bw, bw, 1.2F, 6.0F, out);
         if (b != null) {
            b.ay = 0.1F;
         }
      }
   }

   private static void sideWalls(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (3 + RNG.nextInt(3)) == 0) {
         boolean left = RNG.nextBoolean();
         float y1 = (float)by + RNG.nextFloat() * (float)bh;
         float spd = 2.0F + RNG.nextFloat() * 0.6F;
         float ty = (float)by + RNG.nextFloat() * (float)bh;
         if (left) {
            out.add(aimed((float)(bx - 5), y1, (float)(bx + bw), ty, spd, 4.0F));
         } else {
            out.add(aimed((float)(bx + bw + 5), y1, (float)bx, ty, spd, 4.0F));
         }
      }
   }

   private static void topBottom(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (4 + RNG.nextInt(3)) == 0) {
         boolean top = RNG.nextBoolean();
         float x = (float)bx + RNG.nextFloat() * (float)bw;
         float ty = (float)by + RNG.nextFloat() * (float)bh;
         float spd = 2.0F + RNG.nextFloat() * 0.6F;
         if (top) {
            out.add(aimed(x, (float)(by - 5), (float)bx + RNG.nextFloat() * (float)bw, (float)(by + bh), spd, 4.0F));
         } else {
            out.add(aimed(x, (float)(by + bh + 5), (float)bx + RNG.nextFloat() * (float)bw, (float)by, spd, 4.0F));
         }
      }
   }

   private static void zigzagRain(int t, int bx, int by, int bw, List<Bullet> out) {
      if (t % (2 + RNG.nextInt(2)) == 0) {
         spawnRandomEdgeBullet(bx, by, bw, bw, 1.8F + RNG.nextFloat() * 0.8F, 4.0F, out);
      }
   }

   private static void funnel(int t, int bx, int by, int bw, float sx, List<Bullet> out) {
      if (t % (3 + RNG.nextInt(3)) == 0) {
         float spx = (float)bx + RNG.nextFloat() * (float)bw;
         float spd = 1.8F + RNG.nextFloat() * 0.6F;
         Bullet b = mk(spx, (float)(by - 5), 0.0F, spd, 4.0F);
         float targetX = sx + (RNG.nextFloat() - 0.5F) * 20.0F;
         b.vx = (targetX - spx) * 0.015F;
         out.add(b);
      }
   }

   private static void arcing(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (6 + RNG.nextInt(5)) == 0) {
         boolean left = RNG.nextBoolean();
         float sx2 = left ? (float)(bx - 5) : (float)(bx + bw + 5);
         float sy2 = (float)by + RNG.nextFloat() * (float)bh;
         float vx = left ? 1.8F + RNG.nextFloat() * 0.6F : -(1.8F + RNG.nextFloat() * 0.6F);
         Bullet b = mk(sx2, sy2, vx, -(2.0F + RNG.nextFloat() * 0.8F), 5.0F);
         b.ay = 0.1F + RNG.nextFloat() * 0.05F;
         out.add(b);
      }
   }

   private static void doubleSpiral(int t, float cx, float cy, List<Bullet> out) {
      if (t % (2 + RNG.nextInt(2)) == 0) {
         float ox = cx + (RNG.nextFloat() - 0.5F) * 20.0F;
         float oy = cy + (RNG.nextFloat() - 0.5F) * 20.0F;
         double a = RNG.nextDouble() * 2.0 * Math.PI;
         out.add(mk(ox, oy, (float)Math.cos(a) * 2.2F, (float)Math.sin(a) * 2.2F, 4.0F));
      }
   }

   private static void randomBurst(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      if (t % (10 + RNG.nextInt(6)) == 0) {
         float ox = (float)(bx + 15) + RNG.nextFloat() * (float)(bw - 30);
         float oy = (float)(by + 15) + RNG.nextFloat() * (float)(bh - 30);
         float rot = RNG.nextFloat() * (float) (Math.PI * 2);

         for (int i = 0; i < 6; i++) {
            double a = (Math.PI * 2) * (double)i / 6.0 + (double)rot;
            out.add(mk(ox, oy, (float)Math.cos(a) * 2.5F, (float)Math.sin(a) * 2.5F, 4.0F));
         }
      }
   }

   private static void chaos(int t, int bx, int by, int bw, int bh, float sx, float sy, List<Bullet> out) {
      if (t % 6 == 0) {
         int edge = RNG.nextInt(4);
         float x;
         float y;
         float vx;
         float vy;
         switch (edge) {
            case 0:
               x = (float)bx + RNG.nextFloat() * (float)bw;
               y = (float)(by - 5);
               vx = (RNG.nextFloat() - 0.5F) * 3.0F;
               vy = 1.5F + RNG.nextFloat() * 1.5F;
               break;
            case 1:
               x = (float)bx + RNG.nextFloat() * (float)bw;
               y = (float)(by + bh + 5);
               vx = (RNG.nextFloat() - 0.5F) * 3.0F;
               vy = -(1.5F + RNG.nextFloat() * 1.5F);
               break;
            case 2:
               x = (float)(bx - 5);
               y = (float)by + RNG.nextFloat() * (float)bh;
               vx = 1.5F + RNG.nextFloat() * 1.5F;
               vy = (RNG.nextFloat() - 0.5F) * 3.0F;
               break;
            default:
               x = (float)(bx + bw + 5);
               y = (float)by + RNG.nextFloat() * (float)bh;
               vx = -(1.5F + RNG.nextFloat() * 1.5F);
               vy = (RNG.nextFloat() - 0.5F) * 3.0F;
         }

         out.add(mk(x, y, vx, vy, 4.0F + RNG.nextFloat() * 2.0F));
      }
   }

   private static void fireRings(int t, float cx, float cy, List<Bullet> out) {
      if (t % 20 == 0) {
         int ring = t / 20;
         float radius = (float)(15 + ring * 10);
         int count = 12 + ring * 4;
         float ox = cx + (RNG.nextFloat() - 0.5F) * 24.0F;
         float oy = cy + (RNG.nextFloat() - 0.5F) * 24.0F;
         float rot = RNG.nextFloat() * (float) (Math.PI * 2);

         for (int i = 0; i < count; i++) {
            float angle = (float)((double)(i * 2) * Math.PI / (double)count) + rot;
            float x = ox + (float)Math.cos((double)angle) * radius;
            float y = oy + (float)Math.sin((double)angle) * radius;
            float vx = (float)Math.cos((double)angle) * 1.2F;
            float vy = (float)Math.sin((double)angle) * 1.2F;
            out.add(mk(x, y, vx, vy, 5.0F));
         }
      }
   }

   private static void flameWave(int t, float bx, float by, float bw, float bh, List<Bullet> out) {
      if (t % 8 == 0) {
         float x = bx + RNG.nextFloat() * bw;
         float y = by + bh + 5.0F;
         float vx = (RNG.nextFloat() - 0.5F) * 1.8F;
         float vy = -2.5F - RNG.nextFloat() * 1.0F;
         out.add(mk(x, y, vx, vy, 6.0F));
      }

      if (t % 12 == 6) {
         float x = bx + RNG.nextFloat() * bw;
         float y = by + bh + 5.0F;
         float vy = -3.0F;
         out.add(mk(x, y, (RNG.nextFloat() - 0.5F) * 1.0F, vy, 4.0F));
      }
   }

   private static void iciclesFall(int t, float bx, float by, float bw, List<Bullet> out) {
      if (t % 12 == 0) {
         float x = bx + RNG.nextFloat() * bw;
         float y = by - 10.0F;
         out.add(mk(x, y, (RNG.nextFloat() - 0.5F) * 0.4F, 1.7F + RNG.nextFloat() * 0.6F, 3.0F));
      }

      if (t % 20 == 10) {
         float x = bx + RNG.nextFloat() * bw;
         float y = by - 10.0F;
         out.add(mk(x, y, (RNG.nextFloat() - 0.5F) * 0.6F, 2.2F + RNG.nextFloat() * 0.6F, 4.0F));
      }
   }

   private static void frozenGrid(int t, float bx, float by, float bw, float bh, List<Bullet> out) {
      if (t % 18 == 0) {
         float spawnX = bx + RNG.nextFloat() * bw;
         float spawnY = by - 15.0F;
         float targetX = bx + RNG.nextFloat() * bw;
         float targetY = by + bh * (0.3F + RNG.nextFloat() * 0.4F);

         for (int i = 0; i < 7; i++) {
            float angleOffset = (float)(i - 3) * (0.22F + RNG.nextFloat() * 0.06F);
            float dx = targetX - spawnX;
            float dy = targetY - spawnY;
            float baseAngle = (float)Math.atan2((double)dy, (double)dx);
            float finalAngle = baseAngle + angleOffset;
            float speed = 2.0F + RNG.nextFloat() * 0.4F;
            float vx = (float)Math.cos((double)finalAngle) * speed;
            float vy = (float)Math.sin((double)finalAngle) * speed;
            out.add(mk(spawnX, spawnY, vx, vy, 4.0F));
         }
      }
   }

   private static void vineGrowth(int t, float bx, float by, float bh, List<Bullet> out) {
      if (t % 10 == 0) {
         boolean fromLeft = RNG.nextBoolean();
         float y = by + RNG.nextFloat() * bh;
         if (fromLeft) {
            out.add(mk(bx - 5.0F, y, 1.4F + RNG.nextFloat() * 0.4F, (RNG.nextFloat() - 0.5F) * 0.6F, 5.0F));
         } else {
            out.add(mk(bx + 165.0F, y, -(1.4F + RNG.nextFloat() * 0.4F), (RNG.nextFloat() - 0.5F) * 0.6F, 5.0F));
         }
      }

      if (t % 15 == 5) {
         float y = by + RNG.nextFloat() * bh;
         float side = RNG.nextBoolean() ? -5.0F : 165.0F;
         float vx = side < 0.0F ? 1.2F : -1.2F;
         float vy = (RNG.nextFloat() - 0.5F) * 1.0F;
         out.add(mk(bx + side, y, vx, vy, 4.0F));
      }
   }

   private static void petalSpiral(int t, float cx, float cy, List<Bullet> out) {
      if (t % 5 == 0) {
         float dir = t / 5 % 2 == 0 ? 1.0F : -1.0F;
         float angle = (float)((double)t * Math.PI / 20.0) * dir + RNG.nextFloat() * 0.3F;
         float radius = 18.0F + (float)(t % 80) * 0.85F;
         float ox = cx + (RNG.nextFloat() - 0.5F) * 16.0F;
         float oy = cy + (RNG.nextFloat() - 0.5F) * 16.0F;
         float x = ox + (float)Math.cos((double)angle) * radius;
         float y = oy + (float)Math.sin((double)angle) * radius;
         float vx = (float)Math.cos((double)angle) * 0.7F;
         float vy = (float)Math.sin((double)angle) * 0.7F;
         out.add(mk(x, y, vx, vy, 5.0F));
      }
   }

   private static void voidPulse(int t, float cx, float cy, List<Bullet> out) {
      if (t % 30 == 0) {
         int count = 16;
         float ox = cx + (RNG.nextFloat() - 0.5F) * 30.0F;
         float oy = cy + (RNG.nextFloat() - 0.5F) * 30.0F;
         float rot = RNG.nextFloat() * (float) (Math.PI * 2);
         float speed = 0.5F + RNG.nextFloat() * 1.0F;

         for (int i = 0; i < count; i++) {
            float angle = (float)((double)(i * 2) * Math.PI / (double)count) + rot;
            float vx = (float)Math.cos((double)angle) * speed;
            float vy = (float)Math.sin((double)angle) * speed;
            out.add(mk(ox, oy, vx, vy, 3.0F));
         }
      }
   }

   private static void shadowSnake(int t, float bx, float by, float bw, float bh, List<Bullet> out) {
      if (t % 4 == 0) {
         float x = bx + RNG.nextFloat() * bw;
         float y = by + RNG.nextFloat() * bh;
         double ang = RNG.nextDouble() * 2.0 * Math.PI;
         float spd = 1.0F + RNG.nextFloat() * 1.2F;
         float vx = (float)Math.cos(ang) * spd;
         float vy = (float)Math.sin(ang) * spd;
         out.add(mk(x, y, vx, vy, 4.0F));
      }
   }

   private static void lightBeams(int t, float bx, float by, float bw, float bh, List<Bullet> out) {
      if (t % 8 == 0) {
         boolean horizontal = RNG.nextBoolean();
         float speed = 4.0F + RNG.nextFloat() * 1.0F;
         if (horizontal) {
            boolean fromLeft = RNG.nextBoolean();
            float y = by + RNG.nextFloat() * bh;
            float vx = fromLeft ? speed : -speed;
            float startX = fromLeft ? bx - 10.0F : bx + bw + 10.0F;

            for (int i = 0; i < 15; i++) {
               out.add(mk(startX - (float)(fromLeft ? i * 6 : -i * 6), y, vx, 0.0F, 3.0F));
            }
         } else {
            boolean fromTop = RNG.nextBoolean();
            float x = bx + RNG.nextFloat() * bw;
            float vy = fromTop ? speed : -speed;
            float startY = fromTop ? by - 10.0F : by + bh + 10.0F;

            for (int i = 0; i < 12; i++) {
               out.add(mk(x, startY - (float)(fromTop ? i * 6 : -i * 6), 0.0F, vy, 3.0F));
            }
         }
      }

      if (t % 20 == 10) {
         float diagSpeed = 3.6F + RNG.nextFloat() * 0.8F;
         int corner = RNG.nextInt(4);
         float sx2;
         float sy2;
         float vx;
         float vy;
         switch (corner) {
            case 0:
               sx2 = bx - 20.0F;
               sy2 = by - 20.0F;
               vx = diagSpeed;
               vy = diagSpeed;
               break;
            case 1:
               sx2 = bx + bw + 20.0F;
               sy2 = by - 20.0F;
               vx = -diagSpeed;
               vy = diagSpeed;
               break;
            case 2:
               sx2 = bx - 20.0F;
               sy2 = by + bh + 20.0F;
               vx = diagSpeed;
               vy = -diagSpeed;
               break;
            default:
               sx2 = bx + bw + 20.0F;
               sy2 = by + bh + 20.0F;
               vx = -diagSpeed;
               vy = -diagSpeed;
         }

         float stepX = vx > 0.0F ? -8.0F : 8.0F;
         float stepY = vy > 0.0F ? -8.0F : 8.0F;

         for (int i = 0; i < 10; i++) {
            out.add(mk(sx2 + (float)i * stepX, sy2 + (float)i * stepY, vx * 0.7F, vy * 0.7F, 3.0F));
         }
      }
   }

   private static void holyCircle(int t, float cx, float cy, float scx, float scy, List<Bullet> out) {
      if (t == 0) {
         float rotOffset = RNG.nextFloat() * (float) (Math.PI * 2);
         float radius = 55.0F;
         int count = 16;

         for (int i = 0; i < count; i++) {
            float a = rotOffset + (float)((double)(i * 2) * Math.PI / (double)count);
            float x = cx + (float)Math.cos((double)a) * radius;
            float y = cy + (float)Math.sin((double)a) * radius;
            out.add(mk(x, y, 0.0F, 0.0F, 4.0F));
         }
      }

      if (t % 14 == 5) {
         float speed = 1.8F + RNG.nextFloat() * 0.6F;
         int shots = 2 + RNG.nextInt(3);

         for (int i = 0; i < shots; i++) {
            double ang = RNG.nextDouble() * 2.0 * Math.PI;
            float dist = 55.0F + RNG.nextFloat() * 15.0F;
            float startX = scx + (float)Math.cos(ang) * dist;
            float startY = scy + (float)Math.sin(ang) * dist;
            out.add(aimed(startX, startY, scx, scy, speed, 5.0F));
         }
      }
   }

   private static void simpleX(int t, float cx, float cy, List<Bullet> out) {
      if (t % 15 == 0) {
         float rot = RNG.nextFloat() * (float) (Math.PI * 2);
         float ox = cx + (RNG.nextFloat() - 0.5F) * 20.0F;
         float oy = cy + (RNG.nextFloat() - 0.5F) * 20.0F;

         for (int i = 0; i < 4; i++) {
            float angle = (float)((Math.PI / 4) + (double)i * Math.PI / 2.0) + rot;
            float vx = (float)Math.cos((double)angle) * 1.6F;
            float vy = (float)Math.sin((double)angle) * 1.6F;
            out.add(mk(ox, oy, vx, vy, 5.0F));
         }
      }
   }

   private static void basicPulse(int t, float cx, float cy, List<Bullet> out) {
      if (t % 20 == 0) {
         int count = 8;
         float rot = RNG.nextFloat() * (float) (Math.PI * 2);
         float ox = cx + (RNG.nextFloat() - 0.5F) * 20.0F;
         float oy = cy + (RNG.nextFloat() - 0.5F) * 20.0F;

         for (int i = 0; i < count; i++) {
            float angle = (float)((double)(i * 2) * Math.PI / (double)count) + rot;
            float vx = (float)Math.cos((double)angle) * 1.1F;
            float vy = (float)Math.sin((double)angle) * 1.1F;
            out.add(mk(ox, oy, vx, vy, 5.0F));
         }
      }
   }

   private static void antonioPincer(int t, float bx, float by, float bw, float bh, float sx, float sy, List<Bullet> out) {
      if (t % 18 == 0) {
         int rows = 3;

         for (int r = 0; r < rows; r++) {
            float y = by + bh * (0.2F + 0.3F * (float)r);
            out.add(aimed(bx, y, sx, sy, 1.4F, 5.0F));
            out.add(aimed(bx + bw, y, sx, sy, 1.4F, 5.0F));
         }
      }
   }

   private static void antonioMarch(int t, float bx, float by, float bw, float bh, List<Bullet> out) {
      if (t % 12 == 0) {
         int cols = 5;
         float offset = (float)(t / 12 % 2) * (bw / (float)(cols * 2));

         for (int c = 0; c < cols; c++) {
            float x = bx + bw * (float)c / ((float)cols - 1.0F) + offset;
            out.add(mk(x, by, 0.0F, 1.2F, 4.0F));
         }
      }
   }

   private static void antonioTantrum(int t, float cx, float cy, float sx, float sy, List<Bullet> out) {
      if (t % 8 == 0) {
         float spread = (float)(t % 3 - 1) * 0.3F;
         float dx = sx - cx;
         float dy = sy - cy;
         float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
         if (len < 0.01F) {
            len = 1.0F;
         }

         float nx = dx / len;
         float ny = dy / len;
         float px = -ny;
         float py = nx;

         for (int i = -1; i <= 1; i++) {
            float vx = nx * 1.8F + px * (float)i * spread;
            float vy = ny * 1.8F + py * (float)i * spread;
            out.add(mk(cx, cy, vx, vy, 4.0F));
         }
      }
   }

   private static void antonioStomp(int t, float bx, float by, float bw, float bh, List<Bullet> out) {
      if (t % 25 == 0) {
         int count = 6;
         float bottom = by + bh;

         for (int i = 0; i < count; i++) {
            float x = bx + bw * (float)i / ((float)count - 1.0F);
            float vy = -1.5F - RNG.nextFloat() * 0.8F;
            float vx = (RNG.nextFloat() - 0.5F) * 0.6F;
            out.add(mk(x, bottom, vx, vy, 5.0F));
         }
      }
   }

   public static int getAntonioPattern(Random rng) {
      int[] patterns = new int[]{32, 33, 34, 35};
      return patterns[rng.nextInt(patterns.length)];
   }

   public static int getKrisPattern(Random rng) {
      int[] patterns = new int[]{36, 37, 38, 39, 40, 41};
      return patterns[rng.nextInt(patterns.length)];
   }

   private static Bullet krisMarker(int type, float pos) {
      Bullet b = new Bullet();
      b.size = 990.0F;
      b.vx = (float)type;
      b.vy = pos;
      return b;
   }

   private static void krisSlashFull(int t, float bx, float by, float bw, float bh, float sx, float sy, List<Bullet> out) {
      float[] rows = new float[]{0.2F, 0.5F, 0.8F, 0.35F, 0.65F};

      for (int i = 0; i < 5; i++) {
         if (t == i * 22) {
            out.add(krisMarker(1, by + bh * rows[i]));
         }
      }

      if (t > 21 && t < 44 && t % 5 == 0) {
         out.add(aimed(bx + bw / 2.0F, by - 5.0F, sx, sy, 1.6F, 4.0F));
      }

      if (t > 43 && t < 66 && t % 5 == 0) {
         out.add(aimed(bx + bw / 2.0F, by - 5.0F, sx, sy, 1.6F, 4.0F));
      }

      if (t > 65 && t < 88 && t % 5 == 0) {
         out.add(aimed(bx + bw / 2.0F, by - 5.0F, sx, sy, 1.6F, 4.0F));
      }
   }

   private static void krisSlashH(int t, float bx, float by, float bw, float bh, float sx, float sy, List<Bullet> out) {
      float[] cols = new float[]{0.25F, 0.75F, 0.5F, 0.15F, 0.85F};

      for (int i = 0; i < 5; i++) {
         if (t == i * 22) {
            out.add(krisMarker(2, bx + bw * cols[i]));
         }
      }

      if ((t > 21 && t < 44 || t > 65 && t < 88) && t % 3 == 0) {
         out.add(mk(bx + RNG.nextFloat() * bw, by, 0.0F, 1.6F, 4.0F));
      }

      if (t > 43 && t < 66 && t % 3 == 0) {
         out.add(mk(bx + RNG.nextFloat() * bw, by, 0.0F, 1.6F, 4.0F));
      }
   }

   private static void krisSlashV(int t, float bx, float by, float bw, float bh, float sx, float sy, List<Bullet> out) {
      if (t == 0) {
         out.add(krisMarker(3, 0.0F));
      }

      if (t == 22) {
         out.add(krisMarker(4, 0.0F));
      }

      if (t == 44) {
         out.add(krisMarker(3, 0.0F));
      }

      if (t == 66) {
         out.add(krisMarker(4, 0.0F));
      }

      if (t == 88) {
         out.add(krisMarker(3, 0.0F));
      }

      for (int base : new int[]{21, 43, 65, 87}) {
         if (t == base) {
            for (int i = -2; i <= 2; i++) {
               out.add(aimed(bx + bw / 2.0F, by - 5.0F, sx + (float)(i * 12), sy, 1.7F, 4.0F));
            }
         }
      }
   }

   private static void krisSlashDiag(int t, float bx, float by, float bw, float bh, float sx, float sy, List<Bullet> out) {
      if (t == 0) {
         out.add(krisMarker(3, 0.0F));
         out.add(krisMarker(4, 0.0F));
      }

      if (t == 30) {
         out.add(krisMarker(3, 0.0F));
         out.add(krisMarker(4, 0.0F));
      }

      if (t == 60) {
         out.add(krisMarker(3, 0.0F));
         out.add(krisMarker(4, 0.0F));
      }

      if (t == 90) {
         out.add(krisMarker(3, 0.0F));
         out.add(krisMarker(4, 0.0F));
      }

      if (t > 21 && t < 30 && t % 3 == 0) {
         out.add(mk(bx, by + RNG.nextFloat() * bh, 1.4F, 0.0F, 4.0F));
         out.add(mk(bx + bw, by + RNG.nextFloat() * bh, -1.4F, 0.0F, 4.0F));
      }

      if (t > 51 && t < 60 && t % 3 == 0) {
         out.add(mk(bx, by + RNG.nextFloat() * bh, 1.4F, 0.0F, 4.0F));
         out.add(mk(bx + bw, by + RNG.nextFloat() * bh, -1.4F, 0.0F, 4.0F));
      }
   }

   private static void krisSlashX(int t, float bx, float by, float bw, float bh, float sx, float sy, List<Bullet> out) {
      float[] hRows = new float[]{0.3F, 0.7F, 0.5F};
      float[] vCols = new float[]{0.3F, 0.7F};
      if (t == 0) {
         out.add(krisMarker(1, by + bh * hRows[0]));
      }

      if (t == 22) {
         out.add(krisMarker(2, bx + bw * vCols[0]));
      }

      if (t == 44) {
         out.add(krisMarker(1, by + bh * hRows[1]));
      }

      if (t == 66) {
         out.add(krisMarker(2, bx + bw * vCols[1]));
      }

      if (t == 88) {
         out.add(krisMarker(1, by + bh * hRows[2]));
      }

      for (int base = 21; base < 110; base += 22) {
         if (t == base) {
            for (int r = 0; r < 3; r++) {
               float y = by + bh * (0.25F + 0.25F * (float)r);
               out.add(aimed(bx, y, sx, sy, 1.5F, 4.0F));
               out.add(aimed(bx + bw, y, sx, sy, 1.5F, 4.0F));
            }
         }
      }
   }

   private static void krisSlashSweep(int t, float bx, float by, float bw, float bh, float sx, float sy, List<Bullet> out) {
      for (int i = 0; i < 5; i++) {
         if (t == i * 22) {
            out.add(krisMarker(1, by + bh * (0.1F + 0.2F * (float)i)));
         }
      }

      if (t % 4 == 0) {
         out.add(mk(bx + RNG.nextFloat() * bw, by, (RNG.nextFloat() - 0.5F) * 0.6F, 1.3F, 4.0F));
      }

      if (t % 8 == 0) {
         out.add(aimed(bx, by + RNG.nextFloat() * bh, sx, sy, 1.3F, 4.0F));
         out.add(aimed(bx + bw, by + RNG.nextFloat() * bh, sx, sy, 1.3F, 4.0F));
      }
   }
}
