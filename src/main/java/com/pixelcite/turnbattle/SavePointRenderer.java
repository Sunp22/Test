package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase.Texture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class SavePointRenderer {
   private static final int FRAME_COUNT = 6;
   private static final int TICKS_PER_FRAME = 8;
   private static final float SPRITE_WIDTH = 0.625F;
   private static final float SPRITE_HEIGHT = 0.594F;
   private static final Identifier[] FRAMES = new Identifier[6];
   private static long globalTick;

   public static void tick() {
      globalTick++;
   }

   private static RenderLayer getSavePointLayer(Identifier tex) {
      MultiPhaseParameters params = MultiPhaseParameters.builder()
         .program(RenderPhase.ENTITY_TRANSLUCENT_PROGRAM)
         .texture(new Texture(tex, false, false))
         .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
         .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
         .cull(RenderPhase.DISABLE_CULLING)
         .lightmap(RenderPhase.ENABLE_LIGHTMAP)
         .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
         .build(false);
      return RenderLayer.of("turnbattle_savepoint", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 256, false, false, params);
   }

   public static void render(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         Identifier frame = FRAMES[(int)(globalTick / 8L % 6L)];
         RenderLayer layer = getSavePointLayer(frame);
         Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
         Vec3d cam = ctx.camera().getPos();
         float tickDelta = ctx.tickCounter().getTickDelta(true);
         MatrixStack matrices = new MatrixStack();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof SavePointEntity sp) {
               double lerpX = MathHelper.lerp((double)tickDelta, sp.prevX, sp.getX());
               double lerpY = MathHelper.lerp((double)tickDelta, sp.prevY, sp.getY());
               double lerpZ = MathHelper.lerp((double)tickDelta, sp.prevZ, sp.getZ());
               double px = lerpX - cam.x;
               double py = lerpY - cam.y + 0.594F;
               double pz = lerpZ - cam.z;
               matrices.push();
               matrices.translate(px, py, pz);
               matrices.multiply(ctx.camera().getRotation());
               float hw = 0.3125F;
               float hh = 0.297F;
               Matrix4f posM = matrices.peek().getPositionMatrix();
               int light = 15728880;
               VertexConsumer buf = consumers.getBuffer(layer);
               buf.vertex(posM, -hw, -hh, 0.0F)
                  .color(1.0F, 1.0F, 1.0F, 1.0F)
                  .texture(0.0F, 1.0F)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(light)
                  .normal(0.0F, 1.0F, 0.0F);
               buf.vertex(posM, hw, -hh, 0.0F)
                  .color(1.0F, 1.0F, 1.0F, 1.0F)
                  .texture(1.0F, 1.0F)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(light)
                  .normal(0.0F, 1.0F, 0.0F);
               buf.vertex(posM, hw, hh, 0.0F)
                  .color(1.0F, 1.0F, 1.0F, 1.0F)
                  .texture(1.0F, 0.0F)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(light)
                  .normal(0.0F, 1.0F, 0.0F);
               buf.vertex(posM, -hw, hh, 0.0F)
                  .color(1.0F, 1.0F, 1.0F, 1.0F)
                  .texture(0.0F, 0.0F)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(light)
                  .normal(0.0F, 1.0F, 0.0F);
               matrices.pop();
            }
         }
      }
   }

   static {
      for (int i = 0; i < 6; i++) {
         FRAMES[i] = Identifier.of("turnbattle", "textures/savepoint/savepoint" + (i + 1) + ".png");
      }

      globalTick = 0L;
   }
}
