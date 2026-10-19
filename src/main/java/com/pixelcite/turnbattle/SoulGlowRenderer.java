package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class SoulGlowRenderer {
   private static final Map<UUID, SoulType> glowingPlayers = new ConcurrentHashMap<>();
   private static final Map<UUID, Float> playerAlpha = new ConcurrentHashMap<>();
   private static boolean manualSummon = false;
   private static float manualAlpha = 0.0F;
   private static long lastTickRealTimeNanos = 0L;
   private static final int SOUL_PIXEL_SIZE = 12;
   private static final double REFERENCE_DISTANCE = 4.0;
   private static final double MIN_DISTANCE_FOR_SCALE = 1.0;
   private static final float MAX_SCALE = 2.0F;
   private static final float MIN_SCALE = 0.35F;
   private static volatile List<SoulGlowRenderer.Pending> pending = List.of();

   public static void setGlowing(UUID uuid, SoulType soul, boolean on) {
      if (on) {
         glowingPlayers.put(uuid, soul);
      } else {
         glowingPlayers.remove(uuid);
      }
   }

   public static void setManualSummon(boolean on) {
      manualSummon = on;
   }

   public static void tick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      lastTickRealTimeNanos = System.nanoTime();
      if (mc.world != null) {
         for (PlayerEntity p : mc.world.getPlayers()) {
            UUID id = p.getUuid();
            boolean target = glowingPlayers.containsKey(id);
            float cur = playerAlpha.getOrDefault(id, 0.0F);
            float next = target ? Math.min(1.0F, cur + 0.08F) : Math.max(0.0F, cur - 0.06F);
            if (next <= 0.0F) {
               playerAlpha.remove(id);
            } else {
               playerAlpha.put(id, next);
            }
         }

         if (manualSummon) {
            manualAlpha = Math.min(1.0F, manualAlpha + 0.1F);
         } else {
            manualAlpha = Math.max(0.0F, manualAlpha - 0.07F);
         }
      }
   }

   public static void computeScreenPositions(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world == null) {
         pending = List.of();
      } else {
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         Vec3d cam = ctx.camera().getPos();
         if (lastTickRealTimeNanos == 0L) {
            lastTickRealTimeNanos = System.nanoTime();
         }

         float secondsSinceLastTick = (float)(System.nanoTime() - lastTickRealTimeNanos) / 1.0E9F;
         float tickDelta = Math.min(1.0F, Math.max(0.0F, secondsSinceLastTick / 0.05F));
         List next = new ArrayList<>();

         for (PlayerEntity player : mc.world.getPlayers()) {
            UUID id = player.getUuid();
            boolean isLocal = mc.player != null && mc.player.getUuid().equals(id);
            float alpha = playerAlpha.getOrDefault(id, 0.0F);
            if (isLocal && manualAlpha > 0.0F) {
               alpha = Math.max(alpha, manualAlpha);
            }

            if (!(alpha <= 0.01F)) {
               SoulType soul = glowingPlayers.getOrDefault(id, isLocal ? TurnBattleClientMod.clientSoul : null);
               Identifier tex = soul != null ? soul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
               int rgb = soul != null ? soul.getColor() : 16711680;
               float bob = (float)(Math.sin((double)((float)player.age + tickDelta) * 0.1) * 0.03);
               double lerpX = MathHelper.lerp((double)tickDelta, player.prevX, player.getX());
               double lerpY = MathHelper.lerp((double)tickDelta, player.prevY, player.getY());
               double lerpZ = MathHelper.lerp((double)tickDelta, player.prevZ, player.getZ());
               double relX = lerpX - cam.x;
               double relY = lerpY + 1.15 + (double)bob - cam.y;
               double relZ = lerpZ - cam.z;
               WorldToScreen.ScreenPos pos = WorldToScreen.project(ctx, relX, relY, relZ, sw, sh);
               if (pos != null) {
                  double dist = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
                  float scale = (float)(4.0 / Math.max(dist, 1.0));
                  scale = Math.max(0.35F, Math.min(2.0F, scale));
                  next.add(new SoulGlowRenderer.Pending(pos, tex, rgb, alpha, scale));
               }
            }
         }

         pending = next;
      }
   }

   public static void renderHud(DrawContext ctx) {
      List snapshot = pending;
      if (!snapshot.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         int half = 6;

         for (SoulGlowRenderer.Pending p : snapshot) {
            float r = (float)(p.rgb() >> 16 & 0xFF) / 255.0F;
            float g = (float)(p.rgb() >> 8 & 0xFF) / 255.0F;
            float b = (float)(p.rgb() & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(r, g, b, p.alpha());
            ctx.getMatrices().push();
            ctx.getMatrices().translate((float)p.pos().x(), (float)p.pos().y(), 0.0F);
            ctx.getMatrices().scale(p.scale(), p.scale(), 1.0F);
            ctx.drawTexture(p.tex(), -half, -half, 0.0F, 0.0F, 12, 12, 12, 12);
            ctx.getMatrices().pop();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         RenderSystem.disableBlend();
      }
   }

   private static record Pending(WorldToScreen.ScreenPos pos, Identifier tex, int rgb, float alpha, float scale) {
   }
}
