package com.pixelcite.turnbattle;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class ServerSettings {
   public static final Identifier REQUEST = Identifier.of("turnbattle", "settings_request");
   public static final Identifier SET = Identifier.of("turnbattle", "settings_set");
   public static final Identifier SYNC = Identifier.of("turnbattle", "settings_sync");
   static final Set<String> LOCKED = Set.of("storyMode");
   static final List<ServerSettings.Entry> ENTRIES = List.of(
      new ServerSettings.Entry(
         "rollingHealth",
         "Rolling Health",
         "HP rolls down toward a hit instead of dropping all at once, like EarthBound, so a quick heal can save you before it lands",
         ServerSettings.Kind.BOOL,
         0.0F,
         1.0F,
         1.0F,
         c -> b(c.rollingHealth),
         (c, v) -> c.rollingHealth = v >= 0.5F
      ),
      new ServerSettings.Entry(
         "storyMode",
         "Story Mode",
         "Turns on the story, its opening and the Story page of the menu, for everyone on the server, not available yet",
         ServerSettings.Kind.BOOL,
         0.0F,
         1.0F,
         1.0F,
         c -> b(c.storyMode),
         (c, v) -> c.storyMode = v >= 0.5F
      ),
      new ServerSettings.Entry(
         "passiveMobBattles",
         "Passive Mob Battles",
         "Lets passive mobs, like cows, pigs and sheep, be levelled and fought, fish and squid always can be",
         ServerSettings.Kind.BOOL,
         0.0F,
         1.0F,
         1.0F,
         c -> b(c.passiveMobBattles),
         (c, v) -> c.passiveMobBattles = v >= 0.5F
      ),
      new ServerSettings.Entry(
         "showMobNametags",
         "Mob Nameplates",
         "Shows the plate with a mob's name and LV over its head, applies to mobs as they're levelled",
         ServerSettings.Kind.BOOL,
         0.0F,
         1.0F,
         1.0F,
         c -> b(c.showMobNametags),
         (c, v) -> c.showMobNametags = v >= 0.5F
      ),
      new ServerSettings.Entry(
         "skillDamageScale",
         "Skill Damage Multiplier",
         "Multiplies the damage of every damaging skill, 1.5 by default, lower makes fights longer",
         ServerSettings.Kind.FLOAT,
         0.1F,
         10.0F,
         0.1F,
         c -> c.skillDamageScale,
         (c, v) -> c.skillDamageScale = v
      ),
      new ServerSettings.Entry(
         "healSpellScale",
         "Heal Spell Multiplier",
         "Multiplies the HP every healing skill restores, 1.5 by default",
         ServerSettings.Kind.FLOAT,
         0.1F,
         10.0F,
         0.1F,
         c -> c.healSpellScale,
         (c, v) -> c.healSpellScale = v
      ),
      new ServerSettings.Entry(
         "tpSpellScale",
         "TP Gain Spell Multiplier",
         "Multiplies the TP that skills like PSI Magnet give, 1.5 by default",
         ServerSettings.Kind.FLOAT,
         0.1F,
         10.0F,
         0.1F,
         c -> c.tpSpellScale,
         (c, v) -> c.tpSpellScale = v
      ),
      new ServerSettings.Entry(
         "distanceLevelMultiplier",
         "Distance Level Multiplier",
         "Mobs get stronger the further from 0, 0 they spawn, LV 99 at 10,000 blocks, higher numbers get there sooner, 1.0 by default",
         ServerSettings.Kind.FLOAT,
         0.1F,
         10.0F,
         0.1F,
         c -> c.distanceLevelMultiplier,
         (c, v) -> c.distanceLevelMultiplier = v
      ),
      new ServerSettings.Entry(
         "baseSkillSlots",
         "Base Skill Slots",
         "How many skills everyone can have equipped at LV 1, 10 by default",
         ServerSettings.Kind.INT,
         1.0F,
         999.0F,
         1.0F,
         c -> (float)c.baseSkillSlots,
         (c, v) -> c.baseSkillSlots = Math.round(v)
      ),
      new ServerSettings.Entry(
         "skillSlotsPerTenLevels",
         "Skill Slots per 10 LV",
         "Extra skill slots gained every 10 levels, 5 by default",
         ServerSettings.Kind.INT,
         0.0F,
         999.0F,
         1.0F,
         c -> (float)c.skillSlotsPerTenLevels,
         (c, v) -> c.skillSlotsPerTenLevels = Math.round(v)
      )
   );

   private ServerSettings() {
   }

   private static float b(boolean v) {
      return v ? 1.0F : 0.0F;
   }

   public static boolean canEdit(ServerPlayerEntity p) {
      if (p.hasPermissionLevel(2)) {
         return true;
      } else {
         MinecraftServer s = p.getServer();
         return s != null && s.isSingleplayer() && s.isHost(p.getGameProfile());
      }
   }

   public static void registerChannels() {
      Net121.registerC2SChannel(REQUEST);
      Net121.registerC2SChannel(SET);
      Net121.registerS2CChannel(SYNC);
   }

   public static void registerServer() {
      Net121.registerServerReceiver(REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> sendSync(player)));
      Net121.registerServerReceiver(SET, (server, player, handler, buf, sender) -> {
         String key = buf.readString(64);
         float value = buf.readFloat();
         server.execute(() -> {
            if (!canEdit(player)) {
               player.sendMessage(Text.literal("Only operators can change the server's settings.").formatted(Formatting.GRAY), true);
               sendSync(player);
            } else {
               ServerSettings.Entry e = null;

               for (ServerSettings.Entry x : ENTRIES) {
                  if (x.key().equals(key)) {
                     e = x;
                  }
               }

               if (e != null && !Float.isNaN(value) && !Float.isInfinite(value)) {
                  if (LOCKED.contains(e.key())) {
                     sendSync(player);
                  } else {
                     TurnBattleConfig cfg = TurnBattleConfig.get();
                     float before = e.get().apply(cfg);
                     float v = Math.max(e.min(), Math.min(e.max(), value));
                     if (e.kind() == ServerSettings.Kind.FLOAT) {
                        v = (float)Math.round(v * 100.0F) / 100.0F;
                     }

                     e.set().accept(cfg, v);
                     cfg.clamp();
                     TurnBattleConfig.save();
                     float after = e.get().apply(cfg);
                     if (after != before) {
                        TurnBattleMod.LOGGER.info("[TurnBattle] {} set {} to {}", new Object[]{player.getName().getString(), e.key(), show(e, after)});
                        applied(server, e);
                     }

                     for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                        if (canEdit(p)) {
                           sendSync(p);
                        }
                     }
                  }
               }
            }
         });
      });
   }

   private static void applied(MinecraftServer server, ServerSettings.Entry e) {
      if (e.key().equals("storyMode")) {
         BattleData data = BattleData.get(server);

         for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            StoryManager.sendState(p, data.getOrCreate(p.getUuid()));
         }
      }
   }

   private static String show(ServerSettings.Entry e, float v) {
      return switch (e.kind()) {
         case BOOL -> v >= 0.5F ? "on" : "off";
         case INT -> String.valueOf(Math.round(v));
         case FLOAT -> String.valueOf(v);
      };
   }

   public static void sendSync(ServerPlayerEntity player) {
      PacketByteBuf buf = PacketByteBufs.create();
      boolean can = canEdit(player);
      buf.writeBoolean(can);
      if (can) {
         TurnBattleConfig cfg = TurnBattleConfig.get();
         buf.writeInt(ENTRIES.size());

         for (ServerSettings.Entry e : ENTRIES) {
            buf.writeString(e.key(), 64);
            buf.writeString(e.label(), 64);
            buf.writeString(e.desc(), 512);
            buf.writeByte(e.kind().ordinal());
            buf.writeFloat(e.get().apply(cfg));
            buf.writeFloat(e.min());
            buf.writeFloat(e.max());
            buf.writeFloat(e.step());
            buf.writeBoolean(LOCKED.contains(e.key()));
         }
      }

      Net121.sendToClient(player, SYNC, buf);
   }

   static record Entry(
      String key,
      String label,
      String desc,
      ServerSettings.Kind kind,
      float min,
      float max,
      float step,
      Function<TurnBattleConfig, Float> get,
      BiConsumer<TurnBattleConfig, Float> set
   ) {
   }

   public static enum Kind {
      BOOL,
      INT,
      FLOAT;
   }
}
