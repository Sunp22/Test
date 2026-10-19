package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DepthsClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public abstract class DepthsInputMixin {
   @Inject(
      method = {"tick(ZF)V"},
      at = {@At("TAIL")}
   )
   private void turnbattle$depthsWalk(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
      DepthsClient.adjustInput((Input)this);
   }
}
