package com.pixelcite.turnbattle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public class DarkWorldLevelDatCleaner {
   private static final String DIM_PREFIX = "turnbattle:dark_world_";

   public static void tryCleanupDedicated() {
      try {
         Path worldDir = resolveDedicatedWorldDir();
         if (worldDir != null) {
            cleanup(worldDir);
         }
      } catch (Throwable var1) {
         TurnBattleMod.LOGGER.warn("[DarkFountain] level.dat cleanup skipped (non-fatal): " + var1);
      }
   }

   public static void tryCleanup(MinecraftServer server) {
      try {
         Path worldDir = server.getSavePath(WorldSavePath.ROOT);
         if (worldDir != null) {
            cleanup(worldDir);
         }
      } catch (Throwable var2) {
         TurnBattleMod.LOGGER.warn("[DarkFountain] level.dat cleanup skipped (non-fatal): " + var2);
      }
   }

   private static void cleanup(Path worldDir) throws IOException {
      Path levelDat = worldDir.resolve("level.dat");
      if (Files.isRegularFile(levelDat)) {
         Set validKeys = readValidDimensionKeys(worldDir);
         NbtCompound root = NbtIo.readCompressed(levelDat, NbtSizeTracker.ofUnlimitedBytes());
         if (root != null) {
            NbtCompound data = root.getCompound("Data");
            if (data.contains("WorldGenSettings")) {
               NbtCompound wgs = data.getCompound("WorldGenSettings");
               if (wgs.contains("dimensions")) {
                  NbtCompound dims = wgs.getCompound("dimensions");
                  int removed = 0;

                  for (String key : new HashSet(dims.getKeys())) {
                     if (key.startsWith("turnbattle:dark_world_") && !validKeys.contains(key)) {
                        dims.remove(key);
                        removed++;
                     }
                  }

                  if (removed != 0) {
                     Path backup = worldDir.resolve("level.dat.precleanup.bak");
                     if (!Files.exists(backup)) {
                        Files.copy(levelDat, backup);
                     }

                     NbtIo.writeCompressed(root, levelDat);
                     TurnBattleMod.LOGGER
                        .info(
                           "[DarkFountain] level.dat cleanup: removed "
                              + removed
                              + " abandoned Dark World dimension entr"
                              + (removed == 1 ? "y" : "ies")
                              + " (backup saved as level.dat.precleanup.bak)"
                        );
                  }
               }
            }
         }
      }
   }

   private static Set<String> readValidDimensionKeys(Path worldDir) {
      Set valid = new HashSet<>();

      try {
         Path dataFile = worldDir.resolve("data").resolve("dark_fountain_data.dat");
         if (!Files.isRegularFile(dataFile)) {
            return valid;
         }

         NbtCompound root = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
         if (root == null) {
            return valid;
         }

         NbtCompound data = root.contains("data") ? root.getCompound("data") : root;
         NbtList list = data.getList("instances", 10);

         for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            if (tag.contains("id") && tag.contains("creationToken")) {
               int id = tag.getInt("id");
               long token = tag.getLong("creationToken");
               valid.add("turnbattle:dark_world_" + id + "_" + Long.toHexString(token));
            }
         }
      } catch (Throwable var11) {
         TurnBattleMod.LOGGER
            .warn("[DarkFountain] Could not read dark_fountain_data.dat during cleanup (non-fatal, no entries will be treated as still-active): " + var11);
      }

      return valid;
   }

   private static Path resolveDedicatedWorldDir() {
      try {
         Path properties = Paths.get("server.properties");
         String levelName = "world";
         if (Files.isRegularFile(properties)) {
            Properties props = new Properties();

            try (InputStream in = Files.newInputStream(properties)) {
               props.load(in);
            }

            levelName = props.getProperty("level-name", "world").trim();
            if (levelName.isEmpty()) {
               levelName = "world";
            }
         }

         Path dir = Paths.get(levelName);
         return Files.isDirectory(dir) ? dir : null;
      } catch (Throwable var8) {
         TurnBattleMod.LOGGER.warn("[DarkFountain] Could not resolve world directory during cleanup (non-fatal): " + var8);
         return null;
      }
   }
}
