package com.pixelcite.turnbattle;

public enum SkillType {
   HEAL,
   DAMAGE,
   FREEZE,
   BYPASS,
   SHIELD,
   SLEEP,
   MAGNET,
   CONFUSE,
   MULTI,
   AOE,
   PACIFY,
   BUFF,
   WEAKEN,
   SPARE,
   BURN,
   SPY,
   RAISE_THRESHOLD;

   public int color() {
      return switch (this) {
         case HEAL -> -12255420;
         case DAMAGE -> -48060;
         case FREEZE -> -12268289;
         case BYPASS -> -188;
         case SHIELD -> -7829249;
         case SLEEP -> -5579265;
         case MAGNET -> -12263971;
         case CONFUSE -> -21948;
         case MULTI -> -30516;
         case AOE -> -39356;
         case PACIFY -> -86;
         case BUFF -> -8892;
         case WEAKEN -> -30652;
         case SPARE -> -1;
         case BURN -> -39424;
         case SPY -> -5570595;
         case RAISE_THRESHOLD -> -8960;
      };
   }
}
