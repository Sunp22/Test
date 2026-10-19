package com.pixelcite.turnbattle;

import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class DarknerBossEntity extends DarknerEntity {
   private static final TrackedData<Integer> TIER = DataTracker.registerData(DarknerBossEntity.class, TrackedDataHandlerRegistry.INTEGER);
   int bossLevel = 0;

   public DarknerBossEntity(EntityType<? extends DarknerBossEntity> type, World world) {
      super(type, world);
      this.experiencePoints = 30;
      this.setPersistent();
   }

   public static Builder createAttributes() {
      return HostileEntity.createHostileAttributes()
         .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
         .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
         .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
         .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
         .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0);
   }

   @Override
   protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
      super.initDataTracker(builder);
      builder.add(TIER, 1);
   }

   @Override
   protected void initGoals() {
      this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
      this.goalSelector.add(7, new LookAroundGoal(this));
   }

   public int tier() {
      return (Integer)this.dataTracker.get(TIER);
   }

   public void setTier(int t) {
      this.dataTracker.set(TIER, Math.max(1, Math.min(3, t)));
   }

   public int personality() {
      return personalityOf(this.seed());
   }

   static int personalityOf(int seed) {
      return Math.floorMod(new Random((long)seed * 7919L + 1L).nextInt(), 8);
   }

   public int dream() {
      return Math.floorMod(new Random((long)this.seed() * 104729L + 2L).nextInt(), DarknerBosses.DREAMS.length);
   }

   public int reason() {
      return Math.floorMod(new Random((long)this.seed() * 1299709L + 3L).nextInt(), DarknerBosses.REASONS.length);
   }

   @Override
   public Text getName() {
      return (Text)(this.hasCustomName() ? super.getName() : Text.literal(DarknerBosses.fullName(this)));
   }

   @Override
   public int pickPattern(Random rng) {
      int[] p = DarknerBosses.patternsFor(this);
      return p[rng.nextInt(p.length)];
   }

   @Override
   public MobProfile.Profile profile() {
      Random r = new Random((long)this.seed() * 31L + 9L);
      float hp = 1.3F + r.nextFloat() * 0.3F;
      float atk = 1.0F + r.nextFloat() * 0.2F;
      float speed = 1.0F + r.nextFloat() * 0.1F;
      return new MobProfile.Profile(
         hp,
         atk,
         speed,
         1.1F,
         0.95F,
         1.6F,
         DarknerBosses.PERSONALITY_NAMES[this.personality()]
            + " guardian of the Fountain, born from "
            + (this.item().isEmpty() ? "nothing" : this.item().getName().getString())
            + "."
      );
   }

   protected ActionResult interactMob(PlayerEntity player, Hand hand) {
      if (hand != Hand.MAIN_HAND) {
         return ActionResult.PASS;
      } else {
         if (!this.getWorld().isClient() && player instanceof ServerPlayerEntity sp) {
            DarknerBosses.talk(sp, this);
         }

         return ActionResult.SUCCESS;
      }
   }

   public boolean damage(DamageSource source, float amount) {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   protected void pushAway(Entity entity) {
   }

   public void checkDespawn() {
   }

   public void tick() {
      super.tick();
      this.setVelocity(0.0, this.getVelocity().y, 0.0);
      if (this.getWorld() instanceof ServerWorld sw && this.age % 20 == 7) {
         DarknerBosses.checkStillGuardian(sw, this);
      }
   }

   @Override
   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      nbt.putInt("BossTier", this.tier());
      nbt.putInt("BossLevel", this.bossLevel);
   }

   @Override
   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      this.setTier(nbt.contains("BossTier") ? nbt.getInt("BossTier") : 1);
      this.bossLevel = nbt.getInt("BossLevel");
   }
}
