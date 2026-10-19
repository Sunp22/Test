package com.pixelcite.turnbattle;

import com.pixelcite.turnbattle.mixin.GameRendererInvoker;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DarkFountainClient {
   private static final int E_BLACK_IN_END = 15;
   private static final int E_HOLD_BLACK_END = 60;
   private static final int E_TOTAL = 85;
   private static final int X_BLACK_IN_END = 15;
   private static final int X_HOLD_END = 25;
   private static final int X_FADE_OUT_END = 55;
   private static final int X_TOTAL = 55;
   private static final int HUE_PRESETS = 16;
   private static boolean active = false;
   private static boolean entering = false;
   private static int tickCount = 0;
   private static int activeHuePreset = -1;
   private static boolean confirmedInDarkWorld = false;
   public static int currentTintColor = -1;

   public static void gameTick() {
      if (active) {
         tickCount++;
         if (entering && tickCount == 15) {
            DarkWorldFall.start();
         }

         int total = entering ? 85 : 55;
         if (tickCount >= total) {
            active = false;
            tickCount = 0;
         }
      }
   }

   public static void startFade(boolean isEntering, int tintColor, int slotId, boolean silent, MinecraftClient client) {
      if (isEntering && slotId >= 0) {
         if (tintColor >= 0) {
            applyTintShader(tintColor, client, silent);
         } else {
            clearTintShader(client);
         }

         if (silent || isInDarkWorld(client)) {
            return;
         }
      } else if (!isEntering) {
         clearTintShader(client);
         if (LightPillarClient.whiteness() > 0.0F) {
            return;
         }
      }

      entering = isEntering;
      active = true;
      tickCount = 0;
   }

   public static void renderOverlay(DrawContext ctx, MinecraftClient client) {
      if (!(client.currentScreen instanceof DepthsDescentScreen)) {
         int sw = client.getWindow().getScaledWidth();
         int sh = client.getWindow().getScaledHeight();
         if (active) {
            if (entering) {
               if (!DarkWorldFall.active()) {
                  renderEntering(ctx, sw, sh);
               }
            } else {
               renderExiting(ctx, sw, sh);
            }
         }

         if (activeHuePreset != -1) {
            if (isInDarkWorld(client)) {
               confirmedInDarkWorld = true;
            } else if (confirmedInDarkWorld) {
               clearTintShader(client);
               confirmedInDarkWorld = false;
            }
         }
      }
   }

   public static int currentHuePreset() {
      return currentTintColor >= 0 ? nearestHuePreset(currentTintColor) : -1;
   }

   private static void applyTintShader(int tintColor, MinecraftClient client, boolean force) {
      currentTintColor = tintColor;
      int preset = nearestHuePreset(tintColor);
      if (preset != activeHuePreset || !isOurs(client, preset)) {
         Identifier shaderId = Identifier.of("turnbattle", "shaders/post/dw_tint_" + preset + ".json");

         try {
            ((GameRendererInvoker)client.gameRenderer).invokeLoadPostProcessor(shaderId);
            activeHuePreset = preset;
         } catch (Exception var6) {
            TurnBattleMod.LOGGER.warn("[DarkFountain] Failed to load tint shader {}: {}", shaderId, var6);
         }
      }
   }

   private static boolean isOurs(MinecraftClient client, int preset) {
      if (client.gameRenderer == null) {
         return false;
      } else {
         PostEffectProcessor pp = client.gameRenderer.getPostProcessor();
         return pp != null && pp.getName() != null && pp.getName().endsWith("dw_tint_" + preset + ".json");
      }
   }

   public static void ensureTint(MinecraftClient client) {
      if (activeHuePreset >= 0 && client != null && client.gameRenderer != null && isInDarkWorld(client)) {
         if (!isOurs(client, activeHuePreset)) {
            Identifier shaderId = Identifier.of("turnbattle", "shaders/post/dw_tint_" + activeHuePreset + ".json");

            try {
               ((GameRendererInvoker)client.gameRenderer).invokeLoadPostProcessor(shaderId);
            } catch (Exception var3) {
               TurnBattleMod.LOGGER.warn("[DarkFountain] Failed to restore tint shader {}: {}", shaderId, var3);
               activeHuePreset = -1;
            }
         }
      }
   }

   private static void clearTintShader(MinecraftClient client) {
      currentTintColor = -1;
      if (activeHuePreset != -1) {
         client.gameRenderer.disablePostProcessor();
         activeHuePreset = -1;
         confirmedInDarkWorld = false;
      }
   }

   private static int nearestHuePreset(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      float[] hsb = Color.RGBtoHSB(r, g, b, null);
      int preset = Math.round(hsb[0] * 16.0F) % 16;
      if (preset < 0) {
         preset += 16;
      }

      return preset;
   }

   private static void renderEntering(DrawContext ctx, int sw, int sh) {
      int t = tickCount;
      int a;
      if (t < 15) {
         a = t * 255 / 15;
      } else if (t < 60) {
         a = 255;
      } else {
         a = 255 - (t - 60) * 255 / 25;
      }

      if (a > 0) {
         ctx.fill(0, 0, sw, sh, argb(Math.min(255, a), 0, 0, 0));
      }
   }

   private static void renderExiting(DrawContext ctx, int sw, int sh) {
      int t = tickCount;
      if (t < 15) {
         int a = t * 255 / 15;
         ctx.fill(0, 0, sw, sh, argb(a, 0, 0, 0));
      } else if (t < 25) {
         ctx.fill(0, 0, sw, sh, argb(255, 0, 0, 0));
      } else if (t < 55) {
         int dt = t - 25;
         int a = 255 - dt * 255 / 30;
         ctx.fill(0, 0, sw, sh, argb(a, 0, 0, 0));
      }
   }

   private static boolean isInDarkWorld(MinecraftClient client) {
      if (client.world == null) {
         return false;
      } else {
         String id = client.world.getRegistryKey().getValue().toString();
         return id.startsWith("turnbattle:dark_world_");
      }
   }

   private static int argb(int a, int r, int g, int b) {
      return a << 24 | r << 16 | g << 8 | b;
   }
}
