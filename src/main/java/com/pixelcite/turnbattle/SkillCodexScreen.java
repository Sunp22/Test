package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class SkillCodexScreen extends Screen {
   private final int playerLevel;
   private PlayerClass playerClass;
   private SoulType playerSoul;
   private int skillPoints;
   private Set<String> unlockedSkills;
   private SoulType soulTab;
   private boolean humanTab = false;
   private boolean monsterTab = false;
   private boolean viewingParty = false;
   private PartyMember viewedMember = PartyMember.NONE;
   private int partySkillPoints;
   private Set<String> partyUnlockedSkills;
   private Recruit viewingRecruit = null;
   private int recruitOrdinal = -1;
   private int customRecruitIndex = -1;
   private SkillTreeDefs.Node selectedNode = null;
   private static final int NODE_W = 76;
   private static final int NODE_H = 30;
   private static final int COL_GAP = 20;
   private static final int ROW_GAP = 16;
   private static final int TREE_PAD_X = 24;
   private static final int TREE_PAD_Y = 56;
   private static final int TAB_W = 42;
   private static final int TAB_H = 14;
   private static final int TAB_Y = 32;
   private int scrollX = 0;
   private int scrollY = 0;
   private boolean dragging = false;
   private int dragMX0;
   private int dragMY0;
   private int dragSX0;
   private int dragSY0;
   private int[] btnUnlockRect = new int[4];
   private int[] btnBackRect = new int[4];
   private int[] btnEquipRect = new int[4];

   private boolean isViewingAnyRecruit() {
      return this.viewingRecruit != null && this.recruitOrdinal >= 0 || this.customRecruitIndex >= 0;
   }

   private int activeSkillPoints() {
      if (this.customRecruitIndex < 0) {
         if (this.viewingRecruit != null && this.recruitOrdinal >= 0) {
            return TurnBattleClientMod.clientRecruitSkillPoints[this.recruitOrdinal];
         } else {
            return this.viewingParty ? this.partySkillPoints : this.skillPoints;
         }
      } else {
         int[] cSp = TurnBattleClientMod.customRecruitSkillPoints;
         return cSp != null && this.customRecruitIndex < cSp.length ? cSp[this.customRecruitIndex] : this.skillPoints;
      }
   }

   private Set<String> activeUnlocked() {
      if (this.customRecruitIndex < 0) {
         if (this.viewingRecruit != null && this.recruitOrdinal >= 0) {
            return TurnBattleClientMod.clientRecruitSkills[this.recruitOrdinal];
         } else {
            return this.viewingParty ? this.partyUnlockedSkills : this.unlockedSkills;
         }
      } else {
         Set[] cSkills = TurnBattleClientMod.customRecruitSkills;
         return cSkills != null && this.customRecruitIndex < cSkills.length && cSkills[this.customRecruitIndex] != null
            ? cSkills[this.customRecruitIndex]
            : this.unlockedSkills;
      }
   }

   private Set<String> activeUnequipped() {
      if (this.customRecruitIndex < 0) {
         return this.viewingRecruit != null && this.recruitOrdinal >= 0
            ? TurnBattleClientMod.clientRecruitUnequippedSkills
            : TurnBattleClientMod.clientUnequippedSkills;
      } else {
         Set[] cUneq = TurnBattleClientMod.customRecruitUnequippedSkills;
         return cUneq != null && this.customRecruitIndex < cUneq.length && cUneq[this.customRecruitIndex] != null
            ? cUneq[this.customRecruitIndex]
            : Collections.emptySet();
      }
   }

   private int activeSlotsUsed() {
      Set unlocked = this.activeUnlocked();
      Set uneq = this.activeUnequipped();
      PlayerClass cls = this.activeClass();
      int used = 0;

      for (String s : unlocked) {
         if (!uneq.contains(s)) {
            used += SkillTreeDefs.skillSlotCost(s, cls, this.playerSoul);
         }
      }

      return used;
   }

   private int activeMaxSlots() {
      return SkillTreeDefs.maxSkillSlots(this.playerLevel);
   }

   private PlayerClass activeClass() {
      if (this.customRecruitIndex >= 0) {
         return this.playerClass;
      } else if (this.viewingRecruit != null) {
         return this.viewingRecruit.playerClass;
      } else {
         return this.viewingParty && this.viewedMember != PartyMember.NONE ? this.viewedMember.playerClass : this.playerClass;
      }
   }

   public SkillCodexScreen(Recruit recruit, int level, int pts, Set<String> unlocked) {
      super(Text.literal("Skill Tree \u2014 " + recruit.displayName));
      this.playerLevel = level;
      this.playerClass = recruit.playerClass;
      this.playerSoul = recruit.soulType;
      this.skillPoints = pts;
      this.unlockedSkills = new HashSet<>(unlocked);
      this.soulTab = null;
      this.viewingRecruit = recruit;
      this.recruitOrdinal = recruit.ordinal();
      this.viewingParty = false;
      this.viewedMember = PartyMember.NONE;
      this.partySkillPoints = 0;
      this.partyUnlockedSkills = new HashSet<>();
   }

   public SkillCodexScreen(String customName, PlayerClass cls, SoulType soul, int level, int pts, Set<String> unlocked, int customIndex) {
      super(Text.literal("Skill Tree \u2014 " + customName));
      this.playerLevel = level;
      this.playerClass = cls;
      this.playerSoul = soul;
      this.skillPoints = pts;
      this.unlockedSkills = new HashSet<>(unlocked);
      this.soulTab = null;
      this.viewingRecruit = null;
      this.recruitOrdinal = -1;
      this.customRecruitIndex = customIndex;
      this.viewingParty = false;
      this.viewedMember = PartyMember.NONE;
      this.partySkillPoints = 0;
      this.partyUnlockedSkills = new HashSet<>();
   }

   public SkillCodexScreen(int level, PlayerClass cls, int pts, Set<String> unlocked) {
      super(Text.literal("Skill Tree"));
      this.playerLevel = level;
      this.playerClass = cls;
      this.playerSoul = TurnBattleClientMod.clientSoul;
      this.skillPoints = pts;
      this.unlockedSkills = new HashSet<>(unlocked);
      this.soulTab = cls == PlayerClass.SOUL_MAGE ? this.playerSoul : null;
      this.loadPartyData();
   }

   private void loadPartyData() {
      PartyMember[] members = PartyMember.values();
      int memberOrd = TurnBattleClientMod.clientPartyMember;
      if (memberOrd > 0 && memberOrd < members.length) {
         this.viewedMember = members[memberOrd];
         this.partySkillPoints = TurnBattleClientMod.clientPartySkillPoints[memberOrd];
         this.partyUnlockedSkills = new HashSet<>(TurnBattleClientMod.clientPartyUnlockedSkills[memberOrd]);
      } else {
         this.viewedMember = PartyMember.NONE;
         this.partySkillPoints = 0;
         this.partyUnlockedSkills = new HashSet<>();
      }
   }

   public void refreshData(int pts, Set<String> unlocked) {
      this.playerClass = TurnBattleClientMod.clientPlayerClass;
      this.playerSoul = TurnBattleClientMod.clientSoul;
      this.skillPoints = pts;
      this.unlockedSkills = new HashSet<>(unlocked);
      if (this.playerClass == PlayerClass.SOUL_MAGE && this.soulTab == null) {
         this.soulTab = this.playerSoul;
      }

      this.loadPartyData();
   }

   protected void init() {
      this.addDrawableChild(ButtonWidget.builder(Text.literal("X Close"), b -> this.close()).dimensions(this.width - 62, 5, 56, 16).build());
      if (this.viewedMember != PartyMember.NONE) {
         this.addDrawableChild(ButtonWidget.builder(Text.literal(this.viewingParty ? "\u25c0 My Tree" : this.viewedMember.displayName + " \u25b6"), b -> {
            this.viewingParty = !this.viewingParty;
            b.setMessage(Text.literal(this.viewingParty ? "\u25c0 My Tree" : this.viewedMember.displayName + " \u25b6"));
            this.scrollX = 0;
            this.scrollY = 0;
            this.selectedNode = null;
            this.soulTab = !this.viewingParty && this.playerClass == PlayerClass.SOUL_MAGE ? this.playerSoul : null;
         }).dimensions(this.width - 62 - 84 - 4, 5, 84, 16).build());
      }
   }

   public boolean shouldPause() {
      return false;
   }

   private List<SkillTreeDefs.Node> currentTree() {
      if (this.humanTab && !this.viewingParty && !this.isViewingAnyRecruit()) {
         return SkillTreeDefs.getTree(PlayerClass.HUMAN_MAGE, null);
      } else if (this.monsterTab && !this.viewingParty && !this.isViewingAnyRecruit()) {
         return SkillTreeDefs.getTree(PlayerClass.MONSTER, null);
      } else {
         PlayerClass cls = this.activeClass();
         if (!this.viewingParty && cls == PlayerClass.SOUL_MAGE) {
            SoulType s = this.soulTab != null ? this.soulTab : this.playerSoul;
            return s != null ? SkillTreeDefs.getSoulTree(s) : List.of();
         } else if (this.customRecruitIndex >= 0) {
            return cls != null ? SkillTreeDefs.getTree(cls, null) : List.of();
         } else if (this.viewingRecruit != null) {
            return SkillTreeDefs.getRecruitTree(this.viewingRecruit);
         } else {
            return cls != null ? SkillTreeDefs.getTree(cls, null) : List.of();
         }
      }
   }

   private int nx(SkillTreeDefs.Node n) {
      return 24 + n.col() * 96 - this.scrollX;
   }

   private int ny(SkillTreeDefs.Node n) {
      return 56 + n.row() * 46 - this.scrollY;
   }

   private int ncx(SkillTreeDefs.Node n) {
      return this.nx(n) + 38;
   }

   private int ncy(SkillTreeDefs.Node n) {
      return this.ny(n) + 15;
   }

   private boolean isUnlocked(String s) {
      return this.activeUnlocked().contains(s);
   }

   private boolean prereqsMet(SkillTreeDefs.Node n) {
      for (String r : n.prereqs()) {
         if (!this.isUnlocked(r)) {
            return false;
         }
      }

      return true;
   }

   private boolean isLockedSoul() {
      return this.viewingParty ? false : this.playerClass == PlayerClass.SOUL_MAGE && this.soulTab != null && this.soulTab != this.playerSoul;
   }

   private int soulsNeeded(SkillTreeDefs.Node n) {
      MonsterSkill ms = SkillTreeDefs.monsterSkillByName(n.skill());
      return ms != null ? ms.soulsRequired() : 0;
   }

   private boolean soulReqMet(SkillTreeDefs.Node n) {
      int needed = this.soulsNeeded(n);
      return needed <= 0 ? true : TurnBattleClientMod.clientAbsorbedSouls.size() >= needed;
   }

   private boolean canAfford(SkillTreeDefs.Node n) {
      return !this.isLockedSoul() && this.activeSkillPoints() >= n.cost() && !this.isUnlocked(n.skill()) && this.prereqsMet(n) && this.soulReqMet(n);
   }

   public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, -301726702);
      PlayerClass dispClass = this.activeClass();
      String hdr = dispClass == null
         ? "Skill Tree"
         : (
            !this.viewingParty && dispClass == PlayerClass.SOUL_MAGE && this.soulTab != null
               ? this.soulTab.getDisplayName() + " Soul Skill Tree"
               : (this.viewingParty ? this.viewedMember.displayName + " \u2014 " : "") + (dispClass != null ? dispClass.displayName : "") + " Skill Tree"
         );
      ctx.fill(0, 0, this.width, 28, -1442840542);
      ctx.fill(0, 28, this.width, 29, 1152035583);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("  " + hdr + "  ").formatted(Formatting.AQUA), this.width / 2, 8, 16777215);
      int dispPts = this.activeSkillPoints();
      boolean hasPts = dispPts > 0;
      ctx.fill(this.width / 2 - 64, 16, this.width / 2 + 64, 27, hasPts ? -1724704256 : 856756497);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Skill Points: " + dispPts), this.width / 2, 17, hasPts ? -8892 : -11184811);
      int slotsUsed = this.activeSlotsUsed();
      int slotsMax = this.activeMaxSlots();
      boolean slotsFull = slotsUsed >= slotsMax;
      ctx.fill(this.width / 2 + 70, 16, this.width / 2 + 190, 27, slotsFull ? -1724706543 : 856760849);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Skill Slots: " + slotsUsed + "/" + slotsMax), this.width / 2 + 130, 17, slotsFull ? -39322 : -7798904
      );
      if (this.selectedNode != null) {
         super.render(ctx, mx, my, delta);
         this.renderDetailCard(ctx, this.selectedNode, mx, my);
      } else {
         if (!this.viewingParty && this.playerClass == PlayerClass.SOUL_MAGE) {
            this.renderSoulTabs(ctx, mx, my);
         }

         if (!this.viewingParty && !this.isViewingAnyRecruit() && TurnBattleClientMod.clientPlayerRace == CharacterRace.HUMAN) {
            this.renderHumanTab(ctx, mx, my);
         }

         if (!this.viewingParty && !this.isViewingAnyRecruit() && TurnBattleClientMod.clientPlayerRace == CharacterRace.MONSTER) {
            this.renderMonsterTab(ctx, mx, my);
         }

         List tree = this.currentTree();
         Map bySkill = new HashMap<>();

         for (SkillTreeDefs.Node n : tree) {
            bySkill.put(n.skill(), n);
         }

         if (this.activeClass() != PlayerClass.SOUL_MAGE || this.viewingParty) {
            this.renderBranchDividers(ctx, tree);
         }

         for (SkillTreeDefs.Node n : tree) {
            for (String req : n.prereqs()) {
               SkillTreeDefs.Node p = bySkill.get(req);
               if (p != null) {
                  boolean both = this.isUnlocked(n.skill()) && this.isUnlocked(req);
                  int lc = both ? -12255352 : (this.isUnlocked(req) ? -2002081110 : 858993493);
                  this.drawConnector(ctx, p, n, lc);
               }
            }
         }

         for (SkillTreeDefs.Node n : tree) {
            int x = this.nx(n);
            int y = this.ny(n);
            boolean hov = mx >= x && mx < x + 76 && my >= y && my < y + 30 && my >= 56;
            this.renderNode(ctx, n, hov);
         }

         ctx.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Click a skill to inspect  \u2022  Right-drag to scroll").formatted(Formatting.DARK_GRAY),
            4,
            this.height - 10,
            -13421756
         );
         super.render(ctx, mx, my, delta);
      }
   }

   private void renderHumanTab(DrawContext ctx, int mx, int my) {
      int tab1X = 8;
      int tab2X = 54;
      int ty = 32;
      boolean aHov = mx >= tab1X && mx < tab1X + 42 && my >= ty && my < ty + 14;
      boolean hHov = mx >= tab2X && mx < tab2X + 42 && my >= ty && my < ty + 14;
      ctx.fill(tab1X, ty, tab1X + 42, ty + 14, !this.humanTab ? -871753165 : (aHov ? 1157627903 : 587202559));
      ctx.fill(tab1X, ty + 14 - 1, tab1X + 42, ty + 14, !this.humanTab ? -12285697 : 587202559);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(this.playerClass != null ? this.playerClass.displayName : "Attunement"),
         tab1X + 21,
         ty + 3,
         !this.humanTab ? -7816193 : -11184794
      );
      ctx.fill(tab2X, ty, tab2X + 42, ty + 14, this.humanTab ? -871753165 : (hHov ? 1157627903 : 587202559));
      ctx.fill(tab2X, ty + 14 - 1, tab2X + 42, ty + 14, this.humanTab ? -7798972 : 587202559);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Human"), tab2X + 21, ty + 3, this.humanTab ? -5570714 : -11184794);
   }

   private void renderMonsterTab(DrawContext ctx, int mx, int my) {
      int tab1X = 8;
      int tab2X = 54;
      int ty = 32;
      boolean aHov = mx >= tab1X && mx < tab1X + 42 && my >= ty && my < ty + 14;
      boolean mHov = mx >= tab2X && mx < tab2X + 42 && my >= ty && my < ty + 14;
      ctx.fill(tab1X, ty, tab1X + 42, ty + 14, !this.monsterTab ? -871753165 : (aHov ? 1157627903 : 587202559));
      ctx.fill(tab1X, ty + 14 - 1, tab1X + 42, ty + 14, !this.monsterTab ? -12285697 : 587202559);
      ctx.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(this.playerClass != null ? this.playerClass.displayName : "Attunement"),
         tab1X + 21,
         ty + 3,
         !this.monsterTab ? -7816193 : -11184794
      );
      ctx.fill(tab2X, ty, tab2X + 42, ty + 14, this.monsterTab ? -871753165 : (mHov ? 1157627903 : 587202559));
      ctx.fill(tab2X, ty + 14 - 1, tab2X + 42, ty + 14, this.monsterTab ? -2276284 : 587202559);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Monster"), tab2X + 21, ty + 3, this.monsterTab ? -30584 : -11184794);
   }

   private void renderSoulTabs(DrawContext ctx, int mx, int my) {
      SoulType[] souls = SoulType.values();
      int totalW = souls.length * 44 - 2;
      int startX = (this.width - totalW) / 2;

      for (int i = 0; i < souls.length; i++) {
         SoulType s = souls[i];
         int tx = startX + i * 44;
         boolean active = s == this.soulTab;
         boolean isPlayer = s == this.playerSoul;
         boolean hover = mx >= tx && mx < tx + 42 && my >= 32 && my < 46;
         int sc = s.getColor() | 0xFF000000;
         ctx.fill(tx, 32, tx + 42, 46, active ? sc & 16777215 | -872415232 : (hover ? 1157627903 : 587202559));
         ctx.fill(tx, 45, tx + 42, 46, active ? sc : 587202559);
         String name = s.getDisplayName();
         String lbl = (isPlayer ? "*" : "") + name.substring(0, Math.min(4, name.length())).toUpperCase(Locale.ROOT);
         ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(lbl), tx + 21, 35, active ? sc : (isPlayer ? -188 : -12303275));
      }
   }

   private void renderBranchDividers(DrawContext ctx, List<SkillTreeDefs.Node> tree) {
      Map colRange = new LinkedHashMap<>();

      for (SkillTreeDefs.Node n : tree) {
         if (!n.branch().equals("Root")) {
            int[] r = colRange.computeIfAbsent(n.branch(), k -> new int[]{n.col(), n.col()});
            r[0] = Math.min(r[0], n.col());
            r[1] = Math.max(r[1], n.col());
         }
      }

      List branches = new ArrayList<>(colRange.keySet());

      for (int i = 0; i + 1 < branches.size(); i++) {
         int rc = colRange.get(branches.get(i))[1];
         int dx = 24 + rc * 96 + 76 + 10 - this.scrollX;
         ctx.fill(dx, 42, dx + 1, this.height - 16, 473121962);
      }
   }

   private void renderBranchLabels(DrawContext ctx, List<SkillTreeDefs.Node> tree) {
      Map minCol = new LinkedHashMap<>();

      for (SkillTreeDefs.Node n : tree) {
         if (!n.branch().equals("Root")) {
            minCol.merge(n.branch(), n.col(), Math::min);
         }
      }

      for (Entry e : minCol.entrySet()) {
         int lx = 24 + e.getValue() * 96 - this.scrollX;
         int ly = 42;
         ctx.drawTextWithShadow(this.textRenderer, Text.literal("-- " + e.getKey() + " --").formatted(Formatting.DARK_AQUA), lx, ly, -14535868);
      }
   }

   private void drawConnector(DrawContext ctx, SkillTreeDefs.Node from, SkillTreeDefs.Node to, int col) {
      int x1 = this.ncx(from);
      int y1 = this.ny(from) + 30;
      int x2 = this.ncx(to);
      int y2 = this.ny(to);
      if (x1 == x2) {
         ctx.fill(x1 - 1, y1, x1 + 1, y2, col);
      } else {
         int midY = (y1 + y2) / 2;
         ctx.fill(x1 - 1, y1, x1 + 1, midY, col);
         ctx.fill(Math.min(x1, x2), midY - 1, Math.max(x1, x2) + 1, midY + 1, col);
         ctx.fill(x2 - 1, midY, x2 + 1, y2, col);
      }

      ctx.fill(x2 - 2, y2 - 3, x2 + 2, y2, col);
   }

   private void renderNode(DrawContext ctx, SkillTreeDefs.Node n, boolean hov) {
      int x = this.nx(n);
      int y = this.ny(n);
      if (x + 76 >= 0 && x <= this.width && y + 30 >= 56 && y <= this.height) {
         boolean isRoot = n.branch().equals("Root");
         boolean isStat = SkillTreeDefs.isStatNode(n.skill());
         boolean unlocked = this.isUnlocked(n.skill());
         boolean affordable = this.canAfford(n);
         boolean locked = this.isLockedSoul();
         int nc = isRoot ? 16768324 : n.color();
         if (isStat) {
            int statBg = unlocked ? -1442827486 : -1442833900;
            int statBorder = unlocked ? -12255284 : (affordable ? -12264005 : (hov ? -13408683 : -15060176));
            int statTxt = unlocked ? -1 : (affordable ? -7798802 : -13408683);
            int statIco = unlocked ? -12255284 : (affordable ? -14500950 : -14522795);
            ctx.fill(x, y, x + 76, y + 30, statBg);
            ctx.fill(x, y, x + 76, y + 1, statBorder);
            ctx.fill(x, y + 30 - 1, x + 76, y + 30, statBorder);
            ctx.fill(x, y, x + 1, y + 30, statBorder);
            ctx.fill(x + 76 - 1, y, x + 76, y + 30, statBorder);
            if (unlocked) {
               ctx.fill(x, y, x + 3, y + 30, -12255284);
            }

            ctx.drawTextWithShadow(this.textRenderer, Text.literal("\u25c6"), x + 4, y + 4, statIco);
            String lbl = SkillTreeDefs.statNodeDisplayName(n.skill());

            while (this.textRenderer.getWidth(lbl) > 58 && lbl.length() > 2) {
               lbl = lbl.substring(0, lbl.length() - 1);
            }

            ctx.drawTextWithShadow(this.textRenderer, Text.literal(lbl), x + 15, y + 4, statTxt);
            String costStr = n.cost() + "pt";
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(costStr),
               x + 76 - this.textRenderer.getWidth(costStr) - 3,
               y + 30 - 10,
               this.activeSkillPoints() >= n.cost() ? -12268476 : -8965325
            );
            if (unlocked) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("OK"), x + 4, y + 30 - 10, -13373901);
            } else if (!locked && affordable) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("+" + n.cost()), x + 4, y + 30 - 10, -12255284);
            }
         } else {
            int bg = isRoot ? (unlocked ? -869064192 : -1440608000) : (unlocked ? nc & 16777215 | -1442840576 : (affordable ? -1441524694 : 1712197146));
            ctx.fill(x, y, x + 76, y + 30, bg);
            int border = isRoot ? (unlocked ? -8892 : -5601280) : (unlocked ? nc : (affordable ? -7829266 : (hov ? -12303258 : -14342859)));
            if (hov && !locked) {
               border = isRoot ? -4506 : -2236929;
            }

            ctx.fill(x, y, x + 76, y + 1, border);
            ctx.fill(x, y + 30 - 1, x + 76, y + 30, border);
            ctx.fill(x, y, x + 1, y + 30, border);
            ctx.fill(x + 76 - 1, y, x + 76, y + 30, border);
            if (isRoot) {
               int glow = unlocked ? 1728044356 : 866813952;
               ctx.fill(x - 1, y - 1, x + 76 + 1, y, glow);
               ctx.fill(x - 1, y + 30, x + 76 + 1, y + 30 + 1, glow);
               ctx.fill(x - 1, y - 1, x, y + 30 + 1, glow);
               ctx.fill(x + 76, y - 1, x + 76 + 1, y + 30 + 1, glow);
            }

            if (unlocked) {
               ctx.fill(x, y, x + 3, y + 30, nc);
            }

            BattleSkill skill = this.resolveSkill(n.skill());
            String lbl = skill != null ? skill.getDisplayName() : n.skill();

            while (this.textRenderer.getWidth(lbl) > 64 && lbl.length() > 2) {
               lbl = lbl.substring(0, lbl.length() - 1);
            }

            int textCol = isRoot ? (unlocked ? -4472 : -3364284) : (unlocked ? -1 : (affordable ? -4473857 : -12303275));
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(lbl), x + 5, y + 4, textCol);
            String costStr = n.cost() + "pt";
            ctx.drawTextWithShadow(
               this.textRenderer,
               Text.literal(costStr),
               x + 76 - this.textRenderer.getWidth(costStr) - 3,
               y + 30 - 10,
               this.activeSkillPoints() >= n.cost() ? -12268476 : -8965325
            );
            int soulReq = this.soulsNeeded(n);
            if (soulReq > 0 && !unlocked) {
               boolean soulMet = TurnBattleClientMod.clientAbsorbedSouls.size() >= soulReq;
               String soulStr = "\u2665" + soulReq;
               ctx.drawTextWithShadow(
                  this.textRenderer, Text.literal(soulStr), x + 76 - this.textRenderer.getWidth(soulStr) - 3, y + 4, soulMet ? -12255352 : -48060
               );
            }

            if (unlocked) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("OK"), x + 4, y + 30 - 10, -13373901);
            } else if (isRoot && !locked && affordable) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("START"), x + 4, y + 30 - 10, -8892);
            } else if (!locked && affordable) {
               ctx.drawTextWithShadow(this.textRenderer, Text.literal("+" + n.cost()), x + 4, y + 30 - 10, -7824897);
            }
         }
      }
   }

   private void renderDetailCard(DrawContext ctx, SkillTreeDefs.Node n, int mx, int my) {
      boolean isStat = SkillTreeDefs.isStatNode(n.skill());
      BattleSkill skill = isStat ? null : this.resolveSkill(n.skill());
      if (isStat || skill != null) {
         boolean unlocked = this.isUnlocked(n.skill());
         boolean affordable = this.canAfford(n);
         boolean locked = this.isLockedSoul();
         int ac = isStat ? -12255284 : (n.branch().equals("Root") ? 16768324 : n.color());
         int CW = Math.min(340, this.width - 40);
         int PAD = 14;
         int LH = 12;
         int TW = CW - 28;
         List lines = new ArrayList<>();
         if (isStat) {
            lines.add(new SkillCodexScreen.DetailLine(SkillTreeDefs.statNodeDisplayName(n.skill()), ac, true));
            lines.add(new SkillCodexScreen.DetailLine("", 0, false));
            lines.add(new SkillCodexScreen.DetailLine("Type: Stat Upgrade", 4521932, false));
            lines.add(new SkillCodexScreen.DetailLine("Skill Point Cost: " + n.cost() + " pt" + (n.cost() > 1 ? "s" : ""), 16768324, false));
            lines.add(new SkillCodexScreen.DetailLine("", 0, false));
         } else {
            lines.add(new SkillCodexScreen.DetailLine(skill.getDisplayName(), ac, true));
            lines.add(new SkillCodexScreen.DetailLine("", 0, false));
            lines.add(new SkillCodexScreen.DetailLine("Type: " + skill.getType().name(), 5622988, false));
            lines.add(
               new SkillCodexScreen.DetailLine(
                  skill == LightnerSkill.SNOWGRAVE ? "TP Cost: double your max TP" : "TP Cost: " + skill.getTpCost(), 5622988, false
               )
            );
            if (SkillScaling.scales(skill)) {
               lines.add(
                  new SkillCodexScreen.DetailLine(
                     "Scales with: " + SkillScaling.statName(skill) + (SkillScaling.usesOffense(skill) ? "  (a physical skill)" : ""),
                     SkillScaling.statColor(skill),
                     false
                  )
               );
            }

            lines.add(new SkillCodexScreen.DetailLine("Skill Point Cost: " + n.cost() + " pt" + (n.cost() > 1 ? "s" : ""), 16768324, false));
            int slotCost = SkillTreeDefs.skillSlotCost(n.skill(), this.activeClass(), this.playerSoul);
            lines.add(new SkillCodexScreen.DetailLine("Skill Slot Cost: " + slotCost, -7807489, false));
            int soulReq = this.soulsNeeded(n);
            if (soulReq > 0) {
               int absorbed = TurnBattleClientMod.clientAbsorbedSouls.size();
               lines.add(new SkillCodexScreen.DetailLine("Souls Required: " + absorbed + " / " + soulReq, absorbed >= soulReq ? -12255352 : -48060, false));
            }

            if (skill instanceof MonsterSkill && MonsterSkill.viewerSoulCount > 0) {
               int sc = MonsterSkill.viewerSoulCount;
               lines.add(new SkillCodexScreen.DetailLine("SOUL Power: +" + Math.round(10.0F * (float)sc) + "% (" + sc + " absorbed)", -39254, false));
            }

            lines.add(new SkillCodexScreen.DetailLine("", 0, false));
         }

         if (unlocked) {
            lines.add(new SkillCodexScreen.DetailLine("\u2714  Already Unlocked", 4521796, false));
         } else if (locked) {
            lines.add(new SkillCodexScreen.DetailLine("Locked Soul \u2014 view only", 5596791, false));
         } else if (!this.prereqsMet(n)) {
            StringBuilder sb = new StringBuilder("Requires: ");

            for (int i = 0; i < n.prereqs().length; i++) {
               if (i > 0) {
                  sb.append(", ");
               }

               if (SkillTreeDefs.isStatNode(n.prereqs()[i])) {
                  sb.append(SkillTreeDefs.statNodeDisplayName(n.prereqs()[i]));
               } else {
                  BattleSkill r = this.resolveSkill(n.prereqs()[i]);
                  sb.append(r != null ? r.getDisplayName() : n.prereqs()[i]);
               }
            }

            lines.add(new SkillCodexScreen.DetailLine(sb.toString(), 16746547, false));
         } else if (!this.soulReqMet(n)) {
            lines.add(
               new SkillCodexScreen.DetailLine(
                  "Not enough absorbed souls (" + TurnBattleClientMod.clientAbsorbedSouls.size() + " / " + this.soulsNeeded(n) + ")", 16729156, false
               )
            );
         } else if (this.activeSkillPoints() < n.cost()) {
            lines.add(new SkillCodexScreen.DetailLine("Not enough skill points (" + this.activeSkillPoints() + " / " + n.cost() + ")", 16729156, false));
         } else {
            lines.add(new SkillCodexScreen.DetailLine("Ready to unlock!", isStat ? 4521932 : 8956671, false));
         }

         lines.add(new SkillCodexScreen.DetailLine("", 0, false));
         String desc = isStat ? SkillTreeDefs.statNodeDescription(n.skill()) : skill.getContextDescription();
         if (desc != null && !desc.isEmpty()) {
            lines.add(new SkillCodexScreen.DetailLine("Effect:", 16777130, false));

            for (String wl : wrapText(desc, TW / 6)) {
               lines.add(new SkillCodexScreen.DetailLine(wl, 13421772, false));
            }
         }

         int BTN_H = 20;
         int BTN_GAP = 6;
         boolean showEquipBtn = unlocked && !isStat;
         int numBtns = showEquipBtn ? 3 : 2;
         int btnW = (CW - 28 - 6 * (numBtns - 1)) / numBtns;
         int contentH = lines.size() * 12 + 20 + 6 + 28 + 6;
         int cx = (this.width - CW) / 2;
         int cy = (this.height - contentH) / 2;
         ctx.fill(0, 0, this.width, this.height, -872415215);
         ctx.fill(cx - 3, cy - 3, cx + CW + 3, cy + contentH + 3, ac & 16777215 | 1140850688);
         ctx.fill(cx, cy, cx + CW, cy + contentH, -134020331);
         ctx.fill(cx, cy, cx + CW, cy + 4, ac);
         ctx.fill(cx, cy + 4, cx + 3, cy + contentH, ac & 16777215 | 1711276032);
         ctx.fill(cx, cy + contentH - 1, cx + CW, cy + contentH, ac & 16777215 | -2013265920);
         int ly = cy + 14;

         for (SkillCodexScreen.DetailLine line : lines) {
            if (line.text().isEmpty()) {
               ly += 6;
            } else {
               int col = 0xFF000000 | line.col();
               int lx = cx + 14;
               if (line.bold()) {
                  ctx.drawTextWithShadow(this.textRenderer, Text.literal(line.text()), lx + 1, ly, col);
               }

               ctx.drawTextWithShadow(this.textRenderer, Text.literal(line.text()), lx, ly, col);
               ly += 12;
            }
         }

         int btnY = cy + contentH - 20 - 14;
         int totalBtnW = numBtns * btnW + 6 * (numBtns - 1);
         int btnAreaX = cx + (CW - totalBtnW) / 2;
         boolean backHov = mx >= btnAreaX && mx < btnAreaX + btnW && my >= btnY && my < btnY + 20;
         ctx.fill(btnAreaX, btnY, btnAreaX + btnW, btnY + 20, backHov ? -13421739 : -15066573);
         ctx.fill(btnAreaX, btnY, btnAreaX + btnW, btnY + 1, -10066262);
         ctx.fill(btnAreaX, btnY, btnAreaX + 1, btnY + 20, -10066262);
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, Text.literal("\u25c4 Back").formatted(Formatting.GRAY), btnAreaX + btnW / 2, btnY + (20 - 9) / 2, -3355444
         );
         this.btnBackRect = new int[]{btnAreaX, btnY, btnW, 20};
         if (showEquipBtn) {
            Set uneqSet;
            if (this.customRecruitIndex < 0) {
               if (this.viewingRecruit != null && this.recruitOrdinal >= 0) {
                  uneqSet = TurnBattleClientMod.clientRecruitUnequippedSkills;
               } else {
                  uneqSet = TurnBattleClientMod.clientUnequippedSkills;
               }
            } else {
               Set[] cUneq = TurnBattleClientMod.customRecruitUnequippedSkills;
               uneqSet = cUneq != null && this.customRecruitIndex < cUneq.length && cUneq[this.customRecruitIndex] != null
                  ? cUneq[this.customRecruitIndex]
                  : Collections.emptySet();
            }

            boolean isUneq = uneqSet.contains(n.skill());
            int eqX = btnAreaX + btnW + 6;
            boolean eqHov = mx >= eqX && mx < eqX + btnW && my >= btnY && my < btnY + 20;
            int eqBg = isUneq ? (eqHov ? -14531550 : -15654383) : (eqHov ? -12312030 : -14544623);
            int eqBdr = isUneq ? -12268476 : -3390396;
            ctx.fill(eqX, btnY, eqX + btnW, btnY + 20, eqBg);
            ctx.fill(eqX, btnY, eqX + btnW, btnY + 1, eqBdr);
            ctx.fill(eqX, btnY, eqX + 1, btnY + 20, eqBdr);
            String eqLabel = isUneq ? "\u2295 Equip" : "\u2296 Unequip";
            int eqTxt = isUneq ? (eqHov ? -7798904 : -12268476) : (eqHov ? -30584 : -3390396);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(eqLabel), eqX + btnW / 2, btnY + (20 - 9) / 2, eqTxt);
            this.btnEquipRect = new int[]{eqX, btnY, btnW, 20};
         }

         int unlockX = btnAreaX + (showEquipBtn ? 2 : 1) * (btnW + 6);
         if (!unlocked && !locked) {
            boolean unlHov = mx >= unlockX && mx < unlockX + btnW && my >= btnY && my < btnY + 20;
            int unlBg = affordable ? (unlHov ? -14531550 : -15654383) : -15068911;
            int unlBdr = affordable ? -12268476 : -11189180;
            ctx.fill(unlockX, btnY, unlockX + btnW, btnY + 20, unlBg);
            ctx.fill(unlockX, btnY, unlockX + btnW, btnY + 1, unlBdr);
            ctx.fill(unlockX, btnY, unlockX + 1, btnY + 20, unlBdr);
            int unlTxt = affordable ? (unlHov ? -7798904 : -12268476) : -11189180;
            ctx.drawCenteredTextWithShadow(
               this.textRenderer,
               Text.literal("Unlock  (" + n.cost() + "pt)").formatted(affordable ? Formatting.GREEN : Formatting.DARK_GRAY),
               unlockX + btnW / 2,
               btnY + (20 - 9) / 2,
               unlTxt
            );
         }

         this.btnUnlockRect = new int[]{unlockX, btnY, btnW, 20};
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (btn != 0 && btn != 1 && btn != 2) {
         return super.mouseClicked(mx, my, btn);
      } else if (this.selectedNode != null) {
         if (btn == 0) {
            if (mx >= (double)this.btnBackRect[0]
               && mx < (double)(this.btnBackRect[0] + this.btnBackRect[2])
               && my >= (double)this.btnBackRect[1]
               && my < (double)(this.btnBackRect[1] + this.btnBackRect[3])) {
               this.selectedNode = null;
               TurnBattleSounds.playSelect();
               return true;
            }

            if (this.isUnlocked(this.selectedNode.skill())
               && !SkillTreeDefs.isStatNode(this.selectedNode.skill())
               && mx >= (double)this.btnEquipRect[0]
               && mx < (double)(this.btnEquipRect[0] + this.btnEquipRect[2])
               && my >= (double)this.btnEquipRect[1]
               && my < (double)(this.btnEquipRect[1] + this.btnEquipRect[3])) {
               Set uneqSet;
               if (this.customRecruitIndex >= 0) {
                  Set[] cUneq = TurnBattleClientMod.customRecruitUnequippedSkills;
                  uneqSet = cUneq != null && this.customRecruitIndex < cUneq.length && cUneq[this.customRecruitIndex] != null
                     ? cUneq[this.customRecruitIndex]
                     : null;
               } else if (this.viewingRecruit != null) {
                  uneqSet = TurnBattleClientMod.clientRecruitUnequippedSkills;
               } else {
                  uneqSet = TurnBattleClientMod.clientUnequippedSkills;
               }

               if (uneqSet != null) {
                  boolean wouldEquip = uneqSet.contains(this.selectedNode.skill());
                  if (wouldEquip) {
                     int cost = SkillTreeDefs.skillSlotCost(this.selectedNode.skill(), this.activeClass(), this.playerSoul);
                     if (this.activeSlotsUsed() + cost > this.activeMaxSlots()) {
                        TurnBattleSounds.playClient(TurnBattleSounds.DAMAGE, 1.0F, 1.0F);
                        return true;
                     }

                     uneqSet.remove(this.selectedNode.skill());
                  } else {
                     uneqSet.add(this.selectedNode.skill());
                  }

                  PacketByteBuf buf = PacketByteBufs.create();
                  int slotCode = this.customRecruitIndex >= 0 ? 1000 + this.customRecruitIndex : (this.viewingRecruit != null ? this.recruitOrdinal : -1);
                  buf.writeInt(slotCode);
                  buf.writeString(this.selectedNode.skill(), 64);
                  Net121.sendToServer(TurnBattleMod.SKILL_EQUIP_TOGGLE, buf);
               }

               TurnBattleSounds.playSelect();
               return true;
            }

            if (!this.isUnlocked(this.selectedNode.skill())
               && !this.isLockedSoul()
               && mx >= (double)this.btnUnlockRect[0]
               && mx < (double)(this.btnUnlockRect[0] + this.btnUnlockRect[2])
               && my >= (double)this.btnUnlockRect[1]
               && my < (double)(this.btnUnlockRect[1] + this.btnUnlockRect[3])) {
               if (this.canAfford(this.selectedNode)) {
                  this.sendUnlock(this.selectedNode.skill());
                  this.selectedNode = null;
               }

               TurnBattleSounds.playSelect();
               return true;
            }
         }

         TurnBattleSounds.playSelect();
         return true;
      } else {
         if (!this.viewingParty && !this.isViewingAnyRecruit() && TurnBattleClientMod.clientPlayerRace == CharacterRace.HUMAN) {
            int tab1X = 8;
            int tab2X = 54;
            if (mx >= (double)tab1X && mx < (double)(tab1X + 42) && my >= 32.0 && my < 46.0) {
               this.humanTab = false;
               this.selectedNode = null;
               this.scrollX = 0;
               this.scrollY = 0;
               TurnBattleSounds.playSelect();
               return true;
            }

            if (mx >= (double)tab2X && mx < (double)(tab2X + 42) && my >= 32.0 && my < 46.0) {
               this.humanTab = true;
               this.selectedNode = null;
               this.scrollX = 0;
               this.scrollY = 0;
               TurnBattleSounds.playSelect();
               return true;
            }
         }

         if (!this.viewingParty && !this.isViewingAnyRecruit() && TurnBattleClientMod.clientPlayerRace == CharacterRace.MONSTER) {
            int tab1Xx = 8;
            int tab2Xx = 54;
            if (mx >= (double)tab1Xx && mx < (double)(tab1Xx + 42) && my >= 32.0 && my < 46.0) {
               this.monsterTab = false;
               this.selectedNode = null;
               this.scrollX = 0;
               this.scrollY = 0;
               TurnBattleSounds.playSelect();
               return true;
            }

            if (mx >= (double)tab2Xx && mx < (double)(tab2Xx + 42) && my >= 32.0 && my < 46.0) {
               this.monsterTab = true;
               this.selectedNode = null;
               this.scrollX = 0;
               this.scrollY = 0;
               TurnBattleSounds.playSelect();
               return true;
            }
         }

         if (this.playerClass == PlayerClass.SOUL_MAGE) {
            SoulType[] souls = SoulType.values();
            int totalW = souls.length * 44 - 2;
            int startX = (this.width - totalW) / 2;

            for (int i = 0; i < souls.length; i++) {
               int tx = startX + i * 44;
               if (mx >= (double)tx && mx < (double)(tx + 42) && my >= 32.0 && my < 46.0) {
                  this.soulTab = souls[i];
                  TurnBattleSounds.playSelect();
                  return true;
               }
            }
         }

         if (btn == 0) {
            for (SkillTreeDefs.Node n : this.currentTree()) {
               int x = this.nx(n);
               int y = this.ny(n);
               if (mx >= (double)x && mx < (double)(x + 76) && my >= (double)y && my < (double)(y + 30) && (int)my >= 56) {
                  this.selectedNode = n;
                  TurnBattleSounds.playSelect();
                  return true;
               }
            }
         }

         if (btn != 1 && btn != 2) {
            return super.mouseClicked(mx, my, btn);
         } else {
            this.dragging = true;
            this.dragMX0 = (int)mx;
            this.dragMY0 = (int)my;
            this.dragSX0 = this.scrollX;
            this.dragSY0 = this.scrollY;
            TurnBattleSounds.playSelect();
            return true;
         }
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256 && this.selectedNode != null) {
         this.selectedNode = null;
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean mouseReleased(double mx, double my, int btn) {
      if (btn == 1 || btn == 2) {
         this.dragging = false;
      }

      return super.mouseReleased(mx, my, btn);
   }

   public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
      if (this.dragging) {
         this.scrollX = Math.max(0, this.dragSX0 - ((int)mx - this.dragMX0));
         this.scrollY = Math.max(0, this.dragSY0 - ((int)my - this.dragMY0));
         return true;
      } else {
         return super.mouseDragged(mx, my, btn, dx, dy);
      }
   }

   public boolean mouseScrolled(double mx, double my, double scrollX, double delta) {
      this.scrollY = Math.max(0, this.scrollY - (int)(delta * 22.0));
      return true;
   }

   private void sendUnlock(String skill) {
      int cost = SkillTreeDefs.nodeCost(skill, this.activeClass());
      if (this.customRecruitIndex >= 0) {
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeInt(1000 + this.customRecruitIndex);
         buf.writeString(skill, 64);
         Net121.sendToServer(TurnBattleMod.RECRUIT_SKILL_UNLOCK, buf);
         Set[] cSkills = TurnBattleClientMod.customRecruitSkills;
         if (cSkills != null && this.customRecruitIndex < cSkills.length && cSkills[this.customRecruitIndex] != null) {
            cSkills[this.customRecruitIndex].add(skill);
         }

         int[] cSp = TurnBattleClientMod.customRecruitSkillPoints;
         if (cSp != null && this.customRecruitIndex < cSp.length) {
            cSp[this.customRecruitIndex] = Math.max(0, cSp[this.customRecruitIndex] - cost);
         }
      } else if (this.viewingRecruit != null && this.recruitOrdinal >= 0) {
         PacketByteBuf bufx = PacketByteBufs.create();
         bufx.writeInt(this.recruitOrdinal);
         bufx.writeString(skill, 64);
         Net121.sendToServer(TurnBattleMod.RECRUIT_SKILL_UNLOCK, bufx);
         TurnBattleClientMod.clientRecruitSkills[this.recruitOrdinal].add(skill);
         TurnBattleClientMod.clientRecruitSkillPoints[this.recruitOrdinal] = Math.max(
            0, TurnBattleClientMod.clientRecruitSkillPoints[this.recruitOrdinal] - cost
         );
      } else if (this.viewingParty && this.viewedMember != PartyMember.NONE) {
         PacketByteBuf bufx = PacketByteBufs.create();
         bufx.writeByte(this.viewedMember.ordinal());
         bufx.writeString(skill, 64);
         Net121.sendToServer(TurnBattleMod.PARTY_SKILL_UNLOCK, bufx);
         this.partyUnlockedSkills.add(skill);
         this.partySkillPoints = Math.max(0, this.partySkillPoints - cost);
         TurnBattleClientMod.clientPartySkillPoints[this.viewedMember.ordinal()] = this.partySkillPoints;
         TurnBattleClientMod.clientPartyUnlockedSkills[this.viewedMember.ordinal()].add(skill);
      } else {
         PacketByteBuf bufx = PacketByteBufs.create();
         bufx.writeString(skill, 64);
         Net121.sendToServer(TurnBattleMod.SKILL_UNLOCK, bufx);
         this.unlockedSkills.add(skill);
         this.skillPoints = Math.max(0, this.skillPoints - cost);
         TurnBattleClientMod.clientUnlockedSkills.add(skill);
         TurnBattleClientMod.clientSkillPoints = this.skillPoints;
      }
   }

   private BattleSkill resolveSkill(String name) {
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

   private static List<String> wrapText(String text, int maxChars) {
      List out = new ArrayList<>();
      String[] words = text.split(" ");
      StringBuilder cur = new StringBuilder();

      for (String w : words) {
         if (cur.length() == 0) {
            cur.append(w);
         } else if (cur.length() + 1 + w.length() <= maxChars) {
            cur.append(' ').append(w);
         } else {
            out.add(cur.toString());
            cur = new StringBuilder(w);
         }
      }

      if (cur.length() > 0) {
         out.add(cur.toString());
      }

      return out;
   }

   private static record DetailLine(String text, int col, boolean bold) {
   }
}
