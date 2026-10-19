package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;

@Environment(EnvType.CLIENT)
public class WorldNameScreen extends Screen {
   private static final int VW = 320;
   private static final int VH = 240;
   private static final int MAX_LEN = 32;
   private static final Identifier HEART = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private static final String[] GRID = new String[]{"ABCDEFG", "HIJKLMN", "OPQRSTU", "VWXYZ  ", "abcdefg", "hijklmn", "opqrstu", "vwxyz  "};
   private static final int GX = 92;
   private static final int GY = 82;
   private static final int CW = 20;
   private static final int RH = 14;
   private static final int LOWER_GAP = 6;
   private static final String[] NAME_BTNS = new String[]{"Back", "Erase", "Done"};
   private static final int[] NAME_BTN_X = new int[]{84, 146, 214};
   private static final int BTN_Y = 214;
   private final Screen parent;
   private WorldNameScreen.Step step = WorldNameScreen.Step.NAME;
   private long stepAt;
   private long openedAt;
   private long lastTickNanos;
   private float scale = 1.0F;
   private float offX;
   private float offY;
   private String name = "";
   private int gr;
   private int gc;
   private int yes = 1;
   private int opt;
   private int modeIdx;
   private Difficulty difficulty = Difficulty.NORMAL;
   private boolean cheats;
   private String seed = "";
   private static final String[] OPT_LABELS = new String[]{"MODE", "DIFFICULTY", "COMMANDS", "SEED", "More options...", "BEGIN"};
   private static final String[] MODES = new String[]{"Survival", "Hardcore", "Creative"};
   private float heartX = -1.0F;
   private float heartY;
   private float prevHeartX;
   private float prevHeartY;
   private int hoverKey = Integer.MIN_VALUE;

   public WorldNameScreen(Screen parent) {
      super(Text.literal("Name the world"));
      this.parent = parent;
   }

   protected void init() {
      if (this.openedAt == 0L) {
         this.openedAt = Util.getMeasuringTimeMs();
         this.stepAt = this.openedAt;
      }

      float s = Math.min((float)this.width / 320.0F, (float)this.height / 240.0F);
      this.scale = Math.max(0.75F, (float)Math.floor((double)(s * 4.0F)) / 4.0F);
      this.offX = ((float)this.width - 320.0F * this.scale) / 2.0F;
      this.offY = ((float)this.height - 240.0F * this.scale) / 2.0F;
   }

   private void go(WorldNameScreen.Step s) {
      this.step = s;
      this.stepAt = Util.getMeasuringTimeMs();
      this.heartX = -1.0F;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      MenuBackdrop.render(
         ctx, this.width, this.height, this.step == WorldNameScreen.Step.CONFIRM ? 16777215 : 7321855, this.step == WorldNameScreen.Step.CONFIRM ? 0.5F : 1.0F
      );
      if (this.step == WorldNameScreen.Step.CONFIRM) {
         ctx.fill(0, 0, this.width, this.height, argb(0, 0.45F * this.fade(0, 400)));
      }

      float td = Math.min(1.0F, (float)(System.nanoTime() - this.lastTickNanos) / 5.0E7F);
      this.hover(this.vx((double)mx), this.vy((double)my));
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(this.offX, this.offY, 0.0F);
      m.scale(this.scale, this.scale, 1.0F);
      switch (this.step) {
         case NAME:
            this.drawName(ctx);
            break;
         case CONFIRM:
            this.drawConfirm(ctx);
            break;
         case OPTIONS:
            this.drawOptions(ctx);
      }

      float[] h = this.heartTarget();
      if (h != null) {
         if (this.heartX < 0.0F) {
            this.heartX = this.prevHeartX = h[0];
            this.heartY = this.prevHeartY = h[1];
         }

         float hx = this.prevHeartX + (this.heartX - this.prevHeartX) * td;
         float hy = this.prevHeartY + (this.heartY - this.prevHeartY) * td;
         RenderSystem.enableBlend();
         ctx.drawTexture(HEART, Math.round(hx), Math.round(hy), 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
      }

      m.pop();
   }

   private float fade(int delayMs, int durMs) {
      return ease(clamp01((float)(Util.getMeasuringTimeMs() - this.stepAt - (long)delayMs) / (float)durMs));
   }

   private void drawName(DrawContext ctx) {
      float a = this.fade(0, 350);
      ctx.drawText(this.textRenderer, "Name the world.", 104, 22, argb(16777215, a), true);
      String shown = this.name + (Util.getMeasuringTimeMs() / 400L % 2L == 0L && this.name.length() < 32 ? "_" : " ");
      float ns = (float)this.textRenderer.getWidth(shown) * 2.0F > 280.0F ? 280.0F / (float)this.textRenderer.getWidth(shown) : 2.0F;
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(160.0F - (float)this.textRenderer.getWidth(shown) * ns / 2.0F, 44.0F, 0.0F);
      m.scale(ns, ns, 1.0F);
      ctx.drawText(this.textRenderer, shown, 0, 0, argb(16777215, a), true);
      m.pop();

      for (int r = 0; r < GRID.length; r++) {
         float ra = this.fade(80 + r * 35, 300);

         for (int c = 0; c < 7; c++) {
            char ch = GRID[r].charAt(c);
            if (ch != ' ') {
               boolean sel = this.gr == r && this.gc == c;
               float jitter = sel ? (float)Math.sin((double)(MenuBackdrop.time() * 9.0F)) * 0.6F : 0.0F;
               ctx.drawText(
                  this.textRenderer, String.valueOf(ch), this.gridX(c), (int)((float)this.gridY(r) + jitter), argb(sel ? 16777024 : 16777215, ra), true
               );
            }
         }
      }

      for (int b = 0; b < NAME_BTNS.length; b++) {
         boolean sel = this.gr == 8 && this.gc == b;
         boolean off = b == 2 && this.name.isBlank();
         ctx.drawText(this.textRenderer, NAME_BTNS[b], NAME_BTN_X[b], 214, argb(off ? 6710886 : (sel ? 16777024 : 16777215), this.fade(350, 300)), true);
      }
   }

   private void drawConfirm(DrawContext ctx) {
      float grow = clamp01((float)(Util.getMeasuringTimeMs() - this.stepAt) / 4000.0F);
      float ns = 2.0F + grow * 2.2F;
      float maxS = 290.0F / (float)Math.max(1, this.textRenderer.getWidth(this.name));
      ns = Math.min(ns, maxS);
      float sx = (float)(Math.sin((double)(MenuBackdrop.time() * 37.0F)) * 0.5);
      float sy = (float)(Math.cos((double)(MenuBackdrop.time() * 29.0F)) * 0.5);
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(160.0F - (float)this.textRenderer.getWidth(this.name) * ns / 2.0F + sx, 100.0F - 4.5F * ns + sy, 0.0F);
      m.scale(ns, ns, 1.0F);
      ctx.drawText(this.textRenderer, this.name, 0, 0, -1, true);
      m.pop();
      float a = this.fade(600, 500);
      ctx.drawText(this.textRenderer, "Is this name correct?", 160 - this.textRenderer.getWidth("Is this name correct?") / 2, 160, argb(16777215, a), true);
      ctx.drawText(this.textRenderer, "No", 110, 192, argb(this.yes == 0 ? 16777024 : 16777215, a), true);
      ctx.drawText(this.textRenderer, "Yes", 196, 192, argb(this.yes == 1 ? 16777024 : 16777215, a), true);
   }

   private void drawOptions(DrawContext ctx) {
      float a = this.fade(0, 350);
      ctx.drawText(this.textRenderer, "Set up \"" + this.trim(this.name, 160) + "\".", 40, 26, argb(16777215, a), true);

      for (int i = 0; i < OPT_LABELS.length; i++) {
         float ra = this.fade(60 + i * 45, 300);
         int y = this.optY(i);
         boolean sel = this.opt == i;
         int lc = sel ? 16777024 : 16777215;
         int x0 = 56 + (int)((1.0F - ra) * -16.0F);
         ctx.drawText(this.textRenderer, OPT_LABELS[i], x0, y, argb(lc, ra), true);
         String val = this.valueText(i);
         if (val != null) {
            boolean locked = this.locked(i);
            int vc = locked ? 7829367 : (sel ? 16777215 : 12109526);
            String shown = (sel && !locked && i != 3 ? "\u25c0 " : "") + val + (sel && !locked && i != 3 ? " \u25b6" : "");
            ctx.drawText(this.textRenderer, shown, 270 - this.textRenderer.getWidth(shown), y, argb(vc, ra), true);
         }
      }
      String hint = switch (this.opt) {
         case 0 -> this.modeIdx == 1
         ? "One life. The world ends with you."
         : (this.modeIdx == 2 ? "Build freely - you can't be hurt." : "Gather, build and survive.");
         case 1 -> "How hard the world fights back.";
         case 2 -> "Lets you use commands like /gamemode.";
         case 3 -> "Type a seed, or leave it blank for a random world.";
         case 4 -> "World type, structures, data packs and more.";
         default -> "Begin your story.";
      };
      ctx.drawText(this.textRenderer, hint, 160 - this.textRenderer.getWidth(hint) / 2, 206, argb(9413560, this.fade(350, 300)), true);
   }

   private int optY(int i) {
      return 58 + i * 22 + (i >= 4 ? 8 : 0);
   }

   private String valueText(int i) {
      return switch (i) {
         case 0 -> MODES[this.modeIdx];
         case 1 -> this.modeIdx == 1 ? "Hard" : cap(this.difficulty.getName());
         case 2 -> this.modeIdx == 1 ? "Off" : (this.cheats ? "On" : "Off");
         case 3 -> this.seed.isEmpty()
         ? (this.opt == 3 && this.blink() ? "_" : "Random")
         : this.trim(this.seed, 120) + (this.opt == 3 && this.blink() ? "_" : "");
         default -> null;
      };
   }

   private boolean blink() {
      return Util.getMeasuringTimeMs() / 400L % 2L == 0L;
   }

   private boolean locked(int i) {
      return this.modeIdx == 1 && (i == 1 || i == 2);
   }

   private static String cap(String s) {
      return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
   }

   private String trim(String s, int max) {
      return this.textRenderer.getWidth(s) <= max ? s : this.textRenderer.trimToWidth(s, Math.max(10, max - 6)) + "\u2026";
   }

   private int gridX(int c) {
      return 92 + c * 20;
   }

   private int gridY(int r) {
      return 82 + r * 14 + (r >= 4 ? 6 : 0);
   }

   private float[] heartTarget() {
      return switch (this.step) {
         case NAME -> this.gr == 8
         ? new float[]{(float)(NAME_BTN_X[this.gc] - 11), 214.0F}
         : new float[]{(float)(this.gridX(this.gc) - 10), (float)this.gridY(this.gr)};
         case CONFIRM -> this.fade(600, 500) > 0.5F ? new float[]{(float)((this.yes == 0 ? 110 : 196) - 11), 192.0F} : null;
         case OPTIONS -> new float[]{43.0F, (float)this.optY(this.opt)};
      };
   }

   public void tick() {
      this.lastTickNanos = System.nanoTime();
      float[] t = this.heartTarget();
      this.prevHeartX = this.heartX;
      this.prevHeartY = this.heartY;
      if (t != null && this.heartX >= 0.0F) {
         this.heartX = this.heartX + (t[0] - this.heartX) * 0.6F;
         this.heartY = this.heartY + (t[1] - this.heartY) * 0.6F;
      }
   }

   private float vx(double mx) {
      return (float)((mx - (double)this.offX) / (double)this.scale);
   }

   private float vy(double my) {
      return (float)((my - (double)this.offY) / (double)this.scale);
   }

   private void hover(float x, float y) {
      int key = Integer.MIN_VALUE;
      switch (this.step) {
         case NAME:
            for (int r = 0; r < GRID.length; r++) {
               for (int c = 0; c < 7; c++) {
                  if (GRID[r].charAt(c) != ' '
                     && x >= (float)(this.gridX(c) - 4)
                     && x < (float)(this.gridX(c) + 12)
                     && y >= (float)(this.gridY(r) - 3)
                     && y < (float)(this.gridY(r) + 11)) {
                     key = r * 100 + c;
                  }
               }
            }

            for (int b = 0; b < NAME_BTNS.length; b++) {
               if (x >= (float)(NAME_BTN_X[b] - 4) && x < (float)(NAME_BTN_X[b] + this.textRenderer.getWidth(NAME_BTNS[b]) + 4) && y >= 211.0F && y < 225.0F) {
                  key = 800 + b;
               }
            }
            break;
         case CONFIRM:
            if (y >= 188.0F && y < 204.0F) {
               if (x >= 100.0F && x < 140.0F) {
                  key = 0;
               } else if (x >= 186.0F && x < 226.0F) {
                  key = 1;
               }
            }
            break;
         case OPTIONS:
            for (int i = 0; i < OPT_LABELS.length; i++) {
               if (x >= 40.0F && x < 280.0F && y >= (float)(this.optY(i) - 4) && y < (float)(this.optY(i) + 12)) {
                  key = i;
               }
            }
      }

      if (key != this.hoverKey) {
         this.hoverKey = key;
         if (key != Integer.MIN_VALUE) {
            switch (this.step) {
               case NAME:
                  this.gr = key / 100;
                  this.gc = key % 100;
                  break;
               case CONFIRM:
                  this.yes = key;
                  break;
               case OPTIONS:
                  this.opt = key;
            }

            TurnBattleSounds.playHover();
         }
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 1) {
         this.back();
         return true;
      } else if (button != 0) {
         return true;
      } else {
         this.hoverKey = Integer.MIN_VALUE;
         this.hover(this.vx(mx), this.vy(my));
         if (this.hoverKey == Integer.MIN_VALUE) {
            return true;
         } else if (this.step == WorldNameScreen.Step.OPTIONS && this.opt <= 2) {
            this.change(this.vx(mx) < 200.0F ? -1 : 1);
            return true;
         } else {
            this.confirm();
            return true;
         }
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.step == WorldNameScreen.Step.NAME) {
         if (chr >= ' ' && chr != 127 && this.name.length() < 32 && validChar(chr)) {
            this.name = this.name + chr;
            TurnBattleSounds.playHover();
         }

         return true;
      } else if (this.step == WorldNameScreen.Step.OPTIONS && this.opt == 3) {
         if (chr >= ' ' && chr != 127 && this.seed.length() < 64) {
            this.seed = this.seed + chr;
         }

         return true;
      } else {
         return super.charTyped(chr, modifiers);
      }
   }

   private static boolean validChar(char c) {
      return c != '/' && c != '\\' && c != ':' && c != '*' && c != '?' && c != '"' && c != '<' && c != '>' && c != '|';
   }

   public boolean keyPressed(int key, int scan, int mods) {
      switch (this.step) {
         case NAME:
            switch (key) {
               case 256:
                  this.back();
                  break;
               case 257:
               case 335:
                  if (this.gr == 8) {
                     this.confirm();
                  } else if (!this.name.isBlank()) {
                     this.gr = 8;
                     this.gc = 2;
                     this.confirm();
                  }
                  break;
               case 259:
                  this.erase();
                  break;
               case 262:
                  this.moveGrid(0, 1);
                  break;
               case 263:
                  this.moveGrid(0, -1);
                  break;
               case 264:
                  this.moveGrid(1, 0);
                  break;
               case 265:
                  this.moveGrid(-1, 0);
                  break;
               default:
                  return super.keyPressed(key, scan, mods);
            }

            return true;
         case CONFIRM:
            switch (key) {
               case 32:
               case 90:
               case 257:
               case 335:
                  this.confirm();
                  break;
               case 65:
               case 68:
               case 262:
               case 263:
                  this.yes = 1 - this.yes;
                  TurnBattleSounds.playHover();
                  break;
               case 88:
               case 256:
               case 259:
               case 340:
                  this.back();
                  break;
               default:
                  return super.keyPressed(key, scan, mods);
            }

            return true;
         default:
            boolean typingSeed = this.opt == 3;
            switch (key) {
               case 256:
                  this.back();
                  break;
               case 257:
               case 335:
                  this.confirm();
                  break;
               case 259:
                  if (typingSeed && !this.seed.isEmpty()) {
                     this.seed = this.seed.substring(0, this.seed.length() - 1);
                  } else if (!typingSeed) {
                     this.back();
                  }
                  break;
               case 262:
                  this.change(1);
                  break;
               case 263:
                  this.change(-1);
                  break;
               case 264:
                  this.opt = Math.floorMod(this.opt + 1, OPT_LABELS.length);
                  TurnBattleSounds.playHover();
                  break;
               case 265:
                  this.opt = Math.floorMod(this.opt - 1, OPT_LABELS.length);
                  TurnBattleSounds.playHover();
                  break;
               default:
                  if (!typingSeed) {
                     if (key == 87) {
                        this.opt = Math.floorMod(this.opt - 1, OPT_LABELS.length);
                        TurnBattleSounds.playHover();
                        return true;
                     }

                     if (key == 83) {
                        this.opt = Math.floorMod(this.opt + 1, OPT_LABELS.length);
                        TurnBattleSounds.playHover();
                        return true;
                     }

                     if (key == 65) {
                        this.change(-1);
                        return true;
                     }

                     if (key == 68) {
                        this.change(1);
                        return true;
                     }

                     if (key == 90) {
                        this.confirm();
                        return true;
                     }

                     if (key == 88) {
                        this.back();
                        return true;
                     }
                  }

                  return super.keyPressed(key, scan, mods);
            }

            return true;
      }
   }

   private void moveGrid(int dr, int dc) {
      int r = this.gr;
      int c = this.gc;
      if (dr != 0) {
         if (r == 8) {
            r = dr > 0 ? 0 : GRID.length - 1;
            c = Math.min(c * 3, 6);
         } else {
            r += dr;
            if (r < 0 || r >= GRID.length) {
               r = 8;
               c = Math.min(c / 3, NAME_BTNS.length - 1);
            }
         }
      } else if (r == 8) {
         c = Math.floorMod(c + dc, NAME_BTNS.length);
      } else {
         c = Math.floorMod(c + dc, 7);
      }

      if (r != 8) {
         while (GRID[r].charAt(c) == ' ') {
            c = dc != 0 ? Math.floorMod(c + (dc == 0 ? -1 : dc), 7) : c - 1;
         }
      }

      this.gr = r;
      this.gc = c;
      TurnBattleSounds.playHover();
   }

   private void erase() {
      if (!this.name.isEmpty()) {
         this.name = this.name.substring(0, this.name.length() - 1);
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 0.9F);
      }
   }

   private void change(int d) {
      if (this.step == WorldNameScreen.Step.OPTIONS && !this.locked(this.opt)) {
         switch (this.opt) {
            case 0:
               this.modeIdx = Math.floorMod(this.modeIdx + d, MODES.length);
               break;
            case 1:
               this.difficulty = Difficulty.byId(Math.floorMod(this.difficulty.getId() + d, 4));
               break;
            case 2:
               this.cheats = !this.cheats;
               break;
            default:
               return;
         }

         TurnBattleSounds.playHover();
      }
   }

   private void confirm() {
      switch (this.step) {
         case NAME:
            if (this.gr == 8) {
               switch (this.gc) {
                  case 0:
                     this.back();
                     break;
                  case 1:
                     this.erase();
                     break;
                  default:
                     if (this.name.isBlank()) {
                        TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
                        return;
                     }

                     this.name = this.name.trim();
                     TurnBattleSounds.playSelect();
                     this.yes = 1;
                     this.go(WorldNameScreen.Step.CONFIRM);
               }

               return;
            }

            char ch = GRID[this.gr].charAt(this.gc);
            if (ch != ' ' && this.name.length() < 32) {
               this.name = this.name + ch;
               TurnBattleSounds.playSelect();
            }
            break;
         case CONFIRM:
            if (this.fade(600, 500) < 0.5F) {
               return;
            }

            TurnBattleSounds.playSelect();
            if (this.yes == 0) {
               this.go(WorldNameScreen.Step.NAME);
            } else {
               this.opt = 5;
               this.go(WorldNameScreen.Step.OPTIONS);
            }
            break;
         case OPTIONS:
            switch (this.opt) {
               case 0:
               case 1:
               case 2:
                  this.change(1);
               case 3:
               default:
                  break;
               case 4:
                  TurnBattleSounds.playSelect();
                  this.begin(false);
                  break;
               case 5:
                  TurnBattleSounds.playSelect();
                  this.begin(true);
            }
      }
   }

   private void back() {
      TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
      switch (this.step) {
         case NAME:
            this.close();
            break;
         case CONFIRM:
            this.go(WorldNameScreen.Step.NAME);
            break;
         case OPTIONS:
            this.go(WorldNameScreen.Step.CONFIRM);
      }
   }

   private void begin(boolean start) {
      WorldSetup.pending = new WorldSetup.Pending(this.name, this.modeIdx, this.difficulty, this.cheats, this.seed.trim(), start);
      CreateWorldScreen.create(this.client, this.parent);
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.parent);
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }

   private static float ease(float t) {
      float u = 1.0F - clamp01(t);
      return 1.0F - u * u * u;
   }

   private static int argb(int rgb, float a) {
      return MenuBackdrop.argb(rgb, a);
   }

   private static enum Step {
      NAME,
      CONFIRM,
      OPTIONS;
   }
}
