package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentState.Type;

public class RecruitData extends PersistentState {
   public static final int CUSTOM_OFFSET = 1000;
   public final Map<UUID, RecruitData.PlayerRecruits> players = new HashMap<>();
   private static final String ID = "soulbound_recruits";

   public RecruitData.PlayerRecruits getOrCreate(UUID uuid) {
      return this.players.computeIfAbsent(uuid, k -> new RecruitData.PlayerRecruits());
   }

   public void onPlayerLevelUp(UUID uuid) {
      RecruitData.PlayerRecruits pr = this.getOrCreate(uuid);
      this.levelUpSlot(pr, pr.activeRecruit);
      this.levelUpSlot(pr, pr.secondActiveRecruit);
      this.markDirty();
   }

   private void levelUpSlot(RecruitData.PlayerRecruits pr, int slotCode) {
      if (slotCode >= 0) {
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            if (ci < pr.customRecruits.size()) {
               RecruitData.RecruitState rs = pr.customRecruits.get(ci).state;
               if (rs.level < 99) {
                  rs.level++;
                  rs.stats.unspent += 3;
                  rs.skillPoints += 3;
               }
            }
         } else if (slotCode < pr.recruits.length) {
            RecruitData.RecruitState rs = pr.recruits[slotCode];
            if (rs != null && rs.unlocked && rs.level < 99) {
               rs.level++;
               rs.stats.unspent += 3;
               rs.skillPoints += 3;
            }
         }
      }
   }

   public static float maxHp(RecruitData.RecruitState rs) {
      return 20.0F + effectiveStats(rs).vitalityHp();
   }

   public static CharStats effectiveStats(RecruitData.RecruitState rs) {
      WeaponStats ws = WeaponStats.fromStack(rs.equippedWeapon);
      boolean hasArmor = rs.equippedArmor != null
         && (rs.equippedArmor[0] != null && !rs.equippedArmor[0].isEmpty() || rs.equippedArmor[1] != null && !rs.equippedArmor[1].isEmpty());
      if (ws == null && !hasArmor) {
         return rs.stats;
      } else {
         CharStats e = rs.stats.copy();

         for (int i = 0; i < 6; i++) {
            int add = ws != null ? ws.netStatBonus(i) : 0;
            if (hasArmor) {
               for (ItemStack piece : rs.equippedArmor) {
                  if (piece != null && !piece.isEmpty()) {
                     ArmorStats as = ArmorStats.fromStack(piece);
                     if (as != null) {
                        add += as.netStatBonus(i);
                     }
                  }
               }
            }

            if (add != 0) {
               switch (i) {
                  case 0:
                     e.offense = Math.max(0, e.offense + add);
                     break;
                  case 1:
                     e.defense = Math.max(0, e.defense + add);
                     break;
                  case 2:
                     e.magic = Math.max(0, e.magic + add);
                     break;
                  case 3:
                     e.iq = Math.max(0, e.iq + add);
                     break;
                  case 4:
                     e.vitality = Math.max(0, e.vitality + add);
                     break;
                  case 5:
                     e.luck = Math.max(0, e.luck + add);
               }
            }
         }

         return e;
      }
   }

   public float currentHp(RecruitData.RecruitState rs) {
      float max = maxHp(rs);
      return rs.hp < 0.0F ? max : Math.min(rs.hp, max);
   }

   public static RecruitData get(MinecraftServer server) {
      PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
      return (RecruitData)mgr.getOrCreate(new Type(RecruitData::new, (nbt, lookup) -> readNbt(nbt, lookup), null), "soulbound_recruits");
   }

   public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
      NbtList list = new NbtList();
      this.players.forEach((uuid, pr) -> {
         NbtCompound tag = new NbtCompound();
         tag.putUuid("uuid", uuid);
         tag.putInt("activeRecruit", pr.activeRecruit);
         tag.putInt("secondActiveRecruit", pr.secondActiveRecruit);
         NbtList customList = new NbtList();

         for (RecruitData.CustomRecruitState cr : pr.customRecruits) {
            NbtCompound ct = new NbtCompound();
            ct.putString("name", cr.name);
            ct.putString("class", cr.playerClass.name());
            ct.putString("soul", cr.soulType != null ? cr.soulType.name() : "NONE");
            ct.putInt("level", cr.state.level);
            ct.putFloat("hp", cr.state.hp);
            ct.putInt("skillPoints", cr.state.skillPoints);
            ct.put("stats", cr.state.stats.toNbt());
            NbtList crSkillList = new NbtList();

            for (String s : cr.state.unlockedSkills) {
               crSkillList.add(NbtString.of(s));
            }

            ct.put("skills", crSkillList);
            NbtList crUneqList = new NbtList();

            for (String s : cr.state.unequippedSkills) {
               crUneqList.add(NbtString.of(s));
            }

            ct.put("uneq_skills", crUneqList);
            if (cr.state.equippedWeapon != null && !cr.state.equippedWeapon.isEmpty()) {
               ct.put("weapon", (NbtCompound)cr.state.equippedWeapon.encode(lookup));
            }

            if (cr.state.equippedAccessory != null && !cr.state.equippedAccessory.isEmpty()) {
               ct.put("accessory", (NbtCompound)cr.state.equippedAccessory.encode(lookup));
            }

            for (int ai = 0; ai < cr.state.equippedArmor.length; ai++) {
               ItemStack ap = cr.state.equippedArmor[ai];
               if (ap != null && !ap.isEmpty()) {
                  ct.put("armor" + ai, (NbtCompound)ap.encode(lookup));
               }
            }

            customList.add(ct);
         }

         tag.put("customRecruits", customList);
         NbtCompound recruitsTag = new NbtCompound();

         for (Recruit r : Recruit.values()) {
            RecruitData.RecruitState rs = pr.recruits[r.ordinal()];
            NbtCompound rt = new NbtCompound();
            rt.putBoolean("unlocked", rs.unlocked);
            rt.putInt("level", rs.level);
            rt.putFloat("hp", rs.hp);
            rt.putInt("skillPoints", rs.skillPoints);
            rt.put("stats", rs.stats.toNbt());
            NbtList skillList = new NbtList();

            for (String s : rs.unlockedSkills) {
               skillList.add(NbtString.of(s));
            }

            rt.put("skills", skillList);
            NbtList uneqList = new NbtList();

            for (String s : rs.unequippedSkills) {
               uneqList.add(NbtString.of(s));
            }

            rt.put("uneq_skills", uneqList);
            if (rs.equippedWeapon != null && !rs.equippedWeapon.isEmpty()) {
               rt.put("weapon", (NbtCompound)rs.equippedWeapon.encode(lookup));
            }

            if (rs.equippedAccessory != null && !rs.equippedAccessory.isEmpty()) {
               rt.put("accessory", (NbtCompound)rs.equippedAccessory.encode(lookup));
            }

            for (int aix = 0; aix < rs.equippedArmor.length; aix++) {
               ItemStack ap = rs.equippedArmor[aix];
               if (ap != null && !ap.isEmpty()) {
                  rt.put("armor" + aix, (NbtCompound)ap.encode(lookup));
               }
            }

            recruitsTag.put(r.name(), rt);
         }

         tag.put("recruits", recruitsTag);
         list.add(tag);
      });
      nbt.put("players", list);
      return nbt;
   }

   public static RecruitData readNbt(NbtCompound nbt, WrapperLookup lookup) {
      RecruitData data = new RecruitData();
      NbtList list = nbt.getList("players", 10);

      for (int i = 0; i < list.size(); i++) {
         NbtCompound tag = list.getCompound(i);
         UUID uuid = tag.getUuid("uuid");
         RecruitData.PlayerRecruits pr = new RecruitData.PlayerRecruits();
         pr.activeRecruit = tag.getInt("activeRecruit");
         if (tag.contains("secondActiveRecruit")) {
            pr.secondActiveRecruit = tag.getInt("secondActiveRecruit");
         }

         if (tag.contains("customRecruits")) {
            NbtList cl2 = tag.getList("customRecruits", 10);

            for (int ci = 0; ci < cl2.size(); ci++) {
               NbtCompound ct = cl2.getCompound(ci);
               RecruitData.CustomRecruitState cr = new RecruitData.CustomRecruitState();
               cr.name = ct.getString("name");

               try {
                  cr.playerClass = PlayerClass.valueOf(ct.getString("class"));
               } catch (Exception var20) {
               }

               try {
                  String sn = ct.getString("soul");
                  cr.soulType = sn.equals("NONE") ? null : SoulType.valueOf(sn);
               } catch (Exception var19) {
               }

               cr.state.level = ct.getInt("level");
               cr.state.hp = ct.getFloat("hp");
               cr.state.skillPoints = ct.getInt("skillPoints");
               if (ct.contains("stats")) {
                  CharStats loaded = CharStats.fromNbt(ct.getCompound("stats"));
                  cr.state.stats.offense = loaded.offense;
                  cr.state.stats.defense = loaded.defense;
                  cr.state.stats.magic = loaded.magic;
                  cr.state.stats.iq = loaded.iq;
                  cr.state.stats.vitality = loaded.vitality;
                  cr.state.stats.luck = loaded.luck;
                  cr.state.stats.unspent = loaded.unspent;
               }

               NbtList crSkillList = ct.getList("skills", 8);

               for (int j = 0; j < crSkillList.size(); j++) {
                  cr.state.unlockedSkills.add(crSkillList.getString(j));
               }

               NbtList crUneqList = ct.getList("uneq_skills", 8);

               for (int j = 0; j < crUneqList.size(); j++) {
                  cr.state.unequippedSkills.add(crUneqList.getString(j));
               }

               if (ct.contains("weapon")) {
                  cr.state.equippedWeapon = ItemStack.fromNbt(lookup, ct.getCompound("weapon")).orElse(ItemStack.EMPTY);
               }

               if (ct.contains("accessory")) {
                  cr.state.equippedAccessory = ItemStack.fromNbt(lookup, ct.getCompound("accessory")).orElse(ItemStack.EMPTY);
               }

               for (int ai = 0; ai < cr.state.equippedArmor.length; ai++) {
                  String key = "armor" + ai;
                  if (ct.contains(key)) {
                     cr.state.equippedArmor[ai] = ItemStack.fromNbt(lookup, ct.getCompound(key)).orElse(ItemStack.EMPTY);
                  }
               }

               pr.customRecruits.add(cr);
            }
         }

         NbtCompound recruitsTag = tag.getCompound("recruits");

         for (Recruit r : Recruit.values()) {
            if (recruitsTag.contains(r.name())) {
               NbtCompound rt = recruitsTag.getCompound(r.name());
               RecruitData.RecruitState rs = pr.recruits[r.ordinal()];
               rs.unlocked = rt.getBoolean("unlocked");
               rs.level = rt.getInt("level");
               rs.hp = rt.getFloat("hp");
               rs.skillPoints = rt.getInt("skillPoints");
               if (rt.contains("stats")) {
                  CharStats loaded = CharStats.fromNbt(rt.getCompound("stats"));
                  rs.stats.offense = loaded.offense;
                  rs.stats.defense = loaded.defense;
                  rs.stats.magic = loaded.magic;
                  rs.stats.iq = loaded.iq;
                  rs.stats.vitality = loaded.vitality;
                  rs.stats.luck = loaded.luck;
                  rs.stats.unspent = loaded.unspent;
               }

               NbtList skillList = rt.getList("skills", 8);

               for (int j = 0; j < skillList.size(); j++) {
                  rs.unlockedSkills.add(skillList.getString(j));
               }

               NbtList uneqList = rt.getList("uneq_skills", 8);

               for (int j = 0; j < uneqList.size(); j++) {
                  rs.unequippedSkills.add(uneqList.getString(j));
               }

               if (rt.contains("weapon")) {
                  rs.equippedWeapon = ItemStack.fromNbt(lookup, rt.getCompound("weapon")).orElse(ItemStack.EMPTY);
               }

               if (rt.contains("accessory")) {
                  rs.equippedAccessory = ItemStack.fromNbt(lookup, rt.getCompound("accessory")).orElse(ItemStack.EMPTY);
               }

               for (int aix = 0; aix < rs.equippedArmor.length; aix++) {
                  String key = "armor" + aix;
                  if (rt.contains(key)) {
                     rs.equippedArmor[aix] = ItemStack.fromNbt(lookup, rt.getCompound(key)).orElse(ItemStack.EMPTY);
                  }
               }
            }
         }

         data.players.put(uuid, pr);
      }

      return data;
   }

   public static class CustomRecruitState {
      public String name = "Vessel";
      public PlayerClass playerClass = PlayerClass.HUMAN_MAGE;
      public SoulType soulType = SoulType.INTEGRITY;
      public final RecruitData.RecruitState state = new RecruitData.RecruitState();

      public CustomRecruitState() {
         this.state.unlocked = true;
      }
   }

   public static class PlayerRecruits {
      public int activeRecruit = -1;
      public int secondActiveRecruit = -1;
      public final List<RecruitData.CustomRecruitState> customRecruits = new ArrayList<>();
      public final RecruitData.RecruitState[] recruits = new RecruitData.RecruitState[Recruit.values().length];

      public PlayerRecruits() {
         for (int i = 0; i < this.recruits.length; i++) {
            this.recruits[i] = new RecruitData.RecruitState();
         }
      }

      public RecruitData.RecruitState get(Recruit r) {
         return this.recruits[r.ordinal()];
      }

      public RecruitData.RecruitState getActive() {
         return this.activeRecruit >= 0 && this.activeRecruit < this.recruits.length ? this.recruits[this.activeRecruit] : null;
      }

      public Recruit getActiveRecruit() {
         return this.activeRecruit >= 0 && this.activeRecruit < Recruit.values().length ? Recruit.values()[this.activeRecruit] : null;
      }
   }

   public static class RecruitState {
      public boolean unlocked = false;
      public int level = 1;
      public float hp = -1.0F;
      public int skillPoints = 0;
      public final Set<String> unlockedSkills = new HashSet<>();
      public final Set<String> unequippedSkills = new HashSet<>();
      public ItemStack equippedWeapon = null;
      public ItemStack equippedAccessory = null;
      public ItemStack[] equippedArmor = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      public final CharStats stats = new CharStats();
   }
}
