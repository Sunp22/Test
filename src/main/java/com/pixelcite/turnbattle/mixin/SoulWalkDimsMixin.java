package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.SoulWalk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class SoulWalkDimsMixin {
   @Inject(
      method = {"getDimensions"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$soulSize(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
      if (this instanceof PlayerEntity p && SoulWalk.isOut(p)) {
         cir.setReturnValue(EntityDimensions.changing(0.5F, 0.5F).withEyeHeight(0.25F));
      }
   }
}
