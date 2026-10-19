package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LiveFirstPerson;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public abstract class LiveFirstPersonMixin {
   @Unique
   private boolean turnbattle$livePushed;

   @ModifyVariable(
      method = {"renderFirstPersonItem"},
      at = @At("HEAD"),
      ordinal = 2,
      argsOnly = true
   )
   private float turnbattle$liveNoSwing(float swingProgress) {
      return LiveFirstPerson.suppressSwing() ? 0.0F : swingProgress;
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")}
   )
   private void turnbattle$liveFirstPerson(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      this.turnbattle$livePushed = hand == Hand.MAIN_HAND && LiveFirstPerson.apply(matrices, player);
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("RETURN")}
   )
   private void turnbattle$liveFirstPersonEnd(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      if (this.turnbattle$livePushed) {
         matrices.pop();
         this.turnbattle$livePushed = false;
      }
   }
}
