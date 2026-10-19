package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class HumanEnemyRenderer extends BipedEntityRenderer<HumanEnemyEntity, PlayerEntityModel<HumanEnemyEntity>> {
   private final Identifier texture;

   public HumanEnemyRenderer(Context ctx, HumanEnemies.Trait trait) {
      super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
      this.texture = Identifier.of("turnbattle", "textures/entity/human/" + trait.id + ".png");
   }

   public Identifier getTexture(HumanEnemyEntity entity) {
      return this.texture;
   }

   public void render(HumanEnemyEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
      ((PlayerEntityModel)this.model).rightArmPose = e.getMainHandStack().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
      ((PlayerEntityModel)this.model).leftArmPose = ArmPose.EMPTY;
      super.render(e, yaw, tickDelta, matrices, vcp, light);
   }

   protected void scale(HumanEnemyEntity e, MatrixStack matrices, float amount) {
      matrices.scale(0.9375F, 0.9375F, 0.9375F);
   }

   static void register() {
      for (HumanEnemies.Trait t : HumanEnemies.Trait.values()) {
         EntityRendererRegistry.register(HumanEnemies.TYPES.get(t), ctx -> new HumanEnemyRenderer(ctx, t));
      }
   }
}
