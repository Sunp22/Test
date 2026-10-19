package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkWorldSkinsClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayerEntity.class})
public abstract class ClientPlayerSkinMixin {
   @Inject(
      method = {"getSkinTextures"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void turnbattle$darkWorldSkin(CallbackInfoReturnable<SkinTextures> cir) {
      AbstractClientPlayerEntity self = (AbstractClientPlayerEntity)this;
      SkinTextures swapped = DarkWorldSkinsClient.override(self.getUuid(), (SkinTextures)cir.getReturnValue());
      if (swapped != cir.getReturnValue()) {
         cir.setReturnValue(swapped);
      }
   }
}
