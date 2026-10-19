package com.pixelcite.turnbattle;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.util.math.BlockPos;

public class DarkFountainBlockEntity extends EndPortalBlockEntity {
   public DarkFountainBlockEntity(BlockPos pos, BlockState state) {
      super(TurnBattleMod.DARK_FOUNTAIN_BLOCK_ENTITY_TYPE, pos, state);
   }
}
