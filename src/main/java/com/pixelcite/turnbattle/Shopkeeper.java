package com.pixelcite.turnbattle;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.IntBinaryOperator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;

public final class Shopkeeper {
   public static EntityType<Shopkeeper.Entity> TYPE;
   public static final Identifier SHOP_OPEN = Identifier.of("turnbattle", "shop_open");
   public static final Identifier SHOP_BUY = Identifier.of("turnbattle", "shop_buy");
   public static final Identifier SHOP_RESULT = Identifier.of("turnbattle", "shop_result");
   static final long RESTOCK_TICKS = 12000L;
   static final double MOVE_RANGE = 300.0;
   static final long MOVE_AFTER = 6000L;
   private static final Random RNG = new Random();
   private static final int[] WEAPON_PRICE = new int[]{30, 80, 260, 1000, 4200};
   private static final int[] ARMOR_PRICE = new int[]{22, 60, 190, 750, 3200};
   private static final String[] GREETINGS = new String[]{
      "Welcome, welcome! Everything's for sale, if you've got the D$.",
      "Ah, a customer! Have a look, nothing bites. Mostly.",
      "Fresh stock, straight from who knows where. Browse away!",
      "You look like someone with D$ to spend. Am I right?"
   };

   private Shopkeeper() {
   }

   static void register() {
      TYPE = (EntityType<Shopkeeper.Entity>)Registry.register(
         Registries.ENTITY_TYPE,
         Identifier.of("turnbattle", "shopkeeper"),
         Builder.create(Shopkeeper.Entity::new, SpawnGroup.MISC).dimensions(0.6F, 1.8F).eyeHeight(1.62F).maxTrackingRange(10).build()
      );
      FabricDefaultAttributeRegistry.register(TYPE, MobEntity.createMobAttributes());
      Net121.registerS2CChannel(SHOP_OPEN);
      Net121.registerS2CChannel(SHOP_RESULT);
      Net121.registerC2SChannel(SHOP_BUY);
      Net121.registerServerReceiver(SHOP_BUY, (server, player, handler, buf, sender) -> {
         int entityId = buf.readVarInt();
         int index = buf.readVarInt();
         server.execute(() -> buy(player, entityId, index));
      });
      registerCommands();
   }

   private static void registerCommands() {
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, reg, env) -> dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal(
                                    "soulboundmoney"
                                 )
                                 .executes(ctx -> {
                                    ServerPlayerEntity self = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
                                    int have = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer()).getOrCreate(self.getUuid()).darkDollars;
                                    ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("You have " + have + " D$."), false);
                                    return have;
                                 }))
                              .then(
                                 CommandManager.literal("pay")
                                    .then(
                                       CommandManager.argument("player", EntityArgumentType.player())
                                          .then(
                                             CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(
                                                   ctx -> pay(
                                                         (ServerCommandSource)ctx.getSource(),
                                                         EntityArgumentType.getPlayer(ctx, "player"),
                                                         IntegerArgumentType.getInteger(ctx, "amount")
                                                      )
                                                )
                                          )
                                    )
                              ))
                           .then(adminMoney("add", 1, (have, amt) -> (int)Math.min(2147483647L, (long)have + (long)amt))))
                        .then(adminMoney("remove", 1, (have, amt) -> Math.max(0, have - amt))))
                     .then(adminMoney("set", 0, (have, amt) -> amt))
               )
         );
   }

   private static LiteralArgumentBuilder<ServerCommandSource> adminMoney(String verb, int min, IntBinaryOperator change) {
      return (LiteralArgumentBuilder<ServerCommandSource>)((LiteralArgumentBuilder)CommandManager.literal(verb).requires(src -> src.hasPermissionLevel(2)))
         .then(
            CommandManager.argument("players", EntityArgumentType.players())
               .then(CommandManager.argument("amount", IntegerArgumentType.integer(min)).executes(ctx -> {
                  int amt = IntegerArgumentType.getInteger(ctx, "amount");
                  Collection targets = EntityArgumentType.getPlayers(ctx, "players");
                  BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());

                  for (ServerPlayerEntity t : targets) {
                     BattleData.PlayerProgress prog = data.getOrCreate(t.getUuid());
                     prog.darkDollars = change.applyAsInt(prog.darkDollars, amt);
                     TurnBattleMod.sendStatsExtra(t);
                     int now = prog.darkDollars;
                     String name = t.getName().getString();
                     ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> {
                        return Text.literal(switch (verb) {
                           case "add" -> "Gave " + amt + " D$ to " + name + " (now " + now + " D$).";
                           case "remove" -> "Took " + amt + " D$ from " + name + " (now " + now + " D$).";
                           default -> "Set " + name + "'s D$ to " + now + ".";
                        });
                     }, true);
                  }

                  data.markDirty();
                  return targets.size();
               }))
         );
   }

   private static int pay(ServerCommandSource src, ServerPlayerEntity to, int amount) throws CommandSyntaxException {
      ServerPlayerEntity from = src.getPlayerOrThrow();
      if (from.getUuid().equals(to.getUuid())) {
         src.sendError(Text.literal("You can't pay yourself."));
         return 0;
      } else {
         BattleData data = BattleData.get(src.getServer());
         BattleData.PlayerProgress mine = data.getOrCreate(from.getUuid());
         if (mine.darkDollars < amount) {
            src.sendError(Text.literal("You only have " + mine.darkDollars + " D$."));
            return 0;
         } else {
            BattleData.PlayerProgress theirs = data.getOrCreate(to.getUuid());
            mine.darkDollars -= amount;
            theirs.darkDollars = (int)Math.min(2147483647L, (long)theirs.darkDollars + (long)amount);
            data.markDirty();
            TurnBattleMod.sendStatsExtra(from);
            TurnBattleMod.sendStatsExtra(to);
            src.sendFeedback(
               () -> Text.literal("Paid " + amount + " D$ to " + to.getName().getString() + ". You have " + mine.darkDollars + " D$ left."), false
            );
            to.sendMessage(Text.literal(from.getName().getString() + " paid you " + amount + " D$.").formatted(Formatting.YELLOW), false);
            return amount;
         }
      }
   }

   public static void tick(MinecraftServer server) {
      if (server.getTicks() % 200 == 61) {
         long now = server.getOverworld().getTime();

         for (ServerWorld w : server.getWorlds()) {
            if (DarkFountainManager.isDarkWorld(w.getRegistryKey()) && !w.getPlayers().isEmpty()) {
               try {
                  tickWorld(w, now);
               } catch (Exception var6) {
                  TurnBattleMod.LOGGER.warn("[Shopkeeper] couldn't look after {}'s booth", w.getRegistryKey().getValue(), var6);
               }
            }
         }
      }
   }

   private static void tickWorld(ServerWorld w, long now) {
      Shopkeeper.ShopData d = Shopkeeper.ShopData.get(w);
      if (d.current != null && d.pos != null) {
         boolean near = false;

         for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.getBlockPos().getSquaredDistance(d.pos) < 90000.0) {
               near = true;
               break;
            }
         }

         if (near) {
            d.lastNear = now;
            d.markDirty();
            return;
         }

         if (now - d.lastNear < 6000L) {
            return;
         }

         if (w.getEntity(d.current) instanceof Shopkeeper.Entity e) {
            packUp(w, e);
         }

         d.current = null;
         d.pos = null;
         d.markDirty();
      }

      if (RNG.nextInt(3) == 0) {
         List ps = w.getPlayers();
         ServerPlayerEntity px = ps.get(RNG.nextInt(ps.size()));
         if (!BattleManager.isInBattle(px.getUuid())) {
            for (int attempt = 0; attempt < 14; attempt++) {
               double ang = RNG.nextDouble() * Math.PI * 2.0;
               double dist = 22.0 + RNG.nextDouble() * 22.0;
               int x = (int)Math.floor(px.getX() + Math.cos(ang) * dist);
               int z = (int)Math.floor(px.getZ() + Math.sin(ang) * dist);
               Direction facing = Direction.fromRotation(Math.toDegrees(Math.atan2(px.getZ() - (double)z, px.getX() - (double)x)) - 90.0);
               BlockPos at = boothSpot(w, x, z, facing);
               if (at != null) {
                  spawn(w, at, facing, d, now);
                  return;
               }
            }
         }
      }
   }

   private static BlockPos boothSpot(ServerWorld w, int x, int z, Direction facing) {
      if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
         return null;
      } else {
         int y = w.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, x, z);
         if (y > w.getBottomY() + 2 && y < w.getTopY() - 6) {
            BlockPos base = new BlockPos(x, y, z);
            Direction right = facing.rotateYClockwise();

            for (int u = -2; u <= 2; u++) {
               for (int f = -1; f <= 1; f++) {
                  BlockPos c = base.offset(right, u).offset(facing, f);
                  if (!w.getChunkManager().isChunkLoaded(c.getX() >> 4, c.getZ() >> 4)) {
                     return null;
                  }

                  BlockState ground = w.getBlockState(c.down());
                  if (!ground.isSolidBlock(w, c.down()) || !ground.getFluidState().isEmpty()) {
                     return null;
                  }

                  if (ground.isOf(TurnBattleMod.DARK_FOUNTAIN_BLOCK)) {
                     return null;
                  }

                  for (int h = 0; h < 4; h++) {
                     BlockState s = w.getBlockState(c.up(h));
                     if (!s.isAir() && !s.isReplaceable()) {
                        return null;
                     }

                     if (!s.getFluidState().isEmpty()) {
                        return null;
                     }
                  }
               }
            }

            return base;
         } else {
            return null;
         }
      }
   }

   private static void spawn(ServerWorld w, BlockPos at, Direction facing, Shopkeeper.ShopData d, long now) {
      Shopkeeper.Entity e = (Shopkeeper.Entity)TYPE.create(w);
      if (e != null) {
         e.refreshPositionAndAngles((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5, facing.asRotation(), 0.0F);
         e.setHeadYaw(facing.asRotation());
         e.setBodyYaw(facing.asRotation());
         DarkFountainData.DarkWorldInstance inst = DarkFountainData.get(w.getServer()).getInstanceByWorldKey(w.getRegistryKey());
         e.areaLevel = inst != null ? Math.max(1, inst.mobLevel) : 10;
         buildBooth(w, at, facing, e);
         if (!w.spawnEntity(e)) {
            takeDown(w, e);
         } else {
            d.current = e.getUuid();
            d.pos = at;
            d.lastNear = now;
            d.markDirty();
         }
      }
   }

   private static void buildBooth(ServerWorld w, BlockPos at, Direction facing, Shopkeeper.Entity e) {
      Direction right = facing.rotateYClockwise();

      for (int u = -1; u <= 1; u++) {
         place(w, at.offset(right, u).offset(facing, 1), Blocks.DARK_OAK_PLANKS.getDefaultState(), e);
      }

      place(w, at.offset(right, -1), Blocks.BARREL.getDefaultState(), e);
      place(w, at.offset(right, 1), Blocks.BARREL.getDefaultState(), e);

      for (int u : new int[]{-2, 2}) {
         for (int f : new int[]{-1, 1}) {
            for (int h = 0; h < 3; h++) {
               place(w, at.offset(right, u).offset(facing, f).up(h), Blocks.DARK_OAK_FENCE.getDefaultState(), e);
            }
         }
      }

      for (int u = -2; u <= 2; u++) {
         for (int f = -1; f <= 1; f++) {
            BlockState wool = ((u + 2) % 2 == 0 ? Blocks.PURPLE_WOOL : Blocks.BLACK_WOOL).getDefaultState();
            place(w, at.offset(right, u).offset(facing, f).up(3), wool, e);
         }
      }

      place(w, at.offset(facing, 1).up(2), (BlockState)Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true), e);
   }

   private static void place(ServerWorld w, BlockPos p, BlockState s, Shopkeeper.Entity e) {
      BlockState was = w.getBlockState(p);
      if (was.isAir() || was.isReplaceable()) {
         w.setBlockState(p, s, 3);
         e.booth.add(new long[]{p.asLong()});
         e.boothBlocks.add(Registries.BLOCK.getId(s.getBlock()).toString());
      }
   }

   static void packUp(ServerWorld w, Shopkeeper.Entity e) {
      takeDown(w, e);
      e.discard();
   }

   private static void takeDown(ServerWorld w, Shopkeeper.Entity e) {
      for (int i = 0; i < e.booth.size(); i++) {
         BlockPos p = BlockPos.fromLong(e.booth.get(i)[0]);
         String id = i < e.boothBlocks.size() ? e.boothBlocks.get(i) : "";
         BlockState now = w.getBlockState(p);
         if (Registries.BLOCK.getId(now.getBlock()).toString().equals(id)) {
            if (now.isOf(Blocks.BARREL)) {
               BlockEntity be = w.getBlockEntity(p);
               if (be instanceof Inventory) {
                  Inventory inv = (Inventory)be;
                  if (!inv.isEmpty()) {
                     continue;
                  }
               }
            }

            w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
         }
      }

      e.booth.clear();
      e.boothBlocks.clear();
   }

   static void restock(Shopkeeper.Entity e) {
      e.wares.clear();
      int floor = Math.max(0, Math.min(24, e.areaLevel / 5));

      for (int i = 0; i < 2; i++) {
         addGear(e, DepthsLoot.rollKind(RNG, Math.max(0, floor + RNG.nextInt(3) - 1), 0));
      }

      for (int i = 0; i < 2; i++) {
         addGear(e, DepthsLoot.rollKind(RNG, Math.max(0, floor + RNG.nextInt(3) - 1), 1));
      }

      for (int i = 0; i < 2; i++) {
         ItemStack food = DepthsLoot.rollKind(RNG, floor, 2);
         if (!food.isEmpty()) {
            food.setCount(1);
            boolean dup = false;

            for (Shopkeeper.Ware w : e.wares) {
               if (ItemStack.areItemsEqual(w.stack, food)) {
                  dup = true;
               }
            }

            if (!dup) {
               e.wares.add(new Shopkeeper.Ware(food, foodPrice(food), -1));
            }
         }
      }

      e.wares.add(new Shopkeeper.Ware(new ItemStack(Items.ENDER_PEARL), 150, -1));
      e.wares.add(new Shopkeeper.Ware(new ItemStack(Items.BLAZE_ROD), 250, -1));
      e.wares.add(new Shopkeeper.Ware(new ItemStack(Items.WITHER_SKELETON_SKULL), 1500, -1));
   }

   private static void addGear(Shopkeeper.Entity e, ItemStack s) {
      if (s != null && !s.isEmpty()) {
         if (s.get(DataComponentTypes.FOOD) != null && !ArmorStats.isArmorItem(s)) {
            s.setCount(1);

            for (Shopkeeper.Ware w : e.wares) {
               if (ItemStack.areItemsEqual(w.stack, s)) {
                  return;
               }
            }

            e.wares.add(new Shopkeeper.Ware(s, foodPrice(s), -1));
         } else {
            int price;
            if (ArmorStats.isArmorItem(s)) {
               ArmorStats.Tier t = ArmorStats.tierOf(s);
               price = ARMOR_PRICE[t == null ? 0 : Math.min(ARMOR_PRICE.length - 1, t.ordinal())];
               ArmorStats as = ArmorStats.fromStack(s);
               if (as != null && t != null && t.max > t.min) {
                  price = Math.round((float)price * (0.9F + 0.2F * (float)(as.bonusAmt - t.min) / (float)(t.max - t.min)));
               }
            } else {
               WeaponStats.Tier t = WeaponStats.tierOf(s);
               price = WEAPON_PRICE[Math.min(WEAPON_PRICE.length - 1, t.ordinal())];
               WeaponStats ws = WeaponStats.fromStack(s);
               if (ws != null && ws.isQuirk) {
                  price = Math.round((float)price * 1.25F);
               }
            }

            e.wares.add(new Shopkeeper.Ware(s, roundPrice(price), 1));
         }
      }
   }

   private static int foodPrice(ItemStack s) {
      if (s.isOf(Items.GOLDEN_APPLE)) {
         return 220;
      } else if (s.isOf(Items.GOLDEN_CARROT)) {
         return 45;
      } else {
         FoodComponent food = (FoodComponent)s.get(DataComponentTypes.FOOD);
         return food != null ? Math.max(6, food.nutrition() * 5) : 10;
      }
   }

   private static int roundPrice(int p) {
      if (p >= 1000) {
         return Math.round((float)p / 50.0F) * 50;
      } else {
         return p >= 100 ? Math.round((float)p / 10.0F) * 10 : Math.max(1, Math.round((float)p / 2.0F) * 2);
      }
   }

   static void open(ServerPlayerEntity player, Shopkeeper.Entity e) {
      long now = player.getServer().getOverworld().getTime();
      if (e.wares.isEmpty() || now >= e.restockAt) {
         restock(e);
         e.restockAt = now + 12000L;
      }

      PacketByteBuf b = PacketByteBufs.create();
      b.writeVarInt(e.getId());
      b.writeVarInt(BattleData.get(player.getServer()).getOrCreate(player.getUuid()).darkDollars);
      writeWares(b, e, player);
      b.writeString(GREETINGS[RNG.nextInt(GREETINGS.length)]);
      b.writeVarInt((int)Math.max(0L, (e.restockAt - now) / 20L));
      Net121.sendToClient(player, SHOP_OPEN, b);
   }

   private static void writeWares(PacketByteBuf b, Shopkeeper.Entity e, ServerPlayerEntity player) {
      b.writeVarInt(e.wares.size());

      for (Shopkeeper.Ware w : e.wares) {
         b.writeNbt((NbtCompound)w.stack.encode(player.getServer().getRegistryManager()));
         b.writeVarInt(w.price);
         b.writeVarInt(w.stock);
      }
   }

   private static void buy(ServerPlayerEntity player, int entityId, int index) {
      if (player.getWorld().getEntityById(entityId) instanceof Shopkeeper.Entity e) {
         if (!(e.squaredDistanceTo(player) > 100.0)) {
            BattleData data = BattleData.get(player.getServer());
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            boolean bought = false;
            String msg;
            if (index >= 0 && index < e.wares.size()) {
               Shopkeeper.Ware w = e.wares.get(index);
               if (w.stock == 0) {
                  msg = "Sorry, that one's sold out.";
               } else if (prog.darkDollars < w.price) {
                  msg = "You don't have enough D$ for that.";
               } else {
                  prog.darkDollars = prog.darkDollars - w.price;
                  if (w.stock > 0) {
                     w.stock--;
                  }

                  data.markDirty();
                  ItemStack give = w.stack.copy();
                  if (!player.getInventory().insertStack(give) && !give.isEmpty()) {
                     player.dropItem(give, false);
                  }

                  player.playerScreenHandler.sendContentUpdates();
                  msg = new String[]{"Thanks for that!", "Pleasure doing business!", "Good choice. Treat it well!"}[RNG.nextInt(3)];
                  bought = true;
               }
            } else {
               msg = "Hm? I don't have that.";
            }

            PacketByteBuf b = PacketByteBufs.create();
            b.writeVarInt(prog.darkDollars);
            writeWares(b, e, player);
            b.writeString(msg);
            b.writeBoolean(bought);
            Net121.sendToClient(player, SHOP_RESULT, b);
            TurnBattleMod.sendStatsExtra(player);
         }
      }
   }

   public static class Entity extends PathAwareEntity {
      final List<Shopkeeper.Ware> wares = new ArrayList<>();
      long restockAt = -1L;
      int areaLevel = 10;
      final List<long[]> booth = new ArrayList<>();
      final List<String> boothBlocks = new ArrayList<>();

      public Entity(EntityType<? extends PathAwareEntity> type, World world) {
         super(type, world);
         this.setAiDisabled(true);
         this.setInvulnerable(true);
         this.setPersistent();
      }

      protected void initGoals() {
      }

      public boolean isInvulnerableTo(DamageSource source) {
         return true;
      }

      public boolean damage(DamageSource source, float amount) {
         return false;
      }

      public boolean isPushable() {
         return false;
      }

      protected void pushAway(net.minecraft.entity.Entity entity) {
      }

      public void checkDespawn() {
      }

      protected ActionResult interactMob(PlayerEntity player, Hand hand) {
         if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
         } else {
            if (!this.getWorld().isClient() && player instanceof ServerPlayerEntity sp) {
               Shopkeeper.open(sp, this);
            }

            return ActionResult.SUCCESS;
         }
      }

      public void tick() {
         super.tick();
         this.setVelocity(0.0, this.getVelocity().y, 0.0);
         if (this.getWorld() instanceof ServerWorld sw && this.age % 20 == 0) {
            Shopkeeper.ShopData d = Shopkeeper.ShopData.get(sw);
            if (!this.getUuid().equals(d.current)) {
               Shopkeeper.packUp(sw, this);
               return;
            }

            PlayerEntity p = sw.getClosestPlayer(this, 7.0);
            if (p != null) {
               double dx = p.getX() - this.getX();
               double dz = p.getZ() - this.getZ();
               float want = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
               this.setHeadYaw(want);
            }
         }
      }

      public void writeCustomDataToNbt(NbtCompound nbt) {
         super.writeCustomDataToNbt(nbt);
         nbt.putLong("restockAt", this.restockAt);
         nbt.putInt("areaLevel", this.areaLevel);
         NbtList list = new NbtList();

         for (Shopkeeper.Ware w : this.wares) {
            if (!w.stack.isEmpty()) {
               NbtCompound t = new NbtCompound();
               t.put("item", w.stack.encode(this.getWorld().getRegistryManager()));
               t.putInt("price", w.price);
               t.putInt("stock", w.stock);
               list.add(t);
            }
         }

         nbt.put("wares", list);
         NbtList b = new NbtList();

         for (int i = 0; i < this.booth.size(); i++) {
            NbtCompound t = new NbtCompound();
            t.putLong("pos", this.booth.get(i)[0]);
            t.putString("block", i < this.boothBlocks.size() ? this.boothBlocks.get(i) : "");
            b.add(t);
         }

         nbt.put("booth", b);
      }

      public void readCustomDataFromNbt(NbtCompound nbt) {
         super.readCustomDataFromNbt(nbt);
         this.restockAt = nbt.getLong("restockAt");
         this.areaLevel = nbt.contains("areaLevel") ? nbt.getInt("areaLevel") : 10;
         this.wares.clear();
         NbtList list = nbt.getList("wares", 10);

         for (int i = 0; i < list.size(); i++) {
            NbtCompound t = list.getCompound(i);
            ItemStack s = ItemStack.fromNbt(this.getWorld().getRegistryManager(), t.getCompound("item")).orElse(ItemStack.EMPTY);
            if (!s.isEmpty()) {
               this.wares.add(new Shopkeeper.Ware(s, t.getInt("price"), t.getInt("stock")));
            }
         }

         this.booth.clear();
         this.boothBlocks.clear();
         NbtList b = nbt.getList("booth", 10);

         for (int ix = 0; ix < b.size(); ix++) {
            this.booth.add(new long[]{b.getCompound(ix).getLong("pos")});
            this.boothBlocks.add(b.getCompound(ix).getString("block"));
         }
      }
   }

   static final class ShopData extends PersistentState {
      UUID current;
      BlockPos pos;
      long lastNear;

      static Shopkeeper.ShopData get(ServerWorld w) {
         return (Shopkeeper.ShopData)w.getPersistentStateManager()
            .getOrCreate(new net.minecraft.world.PersistentState.Type(Shopkeeper.ShopData::new, (nbt, lookup) -> read(nbt), null), "turnbattle_shopkeeper");
      }

      static Shopkeeper.ShopData read(NbtCompound nbt) {
         Shopkeeper.ShopData d = new Shopkeeper.ShopData();
         if (nbt.containsUuid("current")) {
            d.current = nbt.getUuid("current");
         }

         if (nbt.contains("pos")) {
            d.pos = BlockPos.fromLong(nbt.getLong("pos"));
         }

         d.lastNear = nbt.getLong("lastNear");
         return d;
      }

      public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
         if (this.current != null) {
            nbt.putUuid("current", this.current);
         }

         if (this.pos != null) {
            nbt.putLong("pos", this.pos.asLong());
         }

         nbt.putLong("lastNear", this.lastNear);
         return nbt;
      }
   }

   public static final class Ware {
      public final ItemStack stack;
      public final int price;
      public int stock;

      public Ware(ItemStack stack, int price, int stock) {
         this.stack = stack;
         this.price = price;
         this.stock = stock;
      }
   }
}
