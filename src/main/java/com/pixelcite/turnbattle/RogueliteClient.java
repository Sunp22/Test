package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public final class RogueliteClient {
   public static volatile RogueliteClient.Info info = new RogueliteClient.Info();
   public static float difficulty = 1.0F;
   public static boolean fallen = false;
   public static int handsWaiting = 0;
   public static List<String> hand = null;
   public static int rerolls = 0;
   public static int handsAfter = 0;
   private static boolean menuWanted = false;

   private RogueliteClient() {
   }

   static void register() {
      Net121.registerClientReceiver(TurnBattleMod.ROGUE_OPEN, (client, handler, buf, sender) -> {
         boolean open = buf.readBoolean();
         client.execute(() -> {
            menuWanted = open;
            if (open) {
               if (client.currentScreen instanceof RogueliteScreen) {
                  menuWanted = false;
               } else if (client.currentScreen == null || client.currentScreen instanceof PartyScreen) {
                  client.setScreen(new RogueliteScreen());
                  menuWanted = false;
               }
            } else if (client.currentScreen instanceof RogueliteScreen) {
               client.setScreen(null);
            }

            Net121.sendToServer(TurnBattleMod.ROGUE_INFO_REQUEST, PacketByteBufs.empty());
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.ROGUE_INFO, (client, handler, buf, sender) -> {
         RogueliteClient.Info in = new RogueliteClient.Info();
         in.enabled = buf.readBoolean();
         in.shards = buf.readInt();
         in.inRun = buf.readBoolean();
         in.fallen = buf.readBoolean();
         int n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            in.online.add(new RogueliteClient.OnlinePlayer(buf.readString(), buf.readUuid(), buf.readBoolean()));
         }

         n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            in.party.add(new RogueliteClient.PartyMate(buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
         }

         n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            in.upgrades.put(buf.readString(), buf.readVarInt());
         }

         String soul = buf.readString();
         in.chosenSoul = soulOf(soul);
         n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            SoulType s = soulOf(buf.readString());
            if (s != null) {
               in.ownedSouls.add(s);
            }
         }

         in.totalRuns = buf.readVarInt();
         in.bestLevel = buf.readVarInt();
         in.bestTimeTicks = buf.readLong();
         in.totalShardsEarned = buf.readVarInt();
         in.lastRunLevel = buf.readInt();
         in.lastRunTicks = buf.readLong();
         in.lastRunBattles = buf.readVarInt();
         in.lastRunShards = buf.readVarInt();
         in.received = true;
         client.execute(() -> {
            info = in;
            TurnBattleClientMod.isRogueliteWorld = in.enabled;
            TurnBattleClientMod.rogueliteInRun = in.inRun;
            if (!in.inRun) {
               TurnBattleClientMod.rogueElapsedTicks = -1L;
            }

            fallen = in.fallen;
            TurnBattleClientMod.onlinePlayers.clear();
            List names = new ArrayList<>();
            List uuids = new ArrayList<>();

            for (RogueliteClient.OnlinePlayer p : in.online) {
               TurnBattleClientMod.onlinePlayers.add(new TurnBattleClientMod.OnlinePlayerEntry(p.name(), p.uuid()));
               names.add(p.name());
               uuids.add(p.uuid());
            }

            if (client.currentScreen instanceof PartyScreen ps) {
               ps.onOnlinePlayers(names, uuids);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.ROGUE_TIMER, (client, handler, buf, sender) -> {
         long elapsed = buf.readLong();
         float scale = buf.readFloat();
         boolean dead = buf.readBoolean();
         int waiting = buf.readVarInt();
         client.execute(() -> {
            TurnBattleClientMod.rogueElapsedTicks = elapsed;
            TurnBattleClientMod.rogueliteInRun = elapsed >= 0L;
            difficulty = scale;
            fallen = dead && elapsed >= 0L;
            handsWaiting = elapsed >= 0L ? waiting : 0;
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.ROGUE_CARD_DRAW, (client, handler, buf, sender) -> {
         int n = buf.readVarInt();
         List cards = new ArrayList<>();

         for (int i = 0; i < n; i++) {
            cards.add(buf.readString());
         }

         int rr = buf.readVarInt();
         int after = buf.readVarInt();
         client.execute(() -> {
            hand = cards.isEmpty() ? null : cards;
            rerolls = rr;
            handsAfter = after;
            if (client.currentScreen instanceof CardDrawScreen cds) {
               if (hand == null) {
                  client.setScreen(null);
               } else {
                  cds.setHand(hand, rerolls, handsAfter);
               }
            } else if (hand != null && client.currentScreen == null) {
               client.setScreen(new CardDrawScreen(hand, rerolls, handsAfter));
            }
         });
      });
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> client.execute(() -> {
            info = new RogueliteClient.Info();
            hand = null;
            menuWanted = false;
            difficulty = 1.0F;
            fallen = false;
            handsWaiting = 0;
         }));
   }

   private static SoulType soulOf(String name) {
      if (name != null && !name.isEmpty()) {
         try {
            return SoulType.valueOf(name);
         } catch (IllegalArgumentException var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   static void tick(MinecraftClient mc) {
      if (mc.player != null && mc.currentScreen == null) {
         if (hand != null && TurnBattleClientMod.rogueliteInRun && !fallen) {
            mc.setScreen(new CardDrawScreen(hand, rerolls, handsAfter));
         } else if (menuWanted) {
            menuWanted = false;
            mc.setScreen(new RogueliteScreen());
         }
      }
   }

   static void renderHud(DrawContext ctx, MinecraftClient mc) {
      if (mc.player != null && mc.currentScreen == null && !mc.options.hudHidden) {
         long elapsed = TurnBattleClientMod.rogueElapsedTicks;
         if (TurnBattleClientMod.rogueliteInRun && elapsed >= 0L) {
            TextRenderer tr = mc.textRenderer;
            String time = "\u23f1 " + RogueUpgrades.formatTime(elapsed);
            String diff = String.format("Enemies \u00d7%.2f", difficulty);
            int x = 4;
            int y = 4;
            int w = Math.max(tr.getWidth(time), tr.getWidth(diff)) + 8;
            int lines = 2 + (fallen ? 1 : 0) + (handsWaiting > 0 && !fallen ? 1 : 0);
            ctx.fill(x - 2, y - 2, x + w, y + lines * 10 + 1, -2013265920);
            ctx.drawTextWithShadow(tr, Text.literal(time).formatted(Formatting.YELLOW), x, y, 16777215);
            int dc = difficulty >= 2.5F ? -48060 : (difficulty >= 1.75F ? -26300 : -2236963);
            ctx.drawTextWithShadow(tr, Text.literal(diff), x, y + 10, dc);
            int ly = y + 20;
            if (fallen) {
               ctx.drawTextWithShadow(tr, Text.literal("Fallen - spectating").formatted(Formatting.GRAY), x, ly, 16777215);
            } else if (handsWaiting > 0) {
               ctx.drawTextWithShadow(
                  tr,
                  Text.literal("\u2727 " + handsWaiting + " hand" + (handsWaiting == 1 ? "" : "s") + " to pick").formatted(Formatting.LIGHT_PURPLE),
                  x,
                  ly,
                  16777215
               );
            }
         }
      }
   }

   public static final class Info {
      public boolean enabled;
      public boolean inRun;
      public boolean fallen;
      public int shards;
      public final List<RogueliteClient.OnlinePlayer> online = new ArrayList<>();
      public final List<RogueliteClient.PartyMate> party = new ArrayList<>();
      public final Map<String, Integer> upgrades = new HashMap<>();
      public SoulType chosenSoul;
      public final Set<SoulType> ownedSouls = EnumSet.noneOf(SoulType.class);
      public int totalRuns;
      public int bestLevel;
      public int totalShardsEarned;
      public int lastRunLevel = -1;
      public int lastRunBattles;
      public int lastRunShards;
      public long bestTimeTicks;
      public long lastRunTicks;
      public boolean received;

      public int upgrade(String id) {
         return this.upgrades.getOrDefault(id, 0);
      }
   }

   public static record OnlinePlayer(String name, UUID uuid, boolean inRun) {
   }

   public static record PartyMate(String name, boolean online, boolean inRun, boolean fallen) {
   }
}
