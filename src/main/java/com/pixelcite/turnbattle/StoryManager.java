package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class StoryManager {
   public static final int TOTAL_CHAPTERS = 5;
   public static final String[] CHAPTER_NAMES = new String[]{"Chapter 1", "Chapter 2", "Chapter 3", "Chapter 4", "Chapter 5"};
   public static final String[][] QUEST_NAMES = new String[][]{
      {"The Bare Essentials", "Practicing The Basics", "The Feeling Of Being Watched?", "Villager's Dilemma", "Pillager's Blight"},
      {"Kris Boss Test"},
      {"Coming Soon"},
      {"Coming Soon"},
      {"Coming Soon"}
   };
   public static final String[][] QUEST_DESCS = new String[][]{
      {
            "Craft a Crafting Table and a Furnace.",
            "Defeat a practice enemy in a training battle.",
            "Walk on top of a grass block and defeat a strange foe.",
            "Speak to a Villager.",
            "Defeat or Spare 4 Pillagers."
      },
      {"(Developer access only)"},
      {"Coming soon."},
      {"Coming soon."},
      {"Coming soon."}
   };
   public static final int[][] QUEST_EXP = new int[][]{{20, 30, 60, 40, 30}, {0}, {0}, {0}, {0}};
   private static final String[] ANTONIO_SPARE = new String[]{
      "Antonio\u001f* Yo, Sorry I crashed out bro. I totally respect your strength. We big guys (guy in a gender neutral way) gotta stick together. I, like, respect your strength. I gotta go now. My people need me man (also gender neutral)",
      "\u001f* \u2026 Okay."
   };
   private static final String[] ANTONIO_DEFEAT = new String[]{
      "Antonio\u001f* YOU... FREAK!!! I\u2019LL BE BACK!!! YOU\u2019LL RUE THE DAY. You\u2019ll RUE IT!!!!"
   };
   private static final String[] ANTONIO_RUTHLESS = new String[]{
      "Antonio\u001f* \u2026 Wait-",
      "\u001f* You stomp. You stomp over and over again. Repeatedly until you stop hearing any noise. The dirt piles around the soles of your shoe as you stomp repeatedly. And once you\u2019re satisfied, you move your leg back. You\u2019re not sure if you got him, but you got more important things to do than try to check."
   };
   private static final String[] CH2Q1_PRE = new String[]{
      "\u001f* You slowly sit up, holding your head. You're reeling, confused and in a new place. Looking around, things look\u2026 different.",
      "\u001f* A voice in the distance catches your attention.",
      "???\u001f* \u2026 Hey, why didn't you tell us you could take us to other Dark Worlds? That would've made things a LOT easier."
   };
   private static final String CH2Q1_CHOICE_NORMAL_OPT1 = "[{vessel}]";
   private static final String CH2Q1_CHOICE_NORMAL_OPT2 = "Kris";
   private static final String CH2Q1_CHOICE_RUTHLESS_OPT2 = "I\u2019m Kris.";
   private static final String[] CH2Q1_BRANCH_VESSEL = new String[]{"Susie\u001f* Cool. Alrighty, lead the way, {vessel}!"};
   private static final String[] CH2Q1_BRANCH_KRIS = new String[]{
      "Susie\u001f* \u2026",
      "Susie\u001f* Your name is, uhhh\u2026. Kris?????",
      "Susie\u001f* That\u2019s gonna get way too confusing. I\u2019m gonna call you, like, K or something."
   };
   private static final String[] CH2Q1_BRANCH_KRIS_RUTHLESS = new String[]{
      "Susie\u001f* \u2026",
      "Susie\u001f* Yeah, no. I\u2019m not calling you that.",
      "Susie\u001f* (Kinda freaky that they pulled off such an accurate impression of their voice.)"
   };
   private static final Set<UUID> ch2q1Played = new HashSet<>();
   private static final Map<UUID, Boolean> pendingCh2Q1Choice = new HashMap<>();
   private static final String[] CH2Q2_PRE = new String[]{
      "Susie\u001f* Hey, that looks good! We can totally take this to Ralsei!",
      "\u001f* You ask Susie where she\u2019ll be after this.",
      "Susie\u001f* \u2026 Huh? Oh, uh\u2026 I guess I\u2019m probably gonna hang out with Kris and Ralsei for a bit after this.",
      "Susie\u001f* Probably gonna head to Castle Town.",
      "Susie\u001f* \u2026 You alright? You look, uh, sad."
   };
   private static final String CH2Q2_CHOICE = "CHOICE\u001fI\u2019m okay.\u001fLonely back home.";
   private static final String[] CH2Q2_BRANCH_OK = new String[]{
      "Susie\u001f* If you say so, dude. I don\u2019t like to pry, or whatever.",
      "Susie\u001f* That\u2019s Ralsei\u2019s job, heh.",
      "Susie\u001f* \u2026 Alrighty, let\u2019s get out of here. It\u2019s dusty and hard to breathe."
   };
   private static final String[] CH2Q2_BRANCH_LONELY = new String[]{
      "Susie\u001f* \u2026 Oh, uhhh\u2026 Maybe\u2026 we can find some way to like, bring you to Castle Town! I mean, you\u2019re\u2026 not a Darkner, but I\u2019m sure Ralsei can think of something. He\u2019s like, smart and stuff.",
      "Susie\u001f* You seem cool, so I\u2019m sure Ralsei would help out.",
      "Susie\u001f* \u2026 Alrighty, let\u2019s get out of here. It\u2019s dusty and hard to breathe."
   };
   private static final String[] CH2Q2_RUTHLESS_PRE = new String[]{
      "Susie\u001f* Hey, looks like we found it. Cool.",
      "\u001f* \u2026",
      "Susie\u001f* So, uh\u2026 I get they\u2019re like, zombies and stuff. And Ralsei said they\u2019re not Darkners or, like, \u201csentient\u201d at all.",
      "Susie\u001f* (Whatever that word means.)",
      "Susie\u001f* \u2026 But, uh\u2026 Going for the throat every single time is a little\u2026 harsh, right?",
      "\u001f* \u2026",
      "Susie\u001f* Not a talker? That\u2019s fine I guess. Gonna head back up now. See ya."
   };
   private static final String[] KRIS_AMBUSH = new String[]{
      "\u001f* You briefly catch a glimmer out of the corner of your eye and duck, a blade narrowly missing you as you spin around, facing that one human from earlier.",
      "\u001f* They sorta look\u2026 sick?"
   };
   private static final Set<UUID> ch2q2DiamondPlayed = new HashSet<>();
   private static final Map<UUID, Boolean> pendingCh2Q2Choice = new HashMap<>();
   private static final Map<UUID, Integer> krisEntityIds = new HashMap<>();
   private static final Map<UUID, Vec3d> ch2q2PostDialoguePos = new HashMap<>();
   public static final Set<UUID> pendingKrisAmbushArm = new HashSet<>();
   public static final Set<UUID> krisRetry = new HashSet<>();
   private static final Map<UUID, Integer> villagerBattleIds = new HashMap<>();
   public static final Set<UUID> pendingKrisBattle = new HashSet<>();
   private static final String[] ORI_CULTIST_INTRO = new String[]{
      "\u001f* You notice a person in the distance. She's being... chased? By a group of hooded figures.",
      "\u001f* They race towards you, and she hides behind you. The cultists draw daggers."
   };
   private static final String[] ORI_POST_NORMAL = new String[]{
      "\u001f* The cultists run off. You look at the person they were chasing.",
      "Ori\u001f* Um- Hi! I'm... sorry, um. Sorry, about all of that. Th- Thank you for saving me! They, um...",
      "\u001f* She looks like she's trying to think of a way to explain herself, but fails completely.",
      "Ori\u001f* ... Anyways! Hi! I'm Ori, uh... Sorry I couldn't be of any help. I just, I'm not really strong. My mom always told me I have a lot of potential in magic, but... Hah, still can't figure it out!",
      "CHOICE\u001fRecruit\u001fSay goodbye."
   };
   private static final String[] ORI_POST_RUTHLESS = new String[]{
      "\u001f* The enemies drop to the ground. You slowly turn to face the person they were chasing. She flinches.",
      "Ori\u001f* Um- Hi! I'm... sorry, um. Sorry, about all of that. Th- Thank you for saving me! They, um...",
      "\u001f* She looks like she's trying to think of a way to explain herself, but fails completely.",
      "Ori\u001f* ... Anyways! Hi! I'm Ori, uh... Sorry I couldn't be of any help. I just, I'm not really strong. My mom always told me I have a lot of potential in magic, but... Hah, still can't figure it out!",
      "CHOICE\u001fRecruit\u001fSay goodbye."
   };
   private static final String[] ORI_RECRUIT = new String[]{"Ori\u001f* Oh- um, come along with you? Thank you! Uhm- you won't regret it! I think!"};
   private static final String[] ORI_GOODBYE = new String[]{"Ori\u001f* Right! Thank you again for your help! I, uh- I hope to see you again someday!"};

   public static void tick(MinecraftServer server) {
      for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
         BattleData data = BattleData.get(server);
         BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
         if (prog.storyQuestStarted && !prog.storyQuestComplete) {
            int ch = prog.storyUnlockedChapter;
            int q = prog.storyQuestIndex;
            if (ch == 1 && q == 1 && !prog.storyQuestComplete) {
               tickCh2Q2Ambush(player);
            }
         }
      }
   }

   public static void onDarkWorldEnter(ServerPlayerEntity player) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      UUID uid = player.getUuid();
      if (prog.storyUnlockedChapter == 1 && prog.storyQuestIndex == 0) {
         if (prog.storyQuestStarted) {
            if (!ch2q1Played.contains(uid)) {
               if (!BattleManager.playersInStoryDialogue.contains(uid)) {
                  if (!BattleManager.activeBattles.containsKey(uid)) {
                     ch2q1Played.add(uid);
                     pendingCh2Q1Choice.put(uid, MoralityManager.isRuthless(prog));
                     String vessel = prog.vesselName != null && !prog.vesselName.isEmpty() ? prog.vesselName : "you";
                     String opt1 = "[{vessel}]".replace("{vessel}", vessel);
                     String opt2 = MoralityManager.isRuthless(prog) ? "I\u2019m Kris." : "Kris";
                     String choiceBeat = "CHOICE\u001f" + opt1 + "\u001f" + opt2;
                     String[] beats = new String[CH2Q1_PRE.length + 1];
                     System.arraycopy(CH2Q1_PRE, 0, beats, 0, CH2Q1_PRE.length);
                     beats[CH2Q1_PRE.length] = choiceBeat;
                     player.setInvulnerable(true);
                     BattleManager.playersInStoryDialogue.add(uid);
                     BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
                     sendDialogue(player, beats, false);
                  }
               }
            }
         }
      }
   }

   public static void onStoryChoice(ServerPlayerEntity player, int choiceIndex) {
      UUID uid = player.getUuid();
      Boolean ruthless1 = pendingCh2Q1Choice.remove(uid);
      if (ruthless1 != null) {
         handleCh2Q1Choice(player, uid, choiceIndex, ruthless1);
      } else {
         Boolean ruthless2 = pendingCh2Q2Choice.remove(uid);
         if (ruthless2 != null) {
            handleCh2Q2Choice(player, uid, choiceIndex);
         }
      }
   }

   private static void handleCh2Q1Choice(ServerPlayerEntity player, UUID uid, int choiceIndex, boolean ruthless) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(uid);
      String vessel = prog.vesselName != null && !prog.vesselName.isEmpty() ? prog.vesselName : "you";
      String[] branch;
      if (choiceIndex == 0) {
         branch = new String[CH2Q1_BRANCH_VESSEL.length];

         for (int i = 0; i < branch.length; i++) {
            branch[i] = CH2Q1_BRANCH_VESSEL[i].replace("{vessel}", vessel);
         }
      } else {
         branch = ruthless ? CH2Q1_BRANCH_KRIS_RUTHLESS : CH2Q1_BRANCH_KRIS;
      }

      data.addExp(uid, QUEST_EXP[1][0]);
      data.markDirty();
      MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
      prog.storyQuestIndex = 1;
      prog.storyQuestStarted = true;
      prog.storyQuestComplete = false;
      data.markDirty();
      sendState(player, prog);
      player.setInvulnerable(true);
      BattleManager.playersInStoryDialogue.add(uid);
      BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
      sendDialogue(player, branch, false);
   }

   private static void handleCh2Q2Choice(ServerPlayerEntity player, UUID uid, int choiceIndex) {
      String[] branch = choiceIndex == 0 ? CH2Q2_BRANCH_OK : CH2Q2_BRANCH_LONELY;
      pendingKrisAmbushArm.add(uid);
      player.setInvulnerable(true);
      BattleManager.playersInStoryDialogue.add(uid);
      BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
      sendDialogue(player, branch, false);
   }

   public static boolean isVillagerBattle(ServerPlayerEntity player, int entityId) {
      Integer v = villagerBattleIds.get(player.getUuid());
      return v != null && v == entityId;
   }

   public static void onVillagerBattleEnd(ServerPlayerEntity player) {
      villagerBattleIds.remove(player.getUuid());
      completeQ4(player);
   }

   private static void completeQ4(ServerPlayerEntity player) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      UUID uid = player.getUuid();
      data.addExp(uid, QUEST_EXP[0][3]);
      data.markDirty();
      MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
      prog.storyQuestIndex = 4;
      prog.storyQuestStarted = true;
      prog.storyQuestComplete = false;
      data.markDirty();
      sendState(player, prog);
   }

   public static boolean onDiamondOreInspect(ServerPlayerEntity player) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      UUID uid = player.getUuid();
      if (!DarkFountainManager.isDarkWorld(player.getWorld().getRegistryKey())) {
         return false;
      } else if (BattleManager.playersInStoryDialogue.contains(uid)) {
         return false;
      } else if (BattleManager.activeBattles.containsKey(uid)) {
         return false;
      } else if (krisRetry.remove(uid)) {
         player.setInvulnerable(true);
         BattleManager.playersInStoryDialogue.add(uid);
         BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
         pendingKrisBattle.add(uid);
         sendDialogue(player, KRIS_AMBUSH, false);
         return true;
      } else if (prog.storyUnlockedChapter != 1 || prog.storyQuestIndex != 1) {
         return false;
      } else if (!prog.storyQuestStarted || prog.storyQuestComplete) {
         return false;
      } else if (ch2q2DiamondPlayed.contains(uid)) {
         return false;
      } else {
         ch2q2DiamondPlayed.add(uid);
         if (MoralityManager.isRuthless(prog)) {
            player.setInvulnerable(true);
            BattleManager.playersInStoryDialogue.add(uid);
            BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
            pendingKrisAmbushArm.add(uid);
            sendDialogue(player, CH2Q2_RUTHLESS_PRE, false);
         } else {
            pendingCh2Q2Choice.put(uid, false);
            String[] beats = new String[CH2Q2_PRE.length + 1];
            System.arraycopy(CH2Q2_PRE, 0, beats, 0, CH2Q2_PRE.length);
            beats[CH2Q2_PRE.length] = "CHOICE\u001fI\u2019m okay.\u001fLonely back home.";
            player.setInvulnerable(true);
            BattleManager.playersInStoryDialogue.add(uid);
            BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
            sendDialogue(player, beats, false);
         }

         return true;
      }
   }

   public static void armKrisAmbush(ServerPlayerEntity player) {
      ch2q2PostDialoguePos.put(player.getUuid(), player.getPos());
   }

   public static void spawnKrisAndBattle(ServerPlayerEntity player) {
      float maxHp = MobLevelManager.computeMobMaxHp(14);
      BattleManager.tryStartVirtual(player, "Kris", "turnbattle:story_kris", maxHp, 10.0F);
      krisEntityIds.put(player.getUuid(), Integer.MIN_VALUE);
   }

   public static boolean isKrisBattle(ServerPlayerEntity player, int entityId) {
      Integer k = krisEntityIds.get(player.getUuid());
      return k != null && k == entityId;
   }

   public static void onKrisBattleEnd(ServerPlayerEntity player, String outcome) {
      UUID uid = player.getUuid();
      krisEntityIds.remove(uid);
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(uid);
      if (!"win".equals(outcome) && !"mercy".equals(outcome)) {
         prog.storyQuestComplete = true;
         data.markDirty();
         sendState(player, prog);
      } else {
         data.addExp(uid, QUEST_EXP[1][1]);
         data.markDirty();
         MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
         prog.storyChapterDone[1] = true;
         prog.storyUnlockedChapter = 2;
         prog.storyQuestIndex = 0;
         prog.storyQuestStarted = false;
         prog.storyQuestComplete = false;
         ch2q2DiamondPlayed.remove(uid);
         data.markDirty();
         sendState(player, prog);
      }
   }

   private static void retryKris(ServerPlayerEntity player, BattleData.PlayerProgress prog, BattleData data) {
      prog.storyQuestComplete = false;
      data.markDirty();
      sendState(player, prog);
      krisRetry.add(player.getUuid());
   }

   private static void tickCh2Q2Ambush(ServerPlayerEntity player) {
      UUID uid = player.getUuid();
      Vec3d origin = ch2q2PostDialoguePos.get(uid);
      if (origin != null) {
         if (!BattleManager.playersInStoryDialogue.contains(uid)) {
            if (!BattleManager.activeBattles.containsKey(uid)) {
               if (!(player.getPos().distanceTo(origin) < 7.0)) {
                  ch2q2PostDialoguePos.remove(uid);
                  player.setInvulnerable(true);
                  BattleManager.playersInStoryDialogue.add(uid);
                  BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
                  pendingKrisBattle.add(uid);
                  sendDialogue(player, KRIS_AMBUSH, false);
               }
            }
         }
      }
   }

   public static void sendState(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(prog.storyUnlockedChapter);
      buf.writeInt(prog.storyQuestIndex);
      buf.writeBoolean(prog.storyQuestStarted);
      buf.writeBoolean(prog.storyQuestComplete);

      for (int i = 0; i < 5; i++) {
         buf.writeBoolean(prog.storyChapterDone[i]);
      }

      buf.writeBoolean(TurnBattleConfig.get().storyMode);
      Net121.sendToClient(player, TurnBattleMod.STORY_STATE, buf);
   }

   private static void sendDialogue(ServerPlayerEntity player, String[] beats, boolean spawnSheepy) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(beats.length);

      for (String b : beats) {
         buf.writeString(b, 4096);
      }

      buf.writeBoolean(spawnSheepy);
      Net121.sendToClient(player, TurnBattleMod.STORY_DIALOGUE, buf);
   }
}
