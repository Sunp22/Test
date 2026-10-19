package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.CloneAnimator;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BipedEntityModel.class})
public abstract class BipedEntityModelPoseMixin {
   @Inject(
      method = {"setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void turnbattle$applyClonePose(
      LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci
   ) {
      CloneAnimator.applyModelPose((BipedEntityModel<?>)this, entity);
   }
}
