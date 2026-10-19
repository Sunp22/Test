package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public final class ClimbAnimClient {
   static final int HANG = 1;
   static final int STEP = 2;
   static final int CHARGE = 3;
   static final int JUMP = 4;
   static final int VAULT = 5;
   static final int UP = 0;
   static final int DOWN = 1;
   static final int LEFT = 2;
   static final int RIGHT = 3;
   private static final Map<UUID, ClimbAnimClient.Anim> anims = new HashMap<>();

   private ClimbAnimClient() {
   }

   static void register() {
      Net121.registerClientReceiver(TurnBattleMod.WALL_CLIMB_ANIM, (client, handler, buf, sender) -> {
         UUID who = buf.readUuid();
         int anim = buf.readByte();
         int dir = buf.readByte();
         Direction wallOut = Direction.byId(buf.readByte());
         int step = buf.readVarInt();
         client.execute(() -> {
            if (anim <= 0) {
               anims.remove(who);
            } else {
               ClimbAnimClient.Anim old = anims.get(who);
               long start = old != null && old.anim == anim && old.dir == dir && old.step == step ? old.startMs : System.currentTimeMillis();
               anims.put(who, new ClimbAnimClient.Anim(anim, dir, wallOut, step, start));
            }
         });
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> client.execute(anims::clear));
   }

   static ClimbAnimClient.Frame frameFor(PlayerEntity e) {
      ClimbAnimClient.Anim a = anims.get(e.getUuid());
      if (a != null && !a.wallOut.getAxis().isVertical()) {
         float t = (float)(System.currentTimeMillis() - a.startMs) / 1000.0F;
         float clock = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
         CloneAnimator.Pose p = new CloneAnimator.Pose();
         p.armW = 1.0F;
         p.legW = 1.0F;
         p.headW = 0.0F;
         hang(p, clock);
         switch (a.anim) {
            case 2:
               step(p, a, t);
               break;
            case 3:
               charge(p, t, clock);
               break;
            case 4:
               jump(p, a, t);
               break;
            case 5:
               vault(p, t);
         }

         return new ClimbAnimClient.Frame(a.wallOut.getOpposite().asRotation(), p);
      } else {
         return null;
      }
   }

   private static void hang(CloneAnimator.Pose p, float clock) {
      float br = (float)Math.sin((double)(clock * 2.1F));
      arm(p, true, -2.55F + 0.03F * br, 0.18F);
      arm(p, false, -2.45F - 0.03F * br, 0.18F);
      p.rLegP = -0.38F + 0.03F * br;
      p.lLegP = -0.16F - 0.03F * br;
      p.tilt = 9.0F;
   }

   private static void step(CloneAnimator.Pose p, ClimbAnimClient.Anim a, float t) {
      float k = clamp01(t / 0.42F);
      float b = bell(k);

      boolean rightLeads = switch (a.dir) {
         case 2 -> false;
         case 3 -> true;
         default -> (a.step & 1) == 0;
      };
      switch (a.dir) {
         case 0:
            arm(p, rightLeads, mix(-2.5F, -3.05F, b), mix(0.18F, 0.08F, b));
            arm(p, !rightLeads, mix(-2.5F, -2.0F, b), 0.22F);
            leg(p, rightLeads, mix(-0.38F, -1.05F, b));
            leg(p, !rightLeads, mix(-0.16F, 0.12F, b));
            p.tilt = 9.0F + 3.0F * b;
            break;
         case 1:
            arm(p, rightLeads, mix(-2.5F, -1.95F, b), 0.25F);
            arm(p, !rightLeads, -2.6F, 0.15F);
            leg(p, rightLeads, mix(-0.38F, 0.25F, b));
            leg(p, !rightLeads, mix(-0.16F, -0.55F, b));
            p.tilt = 9.0F - 2.0F * b;
            break;
         default:
            arm(p, rightLeads, mix(-2.5F, -2.25F, b), mix(0.18F, 0.85F, b));
            arm(p, !rightLeads, mix(-2.45F, -2.6F, b), mix(0.18F, -0.08F, b));
            leg(p, rightLeads, mix(-0.38F, -0.65F, b));
            leg(p, !rightLeads, mix(-0.16F, 0.1F, b));
            p.bodyW = 1.0F;
            p.bodyY = (rightLeads ? -0.28F : 0.28F) * b;
      }
   }

   private static void charge(CloneAnimator.Pose p, float t, float clock) {
      float in = smooth(clamp01(t / 0.25F));
      float shake = (0.015F + 0.05F * clamp01(t / 0.8F)) * (float)Math.sin((double)(clock * 47.0F));
      arm(p, true, mix(p.rArmP, -1.95F, in) + shake, mix(0.18F, 0.38F, in));
      arm(p, false, mix(p.lArmP, -1.95F, in) - shake, mix(0.18F, 0.38F, in));
      p.rLegP = mix(p.rLegP, -0.95F, in) - shake;
      p.lLegP = mix(p.lLegP, -0.85F, in) + shake;
      p.tilt = mix(9.0F, 17.0F, in);
      p.dy = 3.0F * in;
   }

   private static void jump(CloneAnimator.Pose p, ClimbAnimClient.Anim a, float t) {
      float k = smooth(clamp01(t / 0.2F));
      switch (a.dir) {
         case 0:
            arm(p, true, mix(-2.0F, -3.1F, k), 0.1F);
            arm(p, false, mix(-2.0F, -3.1F, k), 0.1F);
            p.rLegP = mix(-0.9F, 0.18F, k);
            p.lLegP = mix(-0.9F, 0.08F, k);
            p.tilt = mix(17.0F, 4.0F, k);
            break;
         case 1:
            arm(p, true, -2.9F, 0.35F);
            arm(p, false, -2.9F, 0.35F);
            p.rLegP = mix(-0.9F, -0.5F, k);
            p.lLegP = mix(-0.9F, -0.7F, k);
            p.tilt = mix(17.0F, 12.0F, k);
            break;
         default:
            boolean right = a.dir == 3;
            arm(p, right, mix(-2.0F, -2.35F, k), mix(0.38F, 1.05F, k));
            arm(p, !right, mix(-2.0F, -2.8F, k), 0.2F);
            leg(p, right, -0.3F);
            leg(p, !right, 0.15F);
            p.bodyW = 1.0F;
            p.bodyY = (right ? -0.35F : 0.35F) * k;
            p.tilt = mix(17.0F, 8.0F, k);
      }
   }

   private static void vault(CloneAnimator.Pose p, float t) {
      float k1 = smooth(clamp01(t / 0.4F));
      float k2 = smooth(clamp01((t - 0.4F) / 0.3F));
      float armP = mix(-2.6F, -0.95F, k1);
      armP = mix(armP, -0.45F, k2);
      arm(p, true, armP, 0.3F);
      arm(p, false, armP, 0.3F);
      p.rLegP = mix(mix(-0.38F, -1.25F, k1), -0.4F, k2);
      p.lLegP = mix(mix(-0.16F, 0.15F, k1), 0.3F, k2);
      p.tilt = mix(mix(9.0F, 22.0F, k1), 4.0F, k2);
   }

   private static void arm(CloneAnimator.Pose p, boolean right, float pitch, float out) {
      if (right) {
         p.rArmP = pitch;
         p.rArmR = out;
         p.rArmY = 0.0F;
      } else {
         p.lArmP = pitch;
         p.lArmR = -out;
         p.lArmY = 0.0F;
      }
   }

   private static void leg(CloneAnimator.Pose p, boolean right, float pitch) {
      if (right) {
         p.rLegP = pitch;
      } else {
         p.lLegP = pitch;
      }
   }

   private static float clamp01(float x) {
      return x < 0.0F ? 0.0F : (x > 1.0F ? 1.0F : x);
   }

   private static float mix(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static float smooth(float x) {
      return x * x * (3.0F - 2.0F * x);
   }

   private static float bell(float k) {
      return k < 0.5F ? smooth(k * 2.0F) : 1.0F - 0.35F * smooth((k - 0.5F) * 2.0F);
   }

   static record Anim(int anim, int dir, Direction wallOut, int step, long startMs) {
   }

   static record Frame(float yaw, CloneAnimator.Pose pose) {
   }
}
