package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class CastAnims {
   static final int RP = 0;
   static final int RY = 1;
   static final int RR = 2;
   static final int LP = 3;
   static final int LY = 4;
   static final int LR = 5;
   static final int HP = 6;
   static final int HY = 7;
   static final int BY = 8;
   static final int RL = 9;
   static final int LL = 10;
   static final int DX = 11;
   static final int DY = 12;
   static final int TL = 13;
   static final int SX = 14;
   static final int SY = 15;
   static final int NCH = 16;
   static final int SINE = 0;
   static final int SNAP = 1;
   static final int BACK = 2;
   static final int LIN = 3;
   static final float[][] T_RUDE_BUSTER;
   static final float[][] T_ICE_SHOCK;
   static final float[][] T_GLITTER_BOMB;
   static final float[][] T_SNOWGRAVE;
   static final float[][] T_X_SLASH;
   static final float[][] T_GIGAFLARE;
   static final float[][] T_HEAL_PRAYER;
   static final float[][] T_DUAL_HEAL;
   static final float[][] T_ANGELIC_HALO;
   static final float[][] T_WARD;
   static final float[][] T_GREAT_SHIELD;
   static final float[][] T_LULLABY;
   static final float[][] T_SHINING_SOUL;
   static final float[][] T_PACIFY;
   static final float[][] T_SLEEP_MIST;
   static final float[][] T_LIFEUP;
   static final float[][] T_PSI_FIRE;
   static final float[][] T_PSI_FREEZE;
   static final float[][] T_PSI_THUNDER;
   static final float[][] T_PSI_SHIELD;
   static final float[][] T_HYPNOSIS;
   static final float[][] T_PSI_MAGNET;
   static final float[][] T_BRAINSHOCK;
   static final float[][] T_PSI_ROCKIN;
   static final float[][] T_STARSTORM;
   static final float[][] T_OFFENSE_UP;
   static final float[][] T_SPY;
   static final int BUSTER_PURPLE = 11824383;
   static final int BUSTER_RED = 16726586;
   static final int ICE = 11070719;
   static final int ICE_DEEP = 5811967;
   static final int HEAL = 6750088;
   static final int GOLD = 16767082;
   static final int WARD_BLUE = 8830719;
   static final int IRON = 11845844;
   static final int PASTEL_PINK = 16757986;
   static final int PASTEL_BLUE = 11851519;
   static final int LAVENDER = 13480191;
   static final int MIST = 14477055;

   private CastAnims() {
   }

   public static CastAnims.Cast castFor(BattleSkill s) {
      if (s instanceof LightnerSkill l) {
         return switch (l) {
            case RUDE_BUSTER, RUDE_BUSTER_PLUS, RED_BUSTER -> CastAnims.Cast.RUDE_BUSTER;
            case ICE_SHOCK -> CastAnims.Cast.ICE_SHOCK;
            case GLITTER_BOMB -> CastAnims.Cast.GLITTER_BOMB;
            case SNOWGRAVE -> CastAnims.Cast.SNOWGRAVE;
            case X_SLASH -> CastAnims.Cast.X_SLASH;
            case GIGAFLARE -> CastAnims.Cast.GIGAFLARE;
            case HEAL_PRAYER -> CastAnims.Cast.HEAL_PRAYER;
            case DUAL_HEAL -> CastAnims.Cast.DUAL_HEAL;
            case ANGELIC_HALO -> CastAnims.Cast.ANGELIC_HALO;
            case WARD -> CastAnims.Cast.WARD;
            case GREAT_SHIELD -> CastAnims.Cast.GREAT_SHIELD;
            case LULLABY -> CastAnims.Cast.LULLABY;
            case SHINING_SOUL -> CastAnims.Cast.SHINING_SOUL;
            case PACIFY -> CastAnims.Cast.PACIFY;
            case C_ACTION -> CastAnims.Cast.SLEEP_MIST;
         };
      } else if (s instanceof PsiSkill p) {
         return switch (p) {
            case LIFEUP_A, LIFEUP_B, LIFEUP_G, LIFEUP_O -> CastAnims.Cast.LIFEUP;
            case FIRE_A, FIRE_B, FIRE_G, FIRE_O -> CastAnims.Cast.PSI_FIRE;
            case FREEZE_A, FREEZE_B, FREEZE_G, FREEZE_O -> CastAnims.Cast.PSI_FREEZE;
            case THUNDER_A, THUNDER_B, THUNDER_G, THUNDER_O -> CastAnims.Cast.PSI_THUNDER;
            case SHIELD_A, SHIELD_B -> CastAnims.Cast.PSI_SHIELD;
            case HYPNOSIS_A, HYPNOSIS_B -> CastAnims.Cast.HYPNOSIS;
            case MAGNET -> CastAnims.Cast.PSI_MAGNET;
            case BRAINSHOCK -> CastAnims.Cast.BRAINSHOCK;
            case ROCKIN_A, ROCKIN_B, ROCKIN_G, ROCKIN_O -> CastAnims.Cast.PSI_ROCKIN;
            case STARSTORM_A, STARSTORM_O -> CastAnims.Cast.STARSTORM;
            case OFFENSE_A, OFFENSE_O -> CastAnims.Cast.OFFENSE_UP;
            case SPY -> CastAnims.Cast.SPY;
         };
      } else {
         return CastAnims.Cast.GENERIC;
      }
   }

   static CastAnims.K K(float p, int ease) {
      return new CastAnims.K(p, ease);
   }

   static float[][] table(CastAnims.K... ks) {
      float[][] t = new float[ks.length][];

      for (int i = 0; i < ks.length; i++) {
         t[i] = ks[i].v;
      }

      return t;
   }

   private static float ease(int e, float x) {
      return switch (e) {
         case 1 -> CloneAnimator.easeOutCubic(x);
         case 2 -> CloneAnimator.easeOutBack(x);
         case 3 -> x;
         default -> CloneAnimator.easeInOutSine(x);
      };
   }

   static void sample(float[][] k, float p, CloneAnimator.Pose o) {
      int i = 0;

      while (i < k.length - 2 && p >= k[i + 1][0]) {
         i++;
      }

      float[] a = k[i];
      float[] b = k[Math.min(i + 1, k.length - 1)];
      float span = b[0] - a[0];
      float f = span <= 0.0F ? 1.0F : ease((int)b[1], CloneAnimator.clamp01((p - a[0]) / span));
      float[] c = new float[16];

      for (int ch = 0; ch < 16; ch++) {
         c[ch] = CloneAnimator.mix(a[2 + ch], b[2 + ch], f);
      }

      o.armW = 1.0F;
      o.headW = 1.0F;
      o.bodyW = 1.0F;
      o.legW = 1.0F;
      o.rArmP = c[0];
      o.rArmY = c[1];
      o.rArmR = c[2];
      o.lArmP = c[3];
      o.lArmY = c[4];
      o.lArmR = c[5];
      o.headP = c[6];
      o.headY = c[7];
      o.bodyY = c[8];
      o.rLegP = c[9];
      o.lLegP = c[10];
      o.dx = c[11];
      o.dy = c[12];
      o.tilt = c[13];
      o.sx = c[14];
      o.sy = c[15];
   }

   static float win(float p, float a, float b, float r) {
      return CloneAnimator.easeInOutSine(CloneAnimator.seg(p, a, a + r)) * (1.0F - CloneAnimator.easeInOutSine(CloneAnimator.seg(p, b - r, b)));
   }

   static float[][] tableFor(CastAnims.Cast c) {
      return switch (c) {
         case RUDE_BUSTER -> T_RUDE_BUSTER;
         case ICE_SHOCK -> T_ICE_SHOCK;
         case GLITTER_BOMB -> T_GLITTER_BOMB;
         case SNOWGRAVE -> T_SNOWGRAVE;
         case X_SLASH -> T_X_SLASH;
         case GIGAFLARE -> T_GIGAFLARE;
         case HEAL_PRAYER -> T_HEAL_PRAYER;
         case DUAL_HEAL -> T_DUAL_HEAL;
         case ANGELIC_HALO -> T_ANGELIC_HALO;
         case WARD -> T_WARD;
         case GREAT_SHIELD -> T_GREAT_SHIELD;
         case LULLABY -> T_LULLABY;
         case SHINING_SOUL -> T_SHINING_SOUL;
         case PACIFY -> T_PACIFY;
         case SLEEP_MIST -> T_SLEEP_MIST;
         case LIFEUP -> T_LIFEUP;
         case PSI_FIRE -> T_PSI_FIRE;
         case PSI_FREEZE -> T_PSI_FREEZE;
         case PSI_THUNDER -> T_PSI_THUNDER;
         case PSI_SHIELD -> T_PSI_SHIELD;
         case HYPNOSIS -> T_HYPNOSIS;
         case PSI_MAGNET -> T_PSI_MAGNET;
         case BRAINSHOCK -> T_BRAINSHOCK;
         case PSI_ROCKIN -> T_PSI_ROCKIN;
         case STARSTORM -> T_STARSTORM;
         case OFFENSE_UP -> T_OFFENSE_UP;
         case SPY -> T_SPY;
         default -> null;
      };
   }

   static void pose(CastAnims.Cast c, CloneAnimator.Pose o, float p, float t) {
      float[][] tab = tableFor(c);
      if (tab != null) {
         sample(tab, p, o);
         switch (c) {
            case RUDE_BUSTER: {
               float ch = win(p, 0.16F, 0.5F, 0.08F) * CloneAnimator.seg(p, 0.16F, 0.48F);
               o.dx = o.dx + CloneAnimator.jitter(t, 1) * 0.7F * ch;
               o.rArmP = o.rArmP + CloneAnimator.jitter(t, 2) * 0.05F * ch;
               break;
            }
            case ICE_SHOCK: {
               float w = win(p, 0.25F, 0.5F, 0.05F);
               o.rArmR = o.rArmR + CloneAnimator.jitter(t, 3) * 0.03F * w;
               o.lArmR = o.lArmR - CloneAnimator.jitter(t, 15) * 0.03F * w;
            }
            case GLITTER_BOMB:
            case X_SLASH:
            case GREAT_SHIELD:
            case SHINING_SOUL:
            case PACIFY:
            case LIFEUP:
            case PSI_FREEZE:
            case PSI_SHIELD:
            case PSI_ROCKIN:
            default:
               break;
            case SNOWGRAVE:
               float tr = win(p, 0.5F, 0.86F, 0.1F);
               o.rArmP = o.rArmP + CloneAnimator.jitter(t, 4) * 0.025F * tr;
               o.lArmP = o.lArmP + CloneAnimator.jitter(t, 16) * 0.025F * tr;
               o.dy = o.dy + (float)Math.sin((double)(t * 0.0021F)) * 0.8F * win(p, 0.3F, 0.86F, 0.1F);
               break;
            case GIGAFLARE: {
               float ch = win(p, 0.2F, 0.74F, 0.1F) * CloneAnimator.seg(p, 0.2F, 0.72F);
               o.dx = o.dx + CloneAnimator.jitter(t, 5) * 0.9F * ch;
               o.dy = o.dy + CloneAnimator.jitter(t, 6) * 0.5F * ch;
               break;
            }
            case HEAL_PRAYER:
            case DUAL_HEAL:
               o.sy = o.sy + (float)Math.sin((double)(t * 0.006F)) * 0.008F * win(p, 0.2F, 0.7F, 0.08F);
               break;
            case ANGELIC_HALO:
               o.dy = o.dy + (float)Math.sin((double)(t * 0.005F)) * 1.2F * win(p, 0.3F, 0.82F, 0.1F);
               break;
            case WARD:
               float th = CloneAnimator.easeInOutSine(CloneAnimator.seg(p, 0.2F, 0.6F)) * (float) (Math.PI * 2);
               float k = win(p, 0.18F, 0.62F, 0.04F) * 0.52F;
               o.rArmP = o.rArmP + (float)Math.sin((double)th) * k;
               o.rArmY = o.rArmY + ((float)Math.cos((double)th) - 1.0F) * k * 0.9F;
               break;
            case LULLABY: {
               float w = win(p, 0.18F, 0.88F, 0.12F);
               float s = (float)Math.sin((double)(t * 0.0055F));
               o.tilt += s * 4.0F * w;
               o.dx += s * 1.2F * w;
               o.headY += s * 0.18F * w;
               o.lArmR = o.lArmR + (float)Math.sin((double)(t * 0.0055F + 1.0F)) * 0.12F * w;
               o.dy = o.dy + -Math.abs((float)Math.cos((double)(t * 0.0055F))) * 0.8F * w;
               break;
            }
            case SLEEP_MIST: {
               float w = win(p, 0.22F, 0.74F, 0.08F);
               float a = t * 0.009F;
               o.rArmP = o.rArmP + (float)Math.sin((double)a) * 0.2F * w;
               o.rArmY = o.rArmY + (float)Math.cos((double)a) * 0.2F * w;
               o.lArmP = o.lArmP + (float)Math.sin((double)(a + 1.6F)) * 0.2F * w;
               o.lArmY = o.lArmY - (float)Math.cos((double)(a + 1.6F)) * 0.2F * w;
               o.tilt = o.tilt + (float)Math.sin((double)(a * 0.5F)) * 1.5F * w;
               break;
            }
            case PSI_FIRE:
               o.headP = o.headP + CloneAnimator.jitter(t, 7) * 0.03F * win(p, 0.18F, 0.36F, 0.04F);
               break;
            case PSI_THUNDER: {
               float w = win(p, 0.24F, 0.76F, 0.06F);
               o.rArmP = o.rArmP + CloneAnimator.jitter(t, 8) * 0.04F * w;
               o.headY = o.headY + (float)Math.sin((double)(t * 0.004F)) * 0.12F * w;
               o.dx = o.dx + CloneAnimator.jitter(t, 9) * 0.4F * w;
               break;
            }
            case HYPNOSIS: {
               float w = win(p, 0.24F, 0.86F, 0.06F);
               float a = t * (0.006F + 0.006F * CloneAnimator.seg(p, 0.25F, 0.85F));
               o.rArmP = o.rArmP + (float)Math.sin((double)a) * 0.22F * w;
               o.rArmY = o.rArmY + ((float)Math.cos((double)a) - 1.0F) * 0.22F * w;
               o.headY = o.headY + (float)Math.sin((double)(a * 0.5F)) * 0.1F * w;
               break;
            }
            case PSI_MAGNET:
               o.dx = o.dx + CloneAnimator.jitter(t, 10) * 0.5F * win(p, 0.3F, 0.78F, 0.08F);
               break;
            case BRAINSHOCK: {
               float w = win(p, 0.08F, 0.47F, 0.06F) * CloneAnimator.seg(p, 0.08F, 0.45F);
               o.dx = o.dx + CloneAnimator.jitter(t, 11) * 1.0F * w;
               o.headP = o.headP + CloneAnimator.jitter(t, 12) * 0.06F * w;
               break;
            }
            case STARSTORM:
               o.dy = o.dy + (float)Math.sin((double)(t * 0.004F)) * 1.4F * win(p, 0.3F, 0.84F, 0.1F);
               break;
            case OFFENSE_UP: {
               float w = win(p, 0.44F, 0.8F, 0.06F);
               o.dx = o.dx + CloneAnimator.jitter(t, 13) * 0.6F * w;
               o.sy = o.sy + CloneAnimator.jitter(t, 14) * 0.008F * w;
               break;
            }
            case SPY: {
               float w = win(p, 0.3F, 0.84F, 0.08F);
               o.headY = o.headY + (float)Math.sin((double)CloneAnimator.seg(p, 0.3F, 0.84F) * Math.PI * 3.0) * 0.32F * w;
               o.tilt = o.tilt + (float)Math.sin((double)CloneAnimator.seg(p, 0.3F, 0.84F) * Math.PI * 3.0) * 1.5F * w;
            }
         }
      }
   }

   static float unit(float hPx) {
      return hPx / 30.72F;
   }

   static void project(CloneAnimator.Pose o, float mx, float my, float mz, float hPx, int dir, float[] out) {
      float k = unit(hPx);
      float wx = -mx;
      float x = -0.70710677F * (wx + (float)dir * mz) * k;
      float y = (my - 24.0F) * k;
      x *= o.sx;
      y *= o.sy;
      float a = (float)Math.toRadians((double)(o.tilt * (float)dir));
      float c = (float)Math.cos((double)a);
      float s = (float)Math.sin((double)a);
      out[0] = x * c - y * s + o.dx * (float)dir;
      out[1] = x * s + y * c + o.dy;
   }

   static void armPt(CloneAnimator.Pose o, boolean right, float ax, float ay, float az, float hPx, int dir, float[] out) {
      float by = o.bodyW > 0.001F ? o.bodyY * Math.min(1.0F, o.bodyW) : 0.0F;
      float w = Math.min(1.0F, o.armW);
      float pitch = (right ? o.rArmP : o.lArmP) * w;
      float yaw = (right ? o.rArmY : o.lArmY) * w + by;
      float roll = (right ? o.rArmR : o.lArmR) * w;
      float sb = (float)Math.sin((double)by);
      float cb = (float)Math.cos((double)by);
      float px = right ? -cb * 5.0F : cb * 5.0F;
      float py = 2.0F;
      float pz = right ? sb * 5.0F : -sb * 5.0F;
      float cp = (float)Math.cos((double)pitch);
      float sp = (float)Math.sin((double)pitch);
      float y1 = ay * cp - az * sp;
      float z1 = ay * sp + az * cp;
      float cy = (float)Math.cos((double)yaw);
      float sy = (float)Math.sin((double)yaw);
      float x2 = ax * cy + z1 * sy;
      float z2 = -ax * sy + z1 * cy;
      float cr = (float)Math.cos((double)roll);
      float sr = (float)Math.sin((double)roll);
      float x3 = x2 * cr - y1 * sr;
      float y3 = x2 * sr + y1 * cr;
      project(o, px + x3, py + y3, pz + z2, hPx, dir, out);
   }

   static void hand(CloneAnimator.Pose o, boolean right, float hPx, int dir, float[] out) {
      armPt(o, right, right ? -1.0F : 1.0F, 10.0F, -0.5F, hPx, dir, out);
   }

   static void headPt(CloneAnimator.Pose o, float hx, float hy, float hz, float hPx, int dir, float[] out) {
      float w = Math.min(1.0F, o.headW);
      float pitch = o.headP * w;
      float yaw = o.headY * w;
      float cp = (float)Math.cos((double)pitch);
      float sp = (float)Math.sin((double)pitch);
      float y1 = hy * cp - hz * sp;
      float z1 = hy * sp + hz * cp;
      float cy = (float)Math.cos((double)yaw);
      float sy = (float)Math.sin((double)yaw);
      project(o, hx * cy + z1 * sy, y1, -hx * sy + z1 * cy, hPx, dir, out);
   }

   static void fx(DrawContext ctx, CloneAnimator.Frame fr, CastAnims.Cast c, float feetX, float feetY, float hPx, int dir, boolean front) {
      CastAnims.Fx f = new CastAnims.Fx(ctx, fr, c, feetX, feetY, hPx, dir, front);
      switch (c) {
         case RUDE_BUSTER:
            CastFx.rudeBuster(f);
            break;
         case ICE_SHOCK:
            CastFx.iceShock(f);
            break;
         case GLITTER_BOMB:
            CastFx.glitterBomb(f);
            break;
         case SNOWGRAVE:
            CastFx.snowgrave(f);
            break;
         case X_SLASH:
            CastFx.xSlash(f);
            break;
         case GIGAFLARE:
            CastFx.gigaflare(f);
            break;
         case HEAL_PRAYER:
            CastFx.healPrayer(f);
            break;
         case DUAL_HEAL:
            CastFx.dualHeal(f);
            break;
         case ANGELIC_HALO:
            CastFx.angelicHalo(f);
            break;
         case WARD:
            CastFx.ward(f);
            break;
         case GREAT_SHIELD:
            CastFx.greatShield(f);
            break;
         case LULLABY:
            CastFx.lullaby(f);
            break;
         case SHINING_SOUL:
            CastFx.shiningSoul(f);
            break;
         case PACIFY:
            CastFx.pacify(f);
            break;
         case SLEEP_MIST:
            CastFx.sleepMist(f);
            break;
         case LIFEUP:
            CastFx.lifeup(f);
            break;
         case PSI_FIRE:
            CastFx.psiFire(f);
            break;
         case PSI_FREEZE:
            CastFx.psiFreeze(f);
            break;
         case PSI_THUNDER:
            CastFx.psiThunder(f);
            break;
         case PSI_SHIELD:
            CastFx.psiShield(f);
            break;
         case HYPNOSIS:
            CastFx.hypnosis(f);
            break;
         case PSI_MAGNET:
            CastFx.psiMagnet(f);
            break;
         case BRAINSHOCK:
            CastFx.brainshock(f);
            break;
         case PSI_ROCKIN:
            CastFx.psiRockin(f);
            break;
         case STARSTORM:
            CastFx.starstorm(f);
            break;
         case OFFENSE_UP:
            CastFx.offenseUp(f);
            break;
         case SPY:
            CastFx.spy(f);
      }
   }

   static {
      CastAnims.K n0 = K(0.0F, 0);
      CastAnims.K n1 = K(1.0F, 0);
      CastAnims.K rbUp = K(0.18F, 1)
         .r(-2.9F, 0.2F, 0.4F)
         .l(-1.0F, 0.3F, -0.1F)
         .head(-0.12F, 0.0F)
         .body(0.45F)
         .legs(0.35F, -0.4F)
         .move(-4.0F, 0.0F, -7.0F)
         .scale(1.04F, 0.94F);
      CastAnims.K rbHold = K(0.4F, 0).like(rbUp).r(-3.1F, 0.2F, 0.45F).move(-5.0F, 0.0F, -8.0F).scale(1.05F, 0.92F);
      CastAnims.K rbSwing = K(0.5F, 1)
         .r(-0.3F, -0.7F, -0.1F)
         .l(0.6F, 0.0F, -0.4F)
         .head(0.2F, 0.0F)
         .body(-0.55F)
         .legs(-0.6F, 0.5F)
         .move(14.0F, -2.0F, 14.0F)
         .scale(1.08F, 0.95F);
      CastAnims.K rbFollow = K(0.64F, 0).like(rbSwing).r(-0.15F, -0.75F, -0.1F).move(12.0F, 0.0F, 9.0F).scale(1.0F, 1.0F);
      CastAnims.K rbBack = K(0.84F, 0).like(rbFollow).move(4.0F, 0.0F, 3.0F).r(-0.2F, -0.4F, 0.0F).body(-0.2F).legs(-0.2F, 0.2F);
      T_RUDE_BUSTER = table(n0, rbUp, rbHold, rbSwing, rbFollow, rbBack, n1);
      CastAnims.K isUp = K(0.25F, 1).r(-2.75F, 0.0F, 0.35F).l(-2.75F, 0.0F, -0.35F).head(-0.3F, 0.0F).move(0.0F, -1.0F, -2.0F).scale(0.98F, 1.03F);
      CastAnims.K isHold = K(0.47F, 0).like(isUp).r(-2.85F, 0.0F, 0.4F).l(-2.85F, 0.0F, -0.4F).move(0.0F, -1.5F, -2.0F);
      CastAnims.K isFlick = K(0.55F, 1).r(-1.55F, -0.25F, 0.0F).l(-1.55F, 0.25F, 0.0F).head(0.0F, 0.0F).body(-0.15F).legs(-0.25F, 0.2F).move(4.0F, 0.0F, 5.0F);
      CastAnims.K isRest = K(0.8F, 0).like(isFlick).move(3.0F, 0.0F, 3.0F);
      T_ICE_SHOCK = table(n0, isUp, isHold, isFlick, isRest, n1);
      CastAnims.K gbBack = K(0.25F, 0)
         .r(0.65F, 0.1F, 0.3F)
         .l(-1.1F, 0.2F, 0.0F)
         .head(-0.05F, 0.0F)
         .body(0.4F)
         .legs(0.35F, -0.35F)
         .move(-3.0F, 0.0F, -5.0F)
         .scale(1.02F, 0.97F);
      CastAnims.K gbThrow = K(0.42F, 1).r(-2.4F, -0.3F, 0.0F).l(0.4F, 0.0F, -0.2F).head(-0.25F, 0.0F).body(-0.4F).legs(-0.45F, 0.4F).move(6.0F, -1.0F, 8.0F);
      CastAnims.K gbFollow = K(0.56F, 0).like(gbThrow).r(-1.2F, -0.6F, 0.0F).head(-0.15F, 0.0F).move(6.0F, 0.0F, 6.0F);
      CastAnims.K gbWatch = K(0.8F, 0).like(gbFollow).r(-0.6F, -0.3F, 0.0F).head(-0.25F, 0.0F).move(3.0F, 0.0F, 2.0F);
      T_GLITTER_BOMB = table(n0, gbBack, gbThrow, gbFollow, gbWatch, n1);
      CastAnims.K sgBow = K(0.16F, 0).r(0.0F, 0.0F, 0.15F).l(0.0F, 0.0F, -0.15F).head(0.55F, 0.0F).move(0.0F, 1.0F, 0.0F).scale(1.0F, 0.98F);
      CastAnims.K sgRise = K(0.55F, 0).like(sgBow).r(-2.95F, 0.0F, 0.12F).l(-2.95F, 0.0F, -0.12F).move(0.0F, -3.0F, 0.0F).scale(1.0F, 1.01F);
      CastAnims.K sgLook = K(0.72F, 0).like(sgRise).head(-0.05F, 0.0F).move(0.0F, -6.0F, 0.0F);
      CastAnims.K sgHold = K(0.84F, 0).like(sgLook).r(-3.05F, 0.0F, 0.14F).l(-3.05F, 0.0F, -0.14F).move(0.0F, -7.0F, 0.0F);
      CastAnims.K sgDrop = K(0.88F, 1).r(-1.3F, -0.15F, 0.0F).l(-1.3F, 0.15F, 0.0F).head(0.05F, 0.0F).move(3.0F, -6.0F, 4.0F);
      CastAnims.K sgEnd = K(0.96F, 0).like(sgDrop).move(2.0F, -1.0F, 2.0F);
      T_SNOWGRAVE = table(n0, sgBow, sgRise, sgLook, sgHold, sgDrop, sgEnd, n1);
      CastAnims.K xsReady = K(0.15F, 1).r(-2.6F, 0.1F, 0.5F).l(-0.7F, 0.3F, 0.0F).body(0.3F).legs(0.3F, -0.35F).move(-2.0F, 1.0F, -4.0F).scale(1.03F, 0.93F);
      CastAnims.K xsCut1 = K(0.3F, 1)
         .r(-0.4F, -0.7F, -0.1F)
         .l(0.4F, 0.0F, -0.3F)
         .head(0.15F, 0.0F)
         .body(-0.45F)
         .legs(-0.6F, 0.5F)
         .move(18.0F, 0.0F, 10.0F)
         .scale(1.06F, 0.96F);
      CastAnims.K xsRaise = K(0.41F, 0).r(-2.4F, -0.9F, -0.3F).l(0.2F, 0.0F, -0.3F).body(-0.6F).legs(-0.5F, 0.45F).move(20.0F, -1.0F, 6.0F);
      CastAnims.K xsCut2 = K(0.52F, 1)
         .r(-0.5F, 0.4F, 0.5F)
         .l(0.3F, 0.0F, -0.4F)
         .head(0.15F, 0.0F)
         .body(0.35F)
         .legs(-0.55F, 0.5F)
         .move(22.0F, 0.0F, 12.0F)
         .scale(1.06F, 0.96F);
      CastAnims.K xsHold = K(0.66F, 0).like(xsCut2).move(21.0F, 0.0F, 8.0F).scale(1.0F, 1.0F);
      CastAnims.K xsBack = K(0.88F, 0).r(-0.3F, 0.1F, 0.2F).legs(0.1F, -0.1F).move(2.0F, -2.0F, -2.0F);
      T_X_SLASH = table(n0, xsReady, xsCut1, xsRaise, xsCut2, xsHold, xsBack, n1);
      CastAnims.K gfUp = K(0.2F, 0).r(-2.9F, 0.0F, 0.3F).l(-2.9F, 0.0F, -0.3F).head(-0.4F, 0.0F).move(0.0F, -2.0F, -2.0F).scale(0.98F, 1.03F);
      CastAnims.K gfCharge = K(0.66F, 0).like(gfUp).r(-2.75F, 0.0F, 0.55F).l(-2.75F, 0.0F, -0.55F).move(-1.0F, -8.0F, -4.0F);
      CastAnims.K gfHurl = K(0.74F, 1)
         .r(-1.55F, -0.2F, 0.0F)
         .l(-1.55F, 0.2F, 0.0F)
         .head(0.05F, 0.0F)
         .body(-0.1F)
         .legs(-0.3F, 0.3F)
         .move(6.0F, -5.0F, 10.0F)
         .scale(1.04F, 0.97F);
      CastAnims.K gfHold = K(0.86F, 0).like(gfHurl).move(5.0F, -2.0F, 6.0F).scale(1.0F, 1.0F);
      T_GIGAFLARE = table(n0, gfUp, gfCharge, gfHurl, gfHold, n1);
      CastAnims.K hpPray = K(0.24F, 0)
         .r(-1.25F, -0.75F, 0.0F)
         .l(-1.25F, 0.75F, 0.0F)
         .head(0.45F, 0.0F)
         .legs(0.05F, -0.05F)
         .move(0.0F, 1.0F, 1.0F)
         .scale(1.01F, 0.97F);
      CastAnims.K hpHold = K(0.7F, 0).like(hpPray).head(0.5F, 0.0F).move(0.0F, 1.2F, 1.5F);
      CastAnims.K hpLift = K(0.84F, 0).r(-1.7F, -0.3F, 0.3F).l(-1.7F, 0.3F, -0.3F).head(-0.2F, 0.0F).move(0.0F, -1.0F, -1.0F).scale(1.0F, 1.02F);
      T_HEAL_PRAYER = table(n0, hpPray, hpHold, hpLift, n1);
      CastAnims.K dhPray = K(0.24F, 0).like(hpPray);
      CastAnims.K dhHold = K(0.56F, 0).like(hpHold);
      CastAnims.K dhOpen = K(0.68F, 2).r(-0.35F, 0.0F, 1.3F).l(-0.35F, 0.0F, -1.3F).head(-0.35F, 0.0F).move(0.0F, -2.0F, -2.0F).scale(1.02F, 1.04F);
      CastAnims.K dhStay = K(0.86F, 0).like(dhOpen).move(0.0F, -1.0F, -1.0F);
      T_DUAL_HEAL = table(n0, dhPray, dhHold, dhOpen, dhStay, n1);
      CastAnims.K ahSpread = K(0.3F, 0)
         .r(-0.35F, 0.0F, 0.75F)
         .l(-0.35F, 0.0F, -0.75F)
         .head(-0.3F, 0.0F)
         .legs(0.1F, -0.05F)
         .move(0.0F, -5.0F, -1.0F)
         .scale(0.99F, 1.03F);
      CastAnims.K ahFloat = K(0.78F, 0).like(ahSpread).r(-0.45F, 0.0F, 0.9F).l(-0.45F, 0.0F, -0.9F).move(0.0F, -7.0F, -1.0F);
      CastAnims.K ahLand = K(0.92F, 0).r(-0.2F, 0.0F, 0.3F).l(-0.2F, 0.0F, -0.3F).move(0.0F, 0.0F, 0.0F).scale(1.03F, 0.96F);
      T_ANGELIC_HALO = table(n0, ahSpread, ahFloat, ahLand, n1);
      CastAnims.K wdRaise = K(0.2F, 0).r(-1.75F, -0.15F, 0.0F).l(-0.5F, 0.6F, 0.0F).head(-0.05F, 0.0F).legs(-0.15F, 0.15F).move(1.0F, 0.0F, 1.0F);
      CastAnims.K wdTrace = K(0.6F, 0).like(wdRaise);
      CastAnims.K wdPush = K(0.68F, 1).r(-1.55F, -0.1F, 0.0F).l(-1.5F, 0.15F, 0.0F).head(0.0F, 0.0F).body(-0.15F).legs(-0.35F, 0.3F).move(5.0F, 0.0F, 5.0F);
      CastAnims.K wdHold = K(0.86F, 0).like(wdPush).move(4.0F, 0.0F, 3.0F);
      T_WARD = table(n0, wdRaise, wdTrace, wdPush, wdHold, n1);
      CastAnims.K gsUp = K(0.24F, 0).r(-2.8F, -0.2F, 0.1F).l(-2.8F, 0.2F, -0.1F).head(-0.3F, 0.0F).move(0.0F, -4.0F, 0.0F).scale(0.97F, 1.05F);
      CastAnims.K gsSlam = K(0.4F, 1).r(-0.6F, -0.3F, 0.0F).l(-0.6F, 0.3F, 0.0F).head(0.35F, 0.0F).legs(0.3F, -0.3F).move(2.0F, 3.0F, 6.0F).scale(1.08F, 0.87F);
      CastAnims.K gsHoldSlam = K(0.56F, 0).like(gsSlam).move(2.0F, 2.5F, 5.0F).scale(1.06F, 0.9F);
      CastAnims.K gsBrace = K(0.76F, 0)
         .r(-0.6F, -0.3F, 0.2F)
         .l(-1.4F, 0.7F, 0.0F)
         .head(0.15F, 0.0F)
         .legs(0.28F, -0.32F)
         .move(-1.0F, 0.0F, 3.0F)
         .scale(1.03F, 0.95F);
      T_GREAT_SHIELD = table(n0, gsUp, gsSlam, gsHoldSlam, gsBrace, n1);
      CastAnims.K llSing = K(0.2F, 0).r(-0.95F, -0.85F, 0.0F).l(-0.45F, 0.0F, -0.7F).head(-0.18F, 0.1F).legs(0.05F, -0.05F);
      CastAnims.K llHold = K(0.86F, 0).like(llSing);
      T_LULLABY = table(n0, llSing, llHold, n1);
      CastAnims.K ssCup = K(0.24F, 0).r(-1.0F, -0.8F, 0.0F).l(-1.0F, 0.8F, 0.0F).head(0.35F, 0.0F).scale(1.0F, 0.98F);
      CastAnims.K ssRaise = K(0.56F, 0).r(-2.5F, -0.35F, 0.0F).l(-2.5F, 0.35F, 0.0F).head(-0.45F, 0.0F).move(0.0F, -2.0F, -1.0F).scale(1.0F, 1.02F);
      CastAnims.K ssRelease = K(0.68F, 2).r(-2.7F, 0.0F, 0.6F).l(-2.7F, 0.0F, -0.6F).head(-0.5F, 0.0F).move(0.0F, -4.0F, -1.0F).scale(0.98F, 1.05F);
      CastAnims.K ssHold = K(0.86F, 0).like(ssRelease).move(0.0F, -2.0F, 0.0F).scale(1.0F, 1.0F);
      T_SHINING_SOUL = table(n0, ssCup, ssRaise, ssRelease, ssHold, n1);
      CastAnims.K pcOffer = K(0.3F, 0).r(-1.35F, -0.15F, 0.0F).l(-0.2F, 0.0F, -0.15F).head(0.05F, 0.15F).legs(-0.12F, 0.1F).move(2.0F, 0.0F, 2.0F);
      CastAnims.K pcHold = K(0.84F, 0).like(pcOffer).r(-1.45F, -0.1F, 0.0F).move(3.0F, 0.0F, 3.0F);
      T_PACIFY = table(n0, pcOffer, pcHold, n1);
      CastAnims.K smLow = K(0.25F, 0).r(-1.1F, -0.25F, 0.0F).l(-1.1F, 0.25F, 0.0F).head(0.12F, 0.0F).legs(0.1F, -0.1F).scale(1.01F, 0.98F);
      CastAnims.K smStir = K(0.72F, 0).like(smLow);
      CastAnims.K smWaft = K(0.8F, 1).r(-1.5F, -0.1F, 0.1F).l(-1.5F, 0.1F, -0.1F).head(-0.05F, 0.0F).legs(-0.3F, 0.25F).move(4.0F, 0.0F, 4.0F);
      CastAnims.K smHold = K(0.9F, 0).like(smWaft);
      T_SLEEP_MIST = table(n0, smLow, smStir, smWaft, smHold, n1);
      CastAnims.K luChest = K(0.25F, 0).r(-0.95F, -0.85F, 0.0F).l(-0.95F, 0.85F, 0.0F).head(0.3F, 0.0F).scale(1.0F, 0.98F);
      CastAnims.K luOpen = K(0.55F, 0).r(-1.3F, 0.0F, 0.45F).l(-1.3F, 0.0F, -0.45F).head(-0.25F, 0.0F).move(0.0F, -2.0F, -1.0F).scale(1.0F, 1.02F);
      CastAnims.K luHold = K(0.86F, 0).like(luOpen).move(0.0F, -3.0F, -1.0F);
      T_LIFEUP = table(n0, luChest, luOpen, luHold, n1);
      CastAnims.K pfTemple = K(0.2F, 1).r(-2.35F, -0.7F, 0.0F).l(-0.3F, 0.0F, -0.3F).head(0.15F, 0.1F).legs(0.15F, -0.15F).move(-1.0F, 0.0F, -2.0F);
      CastAnims.K pfFocus = K(0.32F, 0).like(pfTemple).head(0.2F, 0.1F).move(-1.5F, 0.0F, -3.0F).scale(1.01F, 0.97F);
      CastAnims.K pfThrust = K(0.4F, 1)
         .r(-1.6F, -0.35F, 0.0F)
         .l(0.3F, 0.0F, -0.35F)
         .head(-0.05F, 0.0F)
         .body(-0.35F)
         .legs(-0.45F, 0.4F)
         .move(6.0F, 0.0F, 8.0F)
         .scale(1.04F, 0.97F);
      CastAnims.K pfHold = K(0.66F, 0).like(pfThrust).move(5.0F, 0.0F, 6.0F).scale(1.0F, 1.0F);
      T_PSI_FIRE = table(n0, pfTemple, pfFocus, pfThrust, pfHold, n1);
      CastAnims.K pzCup = K(0.24F, 0).r(-1.25F, -0.55F, 0.0F).l(-1.25F, 0.55F, 0.0F).head(0.3F, 0.0F).legs(0.1F, -0.1F).scale(1.01F, 0.97F);
      CastAnims.K pzForm = K(0.62F, 0).like(pzCup).r(-1.3F, -0.5F, 0.0F).l(-1.3F, 0.5F, 0.0F).move(-1.0F, 0.0F, -2.0F);
      CastAnims.K pzPush = K(0.7F, 1)
         .r(-1.57F, -0.25F, 0.0F)
         .l(-1.57F, 0.25F, 0.0F)
         .head(0.0F, 0.0F)
         .legs(-0.35F, 0.3F)
         .move(5.0F, 0.0F, 6.0F)
         .scale(1.04F, 0.97F);
      CastAnims.K pzHold = K(0.86F, 0).like(pzPush).move(4.0F, 0.0F, 4.0F).scale(1.0F, 1.0F);
      T_PSI_FREEZE = table(n0, pzCup, pzForm, pzPush, pzHold, n1);
      CastAnims.K ptFocus = K(0.24F, 1)
         .r(-2.45F, -0.6F, 0.0F)
         .l(-0.25F, 0.0F, -0.55F)
         .head(0.22F, 0.0F)
         .legs(0.15F, -0.15F)
         .move(0.0F, 0.0F, -1.0F)
         .scale(1.01F, 0.97F);
      CastAnims.K ptGuide = K(0.72F, 0).like(ptFocus).move(0.0F, -1.5F, -1.0F);
      CastAnims.K ptFling = K(0.8F, 1).r(-2.0F, -0.1F, 0.0F).l(-0.2F, 0.0F, -0.45F).head(-0.1F, 0.0F).body(-0.2F).legs(-0.25F, 0.2F).move(3.0F, -1.0F, 4.0F);
      CastAnims.K ptHold = K(0.92F, 0).like(ptFling);
      T_PSI_THUNDER = table(n0, ptFocus, ptGuide, ptFling, ptHold, n1);
      CastAnims.K psCross = K(0.3F, 0)
         .r(-1.45F, -0.9F, 0.0F)
         .l(-1.45F, 0.9F, 0.0F)
         .head(0.22F, 0.0F)
         .legs(0.15F, -0.15F)
         .move(0.0F, 1.0F, 0.0F)
         .scale(1.02F, 0.95F);
      CastAnims.K psOpen = K(0.46F, 2).r(-0.55F, 0.0F, 1.05F).l(-0.55F, 0.0F, -1.05F).head(-0.2F, 0.0F).move(0.0F, -1.0F, -1.0F).scale(0.99F, 1.04F);
      CastAnims.K psHold = K(0.82F, 0).like(psOpen).move(0.0F, 0.0F, 0.0F).scale(1.0F, 1.0F);
      T_PSI_SHIELD = table(n0, psCross, psOpen, psHold, n1);
      CastAnims.K hyPoint = K(0.25F, 0).r(-1.5F, -0.3F, 0.0F).l(-0.3F, 0.0F, -0.2F).head(0.05F, 0.0F).legs(-0.15F, 0.12F).move(3.0F, 0.0F, 3.0F);
      CastAnims.K hyHold = K(0.84F, 0).like(hyPoint);
      T_HYPNOSIS = table(n0, hyPoint, hyHold, n1);
      CastAnims.K pmReach = K(0.25F, 0).r(-1.55F, 0.15F, 0.1F).l(-1.55F, -0.15F, -0.1F).head(0.0F, 0.0F).legs(-0.3F, 0.25F).move(4.0F, 0.0F, 4.0F);
      CastAnims.K pmPull = K(0.75F, 0).r(-1.2F, -0.6F, 0.0F).l(-1.2F, 0.6F, 0.0F).head(0.1F, 0.0F).legs(0.25F, -0.25F).move(-3.0F, 0.0F, -4.0F);
      CastAnims.K pmGrip = K(0.86F, 1).r(-0.95F, -0.85F, 0.0F).l(-0.95F, 0.85F, 0.0F).head(0.2F, 0.0F).move(-3.0F, 0.0F, -3.0F).scale(1.02F, 0.97F);
      T_PSI_MAGNET = table(n0, pmReach, pmPull, pmGrip, n1);
      CastAnims.K bsTemples = K(0.3F, 0)
         .r(-2.5F, -0.75F, 0.0F)
         .l(-2.5F, 0.75F, 0.0F)
         .head(0.3F, 0.0F)
         .legs(0.15F, -0.15F)
         .move(-1.0F, 1.5F, -2.0F)
         .scale(1.03F, 0.93F);
      CastAnims.K bsBlast = K(0.48F, 1)
         .r(-1.6F, -0.1F, 0.45F)
         .l(-1.6F, 0.1F, -0.45F)
         .head(-0.3F, 0.0F)
         .legs(-0.3F, 0.25F)
         .move(4.0F, -1.0F, 5.0F)
         .scale(0.98F, 1.05F);
      CastAnims.K bsHold = K(0.74F, 0).like(bsBlast).move(3.0F, 0.0F, 3.0F).scale(1.0F, 1.0F);
      T_BRAINSHOCK = table(n0, bsTemples, bsBlast, bsHold, n1);
      CastAnims.K prCrouch = K(0.15F, 0).r(0.2F, 0.0F, 0.2F).l(0.2F, 0.0F, -0.2F).head(0.5F, 0.0F).move(0.0F, 3.0F, 0.0F).scale(1.06F, 0.9F);
      CastAnims.K prJump = K(0.3F, 2)
         .r(-2.75F, 0.0F, 0.5F)
         .l(-2.75F, 0.0F, -0.5F)
         .head(-0.4F, 0.0F)
         .legs(0.3F, -0.3F)
         .move(0.0F, -10.0F, 0.0F)
         .scale(0.95F, 1.08F);
      CastAnims.K prLand = K(0.42F, 1)
         .r(-2.2F, 0.0F, 0.4F)
         .l(-2.2F, 0.0F, -0.4F)
         .head(0.5F, 0.0F)
         .legs(0.2F, -0.25F)
         .move(0.0F, 2.0F, 3.0F)
         .scale(1.06F, 0.92F);
      CastAnims.K prBang1 = K(0.52F, 1).like(prLand).head(-0.3F, 0.0F).move(0.0F, 0.0F, -2.0F).scale(1.0F, 1.0F);
      CastAnims.K prBang2 = K(0.62F, 1).like(prLand);
      CastAnims.K prBang3 = K(0.72F, 1).like(prBang1);
      CastAnims.K prFist = K(0.84F, 2)
         .r(-3.0F, 0.0F, 0.15F)
         .l(0.2F, 0.0F, -0.3F)
         .head(-0.2F, 0.0F)
         .legs(0.25F, -0.3F)
         .move(0.0F, -1.0F, -2.0F)
         .scale(0.98F, 1.03F);
      CastAnims.K prHold = K(0.94F, 0).like(prFist);
      T_PSI_ROCKIN = table(n0, prCrouch, prJump, prLand, prBang1, prBang2, prBang3, prFist, prHold, n1);
      CastAnims.K stRaise = K(0.3F, 0).r(-2.75F, 0.0F, 0.45F).l(-2.75F, 0.0F, -0.45F).head(-0.6F, 0.0F).move(0.0F, -3.0F, -1.0F).scale(0.99F, 1.03F);
      CastAnims.K stRise = K(0.78F, 0).like(stRaise).move(0.0F, -9.0F, -1.0F);
      CastAnims.K stCall = K(0.86F, 1).r(-1.35F, -0.1F, 0.15F).l(-1.35F, 0.1F, -0.15F).head(0.1F, 0.0F).move(3.0F, -6.0F, 4.0F);
      CastAnims.K stLand = K(0.95F, 0).like(stCall).move(2.0F, 0.0F, 2.0F).scale(1.03F, 0.96F);
      T_STARSTORM = table(n0, stRaise, stRise, stCall, stLand, n1);
      CastAnims.K ouCrouch = K(0.26F, 0)
         .r(0.25F, 0.0F, 0.3F)
         .l(0.25F, 0.0F, -0.3F)
         .head(0.35F, 0.0F)
         .legs(0.2F, -0.2F)
         .move(0.0F, 3.0F, 0.0F)
         .scale(1.07F, 0.89F);
      CastAnims.K ouFlex = K(0.44F, 2)
         .r(-0.25F, 0.0F, 1.45F)
         .l(-0.25F, 0.0F, -1.45F)
         .head(-0.25F, 0.0F)
         .legs(0.3F, -0.3F)
         .move(0.0F, -2.0F, 0.0F)
         .scale(1.05F, 1.06F);
      CastAnims.K ouHold = K(0.78F, 0).like(ouFlex).move(0.0F, -1.0F, 0.0F).scale(1.03F, 1.03F);
      T_OFFENSE_UP = table(n0, ouCrouch, ouFlex, ouHold, n1);
      CastAnims.K spPeek = K(0.3F, 0).r(-2.6F, -0.75F, 0.0F).l(-0.2F, 0.0F, -0.25F).head(-0.1F, 0.0F).legs(-0.2F, 0.2F).move(3.0F, 0.0F, 10.0F);
      CastAnims.K spScan = K(0.82F, 0).like(spPeek);
      CastAnims.K spNod = K(0.9F, 1).like(spPeek).head(0.25F, 0.0F).r(-0.6F, -0.2F, 0.0F).move(2.0F, 0.0F, 3.0F);
      T_SPY = table(n0, spPeek, spScan, spNod, n1);
   }

   public static enum Cast {
      GENERIC(980L, true),
      RUDE_BUSTER(1300L, true),
      ICE_SHOCK(1150L, false),
      GLITTER_BOMB(1300L, false),
      SNOWGRAVE(2900L, false),
      X_SLASH(1250L, true),
      GIGAFLARE(1600L, false),
      HEAL_PRAYER(1300L, false),
      DUAL_HEAL(1400L, false),
      ANGELIC_HALO(1400L, false),
      WARD(1300L, false),
      GREAT_SHIELD(1300L, true),
      LULLABY(1450L, false),
      SHINING_SOUL(1400L, false),
      PACIFY(1350L, false),
      SLEEP_MIST(1400L, false),
      LIFEUP(1250L, false),
      PSI_FIRE(1050L, false),
      PSI_FREEZE(1250L, false),
      PSI_THUNDER(1300L, false),
      PSI_SHIELD(1150L, false),
      HYPNOSIS(1400L, false),
      PSI_MAGNET(1300L, false),
      BRAINSHOCK(1200L, false),
      PSI_ROCKIN(1500L, false),
      STARSTORM(1900L, false),
      OFFENSE_UP(1150L, false),
      SPY(1300L, false);

      public final long ms;
      public final boolean weapon;

      private Cast(long ms, boolean weapon) {
         this.ms = ms;
         this.weapon = weapon;
      }
   }

   static final class Fx {
      final DrawContext ctx;
      final CloneAnimator.Frame fr;
      final CastAnims.Cast cast;
      final float fx0;
      final float fy0;
      final float h;
      final int dir;
      final boolean front;
      final float p;
      final float t;
      final float[] tmp = new float[2];

      Fx(DrawContext ctx, CloneAnimator.Frame fr, CastAnims.Cast cast, float feetX, float feetY, float hPx, int dir, boolean front) {
         this.ctx = ctx;
         this.fr = fr;
         this.cast = cast;
         this.fx0 = feetX;
         this.fy0 = feetY;
         this.h = hPx;
         this.dir = dir;
         this.front = front;
         this.p = fr.p;
         this.t = fr.t;
      }

      float bx() {
         return this.fx0 + this.fr.pose.dx * (float)this.dir;
      }

      float by() {
         return this.fy0 + this.fr.pose.dy;
      }

      float[] arm(boolean right, float ax, float ay, float az) {
         CastAnims.armPt(this.fr.pose, right, ax, ay, az, this.h, this.dir, this.tmp);
         return new float[]{this.fx0 + this.tmp[0], this.fy0 + this.tmp[1]};
      }

      float[] hand(boolean right) {
         return this.arm(right, right ? -1.0F : 1.0F, 10.0F, -0.5F);
      }

      float[] handsMid() {
         float[] a = this.hand(true);
         float[] b = this.hand(false);
         return new float[]{(a[0] + b[0]) / 2.0F, (a[1] + b[1]) / 2.0F};
      }

      float[] head(float x, float y, float z) {
         CastAnims.headPt(this.fr.pose, x, y, z, this.h, this.dir, this.tmp);
         return new float[]{this.fx0 + this.tmp[0], this.fy0 + this.tmp[1]};
      }

      float[] armAt(float pp, boolean right, float ax, float ay, float az) {
         CloneAnimator.Pose o = new CloneAnimator.Pose();
         CastAnims.pose(this.cast, o, CloneAnimator.clamp01(pp), pp * (float)this.cast.ms);
         CastAnims.armPt(o, right, ax, ay, az, this.h, this.dir, this.tmp);
         return new float[]{this.fx0 + this.tmp[0], this.fy0 + this.tmp[1]};
      }

      float[] handAt(float pp, boolean right) {
         return this.armAt(pp, right, right ? -1.0F : 1.0F, 10.0F, -0.5F);
      }

      float q(float a, float b) {
         return CloneAnimator.seg(this.p, a, b);
      }

      boolean in(float a, float b) {
         return this.p > a && this.p < b;
      }
   }

   static final class K {
      final float[] v = new float[18];

      K(float p, int ease) {
         this.v[0] = p;
         this.v[1] = (float)ease;
         this.v[16] = 1.0F;
         this.v[17] = 1.0F;
      }

      CastAnims.K like(CastAnims.K o) {
         System.arraycopy(o.v, 2, this.v, 2, 16);
         return this;
      }

      CastAnims.K r(float p, float y, float r) {
         this.v[2] = p;
         this.v[3] = y;
         this.v[4] = r;
         return this;
      }

      CastAnims.K l(float p, float y, float r) {
         this.v[5] = p;
         this.v[6] = y;
         this.v[7] = r;
         return this;
      }

      CastAnims.K head(float p, float y) {
         this.v[8] = p;
         this.v[9] = y;
         return this;
      }

      CastAnims.K body(float y) {
         this.v[10] = y;
         return this;
      }

      CastAnims.K legs(float r, float l) {
         this.v[11] = r;
         this.v[12] = l;
         return this;
      }

      CastAnims.K move(float dx, float dy, float tilt) {
         this.v[13] = dx;
         this.v[14] = dy;
         this.v[15] = tilt;
         return this;
      }

      CastAnims.K scale(float sx, float sy) {
         this.v[16] = sx;
         this.v[17] = sy;
         return this;
      }
   }
}
