package com.pixelcite.turnbattle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BattleState {
   public boolean isPvp = false;
   public boolean darknessConstricting = false;
   public boolean lastPvpHitFullyShielded = false;
   public int lastActionHitCount = 1;
   public BattleStyle battleStyle = BattleStyle.BULLET_HELL;
   public boolean staged = false;
   public boolean magicant = false;
   public boolean depthsRun = false;
   public boolean forceOverhaul = false;
   public Vec3d stageOrigin;
   public Vec3d stageForward;
   public RegistryKey<World> stageWorld;
   public final Map<UUID, Integer> recruitEntityIds = new HashMap<>();
   public final Map<Integer, Float> enemyAtkBoostMult = new LinkedHashMap<>();
   public final Map<Integer, Integer> enemyAtkBoostTurns = new LinkedHashMap<>();
   public AtomicInteger directTurnPending = null;
   public String directAttackLogPrefix = null;
   public Map<UUID, Float> directAttackDmgMap = null;
   public final Map<UUID, Integer> playerFreezeMap = new LinkedHashMap<>();
   public final Map<UUID, Integer> playerSleepMap = new LinkedHashMap<>();
   public final Map<UUID, Integer> playerConfusionMap = new LinkedHashMap<>();
   public String depthsFountainKey = null;
   public String questlineBattleId = null;
   public UUID enemyPlayerUuid = null;
   public boolean isTeamPvp = false;
   public final Map<UUID, Integer> participantTeam = new LinkedHashMap<>();
   public final Map<UUID, UUID> pvpChosenTarget = new LinkedHashMap<>();
   public boolean pendingSelfBonusTurn = false;
   public final UUID playerUuid;
   public final UUID entityUuid;
   public final int entityNetworkId;
   public final String entityName;
   public final float entityMaxHp;
   public String entityTypeKey = "";
   public float entityCurrentHp;
   public String entityPassive = "";
   public float entityBulletSpeedMult = 1.0F;
   public Affinity entityAffinity = null;
   public Element battleElement = Element.NONE;
   public int primaryEnemyId;
   public final List<Integer> extraEnemyIds = new ArrayList<>();
   public final Map<Integer, Float> enemyHpMap = new LinkedHashMap<>();
   public final Map<Integer, Float> enemyMaxHpMap = new LinkedHashMap<>();
   public final Map<Integer, String> enemyNames = new LinkedHashMap<>();
   public final Map<Integer, String> enemyTypeKeys = new LinkedHashMap<>();
   public static final UUID PARTY_MEMBER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
   public static final UUID PARTY_MEMBER_UUID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
   public final List<UUID> participantOrder = new ArrayList<>();
   public final Map<UUID, Float> participantTp = new LinkedHashMap<>();
   public final Map<UUID, Float> participantTpMax = new LinkedHashMap<>();
   public final Map<UUID, Float> participantHp = new LinkedHashMap<>();
   public final Map<UUID, Float> participantMaxHp = new LinkedHashMap<>();
   public final Map<UUID, Float> targetParticipantHp = new LinkedHashMap<>();
   public final Map<UUID, String> participantNames = new LinkedHashMap<>();
   public final Map<UUID, PlayerClass> participantClass = new LinkedHashMap<>();
   public final Map<UUID, OffhandGimmick> offhandGimmicks = new LinkedHashMap<>();
   public final Map<UUID, Integer> shieldParryTick = new LinkedHashMap<>();
   public final Map<UUID, Integer> thornDrainTicks = new LinkedHashMap<>();
   public int battleTick = 0;
   public final Map<UUID, Boolean> defending = new LinkedHashMap<>();
   public final Map<UUID, Float> shieldReduction = new LinkedHashMap<>();
   public final Map<UUID, Integer> shieldTurnsLeft = new LinkedHashMap<>();
   public final Map<UUID, Integer> attackBoostTurns = new LinkedHashMap<>();
   public final Map<UUID, Float> attackBoostMult = new LinkedHashMap<>();
   public final Set<UUID> playerBurned = new LinkedHashSet<>();
   public final Map<Integer, Long> iceHitAt = new HashMap<>();
   public final Set<Integer> fateSent = new HashSet<>();
   public final Map<UUID, Float> pvpShield = new LinkedHashMap<>();
   public final Map<UUID, Float> pvpShieldMax = new LinkedHashMap<>();
   public final Map<UUID, Boolean> pvpShieldBroken = new LinkedHashMap<>();
   public final Map<UUID, Integer> pvpHealsUsed = new LinkedHashMap<>();
   public final Map<UUID, Integer> pvpItemsUsed = new LinkedHashMap<>();
   public final Set<UUID> pvpAttackedThisRound = new HashSet<>();
   public final Map<UUID, ItemStack> originalHeldItems = new LinkedHashMap<>();
   public final Map<UUID, Integer> originalHeldSlots = new LinkedHashMap<>();
   public int currentParticipantIndex = 0;
   public final Map<UUID, Boolean> pendingBashCrit = new HashMap<>();
   public final Map<UUID, Integer> pendingBashTarget = new HashMap<>();
   public int enemySleepTurns = 0;
   public boolean enemyConfused = false;
   public int enemyConfusedLeft = 0;
   public boolean enemySpareable = false;
   public final Map<Integer, Integer> enemySleepTurnsMap = new LinkedHashMap<>();
   public final Map<Integer, Integer> enemyConfusedLeftMap = new LinkedHashMap<>();
   public final Set<Integer> enemySpareableSet = new LinkedHashSet<>();
   public int pendingPacifySpareId = -1;
   public final Set<UUID> pendingDeath = new HashSet<>();
   public float spareHpThreshold = 0.0F;
   public final Map<Integer, Float> enemySpareThresholds = new HashMap<>();
   public int enemyFreezeTurns = 0;
   public int enemyBurnTurns = 0;
   public final Map<Integer, Integer> enemyFreezeTurnsMap = new LinkedHashMap<>();
   public final Map<Integer, Integer> enemyBurnTurnsMap = new LinkedHashMap<>();
   public boolean batonPassPending = false;
   public final Set<UUID> batonPassUsed = new HashSet<>();
   public final Set<UUID> recruitDeterminationUsed = new HashSet<>();
   public int batonPassReturnIndex = -1;
   public UUID pendingBonusTurnTarget = null;
   public final Map<Integer, Integer> enemyDownTurns = new LinkedHashMap<>();
   public final Set<Integer> newlyDownedThisTurn = new HashSet<>();
   public final Map<Integer, Integer> enemySturdy = new LinkedHashMap<>();
   public boolean sleepImmune = false;
   public boolean confuseImmune = false;
   public boolean weakenImmune = false;
   public boolean freezeImmune = false;
   public boolean burnImmune = false;
   public final Set<Integer> sleepImmuneSet = new LinkedHashSet<>();
   public final Set<Integer> confuseImmuneSet = new LinkedHashSet<>();
   public final Set<Integer> weakenImmuneSet = new LinkedHashSet<>();
   public final Set<Integer> freezeImmuneSet = new LinkedHashSet<>();
   public final Set<Integer> burnImmuneSet = new LinkedHashSet<>();
   public float enemyAttackReduce = 0.0F;
   public int enemyWeakenTurns = 0;
   public float enemyArmorDebuff = 0.0F;
   public final Map<Integer, Float> enemyAttackReduceMap = new LinkedHashMap<>();
   public final Map<Integer, Integer> enemyWeakenTurnsMap = new LinkedHashMap<>();
   public final Map<Integer, Float> enemyArmorDebuffMap = new LinkedHashMap<>();
   public float rogueliteArmorScale = 1.0F;
   public BattleState.Phase phase = BattleState.Phase.PLAYER_TURN;
   public final Deque<String> logHistory = new ArrayDeque<>();
   public final List<BattleState.QueuedAction> actionQueue = new ArrayList<>();
   public boolean resolving = false;
   public boolean bonusSelection = false;
   public int logSeq = 0;
   public boolean awaitingLogAck = false;
   public long logShownTick = 0L;
   public final ArrayDeque<String> pendingLines = new ArrayDeque<>();
   public final List<BattleState.BashEntry> bashPhase = new ArrayList<>();
   public final List<BattleState.PendingHit> pendingHits = new ArrayList<>();
   public boolean bashPhaseActive = false;
   public int firstStrike = 0;
   public long firstStrikeDue = 0L;
   public int firstStrikeEnemyId = -1;
   public int levelAtStart = 1;
   public String waitWord = null;
   public long bashStartTick = 0L;
   public BlockPos bossBeaconPos = null;
   public int bossHighestLevel = 0;
   public int bossDifficultyOrdinal = -1;
   public int bossLivesRemaining = 0;
   public int bossMaxLives = 0;
   public int bossEnemyId = -1;
   public float currentBulletBaseDmg = 0.0F;
   public final Map<UUID, Boolean> bulletTurnDefendSnap = new HashMap<>();
   public final Map<UUID, Float> bulletTurnShieldSnap = new HashMap<>();
   public static final int VIRTUAL_ENEMY_ID = Integer.MIN_VALUE;
   public boolean isVirtualEnemy = false;
   public float virtualHp = 0.0F;
   public float virtualArmor = 0.0F;
   public float sharedTp = 0.0F;
   public float sharedTpMax = 100.0F;
   public final Map<UUID, Float> pvpTp = new LinkedHashMap<>();

   public void setPlayerStatus(UUID pid, BattleState.PlayerStatusKind kind) {
      if (kind != BattleState.PlayerStatusKind.BOOST) {
         this.attackBoostTurns.remove(pid);
         this.attackBoostMult.remove(pid);
      }

      if (kind != BattleState.PlayerStatusKind.SHIELD) {
         this.shieldTurnsLeft.remove(pid);
         this.shieldReduction.put(pid, 0.0F);
      }

      if (kind != BattleState.PlayerStatusKind.BURN) {
         this.playerBurned.remove(pid);
      }

      if (kind != BattleState.PlayerStatusKind.FREEZE) {
         this.playerFreezeMap.remove(pid);
      }

      if (kind != BattleState.PlayerStatusKind.ASLEEP) {
         this.playerSleepMap.remove(pid);
      }

      if (kind != BattleState.PlayerStatusKind.CONFUSION) {
         this.playerConfusionMap.remove(pid);
      }
   }

   public float getSpareThreshold(int enemyId) {
      return this.enemySpareThresholds.getOrDefault(enemyId, this.spareHpThreshold);
   }

   public void raiseSpareThreshold(int enemyId, float addPct) {
      float current = this.getSpareThreshold(enemyId);
      this.enemySpareThresholds.put(enemyId, Math.min(1.0F, current + addPct));
   }

   public boolean isEnemyAsleep(int enemyId) {
      return this.enemySleepTurnsMap.getOrDefault(enemyId, 0) > 0;
   }

   public boolean isEnemyConfused(int enemyId) {
      return this.enemyConfusedLeftMap.getOrDefault(enemyId, 0) > 0;
   }

   public boolean isEnemySpareable(int enemyId) {
      return this.enemySpareableSet.contains(enemyId);
   }

   public float getEnemyAttackReduce(int enemyId) {
      return this.enemyAttackReduceMap.getOrDefault(enemyId, 0.0F);
   }

   public int getEnemyWeakenTurns(int enemyId) {
      return this.enemyWeakenTurnsMap.getOrDefault(enemyId, 0);
   }

   public float getEnemyArmorDebuff(int enemyId) {
      return this.enemyArmorDebuffMap.getOrDefault(enemyId, 0.0F);
   }

   public boolean isSleepImmune(int enemyId) {
      return this.sleepImmuneSet.contains(enemyId);
   }

   public boolean isConfuseImmune(int enemyId) {
      return this.confuseImmuneSet.contains(enemyId);
   }

   public boolean isWeakenImmune(int enemyId) {
      return this.weakenImmuneSet.contains(enemyId);
   }

   public boolean isFreezeImmune(int enemyId) {
      return this.freezeImmuneSet.contains(enemyId);
   }

   public boolean isBurnImmune(int enemyId) {
      return this.burnImmuneSet.contains(enemyId);
   }

   public void pushLog(String msg) {
      if (msg != null && !msg.isEmpty()) {
         this.logHistory.addLast(msg);

         while (this.logHistory.size() > 6) {
            this.logHistory.removeFirst();
         }
      }
   }

   public int playerLevelAtStart() {
      return this.levelAtStart;
   }

   public boolean holdsLives(int enemyId) {
      return this.bossEnemyId >= 0 ? enemyId == this.bossEnemyId : enemyId == this.primaryEnemyId;
   }

   public BattleState(UUID playerUuid, LivingEntity entity, float scaledMaxHp) {
      this.playerUuid = playerUuid;
      this.entityUuid = entity.getUuid();
      this.entityNetworkId = entity.getId();
      this.entityName = entity.getDisplayName().getString();
      this.entityTypeKey = EntityType.getId(entity.getType()).toString();
      if (entity instanceof MobEntity mob) {
         if (mob.getCommandTags().contains("story_antonio")) {
            this.entityTypeKey = "turnbattle:story_antonio";
         }

         if (mob instanceof DarknerBossEntity boss) {
            this.entityTypeKey = DarknerBosses.actKey(boss);
         }
      }

      this.entityMaxHp = scaledMaxHp;
      this.entityCurrentHp = Math.min(entity.getHealth(), scaledMaxHp);
      this.primaryEnemyId = entity.getId();
   }

   public BattleState(UUID playerUuid, String name, String typeKey, float maxHp, float armor) {
      this.playerUuid = playerUuid;
      this.entityUuid = UUID.randomUUID();
      this.entityNetworkId = Integer.MIN_VALUE;
      this.primaryEnemyId = Integer.MIN_VALUE;
      this.entityName = name;
      this.entityTypeKey = typeKey;
      this.entityMaxHp = maxHp;
      this.entityCurrentHp = maxHp;
      this.isVirtualEnemy = true;
      this.virtualHp = maxHp;
      this.virtualArmor = armor;
   }

   public void addParticipant(UUID uuid, String name, PlayerClass cls, float hp, float maxHp) {
      if (!this.participantOrder.contains(uuid)) {
         this.participantOrder.add(uuid);
         this.participantTp.put(uuid, 0.0F);
         this.participantTpMax.put(uuid, 100.0F);
         this.participantHp.put(uuid, hp);
         this.participantMaxHp.put(uuid, maxHp);
         this.participantNames.put(uuid, name);
         this.participantClass.put(uuid, cls);
         this.defending.put(uuid, false);
         this.shieldReduction.put(uuid, 0.0F);
         this.shieldTurnsLeft.put(uuid, 0);
         this.attackBoostTurns.put(uuid, 0);
         this.recalcSharedTpMax();
      }
   }

   public void removeParticipant(UUID uuid) {
      int idx = this.participantOrder.indexOf(uuid);
      this.participantOrder.remove(uuid);
      this.participantTp.remove(uuid);
      this.participantTpMax.remove(uuid);
      this.participantHp.remove(uuid);
      this.participantMaxHp.remove(uuid);
      this.participantNames.remove(uuid);
      this.participantClass.remove(uuid);
      this.defending.remove(uuid);
      this.shieldReduction.remove(uuid);
      this.shieldTurnsLeft.remove(uuid);
      this.attackBoostTurns.remove(uuid);
      if (idx >= 0 && idx < this.currentParticipantIndex) {
         this.currentParticipantIndex--;
      }

      if (this.currentParticipantIndex >= this.participantOrder.size()) {
         this.currentParticipantIndex = this.participantOrder.isEmpty() ? 0 : this.participantOrder.size() - 1;
      }

      this.recalcSharedTpMax();
   }

   public UUID currentParticipant() {
      return this.participantOrder.isEmpty() ? this.playerUuid : this.participantOrder.get(this.currentParticipantIndex % this.participantOrder.size());
   }

   public void advanceTurn() {
      if (!this.participantOrder.isEmpty()) {
         this.currentParticipantIndex = (this.currentParticipantIndex + 1) % this.participantOrder.size();
      }
   }

   public void recalcSharedTpMax() {
      int count = Math.max(1, this.participantOrder.size());
      this.sharedTpMax = 100.0F + (float)(count - 1) * 75.0F;
      if (this.sharedTp > this.sharedTpMax) {
         this.sharedTp = this.sharedTpMax;
      }
   }

   public float getTp(UUID uuid) {
      return this.isPvp ? this.pvpTp.getOrDefault(uuid, 0.0F) : this.sharedTp;
   }

   public void addTp(UUID uuid, float amt) {
      if (this.isPvp) {
         float max = this.participantTpMax.getOrDefault(uuid, 100.0F);
         float cur = this.pvpTp.getOrDefault(uuid, 0.0F);
         float next = cur + amt;
         float clamped = Math.max(0.0F, Math.min(max, next));
         this.pvpTp.put(uuid, clamped);
      } else {
         float next = this.sharedTp + amt;
         this.sharedTp = Math.max(0.0F, Math.min(this.sharedTpMax, next));
      }
   }

   public void addTpCapped(UUID uuid, float amt) {
      if (this.isPvp) {
         float max = this.participantTpMax.getOrDefault(uuid, 100.0F);
         float cur = this.pvpTp.getOrDefault(uuid, 0.0F);
         this.pvpTp.put(uuid, Math.max(0.0F, Math.min(max, cur + amt)));
      } else {
         this.sharedTp = Math.max(0.0F, Math.min(this.sharedTpMax, this.sharedTp + amt));
      }
   }

   public void clampAllTpToMax() {
      if (this.isPvp) {
         for (UUID pid : this.participantOrder) {
            float max = this.participantTpMax.getOrDefault(pid, 100.0F);
            float cur = this.pvpTp.getOrDefault(pid, 0.0F);
            if (cur > max) {
               this.pvpTp.put(pid, max);
            }
         }
      } else {
         if (this.sharedTp > this.sharedTpMax) {
            this.sharedTp = this.sharedTpMax;
         }
      }
   }

   public float getPlayerTp() {
      return this.getTp(this.playerUuid);
   }

   public void addTp(float amount) {
      this.addTp(this.playerUuid, amount);
   }

   public boolean isDefending(UUID uuid) {
      return this.defending.getOrDefault(uuid, false);
   }

   public float getShieldReduction(UUID uuid) {
      return this.shieldReduction.getOrDefault(uuid, 0.0F);
   }

   public int getShieldTurnsLeft(UUID uuid) {
      return this.shieldTurnsLeft.getOrDefault(uuid, 0);
   }

   public int getAttackBoostTurns(UUID uuid) {
      return this.attackBoostTurns.getOrDefault(uuid, 0);
   }

   public float getAttackBoostMult(UUID uuid) {
      return this.attackBoostMult.getOrDefault(uuid, 1.5F);
   }

   public void clearStatusImmunity() {
      this.sleepImmune = false;
      this.confuseImmune = false;
      this.weakenImmune = false;
      this.freezeImmune = false;
      this.burnImmune = false;
      this.sleepImmuneSet.remove(this.primaryEnemyId);
      this.confuseImmuneSet.remove(this.primaryEnemyId);
      this.weakenImmuneSet.remove(this.primaryEnemyId);
      this.freezeImmuneSet.remove(this.primaryEnemyId);
      this.burnImmuneSet.remove(this.primaryEnemyId);
   }

   public boolean isMercyEligible() {
      if (this.isEnemySpareable(this.primaryEnemyId)) {
         return true;
      } else {
         float threshold = this.getSpareThreshold(this.primaryEnemyId);
         return threshold >= 1.0F;
      }
   }

   public boolean allEnemiesDown() {
      if (this.isVirtualEnemy) {
         return false;
      } else if (this.enemyDownTurns.getOrDefault(this.primaryEnemyId, 0) <= 0) {
         return false;
      } else {
         for (int eid : this.extraEnemyIds) {
            if (this.enemyDownTurns.getOrDefault(eid, 0) <= 0) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean isMercyEligibleById(int enemyId) {
      if (this.isEnemySpareable(enemyId)) {
         return true;
      } else {
         float threshold = this.getSpareThreshold(enemyId);
         return threshold >= 1.0F;
      }
   }

   public void tickStatusEffects() {
      this.tickEnemyStatusEffects();
      this.tickPlayerDefensiveState();
   }

   public void setEnemyStatus(int enemyId, BattleState.StatusKind kind) {
      if (kind != BattleState.StatusKind.SLEEP) {
         this.enemySleepTurnsMap.remove(enemyId);
      }

      if (kind != BattleState.StatusKind.CONFUSE) {
         this.enemyConfusedLeftMap.remove(enemyId);
      }

      if (kind != BattleState.StatusKind.FREEZE) {
         this.enemyFreezeTurnsMap.remove(enemyId);
      }

      if (kind != BattleState.StatusKind.BURN) {
         this.enemyBurnTurnsMap.remove(enemyId);
      }

      if (kind != BattleState.StatusKind.WEAKEN) {
         this.enemyWeakenTurnsMap.remove(enemyId);
         this.enemyAttackReduceMap.remove(enemyId);
         this.enemyArmorDebuffMap.remove(enemyId);
      }

      if (enemyId == this.primaryEnemyId) {
         if (kind != BattleState.StatusKind.SLEEP) {
            this.enemySleepTurns = 0;
         }

         if (kind != BattleState.StatusKind.CONFUSE) {
            this.enemyConfused = false;
            this.enemyConfusedLeft = 0;
         }

         if (kind != BattleState.StatusKind.FREEZE) {
            this.enemyFreezeTurns = 0;
         }

         if (kind != BattleState.StatusKind.BURN) {
            this.enemyBurnTurns = 0;
         }

         if (kind != BattleState.StatusKind.WEAKEN) {
            this.enemyWeakenTurns = 0;
            this.enemyAttackReduce = 0.0F;
            this.enemyArmorDebuff = 0.0F;
         }
      }
   }

   public void tickEnemyStatusEffects() {
      this.enemyConfusedLeftMap.replaceAll((id, t) -> t - 1);
      this.enemyConfusedLeftMap.entrySet().removeIf(e -> {
         if (e.getValue() <= 0) {
            this.confuseImmuneSet.add(e.getKey());
            return true;
         } else {
            return false;
         }
      });
      this.enemyConfusedLeft = this.enemyConfusedLeftMap.getOrDefault(this.primaryEnemyId, 0);
      this.enemyConfused = this.enemyConfusedLeft > 0;
      this.confuseImmune = this.confuseImmuneSet.contains(this.primaryEnemyId);
      this.enemyFreezeTurnsMap.replaceAll((id, t) -> t - 1);
      this.enemyFreezeTurnsMap.entrySet().removeIf(e -> {
         if (e.getValue() <= 0) {
            this.freezeImmuneSet.add(e.getKey());
            return true;
         } else {
            return false;
         }
      });
      this.enemyFreezeTurns = this.enemyFreezeTurnsMap.getOrDefault(this.primaryEnemyId, 0);
      this.freezeImmune = this.freezeImmuneSet.contains(this.primaryEnemyId);
      this.enemyBurnTurnsMap.replaceAll((id, t) -> t - 1);
      this.enemyBurnTurnsMap.entrySet().removeIf(e -> {
         if (e.getValue() <= 0) {
            this.burnImmuneSet.add(e.getKey());
            return true;
         } else {
            return false;
         }
      });
      this.enemyBurnTurns = this.enemyBurnTurnsMap.getOrDefault(this.primaryEnemyId, 0);
      this.burnImmune = this.burnImmuneSet.contains(this.primaryEnemyId);
      this.enemyDownTurns.replaceAll((id, t) -> t - 1);
      this.enemyDownTurns.entrySet().removeIf(e -> {
         if (e.getValue() <= 0) {
            this.enemySturdy.merge(e.getKey(), 2, Math::max);
            return true;
         } else {
            return false;
         }
      });
      this.enemySturdy.replaceAll((id, t) -> t - 1);
      this.enemySturdy.entrySet().removeIf(e -> e.getValue() <= 0);
      this.enemyWeakenTurnsMap.replaceAll((id, t) -> t - 1);
      this.enemyWeakenTurnsMap.entrySet().removeIf(e -> {
         if (e.getValue() <= 0) {
            this.enemyAttackReduceMap.remove(e.getKey());
            this.enemyArmorDebuffMap.remove(e.getKey());
            this.weakenImmuneSet.add(e.getKey());
            return true;
         } else {
            return false;
         }
      });
      this.enemyArmorDebuff = this.enemyArmorDebuffMap.getOrDefault(this.primaryEnemyId, 0.0F);
      this.enemyWeakenTurns = this.enemyWeakenTurnsMap.getOrDefault(this.primaryEnemyId, 0);
      this.enemyAttackReduce = this.enemyAttackReduceMap.getOrDefault(this.primaryEnemyId, 0.0F);
      this.weakenImmune = this.weakenImmuneSet.contains(this.primaryEnemyId);
   }

   public void tickPlayerDefensiveState() {
      for (UUID pid : this.participantOrder) {
         int st = this.shieldTurnsLeft.getOrDefault(pid, 0);
         if (st > 0) {
            this.shieldTurnsLeft.put(pid, --st);
            if (st == 0) {
               this.shieldReduction.put(pid, 0.0F);
            }
         }

         int ab = this.attackBoostTurns.getOrDefault(pid, 0);
         if (ab > 0) {
            this.attackBoostTurns.put(pid, ab - 1);
            if (ab - 1 == 0) {
               this.attackBoostMult.remove(pid);
            }
         }

         this.defending.put(pid, false);
      }
   }

   public boolean isAllyPair(UUID a, UUID b) {
      if (a.equals(b)) {
         return true;
      } else if (!this.isPvp) {
         return true;
      } else {
         Integer ta = this.participantTeam.get(a);
         Integer tb = this.participantTeam.get(b);
         return ta != null && tb != null ? ta.equals(tb) : false;
      }
   }

   public boolean isEnemyPair(UUID a, UUID b) {
      if (a.equals(b)) {
         return false;
      } else {
         return !this.isPvp ? false : !this.isAllyPair(a, b);
      }
   }

   public List<UUID> teammatesOf(UUID uuid) {
      List result = new ArrayList<>();

      for (UUID pid : this.participantOrder) {
         if (this.isAllyPair(uuid, pid)) {
            result.add(pid);
         }
      }

      return result;
   }

   public List<UUID> enemiesOf(UUID uuid) {
      List result = new ArrayList<>();

      for (UUID pid : this.participantOrder) {
         if (this.isEnemyPair(uuid, pid)) {
            result.add(pid);
         }
      }

      return result;
   }

   public static final class BashEntry {
      public final BattleState.QueuedAction pick;
      public final boolean crit;
      public int delay;
      public boolean done;
      public boolean landed;
      public float mult = 1.0F;
      public int tpTier = 0;

      public BashEntry(BattleState.QueuedAction pick, boolean crit) {
         this.pick = pick;
         this.crit = crit;
         this.done = crit;
      }
   }

   public static final class PendingHit {
      public final long due;
      public final BooleanSupplier hit;

      public PendingHit(long due, BooleanSupplier hit) {
         this.due = due;
         this.hit = hit;
      }
   }

   public static enum Phase {
      PLAYER_TURN,
      AWAITING_ENEMY_RESULT;
   }

   public static enum PlayerStatusKind {
      BOOST,
      SHIELD,
      BURN,
      FREEZE,
      ASLEEP,
      CONFUSION;
   }

   public static final class QueuedAction {
      public final UUID actor;
      public final UUID owner;
      public final int type;
      public final int param;
      public final int targetIndex;
      public final int targetEnemyId;
      public final int reservedTp;
      public final int level;
      public final PlayerClass cls;

      public QueuedAction(UUID actor, UUID owner, int type, int param, int targetIndex, int targetEnemyId, int reservedTp, int level, PlayerClass cls) {
         this.actor = actor;
         this.owner = owner;
         this.type = type;
         this.param = param;
         this.targetIndex = targetIndex;
         this.targetEnemyId = targetEnemyId;
         this.reservedTp = reservedTp;
         this.level = level;
         this.cls = cls;
      }

      public int stage() {
         return this.type == 2 ? 0 : (this.type == 0 ? 2 : 1);
      }
   }

   public static enum StatusKind {
      SLEEP,
      CONFUSE,
      FREEZE,
      BURN,
      WEAKEN;
   }
}
