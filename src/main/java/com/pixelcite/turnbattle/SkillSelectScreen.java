package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class SkillSelectScreen extends VoidMenuScreen {
   private float scroll;
   private float scrollTarget;
   private final Map<Integer, Float> light = new HashMap<>();

   public SkillSelectScreen(Screen parent) {
      super("Skills", parent);
   }

   @Override
   protected int navIndex() {
      return 1;
   }

   @Override
   protected void onFirstOpen() {
      Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.SKILL_INFO_REQUEST, PacketByteBufs.empty());
   }

   @Override
   protected String headerRight() {
      int waiting = 0;

      for (VoidMenuScreen.Member m : this.roster()) {
         if (hasTree(m)) {
            waiting += skillPoints(m);
         }
      }

      return waiting > 0 ? waiting + " SP waiting" : "";
   }

   private static boolean hasTree(VoidMenuScreen.Member m) {
      return !m.isYou() && !m.isCustom()
         ? m.code() >= 0 && m.code() < Recruit.values().length && !SkillTreeDefs.getRecruitTree(Recruit.values()[m.code()]).isEmpty()
         : true;
   }

   private static int skillPoints(VoidMenuScreen.Member m) {
      if (m.isYou()) {
         return TurnBattleClientMod.clientSkillPoints;
      } else {
         return m.isCustom()
            ? at(TurnBattleClientMod.customRecruitSkillPoints, m.code() - 1000, 0)
            : at(TurnBattleClientMod.clientRecruitSkillPoints, m.code(), 0);
      }
   }

   private static Set<String> unlocked(VoidMenuScreen.Member m) {
      Set s = m.isYou()
         ? TurnBattleClientMod.clientUnlockedSkills
         : (m.isCustom() ? at(TurnBattleClientMod.customRecruitSkills, m.code() - 1000) : at(TurnBattleClientMod.clientRecruitSkills, m.code()));
      return s != null ? s : Set.of();
   }

   private static Set<String> unequipped(VoidMenuScreen.Member m) {
      Set s = m.isYou()
         ? TurnBattleClientMod.clientUnequippedSkills
         : (m.isCustom() ? at(TurnBattleClientMod.customRecruitUnequippedSkills, m.code() - 1000) : TurnBattleClientMod.clientRecruitUnequippedSkills);
      return s != null ? s : Set.of();
   }

   private static int[] loadout(VoidMenuScreen.Member m) {
      int known = 0;
      int used = 0;
      Set uneq = unequipped(m);

      for (String s : unlocked(m)) {
         if (!SkillTreeDefs.isStatNode(s)) {
            known++;
            if (!uneq.contains(s)) {
               used += SkillTreeDefs.recruitSlotCost(s, m.cls(), m.soul());
            }
         }
      }

      return new int[]{known, used, SkillTreeDefs.maxSkillSlots(m.level())};
   }

   @Override
   protected void renderBody(DrawContext ctx, int mx, int my) {
      float t = this.secs();
      int top = this.bodyTop();
      List ros = this.roster();
      int n = ros.size();
      float spacing = clamp(((float)this.width - 40.0F) / (float)n, 110.0F, 180.0F);
      float rowW = spacing * (float)n;
      float maxScroll = Math.max(0.0F, rowW - (float)(this.width - 20));
      this.scrollTarget = clamp(this.scrollTarget, 0.0F, maxScroll);
      this.scroll = this.approach(this.scroll, this.scrollTarget, 14.0F);
      if (maxScroll > 0.0F) {
         this.scrollArea(0, top, this.width, this.height - top, d -> this.scrollTarget -= (float)d * spacing);
      }

      float x0 = (maxScroll > 0.0F ? 10.0F + spacing / 2.0F : (float)this.width / 2.0F - rowW / 2.0F + spacing / 2.0F) - this.scroll;
      int infoH = 50;
      int footY = this.height - 12 - infoH - 10;
      float scale = clamp(Math.min((float)(footY - top - 22) / 2.1F, spacing / 3.0F), 12.0F, 56.0F);
      this.renderVoid(ctx, (float)this.width / 2.0F, (float)footY, (float)this.width * 0.9F);
      String hint = "Pick whose skill tree to open";
      ctx.drawCenteredTextWithShadow(this.textRenderer, hint, this.width / 2, top, MenuBackdrop.argb(9413560, ease(clamp01((t - 0.2F) / 0.4F))));

      for (int i = 0; i < n; i++) {
         VoidMenuScreen.Member m = ros.get(i);
         float a = ease(clamp01((t - 0.12F - (float)i * 0.07F) / 0.4F));
         int x = Math.round(x0 + (float)i * spacing);
         if (!((float)x < -spacing) && !((float)x > (float)this.width + spacing)) {
            boolean has = hasTree(m);
            int col = m.color();
            int bw = (int)(spacing - 14.0F);
            int hitTop = (int)((float)footY - scale * 2.2F);
            boolean hov = inside((double)mx, (double)my, x - bw / 2, hitTop, bw, footY + infoH + 10 - hitTop);
            if (hov) {
               this.hover("member" + m.code());
            }

            float l = this.approach(this.light.getOrDefault(m.code(), 0.0F), hov ? 1.0F : 0.0F, 10.0F);
            this.light.put(m.code(), l);
            float dim = has ? 1.0F : 0.45F;
            MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, (float)footY, scale * (3.2F + l), scale * (0.8F + 0.2F * l), col, (0.45F + 0.35F * l) * a * dim);
            MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, (float)footY - scale, scale * 2.6F, scale * 3.3F, col, (0.06F + 0.14F * l) * a * dim);
            if (l > 0.02F) {
               float rq = t * 0.7F % 1.0F;
               MenuBackdrop.tex(ctx, TEX_RING, (float)x, (float)footY, scale * (2.0F + rq * 2.0F), scale * (0.5F + rq * 0.5F), col, 0.55F * (1.0F - rq) * l);
            }

            float yaw = (float)Math.sin((double)(t * 0.6F + (float)i)) * 8.0F + (hov ? clamp((float)(mx - x) / 10.0F, -16.0F, 16.0F) : 0.0F);
            this.drawMember(ctx, m, x, footY, scale * (1.0F + 0.05F * l), yaw, null, a);
            int sp = has ? skillPoints(m) : 0;
            if (sp > 0) {
               float bob = (float)Math.sin((double)(t * 3.0F + (float)i)) * 2.0F;
               float sy = (float)footY - scale * 2.25F - 6.0F + bob;
               MenuBackdrop.tex(ctx, TEX_GLOW, (float)x, sy, 26.0F, 26.0F, 16768324, 0.55F * a);
               CloneAnimator.star4(ctx, (float)x, sy, 5.0F + (float)Math.sin((double)(t * 5.0F)) * 0.8F, MenuBackdrop.argb(16771174, a));
            }

            int ty = footY + 7;
            Text nm = Text.literal(this.fit(m.name(), bw)).setStyle(Style.EMPTY.withBold(true));
            ctx.drawCenteredTextWithShadow(this.textRenderer, nm, x, ty, MenuBackdrop.argb(hov && has ? 16777024 : lerpRgb(col, 16777215, 0.5F), a));
            ty += 11;
            String sub = "LV " + m.level() + (m.cls() != null ? "  \u00b7  " + m.cls().displayName : "");
            ctx.drawCenteredTextWithShadow(this.textRenderer, this.fit(sub, bw), x, ty, MenuBackdrop.argb(9413560, a));
            ty += 12;
            if (!has) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, "No skill tree yet", x, ty, MenuBackdrop.argb(6710903, a));
            } else if (m.isYou() && TurnBattleClientMod.rogueliteInRun) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, "Your deck", x, ty, MenuBackdrop.argb(13160411, a));
            } else {
               int[] lo = loadout(m);
               String spS = "SP " + sp;
               String slS = "  \u00b7  SLOTS " + lo[1] + "/" + lo[2];
               int w = this.textRenderer.getWidth(spS) + this.textRenderer.getWidth(slS);
               int sx = x - w / 2;
               ctx.drawTextWithShadow(this.textRenderer, spS, sx, ty, MenuBackdrop.argb(sp > 0 ? 16768324 : 9212838, a));
               ctx.drawTextWithShadow(
                  this.textRenderer, slS, sx + this.textRenderer.getWidth(spS), ty, MenuBackdrop.argb(lo[1] >= lo[2] ? 16746615 : 8378623, a)
               );
               ty += 11;
               int barW = Math.min(bw - 10, 90);
               float f = lo[2] > 0 ? clamp01((float)lo[1] / (float)lo[2]) : 0.0F;
               ctx.fill(x - barW / 2 - 1, ty, x + barW / 2 + 1, ty + 4, MenuBackdrop.argb(0, 0.8F * a));
               ctx.fill(x - barW / 2, ty + 1, x + barW / 2, ty + 3, MenuBackdrop.argb(lerpRgb(329230, col, 0.2F), a));
               ctx.fill(x - barW / 2, ty + 1, x - barW / 2 + Math.round((float)barW * f), ty + 3, MenuBackdrop.argb(8378623, a));
            }

            if (hov && has) {
               this.heart(ctx, (float)x - (float)this.textRenderer.getWidth(nm) / 2.0F - 12.0F, (float)(footY + 7), 8, a);
               String open = m.isYou() && TurnBattleClientMod.rogueliteInRun ? "OPEN DECK" : "OPEN SKILL TREE";
               ctx.drawCenteredTextWithShadow(this.textRenderer, open, x, hitTop - 10, MenuBackdrop.argb(16777024, a));
            }

            this.click(x - bw / 2, hitTop, bw, footY + infoH + 10 - hitTop, () -> this.open(m));
         }
      }
   }

   private void open(VoidMenuScreen.Member m) {
      if (this.client != null) {
         if (!hasTree(m)) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
         } else {
            TurnBattleSounds.playSelect();
            if (m.isYou()) {
               if (TurnBattleClientMod.rogueliteInRun) {
                  this.client.setScreen(new RogueDeckScreen());
               } else {
                  openOwnSkillTree();
               }
            } else {
               this.client.setScreen(new SkillTreeScreen(this, m.code()));
            }
         }
      }
   }
}
