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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class WallClimbTelegraphRenderer {
   private static final Identifier WHITE_TEXTURE = Identifier.of("minecraft", "textures/misc/white.png");
   private static final float SIZE = 0.98F;
   private static final float OFFSET = 0.02F;
   private static boolean active = false;
   private static BlockPos target;
   private static Direction wallOut;
   private static boolean valid;

   public static void set(BlockPos newTarget, Direction newWallOut, boolean newValid) {
      target = newTarget;
      wallOut = newWallOut;
      valid = newValid;
      active = true;
   }

   public static void clear() {
      active = false;
   }

   private static RenderLayer getHighlightLayer() {
      MultiPhaseParameters params = MultiPhaseParameters.builder()
         .program(RenderPhase.ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM)
         .texture(new Texture(WHITE_TEXTURE, false, false))
         .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
         .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
         .cull(RenderPhase.DISABLE_CULLING)
         .lightmap(RenderPhase.ENABLE_LIGHTMAP)
         .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
         .build(false);
      return RenderLayer.of("turnbattle_climb_telegraph", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 256, false, true, params);
   }

   public static void render(WorldRenderContext ctx) {
      if (active) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            float r = valid ? 1.0F : 0.8F;
            float g = valid ? 0.85F : 0.15F;
            float b = valid ? 0.1F : 0.15F;
            float alpha = 0.55F;
            Direction wallRight = wallOut.getOpposite().rotateYClockwise();
            double cx = (double)target.getX() + 0.5 + (double)wallOut.getOffsetX() * 0.5199999995529652;
            double cy = (double)target.getY() + 0.5;
            double cz = (double)target.getZ() + 0.5 + (double)wallOut.getOffsetZ() * 0.5199999995529652;
            double rx = (double)((float)wallRight.getOffsetX() * 0.98F) / 2.0;
            double rz = (double)((float)wallRight.getOffsetZ() * 0.98F) / 2.0;
            double halfH = 0.49F;
            Vec3d cam = ctx.camera().getPos();
            double x0 = cx - rx - cam.x;
            double x1 = cx + rx - cam.x;
            double y0 = cy - halfH - cam.y;
            double y1 = cy + halfH - cam.y;
            double z0 = cz - rz - cam.z;
            double z1 = cz + rz - cam.z;
            MatrixStack matrices = new MatrixStack();
            Matrix4f posM = matrices.peek().getPositionMatrix();
            int light = 15728880;
            Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer buf = consumers.getBuffer(getHighlightLayer());
            buf.vertex(posM, (float)x0, (float)y0, (float)z0)
               .color(r, g, b, alpha)
               .texture(0.0F, 1.0F)
               .overlay(OverlayTexture.DEFAULT_UV)
               .light(light)
               .normal(0.0F, 1.0F, 0.0F);
            buf.vertex(posM, (float)x1, (float)y0, (float)z1)
               .color(r, g, b, alpha)
               .texture(1.0F, 1.0F)
               .overlay(OverlayTexture.DEFAULT_UV)
               .light(light)
               .normal(0.0F, 1.0F, 0.0F);
            buf.vertex(posM, (float)x1, (float)y1, (float)z1)
               .color(r, g, b, alpha)
               .texture(1.0F, 0.0F)
               .overlay(OverlayTexture.DEFAULT_UV)
               .light(light)
               .normal(0.0F, 1.0F, 0.0F);
            buf.vertex(posM, (float)x0, (float)y1, (float)z0)
               .color(r, g, b, alpha)
               .texture(0.0F, 0.0F)
               .overlay(OverlayTexture.DEFAULT_UV)
               .light(light)
               .normal(0.0F, 1.0F, 0.0F);
         }
      }
   }
}
