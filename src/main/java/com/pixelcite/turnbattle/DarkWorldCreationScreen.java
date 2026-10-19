package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

@Environment(EnvType.CLIENT)
public class DarkWorldCreationScreen extends Screen {
   private static final int PW = 280;
   private static final int PH = 224;
   private static final int SW = 232;
   private static final int GAP = 10;
   private static final int CARD_W = 110;
   private static final int CARD_H = 130;
   private static final int CARD_GAP = 16;
   private static final int TOGGLE_SIZE = 12;
   private static final int ROW_H = 12;
   private static final int LIST_TOP = 52;
   private static final int LIST_BOTTOM_PAD = 30;
   private static final int GOLD = -862838;
   private final DarkWorldCreationScreen.Catalogue cat;
   private boolean hoverFountain;
   private boolean hoverDepths;
   private boolean hoverToggle;
   private boolean uniqueColor = true;
   private String shape;
   private final Set<String> picked = new LinkedHashSet<>();
   private final boolean[] woolOn = new boolean[DarkWorldCatalog.WOOL_COLORS.length];
   private boolean biomeTab = true;
   private double scroll = 0.0;
   private String hoverInfoTitle = null;
   private String hoverInfo = null;
   private float s = 1.0F;
   private int ox;
   private int oy;
   private static final int R_SHAPE = 0;
   private static final int R_BIOME = 1;
   private static final int R_WOOL = 2;
   private static final int R_HEADER = 3;

   public DarkWorldCreationScreen() {
      this(DarkWorldCreationScreen.Catalogue.empty());
   }

   public DarkWorldCreationScreen(DarkWorldCreationScreen.Catalogue cat) {
      super(Text.literal(""));
      this.cat = cat;
      this.restorePicks();
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   private boolean shapeUnlocked(String id) {
      for (DarkWorldCreationScreen.Shape sh : this.cat.shapes) {
         if (sh.id.equals(id)) {
            return sh.unlocked;
         }
      }

      return false;
   }

   private int stateOf(String id) {
      for (DarkWorldCreationScreen.Entry e : this.cat.biomes) {
         if (e.id.equals(id)) {
            return e.state;
         }
      }

      return DarkWorldCatalog.woolIndex(id) >= 0 ? 1 : 0;
   }

   private void restorePicks() {
      this.shape = this.shapeUnlocked(this.cat.lastShape) ? this.cat.lastShape : "minecraft:overworld";

      for (String g : this.cat.lastGroups) {
         List members = DarkWorldCatalog.splitGroup(g);
         boolean wool = false;

         for (String m : members) {
            int wi = DarkWorldCatalog.woolIndex(m);
            if (wi >= 0) {
               this.woolOn[wi] = true;
               wool = true;
            }
         }

         if (wool) {
            this.picked.add("turnbattle:wool_fields");
         } else if (members.size() == 1 && this.stateOf(members.get(0)) == 1) {
            this.picked.add(members.get(0));
         }
      }

      if (this.picked.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null && mc.player != null) {
            String here = mc.world.getRegistryKey().getValue().toString();
            if (this.cat.lastGroups.isEmpty() && this.shapeUnlocked(here)) {
               this.shape = here;
            }

            mc.world.getBiome(mc.player.getBlockPos()).getKey().ifPresent(k -> {
               String id = k.getValue().toString();
               int wi = DarkWorldCatalog.woolIndex(id);
               if (wi >= 0) {
                  this.woolOn[wi] = true;
                  this.picked.add("turnbattle:wool_fields");
               } else if (this.stateOf(id) == 1 && !DarkWorldCatalog.isHidden(id)) {
                  this.picked.add(id);
               }
            });
         }
      }

      boolean anyWool = false;

      for (boolean b : this.woolOn) {
         anyWool |= b;
      }

      if (!anyWool) {
         Arrays.fill(this.woolOn, true);
      }
   }

   private List<String> groupsToSend() {
      List out = new ArrayList<>();

      for (String id : this.picked) {
         if (id.equals("turnbattle:wool_fields")) {
            List m = new ArrayList<>();

            for (int i = 0; i < this.woolOn.length; i++) {
               if (this.woolOn[i]) {
                  m.add(DarkWorldCatalog.woolBiome(i));
               }
            }

            if (!m.isEmpty()) {
               out.add(DarkWorldCatalog.joinGroup(m));
            }
         } else {
            out.add(id);
         }
      }

      return out;
   }

   private boolean ready() {
      return !this.groupsToSend().isEmpty();
   }

   private static boolean isCustom(String id) {
      return DarkWorldCatalog.CUSTOM.contains(id);
   }

   private static String biomeName(String id) {
      if (isCustom(id)) {
         return DarkWorldCatalog.customName(id);
      } else {
         Identifier i = Identifier.tryParse(id);
         if (i == null) {
            return id;
         } else {
            String key = "biome." + i.getNamespace() + "." + i.getPath();
            return I18n.hasTranslation(key) ? I18n.translate(key, new Object[0]) : DarkWorldCatalog.prettify(id);
         }
      }
   }

   private static int biomeColor(String id) {
      if (isCustom(id)) {
         return DarkWorldCatalog.customColor(id);
      } else {
         String p = id.substring(id.indexOf(58) + 1);
         if (p.contains("desert") || p.contains("beach")) {
            return 14405518;
         } else if (p.contains("badlands")) {
            return 12609082;
         } else if (p.contains("mushroom")) {
            return 10125216;
         } else if (p.contains("ice") || p.contains("frozen") || p.contains("snowy") || p.contains("peaks") || p.contains("grove")) {
            return 14478322;
         } else if (p.contains("cherry")) {
            return 15906772;
         } else if (p.equals("deep_dark")) {
            return 993843;
         } else if (p.equals("the_void")) {
            return 1118481;
         } else {
            MinecraftClient mc = MinecraftClient.getInstance();

            try {
               if (mc.world != null) {
                  Identifier i = Identifier.tryParse(id);
                  Biome b = i == null ? null : (Biome)mc.world.getRegistryManager().get(RegistryKeys.BIOME).get(i);
                  if (b != null) {
                     if (!p.contains("ocean") && !p.contains("river")) {
                        if (!p.contains("nether")
                           && !p.contains("crimson")
                           && !p.contains("warped")
                           && !p.contains("soul")
                           && !p.contains("basalt")
                           && !p.startsWith("end")
                           && !p.equals("the_end")
                           && !p.contains("small_end")) {
                           return b.getGrassColorAt(0.0, 0.0) & 16777215;
                        }

                        return b.getFogColor() & 16777215;
                     }

                     return b.getWaterColor() & 16777215;
                  }
               }
            } catch (Throwable var5) {
            }

            return 8368696;
         }
      }
   }

   private static int shapeColor(String id) {
      return switch (id) {
         case "minecraft:overworld" -> 8368696;
         case "minecraft:the_nether" -> 9121326;
         case "minecraft:the_end" -> 14474400;
         case "turnbattle:sky_islands" -> 9226495;
         default -> 10127560;
      };
   }

   private void layout() {
      int needW = 522;
      int needH = 224;
      this.s = Math.min(1.0F, Math.min(((float)this.width - 12.0F) / (float)needW, ((float)this.height - 12.0F) / (float)needH));
      float vw = (float)this.width / this.s;
      float vh = (float)this.height / this.s;
      this.ox = (int)((vw - (float)needW) / 2.0F);
      this.oy = (int)((vh - (float)needH) / 2.0F);
   }

   private int px() {
      return this.ox;
   }

   private int py() {
      return this.oy;
   }

   private int sx() {
      return this.ox + 280 + 10;
   }

   private int listTop() {
      return this.py() + 52;
   }

   private int listBottom() {
      return this.py() + 224 - 30;
   }

   public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
      ctx.fill(0, 0, this.width, this.height, -1342177280);
      this.layout();
      int mx = (int)((float)mouseX / this.s);
      int my = (int)((float)mouseY / this.s);
      ctx.getMatrices().push();
      ctx.getMatrices().scale(this.s, this.s, 1.0F);
      this.renderMain(ctx, mx, my);
      this.renderCreativity(ctx, mx, my);
      ctx.getMatrices().pop();
      super.render(ctx, mouseX, mouseY, delta);
   }

   private void renderMain(DrawContext ctx, int mouseX, int mouseY) {
      int px = this.px();
      int py = this.py();
      int cxm = px + 140;
      ctx.fill(px - 2, py - 2, px + 280 + 2, py + 224 + 2, -12311962);
      ctx.fill(px, py, px + 280, py + 224, -16120298);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("The Dark Beckons").formatted(new Formatting[]{Formatting.LIGHT_PURPLE, Formatting.BOLD}), cxm, py + 14, -2249985
      );
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Choose your descent").formatted(Formatting.GRAY), cxm, py + 28, -6710887);
      int cardsY = py + 48;
      int leftX = cxm - 110 - 8;
      int rightX = cxm + 8;
      boolean ok = this.ready();
      this.hoverFountain = mouseX >= leftX && mouseX < leftX + 110 && mouseY >= cardsY && mouseY < cardsY + 130;
      this.hoverDepths = mouseX >= rightX && mouseX < rightX + 110 && mouseY >= cardsY && mouseY < cardsY + 130;
      boolean lit = this.hoverFountain && ok;
      ctx.fill(leftX, cardsY, leftX + 110, cardsY + 130, lit ? -12969387 : -14478534);
      ctx.fill(leftX, cardsY, leftX + 110, cardsY + 2, lit ? -3372801 : -10074966);
      this.drawFountainIcon(ctx, leftX + 55, cardsY + 40, lit);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Dark Fountain").formatted(Formatting.BOLD), leftX + 55, cardsY + 74, lit ? -1122817 : -3364114
      );
      if (ok) {
         this.drawWrapped(ctx, "A dimension shaped by what powers your creativity.", leftX + 8, cardsY + 90, 94, -6715222);
      } else {
         this.drawWrapped(ctx, "Choose at least one biome \u2192", leftX + 8, cardsY + 90, 94, -2056096);
      }

      boolean depthsOk = depthsAvailable();
      boolean dlit = this.hoverDepths && depthsOk;
      if (depthsOk) {
         ctx.fill(rightX, cardsY, rightX + 110, cardsY + 130, dlit ? -14017980 : -15266010);
         ctx.fill(rightX, cardsY, rightX + 110, cardsY + 2, dlit ? -5206785 : -10863463);
      } else {
         ctx.fill(rightX, cardsY, rightX + 110, cardsY + 130, -15264484);
         ctx.fill(rightX, cardsY, rightX + 110, cardsY + 2, -12304321);
      }

      this.drawDepthsIcon(ctx, rightX + 55, cardsY + 40, depthsOk, dlit);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         depthsOk ? Text.literal("Depths Descent").formatted(Formatting.BOLD) : Text.literal("Depths Descent").formatted(Formatting.STRIKETHROUGH),
         rightX + 55,
         cardsY + 74,
         !depthsOk ? -10066330 : (dlit ? -1647105 : -4676376)
      );
      if (depthsOk) {
         this.drawWrapped(ctx, "Go down as Ori, floor after floor.", rightX + 8, cardsY + 90, 94, -6715222);
         if (DepthsClient.bestFloor > 0) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("Deepest: Floor " + DepthsClient.bestFloor), rightX + 55, cardsY + 130 - 12, -7702352
            );
         }
      } else {
         this.drawWrapped(
            ctx,
            TurnBattleClientMod.isRogueliteWorld
               ? "This world's Roguelite mode takes its place."
               : (DepthsClient.inRun() ? "You're already down there." : (!oriInParty() ? "Unavailable" : "You have no SOUL to take down there.")),
            rightX + 8,
            cardsY + 90,
            94,
            -11184811
         );
      }

      int toggleY = cardsY + 130 + 14;
      int labelW = this.textRenderer.getWidth("Unique Color");
      int rowW = 18 + labelW;
      int rowX = cxm - rowW / 2;
      this.hoverToggle = mouseX >= rowX && mouseX < rowX + rowW && mouseY >= toggleY && mouseY < toggleY + 12;
      int boxColor = this.uniqueColor ? (this.hoverToggle ? -3372801 : -6728244) : (this.hoverToggle ? -11189146 : -12964278);
      ctx.fill(rowX, toggleY, rowX + 12, toggleY + 12, -15068124);
      ctx.fill(rowX + 1, toggleY + 1, rowX + 12 - 1, toggleY + 12 - 1, boxColor);
      if (this.uniqueColor) {
         ctx.fill(rowX + 3, toggleY + 3, rowX + 12 - 3, toggleY + 12 - 3, -1122817);
      }

      ctx.drawTextWithShadow(
         this.textRenderer, Text.literal("Unique Color").formatted(this.hoverToggle ? Formatting.WHITE : Formatting.GRAY), rowX + 12 + 6, toggleY + 2, -3359779
      );
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Click outside to close").formatted(Formatting.DARK_GRAY), cxm, py + 224 - 16, -10066330);
   }

   private List<DarkWorldCreationScreen.Row> rows() {
      List out = new ArrayList<>();
      int y = 0;
      if (this.biomeTab) {
         boolean customHeader = false;
         boolean foundHeader = false;

         for (DarkWorldCreationScreen.Entry e : this.cat.biomes) {
            if (isCustom(e.id) && !customHeader) {
               out.add(new DarkWorldCreationScreen.Row(3, "Dark World", y, 12));
               y += 12;
               customHeader = true;
            }

            if (!isCustom(e.id) && !foundHeader) {
               out.add(new DarkWorldCreationScreen.Row(3, "Encountered", y, 12));
               y += 12;
               foundHeader = true;
            }

            out.add(new DarkWorldCreationScreen.Row(1, e.id, y, 12));
            y += 12;
            if (e.id.equals("turnbattle:wool_fields") && this.picked.contains(e.id)) {
               out.add(new DarkWorldCreationScreen.Row(2, e.id, y, 12));
               y += 12;
            }
         }

         return out;
      } else {
         for (DarkWorldCreationScreen.Shape sh : this.cat.shapes) {
            out.add(new DarkWorldCreationScreen.Row(0, sh.id, y, 22));
            y += 22;
         }

         return out;
      }
   }

   private int contentHeight(List<DarkWorldCreationScreen.Row> rows) {
      if (rows.isEmpty()) {
         return 0;
      } else {
         DarkWorldCreationScreen.Row last = rows.get(rows.size() - 1);
         return last.y + last.h;
      }
   }

   private void clampScroll(List<DarkWorldCreationScreen.Row> rows) {
      int max = Math.max(0, this.contentHeight(rows) - (this.listBottom() - this.listTop()));
      this.scroll = Math.max(0.0, Math.min((double)max, this.scroll));
   }

   private int countFound() {
      int n = 0;

      for (DarkWorldCreationScreen.Entry e : this.cat.biomes) {
         if (!isCustom(e.id) && e.state == 1) {
            n++;
         }
      }

      return n;
   }

   private int countAll() {
      int n = 0;

      for (DarkWorldCreationScreen.Entry e : this.cat.biomes) {
         if (!isCustom(e.id)) {
            n++;
         }
      }

      return n;
   }

   private void renderCreativity(DrawContext ctx, int mx, int my) {
      int x = this.sx();
      int y = this.py();
      this.hoverInfoTitle = null;
      this.hoverInfo = null;
      ctx.fill(x - 2, y - 2, x + 232 + 2, y + 224 + 2, -10861280);
      ctx.fill(x, y, x + 232, y + 224, -15922681);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("What powers your creativity?"), x + 116, y + 8, -862838);
      int tabY = y + 22;
      int tabW = 108;
      this.drawTab(ctx, x + 8, tabY, tabW, "TERRAIN", !this.biomeTab, mx, my);
      this.drawTab(ctx, x + 8 + tabW, tabY, tabW, "BIOMES", this.biomeTab, mx, my);
      int sumY = y + 40;
      if (this.biomeTab) {
         int chosen = this.groupsToSend().size();
         ctx.drawTextWithShadow(
            this.textRenderer, Text.literal(chosen + " chosen").formatted(chosen > 0 ? Formatting.YELLOW : Formatting.GOLD), x + 8, sumY, -1
         );
         String found = this.countFound() + "/" + this.countAll() + " found";
         ctx.drawTextWithShadow(
            this.textRenderer, Text.literal(found).formatted(Formatting.DARK_GRAY), x + 232 - 8 - this.textRenderer.getWidth(found), sumY, -1
         );
      } else {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("The shape of the land").formatted(Formatting.GRAY), x + 8, sumY, -1);
      }

      List rows = this.rows();
      this.clampScroll(rows);
      int top = this.listTop();
      int bot = this.listBottom();
      ctx.enableScissor((int)((float)x * this.s), (int)((float)top * this.s), (int)((float)(x + 232) * this.s), (int)((float)bot * this.s));

      for (DarkWorldCreationScreen.Row r : rows) {
         int ry = top + r.y - (int)this.scroll;
         if (ry + r.h >= top && ry <= bot) {
            boolean hov = mx >= x + 4 && mx < x + 232 - 8 && my >= Math.max(ry, top) && my < Math.min(ry + r.h, bot);
            switch (r.kind) {
               case 0:
                  this.drawShapeRow(ctx, x, ry, r, hov);
                  break;
               case 1:
                  this.drawBiomeRow(ctx, x, ry, r, hov);
                  break;
               case 2:
                  this.drawWoolRow(ctx, x, ry, mx, my);
                  break;
               case 3:
                  ctx.drawTextWithShadow(this.textRenderer, Text.literal(r.id.toUpperCase()), x + 8, ry + 2, -7702960);
                  ctx.fill(x + 12 + this.textRenderer.getWidth(r.id.toUpperCase()), ry + 6, x + 232 - 12, ry + 7, -12964326);
            }
         }
      }

      ctx.disableScissor();
      int content = this.contentHeight(rows);
      int view = bot - top;
      if (content > view) {
         int barH = Math.max(12, view * view / content);
         int barY = top + (int)((double)(view - barH) * (this.scroll / (double)(content - view)));
         ctx.fill(x + 232 - 5, top, x + 232 - 3, bot, -14804976);
         ctx.fill(x + 232 - 5, barY, x + 232 - 3, barY + barH, -7702960);
      }

      ctx.fill(x + 6, bot + 2, x + 232 - 6, bot + 3, -14015980);
      if (this.hoverInfoTitle != null) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(this.hoverInfoTitle), x + 8, bot + 6, -862838);
         if (this.hoverInfo != null) {
            List lines = this.textRenderer.wrapLines(Text.literal(this.hoverInfo), 216);
            if (!lines.isEmpty()) {
               ctx.drawTextWithShadow(this.textRenderer, lines.get(0), x + 8, bot + 16, -6649232);
            }
         }
      } else {
         String tip = this.biomeTab ? "Weave in any biomes you've encountered." : "Explore other dimensions to find more.";
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(tip), x + 8, bot + 11, -9807296);
      }
   }

   private void drawTab(DrawContext ctx, int x, int y, int w, String label, boolean on, int mx, int my) {
      boolean hov = mx >= x && mx < x + w && my >= y && my < y + 13;
      ctx.fill(x, y, x + w, y + 13, on ? -13753324 : (hov ? -14804976 : -15462647));
      if (on) {
         ctx.fill(x, y + 12, x + w, y + 13, -862838);
      }

      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), x + w / 2, y + 3, on ? -862838 : (hov ? -3359847 : -8951732));
   }

   private void drawShapeRow(DrawContext ctx, int x, int ry, DarkWorldCreationScreen.Row r, boolean hov) {
      DarkWorldCreationScreen.Shape sh = null;

      for (DarkWorldCreationScreen.Shape c : this.cat.shapes) {
         if (c.id.equals(r.id)) {
            sh = c;
         }
      }

      if (sh != null) {
         boolean on = sh.id.equals(this.shape);
         if (on) {
            ctx.fill(x + 4, ry, x + 232 - 8, ry + r.h - 1, -14015982);
         } else if (hov && sh.unlocked) {
            ctx.fill(x + 4, ry, x + 232 - 8, ry + r.h - 1, -15067892);
         }

         int bx = x + 8;
         int by = ry + 3;
         ctx.fill(bx, by, bx + 8, by + 8, sh.unlocked ? -9807302 : -14015464);
         ctx.fill(bx + 1, by + 1, bx + 7, by + 7, -15922681);
         if (on) {
            ctx.fill(bx + 2, by + 2, bx + 6, by + 6, -862838);
         }

         int col = shapeColor(sh.id);
         ctx.fill(x + 20, by, x + 28, by + 8, sh.unlocked ? 0xFF000000 | col : -14936048);
         String name = sh.unlocked ? sh.name : "???";
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), x + 32, ry + 3, sh.unlocked ? (on ? -862838 : -2240336) : -11911120);
         String blurb = sh.unlocked ? DarkWorldCatalog.shapeBlurb(sh.id) : "Not yet encountered.";
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(blurb), x + 32, ry + 13, sh.unlocked ? -8754096 : -12963290);
         if (hov) {
            this.hoverInfoTitle = sh.unlocked ? sh.name : "???";
            this.hoverInfo = sh.unlocked
               ? (on ? "Your Dark World's terrain." : "Click to shape your Dark World like this.")
               : "Set foot in this dimension to unlock it.";
         }
      }
   }

   private void drawBiomeRow(DrawContext ctx, int x, int ry, DarkWorldCreationScreen.Row r, boolean hov) {
      int state = this.stateOf(r.id);
      boolean on = this.picked.contains(r.id);
      if (on) {
         ctx.fill(x + 4, ry, x + 232 - 8, ry + 12, -14015982);
      } else if (hov && state == 1) {
         ctx.fill(x + 4, ry, x + 232 - 8, ry + 12, -15067892);
      }

      int bx = x + 8;
      int by = ry + 2;
      ctx.fill(bx, by, bx + 8, by + 8, state == 1 ? -9807302 : -14015464);
      ctx.fill(bx + 1, by + 1, bx + 7, by + 7, -15922681);
      if (on) {
         ctx.fill(bx + 2, by + 2, bx + 6, by + 6, -862838);
      }

      if (r.id.equals("turnbattle:wool_fields") && state == 1) {
         for (int i = 0; i < 4; i++) {
            int c = DarkWorldCatalog.WOOL_RGB[(i * 4 + 6) % 16];
            ctx.fill(x + 20 + i % 2 * 4, by + i / 2 * 4, x + 24 + i % 2 * 4, by + 4 + i / 2 * 4, 0xFF000000 | c);
         }
      } else {
         ctx.fill(
            x + 20, by, x + 28, by + 8, state == 1 ? 0xFF000000 | biomeColor(r.id) : (state == 2 ? 0xFF000000 | biomeColor(r.id) >> 1 & 8355711 : -14936048)
         );
      }

      String name = state == 0 ? "???" : biomeName(r.id);
      int col = state == 1 ? (on ? -862838 : -2240336) : (state == 2 ? -9805744 : -11911120);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), x + 32, ry + 2, col);
      if (state == 2) {
         String soon = "SOON";
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(soon), x + 232 - 12 - this.textRenderer.getWidth(soon), ry + 2, -10858432);
      } else if (r.id.equals("turnbattle:wool_fields") && on) {
         int n = 0;

         for (boolean b : this.woolOn) {
            if (b) {
               n++;
            }
         }

         String t = n + "/16";
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(t), x + 232 - 12 - this.textRenderer.getWidth(t), ry + 2, -7702960);
      }

      if (hov) {
         this.hoverInfoTitle = name;
         if (state == 0) {
            this.hoverInfo = "Not yet encountered - explore to find it.";
         } else if (isCustom(r.id)) {
            this.hoverInfo = DarkWorldCatalog.customBlurb(r.id);
         } else {
            this.hoverInfo = on ? "Woven into your Dark World." : "Click to weave it into your Dark World.";
         }
      }
   }

   private void drawWoolRow(DrawContext ctx, int x, int ry, int mx, int my) {
      for (int i = 0; i < 16; i++) {
         int cx = woolX(x, i);
         int cy = ry + 1;
         boolean on = this.woolOn[i];
         boolean hov = mx >= cx && mx < cx + 10 && my >= cy && my < cy + 10;
         ctx.fill(cx - 1, cy - 1, cx + 10, cy + 10, on ? -862838 : (hov ? -7702960 : -14015980));
         int c = DarkWorldCatalog.WOOL_RGB[i];
         ctx.fill(cx, cy, cx + 9, cy + 9, on ? 0xFF000000 | c : 0xFF000000 | c >> 2 & 4144959);
         if (hov) {
            this.hoverInfoTitle = DarkWorldCatalog.prettify(DarkWorldCatalog.WOOL_COLORS[i]) + " Wool Fields";
            this.hoverInfo = on ? "Click to leave this colour out." : "Click to add this colour.";
         }
      }
   }

   private static int woolX(int panelX, int i) {
      return panelX + 20 + i * 12;
   }

   private void drawFountainIcon(DrawContext ctx, int cx, int cy, boolean hover) {
      int c = hover ? -2258689 : -6728244;

      for (int i = 0; i < 5; i++) {
         int w = 14 - i * 2;
         ctx.fill(cx - w / 2, cy - 12 + i * 5, cx + w / 2, cy - 8 + i * 5, c);
      }
   }

   private static boolean depthsAvailable() {
      return !TurnBattleClientMod.isRogueliteWorld && !DepthsClient.inRun() && !TurnBattleClientMod.clientSoulRemoved && oriInParty();
   }

   static boolean oriInParty() {
      int ori = Recruit.ORI.ordinal();
      return TurnBattleClientMod.clientActiveRecruit == ori || TurnBattleClientMod.clientSecondRecruit == ori;
   }

   private void drawDepthsIcon(DrawContext ctx, int cx, int cy, boolean on, boolean lit) {
      if (!on) {
         ctx.fill(cx - 8, cy - 8, cx + 8, cy + 8, -13421773);
         ctx.fill(cx - 5, cy - 5, cx + 5, cy + 5, -15066598);
      } else {
         float t = (float)(System.nanoTime() % 60000000000L) / 1.0E9F;
         int edge = lit ? -5206785 : -9810768;
         ctx.fill(cx - 16, cy + 9, cx + 16, cy + 11, -12962744);
         ctx.fill(cx - 9, cy + 9, cx + 9, cy + 13, -16777216);
         ctx.fill(cx - 7, cy - 18, cx + 7, cy + 10, -1073216496);
         ctx.fill(cx - 7, cy - 18, cx - 6, cy + 10, edge);
         ctx.fill(cx + 6, cy - 18, cx + 7, cy + 10, edge);

         for (int i = 0; i < 5; i++) {
            float q = (t * (0.35F + 0.1F * (float)i) + (float)i * 0.27F) % 1.0F;
            int y = cy - 16 + Math.round(q * 26.0F);
            int x = cx - 4 + i * 7 % 9;
            int a = (int)(255.0 * Math.sin(Math.PI * (double)q));
            ctx.fill(x, y, x + 1, y + 1, a << 24 | (lit ? 14207231 : 11047136));
         }
      }
   }

   private void drawWrapped(DrawContext ctx, String text, int x, int y, int maxWidth, int color) {
      int ly = y;

      for (OrderedText line : this.textRenderer.wrapLines(Text.literal(text), maxWidth)) {
         ctx.drawTextWithShadow(this.textRenderer, line, x, ly, color);
         ly += 9 + 1;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.layout();
      double mx = mouseX / (double)this.s;
      if (mx >= (double)this.sx() && mx < (double)(this.sx() + 232)) {
         this.scroll -= verticalAmount * 12.0 * 3.0;
         this.clampScroll(this.rows());
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         this.layout();
         int mx = (int)(mouseX / (double)this.s);
         int my = (int)(mouseY / (double)this.s);
         if (this.hoverToggle) {
            this.uniqueColor = !this.uniqueColor;
            TurnBattleSounds.playSelect();
            return true;
         } else if (this.hoverFountain) {
            if (!this.ready()) {
               TurnBattleSounds.playClient(TurnBattleSounds.DAMAGE, 0.6F, 1.0F);
               this.biomeTab = true;
               return true;
            } else {
               TurnBattleSounds.playSelect();
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeBoolean(this.uniqueColor);
               buf.writeString(this.shape);
               List groups = this.groupsToSend();
               buf.writeVarInt(groups.size());

               for (String g : groups) {
                  buf.writeString(g);
               }

               Net121.sendToServer(TurnBattleMod.DARK_WORLD_CREATE_CONFIRM, buf);
               this.close();
               return true;
            }
         } else if (this.hoverDepths) {
            if (!depthsAvailable()) {
               TurnBattleSounds.playClient(TurnBattleSounds.DAMAGE, 0.6F, 1.0F);
               return true;
            } else {
               TurnBattleSounds.playSelect();
               Net121.sendToServer(TurnBattleMod.DARK_FOUNTAIN_CANCEL, PacketByteBufs.create());
               Net121.sendToServer(DepthsRun.START, PacketByteBufs.create());
               this.close();
               return true;
            }
         } else {
            int x = this.sx();
            int y = this.py();
            if (mx >= x && mx < x + 232 && my >= y && my < y + 224) {
               int tabY = y + 22;
               int tabW = 108;
               if (my >= tabY && my < tabY + 13) {
                  if (mx >= x + 8 && mx < x + 8 + tabW && this.biomeTab) {
                     this.biomeTab = false;
                     this.scroll = 0.0;
                     TurnBattleSounds.playSelect();
                  } else if (mx >= x + 8 + tabW && mx < x + 8 + 2 * tabW && !this.biomeTab) {
                     this.biomeTab = true;
                     this.scroll = 0.0;
                     TurnBattleSounds.playSelect();
                  }

                  return true;
               } else {
                  int top = this.listTop();
                  int bot = this.listBottom();
                  if (my >= top && my < bot && mx < x + 232 - 8) {
                     for (DarkWorldCreationScreen.Row r : this.rows()) {
                        int ry = top + r.y - (int)this.scroll;
                        if (my >= ry && my < ry + r.h) {
                           this.clickRow(r, x, mx);
                           break;
                        }
                     }
                  }

                  return true;
               }
            } else {
               int px = this.px();
               int py = this.py();
               if (mx >= px && mx <= px + 280 && my >= py && my <= py + 224) {
                  return super.mouseClicked(mouseX, mouseY, button);
               } else {
                  Net121.sendToServer(TurnBattleMod.DARK_FOUNTAIN_CANCEL, PacketByteBufs.empty());
                  this.close();
                  return true;
               }
            }
         }
      }
   }

   private void clickRow(DarkWorldCreationScreen.Row r, int x, int mx) {
      switch (r.kind) {
         case 0:
            if (!this.shapeUnlocked(r.id)) {
               deny();
               return;
            }

            this.shape = r.id;
            TurnBattleSounds.playSelect();
            break;
         case 1:
            if (this.stateOf(r.id) != 1) {
               deny();
               return;
            }

            if (!this.picked.remove(r.id)) {
               this.picked.add(r.id);
            }

            TurnBattleSounds.playSelect();
            break;
         case 2:
            for (int i = 0; i < 16; i++) {
               int cx = woolX(x, i);
               if (mx >= cx && mx < cx + 10) {
                  this.woolOn[i] = !this.woolOn[i];
                  boolean any = false;

                  for (boolean b : this.woolOn) {
                     any |= b;
                  }

                  if (!any) {
                     this.picked.remove("turnbattle:wool_fields");
                     Arrays.fill(this.woolOn, true);
                  }

                  TurnBattleSounds.playSelect();
                  return;
               }
            }
      }
   }

   private static void deny() {
      TurnBattleSounds.playClient(TurnBattleSounds.DAMAGE, 0.5F, 1.2F);
   }

   public static record Catalogue(
      List<DarkWorldCreationScreen.Shape> shapes, List<DarkWorldCreationScreen.Entry> biomes, String lastShape, List<String> lastGroups
   ) {
      public static DarkWorldCreationScreen.Catalogue read(PacketByteBuf buf) {
         List shapes = new ArrayList<>();
         int n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            shapes.add(new DarkWorldCreationScreen.Shape(buf.readString(), buf.readString(), buf.readBoolean()));
         }

         List biomes = new ArrayList<>();
         n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            biomes.add(new DarkWorldCreationScreen.Entry(buf.readString(), buf.readByte()));
         }

         String last = buf.readString();
         List groups = new ArrayList<>();
         n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            groups.add(buf.readString());
         }

         return new DarkWorldCreationScreen.Catalogue(shapes, biomes, last, groups);
      }

      static DarkWorldCreationScreen.Catalogue empty() {
         return new DarkWorldCreationScreen.Catalogue(
            List.of(new DarkWorldCreationScreen.Shape("minecraft:overworld", "Overworld", true)),
            List.of(new DarkWorldCreationScreen.Entry("turnbattle:dw_sky_islands", 1)),
            "minecraft:overworld",
            List.of()
         );
      }
   }

   public static record Entry(String id, int state) {
   }

   private static record Row(int kind, String id, int y, int h) {
   }

   public static record Shape(String id, String name, boolean unlocked) {
   }
}
