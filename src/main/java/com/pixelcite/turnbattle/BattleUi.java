package com.pixelcite.turnbattle;

public enum BattleUi {
   DELTARUNE("Deltarune"),
   EARTHBOUND("Earthbound");

   public final String displayName;

   private BattleUi(String displayName) {
      this.displayName = displayName;
   }

   static BattleUi byName(String s) {
      for (BattleUi u : values()) {
         if (u.name().equalsIgnoreCase(s)) {
            return u;
         }
      }

      return DELTARUNE;
   }
}
