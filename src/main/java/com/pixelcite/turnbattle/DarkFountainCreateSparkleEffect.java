package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase.Texture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class DarkFountainCreateSparkleEffect {
   private static final int PAIR_COUNT = 10;
   private static final float SPRITE1_HOLD_SEC = 0.05F;
   private static final float SPRITE2_HOLD_SEC = 0.1F;
   private static final float PAIR_LIFETIME_SEC = 0.15F;
   private static final float SPAWN_INTERVAL_SEC = 0.1F;
   private static final float TOTAL_SEC = 1.0500001F;
   private static final float SPARKLE_SIZE = 0.14F;
   private static final float WAVE_AMPLITUDE = 0.35F;
   private static final float WAVE_CYCLES = 1.0F;
   private static final float PAIR_Y_GAP = 0.09F;
   private static final float FADE_OUT_SEC = 0.03F;
   private static final Identifier TEX_1 = Identifier.of("turnbattle", "textures/sparkles/spr_fountainsparkle1.png");
   private static final Identifier TEX_2 = Identifier.of("turnbattle", "textures/sparkles/spr_fountainsparkle2.png");
   private static boolean active = false;
   private static long startNanos = 0L;
   private static double leftX;
   private static double leftZ;
   private static double rightX;
   private static double rightZ;
   private static double baseY;

   public static boolean isActive() {
      return active;
   }

   public static void start(double leftX, double leftZ, double rightX, double rightZ, double baseY) {
      DarkFountainCreateSparkleEffect.leftX = leftX;
      DarkFountainCreateSparkleEffect.leftZ = leftZ;
      DarkFountainCreateSparkleEffect.rightX = rightX;
      DarkFountainCreateSparkleEffect.rightZ = rightZ;
      DarkFountainCreateSparkleEffect.baseY = baseY;
      startNanos = System.nanoTime();
      active = true;
   }

   private static float elapsedSeconds() {
      return (float)(System.nanoTime() - startNanos) / 1.0E9F;
   }

   private static float clamp01(float t) {
      return Math.max(0.0F, Math.min(1.0F, t));
   }

   private static double pairX(int i) {
      float t = (float)i / 9.0F;
      return leftX + (rightX - leftX) * (double)t;
   }

   private static double pairZ(int i) {
      float t = (float)i / 9.0F;
      return leftZ + (rightZ - leftZ) * (double)t;
   }

   private static double pairBaseY(int i) {
      float t = (float)i / 9.0F;
      return baseY + Math.sin((double)t * Math.PI * 1.0) * 0.35F;
   }

   private static RenderLayer getSparkleLayer(Identifier tex) {
      MultiPhaseParameters params = MultiPhaseParameters.builder()
         .program(RenderPhase.ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM)
         .texture(new Texture(tex, false, false))
         .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
         .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
         .cull(RenderPhase.DISABLE_CULLING)
         .lightmap(RenderPhase.ENABLE_LIGHTMAP)
         .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
         .build(false);
      return RenderLayer.of("turnbattle_fountain_sparkle", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 256, false, true, params);
   }

   public static void render(WorldRenderContext ctx) {
      if (active) {
         float elapsed = elapsedSeconds();
         if (elapsed >= 1.0500001F) {
            active = false;
         } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) {
               Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
               Vec3d cam = ctx.camera().getPos();
               MatrixStack matrices = new MatrixStack();

               for (int i = 0; i < 10; i++) {
                  float pairSpawn = (float)i * 0.1F;
                  float local = elapsed - pairSpawn;
                  if (!(local < 0.0F) && !(local >= 0.15F)) {
                     Identifier tex = local < 0.05F ? TEX_1 : TEX_2;
                     float alpha = 1.0F;
                     float timeLeft = 0.15F - local;
                     if (timeLeft < 0.03F) {
                        alpha = clamp01(timeLeft / 0.03F);
                     }

                     double x = pairX(i);
                     double y0 = pairBaseY(i);
                     double pz = pairZ(i);
                     drawSparkle(consumers, matrices, ctx, cam, tex, x, y0, pz, alpha);
                     drawSparkle(consumers, matrices, ctx, cam, tex, x, y0 + 0.09F, pz, alpha);
                  }
               }
            }
         }
      }
   }

   private static void drawSparkle(
      VertexConsumerProvider consumers, MatrixStack matrices, WorldRenderContext ctx, Vec3d cam, Identifier tex, double wx, double wy, double wz, float alpha
   ) {
      double px = wx - cam.x;
      double py = wy - cam.y;
      double pz = wz - cam.z;
      matrices.push();
      matrices.translate(px, py, pz);
      matrices.multiply(ctx.camera().getRotation());
      matrices.translate(0.0F, 0.0F, -0.01F);
      float hs = 0.07F;
      Matrix4f posM = matrices.peek().getPositionMatrix();
      int light = 15728880;
      VertexConsumer buf = consumers.getBuffer(getSparkleLayer(tex));
      buf.vertex(posM, -hs, -hs, 0.0F)
         .color(1.0F, 1.0F, 1.0F, alpha)
         .texture(0.0F, 1.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(light)
         .normal(0.0F, 1.0F, 0.0F);
      buf.vertex(posM, hs, -hs, 0.0F)
         .color(1.0F, 1.0F, 1.0F, alpha)
         .texture(1.0F, 1.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(light)
         .normal(0.0F, 1.0F, 0.0F);
      buf.vertex(posM, hs, hs, 0.0F)
         .color(1.0F, 1.0F, 1.0F, alpha)
         .texture(1.0F, 0.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(light)
         .normal(0.0F, 1.0F, 0.0F);
      buf.vertex(posM, -hs, hs, 0.0F)
         .color(1.0F, 1.0F, 1.0F, alpha)
         .texture(0.0F, 0.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(light)
         .normal(0.0F, 1.0F, 0.0F);
      matrices.pop();
   }
}
