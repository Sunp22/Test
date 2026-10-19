package com.pixelcite.turnbattle;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;

public class RogueliteManager {
   private static final Random RNG = new Random();
   private static final long AWAY_GRACE_TICKS = 1800L;
   private static final Map<UUID, Long> lastSeenStanding = new HashMap<>();
   private static MinecraftServer knownServer = null;
   private static long serverFirstTick = 0L;
   private static final Map<String, Long> inviteCooldowns = new ConcurrentHashMap<>();

   private static long noteServer(MinecraftServer server) {
      long now = server.getOverworld().getTime();
      if (knownServer != server) {
         knownServer = server;
         lastSeenStanding.clear();
         serverFirstTick = now;
      }

      return now;
   }

   public static boolean isRunning(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server == null) {
         return false;
      } else {
         RogueliteData.PlayerRogueData rd = RogueliteData.get(server).players.get(player.getUuid());
         return rd != null && rd.inRun && !rd.deadThisRun;
      }
   }

   public static List<String> activeUniques(MinecraftServer server, UUID uuid) {
      RogueliteData.PlayerRogueData rd = RogueliteData.get(server).players.get(uuid);
      return rd != null && rd.inRun && !rd.deadThisRun && !rd.uniqueCards.isEmpty() ? List.copyOf(rd.uniqueCards) : List.of();
   }

   public static float getRunDifficultyScale(MinecraftServer server, UUID playerUuid) {
      RogueliteData.PlayerRogueData rd = RogueliteData.get(server).players.get(playerUuid);
      if (rd != null && rd.inRun) {
         float minutes = (float)rd.runActiveTicks / 1200.0F;
         return Math.min(1.0F + minutes * 0.04F, 3.0F);
      } else {
         return 1.0F;
      }
   }

   public static int scaleExp(MinecraftServer server, UUID uuid, int exp) {
      RogueliteData.PlayerRogueData rd = RogueliteData.get(server).players.get(uuid);
      if (rd != null && rd.inRun && !rd.deadThisRun) {
         float mult = 1.0F + (float)rd.upgrade("exp") * 0.2F + (rd.uniqueCards.contains("SCHOLAR") ? 0.25F : 0.0F);
         return Math.round((float)exp * mult);
      } else {
         return exp;
      }
   }

   public static void onBattleWon(MinecraftServer server, UUID uuid) {
      RogueliteData data = RogueliteData.get(server);
      RogueliteData.PlayerRogueData rd = data.players.get(uuid);
      if (rd != null && rd.inRun && !rd.deadThisRun) {
         rd.runBattlesWon++;
         if (rd.uniqueCards.contains("BOUNTY")) {
            rd.runBonusShards++;
         }

         data.markDirty();
      }
   }

   private static Set<UUID> runGroup(RogueliteData data, UUID uuid) {
      Set group = new HashSet<>();
      group.add(uuid);
      RogueliteData.PlayerRogueData rd = data.players.get(uuid);
      if (rd != null && rd.partyId != null) {
         Set party = data.activeParties.get(rd.partyId);
         if (party != null) {
            for (UUID m : party) {
               RogueliteData.PlayerRogueData md = data.players.get(m);
               if (md != null && md.inRun) {
                  group.add(m);
               }
            }
         }
      }

      return group;
   }

   private static Set<UUID> partyOf(RogueliteData data, UUID uuid) {
      Set members = new LinkedHashSet<>();
      members.add(uuid);
      RogueliteData.PlayerRogueData rd = data.players.get(uuid);
      if (rd != null && rd.partyId != null) {
         Set party = data.activeParties.get(rd.partyId);
         if (party != null) {
            members.addAll(party);
         }
      }

      return members;
   }

   private static ServerPlayerEntity livingAlly(MinecraftServer server, RogueliteData data, UUID uuid) {
      for (UUID m : runGroup(data, uuid)) {
         if (!m.equals(uuid)) {
            RogueliteData.PlayerRogueData md = data.players.get(m);
            ServerPlayerEntity pp = server.getPlayerManager().getPlayer(m);
            if (pp != null && pp.isAlive() && md != null && md.inRun && !md.deadThisRun) {
               return pp;
            }
         }
      }

      return null;
   }

   public static void beginRun(MinecraftServer server, ServerPlayerEntity starter) {
      RogueliteData data = RogueliteData.get(server);
      if (data.rogueliteEnabled) {
         UUID leaderUuid = starter.getUuid();
         RogueliteData.PlayerRogueData leaderData = data.getOrCreate(leaderUuid);
         if (leaderData.inRun) {
            fail(starter, leaderData.deadThisRun ? "Your party is still out there - wait for the run to end." : "You're already in a run.");
         } else {
            List runners = new ArrayList<>();

            for (UUID uid : partyOf(data, leaderUuid)) {
               ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
               if (pp != null) {
                  RogueliteData.PlayerRogueData pd = data.getOrCreate(uid);
                  if (pd.inRun) {
                     fail(starter, pp.getName().getString() + " is still in a run.");
                     return;
                  }

                  if (BattleManager.isInBattle(uid)) {
                     fail(starter, pp.getName().getString() + " is in a battle.");
                     return;
                  }

                  if (!pp.isAlive()) {
                     fail(starter, pp.getName().getString() + " hasn't respawned yet.");
                     return;
                  }

                  runners.add(pp);
               }
            }

            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld != null) {
               BlockPos dest = findRunSpawn(overworld);
               long now = overworld.getTime();
               BattleData battleData = BattleData.get(server);

               for (ServerPlayerEntity player : runners) {
                  UUID uidx = player.getUuid();
                  RogueliteData.PlayerRogueData rd = data.getOrCreate(uidx);
                  BattleData.PlayerProgress prog = battleData.getOrCreate(uidx);
                  resetProgress(server, uidx);
                  clearGear(player, prog);
                  rd.inRun = true;
                  rd.deadThisRun = false;
                  rd.pendingMenuAfterRespawn = false;
                  rd.runStartTime = now;
                  rd.runActiveTicks = 0L;
                  rd.rerollsLeft = rd.upgrade("reroll");
                  prog.rogueRun = true;
                  prog.rogueHandsOwed = rd.upgrade("opening");
                  SoulType soul = rd.chosenSoul != null && rd.ownedSouls.contains(rd.chosenSoul) ? rd.chosenSoul : null;
                  if (soul == null) {
                     soul = RogueUpgrades.SOULS[RNG.nextInt(RogueUpgrades.SOULS.length - 1)];
                  }

                  prog.soulType = soul;
                  prog.playerRace = soul == SoulType.MONSTER ? CharacterRace.MONSTER : CharacterRace.HUMAN;
                  prog.freeRollUsed = true;
                  prog.soulRemoved = false;
                  prog.determinationUsed = false;
                  if (prog.vesselName == null || prog.vesselName.isEmpty()) {
                     prog.vesselName = player.getName().getString();
                  }

                  prog.playerClass = SkillTreeDefs.computeEffectiveClass(prog);
                  prog.playerStats.vitality = prog.playerStats.vitality + 2 * rd.upgrade("vigor");
                  prog.playerStats.offense = prog.playerStats.offense + rd.upgrade("might");
                  prog.playerStats.magic = prog.playerStats.magic + rd.upgrade("might");
                  prog.playerStats.defense = prog.playerStats.defense + 2 * rd.upgrade("guard");
                  if (rd.upgrade("windup") > 0) {
                     rd.uniqueCards.add("SECOND_WIND");
                  }

                  giveStarterKit(player, rd.upgrade("kit"));
                  battleData.markDirty();
                  player.changeGameMode(GameMode.SURVIVAL);
                  player.setSpawnPoint(World.OVERWORLD, null, 0.0F, false, false);
                  player.teleport(overworld, (double)dest.getX() + 0.5, (double)dest.getY(), (double)dest.getZ() + 0.5, player.getYaw(), player.getPitch());
                  MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
                  player.setHealth(player.getMaxHealth());
                  player.getHungerManager().setFoodLevel(20);
                  player.getHungerManager().setSaturationLevel(5.0F);
                  player.extinguish();
                  player.clearStatusEffects();
                  syncProgress(player, prog);
                  sendRogueliteOpen(player, false);
                  sendRunTimer(player, rd, 0L);
                  player.sendMessage(
                     Text.literal(
                           "[Roguelite] Run started as "
                              + soul.getDisplayName()
                              + (soul == SoulType.MONSTER ? " (a Monster)" : "")
                              + "! Every level-up deals you a hand of cards. Enemies grow stronger every minute."
                        )
                        .formatted(Formatting.GOLD),
                     false
                  );
                  if (runners.size() > 1) {
                     player.sendMessage(
                        Text.literal("[Roguelite] Your party: " + String.join(", ", runners.stream().map(p -> p.getName().getString()).toList()))
                           .formatted(Formatting.GRAY),
                        false
                     );
                  }

                  tryDeal(player);
               }

               data.markDirty();
            }
         }
      }
   }

   private static BlockPos findRunSpawn(ServerWorld world) {
      BlockPos center = world.getSpawnPos();

      for (int attempt = 0; attempt < 8; attempt++) {
         double angle = RNG.nextDouble() * Math.PI * 2.0;
         double dist = 90000.0 + RNG.nextDouble() * 20000.0;
         int x = center.getX() + (int)(Math.cos(angle) * dist);
         int z = center.getZ() + (int)(Math.sin(angle) * dist);
         if (!world.getWorldBorder().contains((double)x, (double)z)) {
            dist = Math.min(dist, world.getWorldBorder().getSize() * 0.4);
            x = center.getX() + (int)(Math.cos(angle) * dist);
            z = center.getZ() + (int)(Math.sin(angle) * dist);
         }

         world.getChunk(x >> 4, z >> 4);

         for (int tries = 0; tries < 24; tries++) {
            int bx = (x & -16) + RNG.nextInt(16);
            int bz = (z & -16) + RNG.nextInt(16);
            int top = world.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            BlockPos ground = new BlockPos(bx, top - 1, bz);
            BlockState gs = world.getBlockState(ground);
            if (gs.getFluidState().isEmpty() && !gs.getCollisionShape(world, ground).isEmpty() && !gs.isIn(BlockTags.LEAVES)) {
               BlockPos feet = ground.up();
               if (world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                  && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()
                  && world.getBlockState(feet).getFluidState().isEmpty()) {
                  return feet;
               }
            }
         }
      }

      return world.getSpawnPos();
   }

   private static void clearGear(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      player.getInventory().clear();
      player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
      player.getEnderChestInventory().clear();
      prog.equippedWeapon = null;
      prog.partyEquippedWeapon = null;
      prog.equippedAccessory = null;
      prog.equippedArmor = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      MobLevelManager.removeWeaponStats(player);
      MobLevelManager.removeArmorStats(player, 0);
      MobLevelManager.removeArmorStats(player, 1);
      player.playerScreenHandler.sendContentUpdates();
   }

   private static void giveStarterKit(ServerPlayerEntity player, int level) {
      if (level > 0) {
         List kit = new ArrayList<>();
         if (level == 1) {
            kit.add(new ItemStack(Items.STONE_SWORD));
            kit.add(new ItemStack(Items.BREAD, 8));
            kit.add(new ItemStack(Items.TORCH, 16));
         } else {
            kit.add(new ItemStack(Items.IRON_SWORD));
            kit.add(new ItemStack(level >= 3 ? Items.COOKED_BEEF : Items.BREAD, 16));
            kit.add(new ItemStack(Items.TORCH, 32));
            kit.add(new ItemStack(Items.IRON_CHESTPLATE));
            if (level >= 3) {
               kit.add(new ItemStack(Items.IRON_HELMET));
               kit.add(new ItemStack(Items.IRON_LEGGINGS));
               kit.add(new ItemStack(Items.IRON_BOOTS));
               kit.add(new ItemStack(Items.SHIELD));
            }
         }

         for (ItemStack s : kit) {
            if (!player.getInventory().insertStack(s)) {
               player.dropItem(s, false);
            }
         }
      }
   }

   public static void resetProgress(MinecraftServer server, UUID uid) {
      BattleData battleData = BattleData.get(server);
      BattleData.PlayerProgress prog = battleData.getOrCreate(uid);
      prog.level = 1;
      prog.exp = 0;
      prog.playerStats = new CharStats();
      prog.playerStats.pointValue = prog.magicant ? 2 : 1;
      prog.skillPoints = 0;
      prog.unlockedSkills.clear();
      prog.unequippedSkills.clear();
      prog.treeStatBonuses = new int[6];

      for (PartyMember m : PartyMember.values()) {
         int mi = m.ordinal();
         prog.partyLevels[mi] = 1;
         prog.partyStats[mi] = new CharStats();
         prog.partySkillPoints[mi] = 0;
         prog.partyUnlockedSkills[mi].clear();
      }

      prog.partyMemberHp = -1.0F;
      prog.absorbedSouls.clear();
      if (prog.playerStats != null) {
         prog.playerStats.absorbed = 0;
      }

      prog.rogueRun = false;
      prog.rogueHandsOwed = 0;
      prog.determinationUsed = false;
      prog.playerClass = SkillTreeDefs.computeEffectiveClass(prog);
      battleData.markDirty();
      RecruitData recruits = RecruitData.get(server);
      if (recruits.players.remove(uid) != null) {
         recruits.markDirty();
      }

      RogueliteData data = RogueliteData.get(server);
      RogueliteData.PlayerRogueData rd = data.getOrCreate(uid);
      rd.drawnSkills.clear();
      rd.cardStatBonuses = new int[6];
      rd.uniqueCards.clear();
      rd.pendingHand.clear();
      rd.rerollsLeft = 0;
      rd.runBattlesWon = 0;
      rd.runBonusShards = 0;
      data.markDirty();
   }

   private static void syncProgress(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf lv = PacketByteBufs.create();
      lv.writeInt(prog.level);
      lv.writeInt(prog.exp);
      lv.writeInt(BattleData.expNeeded(prog.level));
      lv.writeBoolean(false);
      Net121.sendToClient(player, TurnBattleMod.LEVEL_UPDATE, lv);
      TurnBattleMod.sendPartyInfo(player);
      TurnBattleMod.sendEquipInfo(player, prog);
      TurnBattleMod.sendAccessoryInfo(player, prog);
      sendRogueSkills(player, RogueliteData.get(player.getServer()).getOrCreate(player.getUuid()));
   }

   public static void onPlayerDeath(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.players.get(player.getUuid());
         if (rd != null && rd.inRun && !rd.deadThisRun) {
            finishRunFor(server, data, player.getUuid(), rd);
            rd.deadThisRun = true;
            rd.pendingHand.clear();
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
            prog.rogueHandsOwed = 0;
            BattleData.get(server).markDirty();
            sendHand(player, rd);
            data.markDirty();
            if (!checkRunOver(server, data, player.getUuid())) {
               player.sendMessage(
                  Text.literal("[Roguelite] You've fallen! You'll watch over your party until they fall too.").formatted(Formatting.GRAY), false
               );

               for (UUID m : runGroup(data, player.getUuid())) {
                  ServerPlayerEntity pp = server.getPlayerManager().getPlayer(m);
                  if (pp != null && pp != player) {
                     pp.sendMessage(Text.literal("[Roguelite] " + player.getName().getString() + " has fallen!").formatted(Formatting.RED), false);
                  }
               }
            }
         }
      }
   }

   private static boolean checkRunOver(MinecraftServer server, RogueliteData data, UUID anyMember) {
      Set group = runGroup(data, anyMember);
      boolean anyoneHere = false;
      long now = noteServer(server);

      for (UUID m : group) {
         RogueliteData.PlayerRogueData md = data.players.get(m);
         if (md != null && md.inRun) {
            ServerPlayerEntity pp = server.getPlayerManager().getPlayer(m);
            if (pp == null) {
               if (!md.deadThisRun && now - lastSeenStanding.getOrDefault(m, serverFirstTick) < 1800L) {
                  return false;
               }
            } else {
               anyoneHere = true;
               if (!md.deadThisRun) {
                  return false;
               }
            }
         }
      }

      if (!anyoneHere) {
         return false;
      } else {
         endRun(server, data, group);
         return true;
      }
   }

   private static void endRun(MinecraftServer server, RogueliteData data, Set<UUID> group) {
      for (UUID uid : group) {
         RogueliteData.PlayerRogueData rd = data.getOrCreate(uid);
         if (rd.inRun) {
            if (!rd.deadThisRun) {
               finishRunFor(server, data, uid, rd);
            }

            rd.inRun = false;
            rd.deadThisRun = false;
            resetProgress(server, uid);
            ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
            if (pp != null && pp.isAlive()) {
               returnToLobby(pp);
            } else {
               rd.pendingMenuAfterRespawn = true;
               if (pp != null) {
                  sendRogueInfo(pp);
               }
            }
         }
      }

      data.markDirty();
   }

   private static void finishRunFor(MinecraftServer server, RogueliteData data, UUID uid, RogueliteData.PlayerRogueData rd) {
      BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(uid);
      long ticks = rd.runActiveTicks;
      int shards = RogueUpgrades.shardsFor(prog.level, rd.runBattlesWon, ticks) + rd.runBonusShards;
      shards = Math.round((float)shards * (1.0F + 0.15F * (float)rd.upgrade("magnet")));
      rd.soulShards += shards;
      rd.totalShardsEarned += shards;
      rd.totalRuns++;
      boolean newBestLevel = prog.level > rd.bestLevel;
      boolean newBestTime = ticks > rd.bestTimeTicks;
      rd.bestLevel = Math.max(rd.bestLevel, prog.level);
      rd.bestTimeTicks = Math.max(rd.bestTimeTicks, ticks);
      rd.lastRunLevel = prog.level;
      rd.lastRunTicks = ticks;
      rd.lastRunBattles = rd.runBattlesWon;
      rd.lastRunShards = shards;
      data.markDirty();
      ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
      if (pp != null) {
         pp.sendMessage(
            Text.literal(
                  "[Roguelite] Run over - level "
                     + prog.level
                     + ", "
                     + formatTime(ticks)
                     + ", "
                     + rd.runBattlesWon
                     + " battle"
                     + (rd.runBattlesWon == 1 ? "" : "s")
                     + " won. "
               )
               .formatted(Formatting.LIGHT_PURPLE)
               .append(Text.literal("+" + shards + " Soul Shards").formatted(Formatting.YELLOW))
               .append(Text.literal(newBestLevel ? "  New best level!" : (newBestTime ? "  Longest run yet!" : "")).formatted(Formatting.GOLD)),
            false
         );
      }
   }

   private static void returnToLobby(ServerPlayerEntity pp) {
      MinecraftServer server = pp.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.getOrCreate(pp.getUuid());
         rd.pendingMenuAfterRespawn = false;
         data.markDirty();
         BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(pp.getUuid());
         pp.changeGameMode(GameMode.SURVIVAL);
         pp.setSpawnPoint(World.OVERWORLD, null, 0.0F, false, false);
         ServerWorld overworld = server.getWorld(World.OVERWORLD);
         if (overworld != null) {
            BlockPos spawn = overworld.getSpawnPos();
            int y = overworld.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
            pp.teleport(overworld, (double)spawn.getX() + 0.5, (double)Math.max(y, spawn.getY()), (double)spawn.getZ() + 0.5, pp.getYaw(), pp.getPitch());
         }

         MobLevelManager.applyPlayerStats(pp, prog.level, prog.playerStats);
         pp.setHealth(pp.getMaxHealth());
         pp.getHungerManager().setFoodLevel(20);
         pp.getHungerManager().setSaturationLevel(5.0F);
         syncProgress(pp, prog);
         sendHand(pp, rd);
         sendRunTimer(pp, rd, -1L);
         sendRogueInfo(pp);
         sendRogueliteOpen(pp, true);
      }
   }

   public static void onRespawn(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.players.get(player.getUuid());
         if (rd != null) {
            if (rd.inRun && rd.deadThisRun) {
               spectate(server, data, player);
            } else if (!rd.inRun && rd.pendingMenuAfterRespawn) {
               returnToLobby(player);
            }
         }
      }
   }

   private static void spectate(MinecraftServer server, RogueliteData data, ServerPlayerEntity player) {
      player.changeGameMode(GameMode.SPECTATOR);
      ServerPlayerEntity ally = livingAlly(server, data, player.getUuid());
      if (ally != null) {
         player.teleport(ally.getServerWorld(), ally.getX(), ally.getY() + 1.0, ally.getZ(), ally.getYaw(), ally.getPitch());
         player.sendMessage(Text.literal("[Roguelite] Spectating " + ally.getName().getString() + ".").formatted(Formatting.GRAY), false);
      }

      RogueliteData.PlayerRogueData rd = data.getOrCreate(player.getUuid());
      sendRunTimer(player, rd, rd.runActiveTicks);
   }

   public static void onPlayerJoin(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.players.get(player.getUuid());
         BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
         if (rd != null && rd.inRun && !prog.rogueRun) {
            rd.inRun = false;
            rd.deadThisRun = false;
            resetProgress(server, player.getUuid());
            player.sendMessage(Text.literal("[Roguelite] Roguelite mode was updated, so your old run has been closed.").formatted(Formatting.YELLOW), false);
            if (player.isAlive()) {
               returnToLobby(player);
            } else {
               rd.pendingMenuAfterRespawn = true;
            }
         } else if (rd == null) {
            if (data.rogueliteEnabled && player.isAlive()) {
               sendRogueliteOpen(player, true);
            }
         } else {
            if (rd.inRun && rd.deadThisRun) {
               if (!checkRunOver(server, data, player.getUuid())) {
                  syncProgress(player, prog);
                  if (player.isAlive()) {
                     spectate(server, data, player);
                  }
               }
            } else if (rd.inRun) {
               syncProgress(player, prog);
               sendRunTimer(player, rd, rd.runActiveTicks);
               if (!rd.pendingHand.isEmpty()) {
                  sendHand(player, rd);
               }

               if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
                  player.changeGameMode(GameMode.SURVIVAL);
               }
            } else if (rd.pendingMenuAfterRespawn) {
               if (player.isAlive()) {
                  returnToLobby(player);
               }
            } else if (data.rogueliteEnabled && player.isAlive()) {
               if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
                  player.changeGameMode(GameMode.SURVIVAL);
               }

               sendRogueliteOpen(player, true);
            }
         }
      }
   }

   public static void abandon(ServerPlayerEntity player) {
      if (isRunning(player)) {
         if (BattleManager.isInBattle(player.getUuid())) {
            fail(player, "You can't give up in the middle of a battle.");
         } else {
            BattleManager.forceKill(player);
         }
      }
   }

   public static void tick(MinecraftServer server) {
      RogueliteData data = RogueliteData.get(server);
      long now = noteServer(server);
      Set checked = new HashSet<>();

      for (ServerPlayerEntity pp : new ArrayList(server.getPlayerManager().getPlayerList())) {
         RogueliteData.PlayerRogueData rd = data.players.get(pp.getUuid());
         if (rd != null && rd.inRun) {
            if (!rd.deadThisRun) {
               rd.runActiveTicks += 20L;
               lastSeenStanding.put(pp.getUuid(), now);
               data.markDirty();
            }

            sendRunTimer(pp, rd, rd.runActiveTicks);
            if (!rd.deadThisRun) {
               tryDeal(pp);
            } else if (checked.add(pp.getUuid())) {
               checked.addAll(runGroup(data, pp.getUuid()));
               checkRunOver(server, data, pp.getUuid());
            }
         }
      }
   }

   public static boolean isPickingCard(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server == null) {
         return false;
      } else {
         RogueliteData.PlayerRogueData rd = RogueliteData.get(server).players.get(player.getUuid());
         return rd != null && rd.inRun && !rd.deadThisRun && !rd.pendingHand.isEmpty();
      }
   }

   public static void tryDeal(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.players.get(player.getUuid());
         if (rd != null && rd.inRun && !rd.deadThisRun && rd.pendingHand.isEmpty()) {
            BattleData battleData = BattleData.get(server);
            BattleData.PlayerProgress prog = battleData.getOrCreate(player.getUuid());
            if (prog.rogueHandsOwed > 0 && player.isAlive() && !BattleManager.isInBattle(player.getUuid())) {
               List hand = dealFor(rd, prog);
               if (!hand.isEmpty()) {
                  prog.rogueHandsOwed--;
                  battleData.markDirty();
                  rd.pendingHand.addAll(hand);
                  data.markDirty();
                  sendHand(player, rd);
               }
            }
         }
      }
   }

   private static List<String> dealFor(RogueliteData.PlayerRogueData rd, BattleData.PlayerProgress prog) {
      int size = 3 + (rd.upgrade("wide") > 0 ? 1 : 0) + (rd.uniqueCards.contains("LUCKY_DRAW") ? 1 : 0);
      return RogueCards.deal(RNG, size, prog.level, prog.playerRace, prog.unlockedSkills, rd.uniqueCards, prog.playerStats);
   }

   public static void onCardChoice(ServerPlayerEntity player, int index) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.players.get(player.getUuid());
         if (rd != null && rd.inRun && !rd.deadThisRun && !rd.pendingHand.isEmpty()) {
            BattleData battleData = BattleData.get(server);
            BattleData.PlayerProgress prog = battleData.getOrCreate(player.getUuid());
            if (index == -1) {
               List fresh = rd.rerollsLeft > 0 ? dealFor(rd, prog) : List.of();
               if (!fresh.isEmpty()) {
                  rd.rerollsLeft--;
                  rd.pendingHand.clear();
                  rd.pendingHand.addAll(fresh);
                  data.markDirty();
               }

               sendHand(player, rd);
            } else if (index >= 0 && index < rd.pendingHand.size()) {
               String card = rd.pendingHand.get(index);
               rd.pendingHand.clear();
               applyCard(player, rd, prog, card);
               battleData.markDirty();
               data.markDirty();
               syncProgress(player, prog);
               sendHand(player, rd);
               tryDeal(player);
            }
         }
      }
   }

   private static void applyCard(ServerPlayerEntity player, RogueliteData.PlayerRogueData rd, BattleData.PlayerProgress prog, String card) {
      String[] p = card.split(":", 3);
      RogueCards.Info info = RogueCards.describe(card);
      String var6 = p[0];
      switch (var6) {
         case "STAT":
            int s = RogueCards.statIndex(p[1]);
            int amount = Integer.parseInt(p[2]);
            if (s < 0) {
               return;
            }

            int added = addStat(prog.playerStats, s, amount);
            rd.cardStatBonuses[s] = rd.cardStatBonuses[s] + added;
            say(player, "+" + added + " " + RogueCards.STAT_NAMES[s] + "!", Formatting.AQUA);
            break;
         case "RISK":
            int lose = RogueCards.statIndex(p[1]);
            int gain = RogueCards.statIndex(p[2]);
            if (lose < 0 || gain < 0) {
               return;
            }

            int lost = -addStat(prog.playerStats, lose, -4);
            int got = addStat(prog.playerStats, gain, 7);
            rd.cardStatBonuses[lose] = rd.cardStatBonuses[lose] - lost;
            rd.cardStatBonuses[gain] = rd.cardStatBonuses[gain] + got;
            say(player, "-" + lost + " " + RogueCards.STAT_NAMES[lose] + ", +" + got + " " + RogueCards.STAT_NAMES[gain] + "!", Formatting.GOLD);
            break;
         case "SKILL":
            String key = p[1] + ":" + p[2];
            if (RogueCards.skillFor(key) == null) {
               return;
            }

            prog.unlockedSkills.add(RogueCards.skillName(key));
            prog.unequippedSkills.remove(RogueCards.skillName(key));
            if (!rd.drawnSkills.contains(key)) {
               rd.drawnSkills.add(key);
            }

            prog.playerClass = SkillTreeDefs.computeEffectiveClass(prog);
            say(player, "New skill: " + info.title() + "!", Formatting.GREEN);
            break;
         case "UNIQUE":
            String keyx = p[1];
            if (RogueCards.unique(keyx) == null || rd.uniqueCards.contains(keyx)) {
               return;
            }

            rd.uniqueCards.add(keyx);
            if (keyx.equals("TOUGH_SKIN")) {
               rd.cardStatBonuses[1] = rd.cardStatBonuses[1] + addStat(prog.playerStats, 1, 5);
            }

            say(player, "\u2726 " + info.title() + " - " + info.desc(), Formatting.GOLD);
      }

      MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
   }

   private static int addStat(CharStats stats, int idx, int amount) {
      int before = stats.valueFor(idx);
      int after = Math.max(0, before + amount);
      if (idx == 5) {
         after = Math.min(after, Math.max(before, stats.capFor(5)));
      }

      switch (idx) {
         case 0:
            stats.offense = after;
            break;
         case 1:
            stats.defense = after;
            break;
         case 2:
            stats.magic = after;
            break;
         case 3:
            stats.iq = after;
            break;
         case 4:
            stats.vitality = after;
            break;
         default:
            stats.luck = after;
      }

      return after - before;
   }

   public static void buyUpgrade(ServerPlayerEntity player, String id) {
      RogueliteData data = RogueliteData.get(player.getServer());
      RogueliteData.PlayerRogueData rd = data.getOrCreate(player.getUuid());
      RogueUpgrades.Upgrade up = RogueUpgrades.byId(id);
      if (up != null) {
         if (rd.inRun) {
            fail(player, "Upgrades can only be bought between runs.");
         } else {
            int level = rd.upgrade(id);
            int cost = up.nextCost(level);
            if (cost >= 0) {
               if (rd.soulShards < cost) {
                  fail(player, "Not enough Soul Shards.");
               } else {
                  rd.soulShards -= cost;
                  rd.upgrades.put(id, level + 1);
                  data.markDirty();
                  say(player, up.name() + " is now level " + (level + 1) + "!", Formatting.GREEN);
               }
            }
         }
      }
   }

   public static void chooseSoul(ServerPlayerEntity player, String name) {
      RogueliteData data = RogueliteData.get(player.getServer());
      RogueliteData.PlayerRogueData rd = data.getOrCreate(player.getUuid());
      if (!name.isEmpty() && !name.equals("RANDOM")) {
         SoulType soul;
         try {
            soul = SoulType.valueOf(name);
         } catch (IllegalArgumentException var6) {
            return;
         }

         int cost = RogueUpgrades.soulCost(soul);
         if (cost >= 0) {
            if (!rd.ownedSouls.contains(soul)) {
               if (rd.soulShards < cost) {
                  fail(player, "Not enough Soul Shards.");
                  return;
               }

               rd.soulShards -= cost;
               rd.ownedSouls.add(soul);
               say(player, "Unlocked the " + soul.getDisplayName() + " soul for your runs!", Formatting.GREEN);
            }

            rd.chosenSoul = soul;
            data.markDirty();
         }
      } else {
         rd.chosenSoul = null;
         data.markDirty();
      }
   }

   public static void sendRogueliteOpen(ServerPlayerEntity player, boolean open) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(open);
      Net121.sendToClient(player, TurnBattleMod.ROGUE_OPEN, buf);
   }

   public static void sendRunTimer(ServerPlayerEntity player, RogueliteData.PlayerRogueData rd, long elapsedTicks) {
      MinecraftServer server = player.getServer();
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeLong(elapsedTicks);
      buf.writeFloat(server != null ? getRunDifficultyScale(server, player.getUuid()) : 1.0F);
      buf.writeBoolean(rd != null && rd.deadThisRun);
      int owed = server != null ? BattleData.get(server).getOrCreate(player.getUuid()).rogueHandsOwed : 0;
      buf.writeVarInt(owed + (rd != null && !rd.pendingHand.isEmpty() ? 1 : 0));
      Net121.sendToClient(player, TurnBattleMod.ROGUE_TIMER, buf);
   }

   public static void sendHand(ServerPlayerEntity player, RogueliteData.PlayerRogueData rd) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeVarInt(rd.pendingHand.size());

      for (String c : rd.pendingHand) {
         buf.writeString(c);
      }

      buf.writeVarInt(rd.rerollsLeft);
      int owed = player.getServer() != null ? BattleData.get(player.getServer()).getOrCreate(player.getUuid()).rogueHandsOwed : 0;
      buf.writeVarInt(owed);
      Net121.sendToClient(player, TurnBattleMod.ROGUE_CARD_DRAW, buf);
   }

   public static void sendRogueInfo(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.getOrCreate(player.getUuid());
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeBoolean(data.rogueliteEnabled);
         buf.writeInt(rd.soulShards);
         buf.writeBoolean(rd.inRun);
         buf.writeBoolean(rd.deadThisRun);
         List online = new ArrayList<>();

         for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (!p.getUuid().equals(player.getUuid())) {
               online.add(p);
            }
         }

         buf.writeVarInt(online.size());

         for (ServerPlayerEntity px : online) {
            buf.writeString(px.getName().getString());
            buf.writeUuid(px.getUuid());
            RogueliteData.PlayerRogueData pd = data.players.get(px.getUuid());
            buf.writeBoolean(pd != null && pd.inRun);
         }

         List party = new ArrayList<>(partyOf(data, player.getUuid()));
         party.remove(player.getUuid());
         buf.writeVarInt(party.size());

         for (UUID uid : party) {
            ServerPlayerEntity pm = server.getPlayerManager().getPlayer(uid);
            buf.writeString(pm != null ? pm.getName().getString() : nameOf(server, uid));
            RogueliteData.PlayerRogueData pd = data.players.get(uid);
            buf.writeBoolean(pm != null);
            buf.writeBoolean(pd != null && pd.inRun);
            buf.writeBoolean(pd != null && pd.deadThisRun);
         }

         buf.writeVarInt(rd.upgrades.size());
         rd.upgrades.forEach((id, lvl) -> {
            buf.writeString(id);
            buf.writeVarInt(lvl);
         });
         buf.writeString(rd.chosenSoul != null ? rd.chosenSoul.name() : "");
         buf.writeVarInt(rd.ownedSouls.size());

         for (SoulType s : rd.ownedSouls) {
            buf.writeString(s.name());
         }

         buf.writeVarInt(rd.totalRuns);
         buf.writeVarInt(rd.bestLevel);
         buf.writeLong(rd.bestTimeTicks);
         buf.writeVarInt(rd.totalShardsEarned);
         buf.writeInt(rd.lastRunLevel);
         buf.writeLong(rd.lastRunTicks);
         buf.writeVarInt(rd.lastRunBattles);
         buf.writeVarInt(rd.lastRunShards);
         Net121.sendToClient(player, TurnBattleMod.ROGUE_INFO, buf);
      }
   }

   public static void sendRogueSkills(ServerPlayerEntity player, RogueliteData.PlayerRogueData rd) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(rd.drawnSkills.size());

      for (String s : rd.drawnSkills) {
         buf.writeString(s);
      }

      buf.writeInt(rd.uniqueCards.size());

      for (String u : rd.uniqueCards) {
         buf.writeString(u);
      }

      for (int i = 0; i < 6; i++) {
         buf.writeInt(rd.cardStatBonuses[i]);
      }

      Net121.sendToClient(player, TurnBattleMod.ROGUE_SKILLS, buf);
   }

   private static String nameOf(MinecraftServer server, UUID uid) {
      UserCache cache = server.getUserCache();
      if (cache != null) {
         Optional prof = cache.getByUuid(uid);
         if (prof.isPresent()) {
            return prof.get().getName();
         }
      }

      return uid.toString().substring(0, 8);
   }

   public static void sendInvite(ServerPlayerEntity inviter, ServerPlayerEntity invitee) {
      MinecraftServer server = inviter.getServer();
      if (server != null) {
         if (inviter.getUuid().equals(invitee.getUuid())) {
            fail(inviter, "You can't invite yourself.");
         } else {
            RogueliteData data = RogueliteData.get(server);
            if (data.getOrCreate(inviter.getUuid()).inRun) {
               fail(inviter, "You can't invite anyone during a run.");
            } else if (data.getOrCreate(invitee.getUuid()).inRun) {
               fail(inviter, invitee.getName().getString() + " is in a run right now.");
            } else {
               String cooldownKey = inviter.getUuidAsString() + "-" + invitee.getUuidAsString();
               long now = System.currentTimeMillis();
               Long lastInvite = inviteCooldowns.get(cooldownKey);
               if (lastInvite != null && now - lastInvite < 60000L) {
                  fail(inviter, "You invited them recently - wait " + (60000L - (now - lastInvite)) / 1000L + " more second(s).");
               } else {
                  inviteCooldowns.put(cooldownKey, now);
                  data.pendingInvites.put(invitee.getUuid(), inviter.getUuid());
                  inviter.sendMessage(Text.literal("[Roguelite] Invited " + invitee.getName().getString() + ".").formatted(Formatting.GRAY), false);
                  invitee.sendMessage(
                     Text.literal("[Roguelite] " + inviter.getName().getString() + " invited you to their party! ")
                        .formatted(Formatting.GOLD)
                        .append(
                           Text.literal("[Click to Accept]")
                              .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                              .styled(s -> s.withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sbpartyrogue accept " + inviter.getName().getString())))
                        ),
                     false
                  );
               }
            }
         }
      }
   }

   public static boolean acceptInvite(ServerPlayerEntity acceptor, String inviterName) {
      MinecraftServer server = acceptor.getServer();
      if (server == null) {
         return false;
      } else {
         RogueliteData data = RogueliteData.get(server);
         UUID inviterUuid = data.pendingInvites.get(acceptor.getUuid());
         if (inviterUuid == null) {
            return false;
         } else {
            ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(inviterUuid);
            if (inviter != null && inviter.getName().getString().equalsIgnoreCase(inviterName)) {
               RogueliteData.PlayerRogueData inviterData = data.getOrCreate(inviterUuid);
               RogueliteData.PlayerRogueData acceptorData = data.getOrCreate(acceptor.getUuid());
               if (!inviterData.inRun && !acceptorData.inRun) {
                  data.pendingInvites.remove(acceptor.getUuid());
                  if (acceptorData.partyId != null && !acceptorData.partyId.equals(inviterData.partyId)) {
                     leaveParty(acceptor, false);
                  }

                  UUID partyId = inviterData.partyId;
                  if (partyId == null || !data.activeParties.containsKey(partyId)) {
                     partyId = UUID.randomUUID();
                     inviterData.partyId = partyId;
                     Set party = new HashSet<>();
                     party.add(inviterUuid);
                     data.activeParties.put(partyId, party);
                  }

                  data.activeParties.get(partyId).add(acceptor.getUuid());
                  acceptorData.partyId = partyId;
                  data.markDirty();
                  say(acceptor, "Joined " + inviter.getName().getString() + "'s party!", Formatting.GREEN);

                  for (UUID m : data.activeParties.get(partyId)) {
                     ServerPlayerEntity pp = server.getPlayerManager().getPlayer(m);
                     if (pp != null) {
                        if (pp != acceptor) {
                           say(pp, acceptor.getName().getString() + " joined your party!", Formatting.GREEN);
                        }

                        sendRogueInfo(pp);
                     }
                  }

                  return true;
               } else {
                  fail(acceptor, "You can't join a party while either of you is in a run.");
                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   public static void leaveParty(ServerPlayerEntity player) {
      leaveParty(player, true);
   }

   private static void leaveParty(ServerPlayerEntity player, boolean announce) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         RogueliteData data = RogueliteData.get(server);
         RogueliteData.PlayerRogueData rd = data.players.get(player.getUuid());
         if (rd != null && rd.partyId != null) {
            if (rd.inRun) {
               fail(player, "You can't leave your party during a run.");
            } else {
               UUID partyId = rd.partyId;
               rd.partyId = null;
               Set party = data.activeParties.get(partyId);
               if (party != null) {
                  party.remove(player.getUuid());
                  List rest = new ArrayList<>(party);
                  if (party.size() < 2) {
                     data.activeParties.remove(partyId);

                     for (UUID m : rest) {
                        data.getOrCreate(m).partyId = null;
                     }
                  }

                  for (UUID uid : rest) {
                     ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
                     if (pp != null) {
                        say(pp, player.getName().getString() + " left the party.", Formatting.YELLOW);
                        sendRogueInfo(pp);
                     }
                  }
               }

               data.markDirty();
               if (announce) {
                  say(player, "You left the party.", Formatting.YELLOW);
               }

               sendRogueInfo(player);
            }
         } else {
            if (announce) {
               fail(player, "You are not in a party.");
            }
         }
      }
   }

   private static void say(ServerPlayerEntity p, String msg, Formatting f) {
      p.sendMessage(Text.literal("[Roguelite] " + msg).formatted(f), false);
   }

   private static void fail(ServerPlayerEntity p, String msg) {
      say(p, msg, Formatting.RED);
   }

   public static String formatTime(long ticks) {
      return RogueUpgrades.formatTime(ticks);
   }
}
