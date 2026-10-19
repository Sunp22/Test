package com.pixelcite.turnbattle;

public enum PlayerClass {
   PSI_USER("PSI User", "All Rounder, No Mercy Skills."),
   LIGHTNER("Lightner", "Mercy, Damage Skills."),
   HUMAN_MAGE("Human", "Status, Mercy Skills/Acts"),
   SOUL_MAGE("Soul Mage", "Skills shaped by your soul trait."),
   ORI("Lunar Spirit", "Lunar & support skills unique to Ori."),
   MONSTER("Monster", "Elemental skills and soul absorption.");

   public final String displayName;
   public final String description;

   private PlayerClass(String displayName, String description) {
      this.displayName = displayName;
      this.description = description;
   }
}
