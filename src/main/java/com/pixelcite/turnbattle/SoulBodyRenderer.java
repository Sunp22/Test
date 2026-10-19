package com.pixelcite.turnbattle;

import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SoulBodyRenderer extends EntityRenderer<SoulBodyEntity> {
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{
      EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   };

   public SoulBodyRenderer(Context ctx) {
      super(ctx);
      this.shadowRadius = 0.5F;
   }

   public Identifier getTexture(SoulBodyEntity e) {
      UUID owner = e.getOwner();
      return owner != null
         ? MirrorCloneRenderer.MirrorFakePlayer.skinOf(owner).texture()
         : MinecraftClient.getInstance().getSkinProvider().getSkinTextures(MinecraftClient.getInstance().getGameProfile()).texture();
   }

   public void render(SoulBodyEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
      UUID owner = e.getOwner();
      if (owner != null) {
         MirrorCloneRenderer.MirrorFakePlayer fake;
         try {
            fake = MirrorCloneRenderer.MirrorFakePlayer.get("body:" + e.getUuid(), owner, e.getOwnerName());
         } catch (Throwable var14) {
            return;
         }

         if (fake != null) {
            fake.setPosition(e.getX(), e.getY(), e.getZ());
            fake.prevX = e.prevX;
            fake.prevY = e.prevY;
            fake.prevZ = e.prevZ;
            fake.lastRenderX = e.lastRenderX;
            fake.lastRenderY = e.lastRenderY;
            fake.lastRenderZ = e.lastRenderZ;
            fake.setYaw(e.getYaw());
            fake.prevYaw = e.getYaw();
            fake.bodyYaw = e.bodyYaw;
            fake.prevBodyYaw = e.bodyYaw;
            fake.headYaw = e.headYaw;
            fake.prevHeadYaw = e.headYaw;
            fake.setPitch(22.0F);
            fake.prevPitch = 22.0F;
            fake.age = e.age;
            fake.hurtTime = 0;
            fake.deathTime = 0;
            fake.handSwingProgress = 0.0F;
            fake.lastHandSwingProgress = 0.0F;
            fake.limbAnimator.setSpeed(0.0F);

            for (EquipmentSlot s : SLOTS) {
               ItemStack want = e.getEquippedStack(s);
               if (!ItemStack.areEqual(fake.getEquippedStack(s), want)) {
                  fake.equipStack(s, want.copy());
               }
            }

            EntityRenderer r = this.dispatcher.getRenderer(fake);
            if (r != null) {
               int block = LightmapTextureManager.getBlockLightCoordinates(light);
               int sky = LightmapTextureManager.getSkyLightCoordinates(light);
               int dim = LightmapTextureManager.pack(Math.round((float)block * 0.6F), Math.round((float)sky * 0.6F));
               r.render(fake, yaw, tickDelta, matrices, consumers, dim);
            }
         }
      }
   }
}
