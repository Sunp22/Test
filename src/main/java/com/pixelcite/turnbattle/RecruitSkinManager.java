package com.pixelcite.turnbattle;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

@Environment(EnvType.CLIENT)
public class RecruitSkinManager {
   private static final String SKIN_DIR = "soulbound-recruit-skins";
   private static final Identifier STEVE = Identifier.of("turnbattle", "textures/entity/steve.png");
   public static final Identifier ORI_SKIN = Identifier.of("turnbattle", "textures/entity/ori_skin.png");
   private static final String PATHS_FILE = "recruit_skin_paths.json";
   private static final Map<String, Identifier> CACHE = new HashMap<>();
   private static Map<String, String> explicitPaths = null;
   public static final AtomicBoolean PICKER_OPEN = new AtomicBoolean();

   private static Path pathsFile() {
      return skinDir().resolve("recruit_skin_paths.json");
   }

   private static Map<String, String> loadExplicitPaths() {
      Path file = pathsFile();
      if (!Files.exists(file)) {
         return new HashMap<>();
      } else {
         try {
            String json = Files.readString(file);
            Map result = new HashMap<>();
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            for (String key : obj.keySet()) {
               result.put(key, obj.get(key).getAsString());
            }

            return result;
         } catch (Exception var6) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Failed to load recruit skin paths: {}", var6.getMessage());
            return new HashMap<>();
         }
      }
   }

   private static void saveExplicitPaths() {
      if (explicitPaths != null) {
         try {
            JsonObject obj = new JsonObject();

            for (Entry e : explicitPaths.entrySet()) {
               obj.addProperty(e.getKey(), e.getValue());
            }

            Files.createDirectories(skinDir());
            Files.writeString(pathsFile(), new GsonBuilder().setPrettyPrinting().create().toJson(obj));
         } catch (IOException var3) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Failed to save recruit skin paths: {}", var3.getMessage());
         }
      }
   }

   private static Map<String, String> explicitPaths() {
      if (explicitPaths == null) {
         explicitPaths = loadExplicitPaths();
      }

      return explicitPaths;
   }

   private static Path skinDir() {
      return FabricLoader.getInstance().getConfigDir().resolve("soulbound-recruit-skins");
   }

   public static Identifier getSkin(String recruitName) {
      if (recruitName == null || recruitName.isBlank()) {
         return STEVE;
      } else if (OriRender.isOri(recruitName)) {
         return ORI_SKIN;
      } else {
         String key = recruitName.toLowerCase(Locale.ROOT).trim();
         return CACHE.computeIfAbsent(key, RecruitSkinManager::load);
      }
   }

   public static boolean hasExplicitPath(String recruitName) {
      if (recruitName == null) {
         return false;
      } else {
         String key = recruitName.toLowerCase(Locale.ROOT).trim();
         return explicitPaths().containsKey(key);
      }
   }

   public static void pickSkinFile(String recruitName, BiConsumer<Boolean, String> onComplete) {
      String key = recruitName.toLowerCase(Locale.ROOT).trim();
      if (OriRender.isOri(recruitName)) {
         MinecraftClient mc0 = MinecraftClient.getInstance();
         if (mc0 != null) {
            mc0.execute(() -> onComplete.accept(false, "Ori's appearance can't be changed."));
         }
      } else if (PICKER_OPEN.compareAndSet(false, true)) {
         new Thread(() -> {
            boolean success = false;
            String errorMsg = null;

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  PointerBuffer filters = stack.mallocPointer(1);
                  filters.put(stack.UTF8("*.png"));
                  filters.flip();
                  String chosen = TinyFileDialogs.tinyfd_openFileDialog("Choose a skin for " + recruitName, "", filters, "PNG images", false);
                  if (chosen != null) {
                     explicitPaths().put(key, chosen);
                     saveExplicitPaths();
                     CACHE.remove(key);
                     success = true;
                  }
               } catch (Throwable var17) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Exception var18) {
               errorMsg = var18.getClass().getSimpleName() + ": " + var18.getMessage();
               TurnBattleMod.LOGGER.warn("[TurnBattle] File picker failed for '{}': {}", recruitName, var18.getMessage());
            } finally {
               PICKER_OPEN.set(false);
               boolean finalSuccess = success;
               String finalError = errorMsg;
               MinecraftClient mc = MinecraftClient.getInstance();
               if (mc != null) {
                  mc.execute(() -> onComplete.accept(finalSuccess, finalError));
               }
            }
         }, "turnbattle-skin-picker").start();
      }
   }

   private static Identifier load(String key) {
      String explicit = explicitPaths().get(key);
      if (explicit != null) {
         Path file = Path.of(explicit);
         return loadFromPath(file, key);
      } else {
         Path dir = skinDir();
         String safeFileName = sanitizeFileName(key);
         Path file = dir.resolve(safeFileName + ".png");
         return loadFromPath(file, key);
      }
   }

   private static Identifier loadFromPath(Path file, String key) {
      if (!Files.exists(file)) {
         return STEVE;
      } else {
         try {
            Identifier var6;
            try (InputStream is = Files.newInputStream(file)) {
               NativeImage img = NativeImage.read(is);
               NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
               Identifier id = Identifier.of("turnbattle", "recruit_skin/" + sanitizeFileName(key));
               MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
               var6 = id;
            }

            return var6;
         } catch (IOException var9) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Failed to load recruit skin '{}': {}", key, var9.getMessage());
            return STEVE;
         } catch (InvalidPathException var10) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Invalid skin file name for recruit '{}': {}", key, var10.getMessage());
            return STEVE;
         } catch (Exception var11) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Unexpected error loading recruit skin '{}': {}", key, var11.getMessage());
            return STEVE;
         }
      }
   }

   private static String sanitizeFileName(String name) {
      String cleaned = name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");
      cleaned = cleaned.replaceAll("[. ]+$", "");
      if (cleaned.isBlank()) {
         cleaned = "recruit";
      }

      return cleaned;
   }

   public static void clearCache() {
      MinecraftClient mc = MinecraftClient.getInstance();

      for (Entry e : CACHE.entrySet()) {
         if (!e.getValue().equals(STEVE)) {
            mc.getTextureManager().destroyTexture(e.getValue());
         }
      }

      CACHE.clear();
   }

   public static void ensureDir() {
      Path dir = skinDir();
      if (!Files.exists(dir)) {
         try {
            Files.createDirectories(dir);
         } catch (IOException var2) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Could not create soulbound-recruit-skins dir: {}", var2.getMessage());
         }
      }
   }
}
