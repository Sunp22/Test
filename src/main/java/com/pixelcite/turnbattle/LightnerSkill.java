package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum LightnerSkill implements BattleSkill {
   RUDE_BUSTER("Rude Buster", 20, 1, SkillType.MULTI, 12, 20, 2, "Strike twice for 31\u201352 damage each. Armor applies."),
   ICE_SHOCK("Ice Shock", 22, 5, SkillType.FREEZE, 15, 26, 2, "39\u201368 ice damage + freeze the enemy for 2 turns."),
   GLITTER_BOMB("Glitter Bomb", 35, 15, SkillType.AOE, 32, 50, 0, "84\u2013131 damage to ALL enemies."),
   SNOWGRAVE(
      "Snowgrave",
      200,
      45,
      SkillType.FREEZE,
      1050,
      1050,
      4,
      "Devastating ice damage, freezes for 4 turns. Fatal. Costs double your max TP: a full party TP bar AND a Thorn Ring."
   ),
   RUDE_BUSTER_PLUS("Rude Buster+", 48, 22, SkillType.MULTI, 43, 60, 3, "Three fierce strikes for 112\u2013157 damage each."),
   RED_BUSTER("Red Buster", 60, 35, SkillType.BURN, 42, 58, 70, "Four brutal strikes for 110\u2013152 damage each + 70% Burn 3."),
   X_SLASH("X-Slash", 50, 50, SkillType.MULTI, 158, 179, 2, "Two crossing slashes for 414\u2013469 damage each."),
   GIGAFLARE("Gigaflare", 85, 55, SkillType.BYPASS, 223, 335, 0, "585\u2013879 light damage, ignores all armor."),
   HEAL_PRAYER("Heal Prayer", 20, 1, SkillType.HEAL, 20, 30, 0, "Pray for healing \u2014 restores 30\u201345 HP."),
   DUAL_HEAL("Dual Heal", 40, 25, SkillType.HEAL, 50, 70, 0, "A powerful prayer that restores 75\u2013105 HP to ALL allies."),
   ANGELIC_HALO("Angelic Halo", 55, 25, SkillType.SHIELD, 30, 3, 40, "Heals 40 HP and reduces damage by 30% for 3 turns."),
   WARD("Ward", 45, 25, SkillType.SHIELD, 50, 5, 0, "Create a ward \u2014 50% damage reduction for 5 turns."),
   GREAT_SHIELD("Great Shield", 65, 40, SkillType.SHIELD, 70, 3, 0, "An iron barrier \u2014 70% damage reduction for 3 turns."),
   LULLABY("Lullaby", 30, 10, SkillType.SLEEP, 3, 3, 75, "75% chance to lull the enemy to sleep (asleep for at least a turn, then likelier to wake each turn)."),
   SHINING_SOUL("Shining Soul", 80, 35, SkillType.BUFF, 2, 2, 0, "You shine your SOUL upon yourself or an ally, giving them 2.5x BASH damage for 2 turns."),
   PACIFY("Pacify", 25, 5, SkillType.PACIFY, 0, 0, 0, "Enemy must be asleep. Spares them instantly if at 50% HP or less."),
   C_ACTION("Sleep Mist", 40, 18, SkillType.PACIFY, 25, 35, 1, "Enemy must be asleep. Spares and absorbs 37\u201378 TP if at 75% HP or less.");

   private final String displayName;
   private final String desc;
   private final int tp;
   private final int lv;
   private final int mn;
   private final int mx;
   private final int ex;
   private final SkillType type;

   private LightnerSkill(String displayName, int tp, int lv, SkillType type, int mn, int mx, int ex, String desc) {
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
         case RUDE_BUSTER, X_SLASH -> new Affinity[]{Affinity.SLASH};
         case ICE_SHOCK, SNOWGRAVE -> new Affinity[]{Affinity.ICE};
         case GLITTER_BOMB -> new Affinity[]{Affinity.IMPACT};
         case RUDE_BUSTER_PLUS -> new Affinity[]{Affinity.SLASH, Affinity.IMPACT};
         case RED_BUSTER -> new Affinity[]{Affinity.FIRE};
         case GIGAFLARE -> new Affinity[]{Affinity.LIGHT};
         default -> new Affinity[0];
      };
   }

   public static List<BattleSkill> getAvailable(int level) {
      List list = new ArrayList<>();

      for (LightnerSkill s : values()) {
         list.add(s);
      }

      return list;
   }

   public static List<BattleSkill> getAvailable(int level, Set<String> unlocked) {
      List list = new ArrayList<>();

      for (LightnerSkill s : values()) {
         if (unlocked.contains(s.name()) && s.lv <= level) {
            list.add(s);
         }
      }

      return list;
   }

   public static int snowgraveCost(float maxTp) {
      return Math.round(Math.max(1.0F, maxTp) * 2.0F);
   }
}
