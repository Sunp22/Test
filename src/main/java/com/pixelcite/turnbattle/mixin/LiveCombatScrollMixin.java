package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LiveCombatClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public abstract class LiveCombatScrollMixin {
   @Inject(
      method = {"onMouseScroll(JDD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$liveScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (window == MinecraftClient.getInstance().getWindow().getHandle()) {
         if (LiveCombatClient.onScroll(vertical)) {
            ci.cancel();
         }
      }
   }
}
