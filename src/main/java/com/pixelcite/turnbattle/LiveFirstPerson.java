package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public final class LiveFirstPerson {
   private static LiveFirstPerson.Kind kind = LiveFirstPerson.Kind.NONE;
   private static int step;
   private static long startMs;
   private static long durMs;
   private static float guard;
   private static float windup;
   private static float menu;
   private static long lastNanos;

   private LiveFirstPerson() {
   }

   public static void play(LiveFirstPerson.Kind k, int comboStep) {
      kind = k;
      step = comboStep;
      startMs = System.currentTimeMillis();

      durMs = switch (k) {
         case NONE -> 0L;
         case COMBO -> comboStep == 4 ? 700L : 450L;
         case BASH -> 720L;
         case DASH -> 240L;
         case CAST -> 950L;
         case ITEM -> 1000L;
      };
   }

   private static float progress() {
      if (kind != LiveFirstPerson.Kind.NONE && durMs > 0L) {
         float p = (float)(System.currentTimeMillis() - startMs) / (float)durMs;
         if (p >= 1.0F) {
            kind = LiveFirstPerson.Kind.NONE;
            return -1.0F;
         } else {
            return p;
         }
      } else {
         return -1.0F;
      }
   }

   public static boolean suppressSwing() {
      return LiveCombatClient.isLive() && (progress() >= 0.0F || guard > 0.05F || windup > 0.05F);
   }

   public static boolean apply(MatrixStack m, PlayerEntity player) {
      long now = System.nanoTime();
      float dt = lastNanos == 0L ? 0.016F : MathHelper.clamp((float)(now - lastNanos) / 1.0E9F, 0.0F, 0.1F);
      lastNanos = now;
      boolean live = LiveCombatClient.isLive();
      float k = 1.0F - (float)Math.exp((double)(-dt * 16.0F));
      guard = guard + ((live && LiveCombatClient.isGuarding() ? 1.0F : 0.0F) - guard) * k;
      windup = windup + ((live && LiveCombatClient.isWindingUp() ? 1.0F : 0.0F) - windup) * k;
      menu = menu + ((live && LiveCombatClient.menuOpen() ? 1.0F : 0.0F) - menu) * k;
      if (!live) {
         kind = LiveFirstPerson.Kind.NONE;
         if (guard + windup + menu < 0.01F) {
            return false;
         }
      }

      float[] o = new float[6];
      float p = progress();
      if (p >= 0.0F) {
         oneShot(o, p);
      }

      float t = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
      add(o, guard, -0.32F, 0.14F, 0.02F, 12.0F, 8.0F, 78.0F);
      float shake = (float)Math.sin((double)(t * 55.0F)) * 0.8F;
      add(o, windup, 0.08F, 0.26F, 0.12F, -12.0F, 58.0F + shake, -22.0F + shake);
      add(o, menu, 0.0F, -0.14F, 0.04F, 0.0F, -12.0F, 0.0F);
      if (Math.abs(o[0]) + Math.abs(o[1]) + Math.abs(o[2]) + Math.abs(o[3]) + Math.abs(o[4]) + Math.abs(o[5]) < 1.0E-4F) {
         return false;
      } else {
         int i = player.getMainArm() == Arm.RIGHT ? 1 : -1;
         float px = (float)i * 0.56F;
         float py = -0.52F;
         float pz = -0.72F;
         m.push();
         m.translate(px + o[0] * (float)i, py + o[1], pz + o[2]);
         m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(o[3] * (float)i));
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(o[4]));
         m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(o[5] * (float)i));
         m.translate(-px, -py, -pz);
         return true;
      }
   }

   private static void add(float[] o, float w, float x, float y, float z, float yaw, float pitch, float roll) {
      if (!(w <= 5.0E-4F)) {
         o[0] += x * w;
         o[1] += y * w;
         o[2] += z * w;
         o[3] += yaw * w;
         o[4] += pitch * w;
         o[5] += roll * w;
      }
   }

   private static float seg(float p, float a, float b) {
      return MathHelper.clamp((p - a) / (b - a), 0.0F, 1.0F);
   }

   private static float out3(float x) {
      float u = 1.0F - x;
      return 1.0F - u * u * u;
   }

   private static float sine(float x) {
      return (float)(-(Math.cos(Math.PI * (double)x) - 1.0) / 2.0);
   }

   private static float bell(float x) {
      return !(x <= 0.0F) && !(x >= 1.0F) ? (float)Math.sin(Math.PI * (double)x) : 0.0F;
   }

   private static void swing(float[] o, float p, float wEnd, float sEnd, float rStart, float[] wind, float[] strike) {
      float a = out3(seg(p, 0.0F, wEnd));
      float s = out3(seg(p, wEnd, sEnd));
      float r = sine(seg(p, rStart, 1.0F));
      float keep = 1.0F - r;

      for (int j = 0; j < 6; j++) {
         o[j] += (wind[j] * a * (1.0F - s) + strike[j] * s) * keep;
      }
   }

   private static void oneShot(float[] o, float p) {
      switch (kind) {
         case NONE:
         default:
            break;
         case COMBO:
            switch (step) {
               case 0:
                  swing(
                     o, p, 0.2F, 0.45F, 0.68F, new float[]{0.16F, 0.06F, 0.04F, -28.0F, 6.0F, -28.0F}, new float[]{-0.78F, -0.04F, -0.12F, 72.0F, -8.0F, 62.0F}
                  );
                  return;
               case 1:
                  swing(
                     o, p, 0.2F, 0.45F, 0.68F, new float[]{-0.6F, 0.08F, 0.02F, 52.0F, 4.0F, 58.0F}, new float[]{0.24F, -0.06F, -0.1F, -46.0F, -6.0F, -48.0F}
                  );
                  return;
               case 2:
                  swing(o, p, 0.2F, 0.45F, 0.68F, new float[]{-0.14F, 0.36F, 0.12F, 6.0F, 62.0F, 8.0F}, new float[]{-0.24F, -0.38F, -0.16F, 4.0F, -72.0F, 4.0F});
                  return;
               case 3:
                  swing(
                     o, p, 0.2F, 0.45F, 0.68F, new float[]{-0.08F, -0.34F, -0.02F, 0.0F, -48.0F, -6.0F}, new float[]{-0.22F, 0.32F, -0.12F, 8.0F, 58.0F, 10.0F}
                  );
                  return;
               default:
                  swing(
                     o,
                     p,
                     0.26F,
                     0.5F,
                     0.72F,
                     new float[]{0.32F, 0.04F, 0.16F, -48.0F, 8.0F, -38.0F},
                     new float[]{-0.95F, -0.06F, -0.26F, 98.0F, -10.0F, 78.0F}
                  );
                  return;
            }
         case BASH:
            swing(o, p, 0.06F, 0.34F, 0.62F, new float[]{0.08F, 0.26F, 0.12F, -12.0F, 58.0F, -22.0F}, new float[]{-0.34F, -0.42F, -0.22F, 22.0F, -82.0F, 30.0F});
            break;
         case DASH: {
            float b = bell(p);
            add(o, b, 0.06F, -0.22F, 0.16F, 0.0F, 18.0F, -10.0F);
            break;
         }
         case CAST: {
            float b = bell(Math.min(1.0F, p * 1.4F));
            add(o, b, 0.1F, -0.42F, 0.1F, -10.0F, -30.0F, -12.0F);
            break;
         }
         case ITEM: {
            float b = bell(p);
            add(o, b, 0.12F, -0.62F, 0.12F, 0.0F, -20.0F, 0.0F);
         }
      }
   }

   public static enum Kind {
      NONE,
      COMBO,
      BASH,
      DASH,
      CAST,
      ITEM;
   }
}
