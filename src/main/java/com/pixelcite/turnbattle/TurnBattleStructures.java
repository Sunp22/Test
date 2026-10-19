package com.pixelcite.turnbattle;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

public class TurnBattleStructures {
   public static StructurePieceType BOSS_BEACON_PIECE;
   public static StructureType<BossBeaconStructure> BOSS_BEACON_STRUCTURE_TYPE;

   public static void register() {
      BOSS_BEACON_PIECE = (StructurePieceType)Registry.register(
         Registries.STRUCTURE_PIECE, Identifier.of("turnbattle", "boss_beacon_piece"), BossBeaconStructurePiece::fromNbt
      );
      BOSS_BEACON_STRUCTURE_TYPE = (StructureType<BossBeaconStructure>)Registry.register(
         Registries.STRUCTURE_TYPE, Identifier.of("turnbattle", "boss_beacon"), (StructureType)() -> BossBeaconStructure.CODEC
      );
   }
}
