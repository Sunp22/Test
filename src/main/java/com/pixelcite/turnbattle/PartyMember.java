package com.pixelcite.turnbattle;

public enum PartyMember {
   NONE("None", null),
   NESS("Ness", PlayerClass.PSI_USER),
   LIGHTNER("Lightner", PlayerClass.LIGHTNER),
   FRISK("Frisk", PlayerClass.HUMAN_MAGE);

   public final String displayName;
   public final PlayerClass playerClass;

   private PartyMember(String displayName, PlayerClass playerClass) {
      this.displayName = displayName;
      this.playerClass = playerClass;
   }
}
