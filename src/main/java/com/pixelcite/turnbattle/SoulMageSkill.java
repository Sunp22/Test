package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum SoulMageSkill implements BattleSkill {
   STRUGGLE("Struggle", 5, 1, SkillType.DAMAGE, 4, 12, 0),
   DET_STRIKE("Crimson Strike", 14, 1, SkillType.BURN, 20, 34, 40),
   PER_STRIKE("Enduring Blow", 14, 1, SkillType.DAMAGE, 15, 44, 0),
   INT_STRIKE("Resolute Slash", 13, 1, SkillType.BYPASS, 18, 27, 0),
   PAT_STRIKE("Tranquil Palm", 12, 1, SkillType.DAMAGE, 12, 20, 3),
   JUS_STRIKE("Retaliation Fist", 14, 1, SkillType.DAMAGE, 20, 39, 0),
   BRA_STRIKE("Courageous Charge", 12, 1, SkillType.DAMAGE, 24, 39, 0),
   KIN_STRIKE("Gentle Touch", 11, 1, SkillType.DAMAGE, 9, 18, 0),
   DET_PULSE("Soul Burst", 18, 3, SkillType.BYPASS, 24, 39, 0),
   PER_PULSE("Steadfast Bolt", 17, 3, SkillType.BYPASS, 19, 34, 0),
   INT_PULSE("True Strike", 16, 3, SkillType.BYPASS, 21, 31, 0),
   PAT_PULSE("Calm Channel", 16, 5, SkillType.BYPASS, 18, 32, 0),
   JUS_PULSE("Wrathful Bolt", 20, 3, SkillType.BYPASS, 27, 53, 0),
   BRA_PULSE("Bold Rush", 17, 3, SkillType.BYPASS, 21, 37, 0),
   KIN_PULSE("Nurturing Ray", 15, 3, SkillType.BYPASS, 13, 25, 0),
   DET_MEND("Last Stand Mend", 22, 1, SkillType.HEAL, 55, 55, 0),
   PER_MEND("Endurance Mend", 22, 1, SkillType.HEAL, 55, 90, 0),
   INT_MEND("Resolve Mend", 22, 1, SkillType.HEAL, 60, 85, 0),
   PAT_MEND("Patient Mend", 20, 1, SkillType.HEAL, 70, 110, 0),
   JUS_MEND("Justice Mend", 24, 1, SkillType.HEAL, 50, 80, 0),
   BRA_MEND("Battle Mend", 20, 1, SkillType.HEAL, 55, 85, 0),
   KIN_MEND("Soulheal", 22, 1, SkillType.HEAL, 70, 120, 0),
   DET_BARRIER("Iron Will", 28, 5, SkillType.SHIELD, 50, 3, 0),
   PER_BARRIER("Survivor's Guard", 26, 5, SkillType.SHIELD, 40, 4, 0),
   INT_BARRIER("Noble Ward", 26, 5, SkillType.SHIELD, 45, 3, 0),
   PAT_BARRIER("Stillness Veil", 24, 5, SkillType.SHIELD, 35, 5, 0),
   JUS_BARRIER("Righteous Aegis", 28, 5, SkillType.SHIELD, 40, 3, 0),
   BRA_BARRIER("Vanguard Shield", 28, 5, SkillType.SHIELD, 55, 2, 0),
   KIN_BARRIER("Shared Ward", 26, 5, SkillType.SHIELD, 40, 3, 0),
   DET_SURGE("Crimson Surge", 42, 9, SkillType.AOE, 14, 26, 3),
   PER_SURGE("Resilient Surge", 40, 9, SkillType.AOE, 12, 23, 3),
   INT_SURGE("Righteous Wave", 40, 9, SkillType.AOE, 13, 24, 3),
   PAT_SURGE("Serenity Wave", 40, 9, SkillType.AOE, 19, 33, 2),
   JUS_SURGE("Judgment Wave", 44, 9, SkillType.AOE, 12, 20, 4),
   BRA_SURGE("Blazing Rush", 44, 9, SkillType.BURN, 13, 23, 50),
   KIN_SURGE("Healing Wave", 38, 9, SkillType.AOE, 9, 17, 2),
   DET_BIND("Last Gasp", 28, 11, SkillType.SLEEP, 2, 2, 80),
   PER_BIND("Endure", 30, 11, SkillType.FREEZE, 23, 41, 2),
   INT_BIND("Binding Law", 28, 11, SkillType.CONFUSE, 4, 4, 0),
   PAT_BIND("Lull", 28, 11, SkillType.SLEEP, 3, 3, 90),
   JUS_BIND("Judgment Lock", 34, 11, SkillType.FREEZE, 24, 44, 3),
   BRA_BIND("War Cry", 28, 11, SkillType.CONFUSE, 3, 3, 0),
   KIN_BIND("Gentle Reprieve", 26, 11, SkillType.SLEEP, 2, 2, 75),
   DET_CHANNEL("Willpower Draw", 0, 15, SkillType.MAGNET, 35, 55, 0),
   PER_CHANNEL("Grit Boost", 30, 15, SkillType.BUFF, 3, 3, 0),
   INT_CHANNEL("Weaken Strike", 28, 15, SkillType.WEAKEN, 4, 4, 25),
   PAT_CHANNEL("Soul Recharge", 0, 15, SkillType.MAGNET, 40, 60, 0),
   JUS_CHANNEL("Power Surge", 32, 15, SkillType.BUFF, 2, 2, 0),
   BRA_CHANNEL("Battle Cry", 30, 15, SkillType.BUFF, 3, 3, 0),
   KIN_CHANNEL("Weaken Aura", 26, 15, SkillType.WEAKEN, 5, 5, 30),
   DET_APEX("Soul Overdrive", 65, 21, SkillType.MULTI, 47, 74, 3),
   PER_APEX("Undying Barrage", 65, 21, SkillType.MULTI, 35, 55, 4),
   INT_APEX("True Judgment", 62, 21, SkillType.BYPASS, 97, 132, 0),
   PAT_APEX("Eternal Flow", 60, 21, SkillType.MULTI, 69, 104, 2),
   JUS_APEX("Final Verdict", 62, 21, SkillType.DAMAGE, 136, 217, 0),
   BRA_APEX("Heroic Finale", 68, 21, SkillType.AOE, 22, 35, 4),
   KIN_APEX("Soul Radiance", 70, 21, SkillType.MULTI, 49, 78, 3),
   DET_STRIKE_P("Crimson Strike+", 19, 16, SkillType.BURN, 28, 48, 40),
   DET_STRIKE_PP("Crimson Strike++", 24, 34, SkillType.BURN, 87, 157, 40),
   PER_STRIKE_P("Enduring Blow+", 19, 16, SkillType.DAMAGE, 22, 60, 0),
   PER_STRIKE_PP("Enduring Blow++", 24, 34, SkillType.DAMAGE, 72, 200, 0),
   INT_STRIKE_P("Resolute Slash+", 18, 16, SkillType.BYPASS, 25, 37, 0),
   INT_STRIKE_PP("Resolute Slash++", 22, 34, SkillType.BYPASS, 56, 87, 0),
   PAT_STRIKE_P("Tranquil Palm+", 16, 16, SkillType.DAMAGE, 29, 48, 3),
   PAT_STRIKE_PP("Tranquil Palm++", 20, 34, SkillType.DAMAGE, 95, 159, 3),
   JUS_STRIKE_P("Retaliation Fist+", 19, 16, SkillType.DAMAGE, 28, 54, 0),
   JUS_STRIKE_PP("Retaliation Fist++", 24, 34, SkillType.DAMAGE, 91, 181, 0),
   BRA_STRIKE_P("Courageous Charge+", 16, 16, SkillType.DAMAGE, 32, 54, 0),
   BRA_STRIKE_PP("Courageous Charge++", 20, 34, SkillType.DAMAGE, 95, 159, 0),
   KIN_STRIKE_P("Gentle Touch+", 15, 16, SkillType.DAMAGE, 28, 48, 0),
   KIN_STRIKE_PP("Gentle Touch++", 19, 34, SkillType.DAMAGE, 89, 160, 0),
   DET_PULSE_P("Soul Burst+", 24, 18, SkillType.BYPASS, 40, 66, 0),
   DET_PULSE_PP("Soul Burst++", 31, 36, SkillType.BYPASS, 97, 162, 0),
   PER_PULSE_P("Steadfast Bolt+", 23, 18, SkillType.BYPASS, 37, 67, 0),
   PER_PULSE_PP("Steadfast Bolt++", 29, 36, SkillType.BYPASS, 89, 160, 0),
   INT_PULSE_P("True Strike+", 22, 18, SkillType.BYPASS, 41, 62, 0),
   INT_PULSE_PP("True Strike++", 27, 36, SkillType.BYPASS, 99, 143, 0),
   PAT_PULSE_P("Calm Channel+", 22, 18, SkillType.BYPASS, 36, 66, 0),
   PAT_PULSE_PP("Calm Channel++", 27, 36, SkillType.BYPASS, 84, 158, 0),
   JUS_PULSE_P("Wrathful Bolt+", 27, 18, SkillType.BYPASS, 37, 75, 0),
   JUS_PULSE_PP("Wrathful Bolt++", 34, 36, SkillType.BYPASS, 90, 180, 0),
   BRA_PULSE_P("Bold Rush+", 23, 18, SkillType.BYPASS, 38, 66, 0),
   BRA_PULSE_PP("Bold Rush++", 29, 36, SkillType.BYPASS, 91, 158, 0),
   KIN_PULSE_P("Nurturing Ray+", 20, 18, SkillType.BYPASS, 33, 66, 0),
   KIN_PULSE_PP("Nurturing Ray++", 26, 36, SkillType.BYPASS, 84, 155, 0),
   DET_SURGE_P("Crimson Surge+", 57, 26, SkillType.AOE, 27, 48, 3),
   DET_SURGE_PP("Crimson Surge++", 71, 44, SkillType.AOE, 60, 104, 3),
   PER_SURGE_P("Resilient Surge+", 54, 26, SkillType.AOE, 25, 48, 3),
   PER_SURGE_PP("Resilient Surge++", 68, 44, SkillType.AOE, 53, 105, 3),
   INT_SURGE_P("Righteous Wave+", 54, 26, SkillType.AOE, 25, 48, 3),
   INT_SURGE_PP("Righteous Wave++", 68, 44, SkillType.AOE, 55, 103, 3),
   PAT_SURGE_P("Serenity Wave+", 54, 26, SkillType.AOE, 39, 71, 2),
   PAT_SURGE_PP("Serenity Wave++", 68, 44, SkillType.AOE, 84, 152, 2),
   JUS_SURGE_P("Judgment Wave+", 59, 26, SkillType.AOE, 21, 37, 4),
   JUS_SURGE_PP("Judgment Wave++", 75, 44, SkillType.AOE, 45, 82, 4),
   BRA_SURGE_P("Blazing Rush+", 59, 26, SkillType.BURN, 124, 223, 50),
   BRA_SURGE_PP("Blazing Rush++", 75, 44, SkillType.BURN, 281, 482, 50),
   KIN_SURGE_P("Healing Wave+", 51, 26, SkillType.AOE, 38, 67, 2),
   KIN_SURGE_PP("Healing Wave++", 65, 44, SkillType.AOE, 82, 149, 2),
   PER_BIND_P("Endure+", 40, 28, SkillType.FREEZE, 97, 173, 2),
   PER_BIND_PP("Endure++", 51, 46, SkillType.FREEZE, 202, 366, 2),
   JUS_BIND_P("Judgment Lock+", 46, 28, SkillType.FREEZE, 104, 189, 3),
   JUS_BIND_PP("Judgment Lock++", 58, 46, SkillType.FREEZE, 217, 398, 3),
   DET_APEX_P("Soul Overdrive+", 88, 40, SkillType.MULTI, 120, 186, 3),
   DET_APEX_PP("Soul Overdrive++", 110, 60, SkillType.MULTI, 225, 350, 3),
   PER_APEX_P("Undying Barrage+", 88, 40, SkillType.MULTI, 89, 141, 4),
   PER_APEX_PP("Undying Barrage++", 110, 60, SkillType.MULTI, 168, 264, 4),
   INT_APEX_P("True Judgment+", 84, 40, SkillType.BYPASS, 207, 283, 0),
   INT_APEX_PP("True Judgment++", 105, 60, SkillType.BYPASS, 387, 528, 0),
   PAT_APEX_P("Eternal Flow+", 81, 40, SkillType.MULTI, 173, 261, 2),
   PAT_APEX_PP("Eternal Flow++", 102, 60, SkillType.MULTI, 326, 488, 2),
   JUS_APEX_P("Final Verdict+", 84, 40, SkillType.DAMAGE, 343, 548, 0),
   JUS_APEX_PP("Final Verdict++", 105, 60, SkillType.DAMAGE, 639, 1024, 0),
   BRA_APEX_P("Heroic Finale+", 92, 40, SkillType.AOE, 53, 89, 4),
   BRA_APEX_PP("Heroic Finale++", 116, 60, SkillType.AOE, 101, 168, 4),
   KIN_APEX_P("Soul Radiance+", 94, 40, SkillType.MULTI, 124, 198, 3),
   KIN_APEX_PP("Soul Radiance++", 119, 60, SkillType.MULTI, 231, 370, 3),
   DET_MEND_P("Last Stand Mend+", 30, 18, SkillType.HEAL, 100, 100, 0),
   DET_MEND_PP("Last Stand Mend++", 38, 38, SkillType.HEAL, 200, 200, 0),
   PER_MEND_P("Endurance Mend+", 30, 18, SkillType.HEAL, 100, 160, 0),
   PER_MEND_PP("Endurance Mend++", 38, 38, SkillType.HEAL, 180, 290, 0),
   INT_MEND_P("Resolve Mend+", 30, 18, SkillType.HEAL, 110, 150, 0),
   INT_MEND_PP("Resolve Mend++", 38, 38, SkillType.HEAL, 190, 250, 0),
   PAT_MEND_P("Patient Mend+", 28, 18, SkillType.HEAL, 130, 200, 0),
   PAT_MEND_PP("Patient Mend++", 36, 38, SkillType.HEAL, 240, 380, 0),
   JUS_MEND_P("Justice Mend+", 32, 18, SkillType.HEAL, 90, 140, 0),
   JUS_MEND_PP("Justice Mend++", 40, 38, SkillType.HEAL, 160, 250, 0),
   BRA_MEND_P("Battle Mend+", 28, 18, SkillType.HEAL, 100, 150, 0),
   BRA_MEND_PP("Battle Mend++", 36, 38, SkillType.HEAL, 180, 275, 0),
   KIN_MEND_P("Soulheal+", 30, 18, SkillType.HEAL, 130, 210, 0),
   KIN_MEND_PP("Soulheal++", 38, 38, SkillType.HEAL, 240, 400, 0);

   private final String d;
   private final int tp;
   private final int lr;
   private final int mn;
   private final int mx;
   private final int ex;
   private final SkillType t;
   public static final String[] SLOT_NAMES = new String[]{"Strike", "Pulse", "Mend", "Barrier", "Surge", "Bind", "Channel", "Apex"};

   private SoulMageSkill(String d, int tp, int lr, SkillType t, int mn, int mx, int ex) {
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
   public Affinity[] getAffinities() {
      return switch (this) {
         case STRUGGLE -> new Affinity[0];
         case DET_STRIKE, DET_PULSE, DET_APEX, DET_STRIKE_P, DET_STRIKE_PP, DET_PULSE_P, DET_PULSE_PP, DET_SURGE_P, DET_SURGE_PP, DET_APEX_P, DET_APEX_PP -> new Affinity[]{
         Affinity.FIRE
      };
         case PER_STRIKE, PER_PULSE, PER_APEX, PER_STRIKE_P, PER_STRIKE_PP, PER_PULSE_P, PER_PULSE_PP, PER_SURGE_P, PER_SURGE_PP, PER_BIND_P, PER_BIND_PP, PER_APEX_P, PER_APEX_PP -> new Affinity[]{
         Affinity.IMPACT
      };
         case INT_STRIKE, INT_PULSE, INT_APEX, INT_STRIKE_P, INT_STRIKE_PP, INT_PULSE_P, INT_PULSE_PP, INT_SURGE_P, INT_SURGE_PP, INT_APEX_P, INT_APEX_PP -> new Affinity[]{
         Affinity.SLASH
      };
         case PAT_STRIKE, PAT_PULSE, PAT_APEX, PAT_STRIKE_P, PAT_STRIKE_PP, PAT_PULSE_P, PAT_PULSE_PP, PAT_SURGE_P, PAT_SURGE_PP, PAT_APEX_P, PAT_APEX_PP -> new Affinity[]{
         Affinity.ICE
      };
         case JUS_STRIKE, JUS_PULSE, JUS_APEX, JUS_STRIKE_P, JUS_STRIKE_PP, JUS_PULSE_P, JUS_PULSE_PP, JUS_SURGE_P, JUS_SURGE_PP, JUS_BIND_P, JUS_BIND_PP, JUS_APEX_P, JUS_APEX_PP -> new Affinity[]{
         Affinity.ELECTRIC
      };
         case BRA_STRIKE, BRA_PULSE, BRA_APEX, BRA_STRIKE_P, BRA_STRIKE_PP, BRA_PULSE_P, BRA_PULSE_PP, BRA_SURGE_P, BRA_SURGE_PP, BRA_APEX_P, BRA_APEX_PP -> new Affinity[]{
         Affinity.LIGHT
      };
         case KIN_STRIKE, KIN_PULSE, KIN_APEX, KIN_STRIKE_P, KIN_STRIKE_PP, KIN_PULSE_P, KIN_PULSE_PP, KIN_SURGE_P, KIN_SURGE_PP, KIN_APEX_P, KIN_APEX_PP -> new Affinity[]{
         Affinity.LIGHT
      };
         default -> new Affinity[0];
      };
   }

   @Override
   public String getEffectDescription() {
      return switch (this) {
         case STRUGGLE -> "Deals 10\u201331 damage. Roll your soul to unlock true power.";
         case DET_STRIKE -> "Deals 52\u201389 fire damage. Fueled by Determination.";
         case PER_STRIKE -> "Deals 39\u2013115 damage. Hits harder the lower your HP.";
         case INT_STRIKE -> "Deals 47\u201370 damage, ignores armor.";
         case PAT_STRIKE -> "Deals 31\u201352 damage and heals you for 3 HP.";
         case JUS_STRIKE -> "Deals 52\u2013102 damage. Bonus damage when below 50% HP.";
         case BRA_STRIKE -> "Deals 63\u2013102 damage up front.";
         case KIN_STRIKE -> "Deals 23\u201347 damage. Restores 15% of damage as HP.";
         case DET_PULSE -> "Deals 63\u2013102 damage, ignores all armor.";
         case PER_PULSE -> "Deals 49\u201389 damage, ignores armor.";
         case INT_PULSE -> "Deals 55\u201381 damage, ignores armor. Reliable and true.";
         case PAT_PULSE -> "Deals 47\u201384 damage, ignores armor.";
         case JUS_PULSE -> "Deals 70\u2013139 damage, ignores armor. Enhanced below 50% HP.";
         case BRA_PULSE -> "Deals 55\u201397 damage, ignores armor.";
         case KIN_PULSE -> "Deals 34\u201365 damage, ignores armor. Restores 15% as HP.";
         case DET_MEND -> "Restores 82 HP. Full heal if at 25% HP or less.";
         case PER_MEND -> "Restores 82\u2013135 HP.";
         case INT_MEND -> "Restores 90\u2013127 HP.";
         case PAT_MEND -> "Restores 105\u2013165 HP.";
         case JUS_MEND -> "Restores 75\u2013120 HP.";
         case BRA_MEND -> "Restores 82\u2013127 HP. 50% chance to not use TP.";
         case KIN_MEND -> "Restores 105\u2013180 HP on a chosen ally.";
         case DET_BARRIER -> "50% damage reduction for 3 turns.";
         case PER_BARRIER -> "40% damage reduction for 4 turns.";
         case INT_BARRIER -> "45% damage reduction for 3 turns.";
         case PAT_BARRIER -> "35% damage reduction for 5 turns.";
         case JUS_BARRIER -> "40% damage reduction for 3 turns.";
         case BRA_BARRIER -> "55% damage reduction for 2 turns.";
         case KIN_BARRIER -> "40% damage reduction for 3 turns for you.";
         case DET_SURGE -> "3 hits of 36\u201368 to ALL enemies.";
         case PER_SURGE -> "3 hits of 31\u201360 to ALL enemies.";
         case INT_SURGE -> "3 hits of 34\u201363 to ALL enemies.";
         case PAT_SURGE -> "2 hits of 49\u201386 to ALL enemies.";
         case JUS_SURGE -> "4 hits of 31\u201352 to ALL enemies.";
         case BRA_SURGE -> "4 hits of 34\u201360 to ALL enemies.";
         case KIN_SURGE -> "2 hits of 23\u201344 to ALL enemies. Restores 15% of total damage as HP.";
         case DET_BIND -> "80% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn).";
         case PER_BIND -> "Deals 60\u2013107 damage + freezes enemy 2 turns.";
         case INT_BIND -> "Confuses the enemy for 4 turns.";
         case PAT_BIND -> "90% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn).";
         case JUS_BIND -> "Deals 63\u2013115 damage + freezes enemy 3 turns.";
         case BRA_BIND -> "Confuses the enemy for 3 turns.";
         case KIN_BIND -> "75% chance to put the enemy to sleep (asleep for at least a turn, then likelier to wake each turn).";
         case DET_CHANNEL -> "Draw 91\u2013144 TP from willpower alone.";
         case PER_CHANNEL -> "Boost your attack for 3 turns.";
         case INT_CHANNEL -> "Lowers enemy defense by 25% for 4 turns, so they take 25% more damage.";
         case PAT_CHANNEL -> "Draw 105\u2013157 TP through calm focus.";
         case JUS_CHANNEL -> "Boost your attack for 2 turns (strong).";
         case BRA_CHANNEL -> "Boost your attack for 3 turns. 50% chance to not use TP.";
         case KIN_CHANNEL -> "Lowers enemy defense by 30% for 5 turns, so they take 30% more damage.";
         case DET_APEX -> "3 rapid hits of 123\u2013194. Soul at full burn.";
         case PER_APEX -> "4 hits of 91\u2013144. Never stopped, never will be.";
         case INT_APEX -> "Single hit of 254\u2013346, ignores all armor.";
         case PAT_APEX -> "2 massive hits of 181\u2013273.";
         case JUS_APEX -> "Deals 357\u2013569 damage. Powered by justice.";
         case BRA_APEX -> "4 hits of 57\u201391 to ALL enemies.";
         case KIN_APEX -> "3 hits of 128\u2013204 on one target. Restores 15% of total damage as HP.";
         case DET_STRIKE_P -> "Deals 73\u2013126 fire damage. Upgraded.";
         case DET_STRIKE_PP -> "Deals 228\u2013412 fire damage. Maximum power.";
         case PER_STRIKE_P -> "Deals 57\u2013157 damage. Hits harder the lower your HP.";
         case PER_STRIKE_PP -> "Deals 189\u2013525 damage. Hits harder the lower your HP.";
         case INT_STRIKE_P -> "Deals 65\u201397 damage, ignores all armor. Upgraded.";
         case INT_STRIKE_PP -> "Deals 147\u2013228 damage, ignores all armor. Maximum power.";
         case PAT_STRIKE_P -> "Deals 76\u2013126 damage. Upgraded.";
         case PAT_STRIKE_PP -> "Deals 249\u2013417 damage. Maximum power.";
         case JUS_STRIKE_P -> "Deals 73\u2013141 damage. Upgraded.";
         case JUS_STRIKE_PP -> "Deals 238\u2013475 damage. Maximum power.";
         case BRA_STRIKE_P -> "Deals 84\u2013141 damage. Upgraded.";
         case BRA_STRIKE_PP -> "Deals 249\u2013417 damage. Maximum power.";
         case KIN_STRIKE_P -> "Deals 73\u2013126 damage. Upgraded.";
         case KIN_STRIKE_PP -> "Deals 233\u2013420 damage. Maximum power.";
         case DET_PULSE_P -> "Deals 105\u2013173 damage, ignores all armor. Upgraded.";
         case DET_PULSE_PP -> "Deals 254\u2013425 damage, ignores all armor. Maximum power.";
         case PER_PULSE_P -> "Deals 97\u2013175 damage, ignores all armor. Upgraded.";
         case PER_PULSE_PP -> "Deals 233\u2013420 damage, ignores all armor. Maximum power.";
         case INT_PULSE_P -> "Deals 107\u2013162 damage, ignores all armor. Upgraded.";
         case INT_PULSE_PP -> "Deals 259\u2013375 damage, ignores all armor. Maximum power.";
         case PAT_PULSE_P -> "Deals 94\u2013173 damage, ignores all armor. Upgraded.";
         case PAT_PULSE_PP -> "Deals 220\u2013414 damage, ignores all armor. Maximum power.";
         case JUS_PULSE_P -> "Deals 97\u2013196 damage, ignores all armor. Upgraded.";
         case JUS_PULSE_PP -> "Deals 236\u2013472 damage, ignores all armor. Maximum power.";
         case BRA_PULSE_P -> "Deals 99\u2013173 damage, ignores all armor. Upgraded.";
         case BRA_PULSE_PP -> "Deals 238\u2013414 damage, ignores all armor. Maximum power.";
         case KIN_PULSE_P -> "Deals 86\u2013173 damage, ignores all armor. Upgraded.";
         case KIN_PULSE_PP -> "Deals 220\u2013406 damage, ignores armor. Restores 15% as HP. Maximum power.";
         case DET_SURGE_P -> "3 hits of 70\u2013126 to ALL enemies. Upgraded.";
         case DET_SURGE_PP -> "3 hits of 157\u2013273 to ALL enemies. Maximum power.";
         case PER_SURGE_P -> "3 hits of 65\u2013126 to ALL enemies. Upgraded.";
         case PER_SURGE_PP -> "3 hits of 139\u2013275 to ALL enemies. Maximum power.";
         case INT_SURGE_P -> "3 hits of 65\u2013126 to ALL enemies. Upgraded.";
         case INT_SURGE_PP -> "3 hits of 144\u2013270 to ALL enemies. Maximum power.";
         case PAT_SURGE_P -> "2 hits of 102\u2013186 to ALL enemies. Upgraded.";
         case PAT_SURGE_PP -> "2 hits of 220\u2013399 to ALL enemies. Maximum power.";
         case JUS_SURGE_P -> "4 hits of 55\u201397 to ALL enemies. Upgraded.";
         case JUS_SURGE_PP -> "4 hits of 118\u2013215 to ALL enemies. Maximum power.";
         case BRA_SURGE_P -> "Deals 325\u2013585 fire damage. Upgraded.";
         case BRA_SURGE_PP -> "Deals 737\u20131265 fire damage. Maximum power.";
         case KIN_SURGE_P -> "2 hits of 99\u2013175 to ALL enemies. Restores 15% as HP. Upgraded.";
         case KIN_SURGE_PP -> "2 hits of 215\u2013391 to ALL enemies. Restores 15% as HP. Maximum power.";
         case PER_BIND_P -> "Deals 254\u2013454 damage + freezes enemy 2 turns. Upgraded.";
         case PER_BIND_PP -> "Deals 530\u2013960 damage + freezes enemy 2 turns. Maximum power.";
         case JUS_BIND_P -> "Deals 273\u2013496 damage + freezes enemy 3 turns. Upgraded.";
         case JUS_BIND_PP -> "Deals 569\u20131044 damage + freezes enemy 3 turns. Maximum power.";
         case DET_APEX_P -> "3 hits of 315\u2013488. Upgraded.";
         case DET_APEX_PP -> "3 hits of 590\u2013918. Maximum power.";
         case PER_APEX_P -> "4 hits of 233\u2013370. Upgraded.";
         case PER_APEX_PP -> "4 hits of 441\u2013693. Maximum power.";
         case INT_APEX_P -> "Deals 543\u2013742 damage, ignores all armor. Upgraded.";
         case INT_APEX_PP -> "Deals 1015\u20131386 damage, ignores all armor. Maximum power.";
         case PAT_APEX_P -> "2 hits of 454\u2013685. Upgraded.";
         case PAT_APEX_PP -> "2 hits of 855\u20131281. Maximum power.";
         case JUS_APEX_P -> "Deals 900\u20131438 damage. Upgraded.";
         case JUS_APEX_PP -> "Deals 1677\u20132688 damage. Maximum power.";
         case BRA_APEX_P -> "4 hits of 139\u2013233 to ALL enemies. Upgraded.";
         case BRA_APEX_PP -> "4 hits of 265\u2013441 to ALL enemies. Maximum power.";
         case KIN_APEX_P -> "3 hits of 325\u2013519 on one target. Restores 15% as HP. Upgraded.";
         case KIN_APEX_PP -> "3 hits of 606\u2013971 on one target. Restores 15% as HP. Maximum power.";
         case DET_MEND_P -> "Restores 150 HP. Full heal if at 35% HP or less.";
         case DET_MEND_PP -> "Restores 300 HP. Full heal if at 50% HP or less.";
         case PER_MEND_P -> "Restores 150\u2013240 HP.";
         case PER_MEND_PP -> "Restores 270\u2013435 HP.";
         case INT_MEND_P -> "Restores 165\u2013225 HP.";
         case INT_MEND_PP -> "Restores 285\u2013375 HP.";
         case PAT_MEND_P -> "Restores 195\u2013300 HP.";
         case PAT_MEND_PP -> "Restores 360\u2013570 HP.";
         case JUS_MEND_P -> "Restores 135\u2013210 HP.";
         case JUS_MEND_PP -> "Restores 240\u2013375 HP.";
         case BRA_MEND_P -> "Restores 150\u2013225 HP. 50% chance to not use TP.";
         case BRA_MEND_PP -> "Restores 270\u2013412 HP. 50% chance to not use TP.";
         case KIN_MEND_P -> "Restores 195\u2013315 HP on a chosen ally.";
         case KIN_MEND_PP -> "Restores 360\u2013600 HP on a chosen ally.";
      };
   }

   public SoulType getSoul() {
      return switch (this) {
         case STRUGGLE -> null;
         case DET_STRIKE, DET_PULSE, DET_MEND, DET_BARRIER, DET_SURGE, DET_BIND, DET_CHANNEL, DET_APEX, DET_STRIKE_P, DET_STRIKE_PP, DET_PULSE_P, DET_PULSE_PP, DET_SURGE_P, DET_SURGE_PP, DET_APEX_P, DET_APEX_PP, DET_MEND_P, DET_MEND_PP -> SoulType.DETERMINATION;
         case PER_STRIKE, PER_PULSE, PER_MEND, PER_BARRIER, PER_SURGE, PER_BIND, PER_CHANNEL, PER_APEX, PER_STRIKE_P, PER_STRIKE_PP, PER_PULSE_P, PER_PULSE_PP, PER_SURGE_P, PER_SURGE_PP, PER_BIND_P, PER_BIND_PP, PER_APEX_P, PER_APEX_PP, PER_MEND_P, PER_MEND_PP -> SoulType.PERSEVERANCE;
         case INT_STRIKE, INT_PULSE, INT_MEND, INT_BARRIER, INT_SURGE, INT_BIND, INT_CHANNEL, INT_APEX, INT_STRIKE_P, INT_STRIKE_PP, INT_PULSE_P, INT_PULSE_PP, INT_SURGE_P, INT_SURGE_PP, INT_APEX_P, INT_APEX_PP, INT_MEND_P, INT_MEND_PP -> SoulType.INTEGRITY;
         case PAT_STRIKE, PAT_PULSE, PAT_MEND, PAT_BARRIER, PAT_SURGE, PAT_BIND, PAT_CHANNEL, PAT_APEX, PAT_STRIKE_P, PAT_STRIKE_PP, PAT_PULSE_P, PAT_PULSE_PP, PAT_SURGE_P, PAT_SURGE_PP, PAT_APEX_P, PAT_APEX_PP, PAT_MEND_P, PAT_MEND_PP -> SoulType.PATIENCE;
         case JUS_STRIKE, JUS_PULSE, JUS_MEND, JUS_BARRIER, JUS_SURGE, JUS_BIND, JUS_CHANNEL, JUS_APEX, JUS_STRIKE_P, JUS_STRIKE_PP, JUS_PULSE_P, JUS_PULSE_PP, JUS_SURGE_P, JUS_SURGE_PP, JUS_BIND_P, JUS_BIND_PP, JUS_APEX_P, JUS_APEX_PP, JUS_MEND_P, JUS_MEND_PP -> SoulType.JUSTICE;
         case BRA_STRIKE, BRA_PULSE, BRA_MEND, BRA_BARRIER, BRA_SURGE, BRA_BIND, BRA_CHANNEL, BRA_APEX, BRA_STRIKE_P, BRA_STRIKE_PP, BRA_PULSE_P, BRA_PULSE_PP, BRA_SURGE_P, BRA_SURGE_PP, BRA_APEX_P, BRA_APEX_PP, BRA_MEND_P, BRA_MEND_PP -> SoulType.BRAVERY;
         case KIN_STRIKE, KIN_PULSE, KIN_MEND, KIN_BARRIER, KIN_SURGE, KIN_BIND, KIN_CHANNEL, KIN_APEX, KIN_STRIKE_P, KIN_STRIKE_PP, KIN_PULSE_P, KIN_PULSE_PP, KIN_SURGE_P, KIN_SURGE_PP, KIN_APEX_P, KIN_APEX_PP, KIN_MEND_P, KIN_MEND_PP -> SoulType.KINDNESS;
      };
   }

   public int getSlot() {
      return switch (this) {
         case STRUGGLE -> -1;
         case DET_STRIKE, PER_STRIKE, INT_STRIKE, PAT_STRIKE, JUS_STRIKE, BRA_STRIKE, KIN_STRIKE, DET_STRIKE_P, DET_STRIKE_PP, PER_STRIKE_P, PER_STRIKE_PP, INT_STRIKE_P, INT_STRIKE_PP, PAT_STRIKE_P, PAT_STRIKE_PP, JUS_STRIKE_P, JUS_STRIKE_PP, BRA_STRIKE_P, BRA_STRIKE_PP, KIN_STRIKE_P, KIN_STRIKE_PP -> 0;
         case DET_PULSE, PER_PULSE, INT_PULSE, PAT_PULSE, JUS_PULSE, BRA_PULSE, KIN_PULSE, DET_PULSE_P, DET_PULSE_PP, PER_PULSE_P, PER_PULSE_PP, INT_PULSE_P, INT_PULSE_PP, PAT_PULSE_P, PAT_PULSE_PP, JUS_PULSE_P, JUS_PULSE_PP, BRA_PULSE_P, BRA_PULSE_PP, KIN_PULSE_P, KIN_PULSE_PP -> 1;
         case DET_MEND, PER_MEND, INT_MEND, PAT_MEND, JUS_MEND, BRA_MEND, KIN_MEND, DET_MEND_P, DET_MEND_PP, PER_MEND_P, PER_MEND_PP, INT_MEND_P, INT_MEND_PP, PAT_MEND_P, PAT_MEND_PP, JUS_MEND_P, JUS_MEND_PP, BRA_MEND_P, BRA_MEND_PP, KIN_MEND_P, KIN_MEND_PP -> 2;
         case DET_BARRIER, PER_BARRIER, INT_BARRIER, PAT_BARRIER, JUS_BARRIER, BRA_BARRIER, KIN_BARRIER -> 3;
         case DET_SURGE, PER_SURGE, INT_SURGE, PAT_SURGE, JUS_SURGE, BRA_SURGE, KIN_SURGE, DET_SURGE_P, DET_SURGE_PP, PER_SURGE_P, PER_SURGE_PP, INT_SURGE_P, INT_SURGE_PP, PAT_SURGE_P, PAT_SURGE_PP, JUS_SURGE_P, JUS_SURGE_PP, BRA_SURGE_P, BRA_SURGE_PP, KIN_SURGE_P, KIN_SURGE_PP -> 4;
         case DET_BIND, PER_BIND, INT_BIND, PAT_BIND, JUS_BIND, BRA_BIND, KIN_BIND, PER_BIND_P, PER_BIND_PP, JUS_BIND_P, JUS_BIND_PP -> 5;
         case DET_CHANNEL, PER_CHANNEL, INT_CHANNEL, PAT_CHANNEL, JUS_CHANNEL, BRA_CHANNEL, KIN_CHANNEL -> 6;
         case DET_APEX, PER_APEX, INT_APEX, PAT_APEX, JUS_APEX, BRA_APEX, KIN_APEX, DET_APEX_P, DET_APEX_PP, PER_APEX_P, PER_APEX_PP, INT_APEX_P, INT_APEX_PP, PAT_APEX_P, PAT_APEX_PP, JUS_APEX_P, JUS_APEX_PP, BRA_APEX_P, BRA_APEX_PP, KIN_APEX_P, KIN_APEX_PP -> 7;
      };
   }

   public static List<BattleSkill> getAvailable(int level, SoulType soul) {
      List list = new ArrayList<>();
      if (soul == null) {
         list.add(STRUGGLE);
         return list;
      } else {
         for (SoulMageSkill s : values()) {
            if (s != STRUGGLE && s.getSoul() == soul) {
               list.add(s);
            }
         }

         return list;
      }
   }

   public static List<BattleSkill> getAvailable(int level, SoulType soul, Set<String> unlocked) {
      List list = new ArrayList<>();
      if (soul == null) {
         list.add(STRUGGLE);
         return list;
      } else {
         for (SoulMageSkill s : values()) {
            if (s != STRUGGLE && s.getSoul() == soul && unlocked.contains(s.name()) && s.lr <= level) {
               list.add(s);
            }
         }

         return list;
      }
   }

   public static SoulMageSkill[] allForCodex() {
      return values();
   }

   public static boolean lowersDefense(BattleSkill s) {
      return s == INT_CHANNEL || s == KIN_CHANNEL;
   }
}
