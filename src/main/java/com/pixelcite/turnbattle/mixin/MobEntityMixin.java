package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleManager;
import com.pixelcite.turnbattle.RogueliteManager;
import com.pixelcite.turnbattle.TurnBattleMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MobEntity.class})
public abstract class MobEntityMixin {
   @Shadow
   protected GoalSelector field_6201;
   @Shadow
   protected GoalSelector field_6185;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnBattle$tick(CallbackInfo ci) {
      MobEntity self = (MobEntity)this;
      if (BattleManager.frozenEntityIds.contains(self.getId())) {
         self.setVelocity(0.0, 0.0, 0.0);
      }
   }

   @Inject(
      method = {"isAffectedByDaylight()Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnBattle$noSunBurn(CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(false);
   }

   @Inject(
      method = {"mobTick"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnBattle$mobTick(CallbackInfo ci) {
      MobEntity self = (MobEntity)this;
      if (BattleManager.frozenEntityIds.contains(self.getId())) {
         ci.cancel();
      } else {
         MemoryModuleType mem = MemoryModuleType.ATTACK_TARGET;
         if ((self.getBrain().hasMemoryModule(mem) ? (LivingEntity)self.getBrain().getOptionalRegisteredMemory(mem).orElse(null) : null) instanceof ServerPlayerEntity sp
            && turnBattle$offLimits(sp)) {
            turnBattle$forget(self, sp);
         }

         if (self.getTarget() instanceof ServerPlayerEntity sp && turnBattle$offLimits(sp)) {
            turnBattle$forget(self, sp);
         }
      }
   }

   @ModifyVariable(
      method = {"setTarget"},
      at = @At("HEAD"),
      argsOnly = true,
      require = 0
   )
   private LivingEntity turnBattle$noSealTarget(LivingEntity target) {
      if (target instanceof ServerPlayerEntity sp && turnBattle$offLimits(sp)) {
         return null;
      }

      return target;
   }

   private static boolean turnBattle$offLimits(ServerPlayerEntity sp) {
      return TurnBattleMod.isUntouchable(sp.getUuid())
         || BattleManager.activeBattles.containsKey(sp.getUuid())
         || sp.isInvulnerable()
         || RogueliteManager.isPickingCard(sp);
   }

   private static void turnBattle$forget(MobEntity self, ServerPlayerEntity sp) {
      self.setTarget(null);
      Brain brain = self.getBrain();
      brain.forget(MemoryModuleType.ATTACK_TARGET);
      if (brain.hasMemoryModuleWithValue(MemoryModuleType.ANGRY_AT, sp.getUuid())) {
         brain.forget(MemoryModuleType.ANGRY_AT);
      }

      if (self instanceof Angerable a && sp.getUuid().equals(a.getAngryAt())) {
         a.stopAnger();
      }

      if (self.getAttacker() == sp) {
         self.setAttacker(null);
      }
   }
}
