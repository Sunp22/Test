package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DepthsClient;
import com.pixelcite.turnbattle.LiveCombatClient;
import com.pixelcite.turnbattle.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class ActionLockMixin {
   @Inject(
      method = {"changeLookDirection(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$lockLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if (this == MinecraftClient.getInstance().player && PlayerActions.localLocked()) {
         ci.cancel();
      } else if (this == MinecraftClient.getInstance().player && DepthsClient.controlling()) {
         ci.cancel();
      } else {
         if (this == MinecraftClient.getInstance().player && LiveCombatClient.lookLocked()) {
            ci.cancel();
         }
      }
   }
}
