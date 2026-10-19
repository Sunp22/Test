package com.pixelcite.turnbattle;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MannequinItem extends Item {
   public MannequinItem() {
      super(new Settings().maxCount(1));
   }

   public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
      return TypedActionResult.pass(user.getStackInHand(hand));
   }
}
