package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.Before;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class SoulWalk {
   public static final float SOUL_W = 0.5F;
   public static final float SOUL_H = 0.5F;
   public static final float SOUL_EYE = 0.25F;
   static final double CHEST_Y = 1.15;
   static final double ENTER_DIST = 0.6;
   static final double LEAVE_DIST = 1.4;
   public static final Identifier SYNC = Identifier.of("turnbattle", "soul_walk_sync");
   public static final Identifier GRAZE = Identifier.of("turnbattle", "soul_walk_graze");
   public static EntityType<SoulBodyEntity> SOUL_BODY;
   private static final Map<UUID, SoulWalk.Out> OUT = new ConcurrentHashMap<>();
   public static final Map<UUID, SoulType> CLIENT_OUT = new ConcurrentHashMap<>();
   public static volatile boolean localOut = false;

   private SoulWalk() {
   }

   public static boolean isOut(UUID uuid) {
      return uuid != null && OUT.containsKey(uuid);
   }

   public static boolean isOut(PlayerEntity p) {
      if (p != null && p.getWorld() != null) {
         return p.getWorld().isClient ? CLIENT_OUT.containsKey(p.getUuid()) : OUT.containsKey(p.getUuid());
      } else {
         return false;
      }
   }

   public static void register() {
      SOUL_BODY = (EntityType<SoulBodyEntity>)Registry.register(
         Registries.ENTITY_TYPE,
         Identifier.of("turnbattle", "soul_body"),
         Builder.create(SoulBodyEntity::new, SpawnGroup.MISC).dimensions(0.6F, 1.8F).maxTrackingRange(10).disableSaving().build()
      );
      FabricDefaultAttributeRegistry.register(SOUL_BODY, MobEntity.createMobAttributes());
      Net121.registerS2CChannel(SYNC);
      Net121.registerS2CChannel(GRAZE);
      Identifier first = Identifier.of("turnbattle", "soul_walk_first");
      Identifier DEF = Event.DEFAULT_PHASE;
      UseBlockCallback.EVENT.addPhaseOrdering(first, DEF);
      UseBlockCallback.EVENT.register(first, (UseBlockCallback)(p, w, h, hit) -> isOut(p) ? ActionResult.FAIL : ActionResult.PASS);
      AttackBlockCallback.EVENT.addPhaseOrdering(first, DEF);
      AttackBlockCallback.EVENT.register(first, (AttackBlockCallback)(p, w, h, pos, dir) -> isOut(p) ? ActionResult.FAIL : ActionResult.PASS);
      UseItemCallback.EVENT.addPhaseOrdering(first, DEF);
      UseItemCallback.EVENT
         .register(first, (UseItemCallback)(p, w, h) -> isOut(p) ? TypedActionResult.fail(p.getStackInHand(h)) : TypedActionResult.pass(ItemStack.EMPTY));
      UseEntityCallback.EVENT.addPhaseOrdering(first, DEF);
      UseEntityCallback.EVENT.register(first, (UseEntityCallback)(p, w, h, e, hit) -> isOut(p) ? ActionResult.FAIL : ActionResult.PASS);
      AttackEntityCallback.EVENT.addPhaseOrdering(first, DEF);
      AttackEntityCallback.EVENT.register(first, (AttackEntityCallback)(p, w, h, e, hit) -> isOut(p) ? ActionResult.FAIL : ActionResult.PASS);
      PlayerBlockBreakEvents.BEFORE.register((Before)(w, p, pos, st, be) -> !isOut(p));
      ServerLifecycleEvents.SERVER_STOPPING.register((ServerStopping)server -> {
         for (ServerPlayerEntity p : new ArrayList(server.getPlayerManager().getPlayerList())) {
            if (isOut(p.getUuid())) {
               returnToBody(p, false);
            }
         }

         OUT.clear();
      });
   }

   public static boolean tryLeave(ServerPlayerEntity p) {
      MinecraftServer server = p.getServer();
      UUID u = p.getUuid();
      if (server != null && !isOut(u)) {
         BattleData data = BattleData.get(server);
         BattleData.PlayerProgress prog = data.getOrCreate(u);
         String why = null;
         if (prog.soulType == null || prog.playerRace == CharacterRace.MONSTER) {
            why = "";
         } else if (DepthsRun.isActive(u)) {
            why = "Ori's SOUL won't come out.";
         } else if (BattleManager.isInBattle(u)) {
            why = "";
         } else if (TurnBattleMod.isSealing(u) || DarkWorldExit.isExiting(u) || MagicantManager.isBusy(u)) {
            why = "";
         } else if (p.hasVehicle() || p.isSleeping()) {
            why = "Not while you're riding or resting.";
         } else if (!p.isOnGround() && !p.isTouchingWater()) {
            why = "Your body needs solid ground to stand on.";
         }

         if (why != null) {
            if (!why.isEmpty()) {
               p.sendMessage(Text.literal(why).formatted(Formatting.GRAY), true);
            }

            return false;
         } else {
            SoulWalk.Out o = new SoulWalk.Out();
            o.dim = p.getWorld().getRegistryKey();
            o.pos = p.getPos();
            o.yaw = p.getYaw();
            o.pitch = p.getPitch();
            o.since = server.getOverworld().getTime();
            o.soul = prog.soulType;
            OUT.put(u, o);
            sync(server);
            prog.soulOutDim = o.dim.getValue().toString();
            prog.soulOutX = o.pos.x;
            prog.soulOutY = o.pos.y;
            prog.soulOutZ = o.pos.z;
            prog.soulOutYaw = o.yaw;
            prog.soulOutPitch = o.pitch;
            data.markDirty();
            spawnBody(p, o);
            p.stopRiding();
            p.calculateDimensions();
            p.teleport(p.getServerWorld(), o.pos.x, o.pos.y + 1.15 - 0.25, o.pos.z, o.yaw, o.pitch);
            p.getAbilities().allowFlying = true;
            p.getAbilities().flying = true;
            p.sendAbilitiesUpdate();
            p.setVelocity(0.0, 0.04, 0.0);
            p.velocityModified = true;
            p.fallDistance = 0.0F;
            p.extinguish();
            TurnBattleMod.forceRevealOff(server, p);
            return true;
         }
      } else {
         return false;
      }
   }

   private static void spawnBody(ServerPlayerEntity p, SoulWalk.Out o) {
      ServerWorld w = p.getServer().getWorld(o.dim);
      if (w != null) {
         SoulBodyEntity body = (SoulBodyEntity)SOUL_BODY.create(w);
         if (body != null) {
            BattleData.PlayerProgress prog = BattleData.get(p.getServer()).getOrCreate(p.getUuid());
            String name = prog.vesselName != null && !prog.vesselName.isEmpty() ? prog.vesselName : p.getName().getString();
            body.setOwner(p.getUuid(), name);
            body.refreshPositionAndAngles(o.pos.x, o.pos.y, o.pos.z, o.yaw, 0.0F);
            body.setHeadYaw(o.yaw);
            body.setBodyYaw(o.yaw);

            for (EquipmentSlot slot : new EquipmentSlot[]{
               EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            }) {
               body.equipStack(slot, p.getEquippedStack(slot).copy());
            }

            w.spawnEntity(body);
            o.bodyId = body.getUuid();
         }
      }
   }

   public static void returnToBody(ServerPlayerEntity p, boolean graze) {
      MinecraftServer server = p.getServer();
      SoulWalk.Out o = OUT.remove(p.getUuid());
      if (server != null && o != null) {
         ServerWorld w = server.getWorld(o.dim);
         if (w == null) {
            w = server.getOverworld();
         }

         Entity body = o.bodyId != null ? w.getEntity(o.bodyId) : null;
         if (body != null) {
            body.discard();
         }

         p.teleport(w, o.pos.x, o.pos.y, o.pos.z, o.yaw, o.pitch);
         p.calculateDimensions();
         p.setVelocity(Vec3d.ZERO);
         p.velocityModified = true;
         p.fallDistance = 0.0F;
         clearSaved(server, p.getUuid());
         sync(server);
         restoreAbilities(p);
         if (graze) {
            for (ServerPlayerEntity other : w.getPlayers()) {
               if (!(other.squaredDistanceTo(p) > 4096.0)) {
                  PacketByteBuf b = PacketByteBufs.create();
                  b.writeInt(p.getId());
                  Net121.sendToClient(other, GRAZE, b);
               }
            }
         } else {
            w.playSound(null, p.getBlockPos(), TurnBattleSounds.SND_GRAB, SoundCategory.PLAYERS, 0.8F, 1.1F);
         }
      }
   }

   private static void restoreAbilities(ServerPlayerEntity p) {
      p.interactionManager.getGameMode().setAbilities(p.getAbilities());
      if (!p.isCreative() && !p.isSpectator()) {
         p.getAbilities().allowFlying = false;
         p.getAbilities().flying = false;
      }

      p.sendAbilitiesUpdate();
   }

   private static void clearSaved(MinecraftServer server, UUID u) {
      BattleData data = BattleData.get(server);
      BattleData.PlayerProgress prog = data.getOrCreate(u);
      if (prog.soulOutDim != null) {
         prog.soulOutDim = null;
         data.markDirty();
      }
   }

   public static void tick(MinecraftServer server) {
      if (!OUT.isEmpty()) {
         long now = server.getOverworld().getTime();

         for (Entry e : new ArrayList<>(OUT.entrySet())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            SoulWalk.Out o = e.getValue();
            if (p == null) {
               OUT.remove(e.getKey());
            } else if (p.isAlive()) {
               if (!p.getWorld().getRegistryKey().equals(o.dim)) {
                  returnToBody(p, false);
               } else {
                  if (!p.getAbilities().flying || !p.getAbilities().allowFlying) {
                     p.getAbilities().allowFlying = true;
                     p.getAbilities().flying = true;
                     p.sendAbilitiesUpdate();
                  }

                  p.fallDistance = 0.0F;
                  if (p.isOnFire()) {
                     p.extinguish();
                  }

                  ServerWorld w = (ServerWorld)p.getWorld();
                  Entity body = o.bodyId != null ? w.getEntity(o.bodyId) : null;
                  if (body == null && now % 10L == 0L && w.isChunkLoaded(BlockPos.ofFloored(o.pos))) {
                     spawnBody(p, o);
                     body = o.bodyId != null ? w.getEntity(o.bodyId) : null;
                  }

                  Vec3d chest = (body != null ? body.getPos() : o.pos).add(0.0, 1.15, 0.0);
                  Vec3d soul = p.getPos().add(0.0, (double)p.getHeight() / 2.0, 0.0);
                  double d = soul.distanceTo(chest);
                  if (d > 1.4) {
                     o.leftBody = true;
                  }

                  if (o.leftBody && now - o.since > 10L && d < 0.6) {
                     returnToBody(p, true);
                  }
               }
            }
         }
      }
   }

   public static void onDisconnect(ServerPlayerEntity p) {
      if (isOut(p.getUuid())) {
         returnToBody(p, false);
      }
   }

   public static void onJoin(ServerPlayerEntity p) {
      MinecraftServer server = p.getServer();
      if (server != null) {
         BattleData data = BattleData.get(server);
         BattleData.PlayerProgress prog = data.getOrCreate(p.getUuid());
         if (prog.soulOutDim != null) {
            Identifier id = Identifier.tryParse(prog.soulOutDim);
            ServerWorld w = id != null ? server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id)) : null;
            if (w != null) {
               p.teleport(w, prog.soulOutX, prog.soulOutY, prog.soulOutZ, prog.soulOutYaw, prog.soulOutPitch);
            }

            prog.soulOutDim = null;
            data.markDirty();
         }

         p.calculateDimensions();
         restoreAbilities(p);
         if (prog.soulRemoved) {
            prog.soulRemoved = false;
            data.markDirty();
            String own = (prog.vesselName != null && !prog.vesselName.isEmpty() ? prog.vesselName : p.getName().getString()) + "'s SOUL";
            PlayerInventory inv = p.getInventory();

            for (int i = 0; i < inv.size(); i++) {
               ItemStack s = inv.getStack(i);
               if (s.getItem() instanceof SoulItem && s.getName().getString().equals(own)) {
                  inv.setStack(i, ItemStack.EMPTY);
               }
            }

            Net121.sendToClient(p, TurnBattleMod.SOUL_RESTORED, PacketByteBufs.empty());
         }

         sendSyncTo(p);
      }
   }

   public static void onRespawn(ServerPlayerEntity newP) {
      SoulWalk.Out o = OUT.remove(newP.getUuid());
      MinecraftServer server = newP.getServer();
      if (server != null) {
         if (o != null) {
            ServerWorld w = server.getWorld(o.dim);
            Entity body = w != null && o.bodyId != null ? w.getEntity(o.bodyId) : null;
            if (body != null) {
               body.discard();
            }
         }

         clearSaved(server, newP.getUuid());
         newP.calculateDimensions();
         restoreAbilities(newP);
         sync(server);
      }
   }

   static boolean bodyWanted(SoulBodyEntity body) {
      UUID owner = body.getOwner();
      SoulWalk.Out o = owner != null ? OUT.get(owner) : null;
      return o != null && body.getUuid().equals(o.bodyId);
   }

   private static PacketByteBuf syncBuf() {
      PacketByteBuf b = PacketByteBufs.create();
      b.writeInt(OUT.size());

      for (Entry e : OUT.entrySet()) {
         b.writeUuid(e.getKey());
         b.writeInt(e.getValue().soul != null ? e.getValue().soul.ordinal() : -1);
      }

      return b;
   }

   static void sync(MinecraftServer server) {
      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         sendSyncTo(p);
      }
   }

   private static void sendSyncTo(ServerPlayerEntity p) {
      try {
         Net121.sendToClient(p, SYNC, syncBuf());
      } catch (Exception var2) {
      }
   }

   static final class Out {
      RegistryKey<World> dim;
      Vec3d pos;
      float yaw;
      float pitch;
      UUID bodyId;
      long since;
      boolean leftBody;
      SoulType soul;
   }
}
