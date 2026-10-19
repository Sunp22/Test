package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class LightPillars {
   public static final double RADIUS = 2.2;
   public static final double USE_RADIUS = 2.9;
   private static final int SCATTERED = 24;
   private static final Map<String, List<int[]>> cache = new ConcurrentHashMap<>();

   private LightPillars() {
   }

   public static List<int[]> positions(String dim, int fountainX, int fountainZ) {
      String key = dim + "@" + fountainX + "," + fountainZ;
      return cache.computeIfAbsent(key, k -> {
         List out = new ArrayList<>();
         out.add(new int[]{56, 0});
         Random r = new Random((long)dim.hashCode() * -7046029254386353131L ^ (long)fountainX * 341873128712L + (long)fountainZ * 132897987541L);
         int guard = 0;

         while (out.size() < 25 && guard++ < 400) {
            double ang = r.nextDouble() * Math.PI * 2.0;
            double dist = 140.0 + r.nextDouble() * 1100.0;
            int x = 50 + (int)Math.round(StrictMath.cos(ang) * dist);
            int z = (int)Math.round(StrictMath.sin(ang) * dist);
            if (!(sq((double)(x - fountainX)) + sq((double)(z - fountainZ)) < 6400.0)) {
               boolean crowded = false;
               Iterator var14 = out.iterator();

               while (true) {
                  if (var14.hasNext()) {
                     int[] o = (int[])var14.next();
                     if (!(sq((double)(o[0] - x)) + sq((double)(o[1] - z)) < 25600.0)) {
                        continue;
                     }

                     crowded = true;
                  }

                  if (!crowded) {
                     out.add(new int[]{x, z});
                  }
                  break;
               }
            }
         }

         return List.copyOf(out);
      });
   }

   public static int[] near(String dim, int fountainX, int fountainZ, double x, double z, double radius) {
      for (int[] p : positions(dim, fountainX, fountainZ)) {
         double dx = (double)p[0] + 0.5 - x;
         double dz = (double)p[1] + 0.5 - z;
         if (dx * dx + dz * dz <= radius * radius) {
            return p;
         }
      }

      return null;
   }

   private static double sq(double v) {
      return v * v;
   }
}
