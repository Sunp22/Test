package com.pixelcite.turnbattle;

import java.util.List;

public final class RogueUpgrades {
   public static final List<RogueUpgrades.Upgrade> ALL = List.of(
      new RogueUpgrades.Upgrade("exp", "Quick Study", "\u270e", "+20% EXP in runs per level.", new int[]{10, 15, 20, 25, 30}),
      new RogueUpgrades.Upgrade("vigor", "Vigor", "\u2764", "Start every run with +2 Vitality per level.", new int[]{8, 12, 16, 20, 25}),
      new RogueUpgrades.Upgrade("might", "Might", "\u2694", "Start every run with +1 Offense and +1 Magic per level.", new int[]{8, 12, 16, 20, 25}),
      new RogueUpgrades.Upgrade("guard", "Guard", "\u26e8", "Start every run with +2 Defense per level.", new int[]{8, 12, 16, 20, 25}),
      new RogueUpgrades.Upgrade("opening", "Opening Hand", "\u2727", "Pick a card the moment a run starts, one per level.", new int[]{15, 25, 40}),
      new RogueUpgrades.Upgrade("reroll", "Second Look", "\u21bb", "Reroll a hand once per run, per level.", new int[]{12, 20, 30}),
      new RogueUpgrades.Upgrade("wide", "Wide Hand", "\u25a4", "Every hand has 4 cards instead of 3.", new int[]{35}),
      new RogueUpgrades.Upgrade("magnet", "Shard Magnet", "\u25c6", "+15% Soul Shards from every run, per level.", new int[]{15, 25, 35, 50}),
      new RogueUpgrades.Upgrade("kit", "Starter Kit", "\u2692", "Start runs with gear: a sword and food, then armor.", new int[]{10, 20, 35}),
      new RogueUpgrades.Upgrade("windup", "Deep Breath", "\u2605", "Start every run already holding Second Wind.", new int[]{40})
   );
   public static final SoulType[] SOULS = new SoulType[]{
      SoulType.DETERMINATION,
      SoulType.PERSEVERANCE,
      SoulType.INTEGRITY,
      SoulType.PATIENCE,
      SoulType.JUSTICE,
      SoulType.BRAVERY,
      SoulType.KINDNESS,
      SoulType.MONSTER
   };

   private RogueUpgrades() {
   }

   public static RogueUpgrades.Upgrade byId(String id) {
      for (RogueUpgrades.Upgrade u : ALL) {
         if (u.id().equals(id)) {
            return u;
         }
      }

      return null;
   }

   public static int soulCost(SoulType soul) {
      return switch (soul) {
         case DETERMINATION -> 50;
         case PERSEVERANCE -> 35;
         case MONSTER -> 40;
         case BRAVERY, JUSTICE -> 20;
         case INTEGRITY, PATIENCE -> 15;
         case KINDNESS -> 10;
         default -> -1;
      };
   }

   public static int shardsFor(int level, int battlesWon, long ticksSurvived) {
      int minutes = (int)(ticksSurvived / 1200L);
      return Math.max(0, Math.round((float)level * 1.5F) - 1) + battlesWon / 3 + minutes / 3;
   }

   public static String formatTime(long ticks) {
      long s = Math.max(0L, ticks) / 20L;
      return s >= 3600L ? String.format("%d:%02d:%02d", s / 3600L, s / 60L % 60L, s % 60L) : String.format("%d:%02d", s / 60L, s % 60L);
   }

   public static record Upgrade(String id, String name, String icon, String desc, int[] costs) {
      public int maxLevel() {
         return this.costs.length;
      }

      public int nextCost(int level) {
         return level < this.costs.length ? this.costs[level] : -1;
      }
   }
}
