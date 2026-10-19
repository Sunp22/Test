package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class TurnBattlePreLaunch implements PreLaunchEntrypoint {
   private static final String ATTRIBUTEFIX_URL = "https://modrinth.com/mod/attributefix";
   private static final String CAXTON_URL = "https://modrinth.com/mod/caxton";

   public void onPreLaunch() {
      FabricLoader loader = FabricLoader.getInstance();
      if (!loader.isDevelopmentEnvironment()) {
         List missing = new ArrayList<>();
         if (!loader.isModLoaded("attributefix")) {
            missing.add("AttributeFix  -  https://modrinth.com/mod/attributefix");
         }

         if (loader.getEnvironmentType() == EnvType.CLIENT && !loader.isModLoaded("caxton")) {
            missing.add("Caxton  -  https://modrinth.com/mod/caxton");
         }

         if (!missing.isEmpty()) {
            StringBuilder msg = new StringBuilder("Soulbound Turn Battle can't start without these mods:\n\n");

            for (String m : missing) {
               msg.append("  \u2022 ").append(m).append('\n');
            }

            msg.append("\nInstall them (the Fabric 1.21.1 versions) into your mods folder, then launch again.");
            String text = msg.toString();
            RuntimeException err = new RuntimeException(text.replace('\n', ' '));
            if (loader.getEnvironmentType() == EnvType.CLIENT) {
               try {
                  Class.forName("net.fabricmc.loader.impl.gui.FabricGuiEntry")
                     .getMethod("displayError", String.class, Throwable.class, boolean.class)
                     .invoke(null, text, err, true);
               } catch (Throwable var7) {
               }
            }

            System.err.println("[Soulbound Turn Battle] " + text);
            throw err;
         }
      }
   }
}
