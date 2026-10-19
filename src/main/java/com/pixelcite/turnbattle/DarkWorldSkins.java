package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;

public final class DarkWorldSkins {
   public static final Identifier UPLOAD = Identifier.of("turnbattle", "dw_skin_upload");
   public static final Identifier SKINS = Identifier.of("turnbattle", "dw_skins");
   public static final int MAX_BYTES = 30000;
   private static final long UPLOAD_COOLDOWN_MS = 3000L;
   private static final Map<UUID, Long> lastUpload = new HashMap<>();

   private DarkWorldSkins() {
   }

   public static void register() {
      Net121.registerS2CChannel(SKINS);
      Net121.registerC2SChannel(SKINS);
      Net121.registerS2CChannel(UPLOAD);
      Net121.registerC2SChannel(UPLOAD);
      Net121.registerServerReceiver(UPLOAD, (server, player, handler, buf, sender) -> {
         boolean remove = buf.readBoolean();
         boolean slim = !remove && buf.readBoolean();
         byte[] png = remove ? null : buf.readByteArray(30000);
         server.execute(() -> onUpload(server, player, remove, slim, png));
      });
   }

   private static void onUpload(MinecraftServer server, ServerPlayerEntity player, boolean remove, boolean slim, byte[] png) {
      long now = System.currentTimeMillis();
      Long last = lastUpload.get(player.getUuid());
      if (!remove && last != null && now - last < 3000L) {
         player.sendMessage(Text.literal("Hold on a moment before changing your Dark World skin again.").formatted(Formatting.YELLOW), false);
      } else {
         if (!remove) {
            lastUpload.put(player.getUuid(), now);
         }

         DarkWorldSkins.Data data = DarkWorldSkins.Data.get(server);
         if (remove) {
            if (data.skins.remove(player.getUuid()) != null) {
               data.markDirty();
               broadcast(server, player.getUuid(), null);
               player.sendMessage(Text.literal("Your Dark World skin was removed.").formatted(Formatting.YELLOW), false);
            }
         } else if (!validPng(png)) {
            player.sendMessage(Text.literal("That skin couldn't be used - it has to be a 64\u00d764 (or 128\u00d7128) PNG.").formatted(Formatting.RED), false);
         } else {
            DarkWorldSkins.Skin skin = new DarkWorldSkins.Skin(png, slim);
            data.skins.put(player.getUuid(), skin);
            data.markDirty();
            broadcast(server, player.getUuid(), skin);
            player.sendMessage(Text.literal("Dark World skin set! You'll wear it whenever you're in a Dark World.").formatted(Formatting.GREEN), false);
         }
      }
   }

   public static void onJoin(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         DarkWorldSkins.Data data = DarkWorldSkins.Data.get(server);
         List all = new ArrayList<>(data.skins.entrySet());
         all.sort(Comparator.comparing(e -> !e.getKey().equals(player.getUuid())));
         boolean first = true;
         int i = 0;

         do {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(first);
            int n = Math.min(16, all.size() - i);
            buf.writeVarInt(n);

            for (int k = 0; k < n; i++) {
               write(buf, all.get(i).getKey(), all.get(i).getValue());
               k++;
            }

            Net121.sendToClient(player, SKINS, buf);
            first = false;
         } while (i < all.size());
      }
   }

   private static void broadcast(MinecraftServer server, UUID who, DarkWorldSkins.Skin skin) {
      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeBoolean(false);
         buf.writeVarInt(1);
         write(buf, who, skin);
         Net121.sendToClient(p, SKINS, buf);
      }
   }

   private static void write(PacketByteBuf buf, UUID who, DarkWorldSkins.Skin skin) {
      buf.writeUuid(who);
      buf.writeBoolean(skin != null);
      if (skin != null) {
         buf.writeBoolean(skin.slim());
         buf.writeByteArray(skin.png());
      }
   }

   static boolean validPng(byte[] png) {
      if (png != null && png.length >= 33 && png.length <= 30000) {
         byte[] sig = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};

         for (int i = 0; i < 8; i++) {
            if (png[i] != sig[i]) {
               return false;
            }
         }

         if (png[12] == 73 && png[13] == 72 && png[14] == 68 && png[15] == 82) {
            int w = (png[16] & 255) << 24 | (png[17] & 255) << 16 | (png[18] & 255) << 8 | png[19] & 255;
            int h = (png[20] & 255) << 24 | (png[21] & 255) << 16 | (png[22] & 255) << 8 | png[23] & 255;
            return w == h && (w == 64 || w == 128);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static class Data extends PersistentState {
      final Map<UUID, DarkWorldSkins.Skin> skins = new LinkedHashMap<>();

      public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
         NbtList list = new NbtList();
         this.skins.forEach((uuid, s) -> {
            NbtCompound t = new NbtCompound();
            t.putUuid("uuid", uuid);
            t.putByteArray("png", s.png());
            t.putBoolean("slim", s.slim());
            list.add(t);
         });
         nbt.put("skins", list);
         return nbt;
      }

      static DarkWorldSkins.Data read(NbtCompound nbt) {
         DarkWorldSkins.Data d = new DarkWorldSkins.Data();
         NbtList list = nbt.getList("skins", 10);

         for (int i = 0; i < list.size(); i++) {
            NbtCompound t = list.getCompound(i);
            byte[] png = t.getByteArray("png");
            if (t.containsUuid("uuid") && DarkWorldSkins.validPng(png)) {
               d.skins.put(t.getUuid("uuid"), new DarkWorldSkins.Skin(png, t.getBoolean("slim")));
            }
         }

         return d;
      }

      static DarkWorldSkins.Data get(MinecraftServer server) {
         return (DarkWorldSkins.Data)server.getWorld(World.OVERWORLD)
            .getPersistentStateManager()
            .getOrCreate(new Type(DarkWorldSkins.Data::new, (nbt, lookup) -> read(nbt), null), "turnbattle_dw_skins");
      }
   }

   public static record Skin(byte[] png, boolean slim) {
   }
}
