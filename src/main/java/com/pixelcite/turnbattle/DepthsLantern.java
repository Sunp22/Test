package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class DepthsLantern {
   private static final Identifier WHITE = Identifier.of("turnbattle", "textures/entity/lantern_white.png");
   private static final float PX = 0.0625F;
   private static final int FULL_BRIGHT = 15728880;

   private DepthsLantern() {
   }

   static boolean carries(LivingEntity e) {
      if (!DepthsClient.inRun()) {
         return false;
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return e == mc.player || e instanceof BattleEntityRenderer.RecruitFakePlayer;
      }
   }

   static boolean inHand(LivingEntity e) {
      return carries(e)
         && !(MinecraftClient.getInstance().currentScreen instanceof BattleScreen)
         && (e != MinecraftClient.getInstance().player || !DepthsClient.lanternDown());
   }

   private static void draw(MatrixStack ms, VertexConsumerProvider vcp, int light, SoulType soul, float t) {
      int sc = soul != null ? soul.getColor() & 16777215 : 16777215;
      float sr = (float)(sc >> 16 & 0xFF) / 255.0F;
      float sg = (float)(sc >> 8 & 0xFF) / 255.0F;
      float sb = (float)(sc & 0xFF) / 255.0F;
      float pulse = 0.85F + 0.15F * (float)Math.sin((double)(t * 3.1F));
      VertexConsumer iron = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
      float ir = 0.2F;
      float ig = 0.2F;
      float ib = 0.25F;
      float hr = 0.36F;
      float hg = 0.36F;
      float hb = 0.42F;
      box(iron, ms, -1.5F, 0.0F, -0.5F, 1.5F, 0.6F, 0.5F, hr, hg, hb, 1.0F, light);
      box(iron, ms, -1.5F, 0.0F, -0.5F, -0.9F, 2.0F, 0.5F, hr, hg, hb, 1.0F, light);
      box(iron, ms, 0.9F, 0.0F, -0.5F, 1.5F, 2.0F, 0.5F, hr, hg, hb, 1.0F, light);
      box(iron, ms, -0.5F, 2.0F, -0.5F, 0.5F, 3.0F, 0.5F, ir, ig, ib, 1.0F, light);
      box(iron, ms, -2.0F, 3.0F, -2.0F, 2.0F, 4.0F, 2.0F, hr, hg, hb, 1.0F, light);
      box(iron, ms, -3.0F, 4.0F, -3.0F, 3.0F, 5.0F, 3.0F, ir, ig, ib, 1.0F, light);
      box(iron, ms, -3.0F, 11.0F, -3.0F, 3.0F, 12.0F, 3.0F, ir, ig, ib, 1.0F, light);

      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sz = -1; sz <= 1; sz += 2) {
            float x0 = sx < 0 ? -3.0F : 2.2F;
            float z0 = sz < 0 ? -3.0F : 2.2F;
            box(iron, ms, x0, 5.0F, z0, x0 + 0.8F, 11.0F, z0 + 0.8F, hr, hg, hb, 1.0F, light);
         }
      }

      VertexConsumer heart = vcp.getBuffer(
         RenderLayer.getEntityTranslucentEmissive(soul != null ? soul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png"))
      );
      float hs = 2.6F * (0.94F + 0.06F * pulse);
      float bob = 0.25F * (float)Math.sin((double)(t * 2.2F));
      ms.push();
      ms.translate(0.0F, (8.0F + bob) * 0.0625F, 0.0F);
      ms.multiply(new Quaternionf().rotationY(t * 0.9F));

      for (int k = 0; k < 2; k++) {
         ms.push();
         ms.multiply(new Quaternionf().rotationY((float)k * (float) Math.PI / 2.0F));
         quad(heart, ms, -hs, -hs, hs, hs, 1.0F, 1.0F, 1.0F, 1.0F, 15728880, true);
         ms.pop();
      }

      ms.pop();
      VertexConsumer glass = vcp.getBuffer(RenderLayer.getEntityTranslucent(WHITE));
      box(glass, ms, -2.4F, 5.0F, -2.4F, 2.4F, 11.0F, 2.4F, 0.75F + 0.25F * sr, 0.75F + 0.25F * sg, 0.75F + 0.25F * sb, 0.22F, 15728880);
   }

   private static void box(
      VertexConsumer vc, MatrixStack ms, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a, int light
   ) {
      x0 *= 0.0625F;
      y0 *= 0.0625F;
      z0 *= 0.0625F;
      x1 *= 0.0625F;
      y1 *= 0.0625F;
      z1 *= 0.0625F;
      Entry en = ms.peek();
      Matrix4f m = en.getPositionMatrix();
      face(vc, m, en, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0.0F, -1.0F, 0.0F, r, g, b, a, light);
      face(vc, m, en, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0.0F, 1.0F, 0.0F, r * 0.7F, g * 0.7F, b * 0.7F, a, light);
      face(vc, m, en, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0.0F, 0.0F, -1.0F, r * 0.85F, g * 0.85F, b * 0.85F, a, light);
      face(vc, m, en, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, 0.0F, 0.0F, 1.0F, r * 0.85F, g * 0.85F, b * 0.85F, a, light);
      face(vc, m, en, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1.0F, 0.0F, 0.0F, r * 0.78F, g * 0.78F, b * 0.78F, a, light);
      face(vc, m, en, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1.0F, 0.0F, 0.0F, r * 0.78F, g * 0.78F, b * 0.78F, a, light);
   }

   private static void face(
      VertexConsumer vc,
      Matrix4f m,
      Entry en,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      float nx,
      float ny,
      float nz,
      float r,
      float g,
      float b,
      float a,
      int light
   ) {
      vc.vertex(m, ax, ay, az).color(r, g, b, a).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, nx, ny, nz);
      vc.vertex(m, bx, by, bz).color(r, g, b, a).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, nx, ny, nz);
      vc.vertex(m, cx, cy, cz).color(r, g, b, a).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, nx, ny, nz);
      vc.vertex(m, dx, dy, dz).color(r, g, b, a).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, nx, ny, nz);
   }

   private static void quad(
      VertexConsumer vc, MatrixStack ms, float x0, float y0, float x1, float y1, float r, float g, float b, float a, int light, boolean twoSided
   ) {
      x0 *= 0.0625F;
      y0 *= 0.0625F;
      x1 *= 0.0625F;
      y1 *= 0.0625F;
      Entry en = ms.peek();
      Matrix4f m = en.getPositionMatrix();
      vc.vertex(m, x0, y1, 0.0F).color(r, g, b, a).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, -1.0F);
      vc.vertex(m, x1, y1, 0.0F).color(r, g, b, a).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, -1.0F);
      vc.vertex(m, x1, y0, 0.0F).color(r, g, b, a).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, -1.0F);
      vc.vertex(m, x0, y0, 0.0F).color(r, g, b, a).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, -1.0F);
      if (twoSided) {
         vc.vertex(m, x0, y0, 0.0F).color(r, g, b, a).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, 1.0F);
         vc.vertex(m, x1, y0, 0.0F).color(r, g, b, a).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, 1.0F);
         vc.vertex(m, x1, y1, 0.0F).color(r, g, b, a).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, 1.0F);
         vc.vertex(m, x0, y1, 0.0F).color(r, g, b, a).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0.0F, 0.0F, 1.0F);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Feature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
      Feature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
         super(ctx);
      }

      public void render(
         MatrixStack ms,
         VertexConsumerProvider vcp,
         int light,
         AbstractClientPlayerEntity e,
         float limbAngle,
         float limbDistance,
         float tickDelta,
         float age,
         float headYaw,
         float headPitch
      ) {
         if (DepthsLantern.carries(e)) {
            float t = age / 20.0F;
            ms.push();

            try {
               if (DepthsLantern.inHand(e)) {
                  ModelPart arm = e.getMainArm() == Arm.LEFT
                     ? ((PlayerEntityModel)this.getContextModel()).leftArm
                     : ((PlayerEntityModel)this.getContextModel()).rightArm;
                  arm.rotate(ms);
                  ms.translate((float)(e.getMainArm() == Arm.LEFT ? 1 : -1) * 0.0625F, 0.65625F, 0.0F);
                  ms.multiply(new Quaternionf().rotationZYX(arm.roll, arm.yaw, arm.pitch).conjugate());
                  float sway = limbDistance * 0.35F * (float)Math.sin((double)(limbAngle * 0.6662F));
                  ms.multiply(new Quaternionf().rotationXYZ(sway, 0.0F, sway * 0.4F));
               } else if (e == MinecraftClient.getInstance().player && DepthsClient.lanternDown()) {
                  ms.translate((e.getMainArm() == Arm.LEFT ? -5.5F : 5.5F) * 0.0625F, 0.75F, -0.375F);
               } else {
                  ms.translate((e.getMainArm() == Arm.LEFT ? 6.5F : -6.5F) * 0.0625F, 0.75F, 0.5625F);
               }

               DepthsLantern.draw(ms, vcp, light, DepthsClient.carriedSoul(), t);
            } finally {
               ms.pop();
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class HeldItems extends PlayerHeldItemFeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
      HeldItems(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, HeldItemRenderer hir) {
         super(ctx, hir);
      }

      protected void renderItem(
         LivingEntity entity, ItemStack stack, ModelTransformationMode mode, Arm arm, MatrixStack matrices, VertexConsumerProvider vcp, int light
      ) {
         if (arm != entity.getMainArm() || !DepthsLantern.inHand(entity)) {
            super.renderItem(entity, stack, mode, arm, matrices, vcp, light);
         }
      }
   }
}
