package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class VesselCreatorScreen extends VoidMenuScreen {
   private static final SoulType[] SOULS = SoulType.values();
   private TextFieldWidget nameField;
   private String nameText = "";
   private int selectedSoul = 0;
   private long soulPickedNanos;
   private long createdNanos = 0L;
   private float figureYaw = 14.0F;
   private boolean leaving = false;

   public VesselCreatorScreen(Screen parent) {
      super("Vessel Creator", parent);
   }

   @Override
   protected int navIndex() {
      return 2;
   }

   @Override
   protected String headerTitle() {
      return "NEW VESSEL";
   }

   @Override
   protected String headerRight() {
      return "";
   }

   @Override
   protected void onFirstOpen() {
      this.soulPickedNanos = System.nanoTime();
   }

   @Override
   protected void init() {
      super.init();
      if (this.nameField != null) {
         this.nameText = this.nameField.getText();
      }

      int fw = Math.min(160, this.width / 3);
      this.nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - fw / 2, this.bodyTop() + 14, fw, 12, Text.literal("Name"));
      this.nameField.setMaxLength(28);
      this.nameField.setDrawsBackground(false);
      this.nameField.setPlaceholder(Text.literal("Give them a name...").formatted(Formatting.DARK_GRAY));
      this.nameField.setText(this.nameText);
      this.addSelectableChild(this.nameField);
   }

   private void focusName() {
      this.nameField.setFocused(true);
      this.setFocused(this.nameField);
      TurnBattleSounds.playSelect();
   }

   private void unfocusName() {
      this.nameField.setFocused(false);
      this.setFocused(null);
      TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
   }

   private SoulType soul() {
      return SOULS[this.selectedSoul];
   }

   @Override
   protected void renderBody(DrawContext ctx, int mx, int my) {
      float t = this.secs();
      int top = this.bodyTop();
      int sideW = (int)clamp((float)this.width * 0.3F, 150.0F, 240.0F);
      int rx = this.width - sideW - 10;
      int sx0 = 10;
      int sx1 = rx - 12;
      float scx = (float)(sx0 + sx1) / 2.0F;
      int footY = this.height - 10 - 40 - 22;
      float scale = clamp(Math.min((float)(footY - top - 50) / 2.1F, (float)(sx1 - sx0) * 0.24F), 12.0F, 60.0F);
      SoulType soul = this.soul();
      int scol = soul.getColor() & 16777215;
      this.renderVoid(ctx, scx, (float)footY, (float)(sx1 - sx0) * 1.3F);
      this.renderSouls(ctx, rx, top, sideW, mx, my);
      float in = ease(clamp01((t - 0.1F) / 0.45F));
      int fw = this.nameField.getWidth();
      int fx = (int)scx - fw / 2;
      int fy = top + 14;
      if (this.nameField.getX() != fx) {
         this.nameField.setX(fx);
      }

      ctx.drawCenteredTextWithShadow(this.textRenderer, "NAME", (int)scx, top, MenuBackdrop.argb(9413560, in));
      boolean focused = this.nameField.isFocused();
      ctx.fill(fx - 6, fy - 4, fx + fw + 6, fy + 12, MenuBackdrop.argb(329230, 0.85F * in));
      ctx.fill(fx - 6, fy + 12, fx + fw + 6, fy + 13, MenuBackdrop.argb(focused ? 16777215 : scol, (focused ? 0.9F : 0.5F) * in));
      this.nameField.render(ctx, mx, my, 0.0F);
      if (!focused && inside((double)mx, (double)my, fx - 6, fy - 4, fw + 12, 17)) {
         this.hover("name");
      }

      this.click(fx - 6, fy - 4, fw + 12, 17, () -> {
         if (!this.nameField.isFocused()) {
            this.focusName();
         }
      });
      if (focused) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, "Enter when you're done", (int)scx, fy + 16, MenuBackdrop.argb(4869724, in));
      }

      float cr = this.createdNanos == 0L ? 0.0F : clamp01(since(this.createdNanos) / 0.9F);
      MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY, scale * 4.2F, scale * 1.0F, scol, 0.55F * in);
      MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY, scale * 1.6F, scale * 0.35F, 16777215, 0.35F * in);
      MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY - scale * 1.1F, scale * 3.2F, scale * 3.6F, scol, (0.1F + 0.5F * cr) * in);

      for (int r = 0; r < 3; r++) {
         float q = (t * 0.3F + (float)r / 3.0F) % 1.0F;
         MenuBackdrop.tex(ctx, TEX_RING, scx, (float)footY, scale * (1.5F + q * 3.0F), scale * (0.35F + q * 0.7F), scol, 0.4F * (1.0F - q) * in);
      }

      this.figureYaw = this.approach(
         this.figureYaw, 14.0F + (float)Math.sin((double)(t * 0.6F)) * 12.0F + clamp(((float)mx - scx) / 12.0F, -14.0F, 14.0F), 4.0F
      );
      VoidMenuScreen.Member pv = new VoidMenuScreen.Member(-2, "Vessel", 1, soul, null, 20.0F, 20.0F);
      this.drawMember(ctx, pv, (int)scx, footY, scale, this.figureYaw, null, in);
      float sp = ease(clamp01(since(this.soulPickedNanos) / 0.35F));
      float hx = scx + scale * 1.2F;
      float hy = (float)footY - scale * 1.5F + (float)Math.sin((double)(t * 1.8F)) * 2.5F;
      int hs = Math.round(Math.max(10.0F, scale * 0.32F) * (0.6F + 0.4F * backOut(sp)));
      MenuBackdrop.tex(ctx, TEX_GLOW, hx, hy, (float)hs * 5.0F, (float)hs * 5.0F, scol, 0.5F * in);
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 300.0F);
      this.soulHeart(ctx, soul, hx - (float)hs / 2.0F, hy - (float)hs / 2.0F, hs, in);
      m.pop();
      String line = "A SOUL of " + soul.getDisplayName();
      ctx.drawCenteredTextWithShadow(this.textRenderer, line, (int)scx, footY + 7, MenuBackdrop.argb(lerpRgb(scol, 16777215, 0.45F), in));
      int bw = 108;
      int by = this.height - 10 - 22;
      boolean done = this.createdNanos != 0L;
      this.button(ctx, (int)scx - bw - 6, by, bw, 14, done ? "..." : "CREATE", mx, my, !done, in, this::create);
      this.button(ctx, (int)scx + 6, by, bw, 14, "CANCEL", mx, my, !done, in, () -> {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
         this.close();
      });
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, "Give them a skin from their Stats page once they're made", (int)scx, this.height - 9, MenuBackdrop.argb(4869724, in)
      );
      if (done) {
         if (cr < 1.0F) {
            MenuBackdrop.tex(ctx, TEX_GLOW, scx, (float)footY - scale, scale * 6.0F * (0.5F + cr), scale * 6.0F * (0.5F + cr), 16777215, 0.6F * (1.0F - cr));
            String welcome = "Welcome, " + (this.nameField.getText().isBlank() ? "Vessel" : this.nameField.getText().trim()) + ".";
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal(welcome).setStyle(Style.EMPTY.withBold(true)),
               (int)scx,
               (int)((float)footY - scale * 2.3F - 10.0F),
               MenuBackdrop.argb(16777215, Math.min(1.0F, cr * 3.0F))
            );
         } else if (this.client != null && !this.leaving) {
            this.leaving = true;
            this.client.execute(() -> this.client.setScreen(this.parent));
         }
      }
   }

   private void renderSouls(DrawContext ctx, int x, int y, int w, int mx, int my) {
      float t = this.secs();
      float a = ease(clamp01((t - 0.2F) / 0.4F));
      ctx.drawTextWithShadow(this.textRenderer, "SOUL", x + 2, y, MenuBackdrop.argb(9413560, a));
      ctx.fill(x + 6 + this.textRenderer.getWidth("SOUL"), y + 4, x + w, y + 5, MenuBackdrop.argb(tint(), 0.3F * a));
      int cell = 26;
      int gap = 5;
      int cols = Math.max(1, (w + gap) / (cell + gap));
      int gx = x + (w - (cols * (cell + gap) - gap)) / 2;
      int gy = y + 14;
      SoulType hovered = null;
      float hvx = 0.0F;
      float hvy = 0.0F;

      for (int i = 0; i < SOULS.length; i++) {
         SoulType s = SOULS[i];
         int cx = gx + i % cols * (cell + gap);
         int cy = gy + i / cols * (cell + gap);
         float sa = backOut(clamp01((t - 0.25F - (float)i * 0.04F) / 0.3F));
         if (!(sa <= 0.01F)) {
            boolean sel = i == this.selectedSoul;
            boolean hov = inside((double)mx, (double)my, cx, cy, cell, cell);
            if (hov) {
               this.hover("soul" + i);
               hovered = s;
               hvx = (float)cx + (float)cell / 2.0F;
               hvy = (float)cy;
            }

            int col = s.getColor() & 16777215;
            MenuBackdrop.tex(
               ctx,
               TEX_GLOW,
               (float)cx + (float)cell / 2.0F,
               (float)cy + (float)cell / 2.0F,
               (float)cell * 1.8F,
               (float)cell * 1.8F,
               col,
               (sel ? 0.6F : (hov ? 0.35F : 0.12F)) * Math.min(1.0F, sa)
            );
            ctx.fill(
               cx - 1,
               cy - 1,
               cx + cell + 1,
               cy + cell + 1,
               MenuBackdrop.argb(sel ? 16777215 : (hov ? 16777024 : lerpRgb(col, 3158080, 0.6F)), Math.min(1.0F, sa))
            );
            ctx.fill(cx, cy, cx + cell, cy + cell, MenuBackdrop.argb(lerpRgb(329230, col, sel ? 0.22F : 0.06F), 0.95F * Math.min(1.0F, sa)));
            if (sel) {
               float rq = t * 0.8F % 1.0F;
               MenuBackdrop.tex(
                  ctx,
                  TEX_RING,
                  (float)cx + (float)cell / 2.0F,
                  (float)cy + (float)cell / 2.0F,
                  (float)cell * (1.1F + rq * 0.8F),
                  (float)cell * (1.1F + rq * 0.8F),
                  col,
                  0.55F * (1.0F - rq)
               );
            }

            int hsz = Math.round(14.0F * Math.min(1.15F, sa) * (sel ? 1.0F + 0.08F * (float)Math.max(0.0, Math.sin((double)(t * 4.5F))) : 1.0F));
            this.soulHeart(
               ctx, s, (float)cx + (float)cell / 2.0F - (float)hsz / 2.0F, (float)cy + (float)cell / 2.0F - (float)hsz / 2.0F, hsz, Math.min(1.0F, sa)
            );
            int idx = i;
            this.click(
               cx,
               cy,
               cell,
               cell,
               () -> {
                  if (this.selectedSoul != idx) {
                     TurnBattleSounds.playClient(TurnBattleSounds.SND_MERCYADD, 0.7F, 0.9F + (float)idx * 0.05F);
                     this.selectedSoul = idx;
                     this.soulPickedNanos = System.nanoTime();
                     this.act(
                        new VoidMenuScreen.Member(-2, "Vessel", 1, SOULS[idx], null, 20.0F, 20.0F),
                        CloneAnimator.Act.SKILL,
                        SOULS[idx].getColor() & 16777215,
                        null
                     );
                  }
               }
            );
         }
      }

      int rows = (SOULS.length + cols - 1) / cols;
      int dy = gy + rows * (cell + gap) + 6;
      SoulType s = this.soul();
      int col = s.getColor() & 16777215;
      float pa = ease(clamp01(since(this.soulPickedNanos) / 0.3F)) * a;
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal(s.getDisplayName()).setStyle(Style.EMPTY.withBold(true)),
         x + 4,
         dy,
         MenuBackdrop.argb(lerpRgb(col, 16777215, 0.3F), pa)
      );
      dy += 12;
      String d = s.getDescription() != null ? s.getDescription() : "";

      for (OrderedText o : this.textRenderer.wrapLines(StringVisitable.plain(d), w - 8)) {
         if (dy > this.height - 40) {
            break;
         }

         ctx.drawTextWithShadow(this.textRenderer, o, x + 4, dy, MenuBackdrop.argb(9413560, pa));
         dy += 10;
      }

      if (hovered != null && hovered != s) {
         List lines = new ArrayList<>();
         lines.add(new String[]{hovered.getDisplayName(), Integer.toHexString(hovered.getColor() & 16777215)});
         this.hoverTag(hvx, hvy - 2.0F, lines, hovered.getColor() & 16777215);
      }
   }

   private void create() {
      if (this.createdNanos == 0L) {
         String name = this.nameField.getText().trim();
         if (name.isEmpty()) {
            name = "Vessel";
         }

         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeString(name, 32);
         buf.writeInt(PlayerClass.PSI_USER.ordinal());
         buf.writeInt(this.selectedSoul);
         Net121.sendToServer(TurnBattleMod.CREATE_VESSEL, buf);
         this.createdNanos = System.nanoTime();
         TurnBattleSounds.playClient(TurnBattleSounds.GREAT_SHINE, 0.8F, 1.0F);
         this.nameField.setFocused(false);
         int footY = this.height - 10 - 40 - 22;
         this.pulse((float)this.width / 2.0F, (float)(footY - 30), this.soul().getColor() & 16777215, 40.0F);
      }
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (this.nameField == null || !this.nameField.isFocused()) {
         return super.keyPressed(key, scan, mods);
      } else if (key != 257 && key != 335 && key != 258 && key != 256 && key != 264) {
         this.nameField.keyPressed(key, scan, mods);
         return true;
      } else {
         this.unfocusName();
         return true;
      }
   }
}
