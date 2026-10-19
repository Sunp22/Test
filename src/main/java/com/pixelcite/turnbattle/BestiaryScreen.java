package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class BestiaryScreen extends Screen {
   private static final int PANEL_W = 300;
   private static final int PANEL_H = 200;
   private static final int CARD_W = 280;
   private static final int CARD_H = 50;
   private static final int CARD_PAD = 4;
   private final Set<String> defeatedTypes;
   private final List<EntityType<?>> sortedTypes = new ArrayList<>();
   private int scrollOffset = 0;

   public BestiaryScreen(Set<String> defeatedTypes) {
      super(Text.literal("Bestiary"));
      this.defeatedTypes = defeatedTypes;

      for (String typeId : defeatedTypes) {
         try {
            EntityType type = (EntityType<?>)Registries.ENTITY_TYPE.get(Identifier.tryParse(typeId));
            if (type != null) {
               this.sortedTypes.add(type);
            }
         } catch (Exception var5) {
         }
      }

      this.sortedTypes.sort(Comparator.comparing(t -> t.getName().getString()));
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      int panelX = (this.width - 300) / 2;
      int panelY = (this.height - 200) / 2;
      ctx.fill(panelX, panelY, panelX + 300, panelY + 200, -587202560);
      ctx.fill(panelX, panelY, panelX + 300, panelY + 1, -7829368);
      ctx.fill(panelX, panelY + 200 - 1, panelX + 300, panelY + 200, -7829368);
      ctx.fill(panelX, panelY, panelX + 1, panelY + 200, -7829368);
      ctx.fill(panelX + 300 - 1, panelY, panelX + 300, panelY + 200, -7829368);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("\u2726  BESTIARY  \u2726").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}),
         panelX + 150,
         panelY + 8,
         -8892
      );
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Defeated Enemies: " + this.defeatedTypes.size()).formatted(Formatting.GRAY), panelX + 150, panelY + 20, -7829368
      );
      if (this.sortedTypes.isEmpty()) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("No enemies defeated yet!").formatted(Formatting.DARK_GRAY), panelX + 150, panelY + 100, -11184794
         );
      } else {
         int listTop = panelY + 32;
         int listH = 160;
         ctx.enableScissor(panelX + 8, listTop, panelX + 300 - 8, listTop + listH);
         int y = listTop - this.scrollOffset;

         for (EntityType type : this.sortedTypes) {
            if (y + 50 >= listTop && y <= listTop + listH) {
               this.renderEnemyCard(ctx, panelX + 10, y, type, mx, my);
               boolean hp2 = !MobProfile.getPassive(type).isEmpty();
               y += (hp2 ? 64 : 50) + 4;
            } else {
               y += 54;
            }
         }

         ctx.disableScissor();
         super.render(ctx, mx, my, delta);
      }
   }

   private void renderEnemyCard(DrawContext ctx, int x, int y, EntityType<?> type, int mx, int my) {
      Element element = ElementRegistry.getElement(type);
      String name = type.getName().getString();
      String passive = MobProfile.getPassive(type);
      boolean hasPassive = !passive.isEmpty();
      int cardH = hasPassive ? 64 : 50;
      boolean hovered = mx >= x && mx < x + 280 && my >= y && my < y + cardH;
      int bgColor = hovered ? -15066582 : -15921894;
      ctx.fill(x, y, x + 280, y + cardH, bgColor);
      int accentColor = element.getColor();
      ctx.fill(x, y, x + 3, y + cardH, accentColor);
      ctx.fill(x + 3, y, x + 280, y + 1, -13421756);
      ctx.fill(x + 3, y + cardH - 1, x + 280, y + cardH, -13421756);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(name).formatted(Formatting.WHITE), x + 8, y + 8, -1);
      int badgeX = x + 8;
      int badgeY = y + 24;
      int badgeW = 60;
      int badgeH = 16;
      int elemBg = accentColor & 16777215 | 1140850688;
      ctx.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, elemBg);
      ctx.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 1, accentColor);
      ctx.fill(badgeX, badgeY + badgeH - 1, badgeX + badgeW, badgeY + badgeH, accentColor);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal(element.getDisplayName()).formatted(Formatting.BOLD), badgeX + badgeW / 2, badgeY + 4, accentColor
      );
      String[] patterns = this.getPatternNames(element);
      int patX = x + 80;
      int patY = y + 24;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Patterns: ").formatted(Formatting.GRAY), patX, patY, -7829368);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(String.join(", ", patterns)).formatted(Formatting.DARK_GRAY), patX, patY + 10, -11184794);
      if (hasPassive) {
         int pY = y + 50 + 1;
         ctx.fill(x + 3, pY - 1, x + 280, pY, -14540237);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(passive).formatted(Formatting.GOLD), x + 8, pY + 2, -21948);
      }
   }

   private String[] getPatternNames(Element element) {
      return switch (element) {
         case FIRE -> new String[]{"Burst", "Scatter", "Chaos"};
         case ICE -> new String[]{"Rain", "Sweep", "Rows"};
         case NATURE -> new String[]{"Wave", "Arc", "Spiral"};
         case DARK -> new String[]{"Ring", "Diagonal", "Funnel"};
         case LIGHT -> new String[]{"Cross", "Walls", "Geometry"};
         case NONE -> new String[]{"Varied"};
      };
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double amount) {
      int maxScroll = Math.max(0, this.sortedTypes.size() * 54 - 160);
      this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int)(amount * 20.0)));
      return true;
   }

   public boolean mouseClicked(double mx, double my, int button) {
      int panelX = (this.width - 300) / 2;
      int panelY = (this.height - 200) / 2;
      if (!(mx < (double)panelX) && !(mx > (double)(panelX + 300)) && !(my < (double)panelY) && !(my > (double)(panelY + 200))) {
         return super.mouseClicked(mx, my, button);
      } else {
         if (this.client != null) {
            this.client.setScreen(null);
         }

         TurnBattleSounds.playSelect();
         return true;
      }
   }
}
