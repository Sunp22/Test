package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class CharacterCreationScreen extends Screen {
   private static final SoulType[] SOULS;
   private int selectedRace = 0;
   private int selectedAttunement = 0;
   private int selectedSoul = 0;
   private int selectedBattleStyle = 0;
   private int selectedView = BattleView.TRADITIONAL.ordinal();
   private TextFieldWidget nameField;
   private String nameText = "";
   private boolean inTitlecard = false;
   private int titleTick = 0;
   private SoundInstance titleMusic = null;
   private static final int TITLE_FADE_IN = 30;
   private static final int TITLE_HOLD = 40;
   private static final int TITLE_FADE_OUT = 30;
   private static final int TITLE_TOTAL = 100;
   private CharacterCreationScreen.Step step = CharacterCreationScreen.Step.INTRO;
   private int stepTick = 0;
   private int leaveTick = -1;
   private CharacterCreationScreen.Step nextStep;
   private int cursor = 0;
   private int nameShake = 0;
   private long lastTickNanos = 0L;
   private int globalTick = 0;
   private static final UUID CLONE_KEY;
   private static final int BEAT = 24;
   private static final int INTRO_BEAT = 96;
   private static final float TYPE_RATE = 1.6F;
   private static final int LEAVE = 9;
   private static final int OPT_STAGGER = 4;
   private static final int OPT_FADE = 7;
   private float cYaw = 0.0F;
   private float cPitch = 8.0F;
   private float cSize = 0.16F;
   private float cX = 0.5F;
   private float cY = 0.8F;
   private float pYaw = 0.0F;
   private float pPitch = 8.0F;
   private float pSize = 0.16F;
   private float pX = 0.5F;
   private float pY = 0.8F;
   private static final Identifier TEX_GLOW;
   private static final Identifier TEX_RING;
   private static final float STAGE_PX = 29.6F;
   private static final ItemStack DISPLAY_SWORD;
   private final List<int[]> optRects = new ArrayList<>();

   public CharacterCreationScreen() {
      super(Text.literal("Character Creation"));
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private boolean isMonster() {
      return this.selectedRace == 1;
   }

   private int beatLen() {
      return this.step == CharacterCreationScreen.Step.INTRO ? 96 : 24;
   }

   private String prompt() {
      return switch (this.step) {
         case INTRO -> "...A VESSEL TAKES SHAPE IN THE DARK.";
         case RACE -> "WHAT ARE YOU?";
         case NAME -> "WHAT IS YOUR NAME?";
         case SOUL -> "WHAT LIES WITHIN YOU?";
         case STYLE -> "HOW WILL YOU FIGHT?";
         case VIEW -> "HOW WILL YOU SEE IT?";
         case CONFIRM -> "IS THIS WHO YOU ARE?";
      };
   }

   private List<CharacterCreationScreen.Opt> options() {
      List o = new ArrayList<>();
      switch (this.step) {
         case RACE:
            o.add(new CharacterCreationScreen.Opt("HUMAN", "Choose the trait of your SOUL - it grants a passive in battle.", -1));
            o.add(new CharacterCreationScreen.Opt("MONSTER", "Work in progress. A Monster's SOUL grows stronger by absorbing the SOULs of others.", -1));
         case NAME:
         default:
            break;
         case SOUL:
            for (SoulType s : SOULS) {
               o.add(new CharacterCreationScreen.Opt(s.getDisplayName().toUpperCase(), s.getDescription(), 0xFF000000 | s.getColor()));
            }
            break;
         case STYLE:
            for (BattleStyle bs : BattleStyle.values()) {
               o.add(new CharacterCreationScreen.Opt(bs.displayName.toUpperCase(), bs.description, -1));
            }
            break;
         case VIEW:
            o.add(new CharacterCreationScreen.Opt("TRADITIONAL", BattleView.TRADITIONAL.description, -1));
            o.add(new CharacterCreationScreen.Opt("3D", BattleView.OVERHAUL.description, -1));
            break;
         case CONFIRM:
            o.add(new CharacterCreationScreen.Opt("YES", "These choices are permanent.", -1));
            o.add(new CharacterCreationScreen.Opt("NO", "Start over from the beginning.", -1));
      }

      return o;
   }

   private int promptDone() {
      return this.beatLen() + (int)Math.ceil((double)((float)this.prompt().length() / 1.6F));
   }

   private int optStart() {
      return this.promptDone() + 5;
   }

   private float optAppear(int i, float td) {
      float t = (float)this.stepTick + td - (float)this.optStart() - (float)(i * 4);
      return clamp01(t / 7.0F);
   }

   private boolean revealed() {
      int n = Math.max(1, this.options().size());
      return this.stepTick >= this.optStart() + (n - 1) * 4 + 7;
   }

   private void skipReveal() {
      int n = Math.max(1, this.options().size());
      this.stepTick = Math.max(this.stepTick, this.optStart() + (n - 1) * 4 + 7);
   }

   private void enterStep(CharacterCreationScreen.Step s) {
      this.step = s;
      this.stepTick = 0;
      this.leaveTick = -1;

      this.cursor = switch (s) {
         case RACE -> this.selectedRace;
         default -> 0;
         case SOUL -> this.selectedSoul;
         case STYLE -> this.selectedBattleStyle;
         case VIEW -> this.selectedView == BattleView.OVERHAUL.ordinal() ? 1 : 0;
      };
      if (s == CharacterCreationScreen.Step.NAME && this.nameField != null) {
         this.nameField.setFocused(true);
      }
   }

   private void leaveTo(CharacterCreationScreen.Step s) {
      if (this.leaveTick < 0) {
         this.nextStep = s;
         this.leaveTick = 0;
      }
   }

   private CharacterCreationScreen.Step previous(CharacterCreationScreen.Step s) {
      return switch (s) {
         case NAME -> CharacterCreationScreen.Step.RACE;
         case SOUL -> CharacterCreationScreen.Step.NAME;
         case STYLE -> this.isMonster() ? CharacterCreationScreen.Step.NAME : CharacterCreationScreen.Step.SOUL;
         case VIEW -> CharacterCreationScreen.Step.STYLE;
         case CONFIRM -> {
            if (BattleStyle.values()[this.selectedBattleStyle].hasViewChoice()) {
            }

            yield CharacterCreationScreen.Step.STYLE;
         }
         default -> null;
      };
   }

   private void choose() {
      if (this.leaveTick < 0) {
         if (!this.revealed()) {
            this.skipReveal();
         } else {
            switch (this.step) {
               case INTRO:
                  this.leaveTo(CharacterCreationScreen.Step.RACE);
                  break;
               case RACE:
                  this.selectedRace = this.cursor;
                  TurnBattleSounds.playSelect();
                  this.leaveTo(CharacterCreationScreen.Step.NAME);
                  break;
               case NAME:
                  if (this.nameText.trim().isEmpty()) {
                     this.nameShake = 10;
                     TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 0.7F, 1.0F);
                     return;
                  }

                  TurnBattleSounds.playSelect();
                  this.leaveTo(this.isMonster() ? CharacterCreationScreen.Step.STYLE : CharacterCreationScreen.Step.SOUL);
                  break;
               case SOUL:
                  this.selectedSoul = this.cursor;
                  TurnBattleSounds.playClient(TurnBattleSounds.GREAT_SHINE, 0.6F, 1.2F);
                  this.leaveTo(CharacterCreationScreen.Step.STYLE);
                  break;
               case STYLE:
                  this.selectedBattleStyle = this.cursor;
                  TurnBattleSounds.playSelect();
                  if (BattleStyle.values()[this.cursor].hasViewChoice()) {
                  }

                  this.leaveTo(CharacterCreationScreen.Step.CONFIRM);
                  break;
               case VIEW:
                  this.selectedView = (this.cursor == 1 ? BattleView.OVERHAUL : BattleView.TRADITIONAL).ordinal();
                  TurnBattleSounds.playSelect();
                  this.leaveTo(CharacterCreationScreen.Step.CONFIRM);
                  break;
               case CONFIRM:
                  if (this.cursor == 0) {
                     this.leaveTick = 0;
                     this.nextStep = null;
                     this.submit();
                  } else {
                     TurnBattleSounds.playSelect();
                     this.leaveTo(CharacterCreationScreen.Step.RACE);
                  }
            }
         }
      }
   }

   private void back() {
      if (this.leaveTick < 0) {
         CharacterCreationScreen.Step p = this.previous(this.step);
         if (p != null) {
            TurnBattleSounds.playHover();
            this.leaveTo(p);
         }
      }
   }

   private void moveCursor(int d) {
      int n = this.options().size();
      if (n != 0 && this.leaveTick < 0) {
         if (!this.revealed()) {
            this.skipReveal();
         }

         int c = Math.max(0, Math.min(n - 1, this.cursor + d));
         if (c != this.cursor) {
            this.cursor = c;
            TurnBattleSounds.playHover();
         }
      }
   }

   private float[] shotFor() {
      float t = (float)this.globalTick;

      return switch (this.step) {
         case INTRO -> new float[]{(float)Math.sin((double)t * 0.01) * 6.0F, 8.0F, mix(0.14F, 0.21F, clamp01((float)this.stepTick / 140.0F)), 0.5F, 0.8F};
         case RACE -> new float[]{30.0F, 5.0F, 0.25F, 0.32F, 0.82F};
         case NAME -> new float[]{-16.0F, -6.0F, 0.58F, 0.32F, 1.52F};
         case SOUL -> new float[]{(float)(this.cursor - 3) * 3.0F, 0.0F, 0.5F, 0.32F, 1.22F};
         case STYLE -> new float[]{64.0F, 10.0F, 0.25F, 0.3F, 0.82F};
         case VIEW -> this.cursor == 1 ? new float[]{158.0F, 14.0F, 0.34F, 0.32F, 0.97F} : new float[]{80.0F, 4.0F, 0.24F, 0.3F, 0.8F};
         case CONFIRM -> new float[]{20.0F + (float)Math.sin((double)t * 0.02) * 14.0F, -11.0F, 0.31F, 0.32F, 0.9F};
      };
   }

   private void tickCamera() {
      this.pYaw = this.cYaw;
      this.pPitch = this.cPitch;
      this.pSize = this.cSize;
      this.pX = this.cX;
      this.pY = this.cY;
      float[] s = this.shotFor();
      float k = this.step == CharacterCreationScreen.Step.INTRO ? 0.05F : 0.13F;
      this.cYaw = this.cYaw + (s[0] - this.cYaw) * k;
      this.cPitch = this.cPitch + (s[1] - this.cPitch) * k;
      this.cSize = this.cSize + (s[2] - this.cSize) * k;
      this.cX = this.cX + (s[3] - this.cX) * k;
      this.cY = this.cY + (s[4] - this.cY) * k;
   }

   protected void init() {
      this.nameField = new TextFieldWidget(this.textRenderer, 0, 0, 120, 14, Text.literal(""));
      this.nameField.setMaxLength(24);
      this.nameField.setText(this.nameText);
      this.nameField.setChangedListener(s -> this.nameText = s);
      this.nameField.setVisible(true);
      this.nameField.setEditable(true);
      this.nameField.setFocused(true);
      this.lastTickNanos = System.nanoTime();
   }

   public void tick() {
      this.lastTickNanos = System.nanoTime();
      this.globalTick++;
      if (this.inTitlecard) {
         this.titleTick++;
         if (this.titleTick >= 100) {
            this.finishAndClose();
         }
      } else {
         int before = (int)((float)(this.stepTick - this.beatLen()) * 1.6F);
         this.stepTick++;
         int after = (int)((float)(this.stepTick - this.beatLen()) * 1.6F);
         String pr = this.prompt();
         if (after > 0 && before < pr.length()) {
            for (int c = Math.max(0, before); c < Math.min(after, pr.length()); c++) {
               if (c % 2 == 0 && pr.charAt(c) != ' ' && pr.charAt(c) != '.') {
                  TurnBattleSounds.playClient(TurnBattleSounds.SND_TEXT, 0.55F, 1.0F);
                  break;
               }
            }
         }

         if (this.step == CharacterCreationScreen.Step.INTRO && this.leaveTick < 0 && this.stepTick > Math.max(this.promptDone() + 40, 192)) {
            this.leaveTo(CharacterCreationScreen.Step.RACE);
         }

         if (this.leaveTick >= 0) {
            this.leaveTick++;
            if (this.leaveTick >= 9 && this.nextStep != null) {
               this.enterStep(this.nextStep);
            }
         }

         if (this.nameShake > 0) {
            this.nameShake--;
         }

         this.tickCamera();
         CloneAnimator.setSoul(
            CLONE_KEY, this.isMonster() ? SoulType.MONSTER : SOULS[this.step == CharacterCreationScreen.Step.SOUL ? this.cursor : this.selectedSoul]
         );
      }
   }

   private float partial() {
      float d = (float)(System.nanoTime() - this.lastTickNanos) / 5.0E7F;
      return clamp01(d);
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      if (this.inTitlecard) {
         ctx.fill(0, 0, this.width, this.height, -16777216);
         this.renderTitlecard(ctx);
      } else {
         float td = this.partial();
         SoulType soul = this.currentSoul();
         int tint = soul != null && this.step.ordinal() >= CharacterCreationScreen.Step.SOUL.ordinal() ? soul.getColor() & 16777215 : 12568831;
         if (this.step == CharacterCreationScreen.Step.SOUL) {
            tint = SOULS[this.cursor].getColor() & 16777215;
         }

         float yaw = lerpAngle(this.pYaw, this.cYaw, td);
         float pitch = mix(this.pPitch, this.cPitch, td);
         float size = mix(this.pSize, this.cSize, td) * (float)this.height;
         float fx = mix(this.pX, this.cX, td) * (float)this.width;
         float fy = mix(this.pY, this.cY, td) * (float)this.height;
         this.renderVoid(ctx, fx, fy, size, yaw, pitch, tint, td);
         ctx.draw();
         RenderSystem.clear(256, false);
         this.renderClone(ctx, fx, fy, size, yaw, pitch, td);
         ctx.draw();
         RenderSystem.clear(256, false);
         if (this.step == CharacterCreationScreen.Step.SOUL) {
            this.renderChestSoul(ctx, fx, fy, size, pitch, td);
         }

         if (this.step == CharacterCreationScreen.Step.INTRO) {
            float a = 1.0F - clamp01(((float)this.stepTick + td - 6.0F) / 40.0F);
            if (a > 0.0F) {
               ctx.fill(0, 0, this.width, this.height, argb(0, a));
            }
         }

         this.vignette(ctx);
         this.renderPanel(ctx, mx, my, td);
         this.renderFooter(ctx, td);
      }
   }

   private SoulType currentSoul() {
      return this.isMonster() ? SoulType.MONSTER : SOULS[Math.max(0, Math.min(SOULS.length - 1, this.selectedSoul))];
   }

   private void renderVoid(DrawContext ctx, float fx, float fy, float size, float yaw, float pitch, int tint, float td) {
      ctx.fillGradient(0, 0, this.width, this.height, -16645624, -16119784);
      float t = (float)this.globalTick + td;
      int shaftW = (int)(size * 1.6F);

      for (int i = 0; i < 6; i++) {
         float w = (float)shaftW * (1.0F - (float)i * 0.14F);
         int x0 = (int)(fx - w / 2.0F);
         int x1 = (int)(fx + w / 2.0F);
         ctx.fillGradient(x0, 0, x1, (int)fy, argb(tint, 0.0F), argb(tint, 0.022F));
      }

      float flat = 0.18F + 0.55F * (float)Math.sin(Math.toRadians((double)Math.max(4.0F, 12.0F + pitch)));
      float pool = size * 2.4F;
      this.tex(ctx, TEX_GLOW, fx, fy, pool, pool * flat, tint, 0.55F);
      this.tex(ctx, TEX_GLOW, fx, fy, pool * 0.45F, pool * 0.45F * flat, 16777215, 0.35F);

      for (int r = 0; r < 3; r++) {
         float q = (t * 0.006F + (float)r / 3.0F) % 1.0F;
         float rs = size * (0.6F + q * 3.2F);
         this.tex(ctx, TEX_RING, fx, fy, rs, rs * flat, tint, 0.35F * (1.0F - q));
      }

      float yr = (float)Math.toRadians((double)yaw);
      float cs = (float)Math.cos((double)yr);
      float sn = (float)Math.sin((double)yr);

      for (int i = 0; i < 70; i++) {
         float ox = (CloneAnimator.rnd(i * 3 + 1) - 0.5F) * 9.0F;
         float oz = (CloneAnimator.rnd(i * 3 + 2) - 0.5F) * 9.0F;
         float speed = 0.004F + CloneAnimator.rnd(i * 7) * 0.006F;
         float oy = (CloneAnimator.rnd(i * 3 + 3) + t * speed) % 1.0F * 4.5F - 0.3F;
         float rx = ox * cs - oz * sn;
         float rz = ox * sn + oz * cs;
         float depth = 1.0F / (1.0F + Math.max(-0.6F, rz) * 0.18F);
         float sx = fx + rx * size * 0.55F * depth;
         float sy = fy - oy * size * 0.55F * depth + rz * size * 0.06F;
         float life = (float)Math.sin(Math.PI * (double)clamp01((oy + 0.3F) / 4.5F));
         float a = 0.5F * life * clamp01(depth);
         int px = Math.max(1, Math.round(size / 40.0F * depth));
         ctx.fill((int)sx, (int)sy, (int)sx + px, (int)sy + px, argb(i % 5 == 0 ? 16777215 : tint, a));
      }
   }

   private void tex(DrawContext ctx, Identifier id, float cx, float cy, float w, float h, int rgb, float a) {
      if (!(a <= 0.003F) && !(w < 1.0F) && !(h < 1.0F)) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F, clamp01(a));
         int iw = Math.round(w);
         int ih = Math.round(h);
         ctx.drawTexture(id, Math.round(cx - w / 2.0F), Math.round(cy - h / 2.0F), iw, ih, 0.0F, 0.0F, 64, 64, 64, 64);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void vignette(DrawContext ctx) {
      int e = Math.max(20, this.height / 5);
      ctx.fillGradient(0, 0, this.width, e, -1073741824, 0);
      ctx.fillGradient(0, this.height - e, this.width, this.height, 0, -1073741824);
   }

   private boolean armed() {
      return this.step == CharacterCreationScreen.Step.STYLE
         || this.step == CharacterCreationScreen.Step.VIEW
         || this.step == CharacterCreationScreen.Step.CONFIRM;
   }

   private void renderClone(DrawContext ctx, float fx, float fy, float size, float yaw, float pitch, float td) {
      if (this.client != null && this.client.player != null) {
         ClientPlayerEntity e = this.client.player;
         CloneAnimator.Frame fr = CloneAnimator.frame(CLONE_KEY, this.armed());
         CloneAnimator.Pose pose = fr != null ? fr.pose.copy() : new CloneAnimator.Pose();
         this.lookAround(pose, td);
         float sBody = e.bodyYaw;
         float sPBody = e.prevBodyYaw;
         float sHead = e.headYaw;
         float sPHead = e.prevHeadYaw;
         float sYaw = e.getYaw();
         float sPYaw = e.prevYaw;
         float sPitch = e.getPitch();
         float sPPitch = e.prevPitch;
         float sSwing = e.handSwingProgress;
         float sLSwing = e.lastHandSwingProgress;
         int slot = e.getInventory().selectedSlot;
         ItemStack sMain = (ItemStack)e.getInventory().main.get(slot);
         ItemStack sOff = (ItemStack)e.getInventory().offHand.get(0);

         try {
            e.bodyYaw = e.prevBodyYaw = 180.0F;
            e.headYaw = e.prevHeadYaw = 180.0F;
            e.setYaw(180.0F);
            e.prevYaw = 180.0F;
            e.setPitch(0.0F);
            e.prevPitch = 0.0F;
            e.handSwingProgress = e.lastHandSwingProgress = 0.0F;
            e.getInventory().main.set(slot, this.armed() ? DISPLAY_SWORD : ItemStack.EMPTY);
            e.getInventory().offHand.set(0, ItemStack.EMPTY);
            Quaternionf q = new Quaternionf()
               .rotateZ((float) Math.PI)
               .rotateX((float)Math.toRadians((double)(-pitch)))
               .rotateY((float)Math.toRadians((double)yaw))
               .rotateX((float)Math.toRadians((double)(-pose.tilt())));
            Vector3f offset = new Vector3f(0.0F, -pose.dy() / 29.6F, -pose.dx() / 29.6F);
            q.transform(offset);
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(fx, fy, 0.0F);
            m.scale(pose.sx(), pose.sy(), 1.0F);
            m.translate(-fx, -fy, 0.0F);
            CloneAnimator.beginModelPose(e, pose);

            try {
               InventoryScreen.drawEntity(ctx, fx, fy, size, offset, q, null, e);
            } finally {
               CloneAnimator.endModelPose();
               m.pop();
            }
         } catch (Exception var36) {
         } finally {
            e.bodyYaw = sBody;
            e.prevBodyYaw = sPBody;
            e.headYaw = sHead;
            e.prevHeadYaw = sPHead;
            e.setYaw(sYaw);
            e.prevYaw = sPYaw;
            e.setPitch(sPitch);
            e.prevPitch = sPPitch;
            e.handSwingProgress = sSwing;
            e.lastHandSwingProgress = sLSwing;
            e.getInventory().main.set(slot, sMain);
            e.getInventory().offHand.set(0, sOff);
         }
      }
   }

   private void lookAround(CloneAnimator.Pose p, float td) {
      float t = (float)this.stepTick + td;
      p.headW = Math.max(p.headW, 1.0F);
      if (this.step == CharacterCreationScreen.Step.INTRO) {
         float hands = bell(seg(t, 30.0F, 92.0F));
         float hk = easeOut(clamp01((t - 30.0F) / 14.0F)) * (1.0F - clamp01((t - 80.0F) / 12.0F));
         p.armW = Math.max(p.armW, hk);
         p.rArmP = mix(p.rArmP, -0.95F, hk);
         p.rArmY = mix(p.rArmY, -0.35F, hk);
         p.lArmP = mix(p.lArmP, -0.95F, hk);
         p.lArmY = mix(p.lArmY, 0.35F, hk);
         p.headP += 0.55F * hands;
         p.headY = p.headY + (0.6F * bell(seg(t, 92.0F, 128.0F)) - 0.55F * bell(seg(t, 122.0F, 160.0F)));
         p.headP = p.headP - 0.25F * bell(seg(t, 150.0F, 185.0F));
      } else {
         if (t < 30.0F) {
            float u = t / 30.0F;
            p.headY = p.headY + (0.55F * bell(seg(u, 0.0F, 0.48F)) - 0.48F * bell(seg(u, 0.38F, 0.86F)));
            p.headP = p.headP - 0.18F * bell(seg(u, 0.72F, 1.0F));
         }

         if (this.step == CharacterCreationScreen.Step.NAME) {
            p.headP -= 0.06F;
         }
      }
   }

   private void renderChestSoul(DrawContext ctx, float fx, float fy, float size, float pitch, float td) {
      float t = (float)this.globalTick + td;
      float appear = clamp01(((float)this.stepTick + td - 14.400001F) / 14.0F);
      if (!(appear <= 0.0F)) {
         SoulType s = SOULS[this.cursor];
         int rgb = s.getColor() & 16777215;
         float cosP = (float)Math.cos(Math.toRadians((double)pitch));
         float cy = fy - 1.12F * size * cosP;
         float pulse = 0.85F + 0.15F * (float)Math.sin((double)(t * 0.18F));
         this.tex(ctx, TEX_GLOW, fx, cy, size * 1.4F * pulse, size * 1.4F * pulse, rgb, 0.55F * appear);
         this.tex(ctx, TEX_GLOW, fx, cy, size * 0.5F, size * 0.5F, 16777215, 0.35F * appear);
         int sz = Math.max(8, Math.round(size * 0.22F * (0.9F + 0.1F * pulse) * easeOutBack(appear)));
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, appear);
         ctx.getMatrices().push();
         ctx.getMatrices().translate(0.0F, 0.0F, 300.0F);
         ctx.drawTexture(soulTex(s), Math.round(fx - (float)sz / 2.0F), Math.round(cy - (float)sz / 2.0F), 0.0F, 0.0F, sz, sz, sz, sz);
         ctx.getMatrices().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private int panelX() {
      return (int)((float)this.width * 0.56F);
   }

   private int panelW() {
      return Math.min((int)((float)this.width * 0.4F), 300);
   }

   private void renderPanel(DrawContext ctx, int mx, int my, float td) {
      this.optRects.clear();
      float leave = this.leaveTick >= 0 ? 1.0F - clamp01(((float)this.leaveTick + td) / 9.0F) : 1.0F;
      if (!(leave <= 0.0F)) {
         int px = this.step == CharacterCreationScreen.Step.INTRO ? this.width / 2 : this.panelX();
         int pw = this.step == CharacterCreationScreen.Step.INTRO ? Math.min(this.width - 40, 360) : this.panelW();
         int py = this.step == CharacterCreationScreen.Step.INTRO ? (int)((float)this.height * 0.15F) : (int)((float)this.height * 0.22F);
         String pr = this.prompt();
         int shown = (int)Math.max(0.0F, Math.min((float)pr.length(), ((float)this.stepTick + td - (float)this.beatLen()) * 1.6F));
         if (shown > 0) {
            float scale = this.step == CharacterCreationScreen.Step.INTRO ? 1.5F : 2.0F;
            List lines = this.wrap(pr.substring(0, shown), (int)((float)pw / scale));

            for (int i = 0; i < lines.size(); i++) {
               int lw = (int)((float)this.textRenderer.getWidth(lines.get(i)) * scale);
               float x = this.step == CharacterCreationScreen.Step.INTRO ? (float)px - (float)lw / 2.0F : (float)px;
               this.drawScaled(ctx, lines.get(i), x, (float)py + (float)(i * 12) * scale, scale, argb(16777215, leave));
            }

            py += (int)((float)(lines.size() * 12) * scale) + 14;
         }

         if (this.step != CharacterCreationScreen.Step.INTRO) {
            if (this.step == CharacterCreationScreen.Step.NAME) {
               this.renderNameEntry(ctx, px, py, pw, td, leave);
            } else {
               List opts = this.options();
               int rowH = this.step == CharacterCreationScreen.Step.SOUL ? 15 : 20;
               float scale = this.step == CharacterCreationScreen.Step.SOUL ? 1.15F : 1.5F;

               for (int i = 0; i < opts.size(); i++) {
                  float a = this.optAppear(i, td) * leave;
                  if (!(a <= 0.0F)) {
                     CharacterCreationScreen.Opt o = opts.get(i);
                     boolean sel = i == this.cursor;
                     float slide = (1.0F - easeOut(this.optAppear(i, td))) * 14.0F;
                     int x = px + 18 + (int)slide + (sel ? 4 : 0);
                     int y = py + i * rowH;
                     int col = sel ? o.color() : dim(o.color());
                     if (this.step == CharacterCreationScreen.Step.SOUL && !sel) {
                        col = dim(o.color());
                     }

                     this.drawScaled(ctx, o.label(), (float)x, (float)y, scale, argb(col & 16777215, a));
                     if (this.step == CharacterCreationScreen.Step.SOUL) {
                        int isz = sel ? 12 : 9;
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a * (sel ? 1.0F : 0.55F));
                        ctx.drawTexture(soulTex(SOULS[i]), x - 16 - isz / 2, y + 4 - isz / 2, 0.0F, 0.0F, isz, isz, isz, isz);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                     }

                     int w = (int)((float)this.textRenderer.getWidth(o.label()) * scale) + 30;
                     this.optRects.add(new int[]{px, y - 3, Math.max(w, pw / 2), rowH});
                     if (sel && this.step != CharacterCreationScreen.Step.SOUL && this.optAppear(i, td) > 0.5F) {
                        int hs = 10;
                        float bob = (float)Math.sin((double)(((float)this.globalTick + td) * 0.25F)) * 1.2F;
                        RenderSystem.enableBlend();
                        ctx.drawTexture(soulTex(this.cursorSoul()), px + 2 + (int)bob, y + (int)(scale * 4.0F) - hs / 2, 0.0F, 0.0F, hs, hs, hs, hs);
                     }
                  }
               }

               if (this.cursor >= 0 && this.cursor < opts.size() && this.revealed()) {
                  float a = leave * clamp01(((float)this.stepTick + td - (float)this.optStart() - (float)(opts.size() * 4)) / 8.0F);
                  int dy = py + opts.size() * rowH + 10;
                  ctx.fill(px + 18, dy - 4, px + 18 + Math.min(pw - 18, 120), dy - 3, argb(8947882, 0.5F * a));
                  List dl = this.wrap(opts.get(this.cursor).desc(), pw - 18);

                  for (int ix = 0; ix < dl.size(); ix++) {
                     this.text(ctx, dl.get(ix), px + 18, dy + ix * 11, argb(11184844, a));
                  }

                  if (this.step == CharacterCreationScreen.Step.CONFIRM) {
                     this.renderSummary(ctx, px + 18, dy + dl.size() * 11 + 12, a);
                  }
               }
            }
         }
      }
   }

   private void renderNameEntry(DrawContext ctx, int px, int py, int pw, float td, float leave) {
      float a = clamp01(((float)this.stepTick + td - (float)this.promptDone() - 3.0F) / 8.0F) * leave;
      if (!(a <= 0.0F)) {
         float shake = this.nameShake > 0 ? (float)Math.sin((double)((float)this.nameShake * 2.2F)) * (float)this.nameShake * 0.5F : 0.0F;
         int x = px + 18 + (int)shake;
         String name = this.nameText;
         boolean blink = this.globalTick / 8 % 2 == 0;
         int lineW = Math.min(pw - 18, 220);
         float scale = Math.max(1.0F, Math.min(2.0F, ((float)lineW - 4.0F) / (float)Math.max(1, this.textRenderer.getWidth(name + "_"))));
         this.drawScaled(ctx, name + (blink ? "_" : " "), (float)x, (float)(py + 4), scale, argb(16777215, a));
         ctx.fill(x, py + 26, x + lineW, py + 27, argb(this.nameShake > 0 ? 16733525 : 8947882, a));
         this.text(ctx, name.isEmpty() ? "Type a name, then press ENTER" : "Press ENTER to continue", x, py + 34, argb(7829401, a));
      }
   }

   private void renderSummary(DrawContext ctx, int x, int y, float a) {
      String[] rows = new String[]{
         "NAME   " + this.nameText.trim(),
         "RACE   " + (this.isMonster() ? "Monster" : "Human"),
         this.isMonster() ? null : "SOUL   " + SOULS[this.selectedSoul].getDisplayName(),
         "STYLE  "
            + BattleStyle.values()[this.selectedBattleStyle].displayName
            + (BattleStyle.values()[this.selectedBattleStyle].hasViewChoice() ? " (" + BattleView.fromOrdinal(this.selectedView).displayName + ")" : "")
      };
      int yy = y;

      for (String r : rows) {
         if (r != null) {
            int col = r.startsWith("SOUL") ? SOULS[this.selectedSoul].getColor() & 16777215 : 14540270;
            this.text(ctx, r, x, yy, argb(col, a));
            yy += 11;
         }
      }
   }

   private void renderFooter(DrawContext ctx, float td) {
      if (this.step == CharacterCreationScreen.Step.INTRO) {
         if (this.stepTick > 20) {
            String s = "Press any key";
            float a = 0.35F + 0.25F * (float)Math.sin((double)(((float)this.globalTick + td) * 0.12F));
            this.text(ctx, s, this.width / 2 - this.textRenderer.getWidth(s) / 2, this.height - 18, argb(8947882, a));
         }
      } else {
         CharacterCreationScreen.Step[] path = this.path();
         int n = path.length;
         int dot = 5;
         int gap = 7;
         int x0 = this.width / 2 - (n * dot + (n - 1) * gap) / 2;
         int y = this.height - 14;

         for (int i = 0; i < n; i++) {
            boolean here = path[i] == this.step;
            boolean done = path[i].ordinal() < this.step.ordinal();
            int c = here ? -1 : (done ? -7829334 : -13421756);
            ctx.fill(x0 + i * (dot + gap), y, x0 + i * (dot + gap) + dot, y + dot, c);
         }

         String hint = this.step == CharacterCreationScreen.Step.NAME
            ? "ENTER  continue     ESC  back"
            : (this.previous(this.step) == null ? "W/S  \u2191\u2193  choose     ENTER  confirm" : "W/S  \u2191\u2193  choose     ENTER  confirm     ESC  back");
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(hint), this.width - this.textRenderer.getWidth(hint) - 8, this.height - 16, -11184784);
      }
   }

   private CharacterCreationScreen.Step[] path() {
      List p = new ArrayList<>();
      p.add(CharacterCreationScreen.Step.RACE);
      p.add(CharacterCreationScreen.Step.NAME);
      if (!this.isMonster()) {
         p.add(CharacterCreationScreen.Step.SOUL);
      }

      p.add(CharacterCreationScreen.Step.STYLE);
      if (BattleStyle.values()[this.selectedBattleStyle].hasViewChoice() || this.step == CharacterCreationScreen.Step.VIEW) {
         p.add(CharacterCreationScreen.Step.VIEW);
      }

      p.add(CharacterCreationScreen.Step.CONFIRM);
      return p.toArray(new CharacterCreationScreen.Step[0]);
   }

   private SoulType cursorSoul() {
      return this.step == CharacterCreationScreen.Step.SOUL ? SOULS[this.cursor] : this.currentSoul();
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (this.inTitlecard) {
         return true;
      } else if (this.step == CharacterCreationScreen.Step.INTRO) {
         this.choose();
         return true;
      } else {
         boolean typing = this.step == CharacterCreationScreen.Step.NAME;
         switch (key) {
            case 32:
               if (typing) {
                  return true;
               }

               this.choose();
               return true;
            case 83:
               if (!typing) {
                  this.moveCursor(1);
                  return true;
               }
               break;
            case 87:
               if (!typing) {
                  this.moveCursor(-1);
                  return true;
               }
               break;
            case 88:
               if (!typing) {
                  this.back();
                  return true;
               }
               break;
            case 90:
               if (!typing) {
                  this.choose();
                  return true;
               }
               break;
            case 256:
               this.back();
               return true;
            case 257:
            case 335:
               this.choose();
               return true;
            case 259:
               if (!typing) {
                  this.back();
                  return true;
               }
               break;
            case 262:
            case 264:
               if (!typing) {
                  this.moveCursor(1);
               } else if (key == 262) {
                  this.nameField.keyPressed(key, scan, mods);
               }

               return true;
            case 263:
            case 265:
               if (!typing) {
                  this.moveCursor(-1);
               } else if (key == 263) {
                  this.nameField.keyPressed(key, scan, mods);
               }

               return true;
         }

         return typing && this.nameField != null ? this.nameField.keyPressed(key, scan, mods) : true;
      }
   }

   public boolean charTyped(char c, int mods) {
      if (this.inTitlecard || this.leaveTick >= 0) {
         return false;
      } else if (this.step == CharacterCreationScreen.Step.NAME && this.nameField != null) {
         if (!this.revealed()) {
            this.skipReveal();
         }

         return this.nameField.charTyped(c, mods);
      } else {
         return false;
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (this.inTitlecard || this.leaveTick >= 0) {
         return true;
      } else if (btn == 1) {
         this.back();
         return true;
      } else if (this.step != CharacterCreationScreen.Step.INTRO && this.step != CharacterCreationScreen.Step.NAME) {
         if (!this.revealed()) {
            this.skipReveal();
            return true;
         } else {
            for (int i = 0; i < this.optRects.size(); i++) {
               int[] r = this.optRects.get(i);
               if (mx >= (double)r[0] && mx < (double)(r[0] + r[2]) && my >= (double)r[1] && my < (double)(r[1] + r[3])) {
                  this.cursor = i;
                  this.choose();
                  return true;
               }
            }

            return true;
         }
      } else {
         if (btn == 0) {
            this.choose();
         }

         return true;
      }
   }

   public void mouseMoved(double mx, double my) {
      if (!this.inTitlecard && this.leaveTick < 0 && this.revealed()) {
         for (int i = 0; i < this.optRects.size(); i++) {
            int[] r = this.optRects.get(i);
            if (mx >= (double)r[0] && mx < (double)(r[0] + r[2]) && my >= (double)r[1] && my < (double)(r[1] + r[3]) && i != this.cursor) {
               this.cursor = i;
               TurnBattleSounds.playHover();
            }
         }
      }
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double amount) {
      if (this.step != CharacterCreationScreen.Step.NAME && this.step != CharacterCreationScreen.Step.INTRO) {
         this.moveCursor(amount > 0.0 ? -1 : 1);
      }

      return true;
   }

   private void submit() {
      TurnBattleSounds.playSelect();
      String vname = this.nameText.trim();
      if (vname.isEmpty()) {
         vname = "Vessel";
      }

      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(this.selectedRace);
      buf.writeInt(this.selectedAttunement);
      buf.writeInt(this.isMonster() ? 0 : this.selectedSoul);
      buf.writeString(vname, 32);
      buf.writeInt(this.selectedBattleStyle);
      buf.writeInt(this.selectedView);
      Net121.sendToServer(TurnBattleMod.CHAR_CREATE_DONE, buf);
      this.startTitlecard();
   }

   private void renderTitlecard(DrawContext ctx) {
      float alpha;
      if (this.titleTick < 70) {
         alpha = 1.0F;
      } else {
         alpha = 1.0F - (float)(this.titleTick - 30 - 40) / 30.0F;
      }

      float a = Math.max(0.0F, Math.min(1.0F, alpha));
      int ia = (int)(a * 255.0F);
      ctx.fill(0, 0, this.width, this.height, ia << 24);
      Identifier logo = Identifier.of("turnbattle", "textures/gui/soulboundlogo2.png");
      int lw = Math.min(this.width - 40, 600);
      int lh2 = (int)((float)lw * 225.0F / 719.0F);
      int lx = this.width / 2 - lw / 2;
      int ly = this.height / 2 - lh2 / 2;
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
      ctx.drawTexture(logo, lx, ly, 0.0F, 0.0F, lw, lh2, lw, lh2);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void startTitlecard() {
      this.inTitlecard = true;
      this.titleTick = 0;
      if (this.client != null) {
         SoundEvent se = (SoundEvent)Registries.SOUND_EVENT.get(Identifier.of("turnbattle", "mus_intronoise"));
         if (se != null) {
            this.titleMusic = PositionedSoundInstance.master(se, 1.0F, 1.0F);
            this.client.getSoundManager().play(this.titleMusic);
         }
      }
   }

   private void finishAndClose() {
      if (this.titleMusic != null && this.client != null) {
         this.client.getSoundManager().stop(this.titleMusic);
         this.titleMusic = null;
      }

      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   private void text(DrawContext ctx, String s, int x, int y, int argb) {
      if ((argb >>> 24 & 0xFF) >= 4) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(s), x, y, argb);
      }
   }

   private void drawScaled(DrawContext ctx, String s, float x, float y, float scale, int argb) {
      if ((argb >>> 24 & 0xFF) >= 4) {
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(x, y, 0.0F);
         m.scale(scale, scale, 1.0F);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(s), 0, 0, argb);
         m.pop();
      }
   }

   private List<String> wrap(String text, int maxW) {
      List lines = new ArrayList<>();
      StringBuilder cur = new StringBuilder();

      for (String word : text.split(" ")) {
         String test = cur.isEmpty() ? word : cur + " " + word;
         if (this.textRenderer.getWidth(test) > maxW && !cur.isEmpty()) {
            lines.add(cur.toString());
            cur = new StringBuilder(word);
         } else {
            cur = new StringBuilder(test);
         }
      }

      if (!cur.isEmpty()) {
         lines.add(cur.toString());
      }

      return lines;
   }

   private static Identifier soulTex(SoulType soul) {
      String f = switch (soul) {
         case DETERMINATION -> "redsoulsimple.png";
         case PERSEVERANCE -> "purplesoulsimple.png";
         case INTEGRITY -> "bluesoulsimple.png";
         case PATIENCE -> "cyansoulsimple.png";
         case JUSTICE -> "yellowsoulsimple.png";
         case BRAVERY -> "orangesoulsimple.png";
         case KINDNESS -> "greensoulsimple.png";
         case ORI -> "orisoulsimple.png";
         case MONSTER -> "monstersoulsimple.png";
      };
      return Identifier.of("turnbattle", "textures/souls/" + f);
   }

   private static int dim(int argb) {
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      r = (int)((float)r * 0.45F + 40.0F);
      g = (int)((float)g * 0.45F + 40.0F);
      b = (int)((float)b * 0.45F + 48.0F);
      return 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
   }

   private static int argb(int rgb, float a) {
      return (int)(clamp01(a) * 255.0F) << 24 | rgb & 16777215;
   }

   private static float clamp01(float x) {
      return x < 0.0F ? 0.0F : (x > 1.0F ? 1.0F : x);
   }

   private static float seg(float p, float a, float b) {
      return clamp01((p - a) / (b - a));
   }

   private static float mix(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static float bell(float q) {
      return !(q <= 0.0F) && !(q >= 1.0F) ? (float)Math.sin(Math.PI * (double)q) : 0.0F;
   }

   private static float easeOut(float x) {
      float u = 1.0F - x;
      return 1.0F - u * u * u;
   }

   private static float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float u = x - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   private static float lerpAngle(float a, float b, float t) {
      float d = ((b - a) % 360.0F + 540.0F) % 360.0F - 180.0F;
      return a + d * t;
   }

   static {
      List s = new ArrayList<>();

      for (SoulType st : SoulType.values()) {
         if (st != SoulType.ORI && st != SoulType.MONSTER) {
            s.add(st);
         }
      }

      SOULS = s.toArray(new SoulType[0]);
      CLONE_KEY = UUID.nameUUIDFromBytes("turnbattle:character-creator".getBytes());
      TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
      TEX_RING = Identifier.of("turnbattle", "textures/effects/ritual_ring.png");
      DISPLAY_SWORD = new ItemStack(Items.IRON_SWORD);
   }

   private static record Opt(String label, String desc, int color) {
   }

   private static enum Step {
      INTRO,
      RACE,
      NAME,
      SOUL,
      STYLE,
      VIEW,
      CONFIRM;
   }
}
