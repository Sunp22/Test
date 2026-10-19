package com.pixelcite.turnbattle;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.world.World;

public class MirrorCloneEntity extends HostileEntity {
   private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(MirrorCloneEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
   private static final TrackedData<String> OWNER_NAME = DataTracker.registerData(MirrorCloneEntity.class, TrackedDataHandlerRegistry.STRING);

   public MirrorCloneEntity(EntityType<? extends MirrorCloneEntity> type, World world) {
      super(type, world);
      this.setAiDisabled(true);
      this.experiencePoints = 0;
   }

   public static Builder createAttributes() {
      return HostileEntity.createHostileAttributes()
         .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
         .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
         .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
   }

   protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
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

   public String mobName() {
      String n = this.getOwnerName();
      return n != null && !n.isEmpty() ? n : "???";
   }

   public boolean isPushable() {
      return false;
   }

   protected boolean isDisallowedInPeaceful() {
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

   public boolean damage(DamageSource source, float amount) {
      return !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) ? false : super.damage(source, amount);
   }

   public void tick() {
      super.tick();
      if (!this.getWorld().isClient && this.age > 40 && !MagicantManager.cloneStillWanted(this)) {
         this.discard();
      }
   }
}
