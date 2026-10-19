package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Arm;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class OriModel extends PlayerEntityModel<AbstractClientPlayerEntity> {
   private final ModelPart leftEar;
   private final ModelPart leftEarMid;
   private final ModelPart leftEarUpper;
   private final ModelPart leftEarTip;
   private final ModelPart rightEar;
   private final ModelPart rightEarMid;
   private final ModelPart rightEarUpper;
   private final ModelPart rightEarTip;
   private final ModelPart[] bones;
   private static final float[][] CONF = new float[][]{
      {0.0F, -8.0F, -9.0F, 0.0F, 0.0F, 0.0F},
      {0.0F, -3.0F, 8.0F, 0.0F, 0.0F, 0.0F},
      {0.0F, 0.0F, -6.0F, 0.0F, 1.0F, 0.0F},
      {0.0F, -30.0F, 0.0F, 0.0F, -1.1F, 0.2F},
      {0.0F, 0.0F, -7.0F, -0.8F, 0.0F, 0.0F},
      {0.0F, 0.0F, 0.0F, -1.6F, 0.0F, 0.0F},
      {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {23.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {17.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {0.0F, 9.0F, -13.0F, 0.7F, 0.0F, 0.0F},
      {83.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6F},
      {37.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F}
   };
   private static final float[][] SHY = new float[][]{
      {5.0926F, -6.5474F, -17.0866F, 0.0F, -0.3F, 0.0F},
      {3.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {125.6856F, -74.0201F, 66.5612F, 0.0F, 1.3F, 2.2F},
      {9.9281F, 40.1712F, 15.9461F, 0.0F, 1.3F, 2.0F},
      {10.5001F, 7.607F, 4.6441F, 0.8F, -0.3F, -1.5F},
      {14.8747F, -8.662F, -6.7777F, -0.8F, 0.0F, 0.3F},
      {0.0F, 0.0F, 7.0F, 0.0F, 0.0F, 0.0F},
      {3.6923F, -13.5138F, -10.4376F, -0.3F, 0.0F, 0.0F},
      {0.0F, 0.0F, -6.0F, 0.0F, 0.0F, 0.0F},
      {25.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {-19.0F, 0.0F, -25.0F, 0.0F, 0.0F, 0.0F},
      {71.7712F, 0.1777F, -22.0209F, 0.0F, 0.0F, 0.0F},
      {7.9808F, 0.5562F, -3.9612F, 0.0F, 0.0F, 0.0F},
      {30.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}
   };
   private static final float PERIOD = 9.0F;

   public OriModel(ModelPart root) {
      super(root, true);
      ModelPart h = root.getChild("head");
      this.leftEar = h.getChild("left_ear");
      this.leftEarMid = this.leftEar.getChild("left_ear_mid");
      this.leftEarUpper = this.leftEarMid.getChild("left_ear_upper");
      this.leftEarTip = this.leftEarUpper.getChild("left_ear_tip");
      this.rightEar = h.getChild("right_ear");
      this.rightEarMid = this.rightEar.getChild("right_ear_mid");
      this.rightEarUpper = this.rightEarMid.getChild("right_ear_upper");
      this.rightEarTip = this.rightEarUpper.getChild("right_ear_tip");
      this.bones = new ModelPart[]{
         this.head,
         this.body,
         this.leftArm,
         this.rightArm,
         this.leftLeg,
         this.rightLeg,
         this.leftEar,
         this.leftEarMid,
         this.leftEarUpper,
         this.leftEarTip,
         this.rightEar,
         this.rightEarMid,
         this.rightEarUpper,
         this.rightEarTip
      };
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData data = new ModelData();
      ModelPartData root = data.getRoot();
      ModelPartData p0 = root.addChild(
         "head",
         ModelPartBuilder.create()
            .uv(0, 0)
            .mirrored(false)
            .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(17, 39)
            .mirrored(false)
            .cuboid(-5.0F, -7.0F, 4.0F, 10.0F, 7.0F, 1.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(1, 33)
            .mirrored(false)
            .cuboid(4.0F, -7.0F, -3.0F, 1.0F, 7.0F, 7.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(1, 33)
            .mirrored(true)
            .cuboid(-5.0F, -7.0F, -3.0F, 1.0F, 7.0F, 7.0F, new Dilation(0.0F, 0.0F, 0.0F)),
         ModelTransform.pivot(0.0F, 0.0F, -1.0F)
      );
      ModelPartData p1 = p0.addChild(
         "r1_2dleft1",
         ModelPartBuilder.create().uv(0, 33).mirrored(false).cuboid(1.0F, -7.0F, 0.0F, 4.0F, 7.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.0F)),
         ModelTransform.of(3.0F, 0.0F, 2.0F, -0.06981F, -0.2618F, 0.12217F)
      );
      ModelPartData p2 = p0.addChild(
         "r2_fringe",
         ModelPartBuilder.create().uv(24, 0).mirrored(false).cuboid(-5.0F, 0.0F, 0.0F, 10.0F, 5.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.0F)),
         ModelTransform.of(-0.125F, -7.8F, -4.075F, -0.05207F, 0.0F, -0.06929F)
      );
      ModelPartData p3 = p0.addChild(
         "r3_longhair",
         ModelPartBuilder.create().uv(44, 0).mirrored(false).cuboid(-5.0F, 0.0F, 0.0F, 10.0F, 4.0F, 1.0F, new Dilation(0.01F, 0.01F, 0.01F)),
         ModelTransform.of(0.0F, -1.0F, -3.2F, -0.15708F, 0.0F, 0.0F)
      );
      ModelPartData p4 = p0.addChild(
         "r4_2dright1",
         ModelPartBuilder.create().uv(0, 33).mirrored(true).cuboid(-5.0F, -7.0F, 0.0F, 4.0F, 7.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.0F)),
         ModelTransform.of(-3.0F, 0.0F, 2.0F, -0.06981F, 0.2618F, -0.12217F)
      );
      ModelPartData p5 = p0.addChild(
         "left_ear",
         ModelPartBuilder.create().uv(52, 56).mirrored(false).cuboid(-2.0F, -3.7F, -2.0F, 3.0F, 5.0F, 3.0F, new Dilation(0.2F, 0.2F, 0.2F)),
         ModelTransform.pivot(4.0F, -6.0F, 2.0F)
      );
      ModelPartData p6 = p5.addChild(
         "left_ear_mid",
         ModelPartBuilder.create().uv(52, 48).mirrored(false).cuboid(-2.0F, -5.0F, -1.0F, 3.0F, 5.0F, 3.0F, new Dilation(0.35F, 0.35F, 0.35F)),
         ModelTransform.pivot(0.0F, -3.7F, -1.0F)
      );
      ModelPartData p7 = p6.addChild("left_ear_upper", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -2.0F, 0.0F));
      ModelPartData p8 = p7.addChild(
         "r1_tip1",
         ModelPartBuilder.create().uv(54, 41).mirrored(false).cuboid(-3.0F, -5.0F, -0.7F, 5.0F, 7.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.3F)),
         ModelTransform.of(0.0F, 0.0F, 1.2F, 0.08286F, -0.01121F, -0.02545F)
      );
      ModelPartData p9 = p7.addChild("left_ear_tip", ModelPartBuilder.create(), ModelTransform.pivot(-2.0F, -4.8F, -0.2F));
      ModelPartData p10 = p9.addChild(
         "r1_tip2",
         ModelPartBuilder.create().uv(54, 37).mirrored(false).cuboid(-1.10724F, -4.05317F, 0.27278F, 5.0F, 4.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.3F)),
         ModelTransform.of(0.0F, 0.0F, 0.0F, 0.08286F, -0.01121F, -0.02545F)
      );
      ModelPartData p11 = p5.addChild(
         "r1_fur",
         ModelPartBuilder.create().uv(49, 56).mirrored(false).cuboid(-1.0F, -2.0F, 0.0F, 3.0F, 3.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.0F)),
         ModelTransform.of(1.0F, 0.3F, 0.0F, 0.0F, -0.47997F, 0.0F)
      );
      ModelPartData p12 = p0.addChild(
         "right_ear",
         ModelPartBuilder.create().uv(52, 56).mirrored(true).cuboid(-1.0F, -3.7F, -2.0F, 3.0F, 5.0F, 3.0F, new Dilation(0.2F, 0.2F, 0.2F)),
         ModelTransform.pivot(-4.0F, -6.0F, 2.0F)
      );
      ModelPartData p13 = p12.addChild(
         "right_ear_mid",
         ModelPartBuilder.create().uv(52, 48).mirrored(true).cuboid(-1.0F, -5.0F, -1.0F, 3.0F, 5.0F, 3.0F, new Dilation(0.35F, 0.35F, 0.35F)),
         ModelTransform.pivot(0.0F, -3.7F, -1.0F)
      );
      ModelPartData p14 = p13.addChild("right_ear_upper", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -2.0F, 0.0F));
      ModelPartData p15 = p14.addChild(
         "r1_tip1",
         ModelPartBuilder.create().uv(54, 41).mirrored(false).cuboid(-2.0F, -5.0F, -0.85F, 5.0F, 7.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.15F)),
         ModelTransform.of(0.0F, 0.0F, 1.2F, 0.08286F, 0.01121F, 0.02545F)
      );
      ModelPartData p16 = p14.addChild("right_ear_tip", ModelPartBuilder.create(), ModelTransform.pivot(2.0F, -4.8F, -0.2F));
      ModelPartData p17 = p16.addChild(
         "r1_tip2",
         ModelPartBuilder.create().uv(54, 37).mirrored(false).cuboid(-3.89276F, -4.05317F, 0.17278F, 5.0F, 4.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.2F)),
         ModelTransform.of(0.0F, 0.0F, 0.0F, 0.08286F, 0.01121F, 0.02545F)
      );
      ModelPartData p18 = p12.addChild(
         "r1_fur",
         ModelPartBuilder.create().uv(49, 56).mirrored(true).cuboid(-2.0F, -2.0F, 0.0F, 3.0F, 3.0F, 0.0F, new Dilation(0.0F, 0.0F, 0.0F)),
         ModelTransform.of(-1.0F, 0.3F, 0.0F, 0.0F, 0.47997F, 0.0F)
      );
      ModelPartData p19 = root.addChild(
         "body",
         ModelPartBuilder.create()
            .uv(2, 74)
            .mirrored(false)
            .cuboid(-3.0F, 5.0F, -2.0F, 6.0F, 3.0F, 4.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(2, 65)
            .mirrored(false)
            .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 5.0F, 4.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(0, 81)
            .mirrored(false)
            .cuboid(-4.0F, 8.0F, -2.0F, 8.0F, 4.0F, 4.0F, new Dilation(0.12F, 0.12F, 0.12F)),
         ModelTransform.pivot(0.0F, 0.0F, 0.0F)
      );
      ModelPartData p20 = p19.addChild("scarf", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -2.2F, -0.6F));
      ModelPartData p21 = p20.addChild(
         "r1_scarf",
         ModelPartBuilder.create().uv(0, 90).mirrored(false).cuboid(-4.0F, 2.0F, -3.0F, 8.0F, 3.0F, 6.0F, new Dilation(0.25F, 0.25F, 0.25F)),
         ModelTransform.of(0.0F, 0.25F, 0.3F, 0.10472F, 0.0F, 0.0F)
      );
      ModelPartData p22 = p20.addChild(
         "r2_string",
         ModelPartBuilder.create().uv(31, 92).mirrored(false).cuboid(-4.0F, 5.0F, -3.0F, 8.0F, 3.0F, 4.0F, new Dilation(0.25F, 0.25F, 0.25F)),
         ModelTransform.of(0.0F, -0.4F, 1.825F, -0.06981F, 0.0F, 0.0F)
      );
      ModelPartData p23 = root.addChild(
         "left_arm",
         ModelPartBuilder.create()
            .uv(66, 0)
            .mirrored(false)
            .cuboid(-1.0F, -1.0F, -2.0F, 3.0F, 12.0F, 4.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(66, 16)
            .mirrored(false)
            .cuboid(-1.0F, -1.0F, -2.0F, 3.0F, 12.0F, 4.0F, new Dilation(0.2F, 0.2F, 0.2F)),
         ModelTransform.pivot(5.0F, 2.0F, 0.0F)
      );
      ModelPartData p24 = root.addChild(
         "right_arm",
         ModelPartBuilder.create()
            .uv(66, 0)
            .mirrored(true)
            .cuboid(-2.0F, -1.0F, -2.0F, 3.0F, 12.0F, 4.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(66, 16)
            .mirrored(true)
            .cuboid(-2.0F, -1.0F, -2.0F, 3.0F, 12.0F, 4.0F, new Dilation(0.2F, 0.2F, 0.2F)),
         ModelTransform.pivot(-5.0F, 2.0F, 0.0F)
      );
      ModelPartData p25 = root.addChild(
         "left_leg",
         ModelPartBuilder.create()
            .uv(0, 104)
            .mirrored(true)
            .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(16, 104)
            .mirrored(true)
            .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.2F, 0.2F, 0.2F)),
         ModelTransform.pivot(1.9F, 12.0F, 0.0F)
      );
      ModelPartData p26 = root.addChild(
         "right_leg",
         ModelPartBuilder.create()
            .uv(0, 104)
            .mirrored(false)
            .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F, 0.0F, 0.0F))
            .uv(16, 104)
            .mirrored(false)
            .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.2F, 0.2F, 0.2F)),
         ModelTransform.pivot(-1.9F, 12.0F, 0.0F)
      );

      for (String empty : new String[]{"hat", "jacket", "ear", "cloak", "left_sleeve", "right_sleeve", "left_pants", "right_pants"}) {
         root.addChild(empty, ModelPartBuilder.create(), ModelTransform.NONE);
      }

      return TexturedModelData.of(data, 120, 120);
   }

   public void setAngles(AbstractClientPlayerEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
      for (ModelPart p : this.bones) {
         p.resetTransform();
      }

      super.setAngles(e, limbAngle, limbDistance, age, headYaw, headPitch);
      this.applyIdle(e, limbDistance);
      if (DepthsLantern.inHand(e)) {
         ModelPart arm = e.getMainArm() == Arm.LEFT ? this.leftArm : this.rightArm;
         float side = e.getMainArm() == Arm.LEFT ? -1.0F : 1.0F;
         arm.pitch = arm.pitch * 0.3F - 0.3F;
         arm.yaw = 0.0F;
         arm.roll = side * 0.05F;
      }
   }

   private void applyIdle(AbstractClientPlayerEntity e, float limbDistance) {
      if (!BattleEntityRenderer.isStillClone()) {
         if (!DepthsClient.applyScenePose(this, e)) {
            CloneAnimator.Pose ap = CloneAnimator.activePoseFor(e);
            float w = ap != null ? ap.idleW() : 1.0F - Math.min(1.0F, limbDistance * 3.0F);
            w = Math.max(0.0F, Math.min(1.0F, w));
            float t = (float)(Util.getMeasuringTimeMs() % 3600000L) / 1000.0F;
            float c = t % 9.0F / 9.0F;
            int n = (int)(t / 9.0F);
            float shy = ease(seg(c, 0.5F, 0.565F)) * (1.0F - ease(seg(c, 0.83F, 0.9F)));
            float conf = 1.0F - shy;
            float[][] pose = new float[14][6];

            for (int b = 0; b < 14; b++) {
               float[] a = CONF[b];
               float[] s = SHY[b];
               float[] q = slerp(quat(a[0], a[1], a[2]), quat(s[0], s[1], s[2]), shy);
               float[] eu = euler(q);
               pose[b][0] = eu[0];
               pose[b][1] = eu[1];
               pose[b][2] = eu[2];

               for (int i = 3; i < 6; i++) {
                  pose[b][i] = a[i] + (s[i] - a[i]) * shy;
               }
            }

            float br = (float)Math.sin((double)(t * 2.6F));
            float sw = (float)Math.sin((double)(t * 0.9F));
            add(pose[0], -5.0F * conf + 1.5F * br, 3.0F * sw, 0.0F);
            add(pose[1], -2.0F * conf + 0.8F * br, 0.0F, 1.5F * sw);
            add(pose[3], 2.0F * br, 0.0F, 3.0F + 2.0F * br);
            add(pose[2], 1.5F * br * conf, 0.0F, (-2.0F - 1.5F * br) * conf);
            float hold = seg(c, 0.58F, 0.82F);
            if (hold > 0.0F && hold < 1.0F) {
               float rub = (float)(Math.sin((double)hold * Math.PI * 7.0) * Math.sin((double)hold * Math.PI)) * shy;
               add(pose[2], rub * 7.0F, rub * 5.0F, 0.0F);
               add(pose[0], 0.0F, 0.0F, rub * 2.0F);
            }

            float look = (float)Math.sin(Math.PI * (double)seg(c, 0.62F, 0.86F)) * (n % 2 == 1 ? 1.0F : -0.6F);
            add(pose[0], 0.0F, 14.0F * look, 0.0F);
            float es = (float)Math.sin((double)(t * 1.7F));
            float es2 = (float)Math.sin((double)(t * 1.3F + 1.1F));
            float flick = n % 3 != 2 ? (float)Math.sin(Math.PI * (double)seg(c, 0.18F, 0.24F)) : 0.0F;
            float flick2 = (float)Math.sin(Math.PI * (double)seg(c, 0.36F, 0.41F));
            add(pose[6], 0.0F, 0.0F, 3.0F * es);
            add(pose[7], 4.0F * es2 - 14.0F * flick, 0.0F, 0.0F);
            add(pose[9], 6.0F * es - 20.0F * flick, 0.0F, 0.0F);
            add(pose[10], 0.0F, 0.0F, -2.0F * es2);
            add(pose[11], 3.0F * es + 10.0F * flick2, 0.0F, 0.0F);
            add(pose[13], 5.0F * es2 + 12.0F * flick2, 0.0F, 0.0F);
            if (w <= 0.001F) {
               sway(this.leftEar, 0.0F, 0.0F, 3.0F * es);
               sway(this.leftEarMid, 4.0F * es2, 0.0F, 0.0F);
               sway(this.leftEarTip, 6.0F * es, 0.0F, 0.0F);
               sway(this.rightEar, 0.0F, 0.0F, -2.0F * es2);
               sway(this.rightEarMid, 3.0F * es, 0.0F, 0.0F);
               sway(this.rightEarTip, 5.0F * es2, 0.0F, 0.0F);
            } else {
               boolean armed = !e.getMainHandStack().isEmpty();

               for (int b = 0; b < 14; b++) {
                  ModelPart p = this.bones[b];
                  float[] v = pose[b];
                  float wb = b == 3 && armed ? w * 0.3F : w;
                  float tp = rad(v[0]);
                  float ty = rad(v[1]);
                  float tr = rad(v[2]);
                  if (b == 0) {
                     tp += p.pitch;
                     ty += p.yaw;
                  }

                  if (wb >= 0.999F) {
                     p.pitch = tp;
                     p.yaw = ty;
                     p.roll = tr;
                  } else {
                     float[] q = slerp(quatR(p.pitch, p.yaw, p.roll), quatR(tp, ty, tr), wb);
                     float[] eu = euler(q);
                     p.pitch = rad(eu[0]);
                     p.yaw = rad(eu[1]);
                     p.roll = rad(eu[2]);
                  }

                  p.pivotX = p.pivotX + v[3] * wb;
                  p.pivotY = p.pivotY + v[4] * wb;
                  p.pivotZ = p.pivotZ + v[5] * wb;
               }

               this.hat.copyTransform(this.head);
            }
         }
      }
   }

   private static void add(float[] v, float p, float y, float r) {
      v[0] += p;
      v[1] += y;
      v[2] += r;
   }

   private static void sway(ModelPart p, float pd, float yd, float rd) {
      p.pitch = p.pitch + rad(pd);
      p.yaw = p.yaw + rad(yd);
      p.roll = p.roll + rad(rd);
   }

   private static float rad(float deg) {
      return deg * (float) (Math.PI / 180.0);
   }

   private static float seg(float x, float a, float b) {
      return Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
   }

   private static float ease(float x) {
      return x * x * (3.0F - 2.0F * x);
   }

   private static float[] quat(float pitchDeg, float yawDeg, float rollDeg) {
      return quatR(rad(pitchDeg), rad(yawDeg), rad(rollDeg));
   }

   private static float[] quatR(float pitch, float yaw, float roll) {
      double cp = Math.cos((double)(pitch / 2.0F));
      double sp = Math.sin((double)(pitch / 2.0F));
      double cy = Math.cos((double)(yaw / 2.0F));
      double sy = Math.sin((double)(yaw / 2.0F));
      double cr = Math.cos((double)(roll / 2.0F));
      double sr = Math.sin((double)(roll / 2.0F));
      double w = cr * cy * cp + sr * sy * sp;
      double x = cr * cy * sp - sr * sy * cp;
      double y = cr * sy * cp + sr * cy * sp;
      double z = sr * cy * cp - cr * sy * sp;
      return new float[]{(float)w, (float)x, (float)y, (float)z};
   }

   private static float[] slerp(float[] a, float[] b, float t) {
      double d = (double)(a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3]);
      float s = 1.0F;
      if (d < 0.0) {
         d = -d;
         s = -1.0F;
      }

      double ka;
      double kb;
      if (d > 0.9995) {
         ka = (double)(1.0F - t);
         kb = (double)t;
      } else {
         double th = Math.acos(d);
         double sn = Math.sin(th);
         ka = Math.sin((double)(1.0F - t) * th) / sn;
         kb = Math.sin((double)t * th) / sn;
      }

      float[] r = new float[4];
      double len = 0.0;

      for (int i = 0; i < 4; i++) {
         r[i] = (float)((double)a[i] * ka + (double)(b[i] * s) * kb);
         len += (double)(r[i] * r[i]);
      }

      len = Math.sqrt(len);

      for (int i = 0; i < 4; i++) {
         r[i] /= (float)len;
      }

      return r;
   }

   private static float[] euler(float[] q) {
      double w = (double)q[0];
      double x = (double)q[1];
      double y = (double)q[2];
      double z = (double)q[3];
      double sy = Math.max(-1.0, Math.min(1.0, 2.0 * (w * y - x * z)));
      double yaw = Math.asin(sy);
      double pitch = Math.atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y));
      double roll = Math.atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z));
      return new float[]{(float)Math.toDegrees(pitch), (float)Math.toDegrees(yaw), (float)Math.toDegrees(roll)};
   }
}
