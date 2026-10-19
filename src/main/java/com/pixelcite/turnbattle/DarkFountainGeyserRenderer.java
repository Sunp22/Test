package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
final class DarkFountainGeyserRenderer {
   private static final float W = 28.0F;
   private static final float H = 600.0F;
   private static final float BASE_FADE = 16.0F;
   private static final float TOP = 120.0F;
   private static final float FADE_IN = 0.6F;
   private static final float SURGE_GAP = 84.0F;
   private static final float SURGE_SPEED = 53.2F;
   private static final float SURGE_THICK = 12.88F;
   private static final float SURGE_ARCH = 11.759999F;
   private static final int SPIKES = 30;
   private static final float[] BLUE = new float[]{0.25490198F, 0.22745098F, 0.74509805F};
   private static final float[] NAVY = new float[]{0.16862746F, 0.14117648F, 0.3019608F};
   private static final Map<String, Float> shown = new HashMap<>();
   private static long lastNanos = System.nanoTime();
   private static float[] verts = new float[28672];
   private static int count;
   private static double gx;
   private static double gz;
   private static double gBase;
   private static double gk;
   private static double gRx;
   private static double gRz;
   private static Vec3d gCam;
   private static float gAlpha;

   private DarkFountainGeyserRenderer() {
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
         float td = mc.getRenderTickCounter().getTickDelta(false);
         float t = ((float)(w.getTime() % 1200000L) + td) / 20.0F;
         count = 0;
         if (!dim.startsWith("turnbattle:dark_world_")) {
            shown.clear();
         } else {
            int[] at = DarkFountainSites.fountainOf(dim);
            if (at != null) {
               boolean away = !DarkFountainPillarRenderer.present && !w.getChunkManager().isChunkLoaded(at[0] >> 4, at[1] >> 4);
               float a = fade(dim + "@" + at[0] + "," + at[1], away, dt);
               if (a > 0.004F) {
                  geyser(mc, cam, (double)at[0] + 0.5, DarkFountainPillarRenderer.floorY(w), (double)at[1] + 0.5, t, a);
               }
            }

            if (shown.size() > 128) {
               shown.clear();
            }

            if (count != 0) {
               Matrix4f m = new MatrixStack().peek().getPositionMatrix();
               RenderSystem.setShader(GameRenderer::getPositionColorProgram);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableCull();
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(515);
               RenderSystem.depthMask(false);

               try {
                  BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                  for (int i = 0; i < count; i++) {
                     int o = i * 7;
                     bb.vertex(m, verts[o], verts[o + 1], verts[o + 2]).color(verts[o + 3], verts[o + 4], verts[o + 5], verts[o + 6]);
                  }

                  BuiltBuffer built = bb.endNullable();
                  if (built != null) {
                     BufferRenderer.drawWithGlobalProgram(built);
                  }
               } finally {
                  RenderSystem.depthMask(true);
                  RenderSystem.enableCull();
                  RenderSystem.disableBlend();
               }
            }
         }
      }
   }

   private static float fade(String key, boolean away, float dt) {
      float v = shown.getOrDefault(key, 0.0F);
      v = away ? Math.min(1.0F, v + dt / 0.6F) : 0.0F;
      shown.put(key, v);
      return v * v * (3.0F - 2.0F * v);
   }

   private static void geyser(MinecraftClient mc, Vec3d cam, double sx, double baseY, double sz, float t, float a) {
      double dx = sx - cam.x;
      double dz = sz - cam.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (!(dist < 1.0)) {
         double far = (double)mc.gameRenderer.getFarPlaneDistance() * 0.7;
         gk = dist > far ? far / dist : 1.0;
         gx = dx;
         gz = dz;
         gBase = baseY;
         gCam = cam;
         gRx = -dz / dist;
         gRz = dx / dist;
         gAlpha = a;
         float px = (float)Math.max(0.12, dist * 0.0012);
         float rim = Math.max(0.5F, 2.2F * px);
         float streakW = Math.max(0.3F, 1.6F * px);
         float dh = 3.0F;

         for (float h0 = 0.0F; h0 < 600.0F; h0 += dh) {
            float h1 = Math.min(600.0F, h0 + dh);
            float hw0 = halfWidth(h0, t);
            float hw1 = halfWidth(h1, t);
            float c0 = center(h0, t);
            float c1 = center(h1, t);
            float a0 = bodyAlpha(h0);
            float a1 = bodyAlpha(h1);
            quad(c0 - hw0, h0, c0 + hw0, h0, c1 + hw1, h1, c1 - hw1, h1, null, a0, a1);
            quad(c0 - hw0 - rim, h0, c0 - hw0, h0, c1 - hw1, h1, c1 - hw1 - rim, h1, BLUE, a0, a1);
            quad(c0 + hw0, h0, c0 + hw0 + rim, h0, c1 + hw1 + rim, h1, c1 + hw1, h1, BLUE, a0, a1);
         }

         for (int i = 0; i < 9; i++) {
            float lane = (rnd(i, 1) - 0.5F) * 1.5F;
            float len = 28.0F * (0.9F + 1.6F * rnd(i, 2));
            float speed = 53.2F * (1.1F + 0.6F * rnd(i, 3));
            float cycle = 300.0F;

            for (int c = 0; c < 2; c++) {
               float y0 = (t * speed + rnd(i, 4) * cycle) % cycle + (float)c * cycle;
               float y1 = Math.min(540.0F, y0 + len);
               if (!(y1 <= y0)) {
                  float cx = center(y0, t) + lane * halfWidth(y0, t) * 0.5F;
                  float al = Math.min(bodyAlpha(y0), bodyAlpha(y1)) * 0.9F;
                  quad(cx - streakW / 2.0F, y0, cx + streakW / 2.0F, y0, cx + streakW / 2.0F, y1, cx - streakW / 2.0F, y1, NAVY, al, al * 0.4F);
               }
            }
         }

         float first = t * 53.2F % 84.0F;

         for (float hc = first - 84.0F; hc < 600.0F; hc += 84.0F) {
            if (!(hc < -12.88F)) {
               int id = (int)Math.floor((double)((t * 53.2F - hc) / 84.0F + 0.5F));
               surge(hc, t, id);
            }
         }

         for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 9; i++) {
               float age = frac(t * 0.33F + (float)i / 9.0F);
               float y = 546.0F + age * 120.0F * 1.6F;
               float r = 28.0F * (0.45F + 1.25F * age) * (0.8F + 0.4F * rnd(i + (int)Math.floor((double)(t * 0.33F + (float)i / 9.0F)) * 9, 5));
               float cx = center(600.0F, t) + (rnd(i, 6) - 0.5F) * 28.0F * 1.4F * age;
               float al = (float)Math.pow((double)(1.0F - age), 1.4) * Math.min(1.0F, age * 6.0F);
               blob(cx, y, r, al, rim, t + (float)i, pass == 0);
            }
         }
      }
   }

   private static float halfWidth(float h, float t) {
      float w = 14.0F * (1.0F + 0.07F * (float)Math.sin((double)(h * 0.045F - t * 2.1F)) + 0.04F * (float)Math.sin((double)(h * 0.12F + t * 1.4F)));
      float rel = ((h - t * 53.2F) % 84.0F + 84.0F) % 84.0F;
      float d = Math.min(rel, 84.0F - rel) / 22.4F;
      w *= 1.0F + 0.16F * (float)Math.exp((double)(-d * d));
      if (h > 480.0F) {
         float k = (h - 480.0F) / 120.0F;
         w *= 1.0F + 0.9F * k * k;
      }

      return w;
   }

   private static float center(float h, float t) {
      return 1.68F * (float)Math.sin((double)(h * 0.022F + t * 0.8F)) + 0.84F * (float)Math.sin((double)(h * 0.061F - t * 1.7F));
   }

   private static float bodyAlpha(float h) {
      float bottom = Math.min(1.0F, Math.max(0.0F, h / 16.0F));
      float top = h > 480.0F ? 1.0F - (h - 480.0F) / 120.0F : 1.0F;
      return bottom * Math.max(0.0F, top);
   }

   private static void surge(float hc, float t, int id) {
      float hw = halfWidth(hc, t) * 0.97F;
      float cx = center(hc, t);
      float al = bodyAlpha(hc);
      if (!(al <= 0.01F)) {
         float step = 2.0F * hw / 30.0F;
         float flick = (float)Math.floor((double)(t * 10.0F));

         for (int j = 0; j < 30; j++) {
            float u = ((float)j + 0.5F) / 30.0F * 2.0F - 1.0F;
            float s = cx + u * hw;
            float arch = 11.759999F * (1.0F - u * u);
            float r1 = rnd(j + id * 97, (int)flick);
            float r2 = rnd(j + id * 97 + 31, (int)flick + 7);
            float lo = hc + arch - 12.88F * (0.35F + 0.65F * r1);
            float hi = hc + arch + 12.88F * (0.25F + 0.9F * r2);
            float ww = step * (0.7F + 0.6F * rnd(j, id + 3));
            float edge = 1.0F - 0.45F * u * u;
            quad(
               s - ww / 2.0F,
               lo,
               s + ww / 2.0F,
               lo,
               s + ww * 0.15F,
               hi * edge + (1.0F - edge) * (hc + arch),
               s - ww * 0.15F,
               hi * edge + (1.0F - edge) * (hc + arch),
               BLUE,
               al,
               al
            );
         }

         float core = 6.44F;

         for (int j = 0; j < 12; j++) {
            float u0 = (float)j / 12.0F * 2.0F - 1.0F;
            float u1 = (float)(j + 1) / 12.0F * 2.0F - 1.0F;
            float y0 = hc + 11.759999F * (1.0F - u0 * u0);
            float y1 = hc + 11.759999F * (1.0F - u1 * u1);
            quad(cx + u0 * hw, y0 - core, cx + u1 * hw, y1 - core, cx + u1 * hw, y1 + core * 0.6F, cx + u0 * hw, y0 + core * 0.6F, BLUE, al, al);
         }
      }
   }

   private static void blob(float cx, float cy, float r, float al, float rim, float seed, boolean edge) {
      if (!(al <= 0.01F)) {
         int n = 14;

         for (int i = 0; i < n; i++) {
            float a0 = (float)((double)i * Math.PI * 2.0 / (double)n);
            float a1 = (float)((double)(i + 1) * Math.PI * 2.0 / (double)n);
            float r0 = r * lump(a0, seed);
            float r1 = r * lump(a1, seed);
            float x0 = cx + (float)Math.cos((double)a0) * r0;
            float y0 = cy + (float)Math.sin((double)a0) * r0;
            float x1 = cx + (float)Math.cos((double)a1) * r1;
            float y1 = cy + (float)Math.sin((double)a1) * r1;
            float ex0 = cx + (float)Math.cos((double)a0) * (r0 + rim);
            float ey0 = cy + (float)Math.sin((double)a0) * (r0 + rim);
            float ex1 = cx + (float)Math.cos((double)a1) * (r1 + rim);
            float ey1 = cy + (float)Math.sin((double)a1) * (r1 + rim);
            if (edge) {
               quad(x0, y0, x1, y1, ex1, ey1, ex0, ey0, BLUE, al, al);
            } else {
               quad(cx, cy, x0, y0, x1, y1, cx, cy, null, al, al);
            }
         }
      }
   }

   private static float lump(float ang, float seed) {
      return 1.0F + 0.16F * (float)Math.sin((double)(ang * 3.0F + seed * 1.7F)) + 0.1F * (float)Math.sin((double)(ang * 5.0F - seed * 2.3F));
   }

   private static void quad(float s0, float h0, float s1, float h1, float s2, float h2, float s3, float h3, float[] rgb, float aLow, float aHigh) {
      float r = rgb == null ? 0.0F : rgb[0];
      float g = rgb == null ? 0.0F : rgb[1];
      float b = rgb == null ? 0.0F : rgb[2];
      put(s0, h0, r, g, b, aLow * gAlpha);
      put(s1, h1, r, g, b, aLow * gAlpha);
      put(s2, h2, r, g, b, aHigh * gAlpha);
      put(s3, h3, r, g, b, aHigh * gAlpha);
   }

   private static void put(float s, float h, float r, float g, float b, float a) {
      int o = count * 7;
      if (o + 7 > verts.length) {
         verts = Arrays.copyOf(verts, verts.length * 2);
      }

      verts[o] = (float)((gx + gRx * (double)s) * gk);
      verts[o + 1] = (float)((gBase + (double)h - gCam.y) * gk);
      verts[o + 2] = (float)((gz + gRz * (double)s) * gk);
      verts[o + 3] = r;
      verts[o + 4] = g;
      verts[o + 5] = b;
      verts[o + 6] = Math.max(0.0F, Math.min(1.0F, a));
      count++;
   }

   private static float rnd(int i, int salt) {
      long h = (long)i * -7046029254386353131L ^ (long)salt * -4417276706812531889L;
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   private static float frac(float x) {
      return x - (float)Math.floor((double)x);
   }
}
