package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.OriRender;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderDispatcher.class})
public abstract class OriRendererMixin {
   @Inject(
      method = {"getRenderer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <T extends Entity> void turnbattle$oriModel(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
      EntityRenderer r = OriRender.rendererFor(entity);
      if (r != null) {
         cir.setReturnValue(r);
      }
   }
}
