package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ShopkeeperRenderer extends BipedEntityRenderer<Shopkeeper.Entity, PlayerEntityModel<Shopkeeper.Entity>> {
   private static final Identifier TEXTURE = Identifier.of("turnbattle", "textures/entity/shopkeeper.png");

   public ShopkeeperRenderer(Context ctx) {
      super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
   }

   public Identifier getTexture(Shopkeeper.Entity entity) {
      return TEXTURE;
   }

   public void render(Shopkeeper.Entity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
      ((PlayerEntityModel)this.model).rightArmPose = ArmPose.EMPTY;
      ((PlayerEntityModel)this.model).leftArmPose = ArmPose.EMPTY;
      super.render(e, yaw, tickDelta, matrices, vcp, light);
   }

   protected void scale(Shopkeeper.Entity e, MatrixStack matrices, float amount) {
      matrices.scale(0.9375F, 0.9375F, 0.9375F);
   }

   protected boolean hasLabel(Shopkeeper.Entity e) {
      return e.hasCustomName() && super.hasLabel(e);
   }
}
