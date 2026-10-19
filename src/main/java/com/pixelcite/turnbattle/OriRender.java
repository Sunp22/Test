package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class OriRender {
   public static final Identifier TEXTURE = Identifier.of("turnbattle", "textures/entity/ori.png");
   private static OriRender.OriPlayerRenderer renderer;
   private static boolean broken;

   private OriRender() {
   }

   public static EntityRenderer<?> rendererFor(Entity entity) {
      if (broken) {
         return null;
      } else {
         boolean ori = entity instanceof BattleEntityRenderer.RecruitFakePlayer f ? isOri(f.recruitName) : DepthsClient.isOri(entity);
         if (!ori) {
            return null;
         } else {
            if (renderer == null) {
               try {
                  MinecraftClient mc = MinecraftClient.getInstance();
                  Context ctx = new Context(
                     mc.getEntityRenderDispatcher(),
                     mc.getItemRenderer(),
                     mc.getBlockRenderManager(),
                     mc.getEntityRenderDispatcher().getHeldItemRenderer(),
                     mc.getResourceManager(),
                     mc.getEntityModelLoader(),
                     mc.textRenderer
                  );
                  renderer = new OriRender.OriPlayerRenderer(ctx);
               } catch (Throwable var4) {
                  TurnBattleMod.LOGGER.warn("[TurnBattle] Couldn't set up Ori's model, she'll use a player model: {}", var4.toString());
                  broken = true;
                  return null;
               }
            }

            return renderer;
         }
      }
   }

   static boolean isOri(String recruitName) {
      if (!Recruit.ORI.displayName.equals(recruitName)) {
         return false;
      } else {
         String[] custom = TurnBattleClientMod.customRecruitNames;
         if (custom != null) {
            boolean ownsStoryOri = TurnBattleClientMod.clientRecruitUnlocked != null
               && Recruit.ORI.ordinal() < TurnBattleClientMod.clientRecruitUnlocked.length
               && TurnBattleClientMod.clientRecruitUnlocked[Recruit.ORI.ordinal()];
            if (!ownsStoryOri) {
               for (String n : custom) {
                  if (recruitName.equals(n)) {
                     return false;
                  }
               }
            }
         }

         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class OriPlayerRenderer extends PlayerEntityRenderer {
      OriPlayerRenderer(Context ctx) {
         super(ctx, true);
         this.model = new OriModel(OriModel.getTexturedModelData().createModel());
         this.features.removeIf(f -> f instanceof HeldItemFeatureRenderer);
         this.addFeature(new DepthsLantern.HeldItems(this, ctx.getHeldItemRenderer()));
         this.addFeature(new DepthsLantern.Feature(this));
      }

      public Identifier getTexture(AbstractClientPlayerEntity player) {
         return OriRender.TEXTURE;
      }
   }
}
