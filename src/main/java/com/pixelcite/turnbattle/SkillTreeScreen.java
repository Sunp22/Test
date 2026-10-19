package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SkillTreeScreen extends Screen {
   private final Screen parent;
   private float cameraX;
   private float cameraY;
   private float cameraZoom = 0.75F;
   private float camTX;
   private float camTY;
   private float camTZoom = 1.0F;
   private float savedCamX;
   private float savedCamY;
   private float savedZoom = 1.0F;
   private static final float MIN_ZOOM = 0.4F;
   private static final float MAX_ZOOM = 2.5F;
   private boolean dragging = false;
   private double dragStartMouseX;
   private double dragStartMouseY;
   private float dragStartCameraX;
   private float dragStartCameraY;
   private long lastFrameNanos;
   private long lastTickRealTimeNanos = 0L;
   private float lastRenderDelta = 1.0F;
   private long openedNanos;
   private long arrivalNanos;
   private List<SkillTreeDefs.GlobalNode> allNodes;
   private final Map<String, SkillTreeDefs.GlobalNode> nodesBySkill = new HashMap<>();
   private float maxNodeDist = 1.0F;
   private SoulType selectedSoulTab;
   private static final SoulType[] HUMAN_SOUL_TABS = new SoulType[]{
      SoulType.DETERMINATION, SoulType.PERSEVERANCE, SoulType.INTEGRITY, SoulType.PATIENCE, SoulType.JUSTICE, SoulType.BRAVERY, SoulType.KINDNESS
   };
   private static final float LIGHT_R = 9.0F;
   private static final int HEADER_H = 22;
   private static final int TAB_H = 18;
   private static final Map<String, Identifier> SKILL_ICONS = Map.of("RUDE_BUSTER", skillIcon("spr_rudebuster_icon.png"));
   private static final Identifier TEX_GLOW = Identifier.of("turnbattle", "textures/effects/ritual_glow.png");
   private static final Identifier TEX_RING = Identifier.of("turnbattle", "textures/effects/ritual_ring.png");
   private static final Identifier DAMAGE_FONT = Identifier.of("turnbattle", "damage_numbers");
   private static final Identifier HEART = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
   private static final int BG_FRAMES = 100;
   private static final int BG_W = 640;
   private static final int BG_H = 480;
   private static final Identifier[] BG_TEX = new Identifier[100];
   private SkillTreeDefs.GlobalNode hoveredNode = null;
   private String lastHoveredSkill = null;
   private long hoverStartNanos;
   private Set<String> lastKnownUnlocked = Set.of();
   private final Map<String, Long> unlockPulseStartNanos = new HashMap<>();
   private static final long UNLOCK_PULSE_DURATION_NANOS = 1100000000L;
   private Set<String> lastKnownAvailable = Set.of();
   private final Map<String, Long> branchOpenStartNanos = new HashMap<>();
   private static final long BRANCH_OPEN_DURATION_NANOS = 700000000L;
   private SkillTreeDefs.GlobalNode selectedNode = null;
   private boolean wantClosed = false;
   private long transitionStartNanos = 0L;
   private float transitionFromT = 0.0F;
   private float transitionTargetT = 0.0F;
   private static final long OPEN_DURATION_NANOS = 380000000L;
   private boolean buttonHovered;
   private String lastButtonState = "";
   private static final UUID PREVIEW_KEY;
   private ArmorStandEntity previewStand;
   private SkillEffect previewEffect;
   private BattleSkill previewSkill;
   private boolean previewOnSelf;
   private int previewTick;
   private int previewNextCast;
   private int standHurt;
   private final Random previewRng = new Random();
   private final List<SkillTreeScreen.Popup> popups = new ArrayList<>();
   private int pvX;
   private int pvY;
   private int pvW;
   private int pvH;
   private int infoX;
   private int infoY;
   private int infoW;
   private int infoH;
   private int btnX;
   private int btnY;
   private int btnW;
   private int btnH;
   private int hitX;
   private int hitY;
   private int hitW;
   private int hitH;
   private final int subject;
   private boolean keyMode = false;
   private boolean keysMoved = false;
   private int lastMouseX = Integer.MIN_VALUE;
   private int lastMouseY = Integer.MIN_VALUE;
   private SkillTreeDefs.GlobalNode keyNode;

   private static Identifier skillIcon(String name) {
      return Identifier.of("turnbattle", "textures/skills/" + name);
   }

   public SkillTreeScreen(Screen parent) {
      this(parent, -1);
   }

   public SkillTreeScreen(Screen parent, int subject) {
      super(Text.literal("Skill Tree"));
      this.parent = parent;
      this.subject = subject;
   }

   private boolean isYou() {
      return this.subject == -1;
   }

   private boolean isCustom() {
      return this.subject >= 1000;
   }

   private boolean isStoryRecruit() {
      return !this.isYou() && !this.isCustom();
   }

   private int ci() {
      return this.subject - 1000;
   }

   private boolean isMonster() {
      return this.isYou() && TurnBattleClientMod.clientPlayerRace == CharacterRace.MONSTER;
   }

   private boolean hasSoulTabs() {
      return !this.isMonster() && !this.isStoryRecruit();
   }

   private static <T> T at(T[] a, int i) {
      return a != null && i >= 0 && i < a.length ? a[i] : null;
   }

   private static int at(int[] a, int i, int def) {
      return a != null && i >= 0 && i < a.length ? a[i] : def;
   }

   private Set<String> unlocked() {
      Set s = this.isYou()
         ? TurnBattleClientMod.clientUnlockedSkills
         : (this.isCustom() ? at(TurnBattleClientMod.customRecruitSkills, this.ci()) : at(TurnBattleClientMod.clientRecruitSkills, this.subject));
      return s != null ? s : Set.of();
   }

   private Set<String> unequipped() {
      Set s = this.isYou()
         ? TurnBattleClientMod.clientUnequippedSkills
         : (this.isCustom() ? at(TurnBattleClientMod.customRecruitUnequippedSkills, this.ci()) : TurnBattleClientMod.clientRecruitUnequippedSkills);
      return s != null ? s : Set.of();
   }

   private int skillPoints() {
      return this.isYou()
         ? TurnBattleClientMod.clientSkillPoints
         : (
            this.isCustom()
               ? at(TurnBattleClientMod.customRecruitSkillPoints, this.ci(), 0)
               : at(TurnBattleClientMod.clientRecruitSkillPoints, this.subject, 0)
         );
   }

   private int level() {
      return this.isYou()
         ? TurnBattleClientMod.clientPlayerLevel
         : (this.isCustom() ? at(TurnBattleClientMod.customRecruitLevels, this.ci(), 1) : at(TurnBattleClientMod.clientRecruitLevels, this.subject, 1));
   }

   private SoulType soul() {
      if (this.isYou()) {
         return TurnBattleClientMod.clientSoul;
      } else if (this.isCustom()) {
         int o = at(TurnBattleClientMod.customRecruitSouls, this.ci(), -1);
         return o >= 0 && o < SoulType.values().length ? SoulType.values()[o] : null;
      } else {
         return this.subject >= 0 && this.subject < Recruit.values().length ? Recruit.values()[this.subject].soulType : null;
      }
   }

   private String subjectName() {
      if (this.isYou()) {
         return "";
      } else if (this.isCustom()) {
         String[] n = TurnBattleClientMod.customRecruitNames;
         return n != null && this.ci() < n.length ? n[this.ci()] : "Vessel";
      } else {
         return this.subject >= 0 && this.subject < Recruit.values().length ? Recruit.values()[this.subject].displayName : "";
      }
   }

   private CharStats stats() {
      CharStats c = this.isYou()
         ? TurnBattleClientMod.clientPlayerStats
         : (this.isCustom() ? at(TurnBattleClientMod.customRecruitStats, this.ci()) : at(TurnBattleClientMod.clientRecruitStats, this.subject));
      return c != null ? c : new CharStats();
   }

   private LivingEntity caster() {
      if (this.client == null) {
         return null;
      } else if (this.isYou()) {
         return this.client.player;
      } else {
         try {
            return BattleEntityRenderer.recruitFake(this.subjectName());
         } catch (Throwable var2) {
            return null;
         }
      }
   }

   private ItemStack casterWeapon() {
      ItemStack w = this.isYou()
         ? TurnBattleClientMod.clientEquippedWeapon
         : (this.isCustom() ? at(TurnBattleClientMod.customRecruitWeapons, this.ci()) : at(TurnBattleClientMod.clientRecruitWeapons, this.subject));
      return w != null ? w : ItemStack.EMPTY;
   }

   private List<SkillTreeDefs.GlobalNode> fullNodeSet() {
      if (this.isMonster()) {
         return SkillTreeDefs.getAllNodesForMonster();
      } else {
         return this.isStoryRecruit() ? SkillTreeDefs.getRenderNodesForRecruit(Recruit.values()[this.subject]) : SkillTreeDefs.getAllNodesForHuman();
      }
   }

   protected void init() {
      if (this.openedNanos == 0L) {
         this.openedNanos = this.arrivalNanos = System.nanoTime();
         this.selectedSoulTab = isValidHumanTab(this.soul()) ? this.soul() : SoulType.DETERMINATION;
         this.reloadNodes();
         this.cameraX = this.cameraY = this.camTX = this.camTY = 0.0F;
         this.cameraZoom = 0.72F;
         this.camTZoom = 1.0F;
         if (this.isStoryRecruit() && !this.allNodes.isEmpty()) {
            Set have = this.unlocked();
            SkillTreeDefs.GlobalNode next = this.allNodes.get(this.allNodes.size() - 1);

            for (SkillTreeDefs.GlobalNode gn : this.allNodes) {
               if (!have.contains(gn.skill())) {
                  next = gn;
                  break;
               }
            }

            this.camTX = next.worldX() * 0.5F;
            this.camTY = Math.min(0.0F, next.worldY() + 110.0F);
         }

         this.lastKnownUnlocked = new HashSet<>(this.unlocked());
         this.lastKnownAvailable = this.computeAvailableSkillNames();
         this.lastFrameNanos = System.nanoTime();
      }
   }

   private static boolean isValidHumanTab(SoulType s) {
      if (s == null) {
         return false;
      } else {
         for (SoulType t : HUMAN_SOUL_TABS) {
            if (t == s) {
               return true;
            }
         }

         return false;
      }
   }

   private void reloadNodes() {
      this.allNodes = this.isMonster()
         ? SkillTreeDefs.getAllNodesForMonster()
         : (
            this.isStoryRecruit()
               ? SkillTreeDefs.getRenderNodesForRecruit(Recruit.values()[this.subject])
               : SkillTreeDefs.getRenderNodesForHuman(this.selectedSoulTab)
         );
      this.nodesBySkill.clear();
      this.maxNodeDist = 1.0F;

      for (SkillTreeDefs.GlobalNode gn : this.allNodes) {
         this.nodesBySkill.put(gn.skill(), gn);
         this.maxNodeDist = Math.max(this.maxNodeDist, (float)Math.hypot((double)gn.worldX(), (double)gn.worldY()));
      }
   }

   public void removed() {
      CloneAnimator.releaseMenuPoses(PREVIEW_KEY);
   }

   public void tick() {
      this.lastTickRealTimeNanos = System.nanoTime();
      float target = this.selectedNode != null && !this.wantClosed ? 1.0F : 0.0F;
      if (target != this.transitionTargetT) {
         this.transitionFromT = this.computeCurrentAnimT();
         this.transitionTargetT = target;
         this.transitionStartNanos = System.nanoTime();
      }

      if (this.computeCurrentAnimT() == 0.0F && this.wantClosed) {
         this.selectedNode = null;
         this.wantClosed = false;
         this.stopPreview();
      }

      Set nowUnlocked = this.unlocked();
      long now = System.nanoTime();

      for (String skill : nowUnlocked) {
         if (!this.lastKnownUnlocked.contains(skill)) {
            this.unlockPulseStartNanos.put(skill, now);
         }
      }

      this.unlockPulseStartNanos.entrySet().removeIf(e -> now - e.getValue() > 1100000000L);
      this.lastKnownUnlocked = new HashSet<>(nowUnlocked);
      Set nowAvailable = this.computeAvailableSkillNames();

      for (String skillx : nowAvailable) {
         if (!this.lastKnownAvailable.contains(skillx)) {
            this.branchOpenStartNanos.put(skillx, now);
         }
      }

      this.branchOpenStartNanos.entrySet().removeIf(e -> now - e.getValue() > 700000000L);
      this.lastKnownAvailable = nowAvailable;
      this.tickPreview();
   }

   private Set<String> computeAvailableSkillNames() {
      Set unlocked = this.unlocked();
      Set out = new HashSet<>();

      for (SkillTreeDefs.GlobalNode gn : this.allNodes) {
         if (!unlocked.contains(gn.skill()) && prereqsMetLocal(gn, unlocked)) {
            out.add(gn.skill());
         }
      }

      return out;
   }

   private float computeCurrentAnimT() {
      long elapsed = System.nanoTime() - this.transitionStartNanos;
      float t = Math.min(1.0F, (float)elapsed / 3.8E8F);
      return this.transitionFromT + (this.transitionTargetT - this.transitionFromT) * t;
   }

   private void startPreview(SkillTreeDefs.GlobalNode gn) {
      this.stopPreview();
      this.previewSkill = SkillTreeDefs.resolveSkill(gn);
      this.previewTick = 0;
      this.previewNextCast = 10;
      if (this.client != null && this.client.world != null) {
         try {
            this.previewStand = new ArmorStandEntity(this.client.world, 0.0, 0.0, 0.0);
         } catch (Throwable var3) {
            this.previewStand = null;
         }
      }

      CloneAnimator.setSoul(PREVIEW_KEY, this.soul());
   }

   private void stopPreview() {
      this.previewEffect = null;
      this.previewSkill = null;
      this.previewStand = null;
      this.popups.clear();
      this.standHurt = 0;
      CloneAnimator.releaseMenuPoses(PREVIEW_KEY);
   }

   private void tickPreview() {
      if (this.selectedNode != null && !this.wantClosed && this.previewSkill != null) {
         this.previewTick++;
         if (this.standHurt > 0) {
            this.standHurt--;
         }

         this.popups.removeIf(p -> this.previewTick - p.born() > 26);
         if (!(this.computeCurrentAnimT() < 0.85F)) {
            if (this.previewEffect == null && this.previewTick >= this.previewNextCast) {
               int color = this.soulRgb();
               this.previewOnSelf = isSupport(this.previewSkill);

               try {
                  CloneAnimator.play(PREVIEW_KEY, CloneAnimator.Act.SKILL, color, null, CastAnims.castFor(this.previewSkill));
               } catch (Throwable var3) {
               }

               this.previewEffect = SkillEffect.forSkill(this.previewSkill, 0, 0, color);
               this.previewEffect.volume = 0.3F;
            }

            if (this.previewEffect != null) {
               this.previewEffect.tick();
               if (this.previewEffect.isAtHitFrame()) {
                  this.onPreviewHit();
               }

               if (this.previewEffect.isDone()) {
                  this.previewEffect = null;
                  this.previewNextCast = this.previewTick + 26;
               }
            }
         }
      }
   }

   private void onPreviewHit() {
      BattleSkill s = this.previewSkill;
      if (s != null) {
         SkillType t = s.getType();
         String txt;
         int rgb;
         if (t == SkillType.HEAL) {
            txt = s.getMinVal() == 999 ? "FULL" : "+" + Math.round((float)s.roll(this.previewRng) * BattleSkill.healMult());
            rgb = 5635959;
         } else if (t == SkillType.SHIELD) {
            txt = "SHIELD";
            rgb = 8947967;
         } else if (t == SkillType.BUFF) {
            txt = "ATK UP";
            rgb = 16768324;
         } else if (s.getMinVal() > 0 && s.getMinVal() != 999) {
            float mult = t == SkillType.MAGNET ? BattleSkill.tpMult() * 0.5F : BattleSkill.damageMult();
            txt = String.valueOf(Math.max(1, Math.round((float)s.roll(this.previewRng) * mult)));
            rgb = 16777215;
            this.standHurt = 10;
         } else {
            txt = pretty(t.name()).toUpperCase();
            rgb = 16777062;
            this.standHurt = 6;
         }

         this.popups.add(new SkillTreeScreen.Popup(txt, rgb, this.previewTick, this.previewOnSelf));
      }
   }

   private static boolean isSupport(BattleSkill s) {
      SkillType t = s.getType();
      return t == SkillType.HEAL || t == SkillType.SHIELD || t == SkillType.BUFF || t == SkillType.MAGNET;
   }

   private int soulRgb() {
      SoulType s = this.soul();
      return s != null ? s.getColor() & 16777215 : 16724787;
   }

   private float screenXf(float worldX) {
      return (float)this.width / 2.0F + (worldX - this.cameraX) * this.cameraZoom;
   }

   private float screenYf(float worldY) {
      return (float)this.height / 2.0F + 6.0F + (worldY - this.cameraY) * this.cameraZoom;
   }

   private float secs() {
      return (float)(System.nanoTime() - this.openedNanos) / 1.0E9F;
   }

   private float arrival(SkillTreeDefs.GlobalNode gn) {
      float d = (float)Math.hypot((double)gn.worldX(), (double)gn.worldY()) / this.maxNodeDist;
      float since = (float)(System.nanoTime() - this.arrivalNanos) / 1.0E9F;
      return clamp01((since - 0.25F - d * 0.75F) / 0.4F);
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      if (mx != this.lastMouseX || my != this.lastMouseY) {
         this.lastMouseX = mx;
         this.lastMouseY = my;
         if (this.keysMoved) {
            this.keysMoved = false;
         } else {
            this.keyMode = false;
         }
      }

      if (this.lastTickRealTimeNanos == 0L) {
         this.lastTickRealTimeNanos = System.nanoTime();
      }

      this.lastRenderDelta = clamp01((float)(System.nanoTime() - this.lastTickRealTimeNanos) / 5.0E7F);
      long nowN = System.nanoTime();
      float dt = Math.min(0.1F, (float)(nowN - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = nowN;
      float k = 1.0F - (float)Math.exp((double)(-dt * (this.dragging ? 30.0F : 7.0F)));
      this.cameraX = this.cameraX + (this.camTX - this.cameraX) * k;
      this.cameraY = this.cameraY + (this.camTY - this.cameraY) * k;
      this.cameraZoom = this.cameraZoom + (this.camTZoom - this.cameraZoom) * k;
      float panelT = this.computeCurrentAnimT();
      boolean panelOpen = this.selectedNode != null || panelT > 0.01F;
      Set unlocked = this.unlocked();
      Set unequipped = this.unequipped();
      this.renderVoid(ctx);
      this.renderThreads(ctx, unlocked);
      this.hoveredNode = null;
      this.renderLights(ctx, mx, my, unlocked, unequipped, panelOpen);
      String nowHoveredSkill = this.hoveredNode != null ? this.hoveredNode.skill() : null;
      if (nowHoveredSkill != null && !nowHoveredSkill.equals(this.lastHoveredSkill)) {
         TurnBattleSounds.playHover();
         this.hoverStartNanos = nowN;
      }

      this.lastHoveredSkill = nowHoveredSkill;
      if (this.hoveredNode != null && !panelOpen) {
         this.renderHoverTag(ctx, this.hoveredNode, unlocked);
      }

      ctx.draw();
      if (panelOpen) {
         this.renderOpened(ctx, mx, my, panelT);
      }

      this.renderHeader(ctx, mx, my);
   }

   private void renderVoid(DrawContext ctx) {
      float t = this.secs();
      float in = ease(clamp01(t / 0.6F));
      ctx.fillGradient(0, 0, this.width, this.height, -16645624, -16119784);
      int tint = this.soul() != null ? this.soul().getColor() & 16777215 : 12568831;
      float hx = this.screenXf(0.0F);
      float hy = this.screenYf(0.0F);
      float pool = 260.0F * this.cameraZoom;
      MenuBackdrop.tex(ctx, TEX_GLOW, hx, hy, pool, pool, tint, 0.2F * in);
      MenuBackdrop.tex(ctx, TEX_GLOW, hx, hy, pool * 0.35F, pool * 0.35F, 16777215, 0.12F * in);

      for (int r = 0; r < 4; r++) {
         float q = (t * 0.05F + (float)r / 4.0F) % 1.0F;
         float rs = pool * (0.3F + q * 3.2F);
         MenuBackdrop.tex(ctx, TEX_RING, hx, hy, rs, rs, tint, 0.16F * (1.0F - q) * in);
      }

      for (int i = 0; i < 110; i++) {
         float par = 0.25F + CloneAnimator.rnd(i * 5) * 0.35F;
         float wx = (CloneAnimator.rnd(i * 3 + 1) - 0.5F) * 1800.0F;
         float speed = 4.0F + CloneAnimator.rnd(i * 7) * 9.0F;
         float wy = ((CloneAnimator.rnd(i * 3 + 2) * 1400.0F - t * speed) % 1400.0F + 1400.0F) % 1400.0F - 700.0F;
         float sx = (float)this.width / 2.0F + (wx - this.cameraX * par) * (0.6F + this.cameraZoom * 0.4F) * par * 1.6F;
         float sy = (float)this.height / 2.0F + (wy - this.cameraY * par) * (0.6F + this.cameraZoom * 0.4F) * par * 1.6F;
         if (!(sx < -4.0F) && !(sx > (float)(this.width + 4)) && !(sy < -4.0F) && !(sy > (float)(this.height + 4))) {
            float tw = 0.5F + 0.5F * (float)Math.sin((double)(t * (1.2F + CloneAnimator.rnd(i) * 2.0F) + (float)i));
            float a = (0.1F + 0.35F * tw) * par * 1.6F * in;
            int px = par > 0.5F ? 2 : 1;
            ctx.fill((int)sx, (int)sy, (int)sx + px, (int)sy + px, MenuBackdrop.argb(i % 4 == 0 ? 16777215 : tint, a));
         }
      }

      int e = Math.max(20, this.height / 5);
      ctx.fillGradient(0, 0, this.width, e, -1342177280, 0);
      ctx.fillGradient(0, this.height - e, this.width, this.height, 0, -1342177280);
   }

   private void renderThreads(DrawContext ctx, Set<String> unlocked) {
      float t = this.secs();

      for (SkillTreeDefs.GlobalNode gn : this.allNodes) {
         for (String prereq : gn.node().prereqs()) {
            SkillTreeDefs.GlobalNode pn = this.findNode(prereq);
            if (pn != null) {
               float a = Math.min(this.arrival(pn), this.arrival(gn));
               if (!(a <= 0.01F)) {
                  float x0 = this.screenXf(pn.worldX());
                  float y0 = this.screenYf(pn.worldY());
                  float x1 = this.screenXf(gn.worldX());
                  float y1 = this.screenYf(gn.worldY());
                  if (!this.offscreen(x0, y0, 60.0F) || !this.offscreen(x1, y1, 60.0F)) {
                     float gx = x0 + (x1 - x0) * ease(a);
                     float gy = y0 + (y1 - y0) * ease(a);
                     boolean both = unlocked.contains(gn.skill()) && unlocked.contains(prereq);
                     boolean open = unlocked.contains(prereq);
                     int col = gn.node().color() & 16777215;
                     if (both) {
                        CloneAnimator.line(ctx, x0, y0, gx, gy, 4.0F * this.cameraZoom, MenuBackdrop.argb(col, 0.16F * a));
                        CloneAnimator.line(ctx, x0, y0, gx, gy, 1.4F, MenuBackdrop.argb(lerpRgb(col, 16777215, 0.5F), 0.85F * a));
                        float q = (t * 0.45F + CloneAnimator.rnd(gn.skill().hashCode() & 65535)) % 1.0F;
                        CloneAnimator.star4(
                           ctx, x0 + (x1 - x0) * q, y0 + (y1 - y0) * q, 1.6F, MenuBackdrop.argb(16777215, 0.7F * a * (float)Math.sin(Math.PI * (double)q))
                        );
                     } else if (open) {
                        CloneAnimator.line(ctx, x0, y0, gx, gy, 1.2F, MenuBackdrop.argb(col, 0.45F * a));
                     } else {
                        int n = Math.max(2, (int)(Math.hypot((double)(gx - x0), (double)(gy - y0)) / 7.0));

                        for (int i = 0; i <= n; i++) {
                           float f = (float)i / (float)n;
                           float px = x0 + (gx - x0) * f;
                           float py = y0 + (gy - y0) * f;
                           ctx.fill((int)px, (int)py, (int)px + 1, (int)py + 1, MenuBackdrop.argb(8028057, 0.35F * a));
                        }
                     }

                     Long openStart = this.branchOpenStartNanos.get(gn.skill());
                     if (openStart != null) {
                        float ot = clamp01((float)(System.nanoTime() - openStart) / 7.0E8F);
                        float fx = x0 + (x1 - x0) * ot;
                        float fy = y0 + (y1 - y0) * ot;
                        CloneAnimator.line(ctx, x0, y0, fx, fy, 2.0F, MenuBackdrop.argb(16768324, 1.0F - ot * 0.5F));
                        CloneAnimator.star4(ctx, fx, fy, 3.0F, MenuBackdrop.argb(16777215, 1.0F - ot));
                     }
                  }
               }
            }
         }
      }
   }

   private void renderLights(DrawContext ctx, int mx, int my, Set<String> unlocked, Set<String> unequipped, boolean panelOpen) {
      float t = this.secs();

      for (SkillTreeDefs.GlobalNode gn : this.allNodes) {
         float a = this.arrival(gn);
         if (!(a <= 0.01F)) {
            float sx = this.screenXf(gn.worldX());
            float sy = this.screenYf(gn.worldY());
            if (!this.offscreen(sx, sy, 50.0F)) {
               boolean isUnlocked = unlocked.contains(gn.skill());
               boolean prereqsOk = prereqsMetLocal(gn, unlocked);
               boolean canAfford = this.skillPoints() >= gn.node().cost();
               boolean equipped = isUnlocked && !unequipped.contains(gn.skill());
               float r = 9.0F * this.cameraZoom;
               boolean hov = !panelOpen
                  && (
                     this.keyMode
                        ? gn == this.keyNode
                        : sy > 40.0F && Math.hypot((double)((float)mx - sx), (double)((float)my - sy)) <= (double)Math.max(7.0F, r * 1.25F)
                  );
               if (hov) {
                  this.hoveredNode = gn;
               }

               boolean chosen = this.selectedNode == gn;
               int col = gn.node().color() & 16777215;
               float pop = backOut(a);
               float twinkle = 0.88F + 0.12F * (float)Math.sin((double)(t * 2.2F + (float)(gn.skill().hashCode() & 0xFF)));
               float glowA;
               float coreA;
               if (isUnlocked) {
                  glowA = 0.75F;
                  coreA = 1.0F;
               } else if (prereqsOk && canAfford) {
                  glowA = 0.42F + 0.18F * (float)Math.sin((double)(t * 3.4F));
                  coreA = 0.85F;
               } else if (prereqsOk) {
                  glowA = 0.28F;
                  coreA = 0.6F;
               } else {
                  glowA = 0.09F;
                  coreA = 0.28F;
               }

               float grow = (!hov && !chosen ? 1.0F : 1.3F) * pop;
               float rr = r * grow;
               MenuBackdrop.tex(ctx, TEX_GLOW, sx, sy, rr * 6.5F, rr * 6.5F, col, glowA * twinkle * a);
               if (isUnlocked) {
                  MenuBackdrop.tex(ctx, TEX_GLOW, sx, sy, rr * 2.6F, rr * 2.6F, 16777215, 0.35F * a);
               }

               if (equipped) {
                  float q = t * 0.6F % 1.0F;
                  MenuBackdrop.tex(ctx, TEX_RING, sx, sy, rr * 3.4F, rr * 3.4F, col, 0.55F * a);
                  MenuBackdrop.tex(ctx, TEX_RING, sx, sy, rr * (3.4F + q * 2.4F), rr * (3.4F + q * 2.4F), col, 0.35F * (1.0F - q) * a);
               }

               int coreCol = isUnlocked ? lerpRgb(col, 16777215, 0.55F) : (prereqsOk ? lerpRgb(col, 16777215, 0.2F) : lerpRgb(col, 2236979, 0.55F));
               CloneAnimator.diamond(ctx, sx, sy, rr * 0.55F, MenuBackdrop.argb(coreCol, coreA * a));
               if (isUnlocked || prereqsOk && canAfford) {
                  CloneAnimator.star4(ctx, sx, sy, rr * (isUnlocked ? 0.95F : 0.7F) * twinkle, MenuBackdrop.argb(16777215, (isUnlocked ? 0.9F : 0.55F) * a));
               }

               if (hov) {
                  CloneAnimator.star4(ctx, sx, sy, rr * 1.5F, MenuBackdrop.argb(16777215, 0.5F));
               }

               Identifier icon = SKILL_ICONS.get(gn.skill());
               if (icon != null && (hov || chosen) && rr > 6.0F) {
                  int half = (int)(rr * 0.8F);
                  RenderSystem.enableBlend();
                  ctx.drawTexture(icon, (int)sx - half, (int)sy - half, half * 2, half * 2, 0.0F, 0.0F, 35, 35, 35, 35);
               }

               Long pulseStart = this.unlockPulseStartNanos.get(gn.skill());
               if (pulseStart != null) {
                  float pt = clamp01((float)(System.nanoTime() - pulseStart) / 1.1E9F);
                  float pr = rr * (2.0F + pt * 9.0F);
                  MenuBackdrop.tex(ctx, TEX_RING, sx, sy, pr, pr, 16768324, 0.9F * (1.0F - pt));
                  MenuBackdrop.tex(ctx, TEX_GLOW, sx, sy, rr * 8.0F * (1.0F - pt * 0.5F), rr * 8.0F * (1.0F - pt * 0.5F), 16777215, 0.6F * (1.0F - pt));

                  for (int i = 0; i < 8; i++) {
                     double ang = (double)i * Math.PI / 4.0 + (double)pt * 0.8;
                     float d = rr * (1.5F + pt * 6.0F);
                     CloneAnimator.star4(
                        ctx, sx + (float)Math.cos(ang) * d, sy + (float)Math.sin(ang) * d, 2.4F * (1.0F - pt), MenuBackdrop.argb(16777215, 1.0F - pt)
                     );
                  }
               }
            }
         }
      }
   }

   private void renderHoverTag(DrawContext ctx, SkillTreeDefs.GlobalNode gn, Set<String> unlocked) {
      float ht = ease(clamp01((float)(System.nanoTime() - this.hoverStartNanos) / 1.6E8F));
      BattleSkill skill = SkillTreeDefs.resolveSkill(gn);
      String name = skill != null ? skill.getDisplayName() : pretty(gn.skill());
      boolean isUnlocked = unlocked.contains(gn.skill());
      String cost;
      int costCol;
      if (isUnlocked) {
         cost = "Slot cost: " + slotCost(gn);
         costCol = 8378623;
      } else {
         cost = gn.node().cost() + " SP";
         int souls = soulsRequiredFor(gn);
         if (souls > 0) {
            cost = cost + "  \u00b7  " + souls + " SOULs";
         }

         boolean ok = prereqsMetLocal(gn, unlocked);
         if (!ok) {
            cost = cost + "  \u00b7  locked";
         }

         costCol = !ok ? 9080744 : (this.skillPoints() >= gn.node().cost() ? 16768324 : 16742246);
      }

      int w = Math.max(this.textRenderer.getWidth(name), this.textRenderer.getWidth(cost)) + 14;
      int h = 28;
      float sx = this.screenXf(gn.worldX());
      float sy = this.screenYf(gn.worldY());
      int x = (int)(sx - (float)w / 2.0F);
      int y = (int)(sy - 9.0F * this.cameraZoom * 1.3F - (float)h - 8.0F - (1.0F - ht) * 6.0F);
      x = Math.max(4, Math.min(this.width - w - 4, x));
      if (y < 44) {
         y = (int)(sy + 9.0F * this.cameraZoom * 1.3F + 8.0F);
      }

      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, 0.0F, 200.0F);
      int col = gn.node().color() & 16777215;
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(329230, 0.88F * ht));
      ctx.fill(x, y, x + w, y + 1, MenuBackdrop.argb(col, ht));
      ctx.fill(x, y + h - 1, x + w, y + h, MenuBackdrop.argb(col, 0.5F * ht));
      ctx.drawTextWithShadow(this.textRenderer, name, x + 7, y + 4, MenuBackdrop.argb(16777215, Math.max(0.05F, ht)));
      ctx.drawTextWithShadow(this.textRenderer, cost, x + 7, y + 16, MenuBackdrop.argb(costCol, Math.max(0.05F, ht)));
      m.pop();
   }

   private void renderOpened(DrawContext ctx, int mx, int my, float t) {
      SkillTreeDefs.GlobalNode gn = this.selectedNode;
      if (gn != null) {
         float e = ease(t);
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(0.0F, 0.0F, 300.0F);
         ctx.fill(0, 0, this.width, this.height, MenuBackdrop.argb(5, 0.55F * e));
         float lx = this.screenXf(gn.worldX());
         float ly = this.screenYf(gn.worldY());
         int fullW = (int)Math.max(170.0F, Math.min(300.0F, (float)this.width * 0.42F));
         int fullH = (int)((float)fullW * 0.6F);
         int gap = 10;
         int iw = (int)Math.max(150.0F, Math.min(220.0F, (float)this.width * 0.32F));
         boolean side = this.width >= fullW + gap + iw + 20;
         float groupW = side ? (float)(fullW + gap + iw) : (float)fullW;
         float cx = side ? lx - groupW / 2.0F + (float)fullW / 2.0F : lx;
         float cy = side ? ly : ly - 34.0F;
         cx = Math.max((float)fullW / 2.0F + 6.0F, Math.min((float)(this.width - (side ? iw + gap : 0)) - (float)fullW / 2.0F - 6.0F, cx));
         cy = Math.max(40.0F + (float)fullH / 2.0F + 8.0F, Math.min((float)this.height - (float)fullH / 2.0F - (float)(side ? 8 : 90), cy));
         this.pvW = Math.max(2, (int)((float)fullW * e));
         this.pvH = Math.max(2, (int)((float)fullH * e));
         this.pvX = (int)(cx - (float)this.pvW / 2.0F);
         this.pvY = (int)(cy - (float)this.pvH / 2.0F);
         int col = gn.node().color() & 16777215;
         ctx.fill(this.pvX - 3, this.pvY - 3, this.pvX + this.pvW + 3, this.pvY + this.pvH + 3, MenuBackdrop.argb(col, 0.25F * e));
         ctx.fill(this.pvX - 2, this.pvY - 2, this.pvX + this.pvW + 2, this.pvY + this.pvH + 2, MenuBackdrop.argb(16777215, e));
         ctx.fill(this.pvX, this.pvY, this.pvX + this.pvW, this.pvY + this.pvH, -16777216);
         if (e > 0.2F) {
            this.renderPreview(ctx, e);
         }

         m.pop();
         m.push();
         m.translate(0.0F, 0.0F, 400.0F);
         if (side) {
            this.infoX = this.pvX + this.pvW + gap + 3;
            this.infoY = this.pvY - 2;
            this.infoW = iw;
            this.infoH = Math.max(this.pvH + 4, 0);
         } else {
            this.infoX = this.pvX - 2;
            this.infoY = this.pvY + this.pvH + 8;
            this.infoW = this.pvW + 4;
            this.infoH = Math.max(0, (int)((float)(this.height - this.infoY - 6) * 1.0F));
         }

         float ia = clamp01((t - 0.45F) / 0.55F);
         if (ia > 0.01F) {
            this.renderInfo(ctx, gn, mx, my, ia, side);
         }

         m.pop();
      }
   }

   private void renderPreview(DrawContext ctx, float e) {
      ctx.enableScissor(this.pvX, this.pvY, this.pvX + this.pvW, this.pvY + this.pvH);
      int frame = (int)(System.currentTimeMillis() / 30L % 100L);
      float srcR = 1.3333334F;
      float dstR = (float)this.pvW / (float)this.pvH;
      int dw;
      int dh;
      if (dstR > srcR) {
         dw = this.pvW;
         dh = (int)((float)this.pvW / srcR);
      } else {
         dh = this.pvH;
         dw = (int)((float)this.pvH * srcR);
      }

      if (TurnBattleConfig.get().deltarune_battle_bg) {
         ctx.drawTexture(BG_TEX[frame], this.pvX + (this.pvW - dw) / 2, this.pvY + (this.pvH - dh) / 2, dw, dh, 0.0F, 0.0F, 640, 480, 640, 480);
      } else {
         ctx.fill(this.pvX, this.pvY, this.pvX + this.pvW, this.pvY + this.pvH, -16119276);
      }

      float scale = (float)this.pvH * 0.21F;
      int footY = this.pvY + (int)((float)this.pvH * 0.8F);
      int youX = this.pvX + (int)((float)this.pvW * 0.27F);
      int standX = this.pvX + (int)((float)this.pvW * 0.74F);
      MenuBackdrop.tex(ctx, TEX_GLOW, (float)youX, (float)footY, scale * 2.2F, scale * 0.5F, 0, 0.7F);
      MenuBackdrop.tex(ctx, TEX_GLOW, (float)standX, (float)footY, scale * 2.2F, scale * 0.5F, 0, 0.7F);
      ctx.draw();
      RenderSystem.clear(256, false);
      if (this.client != null && this.client.player != null && e > 0.6F) {
         try {
            CloneAnimator.Frame fr = CloneAnimator.frame(PREVIEW_KEY, true);
            LivingEntity who = this.caster();
            if (who != null) {
               BattleEntityRenderer.renderEntity(
                  ctx, who, youX, footY + (int)(scale * 0.05F), scale, 45.0F, BattleEntityRenderer.BattleAnim.IDLE, this.casterWeapon(), fr, 1
               );
            }
         } catch (Throwable var24) {
         }

         if (this.previewStand != null) {
            try {
               BattleEntityRenderer.renderEntity(
                  ctx,
                  this.previewStand,
                  standX,
                  footY + (int)(scale * 0.05F),
                  scale,
                  -45.0F,
                  this.standHurt > 0 ? BattleEntityRenderer.BattleAnim.HURT : BattleEntityRenderer.BattleAnim.IDLE,
                  null
               );
            } catch (Throwable var23) {
            }
         }
      }

      ctx.draw();
      RenderSystem.clear(256, false);
      int tgtX = this.previewOnSelf ? youX : standX;
      int tgtY = footY - (int)(scale * 0.9F);
      if (this.previewEffect != null) {
         this.previewEffect.setCenter(tgtX, tgtY);
         this.previewEffect.scale = clamp((float)this.pvH / ((float)this.height * 0.55F), 0.35F, 1.0F);

         try {
            this.previewEffect.render(ctx, this.lastRenderDelta);
         } catch (Throwable var22) {
         }
      }

      for (SkillTreeScreen.Popup p : this.popups) {
         float age = (float)(this.previewTick - p.born()) + this.lastRenderDelta;
         float rise = Math.min(1.0F, age / 8.0F);
         float pa = age < 18.0F ? 1.0F : clamp01(1.0F - (age - 18.0F) / 8.0F);
         int px = p.onSelf() ? youX : standX;
         int py = (int)((float)footY - scale * 2.1F - rise * 10.0F);
         Text txt = Text.literal(p.text()).setStyle(Style.EMPTY.withFont(DAMAGE_FONT));
         ctx.drawTextWithShadow(this.textRenderer, txt, px - this.textRenderer.getWidth(txt) / 2, py, MenuBackdrop.argb(p.rgb(), pa));
      }

      ctx.disableScissor();
      ctx.drawTextWithShadow(this.textRenderer, "PREVIEW", this.pvX + 4, this.pvY + 4, MenuBackdrop.argb(16777215, 0.55F * e));
   }

   private void renderInfo(DrawContext ctx, SkillTreeDefs.GlobalNode gn, int mx, int my, float a, boolean side) {
      BattleSkill skill = SkillTreeDefs.resolveSkill(gn);
      Set unlocked = this.unlocked();
      boolean isUnlocked = unlocked.contains(gn.skill());
      boolean isEquipped = isUnlocked && !this.unequipped().contains(gn.skill());
      int col = gn.node().color() & 16777215;
      int slide = (int)((1.0F - ease(a)) * 12.0F);
      int x = this.infoX + slide;
      int y = this.infoY;
      int w = this.infoW;
      List rows = new ArrayList<>();
      String typeLine = skill != null ? pretty(skill.getType().name()) : "";
      if (skill != null && skill.getAffinities() != null && skill.getAffinities().length > 0) {
         StringBuilder sb = new StringBuilder();

         for (Affinity af : skill.getAffinities()) {
            if (sb.length() > 0) {
               sb.append(" / ");
            }

            sb.append(pretty(af.name()));
         }

         typeLine = typeLine + "  \u00b7  " + sb;
      }

      if (skill != null) {
         rows.add(new String[]{skill == LightnerSkill.SNOWGRAVE ? "TP cost: double your max TP" : "TP cost: " + skill.getTpCost() + "%", "B07CFF"});
         if (skill.getMinVal() > 0) {
            rows.add(new String[]{"Power: " + skill.getScaledRangeDisplay(), "FFFFFF"});
         }

         if (SkillScaling.scales(skill)) {
            int pts = SkillScaling.statOf(this.stats(), skill);
            rows.add(
               new String[]{
                  "Scales with "
                     + SkillScaling.statName(skill)
                     + (SkillScaling.usesOffense(skill) ? " (physical)" : "")
                     + "  \u00b7  +"
                     + Math.round((float)pts * 0.017F * 100.0F)
                     + "% now",
                  Integer.toHexString(SkillScaling.statColor(skill))
               }
            );
         }
      }

      List needs = new ArrayList<>();

      for (String pr : gn.node().prereqs()) {
         if (!unlocked.contains(pr)) {
            SkillTreeDefs.GlobalNode pn = this.findNode(pr);
            BattleSkill ps = pn != null ? SkillTreeDefs.resolveSkill(pn) : null;
            needs.add(ps != null ? ps.getDisplayName() : pretty(pr));
         }
      }

      if (!isUnlocked && !needs.isEmpty()) {
         rows.add(new String[]{"Needs: " + String.join(", ", needs), "FF8877"});
      }

      if (skill != null && skill.getLevelRequired() > 1) {
         rows.add(
            new String[]{
               "Requires LV " + skill.getLevelRequired() + (this.level() < skill.getLevelRequired() ? "   (you're LV " + this.level() + ")" : ""),
               this.level() < skill.getLevelRequired() ? "FF8877" : "88DD88"
            }
         );
      }

      int souls = soulsRequiredFor(gn);
      if (!isUnlocked && souls > 0) {
         rows.add(new String[]{"SOULs absorbed: " + TurnBattleClientMod.clientAbsorbedSouls.size() + " / " + souls, "DD88FF"});
      }

      if (isUnlocked) {
         rows.add(
            new String[]{
               "Slot cost: " + slotCost(gn) + "   (" + this.computeSlotsUsed() + " / " + SkillTreeDefs.maxSkillSlots(this.level()) + " used)", "7FD8FF"
            }
         );
      } else {
         rows.add(new String[]{"Skill points: " + gn.node().cost() + "   (you have " + this.skillPoints() + ")", "FFDD44"});
      }

      String status = isEquipped
         ? "LEARNED  \u00b7  EQUIPPED"
         : (
            isUnlocked
               ? "LEARNED"
               : (
                  skill != null && skill.getLevelRequired() > this.level()
                     ? "NEEDS LV " + skill.getLevelRequired()
                     : (prereqsMetLocal(gn, unlocked) ? "CAN BE LEARNED" : "LOCKED")
               )
         );
      List desc = skill != null ? this.textRenderer.wrapLines(StringVisitable.plain(skill.getContextDescription()), w - 14) : List.of();
      List rowLines = new ArrayList<>();

      for (String[] r : rows) {
         rowLines.add(this.textRenderer.wrapLines(StringVisitable.plain(r[0]), w - 14));
      }

      int contentH = 32 + desc.size() * 10 + 6;

      for (List rl : rowLines) {
         contentH += rl.size() * 10 + 1;
      }

      contentH += 46;
      int h = side ? Math.max(this.infoH, contentH) : Math.min(this.infoH, contentH);
      if (side && y + h > this.height - 4) {
         y = Math.max(44, this.height - 4 - h);
      }

      this.hitX = x - 2;
      this.hitY = y - 2;
      this.hitW = w + 4;
      this.hitH = h + 4;
      ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, MenuBackdrop.argb(16777215, a));
      ctx.fill(x, y, x + w, y + h, MenuBackdrop.argb(329230, 0.96F * a));
      ctx.fill(x, y, x + w, y + 2, MenuBackdrop.argb(col, a));
      int ty = y + 7;
      String name = skill != null ? skill.getDisplayName() : pretty(gn.skill());
      ctx.drawTextWithShadow(
         this.textRenderer, Text.literal(name).setStyle(Style.EMPTY.withBold(true)), x + 7, ty, MenuBackdrop.argb(lerpRgb(col, 16777215, 0.35F), a)
      );
      ty += 12;
      ctx.drawTextWithShadow(this.textRenderer, typeLine, x + 7, ty, MenuBackdrop.argb(9413560, a));
      ty += 14;

      for (OrderedText line : desc) {
         ctx.drawTextWithShadow(this.textRenderer, line, x + 7, ty, MenuBackdrop.argb(13423581, a));
         ty += 10;
      }

      ty += 6;

      for (int i = 0; i < rows.size(); i++) {
         int rgb = Integer.parseInt(rows.get(i)[1], 16);

         for (OrderedText line : rowLines.get(i)) {
            ctx.drawTextWithShadow(this.textRenderer, line, x + 7, ty, MenuBackdrop.argb(rgb, a));
            ty += 10;
         }

         ty++;
      }

      ty += 4;
      int stCol = isUnlocked ? 8978329 : (prereqsMetLocal(gn, unlocked) ? 16768324 : 8947865);
      ctx.drawTextWithShadow(this.textRenderer, status, x + 7, ty, MenuBackdrop.argb(stCol, a));
      String label = !isUnlocked ? "LEARN" : (isEquipped ? "UNEQUIP" : "EQUIP");
      String why = this.blockReason(gn);
      this.btnW = Math.min(w - 14, 110);
      this.btnH = 18;
      this.btnX = x + (w - this.btnW) / 2;
      this.btnY = y + h - this.btnH - 8;
      this.buttonHovered = mx >= this.btnX && mx < this.btnX + this.btnW && my >= this.btnY && my < this.btnY + this.btnH;
      boolean ok = why == null;
      int border = !ok ? 5592422 : (this.buttonHovered ? 16777024 : 16777215);
      ctx.fill(this.btnX - 2, this.btnY - 2, this.btnX + this.btnW + 2, this.btnY + this.btnH + 2, MenuBackdrop.argb(border, a));
      ctx.fill(this.btnX, this.btnY, this.btnX + this.btnW, this.btnY + this.btnH, MenuBackdrop.argb(this.buttonHovered && ok ? 1712694 : 0, a));
      int lw = this.textRenderer.getWidth(label);
      ctx.drawTextWithShadow(
         this.textRenderer,
         label,
         this.btnX + this.btnW / 2 - lw / 2 + (this.buttonHovered && ok ? 5 : 0),
         this.btnY + 5,
         MenuBackdrop.argb(!ok ? 6710903 : (this.buttonHovered ? 16777024 : 16777215), a)
      );
      if (this.buttonHovered && ok) {
         RenderSystem.enableBlend();
         ctx.drawTexture(HEART, this.btnX + this.btnW / 2 - lw / 2 - 7, this.btnY + 5, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
      }

      if (!ok && this.buttonHovered) {
         int ww = this.textRenderer.getWidth(why);
         ctx.drawTextWithShadow(
            this.textRenderer,
            why,
            Math.max(4, Math.min(this.width - ww - 4, this.btnX + this.btnW / 2 - ww / 2)),
            this.btnY - 12,
            MenuBackdrop.argb(16742246, a)
         );
      }

      String key = label + ok + this.buttonHovered;
      if (this.buttonHovered && !key.equals(this.lastButtonState)) {
         TurnBattleSounds.playHover();
      }

      this.lastButtonState = this.buttonHovered ? key : "";
   }

   private String blockReason(SkillTreeDefs.GlobalNode gn) {
      boolean isUnlocked = this.unlocked().contains(gn.skill());
      if (!isUnlocked) {
         if (!prereqsMetLocal(gn, this.unlocked())) {
            return "Learn the skills before it first.";
         } else {
            BattleSkill req = SkillTreeDefs.resolveSkill(gn);
            if (req != null && req.getLevelRequired() > this.level()) {
               return "Reach LV " + req.getLevelRequired() + " to learn this.";
            } else if (this.skillPoints() < gn.node().cost()) {
               return "Not enough skill points.";
            } else {
               int souls = soulsRequiredFor(gn);
               if (souls > 0 && TurnBattleClientMod.clientAbsorbedSouls.size() < souls) {
                  return "Needs " + souls + " absorbed SOULs.";
               } else {
                  if (SkillTreeDefs.resolveSkill(gn) instanceof SoulMageSkill sms && sms.getSoul() != null && sms.getSoul() != this.soul()) {
                     return "Only a SOUL of " + sms.getSoul().getDisplayName() + " can learn this.";
                  }

                  return null;
               }
            }
         }
      } else {
         boolean isEquipped = !this.unequipped().contains(gn.skill());
         return !isEquipped && this.computeSlotsUsed() + slotCost(gn) > SkillTreeDefs.maxSkillSlots(this.level()) ? "No free skill slots." : null;
      }
   }

   private void renderHeader(DrawContext ctx, int mx, int my) {
      float in = ease(clamp01((this.secs() - 0.1F) / 0.4F));
      int off = (int)((1.0F - in) * -40.0F);
      MatrixStack m = ctx.getMatrices();
      m.push();
      m.translate(0.0F, (float)off, 500.0F);
      ctx.fillGradient(0, 0, this.width, 22, -268171760, -536475114);
      ctx.fill(0, 21, this.width, 22, 1090519039);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("SKILL TREE").setStyle(Style.EMPTY.withBold(true)), 10, 7, -1);
      if (!this.isYou()) {
         int tw = this.textRenderer.getWidth(Text.literal("SKILL TREE").setStyle(Style.EMPTY.withBold(true)));
         int sc = this.soul() != null ? this.soul().getColor() & 16777215 : 12107984;
         ctx.drawTextWithShadow(this.textRenderer, "\u00b7  " + this.subjectName(), 10 + tw + 6, 7, 0xFF000000 | lerpRgb(sc, 16777215, 0.4F));
      }

      String sp = "SP " + this.skillPoints();
      String slots = "SLOTS " + this.computeSlotsUsed() + "/" + SkillTreeDefs.maxSkillSlots(this.level());
      int spx = this.width - 10 - this.textRenderer.getWidth(sp);
      ctx.drawTextWithShadow(this.textRenderer, sp, spx, 7, -8892);
      ctx.drawTextWithShadow(this.textRenderer, slots, spx - 16 - this.textRenderer.getWidth(slots), 7, -8398593);
      if (this.hasSoulTabs()) {
         int tabY = 22;
         int tabW = this.width / HUMAN_SOUL_TABS.length;
         ctx.fill(0, tabY, this.width, tabY + 18, -1073544183);

         for (int i = 0; i < HUMAN_SOUL_TABS.length; i++) {
            SoulType s = HUMAN_SOUL_TABS[i];
            int tx = i * tabW;
            boolean isSel = s == this.selectedSoulTab;
            boolean isCur = s == this.soul();
            boolean hov = this.selectedNode == null && mx >= tx && mx < tx + tabW && my >= tabY + off && my < tabY + 18 + off;
            int accent = s.getColor() & 16777215;
            if (isSel) {
               ctx.fillGradient(tx, tabY, tx + tabW - 1, tabY + 18, MenuBackdrop.argb(accent, 0.05F), MenuBackdrop.argb(accent, 0.3F));
            } else if (hov) {
               ctx.fill(tx, tabY, tx + tabW - 1, tabY + 18, 419430399);
            }

            if (isSel) {
               ctx.fill(tx, tabY + 18 - 2, tx + tabW - 1, tabY + 18, accent | 0xFF000000);
            }

            String label = s.getDisplayName();
            int lw = this.textRenderer.getWidth(label);
            boolean fits = lw + 14 < tabW;
            int lx = tx + tabW / 2 - (fits ? (lw + 11) / 2 : 4);
            RenderSystem.enableBlend();

            try {
               ctx.drawTexture(s.getTexture(), lx, tabY + 5, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
            } catch (Throwable var24) {
            }

            if (fits) {
               ctx.drawTextWithShadow(this.textRenderer, label, lx + 11, tabY + 5, isSel ? -1 : (isCur ? accent | 0xFF000000 : -7564378));
            }
         }
      }

      m.pop();
      float ha = this.keyMode ? in : clamp01(1.0F - (this.secs() - 4.0F) / 1.5F) * in;
      if (ha > 0.01F) {
         String ok = TurnBattleClientMod.keyName(TurnBattleClientMod.battleConfirm);
         String no = TurnBattleClientMod.keyName(TurnBattleClientMod.battleBack);
         String hint = this.keyMode
            ? (
               this.selectedNode == null
                  ? "WASD  move  \u00b7  " + ok + "  open  \u00b7  " + no + "  back" + (this.hasSoulTabs() ? "  \u00b7  Q/E  SOULs" : "")
                  : ok + "  " + (this.unlocked().contains(this.selectedNode.skill()) ? "equip / unequip" : "learn") + "  \u00b7  " + no + "  close"
            )
            : (this.selectedNode == null ? "Drag to look around  \u00b7  Scroll to zoom  \u00b7  Click a light  \u00b7  or WASD" : "");
         if (!hint.isEmpty()) {
            ctx.drawTextWithShadow(
               this.textRenderer, hint, this.width / 2 - this.textRenderer.getWidth(hint) / 2, this.height - 14, MenuBackdrop.argb(9413560, ha)
            );
         }
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (this.selectedNode != null) {
         if (button == 0 && this.computeCurrentAnimT() > 0.7F && this.buttonHovered) {
            this.onActionButton();
            return true;
         } else {
            boolean inPreview = mx >= (double)this.pvX && mx < (double)(this.pvX + this.pvW) && my >= (double)this.pvY && my < (double)(this.pvY + this.pvH);
            boolean inInfo = mx >= (double)this.hitX && mx < (double)(this.hitX + this.hitW) && my >= (double)this.hitY && my < (double)(this.hitY + this.hitH);
            if (button == 1 || !inPreview && !inInfo) {
               this.closeSelected();
            }

            return true;
         }
      } else if (button != 0) {
         return super.mouseClicked(mx, my, button);
      } else {
         if (this.hasSoulTabs()) {
            int tabY = 22;
            int tabW = this.width / HUMAN_SOUL_TABS.length;
            if (my >= (double)tabY && my < (double)(tabY + 18)) {
               int idx = (int)(mx / (double)tabW);
               if (idx >= 0 && idx < HUMAN_SOUL_TABS.length && HUMAN_SOUL_TABS[idx] != this.selectedSoulTab) {
                  TurnBattleSounds.playSelect();
                  this.selectedSoulTab = HUMAN_SOUL_TABS[idx];
                  this.reloadNodes();
                  this.arrivalNanos = System.nanoTime() - 200000000L;
               }

               return true;
            }
         }

         if (my < 22.0) {
            return true;
         } else if (this.hoveredNode != null) {
            this.openNode(this.hoveredNode);
            return true;
         } else {
            this.dragging = true;
            this.dragStartMouseX = mx;
            this.dragStartMouseY = my;
            this.dragStartCameraX = this.camTX;
            this.dragStartCameraY = this.camTY;
            return true;
         }
      }
   }

   private void openNode(SkillTreeDefs.GlobalNode gn) {
      TurnBattleSounds.playSelect();
      this.selectedNode = gn;
      this.wantClosed = false;
      this.savedCamX = this.camTX;
      this.savedCamY = this.camTY;
      this.savedZoom = this.camTZoom;
      this.camTX = this.selectedNode.worldX();
      this.camTY = this.selectedNode.worldY();
      this.camTZoom = Math.max(this.camTZoom, 1.15F);
      this.startPreview(this.selectedNode);
   }

   private void moveKeyNode(int dir) {
      if (this.allNodes != null && !this.allNodes.isEmpty()) {
         if (this.keyNode != null && this.allNodes.contains(this.keyNode)) {
            float fx = this.keyNode.worldX();
            float fy = this.keyNode.worldY();
            SkillTreeDefs.GlobalNode best = null;
            double bestScore = Double.MAX_VALUE;

            for (SkillTreeDefs.GlobalNode gn : this.allNodes) {
               if (gn != this.keyNode) {
                  double dx = (double)(gn.worldX() - fx);
                  double dy = (double)(gn.worldY() - fy);
                  double along = dir == 1 ? dx : (dir == 3 ? -dx : (dir == 2 ? dy : -dy));
                  if (!(along <= 4.0)) {
                     double across = dir != 1 && dir != 3 ? Math.abs(dx) : Math.abs(dy);
                     double score = along + across * 1.6;
                     if (score < bestScore) {
                        bestScore = score;
                        best = gn;
                     }
                  }
               }
            }

            if (best == null) {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 0.5F, 1.4F);
               return;
            }

            this.keyNode = best;
         } else {
            SkillTreeDefs.GlobalNode best = null;
            double bd = Double.MAX_VALUE;

            for (SkillTreeDefs.GlobalNode gnx : this.allNodes) {
               double d = Math.hypot((double)(gnx.worldX() - this.camTX), (double)(gnx.worldY() - this.camTY));
               if (d < bd) {
                  bd = d;
                  best = gnx;
               }
            }

            this.keyNode = best;
         }

         this.camTX = this.keyNode.worldX();
         this.camTY = this.keyNode.worldY();
      }
   }

   private void switchSoulTab(int d) {
      if (this.hasSoulTabs()) {
         int i = 0;

         for (int k = 0; k < HUMAN_SOUL_TABS.length; k++) {
            if (HUMAN_SOUL_TABS[k] == this.selectedSoulTab) {
               i = k;
            }
         }

         i = Math.floorMod(i + d, HUMAN_SOUL_TABS.length);
         TurnBattleSounds.playSelect();
         this.selectedSoulTab = HUMAN_SOUL_TABS[i];
         this.reloadNodes();
         this.keyNode = null;
         this.arrivalNanos = System.nanoTime() - 200000000L;
      }
   }

   private void closeSelected() {
      if (this.selectedNode != null && !this.wantClosed) {
         TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
         this.wantClosed = true;
         this.camTX = this.savedCamX;
         this.camTY = this.savedCamY;
         this.camTZoom = this.savedZoom;
      }
   }

   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.dragging && this.selectedNode == null) {
         this.camTX = this.dragStartCameraX - (float)(mx - this.dragStartMouseX) / this.cameraZoom;
         this.camTY = this.dragStartCameraY - (float)(my - this.dragStartMouseY) / this.cameraZoom;
         return true;
      } else {
         return super.mouseDragged(mx, my, button, dx, dy);
      }
   }

   public boolean mouseReleased(double mx, double my, int button) {
      this.dragging = false;
      return super.mouseReleased(mx, my, button);
   }

   public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
      if (this.selectedNode != null) {
         return true;
      } else {
         float worldX = this.camTX + (float)(mx - (double)this.width / 2.0) / this.camTZoom;
         float worldY = this.camTY + (float)(my - (double)this.height / 2.0 - 6.0) / this.camTZoom;
         float zoomFactor = verticalAmount > 0.0 ? 1.12F : 0.89285713F;
         this.camTZoom = Math.max(0.4F, Math.min(2.5F, this.camTZoom * zoomFactor));
         this.camTX = worldX - (float)(mx - (double)this.width / 2.0) / this.camTZoom;
         this.camTY = worldY - (float)(my - (double)this.height / 2.0 - 6.0) / this.camTZoom;
         return true;
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      boolean back = key == 256 || TurnBattleClientMod.isMenuBack(key);
      boolean pick = TurnBattleClientMod.isMenuConfirm(key);
      if (this.selectedNode != null) {
         if (back) {
            this.closeSelected();
            return true;
         } else if (pick) {
            this.onActionButton();
            return true;
         } else {
            return true;
         }
      } else {
         int dir = switch (key) {
            case 65, 263 -> 3;
            case 68, 262 -> 1;
            case 83, 264 -> 2;
            case 87, 265 -> 0;
            default -> -1;
         };
         if (dir >= 0) {
            this.keyMode = true;
            this.keysMoved = true;
            this.moveKeyNode(dir);
            return true;
         } else if (!pick) {
            if (key == 81 || key == 69) {
               this.switchSoulTab(key == 69 ? 1 : -1);
               return true;
            } else if (TurnBattleClientMod.isMenuBack(key)) {
               TurnBattleSounds.playClient(TurnBattleSounds.MENU_MOVE, 1.0F, 1.0F);
               this.close();
               return true;
            } else {
               return super.keyPressed(key, scan, mods);
            }
         } else {
            this.keyMode = true;
            this.keysMoved = true;
            if (this.keyNode != null && this.allNodes.contains(this.keyNode)) {
               this.openNode(this.keyNode);
            } else {
               this.moveKeyNode(0);
            }

            return true;
         }
      }
   }

   private void onActionButton() {
      if (this.selectedNode != null) {
         if (this.blockReason(this.selectedNode) != null) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_B, 1.0F, 1.0F);
         } else {
            String skill = this.selectedNode.skill();
            boolean isUnlocked = this.unlocked().contains(skill);
            TurnBattleSounds.playSelect();
            if (!isUnlocked) {
               PacketByteBuf buf = PacketByteBufs.create();
               if (this.isYou()) {
                  buf.writeString(skill, 64);
                  Net121.sendToServer(TurnBattleMod.SKILL_UNLOCK, buf);
               } else {
                  buf.writeInt(this.subject);
                  buf.writeString(skill, 64);
                  Net121.sendToServer(TurnBattleMod.RECRUIT_SKILL_UNLOCK, buf);
                  int cost = this.selectedNode.node().cost();
                  Set set = this.unlocked();

                  try {
                     set.add(skill);
                  } catch (UnsupportedOperationException var9) {
                  }

                  if (this.isCustom()) {
                     int[] sp = TurnBattleClientMod.customRecruitSkillPoints;
                     if (sp != null && this.ci() < sp.length) {
                        sp[this.ci()] = Math.max(0, sp[this.ci()] - cost);
                     }
                  } else {
                     int[] sp = TurnBattleClientMod.clientRecruitSkillPoints;
                     if (sp != null && this.subject < sp.length) {
                        sp[this.subject] = Math.max(0, sp[this.subject] - cost);
                     }
                  }

                  if (this.computeSlotsUsed() > SkillTreeDefs.maxSkillSlots(this.level())) {
                     try {
                        this.unequipped().add(skill);
                     } catch (UnsupportedOperationException var8) {
                     }
                  }
               }
            } else {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(this.subject);
               buf.writeString(skill, 64);
               Net121.sendToServer(TurnBattleMod.SKILL_EQUIP_TOGGLE, buf);
               if (!this.isYou()) {
                  Set uneq = this.unequipped();

                  try {
                     if (!uneq.remove(skill)) {
                        uneq.add(skill);
                     }
                  } catch (UnsupportedOperationException var7) {
                  }
               }
            }
         }
      }
   }

   private int computeSlotsUsed() {
      Set unlocked = this.unlocked();
      Set unequipped = this.unequipped();
      List fullSet = this.fullNodeSet();
      int used = 0;

      for (SkillTreeDefs.GlobalNode gn : fullSet) {
         if (unlocked.contains(gn.skill()) && !unequipped.contains(gn.skill())) {
            used += slotCost(gn);
         }
      }

      return used;
   }

   private static int slotCost(SkillTreeDefs.GlobalNode gn) {
      return Math.max(1, (int)Math.ceil((double)gn.node().cost() / 2.0));
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.parent);
      }
   }

   private SkillTreeDefs.GlobalNode findNode(String skillName) {
      return this.nodesBySkill.get(skillName);
   }

   private boolean offscreen(float x, float y, float margin) {
      return x < -margin || x > (float)this.width + margin || y < -margin || y > (float)this.height + margin;
   }

   private static boolean prereqsMetLocal(SkillTreeDefs.GlobalNode gn, Set<String> unlocked) {
      for (String prereq : gn.node().prereqs()) {
         if (!unlocked.contains(prereq)) {
            return false;
         }
      }

      return true;
   }

   private static int soulsRequiredFor(SkillTreeDefs.GlobalNode gn) {
      return SkillTreeDefs.resolveSkill(gn) instanceof MonsterSkill ms ? ms.soulsRequired() : 0;
   }

   private static String pretty(String s) {
      if (s != null && !s.isEmpty()) {
         String t = s.replace('_', ' ').toLowerCase(Locale.ROOT);
         return Character.toUpperCase(t.charAt(0)) + t.substring(1);
      } else {
         return "";
      }
   }

   private static int lerpRgb(int a, int b, float t) {
      int r = (int)((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int g = (int)((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = (int)((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | g << 8 | bl;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }

   private static float clamp(float v, float lo, float hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   private static float ease(float t) {
      float u = 1.0F - clamp01(t);
      return 1.0F - u * u * u;
   }

   private static float backOut(float t) {
      t = clamp01(t);
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow((double)(t - 1.0F), 3.0) + c1 * (float)Math.pow((double)(t - 1.0F), 2.0);
   }

   static {
      for (int i = 0; i < 100; i++) {
         BG_TEX[i] = Identifier.of("turnbattle", "textures/battle_bg/bbs_" + (i + 1) + ".png");
      }

      PREVIEW_KEY = UUID.nameUUIDFromBytes("turnbattle:skill-tree-preview".getBytes());
   }

   private static record Popup(String text, int rgb, int born, boolean onSelf) {
   }
}
