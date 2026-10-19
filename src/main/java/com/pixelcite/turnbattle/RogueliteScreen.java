package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class RogueliteScreen extends Screen {
   private static final String[] TABS = new String[]{"Run", "Upgrades", "Souls", "Records"};
   private int tab = 0;
   private int enableStep = 0;
   private boolean confirmGiveUp = false;
   private long beginClickedAt = 0L;
   private int inviteScroll = 0;
   private final List<RogueliteScreen.Btn> buttons = new ArrayList<>();
   private List<OrderedText> tooltip = null;

   public RogueliteScreen() {
      super(Text.literal("Soulbound Roguelite"));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   private int pw() {
      return Math.min(480, this.width - 16);
   }

   private int ph() {
      return Math.min(286, this.height - 16);
   }

   private int px() {
      return (this.width - this.pw()) / 2;
   }

   private int py() {
      return (this.height - this.ph()) / 2;
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      this.buttons.clear();
      this.tooltip = null;
      RogueliteClient.Info info = RogueliteClient.info;
      int pw = this.pw();
      int ph = this.ph();
      int px = this.px();
      int py = this.py();
      ctx.fill(0, 0, this.width, this.height, -1728053248);
      ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, -10070631);
      ctx.fill(px, py, px + pw, py + ph, -234026976);
      ctx.fill(px, py, px + pw, py + 3, -6723892);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u2726 Soulbound Roguelite").formatted(Formatting.LIGHT_PURPLE), px + 8, py + 8, 16777215);
      String shards = "\u25c6 " + info.shards + " Soul Shards";
      ctx.drawTextWithShadow(
         this.textRenderer, Text.literal(shards).formatted(Formatting.YELLOW), px + pw - 8 - this.textRenderer.getWidth(shards), py + 8, 16777215
      );
      if (!info.received) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Loading...").formatted(Formatting.GRAY), px + pw / 2, py + ph / 2, 16777215);
         super.render(ctx, mx, my, delta);
      } else {
         int tx = px + 8;

         for (int i = 0; i < TABS.length; i++) {
            int tw = 74;
            boolean active = this.tab == i;
            boolean hov = in((double)mx, (double)my, tx, py + 22, tw, 14);
            ctx.fill(tx, py + 22, tx + tw, py + 36, active ? -862361908 : (hov ? -2006559335 : 1145320294));
            ctx.fill(tx, active ? py + 22 : py + 35, tx + tw, py + 36, -6723892);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(TABS[i]), tx + tw / 2, py + 25, active ? 16777215 : 11184810);
            int t = i;
            this.buttons.add(new RogueliteScreen.Btn(tx, py + 22, tw, 14, () -> {
               this.tab = t;
               this.confirmGiveUp = false;
            }));
            tx += tw + 4;
         }

         int cy = py + 44;
         int ch = ph - 52;
         if (!info.enabled) {
            this.renderEnable(ctx, px, cy, pw, mx, my);
         } else {
            switch (this.tab) {
               case 0:
                  this.renderRun(ctx, info, px, cy, pw, ch, mx, my);
                  break;
               case 1:
                  this.renderUpgrades(ctx, info, px, cy, pw, ch, mx, my);
                  break;
               case 2:
                  this.renderSouls(ctx, info, px, cy, pw, ch, mx, my);
                  break;
               default:
                  this.renderRecords(ctx, info, px, cy, pw, mx, my);
            }
         }

         super.render(ctx, mx, my, delta);
         if (this.tooltip != null) {
            ctx.drawOrderedTooltip(this.textRenderer, this.tooltip, mx, my);
         }
      }
   }

   private void renderEnable(DrawContext ctx, int px, int y, int pw, int mx, int my) {
      int cx = px + pw / 2;
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Roguelite mode is not enabled on this world.").formatted(Formatting.GRAY), cx, y + 10, 16777215
      );
      if (this.enableStep == 0) {
         this.button(ctx, cx - 110, y + 30, 220, 20, "Enable Roguelite Mode?", -11197918, -8965325, mx, my, () -> this.enableStep = 1);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("(This permanently changes this world. Needs operator.)").formatted(Formatting.DARK_RED), cx, y + 56, 16777215
         );
      } else {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("Are you absolutely sure? This cannot be undone.").formatted(Formatting.RED), cx, y + 30, 16777215
         );
         this.button(ctx, cx - 115, y + 46, 100, 18, "Yes, Enable", -14535902, -12298940, mx, my, () -> {
            Net121.sendToServer(TurnBattleMod.ROGUE_ENABLE, PacketByteBufs.empty());
            this.enableStep = 0;
         });
         this.button(ctx, cx + 15, y + 46, 100, 18, "Cancel", -13426142, -11189180, mx, my, () -> this.enableStep = 0);
      }
   }

   private void renderRun(DrawContext ctx, RogueliteClient.Info info, int px, int y, int pw, int ch, int mx, int my) {
      int half = pw / 2 - 12;
      int lx = px + 10;
      int rx = px + pw / 2 + 4;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Your Party").formatted(Formatting.AQUA), lx, y, 16777215);
      int var12 = y + 12;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u2726 You").formatted(Formatting.GREEN), lx + 4, var12, 16777215);
      var12 += 11;

      for (RogueliteClient.PartyMate m : info.party) {
         String status = !m.online() ? " (away)" : (m.fallen() ? " (fallen)" : (m.inRun() ? " (in run)" : ""));
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal("\u2726 " + m.name())
               .formatted(m.online() ? Formatting.GREEN : Formatting.DARK_GREEN)
               .append(Text.literal(status).formatted(Formatting.GRAY)),
            lx + 4,
            var12,
            16777215
         );
         var12 += 11;
      }

      if (!info.party.isEmpty() && !info.inRun) {
         this.button(
            ctx,
            lx,
            var12 + 2,
            90,
            13,
            "Leave Party",
            -12312030,
            -10079437,
            mx,
            my,
            () -> Net121.sendToServer(TurnBattleMod.ROGUE_LEAVE_PARTY, PacketByteBufs.empty())
         );
         var12 += 18;
      }

      if (!info.inRun) {
         var12 += 6;
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("Invite a Player").formatted(Formatting.AQUA), lx, var12, 16777215);
         var12 += 12;
         List invitable = new ArrayList<>();

         for (RogueliteClient.OnlinePlayer op : info.online) {
            boolean already = false;

            for (RogueliteClient.PartyMate m : info.party) {
               if (m.name().equals(op.name())) {
                  already = true;
               }
            }

            if (!already) {
               invitable.add(op);
            }
         }

         if (invitable.isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("(nobody else is online)").formatted(Formatting.DARK_GRAY), lx + 4, var12, 16777215);
         }

         int rows = Math.max(1, (y + ch - var12) / 13);
         this.inviteScroll = Math.max(0, Math.min(this.inviteScroll, invitable.size() - rows));

         for (int i = this.inviteScroll; i < invitable.size() && i < this.inviteScroll + rows; i++) {
            RogueliteClient.OnlinePlayer op = invitable.get(i);
            int ry = var12 + (i - this.inviteScroll) * 13;
            boolean hov = in((double)mx, (double)my, lx, ry - 1, half, 12) && !op.inRun();
            if (hov) {
               ctx.fill(lx, ry - 1, lx + half, ry + 11, 872415231);
            }

            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal((hov ? "\u25b6 " : "  ") + op.name())
                  .formatted(op.inRun() ? Formatting.DARK_GRAY : Formatting.WHITE)
                  .append(Text.literal(op.inRun() ? "  in a run" : (hov ? "  [Invite]" : "")).formatted(op.inRun() ? Formatting.DARK_GRAY : Formatting.YELLOW)),
               lx + 2,
               ry,
               16777215
            );
            if (!op.inRun()) {
               UUID id = op.uuid();
               this.buttons.add(new RogueliteScreen.Btn(lx, ry - 1, half, 12, () -> {
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeUuid(id);
                  Net121.sendToServer(TurnBattleMod.ROGUE_INVITE, buf);
               }));
            }
         }
      }

      if (info.inRun) {
         long t = TurnBattleClientMod.rogueElapsedTicks;
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal(info.fallen ? "You've fallen" : "Run in progress")
               .formatted(new Formatting[]{info.fallen ? Formatting.RED : Formatting.GOLD, Formatting.BOLD}),
            rx,
            y,
            16777215
         );
         int var29 = y + 14;
         if (t >= 0L) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("Time: " + RogueUpgrades.formatTime(t)), rx, var29, 14540253);
            var29 += 11;
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal(String.format("Enemies: \u00d7%.2f strength", RogueliteClient.difficulty)), rx, var29, 14540253
            );
            var29 += 11;
         }

         if (info.fallen) {
            var29 = this.para(ctx, "You're watching over your party. When they've all fallen, everyone comes back here.", rx, var29 + 4, half, 11184810);
         } else {
            var29 = this.para(ctx, "Level up to draw cards. Enemies grow stronger every minute. Falling ends the run.", rx, var29 + 4, half, 11184810);
            int by = y + ch - 26;
            if (!this.confirmGiveUp) {
               this.button(ctx, rx, by, half, 18, "Give Up Run", -12312030, -10079437, mx, my, () -> this.confirmGiveUp = true);
            } else {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("Give up? It counts as falling.").formatted(Formatting.RED), rx, by - 12, 16777215);
               this.button(ctx, rx, by, half / 2 - 2, 18, "Yes, give up", -11197918, -7851213, mx, my, () -> {
                  Net121.sendToServer(TurnBattleMod.ROGUE_ABANDON, PacketByteBufs.empty());
                  this.confirmGiveUp = false;
                  this.close();
               });
               this.button(ctx, rx + half / 2 + 2, by, half / 2 - 2, 18, "Keep going", -14535902, -13413069, mx, my, () -> this.confirmGiveUp = false);
            }
         }
      } else {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("Next Run").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}), rx, y, 16777215);
         int var24 = y + 14;
         String soul = info.chosenSoul != null ? info.chosenSoul.getDisplayName() : "Random trait";
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Soul: ")
               .formatted(Formatting.GRAY)
               .append(Text.literal(soul).styled(s -> s.withColor(info.chosenSoul != null ? info.chosenSoul.getColor() & 16777215 : 16777215))),
            rx,
            var24,
            16777215
         );
         var24 += 12;
         List perks = new ArrayList<>();

         for (RogueUpgrades.Upgrade u : RogueUpgrades.ALL) {
            int lv = info.upgrade(u.id());
            if (lv > 0) {
               perks.add(u.name() + (u.maxLevel() > 1 ? " " + lv : ""));
            }
         }

         var24 = this.para(
            ctx, perks.isEmpty() ? "No upgrades yet - earn Soul Shards by playing runs." : "Upgrades: " + String.join(", ", perks), rx, var24, half, 11184810
         );
         var24 += 4;
         if (info.lastRunLevel >= 0) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("Last run").formatted(Formatting.LIGHT_PURPLE), rx, var24, 16777215);
            var24 += 11;
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(
                  "Lv "
                     + info.lastRunLevel
                     + " \u00b7 "
                     + RogueUpgrades.formatTime(info.lastRunTicks)
                     + " \u00b7 "
                     + info.lastRunBattles
                     + " won \u00b7 +"
                     + info.lastRunShards
                     + " \u25c6"
               ),
               rx,
               var24,
               13421772
            );
         }

         int by = y + ch - 30;
         boolean loading = System.currentTimeMillis() - this.beginClickedAt < 4000L;
         String label = loading ? "Starting..." : (info.party.isEmpty() ? "\u25b6 Begin Run" : "\u25b6 Begin Party Run");
         this.button(ctx, rx, by, half, 22, label, loading ? -13421790 : -11193447, loading ? -13421790 : -8960837, mx, my, loading ? null : () -> {
            this.beginClickedAt = System.currentTimeMillis();
            Net121.sendToServer(TurnBattleMod.ROGUE_BEGIN_RUN, PacketByteBufs.empty());
         });
         ctx.drawTextWithShadow(
            this.textRenderer, Text.literal("Starts at level 1 with an empty inventory.").formatted(Formatting.DARK_GRAY), rx, by - 11, 16777215
         );
      }
   }

   private void renderUpgrades(DrawContext ctx, RogueliteClient.Info info, int px, int y, int pw, int ch, int mx, int my) {
      List all = RogueUpgrades.ALL;
      int cols = 2;
      int rows = (all.size() + 1) / 2;
      int cw = (pw - 24) / cols;
      int rh = Math.min(44, (ch - 4) / rows);

      for (int i = 0; i < all.size(); i++) {
         RogueUpgrades.Upgrade u = all.get(i);
         int x = px + 8 + i % cols * (cw + 8);
         int yy = y + i / cols * rh;
         int lv = info.upgrade(u.id());
         int cost = u.nextCost(lv);
         boolean afford = cost >= 0 && info.shards >= cost && !info.inRun;
         ctx.fill(x, yy, x + cw, yy + rh - 4, 1143085124);
         ctx.fill(x, yy, x + 2, yy + rh - 4, lv >= u.maxLevel() ? -13244 : (lv > 0 ? -6723892 : -12307610));
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(u.icon() + " " + u.name()).formatted(Formatting.WHITE), x + 6, yy + 3, 16777215);

         for (int p = 0; p < u.maxLevel(); p++) {
            int bx = x + 6 + p * 9;
            ctx.fill(bx, yy + 14, bx + 7, yy + 18, p < lv ? -10031514 : -13421756);
         }

         String d = this.textRenderer.trimToWidth(u.desc(), cw - 10);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(d), x + 6, yy + 21, 10066329);
         if (in((double)mx, (double)my, x, yy, cw, rh - 4)) {
            this.tooltip = this.textRenderer.wrapLines(Text.literal(u.desc()), 200);
         }

         int bw = 70;
         int bx = x + cw - bw - 4;
         if (cost < 0) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("MAXED").formatted(Formatting.GOLD), bx + 18, yy + 4, 16777215);
         } else {
            this.button(
               ctx,
               bx,
               yy + 2,
               bw,
               13,
               "Buy " + cost + " \u25c6",
               afford ? -14531550 : -14540254,
               afford ? -13408717 : -14540254,
               mx,
               my,
               afford ? () -> this.send(0, u.id()) : null
            );
         }
      }

      if (info.inRun) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("Upgrades can be bought between runs.").formatted(Formatting.DARK_GRAY), px + pw / 2, y + ch - 8, 16777215
         );
      }
   }

   private void renderSouls(DrawContext ctx, RogueliteClient.Info info, int px, int y, int pw, int ch, int mx, int my) {
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Which soul do your runs start with?").formatted(Formatting.GRAY), px + 10, y, 16777215);
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal("Buy a soul once, then pick it for free. A Monster soul makes you a Monster for the run.").formatted(Formatting.DARK_GRAY),
         px + 10,
         y + 10,
         16777215
      );
      int cols = 3;
      int cw = (pw - 20 - (cols - 1) * 6) / cols;
      int rh = Math.min(40, (ch - 26) / 3);
      List opts = new ArrayList<>();
      opts.add(null);

      for (SoulType s : RogueUpgrades.SOULS) {
         opts.add(s);
      }

      for (int i = 0; i < opts.size(); i++) {
         SoulType s = opts.get(i);
         int x = px + 10 + i % cols * (cw + 6);
         int yy = y + 24 + i / cols * (rh + 4);
         boolean chosen = s == info.chosenSoul;
         boolean owned = s == null || info.ownedSouls.contains(s);
         int cost = s == null ? 0 : RogueUpgrades.soulCost(s);
         boolean afford = owned || info.shards >= cost;
         boolean hov = in((double)mx, (double)my, x, yy, cw, rh);
         int col = s == null ? -2236963 : s.getColor() | 0xFF000000;
         ctx.fill(x - 1, yy - 1, x + cw + 1, yy + rh + 1, chosen ? col : (hov ? -10070631 : -14015424));
         ctx.fill(x, yy, x + cw, yy + rh, hov ? -14804432 : -15462366);
         if (s != null) {
            try {
               RenderSystem.enableBlend();
               ctx.drawTexture(s.getTexture(), x + 6, yy + rh / 2 - 8, 0.0F, 0.0F, 16, 16, 16, 16);
            } catch (Exception var26) {
            }
         } else {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("?").formatted(Formatting.BOLD), x + 11, yy + rh / 2 - 4, 16777215);
         }

         String name = s == null ? "Random" : s.getDisplayName();
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), x + 28, yy + 5, col);
         String status = chosen ? "\u2713 Selected" : (owned ? "Owned - click to pick" : cost + " \u25c6 to unlock");
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(status), x + 28, yy + 16, chosen ? 6750054 : (owned ? 11184810 : (afford ? 16768341 : 8934741)));
         if (hov) {
            this.tooltip = this.textRenderer
               .wrapLines(Text.literal(s == null ? "A random soul trait (never Monster) every run." : s.getDisplayName() + ": " + s.getDescription()), 220);
         }

         if (!chosen && afford) {
            String id = s == null ? "" : s.name();
            this.buttons.add(new RogueliteScreen.Btn(x, yy, cw, rh, () -> this.send(1, id)));
         }
      }
   }

   private void renderRecords(DrawContext ctx, RogueliteClient.Info info, int px, int y, int pw, int mx, int my) {
      int x = px + 16;
      String[][] rows = new String[][]{
         {"Runs played", String.valueOf(info.totalRuns)},
         {"Highest level", info.bestLevel > 0 ? String.valueOf(info.bestLevel) : "-"},
         {"Longest run", info.bestTimeTicks > 0L ? RogueUpgrades.formatTime(info.bestTimeTicks) : "-"},
         {"Soul Shards earned", String.valueOf(info.totalShardsEarned)},
         {
               "Last run",
               info.lastRunLevel >= 0
                  ? "Lv "
                     + info.lastRunLevel
                     + ", "
                     + RogueUpgrades.formatTime(info.lastRunTicks)
                     + ", "
                     + info.lastRunBattles
                     + " battles won, +"
                     + info.lastRunShards
                     + " \u25c6"
                  : "-"
         }
      };

      for (int i = 0; i < rows.length; i++) {
         int yy = y + 4 + i * 18;
         ctx.fill(x - 4, yy - 3, px + pw - 12, yy + 12, i % 2 == 0 ? 587202559 : 301989887);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(rows[i][0]).formatted(Formatting.GRAY), x, yy, 16777215);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(rows[i][1]).formatted(Formatting.WHITE), x + 150, yy, 16777215);
      }

      this.para(
         ctx,
         "Soul Shards from a run: about 1.5 per level reached, plus 1 for every 3 battles won and every 3 minutes survived. Shard Magnet and the Bounty card add more.",
         x,
         y + 4 + rows.length * 18 + 8,
         pw - 40,
         8947848
      );
   }

   private void button(DrawContext ctx, int x, int y, int w, int h, String label, int bg, int bgHover, int mx, int my, Runnable action) {
      boolean hov = action != null && in((double)mx, (double)my, x, y, w, h);
      ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, action != null ? -6723892 : -12303292);
      ctx.fill(x, y, x + w, y + h, hov ? bgHover : bg);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), x + w / 2, y + (h - 8) / 2, action != null ? 16777215 : 8947848);
      if (action != null) {
         this.buttons.add(new RogueliteScreen.Btn(x, y, w, h, action));
      }
   }

   private int para(DrawContext ctx, String text, int x, int y, int w, int color) {
      for (OrderedText line : this.textRenderer.wrapLines(Text.literal(text), w)) {
         ctx.drawTextWithShadow(this.textRenderer, line, x, y, color);
         y += 10;
      }

      return y;
   }

   private static boolean in(double mx, double my, int x, int y, int w, int h) {
      return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
   }

   private void send(int type, String arg) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeByte(type);
      buf.writeString(arg);
      Net121.sendToServer(TurnBattleMod.ROGUE_UPGRADE, buf);
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (int i = this.buttons.size() - 1; i >= 0; i--) {
            if (this.buttons.get(i).hit(mx, my)) {
               TurnBattleSounds.playSelect();
               this.buttons.get(i).action().run();
               return true;
            }
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double amount) {
      this.inviteScroll = this.inviteScroll - (int)Math.signum(amount);
      if (this.inviteScroll < 0) {
         this.inviteScroll = 0;
      }

      return true;
   }

   private static record Btn(int x, int y, int w, int h, Runnable action) {
      boolean hit(double mx, double my) {
         return mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
      }
   }
}
