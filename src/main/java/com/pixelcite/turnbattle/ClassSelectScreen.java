package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class ClassSelectScreen extends Screen {
   private int hovered = -1;
   private static final PlayerClass[] CLASSES = Arrays.stream(PlayerClass.values()).filter(c -> c != PlayerClass.ORI).toArray(PlayerClass[]::new);
   private static final String[][] PREVIEWS = new String[][]{
      {"PSI Fire \u03b1", "PSI Freeze \u03b1", "PSI Thunder \u03b1", "PSI Shield \u03b1", "Brainshock", "PSI Starstorm \u03a9"},
      {"Heal Prayer", "Rude Buster", "Ice Shock", "Snowgrave", "Pacify", "Gigaflare"},
      {"Check", "Flirt", "Threaten", "Talk", "Entreat", "Invoke"},
      {"Struggle (no soul)", "Strike / Mend / Pulse", "Surge / Bind / Channel", "Apex (lvl 21+)", "", ""}
   };
   private static final int[] CLASS_COLORS = new int[]{-12285697, -30652, -7798972, -2276097};
   private int cardW;
   private int cardH;
   private int gapX;
   private int gapY;
   private int gridStartX;
   private int gridStartY;
   private static final int COLS = 2;
   private static final int ROWS = 2;

   public ClassSelectScreen() {
      super(Text.literal("Choose Your Class"));
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected void init() {
      this.cardW = Math.min(180, (this.width - 48) / 2);
      this.cardH = 105;
      this.gapX = 14;
      this.gapY = 12;
      this.gridStartX = (this.width - (2 * this.cardW + 1 * this.gapX)) / 2;
      this.gridStartY = 48;

      for (int i = 0; i < CLASSES.length; i++) {
         int idx = i;
         int col = i % 2;
         int row = i / 2;
         int cx = this.gridStartX + col * (this.cardW + this.gapX);
         int cy = this.gridStartY + row * (this.cardH + this.gapY);
         int bw = this.cardW - 16;
         int bh = 18;
         this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Choose " + CLASSES[i].displayName), btn -> this.choose(idx))
               .dimensions(cx + 8, cy + this.cardH - bh - 6, bw, bh)
               .build()
         );
      }
   }

   private void choose(int idx) {
      TurnBattleSounds.playSelect();
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(idx);
      Net121.sendToServer(TurnBattleMod.CLASS_CHOSEN, buf);
      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("\u2726  Choose Your Class  \u2726").formatted(Formatting.GOLD), this.width / 2, 18, 16777215
      );
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("This choice is permanent (without commands)").formatted(Formatting.GRAY), this.width / 2, 30, 16777215
      );
      this.hovered = -1;

      for (int i = 0; i < CLASSES.length; i++) {
         int col = i % 2;
         int row = i / 2;
         int cx = this.gridStartX + col * (this.cardW + this.gapX);
         int cy = this.gridStartY + row * (this.cardH + this.gapY);
         boolean over = mx >= cx && mx <= cx + this.cardW && my >= cy && my <= cy + this.cardH;
         if (over) {
            this.hovered = i;
         }

         int accent = i < CLASS_COLORS.length ? CLASS_COLORS[i] : -7829368;
         boolean isSoulMage = CLASSES[i] == PlayerClass.SOUL_MAGE;
         ctx.fill(cx - 1, cy - 1, cx + this.cardW + 1, cy + this.cardH + 1, accent);
         ctx.fill(cx, cy, cx + this.cardW, cy + this.cardH, over ? -15066578 : -15921894);
         ctx.fill(cx, cy, cx + this.cardW, cy + 3, accent);
         PlayerClass cls = CLASSES[i];
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(cls.displayName).formatted(Formatting.BOLD), cx + this.cardW / 2, cy + 7, accent);
         int wrapChars = Math.max(16, this.cardW / 6);
         List lines = wrap(cls.description, wrapChars);

         for (int l = 0; l < Math.min(lines.size(), 2); l++) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal(lines.get(l)).formatted(Formatting.GRAY), cx + this.cardW / 2, cy + 19 + l * 10, 16777215
            );
         }

         if (!isSoulMage) {
            String[] prev = i < PREVIEWS.length ? PREVIEWS[i] : new String[0];
            int previewY = cy + 42;
            int shown = 0;

            for (int k = 0; k < prev.length && shown < 2; k++) {
               if (!prev[k].isEmpty()) {
                  ctx.drawCenteredTextWithShadow(
                     this.textRenderer, Text.literal("\u2022 " + prev[k]).formatted(Formatting.WHITE), cx + this.cardW / 2, previewY + shown * 10, 16777215
                  );
                  shown++;
               }
            }
         } else {
            SoulType[] souls = SoulType.values();
            int swatchSize = 7;
            int swatchSpacing = 2;
            int totalSwatchW = souls.length * swatchSize + (souls.length - 1) * swatchSpacing;
            int swatchX = cx + (this.cardW - totalSwatchW) / 2;
            int swatchY = cy + 43;

            for (int si = 0; si < souls.length; si++) {
               int sx = swatchX + si * (swatchSize + swatchSpacing);
               ctx.fill(sx, swatchY, sx + swatchSize, swatchY + swatchSize, souls[si].getColor() | 0xFF000000);
            }

            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("Skills match your soul").formatted(Formatting.LIGHT_PURPLE), cx + this.cardW / 2, cy + 54, 16777215
            );
         }
      }

      super.render(ctx, mx, my, delta);
   }

   private static List<String> wrap(String text, int maxChars) {
      ArrayList lines = new ArrayList<>();

      while (text.length() > maxChars) {
         int cut = text.lastIndexOf(32, maxChars);
         if (cut <= 0) {
            cut = maxChars;
         }

         lines.add(text.substring(0, cut));
         text = text.substring(cut).stripLeading();
      }

      if (!text.isEmpty()) {
         lines.add(text);
      }

      return lines;
   }
}
