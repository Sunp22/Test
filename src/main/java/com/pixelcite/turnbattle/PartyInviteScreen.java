package com.pixelcite.turnbattle;

import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PartyInviteScreen extends Screen {
   private final String inviterName;
   private final UUID inviterUuid;
   private final Screen parent;
   private static final int W = 220;
   private static final int H = 84;
   private static final int AUTO_CLOSE_TICKS = 400;
   private static final Identifier DEFAULT_SOUL = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private int tick = 0;
   private int sel = 0;
   private float shown = 0.0F;
   private int choseAt = -1;
   private boolean accepted;
   private final String line;

   public PartyInviteScreen(String inviterName, UUID inviterUuid, Screen parent) {
      super(Text.literal("Party Invite"));
      this.inviterName = inviterName;
      this.inviterUuid = inviterUuid;
      this.parent = parent;
      this.line = inviterName + " wants you to join their party!";
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean shouldPause() {
      return false;
   }

   public void tick() {
      this.tick++;
      if (this.choseAt >= 0) {
         if (this.tick - this.choseAt >= 5) {
            this.finish();
         }
      } else {
         if (this.shown < (float)this.line.length()) {
            int before = (int)this.shown;
            this.shown = Math.min((float)this.line.length(), this.shown + 1.5F);

            for (int i = before; i < (int)this.shown; i++) {
               if (i % 2 == 0 && !Character.isWhitespace(this.line.charAt(i))) {
                  TurnBattleSounds.playClient(TurnBattleSounds.SND_TEXT, 0.8F, 1.0F);
                  break;
               }
            }
         }

         if (this.tick >= 400) {
            this.choose(false);
         }
      }
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   private int px() {
      return (this.width - 220) / 2;
   }

   private int py() {
      return (this.height - 84) / 2;
   }

   private int optY() {
      return this.py() + 84 - 24;
   }

   private int acceptX() {
      return this.px() + 110 - 70;
   }

   private int declineX() {
      return this.px() + 110 + 18;
   }

   private float popScale(float delta) {
      float t = (float)this.tick + delta;
      if (this.choseAt >= 0) {
         return Math.max(0.0F, 1.0F - (t - (float)this.choseAt) / 5.0F);
      } else if (t >= 6.0F) {
         return 1.0F;
      } else {
         float x = t / 6.0F;
         return (float)(1.0 + 0.12 * Math.sin((double)x * Math.PI)) * Math.min(1.0F, x * 1.4F);
      }
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      float k = this.popScale(delta);
      if (!(k <= 0.01F)) {
         int px = this.px();
         int py = this.py();
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate((float)this.width / 2.0F, (float)this.height / 2.0F, 0.0F);
         m.scale(k, k, 1.0F);
         m.translate((float)(-this.width) / 2.0F, (float)(-this.height) / 2.0F, 0.0F);
         ctx.fill(px - 3, py - 3, px + 220 + 3, py + 84 + 3, -1);
         ctx.fill(px, py, px + 220, py + 84, -268435456);
         String tab = "PARTY INVITE";
         int tw = this.textRenderer.getWidth(tab) + 14;
         int tx = px + 10;
         ctx.fill(tx - 3, py - 13, tx + tw + 3, py, -1);
         ctx.fill(tx, py - 10, tx + tw, py, -16777216);
         ctx.drawText(this.textRenderer, tab, tx + 7, py - 8, -256, false);
         int fx = px + 10;
         int fy = py + 10;
         int fs = 24;
         ctx.fill(fx - 1, fy - 1, fx + fs + 1, fy + fs + 1, -12566464);
         PlayerListEntry entry = this.client != null && this.client.getNetworkHandler() != null
            ? this.client.getNetworkHandler().getPlayerListEntry(this.inviterUuid)
            : null;
         if (entry != null) {
            PlayerSkinDrawer.draw(ctx, entry.getSkinTextures(), fx, fy, fs);
         } else {
            ctx.fill(fx, fy, fx + fs, fy + fs, -14671840);
         }

         String t = this.line.substring(0, Math.min(this.line.length(), (int)this.shown));
         int ty = py + 12;
         ctx.drawText(this.textRenderer, "*", fx + fs + 8, ty, -1, false);

         for (OrderedText l : this.textRenderer.wrapLines(Text.literal(t), 220 - fs - 34)) {
            ctx.drawText(this.textRenderer, l, fx + fs + 18, ty, -1, false);
            ty += 11;
         }

         int oy = this.optY();
         this.hover(mx, my);
         this.drawOption(ctx, "Accept", this.acceptX(), oy, this.sel == 0, -8323200);
         this.drawOption(ctx, "Decline", this.declineX(), oy, this.sel == 1, -32640);
         float remaining = Math.max(0.0F, 1.0F - ((float)this.tick + delta) / 400.0F);
         int barX = px + 6;
         int barW = 208;
         int barY = py + 84 - 6;
         ctx.fill(barX, barY, barX + barW, barY + 2, -13619152);
         int col = remaining > 0.25F ? -1 : (this.tick / 4 % 2 == 0 ? -44976 : -1);
         ctx.fill(barX, barY, barX + Math.round((float)barW * remaining), barY + 2, col);
         m.pop();
         super.render(ctx, mx, my, delta);
      }
   }

   private void drawOption(DrawContext ctx, String label, int x, int y, boolean selected, int hi) {
      if (selected) {
         SoulType soul = TurnBattleClientMod.clientSoul;
         Identifier tex = soul != null ? soul.getTexture() : DEFAULT_SOUL;
         ctx.drawTexture(tex, x, y - 1, 9, 9, 0.0F, 0.0F, 9, 9, 9, 9);
      }

      ctx.drawText(this.textRenderer, label, x + 13, y, selected ? hi : -1, false);
   }

   private int optionAt(double mx, double my) {
      int oy = this.optY();
      if (!(my < (double)(oy - 4)) && !(my > (double)(oy + 12))) {
         if (mx >= (double)(this.acceptX() - 2) && mx < (double)(this.acceptX() + 13 + this.textRenderer.getWidth("Accept") + 2)) {
            return 0;
         } else {
            return mx >= (double)(this.declineX() - 2) && mx < (double)(this.declineX() + 13 + this.textRenderer.getWidth("Decline") + 2) ? 1 : -1;
         }
      } else {
         return -1;
      }
   }

   private void hover(int mx, int my) {
      int o = this.optionAt((double)mx, (double)my);
      if (o >= 0 && o != this.sel && this.choseAt < 0) {
         this.sel = o;
         TurnBattleSounds.playHover();
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.choseAt >= 0) {
         return true;
      } else {
         switch (key) {
            case 32:
            case 90:
            case 257:
            case 335:
               if (this.shown < (float)this.line.length()) {
                  this.shown = (float)this.line.length();
                  return true;
               }

               TurnBattleSounds.playSelect();
               this.choose(this.sel == 0);
               return true;
            case 65:
            case 68:
            case 262:
            case 263:
               this.sel = 1 - this.sel;
               TurnBattleSounds.playHover();
               return true;
            case 88:
            case 256:
               this.choose(false);
               return true;
            default:
               return super.keyPressed(key, scan, mods);
         }
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (this.choseAt >= 0) {
         return true;
      } else {
         int o = this.optionAt(mx, my);
         if (btn == 0 && o >= 0) {
            TurnBattleSounds.playSelect();
            this.choose(o == 0);
            return true;
         } else {
            return super.mouseClicked(mx, my, btn);
         }
      }
   }

   private void choose(boolean accept) {
      if (this.choseAt < 0) {
         this.accepted = accept;
         this.choseAt = this.tick;
         if (accept) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(this.inviterName);
            Net121.sendToServer(TurnBattleMod.PARTY_INVITE_ACCEPT, buf);
         } else {
            Net121.sendToServer(TurnBattleMod.PARTY_INVITE_DECLINE, PacketByteBufs.empty());
         }
      }
   }

   private void finish() {
      if (this.client != null) {
         this.client.setScreen(this.parent);
      }
   }
}
