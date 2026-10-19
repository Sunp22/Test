package com.pixelcite.turnbattle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;

public class DarkFountainData extends PersistentState {
   public static final String KEY = "dark_fountain_data";
   public final Map<Integer, DarkFountainData.DarkWorldInstance> instances = new HashMap<>();
   public int nextInstanceId = 1;
   public final Map<UUID, Integer> playerTints = new HashMap<>();
   public final Map<UUID, BlockPos> pendingExile = new HashMap<>();
   public final Map<UUID, String> pendingExileDim = new HashMap<>();
   public final Map<UUID, Integer> offlineIn = new HashMap<>();
   public int maxActiveWorlds = 0;
   public int openCooldownSecs = 0;
   public long lastOpenedAt = -1L;
   public final Map<UUID, Set<String>> seenBiomes = new HashMap<>();
   public final Map<UUID, Set<String>> seenDims = new HashMap<>();
   public final Map<UUID, String> lastShape = new HashMap<>();
   public final Map<UUID, List<String>> lastBiomes = new HashMap<>();
   private static DarkFountainData cached;
   private static MinecraftServer cachedFor;

   public boolean discoverBiome(UUID uid, String id) {
      boolean added = this.seenBiomes.computeIfAbsent(uid, k -> new HashSet<>()).add(id);
      if (added) {
         this.markDirty();
      }

      return added;
   }

   public boolean discoverDim(UUID uid, String id) {
      boolean added = this.seenDims.computeIfAbsent(uid, k -> new HashSet<>()).add(id);
      if (added) {
         this.markDirty();
      }

      return added;
   }

   public boolean hasBiome(UUID uid, String id) {
      if (DarkWorldCatalog.isDefaultBiome(id)) {
         return true;
      } else {
         Set s = this.seenBiomes.get(uid);
         return s != null && s.contains(id);
      }
   }

   public boolean hasShape(UUID uid, String id) {
      if (DarkWorldCatalog.isDefaultShape(id)) {
         return true;
      } else {
         Set s = this.seenDims.get(uid);
         return s != null && s.contains(id);
      }
   }

   public DarkFountainData.DarkWorldInstance createInstance() {
      int id = this.nextInstanceId++;
      DarkFountainData.DarkWorldInstance inst = new DarkFountainData.DarkWorldInstance(id);
      this.instances.put(id, inst);
      this.markDirty();
      return inst;
   }

   public void removeInstance(int id) {
      this.instances.remove(id);
      this.markDirty();
   }

   public DarkFountainData.DarkWorldInstance getInstanceByDarkBlock(BlockPos pos, String dimId) {
      long packed = pos.asLong();

      for (DarkFountainData.DarkWorldInstance inst : this.instances.values()) {
         if (dimId.equals(inst.roomDimension) && inst.darkBlockPositions.contains(packed)) {
            return inst;
         }
      }

      return null;
   }

   public DarkFountainData.DarkWorldInstance getInstanceByWorldKey(RegistryKey<World> key) {
      for (DarkFountainData.DarkWorldInstance inst : this.instances.values()) {
         if (inst.getWorldKey().equals(key)) {
            return inst;
         }
      }

      return null;
   }

   public DarkFountainData.DarkWorldInstance getInstanceForPlayer(UUID uid) {
      for (DarkFountainData.DarkWorldInstance inst : this.instances.values()) {
         if (inst.playersInside.contains(uid)) {
            return inst;
         }
      }

      return null;
   }

   public int getOrRollPlayerTint(UUID uid) {
      Integer existing = this.playerTints.get(uid);
      if (existing != null) {
         return existing;
      } else {
         int tint = rollTint(new Random((long)uid.hashCode() ^ System.nanoTime()));
         this.playerTints.put(uid, tint);
         this.markDirty();
         return tint;
      }
   }

   public int rerollPlayerTint(UUID uid) {
      int tint = rollTint(new Random((long)uid.hashCode() ^ System.nanoTime()));
      this.playerTints.put(uid, tint);
      this.markDirty();
      return tint;
   }

   public static int rollTint(Random rng) {
      float hue = rng.nextFloat();
      float sat = 0.55F + rng.nextFloat() * 0.35F;
      float val = 0.45F + rng.nextFloat() * 0.25F;
      return Color.HSBtoRGB(hue, sat, val) & 16777215;
   }

   public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
      nbt.putInt("nextInstanceId", this.nextInstanceId);
      NbtList list = new NbtList();

      for (DarkFountainData.DarkWorldInstance inst : this.instances.values()) {
         NbtCompound tag = new NbtCompound();
         tag.putInt("id", inst.id);
         tag.putLong("creationToken", inst.creationToken);
         tag.putLong("seed", inst.seed);
         if (inst.roomCenter != null) {
            tag.putLong("roomCenter", inst.roomCenter.asLong());
         }

         if (inst.roomDimension != null) {
            tag.putString("roomDim", inst.roomDimension);
         }

         if (inst.creatorReturnPos != null) {
            tag.putLong("returnPos", inst.creatorReturnPos.asLong());
         }

         tag.putString("baseBlockId", inst.baseBlockId);
         if (!inst.shapeId.isEmpty()) {
            tag.putString("shape", inst.shapeId);
         }

         tag.putInt("fountainX", inst.fountainX);
         tag.putInt("fountainZ", inst.fountainZ);
         if (!inst.biomeGroups.isEmpty()) {
            tag.put("biomeGroups", strings(inst.biomeGroups));
         }

         tag.putInt("tintColor", inst.tintColor);
         if (inst.creatorUuid != null) {
            tag.putUuid("creator", inst.creatorUuid);
         }

         tag.putInt("mobLevel", inst.mobLevel);
         if (inst.sealPos != null) {
            tag.putLong("sealPos", inst.sealPos.asLong());
         }

         if (inst.darknerItems != null) {
            tag.put("darknerItems", strings(inst.darknerItems));
         }

         tag.putInt("bossState", inst.bossState);
         if (inst.bossUuid != null) {
            tag.putUuid("bossUuid", inst.bossUuid);
         }

         tag.putString("bossItem", inst.bossItem);
         tag.putInt("bossSeed", inst.bossSeed);
         tag.putInt("bossTier", inst.bossTier);
         tag.putInt("creatorNameLen", inst.creatorNameLen);
         tag.putString("creatorWeapon", inst.creatorWeapon);
         tag.putBoolean("creatorWeaponMagic", inst.creatorWeaponMagic);
         tag.put("creatorArmor", strings(inst.creatorArmor));
         NbtList pList = new NbtList();

         for (UUID u : inst.playersInside) {
            NbtCompound pc = new NbtCompound();
            pc.putUuid("u", u);
            pList.add(pc);
         }

         tag.put("players", pList);
         NbtList bList = new NbtList();

         for (long l : inst.darkBlockPositions) {
            bList.add(NbtLong.of(l));
         }

         tag.put("blocks", bList);
         list.add(tag);
      }

      nbt.put("instances", list);
      NbtList tintList = new NbtList();

      for (Entry e : this.playerTints.entrySet()) {
         NbtCompound tc = new NbtCompound();
         tc.putUuid("u", e.getKey());
         tc.putInt("tint", e.getValue());
         tintList.add(tc);
      }

      nbt.put("playerTints", tintList);
      NbtList exileList = new NbtList();

      for (Entry e : this.pendingExile.entrySet()) {
         NbtCompound ec = new NbtCompound();
         ec.putUuid("u", e.getKey());
         ec.putLong("pos", e.getValue().asLong());
         exileList.add(ec);
      }

      nbt.put("pendingExile", exileList);
      NbtList exileDims = new NbtList();

      for (Entry e : this.pendingExileDim.entrySet()) {
         NbtCompound ec = new NbtCompound();
         ec.putUuid("u", e.getKey());
         ec.putString("dim", e.getValue());
         exileDims.add(ec);
      }

      nbt.put("pendingExileDim", exileDims);
      NbtList away = new NbtList();

      for (Entry e : this.offlineIn.entrySet()) {
         NbtCompound ec = new NbtCompound();
         ec.putUuid("u", e.getKey());
         ec.putInt("id", e.getValue());
         away.add(ec);
      }

      nbt.put("offlineIn", away);
      nbt.putInt("maxActiveWorlds", this.maxActiveWorlds);
      nbt.putInt("openCooldownSecs", this.openCooldownSecs);
      nbt.putLong("lastOpenedAt", this.lastOpenedAt);
      NbtList creativity = new NbtList();
      Set who = new HashSet<>();
      who.addAll(this.seenBiomes.keySet());
      who.addAll(this.seenDims.keySet());
      who.addAll(this.lastShape.keySet());
      who.addAll(this.lastBiomes.keySet());

      for (UUID u : who) {
         NbtCompound c = new NbtCompound();
         c.putUuid("u", u);
         c.put("biomes", strings(this.seenBiomes.getOrDefault(u, Collections.emptySet())));
         c.put("dims", strings(this.seenDims.getOrDefault(u, Collections.emptySet())));
         if (this.lastShape.containsKey(u)) {
            c.putString("lastShape", this.lastShape.get(u));
         }

         if (this.lastBiomes.containsKey(u)) {
            c.put("lastBiomes", strings(this.lastBiomes.get(u)));
         }

         creativity.add(c);
      }

      nbt.put("creativity", creativity);
      return nbt;
   }

   public static DarkFountainData readNbt(NbtCompound nbt) {
      DarkFountainData d = new DarkFountainData();
      d.nextInstanceId = Math.max(1, nbt.getInt("nextInstanceId"));
      NbtList list = nbt.getList("instances", 10);

      for (int i = 0; i < list.size(); i++) {
         NbtCompound tag = list.getCompound(i);
         int id = tag.getInt("id");
         DarkFountainData.DarkWorldInstance inst = new DarkFountainData.DarkWorldInstance(id);
         if (tag.contains("creationToken")) {
            inst.creationToken = tag.getLong("creationToken");
         }

         inst.seed = tag.contains("seed") ? tag.getLong("seed") : (long)id << 32 ^ inst.creationToken;
         if (tag.contains("roomCenter")) {
            inst.roomCenter = BlockPos.fromLong(tag.getLong("roomCenter"));
         }

         if (tag.contains("roomDim")) {
            inst.roomDimension = tag.getString("roomDim");
         }

         if (tag.contains("returnPos")) {
            inst.creatorReturnPos = BlockPos.fromLong(tag.getLong("returnPos"));
         }

         inst.baseBlockId = tag.contains("baseBlockId") ? tag.getString("baseBlockId") : "minecraft:grass_block";
         inst.shapeId = tag.getString("shape");
         inst.fountainX = tag.getInt("fountainX");
         inst.fountainZ = tag.getInt("fountainZ");
         inst.biomeGroups.addAll(readStrings(tag.getList("biomeGroups", 8)));
         inst.tintColor = tag.getInt("tintColor");
         if (tag.contains("creator")) {
            inst.creatorUuid = tag.getUuid("creator");
         }

         inst.mobLevel = Math.max(1, tag.getInt("mobLevel"));
         if (tag.contains("sealPos")) {
            inst.sealPos = BlockPos.fromLong(tag.getLong("sealPos"));
         }

         if (tag.contains("darknerItems")) {
            inst.darknerItems = new ArrayList<>(readStrings(tag.getList("darknerItems", 8)));
         }

         inst.bossState = tag.getInt("bossState");
         if (tag.containsUuid("bossUuid")) {
            inst.bossUuid = tag.getUuid("bossUuid");
         }

         inst.bossItem = tag.getString("bossItem");
         inst.bossSeed = tag.getInt("bossSeed");
         inst.bossTier = tag.contains("bossTier") ? tag.getInt("bossTier") : 1;
         inst.creatorNameLen = tag.contains("creatorNameLen") ? tag.getInt("creatorNameLen") : -1;
         inst.creatorWeapon = tag.getString("creatorWeapon");
         inst.creatorWeaponMagic = tag.getBoolean("creatorWeaponMagic");
         inst.creatorArmor.addAll(readStrings(tag.getList("creatorArmor", 8)));
         NbtList pList = tag.getList("players", 10);

         for (int j = 0; j < pList.size(); j++) {
            inst.playersInside.add(pList.getCompound(j).getUuid("u"));
         }

         NbtList bList = tag.getList("blocks", 4);

         for (int j = 0; j < bList.size(); j++) {
            inst.darkBlockPositions.add(((NbtLong)bList.get(j)).longValue());
         }

         d.instances.put(id, inst);
      }

      NbtList tintList = nbt.getList("playerTints", 10);

      for (int i = 0; i < tintList.size(); i++) {
         NbtCompound tc = tintList.getCompound(i);
         d.playerTints.put(tc.getUuid("u"), tc.getInt("tint"));
      }

      NbtList exileList = nbt.getList("pendingExile", 10);

      for (int i = 0; i < exileList.size(); i++) {
         NbtCompound ec = exileList.getCompound(i);
         d.pendingExile.put(ec.getUuid("u"), BlockPos.fromLong(ec.getLong("pos")));
      }

      NbtList exileDims = nbt.getList("pendingExileDim", 10);

      for (int i = 0; i < exileDims.size(); i++) {
         NbtCompound ec = exileDims.getCompound(i);
         d.pendingExileDim.put(ec.getUuid("u"), ec.getString("dim"));
      }

      NbtList away = nbt.getList("offlineIn", 10);

      for (int i = 0; i < away.size(); i++) {
         NbtCompound ec = away.getCompound(i);
         d.offlineIn.put(ec.getUuid("u"), ec.getInt("id"));
      }

      d.maxActiveWorlds = Math.max(0, nbt.getInt("maxActiveWorlds"));
      d.openCooldownSecs = Math.max(0, nbt.getInt("openCooldownSecs"));
      d.lastOpenedAt = nbt.contains("lastOpenedAt") ? nbt.getLong("lastOpenedAt") : -1L;
      NbtList creativity = nbt.getList("creativity", 10);

      for (int i = 0; i < creativity.size(); i++) {
         NbtCompound c = creativity.getCompound(i);
         UUID u = c.getUuid("u");
         d.seenBiomes.put(u, new HashSet<>(readStrings(c.getList("biomes", 8))));
         d.seenDims.put(u, new HashSet<>(readStrings(c.getList("dims", 8))));
         if (c.contains("lastShape")) {
            d.lastShape.put(u, c.getString("lastShape"));
         }

         if (c.contains("lastBiomes")) {
            d.lastBiomes.put(u, readStrings(c.getList("lastBiomes", 8)));
         }
      }

      return d;
   }

   private static NbtList strings(Collection<String> values) {
      NbtList l = new NbtList();

      for (String v : values) {
         l.add(NbtString.of(v));
      }

      return l;
   }

   private static List<String> readStrings(NbtList l) {
      List out = new ArrayList<>();

      for (int i = 0; i < l.size(); i++) {
         out.add(l.getString(i));
      }

      return out;
   }

   public static DarkFountainData get(MinecraftServer server) {
      DarkFountainData c = cached;
      if (c != null && cachedFor == server) {
         return c;
      } else {
         PersistentStateManager mgr = server.getWorld(World.OVERWORLD).getPersistentStateManager();
         c = (DarkFountainData)mgr.getOrCreate(new Type(DarkFountainData::new, (nbt, lookup) -> readNbt(nbt), null), "dark_fountain_data");
         cached = c;
         cachedFor = server;
         return c;
      }
   }

   public static class DarkWorldInstance {
      public final int id;
      public long creationToken;
      public BlockPos roomCenter;
      public String roomDimension;
      public BlockPos creatorReturnPos;
      public final Set<Long> darkBlockPositions = new HashSet<>();
      public String baseBlockId = "minecraft:grass_block";
      public String shapeId = "";
      public final List<String> biomeGroups = new ArrayList<>();
      public int fountainX = 0;
      public int fountainZ = 0;
      public int tintColor = 2767402;
      public UUID creatorUuid;
      public BlockPos sealPos = null;
      public int mobLevel = 1;
      public List<String> darknerItems = null;
      public int bossState = 0;
      public UUID bossUuid = null;
      public String bossItem = "";
      public int bossSeed = 0;
      public int bossTier = 1;
      public int creatorNameLen = -1;
      public String creatorWeapon = "";
      public boolean creatorWeaponMagic = false;
      public final List<String> creatorArmor = new ArrayList<>();
      public long seed;
      public final Set<UUID> playersInside = new HashSet<>();

      public DarkWorldInstance(int id) {
         this.id = id;
         this.creationToken = (System.nanoTime() ^ (long)(Math.random() * 9.223372E18F)) & 4294967295L;
         this.seed = new Random(System.nanoTime() ^ (long)id << 32 ^ (long)(Math.random() * 9.223372E18F)).nextLong();
      }

      public RegistryKey<World> getWorldKey() {
         return RegistryKey.of(RegistryKeys.WORLD, Identifier.of("turnbattle", "dark_world_" + this.id + "_" + Long.toHexString(this.creationToken)));
      }

      public BlockPos getInitialSpawn() {
         return new BlockPos(50, 64, 0);
      }

      public RegistryKey<World> getRoomDimKey() {
         return RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(this.roomDimension));
      }
   }
}
