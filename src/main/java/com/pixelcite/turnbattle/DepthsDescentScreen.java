package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class DepthsDescentScreen extends Screen {
   private static final int TS = 16;
   private static final float MOVE_SPEED = 3.24F;
   private static final float CHAR_R = 4.0F;
   private static final String[] IDLE = new String[]{"mannequin_idle_n", "mannequin_idle_e", "mannequin_idle_s", "mannequin_idle_w"};
   private static final String[][] WALK = new String[][]{
      {"mannequin_walk1_n", "mannequin_walk2_n"},
      {"mannequin_walk_e", "mannequin_walk_e"},
      {"mannequin_walk1_s", "mannequin_walk2_s"},
      {"mannequin_walk_w", "mannequin_walk_w"}
   };
   private static final int SPR_W = 14;
   private static final int SPR_H = 20;
   private static final int MINIMAP_SIZE = 90;
   private static final int DISCOVER_RADIUS = 7;
   private final Set<Long> discoveredTiles = new HashSet<>();
   public int[][] tiles;
   public int[][] floorTexture;
   public final List<int[]> chestPositions = new ArrayList<>();
   public int stairsTx = -1;
   public int stairsTy = -1;
   public int currentFloor = 1;
   public boolean bossFloor = false;
   public boolean bossDefeated = false;
   public String fountainKey = "";
   public SoulType mySoul = null;
   public float posX;
   public float posY;
   public int myDir = 2;
   private boolean moving = false;
   private int animFrame = 0;
   private int globalTick = 0;
   private float distWalked = 0.0F;
   private static final float ENCOUNTER_DIST = 220.0F;
   private int syncThrottle = 0;
   public final List<DepthsDescentScreen.OtherPlayer> others = new ArrayList<>();
   public boolean encounterActive = false;
   private int encounterTick = 0;
   public int encounterTimerSeconds = 30;
   public final List<float[]> enemyPositions = new ArrayList<>();
   private String textBoxMsg = null;
   private boolean textBoxWaitingClose = false;
   private boolean textBoxIsChest = false;
   private boolean stairsVoteActive = false;
   private int stairsVoteFloor = 0;
   private int stairsVoteCursor = 0;
   private boolean stairsVoteExhausted = false;
   private int camX;
   private int camY;
   private static final String[] FLOOR_TEX = new String[]{
      "minecraft:textures/block/grass_block_top.png",
      "minecraft:textures/block/stone.png",
      "minecraft:textures/block/cobblestone.png",
      "minecraft:textures/block/glass.png",
      "minecraft:textures/block/dirt.png",
      "minecraft:textures/block/oak_planks.png"
   };

   public DepthsDescentScreen() {
      super(Text.literal("Depths Descent"));
   }

   protected void init() {
      super.init();
      if (this.tiles != null) {
         this.encounterActive = false;
         this.encounterTick = 0;
      }
   }

   public void initRoom(
      int[][] tiles, int[][] floorTex, List<int[]> chests, int stx, int sty, int floor, boolean boss, float startPx, float startPy, String key, SoulType soul
   ) {
      this.tiles = tiles;
      this.floorTexture = floorTex;
      this.chestPositions.clear();

      for (int[] c : chests) {
         this.chestPositions.add(new int[]{c[0], c[1], c[2]});
      }

      this.stairsTx = stx;
      this.stairsTy = sty;
      this.currentFloor = floor;
      this.bossFloor = boss;
      this.posX = startPx;
      this.posY = startPy;
      this.fountainKey = key;
      this.mySoul = soul;
   }

   public void updateOtherPlayers(List<DepthsDescentScreen.OtherPlayer> list) {
      this.others.clear();
      this.others.addAll(list);
   }

   public void setEncounter(boolean active) {
      this.encounterActive = active;
      this.encounterTick = 0;
      if (active && this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SND_B, 1.0F));
      }
   }

   public void showChestResult(int idx, boolean hasRoom, String itemName, int count) {
      if (idx >= 0 && idx < this.chestPositions.size()) {
         this.chestPositions.get(idx)[2] = 1;
      }

      this.textBoxMsg = hasRoom ? "You got " + count + "x " + itemName + "!" : "You don't have any room in your inventory.";
      this.textBoxWaitingClose = true;
      this.textBoxIsChest = true;
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SND_CHESTOPEN, 1.0F));
      }
   }

   public void showStairsVote(int floor, boolean boss, boolean exhausted) {
      this.stairsVoteActive = true;
      this.stairsVoteFloor = floor;
      this.stairsVoteCursor = 0;
      this.stairsVoteExhausted = exhausted;
   }

   public void advanceToFloor(int floor, float spawnPx, float spawnPy) {
      this.currentFloor = floor;
      this.bossFloor = floor % 5 == 0;
      this.stairsVoteActive = false;
      this.chestPositions.clear();
      this.stairsTx = -1;
      this.stairsTy = -1;
      this.posX = spawnPx;
      this.posY = spawnPy;
      this.myDir = 2;
      this.enemyPositions.clear();
      this.discoveredTiles.clear();
      this.bossDefeated = false;
   }

   public void tick() {
      this.globalTick++;
      if (this.encounterActive) {
         this.encounterTick++;
      }

      this.animFrame = this.globalTick / 5 % 4;
      if (this.tiles != null) {
         int cx = (int)(this.posX / 16.0F);
         int cy = (int)(this.posY / 16.0F);

         for (int dy = -7; dy <= 7; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
               this.discoveredTiles.add((long)(cx + dx) << 16 | (long)(cy + dy));
            }
         }
      }

      if (!this.encounterActive && this.textBoxMsg == null && !this.stairsVoteActive && this.tiles != null) {
         long win = MinecraftClient.getInstance().getWindow().getHandle();
         float dx = 0.0F;
         float dy = 0.0F;
         if (GLFW.glfwGetKey(win, 87) == 1 || GLFW.glfwGetKey(win, 265) == 1) {
            dy = -3.24F;
            this.myDir = 0;
         } else if (GLFW.glfwGetKey(win, 83) == 1 || GLFW.glfwGetKey(win, 264) == 1) {
            dy = 3.24F;
            this.myDir = 2;
         }

         if (GLFW.glfwGetKey(win, 65) == 1 || GLFW.glfwGetKey(win, 263) == 1) {
            dx = -3.24F;
            this.myDir = 3;
         } else if (GLFW.glfwGetKey(win, 68) == 1 || GLFW.glfwGetKey(win, 262) == 1) {
            dx = 3.24F;
            this.myDir = 1;
         }

         if (dx == 0.0F && dy == 0.0F) {
            this.moving = false;
         } else {
            if (dx != 0.0F && dy != 0.0F) {
               dx *= 0.7071F;
               dy *= 0.7071F;
            }

            this.tryMove(dx, dy);
         }
      } else {
         this.moving = false;
      }
   }

   private void tryMove(float dx, float dy) {
      float nx = this.posX + dx;
      float ny = this.posY + dy;
      if (!this.collidesAt(nx, this.posY)) {
         this.posX = nx;
      }

      if (!this.collidesAt(this.posX, ny)) {
         this.posY = ny;
      }

      float moved = (float)Math.sqrt((double)(dx * dx + dy * dy));
      this.moving = moved > 0.1F;
      int worldW = 1280;
      int worldH = 896;
      this.posX = Math.max(0.0F, Math.min((float)(worldW - 14), this.posX));
      this.posY = Math.max(0.0F, Math.min((float)(worldH - 20), this.posY));
      if (moved > 0.1F) {
         this.distWalked += moved;
         if (this.distWalked >= 220.0F) {
            this.distWalked = 0.0F;
         }
      }

      this.syncThrottle++;
      if (this.syncThrottle >= 3) {
         this.syncThrottle = 0;
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeFloat(this.posX);
         buf.writeFloat(this.posY);
         buf.writeInt(this.myDir);
         Net121.sendToServer(TurnBattleMod.DD_PLAYER_MOVE, buf);
      }
   }

   private boolean collidesAt(float px, float py) {
      float[] xs = new float[]{px + 4.0F, px + 14.0F - 4.0F};
      float[] ys = new float[]{py + 10.0F, py + 20.0F - 2.0F};

      for (float cx : xs) {
         for (float cy : ys) {
            int tx = (int)(cx / 16.0F);
            int ty = (int)(cy / 16.0F);
            if (tx < 0 || tx >= 80 || ty < 0 || ty >= 56 || this.tiles[ty][tx] != 1) {
               return true;
            }
         }
      }

      return false;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, -16777216);
      if (this.tiles != null) {
         float centerX = this.posX + 7.0F;
         float centerY = this.posY + 10.0F;
         this.camX = (int)((float)this.width / 2.0F - centerX);
         this.camY = (int)((float)this.height / 2.0F - centerY);
         this.renderTiles(ctx);
         this.renderChests(ctx);
         this.renderStairs(ctx);
         this.renderEnemies(ctx);
         this.renderOtherPlayers(ctx);
         this.renderMyPlayer(ctx);
         this.renderHUD(ctx);
         this.renderMinimap(ctx);
         if (this.textBoxMsg != null) {
            this.renderTextBox(ctx);
         }

         if (this.stairsVoteActive) {
            this.renderStairsVote(ctx);
         }
      }
   }

   private void renderTiles(DrawContext ctx) {
      int minTx = Math.max(0, -this.camX / 16 - 1);
      int maxTx = Math.min(80, (-this.camX + this.width) / 16 + 2);
      int minTy = Math.max(0, -this.camY / 16 - 1);
      int maxTy = Math.min(56, (-this.camY + this.height) / 16 + 2);

      for (int ty = minTy; ty < maxTy; ty++) {
         for (int tx = minTx; tx < maxTx; tx++) {
            int tile = this.tiles[ty][tx];
            if (tile != 0) {
               int px = this.camX + tx * 16;
               int py = this.camY + ty * 16;
               if (tile == 1) {
                  String path = FLOOR_TEX[this.floorTexture[ty][tx] % FLOOR_TEX.length];
                  String ns = path.contains(":") ? path.split(":")[0] : "minecraft";
                  String id = path.contains(":") ? path.split(":")[1] : path;

                  try {
                     ctx.drawTexture(Identifier.of(ns, id), px, py, 0.0F, 0.0F, 16, 16, 16, 16);
                  } catch (Exception var15) {
                     ctx.fill(px, py, px + 16, py + 16, -11180476);
                  }
               } else {
                  ctx.fill(px, py, px + 16, py + 16, -15066578);
                  ctx.fill(px, py, px + 16, py + 2, -12961186);
               }
            }
         }
      }
   }

   private void renderChests(DrawContext ctx) {
      for (int[] c : this.chestPositions) {
         String name = c[2] == 0 ? "chest_closed" : "chest_open";
         int px = this.camX + c[0] * 16;
         int py = this.camY + c[1] * 16;
         ctx.drawTexture(Identifier.of("turnbattle", "textures/mannequin/" + name + ".png"), px, py, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16);
      }
   }

   private void renderStairs(DrawContext ctx) {
      if (this.stairsTx >= 0) {
         int px = this.camX + this.stairsTx * 16;
         int py = this.camY + this.stairsTy * 16;
         ctx.drawTexture(Identifier.of("minecraft", "textures/block/sculk.png"), px, py, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16);
         if (!this.bossFloor || this.bossDefeated) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u25bc"), px + 8, py + 2, -8892);
         }
      }
   }

   private void renderMyPlayer(DrawContext ctx) {
      Identifier tid = this.getSprite(this.myDir, this.animFrame, this.moving);
      int px = this.camX + (int)this.posX;
      int py = this.camY + (int)this.posY;
      this.drawMannequin(ctx, tid, px, py, this.mySoul);
      if (this.encounterActive) {
         float pulse = (float)Math.abs(Math.sin((double)this.encounterTick * 0.3));
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("!"), px + 7, py - 12, (int)(200.0F + pulse * 55.0F) << 24 | 16720418);
      }
   }

   private void renderOtherPlayers(DrawContext ctx) {
      for (DepthsDescentScreen.OtherPlayer op : this.others) {
         if (this.client == null || this.client.player == null || !op.uuid().equals(this.client.player.getUuid())) {
            int px = this.camX + (int)op.px();
            int py = this.camY + (int)op.py();
            this.drawMannequin(ctx, this.getSprite(op.dir(), this.animFrame, false), px, py, op.soul());
            if (op.encounter()) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("!"), px + 7, py - 12, -56798);
            }
         }
      }
   }

   private void drawMannequin(DrawContext ctx, Identifier tid, int px, int py, SoulType soul) {
      float r = 1.0F;
      float g = 1.0F;
      float b = 1.0F;
      if (soul != null) {
         int c = soul.getColor();
         r = (float)(c >> 16 & 0xFF) / 255.0F;
         g = (float)(c >> 8 & 0xFF) / 255.0F;
         b = (float)(c & 0xFF) / 255.0F;
      }

      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(r, g, b, 1.0F);
      ctx.drawTexture(tid, px, py, 14, 20, 0.0F, 0.0F, 14, 20, 14, 20);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderNameTag(DrawContext ctx, int cx, int topY, String name, SoulType soul) {
      int tw = this.textRenderer.getWidth(name);
      ctx.fill(cx - tw / 2 - 2, topY - 10, cx + tw / 2 + 2, topY - 1, -1442840576);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), cx - tw / 2, topY - 9, soul != null ? soul.getColor() | 0xFF000000 : -1);
   }

   private void renderEnemies(DrawContext ctx) {
      for (int i = 0; i < this.enemyPositions.size(); i++) {
         float[] e = this.enemyPositions.get(i);
         int px = this.camX + (int)e[0];
         int py = this.camY + (int)e[1];
         if (this.bossFloor && !this.bossDefeated) {
            ctx.fill(px - 4, py - 4, px + 14, py + 14, -14548992);
            ctx.fill(px - 3, py - 3, px + 13, py + 13, -4521984);
            ctx.fill(px - 2, py - 2, px + 12, py + 12, -1179648);
            ctx.fill(px, py - 7, px + 2, py - 3, -7864320);
            ctx.fill(px + 4, py - 9, px + 6, py - 3, -7864320);
            ctx.fill(px + 8, py - 7, px + 10, py - 3, -7864320);
            ctx.fill(px + 1, py + 1, px + 3, py + 3, -256);
            ctx.fill(px + 7, py + 1, px + 9, py + 3, -256);
            int diffOrd = difficultyForFloor(this.currentFloor);
            String[] labels = new String[]{"EASY BOSS", "MEDIUM BOSS", "HARD BOSS", "VERY HARD BOSS", "IMPOSSIBLE"};
            int[] cols = new int[]{-12255420, -188, -30720, -48128, -65536};
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(labels[diffOrd]), px + 5, py - 18, cols[diffOrd]);
         } else {
            ctx.fill(px - 1, py - 1, px + 10, py + 10, -14548992);
            ctx.fill(px, py, px + 9, py + 9, -2293760);
            ctx.fill(px + 2, py + 2, px + 4, py + 4, -1);
            ctx.fill(px + 5, py + 2, px + 7, py + 4, -1);
         }
      }
   }

   private void renderHUD(DrawContext ctx) {
      String s = "Floor " + this.currentFloor + (this.bossFloor ? " [BOSS]" : "");
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(s), 5, 5, -8892);
      if (!this.stairsVoteActive && this.textBoxMsg == null) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("WASD: Move  |  F/E: Interact  |  ESC: Exit"), this.width / 2, this.height - 12, -11184794
         );
      }
   }

   private void renderMinimap(DrawContext ctx) {
      if (this.tiles != null) {
         int mmX = this.width - 90 - 6;
         int mmY = this.height - 90 - 6;
         ctx.fill(mmX - 1, mmY - 1, mmX + 90 + 1, mmY + 90 + 1, -872415232);
         float sx = 1.125F;
         float sy = 1.6071428F;

         for (int ty = 0; ty < 56; ty++) {
            for (int tx = 0; tx < 80; tx++) {
               if (this.tiles[ty][tx] != 0) {
                  int px = mmX + (int)((float)tx * sx);
                  int py = mmY + (int)((float)ty * sy);
                  ctx.fill(px, py, px + Math.max(1, (int)sx), py + Math.max(1, (int)sy), this.tiles[ty][tx] == 1 ? -10061978 : -13417421);
               }
            }
         }

         for (int[] c : this.chestPositions) {
            if (this.discoveredTiles.contains((long)c[0] << 16 | (long)c[1])) {
               ctx.fill(
                  mmX + (int)((float)c[0] * sx), mmY + (int)((float)c[1] * sy), mmX + (int)((float)c[0] * sx) + 2, mmY + (int)((float)c[1] * sy) + 2, -2250240
               );
            }
         }

         if (this.stairsTx >= 0 && this.discoveredTiles.contains((long)this.stairsTx << 16 | (long)this.stairsTy)) {
            ctx.fill(
               mmX + (int)((float)this.stairsTx * sx),
               mmY + (int)((float)this.stairsTy * sy),
               mmX + (int)((float)this.stairsTx * sx) + 2,
               mmY + (int)((float)this.stairsTy * sy) + 2,
               -8892
            );
         }

         int mpx = mmX + (int)(this.posX / 16.0F * sx);
         int mpy = mmY + (int)(this.posY / 16.0F * sy);
         ctx.fill(mpx - 1, mpy - 1, mpx + 2, mpy + 2, this.mySoul != null ? this.mySoul.getColor() | 0xFF000000 : -16711936);

         for (DepthsDescentScreen.OtherPlayer op : this.others) {
            if (this.client == null || this.client.player == null || !op.uuid().equals(this.client.player.getUuid())) {
               int opx = mmX + (int)(op.px() / 16.0F * sx);
               int opy = mmY + (int)(op.py() / 16.0F * sy);
               ctx.fill(opx, opy, opx + 2, opy + 2, op.soul() != null ? op.soul().getColor() | 0xFF000000 : -7829249);
            }
         }

         ctx.drawTextWithShadow(this.textRenderer, Text.literal("Map"), mmX + 2, mmY - 9 - 2, -7829368);
      }
   }

   private void renderTextBox(DrawContext ctx) {
      int bw = this.width - 40;
      int bh = 54;
      int bx = 20;
      int by = this.height - bh - 16;
      ctx.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, -1);
      ctx.fill(bx, by, bx + bw, by + bh, -16777216);
      MatrixStack matrices = ctx.getMatrices();
      float scale = 1.4F;
      matrices.push();
      matrices.translate((float)(bx + 10), (float)(by + 10), 0.0F);
      matrices.scale(scale, scale, 1.0F);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("* " + this.textBoxMsg), 0, 0, -1);
      matrices.pop();
      if (this.textBoxWaitingClose) {
         if (this.globalTick / 10 % 2 == 0) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u25bc"), bx + bw - 12, by + bh - 12, -1);
         }

         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("[F/E to close]"), this.width / 2, by + bh - 12, -7829368);
      }
   }

   private void renderStairsVote(DrawContext ctx) {
      int bw = 270;
      int bh = 90;
      int bx = (this.width - bw) / 2;
      int by = this.height / 2 - bh / 2;
      ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, -16777216);
      ctx.fill(bx, by, bx + bw, by + bh, -16119270);
      if (this.stairsVoteExhausted) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("You can't go on..."), this.width / 2, by + 8, -2276284);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u25b6 Leave"), this.width / 2, by + 34, -188);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Space/Enter: confirm"), this.width / 2, by + bh - 12, -11184794);
      } else {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Floor " + this.stairsVoteFloor + " cleared!"), this.width / 2, by + 8, -8892);
         String[] opts = new String[]{"Continue to Floor " + (this.stairsVoteFloor + 1), "Seal the Fountain"};

         for (int i = 0; i < 2; i++) {
            boolean sel = this.stairsVoteCursor == i;
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal((sel ? "\u25b6 " : "  ") + opts[i]), this.width / 2, by + 30 + i * 18, sel ? -188 : -7829368
            );
         }

         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("W/S: choose  \u2022  Space/Enter: confirm"), this.width / 2, by + bh - 12, -11184794);
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.textBoxMsg != null && this.textBoxWaitingClose) {
         if (key != 70 && key != 69 && key != 257 && key != 32) {
            return true;
         } else {
            if (this.textBoxIsChest && !this.textBoxMsg.startsWith("You don't") && this.client != null) {
               this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SND_ITEM, 1.0F));
            }

            this.textBoxMsg = null;
            this.textBoxWaitingClose = false;
            this.textBoxIsChest = false;
            return true;
         }
      } else if (this.stairsVoteActive) {
         if (this.stairsVoteExhausted) {
            if (key != 32 && key != 257) {
               return true;
            } else {
               this.stairsVoteActive = false;
               this.stairsVoteExhausted = false;
               Net121.sendToServer(TurnBattleMod.DD_EXIT, PacketByteBufs.create());
               return true;
            }
         } else if (key == 87 || key == 265) {
            this.stairsVoteCursor = 0;
            return true;
         } else if (key == 83 || key == 264) {
            this.stairsVoteCursor = 1;
            return true;
         } else if (key != 32 && key != 257) {
            return true;
         } else {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(this.stairsVoteCursor == 1);
            Net121.sendToServer(TurnBattleMod.DD_STAIRS_VOTE, buf);
            this.stairsVoteActive = false;
            return true;
         }
      } else if (key == 70 || key == 69) {
         this.tryInteract();
         return true;
      } else if (key == 256) {
         PacketByteBuf buf = PacketByteBufs.create();
         Net121.sendToServer(TurnBattleMod.DD_EXIT, buf);
         if (this.client != null) {
            this.client.setScreen(null);
         }

         return true;
      } else {
         return super.keyPressed(key, scan, mods);
      }
   }

   private void tryInteract() {
      if (this.tiles != null) {
         int cx = (int)((this.posX + 7.0F) / 16.0F);
         int cy = (int)((this.posY + 15.0F) / 16.0F);
         int[][] nb = new int[][]{{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}};

         for (int[] d : nb) {
            int tx = cx + d[0];
            int ty = cy + d[1];
            if (this.stairsTx >= 0 && tx == this.stairsTx && ty == this.stairsTy) {
               Net121.sendToServer(TurnBattleMod.DD_STAIRS_INTERACT, PacketByteBufs.create());
               return;
            }

            for (int i = 0; i < this.chestPositions.size(); i++) {
               int[] c = this.chestPositions.get(i);
               if (c[0] == tx && c[1] == ty && c[2] == 0) {
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeInt(i);
                  Net121.sendToServer(TurnBattleMod.DD_CHEST_OPEN, buf);
                  return;
               }
            }
         }
      }
   }

   private static int difficultyForFloor(int floor) {
      if (floor <= 15) {
         return 0;
      } else if (floor <= 35) {
         return 1;
      } else if (floor <= 50) {
         return 2;
      } else {
         return floor <= 75 ? 3 : 4;
      }
   }

   private Identifier getSprite(int dir, int frame, boolean isMoving) {
      if (isMoving && frame % 2 != 0) {
         String w = WALK[dir][frame == 1 ? 0 : Math.min(1, WALK[dir].length - 1)];
         return Identifier.of("turnbattle", "textures/mannequin/" + w + ".png");
      } else {
         return Identifier.of("turnbattle", "textures/mannequin/" + IDLE[dir] + ".png");
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   static record OtherPlayer(UUID uuid, float px, float py, int dir, boolean encounter, String name, SoulType soul) {
   }
}
