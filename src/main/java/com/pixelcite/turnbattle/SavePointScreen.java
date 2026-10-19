package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SavePointScreen extends Screen {
   private static final Identifier TEXT_BOX = Identifier.of("turnbattle", "textures/gui/deltarune_text_box.png");
   private static final int TEX_W = 593;
   private static final int TEX_H = 167;
   private static final int BOX_DISPLAY_W = 340;
   private static final float BOX_SCALE = 0.5733558F;
   private static final int BOX_DISPLAY_H = 95;
   private static final float TEXT_SCALE = 1.5F;
   private static final int LINGER_TICKS = 60;
   private final SavePointEntity entity;
   private int selectedIdx = 0;
   private boolean saved = false;
   private int savedLingerTicks = 0;
   private int tick = 0;
   private String vesselName = "Kris";
   private int level = 1;
   private long playtimeSec = 0L;
   private String biomeName = "---";

   public SavePointScreen(@Nullable SavePointEntity entity) {
      super(Text.empty());
      this.entity = entity;
   }

   protected void init() {
      super.init();
      if (this.client != null && this.client.player != null) {
         this.vesselName = TurnBattleClientMod.clientVesselName != null && !TurnBattleClientMod.clientVesselName.isEmpty()
            ? TurnBattleClientMod.clientVesselName
            : "Kris";
         this.level = Math.max(1, TurnBattleClientMod.clientLevel);
         this.playtimeSec = TurnBattleClientMod.clientPlaytimeSec;
         RegistryEntry biomeEntry = this.client.world != null ? this.client.world.getBiome(this.client.player.getBlockPos()) : null;
         String vanillaBiomeName = biomeEntry != null ? biomeEntry.getKey().map(k -> this.formatBiomeName(k.getValue().getPath())).orElse("---") : "---";
         String pointName = this.entity != null ? this.entity.getPointName() : null;
         this.biomeName = pointName != null && !pointName.isEmpty() ? pointName : vanillaBiomeName;
      }
   }

   private String formatBiomeName(String raw) {
      String[] parts = raw.split("_");
      StringBuilder sb = new StringBuilder();

      for (String p : parts) {
         if (!p.isEmpty()) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
         }
      }

      return sb.toString().trim();
   }

   public void tick() {
      this.tick++;
      if (this.saved) {
         this.savedLingerTicks++;
         if (this.savedLingerTicks >= 60) {
            this.close();
         }
      }
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      super.render(ctx, mx, my, delta);
      int boxX = (this.width - 340) / 2;
      int boxY = this.height - 95 - (int)((float)this.height * 0.18F);
      MatrixStack matrices = ctx.getMatrices();
      matrices.push();
      matrices.translate((float)boxX, (float)boxY, 0.0F);
      matrices.scale(0.5733558F, 0.5733558F, 1.0F);
      ctx.drawTexture(TEXT_BOX, 0, 0, 0.0F, 0.0F, 593, 167, 593, 167);
      matrices.pop();
      int textColor = this.saved ? -8960 : -1;
      int innerX = boxX + 23;
      int innerY = boxY + 13;
      int innerRight = boxX + 340 - 23;
      int lineH = (int)(9.0F * 1.5F) + 2;
      long mins = this.playtimeSec / 60L;
      long secs = this.playtimeSec % 60L;
      String timeStr = String.format("%d:%02d", mins, secs);
      String lvStr = "LV " + this.level;
      this.drawScaledText(ctx, this.vesselName, innerX, innerY, textColor);
      int lvW = (int)((float)this.textRenderer.getWidth(lvStr) * 1.5F);
      int timeW = (int)((float)this.textRenderer.getWidth(timeStr) * 1.5F);
      int gap = 15;
      int lvX = innerRight - timeW - gap - lvW;
      this.drawScaledText(ctx, lvStr, lvX, innerY, textColor);
      this.drawScaledText(ctx, timeStr, innerRight - timeW, innerY, textColor);
      this.drawScaledText(ctx, this.biomeName, innerX, innerY + lineH, textColor);
      int saveRowY = innerY + lineH * 3;
      SoulType soul = TurnBattleClientMod.clientSoul;
      Identifier soulTex = soul != null ? soul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
      int soulSize = 13;
      int soulY = saveRowY + ((int)(9.0F * 1.5F) - soulSize) / 2;
      int soulPad = soulSize + 4;
      if (!this.saved) {
         int saveTextX = innerX + soulPad;
         int returnX = innerX + 156;
         int soulX = this.selectedIdx == 0 ? innerX : returnX;
         if (this.selectedIdx != 0) {
            int var10000 = returnX + soulPad;
         }

         ctx.drawTexture(soulTex, soulX, soulY, soulSize, soulSize, 0.0F, 0.0F, 9, 9, 9, 9);
         this.drawScaledText(ctx, "Save", saveTextX, saveRowY, textColor);
         this.drawScaledText(ctx, "Return", returnX + soulPad, saveRowY, textColor);
      } else {
         ctx.drawTexture(soulTex, innerX, soulY, soulSize, soulSize, 0.0F, 0.0F, 9, 9, 9, 9);
         this.drawScaledText(ctx, "File saved.", innerX + soulPad, saveRowY, textColor);
      }
   }

   private void drawScaledText(DrawContext ctx, String text, int x, int y, int color) {
      MatrixStack matrices = ctx.getMatrices();
      matrices.push();
      matrices.translate((float)x, (float)y, 0.0F);
      matrices.scale(1.5F, 1.5F, 1.0F);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(text), 0, 0, color);
      matrices.pop();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.saved) {
         return true;
      } else if (keyCode != 65 && keyCode != 263) {
         if (keyCode != 68 && keyCode != 262) {
            if (keyCode == 32 || keyCode == 257 || keyCode == 90) {
               this.confirm();
               return true;
            } else if (keyCode != 256 && keyCode != 88) {
               return super.keyPressed(keyCode, scanCode, modifiers);
            } else {
               this.close();
               return true;
            }
         } else {
            if (this.selectedIdx != 1) {
               this.selectedIdx = 1;
               this.playNavSound();
            }

            return true;
         }
      } else {
         if (this.selectedIdx != 0) {
            this.selectedIdx = 0;
            this.playNavSound();
         }

         return true;
      }
   }

   private void playNavSound() {
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.MENU_MOVE, 1.0F));
      }
   }

   private void confirm() {
      if (this.selectedIdx == 1) {
         this.close();
      } else {
         this.saved = true;
         this.savedLingerTicks = 0;
         if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SND_SAVE, 1.0F, 1.0F));
         }

         PacketByteBuf buf = PacketByteBufs.create();
         Net121.sendToServer(TurnBattleMod.SAVE_POINT_USE, buf);
      }
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
   }
}
