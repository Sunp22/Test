package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum HumanMageSkill implements BattleSkill {
   ACT_CHECK("Check", 0, 1, SkillType.SPY, 0, 0, 0, "Reveal the enemy's ATK, DEF, weaknesses, and resistances."),
   ACT_CHALLENGE("Challenge", 0, 1, SkillType.MAGNET, 15, 15, 0, "Taunt the enemy dramatically. Gain 30 TP."),
   ACT_FLIRT("Flirt", 20, 4, SkillType.CONFUSE, 2, 2, 50, "50% chance to confuse the enemy for 2 turns."),
   ACT_COMFORT(
      "Comfort", 20, 6, SkillType.SLEEP, 2, 2, 40, "40% chance to lull the enemy to sleep (asleep for at least a turn, then likelier to wake each turn)."
   ),
   ACT_SING("Sing", 35, 18, SkillType.SLEEP, 3, 4, 65, "65% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn)."),
   ACT_BECKON("Beckon", 38, 18, SkillType.WEAKEN, 3, 3, 40, "40% enemy attack reduction for 3 turns."),
   ACT_THREATEN("Threaten", 28, 8, SkillType.BUFF, 3, 3, 0, "Raise your power. Your next 3 BASHes deal 1.5\u00d7 damage."),
   ACT_HUM("Hum", 22, 1, SkillType.HEAL, 15, 25, 0, "Hum peacefully. Restore 22\u201337 HP."),
   ACT_INSPIRE("Inspire", 60, 30, SkillType.SHIELD, 40, 3, 60, "Shield 40% for 3 turns and heal 60 HP."),
   ACT_INVOKE("Invoke", 70, 40, SkillType.BYPASS, 165, 267, 0, "Channel raw magic \u2014 433\u2013700 damage, ignores all armor."),
   ACT_SPARE("Console", 18, 1, SkillType.RAISE_THRESHOLD, 0, 0, 10, "Raise the spare threshold by 10%."),
   ACT_TALK("Talk", 30, 10, SkillType.RAISE_THRESHOLD, 0, 0, 15, "Raise the spare threshold by 15%."),
   ACT_PLEAD("Plead", 40, 20, SkillType.RAISE_THRESHOLD, 0, 0, 20, "Raise the spare threshold by 20%."),
   ACT_ENTREAT("SAVE", 100, 50, SkillType.RAISE_THRESHOLD, 0, 0, 50, "Raise the spare threshold by 50%.");

   private final String displayName;
   private final String desc;
   private final int tp;
   private final int lv;
   private final int mn;
   private final int mx;
   private final int ex;
   private final SkillType type;

   private HumanMageSkill(String displayName, int tp, int lv, SkillType type, int mn, int mx, int ex, String desc) {
      this.displayName = displayName;
      this.tp = tp;
      this.lv = lv;
      this.type = type;
      this.mn = mn;
      this.mx = mx;
      this.ex = ex;
      this.desc = desc;
   }

   @Override
   public String getDisplayName() {
      return this.displayName;
   }

   @Override
   public int getTpCost() {
      return this.tp;
   }

   @Override
   public int getLevelRequired() {
      return this.lv;
   }

   @Override
   public SkillType getType() {
      return this.type;
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
   public String getEffectDescription() {
      return this.desc;
   }

   @Override
   public int getOrdinal() {
      return this.ordinal();
   }

   @Override
   public Affinity[] getAffinities() {
      return switch (this) {
         case ACT_INVOKE -> new Affinity[]{Affinity.IMPACT};
         default -> new Affinity[0];
      };
   }

   public static List<BattleSkill> getAvailable(int level) {
      List list = new ArrayList<>();

      for (HumanMageSkill s : values()) {
         list.add(s);
      }

      return list;
   }

   public static List<BattleSkill> getAvailable(int level, Set<String> unlocked) {
      List list = new ArrayList<>();

      for (HumanMageSkill s : values()) {
         if (unlocked.contains(s.name()) && s.lv <= level) {
            list.add(s);
         }
      }

      return list;
   }
}
