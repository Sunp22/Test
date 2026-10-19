package com.pixelcite.turnbattle;

public final class SkillBonus {
   public static float magicFlat = 0.0F;

   private SkillBonus() {
   }

   public static float forSkill(WeaponStats ws, BattleSkill skill) {
      if (ws != null && skill != null && SkillScaling.scales(skill)) {
         boolean physicalSkill = SkillScaling.usesOffense(skill);
         return ws.magic != physicalSkill ? (float)ws.baseDamage : 0.0F;
      } else {
         return 0.0F;
      }
   }
}
