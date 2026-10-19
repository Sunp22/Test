package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SoulVesselItem extends Item {
   private static final SoulType[] ROLLABLE_SOULS;

   public SoulVesselItem() {
      super(new Settings().maxCount(1));
   }

   public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
      tooltip.add(Text.literal("Rolls a random Soul Trait on use,").formatted(Formatting.GRAY));
      tooltip.add(Text.literal("grants a new soul if you are Soulless.").formatted(Formatting.GRAY));
   }

   public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
      if (!world.isClient() && user instanceof ServerPlayerEntity sp) {
         BattleData data = BattleData.get(sp.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(sp.getUuid());
         if (prog.playerRace == CharacterRace.MONSTER) {
            sp.sendMessage(Text.literal("Monsters cannot use a Soul Vessel."), true);
            return TypedActionResult.fail(user.getStackInHand(hand));
         }

         SoulType newSoul = rollSoul(prog.soulType);
         prog.soulType = newSoul;
         boolean wasSoulless = prog.soulRemoved;
         if (wasSoulless) {
            prog.soulRemoved = false;
         }

         prog.freeRollUsed = true;
         data.markDirty();
         sp.getStackInHand(hand).setCount(0);
         sp.playerScreenHandler.sendContentUpdates();
         sp.playSound(TurnBattleSounds.SND_GRAB, 1.0F, 1.0F);
         Net121.sendToClient(sp, TurnBattleMod.ESTABLISHING_CONNECTION, PacketByteBufs.empty());
         if (wasSoulless) {
            Net121.sendToClient(sp, TurnBattleMod.SOUL_RESTORED, PacketByteBufs.empty());
         }

         TurnBattleMod.sendSoulInfo(sp, prog);
      }

      return TypedActionResult.success(ItemStack.EMPTY, !world.isClient());
   }

   private static SoulType rollSoul(SoulType current) {
      if (ROLLABLE_SOULS.length == 0) {
         return SoulType.DETERMINATION;
      } else if (ROLLABLE_SOULS.length == 1) {
         return ROLLABLE_SOULS[0];
      } else {
         Random rng = new Random();

         SoulType result;
         do {
            result = ROLLABLE_SOULS[rng.nextInt(ROLLABLE_SOULS.length)];
         } while (result == current);

         return result;
      }
   }

   static {
      SoulType[] all = SoulType.values();
      List list = new ArrayList<>();

      for (SoulType s : all) {
         if (s != SoulType.ORI && s != SoulType.MONSTER) {
            list.add(s);
         }
      }

      ROLLABLE_SOULS = list.toArray(new SoulType[0]);
   }
}
