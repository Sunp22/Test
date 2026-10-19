package com.pixelcite.turnbattle;

import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class DarkFountainKnife extends Item {
   public DarkFountainKnife() {
      super(new Settings().maxCount(1));
   }

   public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
      tooltip.add(Text.literal("Right-click: open the Dark World menu.").formatted(Formatting.GRAY));
   }

   public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
      if (DarkFountainManager.isDarkWorld(world.getRegistryKey())) {
         if (user instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("You probably shouldn't do that here..."), true);
         }

         return TypedActionResult.fail(user.getStackInHand(hand));
      } else {
         if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            DarkFountainManager.onKnifeOpenMenu(serverPlayer);
         }

         return TypedActionResult.success(user.getStackInHand(hand));
      }
   }
}
