package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.CloneAnimator;
import com.pixelcite.turnbattle.PlayerActions;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemFeatureRenderer.class})
public abstract class HeldItemGripMixin {
   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )},
      require = 0
   )
   private void turnbattle$turnGrip(
      LivingEntity entity,
      ItemStack stack,
      ModelTransformationMode mode,
      Arm arm,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      if (entity != null && arm == entity.getMainArm()) {
         float flip = PlayerActions.gripFlip(entity);
         float roll = CloneAnimator.bladeRollFor(entity);
         float tilt = CloneAnimator.bladeTiltFor(entity);
         if (flip != 0.0F || roll != 0.0F || tilt != 0.0F) {
            if (flip != 0.0F) {
               matrices.translate(0.0F, -0.046F, 0.0834F);
               matrices.multiply(RotationAxis.POSITIVE_X.rotation(flip));
               matrices.translate(0.0F, 0.046F, -0.0834F);
            }

            if (tilt != 0.0F || roll != 0.0F) {
               matrices.translate(0.0F, -0.06F, 0.09F);
               if (tilt != 0.0F) {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotation(tilt));
               }

               if (roll != 0.0F) {
                  matrices.multiply(new Quaternionf().rotationAxis(roll, 0.0F, 0.985F, -0.174F));
               }

               matrices.translate(0.0F, 0.06F, -0.09F);
            }
         }
      }
   }
}
