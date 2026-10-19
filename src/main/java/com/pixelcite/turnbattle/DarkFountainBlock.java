package com.pixelcite.turnbattle;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DarkFountainBlock extends BlockWithEntity {
   public DarkFountainBlock() {
      super(
         Settings.create()
            .sounds(BlockSoundGroup.STONE)
            .strength(-1.0F, 3600000.0F)
            .nonOpaque()
            .suffocates((state, world, pos) -> false)
            .blockVision((state, world, pos) -> false)
            .allowsSpawning((state, world, pos, type) -> false)
      );
   }

   public MapCodec<DarkFountainBlock> getCodec() {
      return createCodec(settings -> new DarkFountainBlock());
   }

   public BlockRenderType getRenderType(BlockState state) {
      return BlockRenderType.INVISIBLE;
   }

   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new DarkFountainBlockEntity(pos, state);
   }

   protected boolean canReplace(BlockState state, ItemPlacementContext context) {
      return false;
   }

   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
         DarkFountainManager.onSealPromptRequest(serverPlayer);
      }

      return ActionResult.SUCCESS;
   }
}
