package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class StoryChapterScreen extends Screen {
   public static int unlockedChapter = 0;
   public static int questIndex = 0;
   public static boolean questStarted = false;
   public static boolean questComplete = false;
   public static final boolean[] chapterDone = new boolean[5];
   private static final int PW = 320;
   private static final int PH = 240;
   private static final int PAD = 12;
   private static final int CH_CARD_H = 32;
   private static final int CH_CARD_GAP = 6;
   private int view = 0;
   private int selectedChapter = -1;
   private boolean hoverBegin = false;
   private boolean hoverContinue = false;
   private boolean hoverBack = false;

   public StoryChapterScreen() {
      super(Text.literal(""));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.fillGradient(0, 0, this.width, this.height, -2013265920, -1442840576);
      int px = this.width / 2 - 160;
      int py = this.height / 2 - 120;
      ctx.fill(px - 1, py - 1, px + 320 + 1, py + 240 + 1, -7829368);
      ctx.fill(px, py, px + 320, py + 240, -16448240);
      ctx.fill(px, py, px + 320, py + 14, -15921894);
      ctx.fill(px, py + 13, px + 320, py + 14, -12303258);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("STORY").formatted(Formatting.GOLD), px + 160, py + 3, -8892);
      if (this.view == 0) {
         this.renderChapterList(ctx, px, py, mx, my);
      } else {
         this.renderQuestDetail(ctx, px, py, mx, my);
      }

      super.render(ctx, mx, my, delta);
   }

   private void renderChapterList(DrawContext ctx, int px, int py, int mx, int my) {
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select a Chapter").formatted(Formatting.GRAY), px + 160, py + 20, -7829368);
      int cardX = px + 12;
      int cardW = 296;
      int y = py + 34;

      for (int i = 0; i < 5; i++) {
         boolean unlocked = i <= unlockedChapter;
         boolean done = chapterDone[i];
         boolean hover = unlocked && mx >= cardX && mx < cardX + cardW && my >= y && my < y + 32;
         int bg = hover ? -15066556 : (unlocked ? -15921878 : -16119276);
         int border = done ? -12277180 : (unlocked ? -12303190 : -13421773);
         ctx.fill(cardX - 1, y - 1, cardX + cardW + 1, y + 32 + 1, border);
         ctx.fill(cardX, y, cardX + cardW, y + 32, bg);
         ctx.fill(cardX, y, cardX + 3, y + 32, border);
         String name = StoryManager.CHAPTER_NAMES[i];
         int nameColor = done ? -12255420 : (unlocked ? -3355393 : -12303275);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), cardX + 8, y + 6, nameColor);
         String tag = done ? "\u2713 Complete" : (unlocked ? (i == unlockedChapter ? "In Progress" : "Unlocked") : "Locked");
         int tagColor = done ? -12277180 : (unlocked ? -7829300 : -13421739);
         int tagW = this.textRenderer.getWidth(tag);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(tag), cardX + cardW - tagW - 8, y + 6, tagColor);
         if (unlocked && !done && i == unlockedChapter && questStarted) {
            String qName = StoryManager.QUEST_NAMES[i][questIndex];
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("Quest: " + qName).formatted(Formatting.GRAY), cardX + 8, y + 18, -10066296);
         }

         y += 38;
      }
   }

   private void renderQuestDetail(DrawContext ctx, int px, int py, int mx, int my) {
      int ch = this.selectedChapter;
      boolean unlocked = ch <= unlockedChapter;
      boolean done = chapterDone[ch];
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(StoryManager.CHAPTER_NAMES[ch]).formatted(Formatting.GOLD), px + 160, py + 22, -8892);
      int contentY = py + 38;
      if (done) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Chapter Complete!").formatted(Formatting.GREEN), px + 160, contentY + 20, -12255420);
      } else if (!unlocked) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("This chapter is not yet available.").formatted(Formatting.DARK_GRAY), px + 160, contentY + 20, -12303275
         );
      } else {
         int qi = ch == unlockedChapter ? questIndex : 0;
         String qName = StoryManager.QUEST_NAMES[ch][qi];
         String qDesc = StoryManager.QUEST_DESCS[ch][qi];
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Quest: ").formatted(Formatting.GRAY).append(Text.literal(qName).formatted(Formatting.WHITE)),
            px + 12,
            contentY,
            -1
         );
         contentY += 9 + 6;
         String[] words = qDesc.split(" ");
         StringBuilder line = new StringBuilder();
         int maxW = 296;

         for (String w : words) {
            String test = line.isEmpty() ? w : line + " " + w;
            if (this.textRenderer.getWidth(test) > maxW) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(line.toString()), px + 12, contentY, -7829351);
               contentY += 9 + 2;
               line = new StringBuilder(w);
            } else {
               line = new StringBuilder(test);
            }
         }

         if (!line.isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(line.toString()), px + 12, contentY, -7829351);
            contentY += 9 + 10;
         }

         int barW = 296;
         int barH = 8;
         float frac = questComplete ? 1.0F : (questStarted ? 0.5F : 0.0F);
         ctx.fill(px + 12, contentY, px + 12 + barW, contentY + barH, -15066582);
         if (frac > 0.0F) {
            ctx.fill(px + 12, contentY, px + 12 + (int)((float)barW * frac), contentY + barH, -12303156);
         }

         String pctText = questComplete ? "Complete!" : (questStarted ? "In Progress" : "Not Started");
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(pctText).formatted(Formatting.GRAY), px + 160, contentY + barH + 4, -10066296);
         contentY += barH + 18;
         if (!questStarted) {
            int btnW = 100;
            int btnH = 14;
            int btnX = px + 160 - btnW / 2;
            this.hoverBegin = mx >= btnX && mx < btnX + btnW && my >= contentY && my < contentY + btnH;
            ctx.fill(btnX - 1, contentY - 1, btnX + btnW + 1, contentY + btnH + 1, this.hoverBegin ? -7829249 : -12303224);
            ctx.fill(btnX, contentY, btnX + btnW, contentY + btnH, -16448224);
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("Begin Chapter").formatted(this.hoverBegin ? Formatting.WHITE : Formatting.GRAY),
               btnX + btnW / 2,
               contentY + 3,
               -7829249
            );
         } else if (questComplete) {
            int btnW = 80;
            int btnH = 14;
            int btnX = px + 160 - btnW / 2;
            this.hoverContinue = mx >= btnX && mx < btnX + btnW && my >= contentY && my < contentY + btnH;
            ctx.fill(btnX - 1, contentY - 1, btnX + btnW + 1, contentY + btnH + 1, this.hoverContinue ? -12255420 : -14531550);
            ctx.fill(btnX, contentY, btnX + btnW, contentY + btnH, -16445435);
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("Continue").formatted(this.hoverContinue ? Formatting.WHITE : Formatting.GRAY),
               btnX + btnW / 2,
               contentY + 3,
               -12255420
            );
         } else {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("Complete the quest objective to continue.").formatted(Formatting.DARK_GRAY), px + 160, contentY, -12303275
            );
         }
      }

      int backY = py + 240 - 20;
      this.hoverBack = mx >= px + 12 && mx < px + 12 + 40 && my >= backY && my < backY + 12;
      ctx.fill(px + 12 - 1, backY - 1, px + 12 + 41, backY + 13, this.hoverBack ? -10066296 : -13421756);
      ctx.fill(px + 12, backY, px + 12 + 40, backY + 12, -16119270);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("\u2190 Back").formatted(this.hoverBack ? Formatting.WHITE : Formatting.GRAY), px + 12 + 20, backY + 2, -7829351
      );
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (btn != 0) {
         return super.mouseClicked(mx, my, btn);
      } else {
         int px = this.width / 2 - 160;
         int py = this.height / 2 - 120;
         if (this.view == 0) {
            int cardX = px + 12;
            int cardW = 296;
            int y = py + 34;

            for (int i = 0; i < 5; i++) {
               if (i <= unlockedChapter && mx >= (double)cardX && mx < (double)(cardX + cardW) && my >= (double)y && my < (double)(y + 32)) {
                  this.selectedChapter = i;
                  this.view = 1;
                  TurnBattleSounds.playSelect();
                  return true;
               }

               y += 38;
            }
         } else {
            int backY = py + 240 - 20;
            if (mx >= (double)(px + 12) && mx < (double)(px + 12 + 40) && my >= (double)backY && my < (double)(backY + 12)) {
               this.view = 0;
               TurnBattleSounds.playSelect();
               return true;
            }

            if (this.hoverBegin) {
               PacketByteBuf b = PacketByteBufs.create();
               b.writeInt(this.selectedChapter);
               Net121.sendToServer(TurnBattleMod.STORY_BEGIN, b);
               this.view = 0;
               TurnBattleSounds.playSelect();
               return true;
            }

            if (this.hoverContinue) {
               PacketByteBuf b = PacketByteBufs.create();
               b.writeBoolean(false);
               Net121.sendToServer(TurnBattleMod.STORY_CONTINUE, b);
               if (this.client != null) {
                  this.client.setScreen(null);
               }

               TurnBattleSounds.playSelect();
               return true;
            }
         }

         return super.mouseClicked(mx, my, btn);
      }
   }
}
