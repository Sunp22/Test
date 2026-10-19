package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.TurnBattleClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class ClimbMovementMixin {
   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$cancelMovementWhileClimbing(Vec3d movementInput, CallbackInfo ci) {
      if (TurnBattleClientMod.isWallClimbing && !TurnBattleClientMod.isClimbStepping && this == MinecraftClient.getInstance().player) {
         ci.cancel();
      }
   }

   @ModifyVariable(
      method = {"travel"},
      at = @At("HEAD"),
      ordinal = 0
   )
   private Vec3d turnbattle$zeroMovementInputDuringClimbStep(Vec3d movementInput) {
      return TurnBattleClientMod.isWallClimbing && TurnBattleClientMod.isClimbStepping && this == MinecraftClient.getInstance().player
         ? Vec3d.ZERO
         : movementInput;
   }
}
