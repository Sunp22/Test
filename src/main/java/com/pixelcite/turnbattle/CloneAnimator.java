package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class CloneAnimator {
   public static final int ITEM_GREEN = 7143290;
   public static final int SPARE_YELLOW = 16770650;
   public static final int GUARD_BLUE = 9427199;
   public static final int ACT_ORANGE = 16753722;
   public static final int HURT_RED = 16730698;
   static final long COMBO_MS = 450L;
   static final long COMBO_FIN_MS = 700L;
   static final float FLINCH_MS = 420.0F;
   private static final Map<UUID, CloneAnimator.Track> tracks = new HashMap<>();
   private static LivingEntity activeEntity;
   private static CloneAnimator.Pose activePose;
   private static final String[][] GLYPH = new String[][]{
      {".###.", "#...#", "..##.", ".....", "..#.."}, {"..#..", "..#..", "..#..", ".....", "..#.."}, {".....", ".....", ".....", ".....", "#.#.#"}
   };

   private CloneAnimator() {
   }

   static boolean isOneShot(CloneAnimator.Act a) {
      return a == CloneAnimator.Act.BASH
         || a == CloneAnimator.Act.SKILL
         || a == CloneAnimator.Act.ITEM
         || a == CloneAnimator.Act.SPARE
         || a == CloneAnimator.Act.ACT
         || a == CloneAnimator.Act.COMBO;
   }

   static boolean isIdle(CloneAnimator.Act a) {
      return a == CloneAnimator.Act.NONE || a == CloneAnimator.Act.IDLE_ARMED;
   }

   static long durationMs(CloneAnimator.Act a) {
      return switch (a) {
         case BASH -> 720L;
         default -> 0L;
         case SKILL -> 980L;
         case ITEM -> 1050L;
         case SPARE -> 1200L;
         case ACT -> 1150L;
         case COMBO -> 450L;
      };
   }

   private static long blendMs(CloneAnimator.Act to) {
      return switch (to) {
         case NONE, IDLE_ARMED -> 260L;
         case BASH_READY -> 200L;
         default -> 150L;
         case DEFEND -> 90L;
         case DOWNED -> 120L;
         case CHOOSE_ACT -> 200L;
      };
   }

   private static CloneAnimator.Track track(UUID key) {
      return tracks.computeIfAbsent(key, CloneAnimator.Track::new);
   }

   public static void observeHp(UUID key, boolean down) {
      observeHp(key, Float.NaN, down);
   }

   public static void observeHp(UUID key, float hp, boolean down) {
      if (key != null) {
         CloneAnimator.Track t = track(key);
         long now = now();
         if (!Float.isNaN(hp)) {
            if (!Float.isNaN(t.lastHp) && hp < t.lastHp - 0.05F && !down && !t.downed && now - t.hurtStart > 220L) {
               t.hurtStart = now;
               t.hurtK = t.held == CloneAnimator.Act.DEFEND ? 0.5F : 1.0F;
            }

            t.lastHp = hp;
         }

         if (!t.hpSeen) {
            t.hpSeen = true;
            if (down) {
               t.downed = true;
               t.downedStart = now - 10000L;
               t.held = CloneAnimator.Act.NONE;
               t.shot = CloneAnimator.Act.NONE;
               t.shown = CloneAnimator.Act.DOWNED;
               t.from = null;
            }
         } else {
            if (down && !t.downed) {
               t.downed = true;
               t.downedStart = now;
               t.held = CloneAnimator.Act.NONE;
               t.shot = CloneAnimator.Act.NONE;
            } else if (!down && t.downed) {
               t.downed = false;
            }
         }
      }
   }

   private static long now() {
      return Util.getMeasuringTimeMs();
   }

   public static void setSoul(UUID key, SoulType soul) {
      if (key != null) {
         CloneAnimator.Track t = track(key);
         if (t.soul != soul) {
            t.soul = soul;
            if (t.shown == CloneAnimator.Act.IDLE_ARMED) {
               t.from = t.last.copy();
               t.blendStart = now();
               t.blendDur = 400L;
            }
         }
      }
   }

   private static int normColor(int c) {
      c &= 16777215;
      return c == 0 ? 16777215 : c;
   }

   public static void play(UUID key, CloneAnimator.Act act, int color, ItemStack item) {
      play(key, act, color, item, CastAnims.Cast.GENERIC);
   }

   public static void play(UUID key, CloneAnimator.Act act, int color, ItemStack item, CastAnims.Cast cast) {
      if (key != null && act != null && act != CloneAnimator.Act.NONE) {
         long now = now();
         CloneAnimator.Track t = track(key);
         if (isOneShot(act)) {
            if (t.shot == act && now - t.shotStart < shotMs(t)) {
               return;
            }

            t.shot = act;
            t.shotCast = act == CloneAnimator.Act.SKILL && cast != null ? cast : CastAnims.Cast.GENERIC;
            t.shotStart = now;
            t.shotColor = normColor(color);
            t.shotItem = item == null ? ItemStack.EMPTY : item.copy();
            t.held = CloneAnimator.Act.NONE;
         } else {
            if (act == CloneAnimator.Act.DEFEND && t.held == CloneAnimator.Act.DEFEND) {
               return;
            }

            setHeld(t, act, normColor(color), item, now, 0L);
            if (act == CloneAnimator.Act.DEFEND) {
               t.shot = CloneAnimator.Act.NONE;
               t.lastTurn = true;
            }
         }
      }
   }

   public static void restart(UUID key, CloneAnimator.Act act, int color, ItemStack item) {
      if (key != null && act != null && isOneShot(act)) {
         long now = now();
         CloneAnimator.Track t = track(key);
         t.from = t.last.copy();
         t.blendStart = now;
         t.blendDur = 70L;
         t.shown = act;
         t.shot = act;
         t.shotCast = CastAnims.Cast.GENERIC;
         t.shotStart = now;
         t.shotColor = normColor(color);
         t.shotItem = item == null ? ItemStack.EMPTY : item.copy();
         t.held = CloneAnimator.Act.NONE;
      }
   }

   public static void playCombo(UUID key, int step, int color) {
      if (key != null) {
         restart(key, CloneAnimator.Act.COMBO, color, null);
         track(key).shotVariant = Math.max(0, Math.min(4, step));
      }
   }

   public static void flinch(UUID key, float k) {
      if (key != null) {
         CloneAnimator.Track t = track(key);
         long now = now();
         if (!t.downed && now - t.hurtStart > 120L) {
            t.hurtStart = now;
            t.hurtK = k;
         }
      }
   }

   public static void hold(UUID key, CloneAnimator.Act act, int color, ItemStack item) {
      hold(key, act, color, item, 0L);
   }

   public static void hold(UUID key, CloneAnimator.Act act, int color, ItemStack item, long expireAfterMs) {
      if (key != null && act != null) {
         if (act == CloneAnimator.Act.NONE) {
            releaseMenuPoses(key);
         } else {
            setHeld(track(key), act, normColor(color), item, now(), expireAfterMs);
         }
      }
   }

   private static void setHeld(CloneAnimator.Track t, CloneAnimator.Act act, int color, ItemStack item, long now, long expireAfterMs) {
      if (t.held != act) {
         t.held = act;
         t.heldStart = now;
      }

      t.heldColor = color;
      t.heldItem = item == null ? ItemStack.EMPTY : item;
      t.heldExpire = expireAfterMs > 0L ? now + expireAfterMs : 0L;
   }

   public static void release(UUID key, CloneAnimator.Act act) {
      if (key != null) {
         CloneAnimator.Track t = tracks.get(key);
         if (t != null && t.held == act) {
            t.held = CloneAnimator.Act.NONE;
         }
      }
   }

   public static void releaseMenuPoses(UUID key) {
      if (key != null) {
         CloneAnimator.Track t = tracks.get(key);
         if (t != null) {
            if (t.held == CloneAnimator.Act.BASH_READY
               || t.held == CloneAnimator.Act.CHOOSE_SKILL
               || t.held == CloneAnimator.Act.CHOOSE_ITEM
               || t.held == CloneAnimator.Act.CHOOSE_ACT) {
               t.held = CloneAnimator.Act.NONE;
            }
         }
      }
   }

   public static CloneAnimator.Act heldOf(UUID key) {
      if (key == null) {
         return CloneAnimator.Act.NONE;
      } else {
         CloneAnimator.Track t = tracks.get(key);
         return t == null ? CloneAnimator.Act.NONE : t.held;
      }
   }

   public static void releaseAllGuards() {
      for (CloneAnimator.Track t : tracks.values()) {
         if (t.held == CloneAnimator.Act.DEFEND) {
            t.held = CloneAnimator.Act.NONE;
         }
      }
   }

   public static void observeTurn(UUID key, boolean isTurn) {
      if (key != null) {
         CloneAnimator.Track t = tracks.get(key);
         if (t != null) {
            if (isTurn && !t.lastTurn && t.held == CloneAnimator.Act.DEFEND) {
               t.held = CloneAnimator.Act.NONE;
            }

            t.lastTurn = isTurn;
         }
      }
   }

   public static void clearAll() {
      tracks.clear();
      activeEntity = null;
      activePose = null;
   }

   public static CloneAnimator.Frame frame(UUID key) {
      return frame(key, false);
   }

   public static CloneAnimator.Frame frame(UUID key, boolean armed) {
      if (key == null) {
         return null;
      } else {
         CloneAnimator.Track t = track(key);
         long now = now();
         if (t.shot != CloneAnimator.Act.NONE && now - t.shotStart >= shotMs(t)) {
            t.shot = CloneAnimator.Act.NONE;
         }

         if (t.held != CloneAnimator.Act.NONE && t.heldExpire > 0L && now >= t.heldExpire) {
            t.held = CloneAnimator.Act.NONE;
         }

         boolean isShot = !t.downed && t.shot != CloneAnimator.Act.NONE;
         CloneAnimator.Act act = t.downed ? CloneAnimator.Act.DOWNED : (isShot ? t.shot : t.held);
         if (act == CloneAnimator.Act.NONE && armed) {
            act = CloneAnimator.Act.IDLE_ARMED;
         }

         float el = act == CloneAnimator.Act.DOWNED
            ? (float)(now - t.downedStart)
            : (isIdle(act) ? (float)(now % 3600000L) + t.idlePhase * 1000.0F : (float)(now - (isShot ? t.shotStart : t.heldStart)));
         float p = isShot ? Math.min(1.0F, el / (float)shotMs(t)) : 0.0F;
         int color = isShot ? t.shotColor : t.heldColor;
         ItemStack item = isShot ? t.shotItem : t.heldItem;
         CastAnims.Cast cast = isShot && act == CloneAnimator.Act.SKILL ? t.shotCast : CastAnims.Cast.GENERIC;
         CloneAnimator.Pose target = act == CloneAnimator.Act.COMBO ? poseCombo(p, t.shotVariant) : computePose(act, el, p, cast, t.soul);
         if (act != t.shown) {
            t.from = t.last.copy();
            t.blendStart = now;
            t.blendDur = t.shown == CloneAnimator.Act.DOWNED ? 700L : blendMs(act);
            t.shown = act;
         }

         CloneAnimator.Pose out = target;
         if (t.from != null) {
            float b = t.blendDur <= 0L ? 1.0F : (float)(now - t.blendStart) / (float)t.blendDur;
            if (b < 1.0F) {
               out = CloneAnimator.Pose.lerp(t.from, target, easeOutCubic(clamp01(b)));
            } else {
               t.from = null;
            }
         }

         t.last = out;
         float hurtT = -1.0F;
         if (t.hurtStart > 0L && !t.downed) {
            float ht = (float)(now - t.hurtStart);
            if (ht < 420.0F) {
               hurtT = ht;
               out = out.copy();
               applyFlinch(out, ht, t.hurtK);
            }
         }

         CloneAnimator.Frame fr = new CloneAnimator.Frame(
            act, el, p, out, color, handFor(act, p, item, cast), act == CloneAnimator.Act.DEFEND, hurtT, t.hurtK, cast
         );
         fr.soul = t.soul;
         return fr;
      }
   }

   private static long shotMs(CloneAnimator.Track t) {
      if (t.shot == CloneAnimator.Act.SKILL && t.shotCast != CastAnims.Cast.GENERIC) {
         return t.shotCast.ms;
      } else if (t.shot == CloneAnimator.Act.COMBO) {
         return t.shotVariant == 4 ? 700L : 450L;
      } else {
         return durationMs(t.shot);
      }
   }

   private static void applyFlinch(CloneAnimator.Pose o, float t, float k) {
      float in = easeOutCubic(clamp01(t / 55.0F));
      float q = seg(t, 55.0F, 420.0F);
      float out = 1.0F - easeInOutSine(q);
      float rebound = (float)Math.sin((double)q * Math.PI) * 0.22F;
      float f = (in * out - rebound * (1.0F - in * out)) * k;
      float shiver = (1.0F - seg(t, 0.0F, 170.0F)) * k;
      o.dx = o.dx + -3.2F * f + jitter(t, 7) * 0.7F * shiver;
      o.dy += 0.6F * f;
      o.tilt += -6.5F * f;
      o.sx *= 1.0F + 0.045F * f;
      o.sy *= 1.0F - 0.05F * f;
      o.headP -= 0.32F * f;
      o.rArmP += 0.22F * f;
      o.rArmR += 0.18F * f;
      o.lArmP += 0.22F * f;
      o.lArmR -= 0.18F * f;
   }

   private static ItemStack handFor(CloneAnimator.Act act, float p, ItemStack item, CastAnims.Cast cast) {
      if (act == CloneAnimator.Act.SKILL && !cast.weapon) {
         return p * (float)cast.ms > 50.0F && p < 0.94F ? ItemStack.EMPTY : null;
      } else {
         return switch (act) {
            case CHOOSE_ITEM -> item;
            case ITEM -> p < 0.8F ? item : ItemStack.EMPTY;
            default -> null;
            case SPARE -> p > 0.06F && p < 0.9F ? ItemStack.EMPTY : null;
            case CHOOSE_ACT -> ItemStack.EMPTY;
            case ACT -> p < 0.9F ? ItemStack.EMPTY : null;
         };
      }
   }

   public static void beginModelPose(LivingEntity entity, CloneAnimator.Pose pose) {
      activeEntity = entity;
      activePose = pose;
   }

   public static float bladeRollFor(LivingEntity entity) {
      CloneAnimator.Pose p = activePose;
      return p != null && entity != null && entity == activeEntity ? p.bladeRoll : 0.0F;
   }

   public static float bladeTiltFor(LivingEntity entity) {
      CloneAnimator.Pose p = activePose;
      return p != null && entity != null && entity == activeEntity ? p.bladeTilt : 0.0F;
   }

   public static CloneAnimator.Pose activePoseFor(LivingEntity entity) {
      return entity != null && entity == activeEntity ? activePose : null;
   }

   public static void endModelPose() {
      activeEntity = null;
      activePose = null;
   }

   public static void applyModelPose(BipedEntityModel<?> m, LivingEntity entity) {
      CloneAnimator.Pose p = activePose;
      if (p != null && entity != null && entity == activeEntity) {
         if (p.armW > 0.001F) {
            float w = Math.min(1.0F, p.armW);
            m.rightArm.pitch = mix(m.rightArm.pitch, p.rArmP, w);
            m.rightArm.yaw = mix(m.rightArm.yaw, p.rArmY, w);
            m.rightArm.roll = mix(m.rightArm.roll, p.rArmR, w);
            m.leftArm.pitch = mix(m.leftArm.pitch, p.lArmP, w);
            m.leftArm.yaw = mix(m.leftArm.yaw, p.lArmY, w);
            m.leftArm.roll = mix(m.leftArm.roll, p.lArmR, w);
         }

         if (p.bodyW > 0.001F) {
            float by = mix(m.body.yaw, p.bodyY, Math.min(1.0F, p.bodyW));
            float delta = by - m.body.yaw;
            m.body.yaw = by;
            float s = (float)Math.sin((double)by);
            float c = (float)Math.cos((double)by);
            m.rightArm.pivotZ = s * 5.0F;
            m.rightArm.pivotX = -c * 5.0F;
            m.leftArm.pivotZ = -s * 5.0F;
            m.leftArm.pivotX = c * 5.0F;
            m.rightArm.yaw += delta;
            m.leftArm.yaw += delta;
         }

         if (p.armW > 0.001F && (p.rArmPX != 0.0F || p.rArmPY != 0.0F || p.rArmPZ != 0.0F)) {
            float w = Math.min(1.0F, p.armW);
            m.rightArm.pivotX = m.rightArm.pivotX + p.rArmPX * w;
            m.rightArm.pivotY = m.rightArm.pivotY + p.rArmPY * w;
            m.rightArm.pivotZ = m.rightArm.pivotZ + p.rArmPZ * w;
         }

         if (p.headW > 0.001F) {
            float w = Math.min(1.0F, p.headW);
            m.head.pitch = mix(m.head.pitch, p.headP, w);
            m.head.yaw = m.head.yaw + p.headY * w;
         }

         if (p.legW > 0.001F) {
            float w = Math.min(1.0F, p.legW);
            m.rightLeg.pitch = mix(m.rightLeg.pitch, p.rLegP, w);
            m.leftLeg.pitch = mix(m.leftLeg.pitch, p.lLegP, w);
            m.rightLeg.yaw = mix(m.rightLeg.yaw, 0.0F, w);
            m.leftLeg.yaw = mix(m.leftLeg.yaw, 0.0F, w);
         }

         m.hat.copyTransform(m.head);
      }
   }

   private static CloneAnimator.Pose computePose(CloneAnimator.Act act, float t, float p) {
      return computePose(act, t, p, CastAnims.Cast.GENERIC);
   }

   private static CloneAnimator.Pose computePose(CloneAnimator.Act act, float t, float p, CastAnims.Cast cast) {
      return computePose(act, t, p, cast, null);
   }

   private static CloneAnimator.Pose computePose(CloneAnimator.Act act, float t, float p, CastAnims.Cast cast, SoulType soul) {
      CloneAnimator.Pose o = new CloneAnimator.Pose();
      if (act == CloneAnimator.Act.SKILL && cast != CastAnims.Cast.GENERIC) {
         CastAnims.pose(cast, o, p, t);
         return o;
      } else {
         switch (act) {
            case NONE:
               poseIdle(o, t);
               o.idleW = 1.0F;
               break;
            case BASH_READY:
               poseReady(o, t);
               break;
            case BASH:
               poseBash(o, p);
               break;
            case CHOOSE_SKILL:
               poseChooseSkill(o, t);
               break;
            case SKILL:
               poseSkill(o, p);
               break;
            case CHOOSE_ITEM:
               poseChooseItem(o, t);
               break;
            case ITEM:
               poseItem(o, p, t);
               break;
            case DEFEND:
               poseDefend(o, t);
               break;
            case SPARE:
               poseSpare(o, p, t);
               break;
            case DOWNED:
               poseDowned(o, t);
               break;
            case IDLE_ARMED:
               poseIdleArmed(o, t, soul);
               o.idleW = 1.0F;
               break;
            case CHOOSE_ACT:
               poseChooseAct(o, t);
               break;
            case ACT:
               poseAct(o, p);
         }

         return o;
      }
   }

   private static void poseIdle(CloneAnimator.Pose o, float t) {
      float br = (float)Math.sin((double)(t * 0.0028F));
      float sway = (float)Math.sin((double)(t * 0.0011F));
      float cyc = t % 6400.0F / 6400.0F;
      int cycN = (int)(t / 6400.0F);
      float glance = cyc > 0.8F ? (float)Math.sin(Math.PI * (double)seg(cyc, 0.8F, 1.0F)) * (cycN % 2 == 0 ? 0.35F : -0.28F) : 0.0F;
      o.armW = 0.9F;
      o.rArmP = -0.32F + br * 0.04F;
      o.rArmY = -0.08F;
      o.rArmR = 0.1F + br * 0.02F;
      o.lArmP = -0.12F - br * 0.03F;
      o.lArmY = 0.1F;
      o.lArmR = -0.12F - br * 0.02F;
      o.headW = 1.0F;
      o.headP = 0.04F + br * 0.025F;
      o.headY = (float)Math.sin((double)(t * 4.5E-4F)) * 0.12F + glance;
      o.legW = 0.8F;
      o.rLegP = 0.1F;
      o.lLegP = -0.08F;
      o.sy = 1.0F + br * 0.01F;
      o.sx = 1.0F - br * 0.006F;
      o.dx = sway * 0.6F;
      o.tilt = sway * 0.8F;
   }

   private static void poseIdleArmed(CloneAnimator.Pose o, float t) {
      float br = (float)Math.sin((double)(t * 0.0036F));
      float bob = (float)Math.sin((double)(t * 0.0072F));
      float sway = (float)Math.sin((double)(t * 0.0013F));
      float cyc = t % 5200.0F / 5200.0F;
      int cycN = (int)(t / 5200.0F);
      float flourish = cyc > 0.82F ? (float)Math.sin(Math.PI * (double)seg(cyc, 0.82F, 1.0F)) : 0.0F;
      float glance = cyc > 0.38F && cyc < 0.56F ? (float)Math.sin(Math.PI * (double)seg(cyc, 0.38F, 0.56F)) * (cycN % 2 == 0 ? 0.2F : -0.16F) : 0.0F;
      o.armW = 1.0F;
      o.rArmP = 0.15F + br * 0.03F - flourish * 0.55F;
      o.rArmY = 0.1F;
      o.rArmR = 0.5F + br * 0.02F + flourish * 0.15F;
      o.lArmP = -0.8F - br * 0.04F;
      o.lArmY = 0.36F;
      o.lArmR = -0.06F;
      o.bodyW = 1.0F;
      o.bodyY = 0.22F;
      o.headW = 1.0F;
      o.headP = 0.1F + br * 0.02F;
      o.headY = -0.2F + glance;
      o.legW = 1.0F;
      o.rLegP = 0.32F;
      o.lLegP = -0.32F;
      o.sy = 0.972F + br * 0.008F - bob * 0.006F;
      o.sx = 1.016F - br * 0.005F;
      o.dy = 0.8F + bob * 0.4F;
      o.dx = sway * 0.5F;
      o.tilt = 2.5F + sway * 0.6F;
   }

   private static void poseIdleArmed(CloneAnimator.Pose o, float t, SoulType soul) {
      if (soul == null) {
         poseIdleArmed(o, t);
      } else {
         switch (soul) {
            case PATIENCE:
               idlePatience(o, t);
               break;
            case BRAVERY:
               idleBravery(o, t);
               break;
            case INTEGRITY:
               idleIntegrity(o, t);
               break;
            case PERSEVERANCE:
               idlePerseverance(o, t);
               break;
            case KINDNESS:
               idleKindness(o, t);
               break;
            case JUSTICE:
               idleJustice(o, t);
               break;
            default:
               poseIdleArmed(o, t);
         }
      }
   }

   private static float bell(float q) {
      return !(q <= 0.0F) && !(q >= 1.0F) ? (float)Math.sin(Math.PI * (double)q) : 0.0F;
   }

   private static float sinT(float t, float k) {
      return (float)Math.sin((double)(t * k));
   }

   private static void idlePatience(CloneAnimator.Pose o, float t) {
      float br = sinT(t, 0.0018F);
      float sway = sinT(t, 7.0E-4F);
      float look = bell(seg(t % 7600.0F / 7600.0F, 0.45F, 0.8F));
      o.armW = 1.0F;
      o.rArmP = 0.38F + br * 0.015F;
      o.rArmY = 0.05F;
      o.rArmR = 0.1F;
      o.lArmP = -0.04F - br * 0.01F;
      o.lArmY = 0.04F;
      o.lArmR = -0.06F;
      o.headW = 1.0F;
      o.headP = 0.32F + br * 0.02F - 0.34F * look;
      o.headY = 0.0F;
      o.legW = 1.0F;
      o.rLegP = 0.04F;
      o.lLegP = -0.04F;
      o.sy = 1.0F + br * 0.012F;
      o.sx = 1.0F - br * 0.006F;
      o.dx = sway * 0.25F;
      o.tilt = 0.6F + sway * 0.3F;
   }

   private static void idleBravery(CloneAnimator.Pose o, float t) {
      float br = sinT(t, 0.0045F);
      float bob = sinT(t, 0.011F);
      float sway = sinT(t, 0.0017F);
      float feint = bell(seg(t % 4200.0F / 4200.0F, 0.58F, 0.74F));
      o.armW = 1.0F;
      o.rArmP = -0.35F - 0.45F * feint + br * 0.03F;
      o.rArmY = 0.05F;
      o.rArmR = 0.75F - 0.25F * feint;
      o.lArmP = -1.35F + 0.25F * feint - br * 0.03F;
      o.lArmY = 0.25F;
      o.lArmR = -0.05F;
      o.bodyW = 1.0F;
      o.bodyY = 0.35F - 0.12F * feint;
      o.headW = 1.0F;
      o.headP = -0.05F + br * 0.02F;
      o.headY = -0.1F;
      o.legW = 1.0F;
      o.rLegP = 0.5F;
      o.lLegP = -0.55F;
      o.sy = 0.975F + br * 0.008F - bob * 0.01F;
      o.dy = 1.5F + bob * 0.8F;
      o.dx = sway * 0.5F + 3.5F * feint;
      o.tilt = 9.0F + sway * 0.6F + 4.0F * feint;
   }

   private static void idleIntegrity(CloneAnimator.Pose o, float t) {
      float br = sinT(t, 0.0026F);
      float cyc = t % 6400.0F / 6400.0F;
      float snap = seg(cyc, 0.7F, 0.74F) * (1.0F - seg(cyc, 0.8F, 0.86F));
      o.armW = 1.0F;
      o.rArmP = -1.3F - 0.22F * snap + br * 0.01F;
      o.rArmY = -0.62F;
      o.rArmR = 0.0F;
      o.lArmP = 0.48F;
      o.lArmY = -0.5F;
      o.lArmR = 0.12F;
      o.headW = 1.0F;
      o.headP = br * 0.01F;
      o.headY = 0.0F;
      o.legW = 1.0F;
      o.rLegP = 0.0F;
      o.lLegP = 0.0F;
      o.sy = 1.01F + br * 0.006F;
      o.tilt = -0.5F;
   }

   private static void idlePerseverance(CloneAnimator.Pose o, float t) {
      float br = sinT(t, 0.0031F);
      float sway = sinT(t, 0.0012F);
      float sag = bell(seg(t % 6000.0F / 6000.0F, 0.6F, 0.86F));
      o.armW = 1.0F;
      o.rArmP = -0.62F + br * 0.05F + sag * 0.25F;
      o.rArmY = -0.55F;
      o.rArmR = 0.0F;
      o.lArmP = -0.66F + br * 0.05F + sag * 0.25F;
      o.lArmY = 0.6F;
      o.lArmR = 0.0F;
      o.headW = 1.0F;
      o.headP = -0.08F + br * 0.05F + sag * 0.25F;
      o.headY = 0.0F;
      o.legW = 1.0F;
      o.rLegP = 0.28F;
      o.lLegP = -0.36F;
      o.sy = 0.97F + br * 0.016F - sag * 0.02F;
      o.dy = 1.4F + br * 0.5F + sag * 1.2F;
      o.dx = sway * 0.4F - sag * 0.6F;
      o.tilt = 8.0F + sway * 0.8F + sag * 4.0F;
   }

   private static void idleKindness(CloneAnimator.Pose o, float t) {
      float br = sinT(t, 0.0024F);
      float sway = sinT(t, 0.001F);
      float nod = bell(seg(t % 5600.0F / 5600.0F, 0.55F, 0.7F));
      o.armW = 1.0F;
      o.rArmP = 0.52F + br * 0.02F;
      o.rArmY = 0.12F;
      o.rArmR = 0.18F;
      o.lArmP = -1.12F - br * 0.03F;
      o.lArmY = 0.78F;
      o.lArmR = 0.0F;
      o.headW = 1.0F;
      o.headP = 0.06F + br * 0.02F + nod * 0.2F;
      o.headY = 0.12F * sinT(t, 6.0E-4F);
      o.legW = 1.0F;
      o.rLegP = 0.12F;
      o.lLegP = -0.1F;
      o.sy = 1.0F + br * 0.01F;
      o.dx = sway * 0.9F;
      o.tilt = 1.0F + sway * 1.1F;
   }

   private static void idleJustice(CloneAnimator.Pose o, float t) {
      float br = sinT(t, 0.003F);
      float sway = sinT(t, 0.0012F);
      float cyc = t % 5600.0F / 5600.0F;
      float draw = easeOutCubic(seg(cyc, 0.5F, 0.53F)) * (1.0F - seg(cyc, 0.66F, 0.72F));
      float tip = bell(seg(cyc, 0.84F, 0.97F));
      o.armW = 1.0F;
      o.rArmP = 0.15F - 0.87F * draw + br * 0.015F;
      o.rArmY = 0.0F;
      o.rArmR = 0.3F - 0.25F * draw;
      o.lArmP = -0.28F - 2.3F * tip;
      o.lArmY = 0.38F - 0.2F * tip;
      o.lArmR = -0.08F;
      o.bodyW = 1.0F;
      o.bodyY = 0.12F + 0.15F * draw;
      o.headW = 1.0F;
      o.headP = -0.03F + 0.16F * tip + br * 0.01F;
      o.headY = -0.15F;
      o.legW = 1.0F;
      o.rLegP = 0.12F;
      o.lLegP = -0.12F;
      o.sy = 1.0F + br * 0.008F;
      o.dx = sway * 0.4F;
      o.tilt = -1.0F + sway * 0.4F - 1.5F * draw;
   }

   private static void poseChooseAct(CloneAnimator.Pose o, float t) {
      float in = easeOutBack(clamp01(t / 320.0F));
      float tap = (float)Math.pow(Math.max(0.0, Math.sin((double)(t * 0.0046F))), 6.0);
      float ponder = (float)Math.sin((double)(t * 0.0011F));
      float br = (float)Math.sin((double)(t * 0.0026F));
      o.armW = 1.0F;
      o.headW = 1.0F;
      o.legW = 0.8F;
      o.bodyW = 0.8F;
      o.rArmP = mix(0.0F, -1.72F, in) + tap * 0.1F;
      o.rArmY = mix(0.0F, -0.5F, in);
      o.rArmR = 0.0F;
      o.lArmP = mix(0.0F, -0.9F, in);
      o.lArmY = mix(0.0F, 0.95F, in);
      o.lArmR = 0.05F;
      o.headP = mix(0.0F, -0.14F, in) + br * 0.02F;
      o.headY = ponder * 0.28F;
      o.bodyY = -0.1F * in;
      o.rLegP = 0.06F;
      o.lLegP = -0.06F;
      o.dx = -1.0F * in;
      o.tilt = (-2.0F + ponder * 0.9F) * in;
      o.sy = 1.0F + br * 0.009F;
      o.sx = 1.0F - br * 0.004F;
   }

   private static void poseAct(CloneAnimator.Pose o, float p) {
      float a = easeInOutSine(seg(p, 0.0F, 0.14F));
      float s = easeOutBack(seg(p, 0.14F, 0.3F));
      float d = easeInOutSine(seg(p, 0.72F, 1.0F));
      float keep = 1.0F - d;
      float emph = (float)Math.sin(Math.PI * (double)seg(p, 0.28F, 0.46F));
      o.armW = keep;
      o.headW = keep;
      o.legW = keep;
      o.bodyW = keep;
      o.rArmP = mix(mix(0.0F, 0.5F, a), -1.7F, s);
      o.rArmY = mix(0.0F, -0.3F, s);
      o.rArmR = mix(mix(0.0F, 0.15F, a), 0.0F, s);
      o.lArmP = mix(0.0F, 0.15F, s);
      o.lArmY = 0.0F;
      o.lArmR = mix(0.0F, -0.6F, s);
      o.bodyY = mix(mix(0.0F, 0.32F, a), -0.5F, s);
      o.headP = mix(mix(0.0F, 0.12F, a), -0.06F, s);
      o.rLegP = mix(0.0F, -0.35F, s);
      o.lLegP = mix(0.0F, 0.3F, s);
      o.dx = (mix(-2.0F * a, 5.0F, s) + emph * 1.5F) * keep;
      o.tilt = (mix(-3.0F * a, 5.0F, s) + emph * 2.0F) * keep;
      o.dy = -1.5F * emph * keep;
      o.sx = 1.0F + 0.04F * emph - 0.03F * a * (1.0F - s);
      o.sy = 1.0F - 0.03F * emph + 0.03F * a * (1.0F - s);
   }

   private static void poseDowned(CloneAnimator.Pose o, float t) {
      float buckle = easeOutCubic(seg(t, 0.0F, 260.0F));
      float fallLin = seg(t, 200.0F, 620.0F);
      float fall = fallLin * fallLin;
      float land = seg(t, 620.0F, 900.0F);
      float bounce = (float)Math.sin(Math.PI * (double)land) * (1.0F - land) * 7.0F;
      float rest = seg(t, 900.0F, 1500.0F);
      float breathe = (float)Math.sin((double)(t * 0.0018F)) * rest;
      o.armW = 1.0F;
      o.headW = 1.0F;
      o.legW = 1.0F;
      o.rArmP = mix(mix(0.0F, 0.25F, buckle), -0.25F, fall);
      o.rArmR = mix(mix(0.1F, 0.3F, buckle), 1.05F, fall) + breathe * 0.02F;
      o.lArmP = mix(mix(0.0F, 0.3F, buckle), -0.15F, fall);
      o.lArmR = -(mix(mix(0.1F, 0.25F, buckle), 0.85F, fall) + breathe * 0.02F);
      o.rArmY = 0.0F;
      o.lArmY = 0.0F;
      o.headP = mix(mix(0.0F, 0.55F, buckle), -0.2F, fall);
      o.headY = mix(0.0F, 0.5F, fall);
      o.rLegP = mix(0.0F, -0.25F, fall);
      o.lLegP = mix(0.0F, 0.18F, fall);
      float kneel = buckle * (1.0F - fall);
      o.sy = mix(1.0F, 0.86F, kneel) + breathe * 0.008F;
      o.sx = mix(1.0F, 1.05F, kneel);
      o.tilt = -86.0F * fall + bounce;
      o.dx = -4.0F * fall;
      o.dy = mix(0.0F, 2.5F, kneel) - 3.5F * fall;
      o.dim = 0.45F * seg(t, 250.0F, 1100.0F);
   }

   private static void poseReady(CloneAnimator.Pose o, float t) {
      float charge = clamp01(t / 900.0F);
      float breathe = (float)Math.sin((double)(t * 0.006F));
      o.armW = 1.0F;
      o.rArmP = -2.85F + breathe * 0.05F;
      o.rArmY = 0.15F;
      o.rArmR = 0.35F;
      o.lArmP = -1.05F;
      o.lArmY = 0.3F;
      o.lArmR = -0.1F;
      o.bodyW = 1.0F;
      o.bodyY = 0.38F;
      o.legW = 1.0F;
      o.rLegP = 0.32F;
      o.lLegP = -0.38F;
      o.dx = -3.0F;
      o.tilt = -5.0F;
      o.sx = 1.03F;
      o.sy = 0.96F + breathe * 0.01F;
      float j = charge * 0.7F;
      o.dx = o.dx + jitter(t, 0) * j;
      o.dy = o.dy + jitter(t, 1) * j * 0.5F;
   }

   private static void poseBash(CloneAnimator.Pose o, float p) {
      float a = easeOutCubic(seg(p, 0.0F, 0.16F));
      float s = easeOutCubic(seg(p, 0.16F, 0.36F));
      float r = easeInOutSine(seg(p, 0.62F, 1.0F));
      float settle = (float)Math.sin((double)seg(p, 0.36F, 0.62F) * Math.PI);
      float keep = 1.0F - r;
      o.armW = keep;
      o.bodyW = keep;
      o.legW = keep;
      o.bladeRoll = 0.45F * Math.max(a, s) * keep;
      o.bladeTilt = -0.8F * Math.max(a, s) * keep;
      o.rArmP = mix(mix(-2.85F, -3.15F, a), -0.35F, s);
      o.rArmY = mix(0.15F, -0.65F, s);
      o.rArmR = mix(0.35F, -0.15F, s);
      o.lArmP = mix(-1.05F, 0.65F, s);
      o.lArmY = mix(0.3F, 0.0F, s);
      o.lArmR = mix(-0.1F, -0.35F, s);
      o.bodyY = mix(mix(0.38F, 0.5F, a), -0.45F, s);
      o.rLegP = mix(0.32F, -0.7F, s);
      o.lLegP = mix(-0.38F, 0.55F, s);
      float lunge = mix(mix(-3.0F, -6.0F, a), 26.0F, s) - settle * 2.0F;
      o.dx = lunge * keep;
      o.dy = -5.0F * (float)Math.sin((double)s * Math.PI) * keep;
      o.tilt = mix(mix(-5.0F, -8.0F, a), 13.0F, s) * keep;
      float streak = (float)Math.sin((double)s * Math.PI);
      float sx = mix(mix(1.03F, 1.07F, a), 1.0F, s) + streak * 0.08F;
      float sy = mix(mix(0.96F, 0.91F, a), 1.0F, s) - streak * 0.05F;
      o.sx = mix(sx, 1.0F, r);
      o.sy = mix(sy, 1.0F, r);
   }

   static CloneAnimator.Pose poseCombo(float p, int step) {
      CloneAnimator.Pose o = new CloneAnimator.Pose();
      boolean fin = step == 4;
      float a = easeOutCubic(seg(p, 0.0F, fin ? 0.26F : 0.2F));
      float s = easeOutCubic(seg(p, fin ? 0.26F : 0.2F, fin ? 0.5F : 0.45F));
      float r = easeInOutSine(seg(p, fin ? 0.72F : 0.68F, 1.0F));
      float keep = 1.0F - r;
      float streak = (float)Math.sin((double)s * Math.PI);
      o.armW = keep;
      o.bodyW = keep;
      o.headW = 0.0F;
      o.legW = keep * 0.85F;
      o.lArmP = -0.9F;
      o.lArmY = 0.35F;
      o.lArmR = -0.15F;
      switch (step) {
         case 0:
            o.rArmP = mix(mix(-1.2F, -1.45F, a), -1.35F, s);
            o.rArmY = mix(mix(0.2F, 0.75F, a), -1.15F, s);
            o.rArmR = mix(mix(0.6F, 1.1F, a), 0.15F, s);
            o.bodyY = mix(mix(0.2F, 0.55F, a), -0.6F, s);
            o.dx = mix(mix(0.0F, -3.0F, a), 11.0F, s) * keep;
            o.tilt = mix(-3.0F * a, 7.0F, s) * keep;
            o.lArmP = mix(-0.9F, -0.4F, s);
            break;
         case 1:
            o.rArmP = mix(mix(-1.2F, -1.3F, a), -1.5F, s);
            o.rArmY = mix(mix(-0.4F, -1.1F, a), 0.7F, s);
            o.rArmR = mix(mix(0.1F, -0.15F, a), 1.15F, s);
            o.bodyY = mix(mix(-0.3F, -0.6F, a), 0.6F, s);
            o.dx = mix(mix(4.0F, 2.0F, a), 12.0F, s) * keep;
            o.tilt = mix(2.0F - 3.0F * a, 8.0F, s) * keep;
            break;
         case 2:
            o.rArmP = mix(mix(-1.5F, -3.05F, a), -0.3F, s);
            o.rArmY = mix(0.1F, -0.35F, s);
            o.rArmR = mix(mix(0.3F, 0.15F, a), -0.1F, s);
            o.bodyY = mix(0.25F * a, -0.15F, s);
            o.dx = mix(mix(3.0F, -2.0F, a), 15.0F, s) * keep;
            o.dy = (-3.0F * a * (1.0F - s) + 2.5F * streak) * keep;
            o.tilt = mix(-7.0F * a, 13.0F, s) * keep;
            o.sy = mix(1.0F, 0.95F, streak);
            o.sx = mix(1.0F, 1.04F, streak);
            break;
         case 3:
            o.rArmP = mix(mix(-0.6F, 0.45F, a), -2.95F, s);
            o.rArmY = mix(mix(-0.2F, -0.45F, a), 0.15F, s);
            o.rArmR = mix(0.2F, 0.4F, s);
            o.bodyY = mix(-0.3F * a, 0.3F, s);
            o.dx = mix(mix(4.0F, 2.0F, a), 10.0F, s) * keep;
            o.dy = mix(1.5F * a, -7.0F, s) * keep * (1.0F - seg(p, 0.5F, 0.75F) * 0.6F);
            o.tilt = mix(5.0F * a, -4.0F, s) * keep;
            o.sy = mix(mix(1.0F, 0.93F, a), 1.05F, s);
            o.sx = mix(mix(1.0F, 1.05F, a), 0.98F, s);
            o.lArmP = mix(-0.9F, -0.2F, s);
            break;
         default:
            o.rArmP = mix(mix(-1.3F, -1.7F, a), -1.25F, s);
            o.rArmY = mix(mix(0.5F, 1.05F, a), -1.35F, s);
            o.rArmR = mix(mix(0.8F, 1.3F, a), 0.1F, s);
            o.bodyY = mix(mix(0.4F, 0.95F, a), -1.1F, s);
            o.dx = (mix(mix(0.0F, -7.0F, a), 28.0F, s) - (float)Math.sin((double)seg(p, 0.5F, 0.72F) * Math.PI) * 2.0F) * keep;
            o.dy = -5.0F * streak * keep;
            o.tilt = mix(-9.0F * a, 15.0F, s) * keep;
            o.sx = mix(mix(1.03F, 1.08F, a), 1.0F, s) + streak * 0.1F;
            o.sy = mix(mix(0.96F, 0.9F, a), 1.0F, s) - streak * 0.06F;
            o.lArmP = mix(-0.9F, 0.5F, s);
            o.lArmR = mix(-0.15F, -0.6F, s);
      }
      float roll = switch (step) {
         case 0, 1, 4 -> (float) (Math.PI / 2);
         default -> 0.0F;
      };
      float w = Math.max(a, s) * keep;
      o.bladeRoll = roll * w;
      o.bladeTilt = (fin ? -1.25F : -1.1F) * w;
      o.rLegP = mix(0.3F, -0.55F, s);
      o.lLegP = mix(-0.35F, 0.5F, s);
      o.sx = mix(o.sx, 1.0F, r);
      o.sy = mix(o.sy, 1.0F, r);
      return o;
   }

   private static void poseChooseSkill(CloneAnimator.Pose o, float t) {
      float w = t * 0.0045F;
      o.armW = 1.0F;
      o.headW = 0.85F;
      o.legW = 0.6F;
      o.rArmP = -1.3F + (float)Math.sin((double)w) * 0.07F;
      o.rArmY = -0.5F;
      o.rArmR = 0.05F;
      o.lArmP = -1.3F + (float)Math.sin((double)(w + 1.3F)) * 0.07F;
      o.lArmY = 0.5F;
      o.lArmR = -0.05F;
      o.headP = 0.3F;
      o.rLegP = 0.12F;
      o.lLegP = -0.08F;
      o.dy = -3.0F - 2.2F * (float)Math.sin((double)(t * 0.0032F));
      o.tilt = 2.0F;
   }

   private static void poseSkill(CloneAnimator.Pose o, float p) {
      float g = easeOutCubic(seg(p, 0.0F, 0.22F));
      float b = easeOutBack(seg(p, 0.25F, 0.42F));
      float c = easeInOutSine(seg(p, 0.42F, 0.6F));
      float d = easeInOutSine(seg(p, 0.72F, 1.0F));
      float keep = 1.0F - d;
      float rP = mix(0.0F, -0.95F, g);
      float rY = mix(0.0F, -0.85F, g);
      float rR = 0.0F;
      float lP = mix(0.0F, -0.95F, g);
      float lY = mix(0.0F, 0.85F, g);
      float lR = 0.0F;
      rP = mix(rP, -2.9F, b);
      rY = mix(rY, 0.0F, b);
      rR = mix(rR, 0.55F, b);
      lP = mix(lP, -2.9F, b);
      lY = mix(lY, 0.0F, b);
      lR = mix(lR, -0.55F, b);
      rP = mix(rP, -1.62F, c);
      rY = mix(rY, -0.18F, c);
      rR = mix(rR, 0.0F, c);
      lP = mix(lP, -1.62F, c);
      lY = mix(lY, 0.18F, c);
      lR = mix(lR, 0.0F, c);
      o.armW = keep;
      o.headW = keep;
      o.legW = keep;
      o.rArmP = rP;
      o.rArmY = rY;
      o.rArmR = rR;
      o.lArmP = lP;
      o.lArmY = lY;
      o.lArmR = lR;
      o.headP = mix(mix(0.0F, 0.35F, g), -0.4F, b);
      o.headP = mix(o.headP, 0.0F, c);
      o.rLegP = 0.22F * b * (1.0F - 0.5F * c);
      o.lLegP = -0.18F * b * (1.0F - 0.5F * c);
      float rise = easeOutCubic(seg(p, 0.25F, 0.4F));
      float fall = easeInOutSine(seg(p, 0.7F, 0.84F));
      float hoverBob = (float)Math.sin((double)seg(p, 0.42F, 0.7F) * Math.PI * 2.0) * 1.2F;
      o.dy = (-13.0F * rise + hoverBob * rise) * (1.0F - fall);
      o.dx = 4.0F * c * keep;
      o.tilt = (mix(0.0F, -4.0F, b) + 8.0F * c) * keep;
      float pulse = (float)Math.sin(Math.PI * (double)seg(p, 0.25F, 0.45F));
      float land = (float)Math.sin(Math.PI * (double)seg(p, 0.8F, 0.94F));
      float crouch = g * (1.0F - seg(p, 0.25F, 0.32F));
      o.sx = 1.0F + 0.09F * crouch - 0.07F * pulse + 0.07F * land;
      o.sy = 1.0F - 0.13F * crouch + 0.14F * pulse - 0.09F * land;
   }

   private static void poseChooseItem(CloneAnimator.Pose o, float t) {
      float w = t * 0.011F;
      o.headW = 1.0F;
      o.headP = 0.55F;
      o.headY = (float)Math.sin((double)(t * 0.0035F)) * 0.35F;
      o.armW = 1.0F;
      o.rArmP = -0.78F + (float)Math.sin((double)w) * 0.12F;
      o.rArmY = -0.35F;
      o.rArmR = 0.05F;
      o.lArmP = -0.55F + (float)Math.cos((double)(w * 1.3F)) * 0.16F;
      o.lArmY = 0.55F;
      o.lArmR = -0.05F;
      o.tilt = 4.0F;
      o.sy = 0.975F + (float)Math.sin((double)(t * 0.009F)) * 0.012F;
      o.sx = 1.0F + (1.0F - o.sy) * 0.5F;
   }

   private static void poseItem(CloneAnimator.Pose o, float p, float t) {
      float a = easeOutBack(seg(p, 0.0F, 0.24F));
      float c = easeInOutSine(seg(p, 0.5F, 0.6F));
      float d = easeInOutSine(seg(p, 0.8F, 1.0F));
      float keep = 1.0F - d;
      o.armW = keep;
      o.headW = keep;
      o.rArmP = mix(mix(0.0F, -3.05F, a), -1.9F, c);
      o.rArmY = mix(0.0F, -0.55F, c);
      o.rArmR = mix(mix(0.0F, 0.12F, a), 0.0F, c);
      o.lArmP = mix(0.0F, -0.25F, a);
      o.lArmY = 0.0F;
      o.lArmR = mix(mix(0.0F, -0.35F, a), -0.12F, c);
      o.headP = mix(mix(0.0F, -0.45F, a), 0.12F, c);
      float hop = (float)Math.sin(Math.PI * (double)seg(p, 0.0F, 0.3F));
      float chomp = c * keep * Math.abs((float)Math.sin((double)(t * 0.03F)));
      o.dy = -6.0F * hop + chomp * 1.2F;
      o.sy = 1.0F + 0.06F * hop - 0.04F * chomp;
      o.sx = 1.0F - 0.03F * hop + 0.03F * chomp;
      o.tilt = mix(-3.0F * a, 2.0F, c) * keep;
   }

   private static void poseDefend(CloneAnimator.Pose o, float t) {
      float i = clamp01(t / 300.0F);
      float e = easeOutBack(i);
      float hop = (float)Math.sin(Math.PI * (double)clamp01(t / 260.0F));
      float land = (float)Math.sin(Math.PI * (double)clamp01((t - 220.0F) / 200.0F));
      float breathe = (float)Math.sin((double)(t * 0.004F));
      o.armW = 1.0F;
      o.headW = 1.0F;
      o.legW = 1.0F;
      o.lArmP = mix(0.0F, -1.45F, e);
      o.lArmY = mix(0.0F, 0.7F, e);
      o.lArmR = 0.0F;
      o.rArmP = mix(0.0F, -0.2F, e);
      o.rArmY = mix(0.0F, 0.55F, e);
      o.rArmR = mix(0.0F, 0.6F, e);
      o.headP = mix(0.0F, 0.18F, e);
      o.rLegP = 0.28F * e;
      o.lLegP = -0.32F * e;
      o.dx = -7.0F * hop - 2.0F * i;
      o.dy = -4.0F * hop;
      o.sx = 1.0F + 0.04F * e + 0.05F * land;
      o.sy = 1.0F - 0.06F * e - 0.06F * land + breathe * 0.012F * i;
      o.tilt = 4.0F * e;
   }

   private static void poseSpare(CloneAnimator.Pose o, float p, float t) {
      float a = easeInOutSine(seg(p, 0.0F, 0.24F));
      float b = easeOutBack(seg(p, 0.26F, 0.5F));
      float d = easeInOutSine(seg(p, 0.8F, 1.0F));
      float keep = 1.0F - d;
      float sway = (float)Math.sin((double)(t * 0.006F)) * seg(p, 0.45F, 0.6F);
      o.armW = keep;
      o.headW = keep;
      float armP = mix(mix(0.0F, 0.1F, a), -0.5F, b);
      float armR = mix(mix(0.0F, 0.12F, a), 1.15F, b) + sway * 0.06F;
      o.rArmP = armP;
      o.rArmY = 0.0F;
      o.rArmR = armR;
      o.lArmP = armP;
      o.lArmY = 0.0F;
      o.lArmR = -armR;
      o.headP = mix(mix(0.0F, 0.6F, a), -0.25F, b);
      o.tilt = (mix(mix(0.0F, 7.0F, a), 0.0F, b) + sway * 1.5F) * keep;
      o.dy = -3.0F * b * keep;
      float sy = 1.0F - 0.05F * a * (1.0F - b) + 0.03F * b;
      o.sy = mix(sy, 1.0F, d);
      o.sx = 1.0F;
   }

   public static void drawFx(DrawContext ctx, CloneAnimator.Frame fr, float feetX, float feetY, float hPx, int dir, boolean front) {
      if (fr != null) {
         float bx = feetX + fr.pose.dx * (float)dir;
         float by = feetY + fr.pose.dy;
         if (fr.hurtT >= 0.0F) {
            fxHurt(ctx, fr, bx, by, hPx, dir, front);
         }

         if (fr.act != CloneAnimator.Act.NONE) {
            switch (fr.act) {
               case BASH_READY:
                  fxReady(ctx, fr, bx, by, hPx, dir, front);
                  break;
               case BASH:
                  fxBash(ctx, fr, feetX, feetY, bx, by, hPx, dir, front);
                  break;
               case CHOOSE_SKILL:
                  fxChooseSkill(ctx, fr, feetY, bx, by, hPx, dir, front);
                  break;
               case SKILL:
                  if (fr.cast != CastAnims.Cast.GENERIC) {
                     CastAnims.fx(ctx, fr, fr.cast, feetX, feetY, hPx, dir, front);
                  } else {
                     fxSkill(ctx, fr, feetY, bx, by, hPx, front);
                  }
                  break;
               case CHOOSE_ITEM:
                  fxChooseItem(ctx, fr, bx, by, hPx, dir, front);
                  break;
               case ITEM:
                  fxItem(ctx, fr, feetY, bx, by, hPx, dir, front);
                  break;
               case DEFEND:
                  fxDefend(ctx, fr, feetY, bx, by, hPx, dir, front);
                  break;
               case SPARE:
                  fxSpare(ctx, fr, bx, by, hPx, front);
                  break;
               case DOWNED:
                  fxDowned(ctx, fr, feetX, feetY, hPx, dir, front);
               case IDLE_ARMED:
               default:
                  break;
               case CHOOSE_ACT:
                  fxChooseAct(ctx, fr, feetY, bx, by, hPx, dir, front);
                  break;
               case ACT:
                  fxAct(ctx, fr, feetY, bx, by, hPx, dir, front);
            }
         }
      }
   }

   private static void fxHurt(DrawContext ctx, CloneAnimator.Frame fr, float bx, float by, float hPx, int dir, boolean front) {
      if (front) {
         float t = fr.hurtT;
         float k = fr.hurtK;
         float q = seg(t, 0.0F, 260.0F);
         if (!(q >= 1.0F)) {
            float ix = bx + (float)dir * hPx * 0.2F;
            float iy = by - hPx * 0.6F;
            float fade = 1.0F - q;
            star4(ctx, ix, iy, (5.5F * (1.0F - easeOutCubic(q)) + 1.0F) * k, argb(16777215, fade));
            quad(ctx, ix, iy, 3.0F * fade * k, 3.0F * fade * k, (float) (Math.PI / 4), argb(16730698, 0.9F * fade));

            for (int i = 0; i < 4; i++) {
               float ang = (float) Math.PI + ((float)i - 1.5F) * 0.5F;
               float cs = (float)Math.cos((double)ang) * (float)dir;
               float sn = (float)Math.sin((double)ang);
               float r0 = 3.0F + easeOutCubic(q) * hPx * 0.16F;
               float r1 = r0 + hPx * 0.12F * fade * k;
               line(ctx, ix + cs * r0, iy + sn * r0, ix + cs * r1, iy + sn * r1, 1.0F, argb(i % 2 == 0 ? 16777215 : 16730698, 0.85F * fade));
            }

            for (int i = 0; i < 3; i++) {
               float ang = (float) Math.PI * (0.85F + 0.3F * rnd(i * 13 + 1)) + (float)(i - 1) * 0.35F;
               float d = easeOutCubic(q) * hPx * (0.22F + 0.1F * rnd(i + 4));
               float x = ix + (float)Math.cos((double)ang) * (float)dir * d;
               float y = iy + (float)Math.sin((double)ang) * d - 4.0F * q + 9.0F * q * q;
               quad(ctx, x, y, 1.6F, 1.6F, q * 6.0F + (float)i, argb(i == 1 ? 16777215 : 16730698, fade));
            }
         }
      }
   }

   private static void glyph(DrawContext ctx, int g, float cx, float cy, float px, int argb) {
      String[] rows = GLYPH[g];

      for (int r = 0; r < 5; r++) {
         for (int c = 0; c < 5; c++) {
            if (rows[r].charAt(c) == '#') {
               quad(ctx, cx + (float)(c - 2) * px, cy + (float)(r - 2) * px, px, px, 0.0F, argb);
            }
         }
      }
   }

   private static void fxChooseAct(DrawContext ctx, CloneAnimator.Frame fr, float feetY, float bx, float by, float hPx, int dir, boolean front) {
      float t = fr.t;
      float appear = clamp01(t / 260.0F);
      float rr = hPx * 0.36F * appear;
      ringHalf(ctx, bx, feetY, rr, rr * 0.3F, 1.0F, argb(16753722, 0.5F * appear), -t * 9.0E-4F, true, front);
      if (front) {
         float pop = easeOutBack(clamp01((t - 120.0F) / 280.0F));
         if (!(pop <= 0.0F)) {
            float cyc = t % 2700.0F / 2700.0F;
            int g = cyc < 0.33F ? 2 : (cyc < 0.7F ? 0 : 1);
            float hop = g == 1 ? (float)Math.sin(Math.PI * (double)seg(cyc, 0.7F, 0.8F)) * 3.0F : 0.0F;
            float bob = (float)Math.sin((double)(t * 0.004F)) * 1.2F;
            float ox = bx + (float)dir * hPx * 0.26F;
            float oy = by - hPx * 1.18F + bob - hop;

            for (int i = 0; i < 3; i++) {
               float f = (float)(i + 1) / 4.0F;
               float px = mix(bx + (float)dir * hPx * 0.06F, ox, f);
               float py = mix(by - hPx * 0.98F, oy, f);
               float sz = (1.4F + (float)i * 0.9F) * pop;
               quad(ctx, px, py, sz + 1.2F, sz + 1.2F, 0.0F, argb(0, 0.5F));
               quad(ctx, px, py, sz, sz, 0.0F, argb(16777215, 0.95F));
            }

            float w = 17.0F * pop;
            float h = 13.0F * pop;
            quad(ctx, ox, oy, w + 2.0F, h, 0.0F, argb(0, 0.55F));
            quad(ctx, ox, oy, w, h + 2.0F, 0.0F, argb(0, 0.55F));
            quad(ctx, ox, oy, w, h - 2.0F, 0.0F, argb(16777215, 0.97F));
            quad(ctx, ox, oy, w - 2.0F, h, 0.0F, argb(16777215, 0.97F));
            if (pop > 0.6F) {
               glyph(ctx, g, ox, oy, 1.6F, argb(g == 1 ? 16753722 : 3158090, 1.0F));
            }

            if (g == 1) {
               float sp = seg(cyc, 0.7F, 0.85F);
               if (sp > 0.0F && sp < 1.0F) {
                  for (int i = 0; i < 4; i++) {
                     float ang = -0.4F - (float)i * 0.75F;
                     float r0 = 10.0F + sp * 5.0F;
                     float r1 = r0 + 3.0F * (1.0F - sp);
                     float cs = (float)Math.cos((double)ang);
                     float sn = (float)Math.sin((double)ang);
                     line(ctx, ox + cs * r0, oy + sn * r0, ox + cs * r1, oy + sn * r1, 1.0F, argb(16753722, 1.0F - sp));
                  }
               }
            }
         }
      }
   }

   private static void fxAct(DrawContext ctx, CloneAnimator.Frame fr, float feetY, float bx, float by, float hPx, int dir, boolean front) {
      float p = fr.p;
      float t = fr.t;
      float gq = seg(p, 0.26F, 0.6F);
      if (gq > 0.0F && gq < 1.0F) {
         float r = hPx * (0.15F + 0.5F * easeOutCubic(gq));
         ringHalf(ctx, bx, feetY, r, r * 0.3F, 1.3F, argb(16753722, 0.8F * (1.0F - gq)), 0.0F, false, front);
      }

      if (!front) {
         float q = seg(p, 0.14F, 0.4F);
         if (q > 0.0F && q < 1.0F) {
            for (int i = 0; i < 3; i++) {
               float y = by - hPx * (0.4F + 0.14F * (float)i);
               float len = hPx * (0.28F + 0.1F * (float)(i % 2)) * (1.0F - q) + 2.0F;
               float x1 = bx - (float)dir * hPx * 0.16F;
               line(ctx, x1 - (float)dir * len, y, x1, y, 1.0F, argb(16777215, 0.5F * (1.0F - q)));
            }
         }
      } else {
         float fx = bx + (float)dir * hPx * 0.46F;
         float fy = by - hPx * 0.7F;
         float bq = seg(p, 0.27F, 0.5F);
         if (bq > 0.0F && bq < 1.0F) {
            float fade = 1.0F - bq;

            for (int i = 0; i < 8; i++) {
               float ang = (float)i * (float) (Math.PI / 4) + 0.2F;
               float cs = (float)Math.cos((double)ang);
               float sn = (float)Math.sin((double)ang);
               float r0 = 2.0F + easeOutCubic(bq) * hPx * 0.14F;
               float r1 = r0 + hPx * 0.1F * fade;
               line(ctx, fx + cs * r0, fy + sn * r0, fx + cs * r1, fy + sn * r1, 1.1F, argb(i % 2 == 0 ? 16777215 : 16753722, fade));
            }

            star4(ctx, fx, fy, 5.0F * fade + 1.0F, argb(16777215, fade));
         }

         for (int i = 0; i < 4; i++) {
            float q = seg(p, 0.28F + (float)i * 0.06F, 0.62F + (float)i * 0.06F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float x = fx + (float)dir * (4.0F + easeOutCubic(q) * hPx * 1.6F);
               float a = (float)Math.sin(Math.PI * (double)q) * (1.0F - q * 0.4F);
               float s = 3.2F + 1.2F * (1.0F - q);
               float tx = x + (float)dir * s * 0.7F;
               line(ctx, x - (float)dir * s * 0.3F, fy - s, tx, fy, 1.6F, argb(16753722, a));
               line(ctx, tx, fy, x - (float)dir * s * 0.3F, fy + s, 1.6F, argb(16753722, a));
               line(ctx, x - (float)dir * s * 0.3F - (float)dir * 0.6F, fy - s * 0.6F, tx - (float)dir * 0.8F, fy, 0.7F, argb(16777215, a));
            }
         }

         float eq = seg(p, 0.24F, 0.78F);
         if (eq > 0.0F && eq < 1.0F) {
            float pop = easeOutBack(seg(eq, 0.0F, 0.25F));
            float a = 1.0F - seg(eq, 0.75F, 1.0F);
            float ex = bx + (float)dir * hPx * 0.08F;
            float ey = by - hPx * 1.22F - 3.0F * pop;
            float px = 1.9F * pop;
            glyph(ctx, 1, ex + 0.7F, ey + 0.7F, px, argb(0, 0.55F * a));
            glyph(ctx, 1, ex, ey, px, argb(16753722, a));
            quad(ctx, ex, ey - px * 2.0F, px * 0.6F, px * 0.6F, 0.0F, argb(16777215, 0.8F * a));
         }
      }
   }

   private static void fxDowned(DrawContext ctx, CloneAnimator.Frame fr, float feetX, float feetY, float hPx, int dir, boolean front) {
      float t = fr.t;
      float fall = seg(t, 200.0F, 620.0F);
      float bodyX = feetX + fr.pose.dx * (float)dir - (float)dir * hPx * 0.48F * fall;
      if (!front) {
         float sa = 0.28F * fall;

         for (int i = 0; i < 3; i++) {
            float w = hPx * (1.05F - (float)i * 0.18F) * fall;
            float h = 4.0F - (float)i;
            quad(ctx, bodyX, feetY - 0.5F, w, h, 0.0F, argb(0, sa * 0.5F));
         }
      } else {
         float dq = seg(t, 600.0F, 1150.0F);
         if (dq > 0.0F && dq < 1.0F) {
            for (int i = 0; i < 7; i++) {
               float along = ((float)i / 6.0F - 0.5F) * hPx * 0.9F;
               float x = bodyX + along + (rnd(i * 5) - 0.5F) * 4.0F;
               float y = feetY - 2.0F - dq * (3.0F + 4.0F * rnd(i + 3));
               float sz = 2.0F + dq * (3.0F + 2.0F * rnd(i));
               quad(ctx, x, y, sz, sz, (float)i * 0.8F + dq, argb(13617340, 0.5F * (1.0F - dq)));
            }
         }

         float rest = seg(t, 1200.0F, 1800.0F);
         if (rest > 0.0F) {
            for (int i = 0; i < 3; i++) {
               float f = (t * 3.5E-4F + (float)i / 3.0F) % 1.0F;
               float x = bodyX + (rnd(i * 7) - 0.5F) * hPx * 0.6F + (float)Math.sin((double)(t * 0.002F + (float)i)) * 2.0F;
               float y = feetY - 6.0F - f * hPx * 0.5F;
               quad(ctx, x, y, 1.6F, 1.6F, 0.0F, argb(12109024, 0.45F * rest * (float)Math.sin(Math.PI * (double)f)));
            }
         }
      }
   }

   private static void fxReady(DrawContext ctx, CloneAnimator.Frame fr, float bx, float by, float hPx, int dir, boolean front) {
      if (front) {
         float t = fr.t;
         float charge = clamp01(t / 500.0F);
         float hx = bx - (float)dir * hPx * 0.08F;
         float hy = by - hPx * 1.08F;

         for (int i = 0; i < 8; i++) {
            float f = (t + (float)i * 77.0F) % 620.0F / 620.0F;
            float ang = (float)i * 0.785F + rnd(i) * 0.6F + f * 1.2F;
            float r = hPx * 0.42F * (1.0F - f) * (1.0F - f);
            float x = hx + (float)Math.cos((double)ang) * r;
            float y = hy + (float)Math.sin((double)ang) * r;
            quad(ctx, x, y, 1.6F, 1.6F, 0.0F, argb(fr.color, f * 0.95F * charge));
         }

         float tw = 2.2F + (float)Math.sin((double)(t * 0.02F)) * 0.9F;
         star4(ctx, hx, hy, tw * charge, argb(16777215, 0.85F * charge));
      }
   }

   private static void fxBash(DrawContext ctx, CloneAnimator.Frame fr, float feetX, float feetY, float bx, float by, float hPx, int dir, boolean front) {
      float p = fr.p;
      if (!front) {
         float q = seg(p, 0.16F, 0.46F);
         if (q > 0.0F && q < 1.0F) {
            for (int i = 0; i < 4; i++) {
               float y = by - hPx * (0.22F + 0.18F * (float)i);
               float len = hPx * (0.35F + 0.12F * (float)(i % 2)) * (1.0F - q) + 2.0F;
               float x1 = bx - (float)dir * hPx * 0.18F;
               float x0 = x1 - (float)dir * len;
               line(ctx, x0, y, x1, y, 1.0F, argb(16777215, 0.55F * (1.0F - q)));
            }
         }
      } else {
         float dq = seg(p, 0.16F, 0.55F);
         if (dq > 0.0F && dq < 1.0F) {
            for (int i = 0; i < 5; i++) {
               float spread = dq * hPx * (0.18F + 0.07F * (float)i);
               float x = feetX - (float)dir * (2.0F + spread);
               float y = feetY - 1.5F - dq * (2.0F + 2.0F * (float)(i % 3));
               float sz = 1.5F + dq * (2.5F + (float)i * 0.5F);
               quad(ctx, x, y, sz, sz, (float)i * 0.7F + dq, argb(14275270, 0.55F * (1.0F - dq)));
            }
         }

         float lead = easeOutCubic(seg(p, 0.15F, 0.36F));
         float fade = 1.0F - seg(p, 0.36F, 0.52F);
         if (p >= 0.15F && fade > 0.0F) {
            float cx = bx + (float)dir * hPx * 0.12F;
            float cy = by - hPx * 0.55F;
            float rad = hPx * 0.62F;
            float start = -35.0F;
            float thetaLead = mix(start, 165.0F, lead);
            float trail = 115.0F;
            int n = 20;

            for (int k = 0; k < n; k++) {
               float f0 = (float)k / (float)n;
               float f1 = (float)(k + 1) / (float)n;
               float a0 = thetaLead - trail * (1.0F - f0);
               float a1 = thetaLead - trail * (1.0F - f1);
               if (!(a1 < start)) {
                  a0 = Math.max(a0, start);
                  float r0 = rad * (0.9F + 0.1F * f0);
                  float r1 = rad * (0.9F + 0.1F * f1);
                  float x0 = cx + (float)dir * r0 * sinD(a0);
                  float y0 = cy - r0 * cosD(a0);
                  float x1 = cx + (float)dir * r1 * sinD(a1);
                  float y1 = cy - r1 * cosD(a1);
                  float al = f1 * f1 * fade;
                  float th = 1.0F + 3.2F * f1;
                  line(ctx, x0, y0, x1, y1, th + 2.0F, argb(fr.color, al * 0.55F));
                  line(ctx, x0, y0, x1, y1, th, argb(16777215, al));
               }
            }
         }

         float iq = seg(p, 0.31F, 0.52F);
         if (iq > 0.0F && iq < 1.0F) {
            float ix = bx + (float)dir * hPx * 0.62F;
            float iy = by - hPx * 0.42F;

            for (int i = 0; i < 8; i++) {
               float ang = (float)i * (float) (Math.PI / 4) + 0.3F;
               float cs = (float)Math.cos((double)ang);
               float sn = (float)Math.sin((double)ang);
               float r0 = 2.0F + iq * hPx * 0.18F;
               float r1 = r0 + hPx * 0.16F * (1.0F - iq);
               line(ctx, ix + cs * r0, iy + sn * r0, ix + cs * r1, iy + sn * r1, 1.2F, argb(i % 2 == 0 ? 16777215 : fr.color, 1.0F - iq));
            }

            star4(ctx, ix, iy, 6.0F * (1.0F - iq) + 1.0F, argb(16777215, 1.0F - iq));
         }
      }
   }

   private static void fxChooseSkill(DrawContext ctx, CloneAnimator.Frame fr, float feetY, float bx, float by, float hPx, int dir, boolean front) {
      float t = fr.t;
      float appear = clamp01(t / 250.0F);
      float rr = hPx * 0.38F * appear;
      ringHalf(ctx, bx, feetY, rr, rr * 0.3F, 1.0F, argb(fr.color, 0.55F * appear), t * 0.0012F, true, front);
      float ocx = bx;
      float ocy = by - hPx * 0.5F;
      float rx = hPx * 0.42F;
      float ry = rx * 0.28F;

      for (int i = 0; i < 3; i++) {
         for (int g = 2; g >= 0; g--) {
            float ang = t * 0.0034F + (float)i * 2.094F - (float)g * 0.16F;
            float s = (float)Math.sin((double)ang);
            if (s >= 0.0F == front) {
               float x = ocx + (float)Math.cos((double)ang) * rx;
               float y = ocy + s * ry + (float)Math.sin((double)(t * 0.004F + (float)i)) * 2.0F;
               float sz = (2.2F - (float)g * 0.5F) * (0.85F + 0.15F * s);
               float al = g == 0 ? 0.95F : 0.45F - (float)g * 0.12F;
               diamond(ctx, x, y, sz, argb(g == 0 ? 16777215 : fr.color, al * appear));
            }
         }
      }

      if (front) {
         float ox = bx + (float)dir * hPx * 0.2F;
         float oy = by - hPx * 0.62F;
         float pulse = 1.0F + (float)Math.sin((double)(t * 0.008F)) * 0.18F;
         quad(ctx, ox, oy, 7.0F * pulse, 7.0F * pulse, t * 0.002F, argb(fr.color, 0.22F * appear));
         diamond(ctx, ox, oy, 2.6F * pulse, argb(fr.color, 0.75F * appear));
         quad(ctx, ox, oy, 1.6F, 1.6F, 0.0F, argb(16777215, 0.95F * appear));
      }
   }

   private static void fxSkill(DrawContext ctx, CloneAnimator.Frame fr, float feetY, float bx, float by, float hPx, boolean front) {
      float p = fr.p;
      float t = fr.t;
      float circ = easeOutBack(seg(p, 0.0F, 0.2F)) * (1.0F - seg(p, 0.7F, 0.86F));
      if (circ > 0.0F) {
         float r = hPx * 0.5F * circ;
         ringHalf(ctx, bx, feetY, r, r * 0.3F, 1.2F, argb(fr.color, 0.8F * circ), t * 0.003F, false, front);
         ringHalf(ctx, bx, feetY, r * 0.72F, r * 0.72F * 0.3F, 1.0F, argb(16777215, 0.6F * circ), -t * 0.004F, true, front);
      }

      float sq = seg(p, 0.25F, 0.62F);
      if (sq > 0.0F && sq < 1.0F) {
         float r = hPx * (0.15F + 0.85F * easeOutCubic(sq));
         ringHalf(ctx, bx, feetY, r, r * 0.3F, 1.6F, argb(fr.color, 0.85F * (1.0F - sq)), 0.0F, false, front);
      }

      if (!front) {
         float q = seg(p, 0.25F, 0.58F);
         if (q > 0.0F && q < 1.0F) {
            float w = hPx * 0.3F * (1.0F - q) + 2.0F;
            float hgt = hPx * 1.7F;
            quad(ctx, bx, feetY - hgt / 2.0F, w, hgt, 0.0F, argb(fr.color, 0.32F * (1.0F - q)));
            quad(ctx, bx, feetY - hgt / 2.0F, w * 0.35F, hgt, 0.0F, argb(16777215, 0.4F * (1.0F - q)));
         }
      } else {
         float gq = seg(p, 0.0F, 0.26F);
         if (gq < 1.0F) {
            float r = hPx * 0.55F * (1.0F - easeOutCubic(gq));

            for (int i = 0; i < 10; i++) {
               float ang = (float)i * 0.628F + rnd(i + 40) * 0.5F;
               float x = bx + (float)Math.cos((double)ang) * r;
               float y = by - hPx * 0.55F + (float)Math.sin((double)ang) * r * 0.8F;
               quad(ctx, x, y, 1.5F, 1.5F, 0.0F, argb(fr.color, gq * 0.9F));
            }
         }

         for (int i = 0; i < 12; i++) {
            float q = seg(p, 0.27F + rnd(i) * 0.06F, 0.95F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float ang = (float)i * 0.5236F + rnd(i + 9) * 0.3F;
               float dist = 5.0F + easeOutCubic(q) * hPx * (0.55F + rnd(i + 20) * 0.25F);
               float x = bx + (float)Math.cos((double)ang) * dist;
               float y = by - hPx * 0.55F + (float)Math.sin((double)ang) * dist * 0.75F - q * 8.0F;
               star4(ctx, x, y, 2.6F * (1.0F - q) + 0.6F, argb(i % 3 == 0 ? 16777215 : fr.color, 1.0F - q));
            }
         }
      }
   }

   private static void fxChooseItem(DrawContext ctx, CloneAnimator.Frame fr, float bx, float by, float hPx, int dir, boolean front) {
      if (front) {
         float t = fr.t;
         float appear = clamp01(t / 200.0F);
         float hx = bx - (float)dir * 2.0F;
         float hy = by - hPx - 7.0F;

         for (int i = 0; i < 3; i++) {
            float bounce = Math.max(0.0F, (float)Math.sin((double)(t * 0.008F - (float)i * 0.9F))) * 3.0F;
            float x = hx + (float)(i - 1) * 5.0F;
            float y = hy - bounce;
            quad(ctx, x, y, 3.2F, 3.2F, 0.0F, argb(0, 0.55F * appear));
            quad(ctx, x, y, 2.0F, 2.0F, 0.0F, argb(16777215, 0.95F * appear));
         }

         if (fr.handOverride != null && !fr.handOverride.isEmpty()) {
            float tw = (float)Math.pow(Math.max(0.0, Math.sin((double)(t * 0.005F))), 3.0);
            star4(ctx, bx + (float)dir * hPx * 0.16F, by - hPx * 0.42F, 3.2F * tw, argb(16777215, 0.9F * tw));
         }
      }
   }

   private static void fxItem(DrawContext ctx, CloneAnimator.Frame fr, float feetY, float bx, float by, float hPx, int dir, boolean front) {
      float p = fr.p;
      float t = fr.t;
      float env = seg(p, 0.1F, 0.24F) * (1.0F - seg(p, 0.46F, 0.56F));
      float ix = bx - (float)dir * hPx * 0.06F;
      float iy = by - hPx * 1.22F;
      float rq = seg(p, 0.52F, 0.9F);
      if (rq > 0.0F && rq < 1.0F) {
         float r = hPx * (0.2F + 0.4F * rq);
         ringHalf(ctx, bx, feetY, r, r * 0.3F, 1.2F, argb(7143290, 0.7F * (1.0F - rq)), 0.0F, false, front);
      }

      if (front) {
         if (env > 0.0F) {
            star4(ctx, ix, iy, (3.0F + (float)Math.sin((double)(t * 0.03F))) * env, argb(16777215, env));
         }

         for (int i = 0; i < 8; i++) {
            float q = seg(p, 0.52F + (float)i * 0.035F, 0.82F + (float)i * 0.022F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float x = bx + (rnd(i + 3) - 0.5F) * hPx * 0.75F;
               float y = by - hPx * (0.15F + rnd(i + 11) * 0.6F) - q * hPx * 0.35F;
               float a = (float)Math.sin(Math.PI * (double)q);
               plus(ctx, x, y, 2.4F, argb(7143290, a));
               quad(ctx, x, y, 1.0F, 1.0F, 0.0F, argb(16777215, a));
            }
         }
      } else {
         if (env > 0.0F) {
            for (int ixx = 0; ixx < 10; ixx++) {
               float ang = t * 0.0025F + (float)ixx * 0.628F;
               float cs = (float)Math.cos((double)ang);
               float sn = (float)Math.sin((double)ang);
               float len = (hPx * 0.18F + hPx * 0.12F * (float)(ixx % 2)) * env;
               line(
                  ctx, ix + cs * 3.0F, iy + sn * 3.0F, ix + cs * (3.0F + len), iy + sn * (3.0F + len), ixx % 2 == 0 ? 1.6F : 1.0F, argb(16774320, 0.55F * env)
               );
            }
         }
      }
   }

   private static void fxDefend(DrawContext ctx, CloneAnimator.Frame fr, float feetY, float bx, float by, float hPx, int dir, boolean front) {
      float t = fr.t;
      float intro = clamp01(t / 280.0F);
      float grow = easeOutBack(intro);
      float flash = 1.0F - intro;
      float alpha = Math.max(0.42F + 0.1F * (float)Math.sin((double)(t * 0.005F)), flash);
      float bq = clamp01(t / 380.0F);
      if (bq < 1.0F) {
         float r = hPx * (0.15F + 0.45F * bq);
         ringHalf(ctx, bx, feetY, r, r * 0.3F, 1.3F, argb(9427199, 0.8F * (1.0F - bq)), 0.0F, false, front);
      }

      if (front) {
         float ax = bx + (float)dir * hPx * 0.34F;
         float ay = by - hPx * 0.52F;
         float rx = hPx * 0.15F * grow;
         float ry = hPx * 0.6F * grow;
         int n = 14;
         float sweep = t * 0.0011F % 1.6F - 0.3F;

         for (int k = 0; k < n; k++) {
            float f0 = (float)k / (float)n;
            float f1 = (float)(k + 1) / (float)n;
            float a0 = mix(-1.35F, 1.35F, f0);
            float a1 = mix(-1.35F, 1.35F, f1);
            float x0 = ax + (float)dir * rx * (float)Math.cos((double)a0);
            float y0 = ay + ry * (float)Math.sin((double)a0);
            float x1 = ax + (float)dir * rx * (float)Math.cos((double)a1);
            float y1 = ay + ry * (float)Math.sin((double)a1);
            float mid = (f0 + f1) * 0.5F;
            float hl = Math.max(0.0F, 1.0F - Math.abs(mid - sweep) * 6.0F);
            float edge = (float)Math.sin(Math.PI * (double)mid);
            line(ctx, x0, y0, x1, y1, 3.2F, argb(9427199, alpha * 0.45F * edge));
            line(ctx, x0, y0, x1, y1, 1.2F, argb(16777215, Math.min(1.0F, (alpha * 0.7F + hl * 0.8F) * edge)));
            if (k % 3 == 0 && k > 0) {
               line(ctx, x0, y0, x0 - (float)dir * 3.0F * grow, y0, 1.0F, argb(9427199, alpha * 0.5F * edge));
            }
         }

         for (int i = 0; i < 3; i++) {
            float f = (t * 6.0E-4F + (float)i / 3.0F) % 1.0F;
            float a = mix(-1.2F, 1.2F, f);
            float gx = ax + (float)dir * rx * (float)Math.cos((double)a) - (float)dir * 1.5F;
            float gy = ay + ry * (float)Math.sin((double)a);
            star4(ctx, gx, gy, 1.8F, argb(16777215, 0.8F * (float)Math.sin(Math.PI * (double)f) * grow));
         }
      }
   }

   private static void fxSpare(DrawContext ctx, CloneAnimator.Frame fr, float bx, float by, float hPx, boolean front) {
      float p = fr.p;
      float t = fr.t;
      if (!front) {
         float g = seg(p, 0.22F, 0.4F) * (1.0F - seg(p, 0.75F, 0.95F));
         if (g > 0.0F) {
            float pulse = 0.92F + 0.08F * (float)Math.sin((double)(t * 0.006F));
            glow(ctx, bx, by - hPx * 0.55F, hPx * 0.62F * pulse, t * 5.0E-4F, 16770650, 0.045F * g);
            glow(ctx, bx, by - hPx * 0.55F, hPx * 0.3F * pulse, -t * 8.0E-4F, 16777215, 0.05F * g);
         }
      } else {
         for (int i = 0; i < 10; i++) {
            float start = 0.24F + rnd(i) * 0.3F;
            float q = seg(p, start, start + 0.42F);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float x = bx + (rnd(i + 5) - 0.5F) * hPx * 0.95F + (float)Math.sin((double)(t * 0.004F + (float)i)) * 2.5F;
               float y = by - hPx * (0.25F + rnd(i + 17) * 0.55F) - q * hPx * 0.45F;
               float a = (float)Math.sin(Math.PI * (double)q);
               star4(ctx, x, y, 2.8F * (1.0F - q * 0.4F), argb(16770650, a));
               quad(ctx, x, y, 1.1F, 1.1F, 0.0F, argb(16777215, a));
            }
         }
      }
   }

   static void quad(DrawContext ctx, float cx, float cy, float w, float h, float rot, int argb) {
      if ((argb >>> 24 & 0xFF) != 0 && !(w <= 0.01F) && !(h <= 0.01F)) {
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(cx, cy, 0.0F);
         if (rot != 0.0F) {
            m.multiply(new Quaternionf().rotationZ(rot));
         }

         m.scale(w / 2.0F, h / 2.0F, 1.0F);
         ctx.fill(RenderLayer.getGuiOverlay(), -1, -1, 1, 1, argb);
         m.pop();
      }
   }

   static void line(DrawContext ctx, float x0, float y0, float x1, float y1, float th, int argb) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len < 0.01F)) {
         quad(ctx, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F, len + th * 0.5F, th, (float)Math.atan2((double)dy, (double)dx), argb);
      }
   }

   static void diamond(DrawContext ctx, float cx, float cy, float r, int argb) {
      quad(ctx, cx, cy, r * 1.414F, r * 1.414F, (float) (Math.PI / 4), argb);
   }

   static void star4(DrawContext ctx, float cx, float cy, float r, int argb) {
      if (!(r <= 0.05F)) {
         float thin = Math.max(0.45F, r * 0.2F);
         quad(ctx, cx, cy, r * 2.2F, thin, 0.0F, argb);
         quad(ctx, cx, cy, thin, r * 2.2F, 0.0F, argb);
         diamond(ctx, cx, cy, Math.max(0.5F, r * 0.42F), argb);
      }
   }

   static void glow(DrawContext ctx, float cx, float cy, float radius, float spin, int rgb, float alpha) {
      if (!(alpha <= 0.003F) && !(radius < 0.5F)) {
         for (int ring = 0; ring < 4; ring++) {
            float size = radius * (0.45F + (float)ring * 0.2F) * 1.6F;

            for (int k = 0; k < 3; k++) {
               quad(ctx, cx, cy, size, size, spin + (float)k * 0.5236F, argb(rgb, alpha));
            }
         }
      }
   }

   static void plus(DrawContext ctx, float cx, float cy, float r, int argb) {
      quad(ctx, cx, cy, r * 2.0F, r * 0.75F, 0.0F, argb);
      quad(ctx, cx, cy, r * 0.75F, r * 2.0F, 0.0F, argb);
   }

   static void ringHalf(DrawContext ctx, float cx, float cy, float rx, float ry, float th, int argb, float rot, boolean dashed, boolean front) {
      if (!(rx < 0.5F) && (argb >>> 24 & 0xFF) != 0) {
         int n = 32;
         float step = (float)((Math.PI * 2) / (double)n);

         for (int k = 0; k < n; k++) {
            float a0 = rot + (float)k * step;
            float a1 = a0 + step * (dashed ? 0.55F : 1.0F);
            float mid = (a0 + a1) * 0.5F;
            if (Math.sin((double)mid) >= 0.0 == front) {
               line(
                  ctx,
                  cx + (float)Math.cos((double)a0) * rx,
                  cy + (float)Math.sin((double)a0) * ry,
                  cx + (float)Math.cos((double)a1) * rx,
                  cy + (float)Math.sin((double)a1) * ry,
                  th,
                  argb
               );
            }
         }
      }
   }

   static int argb(int rgb, float a) {
      int al = (int)(clamp01(a) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   static float clamp01(float x) {
      return x < 0.0F ? 0.0F : (x > 1.0F ? 1.0F : x);
   }

   static float seg(float p, float a, float b) {
      return clamp01((p - a) / (b - a));
   }

   static float mix(float a, float b, float t) {
      return a + (b - a) * t;
   }

   static float sinD(float deg) {
      return (float)Math.sin(Math.toRadians((double)deg));
   }

   static float cosD(float deg) {
      return (float)Math.cos(Math.toRadians((double)deg));
   }

   static float easeOutCubic(float x) {
      float u = 1.0F - x;
      return 1.0F - u * u * u;
   }

   static float easeInOutSine(float x) {
      return (float)(-(Math.cos(Math.PI * (double)x) - 1.0) / 2.0);
   }

   static float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float u = x - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   static float rnd(int i) {
      int h = i * -1640531535;
      h ^= h >>> 15;
      h *= -2048144777;
      h ^= h >>> 13;
      return (float)(h & 16777215) / 1.6777216E7F;
   }

   static float jitter(float t, int salt) {
      return rnd((int)(t / 45.0F) * 31 + salt * 977) * 2.0F - 1.0F;
   }

   public static enum Act {
      NONE,
      BASH_READY,
      BASH,
      CHOOSE_SKILL,
      SKILL,
      CHOOSE_ITEM,
      ITEM,
      DEFEND,
      SPARE,
      DOWNED,
      IDLE_ARMED,
      CHOOSE_ACT,
      ACT,
      COMBO;
   }

   public static final class Frame {
      public final CloneAnimator.Act act;
      public final float t;
      public final float p;
      public final CloneAnimator.Pose pose;
      public final int color;
      public final ItemStack handOverride;
      public final boolean defending;
      public final float hurtT;
      public final float hurtK;
      public final CastAnims.Cast cast;
      SoulType soul;

      Frame(CloneAnimator.Act act, float t, float p, CloneAnimator.Pose pose, int color, ItemStack handOverride, boolean defending, float hurtT, float hurtK) {
         this(act, t, p, pose, color, handOverride, defending, hurtT, hurtK, CastAnims.Cast.GENERIC);
      }

      Frame(
         CloneAnimator.Act act,
         float t,
         float p,
         CloneAnimator.Pose pose,
         int color,
         ItemStack handOverride,
         boolean defending,
         float hurtT,
         float hurtK,
         CastAnims.Cast cast
      ) {
         this.act = act;
         this.t = t;
         this.p = p;
         this.pose = pose;
         this.color = color;
         this.handOverride = handOverride;
         this.defending = defending;
         this.hurtT = hurtT;
         this.hurtK = hurtK;
         this.cast = cast == null ? CastAnims.Cast.GENERIC : cast;
      }

      public boolean hurtFlash() {
         return this.hurtT >= 0.0F && this.hurtT < 140.0F;
      }
   }

   public static final class Pose {
      float dx;
      float dy;
      float tilt;
      float sx = 1.0F;
      float sy = 1.0F;
      float dim;
      float armW;
      float rArmP;
      float rArmY;
      float rArmR;
      float lArmP;
      float lArmY;
      float lArmR;
      float headW;
      float headP;
      float headY;
      float bodyW;
      float bodyY;
      float legW;
      float rLegP;
      float lLegP;
      float rArmPX;
      float rArmPY;
      float rArmPZ;
      float bladeRoll;
      float bladeTilt;
      float idleW;

      CloneAnimator.Pose copy() {
         CloneAnimator.Pose o = new CloneAnimator.Pose();
         o.dx = this.dx;
         o.dy = this.dy;
         o.tilt = this.tilt;
         o.sx = this.sx;
         o.sy = this.sy;
         o.dim = this.dim;
         o.armW = this.armW;
         o.rArmP = this.rArmP;
         o.rArmY = this.rArmY;
         o.rArmR = this.rArmR;
         o.lArmP = this.lArmP;
         o.lArmY = this.lArmY;
         o.lArmR = this.lArmR;
         o.headW = this.headW;
         o.headP = this.headP;
         o.headY = this.headY;
         o.bodyW = this.bodyW;
         o.bodyY = this.bodyY;
         o.legW = this.legW;
         o.rLegP = this.rLegP;
         o.lLegP = this.lLegP;
         o.rArmPX = this.rArmPX;
         o.rArmPY = this.rArmPY;
         o.rArmPZ = this.rArmPZ;
         o.bladeRoll = this.bladeRoll;
         o.bladeTilt = this.bladeTilt;
         o.idleW = this.idleW;
         return o;
      }

      static CloneAnimator.Pose lerp(CloneAnimator.Pose a, CloneAnimator.Pose b, float t) {
         CloneAnimator.Pose o = new CloneAnimator.Pose();
         o.dx = CloneAnimator.mix(a.dx, b.dx, t);
         o.dy = CloneAnimator.mix(a.dy, b.dy, t);
         o.tilt = CloneAnimator.mix(a.tilt, b.tilt, t);
         o.dim = CloneAnimator.mix(a.dim, b.dim, t);
         o.sx = CloneAnimator.mix(a.sx, b.sx, t);
         o.sy = CloneAnimator.mix(a.sy, b.sy, t);
         o.armW = CloneAnimator.mix(a.armW, b.armW, t);
         o.rArmP = CloneAnimator.mix(a.rArmP, b.rArmP, t);
         o.rArmY = CloneAnimator.mix(a.rArmY, b.rArmY, t);
         o.rArmR = CloneAnimator.mix(a.rArmR, b.rArmR, t);
         o.lArmP = CloneAnimator.mix(a.lArmP, b.lArmP, t);
         o.lArmY = CloneAnimator.mix(a.lArmY, b.lArmY, t);
         o.lArmR = CloneAnimator.mix(a.lArmR, b.lArmR, t);
         o.headW = CloneAnimator.mix(a.headW, b.headW, t);
         o.headP = CloneAnimator.mix(a.headP, b.headP, t);
         o.headY = CloneAnimator.mix(a.headY, b.headY, t);
         o.bodyW = CloneAnimator.mix(a.bodyW, b.bodyW, t);
         o.bodyY = CloneAnimator.mix(a.bodyY, b.bodyY, t);
         o.legW = CloneAnimator.mix(a.legW, b.legW, t);
         o.rLegP = CloneAnimator.mix(a.rLegP, b.rLegP, t);
         o.lLegP = CloneAnimator.mix(a.lLegP, b.lLegP, t);
         o.rArmPX = CloneAnimator.mix(a.rArmPX, b.rArmPX, t);
         o.rArmPY = CloneAnimator.mix(a.rArmPY, b.rArmPY, t);
         o.rArmPZ = CloneAnimator.mix(a.rArmPZ, b.rArmPZ, t);
         o.bladeRoll = CloneAnimator.mix(a.bladeRoll, b.bladeRoll, t);
         o.bladeTilt = CloneAnimator.mix(a.bladeTilt, b.bladeTilt, t);
         o.idleW = CloneAnimator.mix(a.idleW, b.idleW, t);
         return o;
      }

      public boolean movesOnScreen() {
         return Math.abs(this.dx) > 0.01F
            || Math.abs(this.dy) > 0.01F
            || Math.abs(this.tilt) > 0.01F
            || Math.abs(this.sx - 1.0F) > 0.001F
            || Math.abs(this.sy - 1.0F) > 0.001F;
      }

      public float dx() {
         return this.dx;
      }

      public float dy() {
         return this.dy;
      }

      public float tilt() {
         return this.tilt;
      }

      public float sx() {
         return this.sx;
      }

      public float sy() {
         return this.sy;
      }

      public float dim() {
         return this.dim;
      }

      public float idleW() {
         return this.idleW;
      }
   }

   private static final class Track {
      CloneAnimator.Act held = CloneAnimator.Act.NONE;
      long heldStart;
      long heldExpire;
      int heldColor = 16777215;
      ItemStack heldItem = ItemStack.EMPTY;
      CloneAnimator.Act shot = CloneAnimator.Act.NONE;
      int shotVariant;
      CastAnims.Cast shotCast = CastAnims.Cast.GENERIC;
      long shotStart;
      int shotColor = 16777215;
      ItemStack shotItem = ItemStack.EMPTY;
      CloneAnimator.Act shown = CloneAnimator.Act.NONE;
      CloneAnimator.Pose last = new CloneAnimator.Pose();
      CloneAnimator.Pose from;
      long blendStart;
      long blendDur;
      boolean lastTurn;
      boolean downed;
      boolean hpSeen;
      long downedStart;
      float lastHp = Float.NaN;
      long hurtStart;
      float hurtK = 1.0F;
      SoulType soul;
      final float idlePhase;

      Track(UUID key) {
         this.idlePhase = (float)(key.hashCode() & 65535) * 0.37F;
      }
   }
}
