package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

final class GimmickModes {
   private final BulletHellRenderer bh;
   private final BattleScreen screen;
   private final Random rng = new Random();
   private static final int[] LDX = new int[]{0, 1, 0, -1};
   private static final int[] LDY = new int[]{-1, 0, 1, 0};
   static final float SHIELD_R = 11.0F;
   static final float SHIELD_LEN = 18.0F;
   static final float SHIELD_THICK = 3.0F;
   private static final int TRICK_SPIN = 9;
   private static final int TRICK_FINAL = 6;
   private static final float TRICK_R = 34.0F;
   private final List<GimmickModes.Spear> spears = new ArrayList<>();
   private final List<GimmickModes.Spark> sparks = new ArrayList<>();
   int shieldDir = 0;
   private int shieldShake = 0;
   private int shieldFlash = 0;
   private int travel = 46;
   static final float GRAVITY = 0.3F;
   static final float JUMP_V = -4.8F;
   static final float MAX_FALL = 6.0F;
   private final List<GimmickModes.Spike> spikes = new ArrayList<>();
   private final List<GimmickModes.Platform> platforms = new ArrayList<>();
   private final List<GimmickModes.Ev> events = new ArrayList<>();
   private GimmickModes.Platform standing;
   private float vy;
   private boolean moving;
   private boolean prevW;
   private static final int ROW = 0;
   private static final int POLE = 1;
   private static final int PILLAR = 2;
   private static final float TOOTH_W = 8.0F;

   GimmickModes(BulletHellRenderer bh, BattleScreen screen) {
      this.bh = bh;
      this.screen = screen;
   }

   private static float clamp01(float x) {
      return x < 0.0F ? 0.0F : (x > 1.0F ? 1.0F : x);
   }

   private static float mix(float a, float b, float t) {
      return a + (b - a) * t;
   }

   void startGreen(int duration) {
      this.spears.clear();
      this.sparks.clear();
      this.shieldDir = 0;
      this.shieldShake = 0;
      this.shieldFlash = 0;
      float m = Math.max(0.5F, this.screen.bulletSpeedMult);
      this.travel = Math.max(28, Math.min(60, Math.round(46.0F / m)));
      float gap = 1.0F / (float)Math.sqrt((double)m);
      List arr = new ArrayList<>();
      int first = this.travel + 8;
      int mid = first + (duration - first) / 2;
      int end = duration - 6;
      int p1 = this.rng.nextInt(6);
      int p2 = (p1 + 1 + this.rng.nextInt(5)) % 6;
      this.greenPattern(p1, first, mid, gap, arr);
      this.greenPattern(p2, mid, end, gap, arr);
      arr.sort((a, b) -> Integer.compare(a[0], b[0]));
      int lastT = -100;
      int lastDir = -1;

      for (int[] a : arr) {
         int minGap = a[1] == lastDir ? 5 : 7;
         if (a[0] - lastT < minGap) {
            a[0] = lastT + minGap;
         }

         if (a[0] <= end) {
            GimmickModes.Spear s = new GimmickModes.Spear();
            s.arrive = a[0];
            s.dir = a[1];
            s.trick = a[2] == 1;
            s.spawn = s.arrive - this.travel - (s.trick ? 15 : 0);
            if (s.spawn >= 2) {
               this.spears.add(s);
               lastT = a[0];
               lastDir = a[1];
            }
         }
      }
   }

   private int randDir(int avoid) {
      int d = this.rng.nextInt(4);
      if (d == avoid && this.rng.nextBoolean()) {
         d = (d + 1 + this.rng.nextInt(3)) % 4;
      }

      return d;
   }

   private void greenPattern(int p, int a, int b, float gap, List<int[]> out) {
      int t = a;
      switch (p) {
         case 0:
            int d = -1;

            while (t < b) {
               d = this.randDir(d);
               out.add(new int[]{t, d, 0});
               t += Math.round((float)(16 + this.rng.nextInt(7)) * gap);
            }

            return;
         case 1:
            while (t < b) {
               int d = this.rng.nextInt(4);

               for (int k = 0; k < 3; k++) {
                  out.add(new int[]{t + k * Math.round(6.0F * gap), d, 0});
               }

               t += Math.round(30.0F * gap);
            }

            return;
         case 2:
            int d = this.rng.nextInt(2);
            int n = 0;

            while (t < b) {
               out.add(new int[]{t, n % 2 == 0 ? d : d + 2, 0});
               if (++n % 6 == 0) {
                  d = 1 - d;
               }

               t += Math.round(11.0F * gap);
            }

            return;
         case 3:
            int d = this.rng.nextInt(4);
            int step = this.rng.nextBoolean() ? 1 : 3;
            int half = (a + b) / 2;
            boolean flipped = false;

            while (t < b) {
               out.add(new int[]{t, d, 0});
               if (!flipped && t >= half) {
                  step = 4 - step;
                  flipped = true;
                  t += Math.round(6.0F * gap);
               }

               d = (d + step) % 4;
               t += Math.round(10.0F * gap);
            }

            return;
         case 4:
            int d = -1;

            while (t < b) {
               d = this.randDir(d);
               out.add(new int[]{t, d, this.rng.nextFloat() < 0.5F ? 1 : 0});
               t += Math.round(22.0F * gap);
            }

            return;
         default:
            while (t < b) {
               int d = -1;

               for (int k = 0; k < 4 && t < b; k++) {
                  d = this.randDir(d);
                  out.add(new int[]{t, d, k == 3 ? 1 : 0});
                  t += Math.round(9.0F * gap);
               }

               t += Math.round(14.0F * gap);
            }
      }
   }

   private float scx() {
      return this.screen.soulX + 4.5F;
   }

   private float scy() {
      return this.screen.soulY + 4.5F;
   }

   private float edgeDist(int d) {
      return switch (d) {
         case 0 -> this.scy() + 12.0F;
         case 1 -> (float)this.screen.getWidth() - this.scx() + 12.0F;
         case 2 -> (float)this.screen.getHeight() - this.scy() + 12.0F;
         default -> this.scx() + 12.0F;
      };
   }

   void tickGreen(int now) {
      this.screen.soulX = (float)this.screen.boxX + (float)(this.screen.targetBoxWidth - 9) / 2.0F;
      this.screen.soulY = (float)this.screen.boxY + (float)(this.screen.targetBoxHeight - 9) / 2.0F;
      if (this.screen.keyW) {
         this.shieldDir = 0;
      } else if (this.screen.keyD) {
         this.shieldDir = 1;
      } else if (this.screen.keyS) {
         this.shieldDir = 2;
      } else if (this.screen.keyA) {
         this.shieldDir = 3;
      }

      if (this.shieldShake > 0) {
         this.shieldShake--;
      }

      if (this.shieldFlash > 0) {
         this.shieldFlash--;
      }

      for (GimmickModes.Spark s : this.sparks) {
         s.age++;
      }

      this.sparks.removeIf(s -> s.age > 12);
      float cx = this.scx();
      float cy = this.scy();

      for (GimmickModes.Spear s : this.spears) {
         if (!s.done && now >= s.spawn) {
            s.prevX = s.x;
            s.prevY = s.y;
            s.prevAng = s.ang;
            boolean finalLane;
            float dist;
            if (!s.trick) {
               dist = this.edgeDist(s.dir) * clamp01((float)(s.arrive - now) / (float)this.travel);
               s.x = cx + (float)LDX[s.dir] * dist;
               s.y = cy + (float)LDY[s.dir] * dist;
               s.ang = (float)Math.atan2((double)(-LDY[s.dir]), (double)(-LDX[s.dir]));
               finalLane = true;
            } else {
               int o = (s.dir + 2) % 4;
               int spinStart = s.arrive - 9 - 6;
               int spinEnd = s.arrive - 6;
               if (now <= spinStart) {
                  float q = clamp01((float)(now - s.spawn) / (float)(spinStart - s.spawn));
                  dist = mix(this.edgeDist(o), 34.0F, q);
                  s.x = cx + (float)LDX[o] * dist;
                  s.y = cy + (float)LDY[o] * dist;
                  s.ang = (float)Math.atan2((double)(-LDY[o]), (double)(-LDX[o]));
                  finalLane = false;
               } else if (now <= spinEnd) {
                  float q = (float)(now - spinStart) / 9.0F;
                  float a0 = (float)Math.atan2((double)LDY[o], (double)LDX[o]);
                  float a = a0 + (float) Math.PI * q;
                  dist = 34.0F;
                  s.x = cx + (float)Math.cos((double)a) * dist;
                  s.y = cy + (float)Math.sin((double)a) * dist;
                  s.ang = a + (float) (Math.PI / 2);
                  finalLane = false;
               } else {
                  float q = clamp01((float)(now - spinEnd) / 6.0F);
                  dist = 34.0F * (1.0F - q);
                  s.x = cx + (float)LDX[s.dir] * dist;
                  s.y = cy + (float)LDY[s.dir] * dist;
                  s.ang = (float)Math.atan2((double)(-LDY[s.dir]), (double)(-LDX[s.dir]));
                  finalLane = true;
               }
            }

            if (!s.seen) {
               s.prevX = s.x;
               s.prevY = s.y;
               s.prevAng = s.ang;
               s.seen = true;
            }

            if (finalLane) {
               if (dist <= 14.0F && this.shieldDir == s.dir) {
                  s.done = true;
                  this.block(cx + (float)LDX[s.dir] * 11.0F, cy + (float)LDY[s.dir] * 11.0F);
               } else if (dist <= 2.0F) {
                  s.done = true;
                  GimmickModes.Spark k = new GimmickModes.Spark();
                  k.x = cx;
                  k.y = cy;
                  this.sparks.add(k);
                  if (this.screen.iframeTicks <= 0) {
                     this.bh.hitSoul();
                  }
               }
            }
         }
      }

      this.spears.removeIf(sx -> sx.done);
   }

   private void block(float x, float y) {
      this.shieldShake = 7;
      this.shieldFlash = 4;
      GimmickModes.Spark k = new GimmickModes.Spark();
      k.x = x;
      k.y = y;
      k.blocked = true;
      this.sparks.add(k);
      this.bh.awardGraze(false);
      SoundEvent snd = (SoundEvent)Registries.SOUND_EVENT.get(Identifier.of("minecraft", "block.note_block.bell"));
      if (snd != null) {
         MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(snd, 1.65F + this.rng.nextFloat() * 0.1F, 0.9F));
      }
   }

   void renderGreen(DrawContext ctx, float delta, float rSoulX, float rSoulY, int now) {
      float cx = rSoulX + 4.5F;
      float cy = rSoulY + 4.5F;
      int arrowCol = 0xFF000000 | this.screen.battleElement.getBulletColor() & 16777215;

      for (GimmickModes.Spear s : this.spears) {
         if (!s.done && s.seen && now >= s.spawn) {
            float x = mix(s.prevX, s.x, delta);
            float y = mix(s.prevY, s.y, delta);
            float da = s.ang - s.prevAng;

            while ((double)da > Math.PI) {
               da -= (float) (Math.PI * 2);
            }

            while ((double)da < -Math.PI) {
               da += (float) (Math.PI * 2);
            }

            drawSpear(ctx, x, y, s.prevAng + da * delta, s.trick ? -8118 : arrowCol);
         }
      }

      for (int i = 0; i < 16; i++) {
         double a = (double)i * Math.PI / 8.0;
         int px = (int)((double)cx + Math.cos(a) * 11.0);
         int py = (int)((double)cy + Math.sin(a) * 11.0);
         ctx.fill(px, py, px + 1, py + 1, 1714661973);
      }

      float sx = cx + (float)LDX[this.shieldDir] * 11.0F;
      float sy = cy + (float)LDY[this.shieldDir] * 11.0F;
      if (this.shieldShake > 0) {
         float k = (float)this.shieldShake / 7.0F;
         sx += (this.rng.nextFloat() - 0.5F) * 3.0F * k + (float)LDX[this.shieldDir] * 1.5F * k;
         sy += (this.rng.nextFloat() - 0.5F) * 3.0F * k + (float)LDY[this.shieldDir] * 1.5F * k;
      }

      boolean horiz = LDY[this.shieldDir] != 0;
      float hw = horiz ? 9.0F : 1.5F;
      float hh = horiz ? 1.5F : 9.0F;
      int body = this.shieldFlash > 0 ? -1 : -13703062;
      fillF(ctx, sx - hw - 1.0F, sy - hh - 1.0F, sx + hw + 1.0F, sy + hh + 1.0F, -16041446);
      fillF(ctx, sx - hw, sy - hh, sx + hw, sy + hh, body);
      if (horiz) {
         fillF(ctx, sx - hw, LDY[this.shieldDir] < 0 ? sy - hh : sy + hh - 1.0F, sx + hw, LDY[this.shieldDir] < 0 ? sy - hh + 1.0F : sy + hh, -4653104);
      } else {
         fillF(ctx, LDX[this.shieldDir] < 0 ? sx - hw : sx + hw - 1.0F, sy - hh, LDX[this.shieldDir] < 0 ? sx - hw + 1.0F : sx + hw, sy + hh, -4653104);
      }

      for (GimmickModes.Spark k : this.sparks) {
         float q = ((float)k.age + delta) / 12.0F;
         int a = (int)(255.0F * (1.0F - q));
         if (a > 0) {
            int c = a << 24 | (k.blocked ? 16777215 : 16729156);

            for (int i = 0; i < 6; i++) {
               double an = (double)i * Math.PI / 3.0 + (k.blocked ? 0.3 : 0.0);
               float r = 2.0F + q * 9.0F;
               float px = k.x + (float)Math.cos(an) * r;
               float py = k.y + (float)Math.sin(an) * r;
               fillF(ctx, px - 1.0F, py - 1.0F, px + 1.0F, py + 1.0F, c);
            }
         }
      }
   }

   private static void drawSpear(DrawContext ctx, float x, float y, float ang, int col) {
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(x, y, 0.0F);
      m.multiply(new Quaternionf().rotationZ(ang));
      int dark = -15724520;
      ctx.fill(-15, -2, 1, 2, dark);
      ctx.fill(-14, -1, 0, 1, col);

      for (int i = 0; i < 5; i++) {
         ctx.fill(-1 + i, -(5 - i), i, 5 - i, dark);
      }

      for (int i = 0; i < 4; i++) {
         ctx.fill(-1 + i, -(4 - i), i, 4 - i, col);
      }

      ctx.fill(-15, -3, -12, -1, col);
      ctx.fill(-15, 1, -12, 3, col);
      ctx.fill(1, -1, 3, 1, -1);
      m.pop();
   }

   private static void fillF(DrawContext ctx, float x1, float y1, float x2, float y2, int c) {
      ctx.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), c);
   }

   private float floorY() {
      return (float)(this.screen.boxY + this.screen.targetBoxHeight);
   }

   private float bxL() {
      return (float)this.screen.boxX;
   }

   private float bxR() {
      return (float)(this.screen.boxX + this.screen.targetBoxWidth);
   }

   void startBlue(int duration) {
      this.spikes.clear();
      this.platforms.clear();
      this.events.clear();
      this.standing = null;
      this.vy = 0.0F;
      this.moving = false;
      this.prevW = false;
      int start = 12;
      int mid = (start + duration) / 2;
      int end = duration - 12;
      int p1 = this.rng.nextInt(6);
      int p2 = (p1 + 1 + this.rng.nextInt(5)) % 6;
      this.bluePattern(p1, start, mid);
      this.bluePattern(p2, mid + 8, end);
   }

   private float spd() {
      return Math.max(0.5F, this.screen.bulletSpeedMult);
   }

   private void at(int tick, Runnable r) {
      this.events.add(new GimmickModes.Ev(tick, r));
   }

   private GimmickModes.Spike floorSpike(float h, float w, boolean fromRight, float speed) {
      GimmickModes.Spike b = new GimmickModes.Spike();
      b.w = w;
      b.h = h;
      b.top = this.floorY() - h;
      b.x = fromRight ? this.bxR() + 2.0F : this.bxL() - w - 2.0F;
      b.vx = fromRight ? -speed : speed;
      b.prevX = b.x;
      b.prevTop = b.top;
      b.prevH = b.h;
      this.spikes.add(b);
      return b;
   }

   private GimmickModes.Spike ceilingSpike(float h, float w, boolean fromRight, float speed) {
      GimmickModes.Spike b = this.floorSpike(h, w, fromRight, speed);
      b.top = (float)this.screen.boxY;
      b.prevTop = b.top;
      b.ceiling = true;
      return b;
   }

   private GimmickModes.Platform platform(float heightAboveFloor, float w, boolean fromRight, float speed) {
      GimmickModes.Platform p = new GimmickModes.Platform();
      p.w = w;
      p.y = this.floorY() - heightAboveFloor;
      p.x = fromRight ? this.bxR() + 2.0F : this.bxL() - w - 2.0F;
      p.vx = fromRight ? -speed : speed;
      p.prevX = p.x;
      this.platforms.add(p);
      return p;
   }

   private void bouncingPlatform(float fracX, float heightAboveFloor, float w, float speed, int expire) {
      GimmickModes.Platform p = this.platform(heightAboveFloor, w, true, speed);
      p.x = this.bxL() + fracX * ((float)this.screen.targetBoxWidth - w);
      p.prevX = p.x;
      p.bounce = true;
      p.expire = expire;
   }

   private void stab(float x, float w, float height, int warn, int hold) {
      GimmickModes.Spike sp = new GimmickModes.Spike();
      sp.w = w;
      sp.x = Math.max(this.bxL(), Math.min(this.bxR() - w, x));
      sp.h = 0.0F;
      sp.top = this.floorY();
      sp.stabH = height;
      sp.warn = warn;
      sp.hold = hold;
      sp.prevX = sp.x;
      sp.prevTop = sp.top;
      sp.prevH = 0.0F;
      this.spikes.add(sp);
   }

   private void bluePattern(int p, int a, int b) {
      float s = this.spd();
      switch (p) {
         case 0:
            boolean right = this.rng.nextBoolean();
            int tx = a;

            while (tx < b - 10) {
               boolean r = right;
               float h = this.rng.nextFloat() < 0.25F ? 24.0F + (float)this.rng.nextInt(5) : 10.0F + (float)this.rng.nextInt(9);
               float w = this.rng.nextFloat() < 0.3F ? 16.0F : 8.0F;
               this.at(tx, () -> this.floorSpike(h, w, r, 2.3F * s));
               tx += 18 + this.rng.nextInt(10);
               if (this.rng.nextFloat() < 0.15F) {
                  right = !right;
               }
            }
            break;
         case 1:
            boolean right = this.rng.nextBoolean();
            int n = 0;

            for (int tx = a; tx < b - 10; n++) {
               boolean r = right;
               if (n % 2 == 0) {
                  this.at(tx, () -> this.floorSpike(13.0F, 8.0F, r, 2.1F * s));
               } else {
                  this.at(tx, () -> this.ceilingSpike((float)this.screen.targetBoxHeight - 20.0F, 8.0F, r, 2.1F * s));
               }

               tx += 26;
            }
            break;
         case 2:
            int expire = b + 4;
            this.at(a, () -> {
               this.bouncingPlatform(0.15F, 28.0F, 40.0F, 0.9F * s, expire);
               this.bouncingPlatform(0.75F, 30.0F, 40.0F, -1.1F * s, expire);
            });
            int carpetStart = a + 10;
            int warn = 34;
            int hold = Math.max(20, b - carpetStart - warn - 14);
            this.at(carpetStart, () -> this.stab(this.bxL(), (float)this.screen.targetBoxWidth, 10.0F, warn, hold));

            for (int tx = carpetStart + warn + 30; tx < b - 30; tx += 42) {
               boolean r = this.rng.nextBoolean();
               this.at(tx, () -> this.floorSpike(52.0F, 6.0F, r, 1.6F * s));
            }
            break;
         case 3:
            for (int t = a; t < b - 16; t += 40) {
               float gapBottom = this.rng.nextBoolean() ? 0.0F : 12.0F + (float)this.rng.nextInt(12);
               float gap = 30.0F;
               float H = (float)this.screen.targetBoxHeight;
               this.at(t, () -> {
                  if (gapBottom > 0.0F) {
                     this.floorSpike(gapBottom, 8.0F, true, 1.9F * s);
                  }

                  this.ceilingSpike(H - gapBottom - gap, 8.0F, true, 1.9F * s);
               });
               if (gapBottom > 14.0F) {
                  this.at(t - 12, () -> this.platform(gapBottom - 2.0F, 30.0F, true, 1.9F * s));
               }
            }
            break;
         case 4:
            boolean right = this.rng.nextBoolean();

            for (int t = a; t < b - 12; t += 46) {
               this.at(t, () -> {
                  GimmickModes.Spike sp = this.floorSpike((float)this.screen.targetBoxHeight, 8.0F, right, 2.2F * s);
                  sp.blue = true;
               });
               this.at(t + 23, () -> this.floorSpike(10.0F, 8.0F, right, 2.2F * s));
            }
            break;
         default:
            for (int t = a; t < b - 30; t += 30) {
               this.at(
                  t,
                  () -> {
                     float w = 30.0F + (float)this.rng.nextInt(24);
                     float center = this.rng.nextFloat() < 0.5F
                        ? this.screen.soulX + 4.5F
                        : this.bxL() + 10.0F + this.rng.nextFloat() * ((float)this.screen.targetBoxWidth - 20.0F);
                     this.stab(center - w / 2.0F, w, 40.0F, 20, 12);
                  }
               );
            }
      }
   }

   void tickBlueWorld(int now) {
      for (GimmickModes.Ev e : this.events) {
         if (e.tick() == now) {
            e.r().run();
         }
      }

      for (GimmickModes.Platform p : this.platforms) {
         p.prevX = p.x;
         p.x = p.x + p.vx;
         if (p.bounce) {
            if (p.x < this.bxL()) {
               p.x = this.bxL();
               p.vx = Math.abs(p.vx);
            }

            if (p.x + p.w > this.bxR()) {
               p.x = this.bxR() - p.w;
               p.vx = -Math.abs(p.vx);
            }
         }
      }

      this.platforms.removeIf(px -> now > px.expire || px.x > this.bxR() + 60.0F || px.x + px.w < this.bxL() - 60.0F);
      if (this.standing != null && !this.platforms.contains(this.standing)) {
         this.standing = null;
      }

      for (GimmickModes.Spike b : this.spikes) {
         b.prevX = b.x;
         b.prevTop = b.top;
         b.prevH = b.h;
         if (b.stabH > 0.0F) {
            if (b.warn > 0) {
               b.warn--;
            } else {
               b.stab++;
               int top = 4 + b.hold;
               float rise = b.stab < 4 ? (float)b.stab / 4.0F : (b.stab < top ? 1.0F : 1.0F - (float)(b.stab - top) / 6.0F);
               b.h = b.stabH * clamp01(rise);
               b.top = this.floorY() - b.h;
            }
         } else {
            b.x = b.x + b.vx;
         }
      }

      this.spikes.removeIf(bx -> bx.stabH > 0.0F && bx.stab > 4 + bx.hold + 6 || bx.x > this.bxR() + 40.0F || bx.x + bx.w < this.bxL() - 40.0F);
   }

   void tickBlueSoul() {
      int S = 9;
      float floor = this.floorY() - (float)S;
      if (this.standing != null) {
         this.screen.soulX = this.screen.soulX + this.standing.vx;
      }

      boolean grounded = this.screen.soulY >= floor - 0.5F || this.standing != null;
      if (this.screen.keyW && !this.prevW && grounded) {
         this.vy = -4.8F;
         this.standing = null;
      }

      this.prevW = this.screen.keyW;
      if (!this.screen.keyW && this.vy < 0.0F) {
         this.vy *= 0.5F;
         if (this.vy > -0.5F) {
            this.vy = 0.0F;
         }
      }

      float prevBottom = this.screen.soulY + (float)S;
      if (this.standing == null) {
         this.vy = Math.min(6.0F, this.vy + 0.3F);
         this.screen.soulY = this.screen.soulY + this.vy;
      }

      if (this.screen.keyA) {
         this.screen.soulX = this.screen.soulX - this.screen.effectiveSoulSpeed();
      }

      if (this.screen.keyD) {
         this.screen.soulX = this.screen.soulX + this.screen.effectiveSoulSpeed();
      }

      this.screen.soulX = BattleScreen.clamp(this.screen.soulX, (float)this.screen.boxX, (float)(this.screen.boxX + this.screen.targetBoxWidth - S));
      if (this.standing != null && (this.screen.soulX + (float)S < this.standing.x + 1.0F || this.screen.soulX > this.standing.x + this.standing.w - 1.0F)) {
         this.standing = null;
         this.vy = 0.0F;
      }

      if (this.standing == null && this.vy >= 0.0F) {
         for (GimmickModes.Platform p : this.platforms) {
            boolean overX = this.screen.soulX + (float)S > p.x + 1.0F && this.screen.soulX < p.x + p.w - 1.0F;
            if (overX && prevBottom <= p.y + 0.5F && this.screen.soulY + (float)S >= p.y) {
               this.screen.soulY = p.y - (float)S;
               this.vy = 0.0F;
               this.standing = p;
               break;
            }
         }
      }

      if (this.standing != null) {
         this.screen.soulY = this.standing.y - (float)S;
      }

      if (this.screen.soulY >= floor) {
         this.screen.soulY = floor;
         this.vy = 0.0F;
      }

      if (this.screen.soulY < (float)this.screen.boxY) {
         this.screen.soulY = (float)this.screen.boxY;
         if (this.vy < 0.0F) {
            this.vy = 0.0F;
         }
      }

      this.moving = this.screen.keyA || this.screen.keyD || this.standing == null && this.screen.soulY < floor - 0.5F;
   }

   private int kind(GimmickModes.Spike s) {
      if (s.stabH > 0.0F) {
         return 0;
      } else if (s.h >= (float)this.screen.targetBoxHeight - 0.5F) {
         return 2;
      } else {
         return s.h > 32.0F ? 1 : 0;
      }
   }

   private static int teeth(GimmickModes.Spike s) {
      return Math.max(1, Math.round(s.w / 8.0F));
   }

   private static float tipLen(float h) {
      return Math.min(12.0F, h / 3.0F);
   }

   private float reach(GimmickModes.Spike s, int kind, float u) {
      if (!(u < s.x) && !(u > s.x + s.w)) {
         return switch (kind) {
            case 0 -> {
               int n = teeth(s);
               float tw = s.w / (float)n;
               float local = u - s.x - tw * (float)Math.min(n - 1, (int)((u - s.x) / tw));
               yield s.h * Math.max(0.0F, 1.0F - Math.abs(local - tw / 2.0F) / (tw / 2.0F));
            }
            case 1 -> s.h - tipLen(s.h) * Math.min(1.0F, Math.abs(u - (s.x + s.w / 2.0F)) / (s.w / 2.0F));
            default -> s.h;
         };
      } else {
         return 0.0F;
      }
   }

   private float maxReach(GimmickModes.Spike s, float a, float b) {
      a = Math.max(a, s.x);
      b = Math.min(b, s.x + s.w);
      if (a >= b) {
         return 0.0F;
      } else {
         int k = this.kind(s);
         float m = Math.max(this.reach(s, k, a), this.reach(s, k, b));
         if (k == 0) {
            int n = teeth(s);
            float tw = s.w / (float)n;

            for (int i = 0; i < n; i++) {
               float c = s.x + tw * ((float)i + 0.5F);
               if (c > a && c < b) {
                  return s.h;
               }
            }
         } else if (k == 1) {
            float c = s.x + s.w / 2.0F;
            if (c > a && c < b) {
               return s.h;
            }
         }

         return m;
      }
   }

   private boolean touches(GimmickModes.Spike s, float x1, float y1, float x2, float y2) {
      if (s.h < 2.0F) {
         return false;
      } else {
         float r = this.maxReach(s, x1, x2);
         if (r <= 0.0F) {
            return false;
         } else {
            return s.ceiling ? y1 < (float)this.screen.boxY + r : y2 > this.floorY() - r;
         }
      }
   }

   void collideBlue() {
      int S = 9;
      float inset = (float)S * 0.25F;
      float hx1 = this.screen.soulX + inset;
      float hy1 = this.screen.soulY + inset;
      float hx2 = this.screen.soulX + (float)S - inset;
      float hy2 = this.screen.soulY + (float)S - inset;

      for (GimmickModes.Spike b : this.spikes) {
         boolean hit = this.touches(b, hx1, hy1, hx2, hy2);
         if (hit && (!b.blue || this.moving)) {
            if (this.screen.iframeTicks <= 0) {
               this.bh.hitSoul();
            }
         } else if (!b.grazed && !hit) {
            float g = 5.0F;
            if (this.touches(b, hx1 - g, hy1 - g, hx2 + g, hy2 + g)) {
               b.grazed = true;
               this.bh.awardGraze(true);
            }
         }
      }
   }

   void renderBlue(DrawContext ctx, float delta) {
      int bx = this.screen.boxX;
      int by = this.screen.boxY;
      int bw = this.screen.targetBoxWidth;
      int bhh = this.screen.targetBoxHeight;
      ctx.enableScissor(bx, by, bx + bw, by + bhh);

      for (GimmickModes.Platform p : this.platforms) {
         float x = mix(p.prevX, p.x, delta);
         fillF(ctx, x - 1.0F, p.y - 1.0F, x + p.w + 1.0F, p.y + 5.0F, -15844834);
         fillF(ctx, x, p.y, x + p.w, p.y + 4.0F, -12986514);
         fillF(ctx, x, p.y, x + p.w, p.y + 1.0F, -4784181);

         for (int i = 3; (float)i < p.w - 2.0F; i += 6) {
            fillF(ctx, x + (float)i, p.y + 2.0F, x + (float)i + 2.0F, p.y + 3.0F, -14710203);
         }
      }

      for (GimmickModes.Spike b : this.spikes) {
         if (!(b.stabH > 0.0F) || b.warn <= 0) {
            float x = mix(b.prevX, b.x, delta);
            float h = mix(b.prevH, b.h, delta);
            if (!(h < 1.0F)) {
               int body = b.blue ? -12604929 : -1643790;
               int edge = b.blue ? -15846304 : -14276042;
               int lit = b.blue ? -4922625 : -1;
               int shade = b.blue ? -14387256 : -6643020;
               float anchor = b.ceiling ? (float)this.screen.boxY : this.floorY();
               switch (this.kind(b)) {
                  case 0:
                     int n = teeth(b);
                     float tw = b.w / (float)n;

                     for (int i = 0; i < n; i++) {
                        drawTooth(ctx, x + tw * (float)i, tw, anchor, h, !b.ceiling, body, edge, lit, shade);
                     }
                     break;
                  case 1:
                     drawPole(ctx, x, b.w, anchor, h, !b.ceiling, true, body, edge, lit, shade);
                     break;
                  default:
                     drawPole(ctx, x, b.w, this.floorY(), (float)this.screen.targetBoxHeight, true, false, body, edge, lit, shade);
               }
            }
         } else if (b.warn / 3 % 2 == 0) {
            float fy = this.floorY();
            int c = -49088;
            fillF(ctx, b.x, fy - b.stabH, b.x + b.w, fy - b.stabH + 1.0F, c);
            fillF(ctx, b.x, fy - b.stabH, b.x + 1.0F, fy, c);
            fillF(ctx, b.x + b.w - 1.0F, fy - b.stabH, b.x + b.w, fy, c);
            ctx.drawTextWithShadow(
               this.screen.getTextRenderer(), Text.literal("!"), Math.round(b.x + b.w / 2.0F - 2.0F), Math.round(fy - b.stabH / 2.0F - 4.0F), -49088
            );
         }
      }

      ctx.disableScissor();
   }

   private static void drawTooth(DrawContext ctx, float x, float w, float anchor, float h, boolean up, int body, int edge, int lit, int shade) {
      float cx = x + w / 2.0F;
      int rows = Math.max(1, Math.round(h));

      for (int r = 0; r < rows; r++) {
         float half = w / 2.0F * (1.0F - ((float)r + 0.5F) / (float)rows);
         float y0 = up ? anchor - (float)r - 1.0F : anchor + (float)r;
         float y1 = y0 + 1.0F;
         fillF(ctx, cx - half - 1.0F, y0, cx + half + 1.0F, y1, edge);
         if (half >= 0.75F) {
            fillF(ctx, cx - half, y0, cx + half, y1, body);
            fillF(ctx, cx - half, y0, cx - half + 1.0F, y1, lit);
            fillF(ctx, cx + half * 0.35F, y0, cx + half, y1, shade);
         } else {
            fillF(ctx, cx - 0.5F, y0, cx + 0.5F, y1, lit);
         }
      }
   }

   private static void drawPole(DrawContext ctx, float x, float w, float anchor, float h, boolean up, boolean pointed, int body, int edge, int lit, int shade) {
      float tip = pointed ? tipLen(h) : 0.0F;
      float shaft = h - tip;
      float y0 = up ? anchor - shaft : anchor;
      float y1 = up ? anchor : anchor + shaft;

      for (float k = 4.0F; k < shaft - 2.0F; k += 7.0F) {
         float yy = up ? anchor - k : anchor + k;

         for (int side = -1; side <= 1; side += 2) {
            float bx = side < 0 ? x : x + w;

            for (int i = 0; i < 3; i++) {
               float len = 3.0F - (float)i;
               float px1 = side < 0 ? bx - len : bx;
               float px2 = side < 0 ? bx : bx + len;
               float py = up ? yy - (float)i : yy + (float)i;
               fillF(ctx, px1 - (float)(side < 0 ? 1 : 0), py, px2 + (float)(side > 0 ? 1 : 0), py + 1.0F, edge);
               fillF(ctx, px1, py, px2, py + 1.0F, side < 0 ? lit : shade);
            }
         }
      }

      fillF(ctx, x - 1.0F, y0, x + w + 1.0F, y1, edge);
      fillF(ctx, x, y0, x + w, y1, body);
      fillF(ctx, x, y0, x + 1.0F, y1, lit);
      fillF(ctx, x + w * 0.6F, y0, x + w, y1, shade);

      for (float k = 3.0F; k < shaft - 1.0F; k += 9.0F) {
         float yy = up ? anchor - k : anchor + k;
         fillF(ctx, x + w / 2.0F - 1.0F, yy, x + w / 2.0F + 1.0F, yy + 1.0F, edge);
      }

      if (pointed) {
         drawTooth(ctx, x, w, up ? anchor - shaft : anchor + shaft, tip, up, body, edge, lit, shade);
      }
   }

   private static record Ev(int tick, Runnable r) {
   }

   private static final class Platform {
      float x;
      float y;
      float w;
      float vx;
      float prevX;
      boolean bounce;
      int expire = Integer.MAX_VALUE;
   }

   private static final class Spark {
      float x;
      float y;
      int age;
      boolean blocked;
   }

   private static final class Spear {
      int spawn;
      int arrive;
      int dir;
      boolean trick;
      boolean done;
      float x;
      float y;
      float ang;
      float prevX;
      float prevY;
      float prevAng;
      boolean seen;
   }

   private static final class Spike {
      float x;
      float top;
      float w;
      float h;
      float vx;
      boolean blue;
      boolean ceiling;
      int warn;
      int stab;
      float stabH;
      int hold = 12;
      boolean grazed;
      float prevX;
      float prevTop;
      float prevH;
   }
}
