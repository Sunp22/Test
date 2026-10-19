package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LoadingScreenRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.server.WorldGenerationProgressTracker;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelLoadingScreen.class})
public abstract class LevelLoadingScreenMixin {
   @Shadow
   @Final
   private WorldGenerationProgressTracker field_17406;

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$loading(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen self = (Screen)this;
      float p = this.field_17406 != null ? (float)Math.max(0, Math.min(100, this.field_17406.getProgressPercentage())) / 100.0F : -1.0F;
      LoadingScreenRenderer.render(ctx, self.width, self.height, Text.literal("Preparing spawn area"), p);
      ci.cancel();
   }
}
