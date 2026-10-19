package com.pixelcite.turnbattle;

import com.pixelcite.turnbattle.mixin.MobEntityPersistAccessor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class EliteMobManager {
   public static final String[] TIER_NAMES = new String[]{"Common", "Uncommon", "Rare", "Legendary", "Impossible"};
   public static final Formatting[] TIER_COLORS = new Formatting[]{Formatting.WHITE, Formatting.GREEN, Formatting.AQUA, Formatting.GOLD, Formatting.RED};
   public static final String[] TEAM_NAMES = new String[]{"tb_elite_white", "tb_elite_green", "tb_elite_blue", "tb_elite_gold", "tb_elite_red"};
   public static final float SPAWN_CHANCE = 0.025F;
   public static final float[] EXP_MULT = new float[]{1.0F, 1.5F, 2.0F, 2.5F, 5.0F};
   public static final int[] ITEM_COUNTS = new int[]{2, 3, 6, 8, 12};
   public static final float[] STAT_MULT = new float[]{1.0F, 1.0F, 1.5F, 2.0F, 2.5F};
   private static final Map<UUID, Integer> eliteMobs = new HashMap<>();
   private static final Random RNG = Random.create();
   private static final Identifier ELITE_HP_MOD = Identifier.of("turnbattle", "elite_hp_mod");
   private static final Identifier ELITE_ATK_MOD = Identifier.of("turnbattle", "elite_atk_mod");
   private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("c", "bosses"));

   private static boolean isBossType(MobEntity mob) {
      if (mob.getType().isIn(BOSS_TAG)) {
         return true;
      } else if (mob instanceof EnderDragonEntity) {
         return true;
      } else if (mob instanceof WitherEntity) {
         return true;
      } else if (mob instanceof ElderGuardianEntity) {
         return true;
      } else if (mob instanceof WardenEntity) {
         return true;
      } else {
         try {
            for (Field field : mob.getClass().getDeclaredFields()) {
               if (field.getType().getSimpleName().contains("BossBar") || field.getType().getSimpleName().contains("BossEvent")) {
                  return true;
               }
            }
         } catch (Exception var5) {
         }

         return false;
      }
   }

   public static void tryRollEliteForced(MobEntity mob) {
      if (mob.getWorld() != null && !mob.getWorld().isClient()) {
         if (!(mob instanceof DarknerBossEntity)) {
            if (isBossType(mob)) {
               int roll = RNG.nextInt(100);
               int tier;
               if (roll < 60) {
                  tier = 2;
               } else if (roll < 85) {
                  tier = 3;
               } else {
                  tier = 4;
               }

               makeElite(mob, tier);
            } else if (mob instanceof HostileEntity) {
               if (!(RNG.nextFloat() > 0.025F)) {
                  int roll = RNG.nextInt(100);
                  int tier;
                  if (roll < 40) {
                     tier = 0;
                  } else if (roll < 70) {
                     tier = 1;
                  } else if (roll < 88) {
                     tier = 2;
                  } else if (roll < 97) {
                     tier = 3;
                  } else {
                     tier = 4;
                  }

                  makeElite(mob, tier);
               }
            }
         }
      }
   }

   public static void tryRollElite(MobEntity mob) {
      if (mob.getWorld() != null && !mob.getWorld().isClient()) {
         if (!mob.hasCustomName()) {
            if (isBossType(mob)) {
               int roll = RNG.nextInt(100);
               int tier;
               if (roll < 60) {
                  tier = 2;
               } else if (roll < 85) {
                  tier = 3;
               } else {
                  tier = 4;
               }

               makeElite(mob, tier);
            } else if (mob instanceof HostileEntity) {
               if (!(RNG.nextFloat() > 0.025F)) {
                  int roll = RNG.nextInt(100);
                  int tier;
                  if (roll < 40) {
                     tier = 0;
                  } else if (roll < 70) {
                     tier = 1;
                  } else if (roll < 88) {
                     tier = 2;
                  } else if (roll < 97) {
                     tier = 3;
                  } else {
                     tier = 4;
                  }

                  makeElite(mob, tier);
               }
            }
         }
      }
   }

   public static int rollRandomTier() {
      int roll = RNG.nextInt(100);
      if (roll < 40) {
         return 0;
      } else if (roll < 70) {
         return 1;
      } else if (roll < 88) {
         return 2;
      } else {
         return roll < 97 ? 3 : 4;
      }
   }

   public static void makeElite(MobEntity mob, int tier) {
      eliteMobs.put(mob.getUuid(), tier);
      applyEliteScale(mob, STAT_MULT[tier]);
      int curLevel = MobLevelManager.getMobLevel(mob);
      int playerLevel = MobLevelManager.nearestPlayerLevel(mob);
      if (playerLevel >= 0) {
         int minLevel = Math.min(99, playerLevel + 5);
         if (curLevel < minLevel) {
            MobLevelManager.applyMobLevel(mob, minLevel);
            MobLevelManager.registerMobLevel(mob, minLevel);
            curLevel = minLevel;
         }
      }

      MobLevelManager.applyBossNameTag(mob, curLevel, TIER_COLORS[tier], TIER_NAMES[tier]);
      if (mob.getWorld() instanceof ServerWorld sw) {
         ensureEliteTeams(sw.getServer());
         Scoreboard sb = sw.getServer().getScoreboard();
         Team team = sb.getTeam(TEAM_NAMES[tier]);
         if (team != null) {
            sb.addScoreHolderToTeam(mob.getUuidAsString(), team);
         }
      }

      ((MobEntityPersistAccessor)mob).setPersistentFlag(false);
   }

   public static void ensureEliteTeams(MinecraftServer server) {
      Scoreboard sb = server.getScoreboard();
      Formatting[] colours = new Formatting[]{Formatting.WHITE, Formatting.GREEN, Formatting.BLUE, Formatting.GOLD, Formatting.RED};

      for (int i = 0; i < TEAM_NAMES.length; i++) {
         if (sb.getTeam(TEAM_NAMES[i]) == null) {
            Team t = sb.addTeam(TEAM_NAMES[i]);
            t.setColor(colours[i]);
            t.setShowFriendlyInvisibles(false);
         }
      }
   }

   private static void applyEliteScale(MobEntity mob, float scale) {
      EntityAttributeInstance hpInst = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
      if (hpInst != null) {
         hpInst.removeModifier(ELITE_HP_MOD);
         double cur = hpInst.getBaseValue();
         hpInst.setBaseValue(cur * (double)scale);
         mob.setHealth(mob.getMaxHealth());
      }

      EntityAttributeInstance atkInst = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
      if (atkInst != null) {
         atkInst.removeModifier(ELITE_ATK_MOD);
         double extra = atkInst.getValue() * ((double)scale - 1.0);
         if (extra > 0.0) {
            atkInst.addPersistentModifier(new EntityAttributeModifier(ELITE_ATK_MOD, extra, Operation.ADD_VALUE));
         }
      }
   }

   public static void restoreElite(MobEntity mob, int tier) {
      eliteMobs.put(mob.getUuid(), tier);
      MobLevelManager.applyBossNameTag(mob, MobLevelManager.getMobLevel(mob), TIER_COLORS[tier], TIER_NAMES[tier]);
   }

   public static boolean isElite(UUID mobUuid) {
      return eliteMobs.containsKey(mobUuid);
   }

   public static int getTier(UUID mobUuid) {
      return eliteMobs.getOrDefault(mobUuid, -1);
   }

   public static void remove(UUID mobUuid) {
      eliteMobs.remove(mobUuid);
   }

   public static Item getLootItem(int playerLevel, int tier, java.util.Random rng) {
      int effectiveLevel = playerLevel + tier * 5;
      if (effectiveLevel < 10) {
         Item[] pool = new Item[]{Items.IRON_INGOT, Items.GOLD_INGOT, Items.ARROW, Items.BREAD};
         return pool[rng.nextInt(pool.length)];
      } else if (effectiveLevel < 20) {
         Item[] pool = new Item[]{Items.IRON_INGOT, Items.GOLD_INGOT, Items.EXPERIENCE_BOTTLE};
         return pool[rng.nextInt(pool.length)];
      } else if (effectiveLevel < 35) {
         Item[] pool = new Item[]{Items.DIAMOND, Items.GOLD_INGOT, Items.EMERALD, Items.EXPERIENCE_BOTTLE};
         return pool[rng.nextInt(pool.length)];
      } else {
         Item[] pool = new Item[]{Items.DIAMOND, Items.NETHERITE_INGOT, Items.EMERALD};
         return pool[rng.nextInt(pool.length)];
      }
   }

   public static void grantLoot(MinecraftServer server, BattleState state, int tier) {
      grantLoot(server, new ArrayList<>(state.participantOrder), tier);
   }

   public static void grantLoot(MinecraftServer server, List<UUID> players, int tier) {
      if (tier >= 0 && tier < ITEM_COUNTS.length) {
         int itemCount = ITEM_COUNTS[tier];
         java.util.Random jrng = new java.util.Random();

         for (UUID pid : players) {
            if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pid);
               if (pp != null) {
                  BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(pid);
                  Item lootItem = getLootItem(prog.level, tier, jrng);
                  int count = itemCount;
                  if (lootItem == Items.DIAMOND) {
                     count = Math.min(itemCount, 5);
                  } else if (lootItem == Items.NETHERITE_INGOT) {
                     count = Math.min(itemCount, 1);
                  }

                  ItemStack loot = new ItemStack(lootItem, count);
                  if (!pp.getInventory().insertStack(loot)) {
                     pp.dropItem(loot, false);
                  }

                  pp.sendMessage(
                     Text.literal("\u2726 [" + TIER_NAMES[tier] + "] Elite defeated! +" + count + "x " + lootItem.getName().getString() + "!")
                        .formatted(TIER_COLORS[tier]),
                     false
                  );
               }
            }
         }
      }
   }
}
