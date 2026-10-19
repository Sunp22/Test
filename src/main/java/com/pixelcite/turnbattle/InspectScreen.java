package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class InspectScreen extends Screen {
   private final String speaker;
   private final String[] lines;
   private int beatIndex = 0;
   private int charIndex = 0;
   private int typeTick = 0;
   private static final int TYPE_SPEED = 1;
   private boolean arrowBlink = true;
   private int blinkTick = 0;
   private static final int BOX_H = 60;
   private static final int BOX_PAD = 10;
   private static final int BOX_MARGIN = 20;
   private static final int NAME_PLATE_H = 14;
   private static final int SPEAKER_PAD = 5;
   private final Runnable onClose;

   public InspectScreen(InspectRegistry.Entry entry) {
      super(Text.literal(""));
      this.speaker = entry.speaker();
      this.lines = entry.lines();
      this.onClose = null;
   }

   public InspectScreen(String[] lines, Runnable onClose) {
      super(Text.literal(""));
      this.speaker = "";
      this.lines = lines;
      this.onClose = onClose;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void tick() {
      this.blinkTick++;
      if (this.blinkTick >= 15) {
         this.blinkTick = 0;
         this.arrowBlink = !this.arrowBlink;
      }

      if (this.beatIndex < this.lines.length) {
         String full = this.lines[this.beatIndex];
         if (this.charIndex < full.length()) {
            this.typeTick++;
            if (this.typeTick >= 1) {
               this.typeTick = 0;
               this.charIndex++;
            }
         }
      }
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      if (this.beatIndex < this.lines.length) {
         int boxX = 20;
         int boxW = this.width - 40;
         int boxY = this.height - 60 - 20;
         boolean hasSpeaker = this.speaker != null && !this.speaker.isEmpty();
         int dimTop = hasSpeaker ? boxY - 14 - 6 : boxY - 4;
         ctx.fill(0, dimTop, this.width, boxY + 60 + 6, -1728053248);
         if (hasSpeaker) {
            int speakerW = this.textRenderer.getWidth(this.speaker) + 10;
            ctx.fill(boxX - 1, boxY - 14 - 3, boxX + speakerW + 1, boxY - 1, -1);
            ctx.fill(boxX, boxY - 14 - 2, boxX + speakerW, boxY - 2, -15658735);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(this.speaker).formatted(Formatting.WHITE), boxX + 5, boxY - 14 - 2 + (14 - 9) / 2, -1);
         }

         ctx.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + 60 + 2, -1);
         ctx.fill(boxX, boxY, boxX + boxW, boxY + 60, -15658735);
         String full = this.lines[this.beatIndex];
         String visible = full.substring(0, Math.min(this.charIndex, full.length()));
         this.drawWrappedText(ctx, visible, boxX + 10, boxY + 10, boxW - 20, -1);
         boolean fullyTyped = this.charIndex >= full.length();
         if (fullyTyped) {
            boolean isLast = this.beatIndex >= this.lines.length - 1;
            String arrow = isLast ? "\u25bc [CLOSE]" : "\u25bc";
            if (this.arrowBlink) {
               ctx.drawTextWithShadow(
                  this.textRenderer,
                  Text.literal(arrow).formatted(Formatting.WHITE),
                  boxX + boxW - 10 - this.textRenderer.getWidth(arrow),
                  boxY + 60 - 9 - 10,
                  -1
               );
            }
         }

         if (this.lines.length > 1) {
            String pg = this.beatIndex + 1 + "/" + this.lines.length;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(pg).formatted(Formatting.DARK_GRAY), boxX + 10, boxY + 60 - 9 - 10, -10066330);
         }

         super.render(ctx, mx, my, delta);
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      this.advance();
      TurnBattleSounds.playSelect();
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 32 && keyCode != 257 && keyCode != 335) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.advance();
         return true;
      }
   }

   private void advance() {
      if (this.beatIndex < this.lines.length) {
         String full = this.lines[this.beatIndex];
         if (this.charIndex < full.length()) {
            this.charIndex = full.length();
         } else {
            this.beatIndex++;
            this.charIndex = 0;
            this.typeTick = 0;
            if (this.beatIndex >= this.lines.length) {
               if (this.client != null) {
                  this.client.setScreen(null);
               }

               if (this.onClose != null) {
                  this.onClose.run();
               }
            }
         }
      }
   }

   private void drawWrappedText(DrawContext ctx, String text, int x, int y, int maxW, int color) {
      if (!text.isEmpty()) {
         List result = new ArrayList<>();
         String[] words = text.split(" ");
         StringBuilder line = new StringBuilder();

         for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (this.textRenderer.getWidth(test) > maxW) {
               if (!line.isEmpty()) {
                  result.add(line.toString());
               }

               line = new StringBuilder(word);
            } else {
               line = new StringBuilder(test);
            }
         }

         if (!line.isEmpty()) {
            result.add(line.toString());
         }

         int lineH = 9 + 3;

         for (int i = 0; i < result.size(); i++) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(result.get(i)), x, y + i * lineH, color);
         }
      }
   }
}
