package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

final class BattleStaging {
   private static final double ENEMY_DIST = 5.5;
   private static final double ENEMY_BACK_ROW = 1.0;
   private static final double ENEMY_SPREAD = 2.6;
   private static final double PARTY_SPACING = 1.7;
   private static final double SPECTATOR_RANGE = 30.0;
   static final int EV_BASH = 0;
   static final int EV_SKILL = 1;
   static final int EV_ITEM = 2;
   static final int EV_DEFEND = 3;
   static final int EV_SPARE = 4;
   static final int EV_ACT = 6;

   private BattleStaging() {
   }

   static boolean wantsOverhaul(BattleState state, BattleData.PlayerProgress prog) {
      return state.forceOverhaul
         && (state.battleStyle == BattleStyle.BULLET_HELL || state.battleStyle == BattleStyle.DIRECT_DAMAGE)
         && !state.isPvp
         && !state.isVirtualEnemy
         && !state.depthsRun;
   }

   static boolean overhaulFor(BattleState state, BattleData.PlayerProgress prog) {
      return state.staged && wantsOverhaul(state, prog);
   }

   private static float yawToward(Vec3d from, Vec3d to) {
      return (float)(MathHelper.atan2(-(to.x - from.x), to.z - from.z) * (180.0 / Math.PI));
   }

   static void stage(BattleState state, ServerPlayerEntity initiator) {
      BattleData data = BattleData.get(initiator.getServer());
      if (wantsOverhaul(state, data.getOrCreate(initiator.getUuid()))) {
         ServerWorld world = initiator.getServerWorld();
         Vec3d origin = initiator.getPos();
         LivingEntity primary = BattleManager.findEnemyByIndex(state, world, 0);
         Vec3d fwd = primary != null ? primary.getPos().subtract(origin) : Vec3d.ZERO;
         fwd = new Vec3d(fwd.x, 0.0, fwd.z);
         if (fwd.lengthSquared() < 0.04) {
            float yaw = initiator.getYaw() * (float) (Math.PI / 180.0);
            fwd = new Vec3d((double)(-MathHelper.sin(yaw)), 0.0, (double)MathHelper.cos(yaw));
         }

         fwd = fwd.normalize();
         Vec3d right = new Vec3d(-fwd.z, 0.0, fwd.x);
         state.staged = true;
         state.stageOrigin = origin;
         state.stageForward = fwd;
         state.stageWorld = world.getRegistryKey();
         List enemyIds = new ArrayList<>();
         enemyIds.add(state.primaryEnemyId);
         enemyIds.addAll(state.extraEnemyIds);
         Vec3d partyEye = origin.add(0.0, (double)initiator.getStandingEyeHeight(), 0.0);

         for (int k = 0; k < enemyIds.size(); k++) {
            Entity var12 = world.getEntityById(enemyIds.get(k));
            if (var12 instanceof LivingEntity) {
               LivingEntity e = (LivingEntity)var12;
               if (e.isAlive()) {
                  double side = k == 0 ? 0.0 : (double)(k % 2 == 1 ? 1 : -1) * 2.6 * (double)((k + 1) / 2);
                  double depth = 5.5 + (k == 0 ? 0.0 : 1.0);
                  Vec3d want = origin.add(fwd.multiply(depth)).add(right.multiply(side));
                  Vec3d spot = movable(e) ? findSpot(world, want, e, partyEye, 3) : null;
                  if (spot != null) {
                     float yaw = yawToward(spot, origin);
                     e.refreshPositionAndAngles(spot.x, spot.y, spot.z, yaw, 0.0F);
                     e.setVelocity(Vec3d.ZERO);
                  }

                  face(e, origin);
               }
            }
         }

         lineUp(state, initiator.getServer(), world, data);
      }
   }

   private static boolean movable(LivingEntity e) {
      if (!(e instanceof MobEntity)) {
         return false;
      } else if (e.getWidth() > 1.4F || e.getHeight() > 2.6F) {
         return false;
      } else {
         return !(e instanceof WitherEntity) && !(e instanceof EnderDragonEntity) ? e.isOnGround() : false;
      }
   }

   private static void face(LivingEntity e, Vec3d target) {
      float yaw = yawToward(e.getPos(), target);
      e.setYaw(yaw);
      e.setHeadYaw(yaw);
      e.setBodyYaw(yaw);
   }

   static void lineUp(BattleState state, MinecraftServer server, ServerWorld world, BattleData data) {
      if (state.staged && state.stageOrigin != null) {
         Vec3d origin = state.stageOrigin;
         Vec3d fwd = state.stageForward;
         Vec3d right = new Vec3d(-fwd.z, 0.0, fwd.x);
         Vec3d enemyCenter = origin.add(fwd.multiply(5.5));
         Vec3d originEye = origin.add(0.0, 1.5, 0.0);
         List order = new ArrayList<>(state.participantOrder);
         int n = order.size();
         PlayerManager pm = server.getPlayerManager();

         for (int i = 0; i < n; i++) {
            UUID pid = order.get(i);
            Vec3d want = origin.add(right.multiply(((double)i - (double)(n - 1) / 2.0) * 1.7));
            if (BattleManager.isRecruitSlot(pid)) {
               spawnRecruit(state, world, pid, want, enemyCenter, originEye);
            } else {
               ServerPlayerEntity p = pm.getPlayer(pid);
               if (p != null && p.getServerWorld() == world) {
                  Vec3d spot = n == 1 ? p.getPos() : findSpot(world, want, p, originEye, 1);
                  if (spot == null || spot.squaredDistanceTo(p.getPos()) > 400.0) {
                     spot = p.getPos();
                  }

                  float yaw = yawToward(spot, enemyCenter);
                  p.networkHandler.requestTeleport(spot.x, spot.y, spot.z, yaw, 0.0F);
                  p.setHeadYaw(yaw);
                  p.setBodyYaw(yaw);
               }
            }
         }
      }
   }

   private static void spawnRecruit(BattleState state, ServerWorld world, UUID slotUuid, Vec3d want, Vec3d enemyCenter, Vec3d originEye) {
      Integer existing = state.recruitEntityIds.get(slotUuid);
      if (existing == null || world.getEntityById(existing) == null) {
         if (TurnBattleMod.BATTLE_RECRUIT_ENTITY_TYPE != null) {
            BattleRecruitEntity rec = new BattleRecruitEntity(TurnBattleMod.BATTLE_RECRUIT_ENTITY_TYPE, world);
            Vec3d spot = findSpot(world, want, rec, originEye, 1);
            if (spot == null) {
               spot = findSpot(world, state.stageOrigin.add(state.stageForward.multiply(-1.4)), rec, originEye, 1);
            }

            if (spot != null) {
               float yaw = yawToward(spot, enemyCenter);
               rec.refreshPositionAndAngles(spot.x, spot.y, spot.z, yaw, 0.0F);
               rec.setHeadYaw(yaw);
               rec.setBodyYaw(yaw);
               int slot = slotUuid.equals(BattleState.PARTY_MEMBER_UUID_2) ? 2 : 1;
               rec.setRecruit(state.participantNames.getOrDefault(slotUuid, "Recruit"), slot, state.playerUuid);
               if (world.spawnEntity(rec)) {
                  state.recruitEntityIds.put(slotUuid, rec.getId());
               }
            }
         }
      }
   }

   static void onJoin(BattleState state, ServerPlayerEntity joiner) {
      if (state.staged) {
         despawnRecruits(state, joiner.getServer());
         ServerWorld world = joiner.getServer().getWorld(state.stageWorld);
         if (world != null) {
            lineUp(state, joiner.getServer(), world, BattleData.get(joiner.getServer()));
         }
      }
   }

   static void cleanup(BattleState state, MinecraftServer server) {
      despawnRecruits(state, server);
   }

   private static void despawnRecruits(BattleState state, MinecraftServer server) {
      if (!state.recruitEntityIds.isEmpty()) {
         ServerWorld world = state.stageWorld != null ? server.getWorld(state.stageWorld) : null;

         for (int id : state.recruitEntityIds.values()) {
            if (world != null && world.getEntityById(id) instanceof BattleRecruitEntity r) {
               r.discard();
            }
         }

         state.recruitEntityIds.clear();
      }
   }

   static Vec3d findSpot(ServerWorld world, Vec3d want, Entity e, Vec3d seenFrom, int maxDy) {
      double[][] offsets = new double[][]{{0.0, 0.0}, {0.8, 0.0}, {-0.8, 0.0}, {0.0, 0.8}, {0.0, -0.8}, {1.2, 0.8}, {-1.2, 0.8}, {1.2, -0.8}, {-1.2, -0.8}};
      int[] dys = new int[]{0, 1, -1, 2, -2, 3, -3};

      for (double[] o : offsets) {
         for (int dy : dys) {
            if (Math.abs(dy) <= maxDy) {
               BlockPos feet = BlockPos.ofFloored(want.x + o[0], want.y + (double)dy, want.z + o[1]);
               Vec3d spot = new Vec3d((double)feet.getX() + 0.5, (double)feet.getY(), (double)feet.getZ() + 0.5);
               if (standable(world, feet, e, spot) && (seenFrom == null || clearView(world, seenFrom, spot.add(0.0, (double)e.getHeight() * 0.6, 0.0), e))) {
                  return spot;
               }
            }
         }
      }

      return null;
   }

   private static boolean standable(ServerWorld world, BlockPos feet, Entity e, Vec3d spot) {
      BlockPos below = feet.down();
      BlockState floor = world.getBlockState(below);
      if (!floor.isSideSolidFullSquare(world, below, Direction.UP)) {
         return false;
      } else if (!floor.isOf(Blocks.MAGMA_BLOCK) && !floor.isOf(Blocks.CAMPFIRE) && !floor.isOf(Blocks.SOUL_CAMPFIRE)) {
         int h = Math.max(2, MathHelper.ceil(e.getHeight()));

         for (int y = 0; y < h; y++) {
            BlockPos p = feet.up(y);
            if (!world.getFluidState(p).isEmpty()) {
               return false;
            }

            BlockState s = world.getBlockState(p);
            if (s.isOf(Blocks.FIRE)
               || s.isOf(Blocks.SOUL_FIRE)
               || s.isOf(Blocks.CACTUS)
               || s.isOf(Blocks.SWEET_BERRY_BUSH)
               || s.isOf(Blocks.POWDER_SNOW)
               || s.isOf(Blocks.COBWEB)) {
               return false;
            }
         }

         double hw = (double)e.getWidth() / 2.0;
         Box box = new Box(spot.x - hw, spot.y + 0.01, spot.z - hw, spot.x + hw, spot.y + (double)e.getHeight(), spot.z + hw);
         return world.isSpaceEmpty(box);
      } else {
         return false;
      }
   }

   private static boolean clearView(ServerWorld world, Vec3d from, Vec3d to, Entity e) {
      BlockHitResult hit = world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, e));
      return hit.getType() == Type.MISS;
   }

   static void spectatorEvent(BattleState state, PlayerManager pm, int kind, UUID actor, int targetIndex, int skillOrdinal, int classOrdinal, int color) {
      if (state.staged && actor != null && pm != null) {
         MinecraftServer server = pm.getServer();
         ServerWorld world = state.stageWorld != null ? server.getWorld(state.stageWorld) : null;
         if (world != null) {
            int actorId;
            if (BattleManager.isRecruitSlot(actor)) {
               Integer rid = state.recruitEntityIds.get(actor);
               if (rid == null) {
                  return;
               }

               actorId = rid;
            } else {
               ServerPlayerEntity ap = pm.getPlayer(actor);
               if (ap == null) {
                  return;
               }

               actorId = ap.getId();
            }

            int targetId = -1;
            if (targetIndex >= 10) {
               int pi = targetIndex - 10;
               if (pi >= 0 && pi < state.participantOrder.size()) {
                  UUID t = state.participantOrder.get(pi);
                  if (BattleManager.isRecruitSlot(t)) {
                     Integer rid = state.recruitEntityIds.get(t);
                     if (rid != null) {
                        targetId = rid;
                     }
                  } else {
                     ServerPlayerEntity tp = pm.getPlayer(t);
                     if (tp != null) {
                        targetId = tp.getId();
                     }
                  }
               }
            } else if (targetIndex >= 0 && kind != 2 && kind != 3) {
               targetId = BattleManager.enemyIdForIndex(state, targetIndex);
            }

            Vec3d center = state.stageOrigin != null ? state.stageOrigin : Vec3d.ZERO;

            for (ServerPlayerEntity p : world.getPlayers()) {
               if (!state.participantOrder.contains(p.getUuid()) && !(p.getPos().squaredDistanceTo(center) > 900.0)) {
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeByte(kind);
                  buf.writeInt(actorId);
                  buf.writeInt(targetId);
                  buf.writeInt(skillOrdinal);
                  buf.writeInt(classOrdinal);
                  buf.writeInt(color);
                  Net121.sendToClient(p, TurnBattleMod.WORLD_BATTLE_EVENT, buf);
               }
            }
         }
      }
   }
}
