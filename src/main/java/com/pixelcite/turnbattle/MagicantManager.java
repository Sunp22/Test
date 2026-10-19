package com.pixelcite.turnbattle;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public final class MagicantManager {
   public static final int LEVEL_REQUIRED = 50;
   public static final RegistryKey<World> MAGICANT_WORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("turnbattle", "magicant"));
   public static final Identifier BEGIN = Identifier.of("turnbattle", "magicant_begin");
   public static final Identifier CLASH_DONE = Identifier.of("turnbattle", "magicant_clash_done");
   public static final Identifier WAKE = Identifier.of("turnbattle", "magicant_wake");
   public static final Identifier STATE = Identifier.of("turnbattle", "magicant_state");
   public static EntityType<MirrorCloneEntity> MIRROR_CLONE;
   private static final Map<UUID, MagicantManager.Session> SESSIONS = new HashMap<>();
   private static final int CLEAR_RADIUS_CHUNKS = 7;
   private static final Random LINE_RNG = new Random();

   private MagicantManager() {
   }

   public static Vec3d arenaCenter(UUID player) {
      int slot = Math.floorMod(player.hashCode(), 4096);
      return new Vec3d((double)(slot % 64 * 1024) + 512.5, 1.0, (double)(slot / 64 * 1024) + 512.5);
   }

   public static boolean isBusy(UUID player) {
      MagicantManager.Session s = SESSIONS.get(player);
      return s != null && s.phase != MagicantManager.Phase.BATTLE;
   }

   static boolean cloneStillWanted(MirrorCloneEntity e) {
      for (MagicantManager.Session s : SESSIONS.values()) {
         if (s.cloneId == e.getId()) {
            return true;
         }
      }

      return false;
   }

   static void register() {
      MIRROR_CLONE = (EntityType<MirrorCloneEntity>)Registry.register(
         Registries.ENTITY_TYPE,
         Identifier.of("turnbattle", "mirror_clone"),
         Builder.create(MirrorCloneEntity::new, SpawnGroup.MISC).dimensions(0.6F, 1.8F).eyeHeight(1.62F).maxTrackingRange(10).disableSaving().build()
      );
      FabricDefaultAttributeRegistry.register(MIRROR_CLONE, MirrorCloneEntity.createAttributes());

      for (Identifier id : new Identifier[]{BEGIN, CLASH_DONE, WAKE, STATE}) {
         Net121.registerS2CChannel(id);
         Net121.registerC2SChannel(id);
      }

      Net121.registerServerReceiver(CLASH_DONE, (server, player, handler, buf, sender) -> server.execute(() -> onClashDone(player)));
      UseBlockCallback.EVENT.register((UseBlockCallback)(player, world, hand, hit) -> {
         if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            if (!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock)) {
               return ActionResult.PASS;
            } else {
               return onBed(sp) ? ActionResult.SUCCESS : ActionResult.PASS;
            }
         } else {
            return ActionResult.PASS;
         }
      });
      ServerTickEvents.END_SERVER_TICK.register(MagicantManager::tick);
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, reg, env) -> dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundmagicant")
                              .requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.literal("seen")
                                 .then(
                                    CommandManager.argument("player", EntityArgumentType.player())
                                       .executes(
                                          ctx -> {
                                             ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                             BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());
                                             data.getOrCreate(target.getUuid()).magicantSeen = false;
                                             data.markDirty();
                                             ((ServerCommandSource)ctx.getSource())
                                                .sendFeedback(
                                                   () -> Text.literal(target.getName().getString() + " will see the MAGICANT cutscene again next time."), true
                                                );
                                             return 1;
                                          }
                                       )
                                 )
                           ))
                        .then(
                           CommandManager.literal("give")
                              .then(
                                 CommandManager.argument("player", EntityArgumentType.player())
                                    .executes(
                                       ctx -> {
                                          ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                          BattleData.PlayerProgress prog = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer())
                                             .getOrCreate(target.getUuid());
                                          if (prog.magicant) {
                                             ((ServerCommandSource)ctx.getSource())
                                                .sendError(Text.literal(target.getName().getString() + " has already awakened MAGICANT."));
                                             return 0;
                                          } else {
                                             awaken(target);
                                             target.sendMessage(
                                                Text.literal("* MAGICANT has awakened within you.")
                                                   .formatted(new Formatting[]{Formatting.LIGHT_PURPLE, Formatting.BOLD}),
                                                false
                                             );
                                             ((ServerCommandSource)ctx.getSource())
                                                .sendFeedback(
                                                   () -> Text.literal(
                                                         "Gave "
                                                            + target.getName().getString()
                                                            + " the MAGICANT boost (stat points doubled, 2 per point from now on)."
                                                      ),
                                                   true
                                                );
                                             return 1;
                                          }
                                       }
                                    )
                              )
                        ))
                     .then(
                        CommandManager.literal("remove")
                           .then(
                              CommandManager.argument("player", EntityArgumentType.player())
                                 .executes(
                                    ctx -> {
                                       ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                       BattleData.PlayerProgress prog = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer())
                                          .getOrCreate(target.getUuid());
                                       if (!prog.magicant) {
                                          ((ServerCommandSource)ctx.getSource())
                                             .sendError(Text.literal(target.getName().getString() + " doesn't have the MAGICANT boost."));
                                          return 0;
                                       } else {
                                          revoke(target);
                                          target.sendMessage(Text.literal("* MAGICANT's power has left you.").formatted(Formatting.GRAY), false);
                                          ((ServerCommandSource)ctx.getSource())
                                             .sendFeedback(
                                                () -> Text.literal(
                                                      "Removed the MAGICANT boost from "
                                                         + target.getName().getString()
                                                         + " (stat points halved, 1 per point from now on)."
                                                   ),
                                                true
                                             );
                                          return 1;
                                       }
                                    }
                                 )
                           )
                     )
               )
         );
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> server.execute(() -> {
            ServerPlayerEntity p = handler.getPlayer();
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(p.getUuid());
            sendState(p, prog.magicant);
            if (prog.magicantReturnDim != null && !SESSIONS.containsKey(p.getUuid())) {
               returnHome(p, prog, false);
               p.setInvulnerable(false);
            }
         }));
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         MagicantManager.Session s = SESSIONS.remove(handler.getPlayer().getUuid());
         if (s != null) {
            discardClone(server, s);
            handler.getPlayer().setInvulnerable(false);
         }
      });
   }

   static void sendState(ServerPlayerEntity p, boolean magicant) {
      PacketByteBuf b = PacketByteBufs.create();
      b.writeBoolean(magicant);
      Net121.sendToClient(p, STATE, b);
   }

   static boolean onBed(ServerPlayerEntity p) {
      MinecraftServer server = p.getServer();
      if (server != null && !SESSIONS.containsKey(p.getUuid()) && !BattleManager.isInBattle(p.getUuid())) {
         BattleData data = BattleData.get(server);
         BattleData.PlayerProgress prog = data.getOrCreate(p.getUuid());
         if (prog.magicant || prog.playerRace != CharacterRace.HUMAN || prog.level < 50) {
            return false;
         } else if (prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty()) {
            begin(p, prog, data);
            return true;
         } else {
            p.sendMessage(
               Text.literal("Something calls to you as you lie down... but you have no weapon in hand.")
                  .formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.ITALIC}),
               true
            );
            return false;
         }
      } else {
         return false;
      }
   }

   private static void begin(ServerPlayerEntity p, BattleData.PlayerProgress prog, BattleData data) {
      MagicantManager.Session s = new MagicantManager.Session(p.getUuid());
      s.skip = prog.magicantSeen;
      SESSIONS.put(p.getUuid(), s);
      prog.magicantReturnDim = p.getWorld().getRegistryKey().getValue().toString();
      prog.magicantRetX = p.getX();
      prog.magicantRetY = p.getY();
      prog.magicantRetZ = p.getZ();
      prog.magicantRetYaw = p.getYaw();
      boolean hadTeam = unequipTeammates(p, prog);
      data.markDirty();
      holdSafe(p);
      if (hadTeam) {
         p.sendMessage(
            Text.literal("* Your companions can't follow you where you're going.").formatted(new Formatting[]{Formatting.DARK_GRAY, Formatting.ITALIC}), false
         );
         TurnBattleMod.sendRecruitInfo(p);
         TurnBattleMod.sendPartyInfo(p);
      }

      PacketByteBuf begin = PacketByteBufs.create();
      begin.writeBoolean(s.skip);
      Net121.sendToClient(p, BEGIN, begin);
   }

   private static boolean unequipTeammates(ServerPlayerEntity p, BattleData.PlayerProgress prog) {
      boolean any = false;
      RecruitData rd = RecruitData.get(p.getServer());
      RecruitData.PlayerRecruits pr = rd.getOrCreate(p.getUuid());
      if (pr.activeRecruit >= 0) {
         pr.activeRecruit = -1;
         any = true;
      }

      if (pr.secondActiveRecruit >= 0) {
         pr.secondActiveRecruit = -1;
         any = true;
      }

      if (any) {
         rd.markDirty();
      }

      if (prog.partyMember != null && prog.partyMember != PartyMember.NONE) {
         prog.partyMember = PartyMember.NONE;
         any = true;
      }

      return any;
   }

   private static void holdSafe(ServerPlayerEntity p) {
      BattleManager.runInvincibleUntil.remove(p.getUuid());
      p.setInvulnerable(true);
      p.setFireTicks(0);
      p.extinguish();
   }

   private static void tick(MinecraftServer server) {
      if (!SESSIONS.isEmpty()) {
         for (MagicantManager.Session s : new ArrayList<>(SESSIONS.values())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(s.player);
            if (p == null) {
               SESSIONS.remove(s.player);
               discardClone(server, s);
            } else {
               s.ticks++;
               if (s.phase != MagicantManager.Phase.BATTLE) {
                  holdSafe(p);
               }

               if (!s.toClear.isEmpty()) {
                  clearSome(server, s);
               }

               switch (s.phase) {
                  case FADING:
                     if (s.ticks >= 26) {
                        enterRealm(p, s);
                     }
                     break;
                  case CUTSCENE:
                     if (s.skip && s.ticks >= 30) {
                        onClashDone(p);
                     } else if (s.ticks > 3600) {
                        onClashDone(p);
                     }
                     break;
                  case BATTLE:
                     if (!BattleManager.isInBattle(p.getUuid()) && s.ticks > 60) {
                        startWaking(p, s, s.outcome);
                     }
                     break;
                  case WAKING:
                     if (s.ticks == 24) {
                        BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(p.getUuid());
                        discardClone(server, s);
                        returnHome(p, prog, s.outcome == 1);
                     }

                     if (s.ticks >= 70) {
                        SESSIONS.remove(s.player);
                        p.setInvulnerable(false);
                     }
               }
            }
         }
      }
   }

   private static void enterRealm(ServerPlayerEntity p, MagicantManager.Session s) {
      MinecraftServer server = p.getServer();
      ServerWorld realm = server.getWorld(MAGICANT_WORLD);
      if (realm != null) {
         s.arena = arenaCenter(p.getUuid());
         double x = s.arena.x;
         double z = s.arena.z;
         s.arenaWorld = MAGICANT_WORLD;
         realm.getChunk(BlockPos.ofFloored(s.arena));
         p.teleport(realm, x, 1.0, z, 180.0F, 0.0F);
      } else {
         TurnBattleMod.LOGGER.warn("[Magicant] dimension turnbattle:magicant isn't loaded; staying in place");
         s.arena = p.getPos();
         s.arenaWorld = p.getWorld().getRegistryKey();
      }

      p.setVelocity(Vec3d.ZERO);
      if (realm != null) {
         queueClearing(s);
      }

      s.to(MagicantManager.Phase.CUTSCENE);
   }

   private static void queueClearing(MagicantManager.Session s) {
      ChunkPos c = new ChunkPos(BlockPos.ofFloored(s.arena));
      List all = new ArrayList<>();

      for (int dx = -7; dx <= 7; dx++) {
         for (int dz = -7; dz <= 7; dz++) {
            all.add(new ChunkPos(c.x + dx, c.z + dz));
         }
      }

      all.sort(Comparator.comparingInt(cp -> (cp.x - c.x) * (cp.x - c.x) + (cp.z - c.z) * (cp.z - c.z)));
      s.toClear.clear();
      s.toClear.addAll(all);
   }

   private static void clearSome(MinecraftServer server, MagicantManager.Session s) {
      ServerWorld realm = server.getWorld(MAGICANT_WORLD);
      if (realm == null) {
         s.toClear.clear();
      } else {
         BlockState air = Blocks.AIR.getDefaultState();
         Mutable pos = new Mutable();
         int n = 0;

         for (int per = s.skip ? 16 : 8; n < per && !s.toClear.isEmpty(); n++) {
            ChunkPos cp = s.toClear.pollFirst();
            WorldChunk chunk = realm.getChunk(cp.x, cp.z);

            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 1; y <= 3; y++) {
                     pos.set(cp.getStartX() + x, y, cp.getStartZ() + z);
                     if (!chunk.getBlockState(pos).getFluidState().isEmpty()) {
                        realm.setBlockState(pos, air, 18);
                     }
                  }
               }
            }
         }
      }
   }

   static void onClashDone(ServerPlayerEntity p) {
      MagicantManager.Session s = SESSIONS.get(p.getUuid());
      if (s != null && s.phase == MagicantManager.Phase.CUTSCENE) {
         MinecraftServer server = p.getServer();
         ServerWorld world = server.getWorld(s.arenaWorld != null ? s.arenaWorld : MAGICANT_WORLD);
         if (world == null || world != p.getWorld()) {
            world = (ServerWorld)p.getWorld();
         }

         BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(p.getUuid());
         if (!prog.magicantSeen) {
            prog.magicantSeen = true;
            BattleData.get(server).markDirty();
         }

         float yaw = p.getYaw();
         Vec3d fwd = Vec3d.fromPolar(0.0F, yaw).multiply(1.0, 0.0, 1.0).normalize();
         Vec3d at = p.getPos().add(fwd.multiply(3.2));
         MirrorCloneEntity clone = (MirrorCloneEntity)MIRROR_CLONE.create(world);
         if (clone == null) {
            startWaking(p, s, 0);
         } else {
            String name = prog.vesselName != null && !prog.vesselName.isEmpty() ? prog.vesselName : p.getName().getString();
            clone.setOwner(p.getUuid(), name);
            clone.refreshPositionAndAngles(at.x, p.getY(), at.z, MathHelper.wrapDegrees(yaw + 180.0F), 0.0F);
            clone.setHeadYaw(yaw + 180.0F);
            clone.setBodyYaw(yaw + 180.0F);
            ItemStack weapon = prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty() ? prog.equippedWeapon.copy() : new ItemStack(Items.IRON_SWORD);
            clone.equipStack(EquipmentSlot.MAINHAND, weapon);
            clone.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
            clone.setCustomName(Text.literal(name));
            s.cloneId = clone.getId();
            world.spawnEntity(clone);
            MobLevelManager.restoreLevel(clone, Math.min(99, prog.level + 5));
            EliteMobManager.makeElite(clone, 4);
            clone.setPersistent();
            clone.setCustomNameVisible(false);
            p.setInvulnerable(false);
            s.to(MagicantManager.Phase.BATTLE);
            BattleManager.tryStart(p, clone);
            if (!BattleManager.isInBattle(p.getUuid())) {
               TurnBattleMod.LOGGER.warn("[Magicant] the mirror battle didn't start for {}", p.getName().getString());
               startWaking(p, s, 0);
            }
         }
      }
   }

   static void onBattleEnd(BattleState state, MinecraftServer server, String outcome) {
      MagicantManager.Session s = SESSIONS.get(state.playerUuid);
      if (s != null) {
         ServerPlayerEntity p = server.getPlayerManager().getPlayer(state.playerUuid);
         boolean won = "win".equals(outcome);
         if (won && p != null) {
            awaken(p);
         }

         s.outcome = won ? 1 : 0;
         s.ticks = 0;
         if (p != null) {
            holdSafe(p);
         }
      }
   }

   private static void startWaking(ServerPlayerEntity p, MagicantManager.Session s, int outcome) {
      s.outcome = outcome;
      s.to(MagicantManager.Phase.WAKING);
      holdSafe(p);
      PacketByteBuf b = PacketByteBufs.create();
      b.writeByte(outcome);
      Net121.sendToClient(p, WAKE, b);
   }

   static void awaken(ServerPlayerEntity p) {
      BattleData data = BattleData.get(p.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(p.getUuid());
      if (!prog.magicant) {
         prog.magicant = true;
         CharStats st = prog.playerStats;
         if (st.pointValue < 2) {
            st.offense *= 2;
            st.defense *= 2;
            st.magic *= 2;
            st.iq *= 2;
            st.vitality *= 2;
            st.luck *= 2;

            for (int i = 0; i < prog.treeStatBonuses.length; i++) {
               prog.treeStatBonuses[i] = prog.treeStatBonuses[i] * 2;
            }

            st.pointValue = 2;
         }

         data.markDirty();
         MobLevelManager.applyPlayerStats(p, prog.level, prog.playerStats);
         p.setHealth(p.getMaxHealth());
         sendState(p, true);
         TurnBattleMod.sendPartyInfo(p);
      }
   }

   static void revoke(ServerPlayerEntity p) {
      BattleData data = BattleData.get(p.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(p.getUuid());
      if (prog.magicant) {
         prog.magicant = false;
         CharStats st = prog.playerStats;
         if (st.pointValue >= 2) {
            st.offense /= 2;
            st.defense /= 2;
            st.magic /= 2;
            st.iq /= 2;
            st.vitality /= 2;
            st.luck /= 2;

            for (int i = 0; i < prog.treeStatBonuses.length; i++) {
               prog.treeStatBonuses[i] = prog.treeStatBonuses[i] / 2;
            }

            st.pointValue = 1;
         }

         data.markDirty();
         MobLevelManager.applyPlayerStats(p, prog.level, prog.playerStats);
         if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
         }

         sendState(p, false);
         TurnBattleMod.sendPartyInfo(p);
      }
   }

   private static void returnHome(ServerPlayerEntity p, BattleData.PlayerProgress prog, boolean won) {
      MinecraftServer server = p.getServer();
      ServerWorld home = null;
      if (prog.magicantReturnDim != null) {
         Identifier id = Identifier.tryParse(prog.magicantReturnDim);
         if (id != null) {
            home = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
         }
      }

      if (home == null) {
         home = server.getOverworld();
      }

      double x = prog.magicantRetX;
      double y = prog.magicantRetY;
      double z = prog.magicantRetZ;
      if (prog.magicantReturnDim == null) {
         BlockPos sp = home.getSpawnPos();
         x = (double)sp.getX() + 0.5;
         y = (double)sp.getY();
         z = (double)sp.getZ() + 0.5;
      }

      p.teleport(home, x, y, z, prog.magicantRetYaw, 0.0F);
      p.setHealth(p.getMaxHealth());
      p.getHungerManager().setFoodLevel(Math.max(p.getHungerManager().getFoodLevel(), 18));
      prog.magicantReturnDim = null;
      BattleData.get(server).markDirty();
      if (won) {
         p.sendMessage(Text.literal("* MAGICANT has awakened within you.").formatted(new Formatting[]{Formatting.LIGHT_PURPLE, Formatting.BOLD}), false);
         p.sendMessage(Text.literal("* Your stat points are doubled - every point you spend now counts twice.").formatted(Formatting.LIGHT_PURPLE), false);
         p.getWorld().playSound(null, p.getBlockPos(), TurnBattleSounds.SND_LEVELUPMIX, SoundCategory.PLAYERS, 1.0F, 1.0F);
      } else {
         p.sendMessage(Text.literal("* ...You wake up. The bed is still warm.").formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}), false);
      }
   }

   private static void discardClone(MinecraftServer server, MagicantManager.Session s) {
      if (s.cloneId >= 0) {
         for (ServerWorld w : server.getWorlds()) {
            Entity e = w.getEntityById(s.cloneId);
            if (e instanceof MirrorCloneEntity) {
               e.discard();
               break;
            }
         }

         s.cloneId = -1;
      }
   }

   static String enemyTurnLine(BattleState state, int pattern, ServerPlayerEntity player) {
      int base = pattern % 1000;
      if (pattern >= 32 && pattern <= 41) {
         return "Your reflection slashes at you!";
      } else if (base == 320) {
         String skill = randomOwnSkill(player);
         return "Your reflection used " + (skill != null ? skill : "your own power") + "!";
      } else {
         return base == 321 ? "Your reflection mirrors your every swing!" : "Your reflection raises your weapon.";
      }
   }

   private static String randomOwnSkill(ServerPlayerEntity player) {
      if (player != null && player.getServer() != null) {
         BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(player.getUuid());
         List names = new ArrayList<>();

         for (String id : prog.unlockedSkills) {
            String n = skillName(id);
            if (n != null) {
               names.add(n);
            }
         }

         return names.isEmpty() ? null : names.get(LINE_RNG.nextInt(names.size()));
      } else {
         return null;
      }
   }

   private static String skillName(String id) {
      try {
         return PsiSkill.valueOf(id).getDisplayName();
      } catch (Exception var6) {
         try {
            return LightnerSkill.valueOf(id).getDisplayName();
         } catch (Exception var5) {
            try {
               return HumanMageSkill.valueOf(id).getDisplayName();
            } catch (Exception var4) {
               try {
                  return SoulMageSkill.valueOf(id).getDisplayName();
               } catch (Exception var3) {
                  try {
                     return OriSkill.valueOf(id).getDisplayName();
                  } catch (Exception var2) {
                     return null;
                  }
               }
            }
         }
      }
   }

   static enum Phase {
      FADING,
      CUTSCENE,
      BATTLE,
      WAKING;
   }

   static final class Session {
      final UUID player;
      MagicantManager.Phase phase = MagicantManager.Phase.FADING;
      int ticks;
      int cloneId = -1;
      int outcome = 0;
      Vec3d arena;
      RegistryKey<World> arenaWorld;
      final ArrayDeque<ChunkPos> toClear = new ArrayDeque<>();
      boolean skip;

      Session(UUID p) {
         this.player = p;
      }

      void to(MagicantManager.Phase p) {
         this.phase = p;
         this.ticks = 0;
      }
   }
}
