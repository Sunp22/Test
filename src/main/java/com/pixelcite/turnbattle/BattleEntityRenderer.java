package com.pixelcite.turnbattle;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.SkinTextures.Model;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class BattleEntityRenderer {
   private static final Map<UUID, BattleEntityRenderer.BattleAnim> playerAnims = new HashMap<>();
   private static final Map<UUID, Integer> animTicks = new HashMap<>();
   private static final int HURT_TICKS = 12;
   private static boolean renderingEntity = false;
   private static final Map<Integer, LivingEntity> lastKnownEnemies = new HashMap<>();
   private static final UUID RECRUIT_UUID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
   private static final UUID RECRUIT_UUID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
   private static final int DIAG_STEP_X = 18;
   private static final int DIAG_STEP_Y = 55;
   private static final float PLAYER_SCALE = 29.6F;
   private static final float ENEMY_SCALE = 37.0F;
   private static final Map<String, BattleEntityRenderer.RecruitFakePlayer> recruitFakePlayers = new HashMap<>();
   private static final Map<String, Object> fakePlayers = new HashMap<>();
   private static final Map<UUID, BattleEntityRenderer.PvpFakePlayer> pvpFakePlayers = new HashMap<>();
   static IntFunction<float[]> playerMotion = null;
   static IntFunction<float[]> enemyMotion = null;
   static IntFunction<int[]> ebAnchor = null;
   private static float cloneYawOverride = Float.NaN;
   private static float cloneScaleOverride = Float.NaN;
   private static boolean stillClone = false;
   private static int[] lookAt = null;
   private static final float[] STILL = new float[]{0.0F, 0.0F, 1.0F};

   public static void setAnim(UUID uuid, BattleEntityRenderer.BattleAnim anim) {
      if (uuid != null && anim != null) {
         switch (anim) {
            case IDLE:
               CloneAnimator.release(uuid, CloneAnimator.Act.DEFEND);
               playerAnims.remove(uuid);
               animTicks.remove(uuid);
               return;
            case SWINGING:
               CloneAnimator.play(uuid, CloneAnimator.Act.BASH, 16777215, null);
               return;
            case DEFENDING:
               CloneAnimator.play(uuid, CloneAnimator.Act.DEFEND, 9427199, null);
               return;
            case HURT:
            default:
               playerAnims.put(uuid, anim);
               if (anim == BattleEntityRenderer.BattleAnim.HURT) {
                  animTicks.put(uuid, 12);
               }

               return;
            case CROUCHING:
         }
      }
   }

   public static BattleEntityRenderer.BattleAnim getAnim(UUID uuid) {
      return CloneAnimator.heldOf(uuid) == CloneAnimator.Act.DEFEND
         ? BattleEntityRenderer.BattleAnim.DEFENDING
         : playerAnims.getOrDefault(uuid, BattleEntityRenderer.BattleAnim.IDLE);
   }

   private static BattleEntityRenderer.BattleAnim baseAnim(UUID uuid) {
      BattleEntityRenderer.BattleAnim a = playerAnims.getOrDefault(uuid, BattleEntityRenderer.BattleAnim.IDLE);
      return a != BattleEntityRenderer.BattleAnim.HURT && a != BattleEntityRenderer.BattleAnim.DYING ? BattleEntityRenderer.BattleAnim.IDLE : a;
   }

   public static void tick() {
      animTicks.entrySet().removeIf(e -> {
         int rem = e.getValue() - 1;
         if (rem <= 0) {
            playerAnims.remove(e.getKey());
            return true;
         } else {
            e.setValue(rem);
            return false;
         }
      });
   }

   public static boolean isRenderingEntity() {
      return renderingEntity;
   }

   public static void setRenderingEntity(boolean v) {
      renderingEntity = v;
   }

   static LivingEntity cachedEnemy(int networkId) {
      return lastKnownEnemies.get(networkId);
   }

   public static void clearAll() {
      playerAnims.clear();
      animTicks.clear();
      fakePlayers.clear();
      recruitFakePlayers.clear();
      lastKnownEnemies.clear();
      CloneAnimator.clearAll();
   }

   static boolean isRecruitUuid(UUID uuid) {
      return uuid == null || RECRUIT_UUID_1.equals(uuid) || RECRUIT_UUID_2.equals(uuid);
   }

   public static UUID recruitKey(String recruitName) {
      return UUID.nameUUIDFromBytes(("anim:" + recruitName).getBytes());
   }

   public static BattleEntityRenderer.BattleAnim getRecruitAnim(String recruitName) {
      return getAnim(recruitKey(recruitName));
   }

   public static void setRecruitAnim(String recruitName, BattleEntityRenderer.BattleAnim anim) {
      setAnim(recruitKey(recruitName), anim);
   }

   public static int[] participantPos(int i, int total, int screenW, int screenH, int textboxH, boolean isPlayer) {
      if (BattleScreen.earthboundLayout()) {
         return earthboundPos(i, total, screenW, screenH, isPlayer);
      } else if (BattleScreen.lightWorldLayout()) {
         return lightWorldPos(i, total, screenW, screenH, isPlayer);
      } else {
         int topMargin = 20;
         int areaH = screenH - textboxH - topMargin;
         int areaCenterY = isPlayer ? topMargin + areaH / 2 + 10 : topMargin + areaH / 2;
         boolean reversed = TurnBattleConfig.get().reverseBattleLayout;
         boolean onLeftSide = reversed != isPlayer;
         int baseX = onLeftSide ? (int)((float)screenW * 0.2F) : (int)((float)screenW * 0.75F);
         int row = i / 3;
         int posInRow = i % 3;
         int rowStart = row * 3;
         int localRowTotal = Math.min(3, total - rowStart);
         int ROW_BACK_X = 14;
         int ROW_BACK_Y = -10;
         int rowShiftX = row * 14 * (onLeftSide ? -1 : 1);
         int rowShiftY = row * -10;
         float offset = (float)posInRow - (float)(localRowTotal - 1) / 2.0F;
         int cy = areaCenterY + (int)(offset * 55.0F) + rowShiftY;
         int cx = baseX + (int)(offset * (float)(onLeftSide ? 18 : -18)) + rowShiftX;
         return new int[]{cx, cy};
      }
   }

   private static int[] lightWorldPos(int i, int total, int screenW, int screenH, boolean isPlayer) {
      if (isPlayer) {
         int vis = Math.max(1, Math.min(total, 3));
         int totalW = vis * 150 + (vis - 1) * 3;
         int startX = (screenW - totalW) / 2;
         int idx = Math.max(0, Math.min(i, vis - 1));
         int colTop = screenH - 53;
         return new int[]{startX + idx * 153 + 75, colTop + 46};
      } else {
         int n = Math.max(1, total);
         int arenaTop = 14;
         int arenaBottom = screenH - 53 - 74 - 6;
         int midY = (arenaTop + arenaBottom) / 2;
         int step = Math.max(28, Math.min(120, (screenW - 70) / n));
         int cx = Math.round((float)screenW / 2.0F + ((float)i - (float)(n - 1) / 2.0F) * (float)step);
         int cy = midY + Math.round(46.25F);
         return new int[]{cx, cy};
      }
   }

   private static int[] earthboundPos(int i, int total, int screenW, int screenH, boolean isPlayer) {
      if (isPlayer) {
         int fit = Math.max(1, (screenW - 8 + 6) / 98);
         int vis = Math.max(1, Math.min(total, Math.min(4, fit)));
         int totalW = vis * 92 + (vis - 1) * 6;
         int idx = Math.max(0, Math.min(i, vis - 1));
         int cx = (screenW - totalW) / 2 + idx * 98 + 46;
         int top = screenH - 44 + 8;
         return new int[]{cx, top + 40};
      } else {
         int n = Math.max(1, total);
         int arenaTop = 90;
         int arenaBottom = screenH - 44 + 8 - 12 - 4;
         int midY = (arenaTop + arenaBottom) / 2;
         int step = Math.max(28, Math.min(120, (screenW - 70) / n));
         int cx = Math.round((float)screenW / 2.0F + ((float)i - (float)(n - 1) / 2.0F) * (float)step);
         return new int[]{cx, midY + Math.round(46.25F)};
      }
   }

   private static float enemyYaw(float classic) {
      return !BattleScreen.lightWorldLayout() && !BattleScreen.earthboundLayout() ? classic : 0.0F;
   }

   public static void renderEntity(
      DrawContext ctx, LivingEntity entity, int cx, int cy, float scale, float playerYawDeg, BattleEntityRenderer.BattleAnim anim, ItemStack holdItem
   ) {
      renderEntity(ctx, entity, cx, cy, scale, playerYawDeg, anim, holdItem, null, 1);
   }

   public static void renderEntity(
      DrawContext ctx,
      LivingEntity entity,
      int cx,
      int cy,
      float scale,
      float playerYawDeg,
      BattleEntityRenderer.BattleAnim anim,
      ItemStack holdItem,
      CloneAnimator.Frame frame,
      int dir
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         if (anim == BattleEntityRenderer.BattleAnim.DYING) {
            frame = null;
         }

         if (stillClone) {
            frame = null;
            anim = BattleEntityRenderer.BattleAnim.IDLE;
            holdItem = null;
         }

         boolean defending = anim == BattleEntityRenderer.BattleAnim.DEFENDING || frame != null && frame.defending;
         boolean handOverridden = frame != null && frame.handOverride != null;
         if (handOverridden) {
            holdItem = frame.handOverride;
         }

         float savBodyYaw = entity.bodyYaw;
         float savHeadYaw = entity.headYaw;
         float savPitch = entity.getPitch();
         boolean savSneak = entity.isSneaking();
         float savSwing = entity.handSwingProgress;
         float savSwingL = entity.lastHandSwingProgress;
         int savSwingT = entity.handSwingTicks;
         ItemStack savMain = null;
         ItemStack savOff = null;
         if (holdItem != null && (handOverridden || !holdItem.isEmpty()) && entity instanceof PlayerEntity pe) {
            savMain = ItemStack.EMPTY;
            HandSwaps.showMain(pe, holdItem);
         }

         boolean emptied = false;
         if (stillClone && entity instanceof PlayerEntity peS) {
            HandSwaps.showMain(peS, ItemStack.EMPTY);
            HandSwaps.showOff(peS, ItemStack.EMPTY);
            emptied = true;
         }

         boolean flinchFlash = frame != null && frame.hurtFlash() && anim != BattleEntityRenderer.BattleAnim.HURT;
         int savHurtTime = entity.hurtTime;
         if (flinchFlash) {
            entity.hurtTime = 10;
         }

         switch (anim) {
            case SWINGING:
               entity.handSwingProgress = 0.5F;
               entity.lastHandSwingProgress = 0.0F;
               entity.handSwingTicks = 4;
               if (entity instanceof PlayerEntity pe0) {
                  pe0.preferredHand = Hand.MAIN_HAND;
               }
            case DEFENDING:
            default:
               break;
            case HURT:
               entity.hurtTime = 10;
               break;
            case CROUCHING:
               if (entity instanceof PlayerEntity pe2) {
                  pe2.setSneaking(true);
               }
               break;
            case DYING:
               entity.deathTime = 10;
         }

         if (defending && entity instanceof PlayerEntity pe3) {
            HandSwaps.showOff(pe3, new ItemStack(Items.SHIELD));
            pe3.setCurrentHand(Hand.OFF_HAND);
            if (pe3 instanceof BattleEntityRenderer.RecruitFakePlayer rfp) {
               rfp.forceBlocking = true;
            }

            if (pe3 instanceof BattleEntityRenderer.PvpFakePlayer pfp) {
               pfp.forceBlocking = true;
            }
         }

         float yawRad = (float)Math.toRadians((double)playerYawDeg);
         int mouseX = cx + (int)(Math.sin((double)yawRad) * 800.0);
         int mouseY = cy;
         if (lookAt != null) {
            mouseX = lookAt[0];
            mouseY = lookAt[1];
         }

         int x1 = cx - (int)(scale * 1.5F);
         int x2 = cx + (int)(scale * 1.5F);
         int y2 = cy;
         int y1 = cy - (int)(scale * 2.5F);
         float hPx = entity.getHeight() * scale;
         float feetY = (float)(y1 + cy) / 2.0F + hPx / 2.0F;
         CloneAnimator.Pose pose = frame != null ? frame.pose : null;
         if (frame != null) {
            CloneAnimator.drawFx(ctx, frame, (float)cx, feetY, hPx, dir, false);
         }

         boolean darkWorld = DarkFountainClient.currentTintColor >= 0;
         boolean realGrade = darkWorld && DarkWorldModelTint.begin(ctx);
         float cr = 1.0F;
         float cg = 1.0F;
         float cb = 1.0F;
         if (darkWorld && !realGrade) {
            int tint = DarkFountainClient.currentTintColor;
            float tr = (float)(tint >> 16 & 0xFF) / 255.0F;
            float tg = (float)(tint >> 8 & 0xFF) / 255.0F;
            float tb = (float)(tint & 0xFF) / 255.0F;
            float BLEND = 0.5F;
            cr = 0.5F + 0.5F * tr;
            cg = 0.5F + 0.5F * tg;
            cb = 0.5F + 0.5F * tb;
         }

         float dim = pose != null ? pose.dim() : 0.0F;
         if (dim > 0.001F) {
            cr *= 1.0F - dim;
            cg *= 1.0F - dim * 0.95F;
            cb *= 1.0F - dim * 0.75F;
         }

         float ca = 1.0F;
         if (GhostRender.active) {
            cr *= GhostRender.r;
            cg *= GhostRender.g;
            cb *= GhostRender.b;
            ca = GhostRender.a;
         }

         float layAlpha = 1.0F;
         if (realGrade) {
            layAlpha = ca;
            ca = 1.0F;
         }

         boolean tinted = darkWorld && !realGrade || dim > 0.001F || GhostRender.active;
         if (tinted) {
            RenderSystem.setShaderColor(cr, cg, cb, ca);
         }

         boolean transformed = pose != null && pose.movesOnScreen();
         MatrixStack matrices = ctx.getMatrices();
         if (transformed) {
            matrices.push();
            matrices.translate((float)cx + pose.dx() * (float)dir, feetY + pose.dy(), 0.0F);
            if (pose.tilt() != 0.0F) {
               matrices.multiply(new Quaternionf().rotationZ((float)Math.toRadians((double)(pose.tilt() * (float)dir))));
            }

            matrices.scale(pose.sx(), pose.sy(), 1.0F);
            matrices.translate((float)(-cx), -feetY, 0.0F);
         }

         try {
            renderingEntity = true;
            if (pose != null) {
               CloneAnimator.beginModelPose(entity, pose);
            }

            InventoryScreen.drawEntity(ctx, x1, y1, x2, y2, (int)scale, 0.0F, (float)mouseX, (float)mouseY, entity);
         } catch (Exception var50) {
         } finally {
            CloneAnimator.endModelPose();
            renderingEntity = false;
            if (transformed) {
               matrices.pop();
            }

            if (tinted) {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            if (realGrade) {
               DarkWorldModelTint.end(ctx, layAlpha);
            }
         }

         if (frame != null) {
            CloneAnimator.drawFx(ctx, frame, (float)cx, feetY, hPx, dir, true);
         }

         entity.bodyYaw = savBodyYaw;
         entity.headYaw = savHeadYaw;
         entity.handSwingProgress = savSwing;
         entity.lastHandSwingProgress = savSwingL;
         entity.handSwingTicks = savSwingT;
         if (anim == BattleEntityRenderer.BattleAnim.CROUCHING && entity instanceof PlayerEntity pe4) {
            pe4.setSneaking(savSneak);
         }

         if (defending && entity instanceof PlayerEntity pe5) {
            HandSwaps.restoreOff(pe5);
            pe5.clearActiveItem();
            if (pe5 instanceof BattleEntityRenderer.RecruitFakePlayer rfp2) {
               rfp2.forceBlocking = false;
            }

            if (pe5 instanceof BattleEntityRenderer.PvpFakePlayer pfp2) {
               pfp2.forceBlocking = false;
            }
         }

         if (anim == BattleEntityRenderer.BattleAnim.SWINGING && entity instanceof PlayerEntity pe6) {
            pe6.preferredHand = Hand.MAIN_HAND;
         }

         if (anim == BattleEntityRenderer.BattleAnim.HURT) {
            entity.hurtTime = 0;
         }

         if (flinchFlash) {
            entity.hurtTime = savHurtTime;
         }

         if (anim == BattleEntityRenderer.BattleAnim.DYING) {
            entity.deathTime = 0;
         }

         if (savMain != null && entity instanceof PlayerEntity pe6) {
            HandSwaps.restoreMain(pe6);
         }

         if (emptied && entity instanceof PlayerEntity peR) {
            HandSwaps.restoreMain(peR);
            HandSwaps.restoreOff(peR);
         }
      }
   }

   static BattleEntityRenderer.RecruitFakePlayer recruitFake(String recruitName) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Identifier skinId = RecruitSkinManager.getSkin(recruitName);
      BattleEntityRenderer.RecruitFakePlayer fake = recruitFakePlayers.get(recruitName);
      if (fake == null || fake.getWorld() != mc.world) {
         fake = new BattleEntityRenderer.RecruitFakePlayer(mc, recruitName, skinId);
         recruitFakePlayers.put(recruitName, fake);
      }

      fake.updateSkin(skinId);
      return fake;
   }

   static ItemStack recruitWeapon(String recruitName) {
      ItemStack weapon = TurnBattleClientMod.getRemoteRecruitWeapon(recruitName);
      if (weapon != null) {
         return weapon;
      } else {
         String[] rnames = TurnBattleClientMod.customRecruitNames;
         ItemStack[] rweapons = TurnBattleClientMod.customRecruitWeapons;

         for (int i = 0; i < rnames.length && i < rweapons.length; i++) {
            if (recruitName.equals(rnames[i]) && rweapons[i] != null && !rweapons[i].isEmpty()) {
               return rweapons[i];
            }
         }

         return ItemStack.EMPTY;
      }
   }

   static boolean isArmed(ItemStack equipped, LivingEntity entity) {
      return equipped != null && !equipped.isEmpty() ? true : entity != null && isWeapon(entity.getMainHandStack());
   }

   static boolean isWeapon(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item it = stack.getItem();
         return it instanceof ToolItem || it instanceof TridentItem || it instanceof MaceItem || it instanceof RangedWeaponItem;
      } else {
         return false;
      }
   }

   private static void renderRecruitClone(
      DrawContext ctx, String recruitName, int cx, int cy, BattleEntityRenderer.BattleAnim anim, CloneAnimator.Frame frame, int dir
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         ItemStack weapon = recruitWeapon(recruitName);
         Identifier skinId = RecruitSkinManager.getSkin(recruitName);
         BattleEntityRenderer.RecruitFakePlayer fake = recruitFakePlayers.computeIfAbsent(
            recruitName, k -> new BattleEntityRenderer.RecruitFakePlayer(mc, k, skinId)
         );
         fake.updateSkin(skinId);
         fake.bodyYaw = mc.player.bodyYaw;
         fake.headYaw = mc.player.headYaw;
         fake.handSwingProgress = mc.player.handSwingProgress;
         fake.lastHandSwingProgress = mc.player.lastHandSwingProgress;
         fake.handSwingTicks = mc.player.handSwingTicks;
         float recruitYaw = !Float.isNaN(cloneYawOverride) ? cloneYawOverride : (TurnBattleConfig.get().reverseBattleLayout ? -45.0F : 45.0F);
         float recruitScale = !Float.isNaN(cloneScaleOverride) ? cloneScaleOverride : 29.6F;
         renderEntity(ctx, fake, cx, cy, recruitScale, recruitYaw, anim, weapon, frame, dir);
      }
   }

   private static BattleEntityRenderer.PvpFakePlayer getPvpFakePlayer(MinecraftClient mc, UUID uuid, String name) {
      BattleEntityRenderer.PvpFakePlayer existing = pvpFakePlayers.get(uuid);
      if (existing == null) {
         existing = new BattleEntityRenderer.PvpFakePlayer(mc, uuid, name);
         pvpFakePlayers.put(uuid, existing);
      }

      return existing;
   }

   public static void clearPvpFakePlayers() {
      pvpFakePlayers.clear();
   }

   public static boolean isStillClone() {
      return stillClone;
   }

   private static float[] motion(IntFunction<float[]> f, int i) {
      if (f == null) {
         return STILL;
      } else {
         float[] m = f.apply(i);
         return m != null ? m : STILL;
      }
   }

   private static boolean pushScale(DrawContext ctx, int cx, int cy, float s) {
      if (s <= 0.02F) {
         return false;
      } else {
         ctx.getMatrices().push();
         if (s != 1.0F) {
            ctx.getMatrices().translate((float)cx, (float)cy, 0.0F);
            ctx.getMatrices().scale(s, s, s);
            ctx.getMatrices().translate((float)(-cx), (float)(-cy), 0.0F);
         }

         return true;
      }
   }

   public static void renderBattle3D(DrawContext ctx, BattleScreen screen, int screenW, int screenH, int textboxH, float tickDelta) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         List participants = screen.getParticipants();
         List uuids = screen.getParticipantUuids();
         int total = participants.size();
         boolean lightWorld = BattleScreen.lightWorldLayout();
         boolean earthbound = BattleScreen.earthboundLayout();
         cloneYawOverride = earthbound ? 0.0F : Float.NaN;
         cloneScaleOverride = earthbound ? 38.0F : Float.NaN;
         stillClone = earthbound;

         try {
            for (int i = 0; i < total && !lightWorld; i++) {
               BattleScreen.ParticipantInfo p = participants.get(i);
               UUID uuid = i < uuids.size() ? uuids.get(i) : null;
               int[] pos = participantPos(i, total, screenW, screenH, textboxH, true);
               float[] mo = motion(playerMotion, i);
               int cx = Math.max(55, pos[0]) + screen.getPlayerShakeX(i) + Math.round(mo[0]);
               int cy = pos[1] + Math.round(mo[1]);
               if (earthbound) {
                  int[] an = ebAnchor != null ? ebAnchor.apply(i) : null;
                  if (an == null) {
                     continue;
                  }

                  cx = an[0];
                  cy = an[1];
                  mo = STILL;
                  lookAt = new int[]{cx, Math.round((float)cy - 47.5F)};
               }

               BattleEntityRenderer.BattleAnim anim = uuid != null ? baseAnim(uuid) : BattleEntityRenderer.BattleAnim.IDLE;
               int playerDir = TurnBattleConfig.get().reverseBattleLayout ? -1 : 1;
               if (isRecruitUuid(uuid)) {
                  UUID rKey = recruitKey(p.name());
                  BattleEntityRenderer.BattleAnim recruitAnim = baseAnim(rKey);
                  if (screen.isPvpMode()) {
                     CloneAnimator.observeTurn(rKey, p.isCurrentTurn());
                  }

                  CloneAnimator.observeHp(rKey, p.hp(), screen.participantShownDown(i));
                  CloneAnimator.setSoul(rKey, p.soul());
                  CloneAnimator.Frame rFrame = CloneAnimator.frame(rKey, isWeapon(recruitWeapon(p.name())));
                  if (pushScale(ctx, cx, cy, mo[2])) {
                     renderRecruitClone(ctx, p.name(), cx, cy, recruitAnim, rFrame, playerDir);
                     ctx.getMatrices().pop();
                  }

                  screen.set3DPlayerPos(i, cx, cy);
               } else {
                  AbstractClientPlayerEntity playerEnt = null;

                  for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
                     if (pl.getUuid().equals(uuid)) {
                        playerEnt = pl;
                        break;
                     }
                  }

                  if (playerEnt == null && p.isMe()) {
                     playerEnt = mc.player;
                  }

                  if (playerEnt != null) {
                     ItemStack weapon = p.isMe() ? TurnBattleClientMod.clientEquippedWeapon : TurnBattleClientMod.getRemoteEquippedWeapon(uuid);
                     float playerYaw = !Float.isNaN(cloneYawOverride) ? cloneYawOverride : (TurnBattleConfig.get().reverseBattleLayout ? -45.0F : 45.0F);
                     float playerScale = !Float.isNaN(cloneScaleOverride) ? cloneScaleOverride : 29.6F;
                     if (screen.isPvpMode()) {
                        CloneAnimator.observeTurn(uuid, p.isCurrentTurn());
                     }

                     CloneAnimator.observeHp(uuid, p.hp(), screen.participantShownDown(i));
                     CloneAnimator.setSoul(uuid, p.soul());
                     CloneAnimator.Frame frame = CloneAnimator.frame(uuid, isArmed(weapon, playerEnt));
                     if (pushScale(ctx, cx, cy, mo[2])) {
                        renderEntity(ctx, playerEnt, cx, cy, playerScale, playerYaw, anim, weapon, frame, playerDir);
                        ctx.getMatrices().pop();
                     }

                     screen.set3DPlayerPos(i, cx, cy);
                  }
               }
            }
         } finally {
            cloneYawOverride = Float.NaN;
            cloneScaleOverride = Float.NaN;
            stillClone = false;
            lookAt = null;
         }

         if (screen.isPvpMode()) {
            List opponents = screen.getPvpOpponents();
            int totalOpp = opponents.size();

            for (int i = 0; i < totalOpp; i++) {
               BattleScreen.OpponentInfo o = opponents.get(i);
               UUID opponentUuid = o.uuid();
               if (opponentUuid != null) {
                  BattleEntityRenderer.PvpFakePlayer fake = getPvpFakePlayer(mc, opponentUuid, o.name());
                  fake.bodyYaw = 180.0F - mc.player.bodyYaw;
                  fake.headYaw = 180.0F - mc.player.headYaw;
                  int[] posx = participantPos(i, totalOpp, screenW, screenH, textboxH, false);
                  float[] mox = motion(enemyMotion, i);
                  posx = new int[]{posx[0], posx[1] + Math.round(mox[1])};
                  int ecx = posx[0] + screen.getEnemyShakeX(i) + Math.round(mox[0]);
                  BattleEntityRenderer.BattleAnim anim = screen.isEnemyHurt(i) ? BattleEntityRenderer.BattleAnim.HURT : baseAnim(opponentUuid);
                  CloneAnimator.observeHp(opponentUuid, o.hp(), o.hp() <= 0.0F || o.downed());
                  ItemStack oppWeapon = TurnBattleClientMod.getRemoteEquippedWeapon(opponentUuid);
                  CloneAnimator.Frame frame = CloneAnimator.frame(opponentUuid, isArmed(oppWeapon, fake));
                  int oppDir = TurnBattleConfig.get().reverseBattleLayout ? 1 : -1;
                  float enemyYaw = enemyYaw(TurnBattleConfig.get().reverseBattleLayout ? 60.0F : -60.0F);
                  if (pushScale(ctx, ecx, posx[1], mox[2])) {
                     renderEntity(ctx, fake, ecx, posx[1], 29.6F, enemyYaw, anim, oppWeapon, frame, oppDir);
                     ctx.getMatrices().pop();
                  }

                  screen.set3DEnemyPos(i, ecx, posx[1]);
               }
            }
         } else {
            List enemies = screen.getEnemies();
            int enemyTotal = enemies.size();

            for (int ix = 0; ix < enemyTotal; ix++) {
               BattleScreen.EnemyInfo e = enemies.get(ix);
               Entity raw = mc.world.getEntityById(e.networkId());
               if (raw instanceof LivingEntity mob) {
                  lastKnownEnemies.put(e.networkId(), mob);
               } else {
                  raw = (Entity)lastKnownEnemies.get(e.networkId());
                  if (!(raw instanceof LivingEntity)) {
                     MobFates.Fate f = MobFates.get(e.networkId());
                     if (f != null) {
                        raw = f.e;
                     }
                  }
               }

               if (raw instanceof LivingEntity mob) {
                  int[] posx = participantPos(ix, enemyTotal, screenW, screenH, textboxH, false);
                  float[] mox = motion(enemyMotion, ix);
                  posx = new int[]{posx[0], posx[1] + Math.round(mox[1])};
                  int ecx = posx[0] + screen.getEnemyShakeX(ix) + Math.round(mox[0]);
                  float mobYaw = enemyYaw(TurnBattleConfig.get().reverseBattleLayout ? 60.0F : -60.0F);
                  MobFates.Fate fate = MobFates.get(e.networkId());
                  if (fate != null && fate.auto && e.hp() > 0.0F) {
                     MobFates.drop(e.networkId());
                     fate = null;
                  }

                  if (fate == null && e.hp() <= 0.0F) {
                     fate = MobFates.autoKill(e.networkId(), mob);
                  }

                  if (fate == null && !(e.hp() <= 0.0F)) {
                     BattleEntityRenderer.BattleAnim anim = screen.isEnemyHurt(ix)
                        ? BattleEntityRenderer.BattleAnim.HURT
                        : BattleEntityRenderer.BattleAnim.IDLE;
                     if (pushScale(ctx, ecx, posx[1], mox[2])) {
                        renderEntity(ctx, mob, ecx, posx[1], 37.0F, mobYaw, anim, null);
                        ctx.getMatrices().pop();
                     }

                     screen.set3DEnemyPos(ix, ecx, posx[1]);
                     screen.set3DEnemyTop(ix, Math.round((float)posx[1] - 46.25F - mob.getHeight() * 37.0F / 2.0F));
                  } else {
                     if (fate != null && pushScale(ctx, ecx, posx[1], mox[2])) {
                        MobFates.renderGui(ctx, fate, mob, ecx, posx[1], 37.0F, mobYaw);
                        ctx.getMatrices().pop();
                     }

                     screen.set3DEnemyPos(ix, ecx, posx[1]);
                  }
               }
            }
         }
      }
   }

   public static enum BattleAnim {
      IDLE,
      SWINGING,
      DEFENDING,
      HURT,
      CROUCHING,
      DYING;
   }

   @Environment(EnvType.CLIENT)
   private static final class PvpFakePlayer extends AbstractClientPlayerEntity {
      boolean forceBlocking = false;

      PvpFakePlayer(MinecraftClient mc, UUID uuid, String name) {
         super(mc.world, new GameProfile(uuid, name.length() > 16 ? name.substring(0, 16) : name));
      }

      public SkinTextures getSkinTextures() {
         ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
         if (handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(this.getUuid());
            if (entry != null) {
               return entry.getSkinTextures();
            }
         }

         return super.getSkinTextures();
      }

      public boolean isPartVisible(PlayerModelPart p) {
         return true;
      }

      public boolean isUsingItem() {
         return this.forceBlocking;
      }

      public Hand getActiveHand() {
         return Hand.OFF_HAND;
      }

      public boolean isBlocking() {
         return this.forceBlocking;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class RecruitFakePlayer extends AbstractClientPlayerEntity {
      private SkinTextures skinTex;
      boolean forceBlocking = false;
      final String recruitName;

      RecruitFakePlayer(MinecraftClient mc, String name, Identifier id) {
         super(mc.world, new GameProfile(UUID.nameUUIDFromBytes(("recruit:" + name).getBytes()), name.length() > 16 ? name.substring(0, 16) : name));
         this.recruitName = name;
         this.updateSkin(id);
      }

      void updateSkin(Identifier id) {
         this.skinTex = new SkinTextures(id, null, null, null, Model.WIDE, true);
      }

      public SkinTextures getSkinTextures() {
         return this.skinTex;
      }

      public boolean isPartVisible(PlayerModelPart p) {
         return true;
      }

      public boolean isUsingItem() {
         return this.forceBlocking;
      }

      public Hand getActiveHand() {
         return Hand.OFF_HAND;
      }

      public boolean isBlocking() {
         return this.forceBlocking;
      }
   }
}
