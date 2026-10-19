package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class DeathSoulTracker {
   public static boolean wasInBattle = false;
   public static boolean wasInBulletHell = false;
   public static float lastSoulScreenX = -1.0F;
   public static float lastSoulScreenY = -1.0F;
   private static long lastUpdateNanos = 0L;

   private DeathSoulTracker() {
   }

   public static void update(boolean inBulletHell, float soulScreenX, float soulScreenY) {
      wasInBattle = true;
      wasInBulletHell = inBulletHell;
      lastSoulScreenX = soulScreenX;
      lastSoulScreenY = soulScreenY;
      lastUpdateNanos = System.nanoTime();
   }

   public static boolean isFresh() {
      return System.nanoTime() - lastUpdateNanos < 2000000000L;
   }
}
