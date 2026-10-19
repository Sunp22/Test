package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkWorldSkinsClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListEntry.class})
public abstract class PlayerListSkinMixin {
   @Inject(
      method = {"getSkinTextures"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void turnbattle$darkWorldSkin(CallbackInfoReturnable<SkinTextures> cir) {
      PlayerListEntry self = (PlayerListEntry)this;
      SkinTextures swapped = DarkWorldSkinsClient.override(self.getProfile().getId(), (SkinTextures)cir.getReturnValue());
      if (swapped != cir.getReturnValue()) {
         cir.setReturnValue(swapped);
      }
   }
}
