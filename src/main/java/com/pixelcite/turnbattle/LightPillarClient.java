package com.pixelcite.turnbattle;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap.Type;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class LightPillarClient {
   private static final float HEIGHT = 44.0F;
   private static final float[] RINGS = new float[]{2.6F, 2.04F, 1.48F};
   private static final float[] POOL = new float[]{3.24F, 2.72F, 2.1F};
   private static final int SEG = 28;
   private static final int SPARKLES = 32;
   private static final double DRAW_RANGE = 160.0;
   private static final Map<Long, int[]> floors = new HashMap<>();
   private static ClientWorld floorsWorld;
   private static boolean wasIn;
   private static float[] verts = new float[57344];
   private static int count;
   private static long exitStartNs = -1L;
   private static long arrivedNs = -1L;
   private static final float HOLD_WHITE = 0.5F;
   private static final float FADE_OUT = 1.3F;

   private LightPillarClient() {
   }

   private static String dim() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.world == null ? null : mc.world.getRegistryKey().getValue().toString();
   }

   private static List<int[]> pillars() {
      String d = dim();
      if (d != null && d.startsWith("turnbattle:dark_world_")) {
         int[] f = DarkFountainSites.fountainOf(d);
         return f == null ? List.of() : LightPillars.positions(d, f[0], f[1]);
      } else {
         return List.of();
      }
   }

   private static int floorOf(ClientWorld w, int x, int z, double camY) {
      if (w != floorsWorld) {
         floors.clear();
         floorsWorld = w;
      }

      long key = BlockPos.asLong(x, 0, z);
      int[] c = floors.get(key);
      int now = (int)(w.getTime() & 2147483647L);
      if (c != null && Math.abs((double)c[1] - camY) < 6.0 && now >= c[2] && now - c[2] < 40) {
         return c[0];
      } else {
         int bottom = w.getBottomY();
         int top = w.getTopY();
         int y = Integer.MIN_VALUE;
         Mutable at = new Mutable();
         boolean open = false;

         for (int yy = Math.min(top - 1, (int)Math.floor(camY) + 40); yy > Math.max(bottom, (int)camY - 120); yy--) {
            at.set(x, yy, z);
            VoxelShape shape = w.getBlockState(at).getCollisionShape(w, at);
            if (shape.isEmpty()) {
               open = true;
            } else if (open) {
               y = (int)Math.round((double)yy + shape.getMax(Axis.Y));
               break;
            }
         }

         if (y == Integer.MIN_VALUE) {
            y = w.getTopY(Type.WORLD_SURFACE, x, z);
         }

         floors.put(key, new int[]{y, (int)camY, now});
         if (floors.size() > 256) {
            floors.clear();
         }

         return y;
      }
   }

   static int[] standingIn() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         for (int[] p : pillars()) {
            double dx = (double)p[0] + 0.5 - mc.player.getX();
            double dz = (double)p[1] + 0.5 - mc.player.getZ();
            if (!(dx * dx + dz * dz > 8.41) && mc.world.getChunkManager().isChunkLoaded(p[0] >> 4, p[1] >> 4)) {
               int gy = floorOf(mc.world, p[0], p[1], mc.player.getY());
               if (!(Math.abs(mc.player.getY() - (double)gy) > 3.0)) {
                  return new int[]{p[0], p[1], gy};
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static boolean tryUse(MinecraftClient mc) {
      if (exitStartNs >= 0L || PlayerActions.isActing(mc.player != null ? mc.player.getUuid() : null)) {
         return false;
      } else if (standingIn() == null) {
         return false;
      } else {
         Net121.sendToServer(DarkWorldExit.USE, PacketByteBufs.empty());
         return true;
      }
   }

   static void tick(MinecraftClient mc, KeyBinding inspectKey) {
      boolean in = mc.player != null && exitStartNs < 0L && standingIn() != null;
      if (in && !wasIn && mc.player != null) {
         mc.player
            .sendMessage(Text.literal("* Press [").append(inspectKey.getBoundKeyLocalizedText()).append(Text.literal("] to return to the Light World.")), true);
      }

      wasIn = in;
      if (mc.world == null) {
         floors.clear();
         exitStartNs = -1L;
         arrivedNs = -1L;
      }
   }

   static void render(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientWorld w = mc.world;
      if (w != null && mc.player != null) {
         List list = pillars();
         if (!list.isEmpty()) {
            Vec3d cam = ctx.camera().getPos();
            float td = mc.getRenderTickCounter().getTickDelta(false);
            float t = ((float)(w.getTime() % 1200000L) + td) / 20.0F;
            count = 0;

            for (int i = 0; i < list.size(); i++) {
               int[] p = list.get(i);
               double cx = (double)p[0] + 0.5;
               double cz = (double)p[1] + 0.5;
               double dx = cx - cam.x;
               double dz = cz - cam.z;
               if (!(dx * dx + dz * dz > 25600.0) && w.getChunkManager().isChunkLoaded(p[0] >> 4, p[1] >> 4)) {
                  int gy = floorOf(w, p[0], p[1], cam.y);
                  pillar((float)dx, (float)((double)gy - cam.y), (float)dz, t, i);
               }
            }

            if (count != 0) {
               Matrix4f m = new MatrixStack().peek().getPositionMatrix();
               RenderSystem.setShader(GameRenderer::getPositionColorProgram);
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
               RenderSystem.disableCull();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(false);

               try {
                  BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                  for (int ix = 0; ix < count; ix++) {
                     int o = ix * 7;
                     bb.vertex(m, verts[o], verts[o + 1], verts[o + 2]).color(verts[o + 3], verts[o + 4], verts[o + 5], verts[o + 6]);
                  }

                  BuiltBuffer built = bb.endNullable();
                  if (built != null) {
                     BufferRenderer.drawWithGlobalProgram(built);
                  }
               } finally {
                  RenderSystem.depthMask(true);
                  RenderSystem.enableCull();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.disableBlend();
               }
            }
         }
      }
   }

   private static void pillar(float x, float y, float z, float t, int seed) {
      float pulse = 0.92F + 0.08F * (float)Math.sin((double)(t * 1.7F + (float)seed));

      for (int r = 0; r < RINGS.length; r++) {
         float rad = RINGS[r] * (0.98F + 0.02F * (float)Math.sin((double)(t * 2.3F + (float)r + (float)seed)));
         float a = (r == 0 ? 0.1F : (r == 1 ? 0.12F : 0.15F)) * pulse;
         int bands = 6;

         for (int b = 0; b < bands; b++) {
            float h0 = 44.0F * (float)b / (float)bands;
            float h1 = 44.0F * (float)(b + 1) / (float)bands;
            float a0 = a * fadeUp(h0 / 44.0F);
            float a1 = a * fadeUp(h1 / 44.0F);

            for (int s = 0; s < 28; s++) {
               double t0 = (double)s * Math.PI * 2.0 / 28.0;
               double t1 = (double)(s + 1) * Math.PI * 2.0 / 28.0;
               float x0 = x + (float)Math.cos(t0) * rad;
               float z0 = z + (float)Math.sin(t0) * rad;
               float x1 = x + (float)Math.cos(t1) * rad;
               float z1 = z + (float)Math.sin(t1) * rad;
               put(x0, y + h0, z0, a0);
               put(x1, y + h0, z1, a0);
               put(x1, y + h1, z1, a1);
               put(x0, y + h1, z0, a1);
            }
         }
      }

      for (int r = 0; r < POOL.length; r++) {
         float rad = POOL[r];
         float a = (r == 0 ? 0.16F : (r == 1 ? 0.2F : 0.3F)) * pulse;
         float yy = y + 0.02F + (float)r * 0.005F;

         for (int s = 0; s < 28; s++) {
            double t0 = (double)s * Math.PI * 2.0 / 28.0;
            double t1 = (double)(s + 1) * Math.PI * 2.0 / 28.0;
            put(x, yy, z, a);
            put(x, yy, z, a);
            put(x + (float)Math.cos(t0) * rad, yy, z + (float)Math.sin(t0) * rad, a);
            put(x + (float)Math.cos(t1) * rad, yy, z + (float)Math.sin(t1) * rad, a);
         }
      }

      float len = (float)Math.sqrt((double)(x * x + z * z));
      float rx = len > 0.001F ? -z / len : 1.0F;
      float rz = len > 0.001F ? x / len : 0.0F;

      for (int i = 0; i < 32; i++) {
         float h1 = rnd(seed * 31 + i, 1);
         float h2 = rnd(seed * 31 + i, 2);
         float h3 = rnd(seed * 31 + i, 3);
         float speed = 1.8F + 2.6F * h2;
         float span = 26.400002F;
         float up = (t * speed + h3 * span) % span;
         float ang = h1 * 6.283F + t * 0.4F * (h2 - 0.5F);
         float rr = RINGS[0] * (0.15F + 0.75F * rnd(seed * 31 + i, 4));
         float sx = x + (float)Math.cos((double)ang) * rr;
         float sz = z + (float)Math.sin((double)ang) * rr;
         float sy = y + 0.2F + up;
         float tw = 0.55F + 0.45F * (float)Math.sin((double)(t * (7.0F + 5.0F * h1) + (float)i));
         float a = 0.95F * tw * (1.0F - up / span) * Math.min(1.0F, up / 0.6F);
         if (!(a <= 0.02F)) {
            float hs = 0.07F + 0.06F * rnd(seed * 31 + i, 5);
            put(sx - rx * hs, sy - hs, sz - rz * hs, a);
            put(sx + rx * hs, sy - hs, sz + rz * hs, a);
            put(sx + rx * hs, sy + hs, sz + rz * hs, a);
            put(sx - rx * hs, sy + hs, sz - rz * hs, a);
         }
      }
   }

   private static float fadeUp(float k) {
      if (k < 0.25F) {
         return 1.0F;
      } else {
         float f = 1.0F - (k - 0.25F) / 0.75F;
         return Math.max(0.0F, f * f);
      }
   }

   private static void put(float x, float y, float z, float a) {
      if ((count + 1) * 7 > verts.length) {
         if (verts.length >= 1835008) {
            return;
         }

         verts = Arrays.copyOf(verts, verts.length * 2);
      }

      int o = count * 7;
      verts[o] = x;
      verts[o + 1] = y;
      verts[o + 2] = z;
      verts[o + 3] = 0.94F;
      verts[o + 4] = 0.95F;
      verts[o + 5] = 1.0F;
      verts[o + 6] = a;
      count++;
   }

   private static float rnd(int i, int salt) {
      long h = (long)i * -7046029254386353131L ^ (long)salt * -4417276706812531889L;
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   static void beginExit() {
      exitStartNs = PlayerActions.now();
      arrivedNs = -1L;
   }

   public static float whiteness() {
      if (exitStartNs < 0L) {
         return 0.0F;
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         String d = dim();
         boolean stillDark = d != null && (d.startsWith("turnbattle:dark_world_") || d.equals("turnbattle:depths"));
         float ticks = (float)(PlayerActions.now() - exitStartNs) / 5.0E7F;
         if (stillDark && arrivedNs < 0L) {
            if (ticks > 288.0F) {
               exitStartNs = -1L;
               return 0.0F;
            } else {
               float k = (ticks - 46.0F) / 32.0F;
               k = Math.max(0.0F, Math.min(1.0F, k));
               return k * k * (3.0F - 2.0F * k);
            }
         } else {
            boolean ready = mc.world != null
               && mc.player != null
               && !(mc.currentScreen instanceof DownloadingTerrainScreen)
               && mc.world.getChunkManager().isChunkLoaded(mc.player.getBlockX() >> 4, mc.player.getBlockZ() >> 4);
            if (arrivedNs < 0L) {
               if (!ready) {
                  return 1.0F;
               }

               arrivedNs = System.nanoTime();
            }

            float s = (float)(System.nanoTime() - arrivedNs) / 1.0E9F;
            if (s < 0.5F) {
               return 1.0F;
            } else {
               float k = Math.min(1.0F, (s - 0.5F) / 1.3F);
               if (k >= 1.0F) {
                  exitStartNs = -1L;
                  arrivedNs = -1L;
                  return 0.0F;
               } else {
                  return 1.0F - k * k * (3.0F - 2.0F * k);
               }
            }
         }
      }
   }

   static void renderHud(DrawContext ctx) {
      float w = whiteness();
      if (!(w <= 0.003F)) {
         ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), Math.round(255.0F * w) << 24 | 16777215);
      }
   }

   public static boolean renderLoading(DrawContext ctx) {
      if (exitStartNs < 0L) {
         return false;
      } else {
         ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), -1);
         return true;
      }
   }
}
