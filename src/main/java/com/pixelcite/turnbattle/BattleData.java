package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;

public class BattleData extends PersistentState {
   private static final String KEY = "turnbattle_data";
   final Map<UUID, BattleData.PlayerProgress> players = new HashMap<>();
   public static final int MAX_LEVEL = 99;
   private static BattleData cached;
   private static MinecraftServer cachedFor;

   public Map<UUID, BattleData.PlayerProgress> players() {
      return this.players;
   }

   public BattleData.PlayerProgress getOrCreate(UUID uuid) {
      return this.players.computeIfAbsent(uuid, k -> new BattleData.PlayerProgress());
   }

   public boolean needsIntro(UUID uuid) {
      BattleData.PlayerProgress p = this.players.get(uuid);
      return p != null && p.playerClass != null ? !p.introSeen || p.soulType == null || p.vesselName == null || p.vesselName.isEmpty() : false;
   }

   public boolean needsWorldTypePicker(UUID uuid) {
      BattleData.PlayerProgress p = this.players.get(uuid);
      return p != null && p.introSeen && !p.worldTypeSeen;
   }

   public boolean needsClass(UUID uuid) {
      BattleData.PlayerProgress p = this.players.get(uuid);
      return p == null || p.playerClass == null;
   }

   public static int expNeeded(int level) {
      return 30 + (level - 1) * 40;
   }

   public int addExp(UUID uuid, int amount) {
      BattleData.PlayerProgress prog = this.getOrCreate(uuid);
      prog.exp += amount;

      int gained;
      for (gained = 0; prog.level < 99 && prog.exp >= expNeeded(prog.level); gained++) {
         prog.exp = prog.exp - expNeeded(prog.level);
         prog.level++;
         if (prog.rogueRun) {
            prog.rogueHandsOwed++;
         } else {
            prog.playerStats.unspent += 3;
            prog.skillPoints += 3;
         }

         if (prog.partyMember != PartyMember.NONE) {
            int mi = prog.partyMember.ordinal();
            if (prog.partyLevels[mi] < 99) {
               prog.partyLevels[mi]++;
               prog.partyStats[mi].unspent += 3;
               prog.partySkillPoints[mi] = prog.partySkillPoints[mi] + 3;
            }
         }
      }

      if (gained > 0) {
         this.markDirty();
      }

      return gained;
   }

   public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
      NbtList list = new NbtList();
      this.players.forEach((uuid, prog) -> {
         NbtCompound tag = new NbtCompound();
         tag.putUuid("uuid", uuid);
         tag.putInt("level", prog.level);
         tag.putInt("savedLevel", prog.savedLevel);
         tag.putLong("savedPlaytimeSec", prog.savedPlaytimeSec);
         if (prog.lastBiome != null) {
            tag.putString("lastBiome", prog.lastBiome);
         }

         tag.putInt("exp", prog.exp);
         if (prog.playerClass != null) {
            tag.putString("class", prog.playerClass.name());
         }

         if (prog.playerRace != null) {
            tag.putString("race", prog.playerRace.name());
         }

         tag.putBoolean("soulRemoved", prog.soulRemoved);
         if (prog.soulOutDim != null) {
            tag.putString("soulOutDim", prog.soulOutDim);
            tag.putDouble("soulOutX", prog.soulOutX);
            tag.putDouble("soulOutY", prog.soulOutY);
            tag.putDouble("soulOutZ", prog.soulOutZ);
            tag.putFloat("soulOutYaw", prog.soulOutYaw);
            tag.putFloat("soulOutPitch", prog.soulOutPitch);
         }

         NbtList favList = new NbtList();

         for (String f : prog.favoriteFood) {
            favList.add(NbtString.of(f));
         }

         tag.put("favoriteFood", favList);
         NbtList absorbArr = new NbtList();

         for (String s : prog.absorbedSouls) {
            absorbArr.add(NbtString.of(s));
         }

         tag.put("absorbedSouls", absorbArr);
         if (prog.monsterWeapon != null) {
            tag.putString("monsterWeapon", prog.monsterWeapon.name());
         }

         tag.putString("party", prog.partyMember.name());
         if (prog.vesselName != null) {
            tag.putString("vesselName", prog.vesselName);
         }

         tag.putInt("vesselChoice", prog.vesselChoice);
         tag.putBoolean("introSeen", prog.introSeen);
         tag.putBoolean("worldTypeSeen", prog.worldTypeSeen);
         tag.putFloat("partyMemberHp", prog.partyMemberHp);
         if (prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty()) {
            new NbtCompound();
            NbtCompound var26 = (NbtCompound)prog.equippedWeapon.encode(lookup);
            tag.put("equippedWeapon", var26);
         }

         if (prog.partyEquippedWeapon != null && !prog.partyEquippedWeapon.isEmpty()) {
            new NbtCompound();
            NbtCompound var27 = (NbtCompound)prog.partyEquippedWeapon.encode(lookup);
            tag.put("partyEquippedWeapon", var27);
         }

         if (prog.equippedAccessory != null && !prog.equippedAccessory.isEmpty()) {
            new NbtCompound();
            NbtCompound var28 = (NbtCompound)prog.equippedAccessory.encode(lookup);
            tag.put("equippedAccessory", var28);
         }

         tag.putInt("battleStyle", prog.battleStyle != null ? prog.battleStyle.ordinal() : 0);
         tag.putInt("battleView", prog.battleView != null ? prog.battleView.ordinal() : 0);
         if (prog.magicant) {
            tag.putBoolean("magicant", true);
         }

         if (prog.rogueRun) {
            tag.putBoolean("rogueRun", true);
         }

         if (prog.rogueHandsOwed > 0) {
            tag.putInt("rogueHandsOwed", prog.rogueHandsOwed);
         }

         if (prog.darkDollars > 0) {
            tag.putInt("darkDollars", prog.darkDollars);
         }

         if (prog.magicantSeen) {
            tag.putBoolean("magicantSeen", true);
         }

         if (prog.magicantReturnDim != null) {
            NbtCompound mr = new NbtCompound();
            mr.putString("dim", prog.magicantReturnDim);
            mr.putDouble("x", prog.magicantRetX);
            mr.putDouble("y", prog.magicantRetY);
            mr.putDouble("z", prog.magicantRetZ);
            mr.putFloat("yaw", prog.magicantRetYaw);
            tag.put("magicantReturn", mr);
         }

         for (int ai = 0; ai < 2; ai++) {
            ItemStack ap = prog.equippedArmor[ai];
            if (ap != null && !ap.isEmpty()) {
               new NbtCompound();
               NbtCompound var9 = (NbtCompound)ap.encode(lookup);
               tag.put("equippedArmor" + ai, var9);
            }
         }

         tag.put("playerStats", prog.playerStats.toNbt());
         if (prog.soulType != null) {
            tag.putString("soulType", prog.soulType.name());
         }

         tag.putBoolean("determinationUsed", prog.determinationUsed);
         tag.putBoolean("freeRollUsed", prog.freeRollUsed);
         tag.putInt("darkFountainSeals", prog.darkFountainSeals);
         if (prog.activeQuestlineId != null) {
            tag.putString("activeQuestlineId", prog.activeQuestlineId);
         }

         tag.putInt("questlineNodeIndex", prog.questlineNodeIndex);
         tag.putBoolean("questlineNodeDone", prog.questlineNodeDone);
         NbtList qlList = new NbtList();

         for (String q : prog.completedQuestlines) {
            qlList.add(NbtString.of(q));
         }

         tag.put("completedQuestlines", qlList);
         tag.putInt("storyUnlockedChapter", prog.storyUnlockedChapter);
         tag.putInt("storyQuestIndex", prog.storyQuestIndex);
         tag.putBoolean("storyQuestStarted", prog.storyQuestStarted);
         tag.putBoolean("storyQuestComplete", prog.storyQuestComplete);
         int[] chDone = new int[5];

         for (int ci = 0; ci < 5; ci++) {
            chDone[ci] = prog.storyChapterDone[ci] ? 1 : 0;
         }

         tag.putIntArray("storyChapterDone", chDone);
         if (prog.activeQuestlineId != null) {
            tag.putString("activeQuestlineId", prog.activeQuestlineId);
         }

         tag.putInt("questNodeIndex", prog.questNodeIndex);
         tag.putBoolean("questNodeComplete", prog.questNodeComplete);
         tag.putInt("routeKills", prog.routeKills);
         tag.putBoolean("routeSpared", prog.routeSpared);
         tag.putBoolean("shownKillWarning", prog.shownKillWarning);
         tag.putBoolean("ruthlessCancelled", prog.ruthlessCancelled);
         tag.putBoolean("isRuthless", prog.isRuthless);
         if (prog.activeQuestlineId != null) {
            tag.putString("activeQuestlineId", prog.activeQuestlineId);
         }

         tag.putInt("questNodeIndex", prog.questNodeIndex);
         tag.putBoolean("questNodeComplete", prog.questNodeComplete);
         NbtCompound partyStatsTag = new NbtCompound();
         NbtCompound partyLevelsTag = new NbtCompound();

         for (PartyMember m : PartyMember.values()) {
            int mi = m.ordinal();
            partyStatsTag.put(m.name(), prog.partyStats[mi].toNbt());
            partyLevelsTag.putInt(m.name(), prog.partyLevels[mi]);
         }

         tag.put("partyStats", partyStatsTag);
         tag.put("partyLevels", partyLevelsTag);
         tag.putInt("skillPoints", prog.skillPoints);
         NbtIntArray tsbArr = new NbtIntArray(prog.treeStatBonuses);
         tag.put("treeStatBonuses", tsbArr);
         NbtList skillList = new NbtList();

         for (String s : prog.unlockedSkills) {
            skillList.add(NbtString.of(s));
         }

         tag.put("unlockedSkills", skillList);
         NbtList uneqList = new NbtList();

         for (String s : prog.unequippedSkills) {
            uneqList.add(NbtString.of(s));
         }

         tag.put("unequippedSkills", uneqList);
         NbtCompound partySpTag = new NbtCompound();
         NbtCompound partySkillsTag = new NbtCompound();

         for (PartyMember m : PartyMember.values()) {
            int mi = m.ordinal();
            partySpTag.putInt(m.name(), prog.partySkillPoints[mi]);
            NbtList psl = new NbtList();

            for (String s : prog.partyUnlockedSkills[mi]) {
               psl.add(NbtString.of(s));
            }

            partySkillsTag.put(m.name(), psl);
         }

         tag.put("partySkillPoints", partySpTag);
         tag.put("partyUnlockedSkills", partySkillsTag);
         list.add(tag);
      });
      nbt.put("players", list);
      return nbt;
   }

   public static BattleData readNbt(NbtCompound nbt, WrapperLookup lookup) {
      BattleData data = new BattleData();
      NbtList list = nbt.getList("players", 10);

      for (int i = 0; i < list.size(); i++) {
         NbtCompound tag = list.getCompound(i);
         UUID uuid = tag.getUuid("uuid");
         BattleData.PlayerProgress prog = new BattleData.PlayerProgress();
         prog.level = tag.getInt("level");
         if (tag.contains("savedLevel")) {
            prog.savedLevel = tag.getInt("savedLevel");
         }

         if (tag.contains("savedPlaytimeSec")) {
            prog.savedPlaytimeSec = tag.getLong("savedPlaytimeSec");
         }

         if (tag.contains("lastBiome")) {
            prog.lastBiome = tag.getString("lastBiome");
         }

         prog.exp = tag.getInt("exp");
         if (tag.contains("class")) {
            try {
               prog.playerClass = PlayerClass.valueOf(tag.getString("class"));
            } catch (IllegalArgumentException var15) {
            }
         }

         if (tag.contains("race")) {
            try {
               prog.playerRace = CharacterRace.valueOf(tag.getString("race"));
            } catch (IllegalArgumentException var16) {
            }
         }

         if (prog.playerRace == null && prog.introSeen) {
            prog.playerRace = CharacterRace.HUMAN;
         }

         prog.soulRemoved = tag.getBoolean("soulRemoved");
         if (tag.contains("soulOutDim")) {
            prog.soulOutDim = tag.getString("soulOutDim");
            prog.soulOutX = tag.getDouble("soulOutX");
            prog.soulOutY = tag.getDouble("soulOutY");
            prog.soulOutZ = tag.getDouble("soulOutZ");
            prog.soulOutYaw = tag.getFloat("soulOutYaw");
            prog.soulOutPitch = tag.getFloat("soulOutPitch");
         }

         if (tag.contains("favoriteFood")) {
            NbtList favL = tag.getList("favoriteFood", 8);

            for (int fi = 0; fi < favL.size(); fi++) {
               prog.favoriteFood.add(favL.getString(fi));
            }
         }

         if (tag.contains("absorbedSouls")) {
            NbtList arr = tag.getList("absorbedSouls", 8);

            for (int ai = 0; ai < arr.size(); ai++) {
               prog.absorbedSouls.add(arr.getString(ai));
            }

            if (prog.playerStats != null) {
               prog.playerStats.absorbed = prog.absorbedSouls.size();
            }
         }

         if (tag.contains("monsterWeapon")) {
            try {
               prog.monsterWeapon = MonsterWeapon.valueOf(tag.getString("monsterWeapon"));
            } catch (IllegalArgumentException var19) {
            }
         }

         if (tag.contains("party")) {
            try {
               prog.partyMember = PartyMember.valueOf(tag.getString("party"));
            } catch (IllegalArgumentException var18) {
            }
         }

         if (tag.contains("vesselName")) {
            prog.vesselName = tag.getString("vesselName");
         }

         if (tag.contains("vesselChoice")) {
            prog.vesselChoice = tag.getInt("vesselChoice");
         }

         prog.introSeen = tag.getBoolean("introSeen");
         if (tag.contains("worldTypeSeen")) {
            prog.worldTypeSeen = tag.getBoolean("worldTypeSeen");
         }

         if (tag.contains("partyMemberHp")) {
            prog.partyMemberHp = tag.getFloat("partyMemberHp");
         }

         if (tag.contains("equippedWeapon")) {
            prog.equippedWeapon = ItemStack.fromNbt(lookup, tag.getCompound("equippedWeapon")).orElse(ItemStack.EMPTY);
         }

         if (tag.contains("partyEquippedWeapon")) {
            prog.partyEquippedWeapon = ItemStack.fromNbt(lookup, tag.getCompound("partyEquippedWeapon")).orElse(ItemStack.EMPTY);
         }

         if (tag.contains("equippedAccessory")) {
            prog.equippedAccessory = ItemStack.fromNbt(lookup, tag.getCompound("equippedAccessory")).orElse(ItemStack.EMPTY);
         }

         if (tag.contains("battleStyle")) {
            int bsOrd = tag.getInt("battleStyle");
            BattleStyle[] bsVals = BattleStyle.values();
            prog.battleStyle = bsOrd >= 0 && bsOrd < bsVals.length ? bsVals[bsOrd] : BattleStyle.BULLET_HELL;
         }

         if (tag.contains("battleView")) {
            prog.battleView = BattleView.fromOrdinal(tag.getInt("battleView"));
         }

         prog.magicant = tag.getBoolean("magicant");
         prog.rogueRun = tag.getBoolean("rogueRun");
         prog.rogueHandsOwed = tag.getInt("rogueHandsOwed");
         prog.darkDollars = tag.getInt("darkDollars");
         prog.magicantSeen = tag.getBoolean("magicantSeen");
         if (tag.contains("magicantReturn")) {
            NbtCompound mr = tag.getCompound("magicantReturn");
            prog.magicantReturnDim = mr.getString("dim");
            prog.magicantRetX = mr.getDouble("x");
            prog.magicantRetY = mr.getDouble("y");
            prog.magicantRetZ = mr.getDouble("z");
            prog.magicantRetYaw = mr.getFloat("yaw");
         }

         for (int ai = 0; ai < 2; ai++) {
            String key = "equippedArmor" + ai;
            if (tag.contains(key)) {
               prog.equippedArmor[ai] = ItemStack.fromNbt(lookup, tag.getCompound(key)).orElse(ItemStack.EMPTY);
            }
         }

         if (tag.contains("playerStats")) {
            prog.playerStats = CharStats.fromNbt(tag.getCompound("playerStats"));
         }

         if (prog.magicant && prog.playerStats != null) {
            prog.playerStats.pointValue = 2;
         }

         if (tag.contains("soulType")) {
            try {
               prog.soulType = SoulType.valueOf(tag.getString("soulType"));
            } catch (IllegalArgumentException var17) {
            }
         }

         if (tag.contains("determinationUsed")) {
            prog.determinationUsed = tag.getBoolean("determinationUsed");
         }

         if (tag.contains("freeRollUsed")) {
            prog.freeRollUsed = tag.getBoolean("freeRollUsed");
         }

         if (tag.contains("darkFountainSeals")) {
            prog.darkFountainSeals = tag.getInt("darkFountainSeals");
         }

         if (tag.contains("activeQuestlineId")) {
            String _aqid = tag.getString("activeQuestlineId");
            prog.activeQuestlineId = _aqid.isEmpty() ? null : _aqid;
         }

         if (tag.contains("questlineNodeIndex")) {
            prog.questlineNodeIndex = tag.getInt("questlineNodeIndex");
         }

         if (tag.contains("questlineNodeDone")) {
            prog.questlineNodeDone = tag.getBoolean("questlineNodeDone");
         }

         if (tag.contains("completedQuestlines")) {
            NbtList qlList = tag.getList("completedQuestlines", 8);

            for (int ci2 = 0; ci2 < qlList.size(); ci2++) {
               prog.completedQuestlines.add(qlList.getString(ci2));
            }
         }

         if (tag.contains("storyUnlockedChapter")) {
            prog.storyUnlockedChapter = tag.getInt("storyUnlockedChapter");
         }

         if (tag.contains("storyQuestIndex")) {
            prog.storyQuestIndex = tag.getInt("storyQuestIndex");
         }

         if (tag.contains("storyQuestStarted")) {
            prog.storyQuestStarted = tag.getBoolean("storyQuestStarted");
         }

         if (tag.contains("storyQuestComplete")) {
            prog.storyQuestComplete = tag.getBoolean("storyQuestComplete");
         }

         if (tag.contains("storyChapterDone")) {
            int[] arr = tag.getIntArray("storyChapterDone");

            for (int ci = 0; ci < 5 && ci < arr.length; ci++) {
               prog.storyChapterDone[ci] = arr[ci] != 0;
            }
         }

         if (tag.contains("activeQuestlineId")) {
            String _aqid = tag.getString("activeQuestlineId");
            prog.activeQuestlineId = _aqid.isEmpty() ? null : _aqid;
         }

         if (tag.contains("questNodeIndex")) {
            prog.questNodeIndex = tag.getInt("questNodeIndex");
         }

         if (tag.contains("questNodeComplete")) {
            prog.questNodeComplete = tag.getBoolean("questNodeComplete");
         }

         if (tag.contains("routeKills")) {
            prog.routeKills = tag.getInt("routeKills");
         }

         if (tag.contains("routeSpared")) {
            prog.routeSpared = tag.getBoolean("routeSpared");
         }

         if (tag.contains("shownKillWarning")) {
            prog.shownKillWarning = tag.getBoolean("shownKillWarning");
         }

         if (tag.contains("ruthlessCancelled")) {
            prog.ruthlessCancelled = tag.getBoolean("ruthlessCancelled");
         }

         if (tag.contains("isRuthless")) {
            prog.isRuthless = tag.getBoolean("isRuthless");
         }

         if (tag.contains("activeQuestlineId")) {
            String _aqid = tag.getString("activeQuestlineId");
            prog.activeQuestlineId = _aqid.isEmpty() ? null : _aqid;
         }

         if (tag.contains("questNodeIndex")) {
            prog.questNodeIndex = tag.getInt("questNodeIndex");
         }

         if (tag.contains("questNodeComplete")) {
            prog.questNodeComplete = tag.getBoolean("questNodeComplete");
         }

         if (tag.contains("partyStats")) {
            NbtCompound ps = tag.getCompound("partyStats");

            for (PartyMember m : PartyMember.values()) {
               if (ps.contains(m.name())) {
                  prog.partyStats[m.ordinal()] = CharStats.fromNbt(ps.getCompound(m.name()));
               }
            }
         }

         if (tag.contains("partyLevels")) {
            NbtCompound pl = tag.getCompound("partyLevels");

            for (PartyMember mx : PartyMember.values()) {
               if (pl.contains(mx.name())) {
                  prog.partyLevels[mx.ordinal()] = pl.getInt(mx.name());
               }
            }
         }

         if (tag.contains("skillPoints")) {
            prog.skillPoints = tag.getInt("skillPoints");
         }

         if (tag.contains("treeStatBonuses")) {
            int[] loaded = tag.getIntArray("treeStatBonuses");

            for (int j = 0; j < 6 && j < loaded.length; j++) {
               prog.treeStatBonuses[j] = loaded[j];
            }
         }

         if (tag.contains("unlockedSkills")) {
            NbtList sl = tag.getList("unlockedSkills", 8);

            for (int j = 0; j < sl.size(); j++) {
               prog.unlockedSkills.add(sl.getString(j));
            }
         }

         if (tag.contains("unequippedSkills")) {
            NbtList ul = tag.getList("unequippedSkills", 8);

            for (int j = 0; j < ul.size(); j++) {
               prog.unequippedSkills.add(ul.getString(j));
            }
         }

         if (tag.contains("partySkillPoints")) {
            NbtCompound psp = tag.getCompound("partySkillPoints");

            for (PartyMember mxx : PartyMember.values()) {
               if (psp.contains(mxx.name())) {
                  prog.partySkillPoints[mxx.ordinal()] = psp.getInt(mxx.name());
               }
            }
         }

         if (tag.contains("partyUnlockedSkills")) {
            NbtCompound pus = tag.getCompound("partyUnlockedSkills");

            for (PartyMember mxxx : PartyMember.values()) {
               if (pus.contains(mxxx.name())) {
                  NbtList psl = pus.getList(mxxx.name(), 8);

                  for (int j = 0; j < psl.size(); j++) {
                     prog.partyUnlockedSkills[mxxx.ordinal()].add(psl.getString(j));
                  }
               }
            }
         }

         data.players.put(uuid, prog);
      }

      return data;
   }

   public static BattleData get(MinecraftServer server) {
      BattleData c = cached;
      if (c != null && cachedFor == server) {
         return c;
      } else {
         PersistentStateManager mgr = server.getWorld(World.OVERWORLD).getPersistentStateManager();
         c = (BattleData)mgr.getOrCreate(new Type(BattleData::new, (nbt, lookup) -> readNbt(nbt, lookup), null), "turnbattle_data");
         cached = c;
         cachedFor = server;
         return c;
      }
   }

   public static class PlayerProgress {
      public int level = 1;
      public int savedLevel = 1;
      public long savedPlaytimeSec = 0L;
      public String lastBiome = null;
      public int exp = 0;
      public PlayerClass playerClass = null;
      public CharacterRace playerRace = null;
      public boolean soulRemoved = false;
      public String soulOutDim = null;
      public double soulOutX;
      public double soulOutY;
      public double soulOutZ;
      public float soulOutYaw;
      public float soulOutPitch;
      public final Set<String> favoriteFood = new HashSet<>();
      public Set<String> absorbedSouls = new LinkedHashSet<>();
      public MonsterWeapon monsterWeapon = null;
      public PartyMember partyMember = PartyMember.NONE;
      public String vesselName = null;
      public int vesselChoice = 0;
      public boolean introSeen = false;
      public boolean worldTypeSeen = false;
      public float partyMemberHp = -1.0F;
      public ItemStack equippedWeapon = null;
      public ItemStack partyEquippedWeapon = null;
      public ItemStack equippedAccessory = null;
      public ItemStack[] equippedArmor = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      public BattleStyle battleStyle = BattleStyle.BULLET_HELL;
      public BattleView battleView = BattleView.TRADITIONAL;
      public boolean rogueRun = false;
      public int rogueHandsOwed = 0;
      public int darkDollars = 0;
      public boolean magicant = false;
      public boolean magicantSeen = false;
      public String magicantReturnDim = null;
      public double magicantRetX;
      public double magicantRetY;
      public double magicantRetZ;
      public float magicantRetYaw;
      public SoulType soulType = null;
      public boolean determinationUsed = false;
      public boolean freeRollUsed = false;
      public int storyUnlockedChapter = 0;
      public int storyQuestIndex = 0;
      public boolean storyQuestStarted = false;
      public boolean storyQuestComplete = false;
      public final boolean[] storyChapterDone = new boolean[5];
      public String activeQuestlineId = null;
      public int questNodeIndex = 0;
      public boolean questNodeComplete = false;
      public int darkFountainSeals = 0;
      public int questlineNodeIndex = 0;
      public boolean questlineNodeDone = false;
      public final Set<String> completedQuestlines = new HashSet<>();
      public int routeKills = 0;
      public boolean routeSpared = false;
      public boolean shownKillWarning = false;
      public boolean ruthlessCancelled = false;
      public boolean isRuthless = false;
      public int skillPoints = 0;
      public final Set<String> unlockedSkills = new HashSet<>();
      public final Set<String> unequippedSkills = new HashSet<>();
      public int[] treeStatBonuses = new int[6];
      public final int[] partySkillPoints = new int[PartyMember.values().length];
      public final Set<String>[] partyUnlockedSkills = new HashSet[PartyMember.values().length];
      public CharStats playerStats = new CharStats();
      public final CharStats[] partyStats = new CharStats[PartyMember.values().length];
      public final int[] partyLevels = new int[PartyMember.values().length];

      public PlayerProgress() {
         for (int i = 0; i < this.partyStats.length; i++) {
            this.partyStats[i] = new CharStats();
         }

         for (int i = 0; i < this.partyLevels.length; i++) {
            this.partyLevels[i] = 1;
         }

         for (int i = 0; i < this.partyUnlockedSkills.length; i++) {
            this.partyUnlockedSkills[i] = new HashSet<>();
         }
      }
   }
}
