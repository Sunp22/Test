package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public class BattleNetworking {
   private static boolean toAll = false;

   static void sendBattleStart(ServerPlayerEntity player, BattleState state, BattleData.PlayerProgress prog) {
      PlayerClass cls = prog.playerClass;
      int classOrdinal = cls != null ? cls.ordinal() : -1;
      if (state.logHistory.isEmpty()) {
         String intro = "You encountered " + state.entityName + "!";
         state.logHistory.addLast(intro);
      }

      if (!state.isPvp) {
         BattleManager.reconcileEnemyLiveness(state, player.getServer().getPlayerManager());
      }

      float curPrimaryHp = state.enemyHpMap.getOrDefault(state.primaryEnemyId, state.entityCurrentHp);
      float curPrimaryMaxHp = state.enemyMaxHpMap.getOrDefault(state.primaryEnemyId, state.entityMaxHp);
      String curPrimaryName = state.enemyNames.getOrDefault(state.primaryEnemyId, state.entityName);
      String curPrimaryType = state.enemyTypeKeys.getOrDefault(state.primaryEnemyId, state.entityTypeKey);
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(state.entityNetworkId);
      buf.writeString(curPrimaryName);
      buf.writeString(curPrimaryType);
      buf.writeFloat(curPrimaryMaxHp);
      buf.writeFloat(curPrimaryHp);
      buf.writeFloat(player.getMaxHealth());
      buf.writeFloat(BattleManager.TARGET_HEALTH.getOrDefault(player.getUuid(), player.getHealth()));
      buf.writeFloat(state.getTp(player.getUuid()));
      buf.writeInt(prog.level);
      buf.writeInt(prog.exp);
      buf.writeInt(BattleData.expNeeded(prog.level));
      buf.writeInt(classOrdinal);
      writeParticipants(buf, state, player.getUuid(), player.getServer());
      buf.writeString(state.entityPassive);
      buf.writeBoolean(state.darknessConstricting);
      writeExtraEnemies(buf, state);
      writeEnemyRoster(buf, state);
      buf.writeInt(state.primaryEnemyId);
      buf.writeString(String.join("\n", state.logHistory));
      buf.writeInt(state.battleElement.ordinal());
      buf.writeBoolean(prog.soulType != null);
      if (prog.soulType != null) {
         buf.writeInt(prog.soulType.ordinal());
      }

      buf.writeInt(state.bossLivesRemaining);
      buf.writeInt(state.bossMaxLives);
      buf.writeString(TurnBattleConfig.get().visualStyle != null ? TurnBattleConfig.get().visualStyle : "deltarune");
      buf.writeInt(state.battleStyle != null ? state.battleStyle.ordinal() : 0);
      buf.writeBoolean(BattleStaging.overhaulFor(state, prog));
      Net121.sendToClient(player, TurnBattleMod.BATTLE_START, buf);
      if (BattleManager.queuedTurns(state)) {
         sendBattleLine(player, 0, BattleManager.encounterLine(state), 0);
      }

      TurnBattleMod.sendEquipInfo(player, prog);
      TurnBattleMod.broadcastEquipInfo(state, player.getServer());
      TurnBattleMod.sendPartyInfo(player);
      RogueliteData rogueData = RogueliteData.get(player.getServer());
      RogueliteData.PlayerRogueData rd = rogueData.players.get(player.getUuid());
      if (rd != null && rd.inRun) {
         RogueliteManager.sendRogueSkills(player, rd);
      }
   }

   static void sendPvpBattleStart(ServerPlayerEntity player, BattleState state, BattleData.PlayerProgress prog) {
      UUID opponentUuid = BattleManager.primaryEnemyOf(state, player.getUuid(), player.getServer().getPlayerManager());
      ServerPlayerEntity opponent = player.getServer().getPlayerManager().getPlayer(opponentUuid);
      BattleData.PlayerProgress opProg = BattleData.get(player.getServer()).getOrCreate(opponentUuid);
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(opponent != null ? opponent.getId() : -1);
      buf.writeUuid(opponentUuid);
      buf.writeString(opponent != null ? opponent.getName().getString() : "???");
      buf.writeString("pvp");
      buf.writeFloat(opponent != null ? opponent.getMaxHealth() : 20.0F);
      buf.writeFloat(opponent != null ? opponent.getHealth() : 20.0F);
      buf.writeFloat(player.getMaxHealth());
      buf.writeFloat(BattleManager.TARGET_HEALTH.getOrDefault(player.getUuid(), player.getHealth()));
      buf.writeFloat(state.getTp(player.getUuid()));
      buf.writeInt(prog.level);
      buf.writeInt(prog.exp);
      buf.writeInt(BattleData.expNeeded(prog.level));
      buf.writeInt(prog.playerClass != null ? prog.playerClass.ordinal() : -1);
      writeParticipants(buf, state, player.getUuid(), player.getServer());
      writeOpponents(buf, state, player.getUuid(), player.getServer());
      buf.writeString("");
      buf.writeBoolean(state.darknessConstricting);
      writeExtraEnemies(buf, state);
      writeEnemyRoster(buf, state);
      buf.writeInt(state.primaryEnemyId);
      buf.writeString(String.join("\n", state.logHistory));
      buf.writeInt(Element.NONE.ordinal());
      buf.writeBoolean(prog.soulType != null);
      if (prog.soulType != null) {
         buf.writeInt(prog.soulType.ordinal());
      }

      buf.writeInt(0);
      buf.writeInt(0);
      buf.writeBoolean(opProg.soulType != null);
      if (opProg.soulType != null) {
         buf.writeInt(opProg.soulType.ordinal());
      }

      buf.writeFloat(state.pvpShield.getOrDefault(opponentUuid, 0.0F));
      buf.writeFloat(state.pvpShieldMax.getOrDefault(opponentUuid, 0.0F));
      buf.writeBoolean(state.pvpShieldBroken.getOrDefault(opponentUuid, true));
      buf.writeFloat(state.pvpShield.getOrDefault(player.getUuid(), 0.0F));
      buf.writeFloat(state.pvpShieldMax.getOrDefault(player.getUuid(), 0.0F));
      buf.writeBoolean(state.pvpShieldBroken.getOrDefault(player.getUuid(), true));
      Net121.sendToClient(player, TurnBattleMod.PVP_BATTLE_START, buf);
      TurnBattleMod.sendEquipInfo(player, prog);
      TurnBattleMod.broadcastEquipInfo(state, player.getServer());
      TurnBattleMod.sendPartyInfo(player);
   }

   static void sendPartyMemberTurn(ServerPlayerEntity player, BattleState state) {
      sendPartyMemberTurn(player, state, BattleState.PARTY_MEMBER_UUID);
   }

   static void sendPartyMemberTurn(ServerPlayerEntity player, BattleState state, UUID slotUuid) {
      PlayerClass cls = state.participantClass.getOrDefault(slotUuid, PlayerClass.PSI_USER);
      String name = state.participantNames.getOrDefault(slotUuid, "Ally");
      float partyMemberTp = state.getTp(slotUuid);
      RecruitData.PlayerRecruits pr = RecruitData.get(player.getServer()).getOrCreate(player.getUuid());
      int actingSlotCode = slotUuid.equals(BattleState.PARTY_MEMBER_UUID_2) ? pr.secondActiveRecruit : pr.activeRecruit;
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeString(name);
      buf.writeInt(cls.ordinal());
      buf.writeFloat(partyMemberTp);
      buf.writeInt(actingSlotCode);
      SoulType recruitSoul = null;
      if (actingSlotCode >= 1000) {
         int ci = actingSlotCode - 1000;
         if (ci < pr.customRecruits.size()) {
            recruitSoul = pr.customRecruits.get(ci).soulType;
         }
      }

      buf.writeBoolean(recruitSoul != null);
      if (recruitSoul != null) {
         buf.writeInt(recruitSoul.ordinal());
      }

      writeParticipants(buf, state, player.getUuid(), player.getServer());
      Net121.sendToClient(player, TurnBattleMod.PARTY_MEMBER_TURN, buf);
   }

   static void sendTurnNotice(ServerPlayerEntity player, BattleState state) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      if (state.isPvp) {
         sendPvpBattleStart(player, state, prog);
      } else {
         sendBattleStart(player, state, prog);
      }
   }

   static void broadcastStateUpdate(BattleState state, PlayerManager pm, String msg) {
      BattleManager.reconcileEnemyLiveness(state, pm);
      state.pushLog(msg);
      String logHistory = String.join("\n", state.logHistory);
      UUID firstReal = state.participantOrder.stream().filter(id -> !id.equals(BattleState.PARTY_MEMBER_UUID)).findFirst().orElse(null);

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2) && BattleManager.activeBattles.containsKey(pid)) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               float curPrimaryHp = state.enemyHpMap.getOrDefault(state.primaryEnemyId, state.entityCurrentHp);
               float curPrimaryMaxHp = state.enemyMaxHpMap.getOrDefault(state.primaryEnemyId, state.entityMaxHp);
               String curPrimaryName = state.enemyNames.getOrDefault(state.primaryEnemyId, state.entityName);
               String curPrimaryType = state.enemyTypeKeys.getOrDefault(state.primaryEnemyId, state.entityTypeKey);
               buf.writeFloat(curPrimaryHp);
               buf.writeFloat(curPrimaryMaxHp);
               buf.writeString(curPrimaryName);
               buf.writeString(curPrimaryType);
               buf.writeFloat(BattleManager.TARGET_HEALTH.getOrDefault(pid, pp.getHealth()));
               buf.writeFloat(pp.getMaxHealth());
               buf.writeFloat(state.getTp(pid));
               buf.writeString(logHistory);
               buf.writeInt(state.lastActionHitCount);
               boolean isMyTurn = state.phase == BattleState.Phase.PLAYER_TURN
                  && !state.resolving
                  && (
                     state.currentParticipant().equals(pid) || state.currentParticipant().equals(BattleState.PARTY_MEMBER_UUID) && pid.equals(state.playerUuid)
                  );
               boolean isEnemyTurn = state.phase == BattleState.Phase.AWAITING_ENEMY_RESULT;
               buf.writeBoolean(isMyTurn);
               buf.writeBoolean(isEnemyTurn);
               buf.writeFloat(state.getShieldReduction(pid));
               buf.writeInt(state.getShieldTurnsLeft(pid));
               buf.writeInt(state.enemySleepTurns);
               buf.writeInt(state.enemyConfusedLeft);
               buf.writeInt(state.enemyFreezeTurns);
               buf.writeInt(state.enemyBurnTurns);
               buf.writeInt(state.enemyDownTurns.getOrDefault(state.primaryEnemyId, 0));
               buf.writeInt(state.enemySturdy.getOrDefault(state.primaryEnemyId, 0));
               buf.writeBoolean(state.enemySpareable || state.isMercyEligible());
               writeParticipants(buf, state, pid, pp.getServer());
               writeOpponents(buf, state, pid, pp.getServer());
               buf.writeString(state.entityPassive);
               buf.writeBoolean(state.darknessConstricting);
               writeExtraEnemies(buf, state);
               writeEnemyRoster(buf, state);
               buf.writeInt(state.primaryEnemyId);
               buf.writeInt(state.bossLivesRemaining);
               buf.writeInt(state.bossMaxLives);
               buf.writeBoolean(TurnBattleConfig.get().rollingHealth);
               buf.writeString(TurnBattleConfig.get().visualStyle != null ? TurnBattleConfig.get().visualStyle : "deltarune");
               buf.writeFloat(state.getSpareThreshold(state.primaryEnemyId));
               buf.writeInt(state.battleStyle != null ? state.battleStyle.ordinal() : 0);
               if (state.isPvp) {
                  UUID oppUuid = BattleManager.primaryEnemyOf(state, pid, pm);
                  buf.writeFloat(state.pvpShield.getOrDefault(oppUuid, 0.0F));
                  buf.writeFloat(state.pvpShieldMax.getOrDefault(oppUuid, 0.0F));
                  buf.writeBoolean(state.pvpShieldBroken.getOrDefault(oppUuid, true));
                  buf.writeFloat(state.pvpShield.getOrDefault(pid, 0.0F));
                  buf.writeFloat(state.pvpShieldMax.getOrDefault(pid, 0.0F));
                  buf.writeBoolean(state.pvpShieldBroken.getOrDefault(pid, true));
               } else {
                  buf.writeFloat(0.0F);
                  buf.writeFloat(0.0F);
                  buf.writeBoolean(true);
                  buf.writeFloat(0.0F);
                  buf.writeFloat(0.0F);
                  buf.writeBoolean(true);
               }

               Net121.sendToClient(pp, TurnBattleMod.BATTLE_STATE, buf);
            }
         }
      }
   }

   static void sendStateUpdateWithLives(ServerPlayerEntity player, BattleState state) {
      PacketByteBuf buf = PacketByteBufs.create();
      float curPrimaryHp = state.enemyHpMap.getOrDefault(state.primaryEnemyId, state.entityCurrentHp);
      float curPrimaryMaxHp = state.enemyMaxHpMap.getOrDefault(state.primaryEnemyId, state.entityMaxHp);
      String curPrimaryName = state.enemyNames.getOrDefault(state.primaryEnemyId, state.entityName);
      String curPrimaryType = state.enemyTypeKeys.getOrDefault(state.primaryEnemyId, state.entityTypeKey);
      String logHistory = String.join("\n", state.logHistory);
      buf.writeFloat(curPrimaryHp);
      buf.writeFloat(curPrimaryMaxHp);
      buf.writeString(curPrimaryName);
      buf.writeString(curPrimaryType);
      buf.writeFloat(BattleManager.TARGET_HEALTH.getOrDefault(player.getUuid(), player.getHealth()));
      buf.writeFloat(player.getMaxHealth());
      buf.writeFloat(state.getTp(player.getUuid()));
      buf.writeString(logHistory);
      buf.writeInt(state.lastActionHitCount);
      UUID pid = player.getUuid();
      boolean isMyTurn = state.phase == BattleState.Phase.PLAYER_TURN
         && !state.resolving
         && (state.currentParticipant().equals(pid) || state.currentParticipant().equals(BattleState.PARTY_MEMBER_UUID) && pid.equals(state.playerUuid));
      boolean isEnemyTurn = state.phase == BattleState.Phase.AWAITING_ENEMY_RESULT;
      buf.writeBoolean(isMyTurn);
      buf.writeBoolean(isEnemyTurn);
      buf.writeFloat(state.getShieldReduction(pid));
      buf.writeInt(state.getShieldTurnsLeft(pid));
      buf.writeInt(state.enemySleepTurns);
      buf.writeInt(state.enemyConfusedLeft);
      buf.writeInt(state.enemyFreezeTurns);
      buf.writeInt(state.enemyBurnTurns);
      buf.writeInt(state.enemyDownTurns.getOrDefault(state.primaryEnemyId, 0));
      buf.writeInt(state.enemySturdy.getOrDefault(state.primaryEnemyId, 0));
      buf.writeBoolean(state.enemySpareable || state.isMercyEligible());
      writeParticipants(buf, state, pid, player.getServer());
      buf.writeString(state.entityPassive);
      buf.writeBoolean(state.darknessConstricting);
      writeExtraEnemies(buf, state);
      writeEnemyRoster(buf, state);
      buf.writeInt(state.primaryEnemyId);
      buf.writeInt(state.bossLivesRemaining);
      buf.writeInt(state.bossMaxLives);
      buf.writeBoolean(TurnBattleConfig.get().rollingHealth);
      buf.writeString(TurnBattleConfig.get().visualStyle != null ? TurnBattleConfig.get().visualStyle : "deltarune");
      buf.writeFloat(state.getSpareThreshold(state.primaryEnemyId));
      buf.writeInt(state.battleStyle != null ? state.battleStyle.ordinal() : 0);
      Net121.sendToClient(player, TurnBattleMod.BATTLE_STATE, buf);
   }

   static void broadcastHeal(BattleState state, PlayerManager pm, UUID targetUuid, float amount) {
      broadcastHeal(state, pm, targetUuid, amount, true, false);
   }

   static void broadcastHeal(BattleState state, PlayerManager pm, UUID targetUuid, float amount, boolean sfx) {
      broadcastHeal(state, pm, targetUuid, amount, sfx, false);
   }

   static void broadcastHeal(BattleState state, PlayerManager pm, UUID targetUuid, float amount, boolean sfx, boolean gold) {
      if (!(amount <= 0.0F)) {
         int pidx = -1;
         int idx = 0;

         for (UUID pid : state.participantOrder) {
            if (pid.equals(targetUuid)) {
               pidx = idx;
               break;
            }

            idx++;
         }

         int finalIdx = pidx;

         for (UUID pid : state.participantOrder) {
            if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               ServerPlayerEntity pp = pm.getPlayer(pid);
               if (pp != null) {
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeInt(finalIdx);
                  buf.writeFloat(amount);
                  buf.writeBoolean(sfx);
                  buf.writeBoolean(gold);
                  Net121.sendToClient(pp, TurnBattleMod.HEAL_PLAY, buf);
               }
            }
         }
      }
   }

   static void broadcastBashPlay(BattleState state, PlayerManager pm, UUID actor, int targetIndex) {
      BattleData bd = pm.getServer() != null ? BattleData.get(pm.getServer()) : null;
      int soulColorRgb = 16777215;
      if (bd != null && !actor.equals(BattleState.PARTY_MEMBER_UUID)) {
         BattleData.PlayerProgress ap = bd.getOrCreate(actor);
         if (ap.soulType != null) {
            soulColorRgb = ap.soulType.getColor() & 16777215;
         }
      }

      UUID resolvedTargetUuid = state.isPvp ? BattleManager.resolveOffensiveTargetUuid(state, actor, targetIndex) : null;
      boolean actorIsPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || actor.equals(BattleState.PARTY_MEMBER_UUID_2);

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID)
            && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)
            && (toAll || !pid.equals(actor))
            && (toAll || !actorIsPartyMember || !pid.equals(state.playerUuid))) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(soulColorRgb);
               buf.writeInt(targetIndex);
               buf.writeBoolean(resolvedTargetUuid != null);
               if (resolvedTargetUuid != null) {
                  buf.writeUuid(resolvedTargetUuid);
               }

               buf.writeUuid(actor);
               Net121.sendToClient(pp, TurnBattleMod.BASH_PLAY, buf);
            }
         }
      }

      BattleStaging.spectatorEvent(
         state, pm, 0, actor, targetIndex, 0, 0, pm.getServer() != null ? resolveActorSoulColorRgb(pm.getServer(), state, actor) : 16777215
      );
   }

   static int resolveActorSoulColorRgb(MinecraftServer server, BattleState state, UUID actor) {
      if (!BattleManager.isRecruitSlot(actor)) {
         BattleData bd = BattleData.get(server);
         BattleData.PlayerProgress ap = bd.getOrCreate(actor);
         return ap.soulType != null ? ap.soulType.getColor() & 16777215 : 16777215;
      } else {
         RecruitData.PlayerRecruits pr = RecruitData.get(server).getOrCreate(state.playerUuid);
         int slotCode = actor.equals(BattleState.PARTY_MEMBER_UUID_2) ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            if (ci < pr.customRecruits.size()) {
               SoulType st = pr.customRecruits.get(ci).soulType;
               if (st != null) {
                  return st.getColor() & 16777215;
               }
            }
         } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
            SoulType st = Recruit.values()[slotCode].soulType;
            if (st != null) {
               return st.getColor() & 16777215;
            }
         }

         return 16777215;
      }
   }

   static void broadcastBashQteStart(BattleState state, PlayerManager pm, UUID actor, int targetIndex, boolean crit, float critZoneCenter) {
      int soulColorRgb = pm.getServer() != null ? resolveActorSoulColorRgb(pm.getServer(), state, actor) : 16777215;
      UUID resolvedTargetUuid = state.isPvp ? BattleManager.resolveOffensiveTargetUuid(state, actor, targetIndex) : null;

      for (UUID pid : state.participantOrder) {
         if (!BattleManager.isRecruitSlot(pid)) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeString(actor.toString());
               buf.writeInt(soulColorRgb);
               buf.writeInt(targetIndex);
               buf.writeBoolean(crit);
               buf.writeFloat(critZoneCenter);
               buf.writeBoolean(pid.equals(actor) || BattleManager.isRecruitSlot(actor) && pid.equals(state.playerUuid));
               buf.writeBoolean(resolvedTargetUuid != null);
               if (resolvedTargetUuid != null) {
                  buf.writeUuid(resolvedTargetUuid);
               }

               Net121.sendToClient(pp, TurnBattleMod.BASH_QTE_START, buf);
            }
         }
      }
   }

   static void broadcastSkillPlay(BattleState state, PlayerManager pm, UUID actor, int skillOrdinal, PlayerClass cls, int targetIndex) {
      int clsOrd = cls == null ? 0 : cls.ordinal();
      boolean actorIsPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || actor.equals(BattleState.PARTY_MEMBER_UUID_2);
      UUID resolvedTargetUuid = state.isPvp && targetIndex < 10 ? BattleManager.resolveOffensiveTargetUuid(state, actor, targetIndex) : null;

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID)
            && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)
            && (toAll || !pid.equals(actor))
            && (toAll || !actorIsPartyMember || !pid.equals(state.playerUuid))) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(skillOrdinal);
               buf.writeInt(clsOrd);
               buf.writeInt(targetIndex);
               buf.writeBoolean(resolvedTargetUuid != null);
               if (resolvedTargetUuid != null) {
                  buf.writeUuid(resolvedTargetUuid);
               }

               buf.writeUuid(actor);
               Net121.sendToClient(pp, TurnBattleMod.SKILL_PLAY, buf);
            }
         }
      }

      BattleStaging.spectatorEvent(
         state, pm, 1, actor, targetIndex, skillOrdinal, clsOrd, pm.getServer() != null ? resolveActorSoulColorRgb(pm.getServer(), state, actor) : 16777215
      );
   }

   public static void broadcastVesselAnim(BattleState state, PlayerManager pm, int participantIndex, int animType, UUID excludeUuid) {
      broadcastVesselAnim(state, pm, participantIndex, animType, excludeUuid, excludeUuid, "");
   }

   public static void broadcastVesselAnim(
      BattleState state, PlayerManager pm, int participantIndex, int animType, UUID excludeUuid, UUID actorUuid, String itemId
   ) {
      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2) && !pid.equals(excludeUuid)) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(participantIndex);
               buf.writeInt(animType);
               buf.writeBoolean(actorUuid != null);
               if (actorUuid != null) {
                  buf.writeUuid(actorUuid);
               }

               buf.writeString(itemId == null ? "" : itemId);
               Net121.sendToClient(pp, TurnBattleMod.VESSEL_ANIM, buf);
            }
         }
      }
      int ev = switch (animType) {
         case 2 -> 2;
         case 3 -> 3;
         default -> -1;
         case 5 -> 4;
         case 7 -> 6;
      };
      UUID evActor = actorUuid != null
         ? actorUuid
         : (participantIndex >= 0 && participantIndex < state.participantOrder.size() ? state.participantOrder.get(participantIndex) : null);
      if (ev >= 0) {
         BattleStaging.spectatorEvent(state, pm, ev, evActor, -1, 0, 0, 16777215);
      }
   }

   public static void broadcastClonePose(BattleState state, PlayerManager pm, UUID actor, int code, String recruitName) {
      for (UUID pid : state.participantOrder) {
         if (!BattleManager.isRecruitSlot(pid) && !pid.equals(actor)) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeUuid(actor);
               buf.writeByte(code);
               buf.writeString(recruitName == null ? "" : recruitName);
               Net121.sendToClient(pp, TurnBattleMod.CLONE_POSE, buf);
            }
         }
      }
   }

   static void broadcastParticipantUpdate(BattleState state, PlayerManager pm) {
      broadcastStateUpdate(state, pm, "A new ally joined the battle!");
   }

   static void broadcastSharedTp(BattleState state, PlayerManager pm) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeFloat(state.sharedTp);
      buf.writeFloat(state.sharedTpMax);

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               Net121.sendToClient(pp, TurnBattleMod.TP_SYNC, buf);
            }
         }
      }
   }

   static void broadcastParticipantsOnly(BattleState state, PlayerManager pm) {
      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               writeParticipants(buf, state, pid, pp.getServer());
               writeOpponents(buf, state, pid, pp.getServer());
               Net121.sendToClient(pp, TurnBattleMod.PARTICIPANT_UPDATE, buf);
            }
         }
      }
   }

   static void writeExtraEnemies(PacketByteBuf buf, BattleState state) {
      buf.writeInt(state.extraEnemyIds.size());

      for (int eid : state.extraEnemyIds) {
         buf.writeInt(eid);
         buf.writeString(state.enemyNames.getOrDefault(eid, "Enemy"));
         buf.writeString(state.enemyTypeKeys.getOrDefault(eid, ""));
         buf.writeFloat(state.enemyHpMap.getOrDefault(eid, 0.0F));
         buf.writeFloat(state.enemyMaxHpMap.getOrDefault(eid, 1.0F));
         buf.writeInt(state.enemyFreezeTurnsMap.getOrDefault(eid, 0));
         buf.writeInt(state.enemyBurnTurnsMap.getOrDefault(eid, 0));
         buf.writeInt(state.enemySleepTurnsMap.getOrDefault(eid, 0));
         buf.writeInt(state.enemyConfusedLeftMap.getOrDefault(eid, 0));
         buf.writeInt(state.enemyDownTurns.getOrDefault(eid, 0));
         buf.writeInt(state.enemySturdy.getOrDefault(eid, 0));
         buf.writeFloat(state.getSpareThreshold(eid));
      }
   }

   static void writeOpponents(PacketByteBuf buf, BattleState state, UUID viewer, MinecraftServer server) {
      List opponents = state.isPvp ? state.enemiesOf(viewer) : List.of();
      buf.writeInt(opponents.size());
      PlayerManager pm = server.getPlayerManager();

      for (UUID pid : opponents) {
         buf.writeString(state.participantNames.getOrDefault(pid, "?"));
         buf.writeUuid(pid);
         buf.writeFloat(state.participantHp.getOrDefault(pid, 0.0F));
         buf.writeFloat(state.participantMaxHp.getOrDefault(pid, 1.0F));
         buf.writeBoolean(BattleManager.isDowned(state, pid, pm));
      }
   }

   static void writeEnemyRoster(PacketByteBuf buf, BattleState state) {
      List roster = new ArrayList<>(state.enemyHpMap.keySet());
      roster.remove(Integer.valueOf(Integer.MIN_VALUE));
      buf.writeInt(roster.size());

      for (int eid : roster) {
         buf.writeInt(eid);
         buf.writeString(state.enemyNames.getOrDefault(eid, "Enemy"));
         buf.writeString(state.enemyTypeKeys.getOrDefault(eid, ""));
         buf.writeFloat(state.enemyHpMap.getOrDefault(eid, 0.0F));
         buf.writeFloat(state.enemyMaxHpMap.getOrDefault(eid, 1.0F));
      }
   }

   static void writeParticipants(PacketByteBuf buf, BattleState state, UUID viewer, MinecraftServer server) {
      BattleData bdSoul = server != null ? BattleData.get(server) : null;
      List sendOrder = state.isPvp ? (state.isTeamPvp ? state.teammatesOf(viewer) : List.of(viewer)) : state.participantOrder;
      buf.writeInt(sendOrder.size());
      int curIdx = state.currentParticipantIndex % Math.max(1, state.participantOrder.size());

      for (int i = 0; i < sendOrder.size(); i++) {
         UUID pid = sendOrder.get(i);
         buf.writeString(state.participantNames.getOrDefault(pid, "?"));
         buf.writeUuid(pid);
         buf.writeFloat(state.targetParticipantHp.getOrDefault(pid, state.participantHp.getOrDefault(pid, 0.0F)));
         buf.writeFloat(state.participantMaxHp.getOrDefault(pid, 1.0F));
         buf.writeFloat(state.getTp(pid));
         buf.writeBoolean(state.isPvp ? state.currentParticipant().equals(pid) : i == curIdx && !state.resolving);
         buf.writeBoolean(pid.equals(viewer));
         SoulType pidSoul = null;
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            if (bdSoul != null) {
               BattleData.PlayerProgress pidProg = bdSoul.getOrCreate(pid);
               pidSoul = pidProg.soulType;
            }
         } else if (server != null) {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.players.get(viewer);
            if (pr != null) {
               int slotCode = pid.equals(BattleState.PARTY_MEMBER_UUID_2) ? pr.secondActiveRecruit : pr.activeRecruit;
               if (slotCode >= 1000) {
                  int ci = slotCode - 1000;
                  if (ci < pr.customRecruits.size()) {
                     pidSoul = pr.customRecruits.get(ci).soulType;
                  }
               } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
                  pidSoul = Recruit.values()[slotCode].soulType;
               }
            }
         }

         buf.writeInt(pidSoul != null ? pidSoul.ordinal() : -1);
         buf.writeInt(state.getShieldTurnsLeft(pid));
         buf.writeInt(state.getAttackBoostTurns(pid));
         buf.writeBoolean(state.playerFreezeMap.getOrDefault(pid, 0) > 0);
         buf.writeBoolean(state.playerBurned.contains(pid));
         buf.writeBoolean(state.playerSleepMap.getOrDefault(pid, 0) > 0);
         buf.writeBoolean(state.playerConfusionMap.getOrDefault(pid, 0) > 0);
         int pidVessel = 0;
         if (bdSoul != null && !BattleManager.isRecruitSlot(pid)) {
            BattleData.PlayerProgress pidProg = bdSoul.getOrCreate(pid);
            pidVessel = pidProg.vesselChoice;
         }

         buf.writeInt(pidVessel);
      }
   }

   static void broadcastSkillPlayToAll(BattleState state, PlayerManager pm, UUID actor, int skillOrdinal, PlayerClass cls, int targetIndex) {
      toAll = true;

      try {
         broadcastSkillPlay(state, pm, actor, skillOrdinal, cls, targetIndex);
      } finally {
         toAll = false;
      }
   }

   static void broadcastBashPlayToAll(BattleState state, PlayerManager pm, UUID actor, int targetIndex) {
      toAll = true;

      try {
         broadcastBashPlay(state, pm, actor, targetIndex);
      } finally {
         toAll = false;
      }
   }

   static void sendBattleLine(ServerPlayerEntity pp, int seq, String text, int mode) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(seq);
      buf.writeString(text == null ? "" : text);
      buf.writeByte(mode);
      Net121.sendToClient(pp, TurnBattleMod.BATTLE_LINE, buf);
   }

   static void broadcastBattleLine(BattleState state, PlayerManager pm, int seq, String text, int mode) {
      for (UUID pid : state.participantOrder) {
         if (!BattleManager.isRecruitSlot(pid) && BattleManager.activeBattles.get(pid) == state) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               sendBattleLine(pp, seq, text, mode);
            }
         }
      }
   }

   static void broadcastBattleLineSkip(BattleState state, PlayerManager pm, int seq) {
      for (UUID pid : state.participantOrder) {
         if (!BattleManager.isRecruitSlot(pid) && BattleManager.activeBattles.get(pid) == state) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(seq);
               Net121.sendToClient(pp, TurnBattleMod.BATTLE_LINE_SKIP, buf);
            }
         }
      }
   }

   static void broadcastMultiBash(BattleState state, PlayerManager pm) {
      for (UUID pid : state.participantOrder) {
         if (!BattleManager.isRecruitSlot(pid) && BattleManager.activeBattles.get(pid) == state) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(state.bashPhase.size());

               for (BattleState.BashEntry e : state.bashPhase) {
                  buf.writeUuid(e.pick.actor);
                  buf.writeUuid(e.pick.owner);
                  buf.writeInt(pm.getServer() != null ? resolveActorSoulColorRgb(pm.getServer(), state, e.pick.actor) : 16777215);
                  buf.writeBoolean(e.crit);
                  buf.writeInt(e.delay);
                  buf.writeInt(state.participantOrder.indexOf(e.pick.actor));
                  buf.writeString(state.participantNames.getOrDefault(e.pick.actor, "?"));
               }

               buf.writeFloat(0.2F);
               Net121.sendToClient(pp, TurnBattleMod.MULTI_BASH_START, buf);
            }
         }
      }
   }

   static void broadcastBashBar(BattleState state, PlayerManager pm, UUID actor, float pos, boolean crit) {
      for (UUID pid : state.participantOrder) {
         if (!BattleManager.isRecruitSlot(pid) && BattleManager.activeBattles.get(pid) == state) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeUuid(actor);
               buf.writeFloat(pos);
               buf.writeBoolean(crit);
               Net121.sendToClient(pp, TurnBattleMod.MULTI_BASH_BAR, buf);
            }
         }
      }
   }
}
