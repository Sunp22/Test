package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LoadingScreenRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ProgressScreen.class})
public abstract class ProgressScreenMixin {
   @Shadow
   private Text field_2541;
   @Shadow
   private Text field_2544;
   @Shadow
   private int field_2542;
   @Shadow
   private boolean field_2543;

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$loading(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!this.field_2543) {
         Screen self = (Screen)this;
         String msg = this.field_2541 != null ? this.field_2541.getString() : "";
         if (this.field_2544 != null && !this.field_2544.getString().isEmpty()) {
            msg = msg.isEmpty() ? this.field_2544.getString() : msg + ", " + this.field_2544.getString();
         }

         LoadingScreenRenderer.render(ctx, self.width, self.height, Text.literal(msg), this.field_2542 > 0 ? (float)this.field_2542 / 100.0F : -1.0F);
         ci.cancel();
      }
   }
}
