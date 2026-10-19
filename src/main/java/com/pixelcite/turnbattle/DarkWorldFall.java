package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class DarkWorldFall {
   private static boolean active;
   private static long startNs;
   private static long twinkleNs = -1L;
   private static long landNs = -1L;
   private static String lastDim;
   private static final float LAND = 0.9F;
   private static final float TWINKLE = 0.6F;
   private static final float GIVE_UP = 25.0F;
   private static final int LINES = 38;
   private static SoundInstance drone;
   private static int droneSilentTicks;
   private static final boolean[] squarePlayed = new boolean[6];
   private static final float[][] TWINKLES = new float[][]{{0.0F, 1.0F}, {0.1F, 1.26F}, {0.19F, 0.84F}, {0.3F, 1.5F}, {0.42F, 1.12F}};
   private static final boolean[] twinklePlayed = new boolean[TWINKLES.length];
   private static boolean revealing;
   private static final float REVEAL_START = 0.12F;
   private static final float REVEAL_SWEEP = 1.25F;
   private static final float REVEAL_HOLD = 0.45F;
   private static boolean oriMode;
   private static boolean depthsReady;
   private static int oriFloor;
   private static final float[] SQUARE_AT = new float[]{0.103F, 0.31F, 0.517F, 0.724F, 0.931F, 1.138F};
   private static final float[] SQUARE_TWIST = new float[]{12.0F, 15.0F, 18.0F, 13.0F, 16.0F, 19.0F};
   private static final float SQUARE_GROW = 1.7931033F;

   private DarkWorldFall() {
   }

   public static boolean active() {
      return active;
   }

   public static boolean oriMode() {
      return active && oriMode;
   }

   static void startDepths(int floor) {
      if (active) {
         stopDrone();
         active = false;
      }

      start();
      oriMode = true;
      depthsReady = false;
      oriFloor = floor;
   }

   static void depthsReady() {
      if (oriMode) {
         depthsReady = true;
      }
   }

   public static boolean hidingNewSkin() {
      if (!active) {
         return false;
      } else {
         return landNs >= 0L ? false : twinkleNs < 0L || !revealing || seconds(twinkleNs) < 1.37F;
      }
   }

   private static float sparkleTime(int i) {
      return revealing ? 0.12F + 1.25F * (float)i / ((float)TWINKLES.length - 1.0F) : TWINKLES[i][0];
   }

   static void start() {
      if (!active) {
         active = true;
         oriMode = false;
         startNs = System.nanoTime();
         twinkleNs = -1L;
         landNs = -1L;
         revealing = false;
         Arrays.fill(squarePlayed, false);
         Arrays.fill(twinklePlayed, false);
         droneSilentTicks = 0;
         playDrone();
      }
   }

   private static void playDrone() {
      MinecraftClient mc = MinecraftClient.getInstance();
      stopDrone();
      drone = new PositionedSoundInstance(
         TurnBattleSounds.DTRANS_DRONE.getId(),
         SoundCategory.MASTER,
         0.9F,
         1.0F,
         SoundInstance.createRandom(),
         true,
         0,
         AttenuationType.NONE,
         0.0,
         0.0,
         0.0,
         true
      );
      mc.getSoundManager().play(drone);
   }

   private static void stopDrone() {
      if (drone != null) {
         MinecraftClient.getInstance().getSoundManager().stop(drone);
      }

      drone = null;
   }

   private static void land() {
      landNs = System.nanoTime();
      stopDrone();
      TurnBattleSounds.playClient(TurnBattleSounds.DTRANS_FLIP, 1.0F, 1.0F);
   }

   private static boolean isDark(String dim) {
      return dim != null && dim.startsWith("turnbattle:dark_world_");
   }

   private static float seconds(long since) {
      return (float)(System.nanoTime() - since) / 1.0E9F;
   }

   private static float squaresDone() {
      return SQUARE_AT[SQUARE_AT.length - 1] + 1.7931033F;
   }

   static void tick(MinecraftClient mc) {
      String dim = mc.world == null ? null : mc.world.getRegistryKey().getValue().toString();
      if (dim == null) {
         if (active) {
            stopDrone();
         }

         active = false;
         oriMode = false;
         lastDim = null;
      } else {
         if (!dim.equals(lastDim)) {
            if (lastDim != null && !isDark(lastDim) && isDark(dim)) {
               start();
            }

            lastDim = dim;
         }

         if (active) {
            float t = seconds(startNs);

            for (int i = 0; i < SQUARE_AT.length && i < squarePlayed.length; i++) {
               if (!squarePlayed[i] && t >= SQUARE_AT[i]) {
                  squarePlayed[i] = true;
                  TurnBattleSounds.playClient(TurnBattleSounds.DTRANS_SQUARE, 1.0F, 1.0F);
               }
            }

            if (landNs < 0L && drone != null) {
               if (!mc.getSoundManager().isPlaying(drone)) {
                  if (++droneSilentTicks > 3) {
                     droneSilentTicks = 0;
                     playDrone();
                  }
               } else {
                  droneSilentTicks = 0;
               }
            }

            if (twinkleNs < 0L && landNs < 0L) {
               boolean arrived = (oriMode ? depthsReady : isDark(dim))
                  && !(mc.currentScreen instanceof DownloadingTerrainScreen)
                  && mc.player != null
                  && mc.world.getChunkManager().isChunkLoaded(mc.player.getBlockX() >> 4, mc.player.getBlockZ() >> 4);
               if (arrived && t >= squaresDone()) {
                  twinkleNs = System.nanoTime();
                  revealing = mc.player != null && DarkWorldSkinsClient.has(mc.player.getUuid());
               } else if (t > 25.0F || !oriMode && !isDark(dim) && t > 12.0F) {
                  land();
               }
            } else if (twinkleNs >= 0L && landNs < 0L) {
               float tw = seconds(twinkleNs);

               for (int ix = 0; ix < TWINKLES.length; ix++) {
                  if (!twinklePlayed[ix] && tw >= sparkleTime(ix)) {
                     twinklePlayed[ix] = true;
                     TurnBattleSounds.playClient(TurnBattleSounds.DTRANS_TWINKLE, 0.8F, TWINKLES[ix][1]);
                  }
               }

               if (tw >= (revealing ? 1.8199999F : 0.6F)) {
                  land();
               }
            } else if (landNs >= 0L && seconds(landNs) >= 0.9F) {
               active = false;
               oriMode = false;
            }
         }
      }
   }

   public static boolean renderLoading(DrawContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (!active && mc.world != null) {
         String dim = mc.world.getRegistryKey().getValue().toString();
         if (isDark(dim) && lastDim != null && !isDark(lastDim)) {
            start();
            lastDim = dim;
         }
      }

      if (!active) {
         return false;
      } else {
         draw(ctx, mc);
         return true;
      }
   }

   static void renderHud(DrawContext ctx) {
      if (active) {
         draw(ctx, MinecraftClient.getInstance());
      }
   }

   private static void draw(DrawContext ctx, MinecraftClient mc) {
      int sw = ctx.getScaledWindowWidth();
      int sh = ctx.getScaledWindowHeight();
      float t = seconds(startNs);
      float land = landNs < 0L ? 0.0F : clamp01(seconds(landNs) / 0.9F);
      float vis = 1.0F - smooth(clamp01((land - 0.4F) / 0.6F));
      float in = clamp01(t / 0.35F);
      float fx = vis * in;
      ctx.fill(0, 0, sw, sh, (int)(vis * 255.0F) << 24);
      if (!(fx <= 0.003F)) {
         int cx = sw / 2;
         int cy = sh / 2;
         float scene = clamp01((t - squaresDone() * 0.5F) / 0.35F);
         float lineA = fx * scene * (1.0F - clamp01(land / 0.35F));
         if (lineA > 0.003F) {
            for (int j = 0; j < 38; j++) {
               float h1 = rnd(j, 1);
               float h2 = rnd(j, 2);
               float h3 = rnd(j, 3);
               float h4 = rnd(j, 4);
               float h5 = rnd(j, 5);
               int x = (int)(h1 * (float)sw);
               float len = (0.05F + 0.17F * h2) * (float)sh;
               float speed = (1.3F + 1.5F * h3) * (float)sh;
               float cycle = (float)sh + len * 2.0F;
               float y = (float)sh + len - (t * speed + h4 * cycle) % cycle;
               int w = h5 > 0.82F ? 2 : 1;
               int a = (int)(255.0F * lineA * (0.55F + 0.45F * h5));
               ctx.fill(x, (int)y, x + w, (int)(y + len), a << 24 | 16777215);
            }
         }

         float drop = land <= 0.0F ? 0.0F : (float)Math.pow((double)clamp01(land / 0.45F), 2.0) * (float)sh * 0.9F;
         if (scene > 0.5F && drop < (float)sh * 0.85F) {
            if (revealing && twinkleNs >= 0L && landNs < 0L) {
               drawRevealingFaller(ctx, mc, (float)cx, (float)cy + drop, sw, sh, t);
            } else {
               drawFaller(ctx, mc, (float)cx, (float)cy + drop, sh, t);
            }
         }

         squares(ctx, cx, cy, sw, sh, t, fx);
         if (twinkleNs >= 0L) {
            if (revealing) {
               revealBand(ctx, cx, (float)cy + drop, sh, fx);
            } else {
               sparkles(ctx, cx, cy, sh, fx);
            }
         }

         if (oriMode && oriFloor > 0) {
            float la = fx * scene;
            int a = (int)(255.0F * la);
            if (a > 4) {
               String s = "Floor " + oriFloor;
               float sc = Math.max(1.0F, (float)sh / 150.0F);
               MatrixStack m = ctx.getMatrices();
               m.push();
               m.translate((float)cx - (float)mc.textRenderer.getWidth(s) * sc / 2.0F, (float)cy - blockPx(sh) * 2.6F, 450.0F);
               m.scale(sc, sc, 1.0F);
               ctx.drawTextWithShadow(mc.textRenderer, s, 0, 0, a << 24 | 16777215);
               m.pop();
            }
         }
      }
   }

   private static float blockPx(int sh) {
      return (float)sh * 0.3F / 1.8F;
   }

   private static float revealProgress() {
      if (twinkleNs < 0L) {
         return 0.0F;
      } else {
         float k = clamp01((seconds(twinkleNs) - 0.12F) / 1.25F);
         return k * k * (3.0F - 2.0F * k);
      }
   }

   private static float bandY(float y, int sh) {
      float b = blockPx(sh);
      float bottom = y + 1.0F * b;
      float top = y - 1.75F * b;
      return bottom + (top - bottom) * revealProgress();
   }

   private static void drawRevealingFaller(DrawContext ctx, MinecraftClient mc, float x, float y, int sw, int sh, float t) {
      int split = Math.round(bandY(y, sh));

      try {
         if (split < sh) {
            DarkWorldSkinsClient.force = 2;
            ctx.enableScissor(0, Math.max(0, split), sw, sh);

            try {
               drawFaller(ctx, mc, x, y, sh, t);
            } finally {
               ctx.disableScissor();
            }
         }

         if (split > 0) {
            DarkWorldSkinsClient.force = 1;
            ctx.enableScissor(0, 0, sw, Math.min(sh, split));

            try {
               drawFaller(ctx, mc, x, y, sh, t);
            } finally {
               ctx.disableScissor();
            }
         }
      } finally {
         DarkWorldSkinsClient.force = 0;
      }
   }

   private static void revealBand(DrawContext ctx, int cx, float y, int sh, float fx) {
      float tw = seconds(twinkleNs);
      float p = revealProgress();
      float fadeIn = clamp01(tw / 0.12F);
      float fadeOut = 1.0F - clamp01((tw - 0.12F - 1.25F) / 0.3F);
      float a0 = fx * fadeIn * fadeOut;
      if (a0 <= 0.01F) {
         finalBurst(ctx, cx, y, sh, fx, tw);
      } else {
         float b = blockPx(sh);
         int by = Math.round(bandY(y, sh));
         int half = Math.round(b * 0.62F);
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(0.0F, 0.0F, 400.0F);

         for (int g = 0; g < 4; g++) {
            int ga = (int)(a0 * (float)(70 - g * 16));
            if (ga > 0) {
               ctx.fill(cx - half - g * 2, by - g, cx + half + g * 2, by + g + 1, ga << 24 | 16777215);
            }
         }

         int n = 9;

         for (int i = 0; i < n; i++) {
            float fxp = (float)i / ((float)n - 1.0F) * 2.0F - 1.0F;
            int x = cx + Math.round(fxp * (float)half + (rnd(i, 21) - 0.5F) * b * 0.12F);
            float k = 0.5F + 0.5F * (float)Math.sin((double)(tw * (9.0F + 5.0F * rnd(i, 22)) + rnd(i, 23) * 6.28F));
            int arm = Math.max(1, Math.round(b * (0.06F + 0.12F * k)));
            int a = (int)(255.0F * a0 * (0.45F + 0.55F * k));
            twinkle(ctx, x, by + Math.round((rnd(i, 24) - 0.5F) * b * 0.1F), arm, a);
         }

         for (int i = 0; i < 7; i++) {
            float life = (tw * 1.6F + rnd(i, 31)) % 1.0F;
            int x = cx + Math.round((rnd(i, 32) * 2.0F - 1.0F) * (float)half);
            int yy = by - Math.round(life * b * 0.8F);
            int arm = Math.max(1, Math.round(b * 0.07F * (1.0F - life)));
            twinkle(ctx, x, yy, arm, (int)(200.0F * a0 * (1.0F - life)));
         }

         m.pop();
         if (p >= 1.0F) {
            finalBurst(ctx, cx, y, sh, fx, tw);
         }
      }
   }

   private static void finalBurst(DrawContext ctx, int cx, float y, int sh, float fx, float tw) {
      float age = (tw - 0.12F - 1.25F) / 0.4F;
      if (!(age < 0.0F) && !(age > 1.0F)) {
         float b = blockPx(sh);
         float k = (float)Math.sin(Math.PI * (double)age);
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(0.0F, 0.0F, 400.0F);

         for (int i = 0; i < 6; i++) {
            float ang = (float)((double)i / 6.0 * Math.PI * 2.0 + (double)rnd(i, 41));
            float r = b * (0.7F + 0.5F * age);
            int x = cx + Math.round((float)Math.cos((double)ang) * r);
            int yy = Math.round(y - b * 0.3F + (float)Math.sin((double)ang) * r * 1.3F);
            twinkle(ctx, x, yy, Math.max(1, Math.round(k * b * 0.16F)), (int)(255.0F * fx * k));
         }

         m.pop();
      }
   }

   private static void twinkle(DrawContext ctx, int x, int y, int arm, int a) {
      if (a > 2) {
         int col = Math.min(255, a) << 24 | 16777215;
         ctx.fill(x - arm, y, x + arm + 1, y + 1, col);
         ctx.fill(x, y - arm, x + 1, y + arm + 1, col);
         if (arm > 2) {
            ctx.fill(x - 1, y - 1, x + 2, y + 2, col);
         }
      }
   }

   private static void sparkles(DrawContext ctx, int cx, int cy, int sh, float fx) {
      float tw = seconds(twinkleNs);

      for (int i = 0; i < TWINKLES.length; i++) {
         float age = (tw - TWINKLES[i][0]) / 0.4F;
         if (!(age < 0.0F) && !(age > 1.0F)) {
            float k = (float)Math.sin(Math.PI * (double)age);
            int x = cx + (int)((rnd(i, 11) - 0.5F) * (float)sh * 0.34F);
            int y = cy + (int)((rnd(i, 12) - 0.5F) * (float)sh * 0.36F);
            int arm = Math.max(1, Math.round(k * (float)sh * (0.025F + 0.02F * rnd(i, 13))));
            int a = (int)(255.0F * fx * Math.min(1.0F, k * 1.4F));
            if (a > 2) {
               int col = a << 24 | 16777215;
               ctx.getMatrices().push();
               ctx.getMatrices().translate(0.0F, 0.0F, 400.0F);
               ctx.fill(x - arm, y, x + arm + 1, y + 1, col);
               ctx.fill(x, y - arm, x + 1, y + arm + 1, col);
               if (arm > 2) {
                  ctx.fill(x - 1, y - 1, x + 2, y + 2, col);
               }

               ctx.getMatrices().pop();
            }
         }
      }
   }

   private static void squares(DrawContext ctx, int cx, int cy, int sw, int sh, float t, float fx) {
      float full = (float)Math.hypot((double)sw, (double)sh) * 0.5F;
      MatrixStack m = ctx.getMatrices();

      for (int i = 0; i < SQUARE_AT.length; i++) {
         float age = t - SQUARE_AT[i];
         if (!(age <= 0.0F)) {
            float k = age / 1.7931033F;
            float half = full * 2.2F * (float)Math.pow((double)k, 1.6);
            float gone = clamp01((half / full - 1.0F) / 1.2F);
            if (!(gone >= 1.0F) && !(half < 0.5F)) {
               float a = fx * (1.0F - gone);
               int ai = (int)(a * 255.0F);
               if (ai > 2) {
                  float ang = SQUARE_TWIST[i] * (1.0F - (float)Math.exp((double)(-k * 2.6F)));
                  int h = Math.max(1, Math.round(half));
                  int th = Math.max(1, Math.round((float)sh / 240.0F));
                  m.push();
                  m.translate((float)cx, (float)cy, 300.0F + (float)i * 10.0F);
                  m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ang));
                  ctx.fill(-h, -h, h, h, ai << 24);
                  int col = ai << 24 | 16777215;
                  ctx.fill(-h, -h, h, -h + th, col);
                  ctx.fill(-h, h - th, h, h, col);
                  ctx.fill(-h, -h + th, -h + th, h - th, col);
                  ctx.fill(h - th, -h + th, h, h - th, col);
                  m.pop();
               }
            }
         }
      }
   }

   private static void drawFaller(DrawContext ctx, MinecraftClient mc, float x, float y, int sh, float t) {
      if (mc.world != null) {
         UUID me = mc.player != null ? mc.player.getUuid() : mc.getGameProfile().getId();
         String name = mc.player != null ? mc.player.getName().getString() : mc.getGameProfile().getName();

         Object e;
         try {
            e = (AbstractClientPlayerEntity)(oriMode
               ? BattleEntityRenderer.recruitFake(Recruit.ORI.displayName)
               : MirrorCloneRenderer.MirrorFakePlayer.get("fall", me, name));
         } catch (Throwable var24) {
            return;
         }

         if (e != null) {
            e.bodyYaw = e.prevBodyYaw = 180.0F;
            e.headYaw = e.prevHeadYaw = 180.0F;
            e.setYaw(180.0F);
            e.prevYaw = 180.0F;
            e.setPitch(0.0F);
            e.prevPitch = 0.0F;
            e.handSwingProgress = e.lastHandSwingProgress = 0.0F;
            e.age = (int)(t * 20.0F);
            CloneAnimator.Pose p = new CloneAnimator.Pose();
            p.armW = 1.0F;
            p.headW = 1.0F;
            p.legW = 1.0F;
            float flap = 0.07F * (float)Math.sin((double)(t * 15.0F));
            float flap2 = 0.07F * (float)Math.sin((double)(t * 13.0F + 1.3F));
            p.rArmP = -2.85F + flap;
            p.rArmR = 0.32F + flap2;
            p.lArmP = -2.85F + flap2;
            p.lArmR = -0.32F - flap;
            p.rLegP = 0.28F + 0.06F * (float)Math.sin((double)(t * 6.0F));
            p.lLegP = -0.2F + 0.06F * (float)Math.sin((double)(t * 6.0F + 2.0F));
            p.headP = 0.3F;
            float sway = (float)Math.sin((double)(t * 1.9F));
            float size = (float)sh * 0.3F / 1.8F;
            float bob = (float)Math.sin((double)(t * 2.6F)) * (float)sh * 0.008F;
            Quaternionf q = new Quaternionf()
               .rotateZ((float) Math.PI)
               .rotateY((float)Math.toRadians((double)(sway * 7.0F)))
               .rotateX((float)Math.toRadians((double)(-3.0F * sway)));
            Vector3f offset = new Vector3f(0.0F, 0.9F, 0.0F);
            CloneAnimator.beginModelPose(e, p);

            try {
               InventoryScreen.drawEntity(ctx, x, y + bob, size, offset, q, null, e);
            } catch (Exception var22) {
            } finally {
               CloneAnimator.endModelPose();
               ctx.draw();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   private static float rnd(int i, int salt) {
      long h = (long)i * -7046029254386353131L ^ (long)salt * -4417276706812531889L;
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float smooth(float x) {
      return x * x * (3.0F - 2.0F * x);
   }
}
