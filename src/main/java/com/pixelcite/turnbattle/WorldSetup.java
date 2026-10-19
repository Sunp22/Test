package com.pixelcite.turnbattle;

import com.pixelcite.turnbattle.mixin.CreateWorldScreenInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterRender;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterTick;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.screen.world.WorldCreator.Mode;
import net.minecraft.world.Difficulty;

@Environment(EnvType.CLIENT)
public final class WorldSetup {
   public static WorldSetup.Pending pending;

   private WorldSetup() {
   }

   public static void register() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, w, h) -> {
         if (screen instanceof CreateWorldScreen cws && pending != null) {
            WorldSetup.Pending p = pending;
            pending = null;

            try {
               WorldCreator wc = cws.getWorldCreator();
               wc.setWorldName(p.name());

               wc.setGameMode(switch (p.mode()) {
                  case 1 -> Mode.HARDCORE;
                  case 2 -> Mode.CREATIVE;
                  default -> Mode.SURVIVAL;
               });
               if (p.mode() != 1) {
                  wc.setDifficulty(p.difficulty());
                  wc.setCheatsEnabled(p.cheats());
               }

               if (!p.seed().isEmpty()) {
                  wc.setSeed(p.seed());
               }
            } catch (Throwable var7) {
               TurnBattleMod.LOGGER.warn("[FileSelect] Couldn't fill in the new world's settings", var7);
               return;
            }

            if (!p.start()) {
               return;
            }

            ScreenEvents.afterRender(screen).register((AfterRender)(s, ctx, mx, my, td) -> {
               MenuBackdrop.render(ctx, s.width, s.height);
               String msg = "Creating your world...";
               TextRenderer tr = client.textRenderer;
               ctx.drawTextWithShadow(tr, msg, s.width / 2 - tr.getWidth(msg) / 2, s.height / 2 - 4, -1);
            });
            boolean[] started = new boolean[]{false};
            ScreenEvents.afterTick(screen).register((AfterTick)s -> {
               if (!started[0] && client.currentScreen == s) {
                  started[0] = true;
                  ((CreateWorldScreenInvoker)s).turnbattle$createLevel();
               }
            });
            return;
         }
      });
   }

   public static record Pending(String name, int mode, Difficulty difficulty, boolean cheats, String seed, boolean start) {
   }
}
