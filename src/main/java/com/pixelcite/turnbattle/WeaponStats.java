package com.pixelcite.turnbattle;

import java.util.Locale;
import java.util.Random;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class WeaponStats {
   public static final String NBT_KEY = "turnbattle";
   public static final String PENDING_KEY = "turnbattle_pending";
   public static final String[] STAT_NAMES = new String[]{"Offense", "Defense", "Magic", "IQ", "Vitality", "Luck"};
   static final int STATS_VERSION = 2;
   public int baseDamage;
   public boolean magic;
   public static final float MAGIC_SHARE = 0.65F;
   public int bonusStat;
   public int bonusAmt;
   public boolean isQuirk;
   public int quirkStat;
   public int quirkMinus;
   public Affinity element1 = null;
   public Affinity element2 = null;

   private static int remap(int v, int oldLo, int oldHi, int lo, int hi) {
      float f = oldHi > oldLo ? (float)(v - oldLo) / (float)(oldHi - oldLo) : 0.5F;
      f = Math.max(0.0F, Math.min(1.0F, f));
      return lo + Math.round(f * (float)(hi - lo));
   }

   public static WeaponStats.Tier tierOf(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Identifier id = Registries.ITEM.getId(stack.getItem());
         String path = id.getPath().toLowerCase(Locale.ROOT);
         if (path.contains("netherite")) {
            return WeaponStats.Tier.NETHERITE;
         } else if (path.equals("real_knife")) {
            return WeaponStats.Tier.DIAMOND;
         } else if (path.equals("empty_gun") || path.equals("burnt_pan") || path.equals("tough_glove")) {
            return WeaponStats.Tier.IRON;
         } else if (path.contains("diamond")) {
            return WeaponStats.Tier.DIAMOND;
         } else if (path.contains("iron") || path.contains("steel") || path.contains("metal") || path.contains("chainmail")) {
            return WeaponStats.Tier.IRON;
         } else if (path.startsWith("stone_") || path.contains("_stone_") || path.contains("cobblestone") || path.contains("flint") || path.contains("copper")) {
            return WeaponStats.Tier.STONE;
         } else if (!path.contains("wood") && !path.contains("gold")) {
            if (stack.getItem() instanceof ToolItem ti && ti.getMaterial() instanceof ToolMaterials tm) {
               return switch (tm) {
                  case WOOD, GOLD -> WeaponStats.Tier.WOOD;
                  case STONE -> WeaponStats.Tier.STONE;
                  case IRON -> WeaponStats.Tier.IRON;
                  case DIAMOND -> WeaponStats.Tier.DIAMOND;
                  case NETHERITE -> WeaponStats.Tier.NETHERITE;
                  default -> throw new MatchException(null, null);
               };
            }

            double dmg = attackDamageOf(stack);
            if (dmg <= 0.0) {
               return WeaponStats.Tier.WOOD;
            } else if (dmg < 4.5) {
               return WeaponStats.Tier.WOOD;
            } else if (dmg < 5.5) {
               return WeaponStats.Tier.STONE;
            } else if (dmg < 6.5) {
               return WeaponStats.Tier.IRON;
            } else {
               return dmg < 7.5 ? WeaponStats.Tier.DIAMOND : WeaponStats.Tier.NETHERITE;
            }
         } else {
            return WeaponStats.Tier.WOOD;
         }
      } else {
         return WeaponStats.Tier.WOOD;
      }
   }

   static double attackDamageOf(ItemStack s) {
      AttributeModifiersComponent mods = (AttributeModifiersComponent)s.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (mods == null) {
         return 0.0;
      } else {
         double v = 1.0;
         boolean any = false;

         for (Entry e : mods.modifiers()) {
            if (e.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && e.modifier().operation() == Operation.ADD_VALUE) {
               v += e.modifier().value();
               any = true;
            }
         }

         return any ? v : 0.0;
      }
   }

   public int bashBonus() {
      return this.magic ? 0 : this.baseDamage;
   }

   public static WeaponStats generate(Random rng, WeaponStats.Tier tier) {
      WeaponStats ws = new WeaponStats();
      ws.baseDamage = tier.dmgMin + rng.nextInt(tier.dmgMax - tier.dmgMin + 1);
      ws.magic = rng.nextBoolean();
      if (ws.magic) {
         ws.baseDamage = Math.max(1, Math.round((float)ws.baseDamage * 0.65F));
      }

      ws.bonusStat = rng.nextInt(6);
      ws.bonusAmt = tier.min + rng.nextInt(tier.max - tier.min + 1);
      ws.isQuirk = tier.canHaveOddity && rng.nextFloat() < 0.1F;
      if (ws.isQuirk) {
         ws.quirkStat = rng.nextInt(6);

         do {
            ws.quirkMinus = rng.nextInt(6);
         } while (ws.quirkMinus == ws.quirkStat);
      }

      Affinity[] allAffinities = Affinity.values();
      if (rng.nextFloat() < 0.2F) {
         ws.element1 = allAffinities[rng.nextInt(allAffinities.length)];
         if (rng.nextFloat() < 0.2F) {
            Affinity second;
            do {
               second = allAffinities[rng.nextInt(allAffinities.length)];
            } while (second == ws.element1);

            ws.element2 = second;
         }
      }

      return ws;
   }

   public static WeaponStats generate(Random rng) {
      return generate(rng, WeaponStats.Tier.WOOD);
   }

   public NbtCompound toNbt() {
      NbtCompound t = new NbtCompound();
      t.putInt("v", 2);
      t.putInt("baseDamage", this.baseDamage);
      if (this.magic) {
         t.putBoolean("magic", true);
      }

      t.putInt("bonusStat", this.bonusStat);
      t.putInt("bonusAmt", this.bonusAmt);
      t.putBoolean("isQuirk", this.isQuirk);
      if (this.isQuirk) {
         t.putInt("quirkStat", this.quirkStat);
         t.putInt("quirkMinus", this.quirkMinus);
      }

      if (this.element1 != null) {
         t.putString("element1", this.element1.name());
      }

      if (this.element2 != null) {
         t.putString("element2", this.element2.name());
      }

      return t;
   }

   public static WeaponStats fromNbt(NbtCompound t) {
      WeaponStats ws = new WeaponStats();
      ws.baseDamage = t.getInt("baseDamage");
      ws.magic = t.getBoolean("magic");
      ws.bonusStat = t.getInt("bonusStat");
      ws.bonusAmt = t.getInt("bonusAmt");
      ws.isQuirk = t.getBoolean("isQuirk");
      if (ws.isQuirk) {
         ws.quirkStat = t.getInt("quirkStat");
         ws.quirkMinus = t.getInt("quirkMinus");
      }

      if (t.contains("element1")) {
         try {
            ws.element1 = Affinity.valueOf(t.getString("element1"));
         } catch (IllegalArgumentException var4) {
         }
      }

      if (t.contains("element2")) {
         try {
            ws.element2 = Affinity.valueOf(t.getString("element2"));
         } catch (IllegalArgumentException var3) {
         }
      }

      return ws;
   }

   public static WeaponStats fromStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         NbtCompound tag = stack.get(DataComponentTypes.CUSTOM_DATA) != null
            ? ((NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA)).getNbt().getCompound("turnbattle")
            : null;
         if (tag != null && tag.contains("baseDamage")) {
            WeaponStats ws = fromNbt(tag);
            if (tag.getInt("v") < 2) {
               WeaponStats.Tier t = tierOf(stack);
               ws.baseDamage = remap(ws.baseDamage, t.oldDmgMin, t.oldDmgMax, t.dmgMin, t.dmgMax);
               ws.bonusAmt = remap(ws.bonusAmt, t.oldMin, t.oldMax, t.min, t.max);
            }

            return ws;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public void applyToStack(ItemStack stack) {
      NbtCompound _root = new NbtCompound();
      _root.put("turnbattle", this.toNbt());
      stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(_root));
      NbtComponent _rcd = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      if (_rcd != null) {
         NbtCompound _rnbt = _rcd.copyNbt();
         _rnbt.remove("turnbattle_pending");
         stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(_rnbt));
      }
   }

   public static void markPending(ItemStack stack) {
      NbtComponent _existingCD = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      NbtCompound _proot = _existingCD != null ? _existingCD.copyNbt() : new NbtCompound();
      NbtCompound _pnbt = new NbtCompound();
      _pnbt.putBoolean("pending", true);
      _proot.put("turnbattle_pending", _pnbt);
      stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(_proot));
   }

   public static boolean isPending(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         NbtCompound tag = stack.get(DataComponentTypes.CUSTOM_DATA) != null
            ? ((NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA)).getNbt().getCompound("turnbattle_pending")
            : null;
         return tag != null && tag.getBoolean("pending");
      } else {
         return false;
      }
   }

   public static boolean revealStats(ItemStack stack, Random rng) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (fromStack(stack) != null) {
         return false;
      } else {
         WeaponStats ws = generate(rng, tierOf(stack));
         ws.applyToStack(stack);
         return true;
      }
   }

   public int netStatBonus(int statIdx) {
      int result = 0;
      if (statIdx == this.bonusStat) {
         result += this.bonusAmt;
      }

      if (this.isQuirk) {
         if (statIdx == this.quirkStat) {
            result += 10;
         }

         if (statIdx == this.quirkMinus) {
            result -= 10;
         }
      }

      return result;
   }

   public String shortDesc() {
      StringBuilder sb = new StringBuilder();
      sb.append("+").append(this.baseDamage).append(this.magic ? " Magic DMG" : " DMG");
      sb.append(", +").append(this.bonusAmt).append(" ").append(STAT_NAMES[this.bonusStat]);
      if (this.isQuirk) {
         sb.append(" / +10 ").append(STAT_NAMES[this.quirkStat]).append(" / -10 ").append(STAT_NAMES[this.quirkMinus]).append(" [ODDITY]");
      }

      if (this.element1 != null) {
         sb.append(", ").append(this.element1.displayName);
         if (this.element2 != null) {
            sb.append("/").append(this.element2.displayName);
         }
      }

      return sb.toString();
   }

   public Affinity[] getElements() {
      if (this.element1 == null) {
         return new Affinity[0];
      } else {
         return this.element2 == null ? new Affinity[]{this.element1} : new Affinity[]{this.element1, this.element2};
      }
   }

   public boolean hasElement(Affinity a) {
      return a != null && (a == this.element1 || a == this.element2);
   }

   public static enum Tier {
      WOOD(3, 6, 1, 3, false, 2, 5, 1, 3),
      STONE(9, 13, 4, 6, false, 4, 8, 2, 5),
      IRON(20, 26, 7, 10, false, 6, 11, 3, 6),
      DIAMOND(40, 50, 12, 16, true, 10, 16, 6, 9),
      NETHERITE(75, 90, 18, 23, true, 16, 20, 9, 12);

      public final int dmgMin;
      public final int dmgMax;
      public final int min;
      public final int max;
      public final boolean canHaveOddity;
      final int oldDmgMin;
      final int oldDmgMax;
      final int oldMin;
      final int oldMax;

      private Tier(int dmgMin, int dmgMax, int min, int max, boolean canHaveOddity, int oldDmgMin, int oldDmgMax, int oldMin, int oldMax) {
         this.dmgMin = dmgMin;
         this.dmgMax = dmgMax;
         this.min = min;
         this.max = max;
         this.canHaveOddity = canHaveOddity;
         this.oldDmgMin = oldDmgMin;
         this.oldDmgMax = oldDmgMax;
         this.oldMin = oldMin;
         this.oldMax = oldMax;
      }
   }
}
