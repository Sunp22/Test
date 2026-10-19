package com.pixelcite.turnbattle;

public final class SkillTiming {
   private SkillTiming() {
   }

   public static int startup(BattleSkill skill) {
      if (skill == LightnerSkill.SNOWGRAVE) {
         return 50;
      } else {
         return switch (skill.getType()) {
            case MULTI -> 25;
            case AOE -> 30;
            case DAMAGE -> 20;
            case BYPASS -> 22;
            case FREEZE -> 24;
            case HEAL -> 20;
            case SHIELD -> 18;
            case SLEEP -> 20;
            case CONFUSE -> 18;
            case MAGNET -> 18;
            case BUFF -> 16;
            case WEAKEN -> 16;
            case PACIFY, SPARE -> 22;
            default -> 18;
         };
      }
   }

   public static int hit(BattleSkill skill) {
      return skill == LightnerSkill.SNOWGRAVE ? 22 : hit(skill.getType());
   }

   public static int mobStartup(SkillType type) {
      return switch (type) {
         case MULTI -> 25;
         case AOE -> 30;
         default -> 20;
         case FREEZE -> 24;
         case HEAL -> 20;
         case SHIELD -> 18;
         case SLEEP -> 20;
         case CONFUSE -> 18;
         case BUFF -> 16;
         case WEAKEN -> 16;
      };
   }

   public static int hit(SkillType t) {
      return switch (t) {
         case MULTI -> 14;
         case AOE -> 16;
         case DAMAGE, BYPASS, FREEZE, BURN -> 13;
         default -> 11;
      };
   }

   public static int at(BattleSkill skill, float frac) {
      return Math.max(1, Math.round((float)(startup(skill) + hit(skill)) * frac));
   }

   public static int mobAt(SkillType type, float frac) {
      return Math.max(1, Math.round((float)(mobStartup(type) + hit(type)) * frac));
   }
}
