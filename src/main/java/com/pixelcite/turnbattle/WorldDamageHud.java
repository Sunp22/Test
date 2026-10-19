package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class WorldDamageHud {
   private static final int COL_W = 90;
   private static final int COL_H = 19;
   private static final int COL_GAP = 3;
   private static final int PAD = 4;
   private static final int MARGIN = 4;
   private static final int SLIDE_TICKS = 10;
   private static final int HOLD_TICKS = 60;
   private static final Identifier DAMAGE_FONT = Identifier.of("turnbattle", "damage_numbers");
   private static final int DMG_NUM_TICKS = 40;
   private static int globalTick = 0;
   private static long lastTickRealTimeNanos = 0L;
   private static float lastRenderDelta = 1.0F;
   private static final WorldDamageHud.ColState selfCol = new WorldDamageHud.ColState(20.0F, 20.0F, "", null);
   private static final Map<UUID, WorldDamageHud.ColState> partyCol = new ConcurrentHashMap<>();
   private static boolean rHeld = false;
   private static int rSlideTick = 0;

   public static void onDamage(float hp, float maxHp, float dmg, String name, UUID uuid) {
      WorldDamageHud.ColState col = getOrCreateCol(hp, maxHp, name, uuid);
      col.holdCountdown = 60;
      col.dmgNumbers.add(new WorldDamageHud.DmgEntry(dmg, false, globalTick));
   }

   public static void onHeal(float hp, float maxHp, float amount, String name, UUID uuid) {
      WorldDamageHud.ColState col = getOrCreateCol(hp, maxHp, name, uuid);
      col.holdCountdown = 60;
      col.dmgNumbers.add(new WorldDamageHud.DmgEntry(amount, true, globalTick));
   }

   private static WorldDamageHud.ColState getOrCreateCol(float hp, float maxHp, String name, UUID uuid) {
      WorldDamageHud.ColState col;
      if (uuid == null) {
         selfCol.hp = hp;
         selfCol.maxHp = maxHp;
         selfCol.name = name;
         selfCol.soul = TurnBattleClientMod.clientSoul;
         col = selfCol;
      } else {
         col = partyCol.computeIfAbsent(uuid, k -> new WorldDamageHud.ColState(hp, maxHp, name, null));
         col.hp = hp;
         col.maxHp = maxHp;
         col.name = name;

         for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
            if (m.uuid().equals(uuid)) {
               col.soul = m.soul();
               break;
            }
         }
      }

      return col;
   }

   public static void setRHeld(boolean held) {
      rHeld = held;
   }

   public static void tick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      globalTick++;
      lastTickRealTimeNanos = System.nanoTime();
      Set current = new HashSet<>();

      for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
         current.add(m.uuid());
      }

      partyCol.keySet().retainAll(current);
      if (rHeld && mc.player != null) {
         selfCol.hp = mc.player.getHealth();
         selfCol.maxHp = mc.player.getMaxHealth();
         selfCol.soul = TurnBattleClientMod.clientSoul;
         selfCol.name = TurnBattleClientMod.clientVesselName.isEmpty() ? mc.player.getName().getString() : TurnBattleClientMod.clientVesselName;

         for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
            WorldDamageHud.ColState pc = partyCol.computeIfAbsent(m.uuid(), k -> new WorldDamageHud.ColState(m.hp(), m.maxHp(), m.vesselName(), m.soul()));
            pc.hp = m.hp();
            pc.maxHp = m.maxHp();
            pc.name = m.vesselName().isEmpty() ? m.name() : m.vesselName();
            pc.soul = m.soul();
         }
      }

      tickSlide(selfCol);

      for (WorldDamageHud.ColState c : partyCol.values()) {
         tickSlide(c);
      }

      if (rHeld) {
         rSlideTick = Math.min(10, rSlideTick + 2);
      } else {
         rSlideTick = Math.max(0, rSlideTick - 2);
      }

      tickNumbers(selfCol);

      for (WorldDamageHud.ColState c : partyCol.values()) {
         tickNumbers(c);
      }
   }

   private static void tickSlide(WorldDamageHud.ColState c) {
      if (c.holdCountdown > 0) {
         c.holdCountdown--;
      }

      boolean want = c.wantsVisible(rHeld);
      if (want) {
         if (c.slideTick < 10) {
            c.slideTick++;
         }
      } else if (c.slideTick > 0) {
         c.slideTick--;
      }
   }

   private static void tickNumbers(WorldDamageHud.ColState c) {
      c.dmgNumbers.removeIf(e -> globalTick - e.born >= 40);
   }

   public static void render(DrawContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (!mc.options.hudHidden) {
         if (lastTickRealTimeNanos == 0L) {
            lastTickRealTimeNanos = System.nanoTime();
         }

         float secondsSinceLastTick = (float)(System.nanoTime() - lastTickRealTimeNanos) / 1.0E9F;
         lastRenderDelta = Math.min(1.0F, Math.max(0.0F, secondsSinceLastTick / 0.05F));
         int screenW = mc.getWindow().getScaledWidth();
         int screenH = mc.getWindow().getScaledHeight();
         TextRenderer tr = mc.textRenderer;
         List cols = new ArrayList<>();
         if (selfCol.slideTick > 0) {
            cols.add(selfCol);
         }

         for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
            WorldDamageHud.ColState pc = partyCol.get(m.uuid());
            if (pc != null && pc.slideTick > 0) {
               cols.add(pc);
            }
         }

         for (int ci = 0; ci < cols.size(); ci++) {
            WorldDamageHud.ColState c = cols.get(ci);
            float ease = smoothstep((float)c.slideTick / 10.0F);
            int offY = (int)((1.0F - ease) * 33.0F);
            int colX = 4;
            int colY = screenH - 4 - 19 - ci * 22 + offY;
            SoulType soul = c.soul;
            int borderClr = soul != null ? 0xFF000000 | soul.getColor() : -14501052;
            ctx.fill(colX - 1, colY - 1, colX + 90 + 1, colY + 19 + 1, borderClr);
            ctx.fill(colX, colY, colX + 90, colY + 19, -301989888);
            int rowY = colY + 2;
            ctx.drawTextWithShadow(tr, c.name, colX + 4, rowY, -1);
            String hpStr = (int)c.hp + "/" + (int)c.maxHp;
            ctx.drawTextWithShadow(tr, hpStr, colX + 90 - 4 - tr.getWidth(hpStr), rowY, -1);
            int barX = colX + 4;
            int barW = 82;
            int barY = rowY + 9 + 2;
            int barH = 4;
            float frac = c.maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, c.hp / c.maxHp)) : 0.0F;
            int fill = (int)((float)barW * frac);
            ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, -15658735);
            ctx.fill(barX, barY, barX + barW, barY + barH, -14540254);
            if (fill > 0) {
               ctx.fill(barX, barY, barX + fill, barY + barH, borderClr);
               ctx.fill(barX, barY, barX + fill, barY + 2, lerpColor(borderClr, -1, 0.4F));
            }

            int dmgCX = colX + 45;
            int dmgBY = colY - 4;

            for (WorldDamageHud.DmgEntry e : c.dmgNumbers) {
               float prog = ((float)(globalTick - e.born) + lastRenderDelta) / 40.0F;
               if (!(prog >= 1.0F) && !(prog < 0.0F)) {
                  String txt = e.isHeal ? "+" + (int)Math.ceil((double)e.amount) : "-" + (int)Math.ceil((double)e.amount);
                  float landY = (float)dmgBY - 24.0F;
                  float drawY;
                  if (prog < 0.45F) {
                     float t = prog / 0.45F;
                     float arc = 4.0F * t * (1.0F - t);
                     drawY = (float)dmgBY + (landY - (float)dmgBY) * t - arc * 30.0F;
                  } else if (prog < 0.7F) {
                     drawY = landY;
                  } else {
                     float t = (prog - 0.7F) / 0.3F;
                     drawY = landY - t * 8.0F;
                  }

                  float scaleX = 1.0F;
                  float scaleY = 1.0F;
                  if (prog < 0.12F) {
                     float t = prog / 0.12F;
                     scaleX = 1.6F - t * 0.6F;
                     scaleY = 0.3F + t * 0.7F;
                  } else if (prog >= 0.7F) {
                     float t = (prog - 0.7F) / 0.3F;
                     scaleX = 1.0F;
                     scaleY = 1.0F + t * 0.5F;
                  }

                  float alpha01 = prog < 0.7F ? 1.0F : 1.0F - (prog - 0.7F) / 0.3F;
                  int alpha = (int)(alpha01 * 255.0F);
                  if (alpha > 0) {
                     int numColor = alpha << 24 | (e.isHeal ? 4521796 : 16777215);
                     float dmgScale = 2.0F;
                     Style dmgStyle = Style.EMPTY.withFont(DAMAGE_FONT);
                     int tw = (int)((float)tr.getWidth(Text.literal(txt).setStyle(dmgStyle)) * dmgScale);
                     float fontH = 9.0F * dmgScale;
                     MatrixStack matrices = ctx.getMatrices();
                     matrices.push();
                     matrices.translate((float)dmgCX, drawY + fontH * 0.5F, 0.0F);
                     matrices.scale(scaleX * dmgScale, scaleY * dmgScale, 1.0F);
                     drawDmgText(ctx, tr, txt, -tw / 2, (int)(-fontH * 0.5F), numColor);
                     matrices.pop();
                  }
               }
            }
         }

         if (rSlideTick > 0) {
            float ease = smoothstep((float)rSlideTick / 10.0F);
            int epW = 90;
            int epH = 30;
            int offY = (int)((1.0F - ease) * (float)(epH + 4 + 10));
            int panX = screenW - 4 - epW;
            int panY = screenH - 4 - epH + offY;
            SoulType mySoul = TurnBattleClientMod.clientSoul;
            int borderClr = mySoul != null ? 0xFF000000 | mySoul.getColor() : -14501052;
            ctx.fill(panX - 1, panY - 1, panX + epW + 1, panY + epH + 1, borderClr);
            ctx.fill(panX, panY, panX + epW, panY + epH, -301989888);
            int lv = TurnBattleClientMod.clientPlayerLevel;
            int exp = TurnBattleClientMod.clientExp;
            int need = Math.max(1, TurnBattleClientMod.clientExpNeeded);
            ctx.drawTextWithShadow(tr, "Lv. " + lv, panX + 4, panY + 3, -188);
            String expStr = exp + "/" + need;
            ctx.drawTextWithShadow(tr, expStr, panX + epW - 4 - tr.getWidth(expStr), panY + 3, -3355444);
            int barX = panX + 4;
            int barW = epW - 8;
            int barY = panY + 3 + 9 + 3;
            int barH = 4;
            int fill = (int)((float)barW * Math.max(0.0F, Math.min(1.0F, (float)exp / (float)need)));
            ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, -15658735);
            ctx.fill(barX, barY, barX + barW, barY + barH, -14540254);
            if (fill > 0) {
               ctx.fill(barX, barY, barX + fill, barY + barH, -4540);
               ctx.fill(barX, barY, barX + fill, barY + 2, lerpColor(-4540, -1, 0.4F));
            }

            ctx.drawTextWithShadow(tr, "EXP", panX + 4, barY + barH + 3, -7829368);
         }
      }
   }

   private static void drawDmgText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color) {
      Style style = Style.EMPTY.withFont(DAMAGE_FONT);
      Text t = Text.literal(text).setStyle(style);
      int outline = color & 0xFF000000 | 0;
      ctx.drawText(tr, Text.literal(text).setStyle(style), x - 1, y, outline, false);
      ctx.drawText(tr, Text.literal(text).setStyle(style), x + 1, y, outline, false);
      ctx.drawText(tr, Text.literal(text).setStyle(style), x, y - 1, outline, false);
      ctx.drawText(tr, Text.literal(text).setStyle(style), x, y + 1, outline, false);
      ctx.drawText(tr, t, x, y, color, false);
   }

   private static float smoothstep(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   private static int lerpColor(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb2 = b & 0xFF;
      return 0xFF000000
         | (int)((float)ar + (float)(br - ar) * t) << 16
         | (int)((float)ag + (float)(bg - ag) * t) << 8
         | (int)((float)ab + (float)(bb2 - ab) * t);
   }

   private static class ColState {
      float hp;
      float maxHp;
      String name = "";
      SoulType soul;
      int holdCountdown = 0;
      int slideTick = 0;
      final List<WorldDamageHud.DmgEntry> dmgNumbers = new ArrayList<>();

      ColState(float hp, float maxHp, String name, SoulType soul) {
         this.hp = hp;
         this.maxHp = maxHp;
         this.name = name;
         this.soul = soul;
      }

      boolean wantsVisible(boolean rHeld) {
         return rHeld || this.holdCountdown > 0;
      }
   }

   private static class DmgEntry {
      float amount;
      boolean isHeal;
      int born;

      DmgEntry(float a, boolean heal, int born) {
         this.amount = a;
         this.isHeal = heal;
         this.born = born;
      }
   }
}
