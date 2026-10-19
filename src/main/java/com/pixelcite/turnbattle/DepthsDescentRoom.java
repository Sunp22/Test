package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class DepthsDescentRoom {
   public static final int TILE_SIZE = 16;
   public static final int ROOM_W = 80;
   public static final int ROOM_H = 56;
   public static final int TILE_VOID = 0;
   public static final int TILE_FLOOR = 1;
   public static final int TILE_WALL = 2;
   public final int[][] tiles;
   public final int[][] floorTexture;
   public final List<int[]> chestPositions = new ArrayList<>();
   public int stairsTx = -1;
   public int stairsTy = -1;
   public int playerStartTx;
   public int playerStartTy;
   public final List<int[]> minimapRooms = new ArrayList<>();

   public DepthsDescentRoom(long seed, int floor, boolean bossFloor) {
      this.tiles = new int[56][80];
      this.floorTexture = new int[56][80];
      this.generate(new Random(seed ^ (long)floor * -7046029254386353131L), floor, bossFloor);
   }

   private void generate(Random rng, int floor, boolean bossFloor) {
      int primary = rng.nextInt(6);
      int accent = (primary + 1 + rng.nextInt(5)) % 6;
      int numRooms = 7 + rng.nextInt(4);
      List rooms = new ArrayList<>();
      int attempts = 0;

      while (rooms.size() < numRooms && attempts < 500) {
         attempts++;
         int rw = 7 + rng.nextInt(9);
         int rh = 5 + rng.nextInt(7);
         int rx = 2 + rng.nextInt(Math.max(1, 80 - rw - 4));
         int ry = 2 + rng.nextInt(Math.max(1, 56 - rh - 4));
         boolean overlap = false;
         Iterator var14 = rooms.iterator();

         while (true) {
            if (var14.hasNext()) {
               DepthsDescentRoom.Room e = (DepthsDescentRoom.Room)var14.next();
               if (rx > e.x + e.w + 1 || rx + rw + 1 < e.x || ry > e.y + e.h + 1 || ry + rh + 1 < e.y) {
                  continue;
               }

               overlap = true;
            }

            if (!overlap) {
               rooms.add(new DepthsDescentRoom.Room(rx, ry, rw, rh));
            }
            break;
         }
      }

      if (rooms.isEmpty()) {
         rooms.add(new DepthsDescentRoom.Room(2, 2, 10, 8));
      }

      for (DepthsDescentRoom.Room r : rooms) {
         for (int y = r.y; y < r.y + r.h; y++) {
            for (int x = r.x; x < r.x + r.w; x++) {
               this.tiles[y][x] = 1;
               this.floorTexture[y][x] = rng.nextFloat() < 0.15F ? accent : primary;
            }
         }

         this.minimapRooms.add(new int[]{r.x, r.y, r.w, r.h});
      }

      for (int i = 0; i + 1 < rooms.size(); i++) {
         DepthsDescentRoom.Room a = rooms.get(i);
         DepthsDescentRoom.Room b = rooms.get(i + 1);
         this.carveHallway(a.icx(), a.icy(), b.icx(), b.icy(), rng, primary, accent);
      }

      boolean[] reached = this.bfsReachable(rooms);

      for (int i = 1; i < rooms.size(); i++) {
         if (!reached[i]) {
            double best = Double.MAX_VALUE;
            int bestJ = 0;

            for (int j = 0; j < i; j++) {
               if (reached[j]) {
                  double d = Math.hypot((double)(rooms.get(i).cx() - rooms.get(j).cx()), (double)(rooms.get(i).cy() - rooms.get(j).cy()));
                  if (d < best) {
                     best = d;
                     bestJ = j;
                  }
               }
            }

            DepthsDescentRoom.Room a = rooms.get(bestJ);
            DepthsDescentRoom.Room b = rooms.get(i);
            this.carveHallway(a.icx(), a.icy(), b.icx(), b.icy(), rng, primary, accent);
            reached = this.bfsReachable(rooms);
         }
      }

      for (int y = 1; y < 55; y++) {
         for (int x = 1; x < 79; x++) {
            if (this.tiles[y][x] == 0 && this.tiles[y + 1][x] == 1) {
               this.tiles[y][x] = 2;
            }
         }
      }

      this.playerStartTx = rooms.get(0).icx();
      this.playerStartTy = rooms.get(0).icy();
      this.stairsTx = rooms.get(rooms.size() - 1).icx();
      this.stairsTy = rooms.get(rooms.size() - 1).icy();
      int chestCount = bossFloor ? 3 : 1 + rng.nextInt(2);
      Set used = new HashSet<>();
      used.add(enc(this.playerStartTx, this.playerStartTy));
      used.add(enc(this.stairsTx, this.stairsTy));

      for (int ci = 0; ci < chestCount; ci++) {
         int roomIdx = 1 + rng.nextInt(Math.max(1, rooms.size() - 2));
         DepthsDescentRoom.Room cr = rooms.get(Math.min(roomIdx, rooms.size() - 1));

         for (int a = 0; a < 30; a++) {
            int cx = cr.x + 1 + rng.nextInt(Math.max(1, cr.w - 2));
            int cy = cr.y + 1 + rng.nextInt(Math.max(1, cr.h - 2));
            long k = enc(cx, cy);
            if (this.tiles[cy][cx] == 1 && !used.contains(k)) {
               this.chestPositions.add(new int[]{cx, cy, 0});
               used.add(k);
               break;
            }
         }
      }
   }

   private boolean[] bfsReachable(List<DepthsDescentRoom.Room> rooms) {
      boolean[][] visited = new boolean[56][80];
      Queue q = new LinkedList<>();
      int sx = rooms.get(0).icx();
      int sy = rooms.get(0).icy();
      if (this.tiles[sy][sx] == 1) {
         visited[sy][sx] = true;
         q.add(new int[]{sx, sy});
      }

      int[][] d4 = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

      while (!q.isEmpty()) {
         int[] cur = q.poll();

         for (int[] d : d4) {
            int nx = cur[0] + d[0];
            int ny = cur[1] + d[1];
            if (nx >= 0 && nx < 80 && ny >= 0 && ny < 56 && !visited[ny][nx] && this.tiles[ny][nx] == 1) {
               visited[ny][nx] = true;
               q.add(new int[]{nx, ny});
            }
         }
      }

      boolean[] reached = new boolean[rooms.size()];
      reached[0] = true;

      label57:
      for (int i = 1; i < rooms.size(); i++) {
         DepthsDescentRoom.Room r = rooms.get(i);

         for (int ry = r.y; ry < r.y + r.h; ry++) {
            for (int rx = r.x; rx < r.x + r.w; rx++) {
               if (visited[ry][rx]) {
                  reached[i] = true;
                  continue label57;
               }
            }
         }
      }

      return reached;
   }

   private void carveHallway(int x1, int y1, int x2, int y2, Random rng, int p, int a) {
      if (rng.nextBoolean()) {
         this.carveH(x1, x2, y1, p, a, rng);
         this.carveV(y1, y2, x2, p, a, rng);
      } else {
         this.carveV(y1, y2, x1, p, a, rng);
         this.carveH(x1, x2, y2, p, a, rng);
      }
   }

   private void carveH(int x1, int x2, int y, int p, int a, Random rng) {
      int lo = Math.max(0, Math.min(x1, x2));
      int hi = Math.min(79, Math.max(x1, x2));

      for (int x = lo; x <= hi; x++) {
         for (int dy = 0; dy < 2; dy++) {
            int ty = Math.max(0, Math.min(55, y + dy));
            if (this.tiles[ty][x] == 0) {
               this.tiles[ty][x] = 1;
               this.floorTexture[ty][x] = rng.nextFloat() < 0.15F ? a : p;
            }
         }
      }
   }

   private void carveV(int y1, int y2, int x, int p, int a, Random rng) {
      int lo = Math.max(0, Math.min(y1, y2));
      int hi = Math.min(55, Math.max(y1, y2));

      for (int y = lo; y <= hi; y++) {
         for (int dx = 0; dx < 2; dx++) {
            int tx = Math.max(0, Math.min(79, x + dx));
            if (this.tiles[y][tx] == 0) {
               this.tiles[y][tx] = 1;
               this.floorTexture[y][tx] = rng.nextFloat() < 0.15F ? a : p;
            }
         }
      }
   }

   private static long enc(int x, int y) {
      return (long)x << 16 | (long)y;
   }

   public boolean isSolid(int tx, int ty) {
      return tx >= 0 && tx < 80 && ty >= 0 && ty < 56 ? this.tiles[ty][tx] != 1 : true;
   }

   private static class Room {
      int x;
      int y;
      int w;
      int h;

      Room(int x, int y, int w, int h) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
      }

      int cx() {
         return this.x + this.w / 2;
      }

      int cy() {
         return this.y + this.h / 2;
      }

      int icx() {
         return this.x + Math.max(1, this.w / 2);
      }

      int icy() {
         return this.y + Math.max(1, this.h / 2);
      }
   }
}
