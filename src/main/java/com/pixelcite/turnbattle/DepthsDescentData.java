package com.pixelcite.turnbattle;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;

public class DepthsDescentData extends PersistentState {
   private static final String KEY = "depths_descent";
   public int globalSealedCount = 0;
   public final Map<String, DepthsDescentData.Instance> instances = new LinkedHashMap<>();

   public DepthsDescentData.Instance getOrCreate(String key, int sealedCount, long seed) {
      return this.instances.computeIfAbsent(key, k -> {
         int startFloor = 1 + sealedCount * 4;
         return new DepthsDescentData.Instance(k, startFloor, seed);
      });
   }

   public DepthsDescentData.Instance getInstance(String key) {
      return this.instances.get(key);
   }

   public void removeInstance(String key) {
      this.instances.remove(key);
   }

   public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
      nbt.putInt("globalSealed", this.globalSealedCount);
      NbtList list = new NbtList();

      for (DepthsDescentData.Instance inst : this.instances.values()) {
         list.add(inst.toNbt());
      }

      nbt.put("instances", list);
      return nbt;
   }

   public static DepthsDescentData fromNbt(NbtCompound nbt) {
      DepthsDescentData d = new DepthsDescentData();
      d.globalSealedCount = nbt.getInt("globalSealed");
      NbtList list = nbt.getList("instances", 10);

      for (int i = 0; i < list.size(); i++) {
         DepthsDescentData.Instance inst = DepthsDescentData.Instance.fromNbt(list.getCompound(i));
         d.instances.put(inst.fountainKey, inst);
      }

      return d;
   }

   public static DepthsDescentData get(MinecraftServer server) {
      return (DepthsDescentData)server.getWorld(World.OVERWORLD)
         .getPersistentStateManager()
         .getOrCreate(new Type(DepthsDescentData::new, (DepthsDescentDatan, DepthsDescentDatal) -> fromNbt(DepthsDescentDatan), null), "depths_descent");
   }

   public static class Instance {
      public String fountainKey;
      public int startingFloor;
      public int currentFloor;
      public boolean sealed = false;
      public long seed;
      public final Set<UUID> playersInside = new HashSet<>();
      public DepthsDescentRoom currentRoom = null;
      public final Set<String> chestOpenedByPlayers = new HashSet<>();

      public Instance(String key, int startFloor, long seed) {
         this.fountainKey = key;
         this.startingFloor = startFloor;
         this.currentFloor = startFloor;
         this.seed = seed;
      }

      public boolean isBossFloor() {
         return this.currentFloor % 5 == 0;
      }

      public int enemyLevel() {
         return this.currentFloor;
      }

      public NbtCompound toNbt() {
         NbtCompound t = new NbtCompound();
         t.putString("key", this.fountainKey);
         t.putInt("startFloor", this.startingFloor);
         t.putInt("currentFloor", this.currentFloor);
         t.putBoolean("sealed", this.sealed);
         t.putLong("seed", this.seed);
         NbtList pl = new NbtList();

         for (UUID u : this.playersInside) {
            NbtCompound pu = new NbtCompound();
            pu.putUuid("id", u);
            pl.add(pu);
         }

         t.put("players", pl);
         NbtList cl = new NbtList();

         for (String s : this.chestOpenedByPlayers) {
            NbtCompound ce = new NbtCompound();
            ce.putString("v", s);
            cl.add(ce);
         }

         t.put("chestOpened", cl);
         return t;
      }

      public static DepthsDescentData.Instance fromNbt(NbtCompound t) {
         DepthsDescentData.Instance i = new DepthsDescentData.Instance(t.getString("key"), t.getInt("startFloor"), t.getLong("seed"));
         i.currentFloor = t.getInt("currentFloor");
         i.sealed = t.getBoolean("sealed");
         NbtList pl = t.getList("players", 10);

         for (int j = 0; j < pl.size(); j++) {
            i.playersInside.add(pl.getCompound(j).getUuid("id"));
         }

         NbtList cl = t.getList("chestOpened", 10);

         for (int j = 0; j < cl.size(); j++) {
            i.chestOpenedByPlayers.add(cl.getCompound(j).getString("v"));
         }

         return i;
      }
   }
}
