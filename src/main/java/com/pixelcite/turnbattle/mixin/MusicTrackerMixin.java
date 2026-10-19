package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MusicTracker.class})
public class MusicTrackerMixin {
   @Shadow
   private SoundInstance field_5574;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void suppressDuringBattle(CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen instanceof BattleScreen) {
         if (this.field_5574 != null) {
            mc.getSoundManager().stop(this.field_5574);
            this.field_5574 = null;
         }

         ci.cancel();
      }
   }
}
