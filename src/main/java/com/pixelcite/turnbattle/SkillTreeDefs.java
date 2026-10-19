package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

public class SkillTreeDefs {
   private static final float HUMAN_MONSTER_ANGLE = 0.0F;
   private static final float PSI_ANGLE = 90.0F;
   private static final float LIGHTNER_ANGLE = 180.0F;
   private static final float SOUL_ANGLE = 270.0F;
   private static final float ROW_SPACING = 90.0F;
   private static final SoulType[] SOUL_TRAIT_ORDER = new SoulType[]{
      SoulType.DETERMINATION, SoulType.PERSEVERANCE, SoulType.INTEGRITY, SoulType.PATIENCE, SoulType.JUSTICE, SoulType.BRAVERY, SoulType.KINDNESS
   };
   private static final float HUB_RADIUS = 180.0F;
   private static final Random CLASS_TIE_RNG = new Random();
   private static final List<SkillTreeDefs.Node> PSI_TREE = List.of(
      new SkillTreeDefs.Node("FIRE_A", 4, 0, new String[0], 1, 16729156, "Root"),
      new SkillTreeDefs.Node("FIRE_B", 6, 1, new String[]{"FIRE_A"}, 3, 16729156, "Offense"),
      new SkillTreeDefs.Node("FIRE_G", 6, 2, new String[]{"FIRE_B"}, 8, 16737860, "Offense"),
      new SkillTreeDefs.Node("FIRE_O", 6, 3, new String[]{"FIRE_G"}, 10, 16755234, "Offense"),
      new SkillTreeDefs.Node("ROCKIN_A", 7, 1, new String[]{"FIRE_A"}, 3, 16737792, "Offense"),
      new SkillTreeDefs.Node("ROCKIN_B", 7, 2, new String[]{"ROCKIN_A"}, 10, 16737792, "Offense"),
      new SkillTreeDefs.Node("ROCKIN_G", 7, 3, new String[]{"ROCKIN_B"}, 12, 16746496, "Offense"),
      new SkillTreeDefs.Node("ROCKIN_O", 7, 4, new String[]{"ROCKIN_G"}, 12, 16755200, "Offense"),
      new SkillTreeDefs.Node("STARSTORM_A", 7, 5, new String[]{"ROCKIN_O"}, 14, 16768426, "Offense"),
      new SkillTreeDefs.Node("STARSTORM_O", 7, 6, new String[]{"STARSTORM_A"}, 14, 16777164, "Offense"),
      new SkillTreeDefs.Node("THUNDER_A", 3, 1, new String[]{"FIRE_A"}, 2, 16777028, "Control"),
      new SkillTreeDefs.Node("THUNDER_B", 3, 2, new String[]{"THUNDER_A"}, 3, 16777028, "Control"),
      new SkillTreeDefs.Node("THUNDER_G", 3, 3, new String[]{"THUNDER_B"}, 8, 16777096, "Control"),
      new SkillTreeDefs.Node("THUNDER_O", 3, 4, new String[]{"THUNDER_G"}, 12, 16777147, "Control"),
      new SkillTreeDefs.Node("FREEZE_A", 2, 2, new String[]{"THUNDER_A"}, 2, 4513279, "Control"),
      new SkillTreeDefs.Node("FREEZE_B", 2, 3, new String[]{"FREEZE_A"}, 3, 4513279, "Control"),
      new SkillTreeDefs.Node("FREEZE_G", 2, 4, new String[]{"FREEZE_B"}, 8, 8974079, "Control"),
      new SkillTreeDefs.Node("FREEZE_O", 2, 5, new String[]{"FREEZE_G"}, 12, 12320767, "Control"),
      new SkillTreeDefs.Node("MAGNET", 4, 2, new String[]{"THUNDER_A"}, 3, 4521864, "Control"),
      new SkillTreeDefs.Node("HYPNOSIS_A", 4, 3, new String[]{"MAGNET"}, 3, 11158783, "Control"),
      new SkillTreeDefs.Node("HYPNOSIS_B", 4, 4, new String[]{"HYPNOSIS_A"}, 10, 13404415, "Control"),
      new SkillTreeDefs.Node("BRAINSHOCK", 5, 4, new String[]{"HYPNOSIS_A"}, 10, 16729343, "Control"),
      new SkillTreeDefs.Node("SPY", 5, 2, new String[]{"THUNDER_A"}, 3, 11206621, "Control"),
      new SkillTreeDefs.Node("LIFEUP_A", 1, 1, new String[]{"FIRE_A"}, 1, 4521796, "Support"),
      new SkillTreeDefs.Node("LIFEUP_B", 1, 2, new String[]{"LIFEUP_A"}, 3, 4521796, "Support"),
      new SkillTreeDefs.Node("LIFEUP_G", 1, 3, new String[]{"LIFEUP_B"}, 8, 8978312, "Support"),
      new SkillTreeDefs.Node("LIFEUP_O", 1, 4, new String[]{"LIFEUP_G"}, 12, 13434828, "Support"),
      new SkillTreeDefs.Node("SHIELD_A", 0, 2, new String[]{"LIFEUP_A"}, 2, 4491519, "Support"),
      new SkillTreeDefs.Node("SHIELD_B", 0, 3, new String[]{"SHIELD_A"}, 10, 8961023, "Support"),
      new SkillTreeDefs.Node("OFFENSE_A", 0, 4, new String[]{"SHIELD_B"}, 8, 16768324, "Support"),
      new SkillTreeDefs.Node("OFFENSE_O", 0, 5, new String[]{"OFFENSE_A"}, 14, 16777096, "Support")
   );
   private static final List<SkillTreeDefs.Node> LIGHTNER_TREE = List.of(
      new SkillTreeDefs.Node("RUDE_BUSTER", 3, 0, new String[0], 1, 16729156, "Root"),
      new SkillTreeDefs.Node("ICE_SHOCK", 1, 1, new String[]{"RUDE_BUSTER"}, 2, 4513279, "Combat"),
      new SkillTreeDefs.Node("GLITTER_BOMB", 2, 1, new String[]{"RUDE_BUSTER"}, 3, 16755268, "Combat"),
      new SkillTreeDefs.Node("SNOWGRAVE", 1, 2, new String[]{"ICE_SHOCK"}, 45, 11202303, "Combat"),
      new SkillTreeDefs.Node("RUDE_BUSTER_PLUS", 2, 2, new String[]{"GLITTER_BOMB"}, 10, 16737894, "Combat"),
      new SkillTreeDefs.Node("RED_BUSTER", 2, 3, new String[]{"RUDE_BUSTER_PLUS"}, 18, 16720418, "Combat"),
      new SkillTreeDefs.Node("X_SLASH", 1, 3, new String[]{"SNOWGRAVE"}, 20, 16746700, "Combat"),
      new SkillTreeDefs.Node("GIGAFLARE", 2, 4, new String[]{"RED_BUSTER"}, 12, 16768324, "Combat"),
      new SkillTreeDefs.Node("HEAL_PRAYER", 3, 1, new String[]{"RUDE_BUSTER"}, 1, 4521796, "Healing"),
      new SkillTreeDefs.Node("DUAL_HEAL", 3, 2, new String[]{"HEAL_PRAYER"}, 3, 4521796, "Healing"),
      new SkillTreeDefs.Node("ANGELIC_HALO", 3, 3, new String[]{"DUAL_HEAL"}, 8, 11206570, "Healing"),
      new SkillTreeDefs.Node("WARD", 3, 4, new String[]{"ANGELIC_HALO"}, 10, 4491519, "Healing"),
      new SkillTreeDefs.Node("GREAT_SHIELD", 3, 5, new String[]{"WARD"}, 12, 8961023, "Healing"),
      new SkillTreeDefs.Node("SHINING_SOUL", 4, 3, new String[]{"ANGELIC_HALO"}, 15, 16777164, "Healing"),
      new SkillTreeDefs.Node("PACIFY", 5, 1, new String[]{"RUDE_BUSTER"}, 2, 16755455, "Mercy"),
      new SkillTreeDefs.Node("LULLABY", 5, 2, new String[]{"PACIFY"}, 3, 12290303, "Mercy"),
      new SkillTreeDefs.Node("C_ACTION", 6, 2, new String[]{"PACIFY"}, 8, 16764159, "Mercy")
   );
   private static final List<SkillTreeDefs.Node> HUMAN_MAGE_TREE = List.of(
      new SkillTreeDefs.Node("ACT_CHECK", 3, 0, new String[0], 1, 4521898, "Root"),
      new SkillTreeDefs.Node("ACT_CHALLENGE", 3, 1, new String[]{"ACT_CHECK"}, 3, 4521898, "TP"),
      new SkillTreeDefs.Node("ACT_FLIRT", 1, 1, new String[]{"ACT_CHECK"}, 1, 16746751, "Status"),
      new SkillTreeDefs.Node("ACT_COMFORT", 1, 2, new String[]{"ACT_FLIRT"}, 2, 11176191, "Status"),
      new SkillTreeDefs.Node("ACT_SING", 1, 3, new String[]{"ACT_COMFORT"}, 3, 8947967, "Status"),
      new SkillTreeDefs.Node("ACT_BECKON", 0, 3, new String[]{"ACT_SING"}, 8, 16746564, "Status"),
      new SkillTreeDefs.Node("ACT_HUM", 3, 2, new String[]{"ACT_CHALLENGE"}, 2, 4521864, "Combat"),
      new SkillTreeDefs.Node("ACT_THREATEN", 3, 3, new String[]{"ACT_HUM"}, 3, 16737860, "Combat"),
      new SkillTreeDefs.Node("ACT_INSPIRE", 3, 4, new String[]{"ACT_THREATEN"}, 12, 16759620, "Combat"),
      new SkillTreeDefs.Node("ACT_INVOKE", 3, 5, new String[]{"ACT_INSPIRE"}, 12, 16729224, "Combat"),
      new SkillTreeDefs.Node("ACT_SPARE", 5, 1, new String[]{"ACT_CHECK"}, 2, 16768511, "Mercy"),
      new SkillTreeDefs.Node("ACT_TALK", 5, 2, new String[]{"ACT_SPARE"}, 8, 16764159, "Mercy"),
      new SkillTreeDefs.Node("ACT_PLEAD", 6, 2, new String[]{"ACT_SPARE"}, 8, 16755455, "Mercy"),
      new SkillTreeDefs.Node("ACT_ENTREAT", 6, 3, new String[]{"ACT_PLEAD"}, 10, 16746751, "Mercy")
   );
   public static final String[] STAT_KEYS = new String[]{"OFF", "DEF", "SPD", "IQ", "VIT", "LUCK"};
   public static final int[] STAT_AMOUNTS = new int[]{1, 2, 3};
   public static final int[] STAT_COSTS = new int[]{2, 4, 8};
   private static final Map<SoulType, List<SkillTreeDefs.Node>> SOUL_MAGE_TREES = new EnumMap<>(SoulType.class);
   private static final String[] ORI_ORDER = new String[]{
      "MOONBEAM",
      "STARLIGHT",
      "MOONMEND",
      "LUNAR_VEIL",
      "CRESCENT",
      "LUNAR_STRIKE",
      "FULL_MOON",
      "ECLIPSE_WARD",
      "MOONFALL",
      "VOID_TIDE",
      "NIGHTBLOOM",
      "UMBRAL_CRASH"
   };
   private static final int[] ORI_COST = new int[]{1, 2, 2, 3, 4, 6, 8, 8, 10, 14, 18, 18};
   private static final List<SkillTreeDefs.Node> ORI_TREE = buildOriBranch();
   private static final List<SkillTreeDefs.Node> MONSTER_TREE = List.of(
      new SkillTreeDefs.Node("MONSTER_SENSE", 3, 5, new String[0], 1, 4521864, "Root"),
      new SkillTreeDefs.Node("STATIC_BOLT", 1, 4, new String[]{"MONSTER_SENSE"}, 2, 16772608, "Electric"),
      new SkillTreeDefs.Node("SHOCK_WAVE", 1, 3, new String[]{"STATIC_BOLT"}, 3, 16763904, "Electric"),
      new SkillTreeDefs.Node("THUNDER_CLAP", 1, 2, new String[]{"SHOCK_WAVE"}, 5, 16755200, "Electric"),
      new SkillTreeDefs.Node("LIGHTNING_FANG", 1, 1, new String[]{"THUNDER_CLAP"}, 8, 16746496, "Electric"),
      new SkillTreeDefs.Node("VOLT_PRISON", 0, 1, new String[]{"LIGHTNING_FANG"}, 10, 16737792, "Electric"),
      new SkillTreeDefs.Node("EMBER_SHOT", 2, 4, new String[]{"MONSTER_SENSE"}, 2, -5618654, "Fire"),
      new SkillTreeDefs.Node("FIRE_SURGE", 2, 3, new String[]{"EMBER_SHOT"}, 4, -48128, "Fire"),
      new SkillTreeDefs.Node("FLAME_VEIL", 2, 2, new String[]{"FIRE_SURGE"}, 5, -30652, "Fire"),
      new SkillTreeDefs.Node("INFERNO", 2, 1, new String[]{"FLAME_VEIL"}, 8, -56832, "Fire"),
      new SkillTreeDefs.Node("MAGMA_SURGE", 2, 0, new String[]{"INFERNO"}, 10, -61184, "Fire"),
      new SkillTreeDefs.Node("BONE_TOSS", 4, 4, new String[]{"MONSTER_SENSE"}, 2, -3355444, "Skeletal"),
      new SkillTreeDefs.Node("BONE_WALL", 4, 3, new String[]{"BONE_TOSS"}, 4, -5592406, "Skeletal"),
      new SkillTreeDefs.Node("BONE_BARRAGE", 4, 2, new String[]{"BONE_WALL"}, 5, -7829368, "Skeletal"),
      new SkillTreeDefs.Node("SKELETAL_HEAL", 4, 1, new String[]{"BONE_BARRAGE"}, 4, -12255352, "Skeletal"),
      new SkillTreeDefs.Node("BONE_STORM", 3, 1, new String[]{"BONE_TOSS"}, 8, -10066330, "Skeletal"),
      new SkillTreeDefs.Node("IRON_PLATING", 5, 4, new String[]{"MONSTER_SENSE"}, 2, -7829334, "Construct"),
      new SkillTreeDefs.Node("GEAR_STRIKE", 5, 3, new String[]{"IRON_PLATING"}, 3, -10066262, "Construct"),
      new SkillTreeDefs.Node("POWER_SURGE", 5, 2, new String[]{"GEAR_STRIKE"}, 5, -12303190, "Construct"),
      new SkillTreeDefs.Node("CONSTRUCT_HEAL", 6, 2, new String[]{"IRON_PLATING"}, 4, -12255352, "Construct"),
      new SkillTreeDefs.Node("OVERCHARGE", 5, 1, new String[]{"POWER_SURGE"}, 8, -14540118, "Construct"),
      new SkillTreeDefs.Node("STAR_BLAZING", 3, 6, new String[]{"MONSTER_SENSE"}, 8, -8892, "Asriel"),
      new SkillTreeDefs.Node("SHOCKING_BREAKER", 3, 7, new String[]{"STAR_BLAZING"}, 8, -4608, "Asriel"),
      new SkillTreeDefs.Node("CHAOS_SABER_ASSAULT", 3, 8, new String[]{"SHOCKING_BREAKER"}, 10, -30652, "Asriel"),
      new SkillTreeDefs.Node("CHAOS_BUSTER", 3, 9, new String[]{"CHAOS_SABER_ASSAULT"}, 10, -86, "Asriel"),
      new SkillTreeDefs.Node("HYPER_GONER", 3, 10, new String[]{"CHAOS_BUSTER"}, 15, -1, "Asriel")
   );

   private static List<SkillTreeDefs.GlobalNode> projectTree(List<SkillTreeDefs.Node> tree, String groupId, float angleDeg) {
      if (tree.isEmpty()) {
         return List.of();
      } else {
         int rootCol = tree.get(0).col();

         for (SkillTreeDefs.Node n : tree) {
            if (n.row() == 0) {
               rootCol = n.col();
               break;
            }
         }

         double rad = Math.toRadians((double)angleDeg);
         double dirX = Math.sin(rad);
         double dirY = -Math.cos(rad);
         double perpX = Math.cos(rad);
         double perpY = Math.sin(rad);
         List out = new ArrayList<>();

         for (SkillTreeDefs.Node nx : tree) {
            if (!isStatNode(nx.skill())) {
               float colOffset = (float)(nx.col() - rootCol);
               float radius = 180.0F + (float)nx.row() * 90.0F;
               float lateralOffset = radius * 0.24F * colOffset;
               float wx = (float)(dirX * (double)radius + perpX * (double)lateralOffset);
               float wy = (float)(dirY * (double)radius + perpY * (double)lateralOffset);
               out.add(new SkillTreeDefs.GlobalNode(nx, groupId, wx, wy));
            }
         }

         return out;
      }
   }

   public static List<SkillTreeDefs.GlobalNode> getAllNodesForHuman() {
      List out = new ArrayList<>();
      out.addAll(projectTree(PSI_TREE, "PSI", 90.0F));
      out.addAll(projectTree(LIGHTNER_TREE, "LIGHTNER", 180.0F));
      out.addAll(projectTree(HUMAN_MAGE_TREE, "HUMAN", 0.0F));

      for (SoulType soul : SOUL_TRAIT_ORDER) {
         List t = SOUL_MAGE_TREES.getOrDefault(soul, List.of());
         out.addAll(projectTree(t, "SOUL_" + soul.name(), 270.0F));
      }

      return out;
   }

   public static List<SkillTreeDefs.GlobalNode> getRenderNodesForHuman(SoulType selectedTab) {
      List out = new ArrayList<>();
      out.addAll(projectTree(PSI_TREE, "PSI", 90.0F));
      out.addAll(projectTree(LIGHTNER_TREE, "LIGHTNER", 180.0F));
      out.addAll(projectTree(HUMAN_MAGE_TREE, "HUMAN", 0.0F));
      SoulType tab = selectedTab != null ? selectedTab : SoulType.DETERMINATION;
      List t = SOUL_MAGE_TREES.getOrDefault(tab, List.of());
      out.addAll(projectTree(t, "SOUL_" + tab.name(), 270.0F));
      return out;
   }

   public static List<SkillTreeDefs.GlobalNode> getRenderNodesForRecruit(Recruit recruit) {
      List t = getRecruitTree(recruit);
      if (t.isEmpty()) {
         return List.of();
      } else {
         String g = recruit == Recruit.ORI
            ? "ORI"
            : (recruit.playerClass == PlayerClass.PSI_USER ? "PSI" : (recruit.playerClass == PlayerClass.LIGHTNER ? "LIGHTNER" : "HUMAN"));
         return isSingleBranch(t) ? projectBranch(t, g) : projectTree(t, g, 0.0F);
      }
   }

   public static SkillTreeDefs.Node findHumanNode(String skillName) {
      for (List t : List.of(PSI_TREE, LIGHTNER_TREE, HUMAN_MAGE_TREE)) {
         for (SkillTreeDefs.Node n : t) {
            if (n.skill().equals(skillName)) {
               return n;
            }
         }
      }

      for (List t : SOUL_MAGE_TREES.values()) {
         for (SkillTreeDefs.Node nx : t) {
            if (nx.skill().equals(skillName)) {
               return nx;
            }
         }
      }

      return null;
   }

   public static int recruitSlotCost(String skillName, PlayerClass cls, SoulType soul) {
      if (isStatNode(skillName)) {
         return 0;
      } else {
         for (SkillTreeDefs.Node n : treeForAll(skillName, cls, soul)) {
            if (n.skill().equals(skillName)) {
               return Math.max(1, (int)Math.ceil((double)n.cost() / 2.0));
            }
         }

         SkillTreeDefs.Node nx = findHumanNode(skillName);
         if (nx == null) {
            for (SkillTreeDefs.Node o : ORI_TREE) {
               if (o.skill().equals(skillName)) {
                  nx = o;
               }
            }
         }

         if (nx == null) {
            for (SkillTreeDefs.Node ox : MONSTER_TREE) {
               if (ox.skill().equals(skillName)) {
                  nx = ox;
               }
            }
         }

         return nx != null ? Math.max(1, (int)Math.ceil((double)nx.cost() / 2.0)) : 1;
      }
   }

   public static PlayerClass leaningClass(Set<String> unlocked, PlayerClass current) {
      Map counts = new EnumMap<>(PlayerClass.class);

      for (SkillTreeDefs.GlobalNode gn : getAllNodesForHuman()) {
         if (unlocked.contains(gn.skill())) {
            PlayerClass c = mapGroupToClass(gn.groupId());
            if (c != null) {
               counts.merge(c, 1, Integer::sum);
            }
         }
      }

      if (counts.isEmpty()) {
         return current != null ? current : PlayerClass.PSI_USER;
      } else {
         int max = Collections.max(counts.values());
         if (current != null && counts.getOrDefault(current, 0) == max) {
            return current;
         } else {
            for (Entry e : counts.entrySet()) {
               if (e.getValue() == max) {
                  return e.getKey();
               }
            }

            return current != null ? current : PlayerClass.PSI_USER;
         }
      }
   }

   public static List<SkillTreeDefs.GlobalNode> getAllNodesForMonster() {
      List out = new ArrayList<>();
      out.addAll(projectTree(PSI_TREE, "PSI", 90.0F));
      out.addAll(projectTree(LIGHTNER_TREE, "LIGHTNER", 180.0F));
      out.addAll(projectTree(MONSTER_TREE, "MONSTER", 0.0F));
      return out;
   }

   public static List<SkillTreeDefs.GlobalNode> getAllNodesFor(BattleData.PlayerProgress prog) {
      return getAllNodesFor(prog.playerRace);
   }

   public static List<SkillTreeDefs.GlobalNode> getAllNodesFor(CharacterRace race) {
      return race == CharacterRace.MONSTER ? getAllNodesForMonster() : getAllNodesForHuman();
   }

   public static SkillTreeDefs.GlobalNode findGlobalNode(String skillName, BattleData.PlayerProgress prog) {
      for (SkillTreeDefs.GlobalNode gn : getAllNodesFor(prog)) {
         if (gn.skill().equals(skillName)) {
            return gn;
         }
      }

      return null;
   }

   public static boolean canUnlockUnified(String skillName, BattleData.PlayerProgress prog) {
      return findGlobalNode(skillName, prog) != null;
   }

   public static boolean prereqsMetUnified(String skillName, Set<String> unlocked, BattleData.PlayerProgress prog) {
      SkillTreeDefs.GlobalNode gn = findGlobalNode(skillName, prog);
      if (gn == null) {
         return false;
      } else {
         for (String req : gn.node().prereqs()) {
            if (!unlocked.contains(req)) {
               return false;
            }
         }

         return true;
      }
   }

   public static int nodeCostUnified(String skillName, BattleData.PlayerProgress prog) {
      SkillTreeDefs.GlobalNode gn = findGlobalNode(skillName, prog);
      return gn != null ? gn.node().cost() : 1;
   }

   public static int skillSlotCostUnified(String skillName, BattleData.PlayerProgress prog) {
      if (isStatNode(skillName)) {
         return 0;
      } else {
         int pointCost = nodeCostUnified(skillName, prog);
         return Math.max(1, (int)Math.ceil((double)pointCost / 2.0));
      }
   }

   public static BattleSkill resolveSkill(SkillTreeDefs.GlobalNode gn) {
      if (gn == null) {
         return null;
      } else {
         String g = gn.groupId();
         if (g.equals("PSI")) {
            return psiByName(gn.skill());
         } else if (g.equals("LIGHTNER")) {
            return lightnerByName(gn.skill());
         } else if (g.equals("HUMAN")) {
            return humanMageByName(gn.skill());
         } else if (g.equals("MONSTER")) {
            return monsterSkillByName(gn.skill());
         } else if (g.equals("ORI")) {
            return oriSkillByName(gn.skill());
         } else {
            return g.startsWith("SOUL_") ? soulMageSkillByName(gn.skill()) : null;
         }
      }
   }

   private static PlayerClass mapGroupToClass(String groupId) {
      if (groupId.equals("PSI")) {
         return PlayerClass.PSI_USER;
      } else if (groupId.equals("LIGHTNER")) {
         return PlayerClass.LIGHTNER;
      } else if (groupId.equals("HUMAN")) {
         return PlayerClass.HUMAN_MAGE;
      } else if (groupId.equals("MONSTER")) {
         return PlayerClass.MONSTER;
      } else if (groupId.equals("ORI")) {
         return PlayerClass.ORI;
      } else {
         return groupId.startsWith("SOUL_") ? PlayerClass.SOUL_MAGE : null;
      }
   }

   public static PlayerClass computeEffectiveClass(BattleData.PlayerProgress prog) {
      Map counts = new EnumMap<>(PlayerClass.class);

      for (SkillTreeDefs.GlobalNode gn : getAllNodesFor(prog)) {
         if (prog.unlockedSkills.contains(gn.skill())) {
            PlayerClass mapped = mapGroupToClass(gn.groupId());
            if (mapped != null) {
               counts.merge(mapped, 1, Integer::sum);
            }
         }
      }

      if (counts.isEmpty()) {
         return PlayerClass.PSI_USER;
      } else {
         int max = Collections.max(counts.values());
         List top = new ArrayList<>();

         for (Entry e : counts.entrySet()) {
            if (e.getValue() == max) {
               top.add(e.getKey());
            }
         }

         return top.size() == 1 ? top.get(0) : top.get(CLASS_TIE_RNG.nextInt(top.size()));
      }
   }

   public static boolean isStatNode(String name) {
      if (name != null && name.startsWith("STAT_")) {
         for (String k : STAT_KEYS) {
            for (int i = 1; i <= 3; i++) {
               if (name.equals("STAT_" + k + "_" + i)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static int statNodeAmount(String name) {
      if (!isStatNode(name)) {
         return 0;
      } else {
         String var1 = name.substring(name.lastIndexOf(95) + 1);

         return switch (var1) {
            case "1" -> 1;
            case "2" -> 2;
            case "3" -> 3;
            default -> 0;
         };
      }
   }

   public static String statNodeDisplayName(String name) {
      if (!isStatNode(name)) {
         return name;
      } else {
         int amt = statNodeAmount(name);
         String var3 = name.substring(5, name.lastIndexOf(95));

         String stat = switch (var3) {
            case "OFF" -> "Offense";
            case "DEF" -> "Defense";
            case "SPD" -> "Magic";
            case "IQ" -> "IQ";
            case "VIT" -> "Vitality";
            case "LUCK" -> "Luck";
            default -> name.substring(5, name.lastIndexOf(95));
         };
         return "+" + amt + " " + stat;
      }
   }

   public static int statNodeCost(String name) {
      return switch (statNodeAmount(name)) {
         case 1 -> 2;
         case 2 -> 4;
         case 3 -> 8;
         default -> 0;
      };
   }

   public static int statNodeStatIndex(String name) {
      if (!isStatNode(name)) {
         return -1;
      } else {
         String inner = name.substring(5, name.lastIndexOf(95));

         return switch (inner) {
            case "OFF" -> 0;
            case "DEF" -> 1;
            case "SPD" -> 2;
            case "IQ" -> 3;
            case "VIT" -> 4;
            case "LUCK" -> 5;
            default -> -1;
         };
      }
   }

   public static String statNodeDescription(String name) {
      int amt = statNodeAmount(name);
      String inner = name.substring(5, name.lastIndexOf(95));

      return switch (inner) {
         case "OFF" -> "Permanently increases BASH damage by " + amt * 18 + "%. Lost if soul is rerolled.";
         case "DEF" -> "Permanently reduces incoming damage by " + amt + " flat. Lost if soul is rerolled.";
         case "SPD" -> "Permanently increases skill effectiveness by " + (float)amt * 1.7F + "%. Lost if soul is rerolled.";
         case "IQ" -> "Permanently increases TP gained per graze by " + (float)amt * 0.5F + ". Lost if soul is rerolled.";
         case "VIT" -> "Permanently increases max HP by " + amt * 15 + ". Lost if soul is rerolled.";
         case "LUCK" -> "Permanently increases critical hit chance by " + amt + "%. Lost if soul is rerolled.";
         default -> "Grants +" + amt + " to a stat. Lost if soul is rerolled.";
      };
   }

   private static void buildSoulTree(SoulType soul, List<SkillTreeDefs.Node> nodes) {
      SOUL_MAGE_TREES.put(soul, Collections.unmodifiableList(nodes));
   }

   private static int soulColor(SoulType soul) {
      return soul.getColor() | 0xFF000000;
   }

   private static List<SkillTreeDefs.Node> buildOriBranch() {
      List out = new ArrayList<>();

      for (int i = 0; i < ORI_ORDER.length; i++) {
         String[] pre = i == 0 ? new String[0] : new String[]{ORI_ORDER[i - 1]};
         int shade = i % 3 == 0 ? 16777215 : (i % 3 == 1 ? 14540253 : 12303291);
         out.add(new SkillTreeDefs.Node(ORI_ORDER[i], 2, i, pre, ORI_COST[i], shade, "Lunar"));
      }

      return List.copyOf(out);
   }

   private static boolean isSingleBranch(List<SkillTreeDefs.Node> t) {
      for (SkillTreeDefs.Node n : t) {
         if (n.col() != t.get(0).col()) {
            return false;
         }
      }

      return true;
   }

   private static List<SkillTreeDefs.GlobalNode> projectBranch(List<SkillTreeDefs.Node> tree, String groupId) {
      List out = new ArrayList<>();

      for (SkillTreeDefs.Node n : tree) {
         float wy = -(126.0F + (float)n.row() * 90.0F * 0.9F);
         float wx = (float)Math.sin((double)((float)n.row() * 0.85F)) * 42.0F;
         out.add(new SkillTreeDefs.GlobalNode(n, groupId, wx, wy));
      }

      return out;
   }

   public static List<SkillTreeDefs.Node> getRecruitTree(Recruit recruit) {
      return recruit == Recruit.ORI ? ORI_TREE : List.of();
   }

   public static OriSkill oriSkillByName(String name) {
      try {
         return OriSkill.valueOf(name);
      } catch (Exception var2) {
         return null;
      }
   }

   public static BattleSkill recruitSkillByName(Recruit recruit, String name) {
      return recruit == Recruit.ORI ? oriSkillByName(name) : null;
   }

   public static List<SkillTreeDefs.Node> getMonsterTree() {
      return MONSTER_TREE;
   }

   public static MonsterSkill monsterSkillByName(String name) {
      try {
         return MonsterSkill.valueOf(name);
      } catch (Exception var2) {
         return null;
      }
   }

   public static List<SkillTreeDefs.Node> getTree(PlayerClass cls, SoulType soul) {
      if (cls == PlayerClass.PSI_USER) {
         return PSI_TREE;
      } else if (cls == PlayerClass.LIGHTNER) {
         return LIGHTNER_TREE;
      } else if (cls == PlayerClass.HUMAN_MAGE) {
         return HUMAN_MAGE_TREE;
      } else if (cls == PlayerClass.MONSTER) {
         return MONSTER_TREE;
      } else if (cls == PlayerClass.ORI) {
         return ORI_TREE;
      } else {
         return cls == PlayerClass.SOUL_MAGE && soul != null ? SOUL_MAGE_TREES.get(soul) : List.of();
      }
   }

   public static List<SkillTreeDefs.Node> getSoulTree(SoulType soul) {
      return SOUL_MAGE_TREES.getOrDefault(soul, List.of());
   }

   public static int nodeCost(String skillName, PlayerClass cls) {
      return nodeCost(skillName, cls, null);
   }

   public static int nodeCost(String skillName, PlayerClass cls, SoulType soul) {
      if (isStatNode(skillName)) {
         return statNodeCost(skillName);
      } else {
         for (SkillTreeDefs.Node n : treeForAll(skillName, cls, soul)) {
            if (n.skill().equals(skillName)) {
               return n.cost();
            }
         }

         return 1;
      }
   }

   public static int skillSlotCost(String skillName, PlayerClass cls, SoulType soul) {
      if (isStatNode(skillName)) {
         return 0;
      } else {
         int pointCost = nodeCost(skillName, cls, soul);
         return Math.max(1, (int)Math.ceil((double)pointCost / 2.0));
      }
   }

   public static int maxSkillSlots(int level) {
      TurnBattleConfig cfg = TurnBattleConfig.get();
      return cfg.baseSkillSlots + cfg.skillSlotsPerTenLevels * (level / 10);
   }

   public static boolean prereqsMet(String skillName, Set<String> unlocked, PlayerClass cls) {
      return prereqsMet(skillName, unlocked, cls, null);
   }

   public static boolean prereqsMet(String skillName, Set<String> unlocked, PlayerClass cls, SoulType soul) {
      for (SkillTreeDefs.Node n : treeForAll(skillName, cls, soul)) {
         if (n.skill().equals(skillName)) {
            for (String req : n.prereqs()) {
               if (!unlocked.contains(req)) {
                  return false;
               }
            }

            return true;
         }
      }

      return false;
   }

   public static boolean canUnlock(String skillName, BattleData.PlayerProgress prog) {
      PlayerClass cls = prog.playerClass;
      if (cls == null) {
         return false;
      } else if (cls == PlayerClass.PSI_USER) {
         return psiByName(skillName) != null;
      } else if (cls == PlayerClass.LIGHTNER) {
         return lightnerByName(skillName) != null;
      } else if (cls == PlayerClass.HUMAN_MAGE) {
         return humanMageByName(skillName) != null;
      } else if (cls == PlayerClass.MONSTER) {
         return monsterSkillByName(skillName) != null;
      } else if (cls == PlayerClass.ORI) {
         return oriSkillByName(skillName) != null;
      } else if (cls == PlayerClass.SOUL_MAGE) {
         if (isStatNode(skillName)) {
            List soulTree = SOUL_MAGE_TREES.get(prog.soulType);
            if (soulTree == null) {
               return false;
            } else {
               for (SkillTreeDefs.Node n : soulTree) {
                  if (n.skill().equals(skillName)) {
                     return true;
                  }
               }

               return false;
            }
         } else {
            SoulMageSkill sms = soulMageSkillByName(skillName);
            return sms != null && sms.getSoul() != null ? sms.getSoul() == prog.soulType : false;
         }
      } else {
         return false;
      }
   }

   private static List<SkillTreeDefs.Node> treeForAll(String skillName, PlayerClass cls) {
      return treeForAll(skillName, cls, null);
   }

   private static List<SkillTreeDefs.Node> treeForAll(String skillName, PlayerClass cls, SoulType soul) {
      if (cls == PlayerClass.PSI_USER) {
         return PSI_TREE;
      } else if (cls == PlayerClass.LIGHTNER) {
         return LIGHTNER_TREE;
      } else if (cls == PlayerClass.HUMAN_MAGE) {
         return HUMAN_MAGE_TREE;
      } else if (cls == PlayerClass.MONSTER) {
         return MONSTER_TREE;
      } else if (cls == PlayerClass.ORI) {
         return ORI_TREE;
      } else {
         if (cls == PlayerClass.SOUL_MAGE) {
            if (soul != null) {
               List soulTree = SOUL_MAGE_TREES.get(soul);
               if (soulTree != null) {
                  return soulTree;
               }
            }

            for (List t : SOUL_MAGE_TREES.values()) {
               for (SkillTreeDefs.Node n : t) {
                  if (n.skill().equals(skillName)) {
                     return t;
                  }
               }
            }
         }

         return List.of();
      }
   }

   public static int levelRequiredFor(String name) {
      BattleSkill s = psiByName(name);
      if (s == null) {
         s = lightnerByName(name);
      }

      if (s == null) {
         s = humanMageByName(name);
      }

      if (s == null) {
         s = soulMageSkillByName(name);
      }

      if (s == null) {
         s = monsterSkillByName(name);
      }

      if (s == null) {
         s = oriSkillByName(name);
      }

      return s == null ? 1 : Math.max(1, s.getLevelRequired());
   }

   public static PsiSkill psiByName(String name) {
      try {
         return PsiSkill.valueOf(name);
      } catch (Exception var2) {
         return null;
      }
   }

   public static LightnerSkill lightnerByName(String name) {
      try {
         return LightnerSkill.valueOf(name);
      } catch (Exception var2) {
         return null;
      }
   }

   public static HumanMageSkill humanMageByName(String name) {
      try {
         return HumanMageSkill.valueOf(name);
      } catch (Exception var2) {
         return null;
      }
   }

   public static SoulMageSkill soulMageSkillByName(String name) {
      try {
         return SoulMageSkill.valueOf(name);
      } catch (Exception var2) {
         return null;
      }
   }

   public static boolean isValidForClass(String skillName, PlayerClass cls) {
      if (cls == null) {
         return false;
      } else if (cls == PlayerClass.PSI_USER) {
         return psiByName(skillName) != null;
      } else if (cls == PlayerClass.LIGHTNER) {
         return lightnerByName(skillName) != null;
      } else if (cls == PlayerClass.HUMAN_MAGE) {
         return humanMageByName(skillName) != null;
      } else if (cls == PlayerClass.MONSTER) {
         return monsterSkillByName(skillName) != null;
      } else {
         return cls == PlayerClass.ORI ? oriSkillByName(skillName) != null : false;
      }
   }

   static {
      buildSoulTree(
         SoulType.DETERMINATION,
         Arrays.asList(
            new SkillTreeDefs.Node("DET_STRIKE", 2, 0, new String[0], 1, 16711680, "Root"),
            new SkillTreeDefs.Node("DET_MEND", 0, 1, new String[]{"DET_STRIKE"}, 1, 16729156, "Support"),
            new SkillTreeDefs.Node("DET_STRIKE_P", 2, 1, new String[]{"DET_STRIKE"}, 3, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_PULSE", 3, 1, new String[]{"DET_STRIKE"}, 2, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_BARRIER", 1, 2, new String[]{"DET_MEND"}, 2, 16729156, "Support"),
            new SkillTreeDefs.Node("DET_MEND_P", 0, 2, new String[]{"DET_MEND"}, 3, 16729156, "Support"),
            new SkillTreeDefs.Node("DET_STRIKE_PP", 2, 2, new String[]{"DET_STRIKE_P"}, 5, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_PULSE_P", 3, 2, new String[]{"DET_PULSE"}, 3, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_SURGE", 4, 2, new String[]{"DET_PULSE"}, 3, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_MEND_PP", 0, 3, new String[]{"DET_MEND_P"}, 5, 16729156, "Support"),
            new SkillTreeDefs.Node("DET_PULSE_PP", 3, 3, new String[]{"DET_PULSE_P"}, 5, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_SURGE_P", 4, 3, new String[]{"DET_SURGE"}, 4, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_BIND", 5, 3, new String[]{"DET_SURGE"}, 3, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_SURGE_PP", 4, 4, new String[]{"DET_SURGE_P"}, 6, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_CHANNEL", 6, 4, new String[]{"DET_BIND"}, 8, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_APEX", 7, 5, new String[]{"DET_CHANNEL"}, 10, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_APEX_P", 7, 6, new String[]{"DET_APEX"}, 5, 16711680, "Attack"),
            new SkillTreeDefs.Node("DET_APEX_PP", 7, 7, new String[]{"DET_APEX_P"}, 7, 16711680, "Attack"),
            new SkillTreeDefs.Node("STAT_OFF_1", 8, 2, new String[]{"DET_STRIKE"}, 2, 16737894, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_1", 9, 2, new String[]{"DET_STRIKE"}, 2, 16737894, "Stats"),
            new SkillTreeDefs.Node("STAT_OFF_2", 8, 3, new String[]{"STAT_OFF_1"}, 4, 16729156, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_2", 9, 3, new String[]{"STAT_VIT_1"}, 4, 16729156, "Stats"),
            new SkillTreeDefs.Node("STAT_OFF_3", 8, 4, new String[]{"STAT_OFF_2"}, 8, 16720418, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_3", 9, 4, new String[]{"STAT_VIT_2"}, 8, 16720418, "Stats")
         )
      );
      buildSoulTree(
         SoulType.PERSEVERANCE,
         Arrays.asList(
            new SkillTreeDefs.Node("PER_STRIKE", 2, 0, new String[0], 1, 11141375, "Root"),
            new SkillTreeDefs.Node("PER_MEND", 0, 1, new String[]{"PER_STRIKE"}, 1, 12272895, "Support"),
            new SkillTreeDefs.Node("PER_STRIKE_P", 2, 1, new String[]{"PER_STRIKE"}, 3, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_PULSE", 3, 1, new String[]{"PER_STRIKE"}, 2, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_MEND_P", 0, 2, new String[]{"PER_MEND"}, 3, 12272895, "Support"),
            new SkillTreeDefs.Node("PER_BARRIER", 1, 2, new String[]{"PER_MEND"}, 2, 12272895, "Support"),
            new SkillTreeDefs.Node("PER_STRIKE_PP", 2, 2, new String[]{"PER_STRIKE_P"}, 5, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_PULSE_P", 3, 2, new String[]{"PER_PULSE"}, 3, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_SURGE", 4, 2, new String[]{"PER_PULSE"}, 3, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_MEND_PP", 0, 3, new String[]{"PER_MEND_P"}, 5, 12272895, "Support"),
            new SkillTreeDefs.Node("PER_PULSE_PP", 3, 3, new String[]{"PER_PULSE_P"}, 5, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_SURGE_P", 4, 3, new String[]{"PER_SURGE"}, 3, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_BIND", 5, 3, new String[]{"PER_SURGE"}, 3, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_CHANNEL", 6, 3, new String[]{"PER_BARRIER"}, 8, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_SURGE_PP", 4, 4, new String[]{"PER_SURGE_P"}, 5, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_BIND_P", 5, 4, new String[]{"PER_BIND"}, 3, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_APEX", 7, 4, new String[]{"PER_BIND", "PER_CHANNEL"}, 10, 12272895, "Attack"),
            new SkillTreeDefs.Node("PER_BIND_PP", 5, 5, new String[]{"PER_BIND_P"}, 5, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_APEX_P", 7, 5, new String[]{"PER_APEX"}, 5, 11141375, "Attack"),
            new SkillTreeDefs.Node("PER_APEX_PP", 7, 6, new String[]{"PER_APEX_P"}, 7, 11141375, "Attack"),
            new SkillTreeDefs.Node("STAT_DEF_1", 8, 2, new String[]{"PER_STRIKE"}, 2, 12281599, "Stats"),
            new SkillTreeDefs.Node("STAT_LUCK_1", 9, 2, new String[]{"PER_STRIKE"}, 2, 12281599, "Stats"),
            new SkillTreeDefs.Node("STAT_DEF_2", 8, 3, new String[]{"STAT_DEF_1"}, 4, 10027246, "Stats"),
            new SkillTreeDefs.Node("STAT_LUCK_2", 9, 3, new String[]{"STAT_LUCK_1"}, 4, 10027246, "Stats"),
            new SkillTreeDefs.Node("STAT_DEF_3", 8, 4, new String[]{"STAT_DEF_2"}, 8, 7798971, "Stats"),
            new SkillTreeDefs.Node("STAT_LUCK_3", 9, 4, new String[]{"STAT_LUCK_2"}, 8, 7798971, "Stats")
         )
      );
      buildSoulTree(
         SoulType.INTEGRITY,
         Arrays.asList(
            new SkillTreeDefs.Node("INT_STRIKE", 2, 0, new String[0], 1, 26367, "Root"),
            new SkillTreeDefs.Node("INT_MEND", 0, 1, new String[]{"INT_STRIKE"}, 1, 4491519, "Support"),
            new SkillTreeDefs.Node("INT_STRIKE_P", 2, 1, new String[]{"INT_STRIKE"}, 3, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_PULSE", 3, 1, new String[]{"INT_STRIKE"}, 2, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_MEND_P", 0, 2, new String[]{"INT_MEND"}, 3, 4491519, "Support"),
            new SkillTreeDefs.Node("INT_BARRIER", 1, 2, new String[]{"INT_PULSE"}, 2, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_STRIKE_PP", 2, 2, new String[]{"INT_STRIKE_P"}, 5, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_PULSE_P", 3, 2, new String[]{"INT_PULSE"}, 3, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_SURGE", 4, 2, new String[]{"INT_BARRIER"}, 3, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_MEND_PP", 0, 3, new String[]{"INT_MEND_P"}, 5, 4491519, "Support"),
            new SkillTreeDefs.Node("INT_PULSE_PP", 3, 3, new String[]{"INT_PULSE_P"}, 5, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_SURGE_P", 4, 3, new String[]{"INT_SURGE"}, 4, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_BIND", 5, 3, new String[]{"INT_SURGE"}, 3, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_SURGE_PP", 4, 4, new String[]{"INT_SURGE_P"}, 6, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_CHANNEL", 6, 4, new String[]{"INT_BIND"}, 8, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_APEX", 7, 5, new String[]{"INT_CHANNEL"}, 10, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_APEX_P", 7, 6, new String[]{"INT_APEX"}, 5, 26367, "Attack"),
            new SkillTreeDefs.Node("INT_APEX_PP", 7, 7, new String[]{"INT_APEX_P"}, 7, 26367, "Attack"),
            new SkillTreeDefs.Node("STAT_DEF_1", 8, 2, new String[]{"INT_PULSE"}, 2, 4487116, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_1", 9, 2, new String[]{"INT_PULSE"}, 2, 4487116, "Stats"),
            new SkillTreeDefs.Node("STAT_DEF_2", 8, 3, new String[]{"STAT_DEF_1"}, 4, 2245785, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_2", 9, 3, new String[]{"STAT_IQ_1"}, 4, 2245785, "Stats"),
            new SkillTreeDefs.Node("STAT_DEF_3", 8, 4, new String[]{"STAT_DEF_2"}, 8, 1127287, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_3", 9, 4, new String[]{"STAT_IQ_2"}, 8, 1127287, "Stats")
         )
      );
      buildSoulTree(
         SoulType.PATIENCE,
         Arrays.asList(
            new SkillTreeDefs.Node("PAT_STRIKE", 2, 0, new String[0], 1, 56831, "Root"),
            new SkillTreeDefs.Node("PAT_MEND", 0, 1, new String[]{"PAT_STRIKE"}, 1, 4517631, "Support"),
            new SkillTreeDefs.Node("PAT_STRIKE_P", 2, 1, new String[]{"PAT_STRIKE"}, 3, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_MEND_P", 0, 2, new String[]{"PAT_MEND"}, 3, 4517631, "Support"),
            new SkillTreeDefs.Node("PAT_BARRIER", 1, 2, new String[]{"PAT_MEND"}, 2, 4517631, "Support"),
            new SkillTreeDefs.Node("PAT_STRIKE_PP", 2, 2, new String[]{"PAT_STRIKE_P"}, 5, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_MEND_PP", 0, 3, new String[]{"PAT_MEND_P"}, 5, 4517631, "Support"),
            new SkillTreeDefs.Node("PAT_PULSE", 3, 3, new String[]{"PAT_BARRIER"}, 2, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_PULSE_P", 3, 4, new String[]{"PAT_PULSE"}, 3, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_SURGE", 4, 4, new String[]{"PAT_PULSE"}, 3, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_PULSE_PP", 3, 5, new String[]{"PAT_PULSE_P"}, 5, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_SURGE_P", 4, 5, new String[]{"PAT_SURGE"}, 4, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_BIND", 5, 5, new String[]{"PAT_SURGE"}, 3, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_SURGE_PP", 4, 6, new String[]{"PAT_SURGE_P"}, 6, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_CHANNEL", 6, 6, new String[]{"PAT_BIND"}, 8, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_APEX", 7, 7, new String[]{"PAT_CHANNEL"}, 10, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_APEX_P", 7, 8, new String[]{"PAT_APEX"}, 5, 56831, "Attack"),
            new SkillTreeDefs.Node("PAT_APEX_PP", 7, 9, new String[]{"PAT_APEX_P"}, 7, 56831, "Attack"),
            new SkillTreeDefs.Node("STAT_IQ_1", 8, 3, new String[]{"PAT_STRIKE"}, 2, 4504524, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_1", 9, 3, new String[]{"PAT_STRIKE"}, 2, 4504524, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_2", 8, 4, new String[]{"STAT_IQ_1"}, 4, 2267545, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_2", 9, 4, new String[]{"STAT_VIT_1"}, 4, 2267545, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_3", 8, 5, new String[]{"STAT_IQ_2"}, 8, 1144695, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_3", 9, 5, new String[]{"STAT_VIT_2"}, 8, 1144695, "Stats")
         )
      );
      buildSoulTree(
         SoulType.JUSTICE,
         Arrays.asList(
            new SkillTreeDefs.Node("JUS_STRIKE", 2, 0, new String[0], 1, 16768256, "Root"),
            new SkillTreeDefs.Node("JUS_MEND", 0, 1, new String[]{"JUS_STRIKE"}, 1, 16772676, "Support"),
            new SkillTreeDefs.Node("JUS_STRIKE_P", 2, 1, new String[]{"JUS_STRIKE"}, 3, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_PULSE", 3, 1, new String[]{"JUS_STRIKE"}, 2, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_MEND_P", 0, 2, new String[]{"JUS_MEND"}, 3, 16772676, "Support"),
            new SkillTreeDefs.Node("JUS_BARRIER", 1, 2, new String[]{"JUS_PULSE"}, 2, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_STRIKE_PP", 2, 2, new String[]{"JUS_STRIKE_P"}, 5, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_PULSE_P", 3, 2, new String[]{"JUS_PULSE"}, 3, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_SURGE", 4, 2, new String[]{"JUS_MEND"}, 3, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_MEND_PP", 0, 3, new String[]{"JUS_MEND_P"}, 5, 16772676, "Support"),
            new SkillTreeDefs.Node("JUS_PULSE_PP", 3, 3, new String[]{"JUS_PULSE_P"}, 5, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_SURGE_P", 4, 3, new String[]{"JUS_SURGE"}, 4, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_BIND", 5, 3, new String[]{"JUS_BARRIER"}, 3, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_CHANNEL", 6, 3, new String[]{"JUS_SURGE"}, 8, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_SURGE_PP", 4, 4, new String[]{"JUS_SURGE_P"}, 6, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_BIND_P", 5, 4, new String[]{"JUS_BIND"}, 3, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_APEX", 7, 4, new String[]{"JUS_BIND", "JUS_CHANNEL"}, 10, 16772676, "Attack"),
            new SkillTreeDefs.Node("JUS_BIND_PP", 5, 5, new String[]{"JUS_BIND_P"}, 5, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_APEX_P", 7, 5, new String[]{"JUS_APEX"}, 5, 16768256, "Attack"),
            new SkillTreeDefs.Node("JUS_APEX_PP", 7, 6, new String[]{"JUS_APEX_P"}, 7, 16768256, "Attack"),
            new SkillTreeDefs.Node("STAT_OFF_1", 8, 2, new String[]{"JUS_STRIKE"}, 2, 13412864, "Stats"),
            new SkillTreeDefs.Node("STAT_SPD_1", 9, 2, new String[]{"JUS_STRIKE"}, 2, 13412864, "Stats"),
            new SkillTreeDefs.Node("STAT_OFF_2", 8, 3, new String[]{"STAT_OFF_1"}, 4, 11175936, "Stats"),
            new SkillTreeDefs.Node("STAT_SPD_2", 9, 3, new String[]{"STAT_SPD_1"}, 4, 11175936, "Stats"),
            new SkillTreeDefs.Node("STAT_OFF_3", 8, 4, new String[]{"STAT_OFF_2"}, 8, 8939008, "Stats"),
            new SkillTreeDefs.Node("STAT_SPD_3", 9, 4, new String[]{"STAT_SPD_2"}, 8, 8939008, "Stats")
         )
      );
      buildSoulTree(
         SoulType.BRAVERY,
         Arrays.asList(
            new SkillTreeDefs.Node("BRA_STRIKE", 2, 0, new String[0], 1, 16746496, "Root"),
            new SkillTreeDefs.Node("BRA_MEND", 0, 1, new String[]{"BRA_STRIKE"}, 1, 16755268, "Support"),
            new SkillTreeDefs.Node("BRA_STRIKE_P", 2, 1, new String[]{"BRA_STRIKE"}, 3, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_PULSE", 3, 1, new String[]{"BRA_STRIKE"}, 2, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_BARRIER", 1, 1, new String[]{"BRA_STRIKE"}, 2, 16755268, "Support"),
            new SkillTreeDefs.Node("BRA_MEND_P", 0, 2, new String[]{"BRA_MEND"}, 3, 16755268, "Support"),
            new SkillTreeDefs.Node("BRA_STRIKE_PP", 2, 2, new String[]{"BRA_STRIKE_P"}, 5, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_PULSE_P", 3, 2, new String[]{"BRA_PULSE"}, 3, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_SURGE", 4, 2, new String[]{"BRA_PULSE"}, 3, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_MEND_PP", 0, 3, new String[]{"BRA_MEND_P"}, 5, 16755268, "Support"),
            new SkillTreeDefs.Node("BRA_PULSE_PP", 3, 3, new String[]{"BRA_PULSE_P"}, 5, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_SURGE_P", 4, 3, new String[]{"BRA_SURGE"}, 4, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_BIND", 5, 3, new String[]{"BRA_PULSE"}, 3, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_CHANNEL", 6, 3, new String[]{"BRA_BARRIER"}, 8, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_SURGE_PP", 4, 4, new String[]{"BRA_SURGE_P"}, 6, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_APEX", 7, 4, new String[]{"BRA_BIND", "BRA_CHANNEL"}, 10, 16755268, "Attack"),
            new SkillTreeDefs.Node("BRA_APEX_P", 7, 5, new String[]{"BRA_APEX"}, 5, 16746496, "Attack"),
            new SkillTreeDefs.Node("BRA_APEX_PP", 7, 6, new String[]{"BRA_APEX_P"}, 7, 16746496, "Attack"),
            new SkillTreeDefs.Node("STAT_OFF_1", 8, 2, new String[]{"BRA_STRIKE"}, 2, 13395456, "Stats"),
            new SkillTreeDefs.Node("STAT_SPD_1", 9, 2, new String[]{"BRA_STRIKE"}, 2, 13395456, "Stats"),
            new SkillTreeDefs.Node("STAT_LUCK_1", 8, 3, new String[]{"STAT_OFF_1"}, 2, 13395456, "Stats"),
            new SkillTreeDefs.Node("STAT_OFF_2", 9, 3, new String[]{"STAT_SPD_1"}, 4, 11158528, "Stats"),
            new SkillTreeDefs.Node("STAT_SPD_2", 8, 4, new String[]{"STAT_LUCK_1"}, 4, 11158528, "Stats"),
            new SkillTreeDefs.Node("STAT_LUCK_2", 9, 4, new String[]{"STAT_OFF_2"}, 8, 8921600, "Stats")
         )
      );
      buildSoulTree(
         SoulType.KINDNESS,
         Arrays.asList(
            new SkillTreeDefs.Node("KIN_STRIKE", 2, 0, new String[0], 1, 65280, "Root"),
            new SkillTreeDefs.Node("KIN_MEND", 0, 1, new String[]{"KIN_STRIKE"}, 1, 4521796, "Support"),
            new SkillTreeDefs.Node("KIN_STRIKE_P", 2, 1, new String[]{"KIN_STRIKE"}, 3, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_PULSE", 3, 1, new String[]{"KIN_STRIKE"}, 2, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_MEND_P", 0, 2, new String[]{"KIN_MEND"}, 3, 4521796, "Support"),
            new SkillTreeDefs.Node("KIN_BARRIER", 1, 2, new String[]{"KIN_MEND"}, 2, 4521796, "Support"),
            new SkillTreeDefs.Node("KIN_STRIKE_PP", 2, 2, new String[]{"KIN_STRIKE_P"}, 5, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_PULSE_P", 3, 2, new String[]{"KIN_PULSE"}, 3, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_SURGE", 4, 2, new String[]{"KIN_PULSE"}, 3, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_MEND_PP", 0, 3, new String[]{"KIN_MEND_P"}, 5, 4521796, "Support"),
            new SkillTreeDefs.Node("KIN_PULSE_PP", 3, 3, new String[]{"KIN_PULSE_P"}, 5, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_SURGE_P", 4, 3, new String[]{"KIN_SURGE"}, 4, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_BIND", 5, 3, new String[]{"KIN_SURGE"}, 3, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_SURGE_PP", 4, 4, new String[]{"KIN_SURGE_P"}, 6, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_CHANNEL", 6, 4, new String[]{"KIN_BARRIER", "KIN_BIND"}, 8, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_APEX", 7, 5, new String[]{"KIN_CHANNEL"}, 10, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_APEX_P", 7, 6, new String[]{"KIN_APEX"}, 5, 65280, "Attack"),
            new SkillTreeDefs.Node("KIN_APEX_PP", 7, 7, new String[]{"KIN_APEX_P"}, 7, 65280, "Attack"),
            new SkillTreeDefs.Node("STAT_VIT_1", 8, 2, new String[]{"KIN_MEND"}, 2, 2276130, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_1", 9, 2, new String[]{"KIN_MEND"}, 2, 2276130, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_2", 8, 3, new String[]{"STAT_VIT_1"}, 4, 1148945, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_2", 9, 3, new String[]{"STAT_IQ_1"}, 4, 1148945, "Stats"),
            new SkillTreeDefs.Node("STAT_VIT_3", 8, 4, new String[]{"STAT_VIT_2"}, 8, 26112, "Stats"),
            new SkillTreeDefs.Node("STAT_IQ_3", 9, 4, new String[]{"STAT_IQ_2"}, 8, 26112, "Stats")
         )
      );
   }

   public static record GlobalNode(SkillTreeDefs.Node node, String groupId, float worldX, float worldY) {
      public String skill() {
         return this.node.skill();
      }
   }

   public static record GroupPlacement(String groupId, float angleDeg, float rowSpacing, float colSpacing) {
   }

   public static record Node(String skill, int col, int row, String[] prereqs, int cost, int color, String branch) {
   }
}
