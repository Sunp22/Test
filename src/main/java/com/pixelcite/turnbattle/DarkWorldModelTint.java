package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class DarkWorldModelTint {
   private static SimpleFramebuffer buffer;
   private static PostEffectProcessor effect;
   private static int effectPreset = -1;
   private static boolean broken;
   private static boolean drawing;

   private DarkWorldModelTint() {
   }

   public static boolean begin(DrawContext ctx) {
      if (!broken && !drawing) {
         int preset = DarkFountainClient.currentHuePreset();
         if (preset < 0) {
            return false;
         } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getFramebufferWidth();
            int h = mc.getWindow().getFramebufferHeight();
            if (w > 0 && h > 0) {
               try {
                  if (buffer == null) {
                     buffer = new SimpleFramebuffer(w, h, true, MinecraftClient.IS_SYSTEM_MAC);
                     buffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                  } else if (buffer.textureWidth != w || buffer.textureHeight != h) {
                     buffer.resize(w, h, MinecraftClient.IS_SYSTEM_MAC);
                     if (effect != null) {
                        effect.setupDimensions(w, h);
                     }
                  }

                  if (effect == null || effectPreset != preset) {
                     if (effect != null) {
                        effect.close();
                        effect = null;
                     }

                     effect = new PostEffectProcessor(
                        mc.getTextureManager(), mc.getResourceManager(), buffer, Identifier.of("turnbattle", "shaders/post/dw_clone_" + preset + ".json")
                     );
                     effect.setupDimensions(w, h);
                     effectPreset = preset;
                  }
               } catch (Throwable var6) {
                  giveUp(mc, var6);
                  return false;
               }

               ctx.draw();
               buffer.clear(MinecraftClient.IS_SYSTEM_MAC);
               buffer.beginWrite(true);
               drawing = true;
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static void end(DrawContext ctx, float alpha) {
      if (drawing) {
         drawing = false;
         MinecraftClient mc = MinecraftClient.getInstance();
         ctx.draw();
         Framebuffer graded = null;

         try {
            effect.render(0.0F);
            graded = effect.getSecondaryTarget("swap");
         } catch (Throwable var18) {
            giveUp(mc, var18);
         } finally {
            mc.getFramebuffer().beginWrite(true);
         }

         if (graded != null && !(alpha <= 0.003F)) {
            double scale = mc.getWindow().getScaleFactor();
            float gw = (float)((double)graded.textureWidth / scale);
            float gh = (float)((double)graded.textureHeight / scale);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, graded.getColorAttachment());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();

            try {
               Matrix4f m = new Matrix4f();
               BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
               bb.vertex(m, 0.0F, gh, 0.0F).texture(0.0F, 0.0F);
               bb.vertex(m, gw, gh, 0.0F).texture(1.0F, 0.0F);
               bb.vertex(m, gw, 0.0F, 0.0F).texture(1.0F, 1.0F);
               bb.vertex(m, 0.0F, 0.0F, 0.0F).texture(0.0F, 1.0F);
               BuiltBuffer built = bb.endNullable();
               if (built != null) {
                  BufferRenderer.drawWithGlobalProgram(built);
               }
            } finally {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.enableCull();
               RenderSystem.enableDepthTest();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   private static void giveUp(MinecraftClient mc, Throwable t) {
      TurnBattleMod.LOGGER.warn("[TurnBattle] Dark World model tint unavailable, using the approximation: {}", t.toString());
      broken = true;
      drawing = false;

      try {
         if (effect != null) {
            effect.close();
         }
      } catch (Throwable var4) {
      }

      effect = null;
      effectPreset = -1;

      try {
         if (buffer != null) {
            buffer.delete();
         }
      } catch (Throwable var3) {
      }

      buffer = null;
      mc.getFramebuffer().beginWrite(true);
   }
}
