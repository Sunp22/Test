package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LiveCombatClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public abstract class LiveCombatInputMixin {
   @Inject(
      method = {"doAttack()Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$liveAttack(CallbackInfoReturnable<Boolean> cir) {
      if (LiveCombatClient.onAttackKey((MinecraftClient)this)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"doItemUse()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$liveUse(CallbackInfo ci) {
      if (LiveCombatClient.onUseKey((MinecraftClient)this)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleBlockBreaking(Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$liveNoMining(boolean breaking, CallbackInfo ci) {
      if (breaking && LiveCombatClient.cancelBlockBreaking()) {
         MinecraftClient mc = (MinecraftClient)this;
         if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
         }

         ci.cancel();
      }
   }
}
