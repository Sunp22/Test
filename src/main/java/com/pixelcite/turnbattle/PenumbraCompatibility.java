package com.pixelcite.turnbattle;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class PenumbraCompatibility {
   private static final String FORGE_CAPS_KEY = "ForgeCaps";
   private static final String CAP_KEY = "penumbra_phantasm:soul_capability";
   private static final String SOUL_TYPE_KEY = "soulType";
   private static final String SEEN_INTRO_KEY = "seenIntro";

   public static boolean isPresent(ServerPlayerEntity player) {
      NbtCompound nbt = player.writeNbt(new NbtCompound());
      if (!nbt.contains("ForgeCaps")) {
         return false;
      } else {
         NbtCompound caps = nbt.getCompound("ForgeCaps");
         return caps.contains("penumbra_phantasm:soul_capability");
      }
   }

   public static int getPenumbraSoulType(ServerPlayerEntity player) {
      NbtCompound nbt = player.writeNbt(new NbtCompound());
      if (!nbt.contains("ForgeCaps")) {
         return -1;
      } else {
         NbtCompound caps = nbt.getCompound("ForgeCaps");
         if (!caps.contains("penumbra_phantasm:soul_capability")) {
            return -1;
         } else {
            NbtCompound cap = caps.getCompound("penumbra_phantasm:soul_capability");
            return cap.getInt("soulType");
         }
      }
   }

   public static boolean hasSeenIntro(ServerPlayerEntity player) {
      NbtCompound nbt = player.writeNbt(new NbtCompound());
      if (!nbt.contains("ForgeCaps")) {
         return false;
      } else {
         NbtCompound caps = nbt.getCompound("ForgeCaps");
         return !caps.contains("penumbra_phantasm:soul_capability") ? false : caps.getCompound("penumbra_phantasm:soul_capability").getBoolean("seenIntro");
      }
   }

   public static SoulType toSoulType(int penumbraType) {
      return switch (penumbraType) {
         case 1 -> SoulType.DETERMINATION;
         case 2 -> SoulType.PATIENCE;
         case 3 -> SoulType.BRAVERY;
         case 4 -> SoulType.INTEGRITY;
         case 5 -> SoulType.PERSEVERANCE;
         case 6 -> SoulType.KINDNESS;
         case 7 -> SoulType.JUSTICE;
         default -> null;
      };
   }

   public static boolean isSoulSynced(ServerPlayerEntity player, SoulType currentSoul) {
      int penumbraType = getPenumbraSoulType(player);
      if (penumbraType <= 0) {
         return true;
      } else {
         SoulType expected = toSoulType(penumbraType);
         return expected == null || expected == currentSoul;
      }
   }

   public static SoulType syncIfNeeded(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      int penumbraType = getPenumbraSoulType(player);
      if (penumbraType <= 0) {
         return null;
      } else if (!hasSeenIntro(player)) {
         return null;
      } else {
         SoulType mapped = toSoulType(penumbraType);
         if (mapped == null) {
            return null;
         } else if (mapped == prog.soulType) {
            return null;
         } else {
            prog.soulType = mapped;
            prog.freeRollUsed = true;
            return mapped;
         }
      }
   }
}
