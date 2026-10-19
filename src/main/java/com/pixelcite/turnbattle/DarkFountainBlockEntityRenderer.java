package com.pixelcite.turnbattle;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class DarkFountainBlockEntityRenderer implements BlockEntityRenderer<DarkFountainBlockEntity> {
   public DarkFountainBlockEntityRenderer(Context ctx) {
   }

   public void render(DarkFountainBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
   }

   private static void renderCube(VertexConsumer vc, Matrix4f matrix, int light, int overlay) {
      face(vc, matrix, light, overlay, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      face(vc, matrix, light, overlay, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
      face(vc, matrix, light, overlay, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F);
      face(vc, matrix, light, overlay, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      face(vc, matrix, light, overlay, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, 0.0F);
      face(vc, matrix, light, overlay, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F);
   }

   private static void face(
      VertexConsumer vc,
      Matrix4f matrix,
      int light,
      int overlay,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float nx,
      float ny,
      float nz
   ) {
      vc.vertex(matrix, x1, y1, z1).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(overlay).light(light).normal(nx, ny, nz);
      vc.vertex(matrix, x2, y2, z2).color(255, 255, 255, 255).texture(1.0F, 0.0F).overlay(overlay).light(light).normal(nx, ny, nz);
      vc.vertex(matrix, x3, y3, z3).color(255, 255, 255, 255).texture(1.0F, 1.0F).overlay(overlay).light(light).normal(nx, ny, nz);
      vc.vertex(matrix, x4, y4, z4).color(255, 255, 255, 255).texture(0.0F, 1.0F).overlay(overlay).light(light).normal(nx, ny, nz);
   }
}
