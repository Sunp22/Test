package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum MonsterSkill implements BattleSkill {
   MONSTER_SENSE("Monster Sense", 0, 1, SkillType.SPY, 0, 0, 0, "Read the enemy's stats, weaknesses, and resistances."),
   EMBER_SHOT("Ember Shot", 10, 1, SkillType.DAMAGE, 34, 54, 0, "89\u2013141 fire damage. Reduced by armor."),
   FIRE_SURGE("Fire Surge", 22, 10, SkillType.AOE, 29, 42, 0, "76\u2013110 fire damage to ALL enemies. Reduced by armor."),
   FLAME_VEIL("Flame Veil", 28, 10, SkillType.SHIELD, 30, 2, 0, "30% damage reduction for 2 turns."),
   INFERNO("Inferno", 45, 25, SkillType.BURN, 115, 173, 3, "301\u2013454 fire damage + 3 burn stacks. Reduced by armor."),
   MAGMA_SURGE("Magma Surge", 55, 35, SkillType.AOE, 105, 150, 0, "275\u2013393 fire damage to ALL enemies. Reduced by armor."),
   STATIC_BOLT("Static Bolt", 12, 2, SkillType.DAMAGE, 39, 58, 0, "102\u2013152 electric damage. Reduced by armor."),
   SHOCK_WAVE("Shock Wave", 25, 12, SkillType.WEAKEN, 2, 2, 35, "Reduces enemy attack for 2 turns. 35% chance to stun."),
   THUNDER_CLAP("Thunder Clap", 30, 15, SkillType.AOE, 36, 50, 0, "94\u2013131 electric damage to ALL enemies. Reduced by armor."),
   LIGHTNING_FANG("Lightning Fang", 38, 22, SkillType.DAMAGE, 111, 161, 0, "291\u2013422 electric damage. Reduced by armor."),
   VOLT_PRISON(
      "Volt Prison", 50, 30, SkillType.SLEEP, 2, 3, 55, "55% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn)."
   ),
   BONE_TOSS("Bone Toss", 10, 1, SkillType.DAMAGE, 30, 46, 0, "78\u2013120 impact damage. Reduced by armor."),
   BONE_WALL("Bone Wall", 20, 6, SkillType.SHIELD, 40, 3, 0, "40% damage reduction for 3 turns."),
   BONE_BARRAGE("Bone Barrage", 32, 18, SkillType.MULTI, 18, 26, 5, "5 hits of 47\u201368 impact damage each. Reduced by armor."),
   SKELETAL_HEAL("Skeletal Mend", 30, 18, SkillType.HEAL, 28, 45, 0, "Restores 42\u201367 HP."),
   BONE_STORM("Bone Storm", 48, 30, SkillType.AOE, 91, 129, 0, "238\u2013338 impact damage to ALL enemies. Reduced by armor."),
   IRON_PLATING("Iron Plating", 18, 10, SkillType.SHIELD, 50, 3, 0, "50% damage reduction for 3 turns."),
   GEAR_STRIKE("Gear Strike", 22, 10, SkillType.DAMAGE, 42, 63, 0, "110\u2013165 impact damage. Reduced by armor."),
   POWER_SURGE("Power Surge", 28, 14, SkillType.BUFF, 4, 4, 0, "Your next 4 BASHes deal 1.5x damage."),
   CONSTRUCT_HEAL("Self-Repair", 35, 16, SkillType.HEAL, 35, 55, 0, "Restores 52\u201382 HP."),
   OVERCHARGE("Overcharge", 55, 38, SkillType.AOE, 158, 237, 0, "414\u2013622 electric damage to ALL enemies. Reduced by armor."),
   STAR_BLAZING("Star Blazing", 80, 55, SkillType.AOE, 233, 350, 0, "611\u2013918 impact damage to ALL enemies. Reduced by armor."),
   SHOCKING_BREAKER("Shocking Breaker", 75, 55, SkillType.BYPASS, 212, 299, 0, "556\u2013784 electric damage, ignores all armor."),
   CHAOS_SABER_ASSAULT("Chaos Saber Assault", 90, 65, SkillType.MULTI, 107, 161, 6, "6 hits of 280\u2013422 slash damage each. Reduced by armor."),
   CHAOS_BUSTER("Chaos Buster", 100, 70, SkillType.MULTI, 13, 25, 30, "30 hits of 34\u201365 light damage to ALL enemies. Reduced by armor."),
   HYPER_GONER(
      "Hyper Goner", 100, 80, SkillType.DAMAGE, 1059, 1471, 0, "2779\u20133861 damage, single target. Matches the enemy's weakness element if they have one."
   );

   private final String displayName;
   private final String desc;
   private final int tp;
   private final int lv;
   private final int mn;
   private final int mx;
   private final int ex;
   private final SkillType type;
   public static final float SOUL_POWER_PER_SOUL = 0.1F;
   public static final int SOUL_SHIELD_PER_SOUL = 2;
   public static volatile int viewerSoulCount = 0;
   public static volatile boolean describingViewer = true;

   private MonsterSkill(String displayName, int tp, int lv, SkillType type, int mn, int mx, int ex, String desc) {
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
         case EMBER_SHOT, FIRE_SURGE, FLAME_VEIL, INFERNO, MAGMA_SURGE -> new Affinity[]{Affinity.FIRE};
         case STATIC_BOLT, SHOCK_WAVE, THUNDER_CLAP, LIGHTNING_FANG, VOLT_PRISON, OVERCHARGE -> new Affinity[]{Affinity.ELECTRIC};
         case BONE_TOSS, BONE_WALL, BONE_BARRAGE, BONE_STORM -> new Affinity[]{Affinity.IMPACT};
         default -> new Affinity[0];
         case IRON_PLATING, GEAR_STRIKE, POWER_SURGE, CONSTRUCT_HEAL -> new Affinity[]{Affinity.IMPACT};
         case STAR_BLAZING -> new Affinity[]{Affinity.IMPACT};
         case SHOCKING_BREAKER -> new Affinity[]{Affinity.ELECTRIC};
         case CHAOS_SABER_ASSAULT -> new Affinity[]{Affinity.SLASH};
         case CHAOS_BUSTER -> new Affinity[]{Affinity.LIGHT};
      };
   }

   public int soulsRequired() {
      return switch (this) {
         case STAR_BLAZING -> 5;
         case SHOCKING_BREAKER -> 5;
         case CHAOS_SABER_ASSAULT -> 6;
         case CHAOS_BUSTER -> 6;
         case HYPER_GONER -> 7;
         default -> 0;
      };
   }

   public static float soulPowerMult(int souls) {
      return 1.0F + 0.1F * (float)Math.max(0, souls);
   }

   @Override
   public String getLiveDescription(int actorMagic, boolean stab, boolean frozen) {
      int souls = describingViewer ? viewerSoulCount : 0;
      if (souls <= 0) {
         return BattleSkill.super.getLiveDescription(actorMagic, stab, frozen);
      } else {
         float magicMult = (1.0F + (float)actorMagic * 0.017F) * soulPowerMult(souls);
         float combined = (stab ? magicMult * 1.5F : magicMult) * (frozen ? 0.5F : 1.0F);
         String base = this.getLiveDescription(combined);
         if (this.getType() == SkillType.SHIELD) {
            base = Math.min(90, this.getMinVal() + 2 * souls) + "% damage reduction for " + this.getMaxVal() + " turns.";
         }

         return base + (stab ? " (1.5x STAB)" : "") + (frozen ? " (Frozen 50%)" : "") + " (+" + Math.round(10.0F * (float)souls) + "% SOULs)";
      }
   }

   public static List<BattleSkill> getAvailable(int level, Set<String> unlocked) {
      List list = new ArrayList<>();

      for (MonsterSkill s : values()) {
         if (unlocked.contains(s.name()) && s.lv <= level) {
            list.add(s);
         }
      }

      return list;
   }
}
