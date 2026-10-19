package com.pixelcite.turnbattle.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MobEntity.class})
public interface MobEntityPersistAccessor {
   @Accessor("persistent")
   void setPersistentFlag(boolean var1);

   @Accessor("persistent")
   boolean getPersistentFlag();
}
