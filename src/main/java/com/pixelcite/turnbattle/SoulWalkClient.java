package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class SoulWalkClient {
   private static final Identifier GRAZE_RING = Identifier.of("turnbattle", "textures/gui/graze_ring.png");
   private static final int[] RING_DELAYS = new int[]{0, 2, 4};
   private static final float RING_TICKS = 5.0F;
   private static final List<SoulWalkClient.Ring> rings = new ArrayList<>();
   private static volatile List<SoulWalkClient.Pending> pending = List.of();
   private static long ticks = 0L;
   private static long lastTickNanos = 0L;
   private static final Map<SoulType, ItemStack> SOUL_STACKS = new EnumMap<>(SoulType.class);
   private static final float SOUL_SIZE = 0.42F;
   private static final Map<UUID, Float> smoothYaw = new HashMap<>();

   private SoulWalkClient() {
   }

   public static void register() {
      Net121.registerClientReceiver(SoulWalk.SYNC, (client, handler, buf, sender) -> {
         int n = buf.readInt();
         Map next = new HashMap<>();
         SoulType[] st = SoulType.values();

         for (int i = 0; i < n; i++) {
            UUID u = buf.readUuid();
            int o = buf.readInt();
            next.put(u, o >= 0 && o < st.length ? st[o] : null);
         }

         client.execute(() -> apply(client, next));
      });
      Net121.registerClientReceiver(SoulWalk.GRAZE, (client, handler, buf, sender) -> {
         int id = buf.readInt();
         client.execute(() -> onGraze(client, id));
      });
   }

   private static void apply(MinecraftClient mc, Map<UUID, SoulType> next) {
      for (UUID u : next.keySet()) {
         if (!SoulWalk.CLIENT_OUT.containsKey(u)) {
            PlayerActions.clear(u);
         }
      }

      SoulWalk.CLIENT_OUT.clear();

      for (Entry e : next.entrySet()) {
         SoulWalk.CLIENT_OUT.put(e.getKey(), e.getValue() != null ? e.getValue() : SoulType.DETERMINATION);
      }

      boolean wasOut = SoulWalk.localOut;
      SoulWalk.localOut = mc.player != null && next.containsKey(mc.player.getUuid());
      if (SoulWalk.localOut && !wasOut) {
         SoulGlowRenderer.setManualSummon(false);
         if (mc.currentScreen instanceof HandledScreen) {
            mc.setScreen(null);
         }
      }

      if (!SoulWalk.localOut && wasOut && mc.player != null && mc.interactionManager != null) {
         GameMode gm = mc.interactionManager.getCurrentGameMode();
         if (gm != null) {
            gm.setAbilities(mc.player.getAbilities());
         }

         if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
         }
      }

      resize(mc);
   }

   private static void resize(MinecraftClient mc) {
      if (mc.world != null) {
         for (PlayerEntity p : mc.world.getPlayers()) {
            boolean small = SoulWalk.CLIENT_OUT.containsKey(p.getUuid());
            if (small != p.getHeight() < 1.0F) {
               p.calculateDimensions();
            }
         }
      }
   }

   private static void onGraze(MinecraftClient mc, int entityId) {
      if (mc.world != null) {
         Entity e = mc.world.getEntityById(entityId);
         if (e != null) {
            int rgb = 16711680;
            if (e == mc.player && TurnBattleClientMod.clientSoul != null) {
               rgb = TurnBattleClientMod.clientSoul.getColor() & 16777215;
            } else if (e instanceof PlayerEntity p) {
               for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
                  if (m.uuid().equals(p.getUuid()) && m.soul() != null) {
                     rgb = m.soul().getColor() & 16777215;
                  }
               }
            }

            rings.add(new SoulWalkClient.Ring(entityId, ticks, rgb));
            if (e == mc.player) {
               TurnBattleSounds.playClient(TurnBattleSounds.GRAZE, 1.0F, 1.0F);
            } else {
               mc.world.playSound(e.getX(), e.getY(), e.getZ(), TurnBattleSounds.GRAZE, SoundCategory.PLAYERS, 0.6F, 1.0F, false);
            }
         }
      }
   }

   public static void tick(MinecraftClient mc) {
      ticks++;
      lastTickNanos = System.nanoTime();
      rings.removeIf(r -> (float)(ticks - r.born()) > (float)RING_DELAYS[RING_DELAYS.length - 1] + 5.0F + 1.0F);
      if (mc.world != null && mc.player != null) {
         resize(mc);
         if (SoulWalk.localOut) {
            mc.player.getAbilities().allowFlying = true;
            mc.player.getAbilities().flying = true;
            if (mc.currentScreen instanceof HandledScreen) {
               mc.setScreen(null);
            }
         }
      } else {
         if (!SoulWalk.CLIENT_OUT.isEmpty()) {
            SoulWalk.CLIENT_OUT.clear();
         }

         SoulWalk.localOut = false;
      }
   }

   public static void startTick(MinecraftClient mc) {
      if (SoulWalk.localOut && mc.player != null) {
         while (mc.options.dropKey.wasPressed()) {
         }

         while (mc.options.inventoryKey.wasPressed()) {
         }

         while (mc.options.swapHandsKey.wasPressed()) {
         }
      }
   }

   private static ItemStack soulStack(SoulType soul) {
      return SOUL_STACKS.computeIfAbsent(soul, st -> {
         SoulItem item = TurnBattleMod.SOUL_ITEMS.get(st);
         return item != null ? new ItemStack(item) : ItemStack.EMPTY;
      });
   }

   public static void compute(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null && (!SoulWalk.CLIENT_OUT.isEmpty() || !rings.isEmpty())) {
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         Vec3d cam = ctx.camera().getPos();
         float td = mc.getRenderTickCounter().getTickDelta(false);
         boolean firstPerson = mc.options.getPerspective().isFirstPerson();
         VertexConsumerProvider vc = mc.getBufferBuilders().getEntityVertexConsumers();
         MatrixStack ms = new MatrixStack();
         List next = new ArrayList<>();

         for (PlayerEntity p : mc.world.getPlayers()) {
            SoulType soul = SoulWalk.CLIENT_OUT.get(p.getUuid());
            if (soul != null && (p != mc.player || !firstPerson)) {
               double x = MathHelper.lerp((double)td, p.prevX, p.getX());
               double y = MathHelper.lerp((double)td, p.prevY, p.getY()) + (double)p.getHeight() / 2.0 + Math.sin((double)((float)p.age + td) * 0.12) * 0.04;
               double z = MathHelper.lerp((double)td, p.prevZ, p.getZ());
               ItemStack stack = soulStack(soul);
               if (!stack.isEmpty()) {
                  float yaw;
                  if (p == mc.player) {
                     yaw = MathHelper.lerpAngleDegrees(td, p.prevHeadYaw, p.headYaw);
                  } else {
                     float want = p.getHeadYaw();
                     Float had = smoothYaw.get(p.getUuid());
                     yaw = had == null ? want : had + MathHelper.wrapDegrees(want - had) * 0.25F;
                     smoothYaw.put(p.getUuid(), MathHelper.wrapDegrees(yaw));
                  }

                  float pitch = p == mc.player ? MathHelper.lerp(td, p.prevPitch, p.getPitch()) : p.getPitch();
                  ms.push();
                  ms.translate(x - cam.x, y - cam.y, z - cam.z);
                  ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
                  ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-pitch * 0.5F));
                  ms.scale(0.84F, 0.84F, 0.84F);
                  mc.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, 15728880, OverlayTexture.DEFAULT_UV, ms, vc, mc.world, p.getId());
                  ms.pop();
               }
            }
         }

         for (SoulWalkClient.Ring ring : rings) {
            Entity e = mc.world.getEntityById(ring.entityId());
            if (e != null) {
               float rr = (float)(ring.rgb() >> 16 & 0xFF) / 255.0F;
               float rg = (float)(ring.rgb() >> 8 & 0xFF) / 255.0F;
               float rb = (float)(ring.rgb() & 0xFF) / 255.0F;
               boolean own = e == mc.player && firstPerson;
               Vec3d c = own
                  ? null
                  : new Vec3d(
                     MathHelper.lerp((double)td, e.prevX, e.getX()),
                     MathHelper.lerp((double)td, e.prevY, e.getY()) + 1.15,
                     MathHelper.lerp((double)td, e.prevZ, e.getZ())
                  );

               for (int d : RING_DELAYS) {
                  float local = ((float)(ticks - ring.born() - (long)d) + td) / 5.0F;
                  if (!(local < 0.0F) && !(local >= 1.0F)) {
                     if (own) {
                        next.add(
                           new SoulWalkClient.Pending(
                              (float)sw / 2.0F, (float)sh / 2.0F, 60.0F * (1.0F + local * 0.25F), GRAZE_RING, ring.rgb(), 1.0F - local, true
                           )
                        );
                     } else {
                        billboard(ctx, vc, ms, cam, GRAZE_RING, c, 1.1F * (1.0F + local * 0.25F), rr, rg, rb, 1.0F - local);
                     }
                  }
               }
            }
         }

         pending = next;
      } else {
         pending = List.of();
      }
   }

   private static void billboard(
      WorldRenderContext ctx, VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Identifier tex, Vec3d c, float size, float r, float g, float b, float alpha
   ) {
      float h = size / 2.0F;
      ms.push();
      ms.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
      ms.multiply(ctx.camera().getRotation());
      Matrix4f m = ms.peek().getPositionMatrix();
      VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(tex));
      vert(buf, m, -h, -h, 0.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, h, -h, 1.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, h, h, 1.0F, 0.0F, r, g, b, alpha);
      vert(buf, m, -h, h, 0.0F, 0.0F, r, g, b, alpha);
      ms.pop();
   }

   private static void vert(VertexConsumer buf, Matrix4f m, float x, float y, float u, float v, float r, float g, float b, float a) {
      buf.vertex(m, x, y, 0.0F).color(r, g, b, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0.0F, 1.0F, 0.0F);
   }

   public static void renderHud(DrawContext ctx) {
      List snap = pending;
      if (!snap.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         for (SoulWalkClient.Pending p : snap) {
            RenderSystem.setShaderColor(
               (float)(p.rgb() >> 16 & 0xFF) / 255.0F, (float)(p.rgb() >> 8 & 0xFF) / 255.0F, (float)(p.rgb() & 0xFF) / 255.0F, p.alpha()
            );
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(p.x(), p.y(), 0.0F);
            int sz = Math.max(4, Math.round(p.scale()));
            ctx.drawTexture(p.tex(), -sz / 2, -sz / 2, 0.0F, 0.0F, sz, sz, sz, sz);
            m.pop();
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   private static record Pending(float x, float y, float scale, Identifier tex, int rgb, float alpha, boolean ring) {
   }

   private static record Ring(int entityId, long born, int rgb) {
   }
}
