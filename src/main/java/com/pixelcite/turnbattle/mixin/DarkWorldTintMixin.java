package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkFountainClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class DarkWorldTintMixin {
   @Inject(
      method = {"onCameraEntitySet(Lnet/minecraft/entity/Entity;)V"},
      at = {@At("TAIL")}
   )
   private void turnbattle$keepDarkWorldTint(Entity entity, CallbackInfo ci) {
      DarkFountainClient.ensureTint(MinecraftClient.getInstance());
   }
}
