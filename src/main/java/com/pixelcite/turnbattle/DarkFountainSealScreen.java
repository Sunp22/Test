package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DarkFountainSealScreen extends Screen {
   private static final int PW = 250;
   private static final int PH = 92;
   private static final Identifier DEFAULT_SOUL = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private static final String WARNING = "The Dark World beyond this Fountain will be deleted forever. (That's a long time.)";
   private int tick = 0;
   private int sel = 1;
   private float shown = 0.0F;

   public DarkFountainSealScreen() {
      super(Text.literal(""));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void tick() {
      this.tick++;
      if (this.shown < (float)"The Dark World beyond this Fountain will be deleted forever. (That's a long time.)".length()) {
         int before = (int)this.shown;
         this.shown = Math.min((float)"The Dark World beyond this Fountain will be deleted forever. (That's a long time.)".length(), this.shown + 1.25F);

         for (int i = before; i < (int)this.shown; i++) {
            if (i % 2 == 0 && !Character.isWhitespace("The Dark World beyond this Fountain will be deleted forever. (That's a long time.)".charAt(i))) {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_TEXT, 0.8F, 0.85F);
               break;
            }
         }
      }
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   private int px() {
      return (this.width - 250) / 2;
   }

   private int py() {
      return this.height / 2 - 46 + 30;
   }

   private int optY() {
      return this.py() + 92 - 22;
   }

   private int sealX() {
      return this.width / 2 - 70;
   }

   private int dontX() {
      return this.width / 2 + 24;
   }

   public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
      float t = (float)this.tick + delta;
      float fade = Math.min(1.0F, t / 8.0F);
      int a = (int)(192.0F * fade);
      ctx.fill(0, 0, this.width, this.height, a << 24);
      int edge = Math.max(20, this.width / 8);
      ctx.fillGradient(0, 0, this.width, edge, (int)(160.0F * fade) << 24, 0);
      ctx.fillGradient(0, this.height - edge, this.width, this.height, 0, (int)(160.0F * fade) << 24);
      this.drawGeyser(ctx, t, fade);
      int px = this.px();
      int py = this.py();
      DarkBox.draw(ctx, px - 3, py - 3, px + 250 + 3, py + 92 + 3);
      String title = "SEAL THE FOUNTAIN?";
      MatrixStack m = ctx.getMatrices();
      m.push();
      float ts = 1.5F;
      m.translate((float)this.width / 2.0F - (float)this.textRenderer.getWidth(title) * ts / 2.0F, (float)(py + 8), 0.0F);
      m.scale(ts, ts, 1.0F);
      ctx.drawText(this.textRenderer, title, 0, 0, -1, false);
      m.pop();
      String w = "The Dark World beyond this Fountain will be deleted forever. (That's a long time.)"
         .substring(0, Math.min("The Dark World beyond this Fountain will be deleted forever. (That's a long time.)".length(), (int)this.shown));
      int ly = py + 26;
      ctx.drawText(this.textRenderer, "*", px + 10, ly, -1, false);

      for (OrderedText line : this.textRenderer.wrapLines(Text.literal(w), 216)) {
         ctx.drawText(this.textRenderer, line, px + 20, ly, -1, false);
         ly += 11;
      }

      int o = this.optionAt((double)mouseX, (double)mouseY);
      if (o >= 0 && o != this.sel) {
         this.sel = o;
         TurnBattleSounds.playHover();
      }

      int oy = this.optY();
      this.drawOption(ctx, "Seal", this.sealX(), oy, this.sel == 0, -40864, true);
      this.drawOption(ctx, "Don't", this.dontX(), oy, this.sel == 1, -256, false);
      super.render(ctx, mouseX, mouseY, delta);
   }

   private void drawGeyser(DrawContext ctx, float t, float fade) {
      int cx = this.width / 2;
      int top = 0;
      int bottom = this.py();
      int alpha = (int)(255.0F * fade);

      for (int y = top; y < bottom; y += 2) {
         double wob = Math.sin((double)y * 0.06 + (double)t * 0.18) * 3.0 + Math.sin((double)y * 0.021 - (double)t * 0.07) * 4.0;
         int half = (int)(26.0 + 6.0 * Math.sin((double)y * 0.04 + (double)t * 0.11));
         int x0 = (int)((double)(cx - half) + wob);
         int x1 = (int)((double)(cx + half) + wob);
         ctx.fill(x0 - 2, y, x1 + 2, y + 2, alpha << 24 | 16777215);
         ctx.fill(x0, y, x1, y + 2, alpha << 24);
      }

      for (int i = 0; i < 18; i++) {
         float speed = 1.2F + (float)(i % 5) * 0.35F;
         float span = (float)(bottom - top + 20);
         float y = (float)bottom - (t * speed + (float)(i * 37)) % span;
         float x = (float)cx + (float)Math.sin((double)i * 1.7 + (double)t * 0.05) * 18.0F;
         int sz = 1 + (i % 3 == 0 ? 1 : 0);
         ctx.fill((int)x, (int)y, (int)x + sz, (int)y + sz, alpha << 24 | 16777215);
      }
   }

   private void drawOption(DrawContext ctx, String label, int x, int y, boolean selected, int hi, boolean tremble) {
      if (selected) {
         SoulType soul = TurnBattleClientMod.clientSoul;
         Identifier tex = soul != null ? soul.getTexture() : DEFAULT_SOUL;
         int jx = tremble ? this.tick % 3 - 1 : 0;
         int jy = tremble ? this.tick / 2 % 3 - 1 : 0;
         ctx.drawTexture(tex, x + jx, y - 1 + jy, 9, 9, 0.0F, 0.0F, 9, 9, 9, 9);
      }

      ctx.drawText(this.textRenderer, label, x + 13, y, selected ? hi : -1, false);
   }

   private int optionAt(double mx, double my) {
      int oy = this.optY();
      if (!(my < (double)(oy - 4)) && !(my > (double)(oy + 12))) {
         if (mx >= (double)(this.sealX() - 2) && mx < (double)(this.sealX() + 13 + this.textRenderer.getWidth("Seal") + 2)) {
            return 0;
         } else {
            return mx >= (double)(this.dontX() - 2) && mx < (double)(this.dontX() + 13 + this.textRenderer.getWidth("Don't") + 2) ? 1 : -1;
         }
      } else {
         return -1;
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      switch (key) {
         case 32:
         case 90:
         case 257:
         case 335:
            if (this.shown < (float)"The Dark World beyond this Fountain will be deleted forever. (That's a long time.)".length()) {
               this.shown = (float)"The Dark World beyond this Fountain will be deleted forever. (That's a long time.)".length();
               return true;
            }

            this.choose(this.sel);
            return true;
         case 65:
         case 68:
         case 262:
         case 263:
            this.sel = 1 - this.sel;
            TurnBattleSounds.playHover();
            return true;
         case 88:
            this.close();
            return true;
         default:
            return super.keyPressed(key, scan, mods);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         int o = this.optionAt(mouseX, mouseY);
         if (o >= 0) {
            this.choose(o);
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   private void choose(int option) {
      TurnBattleSounds.playSelect();
      if (option == 0) {
         Net121.sendToServer(TurnBattleMod.DARK_FOUNTAIN_SEAL_CONFIRM, PacketByteBufs.empty());
      }

      this.close();
   }
}
