package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
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
public class CardDrawScreen extends Screen {
   private static final int CARD_W = 112;
   private static final int CARD_H = 150;
   private static final int GAP = 14;
   private List<String> cards = List.of();
   private final List<RogueCards.Info> infos = new ArrayList<>();
   private int rerolls;
   private int handsAfter;
   private long dealtAt;
   private int hovered = -1;
   private boolean sent = false;

   public CardDrawScreen(List<String> cards, int rerolls, int handsAfter) {
      super(Text.literal("Pick a card"));
      this.setHand(cards, rerolls, handsAfter);
   }

   public void setHand(List<String> cards, int rerolls, int handsAfter) {
      this.cards = new ArrayList<>(cards);
      this.rerolls = rerolls;
      this.handsAfter = handsAfter;
      this.infos.clear();

      for (String c : cards) {
         this.infos.add(RogueCards.describe(c));
      }

      this.dealtAt = System.currentTimeMillis();
      this.sent = false;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   private int cardsLeft() {
      int n = this.infos.size();
      int total = n * 112 + (n - 1) * 14;
      return (this.width - total) / 2;
   }

   private int cardTop() {
      return Math.max(46, (this.height - 150) / 2);
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, -1073741816);
      float t = (float)(System.currentTimeMillis() - this.dealtAt) / 1000.0F;
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("\u2726  LEVEL UP!  \u2726").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}),
         this.width / 2,
         this.cardTop() - 34,
         16777215
      );
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(this.sent ? "..." : "Pick one card" + (this.handsAfter > 0 ? "  (" + this.handsAfter + " more to come)" : ""))
            .formatted(Formatting.YELLOW),
         this.width / 2,
         this.cardTop() - 20,
         16777215
      );
      int left = this.cardsLeft();
      int top = this.cardTop();
      this.hovered = -1;

      for (int i = 0; i < this.infos.size(); i++) {
         RogueCards.Info info = this.infos.get(i);
         int x = left + i * 126;
         float k = Math.max(0.0F, Math.min(1.0F, (t - (float)i * 0.08F) / 0.25F));
         k = k * k * (3.0F - 2.0F * k);
         boolean hov = !this.sent && k >= 1.0F && mx >= x && mx < x + 112 && my >= top && my < top + 150;
         if (hov) {
            this.hovered = i;
         }

         int y = top + (int)((1.0F - k) * 40.0F) - (hov ? 6 : 0);
         int alpha = (int)(k * 255.0F);
         if (alpha >= 8) {
            this.drawCard(ctx, info, x, y, hov, alpha, i);
         }
      }

      if (this.rerolls > 0 && !this.sent) {
         int bw = 120;
         int bx = this.width / 2 - bw / 2;
         int by = top + 150 + 14;
         boolean hovx = mx >= bx && mx < bx + bw && my >= by && my < by + 16;
         ctx.fill(bx - 1, by - 1, bx + bw + 1, by + 17, -7838004);
         ctx.fill(bx, by, bx + bw, by + 16, hovx ? -11914368 : -14016950);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("\u21bb Reroll (" + this.rerolls + " left)  [R]"), this.width / 2, by + 4, hovx ? 16777215 : 14535935
         );
      }

      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Click a card, or press its number").formatted(Formatting.DARK_GRAY), this.width / 2, this.height - 14, 16777215
      );
      super.render(ctx, mx, my, delta);
   }

   private void drawCard(DrawContext ctx, RogueCards.Info info, int x, int y, boolean hov, int alpha, int index) {
      int a = alpha << 24;
      int col = info.color() & 16777215;
      ctx.fill(x + 3, y + 4, x + 112 + 3, y + 150 + 4, alpha / 2 << 24);
      ctx.fill(x - 1, y - 1, x + 112 + 1, y + 150 + 1, a | (hov ? col : dim(col, 0.55F)));
      ctx.fill(x, y, x + 112, y + 150, a | (hov ? 1841200 : 1052188));
      ctx.fill(x, y, x + 112, y + 4, a | col);
      if (hov) {
         ctx.fill(x, y + 4, x + 112, y + 24, alpha / 6 << 24 | col);
      }

      int cx = x + 56;
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info.subtitle()), cx, y + 9, a | 11184810);
      ctx.getMatrices().push();
      ctx.getMatrices().translate((float)cx, (float)(y + 30), 0.0F);
      ctx.getMatrices().scale(2.4F, 2.4F, 1.0F);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info.icon()), 0, 0, a | col);
      ctx.getMatrices().pop();
      List title = this.textRenderer.wrapLines(Text.literal(info.title()).formatted(Formatting.BOLD), 102);
      int ty = y + 58;

      for (int i = 0; i < Math.min(2, title.size()); ty += 10) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, title.get(i), cx, ty, a | col);
         i++;
      }

      List desc = this.textRenderer.wrapLines(Text.literal(info.desc()), 100);
      int dy = ty + 4;

      for (int i = 0; i < desc.size() && dy < y + 150 - 22; dy += 10) {
         ctx.drawText(this.textRenderer, desc.get(i), x + 6, dy, a | 13421772, false);
         i++;
      }

      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal(hov ? "\u25b6 Pick \u25c0" : "[" + (index + 1) + "]"), cx, y + 150 - 13, a | (hov ? 16777062 : 7829367)
      );
   }

   private static int dim(int rgb, float f) {
      int r = (int)((float)(rgb >> 16 & 0xFF) * f);
      int g = (int)((float)(rgb >> 8 & 0xFF) * f);
      int b = (int)((float)(rgb & 0xFF) * f);
      return r << 16 | g << 8 | b;
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (this.sent) {
         return true;
      } else if (this.hovered >= 0) {
         this.pick(this.hovered);
         return true;
      } else {
         if (this.rerolls > 0) {
            int bw = 120;
            int bx = this.width / 2 - bw / 2;
            int by = this.cardTop() + 150 + 14;
            if (mx >= (double)bx && mx < (double)(bx + bw) && my >= (double)by && my < (double)(by + 16)) {
               this.reroll();
               return true;
            }
         }

         return true;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.sent && keyCode >= 49 && keyCode < 49 + this.infos.size() && System.currentTimeMillis() - this.dealtAt > 400L) {
         this.pick(keyCode - 49);
         return true;
      } else if (!this.sent && keyCode == 82 && this.rerolls > 0) {
         this.reroll();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void pick(int index) {
      if (index >= 0 && index < this.cards.size()) {
         this.sent = true;
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeVarInt(index);
         Net121.sendToServer(TurnBattleMod.ROGUE_CARD_CHOSEN, buf);
         TurnBattleSounds.playSelect();
         RogueliteClient.hand = null;
         if (this.client != null) {
            this.client.setScreen(null);
         }
      }
   }

   private void reroll() {
      this.sent = true;
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeVarInt(-1);
      Net121.sendToServer(TurnBattleMod.ROGUE_CARD_CHOSEN, buf);
      TurnBattleSounds.playSelect();
   }
}
