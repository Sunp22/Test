package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.Heightmap.Type;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
final class DarkFountainPillarRenderer {
   private static final Identifier TEX_FLOW = Identifier.of("turnbattle", "textures/effects/fountain_flow.png");
   private static final Identifier TEX_WHITE = Identifier.of("turnbattle", "textures/effects/fountain_white.png");
   private static final Identifier TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   private static double AX = 0.5;
   private static double AZ = 0.5;
   static boolean present;
   private static final float RADIUS = 3.4F;
   private static final float TILE_H = 10.5F;
   private static final float CORNER = 1.5F;
   private static final int[] PALETTE = new int[]{16051112, 10940494, 7270306};
   private static final int MAX_ROWS = 2048;
   private static final float[] Y0 = new float[2048];
   private static final float[] Y1 = new float[2048];
   private static final float[] SH = new float[2048];
   private static final float[] RR = new float[2048];
   private static final float[] MUL = new float[2048];
   private static final float[] SHG = new float[2048];
   private static final float[] RG = new float[2048];
   private static ClientWorld baseWorld;
   private static int baseY;
   private static long baseAt = Long.MIN_VALUE;
   private static double baseCamY = Double.NaN;
   private static final float[] PS = new float[8200];
   private static final float[] PY = new float[8200];
   private static final float[] PD = new float[8200];
   private static final int[] PI = new int[8200];

   private DarkFountainPillarRenderer() {
   }

   static double floorY(ClientWorld w) {
      return baseWorld == w ? (double)baseY : (double)w.getSeaLevel();
   }

   static void render(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientWorld w = mc.world;
      if (w != null && mc.player != null) {
         present = false;
         String dimId = w.getRegistryKey().getValue().toString();
         if (dimId.startsWith("turnbattle:dark_world_")) {
            int[] at = DarkFountainSites.fountainOf(dimId);
            if (at != null) {
               int fx = at[0];
               int fz = at[1];
               AX = (double)fx + 0.5;
               AZ = (double)fz + 0.5;
               if (w.getChunkManager().isChunkLoaded(fx >> 4, fz >> 4)) {
                  int bottom = w.getBottomY();
                  int top = w.getTopY();
                  if (w.getBlockState(new BlockPos(fx, bottom + 1, fz)).isOf(TurnBattleMod.DARK_FOUNTAIN_BLOCK)) {
                     Vec3d cam = ctx.camera().getPos();
                     double ddx = AX - cam.x;
                     double ddz = AZ - cam.z;
                     double dist = Math.sqrt(ddx * ddx + ddz * ddz);
                     if (!(dist > 640.0)) {
                        present = true;
                        float near = 1.0F;
                        long now = w.getTime();
                        if (baseWorld != w || now - baseAt > 40L || now < baseAt || Math.abs(cam.y - baseCamY) > 6.0) {
                           baseWorld = w;
                           baseAt = now;
                           baseCamY = cam.y;
                           baseY = findFloor(w, bottom, top, cam.y);
                        }

                        float base = (float)baseY;
                        float yLoF = (float)Math.max((double)base, cam.y - 160.0);
                        float yHiF = (float)Math.min((double)top, cam.y + 220.0);
                        if (!(yHiF <= yLoF)) {
                           Frustum fr = ctx.frustum();
                           if (fr == null || fr.isVisible(new Box(AX - 6.0, (double)yLoF, AZ - 6.0, AX + 6.0, (double)yHiF, AZ + 6.0))) {
                              float td = mc.getRenderTickCounter().getTickDelta(false);
                              float t = (float)(now % 72000L) + td;
                              boolean inside = dist < 4.2500001192092896;
                              float tcx = dist < 0.001 ? 1.0F : (float)(-ddx / dist);
                              float tcz = dist < 0.001 ? 0.0F : (float)(-ddz / dist);
                              float rx = tcz;
                              float rz = -tcx;
                              float ox = (float)(AX - cam.x);
                              float oz = (float)(AZ - cam.z);
                              float cy = (float)cam.y;
                              float k = 0.5F - 0.5F * (float)Math.cos((double)t * Math.PI * 2.0 / 360.0);
                              int tint = k < 0.5F ? lerpRgb(PALETTE[0], PALETTE[1], k * 2.0F) : lerpRgb(PALETTE[1], PALETTE[2], (k - 0.5F) * 2.0F);
                              float tr = (float)(tint >> 16 & 0xFF) / 255.0F;
                              float tg = (float)(tint >> 8 & 0xFF) / 255.0F;
                              float tb = (float)(tint & 0xFF) / 255.0F;
                              int n = 0;
                              int base8 = baseY * 8;
                              int yi = Math.max(base8, (int)Math.floor((double)(yLoF * 8.0F)));
                              int yEnd = (int)Math.ceil((double)(yHiF * 8.0F));

                              while (yi < yEnd && n < 2048) {
                                 float y = (float)yi / 8.0F;
                                 float z = y - base;
                                 float far = Math.abs(y - cy);
                                 int hs = z < 2.0F ? 1 : (far < 40.0F ? 4 : (far < 100.0F ? 8 : 16));

                                 while (hs > 1 && Math.floorMod(yi, hs) != 0) {
                                    hs >>= 1;
                                 }

                                 if (yi + hs > yEnd) {
                                    hs = Math.max(1, yEnd - yi);
                                 }

                                 float y1 = (float)(yi + hs) / 8.0F;
                                 float ym = (y + y1) * 0.5F;
                                 float zm = ym - base;
                                 float ph = ym * 0.42F - t * 0.11F;
                                 float r = 3.4F * (1.0F + 0.06F * (float)Math.sin((double)ph) + 0.03F * (float)Math.sin((double)(ym * 1.13F - t * 0.23F)));
                                 float sh = 0.16F * (float)Math.sin((double)(ym * 0.93F + t * 0.17F))
                                    + 0.08F * (float)Math.sin((double)(ym * 2.4F - t * 0.29F));
                                 float shg = 0.16F * (float)Math.sin((double)(ym * 0.93F + t * 0.17F + 1.9F))
                                    + 0.1F * (float)Math.sin((double)(ym * 1.7F - t * 0.37F));
                                 Y0[n] = y;
                                 Y1[n] = y1;
                                 SH[n] = sh;
                                 RR[n] = rounded(r, zm, 1.5F);
                                 SHG[n] = shg;
                                 RG[n] = zm - 0.42F < 0.0F ? -1.0F : rounded(r - 0.42F, zm - 0.42F, 1.2F);
                                 MUL[n] = 0.74F + 0.26F * (0.5F + 0.5F * (float)Math.sin((double)(ym * 0.3F - t * 0.14F)));
                                 n++;
                                 yi += hs;
                              }

                              if (n != 0) {
                                 VertexConsumerProvider vcp = mc.getBufferBuilders().getEntityVertexConsumers();
                                 MatrixStack ms = new MatrixStack();
                                 Matrix4f m = ms.peek().getPositionMatrix();
                                 float scroll = frac(t * 0.01F);
                                 float scroll2 = frac(t * 0.017F);
                                 VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_FLOW));

                                 for (int i = 0; i < n; i++) {
                                    float mul = MUL[i];
                                    band(
                                       body,
                                       m,
                                       ox,
                                       oz,
                                       cy,
                                       rx,
                                       rz,
                                       tcx,
                                       tcz,
                                       dist,
                                       inside,
                                       i,
                                       0.25F,
                                       -0.0952381F,
                                       scroll,
                                       tr * mul,
                                       tg * mul,
                                       tb * mul,
                                       0.95F * near,
                                       false
                                    );
                                 }

                                 VertexConsumer glow = vcp.getBuffer(RenderLayer.getEyes(TEX_FLOW));
                                 float sr = mix(tr, 1.0F, 0.35F) * 0.42F * near;
                                 float sg = mix(tg, 1.0F, 0.35F) * 0.42F * near;
                                 float sb = mix(tb, 1.0F, 0.35F) * 0.42F * near;

                                 for (int i = 0; i < n; i++) {
                                    float mul = MUL[i];
                                    band(
                                       glow,
                                       m,
                                       ox,
                                       oz,
                                       cy,
                                       rx,
                                       rz,
                                       tcx,
                                       tcz,
                                       dist,
                                       inside,
                                       i,
                                       -0.32F,
                                       -0.1388889F,
                                       scroll2,
                                       sr * mul,
                                       sg * mul,
                                       sb * mul,
                                       1.0F,
                                       true
                                    );
                                 }

                                 if (!inside) {
                                    VertexConsumer line = vcp.getBuffer(RenderLayer.getEyes(TEX_WHITE));
                                    float wd = Math.max(0.13F, (float)dist * 0.0042F);
                                    float pulse = (0.85F + 0.15F * (float)Math.sin((double)(t * 0.21F))) * near;
                                    outline(
                                       line,
                                       m,
                                       ox,
                                       oz,
                                       cy,
                                       rx,
                                       rz,
                                       tcx,
                                       tcz,
                                       dist,
                                       n,
                                       false,
                                       wd * 4.0F,
                                       tr * 0.2F * pulse,
                                       tg * 0.2F * pulse,
                                       tb * 0.2F * pulse
                                    );
                                    outline(
                                       line,
                                       m,
                                       ox,
                                       oz,
                                       cy,
                                       rx,
                                       rz,
                                       tcx,
                                       tcz,
                                       dist,
                                       n,
                                       true,
                                       wd * 0.75F,
                                       tr * 0.55F * near,
                                       tg * 0.55F * near,
                                       tb * 0.55F * near
                                    );
                                    outline(
                                       line,
                                       m,
                                       ox,
                                       oz,
                                       cy,
                                       rx,
                                       rz,
                                       tcx,
                                       tcz,
                                       dist,
                                       n,
                                       false,
                                       wd,
                                       mix(tr, 1.0F, 0.35F) * pulse,
                                       mix(tg, 1.0F, 0.35F) * pulse,
                                       mix(tb, 1.0F, 0.35F) * pulse
                                    );
                                 }

                                 if (baseY > bottom + 1 && (double)base > cam.y - 160.0 && (double)base < cam.y + 220.0) {
                                    VertexConsumer pool = vcp.getBuffer(RenderLayer.getEyes(TEX_GLOW));
                                    float h = 8.84F;
                                    float gy = base + 0.03F - cy;
                                    float pr = tr * 0.32F * near;
                                    float pg = tg * 0.32F * near;
                                    float pb = tb * 0.32F * near;
                                    v(pool, m, ox - h, gy, oz - h, 0.0F, 0.0F, pr, pg, pb);
                                    v(pool, m, ox - h, gy, oz + h, 0.0F, 1.0F, pr, pg, pb);
                                    v(pool, m, ox + h, gy, oz + h, 1.0F, 1.0F, pr, pg, pb);
                                    v(pool, m, ox + h, gy, oz - h, 1.0F, 0.0F, pr, pg, pb);
                                    v(pool, m, ox + h, gy, oz - h, 1.0F, 0.0F, pr, pg, pb);
                                    v(pool, m, ox + h, gy, oz + h, 1.0F, 1.0F, pr, pg, pb);
                                    v(pool, m, ox - h, gy, oz + h, 0.0F, 1.0F, pr, pg, pb);
                                    v(pool, m, ox - h, gy, oz - h, 0.0F, 0.0F, pr, pg, pb);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void band(
      VertexConsumer buf,
      Matrix4f m,
      float ox,
      float oz,
      float cy,
      float rx,
      float rz,
      float tcx,
      float tcz,
      double dist,
      boolean inside,
      int i,
      float uHalf,
      float vPerBlock,
      float scroll,
      float r,
      float g,
      float b,
      float a,
      boolean bothSides
   ) {
      float rr = RR[i];
      float sh = SH[i];
      if (!(rr <= 0.01F)) {
         float y0 = Y0[i] - cy;
         float y1 = Y1[i] - cy;
         float v0 = Y1[i] * vPerBlock + scroll;
         float v1 = Y0[i] * vPerBlock + scroll;
         int segs = inside ? 24 : 12;
         float aMin;
         float aMax;
         float sinA;
         if (inside) {
            aMin = (float) -Math.PI;
            aMax = (float) Math.PI;
            sinA = 1.0F;
         } else {
            float ca = (float)Math.min((double)rr / dist, 0.98);
            float al = (float)Math.acos((double)ca);
            aMin = -al;
            aMax = al;
            sinA = (float)Math.sin((double)al);
         }

         float pS = (float)Math.sin((double)aMin);
         float pC = (float)Math.cos((double)aMin);

         for (int s = 1; s <= segs; s++) {
            float na = aMin + (aMax - aMin) * (float)s / (float)segs;
            float nS = (float)Math.sin((double)na);
            float nC = (float)Math.cos((double)na);
            float ax = sh + rr * pS;
            float ad = rr * pC;
            float bx = sh + rr * nS;
            float bd = rr * nC;
            float x0 = ox + rx * ax + tcx * ad;
            float z0 = oz + rz * ax + tcz * ad;
            float x1 = ox + rx * bx + tcx * bd;
            float z1 = oz + rz * bx + tcz * bd;
            float u0 = 0.5F + uHalf * pS / sinA;
            float u1 = 0.5F + uHalf * nS / sinA;
            v(buf, m, x0, y0, z0, u0, v1, r, g, b, a);
            v(buf, m, x1, y0, z1, u1, v1, r, g, b, a);
            v(buf, m, x1, y1, z1, u1, v0, r, g, b, a);
            v(buf, m, x0, y1, z0, u0, v0, r, g, b, a);
            if (bothSides && inside) {
               v(buf, m, x0, y1, z0, u0, v0, r, g, b, a);
               v(buf, m, x1, y1, z1, u1, v0, r, g, b, a);
               v(buf, m, x1, y0, z1, u1, v1, r, g, b, a);
               v(buf, m, x0, y0, z0, u0, v1, r, g, b, a);
            }

            pS = nS;
            pC = nC;
         }
      }
   }

   private static void outline(
      VertexConsumer buf,
      Matrix4f m,
      float ox,
      float oz,
      float cy,
      float rx,
      float rz,
      float tcx,
      float tcz,
      double dist,
      int n,
      boolean ghost,
      float width,
      float r,
      float g,
      float b
   ) {
      int c = 0;

      for (int i = n - 1; i >= 0; i--) {
         float rr = ghost ? RG[i] : RR[i];
         if (!(rr <= 0.02F)) {
            float sh = ghost ? SHG[i] : SH[i];
            float ca = (float)Math.min((double)rr / dist, 0.98);
            float sa = (float)Math.sqrt((double)(1.0F - ca * ca));
            PS[c] = sh - rr * sa;
            PY[c] = Y1[i];
            PD[c] = rr * ca;
            PI[c] = i;
            c++;
            PS[c] = sh - rr * sa;
            PY[c] = Y0[i];
            PD[c] = rr * ca;
            PI[c] = i;
            c++;
         }
      }

      if (c != 0) {
         for (int j = c - 1; j >= 0; j--) {
            int ix = PI[j];
            float rr = ghost ? RG[ix] : RR[ix];
            float sh = ghost ? SHG[ix] : SH[ix];
            float ca = (float)Math.min((double)rr / dist, 0.98);
            float sa = (float)Math.sqrt((double)(1.0F - ca * ca));
            PS[c] = sh + rr * sa;
            PY[c] = PY[j];
            PD[c] = rr * ca;
            c++;
         }

         float hw = width * 0.5F;

         for (int p = 0; p + 1 < c; p++) {
            float s0 = PS[p];
            float y0 = PY[p];
            float s1 = PS[p + 1];
            float y1 = PY[p + 1];
            float ds = s1 - s0;
            float dy = y1 - y0;
            float len = (float)Math.sqrt((double)(ds * ds + dy * dy));
            if (!(len < 1.0E-4F)) {
               ds /= len;
               dy /= len;
               s0 -= ds * hw;
               y0 -= dy * hw;
               s1 += ds * hw;
               y1 += dy * hw;
               float ns = -dy * hw;
               float ny = ds * hw;
               float d0 = PD[p];
               float d1 = PD[p + 1];
               quadLine(buf, m, ox, oz, cy, rx, rz, tcx, tcz, s0 - ns, y0 - ny, d0, s1 - ns, y1 - ny, d1, s1 + ns, y1 + ny, d1, s0 + ns, y0 + ny, d0, r, g, b);
            }
         }
      }
   }

   private static void quadLine(
      VertexConsumer buf,
      Matrix4f m,
      float ox,
      float oz,
      float cy,
      float rx,
      float rz,
      float tcx,
      float tcz,
      float s0,
      float y0,
      float d0,
      float s1,
      float y1,
      float d1,
      float s2,
      float y2,
      float d2,
      float s3,
      float y3,
      float d3,
      float r,
      float g,
      float b
   ) {
      v(buf, m, ox + rx * s0 + tcx * d0, y0 - cy, oz + rz * s0 + tcz * d0, 0.5F, 0.5F, r, g, b);
      v(buf, m, ox + rx * s1 + tcx * d1, y1 - cy, oz + rz * s1 + tcz * d1, 0.5F, 0.5F, r, g, b);
      v(buf, m, ox + rx * s2 + tcx * d2, y2 - cy, oz + rz * s2 + tcz * d2, 0.5F, 0.5F, r, g, b);
      v(buf, m, ox + rx * s3 + tcx * d3, y3 - cy, oz + rz * s3 + tcz * d3, 0.5F, 0.5F, r, g, b);
   }

   private static float rounded(float r, float z, float corner) {
      if (z >= corner) {
         return r;
      } else {
         float q = corner - Math.max(0.0F, z);
         return r - corner + (float)Math.sqrt((double)Math.max(0.0F, corner * corner - q * q));
      }
   }

   private static int findFloor(ClientWorld w, int bottom, int top, double camY) {
      int startY = Math.min(top - 1, (int)Math.floor(camY) + 6);
      int below = Integer.MAX_VALUE;
      Mutable at = new Mutable();

      for (int i = 0; i < 8; i++) {
         double a = (double)i * Math.PI / 4.0;
         int x = (int)Math.floor(AX + Math.cos(a) * 5.0);
         int z = (int)Math.floor(AZ + Math.sin(a) * 5.0);
         if (w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
            boolean open = false;

            for (int y = startY; y > bottom; y--) {
               at.set(x, y, z);
               boolean solid = w.getBlockState(at).isSolidBlock(w, at);
               if (!solid) {
                  open = true;
               } else if (open) {
                  below = Math.min(below, y + 1);
                  break;
               }
            }
         }
      }

      if (below != Integer.MAX_VALUE) {
         return below;
      } else {
         int best = Integer.MAX_VALUE;

         for (int ix = 0; ix < 8; ix++) {
            double a = (double)ix * Math.PI / 4.0;
            int x = (int)Math.floor(AX + Math.cos(a) * 5.0);
            int z = (int)Math.floor(AZ + Math.sin(a) * 5.0);
            int h = w.getTopY(Type.WORLD_SURFACE, x, z);
            if (h > bottom) {
               best = Math.min(best, h);
            }
         }

         return best == Integer.MAX_VALUE ? bottom : best;
      }
   }

   private static void v(VertexConsumer buf, Matrix4f m, float x, float y, float z, float u, float vv, float r, float g, float b, float a) {
      buf.vertex(m, x, y, z).color(r, g, b, a).texture(u, vv).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0.0F, 1.0F, 0.0F);
   }

   private static void v(VertexConsumer buf, Matrix4f m, float x, float y, float z, float u, float vv, float r, float g, float b) {
      v(buf, m, x, y, z, u, vv, Math.min(1.0F, r), Math.min(1.0F, g), Math.min(1.0F, b), 1.0F);
   }

   private static int lerpRgb(int a, int b, float k) {
      int r = (int)mix((float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF), k);
      int g = (int)mix((float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF), k);
      int bl = (int)mix((float)(a & 0xFF), (float)(b & 0xFF), k);
      return r << 16 | g << 8 | bl;
   }

   private static float mix(float a, float b, float k) {
      return a + (b - a) * k;
   }

   private static float frac(float x) {
      return x - (float)Math.floor((double)x);
   }
}
