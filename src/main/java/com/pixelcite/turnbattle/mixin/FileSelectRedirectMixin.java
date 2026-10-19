package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.FileSelectScreen;
import com.pixelcite.turnbattle.TitleMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftClient.class})
public abstract class FileSelectRedirectMixin {
   @Shadow
   public Screen field_1755;

   @Shadow
   public abstract void method_1507(Screen var1);

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnbattle$fileSelect(Screen screen, CallbackInfo ci) {
      if (screen instanceof TitleScreen || screen instanceof TitleMenuScreen) {
         FileSelectScreen.classicMode = false;
      }

      if (screen != null && screen.getClass() == TitleScreen.class && !TitleMenuScreen.classic) {
         ci.cancel();
         this.method_1507(new TitleMenuScreen());
      } else if (!(screen instanceof TitleScreen)) {
         if (screen instanceof SelectWorldScreen && !FileSelectScreen.classicMode) {
            Screen parent = (Screen)(!(this.field_1755 instanceof FileSelectScreen) && this.field_1755 != null ? this.field_1755 : new TitleScreen());
            ci.cancel();
            this.method_1507(new FileSelectScreen(parent));
         }
      }
   }
}
