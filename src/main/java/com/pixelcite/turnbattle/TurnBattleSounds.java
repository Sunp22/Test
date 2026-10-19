package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class TurnBattleSounds {
   public static final SoundEvent MUS_BATTLE_PSI = register("mus_battle_psi");
   public static final SoundEvent MUS_BATTLE_LIGHTNER = register("mus_battle_lightner");
   public static final SoundEvent MUS_BATTLE_MOB = register("mus_battle_mob");
   public static final SoundEvent RUDEBUSTER_SWING = register("snd_rudebuster_swing");
   public static final SoundEvent SND_REMOVE_SOUL = register("snd_removesoul");
   public static final SoundEvent SND_REMOVE_SOUL_WORLD = register("snd_removesoul_world");
   public static final SoundEvent SND_GRAB = register("snd_grab");
   public static final SoundEvent DTRANS_DRONE = register("snd_dtrans_drone");
   public static final SoundEvent DTRANS_SQUARE = register("snd_dtrans_square");
   public static final SoundEvent DTRANS_TWINKLE = register("snd_dtrans_twinkle");
   public static final SoundEvent DTRANS_FLIP = register("snd_dtrans_flip");
   public static final SoundEvent DTRANS_LW = register("snd_dtrans_lw");
   public static final SoundEvent GRAZE = register("snd_graze");
   public static final SoundEvent DAMAGE = register("snd_damage");
   public static final SoundEvent HURT = register("snd_hurt");
   public static final SoundEvent DEFEAT = register("snd_defeat");
   public static final SoundEvent SPARE = register("snd_spare");
   public static final SoundEvent DEATHNOISE = register("snd_deathnoise");
   public static final SoundEvent PETRIFY = register("snd_petrify");
   public static final SoundEvent BATTLE_START = register("battle_start");
   public static final SoundEvent SLASH = register("slash");
   public static final SoundEvent FOUNTAIN_MAKE = register("snd_fountain_make");
   public static final SoundEvent FOUNTAIN_TARGET = register("snd_fountain_target");
   public static final SoundEvent CLIMB_NOISE = register("snd_noise");
   public static final SoundEvent CLIMB_STEP = register("snd_climb");
   public static final SoundEvent CHARGEJUMP_1 = register("snd_chargejump_1");
   public static final SoundEvent CHARGEJUMP_2 = register("snd_chargejump_2");
   public static final SoundEvent CHARGEJUMP_3 = register("snd_chargejump_3");
   public static final SoundEvent GREAT_SHINE = register("snd_great_shine");
   public static final SoundEvent USE_FOUNTAIN = register("snd_usefountain");
   public static final SoundEvent REVIVAL = register("snd_revival");
   public static final SoundEvent OMINOUS = register("snd_ominous");
   public static final SoundEvent OMINOUS_CANCEL = register("snd_ominous_cancel");
   public static final SoundEvent MUS_INTRONOISE = register("mus_intronoise");
   public static final SoundEvent MENU_MOVE = register("snd_menumove");
   public static final SoundEvent MENU_SELECT = register("snd_select");
   public static final SoundEvent SND_B = register("snd_b");
   public static final SoundEvent SND_CHESTOPEN = register("snd_chestopen");
   public static final SoundEvent SND_ITEM = register("snd_item");
   public static final SoundEvent SND_POWER = register("snd_power");
   public static final SoundEvent SND_LEVELUPMIX = register("snd_levelupmix");
   public static final SoundEvent SND_MERCYADD = register("snd_mercyadd");
   public static final SoundEvent SND_SAVE = register("snd_save");
   public static final SoundEvent SND_TEXT = register("snd_text");
   public static final SoundEvent MUS_FAINT_COURAGE = register("mus_faint_courage");
   public static final SoundEvent SND_SHOOT = register("snd_shoot");
   public static final SoundEvent SND_CHARGESHOT_CHARGE = register("snd_chargeshot_charge");
   public static final SoundEvent SND_CHARGESHOT_FIRE = register("snd_chargeshot_fire");

   private static SoundEvent register(String name) {
      Identifier id = Identifier.of("turnbattle", name);
      return (SoundEvent)Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
   }

   @Environment(EnvType.CLIENT)
   public static SoundEvent battleMusicFor(PlayerClass cls) {
      if (cls == null) {
         return MUS_BATTLE_MOB;
      } else {
         return switch (cls) {
            case PSI_USER -> MUS_BATTLE_PSI;
            case LIGHTNER -> MUS_BATTLE_LIGHTNER;
            case HUMAN_MAGE, SOUL_MAGE, MONSTER -> MUS_BATTLE_MOB;
            default -> MUS_BATTLE_MOB;
         };
      }
   }

   @Environment(EnvType.CLIENT)
   public static AbstractSoundInstance startBattleMusic(PlayerClass cls) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null) {
         return null;
      } else {
         SoundEvent evt = battleMusicFor(cls);
         PositionedSoundInstance inst = new PositionedSoundInstance(
            evt.getId(), SoundCategory.MUSIC, 1.0F, 1.0F, Random.create(), true, 0, AttenuationType.NONE, 0.0, 0.0, 0.0, true
         );
         mc.getSoundManager().play(inst);
         return inst;
      }
   }

   @Environment(EnvType.CLIENT)
   public static AbstractSoundInstance startFaintCourageMusic() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null) {
         return null;
      } else {
         PositionedSoundInstance inst = new PositionedSoundInstance(
            MUS_FAINT_COURAGE.getId(), SoundCategory.MUSIC, 1.0F, 1.0F, Random.create(), true, 0, AttenuationType.NONE, 0.0, 0.0, 0.0, true
         );
         mc.getSoundManager().play(inst);
         return inst;
      }
   }

   @Environment(EnvType.CLIENT)
   public static void stopBattleMusic(SoundInstance inst) {
      if (inst != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null) {
            mc.getSoundManager().stop(inst);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static void playSelect() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(MENU_SELECT, 1.0F));
      }
   }

   @Environment(EnvType.CLIENT)
   public static void playHover() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(MENU_MOVE, 0.6F));
      }
   }

   @Environment(EnvType.CLIENT)
   public static void playClient(SoundEvent evt, float volume, float pitch) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(evt, pitch, volume));
      }
   }

   public static void init() {
   }
}
