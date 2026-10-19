package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PartyMenuScreen extends VoidMenuScreen {
   private static final String[] PAGES = new String[]{"RECRUITS", "VESSELS", "PLAYERS"};
   private static int lastPage = 0;
   private int page;
   private long pageNanos;
   private float rosterScroll;
   private float rosterScrollTarget;
   private float vesselScroll;
   private float vesselScrollTarget;
   private float onlineScroll;
   private float onlineScrollTarget;
   private int vesselPicked = 0;
   private long vesselPickedNanos;
   private int removeArmed = -1;
   private long removeArmedNanos;
   private final Map<UUID, Long> invitedAt = new HashMap<>();
   private final float[] padLight = new float[3];
   private final Map<Integer, float[]> padAt = new HashMap<>();

   public PartyMenuScreen(Screen parent) {
      super("Party", parent);
   }

   @Override
   protected int navIndex() {
      return 2;
   }

   @Override
   protected void onFirstOpen() {
      this.page = lastPage;
      this.pageNanos = this.vesselPickedNanos = System.nanoTime();
      Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.ROGUE_INFO_REQUEST, PacketByteBufs.empty());
   }

   @Override
   protected String headerRight() {
      int n = 1;
      if (TurnBattleClientMod.clientActiveRecruit >= 0) {
         n++;
      }

      if (TurnBattleClientMod.clientSecondRecruit >= 0) {
         n++;
      }

      return this.page == 2 ? TurnBattleClientMod.clientPartyPlayers.size() + 1 + " in your party" : n + " / 3 in battle";
   }

   @Override
   protected void renderBody(DrawContext ctx, int mx, int my) {
      int top = this.bodyTop();
      float stageCx = (float)this.width / 2.0F;
      this.renderVoid(ctx, stageCx, (float)top + (float)(this.height - top) * 0.5F, (float)this.width * 0.9F);
      int cTop = this.renderPages(ctx, top, mx, my);
      float pa = ease(clamp01(since(this.pageNanos) / 0.3F));
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, (1.0F - pa) * 8.0F, 0.0F);
      switch (this.page) {
         case 0:
            this.renderRecruits(ctx, cTop, mx, my, pa);
            break;
         case 1:
            this.renderVessels(ctx, cTop, mx, my, pa);
            break;
         default:
            this.renderPlayers(ctx, cTop, mx, my, pa);
      }

      m.pop();
   }

   private int renderPages(DrawContext ctx, int y, int mx, int my) {
      float in = ease(clamp01((this.secs() - 0.1F) / 0.35F));
      int tw = 76;
      int th = 14;
      int gap = 6;
      int total = PAGES.length * tw + (PAGES.length - 1) * gap;
      int x0 = this.width / 2 - total / 2;
      int accent = tint();

      for (int i = 0; i < PAGES.length; i++) {
         int x = x0 + i * (tw + gap);
         boolean sel = i == this.page;
         boolean hov = !sel && inside((double)mx, (double)my, x, y, tw, th);
         if (hov) {
            this.hover("page" + i);
         }

         ctx.fill(x - 1, y - 1, x + tw + 1, y + th + 1, MenuBackdrop.argb(sel ? 16777215 : (hov ? 16777024 : 3817298), in));
         ctx.fill(x, y, x + tw, y + th, MenuBackdrop.argb(sel ? lerpRgb(329230, accent, 0.25F) : 329230, in));
         ctx.drawCenteredTextWithShadow(this.textRenderer, PAGES[i], x + tw / 2, y + 3, MenuBackdrop.argb(sel ? 16777215 : (hov ? 16777024 : 9212838), in));
         int p = i;
         if (!sel) {
            this.click(x, y, tw, th, () -> {
               TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
               lastPage = p;
               this.page = p;
               this.pageNanos = System.nanoTime();
               this.removeArmed = -1;
            });
         }
      }

      return y + th + 8;
   }

   private void renderRecruits(DrawContext ctx, int top, int mx, int my, float pa) {
      float t = this.secs();
      int stageH = (int)Math.max(96.0F, (float)(this.height - top) * 0.56F);
      int footY = top + stageH - 34;
      float spacing = Math.min((float)this.width * 0.26F, 190.0F);
      float scale = clamp(Math.min((float)(footY - top - 16) / 2.05F, spacing / 3.1F), 12.0F, 52.0F);
      float cx = (float)this.width / 2.0F;
      int s1 = TurnBattleClientMod.clientActiveRecruit;
      int s2 = TurnBattleClientMod.clientSecondRecruit;
      VoidMenuScreen.Member[] pads = new VoidMenuScreen.Member[]{s1 >= 0 ? this.member(s1) : null, this.you(), s2 >= 0 ? this.member(s2) : null};
      String[] padNames = new String[]{"SLOT I", "YOU", "SLOT II"};
      this.padAt.clear();

      for (int i = 0; i < 3; i++) {
         float a = ease(clamp01((t - 0.15F - (float)i * 0.08F) / 0.4F)) * pa;
         int x = (int)(cx + (float)(i - 1) * spacing);
         VoidMenuScreen.Member m = pads[i];
         int col = m != null ? m.color() : 5922928;
         int bw = (int)Math.min(spacing - 16.0F, 96.0F);
         int hitTop = (int)((float)footY - scale * 2.1F);
         boolean canPull = i != 1 && m != null;
         boolean hov = canPull && inside((double)mx, (double)my, x - bw / 2, hitTop, bw, footY + 30 - hitTop);
         if (hov) {
            this.hover("pad" + i);
         }

         this.padLight[i] = this.approach(this.padLight[i], hov ? 1.0F : 0.0F, 12.0F);
         this.padAt.put(i, new float[]{(float)x, (float)footY});
         MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, (float)footY, scale * 3.4F, scale * 0.85F, col, (m != null ? 0.55F : 0.22F) * a);
         MenuBackdrop.tex(ctx, TEX_RING, (float)x, (float)footY, scale * 2.6F, scale * 0.62F, col, (m != null ? 0.45F : 0.3F) * a);
         if (m != null) {
            MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, (float)footY - scale, scale * 2.4F, scale * 3.2F, col, 0.08F * a);
         }

         ctx.drawCenteredTextWithShadow(this.textRenderer, padNames[i], x, top, MenuBackdrop.argb(i == 1 ? 16777215 : 9212838, a));
         if (m != null) {
            float yaw = (i == 0 ? 24.0F : (i == 2 ? -24.0F : 0.0F)) + (float)Math.sin((double)(t * 0.6F + (float)i)) * 6.0F;
            this.drawMember(ctx, m, x, footY, scale, yaw, null, a);
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, this.fit(m.name(), bw + 20), x, footY + 6, MenuBackdrop.argb(hov ? 16777024 : lerpRgb(col, 16777215, 0.5F), a)
            );
            String sub = "LV " + m.level() + (m.cls() != null ? "  \u00b7  " + m.cls().displayName : "");
            ctx.drawCenteredTextWithShadow(this.textRenderer, this.fit(sub, bw + 30), x, footY + 16, MenuBackdrop.argb(9413560, a));
            this.hpBar(ctx, x - bw / 2, footY + 27, bw, 3, m.hp(), m.maxHp(), 0.0F, a);
            if (hov) {
               String s = "Take out of the party";
               ctx.drawCenteredTextWithShadow(this.textRenderer, s, x, hitTop - 10, MenuBackdrop.argb(16746615, a));
            }

            if (canPull) {
               int slot = i == 0 ? 1 : 2;
               this.click(x - bw / 2, hitTop, bw, footY + 30 - hitTop, () -> this.setSlot(slot, -1, m));
            }
         } else {
            float br = 0.5F + 0.5F * (float)Math.sin((double)(t * 1.6F + (float)i));
            ctx.fillGradient(
               x - 1, (int)((float)footY - scale * 1.8F), x + 1, footY, MenuBackdrop.argb(9212838, 0.0F), MenuBackdrop.argb(9212838, 0.35F * br * a)
            );
            CloneAnimator.diamond(ctx, (float)x, (float)footY - scale * 0.9F, 5.0F, MenuBackdrop.argb(9212838, 0.5F * br * a));
            ctx.drawCenteredTextWithShadow(this.textRenderer, "EMPTY", x, footY + 6, MenuBackdrop.argb(6710903, a));
            ctx.drawCenteredTextWithShadow(this.textRenderer, "choose below", x, footY + 16, MenuBackdrop.argb(4869724, a));
         }
      }

      int ry = top + stageH + 2;
      float ra = ease(clamp01((t - 0.3F) / 0.4F)) * pa;
      String label = "RECRUITS";
      ctx.drawTextWithShadow(this.textRenderer, label, 12, ry, MenuBackdrop.argb(9413560, ra));
      ctx.fill(16 + this.textRenderer.getWidth(label), ry + 4, this.width - 12, ry + 5, MenuBackdrop.argb(tint(), 0.3F * ra));
      List ros = this.roster();
      ros.removeIf(VoidMenuScreen.Member::isYou);
      int gy = ry + 12;
      int gh = this.height - gy - 10;
      if (ros.isEmpty()) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, "Nobody yet.", this.width / 2, gy + 10, MenuBackdrop.argb(6710903, ra));
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            "Recruits join through the story, or make your own on the VESSELS page.",
            this.width / 2,
            gy + 22,
            MenuBackdrop.argb(4869724, ra)
         );
      } else {
         int gap = 8;
         int cardW = (int)clamp((float)(this.width - 24 - gap * 2) / 3.0F, 150.0F, 230.0F);
         int cols = Math.max(1, (this.width - 24 + gap) / (cardW + gap));
         int cardH = 42;
         int gridW = cols * (cardW + gap) - gap;
         int gx = this.width / 2 - gridW / 2;
         int rows = (ros.size() + cols - 1) / cols;
         float maxScroll = (float)Math.max(0, rows * (cardH + gap) - gap - gh);
         this.rosterScrollTarget = clamp(this.rosterScrollTarget, 0.0F, maxScroll);
         this.rosterScroll = this.approach(this.rosterScroll, this.rosterScrollTarget, 16.0F);
         this.scrollArea(0, gy, this.width, gh, d -> this.rosterScrollTarget -= (float)d * (float)(cardH + gap));
         ctx.enableScissor(0, gy - 3, this.width, gy + gh);

         for (int i = 0; i < ros.size(); i++) {
            VoidMenuScreen.Member mxx = ros.get(i);
            int xx = gx + i % cols * (cardW + gap);
            int y = gy + i / cols * (cardH + gap) - Math.round(this.rosterScroll);
            if (y + cardH >= gy && y <= gy + gh) {
               float ax = ease(clamp01((t - 0.35F - (float)i * 0.05F) / 0.35F)) * pa;
               boolean in1 = mxx.code() == s1;
               boolean in2 = mxx.code() == s2;
               boolean inParty = in1 || in2;
               int colx = mxx.color();
               boolean hovx = inside((double)mx, (double)my, xx, y, cardW, cardH) && my >= gy && my < gy + gh;
               if (hovx) {
                  this.hover("card" + mxx.code());
               }

               this.panel(ctx, xx, y, cardW, cardH, colx, ax, inParty || hovx);
               if (inParty) {
                  ctx.fill(xx, y, xx + 3, y + cardH, MenuBackdrop.argb(colx, ax));
               }

               this.face(ctx, this.skinOf(mxx), xx + 7, y + 7, 20, ax);
               this.soulHeart(ctx, mxx.soul(), (float)(xx + 21), (float)(y + 21), 8, ax);
               int tx = xx + 33;
               int btnsW = 48;
               ctx.drawTextWithShadow(
                  this.textRenderer, this.fit(mxx.name(), cardW - 33 - btnsW - 8), tx, y + 6, MenuBackdrop.argb(hovx ? 16777024 : 16777215, ax)
               );
               String subx = "LV " + mxx.level() + (mxx.cls() != null ? "  \u00b7  " + mxx.cls().displayName : "");
               ctx.drawTextWithShadow(this.textRenderer, this.fit(subx, cardW - 33 - btnsW - 8), tx, y + 17, MenuBackdrop.argb(9413560, ax));
               this.hpBar(ctx, tx, y + 31, cardW - 33 - 8, 3, mxx.hp(), mxx.maxHp(), 0.0F, ax);
               int bx = xx + cardW - btnsW - 6;
               int by = y + 6;
               this.slotButton(ctx, bx, by, "I", in1, mx, my, ax, () -> this.setSlot(1, in1 ? -1 : m.code(), m));
               this.slotButton(ctx, bx + 26, by, "II", in2, mx, my, ax, () -> this.setSlot(2, in2 ? -1 : m.code(), m));
               int clickH = Math.max(0, Math.min(cardH, gy + gh - Math.max(y, gy)));
               this.click(xx, Math.max(y, gy), cardW, clickH, () -> {
                  if (in1) {
                     this.setSlot(1, -1, m);
                  } else if (in2) {
                     this.setSlot(2, -1, m);
                  } else if (TurnBattleClientMod.clientActiveRecruit < 0) {
                     this.setSlot(1, m.code(), m);
                  } else if (TurnBattleClientMod.clientSecondRecruit < 0) {
                     this.setSlot(2, m.code(), m);
                  } else {
                     TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
                     this.popup("FULL", (float)x + (float)cardW / 2.0F, (float)(y + 8), 16746615);
                  }
               });
            }
         }

         ctx.disableScissor();
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            "Click someone to bring them  \u00b7  I / II puts them in a slot",
            this.width / 2,
            this.height - 10,
            MenuBackdrop.argb(4869724, ra)
         );
      }
   }

   private void slotButton(DrawContext ctx, int x, int y, String label, boolean on, int mx, int my, float a, Runnable r) {
      int w = 22;
      int h = 12;
      boolean hov = inside((double)mx, (double)my, x, y, w, h);
      if (hov) {
         this.hover("slotbtn" + label + x + "," + y);
      }

      ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, MenuBackdrop.argb(on ? 16777215 : (hov ? 16777024 : 3817298), a));
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(on ? 2371919 : (hov ? 1712694 : 0), a));
      ctx.drawCenteredTextWithShadow(this.textRenderer, label, x + w / 2, y + 2, MenuBackdrop.argb(on ? 16777215 : (hov ? 16777024 : 9212838), a));
      this.click(x - 1, y - 1, w + 2, h + 2, r);
   }

   private void setSlot(int slot, int code, VoidMenuScreen.Member who) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(code);
      if (slot == 1) {
         TurnBattleClientMod.clientActiveRecruit = code;
         if (code >= 0 && TurnBattleClientMod.clientSecondRecruit == code) {
            TurnBattleClientMod.clientSecondRecruit = -1;
         }

         Net121.sendToServer(TurnBattleMod.RECRUIT_SET_ACTIVE, buf);
      } else {
         TurnBattleClientMod.clientSecondRecruit = code;
         if (code >= 0 && TurnBattleClientMod.clientActiveRecruit == code) {
            TurnBattleClientMod.clientActiveRecruit = -1;
         }

         Net121.sendToServer(TurnBattleMod.SET_SECOND_RECRUIT, buf);
      }

      float[] at = this.padAt.get(slot == 1 ? 0 : 2);
      if (code >= 0) {
         TurnBattleSounds.playClient(TurnBattleSounds.SND_MERCYADD, 0.8F, 1.0F);
         if (at != null) {
            this.pulse(at[0], at[1] - 20.0F, who != null ? who.color() : tint(), 24.0F);
         }

         this.act(who, CloneAnimator.Act.ACT, who != null ? who.color() : tint(), null);
      } else {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 0.8F);
         if (at != null) {
            this.pulse(at[0], at[1], 9212838, 16.0F);
         }
      }
   }

   private void renderVessels(DrawContext ctx, int top, int mx, int my, float pa) {
      float t = this.secs();
      String[] names = TurnBattleClientMod.customRecruitNames;
      int n = names != null ? names.length : 0;
      if (this.vesselPicked >= n) {
         this.vesselPicked = Math.max(0, n - 1);
      }

      int leftW = (int)clamp((float)this.width * 0.36F, 170.0F, 270.0F);
      int lx = 12;
      float la = ease(clamp01((t - 0.15F) / 0.35F)) * pa;
      int bh = 22;
      if (inside((double)mx, (double)my, lx, top, leftW, bh)) {
         MenuBackdrop.tex(ctx, TEX_GLOW, (float)lx + (float)leftW / 2.0F, (float)top + (float)bh / 2.0F, (float)leftW * 1.2F, 50.0F, tint(), 0.25F);
      }

      this.button(ctx, lx, top, leftW, bh, "+  CREATE A NEW VESSEL", mx, my, true, la, () -> {
         TurnBattleSounds.playSelect();
         if (this.client != null) {
            this.client.setScreen(new VesselCreatorScreen(this));
         }
      });
      int ly = top + bh + 10;
      String label = "YOUR VESSELS" + (n > 0 ? "  (" + n + ")" : "");
      ctx.drawTextWithShadow(this.textRenderer, label, lx, ly, MenuBackdrop.argb(9413560, la));
      ctx.fill(lx + 4 + this.textRenderer.getWidth(label), ly + 4, lx + leftW, ly + 5, MenuBackdrop.argb(tint(), 0.3F * la));
      int listY = ly + 12;
      int listH = this.height - listY - 10;
      int cardH = 30;
      int gap = 4;
      if (n == 0) {
         List lines = this.textRenderer
            .wrapLines(
               StringVisitable.plain(
                  "None yet. A vessel is a companion of your own making, you choose their name and SOUL, and they fight at your side like any recruit."
               ),
               leftW - 8
            );
         int yy = listY + 4;

         for (OrderedText o : lines) {
            ctx.drawTextWithShadow(this.textRenderer, o, lx + 2, yy, MenuBackdrop.argb(6710903, la));
            yy += 10;
         }
      } else {
         float maxScroll = (float)Math.max(0, n * (cardH + gap) - gap - listH);
         this.vesselScrollTarget = clamp(this.vesselScrollTarget, 0.0F, maxScroll);
         this.vesselScroll = this.approach(this.vesselScroll, this.vesselScrollTarget, 16.0F);
         this.scrollArea(lx, listY, leftW, listH, d -> this.vesselScrollTarget -= (float)d * (float)(cardH + gap));
         ctx.enableScissor(lx - 2, listY, lx + leftW + 2, listY + listH);
         int s1 = TurnBattleClientMod.clientActiveRecruit;
         int s2 = TurnBattleClientMod.clientSecondRecruit;

         for (int ci = 0; ci < n; ci++) {
            VoidMenuScreen.Member m = this.custom(ci);
            int y = listY + ci * (cardH + gap) - Math.round(this.vesselScroll);
            if (y + cardH >= listY && y <= listY + listH) {
               float a = ease(clamp01((t - 0.2F - (float)ci * 0.05F) / 0.35F)) * pa;
               boolean sel = ci == this.vesselPicked;
               boolean hov = !sel && inside((double)mx, (double)my, lx, y, leftW, cardH) && my >= listY && my < listY + listH;
               if (hov) {
                  this.hover("vessel" + ci);
               }

               int col = m.color();
               this.panel(ctx, lx, y, leftW, cardH, col, a, sel);
               if (sel) {
                  ctx.fill(lx, y, lx + 3, y + cardH, MenuBackdrop.argb(col, a));
               }

               this.face(ctx, this.skinOf(m), lx + 7, y + 7, 16, a);
               ctx.drawTextWithShadow(
                  this.textRenderer, this.fit(m.name(), leftW - 70), lx + 28, y + 6, MenuBackdrop.argb(sel ? 16777215 : (hov ? 16777024 : 13160411), a)
               );
               String sub = "LV " + m.level() + (m.cls() != null ? "  \u00b7  " + m.cls().displayName : "");
               ctx.drawTextWithShadow(this.textRenderer, this.fit(sub, leftW - 70), lx + 28, y + 17, MenuBackdrop.argb(9413560, a));
               boolean inParty = m.code() == s1 || m.code() == s2;
               if (inParty) {
                  String p = m.code() == s1 ? "SLOT I" : "SLOT II";
                  ctx.drawTextWithShadow(this.textRenderer, p, lx + leftW - 6 - this.textRenderer.getWidth(p), y + 11, MenuBackdrop.argb(col, a));
               }

               int idx = ci;
               this.click(lx, Math.max(y, listY), leftW, Math.max(0, Math.min(cardH, listY + listH - Math.max(y, listY))), () -> {
                  if (this.vesselPicked != idx) {
                     TurnBattleSounds.playSelect();
                     this.vesselPicked = idx;
                     this.vesselPickedNanos = System.nanoTime();
                     this.removeArmed = -1;
                  }
               });
            }
         }

         ctx.disableScissor();
      }

      int sx0 = lx + leftW + 16;
      int sx1 = this.width - 12;
      float scx = (float)(sx0 + sx1) / 2.0F;
      int infoW = Math.min(sx1 - sx0, 260);
      if (n == 0) {
         float ax = ease(clamp01((t - 0.25F) / 0.4F)) * pa;
         int footY = top + (this.height - top) / 2;
         MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY, 150.0F, 36.0F, 5922928, 0.3F * ax);
         MenuBackdrop.tex(ctx, TEX_RING, scx, (float)footY, 110.0F, 26.0F, 5922928, 0.35F * ax);
         float br = 0.5F + 0.5F * (float)Math.sin((double)(t * 1.6F));
         ctx.fillGradient((int)scx - 1, footY - 70, (int)scx + 1, footY, MenuBackdrop.argb(9212838, 0.0F), MenuBackdrop.argb(9212838, 0.35F * br * ax));
         this.soulHeart(ctx, null, scx - 6.0F, (float)(footY - 44), 12, 0.35F * br * ax);
         ctx.drawCenteredTextWithShadow(this.textRenderer, "Make a companion of your own", (int)scx, footY + 12, MenuBackdrop.argb(9413560, ax));
      } else {
         VoidMenuScreen.Member m = this.custom(this.vesselPicked);
         float ax = ease(clamp01((t - 0.2F) / 0.4F)) * pa;
         float sw = ease(clamp01(since(this.vesselPickedNanos) / 0.35F));
         int colx = m.color();
         int infoH = 92;
         int footY = this.height - 10 - infoH - 24;
         float scale = clamp(Math.min((float)(footY - top - 20) / 2.1F, (float)(sx1 - sx0) * 0.2F), 12.0F, 56.0F);
         MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY, scale * 4.2F, scale * 1.0F, colx, 0.55F * ax);
         MenuBackdrop.tex(ctx, TEX_RING, scx, (float)footY, scale * 3.0F, scale * 0.7F, colx, 0.45F * ax);
         MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY - scale, scale * 3.0F, scale * 3.6F, colx, 0.1F * ax);
         Text nm = Text.literal(m.name()).setStyle(Style.EMPTY.withBold(true));
         int nw = this.textRenderer.getWidth(nm);
         this.soulHeart(ctx, m.soul(), scx - (float)nw / 2.0F - 13.0F, (float)top, 9, ax * sw);
         ctx.drawTextWithShadow(this.textRenderer, nm, (int)(scx - (float)nw / 2.0F), top, MenuBackdrop.argb(lerpRgb(colx, 16777215, 0.45F), ax * sw));
         String sub = "LV "
            + m.level()
            + (m.cls() != null ? "  \u00b7  " + m.cls().displayName : "")
            + (m.soul() != null ? "  \u00b7  " + m.soul().getDisplayName() : "");
         ctx.drawCenteredTextWithShadow(this.textRenderer, this.fit(sub, sx1 - sx0), (int)scx, top + 11, MenuBackdrop.argb(9413560, ax * sw));
         this.drawMember(ctx, m, (int)(scx + (1.0F - sw) * 16.0F), footY, scale, 18.0F + (float)Math.sin((double)(t * 0.6F)) * 10.0F, null, ax * sw);
         int ix = (int)(scx - (float)infoW / 2.0F);
         int iy = footY + 18;
         this.box(ctx, ix, iy, infoW, infoH, colx, ax);
         String desc = m.cls() != null ? m.cls().description : "";
         List dl = this.textRenderer.wrapLines(StringVisitable.plain(desc), infoW - 14);
         int ty = iy + 6;

         for (int i = 0; i < dl.size() && i < 3; i++) {
            ctx.drawTextWithShadow(this.textRenderer, dl.get(i), ix + 7, ty, MenuBackdrop.argb(13423581, ax));
            ty += 10;
         }

         CharStats st = this.statsOf(m.code());
         String[] sn = new String[]{"OFF", "DEF", "MAG", "IQ", "VIT", "LCK"};
         int[] rgb = new int[]{16751178, 8370431, 12946687, 8388564, 8257418, 16769126};
         int cw = (infoW - 14) / 6;
         int sy = iy + infoH - 44;

         for (int i = 0; i < 6; i++) {
            int cx = ix + 7 + i * cw;
            ctx.drawTextWithShadow(this.textRenderer, sn[i], cx, sy, MenuBackdrop.argb(rgb[i], ax));
            ctx.drawTextWithShadow(this.textRenderer, String.valueOf(st.valueFor(i)), cx, sy + 10, MenuBackdrop.argb(16777215, ax));
         }

         this.hpBar(ctx, ix + 7, iy + infoH - 18, infoW - 14 - 76, 4, m.hp(), m.maxHp(), 0.0F, ax);
         boolean armed = this.removeArmed == this.vesselPicked && since(this.removeArmedNanos) < 3.0F;
         int idx = this.vesselPicked;
         this.button(ctx, ix + infoW - 7 - 66, iy + infoH - 22, 66, 12, armed ? "SURE?" : "REMOVE", mx, my, true, ax, () -> {
            if (this.removeArmed == idx && since(this.removeArmedNanos) < 3.0F) {
               this.removeVessel(idx);
            } else {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 0.8F, 1.2F);
               this.removeArmed = idx;
               this.removeArmedNanos = System.nanoTime();
            }
         });
         if (armed) {
            ctx.drawTextWithShadow(
               this.textRenderer, this.fit("Click again to let " + m.name() + " go", infoW - 14), ix + 7, iy + infoH + 6, MenuBackdrop.argb(16746615, ax)
            );
         }
      }
   }

   private void removeVessel(int index) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(index);
      Net121.sendToServer(TurnBattleMod.REMOVE_VESSEL, buf);
      TurnBattleSounds.playClient(TurnBattleSounds.SND_GRAB, 0.8F, 0.7F);
      this.removeArmed = -1;
      this.vesselPicked = Math.max(0, index - 1);
      this.vesselPickedNanos = System.nanoTime();
   }

   private void renderPlayers(DrawContext ctx, int top, int mx, int my, float pa) {
      float t = this.secs();
      int colW = (int)clamp((float)(this.width - 36) / 2.0F, 160.0F, 320.0F);
      int lx = this.width / 2 - colW - 6;
      int rx = this.width / 2 + 6;
      float la = ease(clamp01((t - 0.15F) / 0.35F)) * pa;
      float ra = ease(clamp01((t - 0.25F) / 0.35F)) * pa;
      int accent = tint();
      String label = "YOUR PARTY";
      ctx.drawTextWithShadow(this.textRenderer, label, lx, top, MenuBackdrop.argb(9413560, la));
      ctx.fill(lx + 4 + this.textRenderer.getWidth(label), top + 4, lx + colW, top + 5, MenuBackdrop.argb(accent, 0.3F * la));
      int y = top + 12;
      int cardH = 30;
      int gap = 4;
      VoidMenuScreen.Member me = this.you();
      this.playerCard(ctx, lx, y, colW, cardH, this.skinOf(me), me.name(), "You", me.soul(), me.hp(), me.maxHp(), la, false, mx, my);
      y += cardH + gap;
      List mates = TurnBattleClientMod.clientPartyPlayers;
      int maxCards = Math.max(1, (this.height - 120 - y) / (cardH + gap));
      int shown = 0;

      for (TurnBattleClientMod.PartyMemberInfo pm : mates) {
         if (shown++ >= maxCards) {
            break;
         }

         String name = pm.vesselName() != null && !pm.vesselName().isEmpty() ? pm.vesselName() : pm.name();
         this.playerCard(
            ctx,
            lx,
            y,
            colW,
            cardH,
            this.skinFor(pm.uuid()),
            name,
            pm.vesselName() != null && !pm.vesselName().isEmpty() ? pm.name() : "",
            pm.soul(),
            pm.hp(),
            pm.maxHp(),
            la,
            false,
            mx,
            my
         );
         y += cardH + gap;
      }

      if (mates.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, "Just you, for now.", lx + 4, y + 2, MenuBackdrop.argb(6710903, la));
         y += 14;
      }

      y += 6;
      this.button(ctx, lx + 2, y, 110, 13, "LEAVE PARTY", mx, my, !mates.isEmpty(), la, () -> {
         Net121.sendToServer(TurnBattleMod.PARTY_LEAVE, PacketByteBufs.empty());
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 0.8F);
      });
      y += 26;
      boolean pub = TurnBattleClientMod.publicInvites;
      ctx.drawTextWithShadow(this.textRenderer, "PUBLIC INVITES", lx, y, MenuBackdrop.argb(9413560, la));
      int swX = lx + colW - 40;
      int swY = y - 2;
      int swW = 36;
      int swH = 12;
      boolean swHov = inside((double)mx, (double)my, swX, swY, swW, swH);
      if (swHov) {
         this.hover("switch");
      }

      float knob = pub ? 1.0F : 0.0F;
      ctx.fill(swX - 1, swY - 1, swX + swW + 1, swY + swH + 1, MenuBackdrop.argb(swHov ? 16777024 : (pub ? 16777215 : 3817298), la));
      ctx.fill(swX, swY, swX + swW, swY + swH, MenuBackdrop.argb(pub ? lerpRgb(329230, 5635959, 0.35F) : 329230, la));
      int kx = swX + 2 + (int)(knob * (float)(swW - 12));
      ctx.fill(kx, swY + 2, kx + 8, swY + swH - 2, MenuBackdrop.argb(pub ? 5635959 : 6710903, la));
      ctx.drawTextWithShadow(
         this.textRenderer, pub ? "ON" : "OFF", swX - 6 - this.textRenderer.getWidth(pub ? "ON" : "OFF"), y, MenuBackdrop.argb(pub ? 5635959 : 9212838, la)
      );
      this.click(swX - 30, swY - 2, swW + 32, swH + 4, () -> {
         TurnBattleClientMod.publicInvites = !TurnBattleClientMod.publicInvites;
         PacketByteBuf tb = PacketByteBufs.create();
         tb.writeBoolean(TurnBattleClientMod.publicInvites);
         Net121.sendToServer(TurnBattleMod.PARTY_INVITE_TOGGLE, tb);
         TurnBattleSounds.playSelect();
      });
      y += 13;
      String why = pub ? "Anyone online can invite you to their party." : "Nobody can send you an invite.";

      for (OrderedText o : this.textRenderer.wrapLines(StringVisitable.plain(why), colW - 4)) {
         ctx.drawTextWithShadow(this.textRenderer, o, lx, y, MenuBackdrop.argb(4869724, la));
         y += 10;
      }

      String ol = "ONLINE";
      ctx.drawTextWithShadow(this.textRenderer, ol, rx, top, MenuBackdrop.argb(9413560, ra));
      int refW = 56;
      this.button(ctx, rx + colW - refW - 2, top - 1, refW, 10, "REFRESH", mx, my, true, ra, () -> {
         Net121.sendToServer(TurnBattleMod.ROGUE_INFO_REQUEST, PacketByteBufs.empty());
         TurnBattleSounds.playSelect();
      });
      ctx.fill(rx + 4 + this.textRenderer.getWidth(ol), top + 4, rx + colW - refW - 8, top + 5, MenuBackdrop.argb(accent, 0.3F * ra));
      Set inParty = new HashSet<>();

      for (TurnBattleClientMod.PartyMemberInfo pm : mates) {
         inParty.add(pm.uuid());
      }

      UUID me2 = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
      List others = new ArrayList<>();

      for (TurnBattleClientMod.OnlinePlayerEntry op : TurnBattleClientMod.onlinePlayers) {
         if (!inParty.contains(op.uuid()) && !op.uuid().equals(me2)) {
            others.add(op);
         }
      }

      int listY = top + 14;
      int listH = this.height - listY - 10;
      int rowH = 24;
      if (others.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, "Nobody else is online.", rx + 4, listY + 4, MenuBackdrop.argb(6710903, ra));
      } else {
         float maxScroll = (float)Math.max(0, others.size() * rowH - listH);
         this.onlineScrollTarget = clamp(this.onlineScrollTarget, 0.0F, maxScroll);
         this.onlineScroll = this.approach(this.onlineScroll, this.onlineScrollTarget, 16.0F);
         this.scrollArea(rx, listY, colW, listH, d -> this.onlineScrollTarget -= (float)d * (float)rowH);
         ctx.enableScissor(rx - 2, listY, rx + colW + 2, listY + listH);

         for (int i = 0; i < others.size(); i++) {
            TurnBattleClientMod.OnlinePlayerEntry opx = others.get(i);
            int ry = listY + i * rowH - Math.round(this.onlineScroll);
            if (ry + rowH >= listY && ry <= listY + listH) {
               float a = ease(clamp01((t - 0.3F - (float)i * 0.04F) / 0.3F)) * pa;
               boolean hov = inside((double)mx, (double)my, rx, ry, colW, rowH - 3) && my >= listY && my < listY + listH;
               ctx.fill(rx, ry, rx + colW, ry + rowH - 3, MenuBackdrop.argb(hov ? lerpRgb(724506, accent, 0.16F) : 724506, 0.9F * a));
               this.face(ctx, this.skinFor(opx.uuid()), rx + 5, ry + 3, 16, a);
               ctx.drawTextWithShadow(this.textRenderer, this.fit(opx.name(), colW - 100), rx + 26, ry + 7, MenuBackdrop.argb(16777215, a));
               Long sent = this.invitedAt.get(opx.uuid());
               boolean justSent = sent != null && since(sent) < 4.0F;
               UUID who = opx.uuid();
               String nm = opx.name();
               if (justSent) {
                  ctx.drawTextWithShadow(
                     this.textRenderer, "INVITED", rx + colW - 8 - this.textRenderer.getWidth("INVITED"), ry + 7, MenuBackdrop.argb(5635959, a)
                  );
               } else if (ry >= listY && ry + rowH - 3 <= listY + listH) {
                  this.button(ctx, rx + colW - 6 - 52, ry + 5, 52, 11, "INVITE", mx, my, true, a, () -> this.invite(who, nm));
               }
            }
         }

         ctx.disableScissor();
      }
   }

   private void playerCard(
      DrawContext ctx,
      int x,
      int y,
      int w,
      int h,
      Identifier skin,
      String name,
      String sub,
      SoulType soul,
      float hp,
      float max,
      float a,
      boolean lit,
      int mx,
      int my
   ) {
      int col = soul != null ? soul.getColor() & 16777215 : 12107984;
      this.panel(ctx, x, y, w, h, col, a, lit);
      ctx.fill(x, y, x + 3, y + h, MenuBackdrop.argb(col, a));
      this.face(ctx, skin, x + 8, y + 7, 16, a);
      this.soulHeart(ctx, soul, (float)(x + w - 16), (float)(y + 6), 9, a);
      ctx.drawTextWithShadow(this.textRenderer, this.fit(name, w - 60), x + 30, y + 5, MenuBackdrop.argb(16777215, a));
      if (sub != null && !sub.isEmpty()) {
         ctx.drawTextWithShadow(
            this.textRenderer,
            this.fit(sub, w - 60 - this.textRenderer.getWidth(name)),
            x + 34 + this.textRenderer.getWidth(this.fit(name, w - 60)),
            y + 5,
            MenuBackdrop.argb(6710903, a)
         );
      }

      if (max > 0.0F) {
         this.hpBar(ctx, x + 30, y + 18, w - 30 - 24, 3, hp, max, 0.0F, a);
      }
   }

   private Identifier skinFor(UUID uuid) {
      if (this.client != null && this.client.getNetworkHandler() != null && uuid != null) {
         PlayerListEntry e = this.client.getNetworkHandler().getPlayerListEntry(uuid);
         return e != null ? e.getSkinTextures().texture() : null;
      } else {
         return null;
      }
   }

   private void invite(UUID who, String name) {
      PacketByteBuf ib = PacketByteBufs.create();
      ib.writeUuid(who);
      Net121.sendToServer(TurnBattleMod.PARTY_INVITE, ib);
      this.invitedAt.put(who, System.nanoTime());
      TurnBattleSounds.playClient(TurnBattleSounds.SND_MERCYADD, 0.7F, 1.2F);
   }
}
