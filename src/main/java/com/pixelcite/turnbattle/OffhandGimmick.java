package com.pixelcite.turnbattle;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum OffhandGimmick {
   NONE,
   SHIELD,
   TORCH,
   FEATHER,
   THORN_RING,
   PSI_PROCESSOR,
   HEART_LOCKET,
   SOUL_RESONATOR;

   public static OffhandGimmick fromOffhand(ItemStack stack) {
      return fromStack(stack);
   }

   public static OffhandGimmick fromStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item item = stack.getItem();
         if (item == Items.SHIELD) {
            return SHIELD;
         } else if (item == Items.TORCH) {
            return TORCH;
         } else if (item == Items.FEATHER) {
            return FEATHER;
         } else if (item == TurnBattleMod.THORN_RING) {
            return THORN_RING;
         } else if (item == TurnBattleMod.PSI_PROCESSOR) {
            return PSI_PROCESSOR;
         } else if (item == TurnBattleMod.HEART_LOCKET) {
            return HEART_LOCKET;
         } else {
            return item == TurnBattleMod.SOUL_RESONATOR ? SOUL_RESONATOR : NONE;
         }
      } else {
         return NONE;
      }
   }

   public static boolean isAccessoryItem(ItemStack stack) {
      return fromStack(stack) != NONE;
   }

   public String description() {
      return switch (this) {
         case SHIELD -> "White ring destroys nearby bullets once per enemy turn";
         case TORCH -> "25% chance to inflict Burn 2 on bash";
         case FEATHER -> "Shift to dash with i-frames during bullet phase";
         case THORN_RING -> "50% TP cost reduction; drains 1 HP every 5 ticks down to 1/3 max HP";
         case PSI_PROCESSOR -> "Drains 1 TP every 10 ticks; doubles PSI skill damage";
         case HEART_LOCKET -> "Halves all damage you take in battle";
         case SOUL_RESONATOR -> "Graze converts TP\u2192HP; Soul Mage skills cost HP";
         default -> "";
      };
   }
}
