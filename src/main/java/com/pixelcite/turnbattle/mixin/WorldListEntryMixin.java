package com.pixelcite.turnbattle.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldListWidget.WorldEntry;
import net.minecraft.client.util.Window;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldEntry.class})
public class WorldListEntryMixin {
   @Shadow
   private LevelSummary field_19138;
   private static final Map<String, WorldListEntryMixin.SaveInfo> SAVE_CACHE = new HashMap<>();
   private static final Map<String, Long> CACHE_MTIME = new HashMap<>();

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void drawSaveInfoTag(
      DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci
   ) {
      if (this.field_19138 != null) {
         String name = this.field_19138.getName();
         WorldListEntryMixin.SaveInfo info = getOrRefresh(name);
         long mins = info.playtimeSec() / 60L;
         long secs = info.playtimeSec() % 60L;
         String tag = info.vesselName() + "  LV " + info.level() + " - " + info.biomeName() + " - " + String.format("%d:%02d", mins, secs);
         TextRenderer tr = MinecraftClient.getInstance().textRenderer;
         Window win = MinecraftClient.getInstance().getWindow();
         int tagW = tr.getWidth(tag) + 10;
         int tagH = 9 + 4;
         int tagX = win.getScaledWidth() - tagW - 6;
         int tagY = y + (entryHeight - tagH) / 2;
         ctx.fill(tagX - 1, tagY - 1, tagX + tagW + 1, tagY + tagH + 1, -1);
         ctx.fill(tagX, tagY, tagX + tagW, tagY + tagH, -16777216);
         ctx.drawTextWithShadow(tr, tag, tagX + 4, tagY + 2, -1);
      }
   }

   private static WorldListEntryMixin.SaveInfo getOrRefresh(String worldName) {
      try {
         Path df = MinecraftClient.getInstance().getLevelStorage().getSavesDirectory().resolve(worldName).resolve("data").resolve("turnbattle_data.dat");
         long mtime = Files.exists(df) ? Files.getLastModifiedTime(df).toMillis() : 0L;
         Long cached = CACHE_MTIME.get(worldName);
         if (cached != null && cached == mtime && SAVE_CACHE.containsKey(worldName)) {
            return SAVE_CACHE.get(worldName);
         } else {
            WorldListEntryMixin.SaveInfo info = readSaveInfo(df);
            SAVE_CACHE.put(worldName, info);
            CACHE_MTIME.put(worldName, mtime);
            return info;
         }
      } catch (Exception var6) {
         return SAVE_CACHE.getOrDefault(worldName, new WorldListEntryMixin.SaveInfo("???", 1, 0L, "---"));
      }
   }

   private static WorldListEntryMixin.SaveInfo readSaveInfo(Path dataFile) {
      String vesselName = "???";
      int saveLevel = 1;
      long playtime = 0L;
      String biome = "---";

      try {
         if (!Files.exists(dataFile)) {
            return new WorldListEntryMixin.SaveInfo(vesselName, saveLevel, playtime, biome);
         }

         NbtCompound root = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
         NbtCompound data = root.contains("data") ? root.getCompound("data") : root;
         if (data.contains("players")) {
            NbtList list = data.getList("players", 10);
            if (!list.isEmpty()) {
               NbtCompound prog = list.getCompound(0);
               if (prog.contains("vesselName")) {
                  vesselName = prog.getString("vesselName");
               }

               if (prog.contains("savedLevel")) {
                  saveLevel = prog.getInt("savedLevel");
               }

               if (prog.contains("savedPlaytimeSec")) {
                  playtime = prog.getLong("savedPlaytimeSec");
               }

               if (prog.contains("lastBiome")) {
                  biome = prog.getString("lastBiome");
               }

               if (biome.isEmpty()) {
                  biome = "---";
               }

               if (vesselName.isEmpty()) {
                  vesselName = "???";
               }
            }
         }
      } catch (Exception var10) {
      }

      return new WorldListEntryMixin.SaveInfo(vesselName, saveLevel, playtime, biome);
   }

   private static record SaveInfo(String vesselName, int level, long playtimeSec, String biomeName) {
   }
}
