package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkWorldFall;
import com.pixelcite.turnbattle.LightPillarClient;
import com.pixelcite.turnbattle.LoadingScreenRenderer;
import com.pixelcite.turnbattle.MagicantClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DownloadingTerrainScreen.class})
public abstract class DownloadingTerrainScreenMixin {
   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$magicantBlack(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!MagicantClient.blackout()) {
         if (LightPillarClient.renderLoading(ctx)) {
            ci.cancel();
         } else if (DarkWorldFall.renderLoading(ctx)) {
            ci.cancel();
         } else {
            LoadingScreenRenderer.render(
               ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), Text.translatable("multiplayer.downloadingTerrain"), -1.0F
            );
            ci.cancel();
         }
      } else {
         ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), -16777216);
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$magicantBlackBg(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!MagicantClient.blackout()) {
         ci.cancel();
      } else {
         ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), -16777216);
         ci.cancel();
      }
   }
}
