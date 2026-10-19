package com.pixelcite.turnbattle;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker.Builder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class SavePointEntity extends Entity {
   private static final int DESPAWN_SECONDS = 300;
   private static final TrackedData<String> CUSTOM_POINT_NAME = DataTracker.registerData(SavePointEntity.class, TrackedDataHandlerRegistry.STRING);
   private int unusedSeconds = 0;
   private boolean everUsed = false;

   public SavePointEntity(EntityType<?> type, World world) {
      super(type, world);
      this.setInvulnerable(true);
      this.noClip = true;
      this.ignoreCameraFrustum = true;
   }

   public String getPointName() {
      return (String)this.getDataTracker().get(CUSTOM_POINT_NAME);
   }

   public void setPointName(String name) {
      this.getDataTracker().set(CUSTOM_POINT_NAME, name == null ? "" : name);
   }

   public void tick() {
      super.tick();
      if (!this.getWorld().isClient() && !this.everUsed && this.age % 20 == 0) {
         this.unusedSeconds++;
         if (this.unusedSeconds >= 300) {
            this.discard();
         }
      }
   }

   public void markUsed() {
      this.everUsed = true;
      this.unusedSeconds = 0;
   }

   public boolean isInvulnerable() {
      return true;
   }

   public boolean isInvulnerableTo(DamageSource s) {
      return true;
   }

   public boolean canHit() {
      return false;
   }

   public boolean isCollidable() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   public boolean shouldRender(double d) {
      return true;
   }

   public boolean doesRenderOnFire() {
      return false;
   }

   public boolean isImmuneToExplosion(Explosion e) {
      return true;
   }

   protected void initDataTracker(Builder b) {
      b.add(CUSTOM_POINT_NAME, "");
   }

   protected void writeCustomDataToNbt(NbtCompound nbt) {
      nbt.putBoolean("EverUsed", this.everUsed);
      nbt.putInt("UnusedSeconds", this.unusedSeconds);
      nbt.putString("PointName", this.getPointName());
   }

   protected void readCustomDataFromNbt(NbtCompound nbt) {
      this.everUsed = nbt.getBoolean("EverUsed");
      if (nbt.contains("UnusedSeconds")) {
         this.unusedSeconds = nbt.getInt("UnusedSeconds");
      } else if (nbt.contains("UnusedTicks")) {
         this.unusedSeconds = nbt.getInt("UnusedTicks") / 20;
      }

      if (nbt.contains("PointName")) {
         this.setPointName(nbt.getString("PointName"));
      }
   }

   public ActionResult interact(PlayerEntity player, Hand hand) {
      if (!this.getWorld().isClient() && player instanceof ServerPlayerEntity sp) {
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(this.getId());
         Net121.sendToClient(sp, TurnBattleMod.SAVE_POINT_OPEN, buf);
      }

      return ActionResult.SUCCESS;
   }
}
