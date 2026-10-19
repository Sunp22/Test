package com.pixelcite.turnbattle.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HungerManager.class})
public class HungerManagerMixin {
   @Inject(
      method = {"update"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lockHunger(ServerPlayerEntity player, CallbackInfo ci) {
      HungerManager self = (HungerManager)this;
      self.setFoodLevel(20);
      ci.cancel();
   }
}
