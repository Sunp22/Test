package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class TitleMenuScreen extends Screen {
   public static boolean classic = false;
   private static final int VW = 320;
   private static final int VH = 240;
   private static final Identifier LOGO = Identifier.of("turnbattle", "textures/gui/soulboundlogo_title.png");
   private static final int LOGO_W = 568;
   private static final int LOGO_H = 101;
   private static final Identifier HEART = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private static final Identifier DR_FONT = Identifier.of("turnbattle", "damage_numbers");
   private static final Identifier GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   private final List<TitleMenuScreen.Choice> main = new ArrayList<>();
   private final List<TitleMenuScreen.Choice> small = new ArrayList<>();
   private long openedAt;
   private int row = 0;
   private int col = 0;
   private int hoverRow = -2;
   private int hoverCol = -2;
   private float heartX = -1.0F;
   private float heartY = -1.0F;
   private long lastFrame;
   private float scale = 1.0F;
   private float offX;
   private float offY;
   private String note;
   private long noteUntil;
   private static final int MAIN_Y = 118;
   private static final int MAIN_GAP = 17;
   private static final int SMALL_Y = 214;

   public TitleMenuScreen() {
      super(Text.literal("Soulbound"));
   }

   protected void init() {
      if (this.openedAt == 0L) {
         this.openedAt = Util.getMeasuringTimeMs();
      }

      float s = Math.min((float)this.width / 320.0F, (float)this.height / 240.0F);
      this.scale = Math.max(0.75F, (float)Math.floor((double)(s * 4.0F)) / 4.0F);
      this.offX = ((float)this.width - 320.0F * this.scale) / 2.0F;
      this.offY = ((float)this.height - 240.0F * this.scale) / 2.0F;
      this.main.clear();
      this.small.clear();
      this.main.add(new TitleMenuScreen.Choice("Singleplayer", () -> this.client.setScreen(new SelectWorldScreen(this))));
      this.main.add(new TitleMenuScreen.Choice("Multiplayer", () -> {
         if (!this.client.isMultiplayerEnabled()) {
            this.buzz("Multiplayer is turned off for this account.");
         } else {
            this.client.setScreen((Screen)(this.client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this)));
         }
      }));
      if (FabricLoader.getInstance().isModLoaded("modmenu")) {
         this.main.add(new TitleMenuScreen.Choice("Mods", this::openModMenu));
      }

      this.main.add(new TitleMenuScreen.Choice("Options", () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
      this.main.add(new TitleMenuScreen.Choice("Quit", () -> this.client.scheduleStop()));
      this.small.add(new TitleMenuScreen.Choice("Realms", () -> this.client.setScreen(new RealmsMainScreen(this))));
      this.small
         .add(
            new TitleMenuScreen.Choice(
               "Language", () -> this.client.setScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager()))
            )
         );
      this.small.add(new TitleMenuScreen.Choice("Accessibility", () -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options))));
      this.small.add(new TitleMenuScreen.Choice("Classic", () -> {
         classic = true;
         this.client.setScreen(new TitleScreen());
      }));
      this.row = Math.min(this.row, this.main.size());
   }

   private void openModMenu() {
      try {
         Class c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
         this.client.setScreen((Screen)c.getConstructor(Screen.class).newInstance(this));
      } catch (Throwable var2) {
         this.buzz("Couldn't open the mod list.");
      }
   }

   private int mainY(int i) {
      return 118 + i * 17;
   }

   private int smallX(int i) {
      int total = 0;

      for (TitleMenuScreen.Choice c : this.small) {
         total += this.textRenderer.getWidth(c.label());
      }

      total += (this.small.size() - 1) * 22;
      int x = 160 - total / 2;

      for (int k = 0; k < i; k++) {
         x += this.textRenderer.getWidth(this.small.get(k).label()) + 22;
      }

      return x;
   }

   private static Text dr(String s) {
      return Text.literal(s).setStyle(Style.EMPTY.withFont(DR_FONT));
   }

   private float secs() {
      return (float)(Util.getMeasuringTimeMs() - this.openedAt) / 1000.0F;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      float t = this.secs();
      long now = System.nanoTime();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      MenuBackdrop.render(ctx, this.width, this.height, 7321855, ease(clamp01(t / 1.4F)));
      this.updateHover(this.vx((double)mx), this.vy((double)my));
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(this.offX, this.offY, 0.0F);
      m.scale(this.scale, this.scale, 1.0F);
      float la = ease(clamp01((t - 0.35F) / 0.9F));
      float lw = 268.0F;
      float lh = lw * 101.0F / 568.0F;
      float lx = 160.0F - lw / 2.0F;
      float ly = 30.0F + (1.0F - la) * 10.0F + (float)Math.sin((double)(t * 0.9F)) * 1.2F;
      float flash = clamp01(1.0F - (t - 0.35F) / 0.7F) * clamp01((t - 0.2F) / 0.15F);
      float breathe = 0.5F + 0.5F * (float)Math.sin((double)(t * 1.3F));
      MenuBackdrop.tex(ctx, GLOW, 160.0F, ly + lh / 2.0F, lw * 1.25F, lh * 3.2F, 11563263, (0.22F + 0.08F * breathe) * la);
      MenuBackdrop.tex(ctx, GLOW, 160.0F, ly + lh / 2.0F, lw * 1.5F, lh * 4.0F, 16777215, 0.9F * flash);
      if (la > 0.01F) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, la);
         ctx.drawTexture(LOGO, Math.round(lx), Math.round(ly), Math.round(lw), Math.round(lh), 0.0F, 0.0F, 568, 101, 568, 101);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         float g = t * 0.22F % 1.0F;
         if (g < 0.35F) {
            float gx = lx - 20.0F + (lw + 40.0F) * (g / 0.35F);
            ctx.enableScissor(
               Math.round(this.offX + lx * this.scale),
               Math.round(this.offY + ly * this.scale),
               Math.round(this.offX + (lx + lw) * this.scale),
               Math.round(this.offY + (ly + lh) * this.scale)
            );

            for (int k = 0; k < 6; k++) {
               ctx.fill(
                  Math.round(gx + (float)k),
                  Math.round(ly),
                  Math.round(gx + (float)k + 1.0F),
                  Math.round(ly + lh),
                  MenuBackdrop.argb(16777215, 0.1F * (1.0F - Math.abs((float)k - 2.5F) / 3.0F) * la)
               );
            }

            ctx.disableScissor();
         }
      }

      float ca = ease(clamp01((t - 1.2F) / 0.5F));

      for (int i = 0; i < this.main.size(); i++) {
         float a = ease(clamp01((t - 1.2F - (float)i * 0.08F) / 0.4F));
         boolean sel = this.row == i;
         Text lab = dr(this.main.get(i).label());
         int w = this.textRenderer.getWidth(lab);
         int y = this.mainY(i) + (int)((1.0F - a) * 6.0F);
         ctx.drawText(this.textRenderer, lab, 160 - w / 2, y, MenuBackdrop.argb(sel ? 16777024 : 16777215, a), true);
      }

      float ba = ease(clamp01((t - 1.6F) / 0.5F));

      for (int i = 0; i < this.small.size(); i++) {
         boolean sel = this.row == this.main.size() && this.col == i;
         ctx.drawText(this.textRenderer, this.small.get(i).label(), this.smallX(i), 214, MenuBackdrop.argb(sel ? 16777024 : 9212838, ba), true);
      }

      if (this.note != null && Util.getMeasuringTimeMs() < this.noteUntil) {
         ctx.drawText(this.textRenderer, this.note, 160 - this.textRenderer.getWidth(this.note) / 2, 198, -34970, true);
      }

      float[] target = this.heartTarget();
      if (this.heartX < 0.0F) {
         this.heartX = target[0];
         this.heartY = target[1];
      }

      float k = 1.0F - (float)Math.exp((double)(-dt * 18.0F));
      this.heartX = this.heartX + (target[0] - this.heartX) * k;
      this.heartY = this.heartY + (target[1] - this.heartY) * k;
      if (ca > 0.01F) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ca);
         ctx.drawTexture(HEART, Math.round(this.heartX), Math.round(this.heartY), 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      m.pop();
      float pa = ease(clamp01((t - 1.8F) / 0.6F));
      String ver = "Minecraft " + SharedConstants.getGameVersion().getName() + "  \u00b7  Soulbound " + modVersion();
      ctx.drawTextWithShadow(this.textRenderer, ver, 3, this.height - 11, MenuBackdrop.argb(6975616, pa));
      String cr = "Copyright Mojang AB. Do not distribute!";
      ctx.drawTextWithShadow(this.textRenderer, cr, this.width - 3 - this.textRenderer.getWidth(cr), this.height - 11, MenuBackdrop.argb(6975616, pa));
   }

   private static String modVersion() {
      return FabricLoader.getInstance().getModContainer("turnbattle").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
   }

   private float[] heartTarget() {
      if (this.row < this.main.size()) {
         int w = this.textRenderer.getWidth(dr(this.main.get(this.row).label()));
         return new float[]{160.0F - (float)w / 2.0F - 13.0F, (float)(this.mainY(this.row) + 1)};
      } else {
         return new float[]{(float)(this.smallX(this.col) - 11), 214.0F};
      }
   }

   private float vx(double mx) {
      return (float)((mx - (double)this.offX) / (double)this.scale);
   }

   private float vy(double my) {
      return (float)((my - (double)this.offY) / (double)this.scale);
   }

   private void updateHover(float x, float y) {
      int hr = -2;
      int hc = -2;

      for (int i = 0; i < this.main.size(); i++) {
         int w = this.textRenderer.getWidth(dr(this.main.get(i).label()));
         if (x >= 160.0F - (float)w / 2.0F - 14.0F && x < 160.0F + (float)w / 2.0F + 4.0F && y >= (float)(this.mainY(i) - 3) && y < (float)(this.mainY(i) + 13)
            )
          {
            hr = i;
         }
      }

      for (int ix = 0; ix < this.small.size(); ix++) {
         int w = this.textRenderer.getWidth(this.small.get(ix).label());
         if (x >= (float)(this.smallX(ix) - 12) && x < (float)(this.smallX(ix) + w + 4) && y >= 211.0F && y < 225.0F) {
            hr = this.main.size();
            hc = ix;
         }
      }

      if (hr != this.hoverRow || hc != this.hoverCol) {
         this.hoverRow = hr;
         this.hoverCol = hc;
         if (hr == -2) {
            return;
         }

         if (hr != this.row || hc >= 0 && hc != this.col) {
            this.row = hr;
            if (hc >= 0) {
               this.col = hc;
            }

            TurnBattleSounds.playHover();
         }
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0) {
         return super.mouseClicked(mx, my, button);
      } else {
         this.updateHover(this.vx(mx), this.vy(my));
         if (this.hoverRow != -2 && !(this.secs() < 1.2F)) {
            this.confirm();
            return true;
         } else {
            return true;
         }
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      switch (key) {
         case 32:
         case 90:
         case 257:
         case 335:
            if (this.secs() < 0.4F) {
               this.openedAt -= 1200L;
               return true;
            }

            this.confirm();
            return true;
         case 65:
         case 263:
            this.side(-1);
            return true;
         case 68:
         case 262:
            this.side(1);
            return true;
         case 83:
         case 264:
            this.move(1);
            return true;
         case 87:
         case 265:
            this.move(-1);
            return true;
         default:
            return super.keyPressed(key, scan, mods);
      }
   }

   private void move(int d) {
      this.row = Math.floorMod(this.row + d, this.main.size() + 1);
      TurnBattleSounds.playHover();
   }

   private void side(int d) {
      if (this.row == this.main.size()) {
         this.col = Math.floorMod(this.col + d, this.small.size());
         TurnBattleSounds.playHover();
      }
   }

   private void confirm() {
      if (this.client != null) {
         TurnBattleSounds.playSelect();
         TitleMenuScreen.Choice c = this.row < this.main.size() ? this.main.get(this.row) : this.small.get(this.col);
         c.action().run();
      }
   }

   private void buzz(String s) {
      TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
      this.note = s;
      this.noteUntil = Util.getMeasuringTimeMs() + 2600L;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean shouldPause() {
      return false;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }

   private static float ease(float t) {
      float u = 1.0F - clamp01(t);
      return 1.0F - u * u * u;
   }

   private static record Choice(String label, Runnable action) {
   }
}
