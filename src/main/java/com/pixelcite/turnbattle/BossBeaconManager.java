package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BossBeaconManager {
   private static final List<BossBeaconManager.PendingBossStart> pendingBattles = new ArrayList<>();
   public static final String[] DIFFICULTY_NAMES = new String[]{"Easy", "Medium", "Hard", "Very Hard", "Impossible"};
   public static final float[] STAT_MULT = new float[]{1.0F, 1.0F, 1.5F, 2.0F, 2.5F};
   public static final int[] ITEM_COUNTS = new int[]{2, 4, 8, 12, 20};
   public static final float[] EXP_MULT = new float[]{1.0F, 1.5F, 2.0F, 2.5F, 5.0F};
   private static final Identifier BOSS_HP_MOD = Identifier.of("turnbattle", "d4e5f6a7-0004-0000-0000-000000000001");
   private static final Identifier BOSS_ATK_MOD = Identifier.of("turnbattle", "d4e5f6a7-0004-0000-0000-000000000002");
   private static final float BOSS_BASE_HP_MULT = 1.0F;
   private static final Random RNG = new Random();

   public static void tick() {
      if (!pendingBattles.isEmpty()) {
         List batch = new ArrayList<>(pendingBattles);
         pendingBattles.clear();

         for (BossBeaconManager.PendingBossStart p : batch) {
            if (p.boss().isAlive() && !p.participants().isEmpty()) {
               MinecraftServer srv = p.participants().get(0).getServer();
               if (srv != null) {
                  startBossBeaconBattle(srv, p.boss(), p.participants(), p.beaconPos(), p.level(), p.difficulty());
               }
            }
         }
      }
   }

   private static void startBossBeaconBattle(
      MinecraftServer server, MobEntity boss, List<ServerPlayerEntity> participants, BlockPos beaconPos, int level, int difficulty
   ) {
      BattleManager.tryStart(participants.get(0), boss);
      BattleState state = BattleManager.activeBattles.get(participants.get(0).getUuid());
      if (state != null) {
         state.bossBeaconPos = beaconPos;
         state.bossHighestLevel = level;
         state.bossDifficultyOrdinal = difficulty;
         state.bossMaxLives = difficulty + 1;
         state.bossLivesRemaining = difficulty + 1;
         BattleNetworking.broadcastStateUpdate(state, server.getPlayerManager(), null);
      }

      for (int i = 1; i < participants.size(); i++) {
         BattleManager.addPlayerToExistingBattle(participants.get(i), state != null ? state : BattleManager.activeBattles.get(participants.get(0).getUuid()));
      }
   }

   public static void onPlayerInteract(ServerPlayerEntity player, BlockPos beaconPos) {
      MinecraftServer server = player.getServer();
      BossBeaconData data = BossBeaconData.get(server);
      BossBeaconData.BossBeaconLobby lobby = data.lobbies.get(beaconPos);
      if (lobby == null) {
         lobby = new BossBeaconData.BossBeaconLobby(player.getUuid(), getDisplayName(player));
         data.lobbies.put(beaconPos, lobby);
      } else {
         if (lobby.started) {
            player.sendMessage(Text.literal("[Boss Beacon] This battle has already started!").formatted(Formatting.RED), false);
            return;
         }

         if (!lobby.playerUuids.contains(player.getUuid())) {
            lobby.addPlayer(player.getUuid(), getDisplayName(player));
         }
      }

      broadcastLobbySync(server, beaconPos, lobby);
   }

   public static void onSetDifficulty(ServerPlayerEntity player, BlockPos beaconPos, int difficulty) {
      BossBeaconData data = BossBeaconData.get(player.getServer());
      BossBeaconData.BossBeaconLobby lobby = data.lobbies.get(beaconPos);
      if (lobby != null && lobby.isHost(player.getUuid())) {
         if (difficulty >= 0 && difficulty < DIFFICULTY_NAMES.length) {
            lobby.difficulty = difficulty;
            broadcastLobbySync(player.getServer(), beaconPos, lobby);
         }
      }
   }

   public static void onBeginBattle(ServerPlayerEntity player, BlockPos beaconPos) {
      MinecraftServer server = player.getServer();
      BossBeaconData data = BossBeaconData.get(server);
      BossBeaconData.BossBeaconLobby lobby = data.lobbies.get(beaconPos);
      if (lobby != null) {
         if (lobby.isHost(player.getUuid())) {
            if (lobby.difficulty >= 0) {
               if (!lobby.started) {
                  lobby.started = true;
                  List participants = new ArrayList<>();
                  int highestLevel = 1;

                  for (UUID pid : new ArrayList<>(lobby.playerUuids)) {
                     ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pid);
                     if (pp != null) {
                        participants.add(pp);
                        BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(pid);
                        if (prog.level > highestLevel) {
                           highestLevel = prog.level;
                        }
                     }
                  }

                  if (participants.isEmpty()) {
                     data.lobbies.remove(beaconPos);
                  } else {
                     int difficulty = lobby.difficulty;
                     ServerWorld overworld = server.getWorld(World.OVERWORLD);
                     if (overworld != null) {
                        MobEntity boss = spawnBossMob(overworld, beaconPos, highestLevel, difficulty);
                        if (boss != null) {
                           data.lobbies.remove(beaconPos);

                           for (ServerPlayerEntity pp : participants) {
                              PacketByteBuf closeBuf = PacketByteBufs.create();
                              closeBuf.writeBoolean(true);
                              closeBuf.writeInt(beaconPos.getX());
                              closeBuf.writeInt(beaconPos.getY());
                              closeBuf.writeInt(beaconPos.getZ());
                              closeBuf.writeBoolean(false);
                              closeBuf.writeInt(-1);
                              closeBuf.writeInt(0);
                              Net121.sendToClient(pp, TurnBattleMod.BOSS_BEACON_SYNC, closeBuf);
                           }

                           List finalParticipants = new ArrayList<>(participants);
                           pendingBattles.add(new BossBeaconManager.PendingBossStart(boss, finalParticipants, beaconPos, highestLevel, difficulty));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void onPlayerLeave(ServerPlayerEntity player, BlockPos beaconPos) {
      BossBeaconData data = BossBeaconData.get(player.getServer());
      BossBeaconData.BossBeaconLobby lobby = data.lobbies.get(beaconPos);
      if (lobby != null) {
         lobby.removePlayer(player.getUuid());
         if (lobby.playerUuids.isEmpty()) {
            data.lobbies.remove(beaconPos);
         } else {
            broadcastLobbySync(player.getServer(), beaconPos, lobby);
         }
      }
   }

   public static void onBossVictory(MinecraftServer server, BattleState state) {
      BlockPos beaconPos = state.bossBeaconPos;
      if (beaconPos != null) {
         BossBeaconData.get(server).lobbies.remove(beaconPos);
         ServerWorld world = server.getWorld(World.OVERWORLD);
         if (world != null) {
            world.removeBlock(beaconPos, false);
            BlockPos base = beaconPos.down();

            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  BlockPos bp = base.add(dx, 0, dz);
                  if (world.getBlockState(bp).isOf(Blocks.IRON_BLOCK)) {
                     world.removeBlock(bp, false);
                  }
               }
            }
         }
      }
   }

   private static void broadcastLobbySync(MinecraftServer server, BlockPos pos, BossBeaconData.BossBeaconLobby lobby) {
      for (int i = 0; i < lobby.playerUuids.size(); i++) {
         ServerPlayerEntity pp = server.getPlayerManager().getPlayer(lobby.playerUuids.get(i));
         if (pp != null) {
            sendLobbySync(pp, pos, lobby, false);
         }
      }
   }

   public static void sendLobbySync(ServerPlayerEntity player, BlockPos pos, BossBeaconData.BossBeaconLobby lobby, boolean forceClose) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(forceClose);
      buf.writeInt(pos.getX());
      buf.writeInt(pos.getY());
      buf.writeInt(pos.getZ());
      buf.writeBoolean(lobby.isHost(player.getUuid()));
      buf.writeInt(lobby.difficulty);
      buf.writeInt(lobby.playerUuids.size());

      for (int i = 0; i < lobby.playerUuids.size(); i++) {
         buf.writeString(lobby.playerNames.get(i));
         buf.writeBoolean(lobby.playerUuids.get(i).equals(lobby.hostUuid));
      }

      Net121.sendToClient(player, TurnBattleMod.BOSS_BEACON_SYNC, buf);
   }

   private static MobEntity spawnBossMob(ServerWorld world, BlockPos beaconPos, int level, int difficultyOrd) {
      EntityType type = pickBossType(level);
      if (type.create(world) instanceof MobEntity mob) {
         double var13 = (double)beaconPos.getX() + 0.5;
         double var8 = (double)beaconPos.getY() + 2.0;
         double var10 = (double)beaconPos.getZ() + 0.5;
         mob.refreshPositionAndAngles(var13, var8, var10, RNG.nextFloat() * 360.0F, 0.0F);
         mob.setPersistent();
         String var12 = DIFFICULTY_NAMES[difficultyOrd];
         mob.setCustomName(
            Text.literal("")
               .append(Text.literal("[" + var12 + " Boss] ").formatted(Formatting.GOLD))
               .append(Text.literal("Lv." + level + " ").formatted(Formatting.YELLOW))
               .append(mob.getType().getName().copy().formatted(Formatting.WHITE))
         );
         mob.setCustomNameVisible(true);
         MobLevelManager.restoreLevel(mob, level);
         world.spawnEntity(mob);
         applyBossScale(mob, STAT_MULT[difficultyOrd]);
         return mob;
      } else {
         return null;
      }
   }

   private static EntityType<?> pickBossType(int level) {
      if (level <= 10) {
         return RNG.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
      } else if (level <= 20) {
         int r = RNG.nextInt(3);

         return switch (r) {
            case 0 -> EntityType.WITHER_SKELETON;
            case 1 -> EntityType.CREEPER;
            default -> EntityType.SPIDER;
         };
      } else if (level <= 30) {
         return RNG.nextBoolean() ? EntityType.WITHER_SKELETON : EntityType.BLAZE;
      } else if (level <= 40) {
         return RNG.nextBoolean() ? EntityType.WITHER : EntityType.WARDEN;
      } else {
         int r = RNG.nextInt(3);

         return switch (r) {
            case 0 -> EntityType.WITHER;
            case 1 -> EntityType.WARDEN;
            default -> EntityType.ENDER_DRAGON;
         };
      }
   }

   private static void applyBossScale(MobEntity mob, float scale) {
      EntityAttributeInstance hpInst = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
      if (hpInst != null) {
         hpInst.removeModifier(BOSS_HP_MOD);
         double currentMax = (double)mob.getMaxHealth();
         double targetMax = currentMax * 1.0 * (double)scale;
         hpInst.setBaseValue(targetMax);
         mob.setHealth(mob.getMaxHealth());
      }

      EntityAttributeInstance atkInst = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
      if (atkInst != null) {
         atkInst.removeModifier(BOSS_ATK_MOD);
         double extra = atkInst.getValue() * ((double)scale - 1.0);
         if (extra > 0.0) {
            atkInst.addPersistentModifier(new EntityAttributeModifier(Identifier.of("turnbattle", "boss atk scale"), extra, Operation.ADD_VALUE));
         }
      }
   }

   public static void startDepthsBoss(MinecraftServer server, List<ServerPlayerEntity> participants, int floorLevel, int difficultyOrd, String fountainKey) {
      if (!participants.isEmpty()) {
         String mobTypeKey = DepthsDescentManager.bossMobType(floorLevel).replace("minecraft:", "");
         String diffLabel = DIFFICULTY_NAMES[Math.min(difficultyOrd, DIFFICULTY_NAMES.length - 1)];
         String displayName = "["
            + diffLabel
            + " Boss] Lv."
            + floorLevel
            + " "
            + Character.toUpperCase(mobTypeKey.charAt(0))
            + mobTypeKey.substring(1).replace("_", " ");
         float baseHp = Math.min(4096.0F, (200.0F + (float)floorLevel * 10.0F) * STAT_MULT[difficultyOrd]);
         float armor = Math.min(20.0F, (float)floorLevel / 5.0F);
         ServerPlayerEntity lead = participants.get(0);
         BattleManager.tryStartVirtual(lead, displayName, mobTypeKey, baseHp, armor);
         BattleState state = BattleManager.activeBattles.get(lead.getUuid());
         if (state != null) {
            state.bossMaxLives = difficultyOrd + 1;
            state.bossLivesRemaining = difficultyOrd + 1;
            state.bossDifficultyOrdinal = difficultyOrd;
            state.bossHighestLevel = floorLevel;
            state.depthsFountainKey = fountainKey;
         }

         for (int i = 1; i < participants.size(); i++) {
            if (state != null) {
               BattleManager.addDepthsParticipant(participants.get(i), state);
            }
         }
      }
   }

   public static int getBossItemCount(int difficultyOrd) {
      return difficultyOrd >= 0 && difficultyOrd < ITEM_COUNTS.length ? ITEM_COUNTS[difficultyOrd] : 2;
   }

   public static Item getBossLootItem(int level, Random rng) {
      if (level <= 10) {
         return Items.IRON_INGOT;
      } else if (level <= 20) {
         return rng.nextFloat() < 0.8F ? Items.IRON_INGOT : Items.DIAMOND;
      } else if (level <= 30) {
         return rng.nextFloat() < 0.85F ? Items.DIAMOND : Items.NETHERITE_INGOT;
      } else if (level <= 40) {
         float r = rng.nextFloat();
         if (r < 0.25F) {
            return Items.DIAMOND;
         } else if (r < 0.5F) {
            return Items.NETHERITE_INGOT;
         } else {
            return r < 0.75F ? Items.ENDER_PEARL : Items.BLAZE_ROD;
         }
      } else {
         float r = rng.nextFloat();
         if (r < 0.25F) {
            return Items.DIAMOND;
         } else if (r < 0.5F) {
            return Items.NETHERITE_INGOT;
         } else if (r < 0.6F) {
            return Items.NETHER_STAR;
         } else if (r < 0.75F) {
            return Items.ENDER_PEARL;
         } else {
            return r < 0.9F ? Items.BLAZE_ROD : Items.DIAMOND;
         }
      }
   }

   private static String getDisplayName(ServerPlayerEntity player) {
      BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(player.getUuid());
      String v = prog.vesselName;
      return v != null && !v.isEmpty() ? v : player.getName().getString();
   }

   private static record PendingBossStart(MobEntity boss, List<ServerPlayerEntity> participants, BlockPos beaconPos, int level, int difficulty) {
   }
}
