package com.pixelcite.turnbattle;

public final class AreaLevel {
   public static final double FULL_DISTANCE = 10000.0;
   public static final double END_HEADSTART = 6000.0;

   private AreaLevel() {
   }

   public static int at(String dimId, double x, double z, float multiplier) {
      double d = Math.sqrt(x * x + z * z);
      if ("minecraft:the_nether".equals(dimId)) {
         d *= 8.0;
      } else if ("minecraft:the_end".equals(dimId)) {
         d += 6000.0;
      }

      double f = Math.min(1.0, d * Math.max(0.01, (double)multiplier) / 10000.0);
      return Math.max(1, Math.min(99, 1 + (int)Math.round(98.0 * f)));
   }
}
