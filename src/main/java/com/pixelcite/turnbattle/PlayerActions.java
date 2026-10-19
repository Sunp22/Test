package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class PlayerActions {
   private static final Map<UUID, PlayerActions.Act> acts = new HashMap<>();
   private static long pausedNanos;
   private static long lastTickNanos;
   private static ItemStack knife;
   private static ItemStack cachedSoulStack;
   private static SoulType cachedSoul;
   private static boolean camOn;
   private static float camW;
   private static float prevCamW;
   private static Vec3d camGoal = Vec3d.ZERO;
   private static Vec3d prevCamGoal = Vec3d.ZERO;
   private static Vec3d lookGoal = Vec3d.ZERO;
   private static Vec3d prevLookGoal = Vec3d.ZERO;
   private static float shake;
   private static PlayerActions.Kind shotKind;
   private static boolean hudSaved;
   private static boolean savedHudHidden;
   private static float engulf;
   private static float prevEngulf;
   private static long flashAt = 0L;
   private static final Identifier TEX_BEAM = Identifier.of("turnbattle", "textures/effects/ritual_beam.png");
   private static final Identifier TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   private static final Identifier TEX_RING = Identifier.of("turnbattle", "textures/effects/ritual_ring.png");
   private static final Identifier TEX_WHITE = Identifier.of("turnbattle", "textures/effects/fountain_white.png");
   private static final Identifier TEX_CRACK = Identifier.of("turnbattle", "textures/effects/fountain_crack.png");

   private PlayerActions() {
   }

   static long now() {
      return System.nanoTime() - pausedNanos;
   }

   private static float ticksSince(long nanos) {
      return (float)(now() - nanos) / 5.0E7F;
   }

   private static UUID localId() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.player != null ? mc.player.getUuid() : null;
   }

   private static boolean isLocal(PlayerActions.Act a) {
      return a.who.equals(localId());
   }

   static void onPacket(int kind, UUID who, float yaw, double bx, double by, double bz, int soulOrd) {
      MinecraftClient mc = MinecraftClient.getInstance();
      PlayerActions.Act a = acts.get(who);
      switch (kind) {
         case 0:
            PlayerActions.Act n = new PlayerActions.Act(PlayerActions.Kind.FOUNTAIN, who, yaw);
            n.beam = new Vec3d(bx, by, bz);
            acts.put(who, n);
            if (who.equals(localId()) && mc.currentScreen instanceof DarkWorldCreationScreen) {
               mc.setScreen(null);
            }
            break;
         case 1:
            if (a != null && a.kind == PlayerActions.Kind.FOUNTAIN && a.plungeAt < 0L) {
               a.plungeAt = now();
            }
            break;
         case 2:
            if (a != null && a.kind == PlayerActions.Kind.FOUNTAIN && a.endAt < 0L) {
               a.endAt = now();
            }
            break;
         case 3:
            if (who.equals(localId())) {
               return;
            }

            if (a != null && a.kind == PlayerActions.Kind.FOUNTAIN) {
               return;
            }

            PlayerActions.Act nx = new PlayerActions.Act(PlayerActions.Kind.SOUL, who, yaw);
            SoulType[] st = SoulType.values();
            nx.soul = soulOrd >= 0 && soulOrd < st.length ? st[soulOrd] : null;
            acts.put(who, nx);
            PlayerEntity p = mc.world != null ? mc.world.getPlayerByUuid(who) : null;
            if (p != null) {
               nx.sound = new PositionedSoundInstance(
                  TurnBattleSounds.SND_REMOVE_SOUL_WORLD, SoundCategory.PLAYERS, 0.8F, 1.0F, SoundInstance.createRandom(), p.getX(), p.getY() + 1.0, p.getZ()
               );
               mc.getSoundManager().play(nx.sound);
            }
            break;
         case 4:
            if (a != null && a.kind == PlayerActions.Kind.SOUL && a.cancelAt < 0L && (a.finishAt < 0L || who.equals(localId()))) {
               a.cancelAt = now();
               if (a.sound != null) {
                  mc.getSoundManager().stop(a.sound);
               }
            }
            break;
         case 5:
            if (a != null && a.kind == PlayerActions.Kind.SOUL && a.finishAt < 0L) {
               a.finishAt = now();
            }
         case 6:
         default:
            break;
         case 7:
            if (a != null && a.kind == PlayerActions.Kind.MENU && a.endAt < 0L) {
               a.endAt = now();
            }
            break;
         case 8:
            if (who.equals(localId())) {
               endLocalMenu();
            }

            PlayerActions.Act n = new PlayerActions.Act(PlayerActions.Kind.LIGHT, who, yaw);
            n.beam = new Vec3d(bx, by, bz);
            acts.put(who, n);
            if (who.equals(localId())) {
               if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)) {
                  mc.setScreen(null);
               }

               TurnBattleSounds.playClient(TurnBattleSounds.DTRANS_LW, 1.0F, 1.0F);
               LightPillarClient.beginExit();
            }
      }
   }

   static void startLocalSoulRip(SoulType soul) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         PlayerActions.Act n = new PlayerActions.Act(PlayerActions.Kind.SOUL, mc.player.getUuid(), mc.player.getYaw());
         n.soul = soul;
         acts.put(n.who, n);
      }
   }

   static void cancelLocalSoulRip() {
      PlayerActions.Act a = acts.get(localId());
      if (a != null && a.kind == PlayerActions.Kind.SOUL && a.cancelAt < 0L && a.finishAt < 0L) {
         a.cancelAt = now();
      }
   }

   static void finishLocalSoulRip() {
      PlayerActions.Act a = acts.get(localId());
      if (a != null && a.kind == PlayerActions.Kind.SOUL && a.finishAt < 0L) {
         a.finishAt = now();
      }
   }

   static boolean isActing(UUID who) {
      return who != null && acts.containsKey(who);
   }

   static void clear(UUID who) {
      if (who != null) {
         acts.remove(who);
      }
   }

   static void startLocalMenu() {
      endLocalMenu();
   }

   static void endLocalMenu() {
      PlayerActions.Act a = acts.get(localId());
      if (a != null && a.kind == PlayerActions.Kind.MENU && a.endAt < 0L) {
         a.endAt = now();
         sendMenuState(false);
      }
   }

   private static void sendMenuState(boolean open) {
      try {
         PacketByteBuf b = PacketByteBufs.create();
         b.writeBoolean(open);
         Net121.sendToServer(TurnBattleMod.MENU_STATE, b);
      } catch (Throwable var2) {
      }
   }

   public static boolean localLocked() {
      PlayerActions.Act a = acts.get(localId());
      if (a == null) {
         return false;
      } else if (a.kind == PlayerActions.Kind.FOUNTAIN) {
         return a.endAt < 0L;
      } else if (a.kind == PlayerActions.Kind.MENU) {
         return false;
      } else {
         return a.kind == PlayerActions.Kind.LIGHT ? true : a.cancelAt < 0L;
      }
   }

   static void tick(MinecraftClient mc) {
      long wall = System.nanoTime();
      if (mc.isPaused() && lastTickNanos > 0L) {
         pausedNanos = pausedNanos + (wall - lastTickNanos);
      }

      lastTickNanos = wall;
      if (mc.world == null) {
         acts.clear();
         camW = 0.0F;
         camOn = false;
         prevEngulf = 0.0F;
         engulf = 0.0F;
      } else {
         Iterator it = acts.values().iterator();

         while (it.hasNext()) {
            PlayerActions.Act a = it.next();
            if (a.world != mc.world) {
               it.remove();
            } else if (expired(a)) {
               it.remove();
            } else {
               fx(mc, a);
            }
         }

         tickCamera(mc);
         tickEngulf(mc);
      }
   }

   private static boolean expired(PlayerActions.Act a) {
      if (a.endAt > 0L && ticksSince(a.endAt) > 8.0F) {
         return true;
      } else if (a.kind == PlayerActions.Kind.MENU) {
         return ticksSince(a.start) > 36000.0F;
      } else if (a.kind == PlayerActions.Kind.LIGHT) {
         return ticksSince(a.start) > 148.0F;
      } else if (a.kind == PlayerActions.Kind.FOUNTAIN) {
         return a.plungeAt > 0L ? ticksSince(a.plungeAt) > 195.0F : ticksSince(a.start) > 240.0F;
      } else if (a.cancelAt > 0L) {
         return ticksSince(a.cancelAt) > 8.0F;
      } else {
         return a.finishAt > 0L ? ticksSince(a.finishAt) > 24.0F : ticksSince(a.start) > 96.0F;
      }
   }

   private static void fx(MinecraftClient mc, PlayerActions.Act a) {
      if (a.kind != PlayerActions.Kind.MENU) {
         if (a.kind == PlayerActions.Kind.LIGHT) {
            PlayerEntity p = mc.world.getPlayerByUuid(a.who);
            if (p != null && !(ticksSince(a.start) < 8.0F)) {
               for (int i = 0; i < 2; i++) {
                  double ang = mc.world.random.nextDouble() * Math.PI * 2.0;
                  double rr = 0.3 + mc.world.random.nextDouble() * 0.8;
                  mc.world
                     .addParticle(
                        ParticleTypes.END_ROD,
                        p.getX() + Math.cos(ang) * rr,
                        p.getY() - 0.5 + mc.world.random.nextDouble() * 2.5,
                        p.getZ() + Math.sin(ang) * rr,
                        0.0,
                        0.06 + mc.world.random.nextDouble() * 0.08,
                        0.0
                     );
               }
            }
         } else {
            if (a.kind == PlayerActions.Kind.FOUNTAIN) {
               if (a.plungeAt < 0L || a.endAt > 0L) {
                  return;
               }

               float u = ticksSince(a.plungeAt) - 5.0F;
               if (u < 0.0F) {
                  return;
               }

               Vec3d b = a.beam;
               if (!a.impactFxDone) {
                  a.impactFxDone = true;

                  for (int i = 0; i < 26; i++) {
                     double ang = (double)i / 26.0 * Math.PI * 2.0;
                     mc.world.addParticle(ParticleTypes.END_ROD, b.x, b.y + 0.05, b.z, Math.cos(ang) * 0.32, 0.02, Math.sin(ang) * 0.32);
                  }

                  if (isLocal(a)) {
                     shake = 1.4F;
                  }

                  if (mc.player != null && mc.player.squaredDistanceTo(b) < 1600.0) {
                     flashAt = System.nanoTime();
                  }
               }

               float front = darkFront(u);
               if (front < 0.5F && u < 40.0F) {
                  for (int i = 0; i < 3; i++) {
                     double ox = (mc.world.random.nextDouble() - 0.5) * 1.2;
                     double oz = (mc.world.random.nextDouble() - 0.5) * 1.2;
                     mc.world.addParticle(ParticleTypes.END_ROD, b.x + ox, b.y + 0.1, b.z + oz, 0.0, 0.25 + mc.world.random.nextDouble() * 0.25, 0.0);
                  }
               }

               if (u >= 22.0F && u < 122.0F) {
                  for (int i = 0; i < 2; i++) {
                     double ang = mc.world.random.nextDouble() * Math.PI * 2.0;
                     double rr = 0.4 + mc.world.random.nextDouble() * 0.6;
                     mc.world
                        .addParticle(
                           ParticleTypes.SQUID_INK,
                           b.x + Math.cos(ang) * rr,
                           b.y + 0.08,
                           b.z + Math.sin(ang) * rr,
                           Math.cos(ang) * 0.05,
                           0.02,
                           Math.sin(ang) * 0.05
                        );
                  }

                  if (u >= 46.0F && !Double.isNaN(a.beamTop)) {
                     float spread = CloneAnimator.clamp01((u - 22.0F - 24.0F) / 34.0F) * 8.0F + 1.0F;

                     for (int i = 0; i < 4; i++) {
                        double ang = mc.world.random.nextDouble() * Math.PI * 2.0;
                        double rr = mc.world.random.nextDouble() * (double)spread;
                        mc.world
                           .addParticle(
                              ParticleTypes.SQUID_INK,
                              b.x + Math.cos(ang) * rr,
                              a.beamTop - 0.1,
                              b.z + Math.sin(ang) * rr,
                              0.0,
                              -0.12 - mc.world.random.nextDouble() * 0.1,
                              0.0
                           );
                     }
                  }
               }
            } else {
               PlayerEntity p = mc.world.getPlayerByUuid(a.who);
               if (p == null || a.soul == null || a.cancelAt > 0L) {
                  return;
               }

               float t = ticksSince(a.start);
               int rgb = a.soul.getColor() & 16777215;
               Vector3f col = new Vector3f((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F);
               Vec3d chest = chestPos(p, a, 1.0F);
               if (t > 12.0F && t < 47.0F && mc.world.random.nextFloat() < 0.5F) {
                  mc.world
                     .addParticle(
                        new DustParticleEffect(col, 0.7F),
                        chest.x + (mc.world.random.nextDouble() - 0.5) * 0.3,
                        chest.y + (mc.world.random.nextDouble() - 0.5) * 0.3,
                        chest.z + (mc.world.random.nextDouble() - 0.5) * 0.3,
                        0.0,
                        0.02,
                        0.0
                     );
               }

               if (!a.pullFxDone && t >= 47.0F) {
                  a.pullFxDone = true;

                  for (int i = 0; i < 14; i++) {
                     mc.world
                        .addParticle(
                           new DustParticleEffect(col, 1.0F),
                           chest.x,
                           chest.y,
                           chest.z,
                           (mc.world.random.nextDouble() - 0.5) * 0.25,
                           mc.world.random.nextDouble() * 0.12,
                           (mc.world.random.nextDouble() - 0.5) * 0.25
                        );
                  }
               }
            }
         }
      }
   }

   private static ItemStack knifeStack() {
      if (knife == null && TurnBattleMod.DARK_FOUNTAIN_KNIFE != null) {
         knife = new ItemStack(TurnBattleMod.DARK_FOUNTAIN_KNIFE);
      }

      return knife != null ? knife : ItemStack.EMPTY;
   }

   private static ItemStack soulStack(SoulType s) {
      if (s == null) {
         return ItemStack.EMPTY;
      } else {
         if (s != cachedSoul) {
            SoulItem item = TurnBattleMod.SOUL_ITEMS.get(s);
            cachedSoulStack = item != null ? new ItemStack(item) : ItemStack.EMPTY;
            cachedSoul = s;
         }

         return cachedSoulStack;
      }
   }

   static PlayerActions.ActFrame frameFor(PlayerEntity e) {
      if (e == null) {
         return null;
      } else {
         PlayerActions.Act a = acts.get(e.getUuid());
         if (a == null) {
            return null;
         } else {
            CloneAnimator.Pose p = new CloneAnimator.Pose();
            ItemStack hand;
            float w;
            if (a.kind == PlayerActions.Kind.FOUNTAIN) {
               poseFountain(p, a);
               hand = knifeStack();
               w = CloneAnimator.clamp01(ticksSince(a.start) / 3.0F);
            } else if (a.kind == PlayerActions.Kind.MENU) {
               poseMenu(p, ticksSince(a.start));
               hand = null;
               w = 1.0F;
            } else if (a.kind == PlayerActions.Kind.LIGHT) {
               poseBrace(p, ticksSince(a.start));
               hand = ItemStack.EMPTY;
               w = CloneAnimator.clamp01(ticksSince(a.start) / 6.0F);
            } else {
               float t = ticksSince(a.start);
               boolean out = a.cancelAt < 0L && t >= 47.0F;
               poseSoul(p, a, t);
               hand = out ? soulStack(a.soul) : ItemStack.EMPTY;
               w = CloneAnimator.clamp01(t / 2.5F);
               if (a.cancelAt > 0L) {
                  w *= 1.0F - CloneAnimator.clamp01(ticksSince(a.cancelAt) / 6.0F);
               }

               if (a.finishAt > 0L) {
                  w *= 1.0F - CloneAnimator.clamp01((ticksSince(a.finishAt) - 16.0F) / 6.0F);
               }
            }

            if (a.endAt > 0L) {
               w *= 1.0F - CloneAnimator.clamp01(ticksSince(a.endAt) / 6.0F);
            }

            if (w < 0.999F) {
               fade(p, w);
            }

            if (w < 0.25F) {
               hand = null;
            }

            float yaw = a.yaw;
            if (a.kind == PlayerActions.Kind.LIGHT) {
               float turn = CloneAnimator.clamp01((ticksSince(a.start) - 2.0F) / 12.0F);
               yaw += 180.0F * turn * turn * (3.0F - 2.0F * turn);
            }

            return new PlayerActions.ActFrame(p, hand, yaw);
         }
      }
   }

   private static void fade(CloneAnimator.Pose p, float w) {
      p.armW *= w;
      p.headW *= w;
      p.legW *= w;
      p.bodyW *= w;
      p.dx *= w;
      p.dy *= w;
      p.tilt *= w;
      p.sx = CloneAnimator.mix(1.0F, p.sx, w);
      p.sy = CloneAnimator.mix(1.0F, p.sy, w);
   }

   private static void poseFountain(CloneAnimator.Pose o, PlayerActions.Act a) {
      float t = ticksSince(a.start);
      o.armW = 1.0F;
      o.headW = 1.0F;
      float raise = CloneAnimator.easeOutCubic(CloneAnimator.clamp01(t / 12.0F));
      float trem = CloneAnimator.clamp01((t - 12.0F) / 6.0F);
      float tremble = 0.035F * MathHelper.sin(t * 1.9F) * trem;
      float rP = CloneAnimator.mix(0.0F, -2.1F, raise) + tremble;
      float lP = CloneAnimator.mix(0.0F, -1.64F, raise) + tremble;
      float rY = -0.75F * raise;
      float lY = 0.82F * raise;
      float rR = 0.0F;
      float lR = 0.12F * raise;
      float headP = -0.45F * raise;
      float tilt = -4.0F * raise;
      float dy = -1.2F * raise * (0.5F + 0.5F * MathHelper.sin(t * 0.35F) * trem);
      float sy = 1.0F + 0.02F * raise;
      float legP = 0.0F;
      float legBack = 0.0F;
      float legW = 0.0F;
      if (a.plungeAt > 0L) {
         float u = ticksSince(a.plungeAt);
         float antic = CloneAnimator.clamp01(u / 1.5F) * (1.0F - CloneAnimator.clamp01((u - 1.5F) / 1.0F));
         rP -= 0.45F * antic;
         lP -= 0.45F * antic;
         headP -= 0.15F * antic;
         float q = CloneAnimator.clamp01((u - 1.5F) / 3.5F);
         float k = q * q * q;
         rP = CloneAnimator.mix(rP, -2.1F, k);
         lP = CloneAnimator.mix(lP, -2.12F, k);
         rY = CloneAnimator.mix(rY, -0.75F, k);
         lY = CloneAnimator.mix(lY, 0.92F, k);
         lR = CloneAnimator.mix(lR, 0.18F, k);
         headP = CloneAnimator.mix(headP, 0.5F, k);
         tilt = CloneAnimator.mix(tilt, 30.0F, k);
         dy = CloneAnimator.mix(dy, 12.0F, k);
         sy = CloneAnimator.mix(sy, 1.0F, k);
         legP = -1.0F;
         legBack = 0.9F;
         legW = k;
         float after = u - 5.0F;
         if (after > 0.0F) {
            float imp = 1.0F - CloneAnimator.clamp01(after / 4.0F);
            sy *= 1.0F - 0.07F * imp;
            dy += 1.5F * imp;
            headP = CloneAnimator.mix(0.5F, -0.55F, CloneAnimator.easeOutCubic(CloneAnimator.clamp01((after - 6.0F) / 10.0F)));
            float breath = MathHelper.sin(after * 0.18F) * CloneAnimator.clamp01((after - 4.0F) / 6.0F);
            rP += 0.02F * breath;
            lP += 0.02F * breath;
            dy += 0.4F * breath;
         }
      }

      o.rArmP = rP;
      o.lArmP = lP;
      o.rArmY = rY;
      o.lArmY = lY;
      o.rArmR = rR;
      o.lArmR = lR;
      o.headP = headP;
      o.tilt = tilt;
      o.dy = dy;
      o.sy = sy;
      o.legW = legW;
      o.rLegP = legP;
      o.lLegP = legBack;
   }

   public static float gripFlip(LivingEntity e) {
      if (e instanceof PlayerEntity p) {
         PlayerActions.Act a = acts.get(p.getUuid());
         if (a != null && a.kind == PlayerActions.Kind.FOUNTAIN && a.plungeAt >= 0L) {
            float q = CloneAnimator.clamp01(ticksSince(a.plungeAt) / 5.0F);
            float f = q * q * (3.0F - 2.0F * q) * (float) Math.PI;
            if (a.endAt > 0L) {
               f *= 1.0F - CloneAnimator.clamp01(ticksSince(a.endAt) / 6.0F);
            }

            return f;
         } else {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   private static void poseMenu(CloneAnimator.Pose o, float t) {
      float k = CloneAnimator.easeOutCubic(CloneAnimator.clamp01(t / 8.0F));
      float br = MathHelper.sin(t * 0.14F);
      float swipe = (float)Math.max(0.0, Math.sin((double)(t * 0.09F))) * (float)Math.sin((double)(t * 0.6F));
      o.armW = 1.0F;
      o.headW = 1.0F;
      o.bodyW = 0.7F;
      o.lArmP = -1.3F * k + 0.04F * swipe * k + 0.015F * br;
      o.lArmY = -0.4F * k + 0.05F * swipe * k;
      o.lArmR = -0.12F * k;
      o.rArmP = -0.08F * k;
      o.rArmY = 0.05F * k;
      o.rArmR = 0.08F * k;
      o.headY = -0.55F * k + 0.05F * MathHelper.sin(t * 0.05F) * k;
      o.headP = 0.12F * k + 0.01F * br;
      o.bodyY = -0.18F * k;
      o.sy = 1.0F + 0.006F * br;
   }

   private static void poseBrace(CloneAnimator.Pose o, float t) {
      float k = CloneAnimator.easeOutCubic(CloneAnimator.clamp01(t / 10.0F));
      float lift = CloneAnimator.clamp01((t - 12.0F) / 20.0F);
      float tr = (0.012F + 0.02F * lift) * MathHelper.sin(t * 2.9F);
      o.armW = 1.0F;
      o.headW = 1.0F;
      o.legW = 1.0F;
      o.bodyW = 1.0F;
      o.rArmP = -1.42F * k + tr;
      o.rArmY = -0.62F * k;
      o.rArmR = 0.05F * k;
      o.lArmP = -1.3F * k - tr;
      o.lArmY = 0.66F * k;
      o.lArmR = -0.05F * k;
      o.headP = 0.48F * k;
      o.headY = 0.0F;
      o.rLegP = CloneAnimator.mix(0.0F, -0.62F, k * (0.6F + 0.4F * lift));
      o.lLegP = CloneAnimator.mix(0.0F, -0.12F, k) + 0.06F * lift * MathHelper.sin(t * 0.5F);
      o.tilt = 7.0F * k;
      o.bodyY = 0.04F * MathHelper.sin(t * 0.2F) * lift;
   }

   private static void poseSoul(CloneAnimator.Pose o, PlayerActions.Act a, float t) {
      if (a.cancelAt > 0L) {
         t = Math.min(t, ticksSince(a.start) - ticksSince(a.cancelAt));
      }

      o.armW = 1.0F;
      o.headW = 1.0F;
      float reach = CloneAnimator.easeOutCubic(CloneAnimator.clamp01(t / 14.0F));
      float push = CloneAnimator.easeOutCubic(CloneAnimator.clamp01((t - 8.0F) / 8.0F));
      float fish = CloneAnimator.clamp01((t - 14.0F) / 4.0F) * (1.0F - CloneAnimator.clamp01((t - 42.0F) / 4.0F));
      float pull = CloneAnimator.clamp01((t - 44.0F) / 12.0F);
      float pullE = 1.0F - (1.0F - pull) * (1.0F - pull);
      float show = CloneAnimator.clamp01((t - 56.0F) / 8.0F);
      float rP = 0.2F * reach + 0.45F * push;
      float rY = -0.35F * reach;
      float rR = -1.2F * reach;
      float pivZ = -4.0F * reach;
      float lP = -0.35F * reach;
      float lR = -0.32F * reach;
      float headP = 0.6F * reach;
      float tilt = 7.0F * reach;
      float dx = 0.0F;
      float sx = 1.0F;
      float sy = 1.0F;
      rR += 0.07F * MathHelper.sin(t * 1.3F) * fish;
      rY += 0.08F * MathHelper.sin(t * 2.1F) * fish;
      rP += 0.04F * MathHelper.sin(t * 1.7F) * fish;
      pivZ += 0.4F * MathHelper.sin(t * 2.6F) * fish;
      lR += -0.12F * MathHelper.sin(t * 1.9F) * fish;
      headP += 0.08F * MathHelper.sin(t * 2.6F) * fish;
      tilt += 2.2F * MathHelper.sin(t * 1.1F) * fish;
      dx += 0.6F * MathHelper.sin(t * 3.7F) * fish;
      sy -= 0.015F * fish * (0.5F + 0.5F * MathHelper.sin(t * 0.9F));
      rP = CloneAnimator.mix(rP, -1.58F, pullE);
      rY = CloneAnimator.mix(rY, -0.12F, pullE);
      rR = CloneAnimator.mix(rR, 0.0F, pullE);
      pivZ = CloneAnimator.mix(pivZ, 0.0F, pullE);
      headP = CloneAnimator.mix(headP, -0.12F, pullE);
      tilt = CloneAnimator.mix(tilt, -9.0F, pullE);
      dx = CloneAnimator.mix(dx, -2.2F, pullE);
      lP = CloneAnimator.mix(lP, -0.15F, pullE);
      lR = CloneAnimator.mix(lR, -0.12F, pullE);
      float yank = pull * (1.0F - pull) * 4.0F;
      sy *= 1.0F + 0.03F * yank;
      rP = CloneAnimator.mix(rP, -1.75F, CloneAnimator.easeOutCubic(show)) + 0.03F * MathHelper.sin(t * 0.4F) * show;
      headP = CloneAnimator.mix(headP, -0.25F, show);
      tilt = CloneAnimator.mix(tilt, -5.0F, show);
      dx = CloneAnimator.mix(dx, -1.2F, show);
      o.rArmP = rP;
      o.rArmY = rY;
      o.rArmR = rR;
      o.rArmPZ = pivZ;
      o.lArmP = lP;
      o.lArmY = 0.0F;
      o.lArmR = lR;
      o.headP = headP;
      o.tilt = tilt;
      o.dx = dx;
      o.sx = sx;
      o.sy = sy;
   }

   public static boolean cameraActive() {
      return camOn && !OverhaulView.cameraActive();
   }

   private static void tickCamera(MinecraftClient mc) {
      prevCamW = camW;
      prevCamGoal = camGoal;
      prevLookGoal = lookGoal;
      if (shake > 0.0F) {
         shake = Math.max(0.0F, shake - 0.15F);
      }

      PlayerEntity me = mc.player;
      PlayerActions.Act a = me != null ? acts.get(me.getUuid()) : null;
      boolean want = a != null
         && a.endAt < 0L
         && a.cancelAt < 0L
         && (a.kind != PlayerActions.Kind.SOUL || a.finishAt <= 0L || !(ticksSince(a.finishAt) > 16.0F));
      camW = want ? Math.min(1.0F, camW + 0.12F) : Math.max(0.0F, camW - 0.16F);
      if (camW <= 0.0F) {
         camOn = false;
         menuHud(mc);
      } else if (me == null) {
         camOn = false;
         camW = 0.0F;
      } else if (a == null) {
         prevCamGoal = camGoal;
         prevLookGoal = lookGoal;
      } else {
         float yaw = a.yaw;
         float yr = yaw * (float) (Math.PI / 180.0);
         Vec3d fwd = new Vec3d((double)(-MathHelper.sin(yr)), 0.0, (double)MathHelper.cos(yr));
         Vec3d right = new Vec3d(-fwd.z, 0.0, fwd.x);
         Vec3d pos = me.getPos();
         shotKind = a.kind;
         Vec3d goal;
         Vec3d look;
         if (a.kind == PlayerActions.Kind.MENU) {
            Vec3d left = right.multiply(-1.0);
            goal = pos.add(fwd.multiply(2.4)).add(left.multiply(0.55)).add(0.0, 1.55, 0.0);
            look = pos.add(0.0, 1.4, 0.0).add(left.multiply(1.05));
         } else if (a.kind == PlayerActions.Kind.FOUNTAIN) {
            float beamT = a.plungeAt > 0L ? CloneAnimator.clamp01((ticksSince(a.plungeAt) - 5.0F) / 14.0F) : 0.0F;
            goal = pos.add(fwd.multiply((double)CloneAnimator.mix(3.3F, 4.2F, beamT)))
               .add(right.multiply((double)CloneAnimator.mix(1.9F, 2.5F, beamT)))
               .add(0.0, (double)CloneAnimator.mix(1.5F, 1.7F, beamT), 0.0);
            Vec3d beamMid = a.beam.add(0.0, Math.min(2.6, beamTopOf(a, mc) - a.beam.y) * 0.55, 0.0);
            look = pos.add(0.0, 1.15, 0.0).add(fwd.multiply(0.4)).lerp(beamMid, (double)beamT * 0.6);
         } else if (a.kind == PlayerActions.Kind.LIGHT) {
            float k = CloneAnimator.easeOutCubic(CloneAnimator.clamp01(ticksSince(a.start) / 26.0F));
            double risen = Math.max(0.0, pos.y - a.beam.y);
            Vec3d dir = Vec3d.fromPolar(me.getPitch(), me.getYaw());
            Vec3d eye = new Vec3d(pos.x, a.beam.y + (double)me.getStandingEyeHeight() + risen * 0.85, pos.z);
            goal = eye.subtract(dir.multiply((double)CloneAnimator.mix(0.0F, 15.0F, k)));
            look = goal.add(dir.multiply(1000.0));
         } else {
            goal = pos.add(fwd.multiply(2.2)).add(right.multiply(0.75)).add(0.0, 1.55, 0.0);
            look = pos.add(0.0, 1.2, 0.0);
         }

         Vec3d from = pos.add(0.0, 1.3, 0.0);
         BlockHitResult hit = mc.world.raycast(new RaycastContext(from, goal, ShapeType.COLLIDER, FluidHandling.NONE, me));
         if (hit.getType() == Type.BLOCK) {
            Vec3d d = goal.subtract(from);
            double len = d.length();
            if (len > 0.001) {
               goal = hit.getPos().subtract(d.multiply(0.3 / len));
            }
         }

         if (!camOn) {
            prevCamGoal = goal;
            prevLookGoal = look;
         }

         camGoal = goal;
         lookGoal = look;
         camOn = true;
         menuHud(mc);
         if (a.kind == PlayerActions.Kind.FOUNTAIN && !a.engulfed && a.plungeAt > 0L) {
            BlockPos c = BlockPos.ofFloored(goal);

            for (int dx = -1; dx <= 1; dx++) {
               for (int dy = -1; dy <= 1; dy++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     if (mc.world.getBlockState(c.add(dx, dy, dz)).isOf(TurnBattleMod.DARK_BLOCK)) {
                        a.engulfed = true;
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private static void menuHud(MinecraftClient mc) {
      boolean hide = camOn && shotKind == PlayerActions.Kind.MENU;
      if (hide && !hudSaved) {
         savedHudHidden = mc.options.hudHidden;
         mc.options.hudHidden = true;
         hudSaved = true;
      } else if (!hide && hudSaved) {
         mc.options.hudHidden = savedHudHidden;
         hudSaved = false;
      }
   }

   static float[] menuAnchor(int sw, int sh, float menuW, float menuH, float baseScale) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (cameraActive() && shotKind == PlayerActions.Kind.MENU && mc.player != null) {
         float td = mc.getRenderTickCounter().getTickDelta(false);
         float w = easedW(td);
         float[] p = OverhaulView.project(mc.player.getLerpedPos(td).add(0.0, 1.55, 0.0));
         if (p == null) {
            return null;
         } else {
            float target = Math.min(1.0F, Math.min(0.72F * (float)sh / menuH, 0.62F * (float)sw / menuW));
            float mw = menuW * target;
            float mh = menuH * target;
            float cx = p[0] + 0.45F * p[2] + 12.0F + mw / 2.0F;
            cx = MathHelper.clamp(cx, mw / 2.0F + 6.0F, (float)sw - mw / 2.0F - 6.0F);
            float cy = MathHelper.clamp(p[1] + 0.15F * p[2], mh / 2.0F + 6.0F, (float)sh - mh / 2.0F - 6.0F);
            return new float[]{
               CloneAnimator.mix((float)sw / 2.0F, cx, w), CloneAnimator.mix((float)sh / 2.0F, cy, w), CloneAnimator.mix(baseScale, target, w), w
            };
         }
      } else {
         return null;
      }
   }

   static float menuReveal() {
      MinecraftClient mc = MinecraftClient.getInstance();
      float td = mc.getRenderTickCounter().getTickDelta(false);
      if (mc.player != null && !OverhaulView.cameraActive()) {
         PlayerActions.Act a = acts.get(mc.player.getUuid());
         if (a == null || a.kind != PlayerActions.Kind.MENU || a.endAt >= 0L) {
            return -1.0F;
         } else if (camOn && shotKind != PlayerActions.Kind.MENU) {
            return -1.0F;
         } else {
            return camOn ? easedW(td) : 0.0F;
         }
      } else {
         return -1.0F;
      }
   }

   private static void tickEngulf(MinecraftClient mc) {
      prevEngulf = engulf;
      PlayerActions.Act a = mc.player != null ? acts.get(mc.player.getUuid()) : null;
      boolean on = a != null && a.kind == PlayerActions.Kind.FOUNTAIN && a.engulfed && a.endAt < 0L;
      engulf = on ? Math.min(1.0F, engulf + 0.34F) : Math.max(0.0F, engulf - 0.2F);
   }

   static void renderHud(DrawContext ctx, float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      float fl = 1.0F - (float)(System.nanoTime() - flashAt) / 4.5E8F;
      if (flashAt != 0L && fl > 0.0F) {
         ctx.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), Math.round(200.0F * fl) << 24 | 16777215);
      }

      float e = CloneAnimator.mix(prevEngulf, engulf, td);
      if (!(e <= 0.003F)) {
         int a = Math.round(255.0F * CloneAnimator.clamp01(e));
         ctx.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), a << 24);
      }
   }

   private static float easedW(float td) {
      float w = CloneAnimator.mix(prevCamW, camW, td);
      return w * w * (3.0F - 2.0F * w);
   }

   public static Vec3d cameraPos(float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Vec3d eye = mc.player != null ? mc.player.getCameraPosVec(td) : camGoal;
      Vec3d p = eye.lerp(prevCamGoal.lerp(camGoal, (double)td), (double)easedW(td));
      if (shake > 0.0F) {
         double s = (double)(shake * shake) * 0.06;
         double n = (double)now() / 1.0E7;
         p = p.add(Math.sin(n * 1.7) * s, Math.sin(n * 2.3) * s, Math.cos(n * 1.9) * s);
      }

      return p;
   }

   public static float cameraYaw(float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      float own = mc.player != null ? mc.player.getYaw(td) : 0.0F;
      float[] la = lookAngles(cameraPos(td), prevLookGoal.lerp(lookGoal, (double)td));
      return own + MathHelper.wrapDegrees(la[0] - own) * easedW(td);
   }

   public static float cameraPitch(float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      float own = mc.player != null ? mc.player.getPitch(td) : 0.0F;
      float[] la = lookAngles(cameraPos(td), prevLookGoal.lerp(lookGoal, (double)td));
      return CloneAnimator.mix(own, la[1], easedW(td));
   }

   private static float[] lookAngles(Vec3d from, Vec3d to) {
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double h = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(MathHelper.atan2(dy, h) * (180.0 / Math.PI)));
      return new float[]{yaw, pitch};
   }

   private static double beamTopOf(PlayerActions.Act a, MinecraftClient mc) {
      if (Double.isNaN(a.beamTop) && mc.world != null) {
         Vec3d from = a.beam.add(0.0, 0.2, 0.0);
         Vec3d to = a.beam.add(0.0, 24.0, 0.0);
         BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         a.beamTop = hit.getType() == Type.BLOCK ? hit.getPos().y : to.y;
      }

      return Double.isNaN(a.beamTop) ? a.beam.y + 12.0 : a.beamTop;
   }

   private static Vec3d chestPos(PlayerEntity p, PlayerActions.Act a, float td) {
      float yr = a.yaw * (float) (Math.PI / 180.0);
      return p.getLerpedPos(td).add((double)(-MathHelper.sin(yr)) * 0.34, 1.12, (double)MathHelper.cos(yr) * 0.34);
   }

   static void renderWorld(WorldRenderContext ctx) {
      if (!acts.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            VertexConsumerProvider consumers = mc.getBufferBuilders().getEntityVertexConsumers();
            Vec3d cam = ctx.camera().getPos();
            float td = mc.getRenderTickCounter().getTickDelta(false);
            MatrixStack ms = new MatrixStack();

            for (PlayerActions.Act a : acts.values()) {
               if (a.kind == PlayerActions.Kind.FOUNTAIN) {
                  renderBeam(ctx, consumers, ms, cam, a, mc);
               } else if (a.kind == PlayerActions.Kind.SOUL) {
                  renderSoulGlow(ctx, consumers, ms, cam, a, mc, td);
               }
            }
         }
      }
   }

   private static void renderBeam(WorldRenderContext ctx, VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, PlayerActions.Act a, MinecraftClient mc) {
      if (a.plungeAt >= 0L) {
         float u = ticksSince(a.plungeAt) - 5.0F;
         if (!(u < 0.0F)) {
            float end = a.endAt > 0L ? 1.0F - CloneAnimator.clamp01(ticksSince(a.endAt) / 6.0F) : 1.0F;
            if (!(end <= 0.0F)) {
               Vec3d b = a.beam;
               double top = beamTopOf(a, mc);
               double h = Math.max(1.0, top - b.y);
               boolean ceiling = !Double.isNaN(a.beamTop) && a.beamTop < b.y + 23.5;
               float grow = CloneAnimator.easeOutCubic(CloneAnimator.clamp01(u / 4.0F));
               double y1 = b.y + h * (double)grow;
               float burst = 1.0F + 0.55F * (1.0F - CloneAnimator.clamp01(u / 7.0F));
               float r = 0.85F * burst * (1.0F + 0.025F * MathHelper.sin(u * 0.9F));
               float front = darkFront(u);
               double frontY = b.y + h * (double)front;
               float white = 1.0F - front;
               flat(vc, ms, cam, TEX_CRACK, b.add(0.0, 0.015, 0.0), 2.6F, 1.0F, 1.0F, 1.0F, end);
               flat(vc, ms, cam, TEX_GLOW, b.add(0.0, 0.02, 0.0), 3.4F * burst, 1.0F, 1.0F, 1.0F, 0.85F * white * end);
               float pool = CloneAnimator.clamp01((u - 22.0F) / 30.0F);
               if (pool > 0.0F) {
                  flat(vc, ms, cam, TEX_GLOW, b.add(0.0, 0.025, 0.0), 1.2F + 4.2F * CloneAnimator.easeOutCubic(pool), 0.0F, 0.0F, 0.0F, 0.97F * end);
               }

               if (white > 0.01F) {
                  prism(vc, ms, cam, b, b.y, y1, r * 1.5F, 1.0F, 1.0F, 1.0F, 0.28F * white * end);
               }

               int seg = 28;

               for (int i = 0; i < seg; i++) {
                  double ya = b.y + h * (double)grow * (double)i / (double)seg;
                  double yb = b.y + h * (double)grow * (double)(i + 1) / (double)seg;
                  float k = CloneAnimator.clamp01((float)((frontY - (ya + yb) / 2.0) / 0.7 + 0.5));
                  float c = 1.0F - k;
                  prism(vc, ms, cam, b, ya, yb, r, c, c, c, end);
               }

               if (front > 0.0F) {
                  rim(vc, ms, cam, b, b.y, Math.min(y1, frontY), r, 0.08F, end);
               }

               if (ceiling && grow > 0.98F) {
                  Vec3d c = new Vec3d(b.x, top - 0.02, b.z);
                  float splash = CloneAnimator.easeOutCubic(CloneAnimator.clamp01((u - 3.0F) / 14.0F));
                  flat(vc, ms, cam, TEX_GLOW, c, 1.0F + 3.6F * splash, 1.0F, 1.0F, 1.0F, 0.9F * white * end);
                  float spread = CloneAnimator.clamp01((u - 22.0F - 24.0F) / 34.0F);
                  if (spread > 0.0F) {
                     flat(vc, ms, cam, TEX_GLOW, c.add(0.0, -0.005, 0.0), 2.0F + 16.0F * CloneAnimator.easeOutCubic(spread), 0.0F, 0.0F, 0.0F, 0.98F * end);
                  }
               }

               for (int i = 0; i < 22; i++) {
                  float speed = 0.01F + CloneAnimator.rnd(i * 5 + 1) * 0.012F;
                  float q = (CloneAnimator.rnd(i * 5 + 2) + u * speed) % 1.0F;
                  double y = b.y + 0.2 + (double)q * (h * (double)grow - 0.4);
                  float ang = CloneAnimator.rnd(i * 5 + 3) * 6.2832F + u * 0.02F;
                  float rad = r * (1.05F + 0.45F * CloneAnimator.rnd(i * 5 + 4));
                  float tw = 0.5F + 0.5F * MathHelper.sin(u * 0.45F + (float)i * 1.7F);
                  float size = 0.08F + 0.14F * tw;
                  float life = MathHelper.sin((float) Math.PI * q);
                  star(
                     ctx,
                     vc,
                     ms,
                     cam,
                     new Vec3d(b.x + (double)(MathHelper.cos(ang) * rad), y, b.z + (double)(MathHelper.sin(ang) * rad)),
                     size,
                     0.95F * life * end
                  );
               }
            }
         }
      }
   }

   private static float darkFront(float u) {
      float q = CloneAnimator.clamp01((u - 22.0F) / 24.0F);
      return q * q * (3.0F - 2.0F * q);
   }

   private static void prism(
      VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Vec3d base, double y0, double y1, float radius, float r, float g, float b, float alpha
   ) {
      if (!(alpha <= 0.003F) && !(y1 <= y0)) {
         int n = 12;
         ms.push();
         ms.translate(base.x - cam.x, 0.0, base.z - cam.z);
         Matrix4f m = ms.peek().getPositionMatrix();
         float ya = (float)(y0 - cam.y);
         float yb = (float)(y1 - cam.y);
         VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_WHITE));

         for (int i = 0; i < n; i++) {
            float a0 = (float)((double)i * Math.PI * 2.0 / (double)n);
            float a1 = (float)((double)(i + 1) * Math.PI * 2.0 / (double)n);
            float x0 = MathHelper.cos(a0) * radius;
            float z0 = MathHelper.sin(a0) * radius;
            float x1 = MathHelper.cos(a1) * radius;
            float z1 = MathHelper.sin(a1) * radius;
            vert(buf, m, x0, ya, z0, 0.0F, 1.0F, r, g, b, alpha);
            vert(buf, m, x1, ya, z1, 1.0F, 1.0F, r, g, b, alpha);
            vert(buf, m, x1, yb, z1, 1.0F, 0.0F, r, g, b, alpha);
            vert(buf, m, x0, yb, z0, 0.0F, 0.0F, r, g, b, alpha);
         }

         ms.pop();
      }
   }

   private static void rim(VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Vec3d base, double y0, double y1, float radius, float w, float alpha) {
      if (!(y1 <= y0)) {
         double dx = base.x - cam.x;
         double dz = base.z - cam.z;
         double len = Math.sqrt(dx * dx + dz * dz);
         if (!(len < 0.001)) {
            float sx = (float)(-dz / len);
            float sz = (float)(dx / len);
            float tx = (float)(-dx / len);
            float tz = (float)(-dz / len);
            ms.push();
            ms.translate(base.x - cam.x, 0.0, base.z - cam.z);
            Matrix4f m = ms.peek().getPositionMatrix();
            float ya = (float)(y0 - cam.y);
            float yb = (float)(y1 - cam.y);
            VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_WHITE));

            for (int side = -1; side <= 1; side += 2) {
               float cx = sx * radius * (float)side + tx * 0.05F;
               float cz = sz * radius * (float)side + tz * 0.05F;
               float ox = sx * w * (float)side;
               float oz = sz * w * (float)side;
               vert(buf, m, cx - ox * 0.5F, ya, cz - oz * 0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha);
               vert(buf, m, cx + ox, ya, cz + oz, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha);
               vert(buf, m, cx + ox, yb, cz + oz, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha);
               vert(buf, m, cx - ox * 0.5F, yb, cz - oz * 0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha);
            }

            ms.pop();
         }
      }
   }

   private static void star(WorldRenderContext ctx, VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Vec3d c, float size, float alpha) {
      if (!(alpha <= 0.01F)) {
         ms.push();
         ms.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
         ms.multiply(ctx.camera().getRotation());
         Matrix4f m = ms.peek().getPositionMatrix();
         VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX_WHITE));
         float t = size * 0.18F;
         vert(buf, m, -size, -t, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha);
         vert(buf, m, size, -t, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha);
         vert(buf, m, size, t, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha);
         vert(buf, m, -size, t, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha);
         vert(buf, m, -t, -size, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha);
         vert(buf, m, t, -size, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha);
         vert(buf, m, t, size, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha);
         vert(buf, m, -t, size, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha);
         ms.pop();
      }
   }

   private static void renderSoulGlow(
      WorldRenderContext ctx, VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, PlayerActions.Act a, MinecraftClient mc, float td
   ) {
      if (a.soul != null) {
         PlayerEntity p = mc.world.getPlayerByUuid(a.who);
         if (p != null) {
            float t = ticksSince(a.start);
            float glow = CloneAnimator.clamp01((t - 10.0F) / 10.0F) * (1.0F - CloneAnimator.clamp01((t - 47.0F - 4.0F) / 5.0F));
            if (a.cancelAt > 0L) {
               glow *= 1.0F - CloneAnimator.clamp01(ticksSince(a.cancelAt) / 5.0F);
            }

            float flash = a.cancelAt < 0L && t >= 47.0F ? 1.0F - CloneAnimator.clamp01((t - 47.0F) / 7.0F) : 0.0F;
            if (!(glow <= 0.01F) || !(flash <= 0.01F)) {
               int rgb = a.soul.getColor() & 16777215;
               float r = (float)(rgb >> 16 & 0xFF) / 255.0F;
               float g = (float)(rgb >> 8 & 0xFF) / 255.0F;
               float b = (float)(rgb & 0xFF) / 255.0F;
               Vec3d chest = chestPos(p, a, td);
               float pulse = 0.75F + 0.25F * MathHelper.sin(t * 0.8F);
               billboard(ctx, vc, ms, cam, TEX_GLOW, chest, 0.55F * pulse + 1.4F * flash, r, g, b, Math.min(1.0F, 0.55F * glow * pulse + 0.9F * flash));
               billboard(ctx, vc, ms, cam, TEX_GLOW, chest, 0.22F + 0.5F * flash, 1.0F, 1.0F, 1.0F, Math.min(1.0F, 0.5F * glow + flash));
            }
         }
      }
   }

   private static void cylinder(
      WorldRenderContext ctx,
      VertexConsumerProvider vc,
      MatrixStack ms,
      Vec3d cam,
      Identifier tex,
      Vec3d base,
      double y0,
      double y1,
      float width,
      float r,
      float g,
      float b,
      float alpha
   ) {
      double dx = base.x - cam.x;
      double dz = base.z - cam.z;
      double len = Math.sqrt(dx * dx + dz * dz);
      float sx;
      float sz;
      if (len < 0.001) {
         sx = 1.0F;
         sz = 0.0F;
      } else {
         sx = (float)(-dz / len);
         sz = (float)(dx / len);
      }

      float hw = width / 2.0F;
      ms.push();
      ms.translate(base.x - cam.x, 0.0, base.z - cam.z);
      Matrix4f m = ms.peek().getPositionMatrix();
      float ya = (float)(y0 - cam.y);
      float yb = (float)(y1 - cam.y);
      VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(tex));
      vert(buf, m, -sx * hw, ya, -sz * hw, 0.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, sx * hw, ya, sz * hw, 1.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, sx * hw, yb, sz * hw, 1.0F, 0.0F, r, g, b, alpha);
      vert(buf, m, -sx * hw, yb, -sz * hw, 0.0F, 0.0F, r, g, b, alpha);
      ms.pop();
   }

   private static void flat(VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Identifier tex, Vec3d c, float size, float r, float g, float b, float alpha) {
      float h = size / 2.0F;
      ms.push();
      ms.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
      Matrix4f m = ms.peek().getPositionMatrix();
      VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(tex));
      vert(buf, m, -h, 0.0F, -h, 0.0F, 0.0F, r, g, b, alpha);
      vert(buf, m, -h, 0.0F, h, 0.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, h, 0.0F, h, 1.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, h, 0.0F, -h, 1.0F, 0.0F, r, g, b, alpha);
      ms.pop();
   }

   private static void billboard(
      WorldRenderContext ctx, VertexConsumerProvider vc, MatrixStack ms, Vec3d cam, Identifier tex, Vec3d c, float size, float r, float g, float b, float alpha
   ) {
      float h = size / 2.0F;
      ms.push();
      ms.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
      ms.multiply(ctx.camera().getRotation());
      Matrix4f m = ms.peek().getPositionMatrix();
      VertexConsumer buf = vc.getBuffer(RenderLayer.getEntityTranslucentEmissive(tex));
      vert(buf, m, -h, -h, 0.0F, 0.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, h, -h, 0.0F, 1.0F, 1.0F, r, g, b, alpha);
      vert(buf, m, h, h, 0.0F, 1.0F, 0.0F, r, g, b, alpha);
      vert(buf, m, -h, h, 0.0F, 0.0F, 0.0F, r, g, b, alpha);
      ms.pop();
   }

   private static void vert(VertexConsumer buf, Matrix4f m, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
      buf.vertex(m, x, y, z).color(r, g, b, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(0.0F, 1.0F, 0.0F);
   }

   private static final class Act {
      final PlayerActions.Kind kind;
      final UUID who;
      final float yaw;
      final long start = PlayerActions.now();
      final World world = MinecraftClient.getInstance().world;
      long plungeAt = -1L;
      long cancelAt = -1L;
      long finishAt = -1L;
      long endAt = -1L;
      Vec3d beam;
      double beamTop = Double.NaN;
      SoulType soul;
      boolean impactFxDone;
      boolean pullFxDone;
      boolean engulfed;
      SoundInstance sound;

      Act(PlayerActions.Kind kind, UUID who, float yaw) {
         this.kind = kind;
         this.who = who;
         this.yaw = yaw;
      }
   }

   static record ActFrame(CloneAnimator.Pose pose, ItemStack hand, float yaw) {
   }

   static enum Kind {
      FOUNTAIN,
      SOUL,
      MENU,
      LIGHT;
   }
}
