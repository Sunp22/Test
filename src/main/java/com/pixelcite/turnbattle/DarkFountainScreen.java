package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class DarkFountainScreen extends Screen {
   private static final int PW = 280;
   private static final int PH = 148;
   private static final int PAD = 10;
   private static final int BTN_W = 58;
   private static final int BTN_H = 13;
   private static final int BTN_GAP = 8;
   private static final int RESET_W = 100;
   private final int avgLevel;
   private final float diffMult;
   private final String oreType;
   private int sealCount;
   private boolean resetConfirming = false;
   private boolean hoverConfirm = false;
   private boolean hoverClose = false;
   private boolean hoverReset = false;

   public DarkFountainScreen(int avgLevel, float diffMult, String oreType, int sealCount) {
      super(Text.literal(""));
      this.avgLevel = avgLevel;
      this.diffMult = diffMult;
      this.oreType = oreType;
      this.sealCount = sealCount;
   }

   public void updateSealCount(int newCount) {
      this.sealCount = newCount;
      this.resetConfirming = false;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void close() {
      Net121.sendToServer(TurnBattleMod.DARK_FOUNTAIN_CANCEL, PacketByteBufs.empty());
      super.close();
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.fillGradient(0, 0, this.width, this.height, -1342177280, -1073741824);
      int px = this.width / 2 - 140;
      int py = this.height / 2 - 74;
      ctx.fill(px - 1, py - 1, px + 280 + 1, py + 148 + 1, -1);
      ctx.fill(px, py, px + 280, py + 148, -16777216);
      int titleH = 14;
      ctx.fill(px, py, px + 280, py + titleH, -15921907);
      ctx.fill(px, py + titleH - 1, px + 280, py + titleH, -7829368);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("DARK FOUNTAIN").formatted(Formatting.WHITE), px + 10, py + 3, -1);
      int ty = py + titleH + 10;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Would you like to create a Dark Fountain here?"), px + 10, ty, -3355444);
      ty += 9 + 8;
      int startFloor = 1 + this.sealCount * 4;
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal("\u25b8 ")
            .formatted(Formatting.DARK_PURPLE)
            .append(Text.literal("Starting floor: ").formatted(Formatting.GRAY))
            .append(Text.literal("Floor " + startFloor).formatted(Formatting.YELLOW)),
         px + 10,
         ty,
         -1
      );
      ty += 9 + 4;
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal("\u25b8 ")
            .formatted(Formatting.DARK_PURPLE)
            .append(Text.literal("Fountains sealed: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(this.sealCount)).formatted(Formatting.YELLOW)),
         px + 10,
         ty,
         -1
      );
      int totalBtnW = 124;
      int btnY = py + 148 - 13 - 10;
      int confirmX = px + 140 - totalBtnW / 2;
      int closeX = confirmX + 58 + 8;
      this.hoverConfirm = isOver(mx, my, confirmX, btnY, 58, 13);
      this.hoverClose = isOver(mx, my, closeX, btnY, 58, 13);
      this.drawButton(ctx, confirmX, btnY, 58, 13, "CONFIRM", this.hoverConfirm, -16772864, -12255420);
      this.drawButton(ctx, closeX, btnY, 58, 13, "CLOSE", this.hoverClose, -15663104, -48060);
      int resetX = px + 280 - 100 - 10;
      int resetY = btnY - 13 - 4;
      this.hoverReset = isOver(mx, my, resetX, resetY, 100, 13);
      String resetLabel = this.resetConfirming ? "SURE? CLICK AGAIN" : "RESET DIFFICULTY";
      int resetBg = this.resetConfirming ? -14548992 : -15658735;
      int resetColor = this.resetConfirming ? -39322 : -10066330;
      this.drawButton(ctx, resetX, resetY, 100, 13, resetLabel, this.hoverReset, resetBg, resetColor);
      super.render(ctx, mx, my, delta);
   }

   private void drawButton(DrawContext ctx, int x, int y, int w, int h, String label, boolean hovered, int bgColor, int textColor) {
      int border = hovered ? -1 : -11184811;
      ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
      ctx.fill(x, y, x + w, y + h, bgColor);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal(label).formatted(hovered ? Formatting.WHITE : Formatting.GRAY), x + w / 2, y + h / 2 - 9 / 2, textColor
      );
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (btn != 0) {
         return super.mouseClicked(mx, my, btn);
      } else {
         int px = this.width / 2 - 140;
         int py = this.height / 2 - 74;
         int totalBtnW = 124;
         int btnY = py + 148 - 13 - 10;
         int confirmX = px + 140 - totalBtnW / 2;
         int closeX = confirmX + 58 + 8;
         int resetX = px + 280 - 100 - 10;
         int resetY = btnY - 13 - 4;
         if (isOver((int)mx, (int)my, confirmX, btnY, 58, 13)) {
            Net121.sendToServer(TurnBattleMod.DARK_FOUNTAIN_CONFIRM, PacketByteBufs.empty());
            if (this.client != null) {
               this.client.setScreen(null);
            }

            TurnBattleSounds.playSelect();
            return true;
         } else if (isOver((int)mx, (int)my, closeX, btnY, 58, 13)) {
            Net121.sendToServer(TurnBattleMod.DARK_FOUNTAIN_CANCEL, PacketByteBufs.empty());
            if (this.client != null) {
               this.client.setScreen(null);
            }

            TurnBattleSounds.playSelect();
            return true;
         } else if (isOver((int)mx, (int)my, resetX, resetY, 100, 13)) {
            if (!this.resetConfirming) {
               this.resetConfirming = true;
            } else {
               this.resetConfirming = false;
               Net121.sendToServer(TurnBattleMod.DARK_FOUNTAIN_RESET, PacketByteBufs.empty());
            }

            TurnBattleSounds.playSelect();
            return true;
         } else {
            this.resetConfirming = false;
            return super.mouseClicked(mx, my, btn);
         }
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 256) {
         this.close();
         return true;
      } else {
         return super.keyPressed(key, scan, mods);
      }
   }

   private static boolean isOver(int mx, int my, int x, int y, int w, int h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private static String oreDisplayName(String t) {
      return switch (t) {
         case "coal" -> "Coal";
         case "iron" -> "Iron";
         case "gold" -> "Gold";
         case "diamond" -> "Diamond";
         case "ancient_debris" -> "Ancient Debris";
         default -> t;
      };
   }

   private static Formatting oreColor(String t) {
      return switch (t) {
         case "coal" -> Formatting.DARK_GRAY;
         case "iron" -> Formatting.WHITE;
         case "gold" -> Formatting.GOLD;
         case "diamond" -> Formatting.AQUA;
         case "ancient_debris" -> Formatting.LIGHT_PURPLE;
         default -> Formatting.GRAY;
      };
   }
}
