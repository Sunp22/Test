package com.pixelcite.turnbattle;

public enum BattleView {
   OVERHAUL(
      "3D",
      "Fight right where you stand. The camera follows the action over your shoulder, and skills, effects and damage numbers play out in the world for everyone to see."
   ),
   TRADITIONAL("Traditional", "The classic battle screen: your party and the enemies face off on their own stage.");

   public final String displayName;
   public final String description;
   public static final boolean CHOOSABLE_3D = false;

   private BattleView(String displayName, String description) {
      this.displayName = displayName;
      this.description = description;
   }

   static BattleView fromOrdinal(int ord) {
      BattleView[] v = values();
      return ord >= 0 && ord < v.length ? v[ord] : TRADITIONAL;
   }
}
