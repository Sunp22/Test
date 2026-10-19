package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public final class GuiScaleGuard {
   public static boolean allowed = false;
   private static int previous = -1;
   private static int ticks;

   private GuiScaleGuard() {
   }

   static void tick(MinecraftClient mc) {
      if (!allowed && mc.player != null && mc.world != null) {
         if (++ticks % 10 == 0) {
            if (mc.currentScreen == null) {
               int setting = (Integer)mc.options.getGuiScale().getValue();
               int actual = mc.getWindow().calculateScaleFactor(setting, mc.forcesUnicodeFont());
               if (actual >= 4) {
                  previous = setting;
                  apply(mc, 3);
                  Text button = Text.literal("[I know what I'm doing]")
                     .styled(
                        s -> s.withColor(Formatting.GREEN)
                              .withUnderline(true)
                              .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/soulboundguiscale allow"))
                              .withHoverEvent(
                                 new HoverEvent(
                                    net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Allow GUI scale 4 again (some Soulbound screens may not fit)").formatted(Formatting.GRAY)
                                 )
                              )
                     );
                  mc.player
                     .sendMessage(
                        Text.literal("[Soulbound] ")
                           .formatted(Formatting.GOLD)
                           .append(
                              Text.literal("Soulbound still doesn't quite work at GUI scale 4, so your GUI scale has been set to 3. Please use 3 or less. ")
                                 .formatted(Formatting.YELLOW)
                           )
                           .append(button),
                        false
                     );
               }
            }
         }
      }
   }

   static void allow(MinecraftClient mc) {
      allowed = true;
      TurnBattleClientMod.saveClientPrefs();
      if (previous >= 0) {
         apply(mc, previous);
      }

      previous = -1;
      if (mc.player != null) {
         mc.player
            .sendMessage(
               Text.literal("[Soulbound] ")
                  .formatted(Formatting.GOLD)
                  .append(Text.literal("Okay - GUI scale 4 is allowed. Set it however you like in Video Settings.").formatted(Formatting.GRAY)),
               false
            );
      }
   }

   private static void apply(MinecraftClient mc, int scale) {
      mc.options.getGuiScale().setValue(scale);
      mc.options.write();
      mc.onResolutionChanged();
   }
}
