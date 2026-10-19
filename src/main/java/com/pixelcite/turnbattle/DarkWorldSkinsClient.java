package com.pixelcite.turnbattle;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.SkinTextures.Model;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

@Environment(EnvType.CLIENT)
public final class DarkWorldSkinsClient {
   private static final Map<UUID, DarkWorldSkinsClient.Entry> SKINS = new HashMap<>();
   private static int textureCounter = 0;
   public static final int FORCE_NONE = 0;
   public static final int FORCE_ORIGINAL = 1;
   public static final int FORCE_DARK = 2;
   static int force = 0;
   private static final Path PREF_FILE = Path.of("config", "turnbattle_dark_world_skin.txt");

   private DarkWorldSkinsClient() {
   }

   static void register() {
      Net121.registerClientReceiver(DarkWorldSkins.SKINS, (client, handler, buf, sender) -> {
         boolean full = buf.readBoolean();
         int n = buf.readVarInt();
         Map got = new HashMap<>();
         List removed = new ArrayList<>();

         for (int i = 0; i < n; i++) {
            UUID who = buf.readUuid();
            if (buf.readBoolean()) {
               boolean slim = buf.readBoolean();
               got.put(who, new DarkWorldSkins.Skin(buf.readByteArray(30000), slim));
            } else {
               removed.add(who);
            }
         }

         client.execute(() -> {
            if (full) {
               clearAll();
            }

            for (UUID r : removed) {
               drop(r);
            }

            got.forEach(DarkWorldSkinsClient::install);
            if (full && client.player != null && !SKINS.containsKey(client.player.getUuid())) {
               reupload();
            }
         });
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> client.execute(DarkWorldSkinsClient::clearAll));
   }

   private static void install(UUID who, DarkWorldSkins.Skin skin) {
      try {
         NativeImage img = NativeImage.read(Format.RGBA, new ByteArrayInputStream(skin.png()));
         makeBaseOpaque(img);
         Identifier id = Identifier.of("turnbattle", "dw_skin/" + who.toString().replace("-", "") + "_" + textureCounter++);
         MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
         drop(who);
         SKINS.put(who, new DarkWorldSkinsClient.Entry(id, skin.slim()));
      } catch (Exception var4) {
         TurnBattleMod.LOGGER.warn("[TurnBattle] Couldn't load a Dark World skin: {}", var4.getMessage());
      }
   }

   private static void makeBaseOpaque(NativeImage img) {
      int s = Math.max(1, img.getWidth() / 64);
      int[][] rects = new int[][]{{0, 0, 32, 16}, {0, 16, 64, 32}, {16, 48, 48, 64}};

      for (int[] r : rects) {
         for (int y = r[1] * s; y < r[3] * s && y < img.getHeight(); y++) {
            for (int x = r[0] * s; x < r[2] * s && x < img.getWidth(); x++) {
               img.setColor(x, y, img.getColor(x, y) | 0xFF000000);
            }
         }
      }
   }

   private static void drop(UUID who) {
      DarkWorldSkinsClient.Entry old = SKINS.remove(who);
      if (old != null) {
         MinecraftClient.getInstance().getTextureManager().destroyTexture(old.texture());
      }
   }

   private static void clearAll() {
      for (UUID u : new ArrayList<>(SKINS.keySet())) {
         drop(u);
      }
   }

   public static boolean has(UUID who) {
      return who != null && SKINS.containsKey(who);
   }

   public static boolean inDarkWorld() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.world != null && mc.world.getRegistryKey().getValue().toString().startsWith("turnbattle:dark_world_");
   }

   public static SkinTextures override(UUID who, SkinTextures original) {
      if (who != null && original != null) {
         MinecraftClient mc0 = MinecraftClient.getInstance();
         if (DepthsClient.inRun() && mc0.player != null && who.equals(mc0.player.getUuid())) {
            return new SkinTextures(RecruitSkinManager.ORI_SKIN, null, null, null, Model.WIDE, false);
         } else {
            DarkWorldSkinsClient.Entry e = SKINS.get(who);
            if (e == null) {
               return original;
            } else {
               boolean dark;
               if (force == 1) {
                  dark = false;
               } else if (force == 2) {
                  dark = true;
               } else {
                  dark = inDarkWorld();
                  MinecraftClient mc = MinecraftClient.getInstance();
                  if (dark && mc.player != null && who.equals(mc.player.getUuid()) && DarkWorldFall.hidingNewSkin()) {
                     dark = false;
                  }
               }

               return !dark
                  ? original
                  : new SkinTextures(e.texture(), null, original.capeTexture(), original.elytraTexture(), e.slim() ? Model.SLIM : Model.WIDE, false);
            }
         }
      } else {
         return original;
      }
   }

   public static void pickOwn() {
      if (RecruitSkinManager.PICKER_OPEN.compareAndSet(false, true)) {
         new Thread(() -> {
            String chosen = null;

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  PointerBuffer filters = stack.mallocPointer(1);
                  filters.put(stack.UTF8("*.png"));
                  filters.flip();
                  chosen = TinyFileDialogs.tinyfd_openFileDialog("Choose your Dark World skin", "", filters, "PNG skins", false);
               } catch (Throwable var10) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable var11) {
               String err = var11.getClass().getSimpleName() + ": " + var11.getMessage();
               MinecraftClient.getInstance().execute(() -> tell("Skin picker error: " + err, Formatting.RED));
            } finally {
               RecruitSkinManager.PICKER_OPEN.set(false);
            }

            if (chosen != null) {
               String path = chosen;
               MinecraftClient.getInstance().execute(() -> {
                  if (upload(Path.of(path), true)) {
                     savePref(path);
                  }
               });
            }
         }, "turnbattle-dw-skin-picker").start();
      }
   }

   public static void removeOwn() {
      savePref(null);
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(true);
      Net121.sendToServer(DarkWorldSkins.UPLOAD, buf);
   }

   private static void reupload() {
      String p = loadPref();
      if (p != null) {
         upload(Path.of(p), false);
      }
   }

   private static boolean upload(Path file, boolean loud) {
      byte[] png;
      try {
         if (!Files.isRegularFile(file)) {
            if (loud) {
               tell("Couldn't find that file.", Formatting.RED);
            }

            return false;
         }

         long size = Files.size(file);
         if (size > 30000L) {
            if (loud) {
               tell("That skin file is too big (" + size / 1024L + " KB) - skins up to 29 KB work.", Formatting.RED);
            }

            return false;
         }

         png = Files.readAllBytes(file);
      } catch (Exception var11) {
         if (loud) {
            tell("Couldn't read that file: " + var11.getMessage(), Formatting.RED);
         }

         return false;
      }

      if (png.length > 30000) {
         if (loud) {
            tell("That skin file is too big (" + png.length / 1024 + " KB) - skins up to 29 KB work.", Formatting.RED);
         }

         return false;
      } else {
         boolean slim;
         try {
            NativeImage img = NativeImage.read(Format.RGBA, new ByteArrayInputStream(png));

            label126: {
               int s;
               try {
                  int w = img.getWidth();
                  int h = img.getHeight();
                  if (w == h && (w == 64 || w == 128)) {
                     s = w / 64;
                     slim = (img.getColor(54 * s, 20 * s) >>> 24 & 0xFF) == 0;
                     break label126;
                  }

                  if (loud) {
                     tell("A skin has to be a 64\u00d764 PNG (this one is " + w + "\u00d7" + h + ").", Formatting.RED);
                  }

                  s = 0;
               } catch (Throwable var9) {
                  if (img != null) {
                     try {
                        img.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (img != null) {
                  img.close();
               }

               return (boolean)s;
            }

            if (img != null) {
               img.close();
            }
         } catch (Exception var10) {
            if (loud) {
               tell("That isn't a PNG image Minecraft can read.", Formatting.RED);
            }

            return false;
         }

         if (!DarkWorldSkins.validPng(png)) {
            if (loud) {
               tell("That skin couldn't be used - it has to be a 64\u00d764 PNG.", Formatting.RED);
            }

            return false;
         } else {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(false);
            buf.writeBoolean(slim);
            buf.writeByteArray(png);
            Net121.sendToServer(DarkWorldSkins.UPLOAD, buf);
            return true;
         }
      }
   }

   private static void tell(String msg, Formatting f) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(msg).formatted(f), false);
      }
   }

   private static String loadPref() {
      try {
         if (!Files.exists(PREF_FILE)) {
            return null;
         } else {
            String s = Files.readString(PREF_FILE).trim();
            return s.isEmpty() ? null : s;
         }
      } catch (Exception var1) {
         return null;
      }
   }

   private static void savePref(String path) {
      try {
         if (path == null) {
            Files.deleteIfExists(PREF_FILE);
         } else {
            Files.createDirectories(PREF_FILE.getParent());
            Files.writeString(PREF_FILE, path);
         }
      } catch (Exception var2) {
      }
   }

   private static record Entry(Identifier texture, boolean slim) {
   }
}
