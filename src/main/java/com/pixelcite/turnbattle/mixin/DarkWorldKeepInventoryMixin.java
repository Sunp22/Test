package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkFountainManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public abstract class DarkWorldKeepInventoryMixin {
   @Unique
   private boolean turnbattle$inDarkWorld() {
      PlayerEntity self = (PlayerEntity)this;
      return !self.getWorld().isClient() && DarkFountainManager.isDarkWorld(self.getWorld().getRegistryKey());
   }

   @Inject(
      method = {"dropInventory()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$keepItems(CallbackInfo ci) {
      if (this.turnbattle$inDarkWorld()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"getXpToDrop()I"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$keepXp(CallbackInfoReturnable<Integer> cir) {
      if (this.turnbattle$inDarkWorld()) {
         cir.setReturnValue(0);
      }
   }
}
