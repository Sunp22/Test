package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public class ProximityChatRenderer {
   private static final int CHARS_PER_TICK = 2;
   private static final int PUNCT_PAUSE_TICKS = 8;
   private static final int PAGE_WAIT_TICKS = 60;
   private static final int CLOSE_WAIT_TICKS = 80;
   private static final Map<UUID, ProximityChatRenderer.ActiveBox> activeBoxes = new ConcurrentHashMap<>();
   private static final double VOL_FULL_RANGE = 10.0;
   private static final double VOL_ZERO_RANGE = 50.0;
   private static final int MAX_TEXT_W = 190;
   private static final int PAD = 6;
   private static final int LINE_H = 10;
   private static final double SHOW_RANGE = 32.0;
   private static volatile List<ProximityChatRenderer.Pending> pending = List.of();

   public static void show(UUID speakerId, String[] pages) {
      activeBoxes.put(speakerId, new ProximityChatRenderer.ActiveBox(pages));
   }

   private static int countVisible(String s) {
      int count = 0;

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == 167 && i + 1 < s.length()) {
            i++;
         } else if (c != '\n') {
            count++;
         }
      }

      return count;
   }

   private static String visibleSubstring(String s, int visibleCount) {
      StringBuilder out = new StringBuilder();
      int seen = 0;

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == 167 && i + 1 < s.length()) {
            out.append(c).append(s.charAt(i + 1));
            i++;
         } else if (c == '\n') {
            out.append(c);
         } else {
            if (seen >= visibleCount) {
               break;
            }

            out.append(c);
            seen++;
         }
      }

      return out.toString();
   }

   private static char visibleCharAt(String s, int visibleIndex) {
      int seen = 0;

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == 167 && i + 1 < s.length()) {
            i++;
         } else if (c != '\n') {
            if (seen == visibleIndex) {
               return c;
            }

            seen++;
         }
      }

      return '\u0000';
   }

   public static void tick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      List toRemove = new ArrayList<>();

      for (Entry entry : activeBoxes.entrySet()) {
         UUID speakerId = entry.getKey();
         ProximityChatRenderer.ActiveBox box = entry.getValue();
         box.age++;
         if (box.visibleIdx < box.visibleLen) {
            if (box.pauseTicks > 0) {
               box.pauseTicks--;
            } else {
               int advanced = 0;
               boolean playedThisTick = false;

               while (true) {
                  if (advanced < 2 && box.visibleIdx < box.visibleLen) {
                     char revealed = visibleCharAt(box.pages[box.pageIdx], box.visibleIdx);
                     box.visibleIdx++;
                     advanced++;
                     if (!playedThisTick && mc.player != null) {
                        playPositionedTextSound(mc, speakerId);
                        playedThisTick = true;
                     }

                     if (revealed != '.' && revealed != ',' && revealed != '?' && revealed != '!') {
                        continue;
                     }

                     box.pauseTicks = 8;
                  }

                  if (box.visibleIdx >= box.visibleLen) {
                     boolean hasNext = box.pageIdx + 1 < box.pages.length;
                     box.waitTicks = hasNext ? 60 : 80;
                  }
                  break;
               }
            }
         } else if (box.waitTicks > 0) {
            box.waitTicks--;
         } else if (box.pageIdx + 1 < box.pages.length) {
            box.pageIdx++;
            box.visibleIdx = 0;
            box.pauseTicks = 0;
            box.waitTicks = -1;
            box.visibleLen = countVisible(box.pages[box.pageIdx]);
         } else {
            toRemove.add(speakerId);
         }
      }

      for (UUID id : toRemove) {
         activeBoxes.remove(id);
      }
   }

   private static void playPositionedTextSound(MinecraftClient mc, UUID speakerId) {
      if (mc.world != null && mc.player != null) {
         PlayerEntity speaker = mc.world.getPlayerByUuid(speakerId);
         if (speaker != null) {
            double dist = mc.player.getPos().distanceTo(speaker.getPos());
            float volume;
            if (dist <= 10.0) {
               volume = 1.0F;
            } else if (dist >= 50.0) {
               volume = 0.0F;
            } else {
               volume = 1.0F - (float)((dist - 10.0) / 40.0);
            }

            if (!(volume <= 0.0F)) {
               mc.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SND_TEXT, volume, 1.0F));
            }
         }
      }
   }

   public static void render(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (!activeBoxes.isEmpty() && mc.world != null) {
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         Vec3d cam = ctx.camera().getPos();
         float td = ctx.tickCounter().getTickDelta(true);
         boolean firstPerson = mc.options.getPerspective().isFirstPerson();
         List next = new ArrayList<>();

         for (Entry entry : activeBoxes.entrySet()) {
            PlayerEntity speaker = mc.world.getPlayerByUuid(entry.getKey());
            if (speaker != null && (speaker != mc.player || !firstPerson)) {
               ProximityChatRenderer.ActiveBox box = entry.getValue();
               double x = MathHelper.lerp((double)td, speaker.prevX, speaker.getX());
               double y = MathHelper.lerp((double)td, speaker.prevY, speaker.getY()) + (double)speaker.getHeight() + 0.45;
               double z = MathHelper.lerp((double)td, speaker.prevZ, speaker.getZ());
               double dist = Math.sqrt((x - cam.x) * (x - cam.x) + (y - cam.y) * (y - cam.y) + (z - cam.z) * (z - cam.z));
               if (!(dist > 32.0)) {
                  WorldToScreen.ScreenPos pos = WorldToScreen.project(ctx, x - cam.x, y - cam.y, z - cam.z, sw, sh);
                  if (pos != null) {
                     float alpha = (float)MathHelper.clamp((32.0 - dist) / 8.0, 0.0, 1.0);
                     BlockHitResult hit = mc.world.raycast(new RaycastContext(cam, new Vec3d(x, y, z), ShapeType.VISUAL, FluidHandling.NONE, mc.player));
                     if (hit != null && hit.getType() == Type.BLOCK) {
                        alpha *= 0.35F;
                     }

                     float in = MathHelper.clamp(((float)box.age + td) / 4.0F, 0.0F, 1.0F);
                     boolean last = box.pageIdx + 1 >= box.pages.length;
                     float out = last && box.waitTicks >= 0 ? MathHelper.clamp(((float)box.waitTicks - td) / 8.0F, 0.0F, 1.0F) : 1.0F;
                     alpha *= in * out;
                     if (!(alpha <= 0.02F)) {
                        float scale = (float)MathHelper.clamp(7.0 / Math.max(dist, 1.0), 0.45, 1.15) * (0.85F + 0.15F * in);
                        next.add(new ProximityChatRenderer.Pending((float)pos.x(), (float)pos.y(), scale, alpha, dist, box));
                     }
                  }
               }
            }
         }

         next.sort((a, b2) -> Double.compare(b2.dist(), a.dist()));
         pending = next;
      } else {
         pending = List.of();
      }
   }

   public static void renderHud(DrawContext ctx) {
      List snap = pending;
      if (!snap.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         TextRenderer tr = mc.textRenderer;
         int starW = tr.getWidth("* ");

         for (ProximityChatRenderer.Pending p : snap) {
            ProximityChatRenderer.ActiveBox box = p.box();
            String page = box.pages[Math.min(box.pageIdx, box.pages.length - 1)];
            List full = wrapLines(tr, page, 190 - starW);
            int textW = 0;

            for (String l : full) {
               textW = Math.max(textW, tr.getWidth(l));
            }

            int w = Math.max(48, starW + textW + 12);
            int h = Math.max(1, full.size()) * 10 + 12 - 2;
            List shown = wrapLines(tr, visibleSubstring(page, box.visibleIdx), 190 - starW);
            int a = Math.round(p.alpha() * 255.0F);
            int black = Math.round(p.alpha() * 235.0F) << 24;
            int white = a << 24 | 16777215;
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(p.x(), p.y(), 0.0F);
            m.scale(p.scale(), p.scale(), 1.0F);
            int x0 = -w / 2;
            int y0 = -h - 6;
            if (DarkBox.inDarkWorld()) {
               DarkBox.draw(ctx, x0, y0, x0 + w, y0 + h, p.alpha());
            } else {
               ctx.fill(x0, y0, x0 + w, y0 + h, black);
               ctx.fill(x0, y0, x0 + w, y0 + 2, white);
               ctx.fill(x0, y0 + h - 2, x0 + w, y0 + h, white);
               ctx.fill(x0, y0, x0 + 2, y0 + h, white);
               ctx.fill(x0 + w - 2, y0, x0 + w, y0 + h, white);
            }

            for (int i = 0; i < 5; i++) {
               int hw = 4 - i;
               ctx.fill(-hw - 1, y0 + h + i - 2, hw + 1, y0 + h + i - 1, white);
               if (hw > 1) {
                  ctx.fill(-hw + 1, y0 + h + i - 2, hw - 1, y0 + h + i - 1, black);
               }
            }

            int tx = x0 + 6;
            int ty = y0 + 6;
            ctx.drawText(tr, "*", tx, ty, white, false);

            for (int ix = 0; ix < shown.size(); ix++) {
               ctx.drawText(tr, shown.get(ix), tx + starW, ty + ix * 10, white, false);
            }

            m.pop();
         }
      }
   }

   private static List<String> wrapLines(TextRenderer tr, String text, int maxW) {
      List lines = new ArrayList<>();
      if (text.isEmpty()) {
         return lines;
      } else {
         for (String segment : text.split("\n", -1)) {
            StringBuilder cur = new StringBuilder();

            for (String w : segment.split(" ", -1)) {
               String test = cur.isEmpty() ? w : cur + " " + w;
               if (tr.getWidth(test) > maxW && !cur.isEmpty()) {
                  lines.add(cur.toString());
                  cur = new StringBuilder(w);
               } else {
                  cur = new StringBuilder(test);
               }
            }

            lines.add(cur.toString());
         }

         return lines;
      }
   }

   private static class ActiveBox {
      final String[] pages;
      int pageIdx = 0;
      int visibleIdx = 0;
      int visibleLen;
      int pauseTicks = 0;
      int waitTicks = -1;
      int age = 0;

      ActiveBox(String[] pages) {
         this.pages = pages != null && pages.length > 0 ? pages : new String[]{""};
         this.visibleLen = ProximityChatRenderer.countVisible(this.pages[0]);
         if (this.pages.length != 0 && this.pages[0].isEmpty()) {
         }
      }
   }

   private static record Pending(float x, float y, float scale, float alpha, double dist, ProximityChatRenderer.ActiveBox box) {
   }
}
