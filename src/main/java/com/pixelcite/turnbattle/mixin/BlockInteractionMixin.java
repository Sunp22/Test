package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleData;
import com.pixelcite.turnbattle.BattleManager;
import com.pixelcite.turnbattle.BossBeaconManager;
import com.pixelcite.turnbattle.TurnBattleMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayerInteractionManager.class})
public class BlockInteractionMixin {
   @Shadow
   public ServerPlayerEntity field_14008;
   private static final Identifier BOSS_BEACON_ID = Identifier.of("turnbattle", "boss_beacon");

   @Inject(
      method = {"interactBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void campfire$onInteractBlock(
      ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir
   ) {
      if (!world.isClient()) {
         if (hand == Hand.MAIN_HAND) {
            if (stack.isEmpty()) {
               BlockPos pos = hitResult.getBlockPos();
               BlockState blockState = world.getBlockState(pos);
               if (blockState.isOf(Blocks.CAMPFIRE) || blockState.isOf(Blocks.SOUL_CAMPFIRE)) {
                  if ((Boolean)blockState.get(CampfireBlock.LIT)) {
                     if (world instanceof ServerWorld sw) {
                        BattleData data = BattleData.get(sw.getServer());
                        BattleData.PlayerProgress var11 = data.getOrCreate(player.getUuid());
                        player.setHealth(player.getMaxHealth());
                        int var12 = var11.partyMember.ordinal();
                        float var13 = BattleManager.computePartyMemberMaxHpPublic(var11.partyLevels[var12], var11.partyStats[var12]);
                        var11.partyMemberHp = var13;
                        data.markDirty();
                        TurnBattleMod.sendPartyInfo(player);
                        player.sendMessage(Text.literal("\u2726 Rested at the campfire. HP fully restored.").formatted(Formatting.GREEN), true);
                        cir.setReturnValue(ActionResult.SUCCESS);
                     }
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"interactBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bossBeacon$onInteractBlock(
      ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir
   ) {
      if (!world.isClient()) {
         BlockPos pos = hitResult.getBlockPos();
         if (world.getBlockState(pos).isOf(Blocks.BEACON)) {
            if (world instanceof ServerWorld sw) {
               if (isBossBeaconBlock(sw, pos)) {
                  BossBeaconManager.onPlayerInteract(player, pos);
                  cir.setReturnValue(ActionResult.SUCCESS);
               }
            }
         }
      }
   }

   @Inject(
      method = {"tryBreakBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bossBeacon$onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (this.field_14008 != null && !this.field_14008.getWorld().isClient()) {
         if (this.field_14008.getWorld() instanceof ServerWorld sw) {
            BlockState state = sw.getBlockState(pos);
            if (state.isOf(Blocks.BEACON) || state.isOf(Blocks.IRON_BLOCK)) {
               if (isBossBeaconBlock(sw, pos)) {
                  cir.setReturnValue(false);
                  this.field_14008.sendMessage(Text.literal("This structure cannot be mined.").formatted(Formatting.RED), true);
               }
            }
         }
      }
   }

   private static boolean isBossBeaconBlock(ServerWorld sw, BlockPos pos) {
      Registry structureRegistry = sw.getRegistryManager().get(RegistryKeys.STRUCTURE);
      if (!structureRegistry.containsId(BOSS_BEACON_ID)) {
         return false;
      } else {
         Structure bossStructure = (Structure)structureRegistry.get(BOSS_BEACON_ID);
         if (bossStructure == null) {
            return false;
         } else {
            StructureStart start = sw.getStructureAccessor().getStructureAt(pos, bossStructure);
            if (start.hasChildren()) {
               return true;
            } else {
               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     BlockPos check = pos.add(dx, 1, dz);
                     StructureStart s2 = sw.getStructureAccessor().getStructureAt(check, bossStructure);
                     if (s2.hasChildren()) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      }
   }
}
