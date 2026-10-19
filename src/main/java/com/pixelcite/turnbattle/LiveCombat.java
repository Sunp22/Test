package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class LiveCombat {
   public static final Identifier C2S = Identifier.of("turnbattle", "live_c2s");
   public static final Identifier S2C_STATE = Identifier.of("turnbattle", "live_state");
   public static final Identifier S2C_FX = Identifier.of("turnbattle", "live_fx");
   public static final Identifier S2C_MOB = Identifier.of("turnbattle", "live_mob");
   public static final int OP_ATTACK = 0;
   public static final int OP_BLOCK = 1;
   public static final int OP_DASH = 2;
   public static final int OP_LOCK = 3;
   public static final int OP_SKILL = 4;
   public static final int OP_ACT = 5;
   public static final int OP_ITEM = 6;
   public static final int OP_MERCY = 7;
   public static final int OP_MENU = 8;
   public static final int OP_BASH = 9;
   public static final int FX_HIT = 0;
   public static final int FX_SWING = 1;
   public static final int FX_HURT = 2;
   public static final int FX_GRAZE = 3;
   public static final int FX_SKILL = 4;
   public static final int FX_HEAL = 5;
   public static final int FX_SPARE = 6;
   public static final int FX_WINDUP = 7;
   public static final int FX_POSE = 8;
   public static final int FX_TEXT = 9;
   public static final int FX_PREPARE = 10;
   public static final int FX_LUNGE = 11;
   public static final int FX_MOBSKILL = 12;
   public static final int FX_STUN = 13;
   public static final int FX_ARMED = 14;
   public static final int POSE_NONE = 0;
   public static final int POSE_GUARD = 1;
   public static final int POSE_CHOOSE_SKILL = 2;
   public static final int POSE_CHOOSE_ITEM = 3;
   public static final int POSE_CHOOSE_ACT = 4;
   public static final int POSE_DASH = 5;
   public static final int POSE_ITEM = 6;
   public static final int POSE_ACT = 7;
   public static final int POSE_SPARE = 8;
   public static final int POSE_BASH_READY = 9;
   public static final int POSE_BASH = 10;
   public static final int HIT_FINISHER = 1;
   public static final int HIT_WEAK = 2;
   public static final int HIT_RESIST = 4;
   public static final int HIT_BASH = 8;
   public static final int HIT_STEP_SHIFT = 4;
   public static final int HURT_BLOCKED = 1;
   public static final int HURT_PERFECT = 2;
   public static final int ST_BURN = 1;
   public static final int ST_FREEZE = 2;
   public static final int ST_SLEEP = 4;
   public static final int ST_CONFUSE = 8;
   public static final int ST_WEAK = 16;
   public static final int ST_STUN = 32;
   public static final float TP_MAX = 100.0F;
   static final int SWING = 8;
   static final int COMBO_COOLDOWN = 39;
   static final int COMBO_WINDOW = 22;
   static final float HIT_MULT = 0.225F;
   static final float FINISHER_MULT = 1.0F;
   static final float BASH_QTE_MULT = 2.0F;
   static final int BASH_LUNGE_TICKS = 9;
   static final int BASH_COOLDOWN = 80;
   public static final int BASH_TP = 16;
   public static final int ACT_COOLDOWN = 100;
   static final int GUARD_STUN_MOB = 40;
   static final int GUARD_STUN_PLAYER = 26;
   static final float TP_PER_HIT = 2.5F;
   static final float TP_PER_FINISHER = 6.0F;
   static final float TP_BLOCK = 5.0F;
   static final float TP_PERFECT = 16.0F;
   static final int PERFECT_WINDOW = 6;
   static final int WINDUP_TICKS = 10;
   static final int DASH_IFRAMES = 8;
   static final int DASH_COOLDOWN = 14;
   static final float BLOCK_KEEP = 0.35F;
   static final float MOB_DMG = 0.4F;
   static final float MELEE_MULT = 0.8F;
   static final float PROJECTILE_MULT = 0.64000005F;
   static final float SKILL_MULT = 0.8F;
   static final float PVP_MULT = 0.6F;
   static final double REACH = 3.2;
   static final double FINISHER_REACH = 3.8;
   static final float SKILL_CHANCE = 0.035F;
   static final int PREP_TICKS = 22;
   static final int SELF_PREP_TICKS = 14;
   static final int LUNGE_TICKS = 9;
   static final int SKILL_COOLDOWN = 160;
   static final double LUNGE_SPEED = 0.95;
   static final double FX_RANGE = 30.0;
   public static boolean APPLYING = false;
   static final double PARTY_HEAL_RANGE = 16.0;
   private static final Random RNG = new Random();
   private static final Map<UUID, LiveCombat.LP> players = new HashMap<>();
   private static final Map<Integer, LiveCombat.LM> mobs = new HashMap<>();
   private static final List<LiveCombat.Pending> pending = new ArrayList<>();
   private static final Map<Integer, LiveCombat.MobSkill> skills = new HashMap<>();
   private static final List<LiveCombat.Later> later = new ArrayList<>();
   static final float SKILL_LAND_AT = 0.8F;
   private static final Set<Long> grazedProjectiles = new HashSet<>();
   public static final double ALLY_RANGE = 30.0;

   private LiveCombat() {
   }

   private static void later(MinecraftServer server, int ticks, Runnable r) {
      later.add(new LiveCombat.Later(now(server) + (long)ticks, r));
   }

   private static LiveCombat.LP lp(UUID u) {
      return players.computeIfAbsent(u, k -> new LiveCombat.LP());
   }

   public static void reset() {
      players.clear();
      mobs.clear();
      pending.clear();
      skills.clear();
      later.clear();
      grazedProjectiles.clear();
   }

   private static LiveCombat.LP freshen(UUID u, long now) {
      LiveCombat.LP p = lp(u);
      long ahead = now + 400L;
      if (p.lastAtk <= now + 1L
         && p.comboReadyAt <= ahead
         && p.dashReady <= ahead
         && p.skillReady <= ahead
         && p.itemReady <= ahead
         && p.bashReady <= ahead
         && p.bashUntil <= ahead
         && p.stunUntil <= ahead
         && p.iframesUntil <= ahead
         && p.blockStart <= now + 1L) {
         return p;
      } else {
         LiveCombat.LP n = new LiveCombat.LP();
         n.tp = p.tp;
         n.lockId = p.lockId;
         players.put(u, n);
         return n;
      }
   }

   private static LiveCombat.LM lm(LivingEntity e) {
      LiveCombat.LM m = mobs.computeIfAbsent(e.getId(), k -> new LiveCombat.LM());
      if (m.lives < 0) {
         int tier = EliteMobManager.getTier(e.getUuid());
         int[] livesPerTier = new int[]{1, 2, 3, 4, 4};
         m.maxLives = tier >= 0 ? livesPerTier[Math.min(tier, livesPerTier.length - 1)] : 0;
         m.lives = m.maxLives;
      }

      return m;
   }

   private static long now(MinecraftServer s) {
      return s.getOverworld().getTime();
   }

   public static boolean isLive(MinecraftServer server, UUID uuid) {
      if (server != null && uuid != null) {
         BattleData.PlayerProgress p = BattleData.get(server).players().get(uuid);
         return p != null && p.battleStyle == BattleStyle.LIVE_COMBAT;
      } else {
         return false;
      }
   }

   public static boolean isLive(ServerPlayerEntity p) {
      return p != null
         && isLive(p.getServer(), p.getUuid())
         && !p.isSpectator()
         && !BattleManager.activeBattles.containsKey(p.getUuid())
         && !SoulWalk.isOut(p.getUuid());
   }

   public static boolean sameParty(MinecraftServer server, UUID a, UUID b) {
      if (a.equals(b)) {
         return true;
      } else {
         UUID pa = PlayerPartyManager.getPartyId(a);
         if (pa != null && pa.equals(PlayerPartyManager.getPartyId(b))) {
            return true;
         } else {
            RogueliteData rd = RogueliteData.get(server);
            RogueliteData.PlayerRogueData ra = rd.players.get(a);
            RogueliteData.PlayerRogueData rb = rd.players.get(b);
            return ra != null && rb != null && ra.partyId != null && ra.partyId.equals(rb.partyId);
         }
      }
   }

   public static void register() {
      for (Identifier id : new Identifier[]{C2S, S2C_STATE, S2C_FX, S2C_MOB}) {
         Net121.registerS2CChannel(id);
         Net121.registerC2SChannel(id);
      }

      Net121.registerServerReceiver(C2S, (server, player, handler, buf, sender) -> {
         int op = buf.readByte();
         int a = 0;
         int b = 0;
         float f1 = 0.0F;
         float f2 = 0.0F;
         boolean flag = false;
         switch (op) {
            case 0:
               a = buf.readVarInt();
               b = buf.readInt();
               break;
            case 1:
               flag = buf.readBoolean();
               break;
            case 2:
               f1 = buf.readFloat();
               f2 = buf.readFloat();
               break;
            case 3:
            case 7:
               b = buf.readInt();
               break;
            case 4:
            case 5:
            case 6:
               a = buf.readVarInt();
               b = buf.readInt();
               break;
            case 8:
               flag = buf.readBoolean();
               a = buf.readByte();
               break;
            case 9:
               f1 = buf.readFloat();
               a = buf.readVarInt();
               b = buf.readInt();
               break;
            default:
               return;
         }

         int fa = a;
         int fb = b;
         float ff1 = f1;
         float ff2 = f2;
         boolean fflag = flag;
         server.execute(() -> onPacket(server, player, op, fa, fb, ff1, ff2, fflag));
      });
   }

   private static void onPacket(MinecraftServer server, ServerPlayerEntity player, int op, int a, int b, float f1, float f2, boolean flag) {
      if (isLive(player) && player.isAlive() && !BattleManager.isInBattle(player.getUuid())) {
         long now = now(server);
         LiveCombat.LP p = freshen(player.getUuid(), now);
         if (now >= p.stunUntil || op != 0 && op != 2 && op != 9 && op != 4 && op != 5 && op != 6 && op != 7 && (op != 1 || !flag)) {
            switch (op) {
               case 0:
                  attack(server, player, p, b, now);
                  break;
               case 1:
                  if (flag && !p.blocking) {
                     p.blocking = true;
                     p.blockStart = now;
                     pose(player, 1);
                  } else if (!flag && p.blocking) {
                     p.blocking = false;
                     pose(player, 0);
                  }
                  break;
               case 2:
                  if (now < p.dashReady) {
                     return;
                  }

                  p.dashReady = now + 14L;
                  p.iframesUntil = now + 8L;
                  if (p.blocking) {
                     p.blocking = false;
                  }

                  pose(player, 5);
                  break;
               case 3:
                  p.lockId = b;
                  sendArmed(player, p);
                  Entity e = b >= 0 ? player.getServerWorld().getEntityById(b) : null;
                  if (e instanceof LivingEntity le && !(e instanceof PlayerEntity)) {
                     sendMobInfo(le, lm(le), player);
                  }
                  break;
               case 4:
                  castSkill(server, player, p, a, b, now);
                  break;
               case 5:
                  act(server, player, p, a, b, now);
                  break;
               case 6:
                  useItem(server, player, p, a, b, now);
                  break;
               case 7:
                  mercy(server, player, b);
                  break;
               case 8:
                  p.menuOpen = flag;
                  if (p.blocking && flag) {
                     p.blocking = false;
                  }

                  int pz = a != 2 && a != 3 && a != 4 && a != 9 ? 0 : a;
                  pose(player, flag ? pz : 0);
                  break;
               case 9:
                  startBash(server, player, p, f1, a, b, now);
            }
         }
      }
   }

   public static void sendState(ServerPlayerEntity player) {
      LiveCombat.LP p = lp(player.getUuid());
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(isLive(player.getServer(), player.getUuid()));
      buf.writeFloat(p.tp);
      p.sentTp = p.tp;
      Net121.sendToClient(player, S2C_STATE, buf);
   }

   public static void onJoin(ServerPlayerEntity player) {
      sendState(player);
   }

   public static void onLeave(ServerPlayerEntity player) {
      LiveCombat.LP p = players.get(player.getUuid());
      if (p != null) {
         p.blocking = false;
         p.menuOpen = false;
         p.lockId = -1;
         p.bashUntil = 0L;
         p.armedSent = -2;
      }

      pending.removeIf(h -> h.player().equals(player.getUuid()));
   }

   private static void fx(ServerWorld w, Vec3d at, Consumer<PacketByteBuf> body) {
      double r2 = 900.0;

      for (ServerPlayerEntity p : w.getPlayers()) {
         if (!(p.squaredDistanceTo(at) > r2)) {
            PacketByteBuf buf = PacketByteBufs.create();
            body.accept(buf);
            Net121.sendToClient(p, S2C_FX, buf);
         }
      }
   }

   private static void pose(ServerPlayerEntity p, int pose) {
      fx(p.getServerWorld(), p.getPos(), b -> {
         b.writeByte(8);
         b.writeInt(p.getId());
         b.writeByte(pose);
      });
   }

   private static void text(Entity e, String s, int color) {
      if (e.getWorld() instanceof ServerWorld w) {
         fx(w, e.getPos(), b -> {
            b.writeByte(9);
            b.writeInt(e.getId());
            b.writeString(s);
            b.writeInt(color);
         });
      }
   }

   private static int statusMask(LiveCombat.LM m, long now) {
      int s = 0;
      if (now < m.burnUntil) {
         s |= 1;
      }

      if (now < m.freezeUntil) {
         s |= 2;
      }

      if (now < m.sleepUntil) {
         s |= 4;
      }

      if (now < m.confuseUntil) {
         s |= 8;
      }

      if (now < m.weakUntil) {
         s |= 16;
      }

      if (now < m.stunUntil) {
         s |= 32;
      }

      return s;
   }

   private static void sendMobInfo(LivingEntity e, LiveCombat.LM m, ServerPlayerEntity only) {
      if (e.getWorld() instanceof ServerWorld w) {
         int var9 = statusMask(m, now(w.getServer()));
         m.sentMask = var9;
         Consumer var5 = b -> {
            b.writeInt(e.getId());
            b.writeFloat(Math.min(1.0F, m.mercy));
            b.writeByte(var9);
            b.writeByte(Math.max(0, m.lives));
            b.writeByte(m.maxLives);
            b.writeBoolean(m.spareable || m.mercy >= 1.0F);
         };
         if (only != null) {
            PacketByteBuf buf = PacketByteBufs.create();
            var5.accept(buf);
            Net121.sendToClient(only, S2C_MOB, buf);
         } else {
            for (ServerPlayerEntity p : w.getPlayers()) {
               if (!(p.squaredDistanceTo(e) > 2304.0)) {
                  PacketByteBuf buf = PacketByteBufs.create();
                  var5.accept(buf);
                  Net121.sendToClient(p, S2C_MOB, buf);
               }
            }
         }
      }
   }

   private static void addTp(ServerPlayerEntity p, LiveCombat.LP lp, float amount) {
      lp.tp = MathHelper.clamp(lp.tp + amount, 0.0F, 100.0F);
   }

   private static void attack(MinecraftServer server, ServerPlayerEntity player, LiveCombat.LP p, int lockId, long now) {
      if (now < p.comboReadyAt) {
         p.queuedSwings = 0;
      } else if (p.step >= 0 && p.step < 4 && now - p.lastAtk < 8L) {
         if (p.queuedSwings < 4 - p.step) {
            p.queuedSwings++;
            p.queuedLock = lockId;
            p.queuedAt = now;
         }
      } else {
         int step = p.step >= 0 && p.step < 4 && now - p.lastAtk <= 22L ? p.step + 1 : 0;
         if (step == 4) {
            p.comboReadyAt = now + 39L;
         }

         p.step = step;
         p.lastAtk = now;
         if (p.blocking) {
            p.blocking = false;
            pose(player, 0);
         }

         if (lockId >= 0) {
            p.lockId = lockId;
         }

         boolean fin = step == 4;
         List hits = collectHits(server, player, fin, p.lockId);
         if (hits.isEmpty()) {
            fx(player.getServerWorld(), player.getPos(), b -> {
               b.writeByte(1);
               b.writeInt(player.getId());
               b.writeByte(step);
            });
         } else {
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
            CharStats eff = BattleManager.effectiveStatsOf(player.getUuid(), prog);
            float base = bashBase(player, prog, eff, now, p);
            Affinity[] elems = weaponElements(prog);
            float gained = 0.0F;
            int stepBits = step << 4;

            for (LivingEntity t : hits) {
               strike(server, player, prog, t, base * (fin ? 1.0F : 0.225F), elems, fin, (fin ? 1 : 0) | stepBits, fin ? 22 : 18, now);
               gained += fin ? 6.0F : 2.5F;
            }

            addTp(player, p, gained);
         }
      }
   }

   private static void strike(
      MinecraftServer server,
      ServerPlayerEntity player,
      BattleData.PlayerProgress prog,
      LivingEntity t,
      float dmg,
      Affinity[] elems,
      boolean big,
      int flags,
      int stunTicks,
      long now
   ) {
      if (t instanceof ServerPlayerEntity tp) {
         if (!isLive(tp)) {
            BattleManager.tryStartPvp(player, tp);
         } else {
            hurtPlayer(tp, player, player.getDamageSources().playerAttack(player), dmg, true);
            knock(t, player, big);
            hitFx(player, t, dmg, flags, prog);
         }
      } else {
         LiveCombat.LM m = lm(t);
         float aff = affinityMult(t, elems);
         float armor = (float)t.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
         dmg *= Math.max(0.55F, 1.0F - armor * 0.04F) * MobLevelManager.getMobDamageTaken(t) * aff * (now < m.weakUntil ? m.weakMult : 1.0F);
         dmg /= RogueliteManager.getRunDifficultyScale(server, player.getUuid());
         dmg = Math.max(0.5F, dmg);
         flags |= (aff > 1.01F ? 2 : 0) | (aff < 0.99F ? 4 : 0);
         m.stunUntil = Math.max(m.stunUntil, now + (long)stunTicks);
         m.sleepUntil = 0L;
         pending.removeIf(h -> h.mobId() == t.getId());
         knock(t, player, big);
         if (big) {
            m.slideUntil = now + 8L;
         }

         aggro(t, player);
         hitFx(player, t, dmg, flags, prog);
         damageMob(server, t, m, dmg, player, player.getDamageSources().playerAttack(player));
      }
   }

   private static void startBash(MinecraftServer server, ServerPlayerEntity player, LiveCombat.LP p, float qteMult, int tpTier, int lockId, long now) {
      p.menuOpen = false;
      if (now < p.bashReady || now < p.bashUntil) {
         pose(player, 0);
      } else if (p.tp < 16.0F) {
         pose(player, 0);
         player.sendMessage(Text.literal("Not enough TP! (Bash needs 16)").formatted(Formatting.RED), true);
      } else {
         addTp(player, p, -16.0F);
         if (p.blocking) {
            p.blocking = false;
         }

         p.bashMult = MathHelper.clamp(Float.isNaN(qteMult) ? 1.0F : qteMult, 0.3F, 1.6F) * 2.0F;
         p.bashTp = MathHelper.clamp(tpTier, 1, 3);
         p.bashUntil = now + 9L;
         p.bashReady = now + 80L;
         p.bashHit.clear();
         if (lockId >= 0) {
            p.lockId = lockId;
         }

         pose(player, 10);
      }
   }

   private static void tickBashes(MinecraftServer server, long now) {
      for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
         LiveCombat.LP p = players.get(pl.getUuid());
         if (p != null && now < p.bashUntil && pl.isAlive()) {
            Box box = pl.getBoundingBox().expand(0.7, 0.3, 0.7);
            List hit = new ArrayList<>();

            for (Entity e : pl.getServerWorld().getOtherEntities(pl, box)) {
               if (e instanceof LivingEntity) {
                  LivingEntity t = (LivingEntity)e;
                  if (t.isAlive() && !(t instanceof ArmorStandEntity) && !p.bashHit.contains(t.getId()) && attackable(server, pl, t)) {
                     hit.add(t);
                  }
               }
            }

            if (!hit.isEmpty()) {
               BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(pl.getUuid());
               CharStats eff = BattleManager.effectiveStatsOf(pl.getUuid(), prog);
               float base = bashBase(pl, prog, eff, now, p);
               Affinity[] elems = weaponElements(prog);

               for (LivingEntity t : hit) {
                  boolean first = p.bashHit.isEmpty();
                  p.bashHit.add(t.getId());
                  strike(server, pl, prog, t, base * p.bashMult, elems, true, 9, 30, now);
                  if (first) {
                     addTp(pl, p, p.bashTp == 3 ? 12.0F : (p.bashTp == 2 ? 6.0F : 3.0F));
                  }
               }

               p.bashUntil = Math.min(p.bashUntil, now + 2L);
            }
         }
      }
   }

   private static void guardStun(MinecraftServer server, LivingEntity who, ServerPlayerEntity guard, long now) {
      int ticks;
      if (who instanceof ServerPlayerEntity sp) {
         LiveCombat.LP q = lp(sp.getUuid());
         ticks = 26;
         q.stunUntil = Math.max(q.stunUntil, now + (long)ticks);
         q.bashUntil = 0L;
         q.step = -1;
         q.blocking = false;
         q.menuOpen = false;
         pose(sp, 0);
         sp.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 4, false, false, false));
      } else {
         LiveCombat.LM m = lm(who);
         ticks = 40;
         m.stunUntil = Math.max(m.stunUntil, now + (long)ticks);
         pending.removeIf(h -> h.mobId() == who.getId());
         sendMobInfo(who, m, null);
      }

      knock(who, guard, false);
      text(who, "STUNNED", 16777062);
      if (who.getWorld() instanceof ServerWorld w) {
         int t = ticks;
         fx(w, who.getPos(), b -> {
            b.writeByte(13);
            b.writeInt(who.getId());
            b.writeByte(t);
         });
      }
   }

   private static void sendArmed(ServerPlayerEntity player, LiveCombat.LP p) {
      boolean armed = p.lockId >= 0 && isLive(player);
      int sig = armed ? 1 : 0;
      if (sig != p.armedSent) {
         p.armedSent = sig;
         broadcastArmed(player, armed);
      }
   }

   private static void broadcastArmed(ServerPlayerEntity player, boolean armed) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         ItemStack w = armed ? BattleData.get(server).getOrCreate(player.getUuid()).equippedWeapon : null;
         boolean has = w != null && !w.isEmpty();
         NbtCompound nbt = has ? (NbtCompound)w.encode(server.getRegistryManager()) : null;
         fx(player.getServerWorld(), player.getPos(), b -> {
            b.writeByte(14);
            b.writeInt(player.getId());
            b.writeBoolean(armed);
            b.writeBoolean(has);
            if (has) {
               b.writeNbt(nbt);
            }
         });
      }
   }

   private static void hitFx(ServerPlayerEntity player, LivingEntity t, float dmg, int flags, BattleData.PlayerProgress prog) {
      int col = prog.soulType != null ? prog.soulType.getColor() & 16777215 : 16777215;
      fx(player.getServerWorld(), t.getPos(), b -> {
         b.writeByte(0);
         b.writeInt(player.getId());
         b.writeInt(t.getId());
         b.writeFloat(dmg);
         b.writeByte(flags);
         b.writeInt(col);
      });
   }

   private static List<LivingEntity> collectHits(MinecraftServer server, ServerPlayerEntity player, boolean fin, int lockId) {
      double reach = fin ? 3.8 : 3.2;
      double arcCos = Math.cos(Math.toRadians(fin ? 75.0 : 60.0));
      float yr = player.getYaw() * (float) (Math.PI / 180.0);
      Vec3d fwd = new Vec3d((double)(-MathHelper.sin(yr)), 0.0, (double)MathHelper.cos(yr));
      Vec3d at = player.getPos();
      List out = new ArrayList<>();

      for (Entity e : player.getServerWorld().getOtherEntities(player, player.getBoundingBox().expand(reach + 1.5, 1.5, reach + 1.5))) {
         if (e instanceof LivingEntity) {
            LivingEntity t = (LivingEntity)e;
            if (t.isAlive() && !(t instanceof ArmorStandEntity) && attackable(server, player, t)) {
               Vec3d to = new Vec3d(t.getX() - at.x, 0.0, t.getZ() - at.z);
               double d = to.length() - (double)t.getWidth() / 2.0;
               boolean locked = t.getId() == lockId;
               if (!(d > reach + (locked ? 0.8 : 0.0)) && !(t.getY() > at.y + 2.6) && !(t.getBoundingBox().maxY < at.y - 0.6)) {
                  double dot = to.lengthSquared() < 1.0E-4 ? 1.0 : to.normalize().dotProduct(fwd);
                  if (!(dot < arcCos) || locked && dot > -0.2) {
                     out.add(t);
                  }
               }
            }
         }
      }

      out.sort(Comparator.comparingDouble(t -> t.getId() == lockId ? -1.0 : t.squaredDistanceTo(player)));
      return out.size() > 3 ? out.subList(0, 3) : out;
   }

   static boolean attackable(MinecraftServer server, ServerPlayerEntity attacker, LivingEntity t) {
      if (t instanceof PlayerEntity tp) {
         if (!tp.isSpectator() && !tp.isCreative() && server.isPvpEnabled()) {
            return sameParty(server, attacker.getUuid(), tp.getUuid()) ? false : !BattleManager.isInBattle(tp.getUuid());
         } else {
            return false;
         }
      } else if (BattleManager.frozenEntityIds.contains(t.getId())) {
         return false;
      } else {
         if (t instanceof TameableEntity tame && tame.getOwnerUuid() != null && sameParty(server, attacker.getUuid(), tame.getOwnerUuid())) {
            return false;
         }

         return !(t instanceof BattleRecruitEntity) && !(t instanceof Shopkeeper.Entity);
      }
   }

   private static void knock(LivingEntity t, ServerPlayerEntity from, boolean fin) {
      Vec3d d = new Vec3d(t.getX() - from.getX(), 0.0, t.getZ() - from.getZ());
      if (d.lengthSquared() < 1.0E-4) {
         float yr = from.getYaw() * (float) (Math.PI / 180.0);
         d = new Vec3d((double)(-MathHelper.sin(yr)), 0.0, (double)MathHelper.cos(yr));
      }

      d = d.normalize();
      double k = fin ? 1.15 : 0.12;
      t.setVelocity(d.x * k, fin ? 0.28 : t.getVelocity().y, d.z * k);
      t.velocityModified = true;
   }

   public static void onVanillaHit(ServerPlayerEntity player, LivingEntity t) {
      if (!(t instanceof PlayerEntity)) {
         lm(t);
         aggro(t, player);
      }
   }

   private static void aggro(LivingEntity t, ServerPlayerEntity p) {
      if (t instanceof HumanEnemyEntity h) {
         h.provoke(p);
      }

      if (t instanceof MobEntity m && m.getTarget() == null) {
         m.setTarget(p);
      }
   }

   private static float bashBase(ServerPlayerEntity player, BattleData.PlayerProgress prog, CharStats eff, long now, LiveCombat.LP p) {
      double weapon = 4.0;
      ItemStack eq = prog.equippedWeapon;
      double eqDmg = attackOf(eq);
      double handDmg = attackOf(player.getMainHandStack());
      weapon = Math.max(weapon, Math.max(eqDmg, handDmg));
      WeaponStats ws = WeaponStats.fromStack(eq);
      if (ws != null) {
         weapon += (double)ws.bashBonus();
      }

      double d = weapon * (1.0 + (double)eff.offenseBonus());
      if (now < p.atkUntil) {
         d *= (double)p.atkMult;
      }

      if (prog.soulType == SoulType.JUSTICE && player.getHealth() < player.getMaxHealth() * 0.5F) {
         d *= 1.2;
      }

      return (float)d;
   }

   private static double attackOf(ItemStack s) {
      if (s != null && !s.isEmpty()) {
         AttributeModifiersComponent mods = (AttributeModifiersComponent)s.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
         if (mods == null) {
            return 0.0;
         } else {
            double v = 1.0;

            for (net.minecraft.component.type.AttributeModifiersComponent.Entry e : mods.modifiers()) {
               if (e.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && e.modifier().operation() == Operation.ADD_VALUE) {
                  v += e.modifier().value();
               }
            }

            return v;
         }
      } else {
         return 0.0;
      }
   }

   private static Affinity[] weaponElements(BattleData.PlayerProgress prog) {
      WeaponStats ws = WeaponStats.fromStack(prog.equippedWeapon);
      Affinity[] el = ws != null ? ws.getElements() : null;
      return el != null && el.length > 0 ? el : new Affinity[]{Affinity.SLASH};
   }

   private static float affinityMult(LivingEntity t, Affinity[] elems) {
      Affinity weak = MobLevelManager.getMobWeakness(t);
      Affinity resist = MobLevelManager.getMobResistance(t);
      float m = 1.0F;

      for (Affinity a : elems) {
         if (a != null) {
            if (a == weak) {
               m = Math.max(m, 1.5F);
            } else if (a == resist) {
               m = Math.min(m, 0.5F);
            }
         }
      }

      return m;
   }

   static void damageMob(MinecraftServer server, LivingEntity t, LiveCombat.LM m, float dmg, ServerPlayerEntity by, DamageSource src) {
      if (!(t instanceof SoulBodyEntity)) {
         if (t instanceof ServerPlayerEntity sp && SoulWalk.isOut(sp.getUuid())) {
            return;
         }

         float newHp = t.getHealth() - dmg;
         if (newHp <= 0.0F && m.lives > 0) {
            m.lives--;
            BattleManager.IS_ROLLING = true;

            try {
               t.setHealth(t.getMaxHealth());
            } finally {
               BattleManager.IS_ROLLING = false;
            }

            text(t, m.lives + (m.lives == 1 ? " LIFE LEFT" : " LIVES LEFT"), 16768324);
            if (t.getWorld() instanceof ServerWorld w) {
               w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, t.getX(), t.getBodyY(0.6), t.getZ(), 30, 0.4, 0.6, 0.4, 0.4);
            }

            sendMobInfo(t, m, null);
         } else if (newHp <= 0.0F) {
            kill(server, t, by, src);
         } else {
            BattleManager.IS_ROLLING = true;

            try {
               t.setHealth(newHp);
            } finally {
               BattleManager.IS_ROLLING = false;
            }

            if (t.getWorld() instanceof ServerWorld w) {
               w.sendEntityDamage(t, src);
            }
         }
      }
   }

   private static float lethal(LivingEntity t) {
      float hp = Float.isFinite(t.getHealth()) ? t.getHealth() : 0.0F;
      float ab = Float.isFinite(t.getAbsorptionAmount()) ? t.getAbsorptionAmount() : 0.0F;
      return Math.min(1000000.0F, (hp + ab + t.getMaxHealth()) * 50.0F + 100.0F);
   }

   public static void healNaN(MinecraftServer server) {
      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         if (!Float.isFinite(p.getAbsorptionAmount())) {
            p.setAbsorptionAmount(0.0F);
         }

         if (!Float.isFinite(p.getHealth())) {
            BattleManager.IS_ROLLING = true;

            try {
               p.setHealth(Math.max(1.0F, p.getMaxHealth() * 0.25F));
            } finally {
               BattleManager.IS_ROLLING = false;
            }

            TurnBattleMod.LOGGER.warn("[LiveCombat] Repaired NaN health for {}", p.getName().getString());
         }
      }
   }

   private static void kill(MinecraftServer server, LivingEntity t, ServerPlayerEntity by, DamageSource src) {
      LiveCombat.LM dying = mobs.get(t.getId());
      if (t.getWorld() instanceof ServerWorld fw && !(t instanceof PlayerEntity)) {
         long now = now(server);
         boolean ice = dying != null && (now < dying.freezeUntil || now - dying.iceHitAt <= 20L);
         MobFateNet.send(fw, t, ice ? 2 : 0);
      }

      mobs.remove(t.getId());
      skills.remove(t.getId());
      pending.removeIf(h -> h.mobId() == t.getId());
      float maxHp = t.getMaxHealth();
      APPLYING = true;

      boolean landed;
      try {
         landed = t.damage(src, lethal(t));
      } finally {
         APPLYING = false;
      }

      if (!landed && t.isAlive() && t.getHealth() > 0.0F) {
         APPLYING = true;

         try {
            t.kill();
         } finally {
            APPLYING = false;
         }
      }

      if (by != null) {
         rewards(server, t, by, maxHp, "win");
      }
   }

   private static void rewards(MinecraftServer server, LivingEntity t, ServerPlayerEntity by, float maxHp, String outcome) {
      UUID pid = by.getUuid();
      int exp = Math.max(2, (int)(maxHp * 0.5F * MobLevelManager.getMobExpMult(t)));
      int tier = EliteMobManager.getTier(t.getUuid());
      if (tier >= 0 && tier < EliteMobManager.EXP_MULT.length) {
         exp = Math.round((float)exp * EliteMobManager.EXP_MULT[tier]);
      }

      exp = RogueliteManager.scaleExp(server, pid, exp);
      RogueliteManager.onBattleWon(server, pid);
      BattleData data = BattleData.get(server);
      int levels = data.addExp(pid, exp);
      BattleData.PlayerProgress prog = data.getOrCreate(pid);
      if (levels > 0) {
         RecruitData rd = RecruitData.get(server);

         for (int i = 0; i < levels; i++) {
            rd.onPlayerLevelUp(pid);
         }

         rd.markDirty();
         MobLevelManager.applyPlayerStats(by, prog.level, prog.playerStats);
         by.setHealth(by.getMaxHealth());
      }

      data.markDirty();
      PacketByteBuf lv = PacketByteBufs.create();
      lv.writeInt(prog.level);
      lv.writeInt(prog.exp);
      lv.writeInt(BattleData.expNeeded(prog.level));
      lv.writeBoolean(levels > 0);
      Net121.sendToClient(by, TurnBattleMod.LEVEL_UPDATE, lv);
      text(t, "+" + exp + " EXP", 16777062);
      BestiaryData.get(server).recordDefeat(pid, t.getType());
      if ("win".equals(outcome) && t.getWorld() instanceof ServerWorld w) {
         HumanEnemies.onDefeated(t, w, by);
      }

      MoralityManager.onBattleEnd(by, outcome);
      if (tier >= 0) {
         EliteMobManager.grantLoot(server, List.of(pid), tier);
         EliteMobManager.remove(t.getUuid());
      }
   }

   public static Boolean onPlayerDamaged(ServerPlayerEntity player, DamageSource src, float amount) {
      if (APPLYING) {
         return null;
      } else {
         MinecraftServer server = player.getServer();
         if (server != null && isLive(player)) {
            Entity attacker = src.getAttacker();
            Entity direct = src.getSource();
            if (attacker == player) {
               return null;
            } else {
               long now = now(server);
               LiveCombat.LP p = lp(player.getUuid());
               if (attacker instanceof ServerPlayerEntity ap) {
                  if (sameParty(server, ap.getUuid(), player.getUuid()) || !server.isPvpEnabled()) {
                     return false;
                  } else if (isLive(ap)) {
                     return false;
                  } else {
                     BattleData.PlayerProgress aprog = BattleData.get(server).getOrCreate(ap.getUuid());
                     float raw = (float)(4.0 * (double)(1.0F + BattleManager.effectiveStatsOf(ap.getUuid(), aprog).offenseBonus())) * 0.6F;
                     hurtPlayer(player, ap, src, raw, true);
                     return false;
                  }
               } else if (src.isIn(DamageTypeTags.IS_EXPLOSION)) {
                  if (now >= p.iframesUntil && !(amount < 2.5F)) {
                     hurtPlayer(player, attacker, src, amount * 0.75F * (attacker instanceof PlayerEntity ? 1.0F : 0.4F), true);
                     return false;
                  } else {
                     graze(player, p);
                     return false;
                  }
               } else if (!(attacker instanceof LivingEntity mob)) {
                  return null;
               } else if (now < p.iframesUntil) {
                  graze(player, p);
                  return false;
               } else {
                  LiveCombat.LM m = lm(mob);
                  if (disabled(m, now)) {
                     return false;
                  } else {
                     boolean melee = direct == attacker && !(direct instanceof ProjectileEntity);
                     if (melee && skills.containsKey(mob.getId())) {
                        return false;
                     } else if (melee) {
                        for (LiveCombat.Pending h : pending) {
                           if (h.mobId() == mob.getId() && h.player().equals(player.getUuid())) {
                              return false;
                           }
                        }

                        pending.add(new LiveCombat.Pending(mob.getId(), player.getUuid(), now + 10L));
                        if (mob.getWorld() instanceof ServerWorld w) {
                           fx(w, mob.getPos(), b -> {
                              b.writeByte(7);
                              b.writeInt(mob.getId());
                              b.writeByte(10);
                           });
                        }

                        return false;
                     } else {
                        int lvl = BattleData.get(server).getOrCreate(player.getUuid()).level;
                        float raw = BattleManager.getEnemyDamage(mob, lvl)
                           * 0.64000005F
                           * mobAtk(m, now)
                           * RogueliteManager.getRunDifficultyScale(server, player.getUuid());
                        hurtPlayer(player, mob, src, raw, true);
                        return false;
                     }
                  }
               }
            }
         } else {
            return null;
         }
      }
   }

   private static boolean disabled(LiveCombat.LM m, long now) {
      return now < m.stunUntil || now < m.freezeUntil || now < m.sleepUntil || now < m.confuseUntil;
   }

   static int hurtPlayer(ServerPlayerEntity target, Entity attacker, DamageSource src, float raw, boolean blockable) {
      int guard = guardCheck(target, attacker, blockable);
      if (guard < 0) {
         return -1;
      } else {
         applyHurt(target, attacker, src, raw, guard);
         return guard;
      }
   }

   static int guardCheck(ServerPlayerEntity target, Entity attacker, boolean blockable) {
      MinecraftServer server = target.getServer();
      if (server == null || !target.isAlive()) {
         return -1;
      } else if (RogueliteManager.isPickingCard(target)) {
         return -1;
      } else {
         long now = now(server);
         LiveCombat.LP p = lp(target.getUuid());
         if (now < p.iframesUntil) {
            graze(target, p);
            return -1;
         } else if (blockable && p.blocking) {
            if (now - p.blockStart <= 6L) {
               addTp(target, p, 16.0F);
               text(target, "PERFECT GUARD!", 8972543);
               patienceHeal(server, target);
               if (attacker instanceof LivingEntity le && le != target && le.isAlive()) {
                  guardStun(server, le, target, now);
               }

               return 2;
            } else {
               addTp(target, p, 5.0F);
               patienceHeal(server, target);
               return 1;
            }
         } else {
            return 0;
         }
      }
   }

   private static void patienceHeal(MinecraftServer server, ServerPlayerEntity target) {
      BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(target.getUuid());
      if (prog.soulType == SoulType.PATIENCE && !(target.getHealth() >= target.getMaxHealth())) {
         heal(target, target.getMaxHealth() * 0.05F);
      }
   }

   static void applyHurt(ServerPlayerEntity target, Entity attacker, DamageSource src, float raw, int flags) {
      MinecraftServer server = target.getServer();
      if (server != null && target.isAlive()) {
         long now = now(server);
         LiveCombat.LP p = lp(target.getUuid());
         BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(target.getUuid());
         CharStats eff = BattleManager.effectiveStatsOf(target.getUuid(), prog);
         float dmg = Math.max(1.0F, eff.afterDefense(raw));
         if (prog.soulType == SoulType.INTEGRITY) {
            dmg *= 0.85F;
         }

         if (now < p.shieldUntil) {
            dmg *= 1.0F - p.shield;
         }

         if (flags == 2) {
            dmg = 0.0F;
         } else if (flags == 1) {
            dmg *= 0.35F;
         }

         if (target.isCreative()) {
            dmg = 0.0F;
         }

         float shown = dmg;
         fx(target.getServerWorld(), target.getPos(), b -> {
            b.writeByte(2);
            b.writeInt(target.getId());
            b.writeFloat(shown);
            b.writeByte(flags);
         });
         if (!(dmg <= 0.0F)) {
            if (attacker != null && attacker != target) {
               Vec3d d = new Vec3d(target.getX() - attacker.getX(), 0.0, target.getZ() - attacker.getZ());
               if (d.lengthSquared() > 1.0E-4) {
                  d = d.normalize().multiply(flags == 1 ? 0.18 : 0.42);
                  target.setVelocity(target.getVelocity().add(d.x, flags == 1 ? 0.0 : 0.18, d.z));
                  target.velocityModified = true;
               }
            }

            if (!Float.isFinite(target.getAbsorptionAmount())) {
               target.setAbsorptionAmount(0.0F);
            }

            float hp = target.getHealth() - dmg;
            if (hp <= 3.0F && prog.soulType == SoulType.DETERMINATION && !prog.determinationUsed) {
               prog.determinationUsed = true;
               BattleData.get(server).markDirty();
               hp = target.getMaxHealth();
               text(target, "DETERMINATION!", 16724787);
            } else if (hp <= 0.0F && prog.soulType == SoulType.PERSEVERANCE && RNG.nextBoolean()) {
               hp = Math.min(10.0F, target.getMaxHealth());
               text(target, "PERSEVERANCE!", 11141375);
            }

            if (hp <= 0.0F) {
               APPLYING = true;

               try {
                  boolean landed = target.damage(src, lethal(target));
                  if (!landed && target.isAlive() && target.getHealth() > 0.0F) {
                     target.kill();
                  }
               } finally {
                  APPLYING = false;
               }
            } else {
               BattleManager.IS_ROLLING = true;

               try {
                  target.setHealth(hp);
               } finally {
                  BattleManager.IS_ROLLING = false;
               }

               target.getServerWorld().sendEntityDamage(target, src);
            }
         }
      }
   }

   private static void graze(ServerPlayerEntity player, LiveCombat.LP p) {
      BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(player.getUuid());
      float tp = 2.0F + BattleManager.effectiveStatsOf(player.getUuid(), prog).grazeTP();
      addTp(player, p, tp);
      fx(player.getServerWorld(), player.getPos(), b -> {
         b.writeByte(3);
         b.writeInt(player.getId());
         b.writeFloat(tp);
      });
   }

   private static void castSkill(MinecraftServer server, ServerPlayerEntity player, LiveCombat.LP p, int code, int targetId, long now) {
      if (now >= p.skillReady) {
         BattleSkill skill = LiveSkills.decode(code);
         if (skill != null) {
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
            if (LiveSkills.forPlayer(server, player.getUuid(), prog).contains(skill)) {
               if (!prog.rogueRun && skill.getLevelRequired() > prog.level) {
                  player.sendMessage(Text.literal("Reach LV " + skill.getLevelRequired() + " to use that.").formatted(Formatting.GRAY), true);
               } else {
                  int cost = skill.getTpCost();
                  if (prog.equippedAccessory != null && OffhandGimmick.fromStack(prog.equippedAccessory) == OffhandGimmick.THORN_RING) {
                     cost = (int)Math.ceil((double)((float)cost * 0.5F));
                  }

                  ServerWorld w = player.getServerWorld();
                  boolean support = LiveSkills.isSupport(skill) || skill.getType() == SkillType.MAGNET;
                  Entity te = targetId >= 0 ? w.getEntityById(targetId) : null;
                  double reach = support ? 30.0 : 28.0;
                  if (te != null && te.squaredDistanceTo(player) > reach * reach) {
                     te = null;
                  }

                  LivingEntity var10000;
                  label155: {
                     if (te instanceof LivingEntity le && le.isAlive()) {
                        var10000 = le;
                        break label155;
                     }

                     var10000 = null;
                  }

                  LivingEntity target = var10000;
                  if (!support && target == null) {
                     player.sendMessage(Text.literal("Lock on to something first.").formatted(Formatting.GRAY), true);
                  } else if (p.tp < (float)cost) {
                     player.sendMessage(Text.literal("Not enough TP!").formatted(Formatting.RED), true);
                  } else {
                     p.tp -= (float)cost;
                     p.skillReady = now + 12L;
                     CharStats eff = BattleManager.effectiveStatsOf(player.getUuid(), prog);
                     float scale = 1.0F + (float)SkillScaling.statOf(eff, skill) * 0.017F;
                     if (skill instanceof MonsterSkill && prog.playerRace == CharacterRace.MONSTER && prog.absorbedSouls != null) {
                        scale *= MonsterSkill.soulPowerMult(prog.absorbedSouls.size());
                     }

                     label146:
                     if (support) {
                        if (target instanceof ServerPlayerEntity tp && (tp == player || sameParty(server, player.getUuid(), tp.getUuid()))) {
                           var10000 = tp;
                           break label146;
                        }

                        var10000 = player;
                     } else {
                        var10000 = target;
                     }

                     LivingEntity fxTarget = var10000;
                     int fxId = fxTarget != null ? fxTarget.getId() : player.getId();
                     fx(w, player.getPos(), b -> {
                        b.writeByte(4);
                        b.writeInt(player.getId());
                        b.writeInt(fxId);
                        b.writeInt(code);
                     });
                     SkillBonus.magicFlat = SkillBonus.forSkill(WeaponStats.fromStack(prog.equippedWeapon), skill);

                     int raw;
                     try {
                        raw = skill.rollScaled(RNG);
                     } finally {
                        SkillBonus.magicFlat = 0.0F;
                     }

                     float var21 = scale;
                     UUID var24 = player.getUuid();
                     later(server, SkillTiming.at(skill, 0.8F), () -> {
                        ServerPlayerEntity pl = server.getPlayerManager().getPlayer(var24);
                        if (pl != null && pl.isAlive() && isLive(pl)) {
                           LivingEntity t2 = target != null && target.isAlive() && !target.isRemoved() ? target : null;
                           LivingEntity f2 = fxTarget != null && fxTarget.isAlive() && !fxTarget.isRemoved() ? fxTarget : null;
                           landSkill(server, pl, lp(var24), BattleData.get(server).getOrCreate(var24), skill, t2, f2, raw, var21, now(server));
                        }
                     });
                  }
               }
            }
         }
      }
   }

   private static void landSkill(
      MinecraftServer server,
      ServerPlayerEntity player,
      LiveCombat.LP p,
      BattleData.PlayerProgress prog,
      BattleSkill skill,
      LivingEntity target,
      LivingEntity fxTarget,
      int raw,
      float scale,
      long now
   ) {
      switch (skill.getType()) {
         case HEAL:
            if (skill != LightnerSkill.DUAL_HEAL && skill != OriSkill.NIGHTBLOOM) {
               LivingEntity who = (LivingEntity)(fxTarget != null ? fxTarget : player);
               float heal = raw == 999 ? who.getMaxHealth() : (float)raw * scale;
               if (prog.soulType == SoulType.KINDNESS) {
                  heal *= 1.25F;
               }

               heal(who, heal);
            } else {
               float heal = (float)raw * scale;
               if (prog.soulType == SoulType.KINDNESS) {
                  heal *= 1.25F;
               }

               for (ServerPlayerEntity m : player.getServerWorld().getPlayers()) {
                  if (m == player
                     || sameParty(server, player.getUuid(), m.getUuid())
                        && !(m.squaredDistanceTo(player) > 256.0)
                        && m.isAlive()
                        && !SoulWalk.isOut(m.getUuid())) {
                     heal(m, heal);
                  }
               }
            }
            break;
         case SHIELD: {
            ServerPlayerEntity on = fxTarget instanceof ServerPlayerEntity sp ? sp : player;
            LiveCombat.LP q = on == player ? p : lp(on.getUuid());
            q.shield = Math.min(0.9F, (float)skill.getMinVal() / 100.0F);
            q.shieldUntil = now + (long)Math.max(1, skill.getMaxVal()) * 60L;
            text(on, "SHIELD", 8947967);
            break;
         }
         case BUFF: {
            ServerPlayerEntity on = fxTarget instanceof ServerPlayerEntity sp ? sp : player;
            LiveCombat.LP q = on == player ? p : lp(on.getUuid());
            q.atkMult = 1.0F + (float)Math.max(10, skill.getExtra()) / 100.0F;
            q.atkUntil = now + (long)Math.max(1, skill.getMinVal()) * 60L;
            text(on, "ATK UP", 16768324);
            break;
         }
         case MAGNET:
            addTp(player, p, (float)raw * scale);
            text(player, "+" + (int)((float)raw * scale) + " TP", 16750899);
            break;
         case SPY:
            if (target != null && !(target instanceof PlayerEntity)) {
               Affinity wk = MobLevelManager.getMobWeakness(target);
               Affinity rs = MobLevelManager.getMobResistance(target);
               player.sendMessage(
                  Text.literal(
                        "* "
                           + target.getDisplayName().getString()
                           + " - weak to "
                           + (wk != null ? wk.name() : "nothing")
                           + ", resists "
                           + (rs != null ? rs.name() : "nothing")
                           + "."
                     )
                     .formatted(Formatting.YELLOW),
                  false
               );
            }
            break;
         case PACIFY:
            if (target != null && !(target instanceof PlayerEntity)) {
               LiveCombat.LM m = lm(target);
               boolean needsSleep = skill == LightnerSkill.PACIFY || skill == LightnerSkill.C_ACTION;
               int ex = skill.getExtra();
               int thresholdPct = skill == HumanMageSkill.ACT_TALK
                  ? 40
                  : (skill == HumanMageSkill.ACT_PLEAD ? 65 : (skill == LightnerSkill.PACIFY ? 50 : (skill == LightnerSkill.C_ACTION ? 75 : 65)));
               boolean works = ex == 0 || ex == 1 || skill == HumanMageSkill.ACT_PLEAD && RNG.nextInt(100) < ex;
               float frac = target.getMaxHealth() > 0.0F ? target.getHealth() / target.getMaxHealth() : 1.0F;
               String n = target.getDisplayName().getString();
               if (needsSleep && now >= m.sleepUntil) {
                  player.sendMessage(
                     Text.literal("* " + skill.getDisplayName() + " had no effect. " + n + " must be asleep first!").formatted(Formatting.GRAY), true
                  );
                  text(target, "NO EFFECT", 11184810);
               } else if (!works) {
                  player.sendMessage(Text.literal("* " + n + " wouldn't listen...").formatted(Formatting.GRAY), true);
                  text(target, "NO EFFECT", 11184810);
               } else if (frac > (float)thresholdPct / 100.0F) {
                  player.sendMessage(
                     Text.literal("* " + skill.getDisplayName() + " had no effect. Get " + n + " to " + thresholdPct + "% HP or less.")
                        .formatted(Formatting.GRAY),
                     true
                  );
                  text(target, "NO EFFECT", 11184810);
               } else {
                  m.spareable = true;
                  sendMobInfo(target, m, null);
                  text(target, "SPAREABLE", 16777062);
                  if (ex == 1) {
                     addTp(player, p, (float)raw * scale);
                  }
               }
            }
            break;
         case RAISE_THRESHOLD:
            if (target != null && !(target instanceof PlayerEntity)) {
               LiveCombat.LM m = lm(target);
               m.mercy = Math.min(1.0F, m.mercy + (float)skill.getExtra() / 100.0F);
               sendMobInfo(target, m, null);
            }
            break;
         case SPARE:
            if (target != null) {
               mercy(server, player, target.getId());
            }
            break;
         default:
            offensiveSkill(server, player, p, prog, skill, target, (float)raw * scale, now);
      }
   }

   private static void offensiveSkill(
      MinecraftServer server,
      ServerPlayerEntity player,
      LiveCombat.LP p,
      BattleData.PlayerProgress prog,
      BattleSkill skill,
      LivingEntity target,
      float amount,
      long now
   ) {
      if (target != null) {
         SkillType type = skill.getType();
         List targets = new ArrayList<>();
         if (type == SkillType.AOE) {
            for (Entity e : player.getServerWorld().getOtherEntities(player, target.getBoundingBox().expand(5.0))) {
               if (e instanceof LivingEntity) {
                  LivingEntity le = (LivingEntity)e;
                  if (le.isAlive() && attackable(server, player, le) && (le == target || !(le instanceof PlayerEntity))) {
                     targets.add(le);
                  }
               }
            }
         } else if (attackable(server, player, target)) {
            targets.add(target);
         }

         int hits = type == SkillType.MULTI ? Math.max(2, skill.getExtra()) : 1;

         boolean damaging = switch (type) {
            case DAMAGE, BYPASS, FREEZE, BURN, MULTI, AOE -> true;
            default -> false;
         };

         for (LivingEntity t : targets) {
            if (t instanceof ServerPlayerEntity) {
               ServerPlayerEntity tp = (ServerPlayerEntity)t;
               if (damaging) {
                  hurtPlayer(tp, player, player.getDamageSources().indirectMagic(player, player), amount * (float)hits * 0.6F, true);
               }
            } else {
               LiveCombat.LM m = lm(t);
               aggro(t, player);
               if (MobFateNet.isIce(skill)) {
                  m.iceHitAt = now;
               }

               if (damaging) {
                  float aff = affinityMult(t, skill.getAffinities());
                  float armor = (float)t.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
                  float d = amount
                     * aff
                     * MobLevelManager.getMobDamageTaken(t)
                     * (now < m.weakUntil ? m.weakMult : 1.0F)
                     * (type == SkillType.BYPASS ? 1.0F : Math.max(0.55F, 1.0F - armor * 0.04F))
                     / RogueliteManager.getRunDifficultyScale(server, player.getUuid());
                  if (type == SkillType.AOE && t != target) {
                     d *= 0.75F;
                  }

                  for (int i = 0; i < hits && t.isAlive(); i++) {
                     float dd = Math.max(1.0F, d);
                     int flags = (aff > 1.01F ? 2 : 0) | (aff < 0.99F ? 4 : 0);
                     fx(player.getServerWorld(), t.getPos(), b -> {
                        b.writeByte(0);
                        b.writeInt(-1);
                        b.writeInt(t.getId());
                        b.writeFloat(dd);
                        b.writeByte(flags);
                        b.writeInt(16777215);
                     });
                     damageMob(server, t, m, dd, player, player.getDamageSources().indirectMagic(player, player));
                  }

                  m.sleepUntil = 0L;
               }

               if (t.isAlive()) {
                  switch (type) {
                     case FREEZE:
                        m.freezeUntil = now + (long)Math.max(1, skill.getExtra()) * 40L;
                        text(t, "FROZEN", 8974079);
                        break;
                     case BURN:
                        if (RNG.nextInt(100) < Math.max(25, skill.getExtra())) {
                           m.burnUntil = now + 100L;
                           m.nextBurn = now + 20L;
                           m.burnDmg = Math.max(1.0F, amount * 0.15F);
                           m.burnBy = player.getUuid();
                           text(t, "BURN", 16737826);
                        }
                     case MULTI:
                     case AOE:
                     default:
                        break;
                     case SLEEP:
                        if (RNG.nextInt(100) < Math.max(20, skill.getExtra())) {
                           m.sleepUntil = now + (long)Math.max(2, skill.getMinVal()) * 60L;
                           text(t, "ZZZ", 11197951);
                        } else {
                           text(t, "MISS", 11184810);
                        }
                        break;
                     case CONFUSE:
                        m.confuseUntil = now + (long)Math.max(2, skill.getMinVal()) * 50L;
                        text(t, "CONFUSED", 16755268);
                        break;
                     case WEAKEN:
                        m.weakMult = 1.35F;
                        m.weakUntil = now + (long)Math.max(2, skill.getMinVal()) * 60L;
                        text(t, "DEF DOWN", 16746564);
                  }

                  sendMobInfo(t, m, null);
               }
            }
         }
      }
   }

   private static void heal(LivingEntity who, float amount) {
      float before = who.getHealth();
      float after = Math.min(who.getMaxHealth(), before + amount);
      BattleManager.IS_ROLLING = true;

      try {
         who.setHealth(after);
      } finally {
         BattleManager.IS_ROLLING = false;
      }

      float var4 = after - before;
      if (who.getWorld() instanceof ServerWorld w) {
         fx(w, who.getPos(), b -> {
            b.writeByte(5);
            b.writeInt(who.getId());
            b.writeFloat(var4);
         });
      }
   }

   private static void act(MinecraftServer server, ServerPlayerEntity player, LiveCombat.LP p, int index, int targetId, long now) {
      if (now >= p.skillReady) {
         Entity te = targetId >= 0 ? player.getServerWorld().getEntityById(targetId) : null;
         if (te instanceof LivingEntity t && !(t instanceof PlayerEntity) && t.isAlive() && !(te.squaredDistanceTo(player) > 784.0)) {
            List acts = actsFor(t);
            if (index >= 0 && index < acts.size()) {
               ActDefs.ActEntry a = acts.get(index);
               boolean check = "__CHECK__".equals(a.logText());
               if (!check && now < p.actReady.getOrDefault(t.getId(), 0L)) {
                  player.sendMessage(Text.literal("* You can't do that again just yet.").formatted(Formatting.GRAY), true);
                  return;
               }

               p.skillReady = now + 16L;
               if (!check) {
                  p.actReady.put(t.getId(), now + 100L);
                  if (p.actReady.size() > 32) {
                     p.actReady.values().removeIf(v -> v < now);
                  }
               }

               LiveCombat.LM m = lm(t);
               pose(player, 7);
               String name = t.getDisplayName().getString();
               if (check) {
                  Affinity wk = MobLevelManager.getMobWeakness(t);
                  player.sendMessage(
                     Text.literal(
                           "* "
                              + name
                              + " - ATK "
                              + Math.round(BattleManager.getEnemyDamage(t, 1))
                              + " DEF "
                              + Math.round(t.getAttributeValue(EntityAttributes.GENERIC_ARMOR))
                              + (wk != null ? " - weak to " + wk.name() : "")
                        )
                        .formatted(Formatting.YELLOW),
                     false
                  );
                  return;
               }

               if (a.sleepCheck() && now >= m.sleepUntil) {
                  player.sendMessage(Text.literal("* " + a.sleepLog()).formatted(Formatting.GRAY), true);
                  return;
               }

               m.mercy = Math.min(1.0F, m.mercy + (float)a.mercyPct() / 100.0F);
               if (a.confuseTurns() > 0) {
                  m.confuseUntil = now + (long)a.confuseTurns() * 50L;
               }

               if (a.buffDef() > 0) {
                  m.weakMult = 1.25F;
                  m.weakUntil = now + 300L;
               }

               if (a.shieldTurns() > 0) {
                  p.shield = 0.5F;
                  p.shieldUntil = now + (long)a.shieldTurns() * 60L;
               }

               String log = a.logText() != null && !a.logText().startsWith("__") ? a.logText() : "You " + a.logName() + ".";
               player.sendMessage(Text.literal("* " + log.replace("{name}", name).replace("%s", name)).formatted(Formatting.WHITE), true);
               if (a.mercyPct() > 0) {
                  text(t, "+" + a.mercyPct() + "% MERCY", 16777062);
               }

               sendMobInfo(t, m, null);
               return;
            }

            return;
         }
      }
   }

   public static List<ActDefs.ActEntry> actsFor(LivingEntity t) {
      String key = EntityType.getId(t.getType()).toString();
      List out = new ArrayList<>();

      for (ActDefs.ActEntry a : ActDefs.getActs(key)) {
         if (a.participants() <= 1 && !"__SHINE__".equals(a.logText())) {
            out.add(a);
         }
      }

      return out;
   }

   private static void useItem(MinecraftServer server, ServerPlayerEntity player, LiveCombat.LP p, int slot, int targetId, long now) {
      if (now >= p.itemReady && slot >= 0 && slot < player.getInventory().size()) {
         ItemStack stack = player.getInventory().getStack(slot);
         FoodComponent food = stack.isEmpty() ? null : (FoodComponent)stack.get(DataComponentTypes.FOOD);
         if (food != null) {
            ServerPlayerEntity var10000;
            label40: {
               p.itemReady = now + 20L;
               if ((targetId >= 0 ? player.getServerWorld().getEntityById(targetId) : null) instanceof ServerPlayerEntity tp
                  && tp.isAlive()
                  && tp.squaredDistanceTo(player) <= 900.0
                  && sameParty(server, player.getUuid(), tp.getUuid())) {
                  var10000 = tp;
                  break label40;
               }

               var10000 = player;
            }

            LivingEntity who = var10000;
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
            float heal = ((float)food.nutrition() * 2.0F + food.saturation() * 2.0F) * 2.0F;
            if (prog.soulType == SoulType.KINDNESS) {
               heal *= 1.25F;
            }

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (who == player && prog.favoriteFood.contains(itemId)) {
               heal *= 2.0F;
            }

            stack.decrement(1);
            pose(player, 6);
            heal(who, heal);
         }
      }
   }

   private static void mercy(MinecraftServer server, ServerPlayerEntity player, int targetId) {
      if (!MoralityManager.isRuthless(BattleData.get(server).getOrCreate(player.getUuid()))) {
         Entity te = targetId >= 0 ? player.getServerWorld().getEntityById(targetId) : null;
         if (te instanceof LivingEntity t && !(t instanceof PlayerEntity) && t.isAlive() && !(te.squaredDistanceTo(player) > 784.0)) {
            LiveCombat.LM m = lm(t);
            if (!m.spareable && m.mercy < 1.0F) {
               player.sendMessage(Text.literal("* " + t.getDisplayName().getString() + " isn't ready to be spared...").formatted(Formatting.GRAY), true);
               return;
            }

            pose(player, 8);
            fx(player.getServerWorld(), t.getPos(), b -> {
               b.writeByte(6);
               b.writeInt(t.getId());
            });
            MobFateNet.send(player.getServerWorld(), t, 1);
            float maxHp = t.getMaxHealth();
            mobs.remove(t.getId());
            skills.remove(t.getId());
            pending.removeIf(h -> h.mobId() == t.getId());
            t.discard();
            rewards(server, t, player, maxHp, "mercy");
            player.sendMessage(Text.literal("* You spared " + t.getDisplayName().getString() + ".").formatted(Formatting.YELLOW), true);
            return;
         }
      }
   }

   public static void tick(MinecraftServer server) {
      long now = now(server);
      resolvePending(server, now);
      tickMobs(server, now);
      tickSkills(server, now);
      tickBashes(server, now);
      if (!later.isEmpty()) {
         List due = new ArrayList<>();
         later.removeIf(l -> {
            if (now >= l.due()) {
               due.add(l);
               return true;
            } else {
               return false;
            }
         });

         for (LiveCombat.Later l : due) {
            try {
               l.action().run();
            } catch (Exception var8) {
               TurnBattleMod.LOGGER.warn("Live Combat: a delayed skill effect failed", var8);
            }
         }
      }

      if (now % 2L == 0L) {
         scanProjectiles(server);
      }

      if (now % 20L == 0L) {
         rollSkills(server, now);
      }

      for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
         LiveCombat.LP p = players.get(pl.getUuid());
         if (p != null && p.queuedSwings > 0 && isLive(pl)) {
            if (now - p.queuedAt > 40L) {
               p.queuedSwings = 0;
            } else if (now >= p.comboReadyAt && (p.step < 0 || p.step >= 4 || now - p.lastAtk >= 8L)) {
               p.queuedSwings--;

               try {
                  attack(server, pl, p, p.queuedLock, now);
               } catch (Exception var7) {
                  p.queuedSwings = 0;
               }
            }
         }

         if (p != null && Math.abs(p.tp - p.sentTp) > 0.01F) {
            sendState(pl);
         }

         if (p != null) {
            if (!isLive(pl)) {
               p.lockId = -1;
            }

            sendArmed(pl, p);
            if (now % 40L == 0L && p.armedSent == 1) {
               broadcastArmed(pl, true);
            }
         }
      }

      if (now % 200L == 0L) {
         grazedProjectiles.clear();
      }
   }

   private static void resolvePending(MinecraftServer server, long now) {
      if (!pending.isEmpty()) {
         List due = new ArrayList<>();
         Iterator it = pending.iterator();

         while (it.hasNext()) {
            LiveCombat.Pending h = it.next();
            if (now >= h.due()) {
               due.add(h);
               it.remove();
            }
         }

         for (LiveCombat.Pending h : due) {
            ServerPlayerEntity pl = server.getPlayerManager().getPlayer(h.player());
            if (pl != null && pl.isAlive() && isLive(pl)) {
               Entity e = pl.getServerWorld().getEntityById(h.mobId());
               if (e instanceof LivingEntity) {
                  LivingEntity mob = (LivingEntity)e;
                  if (mob.isAlive()) {
                     LiveCombat.LM m = lm(mob);
                     if (!disabled(m, now) && !skills.containsKey(mob.getId())) {
                        double reach = (double)(mob.getWidth() + pl.getWidth()) / 2.0 + 1.6;
                        double dx = mob.getX() - pl.getX();
                        double dz = mob.getZ() - pl.getZ();
                        double d = Math.sqrt(dx * dx + dz * dz);
                        boolean vert = mob.getBoundingBox().maxY > pl.getY() - 0.3 && mob.getY() < pl.getBoundingBox().maxY + 0.3;
                        LiveCombat.LP p = lp(pl.getUuid());
                        if (d <= reach && vert) {
                           int lvl = BattleData.get(server).getOrCreate(pl.getUuid()).level;
                           float raw = BattleManager.getEnemyDamage(mob, lvl)
                              * 0.8F
                              * mobAtk(m, now)
                              * RogueliteManager.getRunDifficultyScale(server, pl.getUuid());
                           hurtPlayer(pl, mob, pl.getDamageSources().mobAttack(mob), raw, true);
                           if (mob instanceof MobEntity me) {
                              me.swingHand(Hand.MAIN_HAND);
                           }
                        } else if (d <= reach + 1.8) {
                           graze(pl, p);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void tickMobs(MinecraftServer server, long now) {
      if (!mobs.isEmpty()) {
         for (Entry en : new ArrayList<>(mobs.entrySet())) {
            LiveCombat.LM m = en.getValue();
            LivingEntity e = findLiving(server, en.getKey());
            if (e != null && e.isAlive()) {
               boolean held = disabled(m, now);
               LiveCombat.MobSkill sk = skills.get(en.getKey());
               if (held && e instanceof MobEntity) {
                  MobEntity me = (MobEntity)e;
                  if (sk == null || !sk.lunging) {
                     me.getNavigation().stop();
                     if (now >= m.slideUntil) {
                        Vec3d v = me.getVelocity();
                        me.setVelocity(v.x * 0.5, v.y, v.z * 0.5);
                     }
                  }
               }

               if (now < m.burnUntil && now >= m.nextBurn) {
                  m.nextBurn = now + 20L;
                  float bd = m.burnDmg;
                  World var11 = e.getWorld();
                  if (var11 instanceof ServerWorld) {
                     ServerWorld w = (ServerWorld)var11;
                     w.spawnParticles(ParticleTypes.FLAME, e.getX(), e.getBodyY(0.6), e.getZ(), 6, 0.25, 0.4, 0.25, 0.01);
                     fx(w, e.getPos(), b -> {
                        b.writeByte(0);
                        b.writeInt(-1);
                        b.writeInt(e.getId());
                        b.writeFloat(bd);
                        b.writeByte(0);
                        b.writeInt(16737826);
                     });
                     ServerPlayerEntity by = m.burnBy != null ? server.getPlayerManager().getPlayer(m.burnBy) : null;
                     damageMob(server, e, m, bd, by, by != null ? w.getDamageSources().indirectMagic(by, by) : w.getDamageSources().onFire());
                     if (!e.isAlive()) {
                        mobs.remove(en.getKey());
                        continue;
                     }
                  }
               }

               int mask = statusMask(m, now);
               if (mask != m.sentMask) {
                  sendMobInfo(e, m, null);
               }
            } else {
               mobs.remove(en.getKey());
            }
         }
      }
   }

   private static LivingEntity findLiving(MinecraftServer server, int id) {
      for (ServerWorld w : server.getWorlds()) {
         Entity e = w.getEntityById(id);
         if (e instanceof LivingEntity) {
            return (LivingEntity)e;
         }
      }

      return null;
   }

   private static float mobAtk(LiveCombat.LM m, long now) {
      return now < m.atkUntil ? m.atkMult : 1.0F;
   }

   private static void rollSkills(MinecraftServer server, long now) {
      for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
         if (isLive(pl) && pl.isAlive() && !pl.isCreative()) {
            ServerWorld w = pl.getServerWorld();

            for (MobEntity mob : w.getEntitiesByClass(MobEntity.class, pl.getBoundingBox().expand(16.0), m -> m.getTarget() == pl && m instanceof Monster)) {
               if (!skills.containsKey(mob.getId()) && !BattleManager.frozenEntityIds.contains(mob.getId())) {
                  LiveCombat.LM m = lm(mob);
                  if (now >= m.nextSkillAt && !disabled(m, now)) {
                     double d2 = mob.squaredDistanceTo(pl);
                     if (!(d2 < 9.0) && !(d2 > 196.0) && mob.canSee(pl) && !(RNG.nextFloat() >= 0.035F)) {
                        startSkill(server, mob, m, pl, now);
                     }
                  }
               }
            }
         }
      }
   }

   private static void startSkill(MinecraftServer server, MobEntity mob, LiveCombat.LM m, ServerPlayerEntity pl, long now) {
      String key = EntityType.getId(mob.getType()).toString();
      MobAttack atk = MobAttackRegistry.pickAttack(key, RNG);
      if (atk != null) {
         LiveCombat.MobSkill s = new LiveCombat.MobSkill();
         s.mobId = mob.getId();
         s.target = pl.getUuid();
         s.atk = atk;
         s.color = atk.colorOverride != 0 ? atk.colorOverride & 16777215 : 16777215;
         s.selfCast = atk.damageMult <= 0.0F
            && (atk.special == MobAttack.Special.BUFF || atk.special == MobAttack.Special.HEAL_MOB || atk.special == MobAttack.Special.CURE_MOB);
         int lvl = BattleData.get(server).getOrCreate(pl.getUuid()).level;
         float mult = atk.damageMult * (atk.targeting == MobAttack.Targeting.MULTI ? 1.3F : 1.0F);
         s.dmg = BattleManager.getEnemyDamage(mob, lvl) * 0.8F * mult * mobAtk(m, now) * RogueliteManager.getRunDifficultyScale(server, pl.getUuid());
         if (!s.selfCast) {
            s.dmg = Math.max(1.0F, s.dmg);
         }

         int prep = s.selfCast ? 14 : 22;
         s.lungeAt = now + (long)prep;
         m.nextSkillAt = now + 160L + (long)RNG.nextInt(80);
         skills.put(s.mobId, s);
         pending.removeIf(h -> h.mobId() == mob.getId());
         mob.getNavigation().stop();
         int col = s.color;
         fx((ServerWorld)mob.getWorld(), mob.getPos(), b -> {
            b.writeByte(10);
            b.writeInt(mob.getId());
            b.writeString(atk.name);
            b.writeInt(col);
            b.writeByte(prep);
         });
      }
   }

   private static void endPrepare(LivingEntity mob) {
      if (mob.getWorld() instanceof ServerWorld w) {
         fx(w, mob.getPos(), b -> {
            b.writeByte(10);
            b.writeInt(mob.getId());
            b.writeString("");
            b.writeInt(0);
            b.writeByte(0);
         });
      }
   }

   private static void skillFx(Entity on, LiveCombat.MobSkill s) {
      if (on.getWorld() instanceof ServerWorld w) {
         int var5 = s.atk.toSkillType().ordinal();
         int var4 = s.color;
         fx(w, on.getPos(), b -> {
            b.writeByte(12);
            b.writeInt(on.getId());
            b.writeByte(var5);
            b.writeInt(var4);
         });
      }
   }

   private static void tickSkills(MinecraftServer server, long now) {
      if (!skills.isEmpty()) {
         for (LiveCombat.MobSkill s : new ArrayList<>(skills.values())) {
            if (findLiving(server, s.mobId) instanceof MobEntity mob && mob.isAlive()) {
               LiveCombat.LM m = lm(mob);
               ServerWorld w = (ServerWorld)mob.getWorld();
               if (!s.lunging) {
                  if (disabled(m, now)) {
                     skills.remove(s.mobId);
                     endPrepare(mob);
                     text(mob, "INTERRUPTED", 11184810);
                  } else {
                     ServerPlayerEntity tp = server.getPlayerManager().getPlayer(s.target);
                     mob.getNavigation().stop();
                     Vec3d v = mob.getVelocity();
                     mob.setVelocity(v.x * 0.3, v.y, v.z * 0.3);
                     if (tp != null) {
                        mob.getLookControl().lookAt(tp, 30.0F, 30.0F);
                     }

                     if (now >= s.lungeAt) {
                        if (s.selfCast) {
                           skills.remove(s.mobId);
                           skillFx(mob, s);
                           MobAttack.Special sp = s.atk.special;
                           later(server, SkillTiming.mobAt(s.atk.toSkillType(), 0.8F), () -> {
                              if (mob.isAlive()) {
                                 mobSpecial(mob, lm(mob), sp, now(server));
                              }
                           });
                        } else if (tp != null && tp.isAlive() && tp.getWorld() == w && !(tp.squaredDistanceTo(mob) > 400.0)) {
                           Vec3d d = new Vec3d(tp.getX() - mob.getX(), 0.0, tp.getZ() - mob.getZ());
                           if (d.lengthSquared() < 1.0E-4) {
                              float yr = mob.getYaw() * (float) (Math.PI / 180.0);
                              d = new Vec3d((double)(-MathHelper.sin(yr)), 0.0, (double)MathHelper.cos(yr));
                           }

                           s.dir = d.normalize();
                           s.lunging = true;
                           s.endAt = now + 9L;
                           mob.swingHand(Hand.MAIN_HAND);
                           mob.setVelocity(s.dir.x * 0.95, mob.isOnGround() ? 0.18 : mob.getVelocity().y, s.dir.z * 0.95);
                           mob.velocityModified = true;
                           fx(w, mob.getPos(), b -> {
                              b.writeByte(11);
                              b.writeInt(mob.getId());
                              b.writeByte(9);
                           });
                        } else {
                           skills.remove(s.mobId);
                           endPrepare(mob);
                        }
                     }
                  }
                  continue;
               }

               if (now < s.endAt && !disabled(m, now)) {
                  mob.getNavigation().stop();
                  float yaw = (float)(MathHelper.atan2(s.dir.z, s.dir.x) * 180.0F / (float)Math.PI) - 90.0F;
                  mob.setYaw(yaw);
                  mob.setBodyYaw(yaw);
                  mob.setHeadYaw(yaw);
                  mob.setVelocity(s.dir.x * 0.95, mob.getVelocity().y, s.dir.z * 0.95);
                  mob.velocityModified = true;
                  Box mb = mob.getBoundingBox().expand(0.35, 0.2, 0.35);
                  Box nearBox = mob.getBoundingBox().expand(1.5, 0.6, 1.5);

                  for (ServerPlayerEntity pl : w.getPlayers()) {
                     if (pl.isAlive() && !pl.isSpectator() && isLive(pl) && !s.hit.contains(pl.getUuid())) {
                        if (mb.intersects(pl.getBoundingBox())) {
                           contact(server, mob, m, s, pl, now);
                           if (s.atk.targeting != MobAttack.Targeting.AOE) {
                              s.endAt = Math.min(s.endAt, now + 2L);
                           }
                        } else if (nearBox.intersects(pl.getBoundingBox())) {
                           s.near.add(pl.getUuid());
                        }
                     }
                  }
               } else {
                  finishLunge(server, mob, s, now);
               }
               continue;
            }

            skills.remove(s.mobId);
         }
      }
   }

   private static void contact(MinecraftServer server, MobEntity mob, LiveCombat.LM m, LiveCombat.MobSkill s, ServerPlayerEntity pl, long now) {
      s.hit.add(pl.getUuid());
      skillFx(pl, s);
      int guard = guardCheck(pl, mob, true);
      if (guard >= 0) {
         DamageSource src = pl.getDamageSources().mobAttack(mob);
         if (guard == 2) {
            applyHurt(pl, mob, src, s.dmg, guard);
         } else {
            float dmg = s.dmg;
            MobAttack atk = s.atk;
            UUID pid = pl.getUuid();
            later(server, SkillTiming.mobAt(atk.toSkillType(), 0.8F), () -> {
               ServerPlayerEntity p2 = server.getPlayerManager().getPlayer(pid);
               if (p2 != null && p2.isAlive()) {
                  applyHurt(p2, mob, src, dmg, guard);
                  if (guard == 0 && !p2.isCreative()) {
                     playerSpecial(p2, atk.special);
                  }

                  if (mob.isAlive() && atk.damageMult > 0.0F && atk.special != MobAttack.Special.NONE) {
                     mobSpecial(mob, lm(mob), atk.special, now(server));
                  }
               }
            });
         }
      }
   }

   private static void finishLunge(MinecraftServer server, MobEntity mob, LiveCombat.MobSkill s, long now) {
      skills.remove(s.mobId);
      Vec3d v = mob.getVelocity();
      mob.setVelocity(v.x * 0.25, v.y, v.z * 0.25);
      mob.velocityModified = true;
      LiveCombat.LM m = lm(mob);
      if (s.atk.targeting == MobAttack.Targeting.AOE) {
         skillFx(mob, s);

         for (ServerPlayerEntity pl : ((ServerWorld)mob.getWorld()).getPlayers()) {
            if (pl.isAlive() && !pl.isSpectator() && isLive(pl) && !s.hit.contains(pl.getUuid())) {
               if (pl.squaredDistanceTo(mob) <= 7.839999999999999) {
                  contact(server, mob, m, s, pl, now);
               } else if (pl.squaredDistanceTo(mob) <= 20.25) {
                  s.near.add(pl.getUuid());
               }
            }
         }
      }

      for (UUID u : s.near) {
         if (!s.hit.contains(u)) {
            ServerPlayerEntity plx = server.getPlayerManager().getPlayer(u);
            if (plx != null && plx.isAlive()) {
               graze(plx, lp(u));
            }
         }
      }
   }

   private static void playerSpecial(ServerPlayerEntity pl, MobAttack.Special sp) {
      switch (sp) {
         case APPLY_FREEZE:
            pl.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 3));
            pl.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 60, 2));
            text(pl, "FROZEN", 8974079);
            break;
         case APPLY_SLEEP:
            pl.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 6));
            pl.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 50, 0));
            text(pl, "ZZZ", 11197951);
            break;
         case APPLY_CONFUSION:
            pl.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
            text(pl, "CONFUSED", 16755268);
      }
   }

   private static void mobSpecial(MobEntity mob, LiveCombat.LM m, MobAttack.Special sp, long now) {
      switch (sp) {
         case BUFF:
            m.atkMult = 1.5F;
            m.atkUntil = now + 200L;
            text(mob, "ATK UP", 16733508);
            break;
         case HEAL_MOB:
            heal(mob, mob.getMaxHealth() * 0.15F);
            break;
         case CURE_MOB:
            m.burnUntil = 0L;
            m.freezeUntil = 0L;
            m.sleepUntil = 0L;
            m.confuseUntil = 0L;
            m.weakUntil = 0L;
            text(mob, "CURED", 8978312);
            sendMobInfo(mob, m, null);
      }
   }

   private static void scanProjectiles(MinecraftServer server) {
      for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
         if (pl.isAlive() && isLive(pl)) {
            for (ProjectileEntity pr : pl.getServerWorld()
               .getEntitiesByClass(
                  ProjectileEntity.class, pl.getBoundingBox().expand(1.4), prx -> prx.getOwner() != pl && prx.getVelocity().lengthSquared() > 0.04
               )) {
               long key = (long)pr.getId() << 32 ^ (long)pl.getId();
               if (grazedProjectiles.add(key)) {
                  graze(pl, lp(pl.getUuid()));
               }
            }
         }
      }
   }

   static final class LM {
      float mercy;
      boolean spareable;
      long stunUntil;
      long slideUntil;
      int lives = -1;
      int maxLives = 0;
      long burnUntil;
      long nextBurn;
      float burnDmg;
      UUID burnBy;
      long freezeUntil;
      long sleepUntil;
      long confuseUntil;
      float weakMult = 1.0F;
      long weakUntil;
      int sentMask = -1;
      long nextSkillAt;
      long iceHitAt = -1000L;
      float atkMult = 1.0F;
      long atkUntil;
   }

   static final class LP {
      float tp;
      float sentTp = -1.0F;
      int step = -1;
      long lastAtk = -100L;
      long comboReadyAt;
      int queuedSwings = 0;
      int queuedLock = -1;
      long queuedAt;
      boolean blocking;
      long blockStart = -100L;
      long iframesUntil = -1L;
      long dashReady = 0L;
      long skillReady = 0L;
      long itemReady = 0L;
      int lockId = -1;
      float atkMult = 1.0F;
      long atkUntil = 0L;
      float shield = 0.0F;
      long shieldUntil = 0L;
      boolean menuOpen;
      long stunUntil;
      long bashUntil;
      long bashReady;
      float bashMult;
      int bashTp;
      final Set<Integer> bashHit = new HashSet<>();
      final Map<Integer, Long> actReady = new HashMap<>();
      int armedSent = -2;
   }

   private static record Later(long due, Runnable action) {
   }

   private static final class MobSkill {
      int mobId;
      UUID target;
      MobAttack atk;
      float dmg;
      int color;
      long lungeAt;
      long endAt;
      boolean selfCast;
      boolean lunging;
      Vec3d dir = Vec3d.ZERO;
      final Set<UUID> hit = new HashSet<>();
      final Set<UUID> near = new HashSet<>();
   }

   private static record Pending(int mobId, UUID player, long due) {
   }
}
