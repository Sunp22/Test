package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class MobPatterns {
   static final int HORDE = 100;
   static final int GRASP = 101;
   static final int TIDE = 102;
   static final int VOLLEY = 110;
   static final int ARROW_RAIN = 111;
   static final int FUSE_FIELD = 120;
   static final int CHAIN = 121;
   static final int WEB = 130;
   static final int POUNCE = 131;
   static final int BLINK = 140;
   static final int VOID_STARE = 141;
   static final int BREW = 150;
   static final int SLIME = 160;
   static final int MAGMA = 161;
   static final int INFERNO = 170;
   static final int GHAST = 180;
   static final int SWOOP = 190;
   static final int CROSSBOW = 200;
   static final int AXE = 201;
   static final int FANGS = 202;
   static final int STAMPEDE = 203;
   static final int LASER = 210;
   static final int ELDER_LASER = 211;
   static final int SHULKER = 220;
   static final int SWARM = 230;
   static final int GALE = 250;
   static final int SONIC_BOOM = 260;
   static final int ECHO = 261;
   static final int SKULL_STORM = 270;
   static final int DECAY_SPIRAL = 271;
   static final int BLADE_FLURRY = 280;
   static final int CLEAVE = 281;
   static final int DRAGON_WRATH = 290;
   static final int END_CRYSTALS = 291;
   static final int TOY_KNIVES = 300;
   static final int WAITING_GAME = 301;
   static final int HAYMAKER = 302;
   static final int FLURRY = 303;
   static final int PIROUETTE = 304;
   static final int GRAND_JETE = 305;
   static final int TORN_PAGES = 306;
   static final int RULED_LINES = 307;
   static final int HOT_OIL = 308;
   static final int BREAKFAST = 309;
   static final int QUICK_DRAW = 310;
   static final int RICOCHET = 311;
   static final int REAL_SLASH = 312;
   static final int KNIFE_RING = 313;
   static final int MIRROR_PSI = 320;
   static final int MIRROR_BLADE = 321;
   static final int D_RAIN = 400;
   static final int D_THROW = 401;
   static final int D_ORBIT = 402;
   static final int D_BOUNCE = 403;
   static final int D_HOME = 404;
   static final int D_WALL = 405;
   static final int D_SPRINKLER = 406;
   static final int D_SLASH = 407;
   static final int D_FOUNTAIN = 408;
   static final int D_DIVE = 409;
   static final int D_ZIGZAG = 410;
   static final int D_SHATTER = 411;
   static final int DB_FIRST = 420;
   static final int DB_COUNT = 20;
   static final int DB_CROSSFIRE = 420;
   static final int DB_STORM = 421;
   static final int DB_HALO = 422;
   static final int DB_GUILLOTINE = 423;
   static final int DB_PENDULUM = 424;
   static final int DB_TWIN_SPIRAL = 425;
   static final int DB_CLOSING_WALLS = 426;
   static final int DB_RINGS = 427;
   static final int DB_SWARM = 428;
   static final int DB_RICOCHET = 429;
   static final int DB_GEYSER = 430;
   static final int DB_LASER_GRID = 431;
   static final int DB_DIVE = 432;
   static final int DB_MINEFIELD = 433;
   static final int DB_TIDE = 434;
   static final int DB_STARBURST = 435;
   static final int DB_BLINK = 436;
   static final int DB_LIGHTHOUSE = 437;
   static final int DB_CASCADE = 438;
   static final int DB_FINALE = 439;
   static final int V_HUSK = 1;
   static final int V_STRAY = 2;
   static final int V_BOGGED = 3;
   static final int V_CAVE = 4;
   static final int V_PIGLIN = 5;
   static final int V_ENDERMITE = 6;
   static final int K_ARROW = 1;
   static final int K_FIREBALL = 2;
   static final int K_SKULL = 3;
   static final int K_SKULL_BLUE = 4;
   static final int K_PEARL = 6;
   static final int K_POTION = 7;
   static final int K_SLIME = 8;
   static final int K_MAGMA = 9;
   static final int K_WEB = 10;
   static final int K_FANG = 11;
   static final int K_VEX = 12;
   static final int K_SHULKER = 13;
   static final int K_TRIDENT = 14;
   static final int K_HAND = 15;
   static final int K_BUG = 16;
   static final int K_WIND = 17;
   static final int K_BOMB = 18;
   static final int K_SHARD = 19;
   static final int K_DRAGONFIRE = 20;
   static final int K_CLOUD = 21;
   static final int K_ORB = 22;
   static final int K_PHANTOM = 23;
   static final int K_SCULK = 24;
   static final int K_ROD = 25;
   static final int K_SPIDER = 26;
   static final int K_CHARGER = 27;
   static final int K_BUBBLE = 28;
   static final int K_DROPLET = 29;
   static final int K_EMBER = 30;
   static final int K_BOLT = 31;
   static final int K_ROCKET = 32;
   static final int K_SPIKE = 33;
   static final int K_TOY_KNIFE = 34;
   static final int K_FIST = 35;
   static final int K_SHOE = 36;
   static final int K_PAGE = 37;
   static final int K_EGG = 38;
   static final int K_SLUG = 39;
   static final int K_KNIFE = 40;
   static final int K_ITEM = 41;
   static final int HUE_CYAN = 1;
   static final int HUE_ORANGE = 2;
   static final int CYAN = 2803967;
   static final int ORANGE = 16752680;
   static final int B_NONE = 0;
   static final int B_HOMING = 1;
   static final int B_SPLIT = 2;
   static final int B_BOUNCE = 3;
   static final int B_FUSE = 4;
   static final int B_BLINK = 5;
   static final int B_LOB = 6;
   static final int B_WAVE = 7;
   static final int B_POUNCE = 8;
   static final int B_LINGER = 9;
   static final int B_LIFE = 10;
   static final int B_ORBIT = 11;
   static final int B_REACH = 12;
   static final int B_SKITTER = 13;
   static final int B_SLIME = 14;
   static final int S_SONIC = 1;
   static final int S_GUARDIAN = 2;
   static final int S_BLADE = 3;
   static final int S_ENDER = 4;
   static final int S_CRYSTAL = 5;
   static final int S_DRAGON = 6;
   static final int S_LANE = 7;
   static final int S_BLAST = 10;
   static final int S_ECHO = 11;
   static final int S_ROSE = 12;
   static final int S_SHOT = 13;
   static final int S_RULE = 14;
   static final int S_ORANGE = 15;
   static final int S_PKFIRE = 16;
   static final List<MobPatterns.Beam> BEAMS = new ArrayList<>();
   static final List<MobPatterns.Ring> RINGS = new ArrayList<>();
   static float darkness = 0.0F;
   static float prevDarkness = 0.0F;
   static int activeBase = -1;
   static int activeVariant = 0;
   private static final Random FX = new Random();
   private static final float PI = (float) Math.PI;
   private static final float TAU = (float) (Math.PI * 2);
   private static float gx = -1.0F;
   private static float gx2 = -1.0F;

   private MobPatterns() {
   }

   static boolean isDarkner(int base) {
      return base >= 400 && base <= 411;
   }

   static boolean isDarknerBoss(int base) {
      return base >= 420 && base < 440;
   }

   static int variantOf(int id) {
      return id / 1000;
   }

   static int baseOf(int id) {
      return id % 1000;
   }

   static int[] patternsFor(String typeKey) {
      if (typeKey == null) {
         return new int[0];
      } else {
         String k = typeKey.startsWith("minecraft:") ? typeKey.substring(10) : typeKey;

         return switch (k) {
            case "zombie", "zombie_villager" -> new int[]{100, 101};
            case "husk" -> new int[]{1100, 1101};
            case "zombified_piglin" -> new int[]{5100, 5101};
            case "drowned" -> new int[]{102};
            case "skeleton" -> new int[]{110, 111};
            case "stray" -> new int[]{2110, 2111};
            case "bogged" -> new int[]{3110, 3111};
            case "creeper" -> new int[]{120, 121};
            case "spider" -> new int[]{130, 131};
            case "cave_spider" -> new int[]{4130, 4131};
            case "enderman" -> new int[]{140, 141};
            case "witch" -> new int[]{150};
            case "slime" -> new int[]{160};
            case "magma_cube" -> new int[]{161};
            case "blaze" -> new int[]{170};
            case "ghast" -> new int[]{180};
            case "phantom" -> new int[]{190};
            case "pillager", "piglin" -> new int[]{200};
            case "vindicator" -> new int[]{201};
            case "evoker", "vex", "illusioner" -> new int[]{202};
            case "ravager", "hoglin", "zoglin", "piglin_brute" -> new int[]{203};
            case "guardian" -> new int[]{210};
            case "elder_guardian" -> new int[]{211};
            case "shulker" -> new int[]{220};
            case "silverfish" -> new int[]{230};
            case "endermite" -> new int[]{6230};
            case "breeze" -> new int[]{250};
            case "warden" -> new int[]{260, 261};
            case "wither" -> new int[]{270, 271};
            case "wither_skeleton" -> new int[]{280, 281};
            case "ender_dragon" -> new int[]{290, 291};
            case "turnbattle:human_patience" -> new int[]{300, 301};
            case "turnbattle:human_bravery" -> new int[]{302, 303};
            case "turnbattle:human_integrity" -> new int[]{304, 305};
            case "turnbattle:human_perseverance" -> new int[]{306, 307};
            case "turnbattle:human_kindness" -> new int[]{308, 309};
            case "turnbattle:human_justice" -> new int[]{310, 311};
            case "turnbattle:human_determination" -> new int[]{312, 313};
            case "turnbattle:mirror_clone" -> new int[]{36, 37, 38, 39, 40, 41, 312, 320, 320};
            default -> new int[0];
         };
      }
   }

   static boolean hueHarmless(int hue, boolean soulMoving) {
      return hue == 1 && !soulMoving || hue == 2 && soulMoving;
   }

   static int hueInUse() {
      return switch (activeBase) {
         case 300, 301 -> 1;
         case 302, 303 -> 2;
         default -> 0;
      };
   }

   static Element humanGimmickElement(int base) {
      return switch (base) {
         case 300, 301, 302, 303, 312, 313, 320, 321 -> Element.NONE;
         case 304, 305 -> Element.ICE;
         case 306, 307 -> Element.DARK;
         case 308, 309 -> Element.NATURE;
         case 310, 311 -> Element.LIGHT;
         default -> null;
      };
   }

   public static void reset() {
      BEAMS.clear();
      RINGS.clear();
      darkness = 0.0F;
      prevDarkness = 0.0F;
      activeBase = -1;
      activeVariant = 0;
      FX.setSeed(BulletPatterns.RNG.nextLong());
   }

   private static Random rng() {
      return BulletPatterns.RNG;
   }

   private static float frf(float a, float b) {
      return a + FX.nextFloat() * (b - a);
   }

   private static float rf(float a, float b) {
      return a + rng().nextFloat() * (b - a);
   }

   private static int ri(int n) {
      return rng().nextInt(n);
   }

   private static Bullet mk(float x, float y, float vx, float vy, float size, int kind, int color) {
      Bullet b = new Bullet();
      b.x = x;
      b.y = y;
      b.vx = vx;
      b.vy = vy;
      b.size = size;
      b.kind = kind;
      b.color = color;
      b.prevX = x;
      b.prevY = y;
      return b;
   }

   private static Bullet aimed(float fx, float fy, float tx, float ty, float spd, float size, int kind, int color) {
      float dx = tx - fx;
      float dy = ty - fy;
      float l = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (l < 0.001F) {
         l = 1.0F;
      }

      return mk(fx, fy, dx / l * spd, dy / l * spd, size, kind, color);
   }

   private static Bullet aimedAng(float fx, float fy, float ang, float spd, float size, int kind, int color) {
      return mk(fx, fy, (float)Math.cos((double)ang) * spd, (float)Math.sin((double)ang) * spd, size, kind, color);
   }

   private static float angTo(float fx, float fy, float tx, float ty) {
      return (float)Math.atan2((double)(ty - fy), (double)(tx - fx));
   }

   private static float[] edge(int e, float f, int bx, int by, int bw, int bh, float out) {
      return switch (e) {
         case 0 -> new float[]{(float)bx + f * (float)bw, (float)by - out};
         case 1 -> new float[]{(float)(bx + bw) + out, (float)by + f * (float)bh};
         case 2 -> new float[]{(float)bx + f * (float)bw, (float)(by + bh) + out};
         default -> new float[]{(float)bx - out, (float)by + f * (float)bh};
      };
   }

   private static MobPatterns.Beam beam(float ox, float oy, float ang, float len, float half, int warn, int fire, int style) {
      MobPatterns.Beam b = new MobPatterns.Beam();
      b.ox = ox;
      b.oy = oy;
      b.ang = ang;
      b.prevAng = ang;
      b.len = len;
      b.half = half;
      b.warn = warn;
      b.fire = fire;
      b.style = style;
      BEAMS.add(b);
      return b;
   }

   private static MobPatterns.Ring ring(float cx, float cy, float speed, float thick, float gapAng, float gapHalf, float maxR, int style, int delay) {
      MobPatterns.Ring r = new MobPatterns.Ring();
      r.cx = cx;
      r.cy = cy;
      r.r = 2.0F;
      r.prevR = 2.0F;
      r.speed = speed;
      r.thick = thick;
      r.gapAng = gapAng;
      r.gapHalf = gapHalf;
      r.maxR = maxR;
      r.style = style;
      r.delay = delay;
      RINGS.add(r);
      return r;
   }

   static void applySpeed(Bullet b, float m) {
      if (m != 1.0F && !(m <= 0.0F) && !(b.size >= 990.0F)) {
         b.vx *= m;
         b.vy *= m;
         b.ax *= m * m;
         b.ay *= m * m;
         switch (b.behavior) {
            case 1:
               b.p2 *= m;
               b.p3 *= m;
               break;
            case 2:
               b.p2 *= m;
               b.timer = Math.max(1, Math.round((float)b.timer / m));
               break;
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
            case 11:
               b.p2 *= m;
            case 7:
            case 9:
            case 10:
            case 13:
            default:
               break;
            case 12:
               b.timer = Math.max(1, Math.round((float)b.timer / m));
               break;
            case 14:
               b.p1 *= m;
         }
      }
   }

   static float hitRadius(Bullet b) {
      return b.kind == 21 ? b.size * 0.75F : b.size;
   }

   static boolean persists(Bullet b) {
      return switch (b.kind) {
         case 8, 9, 11, 21, 25, 27 -> true;
         case 35 -> b.behavior == 12;
         default -> false;
      };
   }

   static int tint(int base, int variant) {
      return switch (base) {
         case 100, 101 -> variant == 1 ? 13218426 : (variant == 5 ? 15180960 : 7316314);
         case 110, 111 -> variant == 2 ? 10479359 : (variant == 3 ? 10273886 : 15262416);
         case 130, 131 -> variant == 4 ? 3042938 : 4861482;
         case 230 -> variant == 6 ? 9060290 : 10133672;
         default -> 16777215;
      };
   }

   static void spawn(int id, int t, int bx, int by, int bw, int bh, float scx, float scy, List<Bullet> out) {
      int base = baseOf(id);
      int v = variantOf(id);
      activeBase = base;
      activeVariant = v;
      float cx = (float)bx + (float)bw / 2.0F;
      float cy = (float)by + (float)bh / 2.0F;
      int col = tint(base, v);
      if (isDarkner(base)) {
         darkner(base, v, t, bx, by, bw, bh, scx, scy, cx, cy, out);
      } else if (isDarknerBoss(base)) {
         darknerBoss(base, v, t, bx, by, bw, bh, scx, scy, cx, cy, out);
      } else {
         switch (base) {
            case 100:
               if (t % 30 == 5 && t < 196) {
                  int side = t / 30 % 2 == 0 ? 1 : 3;
                  int gap = ri(5);

                  for (int s = 0; s < 7; s++) {
                     if (s != gap && s != gap + 1) {
                        float[] p = edge(side, ((float)s + 0.5F) / 7.0F, bx, by, bw, bh, 8.0F);
                        Bullet h = mk(p[0], p[1], side == 1 ? -1.25F : 1.25F, 0.0F, 4.2F, 15, col);
                        h.behavior = 7;
                        h.p1 = 3.0F;
                        h.p2 = 0.12F;
                        out.add(h);
                     }
                  }
               }

               if (t % 15 == 12 && t > 15 && t < 200) {
                  int side = ri(4);
                  float[] p = edge(side, rf(0.15F, 0.85F), bx, by, bw, bh, 8.0F);
                  out.add(aimed(p[0], p[1], scx, scy, t > 100 ? 2.9F : 2.5F, 3.6F, 15, col));
               }
               break;
            case 101:
               int everyxxxxxxxxx = t > 90 ? 10 : 12;
               if (t % everyxxxxxxxxx == 4 && t < 200) {
                  int n = t > 130 ? 2 : 1;

                  for (int ixxxxxxx = 0; ixxxxxxx < n; ixxxxxxx++) {
                     int e = ri(5) == 0 ? 0 : (ri(3) == 0 ? (ri(2) == 0 ? 1 : 3) : 2);
                     Bullet h;
                     if (e != 0 && e != 2) {
                        float y = Math.max(
                           (float)(by + 6), Math.min((float)(by + bh - 6), scy + rf(-14.0F, 14.0F) + (ixxxxxxx == 0 ? 0.0F : rf(-30.0F, 30.0F)))
                        );
                        h = mk(e == 3 ? (float)(bx - 8) : (float)(bx + bw + 8), y, e == 3 ? 3.8F : -3.8F, 0.0F, 4.0F, 15, col);
                     } else {
                        float x = Math.max(
                           (float)(bx + 6), Math.min((float)(bx + bw - 6), scx + rf(-14.0F, 14.0F) + (ixxxxxxx == 0 ? 0.0F : rf(-40.0F, 40.0F)))
                        );
                        h = mk(x, e == 0 ? (float)(by - 8) : (float)(by + bh + 8), 0.0F, e == 0 ? 3.8F : -3.8F, 4.0F, 15, col);
                     }

                     float reach = e != 0 && e != 2 ? Math.abs(scx - h.x) : Math.abs(scy - h.y);
                     h.behavior = 12;
                     h.timer = Math.max(8, Math.round((reach + 16.0F) / 3.8F));
                     h.pendingTicks = 11;
                     out.add(h);
                  }
               }
               break;
            case 102:
               if (t % (t > 100 ? 7 : 9) == 3 && t < 200) {
                  float[] p = edge(ri(2) == 0 ? 0 : (ri(2) == 0 ? 1 : 3), rf(0.1F, 0.9F), bx, by, bw, bh, 10.0F);
                  float a = angTo(p[0], p[1], scx, scy);
                  int n = t > 80 ? 2 : 1;

                  for (int ixxxxxxx = 0; ixxxxxxx < n; ixxxxxxx++) {
                     out.add(aimedAng(p[0], p[1], a + (n == 2 ? (ixxxxxxx == 0 ? -0.13F : 0.13F) : 0.0F), 3.8F, 3.0F, 14, 5683376));
                  }
               }

               if (t % 22 == 10) {
                  float x = Math.max((float)(bx + 10), Math.min((float)(bx + bw - 10), scx + rf(-30.0F, 30.0F)));

                  for (int ixxxxxxx = 0; ixxxxxxx < 6; ixxxxxxx++) {
                     Bullet bb = mk(x, (float)(by + bh + 6 + ixxxxxxx * 9), 0.0F, -1.6F, 3.0F, 28, 12118271);
                     bb.behavior = 7;
                     bb.p1 = 7.0F;
                     bb.p2 = 0.16F;
                     bb.tx = 0.0F;
                     bb.ty = -1.0F;
                     out.add(bb);
                  }
               }
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 151:
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 171:
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 178:
            case 179:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            case 191:
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
            case 204:
            case 205:
            case 206:
            case 207:
            case 208:
            case 209:
            case 212:
            case 213:
            case 214:
            case 215:
            case 216:
            case 217:
            case 218:
            case 219:
            case 221:
            case 222:
            case 223:
            case 224:
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 231:
            case 232:
            case 233:
            case 234:
            case 235:
            case 236:
            case 237:
            case 238:
            case 239:
            case 240:
            case 241:
            case 242:
            case 243:
            case 244:
            case 245:
            case 246:
            case 247:
            case 248:
            case 249:
            case 251:
            case 252:
            case 253:
            case 254:
            case 255:
            case 256:
            case 257:
            case 258:
            case 259:
            case 262:
            case 263:
            case 264:
            case 265:
            case 266:
            case 267:
            case 268:
            case 269:
            case 272:
            case 273:
            case 274:
            case 275:
            case 276:
            case 277:
            case 278:
            case 279:
            case 282:
            case 283:
            case 284:
            case 285:
            case 286:
            case 287:
            case 288:
            case 289:
            case 292:
            case 293:
            case 294:
            case 295:
            case 296:
            case 297:
            case 298:
            case 299:
            case 314:
            case 315:
            case 316:
            case 317:
            case 318:
            case 319:
            default:
               break;
            case 110:
               if (t % 14 == 4 && t < 200) {
                  int k = t / 14 % 3;
                  int side = k == 0 ? 3 : (k == 1 ? 1 : 0);
                  float[] p = edge(side, rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
                  int n = t > 110 ? 5 : 3;
                  float a = angTo(p[0], p[1], scx, scy);

                  for (int ixxxxxxx = 0; ixxxxxxx < n; ixxxxxxx++) {
                     out.add(aimedAng(p[0], p[1], a + ((float)ixxxxxxx - (float)(n - 1) / 2.0F) * 0.2F, 3.0F, 2.6F, 1, col));
                  }
               }
               break;
            case 111:
               if (t % 7 == 2 && t < 196) {
                  boolean left = ri(2) == 0;
                  Bullet ar = mk(
                     left ? (float)(bx + 4) : (float)(bx + bw - 4),
                     (float)(by + bh + 8),
                     (float)(left ? 1 : -1) * rf(1.1F, 2.1F),
                     -rf(3.6F, 4.3F),
                     2.6F,
                     1,
                     col
                  );
                  ar.ay = 0.085F;
                  out.add(ar);
               }

               if (t % 46 == 30) {
                  for (int ixxxxxxx = -2; ixxxxxxx <= 2; ixxxxxxx++) {
                     Bullet ar = mk(scx + (float)(ixxxxxxx * 9), (float)(by - 6), 0.0F, 3.6F, 2.6F, 1, col);
                     ar.pendingTicks = 14;
                     out.add(ar);
                  }
               }
               break;
            case 120:
               if (t % 30 == 6 && t < 190) {
                  int n = t > 110 ? 2 : 1;
                  float a0 = rf(0.0F, (float) (Math.PI * 2));

                  for (int ixxxxxxx = 0; ixxxxxxx < n; ixxxxxxx++) {
                     float a = a0 + (float)ixxxxxxx * (float) Math.PI;
                     float d = rf(18.0F, 30.0F);
                     float x = Math.max((float)(bx + 8), Math.min((float)(bx + bw - 8), scx + (float)Math.cos((double)a) * d));
                     float y = Math.max((float)(by + 8), Math.min((float)(by + bh - 8), scy + (float)Math.sin((double)a) * d));
                     Bullet bomb = mk(x, y, 0.0F, 0.0F, 4.0F, 18, 5226298);
                     bomb.behavior = 4;
                     bomb.timer = 36;
                     bomb.p1 = 30.0F;
                     bomb.p2 = 1.2F;
                     bomb.count = 5;
                     bomb.noWarn = true;
                     out.add(bomb);
                  }
               }
               break;
            case 121:
               if (t % 46 == 6 && t < 180) {
                  boolean horiz = t / 46 % 2 == 0;
                  boolean rev = ri(2) == 0;

                  for (int ixxxxxx = 0; ixxxxxx < 7; ixxxxxx++) {
                     float f = ((float)ixxxxxx + 0.5F) / 7.0F;
                     float x = horiz ? (float)bx + f * (float)bw : Math.max((float)(bx + 8), Math.min((float)(bx + bw - 8), scx));
                     float y = horiz ? Math.max((float)(by + 8), Math.min((float)(by + bh - 8), scy)) : (float)by + f * (float)bh;
                     if (!(Math.abs(x - scx) + Math.abs(y - scy) < 14.0F)) {
                        Bullet bomb = mk(x, y, 0.0F, 0.0F, 4.0F, 18, 5226298);
                        bomb.behavior = 4;
                        bomb.timer = 28 + (rev ? 6 - ixxxxxx : ixxxxxx) * 4;
                        bomb.p1 = 21.0F;
                        bomb.p2 = 1.2F;
                        bomb.count = 2;
                        bomb.noWarn = true;
                        out.add(bomb);
                     }
                  }
               }
               break;
            case 130:
               if (t % 26 == 4 && t < 196) {
                  boolean diag = ri(2) == 0;
                  int gap = 1 + ri(5);
                  boolean fromRight = t / 26 % 2 == 1;

                  for (int i = 0; i < 9; i++) {
                     if (i != gap && i != gap + 1) {
                        float f = (float)i / 8.0F;
                        float off = diag ? f * 40.0F : 0.0F;
                        float x = fromRight ? (float)(bx + bw + 8) + off : (float)(bx - 8) - off;
                        float y = (float)by + f * (float)bh;
                        out.add(mk(x, y, fromRight ? -1.3F : 1.3F, 0.0F, 3.0F, 10, 15592941));
                     }
                  }
               }

               if (t % 20 == 11 && t < 200) {
                  pounce(bx, by, bw, bh, scx, scy, out, col, 20, 2.5F);
               }
               break;
            case 131:
               if (t % 14 == 5 && t < 200) {
                  pounce(bx, by, bw, bh, scx, scy, out, col, 18, 2.8F);
               }

               if (t > 70 && t % 22 == 12 && t < 200) {
                  pounce(bx, by, bw, bh, scx, scy, out, col, 21, 2.8F);
               }

               if (t > 140 && t % 32 == 20 && t < 200) {
                  pounce(bx, by, bw, bh, scx, scy, out, col, 16, 3.0F);
               }
               break;
            case 140:
               if (t % 20 == 8 && t < 196) {
                  float a0 = rf(0.0F, (float) (Math.PI * 2));
                  int n = t > 100 ? 6 : 5;

                  for (int ixxxxxx = 0; ixxxxxx < n; ixxxxxx++) {
                     float a = a0 + (float)ixxxxxx * (float) (Math.PI * 2) / (float)n;
                     float x = Math.max((float)(bx + 4), Math.min((float)(bx + bw - 4), scx + (float)Math.cos((double)a) * 50.0F));
                     float y = Math.max((float)(by + 4), Math.min((float)(by + bh - 4), scy + (float)Math.sin((double)a) * 50.0F));
                     Bullet p = aimed(x, y, scx, scy, 2.3F, 3.2F, 6, 11559167);
                     p.pendingTicks = 14;
                     p.behavior = 5;
                     p.timer = 26;
                     p.p1 = 34.0F;
                     p.p2 = 2.9F;
                     out.add(p);
                  }
               }
               break;
            case 141:
               if (t % 30 == 6 && t < 190) {
                  int corner = ri(4);

                  for (int k = 0; k < (t > 100 ? 2 : 1); k++) {
                     int c = (corner + k * 2) % 4;
                     float ox = c % 2 == 0 ? (float)(bx - 6) : (float)(bx + bw + 6);
                     float oy = c < 2 ? (float)(by - 6) : (float)(by + bh + 6);
                     MobPatterns.Beam b = beam(ox, oy, angTo(ox, oy, scx, scy), 260.0F, 5.0F, 24 + k * 6, 10, 4);
                     b.track = 8;
                  }
               }

               if (t % 6 == 0) {
                  float[] p = edge(ri(4), rf(0.0F, 1.0F), bx, by, bw, bh, 8.0F);
                  Bullet m = aimed(p[0], p[1], cx + rf(-30.0F, 30.0F), cy + rf(-25.0F, 25.0F), 1.2F, 2.4F, 6, 13794559);
                  m.behavior = 13;
                  m.timer = 120;
                  m.p1 = 0.15F;
                  out.add(m);
               }

               if (t % 15 == 10) {
                  float a = rf(0.0F, (float) (Math.PI * 2));
                  float x = Math.max((float)(bx + 4), Math.min((float)(bx + bw - 4), scx + (float)Math.cos((double)a) * 44.0F));
                  float y = Math.max((float)(by + 4), Math.min((float)(by + bh - 4), scy + (float)Math.sin((double)a) * 44.0F));
                  Bullet p = aimed(x, y, scx, scy, 2.4F, 3.2F, 6, 11559167);
                  p.pendingTicks = 12;
                  p.behavior = 5;
                  p.timer = 26;
                  p.p1 = 34.0F;
                  p.p2 = 2.5F;
                  out.add(p);
               }
               break;
            case 150:
               if (t % 26 == 3 && t < 196) {
                  boolean left = ri(2) == 0;
                  float x0 = left ? (float)(bx + 2) : (float)(bx + bw - 2);
                  float y0 = (float)(by - 8);
                  float tx = Math.max((float)(bx + 6), Math.min((float)(bx + bw - 6), scx + rf(-12.0F, 12.0F)));
                  int T = 30;
                  int[] cols = new int[]{14170698, 6202170, 5926044, 9329352};
                  Bullet p = mk(x0, y0, (tx - x0) / (float)T, 0.0F, 3.6F, 7, cols[ri(4)]);
                  p.ay = 2.0F * (scy - y0) / (float)(T * T);
                  p.behavior = 6;
                  p.tx = tx;
                  p.ty = scy;
                  p.count = 5;
                  p.p2 = 1.4F;
                  p.kind2 = 29;
                  p.p3 = ri(4) == 0 ? 15.0F : 0.0F;
                  out.add(p);
               }
               break;
            case 160:
            case 161:
               boolean magma = base == 161;
               if (t == 4 || t == 60 || t == 120) {
                  for (int sx = -1; sx <= 1; sx += 2) {
                     Bullet sl = mk(
                        sx < 0 ? (float)(bx + 14) : (float)(bx + bw - 14),
                        (float)(by - 6),
                        (float)(-sx) * (magma ? 1.7F : 1.4F),
                        0.0F,
                        8.5F,
                        magma ? 9 : 8,
                        magma ? 14704682 : 7784538
                     );
                     sl.behavior = 14;
                     sl.count = 2;
                     sl.ay = magma ? 0.24F : 0.2F;
                     sl.p1 = magma ? 7.0F : 6.3F;
                     sl.timer = 400;
                     sl.noWarn = true;
                     out.add(sl);
                  }
               }

               if (magma && t % 24 == 12 && t < 190) {
                  float[] p = edge(ri(2) == 0 ? 1 : 3, rf(0.2F, 0.8F), bx, by, bw, bh, 8.0F);
                  out.add(aimed(p[0], p[1], scx, scy, 2.6F, 3.0F, 2, 16742938));
               }
               break;
            case 170:
               if (t == 1) {
                  for (int ixxxxxx = 0; ixxxxxx < 4; ixxxxxx++) {
                     float rr = ixxxxxx % 2 == 0 ? 46.0F : 24.0F;
                     float ra = (float)ixxxxxx * (float) Math.PI / 2.0F;
                     Bullet rod = mk(cx + (float)Math.cos((double)ra) * rr, cy + (float)Math.sin((double)ra) * rr, 0.0F, 0.0F, 3.4F, 25, 16756784);
                     rod.behavior = 11;
                     rod.tx = cx;
                     rod.ty = cy;
                     rod.p1 = rr;
                     rod.p2 = (float)(ixxxxxx % 2 == 0 ? 1 : -1) * 0.032F;
                     rod.p3 = ra;
                     rod.timer = 230;
                     rod.noWarn = true;
                     rod.count = 14;
                     rod.pendingTicks = 16;
                     out.add(rod);
                  }
               }

               int everyxxxxxxxxx = t > 100 ? 15 : 20;
               if (t % everyxxxxxxxxx == 6 && t < 200) {
                  int e = ri(4);
                  float f = rf(0.15F, 0.85F);

                  for (int k = 0; k < 3; k++) {
                     float[] p = edge(e, f, bx, by, bw, bh, 8.0F);
                     Bullet fb = aimed(p[0], p[1], scx, scy, 3.3F, 3.2F, 2, 16747040);
                     fb.pendingTicks = k * 5;
                     out.add(fb);
                  }
               }
               break;
            case 180:
               if (t % 30 == 6 && t < 190) {
                  float[] p = edge(0, rf(0.1F, 0.9F), bx, by, bw, bh, 12.0F);
                  Bullet fb = aimed(p[0], p[1], scx, scy, 1.5F, 6.0F, 2, 16742938);
                  fb.behavior = 2;
                  fb.timer = 70;
                  fb.p3 = 26.0F;
                  fb.count = 10;
                  fb.p2 = 2.0F;
                  fb.kind2 = 30;
                  out.add(fb);
               }

               if (t % 30 == 21) {
                  for (int ixxxxxx = 0; ixxxxxx < 3; ixxxxxx++) {
                     out.add(mk((float)bx + rf(8.0F, (float)(bw - 8)), (float)(by - 8 - ixxxxxx * 6), rf(-0.2F, 0.2F), 1.1F, 2.4F, 29, 14543103));
                  }
               }
               break;
            case 190:
               if (t % 10 == 3 && t < 196) {
                  int n = t > 120 ? 3 : (t > 50 ? 2 : 1);

                  for (int ixxxxxx = 0; ixxxxxx < n; ixxxxxx++) {
                     float[] p = edge(0, rf(0.05F, 0.95F), bx, by, bw, bh, 10.0F);
                     Bullet ph = aimed(p[0], p[1], scx + rf(-8.0F, 8.0F), scy + 10.0F, 3.4F, 3.4F, 23, 4873356);
                     ph.behavior = 1;
                     ph.timer = 26;
                     ph.p1 = 0.04F;
                     ph.p2 = 3.4F;
                     out.add(ph);
                  }
               }
               break;
            case 200:
               if (t % 24 == 4 && t < 196) {
                  int side = ri(2) == 0 ? 1 : 3;
                  float[] p = edge(side, Math.max(0.1F, Math.min(0.9F, (scy - (float)by) / (float)bh + rf(-0.2F, 0.2F))), bx, by, bw, bh, 8.0F);
                  float a = angTo(p[0], p[1], scx, scy);
                  int n = t > 110 ? 3 : 1;

                  for (int ix = 0; ix < n; ix++) {
                     Bullet bolt = aimedAng(p[0], p[1], a + ((float)ix - (float)(n - 1) / 2.0F) * 0.16F, 4.6F, 2.6F, 31, 13152400);
                     bolt.pendingTicks = 14;
                     out.add(bolt);
                  }
               }

               if (t % 60 == 40) {
                  float[] p = edge(2, rf(0.2F, 0.8F), bx, by, bw, bh, 6.0F);
                  Bullet r = aimed(p[0], p[1], scx, scy, 1.7F, 3.4F, 32, 16732272 + ri(3) * 8256);
                  r.behavior = 2;
                  r.timer = 40;
                  r.p3 = 20.0F;
                  r.count = 12;
                  r.p2 = 1.8F;
                  r.kind2 = 19;
                  out.add(r);
               }
               break;
            case 201:
               int everyxxxx = t > 100 ? 18 : 22;
               if (t % everyxxxx == 4 && t < 190) {
                  boolean left = t / everyxxxx % 2 == 0;
                  float oy = (float)by + rf(0.25F, 0.75F) * (float)bh;
                  float ox = left ? (float)(bx - 2) : (float)(bx + bw + 2);
                  float mid = left ? 0.0F : (float) Math.PI;
                  boolean down = ri(2) == 0;
                  MobPatterns.Beam b = beam(ox, oy, 0.0F, 92.0F, 5.0F, 20, 8, 3);
                  b.sweep = true;
                  b.a0 = mid + (down ? -1.1F : 1.1F);
                  b.a1 = mid + (down ? 1.1F : -1.1F);
                  b.ang = b.a0;
                  b.prevAng = b.a0;
                  if (t > 60) {
                     float oy2 = (float)by + rf(0.25F, 0.75F) * (float)bh;
                     float ox2 = left ? (float)(bx + bw + 2) : (float)(bx - 2);
                     float mid2 = left ? (float) Math.PI : 0.0F;
                     MobPatterns.Beam b2 = beam(ox2, oy2, 0.0F, 92.0F, 5.0F, 27, 8, 3);
                     b2.sweep = true;
                     b2.a0 = mid2 + (down ? 1.1F : -1.1F);
                     b2.a1 = mid2 + (down ? -1.1F : 1.1F);
                     b2.ang = b2.a0;
                     b2.prevAng = b2.a0;
                  }
               }

               if (t % 11 == 9 && t > 20 && t < 200) {
                  float[] p = edge(ri(3) == 0 ? 0 : (ri(2) == 0 ? 1 : 3), rf(0.2F, 0.8F), bx, by, bw, bh, 8.0F);
                  out.add(aimed(p[0], p[1], scx, scy, 3.3F, 3.0F, 19, 12107464));
               }
               break;
            case 202:
               if (t % 17 == 4 && t < 196) {
                  for (int line = 0; line < (t > 90 ? 2 : 1); line++) {
                     int side = ri(4);
                     float[] p = edge(side, rf(0.2F, 0.8F), bx, by, bw, bh, -4.0F);
                     float a = angTo(p[0], p[1], scx, scy) + (line == 0 ? 0.0F : rf(-0.5F, 0.5F));

                     for (int ixxxxx = 0; ixxxxx < 16; ixxxxx++) {
                        float x = p[0] + (float)Math.cos((double)a) * (float)ixxxxx * 11.0F;
                        float y = p[1] + (float)Math.sin((double)a) * (float)ixxxxx * 11.0F;
                        if (!(x < (float)(bx + 2)) && !(x > (float)(bx + bw - 2)) && !(y < (float)(by + 2)) && !(y > (float)(by + bh - 2))) {
                           Bullet f = mk(x, y, 0.0F, 0.0F, 4.0F, 11, 13156520);
                           f.pendingTicks = 12 + ixxxxx * 2;
                           f.behavior = 10;
                           f.timer = 9;
                           f.noWarn = true;
                           out.add(f);
                        }
                     }
                  }
               }

               if (t % 40 == 30) {
                  for (int sx = 0; sx < 2; sx++) {
                     float[] p = edge(0, sx == 0 ? 0.05F : 0.95F, bx, by, bw, bh, 8.0F);
                     Bullet vx = aimed(p[0], p[1], scx, scy, 1.7F, 2.8F, 12, 11061480);
                     vx.behavior = 1;
                     vx.timer = 100;
                     vx.p1 = 0.08F;
                     vx.p2 = 1.9F;
                     out.add(vx);
                  }
               }
               break;
            case 203:
               if (t % 22 == 4 && t < 196) {
                  int lanes = t > 90 ? 2 : 1;
                  float off = rf(-6.0F, 6.0F);
                  boolean v0 = ri(3) == 0;

                  for (int l = 0; l < lanes; l++) {
                     boolean vertical = l == 1 ? !v0 : v0;
                     float laneOff = l == 2 ? (float)(ri(2) == 0 ? -22 : 22) : 0.0F;
                     Bullet cxx;
                     if (!vertical) {
                        boolean fromLeft = ri(2) == 0;
                        float y = Math.max((float)(by + 8), Math.min((float)(by + bh - 8), scy + off + laneOff));
                        cxx = mk(fromLeft ? (float)(bx - 12) : (float)(bx + bw + 12), y, fromLeft ? 4.4F : -4.4F, 0.0F, 6.0F, 27, 9071192);
                     } else {
                        boolean fromTop = ri(2) == 0;
                        float x = Math.max((float)(bx + 8), Math.min((float)(bx + bw - 8), scx + off + laneOff));
                        cxx = mk(x, fromTop ? (float)(by - 12) : (float)(by + bh + 12), 0.0F, fromTop ? 4.4F : -4.4F, 6.0F, 27, 9071192);
                     }

                     cxx.pendingTicks = 18 + l * 12;
                     out.add(cxx);
                  }
               }
               break;
            case 210:
            case 211:
               boolean elder = base == 211;
               if (t % 34 == 4 && t < 186) {
                  int beams = elder ? (t > 100 ? 3 : 2) : 1;
                  int e0 = ri(4);

                  for (int ixxxxx = 0; ixxxxx < beams; ixxxxx++) {
                     float[] p = edge((e0 + ixxxxx * (beams == 3 ? 1 : 2)) % 4, rf(0.3F, 0.7F), bx, by, bw, bh, 8.0F);
                     MobPatterns.Beam b = beam(p[0], p[1], angTo(p[0], p[1], scx, scy), 260.0F, 4.0F, 34 + ixxxxx * 6, 10, 2);
                     b.track = 8;
                  }
               }

               if (t % 17 == 12) {
                  float[] p = edge(ri(4), rf(0.2F, 0.8F), bx, by, bw, bh, 6.0F);
                  float a = angTo(p[0], p[1], scx, scy);
                  int n = elder ? 7 : 5;

                  for (int ixxxxx = 0; ixxxxx < n; ixxxxx++) {
                     out.add(aimedAng(p[0], p[1], a + ((float)ixxxxx - (float)(n - 1) / 2.0F) * 0.22F, 2.4F, 2.6F, 33, 10473664));
                  }
               }
               break;
            case 220:
               if (t % 17 == 4 && t < 186) {
                  float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, 8.0F);
                  Bullet sx = aimed(p[0], p[1], scx, scy, 1.7F, 3.2F, 13, 15918752);
                  sx.behavior = 1;
                  sx.timer = 120;
                  sx.p1 = 0.075F;
                  sx.p2 = 1.75F;
                  out.add(sx);
               }

               if (t % 36 == 26) {
                  float x = (float)bx + rf(20.0F, (float)(bw - 20));
                  float y = (float)by + rf(20.0F, (float)(bh - 20));

                  for (int ixxxxx = 0; ixxxxx < 8; ixxxxx++) {
                     Bullet sx = aimedAng(x, y, (float)ixxxxx * (float) (Math.PI * 2) / 8.0F, 1.5F, 2.6F, 13, 13150432);
                     sx.pendingTicks = 14;
                     out.add(sx);
                  }
               }
               break;
            case 230:
               if (t % 4 == 1 && t < 200) {
                  float[] p = edge(1 + ri(3), rf(0.1F, 0.9F), bx, by, bw, bh, 6.0F);
                  Bullet bug = aimed(p[0], p[1], scx + rf(-30.0F, 30.0F), scy + rf(-30.0F, 30.0F), rf(1.6F, 2.2F), 2.5F, 16, col);
                  bug.behavior = 13;
                  bug.timer = 160;
                  bug.p1 = 0.35F;
                  out.add(bug);
               }
               break;
            case 250:
               if (t % 14 == 4 && t < 196) {
                  float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, 6.0F);
                  Bullet w = aimed(p[0], p[1], scx + rf(-12.0F, 12.0F), scy + rf(-12.0F, 12.0F), 2.8F, 3.6F, 17, 14218495);
                  w.behavior = 3;
                  w.count = 2;
                  w.p3 = 6.0F;
                  w.p2 = 1.6F;
                  w.kind2 = 19;
                  out.add(w);
               }

               if (t % 40 == 30) {
                  boolean left = ri(2) == 0;
                  int gap = ri(6);

                  for (int ixxxx = 0; ixxxx < 8; ixxxx++) {
                     if (ixxxx != gap && ixxxx != gap + 1) {
                        float[] p = edge(left ? 3 : 1, ((float)ixxxx + 0.5F) / 8.0F, bx, by, bw, bh, 8.0F);
                        out.add(mk(p[0], p[1], left ? 1.7F : -1.7F, 0.0F, 3.0F, 17, 15399935));
                     }
                  }
               }
               break;
            case 260:
               wardenAmbience(t, bx, by, bw, bh, scx, scy, out);
               int everyxxxxx = t < 100 ? 30 : 24;
               if (t >= 10 && t % everyxxxxx == 10 % everyxxxxx && t < 190) {
                  int n = t > 120 ? 2 : 1;
                  int e0 = ri(4);

                  for (int k = 0; k < n; k++) {
                     float[] p = edge((e0 + k * (n == 2 ? 2 : 1)) % 4, rf(0.2F, 0.8F), bx, by, bw, bh, 14.0F);
                     MobPatterns.Beam b = beam(p[0], p[1], angTo(p[0], p[1], scx, scy), 320.0F, 6.0F, 28 + k * 10, 8, 1);
                     b.track = 9;
                  }
               }

               if (t % 60 == 40 && t < 180) {
                  float x = (float)bx + rf(20.0F, (float)(bw - 20));
                  float y = (float)by + rf(20.0F, (float)(bh - 20));
                  ring(x, y, 1.6F, 5.0F, angTo(x, y, scx, scy) + rf(-0.6F, 0.6F), 0.5F, 170.0F, 11, 16);
               }
               break;
            case 261:
               wardenAmbience(t, bx, by, bw, bh, scx, scy, out);
               if (t % 34 == 6 && t < 186) {
                  boolean atSoul = ri(2) == 0;
                  float x = atSoul ? scx : (float)bx + rf(20.0F, (float)(bw - 20));
                  float y = atSoul ? scy : (float)by + rf(20.0F, (float)(bh - 20));
                  float gap = angTo(
                     x,
                     y,
                     (float)bx + (float)bw / 2.0F + rf((float)(-bw) * 0.25F, (float)bw * 0.25F),
                     (float)by + (float)bh / 2.0F + rf((float)(-bh) * 0.25F, (float)bh * 0.25F)
                  );
                  ring(x, y, 1.35F, 5.0F, gap, 0.75F, 210.0F, 11, 16);
               }

               if (t % 60 == 40) {
                  float[] p = edge(ri(4), rf(0.2F, 0.8F), bx, by, bw, bh, 14.0F);
                  MobPatterns.Beam b = beam(p[0], p[1], angTo(p[0], p[1], scx, scy), 320.0F, 6.0F, 30, 8, 1);
                  b.track = 9;
               }
               break;
            case 270:
               float[][] headsx = witherHeads(bx, by, bw);
               int ev = t > 100 ? 10 : 13;
               if (t % ev == 2 && t < 196) {
                  float[] hd = headsx[t / ev % 3];
                  out.add(aimed(hd[0], hd[1], scx, scy, 2.7F, 3.2F, 3, 2763312));
               }

               if (t % 44 == 30 && t < 180) {
                  float[] hd = headsx[1];
                  Bullet sx = aimed(hd[0], hd[1], scx, scy, 1.2F, 4.0F, 4, 5934335);
                  sx.behavior = 1;
                  sx.timer = 60;
                  sx.p1 = 0.04F;
                  sx.p2 = 1.2F;
                  sx.count = 6;
                  sx.p3 = 1.4F;
                  sx.kind2 = 3;
                  out.add(sx);
               }

               if (t > 100 && t % 48 == 5) {
                  float[] hd = headsx[ri(3)];

                  for (int ixxxx = 0; ixxxx < 9; ixxxx++) {
                     float a = (float) Math.PI * ((float)ixxxx + 0.5F) / 9.0F;
                     out.add(aimedAng(hd[0], hd[1], a, 1.5F, 2.8F, 3, 3811898));
                  }
               }
               break;
            case 271:
               float[][] heads = witherHeads(bx, by, bw);
               if (t % 6 == 0 && t < 196) {
                  float a = 0.3F * (float)(t / 6);

                  for (int hI = 0; hI < 3; hI++) {
                     if (hI == 1 || t >= 110) {
                        float sweep = (float) (Math.PI / 2) + (float)Math.sin((double)(a + (float)hI * 2.1F)) * 0.8F;
                        out.add(aimedAng(heads[hI][0], heads[hI][1], sweep, 1.6F, 2.8F, 3, 2763312));
                        if (hI == 1) {
                           out.add(aimedAng(heads[1][0], heads[1][1], (float) Math.PI - sweep, 1.6F, 2.8F, 3, 2763312));
                        }
                     }
                  }
               }

               if (t % 20 == 14 && t < 196) {
                  float[] hd = heads[ri(2) == 0 ? 0 : 2];
                  out.add(aimed(hd[0], hd[1], scx, scy, 2.6F, 3.2F, 3, 2763312));
               }

               if (t % 40 == 30 && t < 180) {
                  float x = Math.max((float)(bx + 12), Math.min((float)(bx + bw - 12), scx + rf(-34.0F, 34.0F)));
                  float y = Math.max((float)(by + 12), Math.min((float)(by + bh - 12), scy + rf(-30.0F, 30.0F)));

                  for (int ixxxx = 0; ixxxx < 6; ixxxx++) {
                     Bullet p = aimedAng(x, y, (float)ixxxx * (float) (Math.PI * 2) / 6.0F, 1.25F, 2.6F, 19, 3807786);
                     p.pendingTicks = 16;
                     p.noWarn = true;
                     out.add(p);
                  }

                  MobPatterns.Ring rr = ring(x, y, 0.0F, 3.0F, 0.0F, 0.0F, 10.0F, 12, 0);
                  rr.r = 9.0F;
                  rr.prevR = 9.0F;
               }
               break;
            case 280:
               int everyxxx = t > 100 ? 26 : 32;
               if (t % everyxxx == 4 && t < 186) {
                  bladeSweep(bx, by, bw, bh, scx, scy, 0);
               }

               if (t % 20 == 16 && t < 196) {
                  float[] p = edge(ri(4), rf(0.2F, 0.8F), bx, by, bw, bh, 8.0F);
                  out.add(aimed(p[0], p[1], scx, scy, 2.6F, 3.0F, 3, 1973796));
               }
               break;
            case 281:
               int everyxx = t > 100 ? 7 : 9;
               if (t % everyxx == 4 && t < 196) {
                  int k = t / everyxx % 4;
                  float ang = k == 0 ? 0.0F : (k == 1 ? (float) (Math.PI / 2) : (k == 2 ? (float) (Math.PI / 4) : (float) (-Math.PI / 4)));
                  float dx = (float)Math.cos((double)ang);
                  float dy = (float)Math.sin((double)ang);
                  float ox = scx - dx * 200.0F;
                  float oy = scy - dy * 200.0F;
                  beam(ox, oy, ang, 400.0F, 5.0F, 14, 6, 3);
                  if (t > 90 && k % 2 == 0) {
                     float a2 = ang + (float) (Math.PI / 2);
                     float ox2 = scx + rf(-18.0F, 18.0F) - (float)Math.cos((double)a2) * 200.0F;
                     float oy2 = scy + rf(-18.0F, 18.0F) - (float)Math.sin((double)a2) * 200.0F;
                     beam(ox2, oy2, a2, 400.0F, 5.0F, 18, 6, 3);
                  }
               }
               break;
            case 290:
               if (t % 30 == 4 && t < 186) {
                  float[] p = edge(0, rf(0.1F, 0.9F), bx, by, bw, bh, 12.0F);
                  Bullet fb = aimed(p[0], p[1], scx, scy, 2.0F, 4.4F, 20, 12599551);
                  fb.behavior = 2;
                  fb.timer = 200;
                  fb.tx = scx;
                  fb.ty = scy;
                  fb.p3 = -1.0F;
                  fb.count = 5;
                  fb.p2 = 1.2F;
                  fb.kind2 = 29;
                  out.add(fb);
               }

               if (t % 64 == 44) {
                  boolean horiz = ri(3) != 0;
                  float pos = horiz
                     ? Math.max((float)(by + 10), Math.min((float)(by + bh - 10), scy))
                     : Math.max((float)(bx + 10), Math.min((float)(bx + bw - 10), scx));
                  if (horiz) {
                     beam((float)(bx - 20), pos, 0.0F, (float)(bw + 40), 8.0F, 28, 12, 6);
                  } else {
                     beam(pos, (float)(by - 20), (float) (Math.PI / 2), (float)(bh + 40), 8.0F, 28, 12, 6);
                  }
               }

               if (t % 9 == 0) {
                  Bullet m = mk((float)bx + rf(0.0F, (float)bw), (float)(by - 8), rf(-0.3F, 0.3F), rf(0.8F, 1.3F), 2.2F, 29, 13144319);
                  out.add(m);
               }
               break;
            case 291:
               if (t % 50 == 2 && t < 180) {
                  boolean pairA = t / 50 % 2 == 0;

                  for (int ix = 0; ix < 2; ix++) {
                     int corner = pairA ? (ix == 0 ? 0 : 3) : (ix == 0 ? 1 : 2);
                     float ox = corner % 2 == 0 ? (float)(bx + 6) : (float)(bx + bw - 6);
                     float oy = corner < 2 ? (float)(by + 6) : (float)(by + bh - 6);
                     float a = angTo(ox, oy, cx, cy) + (ix == 0 ? -0.5F : 0.5F);
                     MobPatterns.Beam b = beam(ox, oy, a, 260.0F, 5.0F, 22, 22, 5);
                     b.rot = (float)(ix == 0 ? 1 : -1) * 0.02F;
                  }
               }

               if (t % 32 == 14 && t < 190) {
                  float[] p = edge(0, rf(0.1F, 0.9F), bx, by, bw, bh, 12.0F);
                  Bullet fb = aimed(p[0], p[1], scx, scy, 2.0F, 4.2F, 20, 12599551);
                  fb.behavior = 2;
                  fb.timer = 200;
                  fb.tx = scx;
                  fb.ty = scy;
                  fb.p3 = -1.0F;
                  fb.count = 5;
                  fb.p2 = 1.2F;
                  fb.kind2 = 29;
                  out.add(fb);
               }
               break;
            case 300:
               int c = t % 70;
               if (c >= 5 && c <= 30 && c % 8 == 5 && t < 186) {
                  float[] p = edge(ri(2) == 0 ? 0 : (ri(2) == 0 ? 1 : 3), rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
                  float a = angTo(p[0], p[1], scx, scy);
                  int n = t > 110 ? 5 : 3;

                  for (int ix = 0; ix < n; ix++) {
                     Bullet k = aimedAng(p[0], p[1], a + ((float)ix - (float)(n - 1) / 2.0F) * 0.2F, 2.9F, 2.6F, 34, 16053496);
                     k.p1 = 0.45F;
                     out.add(k);
                  }
               }

               if (c == 36 && t < 170) {
                  cyanWall(t, bx, by, bw, bh, out, false);
               }
               break;
            case 301:
               if (t % 4 == 1 && t < 196) {
                  Bullet k = mk((float)bx + rf(4.0F, (float)(bw - 4)), (float)(by - 8), 0.0F, rf(2.4F, 3.0F), 2.6F, 34, 2803967);
                  k.hue = 1;
                  out.add(k);
               }

               int everyx = t > 110 ? 22 : 30;
               if (t % everyx == 12 && t < 190) {
                  float[] p = edge(ri(2) == 0 ? 1 : 3, rf(0.2F, 0.8F), bx, by, bw, bh, 10.0F);
                  Bullet k = aimed(p[0], p[1], scx, scy, 2.4F, 2.8F, 34, 16053496);
                  k.p1 = 0.4F;
                  out.add(k);
               }

               if (t == 120) {
                  cyanWall(t, bx, by, bw, bh, out, true);
               }
               break;
            case 302:
               int everyxxxxxx = t > 110 ? 16 : 22;
               if (t % everyxxxxxx == 4 && t < 190) {
                  int n = t > 130 ? 2 : 1;

                  for (int ix = 0; ix < n; ix++) {
                     boolean fromLeft = (t / everyxxxxxx + ix) % 2 == 0;
                     float y = Math.max((float)(by + 8), Math.min((float)(by + bh - 8), scy + (float)(ix == 0 ? 0 : (ri(2) == 0 ? -30 : 30))));
                     Bullet f = mk(fromLeft ? (float)(bx - 12) : (float)(bx + bw + 12), y, fromLeft ? 6.2F : -6.2F, 0.0F, 6.0F, 35, 12071466);
                     float reach = fromLeft ? scx + 30.0F - f.x : f.x - (scx - 30.0F);
                     f.behavior = 12;
                     f.timer = Math.max(10, Math.round(reach / 6.2F));
                     f.pendingTicks = 16 + ix * 6;
                     out.add(f);
                  }
               }

               if (t % 48 == 30 && t < 170) {
                  orangeColumn(t, bx, by, bw, bh, out);
               }
               break;
            case 303:
               if (t % 7 == 3 && t < 196) {
                  int e = ri(4);
                  float[] p = edge(e, rf(0.2F, 0.8F), bx, by, bw, bh, 10.0F);
                  Bullet f = aimed(p[0], p[1], scx + rf(-10.0F, 10.0F), scy + rf(-10.0F, 10.0F), 4.6F, 4.2F, 35, 12071466);
                  float d = (float)Math.hypot((double)(scx - p[0]), (double)(scy - p[1]));
                  f.behavior = 12;
                  f.timer = Math.max(8, Math.round((d + 14.0F) / 4.6F));
                  f.pendingTicks = 10;
                  out.add(f);
               }

               if (t % 40 == 24 && t < 180) {
                  boolean horiz = ri(2) == 0;
                  MobPatterns.Beam b = horiz
                     ? beam((float)(bx - 4), Math.max((float)(by + 10), Math.min((float)(by + bh - 10), scy)), 0.0F, (float)(bw + 8), 9.0F, 24, 14, 15)
                     : beam(
                        Math.max((float)(bx + 10), Math.min((float)(bx + bw - 10), scx)),
                        (float)(by - 4),
                        (float) (Math.PI / 2),
                        (float)(bh + 8),
                        9.0F,
                        24,
                        14,
                        15
                     );
                  b.hue = 2;
               }
               break;
            case 304:
               float ex = cx;
               float ey = (float)(by + 14);
               if (t >= 14 && t % 4 == 0 && t < 196) {
                  float dir = t / 60 % 2 == 0 ? 1.0F : -1.0F;
                  float base0 = (float)t * 0.19F * dir;
                  int arms = t > 100 ? 3 : 2;

                  for (int ix = 0; ix < arms; ix++) {
                     Bullet sh = aimedAng(ex, ey, base0 + (float)ix * (float) (Math.PI * 2) / (float)arms, 1.55F, 3.0F, 36, 9226495);
                     sh.noWarn = true;
                     out.add(sh);
                  }
               }

               if (t % 50 == 40 && t < 180) {
                  float x = Math.max((float)(bx + 8), Math.min((float)(bx + bw - 8), scx));
                  Bullet st = mk(x, (float)(by - 10), 0.0F, 5.2F, 3.6F, 36, 5146328);
                  st.pendingTicks = 16;
                  out.add(st);
               }
               break;
            case 305:
               if (t % 11 == 2 && t < 190) {
                  boolean left = t / 11 % 2 == 0;
                  float x0 = left ? (float)bx + rf(4.0F, (float)bw * 0.35F) : (float)(bx + bw) - rf(4.0F, (float)bw * 0.35F);
                  float y0 = (float)(by + bh + 8);
                  Bullet sh = lobTo(x0, y0, scx + rf(-10.0F, 10.0F), scy, -5.0F, 0.13F, 36, 9226495);
                  out.add(sh);
               }

               if (t % 36 == 20 && t < 180) {
                  float x = Math.max((float)(bx + 8), Math.min((float)(bx + bw - 8), scx));

                  for (int ixxxx = -1; ixxxx <= 1; ixxxx++) {
                     Bullet st = mk(x + (float)(ixxxx * 22), (float)(by - 10), 0.0F, 5.0F, 3.6F, 36, 5146328);
                     st.pendingTicks = 14 + Math.abs(ixxxx) * 6;
                     out.add(st);
                  }
               }
               break;
            case 306:
               if (t % 6 == 1 && t < 196) {
                  Bullet pg = mk((float)bx + rf(6.0F, (float)(bw - 6)), (float)(by - 8), rf(-0.2F, 0.2F), rf(1.0F, 1.4F), 3.4F, 37, 16052454);
                  pg.behavior = 7;
                  pg.p1 = 9.0F;
                  pg.p2 = 0.09F;
                  if (ri(3) == 0) {
                     pg.behavior = 2;
                     pg.timer = 400;
                     pg.p3 = 22.0F;
                     pg.count = 4;
                     pg.p2 = 1.6F;
                     pg.kind2 = 37;
                     pg.color = 15259896;
                  }

                  out.add(pg);
               }

               if (t % 24 == 10 && t > 30 && t < 190) {
                  float[] p = edge(ri(2) == 0 ? 1 : 3, rf(0.1F, 0.9F), bx, by, bw, bh, 8.0F);
                  Bullet ink = aimed(p[0], p[1], scx, scy, 2.2F, 2.4F, 22, 9063112);
                  ink.behavior = 7;
                  ink.p1 = 6.0F;
                  ink.p2 = 0.2F;
                  out.add(ink);
               }
               break;
            case 307:
               if (t % 38 == 6 && t < 176) {
                  boolean vertical = t > 90 && t / 38 % 2 == 1;
                  int lines = vertical ? 8 : 6;
                  float span = vertical ? (float)bw : (float)bh;
                  float step = span / (float)lines;
                  int gap = ri(lines);

                  for (int ix = 0; ix < lines; ix++) {
                     if (ix != gap) {
                        float pos = (float)(vertical ? bx : by) + ((float)ix + 0.5F) * step;
                        if (vertical) {
                           beam(pos, (float)(by - 4), (float) (Math.PI / 2), (float)(bh + 8), step / 2.0F - 1.0F, 32, 10, 14);
                        } else {
                           beam((float)(bx - 4), pos, 0.0F, (float)(bw + 8), step / 2.0F - 1.0F, 32, 10, 14);
                        }
                     }
                  }
               }

               if (t % 9 == 4 && t < 196) {
                  boolean left = ri(2) == 0;
                  Bullet ink = mk(
                     left ? (float)(bx - 6) : (float)(bx + bw + 6), (float)by + rf(8.0F, (float)(bh - 8)), left ? 1.8F : -1.8F, 0.0F, 2.2F, 22, 9063112
                  );
                  ink.behavior = 7;
                  ink.p1 = 5.0F;
                  ink.p2 = 0.25F;
                  out.add(ink);
               }
               break;
            case 308:
               if (t % 5 == 2 && t < 196) {
                  Bullet d = mk((float)bx + rf(6.0F, (float)(bw - 6)), (float)(by - 6), rf(-0.6F, 0.6F), rf(0.4F, 0.9F), 2.4F, 29, 15245360);
                  d.ay = 0.05F;
                  out.add(d);
               }

               if (t % 34 == 20 && t < 186) {
                  float ox = cx + rf((float)(-bw) * 0.25F, (float)bw * 0.25F);

                  for (int ixxxx = 0; ixxxx < 7; ixxxx++) {
                     Bullet d = aimedAng(ox, (float)(by - 6), (float) (Math.PI / 2) + (float)(ixxxx - 3) * 0.24F, 2.1F, 2.6F, 29, 16758848);
                     d.ay = 0.03F;
                     out.add(d);
                  }
               }
               break;
            case 309:
               if (t % 16 == 6 && t < 186) {
                  boolean left = t / 16 % 2 == 0;
                  float x0 = left ? (float)(bx + 4) : (float)(bx + bw - 4);
                  Bullet egg = lobTo(x0, (float)(by + bh + 8), scx + rf(-8.0F, 8.0F), scy + rf(-6.0F, 6.0F), -5.3F, 0.13F, 38, 16304176);
                  egg.behavior = 6;
                  egg.count = 6;
                  egg.p2 = 1.45F;
                  egg.kind2 = 29;
                  egg.p3 = 0.0F;
                  out.add(egg);
               }

               if (t % 9 == 0 && t > 40 && t < 196) {
                  Bullet d = mk((float)bx + rf(6.0F, (float)(bw - 6)), (float)(by - 6), rf(-0.4F, 0.4F), rf(0.6F, 1.0F), 2.2F, 29, 15245360);
                  d.ay = 0.04F;
                  out.add(d);
               }
               break;
            case 310:
               int everyxxxxxxx = t > 100 ? 20 : 26;
               if (t % everyxxxxxxx == 4 && t < 182) {
                  int n = t > 120 ? 2 : 1;
                  int e0 = ri(4);

                  for (int k = 0; k < n; k++) {
                     float[] p = edge((e0 + k * 2) % 4, rf(0.2F, 0.8F), bx, by, bw, bh, 12.0F);
                     MobPatterns.Beam b = beam(p[0], p[1], angTo(p[0], p[1], scx, scy), 320.0F, 3.0F, 26 + k * 8, 4, 13);
                     b.track = 8;
                  }
               }

               if (t % 30 == 18 && t < 190) {
                  float[] p = edge(ri(4), rf(0.2F, 0.8F), bx, by, bw, bh, 8.0F);
                  out.add(aimed(p[0], p[1], scx, scy, 3.6F, 2.0F, 39, 15255648));
               }
               break;
            case 311:
               if (t % 10 == 3 && t < 190) {
                  float[] p = edge(ri(4), rf(0.15F, 0.85F), bx, by, bw, bh, 8.0F);
                  float ax = Math.max((float)(bx + 6), Math.min((float)(bx + bw - 6), scx + rf(-24.0F, 24.0F)));
                  float ay2 = Math.max((float)(by + 6), Math.min((float)(by + bh - 6), scy + rf(-24.0F, 24.0F)));
                  Bullet sl = aimed(p[0], p[1], ax, ay2, 3.0F, 2.0F, 39, 15255648);
                  sl.behavior = 3;
                  sl.count = t > 100 ? 3 : 2;
                  sl.p3 = 0.0F;
                  out.add(sl);
               }
               break;
            case 312:
               int everyxxxxxxxx = t > 100 ? 18 : 24;
               if (t % everyxxxxxxxx == 4 && t < 186) {
                  int k = ri(4);
                  float ang = k == 0 ? 0.0F : (k == 1 ? (float) (Math.PI / 2) : (k == 2 ? (float) (Math.PI / 4) : (float) (-Math.PI / 4)));
                  float dx = (float)Math.cos((double)ang);
                  float dy = (float)Math.sin((double)ang);
                  float nx = -dy;
                  float ny = dx;

                  for (int ixx = -1; ixx <= 1; ixx++) {
                     float px = scx + nx * (float)ixx * 26.0F;
                     float py = scy + ny * (float)ixx * 26.0F;
                     beam(px - dx * 200.0F, py - dy * 200.0F, ang, 400.0F, 5.0F, 16 + Math.abs(ixx) * 3, 6, 3);
                  }
               }

               if (t % 14 == 9 && t < 196) {
                  float[] p = edge(ri(4), rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
                  out.add(aimed(p[0], p[1], scx, scy, 3.4F, 2.8F, 40, 15265012));
               }
               break;
            case 313:
               if (t % 44 == 6 && t < 170) {
                  int n = t > 100 ? 14 : 12;
                  float rot = rf(0.0F, (float) (Math.PI * 2));
                  int gap = ri(n);

                  for (int ixx = 0; ixx < n; ixx++) {
                     if (ixx != gap && ixx != (gap + 1) % n) {
                        float a = rot + (float)ixx * (float) (Math.PI * 2) / (float)n;
                        float x = Math.max((float)(bx - 18), Math.min((float)(bx + bw + 18), scx + (float)Math.cos((double)a) * 66.0F));
                        float y = Math.max((float)(by - 18), Math.min((float)(by + bh + 18), scy + (float)Math.sin((double)a) * 66.0F));
                        Bullet kn = aimed(x, y, scx, scy, 2.6F, 2.8F, 40, 15265012);
                        kn.pendingTicks = 18;
                        kn.noWarn = true;
                        out.add(kn);
                     }
                  }
               }

               if (t % 44 == 30 && t < 186) {
                  int k = ri(4);
                  float ang = k == 0 ? 0.0F : (k == 1 ? (float) (Math.PI / 2) : (k == 2 ? (float) (Math.PI / 4) : (float) (-Math.PI / 4)));
                  float dx = (float)Math.cos((double)ang);
                  float dy = (float)Math.sin((double)ang);
                  beam(scx - dx * 200.0F, scy - dy * 200.0F, ang, 400.0F, 5.0F, 14, 6, 3);
               }
               break;
            case 320:
               int phase = t / 52 % 4;
               int u = t % 52;
               if (t < 196) {
                  switch (phase) {
                     case 0:
                        if (u == 4 || u == 28) {
                           float x0 = Math.max((float)(bx + 10), Math.min((float)(bx + bw - 10), scx));

                           for (int ixxx = -1; ixxx <= 1; ixxx++) {
                              float x = x0 + (float)ixxx * 34.0F + (u == 28 ? 17.0F : 0.0F);
                              if (!(x < (float)(bx + 4)) && !(x > (float)(bx + bw - 4))) {
                                 beam(x, (float)(by - 4), (float) (Math.PI / 2), (float)(bh + 8), 8.0F, 22 + Math.abs(ixxx) * 3, 9, 16);
                              }
                           }
                        }

                        if (u % 6 == 3) {
                           Bullet e = mk((float)bx + rf(6.0F, (float)(bw - 6)), (float)(by + bh + 4), rf(-0.3F, 0.3F), -rf(1.6F, 2.4F), 2.2F, 30, 16747056);
                           out.add(e);
                        }
                        break;
                     case 1:
                        if (u % 14 == 4) {
                           float a = rf(0.0F, (float) (Math.PI * 2));
                           float x = Math.max((float)(bx + 10), Math.min((float)(bx + bw - 10), scx + (float)Math.cos((double)a) * 42.0F));
                           float y = Math.max((float)(by + 10), Math.min((float)(by + bh - 10), scy + (float)Math.sin((double)a) * 42.0F));
                           int n = 10;
                           float rot = rf(0.0F, (float) (Math.PI * 2));

                           for (int ixxxx = 0; ixxxx < n; ixxxx++) {
                              Bullet ic = aimedAng(x, y, rot + (float)ixxxx * (float) (Math.PI * 2) / (float)n, 2.1F, 2.6F, 19, 10479359);
                              ic.pendingTicks = 16;
                              ic.noWarn = true;
                              out.add(ic);
                           }
                        }
                        break;
                     case 2:
                        if (u % 10 == 2) {
                           float x = Math.max((float)(bx + 6), Math.min((float)(bx + bw - 6), scx + rf(-40.0F, 40.0F)));
                           MobPatterns.Beam b = beam(x, (float)(by - 6), (float) (Math.PI / 2), (float)(bh + 12), 3.0F, 18, 4, 13);
                           b.track = 6;
                        }
                        break;
                     default:
                        if (u % 3 == 0) {
                           float[] p = edge(0, rf(0.3F, 1.15F), bx, by, bw, bh, 10.0F);
                           int[] pal = new int[]{16771168, 16751320, 9103615, 13150463, 16777215};
                           Bullet st = aimedAng(p[0], p[1], 2.1362832F + rf(-0.08F, 0.08F), rf(2.2F, 2.9F), 2.4F, 22, pal[ri(pal.length)]);
                           out.add(st);
                        }
                  }
               }
               break;
            case 321:
               int every = t > 100 ? 26 : 32;
               if (t % every == 4 && t < 186) {
                  int before = BEAMS.size();
                  bladeSweep(bx, by, bw, bh, scx, scy, 0);
                  if (BEAMS.size() > before) {
                     MobPatterns.Beam a = BEAMS.get(BEAMS.size() - 1);
                     MobPatterns.Beam m = beam(2.0F * ((float)bx + (float)bw / 2.0F) - a.ox, a.oy, (float) Math.PI - a.ang, a.len, a.half, a.warn, a.fire, 3);
                     m.sweep = true;
                     m.a0 = (float) Math.PI - a.a0;
                     m.a1 = (float) Math.PI - a.a1;
                     m.ang = m.a0;
                     m.prevAng = m.a0;
                  }
               }

               if (t % 12 == 8 && t < 196) {
                  float[] p = edge(3, rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
                  out.add(aimed(p[0], p[1], scx, scy, 3.0F, 2.8F, 40, 15265012));
                  float mx = 2.0F * ((float)bx + (float)bw / 2.0F) - p[0];
                  float tx = 2.0F * ((float)bx + (float)bw / 2.0F) - scx;
                  out.add(aimed(mx, p[1], tx, scy, 3.0F, 2.8F, 40, 15265012));
               }
         }
      }
   }

   private static void cyanWall(int t, int bx, int by, int bw, int bh, List<Bullet> out, boolean both) {
      boolean fromLeft = ri(2) == 0;

      for (int side = 0; side < (both ? 2 : 1); side++) {
         boolean l = side == 0 ? fromLeft : !fromLeft;

         for (int i = 0; i < 9; i++) {
            float y = (float)by + ((float)i + 0.5F) * (float)bh / 9.0F;
            Bullet k = mk(l ? (float)(bx - 10) : (float)(bx + bw + 10), y, l ? 4.2F : -4.2F, 0.0F, 2.8F, 34, 2803967);
            k.hue = 1;
            k.noWarn = true;
            k.pendingTicks = 10 + side * 26;
            out.add(k);
         }
      }
   }

   private static void orangeColumn(int t, int bx, int by, int bw, int bh, List<Bullet> out) {
      boolean fromLeft = ri(2) == 0;

      for (int i = 0; i < 10; i++) {
         float y = (float)by + ((float)i + 0.5F) * (float)bh / 10.0F;
         Bullet o = mk(fromLeft ? (float)(bx - 10) : (float)(bx + bw + 10), y, fromLeft ? 3.0F : -3.0F, 0.0F, 3.2F, 22, 16752680);
         o.hue = 2;
         o.noWarn = true;
         o.pendingTicks = 12;
         out.add(o);
      }
   }

   private static void darkner(int base, int v, int t, int bx, int by, int bw, int bh, float scx, float scy, float cx, float cy, List<Bullet> out) {
      int K = 41;
      int C = 16777215;
      float spd = 0.92F + (float)(v % 4) * 0.07F;
      int busy = v % 3 == 0 ? 1 : 0;
      boolean flip = v >= 5;
      switch (base) {
         case 400:
            int everyx = Math.max(3, 6 - busy - (t > 120 ? 1 : 0));
            if (t % everyx == 1 && t < 196) {
               out.add(
                  flip
                     ? mk((float)(bx - 8), (float)by + rf(4.0F, (float)(bh - 4)), rf(1.5F, 2.2F) * spd, rf(-0.3F, 0.3F), 3.4F, 41, 16777215)
                     : mk((float)bx + rf(4.0F, (float)(bw - 4)), (float)(by - 8), rf(-0.35F, 0.35F), rf(1.4F, 2.1F) * spd, 3.4F, 41, 16777215)
               );
            }

            if (t % 36 == 18 && t < 180) {
               float[] p = edge(flip ? 3 : 0, rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
               out.add(aimed(p[0], p[1], scx, scy, 2.5F * spd, 3.8F, 41, 16777215));
            }
            break;
         case 401:
            int every = 22 - busy * 5;
            if (t % every == 6 && t < 186) {
               boolean left = t / every % 2 == 0 != flip;
               Bullet b = lobTo(
                  left ? (float)(bx + 6) : (float)(bx + bw - 6),
                  (float)(by + bh + 8),
                  scx + rf(-10.0F, 10.0F),
                  scy + rf(-6.0F, 6.0F),
                  -5.0F,
                  0.13F,
                  41,
                  16777215
               );
               b.behavior = 6;
               b.count = 4 + busy * 2;
               b.p2 = 1.4F * spd;
               b.kind2 = 41;
               b.size = 3.8F;
               out.add(b);
            }
            break;
         case 402:
            if (t == 2) {
               int n = 6 + busy * 2;
               float r0 = (float)Math.min(bw, bh) * 0.33F;

               for (int i = 0; i < n; i++) {
                  float a0 = (float)i * (float) (Math.PI * 2) / (float)n;
                  Bullet b = mk(cx + (float)Math.cos((double)a0) * r0, cy + (float)Math.sin((double)a0) * r0, 0.0F, 0.0F, 3.4F, 41, 16777215);
                  b.behavior = 11;
                  b.tx = cx;
                  b.ty = cy;
                  b.p1 = r0;
                  b.p2 = (flip ? -0.032F : 0.032F) * spd;
                  b.p3 = a0;
                  b.count = 16;
                  b.timer = 196;
                  out.add(b);
               }
            }

            if (t % (28 - busy * 6) == 14 && t < 186) {
               float[] p = edge(ri(4), rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
               out.add(aimed(p[0], p[1], scx, scy, 2.2F * spd, 3.4F, 41, 16777215));
            }
            break;
         case 403:
            if (t % (30 - busy * 8) == 4 && t < 176) {
               float[] p = edge(ri(4), rf(0.2F, 0.8F), bx, by, bw, bh, 6.0F);
               float a = angTo(p[0], p[1], cx + rf(-20.0F, 20.0F), cy + rf(-15.0F, 15.0F));
               Bullet b = aimedAng(p[0], p[1], a, 2.0F * spd, 3.8F, 41, 16777215);
               b.behavior = 3;
               b.count = 3 + v % 2;
               out.add(b);
            }
            break;
         case 404:
            if (t % (34 - busy * 8) == 8 && t < 170) {
               float[] p = edge(flip ? (ri(2) == 0 ? 1 : 3) : 0, rf(0.1F, 0.9F), bx, by, bw, bh, 8.0F);
               Bullet b = aimed(p[0], p[1], scx, scy, 1.4F * spd, 4.2F, 41, 16777215);
               b.behavior = 1;
               b.timer = 56;
               b.p1 = 0.05F;
               b.p2 = 1.4F * spd;
               b.count = 5 + busy;
               b.p3 = 1.5F * spd;
               b.kind2 = 41;
               out.add(b);
            }
            break;
         case 405:
            if (t % 32 == 6 && t < 186) {
               int side = flip ? 0 : (t / 32 % 2 == 0 ? 3 : 1);
               int n = 7;
               int gap = ri(n - 1);

               for (int s = 0; s < n; s++) {
                  if (s != gap && s != gap + 1) {
                     float[] p = edge(side, ((float)s + 0.5F) / (float)n, bx, by, bw, bh, 8.0F);
                     float vx = side == 3 ? 1.3F : (side == 1 ? -1.3F : 0.0F);
                     float vy = side == 0 ? 1.2F : 0.0F;
                     out.add(mk(p[0], p[1], vx * spd, vy * spd, 3.6F, 41, 16777215));
                  }
               }
            }
            break;
         case 406:
            if (t % (3 - busy) == 0 && t > 4 && t < 192) {
               float a = (float) (Math.PI / 2) + (float)Math.sin((double)((float)t * 0.07F)) * 1.05F;
               out.add(aimedAng(cx, (float)(by - 6), a, 1.7F * spd, 3.2F, 41, 16777215));
               if (flip && t > 100 && t % 6 == 0) {
                  out.add(aimedAng(cx, (float)(by + bh + 6), -a, 1.5F * spd, 3.2F, 41, 16777215));
               }
            }
            break;
         case 407:
            if (t % 44 == 10 && t < 170) {
               bladeSweep(bx, by, bw, bh, scx, scy, 0);
            }

            if (t % (20 - busy * 4) == 3 && t < 186) {
               float[] p = edge(ri(4), rf(0.2F, 0.8F), bx, by, bw, bh, 10.0F);
               Bullet b = aimed(p[0], p[1], scx, scy, 2.6F * spd, 3.6F, 41, 16777215);
               b.p1 = 0.35F;
               out.add(b);
            }
            break;
         case 408:
            if (t % (6 - busy) == 2 && t < 186) {
               Bullet b = mk((float)bx + rf(8.0F, (float)(bw - 8)), (float)(by + bh + 6), rf(-0.7F, 0.7F) * spd, -rf(3.1F, 4.0F) * spd, 3.4F, 41, 16777215);
               b.ay = 0.07F * spd * spd;
               out.add(b);
            }
            break;
         case 409:
            if (t % (22 - busy * 5) == 4 && t < 180) {
               float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, -6.0F);
               Bullet b = mk(p[0], p[1], 0.0F, 0.0F, 3.8F, 41, 16777215);
               b.behavior = 8;
               b.timer = 24;
               b.p2 = 3.0F * spd;
               b.tx = scx;
               b.ty = scy;
               out.add(b);
            }
            break;
         case 410:
            if (t % (7 - busy * 2) == 1 && t < 190) {
               boolean fromLeft = flip ? t / 40 % 2 == 0 : true;
               Bullet b = mk(
                  fromLeft ? (float)(bx - 8) : (float)(bx + bw + 8),
                  (float)by + rf(10.0F, (float)(bh - 10)),
                  (fromLeft ? 1.5F : -1.5F) * spd,
                  0.0F,
                  3.2F,
                  41,
                  16777215
               );
               b.behavior = 7;
               b.p1 = 9.0F + (float)(v % 3) * 3.0F;
               b.p2 = 0.14F;
               out.add(b);
            }
            break;
         case 411:
            int everyxx = t > 100 ? 30 : 40;
            if (t % everyxx == 6 && t < 170) {
               int n = t > 100 && busy == 1 ? 2 : 1;

               for (int i = 0; i < n; i++) {
                  float x = (float)bx + rf(16.0F, (float)(bw - 16));
                  float y = (float)by + rf(14.0F, (float)(bh - 14));

                  for (int tries = 0; tries < 6 && Math.hypot((double)(x - scx), (double)(y - scy)) < 34.0; tries++) {
                     x = (float)bx + rf(16.0F, (float)(bw - 16));
                     y = (float)by + rf(14.0F, (float)(bh - 14));
                  }

                  Bullet b = mk(x, y, 0.0F, 0.0F, 5.5F, 41, 16777215);
                  b.behavior = 4;
                  b.timer = 38;
                  b.count = 8 + busy * 2;
                  b.p1 = 42.0F;
                  b.p2 = 1.6F * spd;
                  out.add(b);
               }
            }
      }
   }

   private static void darknerBoss(int base, int v, int t, int bx, int by, int bw, int bh, float scx, float scy, float cx, float cy, List<Bullet> out) {
      int K = 41;
      int C = 16777215;
      int k = Math.max(1, Math.min(3, v));
      switch (base) {
         case 420:
            if (t % (28 - 4 * k) == 5 && t < 188) {
               int c = ri(4);
               float x0 = (c & 1) == 0 ? (float)(bx - 6) : (float)(bx + bw + 6);
               float y0 = (c & 2) == 0 ? (float)(by - 6) : (float)(by + bh + 6);
               float a0 = angTo(x0, y0, scx, scy);
               int n = 2 + k;

               for (int ix = 0; ix < n; ix++) {
                  out.add(aimedAng(x0, y0, a0 + ((float)ix - (float)(n - 1) / 2.0F) * 0.2F, 2.2F, 3.4F, 41, 16777215));
               }
            }
            break;
         case 421:
            if (t % Math.max(2, 6 - k) == 1 && t < 194) {
               out.add(mk((float)bx + rf(4.0F, (float)(bw - 4)), (float)(by - 8), rf(-0.3F, 0.3F), rf(1.5F, 2.1F), 3.2F, 41, 16777215));
            }

            if (t % 40 == 20 && t < 170) {
               Bullet b = mk((float)bx + rf(20.0F, (float)(bw - 20)), (float)(by - 8), 0.0F, 1.2F, 5.0F, 41, 16777215);
               b.behavior = 2;
               b.timer = 38;
               b.count = 5 + k;
               b.p2 = 1.5F;
               b.kind2 = 41;
               b.p3 = 0.0F;
               out.add(b);
            }
            break;
         case 422:
            if (t == 2) {
               float m = (float)Math.min(bw, bh);

               for (int ring = 0; ring < 2; ring++) {
                  int n = ring == 0 ? 4 : 6 + k;
                  float r0 = m * (ring == 0 ? 0.2F : 0.42F);

                  for (int i = 0; i < n; i++) {
                     float a0 = (float)i * (float) (Math.PI * 2) / (float)n;
                     Bullet b = mk(cx + (float)Math.cos((double)a0) * r0, cy + (float)Math.sin((double)a0) * r0, 0.0F, 0.0F, 3.2F, 41, 16777215);
                     b.behavior = 11;
                     b.tx = cx;
                     b.ty = cy;
                     b.p1 = r0;
                     b.p2 = ring == 0 ? 0.04F : -0.026F;
                     b.p3 = a0;
                     b.count = ring == 0 ? 8 : 12;
                     b.timer = 196;
                     out.add(b);
                  }
               }
            }

            if (t % (34 - 5 * k) == 17 && t < 186) {
               float[] p = edge(ri(4), rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
               out.add(aimed(p[0], p[1], scx, scy, 2.1F, 3.4F, 41, 16777215));
            }
            break;
         case 423:
            if (t % (52 - 8 * k) == 8 && t < 168) {
               bladeSweep(bx, by, bw, bh, scx, scy, 0);
               if (k >= 3 && t > 60) {
                  bladeSweep(bx, by, bw, bh, scx, scy, 14);
               }
            }

            if (t % 10 == 3 && t < 190) {
               out.add(mk((float)bx + rf(4.0F, (float)(bw - 4)), (float)(by - 8), 0.0F, rf(1.3F, 1.8F), 3.0F, 41, 16777215));
            }
            break;
         case 424:
            if (t % (15 - 2 * k) == 2 && t < 190) {
               Bullet b = mk((float)bx + rf(16.0F, (float)(bw - 16)), (float)(by - 8), 0.0F, 1.35F, 3.4F, 41, 16777215);
               b.behavior = 7;
               b.p1 = 20.0F;
               b.p2 = 0.06F;
               out.add(b);
            }
            break;
         case 425:
            if (t % 4 == 0 && t > 4 && t < 192) {
               for (int s = 0; s < 2; s++) {
                  float x0 = s == 0 ? (float)(bx - 4) : (float)(bx + bw + 4);
                  float y0 = (float)(by - 4);
                  float a = angTo(x0, y0, cx, cy) + (float)Math.sin((double)((float)t * 0.09F + (float)s * (float) Math.PI)) * 0.75F;
                  out.add(aimedAng(x0, y0, a, 1.6F, 3.0F, 41, 16777215));
               }

               if (k >= 2 && t > 100 && t % 8 == 0) {
                  float a = (float) (-Math.PI / 2) + (float)Math.sin((double)((float)t * 0.07F)) * 0.9F;
                  out.add(aimedAng(cx, (float)(by + bh + 6), a, 1.4F, 3.0F, 41, 16777215));
               }
            }
            break;
         case 426:
            if (t % (44 - 4 * k) == 6 && t < 176) {
               int n = 8;

               for (int side = 0; side < 2; side++) {
                  int gap = ri(n - 1);

                  for (int sxx = 0; sxx < n; sxx++) {
                     if (sxx != gap && sxx != gap + 1) {
                        float[] p = edge(side == 0 ? 3 : 1, ((float)sxx + 0.5F) / (float)n, bx, by, bw, bh, 8.0F);
                        out.add(mk(p[0], p[1], side == 0 ? 1.0F : -1.0F, 0.0F, 3.2F, 41, 16777215));
                     }
                  }
               }
            }
            break;
         case 427:
            if (t % (38 - 6 * k) == 8 && t < 170) {
               float xx = (float)bx + rf(16.0F, (float)(bw - 16));
               float y = (float)by + rf(14.0F, (float)(bh - 14));

               for (int tries = 0; tries < 6 && Math.hypot((double)(xx - scx), (double)(y - scy)) < 30.0; tries++) {
                  xx = (float)bx + rf(16.0F, (float)(bw - 16));
                  y = (float)by + rf(14.0F, (float)(bh - 14));
               }

               ring(xx, y, 1.3F, 5.0F, rf(0.0F, (float) (Math.PI * 2)), 0.55F, 120.0F, 10, 16);
               int n = 5 + k;
               float a0 = rf(0.0F, (float) (Math.PI * 2));

               for (int ix = 0; ix < n; ix++) {
                  Bullet b = aimedAng(xx, y, a0 + (float)ix * (float) (Math.PI * 2) / (float)n, 1.5F, 3.0F, 41, 16777215);
                  b.pendingTicks = 16;
                  out.add(b);
               }
            }
            break;
         case 428:
            if (t % (26 - 4 * k) == 6 && t < 168) {
               float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, 8.0F);
               Bullet b = aimed(p[0], p[1], scx, scy, 1.5F, 3.8F, 41, 16777215);
               b.behavior = 1;
               b.timer = 66;
               b.p1 = 0.045F;
               b.p2 = 1.5F;
               b.count = 4;
               b.p3 = 1.5F;
               b.kind2 = 41;
               out.add(b);
            }
            break;
         case 429:
            if (t % (24 - 4 * k) == 4 && t < 160) {
               float[] p = edge(ri(4), rf(0.2F, 0.8F), bx, by, bw, bh, 6.0F);
               Bullet b = aimedAng(p[0], p[1], angTo(p[0], p[1], cx + rf(-25.0F, 25.0F), cy + rf(-18.0F, 18.0F)), 2.1F, 3.6F, 41, 16777215);
               b.behavior = 3;
               b.count = 4;
               out.add(b);
            }
            break;
         case 430:
            int phase = t % 46;
            if (phase == 1 && t < 180) {
               gx = (float)bx + rf(14.0F, (float)(bw - 14));
               gx2 = k >= 2 ? (float)bx + rf(14.0F, (float)(bw - 14)) : -1.0F;
            }

            if (phase >= 12 && phase < 22 && phase % 2 == 0 && t < 190) {
               for (float x : new float[]{gx, gx2}) {
                  if (!(x < 0.0F)) {
                     Bullet b = mk(x + rf(-3.0F, 3.0F), (float)(by + bh + 6), rf(-0.6F, 0.6F), -rf(3.3F, 4.1F), 3.2F, 41, 16777215);
                     b.ay = 0.075F;
                     out.add(b);
                  }
               }
            }

            if (t % 9 == 4 && t < 190) {
               Bullet b = mk((float)bx + rf(6.0F, (float)(bw - 6)), (float)(by + bh + 6), rf(-0.5F, 0.5F), -rf(2.6F, 3.3F), 2.8F, 41, 16777215);
               b.ay = 0.065F;
               out.add(b);
            }
            break;
         case 431:
            if (t % (46 - 6 * k) == 6 && t < 170) {
               boolean horiz = ri(2) == 0;
               int n = 1 + k;

               for (int i = 0; i < n; i++) {
                  float f = ((float)i + 0.5F) / (float)n + rf(-0.08F, 0.08F);
                  if (horiz) {
                     beam((float)(bx - 20), (float)by + f * (float)bh, 0.0F, (float)(bw + 40), 5.0F, 26, 10, 6);
                  } else {
                     beam((float)bx + f * (float)bw, (float)(by - 20), (float) (Math.PI / 2), (float)(bh + 40), 5.0F, 26, 10, 6);
                  }
               }
            }

            if (t % 14 == 7 && t < 190) {
               float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, 10.0F);
               out.add(aimed(p[0], p[1], scx, scy, 1.8F, 3.0F, 41, 16777215));
            }
            break;
         case 432:
            if (t % 26 == 4 && t < 176) {
               int n = k;

               for (int ix = 0; ix < n; ix++) {
                  float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, -6.0F);
                  Bullet b = mk(p[0], p[1], 0.0F, 0.0F, 3.8F, 41, 16777215);
                  b.behavior = 8;
                  b.timer = 22 + ix * 6;
                  b.p2 = 3.1F;
                  b.tx = scx;
                  b.ty = scy;
                  out.add(b);
               }
            }
            break;
         case 433:
            if (t % 46 == 6 && t < 160) {
               int n = 2 + k;

               for (int ix = 0; ix < n; ix++) {
                  float xx = (float)bx + rf(14.0F, (float)(bw - 14));
                  float y = (float)by + rf(12.0F, (float)(bh - 12));

                  for (int tries = 0; tries < 6 && Math.hypot((double)(xx - scx), (double)(y - scy)) < 32.0; tries++) {
                     xx = (float)bx + rf(14.0F, (float)(bw - 14));
                     y = (float)by + rf(12.0F, (float)(bh - 12));
                  }

                  Bullet b = mk(xx, y, 0.0F, 0.0F, 5.0F, 41, 16777215);
                  b.behavior = 4;
                  b.timer = 36 + ix * 5;
                  b.count = 6;
                  b.p1 = 36.0F;
                  b.p2 = 1.5F;
                  out.add(b);
               }
            }
            break;
         case 434:
            if (t % (22 - 2 * k) == 4 && t < 182) {
               int n = 9;
               int gap = Math.max(0, Math.min(n - 3, Math.round((float)(n - 3) * (0.5F + 0.5F * (float)Math.sin((double)((float)t * 0.05F))))));

               for (int sx = 0; sx < n; sx++) {
                  if (sx < gap || sx >= gap + 3) {
                     float[] p = edge(2, ((float)sx + 0.5F) / (float)n, bx, by, bw, bh, 8.0F);
                     out.add(mk(p[0], p[1], 0.0F, -1.0F, 3.0F, 41, 16777215));
                  }
               }
            }
            break;
         case 435:
            if (t % (32 - 5 * k) == 6 && t < 170) {
               float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, 10.0F);
               float tx = (float)bx + rf(16.0F, (float)(bw - 16));
               float ty = (float)by + rf(14.0F, (float)(bh - 14));
               Bullet b = aimed(p[0], p[1], tx, ty, 2.2F, 4.0F, 41, 16777215);
               b.behavior = 2;
               b.timer = 120;
               b.p3 = -1.0F;
               b.tx = tx;
               b.ty = ty;
               b.count = 7;
               b.p2 = 1.5F;
               b.kind2 = 41;
               out.add(b);
            }
            break;
         case 436:
            if (t % (30 - 5 * k) == 6 && t < 176) {
               float[] p = edge(ri(4), rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
               Bullet b = aimed(p[0], p[1], scx, scy, 1.6F, 3.6F, 41, 16777215);
               b.behavior = 5;
               b.timer = 30;
               b.p1 = 36.0F;
               b.p2 = 2.3F;
               out.add(b);
            }
            break;
         case 437:
            if (t == 8 || k >= 2 && t == 104) {
               boolean cw = t == 8;
               MobPatterns.Beam bm = beam(cx, (float)(by - 6), (float) (Math.PI / 2) + (cw ? -1.0F : 1.0F), 220.0F, 4.0F, 24, 70, 5);
               bm.rot = (float)(cw ? 1 : -1) * 0.028F;
            }

            if (t % (12 - 2 * k) == 5 && t < 190) {
               out.add(mk((float)bx + rf(4.0F, (float)(bw - 4)), (float)(by - 8), rf(-0.2F, 0.2F), rf(1.1F, 1.5F), 3.0F, 41, 16777215));
            }
            break;
         case 438:
            if (t % (34 - 4 * k) == 6 && t < 180) {
               boolean fromLeft = t / 30 % 2 == 0;
               int n = 9;
               int gap = ri(n - 2);

               for (int s = 0; s < n; s++) {
                  if (s != gap && s != gap + 1) {
                     float f = ((float)s + 0.5F) / (float)n;
                     float x0 = fromLeft ? (float)(bx - 8) : (float)(bx + bw + 8);
                     float y0 = (float)(by - 8) - f * (float)bh * 0.9F;
                     out.add(mk(x0 + (fromLeft ? f * (float)bw * 0.6F : -f * (float)bw * 0.6F), y0, fromLeft ? 1.0F : -1.0F, 1.0F, 3.0F, 41, 16777215));
                  }
               }
            }
            break;
         case 439:
            if (t < 70) {
               if (t % Math.max(3, 6 - k) == 1) {
                  out.add(mk((float)bx + rf(4.0F, (float)(bw - 4)), (float)(by - 8), rf(-0.3F, 0.3F), rf(1.5F, 2.0F), 3.0F, 41, 16777215));
               }
            } else if (t < 140) {
               if (t == 72) {
                  int n = 6 + k;
                  float r0 = (float)Math.min(bw, bh) * 0.36F;

                  for (int ix = 0; ix < n; ix++) {
                     float a0 = (float)ix * (float) (Math.PI * 2) / (float)n;
                     Bullet b = mk(cx + (float)Math.cos((double)a0) * r0, cy + (float)Math.sin((double)a0) * r0, 0.0F, 0.0F, 3.2F, 41, 16777215);
                     b.behavior = 11;
                     b.tx = cx;
                     b.ty = cy;
                     b.p1 = r0;
                     b.p2 = 0.035F;
                     b.p3 = a0;
                     b.count = 14;
                     b.timer = 66;
                     out.add(b);
                  }
               }

               if (t % 20 == 10) {
                  float[] p = edge(ri(4), rf(0.15F, 0.85F), bx, by, bw, bh, 10.0F);
                  out.add(aimed(p[0], p[1], scx, scy, 2.1F, 3.2F, 41, 16777215));
               }
            } else if (t % (24 - 4 * k) == 2 && t < 192) {
               int c = ri(4);
               float x0 = (c & 1) == 0 ? (float)(bx - 6) : (float)(bx + bw + 6);
               float y0 = (c & 2) == 0 ? (float)(by - 6) : (float)(by + bh + 6);
               float a0 = angTo(x0, y0, scx, scy);

               for (int ix = 0; ix < 3; ix++) {
                  out.add(aimedAng(x0, y0, a0 + (float)(ix - 1) * 0.22F, 2.2F, 3.2F, 41, 16777215));
               }
            }
      }
   }

   private static Bullet lobTo(float x0, float y0, float tx, float ty, float vy0, float ay, int kind, int color) {
      float apex = vy0 * vy0 / (2.0F * ay);
      float dy = Math.max(ty - y0, -apex * 0.92F);
      float disc = vy0 * vy0 + 2.0F * ay * dy;
      float time = (-vy0 + (float)Math.sqrt((double)Math.max(0.0F, disc))) / ay;
      Bullet b = mk(x0, y0, (tx - x0) / Math.max(1.0F, time), vy0, 3.2F, kind, color);
      b.ay = ay;
      b.tx = tx;
      b.ty = y0 + dy;
      return b;
   }

   private static void pounce(int bx, int by, int bw, int bh, float scx, float scy, List<Bullet> out, int col, int pause, float speed) {
      float[] p = edge(ri(4), rf(0.1F, 0.9F), bx, by, bw, bh, -4.0F);
      Bullet s = mk(p[0], p[1], 0.0F, 0.0F, 4.0F, 26, col);
      s.behavior = 8;
      s.timer = pause;
      s.p2 = speed;
      s.noWarn = true;
      s.tx = scx;
      s.ty = scy;
      s.count = 1;
      out.add(s);
   }

   private static void wardenAmbience(int t, int bx, int by, int bw, int bh, float scx, float scy, List<Bullet> out) {
      if (t % 9 == 0 && t < 200) {
         float[] p = edge(ri(4), rf(0.05F, 0.95F), bx, by, bw, bh, 8.0F);
         Bullet m = aimed(p[0], p[1], scx + rf(-40.0F, 40.0F), scy + rf(-40.0F, 40.0F), 1.2F, 2.5F, 24, 3135184);
         out.add(m);
      }
   }

   static float[][] witherHeads(int bx, int by, int bw) {
      return new float[][]{{(float)(bx + 30), (float)(by - 19)}, {(float)bx + (float)bw / 2.0F, (float)(by - 23)}, {(float)(bx + bw - 30), (float)(by - 19)}};
   }

   private static void bladeSweep(int bx, int by, int bw, int bh, float scx, float scy, int delay) {
      int e = ri(4);
      float ox;
      float oy;
      float mid;
      if (e == 0) {
         ox = (float)(bx - 2);
         oy = (float)by + rf(0.3F, 0.7F) * (float)bh;
         mid = 0.0F;
      } else if (e == 1) {
         ox = (float)(bx + bw + 2);
         oy = (float)by + rf(0.3F, 0.7F) * (float)bh;
         mid = (float) Math.PI;
      } else if (e == 2) {
         ox = (float)bx + rf(0.3F, 0.7F) * (float)bw;
         oy = (float)(by - 2);
         mid = (float) (Math.PI / 2);
      } else {
         ox = (float)bx + rf(0.3F, 0.7F) * (float)bw;
         oy = (float)(by + bh + 2);
         mid = (float) (-Math.PI / 2);
      }

      boolean cw = ri(2) == 0;
      MobPatterns.Beam b = beam(ox, oy, 0.0F, 96.0F, 5.0F, 24 + delay, 9, 3);
      b.sweep = true;
      b.a0 = mid + (cw ? -1.15F : 1.15F);
      b.a1 = mid + (cw ? 1.15F : -1.15F);
      b.ang = b.a0;
      b.prevAng = b.a0;
   }

   static void update(Bullet b, float scx, float scy, int bx, int by, int bw, int bh, List<Bullet> spawned) {
      b.age++;
      switch (b.behavior) {
         case 1:
            if (b.timer > 0) {
               b.timer--;
               float want = angTo(b.x, b.y, scx, scy);
               float cur = (float)Math.atan2((double)b.vy, (double)b.vx);
               float dx = wrap(want - cur);
               cur += Math.max(-b.p1, Math.min(b.p1, dx));
               b.vx = (float)Math.cos((double)cur) * b.p2;
               b.vy = (float)Math.sin((double)cur) * b.p2;
               if (b.timer == 0 && b.count > 0) {
                  burst(b, b.count, b.p3, b.kind2, frf(0.0F, (float) (Math.PI * 2)), spawned);
               }
            }
            break;
         case 2:
            b.timer--;
            boolean close;
            if (b.p3 < 0.0F) {
               float dx = b.tx - b.x;
               float dy = b.ty - b.y;
               close = dx * dx + dy * dy < Math.max(4.0F, b.vx * b.vx + b.vy * b.vy) * 1.2F;
            } else {
               float dx = scx - b.x;
               float dy = scy - b.y;
               close = b.p3 > 0.0F && dx * dx + dy * dy < b.p3 * b.p3;
            }

            if (b.timer <= 0 || close) {
               burst(b, b.count, b.p2, b.kind2, angTo(b.x, b.y, scx, scy), spawned);
               if (b.kind == 20) {
                  Bullet c = mk(b.x, b.y, 0.0F, 0.0F, 6.0F, 21, 11550944);
                  c.behavior = 9;
                  c.timer = 55;
                  c.p1 = 5.0F;
                  c.p2 = 11.0F;
                  c.noWarn = true;
                  c.grazed = true;
                  spawned.add(c);
               }
            }
            break;
         case 3:
            boolean hit = false;
            if (b.count > 0) {
               if (b.x < (float)bx + b.size && b.vx < 0.0F || b.x > (float)(bx + bw) - b.size && b.vx > 0.0F) {
                  b.vx = -b.vx;
                  hit = true;
               }

               if (b.y < (float)by + b.size && b.vy < 0.0F || b.y > (float)(by + bh) - b.size && b.vy > 0.0F) {
                  b.vy = -b.vy;
                  hit = true;
               }

               if (hit) {
                  b.count--;
                  if (b.count == 0 && b.p3 > 0.0F) {
                     burst(b, (int)b.p3, b.p2, b.kind2, frf(0.0F, (float) (Math.PI * 2)), spawned);
                  }
               }
            }
            break;
         case 4:
            b.vx = 0.0F;
            b.vy = 0.0F;
            if (--b.timer <= 0) {
               burst(b, b.count, b.p2, 19, frf(0.0F, (float) (Math.PI * 2)), spawned);
               MobPatterns.Ring r = ring(b.x, b.y, b.p1 / 8.0F, 6.0F, 0.0F, 0.0F, b.p1, 10, 0);
               r.r = 3.0F;
               r.prevR = 3.0F;
               b.active = false;
            }
            break;
         case 5:
            if (--b.timer <= 0) {
               float dx = b.x - scx;
               float dy = b.y - scy;
               float l = (float)Math.sqrt((double)(dx * dx + dy * dy));
               if (l < 0.01F) {
                  dx = 1.0F;
                  l = 1.0F;
               }

               float nxx = scx - dx / l * b.p1;
               float nyx = scy - dy / l * b.p1;
               b.x = Math.max((float)(bx + 4), Math.min((float)(bx + bw - 4), nxx));
               b.y = Math.max((float)(by + 4), Math.min((float)(by + bh - 4), nyx));
               b.prevX = b.x;
               b.prevY = b.y;
               float a = angTo(b.x, b.y, scx, scy);
               b.vx = (float)Math.cos((double)a) * b.p2;
               b.vy = (float)Math.sin((double)a) * b.p2;
               b.behavior = 0;
               b.pendingTicks = 10;
               b.active = false;
            }
            break;
         case 6:
            if (b.vy > 0.0F && b.y >= b.ty) {
               burst(b, b.count, b.p2, b.kind2, frf(0.0F, (float) (Math.PI * 2)), spawned);
               if (b.p3 > 0.0F) {
                  Bullet c = mk(b.x, b.y, 0.0F, 0.0F, 5.0F, 21, b.color);
                  c.behavior = 9;
                  c.timer = 64;
                  c.p1 = 5.0F;
                  c.p2 = b.p3;
                  c.noWarn = true;
                  c.grazed = true;
                  spawned.add(c);
               }
            }
            break;
         case 7:
            float sp = (float)Math.sqrt((double)(b.vx * b.vx + b.vy * b.vy));
            float nx = sp > 0.01F ? -b.vy / sp : 1.0F;
            float ny = sp > 0.01F ? b.vx / sp : 0.0F;
            float d = ((float)Math.sin((double)((float)b.age * b.p2)) - (float)Math.sin((double)((float)(b.age - 1) * b.p2))) * b.p1;
            b.x += nx * d;
            b.y += ny * d;
            break;
         case 8:
            b.vx = 0.0F;
            b.vy = 0.0F;
            if (b.timer > 7) {
               b.tx = scx;
               b.ty = scy;
            }

            if (--b.timer <= 0) {
               float a = angTo(b.x, b.y, b.tx, b.ty);
               b.vx = (float)Math.cos((double)a) * b.p2;
               b.vy = (float)Math.sin((double)a) * b.p2;
               b.behavior = 0;
            }
            break;
         case 9:
            b.vx = 0.0F;
            b.vy = 0.0F;
            float grow = Math.min(1.0F, (float)b.age / 10.0F);
            b.size = b.p1 + (b.p2 - b.p1) * grow;
            if (--b.timer <= 0) {
               b.active = false;
            }
            break;
         case 10:
            b.vx = 0.0F;
            b.vy = 0.0F;
            if (--b.timer <= 0) {
               b.active = false;
            }
            break;
         case 11:
            b.vx = 0.0F;
            b.vy = 0.0F;
            b.p3 = b.p3 + b.p2;
            float rad = b.p1 + (b.count > 0 ? (float)Math.sin((double)((float)b.age * 0.045F)) * (float)b.count : 0.0F);
            b.x = b.tx + (float)Math.cos((double)b.p3) * rad;
            b.y = b.ty + (float)Math.sin((double)b.p3) * rad;
            if (b.age == 1) {
               b.prevX = b.x;
               b.prevY = b.y;
            }

            if (--b.timer <= 0) {
               b.active = false;
            }
            break;
         case 12:
            if (--b.timer == 0) {
               b.vx = -b.vx;
               b.vy = -b.vy;
            }
            break;
         case 13:
            if (b.age % 8 == 0) {
               float cur = (float)Math.atan2((double)b.vy, (double)b.vx);
               float sp = (float)Math.sqrt((double)(b.vx * b.vx + b.vy * b.vy));
               float want = angTo(b.x, b.y, scx, scy);
               cur += wrap(want - cur) * b.p1 + frf(-0.7F, 0.7F);
               b.vx = (float)Math.cos((double)cur) * sp;
               b.vy = (float)Math.sin((double)cur) * sp;
            }

            if (--b.timer <= 0) {
               b.active = false;
            }
            break;
         case 14:
            if (b.y > (float)(by + bh) - b.size && b.vy > 0.0F) {
               b.y = (float)(by + bh) - b.size;
               if (b.count > 0 && b.size > 4.0F && b.age > 50) {
                  for (int s = -1; s <= 1; s += 2) {
                     Bullet c = mk(b.x + (float)(s * 3), b.y, (float)s * (Math.abs(b.vx) + 0.35F), -b.p1 * 0.9F, b.size * 0.66F, b.kind, b.color);
                     c.behavior = 14;
                     c.count = b.count - 1;
                     c.ay = b.ay;
                     c.p1 = b.p1 * 0.92F;
                     c.timer = b.timer;
                     c.noWarn = true;
                     c.child = true;
                     spawned.add(c);
                  }

                  b.active = false;
               } else {
                  b.vy = -b.p1 * frf(0.85F, 1.1F);
                  float spx = Math.abs(b.vx);
                  b.vx = scx < b.x ? -spx : spx;
               }

               if (b.kind == 9) {
                  for (int s = -1; s <= 1; s += 2) {
                     Bullet e = mk(b.x + (float)(s * 4), (float)(by + bh - 3), 0.0F, 0.0F, 2.4F, 30, 16742944);
                     e.behavior = 10;
                     e.timer = 22;
                     e.noWarn = true;
                     spawned.add(e);
                  }
               }
            }

            if (b.x < (float)bx + b.size && b.vx < 0.0F || b.x > (float)(bx + bw) - b.size && b.vx > 0.0F) {
               b.vx = -b.vx;
            }

            if (b.y < (float)by + b.size && b.vy < 0.0F) {
               b.vy = -b.vy;
            }

            if (--b.timer <= 0) {
               b.active = false;
            }
            break;
         default:
            if (b.kind != 26
               || b.count <= 0
               || b.age <= 4
               || !(b.x < (float)(bx + 3)) && !(b.x > (float)(bx + bw - 3)) && !(b.y < (float)(by + 3)) && !(b.y > (float)(by + bh - 3))) {
               if (b.kind == 27 && b.age % 10 == 0 && b.x > (float)bx && b.x < (float)(bx + bw) && b.y > (float)by && b.y < (float)(by + bh)) {
                  float spx = (float)Math.sqrt((double)(b.vx * b.vx + b.vy * b.vy));
                  if (spx > 0.1F) {
                     for (int sd = -1; sd <= 1; sd += 2) {
                        Bullet dx = mk(b.x, b.y, -b.vy / spx * (float)sd * 1.1F + b.vx * 0.1F, b.vx / spx * (float)sd * 1.1F + b.vy * 0.1F, 2.3F, 19, 9075302);
                        dx.noWarn = true;
                        dx.child = true;
                        spawned.add(dx);
                     }
                  }
               }
            } else {
               b.x = Math.max((float)(bx + 3), Math.min((float)(bx + bw - 3), b.x));
               b.y = Math.max((float)(by + 3), Math.min((float)(by + bh - 3), b.y));
               b.vx = 0.0F;
               b.vy = 0.0F;
               b.behavior = 8;
               b.timer = 16;
               b.count--;
               b.tx = scx;
               b.ty = scy;
            }
      }
   }

   private static float wrap(float a) {
      while (a > (float) Math.PI) {
         a -= (float) (Math.PI * 2);
      }

      while (a < (float) -Math.PI) {
         a += (float) (Math.PI * 2);
      }

      return a;
   }

   private static void burst(Bullet b, int n, float spd, int k, float a0, List<Bullet> spawned) {
      for (int i = 0; i < n; i++) {
         float a = a0 + (float)i * (float) (Math.PI * 2) / (float)n;
         Bullet c = aimedAng(b.x, b.y, a, spd, Math.max(2.3F, Math.min(3.0F, b.size * 0.55F)), k == 0 ? 19 : k, b.color);
         c.noWarn = true;
         c.child = true;
         spawned.add(c);
      }

      b.active = false;
   }

   static void tickHazards(int t, float scx, float scy) {
      for (MobPatterns.Beam b : BEAMS) {
         b.prevAng = b.ang;
         b.age++;
         if (b.track > 0 && b.age < b.warn - b.track) {
            float want = angTo(b.ox, b.oy, scx, scy);
            b.ang = b.ang + wrap(want - b.ang) * 0.3F;
         }

         if (b.sweep) {
            float q = b.age < b.warn ? 0.0F : Math.min(1.0F, (float)(b.age - b.warn) / (float)b.fire);
            float e = 1.0F - (1.0F - q) * (1.0F - q);
            b.ang = b.a0 + (b.a1 - b.a0) * e;
         }

         if (b.rot != 0.0F) {
            b.ang = b.ang + b.rot;
         }
      }

      BEAMS.removeIf(MobPatterns.Beam::dead);

      for (MobPatterns.Ring r : RINGS) {
         r.prevR = r.r;
         r.age++;
         if (r.live()) {
            r.r = r.r + r.speed;
         }
      }

      RINGS.removeIf(rx -> rx.style == 12 && rx.age > 16 || rx.style != 12 && rx.r > rx.maxR + 2.0F);
      prevDarkness = darkness;
      if (activeBase != 260 && activeBase != 261) {
         darkness = 0.0F;
      } else {
         float beat = (float)(t % 36) / 36.0F;
         float pulse = beat < 0.12F ? beat / 0.12F : (float)Math.pow((double)(1.0F - (beat - 0.12F) / 0.88F), 1.6);
         darkness = Math.min(1.0F, (float)t / 30.0F) * (0.2F + 0.35F * pulse);
      }
   }

   static boolean hazardGraze(float scx, float scy, float grazeR) {
      return hazardGraze(scx, scy, grazeR, false);
   }

   static boolean hazardGraze(float scx, float scy, float grazeR, boolean soulMoving) {
      boolean any = false;

      for (MobPatterns.Beam b : BEAMS) {
         if (!b.grazed && b.firing() && !hueHarmless(b.hue, soulMoving)) {
            float dx = (float)Math.cos((double)b.ang);
            float dy = (float)Math.sin((double)b.ang);
            float px = scx - b.ox;
            float py = scy - b.oy;
            float along = px * dx + py * dy;
            if (!(along < 0.0F) && !(along > b.len) && Math.abs(px * dy - py * dx) < b.half + grazeR) {
               b.grazed = true;
               any = true;
            }
         }
      }

      for (MobPatterns.Ring r : RINGS) {
         if (!r.grazed && r.live() && r.style != 12) {
            float dx = scx - r.cx;
            float dy = scy - r.cy;
            if (Math.abs((float)Math.sqrt((double)(dx * dx + dy * dy)) - r.r) < r.thick / 2.0F + grazeR) {
               r.grazed = true;
               any = true;
            }
         }
      }

      return any;
   }

   static boolean hazardHit(float scx, float scy) {
      return hazardHit(scx, scy, false);
   }

   static boolean hazardHit(float scx, float scy, boolean soulMoving) {
      float soulR = 2.2F;

      for (MobPatterns.Beam b : BEAMS) {
         if (b.firing() && !hueHarmless(b.hue, soulMoving)) {
            float dx = (float)Math.cos((double)b.ang);
            float dy = (float)Math.sin((double)b.ang);
            float px = scx - b.ox;
            float py = scy - b.oy;
            float along = px * dx + py * dy;
            if (!(along < 0.0F) && !(along > b.len)) {
               float perp = Math.abs(px * dy - py * dx);
               if (perp < b.half + 2.2F) {
                  return true;
               }
            }
         }
      }

      Iterator var12 = RINGS.iterator();

      while (true) {
         if (!var12.hasNext()) {
            return false;
         }

         MobPatterns.Ring r = (MobPatterns.Ring)var12.next();
         if (r.live() && r.style != 12) {
            float dx = scx - r.cx;
            float dy = scy - r.cy;
            float d = (float)Math.sqrt((double)(dx * dx + dy * dy));
            if (!(Math.abs(d - r.r) > r.thick / 2.0F + 2.2F)) {
               if (!(r.gapHalf > 0.0F)) {
                  break;
               }

               float gapArc = r.gapHalf * Math.max(1.0F, 30.0F / Math.max(10.0F, r.r));
               if (!(Math.abs(wrap((float)Math.atan2((double)dy, (double)dx) - r.gapAng)) < gapArc)) {
                  break;
               }
            }
         }
      }

      return true;
   }

   static final class Beam {
      float ox;
      float oy;
      float ang;
      float len;
      float half;
      int warn;
      int fire;
      int fade = 6;
      int age;
      int style;
      int track;
      float rot;
      boolean sweep;
      float a0;
      float a1;
      float prevAng;
      boolean grazed;
      int hue;

      boolean firing() {
         return this.age >= this.warn && this.age < this.warn + this.fire;
      }

      boolean dead() {
         return this.age >= this.warn + this.fire + this.fade;
      }
   }

   static final class Ring {
      float cx;
      float cy;
      float r;
      float speed;
      float thick;
      float gapAng;
      float gapHalf;
      float maxR;
      int style;
      int age;
      int delay;
      float prevR;
      boolean grazed;

      boolean live() {
         return this.age >= this.delay;
      }
   }
}
