package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class PartyScreen extends Screen {
   private static final Identifier TEX_OFFENSE_ICON = gui("spr_offenseicon.png");
   private static final Identifier TEX_DEFENSE_ICON = gui("spr_defenseicon.png");
   private static final Identifier TEX_MAGIC_ICON = gui("spr_magicicon.png");
   private static final Identifier TEX_VITALITY_ICON = gui("spr_vitalityicon.png");
   private static final Identifier TEX_IQ_ICON = gui("spr_iqicon.png");
   private static final Identifier TEX_LUCK_ICON = gui("spr_luckicon.png");
   private static final Identifier TEX_STAT_SOUL = gui("spr_statsoul.png");
   private static final Identifier TEX_WEAPON_ICON = gui("spr_weaponicon.png");
   private static final Identifier TEX_ACCESSORY_ICON = gui("spr_accessoryicon.png");
   private static final Identifier TEX_ARMOR_ICON_1 = gui("spr_armoricon1.png");
   private static final Identifier TEX_ARMOR_ICON_2 = gui("spr_armoricon2.png");
   private static final Identifier TEX_EQUIP_SLOT = gui("spr_equipslot.png");
   private static final Identifier TEX_STATS_TEXT = gui("spr_statstext.png");
   private static final Identifier TEX_STATS_TEXT_HL = gui("spr_statstext_highlight.png");
   private static final Identifier TEX_SKILL_TEXT = gui("spr_skilltext.png");
   private static final Identifier TEX_SKILL_TEXT_HL = gui("spr_skilltext_highlight.png");
   private static final Identifier TEX_EQUIP_TEXT = gui("spr_equiptext.png");
   private static final Identifier TEX_EQUIP_TEXT_HL = gui("spr_equiptexthighlight.png");
   private static final Identifier TEX_INFO_TEXT = gui("spr_infotext.png");
   private static final Identifier TEX_TP_TEXT = gui("spr_tptext.png");
   private static final Identifier TEX_UP_HL = gui("spr_uparrow_highlight.png");
   private static final Identifier TEX_UP_NOHL = gui("spr_uparrow_nohighlight.png");
   private static final Identifier TEX_DOWN_HL = gui("spr_downarrow_highlight.png");
   private static final Identifier TEX_DOWN_NOHL = gui("spr_downarrow_nohighlight.png");
   private int playerLevel = 1;
   private float playerHp = 20.0F;
   private float playerMaxHp = 20.0F;
   private int playerClassOrd = 0;
   private int partyMemberOrd = 0;
   private float partyHp = 0.0F;
   private float partyMaxHp = 0.0F;
   private boolean dataLoaded = false;
   private CharStats playerStats = new CharStats();
   private CharStats[] allPartyStats = new CharStats[PartyMember.values().length];
   private int[] allPartyLevels = new int[PartyMember.values().length];
   private int activeTab = 0;
   private int statsSubTab = 0;
   private int statTarget = 0;
   private int hoveredMember = -1;
   private SoulType currentSoul = null;
   private int currentVessel = 1;
   private boolean freeRollUsed = false;
   private ItemStack equippedWeapon = ItemStack.EMPTY;
   private ItemStack partyEquippedWeapon = ItemStack.EMPTY;
   private int[] invEquipSlots = new int[0];
   private ItemStack[] invEquipStacks = new ItemStack[0];
   private int equipScrollOffset = 0;
   private boolean equipDataLoaded = false;
   private int equipSubTab = 0;
   private final List<int[]> equipBtns = new ArrayList<>();
   private final List<int[]> rollBtns = new ArrayList<>();
   private ItemStack equippedAccessory = ItemStack.EMPTY;
   private int[] invAccSlots = new int[0];
   private ItemStack[] invAccStacks = new ItemStack[0];
   private int accScrollOffset = 0;
   private int accTargetSlot = -1;
   private int armTargetSlot = -1;
   private int accListH = 200;
   private boolean accDataLoaded = false;
   private final List<int[]> accBtns = new ArrayList<>();
   private ItemStack[] equippedArmor = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
   private int[] invArmorSlots = new int[0];
   private ItemStack[] invArmorStacks = new ItemStack[0];
   private int armorScrollOffset = 0;
   private int statSkillScrollIdx = 0;
   private String statInfoHoverText = null;
   private boolean armorDataLoaded = false;
   private final List<int[]> armorBtns = new ArrayList<>();
   private final List<int[]> armorRollBtns = new ArrayList<>();
   private final List<int[]> plusBtns = new ArrayList<>();
   private int[] resetBtnRegion = null;
   private int[] switchBtnRegions = null;
   private static final int BASE_SCALE = 2;
   private static final int SPRITE_W = 284;
   private static final int SPRITE_H = 129;
   private static final int BASE_PANEL_W = 173;
   private static final int BASE_PANEL_H = 103;
   private static final int BASE_CONTENT_X_OFF = 5;
   private static final int BASE_CONTENT_Y_OFF = 20;
   private static final int BASE_TAB_AREA_X_OFF = 188;
   private static final int BASE_TAB_AREA_Y_OFF = 14;
   private static final int BASE_TAB_AREA_W = 91;
   private static final int BASE_TAB_AREA_H = 78;
   private static final int BASE_INFO_AREA_X_OFF = 188;
   private static final int BASE_INFO_AREA_Y_OFF = 102;
   private static final int BASE_INFO_AREA_W = 91;
   private static final int BASE_INFO_AREA_H = 21;
   private static final int BASE_CLOSE_X_OFF = 169;
   private static final int BASE_CLOSE_Y_OFF = 3;
   private static final int BASE_CLOSE_W = 11;
   private static final int BASE_CLOSE_H = 10;
   private static final int BASE_TITLE_X_OFF = 6;
   private static final int BASE_TITLE_Y_OFF = 4;
   private static final int BASE_TITLE_W = 160;
   private static final int BASE_TITLE_H = 8;
   private int SCALE;
   private int PANEL_W;
   private int PANEL_H;
   private int CONTENT_X_OFF;
   private int CONTENT_Y_OFF;
   private int TAB_AREA_X_OFF;
   private int TAB_AREA_Y_OFF;
   private int TAB_AREA_W;
   private int TAB_AREA_H;
   private int INFO_AREA_X_OFF;
   private int INFO_AREA_Y_OFF;
   private int INFO_AREA_W;
   private int INFO_AREA_H;
   private int CLOSE_X_OFF;
   private int CLOSE_Y_OFF;
   private int CLOSE_W;
   private int CLOSE_H;
   private int TITLE_X_OFF;
   private int TITLE_Y_OFF;
   private int TITLE_W;
   private int TITLE_H;
   private int TAB_H = 0;
   private boolean toChild = false;
   private float lastRenderScale = 1.0F;
   private float menuCx = Float.NaN;
   private float menuCy = Float.NaN;
   private boolean menuHidden = false;
   private long shownAtNanos = -1L;
   private final Map<Integer, int[]> statMemberTabRegions = new HashMap<>();
   private final Map<Integer, int[]> statMemberFaceRegions = new HashMap<>();
   private List<String> statMemberLabelsCache = new ArrayList<>();
   private int[] statSkillUpRegion = null;
   private int[] statSkillDownRegion = null;
   private int partySubTab = 0;
   private int partyInviteScroll = 0;
   private static final int EQ_CARD_H = 48;
   private static final int EQ_CARD_PAD = 4;
   private static final int EQ_BTN_W = 46;
   private static final int EQ_BTN_H = 12;
   private static final int ACC_CARD_H = 56;
   private static final int ACC_BTN_W = 46;
   private static final int ACC_BTN_H = 12;
   private static final String[] VESSEL_NAMES = new String[]{"None", "Kris"};
   private static final String[] VESSEL_DESCS = new String[]{"No vessel. Your presence in battle will be invisible.", "The cage. A human wielding a blade."};
   private final List<int[]> vesselBtns = new ArrayList<>();
   private final List<int[]> skillMemberBtns = new ArrayList<>();
   private static final int CUSTOM_SKILL_BASE = 10000;
   private static final int CUSTOM_STAT_BASE = 20000;
   private int storyView = 0;
   private String storySelectedId = "";
   private boolean hoverQlAccept = false;
   private boolean hoverQlBack = false;
   private boolean hoverQlRetry = false;
   private int equipSelectedTarget = 0;
   private static final int ARM_CARD_H = 44;
   private static final int ARM_BTN_W = 52;
   private static final int ARM_BTN_H = 12;

   private static Identifier gui(String name) {
      return Identifier.of("turnbattle", "textures/gui/" + name);
   }

   private int computeScale() {
      return 2;
   }

   private float clampedRenderScale() {
      float totalW = 568.0F;
      float totalH = 258.0F;
      float maxW = (float)this.width * 0.95F;
      float maxH = (float)this.height * 0.95F;
      float scale = 1.0F;
      if (totalW > maxW) {
         scale = Math.min(scale, maxW / totalW);
      }

      if (totalH > maxH) {
         scale = Math.min(scale, maxH / totalH);
      }

      return scale;
   }

   private void updateScaledConstants() {
      this.SCALE = this.computeScale();
      this.PANEL_W = 173 * this.SCALE;
      this.PANEL_H = 103 * this.SCALE;
      this.CONTENT_X_OFF = 5 * this.SCALE;
      this.CONTENT_Y_OFF = 20 * this.SCALE;
      this.TAB_AREA_X_OFF = 188 * this.SCALE;
      this.TAB_AREA_Y_OFF = 14 * this.SCALE;
      this.TAB_AREA_W = 91 * this.SCALE;
      this.TAB_AREA_H = 78 * this.SCALE;
      this.INFO_AREA_X_OFF = 188 * this.SCALE;
      this.INFO_AREA_Y_OFF = 102 * this.SCALE;
      this.INFO_AREA_W = 91 * this.SCALE;
      this.INFO_AREA_H = 21 * this.SCALE;
      this.CLOSE_X_OFF = 169 * this.SCALE;
      this.CLOSE_Y_OFF = 3 * this.SCALE;
      this.CLOSE_W = 11 * this.SCALE;
      this.CLOSE_H = 10 * this.SCALE;
      this.TITLE_X_OFF = 6 * this.SCALE;
      this.TITLE_Y_OFF = 4 * this.SCALE;
      this.TITLE_W = 160 * this.SCALE;
      this.TITLE_H = 8 * this.SCALE;
   }

   public PartyScreen() {
      super(Text.literal("Party"));

      for (int i = 0; i < this.allPartyStats.length; i++) {
         this.allPartyStats[i] = new CharStats();
      }

      for (int i = 0; i < this.allPartyLevels.length; i++) {
         this.allPartyLevels[i] = 1;
      }

      this.currentVessel = TurnBattleClientMod.vesselChoice;
   }

   public PartyScreen(int tab) {
      this();
      this.activeTab = tab;
      if (tab == 5) {
         String active = TurnBattleClientMod.clientActiveQuestlineId;
         if (active != null && !active.isEmpty()) {
            this.storySelectedId = active;
            this.storyView = 1;
         } else {
            this.storyView = 0;
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   private static MutableText magicantTag() {
      return !TurnBattleClientMod.clientMagicant ? Text.literal("") : Text.literal(" (Magicant)");
   }

   private void openChild(Screen s) {
      if (this.client != null) {
         this.toChild = true;
         this.client.setScreen(s);
      }
   }

   public void removed() {
      super.removed();

      try {
         PlayerActions.endLocalMenu();
      } catch (Throwable var2) {
      }

      this.toChild = false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   protected void init() {
      Net121.sendToServer(TurnBattleMod.PARTY_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.EQUIP_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.ACCESSORY_INFO_REQUEST, PacketByteBufs.empty());
      Net121.sendToServer(TurnBattleMod.ARMOR_INFO_REQUEST, PacketByteBufs.empty());
   }

   public void onPartyInfo(
      int level, float hp, float maxHp, int classOrd, int memberOrd, float pHp, float pMaxHp, CharStats pStats, CharStats[] mStats, int[] mLevels
   ) {
      this.playerLevel = level;
      this.playerHp = hp;
      this.playerMaxHp = maxHp;
      this.playerClassOrd = classOrd;
      this.partyMemberOrd = memberOrd;
      this.partyHp = pHp;
      this.partyMaxHp = pMaxHp;
      this.playerStats = pStats;
      this.allPartyStats = mStats;
      this.allPartyLevels = mLevels;
      this.dataLoaded = true;
   }

   public void onEquipInfo(ItemStack equipped, ItemStack partyEquipped, int[] slots, ItemStack[] stacks) {
      this.equippedWeapon = equipped;
      this.partyEquippedWeapon = partyEquipped;
      this.invEquipSlots = slots;
      this.invEquipStacks = stacks;
      this.equipDataLoaded = true;
      this.equipScrollOffset = 0;
   }

   public void onAccessoryInfo(ItemStack equipped, int[] slots, ItemStack[] stacks) {
      this.equippedAccessory = equipped;
      this.invAccSlots = slots;
      this.invAccStacks = stacks;
      this.accDataLoaded = true;
      this.accScrollOffset = 0;
   }

   public void onArmorInfo(ItemStack[] equipped, int[] slots, ItemStack[] stacks) {
      this.equippedArmor = equipped;
      this.invArmorSlots = slots;
      this.invArmorStacks = stacks;
      this.armorDataLoaded = true;
      this.armorScrollOffset = 0;
   }

   public void onVesselInfo(int choice) {
      this.currentVessel = choice;
   }

   public void onSoulInfo(SoulType soul, boolean freeRollUsed) {
      this.currentSoul = soul;
      this.freeRollUsed = freeRollUsed;
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   private double toMenuX(double x) {
      return (x - (double)this.menuCx) / (double)this.lastRenderScale + (double)this.width / 2.0;
   }

   private double toMenuY(double y) {
      return (y - (double)this.menuCy) / (double)this.lastRenderScale + (double)this.height / 2.0;
   }

   private void scissor(DrawContext ctx, int x1, int y1, int x2, int y2) {
      Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
      Vector3f a = m.transformPosition(new Vector3f((float)x1, (float)y1, 0.0F));
      Vector3f b = m.transformPosition(new Vector3f((float)x2, (float)y2, 0.0F));
      ctx.enableScissor(
         (int)Math.floor((double)Math.min(a.x, b.x)),
         (int)Math.floor((double)Math.min(a.y, b.y)),
         (int)Math.ceil((double)Math.max(a.x, b.x)),
         (int)Math.ceil((double)Math.max(a.y, b.y))
      );
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      this.updateScaledConstants();
      float base = this.clampedRenderScale();
      float[] an = null;

      try {
         an = PlayerActions.menuAnchor(this.width, this.height, (float)(284 * this.SCALE), (float)(129 * this.SCALE), base);
      } catch (Throwable var37) {
      }

      this.menuCx = an != null ? an[0] : (float)this.width / 2.0F;
      this.menuCy = an != null ? an[1] : (float)this.height / 2.0F;
      this.lastRenderScale = an != null ? an[2] : base;
      float camW = an != null ? an[3] : 0.0F;
      float reveal = -1.0F;

      try {
         reveal = PlayerActions.menuReveal();
      } catch (Throwable var36) {
      }

      this.menuHidden = reveal >= 0.0F && reveal < 0.995F;
      if (this.menuHidden) {
         this.shownAtNanos = -1L;
      } else {
         if (this.shownAtNanos < 0L) {
            this.shownAtNanos = System.nanoTime();
         }

         if (reveal >= 0.0F) {
            float u = Math.min(1.0F, (float)(System.nanoTime() - this.shownAtNanos) / 1.8E8F);
            float c1 = 1.70158F;
            float c3 = c1 + 1.0F;
            float v = u - 1.0F;
            float pop = 1.0F + c3 * v * v * v + c1 * v * v;
            this.lastRenderScale *= 0.85F + 0.15F * pop;
         }

         int tintA = Math.round(170.0F * (1.0F - 0.85F * camW));
         if (tintA > 0) {
            ctx.fill(0, 0, this.width, this.height, tintA << 24 | 17);
         }

         ctx.getMatrices().push();
         ctx.getMatrices().translate(this.menuCx, this.menuCy, 0.0F);
         ctx.getMatrices().scale(this.lastRenderScale, this.lastRenderScale, 1.0F);
         ctx.getMatrices().translate((float)(-this.width) / 2.0F, (float)(-this.height) / 2.0F, 0.0F);
         mx = (int)Math.floor(this.toMenuX((double)mx));
         my = (int)Math.floor(this.toMenuY((double)my));
         this.plusBtns.clear();
         this.resetBtnRegion = null;
         int spriteX = (this.width - 284 * this.SCALE) / 2;
         int spriteY = (this.height - 129 * this.SCALE) / 2;
         int panelX = spriteX + this.CONTENT_X_OFF;
         int contentY = spriteY + this.CONTENT_Y_OFF;
         int tabAreaX = spriteX + this.TAB_AREA_X_OFF;
         int tabAreaY = spriteY + this.TAB_AREA_Y_OFF;
         int infoAreaX = spriteX + this.INFO_AREA_X_OFF;
         int infoAreaY = spriteY + this.INFO_AREA_Y_OFF;
         boolean showStory = !TurnBattleClientMod.isRogueliteWorld;
         boolean storyEnabled = TurnBattleClientMod.storyModeEnabled;
         String[] tabs = showStory
            ? new String[]{"Stats", TurnBattleClientMod.rogueliteInRun ? "Deck" : "Skill", "Party", "Equip", "Items", "Story"}
            : new String[]{"Stats", TurnBattleClientMod.rogueliteInRun ? "Deck" : "Skill", "Party", "Equip", "Items"};
         int closeX = spriteX + this.CLOSE_X_OFF;
         int closeY = spriteY + this.CLOSE_Y_OFF;
         boolean hoverClose = mx >= closeX && mx < closeX + this.CLOSE_W && my >= closeY && my < closeY + this.CLOSE_H;
         String suffix = this.activeTab == 0 ? "" : "_old";
         Identifier bgTex = hoverClose
            ? Identifier.of("turnbattle", "textures/menu/menuexitsel" + suffix + ".png")
            : Identifier.of("turnbattle", "textures/menu/menu" + suffix + ".png");
         ctx.drawTexture(bgTex, spriteX, spriteY, 284 * this.SCALE, 129 * this.SCALE, 0.0F, 0.0F, 284, 129, 284, 129);
         int titleX = spriteX + this.TITLE_X_OFF;
         int titleY = spriteY + this.TITLE_Y_OFF;
         String tabTitle = this.activeTab < tabs.length ? tabs[this.activeTab] : "";
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(tabTitle).formatted(Formatting.WHITE), titleX, titleY + (this.TITLE_H - 9) / 2, -1);
         if (TurnBattleClientMod.isRogueliteWorld) {
            int rbW = this.TAB_AREA_W - 8;
            int rbH = 14;
            int rbX = tabAreaX + 4;
            int rbY = tabAreaY + this.TAB_AREA_H + 6;
            boolean rbHov = mx >= rbX && mx <= rbX + rbW && my >= rbY && my <= rbY + rbH;
            ctx.fill(rbX - 1, rbY - 1, rbX + rbW + 1, rbY + rbH + 1, rbHov ? -3386112 : -5623040);
            ctx.fill(rbX, rbY, rbX + rbW, rbY + rbH, rbHov ? -299233280 : -300414976);
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("\u2694 ROGUELITE \u2694").formatted(Formatting.GOLD), rbX + rbW / 2, rbY + 3, rbHov ? -1 : -1136060
            );
         }

         int tabH = this.TAB_AREA_H / tabs.length;

         for (int i = 0; i < tabs.length; i++) {
            boolean active = i == this.activeTab;
            boolean hover = mx >= tabAreaX && mx < tabAreaX + this.TAB_AREA_W && my >= tabAreaY + i * tabH && my < tabAreaY + (i + 1) * tabH;
            boolean isStoryTab = showStory && i == tabs.length - 1 && !storyEnabled;
            int ty = tabAreaY + i * tabH;
            ctx.fill(tabAreaX, ty, tabAreaX + this.TAB_AREA_W, ty + tabH, isStoryTab ? 1426063360 : (active ? -1439480730 : (hover ? 1998730069 : 856756531)));
            if (active) {
               ctx.fill(tabAreaX, ty, tabAreaX + 3, ty + tabH, -5587969);
            }

            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal(tabs[i]).formatted(isStoryTab ? Formatting.DARK_GRAY : (active ? Formatting.WHITE : Formatting.GRAY)),
               tabAreaX + this.TAB_AREA_W / 2,
               ty + (tabH - 9) / 2,
               -1
            );
         }

         if (this.dataLoaded && this.activeTab != 0) {
            PlayerClass cls = this.playerClassOrd < PlayerClass.values().length ? PlayerClass.values()[this.playerClassOrd] : null;
            if (cls == null) {
               String var10000 = "?";
            }

            String vesselName = !TurnBattleClientMod.clientVesselName.isEmpty()
               ? TurnBattleClientMod.clientVesselName
               : (this.currentVessel >= 0 && this.currentVessel < VESSEL_NAMES.length ? VESSEL_NAMES[this.currentVessel] : "?");
            int iy = infoAreaY + 3;
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(vesselName + "  Lv." + this.playerLevel).formatted(Formatting.WHITE).append(magicantTag()),
               infoAreaX + 5,
               iy,
               -1
            );
            iy += 9 + 3;
            int barW = this.INFO_AREA_W - 10;
            float hpPct = this.playerMaxHp > 0.0F ? this.playerHp / this.playerMaxHp : 1.0F;
            ctx.fill(infoAreaX + 5, iy, infoAreaX + 5 + barW, iy + 5, -13421773);
            ctx.fill(infoAreaX + 5, iy, infoAreaX + 5 + (int)((float)barW * hpPct), iy + 5, hpPct > 0.5F ? -12268476 : (hpPct > 0.25F ? -3364352 : -3399134));
            iy += 8;
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(String.format("HP  %.0f / %.0f", this.playerHp, this.playerMaxHp)).formatted(Formatting.GRAY),
               infoAreaX + 5,
               iy,
               -3355444
            );
         }

         if (!this.dataLoaded) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("Loading...").formatted(Formatting.GRAY), panelX + this.PANEL_W / 2, contentY + this.PANEL_H / 2 - 5, -1
            );
            super.render(ctx, mx, my, delta);
            ctx.getMatrices().pop();
         } else {
            if (this.activeTab >= 2 && this.activeTab <= 4) {
               this.activeTab = 0;
            }

            if (this.activeTab == 0) {
               this.renderStatsTab(ctx, panelX, contentY, mx, my);
            } else if (this.activeTab == 1) {
               if (TurnBattleClientMod.rogueliteInRun) {
                  this.activeTab = 0;
                  if (this.client != null) {
                     this.client.setScreen(new RogueDeckScreen());
                  }
               } else {
                  this.renderSkillMemberList(ctx, panelX, contentY, mx, my);
               }
            } else if (this.activeTab == 2) {
               this.renderCombinedPartyTab(ctx, panelX, contentY, mx, my);
            } else if (this.activeTab == 3) {
               this.renderEquipTab(ctx, panelX, contentY, mx, my);
            } else if (this.activeTab == 4) {
               this.renderItemsTab(ctx, panelX, contentY, mx, my);
            } else if (this.activeTab == 5 && !TurnBattleClientMod.isRogueliteWorld) {
               this.renderStoryTab(ctx, panelX, contentY, mx, my);
            }

            super.render(ctx, mx, my, delta);
            ctx.getMatrices().pop();
         }
      }
   }

   private BattleSkill resolveSkillByName(String name) {
      BattleSkill s = SkillTreeDefs.psiByName(name);
      if (s != null) {
         return s;
      } else {
         s = SkillTreeDefs.lightnerByName(name);
         if (s != null) {
            return s;
         } else {
            HumanMageSkill var4 = SkillTreeDefs.humanMageByName(name);
            if (var4 != null) {
               return var4;
            } else {
               SoulMageSkill var5 = SkillTreeDefs.soulMageSkillByName(name);
               if (var5 != null) {
                  return var5;
               } else {
                  MonsterSkill var6 = SkillTreeDefs.monsterSkillByName(name);
                  return (BattleSkill)(var6 != null ? var6 : SkillTreeDefs.oriSkillByName(name));
               }
            }
         }
      }
   }

   private void renderStatsTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      this.renderStatsSubTab(ctx, panelX, contentY, mx, my);
   }

   private void renderStatsSubTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      this.statInfoHoverText = null;
      this.statMemberTabRegions.clear();
      this.statMemberFaceRegions.clear();
      List memberLabels = new ArrayList<>();
      List memberLevels = new ArrayList<>();
      memberLabels.add(!TurnBattleClientMod.clientVesselName.isEmpty() ? TurnBattleClientMod.clientVesselName : "YOU");
      memberLevels.add(this.playerLevel);

      for (Recruit r : Recruit.values()) {
         int ri = r.ordinal();
         if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
            memberLabels.add(r.displayName);
            memberLevels.add(TurnBattleClientMod.clientRecruitLevels[ri]);
         }
      }

      String[] cNames = TurnBattleClientMod.customRecruitNames;
      int[] cLevels = TurnBattleClientMod.customRecruitLevels;
      if (cNames != null) {
         for (int ci = 0; ci < cNames.length; ci++) {
            memberLabels.add(cNames[ci]);
            memberLevels.add(cLevels != null && ci < cLevels.length ? cLevels[ci] : 1);
         }
      }

      List memberTargets = new ArrayList<>();
      memberTargets.add(0);
      int seqIdx = 1;

      for (Recruit rx : Recruit.values()) {
         int ri = rx.ordinal();
         if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
            memberTargets.add(seqIdx++);
         }
      }

      if (cNames != null) {
         for (int ci = 0; ci < cNames.length; ci++) {
            memberTargets.add(20000 + ci);
         }
      }

      int total = memberLabels.size();
      SoulType[] memberSouls = new SoulType[total];

      for (int i = 0; i < total; i++) {
         int tgt = memberTargets.get(i);
         if (tgt == 0) {
            memberSouls[i] = TurnBattleClientMod.clientSoul;
         } else if (tgt >= 20000) {
            int ci = tgt - 20000;
            int[] souls = TurnBattleClientMod.customRecruitSouls;
            memberSouls[i] = souls != null && ci < souls.length && souls[ci] >= 0 && souls[ci] < SoulType.values().length ? SoulType.values()[souls[ci]] : null;
         } else {
            int recruitIdx = 0;
            int seen = 0;

            for (Recruit rxx : Recruit.values()) {
               int ri = rxx.ordinal();
               if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
                  if (++seen == tgt) {
                     recruitIdx = ri;
                     break;
                  }
               }
            }

            memberSouls[i] = Recruit.values()[recruitIdx].soulType;
         }
      }

      int tabH = 20;
      int tabY = contentY;
      int slotW = this.PANEL_W / Math.max(1, total);

      for (int ix = 0; ix < total; ix++) {
         int sx = panelX + ix * slotW;
         boolean isActive = memberTargets.get(ix) == this.statTarget;
         SoulType soul = memberSouls[ix];
         int soulColor = soul != null ? soul.getColor() : -7829368;
         int borderColor = isActive ? 0xFF000000 | soulColor : darkenColor(soulColor, 0.4F);
         boolean hover = mx >= sx && mx < sx + slotW && my >= tabY && my < tabY + tabH;
         ctx.fill(sx, tabY, sx + slotW, tabY + tabH, isActive ? -15723492 : (hover ? -15986922 : -16250354));
         ctx.fill(sx, tabY, sx + slotW, tabY + 2, borderColor);
         ctx.fill(sx, tabY + tabH - 2, sx + slotW, tabY + tabH, borderColor);
         ctx.fill(sx, tabY, sx + 2, tabY + tabH, borderColor);
         ctx.fill(sx + slotW - 2, tabY, sx + slotW, tabY + tabH, borderColor);
         Identifier faceTex = this.resolveTargetFaceTexture(ix == 0 ? -1 : 0, memberLabels.get(ix));
         int faceSz = 14;
         int faceX = sx + 5;
         int faceY = tabY + (tabH - faceSz) / 2;
         if (faceTex != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            ctx.drawTexture(faceTex, faceX, faceY, faceSz, faceSz, 8.0F, 8.0F, 8, 8, 64, 64);
            ctx.drawTexture(faceTex, faceX, faceY, faceSz, faceSz, 40.0F, 8.0F, 8, 8, 64, 64);
         }

         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal(memberLabels.get(ix)).formatted(isActive ? Formatting.AQUA : Formatting.GRAY),
            faceX + faceSz + 6,
            tabY + (tabH - 9) / 2,
            -1
         );
         boolean skinPickable = isActive && (ix == 0 || !memberLabels.get(ix).equals(Recruit.ORI.displayName));
         if (skinPickable) {
            boolean faceHover = mx >= faceX && mx < faceX + faceSz && my >= faceY && my < faceY + faceSz;
            if (faceHover) {
               this.statInfoHoverText = ix == 0
                  ? (
                     this.client != null && this.client.player != null && DarkWorldSkinsClient.has(this.client.player.getUuid())
                        ? "Change your Dark World skin (Right-click to remove it)"
                        : "Choose a skin to wear in the Dark World"
                  )
                  : "Change your recruit's skin";
            }

            this.statMemberFaceRegions.put(ix, new int[]{faceX, faceY, faceSz, faceSz});
         }

         int[] region = new int[]{sx, tabY, slotW, tabH};
         this.statMemberTabRegions.put(ix, region);
      }

      this.statMemberLabelsCache = memberLabels;
      int y = tabY + tabH + 1;
      boolean isPlayerTarget = this.statTarget == 0;
      CharStats stats;
      float maxHp;
      float curHp;
      SoulType selSoul;
      if (this.statTarget == 0) {
         stats = this.playerStats;
         maxHp = this.playerMaxHp;
         curHp = this.playerHp;
         selSoul = TurnBattleClientMod.clientSoul;
      } else if (this.statTarget >= 20000) {
         int ci = this.statTarget - 20000;
         CharStats[] cStats = TurnBattleClientMod.customRecruitStats;
         stats = cStats != null && ci < cStats.length && cStats[ci] != null ? cStats[ci] : new CharStats();
         float[] cMax = TurnBattleClientMod.customRecruitMaxHp;
         maxHp = cMax != null && ci < cMax.length && cMax[ci] > 0.0F ? cMax[ci] : 20.0F + stats.vitalityHp();
         float[] cHp = TurnBattleClientMod.customRecruitHp;
         curHp = cHp != null && ci < cHp.length ? cHp[ci] : maxHp;
         int[] souls = TurnBattleClientMod.customRecruitSouls;
         selSoul = souls != null && ci < souls.length && souls[ci] >= 0 && souls[ci] < SoulType.values().length ? SoulType.values()[souls[ci]] : null;
      } else {
         int recruitIdx = 0;
         int seen = 0;

         for (Recruit rxxx : Recruit.values()) {
            int ri = rxxx.ordinal();
            if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
               if (++seen == this.statTarget) {
                  recruitIdx = ri;
                  break;
               }
            }
         }

         stats = recruitIdx < TurnBattleClientMod.clientRecruitStats.length ? TurnBattleClientMod.clientRecruitStats[recruitIdx] : new CharStats();
         float[] rMax = TurnBattleClientMod.clientRecruitMaxHp;
         maxHp = rMax != null && recruitIdx < rMax.length && rMax[recruitIdx] > 0.0F ? rMax[recruitIdx] : 20.0F + stats.vitalityHp();
         float[] rHp = TurnBattleClientMod.clientRecruitHp;
         curHp = rHp != null && recruitIdx < rHp.length ? rHp[recruitIdx] : maxHp;
         selSoul = Recruit.values()[recruitIdx].soulType;
      }

      int unspent = stats.unspent;
      int soulColorFull = selSoul != null ? 0xFF000000 | selSoul.getColor() : -5592406;
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(
         (float)(soulColorFull >> 16 & 0xFF) / 255.0F, (float)(soulColorFull >> 8 & 0xFF) / 255.0F, (float)(soulColorFull & 0xFF) / 255.0F, 1.0F
      );
      ctx.drawTexture(TEX_STAT_SOUL, panelX + 8, y, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      String soulName = selSoul != null ? selSoul.getDisplayName() : "No Soul";
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(soulName).styled(st -> st.withColor(soulColorFull)), panelX + 26, y + (16 - 9) / 2, -1);
      boolean soulHover = mx >= panelX + 8 && mx < panelX + 220 && my >= y && my < y + 16;
      if (soulHover && selSoul != null) {
         this.statInfoHoverText = selSoul.getDescription();
      }

      if (unspent > 0) {
         int spX = panelX + 8 + (this.PANEL_W - 16 - 10) / 2 + 10;
         MutableText spText = Text.literal("SP").formatted(Formatting.BOLD).append(Text.literal(" " + unspent));
         ctx.drawTextWithShadow(this.textRenderer, spText, spX, y + (16 - 9) / 2, -8892);
      }

      y += 19;
      y += 4;
      int barX = panelX + 8;
      int origBarH = 8;
      int barH = Math.round((float)origBarH * 0.35F);
      int halfGap = 10;
      int halfW = (this.PANEL_W - 16 - halfGap) / 2;
      int barW = Math.round((float)halfW * 0.8F);
      int lvX = barX + halfW + halfGap;
      int textRowY = y - 9;
      float hpFrac = maxHp > 0.0F ? Math.max(0.0F, Math.min(1.0F, curHp / maxHp)) : 0.0F;
      int hpBarColor = selSoul != null ? 0xFF000000 | selSoul.getColor() : -13378083;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("HP").formatted(Formatting.BOLD), barX, textRowY, -1);
      String hpStr = (int)curHp + " / " + (int)maxHp;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(hpStr), barX + this.textRenderer.getWidth("HP") + 4, textRowY, -1);
      ctx.fill(barX, y, barX + barW, y + barH, -14540254);
      ctx.fill(barX, y, barX + (int)((float)barW * hpFrac), y + barH, hpBarColor);
      if (isPlayerTarget) {
         MutableText lvText = Text.literal("LV").formatted(Formatting.BOLD).append(Text.literal(" " + this.playerLevel));
         int expNeeded = Math.max(1, TurnBattleClientMod.clientExpNeeded);
         float xpFrac = Math.max(0.0F, Math.min(1.0F, (float)TurnBattleClientMod.clientExp / (float)expNeeded));
         String xpStr = TurnBattleClientMod.clientExp + "/" + expNeeded;
         lvText = lvText.copy().append(magicantTag());
         ctx.drawTextWithShadow(this.textRenderer, lvText, lvX, textRowY, -8892);
         boolean xpFits = this.textRenderer.getWidth(lvText) + 6 + this.textRenderer.getWidth(xpStr) <= barW;
         if (xpFits) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(xpStr), lvX + barW - this.textRenderer.getWidth(xpStr), textRowY, -1);
         } else {
            float xs = 0.7F;
            MatrixStack xm = ctx.getMatrices();
            xm.push();
            xm.translate((float)(lvX + barW) - (float)this.textRenderer.getWidth(xpStr) * xs, (float)textRowY + 9.0F * (1.0F - xs), 0.0F);
            xm.scale(xs, xs, 1.0F);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(xpStr), 0, 0, -1);
            xm.pop();
         }

         ctx.fill(lvX, y, lvX + barW, y + barH, -14540254);
         ctx.fill(lvX, y, lvX + (int)((float)barW * xpFrac), y + barH, -2241485);
      } else {
         int lvIdx = memberTargets.indexOf(this.statTarget);
         int lvl = lvIdx >= 0 ? memberLevels.get(lvIdx) : 1;
         MutableText lvText = Text.literal("LV").formatted(Formatting.BOLD).append(Text.literal(" " + lvl));
         ctx.drawTextWithShadow(this.textRenderer, lvText, lvX, textRowY, -8892);
      }

      y += barH + 5;
      ctx.fill(panelX + 8, y, panelX + this.PANEL_W - 8, y + 1, -13417387);
      y += 3;
      int colBottom = contentY + 82 * this.SCALE;
      int gapW = 10;
      int availW = this.PANEL_W - 2 * gapW;
      int statsColW = (int)((float)availW * 0.28F);
      int skillColW = (int)((float)availW * 0.44F);
      int equipColW = availW - statsColW - skillColW;
      int skillColX = panelX + statsColW + gapW;
      int equipColX = skillColX + skillColW + gapW;
      int tpSubW = 18;
      int tpNameGap = 14;
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int hdrH = 8;
      boolean statsHdrHover = mx >= panelX && mx < panelX + statsColW && my >= y && my < y + hdrH;
      boolean skillHdrHover = mx >= skillColX && mx < skillColX + skillColW && my >= y && my < y + hdrH;
      boolean equipHdrHover = mx >= equipColX && mx < equipColX + equipColW && my >= y && my < y + hdrH;
      int spriteH = 4;
      this.drawCenteredSprite(ctx, statsHdrHover ? TEX_STATS_TEXT_HL : TEX_STATS_TEXT, panelX, y, statsColW, spriteH, 30, 6);
      this.drawCenteredSprite(ctx, TEX_TP_TEXT, skillColX, y + 9, tpSubW, spriteH, 12, 6);
      this.drawCenteredSprite(
         ctx, skillHdrHover ? TEX_SKILL_TEXT_HL : TEX_SKILL_TEXT, skillColX + tpSubW + tpNameGap, y, skillColW - tpSubW - tpNameGap, spriteH, 34, 6
      );
      this.drawCenteredSprite(ctx, equipHdrHover ? TEX_EQUIP_TEXT_HL : TEX_EQUIP_TEXT, equipColX, y, equipColW, spriteH, 28, 6);
      int colY = y + hdrH + 3;
      int div1X = panelX + statsColW + gapW / 2;
      int div2X = skillColX + skillColW + gapW / 2;
      ctx.fill(div1X, y, div1X + 1, colBottom, -13417387);
      ctx.fill(div2X, y, div2X + 1, colBottom, -13417387);
      this.renderStatsColumn(ctx, panelX, colY, statsColW, colBottom, stats, unspent, mx, my);
      this.renderSkillsColumn(ctx, skillColX, colY, skillColW, colBottom, tpSubW, tpNameGap, mx, my);
      this.renderEquipColumn(ctx, equipColX, colY, equipColW, colBottom, mx, my);
      int infoY = colBottom + 4;
      this.drawCenteredSprite(ctx, TEX_INFO_TEXT, panelX, infoY, this.PANEL_W, 6, 22, 6);
      if (this.statInfoHoverText != null) {
         int wrapW = this.PANEL_W - 16;
         List infoLines = new ArrayList<>();
         StringBuilder cur = new StringBuilder();

         for (String w : this.statInfoHoverText.split(" ", -1)) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (this.textRenderer.getWidth(test) > wrapW && !cur.isEmpty()) {
               infoLines.add(cur.toString());
               cur = new StringBuilder(w);
            } else {
               cur = new StringBuilder(test);
            }
         }

         if (!cur.isEmpty()) {
            infoLines.add(cur.toString());
         }

         int lineH = 9 + 2;
         int textTop = infoY + 11;
         int availH = contentY + this.PANEL_H - textTop - 4;
         int maxLines = Math.max(1, availH / lineH);
         int shown = Math.min(infoLines.size(), maxLines);

         for (int ix = 0; ix < shown; ix++) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal(infoLines.get(ix)).formatted(Formatting.WHITE), panelX + this.PANEL_W / 2, textTop + ix * lineH, -2236963
            );
         }
      }
   }

   private void drawCenteredSprite(DrawContext ctx, Identifier tex, int x, int y, int w, int h, int nativeW, int nativeH) {
      int drawW = (int)((float)nativeW * ((float)h / (float)nativeH));
      int drawX = x + (w - drawW) / 2;
      ctx.drawTexture(tex, drawX, y, drawW, h, 0.0F, 0.0F, nativeW, nativeH, nativeW, nativeH);
   }

   private static int darkenColor(int argb, float factor) {
      int r = (int)((float)(argb >> 16 & 0xFF) * factor);
      int g = (int)((float)(argb >> 8 & 0xFF) * factor);
      int b = (int)((float)(argb & 0xFF) * factor);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private void renderStatsColumn(DrawContext ctx, int x, int y, int colW, int colBottom, CharStats stats, int unspent, int mx, int my) {
      int[] order = new int[]{0, 1, 2, 4, 3, 5};
      String[] names = new String[]{"Offense", "Defense", "Magic", "Vitality", "IQ", "Luck"};
      Identifier[] icons = new Identifier[]{TEX_OFFENSE_ICON, TEX_DEFENSE_ICON, TEX_MAGIC_ICON, TEX_VITALITY_ICON, TEX_IQ_ICON, TEX_LUCK_ICON};
      String[] descs = new String[]{
         "Bash DMG +18% per pt",
         "Cuts DMG taken by a share",
         "Skill effectiveness: +1.7% per pt",
         "+10 Max HP per pt",
         "Increases Graze & Defend TP per pt",
         "SMAAAASH!! crit + dodge chance"
      };
      int rowH = 13;

      for (int slot = 0; slot < order.length; slot++) {
         int statIdx = order[slot];
         int ry = y + slot * rowH;
         if (ry + rowH > colBottom) {
            break;
         }

         boolean maxed = stats.isMaxed(statIdx);
         boolean rowHover = mx >= x && mx < x + colW - 6 && my >= ry && my < ry + rowH - 2;
         if (rowHover) {
            ctx.fill(x, ry, x + colW - 6, ry + rowH - 2, 587202559);
            this.statInfoHoverText = descs[slot];
         }

         boolean upgradable = unspent > 0 && (statIdx != 5 || !maxed);
         int iconAreaX = x + 2;
         int iconAreaW = 14;
         if (upgradable) {
            boolean btnHover = mx >= iconAreaX && mx < iconAreaX + iconAreaW && my >= ry && my < ry + rowH - 2;
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("+").formatted(Formatting.GREEN),
               iconAreaX + iconAreaW / 2,
               ry + (rowH - 9) / 2,
               btnHover ? -7798904 : -12268476
            );
            this.plusBtns.add(new int[]{iconAreaX, ry, iconAreaW, rowH - 2, statIdx});
         } else {
            int[][] iconNativeStats = new int[][]{{9, 10}, {9, 10}, {9, 10}, {9, 10}, {9, 9}, {9, 10}};
            int nw = iconNativeStats[slot][0];
            int nh = iconNativeStats[slot][1];
            int maxIconH = 11;
            int drawW = (int)((float)nw * ((float)maxIconH / (float)nh));
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Identifier icon = icons[slot];
            ctx.drawTexture(icon, x + 2, ry + (rowH - maxIconH) / 2, drawW, maxIconH, 0.0F, 0.0F, nw, nh, nw, nh);
         }

         ctx.drawTextWithShadow(this.textRenderer, Text.literal(names[slot]).formatted(Formatting.WHITE), x + 16, ry + (rowH - 9) / 2, -1);
         int statVal = stats.valueFor(statIdx);
         String valStr = String.valueOf(statVal);
         int valX = x + colW - 8;
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal(valStr).formatted(maxed ? Formatting.GREEN : Formatting.YELLOW),
            valX - this.textRenderer.getWidth(valStr),
            ry + (rowH - 9) / 2,
            -8892
         );
      }

      int rstY = y + order.length * rowH + 4;
      if (rstY + 10 <= colBottom) {
         boolean rstHover = mx >= x && mx < x + colW - 6 && my >= rstY && my < rstY + 10;
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Reset (1 Netherite)").formatted(rstHover ? Formatting.GOLD : Formatting.DARK_GRAY),
            x,
            rstY,
            rstHover ? -8824 : -10066347
         );
         this.resetBtnRegion = new int[]{x, rstY, colW - 6, 10};
      }
   }

   private void renderSkillsColumn(DrawContext ctx, int x, int y, int colW, int colBottom, int tpSubW, int tpNameGap, int mx, int my) {
      Set unlocked;
      Set unequipped;
      boolean canFilterEquipped;
      if (this.statTarget == 0) {
         unlocked = TurnBattleClientMod.clientUnlockedSkills;
         unequipped = TurnBattleClientMod.clientUnequippedSkills;
         canFilterEquipped = true;
      } else if (this.statTarget >= 20000) {
         int ci = this.statTarget - 20000;
         Set[] cSkills = TurnBattleClientMod.customRecruitSkills;
         Set[] cUneq = TurnBattleClientMod.customRecruitUnequippedSkills;
         unlocked = cSkills != null && ci < cSkills.length && cSkills[ci] != null ? cSkills[ci] : Collections.emptySet();
         unequipped = cUneq != null && ci < cUneq.length && cUneq[ci] != null ? cUneq[ci] : Collections.emptySet();
         canFilterEquipped = true;
      } else {
         int recruitIdx = 0;
         int seen = 0;

         for (Recruit r : Recruit.values()) {
            int ri = r.ordinal();
            if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
               if (++seen == this.statTarget) {
                  recruitIdx = ri;
                  break;
               }
            }
         }

         Set[] rSkills = TurnBattleClientMod.clientRecruitSkills;
         unlocked = rSkills != null && recruitIdx < rSkills.length && rSkills[recruitIdx] != null ? rSkills[recruitIdx] : Collections.emptySet();
         unequipped = Collections.emptySet();
         canFilterEquipped = false;
      }

      List equippedNames = new ArrayList<>();

      for (String s : unlocked) {
         if (!SkillTreeDefs.isStatNode(s) && (!canFilterEquipped || !unequipped.contains(s))) {
            equippedNames.add(s);
         }
      }

      equippedNames.sort(String::compareTo);
      int rowH = 15;
      int visibleRows = Math.min(5, Math.max(1, (colBottom - y - 12) / rowH));
      int maxScroll = Math.max(0, equippedNames.size() - visibleRows);
      this.statSkillScrollIdx = Math.max(0, Math.min(this.statSkillScrollIdx, maxScroll));
      int namesX = x + tpSubW + tpNameGap;
      int namesW = colW - tpSubW - tpNameGap;
      boolean canScrollUp = this.statSkillScrollIdx > 0;
      ctx.drawTexture(canScrollUp ? TEX_UP_HL : TEX_UP_NOHL, namesX + namesW / 2 - 5, y, 10, 6, 0.0F, 0.0F, 10, 6, 10, 6);
      this.statSkillUpRegion = canScrollUp ? new int[]{namesX + namesW / 2 - 5, y, 10, 6} : null;
      int listY = y + 8;

      for (int row = 0; row < visibleRows; row++) {
         int idx = this.statSkillScrollIdx + row;
         if (idx >= equippedNames.size()) {
            break;
         }

         String skillName = equippedNames.get(idx);
         BattleSkill skill = this.resolveSkillByName(skillName);
         int ry = listY + row * rowH;
         boolean rowHover = mx >= x && mx < x + colW - 6 && my >= ry && my < ry + rowH - 1;
         if (rowHover) {
            ctx.fill(x, ry, x + colW - 6, ry + rowH - 1, 587202559);
            this.statInfoHoverText = skill != null ? skill.getLiveDescription() : skillName;
         }

         String label = skill != null ? skill.getDisplayName() : skillName;
         int tp = skill != null ? skill.getTpCost() : 0;
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(tp + "%").formatted(Formatting.GRAY), x, ry + (rowH - 9) / 2 - 1, -6706484);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(label).formatted(Formatting.WHITE), namesX, ry + (rowH - 9) / 2 - 1, -1);
      }

      if (equippedNames.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("No skills equipped").formatted(Formatting.DARK_GRAY), x, listY, -10066313);
      }

      boolean canScrollDown = this.statSkillScrollIdx < maxScroll;
      int downY = listY + visibleRows * rowH + 2;
      ctx.drawTexture(canScrollDown ? TEX_DOWN_HL : TEX_DOWN_NOHL, namesX + namesW / 2 - 5, downY, 10, 6, 0.0F, 0.0F, 10, 6, 10, 6);
      this.statSkillDownRegion = canScrollDown ? new int[]{namesX + namesW / 2 - 5, downY, 10, 6} : null;
   }

   private void renderEquipColumn(DrawContext ctx, int x, int y, int colW, int colBottom, int mx, int my) {
      ItemStack weapon;
      ItemStack accessory;
      ItemStack[] armor;
      if (this.statTarget == 0) {
         weapon = this.equippedWeapon;
         accessory = this.equippedAccessory;
         armor = this.equippedArmor;
      } else if (this.statTarget >= 20000) {
         int ci = this.statTarget - 20000;
         ItemStack[] cw = TurnBattleClientMod.customRecruitWeapons;
         ItemStack[] ca = TurnBattleClientMod.customRecruitAccessories;
         ItemStack[][] cm = TurnBattleClientMod.customRecruitArmor;
         weapon = cw != null && ci < cw.length ? cw[ci] : ItemStack.EMPTY;
         accessory = ca != null && ci < ca.length ? ca[ci] : ItemStack.EMPTY;
         armor = cm != null && ci < cm.length ? cm[ci] : new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      } else {
         int recruitIdx = 0;
         int seen = 0;

         for (Recruit r : Recruit.values()) {
            int ri = r.ordinal();
            if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
               if (++seen == this.statTarget) {
                  recruitIdx = ri;
                  break;
               }
            }
         }

         ItemStack[] rw = TurnBattleClientMod.clientRecruitWeapons;
         ItemStack[] ra = TurnBattleClientMod.clientRecruitAccessories;
         ItemStack[][] rm = TurnBattleClientMod.clientRecruitArmor;
         weapon = rw != null && recruitIdx < rw.length ? rw[recruitIdx] : ItemStack.EMPTY;
         accessory = ra != null && recruitIdx < ra.length ? ra[recruitIdx] : ItemStack.EMPTY;
         armor = rm != null && recruitIdx < rm.length ? rm[recruitIdx] : new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      }

      ItemStack[] slotItems = new ItemStack[]{weapon, accessory, armor[0], armor[1]};
      Identifier[] slotIcons = new Identifier[]{TEX_WEAPON_ICON, TEX_ACCESSORY_ICON, TEX_ARMOR_ICON_1, TEX_ARMOR_ICON_2};
      String[] slotLabels = new String[]{"Weapon", "Accessory", "Armor", "Armor"};
      int[][] iconNative = new int[][]{{13, 16}, {13, 15}, {13, 13}, {13, 13}};
      int slotSz = 22;
      int gap = 1;
      int typeIconW = 14;
      int groupW = typeIconW + 3 + slotSz;

      for (int i = 0; i < 4; i++) {
         int sy = y + i * (slotSz + gap);
         if (sy + slotSz > colBottom) {
            break;
         }

         int groupX = x + (colW - groupW) / 2;
         int sx = groupX + typeIconW + 3;
         boolean filled = slotItems[i] != null && !slotItems[i].isEmpty();
         boolean hover = mx >= groupX && mx < sx + slotSz && my >= sy && my < sy + slotSz;
         if (hover) {
            this.statInfoHoverText = filled ? slotItems[i].getName().getString() : "No " + slotLabels[i] + " equipped";
         }

         int nw = iconNative[i][0];
         int nh = iconNative[i][1];
         int maxIconH = 14;
         int drawW = (int)((float)nw * ((float)maxIconH / (float)nh));
         int typeIconX = groupX + (typeIconW - drawW) / 2;
         int typeIconY = sy + (slotSz - maxIconH) / 2;
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         ctx.drawTexture(slotIcons[i], typeIconX, typeIconY, drawW, maxIconH, 0.0F, 0.0F, nw, nh, nw, nh);
         ctx.drawTexture(TEX_EQUIP_SLOT, sx, sy, slotSz, slotSz, 0.0F, 0.0F, 24, 24, 24, 24);
         if (filled) {
            ctx.drawItem(slotItems[i], sx + (slotSz - 16) / 2, sy + (slotSz - 16) / 2);
         }
      }
   }

   private void renderSoulSubTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      int x = panelX + this.PANEL_W / 2;
      int y = contentY + 20;
      y = contentY + 4;
      if (this.currentSoul == null) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Your Soul").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}), x, y, -8892);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("Complete the intro sequence to reveal your SOUL.").formatted(Formatting.GRAY), x, y + 18, -7829368
         );
      } else {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(this.currentSoul.getDisplayName()).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}),
            x,
            y,
            this.currentSoul.getColor()
         );
         List lines = wrapText(this.currentSoul.getDescription(), 250);
         int descY = y + 15;

         for (String line : lines) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line).formatted(Formatting.GRAY), x, descY, -5592406);
            descY += 12;
         }

         if (TurnBattleClientMod.clientPlayerRace == CharacterRace.MONSTER) {
            List absorbed = TurnBattleClientMod.clientAbsorbedSouls;
            int absY = descY + 14;
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("Absorbed Souls: " + absorbed.size() + " / 7").formatted(Formatting.AQUA), x, absY, -12263937
            );
            absY += 11;
            if (!absorbed.isEmpty()) {
               ctx.drawCenteredTextWithShadow(
                  this.textRenderer,
                  Text.literal("Monster skills +" + Math.round(10.0F * (float)absorbed.size()) + "% power").formatted(Formatting.LIGHT_PURPLE),
                  x,
                  absY,
                  -39220
               );
               absY += 11;
            }

            absY += 3;
            int soulSz = 14;
            int soulGap = 4;
            int totalW = absorbed.size() * (soulSz + soulGap) - (absorbed.isEmpty() ? 0 : soulGap);
            int startX = x - totalW / 2;

            for (int i = 0; i < absorbed.size(); i++) {
               try {
                  SoulType st = SoulType.valueOf(absorbed.get(i));
                  Identifier tex = st.getTexture();
                  RenderSystem.enableBlend();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  ctx.drawTexture(tex, startX + i * (soulSz + soulGap), absY, 0.0F, 0.0F, soulSz, soulSz, soulSz, soulSz);
               } catch (Exception var19) {
               }
            }
         }
      }
   }

   private void renderCombinedPartyTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      String[] subs = new String[]{"Recruits", "V.Creator", "Players"};
      int[] subWs = new int[]{60, 68, 60};
      int subH = 14;
      int subGap = 4;
      int totalW = 0;

      for (int w : subWs) {
         totalW += w + subGap;
      }

      totalW -= subGap;
      int subStartX = panelX + (this.PANEL_W - totalW) / 2;
      int subY = contentY + 4;
      int sx = subStartX;

      for (int i = 0; i < subs.length; i++) {
         boolean active = this.partySubTab == i;
         boolean hov = mx >= sx && mx < sx + subWs[i] && my >= subY && my < subY + subH;
         int accentBg = i == 1 ? -16115158 : -15066624;
         int accentBord = i == 1 ? -12276993 : -8892;
         int accentHov = i == 1 ? -13417370 : -7829334;
         ctx.fill(sx - 1, subY - 1, sx + subWs[i] + 1, subY + subH + 1, active ? accentBord : (hov ? accentHov : -13421739));
         ctx.fill(sx, subY, sx + subWs[i], subY + subH, active ? accentBg : (hov ? -15790302 : -16250856));
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(subs[i]).formatted(active ? (i == 1 ? Formatting.AQUA : Formatting.YELLOW) : Formatting.WHITE),
            sx + subWs[i] / 2,
            subY + (subH - 9) / 2,
            -1
         );
         sx += subWs[i] + subGap;
      }

      int subContentY = subY + subH + 6;
      if (this.partySubTab == 0) {
         this.renderRecruitsTab(ctx, panelX, subContentY, mx, my);
      } else if (this.partySubTab == 1) {
         this.renderVesselCreatorTab(ctx, panelX, subContentY, mx, my);
      } else {
         this.renderPlayerPartyTab(ctx, panelX, subContentY, mx, my);
      }
   }

   public void onOnlinePlayers(List<String> names, List<UUID> uuids) {
   }

   private void renderPlayerPartyTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      int x = panelX + 8;
      int lineH = 9 + 4;
      boolean pub = TurnBattleClientMod.publicInvites;
      int toggleW = 130;
      int toggleH = 14;
      int toggleX = panelX + (this.PANEL_W - toggleW) / 2;
      ctx.fill(toggleX - 1, contentY - 1, toggleX + toggleW + 1, contentY + toggleH + 1, pub ? -12272828 : -7851213);
      ctx.fill(toggleX, contentY, toggleX + toggleW, contentY + toggleH, pub ? -15650031 : -14548992);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("Public Invites: " + (pub ? "ON" : "OFF")).formatted(pub ? Formatting.GREEN : Formatting.RED),
         toggleX + toggleW / 2,
         contentY + (toggleH - 9) / 2,
         -1
      );
      int var7 = contentY + toggleH + 8;
      List members = TurnBattleClientMod.clientPartyPlayers;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Your Party").formatted(Formatting.AQUA), x, var7, 16777215);
      var7 += lineH;
      if (members.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("  Solo").formatted(Formatting.DARK_GRAY), x, var7, 16777215);
         var7 += lineH;
      } else {
         for (TurnBattleClientMod.PartyMemberInfo m : members) {
            int rgb = m.soul() != null ? m.soul().getColor() : 11184810;
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal("  \u2726 " + (m.vesselName().isEmpty() ? m.name() : m.vesselName())), x, var7, 0xFF000000 | rgb
            );
            var7 += lineH;
         }
      }

      boolean hovLeave = mx >= x && mx < x + 100 && my >= var7 && my < var7 + 12;
      ctx.fill(x, var7, x + 100, var7 + 12, members.isEmpty() ? (hovLeave ? -12303292 : -14013910) : (hovLeave ? -10079437 : -12312030));
      ctx.drawTextWithShadow(
         this.textRenderer, Text.literal("  Leave Party").formatted(members.isEmpty() ? Formatting.DARK_GRAY : Formatting.RED), x + 2, var7 + 2, 16777215
      );
      var7 += 16;
      var7 += 4;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Invite Player").formatted(Formatting.AQUA), x, var7, 16777215);
      int refBtnW = 52;
      int refBtnH = 10;
      int refBtnX = panelX + this.PANEL_W - 14 - refBtnW;
      boolean refHov = mx >= refBtnX && mx < refBtnX + refBtnW && my >= var7 && my < var7 + refBtnH;
      ctx.fill(refBtnX, var7, refBtnX + refBtnW, var7 + refBtnH, refHov ? -13417370 : -15066573);
      ctx.fill(refBtnX, var7 + refBtnH - 2, refBtnX + refBtnW, var7 + refBtnH, refHov ? -7829249 : -13417336);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("\u27f3 Refresh").formatted(refHov ? Formatting.WHITE : Formatting.GRAY),
         refBtnX + refBtnW / 2,
         var7 + 1,
         refHov ? -1 : -7829334
      );
      var7 += lineH;
      List online = TurnBattleClientMod.onlinePlayers;
      Set inParty = new HashSet<>();

      for (TurnBattleClientMod.PartyMemberInfo m : members) {
         inParty.add(m.uuid());
      }

      List filtered = new ArrayList<>();

      for (TurnBattleClientMod.OnlinePlayerEntry op : online) {
         if (!inParty.contains(op.uuid())) {
            filtered.add(op);
         }
      }

      int listAreaY = var7;
      int listAreaH = contentY + this.PANEL_H - 10 - var7;
      int rowH = 13;
      int visibleRows = Math.max(1, listAreaH / rowH);
      int maxScroll = Math.max(0, filtered.size() - visibleRows);
      this.partyInviteScroll = Math.max(0, Math.min(this.partyInviteScroll, maxScroll));
      if (filtered.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("  (No other players online)").formatted(Formatting.DARK_GRAY), x, var7, 16777215);
      } else {
         for (int i = this.partyInviteScroll; i < filtered.size() && i < this.partyInviteScroll + visibleRows; i++) {
            TurnBattleClientMod.OnlinePlayerEntry opx = filtered.get(i);
            int ry = listAreaY + (i - this.partyInviteScroll) * rowH;
            boolean hov = mx >= x && mx < panelX + this.PANEL_W - 12 && my >= ry - 1 && my < ry + rowH - 2;
            if (hov) {
               ctx.fill(x, ry - 1, panelX + this.PANEL_W - 12, ry + rowH - 2, 1157627903);
            }

            ctx.drawTextWithShadow(this.textRenderer, Text.literal((hov ? "\u25b6 " : "  ") + opx.name()).formatted(Formatting.WHITE), x + 2, ry, 16777215);
            if (hov) {
               ctx.drawTextWithShadow(
                  this.textRenderer,
                  Text.literal(" [Invite]").formatted(Formatting.YELLOW),
                  x + 2 + this.textRenderer.getWidth("  " + opx.name()),
                  ry,
                  16777215
               );
            }
         }

         if (filtered.size() > visibleRows) {
            int sbX = panelX + this.PANEL_W - 10;
            int thumbH = Math.max(14, listAreaH * visibleRows / filtered.size());
            int thumbY = listAreaY + (listAreaH - thumbH) * this.partyInviteScroll / Math.max(1, maxScroll);
            ctx.fill(sbX, listAreaY, sbX + 4, listAreaY + listAreaH, -14540254);
            ctx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, -7829368);
         }
      }
   }

   private void renderSwitchTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      PartyMember[] members = PartyMember.values();
      this.hoveredMember = -1;
      int totalCards = members.length;
      int selCardW = this.PANEL_W / totalCards - 6;
      int selCardH = 140;
      int selSpacing = 6;
      int totalW = totalCards * selCardW + (totalCards - 1) * selSpacing;
      int startX = panelX + (this.PANEL_W - totalW) / 2;
      int cardY = contentY + (this.PANEL_H - selCardH) / 2;

      for (int i = 0; i < members.length; i++) {
         PartyMember m = members[i];
         int cx = startX + i * (selCardW + selSpacing);
         boolean selected = i == this.partyMemberOrd;
         boolean hover = mx >= cx && mx < cx + selCardW && my >= cardY && my < cardY + selCardH;
         if (hover) {
            this.hoveredMember = i;
         }

         int border = selected ? -8892 : (hover ? -7829334 : -13421739);
         ctx.fill(cx - 1, cardY - 1, cx + selCardW + 1, cardY + selCardH + 1, border);
         ctx.fill(cx, cardY, cx + selCardW, cardY + selCardH, selected ? -15066624 : (hover ? -15790302 : -16250856));
         int tcx = cx + selCardW / 2;
         int ty = cardY + 8;
         int lh = 9 + 2;
         Formatting nameColor = m == PartyMember.NONE ? Formatting.DARK_GRAY : (selected ? Formatting.YELLOW : Formatting.WHITE);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(m.displayName).formatted(new Formatting[]{Formatting.BOLD, nameColor}), tcx, ty, -1);
         ty += lh + 2;
         if (m == PartyMember.NONE) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Solo").formatted(Formatting.DARK_GRAY), tcx, ty, -10066330);
         } else {
            int mLvl = this.allPartyLevels[i];
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Lv. " + mLvl).formatted(Formatting.YELLOW), tcx, ty, -1);
            ty += lh;
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(m.playerClass.displayName).formatted(Formatting.LIGHT_PURPLE), tcx, ty, -1);
            ty += lh + 2;
            float mMax = 20.0F + this.allPartyStats[i].vitalityHp();
            float mHp = i == this.partyMemberOrd ? this.partyHp : mMax;
            int barW = selCardW - 12;
            int barX = cx + 6;
            ctx.fill(barX, ty, barX + barW, ty + 5, -13421773);
            float frac = mMax > 0.0F ? Math.min(1.0F, mHp / mMax) : 0.0F;
            int fc = frac > 0.5F ? -12255420 : (frac > 0.25F ? -188 : -48060);
            ctx.fill(barX, ty, barX + (int)((float)barW * frac), ty + 5, fc);
            ty += 8;

            for (String line : wrapText(m.playerClass.description, selCardW - 8)) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line).formatted(Formatting.GRAY), tcx, ty, -5592406);
               ty += lh;
            }

            if (selected) {
               ctx.drawCenteredTextWithShadow(
                  this.textRenderer, Text.literal("\u25b6 Active \u25c0").formatted(Formatting.GOLD), tcx, cardY + selCardH - lh - 6, -8892
               );
            }
         }
      }

      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Click to select").formatted(Formatting.DARK_GRAY), panelX + this.PANEL_W / 2, contentY + this.PANEL_H - 14, -11184794
      );
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (this.menuHidden) {
         return true;
      } else {
         this.updateScaledConstants();
         if (!Float.isNaN(this.menuCx)) {
            mx = this.toMenuX(mx);
            my = this.toMenuY(my);
         }

         int spriteX = (this.width - 284 * this.SCALE) / 2;
         int spriteY = (this.height - 129 * this.SCALE) / 2;
         int panelX = spriteX + 5 * this.SCALE;
         if (TurnBattleClientMod.isRogueliteWorld) {
            int tabAreaX = spriteX + this.TAB_AREA_X_OFF;
            int tabAreaY = spriteY + this.TAB_AREA_Y_OFF;
            int rbW = this.TAB_AREA_W - 8;
            int rbH = 14;
            int rbX = tabAreaX + 4;
            int rbY = tabAreaY + this.TAB_AREA_H + 6;
            if (mx >= (double)rbX && mx <= (double)(rbX + rbW) && my >= (double)rbY && my <= (double)(rbY + rbH)) {
               if (this.client != null) {
                  Net121.sendToServer(TurnBattleMod.ROGUE_INFO_REQUEST, PacketByteBufs.empty());
                  this.client.setScreen(new RogueliteScreen());
               }

               TurnBattleSounds.playSelect();
               return true;
            }
         }

         int closeX = spriteX + this.CLOSE_X_OFF;
         int closeY = spriteY + this.CLOSE_Y_OFF;
         if (mx >= (double)closeX && mx < (double)(closeX + this.CLOSE_W) && my >= (double)closeY && my < (double)(closeY + this.CLOSE_H)) {
            if (this.client != null) {
               this.client.setScreen(null);
            }

            TurnBattleSounds.playSelect();
            return true;
         } else {
            boolean showStoryClick = !TurnBattleClientMod.isRogueliteWorld;
            int numTabs = showStoryClick ? 6 : 5;
            int tabAreaX = spriteX + this.TAB_AREA_X_OFF;
            int tabAreaY = spriteY + this.TAB_AREA_Y_OFF;
            int tabHC = this.TAB_AREA_H / numTabs;
            if (mx >= (double)tabAreaX && mx < (double)(tabAreaX + this.TAB_AREA_W)) {
               for (int i = 0; i < numTabs; i++) {
                  if (my >= (double)(tabAreaY + i * tabHC) && my < (double)(tabAreaY + (i + 1) * tabHC)) {
                     if (showStoryClick && i == numTabs - 1) {
                        if (TurnBattleClientMod.storyModeEnabled) {
                           this.activeTab = i;
                           String active = TurnBattleClientMod.clientActiveQuestlineId;
                           if (active != null && !active.isEmpty()) {
                              this.storySelectedId = active;
                              this.storyView = 1;
                           } else {
                              this.storyView = 0;
                           }
                        }
                     } else if (i != 0 && (i < 2 || i > 4)) {
                        if (i == 1 && !TurnBattleClientMod.rogueliteInRun) {
                           this.openChild(new SkillSelectScreen(null));
                        } else {
                           this.activeTab = i;
                        }
                     } else {
                        this.openChild(
                           (Screen)(i == 0
                              ? new StatsScreen(null)
                              : (i == 2 ? new PartyMenuScreen(null) : (i == 3 ? new EquipScreen(null) : new ItemsScreen(null))))
                        );
                     }

                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }
            }

            if (this.activeTab == 0) {
               int contentY = spriteY + this.CONTENT_Y_OFF;

               for (Entry e : this.statMemberFaceRegions.entrySet()) {
                  int[] r = e.getValue();
                  int idx = e.getKey();
                  if (mx >= (double)r[0] && mx < (double)(r[0] + r[2]) && my >= (double)r[1] && my < (double)(r[1] + r[3]) && idx == 0) {
                     if (button == 1) {
                        DarkWorldSkinsClient.removeOwn();
                     } else {
                        DarkWorldSkinsClient.pickOwn();
                     }

                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  if (mx >= (double)r[0]
                     && mx < (double)(r[0] + r[2])
                     && my >= (double)r[1]
                     && my < (double)(r[1] + r[3])
                     && idx < this.statMemberLabelsCache.size()) {
                     String recruitName = this.statMemberLabelsCache.get(idx);
                     if (OriRender.isOri(recruitName)) {
                        return true;
                     }

                     RecruitSkinManager.pickSkinFile(recruitName, (success, error) -> {
                        if (this.client != null && this.client.player != null) {
                           if (success) {
                              this.client.player.sendMessage(Text.literal("Updated skin for " + recruitName + ".").formatted(Formatting.GREEN), false);
                           } else if (error != null) {
                              this.client.player.sendMessage(Text.literal("Skin picker error: " + error).formatted(Formatting.RED), false);
                           }
                        }
                     });
                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }

               for (Entry e : this.statMemberTabRegions.entrySet()) {
                  int[] rx = e.getValue();
                  if (mx >= (double)rx[0] && mx < (double)(rx[0] + rx[2]) && my >= (double)rx[1] && my < (double)(rx[1] + rx[3])) {
                     List statTargets = new ArrayList<>();
                     statTargets.add(0);
                     int seq = 1;

                     for (Recruit r2 : Recruit.values()) {
                        if (r2.ordinal() < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[r2.ordinal()]) {
                           statTargets.add(seq++);
                        }
                     }

                     String[] cNamesC = TurnBattleClientMod.customRecruitNames;
                     if (cNamesC != null) {
                        for (int ci = 0; ci < cNamesC.length; ci++) {
                           statTargets.add(20000 + ci);
                        }
                     }

                     int idxx = e.getKey();
                     if (idxx < statTargets.size()) {
                        this.statTarget = statTargets.get(idxx);
                     }

                     this.statSkillScrollIdx = 0;
                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }

               for (int[] btn : this.plusBtns) {
                  if (mx >= (double)btn[0] && mx < (double)(btn[0] + btn[2]) && my >= (double)btn[1] && my < (double)(btn[1] + btn[3])) {
                     this.sendStatSpend(this.statTarget, btn[4]);
                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }

               if (this.resetBtnRegion != null) {
                  int[] rx = this.resetBtnRegion;
                  if (mx >= (double)rx[0] && mx < (double)(rx[0] + rx[2]) && my >= (double)rx[1] && my < (double)(rx[1] + rx[3])) {
                     this.sendStatReset(this.statTarget);
                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }

               if (this.statSkillUpRegion != null) {
                  int[] rx = this.statSkillUpRegion;
                  if (mx >= (double)rx[0] && mx < (double)(rx[0] + rx[2]) && my >= (double)rx[1] && my < (double)(rx[1] + rx[3])) {
                     this.statSkillScrollIdx = Math.max(0, this.statSkillScrollIdx - 1);
                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }

               if (this.statSkillDownRegion != null) {
                  int[] rx = this.statSkillDownRegion;
                  if (mx >= (double)rx[0] && mx < (double)(rx[0] + rx[2]) && my >= (double)rx[1] && my < (double)(rx[1] + rx[3])) {
                     this.statSkillScrollIdx++;
                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }
            }

            if (this.activeTab == 2) {
               int[] subWs2 = new int[]{60, 68, 60};
               int subH2 = 14;
               int subGap2 = 4;
               int totalSubW2 = 0;

               for (int w : subWs2) {
                  totalSubW2 += w + subGap2;
               }

               totalSubW2 -= subGap2;
               int subStartX2 = panelX + (this.PANEL_W - totalSubW2) / 2;
               int subY2 = spriteY + this.CONTENT_Y_OFF + 4;
               int ssx = subStartX2;

               for (int ix = 0; ix < subWs2.length; ix++) {
                  if (mx >= (double)ssx && mx < (double)(ssx + subWs2[ix]) && my >= (double)subY2 && my < (double)(subY2 + subH2)) {
                     this.partySubTab = ix;
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  ssx += subWs2[ix] + subGap2;
               }

               int subContentY2 = subY2 + subH2 + 6;
               if (this.partySubTab == 0) {
                  int fH2 = 9;
                  int y2 = subContentY2 + 4 + fH2 + 4 + fH2 + 2 + fH2 + 8;
                  int cardH2 = 44;
                  int cardW2 = this.PANEL_W - 16;
                  int btnW2 = 46;
                  int btnH2 = 12;

                  for (Recruit rx : Recruit.values()) {
                     int ri = rx.ordinal();
                     if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
                        int b1x = panelX + 8 + cardW2 - btnW2 * 2 - 6;
                        int b2x = panelX + 8 + cardW2 - btnW2 - 2;
                        int by2 = y2 + cardH2 - btnH2 - 3;
                        if (mx >= (double)b1x && mx < (double)(b1x + btnW2) && my >= (double)by2 && my < (double)(by2 + btnH2)) {
                           int newActive = TurnBattleClientMod.clientActiveRecruit == ri ? -1 : ri;
                           TurnBattleClientMod.clientActiveRecruit = newActive;
                           PacketByteBuf rbuf = PacketByteBufs.create();
                           rbuf.writeInt(newActive);
                           Net121.sendToServer(TurnBattleMod.RECRUIT_SET_ACTIVE, rbuf);
                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        if (mx >= (double)b2x && mx < (double)(b2x + btnW2) && my >= (double)by2 && my < (double)(by2 + btnH2)) {
                           int newSecond = TurnBattleClientMod.clientSecondRecruit == ri ? -1 : ri;
                           TurnBattleClientMod.clientSecondRecruit = newSecond;
                           PacketByteBuf rbuf = PacketByteBufs.create();
                           rbuf.writeInt(newSecond);
                           Net121.sendToServer(TurnBattleMod.SET_SECOND_RECRUIT, rbuf);
                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        y2 += cardH2 + 6;
                     }
                  }

                  String[] cNames = TurnBattleClientMod.customRecruitNames;
                  if (cNames != null) {
                     for (int ci = 0; ci < cNames.length; ci++) {
                        int slotCode = 1000 + ci;
                        int b1xx = panelX + 8 + cardW2 - btnW2 * 2 - 6;
                        int b2xx = panelX + 8 + cardW2 - btnW2 - 2;
                        int by2x = y2 + cardH2 - btnH2 - 3;
                        if (mx >= (double)b1xx && mx < (double)(b1xx + btnW2) && my >= (double)by2x && my < (double)(by2x + btnH2)) {
                           int newActive = TurnBattleClientMod.clientActiveRecruit == slotCode ? -1 : slotCode;
                           TurnBattleClientMod.clientActiveRecruit = newActive;
                           PacketByteBuf rbuf = PacketByteBufs.create();
                           rbuf.writeInt(newActive);
                           Net121.sendToServer(TurnBattleMod.RECRUIT_SET_ACTIVE, rbuf);
                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        if (mx >= (double)b2xx && mx < (double)(b2xx + btnW2) && my >= (double)by2x && my < (double)(by2x + btnH2)) {
                           int newSecond = TurnBattleClientMod.clientSecondRecruit == slotCode ? -1 : slotCode;
                           TurnBattleClientMod.clientSecondRecruit = newSecond;
                           PacketByteBuf rbuf = PacketByteBufs.create();
                           rbuf.writeInt(newSecond);
                           Net121.sendToServer(TurnBattleMod.SET_SECOND_RECRUIT, rbuf);
                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        y2 += cardH2 + 6;
                     }
                  }
               } else if (this.partySubTab == 1) {
                  int vcY = subContentY2 + 8 + 9 + 6 + 9 + 12;
                  int btnW2 = 160;
                  int btnH2 = 20;
                  int btnX2 = panelX + (this.PANEL_W - btnW2) / 2;
                  if (mx >= (double)btnX2 && mx < (double)(btnX2 + btnW2) && my >= (double)vcY && my < (double)(vcY + btnH2)) {
                     if (this.client != null) {
                        this.client.setScreen(new VesselCreatorScreen(this));
                     }

                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  vcY += btnH2 + 8;
                  vcY += btnH2 + 8;
                  vcY += 9 + 4;
                  String[] cNamesV = TurnBattleClientMod.customRecruitNames;
                  boolean hasCustomV = cNamesV != null && cNamesV.length > 0;
                  if (hasCustomV) {
                     for (int ci = 0; ci < cNamesV.length; ci++) {
                        int xBtnX = btnX2 + btnW2 - 16;
                        if (mx >= (double)xBtnX && mx < (double)(xBtnX + 14) && my >= (double)(vcY + 2) && my < (double)(vcY + 14)) {
                           this.removeVessel(ci);
                           return true;
                        }

                        vcY += 18;
                     }
                  }
               } else {
                  int subH3 = 14;
                  int toggleW = 130;
                  int toggleH = 14;
                  int toggleX = panelX + (this.PANEL_W - toggleW) / 2;
                  if (mx >= (double)toggleX && mx < (double)(toggleX + toggleW) && my >= (double)subContentY2 && my < (double)(subContentY2 + toggleH)) {
                     TurnBattleClientMod.publicInvites = !TurnBattleClientMod.publicInvites;
                     PacketByteBuf tb = PacketByteBufs.create();
                     tb.writeBoolean(TurnBattleClientMod.publicInvites);
                     Net121.sendToServer(TurnBattleMod.PARTY_INVITE_TOGGLE, tb);
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  int lineH2 = 9 + 4;
                  int y3 = subContentY2 + toggleH + 8 + lineH2;
                  List currentParty = TurnBattleClientMod.clientPartyPlayers;
                  y3 += currentParty.isEmpty() ? lineH2 : currentParty.size() * lineH2;
                  int x2 = panelX + 8;
                  if (mx >= (double)x2 && mx < (double)(x2 + 100) && my >= (double)y3 && my < (double)(y3 + 12)) {
                     Net121.sendToServer(TurnBattleMod.PARTY_LEAVE, PacketByteBufs.empty());
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  y3 += 20 + lineH2;
                  int refBW = 52;
                  int refBH = 10;
                  int refBX = panelX + this.PANEL_W - 14 - refBW;
                  int refBY = y3 - lineH2;
                  if (mx >= (double)refBX && mx < (double)(refBX + refBW) && my >= (double)refBY && my < (double)(refBY + refBH)) {
                     Net121.sendToServer(TurnBattleMod.ROGUE_INFO_REQUEST, PacketByteBufs.empty());
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  Set alreadyIn = new HashSet<>();

                  for (TurnBattleClientMod.PartyMemberInfo m : currentParty) {
                     alreadyIn.add(m.uuid());
                  }

                  List invitable = new ArrayList<>();

                  for (TurnBattleClientMod.OnlinePlayerEntry op : TurnBattleClientMod.onlinePlayers) {
                     if (!alreadyIn.contains(op.uuid())) {
                        invitable.add(op);
                     }
                  }

                  for (int ix = this.partyInviteScroll; ix < invitable.size(); ix++) {
                     int ry = y3 + (ix - this.partyInviteScroll) * 13;
                     if (ry > spriteY + this.CONTENT_Y_OFF + this.PANEL_H - 10) {
                        break;
                     }

                     if (mx >= (double)x2 && mx < (double)(panelX + this.PANEL_W - 12) && my >= (double)(ry - 1) && my < (double)(ry + 12)) {
                        PacketByteBuf ib = PacketByteBufs.create();
                        ib.writeUuid(invitable.get(ix).uuid());
                        Net121.sendToServer(TurnBattleMod.PARTY_INVITE, ib);
                        TurnBattleSounds.playSelect();
                        return true;
                     }
                  }
               }
            }

            if (this.activeTab == 1 && !TurnBattleClientMod.rogueliteInRun) {
               for (int[] btnx : this.skillMemberBtns) {
                  if (mx >= (double)btnx[0] && mx < (double)(btnx[0] + btnx[2]) && my >= (double)btnx[1] && my < (double)(btnx[1] + btnx[3])) {
                     int memberIdx = btnx[4];
                     if (memberIdx == 0) {
                        TurnBattleClientMod.pendingCodexOpen = true;
                        Net121.sendToServer(TurnBattleMod.SKILL_INFO_REQUEST, PacketByteBufs.empty());
                     } else if (memberIdx >= 10000) {
                        int ci = memberIdx - 10000;
                        String[] cNames = TurnBattleClientMod.customRecruitNames;
                        int[] cClasses = TurnBattleClientMod.customRecruitClasses;
                        int[] cSouls = TurnBattleClientMod.customRecruitSouls;
                        int[] cLevels = TurnBattleClientMod.customRecruitLevels;
                        int[] cSp = TurnBattleClientMod.customRecruitSkillPoints;
                        Set[] cSkills = TurnBattleClientMod.customRecruitSkills;
                        if (cNames != null && ci < cNames.length && this.client != null) {
                           PlayerClass cls = cClasses[ci] >= 0 && cClasses[ci] < PlayerClass.values().length
                              ? PlayerClass.values()[cClasses[ci]]
                              : PlayerClass.HUMAN_MAGE;
                           SoulType st = cSouls[ci] >= 0 && cSouls[ci] < SoulType.values().length ? SoulType.values()[cSouls[ci]] : null;
                           Set skills = (Set<String>)(cSkills != null && ci < cSkills.length && cSkills[ci] != null ? cSkills[ci] : new HashSet<>());
                           int sp = cSp != null && ci < cSp.length ? cSp[ci] : 0;
                           this.client.setScreen(new SkillCodexScreen(cNames[ci], cls, st, cLevels[ci], sp, skills, ci));
                        }
                     } else {
                        int recruitOrd = this.resolveRecruitOrdFromStatTarget(memberIdx);
                        if (recruitOrd >= 0) {
                           Recruit rxx = Recruit.values()[recruitOrd];
                           if (this.client != null) {
                              this.client
                                 .setScreen(
                                    new SkillCodexScreen(
                                       rxx,
                                       TurnBattleClientMod.clientRecruitLevels[recruitOrd],
                                       TurnBattleClientMod.clientRecruitSkillPoints[recruitOrd],
                                       TurnBattleClientMod.clientRecruitSkills[recruitOrd]
                                    )
                                 );
                           }
                        }
                     }

                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }
            }

            if (this.activeTab == 5 && !TurnBattleClientMod.isRogueliteWorld) {
               if (this.storyView == 0) {
                  int rxx = 12;
                  int baseY = spriteY + this.CONTENT_Y_OFF + 6 + 9 + 14;
                  int oriCX = panelX + this.PANEL_W / 2;
                  int oriCY = baseY + rxx;
                  int branchY = oriCY + rxx + 28;
                  Map pos = new LinkedHashMap<>();
                  pos.put("ori", new int[]{oriCX, oriCY});
                  pos.put("avery", new int[]{panelX + this.PANEL_W / 2 - 44, branchY});
                  pos.put("hazel", new int[]{panelX + this.PANEL_W / 2 + 44, branchY});

                  for (QuestlineManager.QuestlineMeta ql : QuestlineManager.ALL_QUESTLINES) {
                     int[] p = pos.get(ql.id());
                     if (p != null) {
                        boolean prereqMet = this.checkPrereqs(ql.prereqs());
                        boolean levelOk = this.playerLevel >= ql.levelRequired();
                        boolean locked = !prereqMet || !levelOk || TurnBattleClientMod.clientCompletedQuestlines.contains(ql.id());
                        if (!locked && mx >= (double)(p[0] - rxx) && mx <= (double)(p[0] + rxx) && my >= (double)(p[1] - rxx) && my <= (double)(p[1] + rxx)) {
                           this.storySelectedId = ql.id();
                           this.storyView = 1;
                           TurnBattleSounds.playSelect();
                           return true;
                        }
                     }
                  }
               } else {
                  int contentY2 = spriteY + this.CONTENT_Y_OFF;
                  if (this.hoverQlBack) {
                     this.storyView = 0;
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  if (this.hoverQlAccept) {
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeString(this.storySelectedId);
                     Net121.sendToServer(TurnBattleMod.QUESTLINE_START, buf);
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  if (this.hoverQlRetry) {
                     PacketByteBuf rb = PacketByteBufs.create();
                     rb.writeString(this.storySelectedId);
                     Net121.sendToServer(TurnBattleMod.QUESTLINE_RETRY, rb);
                     if (this.client != null) {
                        this.client.setScreen(null);
                     }

                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }
            }

            if (this.activeTab == 3) {
               int subTabH = 16;
               int contentY = spriteY + this.CONTENT_Y_OFF;
               Recruit[] recs2 = Recruit.values();
               int accIdx2 = 1 + recs2.length;
               int contentY2x = spriteY + this.CONTENT_Y_OFF;
               int navX2 = panelX + 4;
               int navY2 = contentY2x + 4;
               int rowH2 = 20;
               int navW2 = 64;
               int armorTabIdx2 = accIdx2 + 1;
               int armorBW = 60;
               int tabBH = 14;
               int armorBX = panelX + this.PANEL_W - armorBW - 6;
               int armorBY2 = contentY2x + 4;
               if (mx >= (double)armorBX && mx < (double)(armorBX + armorBW) && my >= (double)armorBY2 && my < (double)(armorBY2 + tabBH)) {
                  this.equipSubTab = armorTabIdx2;
                  TurnBattleSounds.playSelect();
                  return true;
               }

               int accBW = 80;
               int accBH = 14;
               int accBX = armorBX - accBW - 4;
               int accBY = contentY2x + 4;
               if (mx >= (double)accBX && mx < (double)(accBX + accBW) && my >= (double)accBY && my < (double)(accBY + accBH)) {
                  this.equipSubTab = accIdx2;
                  TurnBattleSounds.playSelect();
                  return true;
               }

               if (this.equipSubTab == armorTabIdx2) {
                  int backW2 = 44;
                  int backH2 = 14;
                  int backX2 = panelX + 8;
                  int backY2 = contentY2x + 6;
                  if (mx >= (double)backX2 && mx < (double)(backX2 + backW2) && my >= (double)backY2 && my < (double)(backY2 + backH2)) {
                     this.equipSubTab = 0;
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  for (int[] btnxx : this.armorRollBtns) {
                     if (mx >= (double)btnxx[0] && mx < (double)(btnxx[0] + btnxx[2]) && my >= (double)btnxx[1] && my < (double)(btnxx[1] + btnxx[3])) {
                        PacketByteBuf rbuf = PacketByteBufs.create();
                        rbuf.writeInt(btnxx[4]);
                        Net121.sendToServer(TurnBattleMod.ARMOR_ROLL_REQUEST, rbuf);
                        TurnBattleSounds.playSelect();
                        return true;
                     }
                  }

                  for (int[] btnxxx : this.armorBtns) {
                     if (mx >= (double)btnxxx[0] && mx < (double)(btnxxx[0] + btnxxx[2]) && my >= (double)btnxxx[1] && my < (double)(btnxxx[1] + btnxxx[3])) {
                        int code = btnxxx[4];
                        if (code != -9001 && code != -9002) {
                           PacketByteBuf abuf = PacketByteBufs.create();
                           if (code < 0) {
                              int armorIndex = -code - 1;
                              if (this.armTargetSlot == -1) {
                                 abuf.writeInt(armorIndex);
                                 Net121.sendToServer(TurnBattleMod.UNEQUIP_ARMOR, abuf);
                              } else {
                                 abuf.writeInt(this.armTargetSlot);
                                 abuf.writeInt(armorIndex);
                                 Net121.sendToServer(TurnBattleMod.RECRUIT_UNEQUIP_ARMOR, abuf);
                              }
                           } else {
                              int invSlot = code / 10;
                              int armorIndex = code % 10;
                              if (this.armTargetSlot == -1) {
                                 abuf.writeInt(invSlot);
                                 abuf.writeInt(armorIndex);
                                 Net121.sendToServer(TurnBattleMod.EQUIP_ARMOR, abuf);
                              } else {
                                 abuf.writeInt(this.armTargetSlot);
                                 abuf.writeInt(armorIndex);
                                 abuf.writeInt(invSlot);
                                 Net121.sendToServer(TurnBattleMod.RECRUIT_EQUIP_ARMOR, abuf);
                              }
                           }

                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        List targets = this.buildAccTargetList();
                        int idxx = targets.indexOf(this.armTargetSlot);
                        if (idxx < 0) {
                           idxx = 0;
                        }

                        idxx = code == -9001 ? (idxx - 1 + targets.size()) % targets.size() : (idxx + 1) % targets.size();
                        this.armTargetSlot = targets.get(idxx);
                        TurnBattleSounds.playSelect();
                        return true;
                     }
                  }

                  return false;
               }

               if (this.equipSubTab == accIdx2) {
                  int backW2 = 44;
                  int backH2 = 14;
                  int backX2 = panelX + 8;
                  int backY2 = contentY2x + 6;
                  if (mx >= (double)backX2 && mx < (double)(backX2 + backW2) && my >= (double)backY2 && my < (double)(backY2 + backH2)) {
                     this.equipSubTab = 0;
                     TurnBattleSounds.playSelect();
                     return true;
                  }

                  for (int[] btnxxxx : this.accBtns) {
                     if (mx >= (double)btnxxxx[0]
                        && mx < (double)(btnxxxx[0] + btnxxxx[2])
                        && my >= (double)btnxxxx[1]
                        && my < (double)(btnxxxx[1] + btnxxxx[3])) {
                        int slotIndex = btnxxxx[4];
                        if (slotIndex != -9001 && slotIndex != -9002) {
                           PacketByteBuf buf = PacketByteBufs.create();
                           if (slotIndex < 0) {
                              if (this.accTargetSlot == -1) {
                                 Net121.sendToServer(TurnBattleMod.UNEQUIP_ACCESSORY, buf);
                              } else {
                                 buf.writeInt(this.accTargetSlot);
                                 Net121.sendToServer(TurnBattleMod.RECRUIT_UNEQUIP_ACCESSORY, buf);
                              }
                           } else if (this.accTargetSlot == -1) {
                              buf.writeInt(slotIndex);
                              Net121.sendToServer(TurnBattleMod.EQUIP_ACCESSORY, buf);
                           } else {
                              buf.writeInt(this.accTargetSlot);
                              buf.writeInt(slotIndex);
                              Net121.sendToServer(TurnBattleMod.RECRUIT_EQUIP_ACCESSORY, buf);
                           }

                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        List targets = this.buildAccTargetList();
                        int idxx = targets.indexOf(this.accTargetSlot);
                        if (idxx < 0) {
                           idxx = 0;
                        }

                        idxx = slotIndex == -9001 ? (idxx - 1 + targets.size()) % targets.size() : (idxx + 1) % targets.size();
                        this.accTargetSlot = targets.get(idxx);
                        TurnBattleSounds.playSelect();
                        return true;
                     }
                  }

                  return false;
               }

               int rowY2 = navY2 + 4;
               if (mx >= (double)(navX2 + 2) && mx < (double)(navX2 + navW2 - 2) && my >= (double)rowY2 && my < (double)(rowY2 + rowH2)) {
                  this.equipSelectedTarget = 0;
                  TurnBattleSounds.playSelect();
                  return true;
               }

               rowY2 += rowH2 + 2;

               for (int ri = 0; ri < recs2.length; ri++) {
                  boolean unl = ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri];
                  if (unl) {
                     if (mx >= (double)(navX2 + 2) && mx < (double)(navX2 + navW2 - 2) && my >= (double)rowY2 && my < (double)(rowY2 + rowH2)) {
                        this.equipSelectedTarget = ri + 1;
                        TurnBattleSounds.playSelect();
                        return true;
                     }

                     rowY2 += rowH2 + 2;
                  }
               }

               int customBase2 = recs2.length + 1;
               String[] cNames2 = TurnBattleClientMod.customRecruitNames;
               if (cNames2 != null) {
                  for (int ci = 0; ci < cNames2.length; ci++) {
                     if (mx >= (double)(navX2 + 2) && mx < (double)(navX2 + navW2 - 2) && my >= (double)rowY2 && my < (double)(rowY2 + rowH2)) {
                        this.equipSelectedTarget = customBase2 + ci;
                        TurnBattleSounds.playSelect();
                        return true;
                     }

                     rowY2 += rowH2 + 2;
                  }
               }

               for (int[] btnxxxxx : this.rollBtns) {
                  if (mx >= (double)btnxxxxx[0]
                     && mx < (double)(btnxxxxx[0] + btnxxxxx[2])
                     && my >= (double)btnxxxxx[1]
                     && my < (double)(btnxxxxx[1] + btnxxxxx[3])) {
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeInt(btnxxxxx[4]);
                     Net121.sendToServer(TurnBattleMod.WEAPON_ROLL_REQUEST, buf);
                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }

               for (int[] btnxxxxxx : this.equipBtns) {
                  if (mx >= (double)btnxxxxxx[0]
                     && mx < (double)(btnxxxxxx[0] + btnxxxxxx[2])
                     && my >= (double)btnxxxxxx[1]
                     && my < (double)(btnxxxxxx[1] + btnxxxxxx[3])) {
                     int slot = btnxxxxxx[4];
                     PacketByteBuf buf = PacketByteBufs.create();
                     if (slot == -1) {
                        Net121.sendToServer(TurnBattleMod.UNEQUIP_WEAPON, buf);
                     } else if (slot <= -100) {
                        buf.writeInt(-(slot + 100));
                        Net121.sendToServer(TurnBattleMod.RECRUIT_UNEQUIP_WEAPON, buf);
                     } else if (slot >= 10000) {
                        buf.writeInt((slot - 10000) / 1000);
                        buf.writeInt((slot - 10000) % 1000);
                        Net121.sendToServer(TurnBattleMod.RECRUIT_EQUIP_WEAPON, buf);
                     } else {
                        buf.writeInt(slot);
                        buf.writeBoolean(false);
                        Net121.sendToServer(TurnBattleMod.EQUIP_WEAPON, buf);
                     }

                     TurnBattleSounds.playSelect();
                     return true;
                  }
               }
            }

            if (this.activeTab == 4) {
               int contentY2xx = spriteY + this.CONTENT_Y_OFF;
               int fH = 9;
               int listX = panelX + 8;
               int listW = this.PANEL_W - 16;
               int y = contentY2xx + 4 + fH + 6;
               ClientPlayerEntity lp = MinecraftClient.getInstance().player;
               if (lp != null) {
                  for (int si = 0; si < lp.getInventory().size(); si++) {
                     ItemStack st = lp.getInventory().getStack(si);
                     if (!st.isEmpty() && st.get(DataComponentTypes.FOOD) != null) {
                        if (y > contentY2xx + this.PANEL_H - fH - 4) {
                           break;
                        }

                        int rowH = fH + 6;
                        String id = Registries.ITEM.getId(st.getItem()).toString();
                        int btnW2x = 26;
                        int btnH2x = fH + 4;
                        int btnY = y - 1;
                        if (mx >= (double)listX && mx < (double)(listX + 14) && my >= (double)btnY && my < (double)(btnY + btnH2x)) {
                           PacketByteBuf fb = PacketByteBufs.create();
                           fb.writeString(id, 128);
                           Net121.sendToServer(TurnBattleMod.FAVORITE_TOGGLE, fb);
                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        boolean hasSlot2 = TurnBattleClientMod.clientSecondRecruit >= 0;
                        boolean hasSlot1 = TurnBattleClientMod.clientActiveRecruit >= 0;
                        int btnBaseX = listX + listW - btnW2x;
                        if (hasSlot2) {
                           if (mx >= (double)btnBaseX && mx < (double)(btnBaseX + btnW2x) && my >= (double)btnY && my < (double)(btnY + btnH2x)) {
                              PacketByteBuf fb = PacketByteBufs.create();
                              fb.writeInt(si);
                              fb.writeInt(2);
                              Net121.sendToServer(TurnBattleMod.ITEM_FEED, fb);
                              TurnBattleSounds.playSelect();
                              return true;
                           }

                           btnBaseX -= btnW2x + 2;
                        }

                        if (hasSlot1) {
                           if (mx >= (double)btnBaseX && mx < (double)(btnBaseX + btnW2x) && my >= (double)btnY && my < (double)(btnY + btnH2x)) {
                              PacketByteBuf fb = PacketByteBufs.create();
                              fb.writeInt(si);
                              fb.writeInt(1);
                              Net121.sendToServer(TurnBattleMod.ITEM_FEED, fb);
                              TurnBattleSounds.playSelect();
                              return true;
                           }

                           btnBaseX -= btnW2x + 2;
                        }

                        if (mx >= (double)btnBaseX && mx < (double)(btnBaseX + btnW2x) && my >= (double)btnY && my < (double)(btnY + btnH2x)) {
                           PacketByteBuf fb = PacketByteBufs.create();
                           fb.writeInt(si);
                           fb.writeInt(0);
                           Net121.sendToServer(TurnBattleMod.ITEM_FEED, fb);
                           TurnBattleSounds.playSelect();
                           return true;
                        }

                        y += rowH;
                     }
                  }
               }
            }

            return super.mouseClicked(mx, my, button);
         }
      }
   }

   private void removeVessel(int index) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(index);
      Net121.sendToServer(TurnBattleMod.REMOVE_VESSEL, buf);
      String[] names = TurnBattleClientMod.customRecruitNames;
      if (names != null && index < names.length) {
         String[] updated = new String[names.length - 1];
         System.arraycopy(names, 0, updated, 0, index);
         System.arraycopy(names, index + 1, updated, index, names.length - index - 1);
         TurnBattleClientMod.customRecruitNames = updated;
      }

      TurnBattleSounds.playSelect();
   }

   private void sendStatSpend(int targetType, int statIndex) {
      if (targetType == 0) {
         if (this.playerStats.unspent <= 0) {
            return;
         }

         if (statIndex == 5 && this.playerStats.isMaxed(5)) {
            return;
         }

         this.playerStats.spendPoint(statIndex);
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeByte(0);
         buf.writeByte(statIndex);
         Net121.sendToServer(TurnBattleMod.STAT_SPEND, buf);
      } else if (targetType >= 20000) {
         int ci = targetType - 20000;
         CharStats[] cStats = TurnBattleClientMod.customRecruitStats;
         if (cStats == null || ci >= cStats.length || cStats[ci] == null) {
            return;
         }

         CharStats rs = cStats[ci];
         if (rs.unspent <= 0) {
            return;
         }

         if (statIndex == 5 && rs.isMaxed(5)) {
            return;
         }

         rs.unspent--;
         switch (statIndex) {
            case 0:
               rs.offense++;
               break;
            case 1:
               rs.defense++;
               break;
            case 2:
               rs.magic++;
               break;
            case 3:
               rs.iq++;
               break;
            case 4:
               rs.vitality++;
               break;
            case 5:
               rs.luck++;
         }

         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(1000 + ci);
         buf.writeInt(statIndex);
         Net121.sendToServer(TurnBattleMod.RECRUIT_STAT_SPEND, buf);
      } else {
         int recruitOrd = this.resolveRecruitOrdFromStatTarget(targetType);
         if (recruitOrd < 0) {
            return;
         }

         CharStats rsx = TurnBattleClientMod.clientRecruitStats[recruitOrd];
         if (rsx == null || rsx.unspent <= 0) {
            return;
         }

         if (statIndex == 5 && rsx.isMaxed(5)) {
            return;
         }

         rsx.unspent--;
         switch (statIndex) {
            case 0:
               rsx.offense++;
               break;
            case 1:
               rsx.defense++;
               break;
            case 2:
               rsx.magic++;
               break;
            case 3:
               rsx.iq++;
               break;
            case 4:
               rsx.vitality++;
               break;
            case 5:
               rsx.luck++;
         }

         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(recruitOrd);
         buf.writeInt(statIndex);
         Net121.sendToServer(TurnBattleMod.RECRUIT_STAT_SPEND, buf);
      }
   }

   private int resolveRecruitOrdFromStatTarget(int target) {
      int seen = 0;

      for (Recruit r : Recruit.values()) {
         int ri = r.ordinal();
         if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
            if (++seen == target) {
               return ri;
            }
         }
      }

      return -1;
   }

   private void sendStatReset(int targetType) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(targetType);
      Net121.sendToServer(TurnBattleMod.STAT_RESET, buf);
   }

   private void selectMember(int ordinal) {
      this.partyMemberOrd = ordinal;
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(ordinal);
      Net121.sendToServer(TurnBattleMod.PARTY_SWITCH, buf);
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double amount) {
      if (this.menuHidden) {
         return true;
      } else if (this.activeTab == 2 && this.partySubTab == 2) {
         List online = TurnBattleClientMod.onlinePlayers;
         Set inParty = new HashSet<>();

         for (TurnBattleClientMod.PartyMemberInfo m : TurnBattleClientMod.clientPartyPlayers) {
            inParty.add(m.uuid());
         }

         int filteredCount = (int)online.stream().filter(o -> !inParty.contains(o.uuid())).count();
         int maxScroll = Math.max(0, filteredCount - 5);
         this.partyInviteScroll = (int)Math.max(0.0, Math.min((double)maxScroll, (double)this.partyInviteScroll - amount));
         return true;
      } else if (this.activeTab == 3) {
         Recruit[] scrollRecs = Recruit.values();
         int armorTabIdxScroll = 2 + scrollRecs.length;
         if (this.equipSubTab == armorTabIdxScroll) {
            int armorListH = this.PANEL_H - 26 - 96 - 30;
            int maxArmorScroll = Math.max(0, this.invArmorStacks.length * 44 - armorListH);
            this.armorScrollOffset = (int)Math.max(0.0, Math.min((double)maxArmorScroll, (double)this.armorScrollOffset - amount * 20.0));
         }

         int accTabIdxScroll = 1 + scrollRecs.length;
         if (this.equipSubTab == accTabIdxScroll) {
            int maxScroll = Math.max(0, this.invAccStacks.length * 56 - this.accListH);
            this.accScrollOffset = (int)Math.max(0.0, Math.min((double)maxScroll, (double)this.accScrollOffset - amount * 20.0));
         } else {
            int maxScroll = Math.max(0, this.invEquipStacks.length * 56 - (this.PANEL_H - 80));
            this.equipScrollOffset = (int)Math.max(0.0, Math.min((double)maxScroll, (double)this.equipScrollOffset - amount * 20.0));
         }

         return true;
      } else {
         return super.mouseScrolled(mx, my, scrollX, amount);
      }
   }

   private void renderVesselSubTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      this.vesselBtns.clear();
      int x = panelX + 8;
      int y = contentY + 6;
      int w = this.PANEL_W - 16;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Choose a vessel to represent yourself in battle.").formatted(Formatting.GRAY), x, y, -3355444);
      y += 9 + 8;
      int cardH = 40;
      int gap = 6;

      for (int i = 0; i < VESSEL_NAMES.length; i++) {
         boolean selected = this.currentVessel == i;
         boolean hover = mx >= x && mx < x + w && my >= y && my < y + cardH;
         int bg = selected ? -14535851 : (hover ? -15064525 : -15921894);
         ctx.fill(x, y, x + w, y + cardH, bg);
         ctx.fill(x, y, x + w, y + 1, selected ? -10057473 : -13417370);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(VESSEL_NAMES[i]).formatted(selected ? Formatting.AQUA : Formatting.WHITE), x + 8, y + 6, -1);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(VESSEL_DESCS[i]).formatted(Formatting.GRAY), x + 8, y + 6 + 9 + 3, -5592406);
         if (selected) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u2714").formatted(Formatting.GREEN), x + w - 12, y + (cardH - 9) / 2, -12255420);
         }

         this.vesselBtns.add(new int[]{x, y, x + w, y + cardH, i});
         y += cardH + gap;
      }
   }

   private void renderSkillMemberList(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      this.skillMemberBtns.clear();
      int y = contentY + 8;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Select a member to view their skill tree:").formatted(Formatting.GRAY), panelX + 8, y, -7829368);
      y += 9 + 6;
      int cardH = 24;
      int cardW = this.PANEL_W - 16;
      String vesselLabel = !TurnBattleClientMod.clientVesselName.isEmpty() ? TurnBattleClientMod.clientVesselName : "You";
      boolean hov0 = mx >= panelX + 8 && mx < panelX + 8 + cardW && my >= y && my < y + cardH;
      ctx.fill(panelX + 7, y - 1, panelX + 8 + cardW + 1, y + cardH + 1, hov0 ? -11184760 : -13421739);
      ctx.fill(panelX + 8, y, panelX + 8 + cardW, y + cardH, -16119262);
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal(vesselLabel + "  Lv." + this.playerLevel).formatted(Formatting.WHITE).append(magicantTag()),
         panelX + 14,
         y + (cardH - 9) / 2,
         -1
      );
      if (TurnBattleClientMod.clientPlayerClass != null) {
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal(TurnBattleClientMod.clientPlayerClass.displayName).formatted(Formatting.GRAY),
            panelX + 14 + 120,
            y + (cardH - 9) / 2,
            -7829368
         );
      }

      this.skillMemberBtns.add(new int[]{panelX + 8, y, cardW, cardH, 0});
      y += cardH + 4;
      int memberIdx = 1;

      for (Recruit r : Recruit.values()) {
         int ri = r.ordinal();
         if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
            int rlvl = TurnBattleClientMod.clientRecruitLevels[ri];
            boolean hov = mx >= panelX + 8 && mx < panelX + 8 + cardW && my >= y && my < y + cardH;
            int soulCol = r.soulType != null ? r.soulType.getColor() & 16777215 | 1711276032 : 1722460842;
            ctx.fill(panelX + 7, y - 1, panelX + 8 + cardW + 1, y + cardH + 1, soulCol);
            ctx.fill(panelX + 8, y, panelX + 8 + cardW, y + cardH, hov ? -15395550 : -16119270);
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal(r.displayName + "  Lv." + rlvl).formatted(Formatting.WHITE), panelX + 14, y + (cardH - 9) / 2, -1
            );
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal(r.playerClass.displayName).formatted(Formatting.GRAY), panelX + 14 + 120, y + (cardH - 9) / 2, -7829368
            );
            int[] var10001 = new int[]{panelX + 8, y, cardW, cardH, 0};
            var10001[4] = memberIdx++;
            this.skillMemberBtns.add(var10001);
            y += cardH + 4;
         }
      }

      String[] cNames = TurnBattleClientMod.customRecruitNames;
      int[] cClasses = TurnBattleClientMod.customRecruitClasses;
      int[] cSouls = TurnBattleClientMod.customRecruitSouls;
      int[] cLevels = TurnBattleClientMod.customRecruitLevels;
      if (cNames != null) {
         for (int ci = 0; ci < cNames.length; ci++) {
            PlayerClass cls = cClasses[ci] >= 0 && cClasses[ci] < PlayerClass.values().length ? PlayerClass.values()[cClasses[ci]] : PlayerClass.HUMAN_MAGE;
            SoulType st = cSouls[ci] >= 0 && cSouls[ci] < SoulType.values().length ? SoulType.values()[cSouls[ci]] : null;
            int soulCol = st != null ? st.getColor() & 16777215 | 1711276032 : 1722460842;
            boolean hov = mx >= panelX + 8 && mx < panelX + 8 + cardW && my >= y && my < y + cardH;
            ctx.fill(panelX + 7, y - 1, panelX + 8 + cardW + 1, y + cardH + 1, soulCol);
            ctx.fill(panelX + 8, y, panelX + 8 + cardW, y + cardH, hov ? -15395550 : -16119270);
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal(cNames[ci] + " [Custom]  Lv." + cLevels[ci]).formatted(Formatting.WHITE), panelX + 14, y + (cardH - 9) / 2, -1
            );
            ctx.drawTextWithShadow(
               this.textRenderer, Text.literal(cls.displayName).formatted(Formatting.GRAY), panelX + 14 + 120, y + (cardH - 9) / 2, -7829368
            );
            this.skillMemberBtns.add(new int[]{panelX + 8, y, cardW, cardH, 10000 + ci});
            y += cardH + 4;
         }
      }
   }

   private void renderRecruitsTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      int y = contentY + 4;
      int cardH = 44;
      int cardW = this.PANEL_W - 16;
      int btnW = 46;
      int btnH = 12;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Active Party").formatted(Formatting.GOLD), panelX + 8, y, -8892);
      y += 9 + 4;
      String slot1Name = TurnBattleClientMod.clientActiveRecruit >= 0 ? this.getRecruitDisplayName(TurnBattleClientMod.clientActiveRecruit) : "Empty";
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal("Slot 1: ")
            .formatted(Formatting.GRAY)
            .append(Text.literal(slot1Name).formatted(TurnBattleClientMod.clientActiveRecruit >= 0 ? Formatting.AQUA : Formatting.DARK_GRAY)),
         panelX + 10,
         y,
         -1
      );
      y += 9 + 2;
      String slot2Name = TurnBattleClientMod.clientSecondRecruit >= 0 ? this.getRecruitDisplayName(TurnBattleClientMod.clientSecondRecruit) : "Empty";
      ctx.drawTextWithShadow(
         this.textRenderer,
         Text.literal("Slot 2: ")
            .formatted(Formatting.GRAY)
            .append(Text.literal(slot2Name).formatted(TurnBattleClientMod.clientSecondRecruit >= 0 ? Formatting.AQUA : Formatting.DARK_GRAY)),
         panelX + 10,
         y,
         -1
      );
      y += 9 + 8;
      boolean anyUnlocked = false;

      for (Recruit r : Recruit.values()) {
         int ri = r.ordinal();
         if (ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri]) {
            anyUnlocked = true;
            boolean isSlot1 = TurnBattleClientMod.clientActiveRecruit == ri;
            boolean isSlot2 = TurnBattleClientMod.clientSecondRecruit == ri;
            boolean hov = mx >= panelX + 8 && mx < panelX + 8 + cardW && my >= y && my < y + cardH;
            int soulCol = r.soulType != null ? r.soulType.getColor() & 16777215 | -2013265920 : -2002081110;
            ctx.fill(
               panelX + 7, y - 1, panelX + 8 + cardW + 1, y + cardH + 1, !isSlot1 && !isSlot2 ? soulCol : (r.soulType != null ? r.soulType.getColor() : -1)
            );
            ctx.fill(panelX + 8, y, panelX + 8 + cardW, y + cardH, hov ? -15395544 : -16250856);
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(r.displayName + "  Lv." + TurnBattleClientMod.clientRecruitLevels[ri]).formatted(isSlot1 ? Formatting.AQUA : Formatting.WHITE),
               panelX + 14,
               y + 4,
               -1
            );
            String meta = r.playerClass.displayName + (r.soulType != null ? " \u2022 " + r.soulType.getDisplayName() : "");
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(meta).formatted(Formatting.GRAY), panelX + 14, y + 14, -7829368);
            float hp = TurnBattleClientMod.clientRecruitHp[ri];
            float maxH = TurnBattleClientMod.clientRecruitMaxHp[ri];
            if (maxH > 0.0F) {
               int barW = cardW - 4;
               float frac = hp / maxH;
               ctx.fill(panelX + 10, y + 28, panelX + 10 + barW, y + 34, -14540254);
               ctx.fill(panelX + 10, y + 28, panelX + 10 + (int)((float)barW * frac), y + 34, frac > 0.5F ? -12268476 : (frac > 0.25F ? -3364352 : -3399134));
            }

            int b1x = panelX + 8 + cardW - btnW * 2 - 6;
            int b2x = panelX + 8 + cardW - btnW - 2;
            int by = y + cardH - btnH - 3;
            boolean h1 = mx >= b1x && mx < b1x + btnW && my >= by && my < by + btnH;
            boolean h2 = mx >= b2x && mx < b2x + btnW && my >= by && my < by + btnH;
            ctx.fill(b1x, by, b1x + btnW, by + btnH, isSlot1 ? -14531533 : (h1 ? -15650014 : -16115180));
            ctx.fill(b2x, by, b2x + btnW, by + btnH, isSlot2 ? -14531533 : (h2 ? -15650014 : -16115180));
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal(isSlot1 ? "\u2713 Slot 1" : "Slot 1").formatted(isSlot1 ? Formatting.AQUA : Formatting.GRAY),
               b1x + btnW / 2,
               by + 2,
               -1
            );
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal(isSlot2 ? "\u2713 Slot 2" : "Slot 2").formatted(isSlot2 ? Formatting.AQUA : Formatting.GRAY),
               b2x + btnW / 2,
               by + 2,
               -1
            );
            y += cardH + 6;
         }
      }

      int[] customSouls = TurnBattleClientMod.customRecruitSouls;
      String[] customNames = TurnBattleClientMod.customRecruitNames;
      int[] customClasses = TurnBattleClientMod.customRecruitClasses;
      int[] customLevels = TurnBattleClientMod.customRecruitLevels;
      float[] customHp = TurnBattleClientMod.customRecruitHp;
      float[] customMaxHp = TurnBattleClientMod.customRecruitMaxHp;
      if (customNames != null) {
         for (int ci = 0; ci < customNames.length; ci++) {
            int slotCode = 1000 + ci;
            boolean isSlot1 = TurnBattleClientMod.clientActiveRecruit == slotCode;
            boolean isSlot2 = TurnBattleClientMod.clientSecondRecruit == slotCode;
            boolean hov = mx >= panelX + 8 && mx < panelX + 8 + cardW && my >= y && my < y + cardH;
            SoulType st = customSouls[ci] >= 0 && customSouls[ci] < SoulType.values().length ? SoulType.values()[customSouls[ci]] : null;
            int soulCol = st != null ? st.getColor() & 16777215 | -2013265920 : -2002081110;
            ctx.fill(panelX + 7, y - 1, panelX + 8 + cardW + 1, y + cardH + 1, !isSlot1 && !isSlot2 ? soulCol : (st != null ? st.getColor() : -1));
            ctx.fill(panelX + 8, y, panelX + 8 + cardW, y + cardH, hov ? -15395544 : -16250856);
            PlayerClass cls = customClasses[ci] >= 0 && customClasses[ci] < PlayerClass.values().length
               ? PlayerClass.values()[customClasses[ci]]
               : PlayerClass.HUMAN_MAGE;
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(customNames[ci] + " [Custom]  Lv." + customLevels[ci]).formatted(isSlot1 ? Formatting.AQUA : Formatting.WHITE),
               panelX + 14,
               y + 4,
               -1
            );
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(cls.displayName + (st != null ? " \u2022 " + st.getDisplayName() : "")).formatted(Formatting.GRAY),
               panelX + 14,
               y + 14,
               -7829368
            );
            float hp2 = customHp[ci];
            float maxH2 = customMaxHp[ci];
            if (maxH2 > 0.0F) {
               int barW = cardW - 4;
               float frac = hp2 / maxH2;
               ctx.fill(panelX + 10, y + 28, panelX + 10 + barW, y + 34, -14540254);
               ctx.fill(panelX + 10, y + 28, panelX + 10 + (int)((float)barW * frac), y + 34, frac > 0.5F ? -12268476 : (frac > 0.25F ? -3364352 : -3399134));
            }

            int b1x = panelX + 8 + cardW - btnW * 2 - 6;
            int b2x = panelX + 8 + cardW - btnW - 2;
            int by = y + cardH - btnH - 3;
            boolean h1 = mx >= b1x && mx < b1x + btnW && my >= by && my < by + btnH;
            boolean h2 = mx >= b2x && mx < b2x + btnW && my >= by && my < by + btnH;
            ctx.fill(b1x, by, b1x + btnW, by + btnH, isSlot1 ? -14531533 : (h1 ? -15650014 : -16115180));
            ctx.fill(b2x, by, b2x + btnW, by + btnH, isSlot2 ? -14531533 : (h2 ? -15650014 : -16115180));
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal(isSlot1 ? "\u2713 Slot 1" : "Slot 1").formatted(isSlot1 ? Formatting.AQUA : Formatting.GRAY),
               b1x + btnW / 2,
               by + 2,
               -1
            );
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal(isSlot2 ? "\u2713 Slot 2" : "Slot 2").formatted(isSlot2 ? Formatting.AQUA : Formatting.GRAY),
               b2x + btnW / 2,
               by + 2,
               -1
            );
            anyUnlocked = true;
            y += cardH + 6;
         }
      }

      if (!anyUnlocked) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("No recruits yet. Complete questlines or create a Vessel.").formatted(Formatting.DARK_GRAY),
            panelX + this.PANEL_W / 2,
            contentY + this.PANEL_H / 2 - 4,
            -12303275
         );
      }
   }

   private String getRecruitDisplayName(int slotCode) {
      if (slotCode < 1000) {
         return slotCode >= 0 && slotCode < Recruit.values().length ? Recruit.values()[slotCode].displayName : "Empty";
      } else {
         int ci = slotCode - 1000;
         String[] names = TurnBattleClientMod.customRecruitNames;
         return names != null && ci < names.length ? names[ci] + " [Custom]" : "Custom";
      }
   }

   private void renderVesselCreatorTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      int y = contentY + 8;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Vessel Creator").formatted(Formatting.AQUA), panelX + 8, y, -12276993);
      y += 9 + 6;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Create a custom NPC companion").formatted(Formatting.DARK_GRAY), panelX + 10, y, -11184794);
      y += 9 + 12;
      int btnW = 160;
      int btnH = 20;
      int btnX = panelX + (this.PANEL_W - btnW) / 2;
      boolean hov = mx >= btnX && mx < btnX + btnW && my >= y && my < y + btnH;
      ctx.fill(btnX - 1, y - 1, btnX + btnW + 1, y + btnH + 1, hov ? -12276993 : -14531482);
      ctx.fill(btnX, y, btnX + btnW, y + btnH, hov ? -16115149 : -16380134);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("+ Create New Vessel").formatted(hov ? Formatting.AQUA : Formatting.WHITE), btnX + btnW / 2, y + (btnH - 9) / 2, -1
      );
      y += btnH + 8;
      String[] cNames = TurnBattleClientMod.customRecruitNames;
      boolean hasCustom = cNames != null && cNames.length > 0;
      if (hasCustom) {
         y += btnH + 8;
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("Custom Vessels:").formatted(Formatting.DARK_GRAY), panelX + 10, y, -11184794);
         y += 9 + 4;
         int[] cClasses = TurnBattleClientMod.customRecruitClasses;

         for (int ci = 0; ci < cNames.length; ci++) {
            boolean rowHov = mx >= btnX && mx < btnX + btnW && my >= y && my < y + 16;
            ctx.fill(btnX, y, btnX + btnW, y + 16, rowHov ? -14020600 : -15595516);
            ctx.fill(btnX, y, btnX + 3, y + 16, -7846878);
            PlayerClass cls = cClasses != null && ci < cClasses.length && cClasses[ci] >= 0 && cClasses[ci] < PlayerClass.values().length
               ? PlayerClass.values()[cClasses[ci]]
               : null;
            String label = cNames[ci] + (cls != null ? "  \u00a78(" + cls.displayName + ")" : "");
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(cNames[ci]).formatted(Formatting.WHITE), btnX + 6, y + (16 - 9) / 2, -1);
            if (cls != null) {
               ctx.drawTextWithShadow(
                  this.textRenderer,
                  Text.literal("  " + cls.displayName).formatted(Formatting.DARK_GRAY),
                  btnX + 6 + this.textRenderer.getWidth(cNames[ci]),
                  y + (16 - 9) / 2,
                  -10066347
               );
            }

            int xBtnX = btnX + btnW - 16;
            boolean xHov = mx >= xBtnX && mx < xBtnX + 14 && my >= y + 2 && my < y + 14;
            ctx.fill(xBtnX, y + 2, xBtnX + 14, y + 14, xHov ? -7855582 : -12316399);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2715"), xBtnX + 7, y + (16 - 9) / 2, -39322);
            y += 18;
         }
      }
   }

   public void onQuestlineState() {
      String active = TurnBattleClientMod.clientActiveQuestlineId;
      if (active != null && !active.isEmpty() && this.activeTab == 5) {
         this.storySelectedId = active;
         this.storyView = 1;
      }
   }

   private void renderStoryTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      this.hoverQlAccept = false;
      this.hoverQlBack = false;
      this.hoverQlRetry = false;
      if (this.storyView == 0) {
         this.renderQuestlineDiamonds(ctx, panelX, contentY, mx, my);
      } else {
         this.renderQuestlineDetail(ctx, panelX, contentY, mx, my);
      }
   }

   private void renderQuestlineDiamonds(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      int y = contentY + 6;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Chapter 1").formatted(Formatting.GOLD), panelX + 8, y, -8892);
      y += 9 + 14;
      int r = 12;
      int oriCX = panelX + this.PANEL_W / 2;
      int oriCY = y + r;
      int branchY = oriCY + r + 28;
      int averyX = panelX + this.PANEL_W / 2 - 44;
      int hazelX = panelX + this.PANEL_W / 2 + 44;
      Map pos = new LinkedHashMap<>();
      pos.put("ori", new int[]{oriCX, oriCY});
      pos.put("avery", new int[]{averyX, branchY});
      pos.put("hazel", new int[]{hazelX, branchY});
      int[] oriPos = pos.get("ori");
      int trunkMidY = oriCY + r + 14;
      ctx.fill(oriCX - 1, oriCY + r, oriCX + 1, trunkMidY, -12303258);
      ctx.fill(averyX, trunkMidY - 1, hazelX, trunkMidY + 1, -12303258);
      ctx.fill(averyX - 1, trunkMidY, averyX + 1, branchY - r, -12303258);
      ctx.fill(hazelX - 1, trunkMidY, hazelX + 1, branchY - r, -12303258);

      for (QuestlineManager.QuestlineMeta ql : QuestlineManager.ALL_QUESTLINES) {
         int[] p = pos.get(ql.id());
         if (p != null) {
            int cx = p[0];
            int cy = p[1];
            boolean completed = TurnBattleClientMod.clientCompletedQuestlines.contains(ql.id());
            boolean active = ql.id().equals(TurnBattleClientMod.clientActiveQuestlineId);
            boolean prereqMet = this.checkPrereqs(ql.prereqs());
            boolean locked = !prereqMet && !completed && !active;
            int col = completed ? -12268476 : (active ? -12276993 : (locked ? -14013884 : -8947781));

            for (int dy = -r; dy <= r; dy++) {
               int hw = r - Math.abs(dy);
               ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, col);
            }

            boolean hov = mx >= cx - r && mx <= cx + r && my >= cy - r && my <= cy + r;
            if (hov && !locked) {
               for (int dy = -r; dy <= r; dy++) {
                  int hw = r - Math.abs(dy);
                  if (Math.abs(dy) == r || hw == 0) {
                     ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, -1426063361);
                  }
               }
            }

            String label = ql.name();
            int labelCol = locked ? -12303258 : (completed ? -12255420 : (active ? -7811841 : -3355393));
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), cx, cy + r + 4, labelCol);
            if (active) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("In Progress").formatted(Formatting.AQUA), cx, cy + r + 4 + 9 + 2, -12276993);
            } else if (completed) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2713").formatted(Formatting.GREEN), cx, cy + r + 4 + 9 + 2, -12268476);
            }
         }
      }
   }

   private boolean checkPrereqs(String[] prereqs) {
      for (String p : prereqs) {
         if (!TurnBattleClientMod.clientCompletedQuestlines.contains(p)) {
            return false;
         }
      }

      return true;
   }

   private void renderQuestlineDetail(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      QuestlineManager.QuestlineMeta ql = QuestlineManager.ALL_QUESTLINES.stream().filter(q -> q.id().equals(this.storySelectedId)).findFirst().orElse(null);
      if (ql == null) {
         this.storyView = 0;
      } else {
         boolean completed = TurnBattleClientMod.clientCompletedQuestlines.contains(ql.id());
         boolean active = ql.id().equals(TurnBattleClientMod.clientActiveQuestlineId);
         boolean otherActive = !TurnBattleClientMod.clientActiveQuestlineId.isEmpty() && !active;
         boolean levelMet = this.playerLevel >= ql.levelRequired();
         int y = contentY + 8;
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(ql.name()).formatted(Formatting.GOLD), panelX + 8, y, -8892);
         y += 9 + 4;

         for (String line : wrapText(ql.description(), this.PANEL_W - 16)) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(line).formatted(Formatting.GRAY), panelX + 8, y, -7829351);
            y += 9 + 2;
         }

         y += 4;
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Recruits available: " + ql.recruitsAvailable() + "   Quests: " + ql.questCount()).formatted(Formatting.DARK_GRAY),
            panelX + 8,
            y,
            -11184794
         );
         y += 9 + 4;
         if (ql.levelRequired() > 0) {
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal("Requires Lv." + ql.levelRequired()).formatted(levelMet ? Formatting.GREEN : Formatting.RED),
               panelX + 8,
               y,
               levelMet ? -12268476 : -3390396
            );
            y += 9 + 6;
         } else {
            y += 8;
         }

         if (completed) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("\u2713 Questline Complete").formatted(Formatting.GREEN), panelX + this.PANEL_W / 2, y + 4, -12255420
            );
         } else if (active) {
            String nodeDesc = switch (TurnBattleClientMod.clientQuestlineNodeIndex) {
               case 0 -> "Encounter: Defeat Antonio the Ant King.";
               case 1 -> "Battle: Defeat the cultists threatening Ori.";
               default -> "Questline resolved.";
            };
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("Current: " + nodeDesc).formatted(Formatting.AQUA), panelX + 8, y, -7820545);
            y += 9 + 10;
            int btnW = 90;
            int btnH = 14;
            int btnX = panelX + this.PANEL_W / 2 - btnW / 2;
            this.hoverQlRetry = mx >= btnX && mx < btnX + btnW && my >= y && my < y + btnH;
            ctx.fill(btnX - 1, y - 1, btnX + btnW + 1, y + btnH + 1, this.hoverQlRetry ? -12277180 : -14527198);
            ctx.fill(btnX, y, btnX + btnW, y + btnH, -16445435);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u25b6 Continue").formatted(Formatting.WHITE), btnX + btnW / 2, y + 3, -10027162);
         } else if (otherActive) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("Finish your current questline first.").formatted(Formatting.DARK_GRAY),
               panelX + this.PANEL_W / 2,
               y + 4,
               -11184794
            );
         } else if (!levelMet) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("Requires Lv." + ql.levelRequired() + " to begin.").formatted(Formatting.RED),
               panelX + this.PANEL_W / 2,
               y + 4,
               -3390396
            );
         } else {
            int btnW = 90;
            int btnH = 14;
            int btnX = panelX + this.PANEL_W / 2 - btnW / 2;
            this.hoverQlAccept = mx >= btnX && mx < btnX + btnW && my >= y && my < y + btnH;
            ctx.fill(btnX - 1, y - 1, btnX + btnW + 1, y + btnH + 1, this.hoverQlAccept ? -12294401 : -14540152);
            ctx.fill(btnX, y, btnX + btnW, y + btnH, -16448224);
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("Accept Questline").formatted(this.hoverQlAccept ? Formatting.WHITE : Formatting.GRAY),
               btnX + btnW / 2,
               y + 3,
               -7824897
            );
         }

         int backY = contentY + this.PANEL_H - 20;
         this.hoverQlBack = mx >= panelX + 8 && mx < panelX + 50 && my >= backY && my < backY + 12;
         ctx.fill(panelX + 7, backY - 1, panelX + 51, backY + 13, this.hoverQlBack ? -11184794 : -14540237);
         ctx.fill(panelX + 8, backY, panelX + 50, backY + 12, -16250856);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2190 Back").formatted(Formatting.GRAY), panelX + 29, backY + 2, -7829351);
      }
   }

   public void onRecruitInfo() {
   }

   private static List<String> wrapText(String text, int maxW) {
      List lines = new ArrayList<>();
      if (text != null && !text.isEmpty()) {
         TextRenderer tr = MinecraftClient.getInstance().textRenderer;
         StringBuilder cur = new StringBuilder();

         for (String w : text.split(" ", -1)) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (tr.getWidth(test) > maxW && !cur.isEmpty()) {
               lines.add(cur.toString());
               cur = new StringBuilder(w);
            } else {
               cur = new StringBuilder(test);
            }
         }

         if (!cur.isEmpty()) {
            lines.add(cur.toString());
         }

         return lines;
      } else {
         return lines;
      }
   }

   private void renderEquipTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      this.equipBtns.clear();
      this.rollBtns.clear();
      this.accBtns.clear();
      this.armorBtns.clear();
      this.armorRollBtns.clear();
      if (this.equipDataLoaded && this.accDataLoaded && this.armorDataLoaded) {
         Recruit[] recruits = Recruit.values();
         int accTabIdx = 1 + recruits.length;
         int armorTabIdx = accTabIdx + 1;
         if (this.equipSubTab > armorTabIdx) {
            this.equipSubTab = 0;
         }

         int accTabWidth = 80;
         int tabH = 14;
         int armorTabWidth = 60;
         int armorBtnX = panelX + this.PANEL_W - armorTabWidth - 6;
         int armorBtnY = contentY + 4;
         boolean armorActive = this.equipSubTab == armorTabIdx;
         boolean armorHov = mx >= armorBtnX && mx < armorBtnX + armorTabWidth && my >= armorBtnY && my < armorBtnY + tabH;
         ctx.fill(armorBtnX, armorBtnY, armorBtnX + armorTabWidth, armorBtnY + tabH, armorActive ? -14531516 : (armorHov ? -15060173 : -15918566));
         ctx.fill(armorBtnX, armorBtnY + tabH - 2, armorBtnX + armorTabWidth, armorBtnY + tabH, armorActive ? -12255284 : -14522812);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Armor").formatted(armorActive ? Formatting.WHITE : Formatting.GRAY),
            armorBtnX + armorTabWidth / 2,
            armorBtnY + 2,
            armorActive ? -1 : -7829368
         );
         int accBtnX = armorBtnX - accTabWidth - 4;
         int accBtnY = contentY + 4;
         boolean accActive = this.equipSubTab == accTabIdx;
         boolean accHov = mx >= accBtnX && mx < accBtnX + accTabWidth && my >= accBtnY && my < accBtnY + tabH;
         ctx.fill(accBtnX, accBtnY, accBtnX + accTabWidth, accBtnY + tabH, accActive ? -10074983 : (accHov ? -11193464 : -14544589));
         ctx.fill(accBtnX, accBtnY + tabH - 2, accBtnX + accTabWidth, accBtnY + tabH, accActive ? -3372801 : -12311962);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Accessories").formatted(accActive ? Formatting.WHITE : Formatting.GRAY),
            accBtnX + accTabWidth / 2,
            accBtnY + 2,
            accActive ? -1 : -7829368
         );
         if (armorActive) {
            int x2 = panelX + 8;
            int y2 = contentY + 26;
            int cardW2 = this.PANEL_W - 16;
            int backW = 44;
            int backH = 14;
            int backX = panelX + 8;
            int backY = contentY + 6;
            boolean backHov = mx >= backX && mx < backX + backW && my >= backY && my < backY + backH;
            ctx.fill(backX, backY, backX + backW, backY + backH, backHov ? -13417370 : -15066573);
            ctx.fill(backX, backY + backH - 2, backX + backW, backY + backH, backHov ? -7829249 : -13417336);
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("\u25c0 Back").formatted(backHov ? Formatting.WHITE : Formatting.GRAY),
               backX + backW / 2,
               backY + 3,
               backHov ? -1 : -7829334
            );
            this.renderArmorContent(ctx, panelX, x2, y2, cardW2, contentY, mx, my);
         } else if (accActive) {
            int x2 = panelX + 8;
            int y2 = contentY + 26;
            int cardW2 = this.PANEL_W - 16;
            int backW = 44;
            int backH = 14;
            int backX = panelX + 8;
            int backY = contentY + 6;
            boolean backHov = mx >= backX && mx < backX + backW && my >= backY && my < backY + backH;
            ctx.fill(backX, backY, backX + backW, backY + backH, backHov ? -13417370 : -15066573);
            ctx.fill(backX, backY + backH - 2, backX + backW, backY + backH, backHov ? -7829249 : -13417336);
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("\u25c0 Back").formatted(backHov ? Formatting.WHITE : Formatting.GRAY),
               backX + backW / 2,
               backY + 3,
               backHov ? -1 : -7829334
            );
            this.renderAccessoriesContent(ctx, panelX, x2, y2, cardW2, contentY, mx, my);
         } else {
            int navW = 64;
            int navX = panelX + 4;
            int navY = contentY + 4;
            int navH = this.PANEL_H - 8;
            int rowH = 20;
            ctx.fill(navX, navY, navX + navW, navY + navH, -16119270);
            ctx.fill(navX, navY, navX + 1, navY + navH, -13421739);
            ctx.fill(navX + navW - 1, navY, navX + navW, navY + navH, -13421739);
            int rowY = navY + 4;
            boolean sel = this.equipSelectedTarget == 0;
            boolean hov = mx >= navX + 2 && mx < navX + navW - 2 && my >= rowY && my < rowY + rowH;
            ctx.fill(navX + 2, rowY, navX + navW - 2, rowY + rowH, sel ? -13417336 : (hov ? -15066573 : -15921886));
            if (sel) {
               ctx.fill(navX + 2, rowY, navX + 4, rowY + rowH, -7829249);
            }

            String playerDisplayName = !TurnBattleClientMod.clientVesselName.isEmpty() ? TurnBattleClientMod.clientVesselName : "Player";
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal(playerDisplayName).formatted(sel ? Formatting.WHITE : Formatting.GRAY),
               navX + navW / 2,
               rowY + (rowH - 9) / 2,
               sel ? -1 : -7829334
            );
            rowY += rowH + 2;

            for (int ri = 0; ri < recruits.length; ri++) {
               hov = ri < TurnBattleClientMod.clientRecruitUnlocked.length && TurnBattleClientMod.clientRecruitUnlocked[ri];
               if (hov) {
                  boolean selx = this.equipSelectedTarget == ri + 1;
                  boolean hovx = mx >= navX + 2 && mx < navX + navW - 2 && my >= rowY && my < rowY + rowH;
                  int bg = selx ? -15060190 : (hovx ? -15918575 : -16249590);
                  ctx.fill(navX + 2, rowY, navX + navW - 2, rowY + rowH, bg);
                  if (selx) {
                     ctx.fill(navX + 2, rowY, navX + 4, rowY + rowH, -12268442);
                  }

                  ctx.drawCenteredTextWithShadow(
                     this.textRenderer,
                     Text.literal(recruits[ri].displayName).formatted(selx ? Formatting.WHITE : Formatting.GRAY),
                     navX + navW / 2,
                     rowY + (rowH - 9) / 2,
                     selx ? -1 : -7820664
                  );
                  rowY += rowH + 2;
               }
            }

            String[] cNames = TurnBattleClientMod.customRecruitNames;
            hov = (boolean)(recruits.length + 1);
            if (cNames != null) {
               for (int ci = 0; ci < cNames.length; ci++) {
                  int tgt = hov + ci;
                  boolean selx = this.equipSelectedTarget == tgt;
                  boolean hovx = mx >= navX + 2 && mx < navX + navW - 2 && my >= rowY && my < rowY + rowH;
                  ctx.fill(navX + 2, rowY, navX + navW - 2, rowY + rowH, selx ? -15064525 : (hovx ? -15920862 : -16249574));
                  if (selx) {
                     ctx.fill(navX + 2, rowY, navX + 4, rowY + rowH, -12276993);
                  }

                  ctx.drawCenteredTextWithShadow(
                     this.textRenderer,
                     Text.literal(cNames[ci]).formatted(selx ? Formatting.AQUA : Formatting.GRAY),
                     navX + navW / 2,
                     rowY + (rowH - 9) / 2,
                     selx ? -7807489 : -10057558
                  );
                  rowY += rowH + 2;
               }
            }

            int panX = navX + navW + 4;
            int panW = this.PANEL_W - navW - 12;
            int y = contentY + 22;
            int currentRecruitOrd = this.equipSelectedTarget - 1;
            int customBasePanel = recruits.length + 1;
            String targetName;
            ItemStack currentEquipped;
            if (this.equipSelectedTarget == 0) {
               currentEquipped = this.equippedWeapon;
               targetName = "Player Weapon";
            } else if (this.equipSelectedTarget >= customBasePanel) {
               int ci = this.equipSelectedTarget - customBasePanel;
               currentRecruitOrd = 1000 + ci;
               String[] cNamesP = TurnBattleClientMod.customRecruitNames;
               targetName = (cNamesP != null && ci < cNamesP.length ? cNamesP[ci] : "Custom") + "'s Weapon";
               ItemStack[] cWeapons = TurnBattleClientMod.customRecruitWeapons;
               currentEquipped = cWeapons != null && ci < cWeapons.length && cWeapons[ci] != null ? cWeapons[ci] : ItemStack.EMPTY;
            } else {
               currentEquipped = TurnBattleClientMod.clientRecruitWeapons != null && currentRecruitOrd < TurnBattleClientMod.clientRecruitWeapons.length
                  ? TurnBattleClientMod.clientRecruitWeapons[currentRecruitOrd]
                  : ItemStack.EMPTY;
               targetName = recruits[currentRecruitOrd].displayName + "'s Weapon";
            }

            ctx.drawTextWithShadow(this.textRenderer, Text.literal(targetName).formatted(Formatting.YELLOW), panX, y, -8892);
            y += 11;
            boolean hasEquipped = currentEquipped != null && !currentEquipped.isEmpty();
            if (hasEquipped) {
               WeaponStats ws = WeaponStats.fromStack(currentEquipped);
               ctx.fill(panX - 2, y - 2, panX + panW + 2, y + 48 + 2, -7829249);
               ctx.fill(panX, y, panX + panW, y + 48, ws != null && ws.isQuirk ? -15068672 : -15921886);
               ctx.fill(panX, y, panX + 3, y + 48, -7829249);
               this.renderWeaponCard(ctx, panX + 7, y + 4, panW - 10, currentEquipped, ws, mx, my);
               int btnX = panX + panW - 46 - 4;
               int btnY = y + 48 - 12 - 4;
               boolean hovx = mx >= btnX && mx < btnX + 46 && my >= btnY && my < btnY + 12;
               ctx.fill(btnX, btnY, btnX + 46, btnY + 12, hovx ? -11202287 : -13432310);
               ctx.fill(btnX + 1, btnY + 1, btnX + 46 - 1, btnY + 12 - 1, hovx ? -8974063 : -12317170);
               ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Unequip").formatted(Formatting.RED), btnX + 23, btnY + 2, -48060);
               this.equipBtns.add(new int[]{btnX, btnY, 46, 12, this.equipSelectedTarget == 0 ? -1 : -100 - currentRecruitOrd});
            } else {
               ctx.fill(panX, y, panX + panW, y + 48, -16119270);
               ctx.fill(panX, y, panX + 3, y + 48, -13421739);
               ctx.drawCenteredTextWithShadow(
                  this.textRenderer, Text.literal("\u2014 No weapon equipped \u2014").formatted(Formatting.DARK_GRAY), panX + panW / 2, y + 24 - 4, -10066296
               );
            }

            y += 56;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("Inventory Weapons").formatted(Formatting.GRAY), panX, y, -7829351);
            y += 11;
            int listTop = y;
            int listH = contentY + this.PANEL_H - y - 4;
            if (this.invEquipStacks.length == 0) {
               ctx.drawCenteredTextWithShadow(
                  this.textRenderer,
                  Text.literal("Craft a weapon to see it here!").formatted(Formatting.DARK_GRAY),
                  panX + panW / 2,
                  y + listH / 2 - 4,
                  -11184794
               );
            } else {
               this.scissor(ctx, panX, y, panX + panW, y + listH);
               int drawY = y - this.equipScrollOffset;

               for (int i = 0; i < this.invEquipStacks.length; i++) {
                  ItemStack s = this.invEquipStacks[i];
                  WeaponStats ws = WeaponStats.fromStack(s);
                  boolean unrolled = ws == null;
                  if (drawY + 48 > listTop && drawY < listTop + listH) {
                     ctx.fill(panX, drawY, panX + panW, drawY + 48 - 2, !unrolled && ws.isQuirk ? -15528704 : (unrolled ? -16119278 : -15921894));
                     ctx.fill(panX, drawY, panX + 3, drawY + 48 - 2, unrolled ? -14531533 : -12298906);
                     boolean cardHov = mx >= panX && mx < panX + panW && my >= drawY && my < drawY + 48 - 2;
                     if (cardHov) {
                        ctx.fill(panX, drawY, panX + panW, drawY + 48 - 2, 587202559);
                     }

                     this.renderWeaponCard(ctx, panX + 7, drawY + 4, panW - 10, s, ws, mx, my);
                     int btnX2 = panX + panW - 46 - 4;
                     int btnY2 = drawY + 48 - 12 - 6;
                     boolean hov2 = mx >= btnX2 && mx < btnX2 + 46 && my >= btnY2 && my < btnY2 + 12;
                     if (unrolled) {
                        ctx.fill(btnX2, btnY2, btnX2 + 46, btnY2 + 12, hov2 ? -16108749 : -16377318);
                        ctx.fill(btnX2 + 1, btnY2 + 1, btnX2 + 46 - 1, btnY2 + 12 - 1, hov2 ? -15641259 : -16111062);
                        ctx.drawCenteredTextWithShadow(
                           this.textRenderer, Text.literal("Roll Stats").formatted(Formatting.AQUA), btnX2 + 23, btnY2 + 2, -12255250
                        );
                        this.rollBtns.add(new int[]{btnX2, btnY2, 46, 12, this.invEquipSlots[i]});
                     } else {
                        ctx.fill(btnX2, btnY2, btnX2 + 46, btnY2 + 12, hov2 ? -15645679 : -16111094);
                        ctx.fill(btnX2 + 1, btnY2 + 1, btnX2 + 46 - 1, btnY2 + 12 - 1, hov2 ? -14522846 : -15844850);
                        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Equip").formatted(Formatting.GREEN), btnX2 + 23, btnY2 + 2, -12255420);
                        this.equipBtns
                           .add(
                              new int[]{
                                 btnX2,
                                 btnY2,
                                 46,
                                 12,
                                 this.equipSelectedTarget == 0 ? this.invEquipSlots[i] : 10000 + currentRecruitOrd * 1000 + this.invEquipSlots[i]
                              }
                           );
                     }
                  }

                  drawY += 48;
               }

               ctx.disableScissor();
            }
         }
      } else {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("Loading...").formatted(Formatting.GRAY), panelX + this.PANEL_W / 2, contentY + this.PANEL_H / 2 - 5, -1
         );
      }
   }

   private Identifier resolveTargetFaceTexture(int targetSlot, String targetName) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (targetSlot == -1) {
         if (mc.player == null) {
            return null;
         } else {
            Identifier tex = null;
            if (mc.getNetworkHandler() != null) {
               PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
               if (entry != null) {
                  tex = entry.getSkinTextures().texture();
               }
            }

            return tex != null ? tex : mc.player.getSkinTextures().texture();
         }
      } else {
         return RecruitSkinManager.getSkin(targetName);
      }
   }

   private List<Integer> buildAccTargetList() {
      List targets = new ArrayList<>();
      targets.add(-1);
      Recruit[] recs = Recruit.values();
      boolean[] unlockedArr = TurnBattleClientMod.clientRecruitUnlocked;

      for (int i = 0; i < recs.length; i++) {
         if (unlockedArr != null && i < unlockedArr.length && unlockedArr[i]) {
            targets.add(i);
         }
      }

      String[] customNames = TurnBattleClientMod.customRecruitNames;
      if (customNames != null) {
         for (int ci = 0; ci < customNames.length; ci++) {
            targets.add(1000 + ci);
         }
      }

      return targets;
   }

   private void renderAccessoriesContent(DrawContext ctx, int panelX, int x, int y, int cardW, int contentY, int mx, int my) {
      List accTargets = this.buildAccTargetList();
      List accTargetNames = new ArrayList<>();
      Recruit[] accRecruits = Recruit.values();

      for (int t : accTargets) {
         if (t == -1) {
            accTargetNames.add(!TurnBattleClientMod.clientVesselName.isEmpty() ? TurnBattleClientMod.clientVesselName : "Player");
         } else if (t < 1000) {
            accTargetNames.add(t < accRecruits.length ? accRecruits[t].displayName : "Recruit");
         } else {
            int ci = t - 1000;
            String[] cn = TurnBattleClientMod.customRecruitNames;
            accTargetNames.add(cn != null && ci < cn.length ? cn[ci] : "Custom");
         }
      }

      int curTargetIdx = accTargets.indexOf(this.accTargetSlot);
      if (curTargetIdx < 0) {
         curTargetIdx = 0;
         this.accTargetSlot = -1;
      }

      int selRowH = 20;
      int arrowW = 16;
      boolean leftHov = mx >= x && mx < x + arrowW && my >= y && my < y + selRowH;
      boolean rightHov = mx >= x + cardW - arrowW && mx < x + cardW && my >= y && my < y + selRowH;
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("\u25c0").formatted(accTargets.size() > 1 ? Formatting.WHITE : Formatting.DARK_GRAY),
         x + arrowW / 2,
         y + selRowH / 2 - 4,
         leftHov ? -188 : -5592406
      );
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("\u25b6").formatted(accTargets.size() > 1 ? Formatting.WHITE : Formatting.DARK_GRAY),
         x + cardW - arrowW / 2,
         y + selRowH / 2 - 4,
         rightHov ? -188 : -5592406
      );
      String accTargetName = accTargetNames.get(curTargetIdx);
      Identifier faceTex = this.resolveTargetFaceTexture(this.accTargetSlot, accTargetName);
      int FACE_SZ = 16;
      int nameW = this.textRenderer.getWidth(accTargetName);
      int groupW = (faceTex != null ? 21 : 0) + nameW;
      int groupX = x + cardW / 2 - groupW / 2;
      int faceY = y + (selRowH - 16) / 2;
      if (faceTex != null) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         ctx.drawTexture(faceTex, groupX, faceY, 16, 16, 8.0F, 8.0F, 8, 8, 64, 64);
         ctx.drawTexture(faceTex, groupX, faceY, 16, 16, 40.0F, 8.0F, 8, 8, 64, 64);
         groupX += 21;
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal(accTargetName).formatted(Formatting.AQUA), groupX, y + (selRowH - 9) / 2, -10035713);
      this.accBtns.add(new int[]{x, y, arrowW, selRowH, -9001});
      this.accBtns.add(new int[]{x + cardW - arrowW, y, arrowW, selRowH, -9002});
      y += selRowH + 4;
      ItemStack targetAccessory;
      if (this.accTargetSlot == -1) {
         targetAccessory = this.equippedAccessory;
      } else if (this.accTargetSlot >= 1000) {
         int ci = this.accTargetSlot - 1000;
         ItemStack[] arr = TurnBattleClientMod.customRecruitAccessories;
         targetAccessory = arr != null && ci < arr.length ? arr[ci] : ItemStack.EMPTY;
      } else {
         ItemStack[] arr = TurnBattleClientMod.clientRecruitAccessories;
         targetAccessory = arr != null && this.accTargetSlot < arr.length ? arr[this.accTargetSlot] : ItemStack.EMPTY;
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Equipped Accessory").formatted(Formatting.YELLOW), x, y, -8892);
      y += 11;
      boolean hasEquipped = targetAccessory != null && !targetAccessory.isEmpty();
      if (hasEquipped) {
         ctx.fill(x - 2, y - 2, x + cardW + 2, y + 56 + 2, -3372801);
         ctx.fill(x, y, x + cardW, y + 56, -15925216);
         ctx.fill(x, y, x + 3, y + 56, -3372801);
         this.renderAccessoryCard(ctx, x + 7, y + 4, cardW - 10, targetAccessory, mx, my);
         FACE_SZ = x + cardW - 46 - 4;
         nameW = y + 56 - 12 - 4;
         boolean hov = mx >= FACE_SZ && mx < FACE_SZ + 46 && my >= nameW && my < nameW + 12;
         ctx.fill(FACE_SZ, nameW, FACE_SZ + 46, nameW + 12, hov ? -11202287 : -13432310);
         ctx.fill(FACE_SZ + 1, nameW + 1, FACE_SZ + 46 - 1, nameW + 12 - 1, hov ? -8974063 : -12317170);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Unequip").formatted(Formatting.RED), FACE_SZ + 23, nameW + 2, -48060);
         this.accBtns.add(new int[]{FACE_SZ, nameW, 46, 12, -1});
      } else {
         ctx.fill(x, y, x + cardW, y + 56, -16119270);
         ctx.fill(x, y, x + 3, y + 56, -12311979);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("\u2014 No accessory equipped \u2014").formatted(Formatting.DARK_GRAY), x + cardW / 2, y + 28 - 4, -10075017
         );
      }

      y += 64;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Inventory").formatted(Formatting.GRAY), x, y, -7829351);
      y += 11;
      FACE_SZ = y;
      nameW = contentY + this.PANEL_H - y - 4;
      this.accListH = nameW;
      if (this.invAccStacks.length == 0) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Shield, Feather, Torch, or Thorn Ring").formatted(Formatting.DARK_GRAY),
            panelX + this.PANEL_W / 2,
            y + nameW / 2 - 4,
            -11184794
         );
      } else {
         this.scissor(ctx, panelX + 4, y, panelX + this.PANEL_W - 4, y + nameW);
         groupW = y - this.accScrollOffset;

         for (int i = 0; i < this.invAccStacks.length; i++) {
            ItemStack s = this.invAccStacks[i];
            if (groupW + 56 > FACE_SZ && groupW < FACE_SZ + nameW) {
               ctx.fill(x, groupW, x + cardW, groupW + 56 - 2, -15925222);
               ctx.fill(x, groupW, x + 3, groupW + 56 - 2, -11193498);
               boolean cardHov = mx >= x && mx < x + cardW && my >= groupW && my < groupW + 56 - 2;
               if (cardHov) {
                  ctx.fill(x, groupW, x + cardW, groupW + 56 - 2, 587202559);
               }

               this.renderAccessoryCard(ctx, x + 7, groupW + 4, cardW - 10, s, mx, my);
               int btnX = x + cardW - 46 - 4;
               int btnY2 = groupW + 56 - 12 - 6;
               boolean hov2 = mx >= btnX && mx < btnX + 46 && my >= btnY2 && my < btnY2 + 12;
               ctx.fill(btnX, btnY2, btnX + 46, btnY2 + 12, hov2 ? -14531567 : -16111094);
               ctx.fill(btnX + 1, btnY2 + 1, btnX + 46 - 1, btnY2 + 12 - 1, hov2 ? -13408734 : -15844850);
               ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Equip").formatted(Formatting.GREEN), btnX + 23, btnY2 + 2, -12255420);
               this.accBtns.add(new int[]{btnX, btnY2, 46, 12, this.invAccSlots[i]});
            }

            groupW += 56;
         }

         ctx.disableScissor();
      }
   }

   private void renderArmorContent(DrawContext ctx, int panelX, int x, int y, int cardW, int contentY, int mx, int my) {
      List armTargets = this.buildAccTargetList();
      Recruit[] armRecruits = Recruit.values();
      int curArmTargetIdx = armTargets.indexOf(this.armTargetSlot);
      if (curArmTargetIdx < 0) {
         boolean var40 = false;
         this.armTargetSlot = -1;
      }

      String armTargetName;
      if (this.armTargetSlot == -1) {
         armTargetName = !TurnBattleClientMod.clientVesselName.isEmpty() ? TurnBattleClientMod.clientVesselName : "Player";
      } else if (this.armTargetSlot >= 1000) {
         int ci = this.armTargetSlot - 1000;
         String[] cn = TurnBattleClientMod.customRecruitNames;
         armTargetName = cn != null && ci < cn.length ? cn[ci] : "Custom";
      } else {
         armTargetName = this.armTargetSlot < armRecruits.length ? armRecruits[this.armTargetSlot].displayName : "Recruit";
      }

      int selRowH = 20;
      int arrowW = 16;
      boolean leftHov = mx >= x && mx < x + arrowW && my >= y && my < y + selRowH;
      boolean rightHov = mx >= x + cardW - arrowW && mx < x + cardW && my >= y && my < y + selRowH;
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("\u25c0").formatted(armTargets.size() > 1 ? Formatting.WHITE : Formatting.DARK_GRAY),
         x + arrowW / 2,
         y + selRowH / 2 - 4,
         leftHov ? -188 : -5592406
      );
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("\u25b6").formatted(armTargets.size() > 1 ? Formatting.WHITE : Formatting.DARK_GRAY),
         x + cardW - arrowW / 2,
         y + selRowH / 2 - 4,
         rightHov ? -188 : -5592406
      );
      Identifier faceTex = this.resolveTargetFaceTexture(this.armTargetSlot, armTargetName);
      int FACE_SZ = 16;
      int nameW = this.textRenderer.getWidth(armTargetName);
      int groupW = (faceTex != null ? 21 : 0) + nameW;
      int groupX = x + cardW / 2 - groupW / 2;
      int faceY = y + (selRowH - 16) / 2;
      if (faceTex != null) {
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         ctx.drawTexture(faceTex, groupX, faceY, 16, 16, 8.0F, 8.0F, 8, 8, 64, 64);
         ctx.drawTexture(faceTex, groupX, faceY, 16, 16, 40.0F, 8.0F, 8, 8, 64, 64);
         groupX += 21;
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal(armTargetName).formatted(Formatting.AQUA), groupX, y + (selRowH - 9) / 2, -10035713);
      this.armorBtns.add(new int[]{x, y, arrowW, selRowH, -9001});
      this.armorBtns.add(new int[]{x + cardW - arrowW, y, arrowW, selRowH, -9002});
      y += selRowH + 4;
      ItemStack[] targetArmor;
      if (this.armTargetSlot == -1) {
         targetArmor = this.equippedArmor;
      } else if (this.armTargetSlot >= 1000) {
         FACE_SZ = this.armTargetSlot - 1000;
         ItemStack[][] arr = TurnBattleClientMod.customRecruitArmor;
         targetArmor = arr != null && FACE_SZ < arr.length ? arr[FACE_SZ] : new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      } else {
         ItemStack[][] arr = TurnBattleClientMod.clientRecruitArmor;
         targetArmor = arr != null && this.armTargetSlot < arr.length ? arr[this.armTargetSlot] : new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
      }

      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Equipped Armor").formatted(Formatting.YELLOW), x, y, -13244);
      y += 11;
      FACE_SZ = 6;
      nameW = (cardW - FACE_SZ) / 2;

      for (int slot = 0; slot < 2; slot++) {
         groupX = x + slot * (nameW + FACE_SZ);
         ItemStack piece = targetArmor[slot];
         boolean hasPiece = piece != null && !piece.isEmpty();
         ctx.fill(groupX - 2, y - 2, groupX + nameW + 2, y + 44 + 2, hasPiece ? -12255284 : -14531533);
         ctx.fill(groupX, y, groupX + nameW, y + 44, hasPiece ? -16311788 : -16119278);
         ctx.fill(groupX, y, groupX + 3, y + 44, hasPiece ? -12255284 : -14531533);
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("Slot " + (slot + 1)).formatted(Formatting.DARK_GRAY), groupX + 6, y + 4, -12294571);
         if (hasPiece) {
            this.renderArmorCard(ctx, groupX + 7, y + 4, nameW - 10, piece, ArmorStats.fromStack(piece), mx, my);
            int btnX = groupX + nameW - 52 - 4;
            int btnY = y + 44 - 12 - 4;
            boolean hov = mx >= btnX && mx < btnX + 52 && my >= btnY && my < btnY + 12;
            ctx.fill(btnX, btnY, btnX + 52, btnY + 12, hov ? -11202287 : -13432310);
            ctx.fill(btnX + 1, btnY + 1, btnX + 52 - 1, btnY + 12 - 1, hov ? -8974063 : -12317170);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Unequip").formatted(Formatting.RED), btnX + 26, btnY + 2, -48060);
            this.armorBtns.add(new int[]{btnX, btnY, 52, 12, -(slot + 1)});
         } else {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("\u2014 Empty \u2014").formatted(Formatting.DARK_GRAY), groupX + nameW / 2, y + 22 - 4, -12294571
            );
         }
      }

      y += 48;
      y += 4;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Inventory Armor").formatted(Formatting.GRAY), x, y, -7829351);
      y += 11;
      groupW = y;
      groupX = contentY + this.PANEL_H - y - 4;
      if (this.invArmorStacks.length == 0) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("No armor in inventory").formatted(Formatting.DARK_GRAY), x + cardW / 2, y + groupX / 2 - 4, -12294571
         );
      } else {
         this.scissor(ctx, panelX + 4, y, panelX + this.PANEL_W - 4, y + groupX);
         faceY = y - this.armorScrollOffset;

         for (int i = 0; i < this.invArmorStacks.length; i++) {
            ItemStack s = this.invArmorStacks[i];
            ArmorStats as = ArmorStats.fromStack(s);
            boolean unrolled = as == null;
            if (faceY + 44 > groupW && faceY < groupW + groupX) {
               ctx.fill(x, faceY, x + cardW, faceY + 44 - 2, unrolled ? -16250864 : -15921894);
               ctx.fill(x, faceY, x + 3, faceY + 44 - 2, unrolled ? -15060190 : -13408700);
               boolean cardHov = mx >= x && mx < x + cardW && my >= faceY && my < faceY + 44 - 2;
               if (cardHov) {
                  ctx.fill(x, faceY, x + cardW, faceY + 44 - 2, 587202559);
               }

               this.renderArmorCard(ctx, x + 7, faceY + 4, cardW - 10, s, as, mx, my);
               int btnX2 = x + cardW - 52 - 4;
               int btnY2 = faceY + 44 - 12 - 6;
               boolean hov2 = mx >= btnX2 && mx < btnX2 + 52 && my >= btnY2 && my < btnY2 + 12;
               if (unrolled) {
                  ctx.fill(btnX2, btnY2, btnX2 + 52, btnY2 + 12, hov2 ? -16108749 : -16377318);
                  ctx.fill(btnX2 + 1, btnY2 + 1, btnX2 + 52 - 1, btnY2 + 12 - 1, hov2 ? -15641259 : -16111062);
                  ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Roll Stats").formatted(Formatting.AQUA), btnX2 + 26, btnY2 + 2, -12255250);
                  this.armorRollBtns.add(new int[]{btnX2, btnY2, 52, 12, this.invArmorSlots[i]});
               } else {
                  int halfW = 25;

                  for (int slotx = 0; slotx < 2; slotx++) {
                     int sbx = btnX2 + slotx * (halfW + 2);
                     boolean shov = mx >= sbx && mx < sbx + halfW && my >= btnY2 && my < btnY2 + 12;
                     ctx.fill(sbx, btnY2, sbx + halfW, btnY2 + 12, shov ? -15645679 : -16111094);
                     ctx.fill(sbx + 1, btnY2 + 1, sbx + halfW - 1, btnY2 + 12 - 1, shov ? -14522846 : -15844850);
                     ctx.drawCenteredTextWithShadow(
                        this.textRenderer, Text.literal("S" + (slotx + 1)).formatted(Formatting.GREEN), sbx + halfW / 2, btnY2 + 2, -12255420
                     );
                     this.armorBtns.add(new int[]{sbx, btnY2, halfW, 12, this.invArmorSlots[i] * 10 + slotx});
                  }
               }
            }

            faceY += 44;
         }

         ctx.disableScissor();
      }
   }

   private void renderArmorCard(DrawContext ctx, int x, int y, int maxW, ItemStack stack, ArmorStats as, int mx, int my) {
      ArmorStats.Tier tier = ArmorStats.tierOf(stack);
      int var10000;
      if (tier == null) {
         var10000 = -7829368;
      } else {
         switch (tier) {
            case LEATHER:
               var10000 = -7838157;
               break;
            case CHAINMAIL:
               var10000 = -5592406;
               break;
            case IRON:
               var10000 = -3355444;
               break;
            case DIAMOND:
               var10000 = -12259585;
               break;
            case NETHERITE:
               var10000 = -3390209;
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      int tierColor = var10000;
      String name = stack.getName().getString();
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(name).formatted(Formatting.WHITE), x, y, -1);
      if (tier != null) {
         String reductStr = tier.reductionPercent + "% DR";
         ctx.drawTextWithShadow(this.textRenderer, Text.literal(reductStr), x + maxW - this.textRenderer.getWidth(reductStr) - 4, y, tierColor);
      }

      y += 10;
      if (as == null) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("Stats not rolled yet").formatted(Formatting.DARK_GRAY), x, y, -12277112);
      } else {
         ctx.drawTextWithShadow(
            this.textRenderer, Text.literal("+" + as.bonusAmt + " " + ArmorStats.STAT_NAMES[as.bonusStat]).formatted(Formatting.AQUA), x, y, -7807489
         );
      }
   }

   private static String accessorySkillDesc(ItemStack stack) {
      OffhandGimmick g = OffhandGimmick.fromStack(stack);

      return switch (g) {
         case SHIELD -> "Create a ring-shaped impact to destroy bullets around you";
         case FEATHER -> "Dash through bullets";
         case TORCH -> "Has a 25% chance to burn enemies with Bash";
         case THORN_RING -> "Wearer takes damage from pain. Reduces the TP cost of all spells.";
         case PSI_PROCESSOR -> "Wearer's TP is drained constantly. Doubles PSI Damage";
         case HEART_LOCKET -> "Heart-shaped locket inscribed with the Delta Rune. Whatever damage reaches its wearer in battle is cut in half.";
         case SOUL_RESONATOR -> "A Soul Container, converted into a new machine. Grazing converts TP into HP instead of adding to the pool. Those who've unlocked the full potential of their SOUL use HP for skills instead of TP.";
         default -> "";
      };
   }

   private void renderAccessoryCard(DrawContext ctx, int x, int y, int maxW, ItemStack stack, int mx, int my) {
      String name = stack.getName().getString();
      ctx.drawTextWithShadow(this.textRenderer, Text.literal(name).formatted(Formatting.WHITE), x, y, -1);
      y += 11;
      String desc = accessorySkillDesc(stack);
      if (!desc.isEmpty()) {
         for (String line : wrapText(desc, maxW - 52)) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(line).formatted(Formatting.LIGHT_PURPLE), x, y, -2258689);
            y += 9;
         }
      }
   }

   private void renderItemsTab(DrawContext ctx, int panelX, int contentY, int mx, int my) {
      int fH = 9;
      int x = panelX + 8;
      int w = this.PANEL_W - 16;
      int y = contentY + 4;
      ctx.drawTextWithShadow(this.textRenderer, Text.literal("Items").formatted(new Formatting[]{Formatting.YELLOW, Formatting.BOLD}), x, y, -8892);
      y += fH + 6;
      ClientPlayerEntity lp = MinecraftClient.getInstance().player;
      if (lp != null) {
         boolean hasFood = false;

         for (int si = 0; si < lp.getInventory().size(); si++) {
            ItemStack st = lp.getInventory().getStack(si);
            if (!st.isEmpty() && st.get(DataComponentTypes.FOOD) != null) {
               if (y > contentY + this.PANEL_H - fH - 4) {
                  break;
               }

               hasFood = true;
               String id = Registries.ITEM.getId(st.getItem()).toString();
               boolean fav = TurnBattleClientMod.clientFavoriteFood.contains(id);
               int rowH = fH + 6;
               if (fav) {
                  ctx.fill(x, y - 1, x + w, y + fH + 3, 587193600);
               }

               int starW = 14;
               boolean starHov = mx >= x && mx < x + starW && my >= y - 1 && my < y + fH + 3;
               ctx.drawTextWithShadow(this.textRenderer, Text.literal(fav ? "\u2605" : "\u2606"), x, y, fav ? -8960 : (starHov ? -3355648 : -7829504));
               String name = st.getName().getString() + (st.getCount() > 1 ? " x" + st.getCount() : "");
               if (fav) {
                  name = name + " (\u00d72)";
               }

               ctx.drawTextWithShadow(this.textRenderer, Text.literal(name).formatted(fav ? Formatting.GOLD : Formatting.WHITE), x + 16, y, fav ? -8892 : -1);
               int btnW2 = 26;
               int btnH2 = fH + 4;
               int btnY = y - 1;
               int btnBaseX = x + w - btnW2;
               boolean hasSlot2 = TurnBattleClientMod.clientSecondRecruit >= 0;
               boolean hasSlot1 = TurnBattleClientMod.clientActiveRecruit >= 0;
               if (hasSlot2) {
                  boolean h2 = mx >= btnBaseX && mx < btnBaseX + btnW2 && my >= btnY && my < btnY + btnH2;
                  ctx.fill(btnBaseX, btnY, btnBaseX + btnW2, btnY + btnH2, h2 ? -13413018 : -14535868);
                  ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2192P2"), btnBaseX + btnW2 / 2, btnY + 2, -5579265);
                  btnBaseX -= btnW2 + 2;
               }

               if (hasSlot1) {
                  boolean h1 = mx >= btnBaseX && mx < btnBaseX + btnW2 && my >= btnY && my < btnY + btnH2;
                  ctx.fill(btnBaseX, btnY, btnBaseX + btnW2, btnY + btnH2, h1 ? -13413018 : -14535868);
                  ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u2192P1"), btnBaseX + btnW2 / 2, btnY + 2, -5579265);
                  btnBaseX -= btnW2 + 2;
               }

               boolean hEat = mx >= btnBaseX && mx < btnBaseX + btnW2 && my >= btnY && my < btnY + btnH2;
               ctx.fill(btnBaseX, btnY, btnBaseX + btnW2, btnY + btnH2, hEat ? -13408717 : -14531550);
               ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Eat"), btnBaseX + btnW2 / 2, btnY + 2, -5570646);
               y += rowH;
            }
         }

         if (!hasFood) {
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("No food in inventory.").formatted(Formatting.GRAY),
               panelX + this.PANEL_W / 2,
               contentY + this.PANEL_H / 2,
               -7829368
            );
         }
      }
   }

   private void renderWeaponCard(DrawContext ctx, int x, int y, int maxW, ItemStack stack, WeaponStats ws, int mx, int my) {
      String name = stack.getName().getString();
      MutableText nameText = Text.literal(ws != null && ws.isQuirk ? "\u26a1 " + name : name)
         .formatted(ws != null && ws.isQuirk ? Formatting.GOLD : Formatting.WHITE);
      if (ws != null) {
         Affinity[] elems = ws.getElements();
         if (elems.length > 0) {
            nameText.append(Text.literal(" (").formatted(Formatting.GRAY));

            for (int i = 0; i < elems.length; i++) {
               if (i > 0) {
                  nameText.append(Text.literal(", ").formatted(Formatting.GRAY));
               }

               Affinity elem = elems[i];
               nameText.append(Text.literal(elem.displayName).styled(style -> style.withColor(TextColor.fromRgb(elem.color))));
            }

            nameText.append(Text.literal(")").formatted(Formatting.GRAY));
         }
      }

      ctx.drawTextWithShadow(this.textRenderer, nameText, x, y, -1);
      WeaponStats.Tier tier = WeaponStats.tierOf(stack);

      String tierLabel = switch (tier) {
         case WOOD -> "Wood";
         case STONE -> "Stone";
         case IRON -> "Iron";
         case DIAMOND -> "Diamond";
         case NETHERITE -> "Netherite";
      };

      int tierColor = switch (tier) {
         case WOOD -> -7838157;
         case STONE -> -7697782;
         case IRON -> -5592406;
         case DIAMOND -> -12259585;
         case NETHERITE -> -3390209;
      };
      ctx.drawTextWithShadow(
         this.textRenderer, Text.literal("[" + tierLabel + "]"), x + maxW - this.textRenderer.getWidth("[" + tierLabel + "]") - 50, y - 10, tierColor
      );
      y += 10;
      if (ws == null) {
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("Stats not rolled yet").formatted(Formatting.DARK_GRAY), x, y, -12277112);
      } else {
         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal("+" + ws.baseDamage + (ws.magic ? " Bonus Magic DMG  +" : " Bonus DMG  +") + ws.bonusAmt + " " + WeaponStats.STAT_NAMES[ws.bonusStat])
               .formatted(Formatting.AQUA),
            x,
            y,
            -7807489
         );
         y += 9;
         if (ws.isQuirk) {
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal("+10 " + WeaponStats.STAT_NAMES[ws.quirkStat] + "  -10 " + WeaponStats.STAT_NAMES[ws.quirkMinus])
                  .formatted(Formatting.LIGHT_PURPLE),
               x,
               y,
               -2258689
            );
         }
      }
   }
}
