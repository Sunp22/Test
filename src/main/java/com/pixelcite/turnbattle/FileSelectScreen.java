package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelStorage.Session;

@Environment(EnvType.CLIENT)
public class FileSelectScreen extends Screen {
   public static boolean classicMode;
   private static final int VW = 320;
   private static final int VH = 240;
   private static final int SLOT_X = 46;
   private static final int SLOT_W = 236;
   private static final int SLOT_H = 44;
   private static final int SLOT_Y0 = 50;
   private static final int SLOT_GAP = 8;
   private static final int PER_PAGE = 3;
   private static final int BTN_Y = 214;
   private static final String[] BUTTONS = new String[]{"New", "Copy", "Erase", "Classic", "Back"};
   private static final int[] BTN_X = new int[]{44, 96, 150, 208, 268};
   private static final Identifier HEART = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private final Screen parent;
   private final List<LevelSummary> worlds = new ArrayList<>();
   private boolean loading = true;
   private String loadError;
   private FileSelectScreen.Mode mode = FileSelectScreen.Mode.NORMAL;
   private int page;
   private int row;
   private int col;
   private int chosenSlot = -1;
   private String note;
   private long noteUntil;
   private long openedAt;
   private float heartX = -1.0F;
   private float heartY = -1.0F;
   private float prevHeartX;
   private float prevHeartY;
   private long lastTickNanos;
   private float scale = 1.0F;
   private float offX;
   private float offY;
   private int hoverRow = -2;
   private int hoverCol = -2;
   private static final Map<String, FileSelectScreen.SaveInfo> INFO = new HashMap<>();
   private static final Map<String, Long> INFO_MTIME = new HashMap<>();

   public FileSelectScreen(Screen parent) {
      super(Text.literal("Select a file"));
      this.parent = parent;
   }

   protected void init() {
      if (this.openedAt == 0L) {
         this.openedAt = Util.getMeasuringTimeMs();
         this.reload();
      }

      this.computeLayout();
   }

   private void reload() {
      this.loading = true;
      this.loadError = null;
      if (this.client != null) {
         LevelStorage storage = this.client.getLevelStorage();

         CompletableFuture f;
         try {
            f = storage.loadSummaries(storage.getLevelList());
         } catch (Exception var4) {
            this.loading = false;
            this.loadError = "Couldn't read your saves.";
            return;
         }

         f.whenCompleteAsync((list, err) -> {
            this.worlds.clear();
            if (err == null && list != null) {
               for (LevelSummary s : list) {
                  if (s != null) {
                     this.worlds.add(s);
                  }
               }

               this.worlds.sort(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed());
            } else {
               this.loadError = "Couldn't read your saves.";
            }

            this.loading = false;
            this.page = Math.min(this.page, this.pageCount() - 1);
         }, this.client);
      }
   }

   private int entryCount() {
      return this.worlds.size() + 1;
   }

   private int pageCount() {
      return Math.max(1, (this.entryCount() + 3 - 1) / 3);
   }

   private LevelSummary worldAt(int slot) {
      int i = this.page * 3 + slot;
      return i >= 0 && i < this.worlds.size() ? this.worlds.get(i) : null;
   }

   private boolean isNewSlot(int slot) {
      return this.page * 3 + slot == this.worlds.size();
   }

   private boolean slotExists(int slot) {
      return this.page * 3 + slot < this.entryCount();
   }

   private FileSelectScreen.SaveInfo infoFor(LevelSummary s) {
      String dir = s.getName();

      try {
         Path df = this.client.getLevelStorage().getSavesDirectory().resolve(dir).resolve("data").resolve("turnbattle_data.dat");
         long mtime = Files.exists(df) ? Files.getLastModifiedTime(df).toMillis() : 0L;
         Long was = INFO_MTIME.get(dir);
         if (was != null && was == mtime && INFO.containsKey(dir)) {
            return INFO.get(dir);
         } else {
            FileSelectScreen.SaveInfo si = readInfo(df);
            INFO.put(dir, si);
            INFO_MTIME.put(dir, mtime);
            return si;
         }
      } catch (Exception var8) {
         return INFO.getOrDefault(dir, new FileSelectScreen.SaveInfo("", 0, -1L, ""));
      }
   }

   private static FileSelectScreen.SaveInfo readInfo(Path df) {
      String vessel = "";
      String biome = "";
      int lv = 0;
      long play = -1L;

      try {
         if (!Files.exists(df)) {
            return new FileSelectScreen.SaveInfo(vessel, lv, play, biome);
         }

         NbtCompound root = NbtIo.readCompressed(df, NbtSizeTracker.ofUnlimitedBytes());
         NbtCompound data = root.contains("data") ? root.getCompound("data") : root;
         if (data.contains("players")) {
            NbtList list = data.getList("players", 10);
            if (!list.isEmpty()) {
               NbtCompound p = list.getCompound(0);
               vessel = p.getString("vesselName");
               lv = p.contains("savedLevel") ? p.getInt("savedLevel") : 1;
               play = p.contains("savedPlaytimeSec") ? p.getLong("savedPlaytimeSec") : -1L;
               biome = p.getString("lastBiome");
            }
         }
      } catch (Exception var10) {
      }

      return new FileSelectScreen.SaveInfo(vessel, lv, play, biome);
   }

   private void computeLayout() {
      float s = Math.min((float)this.width / 320.0F, (float)this.height / 240.0F);
      s = Math.max(0.75F, (float)Math.floor((double)(s * 4.0F)) / 4.0F);
      this.scale = s;
      this.offX = ((float)this.width - 320.0F * s) / 2.0F;
      this.offY = ((float)this.height - 240.0F * s) / 2.0F;
   }

   private float vx(double mx) {
      return (float)((mx - (double)this.offX) / (double)this.scale);
   }

   private float vy(double my) {
      return (float)((my - (double)this.offY) / (double)this.scale);
   }

   private int slotY(int slot) {
      return 50 + slot * 52;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      float open = Math.min(1.0F, (float)(Util.getMeasuringTimeMs() - this.openedAt) / 900.0F);
      MenuBackdrop.render(ctx, this.width, this.height, 7321855, ease(open));
      float td = Math.min(1.0F, (float)(System.nanoTime() - this.lastTickNanos) / 5.0E7F);
      this.updateHover(this.vx((double)mx), this.vy((double)my));
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(this.offX, this.offY, 0.0F);
      m.scale(this.scale, this.scale, 1.0F);
      String prompt = this.note != null && Util.getMeasuringTimeMs() < this.noteUntil ? this.note : this.promptText();
      float pa = clamp01((float)(Util.getMeasuringTimeMs() - this.openedAt - 150L) / 400.0F);
      ctx.drawText(this.textRenderer, prompt, 34, 30, argb(16777215, pa), true);
      if (this.pageCount() > 1) {
         String pg = "\u25c0 " + (this.page + 1) + "/" + this.pageCount() + " \u25b6";
         ctx.drawText(this.textRenderer, pg, 282 - this.textRenderer.getWidth(pg), 30, argb(11184810, pa), true);
      }

      for (int slot = 0; slot < 3; slot++) {
         float in = ease(clamp01((float)(Util.getMeasuringTimeMs() - this.openedAt - 200L - (long)(slot * 110)) / 380.0F));
         if (!(in <= 0.01F)) {
            this.drawSlot(ctx, slot, in);
         }
      }

      float bin = ease(clamp01((float)(Util.getMeasuringTimeMs() - this.openedAt - 600L) / 400.0F));

      for (int b = 0; b < BUTTONS.length; b++) {
         boolean sel = this.row == 3 && this.col == b && this.mode != FileSelectScreen.Mode.BUSY;
         boolean dim = this.mode != FileSelectScreen.Mode.NORMAL
            && this.mode != FileSelectScreen.Mode.COPY_PICK
            && this.mode != FileSelectScreen.Mode.ERASE_PICK
            && (b == 1 || b == 2);
         int c = sel ? 16777024 : (dim ? 5592405 : 16777215);
         ctx.drawText(this.textRenderer, BUTTONS[b], BTN_X[b], 214 + (int)((1.0F - bin) * 14.0F), argb(c, bin), true);
      }

      float[] hp = this.heartTarget();
      if (hp != null && bin > 0.01F) {
         if (this.heartX < 0.0F) {
            this.heartX = this.prevHeartX = hp[0];
            this.heartY = this.prevHeartY = hp[1];
         }

         float hx = this.prevHeartX + (this.heartX - this.prevHeartX) * td;
         float hy = this.prevHeartY + (this.heartY - this.prevHeartY) * td;
         RenderSystem.enableBlend();
         ctx.drawTexture(HEART, Math.round(hx), Math.round(hy), 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
      }

      if (this.loading) {
         ctx.drawText(this.textRenderer, "Loading...", 54, 66, -5592406, true);
      }

      if (this.loadError != null) {
         ctx.drawText(this.textRenderer, this.loadError, 54, 40, -39322, true);
      }

      m.pop();
   }

   private String promptText() {
      return switch (this.mode) {
         case COPY_PICK -> "Choose a file to copy.";
         case ERASE_PICK -> "Choose a file to erase.";
         case ERASE_CONFIRM -> "Erase this file? It can't be undone.";
         case BUSY -> "Please wait...";
         default -> "Please select a file.";
      };
   }

   private void drawSlot(DrawContext ctx, int slot, float in) {
      int x = 46 + (int)((1.0F - in) * -30.0F);
      int y = this.slotY(slot);
      int a = (int)(in * 255.0F);
      if (!this.slotExists(slot) && !this.loading) {
         box(ctx, x, y, 236, 44, argb(3357252, in * 0.6F), argb(0, in * 0.25F));
      } else {
         boolean active = this.row == slot && this.mode != FileSelectScreen.Mode.BUSY;
         boolean chosen = this.chosenSlot == slot && (this.mode == FileSelectScreen.Mode.CHOSEN || this.mode == FileSelectScreen.Mode.ERASE_CONFIRM);
         boolean lit = active || chosen;
         float pulse = lit ? 0.85F + 0.15F * (float)Math.sin((double)(MenuBackdrop.time() * 4.0F)) : 1.0F;
         int border = lit ? argb(16777215, in * pulse) : argb(8358553, in);
         int fill = argb(lit ? 660516 : 329743, in * (lit ? 0.82F : 0.62F));
         box(ctx, x, y, 236, 44, border, fill);
         if (lit) {
            ctx.fill(x - 2, y - 2, x + 236 + 2, y - 1, argb(10473727, in * 0.25F));
            ctx.fill(x - 2, y + 44 + 1, x + 236 + 2, y + 44 + 2, argb(10473727, in * 0.25F));
         }

         int tc = lit ? 16777215 : 11121858;
         LevelSummary w = this.worldAt(slot);
         if (w == null) {
            ctx.drawText(this.textRenderer, "[EMPTY]", x + 10, y + 9, argb(tc, in), true);
            ctx.drawText(this.textRenderer, "--:--", x + 236 - 10 - this.textRenderer.getWidth("--:--"), y + 9, argb(tc, in), true);
            ctx.drawText(this.textRenderer, lit ? "Start a new file" : "------------", x + 10, y + 26, argb(lit ? 16777024 : tc, in), true);
         } else {
            FileSelectScreen.SaveInfo si = this.infoFor(w);
            String name = w.getDisplayName();
            String time = si.playSec() >= 0L ? formatTime(si.playSec()) : "--:--";
            int nameMax = 206 - this.textRenderer.getWidth(time);
            ctx.drawText(this.textRenderer, this.trim(name, nameMax), x + 10, y + 9, argb(tc, in), true);
            ctx.drawText(this.textRenderer, time, x + 236 - 10 - this.textRenderer.getWidth(time), y + 9, argb(tc, in), true);
            if (!chosen) {
               StringBuilder sb = new StringBuilder();
               if (!si.vessel().isEmpty()) {
                  sb.append(si.vessel()).append("  ");
               }

               if (si.level() > 0) {
                  sb.append("LV ").append(si.level());
               }

               if (!si.biome().isEmpty()) {
                  sb.append(sb.length() > 0 ? "  \u00b7  " : "").append(si.biome());
               }

               if (sb.length() == 0) {
                  sb.append(w.getGameMode() != null ? w.getGameMode().getTranslatableName().getString() : "------------");
               }

               int sub = lit ? 14542576 : 8885412;
               String tag = w.isHardcore() ? "HARDCORE" : (w.isLocked() ? "IN USE" : (w.requiresConversion() ? "OLD" : ""));
               int tagW = tag.isEmpty() ? 0 : this.textRenderer.getWidth(tag) + 8;
               ctx.drawText(this.textRenderer, this.trim(sb.toString(), 216 - tagW), x + 10, y + 26, argb(sub, in), true);
               if (!tag.isEmpty()) {
                  ctx.drawText(
                     this.textRenderer, tag, x + 236 - 10 - this.textRenderer.getWidth(tag), y + 26, argb(w.isHardcore() ? 16729156 : 16755251, in), true
                  );
               }
            } else {
               String[] opts = this.mode == FileSelectScreen.Mode.ERASE_CONFIRM ? new String[]{"Erase", "Back"} : new String[]{"Continue", "Back"};

               for (int i = 0; i < 2; i++) {
                  boolean sel = this.col == i;
                  int c = this.mode == FileSelectScreen.Mode.ERASE_CONFIRM && i == 0 ? (sel ? 16733525 : 11158596) : (sel ? 16777024 : 16777215);
                  ctx.drawText(this.textRenderer, opts[i], x + 24 + i * 96, y + 26, argb(c, in), true);
               }
            }
         }
      }
   }

   private static String formatTime(long sec) {
      long h = sec / 3600L;
      long mnt = sec % 3600L / 60L;
      return h > 0L ? String.format("%d:%02d", h, mnt) : String.format("0:%02d", mnt);
   }

   private String trim(String s, int max) {
      return this.textRenderer.getWidth(s) <= max ? s : this.textRenderer.trimToWidth(s, Math.max(10, max - 6)) + "\u2026";
   }

   private static void box(DrawContext ctx, int x, int y, int w, int h, int border, int fill) {
      ctx.fill(x, y, x + w, y + h, fill);
      ctx.fill(x, y, x + w, y + 2, border);
      ctx.fill(x, y + h - 2, x + w, y + h, border);
      ctx.fill(x, y, x + 2, y + h, border);
      ctx.fill(x + w - 2, y, x + w, y + h, border);
   }

   private float[] heartTarget() {
      if (this.mode == FileSelectScreen.Mode.BUSY) {
         return null;
      } else if (this.row == 3) {
         return new float[]{(float)(BTN_X[this.col] - 11), 214.0F};
      } else {
         return (this.mode == FileSelectScreen.Mode.CHOSEN || this.mode == FileSelectScreen.Mode.ERASE_CONFIRM) && this.chosenSlot == this.row
            ? new float[]{(float)(59 + this.col * 96), (float)(this.slotY(this.row) + 26)}
            : new float[]{33.0F, (float)(this.slotY(this.row) + 18)};
      }
   }

   public void tick() {
      this.lastTickNanos = System.nanoTime();
      float[] t = this.heartTarget();
      this.prevHeartX = this.heartX;
      this.prevHeartY = this.heartY;
      if (t != null && this.heartX >= 0.0F) {
         this.heartX = this.heartX + (t[0] - this.heartX) * 0.55F;
         this.heartY = this.heartY + (t[1] - this.heartY) * 0.55F;
         if (Math.abs(t[0] - this.heartX) < 0.3F) {
            this.heartX = t[0];
         }

         if (Math.abs(t[1] - this.heartY) < 0.3F) {
            this.heartY = t[1];
         }
      }
   }

   private void updateHover(float x, float y) {
      int hr = -2;
      int hc = -2;

      for (int s = 0; s < 3; s++) {
         int sy = this.slotY(s);
         if (x >= 46.0F && x < 282.0F && y >= (float)sy && y < (float)(sy + 44) && this.slotExists(s)) {
            hr = s;
            hc = (this.mode == FileSelectScreen.Mode.CHOSEN || this.mode == FileSelectScreen.Mode.ERASE_CONFIRM)
                  && this.chosenSlot == s
                  && y >= (float)(sy + 20)
               ? (x < 156.0F ? 0 : 1)
               : -1;
         }
      }

      for (int b = 0; b < BUTTONS.length; b++) {
         int w = this.textRenderer.getWidth(BUTTONS[b]);
         if (x >= (float)(BTN_X[b] - 12) && x < (float)(BTN_X[b] + w + 4) && y >= 211.0F && y < 225.0F) {
            hr = 3;
            hc = b;
         }
      }

      if (hr != this.hoverRow || hc != this.hoverCol) {
         this.hoverRow = hr;
         this.hoverCol = hc;
         if (hr == -2 || this.mode == FileSelectScreen.Mode.BUSY) {
            return;
         }

         boolean inline = this.mode == FileSelectScreen.Mode.CHOSEN || this.mode == FileSelectScreen.Mode.ERASE_CONFIRM;
         if (inline && hr != this.chosenSlot && hr != 3) {
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
      if (this.mode == FileSelectScreen.Mode.BUSY) {
         return true;
      } else if (button == 1) {
         this.back();
         return true;
      } else if (button != 0) {
         return super.mouseClicked(mx, my, button);
      } else {
         float x = this.vx(mx);
         float y = this.vy(my);
         this.updateHover(x, y);
         if (this.hoverRow == -2) {
            if (this.mode == FileSelectScreen.Mode.CHOSEN || this.mode == FileSelectScreen.Mode.ERASE_CONFIRM) {
               this.back();
            }

            return true;
         } else if ((this.mode == FileSelectScreen.Mode.CHOSEN || this.mode == FileSelectScreen.Mode.ERASE_CONFIRM)
            && this.hoverRow == this.chosenSlot
            && this.hoverCol < 0) {
            return true;
         } else {
            this.confirm();
            return true;
         }
      }
   }

   public boolean mouseScrolled(double mx, double my, double h, double v) {
      if (v != 0.0
         && (this.mode == FileSelectScreen.Mode.NORMAL || this.mode == FileSelectScreen.Mode.COPY_PICK || this.mode == FileSelectScreen.Mode.ERASE_PICK)) {
         this.turnPage(v > 0.0 ? -1 : 1);
      }

      return true;
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.mode == FileSelectScreen.Mode.BUSY) {
         return true;
      } else {
         boolean inline = this.mode == FileSelectScreen.Mode.CHOSEN || this.mode == FileSelectScreen.Mode.ERASE_CONFIRM;
         switch (key) {
            case 32:
            case 90:
            case 257:
            case 335:
               this.confirm();
               return true;
            case 65:
            case 263:
               this.moveCol(-1, inline);
               return true;
            case 68:
            case 262:
               this.moveCol(1, inline);
               return true;
            case 69:
            case 267:
               if (!inline) {
                  this.turnPage(1);
               }

               return true;
            case 81:
            case 266:
               if (!inline) {
                  this.turnPage(-1);
               }

               return true;
            case 83:
            case 264:
               if (!inline) {
                  this.moveRow(1);
               }

               return true;
            case 87:
            case 265:
               if (!inline) {
                  this.moveRow(-1);
               }

               return true;
            case 88:
            case 256:
            case 259:
            case 340:
            case 344:
               this.back();
               return true;
            default:
               return super.keyPressed(key, scan, mods);
         }
      }
   }

   private void moveRow(int d) {
      int r = this.row;

      for (int i = 0; i < 4; i++) {
         r = Math.floorMod(r + d, 4);
         if (r == 3 || this.slotExists(r)) {
            break;
         }
      }

      if (r != this.row) {
         this.row = r;
         if (this.row == 3) {
            this.col = Math.min(this.col, BUTTONS.length - 1);
         }

         TurnBattleSounds.playHover();
      }
   }

   private void moveCol(int d, boolean inline) {
      if (inline) {
         this.col = this.col == 0 ? 1 : 0;
         TurnBattleSounds.playHover();
      } else if (this.row == 3) {
         this.col = Math.floorMod(this.col + d, BUTTONS.length);
         TurnBattleSounds.playHover();
      } else {
         this.turnPage(d);
      }
   }

   private void turnPage(int d) {
      int np = Math.max(0, Math.min(this.pageCount() - 1, this.page + d));
      if (np != this.page) {
         this.page = np;
         if (this.row < 3 && !this.slotExists(this.row)) {
            this.row = 0;
         }

         TurnBattleSounds.playHover();
      }
   }

   private void confirm() {
      switch (this.mode) {
         case NORMAL:
            if (this.row == 3) {
               this.pressButton(this.col);
               return;
            }

            if (!this.slotExists(this.row)) {
               return;
            }

            TurnBattleSounds.playSelect();
            if (this.isNewSlot(this.row)) {
               this.client.setScreen(new WorldNameScreen(this));
               return;
            }

            this.mode = FileSelectScreen.Mode.CHOSEN;
            this.chosenSlot = this.row;
            this.col = 0;
            break;
         case CHOSEN:
            if (this.row == 3) {
               this.mode = FileSelectScreen.Mode.NORMAL;
               this.chosenSlot = -1;
               this.pressButton(this.col);
               return;
            }

            if (this.col == 1) {
               this.back();
               return;
            }

            LevelSummary w = this.worldAt(this.chosenSlot);
            if (w == null) {
               return;
            }

            if (w.isLocked()) {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
               this.say("That file is already open.");
               return;
            }

            TurnBattleSounds.playSelect();
            String dir = w.getName();
            this.client.createIntegratedServerLoader().start(dir, () -> this.client.setScreen(new FileSelectScreen(this.parent)));
            break;
         case COPY_PICK:
            if (this.row == 3) {
               this.mode = FileSelectScreen.Mode.NORMAL;
               this.pressButton(this.col);
               return;
            }

            LevelSummary w = this.worldAt(this.row);
            if (w == null) {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
               return;
            }

            TurnBattleSounds.playSelect();
            this.copy(w);
            break;
         case ERASE_PICK:
            if (this.row == 3) {
               this.mode = FileSelectScreen.Mode.NORMAL;
               this.pressButton(this.col);
               return;
            }

            LevelSummary w = this.worldAt(this.row);
            if (w == null) {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
               return;
            }

            TurnBattleSounds.playSelect();
            this.mode = FileSelectScreen.Mode.ERASE_CONFIRM;
            this.chosenSlot = this.row;
            this.col = 1;
            break;
         case ERASE_CONFIRM:
            if (this.row == 3) {
               this.mode = FileSelectScreen.Mode.NORMAL;
               this.chosenSlot = -1;
               this.pressButton(this.col);
               return;
            }

            if (this.col == 1) {
               this.back();
               return;
            }

            LevelSummary w = this.worldAt(this.chosenSlot);
            if (w == null) {
               return;
            }

            this.erase(w);
      }
   }

   private void pressButton(int b) {
      TurnBattleSounds.playSelect();
      switch (b) {
         case 0:
            this.client.setScreen(new WorldNameScreen(this));
            break;
         case 1:
            if (this.worlds.isEmpty()) {
               this.say("There are no files to copy.");
               return;
            }

            this.mode = FileSelectScreen.Mode.COPY_PICK;
            this.row = 0;
            break;
         case 2:
            if (this.worlds.isEmpty()) {
               this.say("There are no files to erase.");
               return;
            }

            this.mode = FileSelectScreen.Mode.ERASE_PICK;
            this.row = 0;
            break;
         case 3:
            classicMode = true;
            this.client.setScreen(new SelectWorldScreen(this));
            break;
         default:
            this.close();
      }
   }

   private void back() {
      TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
      switch (this.mode) {
         case NORMAL:
            this.close();
            break;
         case CHOSEN:
         case ERASE_CONFIRM:
            this.row = this.chosenSlot;
            this.mode = this.mode == FileSelectScreen.Mode.ERASE_CONFIRM ? FileSelectScreen.Mode.ERASE_PICK : FileSelectScreen.Mode.NORMAL;
            this.chosenSlot = -1;
            this.col = 0;
            break;
         case COPY_PICK:
         case ERASE_PICK:
            this.mode = FileSelectScreen.Mode.NORMAL;
      }
   }

   private void say(String s) {
      this.note = s;
      this.noteUntil = Util.getMeasuringTimeMs() + 2600L;
   }

   private void copy(LevelSummary w) {
      this.mode = FileSelectScreen.Mode.BUSY;
      LevelStorage storage = this.client.getLevelStorage();
      String srcDir = w.getName();
      String display = w.getDisplayName();
      CompletableFuture.<String>supplyAsync(() -> {
         Path saves = storage.getSavesDirectory();
         Path src = saves.resolve(srcDir);
         String base = srcDir + " (Copy)";
         String dir = base;

         for (int n = 2; Files.exists(saves.resolve(dir)); n++) {
            dir = base + " " + n;
         }

         Path dst = saves.resolve(dir);

         try (Stream walk = Files.walk(src)) {
            for (Path p : walk::iterator) {
               Path rel = src.relativize(p);
               if (!rel.toString().equals("session.lock")) {
                  Path to = dst.resolve(rel.toString());
                  if (Files.isDirectory(p)) {
                     Files.createDirectories(to);
                  } else {
                     Files.copy(p, to);
                  }
               }
            }
         } catch (Exception var18) {
            throw new RuntimeException(var18);
         }

         try {
            Session s = storage.createSession(dir);

            try {
               s.save(display + " (Copy)");
            } catch (Throwable var15) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (s != null) {
               s.close();
            }
         } catch (Exception var16) {
         }

         return dir;
      }, Util.getIoWorkerExecutor()).whenCompleteAsync((dir, err) -> {
         this.mode = FileSelectScreen.Mode.NORMAL;
         if (err != null) {
            this.say("Couldn't copy that file.");
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
         } else {
            this.say("Copy complete.");
            TurnBattleSounds.playSelect();
         }

         this.reload();
      }, this.client);
   }

   private void erase(LevelSummary w) {
      this.mode = FileSelectScreen.Mode.BUSY;
      LevelStorage storage = this.client.getLevelStorage();
      String dir = w.getName();
      CompletableFuture.runAsync(() -> {
         try {
            Session s = storage.createSession(dir);

            try {
               s.deleteSessionLock();
            } catch (Throwable var6) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (s != null) {
               s.close();
            }
         } catch (Exception var7) {
            throw new RuntimeException(var7);
         }
      }, Util.getIoWorkerExecutor()).whenCompleteAsync((v, err) -> {
         this.mode = FileSelectScreen.Mode.NORMAL;
         this.chosenSlot = -1;
         INFO.remove(dir);
         if (err != null) {
            this.say("Couldn't erase that file.");
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
         } else {
            this.say("The file was erased.");
            TurnBattleSounds.playClient(TurnBattleSounds.DAMAGE, 0.8F, 0.8F);
         }

         this.reload();
         this.row = 0;
      }, this.client);
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

   private static enum Mode {
      NORMAL,
      CHOSEN,
      COPY_PICK,
      ERASE_PICK,
      ERASE_CONFIRM,
      BUSY;
   }

   private static record SaveInfo(String vessel, int level, long playSec, String biome) {
   }
}
