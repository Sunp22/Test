package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public class SavePointNameScreen extends Screen {
   private final Hand hand;
   private TextFieldWidget nameField;

   public SavePointNameScreen(Hand hand) {
      super(Text.literal("Name Save Point"));
      this.hand = hand;
   }

   public boolean shouldPause() {
      return false;
   }

   protected void init() {
      int fw = 200;
      int fh = 18;
      this.nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - fw / 2, this.height / 2 - fh / 2, fw, fh, Text.literal(""));
      this.nameField.setMaxLength(24);
      this.nameField.setFocused(true);
      this.nameField.setPlaceholder(Text.literal("Leave blank to use the biome name\u2026").formatted(Formatting.DARK_GRAY));
      this.addDrawableChild(this.nameField);
      this.setFocused(this.nameField);
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      this.renderBackground(ctx, mx, my, delta);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Name this Save Point").formatted(Formatting.GOLD), this.width / 2, this.height / 2 - 30, -8892
      );
      super.render(ctx, mx, my, delta);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("Naming it renames this biome for everyone").formatted(Formatting.GRAY),
         this.width / 2,
         this.height / 2 + 16,
         -7829334
      );
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("[Enter] Place    [Esc] Cancel").formatted(Formatting.DARK_GRAY), this.width / 2, this.height / 2 + 30, -11184794
      );
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 257 || key == 335) {
         this.confirm();
         return true;
      } else {
         return this.nameField != null && this.nameField.isFocused() && this.nameField.keyPressed(key, scan, mods) ? true : super.keyPressed(key, scan, mods);
      }
   }

   private void confirm() {
      String name = this.nameField != null ? this.nameField.getText().trim() : "";
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeString(name, 32);
      Net121.sendToServer(TurnBattleMod.SAVE_POINT_PLACE, buf);
      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, -1342177280);
   }
}
