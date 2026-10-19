package com.pixelcite.turnbattle;

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
public class WorldTypeSelectScreen extends Screen {
   private static final int CARD_W = 200;
   private static final int CARD_H = 200;
   private static final int GAP = 20;
   private static final int TEXT_PAD = 10;
   private static final String NORMAL_DESC = "A standard RPG experience. Build a base, pick a class, fill out your skill tree, fight bosses, and craft unique builds.";
   private static final String ROGUE_DESC = "Run-based mode (with co-op) where you're tasked with leveling up as high as possible while enemies get stronger every minute. Unlock permanent upgrades, and with every level-up, draw three cards and pick one to unlock stat boosts and skills from all classes.";

   public WorldTypeSelectScreen() {
      super(Text.literal("Choose World Type"));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("\u2726  Choose Your World Type  \u2726").formatted(Formatting.GOLD), this.width / 2, this.height / 2 - 130, 16777215
      );
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("This choice is permanent for this world.").formatted(Formatting.GRAY),
         this.width / 2,
         this.height / 2 - 116,
         16777215
      );
      int totalW = 420;
      int cardY = this.height / 2 - 100;
      int normalX = this.width / 2 - totalW / 2;
      int rogueX = normalX + 200 + 20;
      boolean hovN = isOver((double)mx, (double)my, normalX, cardY, 200, 200);
      boolean hovR = isOver((double)mx, (double)my, rogueX, cardY, 200, 200);
      this.drawCard(
         ctx,
         normalX,
         cardY,
         hovN,
         -14531414,
         -2013243956,
         -301658590,
         "  ",
         "NORMAL",
         -11167233,
         -14531448,
         "A standard RPG experience. Build a base, pick a class, fill out your skill tree, fight bosses, and craft unique builds."
      );
      this.drawCard(
         ctx,
         rogueX,
         cardY,
         hovR,
         -5618688,
         -1999874816,
         -300283904,
         "  ",
         "ROGUE-LITE",
         -35021,
         -7851264,
         "Run-based mode (with co-op) where you're tasked with leveling up as high as possible while enemies get stronger every minute. Unlock permanent upgrades, and with every level-up, draw three cards and pick one to unlock stat boosts and skills from all classes."
      );
      super.render(ctx, mx, my, delta);
   }

   private void drawCard(
      DrawContext ctx, int x, int y, boolean hov, int borderCol, int accentCol, int bgCol, String title, String tag, int tagColor, int tagBg, String desc
   ) {
      if (hov) {
         ctx.fill(x - 2, y - 2, x + 200 + 2, y + 200 + 2, borderCol);
      }

      ctx.fill(x - 1, y - 1, x + 200 + 1, y + 200 + 1, borderCol);
      ctx.fill(x, y, x + 200, y + 200, bgCol);
      ctx.fill(x, y, x + 200, y + 3, accentCol);
      int cy = y + 9;
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(title).formatted(Formatting.WHITE), x + 100, cy, 16777215);
      cy += 9 + 4;
      int tagW = this.textRenderer.getWidth(tag) + 8;
      int tagX = x + (200 - tagW) / 2;
      ctx.fill(tagX - 1, cy - 1, tagX + tagW + 1, cy + 11, borderCol);
      ctx.fill(tagX, cy, tagX + tagW, cy + 10, tagBg);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(tag), x + 100, cy + 1, tagColor);
      cy += 16;
      ctx.fill(x + 8, cy, x + 200 - 8, cy + 1, borderCol);
      cy += 6;
      int wrapWidth = 180;

      for (OrderedText line : this.textRenderer.wrapLines(Text.literal(desc), wrapWidth)) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, line, x + 100, cy, -5592406);
         cy += 9 + 1;
      }

      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(hov ? "\u25b6  Click to select  \u25c0" : "Click to select").formatted(hov ? Formatting.WHITE : Formatting.DARK_GRAY),
         x + 100,
         y + 200 - 12,
         16777215
      );
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      } else {
         int totalW = 420;
         int cardY = this.height / 2 - 100;
         int normalX = this.width / 2 - totalW / 2;
         int rogueX = normalX + 200 + 20;
         if (isOver(mx, my, normalX, cardY, 200, 200)) {
            this.choose(false);
            TurnBattleSounds.playSelect();
            return true;
         } else if (isOver(mx, my, rogueX, cardY, 200, 200)) {
            this.choose(true);
            TurnBattleSounds.playSelect();
            return true;
         } else {
            return super.mouseClicked(mx, my, button);
         }
      }
   }

   private void choose(boolean roguelite) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(roguelite);
      Net121.sendToServer(TurnBattleMod.WORLD_TYPE_CHOSEN, buf);
      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }
}
