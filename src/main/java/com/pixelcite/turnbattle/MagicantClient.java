package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class MagicantClient {
   private static MagicantClient.State state = MagicantClient.State.NONE;
   private static int wakeT;
   private static boolean wakeWon;
   private static long wakeTickNanos;

   private MagicantClient() {
   }

   public static boolean blackout() {
      return state == MagicantClient.State.CUTSCENE || state == MagicantClient.State.WAKING && wakeT >= 14;
   }

   public static boolean active() {
      return state != MagicantClient.State.NONE;
   }

   static void register() {
      EntityRendererRegistry.register(MagicantManager.MIRROR_CLONE, MirrorCloneRenderer::new);
      Net121.registerClientReceiver(MagicantManager.BEGIN, (client, handler, buf, sender) -> {
         boolean skip = buf.readableBytes() > 0 && buf.readBoolean();
         client.execute(() -> begin(skip));
      });
      Net121.registerClientReceiver(MagicantManager.WAKE, (client, handler, buf, sender) -> {
         int outcome = buf.readByte();
         client.execute(() -> wake(outcome == 1));
      });
      Net121.registerClientReceiver(MagicantManager.STATE, (client, handler, buf, sender) -> {
         boolean on = buf.readBoolean();
         client.execute(() -> {
            TurnBattleClientMod.clientMagicant = on;
            if (TurnBattleClientMod.clientPlayerStats != null) {
               TurnBattleClientMod.clientPlayerStats.pointValue = on ? 2 : 1;
            }
         });
      });
      ClientTickEvents.END_CLIENT_TICK.register(MagicantClient::tick);
      WorldRenderEvents.AFTER_ENTITIES.register(MagicantWorldFx::render);
      HudRenderCallback.EVENT.register((HudRenderCallback)(ctx, counter) -> renderWake(ctx));
   }

   private static void begin(boolean skip) {
      MinecraftClient mc = MinecraftClient.getInstance();
      MagicantScreen.reset();
      MagicantScreen.skip = skip;
      state = MagicantClient.State.CUTSCENE;
      mc.setScreen(new MagicantScreen());
   }

   static void release() {
      state = MagicantClient.State.NONE;
   }

   static void sendClashDone() {
      Net121.sendToServer(MagicantManager.CLASH_DONE, PacketByteBufs.empty());
   }

   private static void tick(MinecraftClient mc) {
      if (state != MagicantClient.State.NONE) {
         if (mc.getNetworkHandler() == null) {
            state = MagicantClient.State.NONE;
         } else {
            if (state == MagicantClient.State.CUTSCENE) {
               MagicantScreen.tick(mc);
               boolean battle = mc.currentScreen instanceof BattleScreen;
               if (battle) {
                  state = MagicantClient.State.NONE;
                  return;
               }

               if (!(mc.currentScreen instanceof MagicantScreen)
                  && !(mc.currentScreen instanceof DownloadingTerrainScreen)
                  && mc.world != null
                  && mc.player != null) {
                  mc.setScreen(new MagicantScreen());
               }

               int v = MagicantScreen.v();
               if (v >= 0 && v > MagicantScreen.CLASH_AT + 130 + 200) {
                  state = MagicantClient.State.NONE;
                  if (mc.currentScreen instanceof MagicantScreen) {
                     mc.setScreen(null);
                  }
               }
            } else if (state == MagicantClient.State.WAKING) {
               wakeTickNanos = System.nanoTime();
               wakeT++;
               if (mc.currentScreen instanceof MagicantScreen) {
                  mc.setScreen(null);
               }

               if (wakeT > 80) {
                  state = MagicantClient.State.NONE;
               }
            }
         }
      }
   }

   private static void wake(boolean won) {
      boolean fromCutscene = state == MagicantClient.State.CUTSCENE;
      state = MagicantClient.State.WAKING;
      wakeT = fromCutscene ? 18 : 0;
      wakeWon = won;
      wakeTickNanos = System.nanoTime();
   }

   private static void renderWake(DrawContext ctx) {
      if (state == MagicantClient.State.WAKING) {
         MinecraftClient mc = MinecraftClient.getInstance();
         float td = Math.min(1.0F, (float)(System.nanoTime() - wakeTickNanos) / 5.0E7F);
         float w = (float)wakeT + td;
         float a = w < 18.0F ? w / 18.0F : (w < 34.0F ? 1.0F : 1.0F - (w - 34.0F) / 36.0F);
         a = Math.max(0.0F, Math.min(1.0F, a));
         if (!(a <= 0.003F)) {
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            ctx.fill(0, 0, sw, sh, (int)(a * 255.0F) << 24);
            float ta = Math.max(0.0F, Math.min(1.0F, (w - 20.0F) / 8.0F)) * Math.max(0.0F, Math.min(1.0F, (70.0F - w) / 12.0F));
            if (ta > 0.02F) {
               String s = wakeWon ? "* MAGICANT has awakened." : "* ...You wake up.";
               int col = (int)(ta * 255.0F) << 24 | (wakeWon ? 16746751 : 13158616);
               ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(s), sw / 2, sh / 2 - 4, col);
            }
         }
      }
   }

   static enum State {
      NONE,
      CUTSCENE,
      WAKING;
   }
}
