package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;

public class RogueliteData extends PersistentState {
   private static final String KEY = "turnbattle_roguelite";
   public boolean rogueliteEnabled = false;
   public boolean worldTypeChosen = false;
   public final Map<UUID, RogueliteData.PlayerRogueData> players = new HashMap<>();
   public final Map<UUID, UUID> pendingInvites = new HashMap<>();
   public final Map<UUID, Set<UUID>> activeParties = new HashMap<>();
   private static RogueliteData cached;
   private static MinecraftServer cachedFor;

   public RogueliteData.PlayerRogueData getOrCreate(UUID uuid) {
      return this.players.computeIfAbsent(uuid, k -> new RogueliteData.PlayerRogueData());
   }

   public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
      nbt.putBoolean("rogueliteEnabled", this.rogueliteEnabled);
      nbt.putBoolean("worldTypeChosen", this.worldTypeChosen);
      NbtList list = new NbtList();

      for (Entry e : this.players.entrySet()) {
         NbtCompound tag = new NbtCompound();
         tag.putUuid("uuid", e.getKey());
         RogueliteData.PlayerRogueData d = e.getValue();
         tag.putInt("soulShards", d.soulShards);
         NbtCompound up = new NbtCompound();
         d.upgrades.forEach(up::putInt);
         tag.put("upgrades", up);
         NbtList souls = new NbtList();

         for (SoulType s : d.ownedSouls) {
            souls.add(NbtString.of(s.name()));
         }

         tag.put("ownedSouls", souls);
         if (d.chosenSoul != null) {
            tag.putString("chosenSoul", d.chosenSoul.name());
         }

         if (d.partyId != null) {
            tag.putUuid("partyId", d.partyId);
         }

         tag.putInt("totalRuns", d.totalRuns);
         tag.putInt("bestLevel", d.bestLevel);
         tag.putLong("bestTimeTicks", d.bestTimeTicks);
         tag.putInt("totalShardsEarned", d.totalShardsEarned);
         tag.putInt("lastRunEndLevel", d.lastRunLevel);
         tag.putLong("lastRunTicks", d.lastRunTicks);
         tag.putInt("lastRunBattles", d.lastRunBattles);
         tag.putInt("lastRunShards", d.lastRunShards);
         tag.putBoolean("inRun", d.inRun);
         tag.putBoolean("deadThisRun", d.deadThisRun);
         tag.putLong("runStartTime", d.runStartTime);
         tag.putInt("runBattlesWon", d.runBattlesWon);
         tag.putLong("runActiveTicks", d.runActiveTicks);
         tag.putInt("runBonusShards", d.runBonusShards);
         tag.putInt("rerollsLeft", d.rerollsLeft);
         tag.putBoolean("pendingMenuAfterRespawn", d.pendingMenuAfterRespawn);
         tag.putIntArray("cardStatBonuses", d.cardStatBonuses);
         tag.put("uniqueCards", strings(d.uniqueCards));
         tag.put("drawnSkillsV2", strings(d.drawnSkills));
         tag.put("pendingHand", strings(d.pendingHand));
         list.add(tag);
      }

      nbt.put("players", list);
      return nbt;
   }

   private static NbtList strings(Collection<String> in) {
      NbtList l = new NbtList();

      for (String s : in) {
         l.add(NbtString.of(s));
      }

      return l;
   }

   private static void readStrings(NbtCompound tag, String key, Collection<String> out) {
      NbtList l = tag.getList(key, 8);

      for (int i = 0; i < l.size(); i++) {
         out.add(l.getString(i));
      }
   }

   public static RogueliteData readNbt(NbtCompound nbt) {
      RogueliteData data = new RogueliteData();
      data.rogueliteEnabled = nbt.getBoolean("rogueliteEnabled");
      if (nbt.contains("worldTypeChosen")) {
         data.worldTypeChosen = nbt.getBoolean("worldTypeChosen");
      }

      NbtList list = nbt.getList("players", 10);

      for (int i = 0; i < list.size(); i++) {
         NbtCompound tag = list.getCompound(i);
         UUID uuid = tag.getUuid("uuid");
         RogueliteData.PlayerRogueData d = new RogueliteData.PlayerRogueData();
         d.soulShards = tag.getInt("soulShards");
         NbtCompound up = tag.getCompound("upgrades");

         for (String k : up.getKeys()) {
            if (RogueUpgrades.byId(k) != null) {
               d.upgrades.put(k, up.getInt(k));
            }
         }

         if (tag.contains("expMultiplierLevel") && !d.upgrades.containsKey("exp")) {
            d.upgrades.put("exp", Math.min(5, tag.getInt("expMultiplierLevel")));
         }

         NbtList souls = tag.getList("ownedSouls", 8);

         for (int j = 0; j < souls.size(); j++) {
            try {
               d.ownedSouls.add(SoulType.valueOf(souls.getString(j)));
            } catch (IllegalArgumentException var12) {
            }
         }

         if (tag.contains("chosenSoul")) {
            try {
               d.chosenSoul = SoulType.valueOf(tag.getString("chosenSoul"));
               if (RogueUpgrades.soulCost(d.chosenSoul) < 0) {
                  d.chosenSoul = null;
               } else {
                  d.ownedSouls.add(d.chosenSoul);
               }
            } catch (IllegalArgumentException var11) {
            }
         }

         if (tag.containsUuid("partyId")) {
            d.partyId = tag.getUuid("partyId");
         }

         d.totalRuns = tag.getInt("totalRuns");
         d.bestLevel = tag.getInt("bestLevel");
         d.bestTimeTicks = tag.getLong("bestTimeTicks");
         d.totalShardsEarned = tag.getInt("totalShardsEarned");
         d.lastRunLevel = tag.contains("lastRunTicks") ? tag.getInt("lastRunEndLevel") : -1;
         d.lastRunTicks = tag.getLong("lastRunTicks");
         d.lastRunBattles = tag.getInt("lastRunBattles");
         d.lastRunShards = tag.getInt("lastRunShards");
         d.inRun = tag.getBoolean("inRun");
         d.deadThisRun = tag.getBoolean("deadThisRun");
         d.runStartTime = tag.getLong("runStartTime");
         d.runBattlesWon = tag.getInt("runBattlesWon");
         d.runActiveTicks = tag.getLong("runActiveTicks");
         d.runBonusShards = tag.getInt("runBonusShards");
         d.rerollsLeft = tag.getInt("rerollsLeft");
         d.pendingMenuAfterRespawn = tag.getBoolean("pendingMenuAfterRespawn");
         if (tag.contains("cardStatBonuses")) {
            int[] arr = tag.getIntArray("cardStatBonuses");

            for (int j = 0; j < Math.min(arr.length, 6); j++) {
               d.cardStatBonuses[j] = arr[j];
            }
         }

         readStrings(tag, "uniqueCards", d.uniqueCards);
         if (tag.getList("uniqueCards", 8).isEmpty()) {
            NbtList old = tag.getList("uniqueCards", 10);

            for (int j = 0; j < old.size(); j++) {
               d.uniqueCards.add(old.getCompound(j).getString("u"));
            }
         }

         readStrings(tag, "drawnSkillsV2", d.drawnSkills);
         readStrings(tag, "pendingHand", d.pendingHand);
         data.players.put(uuid, d);
      }

      for (Entry e : data.players.entrySet()) {
         UUID pid = e.getValue().partyId;
         if (pid != null) {
            data.activeParties.computeIfAbsent(pid, kx -> new HashSet<>()).add(e.getKey());
         }
      }

      Iterator it = data.activeParties.entrySet().iterator();

      while (it.hasNext()) {
         Entry ex = it.next();
         if (ex.getValue().size() < 2) {
            for (UUID m : ex.getValue()) {
               data.players.get(m).partyId = null;
            }

            it.remove();
         }
      }

      return data;
   }

   public static RogueliteData get(MinecraftServer server) {
      RogueliteData c = cached;
      if (c != null && cachedFor == server) {
         return c;
      } else {
         PersistentStateManager mgr = server.getWorld(World.OVERWORLD).getPersistentStateManager();
         c = (RogueliteData)mgr.getOrCreate(new Type(RogueliteData::new, (nbt, lookup) -> readNbt(nbt), null), "turnbattle_roguelite");
         cached = c;
         cachedFor = server;
         return c;
      }
   }

   public static class PlayerRogueData {
      public int soulShards = 0;
      public final Map<String, Integer> upgrades = new HashMap<>();
      public final Set<SoulType> ownedSouls = EnumSet.noneOf(SoulType.class);
      public SoulType chosenSoul = null;
      public UUID partyId = null;
      public int totalRuns = 0;
      public int bestLevel = 0;
      public long bestTimeTicks = 0L;
      public int totalShardsEarned = 0;
      public int lastRunLevel = -1;
      public long lastRunTicks = 0L;
      public int lastRunBattles = 0;
      public int lastRunShards = 0;
      public boolean inRun = false;
      public boolean deadThisRun = false;
      public long runStartTime = 0L;
      public long runActiveTicks = 0L;
      public int runBattlesWon = 0;
      public int runBonusShards = 0;
      public int rerollsLeft = 0;
      public final List<String> drawnSkills = new ArrayList<>();
      public int[] cardStatBonuses = new int[6];
      public final List<String> uniqueCards = new ArrayList<>();
      public final List<String> pendingHand = new ArrayList<>();
      public boolean pendingMenuAfterRespawn = false;

      public int upgrade(String id) {
         return this.upgrades.getOrDefault(id, 0);
      }
   }
}
