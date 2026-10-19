package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class MobPlateRenderer {
   private static final Pattern TAG = Pattern.compile("^(?:\\[(.+?) BOSS\\] )?Lv\\.(\\d+) (.+)$");
   private static final Identifier DAMAGE_FONT = Identifier.of("turnbattle", "damage_numbers");
   private static final double RANGE = 20.0;
   private static final int MAX_PLATES = 24;
   private static volatile List<MobPlateRenderer.Plate> pending = List.of();
   private static final Map<Integer, Float> fade = new HashMap<>();

   private MobPlateRenderer() {
   }

   static MobPlateRenderer.Tag tagOf(Entity e) {
      if (e instanceof MobEntity && e.hasCustomName()) {
         Text t = e.getCustomName();
         if (t == null) {
            return null;
         } else {
            Matcher m = TAG.matcher(t.getString());
            if (!m.matches()) {
               return null;
            } else {
               int lv;
               try {
                  lv = Integer.parseInt(m.group(2));
               } catch (NumberFormatException var6) {
                  return null;
               }

               int bossColor = 16755200;
               if (m.group(1) != null) {
                  int[] first = new int[]{-1};
                  t.visit((style, str) -> {
                     if (first[0] < 0 && !str.isEmpty()) {
                        TextColor c = style.getColor();
                        first[0] = c != null ? c.getRgb() : 16755200;
                     }

                     return Optional.empty();
                  }, Style.EMPTY);
                  if (first[0] >= 0) {
                     bossColor = first[0];
                  }
               }

               return new MobPlateRenderer.Tag(m.group(3), lv, m.group(1), bossColor);
            }
         }
      } else {
         return null;
      }
   }

   public static boolean replacesLabel(Entity e) {
      return tagOf(e) != null;
   }

   private static int levelColor(int lv) {
      if (lv <= 5) {
         return 5635925;
      } else if (lv <= 10) {
         return 16777045;
      } else if (lv <= 17) {
         return 16755200;
      } else {
         return lv <= 24 ? 16733525 : 12595248;
      }
   }

   private static boolean hidden(MinecraftClient mc) {
      return mc.currentScreen != null
         || OverhaulView.isActive()
         || LiveCombatClient.lockedOn()
         || LiveCombatClient.menuOpen()
         || DarkFountainSealEffect.cameraActive();
   }

   public static void compute(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null && mc.player != null) {
         boolean hide = hidden(mc);
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         Vec3d cam = ctx.camera().getPos();
         float td = ctx.tickCounter().getTickDelta(true);
         List near = new ArrayList<>();

         for (Entity e : mc.world.getEntities()) {
            if (e instanceof MobEntity) {
               MobEntity mob = (MobEntity)e;
               if (mob.isAlive() && !mob.isInvisible() && mob.isCustomNameVisible() && !(mob.squaredDistanceTo(cam) > 400.0)) {
                  near.add(mob);
               }
            }
         }

         near.sort(Comparator.comparingDouble(m -> m.squaredDistanceTo(cam)));
         if (near.size() > 24) {
            near = near.subList(0, 24);
         }

         Set seen = new HashSet<>();
         List next = new ArrayList<>();

         for (MobEntity mob : near) {
            MobPlateRenderer.Tag tag = tagOf(mob);
            if (tag != null) {
               int id = mob.getId();
               seen.add(id);
               double x = MathHelper.lerp((double)td, mob.prevX, mob.getX());
               double y = MathHelper.lerp((double)td, mob.prevY, mob.getY()) + (double)mob.getHeight() + 0.35;
               double z = MathHelper.lerp((double)td, mob.prevZ, mob.getZ());
               Vec3d at = new Vec3d(x, y, z);
               double dist = at.distanceTo(cam);
               boolean visible = !hide;
               if (visible) {
                  BlockHitResult hit = mc.world.raycast(new RaycastContext(cam, at.add(0.0, -0.2, 0.0), ShapeType.VISUAL, FluidHandling.NONE, mc.player));
                  if (hit != null && hit.getType() == Type.BLOCK) {
                     visible = false;
                  }
               }

               float want = visible ? (float)MathHelper.clamp((20.0 - dist) / 6.0, 0.0, 1.0) : 0.0F;
               float a = fade.getOrDefault(id, 0.0F);
               a += (want - a) * (want > a ? 0.18F : 0.3F);
               fade.put(id, a);
               if (!(a <= 0.02F)) {
                  WorldToScreen.ScreenPos pos = WorldToScreen.project(ctx, x - cam.x, y - cam.y, z - cam.z, sw, sh);
                  if (pos != null) {
                     float scale = (float)MathHelper.clamp(6.5 / Math.max(dist, 1.0), 0.5, 1.15);
                     float hp = mob.getMaxHealth() > 0.0F ? MathHelper.clamp(mob.getHealth() / mob.getMaxHealth(), 0.0F, 1.0F) : 1.0F;
                     next.add(new MobPlateRenderer.Plate((float)pos.x(), (float)pos.y(), scale, a, dist, tag, hp));
                  }
               }
            }
         }

         fade.keySet().retainAll(seen);
         next.sort((p1, p2) -> Double.compare(p2.dist(), p1.dist()));
         pending = next;
      } else {
         pending = List.of();
      }
   }

   public static void renderHud(DrawContext ctx) {
      List snap = pending;
      if (!snap.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (!hidden(mc)) {
            TextRenderer tr = mc.textRenderer;

            for (MobPlateRenderer.Plate p : snap) {
               drawPlate(ctx, tr, p);
            }
         }
      }
   }

   private static int argb(int rgb, float a) {
      return Math.round(MathHelper.clamp(a, 0.0F, 1.0F) * 255.0F) << 24 | rgb & 16777215;
   }

   private static void drawPlate(DrawContext ctx, TextRenderer tr, MobPlateRenderer.Plate p) {
      MobPlateRenderer.Tag t = p.tag();
      float a = p.alpha();
      Text lv = Text.literal("LV " + t.level()).setStyle(Style.EMPTY.withFont(DAMAGE_FONT));
      String name = t.name().length() > 22 ? t.name().substring(0, 21) + "\u2026" : t.name();
      int lvW = tr.getWidth(lv) + 6;
      int nameW = tr.getWidth(name);
      int w = lvW + 4 + nameW + 8;
      int h = 13 + (p.hpFrac() < 0.999F ? 3 : 0);
      boolean boss = t.bossTier() != null;
      int edge = boss ? t.bossColor() : 16777215;
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(p.x(), p.y(), 0.0F);
      m.scale(p.scale(), p.scale(), 1.0F);
      int x0 = -w / 2;
      int y0 = -h;
      if (boss) {
         String tier = t.bossTier() + " BOSS";
         m.push();
         m.translate(0.0F, (float)(y0 - 7), 0.0F);
         m.scale(0.7F, 0.7F, 1.0F);
         ctx.drawText(tr, tier, -tr.getWidth(tier) / 2, 0, argb(t.bossColor(), a), true);
         m.pop();
      }

      ctx.fill(x0 + 1, y0, x0 + w - 1, y0 + h, argb(0, a * 0.82F));
      ctx.fill(x0 + 1, y0 - 1, x0 + w - 1, y0, argb(edge, a));
      ctx.fill(x0 + 1, y0 + h, x0 + w - 1, y0 + h + 1, argb(edge, a));
      ctx.fill(x0, y0, x0 + 1, y0 + h, argb(edge, a));
      ctx.fill(x0 + w - 1, y0, x0 + w, y0 + h, argb(edge, a));
      int lc = levelColor(t.level());
      ctx.fill(x0 + 2, y0 + 2, x0 + 2 + lvW, y0 + 11, argb(lc, a * 0.25F));
      ctx.fill(x0 + 2, y0 + 2, x0 + 2 + lvW, y0 + 3, argb(lc, a));
      ctx.drawText(tr, lv, x0 + 5, y0 + 3, argb(lc, a), true);
      ctx.drawText(tr, name, x0 + 2 + lvW + 4, y0 + 3, argb(16777215, a), true);
      if (p.hpFrac() < 0.999F) {
         int bw = w - 4;
         int by = y0 + h - 2;
         ctx.fill(x0 + 2, by, x0 + 2 + bw, by + 1, argb(5900810, a));
         int col = p.hpFrac() > 0.5F ? 16765470 : (p.hpFrac() > 0.25F ? 16747038 : 16724016);
         ctx.fill(x0 + 2, by, x0 + 2 + Math.round((float)bw * p.hpFrac()), by + 1, argb(col, a));
      }

      ctx.fill(-1, y0 + h + 1, 2, y0 + h + 2, argb(edge, a));
      ctx.fill(0, y0 + h + 2, 1, y0 + h + 3, argb(edge, a));
      m.pop();
   }

   private static record Plate(float x, float y, float scale, float alpha, double dist, MobPlateRenderer.Tag tag, float hpFrac) {
   }

   static record Tag(String name, int level, String bossTier, int bossColor) {
   }
}
