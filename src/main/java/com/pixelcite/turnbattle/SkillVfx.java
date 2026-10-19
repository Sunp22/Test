package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public final class SkillVfx {
   static final float TAU = (float) (Math.PI * 2);
   static final float PI = (float) Math.PI;
   private static final float[][][] GLYPHS = new float[][][]{
      {join(new float[]{0.62F, -0.55F}, arcPts(-0.2F, 0.0F, 0.5F, 0.5F, (float) (Math.PI / 4), (float) Math.PI * 7.0F / 4.0F, 16), new float[]{0.62F, 0.55F})},
      {
            {-0.35F, 1.0F, -0.35F, -0.38F},
            join(
               arcPts(-0.05F, -0.38F, 0.3F, 0.37F, (float) Math.PI, (float) Math.PI * 5.0F / 2.0F, 14),
               arcPts(-0.02F, 0.36F, 0.38F, 0.37F, (float) (-Math.PI / 2), (float) (Math.PI * 9.0 / 10.0), 14)
            )
      },
      {{-0.55F, -0.5F, 0.12F, 0.52F}, {0.55F, -0.5F, -0.12F, 0.52F}, arcPts(0.0F, 0.66F, 0.16F, 0.16F, 0.0F, (float) (Math.PI * 2), 10)},
      {join(new float[]{-0.62F, 0.55F, -0.3F, 0.55F}, arcPts(0.0F, -0.1F, 0.55F, 0.55F, 2.1915927F, 7.2331853F, 22), new float[]{0.3F, 0.55F, 0.62F, 0.55F})}
   };
   private static final int[][] HEX = buildHex(3);

   private SkillVfx() {
   }

   static int tierOf(String name) {
      if (name != null && !name.isEmpty()) {
         return switch (name.charAt(name.length() - 1)) {
            case '\u03a9' -> 4;
            case '\u03b2' -> 2;
            case '\u03b3' -> 3;
            default -> 1;
         };
      } else {
         return 1;
      }
   }

   public static SkillVfx.Spec specFor(BattleSkill skill, int colorOverride) {
      SkillType t = skill.getType();
      SkillVfx.Special sp = SkillVfx.Special.NONE;
      if (skill == LightnerSkill.SNOWGRAVE) {
         sp = SkillVfx.Special.SNOWGRAVE;
      } else if (skill == PsiSkill.STARSTORM_A || skill == PsiSkill.STARSTORM_O) {
         sp = SkillVfx.Special.STARSTORM;
      } else if (skill == LightnerSkill.RUDE_BUSTER || skill == LightnerSkill.RUDE_BUSTER_PLUS) {
         sp = SkillVfx.Special.BUSTER;
      } else if (skill == LightnerSkill.RED_BUSTER) {
         sp = SkillVfx.Special.RED_BUSTER;
      } else if (skill == LightnerSkill.GIGAFLARE) {
         sp = SkillVfx.Special.GIGAFLARE;
      } else if (skill == MonsterSkill.HYPER_GONER) {
         sp = SkillVfx.Special.HYPER_GONER;
      }

      boolean psiAccent = false;
      SkillVfx.Theme th;
      if (skill instanceof OriSkill os) {
         th = os != OriSkill.VOID_TIDE && os != OriSkill.UMBRAL_CRASH ? SkillVfx.Theme.LUNAR : SkillVfx.Theme.DARK;
      } else {
         Affinity[] affs = skill.getAffinities();
         th = affs != null && affs.length > 0 && affs[0] != null ? themeOf(affs[0]) : themeForType(t);
         if (affs != null) {
            for (int i = 1; i < affs.length; i++) {
               if (affs[i] == Affinity.PSI) {
                  psiAccent = true;
               }
            }
         }
      }

      int hits = t == SkillType.MULTI ? Math.max(2, Math.min(6, skill.getExtra())) : 1;
      int accent = colorOverride != 0 ? colorOverride : t.color();
      return new SkillVfx.Spec(t, th, sp, psiAccent, accent, hits, skill.getDisplayName().hashCode(), tierOf(skill.getDisplayName()));
   }

   public static SkillVfx.Spec specForMob(SkillType t, int colorOverride) {
      SkillVfx.Theme th = switch (t) {
         case BURN -> SkillVfx.Theme.FIRE;
         case FREEZE -> SkillVfx.Theme.ICE;
         case BYPASS -> SkillVfx.Theme.ELECTRIC;
         case MULTI -> SkillVfx.Theme.SLASH;
         default -> SkillVfx.Theme.NEUTRAL;
      };
      int accent = colorOverride != 0 ? colorOverride : t.color();
      return new SkillVfx.Spec(t, th, SkillVfx.Special.NONE, false, accent, t == SkillType.MULTI ? 3 : 1, (int)System.nanoTime());
   }

   static SkillVfx.Theme themeOf(Affinity a) {
      return switch (a) {
         case FIRE -> SkillVfx.Theme.FIRE;
         case ICE -> SkillVfx.Theme.ICE;
         case ELECTRIC -> SkillVfx.Theme.ELECTRIC;
         case PSI -> SkillVfx.Theme.PSI;
         case SLASH -> SkillVfx.Theme.SLASH;
         case LIGHT -> SkillVfx.Theme.LIGHT;
         case DARK -> SkillVfx.Theme.DARK;
         case IMPACT -> SkillVfx.Theme.IMPACT;
      };
   }

   static SkillVfx.Theme themeForType(SkillType t) {
      return switch (t) {
         case BURN -> SkillVfx.Theme.FIRE;
         case FREEZE -> SkillVfx.Theme.ICE;
         case BYPASS -> SkillVfx.Theme.ELECTRIC;
         case MULTI -> SkillVfx.Theme.SLASH;
         default -> SkillVfx.Theme.NEUTRAL;
      };
   }

   static SkillVfx.Pal palette(SkillVfx.Theme th, int accent) {
      return switch (th) {
         case FIRE -> new SkillVfx.Pal(16774856, 16752676, 16729618, 3811878, 16769136);
         case ICE -> new SkillVfx.Pal(16777215, 11070719, 4171519, 1849958, 14744319);
         case ELECTRIC -> new SkillVfx.Pal(16777215, 16773754, 5944575, 2107472, 16777136);
         case PSI -> new SkillVfx.Pal(16777215, 16747248, 8019199, 2756680, 9109488);
         case SLASH -> new SkillVfx.Pal(16777215, 14871284, 9086664, 2896964, 16777215);
         case LIGHT -> new SkillVfx.Pal(16777215, 16774324, 16762428, 5917216, 16777184);
         case DARK -> new SkillVfx.Pal(15780095, 10767615, 5116558, 787736, 16731336);
         case IMPACT -> new SkillVfx.Pal(16777215, 16764052, 16746050, 5127728, 14206120);
         case LUNAR -> new SkillVfx.Pal(16777215, 14477055, 9215231, 1843280, 12110079);
         case NEUTRAL -> {
            int a = accent & 16777215;
            int full = 0xFF000000 | a;
            yield new SkillVfx.Pal(
               16777215,
               VfxBatch.lerp(full, -1, 0.45F) & 16777215,
               a,
               VfxBatch.lerp(full, -16777216, 0.75F) & 16777215,
               VfxBatch.lerp(full, -1, 0.7F) & 16777215
            );
         }
      };
   }

   public static void render(DrawContext ctx, SkillVfx.Spec sp, float cx, float cy, float T, int startup, int hit, float screenW, float screenH) {
      SkillVfx.R r = new SkillVfx.R(ctx, sp, cx, cy, Math.max(0.0F, Math.min(T, (float)(startup + hit))), (float)startup, (float)hit, screenW, screenH);
      switch (sp.special) {
         case SNOWGRAVE:
            snowgrave(r);
            return;
         case STARSTORM:
            starstorm(r);
            return;
         case BUSTER:
            buster(r, false);
            return;
         case RED_BUSTER:
            buster(r, true);
            return;
         case GIGAFLARE:
            gigaflare(r);
            return;
         case HYPER_GONER:
            hyperGoner(r);
            return;
         default:
            if (sp.psiAccent) {
               psiSchool(r);
            } else {
               switch (sp.type) {
                  case BURN:
                     if (r.th != SkillVfx.Theme.FIRE && r.th != SkillVfx.Theme.NEUTRAL) {
                        strike(r, false);
                     } else {
                        fireEruption(r, true);
                     }
                     break;
                  case FREEZE:
                     if (r.th != SkillVfx.Theme.ICE && r.th != SkillVfx.Theme.NEUTRAL) {
                        strike(r, false);
                     } else {
                        iceCrystal(r, true);
                     }
                     break;
                  case BYPASS:
                     strike(r, true);
                     break;
                  case MULTI:
                     flurry(r);
                     break;
                  case DAMAGE:
                     strike(r, false);
                     break;
                  case AOE:
                     barrage(r);
                     break;
                  case HEAL:
                     heal(r);
                     break;
                  case SHIELD:
                     barrier(r);
                     break;
                  case BUFF:
                     powerUp(r);
                     break;
                  case WEAKEN:
                     weaken(r);
                     break;
                  case SLEEP:
                     sleep(r);
                     break;
                  case CONFUSE:
                     confuse(r);
                     break;
                  case MAGNET:
                     magnet(r);
                     break;
                  case PACIFY:
                     pacify(r);
                     break;
                  case SPARE:
                     spare(r);
                     break;
                  case RAISE_THRESHOLD:
                     plead(r);
                     break;
                  case SPY:
                     scan(r);
               }
            }
      }
   }

   static void strike(SkillVfx.R r, boolean pierce) {
      switch (r.th) {
         case FIRE:
            fireEruption(r, false);
            break;
         case ICE:
            iceCrystal(r, false);
            break;
         case ELECTRIC:
            thunderbolt(r, pierce);
            break;
         case PSI:
            psiBurst(r);
            break;
         case SLASH:
            greatSlash(r);
            break;
         case LIGHT:
            holyRay(r);
            break;
         case DARK:
            voidImplosion(r);
            break;
         case IMPACT:
            meteorCrash(r);
            break;
         case LUNAR:
            moonbeam(r);
            break;
         case NEUTRAL:
            soulBurst(r);
      }

      if (pierce && r.th != SkillVfx.Theme.ELECTRIC) {
         pierceLance(r);
      }
   }

   static float clamp01(float x) {
      return x < 0.0F ? 0.0F : (x > 1.0F ? 1.0F : x);
   }

   static float seg(float p, float a, float b) {
      return clamp01((p - a) / (b - a));
   }

   static float mix(float a, float b, float t) {
      return a + (b - a) * t;
   }

   static float eo(float x) {
      x = clamp01(x);
      float v = 1.0F - x;
      return 1.0F - v * v * v;
   }

   static float ein(float x) {
      x = clamp01(x);
      return x * x * x;
   }

   static float eis(float x) {
      x = clamp01(x);
      return (float)(-(Math.cos(Math.PI * (double)x) - 1.0) / 2.0);
   }

   static float eob(float x) {
      x = clamp01(x);
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float v = x - 1.0F;
      return 1.0F + c3 * v * v * v + c1 * v * v;
   }

   static float bell(float q) {
      return !(q <= 0.0F) && !(q >= 1.0F) ? (float)Math.sin(Math.PI * (double)q) : 0.0F;
   }

   static float rnd(int i) {
      return VfxBatch.hash01(i);
   }

   static float sin(float a) {
      return (float)Math.sin((double)a);
   }

   static float cos(float a) {
      return (float)Math.cos((double)a);
   }

   static void mat(VfxBatch b, SkillVfx.R r, float x, float y, float s, float ang, float a, int i) {
      if (!(a <= 0.01F) && !(s <= 0.05F)) {
         SkillVfx.Pal p = r.p;
         switch (r.th) {
            case FIRE:
               float fl = 0.75F + 0.25F * sin(r.sec * 30.0F + (float)i * 2.1F);
               b.glow(x, y, s * 2.4F * fl, VfxBatch.col(p.outer, 0.35F * a));
               b.glow(x, y, s * 1.2F * fl, VfxBatch.col(p.mid, 0.9F * a));
               b.glow(x, y, s * 0.5F, VfxBatch.col(p.core, a));
               break;
            case ICE:
               b.glow(x, y, s * 1.8F, VfxBatch.col(p.outer, 0.25F * a));
               b.shard(x, y, s * 2.8F, s * 1.1F, ang + r.sec * 6.0F * (rnd(i) - 0.5F), VfxBatch.col(p.core, 0.95F * a), VfxBatch.col(p.outer, 0.8F * a));
               break;
            case ELECTRIC:
               float j = (rnd(i * 7 + (int)(r.T * 2.0F)) - 0.5F) * s * 1.6F;
               float ex = cos(ang) * s * 1.6F;
               float ey = sin(ang) * s * 1.6F;
               b.beam(x - ex, y - ey, x + j, y + j * 0.5F, 0.5F, VfxBatch.col(p.outer, 0.5F * a), VfxBatch.col(p.core, a));
               b.beam(x + j, y + j * 0.5F, x + ex, y + ey, 0.5F, VfxBatch.col(p.outer, 0.5F * a), VfxBatch.col(p.core, a));
               b.glow(x, y, s * 1.3F, VfxBatch.col(p.mid, 0.4F * a));
               break;
            case PSI:
               int hc = VfxBatch.hsv(rnd(i) + r.sec * 0.7F, 0.55F, 1.0F, a);
               b.glow(x, y, s * 2.0F, VfxBatch.mulA(hc, 0.3F));
               b.shard(x, y, s * 2.2F, s * 1.5F, ang + r.sec * 5.0F, hc, VfxBatch.mulA(hc, 0.55F));
               break;
            case SLASH:
               b.spark(x, y, ang, s * 4.5F, s * 0.8F, VfxBatch.col(p.core, a));
               b.glow(x, y, s * 1.1F, VfxBatch.col(p.outer, 0.4F * a));
               break;
            case LIGHT: {
               float tw = 0.7F + 0.3F * sin(r.sec * 20.0F + (float)i * 1.7F);
               b.glow(x, y, s * 2.2F, VfxBatch.col(p.outer, 0.3F * a));
               b.star(x, y, s * 1.5F * tw, 4, 0.0F, VfxBatch.col(p.core, a));
               break;
            }
            case DARK:
               b.glow(x, y, s * 2.4F, VfxBatch.col(p.outer, 0.5F * a));
               b.glow(x, y, s * 1.1F, VfxBatch.col(p.mid, 0.8F * a));
               b.glow(x, y, s * 0.4F, VfxBatch.col(p.accent, a));
               break;
            case IMPACT:
               b.spark(x, y, ang, s * 3.4F, s * 0.9F, VfxBatch.col(p.mid, a));
               b.glow(x, y, s * 0.9F, VfxBatch.col(p.core, 0.8F * a));
               break;
            case LUNAR: {
               float tw = 0.7F + 0.3F * sin(r.sec * 16.0F + (float)i * 2.3F);
               b.glow(x, y, s * 2.0F, VfxBatch.col(p.outer, 0.3F * a));
               b.star(x, y, s * 1.4F * tw, 4, (float) (Math.PI / 4) * (float)(i % 2), VfxBatch.col(p.core, a));
               break;
            }
            case NEUTRAL:
               b.spark(x, y, ang, s * 3.2F, s * 1.1F, VfxBatch.col(p.mid, a));
               b.glow(x, y, s * 1.3F, VfxBatch.col(p.outer, 0.5F * a));
               b.glow(x, y, s * 0.45F, VfxBatch.col(p.core, a));
         }
      }
   }

   static void burst(VfxBatch b, SkillVfx.R r, int n, float x, float y, float q, float dist, float grav, float size, int salt, float upBias) {
      if (!(q <= 0.0F) && !(q >= 1.0F)) {
         float e = eo(q);

         for (int i = 0; i < n; i++) {
            float ang = rnd(salt + i * 3) * (float) (Math.PI * 2);
            if (upBias > 0.0F) {
               ang = mix(ang, (float) (-Math.PI / 2) + (rnd(salt + i * 5) - 0.5F) * 1.6F, upBias);
            }

            float sp = 0.4F + 0.6F * rnd(salt + i * 3 + 1);
            float px = x + cos(ang) * dist * sp * e;
            float py = y + sin(ang) * dist * sp * e + grav * q * q;
            float life = (1.0F - q) * (0.6F + 0.4F * rnd(salt + i));
            mat(b, r, px, py, size * (0.6F + 0.7F * rnd(salt + i * 11)), ang, life, salt + i);
         }
      }
   }

   static void converge(VfxBatch b, SkillVfx.R r, int n, float x, float y, float rad, float q, int salt) {
      if (!(q <= 0.0F)) {
         float fadeIn = clamp01(q * 3.0F);
         float fadeOut = 1.0F - seg(q, 0.9F, 1.0F);

         for (int i = 0; i < n; i++) {
            float f = (q * 2.4F + rnd(salt + i)) % 1.0F;
            float rr = rad * (float)Math.pow((double)(1.0F - f), 1.4F) + 2.0F;
            float ang = rnd(salt + i * 7) * (float) (Math.PI * 2) + f * 2.2F;
            float px = x + cos(ang) * rr;
            float py = y + sin(ang) * rr * 0.75F;
            mat(b, r, px, py, 1.2F + 0.8F * rnd(salt + i * 5), ang + (float) Math.PI, bell(f) * fadeIn * fadeOut, salt + i);
         }
      }
   }

   static void sigil(VfxBatch b, SkillVfx.R r, float x, float y, float rad, float a, float rot, int points) {
      if (!(a <= 0.01F) && !(rad < 1.0F)) {
         SkillVfx.Pal p = r.p;
         float ky = 0.32F;
         b.ellipse(x, y, rad * 1.15F, rad * 1.15F * ky, 0.0F, VfxBatch.col(p.outer, 0.28F * a), VfxBatch.col(p.outer, 0.0F));
         b.ringArc(x, y, rad, rad * ky, 2.6F, VfxBatch.col(p.mid, 0.9F * a), 0.0F, (float) (Math.PI * 2));
         b.ringArc(x, y, rad * 0.8F, rad * 0.8F * ky, 1.4F, VfxBatch.col(p.core, 0.6F * a), 0.0F, (float) (Math.PI * 2));
         if (points == 6) {
            b.polyLine(x, y, rad * 0.78F, 6, rot, 1.0F, VfxBatch.col(p.outer, 0.75F * a), ky);
            b.polyLine(x, y, rad * 0.55F, 6, -rot + (float) (Math.PI / 6), 0.9F, VfxBatch.col(p.outer, 0.6F * a), ky);
         } else {
            b.starLine(x, y, rad * 0.78F, points, 2, rot, 1.0F, VfxBatch.col(p.outer, 0.75F * a), ky);
         }

         for (int i = 0; i < 12; i++) {
            float ang = rot * -0.7F + (float)i * (float) (Math.PI * 2) / 12.0F;
            b.glow(x + cos(ang) * rad * 0.9F, y + sin(ang) * rad * 0.9F * ky, 1.6F, VfxBatch.col(p.core, 0.85F * a));
         }
      }
   }

   static void snowflake(VfxBatch b, float x, float y, float rad, float rot, int c, float th) {
      if (!(rad < 0.5F)) {
         for (int arm = 0; arm < 6; arm++) {
            float a = rot + (float)arm * (float) Math.PI / 3.0F;
            float ex = x + cos(a) * rad;
            float ey = y + sin(a) * rad;
            b.line(x, y, ex, ey, th, c, VfxBatch.mulA(c, 0.7F));
            float bx = x + cos(a) * rad * 0.58F;
            float by = y + sin(a) * rad * 0.58F;
            float bl = rad * 0.32F;
            b.line(bx, by, bx + cos(a + 0.6F) * bl, by + sin(a + 0.6F) * bl, th * 0.75F, c, VfxBatch.mulA(c, 0.5F));
            b.line(bx, by, bx + cos(a - 0.6F) * bl, by + sin(a - 0.6F) * bl, th * 0.75F, c, VfxBatch.mulA(c, 0.5F));
         }

         b.glow(x, y, rad * 0.35F, c);
      }
   }

   static void rock(VfxBatch b, SkillVfx.R r, float x, float y, float s, float rot, float a) {
      if (!(a <= 0.01F)) {
         b.poly(x, y, s, 5, rot, VfxBatch.col(VfxBatch.lerp(0xFF000000 | r.p.dark, -1, 0.25F), a), VfxBatch.col(r.p.dark, a));
         b.line(
            x + cos(rot) * s,
            y + sin(rot) * s,
            x + cos(rot + 1.25F) * s,
            y + sin(rot + 1.25F) * s,
            Math.max(0.6F, s * 0.25F),
            VfxBatch.col(r.p.accent, 0.8F * a),
            VfxBatch.col(r.p.accent, 0.8F * a)
         );
      }
   }

   static void puff(VfxBatch b, float x, float y, float rad, int rgb, float a) {
      if (!(a <= 0.01F)) {
         b.disc(x, y, rad, VfxBatch.col(rgb, a), VfxBatch.col(rgb, 0.0F));
      }
   }

   static void flash(VfxBatch b, SkillVfx.R r, int rgb, float a) {
      if (a > 0.005F) {
         b.screen(r.sw, r.sh, VfxBatch.col(rgb, a * 0.65F));
      }
   }

   static void fireEruption(SkillVfx.R r, boolean burn) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float app = eob(seg(r.u, 0.0F, 0.35F)) * (r.hit ? 1.0F - seg(r.h, 0.25F, 0.9F) : 1.0F);
      float charge = seg(r.u, 0.35F, 1.0F);
      if (r.hit) {
         r.alpha(b -> {
            for (int k = 0; k < 8; k++) {
               float q = seg(r.h, (float)k * 0.035F, 1.0F);
               float sx = x + (rnd(r.sp.seed + k) - 0.5F) * U * 1.8F + sin(r.sec * 2.0F + (float)k) * 3.0F;
               float sy = y - U * 0.3F - eo(q) * U * 2.0F;
               puff(b, sx, sy, U * (0.35F + 0.65F * q), p.dark(), 0.42F * bell(q));
            }
         });
      }

      r.add(
         b -> {
            sigil(b, r, x, gy, U * 1.25F * app, app, r.sec * 1.4F, 5);
            if (!r.hit) {
               for (int k = 0; k < 10; k++) {
                  float a = (float)k * (float) (Math.PI * 2) / 10.0F + r.sec * 0.6F;
                  float fx = x + cos(a) * U * 1.15F * app;
                  float fy = gy + sin(a) * U * 1.15F * 0.32F * app;
                  float fh = (3.0F + 12.0F * charge) * (0.7F + 0.3F * sin(r.sec * 14.0F + (float)k * 2.0F));
                  b.flame(
                     fx,
                     fy,
                     3.5F + 3.0F * charge,
                     fh,
                     sin(r.sec * 6.0F + (float)k) * 2.0F,
                     VfxBatch.col(p.mid(), 0.9F * app),
                     VfxBatch.col(p.outer(), 0.5F * app)
                  );
               }

               converge(b, r, 18, x, y, U * 2.3F, r.u, r.sp.seed + 11);
               b.glow(x, y, U * (0.3F + 0.7F * eo(r.u)), VfxBatch.col(p.outer(), 0.35F * r.u));
               b.glow(x, y, U * 0.35F * charge, VfxBatch.col(p.core(), 0.6F * charge));
            } else {
               float env = r.env();

               for (int k = 0; k < 11; k++) {
                  float o = (float)(k - 5) / 5.0F;
                  float ao = Math.abs(o);
                  float fx = x + o * U * 0.85F;
                  float hk = U * (1.3F + 2.3F * (1.0F - ao)) * env * (0.78F + 0.22F * sin(r.sec * 18.0F + (float)k * 1.7F));
                  float sway = sin(r.sec * 9.0F + (float)k) * U * 0.25F;
                  b.flame(fx, gy, U * (0.6F - 0.25F * ao), hk, sway, VfxBatch.col(p.outer(), 0.9F * env), VfxBatch.col(p.outer(), 0.6F * env));
                  b.flame(fx, gy, U * (0.42F - 0.2F * ao), hk * 0.78F, sway * 0.8F, VfxBatch.col(p.mid(), 0.95F * env), VfxBatch.col(p.accent(), 0.7F * env));
                  b.flame(fx, gy, U * (0.22F - 0.1F * ao), hk * 0.45F, sway * 0.6F, VfxBatch.col(p.core(), env), VfxBatch.col(p.mid(), 0.0F));
               }

               float fh = 1.0F - r.h;
               b.glow(x, y, U * (1.0F + 1.8F * eo(r.h)), VfxBatch.col(p.outer(), 0.5F * fh));
               b.glow(x, y, U * 1.1F * fh, VfxBatch.col(p.core(), 0.9F * fh));
               float sr = U * (0.5F + 2.8F * eo(r.h));
               b.ringArc(x, gy, sr, sr * 0.32F, U * 0.45F * fh, VfxBatch.col(p.mid(), 0.8F * fh), 0.0F, (float) (Math.PI * 2));
               b.ring(x, y, U * (0.4F + 2.2F * eo(r.h)), U * 0.3F * fh, VfxBatch.col(p.outer(), 0.55F * fh));
               burst(b, r, 28, x, y, r.h, U * 2.8F, -U * 1.4F, 1.7F, r.sp.seed + 101, 0.6F);
               flash(b, r, p.mid(), 0.22F * (1.0F - seg(r.h, 0.0F, 0.18F)));
               if (burn) {
                  float cl = seg(r.h, 0.2F, 0.4F) * (1.0F - seg(r.h, 0.85F, 1.0F));

                  for (int k = 0; k < 7; k++) {
                     float fx = x + (rnd(r.sp.seed + k * 3) - 0.5F) * U * 1.2F;
                     float fy = y + (rnd(r.sp.seed + k * 5) - 0.3F) * U * 1.1F;
                     float hh = U * 0.6F * cl * (0.7F + 0.3F * sin(r.sec * 22.0F + (float)k * 2.4F));
                     b.flame(fx, fy, U * 0.3F, hh, sin(r.sec * 11.0F + (float)k) * 2.5F, VfxBatch.col(p.mid(), cl), VfxBatch.col(p.outer(), 0.3F * cl));
                  }
               }
            }
         }
      );
   }

   static void iceCrystal(SkillVfx.R r, boolean encase) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      int n = 7;
      float frost = eo(seg(r.u, 0.0F, 0.4F)) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
      float crystalFade = !r.hit ? 1.0F : (encase ? 1.0F - seg(r.h, 0.55F, 0.9F) : 0.0F);
      float blockG = r.hit && encase ? eob(seg(r.h, 0.0F, 0.25F)) * (1.0F - seg(r.h, 0.75F, 1.0F)) : 0.0F;
      float blockR = U * 1.25F * blockG;
      r.alpha(
         b -> {
            b.ellipse(x, gy, U * 1.5F * frost, U * 1.5F * 0.32F * frost, 0.0F, VfxBatch.col(p.mid(), 0.35F * frost), VfxBatch.col(p.outer(), 0.0F));
            if (crystalFade > 0.0F) {
               for (int k = 0; k < n; k++) {
                  float o = ((float)k - (float)(n - 1) / 2.0F) / ((float)(n - 1) / 2.0F);
                  float ao = Math.abs(o);
                  float g = eob(seg(r.u, 0.12F + 0.07F * ao + 0.03F * (float)k, 0.62F + 0.05F * (float)k));
                  float len = U * (0.9F + 1.2F * (1.0F - ao)) * g;
                  float ang = (float) (-Math.PI / 2) + o * 0.6F + (rnd(r.sp.seed + k) - 0.5F) * 0.2F;
                  b.spike(
                     x + o * U * 1.1F,
                     gy + 2.0F,
                     len,
                     U * (0.34F - 0.1F * ao),
                     ang,
                     VfxBatch.col(p.core(), 0.92F * crystalFade),
                     VfxBatch.col(p.outer(), 0.85F * crystalFade)
                  );
               }
            }

            if (blockG > 0.0F) {
               b.poly(x, y, blockR, 6, (float) (Math.PI / 6), VfxBatch.col(p.mid(), 0.3F * blockG), VfxBatch.col(p.outer(), 0.5F * blockG));
            }

            if (r.hit && !encase) {
               for (int k = 0; k < 14; k++) {
                  float q = seg(r.h, 0.05F, 1.0F);
                  float px = x + (rnd(r.sp.seed + k * 3) - 0.5F) * U * 2.6F + sin(r.sec * 3.0F + (float)k) * 4.0F;
                  float py = y - U * 0.6F + (rnd(r.sp.seed + k * 5) - 0.5F) * U * 1.4F + q * U * 0.6F;
                  puff(b, px, py, U * (0.12F + 0.08F * rnd(k)), 16777215, 0.7F * bell(q));
               }
            }
         }
      );
      r.add(b -> {
         if (!r.hit) {
            converge(b, r, 16, x, y, U * 2.4F, r.u, r.sp.seed + 21);
            b.ringArc(x, gy, U * 1.4F * frost, U * 1.4F * 0.32F * frost, 2.0F, VfxBatch.col(p.mid(), 0.7F * frost), 0.0F, (float) (Math.PI * 2));

            for (int k = 0; k < n; k++) {
               float o = ((float)k - (float)(n - 1) / 2.0F) / ((float)(n - 1) / 2.0F);
               float ao = Math.abs(o);
               float g = eob(seg(r.u, 0.12F + 0.07F * ao + 0.03F * (float)k, 0.62F + 0.05F * (float)k));
               float len = U * (0.9F + 1.2F * (1.0F - ao)) * g;
               float ang = (float) (-Math.PI / 2) + o * 0.6F + (rnd(r.sp.seed + k) - 0.5F) * 0.2F;
               float tx = x + o * U * 1.1F + cos(ang) * len;
               float ty = gy + 2.0F + sin(ang) * len;
               float tw = (float)Math.pow((double)Math.max(0.0F, sin(r.sec * 3.0F + (float)k * 1.3F)), 6.0);
               b.star(tx, ty, (2.0F + 3.0F * tw) * g, 4, 0.0F, VfxBatch.col(p.core(), (0.3F + 0.7F * tw) * g));
            }

            b.glow(x, y, U * 0.8F * r.u, VfxBatch.col(p.outer(), 0.25F * r.u));
         } else {
            float fh = 1.0F - r.h;
            b.glow(x, y, U * (1.2F + 1.2F * eo(r.h)), VfxBatch.col(p.mid(), 0.55F * fh));
            b.glow(x, y, U * 0.8F * fh, VfxBatch.col(p.core(), 0.9F * fh));
            float sr = U * (0.6F + 2.4F * eo(r.h));
            b.ringArc(x, gy, sr, sr * 0.32F, U * 0.35F * fh, VfxBatch.col(p.core(), 0.8F * fh), 0.0F, (float) (Math.PI * 2));
            if (!encase) {
               burst(b, r, 30, x, y - U * 0.2F, r.h, U * 3.0F, U * 2.2F, 2.2F, r.sp.seed + 202, 0.35F);
            } else {
               burst(b, r, 12, x, y, seg(r.h, 0.0F, 0.5F), U * 1.8F, U * 1.2F, 1.6F, r.sp.seed + 203, 0.2F);
               if (blockG > 0.0F) {
                  b.polyLine(x, y, blockR, 6, (float) (Math.PI / 6), 1.6F, VfxBatch.col(p.core(), 0.9F * blockG), 1.0F);
                  b.polyLine(x, y, blockR * 0.72F, 6, 0.6435988F, 0.9F, VfxBatch.col(p.mid(), 0.5F * blockG), 1.0F);

                  for (int v = 0; v < 6; v++) {
                     float a0 = (float) (Math.PI / 6) + (float)v * (float) (Math.PI * 2) / 6.0F;
                     b.line(x, y, x + cos(a0) * blockR, y + sin(a0) * blockR, 0.7F, VfxBatch.col(p.core(), 0.35F * blockG), VfxBatch.col(p.core(), 0.05F));
                  }

                  float gl = r.T * 0.08F % 1.0F;
                  float ga = (float) (Math.PI / 6) + gl * (float) (Math.PI * 2);
                  b.star(x + cos(ga) * blockR, y + sin(ga) * blockR, 4.0F * blockG, 4, 0.0F, VfxBatch.col(p.core(), blockG));
                  snowflake(b, x, y, U * 0.45F * blockG, r.sec * 0.5F, VfxBatch.col(p.core(), 0.35F * blockG), 1.0F);
               }
            }

            flash(b, r, p.mid(), 0.2F * (1.0F - seg(r.h, 0.0F, 0.2F)));
         }
      });
   }

   static void thunderbolt(SkillVfx.R r, boolean storm) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      r.add(
         b -> {
            if (!r.hit) {
               float a = eo(seg(r.u, 0.0F, 0.3F));
               int flick = (int)(r.T * 1.5F);
               int nodes = 6;
               float rx = U * 1.35F;
               float ry = U * 0.6F;
               float rot = r.sec * 5.0F;

               for (int i = 0; i < nodes; i++) {
                  float a0 = rot + (float)i * (float) (Math.PI * 2) / (float)nodes;
                  float a1 = rot + (float)(i + 1) * (float) (Math.PI * 2) / (float)nodes;
                  float x0 = x + cos(a0) * rx;
                  float y0 = y + sin(a0) * ry;
                  float x1 = x + cos(a1) * rx;
                  float y1 = y + sin(a1) * ry;
                  b.glow(x0, y0, 3.5F, VfxBatch.col(p.mid(), 0.9F * a));
                  if (rnd(i * 13 + flick * 7 + r.sp.seed) < 0.25F + 0.55F * r.u) {
                     b.bolt(x0, y0, x1, y1, flick * 31 + i, U * 0.25F, 0.7F, VfxBatch.col(p.outer(), 0.6F * a), VfxBatch.col(p.core(), 0.9F * a), 0);
                  }
               }

               b.glow(x, y, U * (0.4F + 0.8F * r.u) * (0.85F + 0.15F * sin(r.sec * 40.0F)), VfxBatch.col(p.outer(), 0.35F * r.u));
               b.glow(x, y, U * 0.35F * r.u, VfxBatch.col(p.core(), 0.7F * r.u));
               if (r.u > 0.8F && flick % 3 != 0) {
                  float la = seg(r.u, 0.8F, 1.0F);
                  b.bolt(
                     x + U * 0.3F,
                     -8.0F,
                     x,
                     y - U * 0.5F,
                     flick * 17,
                     U * 0.9F,
                     0.5F,
                     VfxBatch.col(p.outer(), 0.35F * la),
                     VfxBatch.col(p.core(), 0.5F * la),
                     1
                  );
               }
            } else {
               int flick = (int)(r.T * 0.8F);
               int strikes = storm ? 3 : 1;

               for (int s = 0; s < strikes; s++) {
                  float q = seg(r.h, (float)s * 0.12F, 0.7F + (float)s * 0.1F);
                  float e = SkillVfx.R.env(q);
                  if (!(e <= 0.0F)) {
                     float sx = x + (s == 0 ? 0.0F : (s == 1 ? -1.0F : 1.0F) * U * 0.7F);
                     b.bolt(
                        sx + U * (rnd(flick + s) - 0.5F) * 1.5F,
                        -10.0F,
                        sx,
                        y,
                        flick * 13 + s * 101,
                        U * 1.4F,
                        2.2F * e,
                        VfxBatch.col(p.outer(), 0.75F * e),
                        VfxBatch.col(p.core(), e),
                        3
                     );
                     b.glow(sx, y, U * 1.4F * e, VfxBatch.col(p.mid(), 0.6F * e));
                  }
               }

               int fl2 = (int)(r.T * 2.0F);
               float fh = 1.0F - r.h;

               for (int ix = 0; ix < 5; ix++) {
                  float a0 = rnd(fl2 * 5 + ix) * (float) (Math.PI * 2);
                  float a1 = a0 + 0.8F + rnd(fl2 + ix * 3);
                  float rr = U * (0.6F + 0.6F * rnd(ix + fl2));
                  b.bolt(
                     x + cos(a0) * rr,
                     y + sin(a0) * rr * 0.8F,
                     x + cos(a1) * rr,
                     y + sin(a1) * rr * 0.8F,
                     fl2 * 7 + ix,
                     U * 0.3F,
                     0.6F,
                     VfxBatch.col(p.outer(), 0.6F * fh),
                     VfxBatch.col(p.core(), 0.9F * fh),
                     0
                  );
               }

               b.glow(x, y, U * (1.5F + 1.5F * eo(r.h)), VfxBatch.col(p.outer(), 0.45F * fh));
               b.glow(x, y, U * 0.9F * r.env(), VfxBatch.col(p.core(), 0.9F * r.env()));
               burst(b, r, 22, x, y, r.h, U * 2.6F, U * 1.0F, 1.8F, r.sp.seed + 303, 0.0F);
               float sr = U * (0.6F + 2.4F * eo(r.h));
               b.ringArc(x, r.gy, sr, sr * 0.32F, U * 0.3F * fh, VfxBatch.col(p.mid(), 0.7F * fh), 0.0F, (float) (Math.PI * 2));
               flash(b, r, 16777215, 0.35F * (1.0F - seg(r.h, 0.0F, 0.15F)) * (flick % 2 == 0 ? 1.0F : 0.5F));
            }
         }
      );
   }

   static void psiBurst(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      r.add(
         b -> {
            float hue = r.sec * 0.5F;
            if (!r.hit) {
               float a = eo(seg(r.u, 0.0F, 0.3F));
               float rad = U * (2.4F - 1.5F * eo(r.u));
               float rot = r.sec * 2.2F;

               for (int k = 0; k < 2; k++) {
                  b.polyLine(
                     x,
                     y,
                     rad * (k == 0 ? 1.0F : 0.7F),
                     6,
                     rot * (k == 0 ? 1.0F : -1.0F) + (float)k * 0.3F,
                     1.4F,
                     VfxBatch.hsv(hue + (float)k * 0.5F, 0.6F, 1.0F, 0.85F * a),
                     1.0F
                  );
               }

               for (int k = 0; k < 3; k++) {
                  float rr = rad * (0.55F + 0.25F * (float)k);
                  int c = VfxBatch.hsv(hue + 0.33F * (float)k, 0.5F, 1.0F, 0.6F * a);

                  for (int d = 0; d < 6; d++) {
                     float a0 = rot * (k % 2 == 0 ? -1.3F : 1.1F) + (float)d * (float) (Math.PI * 2) / 6.0F;
                     b.ringArc(x, y, rr, rr, 1.6F, c, a0, a0 + 0.6F);
                  }
               }

               for (int i = 0; i < 8; i++) {
                  float ang = r.sec * 3.0F + (float)i * (float) (Math.PI * 2) / 8.0F;
                  mat(b, r, x + cos(ang) * rad * 0.9F, y + sin(ang) * rad * 0.5F, 1.6F, ang, a, i);
               }

               b.glow(x, y, U * 0.6F * r.u, VfxBatch.hsv(hue, 0.3F, 1.0F, 0.5F * r.u));
            } else {
               for (int k = 0; k < 5; k++) {
                  float q = seg(r.h, (float)k * 0.07F, 0.7F + (float)k * 0.06F);
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     b.ring(x, y, U * (0.4F + 3.2F * eo(q)), U * 0.35F * (1.0F - q), VfxBatch.hsv(hue + (float)k * 0.18F, 0.6F, 1.0F, 0.8F * (1.0F - q)));
                  }
               }

               burst(b, r, 26, x, y, r.h, U * 3.0F, U * 0.6F, 2.0F, r.sp.seed + 404, 0.0F);
               b.glow(x, y, U * 1.6F * (1.0F - r.h), VfxBatch.col(16777215, 0.8F * (1.0F - r.h)));
               flash(b, r, 16777215, 0.2F * (1.0F - seg(r.h, 0.0F, 0.15F)));
            }
         }
      );
   }

   static void greatSlash(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float ccx = x + U * 1.6F;
      float ccy = y - U * 1.9F;
      float rad = U * 2.6F;
      float mid = (float)Math.atan2((double)(y - ccy), (double)(x - ccx));
      float span = 1.75F;
      float a0 = mid - span / 2.0F;
      float tx = cos(mid + (float) (Math.PI / 2));
      float ty = sin(mid + (float) (Math.PI / 2));
      r.add(b -> {
         if (!r.hit) {
            float a = seg(r.u, 0.2F, 0.9F);

            for (int s = -1; s <= 1; s += 2) {
               float d = U * 2.4F * (1.0F - eo(r.u));
               b.star(x + tx * d * (float)s, y + ty * d * (float)s, 2.5F + 2.0F * a, 4, r.sec * 3.0F, VfxBatch.col(p.core(), 0.9F * a));
            }

            if (r.u > 0.6F) {
               float la = seg(r.u, 0.6F, 1.0F) * (0.4F + 0.6F * Math.abs(sin(r.sec * 40.0F)));
               b.soft(x - tx * U * 3.0F, y - ty * U * 3.0F, x, y, 1.4F, VfxBatch.col(p.core(), 0.0F), VfxBatch.col(p.core(), 0.6F * la));
               b.soft(x, y, x + tx * U * 3.0F, y + ty * U * 3.0F, 1.4F, VfxBatch.col(p.core(), 0.6F * la), VfxBatch.col(p.core(), 0.0F));
            }

            b.glow(x, y, U * 0.5F * r.u, VfxBatch.col(p.outer(), 0.3F * r.u));
            converge(b, r, 14, x, y, U * 2.2F, r.u, r.sp.seed + 501);
            b.crescent(ccx, ccy, rad, U * 0.12F * a, mid, span, VfxBatch.col(p.outer(), 0.0F), VfxBatch.col(p.outer(), 0.35F * a));
         } else {
            float L = eo(seg(r.h, 0.0F, 0.28F));
            float fade = 1.0F - seg(r.h, 0.3F, 0.8F);
            float aL = a0 + span * L;
            float mA = (a0 + aL) / 2.0F;
            float sA = Math.max(0.01F, aL - a0);
            b.crescent(ccx, ccy, rad, U * 0.9F * fade, mA, sA, VfxBatch.col(p.outer(), 0.0F), VfxBatch.col(p.outer(), 0.55F * fade));
            b.crescent(ccx, ccy, rad, U * 0.5F * fade, mA, sA, VfxBatch.col(p.mid(), 0.0F), VfxBatch.col(p.mid(), 0.9F * fade));
            b.crescent(ccx, ccy, rad, U * 0.18F * fade, mA, sA, VfxBatch.col(p.core(), 0.3F * fade), VfxBatch.col(p.core(), fade));
            float cut = 1.0F - seg(r.h, 0.1F, 0.5F);
            b.soft(x - tx * r.sw, y - ty * r.sw, x, y, 1.2F, VfxBatch.col(p.core(), 0.0F), VfxBatch.col(p.core(), 0.7F * cut));
            b.soft(x, y, x + tx * r.sw, y + ty * r.sw, 1.2F, VfxBatch.col(p.core(), 0.7F * cut), VfxBatch.col(p.core(), 0.0F));
            burst(b, r, 18, x, y, seg(r.h, 0.08F, 1.0F), U * 2.4F, U * 0.8F, 1.6F, r.sp.seed + 505, 0.0F);
            float st = 1.0F - seg(r.h, 0.0F, 0.5F);
            b.star(x, y, U * 1.2F * st, 4, 0.3F, VfxBatch.col(p.core(), st));
            b.glow(x, y, U * 1.2F * fade, VfxBatch.col(p.outer(), 0.4F * fade));
         }
      });
   }

   static void holyRay(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float gy = r.gy;
      float hy = y - U * 2.2F;
      r.add(b -> {
         if (!r.hit) {
            float a = eob(seg(r.u, 0.0F, 0.4F));
            float rr = U * 0.75F * a;
            b.glow(x, hy, U * 1.4F * a, VfxBatch.col(p.outer(), 0.3F * a));
            b.ringArc(x, hy, rr, rr * 0.35F, 2.2F, VfxBatch.col(p.core(), 0.9F * a), 0.0F, (float) (Math.PI * 2));

            for (int i = 0; i < 14; i++) {
               float ang = r.sec * 0.8F + (float)i * (float) (Math.PI * 2) / 14.0F;
               float l = U * (0.5F + 0.4F * sin(r.sec * 4.0F + (float)i)) * a;
               b.spark(x + cos(ang) * (rr + l * 0.6F), hy + sin(ang) * (rr * 0.35F + l * 0.25F), ang, l, 1.6F, VfxBatch.col(p.mid(), 0.6F * a));
            }

            for (int i = 0; i < 14; i++) {
               float f = (r.sec * 0.7F + rnd(r.sp.seed + i)) % 1.0F;
               float px = x + (rnd(r.sp.seed + i * 3) - 0.5F) * U * 2.2F;
               mat(b, r, px, mix(hy, gy, f), 1.3F, (float) (Math.PI / 2), bell(f) * a, i);
            }

            float pre = seg(r.u, 0.7F, 1.0F);
            b.soft(x, hy, x, gy, U * 0.3F * pre, VfxBatch.col(p.mid(), 0.3F * pre), VfxBatch.col(p.mid(), 0.15F * pre));
            b.ellipse(x, gy, U * 1.3F * a, U * 0.4F * a, 0.0F, VfxBatch.col(p.outer(), 0.3F * a), VfxBatch.col(p.outer(), 0.0F));
         } else {
            float env = r.env();
            float fh = 1.0F - r.h;
            b.soft(x, -10.0F, x, gy, U * 1.8F * env, VfxBatch.col(p.outer(), 0.5F * env), VfxBatch.col(p.outer(), 0.7F * env));
            b.soft(x, -10.0F, x, gy, U * 0.9F * env, VfxBatch.col(p.mid(), 0.8F * env), VfxBatch.col(p.mid(), 0.9F * env));
            b.soft(x, -10.0F, x, gy, U * 0.35F * env, VfxBatch.col(p.core(), env), VfxBatch.col(p.core(), env));
            b.spark(x, y, 0.0F, U * 5.0F * env, U * 0.35F, VfxBatch.col(p.core(), env));
            b.spark(x, y, (float) (Math.PI / 2), U * 4.0F * env, U * 0.3F, VfxBatch.col(p.core(), 0.9F * env));
            b.glow(x, y, U * 1.6F * fh, VfxBatch.col(p.mid(), 0.6F * fh));

            for (int k = 0; k < 2; k++) {
               float q = seg(r.h, (float)k * 0.15F, 1.0F);
               float rr = U * (0.6F + 2.6F * eo(q));
               b.ringArc(x, gy, rr, rr * 0.32F, U * 0.3F * (1.0F - q), VfxBatch.col(p.mid(), 0.8F * (1.0F - q)), 0.0F, (float) (Math.PI * 2));
            }

            burst(b, r, 24, x, y, r.h, U * 2.6F, -U * 0.6F, 1.8F, r.sp.seed + 606, 0.3F);
            flash(b, r, p.mid(), 0.25F * (1.0F - seg(r.h, 0.0F, 0.2F)));
         }
      });
   }

   static void voidImplosion(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float orbR = !r.hit ? U * (0.2F + 0.55F * eo(r.u)) : U * 0.75F * (1.0F - eo(seg(r.h, 0.0F, 0.14F)));
      float tA = eo(seg(r.u, 0.0F, 0.3F));
      r.alpha(
         b -> {
            if (!r.hit) {
               for (int k = 0; k < 6; k++) {
                  float base = (float)k * (float) (Math.PI * 2) / 6.0F + r.sec * 1.6F;
                  float px = 0.0F;
                  float py = 0.0F;

                  for (int s = 0; s <= 12; s++) {
                     float f = (float)s / 12.0F;
                     float rr = mix(U * 2.6F, orbR, f);
                     float ang = base + f * 2.2F;
                     float qx = x + cos(ang) * rr;
                     float qy = y + sin(ang) * rr * 0.8F;
                     if (s > 0) {
                        float f0 = (float)(s - 1) / 12.0F;
                        b.taper(
                           px,
                           py,
                           qx,
                           qy,
                           U * (0.04F + 0.2F * f0),
                           U * (0.04F + 0.2F * f),
                           VfxBatch.col(p.dark(), 0.8F * tA * f0),
                           VfxBatch.col(p.dark(), 0.85F * tA * f)
                        );
                     }

                     px = qx;
                     py = qy;
                  }
               }
            }

            if (orbR > 0.3F) {
               b.disc(x, y, orbR * 1.25F, VfxBatch.col(p.dark(), 0.95F), VfxBatch.col(p.dark(), 0.0F));
               b.disc(x, y, orbR, VfxBatch.col(0, 0.95F), VfxBatch.col(p.dark(), 0.95F));
            }

            if (r.hit) {
               float q = seg(r.h, 0.12F, 1.0F);
               if (q > 0.0F && q < 1.0F) {
                  b.ring(x, y, U * (0.6F + 2.6F * eo(q)), U * 0.6F * (1.0F - q), VfxBatch.col(p.dark(), 0.7F * (1.0F - q)));

                  for (int k = 0; k < 8; k++) {
                     float ang = rnd(r.sp.seed + k) * (float) (Math.PI * 2);
                     float d = U * (0.5F + 1.8F * eo(q) * (0.5F + 0.5F * rnd(k + 9)));
                     puff(b, x + cos(ang) * d, y + sin(ang) * d * 0.8F - q * U * 0.5F, U * (0.3F + 0.5F * q), p.dark(), 0.6F * (1.0F - q));
                  }
               }
            }
         }
      );
      r.add(b -> {
         if (!r.hit) {
            for (int k = 0; k < 6; k++) {
               float base = (float)k * (float) (Math.PI * 2) / 6.0F + r.sec * 1.6F;
               float px = 0.0F;
               float py = 0.0F;

               for (int s = 0; s <= 12; s++) {
                  float f = (float)s / 12.0F;
                  float rr = mix(U * 2.6F, orbR, f);
                  float ang = base + f * 2.2F;
                  float qx = x + cos(ang) * rr;
                  float qy = y + sin(ang) * rr * 0.8F;
                  if (s > 0) {
                     b.soft(px, py, qx, qy, 1.6F, VfxBatch.col(p.mid(), 0.5F * tA * (float)(s - 1) / 12.0F), VfxBatch.col(p.mid(), 0.5F * tA * f));
                  }

                  px = qx;
                  py = qy;
               }
            }

            b.ring(x, y, orbR * 1.08F, 3.2F, VfxBatch.col(p.mid(), 0.9F * tA));
            b.ring(x, y, orbR * 1.35F + sin(r.sec * 9.0F) * 1.5F, 2.0F, VfxBatch.col(p.accent(), 0.5F * tA));
            b.glow(x, y, orbR * 2.6F, VfxBatch.col(p.outer(), 0.4F * tA));
            converge(b, r, 20, x, y, U * 2.8F, r.u, r.sp.seed + 31);
         } else {
            float imp = 1.0F - seg(r.h, 0.0F, 0.14F);
            if (orbR > 0.3F) {
               b.ring(x, y, orbR * 1.08F + 1.0F, 3.0F, VfxBatch.col(p.accent(), imp));
            }

            float iq = seg(r.h, 0.0F, 0.16F);
            if (iq < 1.0F) {
               b.ring(x, y, U * (2.2F * (1.0F - eo(iq)) + 0.2F), 4.0F, VfxBatch.col(p.mid(), 0.9F * (1.0F - iq * 0.5F)));
               b.glow(x, y, U * 1.2F, VfxBatch.col(p.outer(), 0.5F));
            }

            float fl = bell(seg(r.h, 0.08F, 0.3F));
            b.star(x, y, U * 1.6F * fl, 4, 0.4F, VfxBatch.col(p.accent(), fl));
            b.glow(x, y, U * 0.9F * fl, VfxBatch.col(p.core(), fl));
            float q = seg(r.h, 0.12F, 1.0F);
            if (q > 0.0F && q < 1.0F) {
               b.ring(x, y, U * (0.6F + 2.6F * eo(q)), U * 0.25F * (1.0F - q), VfxBatch.col(p.mid(), 0.9F * (1.0F - q)));
               b.glow(x, y, U * 1.5F * (1.0F - q), VfxBatch.col(p.accent(), 0.6F * (1.0F - q)));
               burst(b, r, 24, x, y, q, U * 2.8F, 0.0F, 1.9F, r.sp.seed + 707, 0.0F);
            }

            flash(b, r, p.accent(), 0.18F * bell(seg(r.h, 0.08F, 0.3F)));
         }
      });
   }

   static void meteorCrash(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float gy = r.gy;
      float fallQ = ein(seg(r.u, 0.3F, 1.0F));
      float sx = x - U * 2.2F;
      float sy = -U * 1.5F;
      float mx = mix(sx, x, fallQ);
      float my = mix(sy, y, fallQ);
      r.alpha(b -> {
         if (!r.hit) {
            float sh = seg(r.u, 0.3F, 1.0F);
            float sr = U * (0.4F + 0.9F * sh);
            b.ellipse(x, gy, sr, sr * 0.3F, 0.0F, VfxBatch.col(0, 0.45F * sh), VfxBatch.col(0, 0.0F));
            if (r.u > 0.3F) {
               rock(b, r, mx, my, U * 0.32F, r.sec * 6.0F, 1.0F);
            }
         } else {
            float q = r.h;
            float ca = 1.0F - seg(q, 0.6F, 1.0F);
            float grow = eo(seg(q, 0.0F, 0.2F));

            for (int k = 0; k < 9; k++) {
               float ang = (float)k * (float) (Math.PI * 2) / 9.0F + rnd(r.sp.seed + k) * 0.4F;
               float len = U * (1.0F + 0.9F * rnd(k + 5)) * grow;
               float mxk = x + cos(ang) * len * 0.5F + (rnd(k * 3) - 0.5F) * 4.0F;
               float myk = gy + sin(ang) * len * 0.5F * 0.32F;
               float ex = x + cos(ang) * len;
               float ey = gy + sin(ang) * len * 0.32F;
               b.taper(x, gy, mxk, myk, 2.4F, 1.4F, VfxBatch.col(1313800, 0.85F * ca), VfxBatch.col(1313800, 0.7F * ca));
               b.taper(mxk, myk, ex, ey, 1.4F, 0.3F, VfxBatch.col(1313800, 0.7F * ca), VfxBatch.col(1313800, 0.0F));
            }

            for (int k = 0; k < 10; k++) {
               float qq = seg(q, 0.02F * (float)k, 1.0F);
               float ang = rnd(r.sp.seed + k * 7) * (float) (Math.PI * 2);
               float d = U * (0.6F + 1.8F * eo(qq));
               puff(b, x + cos(ang) * d, gy - U * 0.2F + sin(ang) * d * 0.3F - qq * U * 0.6F, U * (0.4F + 0.7F * qq), p.accent(), 0.5F * bell(qq));
            }

            for (int k = 0; k < 12; k++) {
               float ang = (float) (-Math.PI / 2) + (rnd(r.sp.seed + k * 13) - 0.5F) * 2.4F;
               float v = U * (2.0F + 2.2F * rnd(k + 77));
               float px = x + cos(ang) * v * q;
               float py = y + sin(ang) * v * q + U * 4.5F * q * q;
               rock(b, r, px, py, U * (0.1F + 0.12F * rnd(k)), r.sec * 9.0F * (rnd(k + 3) - 0.5F) + (float)k, 1.0F - seg(q, 0.7F, 1.0F));
            }
         }
      });
      r.add(b -> {
         if (!r.hit) {
            float wa = seg(r.u, 0.1F, 0.4F);

            for (int k = 0; k < 2; k++) {
               float f = (r.sec * 1.4F + (float)k * 0.5F) % 1.0F;
               float rr = U * (1.6F - 1.1F * f);
               b.ringArc(x, gy, rr, rr * 0.32F, 2.0F, VfxBatch.col(p.outer(), 0.7F * wa * f), 0.0F, (float) (Math.PI * 2));
            }

            b.ringArc(x, gy, U * 0.5F, U * 0.16F, 1.6F, VfxBatch.col(p.mid(), 0.8F * wa), 0.0F, (float) (Math.PI * 2));
            if (r.u > 0.3F) {
               float dxm = x - sx;
               float dym = y - sy;
               float len = (float)Math.sqrt((double)(dxm * dxm + dym * dym));
               float tx = mx - dxm / len * U * 2.6F;
               float ty = my - dym / len * U * 2.6F;
               b.softTaper(tx, ty, mx, my, 0.0F, U * 1.2F, VfxBatch.col(p.outer(), 0.0F), VfxBatch.col(p.outer(), 0.7F));
               b.softTaper(tx, ty, mx, my, 0.0F, U * 0.55F, VfxBatch.col(p.mid(), 0.0F), VfxBatch.col(p.core(), 0.9F));
               b.glow(mx, my, U * 1.1F, VfxBatch.col(p.mid(), 0.7F));
               b.glow(mx, my, U * 0.45F, VfxBatch.col(p.core(), 1.0F));
            }

            float g = seg(r.u, 0.5F, 1.0F);
            b.glow(x, gy, U * 0.8F * g, VfxBatch.col(p.outer(), 0.3F * g));
         } else {
            float fh = 1.0F - r.h;
            b.glow(x, y, U * (1.4F + 1.4F * eo(r.h)), VfxBatch.col(p.mid(), 0.55F * fh));
            b.glow(x, y, U * 1.0F * fh, VfxBatch.col(p.core(), 0.95F * fh));

            for (int k = 0; k < 2; k++) {
               float q = seg(r.h, (float)k * 0.12F, 1.0F);
               float rr = U * (0.6F + 3.0F * eo(q));
               b.ringArc(x, gy, rr, rr * 0.32F, U * 0.5F * (1.0F - q), VfxBatch.col(p.mid(), 0.8F * (1.0F - q)), 0.0F, (float) (Math.PI * 2));
            }

            burst(b, r, 20, x, y, r.h, U * 2.6F, U * 1.5F, 1.6F, r.sp.seed + 808, 0.5F);
            flash(b, r, p.core(), 0.25F * (1.0F - seg(r.h, 0.0F, 0.15F)));
         }
      });
   }

   static void moonbeam(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float gy = r.gy;
      float my = y - U * 2.4F;
      r.add(b -> {
         float a = eob(seg(r.u, 0.0F, 0.45F)) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
         b.glow(x, my, U * 1.6F * a, VfxBatch.col(p.outer(), 0.35F * a));
         b.crescent(x, my, U * 0.7F, U * 0.5F * a, (float) (Math.PI * 4.0 / 5.0), 2.6F * a + 0.01F, VfxBatch.col(p.mid(), 0.6F * a), VfxBatch.col(p.core(), a));

         for (int i = 0; i < 7; i++) {
            float ang = rnd(r.sp.seed + i) * (float) (Math.PI * 2);
            float d = U * (1.0F + 1.3F * rnd(i + 3));
            float tw = (float)Math.pow((double)Math.max(0.0F, sin(r.sec * 3.0F + (float)i * 1.9F)), 3.0);
            b.star(x + cos(ang) * d, my + sin(ang) * d * 0.6F, (2.0F + 2.0F * tw) * a, 4, 0.0F, VfxBatch.col(p.core(), (0.4F + 0.6F * tw) * a));
         }

         if (!r.hit) {
            for (int i = 0; i < 10; i++) {
               float f = (r.sec * 0.5F + rnd(r.sp.seed + i * 5)) % 1.0F;
               float px = x + (rnd(i * 9 + r.sp.seed) - 0.5F) * U * 2.0F + sin(r.sec * 2.0F + (float)i) * 3.0F;
               mat(b, r, px, mix(my, gy, f), 1.2F, 0.0F, bell(f) * a, i);
            }
         } else {
            float env = r.env();
            float fh = 1.0F - r.h;
            b.soft(x, my, x, gy, U * 1.6F * env, VfxBatch.col(p.outer(), 0.45F * env), VfxBatch.col(p.outer(), 0.6F * env));
            b.soft(x, my, x, gy, U * 0.6F * env, VfxBatch.col(p.core(), 0.9F * env), VfxBatch.col(p.core(), env));

            for (int k = 0; k < 3; k++) {
               float q = seg(r.h, (float)k * 0.12F, 1.0F);
               float rr = U * (0.5F + 2.4F * eo(q));
               b.ringArc(x, gy, rr, rr * 0.32F, 2.5F, VfxBatch.col(p.mid(), 0.8F * (1.0F - q)), 0.0F, (float) (Math.PI * 2));
            }

            b.glow(x, y, U * 1.3F * fh, VfxBatch.col(p.mid(), 0.6F * fh));
            burst(b, r, 18, x, y, r.h, U * 2.4F, -U * 0.4F, 1.8F, r.sp.seed + 909, 0.2F);
         }
      });
   }

   static void soulBurst(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      r.add(b -> {
         if (!r.hit) {
            converge(b, r, 20, x, y, U * 2.4F, r.u, r.sp.seed + 41);
            float g = eo(r.u);
            b.glow(x, y, U * (0.4F + 0.9F * g), VfxBatch.col(p.outer(), 0.4F * g));
            b.glow(x, y, U * 0.4F * g, VfxBatch.col(p.core(), 0.9F * g));
            float rr = U * 0.9F * g;
            float rr2 = U * 1.2F * g;
            b.ringArc(x, y, rr, rr, 1.8F, VfxBatch.col(p.mid(), 0.8F * g), r.sec * 4.0F, r.sec * 4.0F + 4.4F);
            b.ringArc(x, y, rr2, rr2 * 0.5F, 1.4F, VfxBatch.col(p.core(), 0.5F * g), -r.sec * 3.0F, -r.sec * 3.0F + 3.6F);
         } else {
            float fh = 1.0F - r.h;
            float env = r.env();

            for (int k = 0; k < 10; k++) {
               float ang = (float)k * (float) (Math.PI * 2) / 10.0F + (float)(r.sp.seed % 7) * 0.1F;
               float len = U * (2.2F + 1.4F * (float)(k % 2)) * env;
               b.spark(x + cos(ang) * len * 0.5F, y + sin(ang) * len * 0.5F, ang, len, U * 0.28F, VfxBatch.col(p.mid(), 0.9F * env));
            }

            b.glow(x, y, U * (1.0F + 1.6F * eo(r.h)), VfxBatch.col(p.outer(), 0.5F * fh));
            b.glow(x, y, U * 0.8F * fh, VfxBatch.col(p.core(), fh));
            b.ring(x, y, U * (0.5F + 2.6F * eo(r.h)), U * 0.35F * fh, VfxBatch.col(p.mid(), 0.8F * fh));
            burst(b, r, 22, x, y, r.h, U * 2.6F, U * 0.5F, 1.8F, r.sp.seed + 111, 0.0F);
            flash(b, r, p.outer(), 0.15F * (1.0F - seg(r.h, 0.0F, 0.15F)));
         }
      });
   }

   static void pierceLance(SkillVfx.R r) {
      if (r.hit) {
         float q = seg(r.h, 0.0F, 0.4F);
         if (!(q <= 0.0F) && !(q >= 1.0F)) {
            SkillVfx.Pal p = r.p;
            float x = r.cx;
            float y = r.cy;
            float U = r.U;
            float ang = 0.42F;
            float dx = cos(ang);
            float dy = sin(ang);
            float travel = mix(-U * 6.0F, U * 6.0F, eo(q));
            float hx = x + dx * travel;
            float hy = y + dy * travel;
            float tail = U * 4.0F;
            float a = 1.0F - seg(q, 0.5F, 1.0F);
            r.add(
               b -> {
                  b.softTaper(hx - dx * tail, hy - dy * tail, hx, hy, 0.0F, U * 0.8F, VfxBatch.col(p.outer(), 0.0F), VfxBatch.col(p.outer(), 0.65F * a));
                  b.softTaper(hx - dx * tail, hy - dy * tail, hx, hy, 0.0F, U * 0.3F, VfxBatch.col(p.core(), 0.0F), VfxBatch.col(p.core(), a));
                  b.glow(hx, hy, U * 0.6F, VfxBatch.col(p.core(), 0.8F * a));

                  for (int k = 1; k <= 3; k++) {
                     float o = (float)k * 5.0F;
                     b.soft(
                        hx - dx * tail * 1.3F - dy * o,
                        hy - dy * tail * 1.3F + dx * o,
                        hx - dx * U - dy * o,
                        hy - dy * U + dx * o,
                        0.9F,
                        VfxBatch.col(p.mid(), 0.0F),
                        VfxBatch.col(p.mid(), 0.4F * a / (float)k)
                     );
                     b.soft(
                        hx - dx * tail * 1.3F + dy * o,
                        hy - dy * tail * 1.3F - dx * o,
                        hx - dx * U + dy * o,
                        hy - dy * U - dx * o,
                        0.9F,
                        VfxBatch.col(p.mid(), 0.0F),
                        VfxBatch.col(p.mid(), 0.4F * a / (float)k)
                     );
                  }

                  if (travel > 0.0F) {
                     b.ring(x, y, U * (0.3F + 1.6F * seg(q, 0.5F, 1.0F)), 2.5F, VfxBatch.col(p.core(), 0.8F * a));
                  }
               }
            );
         }
      }
   }

   static void psiSchool(SkillVfx.R r) {
      switch (r.th) {
         case FIRE:
            psiFire(r);
            break;
         case ICE:
            psiFreeze(r);
            break;
         case ELECTRIC:
            psiThunder(r);
            break;
         case PSI:
         case SLASH:
         case LIGHT:
         case DARK:
         default:
            psiBurst(r);
            break;
         case IMPACT:
            psiRockin(r);
      }
   }

   static int pc(SkillVfx.R r, float off, float s, float v, float a) {
      float t = r.sec * 1.6F + off * (float) (Math.PI * 2);

      float h = switch (r.th) {
         case FIRE -> 0.97F + 0.11F * sin(t);
         case ICE -> 0.6F + 0.11F * sin(t);
         case ELECTRIC -> 0.2F + 0.09F * sin(t);
         default -> r.sec * 0.45F + off;
      };
      return VfxBatch.hsv(h, s, v, a);
   }

   private static float[] arcPts(float cx, float cy, float rx, float ry, float a0, float a1, int n) {
      float[] p = new float[(n + 1) * 2];

      for (int i = 0; i <= n; i++) {
         float a = a0 + (a1 - a0) * (float)i / (float)n;
         p[i * 2] = cx + (float)Math.cos((double)a) * rx;
         p[i * 2 + 1] = cy + (float)Math.sin((double)a) * ry;
      }

      return p;
   }

   private static float[] join(float[]... parts) {
      int n = 0;

      for (float[] q : parts) {
         n += q.length;
      }

      float[] o = new float[n];
      int i = 0;

      for (float[] q : parts) {
         System.arraycopy(q, 0, o, i, q.length);
         i += q.length;
      }

      return o;
   }

   static void glyph(VfxBatch b, float x, float y, float s, int tier, float rot, float w, int c) {
      if (c >>> 24 != 0 && !(s < 1.0F)) {
         float cs = cos(rot);
         float sn = sin(rot);

         for (float[] st : GLYPHS[Math.max(1, Math.min(4, tier)) - 1]) {
            float px = 0.0F;
            float py = 0.0F;

            for (int i = 0; i + 1 < st.length; i += 2) {
               float gx = st[i] * s;
               float gy = st[i + 1] * s;
               float qx = x + gx * cs - gy * sn;
               float qy = y + gx * sn + gy * cs;
               if (i > 0) {
                  b.line(px, py, qx, qy, w, c, c);
               }

               px = qx;
               py = qy;
            }
         }
      }
   }

   static void waveBands(
      VfxBatch b, SkillVfx.R r, float x, float y, float w, float h, int n, float amp, float freq, float phase, float tear, float a, float hueOff
   ) {
      if (!(a <= 0.01F) && !(w < 1.0F)) {
         float bh = h / (float)n;

         for (int i = 0; i < n; i++) {
            float yy = y - h / 2.0F + (float)i * bh;
            float off = sin(phase + (float)i * freq) * amp + (i % 2 == 0 ? 1.0F : -1.0F) * tear * (0.6F + 0.4F * rnd(i * 13 + 5));
            float hw = w / 2.0F * (0.7F + 0.3F * sin(phase * 0.7F + (float)i * 1.3F));
            float edge = bell(((float)i + 0.5F) / (float)n);
            int c = pc(r, hueOff + (float)i * 0.07F, 0.7F, 1.0F, a * (0.35F + 0.65F * edge) * (i % 2 == 0 ? 1.0F : 0.55F));
            int cl = c & 16777215;
            float bx = x + off;
            float y1 = yy + bh * 0.8F;
            b.quad(bx - hw, yy, cl, bx, yy, c, bx, y1, c, bx - hw, y1, cl);
            b.quad(bx, yy, c, bx + hw, yy, cl, bx + hw, y1, cl, bx, y1, c);
         }
      }
   }

   static void wave(VfxBatch b, float x0, float x1, float y, float amp, float cycles, float phase, int shape, float w, int c) {
      if (c >>> 24 != 0) {
         int n = shape == 1 ? 96 : 48;
         float px = 0.0F;
         float py = 0.0F;
         int pcol = 0;

         for (int i = 0; i <= n; i++) {
            float s = (float)i / (float)n;
            float ph = s * cycles * (float) (Math.PI * 2) + phase;

            float v = switch (shape) {
               case 1 -> sin(ph) >= 0.0F ? 1.0F : -1.0F;
               case 2 -> (float)(0.6366197723675814 * Math.asin(Math.sin((double)ph)));
               default -> sin(ph);
            };
            float qx = mix(x0, x1, s);
            float qy = y + v * amp;
            int qc = VfxBatch.mulA(c, (float)Math.sqrt((double)bell(s)));
            if (i > 0) {
               b.line(px, py, qx, qy, w, pcol, qc);
            }

            px = qx;
            py = qy;
            pcol = qc;
         }
      }
   }

   static void signal(VfxBatch b, float x, float y0, float y1, float amp, float step, float phase, float w, int glow, int core) {
      if (!(amp < 0.3F) && !(step < 1.0F)) {
         float ph = phase % 2.0F;
         if (ph < 0.0F) {
            ph += 2.0F;
         }

         float yy = y0 - ph * step;

         for (int k = 0; yy < y1; k++) {
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            float ya = Math.max(yy, y0);
            float yb = Math.min(yy + step, y1);
            if (yb > ya) {
               b.beam(x + side * amp, ya, x + side * amp, yb, w, glow, core);
            }

            float yn = yy + step;
            if (yn > y0 && yn < y1) {
               b.beam(x + side * amp, yn, x - side * amp, yn, w, glow, core);
            }

            yy = yn;
         }
      }
   }

   static void aperture(VfxBatch b, SkillVfx.R r, float x, float y, float rMax, int sides, int n, float flow, float w, float a, float hueOff) {
      if (!(a <= 0.01F)) {
         float base = sides == 3 ? (float) (-Math.PI / 2) : (sides == 4 ? (float) (Math.PI / 4) : 0.0F);

         for (int k = 0; k < n; k++) {
            float f = ((float)k / (float)n + flow) % 1.0F;
            if (f < 0.0F) {
               f++;
            }

            float al = a * bell(f);
            if (!(al <= 0.01F)) {
               float rot = base + (k % 2 == 0 ? 1.0F : -1.0F) * r.sec * 0.8F * (sides == 3 ? 0.0F : 1.0F);
               b.polyLine(x, y, rMax * (1.0F - f), sides, rot, w, pc(r, hueOff + (float)k * 0.13F, 0.65F, 1.0F, al), 1.0F);
            }
         }
      }
   }

   static void psiCharge(VfxBatch b, SkillVfx.R r, int sides, int waveShape) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float in = eo(seg(r.u, 0.0F, 0.3F));
      float out = r.hit ? 1.0F - seg(r.h, 0.0F, 0.3F) : 1.0F;
      float a = in * out;
      if (!(a <= 0.01F)) {
         float gs = U * (1.45F + 0.08F * sin(r.sec * 5.0F)) * (r.hit ? 1.0F + 0.9F * eo(r.h) : 1.0F);
         glyph(b, x, y - U * 0.25F, gs, r.sp.tier, sin(r.sec * 1.3F) * 0.08F, 2.8F, pc(r, 0.5F, 0.35F, 1.0F, 0.5F * a));
         if (!r.hit) {
            aperture(b, r, x, y, U * 2.7F, sides, 4, r.sec * (0.3F + 0.7F * r.u), 1.4F, 0.8F * a, 0.0F);
            float wa = seg(r.u, 0.2F, 0.7F) * a;

            for (int k = -1; k <= 1; k += 2) {
               wave(
                  b,
                  x - U * 3.2F,
                  x + U * 3.2F,
                  y + (float)k * U * 0.95F,
                  U * 0.12F * (0.4F + 0.6F * r.u),
                  3.0F + 6.0F * r.u,
                  r.sec * 9.0F * (float)k,
                  waveShape,
                  1.1F,
                  pc(r, 0.25F + (float)k * 0.1F, 0.6F, 1.0F, 0.55F * wa)
               );
            }
         }
      }
   }

   static void psiFire(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int T = r.sp.tier;
      int seed = r.sp.seed;
      r.add(
         b -> {
            psiCharge(b, r, 3, 0);
            if (!r.hit) {
               float a = eo(seg(r.u, 0.1F, 0.6F));
               waveBands(b, r, x, y, U * 3.0F, U * 2.6F, 12, U * 0.2F * (0.3F + 0.7F * r.u), 0.9F, -r.sec * 9.0F, 0.0F, 0.4F * a, 0.0F);
               float d = U * 2.1F * (1.0F - eo(r.u));
               float rr = U * (0.6F + 0.35F * (1.0F - r.u));
               float spin = (1.0F - eo(r.u)) * 4.0F;
               b.polyLine(x, y - d, rr, 3, (float) (-Math.PI / 2) + spin, 1.8F, pc(r, 0.0F, 0.7F, 1.0F, 0.95F * a), 1.0F);
               b.polyLine(x, y + d, rr, 3, (float) (-Math.PI / 2) + spin, 1.8F, pc(r, 0.5F, 0.7F, 1.0F, 0.95F * a), 1.0F);

               for (int i = 0; i < 8 + 2 * T; i++) {
                  float f = (r.u * 2.0F + rnd(seed + i)) % 1.0F;
                  float rad = U * 2.4F * (1.0F - f);
                  float ang = rnd(seed + i * 7) * (float) (Math.PI * 2) + f * 2.5F;
                  b.polyLine(
                     x + cos(ang) * rad,
                     y + sin(ang) * rad * 0.8F,
                     3.5F + 2.0F * rnd(seed + i * 3),
                     3,
                     (float) (-Math.PI / 2) + r.sec * 4.0F * (i % 2 == 0 ? 1.0F : -1.0F),
                     1.2F,
                     pc(r, rnd(i), 0.7F, 1.0F, bell(f) * a),
                     1.0F
                  );
               }

               b.glow(x, y, U * 0.55F * r.u, pc(r, 0.2F, 0.35F, 1.0F, 0.5F * r.u));
            } else {
               float h = r.h;
               float env = r.env();
               int tick = (int)r.T;
               float bandW = U * (2.6F + 0.9F * (float)T);
               waveBands(b, r, x, y, bandW, U * 2.8F, 12, U * 0.35F, 0.9F, -r.sec * 14.0F, eo(h) * U * 2.2F, 0.6F * (1.0F - seg(h, 0.15F, 0.85F)), 0.1F);
               if (T >= 4) {
                  waveBands(b, r, r.sw / 2.0F, y, r.sw * 1.1F, U * 4.0F, 18, U * 0.6F, 0.45F, r.sec * 16.0F, eo(h) * U * 1.5F, 0.25F * env, 0.3F);
               }

               float hx = 1.0F - seg(h, 0.05F, 0.6F);
               float hr = U * 0.95F * (1.0F + 0.25F * eo(h));
               b.poly(x, y, hr, 6, 0.0F, pc(r, 0.3F, 0.25F, 1.0F, 0.5F * hx), pc(r, 0.6F, 0.7F, 1.0F, 0.0F));
               b.polyLine(x, y, hr, 3, (float) (-Math.PI / 2), 2.4F, pc(r, 0.0F, 0.6F, 1.0F, hx), 1.0F);
               b.ringArc(x, y, hr * 1.08F, hr * 1.08F, 1.6F, pc(r, 0.5F, 0.6F, 1.0F, 0.8F * hx), 0.0F, (float) (Math.PI * 2));

               for (int k = 0; k < 4 + T; k++) {
                  float q = seg(h, (float)k * 0.06F, 0.55F + (float)k * 0.07F);
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     b.polyLine(
                        x,
                        y,
                        U * (0.5F + 3.0F * eo(q)),
                        3,
                        (float) (-Math.PI / 2) + q * 0.25F,
                        U * 0.12F * (1.0F - q) + 1.0F,
                        pc(r, (float)k * 0.15F, 0.7F, 1.0F, 0.9F * (1.0F - q)),
                        1.0F
                     );
                  }
               }

               b.glow(x, y, U * 1.3F * env, pc(r, 0.1F, 0.3F, 1.0F, 0.75F * env));

               for (int i = 0; i < 5 + 2 * T; i++) {
                  float q = seg(h, 0.1F + 0.05F * (float)(i % 5), 0.75F + 0.05F * (float)(i % 5));
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     float bx = x + (rnd(seed + i * 5) - 0.5F) * U * 1.4F + sin(q * 6.0F + (float)i) * U * 0.25F;
                     float by = y + U * 0.4F - q * U * (1.4F + 0.6F * rnd(seed + i));
                     float fl = (tick + i) % 3 == 0 ? 0.45F : 1.0F;
                     b.polyLine(
                        bx,
                        by,
                        U * (0.12F + 0.08F * rnd(seed + i * 9)),
                        3,
                        (float) (-Math.PI / 2) + q * 3.0F * (i % 2 == 0 ? 1.0F : -1.0F),
                        1.3F,
                        pc(r, rnd(i * 3), 0.7F, 1.0F, bell(q) * fl),
                        1.0F
                     );
                  }
               }

               float sf = 1.0F - seg(h, 0.0F, 0.25F);
               flash(b, r, (tick & 1) == 0 ? 16724080 : 16756768, 0.28F * sf);
            }
         }
      );
   }

   private static int[][] buildHex(int rings) {
      List l = new ArrayList<>();

      for (int d = 0; d <= rings; d++) {
         for (int q = -rings; q <= rings; q++) {
            for (int rr = -rings; rr <= rings; rr++) {
               if ((Math.abs(q) + Math.abs(rr) + Math.abs(q + rr)) / 2 == d) {
                  l.add(new int[]{q, rr, d});
               }
            }
         }
      }

      return l.toArray(new int[0][]);
   }

   private static void hexCell(SkillVfx.R r, int i, float s, float sh, float[] out) {
      int[] hc = HEX[i];
      float cx = s * 1.7320508F * ((float)hc[0] + (float)hc[1] / 2.0F);
      float cy = s * 1.5F * (float)hc[1];
      float len = (float)Math.sqrt((double)(cx * cx + cy * cy));
      float dx;
      float dy;
      if (len < 0.01F) {
         float a = rnd(r.sp.seed + 77) * (float) (Math.PI * 2);
         dx = cos(a);
         dy = sin(a);
      } else {
         dx = cx / len;
         dy = cy / len;
      }

      float push = eo(sh) * r.U * (0.6F + 0.55F * (float)hc[2]);
      out[0] = r.cx + cx + dx * push;
      out[1] = r.cy - r.U * 0.1F + cy + dy * push + sh * sh * r.U * 0.7F;
      out[2] = sh * (rnd(r.sp.seed + i * 7) - 0.5F) * 2.4F;
   }

   static void psiFreeze(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int T = r.sp.tier;
      int rings = Math.min(3, T);
      int cells = 1 + 3 * rings * (rings + 1);
      float s = U * 0.36F;
      int tick = (int)r.T;
      float shat = r.hit ? (float)Math.floor((double)(seg(r.h, 0.3F, 1.0F) * 6.0F)) / 6.0F : 0.0F;
      float[] cp = new float[3];
      if (r.hit) {
         r.alpha(
            b -> {
               for (int i = 0; i < cells; i++) {
                  float d = (float)HEX[i][2];
                  float lock = seg(r.h, d * 0.05F, 0.12F + d * 0.05F);
                  float a = 0.42F * lock * (1.0F - shat);
                  if (!(a <= 0.01F)) {
                     hexCell(r, i, s, shat, cp);
                     b.poly(
                        cp[0],
                        cp[1],
                        s * 0.9F,
                        6,
                        (float) (Math.PI / 6) + cp[2],
                        pc(r, d * 0.15F, 0.2F, 1.0F, a),
                        pc(r, d * 0.15F + 0.3F, 0.55F, 1.0F, a * 0.7F)
                     );
                  }
               }
            }
         );
      }

      r.add(b -> {
         psiCharge(b, r, 6, 2);

         for (int i = 0; i < cells; i++) {
            float d = (float)HEX[i][2];
            float grow = r.hit ? 1.0F : eob(seg(r.u, 0.1F + 0.45F * d / (float)rings + 0.05F * rnd(r.sp.seed + i), 0.35F + 0.45F * d / (float)rings));
            float a = (r.hit ? 1.0F - shat : seg(r.u, 0.1F, 0.4F)) * clamp01(grow);
            if (!(a <= 0.01F) && !(grow <= 0.01F)) {
               hexCell(r, i, s, shat, cp);
               float tw = (float)Math.pow((double)Math.max(0.0F, sin(r.sec * 4.0F + (float)i * 1.7F)), 8.0);
               b.polyLine(cp[0], cp[1], s * grow, 6, (float) (Math.PI / 6) + cp[2], 1.2F, pc(r, d * 0.15F, 0.55F, 1.0F, (0.75F + 0.25F * tw) * a), 1.0F);
               if (!r.hit) {
                  b.rect(cp[0], cp[1], 2.0F, 2.0F, 0.0F, pc(r, 0.5F, 0.2F, 1.0F, 0.8F * tw * a));
               }
            }
         }

         if (!r.hit) {
            float a = eo(seg(r.u, 0.05F, 0.4F));
            float d = U * (2.5F - 1.2F * eo(r.u));
            float L = U * 0.45F;
            int c = pc(r, 0.3F, 0.35F, 1.0F, 0.9F * a);

            for (int sx = -1; sx <= 1; sx += 2) {
               for (int sy = -1; sy <= 1; sy += 2) {
                  float bx = x + (float)sx * d;
                  float by = y - U * 0.1F + (float)sy * d * 0.85F;
                  b.line(bx, by, bx - (float)sx * L, by, 1.6F, c, c);
                  b.line(bx, by, bx, by - (float)sy * L, 1.6F, c, c);
               }
            }

            b.glow(x, y, U * 0.6F * r.u, pc(r, 0.5F, 0.3F, 1.0F, 0.35F * r.u));
         } else {
            float h = r.h;
            float lockIn = eob(seg(h, 0.0F, 0.22F));
            float fade = 1.0F - seg(h, 0.72F, 1.0F);

            for (int k = 0; k < 3; k++) {
               float rr = U * (0.95F + (0.28F + (T >= 4 ? 0.12F : 0.0F)) * (float)k) * mix(2.2F, 1.0F, lockIn);
               float rot = k == 1 ? (float)Math.floor((double)(r.T / 3.0F)) * (float) (Math.PI / 12) : 0.0F;
               b.polyLine(x, y - U * 0.1F, rr, 4, rot, 1.8F - 0.4F * (float)k, pc(r, 0.2F * (float)k, 0.35F, 1.0F, 0.9F * fade * clamp01(lockIn)), 1.0F);
               if (k == 2) {
                  for (int v = 0; v < 4; v++) {
                     float va = (float)v * (float) Math.PI / 2.0F;
                     b.rect(x + cos(va) * rr, y - U * 0.1F + sin(va) * rr, 3.5F, 3.5F, (float) (Math.PI / 4), pc(r, 0.6F, 0.2F, 1.0F, fade * clamp01(lockIn)));
                  }
               }
            }

            float il = 0.4F * (1.0F - seg(h, 0.1F, 0.9F));
            if (il > 0.01F) {
               float hw = U * 1.6F;

               for (int j = 0; (float)j * 4.0F < U * 2.6F; j++) {
                  if ((j + tick & 1) == 0) {
                     float yy = y - U * 1.4F + (float)j * 4.0F;
                     int c = pc(r, 0.5F, 0.15F, 1.0F, il);
                     int cl = c & 16777215;
                     b.quad(x - hw, yy, cl, x, yy, c, x, yy + 1.5F, c, x - hw, yy + 1.5F, cl);
                     b.quad(x, yy, c, x + hw, yy, cl, x + hw, yy + 1.5F, cl, x, yy + 1.5F, c);
                  }
               }
            }

            b.glow(x, y, U * 1.2F * r.env(), pc(r, 0.4F, 0.2F, 1.0F, 0.6F * r.env()));
            flash(b, r, (tick & 1) == 0 ? 13169919 : 9207039, 0.3F * (1.0F - seg(h, 0.0F, 0.2F)));
         }
      });
   }

   static void psiThunder(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int T = r.sp.tier;
      int n = 1 + T;
      int tick = (int)r.T;
      boolean pierce = r.sp.type == SkillType.BYPASS;
      r.add(
         b -> {
            psiCharge(b, r, 4, 1);
            float t = r.sec * 2.4F;
            float dive = r.hit ? ein(seg(r.h, 0.0F, 0.16F)) : 0.0F;
            float orbA = r.hit ? 1.0F - seg(r.h, 0.1F, 0.2F) : eo(seg(r.u, 0.0F, 0.3F));
            if (orbA > 0.01F) {
               for (int i = 0; i < n; i++) {
                  for (int tr = 6; tr >= 0; tr--) {
                     float tt = t - (float)tr * 0.045F;
                     float ox = x + U * 1.7F * sin(tt * 3.0F + (float)i * (float) (Math.PI * 2) / (float)n) * (1.0F - dive);
                     float oy = y - U * 0.1F + U * 0.95F * sin(tt * 2.0F + (float)i * 2.1F) * (1.0F - dive);
                     float fa = orbA * (1.0F - (float)tr / 7.0F);
                     if (tr == 0) {
                        b.ring(ox, oy, 4.5F, 2.2F, pc(r, (float)i * 0.2F, 0.6F, 1.0F, fa));
                        b.glow(ox, oy, 3.5F, VfxBatch.col(16777215, 0.9F * fa));
                     } else {
                        b.rect(ox, oy, 2.5F, 2.5F, 0.0F, pc(r, (float)i * 0.2F + (float)tr * 0.05F, 0.7F, 1.0F, 0.6F * fa));
                     }
                  }
               }
            }

            if (!r.hit) {
               float a = eo(seg(r.u, 0.1F, 0.5F));

               for (int k = 0; k < 3; k++) {
                  wave(
                     b,
                     x - U * 3.0F,
                     x + U * 3.0F,
                     y - U * 0.45F + (float)k * U * 0.45F,
                     U * 0.16F * (0.5F + 0.5F * r.u),
                     2.0F + (float)k + 5.0F * r.u,
                     -r.sec * (10.0F + 4.0F * (float)k),
                     k == 0 ? 1 : (k == 1 ? 2 : 0),
                     1.2F,
                     pc(r, (float)k * 0.3F, 0.65F, 1.0F, 0.75F * a)
                  );
               }

               b.glow(x, y, U * 0.5F * r.u * ((tick & 1) == 0 ? 1.0F : 0.8F), pc(r, 0.1F, 0.3F, 1.0F, 0.5F * r.u));
            } else {
               float h = r.h;
               float env = r.env();
               float colA = SkillVfx.R.env(seg(h, 0.05F, 0.85F));
               int core = (tick & 1) == 0 ? -1 : pc(r, 0.0F, 0.3F, 1.0F, 1.0F);
               float yEnd = pierce ? r.gy + U * 0.3F : y;
               signal(b, x, -4.0F, yEnd, U * 0.3F * colA, U * 0.32F, r.T * 0.5F, 1.4F * colA, pc(r, 0.3F, 0.7F, 1.0F, 0.55F * colA), VfxBatch.mulA(core, colA));
               if (pierce) {
                  wave(b, x - U * 3.4F, x + U * 3.4F, y, U * 0.22F * colA, 7.0F, -r.T * 0.8F, 1, 1.4F, pc(r, 0.5F, 0.6F, 1.0F, 0.8F * colA));
               }

               for (int k = 0; k < 3 + T; k++) {
                  float q = seg(h, 0.04F + (float)k * 0.08F, 0.62F + (float)k * 0.08F);
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     b.polyLine(
                        x,
                        y,
                        U * (0.4F + 3.0F * eo(q)),
                        4,
                        k % 2 == 0 ? (float) (Math.PI / 4) : 0.0F,
                        U * 0.1F * (1.0F - q) + 1.0F,
                        pc(r, (float)k * 0.2F, 0.65F, 1.0F, 0.9F * (1.0F - q)),
                        1.0F
                     );
                  }
               }

               float cf = 0.55F * (1.0F - seg(h, 0.1F, 0.85F));
               if (cf > 0.01F) {
                  float cs = 8.0F;
                  float R0 = U * (1.2F + 0.25F * (float)T);
                  int m = (int)Math.ceil((double)(R0 / cs));

                  for (int i = -m; i <= m; i++) {
                     for (int j = -m; j <= m; j++) {
                        if ((i + j + tick & 1) == 0) {
                           float px = (float)i * cs;
                           float py = (float)j * cs;
                           float dd = (float)Math.sqrt((double)(px * px + py * py));
                           if (!(dd > R0)) {
                              b.rect(x + px, y + py, cs, cs, 0.0F, pc(r, dd / R0, 0.6F, 1.0F, cf * (1.0F - dd / R0)));
                           }
                        }
                     }
                  }
               }

               b.glow(x, y, U * 1.1F * env, VfxBatch.col(16777215, 0.8F * env));
               float sf = 1.0F - seg(h, 0.0F, 0.25F);
               flash(b, r, (tick & 1) == 0 ? 16777215 : 13172560, 0.32F * sf);
            }
         }
      );
   }

   static void mandala(VfxBatch b, SkillVfx.R r, float x, float y, float rad, float rot, float a, float hue) {
      if (!(a <= 0.01F) && !(rad < 1.0F)) {
         b.starLine(x, y, rad, 8, 3, rot, 1.4F, pc(r, hue, 0.65F, 1.0F, 0.9F * a), 1.0F);
         b.polyLine(x, y, rad * 0.62F, 8, -rot * 1.4F, 1.2F, pc(r, hue + 0.33F, 0.6F, 1.0F, 0.85F * a), 1.0F);
         b.polyLine(x, y, rad * 0.38F, 4, rot * 2.0F, 1.2F, pc(r, hue + 0.66F, 0.6F, 1.0F, 0.85F * a), 1.0F);

         for (int j = 0; j < 8; j++) {
            float an = rot + (float)j * (float) (Math.PI * 2) / 8.0F;
            b.rect(x + cos(an) * rad * 1.1F, y + sin(an) * rad * 1.1F, 2.4F, 2.4F, an, pc(r, hue + (float)j * 0.125F, 0.5F, 1.0F, a));
         }

         b.glow(x, y, rad * 0.35F, pc(r, hue, 0.3F, 1.0F, 0.5F * a));
      }
   }

   static void psiRockin(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float A = U * 2.6F;
      int T = r.sp.tier;
      int m = T >= 3 ? 5 : 3;
      int P = T >= 4 ? 4 : 3;
      float t0 = r.st * 0.8F;
      float len = 8.0F;
      float span = Math.max(1.0F, r.st + r.ht - t0 - len);
      r.add(
         b -> {
            float bgA = eo(seg(r.u, 0.0F, 0.4F)) * (r.hit ? 1.0F - seg(r.h, 0.6F, 1.0F) : 1.0F);
            waveBands(b, r, x, y, A * 3.0F, U * 3.4F, 16, U * (0.25F + 0.35F * r.u), 0.55F, r.sec * 7.0F, 0.0F, 0.22F * bgA, 0.0F);
            psiCharge(b, r, 8, 0);
            float fade = r.hit ? 1.0F - seg(r.h, 0.7F, 1.0F) : 1.0F;
            float pulseSum = 0.0F;

            for (int k = 0; k < P; k++) {
               pulseSum += bell(clamp01(r.win(t0 + span * (float)k / (float)(P - 1), len) * 2.0F));
            }

            for (int i = 0; i < m; i++) {
               float mx = x + ((float)i - (float)(m - 1) / 2.0F) * (2.0F * A / (float)(m - 1)) * (m >= 5 ? 1.25F : 0.9F);
               float my = y + (i % 2 == 0 ? -U * 0.2F : U * 0.25F);
               float g = eob(seg(r.u, 0.1F + 0.08F * (float)i, 0.55F + 0.06F * (float)i));
               float rad = U * (m >= 5 ? 0.55F : 0.7F) * g * (1.0F + 0.35F * pulseSum);
               mandala(b, r, mx, my, rad, r.sec * (i % 2 == 0 ? 1.6F : -1.6F), clamp01(g) * fade, (float)i * 0.2F);

               for (int k = 0; k < P; k++) {
                  float q = r.win(t0 + span * (float)k / (float)(P - 1), len);
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     float sz = k == P - 1 ? 1.3F : 1.0F;

                     for (int ring = 0; ring < 2; ring++) {
                        float rq = seg(q, (float)ring * 0.15F, 1.0F);
                        if (!(rq <= 0.0F)) {
                           b.polyLine(
                              mx,
                              my,
                              U * (0.3F + (m >= 5 ? 1.1F : 1.5F) * sz * eo(rq)),
                              8,
                              (float) (Math.PI / 8) + (float)ring * 0.2F,
                              U * 0.12F * (1.0F - rq) + 1.0F,
                              pc(r, (float)k * 0.25F + (float)ring * 0.5F + (float)i * 0.1F, 0.65F, 1.0F, 0.9F * (1.0F - rq)),
                              1.0F
                           );
                        }
                     }

                     for (int sp = 0; sp < 8; sp++) {
                        float an = (float)sp * (float) (Math.PI * 2) / 8.0F + (float)k * 0.4F;
                        float l = U * (0.6F + 1.2F * sz) * eo(q);
                        b.spark(
                           mx + cos(an) * l * 0.6F,
                           my + sin(an) * l * 0.6F,
                           an,
                           l * 0.6F,
                           U * 0.12F,
                           pc(r, (float)k * 0.25F + (float)sp * 0.125F, 0.5F, 1.0F, SkillVfx.R.env(q))
                        );
                     }
                  }
               }
            }

            for (int kx = 0; kx < P; kx++) {
               float q = r.win(t0 + span * (float)kx / (float)(P - 1), len);
               if (q > 0.0F && q < 0.35F) {
                  flash(b, r, pc(r, (float)kx * 0.25F, 0.6F, 1.0F, 1.0F) & 16777215, 0.24F * (1.0F - q / 0.35F));
               }
            }

            if (r.hit) {
               float tear = eo(seg(r.h, 0.55F, 1.0F)) * U * 2.5F;
               if (tear > 0.0F) {
                  waveBands(b, r, x, y, A * 3.0F, U * 3.4F, 16, U * 0.5F, 0.55F, r.sec * 14.0F, tear, 0.3F * (1.0F - seg(r.h, 0.6F, 1.0F)), 0.5F);
               }
            }
         }
      );
   }

   static void flurry(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int n = r.sp.hits;
      float t0 = r.st * 0.4F;
      float len = 7.0F;
      float span = Math.max(1.0F, r.st + r.ht - t0 - len * 0.8F);
      r.add(b -> {
         if (r.u < 0.5F) {
            float q = seg(r.u, 0.0F, 0.5F);
            float g = 1.0F - seg(r.u, 0.35F, 0.5F);
            converge(b, r, 14, x, y, U * 2.0F, q, r.sp.seed + 51);
            b.glow(x, y, U * 0.6F * seg(r.u, 0.0F, 0.4F) * g, VfxBatch.col(p.outer(), 0.3F * g));
         }

         for (int k = 0; k < n; k++) {
            float q = r.win(t0 + span * (float)k / (float)n, len);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float ang = (k % 2 == 0 ? -0.8F : 0.75F) + (rnd(r.sp.seed + k) - 0.5F) * 0.6F;
               float ox = (rnd(r.sp.seed + k * 3) - 0.5F) * U * 0.9F;
               float oy = (rnd(r.sp.seed + k * 5) - 0.5F) * U * 0.8F;
               hitMark(b, r, x + ox, y + oy, ang, q, k == n - 1 ? 1.35F : 1.0F, k);
            }
         }

         if (r.hit) {
            b.glow(x, y, U * 1.4F * (1.0F - r.h), VfxBatch.col(p.mid(), 0.3F * (1.0F - r.h)));
         }
      });
   }

   static void hitMark(VfxBatch b, SkillVfx.R r, float px, float py, float ang, float q, float sz, int k) {
      SkillVfx.Pal p = r.p;
      float U = r.U * 1.25F * sz;
      float dx = cos(ang);
      float dy = sin(ang);
      float nx = -dy;
      float ny = dx;
      float e = SkillVfx.R.env(q);
      switch (r.th) {
         case ELECTRIC:
            b.bolt(
               px - dx * U * 1.5F,
               py - dy * U * 1.5F,
               px + dx * U * 1.5F,
               py + dy * U * 1.5F,
               (int)(r.T * 1.5F) + k * 37,
               U * 0.35F,
               1.2F * e,
               VfxBatch.col(p.outer(), 0.7F * e),
               VfxBatch.col(p.core(), e),
               1
            );
            b.glow(px, py, U * 0.7F * e, VfxBatch.col(p.mid(), 0.6F * e));
            break;
         case DARK:
            float r0 = U * 1.6F;
            float mid = (float)Math.atan2((double)dx, (double)nx);
            float span = 1.2F;
            float a0 = mid - span / 2.0F;
            float L = eo(seg(q, 0.0F, 0.35F));
            float fade = 1.0F - seg(q, 0.35F, 1.0F);
            float aL = a0 + span * L;
            float mA = (a0 + aL) / 2.0F;
            float sA = Math.max(0.01F, aL - a0);

            for (int c = -1; c <= 1; c++) {
               float ccx = px - nx * r0 * 0.9F + nx * (float)c * U * 0.38F;
               float ccy = py - ny * r0 * 0.9F + ny * (float)c * U * 0.38F;
               b.crescent(ccx, ccy, r0, U * 0.35F * fade, mA, sA, VfxBatch.col(p.outer(), 0.0F), VfxBatch.col(p.mid(), 0.85F * fade));
               b.crescent(ccx, ccy, r0, U * 0.1F * fade, mA, sA, VfxBatch.col(p.accent(), 0.0F), VfxBatch.col(p.accent(), fade));
            }
            break;
         case IMPACT:
            for (int s = 0; s < 8; s++) {
               float a = (float)s * (float) (Math.PI * 2) / 8.0F + (float)k;
               float l = U * (0.8F + 0.6F * (float)(s % 2)) * eo(q);
               b.spark(px + cos(a) * l * 0.5F, py + sin(a) * l * 0.5F, a, l, U * 0.18F, VfxBatch.col(p.mid(), e));
            }

            b.ring(px, py, U * (0.2F + 0.9F * eo(q)), U * 0.18F * (1.0F - q), VfxBatch.col(p.core(), 0.8F * (1.0F - q)));
            b.glow(px, py, U * 0.6F * e, VfxBatch.col(p.core(), 0.8F * e));
            break;
         default:
            float r0 = U * 1.5F;
            float ccx = px - nx * r0 * 0.9F;
            float ccy = py - dx * r0 * 0.9F;
            float mid = (float)Math.atan2((double)dx, (double)nx);
            float span = 1.5F;
            float a0 = mid - span / 2.0F;
            float L = eo(seg(q, 0.0F, 0.35F));
            float fade = 1.0F - seg(q, 0.35F, 1.0F);
            float aL = a0 + span * L;
            float mA = (a0 + aL) / 2.0F;
            float sA = Math.max(0.01F, aL - a0);
            int cOut = r.th == SkillVfx.Theme.PSI ? VfxBatch.hsv(r.sec * 0.5F + (float)k * 0.2F, 0.6F, 1.0F, 1.0F) : 0xFF000000 | p.outer();
            float thick = (r.th == SkillVfx.Theme.LUNAR ? 0.55F : 0.4F) * U;
            b.crescent(ccx, ccy, r0, thick * 1.8F * fade, mA, sA, VfxBatch.mulA(cOut, 0.0F), VfxBatch.mulA(cOut, 0.5F * fade));
            b.crescent(ccx, ccy, r0, thick * fade, mA, sA, VfxBatch.col(p.mid(), 0.0F), VfxBatch.col(p.mid(), 0.9F * fade));
            b.crescent(ccx, ccy, r0, thick * 0.35F * fade, mA, sA, VfxBatch.col(p.core(), 0.3F * fade), VfxBatch.col(p.core(), fade));
            if (r.th == SkillVfx.Theme.FIRE) {
               for (int f = 0; f < 4; f++) {
                  float fa = a0 + span * L * ((float)f + 0.5F) / 4.0F;
                  float fx = ccx + cos(fa) * r0;
                  float fy = ccy + sin(fa) * r0;
                  b.flame(
                     fx,
                     fy,
                     U * 0.25F,
                     U * 0.7F * fade,
                     sin(r.sec * 10.0F + (float)f) * 2.0F,
                     VfxBatch.col(p.mid(), fade),
                     VfxBatch.col(p.outer(), 0.3F * fade)
                  );
               }
            }

            float st = 1.0F - seg(q, 0.0F, 0.45F);
            b.star(px, py, U * 0.6F * st, 4, ang, VfxBatch.col(p.core(), st));
      }

      burst(b, r, 8, px, py, seg(q, 0.15F, 1.0F), U * 1.4F, U * 0.6F, 1.3F, r.sp.seed + k * 41, 0.0F);
   }

   static void rain(VfxBatch b, SkillVfx.R r, int m, float A, float fall, float boom, float fromX, float fromY, SkillVfx.Drop head, SkillVfx.Drop impact) {
      float t0 = r.st * 0.3F;
      float span = Math.max(1.0F, (r.st + r.ht - t0 - fall - boom * 0.75F) * (float)m / Math.max(1.0F, (float)m - 1.0F));
      float len = (float)Math.sqrt((double)(fromX * fromX + fromY * fromY));
      float dx = -fromX / len;
      float dy = -fromY / len;

      for (int k = 0; k < m; k++) {
         float tk = t0 + span * (float)k / (float)m;
         float ix = r.cx + (rnd(r.sp.seed + k * 7) - 0.5F) * 2.0F * A;
         float iy = r.gy + (rnd(r.sp.seed + k * 11) - 0.5F) * r.U * 0.6F;
         float qf = r.win(tk, fall);
         if (head != null && qf > 0.0F && qf < 1.0F) {
            float e = qf * qf;
            head.draw(b, mix(ix + fromX, ix, e), mix(iy + fromY, iy, e), dx, dy, qf, k);
         }

         float qb = r.win(tk + fall, boom);
         if (impact != null && qb > 0.0F && qb < 1.0F) {
            impact.draw(b, ix, iy, dx, dy, qb, k);
         }
      }
   }

   static void barrage(SkillVfx.R r) {
      switch (r.th) {
         case FIRE:
            meteorRain(r);
            break;
         case ICE:
            icicleHail(r);
            break;
         case ELECTRIC:
            thunderstorm(r);
            break;
         case PSI:
         case LIGHT:
         case DARK:
         default:
            nova(r);
            break;
         case SLASH:
            bladeStorm(r);
            break;
         case IMPACT:
            quake(r);
      }
   }

   static void meteorRain(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float gy = r.gy;
      float U = r.U;
      float A = U * 2.6F;
      float sa = eob(seg(r.u, 0.0F, 0.3F)) * (r.hit ? 1.0F - seg(r.h, 0.4F, 1.0F) : 1.0F);
      r.alpha(
         b -> rain(
               b,
               r,
               6,
               A,
               7.0F,
               12.0F,
               U * 3.2F,
               -U * 7.0F,
               null,
               (bb, ix, iy, dx, dy, q, k) -> puff(
                     bb, ix + sin(r.sec + (float)k) * 3.0F, iy - U * 0.4F - q * U * 1.4F, U * (0.4F + 0.6F * q), p.dark(), 0.45F * bell(q)
                  )
            )
      );
      r.add(
         b -> {
            sigil(b, r, x, gy, A * 0.9F * sa, sa * 0.8F, r.sec, 6);
            rain(
               b,
               r,
               6,
               A,
               7.0F,
               12.0F,
               U * 3.2F,
               -U * 7.0F,
               (bb, hx, hy, dx, dy, q, k) -> {
                  float tx = hx - dx * U * 2.4F;
                  float ty = hy - dy * U * 2.4F;
                  bb.softTaper(tx, ty, hx, hy, 0.0F, U * 1.1F, VfxBatch.col(p.outer(), 0.0F), VfxBatch.col(p.outer(), 0.75F));
                  bb.softTaper(tx, ty, hx, hy, 0.0F, U * 0.5F, VfxBatch.col(p.mid(), 0.0F), VfxBatch.col(p.core(), 0.95F));
                  bb.glow(hx, hy, U * 0.9F, VfxBatch.col(p.mid(), 0.75F));
                  bb.glow(hx, hy, U * 0.35F, VfxBatch.col(p.core(), 1.0F));

                  for (int e = 0; e < 4; e++) {
                     float f = (float)(e + 1) / 5.0F;
                     mat(bb, r, mix(hx, tx, f) + (rnd(k * 9 + e + (int)r.T) - 0.5F) * 4.0F, mix(hy, ty, f), 1.2F, 0.0F, 1.0F - f, k * 5 + e);
                  }
               },
               (bb, ix, iy, dx, dy, q, k) -> {
                  float fb = 1.0F - q;
                  float en = SkillVfx.R.env(q);
                  bb.glow(ix, iy - U * 0.3F, U * (0.6F + 1.1F * eo(q)), VfxBatch.col(p.mid(), 0.7F * fb));
                  bb.glow(ix, iy - U * 0.3F, U * 0.55F * fb, VfxBatch.col(p.core(), fb));
                  float rr = U * (0.3F + 1.5F * eo(q));
                  bb.ringArc(ix, iy, rr, rr * 0.32F, U * 0.3F * fb, VfxBatch.col(p.mid(), 0.8F * fb), 0.0F, (float) (Math.PI * 2));

                  for (int f = -1; f <= 1; f++) {
                     bb.flame(
                        ix + (float)f * U * 0.3F,
                        iy,
                        U * 0.4F,
                        U * (1.5F - 0.5F * (float)Math.abs(f)) * en,
                        sin(r.sec * 9.0F + (float)f) * 3.0F,
                        VfxBatch.col(p.mid(), 0.9F * en),
                        VfxBatch.col(p.outer(), 0.3F * en)
                     );
                  }

                  burst(bb, r, 10, ix, iy - U * 0.3F, q, U * 1.6F, -U * 0.6F, 1.4F, r.sp.seed + k * 53, 0.6F);
               }
            );
            if (r.hit) {
               flash(b, r, p.mid(), 0.15F * (1.0F - seg(r.h, 0.0F, 0.2F)));
            }
         }
      );
   }

   static void icicleHail(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float A = U * 2.6F;
      float storm = seg(r.u, 0.0F, 0.4F) * (r.hit ? 1.0F - seg(r.h, 0.6F, 1.0F) : 1.0F);
      r.alpha(b -> {
         b.ellipse(x, gy, A * 1.2F * storm, A * 1.2F * 0.3F * storm, 0.0F, VfxBatch.col(p.mid(), 0.3F * storm), VfxBatch.col(p.outer(), 0.0F));

         for (int i = 0; i < 26; i++) {
            float f = (r.sec * 1.6F + rnd(r.sp.seed + i)) % 1.0F;
            float px = x - A * 1.6F + f * A * 3.2F;
            float py = y - U * 2.2F + rnd(r.sp.seed + i * 3) * U * 3.0F + f * U * 0.8F;
            puff(b, px, py, U * 0.09F, 16777215, 0.8F * bell(f) * storm);
         }
      });
      r.add(b -> {
         for (int i = 0; i < 40; i++) {
            float f = (r.sec * 2.2F + rnd(r.sp.seed + i * 13)) % 1.0F;
            float px = x - A * 1.7F + f * A * 3.4F;
            float py = y - U * 2.4F + rnd(r.sp.seed + i * 17) * U * 3.2F + f * U;
            b.soft(px - U * 0.5F, py - U * 0.16F, px, py, 0.9F, VfxBatch.col(p.mid(), 0.0F), VfxBatch.col(p.core(), 0.55F * bell(f) * storm));
         }

         rain(b, r, 7, A, 6.0F, 11.0F, U * 1.2F, -U * 7.5F, (bb, hx, hy, dx, dy, q, k) -> {
            float ang = (float)Math.atan2((double)dy, (double)dx);
            bb.soft(hx - dx * U * 1.6F, hy - dy * U * 1.6F, hx, hy, U * 0.3F, VfxBatch.col(p.outer(), 0.0F), VfxBatch.col(p.mid(), 0.6F));
            bb.shard(hx - dx * U * 0.4F, hy - dy * U * 0.4F, U * 1.1F, U * 0.32F, ang, VfxBatch.col(p.core(), 0.95F), VfxBatch.col(p.outer(), 0.85F));
         }, (bb, ix, iy, dx, dy, q, k) -> {
            float fb = 1.0F - q;
            bb.glow(ix, iy - U * 0.2F, U * (0.5F + 0.8F * eo(q)), VfxBatch.col(p.mid(), 0.6F * fb));
            float rr = U * (0.3F + 1.2F * eo(q));
            bb.ringArc(ix, iy, rr, rr * 0.32F, 2.0F, VfxBatch.col(p.core(), 0.8F * fb), 0.0F, (float) (Math.PI * 2));
            burst(bb, r, 9, ix, iy - U * 0.2F, q, U * 1.5F, U * 1.4F, 1.6F, r.sp.seed + k * 61, 0.5F);
         });
         if (r.hit) {
            float fh = 1.0F - r.h;
            b.glow(x, y, U * (1.6F + 1.6F * eo(r.h)), VfxBatch.col(p.mid(), 0.35F * fh));

            for (int k = 0; k < 6; k++) {
               float a = r.sec * 0.6F + (float)k * (float) (Math.PI * 2) / 6.0F;
               snowflake(b, x + cos(a) * A * 0.7F, y - U * 0.5F + sin(a) * U * 0.6F, U * 0.35F * fh, r.sec + (float)k, VfxBatch.col(p.core(), 0.7F * fh), 1.0F);
            }

            flash(b, r, p.mid(), 0.18F * (1.0F - seg(r.h, 0.0F, 0.2F)));
         }
      });
   }

   static void thunderstorm(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float A = U * 2.6F;
      r.alpha(b -> {
         float c = seg(r.u, 0.0F, 0.4F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);

         for (int i = 0; i < 9; i++) {
            float cx0 = x + (float)(i - 4) * A * 0.38F + sin(r.sec + (float)i) * 4.0F;
            puff(b, cx0, 10.0F + (float)(i % 2) * 8.0F, U * (1.1F + 0.4F * rnd(i)), p.dark(), 0.7F * c);
         }
      });
      r.add(
         b -> {
            float c = seg(r.u, 0.0F, 0.4F);
            if (!r.hit && (int)(r.T * 1.3F) % 4 == 0) {
               b.glow(x + (rnd((int)r.T) - 0.5F) * A * 2.0F, 14.0F, U * 1.4F, VfxBatch.col(p.outer(), 0.4F * c));
            }

            rain(
               b,
               r,
               6,
               A,
               2.0F,
               7.0F,
               0.0F,
               -r.sh,
               null,
               (bb, ix, iy, dx, dy, q, k) -> {
                  float en = SkillVfx.R.env(q);
                  int fl = (int)(r.T * 1.2F) + k * 53;
                  bb.bolt(
                     ix + (rnd(fl) - 0.5F) * U,
                     6.0F,
                     ix,
                     iy - U * 0.4F,
                     fl,
                     U * 1.2F,
                     1.8F * en,
                     VfxBatch.col(p.outer(), 0.75F * en),
                     VfxBatch.col(p.core(), en),
                     2
                  );
                  bb.glow(ix, iy - U * 0.4F, U * 1.1F * en, VfxBatch.col(p.mid(), 0.6F * en));
                  float rr = U * (0.3F + 1.2F * eo(q));
                  bb.ringArc(ix, iy, rr, rr * 0.32F, 2.0F, VfxBatch.col(p.mid(), 0.8F * (1.0F - q)), 0.0F, (float) (Math.PI * 2));
                  burst(bb, r, 8, ix, iy - U * 0.4F, q, U * 1.4F, U * 0.8F, 1.3F, r.sp.seed + k * 71, 0.0F);
               }
            );
            if (r.hit) {
               float ch = SkillVfx.R.env(seg(r.h, 0.0F, 0.7F));
               int fl = (int)(r.T * 1.5F);

               for (int k = 0; k < 5; k++) {
                  float x0 = x + (rnd(r.sp.seed + k * 7) - 0.5F) * 2.0F * A;
                  float y0 = gy - U * 0.4F;
                  float x1 = x + (rnd(r.sp.seed + (k + 1) * 7) - 0.5F) * 2.0F * A;
                  boltArc(b, x0, y0, x1, y0, fl * 7 + k, U * 0.5F, ch, p);
               }

               flash(b, r, 16777215, 0.3F * (1.0F - seg(r.h, 0.0F, 0.12F)));
            }
         }
      );
   }

   private static void boltArc(VfxBatch b, float x0, float y0, float x1, float y1, int seed, float jag, float a, SkillVfx.Pal p) {
      if (!(a <= 0.0F)) {
         b.bolt(x0, y0, x1, y1, seed, jag, 0.9F * a, VfxBatch.col(p.outer(), 0.6F * a), VfxBatch.col(p.core(), a), 1);
      }
   }

   static void quake(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float A = U * 2.6F;
      r.alpha(
         b -> {
            rain(
               b,
               r,
               6,
               A,
               1.0F,
               13.0F,
               0.0F,
               -1.0F,
               null,
               (bb, ix, iy, dx, dy, q, k) -> {
                  float up = eob(seg(q, 0.0F, 0.25F)) * (1.0F - seg(q, 0.7F, 1.0F));

                  for (int s = -1; s <= 1; s++) {
                     float hgt = U * (1.6F - 0.5F * (float)Math.abs(s)) * up;
                     bb.spike(
                        ix + (float)s * U * 0.28F,
                        iy + 2.0F,
                        hgt,
                        U * 0.42F,
                        (float) (-Math.PI / 2) + (float)s * 0.25F,
                        VfxBatch.col(VfxBatch.lerp(0xFF000000 | p.dark(), -1, 0.3F), 1.0F),
                        VfxBatch.col(p.dark(), 1.0F)
                     );
                  }

                  for (int d = 0; d < 6; d++) {
                     float ang = (float) (-Math.PI / 2) + (rnd(k * 13 + d) - 0.5F) * 2.2F;
                     float v = U * (1.6F + 1.2F * rnd(k + d * 5));
                     rock(
                        bb,
                        r,
                        ix + cos(ang) * v * q,
                        iy - U * 0.4F + sin(ang) * v * q + U * 3.5F * q * q,
                        U * (0.08F + 0.08F * rnd(d)),
                        r.sec * 8.0F + (float)d,
                        1.0F - seg(q, 0.7F, 1.0F)
                     );
                  }

                  puff(bb, ix, iy - q * U * 0.5F, U * (0.4F + 0.8F * q), p.accent(), 0.5F * bell(q));
               }
            );
            if (r.hit) {
               float ca = 1.0F - seg(r.h, 0.6F, 1.0F);

               for (int k = 0; k < 7; k++) {
                  float x0 = x + (rnd(r.sp.seed + k * 3) - 0.5F) * A * 2.0F;
                  float x1 = x0 + (rnd(k + 9) - 0.5F) * A;
                  float yy = gy + (rnd(k * 5) - 0.5F) * U * 0.4F;
                  float mxk = (x0 + x1) / 2.0F;
                  float myk = yy + (rnd(k * 7) - 0.5F) * 5.0F;
                  float g = eo(seg(r.h, 0.0F, 0.2F));
                  b.taper(x0, yy, mix(x0, mxk, g), mix(yy, myk, g), 1.8F, 1.2F, VfxBatch.col(1313800, 0.8F * ca), VfxBatch.col(1313800, 0.7F * ca));
                  b.taper(mxk, myk, mix(mxk, x1, g), mix(myk, yy, g), 1.2F, 0.2F, VfxBatch.col(1313800, 0.7F * ca), VfxBatch.col(1313800, 0.0F));
               }
            }
         }
      );
      r.add(b -> {
         rain(b, r, 6, A, 1.0F, 13.0F, 0.0F, -1.0F, null, (bb, ix, iy, dx, dy, q, k) -> {
            float fb = 1.0F - q;
            bb.glow(ix, iy - U * 0.3F, U * (0.4F + 0.8F * eo(q)), VfxBatch.col(p.mid(), 0.5F * fb));
            float rr = U * (0.3F + 1.3F * eo(q));
            bb.ringArc(ix, iy, rr, rr * 0.32F, U * 0.3F * fb, VfxBatch.col(p.mid(), 0.7F * fb), 0.0F, (float) (Math.PI * 2));
            burst(bb, r, 6, ix, iy - U * 0.3F, q, U * 1.2F, U * 0.9F, 1.2F, r.sp.seed + k * 83, 0.6F);
         });
         if (r.hit) {
            float fh = 1.0F - r.h;

            for (int k = 0; k < 2; k++) {
               float q = seg(r.h, (float)k * 0.1F, 1.0F);
               float rr = A * (0.3F + 1.2F * eo(q));
               b.ringArc(x, gy, rr, rr * 0.3F, U * 0.6F * (1.0F - q), VfxBatch.col(p.mid(), 0.85F * (1.0F - q)), 0.0F, (float) (Math.PI * 2));
            }

            b.glow(x, y, A * 0.9F * fh, VfxBatch.col(p.outer(), 0.35F * fh));
            flash(b, r, p.core(), 0.18F * (1.0F - seg(r.h, 0.0F, 0.15F)));
         }
      });
   }

   static void bladeStorm(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float A = U * 2.6F;
      int n = 9;
      float t0 = r.st * 0.3F;
      float len = 7.0F;
      float span = Math.max(1.0F, r.st + r.ht - t0 - len);
      r.add(b -> {
         if (r.u < 0.4F) {
            converge(b, r, 14, x, y, A, seg(r.u, 0.0F, 0.4F), r.sp.seed + 91);
         }

         for (int k = 0; k < n; k++) {
            float q = r.win(t0 + span * (float)k / (float)n, len);
            if (!(q <= 0.0F) && !(q >= 1.0F)) {
               float px = x + (rnd(r.sp.seed + k * 7) - 0.5F) * 2.0F * A;
               float py = y + (rnd(r.sp.seed + k * 11) - 0.5F) * U * 1.2F;
               float ang = (k % 2 == 0 ? -0.75F : 0.8F) + (rnd(k * 3) - 0.5F) * 0.5F;
               hitMark(b, r, px, py, ang, q, 1.1F, k);
            }
         }

         if (r.hit) {
            float cut = 1.0F - seg(r.h, 0.0F, 0.4F);
            b.soft(0.0F, y - U * 0.4F, r.sw, y + U * 0.4F, 1.4F, VfxBatch.col(r.p.core(), 0.6F * cut), VfxBatch.col(r.p.core(), 0.6F * cut));
            flash(b, r, 16777215, 0.15F * (1.0F - seg(r.h, 0.0F, 0.15F)));
         }
      });
   }

   static void nova(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float A = U * 2.6F;
      boolean psi = r.th == SkillVfx.Theme.PSI;
      if (r.th == SkillVfx.Theme.DARK) {
         r.alpha(b -> {
            float g = r.hit ? 1.0F - seg(r.h, 0.3F, 1.0F) : eo(seg(r.u, 0.2F, 1.0F));
            float rr = r.hit ? A * (0.8F + 0.9F * eo(r.h)) : A * 0.8F * g;
            b.ellipse(x, y, rr, rr * 0.75F, 0.0F, VfxBatch.col(p.dark(), 0.75F * g), VfxBatch.col(p.dark(), 0.0F));
         });
      }

      r.add(
         b -> {
            float sa = eob(seg(r.u, 0.0F, 0.3F)) * (r.hit ? 1.0F - seg(r.h, 0.3F, 0.8F) : 1.0F);
            if (psi) {
               for (int k = 0; k < 3; k++) {
                  b.ringArc(
                     x,
                     gy,
                     A * (0.5F + 0.25F * (float)k) * sa,
                     A * (0.5F + 0.25F * (float)k) * 0.32F * sa,
                     2.2F,
                     VfxBatch.hsv(r.sec * 0.5F + (float)k * 0.3F, 0.6F, 1.0F, 0.8F * sa),
                     0.0F,
                     (float) (Math.PI * 2)
                  );
               }

               b.polyLine(x, gy, A * 0.8F * sa, 6, r.sec * 1.5F, 1.2F, VfxBatch.hsv(r.sec * 0.5F + 0.5F, 0.5F, 1.0F, 0.7F * sa), 0.32F);
               b.polyLine(
                  x, gy, A * 0.55F * sa, 6, -r.sec * 1.5F + (float) (Math.PI / 6), 1.0F, VfxBatch.hsv(r.sec * 0.5F + 0.75F, 0.5F, 1.0F, 0.6F * sa), 0.32F
               );
            } else {
               sigil(b, r, x, gy, A * 0.95F * sa, sa, r.sec * 0.8F, r.th == SkillVfx.Theme.LUNAR ? 7 : 8);
            }

            if (!r.hit) {
               converge(b, r, 26, x, y, A * 1.4F, r.u, r.sp.seed + 61);

               for (int k = 0; k < 3; k++) {
                  float q = (r.u * 3.0F + (float)k / 3.0F) % 1.0F;
                  float rr = A * (1.2F - q);
                  int c = psi ? VfxBatch.hsv(r.sec * 0.6F + (float)k * 0.33F, 0.55F, 1.0F, 0.5F * q * r.u) : VfxBatch.col(p.mid(), 0.5F * q * r.u);
                  b.ring(x, y, rr, 2.4F, c);
               }

               if (r.th == SkillVfx.Theme.LUNAR) {
                  float my = y - U * 2.6F;
                  float a = eob(seg(r.u, 0.2F, 0.7F));
                  b.glow(x, my, U * 2.0F * a, VfxBatch.col(p.outer(), 0.4F * a));
                  b.crescent(
                     x, my, U * 1.1F, U * 0.7F * a, (float) (Math.PI * 4.0 / 5.0), 2.6F * a + 0.01F, VfxBatch.col(p.mid(), 0.6F * a), VfxBatch.col(p.core(), a)
                  );
               }

               b.glow(x, y, U * (0.4F + 1.2F * r.u), VfxBatch.col(p.outer(), 0.3F * r.u));
            } else {
               float fh = 1.0F - r.h;

               for (int k = 0; k < 4; k++) {
                  float q = seg(r.h, (float)k * 0.08F, 0.75F + (float)k * 0.06F);
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     float rr = A * (0.3F + 1.4F * eo(q));
                     int c = psi
                        ? VfxBatch.hsv(r.sec * 0.6F + (float)k * 0.2F, 0.6F, 1.0F, 0.85F * (1.0F - q))
                        : VfxBatch.col(k % 2 == 0 ? p.mid() : p.core(), 0.85F * (1.0F - q));
                     b.ring(x, y, rr, U * 0.4F * (1.0F - q), c);
                     b.ringArc(x, gy, rr, rr * 0.3F, U * 0.3F * (1.0F - q), c, 0.0F, (float) (Math.PI * 2));
                  }
               }

               int rays = r.th == SkillVfx.Theme.LIGHT ? 16 : 10;
               float env = r.env();

               for (int kx = 0; kx < rays; kx++) {
                  float ang = (float)kx * (float) (Math.PI * 2) / (float)rays + r.sec * 0.4F;
                  float l = A * (0.9F + 0.6F * (float)(kx % 2)) * env;
                  int c = psi ? VfxBatch.hsv((float)kx / (float)rays + r.sec * 0.3F, 0.5F, 1.0F, 0.7F * env) : VfxBatch.col(p.mid(), 0.7F * env);
                  b.spark(x + cos(ang) * l * 0.5F, y + sin(ang) * l * 0.5F, ang, l, U * 0.25F, c);
               }

               b.glow(x, y, A * (0.6F + 0.6F * eo(r.h)), VfxBatch.col(p.outer(), 0.45F * fh));
               b.glow(x, y, U * 1.2F * fh, VfxBatch.col(p.core(), 0.9F * fh));
               burst(b, r, 34, x, y, r.h, A * 1.3F, U * 0.8F, 2.0F, r.sp.seed + 121, 0.0F);
               if (r.th == SkillVfx.Theme.LIGHT || r.th == SkillVfx.Theme.LUNAR) {
                  for (int i = 0; i < 18; i++) {
                     float f = seg(r.h, rnd(i) * 0.4F, 0.6F + rnd(i) * 0.4F);
                     mat(b, r, x + (rnd(r.sp.seed + i * 3) - 0.5F) * A * 2.4F, y - U * 2.0F + f * U * 3.0F, 1.5F, (float) (Math.PI / 2), bell(f), i);
                  }
               }

               flash(b, r, psi ? 16777215 : p.mid(), 0.22F * (1.0F - seg(r.h, 0.0F, 0.18F)));
            }
         }
      );
   }

   static void snowgrave(SkillVfx.R r) {
      SkillVfx.Pal p = palette(SkillVfx.Theme.ICE, 0);
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float beamQ = seg(r.u, 0.3F, 1.0F);
      float base = r.sh + 10.0F;
      float top = mix(base, -10.0F, eo(beamQ));
      float fade = r.hit ? 1.0F - seg(r.h, 0.3F, 1.0F) : 1.0F;
      float frostQ = seg(r.u, 0.0F, 0.5F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
      r.alpha(
         b -> {
            float fh = r.sh * 0.22F * eo(frostQ);
            b.quad(
               0.0F,
               r.sh - fh,
               VfxBatch.col(p.mid(), 0.0F),
               r.sw,
               r.sh - fh,
               VfxBatch.col(p.mid(), 0.0F),
               r.sw,
               r.sh,
               VfxBatch.col(p.mid(), 0.55F * frostQ),
               0.0F,
               r.sh,
               VfxBatch.col(p.mid(), 0.55F * frostQ)
            );

            for (int i = 0; i < 18; i++) {
               float bx = ((float)i + 0.5F) / 18.0F * r.sw;
               float g = eob(seg(r.u, rnd(i) * 0.4F, 0.4F + rnd(i) * 0.4F)) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
               if (g > 0.0F) {
                  b.spike(
                     bx,
                     r.sh + 2.0F,
                     U * (0.6F + 0.9F * rnd(i + 5)) * g,
                     U * 0.28F,
                     (float) (-Math.PI / 2) + (rnd(i + 9) - 0.5F) * 0.6F,
                     VfxBatch.col(p.core(), 0.85F * g),
                     VfxBatch.col(p.outer(), 0.8F * g)
                  );
               }
            }

            if (r.hit) {
               b.screen(r.sw, r.sh, VfxBatch.col(16055551, 0.85F * (1.0F - seg(r.h, 0.0F, 0.7F))));
            }
         }
      );
      r.add(
         b -> {
            float sa = seg(r.u, 0.0F, 0.2F) * fade;

            for (int i = 0; i < 10; i++) {
               float ang = r.sec * 1.8F + (float)i * (float) (Math.PI * 2) / 10.0F;
               float rr = U * (2.6F - 1.2F * seg(r.u, 0.0F, 0.8F)) * (0.8F + 0.2F * rnd(i));
               snowflake(
                  b, x + cos(ang) * rr, y + sin(ang) * rr * 0.45F - U * 0.3F, U * 0.22F, r.sec * 2.0F + (float)i, VfxBatch.col(p.core(), 0.8F * sa), 1.0F
               );
            }

            float g0 = seg(r.u, 0.0F, 0.4F);
            b.ellipse(x, gy, U * 2.0F * g0, U * 0.6F * g0, 0.0F, VfxBatch.col(p.mid(), 0.4F * fade), VfxBatch.col(p.outer(), 0.0F));
            if (beamQ > 0.0F) {
               float w = U * (1.1F + 0.15F * sin(r.sec * 6.0F)) * fade;
               b.soft(x, base, x, top, w * 3.2F, VfxBatch.col(p.outer(), 0.5F * fade), VfxBatch.col(p.outer(), 0.3F * fade));
               b.soft(x, base, x, top, w * 1.6F, VfxBatch.col(p.mid(), 0.85F * fade), VfxBatch.col(p.mid(), 0.6F * fade));
               b.soft(x, base, x, top, w * 0.6F, VfxBatch.col(p.core(), fade), VfxBatch.col(p.core(), 0.9F * fade));

               for (int i = 0; i < 28; i++) {
                  float f = (r.sec * 0.9F + (float)i / 28.0F) % 1.0F;
                  float py = mix(base, -10.0F, f);
                  if (!(py < top)) {
                     float ang = f * (float) (Math.PI * 2) * 3.0F + (float)(i % 2) * (float) Math.PI;
                     mat(b, r, x + cos(ang) * w * 1.1F, py, 1.4F + 0.6F * sin(ang), ang, 0.9F * fade, i);
                  }
               }

               for (int k = 0; k < 6; k++) {
                  float bt = (float)k / 6.0F;
                  float q = (beamQ - bt) * 3.0F;
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     float by = mix(base, -10.0F, eo(bt));
                     float l = U * 1.8F * bell(q);
                     b.shard(
                        x - w - l / 2.0F,
                        by,
                        l,
                        U * 0.25F,
                        (float) Math.PI,
                        VfxBatch.col(p.core(), 0.9F * (1.0F - q)),
                        VfxBatch.col(p.outer(), 0.7F * (1.0F - q))
                     );
                     b.shard(x + w + l / 2.0F, by, l, U * 0.25F, 0.0F, VfxBatch.col(p.core(), 0.9F * (1.0F - q)), VfxBatch.col(p.outer(), 0.7F * (1.0F - q)));
                  }
               }

               float cr = seg(beamQ, 0.8F, 1.0F);
               if (cr > 0.0F) {
                  float cyy = Math.max(26.0F, top + 26.0F);
                  snowflake(b, x, cyy, U * 1.2F * eob(cr), r.sec * 0.8F, VfxBatch.col(p.core(), cr * fade), 2.0F);
                  b.ring(x, cyy, U * 1.45F * cr, 2.5F, VfxBatch.col(p.mid(), 0.7F * cr * fade));
                  b.glow(x, cyy, U * 1.8F * cr, VfxBatch.col(p.outer(), 0.35F * cr * fade));
               }
            }

            if (r.hit) {
               float q = r.h;

               for (int ix = 0; ix < 24; ix++) {
                  float f = seg(q, rnd(ix) * 0.3F, 1.0F);
                  float px = rnd(r.sp.seed + ix * 7) * r.sw + sin(r.sec * 2.0F + (float)ix) * 8.0F;
                  float py = rnd(ix * 13) * r.sh * 0.6F + f * r.sh * 0.4F;
                  snowflake(b, px, py, U * (0.15F + 0.15F * rnd(ix)), r.sec + (float)ix, VfxBatch.col(16777215, bell(f) * 0.9F), 0.8F);
               }

               b.glow(x, y, U * 3.0F * (1.0F - q), VfxBatch.col(p.mid(), 0.5F * (1.0F - q)));
            }
         }
      );
   }

   static void starstorm(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float A = U * 3.4F;
      r.add(b -> {
         float sky = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);

         for (int i = 0; i < 30; i++) {
            float px = rnd(r.sp.seed + i) * r.sw;
            float py = rnd(r.sp.seed + i * 3) * r.sh * 0.45F;
            float tw = (float)Math.pow((double)Math.max(0.0F, sin(r.sec * (2.0F + rnd(i) * 3.0F) + (float)i)), 4.0);
            b.star(px, py, 1.0F + 2.0F * tw, 4, 0.0F, VfxBatch.hsv(rnd(i), 0.3F, 1.0F, (0.3F + 0.7F * tw) * sky));
         }

         rain(b, r, 12, A, 6.0F, 10.0F, -U * 4.0F, -U * 8.0F, (bb, hx, hy, dx, dy, q, k) -> {
            int hc = VfxBatch.hsv(rnd(k) + r.sec * 0.3F, 0.45F, 1.0F, 1.0F);
            float tx = hx - dx * U * 3.0F;
            float ty = hy - dy * U * 3.0F;
            bb.softTaper(tx, ty, hx, hy, 0.0F, U * 0.8F, VfxBatch.mulA(hc, 0.0F), VfxBatch.mulA(hc, 0.75F));
            bb.softTaper(tx, ty, hx, hy, 0.0F, U * 0.3F, VfxBatch.col(16777215, 0.0F), VfxBatch.col(16777215, 0.95F));
            bb.glow(hx, hy, U * 0.7F, VfxBatch.mulA(hc, 0.6F));
            bb.star(hx, hy, U * 0.45F, 4, r.sec * 6.0F, VfxBatch.col(16777215, 1.0F));
            bb.star(hx, hy, U * 0.35F, 4, r.sec * 6.0F + (float) (Math.PI / 4), hc);
         }, (bb, ix, iy, dx, dy, q, k) -> {
            float fb = 1.0F - q;
            float by = iy - U * 0.4F;
            int hc = VfxBatch.hsv(rnd(k) + r.sec * 0.3F, 0.5F, 1.0F, fb);
            bb.star(ix, by, U * 1.3F * fb + 1.0F, 8, q * 2.0F, hc);
            bb.ring(ix, by, U * (0.3F + 1.4F * eo(q)), U * 0.2F * fb, hc);
            bb.glow(ix, by, U * 0.9F * fb, VfxBatch.col(16777215, 0.7F * fb));

            for (int s = 0; s < 6; s++) {
               float a = rnd(k * 9 + s) * (float) (Math.PI * 2);
               float d = U * 1.4F * eo(q);
               bb.star(ix + cos(a) * d, by + sin(a) * d, 1.6F * fb, 4, 0.0F, VfxBatch.hsv(rnd(s + k), 0.4F, 1.0F, fb));
            }
         });
         if (r.hit) {
            flash(b, r, 16777215, 0.25F * (1.0F - seg(r.h, 0.0F, 0.2F)));
            b.glow(x, y, A * (1.0F - r.h), VfxBatch.hsv(r.sec * 0.3F, 0.3F, 1.0F, 0.4F * (1.0F - r.h)));
         }
      });
   }

   static void buster(SkillVfx.R r, boolean red) {
      SkillVfx.Pal bp = palette(SkillVfx.Theme.NEUTRAL, red ? 16726570 : r.sp.accent);
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float travel = eo(seg(r.u, 0.25F, 1.0F));
      float spin = r.T * 0.75F;
      r.add(
         b -> {
            if (!r.hit) {
               float a = seg(r.u, 0.1F, 0.35F);
               if (r.u < 0.4F) {
                  b.glow(x - U * 6.0F, y, U * 1.2F * a, VfxBatch.col(bp.outer(), 0.5F * a * (1.0F - seg(r.u, 0.3F, 0.4F))));
               }

               for (int g = 3; g >= 0; g--) {
                  float tg = clamp01(travel - (float)g * 0.06F);
                  float gx = mix(x - U * 6.0F, x, tg);
                  float gyy = y - sin(tg * (float) Math.PI) * U * 0.8F;
                  float ga = (g == 0 ? 1.0F : 0.35F / (float)g) * a;
                  float rot = spin - (float)g * 0.5F;
                  b.crescent(gx, gyy, U * 0.9F, U * 0.75F, rot, 2.8F, VfxBatch.col(bp.outer(), 0.0F), VfxBatch.col(bp.outer(), 0.7F * ga));
                  b.crescent(gx, gyy, U * 0.9F, U * 0.42F, rot, 2.8F, VfxBatch.col(bp.mid(), 0.2F * ga), VfxBatch.col(bp.mid(), ga));
                  b.crescent(gx, gyy, U * 0.9F, U * 0.15F, rot, 2.6F, VfxBatch.col(bp.core(), 0.3F * ga), VfxBatch.col(bp.core(), ga));
                  b.crescent(gx, gyy, U * 0.9F, U * 0.42F, rot + (float) Math.PI, 2.8F, VfxBatch.col(bp.mid(), 0.1F * ga), VfxBatch.col(bp.mid(), 0.6F * ga));
                  if (g == 0) {
                     b.glow(gx, gyy, U * 1.3F * a, VfxBatch.col(bp.outer(), 0.35F * a));

                     for (int e = 0; e < 6; e++) {
                        mat(
                           b,
                           r,
                           gx - U * (0.6F + (float)e * 0.45F),
                           gyy + sin(r.sec * 20.0F + (float)e) * 4.0F,
                           1.5F,
                           (float) Math.PI,
                           (1.0F - (float)e / 6.0F) * a,
                           e
                        );
                     }
                  }
               }
            } else {
               float env = r.env();
               float fh = 1.0F - r.h;

               for (int s = -1; s <= 1; s += 2) {
                  b.spark(x, y, (float)s * (float) Math.PI / 4.0F, U * 5.5F * env, U * 0.55F, VfxBatch.col(bp.mid(), env));
                  b.spark(x, y, (float)s * (float) Math.PI / 4.0F, U * 4.5F * env, U * 0.2F, VfxBatch.col(bp.core(), env));
               }

               b.ring(x, y, U * (0.5F + 2.6F * eo(r.h)), U * 0.4F * fh, VfxBatch.col(bp.outer(), 0.8F * fh));
               b.glow(x, y, U * (1.2F + 1.2F * eo(r.h)), VfxBatch.col(bp.outer(), 0.5F * fh));
               b.glow(x, y, U * 0.9F * fh, VfxBatch.col(bp.core(), fh));

               for (int i = 0; i < 16; i++) {
                  float ang = rnd(r.sp.seed + i) * (float) (Math.PI * 2);
                  float d = U * 3.0F * eo(r.h) * (0.4F + 0.6F * rnd(i + 3));
                  b.spark(x + cos(ang) * d, y + sin(ang) * d, ang, U * 0.8F * fh, U * 0.2F, VfxBatch.col(i % 2 == 0 ? bp.core() : bp.mid(), fh));
               }

               burst(b, r, 14, x, y, r.h, U * 2.4F, red ? -U * 1.2F : U * 0.4F, 1.6F, r.sp.seed + 151, red ? 0.5F : 0.0F);
               flash(b, r, bp.outer(), 0.2F * (1.0F - seg(r.h, 0.0F, 0.15F)));
            }
         }
      );
   }

   static void gigaflare(SkillVfx.R r) {
      SkillVfx.Pal p = palette(SkillVfx.Theme.LIGHT, 0);
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float oy = mix(y - U * 3.2F, y, ein(seg(r.u, 0.75F, 1.0F)));
      float g = eob(seg(r.u, 0.0F, 0.5F));
      r.add(b -> {
         if (!r.hit) {
            float orb = U * (0.3F + 0.9F * g);
            b.glow(x, oy, orb * 3.0F, VfxBatch.col(p.outer(), 0.35F * g));
            b.glow(x, oy, orb * 1.6F, VfxBatch.col(p.mid(), 0.7F * g));
            b.disc(x, oy, orb, VfxBatch.col(p.core(), g), VfxBatch.col(p.mid(), 0.8F * g));

            for (int k = 0; k < 16; k++) {
               float ang = r.sec * 0.9F + (float)k * (float) (Math.PI * 2) / 16.0F;
               float l = orb * (1.4F + 0.8F * sin(r.sec * 5.0F + (float)k * 1.3F));
               b.spark(x + cos(ang) * (orb + l * 0.5F), oy + sin(ang) * (orb + l * 0.5F), ang, l, 2.0F, VfxBatch.col(p.mid(), 0.75F * g));
            }

            b.ring(x, oy, orb * 1.5F + sin(r.sec * 8.0F) * 2.0F, 2.0F, VfxBatch.col(p.core(), 0.6F * g));
            converge(b, r, 22, x, oy, U * 3.0F, r.u, r.sp.seed + 71);
         } else {
            float env = r.env();
            float fh = 1.0F - r.h;

            for (int k = 0; k < 3; k++) {
               float q = seg(r.h, (float)k * 0.1F, 0.8F + (float)k * 0.07F);
               if (!(q <= 0.0F) && !(q >= 1.0F)) {
                  b.ring(x, y, U * (0.6F + 4.5F * eo(q)), U * 0.6F * (1.0F - q), VfxBatch.col(k == 1 ? p.core() : p.mid(), 0.8F * (1.0F - q)));
               }
            }

            for (int kx = 0; kx < 20; kx++) {
               float ang = (float)kx * (float) (Math.PI * 2) / 20.0F + r.sec * 0.5F;
               float l = U * (3.0F + 2.0F * (float)(kx % 2)) * env;
               b.spark(x + cos(ang) * l * 0.5F, y + sin(ang) * l * 0.5F, ang, l, U * 0.3F, VfxBatch.col(p.mid(), 0.8F * env));
            }

            b.glow(x, y, U * (2.0F + 2.0F * eo(r.h)), VfxBatch.col(p.outer(), 0.55F * fh));
            b.glow(x, y, U * 1.6F * fh, VfxBatch.col(p.core(), fh));
            burst(b, r, 30, x, y, r.h, U * 4.0F, U * 0.5F, 2.0F, r.sp.seed + 131, 0.0F);
            flash(b, r, p.mid(), 0.45F * (1.0F - seg(r.h, 0.0F, 0.25F)));
         }
      });
   }

   static void hyperGoner(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      r.alpha(b -> {
         float g = r.hit ? 1.0F - seg(r.h, 0.0F, 0.3F) : eo(seg(r.u, 0.2F, 1.0F));
         b.disc(x, y, U * 0.9F * g, VfxBatch.col(0, 0.9F * g), VfxBatch.col(0, 0.0F));
      });
      r.add(
         b -> {
            float a = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.0F, 0.4F) : 1.0F);
            float spin = r.sec * 4.0F;

            for (int arm = 0; arm < 6; arm++) {
               float px = 0.0F;
               float py = 0.0F;

               for (int s = 0; s <= 16; s++) {
                  float f = (float)s / 16.0F;
                  float rr = U * (3.4F - 2.6F * f) * (r.hit ? 1.0F : 1.0F - 0.3F * r.u);
                  float ang = spin + (float)arm * (float) (Math.PI * 2) / 6.0F + f * 3.2F;
                  float qx = x + cos(ang) * rr;
                  float qy = y + sin(ang) * rr * 0.7F;
                  if (s > 0) {
                     float f0 = (float)(s - 1) / 16.0F;
                     b.soft(
                        px,
                        py,
                        qx,
                        qy,
                        U * (0.12F + 0.3F * f),
                        VfxBatch.hsv((float)arm / 6.0F + f0 * 0.3F + r.sec * 0.4F, 0.65F, 1.0F, 0.8F * a * f0),
                        VfxBatch.hsv((float)arm / 6.0F + f * 0.3F + r.sec * 0.4F, 0.65F, 1.0F, 0.8F * a * f)
                     );
                  }

                  px = qx;
                  py = qy;
               }
            }

            for (int i = 0; i < 24; i++) {
               float f = (r.sec * 1.2F + rnd(i)) % 1.0F;
               float rr = U * 3.6F * (1.0F - f);
               float ang = rnd(i * 3) * (float) (Math.PI * 2) + f * 4.0F;
               b.glow(x + cos(ang) * rr, y + sin(ang) * rr * 0.7F, 1.6F, VfxBatch.hsv(rnd(i), 0.4F, 1.0F, bell(f) * a));
            }

            if (r.hit) {
               float fh = 1.0F - r.h;

               for (int k = 0; k < 6; k++) {
                  float q = seg(r.h, (float)k * 0.06F, 0.7F + (float)k * 0.05F);
                  if (!(q <= 0.0F) && !(q >= 1.0F)) {
                     b.ring(
                        x, y, U * (0.5F + 4.0F * eo(q)), U * 0.4F * (1.0F - q), VfxBatch.hsv((float)k / 6.0F + r.sec * 0.2F, 0.6F, 1.0F, 0.85F * (1.0F - q))
                     );
                  }
               }

               b.glow(x, y, U * 3.0F * fh, VfxBatch.col(16777215, 0.6F * fh));

               for (int kx = 0; kx < 14; kx++) {
                  float ang = rnd(kx + 5) * (float) (Math.PI * 2);
                  float d = U * 4.0F * eo(r.h) * (0.5F + 0.5F * rnd(kx));
                  b.star(x + cos(ang) * d, y + sin(ang) * d, 3.0F * fh, 4, 0.0F, VfxBatch.hsv(rnd(kx), 0.4F, 1.0F, fh));
               }

               flash(b, r, 16777215, 0.5F * (1.0F - seg(r.h, 0.0F, 0.3F)));
            }
         }
      );
   }

   static void plusSign(VfxBatch b, float x, float y, float s, int c) {
      b.rect(x, y, s * 2.0F, s * 0.7F, 0.0F, c);
      b.rect(x, y, s * 0.7F, s * 2.0F, 0.0F, c);
   }

   static void heal(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      boolean lunar = r.th == SkillVfx.Theme.LUNAR;
      float sa = eob(seg(r.u, 0.0F, 0.35F)) * (r.hit ? 1.0F - seg(r.h, 0.3F, 1.0F) : 1.0F);
      float rise = r.hit ? 1.0F - seg(r.h, 0.4F, 1.0F) : seg(r.u, 0.15F, 0.5F);
      r.add(
         b -> {
            float rr = U * 1.2F * sa;
            b.ellipse(x, gy, rr * 1.2F, rr * 1.2F * 0.32F, 0.0F, VfxBatch.col(p.outer(), 0.3F * sa), VfxBatch.col(p.outer(), 0.0F));
            b.ringArc(x, gy, rr, rr * 0.32F, 2.2F, VfxBatch.col(p.mid(), 0.85F * sa), 0.0F, (float) (Math.PI * 2));
            b.ringArc(x, gy, rr * 0.7F, rr * 0.7F * 0.32F, 1.2F, VfxBatch.col(p.core(), 0.6F * sa), 0.0F, (float) (Math.PI * 2));
            float rot = r.sec * 1.2F;

            for (int k = 0; k < (lunar ? 6 : 4); k++) {
               float a0 = rot + (float)k * (float) (Math.PI * 2) / (float)(lunar ? 6 : 4);
               b.line(
                  x + cos(a0) * rr * 0.15F,
                  gy + sin(a0) * rr * 0.15F * 0.32F,
                  x + cos(a0) * rr * 0.62F,
                  gy + sin(a0) * rr * 0.62F * 0.32F,
                  2.2F,
                  VfxBatch.col(p.core(), 0.7F * sa),
                  VfxBatch.col(p.mid(), 0.2F * sa)
               );
            }

            for (int i = 0; i < 16; i++) {
               float f = (r.sec * 0.75F + rnd(r.sp.seed + i)) % 1.0F;
               float px = x + (rnd(r.sp.seed + i * 3) - 0.5F) * U * 2.0F;
               float py = gy - f * U * 3.0F;
               b.soft(px, py + U * 0.35F, px, py, 1.4F, VfxBatch.col(p.mid(), 0.0F), VfxBatch.col(p.core(), 0.6F * bell(f) * rise));
               b.glow(px, py, 1.6F, VfxBatch.col(p.core(), 0.9F * bell(f) * rise));
            }

            for (int i = 0; i < 6; i++) {
               float f = (r.sec * 0.5F + (float)i / 6.0F) % 1.0F;
               float px = x + sin((float)i * 2.3F + r.sec) * U * 0.9F;
               float py = y + U * 0.5F - f * U * 2.2F;
               float a = bell(f) * rise;
               if (lunar) {
                  b.star(px, py, 3.0F, 4, 0.0F, VfxBatch.col(p.core(), a));
               } else {
                  plusSign(b, px, py, 3.2F, VfxBatch.col(p.mid(), 0.8F * a));
                  plusSign(b, px, py, 1.5F, VfxBatch.col(p.core(), a));
               }
            }

            if (!r.hit) {
               b.glow(x, y, U * (0.6F + 0.8F * r.u), VfxBatch.col(p.outer(), 0.22F * r.u));
            } else {
               float q = r.h;
               float fh = 1.0F - q;
               b.glow(x, y, U * (1.2F + 1.4F * eo(q)), VfxBatch.col(p.mid(), 0.55F * fh));
               b.glow(x, y, U * 0.8F * fh, VfxBatch.col(p.core(), 0.8F * fh));
               b.ring(x, y, U * (0.5F + 2.2F * eo(q)), U * 0.3F * fh, VfxBatch.col(p.mid(), 0.7F * fh));

               for (int ix = 0; ix < 10; ix++) {
                  float ang = (float)ix * (float) (Math.PI * 2) / 10.0F;
                  float d = U * 2.0F * eo(q);
                  if (lunar) {
                     b.star(x + cos(ang) * d, y + sin(ang) * d * 0.8F, 3.0F * fh, 4, 0.0F, VfxBatch.col(p.core(), fh));
                  } else {
                     plusSign(b, x + cos(ang) * d, y + sin(ang) * d * 0.8F, 2.6F * fh, VfxBatch.col(p.core(), fh));
                  }
               }
            }
         }
      );
   }

   static void barrier(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float rd = U * 1.55F;
      float hr = U * 0.36F;
      r.add(
         b -> {
            float fade = r.hit ? 1.0F - seg(r.h, 0.45F, 1.0F) : 1.0F;
            float sweep = r.sec * 1.2F % 1.6F - 0.3F;
            float lock = r.hit ? 1.0F - seg(r.h, 0.0F, 0.3F) : 0.0F;
            int idx = 0;

            for (int ring = 0; ring < 3; ring++) {
               int count = ring == 0 ? 1 : (ring == 1 ? 6 : 12);
               float rad = ring == 0 ? 0.0F : (ring == 1 ? rd * 0.5F : rd);

               for (int c = 0; c < count; idx++) {
                  float ang = (float)c * (float) (Math.PI * 2) / (float)count + (ring == 1 ? (float) (Math.PI / 6) : 0.0F);
                  float hx = x + cos(ang) * rad;
                  float hy = y + sin(ang) * rad * 0.95F;
                  float st = 0.08F + rnd(r.sp.seed + idx) * 0.5F;
                  float appear = eob(seg(r.u, st, st + 0.25F));
                  if (!(appear <= 0.0F)) {
                     float band = Math.max(0.0F, 1.0F - Math.abs((hx - (x - rd)) / (2.0F * rd) - sweep) * 4.0F);
                     float a = appear * fade;
                     float hs = hr * appear;
                     b.poly(
                        hx,
                        hy,
                        hs,
                        6,
                        (float) (Math.PI / 6),
                        VfxBatch.col(p.mid(), (0.12F + 0.25F * band + 0.4F * lock) * a),
                        VfxBatch.col(p.outer(), (0.25F + 0.2F * band) * a)
                     );
                     b.polyLine(
                        hx, hy, hs, 6, (float) (Math.PI / 6), 1.1F, VfxBatch.col(p.core(), Math.min(1.0F, (0.55F + 0.45F * band + 0.4F * lock) * a)), 1.0F
                     );
                  }

                  c++;
               }
            }

            float rim = seg(r.u, 0.3F, 0.8F) * fade;
            b.ring(x, y, rd * 1.32F, 3.0F, VfxBatch.col(p.mid(), 0.45F * rim));
            b.glow(x, y, rd * 1.3F, VfxBatch.col(p.outer(), 0.12F * rim));
            if (r.hit) {
               float q = r.h;
               b.ring(x, y, rd * (1.3F + 1.2F * eo(q)), U * 0.3F * (1.0F - q), VfxBatch.col(p.core(), 0.8F * (1.0F - q)));
               b.glow(x, y, rd * 1.4F * (1.0F - q), VfxBatch.col(p.mid(), 0.35F * (1.0F - q)));
               burst(b, r, 14, x, y, q, U * 2.4F, -U * 0.6F, 1.4F, r.sp.seed + 141, 0.4F);
            } else {
               converge(b, r, 10, x, y, rd * 1.8F, r.u, r.sp.seed + 143);
            }
         }
      );
   }

   static void powerUp(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float aura = seg(r.u, 0.0F, 0.4F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
      r.add(b -> {
         for (int k = 0; k < 9; k++) {
            float o = (float)(k - 4) / 4.0F;
            float fx = x + o * U * 0.75F;
            float hk = U * (1.2F + 1.3F * (1.0F - Math.abs(o))) * aura * (0.75F + 0.25F * sin(r.sec * 16.0F + (float)k * 1.9F));
            float sway = sin(r.sec * 7.0F + (float)k) * U * 0.2F;
            b.flame(fx, gy, U * 0.42F, hk, sway, VfxBatch.col(p.outer(), 0.55F * aura), VfxBatch.col(p.mid(), 0.15F * aura));
            b.flame(fx, gy, U * 0.22F, hk * 0.6F, sway * 0.75F, VfxBatch.col(p.core(), 0.5F * aura), VfxBatch.col(p.mid(), 0.0F));
         }

         for (int i = 0; i < 4; i++) {
            float f = (r.sec * 0.9F + (float)i / 4.0F) % 1.0F;
            float cyy = gy - f * U * 3.0F;
            b.chevron(x, cyy, U * 1.1F, U * 0.45F, true, 2.4F, VfxBatch.col(p.mid(), bell(f) * aura));
            b.chevron(x, cyy, U * 1.1F, U * 0.45F, true, 0.9F, VfxBatch.col(p.core(), bell(f) * aura));
         }

         for (int i = 0; i < 12; i++) {
            float f = (r.sec * 0.8F + rnd(r.sp.seed + i)) % 1.0F;
            float ang = f * (float) (Math.PI * 2) * 1.5F + (float)i;
            b.glow(x + cos(ang) * U * 0.9F, gy - f * U * 2.8F, 1.8F, VfxBatch.col(p.core(), bell(f) * aura));
         }

         if (r.hit) {
            float q = r.h;
            float fh = 1.0F - q;
            float en = SkillVfx.R.env(q);

            for (int k = 0; k < 12; k++) {
               float px = x + (rnd(r.sp.seed + k) - 0.5F) * U * 2.0F;
               float l = U * (1.5F + 2.0F * rnd(k + 4)) * en;
               b.spark(px, y - l * 0.5F - eo(q) * U, (float) (-Math.PI / 2), l, U * 0.18F, VfxBatch.col(p.mid(), fh));
            }

            float rr = U * (0.6F + 2.2F * eo(q));
            b.ringArc(x, gy, rr, rr * 0.32F, U * 0.3F * fh, VfxBatch.col(p.core(), 0.8F * fh), 0.0F, (float) (Math.PI * 2));
            b.glow(x, y, U * 1.6F * fh, VfxBatch.col(p.mid(), 0.4F * fh));
            flash(b, r, p.mid(), 0.1F * (1.0F - seg(q, 0.0F, 0.2F)));
         }
      });
   }

   static void crest(VfxBatch b, float cx, float cy, float s, int fill, int edge) {
      float[] px = new float[]{-s, s, s * 0.95F, 0.0F, -s * 0.95F};
      float[] py = new float[]{-s * 0.8F, -s * 0.8F, s * 0.15F, s * 1.1F, s * 0.15F};

      for (int i = 0; i < 5; i++) {
         int j = (i + 1) % 5;
         b.tri(cx, cy, fill, cx + px[i], cy + py[i], VfxBatch.mulA(fill, 0.6F), cx + px[j], cy + py[j], VfxBatch.mulA(fill, 0.6F));
         b.line(cx + px[i], cy + py[i], cx + px[j], cy + py[j], 1.4F, edge, edge);
      }
   }

   static void weaken(SkillVfx.R r) {
      SkillVfx.Pal d = r.th == SkillVfx.Theme.NEUTRAL ? palette(SkillVfx.Theme.DARK, 0) : r.p;
      float x = r.cx;
      float y = r.cy;
      float gy = r.gy;
      float U = r.U;
      float a = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
      float sy = y - U * 1.9F;
      r.alpha(b -> {
         b.ellipse(x, gy, U * 1.6F * a, U * 0.5F * a, 0.0F, VfxBatch.col(0, 0.45F * a), VfxBatch.col(0, 0.0F));
         if (r.hit) {
            float q = r.h;

            for (int k = 0; k < 9; k++) {
               float ang = rnd(r.sp.seed + k * 3) * (float) (Math.PI * 2);
               float v = U * (0.6F + 0.9F * rnd(k + 2));
               float px = x + cos(ang) * v * q;
               float py = sy + sin(ang) * v * q * 0.5F + U * 3.5F * q * q;
               b.shard(px, py, U * 0.35F, U * 0.2F, ang + q * 6.0F, VfxBatch.col(d.mid(), 0.85F * (1.0F - q)), VfxBatch.col(d.dark(), 0.85F * (1.0F - q)));
            }
         }
      });
      r.add(b -> {
         if (!r.hit) {
            float ea = eob(seg(r.u, 0.0F, 0.35F));
            crest(b, x, sy, U * 0.7F * ea, VfxBatch.col(d.outer(), 0.55F * ea), VfxBatch.col(d.core(), 0.8F * ea));
            float cr = seg(r.u, 0.45F, 1.0F);

            for (int k = 0; k < 3; k++) {
               float ang = (float) (-Math.PI / 2) + (float)(k - 1) * 1.1F + 0.3F;
               float[] pts = VfxBatch.boltPoints(x, sy, x + cos(ang) * U * 0.8F * cr, sy + sin(ang) * U * 0.8F * cr, r.sp.seed + k, U * 0.15F, 3);

               for (int i = 0; i + 3 < pts.length; i += 2) {
                  b.line(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], 1.2F, VfxBatch.col(d.accent(), cr), VfxBatch.col(d.accent(), cr));
               }
            }
         }

         for (int i = 0; i < 4; i++) {
            float f = (r.sec * 1.1F + (float)i / 4.0F) % 1.0F;
            float cyy = y - U * 1.4F + f * U * 2.4F;
            b.chevron(x, cyy, U * 1.1F, U * 0.4F, false, 2.4F, VfxBatch.col(d.mid(), bell(f) * a));
            b.chevron(x, cyy, U * 1.1F, U * 0.4F, false, 0.9F, VfxBatch.col(d.accent(), bell(f) * a));
         }

         for (int i = 0; i < 12; i++) {
            float f = (r.sec * 0.9F + rnd(r.sp.seed + i)) % 1.0F;
            float px = x + (rnd(r.sp.seed + i * 5) - 0.5F) * U * 1.8F;
            b.glow(px, y - U * 1.2F + f * U * 2.4F, 1.6F, VfxBatch.col(d.mid(), bell(f) * a));
         }

         if (r.hit) {
            float q = r.h;
            float fh = 1.0F - q;
            float rr = U * (2.6F - 1.8F * eo(q));
            b.ringArc(x, gy, rr, rr * 0.32F, U * 0.3F * fh, VfxBatch.col(d.mid(), 0.8F * fh), 0.0F, (float) (Math.PI * 2));
            float br = 1.0F - seg(q, 0.0F, 0.3F);
            b.glow(x, sy, U * 1.2F * br, VfxBatch.col(d.accent(), 0.6F * br));
         }
      });
   }

   static void sleep(SkillVfx.R r) {
      SkillVfx.Pal p = r.th == SkillVfx.Theme.NEUTRAL ? new SkillVfx.Pal(16777215, 14209279, 10128639, 2760784, 16774320) : r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float hx = x + U * 0.3F;
      float hy = y - U * 1.1F;
      float a = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
      r.add(
         b -> {
            b.glow(x, y - U * 0.3F, U * 1.8F, VfxBatch.col(p.outer(), 0.18F * a));

            for (int i = 0; i < 5; i++) {
               float f = (r.sec * 0.45F + (float)i / 5.0F) % 1.0F;
               float zx = hx + f * U * 1.4F + sin(r.sec * 2.5F + (float)i * 1.7F) * U * 0.25F;
               float zy = hy - f * U * 1.8F;
               float s = U * (0.12F + 0.2F * f);
               float za = bell(f) * a;
               float rot = -0.15F + 0.2F * sin(r.sec * 2.0F + (float)i);
               b.zee(zx, zy, s, rot, 2.6F, VfxBatch.col(p.mid(), 0.6F * za));
               b.zee(zx, zy, s, rot, 1.1F, VfxBatch.col(p.core(), za));
            }

            float mx = x - U * 0.9F;
            float my = y - U * 1.6F;
            b.glow(mx, my, U * 0.7F, VfxBatch.col(p.outer(), 0.3F * a));
            b.crescent(mx, my, U * 0.32F, U * 0.24F, (float) (Math.PI * 4.0 / 5.0), 2.6F, VfxBatch.col(p.mid(), 0.5F * a), VfxBatch.col(p.accent(), a));

            for (int i = 0; i < 6; i++) {
               float f = (r.sec * 0.35F + rnd(r.sp.seed + i)) % 1.0F;
               float bx = x + (rnd(r.sp.seed + i * 3) - 0.5F) * U * 1.8F + sin(r.sec * 2.0F + (float)i) * 3.0F;
               float by = y + U * 0.6F - f * U * 2.4F;
               float br = U * (0.08F + 0.1F * rnd(i));
               b.ring(bx, by, br, 1.1F, VfxBatch.col(p.core(), 0.7F * bell(f) * a));
               b.glow(bx - br * 0.3F, by - br * 0.3F, br * 0.35F, VfxBatch.col(16777215, 0.8F * bell(f) * a));
            }

            if (r.th == SkillVfx.Theme.ELECTRIC) {
               int fl = (int)(r.T * 1.5F);

               for (int i = 0; i < 4; i++) {
                  float a0 = rnd(fl * 3 + i) * (float) (Math.PI * 2);
                  float a1 = a0 + 1.2F;
                  b.bolt(
                     x + cos(a0) * U,
                     y + sin(a0) * U * 0.8F,
                     x + cos(a1) * U,
                     y + sin(a1) * U * 0.8F,
                     fl * 11 + i,
                     U * 0.25F,
                     0.6F,
                     VfxBatch.col(p.outer(), 0.6F * a),
                     VfxBatch.col(p.core(), 0.9F * a),
                     0
                  );
               }
            }

            if (r.hit) {
               float q = r.h;
               float fh = 1.0F - q;
               b.glow(x, y - U * 0.4F, U * (1.0F + 1.4F * eo(q)), VfxBatch.col(p.mid(), 0.45F * fh));

               for (int i = 0; i < 8; i++) {
                  float ang = (float)i * (float) (Math.PI * 2) / 8.0F - (float) (Math.PI / 2);
                  float d = U * 1.6F * eo(q);
                  b.zee(x + cos(ang) * d, y - U * 0.4F + sin(ang) * d, U * 0.14F * fh + 0.5F, 0.0F, 1.2F, VfxBatch.col(p.core(), fh));
               }
            }
         }
      );
   }

   static void confuse(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      float hy = y - U * 1.1F;
      float a = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.55F, 1.0F) : 1.0F);
      r.add(
         b -> {
            for (int arm = 0; arm < 3; arm++) {
               float px = 0.0F;
               float py = 0.0F;

               for (int s = 0; s <= 14; s++) {
                  float f = (float)s / 14.0F;
                  float ang = -r.sec * 4.0F + (float)arm * (float) (Math.PI * 2) / 3.0F + f * 4.5F;
                  float rr = U * (0.2F + 1.7F * f);
                  float qx = x + cos(ang) * rr;
                  float qy = y + sin(ang) * rr * 0.75F;
                  if (s > 0) {
                     float f0 = (float)(s - 1) / 14.0F;
                     b.soft(
                        px,
                        py,
                        qx,
                        qy,
                        4.2F * (1.0F - f) + 1.0F,
                        VfxBatch.col(p.outer(), 0.6F * a * (1.0F - f0)),
                        VfxBatch.col(p.outer(), 0.6F * a * (1.0F - f))
                     );
                     b.soft(
                        px, py, qx, qy, 1.8F * (1.0F - f) + 0.6F, VfxBatch.col(p.core(), 0.9F * a * (1.0F - f0)), VfxBatch.col(p.core(), 0.9F * a * (1.0F - f))
                     );
                  }

                  px = qx;
                  py = qy;
               }
            }

            b.ringArc(x, hy, U * 0.8F, U * 0.22F, 1.4F, VfxBatch.col(p.outer(), 0.55F * a), 0.0F, (float) (Math.PI * 2));
            b.glow(x, y, U * 1.6F, VfxBatch.col(p.outer(), 0.15F * a));

            for (int i = 0; i < 5; i++) {
               float ang = r.sec * 3.2F + (float)i * (float) (Math.PI * 2) / 5.0F;
               float depth = sin(ang);
               float sx = x + cos(ang) * U * 0.8F;
               float sy = hy + depth * U * 0.22F;
               b.star(
                  sx,
                  sy,
                  (4.0F + 1.6F * depth) * a,
                  4,
                  r.sec * 4.0F + (float)i,
                  VfxBatch.hsv((float)i / 5.0F + r.sec * 0.2F, 0.5F, 1.0F, (0.6F + 0.4F * depth) * a)
               );
            }

            if (r.hit) {
               float q = r.h;
               float fh = 1.0F - q;

               for (int i = 0; i < 10; i++) {
                  float ang = (float)i * (float) (Math.PI * 2) / 10.0F + q * 3.0F;
                  float d = U * (0.6F + 1.8F * eo(q));
                  b.star(x + cos(ang) * d, y + sin(ang) * d * 0.8F, 2.4F * fh, 4, q * 5.0F, VfxBatch.hsv((float)i / 10.0F, 0.5F, 1.0F, fh));
               }

               b.glow(x, y, U * 1.4F * fh, VfxBatch.col(p.mid(), 0.35F * fh));
            }
         }
      );
   }

   static void magnet(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int tp = 16752688;
      r.add(
         b -> {
            float a = seg(r.u, 0.0F, 0.2F) * (r.hit ? 1.0F - seg(r.h, 0.3F, 0.8F) : 1.0F);

            for (int i = 0; i < 20; i++) {
               float f = (r.sec * 1.3F + rnd(r.sp.seed + i)) % 1.0F;
               float ang = rnd(r.sp.seed + i * 7) * (float) (Math.PI * 2);
               float d0 = U * 3.4F * (1.0F - eo(f)) + U * 0.2F;
               float d1 = d0 + U * 0.9F * (1.0F - f);
               b.soft(
                  x + cos(ang) * d1,
                  y + sin(ang) * d1 * 0.8F,
                  x + cos(ang) * d0,
                  y + sin(ang) * d0 * 0.8F,
                  1.6F,
                  VfxBatch.col(p.mid(), 0.0F),
                  VfxBatch.col(p.core(), 0.9F * bell(f) * a)
               );
            }

            float orb = U * (0.3F + 0.6F * eo(r.u));
            b.glow(x, y, orb * 2.2F, VfxBatch.col(p.outer(), 0.4F * a));
            b.glow(x, y, orb, VfxBatch.col(p.core(), 0.8F * a));
            b.ring(x, y, orb * 1.3F + sin(r.sec * 10.0F) * 1.5F, 1.6F, VfxBatch.col(p.mid(), 0.7F * a));
            if (r.hit) {
               float q = r.h;
               float fh = 1.0F - q;
               b.ring(x, y, U * (0.5F + 2.6F * eo(q)), U * 0.3F * fh, VfxBatch.col(p.mid(), 0.8F * fh));

               for (int i = 0; i < 12; i++) {
                  float f = seg(q, rnd(i) * 0.3F, 0.7F + rnd(i) * 0.3F);
                  float px = x + (rnd(r.sp.seed + i * 3) - 0.5F) * U * 2.0F;
                  b.star(px, y - f * U * 2.2F, 2.6F * bell(f) + 0.2F, 4, 0.0F, VfxBatch.col(tp, bell(f)));
               }

               b.glow(x, y, U * 1.4F * fh, VfxBatch.col(tp, 0.35F * fh));
            }
         }
      );
   }

   static void pacify(SkillVfx.R r) {
      SkillVfx.Pal p = r.p;
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int pink = 16751304;
      r.add(b -> {
         float a = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);

         for (int k = 0; k < 3; k++) {
            float f = (r.sec * 0.5F + (float)k / 3.0F) % 1.0F;
            b.ring(x, y, U * (0.5F + 2.0F * f), 2.0F * (1.0F - f) + 0.5F, VfxBatch.col(p.mid(), 0.5F * (1.0F - f) * a));
         }

         for (int i = 0; i < 12; i++) {
            float f = (r.sec * 0.4F + rnd(r.sp.seed + i)) % 1.0F;
            float px = x + (rnd(r.sp.seed + i * 3) - 0.5F) * U * 2.4F + sin(r.sec * 1.5F + (float)i) * 4.0F;
            b.star(px, y - U * 1.8F + f * U * 2.6F, 2.0F, 4, 0.0F, VfxBatch.col(p.core(), bell(f) * a));
         }

         for (int i = 0; i < 3; i++) {
            float f = (r.sec * 0.35F + (float)i / 3.0F) % 1.0F;
            float hx = x + (float)(i - 1) * U * 0.7F + sin(r.sec * 2.0F + (float)i) * 3.0F;
            float s = U * 0.2F * (0.6F + 0.4F * bell(f));
            b.heart(hx, y - f * U * 1.8F, s, VfxBatch.col(pink, 0.9F * bell(f) * a), VfxBatch.col(pink, 0.4F * bell(f) * a));
         }

         if (r.hit) {
            float q = r.h;
            float fh = 1.0F - q;
            b.glow(x, y, U * (1.0F + 1.6F * eo(q)), VfxBatch.col(p.mid(), 0.4F * fh));

            for (int i = 0; i < 8; i++) {
               float ang = (float)i * (float) (Math.PI * 2) / 8.0F;
               float d = U * 2.0F * eo(q);
               b.heart(x + cos(ang) * d, y + sin(ang) * d * 0.8F, U * 0.16F * fh + 0.3F, VfxBatch.col(pink, fh), VfxBatch.col(pink, 0.4F * fh));
            }
         }
      });
   }

   static void spare(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int gold = 16770650;
      r.add(b -> {
         float a = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.6F, 1.0F) : 1.0F);
         b.glow(x, y, U * (1.0F + 0.8F * r.u), VfxBatch.col(gold, 0.25F * a));

         for (int i = 0; i < 14; i++) {
            float f = (r.sec * 0.6F + rnd(r.sp.seed + i)) % 1.0F;
            float px = x + (rnd(r.sp.seed + i * 3) - 0.5F) * U * 2.2F;
            b.star(px, y + U * 0.6F - f * U * 2.6F, 2.6F * bell(f) + 0.3F, 4, 0.0F, VfxBatch.col(gold, bell(f) * a));
         }

         if (r.hit) {
            float q = r.h;
            float fh = 1.0F - q;

            for (int i = 0; i < 12; i++) {
               float ang = (float)i * (float) (Math.PI * 2) / 12.0F;
               float d = U * 2.6F * eo(q);
               b.star(x + cos(ang) * d, y + sin(ang) * d * 0.8F, 3.4F * fh, 4, 0.0F, VfxBatch.col(gold, fh));
            }

            b.ring(x, y, U * (0.5F + 2.4F * eo(q)), U * 0.3F * fh, VfxBatch.col(gold, 0.8F * fh));
            b.glow(x, y, U * 1.6F * fh, VfxBatch.col(16777215, 0.5F * fh));
         }
      });
   }

   static void plead(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int gold = 16768307;
      int pink = 16747188;
      float hy = y - U * 0.2F;
      r.add(b -> {
         float a = seg(r.u, 0.0F, 0.3F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
         float beat = 1.0F + 0.15F * (float)Math.pow((double)Math.max(0.0F, sin(r.sec * 7.0F)), 8.0);
         float hs = U * 0.45F * eob(seg(r.u, 0.0F, 0.4F)) * beat;
         b.glow(x, hy, hs * 3.0F, VfxBatch.col(pink, 0.3F * a));
         b.heart(x, hy, hs, VfxBatch.col(16763100, a), VfxBatch.col(pink, 0.85F * a));

         for (int i = 0; i < 3; i++) {
            float f = (r.sec * 0.8F + (float)i / 3.0F) % 1.0F;
            b.chevron(x, y + U * 0.6F - f * U * 2.4F, U, U * 0.35F, true, 2.0F, VfxBatch.col(gold, bell(f) * a));
         }

         if (r.hit) {
            float q = r.h;
            float fh = 1.0F - q;
            b.ring(x, hy, U * (0.5F + 2.0F * eo(q)), U * 0.25F * fh, VfxBatch.col(pink, 0.8F * fh));

            for (int i = 0; i < 10; i++) {
               float ang = (float)i * (float) (Math.PI * 2) / 10.0F;
               float d = U * 2.0F * eo(q);
               b.star(x + cos(ang) * d, hy + sin(ang) * d, 2.6F * fh, 4, 0.0F, VfxBatch.col(gold, fh));
            }
         }
      });
   }

   static void scan(SkillVfx.R r) {
      float x = r.cx;
      float y = r.cy;
      float U = r.U;
      int c1 = 8191944;
      r.add(
         b -> {
            float a = seg(r.u, 0.0F, 0.25F) * (r.hit ? 1.0F - seg(r.h, 0.5F, 1.0F) : 1.0F);
            float close = eo(seg(r.u, 0.0F, 0.8F));
            float rr = U * (2.6F - 1.3F * close);
            float rot = (1.0F - close) * 1.6F;
            float l = U * 0.5F;

            for (int k = 0; k < 4; k++) {
               float ang = rot + (float) (Math.PI / 4) + (float)k * (float) Math.PI / 2.0F;
               float cxk = x + cos(ang) * rr;
               float cyk = y + sin(ang) * rr;
               b.line(
                  cxk,
                  cyk,
                  cxk + cos(ang + (float) (Math.PI * 3.0 / 4.0)) * l,
                  cyk + sin(ang + (float) (Math.PI * 3.0 / 4.0)) * l,
                  2.0F,
                  VfxBatch.col(c1, a),
                  VfxBatch.col(c1, 0.3F * a)
               );
               b.line(
                  cxk,
                  cyk,
                  cxk + cos(ang - (float) (Math.PI * 3.0 / 4.0)) * l,
                  cyk + sin(ang - (float) (Math.PI * 3.0 / 4.0)) * l,
                  2.0F,
                  VfxBatch.col(c1, a),
                  VfxBatch.col(c1, 0.3F * a)
               );
            }

            b.ring(x, y, U * 0.5F, 1.2F, VfxBatch.col(c1, 0.6F * a));

            for (int k = 0; k < 4; k++) {
               float ang = (float)k * (float) Math.PI / 2.0F;
               b.line(
                  x + cos(ang) * U * 0.25F,
                  y + sin(ang) * U * 0.25F,
                  x + cos(ang) * U * 0.95F,
                  y + sin(ang) * U * 0.95F,
                  1.0F,
                  VfxBatch.col(c1, 0.85F * a),
                  VfxBatch.col(c1, 0.0F)
               );
            }

            if (!r.hit) {
               float sl = r.sec * 1.5F % 1.0F;
               float sy = y - rr + sl * rr * 2.0F;
               b.soft(x - rr, sy, x + rr, sy, 2.2F, VfxBatch.col(c1, 0.75F * a), VfxBatch.col(c1, 0.75F * a));
               b.quad(
                  x - rr,
                  sy - U * 0.6F,
                  VfxBatch.col(c1, 0.0F),
                  x + rr,
                  sy - U * 0.6F,
                  VfxBatch.col(c1, 0.0F),
                  x + rr,
                  sy,
                  VfxBatch.col(c1, 0.18F * a),
                  x - rr,
                  sy,
                  VfxBatch.col(c1, 0.18F * a)
               );
            }

            int tick = (int)(r.T * 0.5F);

            for (int i = 0; i < 6; i++) {
               float tw = rnd(tick * 7 + i);
               if (!(tw < 0.4F)) {
                  b.rect(x + rr * 1.1F + 6.0F + tw * 8.0F, y - rr + (float)i * rr * 0.35F, tw * 16.0F, 1.6F, 0.0F, VfxBatch.col(c1, 0.6F * a));
               }
            }

            if (r.hit) {
               float q = r.h;
               float fh = 1.0F - q;
               b.ring(x, y, U * (0.5F + 1.5F * eo(q)), 2.4F * fh + 0.2F, VfxBatch.col(c1, fh));
               b.glow(x, y, U * 1.2F * fh, VfxBatch.col(c1, 0.35F * fh));
            }
         }
      );
   }

   @FunctionalInterface
   interface Drop {
      void draw(VfxBatch var1, float var2, float var3, float var4, float var5, float var6, int var7);
   }

   static record Pal(int core, int mid, int outer, int dark, int accent) {
   }

   static final class R {
      final DrawContext ctx;
      final SkillVfx.Spec sp;
      final SkillVfx.Pal p;
      final SkillVfx.Theme th;
      final float cx;
      final float cy;
      final float U;
      final float gy;
      final float sw;
      final float sh;
      final float T;
      final float st;
      final float ht;
      final float u;
      final float h;
      final boolean hit;
      final float sec;

      R(DrawContext ctx, SkillVfx.Spec sp, float cx, float cy, float T, float st, float ht, float sw, float sh) {
         this.ctx = ctx;
         this.sp = sp;
         this.th = sp.theme;
         this.p = SkillVfx.palette(sp.theme, sp.accent);
         this.cx = cx;
         this.cy = cy;
         this.U = 30.0F;
         this.gy = cy + this.U * 0.8F;
         this.sw = sw;
         this.sh = sh;
         this.T = T;
         this.st = st;
         this.ht = ht;
         this.u = SkillVfx.clamp01(T / st);
         this.hit = T >= st;
         this.h = this.hit ? SkillVfx.clamp01((T - st) / ht) : 0.0F;
         this.sec = T / 20.0F;
      }

      void add(Consumer<VfxBatch> f) {
         VfxBatch.draw(this.ctx, true, f);
      }

      void alpha(Consumer<VfxBatch> f) {
         VfxBatch.draw(this.ctx, false, f);
      }

      float env() {
         return this.h < 0.08F ? SkillVfx.eo(this.h / 0.08F) : 1.0F - SkillVfx.eis(SkillVfx.seg(this.h, 0.08F, 1.0F));
      }

      static float env(float q) {
         return !(q <= 0.0F) && !(q >= 1.0F) ? (q < 0.1F ? SkillVfx.eo(q / 0.1F) : 1.0F - SkillVfx.eis(SkillVfx.seg(q, 0.1F, 1.0F))) : 0.0F;
      }

      float win(float t0, float len) {
         return (this.T - t0) / len;
      }
   }

   public static final class Spec {
      final SkillType type;
      final SkillVfx.Theme theme;
      final SkillVfx.Special special;
      final boolean psiAccent;
      final int accent;
      final int hits;
      final int seed;
      final int tier;

      Spec(SkillType type, SkillVfx.Theme theme, SkillVfx.Special special, boolean psiAccent, int accent, int hits, int seed) {
         this(type, theme, special, psiAccent, accent, hits, seed, 1);
      }

      Spec(SkillType type, SkillVfx.Theme theme, SkillVfx.Special special, boolean psiAccent, int accent, int hits, int seed, int tier) {
         this.type = type;
         this.theme = theme;
         this.special = special;
         this.psiAccent = psiAccent;
         this.accent = accent & 16777215;
         this.hits = hits;
         this.seed = seed;
         this.tier = Math.max(1, Math.min(4, tier));
      }
   }

   public static enum Special {
      NONE,
      SNOWGRAVE,
      STARSTORM,
      BUSTER,
      RED_BUSTER,
      GIGAFLARE,
      HYPER_GONER;
   }

   public static enum Theme {
      FIRE,
      ICE,
      ELECTRIC,
      PSI,
      SLASH,
      LIGHT,
      DARK,
      IMPACT,
      LUNAR,
      NEUTRAL;
   }
}
