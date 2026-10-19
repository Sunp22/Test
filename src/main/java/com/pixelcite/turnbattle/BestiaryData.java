package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;

public class BestiaryData extends PersistentState {
   private static final String KEY = "turnbattle_bestiary";
   private final Map<UUID, Set<String>> playerBestiary = new HashMap<>();

   public static BestiaryData get(MinecraftServer server) {
      PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
      return (BestiaryData)manager.getOrCreate(new Type(BestiaryData::new, (besn, besl) -> fromNbt(besn), null), "turnbattle_bestiary");
   }

   public void recordDefeat(UUID playerUuid, EntityType<?> entityType) {
      String typeId = EntityType.getId(entityType).toString();
      this.playerBestiary.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(typeId);
      this.markDirty();
   }

   public boolean hasDefeated(UUID playerUuid, EntityType<?> entityType) {
      String typeId = EntityType.getId(entityType).toString();
      Set defeated = this.playerBestiary.get(playerUuid);
      return defeated != null && defeated.contains(typeId);
   }

   public Set<String> getDefeatedTypes(UUID playerUuid) {
      return new HashSet<>(this.playerBestiary.getOrDefault(playerUuid, new HashSet<>()));
   }

   public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
      NbtCompound playersTag = new NbtCompound();
      this.playerBestiary.forEach((uuid, types) -> {
         NbtList list = new NbtList();
         types.forEach(t -> list.add(NbtString.of(t)));
         playersTag.put(uuid.toString(), list);
      });
      nbt.put("players", playersTag);
      return nbt;
   }

   public static BestiaryData fromNbt(NbtCompound nbt) {
      BestiaryData data = new BestiaryData();
      if (nbt.contains("players")) {
         NbtCompound playersTag = nbt.getCompound("players");

         for (String uuidStr : playersTag.getKeys()) {
            try {
               UUID uuid = UUID.fromString(uuidStr);
               NbtList list = playersTag.getList(uuidStr, 8);
               Set types = new HashSet<>();

               for (int i = 0; i < list.size(); i++) {
                  types.add(list.getString(i));
               }

               data.playerBestiary.put(uuid, types);
            } catch (IllegalArgumentException var9) {
            }
         }
      }

      return data;
   }
}
