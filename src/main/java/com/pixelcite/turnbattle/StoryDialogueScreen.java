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

@Environment(EnvType.CLIENT)
public class StoryDialogueScreen extends Screen {
   private static final int TYPE_SPEED = 1;
   private static final int BOX_H = 70;
   private static final int BOX_PAD = 10;
   private static final int BOX_MARGIN = 20;
   private static final int NAME_H = 14;
   private final String[] rawBeats;
   private final boolean spawnSheepyAfter;
   private String currentSpeaker = "";
   private String currentText = "";
   private boolean isChoiceBeat = false;
   private String[] choiceOptions = new String[0];
   private int beatIndex = 0;
   private int charIndex = 0;
   private int typeTick = 0;
   private boolean arrowBlink = true;
   private int blinkTick = 0;
   private int hoveredChoice = -1;

   public StoryDialogueScreen(String[] beats, boolean spawnSheepyAfter) {
      super(Text.literal(""));
      this.rawBeats = beats;
      this.spawnSheepyAfter = spawnSheepyAfter;
      this.parseBeat(0);
   }

   private void parseBeat(int idx) {
      if (idx < this.rawBeats.length) {
         String raw = this.rawBeats[idx];
         if (raw.startsWith("CHOICE\u001f")) {
            this.isChoiceBeat = true;
            this.choiceOptions = raw.substring(7).split("\u001f", -1);
            this.currentSpeaker = "";
            this.currentText = "";
            this.charIndex = Integer.MAX_VALUE;
         } else {
            this.isChoiceBeat = false;
            int sep = raw.indexOf(31);
            if (sep >= 0) {
               this.currentSpeaker = raw.substring(0, sep);
               this.currentText = raw.substring(sep + 1);
            } else {
               this.currentSpeaker = "";
               this.currentText = raw;
            }
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void tick() {
      if (++this.blinkTick >= 15) {
         this.blinkTick = 0;
         this.arrowBlink = !this.arrowBlink;
      }

      if (!this.isChoiceBeat && this.charIndex < this.currentText.length() && ++this.typeTick >= 1) {
         this.typeTick = 0;
         this.charIndex++;
      }
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      if (this.beatIndex < this.rawBeats.length) {
         int boxX = 20;
         int boxW = this.width - 40;
         int boxY = this.height - 70 - 20;
         if (this.isChoiceBeat) {
            this.renderChoiceBeat(ctx, mx, my, boxX, boxW, boxY);
            super.render(ctx, mx, my, delta);
         } else {
            boolean hasSpeaker = !this.currentSpeaker.isEmpty();
            int dimTop = hasSpeaker ? boxY - 14 - 6 : boxY - 4;
            ctx.fill(0, dimTop, this.width, boxY + 70 + 6, -1442840576);
            if (hasSpeaker) {
               int spW = this.textRenderer.getWidth(this.currentSpeaker) + 10;
               ctx.fill(boxX - 1, boxY - 14 - 3, boxX + spW + 1, boxY - 1, -1);
               ctx.fill(boxX, boxY - 14 - 2, boxX + spW, boxY - 2, -15658735);
               ctx.drawTextWithShadow(
                  this.textRenderer, Text.literal(this.currentSpeaker).formatted(Formatting.WHITE), boxX + 5, boxY - 14 - 2 + (14 - 9) / 2, -1
               );
            }

            ctx.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + 70 + 2, -1);
            ctx.fill(boxX, boxY, boxX + boxW, boxY + 70, -16777216);
            String displayed = this.currentText.substring(0, Math.min(this.charIndex, this.currentText.length()));
            this.drawWrapped(ctx, displayed, boxX + 10, boxY + 10, boxW - 20, -1);
            if (this.charIndex >= this.currentText.length() && this.arrowBlink) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u25bc"), boxX + boxW - 12, boxY + 70 - 9 - 4, -1);
            }

            super.render(ctx, mx, my, delta);
         }
      }
   }

   private void renderChoiceBeat(DrawContext ctx, int mx, int my, int boxX, int boxW, int boxY) {
      ctx.fill(0, boxY - 4, this.width, boxY + 70 + 6, -1442840576);
      ctx.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + 70 + 2, -1);
      ctx.fill(boxX, boxY, boxX + boxW, boxY + 70, -16777216);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("* ..."), boxX + 10, boxY + 10, -7829368);
      int btnW = Math.min(300, boxW - 40);
      int btnH = 14;
      int gap = 6;
      int totalH = this.choiceOptions.length * btnH + (this.choiceOptions.length - 1) * gap;
      int startY = boxY + 35 - totalH / 2 + 6;
      this.hoveredChoice = -1;

      for (int i = 0; i < this.choiceOptions.length; i++) {
         int bx = this.width / 2 - btnW / 2;
         int by = startY + i * (btnH + gap);
         boolean hover = mx >= bx && mx < bx + btnW && my >= by && my < by + btnH;
         if (hover) {
            this.hoveredChoice = i;
         }

         ctx.fill(bx - 1, by - 1, bx + btnW + 1, by + btnH + 1, hover ? -1 : -7829368);
         ctx.fill(bx, by, bx + btnW, by + btnH, hover ? -14540220 : -15658735);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(this.choiceOptions[i]).formatted(hover ? Formatting.WHITE : Formatting.GRAY),
            bx + btnW / 2,
            by + (btnH - 9) / 2,
            -1
         );
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (btn != 0) {
         return false;
      } else if (this.isChoiceBeat) {
         if (this.hoveredChoice >= 0) {
            this.sendChoice(this.hoveredChoice);
            TurnBattleSounds.playSelect();
            return true;
         } else {
            return false;
         }
      } else {
         this.advance();
         TurnBattleSounds.playSelect();
         return true;
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.isChoiceBeat) {
         if (key == 49 && this.choiceOptions.length > 0) {
            this.sendChoice(0);
            return true;
         } else if (key == 50 && this.choiceOptions.length > 1) {
            this.sendChoice(1);
            return true;
         } else {
            return false;
         }
      } else if (key != 32 && key != 257 && key != 335) {
         return super.keyPressed(key, scan, mods);
      } else {
         this.advance();
         return true;
      }
   }

   private void sendChoice(int idx) {
      if (this.client != null) {
         this.client.setScreen(null);
      }

      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(idx);
      Net121.sendToServer(TurnBattleMod.STORY_CHOICE, buf);
   }

   private void advance() {
      if (this.beatIndex < this.rawBeats.length) {
         if (this.charIndex < this.currentText.length()) {
            this.charIndex = this.currentText.length();
         } else {
            this.beatIndex++;
            this.charIndex = 0;
            this.typeTick = 0;
            if (this.beatIndex < this.rawBeats.length) {
               this.parseBeat(this.beatIndex);
            } else {
               if (this.client != null) {
                  this.client.setScreen(null);
               }

               PacketByteBuf buf = PacketByteBufs.create();
               if (this.spawnSheepyAfter) {
                  buf.writeBoolean(true);
                  Net121.sendToServer(TurnBattleMod.STORY_CONTINUE, buf);
               } else {
                  Net121.sendToServer(TurnBattleMod.STORY_DIALOGUE_DONE, PacketByteBufs.empty());
               }
            }
         }
      }
   }

   private void drawWrapped(DrawContext ctx, String text, int x, int y, int maxW, int color) {
      if (!text.isEmpty()) {
         List lines = new ArrayList<>();
         String[] words = text.split(" ", -1);
         StringBuilder cur = new StringBuilder();

         for (String w : words) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (this.textRenderer.getWidth(test) > maxW && !cur.isEmpty()) {
               lines.add(cur.toString());
               cur = new StringBuilder(w);
            } else {
               cur = new StringBuilder(test);
            }
         }

         if (!cur.isEmpty()) {
            lines.add(cur.toString());
         }

         int lh = 9 + 2;

         for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(lines.get(i)), x, y + i * lh, color);
         }
      }
   }
}
