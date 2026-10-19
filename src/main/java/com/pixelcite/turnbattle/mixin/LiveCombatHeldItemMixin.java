package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.HandSwaps;
import com.pixelcite.turnbattle.LiveCombatClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public abstract class LiveCombatHeldItemMixin {
   @Inject(
      method = {"updateHeldItems()V"},
      at = {@At("HEAD")}
   )
   private void turnbattle$armBefore(CallbackInfo ci) {
      ClientPlayerEntity p = MinecraftClient.getInstance().player;
      if (p != null) {
         ItemStack w = LiveCombatClient.armedWeapon(p);
         if (w != null) {
            HandSwaps.showMain(p, w);
         }
      }
   }

   @Inject(
      method = {"updateHeldItems()V"},
      at = {@At("RETURN")}
   )
   private void turnbattle$armAfter(CallbackInfo ci) {
      ClientPlayerEntity p = MinecraftClient.getInstance().player;
      if (p != null) {
         HandSwaps.restoreMain(p);
      }
   }
}
