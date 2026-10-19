package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;

public class BossBeaconData extends PersistentState {
   private static final String KEY = "turnbattle_boss_beacons";
   public final Map<BlockPos, BossBeaconData.BossBeaconLobby> lobbies = new HashMap<>();

   public static BossBeaconData get(MinecraftServer server) {
      return (BossBeaconData)server.getWorld(World.OVERWORLD)
         .getPersistentStateManager()
         .getOrCreate(new Type(BossBeaconData::new, (BossBeaconDatan, BossBeaconDatal) -> fromNbt(BossBeaconDatan), null), "turnbattle_boss_beacons");
   }

   public static BossBeaconData fromNbt(NbtCompound nbt) {
      return new BossBeaconData();
   }

   public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
      return nbt;
   }

   public static class BossBeaconLobby {
      public final UUID hostUuid;
      public final List<UUID> playerUuids = new ArrayList<>();
      public final List<String> playerNames = new ArrayList<>();
      public int difficulty = -1;
      public boolean started = false;

      public BossBeaconLobby(UUID host, String hostName) {
         this.hostUuid = host;
         this.playerUuids.add(host);
         this.playerNames.add(hostName);
      }

      public void addPlayer(UUID uuid, String name) {
         if (!this.playerUuids.contains(uuid)) {
            this.playerUuids.add(uuid);
            this.playerNames.add(name);
         }
      }

      public void removePlayer(UUID uuid) {
         int idx = this.playerUuids.indexOf(uuid);
         if (idx >= 0) {
            this.playerUuids.remove(idx);
            this.playerNames.remove(idx);
         }
      }

      public boolean isHost(UUID uuid) {
         return this.hostUuid.equals(uuid);
      }
   }
}
