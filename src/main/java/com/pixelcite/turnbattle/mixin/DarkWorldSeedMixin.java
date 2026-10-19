package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkWorldGenerator;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fantasy let each runtime world carry its own seed. Infiniverse levels always report the server seed, so give
 * Dark Worlds their per-instance seed back (terrain noise, structures, etc. all read ServerWorld#getSeed).
 */
@Mixin({ServerWorld.class})
public abstract class DarkWorldSeedMixin {
   @Inject(
      method = {"getSeed"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void turnbattle$darkWorldSeed(CallbackInfoReturnable<Long> cir) {
      Long seed = DarkWorldGenerator.seedFor(((ServerWorld)(Object)this).getRegistryKey());
      if (seed != null) {
         cir.setReturnValue(seed);
      }
   }
}
