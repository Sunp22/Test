package com.pixelcite.turnbattle;

import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClimbClawsItem extends Item {
   public ClimbClawsItem() {
      super(new Settings().maxCount(1));
   }

   public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
      tooltip.add(Text.literal("Carry these to climb stone walls.").formatted(Formatting.GRAY));
      tooltip.add(Text.literal("Right-click a wall with an empty hand to grab on.").formatted(Formatting.DARK_GRAY));
   }

   public static boolean has(PlayerEntity player) {
      PlayerInventory inv = player.getInventory();

      for (ItemStack s : inv.main) {
         if (s.getItem() instanceof ClimbClawsItem) {
            return true;
         }
      }

      for (ItemStack sx : inv.offHand) {
         if (sx.getItem() instanceof ClimbClawsItem) {
            return true;
         }
      }

      return false;
   }
}
