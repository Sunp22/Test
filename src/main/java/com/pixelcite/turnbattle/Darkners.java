package com.pixelcite.turnbattle;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BrushItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.item.WritableBookItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public final class Darkners {
   public static EntityType<DarknerEntity> TYPE;
   private static final Random RNG = new Random();
   private static final String[] SUFFIX = new String[]{
      "inn", "y", "o", "man", "kin", "ster", "ette", "ling", "bun", "rick", "ton", "ella", "ix", "op", "zo", "let"
   };
   private static final String[] PREFIX = new String[]{"Sir ", "Captain ", "Old "};

   private Darkners() {
   }

   static void register() {
      TYPE = (EntityType<DarknerEntity>)Registry.register(
         Registries.ENTITY_TYPE,
         Identifier.of("turnbattle", "darkner"),
         Builder.create(DarknerEntity::new, SpawnGroup.MONSTER).dimensions(0.6F, 1.8F).eyeHeight(1.6F).maxTrackingRange(8).build()
      );
      FabricDefaultAttributeRegistry.register(TYPE, DarknerEntity.createAttributes());
      registerCommands();
   }

   static Darkners.Kind kindOf(Item item) {
      String id = Registries.ITEM.getId(item).getPath();
      if (new ItemStack(item).get(DataComponentTypes.FOOD) != null) {
         return Darkners.Kind.FOOD;
      } else if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || id.contains("knife") || id.contains("mace")) {
         return Darkners.Kind.BLADE;
      } else if (item instanceof MiningToolItem
         || item instanceof ShearsItem
         || item instanceof FishingRodItem
         || item instanceof FlintAndSteelItem
         || item instanceof BrushItem) {
         return Darkners.Kind.TOOL;
      } else if (item instanceof RangedWeaponItem
         || item instanceof ArrowItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || id.contains("arrow")) {
         return Darkners.Kind.RANGED;
      } else if (item instanceof PotionItem
         || item instanceof EnchantedBookItem
         || item instanceof EnderPearlItem
         || item instanceof EnderEyeItem
         || item instanceof ExperienceBottleItem
         || item instanceof WritableBookItem
         || id.equals("book")
         || id.contains("blaze")
         || id.contains("nether_star")
         || id.contains("totem")
         || id.contains("dragon")
         || id.contains("ghast_tear")
         || id.contains("echo_shard")) {
         return Darkners.Kind.MAGIC;
      } else if (id.contains("diamond")
         || id.contains("emerald")
         || id.contains("ingot")
         || id.contains("nugget")
         || id.contains("gem")
         || id.contains("crystal")
         || id.contains("quartz")
         || id.contains("lapis")
         || id.contains("redstone")
         || id.contains("amethyst")
         || id.contains("glowstone")
         || id.contains("shard")
         || id.contains("gold")
         || id.contains("raw_")
         || id.contains("dust")) {
         return Darkners.Kind.SHINY;
      } else {
         return !(item instanceof BlockItem) && !(item instanceof ArmorItem) ? Darkners.Kind.MISC : Darkners.Kind.BLOCK;
      }
   }

   private static int[] pool(Darkners.Kind k) {
      return switch (k) {
         case BLADE -> new int[]{407, 401, 409, 402, 405, 404};
         case TOOL -> new int[]{403, 401, 409, 405, 410, 411};
         case RANGED -> new int[]{400, 405, 404, 409, 410, 406};
         case FOOD -> new int[]{400, 408, 403, 401, 410, 402};
         case BLOCK -> new int[]{400, 405, 403, 411, 409, 408};
         case SHINY -> new int[]{406, 402, 411, 404, 400, 410};
         case MAGIC -> new int[]{404, 406, 402, 410, 411, 401};
         case MISC -> new int[]{400, 401, 402, 403, 404, 405, 406, 408, 409, 410, 411};
      };
   }

   static int[] patternsFor(ItemStack item, int seed) {
      int[] pool = (int[])pool(item.isEmpty() ? Darkners.Kind.MISC : kindOf(item.getItem())).clone();
      Random r = new Random((long)seed * 31L + 7L);

      for (int i = pool.length - 1; i > 0; i--) {
         int j = r.nextInt(i + 1);
         int t = pool[i];
         pool[i] = pool[j];
         pool[j] = t;
      }

      int[] out = new int[Math.min(4, pool.length)];

      for (int i = 0; i < out.length; i++) {
         out[i] = pool[i] + 1000 * r.nextInt(10);
      }

      return out;
   }

   static String nameFor(ItemStack item, int seed) {
      String full = item.isEmpty() ? "Thing" : item.getItem().getName().getString();
      String[] words = full.trim().split("\\s+");
      String word = words[words.length - 1].replaceAll("[^A-Za-z]", "");
      if (word.length() < 2) {
         word = "Thing";
      }

      word = word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
      Random r = new Random((long)seed ^ 2654435769L);
      if (r.nextInt(4) == 0) {
         return PREFIX[r.nextInt(PREFIX.length)] + word;
      } else {
         String stem = word;
         if (word.length() > 5) {
            stem = word.substring(0, 3 + r.nextInt(Math.min(3, word.length() - 4)));
         }

         String suf = SUFFIX[r.nextInt(SUFFIX.length)];
         if (isVowel(stem.charAt(stem.length() - 1)) && isVowel(suf.charAt(0))) {
            stem = stem.substring(0, stem.length() - 1);
         }

         if (stem.length() < 2) {
            stem = word.substring(0, Math.min(3, word.length()));
         }

         return stem + suf;
      }
   }

   private static boolean isVowel(char c) {
      return "aeiouAEIOU".indexOf(c) >= 0;
   }

   static List<String> scanRoom(ServerWorld room, Collection<BlockPos> airBlocks) {
      if (airBlocks != null && !airBlocks.isEmpty()) {
         int x0 = Integer.MAX_VALUE;
         int y0 = Integer.MAX_VALUE;
         int z0 = Integer.MAX_VALUE;
         int x1 = Integer.MIN_VALUE;
         int y1 = Integer.MIN_VALUE;
         int z1 = Integer.MIN_VALUE;

         for (BlockPos p : airBlocks) {
            x0 = Math.min(x0, p.getX());
            y0 = Math.min(y0, p.getY());
            z0 = Math.min(z0, p.getZ());
            x1 = Math.max(x1, p.getX());
            y1 = Math.max(y1, p.getY());
            z1 = Math.max(z1, p.getZ());
         }

         return scanBox(room, new BlockPos(x0 - 1, y0 - 1, z0 - 1), new BlockPos(x1 + 1, y1 + 1, z1 + 1));
      } else {
         return new ArrayList<>();
      }
   }

   static List<String> scanBox(ServerWorld w, BlockPos min, BlockPos max) {
      LinkedHashSet found = new LinkedHashSet<>();
      if ((long)(max.getX() - min.getX()) * (long)(max.getZ() - min.getZ()) > 16384L) {
         BlockPos c = new BlockPos((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2, (min.getZ() + max.getZ()) / 2);
         min = c.add(-32, -16, -32);
         max = c.add(32, 16, 32);
      }

      for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
         for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
            WorldChunk ch = w.getChunk(cx, cz);

            for (BlockEntity be : ch.getBlockEntities().values()) {
               BlockPos p = be.getPos();
               if (p.getX() >= min.getX()
                  && p.getX() <= max.getX()
                  && p.getY() >= min.getY()
                  && p.getY() <= max.getY()
                  && p.getZ() >= min.getZ()
                  && p.getZ() <= max.getZ()
                  && (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)) {
                  Inventory inv = (Inventory)be;

                  for (int i = 0; i < inv.size(); i++) {
                     ItemStack s = inv.getStack(i);
                     if (!s.isEmpty() && isBlock(s.getItem())) {
                        found.add(Registries.ITEM.getId(s.getItem()).toString());
                     }

                     if (found.size() >= 64) {
                        break;
                     }
                  }
               }
            }
         }
      }

      return new ArrayList<>(found);
   }

   static List<String> itemsFor(ServerWorld darkWorld) {
      DarkFountainData data = DarkFountainData.get(darkWorld.getServer());
      DarkFountainData.DarkWorldInstance inst = data.getInstanceByWorldKey(darkWorld.getRegistryKey());
      if (inst == null) {
         return Collections.emptyList();
      } else {
         if (inst.darknerItems == null) {
            List items = new ArrayList<>();

            try {
               Identifier dimId = inst.roomDimension != null ? Identifier.tryParse(inst.roomDimension) : null;
               ServerWorld room = dimId != null ? darkWorld.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId)) : null;
               if (room != null) {
                  if (inst.darkBlockPositions.isEmpty()) {
                     if (inst.roomCenter != null) {
                        items = scanBox(room, inst.roomCenter.add(-8, -4, -8), inst.roomCenter.add(8, 6, 8));
                     }
                  } else {
                     int x0 = Integer.MAX_VALUE;
                     int y0 = Integer.MAX_VALUE;
                     int z0 = Integer.MAX_VALUE;
                     int x1 = Integer.MIN_VALUE;
                     int y1 = Integer.MIN_VALUE;
                     int z1 = Integer.MIN_VALUE;

                     for (long l : inst.darkBlockPositions) {
                        BlockPos p = BlockPos.fromLong(l);
                        x0 = Math.min(x0, p.getX());
                        y0 = Math.min(y0, p.getY());
                        z0 = Math.min(z0, p.getZ());
                        x1 = Math.max(x1, p.getX());
                        y1 = Math.max(y1, p.getY());
                        z1 = Math.max(z1, p.getZ());
                     }

                     items = scanBox(room, new BlockPos(x0 - 1, y0 - 1, z0 - 1), new BlockPos(x1 + 1, y1 + 1, z1 + 1));
                  }
               }
            } catch (Exception var16) {
               TurnBattleMod.LOGGER.warn("[Darkners] couldn't check the room of Dark World {}", inst.id, var16);
            }

            inst.darknerItems = items;
            data.markDirty();
            TurnBattleMod.LOGGER.info("[Darkners] Dark World {}: {} item(s) to make Darkners from", inst.id, items.size());
         }

         List blocks = new ArrayList<>();

         for (String id : inst.darknerItems) {
            if (isBlock(id)) {
               blocks.add(id);
            }
         }

         return blocks;
      }
   }

   static boolean isBlock(Item item) {
      return item instanceof BlockItem && !(item instanceof AliasedBlockItem) && item != Items.AIR;
   }

   static boolean isBlock(String id) {
      Identifier i = Identifier.tryParse(id);
      return i != null && isBlock((Item)Registries.ITEM.get(i));
   }

   static boolean onEntityLoad(Entity e, ServerWorld w) {
      if (!(e instanceof MobEntity mob) || !(e instanceof Monster) || e instanceof DarknerEntity) {
         return false;
      }

      if (!DarkFountainManager.isDarkWorld(w.getRegistryKey())) {
         return false;
      } else if (mob.hasCustomName() && !mob.getCustomName().getString().startsWith("Lv.")) {
         return false;
      } else if (mob.isPersistent() || !mob.getCommandTags().isEmpty()) {
         return false;
      } else if (e instanceof WitherEntity || e instanceof EnderDragonEntity || e instanceof WardenEntity || e instanceof ElderGuardianEntity) {
         return false;
      } else if (BattleManager.frozenEntityIds.contains(mob.getId())) {
         return false;
      } else {
         List items = itemsFor(w);
         double x = mob.getX();
         double y = mob.getY();
         double z = mob.getZ();
         float yaw = mob.getYaw();
         w.getServer().execute(() -> {
            if (!mob.isRemoved() && !BattleManager.frozenEntityIds.contains(mob.getId())) {
               mob.discard();
               if (!items.isEmpty()) {
                  spawn(w, x, y, z, yaw, pick(items), RNG.nextInt(), SpawnReason.NATURAL);
               }
            }
         });
         return true;
      }
   }

   private static ItemStack pick(List<String> items) {
      for (int tries = 0; tries < 4; tries++) {
         Identifier id = Identifier.tryParse(items.get(RNG.nextInt(items.size())));
         if (id != null) {
            Item it = (Item)Registries.ITEM.get(id);
            if (it != Items.AIR) {
               return new ItemStack(it);
            }
         }
      }

      return ItemStack.EMPTY;
   }

   static DarknerEntity spawn(ServerWorld w, double x, double y, double z, float yaw, ItemStack item, int seed, SpawnReason reason) {
      if (item.isEmpty()) {
         return null;
      } else {
         DarknerEntity d = (DarknerEntity)TYPE.create(w);
         if (d == null) {
            return null;
         } else {
            d.refreshPositionAndAngles(x, y, z, yaw, 0.0F);
            d.initialize(w, w.getLocalDifficulty(d.getBlockPos()), reason, null);
            d.setMakeup(item, seed);
            return w.spawnEntity(d) ? d : null;
         }
      }
   }

   private static void registerCommands() {
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, reg, env) -> dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulbounddarkner")
                              .requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.literal("spawn")
                                 .then(
                                    ((RequiredArgumentBuilder)CommandManager.argument("item", ItemStackArgumentType.itemStack(reg))
                                          .executes(
                                             ctx -> spawnCmd(
                                                   (ServerCommandSource)ctx.getSource(),
                                                   ItemStackArgumentType.getItemStackArgument(ctx, "item").createStack(1, false),
                                                   RNG.nextInt()
                                                )
                                          ))
                                       .then(
                                          CommandManager.argument("seed", IntegerArgumentType.integer())
                                             .executes(
                                                ctx -> spawnCmd(
                                                      (ServerCommandSource)ctx.getSource(),
                                                      ItemStackArgumentType.getItemStackArgument(ctx, "item").createStack(1, false),
                                                      IntegerArgumentType.getInteger(ctx, "seed")
                                                   )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)CommandManager.literal("boss")
                                 .then(
                                    CommandManager.literal("beaten")
                                       .executes(
                                          ctx -> {
                                             ServerWorld w = ((ServerCommandSource)ctx.getSource()).getWorld();
                                             DarkFountainData.DarkWorldInstance inst = DarknerBosses.instanceOf(w);
                                             if (inst == null) {
                                                ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("You're not in a Dark World."));
                                                return 0;
                                             } else {
                                                if (inst.bossUuid != null
                                                   && w.getEntity(inst.bossUuid) instanceof DarknerBossEntity b
                                                   && !BattleManager.frozenEntityIds.contains(b.getId())) {
                                                   b.discard();
                                                }

                                                inst.bossState = 2;
                                                DarkFountainData.get(w.getServer()).markDirty();
                                                ((ServerCommandSource)ctx.getSource())
                                                   .sendFeedback(
                                                      () -> Text.literal("This Dark World's guardian counts as beaten. The Fountain can be sealed."), true
                                                   );
                                                return 1;
                                             }
                                          }
                                       )
                                 ))
                              .then(
                                 CommandManager.literal("reset")
                                    .executes(
                                       ctx -> {
                                          ServerWorld w = ((ServerCommandSource)ctx.getSource()).getWorld();
                                          DarkFountainData.DarkWorldInstance inst = DarknerBosses.instanceOf(w);
                                          if (inst == null) {
                                             ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("You're not in a Dark World."));
                                             return 0;
                                          } else {
                                             if (inst.bossUuid != null
                                                && w.getEntity(inst.bossUuid) instanceof DarknerBossEntity b
                                                && !BattleManager.frozenEntityIds.contains(b.getId())) {
                                                b.discard();
                                             }

                                             inst.bossState = 0;
                                             inst.bossUuid = null;
                                             DarkFountainData.get(w.getServer()).markDirty();
                                             ((ServerCommandSource)ctx.getSource())
                                                .sendFeedback(
                                                   () -> Text.literal("This Dark World's guardian will be chosen again (it appears by the Fountain shortly)."),
                                                   true
                                                );
                                             return 1;
                                          }
                                       }
                                    )
                              )
                        ))
                     .then(
                        CommandManager.literal("items")
                           .executes(
                              ctx -> {
                                 ServerWorld w = ((ServerCommandSource)ctx.getSource()).getWorld();
                                 if (!DarkFountainManager.isDarkWorld(w.getRegistryKey())) {
                                    ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("You're not in a Dark World."));
                                    return 0;
                                 } else {
                                    List items = itemsFor(w);
                                    ((ServerCommandSource)ctx.getSource())
                                       .sendFeedback(
                                          () -> Text.literal(
                                                items.isEmpty()
                                                   ? "This Dark World's room had nothing in its chests, so there are no Darkners here."
                                                   : "Darkners here are made from: " + String.join(", ", items)
                                             ),
                                          false
                                       );
                                    return items.size();
                                 }
                              }
                           )
                     )
               )
         );
   }

   private static int spawnCmd(ServerCommandSource src, ItemStack item, int seed) throws CommandSyntaxException {
      ServerPlayerEntity p = src.getPlayerOrThrow();
      DarknerEntity d = spawn(p.getServerWorld(), p.getX(), p.getY(), p.getZ(), p.getYaw() + 180.0F, item, seed, SpawnReason.COMMAND);
      if (d == null) {
         src.sendError(Text.literal("Couldn't make a Darkner from that."));
         return 0;
      } else {
         String name = d.getName().getString();
         src.sendFeedback(() -> Text.literal("Made " + name + " (seed " + seed + ")."), true);
         return 1;
      }
   }

   static enum Kind {
      BLADE,
      TOOL,
      RANGED,
      FOOD,
      BLOCK,
      SHINY,
      MAGIC,
      MISC;
   }
}
