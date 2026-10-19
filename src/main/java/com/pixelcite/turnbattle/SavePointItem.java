package com.pixelcite.turnbattle;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class SavePointItem extends Item {
   public SavePointItem() {
      super(new Settings());
   }

   public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
      tooltip.add(Text.literal("Right-click to name and place a save point.").formatted(Formatting.GRAY));
   }
}
