package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class LiveCamera {
   private static final double BACK = 3.4;
   private static final double SIDE = 0.95;
   private static final double UP = 0.45;
   private static final double BACK_PER_BLOCK = 0.18;
   private static final double BACK_MAX = 5.6;
   private static boolean locked;
   private static Perspective before;
   private static boolean have;
   private static Vec3d pos = Vec3d.ZERO;
   private static float yaw;
   private static float pitch;
   private static long lastNanos;

   private LiveCamera() {
   }

   public static boolean active() {
      return LiveCombatClient.lockedOn() && !TurnBattleConfig.get().liveLockFirstPerson && !OverhaulView.cameraActive() && !PlayerActions.cameraActive();
   }

   public static Vec3d pos() {
      return pos;
   }

   public static float yaw() {
      return yaw;
   }

   public static float pitch() {
      return pitch;
   }

   public static void tick(MinecraftClient mc) {
      boolean lockedNow = LiveCombatClient.lockedOn();
      if (lockedNow) {
         if (!locked) {
            before = mc.options.getPerspective();
            have = false;
         }

         boolean changed;
         for (changed = false; mc.options.togglePerspectiveKey.wasPressed(); have = false) {
            TurnBattleConfig cfg = TurnBattleConfig.get();
            cfg.liveLockFirstPerson = !cfg.liveLockFirstPerson;
            changed = true;
         }

         if (changed) {
            TurnBattleConfig.save();
         }

         Perspective want = TurnBattleConfig.get().liveLockFirstPerson ? Perspective.FIRST_PERSON : Perspective.THIRD_PERSON_BACK;
         if (mc.options.getPerspective() != want) {
            mc.options.setPerspective(want);
         }
      } else if (locked) {
         if (before != null && mc.options.getPerspective() != before) {
            mc.options.setPerspective(before);
         }

         before = null;
         have = false;
      }

      locked = lockedNow;
   }

   public static void update(float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      long now = System.nanoTime();
      float dt = lastNanos == 0L ? 0.016F : MathHelper.clamp((float)(now - lastNanos) / 1.0E9F, 0.0F, 0.1F);
      lastNanos = now;
      Entity t = LiveCombatClient.lockTargetEntity();
      if (mc.player != null && t != null && mc.world != null) {
         Vec3d me = mc.player.getLerpedPos(td).add(0.0, (double)mc.player.getStandingEyeHeight(), 0.0);
         Vec3d them = t.getLerpedPos(td).add(0.0, (double)t.getHeight() * 0.6, 0.0);
         Vec3d d = new Vec3d(them.x - me.x, 0.0, them.z - me.z);
         double dist = d.length();
         Vec3d fwd = dist < 0.05 ? Vec3d.fromPolar(0.0F, mc.player.getYaw()) : d.multiply(1.0 / dist);
         Vec3d right = new Vec3d(-fwd.z, 0.0, fwd.x);
         double back = Math.min(5.6, 3.4 + dist * 0.18);
         Vec3d goal = me.subtract(fwd.multiply(back)).add(right.multiply(0.95)).add(0.0, 0.45, 0.0);
         goal = clip(mc, me.add(right.multiply(0.475)), goal);
         Vec3d focus = me.lerp(them, 0.62).add(0.0, -0.15, 0.0);
         float k = have ? 1.0F - (float)Math.exp((double)(-dt * 9.0F)) : 1.0F;
         float kr = have ? 1.0F - (float)Math.exp((double)(-dt * 12.0F)) : 1.0F;
         pos = have ? pos.lerp(goal, (double)k) : goal;
         pos = clip(mc, me, pos);
         Vec3d to = focus.subtract(pos);
         double h = Math.sqrt(to.x * to.x + to.z * to.z);
         float wantYaw = (float)(MathHelper.atan2(to.z, to.x) * 180.0F / (float)Math.PI) - 90.0F;
         float wantPitch = (float)(-(MathHelper.atan2(to.y, h) * 180.0F / (float)Math.PI));
         yaw = have ? yaw + MathHelper.wrapDegrees(wantYaw - yaw) * kr : wantYaw;
         pitch = have ? pitch + (wantPitch - pitch) * kr : wantPitch;
         pitch = MathHelper.clamp(pitch, -80.0F, 80.0F);
         have = true;
      }
   }

   private static Vec3d clip(MinecraftClient mc, Vec3d from, Vec3d to) {
      BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to, ShapeType.VISUAL, FluidHandling.NONE, mc.player));
      if (hit.getType() == Type.MISS) {
         return to;
      } else {
         Vec3d dir = to.subtract(from);
         double len = dir.length();
         if (len < 1.0E-4) {
            return to;
         } else {
            double keep = Math.max(0.2, from.distanceTo(hit.getPos()) - 0.25);
            return from.add(dir.multiply(keep / len));
         }
      }
   }
}
