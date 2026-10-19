package com.pixelcite.turnbattle;

import com.pixelcite.turnbattle.mixin.MobEntityPersistAccessor;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.GlowSquidEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.SalmonEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class MobLevelManager {
   public static final String NBT_LEVEL = "turnbattle_level";
   private static final Map<UUID, Integer> MOB_LEVELS = new ConcurrentHashMap<>();
   public static final Map<UUID, String> MOB_PASSIVES = new ConcurrentHashMap<>();
   public static final Map<UUID, Float> MOB_BULLET_SPEEDS = new ConcurrentHashMap<>();
   public static final Map<UUID, Float> MOB_DMG_TAKEN = new ConcurrentHashMap<>();
   public static final Map<UUID, Affinity> MOB_WEAKNESS = new ConcurrentHashMap<>();
   public static final Map<UUID, Affinity> MOB_RESISTANCE = new ConcurrentHashMap<>();
   private static final Identifier MOD_HP = Identifier.of("turnbattle", "turnbattle-level-hp");
   private static final Identifier MOD_ATK = Identifier.of("turnbattle", "turnbattle-level-atk");
   private static final Identifier MOD_ARM = Identifier.of("turnbattle", "turnbattle-level-arm");
   private static final Identifier PLAYER_HP_MOD = Identifier.of("turnbattle", "player-hp-mod");
   private static final Identifier PLAYER_ATK_MOD = Identifier.of("turnbattle", "player-atk-mod");
   private static final Identifier PLAYER_ARM_MOD = Identifier.of("turnbattle", "player-arm-mod");
   private static final Identifier WEAPON_STATS_MOD = Identifier.of("turnbattle", "turnbattle-weapon-hp");
   private static final Identifier ARMOR_STATS_MOD_0 = Identifier.of("turnbattle", "turnbattle-armor-hp-0");
   private static final Identifier ARMOR_STATS_MOD_1 = Identifier.of("turnbattle", "turnbattle-armor-hp-1");
   private static final Random RNG = new Random();

   public static int getStoredLevel(MobEntity mob) {
      return MOB_LEVELS.getOrDefault(mob.getUuid(), 0);
   }

   public static void restoreLevel(MobEntity mob, int level) {
      MOB_LEVELS.put(mob.getUuid(), level);
      applyMobLevel(mob, level);
   }

   public static void initMob(MobEntity mob) {
      if (!(mob instanceof SoulBodyEntity)) {
         if (!(mob instanceof Shopkeeper.Entity)) {
            if (!MOB_LEVELS.containsKey(mob.getUuid())) {
               if (!mob.hasCustomName()) {
                  if (TurnBattleConfig.get().passiveMobBattles || !(mob instanceof PassiveEntity) || isAquaticMob(mob)) {
                     if (mob.getWorld() instanceof ServerWorld sw && DarkFountainManager.isDarkWorld(sw.getRegistryKey())) {
                        DarkFountainData data = DarkFountainData.get(sw.getServer());
                        DarkFountainData.DarkWorldInstance inst = data.getInstanceByWorldKey(sw.getRegistryKey());
                        if (inst != null) {
                           int level = inst.mobLevel;
                           MOB_LEVELS.put(mob.getUuid(), level);
                           applyMobLevel(mob, level);
                           return;
                        }
                     }

                     int level = pickLevel(mob);
                     MOB_LEVELS.put(mob.getUuid(), level);
                     applyMobLevel(mob, level);
                     EliteMobManager.tryRollEliteForced(mob);
                     Affinity[] affinities = Affinity.values();
                     Affinity weak = affinities[RNG.nextInt(affinities.length)];

                     Affinity resist;
                     do {
                        resist = affinities[RNG.nextInt(affinities.length)];
                     } while (resist == weak);

                     MOB_WEAKNESS.put(mob.getUuid(), weak);
                     MOB_RESISTANCE.put(mob.getUuid(), resist);
                  }
               }
            }
         }
      }
   }

   public static boolean isAquaticMob(MobEntity mob) {
      return mob instanceof FishEntity
         || mob instanceof SquidEntity
         || mob instanceof GlowSquidEntity
         || mob instanceof DolphinEntity
         || mob instanceof TropicalFishEntity
         || mob instanceof PufferfishEntity
         || mob instanceof SalmonEntity
         || mob instanceof CodEntity;
   }

   public static int getMobLevel(LivingEntity mob) {
      return MOB_LEVELS.getOrDefault(mob.getUuid(), 1);
   }

   public static void registerMobLevel(LivingEntity mob, int level) {
      MOB_LEVELS.put(mob.getUuid(), level);
   }

   public static Affinity getMobWeakness(LivingEntity mob) {
      return MOB_WEAKNESS.get(mob.getUuid());
   }

   public static Affinity getMobResistance(LivingEntity mob) {
      return MOB_RESISTANCE.get(mob.getUuid());
   }

   public static void ensureAffinities(LivingEntity mob) {
      if (!MOB_WEAKNESS.containsKey(mob.getUuid()) || !MOB_RESISTANCE.containsKey(mob.getUuid())) {
         Affinity[] affinities = Affinity.values();
         Affinity weak = affinities[RNG.nextInt(affinities.length)];

         Affinity resist;
         do {
            resist = affinities[RNG.nextInt(affinities.length)];
         } while (resist == weak);

         MOB_WEAKNESS.put(mob.getUuid(), weak);
         MOB_RESISTANCE.put(mob.getUuid(), resist);
      }
   }

   public static int nearestPlayerLevel(MobEntity mob) {
      if (mob.getWorld() instanceof ServerWorld sw) {
         ServerPlayerEntity var9 = null;
         double var3 = Double.MAX_VALUE;

         for (ServerPlayerEntity p : sw.getPlayers()) {
            double d = p.squaredDistanceTo(mob);
            if (d < 65536.0 && d < var3) {
               var3 = d;
               var9 = p;
            }
         }

         if (var9 == null) {
            return -1;
         } else {
            BattleData.PlayerProgress prog = BattleData.get(sw.getServer()).getOrCreate(var9.getUuid());
            return prog.level;
         }
      } else {
         return -1;
      }
   }

   private static int pickLevel(MobEntity mob) {
      String dim = mob.getWorld().getRegistryKey().getValue().toString();
      int area = AreaLevel.at(dim, mob.getX(), mob.getZ(), TurnBattleConfig.get().distanceLevelMultiplier);
      return Math.max(1, Math.min(99, area - 2 + RNG.nextInt(5)));
   }

   public static void applyMobLevel(MobEntity mob, int level) {
      MobProfile.Profile profile = MobProfile.getProfile(mob);
      applyMobHp(mob, level, profile.hpMult());
      applyMobAttack(mob, level, profile.atkMult());
      applyMobArmor(mob, level, profile.armorMult());
      equipGear(mob, level);
      applyNameTag(mob, level);
      MOB_PASSIVES.put(mob.getUuid(), profile.passive());
      MOB_BULLET_SPEEDS.put(mob.getUuid(), profile.bulletSpeedMult());
      MOB_DMG_TAKEN.put(mob.getUuid(), profile.damageTakenMult());
   }

   public static String getMobPassive(LivingEntity mob) {
      return MOB_PASSIVES.getOrDefault(mob.getUuid(), "");
   }

   public static float getMobBulletSpeed(LivingEntity mob) {
      return MOB_BULLET_SPEEDS.getOrDefault(mob.getUuid(), 1.0F);
   }

   public static float getMobDamageTaken(LivingEntity mob) {
      return MOB_DMG_TAKEN.getOrDefault(mob.getUuid(), 1.0F);
   }

   public static float getMobExpMult(LivingEntity mob) {
      return MobProfile.getProfile(mob).expMult();
   }

   public static float computeMobMaxHp(int level) {
      return (8.0F + (float)level * 10.0F) * hpStep(level);
   }

   public static float hpStep(int level) {
      return level > 55 ? 4.0F : (level > 35 ? 3.0F : (level > 17 ? 2.0F : 1.0F));
   }

   private static void applyMobHp(MobEntity mob, int level, float hpMult) {
      float targetHp = (8.0F + (float)level * 10.0F) * hpStep(level) * hpMult;
      EntityAttributeInstance inst = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
      if (inst != null) {
         inst.removeModifier(MOD_HP);
         removeIfPresent(inst, "turnbattle-level-hp");
         removeIfPresent(inst, "turnbattle_level_hp");
         removeIfPresent(inst, "a1b2c3d4-0001-0000-0000-000000000001");
         removeIfPresent(inst, "a1b2c3d4-0001-0000-0000-000000000002");
         float extra = Math.max(0.0F, targetHp - (float)inst.getBaseValue());
         if (extra > 0.0F) {
            inst.addPersistentModifier(new EntityAttributeModifier(Identifier.of("turnbattle", "turnbattle-level-hp"), (double)extra, Operation.ADD_VALUE));
         }

         mob.setHealth(mob.getMaxHealth());
      }
   }

   private static void applyMobAttack(MobEntity mob, int level, float atkMult) {
      float targetAtk = (2.0F + (float)level * 1.4F) * atkMult;
      EntityAttributeInstance inst = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
      if (inst != null) {
         inst.removeModifier(MOD_ATK);
         float baseVal = (float)inst.getBaseValue();
         if (targetAtk >= baseVal) {
            float delta = targetAtk - baseVal;
            if (delta > 0.0F) {
               inst.addPersistentModifier(new EntityAttributeModifier(Identifier.of("turnbattle", "turnbattle-level-atk"), (double)delta, Operation.ADD_VALUE));
            }
         } else {
            try {
               inst.setBaseValue((double)Math.max(0.5F, targetAtk));
            } catch (Exception var8) {
               float delta = Math.max(-baseVal + 0.5F, targetAtk - baseVal);
               inst.addPersistentModifier(new EntityAttributeModifier(Identifier.of("turnbattle", "turnbattle-level-atk"), (double)delta, Operation.ADD_VALUE));
            }
         }
      }
   }

   private static void applyMobArmor(MobEntity mob, int level, float armorMult) {
      EntityAttributeInstance inst = mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
      if (inst != null) {
         inst.removeModifier(MOD_ARM);
         inst.addPersistentModifier(
            new EntityAttributeModifier(Identifier.of("turnbattle", "turnbattle-level-arm"), (double)((float)level * 0.2F * armorMult), Operation.ADD_VALUE)
         );
      }
   }

   private static void equipGear(MobEntity mob, int level) {
      if (level >= 10) {
         if (!(mob instanceof HumanEnemyEntity) && !(mob instanceof MirrorCloneEntity)) {
            if (!(mob instanceof DarknerEntity)) {
               ItemStack helmet;
               ItemStack chestplate;
               ItemStack leggings;
               ItemStack boots;
               if (level < 25) {
                  helmet = new ItemStack(Items.LEATHER_HELMET);
                  chestplate = new ItemStack(Items.LEATHER_CHESTPLATE);
                  leggings = new ItemStack(Items.LEATHER_LEGGINGS);
                  boots = new ItemStack(Items.LEATHER_BOOTS);
               } else if (level < 45) {
                  helmet = new ItemStack(Items.IRON_HELMET);
                  chestplate = new ItemStack(Items.IRON_CHESTPLATE);
                  leggings = new ItemStack(Items.IRON_LEGGINGS);
                  boots = new ItemStack(Items.IRON_BOOTS);
               } else if (level < 60) {
                  helmet = new ItemStack(Items.DIAMOND_HELMET);
                  chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
                  leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
                  boots = new ItemStack(Items.DIAMOND_BOOTS);
               } else {
                  helmet = new ItemStack(Items.NETHERITE_HELMET);
                  chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);
                  leggings = new ItemStack(Items.NETHERITE_LEGGINGS);
                  boots = new ItemStack(Items.NETHERITE_BOOTS);
               }

               if (mob.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                  mob.equipStack(EquipmentSlot.HEAD, helmet);
               }

               if (mob.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
                  mob.equipStack(EquipmentSlot.CHEST, chestplate);
               }

               if (mob.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) {
                  mob.equipStack(EquipmentSlot.LEGS, leggings);
               }

               if (mob.getEquippedStack(EquipmentSlot.FEET).isEmpty()) {
                  mob.equipStack(EquipmentSlot.FEET, boots);
               }

               for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                  mob.setEquipmentDropChance(slot, 0.0F);
               }
            }
         }
      }
   }

   public static void applyNameTag(MobEntity mob, int level) {
      if (!TurnBattleConfig.get().showMobNametags) {
         mob.setCustomNameVisible(false);
      } else {
         String typeName = mob instanceof HumanEnemyEntity h
            ? h.trait().mobName()
            : (
               mob instanceof MirrorCloneEntity mc
                  ? mc.mobName()
                  : (
                     mob instanceof DarknerBossEntity db
                        ? DarknerBosses.fullName(db)
                        : (mob instanceof DarknerEntity dk ? Darkners.nameFor(dk.item(), dk.seed()) : mob.getType().getName().getString())
                  )
            );
         mob.setCustomName(Text.literal("Lv." + level + " ").formatted(levelColor(level)).append(Text.literal(typeName).formatted(Formatting.WHITE)));
         mob.setCustomNameVisible(true);
      }
   }

   public static void applyBossNameTag(MobEntity mob, int level, Formatting tierColor, String tierName) {
      if (!TurnBattleConfig.get().showMobNametags) {
         mob.setCustomNameVisible(false);
      } else {
         mob.setCustomName(
            Text.literal("[" + tierName + " BOSS] ")
               .formatted(tierColor)
               .append(Text.literal("Lv." + level + " ").formatted(levelColor(level)))
               .append(
                  Text.literal(
                        mob instanceof HumanEnemyEntity h
                           ? h.trait().mobName()
                           : (
                              mob instanceof MirrorCloneEntity mc
                                 ? mc.mobName()
                                 : (
                                    mob instanceof DarknerBossEntity db
                                       ? DarknerBosses.fullName(db)
                                       : (mob instanceof DarknerEntity dk ? Darkners.nameFor(dk.item(), dk.seed()) : mob.getType().getName().getString())
                                 )
                           )
                     )
                     .formatted(Formatting.WHITE)
               )
         );
         mob.setCustomNameVisible(true);
         ((MobEntityPersistAccessor)mob).setPersistentFlag(false);
      }
   }

   private static Formatting levelColor(int level) {
      if (level <= 5) {
         return Formatting.GREEN;
      } else if (level <= 10) {
         return Formatting.YELLOW;
      } else if (level <= 17) {
         return Formatting.GOLD;
      } else {
         return level <= 24 ? Formatting.RED : Formatting.DARK_RED;
      }
   }

   public static void applyPlayerStats(ServerPlayerEntity player, int level) {
      applyPlayerStats(player, level, new CharStats());
   }

   public static void applyPlayerStats(ServerPlayerEntity player, int level, CharStats stats) {
      applyMod(player, EntityAttributes.GENERIC_MAX_HEALTH, PLAYER_HP_MOD, "turnbattle_player_hp", (double)stats.vitalityHp(), Operation.ADD_VALUE);
      applyMod(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, PLAYER_ATK_MOD, "turnbattle_player_atk", 0.0, Operation.ADD_VALUE);
      applyMod(player, EntityAttributes.GENERIC_ARMOR, PLAYER_ARM_MOD, "turnbattle_player_arm", 0.0, Operation.ADD_VALUE);
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static void removeIfPresent(EntityAttributeInstance inst, String path) {
      try {
         inst.removeModifier(Identifier.of("turnbattle", path));
      } catch (Exception var3) {
      }
   }

   private static void applyMod(PlayerEntity player, RegistryEntry<EntityAttribute> attr, Identifier modId, String name, double value, Operation op) {
      EntityAttributeInstance inst = player.getAttributeInstance(attr);
      if (inst != null) {
         inst.removeModifier(modId);
         String mp = modId.getPath();
         if (mp.contains("hp")) {
            removeIfPresent(inst, "b2c3d4e5-0002-0000-0000-000000000001");
            removeIfPresent(inst, "player-hp-mod");
            removeIfPresent(inst, "turnbattle_player_hp");
         }

         if (mp.contains("atk")) {
            removeIfPresent(inst, "b2c3d4e5-0002-0000-0000-000000000002");
            removeIfPresent(inst, "player-atk-mod");
            removeIfPresent(inst, "turnbattle_player_atk");
         }

         if (mp.contains("arm")) {
            removeIfPresent(inst, "b2c3d4e5-0002-0000-0000-000000000003");
            removeIfPresent(inst, "player-arm-mod");
            removeIfPresent(inst, "turnbattle_player_arm");
         }

         if (value != 0.0) {
            inst.addPersistentModifier(new EntityAttributeModifier(modId, value, op));
         }
      }
   }

   public static float applyPlayerArmor(ServerPlayerEntity player, float rawDamage) {
      if (player.getServer() == null) {
         return rawDamage;
      } else {
         BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(player.getUuid());
         float factor = 1.0F;

         for (int i = 0; i < 2; i++) {
            ItemStack piece = prog.equippedArmor[i];
            if (piece != null && !piece.isEmpty()) {
               ArmorStats.Tier tier = ArmorStats.tierOf(piece);
               if (tier != null) {
                  factor *= tier.factor();
               }
            }
         }

         return rawDamage * factor;
      }
   }

   public static float getArmorFactor(ServerPlayerEntity player) {
      if (player.getServer() == null) {
         return 1.0F;
      } else {
         BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(player.getUuid());
         float factor = 1.0F;

         for (int i = 0; i < 2; i++) {
            ItemStack piece = prog.equippedArmor[i];
            if (piece != null && !piece.isEmpty()) {
               ArmorStats.Tier tier = ArmorStats.tierOf(piece);
               if (tier != null) {
                  factor *= tier.factor();
               }
            }
         }

         return factor;
      }
   }

   public static void applyWeaponStats(ServerPlayerEntity player, WeaponStats ws) {
      if (ws != null) {
         BattleData data = BattleData.get(player.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
         int vitalityBonus = ws.netStatBonus(4);
         double hpBonus = (double)vitalityBonus * 10.0;
         if (hpBonus != 0.0) {
            EntityAttributeInstance hpInst = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (hpInst != null) {
               hpInst.removeModifier(WEAPON_STATS_MOD);
               hpInst.addPersistentModifier(new EntityAttributeModifier(WEAPON_STATS_MOD, hpBonus, Operation.ADD_VALUE));
            }
         }

         if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
         }
      }
   }

   public static void applyArmorStats(ServerPlayerEntity player, ArmorStats as, int slot) {
      if (as != null) {
         Identifier modId = slot == 0 ? ARMOR_STATS_MOD_0 : ARMOR_STATS_MOD_1;
         int vitalityBonus = as.netStatBonus(4);
         double hpBonus = (double)vitalityBonus * 10.0;
         EntityAttributeInstance hpInst = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
         if (hpInst != null) {
            hpInst.removeModifier(modId);
            if (hpBonus != 0.0) {
               hpInst.addPersistentModifier(new EntityAttributeModifier(modId, hpBonus, Operation.ADD_VALUE));
            }
         }

         if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
         }
      }
   }

   public static void removeArmorStats(ServerPlayerEntity player, int slot) {
      Identifier modId = slot == 0 ? ARMOR_STATS_MOD_0 : ARMOR_STATS_MOD_1;
      EntityAttributeInstance hpInst = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
      if (hpInst != null) {
         hpInst.removeModifier(modId);
      }

      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   public static void removeWeaponStats(ServerPlayerEntity player) {
      EntityAttributeInstance hpInst = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
      if (hpInst != null) {
         hpInst.removeModifier(WEAPON_STATS_MOD);
      }

      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }
}
