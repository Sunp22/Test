package com.pixelcite.turnbattle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.world.gen.structure.Structure.Config;
import net.minecraft.world.gen.structure.Structure.Context;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

public class BossBeaconStructure extends Structure {
   public static final MapCodec<BossBeaconStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Structure.configCodecBuilder(instance)).apply(instance, BossBeaconStructure::new)
   );

   public BossBeaconStructure(Config config) {
      super(config);
   }

   public Optional<StructurePosition> getStructurePosition(Context context) {
      int x = context.chunkPos().getStartX() + 8;
      int z = context.chunkPos().getStartZ() + 8;
      int y = context.chunkGenerator().getHeight(x, z, Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
      if (y >= 5 && y <= 230) {
         BlockPos basePos = new BlockPos(x, y, z);
         return Optional.of(new StructurePosition(basePos, collector -> collector.addPiece(new BossBeaconStructurePiece(basePos))));
      } else {
         return Optional.empty();
      }
   }

   public StructureType<?> getType() {
      return TurnBattleStructures.BOSS_BEACON_STRUCTURE_TYPE;
   }
}
