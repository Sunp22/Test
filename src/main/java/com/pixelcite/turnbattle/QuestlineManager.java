package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class QuestlineManager {
   public static final String ORI_ID = "ori";
   public static final String AVERY_ID = "avery";
   public static final String HAZEL_ID = "hazel";
   public static final List<QuestlineManager.QuestlineMeta> ALL_QUESTLINES = List.of(
      new QuestlineManager.QuestlineMeta(
         "ori", "Ori", "A kind, magic-talented girl being chased by cultists. She joins your cause if you help her.", 1, 2, new String[0], 0
      ),
      new QuestlineManager.QuestlineMeta("avery", "Avery", "Coming soon.", 1, 0, new String[]{"ori"}, 7),
      new QuestlineManager.QuestlineMeta("hazel", "Hazel", "Coming soon.", 1, 0, new String[]{"ori"}, 7)
   );
   private static final String[] ORI_ANTONIO_NORMAL = new String[]{
      "???\u001f* HEY! WATCH IT!!!!!!!!!! YOU STEPPED ON ME, YOU CREEP!",
      "\u001f* You look down at the grass. You can\u2019t make out anything.",
      "???\u001f* HEYYYYY?????? ARE YOU LISTENING TO ME?!",
      "\u001f* You nod.",
      "???\u001f* You. YOU. You PEOPLE think you can STEP ALL OVER US?! ALL OVER ME?! DO YOU KNOW WHO I AM????????",
      "\u001f* If you knew you wouldn\u2019t get interrupted, you\u2019d probably say something along the lines of \u201cI can\u2019t even see you dude.\u201d The thought crossing your mind makes you chuckle.",
      "???\u001f* Oh. OH. OKAY, THAT\u2019S IT! YOU WANNA LAUGH AT ME? LAUGH AT THE BIG MAN HIMSELF??? ALRIGHT, WE\u2019LL SEE HOW MUCH YOU LAUGH WHEN YOU FACE ME!!!",
      "Antonio\u001f* BIG. ANTONIO. ANTONIO THE ANT KING."
   };
   private static final String[] ORI_ANTONIO_RUTHLESS = new String[]{
      "???\u001f* HEY!!! WATCH I-",
      "\u001f* You stepped on something. You grind your shoe into the dirt.",
      "???\u001f* OW- HEY! OW- STOP!!!!!",
      "\u001f* It\u2019s alive. Fix that."
   };
   private static final String[] ORI_ANTONIO_SPARE = new String[]{
      "Antonio\u001f* Yo, Sorry I crashed out bro. I totally respect your strength. We big guys (guy in a gender neutral way) gotta stick together. I, like, respect your strength. I gotta go now. My people need me man (also gender neutral)",
      "\u001f* \u2026 Okay."
   };
   private static final String[] ORI_ANTONIO_DEFEAT = new String[]{
      "Antonio\u001f* YOU... FREAK!!! I\u2019LL BE BACK!!! YOU\u2019LL RUE THE DAY. You\u2019ll RUE IT!!!!"
   };
   private static final String[] ORI_ANTONIO_RUTHLESS_END = new String[]{
      "Antonio\u001f* \u2026 Wait-", "\u001f* You stomp. You stomp over and over again. Repeatedly until you stop hearing any noise."
   };
   private static final String[] ORI_PRE_BATTLE = new String[]{
      "\u001f* You notice a person in the distance. She's being... chased? By a group of hooded figures.",
      "\u001f* They race towards you, and she hides behind you.",
      "\u001f* The cultists draw daggers."
   };
   private static final String[] ORI_POST_NORMAL = new String[]{"\u001f* The cultists run off. You look at the person they were chasing."};
   private static final String[] ORI_POST_RUTHLESS = new String[]{
      "\u001f* The enemies drop to the ground. You slowly turn to face the person they were chasing. She flinches."
   };
   private static final String[] ORI_INTRO_DIALOGUE = new String[]{
      "Ori\u001f* Um- Hi! I'm... sorry, um. Sorry, about all of that. Th- Thank you for saving me! They, um...",
      "\u001f* She looks like she's trying to think of a way to explain herself, but fails completely.",
      "Ori\u001f* ... Anyways! Hi! I'm Ori, uh... Sorry I couldn't be of any help. I just, I'm not really strong. My mom always told me I have a lot of potential in magic, but... Hah, still can't figure it out!",
      "CHOICE\u001fRecruit\u001fSay goodbye."
   };
   private static final String[] ORI_RECRUIT_DIALOGUE = new String[]{"Ori\u001f* Oh- um, come along with you? Thank you! Uhm- you won't regret it! I think!"};
   private static final String[] ORI_GOODBYE_DIALOGUE = new String[]{
      "Ori\u001f* Right! Thank you again for your help! I, uh- I hope to see you again someday!"
   };
   private static final Map<UUID, String> pendingBattle = new HashMap<>();
   private static final Map<UUID, Integer> antonioIds = new HashMap<>();
   private static final Map<UUID, Boolean> antonioSpared = new HashMap<>();
   private static final Map<UUID, String> pendingChoice = new HashMap<>();
   private static final Map<UUID, Boolean> choiceResult = new HashMap<>();

   public static void onQuestlineStart(ServerPlayerEntity player, String id) {
      if (id.equals("ori")) {
         startOriNode0(player);
      }
   }

   public static void onQuestlineChoice(ServerPlayerEntity player, int choiceIndex) {
      UUID uid = player.getUuid();
      String ql = pendingChoice.remove(uid);
      if (ql != null) {
         if (ql.equals("ori")) {
            handleOriChoice(player, choiceIndex);
         }
      }
   }

   public static void onQuestlineBattleEnd(ServerPlayerEntity player, boolean won) {
      UUID uid = player.getUuid();
      String ql = pendingBattle.remove(uid);
      if (ql != null) {
         BattleData data = BattleData.get(player.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(uid);
         if (!won) {
            prog.questlineNodeDone = false;
            data.markDirty();
            TurnBattleMod.sendQuestlineState(player, prog);
         } else {
            if (ql.equals("ori")) {
               if (prog.questlineNodeIndex == 0) {
                  prog.questlineNodeDone = true;
                  prog.questlineNodeIndex = 1;
                  prog.questlineNodeDone = false;
                  data.markDirty();
                  boolean spared = Boolean.TRUE.equals(antonioSpared.remove(uid));
                  String[] lines = MoralityManager.isRuthless(prog) ? ORI_ANTONIO_RUTHLESS_END : (spared ? ORI_ANTONIO_SPARE : ORI_ANTONIO_DEFEAT);
                  player.setInvulnerable(true);
                  BattleManager.playersInStoryDialogue.add(uid);
                  BattleManager.storyDialogueEnteredTick.put(uid, player.getServer().getOverworld().getTime());
                  PacketByteBuf buf2 = PacketByteBufs.create();
                  buf2.writeInt(lines.length);

                  for (String b : lines) {
                     buf2.writeString(b, 4096);
                  }

                  buf2.writeBoolean(false);
                  Net121.sendToClient(player, TurnBattleMod.STORY_DIALOGUE, buf2);
                  TurnBattleMod.sendQuestlineState(player, prog);
               } else if (prog.questlineNodeIndex == 1) {
                  prog.questlineNodeDone = true;
                  data.markDirty();
                  advanceOriToBattleResult(player, prog, data);
               }
            }
         }
      }
   }

   public static boolean isQuestlineBattle(UUID uid) {
      return pendingBattle.containsKey(uid);
   }

   private static void startOriNode0(ServerPlayerEntity player) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      prog.questlineNodeIndex = 0;
      prog.questlineNodeDone = false;
      data.markDirty();
      sendDialogueAndBattle(player, MoralityManager.isRuthless(prog) ? ORI_ANTONIO_RUTHLESS : ORI_ANTONIO_NORMAL, "ori");
      TurnBattleMod.sendQuestlineState(player, prog);
   }

   private static void sendDialogueAndBattle(ServerPlayerEntity player, String[] beats, String questlineId) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(beats.length);

      for (String b : beats) {
         buf.writeString(b, 4096);
      }

      buf.writeBoolean(false);
      Net121.sendToClient(player, TurnBattleMod.STORY_DIALOGUE, buf);
      pendingBattle.put(player.getUuid(), questlineId);
   }

   public static void onDialogueDoneForQuestline(ServerPlayerEntity player) {
      UUID uid = player.getUuid();
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(uid);
      if (prog.activeQuestlineId != null) {
         String ql = prog.activeQuestlineId;
         if (ql.equals("ori")) {
            int node = prog.questlineNodeIndex;
            if (node == 0 && pendingBattle.containsKey(uid)) {
               spawnAntonioBattle(player);
            } else if (node == 1 && pendingBattle.containsKey(uid)) {
               spawnCultistBattle(player);
            }
         }
      }
   }

   private static void spawnAntonioBattle(ServerPlayerEntity player) {
      float hp = MobLevelManager.computeMobMaxHp(5);
      BattleManager.tryStartVirtual(player, "Antonio", "turnbattle:story_antonio", hp, 0.0F);
      BattleState state = BattleManager.activeBattles.get(player.getUuid());
      if (state != null) {
         state.questlineBattleId = "ori";
      }

      antonioIds.put(player.getUuid(), Integer.MIN_VALUE);
      pendingBattle.put(player.getUuid(), "ori");
   }

   public static void recordAntonioSpared(UUID uid, boolean spared) {
      antonioSpared.put(uid, spared);
   }

   private static void spawnCultistBattle(ServerPlayerEntity player) {
      float hp = MobLevelManager.computeMobMaxHp(10);
      BattleManager.tryStartVirtual(player, "Cultist", "turnbattle:story_antonio", hp, 3.0F);
      BattleState state = BattleManager.activeBattles.get(player.getUuid());
      if (state != null) {
         state.questlineBattleId = "ori";
         BattleManager.addExtraVirtualEnemy(state, "Cultist", hp, 3.0F);
      }

      pendingBattle.put(player.getUuid(), "ori");
   }

   private static void advanceOriToBattleResult(ServerPlayerEntity player, BattleData.PlayerProgress prog, BattleData data) {
      prog.questlineNodeIndex = 2;
      data.markDirty();
      String[] postLine = MoralityManager.isRuthless(prog) ? ORI_POST_RUTHLESS : ORI_POST_NORMAL;
      String[] beats = new String[postLine.length + ORI_INTRO_DIALOGUE.length];
      System.arraycopy(postLine, 0, beats, 0, postLine.length);
      System.arraycopy(ORI_INTRO_DIALOGUE, 0, beats, postLine.length, ORI_INTRO_DIALOGUE.length);
      player.setInvulnerable(true);
      BattleManager.playersInStoryDialogue.add(player.getUuid());
      BattleManager.storyDialogueEnteredTick.put(player.getUuid(), player.getServer().getOverworld().getTime());
      pendingChoice.put(player.getUuid(), "ori");
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(beats.length);

      for (String b : beats) {
         buf.writeString(b, 4096);
      }

      buf.writeBoolean(false);
      Net121.sendToClient(player, TurnBattleMod.STORY_DIALOGUE, buf);
      TurnBattleMod.sendQuestlineState(player, prog);
   }

   private static void handleOriChoice(ServerPlayerEntity player, int choiceIndex) {
      UUID uid = player.getUuid();
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(uid);
      boolean recruited = choiceIndex == 0;
      choiceResult.put(uid, recruited);
      String[] followUp = recruited ? ORI_RECRUIT_DIALOGUE : ORI_GOODBYE_DIALOGUE;
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(followUp.length);

      for (String b : followUp) {
         buf.writeString(b, 4096);
      }

      buf.writeBoolean(false);
      Net121.sendToClient(player, TurnBattleMod.STORY_DIALOGUE, buf);
      if (recruited) {
         RecruitData rd = RecruitData.get(player.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(uid);
         RecruitData.RecruitState rs = pr.get(Recruit.ORI);
         if (!rs.unlocked) {
            rs.unlocked = true;
            rs.level = Recruit.ORI.startLevel;
            rs.stats.unspent = Recruit.ORI.startLevel * 3;
            rs.skillPoints = Recruit.ORI.startLevel * 3;
         }

         pr.activeRecruit = Recruit.ORI.ordinal();
         rd.markDirty();
         TurnBattleMod.sendRecruitInfo(player);
      }

      prog.questlineNodeIndex = 2;
      prog.questlineNodeDone = true;
      prog.completedQuestlines.add("ori");
      prog.activeQuestlineId = null;
      data.markDirty();
      TurnBattleMod.sendQuestlineState(player, prog);
   }

   public static void retryQuestlineBattle(ServerPlayerEntity player) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      if (prog.activeQuestlineId != null) {
         if ("ori".equals(prog.activeQuestlineId)) {
            if (prog.questlineNodeIndex == 0) {
               sendDialogueAndBattle(player, MoralityManager.isRuthless(prog) ? ORI_ANTONIO_RUTHLESS : ORI_ANTONIO_NORMAL, "ori");
            } else if (prog.questlineNodeIndex == 1) {
               sendDialogueAndBattle(player, ORI_PRE_BATTLE, "ori");
            }
         }
      }
   }

   public static record QuestlineMeta(String id, String name, String description, int recruitsAvailable, int questCount, String[] prereqs, int levelRequired) {
   }
}
