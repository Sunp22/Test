package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class BossBeaconScreen extends Screen {
   private BlockPos beaconPos = null;
   private boolean isHost = false;
   private int difficulty = -1;
   private final List<BossBeaconScreen.PlayerInfo> players = new ArrayList<>();
   private boolean starting = false;
   private int mx;
   private int my;
   private static final String[] DIFF_NAMES = new String[]{"Easy", "Medium", "Hard", "Very Hard", "Impossible"};
   private static final int[] DIFF_BORDER = new int[]{-13391309, -3364352, -2267648, -3399134, -6741300};
   private static final int[] DIFF_BG = new int[]{1426115635, 1438362112, 1439454464, 1438322961, 1434983355};
   private int diffButtonsY = 0;
   private int beginButtonY = 0;

   public BossBeaconScreen() {
      super(Text.literal("Boss Beacon"));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void onSync(BlockPos pos, boolean host, int diff, List<BossBeaconScreen.PlayerInfo> playerList) {
      this.beaconPos = pos;
      this.isHost = host;
      this.difficulty = diff;
      this.players.clear();
      this.players.addAll(playerList);
   }

   public void removed() {
      if (!this.starting && this.beaconPos != null) {
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(this.beaconPos.getX());
         buf.writeInt(this.beaconPos.getY());
         buf.writeInt(this.beaconPos.getZ());
         Net121.sendToServer(TurnBattleMod.BOSS_BEACON_LEAVE, buf);
      }
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
      this.mx = mouseX;
      this.my = mouseY;
      int pw = Math.min(480, this.width - 20);
      int ph = this.computePanelHeight();
      int px = (this.width - pw) / 2;
      int py = (this.height - ph) / 2;
      ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, -5618688);
      ctx.fill(px, py, px + pw, py + ph, -301004792);
      ctx.fill(px, py, px + pw, py + 3, -2267648);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("\u2726  Boss Beacon  \u2726").formatted(Formatting.GOLD), this.width / 2, py + 8, 16777215
      );
      int y = py + 24;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Players in Lobby:").formatted(Formatting.YELLOW), px + 12, y, 16777215);
      y += 13;
      int CARD_W = 110;
      int CARD_H = 28;
      int CARD_GAP = 6;
      int COLS = 4;
      int rows = Math.max(1, (this.players.size() + 4 - 1) / 4);
      int totalW = Math.min(this.players.size(), 4) * 116 - 6;
      int cardStartX = px + (pw - totalW) / 2;

      for (int i = 0; i < this.players.size(); i++) {
         BossBeaconScreen.PlayerInfo pi = this.players.get(i);
         int col = i % 4;
         int row = i / 4;
         int cx2 = cardStartX + col * 116;
         int cy2 = y + row * 32;
         int border = pi.isHost() ? -2263296 : -11193549;
         int bg = pi.isHost() ? -300020736 : -300676598;
         ctx.fill(cx2 - 1, cy2 - 1, cx2 + 110 + 1, cy2 + 28 + 1, border);
         ctx.fill(cx2, cy2, cx2 + 110, cy2 + 28, bg);
         String name = this.truncate(pi.name(), 100);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(name), cx2 + 55, cy2 + 5, 16777215);
         if (pi.isHost()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2605 HOST").formatted(Formatting.GOLD), cx2 + 55, cy2 + 16, 16777215);
         }
      }

      y += rows * 32 + 10;
      ctx.fill(px + 12, y, px + pw - 12, y + 1, -11189197);
      y += 8;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Select Difficulty:").formatted(Formatting.YELLOW), px + 12, y, 16777215);
      y += 13;
      this.diffButtonsY = y;
      int diffW = (pw - 24 - 24) / 5;
      int diffH = 24;

      for (int ix = 0; ix < 5; ix++) {
         int bx = px + 12 + ix * (diffW + 6);
         boolean selected = this.difficulty == ix;
         boolean hov = this.isHost && mouseX >= bx && mouseX <= bx + diffW && mouseY >= y && mouseY <= y + diffH;
         int borderCol = selected ? DIFF_BORDER[ix] : (hov ? brighten(DIFF_BORDER[ix]) : (this.isHost ? -12307661 : -14016480));
         int bgCol = selected ? DIFF_BG[ix] : (hov ? 872415231 : (this.isHost ? -300281840 : -301135864));
         int textCol = this.isHost ? (!selected && !hov ? -7833754 : -1) : -12307661;
         ctx.fill(bx - 1, y - 1, bx + diffW + 1, y + diffH + 1, borderCol);
         ctx.fill(bx, y, bx + diffW, y + diffH, bgCol);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(DIFF_NAMES[ix]), bx + diffW / 2, y + 8, textCol);
      }

      y += diffH + 12;
      this.beginButtonY = py + ph - 36;
      int btnW = 220;
      int btnH = 24;
      int btnX = px + (pw - btnW) / 2;
      int btnY = this.beginButtonY;
      if (this.starting) {
         ctx.fill(btnX - 1, btnY - 1, btnX + btnW + 1, btnY + btnH + 1, -10070784);
         ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, -300149760);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Starting battle...").formatted(Formatting.YELLOW), this.width / 2, btnY + 8, -5592525);
      } else if (!this.isHost) {
         ctx.fill(btnX - 1, btnY - 1, btnX + btnW + 1, btnY + btnH + 1, -12307661);
         ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, -300872435);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("\u23f3  Waiting for host\u2026").formatted(Formatting.GRAY), this.width / 2, btnY + 8, -7829368
         );
      } else if (this.difficulty < 0) {
         ctx.fill(btnX - 1, btnY - 1, btnX + btnW + 1, btnY + btnH + 1, -12307661);
         ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, -300872435);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("Select a difficulty first").formatted(Formatting.GRAY), this.width / 2, btnY + 8, -8952235
         );
      } else {
         boolean hov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
         int border = hov ? -13312 : -3373056;
         int bg = hov ? -298967552 : -300150528;
         ctx.fill(btnX - 1, btnY - 1, btnX + btnW + 1, btnY + btnH + 1, border);
         ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, bg);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("\u2694  BEGIN BATTLE  \u2694").formatted(Formatting.GOLD), this.width / 2, btnY + 8, hov ? -1 : -1127322
         );
      }

      super.render(ctx, mouseX, mouseY, delta);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.beaconPos != null) {
         int pw = Math.min(480, this.width - 20);
         int px = (this.width - pw) / 2;
         int py = (this.height - this.computePanelHeight()) / 2;
         int ph = this.computePanelHeight();
         if (this.isHost && this.diffButtonsY > 0) {
            int diffW = (pw - 24 - 24) / 5;
            int diffH = 24;

            for (int i = 0; i < 5; i++) {
               int bx = px + 12 + i * (diffW + 6);
               if (mouseX >= (double)bx
                  && mouseX <= (double)(bx + diffW)
                  && mouseY >= (double)this.diffButtonsY
                  && mouseY <= (double)(this.diffButtonsY + diffH)) {
                  this.sendSetDifficulty(i);
                  TurnBattleSounds.playSelect();
                  return true;
               }
            }
         }

         if (this.isHost && this.difficulty >= 0 && !this.starting) {
            int btnW = 220;
            int btnH = 24;
            int btnX = px + (pw - btnW) / 2;
            int btnY = py + ph - 36;
            if (mouseX >= (double)btnX && mouseX <= (double)(btnX + btnW) && mouseY >= (double)btnY && mouseY <= (double)(btnY + btnH)) {
               this.sendBegin();
               TurnBattleSounds.playSelect();
               return true;
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void sendSetDifficulty(int diff) {
      if (this.beaconPos != null) {
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(this.beaconPos.getX());
         buf.writeInt(this.beaconPos.getY());
         buf.writeInt(this.beaconPos.getZ());
         buf.writeInt(diff);
         Net121.sendToServer(TurnBattleMod.BOSS_BEACON_SET_DIFFICULTY, buf);
      }
   }

   private void sendBegin() {
      if (this.beaconPos != null && !this.starting) {
         this.starting = true;
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(this.beaconPos.getX());
         buf.writeInt(this.beaconPos.getY());
         buf.writeInt(this.beaconPos.getZ());
         Net121.sendToServer(TurnBattleMod.BOSS_BEACON_BEGIN, buf);
      }
   }

   private int computePanelHeight() {
      int rows = Math.max(1, (this.players.size() + 3) / 4);
      return 37 + rows * 32 + 10 + 9 + 13 + 24 + 12 + 24 + 12;
   }

   private String truncate(String s, int maxPx) {
      if (this.textRenderer.getWidth(s) <= maxPx) {
         return s;
      } else {
         while (s.length() > 1 && this.textRenderer.getWidth(s + "\u2026") > maxPx) {
            s = s.substring(0, s.length() - 1);
         }

         return s + "\u2026";
      }
   }

   private static int brighten(int argb) {
      int r = Math.min(255, (argb >> 16 & 0xFF) + 30);
      int g = Math.min(255, (argb >> 8 & 0xFF) + 30);
      int b = Math.min(255, (argb & 0xFF) + 30);
      return argb & 0xFF000000 | r << 16 | g << 8 | b;
   }

   public static record PlayerInfo(String name, boolean isHost) {
   }
}
