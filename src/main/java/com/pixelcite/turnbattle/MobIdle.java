package com.pixelcite.turnbattle;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class MobIdle {
   private static final ArrayList<Object[]> touched = new ArrayList<>();

   private MobIdle() {
   }

   private static ModelPart touch(ModelPart p) {
      if (p == null) {
         return null;
      } else {
         for (Object[] t : touched) {
            if (t[0] == p) {
               return p;
            }
         }

         touched.add(new Object[]{p, new float[]{p.pitch, p.yaw, p.roll}});
         return p;
      }
   }

   public static void restore() {
      for (int i = touched.size() - 1; i >= 0; i--) {
         Object[] t = touched.get(i);
         ModelPart p = (ModelPart)t[0];
         float[] a = (float[])t[1];
         p.pitch = a[0];
         p.yaw = a[1];
         p.roll = a[2];
      }

      touched.clear();
   }

   public static boolean applies(LivingEntity e) {
      if (e != null && !(e instanceof PlayerEntity) && !GhostRender.active) {
         return e.deathTime <= 0 && (e.isAlive() || BattleEntityRenderer.isRenderingEntity())
            ? BattleEntityRenderer.isRenderingEntity() || OverhaulView.isBattleEnemy(e)
            : false;
      } else {
         return false;
      }
   }

   private static float phase(Entity e) {
      return (float)(e.getUuid().hashCode() & 65535) * 0.0137F;
   }

   public static void breathe(LivingEntity e, float td, MatrixStack m) {
      float t = (float)e.age + td + phase(e) * 40.0F;
      float br = MathHelper.sin(t * 0.085F);
      m.scale(1.0F - br * 0.012F, 1.0F + br * 0.024F, 1.0F - br * 0.012F);
   }

   public static void pose(EntityModel<?> model, LivingEntity e, float t) {
      t += phase(e) * 40.0F;
      float br = MathHelper.sin(t * 0.085F);
      float sway = MathHelper.sin(t * 0.06F);
      float twitch = MathHelper.sin(t * 0.17F) * MathHelper.sin(t * 0.031F);

      try {
         touched.clear();
         if (model instanceof BipedEntityModel<?> b) {
            biped(b, br, sway, twitch);
         } else if (model instanceof SinglePartEntityModel<?> sp) {
            ModelPart root = sp.getPart();
            if (has(root, "right_middle_front_leg")) {
               spider(root, br, sway, twitch);
            } else if (has(root, "right_hind_leg") && has(root, "right_front_leg")) {
               quadruped(root, br, sway, twitch);
            } else if (has(root, "right_leg") && has(root, "left_leg")) {
               legs(root, br, sway, twitch);
            }

            head(root, br, twitch);
         }
      } catch (Throwable var9) {
      }
   }

   private static boolean has(ModelPart root, String name) {
      try {
         return root.hasChild(name);
      } catch (Throwable var3) {
         return false;
      }
   }

   private static ModelPart find(ModelPart root, String name) {
      if (has(root, name)) {
         return touch(root.getChild(name));
      } else {
         if (has(root, "body")) {
            ModelPart body = root.getChild("body");
            if (has(body, name)) {
               return touch(body.getChild(name));
            }
         }

         return null;
      }
   }

   private static void biped(BipedEntityModel<?> m, float br, float sway, float twitch) {
      touch(m.rightLeg);
      touch(m.leftLeg);
      touch(m.rightArm);
      touch(m.leftArm);
      touch(m.head);
      touch(m.hat);
      m.rightLeg.pitch = -0.16F + br * 0.025F;
      m.leftLeg.pitch = 0.2F - br * 0.025F;
      m.rightLeg.roll = 0.07F + sway * 0.015F;
      m.leftLeg.roll = -0.07F + sway * 0.015F;
      m.rightLeg.yaw = 0.0F;
      m.leftLeg.yaw = 0.0F;
      m.rightArm.pitch += -0.05F + br * 0.05F + twitch * 0.04F;
      m.leftArm.pitch += -0.05F - br * 0.04F;
      m.rightArm.roll += 0.04F + br * 0.025F;
      m.leftArm.roll += -0.04F - br * 0.025F;
      m.head.pitch += 0.04F + br * 0.025F;
      m.head.yaw += sway * 0.05F;
      m.hat.copyTransform(m.head);
   }

   private static void legs(ModelPart root, float br, float sway, float twitch) {
      ModelPart r = find(root, "right_leg");
      ModelPart l = find(root, "left_leg");
      if (r != null) {
         r.pitch = -0.12F + br * 0.025F;
         r.roll = 0.06F + sway * 0.015F;
      }

      if (l != null) {
         l.pitch = 0.15F - br * 0.025F;
         l.roll = -0.06F + sway * 0.015F;
      }

      ModelPart ra = find(root, "right_arm");
      ModelPart la = find(root, "left_arm");
      if (ra != null) {
         ra.pitch += br * 0.04F + twitch * 0.03F;
         ra.roll += 0.03F + br * 0.02F;
      }

      if (la != null) {
         la.pitch -= br * 0.04F;
         la.roll -= 0.03F + br * 0.02F;
      }
   }

   private static void quadruped(ModelPart root, float br, float sway, float twitch) {
      float a = sway * 0.09F;
      float b = br * 0.05F;
      ModelPart rh = find(root, "right_hind_leg");
      ModelPart lh = find(root, "left_hind_leg");
      ModelPart rf = find(root, "right_front_leg");
      ModelPart lf = find(root, "left_front_leg");
      if (rh != null) {
         rh.pitch = a + b;
      }

      if (lh != null) {
         lh.pitch = -a + b;
      }

      if (rf != null) {
         rf.pitch = -a - b + twitch * 0.04F;
      }

      if (lf != null) {
         lf.pitch = a - b;
      }
   }

   private static void spider(ModelPart root, float br, float sway, float twitch) {
      String[] names = new String[]{
         "right_hind_leg",
         "left_hind_leg",
         "right_middle_hind_leg",
         "left_middle_hind_leg",
         "right_middle_front_leg",
         "left_middle_front_leg",
         "right_front_leg",
         "left_front_leg"
      };

      for (int i = 0; i < names.length; i++) {
         ModelPart p = find(root, names[i]);
         if (p != null) {
            float side = i % 2 == 0 ? 1.0F : -1.0F;
            float w = MathHelper.sin(sway * 2.2F + (float)i * 0.9F);
            p.yaw += w * 0.05F * side;
            p.roll += (br * 0.05F + twitch * 0.03F) * side;
         }
      }
   }

   private static void head(ModelPart root, float br, float twitch) {
      ModelPart h = find(root, "head");
      if (h != null) {
         h.pitch += 0.03F + br * 0.03F;
         h.yaw += twitch * 0.04F;
      }
   }
}
