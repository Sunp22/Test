package com.pixelcite.turnbattle;

import java.util.UUID;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class HumanEnemyEntity extends HostileEntity {
   private final HumanEnemies.Trait trait;
   private static final int ANGER_TICKS = 1200;
   @Nullable
   private UUID provoker = null;
   private int angryUntil = 0;

   public HumanEnemyEntity(EntityType<? extends HumanEnemyEntity> type, World world, HumanEnemies.Trait trait) {
      super(type, world);
      this.trait = trait;
      this.experiencePoints = 8;
   }

   public HumanEnemies.Trait trait() {
      return this.trait;
   }

   public static Builder createAttributes() {
      return HostileEntity.createHostileAttributes()
         .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
         .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
         .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
         .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
   }

   protected void initGoals() {
      this.goalSelector.add(0, new SwimGoal(this));
      this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
      this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
      this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
      this.goalSelector.add(7, new LookAroundGoal(this));
      this.targetSelector.add(1, new ActiveTargetGoal(this, PlayerEntity.class, 10, true, false, target -> this.isAngryAt(target)));
   }

   public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
      EntityData d = super.initialize(world, difficulty, spawnReason, entityData);
      this.equipWeapon();
      return d;
   }

   private void equipWeapon() {
      Item w = HumanEnemies.weaponItem(this.trait);
      if (w != null) {
         this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(w));
         this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
      }
   }

   public boolean damage(DamageSource source, float amount) {
      if (!this.getWorld().isClient && source.getAttacker() instanceof PlayerEntity p && !p.isCreative() && !p.isSpectator()) {
         this.provoker = p.getUuid();
         this.angryUntil = this.age + 1200;
      }

      return super.damage(source, amount);
   }

   public void provoke(PlayerEntity p) {
      if (!this.getWorld().isClient && !p.isCreative() && !p.isSpectator()) {
         this.provoker = p.getUuid();
         this.angryUntil = this.age + 1200;
         if (this.getTarget() == null) {
            this.setTarget(p);
         }
      }
   }

   private boolean isAngryAt(LivingEntity e) {
      return this.provoker != null && this.age < this.angryUntil && this.provoker.equals(e.getUuid());
   }

   public void tick() {
      super.tick();
      if (!this.getWorld().isClient && this.provoker != null && this.age % 20 == 0) {
         LivingEntity t = this.getTarget();
         if (this.age >= this.angryUntil) {
            this.provoker = null;
            if (t instanceof PlayerEntity) {
               this.setTarget(null);
            }
         } else if (t != null && !this.isAngryAt(t)) {
            this.setTarget(null);
         }
      }

      if (!this.getWorld().isClient && this.age % 40 == 0 && this.getMainHandStack().isEmpty()) {
         this.equipWeapon();
      }
   }

   public float getPathfindingFavor(BlockPos pos, WorldView world) {
      return 0.0F;
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      if (this.provoker != null && this.age < this.angryUntil) {
         nbt.putUuid("AngryAt", this.provoker);
         nbt.putInt("AngryTicks", this.angryUntil - this.age);
      }
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      if (nbt.containsUuid("AngryAt")) {
         this.provoker = nbt.getUuid("AngryAt");
         this.angryUntil = this.age + nbt.getInt("AngryTicks");
      }
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_PLAYER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_PLAYER_DEATH;
   }
}
