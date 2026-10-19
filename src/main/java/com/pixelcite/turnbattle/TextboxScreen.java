package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class TextboxScreen extends Screen {
   private static final Identifier TEXT_BOX = Identifier.of("turnbattle", "textures/gui/deltarune_text_box.png");
   private static final int TEX_W = 593;
   private static final int TEX_H = 167;
   private static final float GAME_FRAC = 0.9265625F;
   private static final float STAR_X = 0.0455312F;
   private static final float TEXT_X = 0.11467116F;
   private static final float RIGHT_PAD = 0.043844856F;
   private static final float TEXT_Y = 0.17964073F;
   private static final float LINE_H = 0.19161677F;
   private static final float GLYPH_H = 0.1017964F;
   private Runnable onDone;
   private boolean doneFired;
   private static final int CHARS_PER_TICK = 2;
   private static final int PUNCT_PAUSE_TICKS = 8;
   private String[] pages;
   private static final int MAX_LINES = 3;
   private boolean paginated;
   private final boolean playSound;
   private int pageIdx = 0;
   private int visibleIdx = 0;
   private int visibleLen = 0;
   private int pauseTicks = 0;
   private String[] options;
   private IntConsumer onChoice;
   private int optSel = 0;
   private int choice = -1;
   private boolean choiceFired;

   public TextboxScreen(String[] pages, boolean sound) {
      super(Text.literal(""));
      this.pages = pages != null && pages.length > 0 ? pages : new String[]{""};
      this.playSound = sound;
      this.visibleLen = countVisible(this.pages[0]);
   }

   public TextboxScreen(String[] pages, boolean sound, Runnable onDone) {
      this(pages, sound);
      this.onDone = onDone;
   }

   public TextboxScreen(String[] pages, boolean sound, String[] options, IntConsumer onChoice) {
      this(pages, sound);
      this.options = options != null && options.length > 0 ? options : null;
      this.onChoice = onChoice;
   }

   private boolean choosing() {
      return this.options != null && this.pageIdx == this.pages.length - 1 && this.visibleIdx >= this.visibleLen;
   }

   private void choose(int i) {
      this.choice = i;
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.MENU_SELECT, 1.0F));
      }

      this.close();
   }

   protected void init() {
      super.init();
      if (!this.paginated) {
         this.paginated = true;
         float textScale = this.textScale();
         int maxW = this.maxTextWidth(textScale);
         List out = new ArrayList<>();

         for (int pi = 0; pi < this.pages.length; pi++) {
            String page = this.pages[pi];
            List lines = this.wrapLines(page, maxW);
            boolean lastWithChoices = this.options != null && pi == this.pages.length - 1;
            if (lines.size() > 3 || lastWithChoices && lines.size() == 3) {
               List chunks = new ArrayList<>();

               for (int i = 0; i < lines.size(); i += 3) {
                  chunks.add(new ArrayList<>(lines.subList(i, Math.min(lines.size(), i + 3))));
               }

               if (lastWithChoices && chunks.get(chunks.size() - 1).size() == 3) {
                  List tail = chunks.get(chunks.size() - 1);
                  List moved = new ArrayList<>(List.of(tail.remove(tail.size() - 1)));
                  chunks.add(moved);
               }

               String carry = "";

               for (List c : chunks) {
                  String chunk = carry + String.join("\n", c);
                  out.add(chunk);
                  carry = activeCodes(chunk);
               }
            } else {
               out.add(page);
            }
         }

         if (out.size() != this.pages.length) {
            this.pages = out.toArray(new String[0]);
            this.pageIdx = 0;
            this.visibleIdx = 0;
            this.pauseTicks = 0;
            this.visibleLen = countVisible(this.pages[0]);
         }
      }
   }

   private static String activeCodes(String s) {
      StringBuilder codes = new StringBuilder();

      for (int i = 0; i + 1 < s.length(); i++) {
         if (s.charAt(i) == 167) {
            char c = Character.toLowerCase(s.charAt(i + 1));
            if (c == 'r') {
               codes.setLength(0);
            } else if ("0123456789abcdef".indexOf(c) >= 0) {
               codes.setLength(0);
               codes.append('\u00a7').append(c);
            } else {
               codes.append('\u00a7').append(c);
            }

            i++;
         }
      }

      return codes.toString();
   }

   private int boxW() {
      float gameW = Math.min((float)this.width, (float)this.height * 4.0F / 3.0F);
      return Math.max(160, Math.round(gameW * 0.9265625F));
   }

   private int boxH() {
      return Math.round((float)this.boxW() * 167.0F / 593.0F);
   }

   private float textScale() {
      return (float)this.boxH() * 0.1017964F / 7.0F * 1.05F;
   }

   private int maxTextWidth(float textScale) {
      int boxW = this.boxW();
      int boxX = (this.width - boxW) / 2;
      int textX = boxX + Math.round((float)boxW * 0.11467116F);
      return (int)((float)(boxX + boxW - Math.round((float)boxW * 0.043844856F) - textX) / textScale);
   }

   public void removed() {
      if (this.onChoice != null && !this.choiceFired) {
         this.choiceFired = true;
         int c = this.choice;
         if (this.client != null) {
            this.client.execute(() -> this.onChoice.accept(c));
         } else {
            this.onChoice.accept(c);
         }
      }

      if (this.onDone != null && !this.doneFired) {
         this.doneFired = true;
         Runnable r = this.onDone;
         if (this.client != null) {
            this.client.execute(r);
         } else {
            r.run();
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
   }

   private static int countVisible(String s) {
      int count = 0;

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == 167 && i + 1 < s.length()) {
            i++;
         } else if (c != '\n') {
            count++;
         }
      }

      return count;
   }

   private static String visibleSubstring(String s, int visibleCount) {
      StringBuilder out = new StringBuilder();
      int seen = 0;

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == 167 && i + 1 < s.length()) {
            out.append(c).append(s.charAt(i + 1));
            i++;
         } else if (c == '\n') {
            out.append(c);
         } else {
            if (seen >= visibleCount) {
               break;
            }

            out.append(c);
            seen++;
         }
      }

      return out.toString();
   }

   private static char visibleCharAt(String s, int visibleIndex) {
      int seen = 0;

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == 167 && i + 1 < s.length()) {
            i++;
         } else if (c != '\n') {
            if (seen == visibleIndex) {
               return c;
            }

            seen++;
         }
      }

      return '\u0000';
   }

   public void tick() {
      String current = this.pages[this.pageIdx];
      if (this.visibleIdx < this.visibleLen) {
         if (this.pauseTicks > 0) {
            this.pauseTicks--;
         } else {
            int advanced = 0;
            boolean playedThisTick = false;

            while (advanced < 2 && this.visibleIdx < this.visibleLen) {
               char revealed = visibleCharAt(current, this.visibleIdx);
               this.visibleIdx++;
               advanced++;
               if (this.playSound && !playedThisTick && this.client != null) {
                  this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SND_TEXT, 1.0F, 1.0F));
                  playedThisTick = true;
               }

               if (revealed == '.' || revealed == ',' || revealed == '?' || revealed == '!') {
                  this.pauseTicks = 8;
                  break;
               }
            }
         }
      }
   }

   private void advance() {
      if (this.choosing()) {
         this.choose(this.optSel);
      } else if (this.visibleIdx < this.visibleLen) {
         this.visibleIdx = this.visibleLen;
         this.pauseTicks = 0;
      } else if (this.pageIdx + 1 < this.pages.length) {
         this.pageIdx++;
         this.visibleIdx = 0;
         this.pauseTicks = 0;
         this.visibleLen = countVisible(this.pages[this.pageIdx]);
      } else {
         this.close();
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      this.advance();
      return true;
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.options != null && this.choosing()) {
         if (key == 263 || key == 262 || key == 65 || key == 68) {
            this.optSel = Math.floorMod(this.optSel + (key != 263 && key != 65 ? 1 : -1), this.options.length);
            if (this.client != null) {
               this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.MENU_MOVE, 1.0F));
            }

            return true;
         }

         if (key == 88 || key == 256) {
            this.choose(this.options.length - 1);
            return true;
         }
      }

      if (key != 32 && key != 257 && key != 335 && (this.options == null || key != 90)) {
         return super.keyPressed(key, scan, mods);
      } else {
         this.advance();
         return true;
      }
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      int screenW = this.width;
      int screenH = this.height;
      float gameW = Math.min((float)screenW, (float)screenH * 4.0F / 3.0F);
      int boxW = Math.max(160, Math.round(gameW * 0.9265625F));
      int boxH = Math.round((float)boxW * 167.0F / 593.0F);
      int boxX = (screenW - boxW) / 2;
      int boxY = screenH - boxH - Math.max(2, Math.round((float)screenH * 0.012F));
      float boxScale = (float)boxW / 593.0F;
      MatrixStack matrices = ctx.getMatrices();
      matrices.push();
      matrices.translate((float)boxX, (float)boxY, 0.0F);
      matrices.scale(boxScale, boxScale, 1.0F);
      ctx.drawTexture(TEXT_BOX, 0, 0, 0.0F, 0.0F, 593, 167, 593, 167);
      matrices.pop();
      float textScale = (float)boxH * 0.1017964F / 7.0F * 1.05F;
      int starX = boxX + Math.round((float)boxW * 0.0455312F);
      int textX = boxX + Math.round((float)boxW * 0.11467116F);
      int textY = boxY + Math.round((float)boxH * 0.17964073F);
      int lineH = Math.round((float)boxH * 0.19161677F);
      int maxW = (int)((float)(boxX + boxW - Math.round((float)boxW * 0.043844856F) - textX) / textScale);
      this.drawScaled(ctx, "*", starX, textY, -1, textScale);
      String visible = visibleSubstring(this.pages[this.pageIdx], this.visibleIdx);
      this.drawWrapped(ctx, visible, textX, textY, maxW, -1, textScale, lineH);
      if (this.choosing()) {
         int oy = textY + 2 * lineH;
         int soulSize = Math.round(8.0F * textScale);

         for (int i = 0; i < this.options.length; i++) {
            int ox = textX + Math.round((float)boxW * 0.34F * (float)i);
            boolean sel = i == this.optSel;
            if (sel) {
               SoulType soul = TurnBattleClientMod.clientSoul;
               Identifier tex = soul != null ? soul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
               ctx.drawTexture(tex, ox - soulSize - Math.round(3.0F * textScale), oy + Math.round(textScale * 0.5F), soulSize, soulSize, 0.0F, 0.0F, 9, 9, 9, 9);
            }

            this.drawScaled(ctx, this.options[i], ox, oy, sel ? -256 : -1, textScale);
         }
      }

      super.render(ctx, mx, my, delta);
   }

   private void drawScaled(DrawContext ctx, String text, int x, int y, int color, float scale) {
      MatrixStack matrices = ctx.getMatrices();
      matrices.push();
      matrices.translate((float)x, (float)y, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      ctx.drawTextWithShadow(this.textRenderer, text, 0, 0, color);
      matrices.pop();
   }

   private void drawWrapped(DrawContext ctx, String text, int x, int y, int maxW, int color, float scale, int lh) {
      if (!text.isEmpty()) {
         List lines = this.wrapLines(text, maxW);

         for (int i = 0; i < lines.size(); i++) {
            this.drawScaled(ctx, lines.get(i), x, y + i * lh, color, scale);
         }
      }
   }

   private List<String> wrapLines(String text, int maxW) {
      List lines = new ArrayList<>();

      for (String segment : text.split("\n", -1)) {
         StringBuilder cur = new StringBuilder();

         for (String w : segment.split(" ", -1)) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (this.textRenderer.getWidth(test) > maxW && !cur.isEmpty()) {
               lines.add(cur.toString());
               cur = new StringBuilder(w);
            } else {
               cur = new StringBuilder(test);
            }
         }

         lines.add(cur.toString());
      }

      return lines;
   }
}
