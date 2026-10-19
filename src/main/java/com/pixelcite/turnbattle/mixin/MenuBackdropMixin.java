package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.MenuBackdrop;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public abstract class MenuBackdropMixin {
   @Inject(
      method = {"renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$backdrop(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen self = (Screen)this;
      if (self instanceof CreateWorldScreen && MinecraftClient.getInstance().world == null) {
         MenuBackdrop.render(ctx, self.width, self.height);
         ci.cancel();
      }
   }
}
