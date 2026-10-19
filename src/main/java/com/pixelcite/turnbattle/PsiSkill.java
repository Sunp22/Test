package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum PsiSkill implements BattleSkill {
   LIFEUP_A("Lifeup \u03b1", 15, 1, SkillType.HEAL, 15, 20, 0),
   LIFEUP_B("Lifeup \u03b2", 28, 12, SkillType.HEAL, 30, 45, 0),
   LIFEUP_G("Lifeup \u03b3", 42, 28, SkillType.HEAL, 60, 85, 0),
   LIFEUP_O("Lifeup \u03a9", 60, 45, SkillType.HEAL, 999, 999, 0),
   FIRE_A("PSI Fire \u03b1", 15, 1, SkillType.BURN, 9, 20, 40),
   FIRE_B("PSI Fire \u03b2", 28, 10, SkillType.BURN, 23, 37, 50),
   FIRE_G("PSI Fire \u03b3", 40, 25, SkillType.BURN, 83, 120, 60),
   FIRE_O("PSI Fire \u03a9", 58, 45, SkillType.BURN, 199, 286, 75),
   FREEZE_A("PSI Freeze \u03b1", 18, 3, SkillType.FREEZE, 7, 15, 1),
   FREEZE_B("PSI Freeze \u03b2", 32, 14, SkillType.FREEZE, 31, 49, 2),
   FREEZE_G("PSI Freeze \u03b3", 48, 30, SkillType.FREEZE, 124, 187, 3),
   FREEZE_O("PSI Freeze \u03a9", 65, 50, SkillType.FREEZE, 276, 400, 4),
   THUNDER_A("PSI Thunder \u03b1", 16, 2, SkillType.BYPASS, 12, 24, 0),
   THUNDER_B("PSI Thunder \u03b2", 30, 12, SkillType.BYPASS, 28, 44, 0),
   THUNDER_G("PSI Thunder \u03b3", 45, 27, SkillType.BYPASS, 57, 81, 2),
   THUNDER_O("PSI Thunder \u03a9", 62, 48, SkillType.BYPASS, 134, 182, 2),
   SHIELD_A("PSI Shield \u03b1", 20, 6, SkillType.SHIELD, 30, 3, 0),
   SHIELD_B("PSI Shield \u03b2", 38, 22, SkillType.SHIELD, 60, 5, 0),
   HYPNOSIS_A("Hypnosis \u03b1", 20, 8, SkillType.SLEEP, 2, 2, 50),
   HYPNOSIS_B("Hypnosis \u03b2", 35, 26, SkillType.SLEEP, 4, 4, 80),
   MAGNET("PSI Magnet", 0, 5, SkillType.MAGNET, 30, 50, 0),
   BRAINSHOCK("Brainshock", 28, 15, SkillType.CONFUSE, 3, 3, 0),
   ROCKIN_A("PSI Rockin \u03b1", 22, 8, SkillType.AOE, 7, 13, 3),
   ROCKIN_B("PSI Rockin \u03b2", 38, 20, SkillType.AOE, 20, 31, 3),
   ROCKIN_G("PSI Rockin \u03b3", 55, 38, SkillType.AOE, 54, 78, 3),
   ROCKIN_O("PSI Rockin \u03a9", 75, 58, SkillType.AOE, 81, 111, 4),
   STARSTORM_A("PSI Starstorm \u03b1", 78, 66, SkillType.AOE, 362, 521, 0),
   STARSTORM_O("PSI Starstorm \u03a9", 90, 80, SkillType.AOE, 502, 680, 0),
   OFFENSE_A("Offense Up \u03b1", 20, 24, SkillType.BUFF, 2, 0, 150),
   OFFENSE_O("Offense Up \u03a9", 38, 36, SkillType.BUFF, 4, 0, 200),
   SPY("Spy", 10, 2, SkillType.SPY, 0, 0, 0);

   private final String d;
   private final int tp;
   private final int lr;
   private final int mn;
   private final int mx;
   private final int ex;
   private final SkillType t;

   private PsiSkill(String d, int tp, int lr, SkillType t, int mn, int mx, int ex) {
      this.d = d;
      this.tp = tp;
      this.lr = lr;
      this.t = t;
      this.mn = mn;
      this.mx = mx;
      this.ex = ex;
   }

   @Override
   public String getDisplayName() {
      return this.d;
   }

   @Override
   public int getTpCost() {
      return this.tp;
   }

   @Override
   public int getLevelRequired() {
      return this.lr;
   }

   @Override
   public SkillType getType() {
      return this.t;
   }

   @Override
   public int getMinVal() {
      return this.mn;
   }

   @Override
   public int getMaxVal() {
      return this.mx;
   }

   @Override
   public int getExtra() {
      return this.ex;
   }

   @Override
   public int getOrdinal() {
      return this.ordinal();
   }

   @Override
   public String getEffectDescription() {
      return switch (this) {
         case LIFEUP_A -> "Restores 22\u201330 HP.";
         case LIFEUP_B -> "Restores 45\u201367 HP.";
         case LIFEUP_G -> "Restores 90\u2013127 HP.";
         case LIFEUP_O -> "Fully restores HP.";
         case FIRE_A -> "23\u201352 fire damage + 40% chance to Burn 2. Reduced by armor.";
         case FIRE_B -> "Deals 60\u201397 fire damage to 2 random enemies + 50% Burn 2. Reduced by armor.";
         case FIRE_G -> "Deals 217\u2013315 fire damage to 2 random enemies + 60% Burn 3. Reduced by armor.";
         case FIRE_O -> "Deals 522\u2013750 fire damage to 2 random enemies + 75% Burn 4. Reduced by armor.";
         case FREEZE_A -> "18\u201339 ice damage + 50% freeze 1 turn.";
         case FREEZE_B -> "81\u2013128 ice damage + freeze 2 turns.";
         case FREEZE_G -> "325\u2013490 ice damage + freeze 3 turns.";
         case FREEZE_O -> "724\u20131050 ice damage. Guaranteed freeze 4 turns.";
         case THUNDER_A -> "31\u201363 lightning damage, ignores armor.";
         case THUNDER_B -> "73\u2013115 lightning damage, ignores armor.";
         case THUNDER_G -> "149\u2013212 damage to 2 random enemies, ignores armor.";
         case THUNDER_O -> "351\u2013477 damage to 2 random enemies, ignores armor.";
         case SHIELD_A -> "30% damage reduction for 3 turns.";
         case SHIELD_B -> "60% damage reduction for 5 turns.";
         case HYPNOSIS_A -> "50% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn).";
         case HYPNOSIS_B -> "80% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn).";
         case MAGNET -> "Absorb 78\u2013131 TP from the environment.";
         case BRAINSHOCK -> "Confuse the enemy for 3 turns.";
         case ROCKIN_A -> "3 hits of 18\u201334 damage to ALL enemies.";
         case ROCKIN_B -> "3 hits of 52\u201381 damage to ALL enemies.";
         case ROCKIN_G -> "3 hits of 141\u2013204 damage to ALL enemies.";
         case ROCKIN_O -> "4 hits of 212\u2013291 damage to ALL enemies.";
         case STARSTORM_A -> "950\u20131367 damage to ALL enemies. Reduced by armor.";
         case STARSTORM_O -> "1317\u20131785 damage to ALL enemies. Reduced by armor.";
         case OFFENSE_A -> "Boosts BASH damage by 1.5\u00d7 for 2 hits.";
         case OFFENSE_O -> "Boosts BASH damage by 2\u00d7 for 4 hits.";
         case SPY -> "Reveals the target's weakness and resistance.";
      };
   }

   @Override
   public Affinity[] getAffinities() {
      return switch (this) {
         case LIFEUP_A, LIFEUP_B, LIFEUP_G, LIFEUP_O, SHIELD_A, SHIELD_B, HYPNOSIS_A, HYPNOSIS_B, MAGNET, BRAINSHOCK, OFFENSE_A, OFFENSE_O -> new Affinity[0];
         case FIRE_A, FIRE_B, FIRE_G, FIRE_O -> new Affinity[]{Affinity.FIRE, Affinity.PSI};
         case FREEZE_A, FREEZE_B, FREEZE_G, FREEZE_O -> new Affinity[]{Affinity.ICE, Affinity.PSI};
         case THUNDER_A, THUNDER_B, THUNDER_G, THUNDER_O -> new Affinity[]{Affinity.ELECTRIC, Affinity.PSI};
         case ROCKIN_A, ROCKIN_B, ROCKIN_G, ROCKIN_O, STARSTORM_A, STARSTORM_O -> new Affinity[]{Affinity.IMPACT, Affinity.PSI};
         default -> new Affinity[]{Affinity.PSI};
      };
   }

   public static List<BattleSkill> getAvailable(int level) {
      List l = new ArrayList<>();

      for (PsiSkill s : values()) {
         l.add(s);
      }

      return l;
   }

   public static List<BattleSkill> getAvailable(int level, Set<String> unlocked) {
      List l = new ArrayList<>();

      for (PsiSkill s : values()) {
         if (unlocked.contains(s.name()) && s.lr <= level) {
            l.add(s);
         }
      }

      return l;
   }
}
