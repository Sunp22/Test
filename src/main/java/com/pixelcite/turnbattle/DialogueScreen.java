package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DialogueScreen extends Screen {
   private static final String[] RAW_SCRIPT_NO_STORY = new String[]{"NAME_INPUT\u001f", "SOUL_REVEAL\u001f", "TITLECARD\u001f"};
   private static final String[] RAW_SCRIPT = new String[]{
      "\u001fOnce upon a time,",
      "\u001fa LEGEND was whispered among shadows.",
      "\u001fFour heroes born of unique light would meet in the thunderous skyline.",
      "\u001fAtop the darkened peaks of jagged stone, the Heroes would face off against the culmination of that which plagued the world.",
      "\u001fAs goes the LEGEND, the Heroes shall shine their SOULs upon the culmination of DESPAIR. Eradicating it and causing it to disappear into the moonlight.",
      "\u001fThey failed.",
      "\u001fThus, putting an end to this tale.",
      "\u001fSince then, the creature has slept amongst the clouds, one day to awaken, stronger against the DESPAIR the world had gained since that day. There is no hope. No distant future. Just the current time that remains, however long that may be.",
      "SOUL_REVEAL\u001f",
      "\u001fDo not fall to DESPAIR. Do not forget who you are.",
      "NAME_INPUT\u001f",
      "TITLECARD\u001f"
   };
   private int beatIndex = 0;
   private int charIndex = 0;
   private int typeTick = 0;
   private static final int TYPE_SPEED = 2;
   private boolean arrowBlink = true;
   private int blinkTick = 0;
   private String vesselName = null;
   private SoulType rolledSoul = null;
   private boolean soulShown = false;
   private boolean waitingForName = false;
   private TextFieldWidget nameField;
   private boolean inSoulReveal = false;
   private int soulRevealTick = 0;
   private static final int FLASH_TICKS = 15;
   private static final int REVEAL_TICKS = 80;
   private boolean inTitlecard = false;
   private int titleTick = 0;
   private static final int TITLE_FADE_IN = 30;
   private static final int TITLE_HOLD = 40;
   private static final int TITLE_FADE_OUT = 30;
   private static final int TITLE_TOTAL = 100;
   private SoundInstance musicInstance = null;
   private static final int BOX_H = 80;
   private static final int BOX_PAD = 10;
   private static final int BOX_MARGIN = 24;
   private static final int NAME_H = 14;
   private final String[] activeScript = TurnBattleClientMod.storyModeEnabled ? RAW_SCRIPT : RAW_SCRIPT_NO_STORY;

   public DialogueScreen() {
      super(Text.literal(""));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected void init() {
      int boxX = 24;
      int boxW = this.width - 48;
      int boxY = this.height - 80 - 24;
      this.nameField = new TextFieldWidget(this.textRenderer, boxX + 10, boxY + 10 + 20, boxW - 20, 14, Text.literal(""));
      this.nameField.setMaxLength(24);
      this.nameField.setVisible(false);
      this.nameField.setFocused(false);
      this.nameField.setPlaceholder(Text.literal("Enter name...").formatted(Formatting.DARK_GRAY));
      this.addDrawableChild(this.nameField);
      if (this.rolledSoul == null && TurnBattleClientMod.clientSoul != null) {
         this.rolledSoul = TurnBattleClientMod.clientSoul;
      }

      if (this.beatIndex < this.activeScript.length && this.activeScript[this.beatIndex].startsWith("NAME_INPUT")) {
         this.nameField.setVisible(true);
         this.nameField.setFocused(true);
      }
   }

   public void tick() {
      if (++this.blinkTick >= 15) {
         this.blinkTick = 0;
         this.arrowBlink = !this.arrowBlink;
      }

      if (this.inSoulReveal) {
         this.soulRevealTick++;
         if (this.soulRevealTick >= 80) {
            this.inSoulReveal = false;
            this.advanceBeat();
         }
      } else if (this.inTitlecard) {
         this.titleTick++;
         if (this.titleTick >= 100) {
            this.finishIntro();
         }
      } else {
         if (this.beatIndex < this.activeScript.length) {
            String text = this.beatText(this.beatIndex);
            if (this.charIndex < text.length() && ++this.typeTick >= 2) {
               this.typeTick = 0;
               this.charIndex++;
            }
         }
      }
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, -16777216);
      if (this.inSoulReveal) {
         this.renderSoulReveal(ctx);
      } else if (this.inTitlecard) {
         this.renderTitlecard(ctx);
      } else if (this.beatIndex < this.activeScript.length) {
         String raw = this.activeScript[this.beatIndex];
         if (!raw.startsWith("SOUL_REVEAL") && !raw.startsWith("TITLECARD") && !raw.startsWith("NAME_INPUT")) {
            int sep = raw.indexOf(31);
            String speaker = sep >= 0 ? raw.substring(0, sep) : "???";
            String text = sep >= 0 ? raw.substring(sep + 1) : raw;
            if (this.vesselName != null) {
               text = text.replace("{name}", this.vesselName);
            }

            this.renderDialogueBox(ctx, speaker, text);
            this.drawSoulIfReady(ctx);
            super.render(ctx, mx, my, delta);
         } else {
            if (raw.startsWith("NAME_INPUT") && this.nameField != null && this.nameField.isVisible()) {
               this.renderNameBox(ctx);
            }

            this.drawSoulIfReady(ctx);
            super.render(ctx, mx, my, delta);
         }
      }
   }

   private void renderDialogueBox(DrawContext ctx, String speaker, String text) {
      int boxX = 24;
      int boxW = this.width - 48;
      int boxY = this.height - 80 - 24;
      if (!speaker.isEmpty()) {
         int spW = this.textRenderer.getWidth(speaker) + 10;
         ctx.fill(boxX - 1, boxY - 14 - 3, boxX + spW + 1, boxY - 1, -1);
         ctx.fill(boxX, boxY - 14 - 2, boxX + spW, boxY - 2, -16777216);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(speaker).formatted(Formatting.WHITE), boxX + 5, boxY - 14 - 2 + (14 - 9) / 2, -1);
      }

      ctx.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + 80 + 2, -1);
      ctx.fill(boxX, boxY, boxX + boxW, boxY + 80, -16777216);
      String vis = text.substring(0, Math.min(this.charIndex, text.length()));
      this.drawWrapped(ctx, vis, boxX + 10, boxY + 10, boxW - 20, -1);
      if (this.charIndex >= text.length() && this.arrowBlink) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u25bc"), boxX + boxW - 10 - 6, boxY + 80 - 9 - 10, -1);
      }
   }

   private void renderNameBox(DrawContext ctx) {
      int boxX = 24;
      int boxW = this.width - 48;
      int boxY = this.height - 80 - 24;
      String speaker = "???";
      String prompt = "Name your vessel.";
      int spW = this.textRenderer.getWidth(speaker) + 10;
      ctx.fill(boxX - 1, boxY - 14 - 3, boxX + spW + 1, boxY - 1, -1);
      ctx.fill(boxX, boxY - 14 - 2, boxX + spW, boxY - 2, -16777216);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(speaker).formatted(Formatting.WHITE), boxX + 5, boxY - 14 - 2 + (14 - 9) / 2, -1);
      ctx.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + 80 + 2, -1);
      ctx.fill(boxX, boxY, boxX + boxW, boxY + 80, -16777216);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(prompt).formatted(Formatting.GRAY), boxX + 10, boxY + 10, -5592406);
      if (!this.waitingForName) {
         String hint = "[ PRESS ENTER TO CONFIRM ]";
         int hw = this.textRenderer.getWidth(hint);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(hint).formatted(Formatting.GRAY), boxX + boxW - hw - 10, boxY + 80 - 9 - 10, -10066330);
      }
   }

   private void renderSoulReveal(DrawContext ctx) {
      if (this.rolledSoul != null) {
         this.drawSoulIfReady(ctx);
      }
   }

   private void renderTitlecard(DrawContext ctx) {
      float alpha;
      if (this.titleTick < 30) {
         alpha = (float)this.titleTick / 30.0F;
      } else if (this.titleTick < 70) {
         alpha = 1.0F;
      } else {
         float t = (float)(this.titleTick - 30 - 40) / 30.0F;
         alpha = 1.0F - t;
      }

      int a = Math.max(0, Math.min(255, (int)(alpha * 255.0F)));
      Identifier logo = Identifier.of("turnbattle", "textures/gui/soulboundlogo2.png");
      int logoW = Math.min(this.width - 40, 600);
      int logoH = (int)((float)logoW * 225.0F / 719.0F);
      int lx = this.width / 2 - logoW / 2;
      int ly = this.height / 2 - logoH / 2;
      float drawAlpha = this.titleTick < 70 ? 1.0F : alpha;
      int da = Math.max(0, Math.min(255, (int)(drawAlpha * 255.0F)));
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)da / 255.0F);
      ctx.drawTexture(logo, lx, ly, 0.0F, 0.0F, logoW, logoH, logoW, logoH);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (this.inSoulReveal || this.inTitlecard) {
         return false;
      } else if (this.beatIndex >= this.activeScript.length) {
         return false;
      } else {
         String raw = this.activeScript[this.beatIndex];
         if (!raw.startsWith("SOUL_REVEAL") && !raw.startsWith("TITLECARD") && !raw.startsWith("NAME_INPUT")) {
            int sep = raw.indexOf(31);
            String text = sep >= 0 ? raw.substring(sep + 1) : raw;
            if (this.vesselName != null) {
               text = text.replace("{name}", this.vesselName);
            }

            if (this.charIndex < text.length()) {
               this.charIndex = text.length();
               TurnBattleSounds.playSelect();
               return true;
            } else {
               this.advanceBeat();
               TurnBattleSounds.playSelect();
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.inSoulReveal || this.inTitlecard) {
         return false;
      } else if (this.beatIndex >= this.activeScript.length) {
         return false;
      } else {
         String raw = this.activeScript[this.beatIndex];
         if (raw.startsWith("NAME_INPUT")) {
            if (key == 257 || key == 335) {
               this.submitName();
               return true;
            } else {
               return this.nameField != null && this.nameField.isVisible() ? this.nameField.keyPressed(key, scan, mods) : false;
            }
         } else if (!raw.startsWith("SOUL_REVEAL") && !raw.startsWith("TITLECARD")) {
            int sep = raw.indexOf(31);
            String text = sep >= 0 ? raw.substring(sep + 1) : raw;
            if (this.vesselName != null) {
               text = text.replace("{name}", this.vesselName);
            }

            if (this.charIndex < text.length()) {
               this.charIndex = text.length();
               return true;
            } else if (key != 32 && key != 257 && key != 335) {
               return super.keyPressed(key, scan, mods);
            } else {
               this.advanceBeat();
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public boolean charTyped(char c, int mods) {
      return this.beatIndex < this.activeScript.length
            && this.activeScript[this.beatIndex].startsWith("NAME_INPUT")
            && !this.waitingForName
            && this.nameField != null
            && this.nameField.isVisible()
         ? this.nameField.charTyped(c, mods)
         : super.charTyped(c, mods);
   }

   private void advanceBeat() {
      this.beatIndex++;
      this.charIndex = 0;
      this.typeTick = 0;
      if (this.beatIndex >= this.activeScript.length) {
         this.finishIntro();
      } else {
         String raw = this.activeScript[this.beatIndex];
         if (raw.startsWith("NAME_INPUT")) {
            if (this.nameField != null) {
               this.nameField.setVisible(true);
               this.nameField.setFocused(true);
            }
         } else if (raw.startsWith("SOUL_REVEAL")) {
            if (this.rolledSoul != null) {
               this.startSoulReveal();
            }
         } else if (raw.startsWith("TITLECARD")) {
            this.startTitlecard();
         }
      }
   }

   private String beatText(int idx) {
      String raw = this.activeScript[idx];
      int sep = raw.indexOf(31);
      String t = sep >= 0 ? raw.substring(sep + 1) : raw;
      if (this.vesselName != null) {
         t = t.replace("{name}", this.vesselName);
      }

      return t;
   }

   private void submitName() {
      if (this.nameField != null) {
         String name = this.nameField.getText().trim();
         if (!name.isEmpty()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(name, 32);
            Net121.sendToServer(TurnBattleMod.VESSEL_NAME, buf);
            this.waitingForName = true;
            this.nameField.setVisible(false);
            this.nameField.setFocused(false);
         }
      }
   }

   public void onNameConfirmed(String name) {
      this.vesselName = name;
      this.waitingForName = false;

      for (int i = 0; i < this.activeScript.length; i++) {
         if (this.activeScript[i].startsWith("NAME_INPUT")) {
            this.beatIndex = i + 1;
            break;
         }
      }

      this.charIndex = 0;
      this.typeTick = 0;
      if (this.nameField != null) {
         this.nameField.setVisible(false);
         this.nameField.setFocused(false);
      }

      if (this.beatIndex < this.activeScript.length) {
         String next = this.activeScript[this.beatIndex];
         if (next.startsWith("SOUL_REVEAL")) {
            this.startSoulReveal();
         } else if (next.startsWith("TITLECARD")) {
            this.startTitlecard();
         }
      }
   }

   public void onSoulRevealReady(SoulType soul) {
      this.rolledSoul = soul;
      if (this.beatIndex < this.activeScript.length && this.activeScript[this.beatIndex].startsWith("SOUL_REVEAL")) {
         this.startSoulReveal();
      }
   }

   private void startSoulReveal() {
      this.inSoulReveal = true;
      this.soulRevealTick = 0;
      this.soulShown = true;
   }

   private void startTitlecard() {
      this.inTitlecard = true;
      this.titleTick = 0;
      if (this.client != null) {
         Identifier musId = Identifier.of("turnbattle", "mus_intronoise");
         SoundEvent se = (SoundEvent)Registries.SOUND_EVENT.get(musId);
         if (se != null) {
            this.musicInstance = PositionedSoundInstance.master(se, 1.0F, 1.0F);
            this.client.getSoundManager().play(this.musicInstance);
         }
      }
   }

   private void finishIntro() {
      if (this.musicInstance != null && this.client != null) {
         this.client.getSoundManager().stop(this.musicInstance);
         this.musicInstance = null;
      }

      Net121.sendToServer(TurnBattleMod.INTRO_DONE, PacketByteBufs.empty());
      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   private void drawWrapped(DrawContext ctx, String text, int x, int y, int maxW, int color) {
      if (!text.isEmpty()) {
         List lines = new ArrayList<>();
         StringBuilder cur = new StringBuilder();

         for (String w : text.split(" ", -1)) {
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

         int lh = 9 + 3;

         for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(lines.get(i)), x, y + i * lh, color);
         }
      }
   }

   private void drawSoulIfReady(DrawContext ctx) {
      if (this.rolledSoul != null && this.soulShown) {
         float alpha = this.inSoulReveal ? Math.min(1.0F, (float)this.soulRevealTick / 20.0F) : 1.0F;
         if (!this.inTitlecard) {
            Identifier soulTex = this.rolledSoul.getTexture();
            int size = 16;
            int sx = this.width / 2 - size / 2;
            int sy = this.height / 2 - size / 2;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            ctx.drawTexture(soulTex, sx, sy, 0.0F, 0.0F, size, size, size, size);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private static record Beat(String speaker, String text, DialogueScreen.BeatType type) {
      static DialogueScreen.Beat text(String s, String t) {
         return new DialogueScreen.Beat(s, t, DialogueScreen.BeatType.TEXT);
      }

      static DialogueScreen.Beat name(String s, String t) {
         return new DialogueScreen.Beat(s, t, DialogueScreen.BeatType.NAME_INPUT);
      }
   }

   private static enum BeatType {
      TEXT,
      NAME_INPUT;
   }
}
