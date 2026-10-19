package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class EquipScreen extends VoidMenuScreen {
   private static final Identifier[] STAT_ICONS = new Identifier[]{
      gui("spr_offenseicon.png"),
      gui("spr_defenseicon.png"),
      gui("spr_magicicon.png"),
      gui("spr_iqicon.png"),
      gui("spr_vitalityicon.png"),
      gui("spr_luckicon.png")
   };
   private static final int[][] STAT_ICON_SIZE = new int[][]{{9, 10}, {9, 10}, {9, 10}, {9, 9}, {9, 10}, {9, 10}};
   private static final String[] STAT_SHORT = new String[]{"OFF", "DEF", "MAG", "IQ", "VIT", "LCK"};
   private static final int[] STAT_RGB = new int[]{16751178, 8370431, 12946687, 8388564, 8257418, 16769126};
   private static final Identifier ICON_WEAPON = gui("spr_weaponicon.png");
   private static final Identifier ICON_ACC = gui("spr_accessoryicon.png");
   private static final Identifier ICON_ARMOR1 = gui("spr_armoricon1.png");
   private static final Identifier ICON_ARMOR2 = gui("spr_armoricon2.png");
   private static final String[] SLOT_NAMES = new String[]{"WEAPON", "ACCESSORY", "ARMOR I", "ARMOR II"};
   private ItemStack[] bagWeapons = new ItemStack[0];
   private ItemStack[] bagAcc = new ItemStack[0];
   private ItemStack[] bagArmor = new ItemStack[0];
   private int[] bagWeaponSlots = new int[0];
   private int[] bagAccSlots = new int[0];
   private int[] bagArmorSlots = new int[0];
   private boolean gotWeapons;
   private boolean gotAcc;
   private boolean gotArmor;
   private int selected = -1;
   private int slot = 0;
   private long memberPickedNanos;
   private long slotPickedNanos;
   private float listScroll;
   private float listScrollTarget;
   private int previewRow = -1;
   private final float[] socketLight = new float[4];
   private float figureYaw = 18.0F;
   private final float[][] socketPos = new float[4][2];
   private float figX;
   private float figY;
   private float figScale;

   private static Identifier gui(String n) {
      return Identifier.of("turnbattle", "textures/gui/" + n);
   }

   public EquipScreen(Screen parent) {
      super("Equip", parent);
   }

   @Override
   protected int navIndex() {
      return 3;
   }

   @Override
   protected void onFirstOpen() {
      this.memberPickedNanos = this.slotPickedNanos = System.nanoTime();
      this.refresh();
   }

   private void refresh() {
      Net121.sendToServer(TurnBattleMod.EQUIP_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.ACCESSORY_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.ARMOR_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
   }

   public void onEquipInfo(int[] slots, ItemStack[] stacks) {
      this.bagWeaponSlots = slots;
      this.bagWeapons = stacks;
      this.gotWeapons = true;
   }

   public void onAccessoryInfo(int[] slots, ItemStack[] stacks) {
      this.bagAccSlots = slots;
      this.bagAcc = stacks;
      this.gotAcc = true;
   }

   public void onArmorInfo(int[] slots, ItemStack[] stacks) {
      this.bagArmorSlots = slots;
      this.bagArmor = stacks;
      this.gotArmor = true;
   }

   @Override
   protected String headerRight() {
      VoidMenuScreen.Member m = this.member(this.selected);
      return m == null ? "" : m.name() + "  LV " + m.level();
   }

   @Override
   protected void renderBody(DrawContext ctx, int mx, int my) {
      List ros = this.roster();
      VoidMenuScreen.Member sel = this.member(this.selected);
      if (sel == null) {
         this.selected = -1;
         sel = ros.get(0);
      }

      int top = this.bodyTop();
      int leftW = (int)clamp((float)this.width * 0.2F, 92.0F, 140.0F);
      int rightW = (int)clamp((float)this.width * 0.34F, 160.0F, 250.0F);
      int lx = 10;
      int rx = this.width - rightW - 10;
      int sx0 = lx + leftW + 12;
      int sx1 = rx - 12;
      int stageW = Math.max(60, sx1 - sx0);
      float scx = (float)(sx0 + sx1) / 2.0F;
      boolean compact = stageW / 7 < 38;
      int statsH = compact ? 34 : 30;
      int statsY = this.height - statsH - 10;
      int nameH = 22;
      float avail = (float)(statsY - 14 - (top + nameH));
      this.figScale = clamp(Math.min(avail / 2.15F, (float)stageW * 0.2F), 12.0F, 64.0F);
      int footY = statsY - 14;
      this.figX = scx;
      this.figY = (float)footY;
      this.renderVoid(ctx, scx, (float)footY, (float)stageW * 1.2F);
      List rows = this.rowsFor(this.slot);
      EquipScreen.Row prev = this.previewRow >= 0 && this.previewRow < rows.size() ? rows.get(this.previewRow) : null;
      this.previewRow = -1;
      this.renderRoster(ctx, ros, lx, top, leftW, this.height - top - 10, mx, my);
      this.renderStage(ctx, sel, sx0, sx1, top, footY, mx, my, prev);
      this.renderStats(ctx, sel, sx0, statsY, stageW, statsH, prev, compact);
      this.renderBag(ctx, sel, rows, rx, top, rightW, this.height - top - 10, mx, my);
   }

   private void renderRoster(DrawContext ctx, List<VoidMenuScreen.Member> ros, int x, int y, int w, int h, int mx, int my) {
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
            boolean sel = m.code() == this.selected;
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
            this.click(cx, cy, w, cardH, () -> this.pickMember(code));
         }
      }
   }

   private void pickMember(int code) {
      if (code != this.selected) {
         TurnBattleSounds.playSelect();
         this.selected = code;
         this.memberPickedNanos = System.nanoTime();
         this.listScroll = this.listScrollTarget = 0.0F;
      }
   }

   private void pickSlot(int s) {
      if (s != this.slot) {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
         this.slot = s;
         this.slotPickedNanos = System.nanoTime();
         this.listScroll = this.listScrollTarget = 0.0F;
      }
   }

   private void renderStage(DrawContext ctx, VoidMenuScreen.Member sel, int sx0, int sx1, int top, int footY, int mx, int my, EquipScreen.Row prev) {
      float t = this.secs();
      float in = ease(clamp01((t - 0.1F) / 0.5F));
      float swap = ease(clamp01(since(this.memberPickedNanos) / 0.35F));
      float scx = (float)(sx0 + sx1) / 2.0F;
      int col = sel.color();
      String cls = sel.cls() != null ? sel.cls().displayName : "";
      Text name = Text.literal(sel.name()).setStyle(Style.EMPTY.withBold(true));
      int nw = this.textRenderer.getWidth(name);
      this.soulHeart(ctx, sel.soul(), scx - (float)nw / 2.0F - 13.0F, (float)(top + 1), 9, in * swap);
      ctx.drawTextWithShadow(this.textRenderer, name, (int)(scx - (float)nw / 2.0F), top + 1, MenuBackdrop.argb(lerpRgb(col, 16777215, 0.45F), in * swap));
      String sub = "LV " + sel.level() + (cls.isEmpty() ? "" : "  \u00b7  " + cls);
      ctx.drawCenteredTextWithShadow(this.textRenderer, sub, (int)scx, top + 12, MenuBackdrop.argb(9413560, in * swap));
      float s = this.figScale;
      MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY, s * 4.2F, s * 1.0F, col, 0.55F * in);
      MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY, s * 1.6F, s * 0.35F, 16777215, 0.35F * in);
      MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY - s * 1.1F, s * 3.2F, s * 3.6F, col, 0.1F * in);
      float spread = Math.min((float)(sx1 - sx0) * 0.38F, s * 2.1F);
      float yU = (float)footY - s * 1.45F;
      float yL = (float)footY - s * 0.55F;
      float[][] pos = new float[][]{{scx - spread, yU}, {scx + spread, yU}, {scx - spread, yL}, {scx + spread, yL}};
      ItemStack[] armor = this.armorOf(sel.code());
      ItemStack[] onNow = new ItemStack[]{this.weaponOf(sel.code()), this.accessoryOf(sel.code()), armor[0], armor[1]};
      int hoveredSocket = -1;

      for (int i = 0; i < 4; i++) {
         float a = ease(clamp01((t - 0.3F - (float)i * 0.06F) / 0.35F));
         if (!(a <= 0.01F)) {
            float px = pos[i][0];
            float py = pos[i][1];
            this.socketPos[i][0] = px;
            this.socketPos[i][1] = py;
            int half = 14;
            boolean hov = inside((double)mx, (double)my, (int)px - half, (int)py - half, half * 2, half * 2);
            if (hov) {
               hoveredSocket = i;
               this.hover("socket" + i);
            }

            boolean chosen = i == this.slot;
            this.socketLight[i] = this.approach(this.socketLight[i], chosen ? 1.0F : (hov ? 0.6F : 0.0F), 10.0F);
            float lit = this.socketLight[i];
            float hx = scx + (px < scx ? -s * 0.28F : s * 0.28F);
            float hy = py + (i < 2 ? s * 0.15F : 0.0F);
            float nearX = px + (float)(px < scx ? half : -half);
            CloneAnimator.line(ctx, nearX, py, hx, hy, 1.1F + lit, MenuBackdrop.argb(lerpRgb(col, 16777215, lit * 0.5F), (0.25F + 0.5F * lit) * a));
            float q = (t * 0.5F + (float)i * 0.25F) % 1.0F;
            CloneAnimator.star4(
               ctx,
               nearX + (hx - nearX) * q,
               py + (hy - py) * q,
               1.4F,
               MenuBackdrop.argb(16777215, 0.6F * a * (float)Math.sin(Math.PI * (double)q) * (0.4F + lit))
            );
            MenuBackdrop.tex(ctx, TEX_GLOW, px, py, 62.0F, 62.0F, col, (0.18F + 0.35F * lit) * a);
            if (chosen) {
               float rq = t * 0.7F % 1.0F;
               MenuBackdrop.tex(ctx, TEX_RING, px, py, 40.0F + rq * 26.0F, 40.0F + rq * 26.0F, col, 0.5F * (1.0F - rq) * a);
            }

            int bx = (int)px - half;
            int by = (int)py - half;
            ctx.fill(
               bx - 1, by - 1, bx + half * 2 + 1, by + half * 2 + 1, MenuBackdrop.argb(chosen ? 16777215 : (hov ? 16777024 : lerpRgb(col, 4210768, 0.4F)), a)
            );
            ctx.fill(bx, by, bx + half * 2, by + half * 2, MenuBackdrop.argb(lerpRgb(329230, col, 0.1F + 0.15F * lit), 0.95F * a));
            ItemStack shown = onNow[i];
            boolean previewing = prev != null && i == this.slot;
            if (previewing) {
               shown = prev.stack();
            }

            if (!shown.isEmpty()) {
               float bob = previewing ? (float)Math.sin((double)(t * 6.0F)) * 1.2F : 0.0F;
               this.item(ctx, shown, px, py + bob, 20.0F);
            } else {
               Identifier ic = i == 0 ? ICON_WEAPON : (i == 1 ? ICON_ACC : (i == 2 ? ICON_ARMOR1 : ICON_ARMOR2));
               RenderSystem.enableBlend();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.35F * a);
               ctx.drawTexture(
                  ic,
                  (int)px - 6,
                  (int)py - 7,
                  13,
                  i == 0 ? 16 : (i == 1 ? 15 : 13),
                  0.0F,
                  0.0F,
                  13,
                  i == 0 ? 16 : (i == 1 ? 15 : 13),
                  13,
                  i == 0 ? 16 : (i == 1 ? 15 : 13)
               );
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            String lab = SLOT_NAMES[i];
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, lab, (int)px, (int)py + half + 4, MenuBackdrop.argb(chosen ? 16777215 : (hov ? 16777024 : 9212838), a)
            );
            int si = i;
            this.click(bx, by, half * 2, half * 2 + 14, () -> this.pickSlot(si));
            if (!onNow[i].isEmpty()) {
               this.rightClick(bx, by, half * 2, half * 2 + 14, () -> this.unequip(si));
            }
         }
      }

      ItemStack hold = prev != null && this.slot == 0 ? prev.stack() : null;
      this.figureYaw = this.approach(
         this.figureYaw, 18.0F + (float)Math.sin((double)(t * 0.6F)) * 10.0F + clamp(((float)mx - scx) / 12.0F, -14.0F, 14.0F), 4.0F
      );
      int fx = (int)(scx + (1.0F - swap) * 18.0F);
      this.drawMember(ctx, sel, fx, footY, s, this.figureYaw, hold, in * swap);
      if (hoveredSocket >= 0) {
         ItemStack st = onNow[hoveredSocket];
         List lines = new ArrayList<>();
         lines.add(new String[]{SLOT_NAMES[hoveredSocket], "8FA3B8"});
         if (st.isEmpty()) {
            lines.add(new String[]{"Nothing here", "666677"});
         } else {
            lines.add(new String[]{st.getName().getString(), "FFFFFF"});

            for (String[] l : this.describe(hoveredSocket, st)) {
               lines.add(l);
            }
         }

         this.tag(ctx, this.socketPos[hoveredSocket][0], this.socketPos[hoveredSocket][1] - 18.0F, lines, col);
      }
   }

   private void renderStats(DrawContext ctx, VoidMenuScreen.Member sel, int x, int y, int w, int h, EquipScreen.Row prev, boolean compact) {
      float in = ease(clamp01((this.secs() - 0.35F) / 0.4F));
      if (!(in <= 0.01F)) {
         CharStats base = this.statsOf(sel.code());
         int[] gear = this.gearBonus(sel.code(), -1, null);
         int[] after = prev != null ? this.gearBonus(sel.code(), this.slot, prev.stack()) : null;
         this.panel(ctx, x, y, w, h, sel.color(), in, false);
         if (compact) {
            int cw = w / 4;

            for (int i = 0; i < 7; i++) {
               int cx = x + 4 + i % 4 * cw;
               int cy = y + 4 + i / 4 * 14;
               int now;
               int then;
               if (i < 6) {
                  int[] sz = STAT_ICON_SIZE[i];
                  RenderSystem.enableBlend();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, in);
                  ctx.drawTexture(STAT_ICONS[i], cx, cy, sz[0], sz[1], 0.0F, 0.0F, sz[0], sz[1], sz[0], sz[1]);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  now = base.valueFor(i) + gear[i];
                  then = after != null ? base.valueFor(i) + after[i] : now;
               } else {
                  ctx.drawTextWithShadow(this.textRenderer, "D", cx + 1, cy + 1, MenuBackdrop.argb(16737894, in));
                  now = bonusDamage(this.weaponOf(sel.code()));
                  then = this.slot == 0 && prev != null ? bonusDamage(prev.stack()) : now;
               }

               String v = String.valueOf(now);
               int vc = i < 6 && gear[i] != 0 ? (gear[i] > 0 ? 8978329 : 16742246) : 16777215;
               ctx.drawTextWithShadow(this.textRenderer, v, cx + 11, cy + 1, MenuBackdrop.argb(vc, in));
               if (then != now) {
                  int d = then - now;
                  ctx.drawTextWithShadow(
                     this.textRenderer,
                     (d > 0 ? "\u25b2" : "\u25bc") + Math.abs(d),
                     cx + 13 + this.textRenderer.getWidth(v),
                     cy + 1,
                     MenuBackdrop.argb(d > 0 ? 5635959 : 16733525, in)
                  );
               }
            }
         } else {
            int cells = 7;
            int cw = w / cells;

            for (int i = 0; i < 6; i++) {
               int cxx = x + i * cw + cw / 2;
               int[] sz = STAT_ICON_SIZE[i];
               RenderSystem.enableBlend();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, in);
               ctx.drawTexture(STAT_ICONS[i], cxx - 14, y + 5, sz[0], sz[1], 0.0F, 0.0F, sz[0], sz[1], sz[0], sz[1]);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               ctx.drawTextWithShadow(this.textRenderer, STAT_SHORT[i], cxx - 3, y + 6, MenuBackdrop.argb(STAT_RGB[i], in));
               int b = base.valueFor(i);
               String v = String.valueOf(b + gear[i]);
               int vc = gear[i] > 0 ? 8978329 : (gear[i] < 0 ? 16742246 : 16777215);
               ctx.drawTextWithShadow(this.textRenderer, v, cxx - 14, y + 18, MenuBackdrop.argb(vc, in));
               if (after != null && after[i] != gear[i]) {
                  int d = after[i] - gear[i];
                  String ds = (d > 0 ? "\u25b2" : "\u25bc") + Math.abs(d);
                  ctx.drawTextWithShadow(
                     this.textRenderer, ds, cxx - 14 + this.textRenderer.getWidth(v) + 3, y + 18, MenuBackdrop.argb(d > 0 ? 5635959 : 16733525, in)
                  );
               }
            }

            int cxx = x + 6 * cw + cw / 2;
            int dmg = bonusDamage(this.slot == 0 && prev != null ? prev.stack() : this.weaponOf(sel.code()));
            int dmgNow = bonusDamage(this.weaponOf(sel.code()));
            ctx.drawTextWithShadow(this.textRenderer, "DMG", cxx - 10, y + 6, MenuBackdrop.argb(16737894, in));
            ctx.drawTextWithShadow(this.textRenderer, "+" + dmgNow, cxx - 10, y + 18, MenuBackdrop.argb(16777215, in));
            if (dmg != dmgNow) {
               int d = dmg - dmgNow;
               ctx.drawTextWithShadow(
                  this.textRenderer,
                  (d > 0 ? "\u25b2" : "\u25bc") + Math.abs(d),
                  cxx - 10 + this.textRenderer.getWidth("+" + dmgNow) + 3,
                  y + 18,
                  MenuBackdrop.argb(d > 0 ? 5635959 : 16733525, in)
               );
            }
         }
      }
   }

   private static int bonusDamage(ItemStack st) {
      WeaponStats ws = st != null && !st.isEmpty() ? WeaponStats.fromStack(st) : null;
      return ws != null ? ws.baseDamage : 0;
   }

   private int[] gearBonus(int code, int swapSlot, ItemStack swapIn) {
      int[] out = new int[6];
      ItemStack w = swapSlot == 0 ? swapIn : this.weaponOf(code);
      ItemStack[] ar = this.armorOf(code);
      ItemStack a0 = swapSlot == 2 ? swapIn : ar[0];
      ItemStack a1 = swapSlot == 3 ? swapIn : ar[1];
      WeaponStats ws = w != null && !w.isEmpty() ? WeaponStats.fromStack(w) : null;
      if (ws != null) {
         for (int i = 0; i < 6; i++) {
            out[i] += ws.netStatBonus(i);
         }
      }

      for (ItemStack a : new ItemStack[]{a0, a1}) {
         ArmorStats as = a != null && !a.isEmpty() ? ArmorStats.fromStack(a) : null;
         if (as != null) {
            for (int i = 0; i < 6; i++) {
               out[i] += as.netStatBonus(i);
            }
         }
      }

      return out;
   }

   private List<EquipScreen.Row> rowsFor(int s) {
      List out = new ArrayList<>();
      ItemStack[] st = s == 0 ? this.bagWeapons : (s == 1 ? this.bagAcc : this.bagArmor);
      int[] sl = s == 0 ? this.bagWeaponSlots : (s == 1 ? this.bagAccSlots : this.bagArmorSlots);

      for (int i = 0; i < st.length && i < sl.length; i++) {
         if (st[i] != null && !st[i].isEmpty()) {
            boolean unrolled = s == 0 ? WeaponStats.fromStack(st[i]) == null : s >= 2 && ArmorStats.fromStack(st[i]) == null;
            out.add(new EquipScreen.Row(st[i], sl[i], unrolled));
         }
      }

      return out;
   }

   private void renderBag(DrawContext ctx, VoidMenuScreen.Member sel, List<EquipScreen.Row> rows, int x, int y, int w, int h, int mx, int my) {
      float t = this.secs();
      float a = ease(clamp01((t - 0.2F) / 0.45F));
      if (!(a <= 0.01F)) {
         int slide = (int)((1.0F - a) * 18.0F);
         x += slide;
         int col = sel.color();
         this.box(ctx, x, y, w, h, col, a);
         float sw = ease(clamp01(since(this.slotPickedNanos) / 0.25F));
         Identifier ic = this.slot == 0 ? ICON_WEAPON : (this.slot == 1 ? ICON_ACC : (this.slot == 2 ? ICON_ARMOR1 : ICON_ARMOR2));
         int ih = this.slot == 0 ? 16 : (this.slot == 1 ? 15 : 13);
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
         ctx.drawTexture(ic, x + 7, y + 7, 13, ih, 0.0F, 0.0F, 13, ih, 13, ih);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal(SLOT_NAMES[this.slot]).setStyle(Style.EMPTY.withBold(true)),
            x + 25,
            y + 8,
            MenuBackdrop.argb(lerpRgb(col, 16777215, 0.4F), a)
         );
         String whose = this.fit("for " + sel.name(), w - 32 - this.textRenderer.getWidth(SLOT_NAMES[this.slot]) - 18);
         ctx.drawTextWithShadow(this.textRenderer, whose, x + w - 7 - this.textRenderer.getWidth(whose), y + 8, MenuBackdrop.argb(9413560, a));
         int cy = y + 24;
         int cardH = 50;
         ItemStack on = this.slot == 0 ? this.weaponOf(sel.code()) : (this.slot == 1 ? this.accessoryOf(sel.code()) : this.armorOf(sel.code())[this.slot - 2]);
         this.panel(ctx, x + 6, cy, w - 12, cardH, col, a * sw, !on.isEmpty());
         if (on.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, "Nothing equipped", x + w / 2, cy + 14, MenuBackdrop.argb(6710903, a * sw));
            ctx.drawCenteredTextWithShadow(this.textRenderer, "Pick something from your bag", x + w / 2, cy + 28, MenuBackdrop.argb(4869724, a * sw));
         } else {
            MenuBackdrop.tex(ctx, TEX_GLOW, (float)(x + 24), (float)cy + (float)cardH / 2.0F, 44.0F, 44.0F, col, 0.4F * a * sw);
            this.item(ctx, on, (float)(x + 24), (float)cy + (float)cardH / 2.0F, 24.0F);
            int tx = x + 42;
            int tw = w - 12 - 40 - 6;
            ctx.drawTextWithShadow(this.textRenderer, this.fit(on.getName().getString(), tw), tx, cy + 5, MenuBackdrop.argb(16777215, a * sw));
            int ly = cy + 16;

            for (String[] l : this.describe(this.slot, on)) {
               if (ly > cy + cardH - 18) {
                  break;
               }

               ctx.drawTextWithShadow(this.textRenderer, this.fit(l[0], tw), tx, ly, MenuBackdrop.argb(Integer.parseInt(l[1], 16), a * sw));
               ly += 10;
            }

            int s = this.slot;
            this.button(ctx, x + w - 6 - 4 - 58, cy + cardH - 16, 58, 12, "UNEQUIP", mx, my, true, a * sw, () -> this.unequip(s));
         }

         int ly = cy + cardH + 8;
         boolean loaded = this.slot == 0 ? this.gotWeapons : (this.slot == 1 ? this.gotAcc : this.gotArmor);
         String bagLabel = "IN YOUR BAG" + (loaded ? "  (" + rows.size() + ")" : "");
         ctx.drawTextWithShadow(this.textRenderer, bagLabel, x + 7, ly, MenuBackdrop.argb(9413560, a));
         ctx.fill(x + 7 + this.textRenderer.getWidth(bagLabel) + 5, ly + 4, x + w - 7, ly + 5, MenuBackdrop.argb(col, 0.35F * a));
         int listY = ly + 13;
         int listH = y + h - 16 - listY;
         int rowH = 26;
         if (!loaded) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, "...", x + w / 2, listY + 10, MenuBackdrop.argb(6710903, a));
         } else if (rows.isEmpty()) {
            String none = this.slot == 0 ? "No weapons in your bag" : (this.slot == 1 ? "No accessories in your bag" : "No armor in your bag");
            String hint = this.slot == 0
               ? "Swords, axes, tridents, bows..."
               : (this.slot == 1 ? "Shields, feathers, torches, rings..." : "Any helmet, chestplate, leggings or boots");
            ctx.drawCenteredTextWithShadow(this.textRenderer, none, x + w / 2, listY + 10, MenuBackdrop.argb(6710903, a));
            ctx.drawCenteredTextWithShadow(this.textRenderer, this.fit(hint, w - 14), x + w / 2, listY + 22, MenuBackdrop.argb(4869724, a));
         } else {
            float maxScroll = (float)Math.max(0, rows.size() * rowH - listH);
            this.listScrollTarget = clamp(this.listScrollTarget, 0.0F, maxScroll);
            this.listScroll = this.approach(this.listScroll, this.listScrollTarget, 16.0F);
            this.scrollArea(x, listY, w, listH, d -> this.listScrollTarget -= (float)d * (float)rowH);
            ctx.enableScissor(x, listY, x + w, listY + listH);
            ItemStack curWeapon = this.weaponOf(sel.code());

            for (int i = 0; i < rows.size(); i++) {
               EquipScreen.Row r = rows.get(i);
               int ry = listY + i * rowH - Math.round(this.listScroll);
               if (ry + rowH >= listY && ry <= listY + listH) {
                  float ra = a * ease(clamp01((since(this.slotPickedNanos) - (float)i * 0.03F) / 0.25F));
                  int rx = x + 6;
                  int rw = w - 12;
                  boolean hov = inside((double)mx, (double)my, rx, ry, rw, rowH - 2) && my >= listY && my < listY + listH;
                  if (hov) {
                     this.hover("row" + this.slot + ":" + i);
                     this.previewRow = i;
                  }

                  ctx.fill(rx, ry, rx + rw, ry + rowH - 2, MenuBackdrop.argb(hov ? lerpRgb(724506, col, 0.18F) : 724506, 0.9F * ra));
                  if (hov) {
                     ctx.fill(rx, ry, rx + 2, ry + rowH - 2, MenuBackdrop.argb(col, ra));
                  }

                  this.item(ctx, r.stack(), (float)(rx + 12), (float)ry + (float)(rowH - 2) / 2.0F, 16.0F);
                  int tx = rx + 24;
                  int btnW = r.unrolled() ? 34 : 0;
                  int tw = rw - 26 - (btnW > 0 ? btnW + 8 : 34);
                  ctx.drawTextWithShadow(
                     this.textRenderer, this.fit(r.stack().getName().getString(), tw), tx, ry + 3, MenuBackdrop.argb(hov ? 16777024 : 16777215, ra)
                  );
                  List d = this.describe(this.slot, r.stack());
                  if (!d.isEmpty()) {
                     ctx.drawTextWithShadow(
                        this.textRenderer, this.fit(d.get(0)[0], tw), tx, ry + 13, MenuBackdrop.argb(Integer.parseInt(d.get(0)[1], 16), ra * 0.9F)
                     );
                  }

                  if (r.unrolled()) {
                     int inv = r.invSlot();
                     this.button(ctx, rx + rw - btnW - 4, ry + 6, btnW, 12, "ROLL", mx, my, true, ra, () -> this.roll(inv));
                  } else {
                     String cmp = this.compare(sel.code(), r.stack(), curWeapon);
                     if (!cmp.isEmpty()) {
                        int cc = cmp.startsWith("-") ? 16737877 : 5635959;
                        ctx.drawTextWithShadow(this.textRenderer, cmp, rx + rw - 5 - this.textRenderer.getWidth(cmp), ry + 8, MenuBackdrop.argb(cc, ra));
                     }

                     int idx = i;
                     this.click(rx, Math.max(ry, listY), rw, Math.min(rowH - 2, listY + listH - Math.max(ry, listY)), () -> this.equip(idx));
                  }
               }
            }

            ctx.disableScissor();
            if (maxScroll > 0.0F) {
               int barH = Math.max(12, (int)((float)(listH * listH) / (float)(rows.size() * rowH)));
               int barY = listY + (int)((float)(listH - barH) * (this.listScroll / maxScroll));
               ctx.fill(x + w - 3, barY, x + w - 1, barY + barH, MenuBackdrop.argb(col, 0.7F * a));
            }
         }

         if (!rows.isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, this.fit("Click to equip", w - 14), x + 7, y + h - 12, MenuBackdrop.argb(4869724, a));
         }
      }
   }

   private String compare(int code, ItemStack st, ItemStack curWeapon) {
      if (this.slot == 1) {
         return "";
      } else {
         int[] now = this.gearBonus(code, -1, null);
         int[] then = this.gearBonus(code, this.slot, st);
         int best = -1;
         int bestAbs = 0;

         for (int i = 0; i < 6; i++) {
            int d = then[i] - now[i];
            if (Math.abs(d) > bestAbs) {
               bestAbs = Math.abs(d);
               best = i;
            }
         }

         if (this.slot == 0) {
            int dd = bonusDamage(st) - bonusDamage(curWeapon);
            if (dd != 0 && Math.abs(dd) >= bestAbs) {
               return (dd > 0 ? "+" : "") + dd + " DMG";
            }
         }

         if (best < 0) {
            return "";
         } else {
            int d = then[best] - now[best];
            return (d > 0 ? "+" : "") + d + " " + STAT_SHORT[best];
         }
      }
   }

   private List<String[]> describe(int s, ItemStack st) {
      List out = new ArrayList<>();
      if (st != null && !st.isEmpty()) {
         if (s == 0) {
            WeaponStats ws = WeaponStats.fromStack(st);
            String tier = pretty(WeaponStats.tierOf(st).name());
            if (ws == null) {
               out.add(new String[]{tier + "  \u00b7  stats not rolled yet", "44AA88"});
               return out;
            }

            out.add(
               new String[]{
                  "+" + ws.baseDamage + (ws.magic ? " Magic DMG" : " DMG") + "  \u00b7  +" + ws.bonusAmt + " " + WeaponStats.STAT_NAMES[ws.bonusStat],
                  ws.magic ? "C58CFF" : "88DDFF"
               }
            );
            StringBuilder el = new StringBuilder(tier);

            for (Affinity af : ws.getElements()) {
               el.append("  \u00b7  ").append(af.displayName);
            }

            out.add(new String[]{el.toString(), "8FA3B8"});
            if (ws.isQuirk) {
               out.add(new String[]{"Oddity: +10 " + WeaponStats.STAT_NAMES[ws.quirkStat] + " / -10 " + WeaponStats.STAT_NAMES[ws.quirkMinus], "DD88FF"});
            }
         } else if (s == 1) {
            String d = accessoryDesc(st);
            if (!d.isEmpty()) {
               out.add(new String[]{d, "DD88FF"});
            }
         } else {
            ArmorStats as = ArmorStats.fromStack(st);
            ArmorStats.Tier tierx = ArmorStats.tierOf(st);
            String dr = tierx != null
               ? (
                     tierx.reductionPercent == (float)Math.round(tierx.reductionPercent)
                        ? String.valueOf(Math.round(tierx.reductionPercent))
                        : String.valueOf(tierx.reductionPercent)
                  )
                  + "% less damage"
               : "";
            if (as == null) {
               out.add(new String[]{(dr.isEmpty() ? "" : dr + "  \u00b7  ") + "stats not rolled yet", "44AA88"});
               return out;
            }

            out.add(new String[]{"+" + as.bonusAmt + " " + ArmorStats.STAT_NAMES[as.bonusStat] + (dr.isEmpty() ? "" : "  \u00b7  " + dr), "88DDFF"});
         }

         return out;
      } else {
         return out;
      }
   }

   static String accessoryDesc(ItemStack stack) {
      return switch (OffhandGimmick.fromStack(stack)) {
         case SHIELD -> "A ring of force that breaks the bullets around you";
         case FEATHER -> "Dash through bullets";
         case TORCH -> "25% chance to burn enemies with Bash";
         case THORN_RING -> "Hurts its wearer, but every spell costs less TP";
         case PSI_PROCESSOR -> "Drains TP constantly, but doubles PSI damage";
         case HEART_LOCKET -> "Halves all damage you take in battle, after every other reduction";
         case SOUL_RESONATOR -> "Grazing turns TP into HP; a SOUL at full potential pays for skills in HP";
         default -> "";
      };
   }

   private void tag(DrawContext ctx, float cx, float cy, List<String[]> lines, int accent) {
      int w = 0;
      int maxW = 190;
      List wrapped = new ArrayList<>();

      for (String[] l : lines) {
         List wl = this.textRenderer.wrapLines(StringVisitable.plain(l[0]), maxW);
         wrapped.add(wl);

         for (OrderedText o : wl) {
            w = Math.max(w, this.textRenderer.getWidth(o));
         }
      }

      w += 14;
      int h = 6;

      for (List wl : wrapped) {
         h += wl.size() * 10;
      }

      h += 4;
      int x = (int)(cx - (float)w / 2.0F);
      int y = (int)(cy - (float)h);
      x = Math.max(4, Math.min(this.width - w - 4, x));
      if (y < this.bodyTop()) {
         y = (int)cy + 36;
      }

      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 400.0F);
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(329230, 0.92F));
      ctx.fill(x, y, x + w, y + 1, MenuBackdrop.argb(accent, 1.0F));
      ctx.fill(x, y + h - 1, x + w, y + h, MenuBackdrop.argb(accent, 0.5F));
      int ty = y + 5;

      for (int i = 0; i < wrapped.size(); i++) {
         int rgb = Integer.parseInt(lines.get(i)[1], 16);

         for (OrderedText o : wrapped.get(i)) {
            ctx.drawTextWithShadow(this.textRenderer, o, x + 7, ty, MenuBackdrop.argb(rgb, 1.0F));
            ty += 10;
         }
      }

      m.pop();
   }

   private void equip(int rowIndex) {
      List rows = this.rowsFor(this.slot);
      if (rowIndex >= 0 && rowIndex < rows.size()) {
         EquipScreen.Row r = rows.get(rowIndex);
         if (r.unrolled()) {
            this.roll(r.invSlot());
         } else {
            int code = this.selected;
            PacketByteBuf buf = PacketByteBufs.create();
            switch (this.slot) {
               case 0:
                  if (code == -1) {
                     buf.writeInt(r.invSlot());
                     buf.writeBoolean(false);
                     Net121.sendToServer(TurnBattleMod.EQUIP_WEAPON, buf);
                  } else {
                     buf.writeInt(code);
                     buf.writeInt(r.invSlot());
                     Net121.sendToServer(TurnBattleMod.RECRUIT_EQUIP_WEAPON, buf);
                  }
                  break;
               case 1:
                  if (code == -1) {
                     buf.writeInt(r.invSlot());
                     Net121.sendToServer(TurnBattleMod.EQUIP_ACCESSORY, buf);
                  } else {
                     buf.writeInt(code);
                     buf.writeInt(r.invSlot());
                     Net121.sendToServer(TurnBattleMod.RECRUIT_EQUIP_ACCESSORY, buf);
                  }
                  break;
               default:
                  int ai = this.slot - 2;
                  if (code == -1) {
                     buf.writeInt(r.invSlot());
                     buf.writeInt(ai);
                     Net121.sendToServer(TurnBattleMod.EQUIP_ARMOR, buf);
                  } else {
                     buf.writeInt(code);
                     buf.writeInt(ai);
                     buf.writeInt(r.invSlot());
                     Net121.sendToServer(TurnBattleMod.RECRUIT_EQUIP_ARMOR, buf);
                  }
            }

            TurnBattleSounds.playClient(TurnBattleSounds.SND_POWER, 0.8F, 1.1F);
            VoidMenuScreen.Member m = this.member(code);
            int col = m != null ? m.color() : tint();
            this.pulse(this.socketPos[this.slot][0], this.socketPos[this.slot][1], col, 18.0F);
            this.pulse(this.figX, this.figY - this.figScale * 0.9F, col, this.figScale * 0.6F);
            this.act(
               m,
               this.slot == 0 ? CloneAnimator.Act.BASH : (this.slot == 1 ? CloneAnimator.Act.SKILL : CloneAnimator.Act.ACT),
               col,
               this.slot == 0 ? r.stack() : null
            );
            this.refresh();
         }
      }
   }

   private void unequip(int s) {
      int code = this.selected;
      PacketByteBuf buf = PacketByteBufs.create();
      switch (s) {
         case 0:
            if (code == -1) {
               Net121.sendToServer(TurnBattleMod.UNEQUIP_WEAPON, buf);
            } else {
               buf.writeInt(code);
               Net121.sendToServer(TurnBattleMod.RECRUIT_UNEQUIP_WEAPON, buf);
            }
            break;
         case 1:
            if (code == -1) {
               Net121.sendToServer(TurnBattleMod.UNEQUIP_ACCESSORY, buf);
            } else {
               buf.writeInt(code);
               Net121.sendToServer(TurnBattleMod.RECRUIT_UNEQUIP_ACCESSORY, buf);
            }
            break;
         default:
            int ai = s - 2;
            if (code == -1) {
               buf.writeInt(ai);
               Net121.sendToServer(TurnBattleMod.UNEQUIP_ARMOR, buf);
            } else {
               buf.writeInt(code);
               buf.writeInt(ai);
               Net121.sendToServer(TurnBattleMod.RECRUIT_UNEQUIP_ARMOR, buf);
            }
      }

      TurnBattleSounds.playClient(TurnBattleSounds.SND_GRAB, 0.8F, 0.9F);
      this.pulse(this.socketPos[s][0], this.socketPos[s][1], 9212838, 14.0F);
      this.refresh();
   }

   private void roll(int invSlot) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(invSlot);
      Net121.sendToServer(this.slot == 0 ? TurnBattleMod.WEAPON_ROLL_REQUEST : TurnBattleMod.ARMOR_ROLL_REQUEST, buf);
      TurnBattleSounds.playClient(TurnBattleSounds.SND_CHESTOPEN, 0.8F, 1.2F);
      this.popup("ROLLED", this.socketPos[this.slot][0], this.socketPos[this.slot][1] - 20.0F, 8388564);
      this.refresh();
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (key >= 49 && key <= 52) {
         this.pickSlot(key - 49);
         return true;
      } else {
         return super.keyPressed(key, scan, mods);
      }
   }

   private static record Row(ItemStack stack, int invSlot, boolean unrolled) {
   }
}
