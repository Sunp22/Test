package com.pixelcite.turnbattle;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.registry.Registries;

public class MobProfile {
   private static final MobProfile.Profile ZOMBIE = new MobProfile.Profile(1.3F, 0.9F, 1.0F, 0.9F, 1.0F, 1.0F, "Tenacious: +30% HP, slightly weaker hits.");
   private static final MobProfile.Profile HUSK = new MobProfile.Profile(
      1.3F, 1.0F, 1.0F, 0.9F, 1.0F, 1.05F, "Dried Out: +30% HP, slightly more EXP. Desert-hardened."
   );
   private static final MobProfile.Profile DROWNED = new MobProfile.Profile(
      1.2F, 1.0F, 1.0F, 1.1F, 1.0F, 1.05F, "Waterlogged: +20% HP, extra armor. Fights in tight patterns."
   );
   private static final MobProfile.Profile SKELETON = new MobProfile.Profile(
      0.85F, 1.1F, 1.35F, 0.8F, 1.0F, 1.0F, "Marksman: Fast bullets. Lower HP but deals more damage."
   );
   private static final MobProfile.Profile STRAY = new MobProfile.Profile(
      0.85F, 1.1F, 1.4F, 0.8F, 1.0F, 1.05F, "Frost Arrows: Very fast bullets. Slight slowdown on hit."
   );
   private static final MobProfile.Profile WITHER_SKELETON = new MobProfile.Profile(
      1.2F, 1.25F, 1.2F, 1.1F, 0.9F, 1.3F, "Wither Touch: High damage, fast bullets, resists 10% of hits."
   );
   private static final MobProfile.Profile CREEPER = new MobProfile.Profile(
      0.9F, 1.4F, 1.0F, 0.7F, 1.0F, 1.1F, "Volatile: Explosive damage \u2014 bullets hit harder but mob is fragile."
   );
   private static final MobProfile.Profile SPIDER = new MobProfile.Profile(
      0.9F, 0.9F, 1.1F, 0.8F, 1.0F, 0.9F, "Wall Crawler: Quick erratic bullets. Low HP, low armor."
   );
   private static final MobProfile.Profile CAVE_SPIDER = new MobProfile.Profile(
      0.8F, 1.0F, 1.25F, 0.7F, 1.0F, 0.95F, "Venomous: Very fast small bullets. Extremely fragile."
   );
   private static final MobProfile.Profile ENDERMAN = new MobProfile.Profile(
      1.1F, 1.05F, 1.1F, 0.9F, 0.85F, 1.15F, "Phase Shift: Dodges 15% of damage. Teleporting bullet patterns."
   );
   private static final MobProfile.Profile BLAZE = new MobProfile.Profile(
      1.0F, 1.2F, 1.15F, 1.0F, 1.0F, 1.2F, "Inferno: Fire-charged bullets deal 20% more damage."
   );
   private static final MobProfile.Profile GHAST = new MobProfile.Profile(
      1.1F, 1.3F, 1.0F, 0.8F, 1.0F, 1.2F, "Floating Terror: High damage fireballs that are hard to dodge."
   );
   private static final MobProfile.Profile WITCH = new MobProfile.Profile(
      1.0F, 1.1F, 1.05F, 1.0F, 0.85F, 1.15F, "Potion Brewer: Resists 15% of hits. Unpredictable patterns."
   );
   private static final MobProfile.Profile PHANTOM = new MobProfile.Profile(
      0.85F, 1.0F, 1.2F, 0.7F, 1.0F, 1.0F, "Swooping: Fast angled dives. Fragile if you stay alert."
   );
   private static final MobProfile.Profile SLIME = new MobProfile.Profile(
      1.1F, 0.75F, 1.0F, 1.1F, 1.0F, 0.85F, "Gelatinous: Bouncy bullets, extra armor, less damage."
   );
   private static final MobProfile.Profile PILLAGER = new MobProfile.Profile(
      0.9F, 1.1F, 1.3F, 0.85F, 1.0F, 1.05F, "Crossbow Expert: Fast aimed shots, slightly fragile."
   );
   private static final MobProfile.Profile VINDICATOR = new MobProfile.Profile(
      1.1F, 1.3F, 1.0F, 1.0F, 1.0F, 1.15F, "Johnny: Very high melee damage and more HP."
   );
   private static final MobProfile.Profile RAVAGER = new MobProfile.Profile(
      1.4F, 1.35F, 1.0F, 1.2F, 0.9F, 1.4F, "Unstoppable: Enormous HP, high damage, resists 10%."
   );
   private static final MobProfile.Profile SILVERFISH = new MobProfile.Profile(
      0.55F, 0.7F, 1.1F, 0.6F, 1.0F, 0.65F, "Infestation: Very low stats but quick. Easy EXP."
   );
   private static final MobProfile.Profile ENDERMITE = new MobProfile.Profile(
      0.5F, 0.65F, 1.1F, 0.55F, 1.0F, 0.6F, "Parasitic: Weakest hostile. Fast tiny bullets."
   );
   private static final MobProfile.Profile IRON_GOLEM = new MobProfile.Profile(
      1.6F, 1.2F, 1.0F, 1.5F, 0.8F, 1.5F, "Iron Body: Massive HP & armor, resists 20% of all damage."
   );
   private static final MobProfile.Profile COW = new MobProfile.Profile(0.9F, 0.8F, 1.0F, 0.9F, 1.0F, 0.85F, "Thick Hide: Natural toughness, steady attacks.");
   private static final MobProfile.Profile MUSHROOM_COW = new MobProfile.Profile(
      0.9F, 0.85F, 1.0F, 0.95F, 0.95F, 0.9F, "Fungi Resilience: Slightly tougher than a cow. Spore-tinged bullets."
   );
   private static final MobProfile.Profile SHEEP = new MobProfile.Profile(
      0.85F, 0.75F, 1.0F, 0.85F, 1.0F, 0.8F, "Fleecy: Wool cushions blows slightly. Harmless looking, mildly aggressive."
   );
   private static final MobProfile.Profile PIG = new MobProfile.Profile(
      0.85F, 0.75F, 1.0F, 0.8F, 1.0F, 0.8F, "Rooter: Digs in. Medium-low stats across the board."
   );
   private static final MobProfile.Profile CHICKEN = new MobProfile.Profile(
      0.65F, 0.7F, 1.1F, 0.65F, 1.0F, 0.7F, "Flighty: Quick erratic movement. Very low HP."
   );
   private static final MobProfile.Profile RABBIT = new MobProfile.Profile(
      0.55F, 0.65F, 1.15F, 0.6F, 1.0F, 0.65F, "Quick Feet: Fast zigzag bullets. Fragile, but surprisingly nimble."
   );
   private static final MobProfile.Profile HORSE = new MobProfile.Profile(
      1.05F, 0.85F, 1.0F, 0.95F, 1.0F, 0.9F, "Sturdy Steed: Slightly above-average HP. Steady galloping patterns."
   );
   private static final MobProfile.Profile WOLF = new MobProfile.Profile(
      0.9F, 1.0F, 1.0F, 0.85F, 1.0F, 0.9F, "Pack Instinct: Loyal but dangerous. Balanced stats."
   );
   private static final MobProfile.Profile CAT = new MobProfile.Profile(
      0.7F, 0.85F, 1.15F, 0.7F, 1.0F, 0.75F, "Nine Lives: Agile and quick. Low HP, fast bullet patterns."
   );
   private static final MobProfile.Profile OCELOT = new MobProfile.Profile(
      0.7F, 0.8F, 1.2F, 0.65F, 1.0F, 0.75F, "Jungle Sprint: Very fast bullets. Fragile like a cat."
   );
   private static final MobProfile.Profile FOX = new MobProfile.Profile(
      0.75F, 0.9F, 1.1F, 0.7F, 1.0F, 0.8F, "Cunning: Unpredictable patterns. Evasive, low HP."
   );
   private static final MobProfile.Profile BEE = new MobProfile.Profile(
      0.7F, 0.9F, 1.1F, 0.65F, 1.0F, 0.75F, "Sting: Faster than it looks. Low HP, moderate damage."
   );
   private static final MobProfile.Profile POLAR_BEAR = new MobProfile.Profile(
      1.1F, 1.0F, 1.0F, 1.05F, 1.0F, 1.0F, "Arctic Fury: Protective instinct. More HP than most passives."
   );
   private static final MobProfile.Profile TURTLE = new MobProfile.Profile(
      1.15F, 0.65F, 1.0F, 1.2F, 0.9F, 0.85F, "Shell Defense: High armor, resists 10% of all damage."
   );
   private static final MobProfile.Profile GOAT = new MobProfile.Profile(
      0.85F, 1.0F, 1.0F, 0.8F, 1.0F, 0.85F, "Ramming Charge: Unpredictable charges. Average stats."
   );
   private static final MobProfile.Profile FROG = new MobProfile.Profile(0.65F, 0.7F, 1.05F, 0.6F, 1.0F, 0.7F, "Leap: Jumpy bullet patterns. Very fragile.");
   private static final MobProfile.Profile AXOLOTL = new MobProfile.Profile(
      0.75F, 0.8F, 1.0F, 0.75F, 1.0F, 0.8F, "Regeneration: Slowly recovers HP between turns. Low stats overall."
   );
   private static final MobProfile.Profile STRIDER = new MobProfile.Profile(
      0.9F, 0.8F, 1.0F, 0.85F, 1.0F, 0.85F, "Lava Walker: Heat-resistant. Moderate stats overall."
   );
   private static final MobProfile.Profile WITHER = new MobProfile.Profile(
      2.5F, 1.8F, 1.3F, 1.6F, 0.75F, 3.0F, "Wither Storm: 2.5\u00d7 HP. All bullets faster, resists 25%. Massive EXP."
   );
   private static final MobProfile.Profile ENDER_DRAGON = new MobProfile.Profile(
      3.0F, 2.0F, 1.25F, 1.5F, 0.7F, 4.0F, "Dragon Rage: 3\u00d7 HP. Devastating damage, resists 30%. Legendary EXP."
   );
   private static final MobProfile.Profile ELDER_GUARDIAN = new MobProfile.Profile(
      1.8F, 1.5F, 1.1F, 1.4F, 0.8F, 2.0F, "Mining Fatigue: 1.8\u00d7 HP. Curses the attacker. High reward."
   );
   private static final MobProfile.Profile GENERIC_HOSTILE = new MobProfile.Profile(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, "");
   private static final MobProfile.Profile GENERIC_PASSIVE = new MobProfile.Profile(
      0.8F, 0.8F, 1.0F, 0.85F, 1.0F, 0.8F, "Peaceful Nature: Lower stats than hostile mobs."
   );
   private static final MobProfile.Profile GENERIC_NEUTRAL = new MobProfile.Profile(0.95F, 0.95F, 1.0F, 0.95F, 1.0F, 0.95F, "");
   private static final MobProfile.Profile MIRROR = new MobProfile.Profile(
      1.0F, 1.1F, 1.1F, 1.2F, 1.0F, 3.0F, "Your reflection: it knows every move you know - your slashes, your PSI. It can't be spared."
   );
   private static final Map<HumanEnemies.Trait, MobProfile.Profile> HUMANS = new EnumMap<>(HumanEnemies.Trait.class);

   private static MobProfile.Profile human(HumanEnemies.Trait t) {
      return HUMANS.computeIfAbsent(t, k -> new MobProfile.Profile(1.4F, 1.1F, 1.0F, 1.0F, 1.0F, 2.0F, k.passive));
   }

   public static MobProfile.Profile getProfile(LivingEntity entity) {
      String typeId = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
      if (typeId.equals("wither")) {
         return WITHER;
      } else if (entity instanceof EnderDragonEntity) {
         return ENDER_DRAGON;
      } else if (typeId.equals("elder_guardian")) {
         return ELDER_GUARDIAN;
      } else if (entity instanceof HuskEntity) {
         return HUSK;
      } else if (entity instanceof DrownedEntity) {
         return DROWNED;
      } else if (entity instanceof ZombieEntity) {
         return ZOMBIE;
      } else if (entity instanceof StrayEntity) {
         return STRAY;
      } else if (entity instanceof WitherSkeletonEntity) {
         return WITHER_SKELETON;
      } else if (entity instanceof SkeletonEntity) {
         return SKELETON;
      } else if (entity instanceof CreeperEntity) {
         return CREEPER;
      } else if (entity instanceof CaveSpiderEntity) {
         return CAVE_SPIDER;
      } else if (entity instanceof SpiderEntity) {
         return SPIDER;
      } else if (entity instanceof EndermanEntity) {
         return ENDERMAN;
      } else if (entity instanceof BlazeEntity) {
         return BLAZE;
      } else if (entity instanceof GhastEntity) {
         return GHAST;
      } else if (entity instanceof WitchEntity) {
         return WITCH;
      } else if (entity instanceof PhantomEntity) {
         return PHANTOM;
      } else if (entity instanceof SlimeEntity) {
         return SLIME;
      } else if (entity instanceof PillagerEntity) {
         return PILLAGER;
      } else if (entity instanceof VindicatorEntity) {
         return VINDICATOR;
      } else if (entity instanceof RavagerEntity) {
         return RAVAGER;
      } else if (entity instanceof SilverfishEntity) {
         return SILVERFISH;
      } else if (entity instanceof EndermiteEntity) {
         return ENDERMITE;
      } else if (entity instanceof HumanEnemyEntity h) {
         return human(h.trait());
      } else if (entity instanceof DarknerEntity d) {
         return d.profile();
      } else if (entity instanceof MirrorCloneEntity) {
         return MIRROR;
      } else if (entity instanceof IronGolemEntity) {
         return IRON_GOLEM;
      } else if (entity instanceof MooshroomEntity) {
         return MUSHROOM_COW;
      } else if (entity instanceof CowEntity) {
         return COW;
      } else if (entity instanceof SheepEntity) {
         return SHEEP;
      } else if (entity instanceof PigEntity) {
         return PIG;
      } else if (entity instanceof ChickenEntity) {
         return CHICKEN;
      } else if (entity instanceof RabbitEntity) {
         return RABBIT;
      } else if (entity instanceof AbstractHorseEntity) {
         return HORSE;
      } else if (entity instanceof WolfEntity) {
         return WOLF;
      } else if (entity instanceof CatEntity) {
         return CAT;
      } else if (entity instanceof OcelotEntity) {
         return OCELOT;
      } else if (entity instanceof FoxEntity) {
         return FOX;
      } else if (entity instanceof BeeEntity) {
         return BEE;
      } else if (entity instanceof PolarBearEntity) {
         return POLAR_BEAR;
      } else if (entity instanceof TurtleEntity) {
         return TURTLE;
      } else if (entity instanceof GoatEntity) {
         return GOAT;
      } else if (entity instanceof FrogEntity) {
         return FROG;
      } else if (entity instanceof AxolotlEntity) {
         return AXOLOTL;
      } else if (entity instanceof StriderEntity) {
         return STRIDER;
      } else if (entity instanceof PassiveEntity) {
         return GENERIC_PASSIVE;
      } else {
         return entity instanceof AnimalEntity ? GENERIC_PASSIVE : GENERIC_HOSTILE;
      }
   }

   public static String getPassive(EntityType<?> type) {
      String id = Registries.ENTITY_TYPE.getId(type).getPath();

      MobProfile.Profile p = switch (id) {
         case "zombie" -> ZOMBIE;
         case "husk" -> HUSK;
         case "drowned" -> DROWNED;
         case "skeleton" -> SKELETON;
         case "stray" -> STRAY;
         case "wither_skeleton" -> WITHER_SKELETON;
         case "creeper" -> CREEPER;
         case "spider" -> SPIDER;
         case "cave_spider" -> CAVE_SPIDER;
         case "enderman" -> ENDERMAN;
         case "blaze" -> BLAZE;
         case "ghast" -> GHAST;
         case "witch" -> WITCH;
         case "phantom" -> PHANTOM;
         case "slime" -> SLIME;
         case "pillager" -> PILLAGER;
         case "vindicator" -> VINDICATOR;
         case "ravager" -> RAVAGER;
         case "silverfish" -> SILVERFISH;
         case "endermite" -> ENDERMITE;
         case "iron_golem" -> IRON_GOLEM;
         case "mooshroom" -> MUSHROOM_COW;
         case "cow" -> COW;
         case "sheep" -> SHEEP;
         case "pig" -> PIG;
         case "chicken" -> CHICKEN;
         case "rabbit" -> RABBIT;
         case "horse", "donkey", "mule", "skeleton_horse", "zombie_horse" -> HORSE;
         case "wolf" -> WOLF;
         case "cat" -> CAT;
         case "ocelot" -> OCELOT;
         case "fox" -> FOX;
         case "bee" -> BEE;
         case "polar_bear" -> POLAR_BEAR;
         case "turtle" -> TURTLE;
         case "goat" -> GOAT;
         case "frog" -> FROG;
         case "axolotl" -> AXOLOTL;
         case "strider" -> STRIDER;
         case "wither" -> WITHER;
         case "elder_guardian" -> ELDER_GUARDIAN;
         default -> null;
      };
      if (p == null && id.equals("ender_dragon")) {
         p = ENDER_DRAGON;
      }

      if (p == null) {
         HumanEnemies.Trait ht = HumanEnemies.traitOf(type);
         if (ht != null) {
            p = human(ht);
         }
      }

      return p != null && p.passive() != null && !p.passive().isEmpty() ? p.passive() : "";
   }

   public static boolean isBoss(LivingEntity entity) {
      String tid = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
      return tid.equals("wither") || entity instanceof EnderDragonEntity || tid.equals("elder_guardian") || tid.equals("warden");
   }

   public static record Profile(float hpMult, float atkMult, float bulletSpeedMult, float armorMult, float damageTakenMult, float expMult, String passive) {
   }
}
