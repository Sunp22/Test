package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
final class MagicantWorldFx {
   private static final Identifier TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   private static final Identifier TEX_RING = Identifier.of("turnbattle", "textures/effects/ritual_ring.png");
   static final int TINT = 10138879;
   static final int RIPPLE = 6982911;

   private MagicantWorldFx() {
   }

   static void render(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null && mc.player != null) {
         if (mc.world.getRegistryKey().getValue().equals(MagicantManager.MAGICANT_WORLD.getValue())) {
            Vec3d c = MagicantManager.arenaCenter(mc.player.getUuid());
            if (c.squaredDistanceTo(mc.player.getPos()) > 25600.0) {
               c = new Vec3d(mc.player.getX(), 1.0, mc.player.getZ());
            }

            Vec3d cam = ctx.camera().getPos();
            float td = mc.getRenderTickCounter().getTickDelta(false);
            float t = (float)(mc.world.getTime() % 240000L) + td;
            VertexConsumerProvider vc = mc.getBufferBuilders().getEntityVertexConsumers();
            MatrixStack ms = new MatrixStack();
            float[] radii = new float[]{0.0F, 3.0F, 8.0F, 16.0F, 32.0F, 64.0F};
            int segs = 32;
            VertexConsumer sea = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_GLOW));
            ms.push();
            ms.translate(c.x - cam.x, c.y + 0.002 - cam.y, c.z - cam.z);
            Matrix4f m = ms.peek().getPositionMatrix();

            for (int ri = 0; ri < radii.length - 1; ri++) {
               float r0 = radii[ri];
               float r1 = radii[ri + 1];
               float k0 = r0 / 64.0F;
               float k1 = r1 / 64.0F;

               for (int s = 0; s < segs; s++) {
                  float a0 = (float)((double)s * Math.PI * 2.0 / (double)segs);
                  float a1 = (float)((double)(s + 1) * Math.PI * 2.0 / (double)segs);
                  seaVert(sea, m, r0, a0, k0);
                  seaVert(sea, m, r1, a0, k1);
                  seaVert(sea, m, r1, a1, k1);
                  seaVert(sea, m, r0, a1, k0);
               }
            }

            ms.pop();

            for (int i = 0; i < 6; i++) {
               shaft(vc, ms, cam, c, 1.6F * (1.0F - (float)i * 0.14F), 12.0F);
            }

            flat(vc, ms, cam, TEX_GLOW, c.add(0.0, 0.004, 0.0), 2.6F, 10138879, 0.45F);
            flat(vc, ms, cam, TEX_GLOW, c.add(0.0, 0.005, 0.0), 1.04F, 16777215, 0.3F);

            for (int r = 0; r < 4; r++) {
               float q = (t * 0.005F + (float)r / 4.0F) % 1.0F;
               flat(vc, ms, cam, TEX_RING, c.add(0.0, 0.006 + (double)r * 5.0E-4, 0.0), 0.6F + q * 4.5F, 6982911, 0.4F * (1.0F - q));
            }

            for (int i = 0; i < 90; i++) {
               float ox = (CloneAnimator.rnd(i * 3 + 1) - 0.5F) * 14.0F;
               float oz = (CloneAnimator.rnd(i * 3 + 2) - 0.5F) * 14.0F;
               boolean glint = i % 3 == 0;
               float oy = glint ? 0.02F : (CloneAnimator.rnd(i * 3 + 3) + t * (0.003F + CloneAnimator.rnd(i * 7) * 0.004F)) % 1.0F * 4.5F;
               float life = glint
                  ? 0.5F + 0.5F * (float)Math.sin((double)(t * 0.08F + (float)i))
                  : (float)Math.sin(Math.PI * (double)Math.max(0.0F, Math.min(1.0F, oy / 4.5F)));
               float a = (glint ? 0.35F : 0.45F) * life;
               if (!(a <= 0.01F)) {
                  int rgb = i % 5 == 0 ? 16777215 : 10138879;
                  mote(ctx, vc, ms, cam, c.add((double)ox, (double)oy, (double)oz), glint ? 0.07F : 0.035F, 0.035F, rgb, a);
               }
            }
         }
      }
   }

   private static void seaVert(VertexConsumer buf, Matrix4f m, float r, float a, float k) {
      float x = (float)Math.cos((double)a) * r;
      float z = (float)Math.sin((double)a) * r;
      float cr = mix(4.0F, 10.0F, k) / 255.0F;
      float cg = mix(8.0F, 26.0F, k) / 255.0F;
      float cb = mix(26.0F, 58.0F, k) / 255.0F;
      float al = mix(0.8F, 0.4F, k) * (1.0F - Math.max(0.0F, k - 0.75F) * 4.0F);
      buf.vertex(m, x, 0.0F, z).color(cr, cg, cb, al).texture(0.5F, 0.5F).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0.0F, 1.0F, 0.0F);
   }

   private static void flat(VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Identifier tex, Vec3d c, float size, int rgb, float alpha) {
      if (!(alpha <= 0.003F)) {
         float h = size / 2.0F;
         float r = (float)(rgb >> 16 & 0xFF) / 255.0F;
         float g = (float)(rgb >> 8 & 0xFF) / 255.0F;
         float b = (float)(rgb & 0xFF) / 255.0F;
         ms.push();
         ms.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
         Matrix4f m = ms.peek().getPositionMatrix();
         VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(tex));
         vert(buf, m, -h, 0.0F, -h, 0.0F, 0.0F, r, g, b, alpha);
         vert(buf, m, -h, 0.0F, h, 0.0F, 1.0F, r, g, b, alpha);
         vert(buf, m, h, 0.0F, h, 1.0F, 1.0F, r, g, b, alpha);
         vert(buf, m, h, 0.0F, -h, 1.0F, 0.0F, r, g, b, alpha);
         ms.pop();
      }
   }

   private static void shaft(VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Vec3d base, float width, float height) {
      double dx = base.x - cam.x;
      double dz = base.z - cam.z;
      double len = Math.sqrt(dx * dx + dz * dz);
      float sx = len < 0.001 ? 1.0F : (float)(-dz / len);
      float sz = len < 0.001 ? 0.0F : (float)(dx / len);
      float hw = width / 2.0F;
      float r = 0.6039216F;
      float g = 0.7058824F;
      float b = 1.0F;
      ms.push();
      ms.translate(base.x - cam.x, base.y - cam.y, base.z - cam.z);
      Matrix4f m = ms.peek().getPositionMatrix();
      VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_GLOW));
      vert(buf, m, -sx * hw, 0.0F, -sz * hw, 0.5F, 0.5F, r, g, b, 0.02F);
      vert(buf, m, sx * hw, 0.0F, sz * hw, 0.5F, 0.5F, r, g, b, 0.02F);
      vert(buf, m, sx * hw, height, sz * hw, 0.5F, 0.5F, r, g, b, 0.0F);
      vert(buf, m, -sx * hw, height, -sz * hw, 0.5F, 0.5F, r, g, b, 0.0F);
      ms.pop();
   }

   private static void mote(WorldRenderContext ctx, VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Vec3d p, float w, float h, int rgb, float alpha) {
      float r = (float)(rgb >> 16 & 0xFF) / 255.0F;
      float g = (float)(rgb >> 8 & 0xFF) / 255.0F;
      float b = (float)(rgb & 0xFF) / 255.0F;
      ms.push();
      ms.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
      ms.multiply(ctx.camera().getRotation());
      Matrix4f m = ms.peek().getPositionMatrix();
      VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_GLOW));
      float hw = w / 2.0F;
      float hh = h / 2.0F;
      vert(buf, m, -hw, -hh, 0.0F, 0.5F, 0.5F, r, g, b, alpha);
      vert(buf, m, hw, -hh, 0.0F, 0.5F, 0.5F, r, g, b, alpha);
      vert(buf, m, hw, hh, 0.0F, 0.5F, 0.5F, r, g, b, alpha);
      vert(buf, m, -hw, hh, 0.0F, 0.5F, 0.5F, r, g, b, alpha);
      ms.pop();
   }

   private static void vert(VertexConsumer buf, Matrix4f m, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
      buf.vertex(m, x, y, z).color(r, g, b, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0.0F, 1.0F, 0.0F);
   }

   private static float mix(float a, float b, float k) {
      return a + (b - a) * k;
   }
}
