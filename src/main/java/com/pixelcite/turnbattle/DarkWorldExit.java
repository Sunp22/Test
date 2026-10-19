package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class DarkWorldExit {
   public static final Identifier USE = Identifier.of("turnbattle", "dark_light_use");
   private static final Map<UUID, Long> exiting = new HashMap<>();
   private static final int RISE_STEP = 5;
   private static final int RISE_MAX_AMP = 5;

   private DarkWorldExit() {
   }

   public static boolean isExiting(UUID who) {
      return exiting.containsKey(who);
   }

   public static void onDisconnect(ServerPlayerEntity player) {
      if (exiting.remove(player.getUuid()) != null) {
         player.removeStatusEffect(StatusEffects.LEVITATION);
      }
   }

   public static void register() {
      Net121.registerS2CChannel(USE);
      Net121.registerC2SChannel(USE);
      Net121.registerServerReceiver(USE, (server, player, handler, buf, sender) -> server.execute(() -> onUse(server, player)));
   }

   private static void onUse(MinecraftServer server, ServerPlayerEntity player) {
      if (!exiting.containsKey(player.getUuid()) && player.isAlive() && !player.isSpectator()) {
         if (DarkFountainManager.isDarkWorld(player.getWorld().getRegistryKey())) {
            if (!BattleManager.isInBattle(player.getUuid())) {
               if (!TurnBattleMod.sealingPlayers.contains(player.getUuid()) && !MagicantManager.isBusy(player.getUuid())) {
                  if (!player.hasVehicle() && !player.isFallFlying() && !player.getAbilities().flying) {
                     DarkFountainData.DarkWorldInstance inst = DarkFountainData.get(server).getInstanceByWorldKey(player.getWorld().getRegistryKey());
                     if (inst != null) {
                        String dim = player.getWorld().getRegistryKey().getValue().toString();
                        int[] p = LightPillars.near(dim, inst.fountainX, inst.fountainZ, player.getX(), player.getZ(), 3.5);
                        if (p != null) {
                           exiting.put(player.getUuid(), server.getOverworld().getTime());
                           double cx = (double)p[0] + 0.5;
                           double cz = (double)p[1] + 0.5;
                           double y = player.getY();
                           if (player.getWorld().isSpaceEmpty(player, player.getBoundingBox().offset(cx - player.getX(), 0.0, cz - player.getZ()))) {
                              player.networkHandler.requestTeleport(cx, y, cz, player.getYaw(), player.getPitch());
                           }

                           player.setVelocity(0.0, 0.0, 0.0);
                           player.fallDistance = 0.0F;
                           PlayerActionNet.broadcast(player, 8, true, b -> {
                              b.writeDouble(cx);
                              b.writeDouble(y);
                              b.writeDouble(cz);
                           });
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void tick(MinecraftServer server) {
      if (!exiting.isEmpty()) {
         long now = server.getOverworld().getTime();
         Iterator it = exiting.entrySet().iterator();

         while (it.hasNext()) {
            Entry e = it.next();
            ServerPlayerEntity pl = server.getPlayerManager().getPlayer(e.getKey());
            long t = now - e.getValue();
            if (pl != null
               && pl.isAlive()
               && t <= 188L
               && !BattleManager.isInBattle(pl.getUuid())
               && DarkFountainManager.isDarkWorld(pl.getWorld().getRegistryKey())) {
               long u = t - 12L;
               if (u >= 0L && u % 5L == 0L && u / 5L <= 5L) {
                  int amp = (int)(u / 5L);
                  pl.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, (int)(88L - t + 10L), amp, false, false, false));
               }

               pl.fallDistance = 0.0F;
               if (t >= 88L) {
                  it.remove();
                  pl.removeStatusEffect(StatusEffects.LEVITATION);
                  pl.setVelocity(0.0, 0.0, 0.0);
                  pl.velocityModified = true;
                  DarkFountainManager.exitDarkWorld(pl, server, 0);
                  pl.fallDistance = 0.0F;
               }
            } else {
               if (pl != null) {
                  pl.removeStatusEffect(StatusEffects.LEVITATION);
               }

               it.remove();
            }
         }
      }
   }
}
