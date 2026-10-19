package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.MobLevelManager;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MobEntity.class})
public abstract class MobNbtMixin {
   @Inject(
      method = {"writeCustomDataToNbt"},
      at = {@At("TAIL")}
   )
   private void turnBattle$writeNbt(NbtCompound nbt, CallbackInfo ci) {
      MobEntity self = (MobEntity)this;
      int level = MobLevelManager.getStoredLevel(self);
      if (level > 0) {
         nbt.putInt("turnbattle_level", level);
      }
   }

   @Inject(
      method = {"readCustomDataFromNbt"},
      at = {@At("TAIL")}
   )
   private void turnBattle$readNbt(NbtCompound nbt, CallbackInfo ci) {
      MobEntity self = (MobEntity)this;
      if (nbt.contains("turnbattle_level")) {
         MobLevelManager.restoreLevel(self, nbt.getInt("turnbattle_level"));
      }
   }
}
