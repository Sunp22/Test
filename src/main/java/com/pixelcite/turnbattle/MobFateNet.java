package com.pixelcite.turnbattle;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class MobFateNet {
   public static final Identifier CHANNEL = Identifier.of("turnbattle", "mob_fate");
   public static final int KILL = 0;
   public static final int SPARE = 1;
   public static final int ICE = 2;
   private static final double RANGE = 30.0;

   private MobFateNet() {
   }

   public static void register() {
      Net121.registerS2CChannel(CHANNEL);
      Net121.registerC2SChannel(CHANNEL);
   }

   public static void send(ServerWorld world, Entity e, int kind) {
      if (world != null && e != null) {
         double r2 = 900.0;

         for (ServerPlayerEntity p : world.getPlayers()) {
            if (!(p.squaredDistanceTo(e) > r2)) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(e.getId());
               buf.writeByte(kind);
               Net121.sendToClient(p, CHANNEL, buf);
            }
         }
      }
   }

   public static int killKind(BattleState state, int enemyId, long now) {
      if (state == null) {
         return 0;
      } else if (state.enemyFreezeTurnsMap.getOrDefault(enemyId, 0) > 0) {
         return 2;
      } else {
         Long t = state.iceHitAt.get(enemyId);
         return t != null && now - t <= 40L ? 2 : 0;
      }
   }

   public static boolean isIce(BattleSkill s) {
      if (s == null) {
         return false;
      } else if (s.getType() == SkillType.FREEZE) {
         return true;
      } else {
         Affinity[] a = s.getAffinities();
         if (a != null) {
            for (Affinity x : a) {
               if (x == Affinity.ICE) {
                  return true;
               }
            }
         }

         return false;
      }
   }
}
