package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

public class WallClimbManager {
   private static final double STEP_SPEED = 0.1111111111111111;
   private static final double BOOST_SPEED = 0.25;
   private static final double JUMP_SPEED = 0.4;
   private static final double VAULT_SPEED = 0.22;
   private static final int STEP_REPEAT_DELAY = 2;
   private static final int BOOST_REPEAT_DELAY = 0;
   private static final int PHASE1_TICKS = 8;
   private static final int PHASE2_TICKS = 16;
   private static final int PHASE3_LOOP_TICKS = 22;
   private static final int[] BOOST_DURATION_TICKS = new int[]{0, 10, 20, 40};
   private static final double WALL_OFFSET = 0.9;
   private static final double SNAP_THRESHOLD = 0.02;
   static final int ANIM_NONE = 0;
   static final int ANIM_HANG = 1;
   static final int ANIM_STEP = 2;
   static final int ANIM_CHARGE = 3;
   static final int ANIM_JUMP = 4;
   static final int ANIM_VAULT = 5;
   static final int DIR_UP = 0;
   static final int DIR_DOWN = 1;
   static final int DIR_LEFT = 2;
   static final int DIR_RIGHT = 3;
   private static final Map<UUID, WallClimbManager.ClimbState> climbing = new HashMap<>();
   private static final String[] CLIMBABLE_IDS = new String[]{
      "stone",
      "cobblestone",
      "deepslate",
      "infested_stone",
      "infested_cobblestone",
      "netherrack",
      "chiseled_bookshelf",
      "_ore",
      "andesite",
      "granite",
      "diorite",
      "prismarine",
      "sculk"
   };

   private static int dirCode(WallClimbManager.ClimbState s, Direction d) {
      if (d == Direction.UP) {
         return 0;
      } else if (d == Direction.DOWN) {
         return 1;
      } else {
         return d == s.wallLeft ? 2 : 3;
      }
   }

   private static void setAnim(ServerPlayerEntity player, WallClimbManager.ClimbState s, int anim, int dir) {
      if (s == null || s.anim != anim || s.animDir != dir) {
         if (s != null) {
            if (anim == 2) {
               s.stepCount++;
            }

            s.anim = anim;
            s.animDir = dir;
         }

         broadcastAnim(player, anim, dir, s == null ? Direction.NORTH : s.wallOut, s == null ? 0 : s.stepCount);
      }
   }

   private static void broadcastAnim(ServerPlayerEntity player, int anim, int dir, Direction wallOut, int stepCount) {
      if (player.getWorld() instanceof ServerWorld world) {
         for (ServerPlayerEntity other : world.getPlayers()) {
            if (other == player || !(other.squaredDistanceTo(player) > 25600.0)) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeUuid(player.getUuid());
               buf.writeByte(anim);
               buf.writeByte(dir);
               buf.writeByte(wallOut.getId());
               buf.writeVarInt(stepCount);
               Net121.sendToClient(other, TurnBattleMod.WALL_CLIMB_ANIM, buf);
            }
         }
      }
   }

   public static boolean isClimbing(UUID uuid) {
      return climbing.containsKey(uuid);
   }

   public static boolean isMoving(UUID uuid) {
      WallClimbManager.ClimbState s = climbing.get(uuid);
      return s != null && s.moving;
   }

   static boolean isClimbableStone(BlockState state) {
      if (state.isAir()) {
         return false;
      } else {
         String path = Registries.BLOCK.getId(state.getBlock()).getPath();
         if (path.contains("polished_")) {
            return false;
         } else if (path.contains("smooth_stone")) {
            return false;
         } else if (path.contains("redstone") && !path.contains("redstone_ore")) {
            return false;
         } else {
            for (String s : CLIMBABLE_IDS) {
               if (path.contains(s)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static boolean isSpaceClear(World world, BlockPos grabPos, Direction wallOut) {
      BlockPos headSpace = grabPos.offset(wallOut);
      BlockPos feetSpace = headSpace.down();
      return !world.getBlockState(headSpace).isSolidBlock(world, headSpace) && !world.getBlockState(feetSpace).isSolidBlock(world, feetSpace);
   }

   private static Vec3d wallPosFor(BlockPos grabPos, Direction wallOut) {
      double x = (double)grabPos.getX() + 0.5 + (double)wallOut.getOffsetX() * 0.9;
      double y = (double)grabPos.getY() - 1.0;
      double z = (double)grabPos.getZ() + 0.5 + (double)wallOut.getOffsetZ() * 0.9;
      return new Vec3d(x, y, z);
   }

   private static boolean moveToward(ServerPlayerEntity player, Vec3d target, double speed) {
      Vec3d current = player.getPos();
      Vec3d toTarget = target.subtract(current);
      double dist = toTarget.length();
      if (!(dist <= speed) && !(dist < 0.02)) {
         Vec3d dir = toTarget.multiply(1.0 / dist);
         player.setVelocity(dir.multiply(speed));
         player.velocityModified = true;
         return false;
      } else {
         player.teleport((ServerWorld)player.getWorld(), target.x, target.y, target.z, player.getYaw(), player.getPitch());
         player.setVelocity(Vec3d.ZERO);
         player.velocityModified = true;
         return true;
      }
   }

   public static boolean tryGrab(ServerPlayerEntity player, BlockHitResult hit) {
      ItemStack main = player.getMainHandStack();
      if (!main.isEmpty() && !(main.getItem() instanceof ClimbClawsItem)) {
         return false;
      } else {
         Direction side = hit.getSide();
         if (side.getAxis() == Axis.Y) {
            return false;
         } else {
            World world = player.getWorld();
            BlockPos headBlockPos = BlockPos.ofFloored(player.getX(), player.getY() + 1.0, player.getZ());
            BlockPos grabPos = headBlockPos.offset(side.getOpposite());
            if (!isClimbableStone(world.getBlockState(grabPos))) {
               return false;
            } else if (!ClimbClawsItem.has(player)) {
               player.sendMessage(Text.literal("You need Climb Claws to climb this.").formatted(Formatting.GRAY), true);
               return false;
            } else if (!isSpaceClear(world, grabPos, side)) {
               return false;
            } else {
               WallClimbManager.ClimbState state = new WallClimbManager.ClimbState();
               state.grabPos = grabPos;
               state.wallOut = side;
               state.wallRight = side.getOpposite().rotateYClockwise();
               state.wallLeft = side.getOpposite().rotateYCounterclockwise();
               climbing.put(player.getUuid(), state);
               Vec3d pos = wallPosFor(grabPos, side);
               player.teleport((ServerWorld)world, pos.x, pos.y, pos.z, player.getYaw(), player.getPitch());
               player.setVelocity(Vec3d.ZERO);
               player.velocityModified = true;
               player.setNoGravity(true);
               player.fallDistance = 0.0F;
               world.playSound(null, player.getBlockPos(), TurnBattleSounds.CLIMB_NOISE, SoundCategory.PLAYERS, 1.0F, 1.0F);
               sendClimbState(player, true);
               setAnim(player, state, 1, 0);
               return true;
            }
         }
      }
   }

   public static void release(ServerPlayerEntity player) {
      if (climbing.remove(player.getUuid()) != null) {
         broadcastAnim(player, 0, 0, Direction.NORTH, 0);
         player.setNoGravity(false);
         player.fallDistance = 0.0F;
         sendClimbState(player, false);
         sendTelegraph(player, false, null, null, false);
      }
   }

   public static void onInput(UUID uuid, byte mask) {
      WallClimbManager.ClimbState s = climbing.get(uuid);
      if (s != null) {
         s.inForward = (mask & 1) != 0;
         s.inBack = (mask & 2) != 0;
         s.inLeft = (mask & 4) != 0;
         s.inRight = (mask & 8) != 0;
         s.inSprint = (mask & 16) != 0;
         s.inSneak = (mask & 32) != 0;
         s.inJump = (mask & 64) != 0;
      }
   }

   public static void tick(MinecraftServer server) {
      if (!climbing.isEmpty()) {
         long now = server.getOverworld().getTime();

         for (UUID uuid : new HashSet<>(climbing.keySet())) {
            WallClimbManager.ClimbState s = climbing.get(uuid);
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null || s == null) {
               climbing.remove(uuid);
            } else if (s.inSneak) {
               release(player);
            } else if (now % 10L == 0L && !ClimbClawsItem.has(player)) {
               release(player);
            } else {
               if (now % 40L == 0L && s.anim >= 0) {
                  broadcastAnim(player, s.anim, s.animDir, s.wallOut, s.stepCount);
               }

               tickChargeJump(server, player, s, now);
               if (!s.charging) {
                  tickStepping(server, player, s, now);
               }

               if (climbing.containsKey(uuid) && s.moving != s.lastSentMovingState) {
                  s.lastSentMovingState = s.moving;
                  sendStepping(player, s.moving);
               }
            }
         }
      }
   }

   private static void tickChargeJump(MinecraftServer server, ServerPlayerEntity player, WallClimbManager.ClimbState s, long now) {
      World world = player.getWorld();
      if (s.inSprint) {
         if (!s.charging) {
            s.charging = true;
            s.chargeStartTick = now;
            s.lastPhase3SoundTick = Long.MIN_VALUE;
            setAnim(player, s, 3, 0);
            world.playSound(null, player.getBlockPos(), TurnBattleSounds.CHARGEJUMP_1, SoundCategory.PLAYERS, 1.0F, 1.0F);
         } else {
            long elapsed = now - s.chargeStartTick;
            if (elapsed == 8L) {
               world.playSound(null, player.getBlockPos(), TurnBattleSounds.CHARGEJUMP_2, SoundCategory.PLAYERS, 1.0F, 1.0F);
            } else if (elapsed == 16L) {
               world.playSound(null, player.getBlockPos(), TurnBattleSounds.CHARGEJUMP_3, SoundCategory.PLAYERS, 1.0F, 1.0F);
               s.lastPhase3SoundTick = now;
            } else if (elapsed > 16L && (elapsed - 16L) % 22L == 0L) {
               world.playSound(null, player.getBlockPos(), TurnBattleSounds.CHARGEJUMP_3, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
         }

         long elapsedx = now - s.chargeStartTick;
         int phase = computePhase(elapsedx);
         Direction jumpDir = computeJumpDir(s);
         BlockPos target = s.grabPos.offset(jumpDir, phase);
         boolean valid = isClimbableStone(world.getBlockState(target)) && isSpaceClear(world, target, s.wallOut);
         sendTelegraph(player, true, target, s.wallOut, valid);
         player.setVelocity(Vec3d.ZERO);
         player.velocityModified = true;
      } else if (s.charging) {
         long elapsed = now - s.chargeStartTick;
         s.charging = false;
         sendTelegraph(player, false, null, null, false);
         int phase = computePhase(elapsed);
         Direction jumpDir = computeJumpDir(s);
         BlockPos target = s.grabPos.offset(jumpDir, phase);
         if (isClimbableStone(world.getBlockState(target)) && isSpaceClear(world, target, s.wallOut)) {
            s.moving = true;
            s.jumpLanding = true;
            s.moveTarget = wallPosFor(target, s.wallOut);
            s.moveGrabPos = target;
            s.moveSpeed = 0.4;
            s.boostUntilTick = now + (long)BOOST_DURATION_TICKS[phase];
            setAnim(player, s, 4, dirCode(s, jumpDir));
         } else {
            setAnim(player, s, 1, 0);
         }
      }
   }

   private static void tickStepping(MinecraftServer server, ServerPlayerEntity player, WallClimbManager.ClimbState s, long now) {
      World world = player.getWorld();
      boolean boosted = now < s.boostUntilTick;
      double speed = boosted ? 0.25 : 0.1111111111111111;
      int repeatDelay = boosted ? 0 : 2;
      if (s.moving) {
         boolean reached = moveToward(player, s.moveTarget, s.moveSpeed);
         if (reached) {
            if (s.vaultPhase == 1) {
               s.vaultPhase = 2;
               s.moveTarget = new Vec3d((double)s.grabPos.getX() + 0.5, (double)s.grabPos.getY() + 1.0, (double)s.grabPos.getZ() + 0.5);
            } else if (s.vaultPhase == 2) {
               s.vaultPhase = 0;
               s.moving = false;
               climbing.remove(player.getUuid());
               player.setNoGravity(false);
               sendClimbState(player, false);
               broadcastAnim(player, 0, 0, Direction.NORTH, 0);
            } else {
               s.moving = false;
               s.grabPos = s.moveGrabPos;
               s.lastStepEndTick = now;
               if (s.jumpLanding) {
                  s.jumpLanding = false;
                  world.playSound(null, player.getBlockPos(), TurnBattleSounds.CLIMB_NOISE, SoundCategory.PLAYERS, 1.0F, 1.0F);
               }
            }
         }
      } else if (now - s.lastStepEndTick >= (long)repeatDelay) {
         if (!s.inJump) {
            Direction dir = null;
            if (s.inForward) {
               dir = Direction.UP;
            } else if (s.inBack) {
               dir = Direction.DOWN;
            } else if (s.inLeft) {
               dir = s.wallLeft;
            } else if (s.inRight) {
               dir = s.wallRight;
            }

            if (dir == null) {
               setAnim(player, s, 1, 0);
               player.setVelocity(Vec3d.ZERO);
               player.velocityModified = true;
            } else {
               BlockPos target = s.grabPos.offset(dir);
               if (isClimbableStone(world.getBlockState(target)) && isSpaceClear(world, target, s.wallOut)) {
                  s.anim = -1;
                  setAnim(player, s, 2, dirCode(s, dir));
                  s.moving = true;
                  s.jumpLanding = false;
                  s.moveTarget = wallPosFor(target, s.wallOut);
                  s.moveGrabPos = target;
                  s.moveSpeed = speed;
                  world.playSound(null, player.getBlockPos(), TurnBattleSounds.CLIMB_STEP, SoundCategory.PLAYERS, 1.0F, 1.0F);
               } else {
                  setAnim(player, s, 1, 0);
                  player.setVelocity(Vec3d.ZERO);
                  player.velocityModified = true;
               }
            }
         } else {
            BlockPos above1 = s.grabPos.up();
            BlockPos above2 = s.grabPos.up(2);
            if (world.getBlockState(above1).isAir() && world.getBlockState(above2).isAir()) {
               setAnim(player, s, 5, 0);
               vaultUp(player, s);
            } else {
               player.setVelocity(Vec3d.ZERO);
               player.velocityModified = true;
            }
         }
      }
   }

   private static void vaultUp(ServerPlayerEntity player, WallClimbManager.ClimbState s) {
      Vec3d cur = player.getPos();
      s.vaultPhase = 1;
      s.moving = true;
      s.moveTarget = new Vec3d(cur.x, (double)s.grabPos.getY() + 1.0, cur.z);
      s.moveSpeed = 0.22;
   }

   private static void sendClimbState(ServerPlayerEntity player, boolean climbingNow) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(climbingNow);
      Net121.sendToClient(player, TurnBattleMod.WALL_CLIMB_STATE, buf);
   }

   private static void sendStepping(ServerPlayerEntity player, boolean stepping) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(stepping);
      Net121.sendToClient(player, TurnBattleMod.WALL_CLIMB_STEPPING, buf);
   }

   private static int computePhase(long elapsed) {
      if (elapsed < 8L) {
         return 1;
      } else {
         return elapsed < 16L ? 2 : 3;
      }
   }

   private static Direction computeJumpDir(WallClimbManager.ClimbState s) {
      if (s.inBack) {
         return Direction.DOWN;
      } else if (s.inLeft) {
         return s.wallLeft;
      } else {
         return s.inRight ? s.wallRight : Direction.UP;
      }
   }

   private static void sendTelegraph(ServerPlayerEntity player, boolean active, BlockPos target, Direction wallOut, boolean valid) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(active);
      if (active) {
         buf.writeBlockPos(target);
         buf.writeInt(wallOut.ordinal());
         buf.writeBoolean(valid);
      }

      Net121.sendToClient(player, TurnBattleMod.WALL_CLIMB_TELEGRAPH, buf);
   }

   public static void forceRelease(ServerPlayerEntity player) {
      if (climbing.remove(player.getUuid()) != null) {
         broadcastAnim(player, 0, 0, Direction.NORTH, 0);
         player.setNoGravity(false);
         sendClimbState(player, false);
         sendTelegraph(player, false, null, null, false);
      }
   }

   static class ClimbState {
      BlockPos grabPos;
      Direction wallOut;
      Direction wallRight;
      Direction wallLeft;
      boolean inForward;
      boolean inBack;
      boolean inLeft;
      boolean inRight;
      boolean inSprint;
      boolean inSneak;
      boolean inJump;
      boolean moving = false;
      Vec3d moveTarget;
      double moveSpeed;
      BlockPos moveGrabPos;
      int vaultPhase = 0;
      boolean jumpLanding = false;
      long lastStepEndTick = -4611686018427387904L;
      boolean lastSentMovingState = false;
      boolean charging = false;
      long chargeStartTick;
      long lastPhase3SoundTick = Long.MIN_VALUE;
      long boostUntilTick = Long.MIN_VALUE;
      int anim = -1;
      int animDir = -1;
      int stepCount = 0;
   }
}
