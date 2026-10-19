package com.pixelcite.turnbattle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler.NoiseParameters;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialCondition;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;

public class DarkWorldGenerator {
   private static final int NATIVE_OVERWORLD = 0;
   private static final int NATIVE_NETHER = 1;
   private static final int NATIVE_END = 2;
   // Infiniverse (unlike Fantasy) has no per-world seed, so remember each Dark World's seed here
   // and let DarkWorldSeedMixin hand it back from ServerWorld#getSeed.
   private static final Map<Identifier, Long> WORLD_SEEDS = new ConcurrentHashMap<>();
   private static final TagKey<Biome> IS_NETHER = TagKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "is_nether"));
   private static final TagKey<Biome> IS_END = TagKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "is_end"));

   public static Long seedFor(RegistryKey<World> key) {
      return WORLD_SEEDS.get(key.getValue());
   }

   public static boolean isNetherShaped(DarkFountainData.DarkWorldInstance inst) {
      return !inst.biomeGroups.isEmpty()
         ? "minecraft:the_nether".equals(inst.shapeId)
         : biomeForBlock(inst.baseBlockId).equals(Identifier.of("minecraft", "crimson_forest"));
   }

   public static boolean isSkyIslands(DarkFountainData.DarkWorldInstance inst) {
      return !inst.biomeGroups.isEmpty()
         ? "turnbattle:sky_islands".equals(inst.shapeId)
         : biomeForBlock(inst.baseBlockId).equals(Identifier.of("turnbattle", "dw_sky_islands"));
   }

   private static Identifier biomeForBlock(String blockId) {
      String id = blockId.toLowerCase();
      if (id.contains("glass")) {
         return Identifier.of("turnbattle", "dw_glass_mountains");
      } else if (id.contains("wool") || id.contains("carpet")) {
         String color = "white";

         for (String c : new String[]{
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
         }) {
            if (id.contains(c)) {
               color = c;
               break;
            }
         }

         return Identifier.of("turnbattle", "dw_wool_fields_" + color);
      } else if (id.contains("iron_block")) {
         return Identifier.of("turnbattle", "dw_steel_caverns");
      } else if (id.contains("sculk")) {
         return Identifier.of("minecraft", "deep_dark");
      } else if (!id.contains("plank") && !id.contains("log") && !id.contains("wood") && !id.contains("leaves") && !id.contains("sapling")) {
         if (!id.contains("deepslate") && !id.contains("tuff") && !id.contains("cobblestone") && (!id.contains("stone") || id.contains("sandstone"))) {
            if (id.contains("sand")) {
               return Identifier.of("minecraft", "desert");
            } else if (id.contains("snow") || id.contains("ice") || id.contains("packed_ice")) {
               return Identifier.of("minecraft", "snowy_plains");
            } else if (id.contains("dirt") || id.contains("grass_block") || id.contains("podzol") || id.contains("mycelium") || id.contains("rooted_dirt")) {
               return Identifier.of("turnbattle", "dw_sky_islands");
            } else if (id.contains("mud") || id.contains("clay") || id.contains("mangrove")) {
               return Identifier.of("minecraft", "swamp");
            } else if (id.contains("netherrack") || id.contains("crimson") || id.contains("warped") || id.contains("basalt") || id.contains("blackstone")) {
               return Identifier.of("minecraft", "crimson_forest");
            } else {
               return !id.contains("end_stone") && !id.contains("chorus") && !id.contains("purpur")
                  ? Identifier.of("minecraft", "forest")
                  : Identifier.of("minecraft", "the_end");
            }
         } else {
            return Identifier.of("minecraft", "dripstone_caves");
         }
      } else {
         return Identifier.of("turnbattle", "dw_wooden_playground");
      }
   }

   private static RegistryKey<ChunkGeneratorSettings> chunkGeneratorSettingsKeyFor(Identifier biomeId) {
      if (biomeId.equals(Identifier.of("minecraft", "crimson_forest"))) {
         return ChunkGeneratorSettings.NETHER;
      } else if (biomeId.equals(Identifier.of("minecraft", "the_end"))) {
         return ChunkGeneratorSettings.END;
      } else {
         return biomeId.equals(Identifier.of("turnbattle", "dw_sky_islands")) ? ChunkGeneratorSettings.FLOATING_ISLANDS : ChunkGeneratorSettings.OVERWORLD;
      }
   }

   private static Identifier dimensionTypeIdFor(Identifier biomeId) {
      return biomeId.equals(Identifier.of("turnbattle", "dw_sky_islands"))
         ? Identifier.of("turnbattle", "dark_world_sky")
         : Identifier.of("turnbattle", "dark_world");
   }

   public static void createDimension(MinecraftServer server, DarkFountainData.DarkWorldInstance inst) {
      DynamicRegistryManager registryManager = server.getRegistryManager();
      Registry biomeRegistry = registryManager.get(RegistryKeys.BIOME);
      Registry settingsRegistry = registryManager.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
      Set present = new LinkedHashSet<>();
      Object biomeSource;
      ChunkGeneratorSettings vanillaSettings;
      int native_;
      boolean isSkyIslands;
      Identifier dimTypeId;
      String describe;
      if (inst.biomeGroups.isEmpty()) {
         Identifier biomeId = biomeForBlock(inst.baseBlockId);
         RegistryEntry biomeEntry = biomeRegistry.getEntry(RegistryKey.of(RegistryKeys.BIOME, biomeId))
            .map(e -> (RegistryEntry<Biome>)e)
            .orElseGet(
               () -> (RegistryEntry<Biome>)biomeRegistry.getEntry(RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "forest"))).orElseThrow()
            );
         RegistryKey settingsKey = chunkGeneratorSettingsKeyFor(biomeId);
         vanillaSettings = (ChunkGeneratorSettings)((Reference)settingsRegistry.getEntry(settingsKey).orElseThrow()).value();
         native_ = settingsKey.equals(ChunkGeneratorSettings.NETHER) ? 1 : (settingsKey.equals(ChunkGeneratorSettings.END) ? 2 : 0);
         isSkyIslands = settingsKey.equals(ChunkGeneratorSettings.FLOATING_ISLANDS);
         dimTypeId = dimensionTypeIdFor(biomeId);
         biomeSource = new FixedBiomeSource(biomeEntry);
         present.add(biomeEntry);
         describe = "biome " + biomeId + " from block " + inst.baseBlockId;
      } else {
         String shape = inst.shapeId != null && !inst.shapeId.isEmpty() ? inst.shapeId : "minecraft:overworld";

         RegistryKey settingsKey = switch (shape) {
            case "minecraft:the_nether" -> ChunkGeneratorSettings.NETHER;
            case "minecraft:the_end" -> ChunkGeneratorSettings.END;
            case "turnbattle:sky_islands" -> ChunkGeneratorSettings.FLOATING_ISLANDS;
            case "minecraft:overworld" -> ChunkGeneratorSettings.OVERWORLD;
            default -> null;
         };
         ChunkGeneratorSettings fromWorld = settingsKey == null ? settingsOfWorld(server, shape) : null;
         if (settingsKey == null && fromWorld == null) {
            settingsKey = ChunkGeneratorSettings.OVERWORLD;
         }

         vanillaSettings = fromWorld != null ? fromWorld : (ChunkGeneratorSettings)((Reference)settingsRegistry.getEntry(settingsKey).orElseThrow()).value();
         native_ = ChunkGeneratorSettings.NETHER.equals(settingsKey) ? 1 : (ChunkGeneratorSettings.END.equals(settingsKey) ? 2 : 0);
         isSkyIslands = ChunkGeneratorSettings.FLOATING_ISLANDS.equals(settingsKey);
         dimTypeId = isSkyIslands ? Identifier.of("turnbattle", "dark_world_sky") : Identifier.of("turnbattle", "dark_world");
         List groups = new ArrayList<>();
         List accepted = new ArrayList<>();

         for (String g : inst.biomeGroups) {
            List members = new ArrayList<>();

            for (String id : DarkWorldCatalog.splitGroup(g)) {
               Identifier bid = Identifier.tryParse(id);
               if (bid != null) {
                  Optional entry = biomeRegistry.getEntry(RegistryKey.of(RegistryKeys.BIOME, bid));
                  if (!entry.isEmpty()) {
                     RegistryEntry e = (RegistryEntry<Biome>)entry.get();
                     if (!accepted.contains(e)) {
                        if (!featuresCompatible(accepted, e)) {
                           TurnBattleMod.LOGGER
                              .warn(
                                 "[DarkFountain] Leaving biome {} out of {}: its features can't generate alongside the others",
                                 id,
                                 inst.getWorldKey().getValue()
                              );
                           continue;
                        }

                        accepted.add(e);
                     }

                     if (!members.contains(e)) {
                        members.add(e);
                     }
                  }
               }
            }

            if (!members.isEmpty()) {
               groups.add(members);
            }
         }

         if (groups.isEmpty()) {
            groups.add(
               List.of(
                  (RegistryEntry<Biome>)biomeRegistry.getEntry(RegistryKey.of(RegistryKeys.BIOME, Identifier.of("turnbattle", "dw_sky_islands"))).orElseThrow()
               )
            );
         }

         List caves = new ArrayList<>();
         int caveBelow = Integer.MIN_VALUE;
         if (ChunkGeneratorSettings.OVERWORLD.equals(settingsKey)) {
            List surface = new ArrayList<>();

            for (List grp : groups) {
               boolean allCave = grp.stream().allMatch(b -> b.getKey().map(k -> DarkWorldCatalog.isCaveBiome(k.getValue().toString())).orElse(false));
               if (allCave) {
                  caves.addAll(grp);
               } else {
                  surface.add(grp);
               }
            }

            if (!surface.isEmpty() && !caves.isEmpty()) {
               groups = surface;
               caveBelow = 0;
            } else {
               caves.clear();
            }
         }

         for (List grpx : groups) {
            present.addAll(grpx);
         }

         present.addAll(caves);
         biomeSource = new DarkWorldBiomeSource(groups, caves, inst.seed ^ 2685821657736338717L, caveBelow);
         describe = "terrain " + shape + ", " + present.size() + " biome(s)";
      }

      MaterialRule newSurfaceRule = buildSurfaceRule(settingsRegistry, vanillaSettings, native_, present);
      ChunkGeneratorSettings customSettings = new ChunkGeneratorSettings(
         vanillaSettings.generationShapeConfig(),
         vanillaSettings.defaultBlock(),
         isSkyIslands ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(),
         vanillaSettings.noiseRouter(),
         newSurfaceRule,
         vanillaSettings.spawnTarget(),
         isSkyIslands ? 24 : vanillaSettings.seaLevel(),
         vanillaSettings.mobGenerationDisabled(),
         false,
         false,
         vanillaSettings.usesLegacyRandom()
      );
      RegistryEntry settingsEntry = RegistryEntry.of(customSettings);
      NoiseChunkGenerator generator = new NoiseChunkGenerator(biomeSource, settingsEntry);
      Registry dimTypeRegistry = registryManager.get(RegistryKeys.DIMENSION_TYPE);
      RegistryEntry dimType = (RegistryEntry<DimensionType>)dimTypeRegistry.getEntry(RegistryKey.of(RegistryKeys.DIMENSION_TYPE, dimTypeId)).orElseThrow();
      // Keep-inventory is handled by DarkWorldKeepInventoryMixin and natural regeneration is forced off
      // globally in TurnBattleMod, so the per-world game rules Fantasy offered are no longer needed.
      DimensionOptions options = new DimensionOptions((RegistryEntry<DimensionType>)dimType, generator);
      RegistryKey<World> worldKey = inst.getWorldKey();
      WORLD_SEEDS.put(worldKey.getValue(), inst.seed);

      try {
         InfiniverseBridge.getOrCreateWorld(server, worldKey, () -> options);
      } catch (RuntimeException ex) {
         WORLD_SEEDS.remove(worldKey.getValue());
         throw ex;
      }

      TurnBattleMod.LOGGER.info("[DarkFountain] Opened dimension {} ({}, seed {})", new Object[]{worldKey.getValue(), describe, inst.seed});
   }

   public static boolean canLendTerrain(MinecraftServer server, String dimId) {
      return switch (dimId) {
         case "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "turnbattle:sky_islands" -> true;
         default -> settingsOfWorld(server, dimId) != null;
      };
   }

   private static ChunkGeneratorSettings settingsOfWorld(MinecraftServer server, String dimId) {
      Identifier id = Identifier.tryParse(dimId);
      if (id != null && !id.getNamespace().equals("turnbattle")) {
         ServerWorld w = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
         if (w == null) {
            return null;
         } else {
            return w.getChunkManager().getChunkGenerator() instanceof NoiseChunkGenerator ncg ? (ChunkGeneratorSettings)ncg.getSettings().value() : null;
         }
      } else {
         return null;
      }
   }

   private static boolean featuresCompatible(List<RegistryEntry<Biome>> accepted, RegistryEntry<Biome> candidate) {
      List trial = new ArrayList<>(accepted);
      trial.add(candidate);

      try {
         PlacedFeatureIndexer.collectIndexedFeatures(trial, b -> ((Biome)b.value()).getGenerationSettings().getFeatures(), false);
         return true;
      } catch (IllegalStateException var4) {
         return false;
      }
   }

   private static MaterialRule buildSurfaceRule(
      Registry<ChunkGeneratorSettings> settingsRegistry, ChunkGeneratorSettings vanillaSettings, int native_, Set<RegistryEntry<Biome>> present
   ) {
      List rules = new ArrayList<>();
      rules.add(MaterialRules.condition(MaterialRules.verticalGradient("bedrock_floor", YOffset.getBottom(), YOffset.aboveBottom(5)), block(Blocks.BEDROCK)));
      rules.addAll(customBiomeRules());
      List nether = new ArrayList<>();
      List end = new ArrayList<>();
      List over = new ArrayList<>();

      for (RegistryEntry b : present) {
         Optional key = b.getKey();
         if (!key.isEmpty() && !key.get().getValue().getNamespace().equals("turnbattle")) {
            if (b.isIn(IS_NETHER)) {
               nether.add(key.get());
            } else if (b.isIn(IS_END)) {
               end.add(key.get());
            } else {
               over.add(key.get());
            }
         }
      }

      if (native_ != 1 && !nether.isEmpty()) {
         rules.add(
            MaterialRules.condition(
               MaterialRules.biome(nether.toArray(RegistryKey[]::new)),
               MaterialRules.sequence(
                  new MaterialRule[]{
                     ((ChunkGeneratorSettings)((Reference)settingsRegistry.getEntry(ChunkGeneratorSettings.NETHER).orElseThrow()).value()).surfaceRule(),
                     MaterialRules.block(Blocks.NETHERRACK.getDefaultState())
                  }
               )
            )
         );
      }

      if (native_ != 2 && !end.isEmpty()) {
         rules.add(MaterialRules.condition(MaterialRules.biome(end.toArray(RegistryKey[]::new)), MaterialRules.block(Blocks.END_STONE.getDefaultState())));
      }

      if (native_ != 0 && !over.isEmpty()) {
         rules.add(
            MaterialRules.condition(
               MaterialRules.biome(over.toArray(RegistryKey[]::new)),
               ((ChunkGeneratorSettings)((Reference)settingsRegistry.getEntry(ChunkGeneratorSettings.OVERWORLD).orElseThrow()).value()).surfaceRule()
            )
         );
      }

      rules.add(vanillaSettings.surfaceRule());
      return MaterialRules.sequence(rules.toArray(new MaterialRule[0]));
   }

   private static RegistryKey<NoiseParameters> noise(String id) {
      return RegistryKey.of(RegistryKeys.NOISE_PARAMETERS, Identifier.ofVanilla(id));
   }

   private static MaterialRule block(Block b) {
      return MaterialRules.block(b.getDefaultState());
   }

   private static List<MaterialRule> customBiomeRules() {
      List rules = new ArrayList<>();
      MaterialCondition floor = MaterialRules.STONE_DEPTH_FLOOR;
      MaterialCondition under = MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH;
      RegistryKey seamA = noise("patch");
      RegistryKey seamB = noise("powder_snow");

      for (String c : DarkWorldCatalog.WOOL_COLORS) {
         Block wool = (Block)Registries.BLOCK.get(Identifier.of("minecraft", c + "_wool"));
         String seamColor = c.equals("white") ? "light_gray" : "white";
         Block seam = (Block)Registries.BLOCK.get(Identifier.of("minecraft", seamColor + "_wool"));
         rules.add(
            MaterialRules.condition(
               MaterialRules.biome(new RegistryKey[]{biomeKey("dw_wool_fields_" + c)}),
               MaterialRules.sequence(
                  new MaterialRule[]{
                     MaterialRules.condition(
                        floor,
                        MaterialRules.sequence(
                           new MaterialRule[]{
                              MaterialRules.condition(MaterialRules.noiseThreshold(seamA, -0.055, 0.055), block(seam)),
                              MaterialRules.condition(MaterialRules.noiseThreshold(seamB, -0.04, 0.04), block(seam)),
                              block(wool)
                           }
                        )
                     ),
                     MaterialRules.condition(under, block(wool))
                  }
               )
            )
         );
      }

      rules.add(
         MaterialRules.condition(
            MaterialRules.biome(new RegistryKey[]{biomeKey("dw_glass_mountains")}),
            MaterialRules.sequence(
               new MaterialRule[]{
                  MaterialRules.condition(
                     floor,
                     MaterialRules.sequence(
                        new MaterialRule[]{
                           MaterialRules.condition(
                              MaterialRules.noiseThreshold(noise("powder_snow"), 0.32, Double.MAX_VALUE), block(Blocks.LIGHT_BLUE_STAINED_GLASS)
                           ),
                           MaterialRules.condition(MaterialRules.noiseThreshold(noise("packed_ice"), 0.38, Double.MAX_VALUE), block(Blocks.WHITE_STAINED_GLASS)),
                           block(Blocks.GLASS)
                        }
                     )
                  ),
                  MaterialRules.condition(under, block(Blocks.CALCITE))
               }
            )
         )
      );
      rules.add(
         MaterialRules.condition(
            MaterialRules.biome(new RegistryKey[]{biomeKey("dw_wooden_playground")}),
            MaterialRules.sequence(
               new MaterialRule[]{
                  MaterialRules.condition(
                     floor,
                     MaterialRules.sequence(
                        new MaterialRule[]{
                           MaterialRules.condition(MaterialRules.noiseThreshold(noise("patch"), 0.18, Double.MAX_VALUE), block(Blocks.BIRCH_PLANKS)),
                           MaterialRules.condition(MaterialRules.noiseThreshold(noise("patch"), -Double.MAX_VALUE, -0.22), block(Blocks.DARK_OAK_PLANKS)),
                           MaterialRules.condition(MaterialRules.noiseThreshold(noise("powder_snow"), 0.35, Double.MAX_VALUE), block(Blocks.OAK_PLANKS)),
                           block(Blocks.SPRUCE_PLANKS)
                        }
                     )
                  ),
                  MaterialRules.condition(under, block(Blocks.STRIPPED_SPRUCE_WOOD))
               }
            )
         )
      );
      rules.add(
         MaterialRules.condition(
            MaterialRules.biome(new RegistryKey[]{biomeKey("dw_sky_islands")}),
            MaterialRules.sequence(
               new MaterialRule[]{
                  MaterialRules.condition(
                     floor,
                     MaterialRules.sequence(
                        new MaterialRule[]{
                           MaterialRules.condition(MaterialRules.noiseThreshold(noise("patch"), 0.3, Double.MAX_VALUE), block(Blocks.MOSS_BLOCK)),
                           block(Blocks.GRASS_BLOCK)
                        }
                     )
                  ),
                  MaterialRules.condition(under, block(Blocks.DIRT))
               }
            )
         )
      );
      rules.add(MaterialRules.condition(MaterialRules.biome(new RegistryKey[]{biomeKey("dw_steel_caverns")}), block(Blocks.IRON_BLOCK)));
      return rules;
   }

   private static RegistryKey<Biome> biomeKey(String path) {
      return RegistryKey.of(RegistryKeys.BIOME, Identifier.of("turnbattle", path));
   }

   public static void reopenAllOnStartup(MinecraftServer server) {
      DarkFountainData data = DarkFountainData.get(server);
      int count = 0;

      for (DarkFountainData.DarkWorldInstance inst : data.instances.values()) {
         try {
            createDimension(server, inst);
            count++;
         } catch (Exception var6) {
            TurnBattleMod.LOGGER.error("[DarkFountain] Failed to re-open dimension {} on startup", inst.getWorldKey().getValue(), var6);
         }
      }

      if (count > 0) {
         TurnBattleMod.LOGGER.info("[DarkFountain] Re-opened {} Dark World dimension(s) on startup", count);
      }

      sweepOrphanFolders(server, data);
   }

   private static void sweepOrphanFolders(MinecraftServer server, DarkFountainData data) {
      try {
         Path dir = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve("turnbattle");
         if (!Files.isDirectory(dir)) {
            return;
         }

         Set keep = new HashSet<>();

         for (DarkFountainData.DarkWorldInstance inst : data.instances.values()) {
            keep.add(inst.getWorldKey().getValue().getPath());
         }

         for (ServerWorld w : server.getWorlds()) {
            if (w.getRegistryKey().getValue().getNamespace().equals("turnbattle")) {
               keep.add(w.getRegistryKey().getValue().getPath());
            }
         }

         int removed = 0;

         try (Stream list = Files.list(dir)) {
            for (Path p : list::iterator) {
               String name = p.getFileName().toString();
               if (name.startsWith("dark_world_") && !keep.contains(name) && Files.isDirectory(p)) {
                  try {
                     try (Stream walk = Files.walk(p)) {
                        for (Path f : walk.sorted(Comparator.reverseOrder()).toList()) {
                           Files.deleteIfExists(f);
                        }
                     }

                     removed++;
                  } catch (IOException var15) {
                     TurnBattleMod.LOGGER.warn("[DarkFountain] Couldn't delete leftover Dark World folder {}: {}", name, var15.toString());
                  }
               }
            }
         }

         if (removed > 0) {
            TurnBattleMod.LOGGER.info("[DarkFountain] Deleted {} leftover folder(s) of sealed Dark Worlds", removed);
         }
      } catch (Throwable var17) {
         TurnBattleMod.LOGGER.warn("[DarkFountain] Leftover Dark World folder sweep skipped (non-fatal): " + var17);
      }
   }

   public static void removeDimension(MinecraftServer server, DarkFountainData.DarkWorldInstance inst) {
      RegistryKey worldKey = inst.getWorldKey();
      if (server.getWorld(worldKey) == null) {
         TurnBattleMod.LOGGER.info("[DarkFountain] Dimension {} was never opened this session - nothing to delete", worldKey.getValue());
      } else {
         InfiniverseBridge.unregisterWorld(server, worldKey);
         TurnBattleMod.LOGGER.info("[DarkFountain] Marked dimension {} for unregistration", worldKey.getValue());
      }
   }

   /**
    * Fantasy deleted a world's folder itself; Infiniverse only unloads it. Call this once the world is gone
    * from the server (see DarkFountainManager#tickDeletions). Anything missed is caught by sweepOrphanFolders.
    */
   public static void onWorldRemoved(MinecraftServer server, RegistryKey<World> key) {
      WORLD_SEEDS.remove(key.getValue());
      Identifier id = key.getValue();
      if (!id.getNamespace().equals("turnbattle") || !id.getPath().startsWith("dark_world_")) {
         return;
      }

      Path dir = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath());
      if (Files.isDirectory(dir)) {
         try (Stream<Path> walk = Files.walk(dir)) {
            for (Path f : walk.sorted(Comparator.reverseOrder()).toList()) {
               Files.deleteIfExists(f);
            }

            TurnBattleMod.LOGGER.info("[DarkFountain] Deleted folder of removed dimension {}", id);
         } catch (IOException var8) {
            TurnBattleMod.LOGGER.warn("[DarkFountain] Couldn't delete folder of {} yet ({}); it will be swept on next startup", id, var8.toString());
         }
      }
   }
}
