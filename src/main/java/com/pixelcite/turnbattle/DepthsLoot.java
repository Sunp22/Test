package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.Registries;

public final class DepthsLoot {
   private static Map<WeaponStats.Tier, List<Item>> weapons;
   private static Map<ArmorStats.Tier, List<Item>> armor;

   private DepthsLoot() {
   }

   private static synchronized void index() {
      if (weapons == null) {
         weapons = new EnumMap<>(WeaponStats.Tier.class);
         armor = new EnumMap<>(ArmorStats.Tier.class);

         for (Item item : Registries.ITEM) {
            ItemStack s;
            try {
               s = new ItemStack(item);
            } catch (Throwable var5) {
               continue;
            }

            if (!s.isEmpty() && !(item instanceof RangedWeaponItem) && !(item instanceof ArrowItem)) {
               try {
                  if (ArmorStats.isArmorItem(s)) {
                     armor.computeIfAbsent(ArmorStats.tierOf(s), k -> new ArrayList<>()).add(item);
                  } else if (hasAttackDamage(s)) {
                     weapons.computeIfAbsent(WeaponStats.tierOf(s), k -> new ArrayList<>()).add(item);
                  }
               } catch (Throwable var4) {
               }
            }
         }
      }
   }

   private static boolean hasAttackDamage(ItemStack s) {
      AttributeModifiersComponent mods = (AttributeModifiersComponent)s.getItem().getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (mods == null) {
         return false;
      } else {
         for (Entry e : mods.modifiers()) {
            if (e.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean keepsAfterRun(ItemStack s) {
      if (s != null && !s.isEmpty()) {
         String path = Registries.ITEM.getId(s.getItem()).getPath();
         return path.endsWith("_ingot");
      } else {
         return false;
      }
   }

   public static List<ItemStack> roll(Random rng, int floor) {
      index();
      List out = new ArrayList<>();
      int rolls = 1 + (rng.nextInt(3) == 0 ? 1 : 0) + (floor >= 8 && rng.nextInt(3) == 0 ? 1 : 0);

      for (int i = 0; i < rolls; i++) {
         float r = rng.nextFloat();
         ItemStack s;
         if (r < 0.24F) {
            s = weapon(rng, floor);
         } else if (r < 0.42F) {
            s = armorPiece(rng, floor);
         } else if (r < 0.75F) {
            s = food(rng, floor);
         } else {
            s = ingot(rng, floor);
         }

         if (s != null && !s.isEmpty()) {
            merge(out, s);
         }
      }

      if (out.isEmpty()) {
         out.add(food(rng, floor));
      }

      return out;
   }

   public static ItemStack rollKind(Random rng, int floor, int kind) {
      index();

      ItemStack s = switch (kind) {
         case 0 -> weapon(rng, floor);
         case 1 -> armorPiece(rng, floor);
         default -> food(rng, floor);
      };
      return s != null && !s.isEmpty() ? s : food(rng, floor);
   }

   private static void merge(List<ItemStack> out, ItemStack s) {
      for (ItemStack o : out) {
         if (ItemStack.areItemsAndComponentsEqual(o, s) && o.getCount() + s.getCount() <= o.getMaxCount()) {
            o.increment(s.getCount());
            return;
         }
      }

      out.add(s);
   }

   private static int tierFor(Random rng, int floor, int tiers) {
      double centre = (double)floor / 4.0 - 0.3;
      double t = centre + rng.nextGaussian() * 0.85;
      return (int)Math.max(0L, Math.min((long)(tiers - 1), Math.round(t)));
   }

   private static ItemStack weapon(Random rng, int floor) {
      WeaponStats.Tier[] tiers = WeaponStats.Tier.values();
      int want = tierFor(rng, floor, tiers.length);

      for (int d = 0; d < tiers.length; d++) {
         for (int sign : new int[]{-1, 1}) {
            int idx = want + d * sign;
            if (idx >= 0 && idx < tiers.length) {
               List pool = weapons.get(tiers[idx]);
               if (pool != null && !pool.isEmpty()) {
                  ItemStack s = new ItemStack((ItemConvertible)pool.get(rng.nextInt(pool.size())));

                  try {
                     WeaponStats.generate(rng, WeaponStats.tierOf(s)).applyToStack(s);
                  } catch (Throwable var13) {
                  }

                  return s;
               }
            }
         }
      }

      return null;
   }

   private static ItemStack armorPiece(Random rng, int floor) {
      ArmorStats.Tier[] tiers = ArmorStats.Tier.values();
      int want = tierFor(rng, floor, tiers.length);

      for (int d = 0; d < tiers.length; d++) {
         for (int sign : new int[]{-1, 1}) {
            int idx = want + d * sign;
            if (idx >= 0 && idx < tiers.length) {
               List pool = armor.get(tiers[idx]);
               if (pool != null && !pool.isEmpty()) {
                  ItemStack s = new ItemStack((ItemConvertible)pool.get(rng.nextInt(pool.size())));

                  try {
                     ArmorStats.generate(rng, ArmorStats.tierOf(s)).applyToStack(s);
                  } catch (Throwable var13) {
                  }

                  return s;
               }
            }
         }
      }

      return null;
   }

   private static ItemStack food(Random rng, int floor) {
      Item[] early = new Item[]{Items.BREAD, Items.BAKED_POTATO, Items.COOKED_CHICKEN, Items.APPLE, Items.COOKED_COD};
      Item[] later = new Item[]{Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_MUTTON, Items.PUMPKIN_PIE, Items.MUSHROOM_STEW};
      if (floor >= 12 && rng.nextInt(12) == 0) {
         return new ItemStack(Items.GOLDEN_APPLE);
      } else if (floor >= 6 && rng.nextInt(6) == 0) {
         return new ItemStack(Items.GOLDEN_CARROT, 1 + rng.nextInt(3));
      } else {
         Item[] pool = floor >= 5 && rng.nextBoolean() ? later : early;
         Item it = pool[rng.nextInt(pool.length)];
         return new ItemStack(it, it.getMaxCount() == 1 ? 1 : 1 + rng.nextInt(3));
      }
   }

   private static ItemStack ingot(Random rng, int floor) {
      float r = rng.nextFloat();
      if (floor >= 15 && r < 0.04F + (float)floor * 0.002F) {
         return new ItemStack(Items.NETHERITE_INGOT);
      } else if (r < 0.4F) {
         return new ItemStack(Items.COPPER_INGOT, 2 + rng.nextInt(4));
      } else {
         return r < 0.78F ? new ItemStack(Items.IRON_INGOT, 1 + rng.nextInt(2 + floor / 5)) : new ItemStack(Items.GOLD_INGOT, 1 + rng.nextInt(2 + floor / 6));
      }
   }
}
