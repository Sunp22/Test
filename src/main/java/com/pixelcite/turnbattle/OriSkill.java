package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum OriSkill implements BattleSkill {
   MOONBEAM("Moonbeam", 15, 1, SkillType.DAMAGE, 20, 34, 0, "A soft beam of moonlight: 52\u201389 damage."),
   MOONMEND("Moon's Mend", 20, 3, SkillType.HEAL, 20, 30, 0, "Restore 30\u201345 HP with a gentle touch to the shoulder."),
   LUNAR_VEIL("Lunar Veil", 25, 8, SkillType.SHIELD, 30, 3, 0, "30% damage reduction for 3 turns."),
   FULL_MOON("Lunar Mend", 40, 20, SkillType.HEAL, 75, 100, 0, "Restore 112\u2013150 HP with a gentle hug."),
   NIGHTBLOOM("Nightbloom", 60, 38, SkillType.HEAL, 117, 150, 0, "Restore 175\u2013225 HP to the whole party."),
   STARLIGHT("Nervous Plea", 18, 5, SkillType.RAISE_THRESHOLD, 10, 10, 10, "A plea. The enemy feels bad for you. Raises spare threshold by 10%."),
   ECLIPSE_WARD("Eclipse Charm", 35, 15, SkillType.BUFF, 0, 3, 150, "Create a charm around yourself or an ally. +50% attack for 3 turns."),
   CRESCENT("Crescent", 28, 6, SkillType.MULTI, 20, 32, 2, "Two slashes of Lunar Magic: 52\u201384 damage each."),
   LUNAR_STRIKE(
      "Lunar Strike", 50, 25, SkillType.BYPASS, 66, 121, 0, "A thin beam of Lunar Magic that phases through enemies and ignores armor. 173\u2013317 damage."
   ),
   MOONFALL("Moonfall", 65, 32, SkillType.AOE, 112, 160, 0, "Gravity based spell crushing ALL enemies: 294\u2013420 damage."),
   VOID_TIDE("Void Tide", 80, 42, SkillType.DAMAGE, 372, 502, 0, "A crushing tidal surge of dark moonlight. 976\u20131317 damage."),
   UMBRAL_CRASH(
      "Umbral Crash", 90, 55, SkillType.BYPASS, 237, 344, 0, "Charge into the opponent, coating yourself in Lunar Magic and igniting. 622\u2013903 damage."
   );

   private final String displayName;
   private final int tpCost;
   private final int levelRequired;
   private final SkillType type;
   private final int minVal;
   private final int maxVal;
   private final int extra;
   private final String effectDescription;

   private OriSkill(String displayName, int tpCost, int levelRequired, SkillType type, int minVal, int maxVal, int extra, String effectDescription) {
      this.displayName = displayName;
      this.tpCost = tpCost;
      this.levelRequired = levelRequired;
      this.type = type;
      this.minVal = minVal;
      this.maxVal = maxVal;
      this.extra = extra;
      this.effectDescription = effectDescription;
   }

   @Override
   public String getDisplayName() {
      return this.displayName;
   }

   @Override
   public int getTpCost() {
      return this.tpCost;
   }

   @Override
   public int getLevelRequired() {
      return this.levelRequired;
   }

   @Override
   public SkillType getType() {
      return this.type;
   }

   @Override
   public int getMinVal() {
      return this.minVal;
   }

   @Override
   public int getMaxVal() {
      return this.maxVal;
   }

   @Override
   public int getExtra() {
      return this.extra;
   }

   @Override
   public String getEffectDescription() {
      return this.effectDescription;
   }

   @Override
   public int getOrdinal() {
      return this.ordinal();
   }

   public static List<BattleSkill> getAvailable(int level, Set<String> unlocked) {
      List l = new ArrayList<>();

      for (OriSkill s : values()) {
         if (unlocked.contains(s.name()) && s.levelRequired <= level) {
            l.add(s);
         }
      }

      return l;
   }
}
