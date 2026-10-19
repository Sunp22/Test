package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class LiveCombatClient {
   private static final int SWING = 9;
   private static final int FIN_SWING = 14;
   private static final int COMBO_COOLDOWN = 27;
   private static final int COMBO_WINDOW = 20;
   private static final int DOUBLE_TAP = 7;
   private static final int DASH_COOLDOWN = 14;
   private static final double LOCK_RANGE = 24.0;
   private static final double LOCK_KEEP = 32.0;
   private static final float LOCK_CONE = 40.0F;
   private static final int DMG_TICKS = 40;
   private static final Identifier DAMAGE_FONT = Identifier.of("turnbattle", "damage_numbers");
   private static final Identifier GRAZE_RING = Identifier.of("turnbattle", "textures/gui/graze_ring.png");
   private static final Identifier COL_FULL = Identifier.of("turnbattle", "textures/gui/playercolumn.png");
   private static final Identifier COL_SLIM = Identifier.of("turnbattle", "textures/gui/playercolumncondensed.png");
   private static final Identifier TP_BG = Identifier.of("turnbattle", "textures/gui/tp_bar.png");
   private static final Identifier TP_PROG = Identifier.of("turnbattle", "textures/gui/tp_bar_prog.png");
   private static final Identifier TP_WHITE = Identifier.of("turnbattle", "textures/gui/tp_bar_white.png");
   private static final int COL_W = 150;
   private static final int COL_SRC_W = 213;
   private static final int COL_SLIM_H = 29;
   private static final int COL_FULL_H = 53;
   private static final int BTN_W = 23;
   private static final int BTN_H = 20;
   private static final int BTN_H_SEL = 24;
   private static final int TP_SRC_W = 55;
   private static final int TP_SRC_H = 195;
   private static final int TP_W = 28;
   private static final int TP_H = 102;
   private static final String[] BTN_TEX = new String[]{"bash", "skills", "goods", "mercy"};
   private static final String[] BTN_TEX_SEL = new String[]{"bashsel", "skillsel", "goodssel", "mercysel"};
   private static final int BTN_BASH = 0;
   private static final int BTN_SKILL = 1;
   private static final int BTN_ITEM = 2;
   private static final int BTN_MERCY = 3;
   private static final int BTN_COUNT = 4;
   private static boolean live;
   private static float tp;
   private static float tpShown;
   private static float prevTpShown;
   private static final Map<Integer, LiveCombatClient.MobInfo> mobInfo = new HashMap<>();
   private static long ticks;
   private static int lockId = -1;
   private static float lockAnim;
   private static float prevLockAnim;
   private static int step = -1;
   private static long lastAtk = -100L;
   private static long moveLockUntil;
   private static long comboReadyAt;
   private static boolean queued;
   private static boolean guarding;
   private static boolean useWasDown;
   private static long lastUsePress = -100L;
   private static long dashReady;
   private static final int QTE_SWEEP = 20;
   private static final int QTE_FADE = 8;
   private static final int BASH_LUNGE = 7;
   private static final int BASH_COOLDOWN = 90;
   private static final int SKILL_COOLDOWN = 12;
   private static final int ACT_SKILL_COOLDOWN = 16;
   private static final int ITEM_COOLDOWN = 20;
   private static long skillReadyAt;
   private static long itemReadyAt;
   private static final Map<Integer, Long> actReadyAt = new HashMap<>();
   private static Vec3d lastLockPos;
   private static boolean lastLockHostile;
   private static final float QTE_CRIT_CENTER = 0.1F;
   private static final float QTE_CRIT_HALF = 0.02F;
   private static boolean qteActive;
   private static boolean qteDone;
   private static boolean qteCrit;
   private static boolean confirmWasDown;
   private static int qteTick;
   private static int qteTickPrev;
   private static float qtePos;
   private static long bashLungeUntil;
   private static long bashReadyAt;
   private static Vec3d bashDir = Vec3d.ZERO;
   private static long stunnedUntil;
   private static final Map<Integer, Long> stunned = new HashMap<>();
   private static final Map<Integer, ItemStack> armed = new HashMap<>();
   private static boolean menuOpen;
   private static int btnIdx;
   private static LiveCombatClient.ListKind list = LiveCombatClient.ListKind.NONE;
   private static int listIdx;
   private static int listScroll;
   private static final List<BattleSkill> skills = new ArrayList<>();
   private static final List<ActDefs.ActEntry> acts = new ArrayList<>();
   private static final List<Integer> itemSlots = new ArrayList<>();
   private static int sentPose = -1;
   private static float colAnim;
   private static float prevColAnim;
   private static float hudAnim;
   private static float prevHudAnim;
   private static float listAnim;
   private static float prevListAnim;
   private static boolean enterWasDown;
   private static boolean backLatch;
   private static final List<Integer> allyTargets = new ArrayList<>();
   private static LiveCombatClient.ListKind giveFrom = LiveCombatClient.ListKind.NONE;
   private static int giveFromIdx;
   private static int giveCode = -1;
   private static int giveSlot = -1;
   private static float allyAnim;
   private static float prevAllyAnim;
   private static final float ALLY_COL_SCALE = 0.85F;
   private static final List<LiveCombatClient.Num> nums = new ArrayList<>();
   private static final List<LiveCombatClient.Popup> popups = new ArrayList<>();
   private static final List<LiveCombatClient.Ring> rings = new ArrayList<>();
   private static final List<LiveCombatClient.Windup> windups = new ArrayList<>();
   private static final List<LiveCombatClient.Spare> spares = new ArrayList<>();
   private static final List<LiveCombatClient.Fx> fxs = new ArrayList<>();
   private static final Map<Integer, LiveCombatClient.Prep> preps = new HashMap<>();
   private static final Map<Integer, Long> lunges = new HashMap<>();
   private static final Map<Integer, CloneAnimator.Act> heldPoses = new HashMap<>();
   private static long lastFrameNanos;
   private static final Set<Integer> lockedOnIds = new HashSet<>();

   private LiveCombatClient() {
   }

   public static boolean isLive() {
      MinecraftClient mc = MinecraftClient.getInstance();
      return live
         && mc.player != null
         && mc.world != null
         && mc.player.isAlive()
         && !mc.player.isSpectator()
         && !(mc.currentScreen instanceof BattleScreen)
         && !TurnBattleClientMod.clientSoulRemoved
         && !SoulWalk.localOut;
   }

   public static boolean lockedOn() {
      return isLive() && lockId >= 0;
   }

   public static boolean menuOpen() {
      return isLive() && menuOpen;
   }

   public static boolean hudShown() {
      return isLive() && (lockId >= 0 || menuOpen);
   }

   public static boolean hideHotbar() {
      return hudShown();
   }

   public static boolean moveLocked() {
      return isLive() && (menuOpen || guarding || ticks < moveLockUntil || qteActive && !qteDone || ticks < bashLungeUntil || ticks < stunnedUntil);
   }

   public static boolean lookLocked() {
      return lockedOn();
   }

   public static boolean isGuarding() {
      return isLive() && guarding;
   }

   public static boolean isWindingUp() {
      return isLive() && qteActive && !qteDone;
   }

   private static boolean busy() {
      return qteActive && !qteDone || ticks < bashLungeUntil || ticks < stunnedUntil;
   }

   public static void register() {
      Net121.registerClientReceiver(LiveCombat.S2C_STATE, (client, handler, buf, sender) -> {
         boolean l = buf.readBoolean();
         float t = buf.readFloat();
         client.execute(() -> {
            if (live && !l) {
               reset(client);
            }

            live = l;
            tp = t;
         });
      });
      Net121.registerClientReceiver(LiveCombat.S2C_MOB, (client, handler, buf, sender) -> {
         int id = buf.readInt();
         LiveCombatClient.MobInfo mi = new LiveCombatClient.MobInfo(buf.readFloat(), buf.readByte(), buf.readByte(), buf.readByte(), buf.readBoolean());
         client.execute(() -> mobInfo.put(id, mi));
      });
      Net121.registerClientReceiver(LiveCombat.S2C_FX, (client, handler, buf, sender) -> {
         int kind = buf.readByte();
         Runnable r = readFx(kind, buf);
         if (r != null) {
            client.execute(r);
         }
      });
   }

   private static Runnable readFx(int kind, PacketByteBuf b) {
      switch (kind) {
         case 0: {
            int atk = b.readInt();
            int tgt = b.readInt();
            float dmg = b.readFloat();
            int flags = b.readByte();
            int col = b.readInt();
            return () -> onHit(atk, tgt, dmg, flags, col);
         }
         case 1: {
            int pid = b.readInt();
            int st = b.readByte();
            return () -> onSwing(pid, st);
         }
         case 2: {
            int pid = b.readInt();
            float dmg = b.readFloat();
            int flags = b.readByte();
            return () -> onHurt(pid, dmg, flags);
         }
         case 3: {
            int pid = b.readInt();
            float g = b.readFloat();
            return () -> onGraze(pid, g);
         }
         case 4: {
            int actor = b.readInt();
            int tgt = b.readInt();
            int code = b.readInt();
            return () -> onSkill(actor, tgt, code);
         }
         case 5: {
            int id = b.readInt();
            float amt = b.readFloat();
            return () -> addNum(id, String.valueOf(Math.max(1, Math.round(amt))), 6750054, true, 1.0F);
         }
         case 6: {
            int id = b.readInt();
            return () -> onSpare(id);
         }
         case 7: {
            int id = b.readInt();
            int t = b.readByte();
            return () -> {
               if (nearMe(id)) {
                  LiveCombatClient.Windup w = new LiveCombatClient.Windup();
                  w.mobId = id;
                  w.born = ticks;
                  w.until = ticks + (long)t;
                  windups.add(w);
               }
            };
         }
         case 8: {
            int id = b.readInt();
            int pose = b.readByte();
            return () -> onPose(id, pose);
         }
         case 9: {
            int id = b.readInt();
            String s = b.readString();
            int col = b.readInt();
            return () -> addPopup(id, s, col);
         }
         case 10: {
            int id = b.readInt();
            String name = b.readString();
            int col = b.readInt();
            int t = b.readByte();
            return () -> onPrepare(id, name, col, t);
         }
         case 11: {
            int id = b.readInt();
            int t = b.readByte();
            return () -> {
               preps.remove(id);
               if (nearMe(id)) {
                  lunges.put(id, ticks + (long)t);
               }
            };
         }
         case 12: {
            int id = b.readInt();
            int type = b.readByte();
            int col = b.readInt();
            return () -> onMobSkill(id, type, col);
         }
         case 13: {
            int id = b.readInt();
            int t = b.readByte();
            return () -> onStun(id, t);
         }
         case 14: {
            int id = b.readInt();
            boolean on = b.readBoolean();
            boolean has = b.readBoolean();
            NbtCompound nbt = has ? b.readNbt() : null;
            return () -> onArmed(id, on, nbt);
         }
         default:
            return null;
      }
   }

   static boolean nearMe(Entity e) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (e == null || mc.player == null) {
         return false;
      } else if (e == mc.player) {
         return true;
      } else {
         double r = 30.0;
         return mc.player.squaredDistanceTo(e) <= r * r;
      }
   }

   private static boolean nearMe(int entityId) {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.world != null && nearMe(mc.world.getEntityById(entityId));
   }

   private static void send(int op, Consumer<PacketByteBuf> body) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeByte(op);
      body.accept(buf);
      Net121.sendToServer(LiveCombat.C2S, buf);
   }

   public static boolean onAttackKey(MinecraftClient mc) {
      if (!isLive() || mc.currentScreen != null) {
         return false;
      } else if (menuOpen) {
         confirm(mc);
         return true;
      } else if (qteActive && !qteDone) {
         hitQte(mc);
         return true;
      } else if (lockId < 0) {
         return false;
      } else if (busy()) {
         return true;
      } else {
         if (!tryAttack(mc) && ticks >= comboReadyAt && step >= 0 && step < 4) {
            queued = true;
         }

         return true;
      }
   }

   public static boolean onUseKey(MinecraftClient mc) {
      if (!isLive() || mc.currentScreen != null) {
         return false;
      } else if (menuOpen) {
         if (!backLatch) {
            backLatch = true;
            back(mc);
         }

         return true;
      } else if (backLatch) {
         return true;
      } else {
         return busy() ? true : lockId >= 0 || guarding;
      }
   }

   public static boolean cancelBlockBreaking() {
      return isLive() && (lockId >= 0 || menuOpen || busy());
   }

   public static boolean onScroll(double v) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (!isLive() || mc.currentScreen != null || v == 0.0) {
         return false;
      } else if (menuOpen) {
         move(mc, v > 0.0 ? -1 : 1);
         return true;
      } else if (lockId < 0) {
         return false;
      } else {
         cycleTarget(mc, v > 0.0 ? -1 : 1);
         return true;
      }
   }

   public static boolean onInspect(MinecraftClient mc) {
      if (!isLive()) {
         return false;
      } else if (lockId >= 0) {
         setLock(-1);
         return true;
      } else {
         if (mc.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK) {
            String bid = Registries.BLOCK.getId(mc.world.getBlockState(bhr.getBlockPos()).getBlock()).getPath();
            if (InspectRegistry.forBlock(bid) != null) {
               return false;
            }
         }

         LivingEntity t = findTarget(mc);
         if (t == null) {
            return false;
         } else {
            setLock(t.getId());
            play((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.35F);
            return true;
         }
      }
   }

   public static boolean onRevealPressed(MinecraftClient mc) {
      if (!isLive() || mc.currentScreen != null) {
         return false;
      } else if (busy()) {
         return true;
      } else {
         if (menuOpen) {
            closeMenu();
         } else {
            openMenu(mc);
         }

         return true;
      }
   }

   public static void frameCamera(float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      long now = System.nanoTime();
      float dt = lastFrameNanos == 0L ? 0.016F : MathHelper.clamp((float)(now - lastFrameNanos) / 1.0E9F, 0.0F, 0.1F);
      lastFrameNanos = now;
      if (lockedOn() && mc.player != null) {
         Entity t = lockTarget(mc);
         if (t != null) {
            Vec3d eye = mc.player.getCameraPosVec(td);
            Vec3d aim = t.getLerpedPos(td).add(0.0, (double)t.getHeight() * 0.6, 0.0);
            Vec3d d = aim.subtract(eye);
            double h = Math.sqrt(d.x * d.x + d.z * d.z);
            if (!(h < 0.05) || !(Math.abs(d.y) < 0.05)) {
               float wantYaw = (float)(MathHelper.atan2(d.z, d.x) * 180.0F / (float)Math.PI) - 90.0F;
               float wantPitch = (float)(-(MathHelper.atan2(d.y, h) * 180.0F / (float)Math.PI));
               if (!mc.options.getPerspective().isFirstPerson() && !LiveCamera.active()) {
                  wantPitch += 12.0F;
               }

               wantPitch = MathHelper.clamp(wantPitch, -70.0F, 70.0F);
               float k = 1.0F - (float)Math.exp((double)(-dt * 14.0F));
               float yaw = mc.player.getYaw();
               float pitch = mc.player.getPitch();
               float ny = h < 0.3 ? yaw : yaw + MathHelper.wrapDegrees(wantYaw - yaw) * k;
               float np = pitch + (wantPitch - pitch) * k;
               mc.player.setYaw(ny);
               mc.player.setPitch(np);
               mc.player.prevYaw = ny;
               mc.player.prevPitch = np;
               mc.player.setHeadYaw(ny);
            }
         }
      }
   }

   private static void setLock(int id) {
      if (id != lockId) {
         lockId = id;
         if (id < 0 && guarding) {
            setGuard(false);
         }

         send(3, b -> b.writeInt(id));
      }
   }

   private static boolean targetable(MinecraftClient mc, Entity e) {
      if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive() || e.isSpectator() || e instanceof ArmorStandEntity) {
         return false;
      }

      if (e.isInvisible() && !(e instanceof PlayerEntity)) {
         return false;
      } else if (e instanceof BattleRecruitEntity) {
         return false;
      } else if (e instanceof SoulBodyEntity) {
         return false;
      } else {
         if (e instanceof PlayerEntity pe && SoulWalk.CLIENT_OUT.containsKey(pe.getUuid())) {
            return false;
         }

         if (e instanceof PlayerEntity p) {
            if (p.isCreative()) {
               return false;
            }

            for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
               if (m.uuid().equals(p.getUuid())) {
                  return false;
               }
            }
         }

         return mc.player.squaredDistanceTo(e) <= 576.0 && mc.player.canSee(e);
      }
   }

   private static LivingEntity nearestHostile(MinecraftClient mc, Vec3d at, double radius) {
      LivingEntity best = null;
      double bestD = radius * radius;

      for (Entity e : mc.world.getEntities()) {
         if (e instanceof Monster && targetable(mc, e)) {
            double d = e.squaredDistanceTo(at);
            if (d <= bestD) {
               bestD = d;
               best = (LivingEntity)e;
            }
         }
      }

      return best;
   }

   private static Vec3d aimPoint(Entity e) {
      return e.getPos().add(0.0, (double)e.getHeight() * 0.6, 0.0);
   }

   private static LivingEntity findTarget(MinecraftClient mc) {
      Vec3d eye = mc.player.getEyePos();
      Vec3d look = mc.player.getRotationVec(1.0F);
      if (mc.crosshairTarget instanceof EntityHitResult ehr && targetable(mc, ehr.getEntity())) {
         return (LivingEntity)ehr.getEntity();
      }

      LivingEntity best = null;
      double bestScore = Double.MAX_VALUE;

      for (Entity e : mc.world.getEntities()) {
         if (targetable(mc, e)) {
            Vec3d to = aimPoint(e).subtract(eye);
            double d = to.length();
            if (!(d < 0.001)) {
               double ang = Math.toDegrees(Math.acos(MathHelper.clamp(to.multiply(1.0 / d).dotProduct(look), -1.0, 1.0)));
               if (!(ang > 40.0)) {
                  double score = ang + d * 0.6;
                  if (score < bestScore) {
                     bestScore = score;
                     best = (LivingEntity)e;
                  }
               }
            }
         }
      }

      return best;
   }

   private static void cycleTarget(MinecraftClient mc, int dir) {
      List cands = new ArrayList<>();
      float yaw = mc.player.getYaw();

      for (Entity e : mc.world.getEntities()) {
         if (targetable(mc, e)) {
            cands.add(e);
         }
      }

      if (!cands.isEmpty()) {
         Comparator byAngle = Comparator.comparingDouble(ex -> relYaw(mc, ex, yaw));
         cands.sort(byAngle);
         int cur = -1;

         for (int i = 0; i < cands.size(); i++) {
            if (cands.get(i).getId() == lockId) {
               cur = i;
            }
         }

         int next = cur < 0 ? 0 : Math.floorMod(cur + dir, cands.size());
         setLock(cands.get(next).getId());
         play((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.8F, 0.25F);
      }
   }

   private static double relYaw(MinecraftClient mc, Entity e, float yaw) {
      double dx = e.getX() - mc.player.getX();
      double dz = e.getZ() - mc.player.getZ();
      float want = (float)(MathHelper.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
      return (double)MathHelper.wrapDegrees(want - yaw);
   }

   public static Entity lockTargetEntity() {
      return lockTarget(MinecraftClient.getInstance());
   }

   private static Entity lockTarget(MinecraftClient mc) {
      return lockId >= 0 && mc.world != null ? mc.world.getEntityById(lockId) : null;
   }

   private static boolean tryAttack(MinecraftClient mc) {
      if (ticks < comboReadyAt) {
         return false;
      } else if (step >= 0 && step < 4 && ticks - lastAtk < 9L) {
         return false;
      } else {
         int next = step >= 0 && step < 4 && ticks - lastAtk <= 20L ? step + 1 : 0;
         step = next;
         lastAtk = ticks;
         queued = false;
         if (guarding) {
            setGuard(false);
         }

         boolean fin = next == 4;
         if (fin) {
            comboReadyAt = ticks + 14L + 27L;
         }

         int lid = lockId;
         send(0, b -> {
            b.writeVarInt(next);
            b.writeInt(lid);
         });
         mc.player.swingHand(Hand.MAIN_HAND);
         LiveFirstPerson.play(LiveFirstPerson.Kind.COMBO, next);
         moveLockUntil = ticks + (long)(fin ? 7 : 3);
         Entity t = lockTarget(mc);
         if (t != null) {
            Vec3d d = new Vec3d(t.getX() - mc.player.getX(), 0.0, t.getZ() - mc.player.getZ());
            double dist = d.length() - (double)t.getWidth() / 2.0;
            if (dist > 1.6 && dist < 6.5) {
               Vec3d v = d.normalize().multiply(fin ? 0.5 : 0.34);
               mc.player.setVelocity(v.x, mc.player.getVelocity().y, v.z);
            }
         }

         return true;
      }
   }

   private static boolean canGuardUnlocked(MinecraftClient mc) {
      if (mc.crosshairTarget != null && mc.crosshairTarget.getType() != Type.MISS) {
         return false;
      } else {
         ItemStack s = mc.player.getMainHandStack();
         return s.isEmpty() || !(s.getItem() instanceof BlockItem) && s.getUseAction() == UseAction.NONE && s.get(DataComponentTypes.FOOD) == null;
      }
   }

   private static void setGuard(boolean on) {
      if (guarding != on) {
         guarding = on;
         send(1, b -> b.writeBoolean(on));
      }
   }

   private static void tickGuard(MinecraftClient mc) {
      boolean down = mc.options.useKey.isPressed() && !menuOpen && !backLatch && !busy() && mc.currentScreen == null;
      if (down && !useWasDown) {
         boolean moving = mc.options.forwardKey.isPressed()
            || mc.options.backKey.isPressed()
            || mc.options.leftKey.isPressed()
            || mc.options.rightKey.isPressed();
         if (lockId >= 0 && moving && ticks - lastUsePress <= 7L && ticks >= dashReady) {
            dash(mc);
         } else if (lockId >= 0) {
            setGuard(true);
         }

         lastUsePress = ticks;
      } else if (!down && guarding) {
         setGuard(false);
      }

      useWasDown = down;
   }

   private static void dash(MinecraftClient mc) {
      setGuard(false);
      dashReady = ticks + 14L;
      float fwd = (float)((mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0));
      float side = (float)((mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0));
      float yr = mc.player.getYaw() * (float) (Math.PI / 180.0);
      float sin = MathHelper.sin(yr);
      float cos = MathHelper.cos(yr);
      Vec3d dir = new Vec3d((double)(side * cos - fwd * sin), 0.0, (double)(fwd * cos + side * sin));
      if (!(dir.lengthSquared() < 1.0E-4)) {
         dir = dir.normalize();
         Vec3d v = dir.multiply(1.15);
         mc.player.setVelocity(v.x, mc.player.isOnGround() ? 0.12 : mc.player.getVelocity().y, v.z);
         float dx = (float)dir.x;
         float dz = (float)dir.z;
         send(2, b -> {
            b.writeFloat(dx);
            b.writeFloat(dz);
         });
         LiveFirstPerson.play(LiveFirstPerson.Kind.DASH, 0);
      }
   }

   private static void openMenu(MinecraftClient mc) {
      if (guarding) {
         setGuard(false);
      }

      menuOpen = true;
      btnIdx = 0;
      list = LiveCombatClient.ListKind.NONE;
      drain(mc);
      sentPose = -1;
      updateMenuPose();
      playUi(TurnBattleSounds.MENU_SELECT);
   }

   private static void closeMenu() {
      if (menuOpen) {
         menuOpen = false;
         list = LiveCombatClient.ListKind.NONE;
         sentPose = -1;
         send(8, b -> {
            b.writeBoolean(false);
            b.writeByte(0);
         });
      }
   }

   private static void drain(MinecraftClient mc) {
      for (KeyBinding k : new KeyBinding[]{
         mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sneakKey
      }) {
         while (k.wasPressed()) {
         }
      }
   }

   private static int poseForMenu() {
      if (!menuOpen) {
         return 0;
      } else {
         return switch (list) {
            case NONE -> btnIdx == 0 ? 9 : (btnIdx == 1 ? 2 : (btnIdx == 2 ? 3 : 0));
            case CHOICE -> listIdx == 1 ? 4 : 2;
            case SKILLS -> 2;
            case ACTS -> 4;
            case ITEMS -> 3;
            case TARGET -> giveFrom == LiveCombatClient.ListKind.ITEMS ? 3 : 2;
         };
      }
   }

   private static void updateMenuPose() {
      int pose = poseForMenu();
      if (pose != sentPose) {
         sentPose = pose;
         send(8, b -> {
            b.writeBoolean(true);
            b.writeByte(pose);
         });
      }
   }

   private static int listSize() {
      return switch (list) {
         case NONE -> 3;
         case CHOICE -> 2;
         case SKILLS -> skills.size();
         case ACTS -> acts.size();
         case ITEMS -> itemSlots.size();
         case TARGET -> allyTargets.size();
      };
   }

   private static void move(MinecraftClient mc, int dir) {
      if (list == LiveCombatClient.ListKind.NONE) {
         btnIdx = Math.floorMod(btnIdx + dir, 4);
      } else {
         int n = listSize();
         if (n == 0) {
            return;
         }

         listIdx = Math.floorMod(listIdx + dir, n);
      }

      playUi(TurnBattleSounds.MENU_MOVE);
      updateMenuPose();
   }

   private static void openList(MinecraftClient mc, LiveCombatClient.ListKind k) {
      list = k;
      listIdx = 0;
      listScroll = 0;
      prevListAnim = 0.0F;
      listAnim = 0.0F;
      playUi(TurnBattleSounds.MENU_SELECT);
      updateMenuPose();
   }

   private static void confirm(MinecraftClient mc) {
      Entity t = lockTarget(mc);
      boolean mobLocked = t instanceof LivingEntity && !(t instanceof PlayerEntity);
      switch (list) {
         case NONE:
            switch (btnIdx) {
               case 0:
                  if (ticks < bashReadyAt) {
                     playUi(TurnBattleSounds.SND_B);
                     toast(mc, "Not ready yet...");
                     return;
                  }

                  if (tp < 16.0F) {
                     playUi(TurnBattleSounds.SND_B);
                     toast(mc, "Not enough TP! (Bash needs 16)");
                     return;
                  }

                  playUi(TurnBattleSounds.MENU_SELECT);
                  closeMenu();
                  startQte(mc);
                  return;
               case 1:
                  buildSkills();
                  acts.clear();
                  if (mobLocked) {
                     acts.addAll(LiveCombat.actsFor((LivingEntity)t));
                  }

                  if (!acts.isEmpty()) {
                     openList(mc, LiveCombatClient.ListKind.CHOICE);
                     return;
                  } else {
                     if (skills.isEmpty()) {
                        toast(mc, "You don't know any skills yet.");
                     } else {
                        openList(mc, LiveCombatClient.ListKind.SKILLS);
                     }

                     return;
                  }
               case 2:
                  buildItems(mc);
                  if (itemSlots.isEmpty()) {
                     toast(mc, "You have nothing to eat.");
                  } else {
                     openList(mc, LiveCombatClient.ListKind.ITEMS);
                  }

                  return;
               case 3:
                  if (TurnBattleClientMod.isRuthless) {
                     playUi(TurnBattleSounds.SND_B);
                     return;
                  }

                  if (!mobLocked) {
                     toast(mc, "Lock on to someone to spare them.");
                     return;
                  }

                  int id = lockId;
                  send(7, b -> b.writeInt(id));
                  playUi(TurnBattleSounds.MENU_SELECT);
                  closeMenu();
                  return;
               default:
                  return;
            }
         case CHOICE:
            if (listIdx == 1) {
               openList(mc, LiveCombatClient.ListKind.ACTS);
            } else if (skills.isEmpty()) {
               toast(mc, "You don't know any skills yet.");
            } else {
               openList(mc, LiveCombatClient.ListKind.SKILLS);
            }
            break;
         case SKILLS:
            if (listIdx < 0 || listIdx >= skills.size()) {
               return;
            }

            BattleSkill s = skills.get(listIdx);
            if (ticks < skillReadyAt) {
               playUi(TurnBattleSounds.SND_B);
               toast(mc, "Not ready yet...");
               return;
            }

            if (tp < (float)costOf(s)) {
               playUi(TurnBattleSounds.SND_B);
               toast(mc, "Not enough TP!");
               return;
            }

            boolean support = LiveSkills.isSupport(s) || s.getType() == SkillType.MAGNET;
            if (!support && lockId < 0) {
               toast(mc, "Lock on to something first.");
               return;
            }

            int code = LiveSkills.encode(s);
            int tid = lockId;
            if (LiveSkills.isSupport(s) && pickRecipient(mc, LiveCombatClient.ListKind.SKILLS, code, -1)) {
               return;
            }

            send(4, b -> {
               b.writeVarInt(code);
               b.writeInt(tid);
            });
            skillReadyAt = ticks + 12L;
            LiveFirstPerson.play(LiveFirstPerson.Kind.CAST, 0);
            playUi(TurnBattleSounds.MENU_SELECT);
            closeMenu();
            break;
         case ACTS:
            if (listIdx < 0 || listIdx >= acts.size() || lockId < 0) {
               return;
            }

            if (actOnCooldown(listIdx) || ticks < skillReadyAt) {
               playUi(TurnBattleSounds.SND_B);
               toast(mc, "You can't do that again just yet.");
               return;
            }

            int idx = listIdx;
            int tid = lockId;
            if (!isCheck(acts.get(listIdx))) {
               actReadyAt.put(lockId, ticks + 100L);
            }

            skillReadyAt = ticks + 16L;
            send(5, b -> {
               b.writeVarInt(idx);
               b.writeInt(tid);
            });
            LiveFirstPerson.play(LiveFirstPerson.Kind.CAST, 0);
            playUi(TurnBattleSounds.MENU_SELECT);
            closeMenu();
            break;
         case ITEMS:
            if (listIdx < 0 || listIdx >= itemSlots.size()) {
               return;
            }

            int slot = itemSlots.get(listIdx);
            if (ticks < itemReadyAt) {
               playUi(TurnBattleSounds.SND_B);
               toast(mc, "Not ready yet...");
               return;
            }

            if (pickRecipient(mc, LiveCombatClient.ListKind.ITEMS, -1, slot)) {
               return;
            }

            send(6, b -> {
               b.writeVarInt(slot);
               b.writeInt(-1);
            });
            itemReadyAt = ticks + 20L;
            LiveFirstPerson.play(LiveFirstPerson.Kind.ITEM, 0);
            playUi(TurnBattleSounds.SND_ITEM);
            closeMenu();
            break;
         case TARGET:
            if (listIdx < 0 || listIdx >= allyTargets.size()) {
               return;
            }

            int who = allyTargets.get(listIdx);
            if (giveFrom == LiveCombatClient.ListKind.SKILLS) {
               BattleSkill sx = LiveSkills.decode(giveCode);
               if (sx == null) {
                  back(mc);
                  return;
               }

               if (tp < (float)costOf(sx)) {
                  playUi(TurnBattleSounds.SND_B);
                  toast(mc, "Not enough TP!");
                  return;
               }

               int code = giveCode;
               if (ticks < skillReadyAt) {
                  playUi(TurnBattleSounds.SND_B);
                  toast(mc, "Not ready yet...");
                  return;
               }

               send(4, b -> {
                  b.writeVarInt(code);
                  b.writeInt(who);
               });
               skillReadyAt = ticks + 12L;
               LiveFirstPerson.play(LiveFirstPerson.Kind.CAST, 0);
               playUi(TurnBattleSounds.MENU_SELECT);
            } else {
               int slotx = giveSlot;
               ItemStack st = slotx >= 0 && slotx < mc.player.getInventory().main.size()
                  ? (ItemStack)mc.player.getInventory().main.get(slotx)
                  : ItemStack.EMPTY;
               if (st.isEmpty() || st.get(DataComponentTypes.FOOD) == null) {
                  back(mc);
                  return;
               }

               if (ticks < itemReadyAt) {
                  playUi(TurnBattleSounds.SND_B);
                  toast(mc, "Not ready yet...");
                  return;
               }

               send(6, b -> {
                  b.writeVarInt(slot);
                  b.writeInt(who);
               });
               itemReadyAt = ticks + 20L;
               LiveFirstPerson.play(LiveFirstPerson.Kind.ITEM, 0);
               playUi(TurnBattleSounds.SND_ITEM);
            }

            closeMenu();
      }
   }

   private static boolean isCheck(ActDefs.ActEntry a) {
      return a != null && "__CHECK__".equals(a.logText());
   }

   private static boolean actOnCooldown(int i) {
      return i >= 0 && i < acts.size() && !isCheck(acts.get(i)) && lockId >= 0 ? ticks < actReadyAt.getOrDefault(lockId, 0L) : false;
   }

   private static boolean buttonOnCooldown(int b) {
      return switch (b) {
         case 0 -> ticks < bashReadyAt || tp < 16.0F;
         case 1 -> ticks < skillReadyAt;
         case 2 -> ticks < itemReadyAt;
         case 3 -> TurnBattleClientMod.isRuthless;
         default -> false;
      };
   }

   private static List<PlayerEntity> alliesInReach(MinecraftClient mc) {
      List out = new ArrayList<>();
      double r = 30.0;

      for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
         PlayerEntity pe = mc.world.getPlayerByUuid(m.uuid());
         if (pe != null && pe != mc.player && pe.isAlive() && !pe.isSpectator() && !(mc.player.squaredDistanceTo(pe) > r * r)) {
            out.add(pe);
         }
      }

      return out;
   }

   private static void rebuildAllyTargets(MinecraftClient mc) {
      Integer was = list == LiveCombatClient.ListKind.TARGET && listIdx >= 0 && listIdx < allyTargets.size() ? allyTargets.get(listIdx) : null;
      allyTargets.clear();
      allyTargets.add(mc.player.getId());

      for (PlayerEntity pe : alliesInReach(mc)) {
         allyTargets.add(pe.getId());
      }

      if (list == LiveCombatClient.ListKind.TARGET) {
         int at = was != null ? allyTargets.indexOf(was) : -1;
         listIdx = at >= 0 ? at : Math.min(listIdx, allyTargets.size() - 1);
      }
   }

   private static boolean pickRecipient(MinecraftClient mc, LiveCombatClient.ListKind from, int code, int slot) {
      if (alliesInReach(mc).isEmpty()) {
         return false;
      } else {
         giveFrom = from;
         giveFromIdx = listIdx;
         giveCode = code;
         giveSlot = slot;
         allyTargets.clear();
         openList(mc, LiveCombatClient.ListKind.TARGET);
         rebuildAllyTargets(mc);
         listIdx = 0;
         return true;
      }
   }

   private static String allyName(MinecraftClient mc, Entity e) {
      if (e == null) {
         return "?";
      } else if (e == mc.player) {
         return "Yourself";
      } else {
         for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
            if (m.uuid().equals(e.getUuid())) {
               return m.vesselName() != null && !m.vesselName().isEmpty() ? m.vesselName() : m.name();
            }
         }

         return e.getName().getString();
      }
   }

   public static boolean chatMoved() {
      return isLive() && (menuOpen || lockId >= 0);
   }

   private static void back(MinecraftClient mc) {
      playUi(TurnBattleSounds.MENU_MOVE);
      switch (list) {
         case NONE:
            closeMenu();
            break;
         case CHOICE:
         case ITEMS:
         default:
            list = LiveCombatClient.ListKind.NONE;
            updateMenuPose();
            break;
         case SKILLS:
         case ACTS:
            if (!acts.isEmpty()) {
               list = LiveCombatClient.ListKind.CHOICE;
               listIdx = 0;
               prevListAnim = 0.0F;
               listAnim = 0.0F;
               updateMenuPose();
            } else {
               list = LiveCombatClient.ListKind.NONE;
               updateMenuPose();
            }
            break;
         case TARGET:
            list = giveFrom != LiveCombatClient.ListKind.ITEMS && giveFrom != LiveCombatClient.ListKind.SKILLS ? LiveCombatClient.ListKind.NONE : giveFrom;
            listIdx = giveFromIdx;
            prevListAnim = 0.0F;
            listAnim = 0.0F;
            updateMenuPose();
      }
   }

   private static void toast(MinecraftClient mc, String s) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("* " + s), true);
      }
   }

   private static void buildSkills() {
      skills.clear();
      if (RogueliteClient.info != null && RogueliteClient.info.inRun) {
         skills.addAll(LiveSkills.fromCards(TurnBattleClientMod.clientRogueSkills));
      } else {
         skills.addAll(
            LiveSkills.available(TurnBattleClientMod.clientPlayerRace, TurnBattleClientMod.clientUnlockedSkills, TurnBattleClientMod.clientUnequippedSkills)
         );
      }
   }

   private static int costOf(BattleSkill s) {
      int c = s.getTpCost();
      ItemStack acc = TurnBattleClientMod.clientEquippedAccessory;
      if (acc != null && !acc.isEmpty() && OffhandGimmick.fromStack(acc) == OffhandGimmick.THORN_RING) {
         c = (int)Math.ceil((double)((float)c * 0.5F));
      }

      return c;
   }

   private static void buildItems(MinecraftClient mc) {
      itemSlots.clear();
      PlayerInventory inv = mc.player.getInventory();

      for (int i = 0; i < inv.main.size(); i++) {
         ItemStack s = (ItemStack)inv.main.get(i);
         if (!s.isEmpty() && s.get(DataComponentTypes.FOOD) != null) {
            itemSlots.add(i);
         }
      }
   }

   private static void tickMenu(MinecraftClient mc) {
      if (menuOpen) {
         if (mc.currentScreen != null) {
            closeMenu();
         } else {
            while (mc.options.forwardKey.wasPressed()) {
               if (list != LiveCombatClient.ListKind.NONE) {
                  move(mc, -1);
               }
            }

            while (mc.options.backKey.wasPressed()) {
               if (list != LiveCombatClient.ListKind.NONE) {
                  move(mc, 1);
               }
            }

            while (mc.options.leftKey.wasPressed()) {
               if (list == LiveCombatClient.ListKind.NONE) {
                  move(mc, -1);
               }
            }

            while (mc.options.rightKey.wasPressed()) {
               if (list == LiveCombatClient.ListKind.NONE) {
                  move(mc, 1);
               }
            }

            while (mc.options.jumpKey.wasPressed()) {
               confirm(mc);
            }

            if (menuOpen) {
               while (mc.options.sneakKey.wasPressed()) {
                  back(mc);
               }

               boolean enter = InputUtil.isKeyPressed(mc.getWindow().getHandle(), 257);
               if (enter && !enterWasDown && menuOpen) {
                  confirm(mc);
               }

               enterWasDown = enter;
               int rows = 6;
               if (listIdx < listScroll) {
                  listScroll = listIdx;
               }

               if (listIdx >= listScroll + rows) {
                  listScroll = listIdx - rows + 1;
               }

               if (list == LiveCombatClient.ListKind.TARGET) {
                  rebuildAllyTargets(mc);
               }

               if (list == LiveCombatClient.ListKind.ITEMS) {
                  buildItems(mc);
                  if (itemSlots.isEmpty()) {
                     list = LiveCombatClient.ListKind.NONE;
                     updateMenuPose();
                  } else {
                     listIdx = Math.min(listIdx, itemSlots.size() - 1);
                  }
               }
            }
         }
      }
   }

   public static void tick(MinecraftClient mc) {
      ticks++;
      if (!mc.options.useKey.isPressed()) {
         backLatch = false;
      }

      prevColAnim = colAnim;
      prevHudAnim = hudAnim;
      prevListAnim = listAnim;
      prevLockAnim = lockAnim;
      prevTpShown = tpShown;
      prevAllyAnim = allyAnim;
      tickFx(mc);
      if (mc.player != null && mc.world != null) {
         if (!isLive()) {
            if (qteActive && !qteDone && live) {
               send(8, b -> {
                  b.writeBoolean(false);
                  b.writeByte(0);
               });
            }

            qteActive = false;
            bashLungeUntil = 0L;
            if (menuOpen) {
               closeMenu();
            }

            if (guarding) {
               setGuard(false);
            }

            if (lockId >= 0) {
               setLock(-1);
            }

            useWasDown = false;
            hudAnim = Math.max(0.0F, hudAnim - 0.2F);
            colAnim = Math.max(0.0F, colAnim - 0.25F);
            allyAnim = Math.max(0.0F, allyAnim - 0.2F);
         } else {
            Entity t = lockTarget(mc);
            if (lockId >= 0 && (t == null || !t.isAlive() || t.isRemoved() || mc.player.squaredDistanceTo(t) > 1024.0)) {
               Vec3d at = t != null ? t.getPos() : lastLockPos;
               boolean beaten = lastLockHostile && at != null && (t == null || !t.isAlive() || t.isRemoved()) && mc.player.squaredDistanceTo(at) <= 900.0;
               setLock(-1);
               t = null;
               if (beaten) {
                  LivingEntity next = nearestHostile(mc, at, 15.0);
                  if (next != null) {
                     setLock(next.getId());
                     t = next;
                  }
               }
            }

            if (lockId >= 0 && t != null) {
               lastLockPos = t.getPos();
               lastLockHostile = t instanceof Monster;
            } else if (lockId < 0) {
               lastLockPos = null;
               lastLockHostile = false;
            }

            tickQte(mc);
            tickBashLunge(mc);
            if (ticks < stunnedUntil && guarding) {
               setGuard(false);
            }

            tickGuard(mc);
            tickMenu(mc);
            if (queued && !menuOpen && !busy()) {
               tryAttack(mc);
            } else if (lockId >= 0 && !menuOpen && !busy() && mc.currentScreen == null && (!qteActive || qteDone) && mc.options.attackKey.isPressed()) {
               tryAttack(mc);
            }

            if (ticks - lastAtk > 22L) {
               step = -1;
               queued = false;
            }

            boolean shown = lockId >= 0 || menuOpen;
            hudAnim = MathHelper.clamp(hudAnim + (shown ? 0.2F : -0.2F), 0.0F, 1.0F);
            colAnim = MathHelper.clamp(colAnim + (menuOpen ? 0.25F : -0.25F), 0.0F, 1.0F);
            listAnim = MathHelper.clamp(listAnim + (menuOpen && list != LiveCombatClient.ListKind.NONE ? 0.34F : -0.34F), 0.0F, 1.0F);
            lockAnim = MathHelper.clamp(lockAnim + (lockId >= 0 ? 0.25F : -0.34F), 0.0F, 1.0F);
            allyAnim = MathHelper.clamp(allyAnim + (menuOpen && !TurnBattleClientMod.clientPartyPlayers.isEmpty() ? 0.2F : -0.2F), 0.0F, 1.0F);
            tpShown = tpShown + (tp - tpShown) * 0.35F;
            if (Math.abs(tp - tpShown) < 0.05F) {
               tpShown = tp;
            }
         }
      } else {
         if (live) {
            reset(mc);
         }

         live = false;
         prevTpShown = 0.0F;
         tpShown = 0.0F;
         tp = 0.0F;
      }
   }

   private static void reset(MinecraftClient mc) {
      menuOpen = false;
      list = LiveCombatClient.ListKind.NONE;
      guarding = false;
      lockId = -1;
      queued = false;
      step = -1;
      mobInfo.clear();
      preps.clear();
      lunges.clear();
      heldPoses.clear();
      stunned.clear();
      armed.clear();
      lockedOnIds.clear();
      qteActive = false;
      bashLungeUntil = 0L;
      stunnedUntil = 0L;
      bashReadyAt = 0L;
      skillReadyAt = 0L;
      itemReadyAt = 0L;
      actReadyAt.clear();
      lastLockPos = null;
      lastLockHostile = false;
   }

   private static void tickFx(MinecraftClient mc) {
      nums.removeIf(n -> ticks - n.born > 40L);
      popups.removeIf(p -> ticks - p.born > 34L);
      rings.removeIf(r -> ticks - r.born > 8L);
      windups.removeIf(w -> ticks > w.until + 2L);
      spares.removeIf(s -> ticks - s.born > 24L);

      for (LiveCombatClient.Fx f : fxs) {
         if (f.skill != null) {
            f.skill.tick();
         }

         if (f.bash != null) {
            f.bash.tick();
         }
      }

      fxs.removeIf(f -> (f.skill == null || f.skill.isDone()) && (f.bash == null || f.bash.isDone()));
      preps.values().removeIf(p -> ticks > p.until + 1L);
      stunned.values().removeIf(u -> ticks > u);
      if (mc.world != null && !armed.isEmpty() && mc.world.getTime() % 40L == 0L) {
         armed.keySet().removeIf(id -> !(mc.world.getEntityById(id) instanceof PlayerEntity));
      }

      if (mc.world != null && !lunges.isEmpty()) {
         Iterator it = lunges.entrySet().iterator();

         while (it.hasNext()) {
            Entry e = it.next();
            Entity m = mc.world.getEntityById(e.getKey());
            if (m != null && ticks <= e.getValue()) {
               for (int k = 0; k < 3; k++) {
                  mc.world
                     .addParticle(
                        ParticleTypes.CLOUD,
                        m.getX() + (Math.random() - 0.5) * (double)m.getWidth(),
                        m.getY() + Math.random() * (double)m.getHeight() * 0.6,
                        m.getZ() + (Math.random() - 0.5) * (double)m.getWidth(),
                        0.0,
                        0.01,
                        0.0
                     );
               }

               mc.world.addParticle(ParticleTypes.SWEEP_ATTACK, m.getX(), m.getY() + (double)m.getHeight() * 0.5, m.getZ(), 0.0, 0.0, 0.0);
            } else {
               it.remove();
            }
         }
      } else if (mc.world == null) {
         lunges.clear();
      }

      if (mc.world == null) {
         heldPoses.clear();
      } else {
         Iterator it = heldPoses.entrySet().iterator();

         while (it.hasNext()) {
            Entry e = it.next();
            Entity ent = mc.world.getEntityById(e.getKey());
            UUID key = OverhaulView.animKeyFor(ent);
            if (key != null && ent.isAlive()) {
               CloneAnimator.hold(key, e.getValue(), colorFor(e.getValue()), null, 1500L);
               OverhaulView.watchLive(key, 400L);
            } else {
               it.remove();
            }
         }

         for (PlayerEntity pl : mc.world.getPlayers()) {
            if (isLockedOn(pl)) {
               UUID k = OverhaulView.animKeyFor(pl);
               if (k != null) {
                  OverhaulView.watchLive(k, 300L);
               }
            }
         }

         if (mc.world.getTime() % 100L == 0L) {
            mobInfo.keySet().removeIf(id -> mc.world.getEntityById(id) == null);
         }
      }
   }

   private static int colorFor(CloneAnimator.Act a) {
      return switch (a) {
         case BASH_READY -> TurnBattleClientMod.clientSoul != null ? TurnBattleClientMod.clientSoul.getColor() & 16777215 : 16777215;
         case DEFEND -> 9427199;
         case CHOOSE_ITEM -> 7143290;
         case CHOOSE_ACT -> 16753722;
         default -> 16777215;
      };
   }

   private static float volumeAt(Entity e) {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.player != null && e != null ? MathHelper.clamp(1.0F - (float)Math.sqrt(mc.player.squaredDistanceTo(e)) / 40.0F, 0.0F, 0.9F) : 0.6F;
   }

   private static void onHit(int attackerId, int targetId, float dmg, int flags, int col) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         Entity atk = attackerId >= 0 ? mc.world.getEntityById(attackerId) : null;
         Entity tgt = mc.world.getEntityById(targetId);
         if (nearMe(tgt) || nearMe(atk)) {
            boolean fin = (flags & 1) != 0;
            boolean bash = (flags & 8) != 0;
            int step = flags >> 4 & 7;
            if (atk != null) {
               UUID key = OverhaulView.animKeyFor(atk);
               if (key != null && !bash) {
                  heldPoses.remove(atk.getId());
                  CloneAnimator.playCombo(key, step, col);
                  OverhaulView.watchLive(key, step == 4 ? 740L : 480L);
               }

               BattleScreen.BashEffect b = new BattleScreen.BashEffect(0, 0, col & 16777215);
               b.volume = volumeAt(tgt);
               LiveCombatClient.Fx f = new LiveCombatClient.Fx();
               f.bash = b;
               f.targetId = targetId;
               f.at = tgt != null ? tgt.getPos() : null;
               if (bash) {
                  f.mul = 1.8F;
               } else {
                  switch (step) {
                     case 1:
                        b.flip = true;
                        break;
                     case 2:
                        b.rotDeg = 70.0F;
                        break;
                     case 3:
                        b.rotDeg = -110.0F;
                        b.flip = true;
                        break;
                     case 4:
                        f.mul = 1.45F;
                        BattleScreen.BashEffect b2 = new BattleScreen.BashEffect(0, 0, col & 16777215);
                        b2.volume = 0.0F;
                        b2.flip = true;
                        LiveCombatClient.Fx f2 = new LiveCombatClient.Fx();
                        f2.bash = b2;
                        f2.targetId = targetId;
                        f2.at = f.at;
                        f2.mul = 1.45F;
                        fxs.add(f2);
                  }
               }

               fxs.add(f);
               if (fin && tgt != null) {
                  play(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8F, volumeAt(tgt));

                  for (int i = 0; i < 8; i++) {
                     mc.world
                        .addParticle(
                           ParticleTypes.CLOUD, tgt.getX(), tgt.getY() + 0.1, tgt.getZ(), (Math.random() - 0.5) * 0.15, 0.02, (Math.random() - 0.5) * 0.15
                        );
                  }
               }
            }

            String txt = String.valueOf(Math.max(1, Math.round(dmg)));
            int rgb = (flags & 4) != 0 ? 11184810 : ((col & 16777215) == 0 ? 16777215 : col & 16777215);
            if ((flags & 2) != 0) {
               txt = txt + "!";
            }

            addNum(targetId, txt, rgb, false, bash ? 1.55F : (fin ? 1.3F : 1.0F));
         }
      }
   }

   private static void onSwing(int playerId, int st) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(playerId) : null;
      if (e != null && nearMe(e)) {
         mc.world
            .playSound(
               e.getX(),
               e.getY(),
               e.getZ(),
               SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
               SoundCategory.PLAYERS,
               0.35F,
               st == 4 ? 0.8F : 1.2F + (float)st * 0.08F,
               false
            );
      }
   }

   private static void onHurt(int playerId, float dmg, int flags) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(playerId) : null;
      if (e != null && nearMe(e)) {
         boolean me = e == mc.player;
         boolean perfect = (flags & 2) != 0;
         boolean blocked = (flags & 1) != 0;
         UUID key = OverhaulView.animKeyFor(e);
         if (perfect) {
            mc.world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2F, 1.6F, false);
            mc.world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8F, 1.5F, false);
            LiveCombatClient.Ring r = new LiveCombatClient.Ring();
            r.entityId = playerId;
            r.born = ticks;
            r.rgb = 8972543;
            rings.add(r);
         } else {
            if (key != null) {
               CloneAnimator.flinch(key, blocked ? 0.5F : 1.0F);
               OverhaulView.watchLive(key, 460L);
            }

            if (blocked) {
               mc.world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8F, 1.0F, false);
            } else if (me) {
               playUi(TurnBattleSounds.HURT);
            } else {
               mc.world.playSound(e.getX(), e.getY(), e.getZ(), TurnBattleSounds.HURT, SoundCategory.PLAYERS, 0.7F, 1.0F, false);
            }

            if (dmg > 0.0F) {
               addNum(playerId, String.valueOf(Math.max(1, Math.round(dmg))), blocked ? 10475775 : 16777215, true, 1.0F);
            }
         }
      }
   }

   private static void onGraze(int playerId, float tpGain) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(playerId) : null;
      if (e != null && nearMe(e)) {
         LiveCombatClient.Ring r = new LiveCombatClient.Ring();
         r.entityId = playerId;
         r.born = ticks;
         r.rgb = e == mc.player && TurnBattleClientMod.clientSoul != null ? TurnBattleClientMod.clientSoul.getColor() & 16777215 : soulOf(e);
         rings.add(r);
         if (e == mc.player) {
            playUi(TurnBattleSounds.GRAZE);
         } else {
            mc.world.playSound(e.getX(), e.getY(), e.getZ(), TurnBattleSounds.GRAZE, SoundCategory.PLAYERS, 0.4F, 1.0F, false);
         }
      }
   }

   private static int soulOf(Entity e) {
      if (e instanceof PlayerEntity p) {
         for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
            if (m.uuid().equals(p.getUuid()) && m.soul() != null) {
               return m.soul().getColor() & 16777215;
            }
         }
      }

      return 16777215;
   }

   private static void onSkill(int actorId, int targetId, int code) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         BattleSkill skill = LiveSkills.decode(code);
         if (skill != null) {
            Entity actor = mc.world.getEntityById(actorId);
            if (nearMe(actor) || nearMe(targetId)) {
               int soulCol = actor == mc.player && TurnBattleClientMod.clientSoul != null
                  ? TurnBattleClientMod.clientSoul.getColor() & 16777215
                  : soulOf(actor);
               int c = skill == LightnerSkill.RED_BUSTER ? 16720418 : soulCol;
               UUID key = OverhaulView.animKeyFor(actor);
               if (key != null) {
                  heldPoses.remove(actorId);
                  CastAnims.Cast cast = CastAnims.castFor(skill);
                  CloneAnimator.play(key, CloneAnimator.Act.SKILL, c, null, cast);
                  OverhaulView.watchLive(key, (cast != null && cast != CastAnims.Cast.GENERIC ? cast.ms : 980L) + 150L);
               }

               int var10000;
               if (skill == LightnerSkill.RED_BUSTER) {
                  var10000 = 16720418;
               } else {
                  label52: {
                     if (skill instanceof SoulMageSkill sms && sms.getSoul() != null) {
                        var10000 = sms.getSoul().getColor() & 16777215;
                        break label52;
                     }

                     var10000 = 0;
                  }
               }

               int fxColor = var10000;
               SkillEffect fx = SkillEffect.forSkill(skill, 0, 0, fxColor);
               if (fx != null) {
                  Entity tgt = mc.world.getEntityById(targetId);
                  fx.volume = tgt != null ? volumeAt(tgt) : 0.6F;
                  LiveCombatClient.Fx f = new LiveCombatClient.Fx();
                  f.skill = fx;
                  f.targetId = targetId;
                  f.at = tgt != null ? tgt.getPos() : null;
                  fxs.add(f);
               }
            }
         }
      }
   }

   private static void onStun(int id, int t) {
      MinecraftClient mc = MinecraftClient.getInstance();
      stunned.put(id, ticks + (long)t);
      if (mc.player != null && id == mc.player.getId()) {
         stunnedUntil = ticks + (long)t;
         if (qteActive && !qteDone) {
            send(8, b -> {
               b.writeBoolean(false);
               b.writeByte(0);
            });
         }

         qteActive = false;
         bashLungeUntil = 0L;
         queued = false;
         step = -1;
         if (guarding) {
            setGuard(false);
         }

         if (menuOpen) {
            closeMenu();
         }
      }

      Entity e = mc.world != null ? mc.world.getEntityById(id) : null;
      if (e != null) {
         mc.world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.8F, 0.6F, false);
      }
   }

   public static boolean isLockedOn(Entity e) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (!(e instanceof PlayerEntity)) {
         return false;
      } else {
         return e == mc.player ? lockedOn() : lockedOnIds.contains(e.getId());
      }
   }

   private static void onArmed(int id, boolean on, NbtCompound nbt) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (on) {
         lockedOnIds.add(id);
      } else {
         lockedOnIds.remove(id);
      }

      if (on && nbt != null && mc.world != null) {
         ItemStack st = ItemStack.fromNbt(mc.world.getRegistryManager(), nbt).orElse(ItemStack.EMPTY);
         if (st.isEmpty()) {
            armed.remove(id);
         } else {
            armed.put(id, st);
         }
      } else {
         armed.remove(id);
      }
   }

   public static ItemStack armedWeapon(Entity e) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (!(e instanceof PlayerEntity)) {
         return null;
      } else if (e == mc.player) {
         if (!lockedOn() && !qteActive && ticks >= bashLungeUntil) {
            return null;
         } else {
            ItemStack w = TurnBattleClientMod.clientEquippedWeapon;
            return w != null && !w.isEmpty() ? w : null;
         }
      } else {
         return armed.get(e.getId());
      }
   }

   private static void startQte(MinecraftClient mc) {
      if (guarding) {
         setGuard(false);
      }

      qteActive = true;
      qteDone = false;
      qteCrit = false;
      qteTickPrev = 0;
      qteTick = 0;
      confirmWasDown = true;
      drain(mc);
      send(8, b -> {
         b.writeBoolean(true);
         b.writeByte(9);
      });
   }

   private static void hitQte(MinecraftClient mc) {
      if (qteActive && !qteDone) {
         float td = mc.getRenderTickCounter().getTickDelta(false);
         float pos = 1.0F - Math.min(1.0F, ((float)qteTickPrev + (float)(qteTick - qteTickPrev) * td) / 20.0F);
         resolveQte(mc, pos);
      }
   }

   private static void resolveQte(MinecraftClient mc, float pos) {
      float dist = Math.abs(pos - 0.1F);
      boolean crit = dist <= 0.02F;
      float mult;
      int tier;
      if (crit) {
         mult = 1.6F;
         tier = 3;
      } else {
         float t = MathHelper.clamp(dist / 0.5F, 0.0F, 1.0F);
         mult = 1.2F - t * 0.9F;
         tier = mult > 1.1F ? 2 : 1;
      }

      qteCrit = crit;
      qtePos = pos;
      qteDone = true;
      qteTickPrev = 0;
      qteTick = 0;
      if (crit) {
         play(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.4F, 1.0F);
      }

      Entity t = lockTarget(mc);
      Vec3d d = t != null ? new Vec3d(t.getX() - mc.player.getX(), 0.0, t.getZ() - mc.player.getZ()) : Vec3d.fromPolar(0.0F, mc.player.getYaw());
      bashDir = d.lengthSquared() < 1.0E-4 ? Vec3d.fromPolar(0.0F, mc.player.getYaw()) : d.normalize();
      bashLungeUntil = ticks + 7L;
      bashReadyAt = ticks + 90L;
      float fm = mult;
      int ft = tier;
      int lid = lockId;
      send(9, b -> {
         b.writeFloat(fm);
         b.writeVarInt(ft);
         b.writeInt(lid);
      });
      LiveFirstPerson.play(LiveFirstPerson.Kind.BASH, 0);
   }

   private static void tickQte(MinecraftClient mc) {
      if (qteActive) {
         qteTickPrev = qteTick++;
         if (!qteDone) {
            boolean down = mc.options.jumpKey.isPressed() || InputUtil.isKeyPressed(mc.getWindow().getHandle(), 257);
            if (down && !confirmWasDown) {
               hitQte(mc);
            }

            confirmWasDown = down;
            if (!qteDone && qteTick >= 20) {
               resolveQte(mc, 1.0F);
            }
         } else if (qteTick >= 8) {
            qteActive = false;
         }
      }
   }

   private static void tickBashLunge(MinecraftClient mc) {
      if (ticks < bashLungeUntil) {
         Entity t = lockTarget(mc);
         if (t != null && mc.player.squaredDistanceTo(t) < Math.pow((double)(t.getWidth() / 2.0F) + 0.9, 2.0)) {
            bashLungeUntil = ticks;
            mc.player.setVelocity(mc.player.getVelocity().multiply(0.2, 1.0, 0.2));
         } else {
            double sp = 1.05;
            mc.player.setVelocity(bashDir.x * sp, mc.player.isOnGround() ? 0.05 : mc.player.getVelocity().y, bashDir.z * sp);
         }
      }
   }

   private static void onPrepare(int mobId, String name, int col, int t) {
      if (t <= 0 || name.isEmpty()) {
         preps.remove(mobId);
      } else if (nearMe(mobId)) {
         LiveCombatClient.Prep p = new LiveCombatClient.Prep();
         p.name = name;
         p.rgb = (col & 16777215) == 0 ? 16777215 : col & 16777215;
         p.born = ticks;
         p.until = ticks + (long)t;
         preps.put(mobId, p);
         MinecraftClient mc = MinecraftClient.getInstance();
         Entity e = mc.world != null ? mc.world.getEntityById(mobId) : null;
         if (e != null) {
            mc.world.playSound(e.getX(), e.getY(), e.getZ(), (SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.HOSTILE, 0.7F, 0.6F, false);
         }
      }
   }

   private static void onMobSkill(int entityId, int typeOrd, int col) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null && typeOrd >= 0 && typeOrd < SkillType.values().length) {
         Entity e = mc.world.getEntityById(entityId);
         if (nearMe(e)) {
            SkillEffect fx = SkillEffect.forMobAttack(SkillType.values()[typeOrd], 0, 0, col == 16777215 ? 0 : col | 0xFF000000);
            if (fx != null) {
               fx.volume = e != null ? volumeAt(e) : 0.6F;
               LiveCombatClient.Fx f = new LiveCombatClient.Fx();
               f.skill = fx;
               f.targetId = entityId;
               f.at = e != null ? e.getPos() : null;
               fxs.add(f);
            }
         }
      }
   }

   private static void onSpare(int mobId) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(mobId) : null;
      if (e != null) {
         if (lockId == mobId) {
            setLock(-1);
         }
      }
   }

   private static void onPose(int playerId, int pose) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(playerId) : null;
      UUID key = OverhaulView.animKeyFor(e);
      if (key != null) {
         switch (pose) {
            case 0:
               heldPoses.remove(playerId);
               CloneAnimator.release(key, CloneAnimator.Act.DEFEND);
               CloneAnimator.releaseMenuPoses(key);
               break;
            case 1:
               heldPoses.put(playerId, CloneAnimator.Act.DEFEND);
               break;
            case 2:
               CloneAnimator.releaseMenuPoses(key);
               heldPoses.put(playerId, CloneAnimator.Act.CHOOSE_SKILL);
               break;
            case 3:
               CloneAnimator.releaseMenuPoses(key);
               heldPoses.put(playerId, CloneAnimator.Act.CHOOSE_ITEM);
               break;
            case 4:
               CloneAnimator.releaseMenuPoses(key);
               heldPoses.put(playerId, CloneAnimator.Act.CHOOSE_ACT);
               break;
            case 5:
               heldPoses.remove(playerId);
               CloneAnimator.release(key, CloneAnimator.Act.DEFEND);
               if (!nearMe(e)) {
                  return;
               }

               for (int i = 0; i < 10; i++) {
                  mc.world
                     .addParticle(
                        ParticleTypes.CLOUD, e.getX() + (Math.random() - 0.5) * 0.6, e.getY() + 0.1, e.getZ() + (Math.random() - 0.5) * 0.6, 0.0, 0.03, 0.0
                     );
               }

               mc.world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.5F, 1.7F, false);
               break;
            case 6:
               heldPoses.remove(playerId);
               CloneAnimator.play(key, CloneAnimator.Act.ITEM, 7143290, null);
               OverhaulView.watchLive(key, 1100L);
               mc.world.playSound(e.getX(), e.getY(), e.getZ(), TurnBattleSounds.SND_ITEM, SoundCategory.PLAYERS, 0.7F, 1.0F, false);
               break;
            case 7:
               heldPoses.remove(playerId);
               CloneAnimator.play(key, CloneAnimator.Act.ACT, 16753722, null);
               OverhaulView.watchLive(key, 1200L);
               break;
            case 8:
               heldPoses.remove(playerId);
               CloneAnimator.play(key, CloneAnimator.Act.SPARE, 16770650, null);
               OverhaulView.watchLive(key, 1250L);
               break;
            case 9:
               CloneAnimator.releaseMenuPoses(key);
               heldPoses.put(playerId, CloneAnimator.Act.BASH_READY);
               break;
            case 10:
               heldPoses.remove(playerId);
               CloneAnimator.releaseMenuPoses(key);
               int c = e == mc.player && TurnBattleClientMod.clientSoul != null ? TurnBattleClientMod.clientSoul.getColor() & 16777215 : soulOf(e);
               CloneAnimator.play(key, CloneAnimator.Act.BASH, c, null);
               OverhaulView.watchLive(key, 780L);
               mc.world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8F, 0.7F, false);
         }

         if (heldPoses.containsKey(playerId)) {
            CloneAnimator.hold(key, heldPoses.get(playerId), colorFor(heldPoses.get(playerId)), null, 1500L);
            OverhaulView.watchLive(key, 400L);
         }
      }
   }

   private static void addNum(int entityId, String text, int rgb, boolean player, float size) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(entityId) : null;
      if (nearMe(e)) {
         LiveCombatClient.Num n = new LiveCombatClient.Num();
         n.entityId = entityId;
         n.text = text;
         n.rgb = rgb;
         n.born = ticks;
         n.player = player;
         n.size = size;
         n.at = e != null ? e.getPos().add(0.0, (double)e.getHeight() * 0.8, 0.0) : null;
         n.jx = (float)(Math.random() * 16.0 - 8.0);
         int same = 0;

         for (LiveCombatClient.Num o : nums) {
            if (o.entityId == entityId && ticks - o.born < 6L) {
               same++;
            }
         }

         n.jx += (float)same * 9.0F;
         if (n.at != null || e != null) {
            nums.add(n);
         }
      }
   }

   private static void addPopup(int entityId, String text, int rgb) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Entity e = mc.world != null ? mc.world.getEntityById(entityId) : null;
      if (e != null && nearMe(e)) {
         LiveCombatClient.Popup p = new LiveCombatClient.Popup();
         p.entityId = entityId;
         p.text = text;
         p.rgb = rgb & 16777215;
         p.born = ticks;
         p.at = e.getPos().add(0.0, (double)e.getHeight() + 0.35, 0.0);
         popups.add(p);
      }
   }

   private static void play(SoundEvent s, float pitch, float vol) {
      if (!(vol <= 0.01F)) {
         MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(s, pitch, vol));
      }
   }

   private static void playUi(SoundEvent s) {
      play(s, 1.0F, 1.0F);
   }

   public static void render(DrawContext ctx, float td) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null && mc.player != null && !(mc.currentScreen instanceof BattleScreen)) {
         try {
            renderWindups(ctx, mc, td);
            renderRings(ctx, mc, td);
            renderFxs(ctx, mc, td);
            renderSpares(ctx, td);
            if (!mc.options.hudHidden) {
               renderPreps(ctx, mc, td);
               renderStuns(ctx, mc, td);
               renderLockGlyph(ctx, mc, td);
               renderQte(ctx, mc, td);
               renderNums(ctx, mc, td);
               renderPopups(ctx, mc, td);
               renderHud(ctx, mc, td);
               renderAllies(ctx, mc, td);
               renderRecipientMark(ctx, mc, td);
            }
         } catch (Throwable var4) {
         }
      }
   }

   private static float[] at(MinecraftClient mc, int entityId, Vec3d fallback, float heightFrac, float td) {
      Entity e = mc.world.getEntityById(entityId);
      if (e != null) {
         return OverhaulView.projectEntity(e, heightFrac, td);
      } else {
         return fallback != null ? OverhaulView.project(fallback) : null;
      }
   }

   private static int argb(int rgb, float a) {
      return (int)(MathHelper.clamp(a, 0.0F, 1.0F) * 255.0F) << 24 | rgb & 16777215;
   }

   private static void renderNums(DrawContext ctx, MinecraftClient mc, float td) {
      TextRenderer tr = mc.textRenderer;
      Style st = Style.EMPTY.withFont(DAMAGE_FONT);

      for (LiveCombatClient.Num n : nums) {
         float prog = ((float)(ticks - n.born) + td) / 40.0F;
         if (!(prog < 0.0F) && !(prog >= 1.0F)) {
            Entity e = mc.world.getEntityById(n.entityId);
            if (e != null) {
               n.at = e.getLerpedPos(td).add(0.0, (double)e.getHeight() * 0.8, 0.0);
            }

            float[] s = n.at != null ? OverhaulView.project(n.at) : null;
            if (s != null) {
               float sx = s[0] + n.jx;
               float sy = s[1];
               float dir = n.player ? -1.0F : 1.0F;
               float reach = n.player ? 10.0F : 25.0F;
               float drawX = prog < 0.45F ? sx + dir * (1.0F - (1.0F - prog / 0.45F) * (1.0F - prog / 0.45F)) * reach : sx + dir * reach;
               float landY = sy - (n.player ? 24.0F : 20.0F);
               float drawY;
               if (prog < 0.45F) {
                  float t = prog / 0.45F;
                  drawY = sy + (landY - sy) * t - 4.0F * t * (1.0F - t) * (n.player ? 30.0F : 40.0F);
               } else if (prog < 0.7F) {
                  drawY = landY;
               } else {
                  drawY = landY - (prog - 0.7F) / 0.3F * 8.0F;
               }

               float scY;
               float scX;
               if (prog < 0.15F) {
                  float t = prog / 0.15F;
                  scX = 1.8F - t * 0.8F;
                  scY = 0.2F + t * 0.8F;
               } else if (prog < 0.7F) {
                  scX = 1.0F;
                  scY = 1.0F;
               } else {
                  scX = 1.0F;
                  scY = 1.0F + (prog - 0.7F) / 0.3F * 0.5F;
               }

               float alpha = prog < 0.7F ? 1.0F : 1.0F - (prog - 0.7F) / 0.3F;
               int a = (int)(alpha * 255.0F);
               if (a > 3) {
                  float size = 2.0F * n.size * MathHelper.clamp(s[2] / 37.0F, 0.55F, 1.15F);
                  Text txt = Text.literal(n.text).setStyle(st);
                  int tw = tr.getWidth(txt);
                  MatrixStack m = ctx.getMatrices();
                  m.push();
                  m.translate(drawX, drawY + 9.0F * size * 0.5F, 400.0F);
                  m.scale(scX * size, scY * size, 1.0F);
                  int x = -tw / 2;
                  int y = -9 / 2;
                  int outline = a << 24;
                  ctx.drawText(tr, txt, x - 1, y, outline, false);
                  ctx.drawText(tr, txt, x + 1, y, outline, false);
                  ctx.drawText(tr, txt, x, y - 1, outline, false);
                  ctx.drawText(tr, txt, x, y + 1, outline, false);
                  ctx.drawText(tr, txt, x, y, a << 24 | n.rgb, false);
                  m.pop();
               }
            }
         }
      }
   }

   private static void renderPopups(DrawContext ctx, MinecraftClient mc, float td) {
      TextRenderer tr = mc.textRenderer;

      for (LiveCombatClient.Popup p : popups) {
         float prog = ((float)(ticks - p.born) + td) / 34.0F;
         if (!(prog >= 1.0F)) {
            Entity e = mc.world.getEntityById(p.entityId);
            if (e != null) {
               p.at = e.getLerpedPos(td).add(0.0, (double)e.getHeight() + 0.35, 0.0);
            }

            float[] s = OverhaulView.project(p.at);
            if (s != null) {
               float pop = prog < 0.12F ? 0.6F + prog / 0.12F * 0.5F : (prog < 0.2F ? 1.1F - (prog - 0.12F) / 0.08F * 0.1F : 1.0F);
               float alpha = prog < 0.7F ? 1.0F : 1.0F - (prog - 0.7F) / 0.3F;
               float y = s[1] - 6.0F - prog * 18.0F;
               MatrixStack m = ctx.getMatrices();
               m.push();
               m.translate(s[0], y, 400.0F);
               m.scale(pop, pop, 1.0F);
               int w = tr.getWidth(p.text);
               ctx.drawText(tr, p.text, -w / 2, -4, argb(p.rgb, alpha), true);
               m.pop();
            }
         }
      }
   }

   private static void renderRings(DrawContext ctx, MinecraftClient mc, float td) {
      for (LiveCombatClient.Ring r : rings) {
         float prog = ((float)(ticks - r.born) + td) / 8.0F;
         if (!(prog >= 1.0F)) {
            Entity e = mc.world.getEntityById(r.entityId);
            if (e != null) {
               if (e == mc.player && mc.options.getPerspective().isFirstPerson()) {
                  drawRing(ctx, (float)mc.getWindow().getScaledWidth() / 2.0F, (float)mc.getWindow().getScaledHeight() / 2.0F, 60.0F, prog, r.rgb);
               } else {
                  float[] s = OverhaulView.projectEntity(e, 0.55F, td);
                  if (s != null) {
                     drawRing(ctx, s[0], s[1], s[2] * 1.7F, prog, r.rgb);
                  }
               }
            }
         }
      }
   }

   private static void drawRing(DrawContext ctx, float cx, float cy, float size, float prog, int rgb) {
      float scale = 1.0F + prog * 0.25F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F, 1.0F - prog);
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(cx, cy, 0.0F);
      m.scale(scale, scale, 1.0F);
      m.translate(-size / 2.0F, -size / 2.0F, 0.0F);
      int sz = Math.max(4, (int)size);
      ctx.drawTexture(GRAZE_RING, 0, 0, 0.0F, 0.0F, sz, sz, sz, sz);
      m.pop();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void renderWindups(DrawContext ctx, MinecraftClient mc, float td) {
      TextRenderer tr = mc.textRenderer;

      for (LiveCombatClient.Windup w : windups) {
         if (ticks <= w.until) {
            Entity e = mc.world.getEntityById(w.mobId);
            float[] s = e != null ? OverhaulView.projectEntity(e, 1.0F, td) : null;
            if (s != null) {
               float t = (float)(ticks - w.born) + td;
               float bounce = t < 3.0F ? (1.0F - t / 3.0F) * 6.0F : 0.0F;
               float sc = MathHelper.clamp(s[2] / 30.0F, 0.9F, 2.2F) * (t < 2.0F ? 0.6F + t * 0.3F : 1.2F);
               MatrixStack m = ctx.getMatrices();
               m.push();
               m.translate(s[0], s[1] - 8.0F - bounce, 300.0F);
               m.scale(sc, sc, 1.0F);
               boolean flash = (int)t / 2 % 2 == 0;
               ctx.drawText(tr, "!", -tr.getWidth("!") / 2, -8, flash ? -48060 : -8892, true);
               m.pop();
            }
         }
      }
   }

   private static void renderSpares(DrawContext ctx, float td) {
      for (LiveCombatClient.Spare sp : spares) {
         float prog = ((float)(ticks - sp.born) + td) / 24.0F;
         if (!(prog >= 1.0F)) {
            float[] s = OverhaulView.project(sp.at);
            if (s != null) {
               float alpha = 1.0F - prog;

               for (int i = 0; i < 8; i++) {
                  double ang = (double)i * Math.PI / 4.0 + (double)prog * 0.8;
                  float r = s[2] * (0.2F + prog * 1.4F);
                  float x = s[0] + (float)Math.cos(ang) * r;
                  float y = s[1] + (float)Math.sin(ang) * r * 0.8F - prog * s[2] * 0.4F;
                  CloneAnimator.star4(ctx, x, y, Math.max(1.5F, s[2] * 0.12F * (1.0F - prog * 0.5F)), argb(16770650, alpha));
               }

               CloneAnimator.glow(ctx, s[0], s[1], s[2] * 0.7F * (1.0F - prog), prog * 2.0F, 16777215, alpha * 0.25F);
            }
         }
      }
   }

   private static void renderFxs(DrawContext ctx, MinecraftClient mc, float td) {
      for (LiveCombatClient.Fx f : fxs) {
         Entity t = mc.world.getEntityById(f.targetId);
         if (t != null) {
            f.at = t.getPos();
         }

         float[] s = t != null ? OverhaulView.projectEntity(t, 0.55F, td) : (f.at != null ? OverhaulView.project(f.at.add(0.0, 0.9, 0.0)) : null);
         if (s != null) {
            float sc = OverhaulView.effectScale(s[2]);
            if (f.skill != null) {
               f.skill.setCenter((int)s[0], (int)s[1]);
               f.skill.scale = sc;
               f.skill.render(ctx, td);
            }

            if (f.bash != null) {
               f.bash.setCenter((int)s[0], (int)s[1]);
               f.bash.scale = sc * f.mul;
               f.bash.render(ctx, td);
            }
         }
      }
   }

   private static void renderStuns(DrawContext ctx, MinecraftClient mc, float td) {
      for (Entry en : stunned.entrySet()) {
         Entity e = mc.world.getEntityById(en.getKey());
         if (e != null && (e != mc.player || !mc.options.getPerspective().isFirstPerson())) {
            Vec3d top = e.getLerpedPos(td).add(0.0, (double)e.getHeight() + 0.25, 0.0);
            float time = (float)ticks + td;
            float left = (float)en.getValue().longValue() - time;
            float alpha = MathHelper.clamp(left / 6.0F, 0.0F, 1.0F);

            for (int i = 0; i < 3; i++) {
               double a = (double)time * 0.35 + (double)i * Math.PI * 2.0 / 3.0;
               float[] s = OverhaulView.project(top.add(Math.cos(a) * (double)e.getWidth() * 0.7, 0.0, Math.sin(a) * (double)e.getWidth() * 0.7));
               if (s != null) {
                  CloneAnimator.star4(ctx, s[0], s[1], Math.max(2.0F, s[2] * 0.13F), argb(16777062, alpha));
               }
            }
         }
      }
   }

   private static void renderQte(DrawContext ctx, MinecraftClient mc, float td) {
      if (qteActive) {
         float x;
         float y;
         if (mc.options.getPerspective().isFirstPerson()) {
            x = (float)mc.getWindow().getScaledWidth() / 2.0F;
            y = (float)mc.getWindow().getScaledHeight() / 2.0F - 40.0F;
         } else {
            float[] s = OverhaulView.project(mc.player.getLerpedPos(td).add(0.0, (double)mc.player.getHeight() + 0.7, 0.0));
            if (s == null) {
               return;
            }

            x = s[0];
            y = s[1];
         }

         int soul = TurnBattleClientMod.clientSoul != null ? TurnBattleClientMod.clientSoul.getColor() & 16777215 : 16777215;
         int barW = 88;
         int barH = 14;
         int bx = (int)(x - (float)barW / 2.0F);
         int by = (int)(y - (float)barH / 2.0F);
         int border = 0xFF000000 | lerpRgb(soul, 0, 0.55F);
         int crit = 0xFF000000 | lerpRgb(soul, 16777215, 0.45F);
         float t = (float)qteTickPrev + (float)(qteTick - qteTickPrev) * td;
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(0.0F, 0.0F, 400.0F);
         float fade = qteDone ? 1.0F - Math.min(1.0F, t / 8.0F) : 1.0F;
         if (!qteDone) {
            TextRenderer tr = mc.textRenderer;
            String hint = "BASH!";
            ctx.drawText(tr, hint, (int)x - tr.getWidth(hint) / 2, by - 11, -1, true);
         }

         int a = (int)(fade * 255.0F) << 24;
         ctx.fill(bx - 1, by - 1, bx + barW + 1, by + barH + 1, border & 16777215 | a);
         ctx.fill(bx, by, bx + barW, by + barH, 1315860 | (int)(fade * 230.0F) << 24);
         int cc = bx + Math.round(0.1F * (float)barW);
         int ch = Math.max(3, Math.round(0.02F * (float)barW));
         ctx.fill(cc - ch, by, cc + ch, by + barH, crit & 16777215 | a);
         if (!qteDone) {
            float pos = 1.0F - Math.min(1.0F, t / 20.0F);

            for (int i = 5; i >= 1; i--) {
               float tp2 = pos + (float)i * 0.018F;
               if (!(tp2 > 1.0F)) {
                  int tx = bx + Math.round(tp2 * (float)barW);
                  ctx.fill(tx - 1, by - 2, tx + 1, by + barH + 2, Math.max(0, 140 - i * 24) << 24 | 16777215);
               }
            }

            int ix = bx + Math.round(pos * (float)barW);
            ctx.fill(ix - 2, by - 3, ix + 2, by + barH + 3, -1);
         } else {
            float e = 1.0F - (1.0F - Math.min(1.0F, t / 8.0F)) * (1.0F - Math.min(1.0F, t / 8.0F)) * (1.0F - Math.min(1.0F, t / 8.0F));
            int grow = 2 + Math.round(e * 16.0F);
            int rx = bx + Math.round(qtePos * (float)barW);
            ctx.fill(
               rx - grow / 2, by - 3 - grow / 3, rx + grow / 2, by + barH + 3 + grow / 3, (int)(255.0F * (1.0F - e)) << 24 | (qteCrit ? 16766720 : 16777215)
            );
         }

         m.pop();
      }
   }

   private static void renderPreps(DrawContext ctx, MinecraftClient mc, float td) {
      if (!preps.isEmpty()) {
         TextRenderer tr = mc.textRenderer;

         for (Entry en : preps.entrySet()) {
            LiveCombatClient.Prep p = en.getValue();
            Entity e = mc.world.getEntityById(en.getKey());
            if (e != null) {
               float[] s = OverhaulView.project(e.getLerpedPos(td).add(0.0, (double)e.getHeight() + 0.55, 0.0));
               if (s != null) {
                  float t = (float)(ticks - p.born) + td;
                  float len = Math.max(1.0F, (float)(p.until - p.born));
                  float charge = MathHelper.clamp(t / len, 0.0F, 1.0F);
                  float pop = t < 3.0F ? 0.5F + t / 3.0F * 0.65F : (t < 5.0F ? 1.15F - (t - 3.0F) / 2.0F * 0.15F : 1.0F);
                  boolean flash = charge > 0.7F && (int)(t / 2.0F) % 2 == 0;
                  int w = tr.getWidth(p.name);
                  int h = 9;
                  MatrixStack m = ctx.getMatrices();
                  m.push();
                  m.translate(s[0], s[1] - 10.0F, 350.0F);
                  m.scale(pop, pop, 1.0F);
                  int bx = -w / 2 - 5;
                  int by = -h - 3;
                  int bw = w + 10;
                  int bh = h + 8;
                  ctx.fill(bx, by, bx + bw, by + bh, -939524096);
                  int border = 0xFF000000 | (flash ? 16777215 : p.rgb);
                  ctx.fill(bx, by, bx + bw, by + 1, border);
                  ctx.fill(bx, by + bh - 1, bx + bw, by + bh, border);
                  ctx.fill(bx, by, bx + 1, by + bh, border);
                  ctx.fill(bx + bw - 1, by, bx + bw, by + bh, border);
                  ctx.drawText(tr, p.name, -w / 2, by + 3, flash ? -1 : 0xFF000000 | p.rgb, true);
                  int fill = (int)((float)(bw - 4) * charge);
                  ctx.fill(bx + 2, by + bh - 3, bx + 2 + fill, by + bh - 2, 0xFF000000 | (flash ? 16777215 : p.rgb));
                  m.pop();
               }
            }
         }
      }
   }

   private static void renderLockGlyph(DrawContext ctx, MinecraftClient mc, float td) {
      float la = MathHelper.lerp(td, prevLockAnim, lockAnim);
      Entity t = lockTarget(mc);
      if (!(la <= 0.01F) && t instanceof LivingEntity le) {
         float[] var6 = OverhaulView.projectEntity(t, 0.5F, td);
         float[] var7 = OverhaulView.projectEntity(t, 1.0F, td);
         if (var6 != null && var7 != null) {
            int soul = TurnBattleClientMod.clientSoul != null ? TurnBattleClientMod.clientSoul.getColor() & 16777215 : 16777215;
            float time = (float)ticks + td;
            float r = MathHelper.clamp(Math.max(var6[2] * t.getHeight() * 0.62F, var6[2] * t.getWidth() * 0.9F), 14.0F, 90.0F) * (1.6F - 0.6F * la);
            float rot = time * 0.04F;
            int col = argb(soul, la * (0.75F + 0.25F * MathHelper.sin(time * 0.25F)));

            for (int k = 0; k < 4; k++) {
               double a = (double)rot + (double)k * Math.PI / 2.0;
               float cx = var6[0] + (float)Math.cos(a) * r;
               float cy = var6[1] + (float)Math.sin(a) * r;
               double a1 = a + (Math.PI * 3.0 / 4.0);
               double a2 = a - (Math.PI * 3.0 / 4.0);
               float len = r * 0.32F;
               CloneAnimator.line(ctx, cx, cy, cx + (float)Math.cos(a1) * len, cy + (float)Math.sin(a1) * len, 2.0F, col);
               CloneAnimator.line(ctx, cx, cy, cx + (float)Math.cos(a2) * len, cy + (float)Math.sin(a2) * len, 2.0F, col);
            }

            CloneAnimator.diamond(ctx, var6[0], var6[1], 2.2F, argb(soul, la * 0.8F));
            TextRenderer tr = mc.textRenderer;
            int barW = 56;
            float x0 = var7[0] - (float)barW / 2.0F;
            float y = var7[1] - 34.0F;
            float alpha = la;
            String name = t.getDisplayName().getString();
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(0.0F, 0.0F, 300.0F);
            int nw = tr.getWidth(name);
            ctx.drawText(tr, name, (int)(var7[0] - (float)nw * 0.4F), (int)y - 1, argb(16777215, la), true);
            m.pop();
            int ix = (int)x0;
            int iy = (int)y + 9;
            float hpFrac = le.getMaxHealth() > 0.0F ? MathHelper.clamp(le.getHealth() / le.getMaxHealth(), 0.0F, 1.0F) : 0.0F;
            ctx.fill(ix - 1, iy - 1, ix + barW + 1, iy + 5, argb(0, la));
            ctx.fill(ix, iy, ix + barW, iy + 4, argb(5900810, la));
            ctx.fill(ix, iy, ix + (int)((float)barW * hpFrac), iy + 4, argb(t instanceof PlayerEntity ? 3398997 : 16765470, la));
            drawSmall(ctx, tr, (int)Math.ceil((double)le.getHealth()) + "/" + (int)le.getMaxHealth(), ix + barW + 3, iy - 1, argb(16777215, la));
            if (!(t instanceof PlayerEntity)) {
               LiveCombatClient.MobInfo mi = mobInfo.get(t.getId());
               iy += 7;
               float mercy = mi != null ? mi.mercy() : 0.0F;
               boolean spareable = mi != null && (mi.spareable() || mercy >= 1.0F);
               ctx.fill(ix - 1, iy - 1, ix + barW + 1, iy + 4, argb(0, la));
               ctx.fill(ix, iy, ix + barW, iy + 3, argb(9059072, la));
               ctx.fill(
                  ix,
                  iy,
                  ix + (int)((float)barW * (spareable ? 1.0F : mercy)),
                  iy + 3,
                  argb(spareable && (int)(time / 4.0F) % 2 == 0 ? 16777215 : 16770650, la)
               );
               drawSmall(ctx, tr, spareable ? "SPARE" : Math.round(mercy * 100.0F) + "%", ix + barW + 3, iy - 2, argb(16770650, la));
               if (mi != null && mi.maxLives() > 0) {
                  for (int i = 0; i < mi.maxLives(); i++) {
                     boolean has = i < mi.lives();
                     CloneAnimator.diamond(ctx, (float)(ix + 3 + i * 7), (float)(iy + 9), 2.4F, argb(has ? 16734810 : 5583667, alpha));
                  }
               }

               if (mi != null && mi.mask() != 0) {
                  int sx = ix + (mi.maxLives() > 0 ? mi.maxLives() * 7 + 4 : 0);
                  String[] names = new String[]{"BURN", "FROZEN", "ZZZ", "CONFUSED", "DEF DOWN", "STUN"};
                  int[] cols = new int[]{16737826, 8974079, 11197951, 16755268, 16746564, 16777062};

                  for (int b = 0; b < names.length; b++) {
                     if ((mi.mask() & 1 << b) != 0) {
                        drawSmall(ctx, tr, names[b], sx, iy + 6, argb(cols[b], alpha));
                        sx += (int)((float)tr.getWidth(names[b]) * 0.6F) + 4;
                     }
                  }
               }
            }
         }
      }
   }

   private static void drawSmall(DrawContext ctx, TextRenderer tr, String s, int x, int y, int argb) {
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate((float)x, (float)y, 300.0F);
      m.scale(0.6F, 0.6F, 1.0F);
      ctx.drawText(tr, s, 0, 0, argb, true);
      m.pop();
   }

   private static void renderHud(DrawContext ctx, MinecraftClient mc, float td) {
      float ha = MathHelper.lerp(td, prevHudAnim, hudAnim);
      if (!(ha <= 0.01F) && live) {
         TextRenderer tr = mc.textRenderer;
         int sw = mc.getWindow().getScaledWidth();
         int sh = mc.getWindow().getScaledHeight();
         float slide = (1.0F - easeOut(ha)) * 65.0F;
         int bottom = sh - 4 + (int)slide;
         int colX = 36;
         float ca = easeOut(MathHelper.lerp(td, prevColAnim, colAnim));
         int colH = Math.round(29.0F + 24.0F * ca);
         int colY = bottom - colH;
         SoulType soul = TurnBattleClientMod.clientSoul;
         int soulRgb = soul != null ? soul.getColor() & 16777215 : 16777215;
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(0.0F, 0.0F, 200.0F);
         int tpX = 4;
         int tpY = bottom - 102;
         RenderSystem.enableBlend();
         ctx.drawTexture(TP_BG, tpX, tpY, 28, 102, 0.0F, 0.0F, 55, 195, 55, 195);
         float shown = MathHelper.lerp(td, prevTpShown, tpShown);
         float frac = MathHelper.clamp(shown / 100.0F, 0.0F, 1.0F);
         int fillPx = (int)(102.0F * frac);
         if (fillPx > 0) {
            ctx.drawTexture(TP_PROG, tpX, tpY + 102 - fillPx, 28, fillPx, 0.0F, (float)(195 - (int)(195.0F * frac)), 55, (int)(195.0F * frac), 55, 195);
         }

         if (menuOpen && list == LiveCombatClient.ListKind.SKILLS && listIdx >= 0 && listIdx < skills.size()) {
            int cost = costOf(skills.get(listIdx));
            float after = Math.max(0.0F, (shown - (float)cost) / 100.0F);
            if (frac > after + 0.001F && cost > 0) {
               int topPx = (int)(102.0F * frac);
               int botPx = (int)(102.0F * after);
               float pulse = 0.35F + 0.35F * MathHelper.sin(((float)ticks + td) * 0.25F);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pulse);
               ctx.drawTexture(
                  TP_WHITE,
                  tpX,
                  tpY + 102 - topPx,
                  28,
                  Math.max(1, topPx - botPx),
                  0.0F,
                  (float)(195 - (int)(195.0F * frac)),
                  55,
                  Math.max(1, (int)(195.0F * (frac - after))),
                  55,
                  195
               );
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }

         m.push();
         m.translate((float)tpX + 5.6F, (float)tpY + 42.84F, 0.0F);
         ctx.drawCenteredTextWithShadow(tr, Text.literal(String.valueOf((int)shown)), 0, 0, -1);
         m.pop();
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor((float)(soulRgb >> 16 & 0xFF) / 255.0F, (float)(soulRgb >> 8 & 0xFF) / 255.0F, (float)(soulRgb & 0xFF) / 255.0F, 1.0F);
         if (colH <= 29) {
            ctx.drawTexture(COL_SLIM, colX, colY, 150, 29, 0.0F, 0.0F, 213, 29, 213, 29);
         } else {
            ctx.enableScissor(colX, colY, colX + 150, bottom);
            ctx.drawTexture(COL_FULL, colX, colY, 150, 53, 0.0F, 0.0F, 213, 53, 213, 53);
            ctx.disableScissor();
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         Identifier skin = mc.player.getSkinTextures().texture();
         int face = 17;
         int fx = colX + 5;
         int fy = colY + (29 - face) / 2;
         ctx.drawTexture(skin, fx, fy, face, face, 8.0F, 8.0F, 8, 8, 64, 64);
         ctx.drawTexture(skin, fx, fy, face, face, 40.0F, 8.0F, 8, 8, 64, 64);
         String name = TurnBattleClientMod.clientVesselName.isEmpty() ? mc.player.getName().getString() : TurnBattleClientMod.clientVesselName;
         int nameX = fx + face + 8;
         int nameY = colY + (29 - 9 * 2) / 2 + 1;
         m.push();
         m.translate((float)nameX, (float)nameY, 0.0F);
         m.scale(1.125F, 1.125F, 1.0F);
         ctx.drawTextWithShadow(tr, Text.literal(name), 0, 0, -1);
         m.pop();
         String sub = guarding
            ? "GUARD"
            : (ticks < comboReadyAt && ticks - lastAtk >= 14L ? "RECOVERING" : (step >= 0 && ticks - lastAtk <= 20L ? "COMBO " + (step + 1) + "/5" : ""));
         if (!sub.isEmpty()) {
            drawSmall(ctx, tr, sub, nameX, nameY + (int)(9.0F * 1.125F) + 1, guarding ? -7350017 : -1);
         }

         float hp = mc.player.getHealth();
         float max = mc.player.getMaxHealth();
         String hpNum = (int)Math.ceil((double)hp) + "/" + (int)max;
         int hpBarW = 37;
         int hpBarH = 5;
         int hpBarX = colX + 150 - 5 - hpBarW;
         int hpBarY = colY + 20 - hpBarH / 2;
         int numW = (int)((float)tr.getWidth(hpNum) * 0.75F);
         m.push();
         m.translate((float)hpBarX + (float)(hpBarW - numW) / 2.0F, (float)hpBarY - 9.0F * 0.75F - 1.0F, 0.0F);
         m.scale(0.75F, 0.75F, 1.0F);
         ctx.drawTextWithShadow(tr, Text.literal(hpNum), 0, 0, -1);
         m.pop();
         float hf = max > 0.0F ? MathHelper.clamp(hp / max, 0.0F, 1.0F) : 0.0F;
         ctx.fill(hpBarX - 1, hpBarY - 1, hpBarX + hpBarW + 1, hpBarY + hpBarH + 1, -15658735);
         ctx.fill(hpBarX, hpBarY, hpBarX + hpBarW, hpBarY + hpBarH, -14540254);
         int fillCol = soul != null ? soul.getColor() | 0xFF000000 : -14501052;
         int hpPx = (int)((float)hpBarW * hf);
         if (hpPx > 0) {
            ctx.fill(hpBarX, hpBarY, hpBarX + hpPx, hpBarY + hpBarH, fillCol);
            ctx.fill(hpBarX, hpBarY, hpBarX + hpPx, hpBarY + 2, 0xFF000000 | lerpRgb(fillCol, 16777215, 0.4F));
         }

         if (colH > 31) {
            ctx.enableScissor(colX, colY, colX + 150, bottom);
            int total = 101;
            int bx0 = colX + (150 - total) / 2;
            int btnBottom = colY + 53 - 3;

            for (int b = 0; b < 4; b++) {
               boolean sel = menuOpen && b == btnIdx;
               int bh = sel ? 24 : 20;
               int bx = bx0 + b * 26;
               int by = btnBottom - 20 + (sel ? 1 : 0);
               Identifier tex = Identifier.of("turnbattle", "textures/gui/" + (sel ? BTN_TEX_SEL[b] : BTN_TEX[b]) + ".png");
               RenderSystem.enableBlend();
               if (buttonOnCooldown(b)) {
                  RenderSystem.setShaderColor(0.38F, 0.38F, 0.38F, 1.0F);
               } else if (b == 3 && lockSpareable(mc)) {
                  float pulse = (float)(Math.sin((double)((float)ticks + td) * 0.15) * 0.5 + 0.5);
                  RenderSystem.setShaderColor(1.0F, 0.6F + pulse * 0.4F, pulse * 0.85F, 1.0F);
               }

               ctx.drawTexture(tex, bx, by, 23, bh, 0.0F, 0.0F, 23, bh, 23, bh);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            ctx.disableScissor();
         }

         float lla = easeOut(MathHelper.lerp(td, prevListAnim, listAnim));
         if (lla > 0.01F && list != LiveCombatClient.ListKind.NONE) {
            renderList(ctx, mc, colX, colY - 4, lla);
         }

         m.pop();
      }
   }

   private static void renderAllies(DrawContext ctx, MinecraftClient mc, float td) {
      float aa = MathHelper.lerp(td, prevAllyAnim, allyAnim);
      if (!(aa <= 0.01F) && live) {
         List party = TurnBattleClientMod.clientPartyPlayers;
         if (!party.isEmpty()) {
            TextRenderer tr = mc.textRenderer;
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            float S = 0.85F;
            int w = Math.round(150.0F * S);
            int h = Math.round(29.0F * S);
            int gap = 2;
            int n = Math.min(party.size(), 5);
            Integer picked = list == LiveCombatClient.ListKind.TARGET && menuOpen && listIdx >= 0 && listIdx < allyTargets.size()
               ? allyTargets.get(listIdx)
               : null;
            double reach = 30.0;
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(0.0F, 0.0F, 200.0F);

            for (int i = 0; i < n; i++) {
               TurnBattleClientMod.PartyMemberInfo pm = party.get(i);
               float a = MathHelper.clamp(aa * 1.4F - (float)i * 0.1F, 0.0F, 1.0F);
               if (!(a <= 0.01F)) {
                  int restY = sh - 4 - h - i * (h + gap);
                  float slide = (1.0F - easeOut(a)) * (float)(sh - restY + 8);
                  PlayerEntity pe = mc.world.getPlayerByUuid(pm.uuid());
                  boolean near = pe != null && pe.isAlive() && mc.player.squaredDistanceTo(pe) <= reach * reach;
                  boolean sel = picked != null && pe != null && picked == pe.getId();
                  int x = sw - 4 - w;
                  float y = (float)restY + slide - (float)(sel ? 3 : 0);
                  float hp = pe != null ? pe.getHealth() : pm.hp();
                  float max = pe != null ? pe.getMaxHealth() : pm.maxHp();
                  SoulType soul = pm.soul();
                  int soulRgb = soul != null ? soul.getColor() & 16777215 : 2276164;
                  float dim = near ? 1.0F : 0.55F;
                  m.push();
                  m.translate((float)x, y, 0.0F);
                  m.scale(S, S, 1.0F);
                  RenderSystem.enableBlend();
                  RenderSystem.setShaderColor(
                     (float)(soulRgb >> 16 & 0xFF) / 255.0F * dim, (float)(soulRgb >> 8 & 0xFF) / 255.0F * dim, (float)(soulRgb & 0xFF) / 255.0F * dim, 1.0F
                  );
                  ctx.drawTexture(COL_SLIM, 0, 0, 150, 29, 0.0F, 0.0F, 213, 29, 213, 29);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  if (sel) {
                     int o = -1;
                     ctx.fill(-1, -1, 151, 0, o);
                     ctx.fill(-1, 29, 151, 30, o);
                     ctx.fill(-1, 0, 0, 29, o);
                     ctx.fill(150, 0, 151, 29, o);
                  }

                  PlayerListEntry entry = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(pm.uuid()) : null;
                  Identifier skin = entry != null ? entry.getSkinTextures().texture() : DefaultSkinHelper.getSkinTextures(pm.uuid()).texture();
                  int face = 17;
                  int fx = 5;
                  int fy = (29 - face) / 2;
                  if (!near) {
                     RenderSystem.setShaderColor(0.55F, 0.55F, 0.55F, 1.0F);
                  }

                  ctx.drawTexture(skin, fx, fy, face, face, 8.0F, 8.0F, 8, 8, 64, 64);
                  ctx.drawTexture(skin, fx, fy, face, face, 40.0F, 8.0F, 8, 8, 64, 64);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  String name = pm.vesselName() != null && !pm.vesselName().isEmpty() ? pm.vesselName() : pm.name();
                  int nameX = fx + face + 8;
                  int nameY = (29 - 9 * 2) / 2 + 1;
                  int hpBarW = 37;
                  int hpBarH = 5;
                  int hpBarX = 145 - hpBarW;
                  int hpBarY = 20 - hpBarH / 2;
                  int maxNameW = hpBarX - 6 - nameX;
                  String shown = tr.getWidth(name) > maxNameW ? tr.trimToWidth(name, Math.max(8, maxNameW - 6)) + "\u2026" : name;
                  ctx.drawTextWithShadow(tr, Text.literal(shown), nameX, nameY, near ? -1 : -6710887);
                  String sub = pe == null ? "AWAY" : (pe.isAlive() && !(hp <= 0.0F) ? (!near ? "FAR" : (sel ? "\u25c0" : "")) : "DOWN");
                  if (!sub.isEmpty()) {
                     drawSmall(ctx, tr, sub, nameX, nameY + 9 + 1, sub.equals("DOWN") ? -43691 : (sel ? -154 : -6710887));
                  }

                  String hpNum = (int)Math.ceil((double)Math.max(0.0F, hp)) + "/" + (int)max;
                  int numW = (int)((float)tr.getWidth(hpNum) * 0.75F);
                  m.push();
                  m.translate((float)hpBarX + (float)(hpBarW - numW) / 2.0F, (float)hpBarY - 9.0F * 0.75F - 1.0F, 0.0F);
                  m.scale(0.75F, 0.75F, 1.0F);
                  ctx.drawTextWithShadow(tr, Text.literal(hpNum), 0, 0, near ? -1 : -5592406);
                  m.pop();
                  float hf = max > 0.0F ? MathHelper.clamp(hp / max, 0.0F, 1.0F) : 0.0F;
                  ctx.fill(hpBarX - 1, hpBarY - 1, hpBarX + hpBarW + 1, hpBarY + hpBarH + 1, -15658735);
                  ctx.fill(hpBarX, hpBarY, hpBarX + hpBarW, hpBarY + hpBarH, -14540254);
                  int fillCol = 0xFF000000 | (near ? soulRgb : lerpRgb(soulRgb, 5592405, 0.5F));
                  int hpPx = (int)((float)hpBarW * hf);
                  if (hpPx > 0) {
                     ctx.fill(hpBarX, hpBarY, hpBarX + hpPx, hpBarY + hpBarH, fillCol);
                     ctx.fill(hpBarX, hpBarY, hpBarX + hpPx, hpBarY + 2, 0xFF000000 | lerpRgb(fillCol, 16777215, 0.4F));
                  }

                  m.pop();
               }
            }

            if (party.size() > n) {
               int topY = sh - 4 - n * (h + gap) - 9 + (int)((1.0F - easeOut(aa)) * (float)(n * (h + gap) + 16));
               String more = "+" + (party.size() - n) + " more";
               ctx.drawTextWithShadow(tr, Text.literal(more), sw - 4 - tr.getWidth(more), topY, -5592406);
            }

            m.pop();
         }
      }
   }

   private static void renderRecipientMark(DrawContext ctx, MinecraftClient mc, float td) {
      if (menuOpen && list == LiveCombatClient.ListKind.TARGET && listIdx >= 0 && listIdx < allyTargets.size()) {
         Entity e = mc.world.getEntityById(allyTargets.get(listIdx));
         if (e != null && (e != mc.player || !mc.options.getPerspective().isFirstPerson() || LiveCamera.active())) {
            float[] top = OverhaulView.projectEntity(e, 1.0F, td);
            if (top != null) {
               float time = (float)ticks + td;
               float la = easeOut(MathHelper.lerp(td, prevListAnim, listAnim));
               int soul = TurnBattleClientMod.clientSoul != null ? TurnBattleClientMod.clientSoul.getColor() & 16777215 : 16777215;
               float y = top[1] - 10.0F - 2.5F * MathHelper.sin(time * 0.3F);
               CloneAnimator.diamond(ctx, top[0], y, 4.0F, argb(0, la * 0.6F));
               CloneAnimator.diamond(ctx, top[0], y, 3.0F, argb(lerpRgb(soul, 16777215, 0.35F), la));
            }
         }
      }
   }

   private static boolean lockSpareable(MinecraftClient mc) {
      if (lockId < 0) {
         return false;
      } else {
         LiveCombatClient.MobInfo mi = mobInfo.get(lockId);
         return mi != null && (mi.spareable() || mi.mercy() >= 1.0F);
      }
   }

   private static void renderList(DrawContext ctx, MinecraftClient mc, int x, int bottomY, float anim) {
      TextRenderer tr = mc.textRenderer;
      int n = listSize();
      int rows = Math.min(6, n);
      int rowH = 11;
      int w = 150;
      int h = rows * rowH + 8 + (list != LiveCombatClient.ListKind.SKILLS && list != LiveCombatClient.ListKind.ACTS ? 0 : 0);
      int y = bottomY - h + (int)((1.0F - anim) * 10.0F);
      int a = (int)(anim * 255.0F);
      ctx.fill(x, y, x + w, y + h, Math.min(a, 235) << 24);
      int border = a << 24 | 16777215;
      ctx.fill(x, y, x + w, y + 1, border);
      ctx.fill(x, y + h - 1, x + w, y + h, border);
      ctx.fill(x, y, x + 1, y + h, border);
      ctx.fill(x + w - 1, y, x + w, y + h, border);
      int soul = TurnBattleClientMod.clientSoul != null ? TurnBattleClientMod.clientSoul.getColor() & 16777215 : 16720418;

      for (int r = 0; r < rows; r++) {
         int i = listScroll + r;
         if (i >= n) {
            break;
         }

         int ry = y + 4 + r * rowH;
         boolean sel = i == listIdx;
         String right = "";
         int col = 16777215;
         String label;
         switch (list) {
            case CHOICE:
               label = i == 0 ? "Skills" : "Acts";
               break;
            case SKILLS:
               BattleSkill sx = skills.get(i);
               label = sx.getDisplayName();
               int cost = costOf(sx);
               right = cost + "% TP";
               if (tp < (float)cost || ticks < skillReadyAt) {
                  col = 7829367;
               }
               break;
            case ACTS:
               ActDefs.ActEntry ex = acts.get(i);
               label = ex.name();
               if (ex.mercyPct() > 0) {
                  right = "+" + ex.mercyPct() + "%";
               }

               if (actOnCooldown(i) || ticks < skillReadyAt) {
                  col = 7829367;
               }
               break;
            case ITEMS:
               ItemStack s = (ItemStack)mc.player.getInventory().main.get(itemSlots.get(i));
               label = s.getName().getString() + (s.getCount() > 1 ? " x" + s.getCount() : "");
               FoodComponent food = (FoodComponent)s.get(DataComponentTypes.FOOD);
               if (food != null) {
                  right = "+" + Math.round(((float)food.nutrition() * 2.0F + food.saturation() * 2.0F) * 2.0F) + " HP";
               }

               if (ticks < itemReadyAt) {
                  col = 7829367;
               }
               break;
            case TARGET:
               Entity e = mc.world.getEntityById(allyTargets.get(i));
               label = allyName(mc, e);
               if (e instanceof LivingEntity le) {
                  right = (int)Math.ceil((double)le.getHealth()) + "/" + (int)le.getMaxHealth();
               }
               break;
            default:
               label = "";
         }

         if (sel) {
            CloneAnimator.diamond(ctx, (float)(x + 7), (float)(ry + 4), 2.6F, argb(soul, anim));
         }

         int maxLabel = w - 16 - (right.isEmpty() ? 0 : tr.getWidth(right) + 6);
         String shownLabel = label;

         while (shownLabel.length() > 1 && tr.getWidth(shownLabel) > maxLabel) {
            shownLabel = shownLabel.substring(0, shownLabel.length() - 1);
         }

         if (!shownLabel.equals(label)) {
            shownLabel = shownLabel.substring(0, Math.max(1, shownLabel.length() - 1)) + "\u2026";
         }

         int c = sel ? (col == 16777215 ? 16777062 : 11184725) : col;
         ctx.drawText(tr, shownLabel, x + 13, ry, argb(c, anim), false);
         if (!right.isEmpty()) {
            ctx.drawText(tr, right, x + w - 5 - tr.getWidth(right), ry, argb(sel ? 16750899 : 13399842, anim), false);
         }
      }

      if (n > rows) {
         if (listScroll > 0) {
            ctx.drawText(tr, "\u25b2", x + w - 9, y - 9, argb(16777215, anim), true);
         }

         if (listScroll + rows < n) {
            ctx.drawText(tr, "\u25bc", x + w - 9, y + h + 1, argb(16777215, anim), true);
         }
      }

      if (list == LiveCombatClient.ListKind.TARGET) {
         String what = "";
         if (giveFrom == LiveCombatClient.ListKind.SKILLS) {
            BattleSkill sx = LiveSkills.decode(giveCode);
            if (sx != null) {
               what = sx.getDisplayName();
            }
         } else if (giveSlot >= 0 && giveSlot < mc.player.getInventory().main.size()) {
            what = ((ItemStack)mc.player.getInventory().main.get(giveSlot)).getName().getString();
         }

         String d = what.isEmpty() ? "Use on whom?" : "Use " + what + " on whom?";
         if (tr.getWidth(d) > 260) {
            d = tr.trimToWidth(d, 255) + "\u2026";
         }

         ctx.drawText(tr, d, x, y - 11, argb(13421772, anim), true);
      }

      if (list == LiveCombatClient.ListKind.ACTS && listIdx >= 0 && listIdx < acts.size()) {
         ActDefs.ActEntry exx = acts.get(listIdx);
         String d = exx.logText() != null && !exx.logText().startsWith("__") ? exx.logText() : "";
         if (!d.isEmpty()) {
            if (tr.getWidth(d) > 260) {
               d = tr.trimToWidth(d, 255) + "\u2026";
            }

            ctx.drawText(tr, d, x, y - 11, argb(13421772, anim), true);
         }
      }
   }

   private static int lerpRgb(int a, int b, float t) {
      int r = MathHelper.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF);
      int g = MathHelper.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF);
      int bl = MathHelper.lerp(t, a & 0xFF, b & 0xFF);
      return r << 16 | g << 8 | bl;
   }

   private static float easeOut(float t) {
      float u = 1.0F - MathHelper.clamp(t, 0.0F, 1.0F);
      return 1.0F - u * u * u;
   }

   private static final class Fx {
      SkillEffect skill;
      BattleScreen.BashEffect bash;
      int targetId;
      Vec3d at;
      float mul = 1.0F;
   }

   private static enum ListKind {
      NONE,
      CHOICE,
      SKILLS,
      ACTS,
      ITEMS,
      TARGET;
   }

   private static record MobInfo(float mercy, int mask, int lives, int maxLives, boolean spareable) {
   }

   private static final class Num {
      int entityId;
      Vec3d at;
      String text;
      int rgb;
      long born;
      float jx;
      boolean player;
      float size = 1.0F;
   }

   private static final class Popup {
      int entityId;
      Vec3d at;
      String text;
      int rgb;
      long born;
   }

   private static final class Prep {
      String name;
      int rgb;
      long born;
      long until;
   }

   private static final class Ring {
      int entityId;
      Vec3d at;
      int rgb;
      long born;
   }

   private static final class Spare {
      Vec3d at;
      long born;
   }

   private static final class Windup {
      int mobId;
      long until;
      long born;
   }
}
