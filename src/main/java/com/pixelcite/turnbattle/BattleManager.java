package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.GlowSquidEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SalmonEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootContextParameterSet.Builder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class BattleManager {
   public static final Map<UUID, BattleState> activeBattles = new HashMap<>();
   public static final Set<Integer> frozenEntityIds = new HashSet<>();
   public static final Set<UUID> playersInStoryDialogue = new HashSet<>();
   public static final Map<UUID, Long> storyDialogueEnteredTick = new HashMap<>();
   private static final Set<UUID> secondWindUsed = ConcurrentHashMap.newKeySet();
   private static final Map<Integer, BattleState> battleByMob = new HashMap<>();
   private static volatile boolean applyBattleDamage = false;
   private static final Random RNG = new Random();
   public static final Map<UUID, Float> TARGET_HEALTH = new ConcurrentHashMap<>();
   public static volatile boolean IS_ROLLING = false;
   private static final Map<UUID, Integer> patienceRegenTicks = new HashMap<>();
   private static final int PATIENCE_REGEN_INTERVAL = 100;
   public static final Map<UUID, Long> battleCooldowns = new HashMap<>();
   public static final Map<UUID, Long> runInvincibleUntil = new HashMap<>();
   private static final long COOLDOWN_TICKS = 240L;
   private static final double TEAM_PVP_PULL_RANGE = 15.0;
   private static final float ROLL_SPEED = 0.5F;
   private static final Map<UUID, Float> lastSentTarget = new ConcurrentHashMap<>();
   private static final Set<UUID> downedPlayers = ConcurrentHashMap.newKeySet();
   private static final int FIRST_STRIKE_DELAY = 52;
   private static int VIRTUAL_EXTRA_ID_COUNTER = -2147483647;
   static final float MULTI_HIT_FALLOFF = 0.85F;
   private static float multiHitLast = -1.0F;
   private static final Identifier ROGUE_HP_MOD = Identifier.of("turnbattle", "d4e5f6a7-0004-0000-0000-000000000001");
   private static final Map<Integer, Set<UUID>> pendingEnemyResults = new HashMap<>();
   private static final Map<Integer, Long> pendingEnemyResultsStartTick = new HashMap<>();
   private static final long ENEMY_RESULT_TIMEOUT_TICKS = 270L;
   static boolean lastActionQueued = false;
   private static final int LOG_ACK_TIMEOUT_TICKS = 1200;
   private static final int BASH_PHASE_TIMEOUT_TICKS = 240;
   private static final String[] WAIT_WORDS = new String[]{
      "patiently",
      "impatiently",
      "nervously",
      "quietly",
      "menacingly",
      "curiously",
      "anxiously",
      "calmly",
      "eagerly",
      "warily",
      "restlessly",
      "silently",
      "angrily",
      "smugly",
      "cautiously",
      "sleepily",
      "hungrily",
      "intently",
      "stubbornly",
      "defiantly",
      "awkwardly",
      "expectantly",
      "grumpily",
      "politely",
      "suspiciously",
      "boredly"
   };
   private static final float HIT_LAND_AT = 0.8F;
   private static final int BASH_LAND_TICKS = 10;

   public static boolean isInBattle(UUID playerUuid) {
      return activeBattles.containsKey(playerUuid);
   }

   public static void freezeMob(MobEntity mob) {
      frozenEntityIds.add(mob.getId());
      mob.setAiDisabled(true);
   }

   public static void unfreezeMob(MobEntity mob) {
      frozenEntityIds.remove(mob.getId());
      wakeMob(mob);
   }

   public static boolean isApplyingBattleDamage() {
      return applyBattleDamage;
   }

   public static boolean isMobInBattle(int mobId) {
      return battleByMob.containsKey(mobId);
   }

   public static BattleState getBattleStateByMobId(int mobId) {
      return battleByMob.get(mobId);
   }

   public static void broadcastLifeLost(BattleState state, String message, MinecraftServer server) {
      if (server != null) {
         state.pushLog(message);

         for (UUID pid : state.participantOrder) {
            if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               ServerPlayerEntity player = server.getPlayerManager().getPlayer(pid);
               if (player != null) {
                  BattleNetworking.sendStateUpdateWithLives(player, state);
               }
            }
         }
      }
   }

   private static void settleRolling(MinecraftServer server, UUID pid) {
      lastSentTarget.remove(pid);
      if (TurnBattleConfig.get().rollingHealth) {
         Float target = TARGET_HEALTH.get(pid);
         ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pid);
         if (target != null && pp != null && pp.isAlive() && !(target <= pp.getHealth())) {
            IS_ROLLING = true;

            try {
               pp.setHealth(Math.max(0.5F, Math.min(pp.getMaxHealth(), target)));
            } finally {
               IS_ROLLING = false;
            }
         }
      }
   }

   public static void tickBattleTicks(MinecraftServer server) {
      Set battles = Collections.newSetFromMap(new IdentityHashMap<>());
      battles.addAll(activeBattles.values());

      for (BattleState st : battles) {
         st.battleTick++;
      }

      for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
         UUID uuid = player.getUuid();
         BattleState state = activeBattles.get(uuid);
         if (state != null && player.isAlive() && state.offhandGimmicks.getOrDefault(uuid, OffhandGimmick.NONE) == OffhandGimmick.THORN_RING) {
            int drainTick = state.thornDrainTicks.getOrDefault(uuid, 0) + 1;
            if (drainTick < 5) {
               state.thornDrainTicks.put(uuid, drainTick);
            } else {
               state.thornDrainTicks.put(uuid, 0);
               float cur = TARGET_HEALTH.getOrDefault(uuid, player.getHealth());
               float floor = player.getMaxHealth() / 3.0F;
               if (!(cur <= floor)) {
                  float next = Math.max(floor, cur - 1.0F);
                  TARGET_HEALTH.put(uuid, next);
                  state.participantHp.put(uuid, next);
                  IS_ROLLING = true;

                  try {
                     player.setHealth(Math.max(0.5F, next));
                  } finally {
                     IS_ROLLING = false;
                  }

                  PacketByteBuf hpBuf = PacketByteBufs.create();
                  hpBuf.writeFloat(next);
                  Net121.sendToClient(player, TurnBattleMod.HP_SYNC, hpBuf);
               }
            }
         }
      }

      for (BattleState state : new HashSet<>(activeBattles.values())) {
         ServerPlayerEntity owner = server.getPlayerManager().getPlayer(state.playerUuid);
         if (owner != null) {
            for (UUID slot : new UUID[]{BattleState.PARTY_MEMBER_UUID, BattleState.PARTY_MEMBER_UUID_2}) {
               if (state.participantOrder.contains(slot) && state.offhandGimmicks.getOrDefault(slot, OffhandGimmick.NONE) == OffhandGimmick.THORN_RING) {
                  int drainTick = state.thornDrainTicks.getOrDefault(slot, 0) + 1;
                  if (drainTick < 5) {
                     state.thornDrainTicks.put(slot, drainTick);
                  } else {
                     state.thornDrainTicks.put(slot, 0);
                     float cur = state.participantHp.getOrDefault(slot, 0.0F);
                     if (!(cur <= 0.0F)) {
                        float maxHp = state.participantMaxHp.getOrDefault(slot, 1.0F);
                        float floor = maxHp / 3.0F;
                        if (!(cur <= floor)) {
                           float next2 = Math.max(floor, cur - 1.0F);
                           state.participantHp.put(slot, next2);
                           state.targetParticipantHp.put(slot, next2);
                           BattleNetworking.broadcastParticipantsOnly(state, server.getPlayerManager());
                        }
                     }
                  }
               }
            }
         }
      }

      for (ServerPlayerEntity playerx : server.getPlayerManager().getPlayerList()) {
         UUID uuid = playerx.getUuid();
         BattleState statex = activeBattles.get(uuid);
         if (statex != null
            && playerx.isAlive()
            && statex.offhandGimmicks.getOrDefault(uuid, OffhandGimmick.NONE) == OffhandGimmick.PSI_PROCESSOR
            && statex.phase != BattleState.Phase.AWAITING_ENEMY_RESULT) {
            int psiTick = statex.thornDrainTicks.getOrDefault(uuid, 0) + 1;
            if (psiTick < 10) {
               statex.thornDrainTicks.put(uuid, psiTick);
            } else {
               statex.thornDrainTicks.put(uuid, 0);
               float tp = statex.getTp(uuid);
               if (!(tp <= 0.0F)) {
                  statex.addTp(uuid, -1.0F);
                  BattleNetworking.broadcastSharedTp(statex, playerx.getServer().getPlayerManager());
               }
            }
         }
      }

      List knockedRecruits = new ArrayList<>();

      for (BattleState statex : battles) {
         if (!statex.targetParticipantHp.isEmpty()) {
            boolean changed = false;
            Iterator it = statex.targetParticipantHp.entrySet().iterator();

            while (it.hasNext()) {
               Entry entry = it.next();
               UUID slotx = entry.getKey();
               float target = entry.getValue();
               float cur = statex.participantHp.getOrDefault(slotx, target);
               if (cur == target) {
                  it.remove();
               } else if (cur <= 0.0F && target > 0.0F) {
                  float maxHpSlot = statex.participantMaxHp.getOrDefault(slotx, 20.0F);
                  float reviveHp = Math.max(1.0F, (float)Math.round(maxHpSlot / 6.0F));
                  statex.participantHp.put(slotx, reviveHp);
                  entry.setValue(reviveHp);
                  changed = true;
               } else {
                  float next = RollingHp.toward(cur, target, statex.participantMaxHp.getOrDefault(slotx, 20.0F));
                  statex.participantHp.put(slotx, next);
                  if (cur > 0.0F && next <= 0.0F) {
                     knockedRecruits.add(new Object[]{statex, slotx});
                  }

                  if (next == target) {
                     changed = true;
                  }
               }
            }

            if (changed) {
               BattleNetworking.broadcastParticipantsOnly(statex, server.getPlayerManager());
            }
         }
      }

      for (Object[] kr : knockedRecruits) {
         BattleState ks = (BattleState)kr[0];
         UUID kslot = (UUID)kr[1];
         if (activeBattles.containsValue(ks)) {
            PlayerManager kpm = server.getPlayerManager();
            String knockedName = ks.participantNames.getOrDefault(kslot, "Ally");
            if (isPartyWiped(ks, kpm)) {
               endBattleFromPartyWipe(ks, server, kpm);
            } else {
               BattleNetworking.broadcastStateUpdate(ks, kpm, knockedName + " has been downed!");
            }
         }
      }
   }

   public static void tickEnemyResultTimeout(MinecraftServer server) {
      if (!pendingEnemyResultsStartTick.isEmpty()) {
         long now = server.getOverworld().getTime();

         for (Entry entry : new ArrayList<>(pendingEnemyResultsStartTick.entrySet())) {
            int mobId = entry.getKey();
            long startTick = entry.getValue();
            if (now - startTick >= 270L) {
               BattleState stuckState = null;

               for (BattleState st : activeBattles.values()) {
                  if (st.entityNetworkId == mobId && st.phase == BattleState.Phase.AWAITING_ENEMY_RESULT) {
                     stuckState = st;
                     break;
                  }
               }

               pendingEnemyResultsStartTick.remove(mobId);
               if (stuckState == null) {
                  pendingEnemyResults.remove(mobId);
               } else {
                  List realPlayers = stuckState.participantOrder.stream().filter(id -> !isRecruitSlot(id)).toList();
                  Set reported = pendingEnemyResults.getOrDefault(mobId, new HashSet<>());

                  for (UUID pid : realPlayers) {
                     if (!reported.contains(pid)) {
                        ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pid);
                        if (pp != null) {
                           processEnemyTurnResult(pp, 0, 0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void tickRollingHealth(MinecraftServer server) {
      for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
         UUID uuid = player.getUuid();
         if (activeBattles.containsKey(uuid) && player.isAlive()) {
            if (!TurnBattleConfig.get().rollingHealth) {
               Float instantTarget = TARGET_HEALTH.get(uuid);
               if (instantTarget != null && instantTarget <= 0.0F && !downedPlayers.contains(uuid)) {
                  downedPlayers.add(uuid);
                  handlePlayerKO(player, uuid, server);
               } else if (instantTarget != null && instantTarget > 0.0F && downedPlayers.remove(uuid)) {
                  float reviveHp = Math.max(1.0F, (float)Math.round(player.getMaxHealth() / 6.0F));
                  TARGET_HEALTH.put(uuid, reviveHp);
                  IS_ROLLING = true;

                  try {
                     player.setHealth(reviveHp);
                  } finally {
                     IS_ROLLING = false;
                  }

                  BattleState reviveState = activeBattles.get(uuid);
                  if (reviveState != null) {
                     BattleNetworking.broadcastStateUpdate(reviveState, server.getPlayerManager(), getPlayerDisplayName(player) + " gets back up!");
                  }
               }
            } else {
               Float target = TARGET_HEALTH.get(uuid);
               if (target != null) {
                  if (target > 0.0F && downedPlayers.remove(uuid)) {
                     float reviveHp = Math.max(1.0F, (float)Math.round(player.getMaxHealth() / 6.0F));
                     TARGET_HEALTH.put(uuid, reviveHp);
                     IS_ROLLING = true;

                     try {
                        player.setHealth(reviveHp);
                     } finally {
                        IS_ROLLING = false;
                     }

                     BattleState reviveState = activeBattles.get(uuid);
                     if (reviveState != null) {
                        BattleNetworking.broadcastStateUpdate(reviveState, server.getPlayerManager(), getPlayerDisplayName(player) + " gets back up!");
                     }
                  } else if (!(target <= 0.0F) || !downedPlayers.contains(uuid)) {
                     float current = player.getHealth();
                     if (current != target) {
                        float next = RollingHp.toward(current, target, player.getMaxHealth());
                        float clamped = next <= 0.0F ? 0.5F : next;
                        IS_ROLLING = true;

                        try {
                           player.setHealth(clamped);
                        } finally {
                           IS_ROLLING = false;
                        }

                        Float sent = lastSentTarget.get(uuid);
                        if (sent == null || Math.abs(sent - target) > 0.001F) {
                           lastSentTarget.put(uuid, target);
                           PacketByteBuf hpBuf = PacketByteBufs.create();
                           hpBuf.writeFloat(target);
                           Net121.sendToClient(player, TurnBattleMod.HP_SYNC, hpBuf);
                        }

                        if (next <= 0.0F && !downedPlayers.contains(uuid)) {
                           downedPlayers.add(uuid);
                           handlePlayerKO(player, uuid, server);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean isPlayerDowned(UUID uuid) {
      return downedPlayers.contains(uuid);
   }

   static boolean isRecruitSlot(UUID uuid) {
      return uuid.equals(BattleState.PARTY_MEMBER_UUID) || uuid.equals(BattleState.PARTY_MEMBER_UUID_2);
   }

   private static float getMaxHpFor(BattleState state, UUID uuid, PlayerManager pm) {
      if (isRecruitSlot(uuid)) {
         return state.participantMaxHp.getOrDefault(uuid, 20.0F);
      } else {
         ServerPlayerEntity p = pm.getPlayer(uuid);
         return p != null ? p.getMaxHealth() : 20.0F;
      }
   }

   private static float getBattleHpFor(BattleState state, UUID uuid, PlayerManager pm) {
      return isRecruitSlot(uuid)
         ? state.participantHp.getOrDefault(uuid, getMaxHpFor(state, uuid, pm))
         : TARGET_HEALTH.getOrDefault(uuid, getMaxHpFor(state, uuid, pm));
   }

   private static void setBattleHpFor(BattleState state, UUID uuid, float value, PlayerManager pm) {
      if (isRecruitSlot(uuid)) {
         state.participantHp.put(uuid, value);
         state.targetParticipantHp.put(uuid, value);
      } else {
         TARGET_HEALTH.put(uuid, value);
         if (value <= 0.0F) {
            downedPlayers.add(uuid);
         } else {
            downedPlayers.remove(uuid);
         }

         ServerPlayerEntity p = pm.getPlayer(uuid);
         if (p != null) {
            float safeVisual = value > 0.0F ? Math.min(p.getMaxHealth(), value) : 1.0F;
            IS_ROLLING = true;

            try {
               p.setHealth(Math.max(0.5F, safeVisual));
            } finally {
               IS_ROLLING = false;
            }

            PacketByteBuf hb = PacketByteBufs.create();
            hb.writeFloat(value);
            Net121.sendToClient(p, TurnBattleMod.HP_SYNC, hb);
         }
      }
   }

   static boolean isDowned(BattleState state, UUID uuid, PlayerManager pm) {
      return TurnBattleConfig.get().rollingHealth && !state.isPvp && !isRecruitSlot(uuid)
         ? downedPlayers.contains(uuid)
         : getBattleHpFor(state, uuid, pm) <= 0.0F;
   }

   static boolean isMortal(BattleState state, UUID uuid, PlayerManager pm) {
      if (!TurnBattleConfig.get().rollingHealth || state.isPvp) {
         return false;
      } else if (isRecruitSlot(uuid)) {
         Float tgt = state.targetParticipantHp.get(uuid);
         return tgt != null && tgt <= 0.0F && state.participantHp.getOrDefault(uuid, 0.0F) > 0.0F;
      } else {
         return getBattleHpFor(state, uuid, pm) <= 0.0F && !downedPlayers.contains(uuid);
      }
   }

   private static float rolledHpFor(BattleState state, UUID uuid, PlayerManager pm) {
      if (isRecruitSlot(uuid)) {
         return state.participantHp.getOrDefault(uuid, 0.0F);
      } else {
         ServerPlayerEntity p = pm.getPlayer(uuid);
         return p != null ? p.getHealth() : getBattleHpFor(state, uuid, pm);
      }
   }

   private static float rollTargetFor(BattleState state, UUID uuid, PlayerManager pm) {
      return isRecruitSlot(uuid) ? state.targetParticipantHp.getOrDefault(uuid, state.participantHp.getOrDefault(uuid, 0.0F)) : getBattleHpFor(state, uuid, pm);
   }

   private static void setRollTargetFor(BattleState state, UUID uuid, float value) {
      if (isRecruitSlot(uuid)) {
         state.targetParticipantHp.put(uuid, value);
      } else {
         TARGET_HEALTH.put(uuid, value);
         state.participantHp.put(uuid, value);
      }
   }

   static <K> boolean staysAsleep(Map<K, Integer> sleepMap, K who) {
      int slept = Math.max(0, sleepMap.getOrDefault(who, 1) - 1);
      int stayChance = 100 - 35 * slept;
      if (RNG.nextInt(100) < stayChance) {
         sleepMap.put(who, slept + 2);
         return true;
      } else {
         return false;
      }
   }

   static int darkDollarsFor(BattleState state, MinecraftServer server) {
      ServerPlayerEntity p = server.getPlayerManager().getPlayer(state.playerUuid);
      if (p != null && DarkFountainManager.isDarkWorld(p.getWorld().getRegistryKey())) {
         ServerWorld w = p.getServerWorld();
         float total = 0.0F;

         for (int eid : state.enemyMaxHpMap.keySet()) {
            int lv = w.getEntityById(eid) instanceof LivingEntity le ? MobLevelManager.getMobLevel(le) : Math.max(1, state.playerLevelAtStart());
            total += 2.0F + (float)lv * 1.5F;
         }

         float tpFrac = state.sharedTpMax > 0.0F ? Math.max(0.0F, Math.min(1.0F, state.sharedTp / state.sharedTpMax)) : 0.0F;
         return Math.max(1, Math.round(total * (1.0F + tpFrac)));
      } else {
         return 0;
      }
   }

   static float heartLocketCut(ServerPlayerEntity anyPlayer, BattleState state, UUID who, float dmg) {
      if (anyPlayer != null && who != null && !(dmg <= 0.0F)) {
         BattleData.PlayerProgress prog = BattleData.get(anyPlayer.getServer()).getOrCreate(isRecruitSlot(who) ? state.playerUuid : who);
         return resolveActorGimmick(anyPlayer, state, who, prog) == OffhandGimmick.HEART_LOCKET ? dmg * 0.5F : dmg;
      } else {
         return dmg;
      }
   }

   static void applyDownableDamage(BattleState state, UUID uuid, float dmg, PlayerManager pm) {
      float cur = getBattleHpFor(state, uuid, pm);
      if (!(cur <= 0.0F)) {
         float max = getMaxHpFor(state, uuid, pm);
         float next = cur - dmg;
         if (next <= 0.0F) {
            next = (float)(-Math.round(max / 2.0F));
         }

         setBattleHpFor(state, uuid, next, pm);
      }
   }

   static void applyDownableHeal(BattleState state, UUID uuid, float amount, PlayerManager pm) {
      if (TurnBattleConfig.get().rollingHealth && !state.isPvp && !isDowned(state, uuid, pm)) {
         float maxR = getMaxHpFor(state, uuid, pm);
         setBattleHpFor(state, uuid, Math.min(maxR, rolledHpFor(state, uuid, pm) + amount), pm);
      } else {
         float max = getMaxHpFor(state, uuid, pm);
         float cur = getBattleHpFor(state, uuid, pm);
         float next = Math.min(max, cur + amount);
         if (cur <= 0.0F && next > 0.0F) {
            next = Math.max(1.0F, (float)Math.round(max / 6.0F));
         }

         setBattleHpFor(state, uuid, next, pm);
      }
   }

   static boolean tickDownedRegenAndCheckRevive(BattleState state, UUID uuid, PlayerManager pm) {
      float max = getMaxHpFor(state, uuid, pm);
      boolean before = isDowned(state, uuid, pm);
      float hpBefore = getBattleHpFor(state, uuid, pm);
      applyDownableHeal(state, uuid, (float)Math.ceil((double)max / 8.0), pm);
      float hpAfter = getBattleHpFor(state, uuid, pm);
      float actualHealed = hpAfter - hpBefore;
      if (actualHealed > 0.0F) {
         BattleNetworking.broadcastHeal(state, pm, uuid, actualHealed);
      }

      return before && !isDowned(state, uuid, pm);
   }

   static boolean isPartyWiped(BattleState state, PlayerManager pm) {
      if (state.participantOrder.isEmpty()) {
         return false;
      } else {
         for (UUID id : state.participantOrder) {
            if (!isDowned(state, id, pm)) {
               return false;
            }
         }

         return true;
      }
   }

   static boolean isTeamWiped(BattleState state, UUID uuid, PlayerManager pm) {
      for (UUID id : state.teammatesOf(uuid)) {
         if (!isDowned(state, id, pm)) {
            return false;
         }
      }

      return true;
   }

   static UUID primaryEnemyOf(BattleState state, UUID actor, PlayerManager pm) {
      UUID chosen = state.pvpChosenTarget.get(actor);
      if (chosen != null && state.isEnemyPair(actor, chosen) && !isDowned(state, chosen, pm)) {
         return chosen;
      } else {
         List enemies = state.enemiesOf(actor);

         for (UUID pid : enemies) {
            if (!isDowned(state, pid, pm)) {
               return pid;
            }
         }

         return enemies.isEmpty() ? null : enemies.get(0);
      }
   }

   static UUID resolveOffensiveTargetUuid(BattleState state, UUID actor, int targetIndex) {
      List opponents = state.enemiesOf(actor);
      return targetIndex >= 0 && targetIndex < opponents.size() ? opponents.get(targetIndex) : null;
   }

   static void setPvpTargetFromIndex(BattleState state, UUID actor, int targetIndex, PlayerManager pm) {
      if (state.isPvp) {
         List opponents = state.enemiesOf(actor);
         if (targetIndex >= 0 && targetIndex < opponents.size()) {
            UUID chosen = opponents.get(targetIndex);
            if (!isDowned(state, chosen, pm)) {
               state.pvpChosenTarget.put(actor, chosen);
            }
         }
      }
   }

   private static void endBattleFromPartyWipe(BattleState state, MinecraftServer server, PlayerManager pm) {
      BattleNetworking.broadcastStateUpdate(state, pm, "The party has fallen!");
      List everyone = new ArrayList<>(state.participantOrder);
      endBattle(state, server, "lose");
      if (!state.magicant && !state.depthsRun) {
         for (UUID pid : everyone) {
            if (!isRecruitSlot(pid)) {
               ServerPlayerEntity pp = pm.getPlayer(pid);
               if (pp != null) {
                  TARGET_HEALTH.remove(pid);
                  applyBattleDamage = true;

                  try {
                     pp.damage(pp.getDamageSources().magic(), pp.getMaxHealth() * 100.0F);
                  } finally {
                     applyBattleDamage = false;
                  }

                  ensureRogueFall(pp);
               }
            }
         }
      }
   }

   private static void handlePlayerKO(ServerPlayerEntity player, UUID uuid, MinecraftServer server) {
      BattleState stateKO = activeBattles.get(uuid);
      if (stateKO != null && stateKO.isPvp && !stateKO.isTeamPvp) {
         TARGET_HEALTH.remove(uuid);
         String pvpOutcome = uuid.equals(stateKO.playerUuid) ? "lose" : "win";
         endBattle(stateKO, server, pvpOutcome);
      } else {
         if (stateKO == null || !stateKO.isPvp) {
            RogueliteData rogueDataSW = RogueliteData.get(server);
            RogueliteData.PlayerRogueData rdSW = rogueDataSW.players.get(uuid);
            if (rdSW != null && rdSW.inRun && rdSW.uniqueCards.contains("SECOND_WIND") && !secondWindUsed.contains(uuid)) {
               secondWindUsed.add(uuid);
               float reviveHp = Math.max(1.0F, player.getMaxHealth() * 0.25F);
               if (stateKO != null) {
                  setBattleHpFor(stateKO, uuid, reviveHp, server.getPlayerManager());
               } else {
                  TARGET_HEALTH.put(uuid, reviveHp);
                  IS_ROLLING = true;

                  try {
                     player.setHealth(reviveHp);
                  } finally {
                     IS_ROLLING = false;
                  }
               }

               if (stateKO != null) {
                  BattleNetworking.broadcastStateUpdate(stateKO, server.getPlayerManager(), getPlayerDisplayName(player) + " activated SECOND WIND!");
               }

               return;
            }
         }

         BattleState state = activeBattles.get(uuid);
         if (state != null) {
            PlayerManager pm = server.getPlayerManager();
            float maxHp = player.getMaxHealth();
            setBattleHpFor(state, uuid, (float)(-Math.round(maxHp / 2.0F)), pm);
            String knockedName = state.participantNames.getOrDefault(uuid, getPlayerDisplayName(player));
            if (state.isTeamPvp) {
               if (isTeamWiped(state, uuid, pm)) {
                  BattleNetworking.broadcastStateUpdate(state, pm, knockedName + " has fallen... their team is defeated!");
                  boolean uuidsTeamIsPlayerUuidsTeam = state.isAllyPair(uuid, state.playerUuid);
                  endBattle(state, server, uuidsTeamIsPlayerUuidsTeam ? "lose" : "win");
               } else {
                  BattleNetworking.broadcastStateUpdate(state, pm, knockedName + " has been downed!");
               }
            } else {
               if (isPartyWiped(state, pm)) {
                  BattleNetworking.broadcastStateUpdate(state, pm, knockedName + " has fallen... the party is defeated!");
                  List everyone = new ArrayList<>(state.participantOrder);
                  endBattle(state, server, "lose");
                  if (!state.magicant && !state.depthsRun) {
                     for (UUID pid : everyone) {
                        if (!isRecruitSlot(pid)) {
                           ServerPlayerEntity pp = pm.getPlayer(pid);
                           if (pp != null) {
                              TARGET_HEALTH.remove(pid);
                              applyBattleDamage = true;

                              try {
                                 pp.damage(pp.getDamageSources().magic(), pp.getMaxHealth() * 100.0F);
                              } finally {
                                 applyBattleDamage = false;
                              }

                              ensureRogueFall(pp);
                           }
                        }
                     }
                  }
               } else {
                  BattleNetworking.broadcastStateUpdate(state, pm, knockedName + " has been downed!");
               }
            }
         }
      }
   }

   static void wakeMob(MobEntity m) {
      if (m != null && !m.getCommandTags().contains("turnbattle_depths")) {
         m.setAiDisabled(false);
      }
   }

   public static void tryStartMobInitiated(ServerPlayerEntity player, LivingEntity enemy) {
      UUID uuid = player.getUuid();
      if (!DepthsRun.blocksBattles(uuid)) {
         if (!DarkWorldExit.isExiting(uuid)) {
            if (!LiveCombat.isLive(player)) {
               Long expires = battleCooldowns.get(uuid);
               if (expires != null) {
                  if (player.getWorld().getTime() < expires) {
                     return;
                  }

                  battleCooldowns.remove(uuid);
               }

               if (player.currentScreenHandler instanceof PlayerScreenHandler) {
                  tryStart(player, enemy);
                  markFirstStrike(player, enemy, -1);
               }
            }
         }
      }
   }

   static boolean freezeIsSure(BattleSkill skill, int ex) {
      if (ex < 0) {
         return true;
      } else {
         String d = skill.getEffectDescription();
         if (d == null) {
            return false;
         } else {
            String low = d.toLowerCase(Locale.ROOT);
            return !low.contains("% freeze") && !low.contains("chance to freeze") && !low.contains("% chance");
         }
      }
   }

   public static void tryStartPlayerStruck(ServerPlayerEntity player, LivingEntity enemy) {
      tryStart(player, enemy);
      markFirstStrike(player, enemy, 1);
   }

   private static void markFirstStrike(ServerPlayerEntity player, LivingEntity enemy, int who) {
      BattleState st = activeBattles.get(player.getUuid());
      if (st != null && !st.isPvp && !st.isVirtualEnemy && !st.magicant && st.firstStrike == 0) {
         if (st.primaryEnemyId == enemy.getId() && st.playerUuid.equals(player.getUuid())) {
            st.firstStrike = who;
            st.firstStrikeEnemyId = enemy.getId();
            st.firstStrikeDue = (long)(player.getServer().getTicks() + 52);
         }
      }
   }

   private static void tickFirstStrikes(MinecraftServer server) {
      long now = (long)server.getTicks();
      Set seen = Collections.newSetFromMap(new IdentityHashMap<>());

      for (BattleState st : new ArrayList<>(activeBattles.values())) {
         if (st.firstStrike != 0 && seen.add(st) && now >= st.firstStrikeDue) {
            int who = st.firstStrike;
            PlayerManager pm = server.getPlayerManager();
            ServerPlayerEntity player = pm.getPlayer(st.playerUuid);
            if (player != null && activeBattles.get(st.playerUuid) == st) {
               ServerWorld world;
               LivingEntity var10000;
               label46: {
                  world = player.getServerWorld();
                  if (world.getEntityById(st.firstStrikeEnemyId) instanceof LivingEntity le && le.isAlive()) {
                     var10000 = le;
                     break label46;
                  }

                  var10000 = null;
               }

               LivingEntity enemy = var10000;
               if (enemy == null) {
                  st.firstStrike = 0;
               } else {
                  int idx = currentIndexOfEnemy(st, st.firstStrikeEnemyId);
                  if (who == 1) {
                     st.firstStrike = 2;
                     st.firstStrikeDue = now + 10L;
                     BattleNetworking.broadcastVesselAnim(st, pm, st.participantOrder.indexOf(st.playerUuid), 0, null, st.playerUuid, "");
                     BattleNetworking.broadcastBashPlayToAll(st, pm, st.playerUuid, idx);
                  } else {
                     st.firstStrike = 0;
                     if (who == 2) {
                        int level = BattleData.get(server).getOrCreate(st.playerUuid).level;
                        doBash(player, st.playerUuid, st, level, idx, world, false, 1.0F, 0);
                        if (stillActive(st) && !postActionCleanup(player, st, "", world)) {
                           BattleNetworking.broadcastStateUpdate(st, pm, "");
                        }
                     } else {
                        BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(st.playerUuid);
                        float dmg = getEnemyDamage(enemy, prog.level) * getRogueliteScale(player) * MobLevelManager.getArmorFactor(player);
                        dmg = getEffectiveStats(st.playerUuid, prog).afterDefense(dmg) * (0.85F + RNG.nextFloat() * 0.3F);
                        if (prog.soulType == SoulType.INTEGRITY) {
                           dmg *= 0.85F;
                        }

                        dmg = Math.min(player.getMaxHealth() * 0.5F, dmg);
                        dmg = heartLocketCut(player, st, st.playerUuid, dmg);
                        applyDownableDamage(st, st.playerUuid, dmg, pm);
                        BattleNetworking.broadcastVesselAnim(st, pm, st.participantOrder.indexOf(st.playerUuid), 6, null, st.playerUuid, "");
                        player.networkHandler
                           .sendPacket(
                              new PlaySoundS2CPacket(
                                 Registries.SOUND_EVENT.getEntry(TurnBattleSounds.DAMAGE),
                                 SoundCategory.PLAYERS,
                                 player.getX(),
                                 player.getY(),
                                 player.getZ(),
                                 1.0F,
                                 1.0F,
                                 0L
                              )
                           );
                        st.participantHp.put(st.playerUuid, TARGET_HEALTH.getOrDefault(st.playerUuid, player.getHealth()));
                        if (isPartyWiped(st, pm)) {
                           endBattleFromPartyWipe(st, server, pm);
                        } else {
                           BattleNetworking.broadcastStateUpdate(st, pm, "");
                        }
                     }
                  }
               }
            } else {
               st.firstStrike = 0;
            }
         }
      }
   }

   public static void tryStartPvp(ServerPlayerEntity attacker, ServerPlayerEntity target) {
      if (!attacker.getUuid().equals(target.getUuid())) {
         if (!TurnBattleMod.isUntouchable(attacker.getUuid()) && !TurnBattleMod.isUntouchable(target.getUuid())) {
            if (!MagicantManager.isBusy(attacker.getUuid()) && !MagicantManager.isBusy(target.getUuid())) {
               if (!DarkWorldExit.isExiting(attacker.getUuid()) && !DarkWorldExit.isExiting(target.getUuid())) {
                  if (!LiveCombat.isLive(attacker) || !LiveCombat.isLive(target)) {
                     if (!activeBattles.containsKey(attacker.getUuid())) {
                        if (!activeBattles.containsKey(target.getUuid())) {
                           Long exp = battleCooldowns.get(attacker.getUuid());
                           if (exp != null) {
                              if (attacker.getWorld().getTime() < exp) {
                                 return;
                              }

                              battleCooldowns.remove(attacker.getUuid());
                           }

                           RogueliteData rd = RogueliteData.get(attacker.getServer());
                           if (!rd.players.containsKey(attacker.getUuid()) || !rd.players.get(attacker.getUuid()).inRun) {
                              if (!rd.players.containsKey(target.getUuid()) || !rd.players.get(target.getUuid()).inRun) {
                                 BattleData data = BattleData.get(attacker.getServer());
                                 BattleData.PlayerProgress aProg = data.getOrCreate(attacker.getUuid());
                                 BattleData.PlayerProgress tProg = data.getOrCreate(target.getUuid());
                                 aProg.playerClass = SkillTreeDefs.computeEffectiveClass(aProg);
                                 tProg.playerClass = SkillTreeDefs.computeEffectiveClass(tProg);
                                 if (!aProg.soulRemoved && !tProg.soulRemoved) {
                                    try {
                                       BattleState state = new BattleState(attacker.getUuid(), target, target.getMaxHealth());
                                       state.isPvp = true;
                                       state.enemyPlayerUuid = target.getUuid();
                                       state.entityTypeKey = "pvp";
                                       state.entityPassive = "";
                                       state.entityBulletSpeedMult = 1.0F;
                                       state.battleElement = Element.NONE;
                                       boolean sharedParty = PlayerPartyManager.inSameParty(attacker.getUuid(), target.getUuid());
                                       List attackerAllies = (List<ServerPlayerEntity>)(sharedParty
                                          ? new ArrayList<>()
                                          : findNearbyEligiblePartyMembers(attacker, attacker.getUuid(), target.getUuid()));
                                       List targetAllies = (List<ServerPlayerEntity>)(sharedParty
                                          ? new ArrayList<>()
                                          : findNearbyEligiblePartyMembers(target, attacker.getUuid(), target.getUuid()));
                                       state.isTeamPvp = !attackerAllies.isEmpty() || !targetAllies.isEmpty();
                                       List attackerSide = new ArrayList<>();
                                       attackerSide.add(attacker);
                                       attackerSide.addAll(attackerAllies);
                                       List targetSide = new ArrayList<>();
                                       targetSide.add(target);
                                       targetSide.addAll(targetAllies);
                                       boolean attackerSideFirst;
                                       if (aProg.playerStats.offense != tProg.playerStats.offense) {
                                          attackerSideFirst = aProg.playerStats.offense > tProg.playerStats.offense;
                                       } else {
                                          attackerSideFirst = RNG.nextBoolean();
                                       }

                                       List firstSide = attackerSideFirst ? attackerSide : targetSide;
                                       List secondSide = attackerSideFirst ? targetSide : attackerSide;
                                       int firstTeam = attackerSideFirst ? 0 : 1;
                                       int secondTeam = attackerSideFirst ? 1 : 0;
                                       int maxSideLen = Math.max(firstSide.size(), secondSide.size());

                                       for (int i = 0; i < maxSideLen; i++) {
                                          if (i < firstSide.size()) {
                                             addPvpParticipant(state, firstSide.get(i), firstTeam, data);
                                          }

                                          if (i < secondSide.size()) {
                                             addPvpParticipant(state, secondSide.get(i), secondTeam, data);
                                          }
                                       }

                                       state.enemyHpMap.put(state.entityNetworkId, target.getHealth());
                                       state.enemyMaxHpMap.put(state.entityNetworkId, target.getMaxHealth());
                                       state.enemyNames.put(state.entityNetworkId, getPlayerDisplayName(target));
                                       state.pushLog(
                                          state.isTeamPvp
                                             ? "A team PvP battle begins! "
                                                + getPlayerDisplayName(attacker)
                                                + "'s side vs "
                                                + getPlayerDisplayName(target)
                                                + "'s side!"
                                             : "A PvP battle between " + getPlayerDisplayName(attacker) + " and " + getPlayerDisplayName(target) + " begins!"
                                       );

                                       for (ServerPlayerEntity p : state.isTeamPvp
                                          ? Stream.concat(attackerSide.stream(), targetSide.stream()).toList()
                                          : List.of(attacker, target)) {
                                          BattleData.PlayerProgress pProg = data.getOrCreate(p.getUuid());
                                          BattleNetworking.sendPvpBattleStart(p, state, pProg);
                                       }

                                       BattleNetworking.broadcastStateUpdate(state, attacker.getServer().getPlayerManager(), "");
                                    } catch (Exception var22) {
                                       TurnBattleMod.LOGGER
                                          .error(
                                             "[PvP] Battle start failed between " + getPlayerDisplayName(attacker) + " and " + getPlayerDisplayName(target),
                                             var22
                                          );
                                       BattleState maybeState = activeBattles.get(attacker.getUuid());

                                       for (UUID pid : maybeState != null
                                          ? new ArrayList<>(maybeState.participantOrder)
                                          : List.of(attacker.getUuid(), target.getUuid())) {
                                          activeBattles.remove(pid);
                                          TARGET_HEALTH.remove(pid);
                                          ServerPlayerEntity pp = attacker.getServer().getPlayerManager().getPlayer(pid);
                                          if (pp != null) {
                                             pp.sendMessage(Text.literal("Something went wrong starting that PvP battle.").formatted(Formatting.RED), false);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static List<ServerPlayerEntity> findNearbyEligiblePartyMembers(ServerPlayerEntity anchor, UUID excludeA, UUID excludeB) {
      List result = new ArrayList<>();
      if (!PlayerPartyManager.isInParty(anchor.getUuid())) {
         return result;
      } else {
         RogueliteData rd = RogueliteData.get(anchor.getServer());
         BattleData bd = BattleData.get(anchor.getServer());

         for (UUID memberId : PlayerPartyManager.getPartyMembers(anchor.getUuid())) {
            if (!memberId.equals(anchor.getUuid()) && !memberId.equals(excludeA) && !memberId.equals(excludeB)) {
               ServerPlayerEntity member = anchor.getServer().getPlayerManager().getPlayer(memberId);
               if (member != null
                  && member.getWorld() == anchor.getWorld()
                  && !(member.getPos().distanceTo(anchor.getPos()) > 15.0)
                  && !activeBattles.containsKey(memberId)
                  && !DarkWorldExit.isExiting(memberId)) {
                  Long memberExp = battleCooldowns.get(memberId);
                  if ((memberExp == null || member.getWorld().getTime() >= memberExp)
                     && (!rd.players.containsKey(memberId) || !rd.players.get(memberId).inRun)
                     && !bd.getOrCreate(memberId).soulRemoved) {
                     result.add(member);
                  }
               }
            }
         }

         return result;
      }
   }

   private static void addPvpParticipant(BattleState state, ServerPlayerEntity p, int team, BattleData data) {
      BattleData.PlayerProgress prog = data.getOrCreate(p.getUuid());
      prog.playerClass = SkillTreeDefs.computeEffectiveClass(prog);
      state.addParticipant(p.getUuid(), getPlayerDisplayName(p), prog.playerClass, p.getHealth(), p.getMaxHealth());
      state.participantTpMax.put(p.getUuid(), prog.playerStats.maxTp());
      state.participantTeam.put(p.getUuid(), team);
      initPvpShield(state, p.getUuid(), prog, p.getMaxHealth());
      if (prog.soulType == SoulType.BRAVERY) {
         state.addTp(p.getUuid(), 25.0F);
      }

      TARGET_HEALTH.put(p.getUuid(), p.getHealth());
      activeBattles.put(p.getUuid(), state);
   }

   private static void maybeShatterFreeze(BattleState state, LivingEntity enemy) {
      if (enemy != null) {
         int id = enemy.getId();
         if (state.enemyFreezeTurnsMap.getOrDefault(id, 0) > 0 && RNG.nextInt(100) < 50) {
            state.enemyFreezeTurnsMap.remove(id);
            if (id == state.primaryEnemyId) {
               state.enemyFreezeTurns = 0;
            }
         }
      }
   }

   static float applyPlayerDamageToEnemy(BattleState state, LivingEntity enemy, ServerPlayerEntity attacker, float dmg) {
      if (state.isVirtualEnemy) {
         dmg *= 1.0F + state.getEnemyArmorDebuff(Integer.MIN_VALUE);
         state.virtualHp = Math.max(0.0F, state.virtualHp - dmg);
         if (state.virtualHp <= 0.0F && state.bossLivesRemaining > 0) {
            state.bossLivesRemaining--;
            state.spareHpThreshold = 0.0F;
            state.enemySpareThresholds.clear();
            float restore = state.entityMaxHp;
            state.virtualHp = restore;
            state.entityCurrentHp = restore;
            state.enemyHpMap.put(Integer.MIN_VALUE, restore);
            if (attacker.getWorld() instanceof ServerWorld sw) {
               broadcastLifeLost(
                  state,
                  state.entityName + " lost a life! " + state.bossLivesRemaining + " " + (state.bossLivesRemaining == 1 ? "life" : "lives") + " remaining!",
                  sw.getServer()
               );
            }

            return restore;
         } else {
            return state.virtualHp;
         }
      } else {
         dmg *= 1.0F + state.getEnemyArmorDebuff(enemy.getId());
         float newHp = Math.max(0.0F, enemy.getHealth() - dmg);
         if (newHp <= 0.0F) {
            BattleState bossState = getBattleStateByMobId(enemy.getId());
            if (bossState != null && bossState.bossLivesRemaining > 0 && bossState.holdsLives(enemy.getId())) {
               bossState.bossLivesRemaining--;
               bossState.spareHpThreshold = 0.0F;
               bossState.enemySpareThresholds.clear();
               float capHp1 = bossState.enemyMaxHpMap.getOrDefault(enemy.getId(), enemy.getMaxHealth());
               IS_ROLLING = true;

               try {
                  enemy.setHealth(capHp1);
               } finally {
                  IS_ROLLING = false;
               }

               if (enemy.getId() == bossState.primaryEnemyId) {
                  bossState.entityCurrentHp = capHp1;
               }

               bossState.enemyHpMap.put(enemy.getId(), capHp1);
               if (attacker.getWorld() instanceof ServerWorld sw) {
                  broadcastLifeLost(
                     bossState,
                     bossState.entityName
                        + " lost a life! "
                        + bossState.bossLivesRemaining
                        + " "
                        + (bossState.bossLivesRemaining == 1 ? "life" : "lives")
                        + " remaining!",
                     sw.getServer()
                  );
               }

               return capHp1;
            }
         }

         IS_ROLLING = true;

         try {
            enemy.setHealth(newHp);
         } finally {
            IS_ROLLING = false;
         }

         maybeShatterFreeze(state, enemy);
         return newHp;
      }
   }

   static float applyMagicDamageToEnemy(BattleState state, LivingEntity enemy, ServerPlayerEntity src, float dmg) {
      if (state.isVirtualEnemy) {
         dmg *= 1.0F + state.getEnemyArmorDebuff(Integer.MIN_VALUE);
         state.virtualHp = Math.max(0.0F, state.virtualHp - dmg);
         if (state.virtualHp <= 0.0F && state.bossLivesRemaining > 0) {
            state.bossLivesRemaining--;
            state.spareHpThreshold = 0.0F;
            state.enemySpareThresholds.clear();
            float restore = state.entityMaxHp;
            state.virtualHp = restore;
            state.entityCurrentHp = restore;
            state.enemyHpMap.put(Integer.MIN_VALUE, restore);
            if (src.getWorld() instanceof ServerWorld sw) {
               broadcastLifeLost(
                  state,
                  state.entityName + " lost a life! " + state.bossLivesRemaining + " " + (state.bossLivesRemaining == 1 ? "life" : "lives") + " remaining!",
                  sw.getServer()
               );
            }

            return restore;
         } else {
            return state.virtualHp;
         }
      } else {
         dmg *= 1.0F + state.getEnemyArmorDebuff(enemy.getId());
         float newHp = Math.max(0.0F, enemy.getHealth() - dmg);
         if (newHp <= 0.0F) {
            BattleState bossState = getBattleStateByMobId(enemy.getId());
            if (bossState != null && bossState.bossLivesRemaining > 0 && bossState.holdsLives(enemy.getId())) {
               bossState.bossLivesRemaining--;
               bossState.spareHpThreshold = 0.0F;
               bossState.enemySpareThresholds.clear();
               float capHp2 = bossState.enemyMaxHpMap.getOrDefault(enemy.getId(), enemy.getMaxHealth());
               IS_ROLLING = true;

               try {
                  enemy.setHealth(capHp2);
               } finally {
                  IS_ROLLING = false;
               }

               if (enemy.getId() == bossState.primaryEnemyId) {
                  bossState.entityCurrentHp = capHp2;
               }

               bossState.enemyHpMap.put(enemy.getId(), capHp2);
               if (src.getWorld() instanceof ServerWorld sw) {
                  broadcastLifeLost(
                     bossState,
                     bossState.entityName
                        + " lost a life! "
                        + bossState.bossLivesRemaining
                        + " "
                        + (bossState.bossLivesRemaining == 1 ? "life" : "lives")
                        + " remaining!",
                     sw.getServer()
                  );
               }

               return capHp2;
            }
         }

         IS_ROLLING = true;

         try {
            enemy.setHealth(newHp);
         } finally {
            IS_ROLLING = false;
         }

         maybeShatterFreeze(state, enemy);
         return newHp;
      }
   }

   static float readEnemyHp(BattleState state, LivingEntity enemy) {
      return state.isVirtualEnemy ? state.virtualHp : Math.max(0.0F, enemy.getHealth());
   }

   static float readEnemyArmor(BattleState state, LivingEntity enemy) {
      return state.isVirtualEnemy ? state.virtualArmor : (float)enemy.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
   }

   public static void tryStartVirtual(ServerPlayerEntity player, String name, String typeKey, float maxHp, float armor) {
      UUID uuid = player.getUuid();
      if (!activeBattles.containsKey(uuid)) {
         if (!playersInStoryDialogue.contains(uuid)) {
            BattleData data = BattleData.get(player.getServer());
            BattleData.PlayerProgress prog = data.getOrCreate(uuid);
            BattleState state = new BattleState(uuid, name, typeKey, maxHp, armor);
            state.entityPassive = "";
            Element[] elements = Element.values();
            state.battleElement = elements[RNG.nextInt(elements.length - 1) + 1];
            PlayerClass cls = prog.playerClass;
            state.addParticipant(uuid, getPlayerDisplayName(player), cls, player.getHealth(), player.getMaxHealth());
            state.participantTpMax.put(uuid, prog.playerStats.maxTp());
            RecruitData.PlayerRecruits pr1 = RecruitData.get(player.getServer()).getOrCreate(uuid);
            int slot1 = pr1.activeRecruit;
            if (slot1 >= 1000) {
               int ci = slot1 - 1000;
               if (ci < pr1.customRecruits.size()) {
                  RecruitData.CustomRecruitState cr = pr1.customRecruits.get(ci);
                  float crMax = RecruitData.maxHp(cr.state);
                  float crHp = cr.state.hp >= 0.0F ? Math.min(cr.state.hp, crMax) : crMax;
                  state.addParticipant(BattleState.PARTY_MEMBER_UUID, cr.name, cr.playerClass, crHp, crMax);
                  state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID, cr.state.stats.maxTp());
                  setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID, cr.state.equippedAccessory);
               }
            } else {
               RecruitData.RecruitState rs = getActiveRecruit(player.getServer(), uuid);
               Recruit rr = getActiveRecruitEnum(player.getServer(), uuid);
               if (rs != null && rr != null) {
                  float rMax = RecruitData.maxHp(rs);
                  float rHp = RecruitData.get(player.getServer()).currentHp(rs);
                  state.addParticipant(BattleState.PARTY_MEMBER_UUID, rr.displayName, rr.playerClass, rHp, rMax);
                  state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID, rs.stats.maxTp());
                  setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID, rs.equippedAccessory);
               } else if (prog.partyMember != PartyMember.NONE) {
                  int mi = prog.partyMember.ordinal();
                  float partyMax = computePartyMemberMaxHp(prog.partyLevels[mi], prog.partyStats[mi]);
                  float partyHp = prog.partyMemberHp >= 0.0F ? Math.min(prog.partyMemberHp, partyMax) : partyMax;
                  state.addParticipant(BattleState.PARTY_MEMBER_UUID, prog.partyMember.displayName, prog.partyMember.playerClass, partyHp, partyMax);
                  state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID, prog.partyStats[mi].maxTp());
               }
            }

            pr1 = RecruitData.get(player.getServer()).getOrCreate(uuid);
            slot1 = pr1.secondActiveRecruit;
            if (slot1 >= 0) {
               if (slot1 >= 1000) {
                  int ci = slot1 - 1000;
                  if (ci < pr1.customRecruits.size()) {
                     RecruitData.CustomRecruitState cr = pr1.customRecruits.get(ci);
                     float crMax = RecruitData.maxHp(cr.state);
                     float crHp = cr.state.hp >= 0.0F ? Math.min(cr.state.hp, crMax) : crMax;
                     state.addParticipant(BattleState.PARTY_MEMBER_UUID_2, cr.name, cr.playerClass, crHp, crMax);
                     state.participantTp.put(BattleState.PARTY_MEMBER_UUID_2, 0.0F);
                     state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID_2, cr.state.stats.maxTp());
                     setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID_2, cr.state.equippedAccessory);
                  }
               } else if (slot1 < Recruit.values().length) {
                  RecruitData.RecruitState rs2 = pr1.recruits[slot1];
                  if (rs2.unlocked) {
                     Recruit r2 = Recruit.values()[slot1];
                     float rMax2 = RecruitData.maxHp(rs2);
                     float rHp2 = rs2.hp >= 0.0F ? Math.min(rs2.hp, rMax2) : rMax2;
                     state.addParticipant(BattleState.PARTY_MEMBER_UUID_2, r2.displayName, r2.playerClass, rHp2, rMax2);
                     state.participantTp.put(BattleState.PARTY_MEMBER_UUID_2, 0.0F);
                     state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID_2, rs2.stats.maxTp());
                     setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID_2, rs2.equippedAccessory);
                  }
               }
            }

            if (prog.soulType == SoulType.BRAVERY) {
               state.addTp(uuid, 25.0F);
            }

            applyRecruitStartPassives(player.getServer(), state);
            activeBattles.put(uuid, state);
            battleByMob.put(Integer.MIN_VALUE, state);
            state.enemyHpMap.put(Integer.MIN_VALUE, maxHp);
            state.enemyMaxHpMap.put(Integer.MIN_VALUE, maxHp);
            state.enemyNames.put(Integer.MIN_VALUE, name);
            state.enemyTypeKeys.put(Integer.MIN_VALUE, typeKey);
            BattleData bdata = BattleData.get(player.getServer());
            BattleNetworking.sendBattleStart(player, state, bdata.getOrCreate(uuid));
         }
      }
   }

   public static void tryStartVirtualDark(ServerPlayerEntity player, String name, String typeKey, float maxHp, float armor) {
      tryStartVirtual(player, name, typeKey, maxHp, armor);
      BattleState state = activeBattles.get(player.getUuid());
      if (state != null) {
         state.entityAffinity = Affinity.DARK;
      }
   }

   public static RecruitData.RecruitState getActiveRecruit(MinecraftServer server, UUID uuid) {
      RecruitData rd = RecruitData.get(server);
      RecruitData.PlayerRecruits pr = rd.players.get(uuid);
      if (pr != null && pr.activeRecruit >= 0) {
         RecruitData.RecruitState rs = pr.getActive();
         return rs != null && rs.unlocked ? rs : null;
      } else {
         return null;
      }
   }

   public static Recruit getActiveRecruitEnum(MinecraftServer server, UUID uuid) {
      RecruitData rd = RecruitData.get(server);
      RecruitData.PlayerRecruits pr = rd.players.get(uuid);
      return pr == null ? null : pr.getActiveRecruit();
   }

   public static void addExtraVirtualEnemy(BattleState state, String name, float hp, float armor) {
      int extraId = VIRTUAL_EXTRA_ID_COUNTER--;
      state.extraEnemyIds.add(extraId);
      state.enemyHpMap.put(extraId, hp);
      state.enemyMaxHpMap.put(extraId, hp);
      state.enemyNames.put(extraId, name);
      state.enemyTypeKeys.put(extraId, state.enemyTypeKeys.getOrDefault(state.primaryEnemyId, ""));
      battleByMob.put(extraId, state);
   }

   public static void addDepthsParticipant(ServerPlayerEntity player, BattleState state) {
      UUID uuid = player.getUuid();
      if (!activeBattles.containsKey(uuid)) {
         BattleData data = BattleData.get(player.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(uuid);
         PlayerClass cls = prog.playerClass;
         state.addParticipant(uuid, getPlayerDisplayName(player), cls, player.getHealth(), player.getMaxHealth());
         state.participantTpMax.put(uuid, prog.playerStats.maxTp());
         activeBattles.put(uuid, state);
         BattleNetworking.sendBattleStart(player, state, prog);
      }
   }

   public static void tryStart(ServerPlayerEntity player, LivingEntity enemy) {
      if (player == null || !DepthsRun.blocksBattles(player.getUuid())) {
         if (enemy instanceof MobEntity || enemy instanceof ServerPlayerEntity) {
            if (!(enemy instanceof SoulBodyEntity)) {
               if (!(enemy instanceof Shopkeeper.Entity)) {
                  UUID uuid = player.getUuid();
                  if (!DarkWorldExit.isExiting(uuid)) {
                     if (!TurnBattleMod.isUntouchable(uuid)) {
                        if (enemy instanceof ServerPlayerEntity other && TurnBattleMod.isUntouchable(other.getUuid())) {
                           return;
                        }

                        if (!activeBattles.containsKey(uuid)) {
                           if (!MagicantManager.isBusy(uuid)) {
                              BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(uuid);
                              if (!prog.soulRemoved) {
                                 if (enemy.isAlive()) {
                                    if (TurnBattleConfig.get().passiveMobBattles
                                       || !(enemy instanceof PassiveEntity)
                                       || MobLevelManager.isAquaticMob((MobEntity)enemy)) {
                                       BattleState existing = battleByMob.get(enemy.getId());
                                       if (existing == null) {
                                          startNewBattle(player, enemy);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isDarknessConstricting(LivingEntity enemy) {
      if (enemy instanceof WardenEntity) {
         return true;
      } else if (enemy.getWorld() instanceof ServerWorld sw) {
         if (sw.getRegistryKey().equals(World.END)) {
            return true;
         } else {
            RegistryEntry biome = sw.getBiome(enemy.getBlockPos());
            return biome.matchesKey(RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "deep_dark")));
         }
      } else {
         return false;
      }
   }

   private static void startNewBattle(ServerPlayerEntity player, LivingEntity enemy) {
      UUID uuid = player.getUuid();
      if (runInvincibleUntil.remove(uuid) != null) {
         player.setInvulnerable(false);
      }

      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(uuid);
      prog.playerClass = SkillTreeDefs.computeEffectiveClass(prog);
      RogueliteData rogueData = RogueliteData.get(player.getServer());
      RogueliteData.PlayerRogueData rogueRd = rogueData.players.get(uuid);
      float rogueScale = rogueRd != null && rogueRd.inRun ? RogueliteManager.getRunDifficultyScale(player.getServer(), uuid) : 1.0F;
      float scaledMax = applyRogueHpScale(enemy, rogueScale);
      BattleState state = new BattleState(uuid, enemy, scaledMax);
      state.levelAtStart = prog.level;
      state.battleStyle = prog.battleStyle != null ? prog.battleStyle : BattleStyle.BULLET_HELL;
      if (state.battleStyle == BattleStyle.LIVE_COMBAT) {
         state.battleStyle = BattleStyle.BULLET_HELL;
         state.forceOverhaul = true;
      }

      if (enemy instanceof MirrorCloneEntity) {
         state.magicant = true;
         state.forceOverhaul = true;
         state.battleStyle = BattleStyle.BULLET_HELL;
      }

      if (DepthsRun.isActive(uuid)) {
         state.depthsRun = true;
         state.forceOverhaul = false;
         state.battleStyle = prog.battleStyle == BattleStyle.DIRECT_DAMAGE ? BattleStyle.DIRECT_DAMAGE : BattleStyle.BULLET_HELL;
         prog.playerClass = PlayerClass.ORI;
      }

      state.rogueliteArmorScale = rogueScale;
      state.entityPassive = MobLevelManager.getMobPassive(enemy);
      state.entityBulletSpeedMult = MobLevelManager.getMobBulletSpeed(enemy);
      state.darknessConstricting = isDarknessConstricting(enemy);
      if (enemy instanceof MobEntity mobToLevel) {
         int floorLevel = DepthsRun.isActive(uuid) ? DepthsRun.enemyLevelFor(uuid, enemy) : DepthsDescentManager.getFloorEnemyLevel(player.getServer(), uuid);
         if (floorLevel > 0) {
            MobLevelManager.registerMobLevel(enemy, floorLevel);
            MobLevelManager.applyMobLevel(mobToLevel, floorLevel);
         }
      }

      PlayerClass cls = prog.playerClass;
      state.addParticipant(uuid, getPlayerDisplayName(player), cls, player.getHealth(), player.getMaxHealth());
      state.participantTpMax.put(uuid, prog.playerStats.maxTp());
      RecruitData.PlayerRecruits pr1 = RecruitData.get(player.getServer()).getOrCreate(uuid);
      if (pr1.activeRecruit >= 1000) {
         int ci = pr1.activeRecruit - 1000;
         if (ci < pr1.customRecruits.size()) {
            RecruitData.CustomRecruitState cr = pr1.customRecruits.get(ci);
            float crMax = RecruitData.maxHp(cr.state);
            float crHp = cr.state.hp >= 0.0F ? Math.min(cr.state.hp, crMax) : crMax;
            state.addParticipant(BattleState.PARTY_MEMBER_UUID, cr.name, cr.playerClass, crHp, crMax);
            state.participantTp.put(BattleState.PARTY_MEMBER_UUID, 0.0F);
            state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID, cr.state.stats.maxTp());
            setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID, cr.state.equippedAccessory);
         }
      } else {
         RecruitData.RecruitState rs = getActiveRecruit(player.getServer(), uuid);
         Recruit rr = getActiveRecruitEnum(player.getServer(), uuid);
         if (rs != null && rr != null) {
            float rMax = RecruitData.maxHp(rs);
            float rHp = RecruitData.get(player.getServer()).currentHp(rs);
            state.addParticipant(BattleState.PARTY_MEMBER_UUID, rr.displayName, rr.playerClass, rHp, rMax);
            state.participantTp.put(BattleState.PARTY_MEMBER_UUID, 0.0F);
            state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID, rs.stats.maxTp());
            setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID, rs.equippedAccessory);
         } else if (prog.partyMember != PartyMember.NONE) {
            int mi = prog.partyMember.ordinal();
            CharStats partyStats = prog.partyStats[mi];
            float partyMax = computePartyMemberMaxHp(prog.partyLevels[mi], partyStats);
            float partyHp = prog.partyMemberHp >= 0.0F ? Math.min(prog.partyMemberHp, partyMax) : partyMax;
            state.addParticipant(BattleState.PARTY_MEMBER_UUID, prog.partyMember.displayName, prog.partyMember.playerClass, partyHp, partyMax);
            state.participantTp.put(BattleState.PARTY_MEMBER_UUID, 0.0F);
            state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID, partyStats.maxTp());
         }
      }

      pr1 = RecruitData.get(player.getServer()).getOrCreate(uuid);
      int slot2 = pr1.secondActiveRecruit;
      if (slot2 >= 0) {
         if (slot2 >= 1000) {
            int ci = slot2 - 1000;
            if (ci < pr1.customRecruits.size()) {
               RecruitData.CustomRecruitState cr = pr1.customRecruits.get(ci);
               float crMax = RecruitData.maxHp(cr.state);
               float crHp = cr.state.hp >= 0.0F ? Math.min(cr.state.hp, crMax) : crMax;
               state.addParticipant(BattleState.PARTY_MEMBER_UUID_2, cr.name, cr.playerClass, crHp, crMax);
               state.participantTp.put(BattleState.PARTY_MEMBER_UUID_2, 0.0F);
               state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID_2, cr.state.stats.maxTp());
               setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID_2, cr.state.equippedAccessory);
            }
         } else if (slot2 < Recruit.values().length) {
            RecruitData.RecruitState rs2 = pr1.recruits[slot2];
            if (rs2.unlocked) {
               Recruit r2 = Recruit.values()[slot2];
               float rMax2 = RecruitData.maxHp(rs2);
               float rHp2 = rs2.hp >= 0.0F ? Math.min(rs2.hp, rMax2) : rMax2;
               state.addParticipant(BattleState.PARTY_MEMBER_UUID_2, r2.displayName, r2.playerClass, rHp2, rMax2);
               state.participantTp.put(BattleState.PARTY_MEMBER_UUID_2, 0.0F);
               state.participantTpMax.put(BattleState.PARTY_MEMBER_UUID_2, rs2.stats.maxTp());
               setRecruitGimmick(state, BattleState.PARTY_MEMBER_UUID_2, rs2.equippedAccessory);
            }
         }
      }

      state.participantTpMax.put(uuid, prog.playerStats.maxTp());
      if (prog.equippedAccessory != null && !prog.equippedAccessory.isEmpty()) {
         OffhandGimmick accGimmick = OffhandGimmick.fromStack(prog.equippedAccessory);
         if (accGimmick != OffhandGimmick.NONE) {
            state.offhandGimmicks.put(uuid, accGimmick);
         }
      }

      if (prog.soulType == SoulType.BRAVERY) {
         state.addTp(uuid, 25.0F);
      }

      applyRecruitStartPassives(player.getServer(), state);
      RogueliteData rogueDataTP = RogueliteData.get(player.getServer());
      RogueliteData.PlayerRogueData rdTP = rogueDataTP.players.get(uuid);
      if (rdTP != null && rdTP.inRun && rdTP.uniqueCards.contains("TP_SURGE")) {
         state.addTp(uuid, 40.0F);
      }

      TARGET_HEALTH.put(uuid, player.getHealth());
      downedPlayers.remove(uuid);
      activeBattles.put(uuid, state);
      if (enemy instanceof MobEntity mobEnemy) {
         freezeMob(mobEnemy);
      } else {
         frozenEntityIds.add(enemy.getId());
      }

      battleByMob.put(enemy.getId(), state);
      MobLevelManager.ensureAffinities(enemy);
      state.enemyHpMap.put(enemy.getId(), state.entityCurrentHp);
      state.enemyMaxHpMap.put(enemy.getId(), state.entityMaxHp);
      state.enemyNames.put(enemy.getId(), state.entityName);
      int eliteTier = EliteMobManager.getTier(enemy.getUuid());
      if (eliteTier >= 0) {
         int[] livesPerTier = new int[]{1, 2, 3, 4, 4};
         state.bossDifficultyOrdinal = eliteTier;
         state.bossMaxLives = livesPerTier[Math.min(eliteTier, livesPerTier.length - 1)];
         state.bossLivesRemaining = state.bossMaxLives;
         state.bossEnemyId = enemy.getId();
      }

      if (player.getWorld() instanceof ServerWorld sw) {
         Box searchBox = new Box(enemy.getX() - 7.0, enemy.getY() - 4.0, enemy.getZ() - 7.0, enemy.getX() + 7.0, enemy.getY() + 4.0, enemy.getZ() + 7.0);
         List nearby = new ArrayList<>();
         if (!(enemy instanceof DarknerBossEntity)) {
            for (LivingEntity ent : sw.getEntitiesByClass(
               LivingEntity.class,
               searchBox,
               e -> e != enemy
                     && e.isAlive()
                     && !(e instanceof PlayerEntity)
                     && e instanceof HostileEntity
                     && !(e instanceof DarknerBossEntity)
                     && !frozenEntityIds.contains(e.getId())
                     && !battleByMob.containsKey(e.getId())
            )) {
               nearby.add(ent);
               if (nearby.size() >= 2) {
                  break;
               }
            }
         }

         for (LivingEntity extra : nearby) {
            state.extraEnemyIds.add(extra.getId());
            if (extra instanceof MobEntity extraMobFreeze) {
               freezeMob(extraMobFreeze);
            } else {
               frozenEntityIds.add(extra.getId());
            }

            battleByMob.put(extra.getId(), state);
            MobLevelManager.ensureAffinities(extra);
            float extraScaledMax = applyRogueHpScale(extra, rogueScale);
            state.enemyHpMap.put(extra.getId(), extra.getHealth());
            state.enemyMaxHpMap.put(extra.getId(), extraScaledMax);
            state.enemyNames.put(extra.getId(), extra.getDisplayName().getString());
            state.enemyTypeKeys.put(extra.getId(), EntityType.getId(extra.getType()).toString());
            if (state.bossLivesRemaining <= 0) {
               int extraEliteTier = EliteMobManager.getTier(extra.getUuid());
               if (extraEliteTier >= 0) {
                  int[] livesPerTier = new int[]{1, 2, 3, 4, 4};
                  state.bossDifficultyOrdinal = extraEliteTier;
                  state.bossMaxLives = livesPerTier[Math.min(extraEliteTier, livesPerTier.length - 1)];
                  state.bossLivesRemaining = state.bossMaxLives;
                  state.bossEnemyId = extra.getId();
               }
            }
         }
      }

      List elements = new ArrayList<>();
      elements.add(ElementRegistry.getElement(enemy));
      if (player.getWorld() instanceof ServerWorld sw2) {
         for (int eid : state.extraEnemyIds) {
            if (sw2.getEntityById(eid) instanceof LivingEntity le) {
               elements.add(ElementRegistry.getElement(le));
            }
         }
      }

      elements.removeIf(e -> e == Element.NONE);
      state.battleElement = elements.isEmpty() ? Element.NONE : elements.get(RNG.nextInt(elements.size()));
      BattleStaging.stage(state, player);
      BattleNetworking.sendBattleStart(player, state, prog);
      state.pushLog("A battle with " + state.entityName + " begins!");
      if (state.darknessConstricting) {
         state.pushLog("Darkness constricts you... TP gain reduced outside of ACTing");
      }

      PlayerPartyManager.onMemberEnteredBattle(player.getServer(), player.getUuid(), state);
   }

   public static void addPlayerToExistingBattle(ServerPlayerEntity player, BattleState state) {
      if (!activeBattles.containsKey(player.getUuid())) {
         if (player.isAlive() && !player.isDead()) {
            if (!DarkWorldExit.isExiting(player.getUuid())) {
               joinBattle(player, state);
            }
         }
      }
   }

   private static void joinBattle(ServerPlayerEntity player, BattleState state) {
      UUID uuid = player.getUuid();
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(uuid);
      PlayerClass cls = prog.playerClass;
      if (state.participantOrder.contains(BattleState.PARTY_MEMBER_UUID)) {
         state.removeParticipant(BattleState.PARTY_MEMBER_UUID);
      }

      if (state.participantOrder.contains(BattleState.PARTY_MEMBER_UUID_2)) {
         state.removeParticipant(BattleState.PARTY_MEMBER_UUID_2);
      }

      state.addParticipant(uuid, getPlayerDisplayName(player), cls, player.getHealth(), player.getMaxHealth());
      state.participantTpMax.put(uuid, prog.playerStats.maxTp());
      TARGET_HEALTH.put(uuid, player.getHealth());
      downedPlayers.remove(uuid);
      activeBattles.put(uuid, state);
      if (prog.soulType == SoulType.BRAVERY) {
         state.addTp(uuid, 25.0F);
      }

      if (prog.equippedAccessory != null && !prog.equippedAccessory.isEmpty()) {
         OffhandGimmick accGimmick = OffhandGimmick.fromStack(prog.equippedAccessory);
         if (accGimmick != OffhandGimmick.NONE) {
            state.offhandGimmicks.put(uuid, accGimmick);
         }
      }

      if (state.phase == BattleState.Phase.AWAITING_ENEMY_RESULT && !state.isPvp) {
         int mobId = state.entityNetworkId;
         Set reported = pendingEnemyResults.computeIfAbsent(mobId, k -> new HashSet<>());
         reported.add(uuid);
      }

      BattleStaging.onJoin(state, player);
      if (state.isPvp) {
         BattleNetworking.sendPvpBattleStart(player, state, prog);
      } else {
         BattleNetworking.sendBattleStart(player, state, prog);
      }

      BattleNetworking.broadcastParticipantUpdate(state, player.getServer().getPlayerManager());
      if (state.phase == BattleState.Phase.PLAYER_TURN && !state.participantOrder.isEmpty()) {
         PlayerManager pm = player.getServer().getPlayerManager();
         UUID nowActing = state.currentParticipant();
         if (isRecruitSlot(nowActing)) {
            ServerPlayerEntity owner = pm.getPlayer(state.playerUuid);
            if (owner != null) {
               BattleNetworking.sendPartyMemberTurn(owner, state, nowActing);
            }
         } else {
            ServerPlayerEntity actingPlayer = pm.getPlayer(nowActing);
            if (actingPlayer != null) {
               BattleNetworking.sendTurnNotice(actingPlayer, state);
            }
         }
      }
   }

   private static boolean applyPreActionStatusChecks(ServerPlayerEntity player, BattleState state, UUID actor) {
      state.clearStatusImmunity();
      if (state.playerBurned.contains(actor)) {
         PlayerManager pmBurn = player.getServer().getPlayerManager();
         float maxHp = getMaxHpFor(state, actor, pmBurn);
         float curHp = getBattleHpFor(state, actor, pmBurn);
         float burnDmg = maxHp * 0.05F;
         setBattleHpFor(state, actor, curHp - burnDmg, pmBurn);
         String burnedName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
         BattleNetworking.broadcastStateUpdate(state, pmBurn, String.format("%s takes %.0f burn damage!", burnedName, burnDmg));
      }

      if (state.playerSleepMap.getOrDefault(actor, 0) > 0) {
         String sleeperName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
         if (!staysAsleep(state.playerSleepMap, actor)) {
            state.playerSleepMap.remove(actor);
            BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), sleeperName + " woke up!");
         } else if (!state.isPvp) {
            String msg = sleeperName + " is asleep... Zzz";
            ServerWorld sleepWorld = player.getServerWorld();
            afterParticipantAction(player, state, msg, false, sleepWorld);
            return false;
         }
      }

      if (state.playerFreezeMap.getOrDefault(actor, 0) > 0) {
         int remaining = state.playerFreezeMap.get(actor) - 1;
         if (remaining <= 0) {
            state.playerFreezeMap.remove(actor);
            String thawedName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
            BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), thawedName + " thawed out!");
         } else {
            state.playerFreezeMap.put(actor, remaining);
         }
      }

      if (state.playerConfusionMap.getOrDefault(actor, 0) > 0 && RNG.nextInt(100) < 15) {
         state.playerConfusionMap.remove(actor);
         PlayerManager pmConf = player.getServer().getPlayerManager();
         float maxHp = getMaxHpFor(state, actor, pmConf);
         float curHp = getBattleHpFor(state, actor, pmConf);
         float snapDmg = maxHp * 0.05F;
         setBattleHpFor(state, actor, curHp - snapDmg, pmConf);
         String snappedName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
         BattleNetworking.broadcastStateUpdate(state, pmConf, String.format("%s snaps out of confusion, hurting themselves for %.0f!", snappedName, snapDmg));
      }

      return true;
   }

   public static void processAction(ServerPlayerEntity player, int actionType, int param, int targetIndex) {
      UUID uuid = player.getUuid();
      BattleState state = activeBattles.get(uuid);
      if (state != null && state.phase == BattleState.Phase.PLAYER_TURN) {
         if (!state.resolving) {
            UUID actor = state.currentParticipant();
            boolean actorIsPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID);
            boolean actorIsPartyMember2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
            boolean actorIsAnyPartyMember = actorIsPartyMember || actorIsPartyMember2;
            if (actor.equals(uuid) || actorIsAnyPartyMember && state.playerUuid.equals(uuid)) {
               if (applyPreActionStatusChecks(player, state, actor)) {
                  ServerWorld world = player.getServerWorld();
                  LivingEntity enemy = findEnemyByIndex(state, world, 0);
                  if (!state.isVirtualEnemy) {
                     if (enemy == null && state.extraEnemyIds.isEmpty()) {
                        endBattle(state, player.getServer(), "enemy_gone");
                        return;
                     }

                     if (enemy == null) {
                        enemy = findEnemyByIndex(state, world, 1);
                     }

                     if (enemy == null) {
                        endBattle(state, player.getServer(), "enemy_gone");
                        return;
                     }
                  }

                  if (!state.isPvp || state.playerSleepMap.getOrDefault(actor, 0) <= 0 || actionType == 1 && param == -99) {
                     BattleData data = BattleData.get(player.getServer());
                     BattleData.PlayerProgress prog = data.getOrCreate(state.playerUuid);
                     BattleData.PlayerProgress actorProg = data.getOrCreate(uuid);
                     int level = actorIsAnyPartyMember ? prog.level : actorProg.level;
                     PlayerClass cls = actorIsPartyMember
                        ? state.participantClass.get(BattleState.PARTY_MEMBER_UUID)
                        : (actorIsPartyMember2 ? state.participantClass.get(BattleState.PARTY_MEMBER_UUID_2) : state.participantClass.get(uuid));
                     if (!queuedTurns(state) || actionType != 0 && actionType != 1 && actionType != 2 && actionType != 5) {
                        String message;
                        switch (actionType) {
                           case 0:
                              BattleNetworking.broadcastBashPlay(state, player.getServer().getPlayerManager(), actor, targetIndex);
                              message = doBash(player, actor, state, level, targetIndex, world);
                              break;
                           case 1:
                              BattleNetworking.broadcastSkillPlay(state, player.getServer().getPlayerManager(), actor, param, cls, targetIndex);

                              try {
                                 message = doSkill(player, actor, state, param, level, cls, targetIndex, world);
                                 break;
                              } finally {
                                 SkillBonus.magicFlat = 0.0F;
                              }
                           case 2:
                              message = doGoods(player, state, param, targetIndex);
                              break;
                           case 3:
                              message = doDefend(player, actor, state, resolveActorIq(player, state, actor, prog, actorProg));
                              BattleNetworking.broadcastSharedTp(state, player.getServer().getPlayerManager());
                              break;
                           case 4:
                              if (state.magicant || state.bossMaxLives > 0 && state.depthsFountainKey != null) {
                                 message = state.magicant ? "You can't run from yourself." : "You can't escape from a boss!";
                                 afterParticipantAction(player, state, message, false, world);
                                 return;
                              }

                              if (RNG.nextFloat() < 0.55F) {
                                 endBattle(state, player.getServer(), "run");
                                 return;
                              }

                              message = "Couldn't escape!";
                              afterParticipantAction(player, state, message, false, world);
                              if (queuedTurns(state) && stillActive(state) && !state.resolving) {
                                 BattleNetworking.broadcastBattleLine(state, player.getServer().getPlayerManager(), 0, "Couldn't escape...", 2);
                              }

                              return;
                           case 5:
                              if (state.isPvp) {
                                 afterParticipantAction(player, state, "There's no sparing in a PvP fight!", false, world);
                                 return;
                              }

                              if (state.magicant) {
                                 message = "You can't show mercy to yourself.";
                                 afterParticipantAction(player, state, message, false, world);
                                 return;
                              }

                              LivingEntity spareTarget = findEnemyByIndex(state, world, targetIndex);
                              int spareId = getEnemyIdByIndex(state, targetIndex);
                              String spareName = state.enemyNames.getOrDefault(spareId, state.entityName);
                              boolean spareEligible = state.isMercyEligibleById(spareId);
                              if (!spareEligible) {
                                 message = spareName + " isn't ready to spare...";
                                 afterParticipantAction(player, state, message, false, world);
                                 return;
                              }

                              message = "You showed MERCY to " + spareName + "!";
                              boolean enemyRemoved = spareOneEnemy(state, world, spareId, targetIndex, player.getServer());
                              boolean allGone2 = state.isVirtualEnemy
                                 ? state.virtualHp <= 0.0F
                                 : enemyRemoved && state.extraEnemyIds.isEmpty() && findEnemyByIndex(state, world, 0) == null;
                              if (allGone2) {
                                 BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), message);
                                 endBattle(state, player.getServer(), "mercy");
                                 return;
                              }

                              afterParticipantAction(player, state, message, false, world);
                              return;
                           default:
                              message = "...";
                        }

                        afterParticipantAction(player, state, message, actionType == 3, world);
                     } else {
                        queueSelection(player, state, actor, actionType, param, targetIndex, level, cls, world);
                     }
                  } else {
                     String sleeperName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
                     afterParticipantAction(player, state, sleeperName + " is too asleep to do that! (Use Awaken)", false, world);
                  }
               }
            }
         }
      }
   }

   private static void applyKindnessLifesteal(ServerPlayerEntity player, UUID actor, BattleState state, float dmg, BattleSkill skill) {
      if (soulOf(player.getServer(), state, actor) == SoulType.KINDNESS) {
         if (skill instanceof SoulMageSkill sms && sms.getSoul() == SoulType.KINDNESS) {
            float heal = dmg * 0.15F;
            if (isRecruitSlot(actor)) {
               applyDownableHeal(state, actor, heal, player.getServer().getPlayerManager());
               return;
            }

            ServerPlayerEntity actorEnt = actor.equals(player.getUuid()) ? player : player.getServer().getPlayerManager().getPlayer(actor);
            if (actorEnt != null) {
               if (TurnBattleConfig.get().rollingHealth && !state.isPvp) {
                  applyDownableHeal(state, actor, heal, player.getServer().getPlayerManager());
                  return;
               }

               float newHp = Math.min(actorEnt.getMaxHealth(), actorEnt.getHealth() + heal);
               IS_ROLLING = true;

               try {
                  actorEnt.setHealth(newHp);
               } finally {
                  IS_ROLLING = false;
               }

               TARGET_HEALTH.put(actor, newHp);
               state.participantHp.put(actor, newHp);
            }

            return;
         }
      }
   }

   static float getRogueliteScale(ServerPlayerEntity player) {
      return RogueliteManager.getRunDifficultyScale(player.getServer(), player.getUuid());
   }

   static double bashBaseDamage(ServerPlayerEntity player, UUID actor, BattleState state) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog2 = data.getOrCreate(state.playerUuid);
      boolean actorIsAnyPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || actor.equals(BattleState.PARTY_MEMBER_UUID_2);
      BattleData.PlayerProgress actorProg = actorIsAnyPartyMember ? prog2 : data.getOrCreate(actor);
      CharStats actorStats = getEffectiveStats(actor, actorProg);
      double weaponDmg;
      if (actorIsAnyPartyMember) {
         weaponDmg = 4.0;
         boolean isSlot2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
         RecruitData rd = RecruitData.get(player.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
         int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            if (ci < pr.customRecruits.size()) {
               RecruitData.CustomRecruitState cr = pr.customRecruits.get(ci);
               actorStats = getRecruitEffectiveStats(cr.state);
               WeaponStats crWs = WeaponStats.fromStack(cr.state.equippedWeapon);
               if (crWs != null) {
                  weaponDmg += (double)crWs.bashBonus();
               }
            }
         } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
            RecruitData.RecruitState rs = pr.recruits[slotCode];
            WeaponStats recruitWs = WeaponStats.fromStack(rs.equippedWeapon);
            if (recruitWs != null) {
               weaponDmg += (double)recruitWs.bashBonus();
            }

            actorStats = getRecruitEffectiveStats(rs);
         } else if (!isSlot2) {
            Recruit activeRecruit = getActiveRecruitEnum(player.getServer(), state.playerUuid);
            if (activeRecruit != null) {
               RecruitData.RecruitState rs = rd.getOrCreate(state.playerUuid).get(activeRecruit);
               WeaponStats recruitWs = WeaponStats.fromStack(rs.equippedWeapon);
               if (recruitWs != null) {
                  weaponDmg += (double)recruitWs.bashBonus();
               }

               actorStats = getRecruitEffectiveStats(rs);
            } else {
               WeaponStats partyWs = WeaponStats.fromStack(prog2.partyEquippedWeapon);
               if (partyWs != null) {
                  weaponDmg += (double)partyWs.bashBonus();
               }
            }
         } else {
            WeaponStats partyWs = WeaponStats.fromStack(prog2.partyEquippedWeapon);
            if (partyWs != null) {
               weaponDmg += (double)partyWs.bashBonus();
            }
         }
      } else {
         double itemDmg = 1.0;
         if (actorProg.equippedWeapon != null && !actorProg.equippedWeapon.isEmpty()) {
            AttributeModifiersComponent attrComp = (AttributeModifiersComponent)actorProg.equippedWeapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attrComp != null) {
               for (net.minecraft.component.type.AttributeModifiersComponent.Entry entry : attrComp.modifiers()) {
                  if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && entry.modifier().operation() == Operation.ADD_VALUE) {
                     itemDmg += entry.modifier().value();
                  }
               }
            }
         }

         weaponDmg = Math.max(4.0, itemDmg);
         WeaponStats ws = WeaponStats.fromStack(actorProg.equippedWeapon);
         if (ws != null) {
            weaponDmg += (double)ws.bashBonus();
         }
      }

      return weaponDmg * (1.0 + (double)actorStats.offenseBonus());
   }

   private static float hitWeight(BattleSkill skill) {
      int hits = 1;
      int ex = skill.getExtra();
      if (skill == LightnerSkill.RED_BUSTER) {
         hits = 4;
      } else if (skill.getType() != SkillType.MULTI && skill.getType() != SkillType.AOE) {
         if (ex == 2 && (skill.getType() == SkillType.DAMAGE || skill.getType() == SkillType.BYPASS)
            || skill == PsiSkill.FIRE_B
            || skill == PsiSkill.FIRE_G
            || skill == PsiSkill.FIRE_O) {
            hits = 2;
         }
      } else {
         hits = Math.max(1, ex);
      }

      float w = 0.0F;
      float k = 1.0F;

      for (int i = 0; i < hits; i++) {
         w += k;
         k *= 0.85F;
      }

      return w;
   }

   private static float offenseSkillOverBash(BattleSkill skill) {
      return 1.25F + 0.01F * (float)Math.max(0, skill.getTpCost());
   }

   private static String doBash(ServerPlayerEntity player, UUID actor, BattleState state, int level, int targetIndex, ServerWorld world) {
      return doBash(player, actor, state, level, targetIndex, world, null, 1.0F, 0);
   }

   private static String doBash(
      ServerPlayerEntity player,
      UUID actor,
      BattleState state,
      int level,
      int targetIndex,
      ServerWorld world,
      Boolean predeterminedCrit,
      float qteMultiplier,
      int qteTpTier
   ) {
      state.lastActionHitCount = 1;
      if (state.isPvp) {
         return doBashPvp(player, actor, state, targetIndex);
      } else {
         LivingEntity enemy = findEnemyByIndex(state, world, targetIndex);
         if (enemy == null && !state.isVirtualEnemy) {
            return "Target is gone!";
         } else {
            int enemyId = getEnemyIdByIndex(state, targetIndex);
            BattleData data = BattleData.get(player.getServer());
            BattleData.PlayerProgress prog2 = data.getOrCreate(state.playerUuid);
            boolean actorIsAnyPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || actor.equals(BattleState.PARTY_MEMBER_UUID_2);
            BattleData.PlayerProgress actorProg = actorIsAnyPartyMember ? prog2 : data.getOrCreate(actor);
            CharStats actorStats = getEffectiveStats(actor, actorProg);
            double weaponDmg;
            if (actorIsAnyPartyMember) {
               weaponDmg = 4.0;
               boolean isSlot2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
               RecruitData rd = RecruitData.get(player.getServer());
               RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
               int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
               if (slotCode >= 1000) {
                  int ci = slotCode - 1000;
                  if (ci < pr.customRecruits.size()) {
                     RecruitData.CustomRecruitState cr = pr.customRecruits.get(ci);
                     actorStats = getRecruitEffectiveStats(cr.state);
                     WeaponStats crWs = WeaponStats.fromStack(cr.state.equippedWeapon);
                     if (crWs != null) {
                        weaponDmg += (double)crWs.bashBonus();
                     }
                  }
               } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
                  RecruitData.RecruitState rs = pr.recruits[slotCode];
                  WeaponStats recruitWs = WeaponStats.fromStack(rs.equippedWeapon);
                  if (recruitWs != null) {
                     weaponDmg += (double)recruitWs.bashBonus();
                  }

                  actorStats = getRecruitEffectiveStats(rs);
               } else if (!isSlot2) {
                  Recruit activeRecruit = getActiveRecruitEnum(player.getServer(), state.playerUuid);
                  if (activeRecruit != null) {
                     RecruitData.RecruitState rs = rd.getOrCreate(state.playerUuid).get(activeRecruit);
                     WeaponStats recruitWs = WeaponStats.fromStack(rs.equippedWeapon);
                     if (recruitWs != null) {
                        weaponDmg += (double)recruitWs.bashBonus();
                     }

                     actorStats = getRecruitEffectiveStats(rs);
                  } else {
                     WeaponStats partyWs = WeaponStats.fromStack(prog2.partyEquippedWeapon);
                     if (partyWs != null) {
                        weaponDmg += (double)partyWs.bashBonus();
                     }
                  }
               } else {
                  WeaponStats partyWs = WeaponStats.fromStack(prog2.partyEquippedWeapon);
                  if (partyWs != null) {
                     weaponDmg += (double)partyWs.bashBonus();
                  }
               }
            } else {
               double itemDmg = 1.0;
               if (actorProg.equippedWeapon != null && !actorProg.equippedWeapon.isEmpty()) {
                  AttributeModifiersComponent attrComp = (AttributeModifiersComponent)actorProg.equippedWeapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                  if (attrComp != null) {
                     for (net.minecraft.component.type.AttributeModifiersComponent.Entry entry : attrComp.modifiers()) {
                        if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && entry.modifier().operation() == Operation.ADD_VALUE) {
                           itemDmg += entry.modifier().value();
                        }
                     }
                  }
               }

               weaponDmg = Math.max(4.0, itemDmg);
               WeaponStats ws = WeaponStats.fromStack(actorProg.equippedWeapon);
               if (ws != null) {
                  weaponDmg += (double)ws.bashBonus();
               }
            }

            double scaled = weaponDmg * (1.0 + (double)actorStats.offenseBonus());
            if (state.getAttackBoostTurns(actor) > 0) {
               scaled *= (double)state.getAttackBoostMult(actor);
            }

            if (soulOf(player.getServer(), state, actor) == SoulType.JUSTICE) {
               float currentHp = state.participantHp.getOrDefault(actor, player.getHealth());
               float maxHp = state.participantMaxHp.getOrDefault(actor, player.getMaxHealth());
               if (currentHp < maxHp * 0.5F) {
                  scaled *= 1.2;
               }
            }

            double armor = (double)(readEnemyArmor(state, enemy) * state.rogueliteArmorScale);
            double factor = Math.max(0.55, 1.0 - armor * 0.04);
            float dtm = enemy != null ? MobLevelManager.getMobDamageTaken(enemy) : 1.0F;
            float dmg = (float)(scaled * factor * (double)dtm) * qteMultiplier;
            List rogueUniques = actorIsAnyPartyMember ? List.of() : RogueliteManager.activeUniques(player.getServer(), actor);
            boolean doubleEdge = rogueUniques.contains("DOUBLE_EDGE");
            if (doubleEdge) {
               dmg *= 2.0F;
            }

            WeaponStats actorWs = resolveActorWeaponStats(player, actor, state, prog2);
            float[] bashAff = applyBashAffinity(state, enemy, dmg, actorWs != null ? actorWs.getElements() : null);
            dmg = bashAff[0];
            if (state.playerBurned.contains(actor)) {
               dmg *= 0.5F;
            }

            String bashAffSuffix = bashAff[1] > 1.0F ? " [WEAK!]" : (bashAff[1] < 1.0F ? " [RESIST]" : "");
            boolean rolledCrit = predeterminedCrit != null ? predeterminedCrit : RNG.nextFloat() < actorStats.critChance();
            applyBattleDamage = true;

            float newHpAfterBash;
            try {
               newHpAfterBash = applyPlayerDamageToEnemy(state, enemy, player, dmg);
            } finally {
               applyBattleDamage = false;
            }

            if (!rogueUniques.isEmpty()) {
               PlayerManager pmRU = player.getServer().getPlayerManager();
               if (doubleEdge) {
                  rogueAdjustHp(state, actor, -dmg * 0.1F, pmRU);
               }

               if (rogueUniques.contains("VAMPIRE_FANG")) {
                  rogueAdjustHp(state, actor, dmg * 0.15F, pmRU);
               }

               if (rogueUniques.contains("TP_SIPHON")) {
                  state.addTp(actor, 6.0F);
               }
            }

            player.networkHandler
               .sendPacket(
                  new PlaySoundS2CPacket(
                     Registries.SOUND_EVENT.getEntry(TurnBattleSounds.DAMAGE),
                     SoundCategory.PLAYERS,
                     player.getX(),
                     player.getY(),
                     player.getZ(),
                     1.0F,
                     1.0F,
                     0L
                  )
               );
            if (targetIndex == 0) {
               state.entityCurrentHp = newHpAfterBash;
            }

            state.enemyHpMap.put(enemyId, newHpAfterBash);
            String actorName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
            String targetName = enemy != null
               ? state.enemyNames.getOrDefault(enemyId, enemy.getDisplayName().getString())
               : state.enemyNames.getOrDefault(enemyId, state.entityName);
            if (qteTpTier > 0) {
               float qteTp = switch (qteTpTier) {
                  case 2 -> 6.0F;
                  case 3 -> 12.0F;
                  default -> 3.0F;
               };
               if (state.darknessConstricting && qteTpTier == 3) {
                  qteTp = 2.0F;
               }

               state.addTpCapped(actor, qteTp);
               BattleNetworking.broadcastSharedTp(state, player.getServer().getPlayerManager());
            }

            if (rolledCrit) {
               float critDmg = dmg * 3.0F;
               applyBattleDamage = true;

               float vHpCrit;
               try {
                  vHpCrit = applyPlayerDamageToEnemy(state, enemy, player, critDmg);
               } finally {
                  applyBattleDamage = false;
               }

               if (targetIndex == 0) {
                  state.entityCurrentHp = vHpCrit;
               }

               state.enemyHpMap.put(enemyId, vHpCrit);
               return String.format("SMAAAASH!! %s dealt %.1f damage to %s!", actorName, dmg * 4.0F, targetName);
            } else {
               String boost = state.getAttackBoostTurns(actor) > 0 ? " (POWERED UP)" : "";
               String burnSuffix = "";
               if (state.offhandGimmicks.getOrDefault(actor, OffhandGimmick.NONE) == OffhandGimmick.TORCH && RNG.nextFloat() < 0.25F) {
                  int newBurn = Math.max(state.enemyBurnTurnsMap.getOrDefault(enemyId, 0), 2);
                  state.setEnemyStatus(enemyId, BattleState.StatusKind.BURN);
                  state.enemyBurnTurnsMap.put(enemyId, newBurn);
                  if (enemyId == state.primaryEnemyId) {
                     state.enemyBurnTurns = newBurn;
                  }

                  burnSuffix = " [Burn 2!]";
               }

               return String.format("%s dealt %.1f damage to %s! (BASH%s)%s%s", actorName, dmg, targetName, boost, bashAffSuffix, burnSuffix);
            }
         }
      }
   }

   private static void initPvpShield(BattleState state, UUID uuid, BattleData.PlayerProgress prog, float maxHp) {
      state.pvpShield.put(uuid, 0.0F);
      state.pvpShieldMax.put(uuid, 0.0F);
      state.pvpShieldBroken.put(uuid, true);
   }

   private static float applyPlayerWeaknessMultiplier(ServerPlayerEntity attacker, UUID defenderUuid, BattleState state, float dmg, Affinity[] atkElements) {
      if (atkElements != null && atkElements.length != 0) {
         WeaponStats defenderWs = resolveActorWeaponStats(attacker, defenderUuid, state, null);
         Affinity[] defenderElements = defenderWs != null ? defenderWs.getElements() : null;
         Set weaknesses = Affinity.weaknessesForWeapon(defenderElements);

         for (Affinity a : atkElements) {
            if (a != null && weaknesses.contains(a)) {
               return dmg * 2.0F;
            }
         }

         return dmg;
      } else {
         return dmg;
      }
   }

   private static float applyPvpDamageReduction(float dmg) {
      return dmg * 0.5F;
   }

   private static float applySoulShieldAbsorption(BattleState state, UUID defender, float dmg) {
      boolean shieldBroken = state.pvpShieldBroken.getOrDefault(defender, true);
      if (shieldBroken) {
         state.lastPvpHitFullyShielded = false;
         return dmg;
      } else {
         float shield = state.pvpShield.getOrDefault(defender, 0.0F);
         float newShield = shield - dmg;
         if (newShield <= 0.0F) {
            float overflow = -newShield;
            state.pvpShield.put(defender, 0.0F);
            state.pvpShieldBroken.put(defender, true);
            state.lastPvpHitFullyShielded = false;
            return overflow;
         } else {
            state.pvpShield.put(defender, newShield);
            state.lastPvpHitFullyShielded = dmg > 0.0F;
            return 0.0F;
         }
      }
   }

   private static float pvpApplyDamage(ServerPlayerEntity attacker, UUID actor, BattleState state, float dmg) {
      return pvpApplyDamage(attacker, actor, state, dmg, new Affinity[]{Affinity.SLASH});
   }

   private static float pvpApplyDamage(ServerPlayerEntity attacker, UUID actor, BattleState state, float dmg, Affinity[] atkElements) {
      UUID opponentUuid = primaryEnemyOf(state, actor, attacker.getServer().getPlayerManager());
      return pvpApplyDamage(attacker, actor, opponentUuid, state, dmg, atkElements);
   }

   private static float pvpApplyDamage(ServerPlayerEntity attacker, UUID actor, UUID opponentUuid, BattleState state, float dmg, Affinity[] atkElements) {
      if (state.isPvp && isDowned(state, opponentUuid, attacker.getServer().getPlayerManager())) {
         return 0.0F;
      } else {
         if (state.isPvp && actor != null) {
            state.pvpAttackedThisRound.add(actor);
         }

         dmg = applyPlayerWeaknessMultiplier(attacker, opponentUuid, state, dmg, atkElements);
         dmg = applyPvpDamageReduction(dmg);
         dmg = applySoulShieldAbsorption(state, opponentUuid, dmg);
         dmg = Math.max(0.0F, dmg * (1.0F - state.getShieldReduction(opponentUuid)));
         if (state.isDefending(opponentUuid)) {
            dmg *= 0.5F;
         }

         ServerPlayerEntity ep = attacker.getServer().getPlayerManager().getPlayer(opponentUuid);
         float opponentMaxHp = ep != null ? ep.getMaxHealth() : 20.0F;
         dmg = Math.min(dmg, opponentMaxHp * 0.35F);
         dmg = heartLocketCut(attacker, state, opponentUuid, dmg);
         float cur = TARGET_HEALTH.getOrDefault(opponentUuid, ep != null ? ep.getHealth() : 20.0F);
         float next = Math.max(0.0F, cur - dmg);
         TARGET_HEALTH.put(opponentUuid, next);
         state.entityCurrentHp = next;
         state.enemyHpMap.put(state.entityNetworkId, next);
         state.participantHp.put(opponentUuid, next);
         if (next <= 0.0F && cur > 0.0F && ep != null) {
            handlePlayerKO(ep, opponentUuid, attacker.getServer());
            return dmg;
         } else {
            if (ep != null) {
               if (!TurnBattleConfig.get().rollingHealth) {
                  IS_ROLLING = true;

                  try {
                     ep.setHealth(Math.max(0.5F, next));
                  } finally {
                     IS_ROLLING = false;
                  }
               }

               PacketByteBuf hpBuf = PacketByteBufs.create();
               hpBuf.writeFloat(next);
               Net121.sendToClient(ep, TurnBattleMod.HP_SYNC, hpBuf);
            }

            return dmg;
         }
      }
   }

   private static String doBashPvp(ServerPlayerEntity player, UUID actor, BattleState state, int targetIndex) {
      PlayerManager pm = player.getServer().getPlayerManager();
      setPvpTargetFromIndex(state, actor, targetIndex, pm);
      UUID opponentUuid = primaryEnemyOf(state, actor, pm);
      ServerPlayerEntity target = pm.getPlayer(opponentUuid);
      if (target == null) {
         return "Opponent is gone!";
      } else {
         BattleData data = BattleData.get(player.getServer());
         BattleData.PlayerProgress actorProg = data.getOrCreate(actor);
         CharStats actorStats = getEffectiveStats(actor, actorProg);
         double weaponDmg = 4.0;
         double itemDmg = 1.0;
         if (actorProg.equippedWeapon != null && !actorProg.equippedWeapon.isEmpty()) {
            AttributeModifiersComponent attrComp = (AttributeModifiersComponent)actorProg.equippedWeapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attrComp != null) {
               for (net.minecraft.component.type.AttributeModifiersComponent.Entry entry : attrComp.modifiers()) {
                  if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && entry.modifier().operation() == Operation.ADD_VALUE) {
                     itemDmg += entry.modifier().value();
                  }
               }
            }
         }

         weaponDmg = Math.max(4.0, itemDmg);
         WeaponStats ws = WeaponStats.fromStack(actorProg.equippedWeapon);
         if (ws != null) {
            weaponDmg += (double)ws.bashBonus();
         }

         double scaled = weaponDmg * (1.0 + (double)actorStats.offenseBonus());
         if (state.getAttackBoostTurns(actor) > 0) {
            scaled *= (double)state.getAttackBoostMult(actor);
         }

         BattleData.PlayerProgress tProg = data.getOrCreate(opponentUuid);
         float dmg = Math.max(1.0F, getEffectiveStats(opponentUuid, tProg).afterDefense((float)scaled));
         String actorName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
         boolean crit = RNG.nextFloat() < actorStats.critChance();
         if (crit) {
            dmg *= 4.0F;
         }

         if (state.playerBurned.contains(actor)) {
            dmg *= 0.5F;
         }

         float actualDmg = pvpApplyDamage(player, actor, state, dmg);
         String shieldNote = state.lastPvpHitFullyShielded ? " (absorbed by Soul Shield!)" : "";
         player.networkHandler
            .sendPacket(
               new PlaySoundS2CPacket(
                  Registries.SOUND_EVENT.getEntry(TurnBattleSounds.DAMAGE), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F, 0L
               )
            );
         String boost = state.getAttackBoostTurns(actor) > 0 ? " (POWERED UP)" : "";
         return crit
            ? String.format("SMAAAASH!! %s dealt %.1f damage to %s!%s", actorName, actualDmg, getPlayerDisplayName(target), shieldNote)
            : String.format("%s dealt %.1f damage to %s! (BASH%s)%s", actorName, actualDmg, getPlayerDisplayName(target), boost, shieldNote);
      }
   }

   private static int monsterSoulsFor(BattleSkill skill, UUID actor, boolean actorIsAnyPartyMember, BattleData.PlayerProgress actorProg) {
      if (!(skill instanceof MonsterSkill) || actorIsAnyPartyMember || actorProg == null) {
         return 0;
      } else {
         return actorProg.playerRace == CharacterRace.MONSTER && actorProg.absorbedSouls != null ? actorProg.absorbedSouls.size() : 0;
      }
   }

   private static BattleSkill decodeActorSkill(
      ServerPlayerEntity player, UUID actor, BattleState state, int ordinal, PlayerClass cls, BattleData.PlayerProgress actorProg, Set<String> actorSkills
   ) {
      boolean actorIsAnyPartyMember = isRecruitSlot(actor);
      Object skill;
      if (cls == null) {
         skill = getRogueSkillByIndex(player.getServer(), actorIsAnyPartyMember ? state.playerUuid : actor, ordinal);
      } else if (ordinal == -99) {
         skill = VesselActionSkill.INSTANCE;
      } else if (ordinal >= 5000 && ordinal < 6000) {
         int oOrd = ordinal - 5000;
         OriSkill[] v = OriSkill.values();
         skill = oOrd < v.length ? v[oOrd] : null;
         if (skill != null && !actorSkills.contains(((Enum)skill).name())) {
            skill = null;
         }
      } else if (ordinal >= 4000 && ordinal < 5000) {
         int smOrd = ordinal - 4000;
         SoulMageSkill[] v = SoulMageSkill.values();
         skill = smOrd < v.length ? v[smOrd] : null;
         if (skill != null && !actorSkills.contains(((Enum)skill).name())) {
            skill = null;
         }
      } else if (ordinal >= 3000 && ordinal < 4000) {
         int lOrd = ordinal - 3000;
         LightnerSkill[] v = LightnerSkill.values();
         skill = lOrd < v.length ? v[lOrd] : null;
         if (skill != null && !actorSkills.contains(((Enum)skill).name())) {
            skill = null;
         }
      } else if (ordinal >= 2000 && ordinal < 3000) {
         int mOrd = ordinal - 2000;
         MonsterSkill[] v = MonsterSkill.values();
         skill = mOrd < v.length ? v[mOrd] : null;
         if (skill != null) {
            boolean raceOk = actorIsAnyPartyMember || actorProg.playerRace == CharacterRace.MONSTER;
            if (!raceOk || !actorSkills.contains(((Enum)skill).name())) {
               skill = null;
            }
         }
      } else if (ordinal >= 1000 && ordinal < 2000) {
         int hOrd = ordinal - 1000;
         HumanMageSkill[] v = HumanMageSkill.values();
         skill = hOrd < v.length ? v[hOrd] : null;
         if (skill != null) {
            boolean raceOk = actorIsAnyPartyMember || actorProg.playerRace == CharacterRace.HUMAN || state.depthsRun;
            if (!raceOk || !actorSkills.contains(((Enum)skill).name())) {
               skill = null;
            }
         }
      } else {
         PsiSkill[] v = PsiSkill.values();
         skill = ordinal >= 0 && ordinal < v.length ? v[ordinal] : null;
         if (skill != null && !actorSkills.contains(((Enum)skill).name())) {
            skill = null;
         }
      }

      return skill;
   }

   private static float multiHitRoll(BattleSkill s) {
      multiHitLast = multiHitLast < 0.0F ? (float)s.rollScaled(RNG) : multiHitLast * 0.85F;
      return multiHitLast;
   }

   private static String doSkill(
      ServerPlayerEntity player, UUID actor, BattleState state, int ordinal, int level, PlayerClass cls, int targetIndex, ServerWorld world
   ) {
      state.lastActionHitCount = 1;
      boolean actorIsAnyPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || actor.equals(BattleState.PARTY_MEMBER_UUID_2);
      BattleData.PlayerProgress actorProg = BattleData.get(player.getServer()).getOrCreate(actor);
      Set actorSkills = resolveActorUnlockedSkills(player, state, actor, actorProg);
      BattleSkill skill = decodeActorSkill(player, actor, state, ordinal, cls, actorProg, actorSkills);
      if (skill == null) {
         return "Invalid skill.";
      } else {
         int monsterSouls = monsterSoulsFor(skill, actor, actorIsAnyPartyMember, actorProg);
         if (skill == MonsterSkill.HYPER_GONER) {
            LivingEntity hyperTarget = state.isVirtualEnemy ? null : (world.getEntityById(state.primaryEnemyId) instanceof LivingEntity le ? le : null);
            Affinity weakness = hyperTarget != null ? MobLevelManager.getMobWeakness(hyperTarget) : null;
            if (weakness != null) {
               final Affinity finalWeak = weakness;
               skill = new BattleSkill() {
                  @Override
                  public String getDisplayName() {
                     return MonsterSkill.HYPER_GONER.getDisplayName();
                  }

                  @Override
                  public int getTpCost() {
                     return MonsterSkill.HYPER_GONER.getTpCost();
                  }

                  @Override
                  public int getLevelRequired() {
                     return MonsterSkill.HYPER_GONER.getLevelRequired();
                  }

                  @Override
                  public SkillType getType() {
                     return MonsterSkill.HYPER_GONER.getType();
                  }

                  @Override
                  public int getMinVal() {
                     return MonsterSkill.HYPER_GONER.getMinVal();
                  }

                  @Override
                  public int getMaxVal() {
                     return MonsterSkill.HYPER_GONER.getMaxVal();
                  }

                  @Override
                  public int getExtra() {
                     return MonsterSkill.HYPER_GONER.getExtra();
                  }

                  @Override
                  public String getEffectDescription() {
                     return MonsterSkill.HYPER_GONER.getEffectDescription();
                  }

                  @Override
                  public int getOrdinal() {
                     return MonsterSkill.HYPER_GONER.getOrdinal();
                  }

                  @Override
                  public Affinity[] getAffinities() {
                     return new Affinity[]{finalWeak};
                  }
               };
            }
         }

         if (cls != null && skill.getLevelRequired() > level) {
            return "Can't use that yet.";
         } else {
            int actualTpCost = skill.getTpCost();
            if (skill == LightnerSkill.SNOWGRAVE) {
               actualTpCost = LightnerSkill.snowgraveCost(maxTpFor(state, actor));
            }

            BattleData.PlayerProgress progForGimmick = BattleData.get(player.getServer()).getOrCreate(actor);
            if (resolveActorGimmick(player, state, actor, progForGimmick) == OffhandGimmick.THORN_RING) {
               actualTpCost = (int)Math.ceil((double)((float)actualTpCost * 0.5F));
            }

            if (state.isPvp && skill == VesselActionSkill.INSTANCE) {
               actualTpCost = 75;
            }

            boolean resonatorHpCost = !state.isPvp
               && cls == PlayerClass.SOUL_MAGE
               && state.offhandGimmicks.getOrDefault(actor, OffhandGimmick.NONE) == OffhandGimmick.SOUL_RESONATOR;
            if (resonatorHpCost) {
               float curHp = player.getHealth();
               if (curHp <= (float)actualTpCost) {
                  return "Not enough HP!";
               }
            } else if (state.getTp(actor) < (float)actualTpCost) {
               return "Not enough TP!";
            }

            if (state.isPvp && skill.getType() == SkillType.HEAL) {
               if (state.pvpHealsUsed.getOrDefault(actor, 0) >= 2) {
                  return "No more healing spells left this battle!";
               }

               state.pvpHealsUsed.merge(actor, 1, Integer::sum);
            }

            UUID healTarget = null;
            if ((skill.getType() == SkillType.HEAL || skill.getType() == SkillType.SHIELD || skill.getType() == SkillType.BUFF) && targetIndex >= 10) {
               int pidx = targetIndex - 10;
               List actorTeammates = state.teammatesOf(actor);
               if (pidx >= 0 && pidx < actorTeammates.size()) {
                  UUID candidate = actorTeammates.get(pidx);
                  if (!state.isPvp || state.isAllyPair(actor, candidate)) {
                     healTarget = candidate;
                  }
               }
            }

            LivingEntity enemy = null;
            int enemyId = -1;
            if (skill.getType() == SkillType.HEAL || skill.getType() == SkillType.SHIELD || skill.getType() == SkillType.BUFF) {
               enemy = state.isPvp ? null : findEnemyByIndex(state, world, 0);
               enemyId = state.entityNetworkId;
            } else if (state.isPvp) {
               PlayerManager pmSkillTarget = player.getServer().getPlayerManager();
               setPvpTargetFromIndex(state, actor, targetIndex, pmSkillTarget);
               UUID opponentUuid = primaryEnemyOf(state, actor, pmSkillTarget);
               enemy = player.getServer().getPlayerManager().getPlayer(opponentUuid);
               enemyId = state.entityNetworkId;
               if (enemy == null && !state.isVirtualEnemy) {
                  return "Opponent is gone!";
               }
            } else {
               enemy = findEnemyByIndex(state, world, targetIndex);
               enemyId = getEnemyIdByIndex(state, targetIndex);
               if (enemy == null
                  && !state.isVirtualEnemy
                  && skill.getType() != SkillType.SPY
                  && skill.getType() != SkillType.RAISE_THRESHOLD
                  && skill.getType() != SkillType.SPARE) {
                  return "Target is gone!";
               }
            }

            if ((skill.getType() == SkillType.RAISE_THRESHOLD || skill.getType() == SkillType.SPARE) && enemyId == -1) {
               enemyId = state.primaryEnemyId;
            }

            if (resonatorHpCost) {
               boolean rollingCost = TurnBattleConfig.get().rollingHealth && !state.isPvp;
               float base = rollingCost ? TARGET_HEALTH.getOrDefault(actor, player.getHealth()) : player.getHealth();
               float newHp = base > 0.0F ? Math.max(1.0F, base - (float)actualTpCost) : base - (float)actualTpCost;
               if (!rollingCost) {
                  IS_ROLLING = true;

                  try {
                     player.setHealth(newHp);
                  } finally {
                     IS_ROLLING = false;
                  }
               }

               TARGET_HEALTH.put(actor, newHp);
               state.participantHp.put(actor, newHp);
            } else {
               boolean var162;
               label1391: {
                  if (skill instanceof SoulMageSkill sms
                     && (
                        sms == SoulMageSkill.BRA_MEND
                           || sms == SoulMageSkill.BRA_MEND_P
                           || sms == SoulMageSkill.BRA_MEND_PP
                           || sms == SoulMageSkill.BRA_CHANNEL
                     )
                     && world.random.nextFloat() < 0.5F) {
                     var162 = true;
                     break label1391;
                  }

                  var162 = false;
               }

               boolean braveryFreeTpChance = var162;
               if (!braveryFreeTpChance) {
                  state.addTp(actor, (float)(-actualTpCost));
               }

               BattleNetworking.broadcastSharedTp(state, player.getServer().getPlayerManager());
            }

            boolean psiDouble = cls == PlayerClass.PSI_USER && state.offhandGimmicks.getOrDefault(actor, OffhandGimmick.NONE) == OffhandGimmick.PSI_PROCESSOR;
            BattleData dataForMagic = BattleData.get(player.getServer());
            BattleData.PlayerProgress progForMagic = dataForMagic.getOrCreate(state.playerUuid);
            BattleData.PlayerProgress actorProgForMagic = dataForMagic.getOrCreate(actor);
            int actorMagic = resolveActorStat(player, state, actor, progForMagic, actorProgForMagic, SkillScaling.statIndex(skill));
            boolean stab = false;
            WeaponStats actorWsForStab = resolveActorWeaponStats(player, actor, state, progForMagic);
            if (actorWsForStab != null) {
               for (Affinity skillAff : skill.getAffinities()) {
                  if (actorWsForStab.hasElement(skillAff)) {
                     stab = true;
                     break;
                  }
               }
            }

            SkillBonus.magicFlat = SkillBonus.forSkill(actorWsForStab, skill);
            float scale = (psiDouble ? 2.0F : 1.0F)
               * (1.0F + (float)actorMagic * 0.017F)
               * (stab ? 1.5F : 1.0F)
               * (state.playerFreezeMap.getOrDefault(actor, 0) > 0 ? 0.5F : 1.0F)
               * MonsterSkill.soulPowerMult(monsterSouls);
            if (SkillScaling.usesOffense(skill) && !state.isPvp && skill.getMinVal() > 0 && skill.getMinVal() != 999) {
               SkillType sty = skill.getType();
               if (sty == SkillType.DAMAGE
                  || sty == SkillType.BYPASS
                  || sty == SkillType.FREEZE
                  || sty == SkillType.MULTI
                  || sty == SkillType.AOE
                  || sty == SkillType.BURN) {
                  float avgHit = (float)(skill.getMinVal() + skill.getMaxVal()) / 2.0F * BattleSkill.damageMult() + SkillBonus.magicFlat;
                  float have = avgHit * scale * hitWeight(skill);
                  float want = (float)(bashBaseDamage(player, actor, state) * (double)offenseSkillOverBash(skill));
                  if (have > 0.0F && have < want) {
                     scale *= want / have;
                  }
               }
            }

            if (state.playerConfusionMap.getOrDefault(actor, 0) > 0 && RNG.nextInt(100) < 15) {
               float selfDmg = (float)skill.rollScaled(RNG) * scale;
               PlayerManager pmConf = player.getServer().getPlayerManager();
               float curHp = getBattleHpFor(state, actor, pmConf);
               float newHp = curHp - selfDmg;
               setBattleHpFor(state, actor, newHp, pmConf);
               String confusedName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
               return String.format("%s is confused and hurts themselves for %.0f damage!", confusedName, selfDmg);
            } else {
               applyBattleDamage = true;

               String suffix;
               try {
                  if (skill == LightnerSkill.DUAL_HEAL) {
                     int healVal = skill.rollScaled(RNG);
                     float kindnessBonus = soulOf(player.getServer(), state, actor) == SoulType.KINDNESS ? 1.25F : 1.0F;
                     float total = 0.0F;
                     int count = 0;

                     for (UUID pid : state.teammatesOf(actor)) {
                        float heal = (float)healVal * scale * kindnessBonus;
                        PlayerManager pmH1 = player.getServer().getPlayerManager();
                        if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                           ServerPlayerEntity target = pmH1.getPlayer(pid);
                           if (target != null) {
                              float cur = getBattleHpFor(state, pid, pmH1);
                              float added = Math.min(heal, Math.max(0.0F, target.getMaxHealth() - cur));
                              applyDownableHeal(state, pid, heal, pmH1);
                              total += added;
                           }
                        } else {
                           float cur = getBattleHpFor(state, pid, pmH1);
                           float max = state.participantMaxHp.getOrDefault(pid, 1.0F);
                           float added = Math.min(heal, Math.max(0.0F, max - cur));
                           applyDownableHeal(state, pid, heal, pmH1);
                           total += added;
                        }

                        count++;
                     }

                     float avgHeal = total / (float)Math.max(1, count);
                     boolean firstHeal = true;

                     for (UUID pid : state.teammatesOf(actor)) {
                        BattleNetworking.broadcastHeal(state, player.getServer().getPlayerManager(), pid, avgHeal, firstHeal);
                        firstHeal = false;
                     }

                     return String.format("Dual Heal! Restored ~%.0f HP to all %d members!", avgHeal, count);
                  }

                  if (skill.getType() != SkillType.AOE) {
                     boolean isBurnMultiRandom = skill == PsiSkill.FIRE_B || skill == PsiSkill.FIRE_G || skill == PsiSkill.FIRE_O;
                     boolean isMultiRandom = skill.getExtra() == 2 && (skill.getType() == SkillType.DAMAGE || skill.getType() == SkillType.BYPASS)
                        || isBurnMultiRandom;
                     if (!isMultiRandom) {
                        return applySkill(player, actor, enemy, enemyId, state, skill, scale, healTarget);
                     }

                     if (state.isPvp) {
                        PlayerManager pmMr = player.getServer().getPlayerManager();
                        List pvpOpponents = new ArrayList<>(state.enemiesOf(actor));
                        pvpOpponents.removeIf(id -> isDowned(state, id, pmMr));
                        float total = 0.0F;
                        Set hitUuids = new LinkedHashSet<>();
                        if (!pvpOpponents.isEmpty()) {
                           multiHitLast = -1.0F;

                           for (int i = 0; i < 2; i++) {
                              UUID oppUuid = pvpOpponents.get(RNG.nextInt(pvpOpponents.size()));
                              float dmg;
                              if (skill.getType() == SkillType.BYPASS) {
                                 dmg = multiHitRoll(skill) * scale;
                              } else {
                                 float armor = enemy != null ? readEnemyArmor(state, enemy) * state.rogueliteArmorScale : 0.0F;
                                 float factor = Math.max(0.55F, 1.0F - armor * 0.04F);
                                 dmg = multiHitRoll(skill) * scale * factor;
                              }

                              total += pvpApplyDamage(player, actor, oppUuid, state, dmg, skill.getAffinities());
                              hitUuids.add(oppUuid);
                           }
                        }

                        if (isBurnMultiRandom && !hitUuids.isEmpty()) {
                           int burnChance = skill.getExtra();
                           if (!state.burnImmune && (burnChance == 0 || RNG.nextInt(100) < burnChance)) {
                              for (UUID opUuid : hitUuids) {
                                 state.setPlayerStatus(opUuid, BattleState.PlayerStatusKind.BURN);
                                 state.playerBurned.add(opUuid);
                              }
                           }
                        }

                        String suffixx = isBurnMultiRandom ? " (fire damage)" : (skill.getType() == SkillType.BYPASS ? " (armor ignored)" : "");
                        state.lastActionHitCount = 2;
                        return String.format("%s struck twice for %.0f total!%s", skill.getDisplayName(), total, suffixx);
                     }

                     float totalx = 0.0F;
                     Set hitEnemyIds = new LinkedHashSet<>();
                     if (!state.isVirtualEnemy) {
                        List living = new ArrayList<>();
                        living.add(state.primaryEnemyId);
                        living.addAll(state.extraEnemyIds);
                        living.removeIf(id -> {
                           if (world.getEntityById(id) instanceof LivingEntity l2 && l2.isAlive()) {
                              return false;
                           }

                           return true;
                        });
                        if (!living.isEmpty()) {
                           multiHitLast = -1.0F;

                           for (int i = 0; i < 2; i++) {
                              int eid = living.get(RNG.nextInt(living.size()));
                              Entity ent = world.getEntityById(eid);
                              if (ent instanceof LivingEntity) {
                                 LivingEntity le = (LivingEntity)ent;
                                 if (le.isAlive()) {
                                    float dmg;
                                    if (skill.getType() == SkillType.BYPASS) {
                                       dmg = multiHitRoll(skill) * scale * MobLevelManager.getMobDamageTaken(le);
                                    } else {
                                       float armor = (float)le.getAttributeValue(EntityAttributes.GENERIC_ARMOR) * state.rogueliteArmorScale;
                                       float factor = Math.max(0.55F, 1.0F - armor * 0.04F);
                                       dmg = multiHitRoll(skill) * scale * factor * MobLevelManager.getMobDamageTaken(le);
                                    }

                                    float[] affMr = applyAffinityModifiers(state, le, skill, dmg);
                                    dmg = affMr[0];
                                    float vHMr = applyMagicDamageToEnemy(state, le, player, dmg);
                                    totalx += dmg;
                                    hitEnemyIds.add(eid);
                                    if (eid == state.entityNetworkId) {
                                       state.entityCurrentHp = vHMr;
                                    }

                                    state.enemyHpMap.put(eid, vHMr);
                                 }
                              }
                           }
                        }
                     } else {
                        float armor = state.virtualArmor;
                        float factor = Math.max(0.55F, 1.0F - armor * 0.04F);
                        multiHitLast = -1.0F;

                        for (int ix = 0; ix < 2; ix++) {
                           float dmgx = skill.getType() == SkillType.BYPASS ? multiHitRoll(skill) * scale : multiHitRoll(skill) * scale * factor;
                           state.virtualHp = Math.max(0.0F, state.virtualHp - dmgx);
                           if (state.virtualHp <= 0.0F && state.bossLivesRemaining > 0) {
                              state.bossLivesRemaining--;
                              state.spareHpThreshold = 0.0F;
                              state.enemySpareThresholds.clear();
                              float restore = state.entityMaxHp;
                              state.virtualHp = restore;
                              broadcastLifeLost(
                                 state,
                                 state.entityName
                                    + " lost a life! "
                                    + state.bossLivesRemaining
                                    + " "
                                    + (state.bossLivesRemaining == 1 ? "life" : "lives")
                                    + " remaining!",
                                 player.getServer()
                              );
                           }

                           totalx += dmgx;
                        }

                        state.entityCurrentHp = state.virtualHp;
                        state.enemyHpMap.put(Integer.MIN_VALUE, state.virtualHp);
                        hitEnemyIds.add(Integer.MIN_VALUE);
                     }

                     if (isBurnMultiRandom) {
                        int burnLevel = skill == PsiSkill.FIRE_O ? 4 : (skill == PsiSkill.FIRE_G ? 3 : 2);
                        int burnChance = skill.getExtra();
                        boolean burnApplied = false;
                        if (!state.burnImmune && (burnChance == 0 || RNG.nextInt(100) < burnChance)) {
                           burnApplied = true;

                           for (int eid2 : hitEnemyIds) {
                              int newBurn2 = Math.max(state.enemyBurnTurnsMap.getOrDefault(eid2, 0), burnLevel);
                              state.setEnemyStatus(eid2, BattleState.StatusKind.BURN);
                              state.enemyBurnTurnsMap.put(eid2, newBurn2);
                              if (eid2 == state.primaryEnemyId) {
                                 state.enemyBurnTurns = newBurn2;
                              }
                           }
                        }

                        return String.format(
                           "%s hit %d target(s) for %.0f total! %s",
                           skill.getDisplayName(),
                           hitEnemyIds.size(),
                           totalx,
                           burnApplied ? "(Burn " + burnLevel + " applied!)" : "(Burn missed)"
                        );
                     }

                     suffix = skill.getType() == SkillType.BYPASS ? " (armor ignored)" : "";
                     state.lastActionHitCount = 2;
                     return String.format("%s struck twice for %.0f total!%s", skill.getDisplayName(), totalx, suffix);
                  }

                  int hits = skill.getExtra() > 0 ? skill.getExtra() : 1;
                  float totalDmg = 0.0F;
                  if (state.isVirtualEnemy) {
                     float armor = state.virtualArmor;
                     float factor = Math.max(0.55F, 1.0F - armor * 0.04F);
                     multiHitLast = -1.0F;

                     for (int h = 0; h < hits; h++) {
                        float dmgx = multiHitRoll(skill) * scale * factor;
                        state.virtualHp = Math.max(0.0F, state.virtualHp - dmgx);
                        if (state.virtualHp <= 0.0F && state.bossLivesRemaining > 0) {
                           state.bossLivesRemaining--;
                           state.spareHpThreshold = 0.0F;
                           state.enemySpareThresholds.clear();
                           float restore = state.entityMaxHp;
                           state.virtualHp = restore;
                           broadcastLifeLost(
                              state,
                              state.entityName
                                 + " lost a life! "
                                 + state.bossLivesRemaining
                                 + " "
                                 + (state.bossLivesRemaining == 1 ? "life" : "lives")
                                 + " remaining!",
                              player.getServer()
                           );
                        }

                        totalDmg += dmgx;
                     }

                     state.entityCurrentHp = state.virtualHp;
                     state.enemyHpMap.put(Integer.MIN_VALUE, state.virtualHp);
                     applyKindnessLifesteal(player, actor, state, totalDmg, skill);
                     return String.format("%s hit for %.0f total!", skill.getDisplayName(), totalDmg);
                  }

                  if (!state.isPvp) {
                     List allIds = new ArrayList<>();
                     allIds.add(state.primaryEnemyId);
                     allIds.addAll(state.extraEnemyIds);
                     int targetsHit = 0;

                     for (int eid : allIds) {
                        Entity ent = world.getEntityById(eid);
                        if (ent instanceof LivingEntity) {
                           LivingEntity le = (LivingEntity)ent;
                           if (le.isAlive()) {
                              targetsHit++;
                              float armor = (float)le.getAttributeValue(EntityAttributes.GENERIC_ARMOR) * state.rogueliteArmorScale;
                              float factor = Math.max(0.55F, 1.0F - armor * 0.04F);
                              float dtm = MobLevelManager.getMobDamageTaken(le);
                              float[] affAoe = applyAffinityModifiers(state, le, skill, 1.0F);
                              float affMult = affAoe[1];
                              float dmgThis = 0.0F;
                              float lastHp = le.getHealth();
                              multiHitLast = -1.0F;

                              for (int h = 0; h < hits; h++) {
                                 float dmgx = multiHitRoll(skill) * scale * factor * dtm * affMult;
                                 lastHp = applyMagicDamageToEnemy(state, le, player, dmgx);
                                 dmgThis += dmgx;
                              }

                              totalDmg += dmgThis;
                              if (eid == state.entityNetworkId) {
                                 state.entityCurrentHp = lastHp;
                              }

                              state.enemyHpMap.put(eid, lastHp);
                           }
                        }
                     }

                     applyKindnessLifesteal(player, actor, state, totalDmg, skill);
                     state.lastActionHitCount = hits;
                     if (hits <= 1) {
                        return String.format("%s hit %d target(s) for %.0f total!", skill.getDisplayName(), targetsHit, totalDmg);
                     }

                     return String.format("%s \u2014 %d hits on %d target(s) for %.0f total!", skill.getDisplayName(), hits * targetsHit, targetsHit, totalDmg);
                  }

                  PlayerManager pmAoe = player.getServer().getPlayerManager();
                  int targetsHit = 0;

                  for (UUID oppUuid : state.enemiesOf(actor)) {
                     if (!isDowned(state, oppUuid, pmAoe)) {
                        targetsHit++;
                        multiHitLast = -1.0F;

                        for (int h = 0; h < hits; h++) {
                           float dmgx = multiHitRoll(skill) * scale;
                           totalDmg += pvpApplyDamage(player, actor, oppUuid, state, dmgx, skill.getAffinities());
                        }
                     }
                  }

                  applyKindnessLifesteal(player, actor, state, totalDmg, skill);
                  state.lastActionHitCount = hits;
                  if (targetsHit == 0) {
                     return String.format("%s had no effect - no opponents left standing!", skill.getDisplayName());
                  }

                  if (hits <= 1 && targetsHit <= 1) {
                     return String.format("%s hit for %.0f total!", skill.getDisplayName(), totalDmg);
                  }

                  suffix = String.format("%s \u2014 hit %d opponent(s) for %.0f total!", skill.getDisplayName(), targetsHit, totalDmg);
               } finally {
                  applyBattleDamage = false;
               }

               return suffix;
            }
         }
      }
   }

   private static void ensureRogueFall(ServerPlayerEntity pp) {
      if (pp.isAlive() && RogueliteManager.isRunning(pp)) {
         forceKill(pp);
      }
   }

   static void forceKill(ServerPlayerEntity pp) {
      TARGET_HEALTH.remove(pp.getUuid());
      applyBattleDamage = true;

      try {
         pp.kill();
      } finally {
         applyBattleDamage = false;
      }
   }

   private static void rogueAdjustHp(BattleState state, UUID who, float delta, PlayerManager pm) {
      float cur = getBattleHpFor(state, who, pm);
      if (!(cur <= 0.0F) && delta != 0.0F) {
         float max = getMaxHpFor(state, who, pm);
         float next = Math.max(1.0F, Math.min(max, cur + delta));
         if (!(delta < 0.0F) || !(cur <= 1.0F)) {
            if (TurnBattleConfig.get().rollingHealth) {
               TARGET_HEALTH.put(who, next);
            } else {
               setBattleHpFor(state, who, next, pm);
            }
         }
      }
   }

   static float applyRogueHpScale(LivingEntity e, float scale) {
      EntityAttributeInstance inst = e.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
      if (inst != null) {
         inst.removeModifier(ROGUE_HP_MOD);
      }

      float base = e.getMaxHealth();
      float scaled = base * Math.max(1.0F, scale);
      if (inst != null && scaled > base + 0.01F) {
         inst.addPersistentModifier(new EntityAttributeModifier(ROGUE_HP_MOD, (double)(scaled - base), Operation.ADD_VALUE));
         IS_ROLLING = true;

         try {
            e.setHealth(e.getMaxHealth());
         } finally {
            IS_ROLLING = false;
         }

         return e.getMaxHealth();
      } else {
         float capped = base;
         if (e.getHealth() > base) {
            IS_ROLLING = true;

            try {
               e.setHealth(capped);
            } finally {
               IS_ROLLING = false;
            }
         }

         return base;
      }
   }

   private static BattleSkill getRogueSkillByIndex(MinecraftServer server, UUID playerUuid, int index) {
      RogueliteData rogueData = RogueliteData.get(server);
      RogueliteData.PlayerRogueData rd = rogueData.players.get(playerUuid);
      return rd != null && index >= 0 && index < rd.drawnSkills.size() ? RogueCards.skillFor(rd.drawnSkills.get(index)) : null;
   }

   private static String applySkill(
      ServerPlayerEntity player, UUID actor, LivingEntity enemy, int enemyId, BattleState state, BattleSkill skill, float scale, UUID healTarget
   ) {
      int val = skill.rollScaled(RNG);
      int ex = skill.getExtra();
      String n = skill.getDisplayName();
      if (MobFateNet.isIce(skill) && player.getWorld() instanceof ServerWorld iceWorld) {
         long iceNow = iceWorld.getTime();
         state.iceHitAt.put(enemyId, iceNow);
         if (skill.getType() == SkillType.AOE) {
            state.iceHitAt.put(state.primaryEnemyId, iceNow);

            for (int iceId : state.extraEnemyIds) {
               state.iceHitAt.put(iceId, iceNow);
            }
         }
      }
      return switch (skill.getType()) {
         case HEAL -> {
            if (skill instanceof OriSkill os && os == OriSkill.NIGHTBLOOM) {
               int healVal = skill.rollScaled(RNG);
               float kindnessBonus = soulOf(player.getServer(), state, actor) == SoulType.KINDNESS ? 1.25F : 1.0F;
               float total = 0.0F;
               int count = 0;

               for (UUID pid : state.teammatesOf(actor)) {
                  float heal = (float)healVal * kindnessBonus;
                  PlayerManager pmH2 = player.getServer().getPlayerManager();
                  if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                     ServerPlayerEntity target = pmH2.getPlayer(pid);
                     if (target != null) {
                        float cur = getBattleHpFor(state, pid, pmH2);
                        float added = Math.min(heal, Math.max(0.0F, target.getMaxHealth() - cur));
                        applyDownableHeal(state, pid, heal, pmH2);
                        total += added;
                     }
                  } else {
                     float cur = getBattleHpFor(state, pid, pmH2);
                     float max = state.participantMaxHp.getOrDefault(pid, 1.0F);
                     float added = Math.min(heal, Math.max(0.0F, max - cur));
                     applyDownableHeal(state, pid, heal, pmH2);
                     total += added;
                  }

                  count++;
               }

               float avgHeal2 = total / (float)Math.max(1, count);
               boolean firstHeal2 = true;

               for (UUID pid : state.teammatesOf(actor)) {
                  BattleNetworking.broadcastHeal(state, player.getServer().getPlayerManager(), pid, avgHeal2, firstHeal2);
                  firstHeal2 = false;
               }

               yield String.format("Nightbloom! Restored ~%.0f HP to all %d members!", avgHeal2, count);
            }

            UUID htarget = healTarget != null ? healTarget : actor;
            boolean isHealTargetPartyMember = htarget.equals(BattleState.PARTY_MEMBER_UUID) || htarget.equals(BattleState.PARTY_MEMBER_UUID_2);
            String targetName = state.participantNames.getOrDefault(htarget, getPlayerDisplayName(player));
            PlayerManager pmHp = player.getServer().getPlayerManager();
            float targetCurHp;
            float targetMaxHp;
            if (!isHealTargetPartyMember && pmHp.getPlayer(htarget) == null && !htarget.equals(actor)) {
               targetCurHp = 0.0F;
               targetMaxHp = 1.0F;
            } else {
               targetMaxHp = Math.max(1.0F, getMaxHpFor(state, htarget, pmHp));
               targetCurHp = getBattleHpFor(state, htarget, pmHp);
               if (TurnBattleConfig.get().rollingHealth && !state.isPvp && !isRecruitSlot(htarget)) {
                  targetCurHp = Math.min(targetCurHp, rolledHpFor(state, htarget, pmHp));
               }
            }

            float heal;
            if (val == 999) {
               heal = targetMaxHp;
            } else if (skill == SoulMageSkill.DET_MEND) {
               heal = targetCurHp / targetMaxHp <= 0.25F ? targetMaxHp : (float)val * scale;
            } else {
               heal = (float)val * scale;
            }

            if (soulOf(player.getServer(), state, actor) == SoulType.KINDNESS) {
               heal *= 1.25F;
            }

            if (isHealTargetPartyMember) {
               applyDownableHeal(state, htarget, heal, player.getServer().getPlayerManager());
            } else {
               PlayerManager pmH3 = player.getServer().getPlayerManager();
               ServerPlayerEntity targetEnt = pmH3.getPlayer(htarget);
               if (targetEnt != null) {
                  applyDownableHeal(state, htarget, heal, pmH3);
               } else if (htarget.equals(actor)) {
                  applyDownableHeal(state, actor, heal, pmH3);
               }
            }

            if (skill == SoulMageSkill.DET_MEND && targetCurHp / targetMaxHp <= 0.25F) {
               setBattleHpFor(state, htarget, targetMaxHp, player.getServer().getPlayerManager());
            }

            if (isHealTargetPartyMember) {
               state.participantHp.put(htarget, Math.min(state.participantMaxHp.getOrDefault(htarget, 1.0F), state.participantHp.getOrDefault(htarget, 0.0F)));
            }

            BattleNetworking.broadcastHeal(state, player.getServer().getPlayerManager(), htarget, Math.min(heal, targetMaxHp - targetCurHp + heal));
            String verb = skill == SoulMageSkill.DET_MEND && targetCurHp / targetMaxHp <= 0.25F ? "fully restored" : "restored";
            yield String.format("%s %s %.0f HP on %s!", n, verb, Math.min(heal, targetMaxHp - targetCurHp + heal), targetName);
         }
         case DAMAGE -> {
            float dmg = (float)val * scale;
            if (skill instanceof SoulMageSkill enduringSms
               && (enduringSms == SoulMageSkill.PER_STRIKE || enduringSms == SoulMageSkill.PER_STRIKE_P || enduringSms == SoulMageSkill.PER_STRIKE_PP)) {
               PlayerManager enduringPm = player.getServer().getPlayerManager();
               float curHp = getBattleHpFor(state, actor, enduringPm);
               float maxHp = getMaxHpFor(state, actor, enduringPm);
               float hpFrac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, curHp / maxHp)) : 1.0F;
               dmg *= 1.0F + (1.0F - hpFrac);
            }

            float armor = readEnemyArmor(state, enemy) * state.rogueliteArmorScale;
            float reduced = dmg * Math.max(0.55F, 1.0F - armor * 0.04F) * (enemy != null ? MobLevelManager.getMobDamageTaken(enemy) : 1.0F);
            float[] aff = applyAffinityModifiers(state, enemy, skill, reduced);
            reduced = aff[0];
            String affSuffix = aff[1] > 1.0F ? " [WEAK!]" : (aff[1] < 1.0F ? " [RESIST]" : "");
            if (state.isPvp) {
               float actualDmg = pvpApplyDamage(player, actor, state, reduced, skill.getAffinities());
               applyKindnessLifesteal(player, actor, state, actualDmg, skill);
               String shieldNote = state.lastPvpHitFullyShielded ? " (absorbed by Soul Shield!)" : "";
               yield String.format("%s dealt %.0f damage!%s%s", n, actualDmg, affSuffix, shieldNote);
            } else {
               float vHpMag = applyMagicDamageToEnemy(state, enemy, player, reduced);
               if (enemyId == state.entityNetworkId) {
                  state.entityCurrentHp = vHpMag;
               }

               state.enemyHpMap.put(enemyId, vHpMag);
               applyKindnessLifesteal(player, actor, state, reduced, skill);
               yield String.format("%s dealt %.0f damage!%s", n, reduced, affSuffix);
            }
         }
         case BYPASS -> {
            float dmg = (float)val * scale * (enemy != null ? MobLevelManager.getMobDamageTaken(enemy) : 1.0F);
            float[] aff = applyAffinityModifiers(state, enemy, skill, dmg);
            dmg = aff[0];
            String affSuffix = aff[1] > 1.0F ? " [WEAK!]" : (aff[1] < 1.0F ? " [RESIST]" : "");
            if (state.isPvp) {
               float actualDmg = pvpApplyDamage(player, actor, state, dmg, skill.getAffinities());
               applyKindnessLifesteal(player, actor, state, actualDmg, skill);
               String shieldNote = state.lastPvpHitFullyShielded ? " (absorbed by Soul Shield!)" : "";
               yield String.format("%s dealt %.0f damage! (armor ignored)%s%s", n, actualDmg, affSuffix, shieldNote);
            } else {
               float vHpMag = applyMagicDamageToEnemy(state, enemy, player, dmg);
               if (enemyId == state.entityNetworkId) {
                  state.entityCurrentHp = vHpMag;
               }

               state.enemyHpMap.put(enemyId, vHpMag);
               applyKindnessLifesteal(player, actor, state, dmg, skill);
               yield String.format("%s dealt %.0f damage! (armor ignored)%s", n, dmg, affSuffix);
            }
         }
         case FREEZE -> {
            float dmg = (float)val * scale;
            float armor = readEnemyArmor(state, enemy) * state.rogueliteArmorScale;
            float reduced = dmg * Math.max(0.55F, 1.0F - armor * 0.04F) * (enemy != null ? MobLevelManager.getMobDamageTaken(enemy) : 1.0F);
            if (state.isPvp) {
               float actualDmg = pvpApplyDamage(player, actor, state, reduced, skill.getAffinities());
               UUID freezeOpponent = primaryEnemyOf(state, actor, player.getServer().getPlayerManager());
               boolean guaranteedPvp = freezeIsSure(skill, ex);
               if (state.playerFreezeMap.getOrDefault(freezeOpponent, 0) > 0) {
                  yield String.format("%s dealt %.0f damage! (Already frozen)", n, actualDmg);
               } else if (!guaranteedPvp && !(RNG.nextFloat() < 0.5F)) {
                  yield String.format("%s dealt %.0f damage! (Freeze missed)", n, actualDmg);
               } else {
                  state.setPlayerStatus(freezeOpponent, BattleState.PlayerStatusKind.FREEZE);
                  state.playerFreezeMap.put(freezeOpponent, 3);
                  yield String.format("%s dealt %.0f damage and froze them for 3 turns!", n, actualDmg);
               }
            } else {
               float[] affF = applyAffinityModifiers(state, enemy, skill, reduced);
               reduced = affF[0];
               String affSuffixF = affF[1] > 1.0F ? " [WEAK!]" : (affF[1] < 1.0F ? " [RESIST]" : "");
               if (state.enemyBurnTurnsMap.getOrDefault(enemyId, 0) > 0) {
                  reduced *= 2.0F;
                  state.enemyBurnTurnsMap.remove(enemyId);
                  if (enemyId == state.primaryEnemyId) {
                     state.enemyBurnTurns = 0;
                  }

                  float vHpMag = applyMagicDamageToEnemy(state, enemy, player, reduced);
                  if (enemyId == state.entityNetworkId) {
                     state.entityCurrentHp = vHpMag;
                  }

                  state.enemyHpMap.put(enemyId, vHpMag);
                  yield String.format("\u2726 SHATTER! %s dealt %.0f damage! (Burn+Freeze destroyed!)", n, reduced);
               } else {
                  float vHpMag = applyMagicDamageToEnemy(state, enemy, player, reduced);
                  if (enemyId == state.entityNetworkId) {
                     state.entityCurrentHp = vHpMag;
                  }

                  state.enemyHpMap.put(enemyId, vHpMag);
                  int freezeTurns = Math.abs(ex);
                  boolean guaranteed = freezeIsSure(skill, ex);
                  if (state.freezeImmuneSet.contains(enemyId)) {
                     yield String.format("%s dealt %.0f damage! (Freeze blocked \u2014 enemy is immune)%s", n, reduced, affSuffixF);
                  } else if (state.enemyFreezeTurnsMap.getOrDefault(enemyId, 0) > 0) {
                     yield String.format("%s dealt %.0f damage! (Already frozen)%s", n, reduced, affSuffixF);
                  } else if (!guaranteed && !(RNG.nextFloat() < 0.5F)) {
                     yield String.format("%s dealt %.0f damage! (Freeze missed)%s", n, reduced, affSuffixF);
                  } else {
                     state.setEnemyStatus(enemyId, BattleState.StatusKind.FREEZE);
                     state.enemyFreezeTurnsMap.put(enemyId, freezeTurns);
                     if (enemyId == state.primaryEnemyId) {
                        state.enemyFreezeTurns = freezeTurns;
                     }

                     yield String.format("%s dealt %.0f damage + froze for %d turns!%s", n, reduced, freezeTurns, affSuffixF);
                  }
               }
            }
         }
         case SPY -> {
            if (!state.isVirtualEnemy && enemy != null) {
               Affinity weak = MobLevelManager.getMobWeakness(enemy);
               Affinity resist = MobLevelManager.getMobResistance(enemy);
               String weakStr = weak != null ? weak.displayName : "none";
               String resistStr = resist != null ? resist.displayName : "none";
               String targetName3 = state.enemyNames.getOrDefault(enemyId, state.entityName);
               if (skill == HumanMageSkill.ACT_CHECK) {
                  BattleData.PlayerProgress progCheck = BattleData.get(player.getServer()).getOrCreate(state.playerUuid);
                  float atk = getEnemyDamage(enemy, progCheck.level) * 1.5F;
                  float def = readEnemyArmor(state, enemy);
                  yield String.format("%s \u2014 ATK %.0f  DEF %.0f.  Weak to %s.  Resists %s.", targetName3, atk, def, weakStr, resistStr);
               } else {
                  yield String.format("Spy: %s is WEAK to %s, RESISTS %s.", targetName3, weakStr, resistStr);
               }
            } else {
               yield "Check: nothing to reveal!";
            }
         }
         case SHIELD -> {
            int shieldPct = skill.getMinVal();
            if (skill instanceof MonsterSkill) {
               BattleData.PlayerProgress sp = BattleData.get(player.getServer()).getOrCreate(actor);
               int souls = monsterSoulsFor(skill, actor, actor.equals(BattleState.PARTY_MEMBER_UUID) || actor.equals(BattleState.PARTY_MEMBER_UUID_2), sp);
               shieldPct = Math.min(90, shieldPct + 2 * souls);
            }

            int turns = skill.getMaxVal();
            float bonusHeal = (float)ex;
            UUID shieldTarget = healTarget != null ? healTarget : actor;
            String shieldTargetName = state.participantNames.getOrDefault(shieldTarget, getPlayerDisplayName(player));
            state.setPlayerStatus(shieldTarget, BattleState.PlayerStatusKind.SHIELD);
            state.shieldReduction.put(shieldTarget, (float)shieldPct / 100.0F);
            state.shieldTurnsLeft.put(shieldTarget, turns + 1);
            if (bonusHeal > 0.0F) {
               if (!shieldTarget.equals(BattleState.PARTY_MEMBER_UUID) && !shieldTarget.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                  ServerPlayerEntity shieldEnt = player.getServer().getPlayerManager().getPlayer(shieldTarget);
                  if (shieldEnt != null) {
                     float added = Math.min(bonusHeal, shieldEnt.getMaxHealth() - shieldEnt.getHealth());
                     shieldEnt.heal(bonusHeal);
                     if (!TurnBattleConfig.get().rollingHealth) {
                        TARGET_HEALTH.compute(shieldTarget, (k, v) -> Math.min(shieldEnt.getMaxHealth(), (v != null ? v : shieldEnt.getHealth()) + bonusHeal));
                     }

                     BattleNetworking.broadcastHeal(state, player.getServer().getPlayerManager(), shieldTarget, added);
                  } else if (shieldTarget.equals(actor)) {
                     float added = Math.min(bonusHeal, player.getMaxHealth() - player.getHealth());
                     player.heal(bonusHeal);
                     if (!TurnBattleConfig.get().rollingHealth) {
                        TARGET_HEALTH.compute(actor, (k, v) -> Math.min(player.getMaxHealth(), (v != null ? v : player.getHealth()) + bonusHeal));
                     }

                     BattleNetworking.broadcastHeal(state, player.getServer().getPlayerManager(), actor, added);
                  }
               } else {
                  float cur = state.participantHp.getOrDefault(shieldTarget, 0.0F);
                  float max = state.participantMaxHp.getOrDefault(shieldTarget, 1.0F);
                  float added = Math.min(bonusHeal, max - cur);
                  state.participantHp.put(shieldTarget, Math.min(max, cur + bonusHeal));
                  BattleNetworking.broadcastHeal(state, player.getServer().getPlayerManager(), shieldTarget, added);
               }
            }

            yield bonusHeal > 0.0F
               ? String.format("%s on %s: %.0f HP healed, %d%% shield for %d turns!", n, shieldTargetName, bonusHeal * scale, shieldPct, turns)
               : String.format("%s on %s! %d%% damage reduction for %d turns.", n, shieldTargetName, shieldPct, turns);
         }
         case SLEEP -> {
            if (state.isPvp) {
               UUID sleepOpponent = primaryEnemyOf(state, actor, player.getServer().getPlayerManager());
               if (state.playerSleepMap.getOrDefault(sleepOpponent, 0) > 0) {
                  yield n + " had no effect \u2014 they're already asleep!";
               } else if (RNG.nextInt(100) < ex) {
                  state.setPlayerStatus(sleepOpponent, BattleState.PlayerStatusKind.ASLEEP);
                  state.playerSleepMap.put(sleepOpponent, 1);
                  yield n + " worked! They fall asleep.";
               } else {
                  yield n + " had no effect!";
               }
            } else if (!state.isEnemyAsleep(enemyId) && !state.isSleepImmune(enemyId)) {
               if (RNG.nextInt(100) < ex) {
                  state.setEnemyStatus(enemyId, BattleState.StatusKind.SLEEP);
                  state.enemySleepTurnsMap.put(enemyId, 1);
                  if (enemyId == state.primaryEnemyId) {
                     state.enemySleepTurns = 1;
                  }

                  String targetName = state.enemyNames.getOrDefault(enemyId, "Enemy");
                  yield n + " worked! " + targetName + " falls asleep.";
               } else {
                  yield n + " had no effect!";
               }
            } else {
               yield n + " had no effect \u2014 enemy is immune to sleep!";
            }
         }
         case MAGNET -> {
            float tpGain = (float)val * scale * 0.5F;
            if (state.darknessConstricting) {
               tpGain *= 0.1F;
            }

            state.addTp(actor, tpGain);
            yield String.format("%s \u2014 gained %.0f TP!", n, tpGain);
         }
         case CONFUSE -> {
            if (state.isPvp) {
               UUID confOpponent = primaryEnemyOf(state, actor, player.getServer().getPlayerManager());
               if (state.playerConfusionMap.getOrDefault(confOpponent, 0) > 0) {
                  yield n + " had no effect \u2014 they're already confused!";
               } else if (ex != 0 && RNG.nextInt(100) >= ex) {
                  yield n + " had no effect!";
               } else {
                  state.setPlayerStatus(confOpponent, BattleState.PlayerStatusKind.CONFUSION);
                  state.playerConfusionMap.put(confOpponent, 1);
                  yield n + " confused them!";
               }
            } else if (!state.isEnemyConfused(enemyId) && !state.isConfuseImmune(enemyId)) {
               if (ex != 0 && RNG.nextInt(100) >= ex) {
                  yield n + " had no effect!";
               } else {
                  int confuseTurns = val + 1;
                  state.setEnemyStatus(enemyId, BattleState.StatusKind.CONFUSE);
                  state.enemyConfusedLeftMap.put(enemyId, confuseTurns);
                  if (enemyId == state.primaryEnemyId) {
                     state.enemyConfused = true;
                     state.enemyConfusedLeft = confuseTurns;
                  }

                  String targetName = state.enemyNames.getOrDefault(enemyId, "the enemy");
                  yield String.format("%s confused %s for %d turns!", n, targetName, val);
               }
            } else {
               yield n + " had no effect \u2014 enemy is immune to confusion!";
            }
         }
         case MULTI -> {
            float totalDmg = 0.0F;
            float armor = readEnemyArmor(state, enemy) * state.rogueliteArmorScale;
            float factor = Math.max(0.55F, 1.0F - armor * 0.04F);
            float dtm = !state.isPvp && enemy != null ? MobLevelManager.getMobDamageTaken(enemy) : 1.0F;
            float affMult = 1.0F;
            String affSuffixM = "";
            if (!state.isPvp && enemy != null && !state.isVirtualEnemy) {
               Affinity weak = MobLevelManager.getMobWeakness(enemy);
               Affinity resist = MobLevelManager.getMobResistance(enemy);
               if (weak != null && resist != null) {
                  boolean hitsWeak = false;
                  boolean hitsResist = false;

                  for (Affinity a : skill.getAffinities()) {
                     if (a == weak) {
                        hitsWeak = true;
                     }

                     if (a == resist) {
                        hitsResist = true;
                     }
                  }

                  if (!hitsWeak || !hitsResist) {
                     if (hitsResist) {
                        affMult = 0.5F;
                        affSuffixM = " [RESIST]";
                     } else if (hitsWeak) {
                        affMult = 1.3F;
                        affSuffixM = " [WEAK!]";
                        if (state.enemySturdy.getOrDefault(enemy.getId(), 0) <= 0) {
                           state.enemyDownTurns.put(enemy.getId(), 1);
                        }
                     }
                  }
               }
            }

            multiHitLast = -1.0F;

            for (int i = 0; i < ex; i++) {
               float dmg = multiHitRoll(skill) * scale * factor * dtm * affMult;
               if (state.isPvp) {
                  float actualDmg = pvpApplyDamage(player, actor, state, dmg, skill.getAffinities());
                  totalDmg += actualDmg;
               } else {
                  float vH = applyMagicDamageToEnemy(state, enemy, player, dmg);
                  if (enemyId == state.entityNetworkId) {
                     state.entityCurrentHp = vH;
                  }

                  state.enemyHpMap.put(enemyId, vH);
                  totalDmg += dmg;
               }
            }

            applyKindnessLifesteal(player, actor, state, totalDmg, skill);
            state.lastActionHitCount = ex;
            yield String.format("%s hit %d times for %.0f total!%s", n, ex, totalDmg, affSuffixM);
         }
         case BURN -> {
            if (skill == LightnerSkill.RED_BUSTER) {
               float armor = readEnemyArmor(state, enemy) * state.rogueliteArmorScale;
               float factor = Math.max(0.55F, 1.0F - armor * 0.04F);
               float dtm = !state.isPvp && enemy != null ? MobLevelManager.getMobDamageTaken(enemy) : 1.0F;
               float totalDmg = 0.0F;
               float totalActualDmg = 0.0F;
               multiHitLast = -1.0F;

               for (int i = 0; i < 4; i++) {
                  float hitDmg = multiHitRoll(skill) * scale * factor * dtm;
                  totalDmg += hitDmg;
                  if (state.isPvp) {
                     float actualHitDmg = pvpApplyDamage(player, actor, state, hitDmg, skill.getAffinities());
                     totalActualDmg += actualHitDmg;
                  } else {
                     float vH2 = applyMagicDamageToEnemy(state, enemy, player, hitDmg);
                     if (enemyId == state.entityNetworkId) {
                        state.entityCurrentHp = vH2;
                     }

                     state.enemyHpMap.put(enemyId, vH2);
                     totalActualDmg += hitDmg;
                  }
               }

               if (!state.isPvp) {
                  if (enemyId == state.entityNetworkId) {
                     state.entityCurrentHp = readEnemyHp(state, enemy);
                  }

                  state.enemyHpMap.put(enemyId, readEnemyHp(state, enemy));
               }

               boolean shatterReady = state.isPvp
                  ? state.playerFreezeMap.getOrDefault(primaryEnemyOf(state, actor, player.getServer().getPlayerManager()), 0) > 0
                  : state.enemyFreezeTurnsMap.getOrDefault(enemyId, 0) > 0;
               if (shatterReady) {
                  if (state.isPvp) {
                     UUID shatterOpponent = primaryEnemyOf(state, actor, player.getServer().getPlayerManager());
                     state.playerFreezeMap.remove(shatterOpponent);
                  } else {
                     state.enemyFreezeTurnsMap.remove(enemyId);
                     if (enemyId == state.primaryEnemyId) {
                        state.enemyFreezeTurns = 0;
                     }
                  }

                  if (!state.isPvp) {
                     float vHpMag = applyMagicDamageToEnemy(state, enemy, player, totalDmg);
                     if (enemyId == state.entityNetworkId) {
                        state.entityCurrentHp = vHpMag;
                     }

                     state.enemyHpMap.put(enemyId, vHpMag);
                     totalActualDmg += totalDmg;
                  } else {
                     float actualBonus = pvpApplyDamage(player, actor, state, totalDmg, skill.getAffinities());
                     totalActualDmg += actualBonus;
                  }

                  state.lastActionHitCount = 4;
                  yield String.format("\u2726 SHATTER! %s hit 4 times for %.0f total (doubled)!", n, totalActualDmg);
               } else {
                  boolean burnApplied = !state.burnImmune && (ex == 0 || RNG.nextInt(100) < ex);
                  if (burnApplied) {
                     if (state.isPvp) {
                        UUID burnOpponent = primaryEnemyOf(state, actor, player.getServer().getPlayerManager());
                        state.setPlayerStatus(burnOpponent, BattleState.PlayerStatusKind.BURN);
                        state.playerBurned.add(burnOpponent);
                     } else {
                        int nb = Math.max(state.enemyBurnTurnsMap.getOrDefault(enemyId, 0), 4);
                        state.setEnemyStatus(enemyId, BattleState.StatusKind.BURN);
                        state.enemyBurnTurnsMap.put(enemyId, nb);
                        if (enemyId == state.primaryEnemyId) {
                           state.enemyBurnTurns = nb;
                        }
                     }
                  }

                  state.lastActionHitCount = 4;
                  yield String.format("%s hit 4 times for %.0f total! %s", n, totalActualDmg, burnApplied ? "(Burn 4 applied!)" : "(Burn missed)");
               }
            } else if (skill == SoulMageSkill.BRA_SURGE) {
               float aoeTotal = 0.0F;
               int hitsLanded = 0;
               if (state.isVirtualEnemy) {
                  float armor = state.virtualArmor;
                  float aoeFactor = Math.max(0.55F, 1.0F - armor * 0.04F);
                  multiHitLast = -1.0F;

                  for (int h = 0; h < 4; h++) {
                     float hd = multiHitRoll(skill) * scale * aoeFactor;
                     float vH2 = applyMagicDamageToEnemy(state, null, player, hd);
                     state.entityCurrentHp = vH2;
                     state.enemyHpMap.put(Integer.MIN_VALUE, vH2);
                     aoeTotal += hd;
                  }

                  hitsLanded = 1;
                  if (!state.burnImmune && (ex == 0 || RNG.nextInt(100) < ex)) {
                     int nb2 = Math.max(state.enemyBurnTurnsMap.getOrDefault(Integer.MIN_VALUE, 0), 2);
                     state.setEnemyStatus(Integer.MIN_VALUE, BattleState.StatusKind.BURN);
                     state.enemyBurnTurnsMap.put(Integer.MIN_VALUE, nb2);
                     state.enemyBurnTurns = nb2;
                  }
               } else if (state.isPvp) {
                  PlayerManager pmSurge = player.getServer().getPlayerManager();

                  for (UUID oppUuid : state.enemiesOf(actor)) {
                     if (!isDowned(state, oppUuid, pmSurge)) {
                        multiHitLast = -1.0F;

                        for (int h = 0; h < 4; h++) {
                           float hd = multiHitRoll(skill) * scale;
                           aoeTotal += pvpApplyDamage(player, actor, oppUuid, state, hd, skill.getAffinities());
                        }

                        hitsLanded++;
                        if (!state.burnImmune && (ex == 0 || RNG.nextInt(100) < ex)) {
                           state.setPlayerStatus(oppUuid, BattleState.PlayerStatusKind.BURN);
                           state.playerBurned.add(oppUuid);
                        }
                     }
                  }
               } else {
                  ServerWorld surgeWorld = player.getServerWorld();
                  List aoeIds = new ArrayList<>();
                  aoeIds.add(state.primaryEnemyId);
                  aoeIds.addAll(state.extraEnemyIds);

                  for (int aoeEid : aoeIds) {
                     Entity aoeEnt = surgeWorld.getEntityById(aoeEid);
                     if (aoeEnt instanceof LivingEntity) {
                        LivingEntity aoeLE = (LivingEntity)aoeEnt;
                        if (aoeLE.isAlive()) {
                           float aoeArmor = (float)aoeLE.getAttributeValue(EntityAttributes.GENERIC_ARMOR) * state.rogueliteArmorScale;
                           float aoeFactor = Math.max(0.55F, 1.0F - aoeArmor * 0.04F);
                           float dtm2 = MobLevelManager.getMobDamageTaken(aoeLE);
                           float lastAoeHp = 0.0F;
                           multiHitLast = -1.0F;

                           for (int h = 0; h < 4; h++) {
                              float hd = multiHitRoll(skill) * scale * aoeFactor * dtm2;
                              lastAoeHp = applyMagicDamageToEnemy(state, aoeLE, player, hd);
                              aoeTotal += hd;
                           }

                           state.enemyHpMap.put(aoeEid, lastAoeHp);
                           if (aoeEid == state.entityNetworkId) {
                              state.entityCurrentHp = lastAoeHp;
                           }

                           hitsLanded++;
                           if (!state.burnImmune && (ex == 0 || RNG.nextInt(100) < ex)) {
                              int nb2 = Math.max(state.enemyBurnTurnsMap.getOrDefault(aoeEid, 0), 2);
                              state.setEnemyStatus(aoeEid, BattleState.StatusKind.BURN);
                              state.enemyBurnTurnsMap.put(aoeEid, nb2);
                              if (aoeEid == state.primaryEnemyId) {
                                 state.enemyBurnTurns = nb2;
                              }
                           }
                        }
                     }
                  }
               }

               state.lastActionHitCount = 4;
               yield String.format("%s \u2014 4 hits on %d target(s) for %.0f total! (Burn 2 chance)", n, hitsLanded, aoeTotal);
            } else {
               float dmg = (float)val * scale;
               float armor = readEnemyArmor(state, enemy) * state.rogueliteArmorScale;
               float reduced = dmg * Math.max(0.55F, 1.0F - armor * 0.04F) * (!state.isPvp && enemy != null ? MobLevelManager.getMobDamageTaken(enemy) : 1.0F);
               float[] affB = applyAffinityModifiers(state, enemy, skill, reduced);
               reduced = affB[0];
               String affSuffixB = affB[1] > 1.0F ? " [WEAK!]" : (affB[1] < 1.0F ? " [RESIST]" : "");
               float actualReducedDmg = reduced;
               if (state.isPvp) {
                  actualReducedDmg = pvpApplyDamage(player, actor, state, reduced, skill.getAffinities());
               } else {
                  float vHpMag = applyMagicDamageToEnemy(state, enemy, player, reduced);
                  if (enemyId == state.entityNetworkId) {
                     state.entityCurrentHp = vHpMag;
                  }

                  state.enemyHpMap.put(enemyId, vHpMag);
               }

               int burnLevel;
               if (skill == PsiSkill.FIRE_O) {
                  burnLevel = 4;
               } else if (skill == PsiSkill.FIRE_G) {
                  burnLevel = 3;
               } else {
                  burnLevel = 2;
               }

               boolean shatterReadyB = state.isPvp
                  ? state.playerFreezeMap.getOrDefault(primaryEnemyOf(state, actor, player.getServer().getPlayerManager()), 0) > 0
                  : state.enemyFreezeTurnsMap.getOrDefault(enemyId, 0) > 0;
               if (shatterReadyB) {
                  if (state.isPvp) {
                     UUID shatterOpponentB = primaryEnemyOf(state, actor, player.getServer().getPlayerManager());
                     state.playerFreezeMap.remove(shatterOpponentB);
                  } else {
                     state.enemyFreezeTurnsMap.remove(enemyId);
                     if (enemyId == state.primaryEnemyId) {
                        state.enemyFreezeTurns = 0;
                     }
                  }

                  float actualShatterDmg = reduced;
                  if (!state.isPvp) {
                     float vHpMag = applyMagicDamageToEnemy(state, enemy, player, reduced);
                     if (enemyId == state.entityNetworkId) {
                        state.entityCurrentHp = vHpMag;
                     }

                     state.enemyHpMap.put(enemyId, vHpMag);
                  } else {
                     actualShatterDmg = pvpApplyDamage(player, actor, state, reduced, skill.getAffinities());
                  }

                  yield String.format("\u2726 SHATTER! %s dealt %.0f damage! (Freeze+Burn destroyed!)", n, actualReducedDmg + actualShatterDmg);
               } else if (state.burnImmune) {
                  yield String.format("%s dealt %.0f damage! (Burn blocked \u2014 enemy is immune)%s", n, actualReducedDmg, affSuffixB);
               } else {
                  boolean burnHit = ex == 0 || RNG.nextInt(100) < ex;
                  if (burnHit) {
                     int existing = state.enemyBurnTurnsMap.getOrDefault(enemyId, 0);
                     int newBurn = Math.max(existing, burnLevel);
                     state.setEnemyStatus(enemyId, BattleState.StatusKind.BURN);
                     state.enemyBurnTurnsMap.put(enemyId, newBurn);
                     if (enemyId == state.primaryEnemyId) {
                        state.enemyBurnTurns = newBurn;
                     }

                     yield String.format("%s dealt %.0f damage + Burn %d applied!%s", n, actualReducedDmg, newBurn, affSuffixB);
                  } else {
                     yield String.format("%s dealt %.0f damage! (Burn missed)%s", n, actualReducedDmg, affSuffixB);
                  }
               }
            }
         }
         case PACIFY -> {
            if (state.isPvp) {
               yield n + " has no effect in PvP!";
            } else {
               boolean requiresSleep = skill == LightnerSkill.PACIFY || skill == LightnerSkill.C_ACTION;
               if (requiresSleep && !state.isEnemyAsleep(enemyId)) {
                  yield n + " had no effect. The enemy must be asleep first!";
               } else {
                  boolean alwaysWorks = ex == 0 || ex == 1;
                  boolean success = alwaysWorks || skill == HumanMageSkill.ACT_PLEAD && RNG.nextInt(100) < ex;
                  int thresholdPct;
                  if (skill == HumanMageSkill.ACT_TALK) {
                     thresholdPct = 40;
                  } else if (skill == HumanMageSkill.ACT_PLEAD) {
                     thresholdPct = 65;
                  } else if (skill == LightnerSkill.PACIFY) {
                     thresholdPct = 50;
                  } else if (skill == LightnerSkill.C_ACTION) {
                     thresholdPct = 75;
                  } else {
                     thresholdPct = 65;
                  }

                  if (success) {
                     float pacifyHp = state.enemyHpMap.getOrDefault(enemyId, state.entityCurrentHp);
                     float pacifyMax = state.enemyMaxHpMap.getOrDefault(enemyId, state.entityMaxHp);
                     boolean lowHp = pacifyMax > 0.0F && pacifyHp / pacifyMax <= (float)thresholdPct / 100.0F;
                     if (lowHp && !state.magicant) {
                        state.pendingPacifySpareId = enemyId;
                        if (ex == 1) {
                           float tp = (float)skill.rollScaled(RNG);
                           state.addTp(actor, tp);
                           yield String.format("%s! %s was spared and you gained %.0f TP!", n, state.enemyNames.getOrDefault(enemyId, state.entityName), tp);
                        } else {
                           yield String.format("%s! %s was spared!", n, state.enemyNames.getOrDefault(enemyId, state.entityName));
                        }
                     } else {
                        yield String.format("%s had no effect. Get the enemy to %d%% HP or less.", n, thresholdPct);
                     }
                  } else {
                     yield String.format("%s had no effect. (%.0f%% chance \u2014 get the enemy to %d%% HP or less.)", n, (float)ex, thresholdPct);
                  }
               }
            }
         }
         case BUFF -> {
            UUID buffTarget = healTarget != null ? healTarget : actor;
            String buffTargetName = state.participantNames.getOrDefault(buffTarget, getPlayerDisplayName(player));
            state.setPlayerStatus(buffTarget, BattleState.PlayerStatusKind.BOOST);
            state.attackBoostTurns.put(buffTarget, val + 1);
            float mult;
            if (skill instanceof LightnerSkill) {
               mult = 2.5F;
            } else if (skill.getExtra() > 0) {
               mult = (float)skill.getExtra() / 100.0F;
            } else {
               mult = 1.5F;
            }

            state.attackBoostMult.put(buffTarget, mult);
            if (skill == LightnerSkill.SHINING_SOUL && !buffTarget.equals(actor) && !state.isPvp) {
               state.pendingBonusTurnTarget = buffTarget;
            }

            yield String.format("%s on %s! Next %d BASHes deal %.1f\u00d7 damage.", n, buffTargetName, val, mult);
         }
         case WEAKEN -> {
            if (state.isPvp) {
               yield n + " has no effect in PvP!";
            } else if (state.getEnemyWeakenTurns(enemyId) <= 0 && !state.isWeakenImmune(enemyId)) {
               int weakenTurns = skill.getMinVal() + 1;
               if (SoulMageSkill.lowersDefense(skill)) {
                  state.setEnemyStatus(enemyId, BattleState.StatusKind.WEAKEN);
                  state.enemyArmorDebuffMap.put(enemyId, (float)ex / 100.0F);
                  state.enemyWeakenTurnsMap.put(enemyId, weakenTurns);
                  if (enemyId == state.primaryEnemyId) {
                     state.enemyArmorDebuff = (float)ex / 100.0F;
                     state.enemyWeakenTurns = weakenTurns;
                  }

                  String defName = state.enemyNames.getOrDefault(enemyId, "Enemy");
                  yield String.format("%s! %s's defense lowered by %d%% for %d turns.", n, defName, ex, skill.getMinVal());
               } else {
                  state.enemyAttackReduceMap.put(enemyId, (float)ex / 100.0F);
                  state.setEnemyStatus(enemyId, BattleState.StatusKind.WEAKEN);
                  state.enemyWeakenTurnsMap.put(enemyId, weakenTurns);
                  if (enemyId == state.primaryEnemyId) {
                     state.enemyAttackReduce = (float)ex / 100.0F;
                     state.enemyWeakenTurns = weakenTurns;
                  }

                  String targetName = state.enemyNames.getOrDefault(enemyId, "Enemy");
                  yield String.format("%s! %s's attack reduced by %d%% for %d turns.", n, targetName, ex, skill.getMinVal());
               }
            } else {
               yield n + " had no effect \u2014 enemy is immune to weakening!";
            }
         }
         case AOE -> n + " had no effect.";
         case SPARE, RAISE_THRESHOLD -> {
            if (state.isPvp) {
               boolean wasAsleep = state.playerSleepMap.getOrDefault(actor, 0) > 0;
               if (wasAsleep) {
                  state.playerSleepMap.remove(actor);
               }

               state.pendingSelfBonusTurn = true;
               String awokenName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
               yield wasAsleep ? String.format("%s awakens and moves again!", awokenName) : String.format("%s stays awake for another turn!", awokenName);
            } else {
               float addPct = (float)ex / 100.0F;
               state.raiseSpareThreshold(enemyId, addPct);
               float newThr = state.getSpareThreshold(enemyId);
               ServerPlayerEntity apl = player.getServer().getPlayerManager().getPlayer(actor);
               PlayerManager pmSkill = apl != null ? apl.getServer().getPlayerManager() : null;
               if (pmSkill != null) {
                  for (UUID skPid : state.participantOrder) {
                     if (!skPid.equals(BattleState.PARTY_MEMBER_UUID) && !skPid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                        ServerPlayerEntity skPp = pmSkill.getPlayer(skPid);
                        if (skPp != null) {
                           PacketByteBuf skBuf = PacketByteBufs.create();
                           skBuf.writeFloat(newThr);
                           skBuf.writeInt(enemyId);
                           Net121.sendToClient(skPp, TurnBattleMod.SPARE_THRESHOLD_UPDATE, skBuf);
                        }
                     }
                  }
               }

               String targetName2 = state.enemyNames.getOrDefault(enemyId, state.entityName);
               yield state.isMercyEligibleById(enemyId)
                  ? String.format("%s! %s can be spared now! (+%d%%)", n, targetName2, ex)
                  : String.format("%s! %s mercy: %d%%.", n, targetName2, Math.round(state.getSpareThreshold(enemyId) * 100.0F));
            }
         }
      };
   }

   public static void performAct(ServerPlayerEntity player, int actIndex, int targetEnemyIdx) {
      BattleState state = activeBattles.get(player.getUuid());
      if (state != null && state.phase == BattleState.Phase.PLAYER_TURN) {
         ServerWorld world = player.getServerWorld();
         UUID actingNow = state.currentParticipant();
         if (actingNow.equals(player.getUuid()) || isRecruitSlot(actingNow) && state.playerUuid.equals(player.getUuid())) {
            if (state.isPvp) {
               afterParticipantAction(player, state, "There's no showing mercy in a PvP fight!", false, world);
            } else if (!state.resolving) {
               if (!queuedTurns(state)) {
                  afterParticipantAction(player, state, runAct(player, state, actIndex, targetEnemyIdx, world, false), false, world);
               } else {
                  ActDefs.ActEntry entry = actEntryFor(state, player, actIndex, targetEnemyIdx);
                  state.actionQueue
                     .add(
                        new BattleState.QueuedAction(
                           actingNow, player.getUuid(), 7, actIndex, targetEnemyIdx, getEnemyIdByIndex(state, targetEnemyIdx), 0, 0, null
                        )
                     );
                  lastActionQueued = true;
                  broadcastReadyPose(state, player.getServer().getPlayerManager(), actingNow, 7);
                  int extraSkips = entry != null ? entry.participants() - 1 : 0;

                  for (int s = 0; s < extraSkips; s++) {
                     state.advanceTurn();
                  }

                  afterParticipantAction(player, state, "", false, world);
               }
            }
         }
      }
   }

   private static boolean isTacticianPlayer(BattleState state, ServerPlayerEntity player) {
      for (UUID pid : state.participantOrder) {
         if (!isRecruitSlot(pid)) {
            return pid.equals(player.getUuid());
         }
      }

      return false;
   }

   private static List<ActDefs.ActEntry> actListFor(BattleState state, int targetEnemyIdx) {
      String targetTypeKey = targetEnemyIdx == 0
         ? state.entityTypeKey
         : (
            targetEnemyIdx - 1 < state.extraEnemyIds.size()
               ? state.enemyTypeKeys.getOrDefault(state.extraEnemyIds.get(targetEnemyIdx - 1), state.entityTypeKey)
               : state.entityTypeKey
         );
      List acts = new ArrayList<>();

      for (ActDefs.ActEntry e : ActDefs.getActs(targetTypeKey)) {
         if (e.participants() <= state.participantOrder.size()) {
            acts.add(e);
         }
      }

      if (state.darknessConstricting) {
         acts.add(ActDefs.SHINE);
      }

      return acts;
   }

   private static ActDefs.ActEntry actEntryFor(BattleState state, ServerPlayerEntity player, int actIndex, int targetEnemyIdx) {
      if (!isTacticianPlayer(state, player)) {
         return null;
      } else {
         List acts = actListFor(state, targetEnemyIdx);
         return actIndex >= 0 && actIndex < acts.size() ? acts.get(actIndex) : null;
      }
   }

   private static String runAct(ServerPlayerEntity player, BattleState state, int actIndex, int targetEnemyIdx, ServerWorld world, boolean queued) {
      int enemyId = getEnemyIdByIndex(state, targetEnemyIdx);
      boolean isTactician = false;

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            isTactician = pid.equals(player.getUuid());
            break;
         }
      }

      if (!isTactician) {
         raiseMercy(state, player, enemyId, 0.05F, "You try to make peace with the enemy. (+5% Mercy)");
         return "";
      } else {
         String targetTypeKey = targetEnemyIdx == 0
            ? state.entityTypeKey
            : (
               targetEnemyIdx - 1 < state.extraEnemyIds.size()
                  ? state.enemyTypeKeys.getOrDefault(state.extraEnemyIds.get(targetEnemyIdx - 1), state.entityTypeKey)
                  : state.entityTypeKey
            );
         List actsRaw = ActDefs.getActs(targetTypeKey);
         int participantCount = state.participantOrder.size();
         List acts = new ArrayList<>();

         for (ActDefs.ActEntry e : actsRaw) {
            if (e.participants() <= participantCount) {
               acts.add(e);
            }
         }

         if (state.darknessConstricting) {
            acts.add(ActDefs.SHINE);
         }

         if (actIndex == 0) {
            LivingEntity checkEnemy = world.getEntityById(state.primaryEnemyId) instanceof LivingEntity le ? le : null;
            BattleData.PlayerProgress progCheck = BattleData.get(player.getServer()).getOrCreate(state.playerUuid);
            float atk = checkEnemy != null ? getEnemyDamage(checkEnemy, progCheck.level) * 1.5F : 0.0F;
            float def = checkEnemy != null ? readEnemyArmor(state, checkEnemy) : 0.0F;
            String passive = state.entityPassive != null && !state.entityPassive.isEmpty() ? "  \u2605 " + state.entityPassive : "";
            String checkMsg = String.format("%s \u2014 ATK %.0f  DEF %.0f%s", state.entityName, atk, def, passive);
            BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), checkMsg);
            return checkMsg;
         } else if (actIndex >= acts.size()) {
            return "...";
         } else {
            ActDefs.ActEntry act = acts.get(actIndex);
            if (act.logText().equals("__SHINE__")) {
               state.addTpCapped(player.getUuid(), 25.0F);
               BattleNetworking.broadcastSharedTp(state, player.getServer().getPlayerManager());
               String shineMsg = "Your SOUL glows, pushing the darkness away! +25 TP";
               BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), shineMsg);
               return shineMsg;
            } else if (act.sleepCheck() && !state.isEnemyAsleep(enemyId)) {
               return act.sleepLog();
            } else {
               String logText = act.logText();
               DarknerBosses.onAct(state, world.getEntityById(enemyId), act.logName());
               if (act.mercyPct() > 0) {
                  raiseMercy(state, player, enemyId, (float)act.mercyPct() / 100.0F, logText);
               } else {
                  BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), logText);
               }

               if (act.confuseTurns() > 0) {
                  state.setEnemyStatus(enemyId, BattleState.StatusKind.CONFUSE);
                  state.enemyConfusedLeftMap.put(enemyId, act.confuseTurns());
                  if (enemyId == state.primaryEnemyId) {
                     state.enemyConfused = true;
                     state.enemyConfusedLeft = act.confuseTurns();
                  }
               }

               if (act.buffAtk() != 0 || act.buffDef() != 0) {
                  LivingEntity enemy = world.getEntityById(state.primaryEnemyId) instanceof LivingEntity le ? le : null;
                  if (enemy != null) {
                     if (act.buffAtk() != 0) {
                        EntityAttributeInstance atkInst = enemy.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                        if (atkInst != null) {
                           atkInst.setBaseValue(Math.max(0.0, atkInst.getBaseValue() - (double)act.buffAtk()));
                        }
                     }

                     if (act.buffDef() != 0) {
                        if (state.isVirtualEnemy) {
                           state.virtualArmor = Math.max(0.0F, state.virtualArmor - (float)act.buffDef());
                        } else {
                           EntityAttributeInstance armorAttr = enemy.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
                           if (armorAttr != null) {
                              armorAttr.addPersistentModifier(
                                 new EntityAttributeModifier(Identifier.of("turnbattle", "mod-defense-debuff"), (double)(-act.buffDef()), Operation.ADD_VALUE)
                              );
                           }
                        }
                     }
                  }
               }

               if (act.shieldTurns() > 0) {
                  state.setPlayerStatus(player.getUuid(), BattleState.PlayerStatusKind.SHIELD);
                  state.shieldTurnsLeft.put(player.getUuid(), act.shieldTurns());
               }

               int extraSkips = queued ? 0 : act.participants() - 1;

               for (int s = 0; s < extraSkips; s++) {
                  state.advanceTurn();
               }

               return "";
            }
         }
      }
   }

   private static void raiseMercy(BattleState state, ServerPlayerEntity player, int enemyId, float addPct, String logText) {
      state.raiseSpareThreshold(enemyId, addPct);
      float newThresh = state.getSpareThreshold(enemyId);
      String fullLog = logText
         + (state.isMercyEligibleById(enemyId) ? "\n" + state.enemyNames.getOrDefault(enemyId, state.entityName) + " can now be spared!" : "");
      BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), fullLog);
      PlayerManager pm = player.getServer().getPlayerManager();

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2) && activeBattles.containsKey(pid)) {
            ServerPlayerEntity pp = pm.getPlayer(pid);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeFloat(newThresh);
               buf.writeInt(enemyId);
               Net121.sendToClient(pp, TurnBattleMod.SPARE_THRESHOLD_UPDATE, buf);
            }
         }
      }
   }

   private static String doGoods(ServerPlayerEntity player, BattleState state, int slot, int targetIndex) {
      ItemStack stack = player.getInventory().getStack(slot);
      if (stack.isEmpty()) {
         return "Nothing there!";
      } else {
         FoodComponent food = (FoodComponent)stack.get(DataComponentTypes.FOOD);
         if (food == null) {
            return "That's not food!";
         } else {
            if (state.isPvp) {
               if (state.pvpItemsUsed.getOrDefault(player.getUuid(), 0) >= 1) {
                  return "No more items left this battle!";
               }

               state.pvpItemsUsed.merge(player.getUuid(), 1, Integer::sum);
            }

            UUID target = null;
            String targetName = null;
            if (targetIndex >= 10) {
               int pidx = targetIndex - 10;
               List actorTeammates = state.teammatesOf(player.getUuid());
               UUID candidate = pidx >= 0 && pidx < actorTeammates.size() ? actorTeammates.get(pidx) : null;
               if (candidate != null && (!state.isPvp || state.isAllyPair(player.getUuid(), candidate))) {
                  target = candidate;
                  targetName = state.participantNames.getOrDefault(candidate, "Ally");
               }
            }

            boolean selfTarget = target == null || target.equals(player.getUuid());
            float heal = ((float)food.nutrition() * 2.0F + food.saturation() * 2.0F) * 2.0F;
            BattleData data = BattleData.get(player.getServer());
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            if (prog.soulType == SoulType.KINDNESS) {
               heal *= 1.25F;
            }

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            boolean isFavourite = prog.favoriteFood.contains(itemId);
            if (isFavourite && selfTarget) {
               heal *= 2.0F;
            }

            String itemName = stack.getItem().getName().getString();
            stack.decrement(1);
            if (target == null) {
               PlayerManager pm = player.getServer().getPlayerManager();
               float cur = getBattleHpFor(state, player.getUuid(), pm);
               float actualSelfHeal = Math.min(heal, Math.max(0.0F, player.getMaxHealth() - cur));
               applyDownableHeal(state, player.getUuid(), heal, pm);
               BattleNetworking.broadcastHeal(state, pm, player.getUuid(), actualSelfHeal, true, isFavourite);
               return String.format("Used %s \u2014 recovered %.0f HP!", itemName, heal);
            } else {
               PlayerManager pm = player.getServer().getPlayerManager();
               if (!target.equals(BattleState.PARTY_MEMBER_UUID) && !target.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                  float cur = getBattleHpFor(state, target, pm);
                  float max = state.participantMaxHp.getOrDefault(target, player.getMaxHealth());
                  float actual = Math.min(heal, Math.max(0.0F, max - cur));
                  applyDownableHeal(state, target, heal, pm);
                  BattleNetworking.broadcastHeal(state, pm, target, actual);
               } else {
                  float cur = getBattleHpFor(state, target, pm);
                  float max = state.participantMaxHp.getOrDefault(target, 1.0F);
                  float actual = Math.min(heal, Math.max(0.0F, max - cur));
                  applyDownableHeal(state, target, heal, pm);
                  BattleNetworking.broadcastHeal(state, pm, target, actual);
               }

               return String.format("Used %s on %s \u2014 recovered %.0f HP!", itemName, targetName, heal);
            }
         }
      }
   }

   private static Set<String> resolveActorUnlockedSkills(ServerPlayerEntity player, BattleState state, UUID actor, BattleData.PlayerProgress actorProg) {
      boolean isSlot2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
      boolean isAnyPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || isSlot2;
      if (!isAnyPartyMember) {
         return actorProg.unlockedSkills;
      } else {
         RecruitData rd = RecruitData.get(player.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
         int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            if (ci < pr.customRecruits.size()) {
               return pr.customRecruits.get(ci).state.unlockedSkills;
            }
         } else {
            if (slotCode >= 0 && slotCode < Recruit.values().length) {
               return pr.recruits[slotCode].unlockedSkills;
            }

            if (!isSlot2) {
               Recruit activeRecruit = getActiveRecruitEnum(player.getServer(), state.playerUuid);
               if (activeRecruit != null) {
                  return rd.getOrCreate(state.playerUuid).get(activeRecruit).unlockedSkills;
               }
            }
         }

         return actorProg.unlockedSkills;
      }
   }

   private static int resolveActorStat(
      ServerPlayerEntity player, BattleState state, UUID actor, BattleData.PlayerProgress prog, BattleData.PlayerProgress actorProg, int statIndex
   ) {
      boolean isSlot2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
      boolean isAnyPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || isSlot2;
      if (!isAnyPartyMember) {
         return actorProg.playerStats.valueFor(statIndex);
      } else {
         RecruitData rd = RecruitData.get(player.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
         int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            if (ci < pr.customRecruits.size()) {
               return getRecruitEffectiveStats(pr.customRecruits.get(ci).state).valueFor(statIndex);
            }
         } else {
            if (slotCode >= 0 && slotCode < Recruit.values().length) {
               return getRecruitEffectiveStats(pr.recruits[slotCode]).valueFor(statIndex);
            }

            if (!isSlot2) {
               Recruit activeRecruit = getActiveRecruitEnum(player.getServer(), state.playerUuid);
               if (activeRecruit != null) {
                  return getRecruitEffectiveStats(rd.getOrCreate(state.playerUuid).get(activeRecruit)).valueFor(statIndex);
               }
            }
         }

         return prog.playerStats.valueFor(statIndex);
      }
   }

   private static int resolveActorIq(
      ServerPlayerEntity player, BattleState state, UUID actor, BattleData.PlayerProgress prog, BattleData.PlayerProgress actorProg
   ) {
      return resolveActorStat(player, state, actor, prog, actorProg, 3);
   }

   private static String doDefend(ServerPlayerEntity player, UUID actor, BattleState state, int actorIq) {
      state.defending.put(actor, true);
      float defendTp = state.darknessConstricting ? 2.0F : 20.0F + (float)actorIq;
      state.addTpCapped(actor, defendTp);
      String shieldMsg = "";
      BattleData.PlayerProgress defendProg = BattleData.get(player.getServer()).getOrCreate(actor);
      if (soulOf(player.getServer(), state, actor) == SoulType.PATIENCE) {
         PlayerManager pmPat = player.getServer().getPlayerManager();
         float maxPat = getMaxHpFor(state, actor, pmPat);
         float curPat = getBattleHpFor(state, actor, pmPat);
         if (curPat > 0.0F && curPat < maxPat) {
            float healed = Math.min(maxPat, curPat + maxPat * 0.05F);
            setBattleHpFor(state, actor, healed, pmPat);
            state.participantHp.put(actor, healed);
            shieldMsg = shieldMsg + String.format(" Patience: +%.0f HP.", healed - curPat);
         }
      }

      if (state.isPvp && defendProg.playerRace != CharacterRace.HUMAN && !state.pvpShieldBroken.getOrDefault(actor, true)) {
         float maxShield = state.pvpShieldMax.getOrDefault(actor, 0.0F);
         float cur = state.pvpShield.getOrDefault(actor, 0.0F);
         float newShield = Math.min(maxShield, cur + maxShield * 0.2F);
         state.pvpShield.put(actor, newShield);
         shieldMsg = " Soul Shield regenerated!";
      }

      return String.format("Defending! Damage halved next turn. +%.0f TP.", defendTp) + shieldMsg;
   }

   private static boolean postActionCleanup(ServerPlayerEntity player, BattleState state, String msg, ServerWorld world) {
      PlayerManager pm = player.getServer().getPlayerManager();
      if (state.pendingPacifySpareId >= 0) {
         int pacifyId = state.pendingPacifySpareId;
         state.pendingPacifySpareId = -1;
         int pacifyIdx = pacifyId == state.primaryEnemyId ? 0 : state.extraEnemyIds.indexOf(pacifyId) + 1;
         boolean enemyRemoved = spareOneEnemy(state, world, pacifyId, pacifyIdx, player.getServer());
         boolean allGone6 = state.isVirtualEnemy
            ? state.virtualHp <= 0.0F
            : enemyRemoved && state.extraEnemyIds.isEmpty() && findEnemyByIndex(state, world, 0) == null;
         if (allGone6) {
            BattleNetworking.broadcastStateUpdate(state, pm, msg);
            endBattle(state, player.getServer(), "mercy");
            return true;
         }
      }

      if (state.enemySpareable) {
         state.enemySpareable = false;
      }

      BestiaryData prunedBestiary = BestiaryData.get(player.getServer());
      state.extraEnemyIds.removeIf(eid -> {
         Entity e;
         boolean var10000;
         label31: {
            e = world.getEntityById(eid);
            if (e instanceof LivingEntity le && le.isAlive()) {
               var10000 = true;
               break label31;
            }

            var10000 = false;
         }

         boolean dead = !var10000;
         if (dead) {
            state.enemyHpMap.put(eid, 0.0F);
            frozenEntityIds.remove(eid);
            if (e instanceof MobEntity me) {
               wakeMob(me);
            }

            battleByMob.remove(eid);
            if (e instanceof LivingEntity dyingLe && !state.isVirtualEnemy) {
               prunedBestiary.recordDefeat(state.playerUuid, dyingLe.getType());
               dropBattleLoot(dyingLe, world, true, player);
            }

            sendFate(state, world, eid, e, false);
            discardEnemy(e);
         }

         return dead;
      });
      LivingEntity primary = !state.isPvp && !state.isVirtualEnemy ? findEnemyByIndex(state, world, 0) : null;
      if (!state.isPvp && primary == null && !state.extraEnemyIds.isEmpty()) {
         promoteNextEnemy(state, world);
         primary = findEnemyByIndex(state, world, 0);
      }

      if (!state.isPvp) {
         boolean allDead = state.isVirtualEnemy ? state.virtualHp <= 0.0F : primary == null;
         if (allDead) {
            if (!state.isVirtualEnemy) {
               killFrozenExtras(state, world, player);
            }

            BattleNetworking.broadcastStateUpdate(state, pm, msg);
            endBattle(state, player.getServer(), "win");
            return true;
         }
      }

      if (state.isPvp) {
         ServerPlayerEntity ep = pm.getPlayer(primaryEnemyOf(state, player.getUuid(), pm));
         if (ep != null) {
            state.entityCurrentHp = ep.getHealth();
            state.enemyHpMap.put(state.entityNetworkId, ep.getHealth());
         }
      } else if (state.isVirtualEnemy) {
         state.entityCurrentHp = state.virtualHp;
         state.enemyHpMap.put(Integer.MIN_VALUE, state.virtualHp);
      } else if (primary != null) {
         state.entityCurrentHp = primary.getHealth();
         state.enemyHpMap.put(state.primaryEnemyId, primary.getHealth());
      }

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            ServerPlayerEntity pp = player.getServer().getPlayerManager().getPlayer(pid);
            if (pp != null) {
               state.participantHp.put(pid, TARGET_HEALTH.getOrDefault(pid, pp.getHealth()));
            }
         }
      }

      return false;
   }

   private static void afterParticipantAction(ServerPlayerEntity player, BattleState state, String msg, boolean isDefend, ServerWorld world) {
      PlayerManager pm = player.getServer().getPlayerManager();
      if (!postActionCleanup(player, state, msg, world)) {
         UUID actor = state.currentParticipant();
         state.newlyDownedThisTurn.clear();
         state.batonPassPending = false;
         if (state.pendingBonusTurnTarget != null) {
            UUID bonusTarget = state.pendingBonusTurnTarget;
            state.pendingBonusTurnTarget = null;
            giveAllyBonusTurn(player, actor, bonusTarget, state, msg);
         } else if (state.pendingSelfBonusTurn) {
            state.pendingSelfBonusTurn = false;
            BattleNetworking.broadcastStateUpdate(state, pm, msg);
         } else if (state.bonusSelection) {
            state.bonusSelection = false;
            state.currentParticipantIndex = 0;
            if (!state.actionQueue.isEmpty()) {
               state.resolving = true;
               BattleNetworking.broadcastStateUpdate(state, pm, msg);
               startResolution(state, player.getServer());
            } else {
               BattleNetworking.broadcastStateUpdate(state, pm, msg);
               beginEnemyPhase(player, state, pm, world);
            }
         } else {
            state.advanceTurn();
            if (state.batonPassReturnIndex >= 0) {
               state.currentParticipantIndex = state.batonPassReturnIndex;
               state.batonPassReturnIndex = -1;
            }

            UUID next;
            for (next = state.currentParticipant(); !next.equals(state.participantOrder.get(0)) && isDowned(state, next, pm); next = state.currentParticipant()) {
               if (isPartyWiped(state, pm)) {
                  endBattleFromPartyWipe(state, player.getServer(), pm);
                  return;
               }

               String downedName = state.participantNames.getOrDefault(next, isRecruitSlot(next) ? "Ally" : "Player");
               boolean revived = tickDownedRegenAndCheckRevive(state, next, pm);
               BattleNetworking.broadcastStateUpdate(state, pm, revived ? downedName + " gets back up!" : downedName + " is still down and can't act...");
               state.advanceTurn();
            }

            if (activeBattles.containsKey(player.getUuid())) {
               if (next.equals(state.participantOrder.get(0))) {
                  if (queuedTurns(state) && !state.actionQueue.isEmpty()) {
                     state.resolving = true;
                     BattleNetworking.broadcastStateUpdate(state, pm, msg);
                     startResolution(state, player.getServer());
                     return;
                  }

                  BattleNetworking.broadcastStateUpdate(state, pm, msg);
                  beginEnemyPhase(player, state, pm, world);
               } else {
                  BattleNetworking.broadcastStateUpdate(state, pm, msg);
                  if (!next.equals(BattleState.PARTY_MEMBER_UUID) && !next.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                     ServerPlayerEntity nextPlayer = player.getServer().getPlayerManager().getPlayer(next);
                     if (nextPlayer != null) {
                        BattleNetworking.sendTurnNotice(nextPlayer, state);
                     }
                  } else {
                     ServerPlayerEntity owner = player.getServer().getPlayerManager().getPlayer(state.playerUuid);
                     if (owner != null) {
                        BattleNetworking.sendPartyMemberTurn(owner, state, next);
                     }
                  }
               }
            }
         }
      }
   }

   private static void runPvpDodgePhase(ServerPlayerEntity triggeringPlayer, BattleState state, PlayerManager pm) {
      state.phase = BattleState.Phase.AWAITING_ENEMY_RESULT;
      long bulletSeed = RNG.nextLong();
      int pvpPattern = BulletPatterns.getPatternForElement(Element.NONE, RNG);

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            ServerPlayerEntity pp = triggeringPlayer.getServer().getPlayerManager().getPlayer(pid);
            if (pp != null) {
               UUID opponentUuid = primaryEnemyOf(state, pid, pm);
               if (opponentUuid != null) {
                  boolean attacked = false;

                  for (UUID other : state.participantOrder) {
                     if (state.isEnemyPair(pid, other) && state.pvpAttackedThisRound.contains(other)) {
                        attacked = true;
                        break;
                     }
                  }

                  if (!attacked) {
                     PacketByteBuf none = PacketByteBufs.create();
                     none.writeFloat(0.0F);
                     none.writeInt(-1);
                     none.writeFloat(1.0F);
                     none.writeInt(Element.NONE.ordinal());
                     none.writeLong(bulletSeed);
                     Net121.sendToClient(pp, TurnBattleMod.ENEMY_TURN, none);
                  } else {
                     ServerPlayerEntity opponent = triggeringPlayer.getServer().getPlayerManager().getPlayer(opponentUuid);
                     BattleData.PlayerProgress opProg = BattleData.get(triggeringPlayer.getServer()).getOrCreate(opponentUuid);
                     double weaponDmg = 4.0;
                     if (opProg.equippedWeapon != null && !opProg.equippedWeapon.isEmpty()) {
                        double itemDmg = 1.0;
                        AttributeModifiersComponent attrComp = (AttributeModifiersComponent)opProg.equippedWeapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                        if (attrComp != null) {
                           for (net.minecraft.component.type.AttributeModifiersComponent.Entry entry : attrComp.modifiers()) {
                              if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && entry.modifier().operation() == Operation.ADD_VALUE) {
                                 itemDmg += entry.modifier().value();
                              }
                           }
                        }

                        weaponDmg = Math.max(4.0, itemDmg);
                        WeaponStats ws = WeaponStats.fromStack(opProg.equippedWeapon);
                        if (ws != null) {
                           weaponDmg += (double)ws.bashBonus();
                        }
                     }

                     double scaled = weaponDmg * (1.0 + (double)opProg.playerStats.offenseBonus());
                     float perTarget = (float)scaled;
                     float effective = perTarget * (1.0F - state.getShieldReduction(pid)) * (state.isDefending(pid) ? 0.5F : 1.0F);
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeFloat(effective);
                     buf.writeInt(pvpPattern);
                     buf.writeFloat(1.0F);
                     buf.writeInt(Element.NONE.ordinal());
                     buf.writeLong(bulletSeed);
                     Net121.sendToClient(pp, TurnBattleMod.ENEMY_TURN, buf);
                  }
               }
            }
         }
      }

      state.pvpAttackedThisRound.clear();
   }

   private static void runEnemyTurn(ServerPlayerEntity triggeringPlayer, LivingEntity enemy, BattleState state, PlayerManager pm) {
      runEnemyTurn(triggeringPlayer, enemy, state, pm, false);
   }

   private static LivingEntity pruneBurnedEnemies(BattleState state, ServerWorld world, ServerPlayerEntity player) {
      BestiaryData bestiary = BestiaryData.get(world.getServer());
      state.extraEnemyIds.removeIf(eid -> {
         Entity e;
         boolean var10000;
         label29: {
            e = world.getEntityById(eid);
            if (e instanceof LivingEntity le && le.isAlive()) {
               var10000 = true;
               break label29;
            }

            var10000 = false;
         }

         boolean dead = !var10000;
         if (dead) {
            state.enemyHpMap.put(eid, 0.0F);
            state.enemyBurnTurnsMap.remove(eid);
            frozenEntityIds.remove(eid);
            if (e instanceof MobEntity me) {
               wakeMob(me);
            }

            battleByMob.remove(eid);
            if (e instanceof LivingEntity dyingLe) {
               bestiary.recordDefeat(state.playerUuid, dyingLe.getType());
               dropBattleLoot(dyingLe, world, true, player);
            }

            sendFate(state, world, eid, e, false);
            discardEnemy(e);
         }

         return dead;
      });
      LivingEntity primary = findEnemyByIndex(state, world, 0);
      if (primary == null && !state.extraEnemyIds.isEmpty()) {
         int deadId = state.primaryEnemyId;
         Entity deadE = world.getEntityById(deadId);
         state.enemyBurnTurnsMap.remove(deadId);
         if (deadE instanceof LivingEntity dl) {
            bestiary.recordDefeat(state.playerUuid, dl.getType());
            dropBattleLoot(dl, world, true, player);
         }

         sendFate(state, world, deadId, deadE, false);
         battleByMob.remove(deadId);
         frozenEntityIds.remove(deadId);
         discardEnemy(deadE);
         promoteNextEnemy(state, world);
         primary = findEnemyByIndex(state, world, 0);
      }

      return primary;
   }

   private static void runEnemyTurn(ServerPlayerEntity triggeringPlayer, LivingEntity enemy, BattleState state, PlayerManager pm, boolean burnsDone) {
      if (!burnsDone && !state.enemyBurnTurnsMap.isEmpty() && triggeringPlayer.getWorld() instanceof ServerWorld burnWorld) {
         StringBuilder burnMsg = new StringBuilder();
         boolean anyDied = false;

         for (int burnId : new ArrayList<>(state.enemyBurnTurnsMap.keySet())) {
            int level = state.enemyBurnTurnsMap.getOrDefault(burnId, 0);
            if (level > 0) {
               Entity burnEnt = burnWorld.getEntityById(burnId);
               if (burnEnt instanceof LivingEntity) {
                  LivingEntity burnLe = (LivingEntity)burnEnt;
                  if (burnLe.isAlive()) {
                     float maxHp = state.enemyMaxHpMap.getOrDefault(burnId, burnLe.getMaxHealth());
                     float burnDmg = maxHp * (float)level * 0.03F;
                     float burnNewHp = applyMagicDamageToEnemy(state, burnLe, triggeringPlayer, burnDmg);
                     state.enemyHpMap.put(burnId, burnNewHp);
                     if (burnId == state.primaryEnemyId) {
                        state.entityCurrentHp = burnNewHp;
                        state.enemyBurnTurns = level;
                     }

                     String burnName = state.enemyNames.getOrDefault(burnId, "Enemy");
                     burnMsg.append(String.format("%s burned for %.0f (Burn %d)! ", burnName, burnDmg, level));
                     if (burnNewHp <= 0.0F) {
                        anyDied = true;
                     }
                  }
               }
            }
         }

         if (burnMsg.length() > 0) {
            BattleNetworking.broadcastStateUpdate(state, pm, burnMsg.toString().trim());
         }

         if (anyDied && !state.isVirtualEnemy) {
            LivingEntity front = pruneBurnedEnemies(state, burnWorld, triggeringPlayer);
            if (front == null) {
               killFrozenExtras(state, burnWorld, triggeringPlayer);
               endBattle(state, triggeringPlayer.getServer(), "win");
               return;
            }

            BattleNetworking.broadcastStateUpdate(state, pm, "");
            if (front != enemy) {
               runEnemyTurn(triggeringPlayer, front, state, pm, true);
               return;
            }
         }
      }

      if (state.extraEnemyIds.isEmpty()) {
         if (state.isEnemyAsleep(state.primaryEnemyId)) {
            if (staysAsleep(state.enemySleepTurnsMap, state.primaryEnemyId)) {
               state.tickStatusEffects();
               BattleNetworking.broadcastStateUpdate(state, pm, state.entityName + " is asleep... Zzz");
               return;
            }

            state.enemySleepTurnsMap.remove(state.primaryEnemyId);
            state.sleepImmuneSet.add(state.primaryEnemyId);
            state.enemySleepTurns = 0;
            state.sleepImmune = true;
            BattleNetworking.broadcastStateUpdate(state, pm, state.entityName + " woke up!");
         }

         if (state.enemyFreezeTurns > 0) {
            state.tickStatusEffects();
            BattleNetworking.broadcastStateUpdate(state, pm, state.entityName + " can't move!");
            return;
         }

         if (state.enemyDownTurns.getOrDefault(state.primaryEnemyId, 0) > 0) {
            state.tickStatusEffects();
            BattleNetworking.broadcastStateUpdate(state, pm, state.entityName + " is DOWN and can't move!");
            return;
         }

         if (state.battleStyle == BattleStyle.DIRECT_DAMAGE && state.isEnemyConfused(state.primaryEnemyId) && RNG.nextFloat() < 0.5F) {
            state.tickStatusEffects();
            BattleNetworking.broadcastStateUpdate(state, pm, state.entityName + " is confused and stumbles!");
            return;
         }
      }

      if (enemy instanceof MobEntity mob && mob.getCommandTags().contains("story_sheepy")) {
         state.tickStatusEffects();
         BattleNetworking.broadcastStateUpdate(state, pm, "The imaginary enemy prepares an imaginary attack.");
         return;
      }

      if (state.isVirtualEnemy && "turnbattle:story_kris".equals(state.entityTypeKey)) {
         state.phase = BattleState.Phase.AWAITING_ENEMY_RESULT;
         state.tickStatusEffects();
         BattleNetworking.broadcastStateUpdate(state, pm, "");
         BattleData dataKris = BattleData.get(triggeringPlayer.getServer());
         long krisBulletSeed = RNG.nextLong();
         int krisPattern = BulletPatterns.getKrisPattern(RNG);

         for (UUID pid : state.participantOrder) {
            if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               ServerPlayerEntity pp = triggeringPlayer.getServer().getPlayerManager().getPlayer(pid);
               if (pp != null) {
                  BattleData.PlayerProgress pprog = dataKris.getOrCreate(pid);
                  float base = 12.3F * (0.3F + (float)pprog.level * 0.04F) / (float)state.participantOrder.size();
                  float eff = base * (1.0F - state.getShieldReduction(pid)) * (state.isDefending(pid) ? 0.5F : 1.0F) * MobLevelManager.getArmorFactor(pp);
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeFloat(eff);
                  buf.writeInt(krisPattern);
                  buf.writeFloat(1.0F);
                  buf.writeInt(Element.NONE.ordinal());
                  buf.writeLong(krisBulletSeed);
                  Net121.sendToClient(pp, TurnBattleMod.ENEMY_TURN, buf);
               }
            }
         }
      } else if (state.isVirtualEnemy && "turnbattle:story_antonio".equals(state.entityTypeKey)) {
         state.phase = BattleState.Phase.AWAITING_ENEMY_RESULT;
         state.tickStatusEffects();
         BattleNetworking.broadcastStateUpdate(state, pm, "");
         BattleData dataAntonio = BattleData.get(triggeringPlayer.getServer());
         long antonioBulletSeed = RNG.nextLong();
         int antonioPattern = BulletPatterns.getAntonioPattern(RNG);

         for (UUID pidx : state.participantOrder) {
            if (!pidx.equals(BattleState.PARTY_MEMBER_UUID) && !pidx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               ServerPlayerEntity pp = triggeringPlayer.getServer().getPlayerManager().getPlayer(pidx);
               if (pp != null) {
                  BattleData.PlayerProgress pprog = dataAntonio.getOrCreate(pidx);
                  float base2 = 6.0F * (0.3F + (float)pprog.level * 0.04F) / (float)state.participantOrder.size();
                  float eff = base2 * (1.0F - state.getShieldReduction(pidx)) * (state.isDefending(pidx) ? 0.5F : 1.0F) * MobLevelManager.getArmorFactor(pp);
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeFloat(eff);
                  buf.writeInt(antonioPattern);
                  buf.writeFloat(1.0F);
                  buf.writeInt(Element.NONE.ordinal());
                  buf.writeLong(antonioBulletSeed);
                  Net121.sendToClient(pp, TurnBattleMod.ENEMY_TURN, buf);
               }
            }
         }
      } else {
         BattleData data = BattleData.get(triggeringPlayer.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(state.playerUuid);
         if (state.battleStyle == BattleStyle.DIRECT_DAMAGE) {
            runDirectDamageTurn(triggeringPlayer, enemy, state, pm, data);
         } else {
            int depthsFloor = DepthsDescentManager.getFloorEnemyLevel(triggeringPlayer.getServer(), state.playerUuid);
            if (depthsFloor > 0) {
               DepthsDescentData.Instance depthsInst = DepthsDescentData.get(triggeringPlayer.getServer())
                  .getInstance(DepthsDescentManager.getInstanceKey(state.playerUuid));
               boolean isBoss = depthsInst != null && depthsInst.isBossFloor();
               float floorMult;
               if (isBoss) {
                  int diff = DepthsDescentManager.difficultyForFloor(depthsFloor);
                  floorMult = 5.0F + (float)diff;
               } else {
                  floorMult = 4.0F;
               }

               float base = (float)depthsFloor * floorMult;
               float perTarget = base;
               long depthsBulletSeed = RNG.nextLong();
               int depthsPattern = BulletPatterns.patternFor(state.entityTypeKey, state.battleElement, RNG);

               for (UUID pidxx : state.participantOrder) {
                  ServerPlayerEntity pp = triggeringPlayer.getServer().getPlayerManager().getPlayer(pidxx);
                  if (pp != null) {
                     float eff = perTarget
                        * (1.0F - state.getShieldReduction(pidxx))
                        * (state.isDefending(pidxx) ? 0.5F : 1.0F)
                        * MobLevelManager.getArmorFactor(pp);
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeFloat(eff);
                     buf.writeInt(depthsPattern);
                     buf.writeFloat(state.entityBulletSpeedMult);
                     buf.writeInt(state.battleElement.ordinal());
                     buf.writeLong(depthsBulletSeed);
                     buf.writeFloat(state.isEnemyConfused(state.primaryEnemyId) ? 0.5F : 1.0F);
                     Net121.sendToClient(pp, TurnBattleMod.ENEMY_TURN, buf);
                  }
               }

               state.tickStatusEffects();
            } else {
               Element currentTurnElement = state.battleElement;
               LivingEntity actingEnemy = enemy;
               boolean actingConfused = state.isEnemyConfused(state.primaryEnemyId);
               String actingTypeKey = state.entityTypeKey;
               List allEids = new ArrayList<>();
               allEids.add(state.primaryEnemyId);
               allEids.addAll(state.extraEnemyIds);
               List aliveEids = new ArrayList<>();

               for (int eid3 : allEids) {
                  float hp3 = state.enemyHpMap.getOrDefault(eid3, 1.0F);
                  if (hp3 > 0.0F) {
                     aliveEids.add(eid3);
                  }
               }

               if (!aliveEids.isEmpty()) {
                  int rolledEid = aliveEids.get(RNG.nextInt(aliveEids.size()));
                  boolean isFrozen = state.enemyFreezeTurnsMap.getOrDefault(rolledEid, 0) > 0;
                  boolean isAsleep = state.isEnemyAsleep(rolledEid);
                  boolean isConfusedRoll = state.isEnemyConfused(rolledEid);
                  String rolledName = state.enemyNames.getOrDefault(rolledEid, state.entityName);
                  if (isAsleep) {
                     if (staysAsleep(state.enemySleepTurnsMap, rolledEid)) {
                        state.tickStatusEffects();
                        BattleNetworking.broadcastStateUpdate(state, pm, rolledName + " is asleep... Zzz");
                        return;
                     }

                     state.enemySleepTurnsMap.remove(rolledEid);
                     state.sleepImmuneSet.add(rolledEid);
                     if (rolledEid == state.primaryEnemyId) {
                        state.enemySleepTurns = 0;
                        state.sleepImmune = true;
                     }

                     BattleNetworking.broadcastStateUpdate(state, pm, rolledName + " woke up!");
                  } else if (isFrozen) {
                     state.tickStatusEffects();
                     BattleNetworking.broadcastStateUpdate(state, pm, rolledName + " can't move!");
                     return;
                  }

                  actingConfused = isConfusedRoll;
                  String typeKey3 = state.enemyTypeKeys.getOrDefault(rolledEid, state.entityTypeKey);
                  actingTypeKey = typeKey3;
                  Element rolled3 = Registries.ENTITY_TYPE.getOrEmpty(Identifier.tryParse(typeKey3)).map(ElementRegistry::getElement).orElse(Element.NONE);
                  currentTurnElement = rolled3 == Element.NONE ? state.battleElement : rolled3;
                  if (triggeringPlayer.getWorld() instanceof ServerWorld actWorld && actWorld.getEntityById(rolledEid) instanceof LivingEntity rolledLe) {
                     actingEnemy = rolledLe;
                  }
               }

               float base;
               if (state.isPvp) {
                  BattleData bdPvp = BattleData.get(triggeringPlayer.getServer());
                  float totalOffense = 0.0F;
                  int pvpCount = 0;

                  for (UUID pidxxx : state.participantOrder) {
                     if (!pidxxx.equals(BattleState.PARTY_MEMBER_UUID) && !pidxxx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                        totalOffense += (float)bdPvp.getOrCreate(pidxxx).playerStats.offense;
                        pvpCount++;
                     }
                  }

                  float avgOffense = pvpCount > 0 ? totalOffense / (float)pvpCount : 0.0F;
                  base = (2.5F + avgOffense * 0.7F) * (0.3F + (float)prog.level * 0.04F);
               } else {
                  base = getEnemyDamage(actingEnemy, prog.level);
               }

               float rogueScale = getRogueliteScale(triggeringPlayer);
               float var58 = base * rogueScale;
               float perTarget = var58;
               float actingBulletSpeed = actingEnemy != null ? MobLevelManager.getMobBulletSpeed(actingEnemy) : state.entityBulletSpeedMult;
               state.phase = BattleState.Phase.AWAITING_ENEMY_RESULT;
               pendingEnemyResultsStartTick.put(state.entityNetworkId, triggeringPlayer.getServer().getOverworld().getTime());
               state.batonPassUsed.clear();
               Map wasDefending = new HashMap<>();
               state.bulletTurnDefendSnap.clear();
               state.bulletTurnShieldSnap.clear();

               for (UUID pidxxxx : state.participantOrder) {
                  boolean def = state.isDefending(pidxxxx);
                  wasDefending.put(pidxxxx, def);
                  state.bulletTurnDefendSnap.put(pidxxxx, def);
                  state.bulletTurnShieldSnap.put(pidxxxx, state.getShieldReduction(pidxxxx));
               }

               state.tickEnemyStatusEffects();

               for (UUID pidxxxx : state.participantOrder) {
                  if (!pidxxxx.equals(BattleState.PARTY_MEMBER_UUID) && !pidxxxx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                     ServerPlayerEntity livepp = triggeringPlayer.getServer().getPlayerManager().getPlayer(pidxxxx);
                     if (livepp != null) {
                        state.participantHp.put(pidxxxx, TARGET_HEALTH.getOrDefault(pidxxxx, livepp.getHealth()));
                     }
                  }
               }

               state.currentBulletBaseDmg = var58;
               long mainBulletSeed = RNG.nextLong();
               boolean darkner = !state.isPvp && !state.magicant && actingEnemy instanceof DarknerEntity;
               int mainPattern = state.isPvp
                  ? BulletPatterns.getPatternForElement(currentTurnElement, RNG)
                  : (darkner ? ((DarknerEntity)actingEnemy).pickPattern(RNG) : BulletPatterns.patternFor(actingTypeKey, currentTurnElement, RNG));
               String bulletItem = darkner ? ((DarknerEntity)actingEnemy).itemId() : "";
               String bossLine = triggeringPlayer.getWorld() instanceof ServerWorld bw ? DarknerBosses.battleLine(state, bw) : "";
               if (!bossLine.isEmpty()) {
                  pendingEnemyResultsStartTick.put(
                     state.entityNetworkId,
                     triggeringPlayer.getServer().getOverworld().getTime() + (long)Math.ceil((double)((float)bossLine.length() / 1.5F)) + 180L + 40L
                  );
               }

               BattleNetworking.broadcastStateUpdate(state, pm, state.magicant ? MagicantManager.enemyTurnLine(state, mainPattern, triggeringPlayer) : "");

               for (UUID pidxxxxx : state.participantOrder) {
                  if (!pidxxxxx.equals(BattleState.PARTY_MEMBER_UUID) && !pidxxxxx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                     ServerPlayerEntity pp = triggeringPlayer.getServer().getPlayerManager().getPlayer(pidxxxxx);
                     if (pp != null) {
                        float effective = perTarget
                           * (1.0F - state.getShieldReduction(pidxxxxx))
                           * (wasDefending.getOrDefault(pidxxxxx, false) ? 0.5F : 1.0F)
                           * (1.0F - state.getEnemyAttackReduce(actingEnemy.getId()))
                           * MobLevelManager.getArmorFactor(pp);
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeFloat(effective);
                        buf.writeInt(mainPattern);
                        buf.writeFloat(actingBulletSpeed);
                        buf.writeInt(currentTurnElement.ordinal());
                        buf.writeLong(mainBulletSeed);
                        buf.writeFloat(actingConfused ? 0.5F : 1.0F);
                        buf.writeString(bulletItem);
                        buf.writeString(bossLine);
                        Net121.sendToClient(pp, TurnBattleMod.ENEMY_TURN, buf);
                     }
                  }
               }
            }
         }
      }
   }

   private static void runDirectDamageTurn(ServerPlayerEntity triggeringPlayer, LivingEntity enemy, BattleState state, PlayerManager pm, BattleData data) {
      for (int eid : new ArrayList<>(state.enemyAtkBoostTurns.keySet())) {
         int t = state.enemyAtkBoostTurns.getOrDefault(eid, 0) - 1;
         if (t <= 0) {
            state.enemyAtkBoostTurns.remove(eid);
            state.enemyAtkBoostMult.remove(eid);
         } else {
            state.enemyAtkBoostTurns.put(eid, t);
         }
      }

      List allIds = new ArrayList<>();
      allIds.add(state.primaryEnemyId);
      allIds.addAll(state.extraEnemyIds);
      List aliveIds = new ArrayList<>();

      for (int eidx : allIds) {
         if (state.enemyHpMap.getOrDefault(eidx, 1.0F) > 0.0F) {
            aliveIds.add(eidx);
         }
      }

      if (!aliveIds.isEmpty()) {
         int actingId = aliveIds.get(RNG.nextInt(aliveIds.size()));
         String actName = state.enemyNames.getOrDefault(actingId, state.entityName);
         String typeKey = state.enemyTypeKeys.getOrDefault(actingId, state.entityTypeKey);
         float atkBoost = state.enemyAtkBoostMult.getOrDefault(actingId, 1.0F);
         MobAttack attack = MobAttackRegistry.pickAttack(typeKey, RNG);
         if (attack.damageMult <= 0.0F) {
            String specialMsg = applyDirectSpecial(attack, actingId, actName, state, triggeringPlayer, aliveIds, pm, data);
            state.phase = BattleState.Phase.PLAYER_TURN;
            state.tickStatusEffects();
            BattleNetworking.broadcastStateUpdate(state, pm, specialMsg);
         } else {
            BattleData.PlayerProgress initiatorProg = data.getOrCreate(state.playerUuid);
            float base = getEnemyDamage(enemy, initiatorProg.level) * getRogueliteScale(triggeringPlayer);
            base *= attack.damageMult * atkBoost;
            List realPlayers = new ArrayList<>();
            List recruitSlots = new ArrayList<>();

            for (UUID pid : state.participantOrder) {
               if (!isDowned(state, pid, pm)) {
                  if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                     realPlayers.add(pid);
                  } else {
                     recruitSlots.add(pid);
                  }
               }
            }

            List allTargetable = new ArrayList<>(realPlayers);
            allTargetable.addAll(recruitSlots);
            if (!allTargetable.isEmpty()) {
               List targets = new ArrayList<>();
               switch (attack.targeting) {
                  case SINGLE:
                     targets.add(allTargetable.get(RNG.nextInt(allTargetable.size())));
                     break;
                  case MULTI:
                     targets.add(allTargetable.get(RNG.nextInt(allTargetable.size())));
                     targets.add(allTargetable.get(RNG.nextInt(allTargetable.size())));
                     break;
                  case AOE:
                     targets.addAll(allTargetable);
               }

               String specialSuffix = "";
               if (attack.special != MobAttack.Special.NONE) {
                  specialSuffix = " " + applyDirectSpecialSide(attack.special, actingId, actName, state, triggeringPlayer, aliveIds, pm, data);
               }

               List targetNames = new ArrayList<>();

               for (UUID t : new LinkedHashSet<>(targets)) {
                  targetNames.add(state.participantNames.getOrDefault(t, "Player"));
               }

               String targetStr = String.join(" and ", targetNames);
               Map damagePerPlayer = new LinkedHashMap<>();

               for (UUID t : targets) {
                  damagePerPlayer.merge(t, base, Float::sum);
               }

               state.targetParticipantHp.remove(BattleState.PARTY_MEMBER_UUID);
               state.targetParticipantHp.remove(BattleState.PARTY_MEMBER_UUID_2);
               Map wasDefending = new HashMap<>();
               Map shieldSnap = new HashMap<>();

               for (UUID pidx : allTargetable) {
                  wasDefending.put(pidx, state.isDefending(pidx));
                  shieldSnap.put(pidx, state.getShieldReduction(pidx));
               }

               for (UUID slot : recruitSlots) {
                  float rdmg = damagePerPlayer.getOrDefault(slot, 0.0F);
                  if (!(rdmg <= 0.0F)) {
                     float curHp = state.participantHp.getOrDefault(slot, 0.0F);
                     if (!(curHp <= 0.0F)) {
                        CharStats rdStats = recruitSlotStats(triggeringPlayer.getServer(), state, slot);
                        rdmg = rdStats != null ? rdStats.afterDefense(rdmg) : rdmg * (1.0F - CharStats.defenseFraction(5.0F));
                        rdmg = Math.max(0.0F, rdmg * (1.0F - shieldSnap.getOrDefault(slot, 0.0F)));
                        if (wasDefending.getOrDefault(slot, false)) {
                           rdmg *= 0.5F;
                        }

                        rdmg = heartLocketCut(triggeringPlayer, state, slot, rdmg);
                        float maxHpSlot = state.participantMaxHp.getOrDefault(slot, 20.0F);
                        float newHp = curHp - rdmg;
                        if (newHp <= 0.0F) {
                           newHp = (float)(-Math.round(maxHpSlot / 2.0F));
                        }

                        if (TurnBattleConfig.get().rollingHealth && !state.isPvp) {
                           float existingTarget = state.targetParticipantHp.getOrDefault(slot, curHp);
                           if (!(existingTarget <= 0.0F)) {
                              float rolledNew = existingTarget - rdmg;
                              if (rolledNew <= 0.0F) {
                                 rolledNew = (float)(-Math.round(maxHpSlot / 2.0F));
                              }

                              state.targetParticipantHp.put(slot, rolledNew);
                           }
                        } else {
                           state.participantHp.put(slot, newHp);
                        }
                     }
                  }
               }

               if (isPartyWiped(state, pm)) {
                  endBattleFromPartyWipe(state, triggeringPlayer.getServer(), pm);
               } else {
                  state.tickStatusEffects();
                  state.phase = BattleState.Phase.AWAITING_ENEMY_RESULT;
                  BattleNetworking.broadcastStateUpdate(state, pm, "");
                  state.directTurnPending = new AtomicInteger(realPlayers.size());
                  StringBuilder logMsg = new StringBuilder();
                  logMsg.append(actName).append(" used ").append(attack.name);
                  if (attack.targeting == MobAttack.Targeting.AOE) {
                     logMsg.append(" on everyone");
                  } else {
                     logMsg.append(" on ").append(targetStr);
                  }

                  logMsg.append(".");
                  state.directAttackLogPrefix = logMsg.toString();
                  state.directAttackDmgMap = new LinkedHashMap<>();

                  for (UUID pidx : realPlayers) {
                     ServerPlayerEntity pp = triggeringPlayer.getServer().getPlayerManager().getPlayer(pidx);
                     if (pp != null) {
                        float dmg = damagePerPlayer.getOrDefault(pidx, 0.0F);
                        dmg *= MobLevelManager.getArmorFactor(pp);
                        BattleData.PlayerProgress pidProg = data.getOrCreate(pidx);
                        CharStats effectivePid = getEffectiveStats(pidx, pidProg);
                        dmg = effectivePid.afterDefense(dmg);
                        dmg = Math.max(0.0F, dmg * (1.0F - shieldSnap.getOrDefault(pidx, 0.0F)));
                        dmg = Math.min(pp.getMaxHealth() * 0.5F, dmg);
                        if (wasDefending.getOrDefault(pidx, false)) {
                           dmg *= 0.5F;
                        }

                        dmg = heartLocketCut(triggeringPlayer, state, pidx, dmg);
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeFloat(dmg);
                        buf.writeString(actName);
                        buf.writeString(attack.name);
                        buf.writeBoolean(damagePerPlayer.containsKey(pidx));
                        buf.writeInt(attack.visual != null ? attack.visual.ordinal() : 0);
                        buf.writeInt(attack.colorOverride);
                        buf.writeInt(targets.size());

                        for (UUID t : targets) {
                           buf.writeUuid(t);
                        }

                        Net121.sendToClient(pp, TurnBattleMod.DIRECT_ENEMY_TURN, buf);
                     }
                  }
               }
            }
         }
      }
   }

   private static String applyDirectSpecial(
      MobAttack attack,
      int actingId,
      String actName,
      BattleState state,
      ServerPlayerEntity triggeringPlayer,
      List<Integer> aliveIds,
      PlayerManager pm,
      BattleData data
   ) {
      return actName + " used " + attack.name + "! " + applyDirectSpecialSide(attack.special, actingId, actName, state, triggeringPlayer, aliveIds, pm, data);
   }

   private static String applyDirectSpecialSide(
      MobAttack.Special special,
      int actingId,
      String actName,
      BattleState state,
      ServerPlayerEntity triggeringPlayer,
      List<Integer> aliveIds,
      PlayerManager pm,
      BattleData data
   ) {
      return switch (special) {
         case BUFF -> {
            int target = aliveIds.get(RNG.nextInt(aliveIds.size()));
            String tname = state.enemyNames.getOrDefault(target, actName);
            state.enemyAtkBoostMult.put(target, 1.5F);
            state.enemyAtkBoostTurns.put(target, 2);
            yield tname + "'s attack rose!";
         }
         case HEAL_MOB -> {
            int target = aliveIds.get(RNG.nextInt(aliveIds.size()));
            String tname = state.enemyNames.getOrDefault(target, actName);
            float maxHp = state.enemyMaxHpMap.getOrDefault(target, 1.0F);
            float heal = maxHp * 0.15F;
            float newHp = Math.min(maxHp, state.enemyHpMap.getOrDefault(target, maxHp) + heal);
            state.enemyHpMap.put(target, newHp);
            if (target == state.primaryEnemyId) {
               state.entityCurrentHp = newHp;
            }

            yield tname + " recovered " + Math.round(heal) + " HP!";
         }
         case CURE_MOB -> {
            int target = aliveIds.get(RNG.nextInt(aliveIds.size()));
            String tname = state.enemyNames.getOrDefault(target, actName);
            if (target == state.primaryEnemyId) {
               state.enemyFreezeTurns = 0;
               state.enemySleepTurns = 0;
               state.enemyConfused = false;
            }

            state.enemyFreezeTurnsMap.remove(target);
            state.enemySleepTurnsMap.remove(target);
            state.enemyConfusedLeftMap.remove(target);
            yield tname + " shook off its status!";
         }
         case APPLY_FREEZE -> {
            List real = state.participantOrder.stream().filter(id -> !id.equals(BattleState.PARTY_MEMBER_UUID)).toList();
            if (!real.isEmpty()) {
               UUID t = real.get(RNG.nextInt(real.size()));
               state.setPlayerStatus(t, BattleState.PlayerStatusKind.FREEZE);
               state.playerFreezeMap.put(t, 3);
               yield state.participantNames.getOrDefault(t, "Player") + " was frozen!";
            } else {
               yield "";
            }
         }
         case APPLY_SLEEP -> {
            List real = state.participantOrder.stream().filter(id -> !id.equals(BattleState.PARTY_MEMBER_UUID)).toList();
            if (!real.isEmpty()) {
               UUID t = real.get(RNG.nextInt(real.size()));
               state.setPlayerStatus(t, BattleState.PlayerStatusKind.ASLEEP);
               state.playerSleepMap.put(t, 1);
               yield state.participantNames.getOrDefault(t, "Player") + " fell asleep!";
            } else {
               yield "";
            }
         }
         case APPLY_CONFUSION -> {
            List real = state.participantOrder.stream().filter(id -> !id.equals(BattleState.PARTY_MEMBER_UUID)).toList();
            if (!real.isEmpty()) {
               UUID t = real.get(RNG.nextInt(real.size()));
               state.setPlayerStatus(t, BattleState.PlayerStatusKind.CONFUSION);
               state.playerConfusionMap.put(t, 2);
               yield state.participantNames.getOrDefault(t, "Player") + " is confused!";
            } else {
               yield "";
            }
         }
         default -> "";
      };
   }

   public static void processDirectEnemyTurnResult(ServerPlayerEntity player, float dmgTaken) {
      BattleState state = activeBattles.get(player.getUuid());
      if (state != null) {
         UUID uuid = player.getUuid();
         float currentHp = TARGET_HEALTH.getOrDefault(uuid, player.getHealth());
         boolean alreadyDowned = currentHp <= 0.0F;
         if (!alreadyDowned) {
            float newHp = currentHp - dmgTaken;
            if (!(newHp <= 0.0F) || TurnBattleConfig.get().rollingHealth && !state.isPvp) {
               if (TurnBattleConfig.get().rollingHealth) {
                  TARGET_HEALTH.put(uuid, newHp);
                  PacketByteBuf hb = PacketByteBufs.create();
                  hb.writeFloat(newHp);
                  Net121.sendToClient(player, TurnBattleMod.HP_SYNC, hb);
               } else {
                  TARGET_HEALTH.put(uuid, newHp);
                  IS_ROLLING = true;

                  try {
                     player.setHealth(Math.max(0.5F, newHp));
                  } finally {
                     IS_ROLLING = false;
                  }

                  PacketByteBuf hb = PacketByteBufs.create();
                  hb.writeFloat(newHp);
                  Net121.sendToClient(player, TurnBattleMod.HP_SYNC, hb);
               }
            } else {
               handlePlayerKO(player, uuid, player.getServer());
            }
         }

         if (state.directAttackDmgMap != null && dmgTaken > 0.0F && !alreadyDowned) {
            state.directAttackDmgMap.put(uuid, dmgTaken);
         }

         BattleData.PlayerProgress progTp = BattleData.get(player.getServer()).getOrCreate(uuid);
         int playerIQ = progTp.playerStats.iq;
         int weaponIQ = 0;
         if (progTp.equippedWeapon != null && !progTp.equippedWeapon.isEmpty()) {
            WeaponStats ws = WeaponStats.fromStack(progTp.equippedWeapon);
            if (ws != null) {
               weaponIQ = ws.netStatBonus(3);
            }
         }

         if (progTp.equippedArmor != null) {
            for (ItemStack ap : progTp.equippedArmor) {
               if (ap != null && !ap.isEmpty()) {
                  ArmorStats as = ArmorStats.fromStack(ap);
                  if (as != null && as.bonusStat == 3) {
                     weaponIQ += as.bonusAmt;
                  }
               }
            }
         }

         float tpPerGraze = 1.0F + (float)(playerIQ + weaponIQ) * 0.125F;
         if (!state.darknessConstricting) {
            state.addTpCapped(uuid, 2.0F * tpPerGraze);
         }

         if (activeBattles.containsKey(uuid)) {
            int remaining = state.directTurnPending != null ? state.directTurnPending.decrementAndGet() : 0;
            if (remaining <= 0) {
               if (!activeBattles.containsKey(uuid) && !activeBattles.containsValue(state)) {
                  return;
               }

               StringBuilder sb = new StringBuilder();
               if (state.directAttackLogPrefix != null) {
                  sb.append(state.directAttackLogPrefix);
               }

               if (state.directAttackDmgMap != null && !state.directAttackDmgMap.isEmpty()) {
                  float total = 0.0F;

                  for (float d : state.directAttackDmgMap.values()) {
                     total += d;
                  }

                  sb.append(" It dealt ").append(Math.round(total)).append(" damage!");
               }

               state.phase = BattleState.Phase.PLAYER_TURN;
               PlayerManager pmSkip = player.getServer().getPlayerManager();
               int guard = 0;

               while (!state.participantOrder.isEmpty() && isDowned(state, state.currentParticipant(), pmSkip) && guard++ < state.participantOrder.size() + 1) {
                  if (isPartyWiped(state, pmSkip)) {
                     endBattleFromPartyWipe(state, player.getServer(), pmSkip);
                     return;
                  }

                  UUID downedUuid = state.currentParticipant();
                  String downedName = state.participantNames.getOrDefault(downedUuid, isRecruitSlot(downedUuid) ? "Ally" : "Player");
                  boolean revived = tickDownedRegenAndCheckRevive(state, downedUuid, pmSkip);
                  sb.append(revived ? " " + downedName + " gets back up!" : " " + downedName + " is still down and can't act...");
                  state.advanceTurn();
               }

               if (!activeBattles.containsKey(uuid)) {
                  return;
               }

               BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), sb.toString());
            }
         }
      }
   }

   public static void processEnemyTurnResult(ServerPlayerEntity player, int hits, int grazes) {
      processEnemyTurnResult(player, hits, grazes, 0.0F);
   }

   public static void processEnemyTurnResult(ServerPlayerEntity player, int hits, int grazes, float heldTp) {
      BattleState state = activeBattles.get(player.getUuid());
      if (state != null) {
         UUID uuid = player.getUuid();
         BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(uuid);
         int playerIQ = prog.playerStats.iq;
         int playerWeaponIQ = 0;
         if (prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty()) {
            WeaponStats ws = WeaponStats.fromStack(prog.equippedWeapon);
            if (ws != null) {
               playerWeaponIQ = ws.netStatBonus(3);
            }
         }

         if (prog.equippedArmor != null) {
            for (ItemStack ap : prog.equippedArmor) {
               if (ap != null && !ap.isEmpty()) {
                  ArmorStats as = ArmorStats.fromStack(ap);
                  if (as != null && as.bonusStat == 3) {
                     playerWeaponIQ += as.bonusAmt;
                  }
               }
            }
         }

         float tpPerGraze = 1.0F + (float)(playerIQ + playerWeaponIQ) * 0.125F;
         float grazeTP = (float)grazes * tpPerGraze + heldTp;
         if (state.offhandGimmicks.getOrDefault(uuid, OffhandGimmick.NONE) == OffhandGimmick.SOUL_RESONATOR) {
            if (grazeTP > 0.0F) {
               float newHp = Math.min(player.getMaxHealth(), player.getHealth() + grazeTP);
               IS_ROLLING = true;

               try {
                  player.setHealth(newHp);
               } finally {
                  IS_ROLLING = false;
               }

               TARGET_HEALTH.put(uuid, newHp);
               state.participantHp.put(uuid, newHp);
            }
         } else if (!state.darknessConstricting) {
            state.addTpCapped(uuid, grazeTP);
            BattleNetworking.broadcastSharedTp(state, player.getServer().getPlayerManager());
         }

         state.participantHp.put(uuid, TARGET_HEALTH.getOrDefault(uuid, player.getHealth()));
         if (state.isPvp && grazes > 0 && prog.playerRace != CharacterRace.MONSTER && !state.pvpShieldBroken.getOrDefault(uuid, true)) {
            float maxShield = state.pvpShieldMax.getOrDefault(uuid, 0.0F);
            if (maxShield > 0.0F) {
               float regen = maxShield * 0.01F * (float)grazes;
               float cur = state.pvpShield.getOrDefault(uuid, 0.0F);
               float newShield = Math.min(maxShield, cur + regen);
               state.pvpShield.put(uuid, newShield);
            }
         }

         BattleNetworking.broadcastParticipantsOnly(state, player.getServer().getPlayerManager());
         int mobId = state.entityNetworkId;
         Set reported = pendingEnemyResults.computeIfAbsent(mobId, k -> new HashSet<>());
         reported.add(uuid);
         long realCount = state.participantOrder
            .stream()
            .filter(id -> !id.equals(BattleState.PARTY_MEMBER_UUID) && !id.equals(BattleState.PARTY_MEMBER_UUID_2))
            .count();
         if ((long)reported.size() >= realCount) {
            pendingEnemyResults.remove(mobId);
            pendingEnemyResultsStartTick.remove(mobId);
            state.targetParticipantHp.remove(BattleState.PARTY_MEMBER_UUID);
            state.targetParticipantHp.remove(BattleState.PARTY_MEMBER_UUID_2);
            state.phase = BattleState.Phase.PLAYER_TURN;
            state.currentParticipantIndex = 0;
            state.clampAllTpToMax();
            state.tickPlayerDefensiveState();
            PlayerManager pmSkip = player.getServer().getPlayerManager();
            int guard = 0;

            while (isDowned(state, state.currentParticipant(), pmSkip) && guard++ < state.participantOrder.size() + 1) {
               UUID downedUuid = state.currentParticipant();
               if (state.isPvp) {
                  if (state.isTeamPvp && isTeamWiped(state, downedUuid, pmSkip)) {
                     boolean isPlayerUuidsTeam = state.isAllyPair(downedUuid, state.playerUuid);
                     endBattle(state, player.getServer(), isPlayerUuidsTeam ? "lose" : "win");
                     return;
                  }
               } else if (isPartyWiped(state, pmSkip)) {
                  endBattleFromPartyWipe(state, player.getServer(), pmSkip);
                  return;
               }

               String downedName = state.participantNames.getOrDefault(downedUuid, isRecruitSlot(downedUuid) ? "Ally" : "Player");
               boolean revived = tickDownedRegenAndCheckRevive(state, downedUuid, pmSkip);
               BattleNetworking.broadcastStateUpdate(state, pmSkip, revived ? downedName + " gets back up!" : downedName + " is still down and can't act...");
               state.advanceTurn();
            }

            if (!activeBattles.containsKey(player.getUuid())) {
               return;
            }

            String msg = hits > 0
               ? String.format("Hit %d time(s), grazed %d time(s).", hits, grazes)
               : (grazes > 0 ? String.format("Flawless! +%.0f TP.", grazeTP) : "You dodged everything!");
            if (state.isPvp) {
               ServerPlayerEntity ep = player.getServer()
                  .getPlayerManager()
                  .getPlayer(primaryEnemyOf(state, player.getUuid(), player.getServer().getPlayerManager()));
               if (ep != null) {
                  state.entityCurrentHp = ep.getHealth();
                  state.enemyHpMap.put(state.entityNetworkId, ep.getHealth());
               }
            }

            BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), msg);
            if (!state.isPvp) {
               ServerWorld world = player.getServerWorld();
               LivingEntity primaryCheck = findEnemyByIndex(state, world, 0);
               boolean allDeadCheck = state.isVirtualEnemy ? state.virtualHp <= 0.0F : primaryCheck == null;
               if (allDeadCheck) {
                  if (!state.isVirtualEnemy) {
                     killFrozenExtras(state, world, player);
                  }

                  endBattle(state, player.getServer(), "win");
                  return;
               }
            }

            if (!state.isPvp && state.allEnemiesDown()) {
               triggerAllOutAttack(state, player.getServer());
            } else {
               UUID nowActing = state.currentParticipant();
               PlayerManager pmNow = player.getServer().getPlayerManager();
               if (isRecruitSlot(nowActing)) {
                  ServerPlayerEntity owner = pmNow.getPlayer(state.playerUuid);
                  if (owner != null) {
                     BattleNetworking.sendPartyMemberTurn(owner, state, nowActing);
                  }
               } else {
                  ServerPlayerEntity actingPlayer = pmNow.getPlayer(nowActing);
                  if (actingPlayer != null) {
                     BattleNetworking.sendTurnNotice(actingPlayer, state);
                  }
               }
            }
         }
      }
   }

   private static void giveAllyBonusTurn(ServerPlayerEntity player, UUID actor, UUID chosenId, BattleState state, String precedingMsg) {
      state.batonPassUsed.add(chosenId);
      PlayerManager pm = player.getServer().getPlayerManager();
      String actorName = state.participantNames.getOrDefault(actor, getPlayerDisplayName(player));
      String chosenName = state.participantNames.getOrDefault(chosenId, "?");
      String msg = (precedingMsg != null && !precedingMsg.isEmpty() ? precedingMsg + " " : "") + actorName + " gives " + chosenName + " a turn!";
      state.advanceTurn();
      int normalNextIndex = state.currentParticipantIndex;
      if (chosenId.equals(BattleState.PARTY_MEMBER_UUID)) {
         state.batonPassReturnIndex = normalNextIndex;
         BattleNetworking.broadcastStateUpdate(state, pm, msg);
         ServerPlayerEntity owner = pm.getPlayer(state.playerUuid);
         if (owner != null) {
            BattleNetworking.sendPartyMemberTurn(owner, state);
         }
      } else {
         ServerPlayerEntity chosen = pm.getPlayer(chosenId);
         if (chosen == null) {
            BattleNetworking.broadcastStateUpdate(state, pm, msg);
            UUID next = state.currentParticipant();
            if (next.equals(state.participantOrder.get(0))) {
               ServerWorld world = player.getServerWorld();
               LivingEntity currentEnemy = findEnemyByIndex(state, world, 0);
               if (currentEnemy != null || state.isVirtualEnemy) {
                  runEnemyTurn(player, currentEnemy, state, pm);
               }
            } else if (!next.equals(BattleState.PARTY_MEMBER_UUID) && !next.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               ServerPlayerEntity nextPlayer = pm.getPlayer(next);
               if (nextPlayer != null) {
                  BattleNetworking.sendTurnNotice(nextPlayer, state);
               }
            } else {
               ServerPlayerEntity owner = pm.getPlayer(state.playerUuid);
               if (owner != null) {
                  BattleNetworking.sendPartyMemberTurn(owner, state, next);
               }
            }

            return;
         }

         int chosenIdx = state.participantOrder.indexOf(chosenId);
         if (chosenIdx >= 0) {
            state.currentParticipantIndex = chosenIdx;
            state.batonPassReturnIndex = normalNextIndex;
         }

         BattleNetworking.broadcastStateUpdate(state, pm, msg);
         BattleNetworking.sendTurnNotice(chosen, state);
      }
   }

   private static void triggerAllOutAttack(BattleState state, MinecraftServer server) {
      PlayerManager pm = server.getPlayerManager();
      List allEids = new ArrayList<>();
      allEids.add(state.primaryEnemyId);
      allEids.addAll(state.extraEnemyIds);

      for (int eid : allEids) {
         state.enemyDownTurns.remove(eid);
         state.enemySturdy.merge(eid, 2, Math::max);
      }

      BattleNetworking.broadcastStateUpdate(state, pm, "The enemy kneels, giving your party a free turn!");
   }

   public static void applySoulHitDamage(ServerPlayerEntity player, float damage) {
      BattleState state = activeBattles.get(player.getUuid());
      if (state != null) {
         boolean[] hurtSoundPlayed = new boolean[]{false};
         List eligible = new ArrayList<>();
         eligible.add(player.getUuid());
         boolean hasRecruit1 = state.participantOrder.contains(BattleState.PARTY_MEMBER_UUID);
         boolean hasRecruit2 = state.participantOrder.contains(BattleState.PARTY_MEMBER_UUID_2);
         if (hasRecruit1) {
            eligible.add(BattleState.PARTY_MEMBER_UUID);
         }

         if (hasRecruit2) {
            eligible.add(BattleState.PARTY_MEMBER_UUID_2);
         }

         Object hitSet;
         if (!hasRecruit1 && !hasRecruit2) {
            hitSet = new HashSet<>(eligible);
         } else {
            List shuffled = new ArrayList<>(eligible);
            Collections.shuffle(shuffled, new Random(RNG.nextLong()));
            int hitCount = 1 + RNG.nextInt(eligible.size());
            hitSet = new HashSet<>(shuffled.subList(0, hitCount));
         }

         BattleData dataD = BattleData.get(player.getServer());
         BattleData.PlayerProgress progD = dataD.getOrCreate(state.playerUuid);
         CharStats effectiveDodge = getEffectiveStats(player.getUuid(), progD);
         if (RNG.nextFloat() < effectiveDodge.dodgeChance()) {
            state.addTpCapped(player.getUuid(), 8.0F);
            BattleNetworking.broadcastParticipantsOnly(state, player.getServer().getPlayerManager());
         } else {
            CharStats effectiveD = getEffectiveStats(player.getUuid(), progD);
            float reduced = effectiveD.afterDefense(damage);
            reduced *= 0.85F + RNG.nextFloat() * 0.3F;
            reduced = Math.min(player.getMaxHealth() * 0.5F, reduced);
            if (progD.soulType == SoulType.INTEGRITY) {
               reduced *= 0.85F;
            }

            reduced = heartLocketCut(player, state, player.getUuid(), reduced);
            float currentTarget = TARGET_HEALTH.getOrDefault(player.getUuid(), player.getHealth());
            boolean playerAlreadyDowned = currentTarget <= 0.0F;
            if (!playerAlreadyDowned && hitSet.contains(player.getUuid())) {
               float newTarget = currentTarget - reduced;
               if (newTarget <= 3.0F && progD.soulType == SoulType.PERSEVERANCE && currentTarget > 3.0F && RNG.nextFloat() < 0.5F) {
                  newTarget = 10.0F;
                  player.networkHandler
                     .sendPacket(
                        new PlaySoundS2CPacket(
                           Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_BEACON_ACTIVATE),
                           SoundCategory.PLAYERS,
                           player.getX(),
                           player.getY(),
                           player.getZ(),
                           1.0F,
                           1.5F,
                           0L
                        )
                     );
               }

               if (newTarget <= 3.0F && !progD.determinationUsed && progD.soulType == SoulType.DETERMINATION && currentTarget > 3.0F) {
                  newTarget = player.getMaxHealth();
                  progD.determinationUsed = true;
                  BattleData.get(player.getServer()).markDirty();
                  player.networkHandler
                     .sendPacket(
                        new PlaySoundS2CPacket(
                           Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_BEACON_ACTIVATE),
                           SoundCategory.PLAYERS,
                           player.getX(),
                           player.getY(),
                           player.getZ(),
                           1.0F,
                           2.0F,
                           0L
                        )
                     );
               }

               boolean justWentDown = newTarget <= 0.0F && currentTarget > 0.0F;
               boolean mortal = justWentDown && TurnBattleConfig.get().rollingHealth && !state.isPvp;
               if (justWentDown && !mortal) {
                  handlePlayerKO(player, player.getUuid(), player.getServer());
               } else {
                  state.participantHp.put(player.getUuid(), newTarget);
                  TARGET_HEALTH.put(player.getUuid(), newTarget);
                  if (!TurnBattleConfig.get().rollingHealth) {
                     IS_ROLLING = true;

                     try {
                        player.setHealth(Math.max(0.5F, newTarget));
                     } finally {
                        IS_ROLLING = false;
                     }

                     PacketByteBuf instantHpBuf = PacketByteBufs.create();
                     instantHpBuf.writeFloat(newTarget);
                     Net121.sendToClient(player, TurnBattleMod.HP_SYNC, instantHpBuf);
                  }
               }

               player.networkHandler
                  .sendPacket(
                     new PlaySoundS2CPacket(
                        Registries.SOUND_EVENT.getEntry(TurnBattleSounds.HURT),
                        SoundCategory.PLAYERS,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        1.0F,
                        1.0F,
                        0L
                     )
                  );
               hurtSoundPlayed[0] = true;
            }

            for (UUID slot : new UUID[]{BattleState.PARTY_MEMBER_UUID, BattleState.PARTY_MEMBER_UUID_2}) {
               if (state.participantOrder.contains(slot) && hitSet.contains(slot)) {
                  CharStats rStats = recruitSlotStats(player.getServer(), state, slot);
                  boolean legacy = slot.equals(BattleState.PARTY_MEMBER_UUID) && progD.partyMember != PartyMember.NONE;
                  float partyDef = rStats != null
                     ? rStats.defenseReduction()
                     : (legacy ? CharStats.defenseFraction((float)getPartyStatWithWeapon(progD, 1)) : 0.0F);
                  float partyDodge = rStats != null ? rStats.dodgeChance() : (legacy ? Math.min((float)getPartyStatWithWeapon(progD, 2) * 0.01F, 0.2F) : 0.0F);
                  if (RNG.nextFloat() < partyDodge) {
                     state.addTpCapped(slot, 8.0F);
                  } else {
                     float baseDmg = state.currentBulletBaseDmg > 0.0F ? state.currentBulletBaseDmg : damage;
                     float recruitShield = state.bulletTurnShieldSnap.getOrDefault(slot, 0.0F);
                     float shielded = baseDmg * (1.0F - recruitShield);
                     float partyBaseDmg = shielded * (1.0F - partyDef);
                     partyBaseDmg *= 0.85F + RNG.nextFloat() * 0.3F;
                     float partyHp = state.participantHp.getOrDefault(slot, 0.0F);
                     if (!(partyHp <= 0.0F)) {
                        boolean rollingR = TurnBattleConfig.get().rollingHealth && !state.isPvp;
                        float rollTgt = state.targetParticipantHp.getOrDefault(slot, partyHp);
                        if (!rollingR || !(rollTgt <= 0.0F)) {
                           if (rollingR) {
                              partyHp = rollTgt;
                           }

                           float maxHpSlot = state.participantMaxHp.getOrDefault(slot, 20.0F);
                           partyBaseDmg = Math.min(maxHpSlot * 0.5F, partyBaseDmg);
                           if (state.bulletTurnDefendSnap.getOrDefault(slot, false)) {
                              partyBaseDmg *= 0.5F;
                           }

                           SoulType rSoul = soulOf(player.getServer(), state, slot);
                           if (rSoul == SoulType.INTEGRITY) {
                              partyBaseDmg *= 0.85F;
                           }

                           partyBaseDmg = heartLocketCut(player, state, slot, partyBaseDmg);
                           float newHp = partyHp - partyBaseDmg;
                           if (newHp <= 3.0F && partyHp > 3.0F) {
                              if (rSoul == SoulType.PERSEVERANCE && RNG.nextFloat() < 0.5F) {
                                 newHp = Math.min(10.0F, maxHpSlot);
                              } else if (rSoul == SoulType.DETERMINATION && state.recruitDeterminationUsed.add(slot)) {
                                 newHp = maxHpSlot;
                              }

                              if (newHp > 3.0F) {
                                 player.networkHandler
                                    .sendPacket(
                                       new PlaySoundS2CPacket(
                                          Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_BEACON_ACTIVATE),
                                          SoundCategory.PLAYERS,
                                          player.getX(),
                                          player.getY(),
                                          player.getZ(),
                                          1.0F,
                                          1.8F,
                                          0L
                                       )
                                    );
                              }
                           }

                           if (newHp <= 0.0F) {
                              newHp = (float)(-Math.round(maxHpSlot / 2.0F));
                           }

                           if (rollingR) {
                              state.targetParticipantHp.put(slot, newHp);
                           } else {
                              state.participantHp.put(slot, newHp);
                              state.targetParticipantHp.put(slot, newHp);
                           }

                           if (!hurtSoundPlayed[0]) {
                              player.networkHandler
                                 .sendPacket(
                                    new PlaySoundS2CPacket(
                                       Registries.SOUND_EVENT.getEntry(TurnBattleSounds.HURT),
                                       SoundCategory.PLAYERS,
                                       player.getX(),
                                       player.getY(),
                                       player.getZ(),
                                       1.0F,
                                       1.0F,
                                       0L
                                    )
                                 );
                              hurtSoundPlayed[0] = true;
                           }

                           if (newHp <= 0.0F && !rollingR) {
                              String knockedName = state.participantNames.getOrDefault(slot, "Ally");
                              BattleNetworking.broadcastStateUpdate(state, player.getServer().getPlayerManager(), knockedName + " has been downed!");
                           }
                        }
                     }
                  }
               }
            }

            BattleNetworking.broadcastParticipantsOnly(state, player.getServer().getPlayerManager());
         }
      }
   }

   public static void endBattle(BattleState state, MinecraftServer server, String outcome) {
      int darkDollarsWon = ("win".equals(outcome) || "mercy".equals(outcome)) && !state.isPvp ? darkDollarsFor(state, server) : 0;
      BattleStaging.cleanup(state, server);
      if (state.magicant) {
         MagicantManager.onBattleEnd(state, server, outcome);
      }

      if (state.depthsRun) {
         DepthsRun.onBattleEnd(state, server, outcome);
      }

      if (state.isPvp) {
         for (UUID pid : new ArrayList<>(state.participantOrder)) {
            if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               settleRolling(server, pid);
               activeBattles.remove(pid);
               TARGET_HEALTH.remove(pid);
               patienceRegenTicks.remove(pid);
               secondWindUsed.remove(pid);
               downedPlayers.remove(pid);
               if (runInvincibleUntil.remove(pid) != null) {
                  ServerPlayerEntity ppInv = server.getPlayerManager().getPlayer(pid);
                  if (ppInv != null) {
                     ppInv.setInvulnerable(false);
                  }
               }

               battleCooldowns.put(pid, ((ServerWorld)server.getWorlds().iterator().next()).getTime() + 240L);
               ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pid);
               if (pp != null) {
                  String pOutcome;
                  if ("win".equals(outcome)) {
                     pOutcome = state.isAllyPair(pid, state.playerUuid) ? "win" : "lose";
                  } else if ("lose".equals(outcome)) {
                     pOutcome = state.isAllyPair(pid, state.playerUuid) ? "lose" : "win";
                  } else {
                     pOutcome = outcome;
                  }

                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeString(pOutcome);
                  buf.writeInt(0);
                  Net121.sendToClient(pp, TurnBattleMod.BATTLE_END, buf);
                  if ("lose".equals(pOutcome) && pp.isAlive()) {
                     applyBattleDamage = true;

                     try {
                        pp.damage(pp.getDamageSources().magic(), pp.getMaxHealth() * 100.0F);
                     } finally {
                        applyBattleDamage = false;
                     }
                  }
               }
            }
         }

         pendingEnemyResults.remove(state.entityNetworkId);
         pendingEnemyResultsStartTick.remove(state.entityNetworkId);
      } else {
         battleByMob.remove(state.entityNetworkId);
         frozenEntityIds.remove(state.entityNetworkId);
         ServerPlayerEntity _ep = server.getPlayerManager().getPlayer(state.playerUuid);
         ServerWorld _ew = _ep != null && _ep.getWorld() instanceof ServerWorld _sw ? _sw : null;
         if (_ew != null && _ew.getEntityById(state.entityNetworkId) instanceof MobEntity _me) {
            wakeMob(_me);
         }

         pendingEnemyResults.remove(state.entityNetworkId);
         pendingEnemyResultsStartTick.remove(state.entityNetworkId);
         battleByMob.remove(state.primaryEnemyId);
         frozenEntityIds.remove(state.primaryEnemyId);
         _ep = server.getPlayerManager().getPlayer(state.playerUuid);
         _ew = _ep != null && _ep.getWorld() instanceof ServerWorld _sw2 ? _sw2 : null;
         if (_ew != null && _ew.getEntityById(state.primaryEnemyId) instanceof MobEntity _me2) {
            wakeMob(_me2);
         }

         activeBattles.remove(BattleState.PARTY_MEMBER_UUID);
         _ep = server.getPlayerManager().getPlayer(state.playerUuid);
         _ew = _ep != null && _ep.getWorld() instanceof ServerWorld _sw3 ? _sw3 : null;

         for (int eid : new ArrayList<>(state.extraEnemyIds)) {
            battleByMob.remove(eid);
            frozenEntityIds.remove(eid);
            if (_ew != null && _ew.getEntityById(eid) instanceof MobEntity _exm) {
               wakeMob(_exm);
            }
         }

         if ("win".equals(outcome) || "mercy".equals(outcome)) {
            _ep = server.getPlayerManager().getPlayer(state.playerUuid);
            _ew = _ep != null && _ep.getWorld() instanceof ServerWorld sw ? sw : server.getWorld(World.OVERWORLD);
            BestiaryData bestiary = BestiaryData.get(server);
            boolean killed = "win".equals(outcome);
            if (_ew != null) {
               DarknerBosses.onBattleWon(state, _ew, killed);
            }

            if (_ew != null && !state.isVirtualEnemy) {
               Entity primaryEnt = _ew.getEntityById(state.primaryEnemyId);
               if (primaryEnt instanceof LivingEntity le) {
                  bestiary.recordDefeat(state.playerUuid, le.getType());
                  dropBattleLoot(le, _ew, killed, _ep);
               }

               DarknerBosses.onBeaten(primaryEnt, _ew, killed);
               if (primaryEnt != null) {
                  sendFate(state, _ew, state.primaryEnemyId, primaryEnt, !killed);
                  if (primaryEnt instanceof EnderDragonEntity dragon) {
                     dragon.kill();
                  } else {
                     discardEnemy(primaryEnt);
                  }
               }

               if (state.primaryEnemyId != state.entityNetworkId) {
                  Entity origEnt = _ew.getEntityById(state.entityNetworkId);
                  if (origEnt instanceof LivingEntity le) {
                     bestiary.recordDefeat(state.playerUuid, le.getType());
                     dropBattleLoot(le, _ew, killed, _ep);
                  }

                  if (origEnt != null) {
                     sendFate(state, _ew, state.entityNetworkId, origEnt, !killed);
                     if (origEnt instanceof EnderDragonEntity dragon) {
                        dragon.kill();
                     } else {
                        discardEnemy(origEnt);
                     }
                  }
               }

               for (int eidx : new ArrayList<>(state.extraEnemyIds)) {
                  Entity extraEnt = _ew.getEntityById(eidx);
                  if (extraEnt instanceof LivingEntity le) {
                     bestiary.recordDefeat(state.playerUuid, le.getType());
                     dropBattleLoot(le, _ew, killed, _ep);
                  }

                  DarknerBosses.onBeaten(extraEnt, _ew, killed);
                  if (extraEnt != null) {
                     sendFate(state, _ew, eidx, extraEnt, !killed);
                     discardEnemy(extraEnt);
                  }
               }
            }
         }

         boolean giveExp = "win".equals(outcome) || "mercy".equals(outcome);
         int expGained = 0;
         if (giveExp) {
            ServerPlayerEntity expPlayer = server.getPlayerManager().getPlayer(state.playerUuid);
            ServerWorld expWorld = expPlayer != null && expPlayer.getWorld() instanceof ServerWorld sw2 ? sw2 : server.getWorld(World.OVERWORLD);

            for (Entry entry : state.enemyMaxHpMap.entrySet()) {
               int eidx = entry.getKey();
               float maxHp = entry.getValue();
               float expMult = 1.0F;
               if (expWorld != null && expWorld.getEntityById(eidx) instanceof LivingEntity le) {
                  expMult = MobLevelManager.getMobExpMult(le);
               }

               expGained += Math.max(2, (int)(maxHp * 0.5F * expMult));
            }

            expGained = Math.max(2, expGained);
            int eliteTier = EliteMobManager.getTier(state.entityUuid);
            if (eliteTier < 0 && state.primaryEnemyId >= 0) {
               eliteTier = -1;
            }

            if (eliteTier >= 0 && eliteTier < EliteMobManager.EXP_MULT.length) {
               expGained = Math.round((float)expGained * EliteMobManager.EXP_MULT[eliteTier]);
            }

            if (state.bossBeaconPos != null && state.bossDifficultyOrdinal >= 0 && state.bossDifficultyOrdinal < BossBeaconManager.EXP_MULT.length) {
               expGained = Math.round((float)expGained * BossBeaconManager.EXP_MULT[state.bossDifficultyOrdinal]);
            }
         }

         float savedPartyHp = state.participantHp.getOrDefault(BattleState.PARTY_MEMBER_UUID, -1.0F);
         boolean restoreParty = false;

         for (UUID pidx : new ArrayList<>(state.participantOrder)) {
            if (!pidx.equals(BattleState.PARTY_MEMBER_UUID) && !pidx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
               settleRolling(server, pidx);
               activeBattles.remove(pidx);
               TARGET_HEALTH.remove(pidx);
               patienceRegenTicks.remove(pidx);
               secondWindUsed.remove(pidx);
               downedPlayers.remove(pidx);
               if (!"lose".equals(outcome)) {
                  long expiry = server.getOverworld().getTime() + 240L;
                  battleCooldowns.put(pidx, expiry);
                  if (("run".equals(outcome) || "win".equals(outcome) || "mercy".equals(outcome)) && !DepthsDescentManager.isInDepths(pidx)) {
                     runInvincibleUntil.put(pidx, expiry);
                     ServerPlayerEntity pp2 = server.getPlayerManager().getPlayer(pidx);
                     if (pp2 != null) {
                        pp2.setInvulnerable(true);
                     }
                  }
               }

               ServerPlayerEntity ppCleanup = server.getPlayerManager().getPlayer(pidx);
               if (ppCleanup != null) {
                  if (StoryManager.isKrisBattle(ppCleanup, state.primaryEnemyId)) {
                     StoryManager.onKrisBattleEnd(ppCleanup, outcome);
                  }

                  if (ppCleanup != null && ("win".equals(outcome) || "mercy".equals(outcome)) && "minecraft:pillager".equals(state.entityTypeKey)) {
                  }
               }

               ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pidx);
               if (pp != null) {
                  if (!giveExp && savedPartyHp >= 0.0F) {
                     BattleData data = BattleData.get(server);
                     BattleData.PlayerProgress prog = data.getOrCreate(pidx);
                     RecruitData.RecruitState rsNE = getActiveRecruit(server, pidx);
                     if (rsNE != null) {
                        float rMax = RecruitData.maxHp(rsNE);
                        rsNE.hp = "lose".equals(outcome) ? rMax : Math.max(1.0F, savedPartyHp);
                        RecruitData.get(server).markDirty();
                     } else if (prog.partyMember != PartyMember.NONE) {
                        int mi2 = prog.partyMember.ordinal();
                        float partyMax = computePartyMemberMaxHp(prog.partyLevels[mi2], prog.partyStats[mi2]);
                        prog.partyMemberHp = "lose".equals(outcome) ? partyMax : Math.max(1.0F, savedPartyHp);
                        data.markDirty();
                     }
                  }

                  if (giveExp) {
                     BattleData data = BattleData.get(server);
                     int finalExp = RogueliteManager.scaleExp(server, pidx, expGained);
                     RogueliteManager.onBattleWon(server, pidx);
                     int levelsGained = data.addExp(pidx, finalExp);
                     BattleData.PlayerProgress prog = data.getOrCreate(pidx);
                     RecruitData rd2 = RecruitData.get(server);
                     if (levelsGained > 0) {
                        for (int lg = 0; lg < levelsGained; lg++) {
                           rd2.onPlayerLevelUp(pidx);
                        }

                        rd2.markDirty();
                     }

                     RecruitData.RecruitState rsEnd2 = getActiveRecruit(server, pidx);
                     if (rsEnd2 != null) {
                        if (savedPartyHp >= 0.0F) {
                           float rMax2 = RecruitData.maxHp(rsEnd2);
                           rsEnd2.hp = restoreParty ? rMax2 : Math.max(1.0F, savedPartyHp);
                        }

                        rd2.markDirty();
                     } else if (savedPartyHp >= 0.0F && prog.partyMember != PartyMember.NONE) {
                        int mi3 = prog.partyMember.ordinal();
                        float partyMax = computePartyMemberMaxHp(prog.partyLevels[mi3], prog.partyStats[mi3]);
                        prog.partyMemberHp = restoreParty ? partyMax : Math.max(1.0F, savedPartyHp);
                     }

                     data.markDirty();
                     ServerPlayerEntity ppForFountain = server.getPlayerManager().getPlayer(pidx);
                     if (ppForFountain != null && DarkFountainManager.isDarkWorld(ppForFountain.getWorld().getRegistryKey())) {
                     }

                     if (state.questlineBattleId != null) {
                        boolean won = "win".equals(outcome) || "mercy".equals(outcome);
                        boolean spared = "mercy".equals(outcome);
                        ServerPlayerEntity ppQl = server.getPlayerManager().getPlayer(pidx);
                        if (ppQl != null) {
                           if ("ori".equals(state.questlineBattleId)) {
                              BattleData.PlayerProgress qlProg = BattleData.get(server).getOrCreate(pidx);
                              if (qlProg.questlineNodeIndex == 0) {
                                 QuestlineManager.recordAntonioSpared(pidx, spared);
                              }
                           }

                           QuestlineManager.onQuestlineBattleEnd(ppQl, won);
                        }

                        state.questlineBattleId = null;
                     }

                     ServerPlayerEntity ppMorality = server.getPlayerManager().getPlayer(pidx);
                     if (ppMorality != null) {
                        MoralityManager.onBattleEnd(ppMorality, outcome);
                     }

                     ServerPlayerEntity ppStory = server.getPlayerManager().getPlayer(pidx);
                     if (levelsGained > 0) {
                        MobLevelManager.applyPlayerStats(pp, prog.level, prog.playerStats);
                        pp.setHealth(pp.getMaxHealth());
                     }

                     PacketByteBuf lvlBuf = PacketByteBufs.create();
                     lvlBuf.writeInt(prog.level);
                     lvlBuf.writeInt(prog.exp);
                     lvlBuf.writeInt(BattleData.expNeeded(prog.level));
                     lvlBuf.writeBoolean(levelsGained > 0);
                     Net121.sendToClient(pp, TurnBattleMod.LEVEL_UPDATE, lvlBuf);
                  }

                  if (darkDollarsWon > 0) {
                     BattleData.PlayerProgress ddProg = BattleData.get(server).getOrCreate(pidx);
                     ddProg.darkDollars += darkDollarsWon;
                     BattleData.get(server).markDirty();
                     TurnBattleMod.sendStatsExtra(pp);
                  }

                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeString(outcome);
                  buf.writeInt(expGained);
                  buf.writeInt(darkDollarsWon);
                  Net121.sendToClient(pp, TurnBattleMod.BATTLE_END, buf);
               }
            }
         }

         if (state.depthsFountainKey != null) {
            boolean won = "win".equals(outcome) || "mercy".equals(outcome) || "run".equals(outcome);
            DepthsDescentManager.onBattleResolved(server, state.depthsFountainKey, won);
            state.depthsFountainKey = null;
         }

         int eliteTierEnd = EliteMobManager.getTier(state.entityUuid);
         if (eliteTierEnd >= 0 && ("win".equals(outcome) || "mercy".equals(outcome))) {
            EliteMobManager.grantLoot(server, state, eliteTierEnd);
            EliteMobManager.remove(state.entityUuid);
         }

         if (state.bossBeaconPos != null) {
            if (!"win".equals(outcome) && !"mercy".equals(outcome)) {
               ServerWorld bossWorld = server.getWorld(World.OVERWORLD);
               if (bossWorld != null) {
                  Entity bossEnt = bossWorld.getEntityById(state.primaryEnemyId);
                  if (bossEnt != null) {
                     discardEnemy(bossEnt);
                  }
               }
            } else {
               if (state.bossDifficultyOrdinal >= 0) {
                  int itemCount = BossBeaconManager.getBossItemCount(state.bossDifficultyOrdinal);

                  for (UUID pidxx : new ArrayList<>(state.participantOrder)) {
                     if (!pidxx.equals(BattleState.PARTY_MEMBER_UUID) && !pidxx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                        ServerPlayerEntity pp2 = server.getPlayerManager().getPlayer(pidxx);
                        if (pp2 != null) {
                           BattleData bData2 = BattleData.get(server);
                           BattleData.PlayerProgress bProg2 = bData2.getOrCreate(pidxx);
                           Item lootItem = BossBeaconManager.getBossLootItem(bProg2.level, RNG);
                           int cappedCount = itemCount;
                           if (lootItem == Items.DIAMOND) {
                              cappedCount = Math.min(itemCount, 5);
                           } else if (lootItem == Items.NETHERITE_INGOT) {
                              cappedCount = Math.min(itemCount, 1);
                           }

                           ItemStack loot = new ItemStack(lootItem, cappedCount);
                           if (!pp2.getInventory().insertStack(loot)) {
                              pp2.dropItem(loot, false);
                           }

                           pp2.sendMessage(
                              Text.literal("\u2726 Boss defeated!  +" + cappedCount + "x " + lootItem.getName().getString() + "!").formatted(Formatting.GOLD),
                              false
                           );
                        }
                     }
                  }
               }

               BossBeaconManager.onBossVictory(server, state);
            }
         }
      }
   }

   public static void endBattle(ServerPlayerEntity player, BattleState state, String outcome) {
      endBattle(state, player.getServer(), outcome);
   }

   private static boolean rollBashCrit(MinecraftServer server, BattleState state, UUID actor) {
      boolean actorIsAnyPartyMember = isRecruitSlot(actor);
      BattleData data = BattleData.get(server);
      CharStats actorStats;
      if (actorIsAnyPartyMember) {
         RecruitData rd = RecruitData.get(server);
         RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
         boolean isSlot2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
         int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            actorStats = ci < pr.customRecruits.size() ? pr.customRecruits.get(ci).state.stats : getEffectiveStats(actor, data.getOrCreate(state.playerUuid));
         } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
            actorStats = pr.recruits[slotCode].stats;
         } else {
            actorStats = getEffectiveStats(actor, data.getOrCreate(state.playerUuid));
         }
      } else {
         actorStats = getEffectiveStats(actor, data.getOrCreate(actor));
      }

      return RNG.nextFloat() < actorStats.critChance();
   }

   public static void onBashQteRoll(ServerPlayerEntity player, int targetIndex) {
      UUID uuid = player.getUuid();
      BattleState state = activeBattles.get(uuid);
      if (state != null && state.phase == BattleState.Phase.PLAYER_TURN) {
         if (!state.resolving) {
            if (queuedTurns(state)) {
               processAction(player, 0, 0, targetIndex);
            } else {
               UUID actor = state.currentParticipant();
               boolean actorIsAnyPartyMember = isRecruitSlot(actor);
               if (actor.equals(uuid) || actorIsAnyPartyMember && state.playerUuid.equals(uuid)) {
                  if (applyPreActionStatusChecks(player, state, actor)) {
                     boolean crit = rollBashCrit(player.getServer(), state, actor);
                     float critZoneCenter = 0.1F;
                     state.pendingBashCrit.put(actor, crit);
                     state.pendingBashTarget.put(actor, targetIndex);
                     BattleNetworking.broadcastBashQteStart(state, player.getServer().getPlayerManager(), actor, targetIndex, crit, critZoneCenter);
                  }
               }
            }
         }
      }
   }

   public static void onBashQteResult(ServerPlayerEntity player, float qteMultiplier, int qteTpTier) {
      UUID uuid = player.getUuid();
      BattleState state = activeBattles.get(uuid);
      if (state != null && state.phase == BattleState.Phase.PLAYER_TURN) {
         if (!state.resolving) {
            UUID actor = state.currentParticipant();
            boolean actorIsAnyPartyMember = isRecruitSlot(actor);
            if (actor.equals(uuid) || actorIsAnyPartyMember && state.playerUuid.equals(uuid)) {
               Boolean crit = state.pendingBashCrit.remove(actor);
               Integer targetIndex = state.pendingBashTarget.remove(actor);
               if (targetIndex != null) {
                  PlayerManager pm = player.getServer().getPlayerManager();
                  ServerWorld world = player.getServerWorld();
                  BattleData data = BattleData.get(player.getServer());
                  BattleData.PlayerProgress prog = data.getOrCreate(state.playerUuid);
                  BattleData.PlayerProgress actorProg = actorIsAnyPartyMember ? prog : data.getOrCreate(uuid);
                  int level = actorIsAnyPartyMember ? prog.level : actorProg.level;
                  BattleNetworking.broadcastBashPlay(state, pm, actor, targetIndex);
                  String message = doBash(player, actor, state, level, targetIndex, world, crit, qteMultiplier, qteTpTier);
                  afterParticipantAction(player, state, message, false, world);
               }
            }
         }
      }
   }

   private static void setRecruitGimmick(BattleState state, UUID slotUuid, ItemStack accessory) {
      if (accessory != null && !accessory.isEmpty()) {
         OffhandGimmick g = OffhandGimmick.fromStack(accessory);
         if (g != OffhandGimmick.NONE) {
            state.offhandGimmicks.put(slotUuid, g);
         }
      }
   }

   private static OffhandGimmick resolveActorGimmick(ServerPlayerEntity player, BattleState state, UUID actor, BattleData.PlayerProgress prog) {
      boolean isSlot2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
      boolean isAnyPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || isSlot2;
      if (!isAnyPartyMember) {
         return OffhandGimmick.fromStack(prog.equippedAccessory);
      } else {
         RecruitData rd = RecruitData.get(player.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
         int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            if (ci < pr.customRecruits.size()) {
               return OffhandGimmick.fromStack(pr.customRecruits.get(ci).state.equippedAccessory);
            }
         } else {
            if (slotCode >= 0 && slotCode < Recruit.values().length) {
               return OffhandGimmick.fromStack(pr.recruits[slotCode].equippedAccessory);
            }

            if (!isSlot2) {
               Recruit activeRecruit = getActiveRecruitEnum(player.getServer(), state.playerUuid);
               if (activeRecruit != null) {
                  return OffhandGimmick.fromStack(rd.getOrCreate(state.playerUuid).get(activeRecruit).equippedAccessory);
               }
            }
         }

         return OffhandGimmick.NONE;
      }
   }

   private static CharStats getRecruitEffectiveStats(RecruitData.RecruitState rs) {
      return RecruitData.effectiveStats(rs);
   }

   static SoulType soulOf(MinecraftServer server, BattleState state, UUID who) {
      if (!isRecruitSlot(who)) {
         return BattleData.get(server).getOrCreate(who).soulType;
      } else {
         RecruitData.PlayerRecruits pr = RecruitData.get(server).getOrCreate(state.playerUuid);
         int code = who.equals(BattleState.PARTY_MEMBER_UUID_2) ? pr.secondActiveRecruit : pr.activeRecruit;
         if (code >= 1000) {
            int ci = code - 1000;
            return ci < pr.customRecruits.size() ? pr.customRecruits.get(ci).soulType : null;
         } else {
            return code >= 0 && code < Recruit.values().length ? Recruit.values()[code].soulType : null;
         }
      }
   }

   private static void applyRecruitStartPassives(MinecraftServer server, BattleState state) {
      for (UUID slot : new UUID[]{BattleState.PARTY_MEMBER_UUID, BattleState.PARTY_MEMBER_UUID_2}) {
         if (state.participantOrder.contains(slot) && soulOf(server, state, slot) == SoulType.BRAVERY) {
            state.addTp(slot, 25.0F);
         }
      }
   }

   private static CharStats recruitSlotStats(MinecraftServer server, BattleState state, UUID slot) {
      if (!isRecruitSlot(slot)) {
         return null;
      } else {
         RecruitData.PlayerRecruits pr = RecruitData.get(server).getOrCreate(state.playerUuid);
         int slotCode = slot.equals(BattleState.PARTY_MEMBER_UUID_2) ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            return ci < pr.customRecruits.size() ? getRecruitEffectiveStats(pr.customRecruits.get(ci).state) : null;
         } else {
            return slotCode >= 0 && slotCode < Recruit.values().length ? getRecruitEffectiveStats(pr.recruits[slotCode]) : null;
         }
      }
   }

   static CharStats effectiveStatsOf(UUID actor, BattleData.PlayerProgress prog) {
      return getEffectiveStats(actor, prog);
   }

   private static CharStats getEffectiveStats(UUID actor, BattleData.PlayerProgress prog) {
      if (actor.equals(BattleState.PARTY_MEMBER_UUID)) {
         return prog.partyStats[prog.partyMember.ordinal()];
      } else {
         WeaponStats ws = WeaponStats.fromStack(prog.equippedWeapon);
         boolean hasWeapon = ws != null;
         boolean hasArmor = prog.equippedArmor != null
            && (prog.equippedArmor[0] != null && !prog.equippedArmor[0].isEmpty() || prog.equippedArmor[1] != null && !prog.equippedArmor[1].isEmpty());
         if (!hasWeapon && !hasArmor) {
            return prog.playerStats;
         } else {
            CharStats effective = prog.playerStats.copy();
            if (hasWeapon) {
               addStatBonus(effective, ws.bonusStat, ws.bonusAmt);
               if (ws.isQuirk) {
                  addStatBonus(effective, ws.quirkStat, 10);
                  addStatBonus(effective, ws.quirkMinus, -10);
               }
            }

            if (hasArmor) {
               for (int ai = 0; ai < 2; ai++) {
                  ItemStack piece = prog.equippedArmor[ai];
                  if (piece != null && !piece.isEmpty()) {
                     ArmorStats as = ArmorStats.fromStack(piece);
                     if (as != null) {
                        addStatBonus(effective, as.bonusStat, as.bonusAmt);
                     }
                  }
               }
            }

            return effective;
         }
      }
   }

   private static void addStatBonus(CharStats s, int statIndex, int amount) {
      switch (statIndex) {
         case 0:
            s.offense = Math.max(0, s.offense + amount);
            break;
         case 1:
            s.defense = Math.max(0, s.defense + amount);
            break;
         case 2:
            s.magic = Math.max(0, s.magic + amount);
            break;
         case 3:
            s.iq = Math.max(0, s.iq + amount);
            break;
         case 4:
            s.vitality = Math.max(0, s.vitality + amount);
            break;
         case 5:
            s.luck = Math.max(0, s.luck + amount);
      }
   }

   private static LivingEntity findEnemy(BattleState state, ServerWorld world) {
      if (state.isVirtualEnemy) {
         return null;
      } else {
         if (world.getEntityById(state.entityNetworkId) instanceof LivingEntity le && le.isAlive()) {
            return le;
         }

         return null;
      }
   }

   private static int getEnemyIdByIndex(BattleState state, int idx) {
      if (idx <= 0) {
         return state.primaryEnemyId;
      } else {
         int extraIdx = idx - 1;
         return extraIdx < state.extraEnemyIds.size() ? state.extraEnemyIds.get(extraIdx) : state.primaryEnemyId;
      }
   }

   static void reconcileEnemyLiveness(BattleState state, PlayerManager pm) {
      if (!state.isPvp && !state.isVirtualEnemy) {
         ServerPlayerEntity anchor = pm.getPlayer(state.playerUuid);
         if (anchor == null) {
            for (UUID pid : state.participantOrder) {
               anchor = pm.getPlayer(pid);
               if (anchor != null) {
                  break;
               }
            }
         }

         if (anchor != null) {
            ServerWorld world = anchor.getServerWorld();
            List ids = new ArrayList<>();
            ids.add(state.primaryEnemyId);
            ids.addAll(state.extraEnemyIds);

            for (int eid : ids) {
               if (!(world.getEntityById(eid) instanceof LivingEntity le) || !le.isAlive()) {
                  state.enemyHpMap.put(eid, 0.0F);
               }
            }
         }
      }
   }

   static int enemyIdForIndex(BattleState state, int idx) {
      return getEnemyIdByIndex(state, idx);
   }

   static LivingEntity findEnemyByIndex(BattleState state, ServerWorld world, int idx) {
      if (state.isVirtualEnemy) {
         return null;
      } else {
         int id = getEnemyIdByIndex(state, idx);
         if (world.getEntityById(id) instanceof LivingEntity le && le.isAlive()) {
            return le;
         }

         return null;
      }
   }

   private static void discardEnemy(Entity e) {
      if (e != null && !(e instanceof PlayerEntity)) {
         e.discard();
      }
   }

   private static boolean spareOneEnemy(BattleState state, ServerWorld world, int enemyId, int targetIndex, MinecraftServer server) {
      if (state.isPvp) {
         return false;
      } else {
         if (state.bossLivesRemaining > 0 && (state.isVirtualEnemy ? targetIndex == 0 : state.holdsLives(enemyId))) {
            state.bossLivesRemaining--;
            state.spareHpThreshold = 0.0F;
            state.enemySpareThresholds.clear();
            if (state.bossLivesRemaining > 0) {
               if (state.isVirtualEnemy) {
                  float restore = state.entityMaxHp;
                  state.virtualHp = restore;
                  state.entityCurrentHp = restore;
                  state.enemyHpMap.put(Integer.MIN_VALUE, restore);
               } else if (world.getEntityById(enemyId) instanceof LivingEntity livingBoss) {
                  float capHp3 = state.enemyMaxHpMap.getOrDefault(enemyId, livingBoss.getMaxHealth());
                  IS_ROLLING = true;

                  try {
                     livingBoss.setHealth(capHp3);
                  } finally {
                     IS_ROLLING = false;
                  }

                  if (enemyId == state.primaryEnemyId) {
                     state.entityCurrentHp = capHp3;
                  }

                  state.enemyHpMap.put(enemyId, capHp3);
               }

               String lifeMsg = state.entityName
                  + " was shown mercy and lost a life! "
                  + state.bossLivesRemaining
                  + " "
                  + (state.bossLivesRemaining == 1 ? "life" : "lives")
                  + " remaining!";
               broadcastLifeLost(state, lifeMsg, server);
               return false;
            }

            String lifeMsg = state.entityName + " was shown mercy on their final life!";
            broadcastLifeLost(state, lifeMsg, server);
         }

         if (state.isVirtualEnemy) {
            state.virtualHp = 0.0F;
            state.entityCurrentHp = 0.0F;
            state.enemyHpMap.put(Integer.MIN_VALUE, 0.0F);
            return true;
         } else {
            Entity e = world.getEntityById(enemyId);
            DarknerBosses.onBeaten(e, world, false);
            sendFate(state, world, enemyId, e, true);
            if (e instanceof EnderDragonEntity dragon) {
               dragon.kill();
            } else {
               discardEnemy(e);
            }

            battleByMob.remove(enemyId);
            frozenEntityIds.remove(enemyId);
            if (e instanceof MobEntity me) {
               wakeMob(me);
            }

            state.enemyHpMap.put(enemyId, 0.0F);
            if (targetIndex == 0) {
               if (!state.extraEnemyIds.isEmpty()) {
                  promoteNextEnemy(state, world);
               }
            } else {
               state.extraEnemyIds.remove(Integer.valueOf(enemyId));
            }

            return true;
         }
      }
   }

   static void sendFate(BattleState state, ServerWorld world, int enemyId, Entity e, boolean spared) {
      if (state != null && world != null && e != null && !state.isVirtualEnemy && !state.isPvp) {
         if (!(e instanceof PlayerEntity)) {
            if (!(e instanceof EnderDragonEntity)) {
               if (state.fateSent.add(enemyId)) {
                  int kind = spared ? 1 : MobFateNet.killKind(state, enemyId, world.getTime());
                  MobFateNet.send(world, e, kind);
               }
            }
         }
      }
   }

   private static void promoteNextEnemy(BattleState state, ServerWorld world) {
      Iterator it = state.extraEnemyIds.iterator();

      while (it.hasNext()) {
         int eid = it.next();
         Entity e = world.getEntityById(eid);
         if (e instanceof LivingEntity le && le.isAlive()) {
            it.remove();
            state.primaryEnemyId = eid;
            state.entityCurrentHp = le.getHealth();
            state.enemyHpMap.put(eid, le.getHealth());
            state.enemyMaxHpMap.put(eid, le.getMaxHealth());
            state.enemyNames.put(eid, le.getDisplayName().getString());
            return;
         }

         it.remove();
         sendFate(state, world, eid, e, false);
         discardEnemy(e);
      }
   }

   private static void killFrozenExtras(BattleState state, ServerWorld world, ServerPlayerEntity killer) {
      BestiaryData bestiary = BestiaryData.get(world.getServer());

      for (int eid : state.extraEnemyIds) {
         frozenEntityIds.remove(eid);
         battleByMob.remove(eid);
         Entity e = world.getEntityById(eid);
         if (e instanceof MobEntity me) {
            wakeMob(me);
         }

         if (e instanceof LivingEntity le) {
            bestiary.recordDefeat(state.playerUuid, le.getType());
            dropBattleLoot(le, world, true, killer);
         }

         sendFate(state, world, eid, e, false);
         discardEnemy(e);
      }

      state.extraEnemyIds.clear();
   }

   static float getEnemyDamage(LivingEntity enemy, int playerLevel) {
      if (enemy == null) {
         return 2.5F * (0.3F + (float)playerLevel * 0.04F);
      } else {
         EntityAttributeInstance atk = enemy.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
         float base = atk != null && atk.getValue() > 0.0 ? (float)atk.getValue() : Math.max(1.5F, enemy.getMaxHealth() / 8.0F);
         int mobLevel = MobLevelManager.getMobLevel(enemy);
         int effectiveLevel = Math.max(mobLevel, playerLevel);
         return base * (0.3F + (float)effectiveLevel * 0.04F);
      }
   }

   public static float computePartyMemberMaxHpPublic(int level, CharStats stats) {
      return computePartyMemberMaxHp(level, stats);
   }

   private static float computePartyMemberMaxHp(int level, CharStats stats) {
      return 20.0F + stats.vitalityHp();
   }

   private static int getPartyStatWithWeapon(BattleData.PlayerProgress prog, int statIndex) {
      if (prog.partyMember == PartyMember.NONE) {
         return 0;
      } else {
         int baseStat = switch (statIndex) {
            case 0 -> prog.partyStats[prog.partyMember.ordinal()].offense;
            case 1 -> prog.partyStats[prog.partyMember.ordinal()].defense;
            case 2 -> prog.partyStats[prog.partyMember.ordinal()].magic;
            case 3 -> prog.partyStats[prog.partyMember.ordinal()].iq;
            case 4 -> prog.partyStats[prog.partyMember.ordinal()].vitality;
            case 5 -> prog.partyStats[prog.partyMember.ordinal()].luck;
            default -> 0;
         };
         int weaponBonus = 0;
         if (prog.partyEquippedWeapon != null && !prog.partyEquippedWeapon.isEmpty()) {
            WeaponStats ws = WeaponStats.fromStack(prog.partyEquippedWeapon);
            if (ws != null) {
               weaponBonus = ws.netStatBonus(statIndex);
            }
         }

         if (prog.equippedArmor != null) {
            for (ItemStack ap : prog.equippedArmor) {
               if (ap != null && !ap.isEmpty()) {
                  ArmorStats as = ArmorStats.fromStack(ap);
                  if (as != null && as.bonusStat == statIndex) {
                     weaponBonus += as.bonusAmt;
                  }
               }
            }
         }

         return baseStat + weaponBonus;
      }
   }

   private static void dropBattleLoot(LivingEntity entity, ServerWorld world, boolean killed, ServerPlayerEntity killer) {
      if (killed) {
         HumanEnemies.onDefeated(entity, world, killer);
         RegistryKey lootTableKey = entity.getLootTable();
         Identifier lootTableId = lootTableKey.getValue();
         if (lootTableId != null) {
            LootTable table = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);
            if (table != LootTable.EMPTY) {
               Builder params = new Builder(world)
                  .add(LootContextParameters.THIS_ENTITY, entity)
                  .add(LootContextParameters.ORIGIN, entity.getPos())
                  .add(LootContextParameters.DAMAGE_SOURCE, killer != null ? world.getDamageSources().playerAttack(killer) : world.getDamageSources().generic())
                  .addOptional(LootContextParameters.LAST_DAMAGE_PLAYER, killer)
                  .addOptional(LootContextParameters.ATTACKING_ENTITY, killer);
               LootContextParameterSet paramSet = params.build(LootContextTypes.ENTITY);

               for (ItemStack drop : table.generateLoot(paramSet)) {
                  if (!drop.isEmpty()) {
                     ItemEntity itemEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), drop);
                     itemEntity.setPickupDelay(10);
                     world.spawnEntity(itemEntity);
                  }
               }

               return;
            }
         }

         Random rng = new Random();
         List drops = new ArrayList<>();
         if (entity instanceof ZombieEntity || entity instanceof HuskEntity || entity instanceof ZombieVillagerEntity) {
            drops.add(stack(Items.ROTTEN_FLESH, rng.nextInt(3)));
         } else if (entity instanceof DrownedEntity) {
            drops.add(stack(Items.ROTTEN_FLESH, rng.nextInt(3)));
            if (rng.nextFloat() < 0.05F) {
               drops.add(stack(Items.COPPER_INGOT, 1));
            }

            if (rng.nextFloat() < 0.085F) {
               drops.add(stack(Items.TRIDENT, 1));
            }
         } else if (entity instanceof SkeletonEntity) {
            drops.add(stack(Items.BONE, rng.nextInt(3)));
            drops.add(stack(Items.ARROW, rng.nextInt(3)));
         } else if (entity instanceof StrayEntity) {
            drops.add(stack(Items.BONE, rng.nextInt(3)));
            drops.add(stack(Items.ARROW, rng.nextInt(3)));
            if (rng.nextBoolean()) {
               drops.add(new ItemStack(Items.TIPPED_ARROW, 1));
            }
         } else if (entity instanceof WitherSkeletonEntity) {
            drops.add(stack(Items.COAL, rng.nextInt(2)));
            drops.add(stack(Items.BONE, rng.nextInt(3)));
            if (rng.nextFloat() < 0.1F) {
               drops.add(stack(Items.WITHER_SKELETON_SKULL, 1));
            }
         } else if (entity instanceof CreeperEntity) {
            drops.add(stack(Items.GUNPOWDER, rng.nextInt(3)));
         } else if (entity instanceof SpiderEntity || entity instanceof CaveSpiderEntity) {
            drops.add(stack(Items.STRING, rng.nextInt(3)));
            if (rng.nextFloat() < 0.333F) {
               drops.add(stack(Items.SPIDER_EYE, 1));
            }
         } else if (entity instanceof EndermanEntity) {
            int count = 1 + rng.nextInt(2);
            drops.add(stack(Items.ENDER_PEARL, count));
         } else if (entity instanceof BlazeEntity) {
            if (rng.nextFloat() < 0.5F) {
               drops.add(stack(Items.BLAZE_ROD, 1));
            }
         } else if (entity instanceof GhastEntity) {
            drops.add(stack(Items.GUNPOWDER, rng.nextInt(3)));
            if (rng.nextBoolean()) {
               drops.add(stack(Items.GHAST_TEAR, 1));
            }
         } else if (entity instanceof SlimeEntity slime) {
            int size = slime.getSize();
            if (size <= 1) {
               drops.add(stack(Items.SLIME_BALL, rng.nextInt(size + 2)));
            }
         } else if (entity instanceof MagmaCubeEntity magma) {
            int size = magma.getSize();
            if (size <= 1) {
               drops.add(stack(Items.MAGMA_CREAM, rng.nextInt(size + 2)));
            }
         } else if (entity instanceof WitchEntity) {
            Item[] witchDrops = new Item[]{
               Items.GLOWSTONE_DUST, Items.SUGAR, Items.REDSTONE, Items.SPIDER_EYE, Items.GLASS_BOTTLE, Items.GUNPOWDER, Items.STICK
            };
            int numDrops = 1 + rng.nextInt(3);

            for (int i = 0; i < numDrops; i++) {
               drops.add(stack(witchDrops[rng.nextInt(witchDrops.length)], 1 + rng.nextInt(3)));
            }
         } else if (entity instanceof PillagerEntity) {
            drops.add(stack(Items.ARROW, rng.nextInt(3)));
         } else if (entity instanceof VindicatorEntity) {
            if (rng.nextFloat() < 0.5F) {
               drops.add(stack(Items.EMERALD, 1));
            }
         } else if (entity instanceof EvokerEntity) {
            drops.add(stack(Items.TOTEM_OF_UNDYING, 1));
            if (rng.nextFloat() < 0.5F) {
               drops.add(stack(Items.EMERALD, 1));
            }
         } else if (entity instanceof PhantomEntity) {
            if (rng.nextBoolean()) {
               drops.add(stack(Items.PHANTOM_MEMBRANE, 1));
            }
         } else if (entity instanceof GuardianEntity) {
            drops.add(stack(Items.PRISMARINE_SHARD, 1 + rng.nextInt(2)));
            if (rng.nextBoolean()) {
               drops.add(stack(Items.PRISMARINE_CRYSTALS, 1));
            }
         } else if (entity instanceof WitherEntity) {
            drops.add(stack(Items.NETHER_STAR, 1));
         } else if (!(entity instanceof EnderDragonEntity)) {
            if (entity instanceof ShulkerEntity) {
               if (rng.nextFloat() < 0.5F) {
                  drops.add(stack(Items.SHULKER_SHELL, 1));
               }
            } else if (entity instanceof RavagerEntity) {
               drops.add(stack(Items.SADDLE, 1));
            } else if (entity instanceof HoglinEntity) {
               drops.add(stack(Items.PORKCHOP, 2 + rng.nextInt(3)));
               if (rng.nextBoolean()) {
                  drops.add(stack(Items.LEATHER, 1));
               }
            } else if (entity instanceof PiglinBruteEntity) {
               drops.add(stack(Items.GOLD_INGOT, 1));
            } else if (!(entity instanceof SilverfishEntity)) {
               if (entity instanceof WardenEntity) {
                  drops.add(stack(Items.SCULK_CATALYST, 1));
               } else if (entity instanceof PigEntity) {
                  drops.add(stack(Items.PORKCHOP, 1 + rng.nextInt(3)));
               } else if (entity instanceof CowEntity || entity instanceof MooshroomEntity) {
                  drops.add(stack(Items.BEEF, 1 + rng.nextInt(3)));
                  drops.add(stack(Items.LEATHER, rng.nextInt(3)));
               } else if (entity instanceof SheepEntity sheep) {
                  drops.add(stack(Items.MUTTON, 1 + rng.nextInt(2)));

                  Item woolItem = switch (sheep.getColor()) {
                     case ORANGE -> Items.ORANGE_WOOL;
                     case MAGENTA -> Items.MAGENTA_WOOL;
                     case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
                     case YELLOW -> Items.YELLOW_WOOL;
                     case LIME -> Items.LIME_WOOL;
                     case PINK -> Items.PINK_WOOL;
                     case GRAY -> Items.GRAY_WOOL;
                     case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
                     case CYAN -> Items.CYAN_WOOL;
                     case PURPLE -> Items.PURPLE_WOOL;
                     case BLUE -> Items.BLUE_WOOL;
                     case BROWN -> Items.BROWN_WOOL;
                     case GREEN -> Items.GREEN_WOOL;
                     case RED -> Items.RED_WOOL;
                     case BLACK -> Items.BLACK_WOOL;
                     default -> Items.WHITE_WOOL;
                  };
                  drops.add(stack(woolItem, 1));
               } else if (entity instanceof ChickenEntity) {
                  drops.add(stack(Items.CHICKEN, 1));
                  drops.add(stack(Items.FEATHER, rng.nextInt(3)));
               } else if (entity instanceof SquidEntity || entity instanceof GlowSquidEntity) {
                  drops.add(stack(entity instanceof GlowSquidEntity ? Items.GLOW_INK_SAC : Items.INK_SAC, 1 + rng.nextInt(3)));
               } else if (entity instanceof RabbitEntity) {
                  drops.add(stack(Items.RABBIT, rng.nextInt(2)));
                  drops.add(stack(Items.RABBIT_HIDE, rng.nextInt(2)));
                  if (rng.nextFloat() < 0.1F) {
                     drops.add(stack(Items.RABBIT_FOOT, 1));
                  }
               } else if (entity instanceof TurtleEntity) {
                  drops.add(stack(Items.SEAGRASS, rng.nextInt(3)));
               } else if (!(entity instanceof FoxEntity)) {
                  if (entity instanceof StriderEntity) {
                     drops.add(stack(Items.STRING, 2 + rng.nextInt(4)));
                  } else if (entity instanceof CodEntity) {
                     drops.add(stack(Items.COD, 1 + rng.nextInt(3)));
                  } else if (entity instanceof SalmonEntity) {
                     drops.add(stack(Items.SALMON, 1 + rng.nextInt(3)));
                  } else if (entity instanceof PufferfishEntity) {
                     drops.add(stack(Items.PUFFERFISH, 1));
                  } else if (entity instanceof TropicalFishEntity) {
                     drops.add(stack(Items.TROPICAL_FISH, 1));
                  } else if (entity instanceof LlamaEntity || entity instanceof TraderLlamaEntity) {
                     drops.add(stack(Items.LEATHER, rng.nextInt(3)));
                  } else if (entity instanceof HorseEntity || entity instanceof DonkeyEntity || entity instanceof MuleEntity) {
                     drops.add(stack(Items.LEATHER, rng.nextInt(3)));
                  } else if (entity instanceof SnowGolemEntity) {
                     drops.add(stack(Items.SNOWBALL, 1 + rng.nextInt(4)));
                  } else if (entity instanceof IronGolemEntity) {
                     drops.add(stack(Items.IRON_INGOT, 3 + rng.nextInt(3)));
                     drops.add(stack(Items.POPPY, rng.nextInt(2)));
                  }
               }
            }
         }

         for (ItemStack dropx : drops) {
            if (dropx != null && !dropx.isEmpty()) {
               ItemEntity itemEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), dropx);
               itemEntity.setPickupDelay(10);
               world.spawnEntity(itemEntity);
            }
         }
      }
   }

   private static ItemStack stack(Item item, int count) {
      return count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
   }

   private static float[] applyAffinityModifiers(BattleState state, LivingEntity enemy, BattleSkill skill, float dmg) {
      if (enemy != null && !state.isVirtualEnemy) {
         Affinity[] affs = skill.getAffinities();
         if (affs.length == 0) {
            return new float[]{dmg, 1.0F};
         } else {
            Affinity weak = MobLevelManager.getMobWeakness(enemy);
            Affinity resist = MobLevelManager.getMobResistance(enemy);
            if (weak != null && resist != null) {
               boolean hitsWeak = false;
               boolean hitsResist = false;

               for (Affinity a : affs) {
                  if (a == weak) {
                     hitsWeak = true;
                  }

                  if (a == resist) {
                     hitsResist = true;
                  }
               }

               if (hitsWeak && hitsResist) {
                  return new float[]{dmg, 1.0F};
               } else if (hitsResist) {
                  return new float[]{dmg * 0.5F, 0.5F};
               } else if (hitsWeak) {
                  int enemyId = enemy.getId();
                  if (state.enemySturdy.getOrDefault(enemyId, 0) <= 0 && !state.enemyDownTurns.containsKey(enemyId)) {
                     state.enemyDownTurns.put(enemyId, 1);
                     state.newlyDownedThisTurn.add(enemyId);
                  }

                  return new float[]{dmg * 1.3F, 1.3F};
               } else {
                  return new float[]{dmg, 1.0F};
               }
            } else {
               return new float[]{dmg, 1.0F};
            }
         }
      } else {
         return new float[]{dmg, 1.0F};
      }
   }

   private static WeaponStats resolveActorWeaponStats(ServerPlayerEntity player, UUID actor, BattleState state, BattleData.PlayerProgress prog2) {
      boolean actorIsAnyPartyMember = actor.equals(BattleState.PARTY_MEMBER_UUID) || actor.equals(BattleState.PARTY_MEMBER_UUID_2);
      if (!actorIsAnyPartyMember) {
         BattleData.PlayerProgress actorProg = BattleData.get(player.getServer()).getOrCreate(actor);
         return WeaponStats.fromStack(actorProg.equippedWeapon);
      } else {
         boolean isSlot2 = actor.equals(BattleState.PARTY_MEMBER_UUID_2);
         RecruitData rd = RecruitData.get(player.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(state.playerUuid);
         int slotCode = isSlot2 ? pr.secondActiveRecruit : pr.activeRecruit;
         if (slotCode >= 1000) {
            int ci = slotCode - 1000;
            return ci < pr.customRecruits.size() ? WeaponStats.fromStack(pr.customRecruits.get(ci).state.equippedWeapon) : null;
         } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
            return WeaponStats.fromStack(pr.recruits[slotCode].equippedWeapon);
         } else if (!isSlot2) {
            Recruit activeRecruit = getActiveRecruitEnum(player.getServer(), state.playerUuid);
            if (activeRecruit != null) {
               RecruitData.RecruitState rs = rd.getOrCreate(state.playerUuid).get(activeRecruit);
               return WeaponStats.fromStack(rs.equippedWeapon);
            } else {
               return WeaponStats.fromStack(prog2.partyEquippedWeapon);
            }
         } else {
            return WeaponStats.fromStack(prog2.partyEquippedWeapon);
         }
      }
   }

   private static float[] applyBashAffinity(BattleState state, LivingEntity enemy, float dmg, Affinity[] weaponElements) {
      if (enemy != null && !state.isVirtualEnemy) {
         Affinity weak = MobLevelManager.getMobWeakness(enemy);
         Affinity resist = MobLevelManager.getMobResistance(enemy);
         if (weak != null && resist != null) {
            Affinity[] types = weaponElements != null && weaponElements.length > 0 ? weaponElements : new Affinity[]{Affinity.SLASH};
            boolean hitsWeak = false;
            boolean hitsResist = false;

            for (Affinity t : types) {
               if (weak == t) {
                  hitsWeak = true;
               }

               if (resist == t) {
                  hitsResist = true;
               }
            }

            if (hitsWeak && hitsResist) {
               return new float[]{dmg, 1.0F};
            } else if (hitsResist) {
               return new float[]{dmg * 0.5F, 0.5F};
            } else if (hitsWeak) {
               if (state.enemySturdy.getOrDefault(enemy.getId(), 0) <= 0 && !state.enemyDownTurns.containsKey(enemy.getId())) {
                  state.enemyDownTurns.put(enemy.getId(), 1);
                  state.newlyDownedThisTurn.add(enemy.getId());
               }

               return new float[]{dmg * 1.3F, 1.3F};
            } else {
               return new float[]{dmg, 1.0F};
            }
         } else {
            return new float[]{dmg, 1.0F};
         }
      } else {
         return new float[]{dmg, 1.0F};
      }
   }

   static String getPlayerDisplayName(ServerPlayerEntity p) {
      if (p == null) {
         return "?";
      } else {
         BattleData bd = BattleData.get(p.getServer());
         BattleData.PlayerProgress prog = bd.getOrCreate(p.getUuid());
         return prog.vesselName != null && !prog.vesselName.isBlank() ? prog.vesselName : p.getName().getString();
      }
   }

   static boolean queuedTurns(BattleState s) {
      return s != null && !s.isPvp && !s.staged && s.battleStyle != BattleStyle.LIVE_COMBAT;
   }

   static String plainName(String name) {
      if (name == null) {
         return "?";
      } else {
         String s = name.replaceAll("\u00a7.", "").replaceAll("\\[[^\\]]*\\]\\s*", "").replaceAll("(?i)\\bLv\\.?\\s*\\d+\\s*", "").trim();
         return s.isEmpty() ? name.replaceAll("\u00a7.", "").trim() : s;
      }
   }

   static String noDashes(String s) {
      return s == null ? "" : s.replaceAll("\\s*[\u2014\u2013]\\s*", ", ").replaceAll("\\s+-+\\s+", ", ").replaceAll(" {2,}", " ");
   }

   static String encounterLine(BattleState state) {
      List names = new ArrayList<>();
      if (state.isVirtualEnemy) {
         names.add(plainName(state.entityName));
      } else {
         names.add(plainName(state.enemyNames.getOrDefault(state.primaryEnemyId, state.entityName)));

         for (int id : state.extraEnemyIds) {
            names.add(plainName(state.enemyNames.getOrDefault(id, "?")));
         }
      }

      if (state.waitWord == null) {
         state.waitWord = WAIT_WORDS[RNG.nextInt(WAIT_WORDS.length)];
      }

      String who = names.size() == 1 ? names.get(0) : String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
      return noDashes(who + (names.size() == 1 ? " waits " : " wait ") + state.waitWord + ".");
   }

   private static boolean stillActive(BattleState state) {
      for (UUID p : state.participantOrder) {
         if (activeBattles.get(p) == state) {
            return true;
         }
      }

      return activeBattles.get(state.playerUuid) == state;
   }

   private static ServerPlayerEntity anyOnlineParticipant(BattleState state, PlayerManager pm) {
      ServerPlayerEntity owner = pm.getPlayer(state.playerUuid);
      if (owner != null && activeBattles.get(state.playerUuid) == state) {
         return owner;
      } else {
         for (UUID p : state.participantOrder) {
            if (!isRecruitSlot(p)) {
               ServerPlayerEntity pp = pm.getPlayer(p);
               if (pp != null && activeBattles.get(p) == state) {
                  return pp;
               }
            }
         }

         return null;
      }
   }

   private static int currentIndexOfEnemy(BattleState state, int enemyId) {
      if (enemyId >= 0 && enemyId != state.primaryEnemyId) {
         int i = state.extraEnemyIds.indexOf(enemyId);
         return i >= 0 ? i + 1 : 0;
      } else {
         return 0;
      }
   }

   private static String vesselName(BattleState state, UUID actor, ServerPlayerEntity owner) {
      String n = state.participantNames.get(actor);
      if (n == null || n.isBlank()) {
         n = isRecruitSlot(actor) ? "Ally" : getPlayerDisplayName(owner);
      }

      return plainName(n);
   }

   private static void broadcastReadyPose(BattleState state, PlayerManager pm, UUID actor, int type) {
      broadcastReadyPose(state, pm, actor, type, "");
   }

   private static void broadcastReadyPose(BattleState state, PlayerManager pm, UUID actor, int type, String itemId) {
      int anim = switch (type) {
         case 0 -> 8;
         default -> 9;
         case 2 -> 10;
         case 5, 7 -> 11;
      };
      int idx = state.participantOrder.indexOf(actor);
      BattleNetworking.broadcastVesselAnim(state, pm, idx, anim, null, actor, itemId);
   }

   private static Object skillReservation(ServerPlayerEntity player, UUID actor, BattleState state, int ordinal, int level, PlayerClass cls) {
      BattleData.PlayerProgress actorProg = BattleData.get(player.getServer()).getOrCreate(actor);
      Set actorSkills = resolveActorUnlockedSkills(player, state, actor, actorProg);
      BattleSkill skill = decodeActorSkill(player, actor, state, ordinal, cls, actorProg, actorSkills);
      if (skill == null) {
         return "You can't use that.";
      } else if (cls != null && skill.getLevelRequired() > level) {
         return "You can't use that yet.";
      } else {
         int cost = skill == LightnerSkill.SNOWGRAVE ? LightnerSkill.snowgraveCost(maxTpFor(state, actor)) : skill.getTpCost();
         if (resolveActorGimmick(player, state, actor, actorProg) == OffhandGimmick.THORN_RING) {
            cost = (int)Math.ceil((double)((float)cost * 0.5F));
         }

         boolean resonatorHpCost = cls == PlayerClass.SOUL_MAGE
            && state.offhandGimmicks.getOrDefault(actor, OffhandGimmick.NONE) == OffhandGimmick.SOUL_RESONATOR;
         if (resonatorHpCost) {
            return player.getHealth() <= (float)cost ? "Not enough HP." : 0;
         } else {
            return state.getTp(actor) < (float)cost ? "Not enough TP." : cost;
         }
      }
   }

   private static void rejectSelection(ServerPlayerEntity player, BattleState state, UUID actor, String why) {
      lastActionQueued = true;
      if (isRecruitSlot(actor)) {
         BattleNetworking.sendPartyMemberTurn(player, state, actor);
      } else {
         BattleNetworking.sendTurnNotice(player, state);
      }

      BattleNetworking.sendBattleLine(player, 0, noDashes(why), 2);
   }

   private static void queueSelection(
      ServerPlayerEntity player, BattleState state, UUID actor, int type, int param, int targetIndex, int level, PlayerClass cls, ServerWorld world
   ) {
      PlayerManager pm = player.getServer().getPlayerManager();
      int reserved = 0;
      if (type == 1) {
         Object r = skillReservation(player, actor, state, param, level, cls);
         if (r instanceof String why) {
            rejectSelection(player, state, actor, why);
            return;
         }

         reserved = (Integer)r;
      } else if (type != 2) {
         if (type == 5 && state.magicant) {
            rejectSelection(player, state, actor, "You can't show mercy to yourself.");
            return;
         }
      } else {
         ItemStack stack = param >= 0 && param < player.getInventory().size() ? player.getInventory().getStack(param) : ItemStack.EMPTY;
         int alreadyPicked = 0;

         for (BattleState.QueuedAction q : state.actionQueue) {
            if (q.type == 2 && q.param == param && q.owner.equals(player.getUuid())) {
               alreadyPicked++;
            }
         }

         if (stack.isEmpty() || stack.getCount() <= alreadyPicked) {
            rejectSelection(player, state, actor, "There's nothing there.");
            return;
         }

         if (stack.get(DataComponentTypes.FOOD) == null) {
            rejectSelection(player, state, actor, "That's not food.");
            return;
         }
      }

      int enemyId = targetIndex < 10 ? getEnemyIdByIndex(state, targetIndex) : -1;
      if (reserved > 0) {
         state.sharedTp = Math.max(0.0F, state.sharedTp - (float)reserved);
      }

      state.actionQueue.add(new BattleState.QueuedAction(actor, player.getUuid(), type, param, targetIndex, enemyId, reserved, level, cls));
      lastActionQueued = true;
      String heldItem = "";
      if (type == 2) {
         heldItem = Registries.ITEM.getId(player.getInventory().getStack(param).getItem()).toString();
      }

      broadcastReadyPose(state, pm, actor, type, heldItem);
      if (reserved > 0) {
         BattleNetworking.broadcastSharedTp(state, pm);
      }

      afterParticipantAction(player, state, "", false, world);
   }

   private static void beginEnemyPhase(ServerPlayerEntity player, BattleState state, PlayerManager pm, ServerWorld world) {
      if (state.isPvp) {
         runPvpDodgePhase(player, state, pm);
      } else if (state.allEnemiesDown()) {
         triggerAllOutAttack(state, player.getServer());
      } else {
         LivingEntity currentEnemy = findEnemyByIndex(state, world, 0);
         if (currentEnemy != null || state.isVirtualEnemy) {
            runEnemyTurn(player, currentEnemy, state, pm);
         }
      }
   }

   private static void startResolution(BattleState state, MinecraftServer server) {
      state.resolving = true;
      state.pendingLines.clear();
      List sorted = new ArrayList<>(state.actionQueue);
      sorted.sort(Comparator.comparingInt(BattleState.QueuedAction::stage).thenComparingInt(q -> {
         int i = state.participantOrder.indexOf(q.actor);
         return i < 0 ? Integer.MAX_VALUE : i;
      }));
      state.actionQueue.clear();
      state.actionQueue.addAll(sorted);
      nextResolution(state, server);
   }

   private static void nextResolution(BattleState state, MinecraftServer server) {
      if (stillActive(state)) {
         if (landPendingHits(state, server, true)) {
            PlayerManager pm = server.getPlayerManager();
            if (!state.pendingLines.isEmpty()) {
               showLine(state, pm, state.pendingLines.poll());
            } else {
               while (!state.actionQueue.isEmpty() && state.actionQueue.get(0).type != 0) {
                  BattleState.QueuedAction q = state.actionQueue.remove(0);

                  List lines;
                  try {
                     lines = executeQueued(state, server, q);
                  } finally {
                     SkillBonus.magicFlat = 0.0F;
                  }

                  if (!stillActive(state)) {
                     return;
                  }

                  if (lines != null && !lines.isEmpty()) {
                     for (int i = 1; i < lines.size(); i++) {
                        state.pendingLines.add(lines.get(i));
                     }

                     showLine(state, pm, lines.get(0));
                     return;
                  }
               }

               startBashPhase(state, server);
            }
         }
      }
   }

   private static void showLine(BattleState state, PlayerManager pm, String text) {
      state.logSeq++;
      state.awaitingLogAck = true;
      state.logShownTick = (long)pm.getServer().getTicks();
      BattleNetworking.broadcastBattleLine(state, pm, state.logSeq, noDashes(text), 1);
   }

   public static void onBattleLineInput(ServerPlayerEntity player, int kind, int seq) {
      BattleState state = activeBattles.get(player.getUuid());
      if (state != null && state.resolving && seq == state.logSeq && state.awaitingLogAck) {
         PlayerManager pm = player.getServer().getPlayerManager();
         if (kind == 0) {
            BattleNetworking.broadcastBattleLineSkip(state, pm, seq);
         } else {
            state.awaitingLogAck = false;
            nextResolution(state, player.getServer());
         }
      }
   }

   private static List<String> executeQueued(BattleState state, MinecraftServer server, BattleState.QueuedAction q) {
      PlayerManager pm = server.getPlayerManager();
      ServerPlayerEntity player = pm.getPlayer(q.owner);
      if (player != null && activeBattles.get(q.owner) == state && !isDowned(state, q.actor, pm)) {
         ServerWorld world = player.getServerWorld();
         int target = q.targetIndex < 10 && q.targetEnemyId >= 0 ? currentIndexOfEnemy(state, q.targetEnemyId) : q.targetIndex;
         int actorIdx = state.participantOrder.indexOf(q.actor);
         String name = vesselName(state, q.actor, player);
         List lines = new ArrayList<>();
         switch (q.type) {
            case 1:
               BattleData.PlayerProgress actorProg = BattleData.get(server).getOrCreate(q.actor);
               BattleSkill skill = decodeActorSkill(
                  player, q.actor, state, q.param, q.cls, actorProg, resolveActorUnlockedSkills(player, state, q.actor, actorProg)
               );
               if (skill == null) {
                  state.sharedTp = Math.min(state.sharedTpMax, state.sharedTp + (float)q.reservedTp);
                  return null;
               }

               int playParam = q.cls == null ? encodeSkillParam(skill, q.param) : q.param;
               BattleNetworking.broadcastSkillPlayToAll(state, pm, q.actor, playParam, q.cls, target);
               BattleNetworking.broadcastVesselAnim(state, pm, actorIdx, 1, null, q.actor, "");
               lines.add(name + " used " + plainName(skill.getDisplayName()) + "!");
               if (skill.getType() == SkillType.SPY) {
                  if (q.reservedTp > 0) {
                     state.sharedTp = state.sharedTp + (float)q.reservedTp;
                  }

                  String found = doSkill(player, q.actor, state, q.param, q.level, q.cls, target, world);
                  if (found != null && !found.isBlank()) {
                     lines.add(found);
                  }
               } else {
                  scheduleHit(state, server, SkillTiming.at(skill, 0.8F), () -> {
                     ServerPlayerEntity p = pm.getPlayer(q.owner);
                     if (p != null && activeBattles.get(q.owner) == state) {
                        if (q.reservedTp > 0) {
                           state.sharedTp = state.sharedTp + (float)q.reservedTp;
                        }

                        int t = q.targetIndex < 10 && q.targetEnemyId >= 0 ? currentIndexOfEnemy(state, q.targetEnemyId) : q.targetIndex;
                        ServerWorld w = p.getServerWorld();

                        try {
                           doSkill(p, q.actor, state, q.param, q.level, q.cls, t, w);
                        } finally {
                           SkillBonus.magicFlat = 0.0F;
                        }

                        if (!stillActive(state)) {
                           return false;
                        } else if (postActionCleanup(p, state, "", w)) {
                           return false;
                        } else {
                           BattleNetworking.broadcastStateUpdate(state, pm, "");
                           return true;
                        }
                     } else {
                        return true;
                     }
                  });
               }
               break;
            case 2:
               ItemStack stack = q.param >= 0 && q.param < player.getInventory().size() ? player.getInventory().getStack(q.param) : ItemStack.EMPTY;
               if (stack.isEmpty() || stack.get(DataComponentTypes.FOOD) == null) {
                  return null;
               }

               String item = plainName(stack.getName().getString());
               String itemId = Registries.ITEM.getId(stack.getItem()).toString();
               UUID onWho = null;
               if (q.targetIndex >= 10) {
                  List team = state.teammatesOf(player.getUuid());
                  int pidx = q.targetIndex - 10;
                  if (pidx >= 0 && pidx < team.size()) {
                     onWho = team.get(pidx);
                  }
               }

               BattleNetworking.broadcastVesselAnim(state, pm, actorIdx, 2, null, q.actor, itemId);
               doGoods(player, state, q.param, q.targetIndex);
               lines.add(
                  onWho != null && !onWho.equals(q.actor)
                     ? name + " used " + item + " on " + vesselName(state, onWho, player) + "!"
                     : name + " used " + item + "!"
               );
               break;
            case 3:
            case 4:
            case 6:
            default:
               return null;
            case 5:
               int spareId = getEnemyIdByIndex(state, target);
               String spareName = plainName(state.enemyNames.getOrDefault(spareId, state.entityName));
               BattleNetworking.broadcastVesselAnim(state, pm, actorIdx, 5, null, q.actor, "");
               if (!state.isMercyEligibleById(spareId)) {
                  lines.add(name + " spared " + spareName + "! But they weren't ready to be spared...");
               } else {
                  lines.add(name + " spared " + spareName + "!");
                  boolean removed = spareOneEnemy(state, world, spareId, target, server);
                  boolean allGone = state.isVirtualEnemy
                     ? state.virtualHp <= 0.0F
                     : removed && state.extraEnemyIds.isEmpty() && findEnemyByIndex(state, world, 0) == null;
                  if (allGone) {
                     BattleNetworking.broadcastStateUpdate(state, pm, "");
                     endBattle(state, server, "mercy");
                     return null;
                  }
               }
               break;
            case 7:
               ActDefs.ActEntry act = actEntryFor(state, player, q.param, target);
               BattleNetworking.broadcastVesselAnim(state, pm, actorIdx, 7, null, q.actor, "");
               String actName = act != null ? plainName(act.logName()) : "Act";
               lines.add(name + " used " + actName + "!");
               if (act == ActDefs.CHECK || act != null && "__CHECK__".equals(act.logText())) {
                  lines.add(checkLine(state, player, world, getEnemyIdByIndex(state, target)));
               }

               runAct(player, state, q.param, target, world, true);
         }

         if (!stillActive(state)) {
            return null;
         } else if (postActionCleanup(player, state, "", world)) {
            return null;
         } else {
            BattleNetworking.broadcastStateUpdate(state, pm, "");
            return lines;
         }
      } else {
         if (q.reservedTp > 0) {
            state.sharedTp = Math.min(state.sharedTpMax, state.sharedTp + (float)q.reservedTp);
         }

         return null;
      }
   }

   static float maxTpFor(BattleState state, UUID actor) {
      return state.isPvp ? state.participantTpMax.getOrDefault(actor, 100.0F) : state.sharedTpMax;
   }

   private static int encodeSkillParam(BattleSkill s, int fallback) {
      if (s instanceof OriSkill o) {
         return 5000 + o.ordinal();
      } else if (s instanceof SoulMageSkill m) {
         return 4000 + m.ordinal();
      } else if (s instanceof LightnerSkill l) {
         return 3000 + l.ordinal();
      } else if (s instanceof MonsterSkill m) {
         return 2000 + m.ordinal();
      } else if (s instanceof HumanMageSkill h) {
         return 1000 + h.ordinal();
      } else {
         return s instanceof PsiSkill p ? p.ordinal() : fallback;
      }
   }

   private static String checkLine(BattleState state, ServerPlayerEntity player, ServerWorld world, int enemyId) {
      LivingEntity e = world.getEntityById(enemyId) instanceof LivingEntity le ? le : null;
      BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(state.playerUuid);
      float atk = e != null ? getEnemyDamage(e, prog.level) * 1.5F : 0.0F;
      float def = e != null ? readEnemyArmor(state, e) : 0.0F;
      String nm = plainName(state.enemyNames.getOrDefault(enemyId, state.entityName));
      String line = String.format("%s, ATK %.0f, DEF %.0f.", nm, atk, def);
      if (enemyId == state.primaryEnemyId && state.entityPassive != null && !state.entityPassive.isEmpty()) {
         line = line + " " + state.entityPassive;
      }

      return line;
   }

   private static void startBashPhase(BattleState state, MinecraftServer server) {
      PlayerManager pm = server.getPlayerManager();
      state.bashPhase.clear();
      state.awaitingLogAck = false;

      for (BattleState.QueuedAction q : state.actionQueue) {
         if (q.type == 0) {
            ServerPlayerEntity owner = pm.getPlayer(q.owner);
            if (owner != null && activeBattles.get(q.owner) == state && !isDowned(state, q.actor, pm)) {
               state.bashPhase.add(new BattleState.BashEntry(q, rollBashCrit(server, state, q.actor)));
            }
         }
      }

      state.actionQueue.clear();
      if (state.bashPhase.isEmpty()) {
         finishResolution(state, server);
      } else {
         List shuffled = new ArrayList<>(state.bashPhase);
         Collections.shuffle(shuffled, RNG);
         Map nextDelay = new HashMap<>();

         for (BattleState.BashEntry e : shuffled) {
            if (!e.crit) {
               int d = nextDelay.getOrDefault(e.pick.owner, 0);
               e.delay = d;
               nextDelay.put(e.pick.owner, d + (RNG.nextFloat() < 0.2F ? 0 : 3 + RNG.nextInt(8)));
            }
         }

         state.bashPhaseActive = true;
         state.bashStartTick = (long)server.getTicks();
         BattleNetworking.broadcastMultiBash(state, pm);

         for (BattleState.BashEntry ex : new ArrayList<>(state.bashPhase)) {
            if (ex.crit && !landBash(state, server, ex)) {
               return;
            }
         }
      }
   }

   private static boolean allBashesIn(BattleState state) {
      for (BattleState.BashEntry e : state.bashPhase) {
         if (!e.done) {
            return false;
         }
      }

      return true;
   }

   public static void onMultiBashResult(ServerPlayerEntity player, List<UUID> actors, List<float[]> results) {
      BattleState state = activeBattles.get(player.getUuid());
      if (state != null && state.bashPhaseActive) {
         PlayerManager pm = player.getServer().getPlayerManager();

         for (int i = 0; i < actors.size(); i++) {
            UUID a = actors.get(i);
            float[] r = results.get(i);

            for (BattleState.BashEntry e : state.bashPhase) {
               if (!e.done && e.pick.actor.equals(a) && e.pick.owner.equals(player.getUuid())) {
                  e.mult = Math.max(0.0F, Math.min(1.6F, r[0]));
                  e.tpTier = Math.max(0, Math.min(3, (int)r[1]));
                  e.done = true;
                  BattleNetworking.broadcastBashBar(state, pm, a, r[2], e.tpTier == 3);
                  if (!landBash(state, player.getServer(), e)) {
                     return;
                  }
                  break;
               }
            }
         }
      }
   }

   private static boolean landBash(BattleState state, MinecraftServer server, BattleState.BashEntry e) {
      if (e.landed) {
         return true;
      } else {
         e.landed = true;
         PlayerManager pm = server.getPlayerManager();
         BattleState.QueuedAction q = e.pick;
         if (!e.crit && e.mult <= 0.0F) {
            return true;
         } else {
            ServerPlayerEntity owner = pm.getPlayer(q.owner);
            if (owner != null && activeBattles.get(q.owner) == state && !isDowned(state, q.actor, pm)) {
               ServerWorld world = owner.getServerWorld();
               int target = currentIndexOfEnemy(state, q.targetEnemyId);
               BattleNetworking.broadcastVesselAnim(state, pm, state.participantOrder.indexOf(q.actor), 0, null, q.actor, "");
               BattleNetworking.broadcastBashPlayToAll(state, pm, q.actor, target);
               scheduleHit(state, server, 10, () -> {
                  ServerPlayerEntity o = pm.getPlayer(q.owner);
                  if (o != null && activeBattles.get(q.owner) == state) {
                     ServerWorld w = o.getServerWorld();
                     int t = currentIndexOfEnemy(state, q.targetEnemyId);
                     doBash(o, q.actor, state, q.level, t, w, e.crit, e.crit ? 1.0F : e.mult, e.crit ? 0 : e.tpTier);
                     if (!stillActive(state)) {
                        return false;
                     } else if (postActionCleanup(o, state, "", w)) {
                        return false;
                     } else {
                        BattleNetworking.broadcastStateUpdate(state, pm, "");
                        return true;
                     }
                  } else {
                     return true;
                  }
               });
               return true;
            } else {
               return true;
            }
         }
      }
   }

   private static void scheduleHit(BattleState state, MinecraftServer server, int ticks, BooleanSupplier hit) {
      state.pendingHits.add(new BattleState.PendingHit((long)(server.getTicks() + Math.max(1, ticks)), hit));
   }

   private static boolean landPendingHits(BattleState state, MinecraftServer server, boolean force) {
      long now = (long)server.getTicks();

      while (!state.pendingHits.isEmpty()) {
         BattleState.PendingHit next = null;

         for (BattleState.PendingHit h : state.pendingHits) {
            if ((force || h.due <= now) && (next == null || h.due < next.due)) {
               next = h;
            }
         }

         if (next == null) {
            return true;
         }

         state.pendingHits.remove(next);

         boolean going;
         try {
            going = next.hit.getAsBoolean();
         } catch (Exception var8) {
            going = stillActive(state);
         }

         if (!going || !stillActive(state)) {
            state.pendingHits.clear();
            return false;
         }
      }

      return true;
   }

   private static void endBashPhase(BattleState state, MinecraftServer server) {
      if (state.bashPhaseActive) {
         state.bashPhaseActive = false;
         state.bashPhase.clear();
         finishResolution(state, server);
      }
   }

   private static void finishResolution(BattleState state, MinecraftServer server) {
      state.resolving = false;
      state.waitWord = null;
      state.bashPhaseActive = false;
      state.bashPhase.clear();
      state.actionQueue.clear();
      state.pendingLines.clear();
      state.awaitingLogAck = false;
      if (stillActive(state)) {
         PlayerManager pm = server.getPlayerManager();
         ServerPlayerEntity player = anyOnlineParticipant(state, pm);
         if (player != null) {
            BattleNetworking.broadcastBattleLine(state, pm, 0, encounterLine(state), 0);
            if (state.pendingBonusTurnTarget != null) {
               UUID t = state.pendingBonusTurnTarget;
               state.pendingBonusTurnTarget = null;
               int idx = state.participantOrder.indexOf(t);
               ServerPlayerEntity picker = isRecruitSlot(t) ? pm.getPlayer(state.playerUuid) : pm.getPlayer(t);
               if (idx >= 0 && picker != null && !isDowned(state, t, pm)) {
                  state.bonusSelection = true;
                  state.currentParticipantIndex = idx;
                  BattleNetworking.broadcastStateUpdate(state, pm, "");
                  if (isRecruitSlot(t)) {
                     BattleNetworking.sendPartyMemberTurn(picker, state, t);
                  } else {
                     BattleNetworking.sendTurnNotice(picker, state);
                  }

                  return;
               }
            }

            state.currentParticipantIndex = 0;
            BattleNetworking.broadcastStateUpdate(state, pm, "");
            beginEnemyPhase(player, state, pm, player.getServerWorld());
         }
      }
   }

   public static void tickQueuedTurns(MinecraftServer server) {
      tickFirstStrikes(server);
      Set seen = Collections.newSetFromMap(new IdentityHashMap<>());
      long now = (long)server.getTicks();

      for (BattleState st : new ArrayList<>(activeBattles.values())) {
         if (st.resolving && seen.add(st) && landPendingHits(st, server, false)) {
            if (st.bashPhaseActive) {
               if (now - st.bashStartTick > 240L) {
                  for (BattleState.BashEntry e : new ArrayList<>(st.bashPhase)) {
                     if (!e.done) {
                        e.done = true;
                        e.mult = 0.3F;
                        e.tpTier = 1;
                        BattleNetworking.broadcastBashBar(st, server.getPlayerManager(), e.pick.actor, -1.0F, false);
                        if (!landBash(st, server, e)) {
                           break;
                        }
                     }
                  }
               }

               if (st.bashPhaseActive && allBashesIn(st) && st.pendingHits.isEmpty()) {
                  boolean allSwung = true;

                  for (BattleState.BashEntry ex : st.bashPhase) {
                     if (!ex.landed) {
                        allSwung = false;
                        break;
                     }
                  }

                  if (allSwung) {
                     endBashPhase(st, server);
                  }
               }
            } else if (st.awaitingLogAck && now - st.logShownTick > 1200L) {
               st.awaitingLogAck = false;
               nextResolution(st, server);
            }
         }
      }
   }

   static {
      new ConcurrentHashMap();
      new ConcurrentHashMap();
   }
}
