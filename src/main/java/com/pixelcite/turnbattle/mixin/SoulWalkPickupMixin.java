package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.SoulWalk;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemEntity.class})
public abstract class SoulWalkPickupMixin {
   @Inject(
      method = {"onPlayerCollision"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$soulPicksNothingUp(PlayerEntity player, CallbackInfo ci) {
      if (SoulWalk.isOut(player)) {
         ci.cancel();
      }
   }
}
