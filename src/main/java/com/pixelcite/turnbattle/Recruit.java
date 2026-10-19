package com.pixelcite.turnbattle;

public enum Recruit {
   ORI("Ori", PlayerClass.ORI, SoulType.ORI, "A kind, magically-talented girl who struggles to use her abilities.", 5),
   AVERY("Avery", PlayerClass.PSI_USER, SoulType.BRAVERY, "Coming soon.", 1),
   HAZEL("Hazel", PlayerClass.HUMAN_MAGE, SoulType.INTEGRITY, "Coming soon.", 1);

   public final String displayName;
   public final PlayerClass playerClass;
   public final SoulType soulType;
   public final String description;
   public final int startLevel;

   private Recruit(String displayName, PlayerClass playerClass, SoulType soulType, String description, int startLevel) {
      this.displayName = displayName;
      this.playerClass = playerClass;
      this.soulType = soulType;
      this.description = description;
      this.startLevel = startLevel;
   }

   public boolean isReady() {
      return !SkillTreeDefs.getRecruitTree(this).isEmpty();
   }
}
