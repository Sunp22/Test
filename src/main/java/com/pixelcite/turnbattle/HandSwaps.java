package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class HandSwaps {
   private static final Map<HandSwaps.Key, HandSwaps.Held> open = new HashMap<>();

   private HandSwaps() {
   }

   public static void showMain(PlayerEntity p, ItemStack shown) {
      int slot = p.getInventory().selectedSlot;
      if (slot >= 0 && slot < 9 && shown != null) {
         open.putIfAbsent(new HandSwaps.Key(p, false), new HandSwaps.Held(slot, (ItemStack)p.getInventory().main.get(slot)));
         p.getInventory().main.set(slot, shown);
      }
   }

   public static void showOff(PlayerEntity p, ItemStack shown) {
      if (shown != null) {
         open.putIfAbsent(new HandSwaps.Key(p, true), new HandSwaps.Held(0, (ItemStack)p.getInventory().offHand.get(0)));
         p.getInventory().offHand.set(0, shown);
      }
   }

   public static boolean isShowing(PlayerEntity p) {
      return open.containsKey(new HandSwaps.Key(p, false));
   }

   public static void restoreMain(PlayerEntity p) {
      HandSwaps.Held h = open.remove(new HandSwaps.Key(p, false));
      if (h != null) {
         p.getInventory().main.set(h.slot(), h.original());
      }
   }

   public static void restoreOff(PlayerEntity p) {
      HandSwaps.Held h = open.remove(new HandSwaps.Key(p, true));
      if (h != null) {
         p.getInventory().offHand.set(0, h.original());
      }
   }

   public static void restore(PlayerEntity p) {
      restoreMain(p);
      restoreOff(p);
   }

   public static void restoreAll() {
      if (!open.isEmpty()) {
         for (Entry e : open.entrySet()) {
            PlayerEntity p = e.getKey().player();
            HandSwaps.Held h = e.getValue();
            if (e.getKey().offhand()) {
               p.getInventory().offHand.set(0, h.original());
            } else {
               p.getInventory().main.set(h.slot(), h.original());
            }
         }

         open.clear();
      }
   }

   private static record Held(int slot, ItemStack original) {
   }

   private static record Key(PlayerEntity player, boolean offhand) {
   }
}
