package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.LiveCombatClient;
import com.pixelcite.turnbattle.PlayerActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class ActionMoveLockMixin {
   @ModifyVariable(
      method = {"travel"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private Vec3d turnbattle$noInputDuringAction(Vec3d movementInput) {
      return this != MinecraftClient.getInstance().player || !PlayerActions.localLocked() && !LiveCombatClient.moveLocked() ? movementInput : Vec3d.ZERO;
   }

   @Inject(
      method = {"jump()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$noJumpDuringAction(CallbackInfo ci) {
      if (this == MinecraftClient.getInstance().player && (PlayerActions.localLocked() || LiveCombatClient.moveLocked())) {
         ci.cancel();
      }
   }
}
