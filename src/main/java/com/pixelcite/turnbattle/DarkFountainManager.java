package com.pixelcite.turnbattle;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.joml.Vector3f;

public class DarkFountainManager {
   private static final int ENCLOSURE_LIMIT = 1500;
   private static final int ENTRY_COOLDOWN = 40;
   private static final int SEAL_WAIT_TICKS = 20;
   private static final int SEAL_RISE_TO_FLASH_TICKS = 200;
   private static final int SEAL_FLASH_TICKS = 60;
   private static final int SEAL_TOTAL_TICKS = 280;
   static final int CREATE_SPARKLE_PAIR_COUNT = 10;
   static final int CREATE_SPARKLE1_HOLD_TICKS = 1;
   static final int CREATE_SPARKLE2_HOLD_TICKS = 2;
   static final int CREATE_SPARKLE_LEAD_TICKS = 1;
   static final int CREATE_SPARKLE_PAIR_LIFETIME_TICKS = 3;
   static final int CREATE_SPARKLE_SPAWN_INTERVAL_TICKS = 2;
   static final int CREATE_SPARKLE_TOTAL_TICKS = 21;
   private static final Map<UUID, DarkFountainManager.PendingFountain> pending = new HashMap<>();
   private static final Map<UUID, Long> entryCooldowns = new HashMap<>();
   private static final Map<UUID, Long> entryHold = new HashMap<>();
   private static final List<DarkFountainManager.ScheduledTask> scheduled = new ArrayList<>();
   private static final Map<Integer, List<long[]>> doorCache = new HashMap<>();
   private static final Map<RegistryKey<World>, Long> deleting = new HashMap<>();
   private static final Map<RegistryKey<World>, DarkFountainData.DarkWorldInstance> deletingInst = new HashMap<>();
   private static final DustParticleEffect BLACK_DUST = new DustParticleEffect(new Vector3f(0.0F, 0.0F, 0.0F), 4.0F);
   private static final Map<Integer, BlockPos> spawnCache = new HashMap<>();
   private static long lastSlowLog = 0L;

   public static boolean isDarkWorld(RegistryKey<World> key) {
      String id = key.getValue().toString();
      return id.startsWith("turnbattle:dark_world_");
   }

   public static void clearEntryCooldown(UUID uid) {
      entryCooldowns.remove(uid);
   }

   public static void onKnifeOpenMenu(ServerPlayerEntity player) {
      String blocked = openBlockedReason(player.getServer(), DarkFountainData.get(player.getServer()));
      if (blocked != null) {
         player.sendMessage(Text.literal(blocked).formatted(Formatting.DARK_GRAY), true);
      } else {
         BlockPos feet = player.getBlockPos();
         DarkFountainManager.EnclosureResult enc = checkEnclosure(player.getServerWorld(), feet);
         if (!enc.enclosed) {
            player.sendMessage(Text.literal("This area is not fully enclosed...").formatted(Formatting.DARK_GRAY), true);
         } else {
            pending.put(player.getUuid(), new DarkFountainManager.PendingFountain(enc.center, player.getWorld().getRegistryKey(), enc.airBlocks));
            PacketByteBuf buf = PacketByteBufs.create();
            writeCatalogue(player, buf);
            Net121.sendToClient(player, TurnBattleMod.DARK_WORLD_CREATE_MENU, buf);
         }
      }
   }

   private static void writeCatalogue(ServerPlayerEntity player, PacketByteBuf buf) {
      MinecraftServer server = player.getServer();
      DarkFountainData data = DarkFountainData.get(server);
      UUID u = player.getUuid();
      List shapes = new ArrayList<>(List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"));

      for (ServerWorld w : server.getWorlds()) {
         String id = w.getRegistryKey().getValue().toString();
         if (!shapes.contains(id) && !id.startsWith("turnbattle:") && DarkWorldGenerator.canLendTerrain(server, id)) {
            shapes.add(id);
         }
      }

      buf.writeVarInt(shapes.size());

      for (String id : shapes) {
         buf.writeString(id);
         buf.writeString(DarkWorldCatalog.shapeName(id));
         buf.writeBoolean(data.hasShape(u, id));
      }

      List biomes = new ArrayList<>(DarkWorldCatalog.CUSTOM);
      List rest = new ArrayList<>();

      for (Identifier id : server.getRegistryManager().get(RegistryKeys.BIOME).getIds()) {
         String s = id.toString();
         if (!DarkWorldCatalog.isHidden(s) && !biomes.contains(s)) {
            rest.add(s);
         }
      }

      rest.sort(Comparator.<String, Boolean>comparing(s -> !s.startsWith("minecraft:")).thenComparing(s -> (String)s));
      biomes.addAll(rest);
      buf.writeVarInt(biomes.size());

      for (String idx : biomes) {
         buf.writeString(idx);
         int state = DarkWorldCatalog.isComingSoon(idx) ? 2 : (!idx.equals("turnbattle:wool_fields") && !data.hasBiome(u, idx) ? 0 : 1);
         buf.writeByte(state);
      }

      String lastShape = data.lastShape.getOrDefault(u, "minecraft:overworld");
      buf.writeString("turnbattle:sky_islands".equals(lastShape) ? "minecraft:overworld" : lastShape);
      List last = data.lastBiomes.getOrDefault(u, List.of());
      buf.writeVarInt(last.size());

      for (String g : last) {
         buf.writeString(g);
      }
   }

   private static void discover(ServerPlayerEntity p, DarkFountainData data) {
      ServerWorld w = p.getServerWorld();
      String dim = w.getRegistryKey().getValue().toString();
      if (!dim.startsWith("turnbattle:") || isDarkWorld(w.getRegistryKey())) {
         boolean quiet = BattleManager.isInBattle(p.getUuid());
         if (!dim.startsWith("turnbattle:")
            && data.discoverDim(p.getUuid(), dim)
            && !DarkWorldCatalog.isDefaultShape(dim)
            && DarkWorldGenerator.canLendTerrain(p.getServer(), dim)
            && !quiet) {
            announce(p, Text.literal(DarkWorldCatalog.shapeName(dim)));
         }

         w.getBiome(p.getBlockPos())
            .getKey()
            .ifPresent(
               k -> {
                  String id = k.getValue().toString();
                  if (!DarkWorldCatalog.isHidden(id) && !DarkWorldCatalog.isDefaultBiome(id)) {
                     if (data.discoverBiome(p.getUuid(), id) && !quiet) {
                        announce(
                           p,
                           Text.translatableWithFallback("biome." + k.getValue().getNamespace() + "." + k.getValue().getPath(), DarkWorldCatalog.prettify(id))
                        );
                     }
                  }
               }
            );
      }
   }

   private static void announce(ServerPlayerEntity p, Text what) {
      p.sendMessage(Text.literal("\u2726 Your creativity grows: ").formatted(Formatting.GRAY).append(what.copy().formatted(Formatting.YELLOW)), true);
   }

   public static void broadcastSites(MinecraftServer server, DarkFountainData data) {
      broadcastSites(server, data, false);
   }

   public static void broadcastSites(MinecraftServer server, DarkFountainData data, boolean rescan) {
      List rooms = new ArrayList<>();

      for (DarkFountainData.DarkWorldInstance inst : data.instances.values()) {
         if (inst.roomCenter != null && inst.roomDimension != null && !inst.darkBlockPositions.isEmpty()) {
            rooms.add(inst);
         }
      }

      doorCache.keySet().removeIf(id -> !data.instances.containsKey(id));
      Map doors = new HashMap<>();

      for (DarkFountainData.DarkWorldInstance instx : rooms) {
         doors.put(instx.id, !rescan && doorCache.containsKey(instx.id) ? doorCache.get(instx.id) : doorsOf(server, instx));
      }

      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeVarInt(rooms.size());

         for (DarkFountainData.DarkWorldInstance instx : rooms) {
            buf.writeString(instx.roomDimension);
            buf.writeInt(instx.roomCenter.getX());
            buf.writeInt(instx.roomCenter.getY());
            buf.writeInt(instx.roomCenter.getZ());
            List ds = doors.get(instx.id);
            buf.writeVarInt(ds.size());

            for (long[] d : ds) {
               buf.writeLong(d[0]);
               buf.writeByte((int)d[1]);
            }
         }

         buf.writeVarInt(data.instances.size());

         for (DarkFountainData.DarkWorldInstance instx : data.instances.values()) {
            buf.writeString(instx.getWorldKey().getValue().toString());
            buf.writeInt(instx.fountainX);
            buf.writeInt(instx.fountainZ);
         }

         Net121.sendToClient(p, TurnBattleMod.DARK_FOUNTAIN_SITES, buf);
      }
   }

   private static List<long[]> doorsOf(MinecraftServer server, DarkFountainData.DarkWorldInstance inst) {
      ServerWorld w = server.getWorld(inst.getRoomDimKey());
      if (w == null) {
         return doorCache.getOrDefault(inst.id, List.of());
      } else {
         Map found = new LinkedHashMap<>();
         boolean looked = false;

         for (long l : inst.darkBlockPositions) {
            BlockPos p = BlockPos.fromLong(l);
            if (w.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
               looked = true;

               for (Direction d : Type.HORIZONTAL) {
                  BlockPos q = p.offset(d);
                  if (!inst.darkBlockPositions.contains(q.asLong()) && w.getChunkManager().isChunkLoaded(q.getX() >> 4, q.getZ() >> 4)) {
                     BlockState st = w.getBlockState(q);
                     if (st.getBlock() instanceof DoorBlock && !inst.darkBlockPositions.contains(q.offset(d).asLong())) {
                        BlockPos lower = st.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? q.down() : q;
                        found.putIfAbsent(lower.asLong(), d.getId());
                     }
                  }
               }
            }
         }

         if (!looked) {
            return doorCache.getOrDefault(inst.id, List.of());
         } else {
            List out = new ArrayList<>();

            for (Entry e : found.entrySet()) {
               out.add(new long[]{e.getKey(), (long)e.getValue().intValue()});
            }

            doorCache.put(inst.id, out);
            return out;
         }
      }
   }

   static String openBlockedReason(MinecraftServer server, DarkFountainData data) {
      if (data.maxActiveWorlds > 0 && data.instances.size() >= data.maxActiveWorlds) {
         return "The darkness is spread too thin... (" + data.instances.size() + "/" + data.maxActiveWorlds + " Dark Worlds are open)";
      } else {
         long left = cooldownLeft(server, data);
         return left > 0L ? "The darkness is still settling... (" + formatTicks(left) + ")" : null;
      }
   }

   static long cooldownLeft(MinecraftServer server, DarkFountainData data) {
      return data.openCooldownSecs > 0 && data.lastOpenedAt >= 0L
         ? Math.max(0L, data.lastOpenedAt + (long)data.openCooldownSecs * 20L - server.getOverworld().getTime())
         : 0L;
   }

   static String formatTicks(long ticks) {
      long secs = (ticks + 19L) / 20L;
      if (secs < 60L) {
         return secs + "s";
      } else {
         return secs < 3600L ? secs / 60L + "m " + secs % 60L + "s" : secs / 3600L + "h " + secs % 3600L / 60L + "m";
      }
   }

   static DarkFountainData.DarkWorldInstance closestTo(ServerPlayerEntity player, DarkFountainData data) {
      DarkFountainData.DarkWorldInstance in = data.getInstanceByWorldKey(player.getWorld().getRegistryKey());
      if (in != null) {
         return in;
      } else {
         String dim = player.getWorld().getRegistryKey().getValue().toString();
         DarkFountainData.DarkWorldInstance best = null;
         double bestD = Double.MAX_VALUE;

         for (DarkFountainData.DarkWorldInstance inst : data.instances.values()) {
            if (inst.roomCenter != null && dim.equals(inst.roomDimension)) {
               double d = inst.roomCenter.getSquaredDistance(player.getPos());
               if (d < bestD) {
                  bestD = d;
                  best = inst;
               }
            }
         }

         return best;
      }
   }

   public static void adminSeal(MinecraftServer server, DarkFountainData.DarkWorldInstance inst) {
      collapseDarkWorld(server, inst, DarkFountainData.get(server));
   }

   private static String describe(MinecraftServer server, DarkFountainData.DarkWorldInstance inst) {
      String who = "unknown";
      if (inst.creatorUuid != null) {
         ServerPlayerEntity c = server.getPlayerManager().getPlayer(inst.creatorUuid);
         if (c != null) {
            who = c.getName().getString();
         } else {
            Optional prof = server.getUserCache() != null ? server.getUserCache().getByUuid(inst.creatorUuid) : Optional.empty();
            if (prof.isPresent()) {
               who = prof.get().getName();
            }
         }
      }

      String where = inst.roomCenter == null ? "?" : inst.roomCenter.getX() + " " + inst.roomCenter.getY() + " " + inst.roomCenter.getZ();
      return "#" + inst.id + " (made by " + who + ", room at " + where + " in " + inst.roomDimension + ")";
   }

   private static void registerAdminCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundfountainlimit")
                  .requires(src -> src.hasPermissionLevel(2)))
               .executes(
                  ctx -> {
                     DarkFountainData data = DarkFountainData.get(((ServerCommandSource)ctx.getSource()).getServer());
                     int open = data.instances.size();
                     int max = data.maxActiveWorlds;
                     ((ServerCommandSource)ctx.getSource())
                        .sendFeedback(
                           () -> Text.literal(
                                 "Dark Worlds open: "
                                    + open
                                    + "  |  limit: "
                                    + (max > 0 ? String.valueOf(max) : "none")
                                    + (pendingDeletions() > 0 ? "  |  still being removed: " + pendingDeletions() : "")
                              ),
                           false
                        );
                     return open;
                  }
               ))
            .then(
               CommandManager.argument("max", IntegerArgumentType.integer(0, 10000))
                  .executes(
                     ctx -> {
                        int max = IntegerArgumentType.getInteger(ctx, "max");
                        DarkFountainData data = DarkFountainData.get(((ServerCommandSource)ctx.getSource()).getServer());
                        data.maxActiveWorlds = max;
                        data.markDirty();
                        int open = data.instances.size();
                        ((ServerCommandSource)ctx.getSource())
                           .sendFeedback(
                              () -> Text.literal(
                                    max > 0
                                       ? "At most "
                                          + max
                                          + " Dark World(s) can be open at once ("
                                          + open
                                          + " open now"
                                          + (open > max ? " - no new ones until enough are sealed" : "")
                                          + ")."
                                       : "No limit on how many Dark Worlds can be open."
                                 ),
                              true
                           );
                        return max;
                     }
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundfountaincooldown")
                     .requires(src -> src.hasPermissionLevel(2)))
                  .executes(
                     ctx -> {
                        MinecraftServer server = ((ServerCommandSource)ctx.getSource()).getServer();
                        DarkFountainData data = DarkFountainData.get(server);
                        long left = cooldownLeft(server, data);
                        int secs = data.openCooldownSecs;
                        ((ServerCommandSource)ctx.getSource())
                           .sendFeedback(
                              () -> Text.literal(
                                    "Dark Fountain cooldown: "
                                       + (secs > 0 ? formatTicks((long)secs * 20L) : "none")
                                       + (left > 0L ? "  |  next one can open in " + formatTicks(left) : "  |  one can open now")
                                 ),
                              false
                           );
                        return secs;
                     }
                  ))
               .then(CommandManager.literal("reset").executes(ctx -> {
                  DarkFountainData data = DarkFountainData.get(((ServerCommandSource)ctx.getSource()).getServer());
                  data.lastOpenedAt = -1L;
                  data.markDirty();
                  ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("Dark Fountain cooldown cleared - one can open now."), true);
                  return 1;
               })))
            .then(
               CommandManager.argument("seconds", IntegerArgumentType.integer(0, 604800))
                  .executes(
                     ctx -> {
                        int secs = IntegerArgumentType.getInteger(ctx, "seconds");
                        DarkFountainData data = DarkFountainData.get(((ServerCommandSource)ctx.getSource()).getServer());
                        data.openCooldownSecs = secs;
                        data.markDirty();
                        ((ServerCommandSource)ctx.getSource())
                           .sendFeedback(
                              () -> Text.literal(
                                    secs > 0
                                       ? "Dark Fountains can open at most once every " + formatTicks((long)secs * 20L) + ", server-wide."
                                       : "No cooldown between Dark Fountains."
                                 ),
                              true
                           );
                        return secs;
                     }
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundfountainseal")
                  .requires(src -> src.hasPermissionLevel(2)))
               .then(
                  CommandManager.literal("close")
                     .executes(
                        ctx -> {
                           ServerPlayerEntity player = ((ServerCommandSource)ctx.getSource()).getPlayer();
                           if (player == null) {
                              ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("Run this as a player (or use 'all')."));
                              return 0;
                           } else {
                              MinecraftServer server = ((ServerCommandSource)ctx.getSource()).getServer();
                              DarkFountainData data = DarkFountainData.get(server);
                              DarkFountainData.DarkWorldInstance inst = closestTo(player, data);
                              if (inst == null) {
                                 ((ServerCommandSource)ctx.getSource())
                                    .sendError(Text.literal("No Dark Fountain here - none in this dimension, and you're not in a Dark World."));
                                 return 0;
                              } else {
                                 String what = describe(server, inst);
                                 adminSeal(server, inst);
                                 ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("Sealing Dark World " + what + "."), true);
                                 return 1;
                              }
                           }
                        }
                     )
               ))
            .then(CommandManager.literal("all").executes(ctx -> {
               MinecraftServer server = ((ServerCommandSource)ctx.getSource()).getServer();
               DarkFountainData data = DarkFountainData.get(server);
               List all = new ArrayList<>(data.instances.values());
               if (all.isEmpty()) {
                  ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("There are no Dark Worlds open."), false);
                  return 0;
               } else {
                  for (DarkFountainData.DarkWorldInstance inst : all) {
                     adminSeal(server, inst);
                  }

                  int n = all.size();
                  ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("Sealing all " + n + " Dark World(s)."), true);
                  return n;
               }
            }))
      );
   }

   public static void registerCommands() {
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, reg, env) -> registerAdminCommands(dispatcher));
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, reg, env) -> dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundcreativity")
                           .requires(src -> src.hasPermissionLevel(2)))
                        .then(
                           CommandManager.literal("unlockall")
                              .then(
                                 CommandManager.argument("player", EntityArgumentType.player())
                                    .executes(
                                       ctx -> {
                                          ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                          MinecraftServer server = ((ServerCommandSource)ctx.getSource()).getServer();
                                          DarkFountainData data = DarkFountainData.get(server);
                                          int n = 0;

                                          for (Identifier id : server.getRegistryManager().get(RegistryKeys.BIOME).getIds()) {
                                             String s = id.toString();
                                             if (!DarkWorldCatalog.isHidden(s)
                                                && !DarkWorldCatalog.isDefaultBiome(s)
                                                && data.discoverBiome(target.getUuid(), s)) {
                                                n++;
                                             }
                                          }

                                          for (ServerWorld w : server.getWorlds()) {
                                             String s = w.getRegistryKey().getValue().toString();
                                             if (!s.startsWith("turnbattle:") && data.discoverDim(target.getUuid(), s)) {
                                                n++;
                                             }
                                          }

                                          int count = n;
                                          ((ServerCommandSource)ctx.getSource())
                                             .sendFeedback(
                                                () -> Text.literal(
                                                      "Unlocked " + count + " biome(s)/dimension(s) for " + target.getName().getString() + "'s Dark Worlds."
                                                   ),
                                                true
                                             );
                                          return 1;
                                       }
                                    )
                              )
                        ))
                     .then(
                        CommandManager.literal("reset")
                           .then(
                              CommandManager.argument("player", EntityArgumentType.player())
                                 .executes(
                                    ctx -> {
                                       ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                       DarkFountainData data = DarkFountainData.get(((ServerCommandSource)ctx.getSource()).getServer());
                                       data.seenBiomes.remove(target.getUuid());
                                       data.seenDims.remove(target.getUuid());
                                       data.lastBiomes.remove(target.getUuid());
                                       data.lastShape.remove(target.getUuid());
                                       data.markDirty();
                                       ((ServerCommandSource)ctx.getSource())
                                          .sendFeedback(
                                             () -> Text.literal(target.getName().getString() + "'s encountered biomes and dimensions were forgotten."), true
                                          );
                                       return 1;
                                    }
                                 )
                           )
                     )
               )
         );
   }

   public static void onCreateConfirm(ServerPlayerEntity player, boolean uniqueColor) {
      onCreateConfirm(player, uniqueColor, "minecraft:overworld", List.of());
   }

   public static void onCreateConfirm(ServerPlayerEntity player, boolean uniqueColor, String shape, List<String> groups) {
      DarkFountainManager.PendingFountain pf = pending.remove(player.getUuid());
      if (pf != null) {
         MinecraftServer server = player.getServer();
         DarkFountainData data = DarkFountainData.get(server);
         UUID uid = player.getUuid();
         String blocked = openBlockedReason(server, data);
         if (blocked != null) {
            player.sendMessage(Text.literal(blocked).formatted(Formatting.DARK_GRAY), true);
         } else {
            if (shape == null || "turnbattle:sky_islands".equals(shape) || !data.hasShape(uid, shape) || !DarkWorldGenerator.canLendTerrain(server, shape)) {
               shape = "minecraft:overworld";
            }

            Registry biomeReg = server.getRegistryManager().get(RegistryKeys.BIOME);
            List chosen = new ArrayList<>();

            for (String g : groups) {
               List ok = new ArrayList<>();

               for (String id : DarkWorldCatalog.splitGroup(g)) {
                  Identifier bid = Identifier.tryParse(id);
                  if (bid != null
                     && biomeReg.containsId(bid)
                     && !DarkWorldCatalog.isComingSoon(id)
                     && (!DarkWorldCatalog.isHidden(id) || DarkWorldCatalog.woolIndex(id) >= 0)
                     && data.hasBiome(uid, id)) {
                     ok.add(id);
                  }
               }

               String joined = DarkWorldCatalog.joinGroup(ok);
               if (!joined.isEmpty() && !chosen.contains(joined)) {
                  chosen.add(joined);
               }

               if (chosen.size() >= 128) {
                  break;
               }
            }

            if (chosen.isEmpty()) {
               chosen.add("turnbattle:dw_sky_islands");
            }

            data.lastShape.put(uid, shape);
            data.lastBiomes.put(uid, new ArrayList<>(chosen));
            DarkFountainData.DarkWorldInstance inst = data.createInstance();
            inst.roomCenter = pf.roomCenter;
            inst.roomDimension = pf.dimension.getValue().toString();
            inst.shapeId = shape;
            inst.biomeGroups.addAll(chosen);
            Random fr = new Random(inst.seed ^ 1594784285L);

            do {
               inst.fountainX = fr.nextInt(2001) - 1000;
               inst.fountainZ = fr.nextInt(2001) - 1000;
            } while (sq((double)(inst.fountainX - 50)) + sq((double)inst.fountainZ) < 40000.0);

            inst.creatorUuid = player.getUuid();
            inst.darknerItems = Darkners.scanRoom(player.getServerWorld(), pf.airBlocks);
            DarknerBosses.recordCreator(inst, BattleData.get(server).getOrCreate(player.getUuid()));
            inst.tintColor = uniqueColor ? data.getOrRollPlayerTint(player.getUuid()) : -1;
            inst.creatorReturnPos = findExitPos(player.getServerWorld(), pf.roomCenter);
            inst.mobLevel = Math.max(1, BattleData.get(server).getOrCreate(player.getUuid()).level);
            data.lastOpenedAt = server.getOverworld().getTime();
            data.markDirty();
            List teleportGroup = new ArrayList<>();
            teleportGroup.add(player);

            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
               if (other != player
                  && other.getWorld().getRegistryKey().equals(pf.dimension)
                  && (
                     blockDist(other.getBlockPos(), pf.roomCenter) <= 25
                        || pf.airBlocks.contains(other.getBlockPos())
                        || pf.airBlocks.contains(other.getBlockPos().up())
                  )) {
                  teleportGroup.add(other);
               }
            }

            data.markDirty();
            ServerWorld roomWorldNow = player.getServerWorld();
            float yawDeg = player.getYaw();
            float yawRad = (float)Math.toRadians((double)yawDeg);
            double fwdX = -Math.sin((double)yawRad);
            double fwdZ = Math.cos((double)yawRad);
            double beamX = player.getX() + fwdX * 1.0;
            double beamY = player.getY();
            double beamZ = player.getZ() + fwdZ * 1.0;
            BlockPos beamPos = BlockPos.ofFloored(beamX, beamY + 0.5, beamZ);
            PlayerActionNet.broadcast(player, 0, true, b -> {
               b.writeDouble(beamX);
               b.writeDouble(beamY);
               b.writeDouble(beamZ);
            });
            long holdUntil = roomWorldNow.getTime() + 2400L;

            for (ServerPlayerEntity p : teleportGroup) {
               entryHold.put(p.getUuid(), holdUntil);
            }

            double sparkleHalfWidth = 1.4;
            double rightDirX = -Math.cos((double)yawRad);
            double rightDirZ = -Math.sin((double)yawRad);
            double sparkleCenterX = player.getX();
            double sparkleCenterZ = player.getZ();
            double sparkleLeftX = sparkleCenterX - rightDirX * sparkleHalfWidth;
            double sparkleLeftZ = sparkleCenterZ - rightDirZ * sparkleHalfWidth;
            double sparkleRightX = sparkleCenterX + rightDirX * sparkleHalfWidth;
            double sparkleRightZ = sparkleCenterZ + rightDirZ * sparkleHalfWidth;
            double sparkleBaseY = player.getY() + (double)player.getHeight() + 0.55;
            scheduleAt(server, 12, () -> {
               for (ServerPlayerEntity p : teleportGroup) {
                  PacketByteBuf sb = PacketByteBufs.create();
                  sb.writeDouble(sparkleLeftX);
                  sb.writeDouble(sparkleLeftZ);
                  sb.writeDouble(sparkleRightX);
                  sb.writeDouble(sparkleRightZ);
                  sb.writeDouble(sparkleBaseY);
                  Net121.sendToClient(p, TurnBattleMod.DARK_FOUNTAIN_CREATE_SPARKLE, sb);
               }
            });

            for (int i = 0; i < 10; i++) {
               int spawnTick = 12 + i * 2;
               scheduleAt(
                  server,
                  spawnTick,
                  () -> player.getServerWorld().playSound(null, player.getBlockPos(), TurnBattleSounds.FOUNTAIN_TARGET, SoundCategory.MASTER, 1.0F, 1.0F)
               );
            }

            Set airSnapshot = pf.airBlocks;
            RegistryKey roomDim = pf.dimension;
            DarkFountainData.DarkWorldInstance fInst = inst;
            boolean[] created = new boolean[]{false};
            int plunge = 33;
            scheduleAt(server, 33, () -> PlayerActionNet.broadcast(player, 1, true, null));
            scheduleAt(server, 38, () -> roomWorldNow.playSound(null, beamPos, TurnBattleSounds.FOUNTAIN_MAKE, SoundCategory.MASTER, 1.0F, 1.0F));
            scheduleAt(server, 40, () -> {
               if (data.instances.containsKey(inst.id)) {
                  try {
                     DarkWorldGenerator.createDimension(server, inst);
                  } catch (Exception var9x) {
                     TurnBattleMod.LOGGER.error("[DarkFountain] Failed to create dimension for instance " + inst.id, var9x);
                     player.sendMessage(Text.literal("Something went wrong creating your Dark World.").formatted(Formatting.RED), false);
                     data.removeInstance(inst.id);

                     for (ServerPlayerEntity p : teleportGroup) {
                        entryHold.remove(p.getUuid());
                     }

                     PlayerActionNet.broadcast(player, 2, true, null);
                     return;
                  }

                  created[0] = true;
                  ServerWorld newDarkWorld = server.getWorld(inst.getWorldKey());
                  if (newDarkWorld != null) {
                     placeFountainPillar(newDarkWorld, inst);
                  }
               } else {
                  for (ServerPlayerEntity p : teleportGroup) {
                     entryHold.remove(p.getUuid());
                  }

                  PlayerActionNet.broadcast(player, 2, true, null);
               }
            });
            Map waves = new TreeMap<>();
            int topY = Integer.MIN_VALUE;
            int botY = Integer.MAX_VALUE;
            double maxH = 1.0;

            for (BlockPos b : airSnapshot) {
               topY = Math.max(topY, b.getY());
               botY = Math.min(botY, b.getY());
               maxH = Math.max(maxH, Math.sqrt(sq((double)b.getX() + 0.5 - beamX) + sq((double)b.getZ() + 0.5 - beamZ)));
            }

            double span = (double)Math.max(1, topY - botY);

            for (BlockPos b : airSnapshot) {
               double down = (double)(topY - b.getY()) / span;
               double out = Math.sqrt(sq((double)b.getX() + 0.5 - beamX) + sq((double)b.getZ() + 0.5 - beamZ)) / maxH;
               int wave = (int)Math.round((down * 0.82 + out * 0.18) * 70.0);
               waves.computeIfAbsent(wave, k -> new ArrayList<>()).add(b);
            }

            for (Entry w : waves.entrySet()) {
               List batch = w.getValue();
               scheduleAt(server, 90 + w.getKey(), () -> {
                  if (created[0] && data.instances.containsKey(fInst.id)) {
                     ServerWorld roomWorld = server.getWorld(roomDim);
                     if (roomWorld != null) {
                        fillRoomWithDarkBlocks(roomWorld, batch, fInst);
                        data.markDirty();
                     }
                  }
               });
            }

            scheduleAt(
               server,
               166,
               () -> {
                  if (created[0]) {
                     for (ServerPlayerEntity p : teleportGroup) {
                        if (p.getWorld().getRegistryKey().equals(roomDim)
                           && (airSnapshot.contains(p.getBlockPos()) || airSnapshot.contains(p.getBlockPos().up()))) {
                           PacketByteBuf fb = PacketByteBufs.create();
                           fb.writeBoolean(true);
                           fb.writeInt(fInst.tintColor);
                           fb.writeInt(-1);
                           fb.writeBoolean(false);
                           Net121.sendToClient(p, TurnBattleMod.DARK_FOUNTAIN_FADE, fb);
                        }
                     }
                  }
               }
            );
            scheduleAt(server, 188, () -> {
               for (ServerPlayerEntity p : teleportGroup) {
                  entryHold.remove(p.getUuid());
               }
            });
         }
      }
   }

   public static void onCancel(ServerPlayerEntity player) {
      pending.remove(player.getUuid());
   }

   public static void tick(MinecraftServer server) {
      DarkFountainData data = DarkFountainData.get(server);
      long tickNo = (long)server.getTicks();

      for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
         if ((tickNo & 3L) != 0L) {
            break;
         }

         if (!isDarkWorld(player.getWorld().getRegistryKey())) {
            tryEnterAt(player, player.getBlockPos());
            tryEnterAt(player, player.getBlockPos().up());
         }
      }

      long overTime = server.getOverworld().getTime();
      if (overTime % 100L == 37L) {
         long t0 = System.nanoTime();
         broadcastSites(server, data, overTime % 600L == 37L);
         slow("broadcastSites", t0);
      }

      if (overTime % 20L == 3L) {
         for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            discover(player, data);
         }
      }

      for (DarkFountainData.DarkWorldInstance inst : data.instances.values()) {
         ServerWorld instWorld = server.getWorld(inst.getWorldKey());
         if (instWorld == null) {
         }
      }

      for (DarkFountainData.DarkWorldInstance instx : data.instances.values()) {
         instx.playersInside.removeIf(uid -> server.getPlayerManager().getPlayer(uid) == null);
      }

      if (overTime % 600L == 105L) {
         for (DarkFountainData.DarkWorldInstance instx : data.instances.values()) {
            instx.playersInside.removeIf(uid -> {
               ServerPlayerEntity sp = server.getPlayerManager().getPlayer(uid);
               return sp != null && !inst.getWorldKey().equals(sp.getWorld().getRegistryKey());
            });
         }
      }

      if (overTime % 600L == 305L) {
         for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (isDarkWorld(p.getWorld().getRegistryKey())) {
               DarkFountainData.DarkWorldInstance instx = data.getInstanceByWorldKey(p.getWorld().getRegistryKey());
               if (instx != null) {
                  PacketByteBuf tb = PacketByteBufs.create();
                  tb.writeBoolean(true);
                  tb.writeInt(instx.tintColor);
                  tb.writeInt(instx.id);
                  tb.writeBoolean(true);
                  Net121.sendToClient(p, TurnBattleMod.DARK_FOUNTAIN_FADE, tb);
               }
            }
         }
      }
   }

   public static void tickScheduled(MinecraftServer server) {
      long now = server.getOverworld().getTime();
      List due = new ArrayList<>();
      scheduled.removeIf(t -> {
         if (now >= t.at) {
            due.add(t);
            return true;
         } else {
            return false;
         }
      });

      for (DarkFountainManager.ScheduledTask t : due) {
         long t0 = System.nanoTime();

         try {
            t.task.run();
         } catch (Exception var9) {
            TurnBattleMod.LOGGER.error("[DarkFountain] Scheduled task failed", var9);
         }

         slow("a scheduled Dark Fountain task", t0);
      }

      tickDeletions(server);
   }

   private static void enterDarkWorld(ServerPlayerEntity player, DarkFountainData.DarkWorldInstance inst, DarkFountainData data, MinecraftServer server) {
      inst.playersInside.add(player.getUuid());
      data.markDirty();
      ServerWorld darkWorld = server.getWorld(inst.getWorldKey());
      if (darkWorld != null) {
         PacketByteBuf fb = PacketByteBufs.create();
         fb.writeBoolean(true);
         fb.writeInt(inst.tintColor);
         fb.writeInt(inst.id);
         fb.writeBoolean(true);
         Net121.sendToClient(player, TurnBattleMod.DARK_FOUNTAIN_FADE, fb);
         broadcastSites(server, data);
         long t0 = System.nanoTime();
         BlockPos spawn = spawnFor(darkWorld, inst);
         slow("finding a spawn spot", t0);
         player.teleport(darkWorld, (double)spawn.getX() + 0.5, (double)spawn.getY(), (double)spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
         StoryManager.onDarkWorldEnter(player);
      }
   }

   public static void tryEnterAt(ServerPlayerEntity player, BlockPos blockPos) {
      if (!isDarkWorld(player.getWorld().getRegistryKey())) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            UUID uid = player.getUuid();
            if (!SoulWalk.isOut(uid)) {
               long now = player.getServerWorld().getTime();
               Long last = entryCooldowns.get(uid);
               if (last == null || now - last >= 40L) {
                  Long hold = entryHold.get(uid);
                  if (hold != null) {
                     if (now < hold) {
                        return;
                     }

                     entryHold.remove(uid);
                  }

                  if (player.getServerWorld().getBlockState(blockPos).isOf(TurnBattleMod.DARK_BLOCK)) {
                     DarkFountainData data = DarkFountainData.get(server);
                     String dimStr = player.getWorld().getRegistryKey().getValue().toString();
                     long posL = blockPos.asLong();

                     for (DarkFountainData.DarkWorldInstance inst : data.instances.values()) {
                        if (dimStr.equals(inst.roomDimension) && inst.darkBlockPositions.contains(posL)) {
                           entryCooldowns.put(uid, now);
                           enterDarkWorld(player, inst, data, server);
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean tryForceEnter(ServerPlayerEntity player, BlockPos clickedPos) {
      if (isDarkWorld(player.getWorld().getRegistryKey())) {
         return false;
      } else if (SoulWalk.isOut(player.getUuid())) {
         return false;
      } else {
         MinecraftServer server = player.getServer();
         DarkFountainData data = DarkFountainData.get(server);
         String dimStr = player.getWorld().getRegistryKey().getValue().toString();
         long posL = clickedPos.asLong();

         for (DarkFountainData.DarkWorldInstance inst : data.instances.values()) {
            if (dimStr.equals(inst.roomDimension) && inst.darkBlockPositions.contains(posL)) {
               enterDarkWorld(player, inst, data, server);
               return true;
            }
         }

         return false;
      }
   }

   public static void exitDarkWorld(ServerPlayerEntity player, MinecraftServer server) {
      exitDarkWorld(player, server, 20);
   }

   public static void exitDarkWorld(ServerPlayerEntity player, MinecraftServer server, int delayTicks) {
      DarkFountainData data = DarkFountainData.get(server);
      DarkFountainData.DarkWorldInstance inst = data.getInstanceByWorldKey(player.getWorld().getRegistryKey());
      if (inst == null) {
         inst = data.getInstanceForPlayer(player.getUuid());
      }

      if (inst != null) {
         inst.playersInside.remove(player.getUuid());
         data.markDirty();
      }

      RegistryKey returnKey = inst != null ? inst.getRoomDimKey() : World.OVERWORLD;
      ServerWorld returnWorld = server.getWorld(returnKey);
      if (returnWorld == null) {
         returnWorld = server.getOverworld();
      }

      BlockPos returnPos = findRoomExitPos(returnWorld, inst);
      PacketByteBuf fb = PacketByteBufs.create();
      fb.writeBoolean(false);
      fb.writeInt(0);
      fb.writeInt(-1);
      fb.writeBoolean(false);
      Net121.sendToClient(player, TurnBattleMod.DARK_FOUNTAIN_FADE, fb);
      ServerWorld fWorld = returnWorld;
      Runnable go = () -> player.teleport(
            fWorld, (double)returnPos.getX() + 0.5, (double)returnPos.getY(), (double)returnPos.getZ() + 0.5, player.getYaw(), player.getPitch()
         );
      if (delayTicks <= 0) {
         go.run();
      } else {
         scheduleAt(server, delayTicks, go);
      }
   }

   private static BlockPos findRoomExitPos(ServerWorld world, DarkFountainData.DarkWorldInstance inst) {
      if (inst != null && inst.roomCenter != null) {
         int lowestY = Integer.MAX_VALUE;

         for (long packed : inst.darkBlockPositions) {
            int y = BlockPos.fromLong(packed).getY();
            if (y < lowestY) {
               lowestY = y;
            }
         }

         if (lowestY == Integer.MAX_VALUE) {
            return inst.creatorReturnPos != null ? inst.creatorReturnPos : inst.roomCenter;
         } else {
            BlockPos centre = inst.roomCenter;

            for (int dist = 4; dist <= 25; dist++) {
               for (int step = 0; step < 16; step++) {
                  double ang = (double)step * Math.PI / 8.0;
                  int ex = centre.getX() + (int)((double)dist * Math.cos(ang));
                  int ez = centre.getZ() + (int)((double)dist * Math.sin(ang));
                  BlockPos p = new BlockPos(ex, lowestY, ez);
                  if (isSafe(world, p)) {
                     return p;
                  }
               }
            }

            return inst.creatorReturnPos != null ? inst.creatorReturnPos : centre;
         }
      } else {
         return world.getSpawnPos();
      }
   }

   private static void collapseDarkWorld(MinecraftServer server, DarkFountainData.DarkWorldInstance inst, DarkFountainData data) {
      collapseDarkWorld(server, inst, data, null);
   }

   private static void collapseDarkWorld(MinecraftServer server, DarkFountainData.DarkWorldInstance inst, DarkFountainData data, ServerPlayerEntity sealer) {
      if (data.instances.containsKey(inst.id)) {
         for (ServerPlayerEntity p : new ArrayList(server.getPlayerManager().getPlayerList())) {
            if (SoulWalk.isOut(p.getUuid()) && p.getWorld().getRegistryKey().equals(inst.getWorldKey())) {
               SoulWalk.returnToBody(p, false);
            }
         }

         ServerWorld returnWorld = server.getWorld(inst.getRoomDimKey());
         if (returnWorld == null) {
            returnWorld = server.getOverworld();
         }

         ServerWorld fReturn = returnWorld;
         RegistryKey darkKey = inst.getWorldKey();
         Set inside = new HashSet<>(inst.playersInside);
         ServerWorld darkNow = server.getWorld(darkKey);
         if (darkNow != null) {
            for (ServerPlayerEntity px : darkNow.getPlayers()) {
               inside.add(px.getUuid());
            }
         }

         for (Entry e : data.offlineIn.entrySet()) {
            if (e.getValue() == inst.id) {
               inside.add(e.getKey());
            }
         }

         List insidePlayers = new ArrayList<>();

         for (UUID uid : inside) {
            ServerPlayerEntity px = server.getPlayerManager().getPlayer(uid);
            if (px != null && px.getWorld().getRegistryKey().equals(darkKey)) {
               insidePlayers.add(px);
            }
         }

         for (ServerPlayerEntity px : insidePlayers) {
            TurnBattleMod.sealingPlayers.add(px.getUuid());
            TurnBattleMod.forceRevealOff(server, px);
            px.extinguish();
         }

         ServerPlayerEntity anchor = sealer != null && insidePlayers.contains(sealer) ? sealer : (insidePlayers.isEmpty() ? null : insidePlayers.get(0));
         if (anchor != null) {
            insidePlayers.remove(anchor);
            insidePlayers.add(0, anchor);
         }

         for (ServerPlayerEntity px : insidePlayers) {
            Net121.sendToClient(px, TurnBattleMod.DARK_FOUNTAIN_SEAL_START, buildSealStartPacket(server, insidePlayers, inst));
            px.getServerWorld().playSound(null, px.getBlockPos(), TurnBattleSounds.GREAT_SHINE, SoundCategory.MASTER, 1.0F, 1.0F);
         }

         scheduleAt(server, 20, () -> {
            for (UUID uidx : inside) {
               ServerPlayerEntity px = server.getPlayerManager().getPlayer(uidx);
               if (px != null && px.getWorld().getRegistryKey().equals(darkKey)) {
                  px.getServerWorld().playSound(null, px.getBlockPos(), TurnBattleSounds.USE_FOUNTAIN, SoundCategory.MASTER, 1.0F, 1.0F);
               }
            }
         });
         scheduleAt(server, 220, () -> {
            for (UUID uidx : inside) {
               ServerPlayerEntity px = server.getPlayerManager().getPlayer(uidx);
               if (px != null && px.getWorld().getRegistryKey().equals(darkKey)) {
                  px.getServerWorld().playSound(null, px.getBlockPos(), TurnBattleSounds.REVIVAL, SoundCategory.MASTER, 1.0F, 1.0F);
               }
            }
         });
         Set darkBlocks = new HashSet<>(inst.darkBlockPositions);
         int instanceId = inst.id;
         data.removeInstance(instanceId);
         doorCache.remove(instanceId);
         broadcastSites(server, data);
         scheduleAt(server, 280, () -> {
            for (UUID uidx : inside) {
               TurnBattleMod.sealingPlayers.remove(uidx);
            }

            BlockPos rp = findRoomExitPos(fReturn, inst);
            String returnDim = fReturn.getRegistryKey().getValue().toString();

            for (UUID uidx : inside) {
               ServerPlayerEntity px = server.getPlayerManager().getPlayer(uidx);
               if (px != null) {
                  if (px.getWorld().getRegistryKey().equals(darkKey)) {
                     px.teleport(fReturn, (double)rp.getX() + 0.5, (double)rp.getY(), (double)rp.getZ() + 0.5, px.getYaw(), px.getPitch());
                  }
               } else if (Integer.valueOf(instanceId).equals(data.offlineIn.get(uidx))) {
                  data.pendingExile.put(uidx, rp);
                  data.pendingExileDim.put(uidx, returnDim);
                  data.offlineIn.remove(uidx);
               }
            }

            data.markDirty();
            Map byChunk = new HashMap<>();

            for (long packed : darkBlocks) {
               BlockPos bp = BlockPos.fromLong(packed);
               byChunk.computeIfAbsent(ChunkPos.toLong(bp.getX() >> 4, bp.getZ() >> 4), k -> new ArrayList<>()).add(bp);
            }

            for (List chunk : byChunk.values()) {
               int cx = chunk.get(0).getX() >> 4;
               int cz = chunk.get(0).getZ() >> 4;
               boolean wasForced = fReturn.getForcedChunks().contains(ChunkPos.toLong(cx, cz));
               if (!wasForced) {
                  fReturn.setChunkForced(cx, cz, true);
               }

               try {
                  for (BlockPos bp : chunk) {
                     if (fReturn.getBlockState(bp).isOf(TurnBattleMod.DARK_BLOCK)) {
                        fReturn.setBlockState(bp, Blocks.AIR.getDefaultState());
                     }
                  }
               } finally {
                  if (!wasForced) {
                     fReturn.setChunkForced(cx, cz, false);
                  }
               }
            }

            releaseAndDelete(server, inst);
            TurnBattleMod.LOGGER.info("[DarkFountain] Sealed and removed dimension for instance {}", instanceId);
         });
      }
   }

   private static void releaseAndDelete(MinecraftServer server, DarkFountainData.DarkWorldInstance inst) {
      RegistryKey key = inst.getWorldKey();
      ServerWorld w = server.getWorld(key);
      if (w != null) {
         releaseChunks(server, w);
         deleting.put(key, server.getOverworld().getTime());
         deletingInst.put(key, inst);
      }

      try {
         DarkWorldGenerator.removeDimension(server, inst);
      } catch (Exception var5) {
         TurnBattleMod.LOGGER.error("[DarkFountain] Failed to delete dimension {}", key.getValue(), var5);
      }
   }

   private static void releaseChunks(MinecraftServer server, ServerWorld w) {
      for (ServerPlayerEntity p : new ArrayList(w.getPlayers())) {
         ServerWorld ow = server.getOverworld();
         BlockPos sp = ow.getSpawnPos();
         p.teleport(ow, (double)sp.getX() + 0.5, (double)sp.getY(), (double)sp.getZ() + 0.5, p.getYaw(), p.getPitch());
      }

      for (long c : new ArrayList(w.getForcedChunks())) {
         w.setChunkForced(ChunkPos.getPackedX(c), ChunkPos.getPackedZ(c), false);
      }
   }

   private static void tickDeletions(MinecraftServer server) {
      if (!deleting.isEmpty()) {
         long now = server.getOverworld().getTime();
         Iterator it = deleting.entrySet().iterator();

         while (it.hasNext()) {
            Entry e = it.next();
            ServerWorld w = server.getWorld(e.getKey());
            if (w == null) {
               TurnBattleMod.LOGGER.info("[DarkFountain] Dimension {} fully removed", e.getKey().getValue());
               DarkWorldGenerator.onWorldRemoved(server, e.getKey());
               deletingInst.remove(e.getKey());
               it.remove();
            } else {
               long waited = now - e.getValue();
               if (waited > 0L && waited % 600L == 0L) {
                  TurnBattleMod.LOGGER
                     .warn(
                        "[DarkFountain] Dimension {} still loaded {}s after sealing ({} chunks) - releasing it again",
                        new Object[]{e.getKey().getValue(), waited / 20L, w.getChunkManager().getLoadedChunkCount()}
                     );
                  releaseChunks(server, w);
                  DarkFountainData.DarkWorldInstance di = deletingInst.get(e.getKey());
                  if (di != null) {
                     try {
                        DarkWorldGenerator.removeDimension(server, di);
                     } catch (Exception var10) {
                     }
                  }
               }
            }
         }
      }
   }

   public static void onServerStopped() {
      scheduled.clear();
      deleting.clear();
      deletingInst.clear();
      pending.clear();
      entryHold.clear();
      entryCooldowns.clear();
      doorCache.clear();
      spawnCache.clear();
   }

   public static int pendingDeletions() {
      return deleting.size();
   }

   private static PacketByteBuf buildSealStartPacket(MinecraftServer server, List<ServerPlayerEntity> insidePlayers, DarkFountainData.DarkWorldInstance inst) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(insidePlayers.size());
      double fcx = (double)inst.fountainX + 0.5;
      double fcz = (double)inst.fountainZ + 0.5;
      Vec3d first = insidePlayers.isEmpty() ? new Vec3d(fcx + 5.0, 64.0, fcz) : insidePlayers.get(0).getPos();
      double dx = first.x - fcx;
      double dz = first.z - fcz;
      double d = Math.sqrt(dx * dx + dz * dz);
      double ang0 = d < 0.001 ? 0.0 : Math.atan2(dz, dx);
      double radius = Math.max(4.8, d);
      double y0 = first.y + 2.7;
      double step = 2.0 * Math.asin(Math.min(1.0, 0.55 / radius));

      for (int i = 0; i < insidePlayers.size(); i++) {
         ServerPlayerEntity p = insidePlayers.get(i);
         BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(p.getUuid());
         int k = (i + 1) / 2;
         double ang = ang0 + (double)(i % 2 == 1 ? k : -k) * step;
         buf.writeUuid(p.getUuid());
         buf.writeInt(prog.soulType != null ? prog.soulType.ordinal() : -1);
         buf.writeDouble(fcx + Math.cos(ang) * radius);
         buf.writeDouble(y0 - (i == 0 ? 0.0 : 0.3));
         buf.writeDouble(fcz + Math.sin(ang) * radius);
      }

      buf.writeDouble(fcx);
      buf.writeDouble(fcz);
      return buf;
   }

   public static void onSealPromptRequest(ServerPlayerEntity player) {
      if (isDarkWorld(player.getWorld().getRegistryKey())) {
         if (!DarknerBosses.blocksSeal(player)) {
            PacketByteBuf buf = PacketByteBufs.create();
            Net121.sendToClient(player, TurnBattleMod.DARK_FOUNTAIN_SEAL_PROMPT, buf);
         }
      }
   }

   public static void onSealConfirm(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      DarkFountainData data = DarkFountainData.get(server);
      DarkFountainData.DarkWorldInstance inst = data.getInstanceByWorldKey(player.getWorld().getRegistryKey());
      if (inst != null) {
         if (!DarknerBosses.blocksSeal(player)) {
            collapseDarkWorld(server, inst, data, player);
         }
      }
   }

   private static void fillRoomWithDarkBlocks(ServerWorld world, Collection<BlockPos> airBlocks, DarkFountainData.DarkWorldInstance inst) {
      for (BlockPos p : airBlocks) {
         if (world.getBlockState(p).isAir()) {
            world.setBlockState(p, TurnBattleMod.DARK_BLOCK.getDefaultState(), 18);
            inst.darkBlockPositions.add(p.asLong());
         }
      }
   }

   public static DarkFountainManager.EnclosureResult checkEnclosure(ServerWorld world, BlockPos startPos) {
      Set visited = new LinkedHashSet<>();
      Queue queue = new ArrayDeque<>();
      BlockPos start = world.getBlockState(startPos).isAir() ? startPos : startPos.up();
      if (!world.getBlockState(start).isAir()) {
         return new DarkFountainManager.EnclosureResult(false, startPos, Collections.emptySet());
      } else {
         visited.add(start);
         queue.add(start);
         int minY = world.getBottomY();
         int maxY = world.getTopY();

         while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (cur.getY() <= minY || cur.getY() >= maxY - 1) {
               return new DarkFountainManager.EnclosureResult(false, startPos, visited);
            }

            if (visited.size() >= 1500) {
               return new DarkFountainManager.EnclosureResult(false, startPos, visited);
            }

            for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
               BlockPos nb = cur.add(d[0], d[1], d[2]);
               if (!visited.contains(nb) && world.getBlockState(nb).isAir()) {
                  visited.add(nb);
                  queue.add(nb);
               }
            }
         }

         long sx = 0L;
         long sy = 0L;
         long sz = 0L;

         for (BlockPos p : visited) {
            sx += (long)p.getX();
            sy += (long)p.getY();
            sz += (long)p.getZ();
         }

         int n = Math.max(1, visited.size());
         BlockPos centre = new BlockPos((int)(sx / (long)n), (int)(sy / (long)n), (int)(sz / (long)n));
         return new DarkFountainManager.EnclosureResult(true, centre, Collections.unmodifiableSet(visited));
      }
   }

   private static void placeFountainPillar(ServerWorld world, DarkFountainData.DarkWorldInstance inst) {
      int fx = inst.fountainX;
      int fz = inst.fountainZ;
      int radius = 3;
      int bottom = world.getBottomY();
      int top = world.getTopY();

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz <= radius * radius) {
               for (int y = bottom; y < top; y++) {
                  world.setBlockState(new BlockPos(fx + dx, y, fz + dz), TurnBattleMod.DARK_FOUNTAIN_BLOCK.getDefaultState(), 18);
               }
            }
         }
      }

      TurnBattleMod.LOGGER.info("[DarkFountain] Placed Fountain pillar at ({},{}..{},{})", new Object[]{fx, bottom, top, fz});
   }

   private static void spawnPillarParticles(ServerWorld world, BlockPos pillar) {
      double cx = (double)pillar.getX() + 0.5;
      double cy = (double)pillar.getY();
      double cz = (double)pillar.getZ() + 0.5;
      int topY = Math.min(world.getTopY(), 320);

      for (int y = (int)cy; y < topY; y += 2) {
         world.spawnParticles(BLACK_DUST, cx, (double)y, cz, 6, 2.5, 0.0, 2.5, 0.08);
      }
   }

   private static BlockPos findExitPos(ServerWorld world, BlockPos roomCenter) {
      for (int dist = 4; dist <= 25; dist++) {
         for (int step = 0; step < 16; step++) {
            double ang = (double)step * Math.PI / 8.0;
            int ex = roomCenter.getX() + (int)((double)dist * Math.cos(ang));
            int ez = roomCenter.getZ() + (int)((double)dist * Math.sin(ang));

            for (int ey = roomCenter.getY() + 6; ey >= roomCenter.getY() - 6; ey--) {
               BlockPos p = new BlockPos(ex, ey, ez);
               if (isSafe(world, p)) {
                  return p;
               }
            }
         }
      }

      return roomCenter;
   }

   private static boolean isSafe(ServerWorld world, BlockPos p) {
      return world.getBlockState(p).isAir()
         && world.getBlockState(p.up()).isAir()
         && !world.getBlockState(p.down()).isAir()
         && !world.getBlockState(p.down()).isOf(TurnBattleMod.DARK_FOUNTAIN_BLOCK)
         && !world.getBlockState(p.down()).isOf(TurnBattleMod.DARK_BLOCK);
   }

   private static int findSafeY(ServerWorld world, int x, int z) {
      for (int y = 100; y > 5; y--) {
         BlockPos p = new BlockPos(x, y, z);
         if (world.getBlockState(p).isAir() && world.getBlockState(p.up()).isAir() && !world.getBlockState(p.down()).isAir()) {
            return y;
         }
      }

      return 65;
   }

   private static BlockPos spawnFor(ServerWorld world, DarkFountainData.DarkWorldInstance inst) {
      BlockPos cached = spawnCache.get(inst.id);
      if (cached != null) {
         BlockState feet = world.getBlockState(cached);
         BlockState head = world.getBlockState(cached.up());
         BlockState ground = world.getBlockState(cached.down());
         if (feet.getCollisionShape(world, cached).isEmpty()
            && head.getCollisionShape(world, cached.up()).isEmpty()
            && feet.getFluidState().isEmpty()
            && !ground.getCollisionShape(world, cached.down()).isEmpty()) {
            return cached;
         }
      }

      BlockPos found = findFountainSpawnPos(world, inst);
      spawnCache.put(inst.id, found);
      return found;
   }

   private static void slow(String what, long startNanos) {
      long ms = (System.nanoTime() - startNanos) / 1000000L;
      if (ms >= 25L) {
         long now = System.currentTimeMillis();
         if (now - lastSlowLog >= 10000L) {
            lastSlowLog = now;
            TurnBattleMod.LOGGER.warn("[DarkFountain] {} took {} ms", what, ms);
         }
      }
   }

   private static BlockPos findFountainSpawnPos(ServerWorld world, DarkFountainData.DarkWorldInstance inst) {
      int x = 50;
      int z = 0;
      if (DarkWorldGenerator.isSkyIslands(inst)) {
         return buildGuaranteedSpawnPlatform(world, x, z);
      } else {
         boolean roofed = DarkWorldGenerator.isNetherShaped(inst);

         for (int pass = 0; pass < 2; pass++) {
            for (int r = 0; r <= 64; r += 4) {
               int steps = r == 0 ? 1 : Math.max(8, r / 2);

               for (int i = 0; i < steps; i++) {
                  double ang = (double)i * Math.PI * 2.0 / (double)steps;
                  int cx = x + (int)Math.round(Math.cos(ang) * (double)r);
                  int cz = z + (int)Math.round(Math.sin(ang) * (double)r);
                  if (Math.abs(cx - inst.fountainX) > 5 || Math.abs(cz - inst.fountainZ) > 5) {
                     BlockPos p = roofed ? caveFloorAt(world, cx, cz) : surfaceAt(world, cx, cz);
                     if (p != null) {
                        return p;
                     }
                  }
               }
            }

            if (roofed) {
               break;
            }

            roofed = true;
         }

         int y = findSafeY(world, x, z);
         return new BlockPos(x, y, z);
      }
   }

   private static BlockPos caveFloorAt(ServerWorld world, int x, int z) {
      world.getChunk(x >> 4, z >> 4);
      int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
      int bottom = world.getBottomY() + 2;
      if (top <= bottom) {
         return null;
      } else {
         boolean underRoof = false;
         int air = 0;
         Mutable p = new Mutable(x, top, z);

         for (int y = top; y > bottom; y--) {
            p.setY(y);
            BlockState st = world.getBlockState(p);
            boolean open = st.getFluidState().isEmpty() && st.getCollisionShape(world, p).isEmpty();
            if (!underRoof) {
               if (!open) {
                  underRoof = true;
               }
            } else if (open) {
               air++;
            } else {
               if (air >= 3
                  && st.getFluidState().isEmpty()
                  && !st.isOf(Blocks.BEDROCK)
                  && !st.isOf(Blocks.MAGMA_BLOCK)
                  && !st.isOf(Blocks.CACTUS)
                  && !st.isOf(TurnBattleMod.DARK_FOUNTAIN_BLOCK)
                  && !st.isOf(TurnBattleMod.DARK_BLOCK)
                  && !nextToLava(world, p.up())) {
                  return new BlockPos(x, y + 1, z);
               }

               if (!st.getFluidState().isEmpty()) {
                  return null;
               }

               air = 0;
            }
         }

         return null;
      }
   }

   private static boolean nextToLava(ServerWorld world, BlockPos feet) {
      for (BlockPos q : new BlockPos[]{
         feet.north(), feet.south(), feet.east(), feet.west(), feet.down().north(), feet.down().south(), feet.down().east(), feet.down().west()
      }) {
         if (world.getBlockState(q).getFluidState().isIn(FluidTags.LAVA)) {
            return true;
         }
      }

      return false;
   }

   private static BlockPos surfaceAt(ServerWorld world, int x, int z) {
      world.getChunk(x >> 4, z >> 4);
      int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
      if (top > world.getBottomY() + 1 && top < world.getTopY() - 2) {
         BlockPos feet = new BlockPos(x, top, z);
         BlockPos below = feet.down();
         BlockState ground = world.getBlockState(below);
         if (!ground.getFluidState().isEmpty()
            || ground.isOf(Blocks.BEDROCK)
            || ground.isOf(TurnBattleMod.DARK_FOUNTAIN_BLOCK)
            || ground.isOf(TurnBattleMod.DARK_BLOCK)
            || ground.isOf(Blocks.MAGMA_BLOCK)
            || ground.isOf(Blocks.CACTUS)) {
            return null;
         } else if (ground.getCollisionShape(world, below).isEmpty()) {
            return null;
         } else {
            if (top > world.getBottomY() + 12) {
               for (int d = 2; d <= 8; d++) {
                  if (world.getBlockState(feet.down(d)).isOf(Blocks.BEDROCK)) {
                     return null;
                  }
               }
            }

            for (BlockPos p : new BlockPos[]{feet, feet.up()}) {
               BlockState st = world.getBlockState(p);
               if (!st.getFluidState().isEmpty() || !st.getCollisionShape(world, p).isEmpty()) {
                  return null;
               }
            }

            return feet;
         }
      } else {
         return null;
      }
   }

   private static BlockPos buildGuaranteedSpawnPlatform(ServerWorld world, int x, int z) {
      int y = 40;
      BlockPos platformTop = new BlockPos(x, y - 1, z);

      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            world.setBlockState(platformTop.add(dx, 0, dz), Blocks.STONE.getDefaultState(), 18);
         }
      }

      for (int px = x + 3; px <= 59; px++) {
         for (int dz = -3; dz <= 3; dz++) {
            BlockPos at = new BlockPos(px, y - 1, z + dz);
            if (world.getBlockState(at).isAir()) {
               world.setBlockState(at, Blocks.STONE.getDefaultState(), 18);
            }
         }
      }

      for (int dx = -1; dx <= 1; dx++) {
         for (int dzx = -1; dzx <= 1; dzx++) {
            for (int dy = 0; dy < 3; dy++) {
               world.setBlockState(new BlockPos(x + dx, y + dy, z + dzx), Blocks.AIR.getDefaultState(), 18);
            }
         }
      }

      return new BlockPos(x, y, z);
   }

   private static int blockDist(BlockPos a, BlockPos b) {
      return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
   }

   private static double sq(double v) {
      return v * v;
   }

   private static void scheduleAt(MinecraftServer server, int delayTicks, Runnable task) {
      long at = server.getOverworld().getTime() + (long)delayTicks;
      scheduled.add(new DarkFountainManager.ScheduledTask(at, task));
   }

   public static record EnclosureResult(boolean enclosed, BlockPos center, Set<BlockPos> airBlocks) {
   }

   public static record PendingFountain(BlockPos roomCenter, RegistryKey<World> dimension, Set<BlockPos> airBlocks) {
   }

   private static record ScheduledTask(long at, Runnable task) {
   }
}
