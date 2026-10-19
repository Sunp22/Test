package com.pixelcite.turnbattle.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.SpawnRestriction.SpawnPredicate;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({SpawnRestriction.class})
public interface SpawnRestrictionInvoker {
   @Invoker("register")
   static <T extends MobEntity> void turnbattle$invokeRegister(EntityType<T> type, SpawnLocation location, Type heightmapType, SpawnPredicate<T> predicate) {
      throw new AssertionError();
   }
}
