package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public class BattleFramerateMixin {
   @Shadow
   private boolean field_1734;

   @Inject(
      method = {"getFramerateLimit"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void unlockBattleFps(CallbackInfoReturnable<Integer> cir) {
      MinecraftClient mc = (MinecraftClient)this;
      if (mc.currentScreen instanceof BattleScreen) {
         int configured = (Integer)mc.options.getMaxFps().getValue();
         cir.setReturnValue(configured >= 260 ? Integer.MAX_VALUE : configured);
      }
   }

   @Inject(
      method = {"isWindowFocused"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void treatBattleAsAlwaysFocused(CallbackInfoReturnable<Boolean> cir) {
      MinecraftClient mc = (MinecraftClient)this;
      if (mc.currentScreen instanceof BattleScreen) {
         cir.setReturnValue(true);
      }
   }
}
