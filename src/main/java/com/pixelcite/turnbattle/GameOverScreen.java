package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class GameOverScreen extends Screen {
   private static final Identifier GAME_OVER_TEX = Identifier.of("turnbattle", "textures/gui/game_over.png");
   private static final int TEX_W = 310;
   private static final int TEX_H = 92;
   private static final int SOUL_NATIVE = 9;
   private static final int SOUL_SIZE = 13;
   private static final float TEXT_SCALE = 2.0F;
   private static final int FADE_TICKS = 30;
   private static final int SOUL_ENTER_TICKS = 20;
   private static final int HOLD_TICKS = 15;
   private static final int IMAGE_FADE_TICKS = 35;
   private static final int SLIDE_TO_OPTION_TICKS = 13;
   private GameOverScreen.Phase phase = GameOverScreen.Phase.FADE_AND_ENTER;
   private int tick = 0;
   private int phaseStartTick = 0;
   private long lastTickRealTimeNanos = 0L;
   private float lastRenderDelta = 0.0F;
   private final boolean diedInBattle;
   private final boolean diedInBulletHell;
   private final float soulStartX;
   private final float soulStartY;
   private final boolean soulFadesIn;
   private int selectedOption = 0;
   private SoundInstance musicInstance = null;
   private boolean musicStarted = false;

   public GameOverScreen() {
      super(Text.literal(""));
      this.diedInBattle = DeathSoulTracker.wasInBattle && DeathSoulTracker.isFresh();
      this.diedInBulletHell = this.diedInBattle && DeathSoulTracker.wasInBulletHell;
      this.soulFadesIn = !this.diedInBulletHell;
      if (this.diedInBattle) {
         this.soulStartX = DeathSoulTracker.lastSoulScreenX;
         this.soulStartY = DeathSoulTracker.lastSoulScreenY;
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         int w = mc != null ? mc.getWindow().getScaledWidth() : 320;
         int h = mc != null ? mc.getWindow().getScaledHeight() : 240;
         this.soulStartX = (float)w / 2.0F;
         this.soulStartY = (float)h * 0.85F;
      }
   }

   public boolean shouldPause() {
      return true;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void tick() {
      this.tick++;
      this.lastTickRealTimeNanos = System.nanoTime();
      int localTick = this.tick - this.phaseStartTick;
      switch (this.phase) {
         case FADE_AND_ENTER:
            if (localTick >= Math.max(30, 20)) {
               this.phase = GameOverScreen.Phase.HOLD;
               this.phaseStartTick = this.tick;
            }
            break;
         case HOLD:
            if (localTick >= 15) {
               this.phase = GameOverScreen.Phase.IMAGE_FADE;
               this.phaseStartTick = this.tick;
            }
            break;
         case IMAGE_FADE:
            if (!this.musicStarted) {
               this.musicInstance = TurnBattleSounds.startFaintCourageMusic();
               this.musicStarted = true;
            }

            if (localTick >= 35) {
               this.phase = GameOverScreen.Phase.SLIDE_TO_OPTION;
               this.phaseStartTick = this.tick;
            }
            break;
         case SLIDE_TO_OPTION:
            if (localTick >= 13) {
               this.phase = GameOverScreen.Phase.INTERACTIVE;
               this.phaseStartTick = this.tick;
            }
         case INTERACTIVE:
      }
   }

   private static float smoothstep(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
   }

   private void navigate(int dir) {
      int newSel = Math.max(0, Math.min(1, this.selectedOption + dir));
      if (newSel != this.selectedOption) {
         this.selectedOption = newSel;
         this.playMenuMove();
      }
   }

   private void confirmSelection() {
      this.playMenuSelect();
      if (this.selectedOption == 0) {
         this.doContinue();
      } else {
         this.doExit();
      }
   }

   private void doContinue() {
      if (this.client != null) {
         if (this.musicInstance != null) {
            this.client.getSoundManager().stop(this.musicInstance);
         }

         if (this.client.player != null) {
            this.client.player.requestRespawn();
         }

         this.client.setScreen(null);
      }
   }

   private void doExit() {
      if (this.client != null) {
         if (this.musicInstance != null) {
            this.client.getSoundManager().stop(this.musicInstance);
         }

         if (this.client.world != null) {
            this.client.world.disconnect();
         }

         this.client.disconnect();
         this.client.setScreen(new TitleScreen());
      }
   }

   private void playMenuMove() {
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.MENU_MOVE, 1.0F));
      }
   }

   private void playMenuSelect() {
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.MENU_SELECT, 1.0F));
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.phase != GameOverScreen.Phase.INTERACTIVE) {
         return true;
      } else if (matchesBind(TurnBattleClientMod.battleLeft, key)) {
         this.navigate(-1);
         return true;
      } else if (matchesBind(TurnBattleClientMod.battleRight, key)) {
         this.navigate(1);
         return true;
      } else if (!matchesBind(TurnBattleClientMod.battleConfirm, key) && key != 257 && key != 335) {
         return super.keyPressed(key, scan, mods);
      } else {
         this.confirmSelection();
         return true;
      }
   }

   private static boolean matchesBind(KeyBinding kb, int glfwKey) {
      if (kb == null) {
         return false;
      } else {
         Key bound;
         try {
            bound = KeyBindingHelper.getBoundKeyOf(kb);
         } catch (Exception var4) {
            bound = kb.getDefaultKey();
         }

         if (bound.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
            Integer def = TurnBattleClientMod.defaultBattleKeys.get(kb);
            return def != null ? def == glfwKey : false;
         } else {
            return bound.getCategory() == Type.KEYSYM && bound.getCode() == glfwKey;
         }
      }
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      if (this.lastTickRealTimeNanos == 0L) {
         this.lastTickRealTimeNanos = System.nanoTime();
      }

      float secondsSinceLastTick = (float)(System.nanoTime() - this.lastTickRealTimeNanos) / 1.0E9F;
      this.lastRenderDelta = Math.min(1.0F, Math.max(0.0F, secondsSinceLastTick / 0.05F));
      int w = this.width;
      int h = this.height;
      float tickF = (float)this.tick + this.lastRenderDelta;
      float centerX = (float)w / 2.0F;
      float centerY = (float)h / 2.0F;
      int fadeAlpha;
      if (this.phase == GameOverScreen.Phase.FADE_AND_ENTER) {
         fadeAlpha = (int)(255.0F * smoothstep(tickF / 30.0F));
      } else {
         fadeAlpha = 255;
      }

      ctx.fill(0, 0, w, h, fadeAlpha << 24);
      float continueTextX = centerX - 70.0F;
      float exitTextX = centerX + 70.0F;
      float optTextY = centerY + 40.0F;
      float continueLeftEdge = continueTextX - (float)this.textRenderer.getWidth("CONTINUE") * 2.0F / 2.0F;
      float exitLeftEdge = exitTextX - (float)this.textRenderer.getWidth("EXIT") * 2.0F / 2.0F;
      float soulPad = 14.5F;
      int soulAlpha = 255;
      float soulX;
      float soulY;
      if (this.phase == GameOverScreen.Phase.FADE_AND_ENTER) {
         float localT = Math.min(1.0F, tickF / 20.0F);
         float ease = smoothstep(localT);
         soulX = this.soulStartX + (centerX - this.soulStartX) * ease;
         soulY = this.soulStartY + (centerY - this.soulStartY) * ease;
         soulAlpha = this.soulFadesIn ? (int)(255.0F * ease) : 255;
      } else if (this.phase != GameOverScreen.Phase.HOLD && this.phase != GameOverScreen.Phase.IMAGE_FADE) {
         float optX = (this.selectedOption == 0 ? continueLeftEdge : exitLeftEdge) - soulPad;
         if (this.phase == GameOverScreen.Phase.SLIDE_TO_OPTION) {
            float localT = Math.min(1.0F, ((float)(this.tick - this.phaseStartTick) + this.lastRenderDelta) / 13.0F);
            float ease = smoothstep(localT);
            soulX = centerX + (optX - centerX) * ease;
            soulY = centerY + (optTextY - centerY) * ease;
         } else {
            soulX = optX;
            soulY = optTextY;
         }

         soulAlpha = 255;
      } else {
         soulX = centerX;
         soulY = centerY;
         soulAlpha = 255;
      }

      int imageAlpha = 0;
      if (this.phase == GameOverScreen.Phase.IMAGE_FADE) {
         float localT = Math.min(1.0F, ((float)(this.tick - this.phaseStartTick) + this.lastRenderDelta) / 35.0F);
         imageAlpha = (int)(255.0F * smoothstep(localT));
      } else if (this.phase == GameOverScreen.Phase.SLIDE_TO_OPTION || this.phase == GameOverScreen.Phase.INTERACTIVE) {
         imageAlpha = 255;
      }

      if (imageAlpha > 0) {
         int drawW = 310;
         int drawH = 92;
         int imgX = w / 2 - drawW / 2;
         int imgY = (int)((float)h * 0.33333334F) - drawH / 2;
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)imageAlpha / 255.0F);
         ctx.drawTexture(GAME_OVER_TEX, imgX, imgY, 0.0F, 0.0F, drawW, drawH, 310, 92);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         int optColor0 = (this.selectedOption == 0 ? 16777215 : 8947848) | imageAlpha << 24;
         int optColor1 = (this.selectedOption == 1 ? 16777215 : 8947848) | imageAlpha << 24;
         this.drawScaledCentered(ctx, "CONTINUE", continueTextX, optTextY, optColor0);
         this.drawScaledCentered(ctx, "EXIT", exitTextX, optTextY, optColor1);
      }

      if (soulAlpha > 0) {
         SoulType soul = TurnBattleClientMod.clientSoul;
         Identifier soulTex = soul != null ? soul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)soulAlpha / 255.0F);
         ctx.drawTexture(soulTex, (int)(soulX - 6.5F), (int)(soulY - 6.5F), 13, 13, 0.0F, 0.0F, 9, 9, 9, 9);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void drawScaledCentered(DrawContext ctx, String text, float cx, float y, int color) {
      int textW = this.textRenderer.getWidth(text);
      MatrixStack matrices = ctx.getMatrices();
      matrices.push();
      matrices.translate(cx - (float)textW * 2.0F / 2.0F, y - 9.0F * 2.0F / 2.0F, 0.0F);
      matrices.scale(2.0F, 2.0F, 1.0F);
      ctx.drawTextWithShadow(this.textRenderer, text, 0, 0, color);
      matrices.pop();
   }

   private static enum Phase {
      FADE_AND_ENTER,
      HOLD,
      IMAGE_FADE,
      SLIDE_TO_OPTION,
      INTERACTIVE;
   }
}
