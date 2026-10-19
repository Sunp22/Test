package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LiveCombatClient;
import com.pixelcite.turnbattle.SoulWalk;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public class InGameHudMixin {
   @Inject(
      method = {"renderHealthBar"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void hideHealthBar(CallbackInfo ci) {
      ci.cancel();
   }

   @Inject(
      method = {"renderStatusBars"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void hideStatusBars(CallbackInfo ci) {
      ci.cancel();
   }

   @Inject(
      method = {"renderHotbar"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$liveHideHotbar(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
      if (LiveCombatClient.hideHotbar() || SoulWalk.localOut) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderExperienceBar"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$liveHideXpBar(DrawContext ctx, int x, CallbackInfo ci) {
      if (LiveCombatClient.hideHotbar() || SoulWalk.localOut) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderExperienceLevel"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$liveHideXpLevel(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
      if (LiveCombatClient.hideHotbar() || SoulWalk.localOut) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderHeldItemTooltip"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$liveHideItemName(DrawContext ctx, CallbackInfo ci) {
      if (LiveCombatClient.hideHotbar() || SoulWalk.localOut) {
         ci.cancel();
      }
   }
}
