package com.pixelcite.turnbattle.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ConnectScreen.class})
public abstract class ConnectScreenMixin {
   @Redirect(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"
      ),
      require = 0
   )
   private void turnbattle$noStatus(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color) {
   }
}
