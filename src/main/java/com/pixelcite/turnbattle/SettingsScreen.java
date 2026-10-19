package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class SettingsScreen extends VoidMenuScreen {
   private static final int WHITE = 16777215;
   private static final int YELLOW = 16776960;
   private static final int GREY = 8421504;
   private static final int DIM = 4210752;
   private static final int LGREY = 11842740;
   private static final Style NUM = Style.EMPTY.withFont(DAMAGE_FONT);
   static volatile boolean remoteKnown = false;
   static volatile boolean remoteCanEdit = false;
   static volatile List<SettingsScreen.Remote> remote = List.of();
   private String openDrop = null;
   private SettingsScreen.Drop openDropRow = null;
   private int dropX;
   private int dropY;
   private int dropW;
   private int section = 0;
   private float scroll;
   private float scrollTarget;
   private int contentH;
   private int viewH;

   static SettingsScreen.Sync readSync(PacketByteBuf buf) {
      boolean can = buf.readBoolean();
      List out = new ArrayList<>();
      if (can) {
         int n = buf.readInt();

         for (int i = 0; i < n && i < 64; i++) {
            String key = buf.readString(64);
            String label = buf.readString(64);
            String desc = buf.readString(512);
            int k = buf.readByte();
            ServerSettings.Kind[] kinds = ServerSettings.Kind.values();
            ServerSettings.Kind kind = k >= 0 && k < kinds.length ? kinds[k] : ServerSettings.Kind.INT;
            float v = buf.readFloat();
            float min = buf.readFloat();
            float max = buf.readFloat();
            float step = buf.readFloat();
            boolean locked = buf.readBoolean();
            out.add(new SettingsScreen.Remote(key, label, desc, kind, v, min, max, step, locked));
         }
      }

      return new SettingsScreen.Sync(can, out);
   }

   static void onSync(SettingsScreen.Sync s) {
      remoteCanEdit = s.canEdit();
      remote = List.copyOf(s.entries());
      remoteKnown = true;
   }

   private static void sendSet(SettingsScreen.Remote r, float v) {
      r.value = v;
      PacketByteBuf b = PacketByteBufs.create();
      b.writeString(r.key, 64);
      b.writeFloat(v);
      Net121.sendToServer(ServerSettings.SET, b);
   }

   private List<SettingsScreen.Row> localRows() {
      List out = new ArrayList<>();
      boolean ebUi = TurnBattleClientMod.battleUi == BattleUi.EARTHBOUND;
      out.add(
         new SettingsScreen.Toggle(
            "Light World UI Change",
            "Changes the classic battle screen to a Light World one, a green grid behind the enemies, who stand centre stage facing you, no clones of your party, and the battle log in a thinner box above your party's columns along the bottom, battles in a Dark World keep the Dark World's screen"
               + (ebUi ? ", does nothing while the Battle UI is EarthBound" : ""),
            () -> TurnBattleClientMod.lightWorldUi,
            v -> {
               TurnBattleClientMod.lightWorldUi = v;
               TurnBattleClientMod.saveClientPrefs();
            },
            ebUi
         )
      );
      BattleUi[] uis = BattleUi.values();
      String[] uiNames = new String[uis.length];

      for (int k = 0; k < uis.length; k++) {
         uiNames[k] = uis[k].displayName;
      }

      out.add(
         new SettingsScreen.Drop(
            "Battle UI",
            ebUi
               ? "EarthBound, a swirling backdrop, the enemies centre stage facing you, a command window in the top left, and a status box for each of your party along the bottom with a rolling HP odometer, made to be played with Rolling Health"
               : "Deltarune, the Dark World's battle screen, or the Light World's with Light World UI Change on",
            uiNames,
            () -> TurnBattleClientMod.battleUi.ordinal(),
            v -> {
               TurnBattleClientMod.battleUi = uis[Math.max(0, Math.min(uis.length - 1, v))];
               TurnBattleClientMod.saveClientPrefs();
            }
         )
      );
      BattleStyle bs = TurnBattleClientMod.clientBattleStyle;
      BattleView bv = TurnBattleClientMod.clientBattleView;
      BattleStyle[] styles = BattleStyle.values();
      out.add(
         new SettingsScreen.Cycle(
            "Battle Style",
            bs.description + " Only changes battles you start",
            bs.displayName,
            () -> this.setBattlePrefs(styles[(bs.ordinal() + 1) % styles.length], bv),
            () -> this.setBattlePrefs(styles[(bs.ordinal() + styles.length - 1) % styles.length], bv)
         )
      );
      if (bs.hasViewChoice()) {
      }

      out.add(
         new SettingsScreen.Toggle(
            "Battle Background",
            "The animated Dark World backdrop behind classic battles, off for plain dark, the Light World UI always has its grid",
            () -> TurnBattleConfig.get().deltarune_battle_bg,
            v -> {
               TurnBattleConfig.get().deltarune_battle_bg = v;
               TurnBattleConfig.save();
            }
         )
      );
      out.add(
         new SettingsScreen.Toggle(
            "Reverse Battle Layout",
            "Mirrors the classic battle screen, your party on the right, the enemies and the TP bar on the left",
            () -> TurnBattleConfig.get().reverseBattleLayout,
            v -> {
               TurnBattleConfig.get().reverseBattleLayout = v;
               TurnBattleConfig.save();
            }
         )
      );
      out.add(
         new SettingsScreen.Num(
            "Effect Pixel Size",
            "How chunky skill and attack effects are drawn, 0 is smooth, 2 is the default, higher is chunkier",
            () -> (float)TurnBattleConfig.get().vfxPixelSize,
            v -> {
               TurnBattleConfig.get().vfxPixelSize = Math.round(v);
               TurnBattleConfig.get().clamp();
               TurnBattleConfig.save();
            },
            0.0F,
            6.0F,
            1.0F,
            false
         )
      );
      out.add(
         new SettingsScreen.Toggle(
            "Live Combat First Person",
            "While locked on in Live Combat, fight in first person instead of over the shoulder, F5 switches it too",
            () -> TurnBattleConfig.get().liveLockFirstPerson,
            v -> {
               TurnBattleConfig.get().liveLockFirstPerson = v;
               TurnBattleConfig.save();
            }
         )
      );
      out.add(
         new SettingsScreen.Toggle(
            "Battle Debug Counters",
            "The FPS, TPS and delta readout in the top right corner of the battle screen",
            () -> TurnBattleClientMod.showBattleDebug,
            v -> {
               TurnBattleClientMod.showBattleDebug = v;
               TurnBattleClientMod.saveClientPrefs();
            }
         )
      );
      out.add(
         new SettingsScreen.Toggle(
            "Allow GUI Scale 4", "Stops Soulbound turning a GUI scale of 4 down to 3, some of its screens may not fit", () -> GuiScaleGuard.allowed, v -> {
               GuiScaleGuard.allowed = v;
               TurnBattleClientMod.saveClientPrefs();
            }
         )
      );
      return out;
   }

   private List<SettingsScreen.Row> serverRows() {
      List out = new ArrayList<>();

      for (SettingsScreen.Remote r : remote) {
         if (r.kind == ServerSettings.Kind.BOOL) {
            out.add(new SettingsScreen.Toggle(r.label, r.desc, () -> r.value >= 0.5F, v -> sendSet(r, v ? 1.0F : 0.0F), r.locked));
         } else {
            out.add(new SettingsScreen.Num(r.label, r.desc, () -> r.value, v -> sendSet(r, v), r.min, r.max, r.step, r.kind == ServerSettings.Kind.FLOAT));
         }
      }

      return out;
   }

   private void setBattlePrefs(BattleStyle style, BattleView view) {
      TurnBattleClientMod.clientBattleStyle = style;
      TurnBattleClientMod.clientBattleView = view;
      PacketByteBuf b = PacketByteBufs.create();
      b.writeInt(style.ordinal());
      b.writeInt(view.ordinal());
      Net121.sendToServer(TurnBattleMod.BATTLE_PREFS, b);
   }

   public SettingsScreen(Screen parent) {
      super("Settings", parent);
   }

   @Override
   protected int navIndex() {
      return 7;
   }

   @Override
   protected void onFirstOpen() {
      remoteKnown = false;
      Net121.sendToServer(ServerSettings.REQUEST, PacketByteBufs.empty());
   }

   @Override
   protected String headerRight() {
      return remoteKnown && remoteCanEdit ? "OPERATOR" : "";
   }

   private void pick(int s) {
      if (s != this.section) {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
         this.section = s;
         this.scroll = this.scrollTarget = 0.0F;
      }
   }

   private void drBox(DrawContext ctx, int x, int y, int w, int h, float a) {
      if (DarkBox.inDarkWorld()) {
         DarkBox.draw(ctx, x, y, x + w, y + h, a);
      } else {
         ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(0, 0.94F * a));
         int b = MenuBackdrop.argb(16777215, a);
         ctx.fill(x, y, x + w, y + 2, b);
         ctx.fill(x, y + h - 2, x + w, y + h, b);
         ctx.fill(x, y, x + 2, y + h, b);
         ctx.fill(x + w - 2, y, x + w, y + h, b);
      }
   }

   @Override
   protected void renderBody(DrawContext ctx, int mx, int my) {
      float in = ease(clamp01((this.secs() - 0.05F) / 0.3F));
      this.renderVoid(ctx, (float)this.width / 2.0F, (float)(this.height + 400), 1.0F);
      if (this.section == 1 && (!remoteKnown || !remoteCanEdit)) {
         this.section = 0;
      }

      boolean server = remoteKnown && remoteCanEdit;
      int top = this.bodyTop();
      int pw = Math.min(this.width - 16, 460);
      int px = this.width / 2 - pw / 2;
      int tabsH = 20;
      String[] names = server ? new String[]{"THIS GAME", "SERVER"} : new String[]{"THIS GAME"};
      int gap = 6;
      int tw = Math.min(150, (pw - gap * (names.length - 1)) / names.length);
      int tx0 = px + pw / 2 - (tw * names.length + gap * (names.length - 1)) / 2;

      for (int i = 0; i < names.length; i++) {
         int x = tx0 + i * (tw + gap);
         boolean sel = i == this.section;
         boolean hov = !sel && inside((double)mx, (double)my, x, top, tw, tabsH);
         if (hov) {
            this.hover("sec" + i);
         }

         ctx.fill(x, top, x + tw, top + tabsH, MenuBackdrop.argb(0, 0.94F * in));
         int b = MenuBackdrop.argb(sel ? 16777215 : (hov ? 16776960 : 4210752), in);
         ctx.fill(x, top, x + tw, top + 1, b);
         ctx.fill(x, top + tabsH - 1, x + tw, top + tabsH, b);
         ctx.fill(x, top, x + 1, top + tabsH, b);
         ctx.fill(x + tw - 1, top, x + tw, top + tabsH, b);
         int lw = this.textRenderer.getWidth(names[i]);
         int lx = x + tw / 2 - lw / 2 + (sel ? 5 : 0);
         if (sel) {
            this.heart(ctx, (float)(lx - 12), (float)top + (float)(tabsH - 8) / 2.0F, 8, in);
         }

         ctx.drawTextWithShadow(this.textRenderer, names[i], lx, top + (tabsH - 8) / 2, MenuBackdrop.argb(sel ? 16777215 : (hov ? 16776960 : 8421504), in));
         int si = i;
         this.click(x, top, tw, tabsH, () -> this.pick(si));
      }

      int by = top + tabsH + 6;
      int bh = this.height - 8 - by;
      this.drBox(ctx, px, by, pw, bh, in);
      String note = this.section == 0 ? "Only for you, saved on this computer" : "The server's config, saved for everyone, only operators can change it";
      ctx.drawTextWithShadow(this.textRenderer, this.fit(note, pw - 20), px + 10, by + 7, MenuBackdrop.argb(8421504, in));
      int listTop = by + 20;
      int listBottom = by + bh - 4;
      this.viewH = listBottom - listTop;
      List rows = this.section == 0 ? this.localRows() : this.serverRows();
      this.scroll = this.scroll + (this.scrollTarget - this.scroll) * Math.min(1.0F, this.dt * 14.0F);
      int y = listTop - Math.round(this.scroll);
      int rx = px + 22;
      int rw = pw - 34;
      this.openDropRow = null;
      ctx.enableScissor(px + 2, listTop, px + pw - 2, listBottom);
      int startY = y;

      for (int i = 0; i < rows.size(); i++) {
         SettingsScreen.Row r = rows.get(i);
         int h = this.drawRow(ctx, r, i, rx, y, rw, listTop, listBottom, mx, my, in);
         y += h;
         if (i < rows.size() - 1) {
            ctx.fill(rx, y - 3, rx + rw, y - 2, MenuBackdrop.argb(4210752, 0.6F * in));
         }
      }

      if (this.section == 1 && rows.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, "Asking the server...", rx, listTop + 6, MenuBackdrop.argb(8421504, in));
      }

      ctx.disableScissor();
      this.contentH = y - startY;
      if (this.openDropRow == null) {
         this.openDrop = null;
      } else {
         this.renderDropList(ctx, this.openDropRow, mx, my, in);
      }

      int maxScroll = Math.max(0, this.contentH - this.viewH);
      this.scrollTarget = clamp(this.scrollTarget, 0.0F, (float)maxScroll);
      this.scrollArea(
         px,
         listTop,
         pw,
         this.viewH,
         by2 -> this.scrollTarget = clamp(this.scrollTarget - (float)by2 * 24.0F, 0.0F, (float)Math.max(0, this.contentH - this.viewH))
      );
      if (maxScroll > 0) {
         int trackH = this.viewH - 4;
         int thumbH = Math.max(12, Math.round((float)trackH * ((float)this.viewH / (float)this.contentH)));
         int thumbY = listTop + 2 + Math.round((float)(trackH - thumbH) * (this.scroll / (float)maxScroll));
         ctx.fill(px + pw - 7, listTop + 2, px + pw - 5, listTop + 2 + trackH, MenuBackdrop.argb(4210752, in));
         ctx.fill(px + pw - 7, thumbY, px + pw - 5, thumbY + thumbH, MenuBackdrop.argb(16777215, in));
      }
   }

   private int drawRow(DrawContext ctx, SettingsScreen.Row r, int i, int x, int y, int w, int clipTop, int clipBottom, int mx, int my, float a) {
      int ctrlW = r instanceof SettingsScreen.Num ? 70 : (!(r instanceof SettingsScreen.Cycle) && !(r instanceof SettingsScreen.Drop) ? 40 : 110);
      List lines = this.textRenderer.wrapLines(StringVisitable.plain(r.desc()), w - 4);
      int h = 16 + lines.size() * 10 + 6;
      boolean visible = y + 12 >= clipTop && y + 2 <= clipBottom - 10;
      boolean onRow = visible && inside((double)mx, (double)my, x - 14, y, w + 14, h - 4) && my >= clipTop && my < clipBottom;
      if (onRow) {
         this.hover("row" + this.section + ":" + i);
         this.heart(ctx, (float)(x - 13), (float)(y + 4), 8, a);
      }

      boolean var10000;
      label231: {
         if (r instanceof SettingsScreen.Toggle tg && tg.locked()) {
            var10000 = true;
            break label231;
         }

         var10000 = false;
      }

      boolean locked = var10000;
      ctx.drawTextWithShadow(
         this.textRenderer, this.fit(r.label(), w - ctrlW - 8), x, y + 4, MenuBackdrop.argb(locked ? 8421504 : (onRow ? 16776960 : 16777215), a)
      );
      int ly = y + 16;

      for (OrderedText line : lines) {
         ctx.drawTextWithShadow(this.textRenderer, line, x, ly, MenuBackdrop.argb(locked ? 8421504 : 11842740, (locked ? 0.7F : 0.85F) * a));
         ly += 10;
      }

      int cx = x + w - ctrlW;
      int cy = y + 2;
      boolean clickable = visible && cy >= clipTop - 2 && cy + 12 <= clipBottom + 2;
      Objects.requireNonNull(r);
      switch (r) {
         case SettingsScreen.Toggle t:
            boolean on = t.get().getAsBoolean() && !t.locked();
            int sw = 30;
            int sh = 12;
            int sx = x + w - sw - 2;
            int lit = t.locked() ? 4210752 : (on ? 16776960 : 8421504);
            ctx.fill(sx, cy, sx + sw, cy + sh, MenuBackdrop.argb(on ? 3815936 : 1710618, a));
            int edge = MenuBackdrop.argb(lit, a);
            ctx.fill(sx, cy, sx + sw, cy + 1, edge);
            ctx.fill(sx, cy + sh - 1, sx + sw, cy + sh, edge);
            ctx.fill(sx, cy, sx + 1, cy + sh, edge);
            ctx.fill(sx + sw - 1, cy, sx + sw, cy + sh, edge);
            int kx = on ? sx + sw - 11 : sx + 1;
            ctx.fill(kx + 1, cy + 2, kx + 9, cy + sh - 2, MenuBackdrop.argb(lit, a));
            String st = t.locked() ? "LOCKED" : (on ? "ON" : "OFF");
            ctx.drawTextWithShadow(this.textRenderer, st, sx - 4 - this.textRenderer.getWidth(st), cy + 2, MenuBackdrop.argb(t.locked() ? 8421504 : lit, a));
            if (clickable) {
               this.click(x - 14, y, w + 14, 14, () -> {
                  if (t.locked()) {
                     TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 0.6F, 1.2F);
                  } else {
                     TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
                     t.set().accept(!on);
                  }
               });
            }
            break;
         case SettingsScreen.Num n:
            float v = n.get().get();
            String txtx = n.decimal() ? String.format(Locale.ROOT, "%.1f", v) : String.valueOf(Math.round(v));
            Text val = Text.literal(txtx).setStyle(NUM);
            int bw = 12;
            int lx = x + w - ctrlW;
            int rxx = x + w - bw;
            boolean canDown = v > n.min() + 1.0E-4F;
            boolean canUp = v < n.max() - 1.0E-4F;
            boolean hl = clickable && inside((double)mx, (double)my, lx, cy, bw, 12);
            boolean hr = clickable && inside((double)mx, (double)my, rxx, cy, bw, 12);
            if (hl) {
               this.hover("dn" + this.section + ":" + i);
            }

            if (hr) {
               this.hover("up" + this.section + ":" + i);
            }

            ctx.drawTextWithShadow(this.textRenderer, "<", lx + 3, cy + 2, MenuBackdrop.argb(!canDown ? 4210752 : (hl ? 16776960 : 16777215), a));
            ctx.drawTextWithShadow(this.textRenderer, ">", rxx + 3, cy + 2, MenuBackdrop.argb(!canUp ? 4210752 : (hr ? 16776960 : 16777215), a));
            int vwx = this.textRenderer.getWidth(val);
            ctx.drawTextWithShadow(this.textRenderer, val, lx + bw + (ctrlW - 2 * bw - vwx) / 2, cy + 2, MenuBackdrop.argb(16777215, a));
            if (clickable) {
               this.click(lx, cy - 1, bw, 14, () -> this.step(n, v, -1, canDown));
               this.click(rxx, cy - 1, bw, 14, () -> this.step(n, v, 1, canUp));
               this.rightClick(x - 14, y, w + 14, h - 4, () -> this.step(n, v, -1, canDown));
            }
            break;
         case SettingsScreen.Drop d:
            boolean open = d.label().equals(this.openDrop);
            int cur = d.get().getAsInt();
            String valx = cur >= 0 && cur < d.options().length ? d.options()[cur] : "?";
            int bxx = x + w - ctrlW;
            int bww = ctrlW - 2;
            int bhh = 13;
            boolean onBox = clickable && inside((double)mx, (double)my, bxx, cy - 1, bww, bhh);
            ctx.fill(bxx, cy - 1, bxx + bww, cy - 1 + bhh, MenuBackdrop.argb(0, a));
            int e = MenuBackdrop.argb(!open && !onBox ? 16777215 : 16776960, a);
            ctx.fill(bxx, cy - 1, bxx + bww, cy, e);
            ctx.fill(bxx, cy - 2 + bhh, bxx + bww, cy - 1 + bhh, e);
            ctx.fill(bxx, cy - 1, bxx + 1, cy - 1 + bhh, e);
            ctx.fill(bxx + bww - 1, cy - 1, bxx + bww, cy - 1 + bhh, e);
            ctx.drawTextWithShadow(this.textRenderer, this.fit(valx, bww - 16), bxx + 4, cy + 2, MenuBackdrop.argb(16777215, a));
            ctx.drawTextWithShadow(this.textRenderer, open ? "^" : "v", bxx + bww - 9, cy + 2, MenuBackdrop.argb(open ? 16776960 : 8421504, a));
            if (open) {
               this.openDropRow = d;
               this.dropX = bxx;
               this.dropY = cy - 1 + bhh;
               this.dropW = bww;
            }

            if (clickable) {
               this.click(x - 14, y, w + 14, 14, () -> {
                  TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
                  this.openDrop = open ? null : d.label();
               });
            }
            break;
         case SettingsScreen.Cycle c:
            String txt = this.fit(c.value(), ctrlW - 12) + " >";
            int vw = this.textRenderer.getWidth(txt);
            ctx.drawTextWithShadow(this.textRenderer, txt, x + w - vw - 2, cy + 2, MenuBackdrop.argb(onRow ? 16776960 : 16777215, a));
            if (clickable) {
               this.click(x - 14, y, w + 14, 14, () -> {
                  TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
                  c.next().run();
               });
               this.rightClick(x - 14, y, w + 14, h - 4, () -> {
                  TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
                  c.prev().run();
               });
            }
            break;
         default:
            throw new MatchException(null, null);
      }

      return h;
   }

   private void renderDropList(DrawContext ctx, SettingsScreen.Drop d, int mx, int my, float a) {
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 300.0F);
      int rowH = 13;
      int n = d.options().length;
      int x = this.dropX;
      int y = this.dropY;
      int w = this.dropW;
      int h = n * rowH + 2;
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(0, 0.97F * a));
      int e = MenuBackdrop.argb(16776960, a);
      ctx.fill(x, y + h - 1, x + w, y + h, e);
      ctx.fill(x, y, x + 1, y + h, e);
      ctx.fill(x + w - 1, y, x + w, y + h, e);
      int cur = d.get().getAsInt();

      for (int k = 0; k < n; k++) {
         int ry = y + 1 + k * rowH;
         boolean on = inside((double)mx, (double)my, x, ry, w, rowH);
         if (on) {
            this.hover("drop" + k);
            ctx.fill(x + 1, ry, x + w - 1, ry + rowH, MenuBackdrop.argb(3815936, a));
         }

         if (k == cur) {
            this.heart(ctx, (float)(x + 3), (float)ry + 2.5F, 8, a);
         }

         ctx.drawTextWithShadow(
            this.textRenderer, this.fit(d.options()[k], w - 18), x + 14, ry + 3, MenuBackdrop.argb(on ? 16776960 : (k == cur ? 16777215 : 11842740), a)
         );
         int pickK = k;
         this.click(x, ry, w, rowH, () -> {
            TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
            d.set().accept(pickK);
            this.openDrop = null;
         });
      }

      m.pop();
   }

   private void step(SettingsScreen.Num n, float v, int dir, boolean can) {
      if (!can) {
         TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 0.6F, 1.2F);
      } else {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
         float next = Math.max(n.min(), Math.min(n.max(), v + (float)dir * n.step()));
         if (n.decimal()) {
            next = (float)Math.round(next * 10.0F) / 10.0F;
         }

         n.set().accept(next);
      }
   }

   private static record Cycle(String label, String desc, String value, Runnable next, Runnable prev) implements SettingsScreen.Row {
   }

   private static record Drop(String label, String desc, String[] options, IntSupplier get, IntConsumer set) implements SettingsScreen.Row {
   }

   private static record Num(String label, String desc, Supplier<Float> get, Consumer<Float> set, float min, float max, float step, boolean decimal)
      implements SettingsScreen.Row {
   }

   static final class Remote {
      final String key;
      final String label;
      final String desc;
      final ServerSettings.Kind kind;
      float value;
      final float min;
      final float max;
      final float step;
      final boolean locked;

      Remote(String key, String label, String desc, ServerSettings.Kind kind, float value, float min, float max, float step, boolean locked) {
         this.key = key;
         this.label = label;
         this.desc = desc;
         this.kind = kind;
         this.value = value;
         this.min = min;
         this.max = max;
         this.step = step;
         this.locked = locked;
      }
   }

   private sealed interface Row permits SettingsScreen.Toggle, SettingsScreen.Num, SettingsScreen.Cycle, SettingsScreen.Drop {
      String label();

      String desc();
   }

   static record Sync(boolean canEdit, List<SettingsScreen.Remote> entries) {
   }

   private static record Toggle(String label, String desc, BooleanSupplier get, Consumer<Boolean> set, boolean locked) implements SettingsScreen.Row {
      Toggle(String label, String desc, BooleanSupplier get, Consumer<Boolean> set) {
         this(label, desc, get, set, false);
      }
   }
}
