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
public class RogueDeckScreen extends Screen {
   private static final String[] STAT_NAMES = RogueCards.STAT_NAMES;
   private static final int[] STAT_COLORS = RogueCards.STAT_COLORS;
   private int tab = 0;
   private int scroll = 0;
   private final List<RogueDeckScreen.Entry> statEntries = new ArrayList<>();
   private final List<RogueDeckScreen.Entry> passiveEntries = new ArrayList<>();
   private final List<RogueDeckScreen.Entry> skillEntries = new ArrayList<>();
   private int hoveredSkill = -1;

   public RogueDeckScreen() {
      super(Text.literal("Your Deck"));
      this.buildEntries();
   }

   private void buildEntries() {
      int[] bonuses = TurnBattleClientMod.clientCardStatBonuses;

      for (int i = 0; i < 6; i++) {
         if (bonuses[i] != 0) {
            String sign = bonuses[i] > 0 ? "+" : "";
            this.statEntries
               .add(
                  new RogueDeckScreen.Entry(
                     sign + bonuses[i] + " " + STAT_NAMES[i],
                     bonuses[i] > 0 ? "Gained from cards this run." : "Traded away on risky cards this run.",
                     bonuses[i] > 0 ? STAT_COLORS[i] : -48060
                  )
               );
         }
      }

      for (String key : TurnBattleClientMod.clientUniqueCards) {
         RogueCards.Unique u = RogueCards.unique(key);
         this.passiveEntries.add(new RogueDeckScreen.Entry("\u2605 " + (u != null ? u.title() : key), u != null ? u.desc() : "", -22016));
      }

      for (String entry : TurnBattleClientMod.clientRogueSkills) {
         RogueCards.Info info = RogueCards.describe("SKILL:" + entry);
         if (info.kind() == RogueCards.Kind.SKILL) {
            this.skillEntries
               .add(new RogueDeckScreen.Entry(info.title() + " \u00a78[" + info.subtitle().replace("Skill - ", "") + "]", info.desc(), info.color()));
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, -872415232);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2726 YOUR DECK \u2726").formatted(Formatting.GOLD), this.width / 2, 10, 16777215);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Cards acquired this run").formatted(Formatting.GRAY), this.width / 2, 20, 16777215);
      String[] tabNames = new String[]{"Stats", "Passives", "Skills"};
      int tabW = 70;
      int tabH = 14;
      int tabsX = this.width / 2 - (tabNames.length * tabW + (tabNames.length - 1) * 4) / 2;
      int tabY = 32;

      for (int i = 0; i < tabNames.length; i++) {
         int tx = tabsX + i * (tabW + 4);
         boolean active = this.tab == i;
         boolean hov = mx >= tx && mx < tx + tabW && my >= tabY && my < tabY + tabH;
         ctx.fill(tx, tabY, tx + tabW, tabY + tabH, active ? -13417370 : (hov ? -14535851 : -15658718));
         ctx.drawBorder(tx, tabY, tabW, tabH, active ? -10057524 : -13417370);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal(tabNames[i]).formatted(active ? Formatting.WHITE : Formatting.GRAY), tx + tabW / 2, tabY + 3, 16777215
         );
      }

      int contentY = tabY + tabH + 6;
      int contentH = this.height - contentY - 14;
      int px = 16;
      int pw = this.width - 32;

      List entries = switch (this.tab) {
         case 0 -> this.statEntries;
         case 1 -> this.passiveEntries;
         case 2 -> this.skillEntries;
         default -> this.statEntries;
      };
      if (entries.isEmpty()) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(switch (this.tab) {
            case 0 -> "No stat bonuses yet.";
            case 1 -> "No passive cards yet.";
            default -> "No skills unlocked yet.";
         }).formatted(Formatting.DARK_GRAY), this.width / 2, contentY + contentH / 2, 16777215);
      } else {
         int rowH = 28;
         int maxScroll = Math.max(0, entries.size() * rowH - contentH);
         this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
         this.hoveredSkill = -1;
         int y = contentY - this.scroll;

         for (int i = 0; i < entries.size(); i++) {
            RogueDeckScreen.Entry e = entries.get(i);
            int rowY = y + i * rowH;
            if (rowY + rowH >= contentY && rowY <= contentY + contentH) {
               boolean hov = mx >= px && mx < px + pw && my >= rowY && my < rowY + rowH - 2;
               if (hov) {
                  this.hoveredSkill = i;
               }

               ctx.fill(px, rowY, px + pw, rowY + rowH - 2, hov ? 1157627903 : 587202559);
               ctx.fill(px, rowY, px + 3, rowY + rowH - 2, e.color());
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(e.title()).formatted(Formatting.WHITE), px + 6, rowY + 3, e.color());
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(e.desc()).formatted(Formatting.GRAY), px + 6, rowY + 13, -5592406);
            }
         }

         if (maxScroll > 0) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("Scroll to see more").formatted(Formatting.DARK_GRAY), this.width / 2, this.height - 12, 16777215
            );
         }
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal("[P / ESC] Close").formatted(Formatting.DARK_GRAY), 4, this.height - 10, 16777215);
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal(this.skillEntries.size() + " skills  " + this.passiveEntries.size() + " passives").formatted(Formatting.DARK_GRAY),
         this.width - 80,
         this.height - 10,
         16777215
      );
      super.render(ctx, mx, my, delta);
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double amount) {
      this.scroll -= (int)(amount * 12.0);
      return true;
   }

   public boolean mouseClicked(double mx, double my, int button) {
      String[] tabNames = new String[]{"Stats", "Passives", "Skills"};
      int tabW = 70;
      int tabH = 14;
      int tabsX = this.width / 2 - (tabNames.length * tabW + (tabNames.length - 1) * 4) / 2;
      int tabY = 32;

      for (int i = 0; i < tabNames.length; i++) {
         int tx = tabsX + i * (tabW + 4);
         if (mx >= (double)tx && mx < (double)(tx + tabW) && my >= (double)tabY && my < (double)(tabY + tabH)) {
            this.tab = i;
            this.scroll = 0;
            TurnBattleSounds.playSelect();
            return true;
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && keyCode != 80) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         if (this.client != null) {
            this.client.setScreen(null);
         }

         return true;
      }
   }

   private static record Entry(String title, String desc, int color) {
   }
}
