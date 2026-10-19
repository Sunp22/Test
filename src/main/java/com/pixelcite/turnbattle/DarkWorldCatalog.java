package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DarkWorldCatalog {
   public static final String SHAPE_OVERWORLD = "minecraft:overworld";
   public static final String SHAPE_NETHER = "minecraft:the_nether";
   public static final String SHAPE_END = "minecraft:the_end";
   public static final String SHAPE_SKY = "turnbattle:sky_islands";
   public static final String[] WOOL_COLORS = new String[]{
      "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
   };
   public static final int[] WOOL_RGB = new int[]{
      15330540, 15758867, 12403891, 3846105, 16303655, 7387417, 15568300, 4080711, 9342598, 1411473, 7940780, 3488157, 7489320, 5532955, 10561314, 1316121
   };
   public static final String WOOL_GROUP = "turnbattle:wool_fields";
   public static final String GLASS = "turnbattle:dw_glass_mountains";
   public static final String PLAYGROUND = "turnbattle:dw_wooden_playground";
   public static final String MEADOW = "turnbattle:dw_sky_islands";
   public static final String STEEL = "turnbattle:dw_steel_caverns";
   public static final List<String> CUSTOM = List.of(
      "turnbattle:wool_fields", "turnbattle:dw_glass_mountains", "turnbattle:dw_wooden_playground", "turnbattle:dw_sky_islands", "turnbattle:dw_steel_caverns"
   );

   private DarkWorldCatalog() {
   }

   public static boolean isDefaultShape(String id) {
      return "minecraft:overworld".equals(id) || "turnbattle:sky_islands".equals(id);
   }

   public static String shapeName(String id) {
      return switch (id) {
         case "minecraft:overworld" -> "Overworld";
         case "minecraft:the_nether" -> "The Nether";
         case "minecraft:the_end" -> "The End";
         case "turnbattle:sky_islands" -> "Sky Islands";
         default -> prettify(id);
      };
   }

   public static String shapeBlurb(String id) {
      return switch (id) {
         case "minecraft:overworld" -> "Rolling hills, cliffs and caves.";
         case "minecraft:the_nether" -> "Vast caverns under a stone sky.";
         case "minecraft:the_end" -> "Islands adrift in the void.";
         case "turnbattle:sky_islands" -> "Floating land over a calm sea.";
         default -> "The lay of a far-off land.";
      };
   }

   public static String woolBiome(int colorIndex) {
      return "turnbattle:dw_wool_fields_" + WOOL_COLORS[colorIndex];
   }

   public static int woolIndex(String biomeId) {
      String pre = "turnbattle:dw_wool_fields_";
      if (biomeId != null && biomeId.startsWith(pre)) {
         String c = biomeId.substring(pre.length());

         for (int i = 0; i < WOOL_COLORS.length; i++) {
            if (WOOL_COLORS[i].equals(c)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static boolean isComingSoon(String id) {
      return "turnbattle:dw_steel_caverns".equals(id);
   }

   public static boolean isDefaultBiome(String biomeId) {
      return woolIndex(biomeId) >= 0
         || "turnbattle:dw_glass_mountains".equals(biomeId)
         || "turnbattle:dw_wooden_playground".equals(biomeId)
         || "turnbattle:dw_sky_islands".equals(biomeId);
   }

   public static boolean isHidden(String biomeId) {
      return biomeId.equals("turnbattle:magicant") || woolIndex(biomeId) >= 0 || biomeId.startsWith("turnbattle:") && !CUSTOM.contains(biomeId);
   }

   public static String customName(String id) {
      return switch (id) {
         case "turnbattle:wool_fields" -> "Wool Fields";
         case "turnbattle:dw_glass_mountains" -> "Glass Mountains";
         case "turnbattle:dw_wooden_playground" -> "Wooden Playground";
         case "turnbattle:dw_sky_islands" -> "Sky Meadow";
         case "turnbattle:dw_steel_caverns" -> "Steel Caverns";
         default -> prettify(id);
      };
   }

   public static String customBlurb(String id) {
      return switch (id) {
         case "turnbattle:wool_fields" -> "Quilted plush hills, cotton-candy trees and candlelight. Pick its colours.";
         case "turnbattle:dw_glass_mountains" -> "Crystal ground over glowing stone, amethyst spires.";
         case "turnbattle:dw_wooden_playground" -> "Parquet floors, toy-block towers, great old trees.";
         case "turnbattle:dw_sky_islands" -> "Blossom trees and wildflowers.";
         case "turnbattle:dw_steel_caverns" -> "Not yet available.";
         default -> "";
      };
   }

   public static int customColor(String id) {
      return switch (id) {
         case "turnbattle:wool_fields" -> 15568300;
         case "turnbattle:dw_glass_mountains" -> 11068656;
         case "turnbattle:dw_wooden_playground" -> 11569232;
         case "turnbattle:dw_sky_islands" -> 9228394;
         case "turnbattle:dw_steel_caverns" -> 9079440;
         default -> 8947848;
      };
   }

   public static boolean isCaveBiome(String biomeId) {
      return biomeId.equals("minecraft:lush_caves") || biomeId.equals("minecraft:dripstone_caves") || biomeId.equals("minecraft:deep_dark");
   }

   public static List<String> splitGroup(String group) {
      if (group != null && !group.isEmpty()) {
         List out = new ArrayList<>();

         for (String s : group.split("\\|")) {
            if (!s.isBlank()) {
               out.add(s.trim());
            }
         }

         return out;
      } else {
         return Collections.emptyList();
      }
   }

   public static String joinGroup(Iterable<String> members) {
      Set uniq = new LinkedHashSet<>();

      for (String m : members) {
         if (m != null && !m.isBlank()) {
            uniq.add(m);
         }
      }

      return String.join("|", uniq);
   }

   public static String prettify(String id) {
      String path = id.contains(":") ? id.substring(id.indexOf(58) + 1) : id;
      StringBuilder b = new StringBuilder();

      for (String w : path.replace('/', ' ').replace('_', ' ').split(" ")) {
         if (!w.isEmpty()) {
            if (b.length() > 0) {
               b.append(' ');
            }

            b.append(w.substring(0, 1).toUpperCase(Locale.ROOT)).append(w.substring(1));
         }
      }

      return b.toString();
   }
}
