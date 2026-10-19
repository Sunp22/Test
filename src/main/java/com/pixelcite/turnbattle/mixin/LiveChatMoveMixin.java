package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LiveCombatClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatHud.class})
public abstract class LiveChatMoveMixin {
   @Unique
   private int turnbattle$pushed;

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"},
      at = {@At("HEAD")}
   )
   private void turnbattle$moveUp(DrawContext ctx, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
      this.turnbattle$pushed = 0;
      if (!focused && LiveCombatClient.chatMoved()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null) {
            double scale = (Double)mc.options.getChatScale().getValue();
            int width = ChatHud.getWidth((Double)mc.options.getChatWidth().getValue());
            int height = ChatHud.getHeight((Double)mc.options.getChatHeightUnfocused().getValue());
            int sw = ctx.getScaledWindowWidth();
            int sh = ctx.getScaledWindowHeight();
            float visW = (float)((double)width + 12.0 * scale);
            float visH = (float)((double)height * scale);
            int top = mc.player.getStatusEffects().isEmpty() ? 4 : 54;
            float dx = (float)sw - visW - 4.0F;
            float dy = (float)top + visH - (float)(sh - 40);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(dx, dy, 0.0F);
            this.turnbattle$pushed = 1;
         }
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"},
      at = {@At("RETURN")}
   )
   private void turnbattle$moveBack(DrawContext ctx, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
      if (this.turnbattle$pushed == 1) {
         ctx.getMatrices().pop();
         this.turnbattle$pushed = 0;
      }
   }
}
