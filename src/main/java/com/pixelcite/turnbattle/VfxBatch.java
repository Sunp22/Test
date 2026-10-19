package com.pixelcite.turnbattle;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class VfxBatch {
   private final BufferBuilder bb;
   private final Matrix4f mat;
   private final VfxBatch.PixelCanvas canvas;
   private final float minW;
   private float[] screenQuads;
   private int screenQuadCount;
   private static final VfxBatch.PixelCanvas ADD_CANVAS = new VfxBatch.PixelCanvas();
   private static final VfxBatch.PixelCanvas ALPHA_CANVAS = new VfxBatch.PixelCanvas();
   private static final float[] BAYER = new float[]{
      0.0F, 0.5F, 0.125F, 0.625F, 0.75F, 0.25F, 0.875F, 0.375F, 0.1875F, 0.6875F, 0.0625F, 0.5625F, 0.9375F, 0.4375F, 0.8125F, 0.3125F
   };
   private static final int ALPHA_LEVELS = 4;
   private static final int COLOR_LEVELS = 7;
   private static final float FAINTEST = 0.1F;

   private VfxBatch(BufferBuilder bb, Matrix4f mat, VfxBatch.PixelCanvas canvas) {
      this.bb = bb;
      this.mat = mat;
      this.canvas = canvas;
      this.minW = canvas != null ? (float)canvas.cell : 0.0F;
   }

   public static void draw(DrawContext ctx, boolean additive, Consumer<VfxBatch> body) {
      draw(ctx, additive, -1, body);
   }

   public static void draw(DrawContext ctx, boolean additive, int cell, Consumer<VfxBatch> body) {
      ctx.draw();
      int px = Math.max(0, Math.min(6, TurnBattleConfig.get().vfxPixelSize));
      if (cell >= 0 && px > 0) {
         px = cell;
      }

      VfxBatch.PixelCanvas canvas = null;
      if (px > 0) {
         canvas = additive ? ADD_CANVAS : ALPHA_CANVAS;
         canvas.reset(px, additive);
      }

      BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      VfxBatch batch = new VfxBatch(bb, ctx.getMatrices().peek().getPositionMatrix(), canvas);

      try {
         body.accept(batch);
         if (canvas != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            canvas.flush(batch, (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
         }

         for (int i = 0; i < batch.screenQuadCount; i++) {
            float w = batch.screenQuads[i * 3];
            float h = batch.screenQuads[i * 3 + 1];
            int c = Float.floatToRawIntBits(batch.screenQuads[i * 3 + 2]);
            batch.emitQuad(0.0F, 0.0F, w, h, c);
         }
      } finally {
         BuiltBuffer built = bb.endNullable();
      }

      Object var16;
      if (var16 != null) {
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.enableBlend();
         if (additive) {
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         } else {
            RenderSystem.defaultBlendFunc();
         }

         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         BufferRenderer.drawWithGlobalProgram((BuiltBuffer)var16);
         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private void v(float x, float y, int c) {
      this.bb.vertex(this.mat, x, y, 0.0F).color(c);
   }

   private void emitQuad(float x0, float y0, float x1, float y1, int c) {
      this.v(x0, y0, c);
      this.v(x1, y0, c);
      this.v(x1, y1, c);
      this.v(x0, y0, c);
      this.v(x1, y1, c);
      this.v(x0, y1, c);
   }

   public void tri(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
      if ((c1 | c2 | c3) >>> 24 != 0) {
         if (this.canvas != null) {
            this.canvas.add(x1, y1, c1, x2, y2, c2, x3, y3, c3);
         } else {
            this.v(x1, y1, c1);
            this.v(x2, y2, c2);
            this.v(x3, y3, c3);
         }
      }
   }

   public void quad(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3, float x4, float y4, int c4) {
      this.tri(x1, y1, c1, x2, y2, c2, x3, y3, c3);
      this.tri(x1, y1, c1, x3, y3, c3, x4, y4, c4);
   }

   public void rect(float cx, float cy, float w, float h, float rot, int c) {
      w = Math.max(w, this.minW);
      h = Math.max(h, this.minW);
      float cs = (float)Math.cos((double)rot);
      float sn = (float)Math.sin((double)rot);
      float hx = w / 2.0F;
      float hy = h / 2.0F;
      float ax = cs * hx;
      float ay = sn * hx;
      float bx = -sn * hy;
      float by = cs * hy;
      this.quad(cx - ax - bx, cy - ay - by, c, cx + ax - bx, cy + ay - by, c, cx + ax + bx, cy + ay + by, c, cx - ax + bx, cy - ay + by, c);
   }

   public void line(float x0, float y0, float x1, float y1, float w, int c0, int c1) {
      this.taper(x0, y0, x1, y1, w, w, c0, c1);
   }

   public void taper(float x0, float y0, float x1, float y1, float w0, float w1, int c0, int c1) {
      if (w0 < this.minW && w1 < this.minW) {
         w0 = Math.max(w0, this.minW);
         w1 = Math.max(w1, this.minW);
      }

      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len < 0.001F)) {
         float nx = -dy / len;
         float ny = dx / len;
         float a = w0 / 2.0F;
         float b = w1 / 2.0F;
         this.quad(x0 + nx * a, y0 + ny * a, c0, x1 + nx * b, y1 + ny * b, c1, x1 - nx * b, y1 - ny * b, c1, x0 - nx * a, y0 - ny * a, c0);
      }
   }

   public void softTaper(float x0, float y0, float x1, float y1, float w0, float w1, int c0, int c1) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len < 0.001F)) {
         float nx = -dy / len;
         float ny = dx / len;
         float a = w0 / 2.0F;
         float b = w1 / 2.0F;
         int e0 = c0 & 16777215;
         int e1 = c1 & 16777215;
         this.quad(x0 + nx * a, y0 + ny * a, e0, x1 + nx * b, y1 + ny * b, e1, x1, y1, c1, x0, y0, c0);
         this.quad(x0, y0, c0, x1, y1, c1, x1 - nx * b, y1 - ny * b, e1, x0 - nx * a, y0 - ny * a, e0);
      }
   }

   public void soft(float x0, float y0, float x1, float y1, float w, int c0, int c1) {
      w = Math.max(w, this.minW * 2.5F);
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len < 0.001F)) {
         float nx = -dy / len * w / 2.0F;
         float ny = dx / len * w / 2.0F;
         int e0 = c0 & 16777215;
         int e1 = c1 & 16777215;
         this.quad(x0 + nx, y0 + ny, e0, x1 + nx, y1 + ny, e1, x1, y1, c1, x0, y0, c0);
         this.quad(x0, y0, c0, x1, y1, c1, x1 - nx, y1 - ny, e1, x0 - nx, y0 - ny, e0);
      }
   }

   public void beam(float x0, float y0, float x1, float y1, float w, int glow, int core) {
      this.soft(x0, y0, x1, y1, w * 4.0F, glow, glow);
      this.soft(x0, y0, x1, y1, w * 1.6F, core, core);
   }

   public void disc(float cx, float cy, float r, int cIn, int cOut) {
      this.ellipse(cx, cy, r, r, 0.0F, cIn, cOut);
   }

   public void ellipse(float cx, float cy, float rx, float ry, float rot, int cIn, int cOut) {
      if (!(rx < 0.05F) && !(ry < 0.05F)) {
         int seg = segs(Math.max(rx, ry));
         float cs = (float)Math.cos((double)rot);
         float sn = (float)Math.sin((double)rot);
         float px = 0.0F;
         float py = 0.0F;

         for (int i = 0; i <= seg; i++) {
            double a = (Math.PI * 2) * (double)i / (double)seg;
            float ex = (float)Math.cos(a) * rx;
            float ey = (float)Math.sin(a) * ry;
            float x = cx + ex * cs - ey * sn;
            float y = cy + ex * sn + ey * cs;
            if (i > 0) {
               this.tri(cx, cy, cIn, px, py, cOut, x, y, cOut);
            }

            px = x;
            py = y;
         }
      }
   }

   public void glow(float cx, float cy, float r, int c) {
      r = Math.max(r, this.minW * 1.2F);
      this.disc(cx, cy, r, c, c & 16777215);
   }

   public void ring(float cx, float cy, float r, float w, int c) {
      this.ringArc(cx, cy, r, r, w, c, 0.0F, (float) (Math.PI * 2));
   }

   public void ringArc(float cx, float cy, float rx, float ry, float w, int c, float a0, float a1) {
      if (!(rx < 0.3F) && !(w <= 0.01F) && c >>> 24 != 0) {
         int seg = Math.max(4, (int)((double)((float)segs(Math.max(rx, ry)) * Math.abs(a1 - a0)) / (Math.PI * 2)));
         int clear = c & 16777215;
         float hw = w / 2.0F;
         float ky = ry / rx;

         for (int i = 0; i < seg; i++) {
            float t0 = a0 + (a1 - a0) * (float)i / (float)seg;
            float t1 = a0 + (a1 - a0) * (float)(i + 1) / (float)seg;
            float c0 = (float)Math.cos((double)t0);
            float s0 = (float)Math.sin((double)t0);
            float c1 = (float)Math.cos((double)t1);
            float s1 = (float)Math.sin((double)t1);
            float ix0 = cx + c0 * (rx - hw);
            float iy0 = cy + s0 * (rx - hw) * ky;
            float mx0 = cx + c0 * rx;
            float my0 = cy + s0 * ry;
            float ox0 = cx + c0 * (rx + hw);
            float oy0 = cy + s0 * (rx + hw) * ky;
            float ix1 = cx + c1 * (rx - hw);
            float iy1 = cy + s1 * (rx - hw) * ky;
            float mx1 = cx + c1 * rx;
            float my1 = cy + s1 * ry;
            float ox1 = cx + c1 * (rx + hw);
            float oy1 = cy + s1 * (rx + hw) * ky;
            this.quad(ix0, iy0, clear, ix1, iy1, clear, mx1, my1, c, mx0, my0, c);
            this.quad(mx0, my0, c, mx1, my1, c, ox1, oy1, clear, ox0, oy0, clear);
         }
      }
   }

   public void groundRing(float cx, float cy, float rx, float ry, float w, int c, boolean back) {
      if (back) {
         this.ringArc(cx, cy, rx, ry, w, c, (float) Math.PI, (float) (Math.PI * 2));
      } else {
         this.ringArc(cx, cy, rx, ry, w, c, 0.0F, (float) Math.PI);
      }
   }

   public void spark(float cx, float cy, float ang, float len, float w, int c) {
      w = Math.max(w, this.minW * 2.0F);
      len = Math.max(len, this.minW * 2.5F);
      float dx = (float)Math.cos((double)ang) * len / 2.0F;
      float dy = (float)Math.sin((double)ang) * len / 2.0F;
      float nx = -((float)Math.sin((double)ang)) * w / 2.0F;
      float ny = (float)Math.cos((double)ang) * w / 2.0F;
      int clear = c & 16777215;
      this.tri(cx, cy, c, cx + dx, cy + dy, clear, cx + nx, cy + ny, clear);
      this.tri(cx, cy, c, cx + nx, cy + ny, clear, cx - dx, cy - dy, clear);
      this.tri(cx, cy, c, cx - dx, cy - dy, clear, cx - nx, cy - ny, clear);
      this.tri(cx, cy, c, cx - nx, cy - ny, clear, cx + dx, cy + dy, clear);
   }

   public void star(float cx, float cy, float r, int spikes, float rot, int c) {
      if (!(r < 0.1F)) {
         for (int i = 0; i < spikes / 2; i++) {
            this.spark(cx, cy, rot + (float)(Math.PI * (double)i / (double)(spikes / 2)), r * 2.0F, Math.max(0.6F, r * 0.22F), c);
         }

         this.glow(cx, cy, r * 0.55F, c);
      }
   }

   public void flame(float bx, float by, float w, float h, float sway, int cBase, int cTip) {
      if (!(h < 0.5F) && !(w < 0.3F)) {
         int n = 7;
         float pxL = 0.0F;
         float pyL = 0.0F;
         float pxC = 0.0F;
         float pyC = 0.0F;
         float pxR = 0.0F;
         float pyR = 0.0F;
         int pcC = 0;
         int pcE = 0;

         for (int i = 0; i <= n; i++) {
            float s = (float)i / (float)n;
            float half = w / 2.0F * (float)Math.pow(Math.sin(Math.PI * (double)(0.22F + 0.78F * s)), 0.8);
            float x = bx + sway * s * s;
            float y = by - h * s;
            float dx = 2.0F * sway * s;
            float dy = -h;
            float l = (float)Math.sqrt((double)(dx * dx + dy * dy));
            float nx = -dy / l;
            float ny = dx / l;
            int cc = lerp(cBase, cTip, s);
            cc = mulA(cc, 1.0F - (float)Math.pow((double)s, 2.6));
            int ce = mulA(cc, 0.15F);
            float lx = x - nx * half;
            float ly = y - ny * half;
            float rx = x + nx * half;
            float ry = y + ny * half;
            if (i > 0) {
               this.quad(pxL, pyL, pcE, lx, ly, ce, x, y, cc, pxC, pyC, pcC);
               this.quad(pxC, pyC, pcC, x, y, cc, rx, ry, ce, pxR, pyR, pcE);
            }

            pxL = lx;
            pyL = ly;
            pxC = x;
            pyC = y;
            pxR = rx;
            pyR = ry;
            pcC = cc;
            pcE = ce;
         }
      }
   }

   public void shard(float cx, float cy, float len, float w, float ang, int lit, int shade) {
      float dx = (float)Math.cos((double)ang) * len / 2.0F;
      float dy = (float)Math.sin((double)ang) * len / 2.0F;
      float nx = -((float)Math.sin((double)ang)) * w / 2.0F;
      float ny = (float)Math.cos((double)ang) * w / 2.0F;
      int mid = lerp(lit, shade, 0.5F);
      this.tri(cx + dx, cy + dy, lit, cx + nx, cy + ny, lit, cx, cy, mid);
      this.tri(cx + dx, cy + dy, mid, cx, cy, mid, cx - nx, cy - ny, shade);
      this.tri(cx - dx, cy - dy, lit, cx + nx, cy + ny, mid, cx, cy, mid);
      this.tri(cx - dx, cy - dy, shade, cx, cy, mid, cx - nx, cy - ny, shade);
   }

   public void spike(float bx, float by, float len, float w, float ang, int lit, int shade) {
      float dx = (float)Math.cos((double)ang) * len;
      float dy = (float)Math.sin((double)ang) * len;
      float nx = -((float)Math.sin((double)ang)) * w / 2.0F;
      float ny = (float)Math.cos((double)ang) * w / 2.0F;
      float mx = bx + dx * 0.72F;
      float my = by + dy * 0.72F;
      int mid = lerp(lit, shade, 0.5F);
      this.tri(bx + nx, by + ny, lit, mx + nx * 0.9F, my + ny * 0.9F, lit, bx, by, mid);
      this.tri(bx, by, mid, mx + nx * 0.9F, my + ny * 0.9F, lit, mx, my, mid);
      this.tri(bx, by, mid, mx, my, mid, bx - nx, by - ny, shade);
      this.tri(bx - nx, by - ny, shade, mx, my, mid, mx - nx * 0.9F, my - ny * 0.9F, shade);
      this.tri(mx + nx * 0.9F, my + ny * 0.9F, lit, bx + dx, by + dy, lit, mx, my, mid);
      this.tri(mx, my, mid, bx + dx, by + dy, shade, mx - nx * 0.9F, my - ny * 0.9F, shade);
   }

   public void poly(float cx, float cy, float r, int sides, float rot, int cIn, int cOut) {
      for (int i = 0; i < sides; i++) {
         double a0 = (double)rot + (Math.PI * 2) * (double)i / (double)sides;
         double a1 = (double)rot + (Math.PI * 2) * (double)(i + 1) / (double)sides;
         this.tri(
            cx, cy, cIn, cx + (float)Math.cos(a0) * r, cy + (float)Math.sin(a0) * r, cOut, cx + (float)Math.cos(a1) * r, cy + (float)Math.sin(a1) * r, cOut
         );
      }
   }

   public void polyLine(float cx, float cy, float r, int sides, float rot, float w, int c, float ky) {
      for (int i = 0; i < sides; i++) {
         double a0 = (double)rot + (Math.PI * 2) * (double)i / (double)sides;
         double a1 = (double)rot + (Math.PI * 2) * (double)(i + 1) / (double)sides;
         this.line(cx + (float)Math.cos(a0) * r, cy + (float)Math.sin(a0) * r * ky, cx + (float)Math.cos(a1) * r, cy + (float)Math.sin(a1) * r * ky, w, c, c);
      }
   }

   public void starLine(float cx, float cy, float r, int points, int step, float rot, float w, int c, float ky) {
      for (int i = 0; i < points; i++) {
         double a0 = (double)rot + (Math.PI * 2) * (double)i / (double)points;
         double a1 = (double)rot + (Math.PI * 2) * (double)((i + step) % points) / (double)points;
         this.line(cx + (float)Math.cos(a0) * r, cy + (float)Math.sin(a0) * r * ky, cx + (float)Math.cos(a1) * r, cy + (float)Math.sin(a1) * r * ky, w, c, c);
      }
   }

   public void crescent(float cx, float cy, float r, float thick, float midAng, float span, int cEdge, int cMid) {
      int n = 16;
      float pox = 0.0F;
      float poy = 0.0F;
      float pix = 0.0F;
      float piy = 0.0F;
      int pcm = 0;

      for (int i = 0; i <= n; i++) {
         float s = (float)i / (float)n;
         float a = midAng - span / 2.0F + span * s;
         float th = thick * (float)Math.sin(Math.PI * (double)s);
         float cs = (float)Math.cos((double)a);
         float sn = (float)Math.sin((double)a);
         float ox = cx + cs * (r + th / 2.0F);
         float oy = cy + sn * (r + th / 2.0F);
         float ix = cx + cs * (r - th / 2.0F);
         float iy = cy + sn * (r - th / 2.0F);
         int cm = lerp(cEdge, cMid, (float)Math.sin(Math.PI * (double)s));
         if (i > 0) {
            this.quad(pox, poy, pcm, ox, oy, cm, ix, iy, cm, pix, piy, pcm);
         }

         pox = ox;
         poy = oy;
         pix = ix;
         piy = iy;
         pcm = cm;
      }
   }

   public void bolt(float x0, float y0, float x1, float y1, int seed, float jag, float w, int glowC, int coreC, int branches) {
      float[] pts = boltPoints(x0, y0, x1, y1, seed, jag, 5);

      for (int i = 0; i + 3 < pts.length; i += 2) {
         this.beam(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], w, glowC, coreC);
      }

      for (int b = 0; b < branches; b++) {
         int idx = 2 * (2 + (int)(hash01(seed * 31 + b * 7) * (float)(pts.length / 2 - 4)));
         float bx = pts[idx];
         float by = pts[idx + 1];
         float dx = x1 - x0;
         float dy = y1 - y0;
         float len = (float)Math.sqrt((double)(dx * dx + dy * dy)) * (0.18F + 0.2F * hash01(seed + b * 13));
         float ang = (float)Math.atan2((double)dy, (double)dx) + (hash01(seed * 7 + b) < 0.5F ? -1.0F : 1.0F) * (0.5F + 0.5F * hash01(seed + b * 3));
         float ex = bx + (float)Math.cos((double)ang) * len;
         float ey = by + (float)Math.sin((double)ang) * len;
         float[] bp = boltPoints(bx, by, ex, ey, seed * 17 + b, jag * 0.6F, 3);

         for (int i = 0; i + 3 < bp.length; i += 2) {
            float f = 1.0F - (float)i / (float)bp.length;
            this.beam(bp[i], bp[i + 1], bp[i + 2], bp[i + 3], w * 0.55F * f + 0.2F, mulA(glowC, f), mulA(coreC, f));
         }
      }
   }

   public static float[] boltPoints(float x0, float y0, float x1, float y1, int seed, float jag, int depth) {
      int n = 1 << depth;
      float[] p = new float[(n + 1) * 2];
      p[0] = x0;
      p[1] = y0;
      p[n * 2] = x1;
      p[n * 2 + 1] = y1;
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      float nx = len > 0.0F ? -dy / len : 0.0F;
      float ny = len > 0.0F ? dx / len : 0.0F;
      float amp = jag;

      for (int step = n; step > 1; step /= 2) {
         for (int i = 0; i < n; i += step) {
            int m = i + step / 2;
            float mx = (p[i * 2] + p[(i + step) * 2]) / 2.0F;
            float my = (p[i * 2 + 1] + p[(i + step) * 2 + 1]) / 2.0F;
            float off = (hash01(seed * 131 + m * 17 + step) * 2.0F - 1.0F) * amp;
            p[m * 2] = mx + nx * off;
            p[m * 2 + 1] = my + ny * off;
         }

         amp *= 0.55F;
      }

      return p;
   }

   public void heart(float cx, float cy, float s, int cIn, int cOut) {
      this.disc(cx - s * 0.48F, cy - s * 0.18F, s * 0.55F, cIn, cOut);
      this.disc(cx + s * 0.48F, cy - s * 0.18F, s * 0.55F, cIn, cOut);
      this.tri(cx - s * 1.0F, cy - s * 0.05F, cOut, cx + s * 1.0F, cy - s * 0.05F, cOut, cx, cy + s * 1.05F, cOut);
      this.tri(cx - s * 0.7F, cy - s * 0.1F, cIn, cx + s * 0.7F, cy - s * 0.1F, cIn, cx, cy + s * 0.75F, cOut);
   }

   public void chevron(float cx, float cy, float w, float h, boolean up, float th, int c) {
      float tipY = up ? cy - h / 2.0F : cy + h / 2.0F;
      float baseY = up ? cy + h / 2.0F : cy - h / 2.0F;
      this.line(cx - w / 2.0F, baseY, cx, tipY, th, c, c);
      this.line(cx, tipY, cx + w / 2.0F, baseY, th, c, c);
   }

   public void zee(float cx, float cy, float s, float rot, float th, int c) {
      float cs = (float)Math.cos((double)rot);
      float sn = (float)Math.sin((double)rot);
      float[][] p = new float[][]{{-s, -s}, {s, -s}, {-s, s}, {s, s}};
      float[] x = new float[4];
      float[] y = new float[4];

      for (int i = 0; i < 4; i++) {
         x[i] = cx + p[i][0] * cs - p[i][1] * sn;
         y[i] = cy + p[i][0] * sn + p[i][1] * cs;
      }

      this.line(x[0], y[0], x[1], y[1], th, c, c);
      this.line(x[1], y[1], x[2], y[2], th, c, c);
      this.line(x[2], y[2], x[3], y[3], th, c, c);
   }

   public void screen(float w, float h, int c) {
      if (c >>> 24 != 0) {
         if (this.canvas == null) {
            this.quad(0.0F, 0.0F, c, w, 0.0F, c, w, h, c, 0.0F, h, c);
         } else {
            if (this.screenQuads == null) {
               this.screenQuads = new float[12];
            }

            if ((this.screenQuadCount + 1) * 3 > this.screenQuads.length) {
               this.screenQuads = Arrays.copyOf(this.screenQuads, this.screenQuads.length * 2);
            }

            this.screenQuads[this.screenQuadCount * 3] = w;
            this.screenQuads[this.screenQuadCount * 3 + 1] = h;
            this.screenQuads[this.screenQuadCount * 3 + 2] = Float.intBitsToFloat(c);
            this.screenQuadCount++;
         }
      }
   }

   private static int segs(float r) {
      return Math.max(12, Math.min(48, (int)(r * 0.9F)));
   }

   public static int col(int rgb, float a) {
      int al = (int)(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   public static int mulA(int argb, float m) {
      int al = (int)((float)(argb >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, m)));
      return al << 24 | argb & 16777215;
   }

   public static int lerp(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int aa = a >>> 24 & 0xFF;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >>> 24 & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bbb = b & 0xFF;
      return (int)((float)aa + (float)(ba - aa) * t) << 24
         | (int)((float)ar + (float)(br - ar) * t) << 16
         | (int)((float)ag + (float)(bg - ag) * t) << 8
         | (int)((float)ab + (float)(bbb - ab) * t);
   }

   public static int hsv(float h, float s, float v, float a) {
      h -= (float)Math.floor((double)h);
      float c = v * s;
      float x = c * (1.0F - Math.abs(h * 6.0F % 2.0F - 1.0F));
      float m = v - c;
      int sector = (int)(h * 6.0F);
      float r;
      float g;
      float b;
      switch (sector) {
         case 0:
            r = c;
            g = x;
            b = 0.0F;
            break;
         case 1:
            r = x;
            g = c;
            b = 0.0F;
            break;
         case 2:
            r = 0.0F;
            g = c;
            b = x;
            break;
         case 3:
            r = 0.0F;
            g = x;
            b = c;
            break;
         case 4:
            r = x;
            g = 0.0F;
            b = c;
            break;
         default:
            r = c;
            g = 0.0F;
            b = x;
      }

      int ri = (int)((r + m) * 255.0F);
      int gi = (int)((g + m) * 255.0F);
      int bi = (int)((b + m) * 255.0F);
      return col(ri << 16 | gi << 8 | bi, a);
   }

   public static float hash01(int i) {
      int h = i * -1640531535;
      h ^= h >>> 15;
      h *= -2048144777;
      h ^= h >>> 13;
      h *= -1028477387;
      h ^= h >>> 16;
      return (float)(h & 16777215) / 1.6777216E7F;
   }

   static final class PixelCanvas {
      int cell;
      boolean additive;
      float[] rec = new float[4608];
      int recCount;
      float[] r = new float[0];
      float[] g = new float[0];
      float[] b = new float[0];
      float[] a = new float[0];
      int gx0;
      int gy0;
      int gw;
      int gh;

      void reset(int cell, boolean additive) {
         this.cell = cell;
         this.additive = additive;
         this.recCount = 0;
      }

      void add(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
         int need = (this.recCount + 1) * 9;
         if (need > this.rec.length) {
            this.rec = Arrays.copyOf(this.rec, Math.max(need, this.rec.length * 2));
         }

         int o = this.recCount * 9;
         this.rec[o] = x1;
         this.rec[o + 1] = y1;
         this.rec[o + 2] = Float.intBitsToFloat(c1);
         this.rec[o + 3] = x2;
         this.rec[o + 4] = y2;
         this.rec[o + 5] = Float.intBitsToFloat(c2);
         this.rec[o + 6] = x3;
         this.rec[o + 7] = y3;
         this.rec[o + 8] = Float.intBitsToFloat(c3);
         this.recCount++;
      }

      void flush(VfxBatch out, float screenW, float screenH) {
         if (this.recCount != 0) {
            float P = (float)this.cell;
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            for (int t = 0; t < this.recCount; t++) {
               int o = t * 9;

               for (int k = 0; k < 9; k += 3) {
                  float x = this.rec[o + k];
                  float y = this.rec[o + k + 1];
                  if (x < minX) {
                     minX = x;
                  }

                  if (x > maxX) {
                     maxX = x;
                  }

                  if (y < minY) {
                     minY = y;
                  }

                  if (y > maxY) {
                     maxY = y;
                  }
               }
            }

            minX = Math.max(minX, -screenW * 0.5F);
            maxX = Math.min(maxX, screenW * 1.5F);
            minY = Math.max(minY, -screenH * 0.5F);
            maxY = Math.min(maxY, screenH * 1.5F);
            if (!(minX > maxX) && !(minY > maxY)) {
               this.gx0 = (int)Math.floor((double)(minX / P)) - 1;
               this.gy0 = (int)Math.floor((double)(minY / P)) - 1;
               this.gw = (int)Math.floor((double)(maxX / P)) + 2 - this.gx0;
               this.gh = (int)Math.floor((double)(maxY / P)) + 2 - this.gy0;
               int n = this.gw * this.gh;
               if (this.r.length < n) {
                  this.r = new float[n];
                  this.g = new float[n];
                  this.b = new float[n];
                  this.a = new float[n];
               } else {
                  Arrays.fill(this.r, 0, n, 0.0F);
                  Arrays.fill(this.g, 0, n, 0.0F);
                  Arrays.fill(this.b, 0, n, 0.0F);
                  Arrays.fill(this.a, 0, n, 0.0F);
               }

               for (int t = 0; t < this.recCount; t++) {
                  this.rasterize(t * 9, P);
               }

               this.emit(out, P);
            }
         }
      }

      private void rasterize(int o, float P) {
         float x1 = this.rec[o];
         float y1 = this.rec[o + 1];
         float x2 = this.rec[o + 3];
         float y2 = this.rec[o + 4];
         float x3 = this.rec[o + 6];
         float y3 = this.rec[o + 7];
         float area = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
         if (!(Math.abs(area) < 1.0E-6F)) {
            float inv = 1.0F / area;
            int c1 = Float.floatToRawIntBits(this.rec[o + 2]);
            int c2 = Float.floatToRawIntBits(this.rec[o + 5]);
            int c3 = Float.floatToRawIntBits(this.rec[o + 8]);
            float a1 = (float)(c1 >>> 24) / 255.0F;
            float a2 = (float)(c2 >>> 24) / 255.0F;
            float a3 = (float)(c3 >>> 24) / 255.0F;
            float r1 = (float)(c1 >> 16 & 0xFF) / 255.0F;
            float r2 = (float)(c2 >> 16 & 0xFF) / 255.0F;
            float r3 = (float)(c3 >> 16 & 0xFF) / 255.0F;
            float g1 = (float)(c1 >> 8 & 0xFF) / 255.0F;
            float g2 = (float)(c2 >> 8 & 0xFF) / 255.0F;
            float g3 = (float)(c3 >> 8 & 0xFF) / 255.0F;
            float b1 = (float)(c1 & 0xFF) / 255.0F;
            float b2 = (float)(c2 & 0xFF) / 255.0F;
            float b3 = (float)(c3 & 0xFF) / 255.0F;
            int i0 = Math.max(this.gx0, (int)Math.ceil((double)(Math.min(x1, Math.min(x2, x3)) / P - 0.5F)));
            int i1 = Math.min(this.gx0 + this.gw - 1, (int)Math.floor((double)(Math.max(x1, Math.max(x2, x3)) / P - 0.5F)));
            int j0 = Math.max(this.gy0, (int)Math.ceil((double)(Math.min(y1, Math.min(y2, y3)) / P - 0.5F)));
            int j1 = Math.min(this.gy0 + this.gh - 1, (int)Math.floor((double)(Math.max(y1, Math.max(y2, y3)) / P - 0.5F)));

            for (int j = j0; j <= j1; j++) {
               float py = ((float)j + 0.5F) * P;
               int row = (j - this.gy0) * this.gw - this.gx0;
               float sxMin = Float.MAX_VALUE;
               float sxMax = -Float.MAX_VALUE;
               if (py >= y1 != py >= y2) {
                  float x = x1 + (py - y1) * (x2 - x1) / (y2 - y1);
                  sxMin = Math.min(sxMin, x);
                  sxMax = Math.max(sxMax, x);
               }

               if (py >= y2 != py >= y3) {
                  float x = x2 + (py - y2) * (x3 - x2) / (y3 - y2);
                  sxMin = Math.min(sxMin, x);
                  sxMax = Math.max(sxMax, x);
               }

               if (py >= y3 != py >= y1) {
                  float x = x3 + (py - y3) * (x1 - x3) / (y1 - y3);
                  sxMin = Math.min(sxMin, x);
                  sxMax = Math.max(sxMax, x);
               }

               if (!(sxMin > sxMax)) {
                  int ri0 = Math.max(i0, (int)Math.ceil((double)(sxMin / P - 0.5F)) - 1);
                  int ri1 = Math.min(i1, (int)Math.floor((double)(sxMax / P - 0.5F)) + 1);

                  for (int i = ri0; i <= ri1; i++) {
                     float px = ((float)i + 0.5F) * P;
                     float w1 = ((x3 - x2) * (py - y2) - (y3 - y2) * (px - x2)) * inv;
                     if (!(w1 < 0.0F)) {
                        float w2 = ((x1 - x3) * (py - y3) - (y1 - y3) * (px - x3)) * inv;
                        if (!(w2 < 0.0F)) {
                           float w3 = 1.0F - w1 - w2;
                           if (!(w3 < 0.0F)) {
                              float al = w1 * a1 + w2 * a2 + w3 * a3;
                              if (!(al <= 0.0F)) {
                                 float cr = w1 * r1 + w2 * r2 + w3 * r3;
                                 float cg = w1 * g1 + w2 * g2 + w3 * g3;
                                 float cb = w1 * b1 + w2 * b2 + w3 * b3;
                                 int k = row + i;
                                 if (this.additive) {
                                    this.r[k] = this.r[k] + cr * al;
                                    this.g[k] = this.g[k] + cg * al;
                                    this.b[k] = this.b[k] + cb * al;
                                 } else {
                                    float keep = 1.0F - al;
                                    this.r[k] = cr * al + this.r[k] * keep;
                                    this.g[k] = cg * al + this.g[k] * keep;
                                    this.b[k] = cb * al + this.b[k] * keep;
                                    this.a[k] = al + this.a[k] * keep;
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

      private static float post(float c) {
         c = c < 0.0F ? 0.0F : (c > 1.0F ? 1.0F : c);
         return (float)Math.round(c * 7.0F) / 7.0F;
      }

      private static float dither(float v, int i, int j) {
         if (v < 0.1F) {
            return 0.0F;
         } else if (v >= 1.0F) {
            return 1.0F;
         } else {
            float d = VfxBatch.BAYER[(j & 3) * 4 + (i & 3)];
            return (float)Math.floor((double)(v * 4.0F + d)) / 4.0F;
         }
      }

      private int pixelColor(int k, int i, int j) {
         float pr;
         float pg;
         float pb;
         float pa;
         if (this.additive) {
            float m = Math.max(this.r[k], Math.max(this.g[k], this.b[k]));
            if (m <= 0.002F) {
               return 0;
            }

            pa = dither(Math.min(1.0F, m), i, j);
            if (pa <= 0.0F) {
               return 0;
            }

            pr = post(this.r[k] / m);
            pg = post(this.g[k] / m);
            pb = post(this.b[k] / m);
         } else {
            if (this.a[k] <= 0.002F) {
               return 0;
            }

            pa = dither(this.a[k], i, j);
            if (pa <= 0.0F) {
               return 0;
            }

            pr = post(this.r[k] / this.a[k]);
            pg = post(this.g[k] / this.a[k]);
            pb = post(this.b[k] / this.a[k]);
         }

         return (int)(pa * 255.0F) << 24 | (int)(pr * 255.0F) << 16 | (int)(pg * 255.0F) << 8 | (int)(pb * 255.0F);
      }

      private void emit(VfxBatch out, float P) {
         for (int jj = 0; jj < this.gh; jj++) {
            int j = this.gy0 + jj;
            int runStart = -1;
            int runColor = 0;

            for (int ii = 0; ii <= this.gw; ii++) {
               int c = 0;
               if (ii < this.gw) {
                  c = this.pixelColor(jj * this.gw + ii, this.gx0 + ii, j);
               }

               if (runStart >= 0 && c != runColor) {
                  out.emitQuad((float)(this.gx0 + runStart) * P, (float)j * P, (float)(this.gx0 + ii) * P, (float)(j + 1) * P, runColor);
                  runStart = -1;
               }

               if (c != 0 && runStart < 0) {
                  runStart = ii;
                  runColor = c;
               }
            }
         }
      }
   }
}
