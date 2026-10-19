package com.pixelcite.turnbattle;

public final class RollingHp {
   public static final float BASE = 0.5F;
   public static final float PER_MAX_HP = 0.001F;

   private RollingHp() {
   }

   public static float step(float maxHp, float target) {
      return 0.5F + Math.max(0.0F, maxHp) * 0.001F;
   }

   public static float toward(float cur, float target, float maxHp) {
      if (target >= cur) {
         return target;
      } else {
         float s = step(maxHp, target);
         float d = target - cur;
         return Math.abs(d) <= s ? target : cur + Math.signum(d) * s;
      }
   }
}
