package com.pixelcite.turnbattle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;

public final class DepthsFloor {
   public static final int Y = 64;
   static final int GRID = 7;
   static final int CELL = 11;
   static final int SIZE = 77;
   static final byte VOID = 0;
   static final byte ROOM = 1;
   static final byte HALL = 2;
   static final byte EDGE = 3;
   static final byte CLIFF = 4;
   static final byte LEDGE = 5;
   final int floor;
   final boolean boss;
   final int ox;
   final int oz;
   final byte[][] tiles = new byte[77][77];
   double spawnX;
   double spawnZ;
   double lightX;
   double lightZ;
   double darkX = Double.NaN;
   double darkZ = Double.NaN;
   final List<BlockPos> chests = new ArrayList<>();
   final List<BlockPos> signs = new ArrayList<>();
   BlockPos loreSign;
   static final int LORE_CHANCE = 40;
   final List<double[]> enemySpots = new ArrayList<>();
   double cliffEdgeZ = Double.NaN;
   final int[][] roomOf = new int[77][77];
   final List<int[]> rooms = new ArrayList<>();
   DepthsFloor.Theme theme;
   private static final int FLAGS = 50;
   static final DepthsFloor.Theme[] THEMES = new DepthsFloor.Theme[]{
      new DepthsFloor.Theme(
         "Depths",
         Blocks.DEEPSLATE_TILES,
         Blocks.CRACKED_DEEPSLATE_TILES,
         9,
         Blocks.POLISHED_BLACKSTONE_BRICKS,
         Blocks.POLISHED_DEEPSLATE,
         Blocks.POLISHED_DEEPSLATE,
         0,
         Blocks.BLACKSTONE
      ),
      new DepthsFloor.Theme(
         "Forest", Blocks.MOSS_BLOCK, Blocks.GRASS_BLOCK, 3, Blocks.MOSSY_STONE_BRICKS, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, 4, Blocks.DIRT
      ),
      new DepthsFloor.Theme(
         "Desert", Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, 6, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, 5, Blocks.SANDSTONE
      ),
      new DepthsFloor.Theme("Snow", Blocks.SNOW_BLOCK, Blocks.CALCITE, 6, Blocks.POLISHED_DIORITE, Blocks.SNOW_BLOCK, Blocks.CALCITE, 7, Blocks.PACKED_ICE),
      new DepthsFloor.Theme(
         "Nether",
         Blocks.NETHER_BRICKS,
         Blocks.CRACKED_NETHER_BRICKS,
         7,
         Blocks.RED_NETHER_BRICKS,
         Blocks.NETHERRACK,
         Blocks.CRIMSON_NYLIUM,
         5,
         Blocks.BLACKSTONE
      ),
      new DepthsFloor.Theme(
         "End", Blocks.END_STONE_BRICKS, Blocks.END_STONE, 6, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK, Blocks.PURPUR_BLOCK, 0, Blocks.END_STONE
      ),
      new DepthsFloor.Theme(
         "Ocean", Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE, 6, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE, Blocks.SEA_LANTERN, 12, Blocks.DARK_PRISMARINE
      ),
      new DepthsFloor.Theme(
         "Badlands",
         Blocks.TERRACOTTA,
         Blocks.ORANGE_TERRACOTTA,
         4,
         Blocks.RED_SANDSTONE,
         Blocks.CUT_RED_SANDSTONE,
         Blocks.SMOOTH_RED_SANDSTONE,
         4,
         Blocks.RED_TERRACOTTA
      ),
      new DepthsFloor.Theme("Cherry", Blocks.CHERRY_PLANKS, Blocks.PINK_WOOL, 0, Blocks.CHERRY_LOG, Blocks.MOSS_BLOCK, Blocks.GRASS_BLOCK, 3, Blocks.DIRT),
      new DepthsFloor.Theme(
         "Jungle", Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, 4, Blocks.CHISELED_STONE_BRICKS, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_PLANKS, 0, Blocks.DIRT
      ),
      new DepthsFloor.Theme(
         "Mushroom", Blocks.MYCELIUM, Blocks.PODZOL, 4, Blocks.MUSHROOM_STEM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK, 5, Blocks.DIRT
      ),
      new DepthsFloor.Theme(
         "Deep Dark",
         Blocks.SCULK,
         Blocks.DEEPSLATE_TILES,
         5,
         Blocks.REINFORCED_DEEPSLATE,
         Blocks.DEEPSLATE_BRICKS,
         Blocks.CRACKED_DEEPSLATE_BRICKS,
         5,
         Blocks.DEEPSLATE
      )
   };
   final Map<Long, Boolean> opened;

   private DepthsFloor(int floor, int ox, int oz) {
      this.theme = THEMES[0];
      this.opened = new HashMap<>();
      this.floor = floor;
      this.boss = floor > 0 && floor % 5 == 0;
      this.ox = ox;
      this.oz = oz;

      for (int[] r : this.roomOf) {
         Arrays.fill(r, -1);
      }
   }

   boolean walkable(int tx, int tz) {
      return tx >= 0 && tz >= 0 && tx < 77 && tz < 77 && this.tiles[tx][tz] != 0;
   }

   boolean walkableAt(double x, double z) {
      return this.walkable((int)Math.floor(x) - this.ox, (int)Math.floor(z) - this.oz);
   }

   int roomAt(double x, double z) {
      int tx = (int)Math.floor(x) - this.ox;
      int tz = (int)Math.floor(z) - this.oz;
      return this.walkable(tx, tz) ? this.roomOf[tx][tz] : -1;
   }

   static DepthsFloor generate(int floor, int ox, int oz, long seed) {
      DepthsFloor f = new DepthsFloor(floor, ox, oz);
      f.theme = themeFor(floor, seed);
      Random rng = new Random(seed ^ (long)floor * -7046029254386353131L);
      int target = Math.max(6, Math.min(14, 6 + floor / 2));
      int[][] cellRoom = new int[7][7];

      for (int[] r : cellRoom) {
         Arrays.fill(r, -1);
      }

      List cells = new ArrayList<>();
      List links = new ArrayList<>();
      int sx = 3;
      int sz = 6;
      cells.add(new int[]{sx, sz});
      cellRoom[sx][sz] = 0;
      int[][] dirs = new int[][]{{0, -1}, {0, -1}, {0, -1}, {1, 0}, {-1, 0}, {1, 0}, {-1, 0}, {0, 1}};
      int guard = 0;

      while (cells.size() < target && guard++ < 2000) {
         int[] from = cells.get(rng.nextInt(cells.size()));
         int[] d = dirs[rng.nextInt(dirs.length)];
         int nx = from[0] + d[0];
         int nz = from[1] + d[1];
         if (nx >= 0 && nz >= 0 && nx < 7 && nz < 7 && cellRoom[nx][nz] < 0) {
            cellRoom[nx][nz] = cells.size();
            cells.add(new int[]{nx, nz});
            links.add(new int[]{cellRoom[from[0]][from[1]], cellRoom[nx][nz]});
         }
      }

      int n = cells.size();
      List adj = new ArrayList<>();

      for (int i = 0; i < n; i++) {
         adj.add(new ArrayList<>());
      }

      for (int[] l : links) {
         adj.get(l[0]).add(l[1]);
         adj.get(l[1]).add(l[0]);
      }

      int[] dist = new int[n];
      Arrays.fill(dist, -1);
      ArrayDeque q = new ArrayDeque<>();
      q.add(0);
      dist[0] = 0;

      while (!q.isEmpty()) {
         int c = q.poll();

         for (int o : adj.get(c)) {
            if (dist[o] < 0) {
               dist[o] = dist[c] + 1;
               q.add(o);
            }
         }
      }

      int exit = 0;

      for (int i = 1; i < n; i++) {
         if (dist[i] > dist[exit] || dist[i] == dist[exit] && rng.nextBoolean()) {
            exit = i;
         }
      }

      int[][] centers = new int[n][2];

      for (int ix = 0; ix < n; ix++) {
         int[] c = cells.get(ix);
         boolean big = ix == 0 || ix == exit;
         int w = big ? 7 : 5 + rng.nextInt(4);
         int h = big ? 7 : 5 + rng.nextInt(4);
         if (ix == exit && f.boss) {
            w = 9;
            h = 9;
         }

         int x0 = c[0] * 11 + (11 - w) / 2 + (big ? 0 : rng.nextInt(3) - 1);
         int z0 = c[1] * 11 + (11 - h) / 2 + (big ? 0 : rng.nextInt(3) - 1);
         x0 = Math.max(c[0] * 11 + 1, Math.min(c[0] * 11 + 11 - 1 - w, x0));
         z0 = Math.max(c[1] * 11 + 1, Math.min(c[1] * 11 + 11 - 1 - h, z0));
         int roomIdx = f.rooms.size();
         f.rooms.add(new int[]{x0, z0, x0 + w - 1, z0 + h - 1});

         for (int x = x0; x < x0 + w; x++) {
            for (int z = z0; z < z0 + h; z++) {
               boolean rim = x == x0 || z == z0 || x == x0 + w - 1 || z == z0 + h - 1;
               f.tiles[x][z] = (byte)(rim ? 3 : 1);
               f.roomOf[x][z] = roomIdx;
            }
         }

         centers[ix][0] = x0 + w / 2;
         centers[ix][1] = z0 + h / 2;
      }

      for (int[] l : links) {
         int[] a = centers[l[0]];
         int[] b = centers[l[1]];
         boolean horiz = rng.nextBoolean();
         int cx = horiz ? b[0] : a[0];
         int cz = horiz ? a[1] : b[1];
         f.hall(a[0], a[1], cx, cz);
         f.hall(cx, cz, b[0], b[1]);
      }

      int[] s = centers[0];
      f.lightX = (double)(f.ox + s[0]) + 0.5;
      f.lightZ = (double)(f.oz + s[1]) + 0.5;
      f.spawnX = f.lightX;
      f.spawnZ = f.lightZ + 2.5;
      int[] e = centers[exit];
      f.darkX = (double)(f.ox + e[0]) + 0.5;
      f.darkZ = (double)(f.oz + e[1]) + 0.5;
      List others = new ArrayList<>();

      for (int ix = 1; ix < n; ix++) {
         if (ix != exit) {
            others.add(ix);
         }
      }

      Collections.shuffle(others, rng);
      int chestCount = Math.min(others.size(), 1 + rng.nextInt(2) + floor / 6);
      chestCount = Math.min(chestCount, 4);

      for (int k = 0; k < chestCount; k++) {
         int[] r = f.rooms.get(others.get(k));
         int tx = r[0] + 1 + rng.nextInt(Math.max(1, r[2] - r[0] - 1));
         int tz = r[1] + 1 + rng.nextInt(Math.max(1, r[3] - r[1] - 1));
         f.chests.add(new BlockPos(f.ox + tx, 65, f.oz + tz));
      }

      if (!others.isEmpty() && rng.nextInt(100) < 40) {
         List order = new ArrayList<>(others);
         Collections.shuffle(order, rng);

         for (int pass = 0; pass < 2 && f.loreSign == null; pass++) {
            for (int ixx : order) {
               int[] r = f.rooms.get(ixx);
               int tx = r[0] + 1 + rng.nextInt(Math.max(1, r[2] - r[0] - 1));
               int tz = r[1] + 1 + rng.nextInt(Math.max(1, r[3] - r[1] - 1));
               boolean roomHasChest = false;

               for (BlockPos c : f.chests) {
                  if (f.roomAt((double)c.getX() + 0.5, (double)c.getZ() + 0.5) == ixx) {
                     roomHasChest = true;
                  }
               }

               if ((pass != 0 || !roomHasChest) && !f.isChestAt((double)(f.ox + tx) + 0.5, (double)(f.oz + tz) + 0.5)) {
                  f.loreSign = new BlockPos(f.ox + tx, 65, f.oz + tz);
                  f.signs.add(f.loreSign);
                  break;
               }
            }
         }
      }

      int maxEnemies = Math.min(10, 3 + floor / 2);

      for (int ixx : others) {
         if (f.enemySpots.size() >= maxEnemies) {
            break;
         }

         int[] r = f.rooms.get(ixx);
         int count = floor > 6 && rng.nextInt(2) == 0 ? 2 : 1;

         for (int k = 0; k < count && f.enemySpots.size() < maxEnemies; k++) {
            double x = (double)(f.ox + r[0]) + 1.5 + (double)rng.nextInt(Math.max(1, r[2] - r[0] - 1));
            double z = (double)(f.oz + r[1]) + 1.5 + (double)rng.nextInt(Math.max(1, r[3] - r[1] - 1));
            if (!f.isChestAt(x, z)) {
               f.enemySpots.add(new double[]{x, z});
            }
         }
      }

      if (f.boss) {
         f.enemySpots.add(new double[]{f.darkX, f.darkZ - 2.5});
      }

      return f;
   }

   private boolean isChestAt(double x, double z) {
      for (BlockPos c : this.chests) {
         if (c.getX() == (int)Math.floor(x) && c.getZ() == (int)Math.floor(z)) {
            return true;
         }
      }

      for (BlockPos cx : this.signs) {
         if (cx.getX() == (int)Math.floor(x) && cx.getZ() == (int)Math.floor(z)) {
            return true;
         }
      }

      return false;
   }

   private void hall(int x0, int z0, int x1, int z1) {
      int dx = Integer.signum(x1 - x0);
      int dz = Integer.signum(z1 - z0);
      int x = x0;
      int z = z0;

      while (true) {
         for (int a = -1; a <= 1; a++) {
            int tx = dx != 0 ? x : x + a;
            int tz = dx != 0 ? z + a : z;
            if (tx >= 0 && tz >= 0 && tx < 77 && tz < 77) {
               if (this.tiles[tx][tz] == 0) {
                  this.tiles[tx][tz] = 2;
               } else if (this.tiles[tx][tz] == 3) {
                  this.tiles[tx][tz] = 1;
               }
            }
         }

         if (x == x1 && z == z1) {
            return;
         }

         x += dx;
         z += dz;
      }
   }

   static DepthsFloor prologue(int ox, int oz) {
      DepthsFloor f = new DepthsFloor(0, ox, oz);
      int mid = 38;
      int r0z1 = 73;
      int r0z0 = r0z1 - 6;
      f.rooms.add(new int[]{mid - 3, r0z0, mid + 3, r0z1});

      for (int x = mid - 3; x <= mid + 3; x++) {
         for (int z = r0z0; z <= r0z1; z++) {
            boolean rim = x == mid - 3 || x == mid + 3 || z == r0z0 || z == r0z1;
            f.tiles[x][z] = (byte)(rim ? 3 : 1);
            f.roomOf[x][z] = 0;
         }
      }

      int corTop = r0z0 - 26;

      for (int z = corTop; z < r0z0; z++) {
         for (int x = mid - 1; x <= mid + 1; x++) {
            f.tiles[x][z] = 2;
         }
      }

      f.tiles[mid][r0z0] = 1;
      f.tiles[mid - 1][r0z0] = 1;
      f.tiles[mid + 1][r0z0] = 1;
      int ledgeBottom = corTop - 1;
      int ledgeTop = ledgeBottom - 15;
      Random rng = new Random(53669L);

      for (int z = ledgeTop; z <= ledgeBottom; z++) {
         for (int x = mid - 4; x <= mid + 4; x++) {
            if (z != ledgeTop || rng.nextInt(3) != 0 || Math.abs(x - mid) <= 1) {
               f.tiles[x][z] = (byte)(z <= ledgeTop + 1 ? 4 : 5);
            }
         }
      }

      f.cliffEdgeZ = (double)(f.oz + ledgeTop) + 2.2;
      int[][] at = new int[][]{{mid - 3, ledgeBottom - 2}, {mid + 3, ledgeBottom - 5}, {mid - 3, ledgeBottom - 8}, {mid + 3, ledgeBottom - 11}};

      for (int[] p : at) {
         f.signs.add(new BlockPos(f.ox + p[0], 65, f.oz + p[1]));
      }

      f.lightX = (double)(f.ox + mid) + 0.5;
      f.lightZ = (double)(f.oz + r0z0) + 3.5;
      f.spawnX = f.lightX;
      f.spawnZ = f.lightZ + 2.0;
      return f;
   }

   void build(ServerWorld w, List<Long> placed) {
      Mutable m = new Mutable();
      Random rng = new Random((long)this.ox * 31L + (long)this.oz + (long)this.floor);

      for (int tx = 0; tx < 77; tx++) {
         for (int tz = 0; tz < 77; tz++) {
            byte t = this.tiles[tx][tz];
            int x = this.ox + tx;
            int z = this.oz + tz;
            if (t != 0) {
               set(w, m.set(x, 64, z), this.surface(t, rng), placed);
               set(w, m.set(x, 63, z), this.under(t, rng), placed);
            } else {
               boolean touches = false;

               for (int ddx = -1; ddx <= 1 && !touches; ddx++) {
                  for (int ddz = -1; ddz <= 1 && !touches; ddz++) {
                     if (this.walkable(tx + ddx, tz + ddz)) {
                        touches = true;
                     }
                  }
               }

               if (touches) {
                  set(w, m.set(x, 65, z), Blocks.BARRIER.getDefaultState(), placed);
                  set(w, m.set(x, 66, z), Blocks.BARRIER.getDefaultState(), placed);
               }
            }
         }
      }

      for (BlockPos c : this.chests) {
         Direction face = Type.HORIZONTAL.random(net.minecraft.util.math.random.Random.create(c.asLong()));
         set(w, c, (BlockState)Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, face), placed);
      }

      for (BlockPos s : this.signs) {
         set(w, s, (BlockState)Blocks.DARK_OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 0), placed);
      }
   }

   private static void set(ServerWorld w, BlockPos pos, BlockState st, List<Long> placed) {
      w.setBlockState(pos, st, 50);
      placed.add(pos.asLong());
   }

   static DepthsFloor.Theme themeFor(int floor, long seed) {
      if (floor <= 0) {
         return THEMES[0];
      } else {
         int prev = -1;
         int pick = 0;

         for (int f = 1; f <= floor; f++) {
            Random r = new Random(seed * 25214903917L + (long)f * 2685821657736338717L);
            pick = r.nextInt(THEMES.length);
            if (pick == prev) {
               pick = (pick + 1 + r.nextInt(THEMES.length - 1)) % THEMES.length;
            }

            prev = pick;
         }

         return THEMES[pick];
      }
   }

   private BlockState surface(byte t, Random rng) {
      DepthsFloor.Theme th = this.theme;

      return switch (t) {
         case 1 -> (th.roomOneIn() > 0 && rng.nextInt(th.roomOneIn()) == 0 ? th.roomAccent() : th.room()).getDefaultState();
         case 2 -> (th.hallOneIn() > 0 && rng.nextInt(th.hallOneIn()) == 0 ? th.hallAccent() : th.hall()).getDefaultState();
         case 3 -> th.edge().getDefaultState();
         case 4 -> rng.nextBoolean() ? Blocks.COBBLED_DEEPSLATE.getDefaultState() : Blocks.DEEPSLATE.getDefaultState();
         default -> rng.nextInt(5) == 0 ? Blocks.COBBLED_DEEPSLATE.getDefaultState() : Blocks.DEEPSLATE.getDefaultState();
      };
   }

   private BlockState under(byte t, Random rng) {
      return t != 4 && t != 5 ? this.theme.under().getDefaultState() : Blocks.COBBLED_DEEPSLATE.getDefaultState();
   }

   static void clear(ServerWorld w, List<Long> placed) {
      Mutable m = new Mutable();
      BlockState air = Blocks.AIR.getDefaultState();

      for (long p : placed) {
         w.setBlockState(m.set(p), air, 50);
      }

      placed.clear();
   }

   static void wipe(ServerWorld w, int ox, int oz) {
      Mutable m = new Mutable();
      BlockState air = Blocks.AIR.getDefaultState();

      for (int x = ox - 2; x < ox + 77 + 2; x++) {
         for (int z = oz - 2; z < oz + 77 + 2; z++) {
            for (int y = 63; y <= 66; y++) {
               m.set(x, y, z);
               if (!w.getBlockState(m).isAir()) {
                  w.setBlockState(m, air, 50);
               }
            }
         }
      }
   }

   static void holdingCell(ServerWorld w, int x, int z, List<Long> placed) {
      Mutable m = new Mutable();
      int y = 24;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            set(w, m.set(x + dx, y - 1, z + dz), Blocks.BARRIER.getDefaultState(), placed);
            set(w, m.set(x + dx, y + 2, z + dz), Blocks.BARRIER.getDefaultState(), placed);
            if (dx != 0 || dz != 0) {
               set(w, m.set(x + dx, y, z + dz), Blocks.BARRIER.getDefaultState(), placed);
               set(w, m.set(x + dx, y + 1, z + dz), Blocks.BARRIER.getDefaultState(), placed);
            }
         }
      }
   }

   static List<BlockPos> breakFloor(ServerWorld w, double cx, double cz, int radius) {
      List gone = new ArrayList<>();
      Mutable m = new Mutable();
      int bx = (int)Math.floor(cx);
      int bz = (int)Math.floor(cz);

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            for (int y = 63; y <= 64; y++) {
               m.set(bx + dx, y, bz + dz);
               if (!w.getBlockState(m).isAir()) {
                  gone.add(m.toImmutable());
                  w.setBlockState(m, Blocks.AIR.getDefaultState(), 50);
               }
            }
         }
      }

      return gone;
   }

   static record Theme(String name, Block room, Block roomAccent, int roomOneIn, Block edge, Block hall, Block hallAccent, int hallOneIn, Block under) {
   }
}
