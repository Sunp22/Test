package com.pixelcite.turnbattle;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class MoralityManager {
   public static final String EVT_KILL_WARNING = "KILL_WARNING";
   public static final String EVT_RUTHLESS = "RUTHLESS";
   public static final String EVT_SPARE_CANCEL = "SPARE_CANCEL";
   public static final boolean ROUTE_ENABLED = false;

   public static boolean isRuthless(BattleData.PlayerProgress prog) {
      return false;
   }

   public static void onBattleEnd(ServerPlayerEntity player, String outcome) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      boolean wasKill = "win".equals(outcome);
      boolean wasMercy = "mercy".equals(outcome);
      if (wasKill || wasMercy) {
         ;
      }
   }

   public static void syncToPlayer(ServerPlayerEntity player) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeString("SYNC");
      buf.writeBoolean(isRuthless(prog));
      Net121.sendToClient(player, TurnBattleMod.MORALITY_EVENT, buf);
   }

   private static void send(ServerPlayerEntity player, String type, boolean ruthless) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeString(type);
      buf.writeBoolean(ruthless);
      Net121.sendToClient(player, TurnBattleMod.MORALITY_EVENT, buf);
   }
}
