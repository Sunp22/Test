package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;

public final class DepthsRun {
   public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("turnbattle", "depths"));
   public static final Identifier START = Identifier.of("turnbattle", "depths_start");
   public static final Identifier STATE = Identifier.of("turnbattle", "depths_state");
   public static final Identifier SCENE = Identifier.of("turnbattle", "depths_scene");
   public static final Identifier DONE = Identifier.of("turnbattle", "depths_done");
   public static final Identifier INTERACT = Identifier.of("turnbattle", "depths_interact");
   static final byte SC_INTRO = 1;
   static final byte SC_TEXT = 2;
   static final byte SC_FALL = 3;
   static final byte SC_READY = 4;
   static final byte SC_SINK = 5;
   static final byte SC_BLACK = 6;
   static final byte SC_VANISH = 7;
   static final byte D_INTRO = 1;
   static final byte D_EDGE = 2;
   static final byte D_NONE = 0;
   static final byte D_VANISH = 3;
   static final String[] SIGNS = new String[]{
      "(You read the sign on the wall.)%(\"Welcome to Depths Descent!\")%(\"This is a roguelite where you descend floors and get skills and stats.\")%(\"You can earn items to take with you back to the Light World.\")",
      "(\"You will encounter enemies, and a boss every 5 floors!\")%(\"You will encounter enemies.\")",
      "(\"You will encounter enemies. Just as they did.\")%(\"These enemies won't hold back.\")",
      "(\"Do you remember her?\")%(\"The owl girl. The one you couldn't pull yourself from.\")%(\"Of course you don't. You don't remember anything.\")%(... You look away from the sign.)"
   };
   private static final Map<UUID, DepthsRun.Session> SESSIONS = new HashMap<>();
   private static final Random RNG = new Random();
   private static final Set<UUID> wiped = new HashSet<>();
   private static List<EntityType<?>> bossMobs;
   private static final Set<String> NOT_BOSS_MOBS = Set.of(
      "minecraft:elder_guardian",
      "minecraft:ender_dragon",
      "minecraft:warden",
      "minecraft:wither",
      "minecraft:giant",
      "minecraft:ghast",
      "minecraft:guardian",
      "minecraft:shulker",
      "minecraft:vex",
      "minecraft:endermite",
      "minecraft:silverfish",
      "minecraft:phantom"
   );
   private static final double SPOT = 6.5;
   private static final double GIVE_UP = 14.0;
   private static final double TOUCH = 1.05;
   private static final double RUSH = 0.3;
   private static final double AMBLE = 0.035;

   private DepthsRun() {
   }

   public static boolean isActive(UUID player) {
      return player != null && SESSIONS.containsKey(player);
   }

   public static boolean blocksBattles(UUID player) {
      DepthsRun.Session s = player == null ? null : SESSIONS.get(player);
      return s != null && s.stage != DepthsRun.Stage.PLAY;
   }

   static int[] origin(UUID player) {
      int slot = Math.floorMod(player.hashCode(), 4096);
      return new int[]{slot % 64 * 512 + 64, slot / 64 * 512 + 64};
   }

   private static NbtCompound stack(ItemStack s, WrapperLookup l) {
      return s != null && !s.isEmpty() ? (NbtCompound)s.encode(l) : new NbtCompound();
   }

   private static ItemStack unstack(NbtCompound t, WrapperLookup l) {
      return t != null && !t.isEmpty() ? ItemStack.fromNbt(l, t).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
   }

   private static NbtList strings(Iterable<String> it) {
      NbtList l = new NbtList();

      for (String s : it) {
         l.add(NbtString.of(s));
      }

      return l;
   }

   private static NbtCompound saveCharacter(ServerPlayerEntity player, BattleData.PlayerProgress p, RecruitData.PlayerRecruits pr) {
      WrapperLookup l = player.getRegistryManager();
      NbtCompound t = new NbtCompound();
      t.putInt("level", p.level);
      t.putInt("exp", p.exp);
      t.put("stats", p.playerStats.toNbt());
      t.putInt("skillPoints", p.skillPoints);
      t.put("skills", strings(p.unlockedSkills));
      t.put("unequipped", strings(p.unequippedSkills));
      t.putIntArray("treeBonus", p.treeStatBonuses != null ? p.treeStatBonuses : new int[6]);
      t.putString("class", p.playerClass != null ? p.playerClass.name() : "");
      t.putString("race", p.playerRace != null ? p.playerRace.name() : "");
      t.putString("soul", p.soulType != null ? p.soulType.name() : "");
      t.putBoolean("detUsed", p.determinationUsed);
      t.put("absorbed", strings(p.absorbedSouls));
      t.putString("monsterWeapon", p.monsterWeapon != null ? p.monsterWeapon.name() : "");
      t.putString("partyMember", p.partyMember != null ? p.partyMember.name() : "NONE");
      t.putFloat("partyHp", p.partyMemberHp);
      t.putString("vessel", p.vesselName != null ? p.vesselName : "");
      t.put("weapon", stack(p.equippedWeapon, l));
      t.put("partyWeapon", stack(p.partyEquippedWeapon, l));
      t.put("accessory", stack(p.equippedAccessory, l));
      t.put("armor0", stack(p.equippedArmor != null ? p.equippedArmor[0] : null, l));
      t.put("armor1", stack(p.equippedArmor != null ? p.equippedArmor[1] : null, l));
      t.putString("style", p.battleStyle != null ? p.battleStyle.name() : "");
      t.putString("view", p.battleView != null ? p.battleView.name() : "");
      t.putBoolean("rogueRun", p.rogueRun);
      t.putInt("handsOwed", p.rogueHandsOwed);
      t.putInt("recA", pr.activeRecruit);
      t.putInt("recB", pr.secondActiveRecruit);
      t.put("inv", player.getInventory().writeNbt(new NbtList()));
      t.putString("dim", player.getWorld().getRegistryKey().getValue().toString());
      t.putDouble("x", player.getX());
      t.putDouble("y", player.getY());
      t.putDouble("z", player.getZ());
      t.putFloat("yaw", player.getYaw());
      t.putFloat("pitch", player.getPitch());
      t.putInt("gm", player.interactionManager.getGameMode().getId());
      t.putFloat("hp", player.getHealth());
      t.putInt("food", player.getHungerManager().getFoodLevel());
      t.putFloat("sat", player.getHungerManager().getSaturationLevel());
      return t;
   }

   private static <E extends Enum<E>> E en(Class<E> c, String name, E def) {
      if (name != null && !name.isEmpty()) {
         try {
            return Enum.valueOf(c, name);
         } catch (IllegalArgumentException var4) {
            return def;
         }
      } else {
         return def;
      }
   }

   private static void loadCharacter(ServerPlayerEntity player, NbtCompound t, BattleData.PlayerProgress p, RecruitData.PlayerRecruits pr) {
      WrapperLookup l = player.getRegistryManager();
      p.level = Math.max(1, t.getInt("level"));
      p.exp = t.getInt("exp");
      p.playerStats = CharStats.fromNbt(t.getCompound("stats"));
      p.skillPoints = t.getInt("skillPoints");
      p.unlockedSkills.clear();
      NbtList sk = t.getList("skills", 8);

      for (int i = 0; i < sk.size(); i++) {
         p.unlockedSkills.add(sk.getString(i));
      }

      p.unequippedSkills.clear();
      NbtList un = t.getList("unequipped", 8);

      for (int i = 0; i < un.size(); i++) {
         p.unequippedSkills.add(un.getString(i));
      }

      int[] tb = t.getIntArray("treeBonus");
      p.treeStatBonuses = tb.length == 6 ? tb : new int[6];
      p.playerClass = en(PlayerClass.class, t.getString("class"), null);
      p.playerRace = en(CharacterRace.class, t.getString("race"), null);
      p.soulType = en(SoulType.class, t.getString("soul"), null);
      p.determinationUsed = t.getBoolean("detUsed");
      p.absorbedSouls.clear();
      NbtList ab = t.getList("absorbed", 8);

      for (int i = 0; i < ab.size(); i++) {
         p.absorbedSouls.add(ab.getString(i));
      }

      if (p.playerStats != null) {
         p.playerStats.absorbed = p.absorbedSouls.size();
      }

      p.monsterWeapon = en(MonsterWeapon.class, t.getString("monsterWeapon"), null);
      p.partyMember = en(PartyMember.class, t.getString("partyMember"), PartyMember.NONE);
      p.partyMemberHp = t.getFloat("partyHp");
      String vn = t.getString("vessel");
      p.vesselName = vn.isEmpty() ? null : vn;
      ItemStack w = unstack(t.getCompound("weapon"), l);
      p.equippedWeapon = w.isEmpty() ? null : w;
      ItemStack pw = unstack(t.getCompound("partyWeapon"), l);
      p.partyEquippedWeapon = pw.isEmpty() ? null : pw;
      ItemStack ac = unstack(t.getCompound("accessory"), l);
      p.equippedAccessory = ac.isEmpty() ? null : ac;
      p.equippedArmor = new ItemStack[]{unstack(t.getCompound("armor0"), l), unstack(t.getCompound("armor1"), l)};
      p.battleStyle = en(BattleStyle.class, t.getString("style"), BattleStyle.BULLET_HELL);
      p.battleView = en(BattleView.class, t.getString("view"), BattleView.TRADITIONAL);
      p.rogueRun = t.getBoolean("rogueRun");
      p.rogueHandsOwed = t.getInt("handsOwed");
      pr.activeRecruit = t.contains("recA") ? t.getInt("recA") : -1;
      pr.secondActiveRecruit = t.contains("recB") ? t.getInt("recB") : -1;
   }

   private static void becomeOri(ServerPlayerEntity player, BattleData.PlayerProgress p, RecruitData.PlayerRecruits pr) {
      BattleStyle style = p.battleStyle == BattleStyle.DIRECT_DAMAGE ? BattleStyle.DIRECT_DAMAGE : BattleStyle.BULLET_HELL;
      p.level = 1;
      p.exp = 0;
      p.playerStats = new CharStats();
      p.playerStats.vitality = 2;
      p.playerStats.magic = 2;
      p.playerStats.offense = 1;
      p.playerStats.defense = 1;
      p.skillPoints = 0;
      p.unlockedSkills.clear();
      p.unlockedSkills.add(OriSkill.MOONBEAM.name());
      p.unequippedSkills.clear();
      p.treeStatBonuses = new int[6];
      p.playerClass = PlayerClass.ORI;
      p.playerRace = CharacterRace.HUMAN;
      p.soulType = SoulType.ORI;
      p.determinationUsed = false;
      p.absorbedSouls.clear();
      if (p.playerStats != null) {
         p.playerStats.absorbed = 0;
      }

      p.monsterWeapon = null;
      p.partyMember = PartyMember.NONE;
      p.partyMemberHp = -1.0F;
      p.vesselName = Recruit.ORI.displayName;
      p.equippedWeapon = null;
      p.partyEquippedWeapon = null;
      p.equippedAccessory = null;
      p.equippedArmor = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      p.battleStyle = style;
      p.battleView = BattleView.TRADITIONAL;
      p.rogueRun = true;
      p.rogueHandsOwed = 0;
      pr.activeRecruit = -1;
      pr.secondActiveRecruit = -1;
      player.getInventory().clear();
      player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
      MobLevelManager.removeWeaponStats(player);
      MobLevelManager.removeArmorStats(player, 0);
      MobLevelManager.removeArmorStats(player, 1);
      MobLevelManager.applyPlayerStats(player, p.level, p.playerStats);
      player.setHealth(player.getMaxHealth());
      player.getHungerManager().setFoodLevel(20);
      player.getHungerManager().setSaturationLevel(5.0F);
      player.clearStatusEffects();
      player.extinguish();
      player.changeGameMode(GameMode.ADVENTURE);
      player.playerScreenHandler.sendContentUpdates();
   }

   private static void restore(ServerPlayerEntity player, NbtCompound saved, boolean teleport) {
      MinecraftServer server = player.getServer();
      if (server != null && saved != null) {
         BattleData bd = BattleData.get(server);
         BattleData.PlayerProgress p = bd.getOrCreate(player.getUuid());
         RecruitData rd = RecruitData.get(server);
         RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
         List keep = new ArrayList<>();

         for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (DepthsLoot.keepsAfterRun(s)) {
               keep.add(s.copy());
            }
         }

         Map brought = new LinkedHashMap<>();

         for (ItemStack s : keep) {
            brought.merge(s.getName().getString(), s.getCount(), Integer::sum);
         }

         loadCharacter(player, saved, p, pr);
         player.getInventory().clear();
         player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
         player.getInventory().readNbt(saved.getList("inv", 10));

         for (ItemStack s : keep) {
            if (!player.getInventory().insertStack(s)) {
               player.dropItem(s, false);
            }
         }

         MobLevelManager.applyPlayerStats(player, p.level, p.playerStats);
         MobLevelManager.removeWeaponStats(player);
         WeaponStats ws = WeaponStats.fromStack(p.equippedWeapon);
         if (ws != null) {
            MobLevelManager.applyWeaponStats(player, ws);
         }

         for (int ix = 0; ix < 2; ix++) {
            MobLevelManager.removeArmorStats(player, ix);
            ArmorStats as = ArmorStats.fromStack(p.equippedArmor[ix]);
            if (as != null) {
               MobLevelManager.applyArmorStats(player, as, ix);
            }
         }

         player.clearStatusEffects();
         player.changeGameMode(GameMode.byId(saved.getInt("gm")));
         float hp = saved.getFloat("hp");
         player.setHealth(hp > 0.5F ? Math.min(hp, player.getMaxHealth()) : player.getMaxHealth());
         player.getHungerManager().setFoodLevel(Math.max(6, saved.getInt("food")));
         player.getHungerManager().setSaturationLevel(saved.getFloat("sat"));
         player.fallDistance = 0.0F;
         player.setInvulnerable(false);
         player.playerScreenHandler.sendContentUpdates();
         bd.markDirty();
         rd.markDirty();
         if (teleport) {
            RegistryKey key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(saved.getString("dim")));
            ServerWorld home = server.getWorld(key);
            if (home == null) {
               home = server.getOverworld();
            }

            player.teleport(home, saved.getDouble("x"), saved.getDouble("y"), saved.getDouble("z"), saved.getFloat("yaw"), saved.getFloat("pitch"));
            player.fallDistance = 0.0F;
         }

         sync(player, p);
         if (!brought.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for (Entry b : brought.entrySet()) {
               if (sb.length() > 0) {
                  sb.append(", ");
               }

               sb.append(b.getValue()).append(" ").append(b.getKey());
            }

            player.sendMessage(Text.literal("You brought back " + sb + " from the Depths.").formatted(Formatting.GOLD), false);
         }
      }
   }

   private static void sync(ServerPlayerEntity player, BattleData.PlayerProgress p) {
      PacketByteBuf lv = PacketByteBufs.create();
      lv.writeInt(p.level);
      lv.writeInt(p.exp);
      lv.writeInt(BattleData.expNeeded(p.level));
      lv.writeBoolean(false);
      Net121.sendToClient(player, TurnBattleMod.LEVEL_UPDATE, lv);
      TurnBattleMod.sendPartyInfo(player);
      TurnBattleMod.sendSkillInfo(player, p);
      TurnBattleMod.sendEquipInfo(player, p);
      TurnBattleMod.sendAccessoryInfo(player, p);
      TurnBattleMod.sendArmorInfo(player, p);
      TurnBattleMod.sendRecruitInfo(player);
   }

   static void register() {
      for (Identifier id : new Identifier[]{START, STATE, SCENE, DONE, INTERACT}) {
         Net121.registerS2CChannel(id);
         Net121.registerC2SChannel(id);
      }

      Net121.registerServerReceiver(START, (server, player, handler, buf, sender) -> server.execute(() -> start(player)));
      Net121.registerServerReceiver(DONE, (server, player, handler, buf, sender) -> {
         byte id = buf.readByte();
         server.execute(() -> onDone(player, id));
      });
      Net121.registerServerReceiver(INTERACT, (server, player, handler, buf, sender) -> server.execute(() -> onInteract(player)));
      UseBlockCallback.EVENT.register((UseBlockCallback)(player, world, hand, hit) -> {
         if (world.isClient || !world.getRegistryKey().equals(WORLD)) {
            return ActionResult.PASS;
         } else if (player instanceof ServerPlayerEntity sp) {
            DepthsRun.Session s = SESSIONS.get(sp.getUuid());
            if (s != null && s.stage == DepthsRun.Stage.PLAY && hand == Hand.MAIN_HAND) {
               BlockPos pos = hit.getBlockPos();
               if (s.layout.chests.contains(pos)) {
                  openChest(sp, s, pos);
               } else if (s.layout.signs.contains(pos)) {
                  readSign(sp, s, pos);
               }
            }

            return ActionResult.FAIL;
         } else {
            return ActionResult.FAIL;
         }
      });
      ServerTickEvents.END_SERVER_TICK.register(DepthsRun::tick);
      ServerPlayConnectionEvents.JOIN
         .register(
            (Join)(handler, sender, server) -> server.execute(
                  () -> {
                     ServerPlayerEntity p = handler.getPlayer();
                     DepthsRun.Data data = DepthsRun.Data.get(server);
                     DepthsRun.Data.Rec rec = data.get(p.getUuid());
                     if (rec.saved != null && !SESSIONS.containsKey(p.getUuid())) {
                        NbtCompound saved = rec.saved;
                        rec.saved = null;
                        data.markDirty();
                        restore(p, saved, true);
                        p.sendMessage(
                           Text.literal("* You wake up somewhere familiar. The Depths let you go.")
                              .formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}),
                           false
                        );
                     } else if (p.getWorld().getRegistryKey().equals(WORLD) && !SESSIONS.containsKey(p.getUuid())) {
                        ServerWorld ow = server.getOverworld();
                        BlockPos sp = ow.getSpawnPos();
                        p.teleport(
                           ow, (double)sp.getX() + 0.5, (double)ow.getTopY(Type.MOTION_BLOCKING, sp.getX(), sp.getZ()), (double)sp.getZ() + 0.5, 0.0F, 0.0F
                        );
                     }

                     sendState(p, null);
                  }
               )
         );
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         DepthsRun.Session s = SESSIONS.remove(handler.getPlayer().getUuid());
         if (s != null) {
            tidy(server, s);
         }
      });
   }

   private static void fail(ServerPlayerEntity p, String why) {
      p.sendMessage(Text.literal(why).formatted(Formatting.RED), true);
   }

   static void start(ServerPlayerEntity player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         UUID u = player.getUuid();
         if (!SESSIONS.containsKey(u)) {
            if (RogueliteData.get(server).rogueliteEnabled) {
               fail(player, "This is a Roguelite world - its own mode takes the Depths' place.");
            } else if (BattleManager.isInBattle(u)) {
               fail(player, "Not in the middle of a battle.");
            } else if (player.isAlive() && !player.isSpectator()) {
               if (MagicantManager.isBusy(u) || TurnBattleMod.isUntouchable(u) || DarkWorldExit.isExiting(u)) {
                  fail(player, "Not right now.");
               } else if (player.hasVehicle()) {
                  fail(player, "Get down first.");
               } else {
                  BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(u);
                  if (prog.soulRemoved) {
                     fail(player, "You have no SOUL to take down there.");
                  } else {
                     ServerWorld depths = server.getWorld(WORLD);
                     if (depths == null) {
                        fail(player, "The Depths couldn't be found (is the mod's data pack loaded?).");
                     } else {
                        RecruitData.PlayerRecruits prCheck = RecruitData.get(server).getOrCreate(u);
                        int ori = Recruit.ORI.ordinal();
                        if (prCheck.activeRecruit != ori && prCheck.secondActiveRecruit != ori) {
                           fail(player, "Unavailable - Ori isn't in your party.");
                        } else {
                           DepthsRun.Data data = DepthsRun.Data.get(server);
                           DepthsRun.Data.Rec rec = data.get(u);
                           if (rec.saved != null) {
                              restore(player, rec.saved, false);
                              rec.saved = null;
                           }

                           RecruitData rd = RecruitData.get(server);
                           RecruitData.PlayerRecruits pr = rd.getOrCreate(u);
                           rec.saved = saveCharacter(player, prog, pr);
                           rec.runs++;
                           data.markDirty();
                           becomeOri(player, prog, pr);
                           BattleData.get(server).markDirty();
                           rd.markDirty();
                           sync(player, prog);
                           int[] o = origin(u);
                           DepthsRun.Session s = new DepthsRun.Session(u, o[0], o[1], RNG.nextLong());
                           SESSIONS.put(u, s);
                           if (!wiped.contains(u)) {
                              DepthsFloor.wipe(depths, s.ox, s.oz);
                              wiped.add(u);
                           }

                           player.setInvulnerable(true);
                           if (!rec.prologueDone) {
                              s.prologue = true;
                              s.floor = 0;
                              buildFloor(depths, s, 0);
                              player.teleport(depths, s.layout.spawnX, 65.0, s.layout.spawnZ, 180.0F, 10.0F);
                              player.setInvulnerable(false);
                              s.to(DepthsRun.Stage.INTRO);
                              sendState(player, s);
                              scene(player, (byte)1, null);
                           } else {
                              s.to(DepthsRun.Stage.CARDS_FIRST);
                              sendState(player, s);
                              prog.rogueHandsOwed = 1;
                              dealNext(player, s);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static void endRun(ServerPlayerEntity player, DepthsRun.Session s, String reason) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         SESSIONS.remove(player.getUuid());
         tidy(server, s);
         player.removeStatusEffect(StatusEffects.LEVITATION);
         player.removeStatusEffect(StatusEffects.SLOW_FALLING);
         DepthsRun.Data data = DepthsRun.Data.get(server);
         DepthsRun.Data.Rec rec = data.get(player.getUuid());
         int reached = Math.max(0, s.floor);
         if (reached > rec.bestFloor) {
            rec.bestFloor = reached;
         }

         NbtCompound saved = rec.saved;
         rec.saved = null;
         data.markDirty();
         restore(player, saved, true);
         sendState(player, null);

         String line = switch (reason) {
            case "lost" -> "* Ori fell on Floor " + reached + ". The Depths spit you back out.";
            case "left" -> reached > 0 ? "* You left the Depths from Floor " + reached + "." : "* You left the Depths.";
            default -> "* The Depths let you go.";
         };
         player.sendMessage(Text.literal(line + (rec.bestFloor > 0 ? "  (Deepest: Floor " + rec.bestFloor + ")" : "")).formatted(Formatting.GRAY), false);
      }
   }

   private static void tidy(MinecraftServer server, DepthsRun.Session s) {
      ServerWorld w = server.getWorld(WORLD);
      if (w != null) {
         discardMobs(w, s);
         DepthsFloor.clear(w, s.placed);
      }
   }

   private static void discardMobs(ServerWorld w, DepthsRun.Session s) {
      for (DepthsRun.Mob m : s.mobs) {
         Entity e = w.getEntityById(m.id);
         if (e != null && !BattleManager.isMobInBattle(m.id)) {
            EliteMobManager.remove(e.getUuid());
            e.discard();
         }
      }

      s.mobs.clear();
   }

   private static void buildFloor(ServerWorld w, DepthsRun.Session s, int floor) {
      discardMobs(w, s);
      DepthsFloor.clear(w, s.placed);
      s.opened.clear();
      s.floor = floor;
      s.layout = floor == 0 ? DepthsFloor.prologue(s.ox, s.oz) : DepthsFloor.generate(floor, s.ox, s.oz, s.seed);
      s.layout.build(w, s.placed);
      s.bossAlive = false;
      s.lore = -1;
      s.loreRead = false;
      if (s.layout.loreSign != null) {
         s.lore = pickLore(w.getServer(), s.player);
      }

      for (int i = 0; i < s.layout.enemySpots.size(); i++) {
         double[] at = s.layout.enemySpots.get(i);
         boolean boss = s.layout.boss && i == s.layout.enemySpots.size() - 1;
         EntityType et = boss ? bossType(floor, s.seed) : (EntityType)Registries.ENTITY_TYPE.get(Identifier.of(DepthsDescentManager.pickMobType(floor, RNG)));
         Entity e = et.create(w);
         if (boss && !(e instanceof MobEntity)) {
            if (e != null) {
               e.discard();
            }

            e = EntityType.ZOMBIE.create(w);
         }

         if (e instanceof MobEntity mob) {
            float yaw = boss ? 0.0F : RNG.nextFloat() * 360.0F;
            mob.refreshPositionAndAngles(at[0], 65.0, at[1], yaw, 0.0F);
            mob.setHeadYaw(yaw);
            mob.setBodyYaw(yaw);
            mob.setAiDisabled(true);
            mob.setPersistent();
            mob.addCommandTag("turnbattle_depths");
            int lvl = enemyLevel(floor, boss);

            try {
               MobLevelManager.registerMobLevel(mob, lvl);
               MobLevelManager.applyMobLevel(mob, lvl);
            } catch (Throwable var13) {
            }

            if (w.spawnEntity(mob)) {
               if (boss) {
                  try {
                     EliteMobManager.makeElite(mob, bossTier(floor));
                  } catch (Throwable var12) {
                  }

                  mob.setPersistent();
                  mob.setAiDisabled(true);
               }

               s.mobs.add(new DepthsRun.Mob(mob.getId(), boss, at[0], at[1]));
               if (boss) {
                  s.bossAlive = true;
               }
            }
         }
      }
   }

   private static synchronized List<EntityType<?>> bossMobs() {
      if (bossMobs != null) {
         return bossMobs;
      } else {
         List out = new ArrayList<>();

         for (EntityType t : Registries.ENTITY_TYPE) {
            if (t.getSpawnGroup() == SpawnGroup.MONSTER && !NOT_BOSS_MOBS.contains(Registries.ENTITY_TYPE.getId(t).toString())) {
               out.add(t);
            }
         }

         if (out.isEmpty()) {
            out.add(EntityType.ZOMBIE);
         }

         bossMobs = out;
         return out;
      }
   }

   static EntityType<?> bossType(int floor, long seed) {
      if (floor % 25 == 0) {
         return EntityType.WITHER;
      } else if (floor % 15 == 0) {
         return EntityType.WARDEN;
      } else {
         List pool = bossMobs();
         Random r = new Random(seed * 31L + (long)floor);
         return pool.get(r.nextInt(pool.size()));
      }
   }

   static int bossTier(int floor) {
      int k = Math.max(0, floor / 5 - 1);
      return Math.min(EliteMobManager.TIER_NAMES.length - 1, k / 2);
   }

   static int enemyLevel(int floor, boolean boss) {
      int base = Math.max(1, Math.round(1.0F + (float)(floor - 1) * 1.6F)) + RNG.nextInt(2);
      return Math.min(99, boss ? base + 5 : base);
   }

   private static void catchUpLevel(ServerPlayerEntity player, int floor) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         int floorLevel = Math.min(99, Math.max(1, Math.round(1.0F + (float)(floor - 1) * 1.6F)));
         BattleData bd = BattleData.get(server);
         BattleData.PlayerProgress prog = bd.getOrCreate(player.getUuid());
         if (prog.level < floorLevel) {
            int gained = floorLevel - prog.level;
            prog.level = floorLevel;
            prog.exp = 0;
            prog.rogueHandsOwed += gained;
            bd.markDirty();
            MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
            player.setHealth(player.getMaxHealth());
            sync(player, prog);
            say(player, "LV " + floorLevel + "! (" + gained + (gained == 1 ? " hand" : " hands") + " of cards to pick)", Formatting.YELLOW);
         }
      }
   }

   public static int enemyLevelFor(UUID player, LivingEntity enemy) {
      DepthsRun.Session s = SESSIONS.get(player);
      if (s != null && enemy != null) {
         for (DepthsRun.Mob m : s.mobs) {
            if (m.id == enemy.getId()) {
               int lv = MobLevelManager.getMobLevel(enemy);
               return lv > 0 ? lv : enemyLevel(s.floor, m.boss);
            }
         }

         return enemyLevel(s.floor, false);
      } else {
         return -1;
      }
   }

   private static void beginFall(ServerPlayerEntity player, DepthsRun.Session s, int floor) {
      s.pendingFloor = floor;
      s.to(DepthsRun.Stage.FALL_WAIT);
      player.setInvulnerable(true);
      PacketByteBuf b = PacketByteBufs.create();
      b.writeVarInt(floor);
      scene(player, (byte)3, b);
   }

   private static void land(MinecraftServer server, ServerPlayerEntity player, DepthsRun.Session s) {
      ServerWorld w = server.getWorld(WORLD);
      if (w == null) {
         endRun(player, s, "gone");
      } else {
         s.prologue = false;
         buildFloor(w, s, s.pendingFloor);
         player.removeStatusEffect(StatusEffects.SLOW_FALLING);
         player.removeStatusEffect(StatusEffects.LEVITATION);
         player.setVelocity(0.0, 0.0, 0.0);
         player.velocityModified = true;
         player.teleport(w, s.layout.spawnX, 65.0, s.layout.spawnZ, 180.0F, 10.0F);
         player.fallDistance = 0.0F;
         player.setInvulnerable(false);
         DepthsRun.Data data = DepthsRun.Data.get(server);
         DepthsRun.Data.Rec rec = data.get(player.getUuid());
         if (s.floor > rec.bestFloor) {
            rec.bestFloor = s.floor;
            data.markDirty();
         }

         catchUpLevel(player, s.floor);
         s.to(DepthsRun.Stage.PLAY);
         s.lastBattleEnd = server.getOverworld().getTime();
         s.noticeAt = s.layout.boss ? 80 : -1;
         sendState(player, s);
         scene(player, (byte)4, null);
      }
   }

   private static void tick(MinecraftServer server) {
      if (!SESSIONS.isEmpty()) {
         long now = server.getOverworld().getTime();
         Iterator it = SESSIONS.entrySet().iterator();

         while (it.hasNext()) {
            DepthsRun.Session s = it.next().getValue();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(s.player);
            if (p == null) {
               it.remove();
               tidy(server, s);
            } else {
               s.ticks++;

               try {
                  tickSession(server, p, s, now);
               } catch (Throwable var7) {
                  TurnBattleMod.LOGGER.error("[TurnBattle] Depths Descent tick failed", var7);
               }
            }
         }
      }
   }

   private static void tickSession(MinecraftServer server, ServerPlayerEntity p, DepthsRun.Session s, long now) {
      if (SESSIONS.containsKey(s.player)) {
         ServerWorld w = server.getWorld(WORLD);
         boolean here = w != null && p.getWorld() == w;
         switch (s.stage) {
            case INTRO:
            case EDGE_TEXT:
               if (here) {
                  hold(p, s);
               }
               break;
            case PLAY:
               if (!here) {
                  return;
               }

               if (BattleManager.isInBattle(p.getUuid())) {
                  s.lastBattleEnd = now;
                  return;
               }

               if (p.getY() < 58.0) {
                  p.teleport(w, s.layout.spawnX, 65.0, s.layout.spawnZ, 180.0F, 10.0F);
                  p.fallDistance = 0.0F;
               }

               if (s.floor == 0 && !Double.isNaN(s.layout.cliffEdgeZ) && p.getZ() <= s.layout.cliffEdgeZ) {
                  s.to(DepthsRun.Stage.EDGE_TEXT);
                  text(p, (byte)2, "(... You can't see the bottom.)");
                  return;
               }

               if (s.noticeAt > 0 && s.ticks == s.noticeAt) {
                  text(p, (byte)0, "(Something is waiting for you, further down this floor.)");
               }

               BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(p.getUuid());
               if (s.hand == null && prog.rogueHandsOwed > 0 && now - s.lastBattleEnd > 30L) {
                  dealNext(p, s);
               }

               tickMobs(w, p, s, now);
               break;
            case EDGE_FALL:
               if (s.ticks > 45 || p.getY() < 54.0) {
                  DepthsRun.Data data = DepthsRun.Data.get(server);
                  data.get(p.getUuid()).prologueDone = true;
                  data.markDirty();
                  int cx = s.ox + 38;
                  int cz = s.oz + 38;
                  DepthsFloor.holdingCell(w, cx, cz, s.placed);
                  p.setVelocity(0.0, 0.0, 0.0);
                  p.velocityModified = true;
                  p.teleport(w, (double)cx + 0.5, 24.0, (double)cz + 0.5, 180.0F, 10.0F);
                  p.fallDistance = 0.0F;
                  p.setInvulnerable(true);
                  PacketByteBuf b = PacketByteBufs.create();
                  b.writeBoolean(true);
                  scene(p, (byte)6, b);
                  s.to(DepthsRun.Stage.CARDS_FIRST);
                  BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(p.getUuid());
                  prog.rogueHandsOwed = Math.max(1, prog.rogueHandsOwed);
                  dealNext(p, s);
               }
               break;
            case CARDS_FIRST:
               if (here) {
                  p.fallDistance = 0.0F;
               }
               break;
            case FALL_WAIT:
               if (s.ticks >= 12) {
                  land(server, p, s);
               }
               break;
            case SINKING:
               p.setVelocity(0.0, -0.09, 0.0);
               p.velocityModified = true;
               p.fallDistance = 0.0F;
               if (here && s.ticks % 2 == 0) {
                  w.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.2, p.getZ(), 3, 0.5, 0.6, 0.5, 0.01);
               }

               if (s.ticks >= 34) {
                  beginFall(p, s, s.floor + 1);
               }
               break;
            case EXITING:
               long u = (long)(s.ticks - 12);
               if (u >= 0L && u % 5L == 0L && u / 5L <= 5L) {
                  p.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 88 - s.ticks + 10, (int)(u / 5L), false, false, false));
               }

               p.fallDistance = 0.0F;
               if (s.ticks >= 88) {
                  endRun(p, s, "left");
               }
               break;
            case ENDING:
               p.setInvulnerable(true);
               if (here) {
                  hold(p, s);
               }

               if (s.ticks >= 40 && !BattleManager.isInBattle(p.getUuid())) {
                  endRun(p, s, s.endReason != null ? s.endReason : "lost");
               }
               break;
            case VANISH:
               if (here) {
                  hold(p, s);
               }

               p.setInvulnerable(true);
               if (s.ticks == 40 && w != null) {
                  DepthsFloor f = s.layout;
                  if (f != null && f.loreSign != null) {
                     w.setBlockState(f.loreSign, Blocks.AIR.getDefaultState(), 50);
                     f.signs.remove(f.loreSign);
                     f.loreSign = null;
                  }

                  discardMobs(w, s);
                  s.bossAlive = false;
                  s.noticeAt = -1;
               }

               if (s.ticks >= 82) {
                  p.setInvulnerable(false);
                  s.to(DepthsRun.Stage.PLAY);
                  s.lastBattleEnd = now;
               }
         }
      }
   }

   private static void hold(ServerPlayerEntity p, DepthsRun.Session s) {
      p.setVelocity(0.0, Math.min(0.0, p.getVelocity().y), 0.0);
      p.velocityModified = true;
   }

   private static void tickMobs(ServerWorld w, ServerPlayerEntity p, DepthsRun.Session s, long now) {
      if (s.hand == null) {
         boolean grace = now - s.lastBattleEnd < 60L;
         Iterator it = s.mobs.iterator();

         while (it.hasNext()) {
            DepthsRun.Mob m = it.next();
            if (!(w.getEntityById(m.id) instanceof MobEntity mob) || !mob.isAlive() || mob.isRemoved()) {
               it.remove();
               if (m.boss) {
                  s.bossAlive = false;
                  text(p, (byte)0, "(The darkness at the end of the floor settles. The way down is open.)");
               }
               continue;
            }

            if (!BattleManager.isMobInBattle(m.id)) {
               if (!mob.isAiDisabled()) {
                  mob.setAiDisabled(true);
               }

               if (m.boss && !m.chasing) {
                  mob.setVelocity(0.0, 0.0, 0.0);
                  if (Math.abs(mob.getX() - m.homeX) + Math.abs(mob.getZ() - m.homeZ) + Math.abs(mob.getY() - 65.0) > 0.05) {
                     mob.refreshPositionAndAngles(m.homeX, 65.0, m.homeZ, mob.getYaw(), 0.0F);
                  }
               }

               double dx = p.getX() - mob.getX();
               double dz = p.getZ() - mob.getZ();
               double dist = Math.sqrt(dx * dx + dz * dz);
               boolean level = Math.abs(p.getY() - mob.getY()) < 2.0;
               if (!m.chasing) {
                  if (grace
                     || !level
                     || !(dist < (m.boss ? Math.max(4.5, 1.05 + (double)mob.getWidth() * 0.5 + 0.7) : 6.5))
                     || !sees(s.layout, mob.getX(), mob.getZ(), p.getX(), p.getZ())) {
                     if (!m.boss) {
                        amble(s, mob, m);
                     }
                     continue;
                  }

                  m.chasing = true;
                  m.alert = m.boss ? 14 : 8;
                  w.playSound(null, mob.getBlockPos(), (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.HOSTILE, 0.8F, 1.6F);
                  w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + (double)mob.getHeight() + 0.4, mob.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
               }

               face(mob, dx, dz);
               if (m.alert > 0) {
                  m.alert--;
               } else if (!(dist > 14.0) && !grace) {
                  double touch = 1.05 + Math.max(0.0, (double)mob.getWidth() * 0.5 - 0.3);
                  if (!(dist < touch) || !level) {
                     double step = Math.min(0.3 * (m.boss ? 0.85 : 1.0), dist - touch * 0.6);
                     move(s, mob, dx / dist * step, dz / dist * step);
                  } else if (p.currentScreenHandler instanceof PlayerScreenHandler && s.hand == null) {
                     BattleManager.tryStart(p, mob);
                     if (BattleManager.isInBattle(p.getUuid())) {
                        s.lastBattleEnd = now;
                        return;
                     }
                  }
               } else {
                  m.chasing = false;
               }
            }
         }
      }
   }

   private static void amble(DepthsRun.Session s, MobEntity mob, DepthsRun.Mob m) {
      if (--m.wanderIn <= 0) {
         m.wanderIn = 50 + RNG.nextInt(90);
         double ang = RNG.nextDouble() * Math.PI * 2.0;
         double r = RNG.nextDouble() * 1.8;
         m.wanderX = m.homeX + Math.cos(ang) * r;
         m.wanderZ = m.homeZ + Math.sin(ang) * r;
      }

      double dx = m.wanderX - mob.getX();
      double dz = m.wanderZ - mob.getZ();
      double d = Math.sqrt(dx * dx + dz * dz);
      if (!(d < 0.05)) {
         face(mob, dx, dz);
         double step = Math.min(0.035, d);
         move(s, mob, dx / d * step, dz / d * step);
      }
   }

   private static void face(MobEntity mob, double dx, double dz) {
      float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      mob.setYaw(yaw);
      mob.setHeadYaw(yaw);
      mob.setBodyYaw(yaw);
   }

   private static void move(DepthsRun.Session s, MobEntity mob, double mx, double mz) {
      double x = mob.getX();
      double z = mob.getZ();
      double r = Math.min(0.45, (double)(mob.getWidth() / 2.0F));
      if (s.layout.walkableAt(x + mx + Math.signum(mx) * r, z)) {
         x += mx;
      }

      if (s.layout.walkableAt(x, z + mz + Math.signum(mz) * r)) {
         z += mz;
      }

      mob.refreshPositionAndAngles(x, 65.0, z, mob.getYaw(), 0.0F);
   }

   private static boolean sees(DepthsFloor f, double x0, double z0, double x1, double z1) {
      double dx = x1 - x0;
      double dz = z1 - z0;
      int n = (int)Math.ceil(Math.sqrt(dx * dx + dz * dz) * 2.0);

      for (int i = 1; i < n; i++) {
         double k = (double)i / (double)n;
         if (!f.walkableAt(x0 + dx * k, z0 + dz * k)) {
            return false;
         }
      }

      return true;
   }

   private static double flat(ServerPlayerEntity p, double x, double z) {
      double dx = p.getX() - x;
      double dz = p.getZ() - z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   static void onInteract(ServerPlayerEntity p) {
      DepthsRun.Session s = SESSIONS.get(p.getUuid());
      if (s != null && s.stage == DepthsRun.Stage.PLAY && s.layout != null && !BattleManager.isInBattle(p.getUuid())) {
         if (p.getWorld().getRegistryKey().equals(WORLD)) {
            DepthsFloor f = s.layout;
            if (!Double.isNaN(f.darkX) && flat(p, f.darkX, f.darkZ) < 1.7) {
               descend(p, s);
            } else if (flat(p, f.lightX, f.lightZ) < 1.7) {
               leave(p, s);
            } else {
               BlockPos best = null;
               double bestD = 2.3;
               boolean chest = false;

               for (BlockPos c : f.chests) {
                  double d = flat(p, (double)c.getX() + 0.5, (double)c.getZ() + 0.5);
                  if (d < bestD) {
                     bestD = d;
                     best = c;
                     chest = true;
                  }
               }

               for (BlockPos sg : f.signs) {
                  double d = flat(p, (double)sg.getX() + 0.5, (double)sg.getZ() + 0.5);
                  if (d < bestD) {
                     bestD = d;
                     best = sg;
                     chest = false;
                  }
               }

               if (best != null) {
                  if (chest) {
                     openChest(p, s, best);
                  } else {
                     readSign(p, s, best);
                  }
               }
            }
         }
      }
   }

   private static int pickLore(MinecraftServer server, UUID u) {
      Set seen = DepthsRun.Data.get(server).get(u).loreSeen;
      List fresh = new ArrayList<>();

      for (int i = 0; i < DepthsLore.ALL.size(); i++) {
         if (!seen.contains(DepthsLore.ALL.get(i).key())) {
            fresh.add(i);
         }
      }

      return fresh.isEmpty() ? RNG.nextInt(DepthsLore.ALL.size()) : fresh.get(RNG.nextInt(fresh.size()));
   }

   private static void readLore(ServerPlayerEntity p, DepthsRun.Session s) {
      if (!s.loreRead && s.lore >= 0 && s.lore < DepthsLore.ALL.size()) {
         s.loreRead = true;
         DepthsLore.Lore l = DepthsLore.ALL.get(s.lore);
         DepthsRun.Data data = DepthsRun.Data.get(p.getServer());
         data.get(p.getUuid()).loreSeen.add(l.key());
         data.markDirty();
         String say = l.text();
         Random r = new Random(s.seed ^ (long)s.floor * 977L ^ (long)s.lore);

         ItemStack give = switch (l.effect()) {
            case EGG -> new ItemStack(Items.EGG);
            case WEAPON -> DepthsLoot.rollKind(r, Math.max(1, s.floor), 0);
            case ARMOR -> DepthsLoot.rollKind(r, Math.max(1, s.floor), 1);
            case FOOD -> DepthsLoot.rollKind(r, Math.max(1, s.floor), 2);
            default -> ItemStack.EMPTY;
         };
         if (!give.isEmpty()) {
            if (l.effect() != DepthsLore.Effect.EGG) {
               say = say + "%(You obtained " + give.getCount() + " " + give.getName().getString() + ".)";
            }

            if (!p.getInventory().insertStack(give.copy())) {
               p.dropItem(give.copy(), false);
            }

            p.playerScreenHandler.sendContentUpdates();
         }

         if (l.effect() == DepthsLore.Effect.CARD) {
            BattleData.PlayerProgress prog = BattleData.get(p.getServer()).getOrCreate(p.getUuid());
            prog.rogueHandsOwed++;
         }

         text(p, (byte)(l.effect() == DepthsLore.Effect.VANISH ? 3 : 0), say);
      } else {
         text(p, (byte)0, "(... ?&There's nothing written there.)");
      }
   }

   private static void readSign(ServerPlayerEntity p, DepthsRun.Session s, BlockPos pos) {
      if (pos.equals(s.layout.loreSign)) {
         readLore(p, s);
      } else {
         int i = s.layout.signs.indexOf(pos);
         if (i >= 0 && i < SIGNS.length) {
            text(p, (byte)0, SIGNS[i]);
         }
      }
   }

   private static void openChest(ServerPlayerEntity p, DepthsRun.Session s, BlockPos pos) {
      if (!s.opened.add(pos.asLong())) {
         text(p, (byte)0, "(The chest is empty.)");
      } else {
         ServerWorld w = (ServerWorld)p.getWorld();
         w.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.7F, 1.0F);
         List loot = DepthsLoot.roll(new Random(s.seed ^ pos.asLong() ^ (long)s.floor), Math.max(1, s.floor));
         StringBuilder pages = new StringBuilder();

         for (ItemStack it : loot) {
            if (pages.length() > 0) {
               pages.append('%');
            }

            pages.append("(You obtained ").append(it.getCount()).append(' ').append(it.getName().getString()).append(".)");
            ItemStack give = it.copy();
            if (!p.getInventory().insertStack(give)) {
               p.dropItem(give, false);
            }
         }

         p.playerScreenHandler.sendContentUpdates();
         text(p, (byte)0, pages.toString());
      }
   }

   private static void descend(ServerPlayerEntity p, DepthsRun.Session s) {
      if (s.bossAlive) {
         text(p, (byte)0, "(The darkness won't let you through.)%(Something on this floor is still watching you.)");
      } else {
         ServerWorld w = (ServerWorld)p.getWorld();
         DepthsFloor f = s.layout;
         p.networkHandler.requestTeleport(f.darkX, p.getY(), f.darkZ, p.getYaw(), p.getPitch());
         p.setVelocity(0.0, 0.0, 0.0);
         p.velocityModified = true;

         for (BlockPos b : DepthsFloor.breakFloor(w, f.darkX, f.darkZ, 1)) {
            w.spawnParticles(ParticleTypes.SQUID_INK, (double)b.getX() + 0.5, (double)b.getY() + 0.8, (double)b.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.02);
         }

         p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false, false));
         s.to(DepthsRun.Stage.SINKING);
         PacketByteBuf b = PacketByteBufs.create();
         b.writeDouble(f.darkX);
         b.writeDouble(f.darkZ);
         scene(p, (byte)5, b);
         w.playSound(null, p.getBlockPos(), TurnBattleSounds.DTRANS_LW, SoundCategory.PLAYERS, 0.8F, 0.6F);
      }
   }

   private static void leave(ServerPlayerEntity p, DepthsRun.Session s) {
      DepthsFloor f = s.layout;
      double y = p.getY();
      p.networkHandler.requestTeleport(f.lightX, y, f.lightZ, p.getYaw(), p.getPitch());
      p.setVelocity(0.0, 0.0, 0.0);
      p.fallDistance = 0.0F;
      s.to(DepthsRun.Stage.EXITING);
      PlayerActionNet.broadcast(p, 8, true, b -> {
         b.writeDouble(f.lightX);
         b.writeDouble(y);
         b.writeDouble(f.lightZ);
      });
   }

   static void onDone(ServerPlayerEntity p, byte id) {
      DepthsRun.Session s = SESSIONS.get(p.getUuid());
      if (s != null) {
         if (id == 3 && s.stage == DepthsRun.Stage.PLAY && !BattleManager.isInBattle(p.getUuid())) {
            s.to(DepthsRun.Stage.VANISH);
            scene(p, (byte)7, null);
         } else {
            if (id == 1 && s.stage == DepthsRun.Stage.INTRO) {
               s.to(DepthsRun.Stage.PLAY);
               s.lastBattleEnd = p.getServer().getOverworld().getTime();
            } else if (id == 2 && s.stage == DepthsRun.Stage.EDGE_TEXT) {
               ServerWorld w = (ServerWorld)p.getWorld();
               int bx = (int)Math.floor(p.getX());
               int bz = (int)Math.floor(p.getZ());

               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -2; dz <= 1; dz++) {
                     for (int y = 63; y <= 64; y++) {
                        BlockPos pos = new BlockPos(bx + dx, y, bz + dz);
                        BlockState st = w.getBlockState(pos);
                        if (!st.isAir() && !st.isOf(Blocks.BARRIER)) {
                           FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, pos, st);
                           fb.dropItem = false;
                           fb.setVelocity((RNG.nextDouble() - 0.5) * 0.1, -0.05 - RNG.nextDouble() * 0.1, (RNG.nextDouble() - 0.5) * 0.1);
                        }
                     }
                  }
               }

               w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_DEEPSLATE_BREAK, SoundCategory.BLOCKS, 1.2F, 0.7F);
               w.playSound(null, p.getBlockPos(), (SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 0.4F, 0.6F);
               w.spawnParticles(
                  new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DEEPSLATE.getDefaultState()), p.getX(), 64.6, p.getZ(), 40, 1.2, 0.2, 1.2, 0.1
               );
               s.to(DepthsRun.Stage.EDGE_FALL);
            }
         }
      }
   }

   private static void dealNext(ServerPlayerEntity p, DepthsRun.Session s) {
      MinecraftServer server = p.getServer();
      if (server != null && s.hand == null) {
         BattleData bd = BattleData.get(server);
         BattleData.PlayerProgress prog = bd.getOrCreate(p.getUuid());
         if (prog.rogueHandsOwed > 0) {
            List hand = RogueCards.dealOri(RNG, 3, prog.level, prog.unlockedSkills, prog.playerStats);
            if (hand.isEmpty()) {
               prog.rogueHandsOwed = 0;
               bd.markDirty();
               afterCards(p, s);
            } else {
               prog.rogueHandsOwed--;
               bd.markDirty();
               s.hand = hand;
               sendHand(p, s, prog.rogueHandsOwed);
            }
         }
      }
   }

   private static void sendHand(ServerPlayerEntity p, DepthsRun.Session s, int owed) {
      PacketByteBuf buf = PacketByteBufs.create();
      List h = s.hand != null ? s.hand : List.of();
      buf.writeVarInt(h.size());

      for (String c : h) {
         buf.writeString(c);
      }

      buf.writeVarInt(0);
      buf.writeVarInt(owed);
      Net121.sendToClient(p, TurnBattleMod.ROGUE_CARD_DRAW, buf);
   }

   public static boolean onCardChoice(ServerPlayerEntity p, int index) {
      DepthsRun.Session s = SESSIONS.get(p.getUuid());
      if (s == null || s.hand == null) {
         return s != null;
      } else if (index >= 0 && index < s.hand.size()) {
         String card = s.hand.get(index);
         s.hand = null;
         BattleData bd = BattleData.get(p.getServer());
         BattleData.PlayerProgress prog = bd.getOrCreate(p.getUuid());
         applyCard(p, prog, card);
         bd.markDirty();
         sync(p, prog);
         if (prog.rogueHandsOwed > 0) {
            dealNext(p, s);
         } else {
            afterCards(p, s);
         }

         return true;
      } else {
         sendHand(p, s, BattleData.get(p.getServer()).getOrCreate(p.getUuid()).rogueHandsOwed);
         return true;
      }
   }

   private static void afterCards(ServerPlayerEntity p, DepthsRun.Session s) {
      if (s.stage == DepthsRun.Stage.CARDS_FIRST) {
         beginFall(p, s, 1);
      }
   }

   private static void applyCard(ServerPlayerEntity p, BattleData.PlayerProgress prog, String card) {
      String[] parts = card.split(":", 3);
      RogueCards.Info info = RogueCards.describe(card);
      String var5 = parts[0];
      switch (var5) {
         case "STAT":
            int i = RogueCards.statIndex(parts[1]);
            if (i < 0) {
               return;
            }

            int before = prog.playerStats.valueFor(i);
            add(prog.playerStats, i, Integer.parseInt(parts[2]));
            say(p, "+" + (prog.playerStats.valueFor(i) - before) + " " + RogueCards.STAT_NAMES[i] + "!", Formatting.AQUA);
            break;
         case "RISK":
            int lose = RogueCards.statIndex(parts[1]);
            int gain = RogueCards.statIndex(parts[2]);
            if (lose < 0 || gain < 0) {
               return;
            }

            add(prog.playerStats, lose, -4);
            add(prog.playerStats, gain, 7);
            say(p, info.title() + "!", Formatting.GOLD);
            break;
         case "SKILL":
            String name = RogueCards.skillName(parts[1] + ":" + parts[2]);
            prog.unlockedSkills.add(name);
            prog.unequippedSkills.remove(name);
            say(p, "New skill: " + info.title() + "!", Formatting.GREEN);
      }

      prog.playerClass = PlayerClass.ORI;
      float before = p.getMaxHealth();
      MobLevelManager.applyPlayerStats(p, prog.level, prog.playerStats);
      float gained = p.getMaxHealth() - before;
      if (gained > 0.0F) {
         p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + gained));
      }
   }

   private static void add(CharStats st, int i, int amount) {
      int v = Math.max(0, st.valueFor(i) + amount);
      if (i == 5) {
         v = Math.min(v, Math.max(st.valueFor(5), st.capFor(5)));
      }

      switch (i) {
         case 0:
            st.offense = v;
            break;
         case 1:
            st.defense = v;
            break;
         case 2:
            st.magic = v;
            break;
         case 3:
            st.iq = v;
            break;
         case 4:
            st.vitality = v;
            break;
         default:
            st.luck = v;
      }
   }

   private static void say(ServerPlayerEntity p, String msg, Formatting f) {
      p.sendMessage(Text.literal(msg).formatted(f), true);
   }

   public static void onBattleEnd(BattleState state, MinecraftServer server, String outcome) {
      DepthsRun.Session s = SESSIONS.get(state.playerUuid);
      if (s != null) {
         s.lastBattleEnd = server.getOverworld().getTime();
         if ("lose".equals(outcome)) {
            s.endReason = "lost";
            s.to(DepthsRun.Stage.ENDING);
         } else if ("win".equals(outcome) || "mercy".equals(outcome)) {
            s.battlesWon++;
         }
      }
   }

   static void sendState(ServerPlayerEntity p, DepthsRun.Session s) {
      PacketByteBuf b = PacketByteBufs.create();
      b.writeBoolean(s != null);
      if (s != null) {
         b.writeVarInt(s.floor);
         b.writeBoolean(s.prologue || s.floor == 0);
         boolean hasLayout = s.layout != null;
         b.writeBoolean(hasLayout);
         if (hasLayout) {
            b.writeDouble(s.layout.lightX);
            b.writeDouble(s.layout.lightZ);
            b.writeBoolean(!Double.isNaN(s.layout.darkX));
            if (!Double.isNaN(s.layout.darkX)) {
               b.writeDouble(s.layout.darkX);
               b.writeDouble(s.layout.darkZ);
            }

            b.writeBoolean(s.layout.boss);
         }
      }

      DepthsRun.Data.Rec rec = p.getServer() != null ? DepthsRun.Data.get(p.getServer()).get(p.getUuid()) : new DepthsRun.Data.Rec();
      b.writeVarInt(rec.bestFloor);
      b.writeVarInt(rec.runs);
      int soul = 0;
      if (s != null && rec.saved != null) {
         try {
            soul = SoulType.valueOf(rec.saved.getString("soul")).ordinal() + 1;
         } catch (IllegalArgumentException var6) {
         }
      }

      b.writeVarInt(soul);
      Net121.sendToClient(p, STATE, b);
   }

   private static void scene(ServerPlayerEntity p, byte kind, PacketByteBuf data) {
      PacketByteBuf b = PacketByteBufs.create();
      b.writeByte(kind);
      if (data != null) {
         b.writeBytes(data);
      }

      Net121.sendToClient(p, SCENE, b);
   }

   private static void text(ServerPlayerEntity p, byte id, String raw) {
      List pages = TurnBattleMod.parseTextboxPages(raw);
      PacketByteBuf b = PacketByteBufs.create();
      b.writeByte(id);
      b.writeVarInt(pages.size());

      for (String pg : pages) {
         b.writeString(pg, 4096);
      }

      scene(p, (byte)2, b);
   }

   public static final class Data extends PersistentState {
      final Map<UUID, DepthsRun.Data.Rec> players = new HashMap<>();

      DepthsRun.Data.Rec get(UUID u) {
         return this.players.computeIfAbsent(u, k -> new DepthsRun.Data.Rec());
      }

      static DepthsRun.Data get(MinecraftServer server) {
         return (DepthsRun.Data)server.getOverworld()
            .getPersistentStateManager()
            .getOrCreate(new net.minecraft.world.PersistentState.Type(DepthsRun.Data::new, (nbt, lookup) -> read(nbt), null), "soulbound_depths");
      }

      public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
         NbtList list = new NbtList();
         this.players.forEach((u, r) -> {
            NbtCompound t = new NbtCompound();
            t.putUuid("uuid", u);
            t.putBoolean("prologue", r.prologueDone);
            t.putInt("best", r.bestFloor);
            t.putInt("runs", r.runs);
            t.putIntArray("lore", r.loreSeen.stream().mapToInt(Integer::intValue).toArray());
            if (r.saved != null) {
               t.put("saved", r.saved);
            }

            list.add(t);
         });
         nbt.put("players", list);
         return nbt;
      }

      static DepthsRun.Data read(NbtCompound nbt) {
         DepthsRun.Data d = new DepthsRun.Data();
         NbtList list = nbt.getList("players", 10);

         for (int i = 0; i < list.size(); i++) {
            NbtCompound t = list.getCompound(i);
            DepthsRun.Data.Rec r = new DepthsRun.Data.Rec();
            r.prologueDone = t.getBoolean("prologue");
            r.bestFloor = t.getInt("best");
            r.runs = t.getInt("runs");

            for (int k : t.getIntArray("lore")) {
               r.loreSeen.add(k);
            }

            if (t.contains("saved")) {
               r.saved = t.getCompound("saved");
            }

            d.players.put(t.getUuid("uuid"), r);
         }

         return d;
      }

      static final class Rec {
         boolean prologueDone;
         int bestFloor;
         int runs;
         final Set<Integer> loreSeen = new HashSet<>();
         NbtCompound saved;
      }
   }

   static final class Mob {
      final int id;
      final boolean boss;
      double homeX;
      double homeZ;
      boolean chasing;
      int alert;
      double wanderX;
      double wanderZ;
      int wanderIn;

      Mob(int id, boolean boss, double x, double z) {
         this.id = id;
         this.boss = boss;
         this.homeX = this.wanderX = x;
         this.homeZ = this.wanderZ = z;
      }
   }

   static final class Session {
      final UUID player;
      final int ox;
      final int oz;
      final long seed;
      DepthsRun.Stage stage;
      int ticks;
      int floor;
      int pendingFloor;
      boolean prologue;
      DepthsFloor layout;
      final List<Long> placed = new ArrayList<>();
      final List<DepthsRun.Mob> mobs = new ArrayList<>();
      final Set<Long> opened = new HashSet<>();
      List<String> hand;
      long lastBattleEnd = -1000L;
      int battlesWon;
      boolean bossAlive;
      int noticeAt = -1;
      String endReason;
      int lore = -1;
      boolean loreRead;

      Session(UUID player, int ox, int oz, long seed) {
         this.player = player;
         this.ox = ox;
         this.oz = oz;
         this.seed = seed;
      }

      void to(DepthsRun.Stage s) {
         this.stage = s;
         this.ticks = 0;
      }
   }

   static enum Stage {
      INTRO,
      PLAY,
      EDGE_TEXT,
      EDGE_FALL,
      CARDS_FIRST,
      FALL_WAIT,
      SINKING,
      EXITING,
      ENDING,
      VANISH;
   }
}
