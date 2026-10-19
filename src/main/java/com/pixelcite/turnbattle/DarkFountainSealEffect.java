package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@Environment(EnvType.CLIENT)
public class DarkFountainSealEffect {
   private static final float WAIT_SEC = 1.0F;
   private static final float RISE_TO_FLASH_SEC = 10.0F;
   private static final float FLASH_SEC = 3.0F;
   private static final float FLASH_START_SEC = 11.0F;
   private static final float SEAL_SEC = 14.0F;
   private static final float HOLD_SEC = 0.5F;
   private static final float FADE_OUT_SEC = 1.0F;
   private static final float TOTAL_SEC = 15.5F;
   private static final float EMERGE_FADE_SEC = 0.4F;
   private static final float RISE_SPEED = 0.5F;
   private static boolean active = false;
   private static long startNanos = 0L;
   private static List<DarkFountainSealEffect.DepartingSoul> souls = List.of();
   private static final float FADE_IN_SEC = 1.2F;
   private static final float SOUL_SIZE = 0.5F;
   private static final Map<SoulType, ItemStack> STACKS = new EnumMap<>(SoulType.class);
   private static double fountainCx = Double.NaN;
   private static double fountainCz = Double.NaN;
   private static Matrix4f frameProj;
   private static Matrix4f frameView;
   private static Vec3d frameCam;
   private static float frameCamYaw;
   private static Vec3d camFrom = null;
   private static float camYaw0 = 0.0F;
   private static Vec3d camTarget;
   private static Vec3d camPos;
   private static float camYaw;
   private static float camPitch;

   public static boolean isActive() {
      return active;
   }

   public static void start(List<DarkFountainSealEffect.DepartingSoul> newSouls, double fountainX, double fountainZ) {
      fountainCx = fountainX;
      fountainCz = fountainZ;
      souls = newSouls;
      camFrom = null;
      startNanos = System.nanoTime();
      active = true;
   }

   private static float elapsedSeconds() {
      return (float)(System.nanoTime() - startNanos) / 1.0E9F;
   }

   private static float clamp01(float t) {
      return Math.max(0.0F, Math.min(1.0F, t));
   }

   private static float smoothstep(float t) {
      t = clamp01(t);
      return t * t * (3.0F - 2.0F * t);
   }

   private static ItemStack stackOf(SoulType soul) {
      SoulType st = soul != null ? soul : SoulType.DETERMINATION;
      return STACKS.computeIfAbsent(st, k -> {
         SoulItem item = TurnBattleMod.SOUL_ITEMS.get(k);
         return item != null ? new ItemStack(item) : ItemStack.EMPTY;
      });
   }

   private static Vec3d soulPos(DarkFountainSealEffect.DepartingSoul s, float elapsed) {
      float rise = elapsed > 1.0F ? (elapsed - 1.0F) * 0.5F : 0.0F;
      float bob = (float)Math.sin((double)elapsed * 2.2 + (double)(s.uuid().hashCode() & 7)) * 0.04F;
      return new Vec3d(s.x(), s.y() + (double)rise + (double)bob, s.z());
   }

   private static float facingYaw(Vec3d p, float camYawNow) {
      if (Double.isNaN(fountainCx)) {
         return 180.0F - camYawNow;
      } else {
         double dx = fountainCx - p.x;
         double dz = fountainCz - p.z;
         return (float)Math.toDegrees(Math.atan2(dx, dz));
      }
   }

   public static void renderWorld(WorldRenderContext ctx) {
      if (active && !souls.isEmpty()) {
         frameProj = new Matrix4f(ctx.projectionMatrix());
         frameView = new Matrix4f(ctx.positionMatrix());
         frameCam = ctx.camera().getPos();
         frameCamYaw = ctx.camera().getYaw();
      } else {
         frameProj = null;
      }
   }

   public static void computeSoulScreenPositions(WorldRenderContext ctx) {
   }

   public static void renderSoulsHud(DrawContext ctx) {
      if (active && !souls.isEmpty() && frameProj != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            float elapsed = elapsedSeconds();
            if (!(elapsed >= 14.0F)) {
               float alpha = smoothstep(elapsed / 1.2F);
               if (!(alpha <= 0.01F)) {
                  ctx.draw();
                  Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
                  VertexSorter oldSort = RenderSystem.getVertexSorting();
                  RenderSystem.setProjectionMatrix(frameProj, VertexSorter.BY_DISTANCE);
                  Matrix4fStack mv = RenderSystem.getModelViewStack();
                  mv.pushMatrix();
                  mv.identity();
                  RenderSystem.applyModelViewMatrix();
                  RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                  DiffuseLighting.enableForLevel();

                  try {
                     Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
                     RenderLayer layer = RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
                     VertexConsumer buf = imm.getBuffer(layer);
                     MatrixStack ms = new MatrixStack();
                     ms.multiplyPositionMatrix(frameView);
                     Random rnd = Random.create(42L);
                     int light = 15728880;
                     int overlay = OverlayTexture.DEFAULT_UV;

                     for (DarkFountainSealEffect.DepartingSoul s : souls) {
                        ItemStack stack = stackOf(s.soul());
                        if (!stack.isEmpty()) {
                           BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, null, 0);
                           Vec3d p = soulPos(s, elapsed);
                           ms.push();
                           ms.translate(p.x - frameCam.x, p.y - frameCam.y, p.z - frameCam.z);
                           ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facingYaw(p, frameCamYaw) + (float)Math.sin((double)elapsed * 0.9) * 8.0F));
                           ms.scale(1.0F, 1.0F, 1.0F);
                           model.getTransformation().getTransformation(ModelTransformationMode.FIXED).apply(false, ms);
                           ms.translate(-0.5, -0.5, -0.5);

                           for (Direction d : Direction.values()) {
                              for (BakedQuad q : model.getQuads(null, d, rnd)) {
                                 buf.quad(ms.peek(), q, 1.0F, 1.0F, 1.0F, alpha, light, overlay);
                              }
                           }

                           for (BakedQuad q : model.getQuads(null, null, rnd)) {
                              buf.quad(ms.peek(), q, 1.0F, 1.0F, 1.0F, alpha, light, overlay);
                           }

                           ms.pop();
                        }
                     }

                     imm.draw(layer);
                  } finally {
                     RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                     mv.popMatrix();
                     RenderSystem.applyModelViewMatrix();
                     RenderSystem.setProjectionMatrix(oldProj, oldSort);
                  }
               }
            }
         }
      }
   }

   public static boolean cameraActive() {
      return active && elapsedSeconds() < 14.5F && ownSoul() != null;
   }

   private static DarkFountainSealEffect.DepartingSoul ownSoul() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return null;
      } else {
         for (DarkFountainSealEffect.DepartingSoul s : souls) {
            if (s.uuid().equals(mc.player.getUuid())) {
               return s;
            }
         }

         return null;
      }
   }

   public static void updateCamera() {
      MinecraftClient mc = MinecraftClient.getInstance();
      DarkFountainSealEffect.DepartingSoul own = ownSoul();
      if (own != null && mc.world != null) {
         float elapsed = elapsedSeconds();
         Vec3d soul = soulPos(own, Math.min(elapsed, 14.0F));
         if (camFrom == null && mc.player != null) {
            camFrom = mc.player.getCameraPosVec(1.0F);
            camYaw0 = mc.player.getYaw();
         }

         Vec3d away;
         if (!Double.isNaN(fountainCx)) {
            Vec3d d = new Vec3d(soul.x - fountainCx, 0.0, soul.z - fountainCz);
            away = d.lengthSquared() > 1.0E-4 ? d.normalize() : new Vec3d(1.0, 0.0, 0.0);
         } else {
            double yr = Math.toRadians((double)camYaw0);
            away = new Vec3d(-Math.sin(yr), 0.0, Math.cos(yr));
         }

         Vec3d want = soul.add(away.multiply(3.0)).add(0.0, -0.7, 0.0);
         BlockHitResult hit = mc.world.raycast(new RaycastContext(soul, want, ShapeType.VISUAL, FluidHandling.NONE, mc.player));
         if (hit != null && hit.getType() == Type.BLOCK) {
            Vec3d d = want.subtract(soul);
            double len = Math.max(0.6, hit.getPos().distanceTo(soul) - 0.25);
            want = soul.add(d.normalize().multiply(len));
         }

         float k = smoothstep(elapsed / 1.4F);
         Vec3d from = camFrom != null ? camFrom : want;
         camPos = from.add(want.subtract(from).multiply((double)k));
         camTarget = soul;
         Vec3d look = camTarget.subtract(camPos);
         double h = Math.sqrt(look.x * look.x + look.z * look.z);
         camYaw = (float)Math.toDegrees(Math.atan2(-look.x, look.z));
         camPitch = (float)(-Math.toDegrees(Math.atan2(look.y, Math.max(1.0E-4, h))));
      }
   }

   public static Vec3d cameraPos() {
      return camPos != null ? camPos : Vec3d.ZERO;
   }

   public static float cameraYaw() {
      return camYaw;
   }

   public static float cameraPitch() {
      return camPitch;
   }

   public static void renderFlash(DrawContext ctx, MinecraftClient mc) {
      if (active) {
         float elapsed = elapsedSeconds();
         if (elapsed >= 15.5F) {
            active = false;
            souls = List.of();
            camFrom = null;
         } else if (!(elapsed < 11.0F)) {
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            int cx = sw / 2;
            if (elapsed < 14.0F) {
               float t = clamp01((elapsed - 11.0F) / 3.0F);
               float transT = smoothstep(clamp01(t * 1.3F));
               float opaqueT = smoothstep(t);
               int transW = Math.max(1, Math.round(transT * (float)sw));
               int opaqueW = Math.max(1, Math.round(opaqueT * (float)sw));
               int transX0 = cx - transW / 2;
               int opaqueX0 = cx - opaqueW / 2;
               ctx.fill(transX0, 0, transX0 + transW, sh, argb(128, 255, 255, 255));
               ctx.fill(opaqueX0, 0, opaqueX0 + opaqueW, sh, argb(255, 255, 255, 255));
            } else {
               float dt = elapsed - 14.0F;
               if (dt < 0.5F) {
                  ctx.fill(0, 0, sw, sh, argb(255, 255, 255, 255));
               } else {
                  float ft = clamp01((dt - 0.5F) / 1.0F);
                  int a = Math.round((1.0F - ft) * 255.0F);
                  ctx.fill(0, 0, sw, sh, argb(a, 255, 255, 255));
               }
            }
         }
      }
   }

   private static int argb(int a, int r, int g, int b) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static record DepartingSoul(UUID uuid, SoulType soul, double x, double y, double z) {
   }
}
