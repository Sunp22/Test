package com.pixelcite.turnbattle;

import java.util.Random;

public class ActionSkill implements BattleSkill {
   public static final ActionSkill INSTANCE = new ActionSkill("");
   private final String displayName;

   public ActionSkill(String vesselName) {
      String first = vesselName != null && !vesselName.isEmpty() ? String.valueOf(vesselName.charAt(0)) : "?";
      this.displayName = first + "-Action";
   }

   @Override
   public String getDisplayName() {
      return this.displayName;
   }

   @Override
   public int getTpCost() {
      return 0;
   }

   @Override
   public int getLevelRequired() {
      return 1;
   }

   @Override
   public SkillType getType() {
      return SkillType.SPARE;
   }

   @Override
   public int getMinVal() {
      return 5;
   }

   @Override
   public int getMaxVal() {
      return 5;
   }

   @Override
   public int getExtra() {
      return 5;
   }

   @Override
   public int getOrdinal() {
      return -99;
   }

   @Override
   public Affinity[] getAffinities() {
      return new Affinity[0];
   }

   @Override
   public int roll(Random rng) {
      return 5;
   }

   @Override
   public String getEffectDescription() {
      return "Raise spare threshold by 5%. Free action.";
   }

   @Override
   public int rollScaled(Random rng) {
      return 5;
   }
}
