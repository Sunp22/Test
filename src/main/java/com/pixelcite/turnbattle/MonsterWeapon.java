package com.pixelcite.turnbattle;

public enum MonsterWeapon {
   LIGHT_SPEAR("Light Spear", "A spear made of light. Can be conjured and released for a barrage of rapid-fire damage and AOE attacks."),
   RED_SABER("Crimson Saber", "A swift saber designed for applying buffs and burn damage."),
   ROYAL_TRIDENT("Royal Trident", "A crimson trident typically forged for royalty. Specializes in big, single-target damaging attacks, and debuffs."),
   CANINE_AXE("Canine Axe", "A heavy axe designed for monsters big enough to hold it. Good damage and applies SHIELD to allies.");

   public final String displayName;
   public final String description;

   private MonsterWeapon(String displayName, String description) {
      this.displayName = displayName;
      this.description = description;
   }
}
