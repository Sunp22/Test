package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class StatsScreen extends VoidMenuScreen {
   private static final int[] ORDER = new int[]{0, 1, 2, 4, 3, 5};
   private static final String[] NAMES = new String[]{"Offense", "Defense", "Magic", "IQ", "Vitality", "Luck"};
   private static final Identifier[] ICONS = new Identifier[]{
      gui("spr_offenseicon.png"),
      gui("spr_defenseicon.png"),
      gui("spr_magicicon.png"),
      gui("spr_iqicon.png"),
      gui("spr_vitalityicon.png"),
      gui("spr_luckicon.png")
   };
   private static final int[] ICON_H = new int[]{10, 10, 10, 9, 10, 10};
   private static final String[] WHAT = new String[]{
      "Bash damage +18% per point. The physical skills, slashes and strikes, grow with it too.",
      "Every hit you take is cut by a share, more with each point but less each time (40% at 50, nearing but never reaching 80%).",
      "Skills +1.7% per point, their damage, their healing, and the TP they draw.",
      "Graze and Defend give more TP.",
      "+10 max HP per point.",
      "The chance of a SMAAAASH!! critical hit, and of dodging."
   };
   private static final Identifier TEX_STATS = gui("spr_statstext.png");
   private static final Identifier TEX_STATS_HL = gui("spr_statstext_highlight.png");
   private static final Identifier TEX_SKILLS = gui("spr_skilltext.png");
   private static final Identifier TEX_SKILLS_HL = gui("spr_skilltext_highlight.png");
   private static final Identifier TEX_EQUIP = gui("spr_equiptext.png");
   private static final Identifier TEX_EQUIP_HL = gui("spr_equiptexthighlight.png");
   private static final Identifier TEX_INFO = gui("spr_infotext.png");
   private static final Identifier TEX_TP = gui("spr_tptext.png");
   private static final Identifier TEX_STAT_SOUL = gui("spr_statsoul.png");
   private static final Identifier TEX_SLOT = gui("spr_equipslot.png");
   private static final Identifier TEX_UP = gui("spr_uparrow_highlight.png");
   private static final Identifier TEX_UP_OFF = gui("spr_uparrow_nohighlight.png");
   private static final Identifier TEX_DOWN = gui("spr_downarrow_highlight.png");
   private static final Identifier TEX_DOWN_OFF = gui("spr_downarrow_nohighlight.png");
   private static final Identifier[] SLOT_ICONS = new Identifier[]{
      gui("spr_weaponicon.png"), gui("spr_accessoryicon.png"), gui("spr_armoricon1.png"), gui("spr_armoricon2.png")
   };
   private static final int[] SLOT_ICON_H = new int[]{16, 15, 13, 13};
   private static final String[] SLOT_NAMES = new String[]{"Weapon", "Accessory", "Armor", "Armor"};
   private static final int WHITE = 16777215;
   private static final int YELLOW = 16776960;
   private static final int GREY = 8421504;
   private static final int DIM = 4210752;
   private static final int GREEN = 65280;
   private static final int ORANGE = 16752704;
   private static final int SKY = 8438015;
   private int selected = -1;
   private long pickedNanos;
   private final long[] spentNanos = new long[6];
   private int skillTop = 0;
   private float figureYaw = 18.0F;
   private long resetArmedNanos;
   private String info;
   private int infoColor;

   private static Identifier gui(String n) {
      return Identifier.of("turnbattle", "textures/gui/" + n);
   }

   public StatsScreen(Screen parent) {
      super("Stats", parent);
   }

   @Override
   protected int navIndex() {
      return 0;
   }

   @Override
   protected void onFirstOpen() {
      this.pickedNanos = System.nanoTime();
      Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.EQUIP_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.ACCESSORY_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.ARMOR_INFO_REQUEST, PacketByteBufs.empty());
   }

   @Override
   protected String headerRight() {
      String area = "";
      if (this.client != null && this.client.player != null && this.client.world != null) {
         int lv = AreaLevel.at(
            this.client.world.getRegistryKey().getValue().toString(),
            this.client.player.getX(),
            this.client.player.getZ(),
            TurnBattleClientMod.clientDistanceLevelMult
         );
         area = this.client.world.getRegistryKey().getValue().toString().startsWith("turnbattle:dark_world_") ? "" : "AREA LV " + lv + "    ";
      }

      return area + "D$ " + TurnBattleClientMod.clientDarkDollars;
   }

   private static Text dr(String s) {
      return Text.literal(s).setStyle(Style.EMPTY.withFont(DAMAGE_FONT));
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

   private void sprite(DrawContext ctx, Identifier id, int x, int y, int nw, int nh, int scale, float a) {
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
      ctx.drawTexture(id, x, y, nw * scale, nh * scale, 0.0F, 0.0F, nw, nh, nw, nh);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void cursor(DrawContext ctx, int x, int y, int h, float a) {
      int s = 8;
      this.heart(ctx, (float)(x - s - 3), (float)y + (float)(h - s) / 2.0F, s, a);
   }

   private void say(String text, int rgb) {
      this.info = text;
      this.infoColor = rgb;
   }

   @Override
   protected void renderBody(DrawContext ctx, int mx, int my) {
      this.info = null;
      List ros = this.roster();
      VoidMenuScreen.Member sel = this.member(this.selected);
      if (sel == null) {
         this.selected = -1;
         sel = ros.get(0);
      }

      float t = this.secs();
      float in = ease(clamp01((t - 0.05F) / 0.3F));
      this.renderVoid(ctx, (float)this.width / 2.0F, (float)(this.height + 400), 1.0F);
      int top = this.bodyTop();
      int pw = Math.min(this.width - 16, 620);
      int px = this.width / 2 - pw / 2;
      int tabsH = 22;
      int infoH = this.height < 300 ? 34 : 42;
      int py = top + tabsH + 6;
      int ph = this.height - 8 - infoH - 6 - py;
      int sc = ph >= 190 ? 2 : 1;
      this.renderTabs(ctx, ros, px, top, pw, tabsH, mx, my, in);
      this.drBox(ctx, px, py, pw, ph, in);
      int c1 = (int)((float)pw * 0.33F);
      int c2 = (int)((float)pw * 0.34F);
      int x1 = px + 2;
      int x2 = x1 + c1;
      int x3 = x2 + c2;
      int xEnd = px + pw - 2;
      ctx.fill(x2, py + 8, x2 + 1, py + ph - 8, MenuBackdrop.argb(8421504, in));
      ctx.fill(x3, py + 8, x3 + 1, py + ph - 8, MenuBackdrop.argb(8421504, in));
      this.renderCharacter(ctx, sel, x1, py + 2, c1, ph - 4, mx, my, in);
      this.renderStats(ctx, sel, x2 + 1, py + 2, c2 - 1, ph - 4, mx, my, in, sc);
      this.renderSkillsAndEquip(ctx, sel, x3 + 1, py + 2, xEnd - x3 - 1, ph - 4, mx, my, in, sc);
      int iy = py + ph + 6;
      this.drBox(ctx, px, iy, pw, infoH, in);
      this.sprite(ctx, TEX_INFO, px + 8, iy + 6, 22, 6, sc == 2 && infoH >= 40 ? 2 : 1, in);
      if (this.info == null) {
         CharStats st = this.statsOf(sel.code());
         if (st.unspent > 0) {
            this.say(sel.name() + " has " + st.unspent + " stat point" + (st.unspent == 1 ? "" : "s") + " to spend.", 16776960);
         }
      }

      if (this.info != null) {
         int tx = px + 8 + (sc == 2 && infoH >= 40 ? 50 : 30);
         List lines = this.textRenderer.wrapLines(StringVisitable.plain(this.info), px + pw - 10 - tx);
         int ly = iy + (infoH - Math.min(lines.size(), 3) * 10) / 2 + 1;

         for (int i = 0; i < lines.size() && i < 3; i++) {
            ctx.drawTextWithShadow(this.textRenderer, lines.get(i), tx, ly, MenuBackdrop.argb(this.infoColor, in));
            ly += 10;
         }
      }
   }

   private void renderTabs(DrawContext ctx, List<VoidMenuScreen.Member> ros, int px, int y, int pw, int h, int mx, int my, float in) {
      int n = ros.size();
      int gap = 6;
      int tw = Math.min(132, (pw - gap * (n - 1)) / Math.max(1, n));
      int x0 = px + pw / 2 - (tw * n + gap * (n - 1)) / 2;

      for (int i = 0; i < n; i++) {
         VoidMenuScreen.Member m = ros.get(i);
         int x = x0 + i * (tw + gap);
         boolean sel = m.code() == this.selected;
         boolean hov = !sel && inside((double)mx, (double)my, x, y, tw, h);
         if (hov) {
            this.hover("tab" + m.code());
            this.say("Look at " + m.name() + ".", 16777215);
         }

         ctx.fill(x, y, x + tw, y + h, MenuBackdrop.argb(0, 0.94F * in));
         int b = MenuBackdrop.argb(sel ? 16777215 : (hov ? 16776960 : 4210752), in);
         ctx.fill(x, y, x + tw, y + 1, b);
         ctx.fill(x, y + h - 1, x + tw, y + h, b);
         ctx.fill(x, y, x + 1, y + h, b);
         ctx.fill(x + tw - 1, y, x + tw, y + h, b);
         int fx = x + (sel ? 16 : 5);
         if (sel) {
            this.heart(ctx, (float)(x + 5), (float)y + (float)(h - 8) / 2.0F, 8, in);
         }

         this.face(ctx, this.skinOf(m), fx, y + (h - 12) / 2, 12, in);
         ctx.drawTextWithShadow(
            this.textRenderer,
            this.fit(m.name(), tw - (fx - x) - 18),
            fx + 16,
            y + (h - 8) / 2,
            MenuBackdrop.argb(sel ? 16777215 : (hov ? 16776960 : 8421504), in)
         );
         int code = m.code();
         this.click(x, y, tw, h, () -> this.pick(code));
      }
   }

   private void pick(int code) {
      if (code != this.selected) {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
         this.selected = code;
         this.pickedNanos = System.nanoTime();
         this.skillTop = 0;
         this.resetArmedNanos = 0L;
      }
   }

   private void renderCharacter(DrawContext ctx, VoidMenuScreen.Member sel, int x, int y, int w, int h, int mx, int my, float in) {
      float t = this.secs();
      float sw = ease(clamp01(since(this.pickedNanos) / 0.3F));
      int col = sel.color();
      int pad = 8;
      ctx.drawTextWithShadow(this.textRenderer, dr(this.fit(sel.name(), w - 2 * pad)), x + pad, y + 6, MenuBackdrop.argb(16777215, in * sw));
      Text lv = dr("LV " + sel.level());
      ctx.drawTextWithShadow(this.textRenderer, lv, x + w - pad - this.textRenderer.getWidth(lv), y + 6, MenuBackdrop.argb(16776960, in * sw));
      int ny = y + 19;
      String sub = (sel.cls() != null ? sel.cls().displayName : "") + (sel.isYou() && TurnBattleClientMod.clientMagicant ? "  (Magicant)" : "");
      ctx.drawTextWithShadow(this.textRenderer, this.fit(sub, w - 2 * pad), x + pad, ny, MenuBackdrop.argb(8421504, in * sw));
      int styleRowH = 0;
      int by = y + h - (sel.isYou() ? 46 : 32);
      Text hpL = dr("HP");
      ctx.drawTextWithShadow(this.textRenderer, hpL, x + pad, by, MenuBackdrop.argb(16777215, in));
      Text hpN = dr(Math.round(sel.hp()) + "/" + Math.round(sel.maxHp()));
      ctx.drawTextWithShadow(this.textRenderer, hpN, x + w - pad - this.textRenderer.getWidth(hpN), by, MenuBackdrop.argb(16777215, in));
      int barX = x + pad;
      int barW = w - 2 * pad;
      float hf = sel.maxHp() > 0.0F ? clamp01(sel.hp() / sel.maxHp()) : 0.0F;
      ctx.fill(barX, by + 12, barX + barW, by + 15, MenuBackdrop.argb(8388608, in));
      ctx.fill(barX, by + 12, barX + Math.round((float)barW * hf), by + 15, MenuBackdrop.argb(col, in));
      int ey = by + 19;
      if (sel.isYou()) {
         int need = Math.max(1, TurnBattleClientMod.clientExpNeeded);
         Text ex = dr("EXP");
         ctx.drawTextWithShadow(this.textRenderer, ex, x + pad, ey, MenuBackdrop.argb(16777215, in));
         Text en = dr(TurnBattleClientMod.clientExp + "/" + need);
         ctx.drawTextWithShadow(this.textRenderer, en, x + w - pad - this.textRenderer.getWidth(en), ey, MenuBackdrop.argb(16777215, in));
         float ef = clamp01((float)TurnBattleClientMod.clientExp / (float)need);
         ctx.fill(barX, ey + 12, barX + barW, ey + 15, MenuBackdrop.argb(4206592, in));
         ctx.fill(barX, ey + 12, barX + Math.round((float)barW * ef), ey + 15, MenuBackdrop.argb(16776960, in));
         if (inside((double)mx, (double)my, x, ey, w, 16)) {
            this.say("Experience. " + (need - TurnBattleClientMod.clientExp) + " more to the next LV.", 16777215);
         }
      }

      if (inside((double)mx, (double)my, x, by, w, 16)) {
         this.say("HP. If it falls to 0, " + sel.name() + " is knocked down.", 16777215);
      }

      int sy = by - 18;
      SoulType soul = sel.soul();
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor((float)(col >> 16 & 0xFF) / 255.0F, (float)(col >> 8 & 0xFF) / 255.0F, (float)(col & 0xFF) / 255.0F, in);
      ctx.drawTexture(TEX_STAT_SOUL, x + pad, sy, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      ctx.drawTextWithShadow(
         this.textRenderer,
         dr(soul != null ? soul.getDisplayName() : "No SOUL"),
         x + pad + 20,
         sy + 3,
         MenuBackdrop.argb(soul != null ? lerpRgb(col, 16777215, 0.25F) : 8421504, in)
      );
      boolean onSoul = inside((double)mx, (double)my, x + pad, sy, w - 2 * pad, 16);
      this.click(x + pad, sy, w - 2 * pad, 16, () -> {
      });
      if (onSoul) {
         this.hover("soul");
         String d = soul != null && soul.getDescription() != null ? soul.getDescription() : "No SOUL.";
         if (sel.isYou() && TurnBattleClientMod.clientPlayerRace == CharacterRace.MONSTER) {
            int n = TurnBattleClientMod.clientAbsorbedSouls.size();
            d = d + "  SOULs absorbed: " + n + "/7" + (n > 0 ? " (Monster skills +" + Math.round(10.0F * (float)n) + "%)" : "");
         }

         this.say(d, soul != null ? lerpRgb(col, 16777215, 0.4F) : 16777215);
      }

      int figTop = ny + 12 + styleRowH;
      int figBottom = sy - 4;
      float scale = clamp((float)(figBottom - figTop) / 2.0F, 10.0F, 58.0F);
      int fx = x + w / 2;
      MenuBackdrop.tex(ctx, TEX_GLOW, (float)fx, (float)figBottom, scale * 3.4F, scale * 0.7F, col, 0.35F * in);
      this.figureYaw = this.approach(this.figureYaw, 18.0F + (float)Math.sin((double)(t * 0.6F)) * 8.0F + clamp((float)(mx - fx) / 12.0F, -12.0F, 12.0F), 4.0F);
      ctx.enableScissor(x, figTop - 4, x + w, figBottom + 2);
      this.drawMember(ctx, sel, (int)((float)fx + (1.0F - sw) * 12.0F), figBottom, scale, this.figureYaw, null, in * sw);
      ctx.disableScissor();
      if (sel.isYou() ? !DepthsClient.inRun() : sel.code() != Recruit.ORI.ordinal() && !OriRender.isOri(sel.name())) {
         Text sk = dr("SKIN");
         int skw = this.textRenderer.getWidth(sk);
         int skx = x + w - pad - skw;
         boolean hv = inside((double)mx, (double)my, skx - 12, figTop - 2, skw + 14, 14);
         if (hv) {
            this.hover("skin");
            this.cursor(ctx, skx, figTop, 11, in);
            boolean has = sel.isYou() && this.client != null && this.client.player != null && DarkWorldSkinsClient.has(this.client.player.getUuid());
            this.say(
               sel.isYou()
                  ? (has ? "Change the skin you wear in the Dark World. (Right-click: go back to your own.)" : "Choose a skin to wear in the Dark World.")
                  : "Choose a skin for " + sel.name() + ".",
               16777215
            );
         }

         ctx.drawTextWithShadow(this.textRenderer, sk, skx, figTop, MenuBackdrop.argb(hv ? 16776960 : 8421504, in));
         this.click(skx - 12, figTop - 2, skw + 14, 14, () -> this.changeSkin(sel));
         if (sel.isYou()) {
            this.rightClick(skx - 12, figTop - 2, skw + 14, 14, () -> {
               TurnBattleSounds.playSelect();
               DarkWorldSkinsClient.removeOwn();
            });
         }
      }
   }

   private void changeSkin(VoidMenuScreen.Member sel) {
      TurnBattleSounds.playSelect();
      if (sel.isYou()) {
         DarkWorldSkinsClient.pickOwn();
      } else {
         String name = sel.name();
         RecruitSkinManager.pickSkinFile(name, (success, error) -> {
            if (this.client != null && this.client.player != null) {
               if (success) {
                  this.client.player.sendMessage(Text.literal("Updated skin for " + name + ".").formatted(Formatting.GREEN), false);
               } else if (error != null) {
                  this.client.player.sendMessage(Text.literal("Skin picker error: " + error).formatted(Formatting.RED), false);
               }
            }
         });
      }
   }

   private void renderStats(DrawContext ctx, VoidMenuScreen.Member sel, int x, int y, int w, int h, int mx, int my, float in, int sc) {
      float t = this.secs();
      CharStats st = this.statsOf(sel.code());
      int[] gear = this.gearBonus(sel.code());
      int pad = 10;
      boolean hdrHov = inside((double)mx, (double)my, x, y, w, 6 * sc + 10);
      this.sprite(ctx, hdrHov ? TEX_STATS_HL : TEX_STATS, x + w / 2 - 15 * sc, y + 6, 30, 6, sc, in);
      if (hdrHov) {
         this.say("What " + sel.name() + " is made of. Each LV brings points to spend on them.", 16777215);
      }

      int ry0 = y + 6 + 6 * sc + 8;
      int bottomRoom = 26;
      int rowH = (int)clamp((float)(h - (ry0 - y) - bottomRoom) / 6.0F, 12.0F, 20.0F);

      for (int k = 0; k < 6; k++) {
         int i = ORDER[k];
         int ry = ry0 + k * rowH;
         float ra = in * ease(clamp01((since(this.pickedNanos) - (float)k * 0.03F) / 0.2F));
         boolean hov = inside((double)mx, (double)my, x + 4, ry, w - 8, rowH);
         boolean can = st.unspent > 0 && (i != 5 || !st.isMaxed(5));
         int v = st.valueFor(i);
         int cap = Math.max(1, st.capFor(i));
         if (hov) {
            this.hover("stat" + i);
            String extra = gear[i] != 0 ? "  (" + (gear[i] > 0 ? "+" : "") + gear[i] + " from gear)" : "";
            String capS = v >= cap && i != 5 ? "  Past its cap, each point counts for a quarter." : (i == 5 && st.isMaxed(5) ? "  As high as it goes." : "");
            this.say(NAMES[i] + ": " + WHAT[i] + extra + capS, 16777215);
         }

         int lineY = ry + (rowH - 10) / 2;
         int iconX = x + pad + 4;
         if (hov) {
            this.cursor(ctx, iconX, lineY, 10, ra);
         }

         if (can) {
            float pulse = 0.75F + 0.25F * (float)Math.sin((double)(t * 4.0F + (float)k));
            ctx.drawTextWithShadow(this.textRenderer, dr("+"), iconX + 1, lineY, MenuBackdrop.argb(65280, hov ? 1.0F : pulse * ra));
            int si = i;
            int code = sel.code();
            this.click(x + 4, ry, w - 8, rowH, () -> this.spend(code, si));
         } else {
            this.click(x + 4, ry, w - 8, rowH, () -> {
            });
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ra);
            ctx.drawTexture(ICONS[i], iconX, lineY, 9, ICON_H[i], 0.0F, 0.0F, 9, ICON_H[i], 9, ICON_H[i]);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         float flare = this.spentNanos[i] == 0L ? 0.0F : 1.0F - clamp01(since(this.spentNanos[i]) / 0.5F);
         int nameCol = flare > 0.0F ? lerpRgb(16777215, 16776960, flare) : (hov ? 16776960 : 16777215);
         ctx.drawTextWithShadow(this.textRenderer, dr(NAMES[i]), iconX + 14, lineY, MenuBackdrop.argb(nameCol, ra));
         Text vt = dr(String.valueOf(v + gear[i]));
         int vx = x + w - pad - this.textRenderer.getWidth(vt);
         ctx.drawTextWithShadow(this.textRenderer, vt, vx, lineY, MenuBackdrop.argb(v >= cap ? '\uff00' : (gear[i] != 0 ? 8438015 : 16776960), ra));
      }

      int by = y + h - 22;
      if (st.unspent > 0) {
         float p = 0.7F + 0.3F * (float)Math.sin((double)(t * 4.0F));
         ctx.drawTextWithShadow(this.textRenderer, dr("SP " + st.unspent), x + pad + 4, by, MenuBackdrop.argb(16776960, p * in));
      }

      boolean armed = this.resetArmedNanos != 0L && since(this.resetArmedNanos) < 3.0F;
      Text rs = dr(armed ? "Sure?" : "Reset");
      int rx = x + w - pad - this.textRenderer.getWidth(rs);
      boolean rh = inside((double)mx, (double)my, rx - 12, by - 2, this.textRenderer.getWidth(rs) + 14, 14);
      if (rh) {
         this.hover("reset");
         this.cursor(ctx, rx, by, 11, in);
         this.say(
            armed
               ? "Click again to take back every point spent, for 1 Netherite Ingot."
               : "Take back every point " + sel.name() + " has spent, to spend again. Costs 1 Netherite Ingot.",
            16752704
         );
      }

      ctx.drawTextWithShadow(this.textRenderer, rs, rx, by, MenuBackdrop.argb(armed ? 16752704 : (rh ? 16776960 : 8421504), in));
      int code = sel.code();
      this.click(rx - 12, by - 2, this.textRenderer.getWidth(rs) + 14, 14, () -> {
         if (this.resetArmedNanos != 0L && since(this.resetArmedNanos) < 3.0F) {
            this.resetArmedNanos = 0L;
            this.reset(code);
         } else {
            this.resetArmedNanos = System.nanoTime();
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 0.8F, 1.2F);
         }
      });
   }

   private int[] gearBonus(int code) {
      int[] out = new int[6];
      ItemStack w = this.weaponOf(code);
      WeaponStats ws = w.isEmpty() ? null : WeaponStats.fromStack(w);
      if (ws != null) {
         for (int i = 0; i < 6; i++) {
            out[i] += ws.netStatBonus(i);
         }
      }

      for (ItemStack a : this.armorOf(code)) {
         ArmorStats as = a.isEmpty() ? null : ArmorStats.fromStack(a);
         if (as != null) {
            for (int i = 0; i < 6; i++) {
               out[i] += as.netStatBonus(i);
            }
         }
      }

      return out;
   }

   private void renderSkillsAndEquip(DrawContext ctx, VoidMenuScreen.Member sel, int x, int y, int w, int h, int mx, int my, float in, int sc) {
      int pad = 10;
      CharStats st = this.statsOf(sel.code());
      int slot = 20;
      int eqH = 6 * sc + 8 + slot + 8;
      int eqY = y + h - eqH;
      boolean skHov = inside((double)mx, (double)my, x, y, w, 6 * sc + 10);
      this.sprite(ctx, skHov ? TEX_SKILLS_HL : TEX_SKILLS, x + w / 2 - 17 * sc + 8 * sc, y + 6, 34, 6, sc, in);
      this.sprite(ctx, TEX_TP, x + pad, y + 6, 12, 6, sc, in);
      if (skHov) {
         this.say(
            DepthsClient.inRun() && sel.isYou()
               ? "The skills Ori has found in the Depths this run."
               : (
                  TurnBattleClientMod.rogueliteInRun && sel.isYou()
                     ? "Click to open your deck."
                     : "The skills " + sel.name() + " has equipped. Click to open their skill tree."
               ),
            16777215
         );
      }

      this.click(x, y, w, 6 * sc + 10, () -> this.openSkills(sel));
      List skills = this.equippedSkills(sel);
      int listY = y + 6 + 6 * sc + 8 + 8;
      int listBottom = eqY - 12;
      int rowH = 12;
      int visible = Math.max(1, (listBottom - listY) / rowH);
      int maxTop = Math.max(0, skills.size() - visible);
      this.skillTop = Math.max(0, Math.min(this.skillTop, maxTop));
      this.scrollArea(
         x, listY, w, listBottom - listY, d -> this.skillTop = Math.max(0, Math.min(this.maxTopFor(sel, visible), this.skillTop - (int)Math.signum(d)))
      );
      if (maxTop > 0) {
         boolean up = this.skillTop > 0;
         boolean down = this.skillTop < maxTop;
         int ax = x + w / 2 - 5;
         this.sprite(ctx, up ? TEX_UP : TEX_UP_OFF, ax, listY - 8, 10, 6, 1, in);
         this.sprite(ctx, down ? TEX_DOWN : TEX_DOWN_OFF, ax, listY + visible * rowH + 1, 10, 6, 1, in);
         if (up) {
            this.click(ax - 4, listY - 10, 18, 9, () -> {
               this.skillTop--;
               TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
            });
         }

         if (down) {
            this.click(ax - 4, listY + visible * rowH, 18, 9, () -> {
               this.skillTop++;
               TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
            });
         }
      }

      if (skills.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, "No skills equipped", x + pad, listY, MenuBackdrop.argb(8421504, in));
      }

      for (int k = 0; k < visible && this.skillTop + k < skills.size(); k++) {
         BattleSkill s = skills.get(this.skillTop + k);
         int ry = listY + k * rowH;
         boolean hov = inside((double)mx, (double)my, x + 4, ry - 1, w - 8, rowH);
         this.click(x + 4, ry - 1, w - 8, rowH, () -> this.openSkills(sel));
         if (hov) {
            this.hover("skill" + (this.skillTop + k));

            String d;
            try {
               d = s.getLiveDescription(SkillScaling.statOf(st, s));
            } catch (Throwable var35) {
               d = s.getEffectDescription();
            }

            if (SkillScaling.scales(s)) {
               d = d + "  (Scales with " + SkillScaling.statName(s) + ".)";
            }

            this.say(s.getDisplayName() + ": " + d, 16777215);
            this.cursor(ctx, x + pad, ry, 9, in);
         }

         ctx.drawTextWithShadow(this.textRenderer, s.getTpCost() + "%", x + pad, ry, MenuBackdrop.argb(16752704, in));
         ctx.drawTextWithShadow(
            this.textRenderer, this.fit(s.getDisplayName(), w - pad * 2 - 30), x + pad + 28, ry, MenuBackdrop.argb(hov ? 16776960 : 16777215, in)
         );
      }

      ctx.fill(x + 6, eqY - 4, x + w - 6, eqY - 3, MenuBackdrop.argb(4210752, in));
      boolean eqHov = inside((double)mx, (double)my, x, eqY, w, 6 * sc + 8);
      this.sprite(ctx, eqHov ? TEX_EQUIP_HL : TEX_EQUIP, x + w / 2 - 14 * sc, eqY + 2, 28, 6, sc, in);
      if (eqHov) {
         this.say("What " + sel.name() + " has equipped. Click to open Equip.", 16777215);
      }

      this.click(x, eqY, w, 6 * sc + 8, () -> this.openSibling(3));
      ItemStack[] armor = this.armorOf(sel.code());
      ItemStack[] gear = new ItemStack[]{this.weaponOf(sel.code()), this.accessoryOf(sel.code()), armor[0], armor[1]};
      int gap = 6;
      int gw = 4 * slot + 3 * gap;
      int gx = x + w / 2 - gw / 2;
      int gy = eqY + 6 * sc + 6;

      for (int i = 0; i < 4; i++) {
         int sx = gx + i * (slot + gap);
         boolean hov = inside((double)mx, (double)my, sx, gy, slot, slot);
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, in);
         ctx.drawTexture(TEX_SLOT, sx, gy, slot, slot, 0.0F, 0.0F, 24, 24, 24, 24);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (!gear[i].isEmpty()) {
            this.item(ctx, gear[i], (float)sx + (float)slot / 2.0F, (float)gy + (float)slot / 2.0F, 16.0F);
         } else {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.35F * in);
            int ih = Math.min(SLOT_ICON_H[i], 12);
            int iw = Math.round(13.0F * (float)ih / (float)SLOT_ICON_H[i]);
            ctx.drawTexture(SLOT_ICONS[i], sx + (slot - iw) / 2, gy + (slot - ih) / 2, iw, ih, 0.0F, 0.0F, 13, SLOT_ICON_H[i], 13, SLOT_ICON_H[i]);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         if (hov) {
            this.hover("gear" + i);
            this.cursor(ctx, sx, gy, slot, in);
            this.say(SLOT_NAMES[i] + ": " + (gear[i].isEmpty() ? "nothing." : gear[i].getName().getString() + "."), 16777215);
         }

         this.click(sx, gy, slot, slot, () -> this.openSibling(3));
      }
   }

   private int maxTopFor(VoidMenuScreen.Member sel, int visible) {
      return Math.max(0, this.equippedSkills(sel).size() - visible);
   }

   private List<BattleSkill> equippedSkills(VoidMenuScreen.Member m) {
      Set unequipped = Collections.emptySet();
      Set unlocked;
      if (m.isYou()) {
         unlocked = TurnBattleClientMod.clientUnlockedSkills;
         unequipped = TurnBattleClientMod.clientUnequippedSkills;
      } else if (m.isCustom()) {
         int ci = m.code() - 1000;
         Set s = at(TurnBattleClientMod.customRecruitSkills, ci);
         Set u = at(TurnBattleClientMod.customRecruitUnequippedSkills, ci);
         unlocked = s != null ? s : Collections.emptySet();
         if (u != null) {
            unequipped = u;
         }
      } else {
         Set s = at(TurnBattleClientMod.clientRecruitSkills, m.code());
         unlocked = s != null ? s : Collections.emptySet();
      }

      List out = new ArrayList<>();

      for (String n : unlocked) {
         if (!SkillTreeDefs.isStatNode(n) && !unequipped.contains(n)) {
            BattleSkill s = resolve(n);
            if (s != null) {
               out.add(s);
            }
         }
      }

      out.sort(Comparator.comparing(BattleSkill::getDisplayName));
      return out;
   }

   private static BattleSkill resolve(String name) {
      BattleSkill s = SkillTreeDefs.psiByName(name);
      if (s != null) {
         return s;
      } else {
         s = SkillTreeDefs.lightnerByName(name);
         if (s != null) {
            return s;
         } else {
            HumanMageSkill var3 = SkillTreeDefs.humanMageByName(name);
            if (var3 != null) {
               return var3;
            } else {
               SoulMageSkill var4 = SkillTreeDefs.soulMageSkillByName(name);
               if (var4 != null) {
                  return var4;
               } else {
                  MonsterSkill var5 = SkillTreeDefs.monsterSkillByName(name);
                  return (BattleSkill)(var5 != null ? var5 : SkillTreeDefs.oriSkillByName(name));
               }
            }
         }
      }
   }

   private void openSkills(VoidMenuScreen.Member m) {
      if (this.client != null) {
         if (m.isYou() && DepthsClient.inRun()) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
         } else if (m.isYou()) {
            TurnBattleSounds.playSelect();
            if (TurnBattleClientMod.rogueliteInRun) {
               this.client.setScreen(new RogueDeckScreen());
            } else {
               openOwnSkillTree();
            }
         } else if (!m.isCustom() && SkillTreeDefs.getRecruitTree(Recruit.values()[m.code()]).isEmpty()) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
         } else {
            TurnBattleSounds.playSelect();
            this.client.setScreen(new SkillTreeScreen(this, m.code()));
         }
      }
   }

   private void spend(int code, int statIndex) {
      CharStats st = this.statsOf(code);
      if (st.unspent > 0 && (statIndex != 5 || !st.isMaxed(5))) {
         PacketByteBuf buf = PacketByteBufs.create();
         if (code == -1) {
            buf.writeByte(0);
            buf.writeByte(statIndex);
            Net121.sendToServer(TurnBattleMod.STAT_SPEND, buf);
         } else {
            buf.writeInt(code);
            buf.writeInt(statIndex);
            Net121.sendToServer(TurnBattleMod.RECRUIT_STAT_SPEND, buf);
         }

         st.spendPoint(statIndex);
         this.spentNanos[statIndex] = System.nanoTime();
         TurnBattleSounds.playClient(TurnBattleSounds.SND_POWER, 0.7F, 1.0F + (float)statIndex * 0.04F);
      } else {
         TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
      }
   }

   private void reset(int code) {
      int target;
      if (code == -1) {
         target = 0;
      } else if (code >= 1000) {
         target = 20000 + (code - 1000);
      } else {
         int seq = 0;
         target = -1;
         boolean[] unl = TurnBattleClientMod.clientRecruitUnlocked;

         for (int ri = 0; ri < Recruit.values().length; ri++) {
            if (unl != null && ri < unl.length && unl[ri]) {
               seq++;
               if (ri == code) {
                  target = seq;
                  break;
               }
            }
         }

         if (target < 0) {
            return;
         }
      }

      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(target);
      Net121.sendToServer(TurnBattleMod.STAT_RESET, buf);
      TurnBattleSounds.playClient(TurnBattleSounds.SND_GRAB, 0.8F, 0.7F);
      Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      return super.keyPressed(key, scan, mods);
   }
}
