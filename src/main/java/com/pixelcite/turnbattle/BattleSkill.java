package com.pixelcite.turnbattle;

import java.util.Random;

public interface BattleSkill {
   String getDisplayName();

   int getTpCost();

   int getLevelRequired();

   SkillType getType();

   int getMinVal();

   int getMaxVal();

   int getExtra();

   String getEffectDescription();

   default Affinity[] getAffinities() {
      return new Affinity[0];
   }

   int getOrdinal();

   default int roll(Random rng) {
      int lo = this.getMinVal();
      int hi = this.getMaxVal();
      return lo >= hi ? lo : lo + rng.nextInt(hi - lo + 1);
   }

   static float damageMult() {
      return 1.75F * TurnBattleConfig.get().skillDamageScale;
   }

   static float healMult() {
      return TurnBattleConfig.get().healSpellScale;
   }

   static float tpMult() {
      return 1.75F * TurnBattleConfig.get().tpSpellScale;
   }

   default String getScaledRangeDisplay() {
      if (this.getMinVal() == 999) {
         return "Full HP";
      } else {
         float scale = switch (this.getType()) {
            case DAMAGE, BYPASS, FREEZE, MULTI, AOE, BURN -> damageMult();
            case HEAL -> healMult();
            case MAGNET -> tpMult() * 0.5F;
            default -> 1.0F;
         };

         return switch (this.getType()) {
            case DAMAGE, BYPASS, FREEZE, MULTI, AOE, BURN, HEAL, MAGNET -> {
               int lo = (int)((float)this.getMinVal() * scale);
               int hi = (int)((float)this.getMaxVal() * scale);
               yield lo == hi ? String.valueOf(lo) : lo + "\u2013" + hi;
            }
            default -> this.getMinVal() == this.getMaxVal() ? String.valueOf(this.getMinVal()) : this.getMinVal() + "\u2013" + this.getMaxVal();
         };
      }
   }

   default String getLiveDescription(int actorMagic) {
      return this.getLiveDescription(1.0F + (float)actorMagic * 0.017F);
   }

   default String getLiveDescription(int actorMagic, boolean stab) {
      float magicMult = 1.0F + (float)actorMagic * 0.017F;
      float combined = stab ? magicMult * 1.5F : magicMult;
      String base = this.getLiveDescription(combined);
      return stab ? base + " (1.5x STAB)" : base;
   }

   default String getLiveDescription(int actorMagic, boolean stab, boolean frozen) {
      float magicMult = 1.0F + (float)actorMagic * 0.017F;
      float combined = (stab ? magicMult * 1.5F : magicMult) * (frozen ? 0.5F : 1.0F);
      String base = this.getLiveDescription(combined);
      String suffix = (stab ? " (1.5x STAB)" : "") + (frozen ? " (Frozen 50%)" : "");
      return base + suffix;
   }

   default String getLiveDescription(float magicMult) {
      int mn = this.getMinVal();
      int mx = this.getMaxVal();
      int ex = this.getExtra();
      float ds = damageMult() * magicMult;
      int dlo = (int)((float)mn * ds);
      int dhi = (int)((float)mx * ds);
      String dr = dlo == dhi ? String.valueOf(dlo) : dlo + "\u2013" + dhi;
      float hs = healMult() * magicMult;
      int hlo = (int)((float)mn * hs);
      int hhi = (int)((float)mx * hs);
      String hr = hlo != hhi && mn != 999 ? hlo + "\u2013" + hhi : (mn == 999 ? "full" : String.valueOf(hlo));
      Affinity[] affs = this.getAffinities();
      String affSuffix = "";
      if (affs != null && affs.length > 0) {
         StringBuilder sb = new StringBuilder();

         for (Affinity a : affs) {
            if (sb.length() > 0) {
               sb.append(", ");
            }

            sb.append(a.displayName);
         }

         affSuffix = " " + sb;
      }
      return switch (this.getType()) {
         case DAMAGE -> dr + affSuffix + " damage";
         case BYPASS -> dr + affSuffix + " damage, ignores armor";
         case FREEZE -> dr + affSuffix + " damage + freeze " + ex + " turns";
         case MULTI -> ex + " hits of " + dr + affSuffix;
         case AOE -> ex + " hits of " + dr + affSuffix + " to ALL enemies";
         case BURN -> dr + " fire damage";
         case HEAL -> mn == 999 ? "Fully restores HP" : "Restores " + hr + " HP";
         case MAGNET -> {
            float ts = tpMult() * magicMult * 0.5F;
            int tlo = (int)((float)mn * ts);
            int thi = (int)((float)mx * ts);
            String tr = tlo == thi ? String.valueOf(tlo) : tlo + "\u2013" + thi;
            yield "Gain " + tr + " TP";
         }
         case SHIELD -> mn + "% damage reduction for " + mx + " turns";
         case BUFF -> "Attack \u00d7" + (ex > 0 ? String.format("%.1f", (float)ex / 100.0F) : "1.5") + " for " + mn + " turns";
         case WEAKEN -> SoulMageSkill.lowersDefense(this)
         ? "Lowers enemy defense by " + ex + "% for " + mn + " turns"
         : "Weaken enemy attack by " + ex + "% for " + mn + " turns";
         case SLEEP -> ex + "% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn)";
         case CONFUSE -> "Confuse enemy for " + mn + " turns";
         case SPARE, RAISE_THRESHOLD -> "Raise spare threshold by " + ex + "%";
         case PACIFY -> this.getEffectDescription();
         default -> this.getEffectDescription();
      };
   }

   default String getLiveDescription() {
      return this.getLiveDescription(1.0F);
   }

   default String getContextDescription() {
      String base = this.getEffectDescription();
      if (base != null && !base.isEmpty()) {
         int mn = this.getMinVal();
         int mx = this.getMaxVal();
         if (mn == 999) {
            return base;
         } else {
            SkillType t = this.getType();

            float scale = switch (t) {
               case DAMAGE, BYPASS, FREEZE, MULTI, AOE, BURN -> damageMult();
               case HEAL -> healMult();
               case MAGNET -> tpMult() * 0.5F;
               default -> 0.0F;
            };
            if (scale <= 0.0F) {
               return base;
            } else {
               int lo = (int)((float)mn * scale);
               int hi = (int)((float)mx * scale);
               String liveRange = lo == hi ? String.valueOf(lo) : lo + "\u2013" + hi;
               return base.matches("(?s).*\\d+\\s*[\u2013-]\\s*\\d+.*") ? base.replaceFirst("\\d+\\s*[\u2013-]\\s*\\d+", liveRange) : base;
            }
         }
      } else {
         return base;
      }
   }

   default int rollScaled(Random rng) {
      int raw = this.roll(rng);
      if (raw == 999) {
         return raw;
      } else {
         return switch (this.getType()) {
            case DAMAGE, BYPASS, FREEZE, MULTI, AOE, BURN -> (int)((float)raw * damageMult() + SkillBonus.magicFlat);
            case HEAL -> (int)((float)raw * healMult());
            case MAGNET -> (int)((float)raw * tpMult());
            default -> raw;
         };
      }
   }
}
