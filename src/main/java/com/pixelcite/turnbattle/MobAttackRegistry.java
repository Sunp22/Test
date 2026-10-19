package com.pixelcite.turnbattle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MobAttackRegistry {
   private static final Map<String, MobAttack[]> REGISTRY = new LinkedHashMap<>();
   private static final int C_FIRE = -39390;
   private static final int C_ICE = -7807489;
   private static final int C_DARK = -7846708;
   private static final int C_POISON = -12268476;
   private static final int C_BONE = -2236997;
   private static final int C_ELEC = -188;
   private static final int C_BLOOD = -3399134;
   private static final int C_GOLDEN = -13244;
   private static final int C_SLIME = -7812028;
   private static final int C_VOID = -14548924;
   private static final int C_WATER = -14522625;
   private static final int C_NETHER = -48128;
   private static final int C_NATURE = -12272828;
   private static final int C_SPECTRAL = -5570646;
   private static final MobAttack[] FALLBACK = new MobAttack[]{
      new MobAttack("Strike", 0.75F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT),
      new MobAttack("Assault", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH),
      new MobAttack("Flurry", 0.7F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH)
   };

   private static void reg(String key, MobAttack a0, MobAttack a1, MobAttack a2) {
      REGISTRY.put(key, new MobAttack[]{a0, a1, a2});
   }

   public static MobAttack[] getAttacks(String typeKey) {
      return REGISTRY.getOrDefault(typeKey, FALLBACK);
   }

   public static MobAttack pickAttack(String typeKey, Random rng) {
      MobAttack[] attacks = getAttacks(typeKey);
      return attacks[rng.nextInt(attacks.length)];
   }

   static {
      reg(
         "minecraft:zombie",
         new MobAttack("Zombie Charge", 0.7F, MobAttack.Targeting.MULTI, MobAttack.Special.NONE, MobAttack.Visual.SLASH, -3399134),
         new MobAttack("Zombie Bite", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -3399134),
         new MobAttack("Zombie Roar", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -3399134)
      );
      reg(
         "minecraft:zombie_villager",
         new MobAttack("Shamble", 0.65F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -3399134),
         new MobAttack("Infected Bite", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -12268476),
         new MobAttack("Desperate Roar", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -3399134)
      );
      reg(
         "minecraft:husk",
         new MobAttack("Desert Lunge", 0.7F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -3372988),
         new MobAttack("Sand Slam", 0.8F, MobAttack.Targeting.MULTI, MobAttack.Visual.AOE_RING, -3372988),
         new MobAttack("Withered Howl", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -3372988)
      );
      reg(
         "minecraft:drowned",
         new MobAttack("Aqua Grab", 0.75F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -14522625),
         new MobAttack("Trident Hurl", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -14522625),
         new MobAttack("Tidal Surge", 0.8F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -14522625)
      );
      reg(
         "minecraft:skeleton",
         new MobAttack("Arrow Shot", 0.8F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -2236997),
         new MobAttack("Barrage", 0.65F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -2236997),
         new MobAttack("Snipe", 1.35F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -2236997)
      );
      reg(
         "minecraft:stray",
         new MobAttack("Frost Arrow", 0.8F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -7807489),
         new MobAttack("Blizzard Shot", 0.7F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -7807489),
         new MobAttack("Ice Lance", 1.3F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_FREEZE, MobAttack.Visual.FREEZE)
      );
      reg(
         "minecraft:wither_skeleton",
         new MobAttack("Bone Cleave", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -7846708),
         new MobAttack("Wither Touch", 0.75F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_CONFUSION, MobAttack.Visual.CONFUSE),
         new MobAttack("Dark Wave", 0.8F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -7846708)
      );
      reg(
         "minecraft:bogged",
         new MobAttack("Spore Arrow", 0.7F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -12268476),
         new MobAttack("Mushroom Burst", 0.65F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -12272828),
         new MobAttack("Toxic Volley", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -12268476)
      );
      reg(
         "minecraft:creeper",
         new MobAttack("Sizzle", 0.6F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -12272828),
         new MobAttack("Lunge", 0.9F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -12272828),
         new MobAttack("Detonation", 1.5F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -39390)
      );
      reg(
         "minecraft:spider",
         new MobAttack("Pounce", 0.75F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -12316416),
         new MobAttack("Venom Bite", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -12268476),
         new MobAttack("Web Snare", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_SLEEP, MobAttack.Visual.SLEEP)
      );
      reg(
         "minecraft:cave_spider",
         new MobAttack("Wall Pounce", 0.8F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -13434846),
         new MobAttack("Poison Fang", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_SLEEP, MobAttack.Visual.SLEEP),
         new MobAttack("Swarm", 0.65F, MobAttack.Targeting.MULTI, MobAttack.Visual.AOE_RING, -12268476)
      );
      reg(
         "minecraft:witch",
         new MobAttack("Splash Potion", 0.8F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -12268476),
         new MobAttack("Hex", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_CONFUSION, MobAttack.Visual.CONFUSE),
         new MobAttack("Brew Haste", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -3390260)
      );
      reg(
         "minecraft:enderman",
         new MobAttack("Teleport Slash", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -14548924),
         new MobAttack("Void Grasp", 0.75F, MobAttack.Targeting.MULTI, MobAttack.Visual.IMPACT, -14548924),
         new MobAttack("Phase Strike", 1.4F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -14548924)
      );
      reg(
         "minecraft:endermite",
         new MobAttack("Gnaw", 0.65F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -14548924),
         new MobAttack("Void Bite", 0.9F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -14548924),
         new MobAttack("Swarm Nip", 0.6F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -14548924)
      );
      reg(
         "minecraft:blaze",
         new MobAttack("Fireball", 0.9F, MobAttack.Targeting.SINGLE, MobAttack.Visual.FIRE, -39390),
         new MobAttack("Flame Burst", 0.75F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -39390),
         new MobAttack("Scorch", 1.3F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -39390)
      );
      reg(
         "minecraft:ghast",
         new MobAttack("Fireball", 1.0F, MobAttack.Targeting.SINGLE, MobAttack.Visual.FIRE, -48128),
         new MobAttack("Volley", 0.7F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -48128),
         new MobAttack("Wail", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_CONFUSION, MobAttack.Visual.CONFUSE)
      );
      reg(
         "minecraft:magma_cube",
         new MobAttack("Body Slam", 0.8F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -48128),
         new MobAttack("Lava Splash", 0.7F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -39390),
         new MobAttack("Melt", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Visual.FIRE, -39390)
      );
      reg(
         "minecraft:slime",
         new MobAttack("Bounce", 0.65F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -7812028),
         new MobAttack("Goo Toss", 0.8F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -7812028),
         new MobAttack("Split Slam", 1.1F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -7812028)
      );
      reg(
         "minecraft:pillager",
         new MobAttack("Crossbow Shot", 0.9F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -3399134),
         new MobAttack("Suppressing Fire", 0.65F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -3399134),
         new MobAttack("Battle Cry", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -3399134)
      );
      reg(
         "minecraft:vindicator",
         new MobAttack("Axe Strike", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -3399134),
         new MobAttack("Cleave", 0.8F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -3399134),
         new MobAttack("Furious Swing", 1.4F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -3399134)
      );
      reg(
         "minecraft:ravager",
         new MobAttack("Gore", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -3399134),
         new MobAttack("Ravage", 0.9F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -3399134),
         new MobAttack("War Cry", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -3399134)
      );
      reg(
         "minecraft:evoker",
         new MobAttack("Fang Summon", 1.0F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -7846708),
         new MobAttack("Vex Storm", 0.7F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -5570646),
         new MobAttack("Dark Ritual", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.HEAL_MOB, MobAttack.Visual.HEAL, -7846708)
      );
      reg(
         "minecraft:vex",
         new MobAttack("Phase Slash", 0.8F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -5570646),
         new MobAttack("Ghost Swipe", 0.7F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -5570646),
         new MobAttack("Spectral Cry", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -5570646)
      );
      reg(
         "minecraft:silverfish",
         new MobAttack("Swarm Bite", 0.65F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -7829385),
         new MobAttack("Gnaw", 0.9F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -7829385),
         new MobAttack("Infest", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_CONFUSION, MobAttack.Visual.CONFUSE)
      );
      reg(
         "minecraft:guardian",
         new MobAttack("Laser Beam", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -14522625),
         new MobAttack("Thorn Pulse", 0.8F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -14522625),
         new MobAttack("Surge", 1.3F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -188)
      );
      reg(
         "minecraft:elder_guardian",
         new MobAttack("Elder Beam", 1.3F, MobAttack.Targeting.SINGLE, MobAttack.Visual.THUNDER, -188),
         new MobAttack("Tidal Crush", 0.9F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -14522625),
         new MobAttack("Fatigue Curse", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_CONFUSION, MobAttack.Visual.WEAKEN)
      );
      reg(
         "minecraft:piglin",
         new MobAttack("Gold Rush", 0.8F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -13244),
         new MobAttack("Crossbow Fire", 1.0F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -13244),
         new MobAttack("War Grunt", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -13244)
      );
      reg(
         "minecraft:piglin_brute",
         new MobAttack("Brute Strike", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -13244),
         new MobAttack("Gold Cleave", 0.85F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -13244),
         new MobAttack("Rampage", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.BUFF, MobAttack.Visual.BUFF, -3399134)
      );
      reg(
         "minecraft:hoglin",
         new MobAttack("Tusk Gore", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -48128),
         new MobAttack("Charge", 0.8F, MobAttack.Targeting.MULTI, MobAttack.Visual.IMPACT, -48128),
         new MobAttack("Stampede", 0.9F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -48128)
      );
      reg(
         "minecraft:zoglin",
         new MobAttack("Undead Gore", 1.1F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -7846708),
         new MobAttack("Feral Charge", 0.8F, MobAttack.Targeting.MULTI, MobAttack.Visual.IMPACT, -7846708),
         new MobAttack("Horrify", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_CONFUSION, MobAttack.Visual.CONFUSE)
      );
      reg(
         "minecraft:shulker",
         new MobAttack("Bullet", 0.85F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -7846708),
         new MobAttack("Volley", 0.7F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -7846708),
         new MobAttack("Shell Clamp", 1.3F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_FREEZE, MobAttack.Visual.FREEZE)
      );
      reg(
         "minecraft:phantom",
         new MobAttack("Swooping Claw", 0.85F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -14548924),
         new MobAttack("Dive Bomb", 1.2F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -14548924),
         new MobAttack("Screech", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_CONFUSION, MobAttack.Visual.CONFUSE)
      );
      reg(
         "minecraft:breeze",
         new MobAttack("Wind Bolt", 0.9F, MobAttack.Targeting.SINGLE, MobAttack.Visual.IMPACT, -7811841),
         new MobAttack("Gale Force", 0.75F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -7811841),
         new MobAttack("Cyclone", 1.3F, MobAttack.Targeting.MULTI, MobAttack.Visual.SLASH, -7811841)
      );
      reg(
         "minecraft:creaking",
         new MobAttack("Petrify", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.APPLY_FREEZE, MobAttack.Visual.FREEZE),
         new MobAttack("Splinter Strike", 1.0F, MobAttack.Targeting.SINGLE, MobAttack.Visual.SLASH, -10075102),
         new MobAttack("Root Crush", 0.85F, MobAttack.Targeting.AOE, MobAttack.Visual.AOE_RING, -10075102)
      );
   }
}
