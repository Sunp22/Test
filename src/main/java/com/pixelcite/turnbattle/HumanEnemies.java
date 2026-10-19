package com.pixelcite.turnbattle;

import com.pixelcite.turnbattle.mixin.SpawnRestrictionInvoker;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;

public final class HumanEnemies {
   public static final float SOUL_DROP_CHANCE = 0.2F;
   private static final int SPAWN_WEIGHT = 2;
   private static final double LONER_RADIUS = 48.0;
   public static final Map<HumanEnemies.Trait, EntityType<HumanEnemyEntity>> TYPES = new EnumMap<>(HumanEnemies.Trait.class);
   public static final Map<HumanEnemies.Trait, Item> WEAPONS = new EnumMap<>(HumanEnemies.Trait.class);
   public static final Map<HumanEnemies.Trait, Item> EGGS = new EnumMap<>(HumanEnemies.Trait.class);

   private HumanEnemies() {
   }

   static Item weaponItem(HumanEnemies.Trait t) {
      return WEAPONS.get(t);
   }

   public static HumanEnemies.Trait traitOf(EntityType<?> type) {
      if (type == null) {
         return null;
      } else {
         for (Entry e : TYPES.entrySet()) {
            if (e.getValue() == type) {
               return e.getKey();
            }
         }

         return null;
      }
   }

   static void register() {
      for (HumanEnemies.Trait t : HumanEnemies.Trait.values()) {
         EntityType type = (EntityType<HumanEnemyEntity>)Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("turnbattle", "human_" + t.id),
            Builder.create((et, w) -> new HumanEnemyEntity(et, w, t), SpawnGroup.MONSTER).dimensions(0.6F, 1.8F).eyeHeight(1.62F).maxTrackingRange(8).build()
         );
         TYPES.put(t, type);
         FabricDefaultAttributeRegistry.register(type, HumanEnemyEntity.createAttributes());
         Item weapon = new HumanEnemies.HumanWeaponItem(t, new Settings().attributeModifiers(SwordItem.createAttributeModifiers(t.material, t.damage, -2.2F)));
         Registry.register(Registries.ITEM, Identifier.of("turnbattle", t.weaponId), weapon);
         WEAPONS.put(t, weapon);
         Item egg = new SpawnEggItem(type, t.eggPrimary, t.eggSecondary, new Settings());
         Registry.register(Registries.ITEM, Identifier.of("turnbattle", "human_" + t.id + "_spawn_egg"), egg);
         EGGS.put(t, egg);
         BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, type, 2, 1, 1);
         SpawnRestrictionInvoker.turnbattle$invokeRegister(type, SpawnLocationTypes.ON_GROUND, Type.MOTION_BLOCKING_NO_LEAVES, HumanEnemies::canNaturallySpawn);
      }

      ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register((ModifyEntries)e -> {
         for (Item w : WEAPONS.values()) {
            e.add(w);
         }
      });
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register((ModifyEntries)e -> {
         for (Item g : EGGS.values()) {
            e.add(g);
         }
      });
   }

   static boolean canNaturallySpawn(EntityType<HumanEnemyEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
      if (!HostileEntity.canSpawnIgnoreLightLevel(type, world, reason, pos, random)) {
         return false;
      } else if (reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION) {
         return true;
      } else if (world.toServerWorld().getRegistryKey() != World.OVERWORLD) {
         return false;
      } else if (!world.isSkyVisible(pos)) {
         return false;
      } else {
         return random.nextInt(3) != 0 ? false : world.getEntitiesByClass(HumanEnemyEntity.class, new Box(pos).expand(48.0), e -> true).isEmpty();
      }
   }

   static void onDefeated(LivingEntity entity, ServerWorld world, ServerPlayerEntity killer) {
      if (entity instanceof HumanEnemyEntity human) {
         if (!(world.getRandom().nextFloat() >= 0.2F)) {
            Item soulItem = TurnBattleMod.SOUL_ITEMS.get(human.trait().soul);
            if (soulItem != null) {
               ItemStack stack = new ItemStack(soulItem);
               int rgb = human.trait().soul.getColor() & 16777215;
               if (killer != null) {
                  killer.sendMessage(
                     Text.literal("* The human's SOUL remained... ")
                        .append(
                           Text.literal(human.trait().displayName.toUpperCase(Locale.ROOT) + " SOUL")
                              .styled(st -> st.withColor(TextColor.fromRgb(rgb)).withBold(true))
                        ),
                     false
                  );
                  world.playSound(null, killer.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 0.7F);
                  if (!killer.getInventory().insertStack(stack)) {
                     killer.dropItem(stack, false);
                  }
               } else {
                  ItemEntity ie = new ItemEntity(world, entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
                  ie.setPickupDelay(10);
                  world.spawnEntity(ie);
               }
            }
         }
      }
   }

   public static final class HumanWeaponItem extends SwordItem {
      final HumanEnemies.Trait trait;

      HumanWeaponItem(HumanEnemies.Trait trait, Settings settings) {
         super(trait.material, settings);
         this.trait = trait;
      }

      public Text getName(ItemStack stack) {
         return Text.literal(this.trait.weaponName);
      }

      public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
         tooltip.add(Text.literal(this.trait.weaponLore).formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}));
         tooltip.add(
            Text.literal("Carried by a human of " + this.trait.displayName + ".")
               .styled(st -> st.withColor(TextColor.fromRgb(this.trait.soul.getColor() & 16777215)))
         );
         super.appendTooltip(stack, context, tooltip, type);
      }
   }

   public static enum Trait {
      PATIENCE(
         SoulType.PATIENCE,
         "patience",
         "Patience",
         "toy_knife",
         "Toy Knife",
         ToolMaterials.WOOD,
         2,
         Element.ICE,
         4114656,
         15760040,
         "Patient: its CYAN attacks only hurt while your SOUL moves - wait them out.",
         "Plastic, a little chewed, still pointy."
      ),
      BRAVERY(
         SoulType.BRAVERY,
         "bravery",
         "Bravery",
         "tough_glove",
         "Tough Glove",
         ToolMaterials.STONE,
         2,
         Element.FIRE,
         15759904,
         1972248,
         "Brave: its ORANGE attacks only hurt while your SOUL stands still - keep moving.",
         "The padding's worn thin from a lot of scraps."
      ),
      INTEGRITY(
         SoulType.INTEGRITY,
         "integrity",
         "Integrity",
         "ballet_shoes",
         "Ballet Shoes",
         ToolMaterials.WOOD,
         3,
         Element.ICE,
         3042008,
         9226495,
         "Graceful: can turn your SOUL blue - gravity takes hold.",
         "Ribbons tied in a perfect bow. Every kick lands just so."
      ),
      PERSEVERANCE(
         SoulType.PERSEVERANCE,
         "perseverance",
         "Perseverance",
         "torn_notebook",
         "Torn Notebook",
         ToolMaterials.WOOD,
         2,
         Element.DARK,
         8010432,
         16053492,
         "Persistent: can turn your SOUL purple - you're bound to the lines.",
         "Half the pages are gone. The rest are covered in notes."
      ),
      KINDNESS(
         SoulType.KINDNESS,
         "kindness",
         "Kindness",
         "burnt_pan",
         "Burnt Pan",
         ToolMaterials.IRON,
         1,
         Element.NATURE,
         4106826,
         16051932,
         "Kind: can turn your SOUL green - stand your ground behind the shield.",
         "Scorched black. Still smells a little of breakfast."
      ),
      JUSTICE(
         SoulType.JUSTICE,
         "justice",
         "Justice",
         "empty_gun",
         "Empty Gun",
         ToolMaterials.IRON,
         2,
         Element.LIGHT,
         15253536,
         9067050,
         "Just: can turn your SOUL yellow - shoot back.",
         "Not a single round left in it. Supposedly."
      ),
      DETERMINATION(
         SoulType.DETERMINATION,
         "determination",
         "Determination",
         "real_knife",
         "Real Knife",
         ToolMaterials.IRON,
         4,
         Element.FIRE,
         14166570,
         8014382,
         "Determined: its slashes come in threes - stand in the gaps between them.",
         "Sharp. Very, very sharp."
      );

      public final SoulType soul;
      public final String id;
      public final String displayName;
      public final String weaponId;
      public final String weaponName;
      final ToolMaterial material;
      final int damage;
      public final Element element;
      final int eggPrimary;
      final int eggSecondary;
      public final String passive;
      public final String weaponLore;

      private Trait(
         SoulType soul,
         String id,
         String displayName,
         String weaponId,
         String weaponName,
         ToolMaterial material,
         int damage,
         Element element,
         int eggPrimary,
         int eggSecondary,
         String passive,
         String weaponLore
      ) {
         this.soul = soul;
         this.id = id;
         this.displayName = displayName;
         this.weaponId = weaponId;
         this.weaponName = weaponName;
         this.material = material;
         this.damage = damage;
         this.element = element;
         this.eggPrimary = eggPrimary;
         this.eggSecondary = eggSecondary;
         this.passive = passive;
         this.weaponLore = weaponLore;
      }

      public String mobName() {
         return "Human (" + this.displayName + ")";
      }
   }
}
