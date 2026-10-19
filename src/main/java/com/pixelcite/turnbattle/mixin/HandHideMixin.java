package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkFountainSealEffect;
import com.pixelcite.turnbattle.OverhaulView;
import com.pixelcite.turnbattle.PlayerActions;
import com.pixelcite.turnbattle.SoulWalk;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class HandHideMixin {
   @Inject(
      method = {"renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$hideHand(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
      if (OverhaulView.cameraActive() || PlayerActions.cameraActive() || SoulWalk.localOut || DarkFountainSealEffect.cameraActive()) {
         ci.cancel();
      }
   }
}
