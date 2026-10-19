package com.pixelcite.turnbattle;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MirrorCloneRenderer extends EntityRenderer<MirrorCloneEntity> {
   public MirrorCloneRenderer(Context ctx) {
      super(ctx);
      this.shadowRadius = 0.5F;
   }

   public Identifier getTexture(MirrorCloneEntity e) {
      UUID owner = e.getOwner();
      return owner != null
         ? MirrorCloneRenderer.MirrorFakePlayer.skinOf(owner).texture()
         : MinecraftClient.getInstance().getSkinProvider().getSkinTextures(MinecraftClient.getInstance().getGameProfile()).texture();
   }

   public void render(MirrorCloneEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
      UUID owner = e.getOwner();
      if (owner != null) {
         MirrorCloneRenderer.MirrorFakePlayer fake;
         try {
            fake = MirrorCloneRenderer.MirrorFakePlayer.get("world:" + e.getUuid(), owner, e.getOwnerName());
         } catch (Throwable var15) {
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
            fake.prevYaw = e.prevYaw;
            fake.setPitch(e.getPitch());
            fake.prevPitch = e.prevPitch;
            fake.bodyYaw = e.bodyYaw;
            fake.prevBodyYaw = e.prevBodyYaw;
            fake.headYaw = e.headYaw;
            fake.prevHeadYaw = e.prevHeadYaw;
            fake.age = e.age;
            fake.hurtTime = e.hurtTime;
            fake.deathTime = e.deathTime;
            fake.handSwingProgress = 0.0F;
            fake.lastHandSwingProgress = 0.0F;
            ItemStack weapon = e.getMainHandStack();
            if (!ItemStack.areEqual(fake.getMainHandStack(), weapon)) {
               fake.equipStack(EquipmentSlot.MAINHAND, weapon.copy());
            }

            CloneAnimator.Frame fr = CloneAnimator.frame(UUID.nameUUIDFromBytes(("mirror:" + e.getUuid()).getBytes()), !weapon.isEmpty());
            EntityRenderer r = this.dispatcher.getRenderer(fake);
            if (r != null) {
               if (fr != null) {
                  CloneAnimator.beginModelPose(fake, fr.pose);
               }

               try {
                  r.render(fake, yaw, tickDelta, matrices, consumers, light);
               } finally {
                  if (fr != null) {
                     CloneAnimator.endModelPose();
                  }
               }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class MirrorFakePlayer extends AbstractClientPlayerEntity {
      private static final Map<String, MirrorCloneRenderer.MirrorFakePlayer> CACHE = new HashMap<>();
      final UUID skinOwner;

      private MirrorFakePlayer(MinecraftClient mc, String key, UUID skinOwner, String name) {
         super(
            mc.world,
            new GameProfile(
               UUID.nameUUIDFromBytes(("mirror-fake:" + key).getBytes()),
               name != null && !name.isEmpty() ? (name.length() > 16 ? name.substring(0, 16) : name) : "Reflection"
            )
         );
         this.skinOwner = skinOwner;
      }

      static MirrorCloneRenderer.MirrorFakePlayer get(String key, UUID skinOwner, String name) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world == null) {
            return null;
         } else {
            MirrorCloneRenderer.MirrorFakePlayer f = CACHE.get(key);
            if (f == null || f.getWorld() != mc.world || !f.skinOwner.equals(skinOwner)) {
               CACHE.values().removeIf(o -> o.getWorld() != mc.world);
               f = new MirrorCloneRenderer.MirrorFakePlayer(mc, key, skinOwner, name);
               CACHE.put(key, f);
            }

            return f;
         }
      }

      static SkinTextures skinOf(UUID owner) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ClientPlayNetworkHandler handler = mc.getNetworkHandler();
         if (handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(owner);
            if (entry != null) {
               return entry.getSkinTextures();
            }
         }

         return mc.player != null && mc.player.getUuid().equals(owner)
            ? mc.player.getSkinTextures()
            : mc.getSkinProvider().getSkinTextures(mc.getGameProfile());
      }

      public SkinTextures getSkinTextures() {
         return skinOf(this.skinOwner);
      }

      public boolean isPartVisible(PlayerModelPart p) {
         return true;
      }

      public boolean isInvisibleTo(PlayerEntity player) {
         return true;
      }

      public boolean isSpectator() {
         return false;
      }

      public boolean isCreative() {
         return false;
      }
   }
}
