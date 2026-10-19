package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.GhostRender;
import com.pixelcite.turnbattle.MobIdle;
import java.util.ArrayDeque;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntityRenderer.class})
public abstract class GhostRenderMixin {
   @Unique
   private final ArrayDeque<int[]> turnbattle$saved = new ArrayDeque<>();

   @Inject(
      method = {"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")}
   )
   private void turnbattle$ghostBegin(
      LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci
   ) {
      int[] sv = new int[]{entity.deathTime, entity.hurtTime, 0};
      if (GhostRender.active) {
         entity.deathTime = 0;
         entity.hurtTime = 0;
      }

      if (MobIdle.applies(entity)) {
         matrices.push();
         MobIdle.breathe(entity, tickDelta, matrices);
         sv[2] = 1;
      }

      this.turnbattle$saved.push(sv);
   }

   @Inject(
      method = {"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("RETURN")}
   )
   private void turnbattle$ghostEnd(
      LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci
   ) {
      int[] sv = this.turnbattle$saved.poll();
      if (sv != null) {
         if (GhostRender.active) {
            entity.deathTime = sv[0];
            entity.hurtTime = sv[1];
         }

         if (sv[2] == 1) {
            matrices.pop();
            MobIdle.restore();
         }
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
         shift = Shift.AFTER
      )}
   )
   private void turnbattle$idlePose(
      LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci
   ) {
      if (MobIdle.applies(entity)) {
         MobIdle.pose(((LivingEntityRenderer)this).getModel(), entity, (float)entity.age + tickDelta);
      }
   }

   @Inject(
      method = {"getRenderLayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$ghostLayer(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
      if (GhostRender.translucent()) {
         Identifier tex = ((EntityRenderer)this).getTexture(entity);
         cir.setReturnValue(RenderLayer.getEntityTranslucent(tex));
      }
   }

   @Inject(
      method = {"getOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void turnbattle$ghostOverlay(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
      if (GhostRender.active) {
         cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(GhostRender.white), OverlayTexture.getV(GhostRender.red)));
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"
      )
   )
   private void turnbattle$ghostFeatures(
      FeatureRenderer feature,
      MatrixStack matrices,
      VertexConsumerProvider consumers,
      int light,
      Entity entity,
      float limbAngle,
      float limbDistance,
      float tickDelta,
      float animationProgress,
      float headYaw,
      float headPitch
   ) {
      if (GhostRender.features()) {
         feature.render(matrices, consumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
      }
   }
}
