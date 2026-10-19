package com.pixelcite.turnbattle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class TurnBattleConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String FILE_NAME = "turnbattle.json";
   public float skillDamageScale = 1.5F;
   public float healSpellScale = 1.5F;
   public float tpSpellScale = 1.5F;
   public float distanceLevelMultiplier = 1.0F;
   public boolean rollingHealth = false;
   public boolean storyMode = false;
   public boolean passiveMobBattles = false;
   public String visualStyle = "deltarune";
   public boolean deltarune_battle_bg = true;
   @SerializedName("reverse-battle-layout")
   public boolean reverseBattleLayout = false;
   public boolean showMobNametags = true;
   public int baseSkillSlots = 10;
   public int skillSlotsPerTenLevels = 5;
   public int vfxPixelSize = 2;
   public boolean liveLockFirstPerson = false;
   private static TurnBattleConfig INSTANCE = null;

   public static TurnBattleConfig get() {
      if (INSTANCE == null) {
         load();
      }

      return INSTANCE;
   }

   public static void load() {
      Path path = FabricLoader.getInstance().getConfigDir().resolve("turnbattle.json");
      if (Files.exists(path)) {
         try (Reader r = Files.newBufferedReader(path)) {
            INSTANCE = (TurnBattleConfig)GSON.fromJson(r, TurnBattleConfig.class);
            if (INSTANCE != null) {
               INSTANCE.clamp();
               return;
            }
         } catch (JsonSyntaxException | IOException var6) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Failed to read config, using defaults: " + var6.getMessage());
         }
      }

      INSTANCE = new TurnBattleConfig();
      save();
   }

   public static void save() {
      if (INSTANCE != null) {
         Path path = FabricLoader.getInstance().getConfigDir().resolve("turnbattle.json");

         try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(INSTANCE, w);
         } catch (IOException var6) {
            TurnBattleMod.LOGGER.warn("[TurnBattle] Failed to save config: " + var6.getMessage());
         }
      }
   }

   public void clamp() {
      this.skillDamageScale = Math.max(0.1F, Math.min(10.0F, this.skillDamageScale));
      if (this.healSpellScale <= 0.0F) {
         this.healSpellScale = 1.5F;
      }

      if (this.tpSpellScale <= 0.0F) {
         this.tpSpellScale = 1.5F;
      }

      this.healSpellScale = Math.max(0.1F, Math.min(10.0F, this.healSpellScale));
      this.tpSpellScale = Math.max(0.1F, Math.min(10.0F, this.tpSpellScale));
      if (this.distanceLevelMultiplier <= 0.0F) {
         this.distanceLevelMultiplier = 1.0F;
      }

      this.distanceLevelMultiplier = Math.max(0.1F, Math.min(10.0F, this.distanceLevelMultiplier));
      this.baseSkillSlots = Math.max(1, Math.min(999, this.baseSkillSlots));
      this.skillSlotsPerTenLevels = Math.max(0, Math.min(999, this.skillSlotsPerTenLevels));
      this.vfxPixelSize = Math.max(0, Math.min(6, this.vfxPixelSize));
      this.storyMode = false;
   }
}
