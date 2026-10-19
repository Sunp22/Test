package com.pixelcite.turnbattle;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class BossBeaconStructurePiece extends StructurePiece {
   public BossBeaconStructurePiece(BlockPos basePos) {
      super(
         TurnBattleStructures.BOSS_BEACON_PIECE,
         0,
         new BlockBox(basePos.getX() - 1, basePos.getY(), basePos.getZ() - 1, basePos.getX() + 1, basePos.getY() + 1, basePos.getZ() + 1)
      );
      this.setOrientation(Direction.NORTH);
   }

   public BossBeaconStructurePiece(StructurePieceType type, NbtCompound nbt) {
      super(type, nbt);
   }

   public static BossBeaconStructurePiece fromNbt(StructureContext ctx, NbtCompound nbt) {
      return new BossBeaconStructurePiece(TurnBattleStructures.BOSS_BEACON_PIECE, nbt);
   }

   protected void writeNbt(StructureContext context, NbtCompound nbt) {
   }

   public void generate(
      StructureWorldAccess world,
      StructureAccessor accessor,
      ChunkGenerator chunkGenerator,
      Random random,
      BlockBox chunkBox,
      ChunkPos chunkPos,
      BlockPos pivot
   ) {
      int cx = this.boundingBox.getMinX() + 1;
      int baseY = this.boundingBox.getMinY();
      int cz = this.boundingBox.getMinZ() + 1;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            BlockPos bp = new BlockPos(cx + dx, baseY, cz + dz);
            if (chunkBox.contains(bp)) {
               world.setBlockState(bp, Blocks.IRON_BLOCK.getDefaultState(), 3);
            }
         }
      }

      for (int dy = 1; dy <= 3; dy++) {
         BlockPos cp = new BlockPos(cx, baseY + dy, cz);
         if (chunkBox.contains(cp) && !world.getBlockState(cp).isAir()) {
            world.setBlockState(cp, Blocks.AIR.getDefaultState(), 3);
         }
      }

      BlockPos beaconPos = new BlockPos(cx, baseY + 1, cz);
      if (chunkBox.contains(beaconPos)) {
         world.setBlockState(beaconPos, Blocks.BEACON.getDefaultState(), 3);
      }
   }
}
