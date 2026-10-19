package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class MagicantScreen extends Screen {
   static final String[] LINES = new String[]{
      "WELCOME BACK.",
      "DESPITE THE DEVELOPMENT OF EACH SANCTUARY",
      "AN ONGOING EFFORT",
      "YOU ARRIVE HERE, REGARDLESS.",
      "YOU HAVE CREATED A REALM",
      "OF NOTHING",
      "'MAGICANT'",
      "THE REALM OF YOUR MIND.",
      "IN MAGICANT, THERE'S NOTHING",
      "YOU HAVE NO MEMORIES OF YOUR OWN TO FORM.",
      "ALL THERE IS...",
      "IS THE SIDE OF YOU, YOU'D RATHER NOT SEE",
      "THE 'SEA OF EDEN'",
      "THE ENDLESS, COLDWATER BENEATH YOUR FEET",
      "IT TAKES YOU TO A TRUTH YOU CANNOT EARN"
   };
   private static final float[][] LINE_SHOTS = new float[][]{
      {0.0F, 4.0F, 3.2F, 1.45F},
      {40.0F, 18.0F, 6.5F, 1.0F},
      {-70.0F, 2.0F, 3.0F, 1.5F},
      {180.0F, 8.0F, 4.5F, 1.2F},
      {120.0F, 35.0F, 9.0F, 0.9F},
      {125.0F, 62.0F, 13.0F, 0.6F},
      {0.0F, -12.0F, 2.8F, 1.3F},
      {30.0F, 0.0F, 1.6F, 1.62F},
      {-140.0F, 12.0F, 7.0F, 1.0F},
      {-100.0F, 5.0F, 2.4F, 1.55F},
      {200.0F, 25.0F, 5.0F, 1.1F},
      {-20.0F, 3.0F, 1.8F, 1.6F},
      {90.0F, -4.0F, 5.0F, 0.35F},
      {60.0F, 40.0F, 7.0F, 0.2F},
      {0.0F, 6.0F, 3.6F, 1.3F}
   };
   static final int FADE_TO_BLACK = 20;
   static final int VOID_IN = 50;
   static final int LINES_START = 64;
   static final float TYPE_RATE = 0.9F;
   static final int HOLD = 46;
   static final int LINE_FADE = 10;
   static final int GAP = 8;
   static final int[] LINE_AT = new int[LINES.length];
   static final int CLASH_AT;
   static final int C_APPEAR = 30;
   static final int C_RAISE = 40;
   static final int C_TURN = 66;
   static final int C_SWING = 70;
   static final int C_HIT = 74;
   static final int C_PUSH = 96;
   static final int C_DONE = 112;
   static final int C_END = 130;
   private static final float[][] TURNS;
   static int t;
   static int voidAt;
   static boolean doneSent;
   static float shake;
   static float prevShake;
   static int flashAt;
   static float cYaw;
   static float cPitch;
   static float cDist;
   static float cTx;
   static float cTy;
   static float cTz;
   static float pYaw;
   static float pPitch;
   static float pDist;
   static float pTx;
   static float pTy;
   static float pTz;
   static long lastTickNanos;
   static boolean skip;
   private static final Identifier TEX_GLOW;
   private static final Identifier TEX_RING;
   private static final int TINT = 10138879;
   private static final float[] GUARD_R;
   private static final float[] GUARD_L;
   private static final float[] WINDUP_R;
   private static final float[] WINDUP_L;
   private static final float[] CUT_R;
   private static final float[] CUT_L;
   private static final float[] THRUST_R;
   private static final float[] THRUST_L;
   private static final float STAGE_PX = 29.6F;

   public MagicantScreen() {
      super(Text.literal("MAGICANT"));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return true;
   }

   private static float lineAt(int i) {
      int at = 64;

      for (int k = 0; k < i; k++) {
         at += (int)Math.ceil((double)((float)LINES[k].length() / 0.9F)) + 46 + 10 + 8;
      }

      return (float)at;
   }

   static void reset() {
      t = 0;
      voidAt = -1;
      doneSent = false;
      prevShake = 0.0F;
      shake = 0.0F;
      flashAt = -1000;
      pYaw = 0.0F;
      cYaw = 0.0F;
      pPitch = 10.0F;
      cPitch = 10.0F;
      pDist = 6.0F;
      cDist = 6.0F;
      pTx = 0.0F;
      cTx = 0.0F;
      pTy = 1.0F;
      cTy = 1.0F;
      pTz = 0.0F;
      cTz = 0.0F;
      lastTickNanos = System.nanoTime();
   }

   static int v() {
      return voidAt < 0 ? -1 : t - voidAt;
   }

   static void tick(MinecraftClient mc) {
      lastTickNanos = System.nanoTime();
      t++;
      if (skip) {
         if (t > 600 && mc.currentScreen instanceof MagicantScreen) {
            mc.setScreen(null);
            MagicantClient.release();
         }
      } else {
         if (voidAt < 0) {
            boolean there = mc.world != null && mc.world.getRegistryKey().getValue().equals(MagicantManager.MAGICANT_WORLD.getValue());
            if (there && t >= 32 || t >= 180) {
               voidAt = t + 6;
            }
         }

         int v = v();

         for (int i = 0; i < LINES.length; i++) {
            int typed0 = (int)((float)(v - 1 - LINE_AT[i]) * 0.9F);
            int typed1 = (int)((float)(v - LINE_AT[i]) * 0.9F);
            if (typed1 > 0 && typed0 < LINES[i].length()) {
               for (int c = Math.max(0, typed0); c < Math.min(typed1, LINES[i].length()); c++) {
                  char ch = LINES[i].charAt(c);
                  if (c % 2 == 0 && ch != ' ' && ch != '.' && ch != '\'') {
                     TurnBattleSounds.playClient(TurnBattleSounds.SND_TEXT, 0.6F, 0.9F);
                     break;
                  }
               }
            }
         }

         int cx = v - CLASH_AT;
         if (cx == 70) {
            TurnBattleSounds.playClient(TurnBattleSounds.SLASH, 1.0F, 1.0F);
         }

         if (cx == 74) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_GRAB, 1.0F, 1.0F);
            shake = 1.0F;
            flashAt = t;
         }

         if (cx == 96) {
            TurnBattleSounds.playClient(TurnBattleSounds.DAMAGE, 1.0F, 1.0F);
            shake = 1.2F;
            flashAt = t;
         }

         if (cx > 74 && cx < 96) {
            shake = Math.max(shake, 0.25F);
         }

         if (cx >= 112 && !doneSent) {
            doneSent = true;
            MagicantClient.sendClashDone();
         }

         prevShake = shake;
         shake = Math.max(0.0F, shake * 0.86F - 0.01F);
         tickCamera(v);
      }
   }

   private static float[] shot(int v) {
      int c = v - CLASH_AT;
      if (c >= 66) {
         float back = c >= 96 ? 1.2F : 0.0F;
         return new float[]{90.0F, 6.0F, 4.4F + back, 0.0F, 1.2F, -1.0F, 0.18F, c == 66 ? 1.0F : 0.0F};
      } else if (c >= 0) {
         float push = clamp01((float)(c - 40) / 26.0F);
         return new float[]{20.0F, 5.0F, mix(4.4F, 3.5F, push), 0.0F, 1.32F, -1.2F, 0.06F, 0.0F};
      } else {
         int line = -1;

         for (int i = 0; i < LINES.length; i++) {
            if (v >= LINE_AT[i] - 14) {
               line = i;
            }
         }

         if (line < 0) {
            float k = clamp01((float)v / 120.0F);
            return new float[]{(float)Math.sin((double)((float)v * 0.01F)) * 8.0F, mix(14.0F, 6.0F, k), mix(8.5F, 4.2F, k), 0.0F, 1.1F, 0.0F, 0.05F, 0.0F};
         } else {
            float[] s = LINE_SHOTS[line];
            float drift = (float)Math.sin((double)((float)(v - LINE_AT[line]) * 0.012F)) * 6.0F;
            return new float[]{s[0] + drift, s[1], s[2], 0.0F, s[3], 0.0F, 0.05F, 0.0F};
         }
      }
   }

   private static void tickCamera(int v) {
      pYaw = cYaw;
      pPitch = cPitch;
      pDist = cDist;
      pTx = cTx;
      pTy = cTy;
      pTz = cTz;
      if (v >= 0) {
         float[] s = shot(v);
         if (s[7] > 0.5F) {
            cYaw = s[0];
            cPitch = s[1];
            cDist = s[2];
            cTx = s[3];
            cTy = s[4];
            cTz = s[5];
            pYaw = cYaw;
            pPitch = cPitch;
            pDist = cDist;
            pTx = cTx;
            pTy = cTy;
            pTz = cTz;
         } else {
            float k = s[6];
            cYaw = cYaw + wrapDeg(s[0] - cYaw) * k;
            cPitch = cPitch + (s[1] - cPitch) * k;
            cDist = cDist + (s[2] - cDist) * k;
            cTx = cTx + (s[3] - cTx) * k;
            cTy = cTy + (s[4] - cTy) * k;
            cTz = cTz + (s[5] - cTz) * k;
         }
      }
   }

   private float partial() {
      return clamp01((float)(System.nanoTime() - lastTickNanos) / 5.0E7F);
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      float td = this.partial();
      int v = v();
      if (v >= 0 && !skip) {
         float vf = (float)v + td;
         MagicantScreen.Cam cam = new MagicantScreen.Cam(
            lerpAngle(pYaw, cYaw, td),
            mix(pPitch, cPitch, td),
            mix(pDist, cDist, td),
            mix(pTx, cTx, td),
            mix(pTy, cTy, td),
            mix(pTz, cTz, td),
            (float)this.height * 0.95F,
            0.5F,
            0.52F
         );
         float sh = mix(prevShake, shake, td);
         MatrixStack m = ctx.getMatrices();
         m.push();
         if (sh > 0.01F) {
            m.translate((float)Math.sin((double)(vf * 2.7F)) * sh * 4.0F, (float)Math.cos((double)(vf * 3.3F)) * sh * 3.0F, 0.0F);
         }

         float[] feet = cam.project(0.0F, 0.0F, 0.0F, this.width, this.height);
         this.renderVoid(ctx, cam, feet, vf);
         MagicantScreen.Figure you = this.you(vf);
         MagicantScreen.Figure it = this.it(vf);
         List figs = new ArrayList<>();
         figs.add(you);
         if (it != null) {
            figs.add(it);
         }

         figs.sort((a, b) -> Float.compare(depthOf(cam, b), depthOf(cam, a)));
         ctx.draw();
         RenderSystem.clear(256, false);

         for (MagicantScreen.Figure f : figs) {
            this.drawFigure(ctx, cam, f, vf);
            ctx.draw();
            RenderSystem.clear(256, false);
         }

         m.pop();
         this.vignette(ctx);
         this.renderLine(ctx, vf);
         float fl = 1.0F - clamp01(((float)t + td - (float)flashAt) / 8.0F);
         if (fl > 0.0F) {
            ctx.fill(0, 0, this.width, this.height, argb(16777215, 0.75F * fl));
         }

         int c = v - CLASH_AT;
         float black = Math.max(1.0F - clamp01(vf / 50.0F), clamp01((vf - (float)CLASH_AT - 112.0F) / 18.0F));
         if (black > 0.0F) {
            ctx.fill(0, 0, this.width, this.height, argb(0, black));
         }
      } else {
         float a = clamp01(((float)t + td) / 20.0F);
         ctx.fill(0, 0, this.width, this.height, argb(0, a));
      }
   }

   private static float depthOf(MagicantScreen.Cam cam, MagicantScreen.Figure f) {
      float[] p = cam.project(f.x, 0.9F, f.z, 1, 1);
      return p == null ? -1.0F : p[3];
   }

   private void renderVoid(DrawContext ctx, MagicantScreen.Cam cam, float[] feet, float t) {
      ctx.fillGradient(0, 0, this.width, this.height, -16645624, -16381416);
      float[] far = this.horizonPoint(cam);
      int hy = far != null ? Math.round(far[1]) : -1;
      if (hy < this.height) {
         int top = Math.max(0, hy);
         ctx.fillGradient(0, top, this.width, this.height, 1711938106, -872151014);
         if (hy > 0) {
            ctx.fillGradient(0, Math.max(0, hy - 2), this.width, hy + 2, 0, 1077566106);
         }
      }

      if (feet != null) {
         float fx = feet[0];
         float fy = feet[1];
         float size = feet[2];
         int shaftW = (int)(size * 1.6F);

         for (int i = 0; i < 6; i++) {
            float w = (float)shaftW * (1.0F - (float)i * 0.14F);
            ctx.fillGradient((int)(fx - w / 2.0F), 0, (int)(fx + w / 2.0F), (int)fy, argb(10138879, 0.0F), argb(10138879, 0.02F));
         }

         float flat = 0.12F + 0.6F * (float)Math.sin(Math.toRadians((double)Math.max(3.0F, 10.0F + cam.pitch())));
         float pool = size * 2.6F;
         this.tex(ctx, TEX_GLOW, fx, fy, pool, pool * flat, 10138879, 0.45F);
         this.tex(ctx, TEX_GLOW, fx, fy, pool * 0.4F, pool * 0.4F * flat, 16777215, 0.3F);

         for (int r = 0; r < 4; r++) {
            float q = (t * 0.005F + (float)r / 4.0F) % 1.0F;
            float rs = size * (0.6F + q * 4.5F);
            this.tex(ctx, TEX_RING, fx, fy, rs, rs * flat, 6982911, 0.4F * (1.0F - q));
         }

         for (int i = 0; i < 90; i++) {
            float ox = (CloneAnimator.rnd(i * 3 + 1) - 0.5F) * 14.0F;
            float oz = (CloneAnimator.rnd(i * 3 + 2) - 0.5F) * 14.0F;
            boolean glint = i % 3 == 0;
            float oy = glint ? 0.02F : (CloneAnimator.rnd(i * 3 + 3) + t * (0.003F + CloneAnimator.rnd(i * 7) * 0.004F)) % 1.0F * 4.5F;
            float[] p = cam.project(ox, oy, oz, this.width, this.height);
            if (p != null) {
               float life = glint ? 0.5F + 0.5F * (float)Math.sin((double)(t * 0.08F + (float)i)) : (float)Math.sin(Math.PI * (double)clamp01(oy / 4.5F));
               float a = (glint ? 0.35F : 0.45F) * life;
               int px = Math.max(1, Math.round(p[2] / 40.0F));
               ctx.fill((int)p[0], (int)p[1], (int)p[0] + px * (glint ? 2 : 1), (int)p[1] + px, argb(i % 5 == 0 ? 16777215 : 10138879, a));
            }
         }
      }
   }

   private float[] horizonPoint(MagicantScreen.Cam cam) {
      float[] f = cam.fwd();
      float[] c = cam.pos();
      float len = (float)Math.hypot((double)f[0], (double)f[2]);
      return len < 0.001F ? null : cam.project(c[0] + f[0] / len * 400.0F, 0.0F, c[2] + f[2] / len * 400.0F, this.width, this.height);
   }

   private void tex(DrawContext ctx, Identifier id, float cx, float cy, float w, float h, int rgb, float a) {
      if (!(a <= 0.003F) && !(w < 1.0F) && !(h < 1.0F)) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F, clamp01(a));
         ctx.drawTexture(id, Math.round(cx - w / 2.0F), Math.round(cy - h / 2.0F), Math.round(w), Math.round(h), 0.0F, 0.0F, 64, 64, 64, 64);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void vignette(DrawContext ctx) {
      int e = Math.max(24, this.height / 4);
      ctx.fillGradient(0, 0, this.width, e, -805306368, 0);
      ctx.fillGradient(0, this.height - e, this.width, this.height, 0, -805306368);
   }

   private void renderLine(DrawContext ctx, float v) {
      for (int i = 0; i < LINES.length; i++) {
         float local = v - (float)LINE_AT[i];
         String line = LINES[i];
         int typeTicks = (int)Math.ceil((double)((float)line.length() / 0.9F));
         if (!(local < 0.0F) && !(local > (float)(typeTicks + 46 + 10))) {
            int shown = Math.min(line.length(), (int)(local * 0.9F));
            if (shown > 0) {
               float a = 1.0F - clamp01((local - (float)typeTicks - 46.0F) / 10.0F);
               String s = line.substring(0, shown);
               float scale = this.width > 520 ? 2.0F : 1.5F;
               int full = this.textRenderer.getWidth(line);
               float x = (float)this.width / 2.0F - (float)full * scale / 2.0F;
               float y = (float)this.height * 0.8F;
               MatrixStack m = ctx.getMatrices();
               m.push();
               m.translate(x, y, 400.0F);
               m.scale(scale, scale, 1.0F);
               int col = argb(16777215, Math.max(0.02F, a));
               ctx.drawText(this.textRenderer, s, 0, 0, col, true);
               m.pop();
            }
         }
      }
   }

   private static float facingAt(float v) {
      float prev = TURNS[0][1];

      for (int i = 1; i < TURNS.length; i++) {
         float at = TURNS[i][0];
         if (v < at) {
            break;
         }

         float k = smooth(clamp01((v - at) / 14.0F));
         prev = mix(prev, TURNS[i][1], k);
         if (k < 1.0F) {
            return prev;
         }

         prev = TURNS[i][1];
      }

      return prev;
   }

   private MagicantScreen.Figure you(float v) {
      CloneAnimator.Pose p = new CloneAnimator.Pose();
      p.armW = 1.0F;
      p.headW = 1.0F;
      float c = v - (float)CLASH_AT;
      float facing = facingAt(Math.min(v, (float)CLASH_AT));
      float z = 0.0F;
      float breath = (float)Math.sin((double)(v * 0.21F));
      p.rArmP = -0.22F + 0.02F * breath;
      p.rArmR = 0.18F;
      p.lArmP = -0.38F + 0.03F * breath;
      p.lArmR = -0.38F;
      p.lArmY = -0.1F;
      p.sy = 1.0F + 0.012F * breath;
      p.headY = 0.55F * (float)Math.sin((double)(v * 0.11F)) * (float)Math.sin((double)(v * 0.037F + 1.3F));
      p.headP = 0.06F * (float)Math.sin((double)(v * 0.07F));
      float flinch = 0.0F;

      for (int i = 1; i < TURNS.length; i++) {
         flinch = Math.max(flinch, bell(clamp01((v - TURNS[i][0]) / 18.0F)));
      }

      p.lArmP -= 0.6F * flinch;
      p.rArmP -= 0.25F * flinch;
      p.tilt = -3.0F * flinch;
      if (v < 64.0F) {
         float look = clamp01((v - 36.0F) / 10.0F);
         p.headP = mix(0.55F, -0.1F, smooth(look));
         p.headY = 0.5F * bell(clamp01((v - 44.0F) / 22.0F));
      }

      if (c >= 66.0F) {
         float spin = smooth(clamp01((c - 66.0F) / 7.0F));
         facing = facingAt((float)CLASH_AT) + 180.0F * spin;
         float guard = smooth(clamp01((c - 66.0F) / 6.0F));
         p.headY *= 1.0F - guard;
         p.headP = mix(p.headP, -0.1F, guard);
         arms(p, GUARD_R, GUARD_L, guard);
         p.tilt = mix(p.tilt, -6.0F, guard);
         if (c >= 74.0F && c < 96.0F) {
            float st = (float)Math.sin((double)(c * 1.7F));
            p.rArmP += 0.04F * st;
            p.lArmP += 0.04F * st;
            p.tilt = -7.0F + 1.5F * st;
            z = 0.12F * clamp01((c - 74.0F) / 20.0F);
         }

         if (c >= 96.0F) {
            float shove = bell(clamp01((c - 96.0F) / 14.0F));
            arms(p, THRUST_R, THRUST_L, shove);
            p.tilt = mix(-6.0F, 9.0F, shove);
            z = mix(0.12F, -0.3F, shove);
         }
      }

      return new MagicantScreen.Figure(true, 0.0F, z, facing, p, 0.0F);
   }

   private MagicantScreen.Figure it(float v) {
      float c = v - (float)CLASH_AT;
      if (c < 30.0F) {
         return null;
      } else {
         CloneAnimator.Pose p = new CloneAnimator.Pose();
         p.armW = 1.0F;
         p.headW = 1.0F;
         float dark = 1.0F - clamp01((c - 30.0F) / 10.0F);
         float facing = facingAt((float)CLASH_AT);
         float z = -2.4F;
         float raise = smooth(clamp01((c - 40.0F) / 22.0F));
         p.rArmP = -0.1F;
         p.lArmP = -0.1F;
         arms(p, WINDUP_R, WINDUP_L, raise);
         p.headP = -0.05F * raise;
         p.tilt = -4.0F * raise;
         if (c >= 70.0F) {
            float sw = smooth(clamp01((c - 70.0F) / 4.0F));
            arms(p, CUT_R, CUT_L, sw);
            p.tilt = mix(p.tilt, 12.0F, sw);
            z = mix(-2.4F, -1.35F, sw);
         }

         if (c >= 74.0F && c < 96.0F) {
            float st = (float)Math.sin((double)(c * 1.7F + 1.0F));
            p.rArmP += 0.04F * st;
            p.lArmP += 0.04F * st;
            p.tilt = 11.0F + 1.5F * st;
            z = -1.35F + 0.12F * clamp01((c - 74.0F) / 20.0F);
         }

         if (c >= 96.0F) {
            float knock = easeOut(clamp01((c - 96.0F) / 10.0F));
            float recover = smooth(clamp01((c - 96.0F - 12.0F) / 16.0F));
            z = mix(-1.23F, -3.6F, knock);
            p.tilt = mix(mix(12.0F, -18.0F, knock), -2.0F, recover);
            p.rArmP = mix(mix(CUT_R[0], -0.5F, knock), -0.9F, recover);
            p.rArmY = mix(CUT_R[1], 0.0F, knock);
            p.lArmP = mix(mix(CUT_L[0], -0.2F, knock), -0.6F, recover);
            p.lArmY = mix(CUT_L[1], 0.0F, knock);
            p.lArmR = -0.5F * knock * (1.0F - recover);
            p.rArmR = 0.4F * knock * (1.0F - recover);
            p.headP = mix(-0.3F * knock, 0.1F, recover);
         }

         return new MagicantScreen.Figure(false, 0.0F, z, facing, p, dark);
      }
   }

   private static void arms(CloneAnimator.Pose p, float[] r, float[] l, float k) {
      p.rArmP = mix(p.rArmP, r[0], k);
      p.rArmY = mix(p.rArmY, r[1], k);
      p.rArmR = mix(p.rArmR, r[2], k);
      p.lArmP = mix(p.lArmP, l[0], k);
      p.lArmY = mix(p.lArmY, l[1], k);
      p.lArmR = mix(p.lArmR, l[2], k);
   }

   private void drawFigure(DrawContext ctx, MagicantScreen.Cam cam, MagicantScreen.Figure f, float v) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         float fr = rad(f.facing);
         float[] p = cam.project(f.x + 0.0F, 0.0F, f.z, this.width, this.height);
         if (p != null) {
            UUID me = mc.player.getUuid();
            String name = mc.player.getName().getString();
            MirrorCloneRenderer.MirrorFakePlayer e = MirrorCloneRenderer.MirrorFakePlayer.get(f.isYou ? "cutscene:you" : "cutscene:it", me, name);
            if (e != null) {
               ItemStack weapon = TurnBattleClientMod.clientEquippedWeapon != null && !TurnBattleClientMod.clientEquippedWeapon.isEmpty()
                  ? TurnBattleClientMod.clientEquippedWeapon
                  : new ItemStack(Items.IRON_SWORD);
               if (!ItemStack.areEqual(e.getMainHandStack(), weapon)) {
                  e.equipStack(EquipmentSlot.MAINHAND, weapon.copy());
               }

               e.bodyYaw = e.prevBodyYaw = 180.0F;
               e.headYaw = e.prevHeadYaw = 180.0F;
               e.setYaw(180.0F);
               e.prevYaw = 180.0F;
               e.setPitch(0.0F);
               e.prevPitch = 0.0F;
               e.handSwingProgress = e.lastHandSwingProgress = 0.0F;
               e.age = t;
               CloneAnimator.Pose pose = f.pose;
               float rel = f.facing - cam.yaw();
               Quaternionf q = new Quaternionf().rotateZ((float) Math.PI).rotateX(rad(-cam.pitch())).rotateY(rad(rel)).rotateX(rad(-pose.tilt));
               Vector3f offset = new Vector3f(0.0F, -pose.dy / 29.6F, -pose.dx / 29.6F);
               q.transform(offset);
               float k = 1.0F - 0.92F * f.dark;
               RenderSystem.setShaderColor(k, k, Math.min(1.0F, k * (1.0F + 0.15F * f.dark)), 1.0F);
               MatrixStack m = ctx.getMatrices();
               m.push();
               m.translate(p[0], p[1], 0.0F);
               m.scale(pose.sx, pose.sy, 1.0F);
               m.translate(-p[0], -p[1], 0.0F);
               CloneAnimator.beginModelPose(e, pose);

               try {
                  InventoryScreen.drawEntity(ctx, p[0], p[1], p[2], offset, q, null, e);
               } catch (Exception var22) {
               } finally {
                  CloneAnimator.endModelPose();
                  m.pop();
                  ctx.draw();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               }
            }
         }
      }
   }

   private static int argb(int rgb, float a) {
      return (int)(clamp01(a) * 255.0F) << 24 | rgb & 16777215;
   }

   private static float clamp01(float x) {
      return x < 0.0F ? 0.0F : (x > 1.0F ? 1.0F : x);
   }

   private static float mix(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static float smooth(float x) {
      return x * x * (3.0F - 2.0F * x);
   }

   private static float bell(float q) {
      return !(q <= 0.0F) && !(q >= 1.0F) ? (float)Math.sin(Math.PI * (double)q) : 0.0F;
   }

   private static float easeOut(float x) {
      float u = 1.0F - x;
      return 1.0F - u * u * u;
   }

   private static float rad(float deg) {
      return (float)Math.toRadians((double)deg);
   }

   private static float wrapDeg(float d) {
      d %= 360.0F;
      if (d > 180.0F) {
         d -= 360.0F;
      }

      if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   private static float lerpAngle(float a, float b, float t) {
      return a + wrapDeg(b - a) * t;
   }

   static {
      int at = 64;

      for (int i = 0; i < LINES.length; i++) {
         LINE_AT[i] = at;
         at += (int)Math.ceil((double)((float)LINES[i].length() / 0.9F)) + 46 + 10 + 8;
      }

      CLASH_AT = at + 6;
      TURNS = new float[][]{
         {0.0F, 0.0F},
         {lineAt(1) + 20.0F, 90.0F},
         {lineAt(2) + 30.0F, 40.0F},
         {lineAt(3) + 10.0F, 180.0F},
         {lineAt(4) + 25.0F, 110.0F},
         {lineAt(6) + 5.0F, 0.0F},
         {lineAt(8) + 15.0F, -95.0F},
         {lineAt(9) + 30.0F, -20.0F},
         {lineAt(10) + 15.0F, 160.0F},
         {lineAt(12) + 10.0F, 70.0F},
         {lineAt(13) + 20.0F, 200.0F},
         {lineAt(14) + 5.0F, 360.0F}
      };
      voidAt = -1;
      flashAt = -1000;
      TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
      TEX_RING = Identifier.of("turnbattle", "textures/effects/ritual_ring.png");
      GUARD_R = new float[]{-2.0F, 0.25F, 1.5F};
      GUARD_L = new float[]{-1.92F, 0.77F, 0.06F};
      WINDUP_R = new float[]{-2.8F, -0.86F, 0.48F};
      WINDUP_L = new float[]{-2.13F, 0.94F, 0.02F};
      CUT_R = new float[]{-1.2F, -0.74F, -0.16F};
      CUT_L = new float[]{-1.02F, 1.02F, -0.02F};
      THRUST_R = CUT_R;
      THRUST_L = CUT_L;
   }

   private static record Cam(float yaw, float pitch, float dist, float tx, float ty, float tz, float focal, float ax, float ay) {
      float[] right() {
         float y = MagicantScreen.rad(this.yaw);
         return new float[]{(float)Math.cos((double)y), 0.0F, -((float)Math.sin((double)y))};
      }

      float[] up() {
         float y = MagicantScreen.rad(this.yaw);
         float p = MagicantScreen.rad(this.pitch);
         return new float[]{
            -((float)(Math.sin((double)y) * Math.sin((double)p))), (float)Math.cos((double)p), -((float)(Math.cos((double)y) * Math.sin((double)p)))
         };
      }

      float[] fwd() {
         float y = MagicantScreen.rad(this.yaw);
         float p = MagicantScreen.rad(this.pitch);
         return new float[]{
            -((float)(Math.sin((double)y) * Math.cos((double)p))), -((float)Math.sin((double)p)), -((float)(Math.cos((double)y) * Math.cos((double)p)))
         };
      }

      float[] pos() {
         float y = MagicantScreen.rad(this.yaw);
         float p = MagicantScreen.rad(this.pitch);
         return new float[]{
            this.tx + this.dist * (float)(Math.sin((double)y) * Math.cos((double)p)),
            this.ty + this.dist * (float)Math.sin((double)p),
            this.tz + this.dist * (float)(Math.cos((double)y) * Math.cos((double)p))
         };
      }

      float[] project(float x, float y, float z, int w, int h) {
         float[] c = this.pos();
         float[] r = this.right();
         float[] u = this.up();
         float[] f = this.fwd();
         float vx = x - c[0];
         float vy = y - c[1];
         float vz = z - c[2];
         float depth = vx * f[0] + vy * f[1] + vz * f[2];
         if (depth < 0.15F) {
            return null;
         } else {
            float s = this.focal / depth;
            return new float[]{
               (float)w * this.ax + (vx * r[0] + vy * r[1] + vz * r[2]) * s, (float)h * this.ay - (vx * u[0] + vy * u[1] + vz * u[2]) * s, s, depth
            };
         }
      }
   }

   private static record Figure(boolean isYou, float x, float z, float facing, CloneAnimator.Pose pose, float dark) {
   }
}
