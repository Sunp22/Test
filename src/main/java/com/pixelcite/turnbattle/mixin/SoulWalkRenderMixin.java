package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.SoulWalk;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderDispatcher.class})
public abstract class SoulWalkRenderMixin {
   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$hideSoulWalker(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (entity instanceof PlayerEntity p && SoulWalk.CLIENT_OUT.containsKey(p.getUuid())) {
         cir.setReturnValue(false);
      }
   }
}
