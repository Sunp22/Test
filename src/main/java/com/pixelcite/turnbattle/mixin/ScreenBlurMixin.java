package com.pixelcite.turnbattle.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class ScreenBlurMixin {
   @Inject(
      method = {"renderBlur"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cancelBlur(float delta, CallbackInfo ci) {
      ci.cancel();
   }
}
