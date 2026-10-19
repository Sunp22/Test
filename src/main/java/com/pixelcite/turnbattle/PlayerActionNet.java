package com.pixelcite.turnbattle;

import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

final class PlayerActionNet {
   private PlayerActionNet() {
   }

   static void broadcast(ServerPlayerEntity actor, int kind, boolean includeActor, Consumer<PacketByteBuf> extras) {
      ServerWorld world = actor.getServerWorld();
      double r2 = 4096.0;
      UUID id = actor.getUuid();
      float yaw = actor.getYaw();

      for (ServerPlayerEntity p : world.getPlayers()) {
         if ((p != actor || includeActor) && (p == actor || !(p.squaredDistanceTo(actor) > r2))) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeByte(kind);
            buf.writeUuid(id);
            buf.writeFloat(yaw);
            if (extras != null) {
               extras.accept(buf);
            }

            Net121.sendToClient(p, TurnBattleMod.PLAYER_ANIM, buf);
         }
      }
   }
}
