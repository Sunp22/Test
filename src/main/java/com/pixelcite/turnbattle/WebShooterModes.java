package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

final class WebShooterModes {
   private final BulletHellRenderer bh;
   private final BattleScreen screen;
   private static final float PI = (float) Math.PI;
   private static final float TAU = (float) (Math.PI * 2);
   private static final float SOUL_R = 1.8F;
   private static final int P_RUNNER = 0;
   private static final int P_FALLER = 1;
   private static final int P_BOOMERANG = 2;
   private static final int SURGE_WARN = 30;
   private static final int SURGE_UP = 36;
   private static final int PLUCK_WARN = 26;
   private static final int PLUCK_HIT = 10;
   private final List<WebShooterModes.Bug> bugs = new ArrayList<>();
   private int purplePattern;
   private int pDir;
   private int lastBoomerangLine;
   private float pSpeed;
   private final List<int[]> surgeAt = new ArrayList<>();
   private final List<int[]> plucks = new ArrayList<>();
   private int purpleTick;
   private static final Identifier BULLET_TEX = Identifier.of("turnbattle", "textures/gui/bullet.png");
   private static final int T_BLOCK = 0;
   private static final int T_STEEL = 1;
   private static final int T_BOMB = 2;
   private static final int T_TURRET = 3;
   private static final int T_PELLET = 4;
   private static final int CHARGE_START = 6;
   private static final int CHARGE_FULL = 34;
   private static final int BOMB_FUSE = 12;
   private static final int BLAST_LIFE = 9;
   private static final float BLAST_ARM = 30.0F;
   private static final float BLAST_HALF = 4.0F;
   private final List<WebShooterModes.Tgt> tgts = new ArrayList<>();
   private final List<WebShooterModes.Shot> shots = new ArrayList<>();
   private final List<WebShooterModes.Blast> blasts = new ArrayList<>();
   private final List<WebShooterModes.Bit> bits = new ArrayList<>();
   private int yellowPattern;
   private int yellowTick;
   private int shotIds;
   int chargeTicks;
   boolean charged;
   private int chargedAge;
   private int readyFlash;
   private static final float HUM_VOLUME = 0.9F;
   private WebShooterModes.ChargeHum hum;
   private int yellowTicks;

   WebShooterModes(BulletHellRenderer bh, BattleScreen screen) {
      this.bh = bh;
      this.screen = screen;
   }

   private static Random rng() {
      return BulletPatterns.RNG;
   }

   private static float rf(float a, float b) {
      return a + rng().nextFloat() * (b - a);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private float spd() {
      return Math.max(0.6F, Math.min(1.3F, this.screen.bulletSpeedMult));
   }

   private float scx() {
      return this.screen.soulX + 4.5F;
   }

   private float scy() {
      return this.screen.soulY + 4.5F;
   }

   private void hit() {
      if (this.screen.iframeTicks <= 0) {
         this.bh.hitSoul();
      }
   }

   private static void play(SoundEvent ev, float pitch, float vol) {
      MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ev, pitch, vol));
   }

   float lineY(int i) {
      return (float)(this.screen.boxY + i * this.screen.targetBoxHeight / 3 + this.screen.targetBoxHeight / 6);
   }

   private int soulLine() {
      float best = Float.MAX_VALUE;
      int bi = 1;

      for (int i = 0; i < 3; i++) {
         float d = Math.abs(this.lineY(i) - this.scy());
         if (d < best) {
            best = d;
            bi = i;
         }
      }

      return bi;
   }

   void startPurple() {
      this.bugs.clear();
      this.surgeAt.clear();
      this.plucks.clear();
      this.purplePattern = rng().nextInt(4);
      this.pDir = rng().nextBoolean() ? 1 : -1;
      this.pSpeed = (1.7F + rng().nextFloat() * 0.5F) * this.spd();
      this.lastBoomerangLine = 1;
      this.purpleTick = 0;
      if (this.purplePattern == 3) {
         this.surgeAt.add(new int[]{30});
         this.surgeAt.add(new int[]{140});
         this.plucks.add(new int[]{rng().nextInt(2), 100});
      }
   }

   private boolean surgeUp(int t) {
      for (int[] p : this.surgeAt) {
         if (t >= p[0] + 30 && t < p[0] + 30 + 36) {
            return true;
         }
      }

      return false;
   }

   private boolean surgeSoon(int t, int within) {
      for (int[] p : this.surgeAt) {
         if (t + within >= p[0] && t < p[0] + 30 + 36) {
            return true;
         }
      }

      return false;
   }

   private void runner(int line, int dir, float speed) {
      WebShooterModes.Bug b = new WebShooterModes.Bug();
      b.kind = 0;
      b.line = line;
      b.r = 3.2F;
      b.x = dir > 0 ? (float)(this.screen.boxX - 6) : (float)(this.screen.boxX + this.screen.targetBoxWidth + 6);
      b.y = this.lineY(line);
      b.vx = (float)dir * speed;
      b.prevX = b.x;
      b.prevY = b.y;
      this.bugs.add(b);
   }

   void tickPurple(int t) {
      this.purpleTick = t;
      int bx = this.screen.boxX;
      int by = this.screen.boxY;
      int bw = this.screen.targetBoxWidth;
      float sp = this.spd();
      switch (this.purplePattern) {
         case 0:
            if (t % 15 == 4 && t < 186) {
               boolean two = t > 40 && rng().nextFloat() < 0.45F;
               int skip = rng().nextInt(3);
               int one = rng().nextInt(3);

               for (int l = 0; l < 3; l++) {
                  if (two ? l != skip : l == one) {
                     this.runner(l, this.pDir, this.pSpeed);
                  }
               }
            }
            break;
         case 1:
            if (t % 10 == 3 && t < 186) {
               WebShooterModes.Bug d = new WebShooterModes.Bug();
               d.kind = 1;
               d.r = 3.4F;
               d.x = (float)bx + rf(10.0F, (float)(bw - 10));
               d.y = (float)(by - 6);
               d.vx = rf(-0.45F, 0.45F) * sp;
               d.vy = 0.6F * sp;
               d.ay = 0.035F * sp * sp;
               d.prevX = d.x;
               d.prevY = d.y;
               this.bugs.add(d);
            }

            if (t % 36 == 20 && t < 180) {
               this.runner(rng().nextInt(3), this.pDir, this.pSpeed * 0.9F);
            }
            break;
         case 2:
            if (t % 20 == 4 && t < 176) {
               int l = rng().nextInt(3);
               int side = rng().nextBoolean() ? 1 : -1;
               WebShooterModes.Bug c = new WebShooterModes.Bug();
               c.kind = 2;
               c.line = l;
               c.r = 3.6F;
               c.side = side;
               c.x = side > 0 ? (float)(bx - 8) : (float)(bx + bw + 8);
               c.y = this.lineY(l);
               c.vx = (float)side * 3.4F * sp;
               c.ax = (float)(-side) * 0.065F * sp * sp;
               c.pending = 14;
               c.prevX = c.x;
               c.prevY = c.y;
               this.bugs.add(c);
               this.lastBoomerangLine = l;
            }

            if (t % 20 == 14 && t < 186) {
               this.runner((this.lastBoomerangLine + 1 + rng().nextInt(2)) % 3, this.pDir, this.pSpeed);
            }
            break;
         default:
            if (t % 20 == 6 && t < 186) {
               int l = rng().nextInt(3);
               if (l == 2 && this.surgeSoon(t, 60)) {
                  l = rng().nextInt(2);
               }

               this.runner(l, this.pDir, this.pSpeed);
            }
      }

      float scx = this.scx();
      float scy = this.scy();
      int sl = this.soulLine();

      for (WebShooterModes.Bug b : this.bugs) {
         b.prevX = b.x;
         b.prevY = b.y;
         if (b.pending > 0) {
            b.pending--;
         } else {
            b.age++;
            b.vx = b.vx + b.ax;
            b.vy = b.vy + b.ay;
            b.x = b.x + b.vx;
            b.y = b.y + b.vy;
            if (b.kind == 2 && b.vx * (float)b.side < 0.0F && (b.side > 0 ? b.x < (float)(bx - 10) : b.x > (float)(bx + bw + 10))) {
               b.dead = true;
            }

            if (b.x < (float)(bx - 24) || b.x > (float)(bx + bw + 24) || b.y > (float)(by + this.screen.targetBoxHeight + 12)) {
               b.dead = true;
            }

            if (!b.dead) {
               float dx = b.x - scx;
               float dy = b.y - scy;
               float d2 = dx * dx + dy * dy;
               float hr = b.r + 1.8F;
               if (d2 < hr * hr && this.screen.iframeTicks <= 0) {
                  this.hit();
                  b.dead = true;
               } else if (!b.grazed && d2 < (hr + 9.0F) * (hr + 9.0F)) {
                  b.grazed = true;
                  this.bh.awardGraze(true);
               }
            }
         }
      }

      this.bugs.removeIf(bx -> bx.dead);
      if (this.surgeUp(t) && sl == 2) {
         this.hit();
      }

      for (int[] p : this.plucks) {
         if (t >= p[1] + 26 && t < p[1] + 26 + 10 && sl == p[0]) {
            this.hit();
         }
      }
   }

   void renderPurple(DrawContext ctx, float delta) {
      int bx = this.screen.boxX;
      int by = this.screen.boxY;
      int bw = this.screen.targetBoxWidth;
      int bh = this.screen.targetBoxHeight;
      int t = this.purpleTick;
      ctx.enableScissor(bx, by, bx + bw, by + bh);
      VfxBatch.draw(ctx, false, 1, v -> {
         for (int i = 0; i < 3; i++) {
            float y = this.lineY(i);
            float amp = 0.0F;
            int c = -5635841;
            float w = 2.0F;
            boolean live = false;

            for (int[] p : this.plucks) {
               if (p[0] == i) {
                  int a = t - p[1];
                  if (a >= 0 && a < 26) {
                     amp = 1.2F * (float)a / 26.0F;
                     if (a / 3 % 2 == 0) {
                        c = -49056;
                     }
                  } else if (a >= 26 && a < 36) {
                     live = true;
                  }
               }
            }

            if (i == 2) {
               for (int[] px : this.surgeAt) {
                  int a = t - px[0];
                  if (a >= 0 && a < 30) {
                     amp = 1.2F * (float)a / 30.0F;
                     if (a / 3 % 2 == 0) {
                        c = -49056;
                     }
                  } else if (a >= 30 && a < 66) {
                     live = true;
                  }
               }
            }

            if (live) {
               amp = 3.0F;
               c = -7937;
               w = 4.0F;
            }

            int seg = 20;

            for (int s = 0; s < seg; s++) {
               float x0 = (float)bx + (float)(bw * s) / (float)seg;
               float x1 = (float)bx + (float)(bw * (s + 1)) / (float)seg;
               float y0 = y + (float)Math.sin((double)((float)s * 1.3F + (float)t * 1.7F)) * amp;
               float y1 = y + (float)Math.sin((double)((float)(s + 1) * 1.3F + (float)t * 1.7F)) * amp;
               v.line(x0, y0, x1, y1, w, c, c);
            }
         }

         for (WebShooterModes.Bug b : this.bugs) {
            if (b.kind == 2 && b.pending > 0) {
               boolean fl = b.pending / 3 % 2 == 0;
               int c = VfxBatch.col(16728128, fl ? 0.9F : 0.4F);
               float ex = b.side > 0 ? (float)(bx + 4) : (float)(bx + bw - 4);
               float ux = (float)b.side;
               float y = b.y;
               v.line(ex, y, ex + ux * 14.0F, y, 1.0F, c, VfxBatch.col(16728128, 0.1F));
               v.line(ex + ux * 3.0F, y - 3.0F, ex + ux * 6.0F, y, 1.0F, c, c);
               v.line(ex + ux * 3.0F, y + 3.0F, ex + ux * 6.0F, y, 1.0F, c, c);
            }
         }
      });
      int tint = this.screen.battleElement.getBulletColor();
      RenderSystem.setShaderColor((float)(tint >> 16 & 0xFF) / 255.0F, (float)(tint >> 8 & 0xFF) / 255.0F, (float)(tint & 0xFF) / 255.0F, 1.0F);

      for (WebShooterModes.Bug b : this.bugs) {
         if (b.pending <= 0) {
            float x = b.prevX + (b.x - b.prevX) * delta;
            float y = b.prevY + (b.y - b.prevY) * delta;
            int d = Math.round(b.r * 2.0F + 1.0F);
            ctx.drawTexture(BULLET_TEX, Math.round(x - (float)d / 2.0F), Math.round(y - (float)d / 2.0F), d, d, 0.0F, 0.0F, 16, 16, 16, 16);
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      ctx.disableScissor();
   }

   void startYellow() {
      this.tgts.clear();
      this.shots.clear();
      this.blasts.clear();
      this.bits.clear();
      this.yellowPattern = rng().nextInt(4);
      this.chargeTicks = 0;
      this.charged = false;
      this.chargedAge = 0;
      this.readyFlash = 0;
      this.yellowTick = 0;
      this.hum = null;
   }

   private WebShooterModes.Tgt tgt(int kind, float x, float y, float w, float h, int hp, float vy) {
      WebShooterModes.Tgt g = new WebShooterModes.Tgt();
      g.kind = kind;
      g.x = x;
      g.y = y;
      g.w = w;
      g.h = h;
      g.hp = hp;
      g.vy = vy;
      g.prevX = x;
      g.prevY = y;
      this.tgts.add(g);
      return g;
   }

   void shoot() {
      WebShooterModes.Shot s = new WebShooterModes.Shot();
      s.x = this.scx();
      s.y = this.scy() - 6.0F;
      s.prevX = s.x;
      s.prevY = s.y;
      this.shots.add(s);
      play(TurnBattleSounds.SND_SHOOT, 1.0F, 0.8F);
   }

   private void shootBig() {
      WebShooterModes.Shot s = new WebShooterModes.Shot();
      s.big = true;
      s.id = ++this.shotIds;
      s.x = this.scx();
      s.y = this.scy() - 8.0F;
      s.prevX = s.x;
      s.prevY = s.y;
      this.shots.add(s);
      play(TurnBattleSounds.SND_CHARGESHOT_FIRE, 1.0F, 1.0F);
   }

   void tickYellow(int t, boolean held) {
      this.yellowTick = t;
      int bx = this.screen.boxX;
      int by = this.screen.boxY;
      int bw = this.screen.targetBoxWidth;
      int boxH = this.screen.targetBoxHeight;
      float sp = this.spd();
      float cw = (float)bw / 8.0F;
      this.yellowTicks++;
      if (held) {
         this.chargeTicks++;
         if (this.chargeTicks == 6) {
            this.hum = new WebShooterModes.ChargeHum();
            MinecraftClient.getInstance().getSoundManager().play(this.hum);
         }

         if (this.chargeTicks == 34) {
            this.charged = true;
            this.chargedAge = 0;
            this.readyFlash = 8;
         }
      } else {
         if (this.charged) {
            this.shootBig();
         }

         this.chargeTicks = 0;
         this.charged = false;
         this.hum = null;
      }

      if (this.charged) {
         this.chargedAge++;
      }

      if (this.readyFlash > 0) {
         this.readyFlash--;
      }

      switch (this.yellowPattern) {
         case 0:
            if (t % 28 == 2 && t < 168) {
               int gap = rng().nextInt(7);
               float slide = (float)(t / 28 % 2 == 0 ? 1 : -1) * 0.35F * sp;

               for (int c = -1; c < 9; c++) {
                  boolean tough = rng().nextFloat() < 0.3F;
                  if (c != gap && c != gap + 1) {
                     WebShooterModes.Tgt g = this.tgt(0, (float)bx + cw * ((float)c + 0.5F), (float)(by - 6), cw - 2.0F, 10.0F, tough ? 2 : 1, 1.4F * sp);
                     g.vx = slide;
                  }
               }
            }

            if (t % 30 == 17 && t < 186) {
               this.aimedPellet((float)bx + rf(10.0F, (float)(bw - 10)), (float)(by - 4), 1.6F * sp);
            }
            break;
         case 1:
            if (t == 4 || t == 44 || t == 84 || t == 124) {
               WebShooterModes.Tgt g = this.tgt(3, (float)bx + cw * ((float)rng().nextInt(8) + 0.5F), (float)(by - 8), cw - 2.0F, 14.0F, 5, 0.6F * sp);
               g.cool = 20;
            }

            if (t % 22 == 10 && t < 180) {
               this.tgt(0, (float)bx + cw * ((float)rng().nextInt(8) + 0.5F), (float)(by - 6), cw - 2.0F, 10.0F, 1, 1.6F * sp);
            }
            break;
         case 2:
            if (t % 32 == 2 && t < 168) {
               int gap = rng().nextInt(8);
               int b1 = rng().nextInt(8);
               int b2 = rng().nextInt(8);
               float slide = (float)(t / 32 % 2 == 0 ? 1 : -1) * 0.25F * sp;

               for (int cx = 0; cx < 8; cx++) {
                  boolean tough = rng().nextFloat() < 0.25F;
                  if (cx != gap) {
                     boolean bomb = cx == b1 || cx == b2;
                     WebShooterModes.Tgt g = this.tgt(
                        bomb ? 2 : 0, (float)bx + cw * ((float)cx + 0.5F), (float)(by - 6), cw - 2.0F, 10.0F, bomb ? 1 : (tough ? 2 : 1), 1.3F * sp
                     );
                     g.vx = slide;
                  }
               }
            }
            break;
         default:
            if (t == 6 || t == 66 || t == 126) {
               int crack = rng().nextInt(8);

               for (int cx = 0; cx < 8; cx++) {
                  this.tgt(cx == crack ? 0 : 1, (float)bx + cw * ((float)cx + 0.5F), (float)(by - 6), cw - 0.5F, 10.0F, cx == crack ? 3 : 1, 1.1F * sp);
               }
            }

            if (t % 12 == 8 && t < 186) {
               WebShooterModes.Tgt p = this.tgt(4, (float)bx + rf(6.0F, (float)(bw - 6)), (float)(by - 4), 5.0F, 5.0F, 1, 1.5F * sp);
               p.vx = 0.0F;
            }
      }

      float scx = this.scx();
      float scy = this.scy();
      List fresh = new ArrayList<>();

      for (WebShooterModes.Tgt g : this.tgts) {
         g.prevX = g.x;
         g.prevY = g.y;
         g.age++;
         if (g.flash > 0) {
            g.flash--;
         }

         g.x = g.x + g.vx;
         g.y = g.y + g.vy;
         if (g.kind == 3 && g.y > (float)(by + 4) && --g.cool <= 0) {
            g.cool = 40;
            float a0 = (float)Math.atan2((double)(scy - g.y), (double)(scx - g.x));

            for (int k = -1; k <= 1; k++) {
               WebShooterModes.Tgt p = new WebShooterModes.Tgt();
               p.kind = 4;
               p.w = 5.0F;
               p.h = 5.0F;
               p.hp = 1;
               p.x = g.x;
               p.y = g.y + g.h / 2.0F;
               p.prevX = p.x;
               p.prevY = p.y;
               p.vx = (float)Math.cos((double)(a0 + (float)k * 0.32F)) * 1.6F * sp;
               p.vy = (float)Math.sin((double)(a0 + (float)k * 0.32F)) * 1.6F * sp;
               fresh.add(p);
            }
         }

         if (g.kind == 2 && g.fuse < 0 && g.y > (float)by + (float)boxH * 0.6F) {
            g.fuse = 18;
         }

         if (g.fuse > 0 && --g.fuse == 0) {
            this.explode(g);
         }

         if (g.y - g.h / 2.0F > (float)(by + boxH + 4) || g.x < (float)(bx - 20) || g.x > (float)(bx + bw + 20)) {
            g.dead = true;
         }
      }

      this.tgts.addAll(fresh);

      for (WebShooterModes.Shot s : this.shots) {
         s.prevX = s.x;
         s.prevY = s.y;
         s.y = s.y - (s.big ? 4.5F : 6.0F);
         if (s.y < (float)(by - 10)) {
            s.dead = true;
         } else {
            float r = s.big ? 6.0F : 2.0F;

            for (WebShooterModes.Tgt g : this.tgts) {
               if (!g.dead && !s.dead && !(Math.abs(s.x - g.x) > g.w / 2.0F + r) && !(Math.abs(s.y - g.y) > g.h / 2.0F + r)) {
                  if (s.big) {
                     if (g.lastBig != s.id) {
                        g.lastBig = s.id;
                        if (g.kind == 2) {
                           if (g.fuse < 0 || g.fuse > 2) {
                              g.fuse = 2;
                           }
                        } else {
                           this.destroy(g);
                        }
                     }
                  } else {
                     s.dead = true;
                     switch (g.kind) {
                        case 1:
                           g.flash = 3;
                           this.spark(s.x, g.y + g.h / 2.0F, -1513232);
                           break;
                        case 2:
                           if (g.fuse < 0) {
                              g.fuse = 12;
                           }
                           break;
                        default:
                           g.flash = 3;
                           if (--g.hp <= 0) {
                              this.destroy(g);
                           }
                     }
                  }
               }
            }
         }
      }

      this.shots.removeIf(sx -> sx.dead);

      for (WebShooterModes.Blast b : this.blasts) {
         b.age++;
         if (b.age <= 9) {
            for (WebShooterModes.Tgt gx : this.tgts) {
               if (!gx.dead && this.inCross(b, gx.x, gx.y, gx.w / 2.0F, gx.h / 2.0F)) {
                  if (gx.kind == 2) {
                     if (gx.fuse < 0) {
                        gx.fuse = 4;
                     }
                  } else if (gx.kind != 1) {
                     this.destroy(gx);
                  }
               }
            }

            if (this.inCross(b, scx, scy, 1.8F, 1.8F)) {
               this.hit();
            }
         }
      }

      this.blasts.removeIf(bxx -> bxx.age > 15);
      this.tgts.removeIf(gxx -> gxx.dead);

      for (WebShooterModes.Tgt gxx : this.tgts) {
         float ex = Math.max(Math.abs(scx - gxx.x) - gxx.w / 2.0F, 0.0F);
         float ey = Math.max(Math.abs(scy - gxx.y) - gxx.h / 2.0F, 0.0F);
         float d2 = ex * ex + ey * ey;
         if (d2 < 3.2399998F && this.screen.iframeTicks <= 0) {
            this.hit();
            if (gxx.kind == 4) {
               gxx.dead = true;
            }
         } else if (!gxx.grazed && d2 < 64.0F) {
            gxx.grazed = true;
            this.bh.awardGraze(true);
         }
      }

      this.tgts.removeIf(gxxx -> gxxx.dead);

      for (WebShooterModes.Bit bxx : this.bits) {
         bxx.age++;
         bxx.x = bxx.x + bxx.vx;
         bxx.y = bxx.y + bxx.vy;
         bxx.vy += 0.08F;
      }

      this.bits.removeIf(bxx -> bxx.age > bxx.life);
   }

   private void aimedPellet(float x, float y, float speed) {
      float dx = this.scx() - x;
      float dy = this.scy() - y;
      float l = Math.max(1.0F, (float)Math.sqrt((double)(dx * dx + dy * dy)));
      WebShooterModes.Tgt p = this.tgt(4, x, y, 5.0F, 5.0F, 1, dy / l * speed);
      p.vx = dx / l * speed;
   }

   private boolean inCross(WebShooterModes.Blast b, float x, float y, float hx, float hy) {
      boolean horiz = Math.abs(y - b.y) < 4.0F + hy && Math.abs(x - b.x) < 30.0F + hx;
      boolean vert = Math.abs(x - b.x) < 4.0F + hx && Math.abs(y - b.y) < 30.0F + hy;
      return horiz || vert;
   }

   private void explode(WebShooterModes.Tgt g) {
      WebShooterModes.Blast b = new WebShooterModes.Blast();
      b.x = g.x;
      b.y = g.y;
      this.blasts.add(b);
      g.dead = true;
      this.burst(g.x, g.y, -16320, 10);
   }

   private void destroy(WebShooterModes.Tgt g) {
      if (g.kind == 2) {
         this.explode(g);
      } else {
         g.dead = true;

         int c = switch (g.kind) {
            case 1 -> -4670264;
            default -> -2054064;
            case 3 -> -7705912;
            case 4 -> -24512;
         };
         this.burst(g.x, g.y, c, g.kind == 4 ? 3 : 7);
      }
   }

   private void spark(float x, float y, int c) {
      this.burst(x, y, c, 2);
   }

   private void burst(float x, float y, int c, int n) {
      for (int i = 0; i < n; i++) {
         WebShooterModes.Bit b = new WebShooterModes.Bit();
         float a = (float)(Math.random() * (float) (Math.PI * 2));
         float s = 0.6F + (float)Math.random() * 1.4F;
         b.x = x;
         b.y = y;
         b.vx = (float)Math.cos((double)a) * s;
         b.vy = (float)Math.sin((double)a) * s - 0.6F;
         b.life = 10 + (int)(Math.random() * 8.0);
         b.color = c;
         this.bits.add(b);
      }
   }

   void renderYellow(DrawContext ctx, float delta) {
      int bx = this.screen.boxX;
      int by = this.screen.boxY;
      int bw = this.screen.targetBoxWidth;
      int bh = this.screen.targetBoxHeight;
      int t = this.yellowTick;
      ctx.enableScissor(bx, by, bx + bw, by + bh);
      VfxBatch.draw(ctx, false, 1, v -> {
         for (WebShooterModes.Tgt g : this.tgts) {
            float x = g.prevX + (g.x - g.prevX) * delta;
            float y = g.prevY + (g.y - g.prevY) * delta;
            boolean fl = g.flash > 0;
            switch (g.kind) {
               case 0:
                  boolean cracked = this.yellowPattern == 3;
                  int base = cracked ? -3624816 : -2580408;
                  int edge = cracked ? -7704496 : -7710176;
                  v.rect(x, y, g.w, g.h, 0.0F, fl ? -1 : edge);
                  v.rect(x, y, g.w - 2.0F, g.h - 2.0F, 0.0F, fl ? -1 : base);
                  if (fl) {
                     break;
                  }

                  v.line(x - g.w / 2.0F + 1.0F, y - g.h / 2.0F + 1.0F, x + g.w / 2.0F - 1.0F, y + g.h / 2.0F - 1.0F, 0.8F, edge, edge);
                  if (g.hp < (cracked ? 3 : 2) || cracked) {
                     v.line(x - 2.0F, y - g.h / 2.0F + 1.0F, x + 1.0F, y, 0.8F, -12966896, -12966896);
                     v.line(x + 1.0F, y, x - 1.0F, y + g.h / 2.0F - 1.0F, 0.8F, -12966896, -12966896);
                  }

                  for (int k = 1; k < g.hp; k++) {
                     v.rect(x + g.w / 2.0F - 2.0F - (float)(k - 1) * 2.5F, y - g.h / 2.0F + 2.0F, 1.5F, 1.5F, 0.0F, -3920);
                  }
                  break;
               case 1:
                  v.rect(x, y, g.w, g.h, 0.0F, -11907496);
                  v.rect(x, y - 0.5F, g.w - 2.0F, g.h - 3.0F, 0.0F, fl ? -1 : -6643540);
                  v.rect(x, y - g.h / 2.0F + 1.5F, g.w - 2.0F, 1.0F, 0.0F, -2301720);
                  v.rect(x - g.w / 2.0F + 2.5F, y, 1.0F, 1.0F, 0.0F, -11907496);
                  v.rect(x + g.w / 2.0F - 2.5F, y, 1.0F, 1.0F, 0.0F, -11907496);
                  break;
               case 2:
                  boolean lit = g.fuse > 0 && g.fuse / 2 % 2 == 0;
                  v.disc(x, y + 0.5F, 4.5F, lit ? -49088 : -12961212, lit ? -5238768 : -15461350);
                  v.rect(x - 1.5F, y - 1.5F, 1.0F, 1.0F, 0.0F, -4671288);
                  v.line(x + 1.5F, y - 3.5F, x + 3.0F, y - 5.5F, 1.0F, -7706054, -7706054);
                  if (g.fuse > 0) {
                     int pc = VfxBatch.col(16728128, 0.35F + 0.4F * (1.0F - (float)g.fuse / 12.0F));
                     v.line(x - 30.0F, y - 4.0F, x + 30.0F, y - 4.0F, 0.8F, pc, pc);
                     v.line(x - 30.0F, y + 4.0F, x + 30.0F, y + 4.0F, 0.8F, pc, pc);
                     v.line(x - 4.0F, y - 30.0F, x - 4.0F, y + 30.0F, 0.8F, pc, pc);
                     v.line(x + 4.0F, y - 30.0F, x + 4.0F, y + 30.0F, 0.8F, pc, pc);
                  }
                  break;
               case 3:
                  float a = (float)Math.atan2((double)(this.scy() - y), (double)(this.scx() - x));
                  v.rect(x, y, g.w, g.h, 0.0F, fl ? -1 : -12965286);
                  v.rect(x, y, g.w - 3.0F, g.h - 3.0F, 0.0F, fl ? -1 : -9811296);
                  v.disc(x, y, 3.2F, -1, -2039568);
                  v.disc(x + (float)Math.cos((double)a) * 1.4F, y + (float)Math.sin((double)a) * 1.4F, 1.5F, -14677976, -14677976);

                  for (int k = 0; k < g.hp; k++) {
                     v.rect(x - g.w / 2.0F + 2.0F + (float)k * 2.4F, y + g.h / 2.0F - 1.5F, 1.4F, 1.0F, 0.0F, -8096);
                  }
                  break;
               case 4:
                  v.rect(x, y, 4.2F, 4.2F, (float)g.age * 0.2F, fl ? -1 : -24512);
            }
         }
      });
      VfxBatch.draw(ctx, true, 1, v -> {
         for (WebShooterModes.Tgt g : this.tgts) {
            if (g.kind == 4) {
               float x = g.prevX + (g.x - g.prevX) * delta;
               float y = g.prevY + (g.y - g.prevY) * delta;
               v.glow(x, y, 4.5F, VfxBatch.col(16744480, 0.4F));
            }
         }

         for (WebShooterModes.Blast b : this.blasts) {
            float k = clamp01((float)b.age / 9.0F);
            float a = b.age <= 9 ? 1.0F : 1.0F - (float)(b.age - 9) / 6.0F;
            float w = 8.0F * (b.age <= 9 ? 1.0F : a);
            v.line(b.x - 30.0F, b.y, b.x + 30.0F, b.y, w, VfxBatch.col(16769136, 0.9F * a), VfxBatch.col(16769136, 0.9F * a));
            v.line(b.x, b.y - 30.0F, b.x, b.y + 30.0F, w, VfxBatch.col(16769136, 0.9F * a), VfxBatch.col(16769136, 0.9F * a));
            v.line(b.x - 30.0F, b.y, b.x + 30.0F, b.y, w * 0.4F, VfxBatch.col(16777215, a), VfxBatch.col(16777215, a));
            v.line(b.x, b.y - 30.0F, b.x, b.y + 30.0F, w * 0.4F, VfxBatch.col(16777215, a), VfxBatch.col(16777215, a));
            v.glow(b.x, b.y, 10.0F + 6.0F * k, VfxBatch.col(16760896, 0.6F * a));
         }

         for (WebShooterModes.Shot s : this.shots) {
            float x = s.prevX + (s.x - s.prevX) * delta;
            float y = s.prevY + (s.y - s.prevY) * delta;
            if (s.big) {
               v.softTaper(x, y, x, y + 16.0F, 10.0F, 2.0F, VfxBatch.col(16769088, 0.6F), VfxBatch.col(16769088, 0.0F));
               v.glow(x, y, 9.0F, VfxBatch.col(16764960, 0.6F));
               v.disc(x, y, 5.0F, -1, VfxBatch.col(16771168, 0.95F));
               v.star(x, y, 6.0F, 4, (float)t * 0.3F, VfxBatch.col(16777215, 0.8F));
            } else {
               v.line(x, y - 3.0F, x, y + 3.0F, 2.2F, -64, VfxBatch.col(16769088, 0.7F));
               v.glow(x, y, 3.5F, VfxBatch.col(16769088, 0.4F));
            }
         }

         for (WebShooterModes.Bit b : this.bits) {
            v.rect(b.x, b.y, 1.0F, 1.0F, 0.0F, VfxBatch.col(b.color, 1.0F - (float)b.age / (float)b.life));
         }
      });
      ctx.disableScissor();
   }

   void renderCharge(DrawContext ctx, float delta, float rSoulX, float rSoulY, Identifier soulTex) {
      float cx = rSoulX + 4.5F;
      float cy = rSoulY + 4.5F;
      if (this.chargeTicks > 6 && !this.charged) {
         float q = clamp01(((float)this.chargeTicks + delta - 6.0F) / 28.0F);
         float rad = 24.0F - 18.0F * q * q * (3.0F - 2.0F * q);
         float spin = ((float)this.chargeTicks + delta) * (0.18F + 0.25F * q);
         VfxBatch.draw(ctx, false, 1, v -> {
            for (int i = 0; i < 4; i++) {
               float a = spin + (float)i * (float) Math.PI / 2.0F;
               float px = cx + (float)Math.cos((double)a) * rad;
               float py = cy + (float)Math.sin((double)a) * rad;
               v.disc(px, py, 3.4F, VfxBatch.col(16775328, 0.5F + 0.25F * q), VfxBatch.col(16769088, 0.4F + 0.25F * q));
            }
         });
         VfxBatch.draw(ctx, true, 1, v -> {
            for (int i = 0; i < 4; i++) {
               float a = spin + (float)i * (float) Math.PI / 2.0F;
               v.glow(cx + (float)Math.cos((double)a) * rad, cy + (float)Math.sin((double)a) * rad, 5.0F, VfxBatch.col(16769088, 0.25F + 0.2F * q));
            }
         });
      }

      if (this.readyFlash > 0) {
         float k = 1.0F - ((float)this.readyFlash - delta) / 8.0F;
         VfxBatch.draw(ctx, true, 1, v -> v.ring(cx, cy, 5.0F + 14.0F * k, 2.0F, VfxBatch.col(16774304, 0.9F * (1.0F - k))));
      }

      if (this.charged && soulTex != null) {
         for (int i = 0; i < 2; i++) {
            float ph = (((float)this.chargedAge + delta) / 16.0F + (float)i * 0.5F) % 1.0F;
            float sc = 1.0F + 1.1F * ph;
            float al = 0.5F * (1.0F - ph);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx, cy, 0.0F);
            ctx.getMatrices().scale(sc, sc, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 0.6F, al);
            ctx.drawTexture(soulTex, -4, -4, 0.0F, 0.0F, 9, 9, 9, 9);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            ctx.getMatrices().pop();
         }
      }
   }

   private static final class Bit {
      float x;
      float y;
      float vx;
      float vy;
      int age;
      int life;
      int color;
   }

   private static final class Blast {
      float x;
      float y;
      int age;
   }

   private static final class Bug {
      int kind;
      int line = -1;
      int age;
      int pending;
      int side;
      float x;
      float y;
      float vx;
      float vy;
      float ax;
      float ay;
      float prevX;
      float prevY;
      float r;
      boolean grazed;
      boolean dead;
   }

   private final class ChargeHum extends MovingSoundInstance {
      private int lastSeen = -1;
      private int stale;

      ChargeHum() {
         super(TurnBattleSounds.SND_CHARGESHOT_CHARGE, SoundCategory.MASTER, SoundInstance.createRandom());
         this.repeat = true;
         this.repeatDelay = 0;
         this.volume = 0.01F;
         this.pitch = 1.0F;
         this.relative = true;
         this.attenuationType = AttenuationType.NONE;
      }

      public boolean shouldAlwaysPlay() {
         return true;
      }

      public void tick() {
         if (WebShooterModes.this.hum == this && WebShooterModes.this.chargeTicks > 0) {
            if (WebShooterModes.this.yellowTicks == this.lastSeen) {
               if (++this.stale > 2) {
                  this.setDone();
                  return;
               }
            } else {
               this.stale = 0;
               this.lastSeen = WebShooterModes.this.yellowTicks;
            }

            float q = WebShooterModes.clamp01((float)(WebShooterModes.this.chargeTicks - 6) / 28.0F);
            this.volume = Math.max(0.01F, 0.9F * q);
         } else {
            this.setDone();
         }
      }
   }

   private static final class Shot {
      float x;
      float y;
      float prevX;
      float prevY;
      boolean big;
      boolean dead;
      int id;
   }

   private static final class Tgt {
      int kind;
      int hp;
      int age;
      int flash;
      int fuse = -1;
      int cool;
      int lastBig = -1;
      float x;
      float y;
      float vx;
      float vy;
      float prevX;
      float prevY;
      float w;
      float h;
      boolean dead;
      boolean grazed;
   }
}
