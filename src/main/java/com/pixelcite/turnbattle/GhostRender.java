package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GhostRender {
   public static boolean active;
   public static float r = 1.0F;
   public static float g = 1.0F;
   public static float b = 1.0F;
   public static float a = 1.0F;
   public static float white;
   public static boolean red;

   private GhostRender() {
   }

   public static void set(float r, float g, float b, float a, float white, boolean red) {
      active = true;
      GhostRender.r = r;
      GhostRender.g = g;
      GhostRender.b = b;
      GhostRender.a = a;
      GhostRender.white = white;
      GhostRender.red = red;
   }

   public static void clear() {
      active = false;
      a = 1.0F;
      b = 1.0F;
      g = 1.0F;
      r = 1.0F;
      white = 0.0F;
      red = false;
   }

   public static boolean translucent() {
      return active && a < 0.995F;
   }

   public static boolean features() {
      return !active || a >= 0.995F;
   }
}
