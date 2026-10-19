package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class RogueCards {
   public static final String[] STAT_IDS = new String[]{"offense", "defense", "magic", "iq", "vitality", "luck"};
   public static final String[] STAT_NAMES = new String[]{"Offense", "Defense", "Magic", "IQ", "Vitality", "Luck"};
   public static final String[] STAT_ICONS = new String[]{"\u2694", "\u26e8", "\u2726", "\u270e", "\u2764", "\u2618"};
   public static final int[] STAT_COLORS = new int[]{-43691, -11167233, -5605377, -8892, -11145626, -30499};
   public static final int RISK_LOSS = 4;
   public static final int RISK_GAIN = 7;
   public static final List<RogueCards.Unique> UNIQUES = List.of(
      new RogueCards.Unique("SECOND_WIND", "Second Wind", "Once per battle, a hit that would down you leaves you at 25% HP instead."),
      new RogueCards.Unique("TP_SURGE", "TP Surge", "Start every battle with 40 TP."),
      new RogueCards.Unique("TOUGH_SKIN", "Tough Skin", "+5 Defense, right away."),
      new RogueCards.Unique("DOUBLE_EDGE", "Double Edge", "BASH deals double damage, but 10% of it comes back at you."),
      new RogueCards.Unique("VAMPIRE_FANG", "Vampire Fang", "BASH heals you for 15% of the damage it deals."),
      new RogueCards.Unique("TP_SIPHON", "TP Siphon", "Every BASH that lands gives you 6 TP."),
      new RogueCards.Unique("SCHOLAR", "Scholar", "+25% EXP for the rest of the run."),
      new RogueCards.Unique("BOUNTY", "Bounty", "+1 Soul Shard for every battle you win from now on."),
      new RogueCards.Unique("LUCKY_DRAW", "Lucky Draw", "Every hand from now on has one more card to choose from.")
   );
   private static Map<String, SkillTreeDefs.GlobalNode> allNodes;

   private RogueCards() {
   }

   public static RogueCards.Unique unique(String key) {
      for (RogueCards.Unique u : UNIQUES) {
         if (u.key().equals(key)) {
            return u;
         }
      }

      return null;
   }

   private static synchronized Map<String, SkillTreeDefs.GlobalNode> nodes() {
      if (allNodes == null) {
         Map m = new LinkedHashMap<>();

         for (SkillTreeDefs.GlobalNode gn : SkillTreeDefs.getAllNodesForHuman()) {
            m.putIfAbsent(gn.groupId() + ":" + gn.skill(), gn);
         }

         for (SkillTreeDefs.GlobalNode gn : SkillTreeDefs.getAllNodesForMonster()) {
            m.putIfAbsent(gn.groupId() + ":" + gn.skill(), gn);
         }

         for (SkillTreeDefs.GlobalNode gn : SkillTreeDefs.getRenderNodesForRecruit(Recruit.ORI)) {
            m.putIfAbsent(gn.groupId() + ":" + gn.skill(), gn);
         }

         allNodes = m;
      }

      return allNodes;
   }

   public static BattleSkill skillFor(String groupAndName) {
      SkillTreeDefs.GlobalNode gn = nodes().get(groupAndName);
      return gn == null ? null : SkillTreeDefs.resolveSkill(gn);
   }

   public static String skillName(String groupAndName) {
      int i = groupAndName.indexOf(58);
      return i < 0 ? groupAndName : groupAndName.substring(i + 1);
   }

   public static int tier(String groupAndName) {
      SkillTreeDefs.GlobalNode gn = nodes().get(groupAndName);
      return gn == null ? 99 : gn.node().cost();
   }

   public static int maxTierAt(int level) {
      return 3 + (int)((float)level * 0.6F);
   }

   public static String groupLabel(String group) {
      if (group.startsWith("SOUL_")) {
         try {
            return SoulType.valueOf(group.substring(5)).getDisplayName();
         } catch (IllegalArgumentException var3) {
            return "Soul";
         }
      } else {
         return switch (group) {
            case "PSI" -> "PSI";
            case "LIGHTNER" -> "Lightner";
            case "HUMAN" -> "Human";
            case "MONSTER" -> "Monster";
            case "ORI" -> "Ori";
            default -> group;
         };
      }
   }

   public static List<String> deal(
      Random rng, int size, int level, CharacterRace race, Set<String> unlockedSkills, Collection<String> heldUniques, CharStats stats
   ) {
      List skills = new ArrayList<>();
      int maxTier = maxTierAt(level);

      for (SkillTreeDefs.GlobalNode gn : SkillTreeDefs.getAllNodesFor(race == null ? CharacterRace.HUMAN : race)) {
         if (!unlockedSkills.contains(gn.skill())
            && gn.node().cost() <= maxTier
            && SkillTreeDefs.levelRequiredFor(gn.skill()) <= level
            && SkillTreeDefs.resolveSkill(gn) != null) {
            skills.add(gn.groupId() + ":" + gn.skill());
         }
      }

      List uniques = new ArrayList<>();

      for (RogueCards.Unique u : UNIQUES) {
         if (!heldUniques.contains(u.key())) {
            uniques.add(u.key());
         }
      }

      List hand = new ArrayList<>();
      Set statsInHand = new HashSet<>();
      int guard = 0;

      while (hand.size() < size && guard++ < 200) {
         float roll = rng.nextFloat();
         String card = null;
         if (roll < 0.45F && !skills.isEmpty()) {
            card = "SKILL:" + skills.get(rng.nextInt(skills.size()));
         } else if (roll < 0.57F && !uniques.isEmpty()) {
            card = "UNIQUE:" + uniques.get(rng.nextInt(uniques.size()));
         } else if (roll < 0.65F) {
            int lose = rng.nextInt(6);
            int gain = rng.nextInt(6);
            if (lose != gain && stats.valueFor(lose) >= 4 && !stats.isMaxed(gain)) {
               card = "RISK:" + STAT_IDS[lose] + ":" + STAT_IDS[gain];
            }
         } else {
            int s = rng.nextInt(6);
            if (stats.isMaxed(5) && s == 5 || !statsInHand.add(STAT_IDS[s])) {
               continue;
            }

            float a = rng.nextFloat();
            int amount = a < 0.35F ? 2 : (a < 0.7F ? 3 : (a < 0.9F ? 4 : 6));
            card = "STAT:" + STAT_IDS[s] + ":" + amount;
         }

         if (card != null && !hand.contains(card)) {
            hand.add(card);
         }
      }

      return hand;
   }

   public static List<String> dealOri(Random rng, int size, int level, Set<String> unlockedSkills, CharStats stats) {
      List own = new ArrayList<>();
      List other = new ArrayList<>();
      Set ownKeys = new HashSet<>();
      int maxTier = maxTierAt(level);

      for (SkillTreeDefs.GlobalNode gn : SkillTreeDefs.getRenderNodesForRecruit(Recruit.ORI)) {
         if (!unlockedSkills.contains(gn.skill())
            && gn.node().cost() <= maxTier + 1
            && SkillTreeDefs.levelRequiredFor(gn.skill()) <= level
            && SkillTreeDefs.resolveSkill(gn) != null) {
            String key = gn.groupId() + ":" + gn.skill();
            if (ownKeys.add(key)) {
               own.add(key);
            }
         }
      }

      Set seenNames = new HashSet<>(unlockedSkills);

      for (SkillTreeDefs.GlobalNode gnx : SkillTreeDefs.getRenderNodesForRecruit(Recruit.ORI)) {
         seenNames.add(gnx.skill());
      }

      for (SkillTreeDefs.GlobalNode gnx : SkillTreeDefs.getAllNodesForHuman()) {
         String key = gnx.groupId() + ":" + gnx.skill();
         if (!ownKeys.contains(key)
            && seenNames.add(gnx.skill())
            && !SkillTreeDefs.isStatNode(gnx.skill())
            && gnx.node().cost() <= maxTier
            && SkillTreeDefs.levelRequiredFor(gnx.skill()) <= level) {
            BattleSkill bs = SkillTreeDefs.resolveSkill(gnx);
            if (bs != null && !(bs instanceof MonsterSkill)) {
               other.add(key);
            }
         }
      }

      Collections.shuffle(own, rng);
      Collections.shuffle(other, rng);
      List hand = new ArrayList<>();
      Set statsInHand = new HashSet<>();
      String first = pickSkill(rng, own, other);
      if (first != null) {
         hand.add("SKILL:" + first);
      }

      int guard = 0;

      while (hand.size() < size && guard++ < 200) {
         float roll = rng.nextFloat();
         String card = null;
         if (!(roll < 0.4F) || own.isEmpty() && other.isEmpty()) {
            if (roll < 0.45F) {
               int lose = rng.nextInt(6);
               int gain = rng.nextInt(6);
               if (lose != gain && stats.valueFor(lose) >= 4 && !stats.isMaxed(gain)) {
                  card = "RISK:" + STAT_IDS[lose] + ":" + STAT_IDS[gain];
               }
            } else {
               int s = rng.nextInt(6);
               if (stats.isMaxed(5) && s == 5 || !statsInHand.add(STAT_IDS[s])) {
                  continue;
               }

               float a = rng.nextFloat();
               int amount = a < 0.35F ? 2 : (a < 0.7F ? 3 : (a < 0.9F ? 4 : 6));
               card = "STAT:" + STAT_IDS[s] + ":" + amount;
            }
         } else {
            card = "SKILL:" + pickSkill(rng, own, other);
         }

         if (card != null && !hand.contains(card)) {
            hand.add(card);
         }
      }

      Collections.shuffle(hand, rng);
      return hand;
   }

   private static String pickSkill(Random rng, List<String> own, List<String> other) {
      if (own.isEmpty() || !other.isEmpty() && !(rng.nextFloat() < 0.35F)) {
         return !other.isEmpty() ? other.remove(0) : null;
      } else {
         return own.remove(0);
      }
   }

   public static int statIndex(String id) {
      for (int i = 0; i < STAT_IDS.length; i++) {
         if (STAT_IDS[i].equals(id)) {
            return i;
         }
      }

      return "speed".equals(id) ? 2 : -1;
   }

   public static RogueCards.Info describe(String card) {
      String[] p = card.split(":", 3);

      try {
         String var2 = p[0];
         switch (var2) {
            case "STAT":
               int s = statIndex(p[1]);
               int amount = Integer.parseInt(p[2]);
               return new RogueCards.Info(
                  RogueCards.Kind.STAT,
                  "+" + amount + " " + STAT_NAMES[s],
                  "Stat Boost",
                  "Raises your " + STAT_NAMES[s] + " by " + amount + " for the rest of the run.",
                  STAT_ICONS[s],
                  STAT_COLORS[s]
               );
            case "RISK":
               int lose = statIndex(p[1]);
               int gain = statIndex(p[2]);
               return new RogueCards.Info(
                  RogueCards.Kind.RISK,
                  "+7 " + STAT_NAMES[gain] + " / -4 " + STAT_NAMES[lose],
                  "Risky Trade",
                  "Lose 4 " + STAT_NAMES[lose] + " to gain 7 " + STAT_NAMES[gain] + ".",
                  "\u21c5",
                  -35004
               );
            case "SKILL":
               String key = p[1] + ":" + p[2];
               BattleSkill skill = skillFor(key);
               if (skill != null) {
                  int var10000;
                  label49: {
                     if (skill instanceof SoulMageSkill sms && sms.getSoul() != null) {
                        var10000 = sms.getSoul().getColor() | 0xFF000000;
                        break label49;
                     }

                     var10000 = skill.getType().color() | 0xFF000000;
                  }

                  int color = var10000;
                  String desc = skill.getContextDescription() + " (" + skill.getTpCost() + " TP)";
                  return new RogueCards.Info(RogueCards.Kind.SKILL, skill.getDisplayName(), "Skill - " + groupLabel(p[1]), desc, "\u2727", color);
               }
               break;
            case "UNIQUE":
               RogueCards.Unique u = unique(p[1]);
               if (u != null) {
                  return new RogueCards.Info(RogueCards.Kind.UNIQUE, u.title(), "Passive", u.desc(), "\u2605", -22016);
               }
         }
      } catch (RuntimeException var8) {
      }

      return new RogueCards.Info(RogueCards.Kind.UNKNOWN, "???", "", "", "?", -5592406);
   }

   public static record Info(RogueCards.Kind kind, String title, String subtitle, String desc, String icon, int color) {
   }

   public static enum Kind {
      STAT,
      RISK,
      SKILL,
      UNIQUE,
      UNKNOWN;
   }

   public static record Unique(String key, String title, String desc) {
   }
}
