package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public abstract class VoidMenuScreen extends Screen {
   protected static final Identifier TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   protected static final Identifier TEX_RING = Identifier.of("turnbattle", "textures/effects/ritual_ring.png");
   protected static final Identifier HEART = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   protected static final Identifier DAMAGE_FONT = Identifier.of("turnbattle", "damage_numbers");
   protected static final int HEADER_H = 22;
   protected static final int NAV_H = 16;
   protected final Screen parent;
   protected long openedNanos;
   private long lastFrameNanos;
   protected float dt;
   private final List<VoidMenuScreen.Hit> hits = new ArrayList<>();
   private final List<VoidMenuScreen.Scroll> scrolls = new ArrayList<>();
   private String hoverKey;
   private String lastHoverKey;
   protected final List<VoidMenuScreen.Pulse> pulses = new ArrayList<>();
   protected final List<VoidMenuScreen.Popup> popups = new ArrayList<>();
   protected static final int NAV_STATS = 0;
   protected static final int NAV_SKILLS = 1;
   protected static final int NAV_PARTY = 2;
   protected static final int NAV_EQUIP = 3;
   protected static final int NAV_ITEMS = 4;
   protected static final int NAV_STORY = 5;
   protected static final int NAV_ROGUE = 6;
   protected static final int NAV_SETTINGS = 7;
   private boolean keyMode = false;
   private boolean keysMoved = false;
   private int lastMouseX = Integer.MIN_VALUE;
   private int lastMouseY = Integer.MIN_VALUE;
   private VoidMenuScreen.Hit focus;
   private String lastEdgeSig;
   private final Set<UUID> posed = new HashSet<>();
   private VoidMenuScreen.PendingTag pendingTag;

   protected VoidMenuScreen(String title, Screen parent) {
      super(Text.literal(title));
      this.parent = parent;
   }

   protected abstract int navIndex();

   protected String headerTitle() {
      return null;
   }

   protected abstract String headerRight();

   protected int headerRightColor() {
      return -8892;
   }

   protected abstract void renderBody(DrawContext var1, int var2, int var3);

   protected void onFirstOpen() {
   }

   protected void init() {
      if (this.openedNanos == 0L) {
         this.openedNanos = this.lastFrameNanos = System.nanoTime();
         this.onFirstOpen();
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.parent);
      }
   }

   protected float secs() {
      return (float)(System.nanoTime() - this.openedNanos) / 1.0E9F;
   }

   protected static float since(long nanos) {
      return (float)(System.nanoTime() - nanos) / 1.0E9F;
   }

   protected int bodyTop() {
      return 44;
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      long now = System.nanoTime();
      this.dt = Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = now;
      if (mx != this.lastMouseX || my != this.lastMouseY) {
         this.lastMouseX = mx;
         this.lastMouseY = my;
         if (this.keysMoved) {
            this.keysMoved = false;
         } else {
            this.keyMode = false;
         }
      }

      if (this.keyMode && this.focus != null) {
         mx = this.focus.x() + this.focus.w() / 2;
         my = this.focus.y() + this.focus.h() / 2;
      }

      this.hits.clear();
      this.scrolls.clear();
      this.hoverKey = null;
      this.pendingTag = null;
      this.renderBody(ctx, mx, my);
      if (this.pendingTag != null) {
         this.drawTag(ctx, this.pendingTag);
      }

      this.renderPulses(ctx);
      this.renderHeader(ctx, mx, my);
      if (this.hoverKey != null && !this.hoverKey.equals(this.lastHoverKey)) {
         TurnBattleSounds.playHover();
      }

      this.lastHoverKey = this.hoverKey;
      if (this.focus != null) {
         this.focus = this.refind(this.focus);
      }

      if (this.keyMode && this.focus != null) {
         this.drawFocus(ctx, this.focus);
      }

      if (this.keyMode) {
         this.drawKeyHints(ctx);
      }
   }

   private static int cx(VoidMenuScreen.Hit h) {
      return h.x() + h.w() / 2;
   }

   private static int cy(VoidMenuScreen.Hit h) {
      return h.y() + h.h() / 2;
   }

   private VoidMenuScreen.Hit refind(VoidMenuScreen.Hit f) {
      VoidMenuScreen.Hit same = null;
      VoidMenuScreen.Hit under = null;
      VoidMenuScreen.Hit near = null;
      double nd = Double.MAX_VALUE;

      for (VoidMenuScreen.Hit h : this.hits) {
         if (!h.right() && h.w() > 0 && h.h() > 0) {
            if (h.x() == f.x() && h.y() == f.y() && h.w() == f.w() && h.h() == f.h()) {
               same = h;
            }

            if (inside((double)cx(f), (double)cy(f), h.x(), h.y(), h.w(), h.h())) {
               under = h;
            }

            double d = Math.hypot((double)(cx(h) - cx(f)), (double)(cy(h) - cy(f)));
            if (d < nd) {
               nd = d;
               near = h;
            }
         }
      }

      return same != null ? same : (under != null ? under : near);
   }

   private void moveFocus(int dir) {
      if (this.focus == null) {
         this.focus = this.firstFocus();
      } else {
         int fx = cx(this.focus);
         int fy = cy(this.focus);
         VoidMenuScreen.Hit best = null;
         double bestScore = Double.MAX_VALUE;
         int bestTier = Integer.MAX_VALUE;
         boolean horiz = dir == 1 || dir == 3;
         boolean focusHeader = this.focus.y() < this.bodyTop();

         for (VoidMenuScreen.Hit h : this.hits) {
            if (!h.right()
               && h.w() > 0
               && h.h() > 0
               && (h.x() != this.focus.x() || h.y() != this.focus.y() || h.w() != this.focus.w() || h.h() != this.focus.h())) {
               int dx = cx(h) - fx;
               int dy = cy(h) - fy;
               double along = dir == 1 ? (double)dx : (dir == 3 ? (double)(-dx) : (dir == 2 ? (double)dy : (double)(-dy)));
               if (!(along <= 2.0)) {
                  boolean otherZone = h.y() < this.bodyTop() != focusHeader;
                  if (!otherZone || !horiz) {
                     double across = horiz ? (double)Math.abs(dy) : (double)Math.abs(dx);
                     boolean lined = horiz
                        ? h.y() < this.focus.y() + this.focus.h() && this.focus.y() < h.y() + h.h()
                        : h.x() < this.focus.x() + this.focus.w() && this.focus.x() < h.x() + h.w();
                     int tier = (otherZone ? 2 : 0) + (horiz && !lined ? 1 : 0);
                     double score = along + across * (lined ? 0.4 : 2.5);
                     if (tier < bestTier || tier == bestTier && score < bestScore) {
                        bestTier = tier;
                        bestScore = score;
                        best = h;
                     }
                  }
               }
            }
         }

         VoidMenuScreen.Scroll inList = this.scrollAt(fx, fy);
         boolean leaving = best == null || inList != null && !inside((double)cx(best), (double)cy(best), inList.x(), inList.y(), inList.w(), inList.h());
         if (leaving && inList != null && (dir == 0 || dir == 2 || best == null)) {
            String sig = this.signature(inList);
            if (!sig.equals(this.lastEdgeSig)) {
               inList.by().accept(dir != 2 && dir != 1 ? 1.0 : -1.0);
               this.lastEdgeSig = sig;
               return;
            }
         }

         this.lastEdgeSig = null;
         if (best == null) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 0.5F, 1.4F);
         } else {
            this.focus = best;
            VoidMenuScreen.Scroll sa = this.scrollAt(cx(best), cy(best));
            if (sa != null) {
               if (dir == 2 && best.y() + best.h() >= sa.y() + sa.h() - 3) {
                  sa.by().accept(-1.0);
               }

               if (dir == 0 && best.y() <= sa.y() + 3) {
                  sa.by().accept(1.0);
               }
            }
         }
      }
   }

   private String signature(VoidMenuScreen.Scroll s) {
      StringBuilder sb = new StringBuilder();

      for (VoidMenuScreen.Hit h : this.hits) {
         if (inside((double)cx(h), (double)cy(h), s.x(), s.y(), s.w(), s.h())) {
            sb.append(h.x()).append(',').append(h.y()).append(';');
         }
      }

      return sb.toString();
   }

   private VoidMenuScreen.Scroll scrollAt(int x, int y) {
      for (int i = this.scrolls.size() - 1; i >= 0; i--) {
         VoidMenuScreen.Scroll s = this.scrolls.get(i);
         if (inside((double)x, (double)y, s.x(), s.y(), s.w(), s.h())) {
            return s;
         }
      }

      return null;
   }

   private VoidMenuScreen.Hit firstFocus() {
      for (VoidMenuScreen.Hit h : this.hits) {
         if (!h.right() && h.w() > 0 && h.h() > 0 && h.y() >= this.bodyTop()) {
            return h;
         }
      }

      for (VoidMenuScreen.Hit hx : this.hits) {
         if (!hx.right() && hx.w() > 0 && hx.h() > 0) {
            return hx;
         }
      }

      return null;
   }

   private void drawFocus(DrawContext ctx, VoidMenuScreen.Hit f) {
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 600.0F);
      float br = 0.65F + 0.35F * (float)Math.sin((double)(this.secs() * 6.0F));
      int c = MenuBackdrop.argb(16777024, br);
      int x0 = f.x() - 2;
      int y0 = f.y() - 2;
      int x1 = f.x() + f.w() + 2;
      int y1 = f.y() + f.h() + 2;
      int l = Math.max(3, Math.min(8, Math.min(f.w(), f.h()) / 3));
      ctx.fill(x0, y0, x0 + l, y0 + 1, c);
      ctx.fill(x0, y0, x0 + 1, y0 + l, c);
      ctx.fill(x1 - l, y0, x1, y0 + 1, c);
      ctx.fill(x1 - 1, y0, x1, y0 + l, c);
      ctx.fill(x0, y1 - 1, x0 + l, y1, c);
      ctx.fill(x0, y1 - l, x0 + 1, y1, c);
      ctx.fill(x1 - l, y1 - 1, x1, y1, c);
      ctx.fill(x1 - 1, y1 - l, x1, y1, c);
      m.pop();
   }

   private void drawKeyHints(DrawContext ctx) {
      String ok = TurnBattleClientMod.keyName(TurnBattleClientMod.battleConfirm);
      String no = TurnBattleClientMod.keyName(TurnBattleClientMod.battleBack);
      String hint = "WASD move   " + ok + " select   " + no + " back   Q/E pages";
      int w = this.textRenderer.getWidth(hint) + 8;
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 600.0F);
      ctx.fill(this.width - w - 2, this.height - 13, this.width - 2, this.height - 2, -1073741824);
      ctx.drawTextWithShadow(this.textRenderer, hint, this.width - w + 2, this.height - 11, -4669232);
      m.pop();
   }

   private boolean menuKey(int key) {
      int dir = switch (key) {
         case 65, 263 -> 3;
         case 68, 262 -> 1;
         case 83, 264 -> 2;
         case 87, 265 -> 0;
         default -> -1;
      };
      if (dir >= 0) {
         this.keyMode = true;
         this.keysMoved = true;
         VoidMenuScreen.Hit before = this.focus;
         this.moveFocus(dir);
         if (this.focus != before) {
            TurnBattleSounds.playHover();
         }

         return true;
      } else if (TurnBattleClientMod.isMenuConfirm(key)) {
         this.keyMode = true;
         this.keysMoved = true;
         if (this.focus == null) {
            this.focus = this.firstFocus();
            return true;
         } else {
            this.focus.action().run();
            return true;
         }
      } else if (TurnBattleClientMod.isMenuBack(key)) {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
         this.close();
         return true;
      } else {
         switch (key) {
            case 69:
            case 81:
               List tabs = navTabs();
               int i = 0;

               for (int k = 0; k < tabs.size(); k++) {
                  if (tabs.get(k).id() == this.navIndex()) {
                     i = k;
                  }
               }

               i = Math.floorMod(i + (key == 69 ? 1 : -1), tabs.size());
               this.openSibling(tabs.get(i).id());
               return true;
            default:
               return false;
         }
      }
   }

   protected void renderVoid(DrawContext ctx, float hx, float hy, float pool) {
      float t = this.secs();
      float in = ease(clamp01(t / 0.6F));
      ctx.fillGradient(0, 0, this.width, this.height, -16645624, -16119784);
      int tint = tint();
      MenuBackdrop.tex(ctx, TEX_GLOW, hx, hy, pool, pool * 0.55F, tint, 0.22F * in);
      MenuBackdrop.tex(ctx, TEX_GLOW, hx, hy, pool * 0.35F, pool * 0.18F, 16777215, 0.1F * in);

      for (int r = 0; r < 3; r++) {
         float q = (t * 0.06F + (float)r / 3.0F) % 1.0F;
         float rs = pool * (0.2F + q * 1.6F);
         MenuBackdrop.tex(ctx, TEX_RING, hx, hy, rs, rs * 0.3F, tint, 0.18F * (1.0F - q) * in);
      }

      for (int i = 0; i < 80; i++) {
         float rx = CloneAnimator.rnd(i * 3 + 1);
         float speed = 0.02F + CloneAnimator.rnd(i * 7) * 0.035F;
         float q = (CloneAnimator.rnd(i * 3 + 2) + t * speed) % 1.0F;
         float sway = (float)Math.sin((double)(t * (0.5F + CloneAnimator.rnd(i) * 0.7F) + (float)i)) * 5.0F;
         float x = rx * (float)this.width + sway;
         float y = (float)this.height - q * (float)(this.height + 10);
         float life = (float)Math.sin(Math.PI * (double)q);
         float a = (0.12F + 0.4F * life) * in * (0.4F + CloneAnimator.rnd(i * 5) * 0.6F);
         int px = i % 5 == 0 ? 2 : 1;
         ctx.fill((int)x, (int)y, (int)x + px, (int)y + px, MenuBackdrop.argb(i % 4 == 0 ? 16777215 : tint, a));
      }

      int e = Math.max(20, this.height / 5);
      ctx.fillGradient(0, 0, this.width, e, -1342177280, 0);
      ctx.fillGradient(0, this.height - e, this.width, this.height, 0, -1342177280);
   }

   protected static int tint() {
      SoulType s = TurnBattleClientMod.clientSoul;
      return s != null ? s.getColor() & 16777215 : 12568831;
   }

   protected static List<VoidMenuScreen.NavTab> navTabs() {
      List out = new ArrayList<>();
      if (DepthsClient.inRun()) {
         out.add(new VoidMenuScreen.NavTab("STATS", 0));
         out.add(new VoidMenuScreen.NavTab("EQUIP", 3));
         out.add(new VoidMenuScreen.NavTab("ITEMS", 4));
         out.add(new VoidMenuScreen.NavTab("SETTINGS", 7));
         return out;
      } else {
         out.add(new VoidMenuScreen.NavTab("STATS", 0));
         out.add(new VoidMenuScreen.NavTab(TurnBattleClientMod.rogueliteInRun ? "DECK" : "SKILLS", 1));
         out.add(new VoidMenuScreen.NavTab("PARTY", 2));
         out.add(new VoidMenuScreen.NavTab("EQUIP", 3));
         out.add(new VoidMenuScreen.NavTab("ITEMS", 4));
         if (TurnBattleClientMod.isRogueliteWorld) {
            out.add(new VoidMenuScreen.NavTab("ROGUELITE", 6));
         } else if (TurnBattleClientMod.storyModeEnabled) {
            out.add(new VoidMenuScreen.NavTab("STORY", 5));
         }

         out.add(new VoidMenuScreen.NavTab("SETTINGS", 7));
         return out;
      }
   }

   private void renderHeader(DrawContext ctx, int mx, int my) {
      float in = ease(clamp01((this.secs() - 0.05F) / 0.35F));
      int off = (int)((1.0F - in) * -38.0F);
      List tabs = navTabs();
      String title = this.headerTitle();
      if (title == null) {
         title = "";

         for (VoidMenuScreen.NavTab t : tabs) {
            if (t.id() == this.navIndex()) {
               title = t.label();
            }
         }
      }

      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, (float)off, 500.0F);
      ctx.fillGradient(0, 0, this.width, 22, -268171760, -536475114);
      ctx.fill(0, 21, this.width, 22, 1090519039);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(title).setStyle(Style.EMPTY.withBold(true)), 10, 7, -1);
      int cw = 16;
      int cx = this.width - cw - 4;
      int cy = 3;
      boolean ch = mx >= cx && mx < cx + cw && my >= cy + off && my < cy + cw + off;
      if (ch) {
         this.hover("close");
      }

      ctx.fill(cx, cy, cx + cw, cy + cw, ch ? -192 : 1358954495);
      ctx.fill(cx + 1, cy + 1, cx + cw - 1, cy + cw - 1, -16447986);
      ctx.drawCenteredTextWithShadow(this.textRenderer, "x", cx + cw / 2, cy + 4, ch ? -192 : -4669232);
      if (off == 0) {
         this.click(cx, cy, cw, cw, () -> {
            TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
            this.close();
         });
      }

      String right = this.headerRight();
      if (right != null && !right.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, right, cx - 10 - this.textRenderer.getWidth(right), 7, this.headerRightColor());
      }

      int ty = 22;
      int tw = Math.min(96, this.width / tabs.size());
      int tx0 = this.width / 2 - tw * tabs.size() / 2;
      ctx.fill(0, ty, this.width, ty + 16, -1073544183);
      int accent = tint();

      for (int i = 0; i < tabs.size(); i++) {
         VoidMenuScreen.NavTab nt = tabs.get(i);
         int tx = tx0 + i * tw;
         boolean sel = nt.id() == this.navIndex();
         boolean hov = !sel && mx >= tx && mx < tx + tw && my >= ty + off && my < ty + 16 + off;
         if (hov) {
            this.hover("nav" + nt.id());
         }

         if (sel) {
            ctx.fillGradient(tx, ty, tx + tw - 1, ty + 16, MenuBackdrop.argb(accent, 0.04F), MenuBackdrop.argb(accent, 0.28F));
            ctx.fill(tx, ty + 16 - 2, tx + tw - 1, ty + 16, accent | 0xFF000000);
         } else if (hov) {
            ctx.fill(tx, ty, tx + tw - 1, ty + 16, 419430399);
         }

         ctx.drawCenteredTextWithShadow(this.textRenderer, this.fit(nt.label(), tw - 4), tx + tw / 2, ty + 4, sel ? -1 : (hov ? -192 : -7564378));
         int id = nt.id();
         if (!sel && off == 0) {
            this.click(tx, ty, tw, 16, () -> this.openSibling(id));
         }
      }

      m.pop();
   }

   protected void openSibling(int id) {
      if (this.client != null) {
         TurnBattleSounds.playSelect();
         switch (id) {
            case 0:
               this.client.setScreen(new StatsScreen(this.parent));
               break;
            case 1:
               if (TurnBattleClientMod.rogueliteInRun) {
                  this.client.setScreen(new RogueDeckScreen());
               } else {
                  this.client.setScreen(new SkillSelectScreen(this.parent));
               }
               break;
            case 2:
               this.client.setScreen(new PartyMenuScreen(this.parent));
               break;
            case 3:
               this.client.setScreen(new EquipScreen(this.parent));
               break;
            case 4:
               this.client.setScreen(new ItemsScreen(this.parent));
               break;
            case 5:
               this.client.setScreen(new PartyScreen(5));
               break;
            case 6:
               Net121.sendToServer(TurnBattleMod.ROGUE_INFO_REQUEST, PacketByteBufs.empty());
               this.client.setScreen(new RogueliteScreen());
               break;
            case 7:
               this.client.setScreen(new SettingsScreen(this.parent));
         }
      }
   }

   protected static void openOwnSkillTree() {
      TurnBattleClientMod.pendingCodexOpen = true;
      Net121.sendToServer(TurnBattleMod.SKILL_INFO_REQUEST, PacketByteBufs.empty());
   }

   protected void click(int x, int y, int w, int h, Runnable action) {
      this.hits.add(new VoidMenuScreen.Hit(x, y, w, h, action, false));
   }

   protected void rightClick(int x, int y, int w, int h, Runnable action) {
      this.hits.add(new VoidMenuScreen.Hit(x, y, w, h, action, true));
   }

   protected void scrollArea(int x, int y, int w, int h, DoubleConsumer by) {
      this.scrolls.add(new VoidMenuScreen.Scroll(x, y, w, h, by));
   }

   protected void hover(String key) {
      this.hoverKey = key;
   }

   protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
      return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
   }

   public boolean mouseClicked(double mx, double my, int button) {
      for (int i = this.hits.size() - 1; i >= 0; i--) {
         VoidMenuScreen.Hit h = this.hits.get(i);
         if (h.right() == (button == 1) && inside(mx, my, h.x(), h.y(), h.w(), h.h())) {
            h.action().run();
            return true;
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
      for (int i = this.scrolls.size() - 1; i >= 0; i--) {
         VoidMenuScreen.Scroll s = this.scrolls.get(i);
         if (inside(mx, my, s.x(), s.y(), s.w(), s.h())) {
            s.by().accept(verticalAmount);
            return true;
         }
      }

      return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (TurnBattleClientMod.isPartyKey(key, scan)) {
         if (this.client != null) {
            this.client.setScreen(null);
         }

         return true;
      } else if (this.menuKey(key)) {
         return true;
      } else if (key == 258) {
         int[] ring = DepthsClient.inRun() ? new int[]{0, 3, 4} : new int[]{0, 2, 3, 4};
         int i = 0;

         for (int k = 0; k < ring.length; k++) {
            if (ring[k] == this.navIndex()) {
               i = k;
            }
         }

         i = (i + ((mods & 1) != 0 ? ring.length - 1 : 1)) % ring.length;
         this.openSibling(ring[i]);
         return true;
      } else {
         return super.keyPressed(key, scan, mods);
      }
   }

   protected void box(DrawContext ctx, int x, int y, int w, int h, int accent, float a) {
      if (DarkBox.inDarkWorld()) {
         DarkBox.draw(ctx, x - 2, y - 2, x + w + 2, y + h + 2, a);
         ctx.fill(x + 1, y + 1, x + w - 1, y + 3, MenuBackdrop.argb(accent, a));
      } else {
         ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, MenuBackdrop.argb(16777215, a));
         ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(329230, 0.96F * a));
         ctx.fill(x, y, x + w, y + 2, MenuBackdrop.argb(accent, a));
      }
   }

   protected void panel(DrawContext ctx, int x, int y, int w, int h, int accent, float a, boolean lit) {
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(lit ? lerpRgb(329230, accent, 0.16F) : 329230, 0.86F * a));
      int edge = MenuBackdrop.argb(accent, (lit ? 0.9F : 0.35F) * a);
      ctx.fill(x, y, x + w, y + 1, edge);
      ctx.fill(x, y + h - 1, x + w, y + h, edge);
      ctx.fill(x, y, x + 1, y + h, edge);
      ctx.fill(x + w - 1, y, x + w, y + h, edge);
   }

   protected boolean button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean enabled, float a, Runnable action) {
      boolean hov = inside((double)mx, (double)my, x, y, w, h);
      if (hov) {
         this.hover("btn:" + label + x + "," + y);
      }

      int border = !enabled ? 5592422 : (hov ? 16777024 : 16777215);
      ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, MenuBackdrop.argb(border, a));
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(hov && enabled ? 1712694 : 0, a));
      int lw = this.textRenderer.getWidth(label);
      int shift = hov && enabled ? 5 : 0;
      ctx.drawTextWithShadow(
         this.textRenderer, label, x + w / 2 - lw / 2 + shift, y + (h - 8) / 2, MenuBackdrop.argb(!enabled ? 6710903 : (hov ? 16777024 : 16777215), a)
      );
      if (hov && enabled) {
         this.heart(ctx, (float)(x + w / 2 - lw / 2 + shift - 11), (float)(y + (h - 8) / 2), 8, a);
      }

      this.click(x - 2, y - 2, w + 4, h + 4, () -> {
         if (!enabled) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
         } else {
            action.run();
         }
      });
      return hov;
   }

   protected void heart(DrawContext ctx, float x, float y, int size, float a) {
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
      ctx.drawTexture(HEART, Math.round(x), Math.round(y), size, size, 0.0F, 0.0F, 16, 16, 16, 16);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected void soulHeart(DrawContext ctx, SoulType s, float x, float y, int size, float a) {
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);

      try {
         ctx.drawTexture(s != null ? s.getTexture() : HEART, Math.round(x), Math.round(y), size, size, 0.0F, 0.0F, 16, 16, 16, 16);
      } catch (Throwable var8) {
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected void hpBar(DrawContext ctx, int x, int y, int w, int h, float hp, float max, float ghost, float a) {
      ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, MenuBackdrop.argb(0, 0.8F * a));
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(7998994, a));
      float f = max > 0.0F ? clamp01(hp / max) : 0.0F;
      int fw = Math.round((float)w * f);
      if (ghost > 0.0F && max > 0.0F) {
         int gw = Math.round((float)w * clamp01((hp + ghost) / max));
         float blink = 0.55F + 0.45F * (float)Math.sin((double)(this.secs() * 7.0F));
         if (gw > fw) {
            ctx.fill(x + fw, y, x + gw, y + h, MenuBackdrop.argb(5635959, blink * a));
         }
      }

      if (fw > 0) {
         ctx.fill(x, y, x + fw, y + h, MenuBackdrop.argb(f > 0.25F ? 16769866 : 16744512, a));
      }
   }

   protected void face(DrawContext ctx, Identifier skin, int x, int y, int size, float a) {
      if (skin != null) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
         ctx.drawTexture(skin, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
         ctx.drawTexture(skin, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   protected void item(DrawContext ctx, ItemStack st, float cx, float cy, float size) {
      if (st != null && !st.isEmpty()) {
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(cx - size / 2.0F, cy - size / 2.0F, 0.0F);
         m.scale(size / 16.0F, size / 16.0F, 1.0F);
         ctx.drawItem(st, 0, 0);
         m.pop();
      }
   }

   protected String fit(String s, int w) {
      if (s == null) {
         return "";
      } else {
         return this.textRenderer.getWidth(s) <= w ? s : this.textRenderer.trimToWidth(s, Math.max(0, w - this.textRenderer.getWidth("..."))) + "...";
      }
   }

   protected void popup(String text, float x, float y, int rgb) {
      this.popups.add(new VoidMenuScreen.Popup(text, x, y, rgb, System.nanoTime()));
   }

   protected void pulse(float x, float y, int rgb, float size) {
      this.pulses.add(new VoidMenuScreen.Pulse(x, y, rgb, System.nanoTime(), size));
   }

   protected void renderPulses(DrawContext ctx) {
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 450.0F);
      this.pulses.removeIf(p -> since(p.born()) > 0.9F);

      for (VoidMenuScreen.Pulse p : this.pulses) {
         float q = clamp01(since(p.born()) / 0.9F);
         float e = ease(q);
         MenuBackdrop.tex(ctx, TEX_RING, p.x(), p.y(), p.size() * (0.6F + e * 2.4F), p.size() * (0.6F + e * 2.4F), p.rgb(), 0.85F * (1.0F - q));
         MenuBackdrop.tex(ctx, TEX_GLOW, p.x(), p.y(), p.size() * 2.2F, p.size() * 2.2F, 16777215, 0.5F * (1.0F - q));

         for (int i = 0; i < 8; i++) {
            double ang = (double)i * Math.PI / 4.0 + (double)q * 0.9;
            float d = p.size() * (0.5F + e * 1.5F);
            CloneAnimator.star4(
               ctx,
               p.x() + (float)Math.cos(ang) * d,
               p.y() + (float)Math.sin(ang) * d,
               2.2F * (1.0F - q),
               MenuBackdrop.argb(i % 2 == 0 ? 16777215 : p.rgb(), 1.0F - q)
            );
         }
      }

      this.popups.removeIf(p -> since(p.born()) > 1.3F);

      for (VoidMenuScreen.Popup p : this.popups) {
         float s = since(p.born());
         float rise = ease(clamp01(s / 0.4F));
         float a = s < 0.9F ? 1.0F : clamp01(1.0F - (s - 0.9F) / 0.4F);
         Text txt = Text.literal(p.text()).setStyle(Style.EMPTY.withFont(DAMAGE_FONT));
         ctx.drawTextWithShadow(
            this.textRenderer, txt, (int)(p.x() - (float)this.textRenderer.getWidth(txt) / 2.0F), (int)(p.y() - rise * 14.0F), MenuBackdrop.argb(p.rgb(), a)
         );
      }

      m.pop();
   }

   protected List<VoidMenuScreen.Member> roster() {
      List out = new ArrayList<>();
      out.add(this.you());
      if (DepthsClient.inRun()) {
         return out;
      } else {
         Recruit[] recs = Recruit.values();
         boolean[] unl = TurnBattleClientMod.clientRecruitUnlocked;

         for (int ri = 0; ri < recs.length; ri++) {
            if (unl != null && ri < unl.length && unl[ri]) {
               out.add(
                  new VoidMenuScreen.Member(
                     ri,
                     recs[ri].displayName,
                     at(TurnBattleClientMod.clientRecruitLevels, ri, 1),
                     recs[ri].soulType,
                     recs[ri].playerClass,
                     at(TurnBattleClientMod.clientRecruitHp, ri),
                     at(TurnBattleClientMod.clientRecruitMaxHp, ri)
                  )
               );
            }
         }

         String[] cn = TurnBattleClientMod.customRecruitNames;
         if (cn != null) {
            for (int ci = 0; ci < cn.length; ci++) {
               out.add(this.custom(ci));
            }
         }

         return out;
      }
   }

   protected VoidMenuScreen.Member you() {
      String name = !TurnBattleClientMod.clientVesselName.isEmpty()
         ? TurnBattleClientMod.clientVesselName
         : (this.client != null && this.client.player != null ? this.client.player.getName().getString() : "You");
      float hp = this.client != null && this.client.player != null ? this.client.player.getHealth() : 20.0F;
      float max = this.client != null && this.client.player != null ? this.client.player.getMaxHealth() : 20.0F;
      return new VoidMenuScreen.Member(
         -1, name, TurnBattleClientMod.clientPlayerLevel, TurnBattleClientMod.clientSoul, TurnBattleClientMod.clientPlayerClass, hp, max
      );
   }

   protected VoidMenuScreen.Member custom(int ci) {
      String[] cn = TurnBattleClientMod.customRecruitNames;
      int[] cls = TurnBattleClientMod.customRecruitClasses;
      int[] souls = TurnBattleClientMod.customRecruitSouls;
      int clsOrd = at(cls, ci, -1);
      int soulOrd = at(souls, ci, -1);
      PlayerClass pc = clsOrd >= 0 && clsOrd < PlayerClass.values().length ? PlayerClass.values()[clsOrd] : PlayerClass.HUMAN_MAGE;
      SoulType st = soulOrd >= 0 && soulOrd < SoulType.values().length ? SoulType.values()[soulOrd] : null;
      return new VoidMenuScreen.Member(
         1000 + ci,
         cn != null && ci < cn.length ? cn[ci] : "Vessel",
         at(TurnBattleClientMod.customRecruitLevels, ci, 1),
         st,
         pc,
         at(TurnBattleClientMod.customRecruitHp, ci),
         at(TurnBattleClientMod.customRecruitMaxHp, ci)
      );
   }

   protected VoidMenuScreen.Member member(int code) {
      for (VoidMenuScreen.Member m : this.roster()) {
         if (m.code() == code) {
            return m;
         }
      }

      return null;
   }

   protected Identifier skinOf(VoidMenuScreen.Member m) {
      if (m == null) {
         return null;
      } else if (m.isYou() && DepthsClient.inRun()) {
         try {
            return RecruitSkinManager.getSkin(Recruit.ORI.displayName);
         } catch (Throwable var3) {
            return null;
         }
      } else if (m.isYou()) {
         if (this.client != null && this.client.player != null) {
            if (this.client.getNetworkHandler() != null) {
               PlayerListEntry e = this.client.getNetworkHandler().getPlayerListEntry(this.client.player.getUuid());
               if (e != null) {
                  return e.getSkinTextures().texture();
               }
            }

            return this.client.player.getSkinTextures().texture();
         } else {
            return null;
         }
      } else {
         try {
            return RecruitSkinManager.getSkin(m.name());
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   protected CharStats statsOf(int code) {
      if (code == -1) {
         return TurnBattleClientMod.clientPlayerStats;
      } else if (code >= 1000) {
         CharStats[] a = TurnBattleClientMod.customRecruitStats;
         int ci = code - 1000;
         return a != null && ci < a.length && a[ci] != null ? a[ci] : new CharStats();
      } else {
         CharStats[] a = TurnBattleClientMod.clientRecruitStats;
         return a != null && code < a.length && a[code] != null ? a[code] : new CharStats();
      }
   }

   protected static UUID poseKey(VoidMenuScreen.Member m) {
      return UUID.nameUUIDFromBytes(("turnbattle:menu-pose:" + (m.isYou() ? "<you>" : m.name())).getBytes());
   }

   protected void drawMember(DrawContext ctx, VoidMenuScreen.Member m, int x, int footY, float scale, float yaw, ItemStack hold, float a) {
      if (this.client != null && this.client.world != null && this.client.player != null && m != null && !(a < 0.3F)) {
         Object e;
         try {
            e = (LivingEntity)(m.isYou() && DepthsClient.inRun()
               ? BattleEntityRenderer.recruitFake(Recruit.ORI.displayName)
               : (m.isYou() ? this.client.player : BattleEntityRenderer.recruitFake(m.name())));
         } catch (Throwable var15) {
            return;
         }

         if (e != null) {
            UUID key = poseKey(m);
            if (this.posed.add(key)) {
               CloneAnimator.setSoul(key, m.soul());
            }

            ItemStack held = hold != null ? hold : this.weaponOf(m.code());
            boolean armed = held != null && !held.isEmpty();
            MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, (float)footY, scale * 2.4F, scale * 0.55F, 0, 0.75F * a);
            ctx.draw();
            RenderSystem.clear(256, false);

            try {
               CloneAnimator.Frame fr = CloneAnimator.frame(key, armed);
               BattleEntityRenderer.renderEntity(
                  ctx, e, x, footY + (int)(scale * 0.35F), scale, yaw, BattleEntityRenderer.BattleAnim.IDLE, held, fr, yaw >= 0.0F ? 1 : -1
               );
            } catch (Throwable var14) {
            }

            ctx.draw();
            RenderSystem.clear(256, false);
         }
      }
   }

   protected void act(VoidMenuScreen.Member m, CloneAnimator.Act act, int rgb, ItemStack item) {
      if (m != null) {
         try {
            CloneAnimator.play(poseKey(m), act, rgb, item);
         } catch (Throwable var6) {
         }
      }
   }

   public void removed() {
      super.removed();

      for (UUID k : this.posed) {
         CloneAnimator.releaseMenuPoses(k);
      }

      try {
         PlayerActions.endLocalMenu();
      } catch (Throwable var3) {
      }
   }

   protected ItemStack weaponOf(int code) {
      if (code == -1) {
         return orEmpty(TurnBattleClientMod.clientEquippedWeapon);
      } else {
         return code >= 1000 ? orEmpty(at(TurnBattleClientMod.customRecruitWeapons, code - 1000)) : orEmpty(at(TurnBattleClientMod.clientRecruitWeapons, code));
      }
   }

   protected ItemStack accessoryOf(int code) {
      if (code == -1) {
         return orEmpty(TurnBattleClientMod.clientEquippedAccessory);
      } else {
         return code >= 1000
            ? orEmpty(at(TurnBattleClientMod.customRecruitAccessories, code - 1000))
            : orEmpty(at(TurnBattleClientMod.clientRecruitAccessories, code));
      }
   }

   protected ItemStack[] armorOf(int code) {
      ItemStack[] a;
      if (code == -1) {
         a = TurnBattleClientMod.clientEquippedArmor;
      } else if (code >= 1000) {
         a = at(TurnBattleClientMod.customRecruitArmor, code - 1000);
      } else {
         a = at(TurnBattleClientMod.clientRecruitArmor, code);
      }

      ItemStack[] out = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      if (a != null) {
         for (int i = 0; i < 2 && i < a.length; i++) {
            out[i] = orEmpty(a[i]);
         }
      }

      return out;
   }

   protected void hoverTag(float cx, float cy, List<String[]> lines, int accent) {
      this.pendingTag = new VoidMenuScreen.PendingTag(cx, cy, lines, accent);
   }

   private void drawTag(DrawContext ctx, VoidMenuScreen.PendingTag t) {
      int w = 0;
      List wrapped = new ArrayList<>();

      for (String[] l : t.lines()) {
         List wl = this.textRenderer.wrapLines(StringVisitable.plain(l[0]), 200);
         wrapped.add(wl);

         for (OrderedText o : wl) {
            w = Math.max(w, this.textRenderer.getWidth(o));
         }
      }

      w += 14;
      int h = 10;

      for (List wl : wrapped) {
         h += wl.size() * 10;
      }

      int x = (int)(t.cx() - (float)w / 2.0F);
      int y = (int)(t.cy() - (float)h);
      x = Math.max(4, Math.min(this.width - w - 4, x));
      if (y < this.bodyTop()) {
         y = (int)t.cy() + 30;
      }

      y = Math.min(y, this.height - h - 4);
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 420.0F);
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(329230, 0.94F));
      ctx.fill(x, y, x + w, y + 1, MenuBackdrop.argb(t.accent(), 1.0F));
      ctx.fill(x, y + h - 1, x + w, y + h, MenuBackdrop.argb(t.accent(), 0.5F));
      int ty = y + 5;

      for (int i = 0; i < wrapped.size(); i++) {
         int rgb = Integer.parseInt(t.lines().get(i)[1], 16);

         for (OrderedText o : wrapped.get(i)) {
            ctx.drawTextWithShadow(this.textRenderer, o, x + 7, ty, MenuBackdrop.argb(rgb, 1.0F));
            ty += 10;
         }
      }

      m.pop();
   }

   protected void renderRosterColumn(
      DrawContext ctx, List<VoidMenuScreen.Member> ros, int x, int y, int w, int h, int mx, int my, int selectedCode, IntConsumer pick
   ) {
      float t = this.secs();
      int cardH = 30;
      int gap = 4;

      for (int i = 0; i < ros.size(); i++) {
         VoidMenuScreen.Member m = ros.get(i);
         float a = ease(clamp01((t - 0.15F - (float)i * 0.05F) / 0.35F));
         if (!(a <= 0.01F)) {
            int cy = y + i * (cardH + gap);
            if (cy + cardH > y + h) {
               break;
            }

            int cx = x - (int)((1.0F - a) * 16.0F);
            boolean sel = m.code() == selectedCode;
            boolean hov = inside((double)mx, (double)my, cx, cy, w, cardH);
            if (hov && !sel) {
               this.hover("member" + m.code());
            }

            int col = m.color();
            this.panel(ctx, cx, cy, w, cardH, col, a, sel);
            if (sel) {
               ctx.fill(cx, cy, cx + 3, cy + cardH, MenuBackdrop.argb(col, a));
               MenuBackdrop.tex(ctx, TEX_GLOW, (float)(cx + 14), (float)cy + (float)cardH / 2.0F, 46.0F, 30.0F, col, 0.35F * a);
            } else if (hov) {
               ctx.fill(cx + 1, cy + 1, cx + w - 1, cy + cardH - 1, MenuBackdrop.argb(16777215, 0.06F * a));
            }

            this.face(ctx, this.skinOf(m), cx + 7, cy + 7, 16, a);
            int tx = cx + 28;
            ctx.drawTextWithShadow(
               this.textRenderer, this.fit(m.name(), w - 34 - 14), tx, cy + 6, MenuBackdrop.argb(sel ? 16777215 : (hov ? 16777024 : 13160411), a)
            );
            ctx.drawTextWithShadow(this.textRenderer, "LV " + m.level(), tx, cy + 17, MenuBackdrop.argb(9413560, a));
            ItemStack wpn = this.weaponOf(m.code());
            if (!wpn.isEmpty()) {
               this.item(ctx, wpn, (float)(cx + w - 11), (float)cy + (float)cardH / 2.0F, 12.0F);
            }

            int code = m.code();
            this.click(cx, cy, w, cardH, () -> pick.accept(code));
         }
      }
   }

   protected static ItemStack orEmpty(ItemStack s) {
      return s == null ? ItemStack.EMPTY : s;
   }

   protected static <T> T at(T[] a, int i) {
      return a != null && i >= 0 && i < a.length ? a[i] : null;
   }

   protected static int at(int[] a, int i, int def) {
      return a != null && i >= 0 && i < a.length ? a[i] : def;
   }

   protected static float at(float[] a, int i) {
      return a != null && i >= 0 && i < a.length ? a[i] : 0.0F;
   }

   protected static int lerpRgb(int a, int b, float t) {
      int r = (int)((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int g = (int)((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = (int)((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | g << 8 | bl;
   }

   protected static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }

   protected static float clamp(float v, float lo, float hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   protected static float ease(float t) {
      float u = 1.0F - clamp01(t);
      return 1.0F - u * u * u;
   }

   protected static float backOut(float t) {
      t = clamp01(t);
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow((double)(t - 1.0F), 3.0) + c1 * (float)Math.pow((double)(t - 1.0F), 2.0);
   }

   protected float approach(float cur, float target, float speed) {
      return cur + (target - cur) * (1.0F - (float)Math.exp((double)(-this.dt * speed)));
   }

   protected static String pretty(String s) {
      if (s != null && !s.isEmpty()) {
         String t = s.replace('_', ' ').toLowerCase(Locale.ROOT);
         return Character.toUpperCase(t.charAt(0)) + t.substring(1);
      } else {
         return "";
      }
   }

   protected static record Hit(int x, int y, int w, int h, Runnable action, boolean right) {
   }

   protected static record Member(int code, String name, int level, SoulType soul, PlayerClass cls, float hp, float maxHp) {
      boolean isYou() {
         return this.code == -1;
      }

      boolean isCustom() {
         return this.code >= 1000;
      }

      int color() {
         return this.soul != null ? this.soul.getColor() & 16777215 : 12107984;
      }
   }

   protected static record NavTab(String label, int id) {
   }

   private static record PendingTag(float cx, float cy, List<String[]> lines, int accent) {
   }

   protected static record Popup(String text, float x, float y, int rgb, long born) {
   }

   protected static record Pulse(float x, float y, int rgb, long born, float size) {
   }

   protected static record Scroll(int x, int y, int w, int h, DoubleConsumer by) {
   }
}
