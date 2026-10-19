package com.pixelcite.turnbattle;

public final class SkillScaling {
   public static final int OFFENSE = 0;
   public static final int MAGIC = 2;
   public static final float PER_POINT = 0.017F;

   private SkillScaling() {
   }

   public static boolean usesOffense(BattleSkill s) {
      if (s == null) {
         return false;
      } else if (!scales(s)) {
         return false;
      } else {
         for (Affinity a : s.getAffinities()) {
            if (a == Affinity.SLASH) {
               return true;
            }
         }

         if (s instanceof LightnerSkill l) {
            return switch (l) {
               case RUDE_BUSTER, RUDE_BUSTER_PLUS, RED_BUSTER, X_SLASH -> true;
               default -> false;
            };
         } else if (s instanceof SoulMageSkill m) {
            return switch (m) {
               case STRUGGLE, DET_STRIKE, DET_STRIKE_P, DET_STRIKE_PP, PER_STRIKE, PER_STRIKE_P, PER_STRIKE_PP, INT_STRIKE, INT_STRIKE_P, INT_STRIKE_PP, PAT_STRIKE, PAT_STRIKE_P, PAT_STRIKE_PP, JUS_STRIKE, JUS_STRIKE_P, JUS_STRIKE_PP, BRA_STRIKE, BRA_STRIKE_P, BRA_STRIKE_PP, KIN_STRIKE, KIN_STRIKE_P, KIN_STRIKE_PP, PER_APEX, PER_APEX_P, PER_APEX_PP -> true;
               default -> false;
            };
         } else if (s instanceof MonsterSkill m) {
            return switch (m) {
               case BONE_TOSS, BONE_BARRAGE, GEAR_STRIKE, CHAOS_SABER_ASSAULT -> true;
               default -> false;
            };
         } else {
            return false;
         }
      }
   }

   public static boolean scales(BattleSkill s) {
      if (s == null) {
         return false;
      } else {
         return switch (s.getType()) {
            case DAMAGE, BYPASS, FREEZE, MULTI, AOE, HEAL, MAGNET, BURN -> true;
            default -> false;
         };
      }
   }

   public static int statIndex(BattleSkill s) {
      return usesOffense(s) ? 0 : 2;
   }

   public static String statName(BattleSkill s) {
      return usesOffense(s) ? "Offense" : "Magic";
   }

   public static int statColor(BattleSkill s) {
      return usesOffense(s) ? 16751178 : 12946687;
   }

   public static int statOf(CharStats st, BattleSkill s) {
      if (st == null) {
         return 0;
      } else {
         return usesOffense(s) ? st.offense : st.magic;
      }
   }
}
