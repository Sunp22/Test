package com.pixelcite.turnbattle;

import java.util.Random;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class DarknerEntity extends HostileEntity {
   private static final TrackedData<ItemStack> ITEM = DataTracker.registerData(DarknerEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
   private static final TrackedData<Integer> SEED = DataTracker.registerData(DarknerEntity.class, TrackedDataHandlerRegistry.INTEGER);

   public DarknerEntity(EntityType<? extends DarknerEntity> type, World world) {
      super(type, world);
      this.experiencePoints = 6;
   }

   public static Builder createAttributes() {
      return HostileEntity.createHostileAttributes()
         .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
         .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
         .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
         .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
   }

   protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
      super.initDataTracker(builder);
      builder.add(ITEM, ItemStack.EMPTY);
      builder.add(SEED, 0);
   }

   protected void initGoals() {
      this.goalSelector.add(0, new SwimGoal(this));
      this.goalSelector.add(2, new MeleeAttackGoal(this, 1.05, false));
      this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
      this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
      this.goalSelector.add(7, new LookAroundGoal(this));
      this.targetSelector.add(1, new RevengeGoal(this, new Class[0]));
      this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true));
   }

   public ItemStack item() {
      return (ItemStack)this.dataTracker.get(ITEM);
   }

   public int seed() {
      return (Integer)this.dataTracker.get(SEED);
   }

   public String itemId() {
      return this.item().isEmpty() ? "" : Registries.ITEM.getId(this.item().getItem()).toString();
   }

   public void setMakeup(ItemStack item, int seed) {
      this.dataTracker.set(ITEM, item.isEmpty() ? ItemStack.EMPTY : new ItemStack(item.getItem()));
      this.dataTracker.set(SEED, seed);
   }

   public Text getName() {
      return (Text)(this.hasCustomName() ? super.getName() : Text.literal(Darkners.nameFor(this.item(), this.seed())));
   }

   public int pickPattern(Random rng) {
      int[] p = Darkners.patternsFor(this.item(), this.seed());
      return p[rng.nextInt(p.length)];
   }

   public MobProfile.Profile profile() {
      Random r = new Random((long)this.seed() * 13L + 5L);
      float hp = 0.9F + r.nextFloat() * 0.4F;
      float atk = 0.9F + r.nextFloat() * 0.25F;
      float speed = 0.95F + r.nextFloat() * 0.2F;
      String from = this.item().isEmpty() ? "nothing at all" : this.item().getName().getString();
      return new MobProfile.Profile(hp, atk, speed, 1.0F, 1.0F, 1.1F, "Darkner: born from " + from + ", left in a chest in the room this Dark World grew from.");
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      if (!this.item().isEmpty()) {
         nbt.put("DarknerItem", this.item().encode(this.getWorld().getRegistryManager()));
      }

      nbt.putInt("DarknerSeed", this.seed());
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      ItemStack s = nbt.contains("DarknerItem")
         ? ItemStack.fromNbt(this.getWorld().getRegistryManager(), nbt.getCompound("DarknerItem")).orElse(ItemStack.EMPTY)
         : ItemStack.EMPTY;
      this.setMakeup(s, nbt.getInt("DarknerSeed"));
   }

   public float getPathfindingFavor(BlockPos pos, WorldView world) {
      return 0.0F;
   }
}
