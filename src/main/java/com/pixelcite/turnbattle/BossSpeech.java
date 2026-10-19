package com.pixelcite.turnbattle;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class BossSpeech {
   private static String text = "";
   private static float shown;
   private static boolean active;
   private static Runnable then;
   private static long lastNanos;
   private static long doneAtMs;
   private static final long AUTO_CONTINUE_MS = 9000L;
   private static final int MAX_W = 130;
   private static final int PAD = 6;

   private BossSpeech() {
   }

   public static boolean active() {
      return active;
   }

   public static void show(String line, Runnable after) {
      text = line == null ? "" : line;
      shown = 0.0F;
      active = true;
      then = after;
      lastNanos = 0L;
      doneAtMs = 0L;
   }

   public static void press() {
      if (active) {
         if (shown < (float)text.length()) {
            shown = (float)text.length();
         } else {
            finish();
         }
      }
   }

   public static void clear() {
      active = false;
      then = null;
   }

   private static void finish() {
      active = false;
      Runnable r = then;
      then = null;
      if (r != null) {
         r.run();
      }
   }

   public static void render(DrawContext ctx, TextRenderer tr, int ax, int ay, int screenW, int screenH) {
      if (active) {
         long now = System.nanoTime();
         float dt = lastNanos == 0L ? 0.0F : Math.min(5.0F, (float)(now - lastNanos) / 5.0E7F);
         lastNanos = now;
         if (shown < (float)text.length()) {
            int before = (int)shown;
            shown = Math.min((float)text.length(), shown + 1.5F * dt);

            for (int i = before; i < (int)shown; i++) {
               if (i % 2 == 0 && !Character.isWhitespace(text.charAt(i))) {
                  MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SND_TEXT, 1.0F));
                  break;
               }
            }
         } else if (doneAtMs == 0L) {
            doneAtMs = System.currentTimeMillis();
         } else if (System.currentTimeMillis() - doneAtMs > 9000L) {
            finish();
            return;
         }

         List full = tr.wrapLines(Text.literal(text), 130);
         int tw = 0;

         for (OrderedText l : full) {
            tw = Math.max(tw, tr.getWidth(l));
         }

         int w = tw + 12;
         int h = full.size() * 10 + 12 - 1;
         if (ax <= 0 || ay <= 0) {
            ax = (int)((float)screenW * 0.75F);
            ay = (int)((float)screenH * 0.3F);
         }

         int x1 = ax - 26;
         int x0 = x1 - w;
         int y0 = ay - h / 2;
         if (x0 < 4) {
            x0 = 4;
            x1 = x0 + w;
         }

         y0 = Math.max(4, Math.min(screenH - h - 4, y0));
         int y1 = y0 + h;
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(0.0F, 0.0F, 500.0F);
         int white = -1;
         ctx.fill(x0 + 3, y0, x1 - 3, y1, white);
         ctx.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, white);
         ctx.fill(x0, y0 + 3, x1, y1 - 3, white);
         int ty = Math.max(y0 + 6, Math.min(y1 - 6, ay));

         for (int ix = 0; ix < 6; ix++) {
            int half = Math.max(0, 4 - ix * 4 / 6);
            ctx.fill(x1 + ix * 2 - 1, ty - half, x1 + ix * 2 + 1, ty + half + 1, white);
         }

         String vis = text.substring(0, Math.min(text.length(), (int)shown));
         List lines = tr.wrapLines(Text.literal(vis), 130);

         for (int ix = 0; ix < lines.size(); ix++) {
            ctx.drawText(tr, lines.get(ix), x0 + 6, y0 + 6 + ix * 10, -16777216, false);
         }

         m.pop();
      }
   }
}
