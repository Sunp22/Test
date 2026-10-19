package com.pixelcite.turnbattle;

import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
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

public class SoulItem extends Item {
   private final SoulType soulType;

   public SoulItem(SoulType soulType) {
      super(new Settings().maxCount(1));
      this.soulType = soulType;
   }

   public SoulType getSoulType() {
      return this.soulType;
   }

   public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
      tooltip.add(Text.literal("Right-click to return to your chest.").formatted(Formatting.GRAY));
      if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
      }
   }

   public Text getName(ItemStack stack) {
      return (Text)(stack.contains(DataComponentTypes.CUSTOM_NAME)
         ? stack.getName()
         : Text.literal(this.soulType.getDisplayName() + " Soul").formatted(Formatting.WHITE));
   }

   public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
      if (!world.isClient() && user instanceof ServerPlayerEntity sp) {
         ItemStack stack = sp.getStackInHand(hand);
         BattleData data = BattleData.get(sp.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(sp.getUuid());
         if (prog.playerRace == CharacterRace.MONSTER) {
            String traitName = this.soulType.getDisplayName();
            String traitKey = this.soulType.name();
            if (this.soulType == SoulType.MONSTER) {
               sp.sendMessage(Text.literal("Monsters cannot absorb their own soul."), true);
               return TypedActionResult.fail(stack);
            }

            if (prog.absorbedSouls.contains(traitKey)) {
               sp.sendMessage(Text.literal("You've already absorbed a " + traitName + " soul."), true);
               return TypedActionResult.fail(stack);
            }

            prog.absorbedSouls.add(traitKey);
            prog.playerStats.absorbed = prog.absorbedSouls.size();
            prog.playerStats.offense += 2;
            prog.playerStats.defense += 2;
            prog.playerStats.magic += 2;
            prog.playerStats.iq += 2;
            prog.playerStats.vitality += 2;
            prog.playerStats.luck += 2;
            data.markDirty();
            sp.playSound(TurnBattleSounds.SND_GRAB, 1.0F, 1.0F);
            stack.setCount(0);
            sp.playerScreenHandler.sendContentUpdates();
            TurnBattleMod.sendPartyInfo(sp);
            sp.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue((double)prog.playerStats.maxHp());
            sp.heal(0.0F);
            return TypedActionResult.success(ItemStack.EMPTY, true);
         }

         boolean isOwnSoul = stack.contains(DataComponentTypes.CUSTOM_NAME)
            && prog.vesselName != null
            && stack.getName().getString().equals(prog.vesselName + "'s SOUL")
            && this.soulType == prog.soulType;
         if (this.soulType == SoulType.MONSTER && prog.playerRace != CharacterRace.MONSTER) {
            sp.sendMessage(Text.literal("This soul belongs to a Monster. You cannot absorb it."), true);
            return TypedActionResult.fail(stack);
         }

         sp.playSound(TurnBattleSounds.SND_GRAB, 1.0F, 1.0F);
         if (isOwnSoul) {
            prog.soulRemoved = false;
         } else {
            prog.soulType = this.soulType;
            prog.soulRemoved = false;
            Net121.sendToClient(sp, TurnBattleMod.ESTABLISHING_CONNECTION, PacketByteBufs.empty());
         }

         data.markDirty();
         stack.setCount(0);
         sp.playerScreenHandler.sendContentUpdates();
         Net121.sendToClient(sp, TurnBattleMod.SOUL_RESTORED, PacketByteBufs.empty());
         TurnBattleMod.sendSoulInfo(sp, prog);
      }

      return TypedActionResult.success(ItemStack.EMPTY, !world.isClient());
   }
}
