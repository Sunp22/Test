package com.pixelcite.turnbattle;

import java.util.Locale;
import java.util.Random;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

public class ArmorStats {
   public static final String NBT_KEY = "turnbattle_armor";
   public static final String[] STAT_NAMES = WeaponStats.STAT_NAMES;
   public int bonusStat;
   public int bonusAmt;
   static final int STATS_VERSION = 2;

   public static ArmorStats.Tier tierOf(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (stack.getItem() instanceof ArmorItem ai) {
            RegistryEntry matEntry = ai.getMaterial();
            ArmorMaterial var3 = (ArmorMaterial)matEntry.value();
            if (matEntry.getKey().map(k -> k.equals(ArmorMaterials.NETHERITE)).orElse(false)) {
               return ArmorStats.Tier.NETHERITE;
            } else if (matEntry.getKey().map(k -> k.equals(ArmorMaterials.DIAMOND)).orElse(false)) {
               return ArmorStats.Tier.DIAMOND;
            } else if (matEntry.getKey().map(k -> k.equals(ArmorMaterials.IRON)).orElse(false)) {
               return ArmorStats.Tier.IRON;
            } else if (matEntry.getKey().map(k -> k.equals(ArmorMaterials.CHAIN)).orElse(false)) {
               return ArmorStats.Tier.CHAINMAIL;
            } else if (!matEntry.getKey().map(k -> k.equals(ArmorMaterials.LEATHER)).orElse(false)
               && !matEntry.getKey().map(k -> k.equals(ArmorMaterials.GOLD)).orElse(false)
               && !matEntry.getKey().map(k -> k.equals(ArmorMaterials.TURTLE)).orElse(false)) {
               String path = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
               if (path.contains("netherite")) {
                  return ArmorStats.Tier.NETHERITE;
               } else if (path.contains("diamond")) {
                  return ArmorStats.Tier.DIAMOND;
               } else if (path.contains("iron") || path.contains("steel") || path.contains("metal")) {
                  return ArmorStats.Tier.IRON;
               } else {
                  return !path.contains("chain") && !path.contains("mail") ? ArmorStats.Tier.LEATHER : ArmorStats.Tier.CHAINMAIL;
               }
            } else {
               return ArmorStats.Tier.LEATHER;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean isArmorItem(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof ArmorItem;
   }

   public static EquipmentSlot getSlotType(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof ArmorItem ai ? ai.getSlotType() : null;
   }

   public static ArmorStats generate(Random rng, ArmorStats.Tier tier) {
      ArmorStats as = new ArmorStats();
      as.bonusStat = rng.nextInt(6);
      as.bonusAmt = tier.min + (tier.max > tier.min ? rng.nextInt(tier.max - tier.min + 1) : 0);
      return as;
   }

   public NbtCompound toNbt() {
      NbtCompound t = new NbtCompound();
      t.putInt("bonusStat", this.bonusStat);
      t.putInt("bonusAmt", this.bonusAmt);
      t.putInt("v", 2);
      return t;
   }

   public static ArmorStats fromNbt(NbtCompound t) {
      ArmorStats as = new ArmorStats();
      as.bonusStat = t.getInt("bonusStat");
      as.bonusAmt = t.getInt("bonusAmt");
      return as;
   }

   public static ArmorStats fromStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         NbtCompound tag = stack.get(DataComponentTypes.CUSTOM_DATA) != null
            ? ((NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA)).getNbt().getCompound("turnbattle_armor")
            : null;
         if (tag != null && tag.contains("bonusStat")) {
            ArmorStats as = fromNbt(tag);
            if (tag.getInt("v") < 2) {
               ArmorStats.Tier tier = tierOf(stack);
               if (tier != null) {
                  float f = tier.oldMax > tier.oldMin ? (float)(as.bonusAmt - tier.oldMin) / (float)(tier.oldMax - tier.oldMin) : 0.5F;
                  f = Math.max(0.0F, Math.min(1.0F, f));
                  as.bonusAmt = tier.min + Math.round(f * (float)(tier.max - tier.min));
               }
            }

            return as;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public void applyToStack(ItemStack stack) {
      NbtCompound _nbt = new NbtCompound();
      _nbt.put("turnbattle_armor", this.toNbt());
      stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(_nbt));
   }

   public static boolean rollStats(ItemStack stack, Random rng) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (fromStack(stack) != null) {
         return false;
      } else {
         ArmorStats.Tier tier = tierOf(stack);
         if (tier == null) {
            return false;
         } else {
            ArmorStats as = generate(rng, tier);
            as.applyToStack(stack);
            return true;
         }
      }
   }

   public int netStatBonus(int statIdx) {
      return statIdx == this.bonusStat ? this.bonusAmt : 0;
   }

   public String shortDesc() {
      return "+" + this.bonusAmt + " " + STAT_NAMES[this.bonusStat];
   }

   public static enum Tier {
      LEATHER(1, 2, 4.0F, "Leather", 1, 1),
      CHAINMAIL(2, 4, 7.0F, "Chainmail", 1, 1),
      IRON(4, 6, 10.0F, "Iron", 1, 2),
      DIAMOND(7, 10, 15.0F, "Diamond", 2, 4),
      NETHERITE(11, 14, 20.0F, "Netherite", 5, 5);

      public final int min;
      public final int max;
      final int oldMin;
      final int oldMax;
      public final float reductionPercent;
      public final String displayName;

      private Tier(int min, int max, float reductionPercent, String displayName, int oldMin, int oldMax) {
         this.min = min;
         this.max = max;
         this.oldMin = oldMin;
         this.oldMax = oldMax;
         this.reductionPercent = reductionPercent;
         this.displayName = displayName;
      }

      public float factor() {
         return 1.0F - this.reductionPercent / 100.0F;
      }
   }
}
