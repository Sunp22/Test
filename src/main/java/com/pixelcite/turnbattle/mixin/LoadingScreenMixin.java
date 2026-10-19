package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LoadingScreenRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public abstract class LoadingScreenMixin {
   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$messageLoading(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen self = (Screen)this;
      if (self instanceof MessageScreen) {
         LoadingScreenRenderer.render(ctx, self.width, self.height, self.getTitle(), -1.0F);
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$connectLoading(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen self = (Screen)this;
      if (self instanceof ConnectScreen) {
         LoadingScreenRenderer.render(ctx, self.width, self.height, ((ConnectScreenAccessor)self).turnbattle$getStatus(), -1.0F);
         ci.cancel();
      }
   }
}
