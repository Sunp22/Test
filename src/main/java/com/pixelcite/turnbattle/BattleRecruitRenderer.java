package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class BattleRecruitRenderer extends EntityRenderer<BattleRecruitEntity> {
   public BattleRecruitRenderer(Context ctx) {
      super(ctx);
      this.shadowRadius = 0.5F;
   }

   public Identifier getTexture(BattleRecruitEntity e) {
      return RecruitSkinManager.getSkin(e.getRecruitName());
   }

   public void render(BattleRecruitEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
      String name = e.getRecruitName();
      if (name != null && !name.isEmpty()) {
         BattleEntityRenderer.RecruitFakePlayer fake;
         try {
            fake = BattleEntityRenderer.recruitFake(name);
         } catch (Throwable var11) {
            return;
         }

         fake.setPosition(e.getX(), e.getY(), e.getZ());
         fake.prevX = e.prevX;
         fake.prevY = e.prevY;
         fake.prevZ = e.prevZ;
         fake.lastRenderX = e.lastRenderX;
         fake.lastRenderY = e.lastRenderY;
         fake.lastRenderZ = e.lastRenderZ;
         fake.setYaw(e.getYaw());
         fake.prevYaw = e.prevYaw;
         fake.setPitch(e.getPitch());
         fake.prevPitch = e.prevPitch;
         fake.bodyYaw = e.bodyYaw;
         fake.prevBodyYaw = e.prevBodyYaw;
         fake.headYaw = e.headYaw;
         fake.prevHeadYaw = e.prevHeadYaw;
         fake.age = e.age;
         fake.handSwingProgress = 0.0F;
         fake.lastHandSwingProgress = 0.0F;
         ItemStack weapon = BattleEntityRenderer.recruitWeapon(name);
         if (!ItemStack.areEqual(fake.getMainHandStack(), weapon)) {
            fake.equipStack(EquipmentSlot.MAINHAND, weapon.copy());
         }

         EntityRenderer r = this.dispatcher.getRenderer(fake);
         if (r != null) {
            r.render(fake, yaw, tickDelta, matrices, consumers, light);
         }
      }
   }
}
