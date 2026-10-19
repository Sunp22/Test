package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class MobFates {
   public static final int KILL = 0;
   public static final int SPARE = 1;
   public static final int ICE = 2;
   private static final long[] DURATION = new long[]{950L, 1000L, 1650L};
   private static final long ICE_HOLD_MS = 8000L;
   private static final long ICE_MELT_MS = 1200L;
   private static final Map<Integer, MobFates.Fate> fates = new HashMap<>();
   private static final Map<Integer, Long> finished = new HashMap<>();
   private static boolean drawingGhost;
   private static final float ICE_R = 0.58F;
   private static final float ICE_G = 0.86F;
   private static final float ICE_B = 1.0F;
   private static final float ICE_WHITE = 0.55F;

   private MobFates() {
   }

   public static void register() {
      Net121.registerClientReceiver(MobFateNet.CHANNEL, (client, handler, buf, sender) -> {
         int id = buf.readInt();
         int kind = buf.readByte();
         client.execute(() -> onFate(id, kind));
      });
   }

   private static long now() {
      return System.currentTimeMillis();
   }

   private static LivingEntity find(int id) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(id) : null;
      return e instanceof LivingEntity ? (LivingEntity)e : BattleEntityRenderer.cachedEnemy(id);
   }

   static void onFate(int id, int kind) {
      MobFates.Fate f = fates.get(id);
      if (f == null || f.auto) {
         if (f != null || !finished.containsKey(id)) {
            LivingEntity e = f != null ? f.e : find(id);
            if (e != null) {
               if (f != null || MinecraftClient.getInstance().currentScreen instanceof BattleScreen || LiveCombatClient.nearMe(e)) {
                  if (f == null || f.kind != kind) {
                     f = start(id, e, kind);
                  }

                  f.auto = false;
                  if (!f.sounded) {
                     sound(f);
                  }
               }
            }
         }
      }
   }

   static MobFates.Fate autoKill(int id, LivingEntity e) {
      MobFates.Fate f = fates.get(id);
      if (f != null) {
         return f;
      } else if (finished.containsKey(id)) {
         return null;
      } else {
         f = start(id, e, 0);
         f.auto = true;
         return f;
      }
   }

   private static MobFates.Fate start(int id, LivingEntity e, int kind) {
      MobFates.Fate f = new MobFates.Fate();
      f.id = id;
      f.e = e;
      f.kind = MathHelper.clamp(kind, 0, 2);
      f.start = now();
      f.pos = e.getPos();
      f.yaw = e.bodyYaw;
      f.seed = e.getUuid().getLeastSignificantBits() ^ (long)id;
      f.bits = makeBits(f);
      fates.put(id, f);
      return f;
   }

   private static void sound(MobFates.Fate f) {
      f.sounded = true;
      MinecraftClient mc = MinecraftClient.getInstance();
      boolean inBattle = mc.currentScreen instanceof BattleScreen;
      SoundEvent ev = f.kind == 1 ? TurnBattleSounds.SPARE : (f.kind == 2 ? TurnBattleSounds.PETRIFY : TurnBattleSounds.DEATHNOISE);
      if (!inBattle && mc.player != null) {
         mc.world.playSound(f.pos.x, f.pos.y, f.pos.z, ev, SoundCategory.HOSTILE, 1.2F, 1.0F, false);
      } else {
         mc.getSoundManager().play(PositionedSoundInstance.master(ev, 1.0F, 1.0F));
      }
   }

   private static float progress(MobFates.Fate f) {
      return (float)(now() - f.start) / (float)DURATION[f.kind];
   }

   public static void tick() {
      long t = now();
      Iterator it = fates.values().iterator();

      while (it.hasNext()) {
         MobFates.Fate f = it.next();
         if (f.auto && !f.sounded && t - f.start > 180L) {
            sound(f);
         }

         long age = t - f.start;
         if (f.kind != 2 || !(MinecraftClient.getInstance().currentScreen instanceof BattleScreen) || OverhaulView.isActive()) {
            long over = f.kind == 2 ? 9200L : DURATION[f.kind];
            if (age > over + 4000L && (f.e == null || f.e.isRemoved() || age > over + 20000L)) {
               finished.put(f.id, t);
               it.remove();
            }
         }
      }

      if (!(MinecraftClient.getInstance().currentScreen instanceof BattleScreen)) {
         finished.values().removeIf(v -> t - v > 300000L);
      }
   }

   public static void clearAll() {
      fates.clear();
      finished.clear();
   }

   public static boolean hideInWorld(Entity e) {
      if (!drawingGhost && e != null) {
         MobFates.Fate f = fates.get(e.getId());
         return f != null && f.e == e;
      } else {
         return false;
      }
   }

   static MobFates.Fate get(int id) {
      return fates.get(id);
   }

   static boolean playing(MobFates.Fate f) {
      return f != null && progress(f) < 1.05F;
   }

   static void drop(int id) {
      fates.remove(id);
   }

   private static float[][] makeBits(MobFates.Fate f) {
      Random r = new Random(f.seed);
      if (f.kind == 0) {
         float[][] b = new float[90][];

         for (int i = 0; i < b.length; i++) {
            b[i] = new float[]{r.nextFloat(), r.nextFloat(), 0.7F + r.nextFloat() * 0.6F, (r.nextFloat() - 0.5F) * 0.6F, 0.8F + r.nextFloat() * 0.5F};
         }

         return b;
      } else if (f.kind == 1) {
         float[][] b = new float[16][];

         for (int i = 0; i < b.length; i++) {
            b[i] = new float[]{
               0.1F + r.nextFloat() * 0.8F,
               0.05F + r.nextFloat() * 0.85F,
               r.nextFloat(),
               (float)((double)r.nextFloat() * Math.PI * 2.0),
               0.4F + r.nextFloat() * 0.9F,
               r.nextFloat()
            };
         }

         return b;
      } else if (f.kind != 2) {
         return new float[0][];
      } else {
         float[][] b = new float[14][];

         for (int i = 0; i < b.length; i++) {
            b[i] = new float[]{0.1F + r.nextFloat() * 0.8F, r.nextFloat(), r.nextFloat(), 0.6F + r.nextFloat() * 0.7F};
         }

         return b;
      }
   }

   private static void drawSquares(DrawContext ctx, MobFates.Fate f, float p, float left, float top, float right, float bottom, float px) {
      float w = right - left;
      float h = bottom - top;
      float size = Math.max(2.0F, h / 14.0F);
      float travel = px * 1.4F;

      for (float[] b : f.bits) {
         float spawn = 0.25F + b[0] * 0.6F;
         float age = (p - spawn) / (0.32F * b[4]);
         if (!(age < 0.0F) && !(age >= 1.0F)) {
            float e = 1.0F - (1.0F - age) * (1.0F - age);
            float x = left + b[0] * w + e * travel * b[4];
            float y = top + b[1] * h + b[3] * size * 3.0F * e;
            float s = size * b[2] * (1.0F - age * 0.35F);
            int a = (int)((1.0F - age) * 255.0F);
            int rgb = age < 0.35F ? 16722474 : 12589080;
            ctx.fill((int)(x - s / 2.0F), (int)(y - s / 2.0F), (int)(x + s / 2.0F), (int)(y + s / 2.0F), a << 24 | rgb);
         }
      }
   }

   private static float freezeLevel(float p) {
      float q = MathHelper.clamp(p / 0.97F, 0.0F, 1.0F);
      return q * q * (3.0F - 2.0F * q);
   }

   private static void drawFrost(DrawContext ctx, MobFates.Fate f, float p, float left, float top, float right, float bottom, float px, float alpha) {
      float fz = freezeLevel(p);
      float w = right - left;
      float h = bottom - top;
      float lineY = bottom - h * fz;
      if (fz > 0.0F && fz < 1.0F) {
         int a = (int)(alpha * 230.0F);
         ctx.fill((int)left, (int)lineY - 1, (int)right, (int)lineY + 1, a << 24 | 15399935);
         Random r = new Random(f.seed + (long)(p * 30.0F));

         for (int i = 0; i < 6; i++) {
            float x = left + r.nextFloat() * w;
            float y = lineY + (r.nextFloat() - 0.6F) * Math.max(3.0F, px * 0.15F);
            CloneAnimator.star4(ctx, x, y, Math.max(1.5F, px * 0.07F * (0.5F + r.nextFloat())), (int)(alpha * 255.0F) << 24 | 16777215);
         }
      }

      float time = (float)(now() % 100000L) / 1000.0F;

      for (float[] b : f.bits) {
         float y = top + b[1] * h;
         if (!(y < lineY)) {
            float tw = MathHelper.sin(time * 2.3F + b[2] * 12.0F);
            if (!(tw < 0.55F)) {
               float k = (tw - 0.55F) / 0.45F;
               CloneAnimator.star4(ctx, left + b[0] * w, y, Math.max(1.2F, px * 0.06F * b[3]) * k, (int)(alpha * k * 230.0F) << 24 | 16777215);
            }
         }
      }
   }

   static boolean renderGui(DrawContext ctx, MobFates.Fate f, LivingEntity mob, int cx, int cy, float scale, float yawDeg) {
      float p = progress(f);
      if (p >= 1.0F && f.kind != 2) {
         return false;
      } else {
         p = Math.min(p, 1.0F);
         MinecraftClient mc = MinecraftClient.getInstance();
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         float h = mob.getHeight() * scale;
         float feetY = (float)cy - 1.25F * scale + h / 2.0F;
         float top = feetY - h;
         float half = (mob.getWidth() * 0.75F + 0.25F) * scale;
         float left = (float)cx - half;
         float right = (float)cx + half;
         switch (f.kind) {
            case 0:
               float red = MathHelper.clamp(p / 0.25F, 0.0F, 1.0F);
               float d = MathHelper.clamp((p - 0.25F) / 0.6F, 0.0F, 1.0F);
               int slide = (int)(d * 0.35F * scale);
               if (d < 1.0F) {
                  int cut = (int)(left + (right - left) * d) + slide;
                  int cutS = (int)ctx.getMatrices().peek().getPositionMatrix().transformPosition((float)cut, 0.0F, 0.0F, new Vector3f()).x;
                  ctx.enableScissor(MathHelper.clamp(cutS, 0, sw), 0, sw, sh);
                  ghost(ctx, mob, cx + slide, cy, scale, yawDeg, 1.0F, 1.0F - 0.85F * red, 1.0F - 0.85F * red, 1.0F, 0.0F, red > 0.25F);
                  ctx.disableScissor();
               }

               drawSquares(ctx, f, p, left + (float)slide, top, right + (float)slide, feetY, scale);
               break;
            case 1:
               float white = MathHelper.clamp(p / 0.15F, 0.0F, 1.0F);
               float a = spareAlpha(p);
               float dist = 1.8F * scale;

               for (int k = 2; k >= 1; k--) {
                  float s = spareSlide(p - 0.07F * (float)k);
                  if (!(s <= 0.001F)) {
                     ghost(ctx, mob, cx + (int)(s * dist), cy, scale, yawDeg, 1.0F, 1.0F, 1.0F, a * (k == 1 ? 0.5F : 0.28F), 1.0F, false);
                  }
               }

               if (a > 0.003F) {
                  ghost(ctx, mob, cx + (int)(spareSlide(p) * dist), cy, scale, yawDeg, 1.0F, 1.0F, 1.0F, a, white, false);
               }

               drawSparkles(ctx, f, p, left, top, right, feetY, scale, spareSlide(p) * dist);
               break;
            default:
               float fz = freezeLevel(p);
               float lineY = feetY - h * fz;
               int lineS = (int)ctx.getMatrices().peek().getPositionMatrix().transformPosition((float)cx, lineY, 0.0F, new Vector3f()).y;
               lineS = MathHelper.clamp(lineS, 0, sh);
               if (fz < 1.0F && lineS > 0) {
                  ctx.enableScissor(0, 0, sw, lineS);
                  ghost(ctx, mob, cx, cy, scale, yawDeg, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
                  ctx.disableScissor();
               }

               if (fz > 0.0F && lineS < sh) {
                  ctx.enableScissor(0, lineS, sw, sh);
                  ghost(ctx, mob, cx, cy, scale, yawDeg, 0.58F, 0.86F, 1.0F, 1.0F, 0.55F, false);
                  ctx.disableScissor();
               }

               drawFrost(ctx, f, p, left, top, right, feetY, scale, 1.0F);
         }

         return true;
      }
   }

   private static float spareSlide(float p) {
      float q = MathHelper.clamp((p - 0.1F) / 0.88F, 0.0F, 1.0F);
      return q * q * (0.6F + 0.4F * q);
   }

   private static float spareAlpha(float p) {
      return 1.0F - MathHelper.clamp((p - 0.38F) / 0.42F, 0.0F, 1.0F);
   }

   private static void drawSparkles(DrawContext ctx, MobFates.Fate f, float p, float left, float top, float right, float bottom, float px, float slide) {
      float w = right - left;
      float h = bottom - top;

      for (float[] b : f.bits) {
         float born = 0.02F + b[2] * 0.3F;
         float life = 0.42F + b[5] * 0.2F;
         float q = (p - born) / life;
         if (!(q < 0.0F) && !(q >= 1.0F)) {
            float e = 1.0F - (1.0F - q) * (1.0F - q);
            float x = left + b[0] * w + (float)Math.cos((double)b[3]) * b[4] * px * 0.7F * e + slide * 0.6F;
            float y = top + b[1] * h + (float)Math.sin((double)b[3]) * b[4] * px * 0.7F * e - q * px * 0.15F;
            float twinkle = 0.6F + 0.4F * MathHelper.sin(p * 40.0F + b[3] * 3.0F);
            float r = Math.max(1.5F, px * 0.11F * (0.6F + b[5] * 0.6F)) * (q < 0.2F ? q / 0.2F : 1.0F - (q - 0.2F) * 0.6F) * twinkle;
            int a = (int)((q < 0.6F ? 1.0F : 1.0F - (q - 0.6F) / 0.4F) * 255.0F);
            CloneAnimator.star4(ctx, x, y, r, a << 24 | 16777215);
         }
      }
   }

   private static void ghost(
      DrawContext ctx, LivingEntity mob, int cx, int cy, float scale, float yawDeg, float r, float g, float b, float a, float white, boolean red
   ) {
      GhostRender.set(r, g, b, a, white, red);

      try {
         BattleEntityRenderer.renderEntity(ctx, mob, cx, cy, scale, yawDeg, BattleEntityRenderer.BattleAnim.IDLE, null);
      } finally {
         GhostRender.clear();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static boolean worldShown() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return !(mc.currentScreen instanceof BattleScreen) || OverhaulView.isActive();
   }

   public static void renderWorld(WorldRenderContext ctx) {
      if (!fates.isEmpty() && worldShown()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            float td = mc.getRenderTickCounter().getTickDelta(false);
            Vec3d cam = ctx.camera().getPos();
            float camYaw = ctx.camera().getYaw() * (float) (Math.PI / 180.0);
            Vec3d right = new Vec3d((double)(-MathHelper.cos(camYaw)), 0.0, (double)(-MathHelper.sin(camYaw)));

            for (MobFates.Fate f : fates.values()) {
               if (f.e != null && f.e.getWorld() == mc.world) {
                  float p = progress(f);
                  if (!(p >= 1.0F) || f.kind == 2) {
                     switch (f.kind) {
                        case 0:
                           float red = MathHelper.clamp(p / 0.25F, 0.0F, 1.0F);
                           float d = MathHelper.clamp((p - 0.25F) / 0.6F, 0.0F, 1.0F);
                           if (d >= 1.0F) {
                              break;
                           }

                           Vec3d at = f.pos.add(right.multiply((double)d * 0.35));
                           float[] rect = screenRect(f.e, at, right);
                           boolean scissor = rect != null && d > 0.0F;
                           mc.getBufferBuilders().getEntityVertexConsumers().draw();
                           if (scissor) {
                              double sfx = mc.getWindow().getScaleFactor();
                              int cut = (int)((double)(rect[0] + (rect[2] - rect[0]) * d) * sfx);
                              int fwx = mc.getWindow().getFramebufferWidth();
                              int fhx = mc.getWindow().getFramebufferHeight();
                              RenderSystem.enableScissor(MathHelper.clamp(cut, 0, fwx), 0, Math.max(0, fwx - cut), fhx);
                           }

                           drawGhost(mc, f, at, cam, td, 1.0F, 1.0F - 0.85F * red, 1.0F - 0.85F * red, 1.0F, 0.0F, red > 0.25F);
                           if (scissor) {
                              RenderSystem.disableScissor();
                           }
                           break;
                        case 1:
                           float white = MathHelper.clamp(p / 0.15F, 0.0F, 1.0F);
                           float a = spareAlpha(p);

                           for (int k = 2; k >= 1; k--) {
                              float s = spareSlide(p - 0.07F * (float)k);
                              if (!(s <= 0.001F)) {
                                 drawGhost(
                                    mc, f, f.pos.add(right.multiply((double)s * 1.8)), cam, td, 1.0F, 1.0F, 1.0F, a * (k == 1 ? 0.5F : 0.28F), 1.0F, false
                                 );
                              }
                           }

                           if (a > 0.003F) {
                              drawGhost(mc, f, f.pos.add(right.multiply((double)spareSlide(p) * 1.8)), cam, td, 1.0F, 1.0F, 1.0F, a, white, false);
                           }
                           break;
                        default:
                           long age = now() - f.start;
                           float melt = MathHelper.clamp((float)(age - 8000L) / 1200.0F, 0.0F, 1.0F);
                           if (!(melt >= 1.0F)) {
                              float fz = freezeLevel(Math.min(p, 1.0F));
                              float[] rect = screenRect(f.e, f.pos, right);
                              mc.getBufferBuilders().getEntityVertexConsumers().draw();
                              if (rect != null && !(fz >= 1.0F)) {
                                 double sf = mc.getWindow().getScaleFactor();
                                 int fw = mc.getWindow().getFramebufferWidth();
                                 int fh = mc.getWindow().getFramebufferHeight();
                                 float lineGui = rect[3] + (rect[1] - rect[3]) * fz;
                                 int yLine = MathHelper.clamp(fh - (int)((double)lineGui * sf), 0, fh);
                                 if (yLine < fh) {
                                    RenderSystem.enableScissor(0, yLine, fw, fh - yLine);
                                    drawGhost(mc, f, f.pos, cam, td, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
                                    RenderSystem.disableScissor();
                                 }

                                 if (yLine > 0) {
                                    RenderSystem.enableScissor(0, 0, fw, yLine);
                                    drawGhost(mc, f, f.pos, cam, td, 0.58F, 0.86F, 1.0F, 1.0F, 0.55F, false);
                                    RenderSystem.disableScissor();
                                 }
                              } else {
                                 drawGhost(mc, f, f.pos, cam, td, 0.58F, 0.86F, 1.0F, 1.0F - melt, 0.55F, false);
                              }
                           }
                     }
                  }
               }
            }
         }
      }
   }

   private static void drawGhost(
      MinecraftClient mc, MobFates.Fate f, Vec3d at, Vec3d cam, float td, float r, float g, float b, float a, float white, boolean red
   ) {
      Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
      imm.draw();
      GhostRender.set(r, g, b, a, white, red);
      RenderSystem.setShaderColor(r, g, b, a);
      drawingGhost = true;

      try {
         int light = WorldRenderer.getLightmapCoordinates(mc.world, BlockPos.ofFloored(at.add(0.0, (double)f.e.getHeight() * 0.5, 0.0)));
         mc.getEntityRenderDispatcher().render(f.e, at.x - cam.x, at.y - cam.y, at.z - cam.z, f.yaw, td, new MatrixStack(), imm, light);
         imm.draw();
      } catch (Throwable var16) {
      } finally {
         drawingGhost = false;
         GhostRender.clear();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static float[] screenRect(LivingEntity e, Vec3d at, Vec3d right) {
      double half = (double)e.getWidth() * 0.6 + 0.15;
      float[] a = OverhaulView.project(at.add(right.multiply(-half)));
      float[] b = OverhaulView.project(at.add(right.multiply(half)));
      float[] top = OverhaulView.project(at.add(0.0, (double)e.getHeight(), 0.0));
      float[] feet = OverhaulView.project(at);
      return a != null && b != null && top != null && feet != null ? new float[]{Math.min(a[0], b[0]), top[1], Math.max(a[0], b[0]), feet[1], feet[2]} : null;
   }

   public static void renderHud(DrawContext ctx) {
      if (!fates.isEmpty() && worldShown()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null && mc.gameRenderer != null) {
            float camYaw = mc.gameRenderer.getCamera().getYaw() * (float) (Math.PI / 180.0);
            Vec3d right = new Vec3d((double)(-MathHelper.cos(camYaw)), 0.0, (double)(-MathHelper.sin(camYaw)));

            for (MobFates.Fate f : fates.values()) {
               if (f.e != null && f.e.getWorld() == mc.world) {
                  float p = progress(f);
                  if (!(p >= 1.0F) || f.kind == 2) {
                     if (f.kind == 1) {
                        float[] r = screenRect(f.e, f.pos, right);
                        float[] moved = screenRect(f.e, f.pos.add(right.multiply((double)spareSlide(p) * 1.8)), right);
                        if (r != null && moved != null) {
                           drawSparkles(ctx, f, p, r[0], r[1], r[2], r[3], r[4], moved[0] - r[0]);
                        }
                     } else if (f.kind == 0) {
                        float d = MathHelper.clamp((p - 0.25F) / 0.6F, 0.0F, 1.0F);
                        float[] r = screenRect(f.e, f.pos, right);
                        if (r != null) {
                           float slide = d * 0.35F * r[4];
                           drawSquares(ctx, f, p, r[0] + slide, r[1], r[2] + slide, r[3], r[4]);
                        }
                     } else {
                        float[] r = screenRect(f.e, f.pos, right);
                        if (r != null) {
                           float melt = MathHelper.clamp((float)(now() - f.start - 8000L) / 1200.0F, 0.0F, 1.0F);
                           if (melt < 1.0F) {
                              drawFrost(ctx, f, Math.min(p, 1.0F), r[0], r[1], r[2], r[3], r[4], 1.0F - melt);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static final class Fate {
      int id;
      int kind;
      LivingEntity e;
      long start;
      boolean auto;
      boolean sounded;
      Vec3d pos;
      float yaw;
      long seed;
      float[][] bits;
   }
}
