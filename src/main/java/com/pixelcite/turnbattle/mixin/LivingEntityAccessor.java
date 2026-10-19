package com.pixelcite.turnbattle.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Accessor("hurtTime")
   void setHurtTime(int var1);

   @Accessor("hurtTime")
   int getHurtTime();
}
