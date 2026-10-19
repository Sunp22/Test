package com.pixelcite.turnbattle;

import java.util.Random;

public enum VesselActionSkill implements BattleSkill {
   INSTANCE;

   public static String nameFor(String vesselName) {
      return vesselName != null && !vesselName.isEmpty() ? vesselName.charAt(0) + "-Action" : "?-Action";
   }

   @Override
   public String getDisplayName() {
      try {
         return nameFor(TurnBattleClientMod.clientVesselName);
      } catch (Throwable var2) {
         return "?-Action";
      }
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
      return SkillType.RAISE_THRESHOLD;
   }

   @Override
   public int getMinVal() {
      return 0;
   }

   @Override
   public int getMaxVal() {
      return 0;
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
      return 0;
   }

   @Override
   public String getEffectDescription() {
      return "Raises the spare threshold by 5%. Costs no TP.";
   }
}
