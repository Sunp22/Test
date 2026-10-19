package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class BattleScreen extends Screen {
   static final Identifier ICONS = Identifier.of("minecraft", "textures/gui/icons.png");
   private static final Identifier GRAZE_RING = Identifier.of("turnbattle", "textures/gui/graze_ring.png");
   static final int GRAZE_RING_TICKS = 5;
   private static final int SPRITE_SIZE = 64;
   private static final int DAMAGE_TICKS = 12;
   private static final int SHAKE_AMPLITUDE = 4;
   private static final Map<String, Object[]> MOB_SPRITES = new HashMap<>();
   private final int[] damageTicks = new int[8];
   private final int[] playerDamageTicks = new int[8];
   private String entityTypeKey = "";
   private static final int DMG_NUM_TICKS = 40;
   private static final int MERCY_NUM_RGB = 16768256;
   private float lastRenderScale = 1.0F;
   private static final int BATTLE_BG_FRAMES = 100;
   private static final int BATTLE_BG_FRAME_W = 640;
   private static final int BATTLE_BG_FRAME_H = 480;
   private static final Identifier[] BATTLE_BG_TEX = new Identifier[100];
   private long battleBgStartMs = -1L;
   private static final Identifier DAMAGE_FONT;
   private final Map<Integer, List<BattleScreen.DamageNumber>> damageNumbers = new HashMap<>();
   private final int[] lastSpriteCX = new int[8];
   private final int[] lastSpriteCY = new int[8];
   private final int[] lastSpriteTopY = new int[8];
   private final Map<Integer, List<BattleScreen.DamageNumber>> playerDamageNums = new HashMap<>();
   private final int[] lastParticipantCX = new int[8];
   private final int[] lastParticipantTopY = new int[8];
   private final float[] animColH = new float[8];
   private final float[] animColHPrev = new float[8];
   private int hoveredParticipantIndex = -1;
   static final int BOX_W = 160;
   static final int BOX_H = 120;
   static final int BORDER = 3;
   static final float SOUL_SPEED = 3.6F;
   static final int SOUL_SIZE = 9;
   OffhandGimmick offhandGimmick = OffhandGimmick.NONE;
   boolean shieldRingUsed = false;
   final List<float[]> shieldRings = new ArrayList<>();
   boolean pvpProtectTriggered = false;
   private boolean featherDashAvailable = false;
   int featherDashIframes = 0;
   int featherDashCooldown = 0;
   float featherDashVX = 0.0F;
   float featherDashVY = 0.0F;
   private int effectTick = 0;
   private int effectDuration = 40;
   private SkillType effectSkillType = null;
   private int effectColor = -1;
   private int effectPendingType = 0;
   private int effectPendingParam = 0;
   private int effectPendingTarget = 0;
   private boolean effectIsDamaging = false;
   private boolean effectSoundPlayed = false;
   private final Random effectRng = Random.create();
   BattleScreen.Phase phase = BattleScreen.Phase.TRANSITION;
   private String entityName = "???";
   private float entityHp = 0.0F;
   private float entityMaxHp = 1.0F;
   private int entityNetworkId = -1;
   private float playerHp = 20.0F;
   private float playerMaxHp = 20.0F;
   float playerTp = 0.0F;
   float playerTpMax = 100.0F;
   private int playerLevel = 1;
   private int playerExp = 0;
   private int expNeeded = 20;
   private float shieldReduce = 0.0F;
   private int shieldTurns = 0;
   private int enemySleepTurns = 0;
   private int enemyConfusedTurns = 0;
   private int enemyFreezeTurns = 0;
   private int enemyBurnTurns = 0;
   private int enemyDownTurns = 0;
   private int enemySturdy = 0;
   private boolean rollingHealth = false;
   private BattleStyle battleStyle = BattleStyle.BULLET_HELL;
   private boolean enemySpareable = false;
   private float spareThreshold = 0.0F;
   private final List<BattleScreen.SparePopup> sparePopups = new ArrayList<>();
   private static final int SPARE_NUM_TICKS = 50;
   private static final int CHAT_BUBBLE_TICKS = 100;
   private final Map<UUID, List<BattleScreen.ChatBubble>> chatBubbles = new LinkedHashMap<>();
   private boolean chatInputActive = false;
   private String chatInputText = "";
   private boolean suppressNextChar = false;
   private final Map<UUID, Integer> chatSenderIndex = new HashMap<>();
   private final Map<Integer, String> chatSenderName = new HashMap<>();
   private PlayerClass playerClass = PlayerClass.PSI_USER;
   private boolean isMyTurn = true;
   private PlayerClass realPlayerClass = null;
   String partyMemberLabel = null;
   private float partyMemberTp = 0.0F;
   SoulType playerSoul = null;
   private SoulType recruitSkillSoul = null;
   boolean pvpMode = false;
   private SoulType pvpOpponentSoul = null;
   private static final float ROLL_RATE = 0.5F;
   private float rollingPlayerHp = -1.0F;
   private float rollingPlayerHpPrev = -1.0F;
   private final Map<Integer, Float> rollingParticipantHp = new LinkedHashMap<>();
   private final Map<Integer, Float> rollingParticipantHpPrev = new LinkedHashMap<>();
   private final Map<Integer, Float> lastParticipantTrueHp = new HashMap<>();
   private float displayedEntityHp = -1.0F;
   private float drainEntityHp = -1.0F;
   private final Map<Integer, Float> displayedExtraHp = new LinkedHashMap<>();
   private final Map<Integer, Float> drainExtraHp = new LinkedHashMap<>();
   private static final float ENEMY_FILL_RATE = 1.5F;
   private static final float ENEMY_DRAIN_RATE = 0.4F;
   private float displayedTp = -1.0F;
   private float displayedTpPrev = -1.0F;
   private float drainTp = -1.0F;
   private float drainTpPrev = -1.0F;
   private static final float TP_FILL_RATE = 4.0F;
   private static final float TP_DRAIN_RATE = 1.2F;
   private float speedLineTp = -1.0F;
   private float speedLineTpPrev = -1.0F;
   private static final float TP_SPEEDLINE_RATE = 10.0F;
   final List<BattleScreen.ParticipantInfo> participants = new ArrayList<>();
   final List<UUID> participantUuids = new ArrayList<>();
   private int participantScrollOffset = 0;
   final List<BattleScreen.OpponentInfo> pvpOpponents = new ArrayList<>();
   private final Map<Integer, Float> rollingOpponentHp = new HashMap<>();
   private final List<BattleScreen.EnemyInfo> extraEnemies = new ArrayList<>();
   private BattleScreen.TargetMode targetMode = BattleScreen.TargetMode.NONE;
   private int pendingAction = -1;
   private int pendingParam = 0;
   private int pendingTarget = 0;
   private int pendingGoodsSlot = -1;
   private int hoveredTarget = -1;
   private static final int VESSEL_IDLE = 4;
   private static final int VESSEL_PARRY = 5;
   private static final int VESSEL_BASH = 0;
   private static final int VESSEL_ACT = 1;
   private static final int VESSEL_BLOCK = 2;
   private static final int VESSEL_HURT = 3;
   private static final int[] VESSEL_FRAME_COUNT;
   private static final String[] VESSEL_ANIM_NAME;
   private static final boolean[] VESSEL_LOOPS;
   private final int[] vesselAnimType = new int[8];
   private final int[] vesselAnimTick = new int[8];
   private final int[] vesselHurtTick = new int[8];
   private final boolean[] vesselBlocking = new boolean[8];
   private float vesselAlpha = 255.0F;
   private final int[] vesselHoldTick = new int[8];
   private static final int VESSEL_HOLD_TICKS = 14;
   private static final int VESSEL_PARRY_HOLD_TICKS = 6;
   private SkillEffect activeEffect = null;
   private SkillEffect directHitEffect = null;
   private float directHitDmg = 0.0F;
   private int directHitMyIdx = 0;
   private boolean directHitIsTarget = false;
   private final List<BattleScreen.OtherDirectHit> otherDirectHitEffects = new ArrayList<>();
   private BattleScreen.BashEffect activeBashEffect = null;
   private boolean qteActive = false;
   private boolean qteIsMe = false;
   private boolean qteDone = false;
   private int qteTick = 0;
   private int qteTickPrev = 0;
   float lastRenderDelta = 1.0F;
   private long lastTickRealTimeNanos = 0L;
   private static final int QTE_SWEEP_TICKS = 24;
   private static final int QTE_FADE_TICKS = 8;
   private static final float QTE_CRIT_HALF_WIDTH = 0.02F;
   private static final float QTE_BOX_CENTER = 0.0F;
   private static final float QTE_TRACK_PX = 220.0F;
   private static final float QTE_BOX_HALF = 0.020454545F;
   private static final float QTE_LATE = 0.16363636F;
   private static final float QTE_FRAME = 0.5204545F;
   private float qteCritCenter = 0.5F;
   private int qteSoulColorRgb = 16777215;
   private int qteTargetIndexPending = 0;
   private float qteResultPos = 0.0F;
   private boolean qteResultWasCrit = false;
   private boolean qteRanOut = false;
   private float qteResultMultiplier = 1.0F;
   private int qteResultTpTier = 0;
   private final List<String> messages = new ArrayList<>();
   private final ArrayDeque<String> logQueue = new ArrayDeque<>();
   private String twCurrent = "";
   private int twCharIdx = 0;
   private static final int TW_SPEED = 2;
   private final List<String> knownLogLines = new ArrayList<>();
   private int knownLogCount = 0;
   float enemyDmgPerHit = 2.0F;
   int patternIndex = 0;
   float bulletSpeedMult = 1.0F;
   private int mouseX = 0;
   private int mouseY = 0;
   private String entityPassive = "";
   boolean darknessConstricting = false;
   Element battleElement = Element.NONE;
   private int bossLivesRemaining = 0;
   private int bossMaxLives = 0;
   private static final int TEXTBOX_HEIGHT = 75;
   private static final int TEXTBOX_MARGIN = 60;
   private static final int BUTTON_BAR_HEIGHT = 72;
   private static final float MESSAGE_TEXT_SCALE = 1.4F;
   private static final int DR_COL_W_MAX = 213;
   static final int DR_COL_W = 150;
   private static final int DR_COL_SLIM = 29;
   static final int DR_COL_FULL = 53;
   static final int DR_COL_GAP = 3;
   private int drScrollOffset = 0;
   private int allyScrollOffset = 0;
   int drSpinTick = 0;
   int drSpinOutTick = 0;
   boolean boxShrinkPlaying = false;
   boolean boxRetreatFx = false;
   static final int RETREAT_BOX_TICKS = 10;
   static final int RETREAT_FX_TICKS = 20;
   static final int SPAWN_BOX_TICKS = 10;
   static final int SPAWN_FX_TICKS = 12;
   static final float SOUL_RETURN_TRAVEL = 4.6666665F;
   static final float HEART_BURST_TICKS = 6.6666665F;
   boolean soulReturning = false;
   int soulReturnTick = 0;
   float soulReturnStartX;
   float soulReturnStartY;
   float soulReturnTargetX;
   float soulReturnTargetY;
   boolean soulLeaving = false;
   int soulLeaveTick = 0;
   float soulLeaveStartX;
   float soulLeaveStartY;
   float soulLeaveTargetX;
   float soulLeaveTargetY;
   private int playerGrazeTriggerTick = -1000;
   private static final int[] GRAZE_PULSE_DELAYS;
   private static final float GRAZE_PULSE_SCALE = 0.75F;
   private static final float GRAZE_PULSE_RING_TICKS = 3.3333333F;
   int drSpinTickPrev = 0;
   int drSpinOutTickPrev = 0;
   static final int DR_SPIN_TICKS = 12;
   static final int DR_TEXTBOX_H = 82;
   static final int LW_TEXTBOX_H = 74;
   private int popTick = -1;
   private static final int POP_TICKS = 24;
   private int outroTick = -1;
   private static final int OUTRO_TICKS = 20;
   private static final int DR_BTN_W = 23;
   private static final int DR_BTN_H = 20;
   private static final int DR_BTN_H_SEL = 24;
   private static final String[] DR_BTN_TEX;
   private static final String[] DR_BTN_TEX_SEL;
   private int drNavIdx = 0;
   private boolean mercySubmenuOpen = false;
   private int mercySubmenuIdx = 0;
   private boolean skillsSubmenuOpen = false;
   private int skillsSubmenuIdx = 0;
   int currentBoxWidth = 160;
   int currentBoxHeight = 120;
   int targetBoxWidth = 160;
   int targetBoxHeight = 120;
   private static final float BOX_ANIMATION_SPEED = 28.0F;
   BattleScreen.GimmickType activeGimmick = BattleScreen.GimmickType.NONE;
   final List<Bullet> playerBullets = new ArrayList<>();
   boolean spacePressed = false;
   private int navRow = 0;
   private int navCol = 0;
   private int skillNavIdx = 0;
   private int goodsNavIdx = 0;
   private int actNavRow = 0;
   private int actNavCol = 0;
   private int actTargetIdx = 0;
   private List<ActDefs.ActEntry> actList = new ArrayList<>();
   private int targetNavIdx = 0;
   int iframeTicks = 0;
   float soulX;
   float soulY;
   int grazeRingTick = 0;
   int grazeRingTickPrev = 0;
   int boxX;
   int boxY;
   boolean keyW;
   boolean keyA;
   boolean keyS;
   boolean keyD;
   boolean keySneak;
   final List<Bullet> bullets = new ArrayList<>();
   final Map<UUID, BattleScreen.PeerSoulSample> peerSoulPos = new HashMap<>();
   final List<BattleScreen.KrisSlash> krisSlashes = new ArrayList<>();
   private int globalTick = 0;
   private int transitionTick = 0;
   private String pendingAfterTransition = "player";
   private int deferredTurnStartedAtTick = -1;
   private static final int DEFERRED_TURN_TIMEOUT_TICKS = 60;
   private int resultCountdown = -1;
   private int allOutMyVote = -1;
   private List<BattleScreen.BatonOption> batonOptions = new ArrayList<>();
   private int batonCursor = 0;
   private List<int[]> allOutAnims = new ArrayList<>();
   private int allOutAnimTick = 0;
   private int allOutAnimIdx = 0;
   private boolean allOutAnimating = false;
   private int introTick = 0;
   private final long screenOpenedAtMillis = System.currentTimeMillis();
   private SoundInstance battleMusicInstance = null;
   private float introSoulX = 0.0F;
   private float introSoulY = 0.0F;
   private float introStartX = 0.0F;
   private float introStartY = 0.0F;
   private float introTargetX = 0.0F;
   private float introTargetY = 0.0F;
   private float[] introParticipantX = new float[8];
   private float[] introParticipantY = new float[8];
   private float[] introParticipantXPrev = new float[8];
   private float[] introParticipantYPrev = new float[8];
   private float[] introParticipantStartX = new float[8];
   private float[] introParticipantStartY = new float[8];
   private float[] introParticipantTargetX = new float[8];
   private float[] introParticipantTargetY = new float[8];
   private List<BattleSkill> availableSkills = new ArrayList<>();
   private int skillScrollOffset = 0;
   private boolean draggingSkillBar = false;
   private int dragStartY = 0;
   private int dragStartOffset = 0;
   private List<BattleScreen.FoodEntry> foodItems = new ArrayList<>();
   private static final String[] CARD_LABELS;
   private static final String[] CARD_SUBTEXTS;
   private final boolean[] cardActive = new boolean[]{true, true, true, true, true, true};
   final BulletHellRenderer bulletHell = new BulletHellRenderer(this);
   private boolean quickTransition = false;
   private static int debugFrameCount;
   private static long debugFpsWindowStart;
   private static int debugLastFps;
   private static int debugTickCount;
   private static long debugTpsWindowStart;
   private static int debugLastTps;
   private static final int COL_W_SLIM = 100;
   private static final int COL_H_SLIM = 40;
   private static final int COL_H_FULL = 62;
   private static final int COL_GAP = 10;
   private static final int LW_LINE_Y = 20;
   private static final int LW_LINE_COL = -7701600;
   static final int EB_M = 4;
   static final int EB_TB_OUT = 82;
   static final int EB_CMD_W = 66;
   static final int EB_COL_W = 92;
   static final int EB_COL_GAP = 6;
   static final int EB_COL_H = 44;
   static final int EB_REST_SINK = 8;
   static final int EB_RAISE = 12;
   static final float EB_CLONE_SCALE = 38.0F;
   private static final int EB_FRAME = -987936;
   private static final int EB_OUTLINE = -15724520;
   private static final int[] EB_CMD_ORDER;
   private final float[] ebRaise = new float[8];
   private final float[] ebRaisePrev = new float[8];
   private final float[] odoShown = new float[8];
   private long ebLastNanos;
   private static final String[] EB_FINGER;
   private int lastDrawnNav;
   private float navPopAt;
   private final OverheadBars overheadBars;
   private static final Identifier TP_BAR_BG;
   private static final Identifier TP_BAR_PROG;
   private static final Identifier TP_BAR_WHITE;
   private static final Identifier TP_BAR_BG_DARKNESS;
   private static final Identifier TP_BAR_PROG_DARKNESS;
   private static final int TP_BAR_SRC_W = 55;
   private static final int TP_BAR_SRC_H = 195;
   private static final int TP_BAR_W = 28;
   private static final int TP_BAR_H = 102;
   boolean queueMode;
   private String drText;
   private int drIdx;
   private int drSeq;
   private int drMode;
   private boolean drAcked;
   private int drShownTick;
   boolean drResolving;
   private final Set<UUID> readyKeys;
   private final List<BattleScreen.MbBar> mbBars;
   boolean mbActive;
   private int mbTick;
   private int mbTickPrev;
   private int mbEndTick;
   private static final int MB_HOLD_TICKS = 10;
   private static final int MB_FADE_TICKS = 3;
   private final List<BattleScreen.BashEffect> extraBashEffects;
   private String lastKnownLogLine;
   private static final int INL_COLS = 2;
   private static final int INL_ROWS = 3;
   private static final int INL_ROW_H = 22;
   private static final int INL_PAD = 4;
   private static final int INL_GAP = 2;
   private static final int INL_DESC_H = 10;
   private static final float INL_NAME_SCALE = 1.3F;
   private static final int INL_TP_COLOR = -21965;
   private static final int INL_GD_CARD_H = 22;
   private static final int INL_GD_COLS = 3;
   private static final int SK_PW = 230;
   private static final int SK_PH = 210;
   private static final int SK_CARD_H = 34;
   private static final int SK_LIST_H = 170;
   private static final int SK_BAR_W = 6;
   private static final int SK_DESC_PAD = 10;
   private static final int GD_PW = 230;
   private static final int GD_PH = 210;
   private static final int GD_CARD_H = 28;
   private static final int GD_LIST_H = 170;
   private static final int GD_BAR_W = 6;
   private int goodsScrollOffset;
   private boolean draggingGoodsBar;
   private int goodsDragStartY;
   private int goodsDragStartOffset;
   private String actingRecruitName;
   private static final int SOUL_FADE_TICKS = 6;
   private int bashRequestTick;
   private static final int BASH_REQUEST_TIMEOUT_TICKS = 50;
   float turnLengthMult;
   public String pendingBossLine;
   private final List<BattleScreen.EnemyInfo> enemyRoster;
   private int currentPrimaryEnemyId;
   boolean overhaul;
   private int fxTarget;
   private SkillType fxSkillType;
   private int fxTargetNetId;
   private final List<Entity> ohParty;
   private float ohColX;
   private float ohColY;
   private int ohColFor;
   private float pvpOpponentShield;
   private float pvpOpponentShieldMax;
   private boolean pvpOpponentShieldBroken;
   private float myPvpShield;
   private float myPvpShieldMax;
   private boolean myPvpShieldBroken;
   private int spareThresholdOwnerId;
   private String[] allOutParticipantNames;
   private int[] allOutParticipantSouls;
   private Map<String, Integer> allOutVoteTally;
   private int allOutCursor;
   private static final int[] CARD_ACCENT;
   private static final String[] CARD_ICONS;
   private boolean enemyPhaseSinceGuards;
   private CloneAnimator.Act lastLocalMenuPose;
   private UUID lastLocalMenuPoseKey;
   private int lastSentClonePose;
   private String lastSentPoseRecruit;
   private static final int BASH_FRAMES = 6;
   private static final int BASH_TICKS_FRAME = 2;
   private static final int BASH_TOTAL_TICKS = 12;
   private static final int BASH_DRAW_W = 20;
   private static final int BASH_DRAW_H = 85;

   private float clampedBattleScale() {
      return 1.0F;
   }

   private int getBattleBgFrame() {
      if (this.battleBgStartMs < 0L) {
         this.battleBgStartMs = System.currentTimeMillis();
      }

      long ms = System.currentTimeMillis() - this.battleBgStartMs;
      long totalMs = 3000L;
      return (int)(ms % totalMs / 30L);
   }

   private void renderBattleBg(DrawContext ctx) {
      if (TurnBattleConfig.get().deltarune_battle_bg) {
         Identifier tex = BATTLE_BG_TEX[this.getBattleBgFrame()];
         float srcRatio = 1.3333334F;
         float dstRatio = (float)this.width / (float)this.height;
         int drawW;
         int drawH;
         if (dstRatio > srcRatio) {
            drawW = this.width;
            drawH = (int)((float)this.width / srcRatio);
         } else {
            drawH = this.height;
            drawW = (int)((float)this.height * srcRatio);
         }

         int drawX = (this.width - drawW) / 2;
         int drawY = (this.height - drawH) / 2;
         float a = this.bgAlpha();
         ctx.fill(0, 0, this.width, this.height, fadeArgb(-16777216, a));
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
         ctx.drawTexture(tex, drawX, drawY, drawW, drawH, 0.0F, 0.0F, 640, 480, 640, 480);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private TextRenderer getDmgRenderer() {
      return this.textRenderer;
   }

   private static float easeOutBack(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow((double)(t - 1.0F), 3.0) + c1 * (float)Math.pow((double)(t - 1.0F), 2.0);
   }

   private static int lerpRgbInt(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int r = (int)((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int g = (int)((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = (int)((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | g << 8 | bl;
   }

   private void drawDmgText(DrawContext ctx, String text, int x, int y, int color) {
      Style style = Style.EMPTY.withFont(DAMAGE_FONT);
      Text t = Text.literal(text).setStyle(style);
      int outline = color & 0xFF000000 | 0;
      ctx.drawText(this.textRenderer, Text.literal(text).setStyle(style), x - 1, y, outline, false);
      ctx.drawText(this.textRenderer, Text.literal(text).setStyle(style), x + 1, y, outline, false);
      ctx.drawText(this.textRenderer, Text.literal(text).setStyle(style), x, y - 1, outline, false);
      ctx.drawText(this.textRenderer, Text.literal(text).setStyle(style), x, y + 1, outline, false);
      ctx.drawText(this.textRenderer, t, x, y, color, false);
   }

   float effectiveSoulSpeed() {
      return this.keySneak ? 1.8F : 3.6F;
   }

   public void updateOpponents(List<BattleScreen.OpponentInfo> list) {
      for (int i = 0; i < list.size(); i++) {
         float incoming = list.get(i).hp();
         Float prev = this.rollingOpponentHp.get(i);
         if (prev != null && incoming < prev - 0.5F) {
            SoulType attackerSoul = this.playerSoul;

            for (BattleScreen.ParticipantInfo pip : this.participants) {
               if (pip.isCurrentTurn() && pip.soul() != null) {
                  attackerSoul = pip.soul();
                  break;
               }
            }

            if (i < this.damageTicks.length) {
               this.damageTicks[i] = 12;
            }

            this.spawnDamageNumber(i, prev - incoming, attackerSoul, "");
         }

         this.rollingOpponentHp.put(i, incoming);
      }

      this.rollingOpponentHp.keySet().removeIf(i -> i >= list.size());
      this.pvpOpponents.clear();
      this.pvpOpponents.addAll(list);
      if (this.targetNavIdx >= this.pvpOpponents.size()) {
         this.targetNavIdx = Math.max(0, this.pvpOpponents.size() - 1);
      }
   }

   public void setVisualStyle(String s) {
   }

   public boolean isDeltarune() {
      return true;
   }

   private float drSpinTickSmooth(float partialTick) {
      return (float)this.drSpinTickPrev + (float)(this.drSpinTick - this.drSpinTickPrev) * partialTick;
   }

   private float drSpinOutTickSmooth(float partialTick) {
      return (float)this.drSpinOutTickPrev + (float)(this.drSpinOutTick - this.drSpinOutTickPrev) * partialTick;
   }

   private int drColW(int count) {
      return 150;
   }

   private int findActiveColW() {
      return 150;
   }

   boolean lw() {
      return TurnBattleClientMod.lightWorldUi
         && TurnBattleClientMod.battleUi == BattleUi.DELTARUNE
         && !this.overhaul
         && !DarkWorldSkinsClient.inDarkWorld()
         && !DepthsClient.inRun();
   }

   boolean eb() {
      return TurnBattleClientMod.battleUi == BattleUi.EARTHBOUND && !this.overhaul;
   }

   static boolean lightWorldLayout() {
      if (TurnBattleClientMod.lightWorldUi
         && TurnBattleClientMod.battleUi == BattleUi.DELTARUNE
         && !DarkWorldSkinsClient.inDarkWorld()
         && !DepthsClient.inRun()) {
         if (MinecraftClient.getInstance().currentScreen instanceof BattleScreen b && b.overhaul) {
            return false;
         }

         return true;
      } else {
         return false;
      }
   }

   static boolean earthboundLayout() {
      if (TurnBattleClientMod.battleUi != BattleUi.EARTHBOUND) {
         return false;
      } else {
         if (MinecraftClient.getInstance().currentScreen instanceof BattleScreen b && b.overhaul) {
            return false;
         }

         return true;
      }
   }

   int tbTop() {
      return this.eb() ? 8 : (this.lw() ? this.height - 53 - 74 : this.height - 82);
   }

   int tbH() {
      return this.eb() ? 74 : (this.lw() ? 74 : 82);
   }

   int tbX() {
      return this.eb() ? 78 : 2;
   }

   int tbW() {
      return this.eb() ? this.width - 4 - 4 - this.tbX() : this.width - 4;
   }

   int colsBottom() {
      return !this.eb() && !this.lw() ? this.height - 82 : this.height;
   }

   int arenaTop() {
      return this.eb() ? 90 : 20;
   }

   int arenaBottom() {
      if (this.eb()) {
         return this.height - 44 + 8 - 12 - 4;
      } else {
         return this.lw() ? this.tbTop() : this.height - 82 - 53;
      }
   }

   private void renderBackdrop(DrawContext ctx) {
      if (!(this.bgAlpha() <= 0.003F)) {
         if (this.eb()) {
            EarthboundBackdrop.render(ctx, this.width, this.height, this.entityTypeKey != null ? this.entityTypeKey.hashCode() : 0, this.bgAlpha());
         } else if (this.lw()) {
            this.renderLightWorldBg(ctx);
         } else if (TurnBattleConfig.get().deltarune_battle_bg) {
            this.renderBattleBg(ctx);
         } else {
            ctx.fill(0, 0, this.width, this.height, fadeArgb(-1342177280, this.bgAlpha()));
         }
      }
   }

   private static float c01(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }

   private static float uiEaseOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float u = x - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   private static float uiEaseInBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return c3 * x * x * x - c1 * x * x;
   }

   private float uiIn(float popStart, float popDur, float outStart, float outDur) {
      if (this.overhaul) {
         return 1.0F;
      } else if (this.outroTick >= 0) {
         return 1.0F - uiEaseInBack(c01(((float)this.outroTick + this.lastRenderDelta - outStart) / outDur));
      } else {
         return this.popTick >= 0 ? uiEaseOutBack(c01(((float)this.popTick + this.lastRenderDelta - popStart) / popDur)) : 1.0F;
      }
   }

   private boolean uiMoving() {
      return !this.overhaul && (this.popTick >= 0 || this.outroTick >= 0);
   }

   private float bgAlpha() {
      return !this.overhaul && this.outroTick >= 0 ? 1.0F - c01(((float)this.outroTick + this.lastRenderDelta - 7.0F) / 12.0F) : 1.0F;
   }

   private static int fadeArgb(int argb, float a) {
      int al = Math.round((float)(argb >>> 24 & 0xFF) * c01(a));
      return al << 24 | argb & 16777215;
   }

   private int colAnimDY(int i) {
      if (!this.uiMoving()) {
         return 0;
      } else {
         float in = this.uiIn(7.0F + (float)i * 2.0F, 8.0F, (float)i * 1.5F, 8.0F);
         int span = this.eb() ? 56 : 53;
         return Math.round((1.0F - in) * (float)(span + (this.height - this.colsBottom()) + 8));
      }
   }

   private float[] enemyMotion(int i) {
      float away = 1.0F - this.uiIn((float)i * 2.0F, 8.0F, (float)i * 1.5F, 8.0F);
      if (!this.lw() && !this.eb()) {
         float side = TurnBattleConfig.get().reverseBattleLayout ? -1.0F : 1.0F;
         return new float[]{side * away * (float)this.width * 0.6F, 0.0F, 1.0F};
      } else {
         return new float[]{0.0F, -away * (float)this.height * 0.75F, 1.0F};
      }
   }

   private float[] playerMotion(int i) {
      if (this.outroTick >= 0) {
         float side = TurnBattleConfig.get().reverseBattleLayout ? 1.0F : -1.0F;
         float away = 1.0F - this.uiIn(0.0F, 1.0F, 2.0F + (float)i * 1.5F, 8.0F);
         return new float[]{side * away * (float)this.width * 0.6F, 0.0F, 1.0F};
      } else {
         return new float[]{0.0F, 0.0F, this.uiIn((float)i * 2.0F, 7.0F, 0.0F, 1.0F)};
      }
   }

   private void renderLightWorldBg(DrawContext ctx) {
      float ba = this.bgAlpha();
      ctx.fill(0, 0, this.width, this.height, fadeArgb(-16777216, ba));
      int top = 6;
      int bottom = this.tbTop() - 6;
      int left = 6;
      int right = this.width - 6;
      if (bottom - top >= 24 && right - left >= 24) {
         int CELL = 24;
         int cols = Math.max(1, (right - left) / 24);
         int rows = Math.max(1, (bottom - top) / 24);
         int gw = cols * 24;
         int gh = rows * 24;
         int ox = (this.width - gw) / 2;
         int oy = top + (bottom - top - gh) / 2;
         float t = ((float)this.globalTick + this.lastRenderDelta) * 0.05F;

         for (int r = 0; r <= rows; r++) {
            float glow = 0.5F + 0.5F * (float)Math.sin((double)(t - (float)r * 0.7F));
            int g = 88 + Math.round(glow * 28.0F);
            int col = 0xFF000000 | g / 5 << 16 | g << 8 | g / 3;
            ctx.fill(ox, oy + r * 24, ox + gw + 1, oy + r * 24 + 1, fadeArgb(col, ba));
         }

         for (int c = 0; c <= cols; c++) {
            ctx.fill(ox + c * 24, oy, ox + c * 24 + 1, oy + gh + 1, fadeArgb(-15771100, ba));
         }

         for (int r = 0; r <= rows; r++) {
            for (int c = 0; c <= cols; c++) {
               ctx.fill(ox + c * 24, oy + r * 24, ox + c * 24 + 1, oy + r * 24 + 1, fadeArgb(-13651888, ba));
            }
         }

         int fr = fadeArgb(-14245818, ba);
         ctx.fill(ox - 1, oy - 1, ox + gw + 2, oy, fr);
         ctx.fill(ox - 1, oy + gh + 1, ox + gw + 2, oy + gh + 2, fr);
         ctx.fill(ox - 1, oy - 1, ox, oy + gh + 2, fr);
         ctx.fill(ox + gw + 1, oy - 1, ox + gw + 2, oy + gh + 2, fr);
         ctx.fillGradient(ox, oy + gh * 2 / 3, ox + gw + 1, oy + gh + 1, 0, fadeArgb(-1879048192, ba));
      }
   }

   public void onPeerSoulPos(UUID peer, float relX, float relY) {
      BattleScreen.PeerSoulSample s = this.peerSoulPos.computeIfAbsent(peer, k -> {
         BattleScreen.PeerSoulSample fresh = new BattleScreen.PeerSoulSample();
         fresh.curX = relX;
         fresh.curY = relY;
         return fresh;
      });
      s.prevX = s.curX;
      s.prevY = s.curY;
      s.curX = relX;
      s.curY = relY;
      s.receivedAtNanos = System.nanoTime();
   }

   public BattleScreen() {
      super(Text.empty());
      Arrays.fill(this.odoShown, Float.NaN);
      this.ebLastNanos = 0L;
      this.lastDrawnNav = -1;
      this.navPopAt = -100.0F;
      this.overheadBars = new OverheadBars();
      this.queueMode = false;
      this.drText = "";
      this.drIdx = 0;
      this.drSeq = 0;
      this.drMode = 0;
      this.drAcked = false;
      this.drShownTick = 0;
      this.drResolving = false;
      this.readyKeys = new HashSet<>();
      this.mbBars = new ArrayList<>();
      this.mbActive = false;
      this.mbTick = 0;
      this.mbTickPrev = 0;
      this.mbEndTick = -1;
      this.extraBashEffects = new ArrayList<>();
      this.lastKnownLogLine = "";
      this.goodsScrollOffset = 0;
      this.draggingGoodsBar = false;
      this.goodsDragStartY = 0;
      this.goodsDragStartOffset = 0;
      this.actingRecruitName = null;
      this.bashRequestTick = -1;
      this.turnLengthMult = 1.0F;
      this.pendingBossLine = "";
      this.enemyRoster = new ArrayList<>();
      this.currentPrimaryEnemyId = -1;
      this.fxTarget = -1;
      this.fxTargetNetId = -1;
      this.ohParty = new ArrayList<>();
      this.ohColX = Float.NaN;
      this.ohColY = Float.NaN;
      this.ohColFor = -1;
      this.pvpOpponentShield = 0.0F;
      this.pvpOpponentShieldMax = 0.0F;
      this.pvpOpponentShieldBroken = true;
      this.myPvpShield = 0.0F;
      this.myPvpShieldMax = 0.0F;
      this.myPvpShieldBroken = true;
      this.spareThresholdOwnerId = Integer.MIN_VALUE;
      this.allOutParticipantNames = new String[0];
      this.allOutParticipantSouls = new int[0];
      this.allOutVoteTally = new LinkedHashMap<>();
      this.allOutCursor = 0;
      this.enemyPhaseSinceGuards = false;
      this.lastLocalMenuPose = CloneAnimator.Act.NONE;
      this.lastLocalMenuPoseKey = null;
      this.lastSentClonePose = 0;
      this.lastSentPoseRecruit = "";
   }

   protected void init() {
      int colsBottom = this.arenaBottom();
      int areaTop = this.arenaTop();
      int centerY = (areaTop + colsBottom) / 2 + 5;
      this.boxX = (this.width - 160) / 2;
      this.boxY = centerY - 60;
      this.currentBoxWidth = 160;
      this.currentBoxHeight = 120;
      this.targetBoxWidth = 160;
      this.targetBoxHeight = 120;
      this.updateCardActive();
      Arrays.fill(this.vesselAnimType, 4);
      Arrays.fill(this.vesselAnimTick, 0);
      Arrays.fill(this.vesselHurtTick, 0);
      Arrays.fill(this.vesselBlocking, false);
      Arrays.fill(this.vesselHoldTick, 0);
      this.vesselAlpha = 0.0F;
      if (this.client != null) {
         this.client.options.hudHidden = true;
      }
   }

   public void removed() {
      super.removed();
      BossSpeech.clear();
      if (this.client != null && this.client.getNetworkHandler() != null) {
         Net121.sendToServer(TurnBattleMod.FORCE_END, PacketByteBufs.empty());
      }

      if (this.client != null) {
         this.client.options.hudHidden = false;
      }

      OverhaulView.end();
      TurnBattleSounds.stopBattleMusic(this.battleMusicInstance);
      this.battleMusicInstance = null;
   }

   private ButtonWidget addBtn(int x, int y, int w, int h, String lbl, Runnable fn) {
      return (ButtonWidget)this.addDrawableChild(ButtonWidget.builder(Text.literal(lbl), b -> fn.run()).dimensions(x, y, w, h).build());
   }

   private void updateButtonsForTurn() {
      if (this.participants.size() > 1) {
         boolean enabled = this.isMyTurn && this.phase == BattleScreen.Phase.PLAYER_TURN;
         this.cardActive[0] = enabled;
         this.cardActive[1] = enabled;
         this.cardActive[2] = enabled;
         this.cardActive[3] = enabled;
         this.cardActive[4] = enabled;
         this.cardActive[5] = enabled && this.enemySpareable && !this.pvpMode && !TurnBattleClientMod.isRuthless;
      }
   }

   private void updateCardActive() {
      this.cardActive[5] = this.enemySpareable && !this.pvpMode && !TurnBattleClientMod.isRuthless;
   }

   private void showMainButtons(boolean vis) {
   }

   private void updateMercyButton() {
      this.cardActive[5] = this.enemySpareable && !this.pvpMode && !TurnBattleClientMod.isRuthless;
   }

   private boolean isMercyEligible() {
      return this.enemySpareable || this.spareThreshold >= 1.0F;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   int getWidth() {
      return this.width;
   }

   int getHeight() {
      return this.height;
   }

   TextRenderer getTextRenderer() {
      return this.textRenderer;
   }

   MinecraftClient getClient() {
      return this.client;
   }

   public void tick() {
      this.globalTick++;
      this.tickEarthbound();
      if (this.popTick >= 0 && ++this.popTick > 24) {
         this.popTick = -1;
      }

      this.lastTickRealTimeNanos = System.nanoTime();
      long tickNowMs = System.currentTimeMillis();
      if (debugTpsWindowStart == 0L) {
         debugTpsWindowStart = tickNowMs;
      }

      debugTickCount++;
      if (tickNowMs - debugTpsWindowStart >= 1000L) {
         debugLastTps = debugTickCount;
         debugTickCount = 0;
         debugTpsWindowStart = tickNowMs;
      }

      this.overhaulTick();
      this.tickTypewriter();
      this.tickBattleLine();
      this.tickMultiBash();
      if (!this.extraBashEffects.isEmpty()) {
         for (BattleScreen.BashEffect fx : this.extraBashEffects) {
            fx.tick();
         }

         this.extraBashEffects.removeIf(BattleScreen.BashEffect::isDone);
      }

      this.tickRollingHp();
      this.tickBashQte();
      this.tickCloneMenuPose();
      this.tickDeferredTurnTimeout();
      this.tickBashRequestTimeout();
      this.tickBoxShrink();
      this.tickSoulReturn();
      this.tickSoulLeave();
      this.tickPlayerGrazePulses();
      if (this.directHitEffect != null) {
         this.directHitEffect.tick();
         if (this.directHitEffect.isAtHitFrame() && this.directHitIsTarget && this.directHitDmg > 0.0F) {
            this.spawnPlayerDamageNumber(this.directHitMyIdx, Math.max(1.0F, this.directHitDmg));
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.DAMAGE, 1.0F, 1.0F));
            if (this.directHitMyIdx < this.vesselAnimType.length) {
               this.setVesselAnim(this.directHitMyIdx, 3);
            }

            if (this.anchors3D() && this.client != null && this.client.player != null && this.directHitMyIdx < this.participantUuids.size()) {
               UUID hurtUuid = this.participantUuids.get(this.directHitMyIdx);
               if (hurtUuid == null) {
                  hurtUuid = this.client.player.getUuid();
               }

               BattleEntityRenderer.setAnim(hurtUuid, BattleEntityRenderer.BattleAnim.HURT);
            }
         }

         if (this.directHitEffect.isDone()) {
            this.directHitEffect = null;
            PacketByteBuf ackBuf = PacketByteBufs.create();
            ackBuf.writeFloat(this.directHitIsTarget ? this.directHitDmg : 0.0F);
            Net121.sendToServer(TurnBattleMod.DIRECT_ENEMY_TURN_RESULT, ackBuf);
         }
      }

      if (!this.otherDirectHitEffects.isEmpty()) {
         Iterator it = this.otherDirectHitEffects.iterator();

         while (it.hasNext()) {
            BattleScreen.OtherDirectHit odh = it.next();
            odh.effect().tick();
            if (odh.effect().isAtHitFrame() && this.anchors3D() && odh.participantIdx() >= 0 && odh.participantIdx() < this.participantUuids.size()) {
               UUID hurtUuid = this.participantUuids.get(odh.participantIdx());
               if (hurtUuid != null) {
                  BattleEntityRenderer.setAnim(hurtUuid, BattleEntityRenderer.BattleAnim.HURT);
               }
            }

            if (odh.effect().isDone()) {
               it.remove();
            }
         }
      }

      if (this.currentBoxWidth != this.targetBoxWidth || this.currentBoxHeight != this.targetBoxHeight) {
         if (this.currentBoxWidth < this.targetBoxWidth) {
            this.currentBoxWidth = (int)Math.min((float)this.targetBoxWidth, (float)this.currentBoxWidth + 28.0F);
         } else if (this.currentBoxWidth > this.targetBoxWidth) {
            this.currentBoxWidth = (int)Math.max((float)this.targetBoxWidth, (float)this.currentBoxWidth - 28.0F);
         }

         if (this.currentBoxHeight < this.targetBoxHeight) {
            this.currentBoxHeight = (int)Math.min((float)this.targetBoxHeight, (float)this.currentBoxHeight + 28.0F);
         } else if (this.currentBoxHeight > this.targetBoxHeight) {
            this.currentBoxHeight = (int)Math.max((float)this.targetBoxHeight, (float)this.currentBoxHeight - 28.0F);
         }
      }

      switch (this.phase) {
         case ENEMY_TURN:
            if (this.drSpinTick < 12) {
               this.drSpinTickPrev = this.drSpinTick++;
               if (this.drSpinTick == 10) {
                  this.currentBoxWidth = this.targetBoxWidth;
                  this.currentBoxHeight = this.targetBoxHeight;
               }
            }

            this.bulletHell.tickEnemy();
            break;
         case ENEMY_TURN_END:
            if (!this.boxShrinkPlaying) {
               this.bullets.clear();
               this.playerBullets.clear();
               if (!"wait".equals(this.pendingAfterTransition) && !"enemy".equals(this.pendingAfterTransition)) {
                  this.pendingAfterTransition = "player";
               }

               this.transitionTick = 0;
               this.setPhase(BattleScreen.Phase.TRANSITION);
            }
            break;
         case TRANSITION:
            this.tickTransition();
            break;
         case RESULT:
            boolean fxPlaying = !this.extraBashEffects.isEmpty();
            if (this.activeBashEffect != null) {
               this.activeBashEffect.tick();
               if (this.activeBashEffect.isDone()) {
                  this.activeBashEffect = null;
               } else {
                  fxPlaying = true;
               }
            }

            if (this.activeEffect != null) {
               this.activeEffect.tick();
               if (this.activeEffect.isDone()) {
                  this.activeEffect = null;
               } else {
                  fxPlaying = true;
               }
            }

            if (!fxPlaying) {
               if (this.resultCountdown > 0) {
                  this.resultCountdown--;
               } else if (this.resultCountdown == 0 && !this.overhaul && this.outroTick < 20) {
                  this.popTick = -1;
                  this.outroTick++;
               } else if (this.resultCountdown == 0 && this.client != null) {
                  if (TurnBattleClientMod.pendingDepthsScreen != null) {
                     TurnBattleClientMod.pendingDepthsScreen.encounterActive = false;
                     this.client.setScreen(TurnBattleClientMod.pendingDepthsScreen);
                     TurnBattleClientMod.pendingDepthsScreen = null;
                  } else {
                     this.client.setScreen(null);
                  }
               }
            }
            break;
         case SKILL_EFFECT:
            if (this.activeBashEffect != null) {
               this.activeBashEffect.tick();
               if (this.activeBashEffect.isDone()) {
                  this.activeBashEffect = null;
                  if (this.pendingAction == 0) {
                     this.setPhase(BattleScreen.Phase.TRANSITION);
                     this.pendingAfterTransition = "player";
                     PacketByteBuf qteBuf = PacketByteBufs.create();
                     qteBuf.writeFloat(this.qteResultMultiplier);
                     qteBuf.writeInt(this.qteResultTpTier);
                     Net121.sendToServer(TurnBattleMod.BASH_QTE_RESULT, qteBuf);
                     this.qteResultMultiplier = 1.0F;
                     this.qteResultTpTier = 0;
                  } else if (this.pendingAction > 0) {
                     this.sendAction(this.pendingAction, this.pendingParam, this.pendingTarget);
                  } else {
                     this.transitionTick = 0;
                     this.setPhase(BattleScreen.Phase.TRANSITION);
                  }
               }
            } else if (this.activeEffect != null) {
               this.activeEffect.tick();
               if (this.activeEffect.isDone()) {
                  this.activeEffect = null;
                  if (this.pendingAction >= 0) {
                     this.sendAction(this.pendingAction, this.pendingParam, this.pendingTarget);
                  } else {
                     this.transitionTick = 0;
                     this.setPhase(BattleScreen.Phase.TRANSITION);
                  }
               }
            }
            break;
         case BATTLE_INTRO:
            this.tickBattleIntro();
      }
   }

   private void tickRollingHp() {
      this.grazeRingTickPrev = this.grazeRingTick;
      if (this.grazeRingTick > 0) {
         this.grazeRingTick--;
      }

      for (int i = 0; i < this.damageTicks.length; i++) {
         if (this.damageTicks[i] > 0) {
            this.damageTicks[i]--;
         }
      }

      for (int ix = 0; ix < this.playerDamageTicks.length; ix++) {
         if (this.playerDamageTicks[ix] > 0) {
            this.playerDamageTicks[ix]--;
         }
      }

      this.tickDamageNumbers();
      this.sparePopups.removeIf(p -> this.globalTick - p.born() > 50);
      this.chatBubbles.values().forEach(list -> list.removeIf(b -> this.globalTick - b.born() > 100));
      this.tickVesselAnims();

      for (List list : this.playerDamageNums.values()) {
         list.removeIf(d -> this.globalTick - d.born() >= 40);
      }

      this.rollingPlayerHpPrev = this.rollingPlayerHp;
      if (this.rollingPlayerHp >= 0.0F) {
         if (!this.rollingHealth) {
            this.rollingPlayerHp = this.playerHp;
         } else {
            this.rollingPlayerHp = RollingHp.toward(this.rollingPlayerHp, this.playerHp, this.playerMaxHp);
         }
      }

      for (int ixx = 0; ixx < this.participants.size(); ixx++) {
         float target = this.participants.get(ixx).hp();
         float cur = this.rollingParticipantHp.getOrDefault(ixx, target);
         this.rollingParticipantHpPrev.put(ixx, cur);
         if (!this.rollingHealth) {
            this.rollingParticipantHp.put(ixx, target);
         } else {
            this.rollingParticipantHp.put(ixx, RollingHp.toward(cur, target, this.participants.get(ixx).maxHp()));
         }
      }

      this.rollingParticipantHp.entrySet().removeIf(e -> e.getKey() >= this.participants.size());
      this.rollingParticipantHpPrev.entrySet().removeIf(e -> e.getKey() >= this.participants.size());
      boolean playerTurnNow = this.phase == BattleScreen.Phase.PLAYER_TURN
         || this.phase == BattleScreen.Phase.SKILLS_MENU
         || this.phase == BattleScreen.Phase.GOODS_MENU
         || this.phase == BattleScreen.Phase.ACT_MENU
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY
         || this.phase == BattleScreen.Phase.ALL_OUT
         || this.phase == BattleScreen.Phase.BATON_PASS;

      for (int ixxx = 0; ixxx < this.participants.size() && ixxx < this.animColH.length; ixxx++) {
         boolean isActive = this.participants.get(ixxx).isCurrentTurn() && playerTurnNow;
         float target = isActive ? 53.0F : 29.0F;
         float cur = this.animColH[ixxx] == 0.0F ? 29.0F : this.animColH[ixxx];
         this.animColHPrev[ixxx] = cur;
         float diff = target - cur;
         float step = diff * 0.5F;
         if (Math.abs(step) < 1.0F) {
            step = Math.signum(diff) * Math.min(1.0F, Math.abs(diff));
         }

         this.animColH[ixxx] = Math.abs(diff) <= 0.5F ? target : cur + step;
      }

      if (this.displayedEntityHp < 0.0F) {
         this.displayedEntityHp = this.entityHp;
         this.drainEntityHp = this.entityHp;
      }

      this.displayedEntityHp = this.entityHp;
      if (this.drainEntityHp > this.displayedEntityHp) {
         this.drainEntityHp = Math.max(this.displayedEntityHp, this.drainEntityHp - 0.4F);
      } else {
         this.drainEntityHp = this.displayedEntityHp;
      }

      for (int ixxx = 0; ixxx < this.extraEnemies.size(); ixxx++) {
         float target = this.extraEnemies.get(ixxx).hp();
         float drain = this.drainExtraHp.getOrDefault(ixxx, target);
         if (drain > target) {
            drain = Math.max(target, drain - 0.4F);
         } else {
            drain = target;
         }

         this.displayedExtraHp.put(ixxx, target);
         this.drainExtraHp.put(ixxx, drain);
      }

      this.displayedExtraHp.entrySet().removeIf(e -> e.getKey() >= this.extraEnemies.size());
      this.drainExtraHp.entrySet().removeIf(e -> e.getKey() >= this.extraEnemies.size());
      float trueTp = this.playerTp;
      if (this.displayedTp < 0.0F) {
         this.displayedTp = trueTp;
         this.drainTp = trueTp;
      }

      if (this.speedLineTp < 0.0F) {
         this.speedLineTp = trueTp;
      }

      this.displayedTpPrev = this.displayedTp;
      this.drainTpPrev = this.drainTp;
      this.speedLineTpPrev = this.speedLineTp;
      float dtpDiff = trueTp - this.displayedTp;
      if (Math.abs(dtpDiff) <= 4.0F) {
         this.displayedTp = trueTp;
      } else {
         this.displayedTp = this.displayedTp + Math.signum(dtpDiff) * 4.0F;
      }

      if (this.drainTp > this.displayedTp) {
         this.drainTp = Math.max(this.displayedTp, this.drainTp - 1.2F);
      } else {
         this.drainTp = this.displayedTp;
      }

      float speedDiff = trueTp - this.speedLineTp;
      if (Math.abs(speedDiff) <= 10.0F) {
         this.speedLineTp = trueTp;
      } else {
         this.speedLineTp = this.speedLineTp + Math.signum(speedDiff) * 10.0F;
      }
   }

   private void tickTransition() {
      if ("enemy".equals(this.pendingAfterTransition) && BossSpeech.active()) {
         this.transitionTick = 0;
      } else {
         if (++this.transitionTick >= (this.quickTransition ? 2 : 20)) {
            this.transitionTick = 0;
            this.quickTransition = false;
            if ("enemy".equals(this.pendingAfterTransition)) {
               this.bulletHell.startEnemyPhase();
            } else if (!"wait".equals(this.pendingAfterTransition) && !this.drResolving) {
               for (int vi = 0; vi < this.participants.size(); vi++) {
                  if (this.participants.get(vi).isCurrentTurn()) {
                     this.resetVesselForNewTurn(vi);
                     break;
                  }
               }

               if (this.participants.isEmpty()) {
                  this.resetVesselForNewTurn(0);
               }

               this.setPhase(BattleScreen.Phase.PLAYER_TURN);
            }
         }
      }
   }

   private void tickBattleIntro() {
      this.introTick++;
      int count = Math.max(1, this.participants.isEmpty() ? 1 : this.participants.size());
      System.arraycopy(this.introParticipantX, 0, this.introParticipantXPrev, 0, this.introParticipantX.length);
      System.arraycopy(this.introParticipantY, 0, this.introParticipantYPrev, 0, this.introParticipantY.length);
      if (this.introTick == 10) {
         float gap = 17.0F;
         float totalFlashW = (float)(count * 9) + (float)(count - 1) * 8.0F;
         float flashStartX = (float)this.width / 2.0F - totalFlashW / 2.0F;
         float flashCenterY = (float)this.height / 2.0F - 4.5F;

         for (int i = 0; i < count && i < 8; i++) {
            this.introParticipantStartX[i] = flashStartX + (float)i * 17.0F;
            this.introParticipantStartY[i] = flashCenterY;
            this.introParticipantX[i] = this.introParticipantStartX[i];
            this.introParticipantY[i] = this.introParticipantStartY[i];
            if (this.anchors3D()) {
               int[] clonePos = BattleEntityRenderer.participantPos(i, count, this.width, this.height, 82, true);
               this.introParticipantTargetX[i] = (float)clonePos[0] - 4.5F;
               this.introParticipantTargetY[i] = (float)(clonePos[1] - 40) - 4.5F;
            } else {
               int maxVisible = 3;
               int vis = Math.min(count, 3);
               int totalW = vis * 150 + (vis - 1) * 3;
               int startX2 = (this.width - totalW) / 2;
               int drawIndex = Math.max(0, Math.min(i - this.drScrollOffset, 2));
               int colX = startX2 + drawIndex * 153;
               int colY = this.colsBottom() - 29;
               this.introParticipantTargetX[i] = (float)colX + 75.0F - 4.5F;
               this.introParticipantTargetY[i] = (float)colY + 14.5F - 4.5F;
            }
         }

         int myIdx = 0;

         for (int ix = 0; ix < this.participants.size(); ix++) {
            if (this.participants.get(ix).isMe()) {
               myIdx = ix;
               break;
            }
         }

         this.introStartX = this.introParticipantStartX[myIdx];
         this.introStartY = this.introParticipantStartY[myIdx];
         this.introTargetX = this.introParticipantTargetX[myIdx];
         this.introTargetY = this.introParticipantTargetY[myIdx];
      }

      if (this.overhaul) {
         if (this.introTick >= 26 && !OverhaulView.isActive()) {
            OverhaulView.begin();
            OverhaulView.startIntro();
         }

         if (this.introTick >= 10) {
            this.overhaulIntroTargets();
         }
      }

      if (this.introTick >= 24 && this.introTick <= 36) {
         float t = (float)(this.introTick - 24) / 12.0F;
         t = t * t * (3.0F - 2.0F * t);

         for (int ixx = 0; ixx < count && ixx < 8; ixx++) {
            this.introParticipantX[ixx] = this.introParticipantStartX[ixx] + (this.introParticipantTargetX[ixx] - this.introParticipantStartX[ixx]) * t;
            this.introParticipantY[ixx] = this.introParticipantStartY[ixx] + (this.introParticipantTargetY[ixx] - this.introParticipantStartY[ixx]) * t;
         }

         int myIdx = 0;

         for (int ixx = 0; ixx < this.participants.size(); ixx++) {
            if (this.participants.get(ixx).isMe()) {
               myIdx = ixx;
               break;
            }
         }

         this.introSoulX = this.introParticipantX[myIdx];
         this.introSoulY = this.introParticipantY[myIdx];
      }

      if (this.introTick >= 46) {
         if (!this.overhaul) {
            this.popTick = 0;
         }

         this.transitionTick = 0;
         this.setPhase(BattleScreen.Phase.TRANSITION);
         this.battleMusicInstance = TurnBattleSounds.startBattleMusic(this.playerClass);
      }
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      if (this.lastTickRealTimeNanos == 0L) {
         this.lastTickRealTimeNanos = System.nanoTime();
      }

      float secondsSinceLastTick = (float)(System.nanoTime() - this.lastTickRealTimeNanos) / 1.0E9F;
      this.lastRenderDelta = Math.min(1.0F, Math.max(0.0F, secondsSinceLastTick / 0.05F));
      long nowMs = System.currentTimeMillis();
      if (debugFpsWindowStart == 0L) {
         debugFpsWindowStart = nowMs;
      }

      debugFrameCount++;
      if (nowMs - debugFpsWindowStart >= 1000L) {
         debugLastFps = debugFrameCount;
         debugFrameCount = 0;
         debugFpsWindowStart = nowMs;
      }

      if (this.phase == BattleScreen.Phase.BATTLE_INTRO) {
         if (!this.overhaul && this.introTick >= 23) {
            BattleEntityRenderer.renderBattle3D(ctx, this, this.width, this.height, 82, this.lastRenderDelta);
            MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
            RenderSystem.clear(256, false);
         }

         this.renderBattleIntro(ctx);
      } else if (this.overhaul) {
         this.renderOverhaul(ctx, mx, my);
      } else {
         this.lastRenderScale = this.clampedBattleScale();
         if (this.lastRenderScale < 1.0F) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate((float)this.width / 2.0F, (float)this.height / 2.0F, 0.0F);
            ctx.getMatrices().scale(this.lastRenderScale, this.lastRenderScale, 1.0F);
            ctx.getMatrices().translate((float)(-this.width) / 2.0F, (float)(-this.height) / 2.0F, 0.0F);
            mx = (int)(((float)mx - (float)this.width / 2.0F) / this.lastRenderScale + (float)this.width / 2.0F);
            my = (int)(((float)my - (float)this.height / 2.0F) / this.lastRenderScale + (float)this.height / 2.0F);
         }

         this.renderBackdrop(ctx);
         if (!this.overhaul) {
            if (this.uiMoving()) {
               BattleEntityRenderer.playerMotion = this::playerMotion;
               BattleEntityRenderer.enemyMotion = this::enemyMotion;
            }

            if (this.eb()) {
               BattleEntityRenderer.ebAnchor = this::ebCloneAnchor;
            }

            try {
               BattleEntityRenderer.renderBattle3D(ctx, this, this.width, this.height, 82, this.lastRenderDelta);
            } finally {
               BattleEntityRenderer.playerMotion = null;
               BattleEntityRenderer.enemyMotion = null;
               BattleEntityRenderer.ebAnchor = null;
            }

            MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
            RenderSystem.clear(256, false);
         }

         this.renderEntityBar(ctx);
         if (this.directHitEffect != null) {
            this.directHitEffect.render(ctx, this.lastRenderDelta);
         }

         for (BattleScreen.OtherDirectHit odh : this.otherDirectHitEffects) {
            odh.effect().render(ctx, this.lastRenderDelta);
         }

         this.renderDamageNumbers(ctx, this.lastRenderDelta);
         this.renderTpMeter(ctx);
         this.renderRogueTimer(ctx);
         this.renderDarkFountainEncounterCard(ctx);
         switch (this.phase) {
            case PLAYER_TURN:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               break;
            case SKILLS_MENU:
            case GOODS_MENU:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               break;
            case ACT_MENU:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               this.renderActMenu(ctx);
               break;
            case ENEMY_TURN:
               this.renderAnimatedBattleBox(ctx, this.lastRenderDelta);
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               this.bulletHell.renderEnemyPhase(ctx, this.lastRenderDelta);
               break;
            case ENEMY_TURN_END:
               this.renderAnimatedBattleBox(ctx, this.lastRenderDelta);
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               break;
            case TRANSITION:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               if (this.transitionTick < 15 && !this.drResolving) {
                  ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u25b6"), this.width / 2, this.height / 2 - 10, -1);
               }
               break;
            case RESULT:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               if (this.activeBashEffect != null) {
                  this.activeBashEffect.render(ctx, this.lastRenderDelta);
               }

               if (this.activeEffect != null) {
                  this.activeEffect.render(ctx, this.lastRenderDelta);
               }
               break;
            case SKILL_EFFECT:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               if (this.activeBashEffect != null) {
                  this.activeBashEffect.render(ctx, this.lastRenderDelta);
               }

               if (this.activeEffect != null) {
                  this.activeEffect.render(ctx, this.lastRenderDelta);
               }
               break;
            case BATTLE_INTRO:
               this.renderBattleIntro(ctx);
               break;
            case TARGET_SELECT_ENEMY:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               if (this.pvpMode) {
                  this.renderTargetSelectEnemyPvp(ctx, mx, my);
               } else {
                  this.renderTargetSelectEnemy(ctx, mx, my);
               }
               break;
            case TARGET_SELECT_ALLY:
               this.renderTextBox(ctx);
               this.drawParticipantPanel(ctx);
               super.render(ctx, mx, my, this.lastRenderDelta);
               this.renderTargetSelectAlly(ctx, mx, my);
               break;
            case ALL_OUT:
               this.renderAllOutPrompt(ctx, mx, my);
               break;
            case BATON_PASS:
               this.renderBatonPassPrompt(ctx, mx, my);
         }

         for (BattleScreen.BashEffect fx : this.extraBashEffects) {
            fx.render(ctx, this.lastRenderDelta);
         }

         if ((this.boxShrinkPlaying || this.boxRetreatFx) && this.phase != BattleScreen.Phase.ENEMY_TURN && this.phase != BattleScreen.Phase.ENEMY_TURN_END) {
            this.renderAnimatedBattleBox(ctx, this.lastRenderDelta);
         }

         this.renderSoulReturn(ctx, this.lastRenderDelta);
         this.renderSoulLeave(ctx, this.lastRenderDelta);
         this.renderPlayerGrazePulses(ctx);
         BossSpeech.render(ctx, this.textRenderer, this.lastSpriteCX[0], this.lastSpriteTopY[0] + 24, this.width, this.height);
         if (TurnBattleClientMod.showBattleDebug) {
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal("FPS: " + debugLastFps + "  TPS: " + debugLastTps).formatted(Formatting.YELLOW), this.width - 140, 4, -256
            );
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal(String.format("delta: %.3f", delta)).formatted(Formatting.AQUA), this.width - 140, 14, -12255233
            );
         }

         if (this.phase != BattleScreen.Phase.ENEMY_TURN
            && this.phase != BattleScreen.Phase.ENEMY_TURN_END
            && this.phase != BattleScreen.Phase.BATTLE_INTRO
            && this.phase != BattleScreen.Phase.RESULT) {
            this.renderChatBubbles(ctx);
            if (this.chatInputActive) {
               this.renderChatInput(ctx);
            }
         }

         if (this.phase != BattleScreen.Phase.BATTLE_INTRO
            && this.phase != BattleScreen.Phase.RESULT
            && this.phase != BattleScreen.Phase.ALL_OUT
            && this.phase != BattleScreen.Phase.BATON_PASS
            && !this.mercySubmenuOpen) {
            this.renderVesselSprites(ctx, (int)this.vesselAlpha);
         }

         this.renderPlayerDamageNumbers(ctx, this.lastRenderDelta);
         if (this.lastRenderScale < 1.0F) {
            ctx.getMatrices().pop();
         }

         boolean inBulletHellNow = this.phase == BattleScreen.Phase.ENEMY_TURN;
         float trackSoulY;
         float trackSoulX;
         if (inBulletHellNow) {
            trackSoulX = this.soulX + 4.5F;
            trackSoulY = this.soulY + 4.5F;
         } else if (this.lastParticipantCX.length > 0 && this.lastParticipantCX[0] > 0) {
            trackSoulX = (float)this.lastParticipantCX[0];
            trackSoulY = (float)this.lastParticipantTopY[0];
         } else {
            trackSoulX = (float)this.width / 2.0F;
            trackSoulY = (float)this.height / 2.0F;
         }

         DeathSoulTracker.update(inBulletHellNow, trackSoulX, trackSoulY);
      }
   }

   private int enemyTargetBarW() {
      int total = 1 + this.extraEnemies.size();
      int spacing = 8;
      int barW = 180;
      if (total > 1) {
         int tW = total * barW + (total - 1) * spacing;
         if (tW > this.width - 20) {
            barW = (this.width - 20 - (total - 1) * spacing) / total;
         }
      }

      return barW;
   }

   private int enemyTargetBtnY() {
      return this.height / 2 - 8;
   }

   private List<Integer> livingEnemyIndices() {
      List out = new ArrayList<>();
      float primHp = this.displayedEntityHp >= 0.0F ? this.displayedEntityHp : this.entityHp;
      if (primHp > 0.0F) {
         out.add(0);
      }

      for (int i = 0; i < this.extraEnemies.size(); i++) {
         float hp = this.displayedExtraHp.getOrDefault(i, this.extraEnemies.get(i).hp());
         if (hp > 0.0F) {
            out.add(i + 1);
         }
      }

      return out;
   }

   private void renderTargetSelectEnemy(DrawContext ctx, int mx, int my) {
      int tbY = this.tbTop();
      int tbH = this.tbH();
      int tbX = this.tbX();
      int tbW = this.tbW();
      List aliveIdx = this.livingEnemyIndices();
      int MAX_ROWS = 3;
      int PAD = 4;
      int ROW_H = (tbH - 8) / 3;
      int BAR_H = 5;
      int BAR_W = 90;
      int GAP = 8;
      int leftW = tbW * 40 / 100;
      int rightX = tbX + leftW;
      int nameX = tbX + 71;

      for (int row = 0; row < aliveIdx.size(); row++) {
         int i = aliveIdx.get(row);
         String name;
         float hp;
         float maxHp;
         float spareThr;
         int sleepT;
         int freezeT;
         int burnT;
         int confT;
         if (i != 0) {
            BattleScreen.EnemyInfo e = this.extraEnemies.get(i - 1);
            name = e.name();
            hp = this.displayedExtraHp.getOrDefault(i - 1, e.hp());
            maxHp = e.maxHp();
            spareThr = e.spareThreshold();
            sleepT = e.sleepTurns();
            freezeT = e.freezeTurns();
            burnT = e.burnTurns();
            confT = e.confusedTurns();
         } else {
            name = this.entityName;
            hp = this.displayedEntityHp >= 0.0F ? this.displayedEntityHp : this.entityHp;
            maxHp = this.entityMaxHp;
            spareThr = this.spareThreshold;
            if (!this.pvpMode) {
               sleepT = this.enemySleepTurns;
               freezeT = this.enemyFreezeTurns;
               burnT = this.enemyBurnTurns;
               confT = this.enemyConfusedTurns;
            } else {
               BattleScreen.ParticipantInfo opp = null;

               for (BattleScreen.ParticipantInfo pi : this.participants) {
                  if (!pi.isMe()) {
                     opp = pi;
                     break;
                  }
               }

               sleepT = opp != null && opp.isAsleep() ? 1 : 0;
               freezeT = opp != null && opp.isFrozen() ? 1 : 0;
               burnT = opp != null && opp.isBurned() ? 1 : 0;
               confT = opp != null && opp.isConfused() ? 1 : 0;
            }
         }

         boolean selected = i == this.targetNavIdx;
         float hpFrac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, hp / maxHp)) : 0.0F;
         float mercyFrac = Math.min(1.0F, spareThr);
         boolean spareReady = spareThr >= 1.0F;
         boolean asleep = sleepT > 0;
         int rowY = tbY + 4 + row * ROW_H;
         int nameY = rowY + (ROW_H - 9) / 2 - 4;
         int barY = nameY + 9 + 2;
         int nameColor = asleep ? -12276993 : (spareReady ? -8960 : -1);
         Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
         if (selected) {
            int soulDrawX = tbX + 57;
            int soulDrawY = nameY + (9 - 9) / 2;
            if (sTex != null) {
               ctx.drawTexture(sTex, soulDrawX, soulDrawY, 0.0F, 0.0F, 9, 9, 9, 9);
            } else {
               ctx.drawTexture(ICONS, soulDrawX, soulDrawY, 52, 0, 9, 9);
            }
         }

         String dispName = name;
         int maxNameW = leftW - (nameX - tbX) - 4;

         while (this.textRenderer.getWidth(dispName) > maxNameW && dispName.length() > 1) {
            dispName = dispName.substring(0, dispName.length() - 1);
         }

         ctx.drawTextWithShadow(this.textRenderer, Text.literal(dispName), nameX, nameY, nameColor);
         String st = sleepT > 0 ? " (Tired)" : (freezeT > 0 ? " (Frozen)" : (burnT > 0 ? " (Burnt)" : (confT > 0 ? " (Confused)" : "")));
         if (!st.isEmpty()) {
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal(st).formatted(Formatting.GRAY), nameX + this.textRenderer.getWidth(dispName) + 2, nameY, -5592406
            );
         }

         int barsW = tbW - leftW - 4;
         int fullBarW = (barsW - 8) / 2;
         int eachBarW = fullBarW / 2;
         int mercyBarX = rightX + fullBarW + 8;
         int hpBarX = mercyBarX - 8 - eachBarW;
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("HP"), hpBarX, nameY, -1);
         ctx.fill(hpBarX, barY, hpBarX + eachBarW, barY + 5, -15658735);
         int hpFill = (int)((float)eachBarW * hpFrac);
         int hpCol;
         if (spareReady) {
            hpCol = -12255420;
         } else if (hpFrac > 0.5F) {
            hpCol = -14501052;
         } else if (hpFrac > 0.25F) {
            hpCol = -8960;
         } else {
            hpCol = -56798;
         }

         if (hpFill > 0) {
            ctx.fill(hpBarX, barY, hpBarX + hpFill, barY + 5, hpCol);
            ctx.fill(hpBarX, barY, hpBarX + hpFill, barY + 1, lerpColor(hpCol, -1, 0.3F));
         }

         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal((int)(hpFrac * 100.0F) + "%").formatted(Formatting.WHITE),
            hpBarX + eachBarW / 2,
            barY + 5 + 1,
            selected ? -188 : -1
         );
         if (!this.pvpMode || !(this.pvpOpponentShieldMax <= 0.0F)) {
            if (this.pvpMode) {
               float shieldFrac = this.pvpOpponentShieldMax > 0.0F ? Math.max(0.0F, Math.min(1.0F, this.pvpOpponentShield / this.pvpOpponentShieldMax)) : 0.0F;
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("SHIELD").formatted(Formatting.AQUA), mercyBarX, nameY, -12263937);
               ctx.fill(mercyBarX, barY, mercyBarX + eachBarW, barY + 5, -15658735);
               int sFill = (int)((float)eachBarW * shieldFrac);
               int sCol = this.pvpOpponentShieldBroken ? -10066330 : -12263937;
               if (sFill > 0) {
                  ctx.fill(mercyBarX, barY, mercyBarX + sFill, barY + 5, sCol);
                  ctx.fill(mercyBarX, barY, mercyBarX + sFill, barY + 1, -3344641);
               }

               ctx.drawCenteredTextWithShadow(
                  this.textRenderer,
                  Text.literal(this.pvpOpponentShieldBroken ? "BROKEN" : String.valueOf((int)this.pvpOpponentShield))
                     .formatted(this.pvpOpponentShieldBroken ? Formatting.DARK_GRAY : Formatting.AQUA),
                  mercyBarX + eachBarW / 2,
                  barY + 5 + 1,
                  this.pvpOpponentShieldBroken ? -10066330 : -12263937
               );
            } else {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("MERCY").formatted(Formatting.GOLD), mercyBarX, nameY, -8960);
               ctx.fill(mercyBarX, barY, mercyBarX + eachBarW, barY + 5, -15658735);
               int mFill = (int)((float)eachBarW * mercyFrac);
               int mCol = spareReady ? -12255420 : -13312;
               if (mFill > 0) {
                  ctx.fill(mercyBarX, barY, mercyBarX + mFill, barY + 5, mCol);
                  ctx.fill(mercyBarX, barY, mercyBarX + mFill, barY + 1, -86);
               }

               ctx.drawCenteredTextWithShadow(
                  this.textRenderer,
                  Text.literal((int)(mercyFrac * 100.0F) + "%").formatted(spareReady ? Formatting.GREEN : Formatting.GOLD),
                  mercyBarX + eachBarW / 2,
                  barY + 5 + 1,
                  spareReady ? -12255420 : -8892
               );
            }
         }
      }
   }

   private void renderTargetSelectEnemyPvp(DrawContext ctx, int mx, int my) {
      if (this.pvpOpponents.isEmpty()) {
         this.sendAction(this.pendingAction, this.pendingParam, 0);
      } else {
         int tbY = this.tbTop();
         int tbH = this.tbH();
         int tbX = this.tbX();
         int tbW = this.tbW();
         int MAX_ROWS = 3;
         int PAD = 4;
         int ROW_H = (tbH - 8) / 3;
         int BAR_H = 5;
         int leftW = tbW * 40 / 100;
         int rightX = tbX + leftW;
         int nameX = tbX + 71;
         int total = Math.min(this.pvpOpponents.size(), 3);

         for (int row = 0; row < total; row++) {
            BattleScreen.OpponentInfo o = this.pvpOpponents.get(row);
            String name = o.name();
            float maxHp = o.maxHp();
            float hp = o.hp();
            boolean downed = o.downed();
            boolean selected = row == this.targetNavIdx;
            int rowY = tbY + 4 + row * ROW_H;
            int nameY = rowY + (ROW_H - 9) / 2 - 4;
            int barY = nameY + 9 + 2;
            int nameColor = downed ? -7829368 : -1;
            Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
            if (selected) {
               int soulDrawX = tbX + 57;
               int soulDrawY = nameY + (9 - 9) / 2;
               if (sTex != null) {
                  ctx.drawTexture(sTex, soulDrawX, soulDrawY, 0.0F, 0.0F, 9, 9, 9, 9);
               } else {
                  ctx.drawTexture(ICONS, soulDrawX, soulDrawY, 52, 0, 9, 9);
               }
            }

            String dispName = name;
            int maxNameW = leftW - (nameX - tbX) - 4;

            while (this.textRenderer.getWidth(dispName) > maxNameW && dispName.length() > 1) {
               dispName = dispName.substring(0, dispName.length() - 1);
            }

            ctx.drawTextWithShadow(this.textRenderer, Text.literal(dispName), nameX, nameY, nameColor);
            if (downed) {
               ctx.drawTextWithShadow(
                  this.textRenderer, Text.literal(" (Downed)").formatted(Formatting.GRAY), nameX + this.textRenderer.getWidth(dispName) + 2, nameY, -5592406
               );
            }

            int hpBarW = tbW - leftW - 4;
            float hpFrac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, hp / maxHp)) : 0.0F;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("HP"), rightX, nameY, -1);
            ctx.fill(rightX, barY, rightX + hpBarW, barY + 5, -15658735);
            int hpFill = (int)((float)hpBarW * hpFrac);
            int hpCol;
            if (downed) {
               hpCol = -10066330;
            } else if (hpFrac > 0.5F) {
               hpCol = -14501052;
            } else if (hpFrac > 0.25F) {
               hpCol = -8960;
            } else {
               hpCol = -56798;
            }

            if (hpFill > 0) {
               ctx.fill(rightX, barY, rightX + hpFill, barY + 5, hpCol);
               ctx.fill(rightX, barY, rightX + hpFill, barY + 1, lerpColor(hpCol, -1, 0.3F));
            }

            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal((int)(hpFrac * 100.0F) + "%").formatted(Formatting.WHITE),
               rightX + hpBarW / 2,
               barY + 5 + 1,
               selected ? -188 : -1
            );
         }
      }
   }

   private void renderTargetSelectAlly(DrawContext ctx, int mx, int my) {
      if (this.participants.isEmpty()) {
         this.sendAction(this.pendingAction, this.pendingParam, 0);
      } else {
         int tbY = this.tbTop();
         int tbH = this.tbH();
         int tbX = this.tbX();
         int tbW = this.tbW();
         int MAX_ROWS = 3;
         int PAD = 4;
         int ROW_H = (tbH - 8) / 3;
         int BAR_H = 5;
         int leftW = tbW * 40 / 100;
         int rightX = tbX + leftW;
         int nameX = tbX + 71;
         int total = this.participants.size();
         int maxAllyScroll = Math.max(0, total - 3);
         this.allyScrollOffset = Math.max(0, Math.min(this.allyScrollOffset, maxAllyScroll));
         this.hoveredTarget = -1;

         for (int i = this.allyScrollOffset; i < Math.min(total, this.allyScrollOffset + 3); i++) {
            int row = i - this.allyScrollOffset;
            BattleScreen.ParticipantInfo p = this.participants.get(i);
            String name = p.name();
            float maxHp = p.maxHp();
            float hp = p.isMe() ? (this.rollingPlayerHp >= 0.0F ? this.rollingPlayerHpSmooth() : this.playerHp) : this.rollingParticipantHpSmooth(i, p.hp());
            boolean downed = hp <= 0.0F;
            boolean selected = i == this.targetNavIdx;
            int rowY = tbY + 4 + row * ROW_H;
            if (mx >= tbX && mx < tbX + tbW && my >= rowY && my < rowY + ROW_H) {
               this.hoveredTarget = i + 10;
            }

            int nameY = rowY + (ROW_H - 9) / 2 - 4;
            int barY = nameY + 9 + 2;
            int nameColor = downed ? -7829368 : -1;
            Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
            if (selected) {
               int soulDrawX = tbX + 57;
               int soulDrawY = nameY + (9 - 9) / 2;
               if (sTex != null) {
                  ctx.drawTexture(sTex, soulDrawX, soulDrawY, 0.0F, 0.0F, 9, 9, 9, 9);
               } else {
                  ctx.drawTexture(ICONS, soulDrawX, soulDrawY, 52, 0, 9, 9);
               }
            }

            String dispName = name;
            int maxNameW = leftW - (nameX - tbX) - 4;

            while (this.textRenderer.getWidth(dispName) > maxNameW && dispName.length() > 1) {
               dispName = dispName.substring(0, dispName.length() - 1);
            }

            ctx.drawTextWithShadow(this.textRenderer, Text.literal(dispName), nameX, nameY, nameColor);
            if (downed) {
               ctx.drawTextWithShadow(
                  this.textRenderer, Text.literal(" (Downed)").formatted(Formatting.GRAY), nameX + this.textRenderer.getWidth(dispName) + 2, nameY, -5592406
               );
            }

            int hpBarW = tbW - leftW - 4;
            float hpFrac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, hp / maxHp)) : 0.0F;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("HP"), rightX, nameY, -1);
            ctx.fill(rightX, barY, rightX + hpBarW, barY + 5, -15658735);
            int hpFill = (int)((float)hpBarW * hpFrac);
            int hpCol;
            if (downed) {
               hpCol = -10066330;
            } else if (hpFrac > 0.5F) {
               hpCol = -14501052;
            } else if (hpFrac > 0.25F) {
               hpCol = -8960;
            } else {
               hpCol = -56798;
            }

            if (hpFill > 0) {
               ctx.fill(rightX, barY, rightX + hpFill, barY + 5, hpCol);
               ctx.fill(rightX, barY, rightX + hpFill, barY + 1, lerpColor(hpCol, -1, 0.3F));
            }

            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal((int)(hpFrac * 100.0F) + "%").formatted(Formatting.WHITE),
               rightX + hpBarW / 2,
               barY + 5 + 1,
               selected ? -188 : -1
            );
         }
      }
   }

   private void drawPassiveTooltipIfHovered(DrawContext ctx) {
      int nameW = this.textRenderer.getWidth(this.entityName);
      int nx = this.width / 2 - nameW / 2;
      int ny = 2;
      int hoverPad = 10;
      if (this.mouseX >= nx - hoverPad && this.mouseX <= nx + nameW + hoverPad + 14 && this.mouseY >= ny - 4 && this.mouseY <= ny + 14) {
         List lines = new ArrayList<>();
         String[] words = this.entityPassive.split(" ");
         StringBuilder line = new StringBuilder();

         for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (this.textRenderer.getWidth(test) > 200) {
               if (!line.isEmpty()) {
                  lines.add(line.toString());
               }

               line = new StringBuilder(word);
            } else {
               line = new StringBuilder(test);
            }
         }

         if (!line.isEmpty()) {
            lines.add(line.toString());
         }

         if (!lines.isEmpty()) {
            int lineH = 9 + 2;
            int tipW = lines.stream().mapToInt(this.textRenderer::getWidth).max().orElse(100) + 8;
            int tipH = lines.size() * lineH + 4;
            int tipX = Math.min(this.mouseX + 6, this.width - tipW - 4);
            int tipY = Math.max(this.mouseY - tipH - 4, 4);
            ctx.fill(tipX - 1, tipY - 1, tipX + tipW + 1, tipY + tipH + 1, -12307712);
            ctx.fill(tipX, tipY, tipX + tipW, tipY + tipH, -299757312);

            for (int i = 0; i < lines.size(); i++) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(lines.get(i)).formatted(Formatting.GOLD), tipX + 4, tipY + 2 + i * lineH, -22016);
            }
         }
      }
   }

   private void renderBattleIntro(DrawContext ctx) {
      if (this.pvpMode) {
         this.renderPvpIntro(ctx);
      } else {
         boolean flashOn = this.introTick >= 2 && this.introTick <= 3
            || this.introTick >= 6 && this.introTick <= 7
            || this.introTick >= 10 && this.introTick <= 23;
         boolean inSlide = this.introTick >= 24 && this.introTick <= 35;
         boolean inFade = this.introTick >= 36 && this.introTick <= 45;
         boolean needsBlack = flashOn || inSlide || inFade;
         if (inFade && this.overhaul) {
            this.overhaulAnchors(this.lastRenderDelta);
            this.renderTpMeter(ctx);
         } else if (inFade) {
            float fadeIn = (float)(this.introTick - 36) / 10.0F;
            int uiAlpha = (int)(fadeIn * 255.0F);
            if (!this.lw() && !this.eb() && !TurnBattleConfig.get().deltarune_battle_bg) {
               ctx.fill(0, 0, this.width, this.height, -1342177280);
            } else {
               this.renderBackdrop(ctx);
               int fadeAlpha = (int)((1.0F - fadeIn) * 176.0F);
               if (fadeAlpha > 0) {
                  ctx.fill(0, 0, this.width, this.height, fadeAlpha << 24);
               }
            }
         }

         if (needsBlack) {
            int blackAlpha = inFade ? (int)((1.0F - (float)(this.introTick - 36) / 10.0F) * 255.0F) : 255;
            if (blackAlpha > 0) {
               ctx.fill(0, 0, this.width, this.height, blackAlpha << 24);
            }
         }

         if (flashOn || inSlide || inFade) {
            int soulAlpha = inFade ? (int)((1.0F - (float)(this.introTick - 36) / 10.0F) * 255.0F) : 255;
            int count = Math.max(1, this.participants.isEmpty() ? 1 : this.participants.size());

            for (int i = 0; i < count && i < 8; i++) {
               Identifier soulTex;
               if (this.participants.isEmpty() || i == 0 && count == 1) {
                  soulTex = this.playerSoul != null ? this.playerSoul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
               } else if (i < this.participants.size()) {
                  SoulType pSoul = this.participants.get(i).soul();
                  soulTex = pSoul != null ? pSoul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
               } else {
                  soulTex = Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
               }

               float sy;
               float sx;
               if (flashOn) {
                  float bx = this.introParticipantStartX[i];
                  float by = this.introParticipantStartY[i];
                  if (bx == 0.0F && by == 0.0F) {
                     int cnt = Math.max(1, this.participants.isEmpty() ? 1 : this.participants.size());
                     float totalW2 = (float)(cnt * 9) + (float)(cnt - 1) * 8.0F;
                     bx = (float)this.width / 2.0F - totalW2 / 2.0F + (float)i * 17.0F;
                     by = (float)this.height / 2.0F - 4.5F;
                  }

                  sx = bx;
                  sy = by;
               } else {
                  sx = this.introParticipantXPrev[i] + (this.introParticipantX[i] - this.introParticipantXPrev[i]) * this.lastRenderDelta;
                  sy = this.introParticipantYPrev[i] + (this.introParticipantY[i] - this.introParticipantYPrev[i]) * this.lastRenderDelta;
               }

               MatrixStack matrices = ctx.getMatrices();
               if (inSlide) {
                  float tNow = ((float)(this.introTick - 24) + this.lastRenderDelta) / 12.0F;

                  for (int k = 3; k >= 1; k--) {
                     float tt = Math.max(0.0F, Math.min(1.0F, tNow - (float)k * 0.07F));
                     float st = tt * tt * (3.0F - 2.0F * tt);
                     float ax = this.introParticipantStartX[i] + (this.introParticipantTargetX[i] - this.introParticipantStartX[i]) * st;
                     float ay = this.introParticipantStartY[i] + (this.introParticipantTargetY[i] - this.introParticipantStartY[i]) * st;
                     float aa = (0.42F - (float)k * 0.11F) * ((float)soulAlpha / 255.0F);
                     if (!(aa <= 0.0F)) {
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, aa);
                        ctx.drawTexture(soulTex, Math.round(ax), Math.round(ay), 0.0F, 0.0F, 9, 9, 9, 9);
                     }
                  }

                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               }

               float pop = 1.0F;
               if (flashOn) {
                  int fs = this.introTick >= 10 ? 10 : (this.introTick >= 6 ? 6 : 2);
                  pop = 1.0F + 0.3F * Math.max(0.0F, 1.0F - ((float)(this.introTick - fs) + this.lastRenderDelta) / 2.5F);
               }

               matrices.push();
               matrices.translate(sx + 4.5F, sy + 4.5F, 0.0F);
               matrices.scale(pop, pop, 1.0F);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)soulAlpha / 255.0F);
               RenderSystem.enableBlend();
               ctx.drawTexture(soulTex, -4, -4, 0.0F, 0.0F, 9, 9, 9, 9);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               matrices.pop();
               if (inFade) {
                  float rt = Math.min(1.0F, ((float)(this.introTick - 36) + this.lastRenderDelta) / 7.0F);
                  if (rt < 1.0F) {
                     int soulRgb = i < this.participants.size() && this.participants.get(i).soul() != null
                        ? this.participants.get(i).soul().getColor() & 16777215
                        : (this.playerSoul != null ? this.playerSoul.getColor() & 16777215 : 16724016);
                     float rs = 9.0F * (1.2F + rt * 3.2F);
                     MenuBackdrop.tex(
                        ctx, Identifier.of("turnbattle", "textures/effects/ritual_ring.png"), sx + 4.5F, sy + 4.5F, rs, rs, soulRgb, 0.8F * (1.0F - rt)
                     );
                  }
               }
            }
         }
      }
   }

   private void renderPvpIntro(DrawContext ctx) {
      boolean flashOn = this.introTick >= 2 && this.introTick <= 3
         || this.introTick >= 6 && this.introTick <= 7
         || this.introTick >= 10 && this.introTick <= 23;
      boolean inSlide = this.introTick >= 24 && this.introTick <= 35;
      boolean inFade = this.introTick >= 36 && this.introTick <= 45;
      if (inFade) {
         float fadeIn = (float)(this.introTick - 36) / 10.0F;
         ctx.fill(0, 0, this.width, this.height, -1342177280);
      }

      boolean needsBlack = flashOn || inSlide || inFade;
      if (needsBlack) {
         int blackAlpha = inFade ? (int)((1.0F - (float)(this.introTick - 36) / 10.0F) * 255.0F) : 255;
         if (blackAlpha > 0) {
            ctx.fill(0, 0, this.width, this.height, blackAlpha << 24);
         }
      }

      if (flashOn || inSlide || inFade) {
         Identifier mySoul = this.playerSoul != null ? this.playerSoul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
         Identifier opSoul = this.pvpOpponentSoul != null ? this.pvpOpponentSoul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
         int soulAlpha = inFade ? (int)((1.0F - (float)(this.introTick - 36) / 10.0F) * 255.0F) : 255;
         float gapHalf = 20.0F;
         float centerX = (float)this.width / 2.0F;
         float centerY = (float)this.height / 2.0F - 4.5F;
         float mySX;
         float mySY;
         float opSX;
         float opSY;
         if (flashOn) {
            mySX = centerX + gapHalf;
            mySY = centerY;
            opSX = centerX - gapHalf - 9.0F;
            opSY = centerY;
         } else {
            mySX = this.introSoulX;
            mySY = this.introSoulY;
            float opTargX = centerX - 4.5F;
            float opTargY = 30.0F;
            float opStartX = centerX - gapHalf - 9.0F;
            float t = Math.min(1.0F, (float)(this.introTick - 24) / 12.0F);
            t = t * t * (3.0F - 2.0F * t);
            opSX = opStartX + (opTargX - opStartX) * t;
            opSY = centerY + (opTargY - centerY) * t;
         }

         RenderSystem.enableBlend();
         MatrixStack mat = ctx.getMatrices();
         mat.push();
         mat.translate(opSX + 4.5F, opSY + 4.5F, 0.0F);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)soulAlpha / 255.0F);
         ctx.drawTexture(opSoul, -4, -4, 0.0F, 0.0F, 9, 9, 9, 9);
         mat.pop();
         mat.push();
         mat.translate(mySX + 4.5F, mySY + 4.5F, 0.0F);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)soulAlpha / 255.0F);
         ctx.drawTexture(mySoul, -4, -4, 0.0F, 0.0F, 9, 9, 9, 9);
         mat.pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (flashOn) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("VS").formatted(Formatting.WHITE), (int)centerX, (int)(centerY - 14.0F), -1);
         }
      }
   }

   private void drawParticipantPanel(DrawContext ctx) {
      this.drawDeltarunePanel(ctx);
      this.renderMortalTags(ctx);
   }

   private void drawDeltarunePanel(DrawContext ctx) {
      this.drawDeltarunePanel(ctx, this.colsBottom());
   }

   private void drawDeltarunePanel(DrawContext ctx, int panelBottom) {
      int count = this.participants.size();
      if (count != 0) {
         int colW = 150;
         int maxVisible = 3;
         int maxScroll = Math.max(0, count - 3);
         this.drScrollOffset = Math.max(0, Math.min(this.drScrollOffset, maxScroll));
         int visible = Math.min(count, 3);
         int totalW = visible * 150 + (visible - 1) * 3;
         int startX = (this.width - totalW) / 2;
         boolean playerTurn = this.phase == BattleScreen.Phase.PLAYER_TURN
            || this.phase == BattleScreen.Phase.SKILLS_MENU
            || this.phase == BattleScreen.Phase.GOODS_MENU
            || this.phase == BattleScreen.Phase.ACT_MENU
            || this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY
            || this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY
            || this.phase == BattleScreen.Phase.ALL_OUT
            || this.phase == BattleScreen.Phase.BATON_PASS;
         if (this.eb()) {
            this.drawEarthboundPanel(ctx, playerTurn);
         } else if (this.lw()) {
            this.drawLightWorldPanel(ctx, startX, 150, 3, playerTurn);
         } else {
            for (int i = 0; i < count; i++) {
               int drawIndex = i - this.drScrollOffset;
               if (drawIndex >= 0 && drawIndex < 3) {
                  BattleScreen.ParticipantInfo p = this.participants.get(i);
                  boolean isActive = p.isCurrentTurn() && playerTurn;
                  int colH = isActive ? 53 : 29;
                  int colX = startX + drawIndex * 153;
                  float animH = i < this.animColH.length && this.animColH[i] > 0.0F
                     ? this.animColHPrev[i] + (this.animColH[i] - this.animColHPrev[i]) * this.lastRenderDelta
                     : (float)colH;
                  int slideOffset = Math.max(0, colH - Math.round(animH));
                  int colY = panelBottom - colH + slideOffset;
                  if (i < this.lastParticipantCX.length && (!this.anchors3D() || this.lw())) {
                     this.lastParticipantCX[i] = colX + 75;
                     this.lastParticipantTopY[i] = this.lw() ? colY + 4 : colY - 60;
                  }

                  int hurtShake = this.getPlayerShakeX(i);
                  if (hurtShake != 0) {
                     colX += hurtShake;
                  }

                  int ady = this.colAnimDY(drawIndex);
                  this.drawParticipantColumn(ctx, i, p, colX, colY + ady, 150, colH, isActive, panelBottom + ady, playerTurn);
                  if (i < this.playerDamageTicks.length && this.playerDamageTicks[i] > 0) {
                     float hp = Math.max(0.0F, ((float)this.playerDamageTicks[i] - this.lastRenderDelta) / 12.0F);
                     int fa = (int)(hp * hp * 110.0F);
                     if (fa > 0) {
                        ctx.fill(colX + 2, colY + ady + 2, colX + 150 - 2, Math.min(colY + colH, panelBottom) + ady - 1, fa << 24 | 16719904);
                     }
                  }
               }
            }

            if (count > 3) {
               int dotY = panelBottom - 29 - 10;
               int dotSize = 4;
               int dotGap = 3;
               int dotsW = count * (dotSize + dotGap) - dotGap;
               int dotX0 = (this.width - dotsW) / 2;

               for (int ix = 0; ix < count; ix++) {
                  boolean vis = ix >= this.drScrollOffset && ix < this.drScrollOffset + 3;
                  int dx = dotX0 + ix * (dotSize + dotGap);
                  ctx.fill(dx, dotY, dx + dotSize, dotY + dotSize, vis ? -3355444 : 1432774246);
               }
            }
         }
      }
   }

   private void drawParticipantColumn(
      DrawContext ctx, int i, BattleScreen.ParticipantInfo p, int colX, int colY, int colW, int colH, boolean isActive, int panelBottom, boolean playerTurn
   ) {
      Identifier colTex = Identifier.of("turnbattle", isActive ? "textures/gui/playercolumn.png" : "textures/gui/playercolumncondensed.png");
      int soulRgb = p.soul() != null ? p.soul().getColor() | 0xFF000000 : -1;
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor((float)(soulRgb >> 16 & 0xFF) / 255.0F, (float)(soulRgb >> 8 & 0xFF) / 255.0F, (float)(soulRgb & 0xFF) / 255.0F, 1.0F);
      ctx.enableScissor(colX, 0, colX + colW, panelBottom);
      ctx.drawTexture(colTex, colX, colY, colW, colH, 0.0F, 0.0F, 213, colH, 213, colH);
      ctx.disableScissor();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float trueHp = p.isMe() ? this.playerHp : p.hp();
      float dispMaxHp = p.isMe() ? this.playerMaxHp : p.maxHp();
      float dispHp = p.isMe() ? (this.rollingPlayerHp >= 0.0F ? this.rollingPlayerHpSmooth() : trueHp) : this.rollingParticipantHpSmooth(i, trueHp);
      ctx.enableScissor(colX, colY, colX + colW, panelBottom);
      Identifier skinTex = null;
      MinecraftClient mc = MinecraftClient.getInstance();
      UUID pUuid = i < this.participantUuids.size() ? this.participantUuids.get(i) : null;
      boolean isRecruitSlot = pUuid != null && (pUuid.equals(BattleState.PARTY_MEMBER_UUID) || pUuid.equals(BattleState.PARTY_MEMBER_UUID_2));
      if (!isRecruitSlot && pUuid != null && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(pUuid);
         if (entry != null) {
            skinTex = entry.getSkinTextures().texture();
         }
      }

      if (skinTex == null && p.isMe() && mc.player != null) {
         skinTex = mc.player.getSkinTextures().texture();
      }

      if (skinTex == null && isRecruitSlot) {
         skinTex = RecruitSkinManager.getSkin(p.name());
      }

      int FACE_SZ = 17;
      int faceX = colX + 5;
      isRecruitSlot = (boolean)(colY + 6);
      if (skinTex != null) {
         RenderSystem.enableBlend();
         float fb = this.shownDown(trueHp, dispHp) ? 0.4F : 1.0F;
         RenderSystem.setShaderColor(fb, fb, fb * 1.1F, 1.0F);
         ctx.drawTexture(skinTex, faceX, isRecruitSlot, 17, 17, 8.0F, 8.0F, 8, 8, 64, 64);
         ctx.drawTexture(skinTex, faceX, isRecruitSlot, 17, 17, 40.0F, 8.0F, 8, 8, 64, 64);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      int nameX = faceX + 17 + 8;
      int nameY = colY + (29 - 9 * 2) / 2 + 1;
      if (isActive) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u25b6"), nameX - 8, nameY + 9 / 2, -1);
      }

      ctx.getMatrices().push();
      ctx.getMatrices().translate((float)nameX, (float)nameY, 0.0F);
      ctx.getMatrices().scale(1.125F, 1.125F, 1.0F);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(p.name()), 0, 0, -1);
      ctx.getMatrices().pop();
      int scaledNameW = (int)((float)this.textRenderer.getWidth(p.name()) * 1.125F);
      int statusY = nameY + (int)(9.0F * 1.125F) + 1;
      ctx.getMatrices().push();
      ctx.getMatrices().translate((float)nameX, (float)statusY, 0.0F);
      ctx.getMatrices().scale(0.75F, 0.75F, 1.0F);
      ctx.getMatrices().translate((float)(-nameX), (float)(-statusY), 0.0F);
      this.renderParticipantStatus(ctx, nameX, statusY, p);
      ctx.getMatrices().pop();
      String hpNum = (int)(this.rollingHealth ? (float)Math.round(dispHp) : trueHp) + "/" + (int)dispMaxHp;
      int numW = (int)((float)this.textRenderer.getWidth(hpNum) * 0.75F);
      int hpBarW = colW / 4;
      int hpBarH = 5;
      int pad = 5;
      int hpBarX = colX + colW - pad - hpBarW;
      int hpBarY = colY + 20 - hpBarH / 2;
      float hpFrac = dispMaxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, dispHp / dispMaxHp)) : 0.0F;
      float trueFrac = dispMaxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, trueHp / dispMaxHp)) : 0.0F;
      int fillPx = (int)((float)hpBarW * hpFrac);
      int truePx = (int)((float)hpBarW * trueFrac);
      ctx.getMatrices().push();
      ctx.getMatrices().translate((float)hpBarX + (float)(hpBarW - numW) / 2.0F, (float)hpBarY - 9.0F * 0.75F - 1.0F, 0.0F);
      ctx.getMatrices().scale(0.75F, 0.75F, 1.0F);
      float shownHpV = this.rollingHealth ? dispHp : trueHp;
      int hpNumCol = this.shownDown(trueHp, dispHp) ? -53200 : (dispMaxHp > 0.0F && shownHpV <= dispMaxHp / 4.0F ? -256 : -1);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(hpNum), 0, 0, hpNumCol);
      ctx.getMatrices().pop();
      ctx.fill(hpBarX - 1, hpBarY - 1, hpBarX + hpBarW + 1, hpBarY + hpBarH + 1, -15658735);
      ctx.fill(hpBarX, hpBarY, hpBarX + hpBarW, hpBarY + hpBarH, -14540254);
      if (dispHp > trueHp + 1.0F && truePx < fillPx) {
         ctx.fill(hpBarX + truePx, hpBarY, hpBarX + fillPx, hpBarY + hpBarH, -5618688);
      }

      int hpFillColor = p.soul() != null ? p.soul().getColor() | 0xFF000000 : -14501052;
      if (fillPx > 0) {
         ctx.fill(hpBarX, hpBarY, hpBarX + fillPx, hpBarY + hpBarH, hpFillColor);
         ctx.fill(hpBarX, hpBarY, hpBarX + fillPx, hpBarY + 2, lerpColor(hpFillColor, -1, 0.4F));
      }

      if (this.pvpMode && p.isMe() && this.myPvpShieldMax > 0.0F) {
         int hpNumTopY = (int)((float)hpBarY - 9.0F * 0.75F - 1.0F);
         int shieldBarH = 2;
         int shieldBarY = hpNumTopY - shieldBarH;
         ctx.fill(hpBarX - 1, shieldBarY - 1, hpBarX + hpBarW + 1, shieldBarY + shieldBarH + 1, -15658735);
         float shieldFrac = this.myPvpShieldMax > 0.0F ? Math.max(0.0F, Math.min(1.0F, this.myPvpShield / this.myPvpShieldMax)) : 0.0F;
         int shieldFillPx = (int)((float)hpBarW * shieldFrac);
         int shieldCol = this.myPvpShieldBroken ? -12303292 : -12263937;
         if (shieldFillPx > 0) {
            ctx.fill(hpBarX, shieldBarY, hpBarX + shieldFillPx, shieldBarY + shieldBarH, shieldCol);
         }

         String shieldNum = this.myPvpShieldBroken ? "BROKEN" : String.valueOf((int)this.myPvpShield);
         int shieldNumW = (int)((float)this.textRenderer.getWidth(shieldNum) * 0.6F);
         ctx.getMatrices().push();
         ctx.getMatrices().translate((float)hpBarX + (float)(hpBarW - shieldNumW) / 2.0F, (float)shieldBarY - 9.0F * 0.6F - 1.0F, 0.0F);
         ctx.getMatrices().scale(0.6F, 0.6F, 1.0F);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(shieldNum), 0, 0, this.myPvpShieldBroken ? -7829368 : -12263937);
         ctx.getMatrices().pop();
      }

      ctx.disableScissor();
      if (isActive && playerTurn) {
         int glowCol = p.soul() != null ? p.soul().getColor() & 16777215 : 4500223;
         float br = 0.55F + 0.45F * (float)Math.sin((double)(((float)this.globalTick + this.lastRenderDelta) * 0.12F));
         int ga = (int)(br * 120.0F);
         ctx.fillGradient(colX + 2, colY - 6, colX + colW - 2, colY, 0 | glowCol, ga << 24 | glowCol);
         ctx.fill(colX + 2, colY, colX + colW - 2, colY + 1, (int)(br * 200.0F) << 24 | glowCol);
      }

      if (isActive) {
         boolean animDone = i < this.animColH.length && this.animColH[i] >= 52.5F;
         if (!animDone) {
            ctx.enableScissor(colX, colY, colX + colW, panelBottom);
         }

         this.drawDeltaruneButtons(ctx, colX, colY, colW);
         if (!animDone) {
            ctx.disableScissor();
         }

         if (animDone && playerTurn) {
            int soulCol = p.soul() != null ? p.soul().getColor() & 16777215 : 4500223;
            int lineTop = colY + 29 + 1;
            int lineBottom = colY + 53 - 1;
            int lineW = 1;
            int totalBtnW = 127;
            int maxTravel = Math.max(6, (colW - totalBtnW) / 2 - 3);
            float PERIOD = 36.0F;
            float now = (float)this.globalTick + this.lastRenderDelta;

            for (int k = 0; k < 3; k++) {
               float t = (now / 36.0F + (float)k / 3.0F) % 1.0F;
               float e = 1.0F - (1.0F - t) * (1.0F - t);
               int travel = Math.round(e * (float)maxTravel);
               float alpha = t < 0.12F ? t / 0.12F : 1.0F - (t - 0.12F) / 0.88F;
               int lineCol = (int)(Math.max(0.0F, alpha) * 235.0F) << 24 | soulCol;
               int lx = colX + 1 + travel;
               ctx.fill(lx, lineTop, lx + lineW, lineBottom, lineCol);
               int rx = colX + colW - 1 - travel - lineW;
               ctx.fill(rx, lineTop, rx + lineW, lineBottom, lineCol);
            }
         }
      }
   }

   private void drawLightWorldPanel(DrawContext ctx, int startX, int colW, int maxVisible, boolean playerTurn) {
      int count = this.participants.size();
      int stripTop0 = this.height - 53;
      int lineY0 = stripTop0 + 20 + this.colAnimDY(0);
      ctx.fill(0, lineY0, this.width, lineY0 + 2, -7701600);

      for (int i = 0; i < count; i++) {
         int drawIndex = i - this.drScrollOffset;
         if (drawIndex >= 0 && drawIndex < maxVisible) {
            BattleScreen.ParticipantInfo p = this.participants.get(i);
            boolean isActive = p.isCurrentTurn() && playerTurn;
            int colX = startX + drawIndex * (colW + 3) + this.getPlayerShakeX(i);
            int stripTop = stripTop0 + this.colAnimDY(drawIndex);
            int lineY = stripTop + 20;
            float animH = i < this.animColH.length && this.animColH[i] > 0.0F
               ? this.animColHPrev[i] + (this.animColH[i] - this.animColHPrev[i]) * this.lastRenderDelta
               : 53.0F;
            int slide = isActive ? Math.max(0, 53 - Math.round(animH)) : 0;
            int top = stripTop + slide;
            if (i < this.lastParticipantCX.length) {
               this.lastParticipantCX[i] = colX + colW / 2;
               this.lastParticipantTopY[i] = isActive ? top + 4 : lineY + 4;
            }

            float trueHp = p.isMe() ? this.playerHp : p.hp();
            float dispMaxHp = p.isMe() ? this.playerMaxHp : p.maxHp();
            float dispHp = p.isMe() ? (this.rollingPlayerHp >= 0.0F ? this.rollingPlayerHpSmooth() : trueHp) : this.rollingParticipantHpSmooth(i, trueHp);
            int soulRgb = p.soul() != null ? p.soul().getColor() & 16777215 : 16776960;
            ctx.enableScissor(colX - 2, 0, colX + colW + 2, this.height);
            if (isActive) {
               float br = 0.8F + 0.2F * (float)Math.sin((double)(((float)this.globalTick + this.lastRenderDelta) * 0.12F));
               int edge = 0xFF000000 | scaleColor(soulRgb, br);
               ctx.fill(colX, top, colX + colW, this.height, -16777216);
               ctx.fill(colX, top, colX + colW, top + 2, edge);
               ctx.fill(colX, top, colX + 2, this.height, edge);
               ctx.fill(colX + colW - 2, top, colX + colW, this.height, edge);
               this.lwNameRow(ctx, i, p, colX + 5, top + 4, this.shownDown(trueHp, dispHp) ? 0.0F : 1.0F, colW - 10);
               this.lwHpRow(ctx, colX + 5, top + 17, trueHp, dispHp, dispMaxHp, colW - 10);
               this.drawLightWorldButtons(ctx, colX, top + 28, colW);
            } else {
               this.lwNameRow(ctx, i, p, colX + 5, lineY + 4, this.shownDown(trueHp, dispHp) ? 0.0F : 1.0F, colW - 10);
               this.lwHpRow(ctx, colX + 5, lineY + 17, trueHp, dispHp, dispMaxHp, colW - 10);
            }

            ctx.disableScissor();
            if (i < this.playerDamageTicks.length && this.playerDamageTicks[i] > 0) {
               float hp = Math.max(0.0F, ((float)this.playerDamageTicks[i] - this.lastRenderDelta) / 12.0F);
               int fa = (int)(hp * hp * 110.0F);
               int fy = isActive ? top + 2 : lineY + 2;
               if (fa > 0) {
                  ctx.fill(colX + 2, fy, colX + colW - 2, this.height, fa << 24 | 16719904);
               }
            }
         }
      }

      if (count > maxVisible) {
         int dotSize = 4;
         int dotGap = 3;
         int dotsW = count * (dotSize + dotGap) - dotGap;
         int dotX0 = this.width - 8 - dotsW;

         for (int ix = 0; ix < count; ix++) {
            boolean vis = ix >= this.drScrollOffset && ix < this.drScrollOffset + maxVisible;
            int dx = dotX0 + ix * (dotSize + dotGap);
            ctx.fill(dx, lineY0 - 6, dx + dotSize, lineY0 - 6 + dotSize, vis ? -3355444 : 1432774246);
         }
      }
   }

   private void lwNameRow(DrawContext ctx, int i, BattleScreen.ParticipantInfo p, int x, int y, float trueHp, int w) {
      Identifier skin = this.participantSkin(i, p);
      int FACE = 11;
      if (skin != null) {
         RenderSystem.enableBlend();
         float fb = trueHp <= 0.0F ? 0.4F : 1.0F;
         RenderSystem.setShaderColor(fb, fb, fb * 1.1F, 1.0F);
         ctx.drawTexture(skin, x, y - 1, 11, 11, 8.0F, 8.0F, 8, 8, 64, 64);
         ctx.drawTexture(skin, x, y - 1, 11, 11, 40.0F, 8.0F, 8, 8, 64, 64);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      String name = p.name().toUpperCase(Locale.ROOT);
      int nx = x + 11 + 4;
      int maxW = w - 11 - 4;

      while (name.length() > 1 && this.textRenderer.getWidth(name) > maxW) {
         name = name.substring(0, name.length() - 1);
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal(name).formatted(Formatting.BOLD), nx, y + 1, trueHp <= 0.0F ? -53200 : -1);
      int sx = nx + this.textRenderer.getWidth(Text.literal(name).formatted(Formatting.BOLD)) + 4;
      if (sx < x + w - 10) {
         ctx.getMatrices().push();
         ctx.getMatrices().translate((float)sx, (float)(y + 1), 0.0F);
         ctx.getMatrices().scale(0.75F, 0.75F, 1.0F);
         ctx.getMatrices().translate((float)(-sx), (float)(-(y + 1)), 0.0F);
         this.renderParticipantStatus(ctx, sx, y + 1, p);
         ctx.getMatrices().pop();
      }
   }

   private void lwHpRow(DrawContext ctx, int x, int y, float trueHp, float dispHp, float maxHp, int w) {
      Style num = Style.EMPTY.withFont(DAMAGE_FONT);
      Text hpL = Text.literal("HP").setStyle(num);
      ctx.drawTextWithShadow(this.textRenderer, hpL, x, y, -1);
      int bx = x + this.textRenderer.getWidth(hpL) + 4;
      int bw = Math.min(46, w / 3);
      int bh = 7;
      float frac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, dispHp / maxHp)) : 0.0F;
      float tFrac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, trueHp / maxHp)) : 0.0F;
      ctx.fill(bx, y, bx + bw, y + bh, -3670016);
      int fill = Math.round((float)bw * frac);
      int tPx = Math.round((float)bw * tFrac);
      if (fill > 0) {
         ctx.fill(bx, y, bx + fill, y + bh, -256);
      }

      if (fill > tPx) {
         ctx.fill(bx + tPx, y, bx + fill, y + bh, -24576);
      }

      float shownV = this.rollingHealth ? dispHp : trueHp;
      int col = this.shownDown(trueHp, dispHp) ? -53200 : (maxHp > 0.0F && shownV <= maxHp / 4.0F ? -256 : -1);
      ctx.drawTextWithShadow(
         this.textRenderer, Text.literal((int)(this.rollingHealth ? (float)Math.round(dispHp) : trueHp) + "/" + (int)maxHp).setStyle(num), bx + bw + 4, y, col
      );
   }

   private void drawLightWorldButtons(DrawContext ctx, int colX, int y, int colW) {
      float nowT = (float)this.globalTick + this.lastRenderDelta;
      if (this.drNavIdx != this.lastDrawnNav) {
         this.lastDrawnNav = this.drNavIdx;
         this.navPopAt = nowT;
      }

      float navPop = Math.max(0.0F, 1.0F - (nowT - this.navPopAt) / 4.0F);
      int gap = 4;
      int totalW = 115 + gap * 4;
      int x0 = colX + 6;
      if (x0 + totalW > colX + colW - 4) {
         x0 = colX + (colW - totalW) / 2;
      }

      for (int b = 0; b < 5; b++) {
         boolean sel = this.drNavIdx == b && !this.mercySubmenuOpen && !this.skillsSubmenuOpen;
         int bx = x0 + b * (23 + gap);
         int bh = sel ? 24 : 20;
         Identifier tex = Identifier.of("turnbattle", "textures/gui/" + (sel ? DR_BTN_TEX_SEL[b] : DR_BTN_TEX[b]) + ".png");
         RenderSystem.enableBlend();
         boolean mercyFlash = b == 3 && !this.pvpMode && !TurnBattleClientMod.isRuthless && this.isMercyEligible();
         if (mercyFlash) {
            float pulse = (float)(Math.sin((double)this.globalTick * 0.15) * 0.5 + 0.5);
            RenderSystem.setShaderColor(1.0F, 0.6F + pulse * 0.4F, pulse * 0.85F, 1.0F);
         }

         MatrixStack mm = ctx.getMatrices();
         mm.push();
         if (sel && navPop > 0.0F) {
            float sc = 1.0F + 0.14F * navPop * navPop;
            mm.translate((float)bx + 11.5F, (float)y + 10.0F, 0.0F);
            mm.scale(sc, sc, 1.0F);
            mm.translate(-((float)bx + 11.5F), -((float)y + 10.0F), 0.0F);
         }

         ctx.drawTexture(tex, bx, y, 23, bh, 0.0F, 0.0F, 23, bh, 23, bh);
         mm.pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private Identifier participantSkin(int i, BattleScreen.ParticipantInfo p) {
      MinecraftClient mc = MinecraftClient.getInstance();
      UUID pUuid = i < this.participantUuids.size() ? this.participantUuids.get(i) : null;
      boolean isRecruitSlot = pUuid != null && (pUuid.equals(BattleState.PARTY_MEMBER_UUID) || pUuid.equals(BattleState.PARTY_MEMBER_UUID_2));
      Identifier skinTex = null;
      if (!isRecruitSlot && pUuid != null && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(pUuid);
         if (entry != null) {
            skinTex = entry.getSkinTextures().texture();
         }
      }

      if (skinTex == null && p.isMe() && mc.player != null) {
         skinTex = mc.player.getSkinTextures().texture();
      }

      if (skinTex == null && isRecruitSlot) {
         skinTex = RecruitSkinManager.getSkin(p.name());
      }

      return skinTex;
   }

   private boolean ebPlayerTurn() {
      return this.phase == BattleScreen.Phase.PLAYER_TURN
         || this.phase == BattleScreen.Phase.SKILLS_MENU
         || this.phase == BattleScreen.Phase.GOODS_MENU
         || this.phase == BattleScreen.Phase.ACT_MENU
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY
         || this.phase == BattleScreen.Phase.ALL_OUT
         || this.phase == BattleScreen.Phase.BATON_PASS;
   }

   private void tickEarthbound() {
      boolean pt = this.ebPlayerTurn();

      for (int i = 0; i < this.ebRaise.length; i++) {
         this.ebRaisePrev[i] = this.ebRaise[i];
         boolean up = i < this.participants.size() && this.participants.get(i).isCurrentTurn() && pt;
         this.ebRaise[i] = this.ebRaise[i] + ((up ? 1.0F : 0.0F) - this.ebRaise[i]) * 0.35F;
         if (Math.abs(this.ebRaise[i] - (up ? 1.0F : 0.0F)) < 0.002F) {
            this.ebRaise[i] = up ? 1.0F : 0.0F;
         }
      }
   }

   private int ebVisible() {
      int fit = Math.max(1, (this.width - 8 + 6) / 98);
      return Math.max(1, Math.min(this.participants.size(), Math.min(4, fit)));
   }

   private int ebColX(int drawIndex, int vis) {
      int totalW = vis * 92 + (vis - 1) * 6;
      return (this.width - totalW) / 2 + drawIndex * 98;
   }

   private int ebColTop(int i, int drawIndex) {
      float r = i < this.ebRaise.length ? this.ebRaisePrev[i] + (this.ebRaise[i] - this.ebRaisePrev[i]) * this.lastRenderDelta : 0.0F;
      float e = r * r * (3.0F - 2.0F * r);
      return this.height - 44 + 8 - Math.round(e * 12.0F) + this.colAnimDY(drawIndex);
   }

   int[] ebCloneAnchor(int i) {
      int vis = this.ebVisible();
      int drawIndex = i - this.drScrollOffset;
      if (drawIndex >= 0 && drawIndex < vis && i < this.ebRaise.length) {
         float r = this.ebRaisePrev[i] + (this.ebRaise[i] - this.ebRaisePrev[i]) * this.lastRenderDelta;
         if (r < 0.02F) {
            return null;
         } else {
            float e = r * r * (3.0F - 2.0F * r);
            int cx = this.ebColX(drawIndex, vis) + 46 + this.getPlayerShakeX(i);
            int top = this.ebColTop(i, drawIndex);
            int cy = Math.round((float)(top - 3) + 64.6F + (1.0F - e) * 0.9F * 38.0F);
            return new int[]{cx, cy};
         }
      } else {
         return null;
      }
   }

   private void ebWindow(DrawContext ctx, int x, int y, int w, int h, int fill) {
      this.ebWindow(ctx, x, y, w, h, fill, -987936);
   }

   private void ebWindow(DrawContext ctx, int x, int y, int w, int h, int fill, int frame) {
      ctx.fill(x + 2, y, x + w - 2, y + h, -15724520);
      ctx.fill(x, y + 2, x + w, y + h - 2, -15724520);
      ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, -15724520);
      ctx.fill(x + 3, y + 1, x + w - 3, y + h - 1, frame);
      ctx.fill(x + 1, y + 3, x + w - 1, y + h - 3, frame);
      ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, frame);
      ctx.fill(x + 3, y + 3, x + w - 3, y + h - 3, -15724520);
      ctx.fill(x + 4, y + 4, x + w - 4, y + h - 4, fill);
      ctx.fill(x + 4, y + 1, x + w - 4, y + 2, 1627389951);
      ctx.fill(x + 4, y + h - 2, x + w - 4, y + h - 1, 1073741824);
   }

   private void ebFinger(DrawContext ctx, int tipX, int midY, float a) {
      int w = EB_FINGER[0].length();
      int h = EB_FINGER.length;
      int x0 = tipX - w;
      int y0 = midY - h / 2;

      for (int r = 0; r < h; r++) {
         String row = EB_FINGER[r];

         for (int c = 0; c < w; c++) {
            char ch = row.charAt(c);
            if (ch != '.') {
               ctx.fill(x0 + c, y0 + r, x0 + c + 1, y0 + r + 1, fadeArgb(ch == 'o' ? -15724520 : -1, a));
            }
         }
      }
   }

   private void renderEbCommandWindow(DrawContext ctx) {
      int x = 4;
      int y = 4;
      int w = 66;
      int h = 82;
      this.ebWindow(ctx, x, y, w, h, -16777216);
      boolean live = this.isMyTurn && this.ebPlayerTurn();
      boolean sub = this.mercySubmenuOpen || this.skillsSubmenuOpen || this.phase != BattleScreen.Phase.PLAYER_TURN;
      String[] labels = new String[]{"Bash", this.playerClass == PlayerClass.PSI_USER ? "PSI" : "Skills", "Goods", "Mercy", "Defend"};
      int rowH = 14;
      int ty = y + 8;
      float nowT = (float)this.globalTick + this.lastRenderDelta;

      for (int k = 0; k < EB_CMD_ORDER.length; k++) {
         int b = EB_CMD_ORDER[k];
         boolean sel = live && this.drNavIdx == b;
         int col;
         if (!live) {
            col = -9803144;
         } else if (b == 3 && !this.pvpMode && !TurnBattleClientMod.isRuthless && this.isMercyEligible()) {
            col = (int)nowT / 6 % 2 == 0 ? -8128 : -1;
         } else {
            col = sel ? -1 : -4671288;
         }

         int lx = x + 19;
         ctx.drawText(this.textRenderer, Text.literal(labels[b]), lx, ty + k * rowH + 3, col, true);
         if (sel) {
            float bob = (float)Math.sin((double)(nowT * 0.45F)) * 1.2F;
            this.ebFinger(ctx, lx - 3 + Math.round(bob), ty + k * rowH + 6, sub ? 0.45F : 1.0F);
         }
      }
   }

   private void drawEarthboundPanel(DrawContext ctx, boolean playerTurn) {
      int count = this.participants.size();
      if (count != 0) {
         int vis = this.ebVisible();
         this.drScrollOffset = Math.max(0, Math.min(this.drScrollOffset, Math.max(0, count - vis)));
         long now = System.nanoTime();
         float dtF = this.ebLastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.ebLastNanos) / 1.0E9F);
         this.ebLastNanos = now;

         for (int i = 0; i < count; i++) {
            int drawIndex = i - this.drScrollOffset;
            if (drawIndex >= 0 && drawIndex < vis) {
               BattleScreen.ParticipantInfo p = this.participants.get(i);
               boolean active = p.isCurrentTurn() && playerTurn;
               int x = this.ebColX(drawIndex, vis) + this.getPlayerShakeX(i);
               int top = this.ebColTop(i, drawIndex);
               if (i < this.lastParticipantCX.length) {
                  this.lastParticipantCX[i] = x + 46;
                  this.lastParticipantTopY[i] = top - 14;
               }

               float trueHp = p.isMe() ? this.playerHp : p.hp();
               float maxHp = p.isMe() ? this.playerMaxHp : p.maxHp();
               float dispHp = p.isMe() ? (this.rollingPlayerHp >= 0.0F ? this.rollingPlayerHpSmooth() : trueHp) : this.rollingParticipantHpSmooth(i, trueHp);
               if (i < this.odoShown.length) {
                  if (!Float.isNaN(this.odoShown[i]) && !this.rollingHealth) {
                     this.odoShown[i] = this.odoShown[i] + (dispHp - this.odoShown[i]) * Math.min(1.0F, dtF * 9.0F);
                  } else {
                     this.odoShown[i] = dispHp;
                  }

                  if (Math.abs(this.odoShown[i] - dispHp) < 0.01F) {
                     this.odoShown[i] = dispHp;
                  }
               }

               float odo = i < this.odoShown.length ? this.odoShown[i] : dispHp;
               odo += (float)Math.round(trueHp) - trueHp;
               boolean down = trueHp <= 0.0F && odo <= 0.5F;
               int soul = p.soul() != null ? p.soul().getColor() & 16777215 : 16777215;
               this.ebStatusBox(ctx, x, top, 92, this.height - top + 6, soul, active, down);
               String name = p.name();
               int maxNameW = 78;

               while (name.length() > 1 && this.textRenderer.getWidth(name) > maxNameW) {
                  name = name.substring(0, name.length() - 1);
               }

               int nw = this.textRenderer.getWidth(name);
               ctx.drawText(this.textRenderer, Text.literal(name), x + (92 - nw) / 2, top + 7, down ? -5234656 : -14934998, false);
               int rowY = top + 20;
               ctx.drawText(this.textRenderer, Text.literal("HP").formatted(Formatting.BOLD), x + 8, rowY + 2, -14934998, false);
               int digits = Math.max(3, String.valueOf(Math.max(1, Math.round(maxHp))).length());
               int cellW = 8;
               int cellH = 12;
               int odoW = digits * cellW + (digits - 1) + 2;
               int ox = x + 92 - 8 - odoW;
               this.ebOdometer(ctx, ox, rowY, digits, cellW, cellH, Math.max(0.0F, odo), down);
               ctx.getMatrices().push();
               ctx.getMatrices().translate((float)(x + 4), (float)(top - 7), 0.0F);
               ctx.getMatrices().scale(0.6F, 0.6F, 1.0F);
               ctx.getMatrices().translate((float)(-(x + 4)), (float)(-(top - 7)), 0.0F);
               this.renderParticipantStatus(ctx, x + 4, top - 7, p);
               ctx.getMatrices().pop();
               if (i < this.playerDamageTicks.length && this.playerDamageTicks[i] > 0) {
                  float hp = Math.max(0.0F, ((float)this.playerDamageTicks[i] - this.lastRenderDelta) / 12.0F);
                  int fa = (int)(hp * hp * 120.0F);
                  if (fa > 0) {
                     ctx.fill(x + 4, top + 4, x + 92 - 4, this.height, fa << 24 | 16719904);
                  }
               }
            }
         }

         if (count > vis) {
            int dotSize = 4;
            int dotGap = 3;
            int dotsW = count * (dotSize + dotGap) - dotGap;
            int dx0 = this.width - 8 - dotsW;
            int dy = this.height - 44 - 12 - 8;

            for (int ix = 0; ix < count; ix++) {
               boolean v = ix >= this.drScrollOffset && ix < this.drScrollOffset + vis;
               ctx.fill(dx0 + ix * (dotSize + dotGap), dy, dx0 + ix * (dotSize + dotGap) + dotSize, dy + dotSize, v ? -1118482 : 1720223880);
            }
         }
      }
   }

   private void ebStatusBox(DrawContext ctx, int x, int y, int w, int h, int soul, boolean active, boolean down) {
      int base = down ? 10132134 : soul;
      int c1 = 0xFF000000 | lerpRgbInt(base, 16777215, 0.58F);
      int c2 = 0xFF000000 | lerpRgbInt(base, 16777215, 0.76F);
      int frame = -987936;
      if (active) {
         float pulse = 0.5F + 0.5F * (float)Math.sin((double)(((float)this.globalTick + this.lastRenderDelta) * 0.18F));
         frame = 0xFF000000 | lerpRgbInt(15789280, lerpRgbInt(soul, 16777215, 0.35F), 0.45F + 0.35F * pulse);
      }

      this.ebWindow(ctx, x, y, w, h, c2, frame);
      int T = 4;
      int off = this.globalTick / 4 % 8;
      int ix0 = x + 4;
      int iy0 = y + 4;
      int ix1 = x + w - 4;
      int iy1 = Math.min(this.height, y + h - 4);
      ctx.enableScissor(ix0, iy0, ix1, iy1);

      for (int ty = iy0 - 8 + off; ty < iy1; ty += 4) {
         for (int tx = ix0 - 8 + off; tx < ix1; tx += 4) {
            if (((tx - ix0 - off) / 4 + (ty - iy0 - off) / 4) % 2 == 0) {
               ctx.fill(tx, ty, tx + 4, ty + 4, c1);
            }
         }
      }

      ctx.disableScissor();
      ctx.fillGradient(ix0, iy0, ix1, iy0 + 6, 1358954495, 16777215);
   }

   private void ebOdometer(DrawContext ctx, int x, int y, int digits, int cellW, int cellH, float value, boolean down) {
      int w = digits * cellW + (digits - 1) + 2;
      int h = cellH + 2;
      ctx.fill(x, y, x + w, y + h, -14013894);
      int digitCol = down ? -5234656 : -14934998;

      for (int k = 0; k < digits; k++) {
         int place = digits - 1 - k;
         int cx = x + 1 + k * (cellW + 1);
         int cy = y + 1;
         ctx.fillGradient(cx, cy, cx + cellW, cy + cellH / 2, -2697494, -1);
         ctx.fillGradient(cx, cy + cellH / 2, cx + cellW, cy + cellH, -1, -3881764);
         float pow = (float)Math.pow(10.0, (double)place);
         float wheel;
         if (place == 0) {
            wheel = value - 10.0F * (float)Math.floor((double)(value / 10.0F));
         } else {
            float digit = (float)Math.floor((double)(value / pow)) % 10.0F;
            float below = value - pow * (float)Math.floor((double)(value / pow));
            float turn = Math.max(0.0F, Math.min(1.0F, below - (pow - 1.0F)));
            wheel = digit + turn;
         }

         int d0 = (int)Math.floor((double)wheel) % 10;
         float frac = wheel - (float)Math.floor((double)wheel);
         boolean leading = place > 0 && value < pow;
         ctx.enableScissor(cx, cy, cx + cellW, cy + cellH);
         int tyBase = cy + (cellH - 8) / 2 + 1;
         int shift = Math.round(frac * (float)cellH);
         if (!leading || d0 != 0) {
            ctx.drawText(
               this.textRenderer, String.valueOf(d0), cx + (cellW - this.textRenderer.getWidth(String.valueOf(d0))) / 2 + 1, tyBase - shift, digitCol, false
            );
         }

         if (shift > 0) {
            int d1 = (d0 + 1) % 10;
            ctx.drawText(
               this.textRenderer,
               String.valueOf(d1),
               cx + (cellW - this.textRenderer.getWidth(String.valueOf(d1))) / 2 + 1,
               tyBase - shift + cellH,
               digitCol,
               false
            );
         }

         ctx.disableScissor();
         ctx.fill(cx, cy, cx + cellW, cy + 1, 1342177280);
         ctx.fill(cx, cy + cellH - 1, cx + cellW, cy + cellH, 1342177280);
      }
   }

   private boolean shownDown(float trueHp, float dispHp) {
      return this.rollingHealth ? trueHp <= 0.0F && dispHp <= 0.01F : trueHp <= 0.0F;
   }

   public float shownHp(int i) {
      if (i >= 0 && i < this.participants.size()) {
         BattleScreen.ParticipantInfo p = this.participants.get(i);
         if (p.isMe()) {
            return this.rollingPlayerHp >= 0.0F ? this.rollingPlayerHpSmooth() : this.playerHp;
         } else {
            return this.rollingParticipantHpSmooth(i, p.hp());
         }
      } else {
         return 0.0F;
      }
   }

   public boolean participantShownDown(int i) {
      if (i >= 0 && i < this.participants.size()) {
         BattleScreen.ParticipantInfo p = this.participants.get(i);
         float trueHp = p.isMe() ? this.playerHp : p.hp();
         return this.shownDown(trueHp, this.shownHp(i));
      } else {
         return false;
      }
   }

   private void renderMortalTags(DrawContext ctx) {
      if (this.rollingHealth && !this.overhaul) {
         float t = (float)this.globalTick + this.lastRenderDelta;

         for (int i = 0; i < this.participants.size() && i < this.lastParticipantCX.length; i++) {
            BattleScreen.ParticipantInfo p = this.participants.get(i);
            float trueHp = p.isMe() ? this.playerHp : p.hp();
            float shown = this.shownHp(i);
            if (trueHp <= 0.0F && shown > 0.01F) {
               int cx = this.lastParticipantCX[i];
               int cy = this.lastParticipantTopY[i] - 6;
               if (cx != 0 || cy != -6) {
                  float pulse = 0.5F + 0.5F * (float)Math.sin((double)(t * 0.5F));
                  int jx = (int)Math.round(Math.sin((double)(t * 2.3F + (float)i)) * 1.0);
                  Text tag = Text.literal("MORTAL DAMAGE").setStyle(Style.EMPTY.withFont(DAMAGE_FONT));
                  int tw = this.textRenderer.getWidth(tag);
                  MatrixStack m = ctx.getMatrices();
                  m.push();
                  m.translate((float)(cx + jx), (float)cy, 450.0F);
                  m.scale(0.8F, 0.8F, 1.0F);
                  ctx.fill(-tw / 2 - 3, -2, tw / 2 + 3, 10, Math.round(140.0F + 60.0F * pulse) << 24);
                  int col = 0xFF000000 | lerpRgbInt(12589080, 16736352, pulse);
                  ctx.drawText(this.textRenderer, tag, -tw / 2, 0, col, true);
                  m.pop();
               }
            }
         }
      }
   }

   private void drawDeltaruneButtons(DrawContext ctx, int colX, int colY, int colW) {
      float nowT = (float)this.globalTick + this.lastRenderDelta;
      if (this.drNavIdx != this.lastDrawnNav) {
         this.lastDrawnNav = this.drNavIdx;
         this.navPopAt = nowT;
      }

      float navPop = Math.max(0.0F, 1.0F - (nowT - this.navPopAt) / 4.0F);
      int btnAreaTop = colY + 29;
      int btnAreaH = 24;
      int totalBtnW = 127;
      int btnStartX = colX + (colW - totalBtnW) / 2;
      int btnBottom = colY + 53 - 3;

      for (int b = 0; b < 5; b++) {
         boolean sel = this.drNavIdx == b && !this.mercySubmenuOpen && !this.skillsSubmenuOpen;
         int bx = btnStartX + b * 26;
         int bh = sel ? 24 : 20;
         int by = btnBottom - 20 + (sel ? 1 : 0);
         String texName = sel ? DR_BTN_TEX_SEL[b] : DR_BTN_TEX[b];
         Identifier btnTex = Identifier.of("turnbattle", "textures/gui/" + texName + ".png");
         RenderSystem.enableBlend();
         if (b == 3 && !this.pvpMode && !TurnBattleClientMod.isRuthless && this.isMercyEligible()) {
            float pulse = (float)(Math.sin((double)this.globalTick * 0.15) * 0.5 + 0.5);
            RenderSystem.setShaderColor(1.0F, 0.6F + pulse * 0.4F, pulse * 0.85F, 1.0F);
            RenderSystem.enableBlend();
            ctx.drawTexture(btnTex, bx, by, 23, bh, 0.0F, 0.0F, 23, bh, 23, bh);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         } else if (sel && navPop > 0.0F) {
            float sc = 1.0F + 0.14F * navPop * navPop;
            MatrixStack mm = ctx.getMatrices();
            mm.push();
            mm.translate((float)bx + 11.5F, (float)by + (float)bh / 2.0F, 0.0F);
            mm.scale(sc, sc, 1.0F);
            mm.translate(-((float)bx + 11.5F), -((float)by + (float)bh / 2.0F), 0.0F);
            ctx.drawTexture(btnTex, bx, by, 23, bh, 0.0F, 0.0F, 23, bh, 23, bh);
            mm.pop();
         } else {
            ctx.drawTexture(btnTex, bx, by, 23, bh, 0.0F, 0.0F, 23, bh, 23, bh);
         }
      }
   }

   private int findActiveColX() {
      int n = this.participants.size();
      int visible = Math.min(n, 3);
      int totalW = visible * 150 + (visible - 1) * 3;
      int startX = (this.width - totalW) / 2;

      for (int i = 0; i < n; i++) {
         int drawIndex = i - this.drScrollOffset;
         if (drawIndex >= 0 && drawIndex < 3 && this.participants.get(i).isCurrentTurn()) {
            return startX + drawIndex * 153;
         }
      }

      return startX;
   }

   private void playMenuMove() {
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.MENU_MOVE, 1.0F));
      }
   }

   private void playMenuSelect() {
      if (this.client != null) {
         this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.MENU_SELECT, 1.0F));
      }
   }

   private void activateSkillsSubmenu() {
      this.skillsSubmenuOpen = false;
      boolean isTactician = !this.pvpMode && !this.participants.isEmpty() && this.participants.get(0).isMe() && this.realPlayerClass == null;
      if (this.skillsSubmenuIdx == 0) {
         this.openSkillsMenu();
      } else if (isTactician) {
         this.openActMenu();
      } else {
         this.openSkillsMenu();
      }
   }

   private static Object[] spritesForTypeKey(String typeKey) {
      if (typeKey != null && !typeKey.isEmpty()) {
         String shortName = typeKey.contains(":") ? typeKey.substring(typeKey.lastIndexOf(58) + 1) : typeKey;
         return MOB_SPRITES.get(shortName);
      } else {
         return null;
      }
   }

   private void renderMobSprite(DrawContext ctx, String typeKey, int slot, int cx, int cy) {
      if (!this.anchors3D()) {
         if ("pvp".equals(typeKey) && this.pvpOpponentSoul != null) {
            Identifier soulTex = this.pvpOpponentSoul.getTexture();
            int size = 18;
            int x = cx - size / 2;
            int y = cy - size / 2;
            RenderSystem.enableBlend();
            ctx.drawTexture(soulTex, x, y, size, size, 0.0F, 0.0F, 9, 9, 9, 9);
            if (slot >= 0 && slot < this.lastSpriteCX.length) {
               this.lastSpriteCX[slot] = cx;
               this.lastSpriteCY[slot] = cy;
               this.lastSpriteTopY[slot] = y;
            }
         } else {
            Object[] entry = spritesForTypeKey(typeKey);
            if (entry != null) {
               boolean damaged = this.damageTicks[slot] > 0;
               String path = damaged ? (String)entry[1] : (String)entry[0];
               int nativeW = damaged ? (Integer)entry[4] : (Integer)entry[2];
               int nativeH = damaged ? (Integer)entry[5] : (Integer)entry[3];
               Identifier tex = Identifier.of("turnbattle", path);
               int MAX_W = 80;
               int MAX_H = 120;
               float scale = Math.min(80.0F / (float)nativeW, 120.0F / (float)nativeH);
               scale = Math.min(scale, 1.0F);
               int drawW = Math.max(1, (int)((float)nativeW * scale));
               int drawH = Math.max(1, (int)((float)nativeH * scale));
               int shakeX = 0;
               if (damaged) {
                  float progress = (float)this.damageTicks[slot] / 12.0F;
                  shakeX = (int)(Math.sin((double)this.damageTicks[slot] * 1.8) * 4.0 * (double)progress);
               }

               int x = cx - drawW / 2 + shakeX;
               int y = cy - drawH / 2;
               ctx.drawTexture(tex, x, y, drawW, drawH, 0.0F, 0.0F, nativeW, nativeH, nativeW, nativeH);
               if (slot >= 0 && slot < this.lastSpriteCX.length) {
                  this.lastSpriteCX[slot] = cx + shakeX;
                  this.lastSpriteCY[slot] = cy;
                  this.lastSpriteTopY[slot] = y;
               }
            }
         }
      }
   }

   private int spriteNativeHeight(String typeKey) {
      Object[] entry = spritesForTypeKey(typeKey);
      return entry != null ? (Integer)entry[3] : 0;
   }

   private void renderEntityBar(DrawContext ctx) {
      if (!this.pvpMode) {
         this.renderAllEnemyBars(ctx);
         this.renderSparePopups(ctx);
      }
   }

   private void renderSparePopups(DrawContext ctx) {
      if (!this.sparePopups.isEmpty()) {
         int fallbackX = this.width / 2;
         int fallbackY = this.height / 3;

         for (BattleScreen.SparePopup pop : this.sparePopups) {
            float prog = (float)(this.globalTick - pop.born()) / 50.0F;
            if (!(prog >= 1.0F)) {
               float rise = -(prog * 24.0F);
               float alpha = prog < 0.6F ? 1.0F : 1.0F - (prog - 0.6F) / 0.4F;
               int a = (int)(alpha * 255.0F);
               int popColor = a << 24 | 16768256;
               int popCX = this.lastSpriteCX[0] > 0 ? this.lastSpriteCX[0] : fallbackX;
               int popCY = this.lastSpriteCY[0] > 0 ? this.lastSpriteCY[0] - 20 : fallbackY;
               int tw = this.textRenderer.getWidth(pop.text());
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(pop.text()), popCX - tw / 2, (int)((float)popCY + rise), popColor);
            }
         }
      }
   }

   private void renderAllEnemyBars(DrawContext ctx) {
      boolean hideBars = true;
      int barW = 180;
      int barH = 14;
      int bx = (this.width - barW) / 2;
      int by = 14;
      int total = 1 + this.extraEnemies.size();
      if (total > 1) {
         int spacing = 8;
         int totalBarW = total * barW + (total - 1) * spacing;
         if (totalBarW > this.width - 20) {
            barW = (this.width - 20 - (total - 1) * spacing) / total;
            totalBarW = total * barW + (total - 1) * spacing;
         }

         int startX = (this.width - totalBarW) / 2;
         int barAreaBottom = by + barH + 2 + 9 + 4;
         int textBoxTop = this.height - 72 - 75 - 10;
         int spriteMidY = (barAreaBottom + textBoxTop) / 2;
         float dispPrim = this.displayedEntityHp < 0.0F ? this.entityHp : this.displayedEntityHp;
         float drainPrim = this.drainEntityHp < 0.0F ? this.entityHp : this.drainEntityHp;
         if (!hideBars) {
            this.renderSingleEnemyBar(
               ctx,
               startX,
               by,
               barW,
               barH,
               this.entityName,
               dispPrim,
               drainPrim,
               this.entityMaxHp,
               this.entityPassive,
               this.enemySpareable,
               0,
               this.enemyFreezeTurns,
               this.enemyBurnTurns,
               this.enemySleepTurns,
               this.enemyConfusedTurns,
               this.enemyDownTurns,
               this.enemySturdy,
               this.spareThreshold
            );
         }

         if (this.entityHp > 0.0F) {
            this.renderMobSprite(ctx, this.entityTypeKey, 0, startX + barW / 2, spriteMidY);
         }

         if (this.entityHp > 0.0F || this.overheadBars.recentlyHit(this.currentPrimaryEnemyId)) {
            this.drawOverheadBar(
               ctx,
               this.overheadSlot(this.currentPrimaryEnemyId, 0),
               this.currentPrimaryEnemyId,
               this.entityHp,
               this.entityMaxHp,
               true,
               this.enemyStatusTags(0)
            );
         }

         for (int i = 0; i < this.extraEnemies.size(); i++) {
            BattleScreen.EnemyInfo e = this.extraEnemies.get(i);
            int ex = startX + (i + 1) * (barW + spacing);
            float dDisp = this.displayedExtraHp.getOrDefault(i, e.hp());
            float dDrain = this.drainExtraHp.getOrDefault(i, e.hp());
            if (!hideBars) {
               this.renderSingleEnemyBar(
                  ctx,
                  ex,
                  by,
                  barW,
                  barH,
                  e.name(),
                  dDisp,
                  dDrain,
                  e.maxHp(),
                  "",
                  false,
                  i + 1,
                  e.freezeTurns(),
                  e.burnTurns(),
                  e.sleepTurns(),
                  e.confusedTurns(),
                  e.downTurns(),
                  e.sturdyTurns(),
                  e.spareThreshold()
               );
            }

            this.renderMobSprite(ctx, e.typeKey(), i + 1, ex + barW / 2, spriteMidY);
            if (e.hp() > 0.0F || this.overheadBars.recentlyHit(e.networkId())) {
               this.drawOverheadBar(ctx, this.overheadSlot(e.networkId(), i + 1), e.networkId(), e.hp(), e.maxHp(), false, this.enemyStatusTags(i + 1));
            }
         }
      } else {
         float dispS = this.displayedEntityHp < 0.0F ? this.entityHp : this.displayedEntityHp;
         float drainS = this.drainEntityHp < 0.0F ? this.entityHp : this.drainEntityHp;
         if (!hideBars) {
            this.renderSingleEnemyBar(
               ctx,
               bx,
               by,
               barW,
               barH,
               this.entityName,
               dispS,
               drainS,
               this.entityMaxHp,
               this.entityPassive,
               this.enemySpareable,
               0,
               this.enemyFreezeTurns,
               this.enemyBurnTurns,
               this.enemySleepTurns,
               this.enemyConfusedTurns,
               this.enemyDownTurns,
               this.enemySturdy,
               this.spareThreshold
            );
         }

         int barBottom = by + barH + 2 + 9 + 4;
         int textBoxTopx = this.height - 72 - 75 - 10;
         int spriteCenterY = (barBottom + textBoxTopx) / 2;
         if (this.entityHp > 0.0F) {
            this.renderMobSprite(ctx, this.entityTypeKey, 0, this.width / 2, spriteCenterY);
         }

         if (this.entityHp > 0.0F || this.overheadBars.recentlyHit(this.currentPrimaryEnemyId)) {
            this.drawOverheadBar(
               ctx,
               this.overheadSlot(this.currentPrimaryEnemyId, 0),
               this.currentPrimaryEnemyId,
               this.entityHp,
               this.entityMaxHp,
               true,
               this.enemyStatusTags(0)
            );
         }
      }
   }

   private int overheadSlot(int networkId, int slot2D) {
      if (this.anchors3D()) {
         int r = this.rosterSlotForId(networkId);
         return r >= 0 ? r : slot2D;
      } else {
         return slot2D;
      }
   }

   private void drawOverheadBar(DrawContext ctx, int slot, int key, float hp, float maxHp, boolean primary, List<Object[]> statuses) {
      if (!this.overhaul && slot >= 0 && slot < this.lastSpriteCX.length) {
         int cx = this.lastSpriteCX[slot];
         int top = this.lastSpriteTopY[slot];
         if (cx > 0 && top > 0) {
            boolean lives = primary && this.bossMaxLives > 0;
            boolean boxUp = this.phase == BattleScreen.Phase.ENEMY_TURN || this.phase == BattleScreen.Phase.ENEMY_TURN_END || this.boxShrinkPlaying;
            if (boxUp) {
               statuses = List.of();
            }

            this.overheadBars
               .draw(ctx, this.textRenderer, key, cx, top, hp, maxHp, lives ? this.bossLivesRemaining : 0, lives ? this.bossMaxLives : 0, statuses);
         }
      }
   }

   private List<Object[]> enemyStatusTags(int legacyIdx) {
      int fr;
      int bu;
      int sl;
      int co;
      int dn;
      int st;
      if (legacyIdx == 0) {
         fr = this.enemyFreezeTurns;
         bu = this.enemyBurnTurns;
         sl = this.enemySleepTurns;
         co = this.enemyConfusedTurns;
         dn = this.enemyDownTurns;
         st = this.enemySturdy;
      } else {
         if (legacyIdx - 1 >= this.extraEnemies.size()) {
            return List.of();
         }

         BattleScreen.EnemyInfo e = this.extraEnemies.get(legacyIdx - 1);
         fr = e.freezeTurns();
         bu = e.burnTurns();
         sl = e.sleepTurns();
         co = e.confusedTurns();
         dn = e.downTurns();
         st = e.sturdyTurns();
      }

      List out = new ArrayList<>();
      if (fr > 0) {
         out.add(new Object[]{"FROZEN " + fr, -12268289});
      }

      if (bu > 0) {
         out.add(new Object[]{"BURN " + bu, -39424});
      }

      if (sl > 0) {
         out.add(new Object[]{"SLEEP " + sl, -7807489});
      }

      if (co > 0) {
         out.add(new Object[]{"CONFUSED " + co, -21948});
      }

      if (dn > 0) {
         out.add(new Object[]{"DOWN", -47992});
      }

      if (st > 0) {
         out.add(new Object[]{"STURDY " + st, -5588020});
      }

      return out;
   }

   public float getSpareThreshold() {
      return this.spareThreshold;
   }

   public void onSpareThresholdUpdate(float threshold, int enemyId) {
      if (enemyId == Integer.MIN_VALUE || enemyId == this.currentPrimaryEnemyId) {
         this.setSpareThreshold(threshold);
      }
   }

   private void renderSingleEnemyBar(
      DrawContext ctx,
      int bx,
      int by,
      int barW,
      int barH,
      String name,
      float dispHp,
      float drainHp,
      float maxHp,
      String passive,
      boolean spareable,
      int idx,
      int freezeTurns,
      int burnTurns,
      int sleepTurns,
      int confusedTurns,
      int downTurns,
      int sturdyTurns,
      float barSpareThreshold
   ) {
      float safMax = Math.max(1.0F, maxHp);
      float fillFrac = Math.min(1.0F, Math.max(0.0F, dispHp / safMax));
      float drainFrac = Math.min(1.0F, Math.max(0.0F, drainHp / safMax));
      boolean thresholdSpareable = !spareable && barSpareThreshold >= 1.0F;
      boolean showYellow = spareable || thresholdSpareable;
      int nameColor = showYellow ? -188 : -1;
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal(name).formatted(showYellow ? Formatting.YELLOW : Formatting.WHITE), bx + barW / 2, by - 12, nameColor
      );
      if (sleepTurns > 0 || confusedTurns > 0 || freezeTurns > 0 || burnTurns > 0 || downTurns > 0 || sturdyTurns > 0) {
         int nameW2 = this.textRenderer.getWidth(name);
         int sx = bx + barW / 2 + nameW2 / 2 + 3;
         int sy = by - 12;
         if (freezeTurns > 0) {
            int sw = this.renderStatusCard(ctx, sx, sy, "Frozen", freezeTurns, -12268289);
            sx += sw + 3;
         }

         if (burnTurns > 0) {
            int sw = this.renderStatusCard(ctx, sx, sy, "Burn", burnTurns, -39424);
            sx += sw + 3;
         }

         if (sleepTurns > 0) {
            int sw = this.renderStatusCard(ctx, sx, sy, "Sleep", sleepTurns, -7807489);
            sx += sw + 3;
         }

         if (confusedTurns > 0) {
            int sw = this.renderStatusCard(ctx, sx, sy, "Confused", confusedTurns, -21948);
            sx += sw + 3;
         }

         if (downTurns > 0) {
            int sw = this.renderStatusCard(ctx, sx, sy, "DOWN", 0, -47992);
            sx += sw + 3;
         }

         if (sturdyTurns > 0) {
            this.renderStatusCard(ctx, sx, sy, "STURDY", sturdyTurns, -5588020);
         }
      }

      ctx.fill(bx - 1, by - 1, bx + barW + 1, by + barH + 1, -15658735);
      ctx.fill(bx, by, bx + barW, by + barH, -14540254);
      int fillPx = (int)((float)barW * fillFrac);
      int drainPx = (int)((float)barW * drainFrac);
      if (drainPx > fillPx) {
         ctx.fill(bx + fillPx, by, bx + drainPx, by + barH, -5618688);
      }

      int fillColor;
      if (fillFrac > 0.6F) {
         fillColor = -14496700;
      } else if (fillFrac > 0.3F) {
         fillColor = -2250240;
      } else {
         fillColor = -3399134;
      }

      if (fillPx > 0) {
         ctx.fill(bx, by, bx + fillPx, by + barH, fillColor);
         int hiColor = lerpColor(fillColor, -1, 0.45F);
         ctx.fill(bx, by, bx + fillPx, by + 2, hiColor);
         int shColor = lerpColor(fillColor, -16777216, 0.4F);
         ctx.fill(bx, by + barH - 2, bx + fillPx, by + barH, shColor);
      }

      int threshPx = (int)((float)barW * Math.min(1.0F, barSpareThreshold));
      ctx.fill(bx, by, bx + threshPx, by + barH, 1442831616);
      ctx.fill(bx + threshPx - 1, by - 1, bx + threshPx + 1, by + barH + 1, -8960);
      ctx.fill(bx + threshPx, by, bx + threshPx + 1, by + barH, -86);
      ctx.fill(bx + threshPx - 2, by - 2, bx + threshPx + 2, by, -8960);
      ctx.fill(bx + threshPx - 2, by + barH, bx + threshPx + 2, by + barH + 2, -8960);

      for (int seg = 1; seg <= 3; seg++) {
         threshPx = bx + barW * seg / 4;
         ctx.fill(threshPx, by, threshPx + 1, by + barH, 1426063360);
      }

      if (fillPx > 2 && fillFrac > 0.02F) {
         ctx.fill(bx + fillPx - 1, by, bx + fillPx, by + barH, -855638017);
      }

      String hpText = String.format("%.0f / %.0f", dispHp, maxHp);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hpText), bx + barW / 2, by + barH + 3, -3355444);
      if (idx == 0 && this.bossMaxLives > 0) {
         String livesText = this.bossLivesRemaining + " " + (this.bossLivesRemaining == 1 ? "Life" : "Lives");
         int livesTextWidth = this.textRenderer.getWidth(livesText);
         int cardWidth = livesTextWidth + 8;
         int cardHeight = 9 + 4;
         int cardX = bx + barW + 6;
         int cardY = by + (barH - cardHeight) / 2;
         ctx.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, -872415232);
         ctx.fill(cardX, cardY, cardX + cardWidth, cardY + 1, -22016);
         ctx.fill(cardX, cardY + cardHeight - 1, cardX + cardWidth, cardY + cardHeight, -22016);
         ctx.fill(cardX, cardY, cardX + 1, cardY + cardHeight, -22016);
         ctx.fill(cardX + cardWidth - 1, cardY, cardX + cardWidth, cardY + cardHeight, -22016);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(livesText).formatted(Formatting.GOLD), cardX + 4, cardY + 2, -22016);
      }
   }

   private static int lerpColor(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int rr = (int)((float)ar + (float)(br - ar) * t);
      int rg = (int)((float)ag + (float)(bg - ag) * t);
      int rb = (int)((float)ab + (float)(bb - ab) * t);
      return 0xFF000000 | rr << 16 | rg << 8 | rb;
   }

   private void renderTpMeter(DrawContext ctx) {
      if (!this.uiMoving()) {
         this.renderTpMeterInner(ctx);
      } else {
         float in = this.uiIn(6.0F, 8.0F, 2.0F, 7.0F);
         boolean tpRight = TurnBattleConfig.get().reverseBattleLayout != (this.lw() || this.eb());
         ctx.getMatrices().push();
         ctx.getMatrices().translate((1.0F - in) * 44.0F * (tpRight ? 1.0F : -1.0F), 0.0F, 0.0F);
         this.renderTpMeterInner(ctx);
         ctx.getMatrices().pop();
      }
   }

   private void renderTpMeterInner(DrawContext ctx) {
      boolean tpRight = TurnBattleConfig.get().reverseBattleLayout != (this.lw() || this.eb());
      int bx = tpRight ? this.width - 4 - 28 : 4;
      int by = this.eb() ? 92 : 35;
      float trueTpClamped = Math.max(0.0F, Math.min(this.playerTpMax, this.playerTp));
      float dispTp = this.displayedTp >= 0.0F ? Math.max(0.0F, Math.min(this.playerTpMax, this.displayedTpSmooth())) : trueTpClamped;
      float ghostTp = this.drainTp >= 0.0F ? Math.max(0.0F, Math.min(this.playerTpMax, this.drainTpSmooth())) : dispTp;
      float fillFrac = this.playerTpMax > 0.0F ? dispTp / this.playerTpMax : 0.0F;
      float ghostFrac = this.playerTpMax > 0.0F ? ghostTp / this.playerTpMax : 0.0F;
      Identifier barBg = this.darknessConstricting ? TP_BAR_BG_DARKNESS : TP_BAR_BG;
      Identifier barProg = this.darknessConstricting ? TP_BAR_PROG_DARKNESS : TP_BAR_PROG;
      ctx.drawTexture(barBg, bx, by, 28, 102, 0.0F, 0.0F, 55, 195, 55, 195);
      if (ghostFrac > fillFrac + 0.001F) {
         int ghostPx = (int)(102.0F * ghostFrac);
         if (ghostPx > 0) {
            int ghostY = by + 102 - ghostPx;
            int uvYg = 195 - (int)(195.0F * ghostFrac);
            int uvFillG = (int)(195.0F * ghostFrac);
            RenderSystem.setShaderColor(1.0F, 0.15F, 0.15F, 1.0F);
            ctx.drawTexture(barProg, bx, ghostY, 28, ghostPx, 0.0F, (float)uvYg, 55, uvFillG, 55, 195);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      int fillPx = (int)(102.0F * fillFrac);
      if (fillPx > 0) {
         int fillY = by + 102 - fillPx;
         int uvY = 195 - (int)(195.0F * fillFrac);
         int uvFill = (int)(195.0F * fillFrac);
         ctx.drawTexture(barProg, bx, fillY, 28, fillPx, 0.0F, (float)uvY, 55, uvFill, 55, 195);
      }

      float speedFrac = this.playerTpMax > 0.0F ? Math.max(0.0F, Math.min(1.0F, this.speedLineTpSmooth() / this.playerTpMax)) : 0.0F;
      int speedPx = (int)(102.0F * speedFrac);
      int lineH = 2;
      int lineY = by + 102 - speedPx - lineH + 1;
      int uvYLine = Math.max(0, 195 - (int)(195.0F * speedFrac) - 1);
      ctx.drawTexture(TP_BAR_WHITE, bx, lineY, 28, lineH, 0.0F, (float)uvYLine, 55, 2, 55, 195);
      if (this.phase == BattleScreen.Phase.SKILLS_MENU && this.skillNavIdx >= 0 && this.skillNavIdx < this.availableSkills.size()) {
         int cost = this.effectiveTpCost(this.availableSkills.get(this.skillNavIdx));
         if (cost > 0 && this.playerTpMax > 0.0F) {
            float afterFrac = Math.max(0.0F, (dispTp - (float)cost) / this.playerTpMax);
            if (fillFrac > afterFrac + 0.001F) {
               lineH = (int)(102.0F * fillFrac);
               lineY = (int)(102.0F * afterFrac);
               uvYLine = Math.max(1, lineH - lineY);
               int previewY = by + 102 - lineH;
               int uvYp = 195 - (int)(195.0F * fillFrac);
               int uvFillP = Math.max(1, (int)(195.0F * (fillFrac - afterFrac)));
               float pulse = 0.35F + 0.35F * (float)Math.sin((double)this.globalTick * 0.25);
               RenderSystem.enableBlend();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pulse);
               ctx.drawTexture(TP_BAR_WHITE, bx, previewY, 28, uvYLine, 0.0F, (float)uvYp, 55, uvFillP, 55, 195);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }

      int numX = bx + 5;
      speedPx = by + 42;
      String numStr = String.valueOf((int)dispTp);
      ctx.getMatrices().push();
      ctx.getMatrices().translate((float)numX, (float)speedPx, 0.0F);
      ctx.getMatrices().scale(1.02F, 1.02F, 1.0F);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(numStr), 0, 0, -1);
      ctx.getMatrices().pop();
   }

   private void renderLevelHud(DrawContext ctx) {
      int rx = this.width - 88;
      int ry = this.height - 38;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Lvl. " + this.playerLevel).formatted(Formatting.YELLOW), rx, ry, 16777215);
      int barW = 76;
      ctx.fill(rx, ry + 12, rx + barW, ry + 18, -13421773);
      ctx.fill(rx, ry + 12, rx + (int)((float)barW * Math.min(1.0F, (float)this.playerExp / (float)Math.max(1, this.expNeeded))), ry + 18, -12255420);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(this.playerExp + "/" + this.expNeeded).formatted(Formatting.GREEN), rx, ry + 20, 16777215);
   }

   private void renderRogueTimer(DrawContext ctx) {
      long elapsed = TurnBattleClientMod.rogueElapsedTicks;
      if (TurnBattleClientMod.rogueliteInRun && elapsed >= 0L) {
         String timerStr = "\u23f1 " + RogueUpgrades.formatTime(elapsed) + String.format("  \u00d7%.2f", RogueliteClient.difficulty);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(timerStr).formatted(Formatting.YELLOW), 4, 4, 16777215);
      }
   }

   private void renderDarkFountainEncounterCard(DrawContext ctx) {
   }

   private void renderMessages(DrawContext ctx) {
      int startY = 42;
      int shown = Math.min(this.messages.size(), 4);

      for (int i = 0; i < shown; i++) {
         String msg = this.messages.get(this.messages.size() - shown + i);
         int alpha = i == shown - 1 ? 255 : 85 + i * 42;
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(msg), this.width / 2, startY + i * 11, alpha << 24 | 16777215);
      }
   }

   private int inlineTbX() {
      return this.tbX();
   }

   private int inlineTbY() {
      return this.tbTop();
   }

   private int inlineTbW() {
      return this.tbW();
   }

   private int inlineTbH() {
      return this.tbH();
   }

   private void renderTextBox(DrawContext ctx) {
      if (!this.uiMoving()) {
         this.renderTextBoxInner(ctx);
      } else {
         float in = this.uiIn(4.0F, 8.0F, 3.0F, 8.0F);
         ctx.getMatrices().push();
         ctx.getMatrices().translate(0.0F, (1.0F - in) * (float)(this.eb() ? -(this.tbTop() + this.tbH() + 14) : this.height - this.tbTop() + 8), 0.0F);
         this.renderTextBoxInner(ctx);
         ctx.getMatrices().pop();
      }
   }

   private void renderTextBoxInner(DrawContext ctx) {
      int textBoxX = this.inlineTbX();
      int textBoxY = this.inlineTbY();
      int textBoxW = this.inlineTbW();
      int tbH = this.inlineTbH();
      if (this.eb()) {
         this.renderEbCommandWindow(ctx);
         this.ebWindow(ctx, textBoxX - 4, textBoxY - 4, textBoxW + 8, tbH + 8, -16777216);
      } else {
         ctx.fill(textBoxX - 2, textBoxY - 2, textBoxX + textBoxW + 2, textBoxY + tbH + 2, this.lw() ? -7701600 : -14540254);
         ctx.fill(textBoxX, textBoxY, textBoxX + textBoxW, textBoxY + tbH, -16777216);
      }

      if (this.phase == BattleScreen.Phase.SKILLS_MENU) {
         this.renderInlineSkillsMenu(ctx, textBoxX, textBoxY, textBoxW, tbH);
      } else if (this.phase == BattleScreen.Phase.GOODS_MENU) {
         this.renderInlineGoodsMenu(ctx, textBoxX, textBoxY, textBoxW, tbH);
      } else if (this.mercySubmenuOpen && this.phase == BattleScreen.Phase.PLAYER_TURN) {
         this.renderInlineMercySubmenu(ctx, textBoxX, textBoxY, textBoxW, tbH);
      } else if (this.skillsSubmenuOpen && this.phase == BattleScreen.Phase.PLAYER_TURN) {
         this.renderInlineSkillsSubmenu(ctx, textBoxX, textBoxY, textBoxW, tbH);
      } else if (this.mbActive) {
         this.renderMultiBash(ctx, textBoxX, textBoxY, textBoxW, tbH);
      } else if (this.qteActive && this.phase == BattleScreen.Phase.SKILL_EFFECT) {
         this.renderBashQte(ctx, textBoxX, textBoxY, textBoxW, tbH);
      } else if (this.phase != BattleScreen.Phase.TARGET_SELECT_ENEMY
         && this.phase != BattleScreen.Phase.TARGET_SELECT_ALLY
         && this.phase != BattleScreen.Phase.ACT_MENU) {
         this.renderBattleLogInBox(ctx, textBoxX + 10, textBoxY + 10);
      }
   }

   private void renderInlineMercySubmenu(DrawContext ctx, int tbX, int tbY, int tbW, int tbH) {
      String[] opts = new String[]{"Spare", "Run"};
      int pad = 10;
      int rowH = 9 + 6;
      int soulPad = 13;
      int leftPad = pad + soulPad;

      for (int i = 0; i < opts.length; i++) {
         boolean sel = this.mercySubmenuIdx == i;
         boolean disabled = i == 0 && (TurnBattleClientMod.isRuthless || this.pvpMode);
         int cy = tbY + pad + i * rowH;
         int cx = tbX + leftPad;
         int nameCol = disabled ? -12303292 : (sel ? -188 : -1);
         if (sel && !disabled) {
            Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
            int soulX = cx - 9 - 4;
            int soulY = cy + (9 - 9) / 2;
            if (sTex != null) {
               ctx.drawTexture(sTex, soulX, soulY, 0.0F, 0.0F, 9, 9, 9, 9);
            }
         }

         ctx.drawTextWithShadow(this.textRenderer, Text.literal(opts[i]), cx, cy, nameCol);
      }
   }

   private void renderInlineSkillsSubmenu(DrawContext ctx, int tbX, int tbY, int tbW, int tbH) {
      boolean isTactician = !this.pvpMode && !this.participants.isEmpty() && this.participants.get(0).isMe();
      String[] opts = isTactician ? new String[]{"Skills", "Acts"} : new String[]{"Skills"};
      int pad = 10;
      int rowH = 9 + 6;
      int soulPad = 13;
      int leftPad = pad + soulPad;

      for (int i = 0; i < opts.length; i++) {
         boolean sel = this.skillsSubmenuIdx == i;
         int cy = tbY + pad + i * rowH;
         int cx = tbX + leftPad;
         int nameCol = sel ? -188 : -1;
         if (sel) {
            Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
            int soulX = cx - 9 - 4;
            int soulY = cy + (9 - 9) / 2;
            if (sTex != null) {
               ctx.drawTexture(sTex, soulX, soulY, 0.0F, 0.0F, 9, 9, 9, 9);
            }
         }

         ctx.drawTextWithShadow(this.textRenderer, Text.literal(opts[i]), cx, cy, nameCol);
      }
   }

   private static int scaleColor(int rgb, float factor) {
      int r = Math.max(0, Math.min(255, Math.round((float)(rgb >> 16 & 0xFF) * factor)));
      int g = Math.max(0, Math.min(255, Math.round((float)(rgb >> 8 & 0xFF) * factor)));
      int b = Math.max(0, Math.min(255, Math.round((float)(rgb & 0xFF) * factor)));
      return r << 16 | g << 8 | b;
   }

   private void renderBashQte(DrawContext ctx, int tbX, int tbY, int tbW, int tbH) {
      float fade = this.qteDone ? Math.max(0.0F, 1.0F - this.qteTickSmooth() / 12.0F) : 1.0F;
      float linePos = this.qteDone ? -9.0F : 1.0F - this.qteTickSmooth() / 24.0F;
      UUID me = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
      String nm = this.partyMemberLabel != null ? this.partyMemberLabel.replace("'s Turn!", "").trim() : null;
      UUID who = nm != null ? BattleState.PARTY_MEMBER_UUID : me;
      this.drawQteLane(
         ctx,
         tbX,
         tbY + 3,
         tbW,
         34,
         who,
         nm,
         this.qteSoulColorRgb,
         linePos,
         this.qteDone,
         this.qteResultPos,
         this.qteResultWasCrit,
         this.qteRanOut,
         this.qteDone ? this.qteTickSmooth() : 0.0F,
         Math.max(0.15F, fade),
         this.qteIsMe,
         true
      );
   }

   public void onBattleLine(int seq, String text, int mode) {
      this.queueMode = true;
      boolean wasResolving = this.drResolving;
      this.drText = text == null ? "" : text;
      this.drIdx = 0;
      this.drSeq = seq;
      this.drMode = mode;
      this.drAcked = false;
      this.drShownTick = this.globalTick;
      if (mode == 1) {
         this.drResolving = true;
      } else if (mode == 0) {
         this.drResolving = false;
         if (wasResolving) {
            this.releaseReadyPoses();
         }
      }
   }

   public void onBattleLineSkip(int seq) {
      if (seq == this.drSeq) {
         this.drIdx = this.drText.length();
      }
   }

   private boolean drTyping() {
      return this.drIdx < this.drText.length();
   }

   private void tickBattleLine() {
      if (this.queueMode && this.drTyping() && this.phase != BattleScreen.Phase.BATTLE_INTRO) {
         this.drIdx = Math.min(this.drText.length(), this.drIdx + 2);
         if ((this.globalTick & 1) == 0 && this.phase != BattleScreen.Phase.ENEMY_TURN) {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_TEXT, 0.45F, 1.0F);
         }
      }
   }

   private boolean battleLineKey(int key) {
      if (!this.queueMode || this.drText.isEmpty() || this.chatInputActive || this.mbActive) {
         return false;
      } else if (this.phase == BattleScreen.Phase.ENEMY_TURN || this.phase == BattleScreen.Phase.RESULT || this.phase == BattleScreen.Phase.BATTLE_INTRO) {
         return false;
      } else if (this.drMode == 1 && this.drResolving) {
         if (key == 32 && this.drTyping()) {
            this.drIdx = this.drText.length();
            this.sendBattleLineInput(0);
            return true;
         } else if (this.isConfirm(key)) {
            if (!this.drTyping() && !this.drAcked && this.globalTick - this.drShownTick > 2) {
               this.drAcked = true;
               this.sendBattleLineInput(1);
            }

            return true;
         } else {
            return false;
         }
      } else if (key == 32 && this.drTyping() && !this.isConfirm(key)) {
         this.drIdx = this.drText.length();
         return true;
      } else {
         return false;
      }
   }

   private void sendBattleLineInput(int kind) {
      PacketByteBuf b = PacketByteBufs.create();
      b.writeByte(kind);
      b.writeInt(this.drSeq);
      Net121.sendToServer(TurnBattleMod.BATTLE_LINE_INPUT, b);
   }

   private void renderBattleLineInBox(DrawContext ctx, int x, int y) {
      float scale = 1.4F;
      int usable = (int)((float)(this.inlineTbW() - 20) / scale);
      int starW = this.textRenderer.getWidth("* ");
      List wrapped = new ArrayList<>();
      this.wrapText(this.drText, Math.max(20, usable - starW), wrapped);
      int remaining = this.drIdx;
      MatrixStack m = ctx.getMatrices();
      int shown = Math.min(3, wrapped.size());

      for (int i = 0; i < shown && remaining > 0; i++) {
         String line = wrapped.get(i);
         String part = line.substring(0, Math.min(line.length(), remaining));
         remaining -= line.length() + 1;
         m.push();
         m.translate((float)x, (float)(y + i * 17), 0.0F);
         m.scale(scale, scale, 1.0F);
         if (i == 0) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("*"), 0, 0, -1);
         }

         ctx.drawTextWithShadow(this.textRenderer, Text.literal(part), starW, 0, -1);
         m.pop();
      }

      if (this.drMode == 1 && this.drResolving && !this.drTyping() && !this.drAcked && (this.globalTick / 8 & 1) == 0) {
         int ax = this.inlineTbX() + this.inlineTbW() - 14;
         int ay = this.inlineTbY() + this.inlineTbH() - 11;
         ctx.fill(ax - 3, ay, ax + 4, ay + 1, -1);
         ctx.fill(ax - 2, ay + 1, ax + 3, ay + 2, -1);
         ctx.fill(ax - 1, ay + 2, ax + 2, ay + 3, -1);
         ctx.fill(ax, ay + 3, ax + 1, ay + 4, -1);
      }
   }

   private void holdReadyPose(UUID actor, int animType, ItemStack item) {
      UUID key = this.cloneKeyForActor(actor);
      if (key != null) {
         int color = this.cloneColorForActor(actor);

         CloneAnimator.Act pose = switch (animType) {
            case 8 -> CloneAnimator.Act.BASH_READY;
            default -> CloneAnimator.Act.CHOOSE_SKILL;
            case 10 -> CloneAnimator.Act.CHOOSE_ITEM;
            case 11 -> CloneAnimator.Act.CHOOSE_ACT;
         };
         if (pose == CloneAnimator.Act.CHOOSE_ACT) {
            color = 16753722;
         }

         CloneAnimator.hold(key, pose, color, item, 300000L);
         this.readyKeys.add(key);
      }
   }

   private void releaseReadyPoses() {
      for (UUID k : this.readyKeys) {
         CloneAnimator.releaseMenuPoses(k);
      }

      this.readyKeys.clear();
   }

   private void endQueuedRound() {
      this.drResolving = false;
      if (this.mbActive) {
         for (BattleScreen.MbBar b : this.mbBars) {
            if (!b.done) {
               b.done = true;
               b.timedOut = true;
               b.resultPos = -0.16363636F;
               b.doneTick = this.mbTick;
            }
         }

         if (this.mbEndTick < 0) {
            this.mbEndTick = this.mbTick;
         }
      }

      this.releaseReadyPoses();
   }

   public void onMultiBashStart(List<Object[]> bars) {
      UUID me = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
      this.mbBars.clear();

      for (Object[] o : bars) {
         BattleScreen.MbBar b = new BattleScreen.MbBar();
         b.actor = (UUID)o[0];
         b.owner = (UUID)o[1];
         b.color = (Integer)o[2];
         b.crit = (Boolean)o[3];
         b.delay = (Integer)o[4];
         b.pIdx = (Integer)o[5];
         b.name = (String)o[6];
         b.mine = b.owner != null && b.owner.equals(me);
         if (b.crit) {
            b.done = true;
            b.resultPos = 0.0F;
            b.resultCrit = true;
         }

         this.mbBars.add(b);
      }

      this.mbActive = !this.mbBars.isEmpty();
      this.mbTick = 0;
      this.mbTickPrev = 0;
      this.mbEndTick = -1;
      this.drResolving = true;
      this.qteActive = false;
   }

   public void onMultiBashBar(UUID actor, float pos, boolean crit) {
      for (BattleScreen.MbBar b : this.mbBars) {
         if (b.actor.equals(actor) && !b.done) {
            b.done = true;
            b.resultPos = pos;
            b.resultCrit = crit;
            b.timedOut = pos <= -0.16263635F;
            b.doneTick = this.mbTick;
            break;
         }
      }
   }

   private float mbTickSmooth() {
      return (float)this.mbTickPrev + (float)(this.mbTick - this.mbTickPrev) * this.lastRenderDelta;
   }

   private static float mbPos(BattleScreen.MbBar b, float t) {
      return 1.0F - (t - (float)b.delay) / 24.0F;
   }

   private void tickMultiBash() {
      if (this.mbActive) {
         this.mbTickPrev = this.mbTick++;
         List ranOut = new ArrayList<>();
         boolean allDone = true;

         for (BattleScreen.MbBar b : this.mbBars) {
            if (!b.done && b.mine && mbPos(b, (float)this.mbTick) <= -0.16363636F) {
               b.done = true;
               b.timedOut = true;
               b.resultPos = -0.16363636F;
               b.doneTick = this.mbTick;
               ranOut.add(b);
            }

            if (!b.done) {
               allDone = false;
            }
         }

         if (!ranOut.isEmpty()) {
            this.sendMultiBash(ranOut);
         }

         if (allDone && this.mbEndTick < 0) {
            this.mbEndTick = this.mbTick;
         }

         if (this.mbEndTick >= 0 && this.mbTick - this.mbEndTick > 13) {
            this.mbActive = false;
            this.mbBars.clear();
         }
      }
   }

   private void hitMultiBash() {
      float t = this.mbTickSmooth();
      BattleScreen.MbBar lead = null;
      float leadPos = 2.0F;

      for (BattleScreen.MbBar b : this.mbBars) {
         if (b.mine && !b.done) {
            float p = mbPos(b, t);
            if (!(p > 0.5204545F) && p < leadPos) {
               lead = b;
               leadPos = p;
            }
         }
      }

      if (lead != null) {
         List hit = new ArrayList<>();

         for (BattleScreen.MbBar bx : this.mbBars) {
            if (bx.mine && !bx.done) {
               float p = mbPos(bx, t);
               if (Math.abs(p - leadPos) <= 0.012F) {
                  bx.done = true;
                  bx.resultPos = p;
                  bx.resultCrit = qteScore(bx.resultPos)[2] > 0.0F;
                  bx.doneTick = this.mbTick;
                  hit.add(bx);
               }
            }
         }

         if (!hit.isEmpty()) {
            this.sendMultiBash(hit);
         }
      }
   }

   private void sendMultiBash(List<BattleScreen.MbBar> bars) {
      PacketByteBuf b = PacketByteBufs.create();
      b.writeInt(bars.size());

      for (BattleScreen.MbBar bar : bars) {
         float[] s = bar.timedOut ? new float[]{0.0F, 0.0F, 0.0F} : qteScore(bar.resultPos);
         b.writeUuid(bar.actor);
         b.writeFloat(s[0]);
         b.writeInt((int)s[1]);
         b.writeFloat(bar.resultPos);
      }

      Net121.sendToServer(TurnBattleMod.MULTI_BASH_RESULT, b);
   }

   private void renderMultiBash(DrawContext ctx, int tbX, int tbY, int tbW, int tbH) {
      int n = this.mbBars.size();
      if (n != 0) {
         float fadeAll = this.mbEndTick < 0
            ? 1.0F
            : Math.max(0.0F, Math.min(1.0F, 1.0F - ((float)(this.mbTick - this.mbEndTick - 10) + this.lastRenderDelta) / 3.0F));
         int rowH = Math.max(12, Math.min(34, (tbH - 6) / n));
         float t = this.mbTickSmooth();

         for (int i = 0; i < n; i++) {
            BattleScreen.MbBar b = this.mbBars.get(i);
            float linePos = b.done ? -9.0F : mbPos(b, t);
            this.drawQteLane(
               ctx,
               tbX,
               tbY + 3 + i * rowH,
               tbW,
               rowH,
               b.actor,
               b.name,
               b.color,
               linePos,
               b.done,
               b.resultPos,
               b.resultCrit,
               b.timedOut,
               b.done ? (float)(this.mbTick - b.doneTick) + this.lastRenderDelta : 0.0F,
               fadeAll,
               b.mine,
               i == n - 1
            );
         }
      }
   }

   private void drawQteLane(
      DrawContext ctx,
      int tbX,
      int y,
      int tbW,
      int rowH,
      UUID actor,
      String name,
      int soulRgb,
      float linePos,
      boolean done,
      float resultPos,
      boolean resultCrit,
      boolean timedOut,
      float since,
      float fade,
      boolean mine,
      boolean lastLane
   ) {
      int A = Math.round(255.0F * fade);
      if (A > 4) {
         int rgb = soulRgb == 0 ? 16777215 : soulRgb;
         int light = scaleColor(rgb, 1.4F);
         int dark = scaleColor(rgb, 0.6F);
         int h = Math.max(8, rowH - 4);
         int icon = Math.max(8, Math.min(22, h - 6));
         int iconX = tbX + 6;
         int iconY = y + (h - icon) / 2 + 1;
         Identifier skin = this.skinForActor(actor, name);
         if (skin != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fade);
            ctx.drawTexture(skin, iconX, iconY, icon, icon, 8.0F, 8.0F, 8, 8, 64, 64);
            ctx.drawTexture(skin, iconX, iconY, icon, icon, 40.0F, 8.0F, 8, 8, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate((float)(tbX + 36), (float)(y + 3), 0.0F);
         m.scale(0.75F, 0.75F, 1.0F);
         ctx.drawText(this.textRenderer, Text.literal("PRESS"), 0, 0, A << 24 | 16777215, false);
         m.pop();
         int boxX = tbX + 96;
         float boxCx = (float)boxX + 4.5F;
         int frameR = boxX + 119;
         int trackEnd = Math.round(boxCx + 220.0F) + 6;
         int right = tbX + tbW - 4;
         ctx.fill(lastLane ? tbX + 4 : boxX, y + h, Math.min(right, trackEnd), y + h + 2, A << 24 | dark);
         outline(ctx, boxX, y, frameR, y + h, A << 24 | dark);
         boolean lineInBox = !done && linePos <= 0.020454545F && linePos >= -0.020454545F;
         outline(ctx, boxX, y, boxX + 9, y + h, A << 24 | (lineInBox && mine ? 16777215 : light));
         int LINE_W = 4;
         int GAP = 13;
         if (done) {
            float k = Math.min(1.0F, since / 12.0F);
            float eased = 1.0F - (1.0F - k) * (1.0F - k) * (1.0F - k);
            int ix = Math.round(boxCx + resultPos * 220.0F) - 2;
            if (timedOut) {
               int ma = Math.round((float)A * (1.0F - k));
               if (ma > 4) {
                  m.push();
                  m.translate((float)(boxX + 12), (float)y + (float)(h - 6) / 2.0F, 0.0F);
                  m.scale(0.75F, 0.75F, 1.0F);
                  ctx.drawText(this.textRenderer, Text.literal("MISS"), 0, 0, ma << 24 | 12632256, false);
                  m.pop();
               }
            } else {
               int col = resultCrit ? 16770138 : 16777215;
               int grow = Math.round(eased * 5.0F);
               int fa = Math.round((float)A * (1.0F - eased));
               ctx.fill(ix - grow, y - grow, ix + 4 + grow, y + h + grow, fa << 24 | col);
               if (resultCrit && k < 1.0F) {
                  outline(ctx, boxX - grow, y - grow, boxX + 9 + grow, y + h + grow, fa << 24 | 16770138);
               }
            }
         } else {
            float px = boxCx + linePos * 220.0F;
            int ix = Math.round(px) - 2;
            if (ix <= right) {
               float runOut = linePos < -0.020454545F ? Math.max(0.0F, 1.0F + (linePos + 0.020454545F) / 0.14318182F) : 1.0F;
               int lineA = Math.round((float)A * runOut * (mine ? 1.0F : 0.7F));
               int[] greys = new int[]{154, 116, 82, 54, 30};

               for (int k = 0; k < greys.length; k++) {
                  int tx = ix + (k + 1) * 13;
                  if (tx + 4 > right) {
                     break;
                  }

                  int g = greys[k];
                  ctx.fill(tx, y, tx + 4, y + h, lineA << 24 | g << 16 | g << 8 | g);
               }

               if (ix + 4 <= right) {
                  ctx.fill(ix, y, ix + 4, y + h, lineA << 24 | 16777215);
               }
            }
         }
      }
   }

   private Identifier skinForActor(UUID actor, String name) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (actor == null) {
         return mc.player != null ? mc.player.getSkinTextures().texture() : null;
      } else if (BattleEntityRenderer.isRecruitUuid(actor)) {
         return RecruitSkinManager.getSkin(name);
      } else {
         if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(actor);
            if (e != null) {
               return e.getSkinTextures().texture();
            }
         }

         return mc.player != null && actor.equals(mc.player.getUuid()) ? mc.player.getSkinTextures().texture() : null;
      }
   }

   private String shortName(String s) {
      if (s == null) {
         return "";
      } else {
         String p = s.replace("'s Turn!", "").trim();
         return p.length() > 12 ? p.substring(0, 11) + "." : p;
      }
   }

   private static float[] qteScore(float pos) {
      if (pos <= 0.020454545F && pos >= -0.020454545F) {
         return new float[]{1.6F, 3.0F, 1.0F};
      } else {
         float k;
         if (pos > 0.020454545F) {
            k = (pos - 0.020454545F) / 0.49999997F;
         } else {
            k = (-pos - 0.020454545F) / 0.14318182F;
         }

         k = Math.max(0.0F, Math.min(1.0F, k));
         return new float[]{1.2F - 0.8F * k, k < 0.35F ? 2.0F : 1.0F, 0.0F};
      }
   }

   private static void outline(DrawContext ctx, int x0, int y0, int x1, int y1, int argb) {
      ctx.fill(x0, y0, x1, y0 + 1, argb);
      ctx.fill(x0, y1 - 1, x1, y1, argb);
      ctx.fill(x0, y0, x0 + 1, y1, argb);
      ctx.fill(x1 - 1, y0, x1, y1, argb);
   }

   private void renderBattleLogInBox(DrawContext ctx, int x, int y) {
      if (this.queueMode) {
         this.renderBattleLineInBox(ctx, x, y);
      } else {
         int tbW = this.inlineTbW() - 20;
         float scale = 1.4F;
         int scaledW = (int)((float)tbW / scale);
         List rawLines = new ArrayList<>(this.messages);
         if (!this.twCurrent.isEmpty()) {
            String partial = this.twCurrent.substring(0, Math.min(this.twCharIdx, this.twCurrent.length()));
            if (!partial.isEmpty()) {
               rawLines.add(partial);
            }
         }

         List wrapped = new ArrayList<>();

         for (String raw : rawLines) {
            this.wrapText("* " + raw, scaledW, wrapped);
         }

         int shown = Math.min(wrapped.size(), 3);
         MatrixStack matrices = ctx.getMatrices();

         for (int i = 0; i < shown; i++) {
            String line = wrapped.get(wrapped.size() - shown + i);
            matrices.push();
            matrices.translate((float)x, (float)(y + i * 17), 0.0F);
            matrices.scale(scale, scale, 1.0F);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(line), 0, 0, -1);
            matrices.pop();
         }
      }
   }

   private void wrapText(String text, int maxWidth, List<String> out) {
      if (this.textRenderer.getWidth(text) <= maxWidth) {
         out.add(text);
      } else {
         String[] words = text.split(" ", -1);
         StringBuilder current = new StringBuilder();

         for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (this.textRenderer.getWidth(test) > maxWidth && current.length() > 0) {
               out.add(current.toString());
               current = new StringBuilder(word);
            } else {
               current = new StringBuilder(test);
            }
         }

         if (current.length() > 0) {
            out.add(current.toString());
         }
      }
   }

   private void tickTypewriter() {
      if (!this.twCurrent.isEmpty()) {
         this.twCharIdx += 2;
         if (this.twCharIdx >= this.twCurrent.length()) {
            this.twCharIdx = this.twCurrent.length();
            this.messages.add(this.twCurrent);
            if (this.messages.size() > 6) {
               this.messages.remove(0);
            }

            this.twCurrent = "";
            this.twCharIdx = 0;
            if (!this.logQueue.isEmpty()) {
               this.twCurrent = this.logQueue.poll();
               this.twCharIdx = 0;
            }
         }
      } else if (!this.logQueue.isEmpty()) {
         this.twCurrent = this.logQueue.poll();
         this.twCharIdx = 0;
      }
   }

   private void enqueueLog(String text) {
      List incoming = new ArrayList<>();

      for (String line : text.split("\n", -1)) {
         if (!line.isEmpty()) {
            incoming.add(line);
         }
      }

      if (!incoming.isEmpty()) {
         int startIdx = 0;
         if (!this.lastKnownLogLine.isEmpty()) {
            for (int i = incoming.size() - 1; i >= 0; i--) {
               if (incoming.get(i).equals(this.lastKnownLogLine)) {
                  startIdx = i + 1;
                  break;
               }
            }
         }

         for (int ix = startIdx; ix < incoming.size(); ix++) {
            this.logQueue.add(incoming.get(ix));
         }

         if (!incoming.isEmpty()) {
            this.lastKnownLogLine = incoming.get(incoming.size() - 1);
         }
      }
   }

   private void flushLog(String text) {
      this.logQueue.clear();
      this.twCurrent = "";
      this.twCharIdx = 0;
      this.messages.clear();
      this.knownLogLines.clear();
      this.knownLogCount = 0;
      this.lastKnownLogLine = "";
      List lines = new ArrayList<>();

      for (String line : text.split("\n", -1)) {
         if (!line.isEmpty()) {
            lines.add(line);
         }
      }

      this.messages.addAll(lines);
      this.knownLogLines.addAll(lines);
      this.knownLogCount = lines.size();
      if (!lines.isEmpty()) {
         this.lastKnownLogLine = lines.get(lines.size() - 1);
      }

      while (this.messages.size() > 6) {
         this.messages.remove(0);
      }
   }

   private void renderAnimatedBattleBox(DrawContext ctx) {
      this.renderAnimatedBattleBox(ctx, 1.0F);
   }

   private void renderSoulReturn(DrawContext ctx, float delta) {
      if (this.soulReturning) {
         float tickF = (float)this.soulReturnTick + delta;
         Identifier soulTexture = this.playerSoul != null ? this.playerSoul.getTexture() : null;
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         if (tickF < 4.6666665F) {
            float t = tickF / 4.6666665F;
            float sx = this.soulReturnStartX + (this.soulReturnTargetX - this.soulReturnStartX) * t;
            float sy = this.soulReturnStartY + (this.soulReturnTargetY - this.soulReturnStartY) * t;
            this.drawSoulScaled(ctx, soulTexture, sx + 4.5F, sy + 4.5F, 1.0F, 1.0F, 1.0F);
         } else {
            float burst = (tickF - 4.6666665F) * 1.5F;
            float cx = this.soulReturnTargetX + 4.5F;
            float cy = this.soulReturnTargetY + 4.5F;
            this.drawSoulScaled(ctx, soulTexture, cx, cy, 0.25F + burst, 0.25F + burst / 2.0F, 0.8F - burst / 6.0F);
            this.drawSoulScaled(ctx, soulTexture, cx, cy, 0.25F + burst / 1.5F, 0.25F + burst / 3.0F, (1.0F - burst / 6.0F) * 0.7F);
            this.drawSoulScaled(ctx, soulTexture, cx, cy, 0.2F + burst / 2.5F, 0.2F + burst / 5.0F, (1.2F - burst / 6.0F) * 0.7F);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   private void drawSoulScaled(DrawContext ctx, Identifier tex, float cx, float cy, float sx, float sy, float alpha) {
      if (!(alpha <= 0.004F) && !(sx <= 0.0F) && !(sy <= 0.0F)) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate(cx, cy, 0.0F);
         m.scale(sx, sy, 1.0F);
         m.translate(-4.5F, -4.5F, 0.0F);
         if (tex != null) {
            ctx.drawTexture(tex, 0, 0, 0.0F, 0.0F, 9, 9, 9, 9);
         } else {
            ctx.drawTexture(ICONS, 0, 0, 52, 0, 9, 9);
         }

         m.pop();
      }
   }

   private void renderAnimatedBattleBox(DrawContext ctx, float partialTick) {
      int centerX = this.width / 2;
      int colsBottom = this.arenaBottom();
      int centerY = (this.arenaTop() + colsBottom) / 2 + 5;
      if (this.phase == BattleScreen.Phase.ENEMY_TURN && this.drSpinTick < 12) {
         float p = this.renderBoxSpawn(ctx, centerX, centerY, partialTick);
         int tw = Math.max(1, (int)((float)this.targetBoxWidth * p));
         int th = Math.max(1, (int)((float)this.targetBoxHeight * p));
         this.boxX = centerX - tw / 2;
         this.boxY = centerY - th / 2;
      } else if (!this.boxShrinkPlaying && !this.boxRetreatFx) {
         int boxDrawX = centerX - this.currentBoxWidth / 2;
         int boxDrawY = centerY - this.currentBoxHeight / 2;
         int borderColor = -16711868;
         ctx.fill(boxDrawX - 3, boxDrawY - 3, boxDrawX + this.currentBoxWidth + 3, boxDrawY + this.currentBoxHeight + 3, borderColor);
         ctx.fill(boxDrawX, boxDrawY, boxDrawX + this.currentBoxWidth, boxDrawY + this.currentBoxHeight, -16777216);
         if (this.currentBoxWidth == this.targetBoxWidth && this.currentBoxHeight == this.targetBoxHeight) {
            this.boxX = boxDrawX;
            this.boxY = boxDrawY;
         }
      } else {
         float p = this.renderBoxRetreat(ctx, centerX, centerY, partialTick);
         int tw = Math.max(1, (int)((float)this.targetBoxWidth * p));
         int th = Math.max(1, (int)((float)this.targetBoxHeight * p));
         this.boxX = centerX - tw / 2;
         this.boxY = centerY - th / 2;
      }
   }

   private float renderBoxRetreat(DrawContext ctx, int cx, int cy, float partialTick) {
      float f = this.drSpinOutTickSmooth(partialTick) * 1.5F;
      float p = Math.max(0.0F, 1.0F - f / 15.0F);
      if (p > 0.0F) {
         float alpha = 0.5F + 0.5F * p;
         this.drawRetreatBox(ctx, cx, cy, p, (float)(Math.PI * (double)f / 15.0), alpha, true);
      }

      int made = Math.min(15, (int)Math.floor((double)f));

      for (int k = 1; k <= made; k++) {
         float pk = 1.0F - (float)k / 15.0F;
         if (!(pk <= 0.0F)) {
            float alpha = 0.6F - 0.5F * pk - 0.04F * (f - (float)k);
            if (!(alpha <= 0.004F)) {
               this.drawRetreatBox(ctx, cx, cy, pk, (float)(Math.PI * (double)k / 15.0), alpha, false);
            }
         }
      }

      return p;
   }

   private float renderBoxSpawn(DrawContext ctx, int cx, int cy, float partialTick) {
      float f = this.drSpinTickSmooth(partialTick) * 1.5F;
      float p = Math.min(1.0F, f / 15.0F);
      this.drawRetreatBox(ctx, cx, cy, p, (float)(Math.PI * (double)(1.0F - p)), 0.5F + 0.5F * p, true);
      int made = Math.min(15, (int)Math.floor((double)f));

      for (int k = made; k >= 1; k--) {
         float pk = (float)k / 15.0F;
         float alpha = 0.6F - 0.5F * pk - 0.04F * (f - (float)k);
         if (!(alpha <= 0.004F)) {
            this.drawRetreatBox(ctx, cx, cy, pk, (float)(Math.PI * (double)(1.0F - pk)), alpha, false);
         }
      }

      return p;
   }

   private void drawRetreatBox(DrawContext ctx, int cx, int cy, float scale, float angle, float alpha, boolean filled) {
      float w = (float)this.targetBoxWidth * scale;
      float h = (float)this.targetBoxHeight * scale;
      float b = 3.0F * scale;
      if (!(w < 0.5F) && !(h < 0.5F)) {
         int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
         MatrixStack m = ctx.getMatrices();
         m.push();
         m.translate((float)cx, (float)cy, 0.0F);
         m.multiply(RotationAxis.POSITIVE_Z.rotation(angle));
         m.scale(scale, scale, 1.0F);
         int hw = this.targetBoxWidth / 2;
         int hh = this.targetBoxHeight / 2;
         int col = a << 24 | 65348;
         if (filled) {
            ctx.fill(-hw - 3, -hh - 3, hw + 3, hh + 3, col);
            ctx.fill(-hw, -hh, hw, hh, a << 24);
         } else {
            ctx.fill(-hw - 3, -hh - 3, hw + 3, -hh, col);
            ctx.fill(-hw - 3, hh, hw + 3, hh + 3, col);
            ctx.fill(-hw - 3, -hh, -hw, hh, col);
            ctx.fill(hw, -hh, hw + 3, hh, col);
         }

         m.pop();
      }
   }

   private void renderSpinBox(DrawContext ctx, int cx, int cy, int tw, int th, float angle, float ease) {
      MatrixStack matrices = ctx.getMatrices();
      int afterCount = 14;
      float angleStep = 0.1308997F;
      float easeStep = 0.05F;

      for (int a = afterCount; a >= 0; a--) {
         float aAngle = angle - (float)a * angleStep;
         float aEase = Math.max(0.0F, ease - (float)a * easeStep);
         if (!(aEase <= 0.0F) || a <= 0) {
            int aw = Math.max(4, (int)((float)this.targetBoxWidth * aEase));
            int ah = Math.max(4, (int)((float)this.targetBoxHeight * aEase));
            float fadeT = 1.0F - (float)a / (float)(afterCount + 1);
            int alpha = a == 0 ? 255 : (int)(110.0F * fadeT * fadeT);
            if (a == 0 || alpha > 2) {
               matrices.push();
               matrices.translate((float)cx, (float)cy, 0.0F);
               matrices.multiply(RotationAxis.POSITIVE_Z.rotation(aAngle));
               matrices.translate((float)(-aw) / 2.0F, (float)(-ah) / 2.0F, 0.0F);
               int borderColor = alpha << 24 | 65348;
               ctx.fill(-3, -3, aw + 3, ah + 3, borderColor);
               if (a == 0) {
                  ctx.fill(0, 0, aw, ah, -16777216);
               }

               matrices.pop();
            }
         }
      }
   }

   private int renderStatusCard(DrawContext ctx, int x, int y, String label, int turns, int color) {
      String text = turns > 0 ? label + " " + turns : label;
      int textWidth = this.textRenderer.getWidth(text);
      int cardWidth = textWidth + 6;
      int cardHeight = 9 + 2;
      ctx.fill(x, y, x + cardWidth, y + cardHeight, -872415232);
      ctx.fill(x, y, x + cardWidth, y + 1, color);
      ctx.fill(x, y + cardHeight - 1, x + cardWidth, y + cardHeight, color);
      ctx.fill(x, y, x + 1, y + cardHeight, color);
      ctx.fill(x + cardWidth - 1, y, x + cardWidth, y + cardHeight, color);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(text), x + 3, y + 1, color);
      return cardWidth;
   }

   private void renderEnemyStatus(DrawContext ctx, int barX, int barY, int barW, int idx) {
      if (idx == 0) {
         int statusX = barX + barW + 6;
         int statusY = barY;
         if (this.bossMaxLives > 0) {
            String livesText = this.bossLivesRemaining + " " + (this.bossLivesRemaining == 1 ? "Life" : "Lives");
            int livesTextWidth = this.textRenderer.getWidth(livesText);
            int livesCardWidth = livesTextWidth + 8;
            statusX = barX + barW + 6 + livesCardWidth + 4;
            statusY = barY + (10 - 9 - 2) / 2;
         }

         if (this.enemySleepTurns > 0) {
            int width = this.renderStatusCard(ctx, statusX, statusY, "Sleep", this.enemySleepTurns, -7807489);
            statusX += width + 3;
         }

         if (this.enemyConfusedTurns > 0) {
            this.renderStatusCard(ctx, statusX, statusY, "Confused", this.enemyConfusedTurns, -21948);
         }
      }
   }

   private int renderParticipantStatus(DrawContext ctx, int nameX, int nameY, BattleScreen.ParticipantInfo p) {
      int statusX = nameX + 4;
      int totalWidth = 0;
      if (p.shieldTurns() > 0) {
         int width = this.renderStatusCard(ctx, statusX, nameY, "Shield", p.shieldTurns(), -7829249);
         statusX += width + 3;
         totalWidth += width + 3;
      }

      if (p.attackBoostTurns() > 0) {
         int width = this.renderStatusCard(ctx, statusX, nameY, "Boost", p.attackBoostTurns(), -30652);
         statusX += width + 3;
         totalWidth += width + 3;
      }

      if (p.isFrozen()) {
         int width = this.renderStatusCard(ctx, statusX, nameY, "Frozen", 0, -12263937);
         statusX += width + 3;
         totalWidth += width + 3;
      }

      if (p.isBurned()) {
         int width = this.renderStatusCard(ctx, statusX, nameY, "Burned", 0, -39373);
         statusX += width + 3;
         totalWidth += width + 3;
      }

      if (p.isAsleep()) {
         int width = this.renderStatusCard(ctx, statusX, nameY, "Asleep", 0, -6715188);
         statusX += width + 3;
         totalWidth += width + 3;
      }

      if (p.isConfused()) {
         int width = this.renderStatusCard(ctx, statusX, nameY, "Confused", 0, -47958);
         totalWidth += width + 3;
      }

      return totalWidth;
   }

   private void renderInlineSkillsMenu(DrawContext ctx, int tbX, int tbY, int tbW, int tbH) {
      int cols = 2;
      int rows = 3;
      int totalSkills = this.availableSkills.size();
      int leftW = (int)((float)tbW * 0.55F);
      int rightX = tbX + leftW + 4;
      int rightW = tbW - leftW - 8;
      int colW = (leftW - 8) / cols;
      int soulPad = 13;
      int leftPad = 4 + soulPad;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("SKILLS").formatted(Formatting.AQUA), tbX + 4, tbY + 2, -7829249);
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal("[SHIFT] Back").formatted(Formatting.DARK_GRAY),
         tbX + tbW - this.textRenderer.getWidth("[SHIFT] Back") - 4,
         tbY + 2,
         -12303275
      );
      int gridY = tbY + 14;
      int selRow = this.skillNavIdx / cols;
      int startRow = selRow / rows * rows;
      int startIdx = startRow * cols;

      for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
            int idx = startIdx + r * cols + c;
            if (idx < totalSkills) {
               BattleSkill s = this.availableSkills.get(idx);
               boolean sel = idx == this.skillNavIdx;
               boolean resonator = !this.pvpMode && this.currentActorGimmick() == OffhandGimmick.SOUL_RESONATOR && this.playerClass == PlayerClass.SOUL_MAGE;
               boolean canAfford = resonator ? this.playerHp > (float)this.effectiveTpCost(s) : this.activeTp() >= (float)this.effectiveTpCost(s);
               boolean spareBlocked = (s.getType() == SkillType.SPARE || s.getType() == SkillType.PACIFY) && TurnBattleClientMod.isRuthless;
               int cx = tbX + leftPad + c * colW;
               int cy = gridY + r * 22;
               int nameCol = canAfford && !spareBlocked ? (sel ? -1 : -4473874) : -11184777;
               if (sel) {
                  Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
                  int soulX = cx - 9 - 4;
                  int soulY = cy + (int)(9.0F * 1.3F - 9.0F) / 2;
                  if (sTex != null) {
                     ctx.drawTexture(sTex, soulX, soulY, 0.0F, 0.0F, 9, 9, 9, 9);
                  }
               }

               String name = this.skillMenuDisplayName(s);
               int maxNameW = (int)((float)(colW - 4) / 1.3F);

               while (name.length() > 1 && this.textRenderer.getWidth(name) > maxNameW) {
                  name = name.substring(0, name.length() - 1);
               }

               ctx.getMatrices().push();
               ctx.getMatrices().translate((float)cx, (float)cy, 0.0F);
               ctx.getMatrices().scale(1.3F, 1.3F, 1.0F);
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), 0, 0, nameCol);
               ctx.getMatrices().pop();
            }
         }
      }

      if (startRow > 0) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u25b2").formatted(Formatting.GRAY), tbX + leftW - 6, gridY, -10066296);
      }

      if ((startRow + rows) * cols < totalSkills) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u25bc").formatted(Formatting.GRAY), tbX + leftW - 6, gridY + rows * 22 - 8, -10066296);
      }

      if (this.skillNavIdx >= 0 && this.skillNavIdx < totalSkills) {
         BattleSkill sel2 = this.availableSkills.get(this.skillNavIdx);
         boolean resonator2 = !this.pvpMode && this.currentActorGimmick() == OffhandGimmick.SOUL_RESONATOR && this.playerClass == PlayerClass.SOUL_MAGE;
         String desc = sel2.getLiveDescription(this.currentActorPower(sel2), this.currentActorStab(sel2), this.currentActorFrozen());
         int lineH = 9 + 2;
         int descY = gridY;
         if (desc != null && !desc.isEmpty()) {
            List descLines = new ArrayList<>();
            this.wrapText(desc, rightW, descLines);

            for (String line : descLines) {
               if (descY > tbY + tbH - lineH) {
                  break;
               }

               ctx.drawTextWithShadow(this.textRenderer, Text.literal(line), rightX, descY, -5592406);
               descY += lineH;
            }
         }

         String tpStr = this.effectiveTpCost(sel2) + (resonator2 ? " HP" : " TP");
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(tpStr), rightX, descY + 2, -21965);
      }
   }

   private void renderInlineGoodsMenu(DrawContext ctx, int tbX, int tbY, int tbW, int tbH) {
      int cols = 3;
      int pad = 4;
      int gap = 2;
      int gridW = tbW - pad * 2;
      int cardW = (gridW - gap * (cols - 1)) / cols;
      int cardH = 22;
      int visRows = 3;
      int startRow = this.goodsNavIdx / cols * (cardH + gap) / (cardH + gap);
      startRow = this.goodsNavIdx / cols;
      if (startRow > 0 && this.goodsNavIdx / cols < startRow + visRows) {
         startRow = this.goodsNavIdx / cols - (visRows - 1);
      }

      startRow = Math.max(0, this.goodsNavIdx / cols - (visRows - 1));
      int navRow2 = this.goodsNavIdx / cols;
      if (navRow2 >= startRow + visRows) {
         startRow = navRow2 - visRows + 1;
      }

      if (navRow2 < startRow) {
         startRow = navRow2;
      }

      startRow = Math.max(0, startRow);
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("ITEMS").formatted(Formatting.GREEN), tbX + pad, tbY + 2, -12272794);
      int gridY = tbY + 12;
      if (this.foodItems.isEmpty()) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("No food items!").formatted(Formatting.GRAY), tbX + tbW / 2, tbY + tbH / 2 - 4, -7829368
         );
      } else {
         int totalItems = this.foodItems.size();
         int endRow = startRow + visRows;

         for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
               int idx = row * cols + col;
               if (idx < totalItems) {
                  BattleScreen.FoodEntry f = this.foodItems.get(idx);
                  int cx = tbX + pad + col * (cardW + gap);
                  int cy = gridY + (row - startRow) * (cardH + gap);
                  boolean sel = idx == this.goodsNavIdx;
                  int bg = sel ? -871749094 : (idx % 2 == 0 ? -2012602352 : 1426922000);
                  ctx.fill(cx, cy, cx + cardW, cy + cardH, bg);
                  int nameX = cx + 3;
                  if (sel) {
                     Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
                     int soulX = cx + 2;
                     int soulY = cy + (cardH - 9) / 2;
                     if (sTex != null) {
                        ctx.drawTexture(sTex, soulX, soulY, 0.0F, 0.0F, 9, 9, 9, 9);
                     }

                     nameX = soulX + 9 + 3;
                  }

                  String name = f.name() + (f.count() > 1 ? " x" + f.count() : "");
                  int maxNW = cardW - 34 - (sel ? 12 : 0);

                  while (name.length() > 1 && this.textRenderer.getWidth(name) > maxNW) {
                     name = name.substring(0, name.length() - 1);
                  }

                  int nc = sel ? -1 : -5579333;
                  ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), nameX, cy + 3, nc);
                  boolean inlFav = TurnBattleClientMod.clientFavoriteFood.contains(f.itemId());
                  int displayHpInl = inlFav ? f.hp() * 2 : f.hp();
                  String hpStr = "+" + displayHpInl + (inlFav ? " \u2605" : "");
                  int hpW = this.textRenderer.getWidth(hpStr);
                  ctx.fill(cx + cardW - hpW - 5, cy + 2, cx + cardW - 2, cy + 12, sel ? (inlFav ? -11193600 : -15051469) : (inlFav ? -13426176 : -15654383));
                  ctx.drawTextWithShadow(
                     this.textRenderer, Text.literal(hpStr).formatted(inlFav ? Formatting.GOLD : Formatting.GREEN), cx + cardW - hpW - 4, cy + 3, 16777215
                  );
                  ctx.fill(cx + 1, cy + cardH - 1, cx + cardW - 1, cy + cardH, 574947208);
               }
            }
         }

         if (startRow > 0) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u25b2").formatted(Formatting.GRAY), tbX + tbW - 8, gridY, -11180476);
         }

         if (endRow * cols < totalItems) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("\u25bc").formatted(Formatting.GRAY), tbX + tbW - 8, gridY + visRows * (cardH + gap) - 8, -11180476
            );
         }
      }

      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal("[SHIFT] Back").formatted(Formatting.DARK_GRAY),
         tbX + tbW - this.textRenderer.getWidth("[SHIFT] Back") - 4,
         tbY + 2,
         -13417421
      );
   }

   private List<String> wrapDesc(String desc, int maxW) {
      List lines = new ArrayList<>();
      if (desc != null && !desc.isEmpty()) {
         String[] words = desc.split(" ");
         StringBuilder cur = new StringBuilder();

         for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (this.textRenderer.getWidth(test) > maxW && cur.length() > 0) {
               lines.add(cur.toString());
               cur = new StringBuilder(w);
            } else {
               cur = new StringBuilder(test);
            }
         }

         if (cur.length() > 0) {
            lines.add(cur.toString());
         }

         return lines;
      } else {
         return lines;
      }
   }

   private void renderSkillsMenu(DrawContext ctx, int mx, int my) {
      int px = this.width / 2 - 115;
      int py = this.height / 2 - 105;
      int pw = 230;
      int ph = 210;
      ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 1722460927);
      ctx.fill(px, py, px + pw, py + ph, -268106480);
      ctx.fill(px, py, px + pw, py + 3, -10066177);
      ctx.fill(px, py + ph - 3, px + pw, py + ph, -12303224);
      ctx.fill(px, py + 3, px + 2, py + ph - 3, -13417336);
      ctx.fill(px + pw - 6 - 3, py + 3, px + pw - 6 - 2, py + ph - 3, -14540203);
      int listX = px + 4;
      int listW = pw - 6 - 10;
      int listY = py + 4;
      int listH = 170;
      int[] cardHeights = new int[this.availableSkills.size()];
      int hovIdx = -1;
      int cy = listY - this.skillScrollOffset;

      for (int i = 0; i < this.availableSkills.size(); i++) {
         int h = 34;
         int bot = cy + h;
         if (mx >= listX && mx <= listX + listW && my >= Math.max(cy, listY) && my <= Math.min(bot, listY + listH)) {
            hovIdx = i;
         }

         cardHeights[i] = h;
         cy += h;
      }

      if (hovIdx < 0 && this.phase == BattleScreen.Phase.SKILLS_MENU && this.skillNavIdx >= 0 && this.skillNavIdx < this.availableSkills.size()) {
         hovIdx = this.skillNavIdx;
      }

      if (hovIdx >= 0) {
         BattleSkill hs = this.availableSkills.get(hovIdx);
         List dl = this.wrapDesc(hs.getLiveDescription(this.currentActorPower(hs), this.currentActorStab(hs), this.currentActorFrozen()), listW - 12);
         if (!dl.isEmpty()) {
            cardHeights[hovIdx] = 34 + dl.size() * 10 + 4;
         }
      }

      cy = 0;

      for (int h : cardHeights) {
         cy += h;
      }

      int maxScroll = Math.max(0, cy - listH);
      this.skillScrollOffset = Math.max(0, Math.min(this.skillScrollOffset, maxScroll));
      int cyx = listY - this.skillScrollOffset;

      for (int i = 0; i < this.availableSkills.size(); i++) {
         BattleSkill s = this.availableSkills.get(i);
         int cardH = cardHeights[i];
         int cardTop = cyx;
         int cardBot = cyx + cardH - 2;
         cyx += cardH;
         if (cardBot >= listY && cardTop <= listY + listH) {
            boolean hover = i == hovIdx;
            boolean isSpare = s.getType() == SkillType.SPARE || s.getType() == SkillType.PACIFY;
            boolean blocked = isSpare && TurnBattleClientMod.isRuthless;
            boolean resonator = !this.pvpMode && this.currentActorGimmick() == OffhandGimmick.SOUL_RESONATOR && this.playerClass == PlayerClass.SOUL_MAGE;
            boolean canAfford;
            String tpStr;
            if (resonator) {
               canAfford = !blocked && this.playerHp > (float)this.effectiveTpCost(s);
               tpStr = this.effectiveTpCost(s) + " HP";
            } else {
               canAfford = !blocked && this.activeTp() >= (float)this.effectiveTpCost(s);
               tpStr = this.effectiveTpCost(s) + " TP";
            }

            int visTop = Math.max(cardTop, listY);
            int visBot = Math.min(cardBot, listY + listH);
            int cardBg = hover ? -870704555 : (i % 2 == 0 ? -2012607952 : 1426918696);
            ctx.fill(listX, visTop, listX + listW, visBot, cardBg);
            if (hover) {
               int var10000;
               label234: {
                  if (s instanceof SoulMageSkill sms && sms.getSoul() != null) {
                     var10000 = sms.getSoul().getColor() | 0xFF000000;
                     break label234;
                  }

                  var10000 = -7829249;
               }

               int accentBar = var10000;
               ctx.fill(listX, visTop, listX + 2, visBot, accentBar);
            }

            if (cardTop >= listY - 4 && cardTop < listY + listH) {
               int nameCol;
               if (!canAfford) {
                  nameCol = -10066296;
               } else {
                  label288: {
                     if (s instanceof SoulMageSkill) {
                        SoulMageSkill sms = (SoulMageSkill)s;
                        if (sms.getSoul() != null) {
                           int sc = sms.getSoul().getColor() | 0xFF000000;
                           nameCol = hover ? -1 : sc;
                           break label288;
                        }
                     }

                     nameCol = hover ? -1 : -4473874;
                  }
               }

               ctx.drawTextWithShadow(this.textRenderer, Text.literal(this.skillMenuDisplayName(s)), listX + 6, cardTop + 4, nameCol);
               int tpW = this.textRenderer.getWidth(tpStr);
               int badgeX = listX + listW - tpW - 8;
               ctx.fill(badgeX - 2, cardTop + 2, badgeX + tpW + 3, cardTop + 12, canAfford ? -12298804 : -13426108);
               ctx.drawTextWithShadow(
                  this.textRenderer, Text.literal(tpStr).formatted(canAfford ? Formatting.AQUA : Formatting.DARK_GRAY), badgeX, cardTop + 3, 16777215
               );
               if (hover) {
                  List dl = this.wrapDesc(s.getLiveDescription(this.currentActorPower(s), this.currentActorStab(s), this.currentActorFrozen()), listW - 12);
                  int descCol = canAfford ? -3355393 : -10066296;

                  for (int li = 0; li < dl.size(); li++) {
                     int ly = cardTop + 34 - 10 + li * 10;
                     if (ly + 8 > listY + listH) {
                        break;
                     }

                     ctx.drawTextWithShadow(this.textRenderer, Text.literal(dl.get(li)), listX + 8, ly, descCol);
                  }
               } else {
                  String desc = s.getLiveDescription(this.currentActorPower(s), this.currentActorStab(s), this.currentActorFrozen());
                  if (desc != null && !desc.isEmpty()) {
                     String t = desc;
                     int maxW = listW - 10;

                     while (t.length() > 0 && this.textRenderer.getWidth(t + "\u2026") > maxW) {
                        t = t.substring(0, t.length() - 1);
                     }

                     if (t.length() < desc.length()) {
                        t = t + "\u2026";
                     }

                     ctx.drawTextWithShadow(this.textRenderer, Text.literal(t), listX + 6, cardTop + 15, canAfford ? -8947798 : -12303258);
                  }
               }

               if (cardBot < listY + listH) {
                  ctx.fill(listX + 2, cardBot, listX + listW - 2, cardBot + 1, 866822911);
               }
            }
         }
      }

      if (cy > listH) {
         int barX = px + pw - 6 - 1;
         ctx.fill(barX, listY, barX + 6, listY + listH, 1143087684);
         int thumbH = Math.max(20, listH * listH / cy);
         int thumbY = listY + (maxScroll > 0 ? (int)((long)(listH - thumbH) * (long)this.skillScrollOffset / (long)maxScroll) : 0);
         boolean barHover = mx >= barX && mx <= barX + 6 && my >= listY && my <= listY + listH;
         int thumbCol = this.draggingSkillBar ? -6710785 : (barHover ? -8947764 : -11184743);
         ctx.fill(barX + 1, thumbY, barX + 6 - 1, thumbY + thumbH, thumbCol);
         ctx.fill(barX + 1, thumbY, barX + 2, thumbY + thumbH, 1442840575);
      }

      int backY = py + ph - 16;
      boolean hBack = mx >= px + 4 && mx <= px + 54 && my >= backY && my <= backY + 12;
      if (hBack) {
         ctx.fill(px + 4, backY - 1, px + 54, backY + 12, 1157627903);
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u25c4 BACK"), px + 7, backY, hBack ? -86 : -5592406);
   }

   private void renderGoodsMenu(DrawContext ctx, int mx, int my) {
      int px = this.width / 2 - 115;
      int py = this.height / 2 - 105;
      int pw = 230;
      int ph = 210;
      ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 1716912025);
      ctx.fill(px, py, px + pw, py + ph, -268103675);
      ctx.fill(px, py, px + pw, py + 3, -12268442);
      ctx.fill(px, py + ph - 3, px + pw, py + ph, -14531533);
      ctx.fill(px, py + 3, px + 2, py + ph - 3, -14527181);
      ctx.fill(px + pw - 6 - 3, py + 3, px + pw - 6 - 2, py + ph - 3, -15654366);
      int listX = px + 4;
      int listW = pw - 6 - 10;
      int listY = py + 4;
      int listH = 170;
      if (this.foodItems.isEmpty()) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No food items!").formatted(Formatting.GRAY), px + pw / 2, py + ph / 2 - 4, 16777215);
      } else {
         int totalH = this.foodItems.size() * 28;
         int maxScroll = Math.max(0, totalH - listH);
         this.goodsScrollOffset = Math.max(0, Math.min(this.goodsScrollOffset, maxScroll));

         for (int i = 0; i < this.foodItems.size(); i++) {
            BattleScreen.FoodEntry f = this.foodItems.get(i);
            int cardTop = listY + i * 28 - this.goodsScrollOffset;
            int cardBot = cardTop + 28 - 2;
            if (cardBot >= listY && cardTop <= listY + listH) {
               int visTop = Math.max(cardTop, listY);
               int visBot = Math.min(cardBot, listY + listH);
               boolean mouseInList = mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH;
               boolean hover = mouseInList && mx >= listX && mx <= listX + listW && my >= visTop && my <= visBot || !mouseInList && i == this.goodsNavIdx;
               int cardBg = hover ? -871749094 : (i % 2 == 0 ? -2012602352 : 1426922000);
               boolean isFav = TurnBattleClientMod.clientFavoriteFood.contains(f.itemId());
               if (isFav) {
                  cardBg = hover ? -869654528 : -2011557376;
               }

               ctx.fill(listX, visTop, listX + listW, visBot, cardBg);
               if (hover) {
                  ctx.fill(listX, visTop, listX + 2, visBot, isFav ? -8960 : -12268442);
               }

               if (cardTop >= listY - 4 && cardTop < listY + listH) {
                  String namePrefix = isFav ? "\u2605 " : "";
                  int nameCol = isFav ? (hover ? -188 : -8960) : (hover ? -1 : -5579333);
                  ctx.drawTextWithShadow(this.textRenderer, Text.literal(namePrefix + f.name()), listX + 6, cardTop + 9, nameCol);
                  int displayHp = isFav ? f.hp() * 2 : f.hp();
                  String hpStr = "+" + displayHp + " HP" + (isFav ? " \u2605" : "");
                  int hpW = this.textRenderer.getWidth(hpStr);
                  int badgeX = listX + listW - hpW - 8;
                  ctx.fill(badgeX - 2, cardTop + 6, badgeX + hpW + 3, cardTop + 18, hover ? (isFav ? -11193600 : -15051469) : (isFav ? -13426176 : -15654383));
                  ctx.drawTextWithShadow(
                     this.textRenderer, Text.literal(hpStr).formatted(isFav ? Formatting.GOLD : Formatting.GREEN), badgeX, cardTop + 7, 16777215
                  );
                  if (cardBot < listY + listH) {
                     ctx.fill(listX + 2, cardBot, listX + listW - 2, cardBot + 1, 574947208);
                  }
               }
            }
         }

         if (totalH > listH) {
            int barX = px + pw - 6 - 1;
            ctx.fill(barX, listY, barX + 6, listY + listH, 1141973521);
            int thumbH = Math.max(20, listH * listH / totalH);
            int thumbY = listY + (maxScroll > 0 ? (int)((long)(listH - thumbH) * (long)this.goodsScrollOffset / (long)maxScroll) : 0);
            boolean barHover = mx >= barX && mx <= barX + 6 && my >= listY && my <= listY + listH;
            int thumbCol = this.draggingGoodsBar ? -10027111 : (barHover ? -12272794 : -13408700);
            ctx.fill(barX + 1, thumbY, barX + 6 - 1, thumbY + thumbH, thumbCol);
            ctx.fill(barX + 1, thumbY, barX + 2, thumbY + thumbH, 1442840575);
         }
      }

      int backY = py + ph - 16;
      boolean hBack = mx >= px + 4 && mx <= px + 54 && my >= backY && my <= backY + 12;
      if (hBack) {
         ctx.fill(px + 4, backY - 1, px + 54, backY + 12, 1157627903);
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u25c4 BACK"), px + 7, backY, hBack ? -86 : -5592406);
   }

   void renderGrazeRingAt(DrawContext ctx, float cx, float cy, float progress, float sizeMult) {
      this.renderGrazeRingScaled(ctx, cx, cy, 1.0F + progress * 0.25F, 1.0F - progress, sizeMult);
   }

   void renderGrazeRingScaled(DrawContext ctx, float cx, float cy, float scale, float alpha, float sizeMult) {
      float baseSize = 36.0F * sizeMult;
      int color = this.playerSoul != null ? this.playerSoul.getColor() : -1;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(r, g, b, alpha);
      MatrixStack ms = ctx.getMatrices();
      ms.push();
      ms.translate(cx, cy, 0.0F);
      ms.scale(scale, scale, 1.0F);
      ms.translate(-baseSize / 2.0F, -baseSize / 2.0F, 0.0F);
      int s = (int)baseSize;
      ctx.drawTexture(GRAZE_RING, 0, 0, 0.0F, 0.0F, s, s, s, s);
      ms.pop();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   private void renderPlayerGrazePulses(DrawContext ctx) {
      if (this.playerGrazeTriggerTick >= 0) {
         float[] anchor = this.playerSoulAnchor();
         float cx = anchor[0] + 4.5F;
         float cy = anchor[1] + 4.5F;

         for (int delay : GRAZE_PULSE_DELAYS) {
            int localTick = this.playerGrazeTriggerTick - delay;
            if (localTick >= 0 && !((float)localTick >= 3.3333333F)) {
               float progress = ((float)localTick + this.lastRenderDelta) / 3.3333333F;
               this.renderGrazeRingAt(ctx, cx, cy, progress, 0.75F);
            }
         }
      }
   }

   private void renderSoulLeave(DrawContext ctx, float delta) {
      if (this.soulLeaving) {
         float tickF = (float)this.soulLeaveTick + delta;
         float t = Math.min(1.0F, tickF / 4.6666665F);
         float sx = this.soulLeaveStartX + (this.soulLeaveTargetX - this.soulLeaveStartX) * t;
         float sy = this.soulLeaveStartY + (this.soulLeaveTargetY - this.soulLeaveStartY) * t;
         float alpha = Math.min(1.0F, tickF * 1.5F / 3.0F);
         Identifier soulTexture = this.playerSoul != null ? this.playerSoul.getTexture() : null;
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         this.drawSoulScaled(ctx, soulTexture, sx + 4.5F, sy + 4.5F, 1.0F, 1.0F, alpha);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   private boolean isUp(int key) {
      return matchesBind(TurnBattleClientMod.battleUp, key);
   }

   private boolean isDown(int key) {
      return matchesBind(TurnBattleClientMod.battleDown, key);
   }

   private boolean isLeft(int key) {
      return matchesBind(TurnBattleClientMod.battleLeft, key);
   }

   private boolean isRight(int key) {
      return matchesBind(TurnBattleClientMod.battleRight, key);
   }

   private boolean isConfirm(int key) {
      return matchesBind(TurnBattleClientMod.battleConfirm, key) || key == 335;
   }

   private boolean isBack(int key) {
      return matchesBind(TurnBattleClientMod.battleBack, key);
   }

   private boolean isScrollPartyUp(int key) {
      return matchesBind(TurnBattleClientMod.battleScrollPartyUp, key);
   }

   private boolean isScrollPartyDown(int key) {
      return matchesBind(TurnBattleClientMod.battleScrollPartyDown, key);
   }

   boolean shootHeld() {
      long win = MinecraftClient.getInstance().getWindow().getHandle();
      if (!InputUtil.isKeyPressed(win, 32) && !InputUtil.isKeyPressed(win, 257) && !InputUtil.isKeyPressed(win, 335)) {
         KeyBinding kb = TurnBattleClientMod.battleConfirm;
         if (kb == null) {
            return false;
         } else {
            Key bound;
            try {
               bound = KeyBindingHelper.getBoundKeyOf(kb);
            } catch (Exception var6) {
               bound = kb.getDefaultKey();
            }

            if (bound.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
               Integer def = TurnBattleClientMod.defaultBattleKeys.get(kb);
               return def != null && InputUtil.isKeyPressed(win, def);
            } else {
               return bound.getCategory() == Type.KEYSYM && InputUtil.isKeyPressed(win, bound.getCode());
            }
         }
      } else {
         return true;
      }
   }

   private static boolean matchesBind(KeyBinding kb, int glfwKey) {
      if (kb == null) {
         return false;
      } else {
         Key bound;
         try {
            bound = KeyBindingHelper.getBoundKeyOf(kb);
         } catch (Exception var4) {
            bound = kb.getDefaultKey();
         }

         if (bound.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
            Integer def = TurnBattleClientMod.defaultBattleKeys.get(kb);
            return def != null ? def == glfwKey : false;
         } else {
            return bound.getCategory() == Type.KEYSYM && bound.getCode() == glfwKey;
         }
      }
   }

   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 269) {
         this.forceExit();
         return true;
      } else if (!BossSpeech.active() || this.chatInputActive || !this.isConfirm(key) && !this.isBack(key)) {
         if (this.qteActive && !this.qteDone && this.qteIsMe && this.isConfirm(key)) {
            this.hitBashQte();
            return true;
         } else if (this.mbActive && !this.chatInputActive && this.isConfirm(key)) {
            this.hitMultiBash();
            return true;
         } else if (this.battleLineKey(key)) {
            return true;
         } else if (this.chatInputActive) {
            if (key == 256) {
               this.chatInputActive = false;
               this.chatInputText = "";
               return true;
            } else if (key != 257 && key != 335) {
               if (key == 259) {
                  if (!this.chatInputText.isEmpty()) {
                     this.chatInputText = this.chatInputText.substring(0, this.chatInputText.length() - 1);
                  }

                  return true;
               } else {
                  return true;
               }
            } else {
               if (!this.chatInputText.isBlank()) {
                  PacketByteBuf buf = PacketByteBufs.create();
                  buf.writeString(this.chatInputText.trim(), 256);
                  Net121.sendToServer(TurnBattleMod.BATTLE_CHAT, buf);
               }

               this.chatInputActive = false;
               this.chatInputText = "";
               return true;
            }
         } else if (key == 84
            && this.phase != BattleScreen.Phase.ENEMY_TURN
            && this.phase != BattleScreen.Phase.ENEMY_TURN_END
            && this.phase != BattleScreen.Phase.RESULT
            && this.phase != BattleScreen.Phase.BATTLE_INTRO) {
            this.chatInputActive = true;
            this.chatInputText = "";
            this.suppressNextChar = true;
            return true;
         } else if (this.phase == BattleScreen.Phase.BATON_PASS) {
            if (this.isUp(key)) {
               this.batonCursor = Math.max(0, this.batonCursor - 1);
               this.playMenuMove();
               return true;
            } else if (this.isDown(key)) {
               this.batonCursor = Math.min(this.batonOptions.size() - 1, this.batonCursor + 1);
               this.playMenuMove();
               return true;
            } else if (this.isConfirm(key)) {
               this.playMenuSelect();
               this.submitBatonPassChoice();
               return true;
            } else {
               return true;
            }
         } else if (this.phase == BattleScreen.Phase.ALL_OUT) {
            if (this.isLeft(key)) {
               this.allOutCursor = 0;
               this.playMenuMove();
               return true;
            } else if (this.isRight(key)) {
               this.allOutCursor = 1;
               this.playMenuMove();
               return true;
            } else if (!this.isUp(key) && !this.isConfirm(key)) {
               return true;
            } else {
               this.playMenuSelect();
               this.submitAllOutVote(this.allOutCursor);
               return true;
            }
         } else if ((this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY || this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY) && this.isBack(key)) {
            this.setPhase(BattleScreen.Phase.PLAYER_TURN);
            return true;
         } else {
            if (this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY) {
               Object aliveIdx;
               if (this.pvpMode) {
                  aliveIdx = new ArrayList<>();

                  for (int i = 0; i < this.pvpOpponents.size(); i++) {
                     if (!this.pvpOpponents.get(i).downed()) {
                        aliveIdx.add(i);
                     }
                  }
               } else {
                  aliveIdx = this.livingEnemyIndices();
               }

               boolean tPrev = this.overhaul ? this.isLeft(key) : this.isUp(key);
               boolean tNext = this.overhaul ? this.isRight(key) : this.isDown(key);
               if (tPrev || tNext) {
                  int pos = aliveIdx.indexOf(this.targetNavIdx);
                  if (pos < 0) {
                     pos = 0;
                  }

                  if (tPrev) {
                     pos = Math.max(0, pos - 1);
                  } else {
                     pos = Math.min(Math.max(0, aliveIdx.size() - 1), pos + 1);
                  }

                  if (!aliveIdx.isEmpty()) {
                     this.targetNavIdx = aliveIdx.get(pos);
                  }

                  this.playMenuMove();
                  return true;
               }

               if (this.isConfirm(key)) {
                  this.playMenuSelect();
                  if (this.pendingAction == 1) {
                     this.startSkillEffect(this.pendingAction, this.pendingParam, this.targetNavIdx);
                  } else if (this.pendingAction == 0) {
                     this.requestBashQte(this.targetNavIdx);
                  } else {
                     if (this.pendingAction == 5) {
                        this.sendAction(5, 0, this.targetNavIdx);
                        return true;
                     }

                     if (this.pendingAction == 98) {
                        this.openActMenuForTarget(this.targetNavIdx);
                        return true;
                     }

                     if (this.pendingAction == 99) {
                        this.sendActPerform(this.pendingParam, this.targetNavIdx);
                        return true;
                     }

                     this.sendAction(this.pendingAction, this.pendingParam, this.targetNavIdx);
                  }

                  return true;
               }
            }

            if (this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY) {
               int totalAllies = this.participants.size();
               boolean aPrev = this.overhaul ? this.isLeft(key) : this.isUp(key);
               boolean aNext = this.overhaul ? this.isRight(key) : this.isDown(key);
               if (aPrev) {
                  this.targetNavIdx = Math.max(0, this.targetNavIdx - 1);
                  if (this.targetNavIdx < this.allyScrollOffset) {
                     this.allyScrollOffset = this.targetNavIdx;
                  }

                  this.playMenuMove();
                  return true;
               }

               if (aNext) {
                  this.targetNavIdx = Math.min(totalAllies - 1, this.targetNavIdx + 1);
                  if (this.targetNavIdx > this.allyScrollOffset + 2) {
                     this.allyScrollOffset = this.targetNavIdx - 2;
                  }

                  this.playMenuMove();
                  return true;
               }

               if (this.isConfirm(key)) {
                  this.playMenuSelect();
                  int encodedTarget = this.targetNavIdx + 10;
                  if (this.pendingAction == 1) {
                     this.startSkillEffect(this.pendingAction, this.pendingParam, encodedTarget);
                  } else if (this.pendingAction == 2) {
                     this.sendAction(2, this.pendingGoodsSlot, encodedTarget);
                     this.pendingGoodsSlot = -1;
                     this.setPhase(BattleScreen.Phase.PLAYER_TURN);
                  } else {
                     if (this.pendingAction == 5) {
                        this.sendAction(5, 0, this.targetNavIdx);
                        return true;
                     }

                     if (this.pendingAction == 98) {
                        this.openActMenuForTarget(this.targetNavIdx);
                        return true;
                     }

                     if (this.pendingAction == 99) {
                        this.sendActPerform(this.pendingParam, this.targetNavIdx);
                        return true;
                     }

                     this.sendAction(this.pendingAction, this.pendingParam, encodedTarget);
                  }

                  return true;
               }
            }

            if (this.phase == BattleScreen.Phase.PLAYER_TURN) {
               if (this.mercySubmenuOpen) {
                  if (this.isUp(key)) {
                     this.mercySubmenuIdx = Math.max(0, this.mercySubmenuIdx - 1);
                     this.playMenuMove();
                     return true;
                  }

                  if (this.isDown(key)) {
                     this.mercySubmenuIdx = Math.min(1, this.mercySubmenuIdx + 1);
                     this.playMenuMove();
                     return true;
                  }

                  if (this.isConfirm(key)) {
                     this.playMenuSelect();
                     this.activateMercySubmenu();
                     return true;
                  }

                  if (this.isBack(key)) {
                     this.mercySubmenuOpen = false;
                     return true;
                  }

                  return true;
               }

               if (this.skillsSubmenuOpen) {
                  if (this.isUp(key)) {
                     this.skillsSubmenuIdx = Math.max(0, this.skillsSubmenuIdx - 1);
                     this.playMenuMove();
                     return true;
                  }

                  if (this.isDown(key)) {
                     this.skillsSubmenuIdx = Math.min(1, this.skillsSubmenuIdx + 1);
                     this.playMenuMove();
                     return true;
                  }

                  if (this.isConfirm(key)) {
                     this.playMenuSelect();
                     this.activateSkillsSubmenu();
                     return true;
                  }

                  if (this.isBack(key)) {
                     this.skillsSubmenuOpen = false;
                     return true;
                  }

                  return true;
               }

               if (this.eb()) {
                  if (this.isUp(key)) {
                     this.drNavIdx = (this.drNavIdx + EB_CMD_ORDER.length - 1) % EB_CMD_ORDER.length;
                     this.playMenuMove();
                     return true;
                  }

                  if (this.isDown(key)) {
                     this.drNavIdx = (this.drNavIdx + 1) % EB_CMD_ORDER.length;
                     this.playMenuMove();
                     return true;
                  }
               }

               if (this.isLeft(key)) {
                  this.drNavIdx = Math.max(0, this.drNavIdx - 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isRight(key)) {
                  this.drNavIdx = Math.min(4, this.drNavIdx + 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isConfirm(key)) {
                  this.playMenuSelect();
                  this.activateDeltaruneButton();
                  return true;
               }

               if (this.participants.size() > 3) {
                  if (this.isScrollPartyUp(key)) {
                     this.drScrollOffset = Math.max(0, this.drScrollOffset - 1);
                     this.playMenuMove();
                     return true;
                  }

                  if (this.isScrollPartyDown(key)) {
                     this.drScrollOffset = Math.min(this.participants.size() - 3, this.drScrollOffset + 1);
                     this.playMenuMove();
                     return true;
                  }
               }
            }

            if (this.phase == BattleScreen.Phase.SKILLS_MENU) {
               if (this.isBack(key)) {
                  this.setPhase(BattleScreen.Phase.PLAYER_TURN);
                  return true;
               }

               int skillCount = this.availableSkills.size();
               if (this.isUp(key) && skillCount > 0) {
                  this.skillNavIdx = Math.max(0, this.skillNavIdx - 2);
                  this.scrollToNavSkill();
                  this.playMenuMove();
                  return true;
               }

               if (this.isDown(key) && skillCount > 0) {
                  this.skillNavIdx = Math.min(skillCount - 1, this.skillNavIdx + 2);
                  this.scrollToNavSkill();
                  this.playMenuMove();
                  return true;
               }

               if (this.isLeft(key) && skillCount > 0) {
                  this.skillNavIdx = Math.max(0, this.skillNavIdx - 1);
                  this.scrollToNavSkill();
                  this.playMenuMove();
                  return true;
               }

               if (this.isRight(key) && skillCount > 0) {
                  this.skillNavIdx = Math.min(skillCount - 1, this.skillNavIdx + 1);
                  this.scrollToNavSkill();
                  this.playMenuMove();
                  return true;
               }

               if (this.isConfirm(key) && skillCount > 0) {
                  this.playMenuSelect();
                  this.activateNavSkill();
                  return true;
               }
            }

            if (this.phase == BattleScreen.Phase.ACT_MENU) {
               if (this.isBack(key)) {
                  this.setPhase(BattleScreen.Phase.PLAYER_TURN);
                  return true;
               }

               int total = this.actList.size();
               int cols = 3;
               int rows = (total + cols - 1) / cols;
               if (this.isUp(key)) {
                  this.actNavRow = Math.max(0, this.actNavRow - 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isDown(key)) {
                  this.actNavRow = Math.min(rows - 1, this.actNavRow + 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isLeft(key)) {
                  this.actNavCol = Math.max(0, this.actNavCol - 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isRight(key)) {
                  this.actNavCol = Math.min(cols - 1, this.actNavCol + 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isConfirm(key)) {
                  int idx = this.actNavRow * cols + this.actNavCol;
                  if (idx < total) {
                     this.playMenuSelect();
                     this.confirmActSelection(idx);
                     return true;
                  }
               }
            }

            if (this.phase == BattleScreen.Phase.GOODS_MENU) {
               if (this.isBack(key)) {
                  this.setPhase(BattleScreen.Phase.PLAYER_TURN);
                  return true;
               }

               int goodsCount = this.foodItems.size();
               if (this.isUp(key) && goodsCount > 0) {
                  this.goodsNavIdx = Math.max(0, this.goodsNavIdx - 3);
                  this.playMenuMove();
                  return true;
               }

               if (this.isDown(key) && goodsCount > 0) {
                  this.goodsNavIdx = Math.min(goodsCount - 1, this.goodsNavIdx + 3);
                  this.playMenuMove();
                  return true;
               }

               if (this.isLeft(key) && goodsCount > 0) {
                  this.goodsNavIdx = Math.max(0, this.goodsNavIdx - 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isRight(key) && goodsCount > 0) {
                  this.goodsNavIdx = Math.min(goodsCount - 1, this.goodsNavIdx + 1);
                  this.playMenuMove();
                  return true;
               }

               if (this.isConfirm(key) && goodsCount > 0) {
                  this.playMenuSelect();
                  this.activateNavGoods();
                  return true;
               }
            }

            if (this.phase == BattleScreen.Phase.ENEMY_TURN) {
               if (this.isUp(key)) {
                  this.keyW = true;
                  return true;
               }

               if (this.isLeft(key)) {
                  this.keyA = true;
                  return true;
               }

               if (this.isDown(key)) {
                  this.keyS = true;
                  return true;
               }

               if (this.isRight(key)) {
                  this.keyD = true;
                  return true;
               }

               if ((key == 32 || this.isConfirm(key)) && this.activeGimmick == BattleScreen.GimmickType.LIGHT_SHOOTER) {
                  if (!this.spacePressed) {
                     this.spacePressed = true;
                     this.bulletHell.shootPlayerBullet();
                  }

                  return true;
               }

               if (this.isBack(key)) {
                  this.keySneak = true;
                  this.activateOffhandGimmick();
                  return true;
               }
            }

            return super.keyPressed(key, scan, mods);
         }
      } else {
         BossSpeech.press();
         return true;
      }
   }

   private void activateNavButton() {
      int idx = this.navRow * 3 + this.navCol;
      this.activateCard(idx);
   }

   private void activateDeltaruneButton() {
      switch (this.drNavIdx) {
         case 0:
            this.onBashOrSkillAction(0, 0);
            break;
         case 1:
            boolean isTactician = !this.pvpMode && !this.participants.isEmpty() && this.participants.get(0).isMe() && this.realPlayerClass == null;
            if (isTactician) {
               this.skillsSubmenuOpen = true;
               this.skillsSubmenuIdx = 0;
            } else {
               this.openSkillsMenu();
            }
            break;
         case 2:
            this.openGoodsMenu();
            break;
         case 3:
            this.mercySubmenuOpen = true;
            this.mercySubmenuIdx = 0;
            break;
         case 4:
            this.sendAction(3, 0);
      }
   }

   private void activateMercySubmenu() {
      this.mercySubmenuOpen = false;
      if (this.mercySubmenuIdx == 0) {
         if (!TurnBattleClientMod.isRuthless && !this.pvpMode) {
            this.pendingAction = 5;
            this.pendingParam = 0;
            this.setPhase(BattleScreen.Phase.TARGET_SELECT_ENEMY);
         }
      } else {
         this.sendAction(4, 0);
      }
   }

   private static int encodeSkillParam(BattleSkill s) {
      if (s instanceof HumanMageSkill) {
         return 1000 + s.getOrdinal();
      } else if (s instanceof MonsterSkill) {
         return 2000 + s.getOrdinal();
      } else if (s instanceof LightnerSkill) {
         return 3000 + s.getOrdinal();
      } else if (s instanceof SoulMageSkill) {
         return 4000 + s.getOrdinal();
      } else {
         return s instanceof OriSkill ? 5000 + s.getOrdinal() : s.getOrdinal();
      }
   }

   static BattleSkill decodeSkillParam(int ordinal) {
      if (ordinal >= 5000 && ordinal < 6000) {
         OriSkill[] v = OriSkill.values();
         int i = ordinal - 5000;
         return i < v.length ? v[i] : null;
      } else if (ordinal >= 4000 && ordinal < 5000) {
         SoulMageSkill[] v = SoulMageSkill.values();
         int i = ordinal - 4000;
         return i < v.length ? v[i] : null;
      } else if (ordinal >= 3000 && ordinal < 4000) {
         LightnerSkill[] v = LightnerSkill.values();
         int i = ordinal - 3000;
         return i < v.length ? v[i] : null;
      } else if (ordinal >= 2000 && ordinal < 3000) {
         MonsterSkill[] v = MonsterSkill.values();
         int i = ordinal - 2000;
         return i < v.length ? v[i] : null;
      } else if (ordinal >= 1000 && ordinal < 2000) {
         HumanMageSkill[] v = HumanMageSkill.values();
         int i = ordinal - 1000;
         return i < v.length ? v[i] : null;
      } else {
         PsiSkill[] v = PsiSkill.values();
         return ordinal >= 0 && ordinal < v.length ? v[ordinal] : null;
      }
   }

   private void activateNavSkill() {
      if (this.skillNavIdx >= 0 && this.skillNavIdx < this.availableSkills.size()) {
         BattleSkill s = this.availableSkills.get(this.skillNavIdx);
         boolean resonator = !this.pvpMode && this.currentActorGimmick() == OffhandGimmick.SOUL_RESONATOR && this.playerClass == PlayerClass.SOUL_MAGE;
         boolean canAfford = resonator ? this.playerHp > (float)this.effectiveTpCost(s) : this.activeTp() >= (float)this.effectiveTpCost(s);
         if (canAfford) {
            if (s.getType() != SkillType.SPARE && s.getType() != SkillType.PACIFY || !TurnBattleClientMod.isRuthless) {
               int skillParam = this.playerClass == null ? this.skillNavIdx : encodeSkillParam(s);
               this.onBashOrSkillAction(1, skillParam);
            }
         }
      }
   }

   private void activateNavGoods() {
      if (this.goodsNavIdx >= 0 && this.goodsNavIdx < this.foodItems.size()) {
         BattleScreen.FoodEntry f = this.foodItems.get(this.goodsNavIdx);
         if (this.participants.size() > 1) {
            this.pendingAction = 2;
            this.pendingGoodsSlot = f.slot();
            this.setPhase(BattleScreen.Phase.TARGET_SELECT_ALLY);
         } else {
            this.sendAction(2, f.slot(), 0);
            this.setPhase(BattleScreen.Phase.PLAYER_TURN);
         }
      }
   }

   private void scrollToNavSkill() {
      this.scrollSkillNavIntoView();
   }

   private void scrollSkillNavIntoView() {
      int py = this.height / 2 - 105;
      int listY = py + 4;
      int listH = 170;
      int top = this.skillNavIdx * 34;
      int bot = top + 34;
      if (top < this.skillScrollOffset) {
         this.skillScrollOffset = top;
      } else if (bot > this.skillScrollOffset + listH) {
         this.skillScrollOffset = bot - listH;
      }

      this.skillScrollOffset = Math.max(0, this.skillScrollOffset);
   }

   private void scrollGoodsNavIntoView() {
      int listH = 170;
      int top = this.goodsNavIdx * 28;
      int bot = top + 28;
      if (top < this.goodsScrollOffset) {
         this.goodsScrollOffset = top;
      } else if (bot > this.goodsScrollOffset + listH) {
         this.goodsScrollOffset = bot - listH;
      }

      this.goodsScrollOffset = Math.max(0, this.goodsScrollOffset);
   }

   public boolean keyReleased(int key, int scan, int mods) {
      if (this.isUp(key)) {
         this.keyW = false;
         return true;
      } else if (this.isLeft(key)) {
         this.keyA = false;
         return true;
      } else if (this.isDown(key)) {
         this.keyS = false;
         return true;
      } else if (this.isRight(key)) {
         this.keyD = false;
         return true;
      } else if (this.isBack(key)) {
         this.keySneak = false;
         return true;
      } else if (key != 32 && !this.isConfirm(key)) {
         return super.keyReleased(key, scan, mods);
      } else {
         this.spacePressed = false;
         return true;
      }
   }

   public boolean charTyped(char chr, int mods) {
      if (this.suppressNextChar) {
         this.suppressNextChar = false;
         return true;
      } else if (this.chatInputActive && this.chatInputText.length() < 200 && chr >= ' ') {
         this.chatInputText = this.chatInputText + chr;
         return true;
      } else {
         return super.charTyped(chr, mods);
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (this.lastRenderScale < 1.0F) {
         mx = (mx - (double)this.width / 2.0) / (double)this.lastRenderScale + (double)this.width / 2.0;
         my = (my - (double)this.height / 2.0) / (double)this.lastRenderScale + (double)this.height / 2.0;
      }

      return super.mouseClicked(mx, my, btn);
   }

   public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
      return true;
   }

   public boolean mouseReleased(double mx, double my, int btn) {
      this.draggingSkillBar = false;
      return true;
   }

   private boolean handleEnemyTargetClick(int mx, int my) {
      int textBoxY = this.tbTop();
      int tbH = this.tbH();
      int textBoxX = this.tbX();
      int textBoxW = this.tbW();
      int total = 1 + this.extraEnemies.size();
      int spacing = 8;
      int barW = Math.min(150, (textBoxW - spacing * (total - 1)) / total);
      int totalBarW = total * barW + (total - 1) * spacing;
      int startX = textBoxX + (textBoxW - totalBarW) / 2;
      int btnH = 20;
      int btnY = textBoxY + tbH / 2 - btnH / 2;

      for (int i = 0; i < total; i++) {
         int bx = startX + i * (barW + spacing);
         if (mx >= bx && mx < bx + barW && my >= btnY && my < btnY + btnH) {
            if (this.pendingAction == 1) {
               this.startSkillEffect(this.pendingAction, this.pendingParam, i);
            } else if (this.pendingAction == 0) {
               this.requestBashQte(i);
            } else {
               this.sendAction(this.pendingAction, this.pendingParam, i);
            }

            return true;
         }
      }

      return true;
   }

   private boolean handleAllyTargetClick(int mx, int my) {
      int textBoxY = this.tbTop();
      int tbH = this.tbH();
      int textBoxX = this.tbX();
      int textBoxW = this.tbW();
      int btnW = 100;
      int btnH = 18;
      int space = 8;
      int total = this.participants.size();
      if (total == 0) {
         this.sendAction(this.pendingAction, this.pendingParam, 0);
         return true;
      } else {
         int totalW = total * btnW + (total - 1) * space;
         int startX = textBoxX + (textBoxW - totalW) / 2;
         int btnY = textBoxY + tbH / 2 - btnH / 2;

         for (int i = 0; i < total; i++) {
            int bx = startX + i * (btnW + space);
            if (mx >= bx && mx < bx + btnW && my >= btnY && my < btnY + btnH) {
               if (this.pendingAction == 1) {
                  this.startSkillEffect(this.pendingAction, this.pendingParam, i + 10);
               } else if (this.pendingAction == 2) {
                  this.sendAction(2, this.pendingGoodsSlot, i + 10);
                  this.pendingGoodsSlot = -1;
               } else {
                  this.sendAction(this.pendingAction, this.pendingParam, i + 10);
               }

               return true;
            }
         }

         return true;
      }
   }

   private boolean handleSkillClick(int mx, int my) {
      int px = this.width / 2 - 115;
      int py = this.height / 2 - 105;
      int pw = 230;
      int listX = px + 4;
      int listW = pw - 6 - 10;
      int listY = py + 4;
      int listH = 170;
      if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
         int cy = listY - this.skillScrollOffset;

         for (int i = 0; i < this.availableSkills.size(); i++) {
            BattleSkill s = this.availableSkills.get(i);
            int cardH = 34;
            if (i == this.skillNavIdx) {
               List dl = this.wrapDesc(s.getLiveDescription(this.currentActorPower(s), this.currentActorStab(s), this.currentActorFrozen()), listW - 12);
               if (!dl.isEmpty()) {
                  cardH = 34 + dl.size() * 10 + 4;
               }
            }

            int cardTop = cy;
            int cardBot = cy + cardH;
            cy += cardH;
            if (cardBot >= listY && cardTop <= listY + listH) {
               int hitTop = Math.max(cardTop, listY);
               int hitBot = Math.min(cardBot, listY + listH);
               if (mx >= listX && mx <= listX + listW && my >= hitTop && my < hitBot) {
                  this.skillNavIdx = i;
                  boolean resonator = !this.pvpMode && this.currentActorGimmick() == OffhandGimmick.SOUL_RESONATOR && this.playerClass == PlayerClass.SOUL_MAGE;
                  boolean canAfford = resonator ? this.playerHp > (float)this.effectiveTpCost(s) : this.activeTp() >= (float)this.effectiveTpCost(s);
                  boolean spareBlocked = (s.getType() == SkillType.SPARE || s.getType() == SkillType.PACIFY) && TurnBattleClientMod.isRuthless;
                  if (!spareBlocked && canAfford) {
                     int skillParam = this.playerClass == null ? i : encodeSkillParam(s);
                     this.playMenuSelect();
                     this.onBashOrSkillAction(1, skillParam);
                  }

                  return true;
               }
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private boolean handleGoodsClick(int mx, int my) {
      int tbX = this.inlineTbX();
      int tbY = this.inlineTbY();
      int tbW = this.inlineTbW();
      int cols = 3;
      int pad = 4;
      int gap = 2;
      int cardW = (tbW - pad * 2 - gap * (cols - 1)) / cols;
      int cardH = 22;
      int gridY = tbY + 12;
      int totalItems = this.foodItems.size();
      int startRow = Math.max(0, this.goodsNavIdx / cols - 2);

      for (int row = startRow; row < startRow + 3; row++) {
         for (int col = 0; col < cols; col++) {
            int idx = row * cols + col;
            if (idx < totalItems) {
               int cx = tbX + pad + col * (cardW + gap);
               int cy = gridY + (row - startRow) * (cardH + gap);
               if (mx >= cx && mx < cx + cardW && my >= cy && my < cy + cardH) {
                  this.goodsNavIdx = idx;
                  this.playMenuSelect();
                  BattleScreen.FoodEntry f = this.foodItems.get(idx);
                  if (this.participants.size() > 1) {
                     this.pendingGoodsSlot = f.slot();
                     this.pendingAction = 2;
                     this.setPhase(BattleScreen.Phase.TARGET_SELECT_ALLY);
                  } else {
                     this.sendAction(2, f.slot(), 0);
                     this.setPhase(BattleScreen.Phase.PLAYER_TURN);
                  }

                  return true;
               }
            }
         }
      }

      return true;
   }

   void setPhase(BattleScreen.Phase p) {
      if (p == BattleScreen.Phase.PLAYER_TURN) {
         this.deferredTurnStartedAtTick = -1;
      }

      if (p == BattleScreen.Phase.ENEMY_TURN) {
         this.enemyPhaseSinceGuards = true;
      }

      if (p == BattleScreen.Phase.PLAYER_TURN && this.enemyPhaseSinceGuards) {
         this.enemyPhaseSinceGuards = false;
         CloneAnimator.releaseAllGuards();
      }

      if (p == BattleScreen.Phase.TARGET_SELECT_ENEMY) {
         List aliveIdx = this.livingEnemyIndices();
         this.targetNavIdx = aliveIdx.isEmpty() ? 0 : aliveIdx.get(0);
      } else if (p == BattleScreen.Phase.TARGET_SELECT_ALLY) {
         this.targetNavIdx = 0;
         this.allyScrollOffset = 0;
      }

      if (p == BattleScreen.Phase.ENEMY_TURN) {
         this.chatBubbles.clear();
         this.chatInputActive = false;
         this.chatInputText = "";
      }

      if (p == BattleScreen.Phase.PLAYER_TURN) {
         int maxVisible = 3;
         int activeIdx = 0;

         for (int i = 0; i < this.participants.size(); i++) {
            if (this.participants.get(i).isCurrentTurn()) {
               activeIdx = i;
               break;
            }
         }

         this.drScrollOffset = Math.max(0, Math.min(Math.max(0, this.participants.size() - maxVisible), activeIdx));
      }

      this.phase = p;
      int textBoxW = this.width - 120;
      if (p == BattleScreen.Phase.ENEMY_TURN) {
         if (this.activeGimmick == BattleScreen.GimmickType.NATURE_SHIELD) {
            this.targetBoxWidth = 64;
            this.targetBoxHeight = 64;
         } else if (this.activeGimmick == BattleScreen.GimmickType.ICE_PLATFORMER) {
            this.targetBoxWidth = Math.min(220, this.width - 60);
            this.targetBoxHeight = 110;
         } else {
            this.targetBoxWidth = 160;
            this.targetBoxHeight = 120;
         }
      } else {
         this.targetBoxWidth = 160;
         this.targetBoxHeight = 120;
      }

      this.showMainButtons(p == BattleScreen.Phase.PLAYER_TURN);
      if (p == BattleScreen.Phase.TARGET_SELECT_ENEMY || p == BattleScreen.Phase.TARGET_SELECT_ALLY) {
         this.showMainButtons(false);
      }

      this.updateMercyButton();
      if (p == BattleScreen.Phase.PLAYER_TURN) {
         this.updateButtonsForTurn();
      }
   }

   public void onPartyMemberTurn(String memberName, int classOrd, float memberTp, List<BattleScreen.ParticipantInfo> plist) {
      this.onPartyMemberTurn(memberName, classOrd, memberTp, plist, null);
   }

   public void onPartyMemberTurn(String memberName, int classOrd, float memberTp, List<BattleScreen.ParticipantInfo> plist, SoulType recruitSoul) {
      this.updateParticipants(plist);
      if (this.realPlayerClass == null) {
         this.realPlayerClass = this.playerClass;
      }

      this.partyMemberTp = memberTp;
      PlayerClass[] classes = PlayerClass.values();
      this.playerClass = classOrd >= 0 && classOrd < classes.length ? classes[classOrd] : PlayerClass.PSI_USER;
      this.recruitSkillSoul = recruitSoul;
      this.actingRecruitName = memberName;
      this.partyMemberLabel = memberName + "'s Turn!";
      this.pushMessage(memberName + "'s turn \u2014 choose their action!");
      this.isMyTurn = true;
      this.transitionTick = 0;
      this.pendingAfterTransition = "player";
      this.quickTransition = this.queueMode;
      this.setPhase(BattleScreen.Phase.TRANSITION);
   }

   private int actingRecruitCode() {
      String n = this.actingRecruitName;
      if (n != null) {
         String[] cn = TurnBattleClientMod.customRecruitNames;
         if (cn != null) {
            for (int i = 0; i < cn.length; i++) {
               if (n.equals(cn[i])) {
                  return 1000 + i;
               }
            }
         }

         for (Recruit r : Recruit.values()) {
            if (n.equals(r.displayName)) {
               return r.ordinal();
            }
         }
      }

      return TurnBattleClientMod.clientActiveRecruit;
   }

   private float activeTp() {
      return this.partyMemberLabel != null ? this.partyMemberTp : this.playerTp;
   }

   private int effectiveTpCost(BattleSkill s) {
      int base = s == LightnerSkill.SNOWGRAVE ? LightnerSkill.snowgraveCost(this.playerTpMax) : s.getTpCost();
      if (this.currentActorGimmick() == OffhandGimmick.THORN_RING) {
         return (int)Math.ceil((double)((float)base * 0.5F));
      } else {
         return this.pvpMode && s == VesselActionSkill.INSTANCE ? 75 : base;
      }
   }

   private String skillMenuDisplayName(BattleSkill s) {
      return this.pvpMode && s == VesselActionSkill.INSTANCE ? "Awaken" : s.getDisplayName();
   }

   private static BattleSkill resolveRogueSkill(String entry) {
      return RogueCards.skillFor(entry);
   }

   private void confirmActSelection(int idx) {
      if (!this.queueMode) {
         CloneAnimator.play(this.localActorCloneKey(), CloneAnimator.Act.ACT, 16753722, null);
         if (this.partyMemberLabel == null) {
            this.setVesselAnim(this.localParticipantIndex(), 7);
         }
      }

      this.sendActPerform(idx, this.actTargetIdx);
   }

   private void sendActPerform(int actIdx, int targetEnemyIdx) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(actIdx);
      buf.writeInt(targetEnemyIdx);
      Net121.sendToServer(TurnBattleMod.ACT_PERFORM, buf);
      this.setPhase(BattleScreen.Phase.PLAYER_TURN);
   }

   private void renderActMenu(DrawContext ctx) {
      int tbY = this.tbTop();
      int tbW = this.tbW();
      int tbX = this.tbX();
      int tbH = this.tbH();
      int COLS = 3;
      int ROWS = 3;
      int PAD = 6;
      int cellW = (tbW - 12 - 8) / 3;
      int cellH = (tbH - 12 - 6) / 3;
      int fH = 9;
      int total = this.actList.size();
      int startIdx = Math.max(0, this.actNavRow / 3 * 3) * 3;
      int pageFirstRow = this.actNavRow / 3 * 3;
      startIdx = pageFirstRow * 3;
      int SOUL_PAD = 13;
      int LEFT_PAD = 19;

      for (int r = 0; r < 3; r++) {
         for (int c = 0; c < 3; c++) {
            int idx = pageFirstRow * 3 + r * 3 + c;
            if (idx < total) {
               ActDefs.ActEntry act = this.actList.get(idx);
               boolean sel = r == this.actNavRow - pageFirstRow && c == this.actNavCol;
               int cx = tbX + 19 + c * (cellW + 4);
               int cy = tbY + 6 + r * (cellH + 3);
               int nameCol = sel ? -188 : -1;
               if (sel) {
                  Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : null;
                  int soulX = cx - 9 - 4;
                  int soulY = cy + (9 - 9) / 2;
                  if (sTex != null) {
                     ctx.drawTexture(sTex, soulX, soulY, 0.0F, 0.0F, 9, 9, 9, 9);
                  }
               }

               ctx.drawTextWithShadow(this.textRenderer, Text.literal(act.name()), cx, cy, nameCol);
            }
         }
      }
   }

   private void openActMenu() {
      if (1 + this.extraEnemies.size() > 1) {
         this.pendingAction = 98;
         this.pendingParam = 0;
         this.setPhase(BattleScreen.Phase.TARGET_SELECT_ENEMY);
      } else {
         this.openActMenuForTarget(0);
      }
   }

   private void openActMenuForTarget(int targetEnemyIdx) {
      this.actTargetIdx = targetEnemyIdx;
      String typeKey = this.entityTypeKey;
      if (targetEnemyIdx > 0 && targetEnemyIdx - 1 < this.extraEnemies.size()) {
         typeKey = this.extraEnemies.get(targetEnemyIdx - 1).typeKey();
      }

      int realCount = 0;

      for (BattleScreen.ParticipantInfo p : this.participants) {
         if (p.isMe() || !p.name().isEmpty()) {
            realCount++;
         }
      }

      int participantCount = realCount;
      List all = ActDefs.getActs(typeKey);
      this.actList = new ArrayList<>();

      for (ActDefs.ActEntry e : all) {
         if (e.participants() <= participantCount) {
            this.actList.add(e);
         }
      }

      if (this.darknessConstricting) {
         this.actList.add(ActDefs.SHINE);
      }

      this.actNavRow = 0;
      this.actNavCol = 0;
      this.setPhase(BattleScreen.Phase.ACT_MENU);
   }

   private void openSkillsMenu() {
      if (this.playerClass == null) {
         this.availableSkills = new ArrayList<>();

         for (String entry : TurnBattleClientMod.clientRogueSkills) {
            BattleSkill skill = resolveRogueSkill(entry);
            if (skill != null) {
               this.availableSkills.add(skill);
            }
         }

         if (this.availableSkills.isEmpty()) {
            return;
         }
      } else {
         Set skillSet = TurnBattleClientMod.clientUnlockedSkills;
         boolean isRecruitTurn = false;
         if (this.realPlayerClass != null) {
            int recruitOrd = this.actingRecruitCode();
            if (recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               Set[] cSkills = TurnBattleClientMod.customRecruitSkills;
               if (cSkills != null && ci < cSkills.length && cSkills[ci] != null) {
                  skillSet = cSkills[ci];
               }

               isRecruitTurn = true;
            } else if (recruitOrd >= 0 && recruitOrd < TurnBattleClientMod.clientRecruitSkills.length) {
               skillSet = TurnBattleClientMod.clientRecruitSkills[recruitOrd];
               isRecruitTurn = true;
            } else {
               int memberOrd = TurnBattleClientMod.clientPartyMember;
               if (memberOrd > 0 && memberOrd < PartyMember.values().length) {
                  skillSet = TurnBattleClientMod.clientPartyUnlockedSkills[memberOrd];
               }
            }
         }

         Set activeSkillSet = skillSet;

         this.availableSkills = new ArrayList<>(switch (this.playerClass) {
            case PSI_USER -> PsiSkill.getAvailable(this.playerLevel, skillSet);
            case LIGHTNER -> LightnerSkill.getAvailable(this.playerLevel, skillSet);
            case HUMAN_MAGE -> HumanMageSkill.getAvailable(this.playerLevel, skillSet);
            case SOUL_MAGE -> SoulMageSkill.getAvailable(this.playerLevel, this.recruitSkillSoul != null ? this.recruitSkillSoul : this.playerSoul, skillSet);
            case ORI -> OriSkill.getAvailable(this.playerLevel, skillSet);
            case MONSTER -> MonsterSkill.getAvailable(this.playerLevel, skillSet);
         });
         if (!isRecruitTurn && this.realPlayerClass == null) {
            if (this.playerClass != PlayerClass.PSI_USER) {
               for (BattleSkill s : PsiSkill.getAvailable(this.playerLevel, skillSet)) {
                  if (!this.availableSkills.contains(s)) {
                     this.availableSkills.add(s);
                  }
               }
            }

            if (this.playerClass != PlayerClass.LIGHTNER) {
               for (BattleSkill sx : LightnerSkill.getAvailable(this.playerLevel, skillSet)) {
                  if (!this.availableSkills.contains(sx)) {
                     this.availableSkills.add(sx);
                  }
               }
            }

            if (TurnBattleClientMod.clientPlayerRace == CharacterRace.HUMAN) {
               for (SoulType soul : SoulType.values()) {
                  if (soul != SoulType.ORI
                     && soul != SoulType.MONSTER
                     && (this.playerClass != PlayerClass.SOUL_MAGE || soul != (this.recruitSkillSoul != null ? this.recruitSkillSoul : this.playerSoul))) {
                     for (BattleSkill sxx : SoulMageSkill.getAvailable(this.playerLevel, soul, activeSkillSet)) {
                        if (!this.availableSkills.contains(sxx)) {
                           this.availableSkills.add(sxx);
                        }
                     }
                  }
               }
            }
         }

         if (isRecruitTurn && this.actingRecruitCode() >= 1000) {
            List more = new ArrayList<>();
            more.addAll(PsiSkill.getAvailable(this.playerLevel, activeSkillSet));
            more.addAll(LightnerSkill.getAvailable(this.playerLevel, activeSkillSet));
            more.addAll(HumanMageSkill.getAvailable(this.playerLevel, activeSkillSet));

            for (SoulType soulx : SoulType.values()) {
               if (soulx != SoulType.ORI && soulx != SoulType.MONSTER) {
                  more.addAll(SoulMageSkill.getAvailable(this.playerLevel, soulx, activeSkillSet));
               }
            }

            for (BattleSkill sxxx : more) {
               if (!this.availableSkills.contains(sxxx)) {
                  this.availableSkills.add(sxxx);
               }
            }
         }

         if (!isRecruitTurn
            && this.realPlayerClass == null
            && TurnBattleClientMod.clientPlayerRace == CharacterRace.HUMAN
            && this.playerClass != PlayerClass.HUMAN_MAGE) {
            for (BattleSkill hs : HumanMageSkill.getAvailable(this.playerLevel, activeSkillSet)) {
               if (!this.availableSkills.contains(hs)) {
                  this.availableSkills.add(hs);
               }
            }
         }

         if (!isRecruitTurn
            && this.realPlayerClass == null
            && TurnBattleClientMod.clientPlayerRace == CharacterRace.MONSTER
            && this.playerClass != PlayerClass.MONSTER) {
            for (BattleSkill ms : MonsterSkill.getAvailable(this.playerLevel, activeSkillSet)) {
               if (!this.availableSkills.contains(ms)) {
                  this.availableSkills.add(ms);
               }
            }
         }

         if (this.realPlayerClass == null || isRecruitTurn) {
            int recruitOrd = this.actingRecruitCode();
            Set uneq;
            if (isRecruitTurn && recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               Set[] cUneq = TurnBattleClientMod.customRecruitUnequippedSkills;
               uneq = cUneq != null && ci < cUneq.length && cUneq[ci] != null ? cUneq[ci] : Collections.emptySet();
            } else {
               uneq = isRecruitTurn ? TurnBattleClientMod.clientRecruitUnequippedSkills : TurnBattleClientMod.clientUnequippedSkills;
            }

            this.availableSkills.removeIf(sxxxx -> uneq.contains(((Enum)sxxxx).name()));
         }
      }

      float tp = this.activeTp();
      if (this.playerClass != null) {
         this.availableSkills.sort((a, b) -> {
            boolean aAfford = tp >= (float)this.effectiveTpCost(a);
            boolean bAfford = tp >= (float)this.effectiveTpCost(b);
            if (aAfford != bAfford) {
               return aAfford ? -1 : 1;
            } else {
               return 0;
            }
         });
      }

      this.skillScrollOffset = 0;
      this.skillNavIdx = 0;
      if (this.playerClass != null) {
         this.availableSkills.add(0, VesselActionSkill.INSTANCE);
      }

      this.setPhase(BattleScreen.Phase.SKILLS_MENU);
   }

   private void openGoodsMenu() {
      this.foodItems.clear();
      if (this.client != null && this.client.player != null) {
         PlayerInventory inv = this.client.player.getInventory();

         for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            FoodComponent food = (FoodComponent)stack.get(DataComponentTypes.FOOD);
            if (!stack.isEmpty() && food != null) {
               int hp = (int)(((float)(food.nutrition() * 2) + food.saturation() * 2.0F) * 2.0F);
               String itemId2 = Registries.ITEM.getId(stack.getItem()).toString();
               this.foodItems.add(new BattleScreen.FoodEntry(i, stack.getItem().getName().getString(), hp, itemId2, stack.getCount()));
            }
         }
      }

      this.goodsScrollOffset = 0;
      this.setPhase(BattleScreen.Phase.GOODS_MENU);
   }

   private void tickBoxShrink() {
      if (this.boxShrinkPlaying || this.boxRetreatFx) {
         this.drSpinOutTickPrev = this.drSpinOutTick++;
         if (this.boxShrinkPlaying && this.drSpinOutTick >= 10) {
            this.boxShrinkPlaying = false;
            this.boxRetreatFx = true;
         }

         if (this.boxRetreatFx && this.drSpinOutTick >= 20) {
            this.boxRetreatFx = false;
         }
      }
   }

   private void tickSoulReturn() {
      if (this.soulReturning) {
         this.soulReturnTick++;
         if ((float)this.soulReturnTick >= 12.333333F) {
            this.soulReturning = false;
         }
      }
   }

   private void tickSoulLeave() {
      if (this.soulLeaving) {
         this.soulLeaveTick++;
         if ((float)this.soulLeaveTick >= 4.6666665F) {
            this.soulLeaving = false;
         }
      }
   }

   private void tickPlayerGrazePulses() {
      if (this.playerGrazeTriggerTick >= 0) {
         this.playerGrazeTriggerTick++;
         int maxDelay = GRAZE_PULSE_DELAYS[GRAZE_PULSE_DELAYS.length - 1];
         if ((float)this.playerGrazeTriggerTick > (float)maxDelay + 3.3333333F) {
            this.playerGrazeTriggerTick = -1000;
         }
      }
   }

   void triggerPlayerGrazePulses() {
      this.playerGrazeTriggerTick = 0;
   }

   float[] playerSoulAnchor() {
      int count = Math.max(1, this.participants.isEmpty() ? 1 : this.participants.size());
      int myIdx = 0;

      for (int i = 0; i < this.participants.size(); i++) {
         if (this.participants.get(i).isMe()) {
            myIdx = i;
            break;
         }
      }

      if (this.overhaul) {
         float[] c = OverhaulView.projectEntity(this.ohPartyEntity(myIdx), 0.62F, this.lastRenderDelta);
         if (c == null && this.client != null) {
            c = OverhaulView.projectEntity(this.client.player, 0.62F, this.lastRenderDelta);
         }

         return c != null ? new float[]{c[0] - 4.5F, c[1] - 4.5F} : new float[]{(float)this.width / 2.0F - 4.5F, (float)this.height * 0.7F};
      } else if (this.anchors3D()) {
         int[] clonePos = BattleEntityRenderer.participantPos(myIdx, count, this.width, this.height, 82, true);
         return new float[]{(float)clonePos[0] - 4.5F, (float)(clonePos[1] - 40) - 4.5F};
      } else {
         int maxVisible = 3;
         int vis = Math.min(count, 3);
         int totalW = vis * 150 + (vis - 1) * 3;
         int startX2 = (this.width - totalW) / 2;
         int drawIndex = Math.max(0, Math.min(myIdx - this.drScrollOffset, 2));
         int colX = startX2 + drawIndex * 153;
         int colY = this.colsBottom() - 29;
         return new float[]{(float)colX + 75.0F - 4.5F, (float)colY + 14.5F - 4.5F};
      }
   }

   public void mouseMoved(double mx, double my) {
      this.mouseX = (int)mx;
      this.mouseY = (int)my;
      super.mouseMoved(mx, my);
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double delta) {
      int maxScroll = Math.max(0, this.participants.size() - 4);
      if (maxScroll > 0) {
         this.participantScrollOffset = (int)Math.max(0.0, Math.min((double)maxScroll, (double)this.participantScrollOffset - delta));
      }

      return true;
   }

   private void forceExit() {
      Net121.sendToServer(TurnBattleMod.FORCE_END, PacketByteBufs.empty());
   }

   private int[] effectCenterForRemoteTarget(UUID targetUuid, int legacyTargetIndex, SkillType skillType) {
      if (this.pvpMode && targetUuid != null) {
         for (int i = 0; i < this.participants.size(); i++) {
            if (targetUuid.equals(this.participants.get(i).uuid())) {
               return this.effectCenterForTarget(10 + i, skillType);
            }
         }

         for (int ix = 0; ix < this.pvpOpponents.size(); ix++) {
            if (targetUuid.equals(this.pvpOpponents.get(ix).uuid())) {
               return this.effectCenterForTarget(ix, skillType);
            }
         }
      }

      return this.effectCenterForTarget(this.remoteEffectTarget(legacyTargetIndex, skillType), skillType);
   }

   public void playRemoteSkillEffect(int skillOrd, PlayerClass cls, int targetIndex, UUID targetUuid) {
      BattleSkill skill = decodeSkillParam(skillOrd);
      if (skill != null) {
         if (this.queueMode && this.drResolving && this.phase == BattleScreen.Phase.SKILL_EFFECT) {
            this.activeEffect = null;
            this.activeBashEffect = null;
         } else if (this.phase == BattleScreen.Phase.SKILL_EFFECT || this.phase == BattleScreen.Phase.ENEMY_TURN || this.phase == BattleScreen.Phase.RESULT) {
            return;
         }

         this.pendingAction = -1;
         this.pendingParam = skillOrd;
         this.pendingTarget = -1;
         int remoteSoulColor = 0;
         if (cls == PlayerClass.SOUL_MAGE && skill instanceof SoulMageSkill sms && sms.getSoul() != null) {
            remoteSoulColor = sms.getSoul().getColor() & 16777215;
         }

         if (skill == LightnerSkill.RED_BUSTER) {
            remoteSoulColor = 16720418;
         }

         int[] ec = this.effectCenterForRemoteTarget(targetUuid, targetIndex, skill.getType());
         this.activeEffect = SkillEffect.forSkill(skill, ec[0], ec[1], remoteSoulColor);
         this.pendingAfterTransition = this.queueMode && this.drResolving ? "wait" : "player";
         this.setPhase(BattleScreen.Phase.SKILL_EFFECT);
      }
   }

   private int networkIdForLegacyTarget(int target) {
      if (target == 0) {
         return this.currentPrimaryEnemyId >= 0 ? this.currentPrimaryEnemyId : this.entityNetworkId;
      } else {
         int idx = target - 1;
         return idx >= 0 && idx < this.extraEnemies.size() ? this.extraEnemies.get(idx).networkId() : -1;
      }
   }

   private int remoteEffectTarget(int targetIndex, SkillType skillType) {
      if (!this.pvpMode) {
         return targetIndex;
      } else {
         boolean isAllyTarget = targetIndex >= 10
            || skillType == SkillType.HEAL
            || skillType == SkillType.SHIELD
            || skillType == SkillType.BUFF
            || skillType == SkillType.MAGNET;
         return isAllyTarget ? targetIndex : 10 + this.localParticipantIndex();
      }
   }

   private int[] effectCenterForTarget(int target, SkillType skillType) {
      this.fxTarget = target;
      this.fxSkillType = skillType;
      this.fxTargetNetId = target >= 0 && target < 10 && !isAllySkillType(skillType) ? this.networkIdForLegacyTarget(target) : -1;
      if (this.overhaul) {
         Entity t = this.ohFxEntity();
         float[] c = OverhaulView.projectEntity(t, 0.55F, this.lastRenderDelta);
         if (c != null) {
            return new int[]{Math.round(c[0]), Math.round(c[1])};
         }
      }

      if (!this.anchors3D()) {
         int barAreaBottom = 26 + 9 + 4;
         int textBoxTop = this.height - 72 - 75 - 10;
         int spriteZoneY = (barAreaBottom + textBoxTop) / 2;
         if (target < 10 && skillType != SkillType.HEAL && skillType != SkillType.SHIELD && skillType != SkillType.BUFF && skillType != SkillType.MAGNET) {
            int total = 1 + this.extraEnemies.size();
            int barW = 180;
            int spacing = 8;
            if (total > 1) {
               int totalBarW = total * barW + (total - 1) * spacing;
               if (totalBarW > this.width - 20) {
                  barW = (this.width - 20 - (total - 1) * spacing) / total;
               }

               int startX = (this.width - (total * barW + (total - 1) * spacing)) / 2;
               int barCenterX = startX + target * (barW + spacing) + barW / 2;
               return new int[]{barCenterX, spriteZoneY};
            } else {
               return new int[]{this.width / 2, spriteZoneY};
            }
         } else {
            return new int[]{this.width / 2, spriteZoneY};
         }
      } else {
         boolean isAllyTarget = target >= 10
            || skillType == SkillType.HEAL
            || skillType == SkillType.SHIELD
            || skillType == SkillType.BUFF
            || skillType == SkillType.MAGNET;
         if (isAllyTarget) {
            int total = Math.max(1, this.participants.size());
            int participantIdx = target >= 10 ? Math.max(0, Math.min(target - 10, total - 1)) : 0;
            if (participantIdx < this.lastParticipantCX.length && this.lastParticipantCX[participantIdx] > 0) {
               return new int[]{this.lastParticipantCX[participantIdx], this.lastParticipantTopY[participantIdx]};
            } else {
               int[] pos = BattleEntityRenderer.participantPos(participantIdx, total, this.width, this.height, 82, true);
               return new int[]{Math.max(55, pos[0]), pos[1] - 20};
            }
         } else {
            int targetNetworkId = this.networkIdForLegacyTarget(target);
            int slot = this.rosterSlotForId(targetNetworkId);
            if (slot < 0) {
               slot = Math.min(target, this.lastSpriteCX.length - 1);
            }

            if (slot >= 0 && slot < this.lastSpriteCX.length && this.lastSpriteCX[slot] > 0) {
               return new int[]{this.lastSpriteCX[slot], this.lastSpriteCY[slot] - 20};
            } else {
               int fallbackSlot = slot >= 0 ? slot : target;
               int[] pos = BattleEntityRenderer.participantPos(fallbackSlot, Math.max(1, this.enemyRoster.size()), this.width, this.height, 82, false);
               return new int[]{pos[0], pos[1] - 20};
            }
         }
      }
   }

   private void requestBashQte(int targetIndex) {
      if (this.phase != BattleScreen.Phase.SKILL_EFFECT && (!this.qteActive || this.qteDone)) {
         if (this.queueMode) {
            this.sendAction(0, 0, targetIndex);
         } else {
            this.qteTargetIndexPending = targetIndex;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(targetIndex);
            Net121.sendToServer(TurnBattleMod.BASH_QTE_ROLL, buf);
            this.setPhase(BattleScreen.Phase.SKILL_EFFECT);
            this.bashRequestTick = this.globalTick;
         }
      }
   }

   private void tickBashRequestTimeout() {
      if (this.bashRequestTick >= 0) {
         if (this.phase == BattleScreen.Phase.SKILL_EFFECT && !this.qteActive && this.activeBashEffect == null && this.activeEffect == null) {
            if (this.globalTick - this.bashRequestTick >= 50) {
               this.bashRequestTick = -1;
               this.pendingAction = -1;
               if (!"wait".equals(this.pendingAfterTransition) && !"enemy".equals(this.pendingAfterTransition)) {
                  this.pendingAfterTransition = "player";
               }

               this.transitionTick = 0;
               this.setPhase(BattleScreen.Phase.TRANSITION);
            }
         } else {
            this.bashRequestTick = -1;
         }
      }
   }

   public void onBashQteStart(int soulColorRgb, int targetIndex, boolean crit, float critZoneCenter, boolean isMe, UUID targetUuid) {
      this.qteSoulColorRgb = soulColorRgb;
      this.qteTargetIndexPending = targetIndex;
      this.qteCritCenter = 0.0F;
      if (isMe) {
         this.bashRequestTick = -1;
      }

      if (crit) {
         if (isMe) {
            this.playOwnBashSlash(targetIndex, soulColorRgb, 1.0F, 0);
         } else {
            this.playRemoteBashEffect(soulColorRgb, targetIndex, targetUuid);
         }
      } else if (isMe) {
         if (!this.qteActive || this.qteDone) {
            this.qteActive = true;
            this.qteIsMe = true;
            this.qteDone = false;
            this.qteTick = 0;
            this.qteTickPrev = 0;
            this.setPhase(BattleScreen.Phase.SKILL_EFFECT);
         }
      }
   }

   private void tickBashQte() {
      if (this.qteActive) {
         this.qteTickPrev = this.qteTick++;
         if (!this.qteDone) {
            if ((float)this.qteTick >= 27.927273F) {
               this.resolveBashQte(-0.16363636F, true);
            }
         } else if (this.qteTick >= 8) {
            this.qteActive = false;
         }
      }
   }

   private void tickDeferredTurnTimeout() {
      if (this.deferredTurnStartedAtTick >= 0) {
         if (this.phase == BattleScreen.Phase.PLAYER_TURN) {
            this.deferredTurnStartedAtTick = -1;
         } else if (this.globalTick - this.deferredTurnStartedAtTick >= 60) {
            this.deferredTurnStartedAtTick = -1;
            this.activeBashEffect = null;
            this.activeEffect = null;
            this.qteActive = false;
            this.pendingAction = -1;
            this.transitionTick = 0;
            this.pendingAfterTransition = "player";
            this.setPhase(BattleScreen.Phase.TRANSITION);
         }
      }
   }

   private float qteTickSmooth() {
      return (float)this.qteTickPrev + (float)(this.qteTick - this.qteTickPrev) * this.lastRenderDelta;
   }

   private float rollingPlayerHpSmooth() {
      return !(this.rollingPlayerHp < 0.0F) && !(this.rollingPlayerHpPrev < 0.0F)
         ? this.rollingPlayerHpPrev + (this.rollingPlayerHp - this.rollingPlayerHpPrev) * this.lastRenderDelta
         : this.rollingPlayerHp;
   }

   private float rollingParticipantHpSmooth(int i, float fallback) {
      Float cur = this.rollingParticipantHp.get(i);
      Float prev = this.rollingParticipantHpPrev.get(i);
      if (cur == null) {
         return fallback;
      } else {
         return prev == null ? cur : prev + (cur - prev) * this.lastRenderDelta;
      }
   }

   private float displayedTpSmooth() {
      return !(this.displayedTp < 0.0F) && !(this.displayedTpPrev < 0.0F)
         ? this.displayedTpPrev + (this.displayedTp - this.displayedTpPrev) * this.lastRenderDelta
         : this.displayedTp;
   }

   private float drainTpSmooth() {
      return !(this.drainTp < 0.0F) && !(this.drainTpPrev < 0.0F) ? this.drainTpPrev + (this.drainTp - this.drainTpPrev) * this.lastRenderDelta : this.drainTp;
   }

   private float speedLineTpSmooth() {
      return !(this.speedLineTp < 0.0F) && !(this.speedLineTpPrev < 0.0F)
         ? this.speedLineTpPrev + (this.speedLineTp - this.speedLineTpPrev) * this.lastRenderDelta
         : this.speedLineTp;
   }

   private void hitBashQte() {
      float pos = 1.0F - this.qteTickSmooth() / 24.0F;
      if (!(pos > 0.5204545F)) {
         this.resolveBashQte(pos, false);
      }
   }

   private void resolveBashQte(float pos, boolean ranOut) {
      float[] score = ranOut ? new float[]{0.0F, 0.0F, 0.0F} : qteScore(pos);
      boolean isCrit = score[2] > 0.0F;
      float multiplier = score[0];
      int tpTier = (int)score[1];
      this.qteResultWasCrit = isCrit;
      this.qteRanOut = ranOut;
      this.qteResultPos = pos;
      this.qteDone = true;
      this.qteTick = 0;
      this.qteTickPrev = 0;
      this.playOwnBashSlash(this.qteTargetIndexPending, this.qteSoulColorRgb, multiplier, tpTier);
   }

   private void playOwnBashSlash(int targetIndex, int soulColorRgb, float multiplier, int tpTier) {
      this.pendingAction = 0;
      this.pendingParam = 0;
      this.pendingTarget = targetIndex;
      this.qteResultMultiplier = multiplier;
      this.qteResultTpTier = tpTier;
      if (this.partyMemberLabel == null) {
         this.setVesselAnim(this.localParticipantIndex(), 0);
      }

      CloneAnimator.play(this.localActorCloneKey(), CloneAnimator.Act.BASH, soulColorRgb, null);
      int[] ec = this.effectCenterForTarget(targetIndex, null);
      this.activeBashEffect = new BattleScreen.BashEffect(ec[0], ec[1], soulColorRgb);
      this.setPhase(BattleScreen.Phase.SKILL_EFFECT);
   }

   public void playRemoteBashEffect(int soulColorRgb, int targetIndex, UUID targetUuid) {
      if (this.queueMode) {
         int[] qc = this.effectCenterForRemoteTarget(targetUuid, targetIndex, null);
         BattleScreen.BashEffect fx = new BattleScreen.BashEffect(qc[0], qc[1], soulColorRgb);
         boolean free = this.activeBashEffect == null
            && this.activeEffect == null
            && this.phase != BattleScreen.Phase.ENEMY_TURN
            && this.phase != BattleScreen.Phase.RESULT
            && this.phase != BattleScreen.Phase.ENEMY_TURN_END;
         if (!free) {
            this.extraBashEffects.add(fx);
         } else {
            this.qteActive = false;
            this.pendingAction = -1;
            this.activeBashEffect = fx;
            this.pendingAfterTransition = "wait";
            this.setPhase(BattleScreen.Phase.SKILL_EFFECT);
         }
      } else {
         boolean genuinelyBusy = this.phase == BattleScreen.Phase.SKILL_EFFECT && (this.activeBashEffect != null || this.activeEffect != null)
            || this.phase == BattleScreen.Phase.ENEMY_TURN
            || this.phase == BattleScreen.Phase.RESULT;
         if (!genuinelyBusy) {
            this.qteActive = false;
            this.pendingAction = -1;
            int[] ec = this.effectCenterForRemoteTarget(targetUuid, targetIndex, null);
            this.activeBashEffect = new BattleScreen.BashEffect(ec[0], ec[1], soulColorRgb);
            this.pendingAfterTransition = "player";
            this.setPhase(BattleScreen.Phase.SKILL_EFFECT);
         }
      }
   }

   private void startSkillEffect(int type, int skillOrd, int target) {
      if (this.queueMode) {
         this.sendAction(type, skillOrd, target);
      } else {
         if (this.partyMemberLabel == null) {
            this.setVesselAnim(this.localParticipantIndex(), type);
         }

         Object skill;
         if (this.playerClass == null) {
            List rogueSkills = TurnBattleClientMod.clientRogueSkills;
            skill = skillOrd >= 0 && skillOrd < rogueSkills.size() ? resolveRogueSkill(rogueSkills.get(skillOrd)) : null;
         } else if (skillOrd == -99) {
            skill = VesselActionSkill.INSTANCE;
         } else {
            skill = decodeSkillParam(skillOrd);
         }

         if (skill == null) {
            this.sendAction(type, skillOrd, target);
         } else {
            this.pendingAction = type;
            this.pendingParam = skillOrd;
            this.pendingTarget = target;
            int var10000;
            if (this.playerClass == PlayerClass.SOUL_MAGE && this.playerSoul != null) {
               var10000 = this.playerSoul.getColor() & 16777215;
            } else {
               label68: {
                  if (skill instanceof SoulMageSkill sms && sms.getSoul() != null) {
                     var10000 = sms.getSoul().getColor() & 16777215;
                     break label68;
                  }

                  var10000 = 0;
               }
            }

            int soulColor = var10000;
            if (this.playerClass == PlayerClass.ORI) {
               soulColor = 14540253;
            }

            if (skill == LightnerSkill.RED_BUSTER) {
               soulColor = 16720418;
            }

            CloneAnimator.play(
               this.localActorCloneKey(), CloneAnimator.Act.SKILL, soulColor != 0 ? soulColor : this.localCloneSoulColor(), null, CastAnims.castFor(skill)
            );
            int[] ec = this.effectCenterForTarget(target, skill.getType());
            this.activeEffect = SkillEffect.forSkill(skill, ec[0], ec[1], soulColor);
            this.setPhase(BattleScreen.Phase.SKILL_EFFECT);
         }
      }
   }

   private void onBashOrSkillAction(int type, int skillOrd) {
      boolean isHeal = false;
      if (type == 1) {
         PlayerClass[] classes = PlayerClass.values();
         SkillType st;
         if (this.playerClass != null) {
            PlayerClass cls = this.playerClass.ordinal() < classes.length ? this.playerClass : classes[0];
            st = this.getSkillType(cls, skillOrd);
         } else {
            List rogueSkills = TurnBattleClientMod.clientRogueSkills;
            BattleSkill rs = skillOrd >= 0 && skillOrd < rogueSkills.size() ? resolveRogueSkill(rogueSkills.get(skillOrd)) : null;
            st = rs != null ? rs.getType() : null;
         }

         isHeal = st == SkillType.HEAL || st == SkillType.SHIELD || st == SkillType.BUFF;
      }

      boolean isThreshold = false;
      if (type == 1) {
         if (skillOrd == -99) {
            isThreshold = true;
         } else if (this.playerClass != null) {
            PlayerClass cls2 = this.playerClass;
            SkillType st2 = this.getSkillType(cls2, skillOrd);
            if (st2 != SkillType.RAISE_THRESHOLD && st2 != SkillType.SPARE) {
               boolean var13 = false;
            } else {
               boolean var10000 = true;
            }
         }
      }

      if (isHeal && this.participants.size() > 1) {
         this.pendingAction = type;
         this.pendingParam = skillOrd;
         this.setPhase(BattleScreen.Phase.TARGET_SELECT_ALLY);
      } else if (!isHeal) {
         this.pendingAction = type;
         this.pendingParam = skillOrd;
         this.setPhase(BattleScreen.Phase.TARGET_SELECT_ENEMY);
      } else if (type == 1) {
         this.startSkillEffect(type, skillOrd, 0);
      } else {
         this.sendAction(type, skillOrd, 0);
      }
   }

   private SkillType getSkillType(PlayerClass cls, int ordinal) {
      if (cls == null) {
         return null;
      } else if (ordinal == -99) {
         return VesselActionSkill.INSTANCE.getType();
      } else {
         BattleSkill skill = decodeSkillParam(ordinal);
         return skill != null ? skill.getType() : null;
      }
   }

   int localParticipantIndex() {
      for (int i = 0; i < this.participants.size(); i++) {
         if (this.participants.get(i).isMe()) {
            return i;
         }
      }

      return 0;
   }

   private void sendAction(int type, int param) {
      this.sendAction(type, param, 0);
   }

   private void sendAction(int type, int param, int targetIndex) {
      if (this.phase == BattleScreen.Phase.PLAYER_TURN
         || this.phase == BattleScreen.Phase.SKILLS_MENU
         || this.phase == BattleScreen.Phase.GOODS_MENU
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY
         || this.phase == BattleScreen.Phase.SKILL_EFFECT) {
         boolean justPicked = this.queueMode && (type == 0 || type == 1 || type == 2 || type == 5);
         if ((type == 3 || type == 4 || type == 5) && this.partyMemberLabel == null && !justPicked) {
            this.setVesselAnim(this.localParticipantIndex(), type);
         }

         if (this.phase != BattleScreen.Phase.SKILL_EFFECT && !justPicked) {
            UUID cloneKey = this.localActorCloneKey();
            switch (type) {
               case 0:
                  CloneAnimator.play(cloneKey, CloneAnimator.Act.BASH, this.localCloneSoulColor(), null);
                  break;
               case 1:
                  CloneAnimator.play(cloneKey, CloneAnimator.Act.SKILL, this.localCloneSoulColor(), null);
                  break;
               case 2:
                  CloneAnimator.play(cloneKey, CloneAnimator.Act.ITEM, 7143290, this.localInventoryStack(param));
                  break;
               case 3:
                  CloneAnimator.play(cloneKey, CloneAnimator.Act.DEFEND, 9427199, null);
               case 4:
               default:
                  break;
               case 5:
                  CloneAnimator.play(cloneKey, CloneAnimator.Act.SPARE, 16770650, null);
            }
         }

         this.pvpProtectTriggered = false;
         this.setPhase(BattleScreen.Phase.TRANSITION);
         this.pendingAfterTransition = "player";
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeByte(type);
         buf.writeInt(param);
         buf.writeByte(targetIndex);
         Net121.sendToServer(TurnBattleMod.PLAYER_ACTION, buf);
      }
   }

   public void onBattleStart(
      int entityId, String name, String typeKey, float eMaxHp, float eHp, float pMaxHp, float pHp, float tp, int level, int exp, int expNeeded, int classOrd
   ) {
      boolean freshStart = this.phase == BattleScreen.Phase.TRANSITION && this.introTick == 0 || System.currentTimeMillis() - this.screenOpenedAtMillis < 500L;
      if (freshStart) {
         this.popTick = -1;
         this.outroTick = -1;
      }

      if (freshStart) {
         this.queueMode = false;
         this.drText = "";
         this.drIdx = 0;
         this.drMode = 0;
         this.mbActive = false;
         this.mbBars.clear();
         this.readyKeys.clear();
      }

      this.drResolving = false;
      this.directHitEffect = null;
      this.otherDirectHitEffects.clear();
      this.entityName = name;
      this.entityTypeKey = typeKey != null ? typeKey : "";
      this.entityNetworkId = entityId;
      this.entityMaxHp = eMaxHp;
      this.entityHp = eHp;
      if (this.anchors3D()) {
         this.lastSpriteCX[0] = (int)((float)this.width * 0.75F);
         this.lastSpriteCY[0] = this.height / 2;
         this.lastParticipantCX[0] = (int)((float)this.width * 0.2F);
         this.lastParticipantTopY[0] = this.height / 2 - 30;
      }

      if (freshStart) {
         this.displayedEntityHp = eHp;
         this.drainEntityHp = eHp;
         this.displayedExtraHp.clear();
         this.drainExtraHp.clear();
      }

      this.displayedTp = -1.0F;
      this.drainTp = -1.0F;
      this.speedLineTp = -1.0F;
      this.krisSlashes.clear();
      this.playerMaxHp = pMaxHp;
      this.playerHp = pHp;
      if (freshStart || !this.rollingHealth || this.rollingPlayerHp < 0.0F) {
         this.rollingPlayerHp = pHp;
         this.rollingParticipantHp.clear();
         this.lastParticipantTrueHp.clear();
         this.rollingParticipantHpPrev.clear();
      }

      this.playerTp = tp;
      if (!this.pvpMode) {
         this.pvpOpponentSoul = null;
      }

      this.playerLevel = level;
      this.playerExp = exp;
      this.expNeeded = expNeeded;
      PlayerClass[] classes = PlayerClass.values();
      this.playerClass = classOrd >= 0 && classOrd < classes.length ? classes[classOrd] : null;
      this.flushLog("A battle started against " + name + "!");
      this.offhandGimmick = OffhandGimmick.NONE;
      if (this.client != null && this.client.player != null) {
         this.offhandGimmick = OffhandGimmick.fromStack(TurnBattleClientMod.clientEquippedAccessory);
      }

      if (this.offhandGimmick != OffhandGimmick.NONE) {
         PacketByteBuf gBuf = PacketByteBufs.create();
         gBuf.writeInt(this.offhandGimmick.ordinal());
         Net121.sendToServer(TurnBattleMod.OFFHAND_GIMMICK, gBuf);
      }

      Arrays.fill(this.vesselAnimType, 4);
      Arrays.fill(this.vesselAnimTick, 0);
      Arrays.fill(this.vesselHurtTick, 0);
      Arrays.fill(this.vesselBlocking, false);
      Arrays.fill(this.vesselHoldTick, 0);
      this.vesselAlpha = 0.0F;
      boolean freshBattle = this.phase == BattleScreen.Phase.TRANSITION && this.introTick == 0 || System.currentTimeMillis() - this.screenOpenedAtMillis < 500L;
      if (this.phase == BattleScreen.Phase.SKILL_EFFECT) {
         this.pendingAfterTransition = "player";
         if (this.deferredTurnStartedAtTick < 0) {
            this.deferredTurnStartedAtTick = this.globalTick;
         }
      } else if (freshBattle) {
         if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.BATTLE_START, 1.0F, 1.0F));
         }

         this.introSoulX = (float)this.width / 2.0F - 4.5F;
         this.introSoulY = (float)this.height / 2.0F - 4.5F;
         this.introTick = 0;
         this.setPhase(BattleScreen.Phase.BATTLE_INTRO);
      } else {
         this.vesselAlpha = 255.0F;
         this.setPhase(BattleScreen.Phase.PLAYER_TURN);
      }
   }

   public void onBattleState(
      float eHp,
      float eMaxHp,
      String eName,
      String eTypeKey,
      float pHp,
      float pMaxHp,
      float tp,
      String message,
      boolean isPlayerTurn,
      boolean isEnemyTurn,
      float shieldR,
      int shieldT,
      int sleepT,
      int confusedT,
      boolean spareable,
      int serverHitCount
   ) {
      if (!this.pvpMode && eHp < this.entityHp) {
         int primarySlot = 0;
         if (this.anchors3D()) {
            int resolved = this.rosterSlotForId(this.currentPrimaryEnemyId);
            if (resolved >= 0) {
               primarySlot = resolved;
            }
         }

         if (primarySlot < this.damageTicks.length) {
            this.damageTicks[primarySlot] = 12;
         }

         SoulType attackerSoul = this.playerSoul;

         for (BattleScreen.ParticipantInfo pip : this.participants) {
            if (pip.isCurrentTurn() && pip.soul() != null) {
               attackerSoul = pip.soul();
               break;
            }
         }

         float totalDmg = this.entityHp - eHp;
         int hitCount = Math.max(1, serverHitCount);
         if (hitCount > 1) {
            float weightSum = 0.0F;
            float w = 1.0F;

            for (int i = 0; i < hitCount; i++) {
               weightSum += w;
               w *= 0.85F;
            }

            float first = totalDmg / weightSum;
            float shown = 0.0F;
            w = 1.0F;

            for (int i = 0; i < hitCount; i++) {
               float thisHit = i == hitCount - 1 ? totalDmg - shown : first * w;
               shown += thisHit;
               w *= 0.85F;
               this.spawnDamageNumber(primarySlot, thisHit, attackerSoul, "", i * 3);
            }
         } else {
            this.spawnDamageNumber(primarySlot, totalDmg, attackerSoul, "");
         }
      }

      this.entityHp = eHp;
      this.entityMaxHp = eMaxHp;
      if (eName != null && !eName.isEmpty()) {
         this.entityName = eName;
      }

      if (eTypeKey != null && !eTypeKey.isEmpty()) {
         this.entityTypeKey = eTypeKey;
      }

      this.playerHp = pHp;
      if (this.rollingPlayerHp < 0.0F) {
         this.rollingPlayerHp = pHp;
      }

      this.playerMaxHp = pMaxHp;
      this.playerTp = Math.min(this.playerTpMax, tp);
      this.shieldReduce = shieldR;
      this.shieldTurns = shieldT;
      this.enemySleepTurns = sleepT;
      this.enemyConfusedTurns = confusedT;
      this.enemySpareable = spareable;
      if (!this.queueMode) {
         this.enqueueLog(message);
      }

      this.updateMercyButton();
      this.isMyTurn = isPlayerTurn;
      if (isPlayerTurn && this.realPlayerClass != null) {
         this.playerClass = this.realPlayerClass;
         this.realPlayerClass = null;
         this.recruitSkillSoul = null;
         this.partyMemberLabel = null;
         this.partyMemberTp = 0.0F;
      }

      if (isEnemyTurn) {
         this.pendingAfterTransition = "enemy";
         if (this.phase == BattleScreen.Phase.PLAYER_TURN || this.phase == BattleScreen.Phase.SKILLS_MENU || this.phase == BattleScreen.Phase.GOODS_MENU) {
            this.transitionTick = 0;
            this.setPhase(BattleScreen.Phase.TRANSITION);
         }
      } else if (isPlayerTurn) {
         this.pendingAfterTransition = "player";
         if (this.phase != BattleScreen.Phase.SKILL_EFFECT && this.phase != BattleScreen.Phase.ENEMY_TURN_END && this.phase != BattleScreen.Phase.BATTLE_INTRO) {
            this.transitionTick = 0;
            this.quickTransition = this.queueMode;
            this.setPhase(BattleScreen.Phase.TRANSITION);
         }
      } else {
         this.pendingAfterTransition = "wait";
         if (this.phase != BattleScreen.Phase.TRANSITION
            && this.phase != BattleScreen.Phase.SKILL_EFFECT
            && this.phase != BattleScreen.Phase.ENEMY_TURN_END
            && this.phase != BattleScreen.Phase.BATTLE_INTRO) {
            this.transitionTick = 0;
            this.setPhase(BattleScreen.Phase.TRANSITION);
         }
      }
   }

   public void onEnemyTurn(float dmg, int pattern, float speedMult, int elementOrd, long bulletSeed) {
      this.endQueuedRound();
      if (this.pendingBossLine != null && !this.pendingBossLine.isEmpty() && pattern >= 0) {
         String line = this.pendingBossLine;
         this.pendingBossLine = "";
         BossSpeech.show(line, () -> {
            this.onEnemyTurn(dmg, pattern, speedMult, elementOrd, bulletSeed);
            this.quickTransition = true;
         });
      } else if (pattern < 0) {
         this.pendingAfterTransition = "wait";
         if (this.phase != BattleScreen.Phase.SKILL_EFFECT && this.phase != BattleScreen.Phase.TRANSITION) {
            this.transitionTick = 0;
            this.setPhase(BattleScreen.Phase.TRANSITION);
         }

         PacketByteBuf rb = PacketByteBufs.create();
         rb.writeInt(0);
         rb.writeInt(0);
         Net121.sendToServer(TurnBattleMod.ENEMY_TURN_RESULT, rb);
      } else {
         this.enemyDmgPerHit = dmg;
         this.patternIndex = pattern;
         this.bulletSpeedMult = speedMult;
         BulletPatterns.RNG.setSeed(bulletSeed);
         Element[] elements = Element.values();
         if (elementOrd >= 0 && elementOrd < elements.length) {
            this.battleElement = elements[elementOrd];
         }

         this.pendingAfterTransition = "enemy";
         if (this.phase != BattleScreen.Phase.SKILL_EFFECT) {
            this.transitionTick = 0;
            this.setPhase(BattleScreen.Phase.TRANSITION);
         }
      }
   }

   private int currentActorPower(BattleSkill s) {
      int magic = this.currentActorMagic();
      if (!SkillScaling.usesOffense(s)) {
         return magic;
      } else {
         CharStats st = TurnBattleClientMod.clientPlayerStats;
         if (this.realPlayerClass != null) {
            int recruitOrd = this.actingRecruitCode();
            if (recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               CharStats[] cStats = TurnBattleClientMod.customRecruitStats;
               if (cStats != null && ci < cStats.length && cStats[ci] != null) {
                  st = cStats[ci];
               }
            } else if (recruitOrd >= 0) {
               CharStats[] rStats = TurnBattleClientMod.clientRecruitStats;
               if (rStats != null && recruitOrd < rStats.length && rStats[recruitOrd] != null) {
                  st = rStats[recruitOrd];
               }
            }
         }

         return st != null ? st.offense : 0;
      }
   }

   private int currentActorMagic() {
      MonsterSkill.describingViewer = this.realPlayerClass == null;
      if (this.realPlayerClass == null) {
         return TurnBattleClientMod.clientPlayerStats.magic;
      } else {
         int recruitOrd = this.actingRecruitCode();
         if (recruitOrd >= 1000) {
            int ci = recruitOrd - 1000;
            CharStats[] cStats = TurnBattleClientMod.customRecruitStats;
            if (cStats != null && ci < cStats.length && cStats[ci] != null) {
               return cStats[ci].magic;
            }
         } else if (recruitOrd >= 0) {
            CharStats[] rStats = TurnBattleClientMod.clientRecruitStats;
            if (rStats != null && recruitOrd < rStats.length && rStats[recruitOrd] != null) {
               return rStats[recruitOrd].magic;
            }
         }

         return TurnBattleClientMod.clientPlayerStats.magic;
      }
   }

   private boolean currentActorFrozen() {
      for (BattleScreen.ParticipantInfo p : this.participants) {
         if (p.isMe()) {
            return p.isFrozen();
         }
      }

      return false;
   }

   private boolean currentActorStab(BattleSkill skill) {
      ItemStack weaponStack;
      if (this.realPlayerClass == null) {
         weaponStack = TurnBattleClientMod.clientEquippedWeapon;
      } else {
         int recruitOrd = this.actingRecruitCode();
         if (recruitOrd >= 1000) {
            int ci = recruitOrd - 1000;
            ItemStack[] cWeapons = TurnBattleClientMod.customRecruitWeapons;
            weaponStack = cWeapons != null && ci < cWeapons.length ? cWeapons[ci] : ItemStack.EMPTY;
         } else if (recruitOrd >= 0) {
            ItemStack[] rWeapons = TurnBattleClientMod.clientRecruitWeapons;
            weaponStack = rWeapons != null && recruitOrd < rWeapons.length ? rWeapons[recruitOrd] : ItemStack.EMPTY;
         } else {
            weaponStack = TurnBattleClientMod.clientEquippedWeapon;
         }
      }

      WeaponStats ws = WeaponStats.fromStack(weaponStack);
      if (ws == null) {
         return false;
      } else {
         for (Affinity a : skill.getAffinities()) {
            if (ws.hasElement(a)) {
               return true;
            }
         }

         return false;
      }
   }

   private OffhandGimmick currentActorGimmick() {
      if (this.realPlayerClass == null) {
         return this.offhandGimmick;
      } else {
         int recruitOrd = this.actingRecruitCode();
         if (recruitOrd >= 1000) {
            int ci = recruitOrd - 1000;
            ItemStack[] cAcc = TurnBattleClientMod.customRecruitAccessories;
            if (cAcc != null && ci < cAcc.length && cAcc[ci] != null) {
               return OffhandGimmick.fromStack(cAcc[ci]);
            }
         } else if (recruitOrd >= 0) {
            ItemStack[] rAcc = TurnBattleClientMod.clientRecruitAccessories;
            if (rAcc != null && recruitOrd < rAcc.length && rAcc[recruitOrd] != null) {
               return OffhandGimmick.fromStack(rAcc[recruitOrd]);
            }
         }

         return this.offhandGimmick;
      }
   }

   public void spawnDamageNumber(int slot, float amount) {
      this.spawnDamageNumber(slot, amount, null, "");
   }

   public void spawnDamageNumber(int slot, float amount, SoulType soul) {
      this.spawnDamageNumber(slot, amount, soul, "");
   }

   public void spawnDamageNumber(int slot, float amount, SoulType soul, String suffix) {
      this.spawnDamageNumber(slot, amount, soul, suffix, 0);
   }

   public void spawnDamageNumber(int slot, float amount, SoulType soul, String suffix, int tickOffset) {
      int rgb = soul != null ? soul.getColor() & 16777215 : 16733457;
      this.spawnEnemyNumber(slot, amount, rgb, "", suffix, tickOffset);
   }

   public void spawnMercyNumber(int slot, int pct) {
      this.spawnEnemyNumber(slot, (float)pct, 16768256, "+", "%", 0);
   }

   private void spawnEnemyNumber(int slot, float amount, int rgb, String prefix, String suffix, int tickOffset) {
      if (!(amount <= 0.0F) && slot >= 0 && slot < this.lastSpriteCX.length) {
         float rx = (float)(Math.random() * 28.0 - 14.0);
         int cx = this.lastSpriteCX[slot];
         int cy = this.lastSpriteCY[slot];
         if (cx == 0 && this.anchors3D()) {
            int total = Math.max(1, this.enemyRoster.size());
            int[] pos = BattleEntityRenderer.participantPos(slot, total, this.width, this.height, 82, false);
            cx = pos[0];
            cy = pos[1];
         }

         int bornTick = this.globalTick + tickOffset;
         long sameTimeCount = this.damageNumbers.getOrDefault(slot, Collections.emptyList()).stream().filter(d -> d.born() == bornTick).count();
         float x = (float)cx + rx;
         float y = (float)(cy - 10) - (float)sameTimeCount * 12.0F;
         this.damageNumbers
            .computeIfAbsent(slot, k -> new ArrayList<>())
            .add(new BattleScreen.DamageNumber(amount, x, y, bornTick, rgb, suffix, prefix, (float)cx, (float)cy));
      }
   }

   public void spawnHealNumber(int participantIdx, float amount) {
      this.spawnHealNumber(participantIdx, amount, false);
   }

   public void spawnHealNumber(int participantIdx, float amount, boolean gold) {
      if (!(amount <= 0.0F)) {
         int colour = gold ? 16755200 : 4521796;
         if (participantIdx >= 0 && participantIdx < this.lastParticipantCX.length) {
            int cx = this.lastParticipantCX[participantIdx];
            int cy = this.lastParticipantTopY[participantIdx];
            if (cx == 0 && this.anchors3D()) {
               int[] pos = BattleEntityRenderer.participantPos(participantIdx, Math.max(1, this.participants.size()), this.width, this.height, 82, true);
               cx = Math.max(55, pos[0]);
               cy = pos[1] - 30;
            }

            float rx = (float)(Math.random() * 20.0 - 10.0);
            float x = (float)cx + rx;
            float y = (float)(cy - 2);
            this.playerDamageNums
               .computeIfAbsent(participantIdx, k -> new ArrayList<>())
               .add(new BattleScreen.DamageNumber(-amount, x, y, this.globalTick, colour, "", "", (float)cx, (float)cy));
         }
      }
   }

   private void tickDamageNumbers() {
      for (List list : this.damageNumbers.values()) {
         list.removeIf(d -> this.globalTick - d.born() >= 40);
      }
   }

   private void renderChatBubbles(DrawContext ctx) {
      if (!this.chatBubbles.isEmpty()) {
         int padX = 6;
         int padY = 4;
         int maxBubbleW = 140;
         int bubbleGap = 6;
         Iterator var6 = this.chatBubbles.entrySet().iterator();

         while (true) {
            List bubbles;
            int idx;
            int cx;
            int baseY;
            while (true) {
               if (!var6.hasNext()) {
                  return;
               }

               Entry entry = (Entry<UUID, List<BattleScreen.ChatBubble>>)var6.next();
               UUID uid = entry.getKey();
               bubbles = entry.getValue();
               if (!bubbles.isEmpty()) {
                  Integer resolvedIdx = this.chatSenderIndex.get(uid);
                  if (resolvedIdx != null) {
                     idx = resolvedIdx;
                     if (idx <= -2) {
                        int oppIdx = -2 - idx;
                        if (oppIdx >= 0 && oppIdx < this.lastSpriteCX.length) {
                           cx = this.lastSpriteCX[oppIdx];
                           baseY = this.lastSpriteCY[oppIdx] - 20;
                           break;
                        }
                     } else if (idx >= 0 && idx < this.lastParticipantCX.length) {
                        cx = this.lastParticipantCX[idx];
                        baseY = this.lastParticipantTopY[idx] - 10;
                        break;
                     }
                  }
               }
            }

            int totalH = 0;

            for (BattleScreen.ChatBubble b : bubbles) {
               float prog = (float)(this.globalTick - b.born()) / 100.0F;
               if (!(prog >= 1.0F)) {
                  List lines = this.wrapDesc(b.text(), maxBubbleW - padX * 2);
                  totalH += 9 + 5 + lines.size() * 9 + padY * 2 + bubbleGap;
               }
            }

            int nameTagH = 9 + 5;
            int curY = baseY - totalH + nameTagH;

            for (BattleScreen.ChatBubble bx : bubbles) {
               float prog = (float)(this.globalTick - bx.born()) / 100.0F;
               if (!(prog >= 1.0F)) {
                  int alpha = prog < 0.7F ? 255 : (int)(255.0F * (1.0F - (prog - 0.7F) / 0.3F));
                  List lines = this.wrapDesc(bx.text(), maxBubbleW - padX * 2);
                  int bh = lines.size() * 9 + padY * 2;
                  int bw = 0;

                  for (String l : lines) {
                     bw = Math.max(bw, this.textRenderer.getWidth(l));
                  }

                  bw += padX * 2;
                  int bxx = cx - bw / 2;
                  int bgAlpha = (int)((float)alpha * 0.82F);
                  this.fillAlpha(ctx, bxx - 1, curY - 1, bxx + bw + 1, curY + bh + 1, bgAlpha, 0);
                  this.fillAlpha(ctx, bxx, curY, bxx + bw, curY + bh, bgAlpha, 1118515);
                  String nameTag = this.chatSenderName.getOrDefault(idx, "?");
                  int ntW = this.textRenderer.getWidth(nameTag) + 6;
                  int ntX = cx - ntW / 2;
                  int ntY = curY - 9 - 3;
                  this.fillAlpha(ctx, ntX - 1, ntY - 1, ntX + ntW + 1, ntY + 9 + 3, alpha, 0);
                  this.fillAlpha(ctx, ntX, ntY, ntX + ntW, ntY + 9 + 2, (int)((float)alpha * 0.8F), 4420);
                  int nameCol = alpha << 24 | 11197951;
                  ctx.drawTextWithShadow(this.textRenderer, Text.literal(nameTag), ntX + 3, ntY + 1, nameCol);
                  int textCol = alpha << 24 | 16777215;

                  for (int li = 0; li < lines.size(); li++) {
                     int lw = this.textRenderer.getWidth(lines.get(li));
                     ctx.drawTextWithShadow(this.textRenderer, Text.literal(lines.get(li)), cx - lw / 2, curY + padY + li * 9, textCol);
                  }

                  curY += 9 + 5 + bh + bubbleGap;
               }
            }
         }
      }
   }

   private void fillAlpha(DrawContext ctx, int x1, int y1, int x2, int y2, int alpha, int rgb) {
      ctx.fill(x1, y1, x2, y2, alpha << 24 | rgb);
   }

   private void renderChatInput(DrawContext ctx) {
      int boxW = Math.min(300, this.width - 40);
      int boxH = 18;
      int bx = (this.width - boxW) / 2;
      int by = this.height / 2 - boxH / 2 - 60;
      ctx.fill(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, -16777216);
      ctx.fill(bx, by, bx + boxW, by + boxH, -15658701);
      int borderCol = (int)(Math.sin((double)this.globalTick * 0.2) * 40.0 + 180.0);
      int border = 0xFF000000 | borderCol << 16 | borderCol << 8 | 0xFF;
      ctx.fill(bx - 1, by - 1, bx + boxW + 1, by, border);
      ctx.fill(bx - 1, by + boxH, bx + boxW + 1, by + boxH + 1, border);
      ctx.fill(bx - 1, by - 1, bx, by + boxH + 1, border);
      ctx.fill(bx + boxW, by - 1, bx + boxW + 1, by + boxH + 1, border);
      String label = "Say:";
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(label), bx + 3, by + (boxH - 9) / 2, -7820545);
      String display = this.chatInputText + (this.globalTick % 20 < 10 ? "|" : "");
      int textX = bx + 3 + this.textRenderer.getWidth(label) + 4;
      int maxTextW = boxW - 6 - this.textRenderer.getWidth(label) - 4;
      String clipped = display;

      while (clipped.length() > 0 && this.textRenderer.getWidth(clipped) > maxTextW) {
         clipped = clipped.substring(1);
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal(clipped), textX, by + (boxH - 9) / 2, -1);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Enter to send  \u2022  Esc to cancel"), this.width / 2, by + boxH + 3, -11184794);
   }

   private void renderDamageNumbers(DrawContext ctx, float delta) {
      for (Entry entry : this.damageNumbers.entrySet()) {
         for (BattleScreen.DamageNumber d : entry.getValue()) {
            float prog = ((float)(this.globalTick - d.born()) + delta) / 40.0F;
            if (!(prog >= 1.0F) && !(prog < 0.0F)) {
               boolean isHeal = d.amount() < 0.0F;
               String txt = d.prefix() + (isHeal ? (int)(-d.amount()) + d.suffix() : (int)d.amount() + d.suffix());
               float landX = d.startX() + 25.0F;
               float drawX;
               if (prog < 0.45F) {
                  float t = prog / 0.45F;
                  float ease = 1.0F - (1.0F - t) * (1.0F - t);
                  drawX = d.startX() + ease * 25.0F;
               } else {
                  drawX = landX;
               }

               float landY = d.startY() - 20.0F;
               float drawY;
               if (prog < 0.45F) {
                  float t = prog / 0.45F;
                  float arc = 4.0F * t * (1.0F - t);
                  drawY = d.startY() + (landY - d.startY()) * t - arc * 40.0F;
               } else if (prog < 0.7F) {
                  drawY = landY;
               } else {
                  float t = (prog - 0.7F) / 0.3F;
                  drawY = landY - t * 8.0F;
               }

               if (prog >= 0.45F && prog < 0.58F) {
                  float bt = (prog - 0.45F) / 0.13F;
                  drawY -= (float)Math.sin(Math.PI * (double)bt) * 5.0F;
               }

               float scaleX;
               float scaleY;
               if (prog < 0.15F) {
                  float t = easeOutBack(prog / 0.15F);
                  scaleX = 1.8F - t * 0.8F;
                  scaleY = 0.2F + t * 0.8F;
               } else if (prog < 0.7F) {
                  scaleX = 1.0F;
                  scaleY = 1.0F;
               } else {
                  float t = (prog - 0.7F) / 0.3F;
                  scaleX = 1.0F;
                  scaleY = 1.0F + t * 0.5F;
               }

               float alpha01;
               if (prog < 0.7F) {
                  alpha01 = 1.0F;
               } else {
                  alpha01 = 1.0F - (prog - 0.7F) / 0.3F;
               }

               int alpha = (int)(alpha01 * 255.0F);
               if (alpha > 0) {
                  int rgbNow = prog < 0.1F ? lerpRgbInt(16777215, d.rgb(), prog / 0.1F) : d.rgb();
                  int color = alpha << 24 | rgbNow;
                  MatrixStack matrices = ctx.getMatrices();
                  matrices.push();
                  this.followAnchor(matrices, d, entry.getKey(), false);
                  float dmgScale = 2.0F;
                  Style _ds = Style.EMPTY.withFont(DAMAGE_FONT);
                  int tw = (int)((float)this.textRenderer.getWidth(Text.literal(txt).setStyle(_ds)) * dmgScale);
                  float fontH = 9.0F * dmgScale;
                  matrices.translate(drawX, drawY + fontH * 0.5F, 0.0F);
                  matrices.scale(scaleX * dmgScale, scaleY * dmgScale, 1.0F);
                  this.drawDmgText(ctx, txt, -tw / 2, (int)(-fontH * 0.5F), color);
                  matrices.pop();
               }
            }
         }
      }
   }

   private void spawnPlayerDamageNumber(int participantIdx, float amount) {
      if (!(amount <= 0.0F) && participantIdx >= 0 && participantIdx < this.lastParticipantCX.length) {
         if (participantIdx < this.playerDamageTicks.length) {
            this.playerDamageTicks[participantIdx] = 12;
         }

         float rx = (float)(Math.random() * 20.0 - 10.0);
         int cx = this.lastParticipantCX[participantIdx];
         int cy = this.lastParticipantTopY[participantIdx];
         if (cx == 0 && this.anchors3D()) {
            int[] pos = BattleEntityRenderer.participantPos(participantIdx, Math.max(1, this.participants.size()), this.width, this.height, 82, true);
            cx = Math.max(55, pos[0]);
            cy = pos[1] - 30;
         }

         float x = (float)cx + rx;
         float y = (float)(cy - 2);
         this.playerDamageNums
            .computeIfAbsent(participantIdx, k -> new ArrayList<>())
            .add(new BattleScreen.DamageNumber(amount, x, y, this.globalTick, 16777215, "", "", (float)cx, (float)cy));
      }
   }

   private void renderPlayerDamageNumbers(DrawContext ctx, float delta) {
      for (Entry entry : this.playerDamageNums.entrySet()) {
         for (BattleScreen.DamageNumber d : entry.getValue()) {
            float prog = ((float)(this.globalTick - d.born()) + delta) / 40.0F;
            if (!(prog >= 1.0F) && !(prog < 0.0F)) {
               boolean isHeal = d.amount() < 0.0F;
               String txt = isHeal ? (int)(-d.amount()) + "" : (int)d.amount() + "";
               float drawX;
               if (prog < 0.45F) {
                  float t = prog / 0.45F;
                  float ease = 1.0F - (1.0F - t) * (1.0F - t);
                  drawX = d.startX() - ease * 10.0F;
               } else {
                  drawX = d.startX() - 10.0F;
               }

               float landY = d.startY() - 24.0F;
               float drawY;
               if (prog < 0.45F) {
                  float t = prog / 0.45F;
                  float arc = 4.0F * t * (1.0F - t);
                  drawY = d.startY() + (landY - d.startY()) * t - arc * 30.0F;
               } else if (prog < 0.7F) {
                  drawY = landY;
               } else {
                  float t = (prog - 0.7F) / 0.3F;
                  drawY = landY - t * 8.0F;
               }

               if (prog >= 0.45F && prog < 0.58F) {
                  float bt = (prog - 0.45F) / 0.13F;
                  drawY -= (float)Math.sin(Math.PI * (double)bt) * 4.0F;
               }

               float scaleX = 1.0F;
               float scaleY = 1.0F;
               if (prog < 0.12F) {
                  float t = easeOutBack(prog / 0.12F);
                  scaleX = 1.6F - t * 0.6F;
                  scaleY = 0.3F + t * 0.7F;
               } else if (prog >= 0.7F) {
                  float t = (prog - 0.7F) / 0.3F;
                  scaleX = 1.0F;
                  scaleY = 1.0F + t * 0.5F;
               }

               float alpha01 = prog < 0.7F ? 1.0F : 1.0F - (prog - 0.7F) / 0.3F;
               int alpha = (int)(alpha01 * 255.0F);
               if (alpha > 0) {
                  int color = alpha << 24 | (isHeal ? d.rgb() : 16777215);
                  float pdmgScale = 2.0F;
                  Style _ps = Style.EMPTY.withFont(DAMAGE_FONT);
                  int tw = (int)((float)this.textRenderer.getWidth(Text.literal(txt).setStyle(_ps)) * pdmgScale);
                  float fontH = 9.0F * pdmgScale;
                  MatrixStack matrices = ctx.getMatrices();
                  matrices.push();
                  this.followAnchor(matrices, d, entry.getKey(), true);
                  matrices.translate(drawX, drawY + fontH * 0.5F, 0.0F);
                  matrices.scale(scaleX * pdmgScale, scaleY * pdmgScale, 1.0F);
                  this.drawDmgText(ctx, txt, -tw / 2, (int)(-fontH * 0.5F), color);
                  matrices.pop();
               }
            }
         }
      }
   }

   public void checkExtraDamage(List<BattleScreen.EnemyInfo> incoming) {
      for (BattleScreen.EnemyInfo fresh : incoming) {
         for (int slot = 0; slot < this.extraEnemies.size(); slot++) {
            if (this.extraEnemies.get(slot).networkId() == fresh.networkId() && fresh.hp() < this.extraEnemies.get(slot).hp()) {
               int tickSlot = slot + 1;
               if (this.anchors3D()) {
                  int resolved = this.rosterSlotForId(fresh.networkId());
                  if (resolved >= 0) {
                     tickSlot = resolved;
                  }
               }

               if (tickSlot < this.damageTicks.length) {
                  this.damageTicks[tickSlot] = 12;
               }

               SoulType attackerSoul = this.playerSoul;

               for (BattleScreen.ParticipantInfo pip : this.participants) {
                  if (pip.isCurrentTurn() && pip.soul() != null) {
                     attackerSoul = pip.soul();
                     break;
                  }
               }

               this.spawnDamageNumber(tickSlot, this.extraEnemies.get(slot).hp() - fresh.hp(), attackerSoul);
               break;
            }
         }
      }

      boolean mercySound = false;

      for (BattleScreen.EnemyInfo fresh : incoming) {
         for (int slotx = 0; slotx < this.extraEnemies.size(); slotx++) {
            BattleScreen.EnemyInfo old = this.extraEnemies.get(slotx);
            if (old.networkId() == fresh.networkId()) {
               if (fresh.spareThreshold() > old.spareThreshold() + 0.001F) {
                  int tickSlotx = slotx + 1;
                  if (this.anchors3D()) {
                     int resolved = this.rosterSlotForId(fresh.networkId());
                     if (resolved >= 0) {
                        tickSlotx = resolved;
                     }
                  }

                  this.spawnMercyNumber(tickSlotx, Math.max(1, Math.round((fresh.spareThreshold() - old.spareThreshold()) * 100.0F)));
                  mercySound = true;
               }
               break;
            }
         }
      }

      if (mercySound) {
         TurnBattleSounds.playClient(TurnBattleSounds.SND_MERCYADD, 1.0F, 1.0F);
      }
   }

   public void setExtraEnemies(List<BattleScreen.EnemyInfo> extras) {
      this.extraEnemies.clear();
      this.extraEnemies.addAll(extras);
   }

   public void setEnemyRoster(List<BattleScreen.EnemyInfo> roster) {
      this.enemyRoster.clear();
      this.enemyRoster.addAll(roster);
   }

   public void setCurrentPrimaryId(int id) {
      this.currentPrimaryEnemyId = id;
   }

   private int rosterSlotForId(int networkId) {
      for (int i = 0; i < this.enemyRoster.size(); i++) {
         if (this.enemyRoster.get(i).networkId() == networkId) {
            return i;
         }
      }

      return -1;
   }

   public List<BattleScreen.ParticipantInfo> getParticipants() {
      return this.participants;
   }

   public List<BattleScreen.OpponentInfo> getPvpOpponents() {
      return this.pvpOpponents;
   }

   public List<UUID> getParticipantUuids() {
      return this.participantUuids;
   }

   public List<BattleScreen.EnemyInfo> getEnemies() {
      return this.enemyRoster;
   }

   public boolean isEnemyHurt(int slot) {
      return slot >= 0 && slot < this.damageTicks.length && this.damageTicks[slot] > 0;
   }

   public int getEnemyShakeX(int slot) {
      if (slot >= 0 && slot < this.damageTicks.length && this.damageTicks[slot] > 0) {
         float progress = (float)this.damageTicks[slot] / 12.0F;
         return (int)(Math.sin((double)this.damageTicks[slot] * 1.8) * 4.0 * (double)progress);
      } else {
         return 0;
      }
   }

   public int getPlayerShakeX(int slot) {
      if (slot >= 0 && slot < this.playerDamageTicks.length && this.playerDamageTicks[slot] > 0) {
         float progress = (float)this.playerDamageTicks[slot] / 12.0F;
         return (int)(Math.sin((double)this.playerDamageTicks[slot] * 1.8) * 4.0 * (double)progress);
      } else {
         return 0;
      }
   }

   public boolean isPvpMode() {
      return this.pvpMode;
   }

   public float getEntityHp() {
      return this.entityHp;
   }

   public float getEntityMaxHp() {
      return this.entityMaxHp;
   }

   public String getEntityName() {
      return this.entityName;
   }

   boolean anchors3D() {
      return true;
   }

   public boolean isOverhaul() {
      return this.overhaul;
   }

   public void setOverhaul(boolean on) {
      this.overhaul = on;
      if (!on) {
         OverhaulView.end();
      } else {
         if (this.phase != BattleScreen.Phase.BATTLE_INTRO && !OverhaulView.isActive()) {
            OverhaulView.begin();
         }
      }
   }

   private static boolean isAllySkillType(SkillType t) {
      return t == SkillType.HEAL || t == SkillType.SHIELD || t == SkillType.BUFF || t == SkillType.MAGNET;
   }

   private Entity ohFindParticipant(int i) {
      if (this.client == null || this.client.world == null) {
         return null;
      } else if (this.participants.isEmpty()) {
         return i == 0 ? this.client.player : null;
      } else if (i >= 0 && i < this.participants.size()) {
         BattleScreen.ParticipantInfo p = this.participants.get(i);
         UUID u = i < this.participantUuids.size() ? this.participantUuids.get(i) : null;
         if (!BattleEntityRenderer.isRecruitUuid(u)) {
            Entity e = this.client.world.getPlayerByUuid(u);
            if (e != null) {
               return e;
            }
         } else {
            BattleRecruitEntity fallback = null;

            for (Entity e : this.client.world.getEntities()) {
               if (e instanceof BattleRecruitEntity) {
                  BattleRecruitEntity r = (BattleRecruitEntity)e;
                  if (p.name().equals(r.getRecruitName())) {
                     UUID owner = r.getOwner();
                     if (owner != null && (this.participantUuids.contains(owner) || this.client.player != null && owner.equals(this.client.player.getUuid()))) {
                        return r;
                     }

                     if (fallback == null) {
                        fallback = r;
                     }
                  }
               }
            }

            if (fallback != null) {
               return fallback;
            }
         }

         return p.isMe() ? this.client.player : null;
      } else {
         return null;
      }
   }

   private UUID ohKey(int i) {
      if (!this.participants.isEmpty()) {
         if (i >= 0 && i < this.participants.size()) {
            UUID u = i < this.participantUuids.size() ? this.participantUuids.get(i) : null;
            return BattleEntityRenderer.isRecruitUuid(u) ? BattleEntityRenderer.recruitKey(this.participants.get(i).name()) : u;
         } else {
            return null;
         }
      } else {
         return this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
      }
   }

   private Entity ohPartyEntity(int i) {
      return i >= 0 && i < this.ohParty.size() ? this.ohParty.get(i) : null;
   }

   private Entity ohEnemyEntity(int networkId) {
      return this.client != null && this.client.world != null && networkId >= 0 ? this.client.world.getEntityById(networkId) : null;
   }

   private int ohActiveIndex() {
      for (int i = 0; i < this.participants.size(); i++) {
         if (this.participants.get(i).isCurrentTurn()) {
            return i;
         }
      }

      return this.participants.isEmpty() ? 0 : -1;
   }

   private boolean ohPlayerPhase() {
      return this.phase == BattleScreen.Phase.PLAYER_TURN
         || this.phase == BattleScreen.Phase.SKILLS_MENU
         || this.phase == BattleScreen.Phase.GOODS_MENU
         || this.phase == BattleScreen.Phase.ACT_MENU
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY
         || this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY
         || this.phase == BattleScreen.Phase.ALL_OUT
         || this.phase == BattleScreen.Phase.BATON_PASS;
   }

   private List<BattleScreen.OhEnemy> ohEnemies() {
      List out = new ArrayList<>();
      int prim = this.currentPrimaryEnemyId >= 0 ? this.currentPrimaryEnemyId : this.entityNetworkId;
      List ids = new ArrayList<>();
      if (!this.enemyRoster.isEmpty()) {
         for (BattleScreen.EnemyInfo r : this.enemyRoster) {
            ids.add(r.networkId());
         }
      } else {
         ids.add(prim);

         for (BattleScreen.EnemyInfo e : this.extraEnemies) {
            ids.add(e.networkId());
         }
      }

      for (int slot = 0; slot < ids.size(); slot++) {
         int id = ids.get(slot);
         String name = null;
         float hp = 0.0F;
         float max = 0.0F;
         float spare = 0.0F;
         int legacy = -1;
         if (id == prim) {
            name = this.entityName;
            hp = this.displayedEntityHp >= 0.0F ? this.displayedEntityHp : this.entityHp;
            max = this.entityMaxHp;
            spare = this.spareThreshold;
            legacy = 0;
         } else {
            for (int j = 0; j < this.extraEnemies.size(); j++) {
               BattleScreen.EnemyInfo e = this.extraEnemies.get(j);
               if (e.networkId() == id) {
                  name = e.name();
                  hp = this.displayedExtraHp.getOrDefault(j, e.hp());
                  max = e.maxHp();
                  spare = e.spareThreshold();
                  legacy = j + 1;
                  break;
               }
            }
         }

         if (name == null && slot < this.enemyRoster.size()) {
            BattleScreen.EnemyInfo r = this.enemyRoster.get(slot);
            name = r.name();
            hp = r.hp();
            max = r.maxHp();
         }

         if (name != null) {
            out.add(new BattleScreen.OhEnemy(slot, id, name, hp, max, spare, legacy));
         }
      }

      return out;
   }

   private Entity ohFxEntity() {
      if (this.fxTarget < 0) {
         return null;
      } else if (this.fxTarget < 10 && !isAllySkillType(this.fxSkillType)) {
         return this.ohEnemyEntity(this.fxTargetNetId >= 0 ? this.fxTargetNetId : this.networkIdForLegacyTarget(this.fxTarget));
      } else {
         int idx = this.fxTarget >= 10 ? this.fxTarget - 10 : 0;
         return this.ohPartyEntity(Math.max(0, Math.min(idx, Math.max(0, this.participants.size() - 1))));
      }
   }

   private void overhaulTick() {
      if (this.overhaul && this.client != null && this.client.world != null) {
         this.ohParty.clear();
         List party = new ArrayList<>();
         Set keys = new HashSet<>();
         int n = Math.max(1, this.participants.size());
         int active = this.ohActiveIndex();
         UUID turnKey = null;

         for (int i = 0; i < n; i++) {
            Entity e = this.ohFindParticipant(i);
            this.ohParty.add(e);
            if (e != null) {
               party.add(e);
            }

            UUID key = this.ohKey(i);
            if (key != null) {
               keys.add(key);
               if (i < this.participants.size()) {
                  BattleScreen.ParticipantInfo p = this.participants.get(i);
                  float hp = p.isMe() ? this.playerHp : p.hp();
                  CloneAnimator.observeHp(key, hp, hp <= 0.0F);
                  CloneAnimator.setSoul(key, p.soul());
               }

               if (i == active) {
                  turnKey = key;
               }
            }
         }

         if (OverhaulView.isActive()) {
            List enemies = new ArrayList<>();

            for (BattleScreen.OhEnemy en : this.ohEnemies()) {
               Entity ex = this.ohEnemyEntity(en.networkId());
               MobFates.Fate fate = MobFates.get(en.networkId());
               if (en.hp() > 0.0F) {
                  if (fate != null && fate.auto) {
                     MobFates.drop(en.networkId());
                  }
               } else if (fate == null && ex instanceof LivingEntity le) {
                  MobFates.autoKill(en.networkId(), le);
               }

               if (!(en.hp() <= 0.0F) && ex != null) {
                  enemies.add(ex);
               }
            }

            OverhaulView.setScene(party, enemies, keys, turnKey);
            Entity actor = this.ohPartyEntity(active);
            boolean directAttack = this.battleStyle == BattleStyle.DIRECT_DAMAGE
               && (
                  this.directHitEffect != null
                     || !this.otherDirectHitEffects.isEmpty()
                     || this.phase == BattleScreen.Phase.TRANSITION && "wait".equals(this.pendingAfterTransition)
               );
            if (directAttack) {
               OverhaulView.setShot(OverhaulView.Shot.ENEMY_TURN, actor, null);
               OverhaulView.tickCamera();
            } else {
               for (BattleScreen.OhEnemy en : this.ohEnemies()) {
                  MobFates.Fate f = MobFates.get(en.networkId());
                  if (MobFates.playing(f) && f.e != null) {
                     OverhaulView.setShot(OverhaulView.Shot.TARGET, actor, f.e);
                     OverhaulView.tickCamera();
                     return;
                  }
               }

               switch (this.phase) {
                  case ENEMY_TURN:
                  case ENEMY_TURN_END:
                     OverhaulView.setShot(OverhaulView.Shot.ENEMY_TURN, actor, null);
                     break;
                  case TRANSITION:
                  case RESULT:
                  default:
                     OverhaulView.setShot(OverhaulView.Shot.ACTOR, actor, null);
                     break;
                  case SKILL_EFFECT:
                     Entity t = this.activeEffect == null && this.activeBashEffect == null ? null : this.ohFxEntity();
                     boolean onAlly = t != null && (this.fxTarget >= 10 || isAllySkillType(this.fxSkillType));
                     if (onAlly && t != actor) {
                        OverhaulView.setShot(OverhaulView.Shot.ALLY, actor, t);
                     } else {
                        OverhaulView.setShot(OverhaulView.Shot.ACTOR, actor, onAlly ? null : t);
                     }
                     break;
                  case BATTLE_INTRO:
                     OverhaulView.setShot(OverhaulView.Shot.INTRO, actor, null);
                     break;
                  case TARGET_SELECT_ENEMY:
                     OverhaulView.setShot(OverhaulView.Shot.TARGET, actor, this.ohEnemyEntity(this.networkIdForLegacyTarget(this.targetNavIdx)));
                     break;
                  case TARGET_SELECT_ALLY:
                     OverhaulView.setShot(OverhaulView.Shot.ALLY, actor, this.ohPartyEntity(this.targetNavIdx));
               }

               OverhaulView.tickCamera();
            }
         }
      }
   }

   private void overhaulAnchors(float td) {
      if (this.client != null && this.client.world != null) {
         for (BattleScreen.OhEnemy en : this.ohEnemies()) {
            if (en.slot() < this.lastSpriteCX.length) {
               float[] c = OverhaulView.projectEntity(this.ohEnemyEntity(en.networkId()), 0.55F, td);
               if (c != null) {
                  this.lastSpriteCX[en.slot()] = Math.round(c[0]);
                  this.lastSpriteCY[en.slot()] = Math.round(c[1]) + 20;
               }
            }
         }

         for (int i = 0; i < this.ohParty.size() && i < this.lastParticipantCX.length; i++) {
            float[] c = OverhaulView.projectEntity(this.ohParty.get(i), 0.72F, td);
            if (c != null) {
               this.lastParticipantCX[i] = Math.round(c[0]);
               this.lastParticipantTopY[i] = Math.round(c[1]);
            }
         }

         if (this.directHitEffect != null) {
            this.anchorOnParty(this.directHitEffect, this.directHitMyIdx, td);
         }

         for (BattleScreen.OtherDirectHit odh : this.otherDirectHitEffects) {
            this.anchorOnParty(odh.effect(), odh.participantIdx(), td);
         }

         Entity t = this.activeEffect == null && this.activeBashEffect == null ? null : this.ohFxEntity();
         float[] c = t != null ? OverhaulView.projectEntity(t, 0.55F, td) : null;
         if (c != null) {
            float sc = OverhaulView.effectScale(c[2]);
            if (this.activeEffect != null) {
               this.activeEffect.setCenter(Math.round(c[0]), Math.round(c[1]));
               this.activeEffect.scale = sc;
            }

            if (this.activeBashEffect != null) {
               this.activeBashEffect.setCenter(Math.round(c[0]), Math.round(c[1]));
               this.activeBashEffect.scale = sc;
            }
         }
      }
   }

   private void anchorOnParty(SkillEffect fx, int idx, float td) {
      if (fx != null && idx >= 0 && idx < this.ohParty.size()) {
         float[] c = OverhaulView.projectEntity(this.ohParty.get(idx), 0.62F, td);
         if (c != null) {
            fx.setCenter(Math.round(c[0]), Math.round(c[1]));
            fx.scale = OverhaulView.effectScale(c[2]);
         }
      }
   }

   private void followAnchor(MatrixStack m, BattleScreen.DamageNumber d, int idx, boolean party) {
      if (this.overhaul && !Float.isNaN(d.ax()) && idx >= 0) {
         int[] xs = party ? this.lastParticipantCX : this.lastSpriteCX;
         int[] ys = party ? this.lastParticipantTopY : this.lastSpriteCY;
         if (idx < xs.length && xs[idx] != 0) {
            m.translate((float)xs[idx] - d.ax(), (float)ys[idx] - d.ay(), 0.0F);
         }
      }
   }

   private void overhaulIntroTargets() {
      int count = Math.max(1, this.participants.isEmpty() ? 1 : this.participants.size());

      for (int i = 0; i < count && i < 8; i++) {
         float[] c = OverhaulView.projectEntity(this.ohPartyEntity(i), 0.62F, 1.0F);
         if (c != null) {
            this.introParticipantTargetX[i] = c[0] - 4.5F;
            this.introParticipantTargetY[i] = c[1] - 4.5F;
         }
      }
   }

   private void renderOverhaul(DrawContext ctx, int mx, int my) {
      this.lastRenderScale = 1.0F;
      float td = this.lastRenderDelta;
      this.overhaulAnchors(td);
      OverhaulView.drawWorldFx(ctx);
      this.renderOverhaulPlates(ctx, td);
      this.renderDamageNumbers(ctx, td);
      this.renderTpMeter(ctx);
      this.renderRogueTimer(ctx);
      this.renderDarkFountainEncounterCard(ctx);
      switch (this.phase) {
         case PLAYER_TURN:
            if (!this.mercySubmenuOpen && !this.skillsSubmenuOpen) {
               this.renderOverhaulCaption(ctx, null);
            } else {
               this.renderTextBox(ctx);
            }

            this.drawOverhaulColumn(ctx);
            super.render(ctx, mx, my, td);
            break;
         case SKILLS_MENU:
         case GOODS_MENU:
            this.renderTextBox(ctx);
            this.drawOverhaulColumn(ctx);
            super.render(ctx, mx, my, td);
            break;
         case ACT_MENU:
            this.renderTextBox(ctx);
            this.drawOverhaulColumn(ctx);
            super.render(ctx, mx, my, td);
            this.renderActMenu(ctx);
            break;
         case ENEMY_TURN:
            this.renderAnimatedBattleBox(ctx, td);
            this.drawDeltarunePanel(ctx, this.height);
            super.render(ctx, mx, my, td);
            this.bulletHell.renderEnemyPhase(ctx, td);
            break;
         case ENEMY_TURN_END:
            this.renderAnimatedBattleBox(ctx, td);
            this.drawDeltarunePanel(ctx, this.height);
            super.render(ctx, mx, my, td);
            break;
         case TRANSITION:
            this.renderOverhaulCaption(ctx, null);
            super.render(ctx, mx, my, td);
            break;
         case RESULT:
            this.renderTextBox(ctx);
            break;
         case SKILL_EFFECT:
            if (this.qteActive) {
               this.renderTextBox(ctx);
            } else {
               this.renderOverhaulCaption(ctx, null);
            }

            super.render(ctx, mx, my, td);
            if (this.activeBashEffect != null) {
               this.activeBashEffect.render(ctx, td);
            }

            if (this.activeEffect != null) {
               this.activeEffect.render(ctx, td);
            }
         case BATTLE_INTRO:
         default:
            break;
         case TARGET_SELECT_ENEMY:
            this.drawOverhaulColumn(ctx);
            super.render(ctx, mx, my, td);
            String name = null;

            for (BattleScreen.OhEnemy en : this.ohEnemies()) {
               if (en.legacyIdx() == this.targetNavIdx) {
                  name = en.name();
                  break;
               }
            }

            this.renderOverhaulCaption(ctx, name != null ? "Target: " + name + "   (A / D to switch)" : "Choose a target");
            break;
         case TARGET_SELECT_ALLY:
            this.drawOverhaulColumn(ctx);
            super.render(ctx, mx, my, td);
            String name = this.targetNavIdx >= 0 && this.targetNavIdx < this.participants.size() ? this.participants.get(this.targetNavIdx).name() : null;
            this.renderOverhaulCaption(ctx, name != null ? "Target: " + name + "   (A / D to switch)" : "Choose a target");
            break;
         case ALL_OUT:
            this.renderAllOutPrompt(ctx, mx, my);
            break;
         case BATON_PASS:
            this.renderBatonPassPrompt(ctx, mx, my);
      }

      if (this.directHitEffect != null) {
         this.directHitEffect.render(ctx, td);
      }

      for (BattleScreen.OtherDirectHit odh : this.otherDirectHitEffects) {
         odh.effect().render(ctx, td);
      }

      if ((this.boxShrinkPlaying || this.boxRetreatFx) && this.phase != BattleScreen.Phase.ENEMY_TURN && this.phase != BattleScreen.Phase.ENEMY_TURN_END) {
         this.renderAnimatedBattleBox(ctx, td);
      }

      this.renderSoulReturn(ctx, td);
      this.renderSoulLeave(ctx, td);
      this.renderPlayerGrazePulses(ctx);
      BossSpeech.render(ctx, this.textRenderer, this.lastSpriteCX[0], this.lastSpriteTopY[0] + 24, this.width, this.height);
      if (this.phase != BattleScreen.Phase.ENEMY_TURN
         && this.phase != BattleScreen.Phase.ENEMY_TURN_END
         && this.phase != BattleScreen.Phase.BATTLE_INTRO
         && this.phase != BattleScreen.Phase.RESULT) {
         this.renderChatBubbles(ctx);
         if (this.chatInputActive) {
            this.renderChatInput(ctx);
         }
      }

      this.renderPlayerDamageNumbers(ctx, td);
      boolean inBulletHellNow = this.phase == BattleScreen.Phase.ENEMY_TURN;
      float[] me = this.playerSoulAnchor();
      DeathSoulTracker.update(inBulletHellNow, inBulletHellNow ? this.soulX + 4.5F : me[0] + 4.5F, inBulletHellNow ? this.soulY + 4.5F : me[1] + 4.5F);
   }

   private void drawOverhaulColumn(DrawContext ctx) {
      int ai = this.ohActiveIndex();
      if (ai >= 0 && ai < this.participants.size() && this.ohPlayerPhase()) {
         BattleScreen.ParticipantInfo p = this.participants.get(ai);
         Entity e = this.ohPartyEntity(ai);
         float td = this.lastRenderDelta;
         float[] sh = OverhaulView.projectEntity(e, 0.78F, td);
         float[] hd = OverhaulView.projectEntity(e, 1.08F, td);
         float tx;
         float ty;
         if (sh != null && hd != null) {
            float halfBody = 0.4F * sh[2];
            tx = sh[0] - halfBody - 10.0F - 150.0F;
            ty = sh[1] - 26.5F;
            if (tx < 4.0F) {
               tx = hd[0] - 75.0F;
               ty = hd[1] - 53.0F - 8.0F;
            }
         } else {
            tx = (float)(this.width - 150) / 2.0F;
            ty = (float)(this.height - 53 - 6);
         }

         boolean boxOpen = this.phase == BattleScreen.Phase.SKILLS_MENU
            || this.phase == BattleScreen.Phase.GOODS_MENU
            || this.phase == BattleScreen.Phase.ACT_MENU
            || this.phase == BattleScreen.Phase.PLAYER_TURN && (this.mercySubmenuOpen || this.skillsSubmenuOpen);
         int maxY = (boxOpen ? this.height - 82 - 4 : this.height - 4) - 53;
         tx = Math.max(4.0F, Math.min((float)(this.width - 150 - 4), tx));
         ty = Math.max(24.0F, Math.min((float)maxY, ty));
         if (this.ohColFor == ai && !Float.isNaN(this.ohColX)) {
            this.ohColX = this.ohColX + (tx - this.ohColX) * 0.3F;
            this.ohColY = this.ohColY + (ty - this.ohColY) * 0.3F;
         } else {
            this.ohColX = tx;
            this.ohColY = ty;
            this.ohColFor = ai;
         }

         int colX = Math.round(this.ohColX);
         int colY = Math.round(this.ohColY);
         float animH = ai < this.animColH.length && this.animColH[ai] > 0.0F ? this.animColHPrev[ai] + (this.animColH[ai] - this.animColHPrev[ai]) * td : 53.0F;
         int slide = Math.max(0, 53 - Math.round(animH));
         ctx.fill(colX - 2, colY + slide - 2, colX + 150 + 2, colY + 53 + 2, 1711276032);
         this.drawParticipantColumn(ctx, ai, p, colX, colY + slide, 150, 53, true, colY + 53, true);
      } else {
         this.ohColFor = -1;
      }
   }

   private void renderOverhaulCaption(DrawContext ctx, String text) {
      if (text == null) {
         if (!this.twCurrent.isEmpty()) {
            text = this.twCurrent.substring(0, Math.min(this.twCharIdx, this.twCurrent.length()));
         } else if (!this.messages.isEmpty()) {
            text = this.messages.get(this.messages.size() - 1);
         }

         if (text == null || text.isEmpty()) {
            return;
         }

         text = "* " + text;
      }

      int maxW = Math.min(this.width - 40, 360);
      List lines = new ArrayList<>();
      this.wrapText(text, maxW, lines);

      while (lines.size() > 2) {
         lines.remove(0);
      }

      int lh = 9 + 2;
      int h = lines.size() * lh + 8;
      int w = 0;

      for (String l : lines) {
         w = Math.max(w, this.textRenderer.getWidth(l));
      }

      w += 16;
      int x = (this.width - w) / 2;
      int y = 6;
      ctx.fill(x, y, x + w, y + h, -1610612736);
      ctx.fill(x, y + h - 1, x + w, y + h, 1627389951);

      for (int i = 0; i < lines.size(); i++) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(lines.get(i)), x + 8, y + 5 + i * lh, -1);
      }
   }

   private void renderOverhaulPlates(DrawContext ctx, float td) {
      if (this.client != null && this.client.world != null && this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY) {
         boolean showMercy = !this.pvpMode && !TurnBattleClientMod.isRuthless;

         for (BattleScreen.OhEnemy en : this.ohEnemies()) {
            if (!(en.hp() <= 0.0F) && en.legacyIdx() == this.targetNavIdx) {
               Entity e = this.ohEnemyEntity(en.networkId());
               float[] top = OverhaulView.projectEntity(e, 1.0F, td);
               if (top != null) {
                  boolean boss = en.networkId() == this.currentPrimaryEnemyId && this.bossMaxLives > 0;
                  this.drawPlate(
                     ctx,
                     Math.round(top[0]),
                     Math.round(top[1] - 0.25F * top[2]),
                     en.name(),
                     en.hp(),
                     en.maxHp(),
                     -2285022,
                     showMercy ? en.spare() : -1.0F,
                     true,
                     en.spare() >= 1.0F && showMercy,
                     boss ? this.bossLivesRemaining : 0,
                     boss ? this.bossMaxLives : 0,
                     this.pvpMode ? List.of() : this.enemyStatusTags(en.legacyIdx())
                  );
               }
            }
         }
      }
   }

   private void drawPlate(
      DrawContext ctx, int cx, int bottomY, String name, float hp, float maxHp, int hpColor, float mercy, boolean selected, boolean spareReady
   ) {
      this.drawPlate(ctx, cx, bottomY, name, hp, maxHp, hpColor, mercy, selected, spareReady, 0, 0, List.of());
   }

   private void drawPlate(
      DrawContext ctx,
      int cx,
      int bottomY,
      String name,
      float hp,
      float maxHp,
      int hpColor,
      float mercy,
      boolean selected,
      boolean spareReady,
      int lives,
      int maxLives,
      List<Object[]> statuses
   ) {
      int BAR_W = 56;
      int BAR_H = 4;
      float ts = 0.75F;
      int nameW = Math.round((float)this.textRenderer.getWidth(name) * ts);
      float ss = 0.6F;
      int tagW = 0;

      for (Object[] t : statuses) {
         tagW += Math.round((float)this.textRenderer.getWidth((String)t[0]) * ss) + 6;
      }

      int w = Math.max(Math.max(56, nameW), Math.min(tagW, 140)) + 8;
      List tagRows = new ArrayList<>();
      int rowW = 0;

      for (Object[] t : statuses) {
         int tw = Math.round((float)this.textRenderer.getWidth((String)t[0]) * ss) + 6;
         if (tagRows.isEmpty() || rowW + tw > w - 8) {
            tagRows.add(new ArrayList<>());
            rowW = 0;
         }

         tagRows.get(tagRows.size() - 1).add(t);
         rowW += tw;
      }

      int extraH = (maxLives > 0 ? 8 : 0) + tagRows.size() * 8;
      int h = Math.round(9.0F * ts) + 3 + 4 + (mercy >= 0.0F ? 4 : 0) + 6 + extraH;
      int x = cx - w / 2;
      int y = bottomY - h;
      if (x + w >= 0 && x <= this.width && y + h >= 0 && y <= this.height) {
         ctx.fill(x, y, x + w, y + h, selected ? -1071638016 : -1879048192);
         if (selected) {
            int pulse = (int)(160.0 + 95.0 * Math.sin((double)((float)this.globalTick + this.lastRenderDelta) * 0.3));
            int edge = pulse << 24 | 16768256;
            ctx.fill(x - 1, y - 1, x + w + 1, y, edge);
            ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, edge);
            ctx.fill(x - 1, y, x, y + h, edge);
            ctx.fill(x + w, y, x + w + 1, y + h, edge);
            Identifier sTex = this.playerSoul != null ? this.playerSoul.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
            int bob = (int)Math.round(Math.sin((double)((float)this.globalTick + this.lastRenderDelta) * 0.25) * 2.0);
            RenderSystem.enableBlend();
            ctx.drawTexture(sTex, cx - 4, y - 9 - 4 + bob, 0.0F, 0.0F, 9, 9, 9, 9);
         }

         int nameCol = selected ? -188 : (spareReady ? -8960 : -1);
         ctx.getMatrices().push();
         ctx.getMatrices().translate((float)cx - (float)nameW / 2.0F, (float)(y + 3), 0.0F);
         ctx.getMatrices().scale(ts, ts, 1.0F);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(name), 0, 0, nameCol);
         ctx.getMatrices().pop();
         int by = y + 3 + Math.round(9.0F * ts) + 2;
         int bx = cx - 28;
         float frac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, hp / maxHp)) : 0.0F;
         ctx.fill(bx - 1, by - 1, bx + 56 + 1, by + 4 + 1, -15658735);
         ctx.fill(bx, by, bx + 56, by + 4, -13430511);
         int fill = Math.round(56.0F * frac);
         if (fill > 0) {
            ctx.fill(bx, by, bx + fill, by + 4, hpColor);
            ctx.fill(bx, by, bx + fill, by + 1, lerpColor(hpColor, -1, 0.4F));
         }

         int ry = by + 4 + 2;
         if (mercy >= 0.0F) {
            int mf = Math.round(56.0F * Math.min(1.0F, mercy));
            ctx.fill(bx, ry, bx + 56, ry + 2, -13426176);
            if (mf > 0) {
               ctx.fill(bx, ry, bx + mf, ry + 2, -8960);
            }

            ry += 4;
         }

         if (maxLives > 0) {
            float dx = 7.0F;
            float x0 = (float)cx - (float)(maxLives - 1) * dx / 2.0F;

            for (int i = 0; i < maxLives; i++) {
               CloneAnimator.diamond(ctx, x0 + (float)i * dx, (float)(ry + 4), 3.1F, -16777216);
               CloneAnimator.diamond(ctx, x0 + (float)i * dx, (float)(ry + 4), 2.3F, i < lives ? -42406 : -11193549);
            }

            ry += 8;
         }

         for (List row : tagRows) {
            int rw = 0;

            for (Object[] t : row) {
               rw += Math.round((float)this.textRenderer.getWidth((String)t[0]) * ss) + 6;
            }

            int tx = cx - rw / 2;

            for (Object[] t : row) {
               String label = (String)t[0];
               int col = (Integer)t[1];
               int tw = Math.round((float)this.textRenderer.getWidth(label) * ss) + 4;
               ctx.fill(tx, ry, tx + tw, ry + 7, -872415232);
               ctx.fill(tx, ry, tx + tw, ry + 1, col);
               ctx.getMatrices().push();
               ctx.getMatrices().translate((float)(tx + 2), (float)ry + 1.5F, 0.0F);
               ctx.getMatrices().scale(ss, ss, 1.0F);
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(label), 0, 0, col);
               ctx.getMatrices().pop();
               tx += tw + 2;
            }

            ry += 8;
         }
      }
   }

   public void set3DEnemyPos(int slot, int cx, int cy) {
      if (slot >= 0 && slot < this.lastSpriteCX.length) {
         this.lastSpriteCX[slot] = cx;
         this.lastSpriteCY[slot] = cy - 20;
      }
   }

   public void set3DEnemyTop(int slot, int topY) {
      if (slot >= 0 && slot < this.lastSpriteTopY.length) {
         this.lastSpriteTopY[slot] = topY;
      }
   }

   public void set3DPlayerPos(int idx, int cx, int cy) {
      if (idx >= 0 && idx < this.lastParticipantCX.length) {
         this.lastParticipantCX[idx] = cx;
         this.lastParticipantTopY[idx] = cy - 30;
      }
   }

   public void onLiveHpSync(float hp) {
      float prev = this.playerHp;
      this.playerHp = hp;
      if (!this.rollingHealth || this.rollingPlayerHp < 0.0F) {
         this.rollingPlayerHp = hp;
      }

      if (prev > 0.0F && hp < prev - 0.1F && this.phase == BattleScreen.Phase.ENEMY_TURN) {
         float actualDmg = prev - hp;
         int myIdx = 0;

         for (int pi = 0; pi < this.participants.size(); pi++) {
            if (this.participants.get(pi).isMe()) {
               myIdx = pi;
               break;
            }
         }

         this.spawnPlayerDamageNumber(myIdx, Math.max(1.0F, actualDmg));
      }
   }

   public void onLiveTpSync(float tp) {
      this.onLiveTpSync(tp, this.playerTpMax);
   }

   public void onLiveTpSync(float tp, float tpMax) {
      this.playerTp = Math.max(0.0F, tp);
      this.playerTpMax = tpMax;
   }

   public void setRollingHealth(boolean rolling) {
      this.rollingHealth = rolling;
   }

   public void setEnemyStatusTurns(int freeze, int burn) {
      this.enemyFreezeTurns = freeze;
      this.enemyBurnTurns = burn;
   }

   public void setEnemyDownSturdy(int down, int sturdy) {
      this.enemyDownTurns = down;
      this.enemySturdy = sturdy;
   }

   public void setEntityPassive(String passive) {
      this.entityPassive = passive == null ? "" : passive;
   }

   public void setDarknessConstricting(boolean active) {
      this.darknessConstricting = active;
   }

   public void setBattleElement(Element element) {
      this.battleElement = element == null ? Element.NONE : element;
   }

   public void setLogHistory(String history) {
      if (history != null) {
         this.flushLog(history);
      }
   }

   public void setPlayerSoul(SoulType soul) {
      this.playerSoul = soul;
   }

   public void setPvpMode(SoulType opponentSoul) {
      this.pvpMode = true;
      this.pvpOpponentSoul = opponentSoul;
      BattleEntityRenderer.clearPvpFakePlayers();
   }

   public void receiveBattleChat(UUID senderUuid, String senderName, String msg) {
      int idx = -1;
      boolean isLocalPlayer = this.client != null && this.client.player != null && senderUuid.equals(this.client.player.getUuid());
      if (isLocalPlayer) {
         for (int i = 0; i < this.participants.size(); i++) {
            if (this.participants.get(i).isMe()) {
               idx = i;
               break;
            }
         }
      } else if (this.pvpMode) {
         for (int ix = 0; ix < this.participants.size(); ix++) {
            UUID pu = this.participants.get(ix).uuid();
            if (pu != null && pu.equals(senderUuid)) {
               idx = ix;
               break;
            }
         }

         if (idx == -1) {
            for (int ixx = 0; ixx < this.pvpOpponents.size(); ixx++) {
               if (senderUuid.equals(this.pvpOpponents.get(ixx).uuid())) {
                  idx = -2 - ixx;
                  break;
               }
            }

            if (idx == -1) {
               idx = -2;
            }
         }
      } else {
         for (int ixxx = 0; ixxx < this.participants.size(); ixxx++) {
            if (this.participants.get(ixxx).name().equals(senderName)) {
               idx = ixxx;
               break;
            }
         }
      }

      if (idx == -1) {
         idx = 0;
      }

      this.chatSenderIndex.put(senderUuid, idx);
      this.chatSenderName.put(idx, senderName);
      this.chatBubbles.computeIfAbsent(senderUuid, k -> new ArrayList<>()).add(new BattleScreen.ChatBubble(msg, this.globalTick));
   }

   public void setPvpShield(float shield, float shieldMax, boolean broken) {
      this.pvpOpponentShield = shield;
      this.pvpOpponentShieldMax = shieldMax;
      this.pvpOpponentShieldBroken = broken;
   }

   public void setMyPvpShield(float shield, float shieldMax, boolean broken) {
      this.myPvpShield = shield;
      this.myPvpShieldMax = shieldMax;
      this.myPvpShieldBroken = broken;
   }

   public void setSpareThreshold(float threshold) {
      if (this.pvpMode) {
         this.spareThreshold = 0.0F;
      } else {
         float prev = this.spareThreshold;
         this.spareThreshold = threshold;
         boolean sameEnemy = this.spareThresholdOwnerId == this.currentPrimaryEnemyId;
         this.spareThresholdOwnerId = this.currentPrimaryEnemyId;
         if (sameEnemy && threshold > prev + 0.001F) {
            int pct = Math.max(1, Math.round((threshold - prev) * 100.0F));
            int slot = 0;
            if (this.anchors3D()) {
               int resolved = this.rosterSlotForId(this.currentPrimaryEnemyId);
               if (resolved >= 0) {
                  slot = resolved;
               }
            }

            this.spawnMercyNumber(slot, pct);
            TurnBattleSounds.playClient(TurnBattleSounds.SND_MERCYADD, 1.0F, 1.0F);
         }
      }
   }

   public void setBossLives(int livesRemaining, int maxLives) {
      this.bossLivesRemaining = livesRemaining;
      this.bossMaxLives = maxLives;
   }

   public void setBattleStyle(BattleStyle style) {
      this.battleStyle = style != null ? style : BattleStyle.BULLET_HELL;
   }

   public void setDirectDamageWaiting(boolean waiting) {
      if (waiting) {
         if (this.phase == BattleScreen.Phase.PLAYER_TURN
            || this.phase == BattleScreen.Phase.SKILLS_MENU
            || this.phase == BattleScreen.Phase.GOODS_MENU
            || this.phase == BattleScreen.Phase.TARGET_SELECT_ENEMY
            || this.phase == BattleScreen.Phase.TARGET_SELECT_ALLY) {
            this.pendingAfterTransition = "wait";
            this.transitionTick = 0;
            this.setPhase(BattleScreen.Phase.TRANSITION);
         }
      }
   }

   public void onDirectEnemyTurn(
      float dmg, String actorName, String attackName, boolean isTarget, MobAttack.Visual visual, int colorOverride, List<UUID> targetUuids
   ) {
      int myIdx = 0;

      for (int i = 0; i < this.participants.size(); i++) {
         if (this.participants.get(i).isMe()) {
            myIdx = i;
            break;
         }
      }

      this.enemyPhaseSinceGuards = true;
      this.endQueuedRound();
      this.directHitDmg = dmg;
      this.directHitMyIdx = myIdx;
      this.directHitIsTarget = isTarget;
      SkillType sType = visual != null
         ? new MobAttack("", 0.0F, MobAttack.Targeting.SINGLE, MobAttack.Special.NONE, visual, 0).toSkillType()
         : SkillType.DAMAGE;
      int fallbackCx = this.width / 2;
      int barAreaBottom = 26 + 9 + 4;
      int textBoxTop = this.height - 72 - 75 - 10;
      int fallbackCy = (barAreaBottom + textBoxTop) / 2;
      this.otherDirectHitEffects.clear();
      this.directHitEffect = null;
      boolean mineSpawned = false;

      for (UUID t : targetUuids) {
         int idx = -1;

         for (int ix = 0; ix < this.participantUuids.size(); ix++) {
            UUID pu = this.participantUuids.get(ix);
            if (pu != null && pu.equals(t)) {
               idx = ix;
               break;
            }
         }

         int px = fallbackCx;
         int py = fallbackCy;
         if (idx >= 0 && idx < this.lastParticipantCX.length && this.lastParticipantCX[idx] > 0) {
            px = this.lastParticipantCX[idx];
            py = this.lastParticipantTopY[idx];
         }

         if (idx == myIdx && !mineSpawned) {
            this.directHitEffect = SkillEffect.forMobAttack(sType, px, py, colorOverride);
            mineSpawned = true;
         } else {
            this.otherDirectHitEffects.add(new BattleScreen.OtherDirectHit(SkillEffect.forMobAttack(sType, px, py, colorOverride), idx));
         }
      }

      if (!mineSpawned) {
         int pxx = fallbackCx;
         int pyx = fallbackCy;
         if (myIdx < this.lastParticipantCX.length && this.lastParticipantCX[myIdx] > 0) {
            pxx = this.lastParticipantCX[myIdx];
            pyx = this.lastParticipantTopY[myIdx];
         }

         this.directHitEffect = SkillEffect.forMobAttack(sType, pxx, pyx, colorOverride);
      }
   }

   public void updateParticipants(List<BattleScreen.ParticipantInfo> list) {
      this.participants.clear();
      this.participants.addAll(list);
      this.playerTpMax = 100.0F + (float)Math.max(0, list.size() - 1) * 75.0F;
      this.participantUuids.clear();
      MinecraftClient mc = MinecraftClient.getInstance();

      for (BattleScreen.ParticipantInfo p : list) {
         UUID uuid = p.uuid();
         if (uuid == null && p.isMe() && mc.player != null) {
            uuid = mc.player.getUuid();
         }

         this.participantUuids.add(uuid);
      }

      if (this.anchors3D()) {
         for (int i = 0; i < list.size() && i < this.lastParticipantCX.length; i++) {
            int[] pos = BattleEntityRenderer.participantPos(
               i, Math.max(1, list.size()), this.width > 0 ? this.width : 854, this.height > 0 ? this.height : 480, 82, true
            );
            this.lastParticipantCX[i] = Math.max(55, pos[0]);
            this.lastParticipantTopY[i] = pos[1] - 30;
         }
      }

      for (int i = 0; i < list.size(); i++) {
         float incoming = list.get(i).hp();
         Float lastTrue = this.lastParticipantTrueHp.get(i);
         if (lastTrue != null && !list.get(i).isMe() && incoming < lastTrue - 0.5F) {
            this.spawnPlayerDamageNumber(i, lastTrue - incoming);
         }

         this.lastParticipantTrueHp.put(i, incoming);
         this.rollingParticipantHp.putIfAbsent(i, incoming);
      }

      this.rollingParticipantHp.entrySet().removeIf(e -> e.getKey() >= list.size());
      this.lastParticipantTrueHp.entrySet().removeIf(e -> e.getKey() >= list.size());
   }

   public void onLevelUpdate(int newLevel, int exp, int needed, boolean leveledUp) {
      this.playerLevel = newLevel;
      this.playerExp = exp;
      this.expNeeded = needed;
      if (leveledUp) {
         this.pushMessage("Level Up! You are now Lvl. " + newLevel + "!");
      }
   }

   public void onBatonPassStart(String[] names, String[] uuids, int[] souls) {
      this.batonOptions.clear();

      for (int i = 0; i < names.length; i++) {
         this.batonOptions.add(new BattleScreen.BatonOption(names[i], uuids[i], souls[i]));
      }

      this.batonCursor = 0;
      this.setPhase(BattleScreen.Phase.BATON_PASS);
   }

   private void submitBatonPassChoice() {
      if (!this.batonOptions.isEmpty()) {
         BattleScreen.BatonOption chosen = this.batonOptions.get(this.batonCursor);
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeString(chosen.uuid());
         Net121.sendToServer(TurnBattleMod.BATON_PASS_CHOICE, buf);
         this.setPhase(BattleScreen.Phase.PLAYER_TURN);
      }
   }

   private void renderBatonPassPrompt(DrawContext ctx, int mx, int my) {
      int pw = 220;
      int cardH = 26;
      int pad = 8;
      int ph = pad + this.batonOptions.size() * (cardH + 4) + pad + 18;
      int px = this.width / 2 - pw / 2;
      int py = this.height / 2 - ph / 2;
      ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, -1);
      ctx.fill(px, py, px + pw, py + ph, -16777216);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Baton Pass \u2014 give a turn to:").formatted(Formatting.YELLOW), this.width / 2, py + 5, -8960
      );

      for (int i = 0; i < this.batonOptions.size(); i++) {
         BattleScreen.BatonOption opt = this.batonOptions.get(i);
         int cardY = py + pad + 14 + i * (cardH + 4);
         boolean sel = i == this.batonCursor;
         boolean hov = mx >= px + pad && mx < px + pw - pad && my >= cardY && my < cardY + cardH;
         ctx.fill(px + pad - 1, cardY - 1, px + pw - pad + 1, cardY + cardH + 1, -1);
         ctx.fill(px + pad, cardY, px + pw - pad, cardY + cardH, sel ? -14024646 : (hov ? -15073254 : -16121846));
         int soulColor = -7829368;
         if (opt.soulOrd() >= 0 && opt.soulOrd() < SoulType.values().length) {
            soulColor = SoulType.values()[opt.soulOrd()].getColor() | 0xFF000000;
         }

         ctx.fill(px + pad, cardY, px + pad + 3, cardY + cardH, soulColor);
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal(opt.name()).formatted(sel ? Formatting.WHITE : Formatting.GRAY),
            px + pad + 8,
            cardY + (cardH - 9) / 2,
            sel ? -1 : -5592406
         );
      }

      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("W/S to navigate  \u00b7  Space/Enter to confirm").formatted(Formatting.DARK_GRAY),
         this.width / 2,
         py + ph - 12,
         -11184794
      );
   }

   public void onAllOutStart(String[] names, int[] souls) {
      this.allOutParticipantNames = names;
      this.allOutParticipantSouls = souls;
      this.allOutVoteTally.clear();
      this.allOutMyVote = -1;
      this.allOutCursor = 0;
      this.setPhase(BattleScreen.Phase.ALL_OUT);
   }

   public void onAllOutUpdate(Map<String, Integer> votes) {
      this.allOutVoteTally = votes;
   }

   public void onAllOutExec(float totalDmg) {
      if (this.client != null && this.client.player != null) {
         this.client.player.playSound((SoundEvent)Registries.SOUND_EVENT.get(Identifier.of("turnbattle", "snd_rudebuster_swing")), 1.0F, 1.0F);
      }

      for (int i = 0; i < this.participants.size(); i++) {
         this.vesselAnimType[i] = 0;
         this.vesselAnimTick[i] = 0;
      }

      this.allOutAnimating = true;
      this.allOutAnimTick = 0;
      this.setPhase(BattleScreen.Phase.PLAYER_TURN);
      this.pushMessage(String.format("ALL-OUT ATTACK! %.0f total damage!", totalDmg));
   }

   private void renderAllOutPrompt(DrawContext ctx, int mx, int my) {
      int pw = 260;
      int ph = 130;
      int px = this.width / 2 - pw / 2;
      int py = this.height / 2 - ph / 2;
      ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, -1);
      ctx.fill(px, py, px + pw, py + ph, -16777216);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Use an ALL-OUT ATTACK?").formatted(Formatting.YELLOW), this.width / 2, py + 12, -8960);
      int btnW = 90;
      int btnH = 22;
      int attackX = this.width / 2 - btnW - 8;
      int relentX = this.width / 2 + 8;
      int btnY = py + 42;
      if (this.allOutCursor != 0 && (mx < attackX || mx >= attackX + btnW || my < btnY || my >= btnY + btnH)) {
         boolean var22 = false;
      } else {
         boolean var10000 = true;
      }

      if (this.allOutCursor != 1 && (mx < relentX || mx >= relentX + btnW || my < btnY || my >= btnY + btnH)) {
         boolean var24 = false;
      } else {
         boolean var23 = true;
      }

      ctx.fill(attackX - 1, btnY - 1, attackX + btnW + 1, btnY + btnH + 1, -1);
      ctx.fill(attackX, btnY, attackX + btnW, btnY + btnH, this.allOutCursor == 0 ? -14022086 : -16119262);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Attack!").formatted(Formatting.WHITE), attackX + btnW / 2, btnY + 7, this.allOutCursor == 0 ? -30465 : -5592406
      );
      ctx.fill(relentX - 1, btnY - 1, relentX + btnW + 1, btnY + btnH + 1, -1);
      ctx.fill(relentX, btnY, relentX + btnW, btnY + btnH, this.allOutCursor == 1 ? -14022086 : -16119262);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Relent.").formatted(Formatting.WHITE), relentX + btnW / 2, btnY + 7, this.allOutCursor == 1 ? -30465 : -5592406
      );
      int starY = btnY + btnH + 4;

      for (int i = 0; i < this.allOutParticipantNames.length; i++) {
         String name = this.allOutParticipantNames[i];
         Integer vote = this.allOutVoteTally.get(name);
         if (vote != null) {
            int soulColor = -1;
            if (i < this.allOutParticipantSouls.length) {
               int ord = this.allOutParticipantSouls[i];
               if (ord >= 0 && ord < SoulType.values().length) {
                  soulColor = SoulType.values()[ord].getColor() | 0xFF000000;
               }
            }

            int starX = vote == 0 ? attackX + btnW / 2 : relentX + btnW / 2;
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2605"), starX + i * 8, starY, soulColor);
         }
      }

      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("A/D or click to choose, confirm with W or Enter").formatted(Formatting.DARK_GRAY),
         this.width / 2,
         py + ph - 14,
         -11184794
      );
   }

   private void submitAllOutVote(int vote) {
      if (this.allOutMyVote == -1) {
         this.allOutMyVote = vote;
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(vote);
         Net121.sendToServer(TurnBattleMod.ALL_OUT_VOTE, buf);
      }
   }

   public void onBattleEnd(String outcome, int expGained) {
      this.onBattleEnd(outcome, expGained, 0);
   }

   public void onBattleEnd(String outcome, int expGained, int darkDollars) {
      String dd = darkDollars > 0 ? " and " + darkDollars + " D$" : "";

      String endMsg = switch (outcome) {
         case "win" -> darkDollars > 0 ? "You won! Got " + expGained + " EXP" + dd + "." : "You won! Gained " + expGained + " EXP.";
         case "mercy" -> darkDollars > 0 ? "You showed MERCY! Got " + expGained + " EXP" + dd + "." : "You showed MERCY! Gained " + expGained + " EXP.";
         case "lose" -> "You were defeated...";
         case "run" -> "Got away safely!";
         default -> "Battle ended.";
      };
      this.pushMessage(endMsg);
      if (this.queueMode) {
         this.endQueuedRound();
         this.drText = endMsg;
         this.drIdx = 0;
         this.drMode = 0;
         this.drAcked = false;
      }

      this.showMainButtons(false);
      this.phase = BattleScreen.Phase.RESULT;
      TurnBattleSounds.stopBattleMusic(this.battleMusicInstance);
      this.battleMusicInstance = null;
      this.resultCountdown = !"win".equals(outcome) && !"mercy".equals(outcome) ? 20 : 40;
   }

   private void pushMessage(String msg) {
      if (!msg.isEmpty()) {
         this.logQueue.add(msg);
      }
   }

   static float clamp(float v, float lo, float hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   private void activateCard(int idx) {
      if (this.cardActive[idx]) {
         switch (idx) {
            case 0:
               this.onBashOrSkillAction(0, 0);
               break;
            case 1:
               this.openSkillsMenu();
               break;
            case 2:
               this.openGoodsMenu();
               break;
            case 3:
               this.sendAction(3, 0);
               break;
            case 4:
               this.sendAction(4, 0);
               break;
            case 5:
               this.sendAction(5, 0);
         }
      }
   }

   private void activateOffhandGimmick() {
      switch (this.offhandGimmick) {
         case SHIELD:
            if (!this.pvpMode && !this.shieldRingUsed) {
               this.shieldRingUsed = true;
               float cx = this.soulX + 4.5F;
               float cy = this.soulY + 4.5F;
               this.shieldRings.add(new float[]{cx, cy, 0.0F, 1.0F});
            }
            break;
         case FEATHER:
            if (this.featherDashIframes == 0 && this.featherDashCooldown == 0) {
               this.featherDashCooldown = 30;
               this.featherDashIframes = 8;
               this.iframeTicks = Math.max(this.iframeTicks, 8);
               float dx = 0.0F;
               float dy = 0.0F;
               if (this.keyW) {
                  dy--;
               }

               if (this.keyS) {
                  dy++;
               }

               if (this.keyA) {
                  dx--;
               }

               if (this.keyD) {
                  dx++;
               }

               if (dx == 0.0F && dy == 0.0F) {
                  dx = 1.0F;
               }

               float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
               this.featherDashVX = dx / len * 22.0F;
               this.featherDashVY = dy / len * 22.0F;
            }
      }
   }

   private void tickVesselAnims() {
      boolean isEnemyActive = this.phase == BattleScreen.Phase.ENEMY_TURN;
      boolean isReturning = this.phase == BattleScreen.Phase.ENEMY_TURN_END;
      if (isEnemyActive) {
         this.vesselAlpha = Math.max(0.0F, this.vesselAlpha - 42.0F);
      } else if (isReturning) {
         int textBoxW = this.width - 120;
         float returnProg = textBoxW > 160 ? Math.min(1.0F, (float)(this.currentBoxWidth - 160) / (float)(textBoxW - 160)) : 1.0F;
         this.vesselAlpha = returnProg * 255.0F;
      } else {
         this.vesselAlpha = 255.0F;
      }

      int count = Math.max(1, this.participants.isEmpty() ? 1 : this.participants.size());

      for (int i = 0; i < count && i < this.vesselAnimType.length; i++) {
         if (this.vesselHurtTick[i] > 0) {
            this.vesselHurtTick[i]--;
         }

         int type = this.vesselAnimType[i];
         if (type >= 0 && type < VESSEL_FRAME_COUNT.length) {
            int frames = VESSEL_FRAME_COUNT[type];
            boolean loops = VESSEL_LOOPS[type];
            int totalTicks = frames * 2;
            if (loops) {
               this.vesselAnimTick[i] = (this.vesselAnimTick[i] + 1) % totalTicks;
            } else {
               boolean onLastFrame = this.vesselAnimTick[i] >= totalTicks - 1;
               if (!onLastFrame) {
                  this.vesselAnimTick[i]++;
               } else if (type != 2) {
                  if (this.vesselHoldTick[i] == 0) {
                     this.vesselHoldTick[i] = type == 5 ? 6 : 14;
                  } else {
                     this.vesselHoldTick[i]--;
                     if (this.vesselHoldTick[i] == 0) {
                        this.vesselAnimType[i] = 4;
                        this.vesselAnimTick[i] = 0;
                     }
                  }
               }
            }
         }
      }
   }

   public void onShieldParryResult(boolean success) {
      this.pvpProtectTriggered = success;
      if (success) {
         int idx = this.localParticipantIndex();
         if (idx >= 0 && idx < this.vesselAnimType.length) {
            this.vesselAnimType[idx] = 5;
            this.vesselAnimTick[idx] = 0;
            this.vesselHoldTick[idx] = 0;
         }
      }
   }

   UUID localActorCloneKey() {
      if (this.partyMemberLabel != null) {
         return BattleEntityRenderer.recruitKey(this.partyMemberLabel.replace("'s Turn!", "").trim());
      } else {
         return this.client != null && this.client.player != null ? this.client.player.getUuid() : null;
      }
   }

   UUID cloneKeyForActor(UUID actor) {
      if (actor == null) {
         return null;
      } else if (!BattleEntityRenderer.isRecruitUuid(actor)) {
         return actor;
      } else {
         for (int i = 0; i < this.participants.size() && i < this.participantUuids.size(); i++) {
            if (actor.equals(this.participantUuids.get(i))) {
               return BattleEntityRenderer.recruitKey(this.participants.get(i).name());
            }
         }

         int wantSlot = "00000000-0000-0000-0000-000000000002".equals(actor.toString()) ? 1 : 0;
         int seen = 0;

         for (int ix = 0; ix < this.participants.size(); ix++) {
            UUID u = ix < this.participantUuids.size() ? this.participantUuids.get(ix) : null;
            if (BattleEntityRenderer.isRecruitUuid(u)) {
               if (seen == wantSlot) {
                  return BattleEntityRenderer.recruitKey(this.participants.get(ix).name());
               }

               seen++;
            }
         }

         return null;
      }
   }

   int cloneColorForActor(UUID actor) {
      if (actor != null) {
         for (int i = 0; i < this.participants.size() && i < this.participantUuids.size(); i++) {
            if (actor.equals(this.participantUuids.get(i))) {
               SoulType s = this.participants.get(i).soul();
               if (s != null) {
                  return s.getColor() & 16777215;
               }
            }
         }
      }

      return 16777215;
   }

   private int localCloneSoulColor() {
      SoulType s = this.partyMemberLabel != null && this.recruitSkillSoul != null ? this.recruitSkillSoul : this.playerSoul;
      return s != null ? s.getColor() & 16777215 : 16777215;
   }

   private ItemStack localInventoryStack(int slot) {
      if (this.client != null && this.client.player != null) {
         PlayerInventory inv = this.client.player.getInventory();
         return slot >= 0 && slot < inv.size() ? inv.getStack(slot).copy() : ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private void tickCloneMenuPose() {
      UUID key = this.localActorCloneKey();
      CloneAnimator.Act pose = CloneAnimator.Act.NONE;
      ItemStack item = ItemStack.EMPTY;
      int color = this.localCloneSoulColor();
      boolean ownSlashPlaying = false;
      switch (this.phase) {
         case SKILLS_MENU:
            pose = CloneAnimator.Act.CHOOSE_SKILL;
            break;
         case GOODS_MENU:
            pose = CloneAnimator.Act.CHOOSE_ITEM;
            if (this.goodsNavIdx >= 0 && this.goodsNavIdx < this.foodItems.size()) {
               item = this.localInventoryStack(this.foodItems.get(this.goodsNavIdx).slot());
            }
            break;
         case ACT_MENU:
            pose = CloneAnimator.Act.CHOOSE_ACT;
         case ENEMY_TURN:
         case ENEMY_TURN_END:
         case TRANSITION:
         case RESULT:
         case BATTLE_INTRO:
         default:
            break;
         case SKILL_EFFECT:
            if (this.pendingAction == 0) {
               if (this.activeBashEffect != null) {
                  ownSlashPlaying = true;
               } else if (!this.qteActive || !this.qteDone) {
                  pose = CloneAnimator.Act.BASH_READY;
                  if (this.qteActive) {
                     color = this.qteSoulColorRgb;
                  }
               }
            }
            break;
         case TARGET_SELECT_ENEMY:
         case TARGET_SELECT_ALLY:
            if (this.pendingAction == 1) {
               pose = CloneAnimator.Act.CHOOSE_SKILL;
            } else if (this.pendingAction == 2) {
               pose = CloneAnimator.Act.CHOOSE_ITEM;
               item = this.localInventoryStack(this.pendingGoodsSlot);
            } else if (this.pendingAction == 0) {
               pose = CloneAnimator.Act.BASH_READY;
            } else if (this.pendingAction == 98) {
               pose = CloneAnimator.Act.CHOOSE_ACT;
            }
      }

      if (this.lastLocalMenuPoseKey != null && !this.lastLocalMenuPoseKey.equals(key) && !this.readyKeys.contains(this.lastLocalMenuPoseKey)) {
         CloneAnimator.releaseMenuPoses(this.lastLocalMenuPoseKey);
      }

      if (key != null) {
         if (this.lastLocalMenuPose != CloneAnimator.Act.NONE && this.lastLocalMenuPose != pose && !this.readyKeys.contains(key)) {
            CloneAnimator.release(key, this.lastLocalMenuPose);
         }

         if (pose != CloneAnimator.Act.NONE) {
            CloneAnimator.hold(key, pose, color, item);
         }
      }

      this.lastLocalMenuPose = pose;
      this.lastLocalMenuPoseKey = key;
      CloneAnimator.Act relay = ownSlashPlaying ? CloneAnimator.Act.BASH_READY : pose;

      int code = switch (relay) {
         case CHOOSE_SKILL -> 1;
         case CHOOSE_ITEM -> 2;
         case BASH_READY -> 3;
         case CHOOSE_ACT -> 4;
         default -> 0;
      };
      String recruitName = this.partyMemberLabel != null ? this.partyMemberLabel.replace("'s Turn!", "").trim() : "";
      if (code != this.lastSentClonePose || !recruitName.equals(this.lastSentPoseRecruit)) {
         if (this.otherHumansInBattle()) {
            if (!recruitName.equals(this.lastSentPoseRecruit) && this.lastSentClonePose != 0) {
               this.sendClonePose(0, this.lastSentPoseRecruit);
            }

            this.sendClonePose(code, recruitName);
         }

         this.lastSentClonePose = code;
         this.lastSentPoseRecruit = recruitName;
      }
   }

   private void sendClonePose(int code, String recruitName) {
      PacketByteBuf poseBuf = PacketByteBufs.create();
      poseBuf.writeByte(code);
      poseBuf.writeString(recruitName);
      Net121.sendToServer(TurnBattleMod.CLONE_POSE, poseBuf);
   }

   private boolean otherHumansInBattle() {
      if (this.pvpMode) {
         return true;
      } else {
         UUID me = this.client != null && this.client.player != null ? this.client.player.getUuid() : null;

         for (UUID u : this.participantUuids) {
            if (u != null && !BattleEntityRenderer.isRecruitUuid(u) && !u.equals(me)) {
               return true;
            }
         }

         return false;
      }
   }

   public void onRemoteClonePose(UUID actor, int code, String recruitName) {
      UUID key = recruitName != null && !recruitName.isEmpty() ? BattleEntityRenderer.recruitKey(recruitName) : this.cloneKeyForActor(actor);
      if (key != null) {
         int color = this.cloneColorForActor(actor);
         switch (code) {
            case 1:
               CloneAnimator.hold(key, CloneAnimator.Act.CHOOSE_SKILL, color, null, 120000L);
               break;
            case 2:
               CloneAnimator.hold(key, CloneAnimator.Act.CHOOSE_ITEM, color, ItemStack.EMPTY, 120000L);
               break;
            case 3:
               CloneAnimator.hold(key, CloneAnimator.Act.BASH_READY, color, null, 15000L);
               break;
            case 4:
               CloneAnimator.hold(key, CloneAnimator.Act.CHOOSE_ACT, 16753722, null, 120000L);
               break;
            default:
               if (!this.readyKeys.contains(key)) {
                  CloneAnimator.releaseMenuPoses(key);
               }
         }
      }
   }

   public void onCloneBashWindup(UUID actor, int soulColorRgb) {
      UUID key = this.cloneKeyForActor(actor);
      if (key != null) {
         CloneAnimator.hold(key, CloneAnimator.Act.BASH_READY, soulColorRgb, null, 15000L);
      }
   }

   public void onCloneRemoteSkill(UUID actor, int skillOrd) {
      UUID key = this.cloneKeyForActor(actor);
      if (key != null) {
         BattleSkill skill = decodeSkillParam(skillOrd);
         int color = skill == LightnerSkill.RED_BUSTER ? 16720418 : this.cloneColorForActor(actor);
         CloneAnimator.play(key, CloneAnimator.Act.SKILL, color, null, CastAnims.castFor(skill));
      }
   }

   public void onCloneRemoteAction(UUID actor, CloneAnimator.Act act, int color, ItemStack item) {
      UUID key = this.cloneKeyForActor(actor);
      if (key != null) {
         CloneAnimator.play(key, act, color != 0 ? color : this.cloneColorForActor(actor), item);
      }
   }

   public void onRemoteVesselAnim(int participantIndex, int animType, UUID actor, String itemId) {
      int idx = participantIndex;
      if (actor != null) {
         int found = -1;

         for (int i = 0; i < this.participantUuids.size(); i++) {
            if (actor.equals(this.participantUuids.get(i))) {
               found = i;
               break;
            }
         }

         if (found >= 0) {
            idx = found;
         } else if (this.pvpMode) {
            idx = -1;
         }
      }

      if (idx >= 0) {
         this.setVesselAnim(idx, animType);
      }

      if (actor != null) {
         ItemStack item = ItemStack.EMPTY;
         if (itemId != null && !itemId.isEmpty()) {
            Identifier id = Identifier.tryParse(itemId);
            if (id != null) {
               Item it = (Item)Registries.ITEM.get(id);
               if (it != Items.AIR) {
                  item = new ItemStack(it);
               }
            }
         }

         if (animType >= 8 && animType <= 11) {
            this.holdReadyPose(actor, animType, item);
         } else {
            UUID playedKey = this.cloneKeyForActor(actor);
            if (playedKey != null) {
               this.readyKeys.remove(playedKey);
            }

            switch (animType) {
               case 1:
                  this.onCloneRemoteAction(actor, CloneAnimator.Act.SKILL, 0, null);
                  break;
               case 2:
                  this.onCloneRemoteAction(actor, CloneAnimator.Act.ITEM, 7143290, item);
                  break;
               case 3:
                  this.onCloneRemoteAction(actor, CloneAnimator.Act.DEFEND, 9427199, null);
               case 4:
               case 6:
               default:
                  break;
               case 5:
                  this.onCloneRemoteAction(actor, CloneAnimator.Act.SPARE, 16770650, null);
                  break;
               case 7:
                  this.onCloneRemoteAction(actor, CloneAnimator.Act.ACT, 16753722, null);
            }
         }
      }
   }

   public void setVesselAnim(int participantIndex, int actionType) {
      if (participantIndex >= 0 && participantIndex < this.vesselAnimType.length) {
         int mappedType = switch (actionType) {
            case 0 -> 0;
            case 1, 2, 5, 7 -> 1;
            case 3 -> 2;
            default -> -1;
         };
         if (mappedType < 0 || this.vesselAnimType[participantIndex] != mappedType || this.vesselAnimTick[participantIndex] >= 4) {
            switch (actionType) {
               case 0:
                  this.vesselAnimType[participantIndex] = 0;
                  this.vesselAnimTick[participantIndex] = 0;
                  this.vesselHoldTick[participantIndex] = 0;
                  this.vesselBlocking[participantIndex] = false;
                  break;
               case 1:
               case 2:
               case 5:
               case 7:
                  this.vesselAnimType[participantIndex] = 1;
                  this.vesselAnimTick[participantIndex] = 0;
                  this.vesselHoldTick[participantIndex] = 0;
                  this.vesselBlocking[participantIndex] = false;
                  break;
               case 3:
                  this.vesselAnimType[participantIndex] = 2;
                  this.vesselAnimTick[participantIndex] = 0;
                  this.vesselHoldTick[participantIndex] = 0;
                  this.vesselBlocking[participantIndex] = true;
                  break;
               case 4:
                  this.vesselAnimType[participantIndex] = 4;
                  this.vesselAnimTick[participantIndex] = 0;
                  this.vesselBlocking[participantIndex] = false;
                  break;
               case 6:
                  if (!this.vesselBlocking[participantIndex]) {
                     this.vesselHurtTick[participantIndex] = 10;
                  }
            }
         }
      }
   }

   private void resetVesselForNewTurn(int participantIndex) {
      if (participantIndex >= 0 && participantIndex < this.vesselAnimType.length) {
         this.vesselAnimType[participantIndex] = 4;
         this.vesselAnimTick[participantIndex] = 0;
         this.vesselBlocking[participantIndex] = false;
         this.vesselHoldTick[participantIndex] = 0;
      }
   }

   private void renderVesselSprites(DrawContext ctx, int alpha) {
      if (alpha > 0) {
         ;
      }
   }

   private void drawOneVessel(DrawContext ctx, int alpha, int i, int colCenterX, int panelY, int count, int screenCx) {
      int DRAW_W = 42;
      int DRAW_H = 33;
      int NATIVE_W = 63;
      int NATIVE_H = 50;
      float tintR = 1.0F;
      float tintG = 1.0F;
      float tintB = 1.0F;
      if (i < this.participants.size() && this.participants.get(i).soul() != null) {
         int col = this.participants.get(i).soul().getColor();
         tintR = (float)(col >> 16 & 0xFF) / 255.0F;
         tintG = (float)(col >> 8 & 0xFF) / 255.0F;
         tintB = (float)(col & 0xFF) / 255.0F;
      }

      boolean showHurt = i < this.vesselHurtTick.length && this.vesselHurtTick[i] > 0;
      int animType = showHurt && i < this.vesselAnimType.length ? 3 : (i < this.vesselAnimType.length ? this.vesselAnimType[i] : 4);
      if (animType < 0 || animType >= VESSEL_FRAME_COUNT.length) {
         animType = 4;
      }

      int tick = !showHurt && i < this.vesselAnimTick.length ? this.vesselAnimTick[i] : 0;
      int frame = Math.min(tick / 2, VESSEL_FRAME_COUNT[animType] - 1) + 1;
      String animName = VESSEL_ANIM_NAME[animType];
      Identifier tex = Identifier.of("turnbattle", "textures/vessel/vessel" + animName + frame + ".png");
      int shakeX = 0;
      if (i < this.vesselHurtTick.length && this.vesselHurtTick[i] > 0) {
         float p2 = (float)this.vesselHurtTick[i] / 10.0F;
         shakeX = (int)(Math.sin((double)((float)this.vesselHurtTick[i] * 1.6F)) * 3.0 * (double)p2);
      }

      boolean flipX;
      if (count == 1) {
         flipX = false;
      } else if (count == 3 && i == 1) {
         flipX = false;
      } else {
         flipX = colCenterX > screenCx;
      }

      int drawX = colCenterX - 21 + shakeX;
      int drawY = panelY + 3 - 33;
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(tintR, tintG, tintB, (float)alpha / 255.0F);
      if (flipX) {
         ctx.drawTexture(tex, drawX, drawY, 42, 33, 63.0F, 0.0F, -63, 50, 63, 50);
      } else {
         ctx.drawTexture(tex, drawX, drawY, 42, 33, 0.0F, 0.0F, 63, 50, 63, 50);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   static {
      MOB_SPRITES.put("zombie", new Object[]{"textures/mob_portraits/zombie1.png", "textures/mob_portraits/zombie2.png", 278, 485, 277, 502});
      MOB_SPRITES.put("skeleton", new Object[]{"textures/mob_portraits/skeleton1.png", "textures/mob_portraits/skeleton2.png", 173, 484, 171, 501});
      MOB_SPRITES.put("spider", new Object[]{"textures/mob_portraits/spider1.png", "textures/mob_portraits/spider2.png", 622, 334, 566, 413});
      MOB_SPRITES.put("creeper", new Object[]{"textures/mob_portraits/creeper1.png", "textures/mob_portraits/creeper2.png", 223, 529, 233, 482});
      MOB_SPRITES.put("enderman", new Object[]{"textures/mob_portraits/enderman1.png", "textures/mob_portraits/enderman2.png", 181, 732, 398, 783});
      MOB_SPRITES.put("blaze", new Object[]{"textures/mob_portraits/blaze1.png", "textures/mob_portraits/blaze2.png", 302, 434, 302, 434});
      MOB_SPRITES.put("warden", new Object[]{"textures/mob_portraits/warden1.png", "textures/mob_portraits/warden2.png", 402, 577, 402, 577});
      MOB_SPRITES.put("wither", new Object[]{"textures/mob_portraits/wither1.png", "textures/mob_portraits/wither2.png", 520, 535, 520, 535});
      MOB_SPRITES.put(
         "wither_skeleton", new Object[]{"textures/mob_portraits/wither_skeleton1.png", "textures/mob_portraits/wither_skeleton2.png", 171, 485, 172, 500}
      );
      MOB_SPRITES.put("ender_dragon", new Object[]{"textures/mob_portraits/ender_dragon1.png", "textures/mob_portraits/ender_dragon2.png", 858, 509, 858, 509});
      MOB_SPRITES.put("story_antonio", new Object[]{"textures/mob_portraits/antonio1.png", "textures/mob_portraits/antonio2.png", 1, 1, 1, 1});
      MOB_SPRITES.put("story_kris", new Object[]{"textures/mob_portraits/antonio1.png", "textures/mob_portraits/antonio2.png", 1, 1, 1, 1});

      for (int i = 0; i < 100; i++) {
         BATTLE_BG_TEX[i] = Identifier.of("turnbattle", "textures/battle_bg/bbs_" + (i + 1) + ".png");
      }

      DAMAGE_FONT = Identifier.of("turnbattle", "damage_numbers");
      VESSEL_FRAME_COUNT = new int[]{6, 3, 4, 1, 6, 3};
      VESSEL_ANIM_NAME = new String[]{"bash", "act", "block", "hurt", "idle", "block"};
      VESSEL_LOOPS = new boolean[]{false, false, false, false, true, false};
      GRAZE_PULSE_DELAYS = new int[]{0, 2, 4};
      DR_BTN_TEX = new String[]{"bash", "skills", "goods", "mercy", "defend"};
      DR_BTN_TEX_SEL = new String[]{"bashsel", "skillsel", "goodssel", "mercysel", "defendsel"};
      CARD_LABELS = new String[]{"BASH", "SKILLS", "GOODS", "DEFEND", "RUN", "MERCY"};
      CARD_SUBTEXTS = new String[]{"Strike the enemy", "Use powerful skills", "Use an item", "Guard, +20 TP", "Flee the battle", "Spare the enemy"};
      debugFrameCount = 0;
      debugFpsWindowStart = 0L;
      debugLastFps = 0;
      debugTickCount = 0;
      debugTpsWindowStart = 0L;
      debugLastTps = 0;
      EB_CMD_ORDER = new int[]{0, 1, 2, 3, 4};
      EB_FINGER = new String[]{"..oooo.....", ".owwwwooooo", "owwwwwwwwwo", "owwwwwooooo", "owwwwwo....", ".owwwwo....", "..oooo....."};
      TP_BAR_BG = Identifier.of("turnbattle", "textures/gui/tp_bar.png");
      TP_BAR_PROG = Identifier.of("turnbattle", "textures/gui/tp_bar_prog.png");
      TP_BAR_WHITE = Identifier.of("turnbattle", "textures/gui/tp_bar_white.png");
      TP_BAR_BG_DARKNESS = Identifier.of("turnbattle", "textures/gui/tp_bar_darkness.png");
      TP_BAR_PROG_DARKNESS = Identifier.of("turnbattle", "textures/gui/tp_bar_prog_darkness.png");
      CARD_ACCENT = new int[]{-2276284, -12285697, -12272794, -12303139, -4487117, -8892};
      CARD_ICONS = new String[]{"\u2694", "\u2726", "\u2665", "\ud83d\udee1", "\u21a9", "\u273f"};
   }

   static class BashEffect {
      private int tick = 0;
      private int cx;
      private int cy;
      float scale = 1.0F;
      float volume = 1.0F;
      float rotDeg = 0.0F;
      boolean flip = false;
      private final int soulColorRgb;

      void setCenter(int x, int y) {
         this.cx = x;
         this.cy = y;
      }

      BashEffect(int cx, int cy, int soulColorRgb) {
         this.cx = cx;
         this.cy = cy;
         this.soulColorRgb = soulColorRgb;
      }

      void tick() {
         this.tick++;
         if (this.tick == 1 && this.volume > 0.01F) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.SLASH, 1.0F, this.volume));
         }
      }

      boolean isDone() {
         return this.tick >= 12;
      }

      void render(DrawContext ctx, float delta) {
         if (!this.isDone()) {
            boolean xf = this.scale != 1.0F || this.rotDeg != 0.0F || this.flip;
            if (xf) {
               ctx.getMatrices().push();
               ctx.getMatrices().translate((float)this.cx, (float)this.cy, 0.0F);
               if (this.rotDeg != 0.0F) {
                  ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotDeg));
               }

               ctx.getMatrices().scale(this.flip ? -this.scale : this.scale, this.scale, 1.0F);
               ctx.getMatrices().translate((float)(-this.cx), (float)(-this.cy), 0.0F);
            }

            if (this.flip) {
               RenderSystem.disableCull();
            }

            try {
               this.renderAt(ctx, delta);
            } finally {
               if (this.flip) {
                  RenderSystem.enableCull();
               }

               if (xf) {
                  ctx.getMatrices().pop();
               }
            }
         }
      }

      private void renderAt(DrawContext ctx, float delta) {
         int frame = Math.min(this.tick / 2, 5) + 1;
         Identifier tex = Identifier.of("turnbattle", "textures/effects/slash" + frame + ".png");
         int x = this.cx - 10;
         int y = this.cy - 42;
         float progress = ((float)this.tick + delta) / 12.0F;
         int alpha = progress > 0.75F ? (int)((1.0F - (progress - 0.75F) / 0.25F) * 255.0F) : 255;
         float r = (float)(this.soulColorRgb >> 16 & 0xFF) / 255.0F;
         float g = (float)(this.soulColorRgb >> 8 & 0xFF) / 255.0F;
         float b = (float)(this.soulColorRgb & 0xFF) / 255.0F;
         if (this.soulColorRgb == 0) {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
         }

         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(r, g, b, (float)alpha / 255.0F);
         ctx.drawTexture(tex, x, y, 0.0F, 0.0F, 20, 85, 20, 85);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static record BatonOption(String name, String uuid, int soulOrd) {
   }

   private static record ChatBubble(String text, int born) {
   }

   private static record DamageNumber(float amount, float startX, float startY, int born, int rgb, String suffix, String prefix, float ax, float ay) {
      DamageNumber(float amount, float startX, float startY, int born, int rgb, String suffix, String prefix) {
         this(amount, startX, startY, born, rgb, suffix, prefix, Float.NaN, Float.NaN);
      }

      DamageNumber(float amount, float startX, float startY, int born, int rgb, String suffix) {
         this(amount, startX, startY, born, rgb, suffix, "");
      }
   }

   public static record EnemyInfo(
      int networkId,
      String name,
      String typeKey,
      float hp,
      float maxHp,
      int freezeTurns,
      int burnTurns,
      int sleepTurns,
      int confusedTurns,
      int downTurns,
      int sturdyTurns,
      float spareThreshold
   ) {
      public EnemyInfo(int networkId, String name, String typeKey, float hp, float maxHp) {
         this(networkId, name, typeKey, hp, maxHp, 0, 0, 0, 0, 0, 0, 0.25F);
      }

      public EnemyInfo(int networkId, String name, String typeKey, float hp, float maxHp, int freezeTurns, int burnTurns, int sleepTurns, int confusedTurns) {
         this(networkId, name, typeKey, hp, maxHp, freezeTurns, burnTurns, sleepTurns, confusedTurns, 0, 0, 0.25F);
      }

      public EnemyInfo(
         int networkId,
         String name,
         String typeKey,
         float hp,
         float maxHp,
         int freezeTurns,
         int burnTurns,
         int sleepTurns,
         int confusedTurns,
         int downTurns,
         int sturdyTurns
      ) {
         this(networkId, name, typeKey, hp, maxHp, freezeTurns, burnTurns, sleepTurns, confusedTurns, downTurns, sturdyTurns, 0.25F);
      }
   }

   private static record FoodEntry(int slot, String name, int hp, String itemId, int count) {
   }

   static enum GimmickType {
      NONE,
      ICE_PLATFORMER,
      NATURE_SHIELD,
      DARK_LINES,
      LIGHT_SHOOTER;
   }

   static class KrisSlash {
      int type;
      float pos;
      int tick = 0;
      static final int WARN_END = 15;
      static final int GROW_END = 18;
      static final int SHRINK_END = 21;
      boolean active = true;

      KrisSlash(int type, float pos) {
         this.type = type;
         this.pos = pos;
      }
   }

   private static final class MbBar {
      UUID actor;
      UUID owner;
      int color;
      int delay;
      int pIdx;
      String name;
      boolean mine;
      boolean crit;
      boolean done;
      boolean timedOut;
      float resultPos = -1.0F;
      boolean resultCrit;
      int doneTick;
   }

   private static record OhEnemy(int slot, int networkId, String name, float hp, float maxHp, float spare, int legacyIdx) {
   }

   public static record OpponentInfo(String name, UUID uuid, float hp, float maxHp, boolean downed) {
   }

   private static record OtherDirectHit(SkillEffect effect, int participantIdx) {
   }

   public static record ParticipantInfo(
      String name,
      float hp,
      float maxHp,
      float tp,
      boolean isCurrentTurn,
      boolean isMe,
      SoulType soul,
      int shieldTurns,
      int attackBoostTurns,
      int vesselChoice,
      UUID uuid,
      boolean isFrozen,
      boolean isBurned,
      boolean isAsleep,
      boolean isConfused
   ) {
      public ParticipantInfo(
         String name, float hp, float maxHp, float tp, boolean isCurrentTurn, boolean isMe, SoulType soul, int shieldTurns, int attackBoostTurns
      ) {
         this(name, hp, maxHp, tp, isCurrentTurn, isMe, soul, shieldTurns, attackBoostTurns, 1, null, false, false, false, false);
      }

      public ParticipantInfo(
         String name,
         float hp,
         float maxHp,
         float tp,
         boolean isCurrentTurn,
         boolean isMe,
         SoulType soul,
         int shieldTurns,
         int attackBoostTurns,
         int vesselChoice
      ) {
         this(name, hp, maxHp, tp, isCurrentTurn, isMe, soul, shieldTurns, attackBoostTurns, vesselChoice, null, false, false, false, false);
      }
   }

   static final class PeerSoulSample {
      float prevX;
      float prevY;
      float curX;
      float curY;
      long receivedAtNanos;
   }

   static enum Phase {
      PLAYER_TURN,
      SKILLS_MENU,
      GOODS_MENU,
      ACT_MENU,
      ENEMY_TURN,
      ENEMY_TURN_END,
      TRANSITION,
      RESULT,
      SKILL_EFFECT,
      BATTLE_INTRO,
      TARGET_SELECT_ENEMY,
      TARGET_SELECT_ALLY,
      ALL_OUT,
      BATON_PASS;
   }

   private static record SparePopup(String text, int born) {
   }

   private static enum TargetMode {
      NONE,
      ENEMY,
      ALLY;
   }
}
