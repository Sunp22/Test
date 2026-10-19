package com.pixelcite.turnbattle;

public enum CharacterRace {
   HUMAN("Human"),
   MONSTER("Monster");

   public final String displayName;

   private CharacterRace(String displayName) {
      this.displayName = displayName;
   }
}
