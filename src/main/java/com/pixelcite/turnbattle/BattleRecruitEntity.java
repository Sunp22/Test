package com.pixelcite.turnbattle;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker.Builder;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class BattleRecruitEntity extends PathAwareEntity {
   private static final TrackedData<String> RECRUIT_NAME = DataTracker.registerData(BattleRecruitEntity.class, TrackedDataHandlerRegistry.STRING);
   private static final TrackedData<Integer> SLOT = DataTracker.registerData(BattleRecruitEntity.class, TrackedDataHandlerRegistry.INTEGER);
   private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(BattleRecruitEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

   public BattleRecruitEntity(EntityType<? extends PathAwareEntity> type, World world) {
      super(type, world);
      this.setAiDisabled(true);
      this.setInvulnerable(true);
   }

   protected void initDataTracker(Builder builder) {
      super.initDataTracker(builder);
      builder.add(RECRUIT_NAME, "");
      builder.add(SLOT, 1);
      builder.add(OWNER, Optional.empty());
   }

   public void setRecruit(String name, int slot, UUID owner) {
      this.getDataTracker().set(RECRUIT_NAME, name == null ? "" : name);
      this.getDataTracker().set(SLOT, slot);
      this.getDataTracker().set(OWNER, Optional.ofNullable(owner));
   }

   public String getRecruitName() {
      return (String)this.getDataTracker().get(RECRUIT_NAME);
   }

   public int getSlot() {
      return (Integer)this.getDataTracker().get(SLOT);
   }

   public UUID getOwner() {
      return (UUID)((Optional)this.getDataTracker().get(OWNER)).orElse(null);
   }

   public void tick() {
      super.tick();
      if (!this.getWorld().isClient()) {
         UUID owner = this.getOwner();
         if (owner == null || !BattleManager.isInBattle(owner)) {
            this.discard();
         }
      }
   }

   public boolean isInvulnerableTo(DamageSource source) {
      return true;
   }

   public boolean damage(DamageSource source, float amount) {
      return false;
   }

   public boolean canHit() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   public boolean isCollidable() {
      return false;
   }

   protected void pushAway(Entity entity) {
   }

   public boolean cannotDespawn() {
      return true;
   }

   public void checkDespawn() {
   }

   public boolean shouldSave() {
      return false;
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
   }
}
