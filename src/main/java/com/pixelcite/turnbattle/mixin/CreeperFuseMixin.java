package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleManager;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CreeperEntity.class})
public abstract class CreeperFuseMixin {
   @Shadow
   private int field_7227;
   @Shadow
   private int field_7229;
   @Shadow
   @Final
   private static TrackedData<Boolean> field_7231;

   @Shadow
   public abstract void method_7005(int var1);

   @Inject(
      method = {"tick()V"},
      at = {@At("HEAD")}
   )
   private void turnbattle$noFuseInBattle(CallbackInfo ci) {
      CreeperEntity self = (CreeperEntity)this;
      if (!self.getWorld().isClient() && BattleManager.frozenEntityIds.contains(self.getId())) {
         this.field_7227 = 0;
         this.field_7229 = 0;
         this.method_7005(-1);
         if (self.isIgnited()) {
            self.getDataTracker().set(field_7231, false);
         }
      }
   }
}
