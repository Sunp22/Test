package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
final class DarkFountainDoorGlow {
   private static final Identifier TEX_WHITE = Identifier.of("turnbattle", "textures/effects/fountain_white.png");
   private static final double RANGE = 64.0;
   private static final int RAYS = 11;
   private static final float REACH = 2.9F;
   private static final float SPREAD = 1.5F;
   private static final float SQUARE_EVERY = 9.0F;
   private static final float SQUARE_LIFE = 34.0F;
   private static final Map<Long, Float> openness = new HashMap<>();
   private static long lastNanos = System.nanoTime();

   private DarkFountainDoorGlow() {
   }

   static void render(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientWorld w = mc.world;
      if (w != null && mc.player != null) {
         long now = System.nanoTime();
         float dt = Math.min(0.25F, (float)(now - lastNanos) / 1.0E9F);
         lastNanos = now;
         String dim = w.getRegistryKey().getValue().toString();
         Vec3d cam = ctx.camera().getPos();
         float t = (float)(w.getTime() % 72000L) + mc.getRenderTickCounter().getTickDelta(false);
         VertexConsumer buf = null;
         Matrix4f m = new MatrixStack().peek().getPositionMatrix();

         for (DarkFountainSites.Site s : DarkFountainSites.sites) {
            if (s.dim().equals(dim)) {
               for (DarkFountainSites.Door d : s.doors()) {
                  BlockPos pos = d.pos();
                  if (!(cam.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) > 4096.0) && w.isChunkLoaded(pos)) {
                     BlockState st = w.getBlockState(pos);
                     if (st.getBlock() instanceof DoorBlock) {
                        boolean isOpen = (Boolean)st.get(DoorBlock.OPEN);
                        float o = openness.getOrDefault(pos.asLong(), isOpen ? 1.0F : 0.0F);
                        o = isOpen ? Math.min(1.0F, o + dt / 0.45F) : Math.max(0.0F, o - dt / 0.3F);
                        openness.put(pos.asLong(), o);
                        VoxelShape shape = st.getOutlineShape(w, pos);
                        if (!shape.isEmpty()) {
                           if (buf == null) {
                              buf = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_WHITE));
                           }

                           drawDoor(buf, m, cam, w, pos, shape.getBoundingBox(), d.out(), t, smooth(o), isOpen);
                        }
                     }
                  }
               }
            }
         }

         if (openness.size() > 256) {
            openness.clear();
         }
      }
   }

   private static void drawDoor(
      VertexConsumer buf, Matrix4f m, Vec3d cam, ClientWorld w, BlockPos pos, Box box, Direction out, float t, float open, boolean isOpen
   ) {
      double ox = (double)out.getOffsetX();
      double oz = (double)out.getOffsetZ();
      double lxv = -oz;
      double lzv = ox;
      double face;
      double lat0;
      double lat1;
      switch (out) {
         case EAST:
            face = box.maxX - 0.5;
            lat0 = box.minZ - 0.5;
            lat1 = box.maxZ - 0.5;
            break;
         case WEST:
            face = 0.5 - box.minX;
            lat0 = -(box.maxZ - 0.5);
            lat1 = -(box.minZ - 0.5);
            break;
         case SOUTH:
            face = box.maxZ - 0.5;
            lat0 = -(box.maxX - 0.5);
            lat1 = -(box.minX - 0.5);
            break;
         default:
            face = 0.5 - box.minZ;
            lat0 = box.minX - 0.5;
            lat1 = box.maxX - 0.5;
      }

      if (lat1 < lat0) {
         double tmp = lat0;
         lat0 = lat1;
         lat1 = tmp;
      }

      if (isOpen) {
         face = -0.5;
         lat0 = -0.5;
         lat1 = 0.5;
      }

      double cx = (double)pos.getX() + 0.5 - cam.x;
      double cz = (double)pos.getZ() + 0.5 - cam.z;
      double fy = (double)pos.getY() - cam.y;
      BlockPos front = pos.offset(out);
      double lift = 0.012;
      if (w.isChunkLoaded(front)) {
         VoxelShape fs = w.getBlockState(front).getOutlineShape(w, front);
         if (!fs.isEmpty() && fs.getMax(Axis.Y) < 0.5) {
            lift += fs.getMax(Axis.Y);
         }
      }

      float flicker = 0.88F + 0.12F * (float)Math.sin((double)(t * 0.07F)) * (float)Math.sin((double)(t * 0.031F + 1.1F));
      float shut = 1.0F - open;
      if (shut > 0.01F) {
         double fo = face + 0.004;
         quadV(buf, m, cx, cz, ox, oz, lxv, ox, fo, lat0, lat1, fy, fy + 0.07, 0.9F * flicker * shut, 0.9F * flicker * shut);
         quadV(buf, m, cx, cz, ox, oz, lxv, ox, fo + 0.001, lat0, lat1, fy, fy + 0.55, 0.45F * flicker * shut, 0.0F);
      }

      double gy = fy + lift;
      double width = lat1 - lat0;
      double mid = (lat0 + lat1) * 0.5;
      float reachK = 1.0F + 1.5F * open;
      float darkK = 1.0F + 0.55F * open;
      float spreadK = 1.0F + 0.6F * open;

      for (int i = 0; i < 11; i++) {
         double a0 = lat0 + width * (double)i / 11.0;
         double a1 = lat0 + width * (double)(i + 1) / 11.0;
         float ray = 0.5F + 0.5F * (float)Math.sin((double)((float)i * 1.93F + t * 0.045F)) * (float)Math.sin((double)((float)i * 0.71F - t * 0.028F + 2.0F));
         float reach = 2.9F * reachK * (0.72F + 0.45F * ray);
         float base = Math.min(0.92F, (0.38F + 0.3F * ray) * flicker * darkK);
         double fan = 1.0 + (double)(1.5F * spreadK * (reach / (2.9F * reachK))) * 1.6;
         floor(buf, m, cx, cz, ox, oz, lxv, lzv, face, a0, a1, face + (double)reach, mid + (a0 - mid) * fan, mid + (a1 - mid) * fan, gy, base);
      }

      floor(
         buf,
         m,
         cx,
         cz,
         ox,
         oz,
         lxv,
         lzv,
         face,
         lat0 - 0.05,
         lat1 + 0.05,
         face + (double)(2.9F * reachK) * 0.55,
         lat0 - 0.6 * (double)spreadK,
         lat1 + 0.6 * (double)spreadK,
         gy + 0.001,
         Math.min(0.85F, 0.32F * flicker * darkK)
      );
      if (!(open <= 0.01F)) {
         double dw = 0.5;
         double dmid = fy + 1.0;
         quadV(buf, m, cx, cz, ox, oz, lxv, lzv, 0.0, -dw, dw, fy, fy + 2.0, 0.62F * open * flicker, 0.42F * open * flicker);
         int newest = (int)Math.floor((double)(t / 9.0F));

         for (int n = newest - (int)Math.ceil(3.7777777F); n <= newest; n++) {
            float age = (t - (float)n * 9.0F) / 34.0F;
            if (!(age < 0.0F) && !(age > 1.0F)) {
               float grow = 1.0F - (1.0F - age) * (1.0F - age);
               double half = 0.25 + 1.35 * (double)grow;
               double outward = 0.12 + 1.0 * (double)grow;
               float alpha = 0.55F * open * (1.0F - age) * (1.0F - age) * (0.6F + 0.4F * (1.0F - grow));
               double ring = Math.max(0.06, half * 0.22);
               rippleSquare(buf, m, cx, cz, ox, oz, lxv, lzv, outward, dmid, half, ring, alpha, t * 0.18F + (float)n * 2.3F, n);
            }
         }
      }
   }

   private static void rippleSquare(
      VertexConsumer buf,
      Matrix4f m,
      double cx,
      double cz,
      double ox,
      double oz,
      double lx,
      double lz,
      double out,
      double ymid,
      double half,
      double ring,
      float alpha,
      float phase,
      int seed
   ) {
      if (!(alpha <= 0.004F)) {
         double tilt = Math.toRadians(4.0 * Math.sin((double)phase * 0.7 + (double)seed));
         int seg = 6;
         double[][] side = new double[][]{{-1.0, -1.0, 1.0, -1.0}, {1.0, -1.0, 1.0, 1.0}, {1.0, 1.0, -1.0, 1.0}, {-1.0, 1.0, -1.0, -1.0}};

         for (int si = 0; si < 4; si++) {
            double u0 = side[si][0];
            double v0 = side[si][1];
            double u1 = side[si][2];
            double v1 = side[si][3];
            double nu = -(v1 - v0) / 2.0;
            double nv = (u1 - u0) / 2.0;

            for (int k = 0; k < seg; k++) {
               double f0 = (double)k / (double)seg;
               double f1 = (double)(k + 1) / (double)seg;
               double r0 = ripple(phase, si, f0);
               double r1 = ripple(phase, si, f1);
               double[] a = pt(u0 + (u1 - u0) * f0, v0 + (v1 - v0) * f0, half, r0, nu, nv, 0.0, tilt);
               double[] b = pt(u0 + (u1 - u0) * f1, v0 + (v1 - v0) * f1, half, r1, nu, nv, 0.0, tilt);
               double[] c = pt(u0 + (u1 - u0) * f1, v0 + (v1 - v0) * f1, half, r1, nu, nv, ring, tilt);
               double[] d = pt(u0 + (u1 - u0) * f0, v0 + (v1 - v0) * f0, half, r0, nu, nv, ring, tilt);
               vert(buf, m, cx, cz, ox, oz, lx, lz, out, ymid, a, alpha);
               vert(buf, m, cx, cz, ox, oz, lx, lz, out, ymid, b, alpha);
               vert(buf, m, cx, cz, ox, oz, lx, lz, out, ymid, c, alpha * 0.35F);
               vert(buf, m, cx, cz, ox, oz, lx, lz, out, ymid, d, alpha * 0.35F);
            }
         }
      }
   }

   private static double ripple(float phase, int side, double f) {
      double env = Math.sin(Math.PI * f);
      return env * (0.06 * Math.sin((double)phase * 1.3 + (double)side * 1.7 + f * 9.0) + 0.04 * Math.sin((double)phase * 2.1 - f * 14.0 + (double)side));
   }

   private static double[] pt(double u, double v, double half, double bow, double nu, double nv, double inset, double tilt) {
      double pu = u * half + nu * (inset - bow * half);
      double pv = v * half + nv * (inset - bow * half);
      double c = Math.cos(tilt);
      double s = Math.sin(tilt);
      return new double[]{pu * c - pv * s, pu * s + pv * c};
   }

   private static void vert(
      VertexConsumer buf, Matrix4f m, double cx, double cz, double ox, double oz, double lx, double lz, double out, double ymid, double[] p, float alpha
   ) {
      v(buf, m, (float)(cx + ox * out + lx * p[0]), (float)(ymid + p[1]), (float)(cz + oz * out + lz * p[0]), alpha);
   }

   private static void quadV(
      VertexConsumer buf,
      Matrix4f m,
      double cx,
      double cz,
      double ox,
      double oz,
      double lx,
      double lz,
      double dist,
      double a0,
      double a1,
      double y0,
      double y1,
      float alpha0,
      float alpha1
   ) {
      float x0 = (float)(cx + ox * dist + lx * a0);
      float z0 = (float)(cz + oz * dist + lz * a0);
      float x1 = (float)(cx + ox * dist + lx * a1);
      float z1 = (float)(cz + oz * dist + lz * a1);
      v(buf, m, x0, (float)y0, z0, alpha0);
      v(buf, m, x1, (float)y0, z1, alpha0);
      v(buf, m, x1, (float)y1, z1, alpha1);
      v(buf, m, x0, (float)y1, z0, alpha1);
   }

   private static void floor(
      VertexConsumer buf,
      Matrix4f m,
      double cx,
      double cz,
      double ox,
      double oz,
      double lx,
      double lz,
      double near,
      double a0,
      double a1,
      double far,
      double e0,
      double e1,
      double y,
      float alpha
   ) {
      v(buf, m, (float)(cx + ox * near + lx * a0), (float)y, (float)(cz + oz * near + lz * a0), alpha);
      v(buf, m, (float)(cx + ox * near + lx * a1), (float)y, (float)(cz + oz * near + lz * a1), alpha);
      v(buf, m, (float)(cx + ox * far + lx * e1), (float)y, (float)(cz + oz * far + lz * e1), 0.0F);
      v(buf, m, (float)(cx + ox * far + lx * e0), (float)y, (float)(cz + oz * far + lz * e0), 0.0F);
   }

   private static void v(VertexConsumer buf, Matrix4f m, float x, float y, float z, float a) {
      buf.vertex(m, x, y, z)
         .color(0.0F, 0.0F, 0.0F, Math.max(0.0F, Math.min(1.0F, a)))
         .texture(0.5F, 0.5F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(0.0F, 1.0F, 0.0F);
   }

   private static float smooth(float x) {
      return x * x * (3.0F - 2.0F * x);
   }
}
