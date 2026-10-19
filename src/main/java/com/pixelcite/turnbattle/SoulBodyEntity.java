package com.pixelcite.turnbattle;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker.Builder;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

public class SoulBodyEntity extends PathAwareEntity {
   private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(SoulBodyEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
   private static final TrackedData<String> OWNER_NAME = DataTracker.registerData(SoulBodyEntity.class, TrackedDataHandlerRegistry.STRING);

   public SoulBodyEntity(EntityType<? extends SoulBodyEntity> type, World world) {
      super(type, world);
      this.setAiDisabled(true);
      this.setPersistent();
      this.setSilent(true);
      this.experiencePoints = 0;
   }

   protected void initDataTracker(Builder builder) {
      super.initDataTracker(builder);
      builder.add(OWNER, Optional.empty());
      builder.add(OWNER_NAME, "");
   }

   public void setOwner(UUID owner, String name) {
      this.dataTracker.set(OWNER, Optional.ofNullable(owner));
      this.dataTracker.set(OWNER_NAME, name == null ? "" : name);
   }

   public UUID getOwner() {
      return (UUID)((Optional)this.dataTracker.get(OWNER)).orElse(null);
   }

   public String getOwnerName() {
      return (String)this.dataTracker.get(OWNER_NAME);
   }

   public boolean isPushable() {
      return false;
   }

   protected void pushAway(Entity entity) {
   }

   public boolean isCollidable() {
      return false;
   }

   public boolean cannotDespawn() {
      return true;
   }

   public boolean canImmediatelyDespawn(double distanceSquared) {
      return false;
   }

   public boolean isFireImmune() {
      return true;
   }

   public boolean isInvulnerable() {
      return true;
   }

   protected float getDropChance(EquipmentSlot slot) {
      return 0.0F;
   }

   public boolean damage(DamageSource source, float amount) {
      return source.getTypeRegistryEntry().matchesKey(DamageTypes.OUT_OF_WORLD) && super.damage(source, amount);
   }

   public void tick() {
      super.tick();
      if (!this.getWorld().isClient && this.age > 2 && !SoulWalk.bodyWanted(this)) {
         this.discard();
      }
   }
}
