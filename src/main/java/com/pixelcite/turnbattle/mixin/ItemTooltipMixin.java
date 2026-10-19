package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.Affinity;
import com.pixelcite.turnbattle.ArmorStats;
import com.pixelcite.turnbattle.OffhandGimmick;
import com.pixelcite.turnbattle.WeaponStats;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public class ItemTooltipMixin {
   @Inject(
      method = {"getTooltip"},
      at = {@At("RETURN")}
   )
   private void addWeaponStatsTooltip(TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
      ItemStack self = (ItemStack)this;
      if (hasAttackDamage(self)) {
         List tooltip = (List<Text>)cir.getReturnValue();
         tooltip.add(Text.literal(""));
         tooltip.add(Text.literal("WEAPON").formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.BOLD}));
         WeaponStats ws = WeaponStats.fromStack(self);
         if (ws == null) {
            tooltip.add(Text.literal("\u2694 Battle Stats").formatted(Formatting.YELLOW));
            tooltip.add(Text.literal(" Roll stats via the Equip menu").formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}));
         } else {
            if (ws.isQuirk) {
               tooltip.add(Text.literal("\u26a1 ODDITY WEAPON").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}));
            } else {
               tooltip.add(Text.literal("\u2694 Battle Stats").formatted(Formatting.YELLOW));
            }

            tooltip.add(
               Text.literal(" +" + ws.baseDamage + (ws.magic ? " Bonus Magic DMG (magic skills)" : " Bonus DMG (Bash, physical skills)"))
                  .formatted(ws.magic ? Formatting.LIGHT_PURPLE : Formatting.WHITE)
            );
            tooltip.add(Text.literal(" +" + ws.bonusAmt + " " + WeaponStats.STAT_NAMES[ws.bonusStat]).formatted(Formatting.AQUA));
            if (ws.isQuirk) {
               tooltip.add(Text.literal(" +10 " + WeaponStats.STAT_NAMES[ws.quirkStat] + " (Oddity)").formatted(Formatting.GREEN));
               tooltip.add(Text.literal(" -10 " + WeaponStats.STAT_NAMES[ws.quirkMinus] + " (Oddity)").formatted(Formatting.RED));
            }

            for (Affinity elem : ws.getElements()) {
               tooltip.add(Text.literal(" " + elem.displayName + " Element").styled(style -> style.withColor(TextColor.fromRgb(elem.color))));
            }

            tooltip.add(Text.literal("Equip via Party Menu").formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.ITALIC}));
         }
      }
   }

   @Inject(
      method = {"getTooltip"},
      at = {@At("RETURN")}
   )
   private void addArmorStatsTooltip(TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
      ItemStack self = (ItemStack)this;
      if (self.getItem() instanceof ArmorItem) {
         List tooltip = (List<Text>)cir.getReturnValue();
         tooltip.add(Text.literal(""));
         tooltip.add(Text.literal("ARMOR").formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.BOLD}));
         ArmorStats.Tier tier = ArmorStats.tierOf(self);
         ArmorStats as = ArmorStats.fromStack(self);
         if (as == null) {
            tooltip.add(Text.literal("\ud83d\udee1 Battle Stats").formatted(Formatting.YELLOW));
            tooltip.add(Text.literal(" Roll stats via the Equip menu").formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}));
         } else {
            tooltip.add(Text.literal("\ud83d\udee1 Battle Stats").formatted(Formatting.YELLOW));
            if (tier != null) {
               tooltip.add(Text.literal(" " + tier.reductionPercent + "% damage reduction in battle").formatted(Formatting.WHITE));
            }

            tooltip.add(Text.literal(" +" + as.bonusAmt + " " + ArmorStats.STAT_NAMES[as.bonusStat]).formatted(Formatting.AQUA));
            tooltip.add(Text.literal("Equip via Party Menu").formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.ITALIC}));
         }
      }
   }

   @Inject(
      method = {"getTooltip"},
      at = {@At("RETURN")}
   )
   private void addFoodHealTooltip(TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
      ItemStack self = (ItemStack)this;
      FoodComponent food = (FoodComponent)self.get(DataComponentTypes.FOOD);
      if (food != null) {
         int hp = (int)(((float)(food.nutrition() * 2) + food.saturation() * 2.0F) * 2.0F);
         List tooltip = (List<Text>)cir.getReturnValue();
         tooltip.add(Text.literal(""));
         tooltip.add(Text.literal("\u2665 Recovers " + hp + " HP in battle").formatted(Formatting.RED));
      }
   }

   @Inject(
      method = {"getTooltip"},
      at = {@At("RETURN")}
   )
   private void addAccessoryTooltip(TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
      ItemStack self = (ItemStack)this;
      OffhandGimmick gimmick = OffhandGimmick.fromStack(self);
      if (gimmick != OffhandGimmick.NONE) {
         List tooltip = (List<Text>)cir.getReturnValue();
         tooltip.add(Text.literal(""));
         tooltip.add(Text.literal("ACCESSORY").formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.BOLD}));
         tooltip.add(Text.literal("\ud83c\udf97 Battle Gimmick").formatted(Formatting.YELLOW));
         tooltip.add(Text.literal(" " + gimmick.description()).formatted(Formatting.AQUA));
         tooltip.add(Text.literal("Equip via Party Menu").formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.ITALIC}));
      }
   }

   private static boolean hasAttackDamage(ItemStack stack) {
      AttributeModifiersComponent attrComp = (AttributeModifiersComponent)stack.getItem().getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      return attrComp == null ? false : !attrComp.modifiers().isEmpty();
   }
}
