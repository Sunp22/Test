package com.pixelcite.turnbattle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class OverhaulView {
   private static boolean active;
   private static OverhaulView.Shot shot = OverhaulView.Shot.ACTOR;
   private static Entity actor;
   private static Entity target;
   private static final List<Entity> party = new ArrayList<>();
   private static final List<Entity> enemies = new ArrayList<>();
   private static final Set<UUID> poseKeys = new HashSet<>();
   private static UUID activeKey;
   private static int introHold;
   private static boolean camInit;
   private static Vec3d camPos = Vec3d.ZERO;
   private static Vec3d prevCamPos = Vec3d.ZERO;
   private static float camYaw;
   private static float camPitch;
   private static float prevCamYaw;
   private static float prevCamPitch;
   private static final Matrix4f VIEW = new Matrix4f();
   private static final Matrix4f PROJ = new Matrix4f();
   private static Vec3d capturedCam = Vec3d.ZERO;
   private static boolean captured;
   private static final float STAGE_PX_PER_BLOCK = 29.6F;
   private static final float STEP_FORWARD = 0.6F;
   private static final Map<UUID, Float> steps = new HashMap<>();
   private static final Map<UUID, Float> prevSteps = new HashMap<>();
   private static final Map<UUID, Long> liveWatched = new HashMap<>();
   private static final ArrayDeque<OverhaulView.Swap> swaps = new ArrayDeque<>();
   private static final Map<UUID, OverhaulView.FxEntry> fxQueue = new HashMap<>();
   private static final Map<UUID, Long> watched = new HashMap<>();
   private static final List<OverhaulView.WatchFx> watchFx = new ArrayList<>();

   private OverhaulView() {
   }

   public static boolean isActive() {
      return active;
   }

   public static boolean isBattleEnemy(Entity e) {
      return active && e != null && enemies.contains(e);
   }

   static void begin() {
      if (!active) {
         active = true;
         camInit = false;
         shot = OverhaulView.Shot.INTRO;
         steps.clear();
         prevSteps.clear();
      }
   }

   static void end() {
      if (active) {
         active = false;
         camInit = false;
         introHold = 0;
         actor = null;
         target = null;
         party.clear();
         enemies.clear();
         poseKeys.clear();
         steps.clear();
         prevSteps.clear();
         fxQueue.clear();
         activeKey = null;
      }
   }

   static void setScene(List<Entity> partyEntities, List<Entity> enemyEntities, Collection<UUID> keys, UUID turnKey) {
      party.clear();
      party.addAll(partyEntities);
      enemies.clear();
      enemies.addAll(enemyEntities);
      poseKeys.clear();
      poseKeys.addAll(keys);
      activeKey = turnKey;
   }

   static void setShot(OverhaulView.Shot s, Entity actorEntity, Entity targetEntity) {
      if (shot != OverhaulView.Shot.INTRO || s == OverhaulView.Shot.INTRO || introHold <= 0) {
         shot = s;
         actor = actorEntity;
         target = targetEntity;
      }
   }

   static void startIntro() {
      shot = OverhaulView.Shot.INTRO;
      introHold = 30;
   }

   static UUID animKeyFor(Entity e) {
      if (e instanceof BattleRecruitEntity r) {
         return BattleEntityRenderer.recruitKey(r.getRecruitName());
      } else if (e instanceof BattleEntityRenderer.RecruitFakePlayer f) {
         return BattleEntityRenderer.recruitKey(f.recruitName);
      } else {
         return e instanceof PlayerEntity p ? p.getUuid() : null;
      }
   }

   public static boolean cameraActive() {
      return active && camInit;
   }

   public static Vec3d cameraPos(float td) {
      return prevCamPos.lerp(camPos, (double)td);
   }

   public static float cameraYaw(float td) {
      return prevCamYaw + MathHelper.wrapDegrees(camYaw - prevCamYaw) * td;
   }

   public static float cameraPitch(float td) {
      return MathHelper.lerp(td, prevCamPitch, camPitch);
   }

   static void tickCamera() {
      if (active) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            if (!camInit) {
               camPos = mc.player.getCameraPosVec(1.0F);
               camYaw = mc.player.getYaw();
               camPitch = mc.player.getPitch();
               prevCamPos = camPos;
               prevCamYaw = camYaw;
               prevCamPitch = camPitch;
               camInit = true;
            }

            prevCamPos = camPos;
            prevCamYaw = camYaw;
            prevCamPitch = camPitch;
            if (introHold > 0) {
               introHold--;
            }

            Vec3d[] goal = goal(mc);
            if (goal != null) {
               float moveRate = shot == OverhaulView.Shot.INTRO ? 0.09F : 0.2F;
               camPos = camPos.lerp(goal[0], (double)moveRate);
               float[] want = lookAngles(camPos, goal[1]);
               float turnRate = shot == OverhaulView.Shot.INTRO ? 0.18F : 0.35F;
               camYaw = camYaw + MathHelper.wrapDegrees(want[0] - camYaw) * turnRate;
               camPitch = camPitch + (want[1] - camPitch) * turnRate;
               prevSteps.clear();
               prevSteps.putAll(steps);

               for (UUID k : poseKeys) {
                  float wantStep = 0.0F;
                  float cur = steps.getOrDefault(k, 0.0F);
                  steps.put(k, cur + (wantStep - cur) * 0.25F);
               }
            }
         }
      }
   }

   private static Vec3d[] goal(MinecraftClient mc) {
      Entity a = (Entity)(actor != null && actor.isAlive() ? actor : mc.player);
      Vec3d aPos = a.getPos();
      Vec3d c = center(enemies);
      Vec3d fwd;
      if (c != null) {
         fwd = horizontal(c.subtract(aPos));
      } else {
         float y = a.getYaw() * (float) (Math.PI / 180.0);
         fwd = new Vec3d((double)(-MathHelper.sin(y)), 0.0, (double)MathHelper.cos(y));
         c = aPos.add(fwd.multiply(5.0));
      }

      Vec3d right = new Vec3d(-fwd.z, 0.0, fwd.x);
      Vec3d pos;
      Vec3d look;
      Vec3d focus;
      switch (shot) {
         case TARGET:
            if (target == null || !target.isAlive()) {
               return shotActor(aPos, c, fwd, right, mc);
            }

            Vec3d t = target.getPos();
            Vec3d d = horizontal(t.subtract(aPos));
            Vec3d r = new Vec3d(-d.z, 0.0, d.x);
            pos = t.subtract(d.multiply(3.4)).add(r.multiply(1.6)).add(0.0, 1.6 + (double)target.getHeight() * 0.3, 0.0);
            look = t.add(0.0, (double)target.getHeight() * 0.55, 0.0);
            focus = look;
            break;
         case ALLY:
            if (target == null || !target.isAlive()) {
               return shotActor(aPos, c, fwd, right, mc);
            }

            Vec3d l = target.getPos();
            pos = l.add(fwd.multiply(2.6)).add(right.multiply(1.3)).add(0.0, 1.5, 0.0);
            look = l.add(0.0, 1.2, 0.0);
            focus = look;
            break;
         case ENEMY_TURN:
            Vec3d p = center(party);
            if (p == null) {
               p = aPos;
            }

            pos = p.subtract(fwd.multiply(3.6)).add(right.multiply(0.5)).add(0.0, 2.9, 0.0);
            look = p.lerp(c, 0.62).add(0.0, 0.8, 0.0);
            focus = p.add(0.0, 1.6, 0.0);
            break;
         default:
            Vec3d lookAt = shot == OverhaulView.Shot.ACTOR && target != null && target.isAlive() ? target.getPos() : c;
            return shotActor(aPos, lookAt, fwd, right, mc);
      }

      return new Vec3d[]{clip(mc, focus, pos), look};
   }

   private static boolean isChoosing(CloneAnimator.Act a) {
      return a == CloneAnimator.Act.BASH_READY
         || a == CloneAnimator.Act.CHOOSE_SKILL
         || a == CloneAnimator.Act.CHOOSE_ITEM
         || a == CloneAnimator.Act.CHOOSE_ACT;
   }

   private static Vec3d[] shotActor(Vec3d aPos, Vec3d c, Vec3d fwd, Vec3d right, MinecraftClient mc) {
      Vec3d pos = aPos.add(0.0, 1.85, 0.0).subtract(fwd.multiply(2.4)).add(right.multiply(1.35));
      Vec3d look = aPos.add(0.0, 1.5, 0.0).lerp(c.add(0.0, 0.9, 0.0), 0.9);
      return new Vec3d[]{clip(mc, aPos.add(0.0, 1.5, 0.0), pos), look};
   }

   private static Vec3d clip(MinecraftClient mc, Vec3d from, Vec3d to) {
      BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      if (hit.getType() != Type.BLOCK) {
         return to;
      } else {
         Vec3d dir = to.subtract(from);
         double len = dir.length();
         return len < 0.001 ? to : hit.getPos().subtract(dir.multiply(0.3 / len));
      }
   }

   private static Vec3d center(List<Entity> list) {
      double x = 0.0;
      double y = 0.0;
      double z = 0.0;
      int n = 0;

      for (Entity e : list) {
         if (e != null && e.isAlive()) {
            x += e.getX();
            y += e.getY();
            z += e.getZ();
            n++;
         }
      }

      return n == 0 ? null : new Vec3d(x / (double)n, y / (double)n, z / (double)n);
   }

   private static Vec3d horizontal(Vec3d v) {
      Vec3d h = new Vec3d(v.x, 0.0, v.z);
      return h.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : h.normalize();
   }

   private static float[] lookAngles(Vec3d from, Vec3d to) {
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double h = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, h) * (180.0 / Math.PI)));
      return new float[]{yaw, pitch};
   }

   static void captureWorld(WorldRenderContext ctx) {
      VIEW.identity().rotate(new Quaternionf(ctx.camera().getRotation()).conjugate());
      PROJ.set(ctx.projectionMatrix());
      capturedCam = ctx.camera().getPos();
      captured = true;
   }

   static float[] project(Vec3d p) {
      if (captured && p != null) {
         Vector4f v = new Vector4f((float)(p.x - capturedCam.x), (float)(p.y - capturedCam.y), (float)(p.z - capturedCam.z), 1.0F);
         VIEW.transform(v);
         PROJ.transform(v);
         if (v.w < 0.05F) {
            return null;
         } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            float sw = (float)mc.getWindow().getScaledWidth();
            float sh = (float)mc.getWindow().getScaledHeight();
            float nx = v.x / v.w;
            float ny = v.y / v.w;
            return new float[]{(nx * 0.5F + 0.5F) * sw, (0.5F - ny * 0.5F) * sh, PROJ.m11() * 0.5F * sh / v.w};
         }
      } else {
         return null;
      }
   }

   static float[] projectEntity(Entity e, float heightFrac, float td) {
      if (e == null) {
         return null;
      } else {
         Vec3d p = e.getLerpedPos(td).add(0.0, (double)(e.getHeight() * heightFrac), 0.0);
         return project(p);
      }
   }

   static float effectScale(float pixelsPerBlock) {
      return MathHelper.clamp(pixelsPerBlock / 37.0F, 0.75F, 2.2F);
   }

   private static UUID worldPoseKey(LivingEntity e) {
      UUID key = animKeyFor(e);
      if (key == null) {
         return null;
      } else if (active && poseKeys.contains(key)) {
         return key;
      } else {
         Long until = watched.get(key);
         if (until != null && until > System.currentTimeMillis()) {
            return key;
         } else {
            Long live = liveWatched.get(key);
            return live != null && live > System.currentTimeMillis() ? key : null;
         }
      }
   }

   public static void watchLive(UUID key, long ms) {
      if (key != null) {
         long until = System.currentTimeMillis() + ms;
         liveWatched.merge(key, until, Math::max);
      }
   }

   public static void unwatchLive(UUID key) {
      if (key != null) {
         liveWatched.remove(key);
      }
   }

   private static boolean liveOnly(UUID key) {
      if (active && poseKeys.contains(key)) {
         return false;
      } else {
         Long until = watched.get(key);
         return until == null || until <= System.currentTimeMillis();
      }
   }

   private static ItemStack weaponFor(LivingEntity e) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (e instanceof BattleEntityRenderer.RecruitFakePlayer f) {
         return BattleEntityRenderer.recruitWeapon(f.recruitName);
      } else {
         ItemStack live = LiveCombatClient.armedWeapon(e);
         if (live != null) {
            return live;
         } else if (mc.player != null && e == mc.player) {
            return TurnBattleClientMod.clientEquippedWeapon;
         } else {
            return e instanceof PlayerEntity p ? TurnBattleClientMod.getRemoteEquippedWeapon(p.getUuid()) : null;
         }
      }
   }

   public static void beginWorldPose(LivingEntity e, float td, MatrixStack m) {
      OverhaulView.Swap sw = new OverhaulView.Swap();

      try {
         if (!BattleEntityRenderer.isRenderingEntity() && e instanceof PlayerEntity ap && !(e instanceof BattleEntityRenderer.RecruitFakePlayer)) {
            ItemStack lw = LiveCombatClient.armedWeapon(ap);
            int sl = ap.getInventory().selectedSlot;
            if (lw != null && sl >= 0 && sl < 9 && !ItemStack.areEqual(ap.getMainHandStack(), lw)) {
               sw.player = ap;
               sw.main = ItemStack.EMPTY;
               HandSwaps.showMain(ap, lw);
            }
         }

         if (!BattleEntityRenderer.isRenderingEntity()
            && e instanceof PlayerEntity pl
            && !(e instanceof BattleEntityRenderer.RecruitFakePlayer)
            && PlayerActions.isActing(pl.getUuid())) {
            PlayerActions.ActFrame af = PlayerActions.frameFor(pl);
            if (af != null) {
               sw.yawSet = true;
               sw.bodyYaw = e.bodyYaw;
               sw.prevBodyYaw = e.prevBodyYaw;
               sw.headYaw = e.headYaw;
               sw.prevHeadYaw = e.prevHeadYaw;
               e.bodyYaw = e.prevBodyYaw = af.yaw();
               e.headYaw = e.prevHeadYaw = af.yaw();
               CloneAnimator.Pose p = af.pose();
               if (p.movesOnScreen()) {
                  m.push();
                  sw.push = true;
                  bodyMotion(e, td, m, p, 0.0F);
               }

               ItemStack hold = af.hand();
               int slot = pl.getInventory().selectedSlot;
               if (hold != null && !ItemStack.areEqual(pl.getMainHandStack(), hold)) {
                  sw.player = pl;
                  sw.main = ItemStack.EMPTY;
                  HandSwaps.showMain(pl, hold);
               }

               CloneAnimator.beginModelPose(e, p);
               sw.pose = true;
               swaps.push(sw);
               return;
            }
         }

         if (!BattleEntityRenderer.isRenderingEntity() && e instanceof PlayerEntity cl && !(e instanceof BattleEntityRenderer.RecruitFakePlayer)) {
            ClimbAnimClient.Frame cf = ClimbAnimClient.frameFor(cl);
            if (cf != null) {
               sw.yawSet = true;
               sw.bodyYaw = e.bodyYaw;
               sw.prevBodyYaw = e.prevBodyYaw;
               sw.headYaw = e.headYaw;
               sw.prevHeadYaw = e.prevHeadYaw;
               e.bodyYaw = e.prevBodyYaw = cf.yaw();
               float look = MathHelper.lerpAngleDegrees(td, e.prevHeadYaw, e.headYaw);
               float rel = MathHelper.clamp(MathHelper.wrapDegrees(look - cf.yaw()), -80.0F, 80.0F);
               e.headYaw = e.prevHeadYaw = cf.yaw() + rel;
               CloneAnimator.Pose px = cf.pose();
               if (px.movesOnScreen()) {
                  m.push();
                  sw.push = true;
                  bodyMotion(e, td, m, px, 0.0F);
               }

               CloneAnimator.beginModelPose(e, px);
               sw.pose = true;
               swaps.push(sw);
               return;
            }
         }

         if (!BattleEntityRenderer.isRenderingEntity()) {
            UUID key = worldPoseKey(e);
            if (key != null) {
               ItemStack weapon = weaponFor(e);
               boolean armed = BattleEntityRenderer.isArmed(weapon, e);
               CloneAnimator.Frame fr = CloneAnimator.frame(key, armed);
               boolean liveStance = LiveCombatClient.isLockedOn(e) && e.isOnGround() && e.limbAnimator.getSpeed(td) < 0.08F && !e.isSneaking();
               if (fr != null && CloneAnimator.isIdle(fr.act) && fr.hurtT < 0.0F && liveOnly(key) && !liveStance) {
                  swaps.push(sw);
                  return;
               }

               CloneAnimator.Pose px = fr != null ? fr.pose : null;
               if (px != null && isChoosing(fr.act)) {
                  px = px.copy();
                  px.dx = 0.0F;
                  px.dy = 0.0F;
               }

               float step = MathHelper.lerp(td, prevSteps.getOrDefault(key, 0.0F), steps.getOrDefault(key, 0.0F));
               if (px != null && px.movesOnScreen() || step > 0.002F) {
                  m.push();
                  sw.push = true;
                  bodyMotion(e, td, m, px, step);
               }

               if (fr != null) {
                  holdItems(e, fr, weapon, sw);
                  if (fr.hurtFlash()) {
                     sw.flash = true;
                     sw.hurtTime = e.hurtTime;
                     e.hurtTime = 10;
                  }

                  float yaw = MathHelper.lerpAngleDegrees(td, e.prevBodyYaw, e.bodyYaw) * (float) (Math.PI / 180.0);
                  fxQueue.put(key, new OverhaulView.FxEntry(e, fr, yaw, step, td, System.nanoTime()));
               }

               if (px != null) {
                  CloneAnimator.beginModelPose(e, px);
                  sw.pose = true;
               }
            }
         }
      } catch (Throwable var12) {
      }

      swaps.push(sw);
   }

   private static void holdItems(LivingEntity e, CloneAnimator.Frame fr, ItemStack weapon, OverhaulView.Swap sw) {
      if (e instanceof PlayerEntity pe) {
         boolean overridden = fr.handOverride != null;
         ItemStack hold = overridden ? fr.handOverride : weapon;
         int slot = pe.getInventory().selectedSlot;
         if (hold != null && (overridden || !hold.isEmpty()) && !ItemStack.areEqual(pe.getMainHandStack(), hold)) {
            sw.player = pe;
            sw.main = ItemStack.EMPTY;
            HandSwaps.showMain(pe, hold);
         }

         if (fr.defending) {
            sw.player = pe;
            sw.shield = true;
            HandSwaps.showOff(pe, new ItemStack(Items.SHIELD));
            if (!pe.isUsingItem()) {
               pe.setCurrentHand(Hand.OFF_HAND);
               sw.setHand = true;
            }

            if (pe instanceof BattleEntityRenderer.RecruitFakePlayer rf) {
               rf.forceBlocking = true;
               sw.fakeBlock = true;
            }
         }
      }
   }

   public static void endWorldPose(LivingEntity e, MatrixStack m) {
      OverhaulView.Swap sw = swaps.isEmpty() ? null : swaps.pop();
      if (sw != null) {
         try {
            if (sw.pose) {
               CloneAnimator.endModelPose();
            }

            if (sw.flash) {
               e.hurtTime = sw.hurtTime;
            }

            if (sw.yawSet) {
               e.bodyYaw = sw.bodyYaw;
               e.prevBodyYaw = sw.prevBodyYaw;
               e.headYaw = sw.headYaw;
               e.prevHeadYaw = sw.prevHeadYaw;
            }

            PlayerEntity pe = sw.player;
            if (pe != null) {
               if (sw.main != null) {
                  HandSwaps.restoreMain(pe);
               }

               if (sw.shield) {
                  HandSwaps.restoreOff(pe);
                  if (sw.setHand) {
                     pe.clearActiveItem();
                  }

                  if (sw.fakeBlock && pe instanceof BattleEntityRenderer.RecruitFakePlayer rf) {
                     rf.forceBlocking = false;
                  }
               }
            }
         } catch (Throwable var8) {
         } finally {
            if (sw.push) {
               m.pop();
            }
         }
      }
   }

   static void drawWorldFx(DrawContext ctx) {
      if (!fxQueue.isEmpty()) {
         long now = System.nanoTime();
         MatrixStack ms = ctx.getMatrices();

         for (OverhaulView.FxEntry f : fxQueue.values()) {
            if (now - f.at() <= 150000000L
               && (f.e().isAlive() || f.e() instanceof PlayerEntity)
               && (f.fr().act != CloneAnimator.Act.SKILL || f.fr().cast == CastAnims.Cast.GENERIC)) {
               CloneAnimator.Pose p = f.fr().pose;
               float k = 0.033783782F;
               float fx = -MathHelper.sin(f.yawRad());
               float fz = MathHelper.cos(f.yawRad());
               boolean planted = isChoosing(f.fr().act);
               float fwd = f.step() + (p != null && !planted ? p.dx() * k : 0.0F);
               float up = p != null && !planted ? -p.dy() * k : 0.0F;
               Vec3d body = f.e().getLerpedPos(f.td()).add((double)(fx * fwd), (double)up, (double)(fz * fwd));
               float[] s = project(body);
               if (s != null) {
                  float[] ahead = project(body.add((double)fx, 0.0, (double)fz));
                  int dir = ahead != null && !(ahead[0] >= s[0]) ? -1 : 1;
                  float sc = s[2] / 29.6F;
                  float hPx = f.e().getHeight() * 29.6F;
                  float ox = p != null ? -p.dx() * (float)dir : 0.0F;
                  float oy = p != null ? -p.dy() : 0.0F;
                  ms.push();
                  ms.translate(s[0], s[1], 0.0F);
                  ms.scale(sc, sc, 1.0F);

                  try {
                     CloneAnimator.drawFx(ctx, f.fr(), ox, oy, hPx, dir, false);
                     CloneAnimator.drawFx(ctx, f.fr(), ox, oy, hPx, dir, true);
                  } catch (Throwable var22) {
                  }

                  ms.pop();
               }
            }
         }

         fxQueue.clear();
      }
   }

   private static void bodyMotion(LivingEntity e, float td, MatrixStack m, CloneAnimator.Pose p, float step) {
      float yaw = MathHelper.lerpAngleDegrees(td, e.prevBodyYaw, e.bodyYaw) * (float) (Math.PI / 180.0);
      float fx = -MathHelper.sin(yaw);
      float fz = MathHelper.cos(yaw);
      float k = 0.033783782F;
      float fwd = step + (p != null ? p.dx() * k : 0.0F);
      float up = p != null ? -p.dy() * k : 0.0F;
      m.translate(fx * fwd, up, fz * fwd);
      if (p != null && p.tilt() != 0.0F) {
         m.multiply(new Quaternionf().rotationAxis(p.tilt() * (float) (Math.PI / 180.0), fz, 0.0F, -fx));
      }

      if (p != null && (p.sx() != 1.0F || p.sy() != 1.0F)) {
         m.scale(p.sx(), p.sy(), p.sx());
      }
   }

   static void onWorldBattleEvent(int kind, int actorId, int targetId, int skillOrd, int classOrd, int color) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         Entity actorE = mc.world.getEntityById(actorId);
         UUID key = animKeyFor(actorE);
         if (key != null) {
            if (LiveCombatClient.nearMe(actorE)) {
               watched.put(key, System.currentTimeMillis() + 8000L);
               int col = color & 16777215;
               float vol = mc.player != null ? MathHelper.clamp(1.0F - (float)Math.sqrt(mc.player.squaredDistanceTo(actorE)) / 40.0F, 0.0F, 0.8F) : 0.0F;
               switch (kind) {
                  case 0:
                     CloneAnimator.play(key, CloneAnimator.Act.BASH, col, null);
                     if (targetId >= 0) {
                        BattleScreen.BashEffect b = new BattleScreen.BashEffect(0, 0, col);
                        b.volume = vol;
                        watchFx.add(new OverhaulView.WatchFx(null, b, targetId));
                     }
                     break;
                  case 1:
                     BattleSkill skill = BattleScreen.decodeSkillParam(skillOrd);
                     if (skill == null) {
                        return;
                     }

                     int c = skill == LightnerSkill.RED_BUSTER ? 16720418 : col;
                     CloneAnimator.play(key, CloneAnimator.Act.SKILL, c, null, CastAnims.castFor(skill));
                     int var10000;
                     if (skill == LightnerSkill.RED_BUSTER) {
                        var10000 = 16720418;
                     } else {
                        label72: {
                           if (skill instanceof SoulMageSkill sms && sms.getSoul() != null) {
                              var10000 = sms.getSoul().getColor() & 16777215;
                              break label72;
                           }

                           var10000 = 0;
                        }
                     }

                     int fxColor = var10000;
                     SkillEffect fx = SkillEffect.forSkill(skill, 0, 0, fxColor);
                     if (fx == null) {
                        return;
                     }

                     fx.volume = vol;
                     watchFx.add(new OverhaulView.WatchFx(fx, null, targetId >= 0 ? targetId : actorId));
                     break;
                  case 2:
                     CloneAnimator.play(key, CloneAnimator.Act.ITEM, 7143290, null);
                     break;
                  case 3:
                     CloneAnimator.hold(key, CloneAnimator.Act.DEFEND, 9427199, null, 6000L);
                     break;
                  case 4:
                     CloneAnimator.play(key, CloneAnimator.Act.SPARE, 16770650, null);
                  case 5:
                  default:
                     break;
                  case 6:
                     CloneAnimator.play(key, CloneAnimator.Act.ACT, 16753722, null);
               }
            }
         }
      }
   }

   static void tickWatching() {
      if (!liveWatched.isEmpty()) {
         long n = System.currentTimeMillis();
         liveWatched.values().removeIf(t -> t < n);
      }

      if (!watchFx.isEmpty()) {
         for (OverhaulView.WatchFx f : watchFx) {
            if (f.skill() != null) {
               f.skill().tick();
            }

            if (f.bash() != null) {
               f.bash().tick();
            }
         }

         watchFx.removeIf(f -> (f.skill() == null || f.skill().isDone()) && (f.bash() == null || f.bash().isDone()));
         long now = System.currentTimeMillis();
         watched.values().removeIf(t -> t < now);
         liveWatched.values().removeIf(t -> t < now);
      }
   }

   static void renderWatching(DrawContext ctx, float td) {
      drawWorldFx(ctx);
      if (!watchFx.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            for (OverhaulView.WatchFx f : watchFx) {
               Entity t = mc.world.getEntityById(f.targetId());
               float[] s = projectEntity(t, 0.55F, td);
               if (s != null) {
                  float sc = effectScale(s[2]);
                  if (f.skill() != null) {
                     f.skill().setCenter((int)s[0], (int)s[1]);
                     f.skill().scale = sc;
                     f.skill().render(ctx, td);
                  }

                  if (f.bash() != null) {
                     f.bash().setCenter((int)s[0], (int)s[1]);
                     f.bash().scale = sc;
                     f.bash().render(ctx, td);
                  }
               }
            }
         }
      }
   }

   private static record FxEntry(LivingEntity e, CloneAnimator.Frame fr, float yawRad, float step, float td, long at) {
   }

   static enum Shot {
      INTRO,
      ACTOR,
      TARGET,
      ALLY,
      ENEMY_TURN;
   }

   private static final class Swap {
      boolean push;
      boolean pose;
      PlayerEntity player;
      ItemStack main;
      ItemStack off;
      boolean shield;
      boolean setHand;
      boolean fakeBlock;
      boolean flash;
      int hurtTime;
      boolean yawSet;
      float bodyYaw;
      float prevBodyYaw;
      float headYaw;
      float prevHeadYaw;
   }

   private static record WatchFx(SkillEffect skill, BattleScreen.BashEffect bash, int targetId) {
   }
}
