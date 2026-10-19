package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class DepthsDescentManager {
   private static final Map<UUID, String> playerInstance = new HashMap<>();
   private static final Map<UUID, float[]> playerPos = new HashMap<>();
   private static final Map<UUID, float[]> lastTickPos = new HashMap<>();
   private static final Map<UUID, Integer> playerDir = new HashMap<>();
   private static final Map<UUID, Integer> encounterCountdown = new HashMap<>();
   private static final Map<String, Set<UUID>> activeEncounters = new HashMap<>();
   private static final Set<String> bossDefeatedKeys = new HashSet<>();
   private static final Map<String, List<float[]>> roomEnemies = new HashMap<>();
   private static final float ENEMY_SPEED = 4.8F;
   private static final float CATCH_RADIUS = 12.0F;
   private static final float AGGRO_RADIUS = 96.0F;
   private static final int NUM_ENEMIES = 1;
   private static final Map<String, Integer> battleStartDelay = new HashMap<>();
   private static final Map<String, DepthsDescentManager.StairsVote> stairsVotes = new HashMap<>();
   private static final int ENCOUNTER_DELAY = 20;
   private static final float ENCOUNTER_DIST_THRESHOLD = 16.0F;
   private static final Map<UUID, float[]> playerDistWalked = new HashMap<>();

   private static String bossKey(String fountainKey, int floor) {
      return fountainKey + "#" + floor;
   }

   static String pickMobType(int floor, Random rng) {
      if (floor <= 5) {
         return rng.nextBoolean() ? "minecraft:zombie" : "minecraft:skeleton";
      } else if (floor <= 10) {
         int r = rng.nextInt(3);
         return r == 0 ? "minecraft:zombie" : (r == 1 ? "minecraft:skeleton" : "minecraft:spider");
      } else if (floor <= 20) {
         int r = rng.nextInt(4);
         return r == 0 ? "minecraft:blaze" : (r == 1 ? "minecraft:skeleton" : (r == 2 ? "minecraft:spider" : "minecraft:creeper"));
      } else if (floor <= 40) {
         int r = rng.nextInt(3);
         return r == 0 ? "minecraft:wither_skeleton" : (r == 1 ? "minecraft:blaze" : "minecraft:enderman");
      } else if (floor <= 70) {
         int r = rng.nextInt(3);
         return r == 0 ? "minecraft:wither_skeleton" : (r == 1 ? "minecraft:enderman" : "minecraft:phantom");
      } else {
         int r = rng.nextInt(3);
         return r == 0 ? "minecraft:wither_skeleton" : (r == 1 ? "minecraft:warden" : "minecraft:enderman");
      }
   }

   static String bossMobType(int floor) {
      if (floor <= 10) {
         return "minecraft:zombie";
      } else if (floor <= 20) {
         return "minecraft:skeleton";
      } else if (floor <= 30) {
         return "minecraft:blaze";
      } else if (floor <= 50) {
         return "minecraft:wither_skeleton";
      } else if (floor <= 70) {
         return "minecraft:enderman";
      } else {
         return floor <= 90 ? "minecraft:warden" : "minecraft:wither";
      }
   }

   public static boolean isInDepths(UUID uuid) {
      return playerInstance.containsKey(uuid);
   }

   public static int getFloorEnemyLevel(MinecraftServer server, UUID uuid) {
      String key = playerInstance.get(uuid);
      if (key == null) {
         return -1;
      } else {
         DepthsDescentData.Instance inst = DepthsDescentData.get(server).getInstance(key);
         return inst != null ? inst.enemyLevel() : -1;
      }
   }

   public static String getInstanceKey(UUID uuid) {
      return playerInstance.get(uuid);
   }

   public static void enterDepths(ServerPlayerEntity player, String fountainKey) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         player.setInvulnerable(true);
         if (isInDepths(player.getUuid())) {
            DepthsDescentData data2 = DepthsDescentData.get(server);
            DepthsDescentData.Instance inst2 = data2.getInstance(fountainKey);
            if (inst2 != null) {
               sendRoomData(player, inst2);
            }
         } else {
            DepthsDescentData data = DepthsDescentData.get(server);
            long seed = (long)fountainKey.hashCode() * 25214903917L;
            DepthsDescentData.Instance inst = data.getOrCreate(fountainKey, data.globalSealedCount, seed);
            if (inst.sealed) {
               player.sendMessage(Text.literal("This Dark World has been sealed."), true);
            } else {
               inst.playersInside.add(player.getUuid());
               data.markDirty();
               if (inst.currentRoom == null) {
                  inst.currentRoom = new DepthsDescentRoom(inst.seed + (long)inst.currentFloor, inst.currentFloor, inst.isBossFloor());
               }

               playerInstance.put(player.getUuid(), fountainKey);
               playerPos.put(player.getUuid(), new float[]{(float)inst.currentRoom.playerStartTx * 16.0F, (float)inst.currentRoom.playerStartTy * 16.0F});
               playerDir.put(player.getUuid(), 2);
               sendEnterPacket(player, inst);
               sendRoomData(player, inst);
               broadcastPositions(server, inst, fountainKey);
            }
         }
      }
   }

   public static void exitDepths(ServerPlayerEntity player) {
      UUID uuid = player.getUuid();
      player.setInvulnerable(false);
      String key = playerInstance.remove(uuid);
      playerPos.remove(uuid);
      playerDir.remove(uuid);
      encounterCountdown.remove(uuid);
      playerDistWalked.remove(uuid);
      if (key != null) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            DepthsDescentData data = DepthsDescentData.get(server);
            DepthsDescentData.Instance inst = data.getInstance(key);
            if (inst != null) {
               inst.playersInside.remove(uuid);
               data.markDirty();
            }

            try {
               int instanceId = Integer.parseInt(key.split(":")[0]);
               DarkFountainData dfData = DarkFountainData.get(server);
               DarkFountainData.DarkWorldInstance slot = dfData.instances.get(instanceId);
               if (slot != null) {
                  BlockPos rp = slot.creatorReturnPos != null ? slot.creatorReturnPos : (slot.roomCenter != null ? slot.roomCenter.add(15, 0, 0) : null);
                  if (rp != null) {
                     ServerWorld rw = server.getWorld(slot.getRoomDimKey());
                     if (rw == null) {
                        rw = server.getOverworld();
                     }

                     ServerWorld fRw = rw;
                     double fx = (double)rp.getX() + 0.5;
                     double fy = (double)rp.getY();
                     double fz = (double)rp.getZ() + 0.5;
                     server.execute(() -> player.teleport(fRw, fx, fy, fz, player.getYaw(), player.getPitch()));
                  }
               }
            } catch (Exception var18) {
            }

            DarkFountainManager.clearEntryCooldown(uuid);
         }

         PacketByteBuf buf = PacketByteBufs.create();
         Net121.sendToClient(player, TurnBattleMod.DD_EXIT, buf);
      }
   }

   public static void onPlayerMove(ServerPlayerEntity player, float px, float py, int dir) {
      UUID uuid = player.getUuid();
      if (isInDepths(uuid)) {
         if (!encounterCountdown.containsKey(uuid)) {
            String key = playerInstance.get(uuid);
            MinecraftServer server = player.getServer();
            if (server != null) {
               DepthsDescentData.Instance inst = DepthsDescentData.get(server).getInstance(key);
               if (inst != null) {
                  float maxX = 1280.0F;
                  float maxY = 896.0F;
                  px = Math.max(0.0F, Math.min(maxX, px));
                  py = Math.max(0.0F, Math.min(maxY, py));
                  playerPos.put(uuid, new float[]{px, py});
                  playerDir.put(uuid, dir);
                  broadcastPositions(server, inst, key);
               }
            }
         }
      }
   }

   private static void triggerEncounter(MinecraftServer server, DepthsDescentData.Instance inst, String key) {
      Set group = new HashSet<>(inst.playersInside);
      if (!group.isEmpty()) {
         activeEncounters.put(key, group);

         for (UUID uid : group) {
            ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
            if (pp != null) {
               encounterCountdown.put(uid, 20);
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeBoolean(true);
               Net121.sendToClient(pp, TurnBattleMod.DD_ENCOUNTER, buf);
            }
         }
      }
   }

   public static void tick(MinecraftServer server) {
      List toFire = new ArrayList<>();

      for (Entry e : encounterCountdown.entrySet()) {
         int remaining = e.getValue() - 1;
         if (remaining <= 0) {
            toFire.add(e.getKey());
         } else {
            e.setValue(remaining);
         }
      }

      for (UUID uuid : toFire) {
         encounterCountdown.remove(uuid);
         String key = playerInstance.get(uuid);
         if (key != null) {
            Set group = activeEncounters.get(key);
            if (group != null && group.contains(uuid)) {
               startEncounterBattle(server, key, group);
               break;
            }
         }
      }

      for (String key : new HashSet<>(playerInstance.values())) {
         DepthsDescentData.Instance inst = DepthsDescentData.get(server).getInstance(key);
         if (inst != null && !inst.playersInside.isEmpty() && inst.currentRoom != null) {
            List enemies = roomEnemies.computeIfAbsent(key, k -> spawnEnemies(inst.currentRoom, inst.seed + (long)inst.currentFloor, inst.isBossFloor()));
            List playerPositions = new ArrayList<>();
            List playerUuids = new ArrayList<>();

            for (UUID uid : inst.playersInside) {
               float[] pos = playerPos.get(uid);
               if (pos != null) {
                  playerPositions.add(pos);
                  playerUuids.add(uid);
               }
            }

            if (!playerPositions.isEmpty()) {
               if (battleStartDelay.containsKey(key)) {
                  int delay = battleStartDelay.get(key) - 1;
                  if (delay <= 0) {
                     battleStartDelay.remove(key);
                     fireBattle(server, inst, key);
                  } else {
                     battleStartDelay.put(key, delay);
                  }

                  broadcastEnemies(server, inst, key, enemies);
               } else if (!enemies.isEmpty()) {
                  float[] chaser = null;
                  float[] target = playerPositions.get(0);
                  UUID targetUuid = playerUuids.get(0);
                  float bestD = Float.MAX_VALUE;

                  for (float[] enemy : enemies) {
                     for (int pi = 0; pi < playerPositions.size(); pi++) {
                        float d = dist(enemy, playerPositions.get(pi));
                        if (d < bestD) {
                           bestD = d;
                           chaser = enemy;
                           target = playerPositions.get(pi);
                           targetUuid = playerUuids.get(pi);
                        }
                     }
                  }

                  if (chaser != null) {
                     if (bestD <= 96.0F) {
                        float dx = target[0] - chaser[0];
                        float dy = target[1] - chaser[1];
                        float d = (float)Math.sqrt((double)(dx * dx + dy * dy));
                        if (d > 0.5F) {
                           chaser[0] += dx / d * 4.8F;
                           chaser[1] += dy / d * 4.8F;
                        }
                     }

                     if (bestD < (inst.isBossFloor() ? 24.0F : 12.0F) && !battleStartDelay.containsKey(key)) {
                        battleStartDelay.put(key, 20);

                        for (UUID uidx : inst.playersInside) {
                           ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uidx);
                           if (pp != null) {
                              PacketByteBuf enc = PacketByteBufs.create();
                              enc.writeBoolean(true);
                              Net121.sendToClient(pp, TurnBattleMod.DD_ENCOUNTER, enc);
                              PacketByteBuf eBuf = PacketByteBufs.create();
                              eBuf.writeInt(0);
                              Net121.sendToClient(pp, TurnBattleMod.DD_ENEMIES_SYNC, eBuf);
                           }
                        }

                        enemies.clear();
                     }

                     if (!enemies.isEmpty()) {
                        broadcastEnemies(server, inst, key, enemies);
                     }
                  }
               }
            }
         }
      }
   }

   private static float dist(float[] a, float[] b) {
      float dx = a[0] - b[0];
      float dy = a[1] - b[1];
      return (float)Math.sqrt((double)(dx * dx + dy * dy));
   }

   private static void fireBattle(MinecraftServer server, DepthsDescentData.Instance inst, String key) {
      ServerPlayerEntity lead = null;

      for (UUID uid : inst.playersInside) {
         ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
         if (pp != null) {
            lead = pp;
            break;
         }
      }

      if (lead != null) {
         if (inst.isBossFloor()) {
            List group = new ArrayList<>();

            for (UUID uidx : inst.playersInside) {
               ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uidx);
               if (pp != null) {
                  group.add(pp);
               }
            }

            int diffOrd = difficultyForFloor(inst.currentFloor);
            BossBeaconManager.startDepthsBoss(server, group, inst.currentFloor, diffOrd, key);
            ServerPlayerEntity checkLead = group.isEmpty() ? null : group.get(0);
            boolean started = checkLead != null && BattleManager.activeBattles.containsKey(checkLead.getUuid());
            if (started) {
               bossDefeatedKeys.add(bossKey(key, inst.currentFloor));
            } else {
               roomEnemies.remove(key);
               battleStartDelay.remove(key);
            }
         } else {
            String mobType = pickMobType(inst.currentFloor, new Random());
            float maxHp = calcBossHp(mobType, inst.currentFloor);
            BattleManager.tryStartVirtual(lead, mobDisplayName(mobType), mobSpriteKey(mobType), maxHp, (float)calcArmor(inst.currentFloor));
            BattleState state = BattleManager.activeBattles.get(lead.getUuid());
            if (state != null) {
               state.depthsFountainKey = key;

               for (UUID uidxx : inst.playersInside) {
                  if (!uidxx.equals(lead.getUuid())) {
                     ServerPlayerEntity other = server.getPlayerManager().getPlayer(uidxx);
                     if (other != null) {
                        BattleManager.addDepthsParticipant(other, state);
                     }
                  }
               }
            }
         }
      }
   }

   private static List<float[]> spawnEnemies(DepthsDescentRoom room, long seed) {
      return spawnEnemies(room, seed, false);
   }

   private static List<float[]> spawnEnemies(DepthsDescentRoom room, long seed, boolean bossFloor) {
      if (bossFloor) {
         float px = (float)room.stairsTx * 16.0F;
         float py = (float)room.stairsTy * 16.0F;
         List list = new ArrayList<>();
         list.add(new float[]{px, py});
         return list;
      } else {
         Random rng = new Random(seed);
         List list = new ArrayList<>();
         int attempts = 0;

         while (list.size() < 1 && attempts < 200) {
            attempts++;
            int tx = 1 + rng.nextInt(78);
            int ty = 1 + rng.nextInt(54);
            if (!room.isSolid(tx, ty)) {
               float px = (float)(tx * 16);
               float py = (float)(ty * 16);
               float startX = (float)(room.playerStartTx * 16);
               float startY = (float)(room.playerStartTy * 16);
               float d = (float)Math.sqrt(Math.pow((double)(px - startX), 2.0) + Math.pow((double)(py - startY), 2.0));
               if (d > 80.0F) {
                  list.add(new float[]{px, py});
               }
            }
         }

         float exitX = (float)room.stairsTx * 16.0F;
         float exitY = (float)room.stairsTy * 16.0F;
         list.add(new float[]{exitX, exitY});
         return list;
      }
   }

   private static void broadcastEnemies(MinecraftServer server, DepthsDescentData.Instance inst, String key, List<float[]> enemies) {
      for (UUID uid : inst.playersInside) {
         ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
         if (pp != null) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(enemies.size());

            for (float[] e : enemies) {
               buf.writeFloat(e[0]);
               buf.writeFloat(e[1]);
            }

            Net121.sendToClient(pp, TurnBattleMod.DD_ENEMIES_SYNC, buf);
         }
      }
   }

   private static void startEncounterBattle(MinecraftServer server, String key, Set<UUID> group) {
      DepthsDescentData data = DepthsDescentData.get(server);
      DepthsDescentData.Instance inst = data.getInstance(key);
      if (inst == null) {
         activeEncounters.remove(key);

         for (UUID uid : group) {
            encounterCountdown.remove(uid);
            playerDistWalked.remove(uid);
         }
      } else {
         String mobType = inst.isBossFloor() ? bossMobType(inst.currentFloor) : pickMobType(inst.currentFloor, new Random());
         ServerPlayerEntity lead = null;

         for (UUID uid : group) {
            ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
            if (pp != null) {
               lead = pp;
               break;
            }
         }

         if (lead == null) {
            activeEncounters.remove(key);

            for (UUID uidx : group) {
               encounterCountdown.remove(uidx);
               playerDistWalked.remove(uidx);
            }
         } else {
            String displayName = mobDisplayName(mobType);
            String typeKey = mobSpriteKey(mobType);
            float maxHp = calcBossHp(mobType, inst.currentFloor);
            int armor = calcArmor(inst.currentFloor);
            BattleManager.tryStartVirtual(lead, displayName, typeKey, maxHp, (float)armor);
            BattleState state = BattleManager.activeBattles.get(lead.getUuid());
            if (state != null) {
               state.depthsFountainKey = key;
            } else {
               activeEncounters.remove(key);
            }

            for (UUID uidx : group) {
               encounterCountdown.remove(uidx);
               playerDistWalked.remove(uidx);
            }
         }
      }
   }

   public static void onBattleResolved(MinecraftServer server, String fountainKey, boolean won) {
      Set group = activeEncounters.remove(fountainKey);
      if (group != null) {
         for (UUID uid : group) {
            encounterCountdown.remove(uid);
         }
      }

      if (group != null) {
         DepthsDescentData data = DepthsDescentData.get(server);
         DepthsDescentData.Instance inst = data.getInstance(fountainKey);
         if (inst != null) {
            roomEnemies.put(fountainKey, new ArrayList<>());
            battleStartDelay.remove(fountainKey);

            for (UUID uid : inst.playersInside) {
               ServerPlayerEntity pp2 = server.getPlayerManager().getPlayer(uid);
               if (pp2 != null) {
                  PacketByteBuf enc = PacketByteBufs.create();
                  enc.writeBoolean(false);
                  Net121.sendToClient(pp2, TurnBattleMod.DD_ENCOUNTER, enc);
               }
            }

            if (!won) {
               for (UUID uidx : group) {
                  ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uidx);
                  if (pp != null) {
                     exitDepths(pp);
                  }
               }
            } else {
               for (UUID uidxx : group) {
                  playerDistWalked.remove(uidxx);
               }

               if (inst.isBossFloor() && inst.currentRoom != null) {
                  bossDefeatedKeys.add(bossKey(fountainKey, inst.currentFloor));
                  inst.currentRoom.chestPositions.clear();
                  int sx = inst.currentRoom.stairsTx;
                  int sy = inst.currentRoom.stairsTy;
                  inst.currentRoom.chestPositions.add(new int[]{sx - 2, sy, 0});
                  inst.currentRoom.chestPositions.add(new int[]{sx - 2, sy + 1, 0});
                  inst.currentRoom.chestPositions.add(new int[]{sx - 2, sy - 1, 0});
               }

               for (UUID uidxx : group) {
                  ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uidxx);
                  if (pp != null) {
                     if (!isInDepths(uidxx)) {
                        playerInstance.put(uidxx, fountainKey);
                        inst.playersInside.add(uidxx);
                        if (!playerPos.containsKey(uidxx) && inst.currentRoom != null) {
                           playerPos.put(uidxx, new float[]{(float)inst.currentRoom.playerStartTx * 16.0F, (float)inst.currentRoom.playerStartTy * 16.0F});
                        }

                        playerDir.put(uidxx, 2);
                     }

                     sendRoomData(pp, inst);
                     PacketByteBuf encBuf = PacketByteBufs.create();
                     encBuf.writeBoolean(false);
                     Net121.sendToClient(pp, TurnBattleMod.DD_ENCOUNTER, encBuf);
                  }
               }

               broadcastPositions(server, inst, fountainKey);
               data.markDirty();
            }
         }
      }
   }

   public static void onChestOpen(ServerPlayerEntity player, int chestIdx) {
      UUID uuid = player.getUuid();
      String key = playerInstance.get(uuid);
      if (key != null) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            DepthsDescentData data = DepthsDescentData.get(server);
            DepthsDescentData.Instance inst = data.getInstance(key);
            if (inst != null && inst.currentRoom != null) {
               if (chestIdx >= 0 && chestIdx < inst.currentRoom.chestPositions.size()) {
                  String chestKey = uuid + "#" + inst.currentFloor;
                  if (inst.chestOpenedByPlayers.contains(chestKey)) {
                     player.sendMessage(Text.literal("You attempt to open the chest, but you've already opened a chest on this floor before!"), true);
                  } else {
                     ItemStack loot = generateLoot(inst.currentFloor, new Random());
                     boolean hasRoom = player.getInventory().getEmptySlot() >= 0;
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeInt(chestIdx);
                     buf.writeBoolean(hasRoom);
                     if (hasRoom) {
                        buf.writeString(loot.getItem().getName().getString(), 64);
                        buf.writeInt(loot.getCount());
                        if (!player.getInventory().insertStack(loot.copy())) {
                           player.dropItem(loot.copy(), false);
                        }
                     }

                     Net121.sendToClient(player, TurnBattleMod.DD_CHEST_RESULT, buf);
                     inst.chestOpenedByPlayers.add(chestKey);
                     data.markDirty();
                  }
               }
            }
         }
      }
   }

   static ItemStack generateLoot(int floor, Random rng) {
      int tier = Math.min(5, floor / 20);
      List pool = new ArrayList<>();
      pool.add(new ItemStack(Items.GOLD_INGOT, 1 + rng.nextInt(3)));
      pool.add(new ItemStack(Items.IRON_INGOT, 2 + rng.nextInt(4)));
      pool.add(new ItemStack(Items.COAL, 4 + rng.nextInt(6)));
      pool.add(new ItemStack(Items.BREAD, 2 + rng.nextInt(3)));
      pool.add(new ItemStack(Items.COOKED_BEEF, 1 + rng.nextInt(3)));
      pool.add(new ItemStack(Items.ARROW, 4 + rng.nextInt(8)));
      if (tier >= 1) {
         pool.add(new ItemStack(Items.DIAMOND, 1 + rng.nextInt(2)));
         pool.add(new ItemStack(Items.EMERALD, 1 + rng.nextInt(3)));
         pool.add(new ItemStack(Items.IRON_SWORD, 1));
         pool.add(new ItemStack(Items.IRON_CHESTPLATE, 1));
      }

      if (tier >= 2) {
         pool.add(new ItemStack(Items.GOLDEN_APPLE, 1));
         pool.add(new ItemStack(Items.DIAMOND_SWORD, 1));
         pool.add(new ItemStack(Items.DIAMOND_CHESTPLATE, 1));
         pool.add(new ItemStack(Items.ENDER_PEARL, 1 + rng.nextInt(3)));
      }

      if (tier >= 3) {
         pool.add(new ItemStack(Items.NETHERITE_SCRAP, 1));
         pool.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
         pool.add(new ItemStack(Items.BLAZE_ROD, 2 + rng.nextInt(4)));
         pool.add(new ItemStack(Items.GHAST_TEAR, 1 + rng.nextInt(2)));
      }

      if (tier >= 4) {
         pool.add(new ItemStack(Items.NETHERITE_INGOT, 1));
         pool.add(new ItemStack(Items.TOTEM_OF_UNDYING, 1));
         pool.add(new ItemStack(Items.ELYTRA, 1));
      }

      if (tier >= 5) {
         pool.add(new ItemStack(Items.NETHER_STAR, 1));
         pool.add(new ItemStack(Items.BEACON, 1));
      }

      return pool.get(rng.nextInt(pool.size()));
   }

   public static void onStairsInteract(ServerPlayerEntity player) {
      UUID uuid = player.getUuid();
      String key = playerInstance.get(uuid);
      if (key != null) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            DepthsDescentData.Instance inst = DepthsDescentData.get(server).getInstance(key);
            if (inst != null) {
               if (inst.isBossFloor()) {
                  if (bossDefeatedKeys.contains(bossKey(key, inst.currentFloor))) {
                     DepthsDescentManager.StairsVote vote = stairsVotes.getOrDefault(key, new DepthsDescentManager.StairsVote(inst.playersInside.size()));
                     stairsVotes.put(key, vote);
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeInt(inst.currentFloor);
                     buf.writeBoolean(true);
                     buf.writeBoolean(false);
                     Net121.sendToClient(player, TurnBattleMod.DD_STAIRS_PROMPT, buf);
                  }
               } else {
                  boolean exhausted = false;

                  for (UUID uid : inst.playersInside) {
                     ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
                     if (pp != null && pp.getHealth() <= 0.5F) {
                        exhausted = true;
                        break;
                     }
                  }

                  if (exhausted) {
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeInt(inst.currentFloor);
                     buf.writeBoolean(false);
                     buf.writeBoolean(true);
                     Net121.sendToClient(player, TurnBattleMod.DD_STAIRS_PROMPT, buf);
                  } else {
                     DepthsDescentManager.StairsVote vote = new DepthsDescentManager.StairsVote(inst.playersInside.size());
                     stairsVotes.put(key, vote);

                     for (UUID uidx : inst.playersInside) {
                        ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uidx);
                        if (pp != null) {
                           PacketByteBuf buf = PacketByteBufs.create();
                           buf.writeInt(inst.currentFloor);
                           buf.writeBoolean(false);
                           buf.writeBoolean(false);
                           Net121.sendToClient(pp, TurnBattleMod.DD_STAIRS_PROMPT, buf);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static int difficultyForFloor(int floor) {
      if (floor <= 15) {
         return 0;
      } else if (floor <= 35) {
         return 1;
      } else if (floor <= 50) {
         return 2;
      } else {
         return floor <= 75 ? 3 : 4;
      }
   }

   public static void onStairsVote(ServerPlayerEntity player, boolean seal) {
      UUID uuid = player.getUuid();
      String key = playerInstance.get(uuid);
      if (key != null) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            DepthsDescentManager.StairsVote vote = stairsVotes.get(key);
            if (vote != null) {
               vote.cast(uuid, seal);
               if (vote.isComplete()) {
                  stairsVotes.remove(key);
                  resolveStairsVote(server, key, vote.shouldSeal());
               }
            }
         }
      }
   }

   private static void resolveStairsVote(MinecraftServer server, String key, boolean seal) {
      DepthsDescentData data = DepthsDescentData.get(server);
      DepthsDescentData.Instance inst = data.getInstance(key);
      if (inst != null) {
         if (seal) {
            data.globalSealedCount++;
            data.markDirty();

            for (UUID uid : new HashSet<>(inst.playersInside)) {
               ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
               if (pp != null) {
                  exitDepths(pp);
               }
            }
         } else {
            bossDefeatedKeys.remove(bossKey(key, inst.currentFloor));
            roomEnemies.remove(key);
            battleStartDelay.remove(key);
            inst.currentFloor++;
            inst.currentRoom = new DepthsDescentRoom(inst.seed + (long)inst.currentFloor, inst.currentFloor, inst.isBossFloor());
            data.markDirty();

            for (UUID uidx : inst.playersInside) {
               ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uidx);
               if (pp != null) {
                  float spawnPx = (float)inst.currentRoom.playerStartTx * 16.0F;
                  float spawnPy = (float)inst.currentRoom.playerStartTy * 16.0F;
                  playerPos.put(uidx, new float[]{spawnPx, spawnPy});
                  playerDir.put(uidx, 2);
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeInt(inst.currentFloor);
                  buf.writeFloat(spawnPx);
                  buf.writeFloat(spawnPy);
                  Net121.sendToClient(pp, TurnBattleMod.DD_NEXT_FLOOR, buf);
                  sendRoomData(pp, inst);
               }
            }

            broadcastPositions(server, inst, key);
         }
      }
   }

   private static void sendEnterPacket(ServerPlayerEntity player, DepthsDescentData.Instance inst) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeString(inst.fountainKey, 128);
      buf.writeInt(inst.currentFloor);
      buf.writeBoolean(inst.isBossFloor());
      float[] pos = playerPos.get(player.getUuid());
      buf.writeFloat(pos != null ? pos[0] : (float)inst.currentRoom.playerStartTx * 16.0F);
      buf.writeFloat(pos != null ? pos[1] : (float)inst.currentRoom.playerStartTy * 16.0F);
      BattleData data = BattleData.get(player.getServer());
      SoulType soul = data.getOrCreate(player.getUuid()).soulType;
      buf.writeBoolean(soul != null);
      if (soul != null) {
         buf.writeInt(soul.ordinal());
      }

      Net121.sendToClient(player, TurnBattleMod.DD_ENTER, buf);
   }

   static void sendRoomData(ServerPlayerEntity player, DepthsDescentData.Instance inst) {
      if (inst.currentRoom != null) {
         DepthsDescentRoom room = inst.currentRoom;
         PacketByteBuf buf = PacketByteBufs.create();

         for (int y = 0; y < 56; y++) {
            for (int x = 0; x < 80; x++) {
               buf.writeByte(room.tiles[y][x]);
               buf.writeByte(room.floorTexture[y][x]);
            }
         }

         buf.writeInt(room.chestPositions.size());

         for (int[] c : room.chestPositions) {
            buf.writeInt(c[0]);
            buf.writeInt(c[1]);
            buf.writeInt(c[2]);
         }

         buf.writeInt(room.stairsTx);
         buf.writeInt(room.stairsTy);
         Net121.sendToClient(player, TurnBattleMod.DD_ROOM_DATA, buf);
      }
   }

   static void broadcastPositions(MinecraftServer server, DepthsDescentData.Instance inst, String key) {
      List uids = new ArrayList<>(inst.playersInside);

      for (UUID uid : uids) {
         ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uid);
         if (pp != null) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(uids.size());

            for (UUID other : uids) {
               buf.writeUuid(other);
               float[] pos = playerPos.getOrDefault(other, new float[]{16.0F, 16.0F});
               buf.writeFloat(pos[0]);
               buf.writeFloat(pos[1]);
               buf.writeInt(playerDir.getOrDefault(other, 2));
               buf.writeBoolean(encounterCountdown.containsKey(other));
               BattleData bdata = BattleData.get(server);
               String vname = bdata.getOrCreate(other).vesselName;
               if (vname == null || vname.isBlank()) {
                  ServerPlayerEntity op = server.getPlayerManager().getPlayer(other);
                  vname = op != null ? op.getName().getString() : "?";
               }

               buf.writeString(vname, 32);
               SoulType soul = bdata.getOrCreate(other).soulType;
               buf.writeBoolean(soul != null);
               if (soul != null) {
                  buf.writeInt(soul.ordinal());
               }
            }

            Net121.sendToClient(pp, TurnBattleMod.DD_PLAYERS_SYNC, buf);
         }
      }
   }

   private static float calcBossHp(String mobType, int floor) {
      float base = 100.0F + (float)floor * 15.0F;
      if (mobType.contains("wither")) {
         base *= 2.0F;
      }

      if (mobType.contains("warden")) {
         base *= 1.5F;
      }

      return Math.min(4096.0F, base);
   }

   private static int calcArmor(int floor) {
      return Math.min(20, floor / 5);
   }

   private static String mobDisplayName(String typeKey) {
      String name = typeKey.replace("minecraft:", "").replace("_", " ");
      return Character.toUpperCase(name.charAt(0)) + name.substring(1);
   }

   private static String mobSpriteKey(String typeKey) {
      return typeKey.replace("minecraft:", "");
   }

   public static void onPlayerDisconnect(UUID uuid, MinecraftServer server) {
      String key = playerInstance.get(uuid);
      if (key != null) {
         ServerPlayerEntity pp = server.getPlayerManager().getPlayer(uuid);
         if (pp != null) {
            pp.setInvulnerable(false);
         }

         playerInstance.remove(uuid);
         playerPos.remove(uuid);
         lastTickPos.remove(uuid);
         playerDir.remove(uuid);
         encounterCountdown.remove(uuid);
         playerDistWalked.remove(uuid);
         DepthsDescentData data = DepthsDescentData.get(server);
         DepthsDescentData.Instance inst = data.getInstance(key);
         if (inst != null) {
            inst.playersInside.remove(uuid);
            data.markDirty();
            broadcastPositions(server, inst, key);
         }
      }
   }

   private static class StairsVote {
      final int total;
      int sealVotes = 0;
      int continueVotes = 0;
      final Set<UUID> voted = new HashSet<>();

      StairsVote(int total) {
         this.total = Math.max(1, total);
      }

      void cast(UUID uid, boolean seal) {
         if (this.voted.add(uid)) {
            if (seal) {
               this.sealVotes++;
            } else {
               this.continueVotes++;
            }
         }
      }

      boolean isComplete() {
         return this.voted.size() >= this.total;
      }

      boolean shouldSeal() {
         return this.sealVotes > this.continueVotes;
      }
   }
}
