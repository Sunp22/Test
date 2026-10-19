package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ItemsScreen extends VoidMenuScreen {
   private int pickedSlot = -1;
   private long pickedNanos;
   private long lastClickNanos;
   private int lastClickSlot = -1;
   private float shelfScroll;
   private float shelfScrollTarget;
   private UUID heldPoseKey;
   private final float[] targetLight = new float[3];
   private final Map<Integer, float[]> drawnAt = new HashMap<>();

   public ItemsScreen(Screen parent) {
      super("Items", parent);
   }

   @Override
   protected int navIndex() {
      return 4;
   }

   @Override
   protected void onFirstOpen() {
      Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
   }

   @Override
   protected String headerRight() {
      int n = 0;

      for (ItemsScreen.Food f : this.foods()) {
         n += f.stack().getCount();
      }

      return n == 0 ? "" : n + " to eat";
   }

   private List<ItemsScreen.Food> foods() {
      List out = new ArrayList<>();
      if (this.client != null && this.client.player != null) {
         PlayerInventory inv = this.client.player.getInventory();

         for (int si = 0; si < inv.size(); si++) {
            ItemStack st = inv.getStack(si);
            if (!st.isEmpty()) {
               FoodComponent fc = (FoodComponent)st.get(DataComponentTypes.FOOD);
               if (fc != null) {
                  String id = Registries.ITEM.getId(st.getItem()).toString();
                  out.add(new ItemsScreen.Food(si, st, fc, TurnBattleClientMod.clientFavoriteFood.contains(id)));
               }
            }
         }

         out.sort((a, b) -> Boolean.compare(b.fav(), a.fav()));
         return out;
      } else {
         return out;
      }
   }

   private List<ItemsScreen.Target> targets() {
      List out = new ArrayList<>();
      out.add(new ItemsScreen.Target(0, this.you()));
      int s1 = TurnBattleClientMod.clientActiveRecruit;
      int s2 = TurnBattleClientMod.clientSecondRecruit;
      VoidMenuScreen.Member m1 = s1 >= 0 ? this.member(s1) : null;
      VoidMenuScreen.Member m2 = s2 >= 0 ? this.member(s2) : null;
      if (m1 != null) {
         out.add(new ItemsScreen.Target(1, m1));
      }

      if (m2 != null) {
         out.add(new ItemsScreen.Target(2, m2));
      }

      return out;
   }

   private static float healOf(ItemsScreen.Food f, int targetCode) {
      float heal = ((float)f.food().nutrition() * 2.0F + f.food().saturation() * 2.0F) * 2.0F;
      if (TurnBattleClientMod.clientSoul == SoulType.KINDNESS) {
         heal *= 1.25F;
      }

      if (targetCode == 0 && f.fav()) {
         heal *= 2.0F;
      }

      return heal;
   }

   private ItemsScreen.Food picked(List<ItemsScreen.Food> foods) {
      for (ItemsScreen.Food f : foods) {
         if (f.invSlot() == this.pickedSlot) {
            return f;
         }
      }

      return null;
   }

   @Override
   protected void renderBody(DrawContext ctx, int mx, int my) {
      List foods = this.foods();
      ItemsScreen.Food pick = this.picked(foods);
      if (pick == null) {
         this.pickedSlot = -1;
      }

      List tgts = this.targets();
      int top = this.bodyTop();
      int rightW = (int)clamp((float)this.width * 0.32F, 156.0F, 236.0F);
      int rx = this.width - rightW - 10;
      int ax = 10;
      int aw = rx - 12 - ax;
      int stageH = (int)((float)(this.height - top) * 0.5F);
      float stageCx = (float)ax + (float)aw / 2.0F;
      int footY = top + stageH - 30;
      this.renderVoid(ctx, stageCx, (float)footY, (float)aw * 1.1F);
      int hoveredTarget = this.renderStage(ctx, tgts, pick, ax, aw, top, footY, mx, my);
      this.renderShelf(ctx, foods, ax, top + stageH + 4, aw, this.height - (top + stageH + 4) - 10, mx, my);
      int hoveredRow = this.renderDetail(ctx, pick, tgts, rx, top, rightW, this.height - top - 10, mx, my);
      if (hoveredTarget < 0) {
         hoveredTarget = hoveredRow;
      }

      UUID want = null;
      if (pick != null && hoveredTarget >= 0) {
         for (ItemsScreen.Target t : tgts) {
            if (t.targetCode() == hoveredTarget) {
               want = poseKey(t.member());
            }
         }
      }

      if (want == null ? this.heldPoseKey != null : !want.equals(this.heldPoseKey)) {
         if (this.heldPoseKey != null) {
            CloneAnimator.releaseMenuPoses(this.heldPoseKey);
         }

         if (want != null) {
            for (ItemsScreen.Target tx : tgts) {
               if (poseKey(tx.member()).equals(want)) {
                  try {
                     CloneAnimator.hold(want, CloneAnimator.Act.CHOOSE_ITEM, 7143290, pick.stack().copy());
                  } catch (Throwable var21) {
                  }
               }
            }
         }

         this.heldPoseKey = want;
      }
   }

   private int renderStage(DrawContext ctx, List<ItemsScreen.Target> tgts, ItemsScreen.Food pick, int ax, int aw, int top, int footY, int mx, int my) {
      float t = this.secs();
      float in = ease(clamp01((t - 0.1F) / 0.5F));
      int n = tgts.size();
      float scale = clamp(Math.min((float)(footY - top - 14) / 2.0F, (float)aw / ((float)n * 3.2F)), 12.0F, 54.0F);
      float spacing = Math.min((float)aw / (float)n, scale * 3.6F);
      float x0 = (float)ax + (float)aw / 2.0F - spacing * (float)(n - 1) / 2.0F;
      int hovered = -1;
      this.drawnAt.clear();
      int[] order = n == 3 ? new int[]{1, 0, 2} : (n == 2 ? new int[]{0, 1} : new int[]{0});

      for (int k = 0; k < n; k++) {
         ItemsScreen.Target tg = tgts.get(order[k]);
         VoidMenuScreen.Member m = tg.member();
         float a = ease(clamp01((t - 0.15F - (float)k * 0.08F) / 0.4F));
         int x = (int)(x0 + (float)k * spacing);
         int col = m.color();
         int bw = (int)Math.min(spacing - 12.0F, 70.0F);
         int hitTop = (int)((float)footY - scale * 2.0F);
         boolean hov = pick != null && inside((double)mx, (double)my, x - bw / 2, hitTop, bw, footY + 28 - hitTop);
         int ti = tg.targetCode();
         this.targetLight[ti] = this.approach(this.targetLight[ti], hov ? 1.0F : 0.0F, 12.0F);
         float lit = this.targetLight[ti];
         if (hov) {
            hovered = ti;
            this.hover("tgt" + ti);
         }

         MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, (float)footY, scale * 3.2F, scale * 0.8F, col, (0.45F + 0.3F * lit) * in);
         MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, (float)footY - scale, scale * 2.6F, scale * 3.2F, col, (0.06F + 0.12F * lit) * in);
         if (lit > 0.02F) {
            float rq = t * 0.8F % 1.0F;
            MenuBackdrop.tex(ctx, TEX_RING, (float)x, (float)footY, scale * (2.0F + rq * 1.5F), scale * (0.5F + rq * 0.4F), 5635959, 0.6F * (1.0F - rq) * lit);
         }

         float yaw = ((float)x < (float)ax + (float)aw / 2.0F - 4.0F ? 22.0F : ((float)x > (float)ax + (float)aw / 2.0F + 4.0F ? -22.0F : 0.0F))
            + (float)Math.sin((double)(t * 0.7F + (float)k)) * 6.0F;
         this.drawMember(ctx, m, x, footY, scale, yaw, null, a);
         this.drawnAt.put(ti, new float[]{(float)x, (float)footY - scale * 2.0F});
         String nm = this.fit(m.name(), bw + 10);
         ctx.drawCenteredTextWithShadow(this.textRenderer, nm, x, footY + 5, MenuBackdrop.argb(hov ? 16777024 : lerpRgb(col, 16777215, 0.5F), a));
         float ghost = pick != null && hov ? healOf(pick, ti) : 0.0F;
         this.hpBar(ctx, x - bw / 2, footY + 16, bw, 4, m.hp(), m.maxHp(), ghost, a);
         String hp = Math.round(m.hp()) + "/" + Math.round(m.maxHp());
         ctx.drawCenteredTextWithShadow(this.textRenderer, hp, x, footY + 23, MenuBackdrop.argb(9413560, a * 0.9F));
         if (hov && pick != null) {
            int heal = Math.round(healOf(pick, ti));
            boolean full = m.hp() >= m.maxHp() - 0.01F;
            String s = full ? "FULL HP" : "+" + heal;
            Text tx = Text.literal(s).setStyle(Style.EMPTY.withFont(DAMAGE_FONT));
            ctx.drawTextWithShadow(
               this.textRenderer,
               tx,
               x - this.textRenderer.getWidth(tx) / 2,
               (int)((double)((float)footY - scale * 2.1F - 12.0F) + Math.sin((double)(t * 5.0F)) * 1.5),
               MenuBackdrop.argb(full ? 16755268 : 5635959, 1.0F)
            );
            this.heart(ctx, (float)(x - 4), (float)((int)((float)footY - scale * 2.1F - 26.0F)), 8, 0.6F + 0.4F * (float)Math.sin((double)(t * 6.0F)));
         }

         if (pick != null) {
            this.click(x - bw / 2, hitTop, bw, footY + 28 - hitTop, () -> this.use(ti));
         }
      }

      if (pick == null) {
         float ha = in * (0.55F + 0.2F * (float)Math.sin((double)(t * 2.0F)));
         ctx.drawCenteredTextWithShadow(this.textRenderer, "Pick something to eat from below", ax + aw / 2, top + 2, MenuBackdrop.argb(9413560, ha));
      } else {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            "Who eats the " + this.fit(pick.stack().getName().getString(), aw - 120) + "?",
            ax + aw / 2,
            top + 2,
            MenuBackdrop.argb(16777215, in)
         );
      }

      return hovered;
   }

   private void renderShelf(DrawContext ctx, List<ItemsScreen.Food> foods, int x, int y, int w, int h, int mx, int my) {
      float t = this.secs();
      float in = ease(clamp01((t - 0.25F) / 0.4F));
      int col = tint();
      String label = "FOOD";
      ctx.drawTextWithShadow(this.textRenderer, label, x + 2, y, MenuBackdrop.argb(9413560, in));
      ctx.fill(x + 6 + this.textRenderer.getWidth(label), y + 4, x + w, y + 5, MenuBackdrop.argb(col, 0.35F * in));
      int gy = y + 12;
      int gh = h - 12;
      if (foods.isEmpty()) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, "You aren't carrying anything to eat.", x + w / 2, gy + gh / 2 - 8, MenuBackdrop.argb(6710903, in));
         ctx.drawCenteredTextWithShadow(this.textRenderer, "Bread, cooked meat, fruit, stew...", x + w / 2, gy + gh / 2 + 4, MenuBackdrop.argb(4869724, in));
      } else {
         int cell = 34;
         int gap = 5;
         int cols = Math.max(1, (w + gap) / (cell + gap));
         int gridW = cols * (cell + gap) - gap;
         int gx = x + (w - gridW) / 2;
         int rows = (foods.size() + cols - 1) / cols;
         float maxScroll = (float)Math.max(0, rows * (cell + gap) - gap - gh);
         this.shelfScrollTarget = clamp(this.shelfScrollTarget, 0.0F, maxScroll);
         this.shelfScroll = this.approach(this.shelfScroll, this.shelfScrollTarget, 16.0F);
         this.scrollArea(x, gy, w, gh, d -> this.shelfScrollTarget -= (float)d * (float)(cell + gap));
         ctx.enableScissor(x - 4, gy, x + w + 4, gy + gh);
         ItemsScreen.Food hov = null;
         float hovX = 0.0F;
         float hovY = 0.0F;

         for (int i = 0; i < foods.size(); i++) {
            ItemsScreen.Food f = foods.get(i);
            int cx = gx + i % cols * (cell + gap);
            int cy = gy + i / cols * (cell + gap) - Math.round(this.shelfScroll);
            if (cy + cell >= gy && cy <= gy + gh) {
               float a = backOut(clamp01((t - 0.3F - (float)i * 0.025F) / 0.3F));
               if (!(a <= 0.01F)) {
                  boolean sel = f.invSlot() == this.pickedSlot;
                  boolean hv = inside((double)mx, (double)my, cx, cy, cell, cell) && my >= gy && my < gy + gh;
                  if (hv) {
                     hov = f;
                     hovX = (float)cx + (float)cell / 2.0F;
                     hovY = (float)cy;
                     this.hover("food" + f.invSlot());
                  }

                  int tile = f.fav() ? 16764992 : col;
                  float mid = (float)cell / 2.0F;
                  float sz = (float)cell * (0.6F + 0.4F * Math.min(1.0F, a));
                  MenuBackdrop.tex(
                     ctx,
                     TEX_GLOW,
                     (float)cx + mid,
                     (float)cy + mid,
                     sz * 1.6F,
                     sz * 1.6F,
                     tile,
                     (sel ? 0.55F : (hv ? 0.35F : (f.fav() ? 0.22F : 0.1F))) * Math.min(1.0F, a)
                  );
                  int bx = (int)((float)cx + mid - sz / 2.0F);
                  int by = (int)((float)cy + mid - sz / 2.0F);
                  int bs = (int)sz;
                  ctx.fill(bx, by, bx + bs, by + bs, MenuBackdrop.argb(sel ? 16777215 : (hv ? 16777024 : lerpRgb(tile, 3158080, 0.55F)), Math.min(1.0F, a)));
                  ctx.fill(bx + 1, by + 1, bx + bs - 1, by + bs - 1, MenuBackdrop.argb(lerpRgb(329230, tile, sel ? 0.22F : 0.08F), 0.95F * Math.min(1.0F, a)));
                  if (sel) {
                     float rq = t * 0.8F % 1.0F;
                     MenuBackdrop.tex(
                        ctx,
                        TEX_RING,
                        (float)cx + mid,
                        (float)cy + mid,
                        (float)cell * (1.1F + rq * 0.7F),
                        (float)cell * (1.1F + rq * 0.7F),
                        tile,
                        0.5F * (1.0F - rq)
                     );
                  }

                  float bob = !hv && !sel ? 0.0F : (float)Math.sin((double)(t * 5.0F + (float)i)) * 1.2F;
                  this.item(ctx, f.stack(), (float)cx + mid, (float)cy + mid + bob, 20.0F * Math.min(1.0F, a));
                  if (f.stack().getCount() > 1) {
                     MatrixStack m = ctx.getMatrices();
                     m.push();
                     m.translate(0.0F, 0.0F, 200.0F);
                     String c = String.valueOf(f.stack().getCount());
                     ctx.drawTextWithShadow(this.textRenderer, c, cx + cell - 3 - this.textRenderer.getWidth(c), cy + cell - 10, -1);
                     m.pop();
                  }

                  if (f.fav()) {
                     MatrixStack m = ctx.getMatrices();
                     m.push();
                     m.translate(0.0F, 0.0F, 200.0F);
                     ctx.drawTextWithShadow(this.textRenderer, "\u2605", cx + 3, cy + 2, -12224);
                     m.pop();
                  }

                  int slot = f.invSlot();
                  this.click(cx, Math.max(cy, gy), cell, Math.max(0, Math.min(cell, gy + gh - Math.max(cy, gy))), () -> this.pick(slot));
                  this.rightClick(cx, Math.max(cy, gy), cell, Math.max(0, Math.min(cell, gy + gh - Math.max(cy, gy))), () -> this.favourite(slot));
               }
            }
         }

         ctx.disableScissor();
         if (maxScroll > 0.0F) {
            int barH = Math.max(12, (int)((float)(gh * gh) / (float)(rows * (cell + gap))));
            int barY = gy + (int)((float)(gh - barH) * (this.shelfScroll / maxScroll));
            ctx.fill(x + w + 2, barY, x + w + 4, barY + barH, MenuBackdrop.argb(col, 0.7F * in));
         }

         if (hov != null) {
            List lines = new ArrayList<>();
            lines.add(new String[]{hov.stack().getName().getString() + (hov.stack().getCount() > 1 ? "  x" + hov.stack().getCount() : ""), "FFFFFF"});
            lines.add(new String[]{"Heals " + Math.round(healOf(hov, 0)) + " HP" + (hov.fav() ? "  (favourite: x2 for you)" : ""), "55FF77"});
            lines.add(new String[]{"Click to pick  \u00b7  double-click to eat  \u00b7  right-click to favourite", "666677"});
            this.tag(ctx, hovX, hovY - 4.0F, lines, hov.fav() ? 16764992 : col);
         }
      }
   }

   private int renderDetail(DrawContext ctx, ItemsScreen.Food pick, List<ItemsScreen.Target> tgts, int x, int y, int w, int h, int mx, int my) {
      float t = this.secs();
      float a = ease(clamp01((t - 0.2F) / 0.45F));
      if (a <= 0.01F) {
         return -1;
      } else {
         x += (int)((1.0F - a) * 18.0F);
         int col = pick != null && pick.fav() ? 16764992 : tint();
         this.box(ctx, x, y, w, h, col, a);
         int hovered = -1;
         if (pick == null) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, "Nothing picked", x + w / 2, y + h / 2 - 14, MenuBackdrop.argb(6710903, a));
            List hint = this.textRenderer
               .wrapLines(StringVisitable.plain("Pick something from the shelf, then choose who eats it. Favourites heal you twice as much."), w - 20);
            int hy = y + h / 2;

            for (OrderedText o : hint) {
               ctx.drawTextWithShadow(this.textRenderer, o, x + w / 2 - this.textRenderer.getWidth(o) / 2, hy, MenuBackdrop.argb(4869724, a));
               hy += 10;
            }

            return -1;
         } else {
            float pa = ease(clamp01(since(this.pickedNanos) / 0.3F));
            boolean compact = h < 240;
            int iy = y + (compact ? 20 : 30);
            float isz = compact ? 24.0F : 32.0F;
            float pulse = 1.0F + 0.04F * (float)Math.sin((double)(t * 3.0F));
            MenuBackdrop.tex(ctx, TEX_GLOW, (float)x + (float)w / 2.0F, (float)iy, isz * 2.8F * pulse, isz * 2.8F * pulse, col, 0.45F * a * pa);
            float rq = t * 0.4F % 1.0F;
            MenuBackdrop.tex(
               ctx, TEX_RING, (float)x + (float)w / 2.0F, (float)iy, isz * (1.2F + rq * 1.3F), isz * (1.2F + rq * 1.3F), col, 0.4F * (1.0F - rq) * a * pa
            );
            this.item(ctx, pick.stack(), (float)x + (float)w / 2.0F, (float)iy + (float)Math.sin((double)(t * 2.0F)) * 1.5F, isz * (0.7F + 0.3F * backOut(pa)));
            int ty = iy + (compact ? 17 : 24);
            String cnt = pick.stack().getCount() > 1 ? pick.stack().getCount() + " of them" : "the last one";
            Text nm = Text.literal(
                  this.fit(pick.stack().getName().getString() + (compact && pick.stack().getCount() > 1 ? "  x" + pick.stack().getCount() : ""), w - 16)
               )
               .setStyle(Style.EMPTY.withBold(true));
            ctx.drawCenteredTextWithShadow(this.textRenderer, nm, x + w / 2, ty, MenuBackdrop.argb(16777215, a * pa));
            ty += 11;
            if (!compact) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, cnt, x + w / 2, ty, MenuBackdrop.argb(9413560, a * pa));
               ty += 14;
            }

            String heals = pick.fav()
               ? "Heals " + Math.round(healOf(pick, 0)) + " HP for you, " + Math.round(healOf(pick, 1)) + " for others"
               : "Heals " + Math.round(healOf(pick, 1)) + " HP";
            ctx.drawCenteredTextWithShadow(this.textRenderer, this.fit(heals, w - 12), x + w / 2, ty, MenuBackdrop.argb(5635959, a * pa));
            ty += compact ? 12 : 14;
            int slot = pick.invSlot();
            String favLabel = pick.fav() ? "\u2605 FAVOURITE" : "\u2606 FAVOURITE";
            int fbw = Math.min(w - 24, 104);
            this.button(ctx, x + w / 2 - fbw / 2, ty, fbw, compact ? 11 : 13, favLabel, mx, my, true, a * pa, () -> this.favourite(slot));
            ty += compact ? 18 : 24;
            ctx.drawTextWithShadow(this.textRenderer, "GIVE TO", x + 8, ty, MenuBackdrop.argb(9413560, a));
            ctx.fill(x + 12 + this.textRenderer.getWidth("GIVE TO"), ty + 4, x + w - 8, ty + 5, MenuBackdrop.argb(col, 0.35F * a));
            ty += 12;
            int rowH = compact ? 23 : 26;

            for (ItemsScreen.Target tg : tgts) {
               if (ty + rowH > y + h - 14) {
                  break;
               }

               VoidMenuScreen.Member m = tg.member();
               int rx = x + 6;
               int rw = w - 12;
               boolean hov = inside((double)mx, (double)my, rx, ty, rw, rowH - 3);
               if (hov) {
                  hovered = tg.targetCode();
                  this.hover("row" + tg.targetCode());
               }

               int mc = m.color();
               ctx.fill(rx, ty, rx + rw, ty + rowH - 3, MenuBackdrop.argb(hov ? lerpRgb(724506, mc, 0.2F) : 724506, 0.9F * a));
               if (hov) {
                  ctx.fill(rx, ty, rx + 2, ty + rowH - 3, MenuBackdrop.argb(mc, a));
                  this.heart(ctx, (float)(rx + rw - 13), (float)ty + (float)(rowH - 3) / 2.0F - 4.0F, 8, a);
               }

               this.face(ctx, this.skinOf(m), rx + 5, ty + 3, 16, a);
               ctx.drawTextWithShadow(
                  this.textRenderer,
                  this.fit(m.name() + (tg.targetCode() == 0 ? "" : "  (slot " + tg.targetCode() + ")"), rw - 70),
                  rx + 26,
                  ty + 3,
                  MenuBackdrop.argb(hov ? 16777024 : 16777215, a)
               );
               int heal = Math.round(healOf(pick, tg.targetCode()));
               String hs = "+" + heal;
               ctx.drawTextWithShadow(this.textRenderer, hs, rx + rw - (hov ? 18 : 6) - this.textRenderer.getWidth(hs), ty + 3, MenuBackdrop.argb(5635959, a));
               this.hpBar(ctx, rx + 26, ty + 15, rw - 26 - 30, 3, m.hp(), m.maxHp(), hov ? (float)heal : 0.0F, a);
               int code = tg.targetCode();
               this.click(rx, ty, rw, rowH - 3, () -> this.use(code));
               ty += rowH;
            }

            ctx.drawTextWithShadow(this.textRenderer, this.fit("Or click them on the left", w - 14), x + 7, y + h - 12, MenuBackdrop.argb(4869724, a));
            return hovered;
         }
      }
   }

   private void tag(DrawContext ctx, float cx, float cy, List<String[]> lines, int accent) {
      int w = 0;
      List wrapped = new ArrayList<>();

      for (String[] l : lines) {
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

      int x = (int)(cx - (float)w / 2.0F);
      int y = (int)(cy - (float)h);
      x = Math.max(4, Math.min(this.width - w - 4, x));
      if (y < this.bodyTop()) {
         y = (int)cy + 40;
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

   private void pick(int slot) {
      long now = System.nanoTime();
      if (slot == this.lastClickSlot && now - this.lastClickNanos < 350000000L && this.pickedSlot == slot) {
         this.lastClickSlot = -1;
         this.use(0);
      } else {
         this.lastClickSlot = slot;
         this.lastClickNanos = now;
         if (slot != this.pickedSlot) {
            TurnBattleSounds.playSelect();
            this.pickedSlot = slot;
            this.pickedNanos = now;
         }
      }
   }

   private void favourite(int slot) {
      if (this.client != null && this.client.player != null) {
         ItemStack st = this.client.player.getInventory().getStack(slot);
         if (!st.isEmpty()) {
            String id = Registries.ITEM.getId(st.getItem()).toString();
            PacketByteBuf fb = PacketByteBufs.create();
            fb.writeString(id, 128);
            Net121.sendToServer(TurnBattleMod.FAVORITE_TOGGLE, fb);
            boolean nowFav = !TurnBattleClientMod.clientFavoriteFood.contains(id);
            if (nowFav) {
               TurnBattleClientMod.clientFavoriteFood.add(id);
            } else {
               TurnBattleClientMod.clientFavoriteFood.remove(id);
            }

            TurnBattleSounds.playClient(nowFav ? TurnBattleSounds.SND_MERCYADD : TurnBattleSounds.MENU_MOVE, 0.8F, 1.1F);
         }
      }
   }

   private void use(int targetCode) {
      List foods = this.foods();
      ItemsScreen.Food f = this.picked(foods);
      if (f != null) {
         VoidMenuScreen.Member who = null;

         for (ItemsScreen.Target t : this.targets()) {
            if (t.targetCode() == targetCode) {
               who = t.member();
            }
         }

         if (who != null) {
            PacketByteBuf fb = PacketByteBufs.create();
            fb.writeInt(f.invSlot());
            fb.writeInt(targetCode);
            Net121.sendToServer(TurnBattleMod.ITEM_FEED, fb);
            TurnBattleSounds.playClient(TurnBattleSounds.SND_ITEM, 0.9F, 1.0F);
            if (this.heldPoseKey != null) {
               CloneAnimator.releaseMenuPoses(this.heldPoseKey);
               this.heldPoseKey = null;
            }

            this.act(who, CloneAnimator.Act.ITEM, 7143290, f.stack().copy());
            float[] at = this.drawnAt.get(targetCode);
            if (at != null) {
               int heal = Math.round(healOf(f, targetCode));
               this.popup("+" + heal, at[0], at[1] - 4.0F, 5635959);
               this.pulse(at[0], at[1] + 24.0F, 5635959, 22.0F);
            }

            if (f.stack().getCount() <= 1) {
               this.pickedSlot = -1;
            }

            Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
         }
      }
   }

   @Override
   public void removed() {
      if (this.heldPoseKey != null) {
         CloneAnimator.releaseMenuPoses(this.heldPoseKey);
      }

      super.removed();
   }

   private static record Food(int invSlot, ItemStack stack, FoodComponent food, boolean fav) {
   }

   private static record Target(int targetCode, VoidMenuScreen.Member member) {
   }
}
