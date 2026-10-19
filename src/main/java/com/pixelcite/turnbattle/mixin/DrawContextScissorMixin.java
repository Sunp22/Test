package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleEntityRenderer;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DrawContext.class})
public class DrawContextScissorMixin {
   @Inject(
      method = {"enableScissor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void noClipDuringBattleRender(int x1, int y1, int x2, int y2, CallbackInfo ci) {
      if (BattleEntityRenderer.isRenderingEntity()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"disableScissor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void noUnclipDuringBattleRender(CallbackInfo ci) {
      if (BattleEntityRenderer.isRenderingEntity()) {
         ci.cancel();
      }
   }
}
