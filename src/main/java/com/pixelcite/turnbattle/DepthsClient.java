package com.pixelcite.turnbattle;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.Input;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@Environment(EnvType.CLIENT)
public final class DepthsClient {
   private static boolean active;
   private static int floor;
   private static boolean prologue;
   private static boolean hasLayout;
   private static double lightX;
   private static double lightZ;
   private static double darkX = Double.NaN;
   private static double darkZ = Double.NaN;
   private static boolean bossFloor;
   public static int bestFloor;
   public static int runs;
   private static long introNs = -1L;
   private static boolean introBoxShown;
   private static long sinkNs = -1L;
   private static boolean blackHold;
   private static final Identifier FLOW = Identifier.of("turnbattle", "textures/effects/fountain_flow.png");
   private static SoulType carried;
   private static long vanishNs = -1L;
   private static boolean vanishNoise2;
   private static final float V_BLACK = 2.5F;
   private static final float V_STARE = 3.6F;
   private static final float V_PICK = 4.1F;
   private static final float V_LANTERN_BACK = 3.85F;
   private static boolean confirmWasDown;
   private static boolean freeLastTick;
   private static long lastInteractNs;
   private static Perspective perspectiveBefore;
   private static int hintAt;
   private static final double CAM_UP = 10.5;
   private static final double CAM_BACK = 4.6;
   private static Vec3d camPos;
   private static long camNs;
   private static final Map<Integer, ArrayDeque<DepthsClient.Past>> history = new HashMap<>();
   private static final int KEEP = 10;
   private static final float HEIGHT = 30.0F;
   private static final float[] RINGS = new float[]{2.3F, 1.8F, 1.3F};
   private static final float[] POOL = new float[]{2.9F, 2.4F, 1.85F};
   private static final int SEG = 24;
   private static float[] verts = new float[114688];
   private static int count;
   private static float cr;
   private static float cg;
   private static float cb;

   private DepthsClient() {
   }

   public static boolean inRun() {
      return active;
   }

   public static SoulType carriedSoul() {
      return carried;
   }

   public static boolean isDepthsWorld(World w) {
      return w != null && w.getRegistryKey().equals(DepthsRun.WORLD);
   }

   private static boolean here() {
      return isDepthsWorld(MinecraftClient.getInstance().world);
   }

   public static boolean controlling() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return active && here() && mc.player != null;
   }

   public static boolean isOri(Entity e) {
      if (e instanceof AbstractClientPlayerEntity p && isDepthsWorld(p.getWorld())) {
         return true;
      }

      return false;
   }

   public static boolean inputLocked() {
      return introNs >= 0L || sinkNs >= 0L || vanishNs >= 0L || blackHold || DarkWorldFall.active() && DarkWorldFall.oriMode();
   }

   public static boolean lanternDown() {
      return vanishNs >= 0L && secs(vanishNs) < 3.85F;
   }

   public static boolean blocksMenu() {
      return active && inputLocked();
   }

   static void init() {
      Net121.registerClientReceiver(DepthsRun.STATE, (client, handler, buf, sender) -> {
         boolean on = buf.readBoolean();
         int f = 0;
         boolean pro = false;
         boolean lay = false;
         boolean dark = false;
         boolean boss = false;
         double lx = 0.0;
         double lz = 0.0;
         double dx = Double.NaN;
         double dz = Double.NaN;
         if (on) {
            f = buf.readVarInt();
            pro = buf.readBoolean();
            lay = buf.readBoolean();
            if (lay) {
               lx = buf.readDouble();
               lz = buf.readDouble();
               dark = buf.readBoolean();
               if (dark) {
                  dx = buf.readDouble();
                  dz = buf.readDouble();
               }

               boss = buf.readBoolean();
            }
         }

         int best = buf.readVarInt();
         int r = buf.readVarInt();
         int soulPlus = buf.isReadable() ? buf.readVarInt() : 0;
         int ff = f;
         boolean fp = pro;
         boolean fl = lay;
         boolean fb = boss;
         double flx = lx;
         double flz = lz;
         double fdx = dx;
         double fdz = dz;
         client.execute(() -> {
            boolean was = active;
            active = on;
            floor = ff;
            prologue = fp;
            hasLayout = fl;
            lightX = flx;
            lightZ = flz;
            darkX = fdx;
            darkZ = fdz;
            bossFloor = fb;
            bestFloor = best;
            runs = r;
            SoulType[] st = SoulType.values();
            carried = soulPlus > 0 && soulPlus <= st.length ? st[soulPlus - 1] : null;
            if (!on) {
               introNs = -1L;
               sinkNs = -1L;
               vanishNs = -1L;
               blackHold = false;
               history.clear();
               if (was) {
                  restorePerspective(client);
               }
            }
         });
      });
      Net121.registerClientReceiver(DepthsRun.SCENE, (client, handler, buf, sender) -> {
         byte kind = buf.readByte();
         switch (kind) {
            case 1:
               client.execute(() -> {
                  introNs = System.nanoTime();
                  introBoxShown = false;
               });
               break;
            case 2:
               byte id = buf.readByte();
               int n = buf.readVarInt();
               String[] pages = new String[n];

               for (int i = 0; i < n; i++) {
                  pages[i] = buf.readString(4096);
               }

               client.execute(() -> client.setScreen(new TextboxScreen(pages, true, () -> {
                     if (id != 0) {
                        done(id);
                     }
                  })));
               break;
            case 3:
               int f = buf.readVarInt();
               client.execute(() -> {
                  blackHold = false;
                  sinkNs = -1L;
                  if (client.currentScreen instanceof TextboxScreen) {
                     client.setScreen(null);
                  }

                  DarkWorldFall.startDepths(f);
               });
               break;
            case 4:
               client.execute(DarkWorldFall::depthsReady);
               break;
            case 5:
               client.execute(() -> sinkNs = System.nanoTime());
               break;
            case 6:
               boolean on = buf.readBoolean();
               client.execute(() -> blackHold = on);
               break;
            case 7:
               client.execute(() -> {
                  if (client.currentScreen instanceof TextboxScreen) {
                     client.setScreen(null);
                  }

                  vanishNs = System.nanoTime();
                  vanishNoise2 = false;
                  TurnBattleSounds.playClient(TurnBattleSounds.CLIMB_NOISE, 1.0F, 1.0F);
               });
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> client.execute(() -> {
            active = false;
            introNs = -1L;
            sinkNs = -1L;
            vanishNs = -1L;
            blackHold = false;
            history.clear();
            restorePerspective(client);
         }));
      ClientTickEvents.END_CLIENT_TICK.register(DepthsClient::tick);
      HudRenderCallback.EVENT.register((HudRenderCallback)(ctx, tc) -> renderHud(ctx));
      WorldRenderEvents.AFTER_ENTITIES.register(DepthsClient::renderWorld);
      DimensionRenderingRegistry.registerSkyRenderer(DepthsRun.WORLD, DepthsClient::renderSky);
   }

   private static void done(byte id) {
      PacketByteBuf b = PacketByteBufs.create();
      b.writeByte(id);
      Net121.sendToServer(DepthsRun.DONE, b);
   }

   private static void tick(MinecraftClient mc) {
      if (mc.world != null && mc.player != null) {
         boolean ctl = controlling();
         if (ctl) {
            if (perspectiveBefore == null) {
               perspectiveBefore = mc.options.getPerspective();
            }

            if (mc.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
               mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
         } else if (perspectiveBefore != null && !active) {
            restorePerspective(mc);
         }

         if (introNs >= 0L && !introBoxShown && secs(introNs) > 2.9F && mc.currentScreen == null) {
            introBoxShown = true;
            mc.setScreen(new TextboxScreen(new String[]{"W-Where...\nWhere am I?"}, true, () -> {
               introNs = -1L;
               done((byte)1);
            }));
         }

         if (sinkNs >= 0L && secs(sinkNs) > 6.0F) {
            sinkNs = -1L;
         }

         if (vanishNs >= 0L) {
            float vt = secs(vanishNs);
            if (!vanishNoise2 && vt >= 2.0F) {
               vanishNoise2 = true;
               TurnBattleSounds.playClient(TurnBattleSounds.CLIMB_NOISE, 1.0F, 1.0F);
            }

            if (vt >= 4.2F) {
               vanishNs = -1L;
            }
         }

         if (active && RogueliteClient.hand != null && mc.currentScreen == null && !inputLockedForCards()) {
            mc.setScreen(new CardDrawScreen(RogueliteClient.hand, RogueliteClient.rerolls, RogueliteClient.handsAfter));
         }

         if (ctl) {
            boolean down = false;

            try {
               Key k = TurnBattleClientMod.boundKey(TurnBattleClientMod.battleConfirm);
               down = k.getCode() >= 0 && InputUtil.isKeyPressed(mc.getWindow().getHandle(), k.getCode());
            } catch (Throwable var6) {
            }

            boolean free = mc.currentScreen == null;
            if (down && !confirmWasDown && free && freeLastTick) {
               onInspect(mc);
            }

            confirmWasDown = down;
            freeLastTick = free;
            int in = 0;
            if (hasLayout && !inputLocked()) {
               if (flat(mc.player, lightX, lightZ) < 1.7) {
                  in = 1;
               } else if (!Double.isNaN(darkX) && flat(mc.player, darkX, darkZ) < 1.7) {
                  in = 2;
               }
            }

            if (in != 0 && in != hintAt) {
               String key = TurnBattleClientMod.inspectKey != null ? TurnBattleClientMod.inspectKey.getBoundKeyLocalizedText().getString() : "?";
               mc.player.sendMessage(Text.literal(in == 1 ? "* Press [" + key + "] to leave the Depths." : "* Press [" + key + "] to go down."), true);
            }

            hintAt = in;
            recordHistory(mc);
         }
      }
   }

   private static boolean inputLockedForCards() {
      return introNs >= 0L || sinkNs >= 0L || vanishNs >= 0L || DarkWorldFall.active() && DarkWorldFall.oriMode();
   }

   private static void restorePerspective(MinecraftClient mc) {
      if (perspectiveBefore != null) {
         mc.options.setPerspective(perspectiveBefore);
      }

      perspectiveBefore = null;
   }

   public static boolean onInspect(MinecraftClient mc) {
      if (controlling() && !inputLocked() && mc.currentScreen == null) {
         long now = System.nanoTime();
         if (now - lastInteractNs < 300000000L) {
            return true;
         } else {
            lastInteractNs = now;
            Net121.sendToServer(DepthsRun.INTERACT, PacketByteBufs.create());
            return true;
         }
      } else {
         return controlling();
      }
   }

   private static double flat(Entity e, double x, double z) {
      double dx = e.getX() - x;
      double dz = e.getZ() - z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static float secs(long ns) {
      return (float)(System.nanoTime() - ns) / 1.0E9F;
   }

   public static void adjustInput(Input in) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (controlling()) {
         in.jumping = false;
         if (!inputLocked() && mc.player != null) {
            float f = in.movementForward;
            float sd = in.movementSideways;
            double vx = (double)(-sd);
            double vz = (double)(-f);
            double len = Math.sqrt(vx * vx + vz * vz);
            if (!(len < 0.01)) {
               float mag = (float)Math.min(1.0, len);
               vx /= len;
               vz /= len;
               float want = (float)Math.toDegrees(Math.atan2(-vx, vz));
               float yaw = mc.player.getYaw();
               float turn = MathHelper.wrapDegrees(want - yaw);
               yaw += MathHelper.clamp(turn, -32.0F, 32.0F);
               mc.player.setYaw(yaw);
               mc.player.setPitch(12.0F);
               double ry = Math.toRadians((double)yaw);
               double fx = -Math.sin(ry);
               double fz = Math.cos(ry);
               double lx = Math.cos(ry);
               double lz = Math.sin(ry);
               in.movementForward = (float)((vx * fx + vz * fz) * (double)mag);
               in.movementSideways = (float)((vx * lx + vz * lz) * (double)mag);
            }
         } else {
            in.movementForward = 0.0F;
            in.movementSideways = 0.0F;
            in.pressingForward = in.pressingBack = in.pressingLeft = in.pressingRight = false;
            in.sneaking = false;
         }
      }
   }

   public static boolean cameraActive() {
      return controlling();
   }

   public static void updateCamera(float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         long now = System.nanoTime();
         float dt = camNs == 0L ? 1.0F : MathHelper.clamp((float)(now - camNs) / 1.0E9F, 0.0F, 0.1F);
         camNs = now;
         Vec3d at = mc.player.getLerpedPos(td);
         double y = sinkNs >= 0L ? 65.0 : at.y;
         Vec3d goal = new Vec3d(at.x, y + 10.5, at.z + 4.6);
         if (camPos != null && !(camPos.squaredDistanceTo(goal) > 400.0)) {
            camPos = camPos.lerp(goal, (double)(1.0F - (float)Math.exp((double)(-dt * 10.0F))));
         } else {
            camPos = goal;
         }
      }
   }

   public static Vec3d cameraPos() {
      return camPos != null ? camPos : Vec3d.ZERO;
   }

   public static float cameraYaw() {
      return 180.0F;
   }

   public static float cameraPitch() {
      return (float)Math.toDegrees(Math.atan2(10.5, 4.6));
   }

   public static boolean applyScenePose(OriModel m, AbstractClientPlayerEntity e) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (e != mc.player || !controlling()) {
         return false;
      } else if (introNs >= 0L) {
         float t = secs(introNs);
         float k = t < 1.3F ? 1.0F : 1.0F - smooth(MathHelper.clamp((t - 1.3F) / 1.3F, 0.0F, 1.0F));
         if (k <= 0.001F) {
            return false;
         } else {
            float drop = 9.6F * k;
            m.body.pivotY += drop;
            m.head.pivotY += drop;
            m.leftArm.pivotY += drop;
            m.rightArm.pivotY += drop;
            m.leftLeg.pivotY += drop;
            m.rightLeg.pivotY += drop;
            m.leftLeg.pitch = mix(m.leftLeg.pitch, 1.5F, k);
            m.rightLeg.pitch = mix(m.rightLeg.pitch, 1.5F, k);
            m.leftLeg.yaw = mix(m.leftLeg.yaw, -0.1F, k);
            m.rightLeg.yaw = mix(m.rightLeg.yaw, 0.1F, k);
            float sway = 0.04F * MathHelper.sin(t * 1.7F);
            m.head.pitch = mix(m.head.pitch, 0.62F + sway, k);
            m.head.yaw = mix(m.head.yaw, 0.0F, k);
            m.body.pitch = mix(m.body.pitch, 0.24F, k);
            m.leftArm.pitch = mix(m.leftArm.pitch, -0.52F, k);
            m.rightArm.pitch = mix(m.rightArm.pitch, -0.52F, k);
            m.leftArm.roll = mix(m.leftArm.roll, -0.08F, k);
            m.rightArm.roll = mix(m.rightArm.roll, 0.08F, k);
            m.hat.copyTransform(m.head);
            return true;
         }
      } else if (vanishNs >= 0L && secs(vanishNs) < 4.1F) {
         float t = secs(vanishNs);
         boolean armed = !e.getMainHandStack().isEmpty();
         boolean leftMain = e.getMainArm() == Arm.LEFT;
         ModelPart main = leftMain ? m.leftArm : m.rightArm;
         ModelPart off = leftMain ? m.rightArm : m.leftArm;
         float side = leftMain ? -1.0F : 1.0F;
         float breathe = 0.02F * MathHelper.sin(t * 2.2F);
         m.head.pitch = 0.82F + breathe;
         m.head.yaw = 0.0F;
         m.head.roll = 0.0F;
         m.body.pitch = 0.1F;
         main.pitch = armed ? -0.18F : 0.04F;
         main.yaw = 0.0F;
         main.roll = side * 0.06F;
         off.pitch = 0.05F;
         off.yaw = 0.0F;
         off.roll = -side * 0.05F;
         m.leftLeg.pitch = 0.0F;
         m.rightLeg.pitch = 0.0F;
         if (t > 3.6F) {
            float u = MathHelper.clamp((t - 3.6F) / 0.5F, 0.0F, 1.0F);
            float k = MathHelper.sin(u * (float) Math.PI);
            m.body.pitch = 0.1F + 0.42F * k;
            m.head.pitch = mix(0.82F, 0.35F, u);
            off.pitch = mix(0.05F, -0.95F, k);
            m.leftLeg.pitch = -0.25F * k;
            m.rightLeg.pitch = 0.15F * k;
         }

         m.hat.copyTransform(m.head);
         return true;
      } else if (sinkNs >= 0L) {
         float t = secs(sinkNs) * 20.0F;
         float k = smooth(MathHelper.clamp(t / 10.0F, 0.0F, 1.0F));
         float tr = 0.025F * MathHelper.sin(t * 2.9F);
         m.rightArm.pitch = mix(m.rightArm.pitch, -1.42F + tr, k);
         m.rightArm.yaw = mix(m.rightArm.yaw, -0.62F, k);
         m.leftArm.pitch = mix(m.leftArm.pitch, -1.3F - tr, k);
         m.leftArm.yaw = mix(m.leftArm.yaw, 0.66F, k);
         m.head.pitch = mix(m.head.pitch, 0.48F, k);
         m.rightLeg.pitch = mix(m.rightLeg.pitch, -0.62F, k);
         m.leftLeg.pitch = mix(m.leftLeg.pitch, -0.12F, k);
         m.hat.copyTransform(m.head);
         return true;
      } else {
         return false;
      }
   }

   private static float mix(float a, float b, float k) {
      return a + (b - a) * k;
   }

   private static float smooth(float x) {
      return x * x * (3.0F - 2.0F * x);
   }

   private static void recordHistory(MinecraftClient mc) {
      Iterator it = history.entrySet().iterator();

      while (it.hasNext()) {
         Entity e = mc.world.getEntityById(it.next().getKey());
         if (e == null || e.isRemoved()) {
            it.remove();
         }
      }

      for (Entity e : mc.world.getEntities()) {
         if (e instanceof LivingEntity le && !(e instanceof PlayerEntity) && le.isAlive() && !(e.squaredDistanceTo(mc.player) > 2304.0)) {
            ArrayDeque q = history.computeIfAbsent(e.getId(), k -> new ArrayDeque<>());
            q.addFirst(new DepthsClient.Past(e.getX(), e.getY(), e.getZ(), le.bodyYaw));

            while (q.size() > 10) {
               q.removeLast();
            }
         }
      }
   }

   private static void renderWorld(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (active && here() && mc.world != null && mc.player != null) {
         if (!(mc.currentScreen instanceof BattleScreen)) {
            Vec3d cam = ctx.camera().getPos();
            float td = mc.getRenderTickCounter().getTickDelta(false);
            float t = ((float)(mc.world.getTime() % 1200000L) + td) / 20.0F;
            if (hasLayout) {
               begin();
               pillar((float)(lightX - cam.x), (float)(65.0 - cam.y), (float)(lightZ - cam.z), t, 1, false);
               flush(true);
               if (!Double.isNaN(darkX)) {
                  begin();
                  pillar((float)(darkX - cam.x), (float)(65.0 - cam.y), (float)(darkZ - cam.z), t, 2, true);
                  flush(false);
               }
            }

            Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
            imm.draw();
            EntityRenderDispatcher disp = mc.getEntityRenderDispatcher();
            boolean shadows = (Boolean)mc.options.getEntityShadows().getValue();
            disp.setRenderShadows(false);

            try {
               for (Entry en : history.entrySet()) {
                  Entity e = mc.world.getEntityById(en.getKey());
                  if (e instanceof LivingEntity) {
                     LivingEntity le = (LivingEntity)e;
                     if (le.isAlive() && !le.isInvisible()) {
                        DepthsClient.Past[] ps = en.getValue().toArray(new DepthsClient.Past[0]);
                        if (ps.length != 0) {
                           for (int i = 3; i < ps.length; i += 3) {
                              DepthsClient.Past p = ps[i];
                              double moved = Math.abs(p.x() - ps[0].x()) + Math.abs(p.z() - ps[0].z());
                              if (!(moved < 0.15)) {
                                 float a = 0.42F * (1.0F - (float)i / 10.0F);
                                 ghost(mc, imm, le, p.x(), p.y(), p.z(), p.yaw(), 1.0F, a, cam, td);
                              }
                           }

                           Vec3d now = le.getLerpedPos(td);

                           for (int k = 0; k < 2; k++) {
                              float q = (t * 0.55F + (float)k * 0.5F + (float)(e.getId() % 7) * 0.13F) % 1.0F;
                              float a = 0.34F * (1.0F - q) * Math.min(1.0F, q * 5.0F);
                              ghost(mc, imm, le, now.x, now.y + (double)q * 0.12, now.z, le.bodyYaw, 1.0F + 0.22F * q, a, cam, td);
                           }
                        }
                     }
                  }
               }
            } finally {
               disp.setRenderShadows(shadows);
            }
         }
      }
   }

   private static void ghost(
      MinecraftClient mc, Immediate imm, LivingEntity e, double x, double y, double z, float yaw, float scale, float a, Vec3d cam, float td
   ) {
      if (!(a <= 0.02F)) {
         GhostRender.set(1.0F, 0.12F, 0.14F, a, 0.0F, false);
         RenderSystem.setShaderColor(1.0F, 0.12F, 0.14F, a);

         try {
            MatrixStack ms = new MatrixStack();
            ms.translate(x - cam.x, y - cam.y, z - cam.z);
            ms.scale(scale, scale, scale);
            int light = WorldRenderer.getLightmapCoordinates(mc.world, BlockPos.ofFloored(x, y + 0.5, z));
            mc.getEntityRenderDispatcher().render(e, 0.0, 0.0, 0.0, yaw, td, ms, imm, light);
            imm.draw();
         } catch (Throwable var19) {
         } finally {
            GhostRender.clear();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private static void begin() {
      count = 0;
   }

   private static void flush(boolean additive) {
      if (count != 0) {
         Matrix4f m = new MatrixStack().peek().getPositionMatrix();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.enableBlend();
         if (additive) {
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         } else {
            RenderSystem.defaultBlendFunc();
         }

         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);

         try {
            BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int i = 0; i < count; i++) {
               int o = i * 7;
               bb.vertex(m, verts[o], verts[o + 1], verts[o + 2]).color(verts[o + 3], verts[o + 4], verts[o + 5], verts[o + 6]);
            }

            BuiltBuffer built = bb.endNullable();
            if (built != null) {
               BufferRenderer.drawWithGlobalProgram(built);
            }
         } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private static void pillar(float x, float y, float z, float t, int seed, boolean dark) {
      float pulse = 0.92F + 0.08F * MathHelper.sin(t * 1.7F + (float)seed);
      if (dark) {
         cr = 0.05F;
         cg = 0.0F;
         cb = 0.09F;
      } else {
         cr = 0.94F;
         cg = 0.95F;
         cb = 1.0F;
      }

      float boost = dark ? 3.2F : 1.0F;

      for (int r = 0; r < RINGS.length; r++) {
         float rad = RINGS[r] * (0.98F + 0.02F * MathHelper.sin(t * 2.3F + (float)r + (float)seed));
         float a = (r == 0 ? 0.1F : (r == 1 ? 0.12F : 0.15F)) * pulse * boost;
         int bands = 6;

         for (int b = 0; b < bands; b++) {
            float h0 = 30.0F * (float)b / (float)bands;
            float h1 = 30.0F * (float)(b + 1) / (float)bands;
            float a0 = a * fadeUp(h0 / 30.0F);
            float a1 = a * fadeUp(h1 / 30.0F);

            for (int s = 0; s < 24; s++) {
               double t0 = (double)s * Math.PI * 2.0 / 24.0;
               double t1 = (double)(s + 1) * Math.PI * 2.0 / 24.0;
               float x0 = x + (float)Math.cos(t0) * rad;
               float z0 = z + (float)Math.sin(t0) * rad;
               float x1 = x + (float)Math.cos(t1) * rad;
               float z1 = z + (float)Math.sin(t1) * rad;
               put(x0, y + h0, z0, a0);
               put(x1, y + h0, z1, a0);
               put(x1, y + h1, z1, a1);
               put(x0, y + h1, z0, a1);
            }
         }
      }

      for (int r = 0; r < POOL.length; r++) {
         float rad = POOL[r];
         float a = (r == 0 ? 0.16F : (r == 1 ? 0.2F : 0.3F)) * pulse * (dark ? 2.6F : 1.0F);
         float yy = y + 0.02F + (float)r * 0.005F;

         for (int s = 0; s < 24; s++) {
            double t0 = (double)s * Math.PI * 2.0 / 24.0;
            double t1 = (double)(s + 1) * Math.PI * 2.0 / 24.0;
            put(x, yy, z, a);
            put(x, yy, z, a);
            put(x + (float)Math.cos(t0) * rad, yy, z + (float)Math.sin(t0) * rad, a);
            put(x + (float)Math.cos(t1) * rad, yy, z + (float)Math.sin(t1) * rad, a);
         }
      }

      float len = (float)Math.sqrt((double)(x * x + z * z));
      float rx = len > 0.001F ? -z / len : 1.0F;
      float rz = len > 0.001F ? x / len : 0.0F;
      if (dark) {
         cr = 0.55F;
         cg = 0.25F;
         cb = 0.85F;
      }

      for (int i = 0; i < 28; i++) {
         float h1 = rnd(seed * 31 + i, 1);
         float h2 = rnd(seed * 31 + i, 2);
         float h3 = rnd(seed * 31 + i, 3);
         float speed = 1.6F + 2.4F * h2;
         float span = 18.0F;
         float up = (t * speed + h3 * span) % span;
         if (dark) {
            up = span - up;
         }

         float ang = h1 * 6.283F + t * 0.4F * (h2 - 0.5F);
         float rr = RINGS[0] * (0.15F + 0.75F * rnd(seed * 31 + i, 4));
         float sx = x + (float)Math.cos((double)ang) * rr;
         float sz = z + (float)Math.sin((double)ang) * rr;
         float sy = y + 0.2F + up;
         float tw = 0.55F + 0.45F * MathHelper.sin(t * (7.0F + 5.0F * h1) + (float)i);
         float a = 0.9F * tw * (1.0F - up / span) * Math.min(1.0F, up / 0.6F);
         if (!(a <= 0.02F)) {
            float hs = 0.07F + 0.06F * rnd(seed * 31 + i, 5);
            put(sx - rx * hs, sy - hs, sz - rz * hs, a);
            put(sx + rx * hs, sy - hs, sz + rz * hs, a);
            put(sx + rx * hs, sy + hs, sz + rz * hs, a);
            put(sx - rx * hs, sy + hs, sz - rz * hs, a);
         }
      }
   }

   private static float fadeUp(float k) {
      if (k < 0.25F) {
         return 1.0F;
      } else {
         float f = 1.0F - (k - 0.25F) / 0.75F;
         return Math.max(0.0F, f * f);
      }
   }

   private static void put(float x, float y, float z, float a) {
      if ((count + 1) * 7 > verts.length) {
         if (verts.length >= 1835008) {
            return;
         }

         verts = Arrays.copyOf(verts, verts.length * 2);
      }

      int o = count * 7;
      verts[o] = x;
      verts[o + 1] = y;
      verts[o + 2] = z;
      verts[o + 3] = cr;
      verts[o + 4] = cg;
      verts[o + 5] = cb;
      verts[o + 6] = Math.min(1.0F, a);
      count++;
   }

   private static float rnd(int i, int salt) {
      long h = (long)i * -7046029254386353131L ^ (long)salt * -4417276706812531889L;
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   private static void renderSky(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      int w = mc.getWindow().getFramebufferWidth();
      int h = mc.getWindow().getFramebufferHeight();
      if (w > 0 && h > 0) {
         float t = (float)(System.nanoTime() % 3600000000000L) / 1.0E9F;
         RenderSystem.backupProjectionMatrix();
         RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, (float)w, (float)h, 0.0F, -1000.0F, 1000.0F), VertexSorter.BY_Z);
         Matrix4fStack mv = RenderSystem.getModelViewStack();
         mv.pushMatrix();
         mv.identity();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.depthMask(false);
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         try {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Matrix4f id = new Matrix4f();
            BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bb.vertex(id, 0.0F, (float)h, 0.0F).color(0, 0, 0, 255);
            bb.vertex(id, (float)w, (float)h, 0.0F).color(0, 0, 0, 255);
            bb.vertex(id, (float)w, 0.0F, 0.0F).color(0, 0, 0, 255);
            bb.vertex(id, 0.0F, 0.0F, 0.0F).color(0, 0, 0, 255);
            BuiltBuffer b0 = bb.endNullable();
            if (b0 != null) {
               BufferRenderer.drawWithGlobalProgram(b0);
            }

            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, FLOW);
            BufferBuilder fb = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float tileW = (float)w * 0.62F;
            float tileH = tileW * 476.0F / 632.0F;
            int strips = 48;
            float sh = (float)h / (float)strips;
            float hue = 0.5F + 0.5F * MathHelper.sin(t * 0.12F);

            for (int i = 0; i < strips; i++) {
               float y0 = (float)i * sh;
               float y1 = y0 + sh;
               float yc = (y0 + y1) * 0.5F / (float)h;
               float wave = MathHelper.sin(t * 1.1F + yc * 9.0F) * tileW * 0.03F + MathHelper.sin(t * 0.6F - yc * 4.0F) * tileW * 0.02F;
               float band = 0.75F + 0.45F * Math.max(0.0F, MathHelper.sin(yc * 7.0F + t * 1.6F));
               float vign = 1.0F - 0.65F * Math.abs(yc - 0.5F) * 2.0F;
               float lum = 0.2F * band * vign;
               int r = (int)(255.0F * lum * (0.8F + 0.15F * hue));
               int g = (int)(255.0F * lum * 0.95F);
               int bl = (int)(255.0F * lum * (0.55F + 0.25F * (1.0F - hue)));
               float vBase = (y0 / tileH + t * 0.05F) % 1.0F;
               float vSpan = sh / tileH;
               float[][] parts = vBase + vSpan <= 1.0F
                  ? new float[][]{{y0, y1, vBase, vBase + vSpan}}
                  : new float[][]{{y0, y0 + (1.0F - vBase) * tileH, vBase, 1.0F}, {y0 + (1.0F - vBase) * tileH, y1, 0.0F, vBase + vSpan - 1.0F}};

               for (float[] p : parts) {
                  for (float x = -tileW + wave % tileW; x < (float)w; x += tileW) {
                     float x1 = x + tileW;
                     fb.vertex(id, x, p[1], 0.0F).texture(0.0F, p[3]).color(r, g, bl, 255);
                     fb.vertex(id, x1, p[1], 0.0F).texture(1.0F, p[3]).color(r, g, bl, 255);
                     fb.vertex(id, x1, p[0], 0.0F).texture(1.0F, p[2]).color(r, g, bl, 255);
                     fb.vertex(id, x, p[0], 0.0F).texture(0.0F, p[2]).color(r, g, bl, 255);
                  }
               }
            }

            BuiltBuffer b1 = fb.endNullable();
            if (b1 != null) {
               BufferRenderer.drawWithGlobalProgram(b1);
            }
         } catch (Throwable var39) {
         } finally {
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            mv.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
         }
      }
   }

   private static void renderHud(DrawContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (active) {
         int sw = ctx.getScaledWindowWidth();
         int sh = ctx.getScaledWindowHeight();
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(0.0F, 0.0F, 600.0F);
         if (blackHold) {
            ctx.fill(0, 0, sw, sh, -16777216);
         } else if (vanishNs >= 0L && secs(vanishNs) < 2.85F) {
            float k = 1.0F - MathHelper.clamp((secs(vanishNs) - 2.5F) / 0.35F, 0.0F, 1.0F);
            ctx.fill(0, 0, sw, sh, (int)(255.0F * k) << 24);
         } else if (sinkNs >= 0L) {
            float k = MathHelper.clamp((secs(sinkNs) - 0.7F) / 1.0F, 0.0F, 1.0F);
            if (k > 0.003F) {
               ctx.fill(0, 0, sw, sh, (int)(255.0F * smooth(k)) << 24);
            }
         }

         if (!blackHold && here() && floor > 0 && (!DarkWorldFall.active() || !DarkWorldFall.oriMode()) && !(mc.currentScreen instanceof BattleScreen)) {
            String s = "FLOOR " + floor + (bossFloor ? "  \u2726" : "");
            ctx.drawTextWithShadow(mc.textRenderer, s, 8, 8, -788529153);
         }

         m.pop();
      }
   }

   private static record Past(double x, double y, double z, float yaw) {
   }
}
