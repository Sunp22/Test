package com.pixelcite.turnbattle;

public class MobAttack {
   public final String name;
   public final float damageMult;
   public final MobAttack.Targeting targeting;
   public final MobAttack.Special special;
   public final MobAttack.Visual visual;
   public final int colorOverride;

   public MobAttack(String name, float damageMult, MobAttack.Targeting targeting) {
      this(name, damageMult, targeting, MobAttack.Special.NONE, MobAttack.Visual.IMPACT, 0);
   }

   public MobAttack(String name, float damageMult, MobAttack.Targeting targeting, MobAttack.Visual visual) {
      this(name, damageMult, targeting, MobAttack.Special.NONE, visual, 0);
   }

   public MobAttack(String name, float damageMult, MobAttack.Targeting targeting, MobAttack.Visual visual, int color) {
      this(name, damageMult, targeting, MobAttack.Special.NONE, visual, color);
   }

   public MobAttack(String name, float damageMult, MobAttack.Targeting targeting, MobAttack.Special special, MobAttack.Visual visual) {
      this(name, damageMult, targeting, special, visual, 0);
   }

   public MobAttack(String name, float damageMult, MobAttack.Targeting targeting, MobAttack.Special special, MobAttack.Visual visual, int colorOverride) {
      this.name = name;
      this.damageMult = damageMult;
      this.targeting = targeting;
      this.special = special;
      this.visual = visual;
      this.colorOverride = colorOverride;
   }

   public MobAttack(String name, float damageMult, MobAttack.Targeting targeting, MobAttack.Special special) {
      this(name, damageMult, targeting, special, defaultVisualForSpecial(special), 0);
   }

   private static MobAttack.Visual defaultVisualForSpecial(MobAttack.Special s) {
      return switch (s) {
         case BUFF -> MobAttack.Visual.BUFF;
         case HEAL_MOB -> MobAttack.Visual.HEAL;
         case CURE_MOB -> MobAttack.Visual.SHIELD;
         case APPLY_FREEZE -> MobAttack.Visual.FREEZE;
         case APPLY_SLEEP -> MobAttack.Visual.SLEEP;
         case APPLY_CONFUSION -> MobAttack.Visual.CONFUSE;
         default -> MobAttack.Visual.IMPACT;
      };
   }

   public SkillType toSkillType() {
      return switch (this.visual) {
         case SLASH -> SkillType.MULTI;
         case AOE_RING -> SkillType.AOE;
         case THUNDER -> SkillType.BYPASS;
         case FREEZE -> SkillType.FREEZE;
         case FIRE -> SkillType.BURN;
         case HEAL -> SkillType.HEAL;
         case BUFF -> SkillType.BUFF;
         case WEAKEN -> SkillType.WEAKEN;
         case SLEEP -> SkillType.SLEEP;
         case CONFUSE -> SkillType.CONFUSE;
         case SHIELD -> SkillType.SHIELD;
         default -> SkillType.DAMAGE;
      };
   }

   public static enum Special {
      NONE,
      BUFF,
      HEAL_MOB,
      CURE_MOB,
      APPLY_FREEZE,
      APPLY_SLEEP,
      APPLY_CONFUSION;
   }

   public static enum Targeting {
      SINGLE,
      MULTI,
      AOE;
   }

   public static enum Visual {
      IMPACT,
      SLASH,
      AOE_RING,
      THUNDER,
      FREEZE,
      FIRE,
      HEAL,
      BUFF,
      WEAKEN,
      SLEEP,
      CONFUSE,
      SHIELD;
   }
}
