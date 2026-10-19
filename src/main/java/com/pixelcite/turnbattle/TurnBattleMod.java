package com.pixelcite.turnbattle;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AfterDeath;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDeath;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.CopyFrom;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.Load;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.Settings;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnBattleMod implements ModInitializer {
   public static final String MOD_ID = "turnbattle";
   public static final Logger LOGGER = LoggerFactory.getLogger("turnbattle");
   private static boolean eliteTeamsInitialized = false;
   private static final Random RNG = new Random();
   public static final Identifier BATTLE_START = Identifier.of("turnbattle", "battle_start");
   public static final Identifier PVP_BATTLE_START = Identifier.of("turnbattle", "pvp_battle_start");
   public static final Identifier BATTLE_STATE = Identifier.of("turnbattle", "battle_state");
   public static final Identifier ENEMY_TURN = Identifier.of("turnbattle", "enemy_turn");
   public static final Identifier SOUL_POS_SYNC = Identifier.of("turnbattle", "soul_pos_sync");
   public static final Identifier BATTLE_END = Identifier.of("turnbattle", "battle_end");
   public static final Identifier ALL_OUT_START = Identifier.of("turnbattle", "all_out_start");
   public static final Identifier ALL_OUT_VOTE = Identifier.of("turnbattle", "all_out_vote");
   public static final Identifier ALL_OUT_UPDATE = Identifier.of("turnbattle", "all_out_update");
   public static final Identifier ALL_OUT_EXEC = Identifier.of("turnbattle", "all_out_exec");
   public static final Identifier BATON_PASS_START = Identifier.of("turnbattle", "baton_pass_start");
   public static final Identifier BATON_PASS_CHOICE = Identifier.of("turnbattle", "baton_pass_choice");
   public static final Identifier LEVEL_UPDATE = Identifier.of("turnbattle", "level_update");
   public static final Identifier CLASS_SELECT = Identifier.of("turnbattle", "class_select");
   public static final Identifier SKILL_PLAY = Identifier.of("turnbattle", "skill_play");
   public static final Identifier BASH_PLAY = Identifier.of("turnbattle", "bash_play");
   public static final Identifier BASH_QTE_ROLL = Identifier.of("turnbattle", "bash_qte_roll");
   public static final Identifier BASH_QTE_START = Identifier.of("turnbattle", "bash_qte_start");
   public static final Identifier BASH_QTE_RESULT = Identifier.of("turnbattle", "bash_qte_result");
   public static final Identifier STATS_EXTRA = Identifier.of("turnbattle", "stats_extra");
   public static final Identifier BATTLE_LINE = Identifier.of("turnbattle", "battle_line");
   public static final Identifier BATTLE_LINE_SKIP = Identifier.of("turnbattle", "battle_line_skip");
   public static final Identifier BATTLE_LINE_INPUT = Identifier.of("turnbattle", "battle_line_input");
   public static final Identifier MULTI_BASH_START = Identifier.of("turnbattle", "multi_bash_start");
   public static final Identifier MULTI_BASH_BAR = Identifier.of("turnbattle", "multi_bash_bar");
   public static final Identifier MULTI_BASH_RESULT = Identifier.of("turnbattle", "multi_bash_result");
   public static final Identifier VESSEL_ANIM = Identifier.of("turnbattle", "vessel_anim");
   public static final Identifier CLONE_POSE = Identifier.of("turnbattle", "clone_pose");
   public static final Identifier OFFHAND_GIMMICK = Identifier.of("turnbattle", "offhand_gimmick");
   public static final Identifier SHIELD_PARRY = Identifier.of("turnbattle", "shield_parry");
   public static final Identifier SHIELD_PARRY_RESULT = Identifier.of("turnbattle", "shield_parry_result");
   public static final Identifier PLAYER_ACTION = Identifier.of("turnbattle", "player_action");
   public static final Identifier ACT_PERFORM = Identifier.of("turnbattle", "act_perform");
   public static final Identifier MERCY_SFX = Identifier.of("turnbattle", "mercy_sfx");
   public static final Identifier SPARE_THRESHOLD_UPDATE = Identifier.of("turnbattle", "spare_threshold_update");
   public static final Identifier ENEMY_TURN_RESULT = Identifier.of("turnbattle", "enemy_turn_result");
   public static final Identifier SOUL_HIT = Identifier.of("turnbattle", "soul_hit");
   public static final Identifier HP_SYNC = Identifier.of("turnbattle", "hp_sync");
   public static final Identifier HEAL_PLAY = Identifier.of("turnbattle", "heal_play");
   public static final Identifier OVERWORLD_HEAL = Identifier.of("turnbattle", "overworld_heal");
   public static final Identifier TP_SYNC = Identifier.of("turnbattle", "tp_sync");
   public static final Identifier CLASS_CHOSEN = Identifier.of("turnbattle", "class_chosen");
   public static final Identifier CHAR_CREATE_DONE = Identifier.of("turnbattle", "char_create_done");
   public static final Identifier SOUL_REMOVE_REQUEST = Identifier.of("turnbattle", "soul_remove_request");
   public static final Identifier SOUL_REMOVED = Identifier.of("turnbattle", "soul_removed");
   public static final Identifier SOUL_RESTORED = Identifier.of("turnbattle", "soul_restored");
   public static final Identifier ESTABLISHING_CONNECTION = Identifier.of("turnbattle", "establishing_connection");
   public static final Identifier FORCE_END = Identifier.of("turnbattle", "force_end");
   public static final Identifier PARTY_UPDATE = Identifier.of("turnbattle", "party_update");
   public static final Identifier PARTY_MEMBER_TURN = Identifier.of("turnbattle", "party_member_turn");
   public static final Identifier PARTICIPANT_UPDATE = Identifier.of("turnbattle", "participant_update");
   public static final Set<UUID> SKIP_INTRO_NEXT = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final Set<UUID> nearHazardPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final Map<UUID, Float> lastPlayerHp = new ConcurrentHashMap<>();
   private static final Map<UUID, BlockPos> soulRevealingPlayers = new ConcurrentHashMap<>();
   static final Set<UUID> sealingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
   public static final Identifier INTRO_START = Identifier.of("turnbattle", "intro_start");
   public static final Identifier VESSEL_NAME = Identifier.of("turnbattle", "vessel_name");
   public static final Identifier INTRO_DONE = Identifier.of("turnbattle", "intro_done");
   public static final Identifier GASTER_TRIGGER = Identifier.of("turnbattle", "gaster_trigger");
   public static final Identifier PARTY_INFO_REQUEST = Identifier.of("turnbattle", "party_info_request");
   public static final Identifier PARTY_INFO = Identifier.of("turnbattle", "party_info");
   public static final Identifier PARTY_SWITCH = Identifier.of("turnbattle", "party_switch");
   public static final Identifier STAT_SPEND = Identifier.of("turnbattle", "stat_spend");
   public static final Identifier STAT_RESET = Identifier.of("turnbattle", "stat_reset");
   public static final Identifier EQUIP_INFO_REQUEST = Identifier.of("turnbattle", "equip_info_request");
   public static final Identifier EQUIP_INFO = Identifier.of("turnbattle", "equip_info");
   public static final Identifier PLAYER_EQUIP_BROADCAST = Identifier.of("turnbattle", "player_equip_broadcast");
   public static final Identifier EQUIP_WEAPON = Identifier.of("turnbattle", "equip_weapon");
   public static final Identifier UNEQUIP_WEAPON = Identifier.of("turnbattle", "unequip_weapon");
   public static final Identifier PARTY_UNEQUIP_WEAPON = Identifier.of("turnbattle", "party_unequip_weapon");
   public static final Identifier RECRUIT_EQUIP_WEAPON = Identifier.of("turnbattle", "recruit_equip_weapon");
   public static final Identifier RECRUIT_UNEQUIP_WEAPON = Identifier.of("turnbattle", "recruit_unequip_weapon");
   public static final Identifier RECRUIT_EQUIP_ACCESSORY = Identifier.of("turnbattle", "recruit_equip_accessory");
   public static final Identifier RECRUIT_UNEQUIP_ACCESSORY = Identifier.of("turnbattle", "recruit_unequip_accessory");
   public static final Identifier RECRUIT_EQUIP_ARMOR = Identifier.of("turnbattle", "recruit_equip_armor");
   public static final Identifier RECRUIT_UNEQUIP_ARMOR = Identifier.of("turnbattle", "recruit_unequip_armor");
   public static final Identifier EQUIP_ACCESSORY = Identifier.of("turnbattle", "equip_accessory");
   public static final Identifier UNEQUIP_ACCESSORY = Identifier.of("turnbattle", "unequip_accessory");
   public static final Identifier ACCESSORY_INFO_REQUEST = Identifier.of("turnbattle", "accessory_info_request");
   public static final Identifier ACCESSORY_INFO = Identifier.of("turnbattle", "accessory_info");
   public static final Identifier BESTIARY_REQUEST = Identifier.of("turnbattle", "bestiary_request");
   public static final Identifier ITEM_FEED = Identifier.of("turnbattle", "item_feed");
   public static final Identifier FAVORITE_TOGGLE = Identifier.of("turnbattle", "favorite_toggle");
   public static final Identifier FAVORITE_SYNC = Identifier.of("turnbattle", "favorite_sync");
   public static final Identifier BESTIARY_DATA = Identifier.of("turnbattle", "bestiary_data");
   public static final Identifier SOUL_ROLL_REQUEST = Identifier.of("turnbattle", "soul_roll_request");
   public static final Identifier WEAPON_ROLL_REQUEST = Identifier.of("turnbattle", "weapon_roll_request");
   public static final Identifier ARMOR_INFO_REQUEST = Identifier.of("turnbattle", "armor_info_request");
   public static final Identifier ARMOR_INFO = Identifier.of("turnbattle", "armor_info");
   public static final Identifier EQUIP_ARMOR = Identifier.of("turnbattle", "equip_armor");
   public static final Identifier UNEQUIP_ARMOR = Identifier.of("turnbattle", "unequip_armor");
   public static final Identifier ARMOR_ROLL_REQUEST = Identifier.of("turnbattle", "armor_roll_request");
   public static final Identifier DIRECT_ENEMY_TURN = Identifier.of("turnbattle", "direct_enemy_turn");
   public static final Identifier DIRECT_ENEMY_TURN_RESULT = Identifier.of("turnbattle", "direct_enemy_turn_result");
   public static final Identifier SOUL_INFO = Identifier.of("turnbattle", "soul_info");
   public static final Identifier ROGUE_ENABLE = Identifier.of("turnbattle", "rogue_enable");
   public static final Identifier ROGUE_INFO_REQUEST = Identifier.of("turnbattle", "rogue_info_request");
   public static final Identifier ROGUE_INFO = Identifier.of("turnbattle", "rogue_info");
   public static final Identifier ROGUE_INVITE = Identifier.of("turnbattle", "rogue_invite");
   public static final Identifier ROGUE_BEGIN_RUN = Identifier.of("turnbattle", "rogue_begin_run");
   public static final Identifier ROGUE_OPEN = Identifier.of("turnbattle", "rogue_open");
   public static final Identifier ROGUE_TIMER = Identifier.of("turnbattle", "rogue_timer");
   public static final Identifier ROGUE_UPGRADE = Identifier.of("turnbattle", "rogue_upgrade");
   public static final Identifier ROGUE_CARD_DRAW = Identifier.of("turnbattle", "rogue_card_draw");
   public static final Identifier ROGUE_CARD_CHOSEN = Identifier.of("turnbattle", "rogue_card_chosen");
   public static final Identifier ROGUE_SKILLS = Identifier.of("turnbattle", "rogue_skills");
   public static final Identifier ROGUE_LEAVE_PARTY = Identifier.of("turnbattle", "rogue_leave_party");
   public static final Identifier ROGUE_ABANDON = Identifier.of("turnbattle", "rogue_abandon");
   public static final Identifier SKILL_UNLOCK = Identifier.of("turnbattle", "skill_unlock");
   public static final Identifier PARTY_SKILL_UNLOCK = Identifier.of("turnbattle", "party_skill_unlock");
   public static final Identifier SKILL_INFO = Identifier.of("turnbattle", "skill_info");
   public static final Identifier SKILL_EQUIP_TOGGLE = Identifier.of("turnbattle", "skill_equip_toggle");
   public static final Identifier SKILL_INFO_REQUEST = Identifier.of("turnbattle", "skill_info_request");
   public static final Identifier WORLD_TYPE_CHOSEN = Identifier.of("turnbattle", "world_type_chosen");
   public static final Identifier WORLD_TYPE_OPEN = Identifier.of("turnbattle", "world_type_open");
   public static final Identifier BOSS_BEACON_SYNC = Identifier.of("turnbattle", "boss_beacon_sync");
   public static final Identifier BOSS_BEACON_SET_DIFFICULTY = Identifier.of("turnbattle", "boss_beacon_diff");
   public static final Identifier BOSS_BEACON_BEGIN = Identifier.of("turnbattle", "boss_beacon_begin");
   public static final Identifier BOSS_BEACON_LEAVE = Identifier.of("turnbattle", "boss_beacon_leave");
   public static final Identifier DARK_FOUNTAIN_PROMPT = Identifier.of("turnbattle", "dark_fountain_prompt");
   public static final Identifier DARK_FOUNTAIN_CONFIRM = Identifier.of("turnbattle", "dark_fountain_confirm");
   public static final Identifier DARK_FOUNTAIN_CANCEL = Identifier.of("turnbattle", "dark_fountain_cancel");
   public static final Identifier DARK_FOUNTAIN_FADE = Identifier.of("turnbattle", "dark_fountain_fade");
   public static final Identifier DARK_FOUNTAIN_RESET = Identifier.of("turnbattle", "dark_fountain_reset");
   public static final Identifier DARK_FOUNTAIN_SEALS = Identifier.of("turnbattle", "dark_fountain_seals");
   public static final Identifier DARK_FOUNTAIN_ENCOUNTER = Identifier.of("turnbattle", "dark_fountain_encounter");
   public static final Identifier DARK_WORLD_CREATE_MENU = Identifier.of("turnbattle", "dark_world_create_menu");
   public static final Identifier DARK_FOUNTAIN_SITES = Identifier.of("turnbattle", "dark_fountain_sites");
   public static final Identifier DARK_WORLD_CREATE_CONFIRM = Identifier.of("turnbattle", "dark_world_create_confirm");
   public static final Identifier DARK_FOUNTAIN_SEAL_PROMPT = Identifier.of("turnbattle", "dark_fountain_seal_prompt");
   public static final Identifier DARK_FOUNTAIN_SEAL_CONFIRM = Identifier.of("turnbattle", "dark_fountain_seal_confirm");
   public static final Identifier DARK_FOUNTAIN_SEAL_START = Identifier.of("turnbattle", "dark_fountain_seal_start");
   public static final Identifier DARK_FOUNTAIN_CREATE_SPARKLE = Identifier.of("turnbattle", "dark_fountain_create_sparkle");
   public static final Identifier PLAYER_ANIM = Identifier.of("turnbattle", "player_anim");
   public static final Identifier SOUL_RIP_STATE = Identifier.of("turnbattle", "soul_rip_state");
   public static final Identifier BATTLE_PREFS = Identifier.of("turnbattle", "battle_prefs");
   public static final Identifier MENU_STATE = Identifier.of("turnbattle", "menu_state");
   public static final Identifier DARK_WORLD_LEAVE = Identifier.of("turnbattle", "dark_world_leave");
   public static final Identifier WALL_CLIMB_INPUT = Identifier.of("turnbattle", "wall_climb_input");
   public static final Identifier WALL_CLIMB_STATE = Identifier.of("turnbattle", "wall_climb_state");
   public static final Identifier WALL_CLIMB_ANIM = Identifier.of("turnbattle", "wall_climb_anim");
   public static final Identifier WALL_CLIMB_TELEGRAPH = Identifier.of("turnbattle", "wall_climb_telegraph");
   public static final Identifier WALL_CLIMB_STEPPING = Identifier.of("turnbattle", "wall_climb_stepping");
   public static final Identifier MORALITY_EVENT = Identifier.of("turnbattle", "morality_event");
   public static final Identifier STORY_STATE = Identifier.of("turnbattle", "story_state");
   public static final Identifier STORY_BEGIN = Identifier.of("turnbattle", "story_begin");
   public static final Identifier STORY_CONTINUE = Identifier.of("turnbattle", "story_continue");
   public static final Identifier STORY_DIALOGUE = Identifier.of("turnbattle", "story_dialogue");
   public static final Identifier STORY_DIALOGUE_DONE = Identifier.of("turnbattle", "story_dialogue_done");
   public static final Identifier STORY_CHOICE = Identifier.of("turnbattle", "story_choice");
   public static final Identifier TEXTBOX_SHOW = Identifier.of("turnbattle", "textbox_show");
   public static final Identifier PROXIMITY_CHAT_TOGGLE = Identifier.of("turnbattle", "proximity_chat_toggle");
   public static final Identifier PROXIMITY_CHAT_SEND = Identifier.of("turnbattle", "proximity_chat_send");
   public static final Identifier PROXIMITY_CHAT_SHOW = Identifier.of("turnbattle", "proximity_chat_show");
   public static final Set<UUID> proximityChatDisabled = ConcurrentHashMap.newKeySet();
   private static final double PROXIMITY_CHAT_RANGE_SQ = 10000.0;
   public static final Identifier RECRUIT_ACTIVATE = Identifier.of("turnbattle", "recruit_activate");
   public static final Identifier STORY_INSPECT = Identifier.of("turnbattle", "story_inspect");
   public static final Identifier VESSEL_SET = Identifier.of("turnbattle", "vessel_set");
   public static final Identifier VESSEL_INFO = Identifier.of("turnbattle", "vessel_info");
   public static final Identifier SOUL_GLOW = Identifier.of("turnbattle", "soul_glow");
   public static final Identifier SOUL_REVEAL = Identifier.of("turnbattle", "soul_reveal");
   public static final Identifier SAVE_POINT_USE = Identifier.of("turnbattle", "save_point_use");
   public static final Identifier SAVE_POINT_ACK = Identifier.of("turnbattle", "save_point_ack");
   public static final Identifier SAVE_POINT_OPEN = Identifier.of("turnbattle", "save_point_open");
   public static final Identifier SAVE_POINT_PLACE = Identifier.of("turnbattle", "save_point_place");
   public static final Identifier SAVE_POINT_DELETE = Identifier.of("turnbattle", "save_point_delete");
   public static EntityType<SavePointEntity> SAVE_POINT_ENTITY_TYPE;
   public static EntityType<BattleRecruitEntity> BATTLE_RECRUIT_ENTITY_TYPE;
   public static final double VFX_RANGE = 30.0;
   public static final Identifier WORLD_BATTLE_EVENT = Identifier.of("turnbattle", "world_battle_event");
   public static final Identifier WORLD_DAMAGE_HUD = Identifier.of("turnbattle", "world_damage_hud");
   public static final Identifier PLAYER_PARTY_STATE = Identifier.of("turnbattle", "player_party_state");
   public static final Identifier PARTY_MEMBER_DAMAGE = Identifier.of("turnbattle", "party_member_damage");
   public static final Identifier PARTY_INVITE_TOGGLE = Identifier.of("turnbattle", "party_invite_toggle");
   public static final Identifier PARTY_INVITE = Identifier.of("turnbattle", "party_invite_send");
   public static final Identifier PARTY_LEAVE = Identifier.of("turnbattle", "party_leave");
   public static final Identifier PARTY_INVITE_NOTIFY = Identifier.of("turnbattle", "party_invite_notify");
   public static final Identifier PARTY_INVITE_ACCEPT = Identifier.of("turnbattle", "party_invite_accept");
   public static final Identifier PARTY_INVITE_DECLINE = Identifier.of("turnbattle", "party_invite_decline");
   public static final Identifier BATTLE_CHAT = Identifier.of("turnbattle", "battle_chat");
   public static final Identifier BATTLE_CHAT_MSG = Identifier.of("turnbattle", "battle_chat_msg");
   public static final Identifier DD_ENTER = Identifier.of("turnbattle", "dd_enter");
   public static final Identifier DD_EXIT = Identifier.of("turnbattle", "dd_exit");
   public static final Identifier DD_ROOM_DATA = Identifier.of("turnbattle", "dd_room_data");
   public static final Identifier DD_PLAYER_MOVE = Identifier.of("turnbattle", "dd_player_move");
   public static final Identifier DD_ENCOUNTER = Identifier.of("turnbattle", "dd_encounter");
   public static final Identifier DD_ENCOUNTER_DONE = Identifier.of("turnbattle", "dd_enc_done");
   public static final Identifier DD_CHEST_OPEN = Identifier.of("turnbattle", "dd_chest_open");
   public static final Identifier DD_CHEST_RESULT = Identifier.of("turnbattle", "dd_chest_result");
   public static final Identifier DD_STAIRS_INTERACT = Identifier.of("turnbattle", "dd_stairs");
   public static final Identifier DD_STAIRS_VOTE = Identifier.of("turnbattle", "dd_stairs_vote");
   public static final Identifier DD_STAIRS_PROMPT = Identifier.of("turnbattle", "dd_stairs_prompt");
   public static final Identifier DD_NEXT_FLOOR = Identifier.of("turnbattle", "dd_next_floor");
   public static final Identifier DD_PLAYERS_SYNC = Identifier.of("turnbattle", "dd_players_sync");
   public static final Identifier DD_PLAYER_POS = Identifier.of("turnbattle", "dd_player_pos");
   public static final Identifier DD_TIMER_SYNC = Identifier.of("turnbattle", "dd_timer_sync");
   public static final Identifier DD_ENEMIES_SYNC = Identifier.of("turnbattle", "dd_enemies_sync");
   public static final Identifier RECRUIT_INFO = Identifier.of("turnbattle", "recruit_info");
   public static final Identifier SET_SECOND_RECRUIT = Identifier.of("turnbattle", "set_second_recruit");
   public static final Identifier CREATE_VESSEL = Identifier.of("turnbattle", "create_vessel");
   public static final Identifier REMOVE_VESSEL = Identifier.of("turnbattle", "remove_vessel");
   public static final Identifier RECRUIT_SET_ACTIVE = Identifier.of("turnbattle", "recruit_set_active");
   public static final Identifier RECRUIT_STAT_SPEND = Identifier.of("turnbattle", "recruit_stat_spend");
   public static final Identifier RECRUIT_SKILL_UNLOCK = Identifier.of("turnbattle", "recruit_skill_unlock");
   public static final Identifier QUESTLINE_STATE = Identifier.of("turnbattle", "questline_state");
   public static final Identifier QUESTLINE_START = Identifier.of("turnbattle", "questline_start");
   public static final Identifier QUESTLINE_RETRY = Identifier.of("turnbattle", "questline_retry");
   public static Item THORN_RING;
   public static final Map<SoulType, SoulItem> SOUL_ITEMS = new EnumMap<>(SoulType.class);
   public static Item PSI_PROCESSOR;
   public static Item HEART_LOCKET;
   public static Item SOUL_RESONATOR;
   public static DarkFountainKnife DARK_FOUNTAIN_KNIFE;
   public static DarkFountainBlock DARK_FOUNTAIN_BLOCK;
   public static BlockEntityType<DarkFountainBlockEntity> DARK_FOUNTAIN_BLOCK_ENTITY_TYPE;
   public static DarkBlock DARK_BLOCK;
   public static MannequinItem MANNEQUIN_ITEM;
   public static SavePointItem SAVE_POINT_ITEM;
   public static ClimbClawsItem CLIMB_CLAWS;
   private static final String[] WEAPON_STAT_NAMES = new String[]{
      "attack_damage", "attack_speed", "max_health", "armor", "armor_toughness", "movement_speed", "knockback_resistance", "luck"
   };

   private static void placeSoulLight(ServerPlayerEntity player) {
      BlockPos pos = player.getBlockPos();
      ServerWorld world = player.getServerWorld();
      BlockState existing = world.getBlockState(pos);
      if (existing.isAir()) {
         world.setBlockState(pos, (BlockState)Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 12), 3);
      }
   }

   private static void removeSoulLight(ServerPlayerEntity player) {
      removeSoulLightAt(player.getServerWorld(), player.getBlockPos());
   }

   private static void removeSoulLightAt(ServerWorld world, BlockPos pos) {
      if (world.getBlockState(pos).isOf(Blocks.LIGHT)) {
         world.removeBlock(pos, false);
      }
   }

   public static boolean isSealing(UUID uuid) {
      return uuid != null && sealingPlayers.contains(uuid);
   }

   public static boolean isUntouchable(UUID uuid) {
      return isSealing(uuid) || SoulWalk.isOut(uuid);
   }

   static void forceRevealOff(MinecraftServer server, ServerPlayerEntity player) {
      UUID uid = player.getUuid();
      if (nearHazardPlayers.remove(uid)) {
         setSoulGlow(server, player, null, false);
      }

      BlockPos trackedPos = soulRevealingPlayers.remove(uid);
      if (trackedPos != null) {
         removeSoulLightAt(player.getServerWorld(), trackedPos);
      } else {
         removeSoulLight(player);
      }
   }

   public void onInitialize() {
      DarkWorldLevelDatCleaner.tryCleanupDedicated();
      DarkWorldSkins.register();
      DarkWorldExit.register();
      LiveCombat.register();
      MobFateNet.register();
      ServerLifecycleEvents.SERVER_STARTING.register(DarkWorldLevelDatCleaner::tryCleanup);
      ServerLifecycleEvents.SERVER_STARTING.register((ServerStarting)s -> LiveCombat.reset());
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)s -> LiveCombat.reset());
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)s -> {
         sealingPlayers.clear();
         DarkFountainManager.onServerStopped();
      });
      ServerLifecycleEvents.SERVER_STARTED.register(DarkWorldGenerator::reopenAllOnStartup);
      LOGGER.info("[TurnBattle] Initialising.");
      UseBlockCallback.EVENT.register((UseBlockCallback)(player, world, hand, hitResult) -> {
         if (world.isClient || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
         } else if (player instanceof ServerPlayerEntity sp) {
            return WallClimbManager.tryGrab(sp, hitResult) ? ActionResult.SUCCESS : ActionResult.PASS;
         } else {
            return ActionResult.PASS;
         }
      });
      UseBlockCallback.EVENT.register((UseBlockCallback)(player, world, hand, hitResult) -> {
         if (world.isClient || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
         } else if (player instanceof ServerPlayerEntity sp) {
            BlockPos var5x = hitResult.getBlockPos();
            if (!world.getBlockState(var5x).isOf(DARK_BLOCK)) {
               return ActionResult.PASS;
            } else {
               return DarkFountainManager.tryForceEnter(sp, var5x) ? ActionResult.SUCCESS : ActionResult.PASS;
            }
         } else {
            return ActionResult.PASS;
         }
      });
      SAVE_POINT_ENTITY_TYPE = (EntityType<SavePointEntity>)Registry.register(
         Registries.ENTITY_TYPE,
         Identifier.of("turnbattle", "save_point"),
         Builder.create(SavePointEntity::new, SpawnGroup.MISC).dimensions(0.01F, 0.01F).maxTrackingRange(64).build()
      );
      SoulWalk.register();
      BATTLE_RECRUIT_ENTITY_TYPE = (EntityType<BattleRecruitEntity>)Registry.register(
         Registries.ENTITY_TYPE,
         Identifier.of("turnbattle", "battle_recruit"),
         Builder.create(BattleRecruitEntity::new, SpawnGroup.MISC).dimensions(0.6F, 1.8F).maxTrackingRange(10).disableSaving().build()
      );
      FabricDefaultAttributeRegistry.register(BATTLE_RECRUIT_ENTITY_TYPE, MobEntity.createMobAttributes());
      Shopkeeper.register();
      Darkners.register();
      DarknerBosses.register();
      Net121.registerC2SChannel(SAVE_POINT_USE);
      Net121.registerS2CChannel(SAVE_POINT_ACK);
      Net121.registerS2CChannel(SAVE_POINT_OPEN);
      Net121.registerC2SChannel(SAVE_POINT_PLACE);
      Net121.registerC2SChannel(SAVE_POINT_DELETE);
      Net121.registerS2CChannel(BATTLE_START);
      Net121.registerC2SChannel(BATTLE_START);
      Net121.registerS2CChannel(PVP_BATTLE_START);
      Net121.registerC2SChannel(PVP_BATTLE_START);
      Net121.registerS2CChannel(BATTLE_STATE);
      Net121.registerC2SChannel(BATTLE_STATE);
      Net121.registerS2CChannel(ENEMY_TURN);
      Net121.registerC2SChannel(ENEMY_TURN);
      Net121.registerS2CChannel(SOUL_POS_SYNC);
      Net121.registerC2SChannel(SOUL_POS_SYNC);
      Net121.registerS2CChannel(BATTLE_END);
      Net121.registerC2SChannel(BATTLE_END);
      Net121.registerS2CChannel(ALL_OUT_START);
      Net121.registerC2SChannel(ALL_OUT_START);
      Net121.registerS2CChannel(ALL_OUT_VOTE);
      Net121.registerC2SChannel(ALL_OUT_VOTE);
      Net121.registerS2CChannel(ALL_OUT_UPDATE);
      Net121.registerC2SChannel(ALL_OUT_UPDATE);
      Net121.registerS2CChannel(ALL_OUT_EXEC);
      Net121.registerC2SChannel(ALL_OUT_EXEC);
      Net121.registerS2CChannel(BATON_PASS_START);
      Net121.registerC2SChannel(BATON_PASS_START);
      Net121.registerS2CChannel(BATON_PASS_CHOICE);
      Net121.registerC2SChannel(BATON_PASS_CHOICE);
      Net121.registerS2CChannel(LEVEL_UPDATE);
      Net121.registerC2SChannel(LEVEL_UPDATE);
      Net121.registerS2CChannel(CLASS_SELECT);
      Net121.registerC2SChannel(CLASS_SELECT);
      Net121.registerS2CChannel(SKILL_PLAY);
      Net121.registerC2SChannel(SKILL_PLAY);
      Net121.registerS2CChannel(BASH_PLAY);
      Net121.registerC2SChannel(BASH_PLAY);
      Net121.registerC2SChannel(BASH_QTE_ROLL);
      Net121.registerS2CChannel(BASH_QTE_START);
      Net121.registerC2SChannel(BASH_QTE_RESULT);
      Net121.registerS2CChannel(BATTLE_LINE);
      Net121.registerS2CChannel(STATS_EXTRA);
      Net121.registerS2CChannel(BATTLE_LINE_SKIP);
      Net121.registerC2SChannel(BATTLE_LINE_INPUT);
      Net121.registerS2CChannel(MULTI_BASH_START);
      Net121.registerS2CChannel(MULTI_BASH_BAR);
      Net121.registerC2SChannel(MULTI_BASH_RESULT);
      Net121.registerS2CChannel(VESSEL_ANIM);
      Net121.registerS2CChannel(WORLD_BATTLE_EVENT);
      Net121.registerC2SChannel(VESSEL_ANIM);
      Net121.registerS2CChannel(CLONE_POSE);
      Net121.registerC2SChannel(CLONE_POSE);
      Net121.registerS2CChannel(OFFHAND_GIMMICK);
      Net121.registerC2SChannel(OFFHAND_GIMMICK);
      Net121.registerS2CChannel(SHIELD_PARRY);
      Net121.registerC2SChannel(SHIELD_PARRY);
      Net121.registerS2CChannel(SHIELD_PARRY_RESULT);
      Net121.registerC2SChannel(SHIELD_PARRY_RESULT);
      Net121.registerS2CChannel(PLAYER_ACTION);
      Net121.registerC2SChannel(PLAYER_ACTION);
      Net121.registerS2CChannel(ACT_PERFORM);
      Net121.registerC2SChannel(ACT_PERFORM);
      Net121.registerS2CChannel(MERCY_SFX);
      Net121.registerC2SChannel(MERCY_SFX);
      Net121.registerS2CChannel(SPARE_THRESHOLD_UPDATE);
      Net121.registerC2SChannel(SPARE_THRESHOLD_UPDATE);
      Net121.registerS2CChannel(ENEMY_TURN_RESULT);
      Net121.registerC2SChannel(ENEMY_TURN_RESULT);
      Net121.registerS2CChannel(SOUL_HIT);
      Net121.registerC2SChannel(SOUL_HIT);
      Net121.registerS2CChannel(HP_SYNC);
      Net121.registerC2SChannel(HP_SYNC);
      Net121.registerS2CChannel(HEAL_PLAY);
      Net121.registerC2SChannel(HEAL_PLAY);
      Net121.registerS2CChannel(OVERWORLD_HEAL);
      Net121.registerC2SChannel(OVERWORLD_HEAL);
      Net121.registerS2CChannel(TP_SYNC);
      Net121.registerC2SChannel(TP_SYNC);
      Net121.registerS2CChannel(CLASS_CHOSEN);
      Net121.registerC2SChannel(CLASS_CHOSEN);
      Net121.registerS2CChannel(CHAR_CREATE_DONE);
      Net121.registerC2SChannel(CHAR_CREATE_DONE);
      Net121.registerS2CChannel(SOUL_REMOVE_REQUEST);
      Net121.registerC2SChannel(SOUL_REMOVE_REQUEST);
      Net121.registerS2CChannel(SOUL_REMOVED);
      Net121.registerC2SChannel(SOUL_REMOVED);
      Net121.registerS2CChannel(SOUL_RESTORED);
      Net121.registerC2SChannel(SOUL_RESTORED);
      Net121.registerS2CChannel(ESTABLISHING_CONNECTION);
      Net121.registerC2SChannel(ESTABLISHING_CONNECTION);
      Net121.registerS2CChannel(FORCE_END);
      Net121.registerC2SChannel(FORCE_END);
      Net121.registerS2CChannel(PARTY_UPDATE);
      Net121.registerC2SChannel(PARTY_UPDATE);
      Net121.registerS2CChannel(PARTY_MEMBER_TURN);
      Net121.registerC2SChannel(PARTY_MEMBER_TURN);
      Net121.registerS2CChannel(PARTICIPANT_UPDATE);
      Net121.registerC2SChannel(PARTICIPANT_UPDATE);
      Net121.registerS2CChannel(INTRO_START);
      Net121.registerC2SChannel(INTRO_START);
      Net121.registerS2CChannel(VESSEL_NAME);
      Net121.registerC2SChannel(VESSEL_NAME);
      Net121.registerS2CChannel(INTRO_DONE);
      Net121.registerC2SChannel(INTRO_DONE);
      Net121.registerS2CChannel(GASTER_TRIGGER);
      Net121.registerC2SChannel(GASTER_TRIGGER);
      Net121.registerS2CChannel(PARTY_INFO_REQUEST);
      Net121.registerC2SChannel(PARTY_INFO_REQUEST);
      Net121.registerS2CChannel(PARTY_INFO);
      Net121.registerC2SChannel(PARTY_INFO);
      Net121.registerS2CChannel(PARTY_SWITCH);
      Net121.registerC2SChannel(PARTY_SWITCH);
      Net121.registerS2CChannel(STAT_SPEND);
      Net121.registerC2SChannel(STAT_SPEND);
      Net121.registerS2CChannel(STAT_RESET);
      Net121.registerC2SChannel(STAT_RESET);
      Net121.registerS2CChannel(EQUIP_INFO_REQUEST);
      Net121.registerC2SChannel(EQUIP_INFO_REQUEST);
      Net121.registerS2CChannel(EQUIP_INFO);
      Net121.registerC2SChannel(EQUIP_INFO);
      Net121.registerS2CChannel(PLAYER_EQUIP_BROADCAST);
      Net121.registerS2CChannel(EQUIP_WEAPON);
      Net121.registerC2SChannel(EQUIP_WEAPON);
      Net121.registerS2CChannel(UNEQUIP_WEAPON);
      Net121.registerC2SChannel(UNEQUIP_WEAPON);
      Net121.registerS2CChannel(PARTY_UNEQUIP_WEAPON);
      Net121.registerC2SChannel(PARTY_UNEQUIP_WEAPON);
      Net121.registerS2CChannel(RECRUIT_EQUIP_WEAPON);
      Net121.registerC2SChannel(RECRUIT_EQUIP_WEAPON);
      Net121.registerS2CChannel(RECRUIT_UNEQUIP_WEAPON);
      Net121.registerC2SChannel(RECRUIT_UNEQUIP_WEAPON);
      Net121.registerS2CChannel(RECRUIT_EQUIP_ACCESSORY);
      Net121.registerC2SChannel(RECRUIT_EQUIP_ACCESSORY);
      Net121.registerS2CChannel(RECRUIT_UNEQUIP_ACCESSORY);
      Net121.registerC2SChannel(RECRUIT_UNEQUIP_ACCESSORY);
      Net121.registerS2CChannel(RECRUIT_EQUIP_ARMOR);
      Net121.registerC2SChannel(RECRUIT_EQUIP_ARMOR);
      Net121.registerS2CChannel(RECRUIT_UNEQUIP_ARMOR);
      Net121.registerC2SChannel(RECRUIT_UNEQUIP_ARMOR);
      Net121.registerS2CChannel(EQUIP_ACCESSORY);
      Net121.registerC2SChannel(EQUIP_ACCESSORY);
      Net121.registerS2CChannel(UNEQUIP_ACCESSORY);
      Net121.registerC2SChannel(UNEQUIP_ACCESSORY);
      Net121.registerS2CChannel(ACCESSORY_INFO_REQUEST);
      Net121.registerC2SChannel(ACCESSORY_INFO_REQUEST);
      Net121.registerS2CChannel(ACCESSORY_INFO);
      Net121.registerC2SChannel(ACCESSORY_INFO);
      Net121.registerS2CChannel(BESTIARY_REQUEST);
      Net121.registerC2SChannel(BESTIARY_REQUEST);
      Net121.registerS2CChannel(ITEM_FEED);
      Net121.registerC2SChannel(ITEM_FEED);
      Net121.registerS2CChannel(FAVORITE_TOGGLE);
      Net121.registerC2SChannel(FAVORITE_TOGGLE);
      Net121.registerS2CChannel(FAVORITE_SYNC);
      Net121.registerC2SChannel(FAVORITE_SYNC);
      Net121.registerS2CChannel(BESTIARY_DATA);
      Net121.registerC2SChannel(BESTIARY_DATA);
      Net121.registerS2CChannel(SOUL_ROLL_REQUEST);
      Net121.registerC2SChannel(SOUL_ROLL_REQUEST);
      Net121.registerS2CChannel(WEAPON_ROLL_REQUEST);
      Net121.registerC2SChannel(WEAPON_ROLL_REQUEST);
      Net121.registerS2CChannel(ARMOR_INFO_REQUEST);
      Net121.registerC2SChannel(ARMOR_INFO_REQUEST);
      Net121.registerS2CChannel(ARMOR_INFO);
      Net121.registerC2SChannel(ARMOR_INFO);
      Net121.registerS2CChannel(EQUIP_ARMOR);
      Net121.registerC2SChannel(EQUIP_ARMOR);
      Net121.registerS2CChannel(UNEQUIP_ARMOR);
      Net121.registerC2SChannel(UNEQUIP_ARMOR);
      Net121.registerS2CChannel(ARMOR_ROLL_REQUEST);
      Net121.registerC2SChannel(ARMOR_ROLL_REQUEST);
      Net121.registerS2CChannel(DIRECT_ENEMY_TURN);
      Net121.registerC2SChannel(DIRECT_ENEMY_TURN);
      Net121.registerS2CChannel(DIRECT_ENEMY_TURN_RESULT);
      Net121.registerC2SChannel(DIRECT_ENEMY_TURN_RESULT);
      Net121.registerS2CChannel(SOUL_INFO);
      Net121.registerC2SChannel(SOUL_INFO);
      Net121.registerS2CChannel(ROGUE_ENABLE);
      Net121.registerC2SChannel(ROGUE_ENABLE);
      Net121.registerS2CChannel(ROGUE_INFO_REQUEST);
      Net121.registerC2SChannel(ROGUE_INFO_REQUEST);
      Net121.registerS2CChannel(ROGUE_INFO);
      Net121.registerC2SChannel(ROGUE_INFO);
      Net121.registerS2CChannel(ROGUE_INVITE);
      Net121.registerC2SChannel(ROGUE_INVITE);
      Net121.registerS2CChannel(ROGUE_BEGIN_RUN);
      Net121.registerC2SChannel(ROGUE_BEGIN_RUN);
      Net121.registerS2CChannel(ROGUE_OPEN);
      Net121.registerC2SChannel(ROGUE_OPEN);
      Net121.registerS2CChannel(ROGUE_TIMER);
      Net121.registerC2SChannel(ROGUE_TIMER);
      Net121.registerS2CChannel(ROGUE_UPGRADE);
      Net121.registerC2SChannel(ROGUE_UPGRADE);
      Net121.registerS2CChannel(ROGUE_CARD_DRAW);
      Net121.registerC2SChannel(ROGUE_CARD_DRAW);
      Net121.registerS2CChannel(ROGUE_CARD_CHOSEN);
      Net121.registerC2SChannel(ROGUE_CARD_CHOSEN);
      Net121.registerS2CChannel(ROGUE_SKILLS);
      Net121.registerC2SChannel(ROGUE_SKILLS);
      Net121.registerS2CChannel(ROGUE_LEAVE_PARTY);
      Net121.registerC2SChannel(ROGUE_LEAVE_PARTY);
      Net121.registerS2CChannel(ROGUE_ABANDON);
      Net121.registerC2SChannel(ROGUE_ABANDON);
      Net121.registerS2CChannel(SKILL_UNLOCK);
      Net121.registerC2SChannel(SKILL_UNLOCK);
      Net121.registerS2CChannel(PARTY_SKILL_UNLOCK);
      Net121.registerC2SChannel(PARTY_SKILL_UNLOCK);
      Net121.registerS2CChannel(SKILL_INFO);
      Net121.registerC2SChannel(SKILL_INFO);
      Net121.registerS2CChannel(SKILL_EQUIP_TOGGLE);
      Net121.registerC2SChannel(SKILL_EQUIP_TOGGLE);
      Net121.registerS2CChannel(SKILL_INFO_REQUEST);
      Net121.registerC2SChannel(SKILL_INFO_REQUEST);
      Net121.registerS2CChannel(WORLD_TYPE_CHOSEN);
      Net121.registerC2SChannel(WORLD_TYPE_CHOSEN);
      Net121.registerS2CChannel(WORLD_TYPE_OPEN);
      Net121.registerC2SChannel(WORLD_TYPE_OPEN);
      Net121.registerS2CChannel(BOSS_BEACON_SYNC);
      Net121.registerC2SChannel(BOSS_BEACON_SYNC);
      Net121.registerS2CChannel(BOSS_BEACON_SET_DIFFICULTY);
      Net121.registerC2SChannel(BOSS_BEACON_SET_DIFFICULTY);
      Net121.registerS2CChannel(BOSS_BEACON_BEGIN);
      Net121.registerC2SChannel(BOSS_BEACON_BEGIN);
      Net121.registerS2CChannel(BOSS_BEACON_LEAVE);
      Net121.registerC2SChannel(BOSS_BEACON_LEAVE);
      Net121.registerS2CChannel(DARK_FOUNTAIN_PROMPT);
      Net121.registerC2SChannel(DARK_FOUNTAIN_PROMPT);
      Net121.registerS2CChannel(DARK_FOUNTAIN_CONFIRM);
      Net121.registerC2SChannel(DARK_FOUNTAIN_CONFIRM);
      Net121.registerS2CChannel(DARK_FOUNTAIN_CANCEL);
      Net121.registerC2SChannel(DARK_FOUNTAIN_CANCEL);
      Net121.registerS2CChannel(DARK_FOUNTAIN_FADE);
      Net121.registerC2SChannel(DARK_FOUNTAIN_FADE);
      Net121.registerS2CChannel(DARK_FOUNTAIN_RESET);
      Net121.registerC2SChannel(DARK_FOUNTAIN_RESET);
      Net121.registerS2CChannel(DARK_FOUNTAIN_SEALS);
      Net121.registerC2SChannel(DARK_FOUNTAIN_SEALS);
      Net121.registerS2CChannel(DARK_FOUNTAIN_ENCOUNTER);
      Net121.registerC2SChannel(DARK_FOUNTAIN_ENCOUNTER);
      Net121.registerS2CChannel(DARK_FOUNTAIN_SITES);
      Net121.registerC2SChannel(DARK_FOUNTAIN_SITES);
      Net121.registerS2CChannel(DARK_WORLD_CREATE_MENU);
      Net121.registerC2SChannel(DARK_WORLD_CREATE_MENU);
      Net121.registerS2CChannel(DARK_WORLD_CREATE_CONFIRM);
      Net121.registerC2SChannel(DARK_WORLD_CREATE_CONFIRM);
      Net121.registerS2CChannel(DARK_FOUNTAIN_SEAL_PROMPT);
      Net121.registerC2SChannel(DARK_FOUNTAIN_SEAL_PROMPT);
      Net121.registerS2CChannel(DARK_FOUNTAIN_SEAL_CONFIRM);
      Net121.registerC2SChannel(DARK_FOUNTAIN_SEAL_CONFIRM);
      Net121.registerS2CChannel(DARK_FOUNTAIN_SEAL_START);
      Net121.registerC2SChannel(DARK_FOUNTAIN_SEAL_START);
      Net121.registerS2CChannel(DARK_FOUNTAIN_CREATE_SPARKLE);
      Net121.registerC2SChannel(DARK_FOUNTAIN_CREATE_SPARKLE);
      Net121.registerS2CChannel(DARK_WORLD_LEAVE);
      Net121.registerC2SChannel(DARK_WORLD_LEAVE);
      Net121.registerS2CChannel(PLAYER_ANIM);
      Net121.registerC2SChannel(PLAYER_ANIM);
      Net121.registerS2CChannel(SOUL_RIP_STATE);
      Net121.registerC2SChannel(SOUL_RIP_STATE);
      Net121.registerC2SChannel(BATTLE_PREFS);
      Net121.registerS2CChannel(BATTLE_PREFS);
      ServerSettings.registerChannels();
      Net121.registerS2CChannel(MENU_STATE);
      Net121.registerC2SChannel(MENU_STATE);
      Net121.registerS2CChannel(WALL_CLIMB_INPUT);
      Net121.registerC2SChannel(WALL_CLIMB_INPUT);
      Net121.registerS2CChannel(WALL_CLIMB_STATE);
      Net121.registerS2CChannel(WALL_CLIMB_ANIM);
      Net121.registerC2SChannel(WALL_CLIMB_ANIM);
      Net121.registerC2SChannel(WALL_CLIMB_STATE);
      Net121.registerS2CChannel(WALL_CLIMB_TELEGRAPH);
      Net121.registerC2SChannel(WALL_CLIMB_TELEGRAPH);
      Net121.registerS2CChannel(WALL_CLIMB_STEPPING);
      Net121.registerC2SChannel(WALL_CLIMB_STEPPING);
      Net121.registerS2CChannel(MORALITY_EVENT);
      Net121.registerC2SChannel(MORALITY_EVENT);
      Net121.registerS2CChannel(STORY_STATE);
      Net121.registerC2SChannel(STORY_STATE);
      Net121.registerS2CChannel(STORY_BEGIN);
      Net121.registerC2SChannel(STORY_BEGIN);
      Net121.registerS2CChannel(STORY_CONTINUE);
      Net121.registerC2SChannel(STORY_CONTINUE);
      Net121.registerS2CChannel(STORY_DIALOGUE);
      Net121.registerS2CChannel(TEXTBOX_SHOW);
      Net121.registerC2SChannel(PROXIMITY_CHAT_TOGGLE);
      Net121.registerC2SChannel(PROXIMITY_CHAT_SEND);
      Net121.registerS2CChannel(PROXIMITY_CHAT_SHOW);
      Net121.registerC2SChannel(STORY_DIALOGUE);
      Net121.registerS2CChannel(STORY_DIALOGUE_DONE);
      Net121.registerC2SChannel(STORY_DIALOGUE_DONE);
      Net121.registerS2CChannel(STORY_CHOICE);
      Net121.registerC2SChannel(STORY_CHOICE);
      Net121.registerS2CChannel(RECRUIT_ACTIVATE);
      Net121.registerC2SChannel(RECRUIT_ACTIVATE);
      Net121.registerS2CChannel(STORY_INSPECT);
      Net121.registerC2SChannel(STORY_INSPECT);
      Net121.registerS2CChannel(VESSEL_SET);
      Net121.registerC2SChannel(VESSEL_SET);
      Net121.registerS2CChannel(VESSEL_INFO);
      Net121.registerC2SChannel(VESSEL_INFO);
      Net121.registerS2CChannel(SOUL_GLOW);
      Net121.registerC2SChannel(SOUL_GLOW);
      Net121.registerC2SChannel(SOUL_REVEAL);
      Net121.registerServerReceiver(SOUL_REVEAL, (server, player, handler, buf, sender) -> {
         boolean held = buf.readBoolean();
         server.execute(() -> {
            if (SoulWalk.isOut(player.getUuid())) {
               if (held) {
                  SoulWalk.returnToBody(player, false);
               }
            } else if (!held || !sealingPlayers.contains(player.getUuid())) {
               BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
               if (!prog.soulRemoved && prog.soulType != null) {
                  setSoulGlow(server, player, prog.soulType, held);
                  if (held) {
                     soulRevealingPlayers.put(player.getUuid(), player.getBlockPos());
                     placeSoulLight(player);
                  } else {
                     BlockPos trackedPos = soulRevealingPlayers.remove(player.getUuid());
                     if (trackedPos != null) {
                        removeSoulLightAt(player.getServerWorld(), trackedPos);
                     } else {
                        removeSoulLight(player);
                     }
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(PROXIMITY_CHAT_TOGGLE, (server, player, handler, buf, sender) -> {
         boolean enabled = buf.readBoolean();
         server.execute(() -> {
            if (enabled) {
               proximityChatDisabled.remove(player.getUuid());
            } else {
               proximityChatDisabled.add(player.getUuid());
            }
         });
      });
      Net121.registerServerReceiver(PROXIMITY_CHAT_SEND, (server, player, handler, buf, sender) -> {
         String raw = buf.readString(256);
         server.execute(() -> {
            if (!proximityChatDisabled.contains(player.getUuid())) {
               List pages = parseTextboxPages(raw);
               UUID speakerId = player.getUuid();
               Vec3d speakerPos = player.getPos();

               for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
                  if (target.getServerWorld().equals(player.getServerWorld()) && !(target.getPos().squaredDistanceTo(speakerPos) > 10000.0)) {
                     PacketByteBuf outBuf = PacketByteBufs.create();
                     outBuf.writeUuid(speakerId);
                     outBuf.writeInt(pages.size());

                     for (String page : pages) {
                        outBuf.writeString(page, 4096);
                     }

                     Net121.sendToClient(target, PROXIMITY_CHAT_SHOW, outBuf);
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(
         SAVE_POINT_USE,
         (server, player, handler, buf, sender) -> server.execute(
               () -> {
                  player.setSpawnPoint(player.getWorld().getRegistryKey(), player.getBlockPos(), player.getYaw(), true, false);
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  prog.savedLevel = prog.level;
                  prog.savedPlaytimeSec = server.getOverworld().getTime() / 20L;
                  RegistryEntry biomeEntry = player.getWorld().getBiome(player.getBlockPos());
                  prog.lastBiome = biomeEntry.getKey().map(k -> formatBiomeName(k.getValue().getPath())).orElse("---");
                  ServerWorld sw = (ServerWorld)player.getWorld();
                  sw.getEntitiesByClass(SavePointEntity.class, new Box(player.getPos(), player.getPos()).expand(4.0), e -> true)
                     .stream()
                     .findFirst()
                     .ifPresent(SavePointEntity::markUsed);
                  BattleManager.IS_ROLLING = true;

                  try {
                     player.setHealth(player.getMaxHealth());
                  } finally {
                     BattleManager.IS_ROLLING = false;
                  }

                  RecruitData rd = RecruitData.get(server);
                  RecruitData.PlayerRecruits pr = rd.players.get(player.getUuid());
                  if (pr != null) {
                     for (RecruitData.RecruitState rs : pr.recruits) {
                        if (rs != null) {
                           rs.hp = RecruitData.maxHp(rs);
                        }
                     }

                     for (RecruitData.CustomRecruitState cr : pr.customRecruits) {
                        if (cr != null && cr.state != null) {
                           cr.state.hp = RecruitData.maxHp(cr.state);
                        }
                     }
                  }

                  data.markDirty();
                  rd.markDirty();
                  PacketByteBuf ack = PacketByteBufs.create();
                  ack.writeLong(prog.savedPlaytimeSec);
                  ack.writeInt(prog.savedLevel);
                  Net121.sendToClient(player, SAVE_POINT_ACK, ack);
               }
            )
      );
      Net121.registerServerReceiver(SAVE_POINT_PLACE, (server, player, handler, buf, sender) -> {
         String name = buf.readString(32);
         server.execute(() -> {
            boolean holding = player.getMainHandStack().getItem() == SAVE_POINT_ITEM || player.getOffHandStack().getItem() == SAVE_POINT_ITEM;
            if (holding) {
               ServerWorld sw = (ServerWorld)player.getWorld();
               SavePointEntity sp = new SavePointEntity(SAVE_POINT_ENTITY_TYPE, sw);
               sp.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
               sw.spawnEntity(sp);
               if (player.getMainHandStack().getItem() == SAVE_POINT_ITEM) {
                  player.getMainHandStack().decrement(1);
               } else {
                  player.getOffHandStack().decrement(1);
               }

               String trimmed = name == null ? "" : name.trim();
               if (!trimmed.isEmpty()) {
                  sp.setPointName(trimmed);
               }
            }
         });
      });
      Net121.registerServerReceiver(SAVE_POINT_DELETE, (server, player, handler, buf, sender) -> {
         int entityId = buf.readInt();
         server.execute(() -> {
            if (player.getWorld().getEntityById(entityId) instanceof SavePointEntity sp && player.squaredDistanceTo(sp) < 16.0) {
               sp.discard();
            }
         });
      });
      Net121.registerS2CChannel(WORLD_DAMAGE_HUD);
      Net121.registerC2SChannel(WORLD_DAMAGE_HUD);
      Net121.registerS2CChannel(PLAYER_PARTY_STATE);
      Net121.registerC2SChannel(PLAYER_PARTY_STATE);
      Net121.registerS2CChannel(PARTY_MEMBER_DAMAGE);
      Net121.registerC2SChannel(PARTY_MEMBER_DAMAGE);
      Net121.registerS2CChannel(PARTY_INVITE_TOGGLE);
      Net121.registerC2SChannel(PARTY_INVITE_TOGGLE);
      Net121.registerS2CChannel(PARTY_INVITE);
      Net121.registerC2SChannel(PARTY_INVITE);
      Net121.registerS2CChannel(PARTY_LEAVE);
      Net121.registerC2SChannel(PARTY_LEAVE);
      Net121.registerS2CChannel(PARTY_INVITE_NOTIFY);
      Net121.registerC2SChannel(PARTY_INVITE_NOTIFY);
      Net121.registerS2CChannel(PARTY_INVITE_ACCEPT);
      Net121.registerC2SChannel(PARTY_INVITE_ACCEPT);
      Net121.registerS2CChannel(PARTY_INVITE_DECLINE);
      Net121.registerC2SChannel(PARTY_INVITE_DECLINE);
      Net121.registerS2CChannel(BATTLE_CHAT);
      Net121.registerC2SChannel(BATTLE_CHAT);
      Net121.registerS2CChannel(BATTLE_CHAT_MSG);
      Net121.registerC2SChannel(BATTLE_CHAT_MSG);
      Net121.registerS2CChannel(DD_ENTER);
      Net121.registerC2SChannel(DD_ENTER);
      Net121.registerS2CChannel(DD_EXIT);
      Net121.registerC2SChannel(DD_EXIT);
      Net121.registerS2CChannel(DD_ROOM_DATA);
      Net121.registerC2SChannel(DD_ROOM_DATA);
      Net121.registerS2CChannel(DD_PLAYER_MOVE);
      Net121.registerC2SChannel(DD_PLAYER_MOVE);
      Net121.registerS2CChannel(DD_ENCOUNTER);
      Net121.registerC2SChannel(DD_ENCOUNTER);
      Net121.registerS2CChannel(DD_ENCOUNTER_DONE);
      Net121.registerC2SChannel(DD_ENCOUNTER_DONE);
      Net121.registerS2CChannel(DD_CHEST_OPEN);
      Net121.registerC2SChannel(DD_CHEST_OPEN);
      Net121.registerS2CChannel(DD_CHEST_RESULT);
      Net121.registerC2SChannel(DD_CHEST_RESULT);
      Net121.registerS2CChannel(DD_STAIRS_INTERACT);
      Net121.registerC2SChannel(DD_STAIRS_INTERACT);
      Net121.registerS2CChannel(DD_STAIRS_VOTE);
      Net121.registerC2SChannel(DD_STAIRS_VOTE);
      Net121.registerS2CChannel(DD_STAIRS_PROMPT);
      Net121.registerC2SChannel(DD_STAIRS_PROMPT);
      Net121.registerS2CChannel(DD_NEXT_FLOOR);
      Net121.registerC2SChannel(DD_NEXT_FLOOR);
      Net121.registerS2CChannel(DD_PLAYERS_SYNC);
      Net121.registerC2SChannel(DD_PLAYERS_SYNC);
      Net121.registerS2CChannel(DD_PLAYER_POS);
      Net121.registerC2SChannel(DD_PLAYER_POS);
      Net121.registerS2CChannel(DD_TIMER_SYNC);
      Net121.registerC2SChannel(DD_TIMER_SYNC);
      Net121.registerS2CChannel(DD_ENEMIES_SYNC);
      Net121.registerC2SChannel(DD_ENEMIES_SYNC);
      Net121.registerS2CChannel(RECRUIT_INFO);
      Net121.registerC2SChannel(RECRUIT_INFO);
      Net121.registerS2CChannel(SET_SECOND_RECRUIT);
      Net121.registerC2SChannel(SET_SECOND_RECRUIT);
      Net121.registerS2CChannel(CREATE_VESSEL);
      Net121.registerC2SChannel(CREATE_VESSEL);
      Net121.registerS2CChannel(REMOVE_VESSEL);
      Net121.registerC2SChannel(REMOVE_VESSEL);
      Net121.registerS2CChannel(RECRUIT_SET_ACTIVE);
      Net121.registerC2SChannel(RECRUIT_SET_ACTIVE);
      Net121.registerS2CChannel(RECRUIT_STAT_SPEND);
      Net121.registerC2SChannel(RECRUIT_STAT_SPEND);
      Net121.registerS2CChannel(RECRUIT_SKILL_UNLOCK);
      Net121.registerC2SChannel(RECRUIT_SKILL_UNLOCK);
      Net121.registerS2CChannel(QUESTLINE_STATE);
      Net121.registerC2SChannel(QUESTLINE_STATE);
      Net121.registerS2CChannel(QUESTLINE_START);
      Net121.registerC2SChannel(QUESTLINE_START);
      Net121.registerS2CChannel(QUESTLINE_RETRY);
      Net121.registerC2SChannel(QUESTLINE_RETRY);
      TurnBattleConfig.load();
      LOGGER.info(
         "[TurnBattle] Config loaded \u2014 skillDamageScale="
            + TurnBattleConfig.get().skillDamageScale
            + ", healSpellScale="
            + TurnBattleConfig.get().healSpellScale
            + ", tpSpellScale="
            + TurnBattleConfig.get().tpSpellScale
            + ", distanceLevelMultiplier="
            + TurnBattleConfig.get().distanceLevelMultiplier
      );
      TurnBattleSounds.init();

      for (SoulType st : SoulType.values()) {
         SoulItem soulItem = new SoulItem(st);
         String id = st.name().toLowerCase(Locale.ROOT) + "_soul";
         Registry.register(Registries.ITEM, Identifier.of("turnbattle", id), soulItem);
         SOUL_ITEMS.put(st, soulItem);
      }

      SoulVesselItem soulVessel = new SoulVesselItem();
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "soul_vessel"), soulVessel);
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register((ModifyEntries)e -> {
         e.add(soulVessel);

         for (SoulItem si : SOUL_ITEMS.values()) {
            e.add(si);
         }
      });
      HumanEnemies.register();
      MagicantManager.register();
      DepthsRun.register();
      DarkFountainManager.registerCommands();
      Registry.register(Registries.BIOME_SOURCE, Identifier.of("turnbattle", "dark_world_mix"), DarkWorldBiomeSource.CODEC);
      THORN_RING = new Item(new Settings().maxCount(1));
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "thorn_ring"), THORN_RING);
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register((ModifyEntries)e -> e.add(THORN_RING));
      PSI_PROCESSOR = new Item(new Settings().maxCount(1));
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "psi_processor"), PSI_PROCESSOR);
      HEART_LOCKET = new Item(new Settings().maxCount(1));
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "heart_locket"), HEART_LOCKET);
      SOUL_RESONATOR = new Item(new Settings().maxCount(1));
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "soul_resonator"), SOUL_RESONATOR);
      SAVE_POINT_ITEM = new SavePointItem();
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "save_point"), SAVE_POINT_ITEM);
      CLIMB_CLAWS = new ClimbClawsItem();
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "climb_claws"), CLIMB_CLAWS);
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register((ModifyEntries)e -> e.add(CLIMB_CLAWS));
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register((ModifyEntries)e -> {
         e.add(PSI_PROCESSOR);
         e.add(HEART_LOCKET);
         e.add(SOUL_RESONATOR);
         e.add(SAVE_POINT_ITEM);
      });
      MANNEQUIN_ITEM = new MannequinItem();
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "mannequin"), MANNEQUIN_ITEM);
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register((ModifyEntries)e -> e.add(MANNEQUIN_ITEM));
      DARK_FOUNTAIN_KNIFE = new DarkFountainKnife();
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "dark_fountain_knife"), DARK_FOUNTAIN_KNIFE);
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register((ModifyEntries)e -> e.add(DARK_FOUNTAIN_KNIFE));
      DARK_FOUNTAIN_BLOCK = new DarkFountainBlock();
      Registry.register(Registries.BLOCK, Identifier.of("turnbattle", "dark_fountain_block"), DARK_FOUNTAIN_BLOCK);
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "dark_fountain_block"), new BlockItem(DARK_FOUNTAIN_BLOCK, new Settings().maxCount(64)));
      DARK_FOUNTAIN_BLOCK_ENTITY_TYPE = (BlockEntityType<DarkFountainBlockEntity>)Registry.register(
         Registries.BLOCK_ENTITY_TYPE,
         Identifier.of("turnbattle", "dark_fountain_block"),
         FabricBlockEntityTypeBuilder.create(DarkFountainBlockEntity::new, new Block[]{DARK_FOUNTAIN_BLOCK}).build()
      );
      DARK_BLOCK = new DarkBlock();
      Registry.register(Registries.BLOCK, Identifier.of("turnbattle", "dark_block"), DARK_BLOCK);
      Registry.register(Registries.ITEM, Identifier.of("turnbattle", "dark_block"), new BlockItem(DARK_BLOCK, new Settings().maxCount(64)));
      Net121.registerServerReceiver(WALL_CLIMB_INPUT, (server, player, handler, buf, sender) -> {
         byte mask = buf.readByte();
         server.execute(() -> WallClimbManager.onInput(player.getUuid(), mask));
      });
      Net121.registerServerReceiver(
         PLAYER_ACTION,
         (server, player, handler, buf, sender) -> {
            int type = buf.readByte();
            int param = buf.readInt();
            int targetIndex = buf.readByte();
            server.execute(
               () -> {
                  BattleState vesselState = BattleManager.activeBattles.get(player.getUuid());
                  int actorIdx = -1;
                  UUID vesselActor = null;
                  String usedItemId = "";
                  if (vesselState != null) {
                     UUID actor = vesselState.currentParticipant();
                     actorIdx = vesselState.participantOrder.indexOf(actor);
                     vesselActor = actor;
                     if (type == 2 && param >= 0 && param < player.getInventory().size()) {
                        ItemStack used = player.getInventory().getStack(param);
                        if (!used.isEmpty()) {
                           usedItemId = Registries.ITEM.getId(used.getItem()).toString();
                        }
                     }
                  }

                  BattleManager.lastActionQueued = false;

                  try {
                     BattleManager.processAction(player, type, param, targetIndex);
                  } catch (Exception var11) {
                     LOGGER.error(
                        "[Battle] Action processing failed for "
                           + player.getName().getString()
                           + " (type="
                           + type
                           + ", param="
                           + param
                           + ", target="
                           + targetIndex
                           + ")",
                        var11
                     );
                     BattleState broken = BattleManager.activeBattles.get(player.getUuid());
                     if (broken != null) {
                        BattleManager.endBattle(broken, server, "error");
                     }

                     return;
                  }

                  boolean queued = BattleManager.lastActionQueued;
                  BattleManager.lastActionQueued = false;
                  if (vesselState != null && actorIdx >= 0 && !queued) {
                     BattleNetworking.broadcastVesselAnim(
                        vesselState, player.getServer().getPlayerManager(), actorIdx, type, player.getUuid(), vesselActor, usedItemId
                     );
                  }
               }
            );
         }
      );
      Net121.registerServerReceiver(CLONE_POSE, (server, player, handler, buf, sender) -> {
         int code = buf.readByte();
         String recruitName = buf.readableBytes() > 0 ? buf.readString(64) : "";
         server.execute(() -> {
            BattleState poseState = BattleManager.activeBattles.get(player.getUuid());
            if (poseState != null) {
               int safe = code >= 0 && code <= 4 ? code : 0;
               BattleNetworking.broadcastClonePose(poseState, server.getPlayerManager(), player.getUuid(), safe, recruitName);
            }
         });
      });
      Net121.registerServerReceiver(BASH_QTE_ROLL, (server, player, handler, buf, sender) -> {
         int targetIndex = buf.readInt();
         server.execute(() -> BattleManager.onBashQteRoll(player, targetIndex));
      });
      Net121.registerServerReceiver(BASH_QTE_RESULT, (server, player, handler, buf, sender) -> {
         float qteMultiplier = buf.readFloat();
         int qteTpTier = buf.readInt();
         server.execute(() -> BattleManager.onBashQteResult(player, qteMultiplier, qteTpTier));
      });
      Net121.registerServerReceiver(BATTLE_LINE_INPUT, (server, player, handler, buf, sender) -> {
         int kind = buf.readByte();
         int seq = buf.readInt();
         server.execute(() -> BattleManager.onBattleLineInput(player, kind, seq));
      });
      Net121.registerServerReceiver(MULTI_BASH_RESULT, (server, player, handler, buf, sender) -> {
         int n = Math.min(8, buf.readInt());
         List actors = new ArrayList<>();
         List results = new ArrayList<>();

         for (int i = 0; i < n; i++) {
            actors.add(buf.readUuid());
            float mult = buf.readFloat();
            int tier = buf.readInt();
            float pos = buf.readFloat();
            results.add(new float[]{mult, (float)tier, pos});
         }

         server.execute(() -> BattleManager.onMultiBashResult(player, actors, results));
      });
      Net121.registerServerReceiver(ACT_PERFORM, (server, player, handler, buf, sender) -> {
         int actIndex = buf.readInt();
         int targetEnemyIdx = buf.readInt();
         server.execute(() -> {
            BattleState actState = BattleManager.activeBattles.get(player.getUuid());
            UUID actActor = null;
            int actIdx = -1;
            if (actState != null && actState.phase == BattleState.Phase.PLAYER_TURN) {
               actActor = actState.currentParticipant();
               actIdx = actState.participantOrder.indexOf(actActor);
            }

            BattleManager.lastActionQueued = false;
            BattleManager.performAct(player, actIndex, targetEnemyIdx);
            boolean actQueued = BattleManager.lastActionQueued;
            BattleManager.lastActionQueued = false;
            if (actState != null && actIdx >= 0 && !actQueued) {
               BattleNetworking.broadcastVesselAnim(actState, server.getPlayerManager(), actIdx, 7, player.getUuid(), actActor, "");
            }
         });
      });
      Net121.registerServerReceiver(ENEMY_TURN_RESULT, (server, player, handler, buf, sender) -> {
         int hits = buf.readInt();
         int grazes = buf.readInt();
         float held = buf.isReadable() ? Math.max(0.0F, Math.min(200.0F, buf.readFloat())) : 0.0F;
         server.execute(() -> BattleManager.processEnemyTurnResult(player, hits, grazes, held));
      });
      Net121.registerServerReceiver(ALL_OUT_VOTE, (server, player, handler, buf, sender) -> {
      });
      Net121.registerServerReceiver(BATON_PASS_CHOICE, (server, player, handler, buf, sender) -> {
      });
      Net121.registerServerReceiver(SOUL_HIT, (server, player, handler, buf, sender) -> {
         float dmg = buf.readFloat();
         server.execute(() -> {
            BattleManager.applySoulHitDamage(player, dmg);
            BattleState hurtState = BattleManager.activeBattles.get(player.getUuid());
            if (hurtState != null) {
               int hurtIdx = hurtState.participantOrder.indexOf(player.getUuid());
               if (hurtIdx >= 0) {
                  BattleNetworking.broadcastVesselAnim(hurtState, player.getServer().getPlayerManager(), hurtIdx, 6, player.getUuid());
               }
            }

            if (TurnBattleConfig.get().rollingHealth) {
               PacketByteBuf hpBuf = PacketByteBufs.create();
               hpBuf.writeFloat(BattleManager.TARGET_HEALTH.getOrDefault(player.getUuid(), player.getHealth()));
               Net121.sendToClient(player, HP_SYNC, hpBuf);
            }
         });
      });
      Net121.registerServerReceiver(CLASS_CHOSEN, (server, player, handler, buf, sender) -> {
         int ord = buf.readInt();
         server.execute(() -> {
            PlayerClass[] classes = PlayerClass.values();
            if (ord >= 0 && ord < classes.length) {
               BattleData data = BattleData.get(server);
               BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
               prog.playerClass = classes[ord];
               data.markDirty();
               if (!SKIP_INTRO_NEXT.remove(player.getUuid())) {
                  player.setInvulnerable(true);
                  if (prog.soulType == null) {
                     SoulType[] introTypes = Arrays.stream(SoulType.values()).filter(s -> s != SoulType.ORI).toArray(SoulType[]::new);
                     prog.soulType = introTypes[RNG.nextInt(introTypes.length)];
                     prog.freeRollUsed = true;
                     data.markDirty();
                  }

                  sendSoulInfo(player, prog);
                  PacketByteBuf _ib = PacketByteBufs.create();
                  _ib.writeBoolean(TurnBattleConfig.get().storyMode);
                  Net121.sendToClient(player, INTRO_START, _ib);
               } else {
                  prog.introSeen = true;
                  data.markDirty();
               }
            }
         });
      });
      Net121.registerServerReceiver(MENU_STATE, (server, player, handler, buf, sender) -> {
         boolean open = buf.readBoolean();
         server.execute(() -> PlayerActionNet.broadcast(player, open ? 6 : 7, false, null));
      });
      ServerSettings.registerServer();
      Net121.registerServerReceiver(BATTLE_PREFS, (server, player, handler, buf, sender) -> {
         int styleOrd = buf.readInt();
         int viewOrd = buf.readInt();
         server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            BattleStyle[] styles = BattleStyle.values();
            BattleStyle style = styleOrd >= 0 && styleOrd < styles.length ? styles[styleOrd] : prog.battleStyle;
            if (style != prog.battleStyle && BattleManager.isInBattle(player.getUuid())) {
               player.sendMessage(Text.literal("Finish this battle first.").formatted(Formatting.GRAY), true);
               style = prog.battleStyle;
            }

            prog.battleStyle = style;
            prog.battleView = BattleView.TRADITIONAL;
            data.markDirty();
            LiveCombat.sendState(player);
            sendBattlePrefs(player);
         });
      });
      Net121.registerServerReceiver(SOUL_RIP_STATE, (server, player, handler, buf, sender) -> {
         boolean on = buf.readBoolean();
         server.execute(() -> {
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
            if (on) {
               if (prog.soulRemoved || prog.soulType == null || prog.playerRace == CharacterRace.MONSTER) {
                  return;
               }

               if (SoulWalk.isOut(player.getUuid())) {
                  return;
               }

               if (DepthsRun.isActive(player.getUuid())) {
                  return;
               }

               int soulOrd = prog.soulType.ordinal();
               PlayerActionNet.broadcast(player, 3, false, b -> b.writeInt(soulOrd));
            } else {
               PlayerActionNet.broadcast(player, 4, false, null);
            }
         });
      });
      Net121.registerServerReceiver(SOUL_REMOVE_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            if (SoulWalk.tryLeave(player)) {
               PlayerActionNet.broadcast(player, 5, false, null);
            } else {
               PlayerActionNet.broadcast(player, 4, true, null);
            }
         }));
      Net121.registerServerReceiver(CHAR_CREATE_DONE, (server, player, handler, buf, sender) -> {
         int raceOrd = buf.readInt();
         int classOrd = buf.readInt();
         int soulOrd = buf.readInt();
         String vname = buf.readString(32);
         int bsOrd = buf.readInt();
         int viewOrd = buf.readableBytes() >= 4 ? buf.readInt() : BattleView.TRADITIONAL.ordinal();
         server.execute(() -> {
            if (vname.equalsIgnoreCase("gaster")) {
               Net121.sendToClient(player, GASTER_TRIGGER, PacketByteBufs.empty());
            } else {
               BattleData data = BattleData.get(server);
               BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
               CharacterRace[] races = CharacterRace.values();
               if (raceOrd >= 0 && raceOrd < races.length) {
                  prog.playerRace = races[raceOrd];
               } else {
                  prog.playerRace = CharacterRace.HUMAN;
               }

               if (prog.playerRace == CharacterRace.MONSTER) {
                  PlayerClass[] monsterAttunements = new PlayerClass[]{PlayerClass.PSI_USER, PlayerClass.LIGHTNER};
                  if (classOrd >= 0 && classOrd < monsterAttunements.length) {
                     prog.playerClass = monsterAttunements[classOrd];
                  } else {
                     prog.playerClass = PlayerClass.PSI_USER;
                  }

                  prog.soulType = SoulType.MONSTER;
                  prog.freeRollUsed = true;
               } else {
                  PlayerClass[] attunements = new PlayerClass[]{PlayerClass.PSI_USER, PlayerClass.LIGHTNER, PlayerClass.SOUL_MAGE};
                  if (classOrd >= 0 && classOrd < attunements.length) {
                     prog.playerClass = attunements[classOrd];
                  } else {
                     prog.playerClass = PlayerClass.PSI_USER;
                  }

                  SoulType[] souls = Arrays.stream(SoulType.values()).filter(s -> s != SoulType.ORI && s != SoulType.MONSTER).toArray(SoulType[]::new);
                  if (soulOrd >= 0 && soulOrd < souls.length) {
                     prog.soulType = souls[soulOrd];
                  } else {
                     prog.soulType = SoulType.DETERMINATION;
                  }

                  prog.freeRollUsed = true;
               }

               String trimmed = vname.trim();
               if (trimmed.isEmpty()) {
                  trimmed = "Vessel";
               }

               prog.vesselName = trimmed;
               prog.introSeen = true;
               prog.worldTypeSeen = true;
               BattleStyle[] bsVals = BattleStyle.values();
               prog.battleStyle = bsOrd >= 0 && bsOrd < bsVals.length ? bsVals[bsOrd] : BattleStyle.BULLET_HELL;
               prog.battleView = BattleView.TRADITIONAL;
               data.markDirty();
               LiveCombat.sendState(player);
               server.getPlayerManager().sendToAll(new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, player));
               player.setInvulnerable(false);
               sendSoulInfo(player, prog);
               PacketByteBuf resp = PacketByteBufs.create();
               resp.writeString(trimmed);
               Net121.sendToClient(player, VESSEL_NAME, resp);
            }
         });
      });
      Net121.registerServerReceiver(OFFHAND_GIMMICK, (server, player, handler, buf, sender) -> {
         int gimmickOrd = buf.readInt();
         server.execute(() -> {
            BattleState st = BattleManager.activeBattles.get(player.getUuid());
            if (st != null) {
               OffhandGimmick[] vals = OffhandGimmick.values();
               OffhandGimmick g = gimmickOrd >= 0 && gimmickOrd < vals.length ? vals[gimmickOrd] : OffhandGimmick.NONE;
               st.offhandGimmicks.put(player.getUuid(), g);
            }
         });
      });
      Net121.registerServerReceiver(VESSEL_NAME, (server, player, handler, buf, sender) -> {
         String name = buf.readString(32);
         server.execute(() -> {
            if (name.equalsIgnoreCase("gaster")) {
               Net121.sendToClient(player, GASTER_TRIGGER, PacketByteBufs.empty());
            } else {
               BattleData data = BattleData.get(server);
               BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
               String trimmed = name.trim();
               if (trimmed.isEmpty()) {
                  trimmed = "Vessel";
               }

               prog.vesselName = trimmed;
               data.markDirty();
               server.getPlayerManager().sendToAll(new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, player));
               if (prog.soulType == null) {
                  SoulType[] types = Arrays.stream(SoulType.values()).filter(s -> s != SoulType.ORI).toArray(SoulType[]::new);
                  prog.soulType = types[RNG.nextInt(types.length)];
                  prog.freeRollUsed = true;
                  data.markDirty();
               }

               PacketByteBuf resp = PacketByteBufs.create();
               resp.writeString(trimmed);
               Net121.sendToClient(player, VESSEL_NAME, resp);
               sendSoulInfo(player, prog);
            }
         });
      });
      Net121.registerServerReceiver(PARTY_INFO_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            sendPartyInfo(player);
            sendRecruitInfo(player);
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
            sendQuestlineState(player, prog);
            sendStatsExtra(player);
         }));
      Net121.registerServerReceiver(SKILL_INFO_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> sendPartyInfo(player)));
      Net121.registerServerReceiver(PARTY_SWITCH, (server, player, handler, buf, sender) -> {
         int ord = buf.readInt();
         server.execute(() -> {
            PartyMember[] members = PartyMember.values();
            if (ord >= 0 && ord < members.length) {
               BattleData data = BattleData.get(server);
               BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
               prog.partyMember = members[ord];
               data.markDirty();
            }
         });
      });
      Net121.registerServerReceiver(STAT_SPEND, (server, player, handler, buf, sender) -> {
         int targetType = buf.readByte();
         int statIndex = buf.readByte();
         server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            CharStats stats = targetType == 0 ? prog.playerStats : (targetType < PartyMember.values().length ? prog.partyStats[targetType] : null);
            if (stats != null && stats.unspent > 0) {
               if (targetType == 0) {
                  stats.pointValue = prog.magicant ? 2 : 1;
               }

               if (statIndex != 5 || !stats.isMaxed(5)) {
                  if (stats.spendPoint(statIndex)) {
                     data.markDirty();
                     if (targetType == 0) {
                        MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
                     }

                     int miS = prog.partyMember.ordinal();
                     float partyMaxS = BattleManager.computePartyMemberMaxHpPublic(prog.partyLevels[miS], prog.partyStats[miS]);
                     float partyHpS = prog.partyMemberHp >= 0.0F ? Math.min(prog.partyMemberHp, partyMaxS) : partyMaxS;
                     PacketByteBuf respS = PacketByteBufs.create();
                     respS.writeInt(prog.level);
                     respS.writeFloat(player.getHealth());
                     respS.writeFloat(player.getMaxHealth());
                     respS.writeInt(prog.playerClass == null ? 0 : prog.playerClass.ordinal());
                     respS.writeInt(prog.partyMember.ordinal());
                     respS.writeFloat(partyHpS);
                     respS.writeFloat(partyMaxS);
                     respS.writeInt(prog.playerRace == null ? 0 : prog.playerRace.ordinal());
                     respS.writeBoolean(prog.soulRemoved);
                     respS.writeInt(prog.absorbedSouls.size());

                     for (String s : prog.absorbedSouls) {
                        respS.writeString(s, 32);
                     }

                     writeCharStats(respS, prog.playerStats);

                     for (PartyMember m : PartyMember.values()) {
                        respS.writeInt(prog.partyLevels[m.ordinal()]);
                        writeCharStats(respS, prog.partyStats[m.ordinal()]);
                     }

                     Net121.sendToClient(player, PARTY_INFO, respS);
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(STAT_RESET, (server, player, handler, buf, sender) -> {
         int targetType = buf.readInt();
         server.execute(() -> {
            if (targetType == 0 && BattleData.get(server).getOrCreate(player.getUuid()).rogueRun) {
               player.sendMessage(Text.literal("[Stats] Your stats come from your cards during a Roguelite run - they can't be reset."), false);
            } else {
               CharStats targetStats = null;
               Runnable afterReset = null;
               if (targetType >= 1000) {
                  int ci = targetType >= 20000 ? targetType - 20000 : targetType - 1000;
                  RecruitData rd = RecruitData.get(server);
                  RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
                  if (ci >= 0 && ci < pr.customRecruits.size()) {
                     targetStats = pr.customRecruits.get(ci).state.stats;
                     afterReset = () -> {
                        rd.markDirty();
                        sendRecruitInfo(player);
                     };
                  }
               } else if (targetType >= 1) {
                  RecruitData rd = RecruitData.get(server);
                  RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
                  int seen = 0;
                  Recruit matched = null;

                  for (Recruit r : Recruit.values()) {
                     if (pr.recruits[r.ordinal()].unlocked) {
                        if (++seen == targetType) {
                           matched = r;
                           break;
                        }
                     }
                  }

                  if (matched != null) {
                     targetStats = pr.recruits[matched.ordinal()].stats;
                     afterReset = () -> {
                        rd.markDirty();
                        sendRecruitInfo(player);
                     };
                  }
               } else {
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  CharStats stats = targetType == 0 ? prog.playerStats : (targetType < PartyMember.values().length ? prog.partyStats[targetType] : null);
                  if (stats != null) {
                     targetStats = stats;
                     afterReset = () -> {
                        data.markDirty();
                        if (targetType == 0) {
                           MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
                        }

                        sendUpdatedPartyInfoAfterStatReset(server, player, prog);
                     };
                  }
               }

               if (targetStats == null) {
                  player.sendMessage(Text.literal("[Stats] Couldn't find that target to reset (no netherite consumed)."), false);
               } else {
                  boolean hasIngot = player.getInventory().contains(stack -> stack.isOf(Items.NETHERITE_INGOT));
                  if (!hasIngot) {
                     player.sendMessage(Text.literal("[Stats] You need 1 Netherite Ingot to reset stats."), false);
                  } else {
                     player.getInventory().remove(stack -> stack.isOf(Items.NETHERITE_INGOT), 1, player.playerScreenHandler.getCraftingInput());
                     targetStats.reset();
                     afterReset.run();
                     player.sendMessage(Text.literal("[Stats] Stats reset! Points refunded."), false);
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(SOUL_POS_SYNC, (server, player, handler, buf, sender) -> {
         float relX = buf.readFloat();
         float relY = buf.readFloat();
         server.execute(() -> {
            BattleState state = BattleManager.activeBattles.get(player.getUuid());
            if (state != null) {
               for (UUID pid : state.participantOrder) {
                  if (!pid.equals(player.getUuid()) && !pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
                     ServerPlayerEntity peer = server.getPlayerManager().getPlayer(pid);
                     if (peer != null) {
                        PacketByteBuf relay = PacketByteBufs.create();
                        relay.writeUuid(player.getUuid());
                        relay.writeFloat(relX);
                        relay.writeFloat(relY);
                        Net121.sendToClient(peer, SOUL_POS_SYNC, relay);
                     }
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(DIRECT_ENEMY_TURN_RESULT, (server, player, handler, buf, sender) -> {
         float dmgTaken = buf.readFloat();
         server.execute(() -> BattleManager.processDirectEnemyTurnResult(player, dmgTaken));
      });
      Net121.registerServerReceiver(SET_SECOND_RECRUIT, (server, player, handler, buf, sender) -> {
         int slot = buf.readInt();
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            pr.secondActiveRecruit = slot;
            if (slot >= 0 && pr.activeRecruit == slot) {
               pr.activeRecruit = -1;
            }

            rd.markDirty();
            sendRecruitInfo(player);
         });
      });
      Net121.registerServerReceiver(CREATE_VESSEL, (server, player, handler, buf, sender) -> {
         String name = buf.readString(32);
         int classOrd = buf.readInt();
         int soulOrd = buf.readInt();
         server.execute(() -> {
            PlayerClass[] classes = PlayerClass.values();
            SoulType[] souls = SoulType.values();
            if (classOrd >= 0 && classOrd < classes.length) {
               PlayerClass cls = classes[classOrd];
               SoulType soul = soulOrd >= 0 && soulOrd < souls.length ? souls[soulOrd] : null;
               RecruitData rd = RecruitData.get(server);
               RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
               BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
               RecruitData.CustomRecruitState cr = new RecruitData.CustomRecruitState();
               cr.name = name.isBlank() ? "Vessel" : name.trim();
               cr.playerClass = cls;
               cr.soulType = soul;
               cr.state.level = Math.max(1, prog.level);
               cr.state.stats.unspent = (cr.state.level - 1) * 3;
               cr.state.skillPoints = (cr.state.level - 1) * 3;
               pr.customRecruits.add(cr);
               rd.markDirty();
               sendRecruitInfo(player);
            }
         });
      });
      Net121.registerServerReceiver(REMOVE_VESSEL, (server, player, handler, buf, sender) -> {
         int index = buf.readInt();
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            if (index >= 0 && index < pr.customRecruits.size()) {
               int code = 1000 + index;
               if (pr.activeRecruit == code) {
                  pr.activeRecruit = -1;
               }

               if (pr.secondActiveRecruit == code) {
                  pr.secondActiveRecruit = -1;
               }

               if (pr.activeRecruit >= 1000 && pr.activeRecruit > code) {
                  pr.activeRecruit--;
               }

               if (pr.secondActiveRecruit >= 1000 && pr.secondActiveRecruit > code) {
                  pr.secondActiveRecruit--;
               }

               pr.customRecruits.remove(index);
               rd.markDirty();
               sendRecruitInfo(player);
            }
         });
      });
      Net121.registerServerReceiver(EQUIP_INFO_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            sendEquipInfo(player, prog);
         }));
      Net121.registerServerReceiver(WEAPON_ROLL_REQUEST, (server, player, handler, buf, sender) -> {
         int slot = buf.readInt();
         server.execute(() -> {
            ItemStack s = player.getInventory().getStack(slot);
            if (!s.isEmpty() && hasAttackDamage(s)) {
               if (WeaponStats.fromStack(s) == null) {
                  WeaponStats ws = WeaponStats.generate(new Random(), WeaponStats.tierOf(s));
                  ws.applyToStack(s);
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  sendEquipInfo(player, prog);
               }
            }
         });
      });
      Net121.registerServerReceiver(ARMOR_INFO_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            sendArmorInfo(player, prog);
         }));
      Net121.registerServerReceiver(EQUIP_ARMOR, (server, player, handler, buf, sender) -> {
         int invSlot = buf.readInt();
         int armorIndex = buf.readInt();
         server.execute(() -> {
            if (armorIndex >= 0 && armorIndex <= 1) {
               ItemStack toEquip = player.getInventory().getStack(invSlot);
               if (!toEquip.isEmpty() && ArmorStats.isArmorItem(toEquip)) {
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  ItemStack other = prog.equippedArmor[1 - armorIndex];
                  if (ArmorStats.getSlotType(toEquip) != ArmorStats.getSlotType(other)) {
                     ItemStack current = prog.equippedArmor[armorIndex];
                     if (current != null && !current.isEmpty()) {
                        MobLevelManager.removeArmorStats(player, armorIndex);
                        if (!player.getInventory().insertStack(current.copy())) {
                           player.dropItem(current.copy(), false);
                        }
                     }

                     prog.equippedArmor[armorIndex] = toEquip.copy();
                     MobLevelManager.applyArmorStats(player, ArmorStats.fromStack(toEquip), armorIndex);
                     player.getInventory().removeStack(invSlot);
                     data.markDirty();
                     sendArmorInfo(player, prog);
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(UNEQUIP_ARMOR, (server, player, handler, buf, sender) -> {
         int armorIndex = buf.readInt();
         server.execute(() -> {
            if (armorIndex >= 0 && armorIndex <= 1) {
               BattleData data = BattleData.get(server);
               BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
               ItemStack current = prog.equippedArmor[armorIndex];
               if (current != null && !current.isEmpty()) {
                  MobLevelManager.removeArmorStats(player, armorIndex);
                  if (!player.getInventory().insertStack(current.copy())) {
                     player.dropItem(current.copy(), false);
                  }

                  prog.equippedArmor[armorIndex] = ItemStack.EMPTY;
                  data.markDirty();
                  sendArmorInfo(player, prog);
               }
            }
         });
      });
      Net121.registerServerReceiver(RECRUIT_EQUIP_ARMOR, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         int armorIndex = buf.readInt();
         int invSlot = buf.readInt();
         server.execute(() -> {
            if (armorIndex >= 0 && armorIndex <= 1) {
               ItemStack toEquip = player.getInventory().getStack(invSlot);
               if (!toEquip.isEmpty() && ArmorStats.isArmorItem(toEquip)) {
                  RecruitData rd = RecruitData.get(server);
                  RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
                  RecruitData.RecruitState rs;
                  if (recruitOrd >= 1000) {
                     int ci = recruitOrd - 1000;
                     if (ci < 0 || ci >= pr.customRecruits.size()) {
                        return;
                     }

                     rs = pr.customRecruits.get(ci).state;
                  } else {
                     if (recruitOrd < 0 || recruitOrd >= pr.recruits.length) {
                        return;
                     }

                     rs = pr.recruits[recruitOrd];
                  }

                  ItemStack current = rs.equippedArmor[armorIndex];
                  ItemStack other = rs.equippedArmor[1 - armorIndex];
                  if (ArmorStats.getSlotType(toEquip) != ArmorStats.getSlotType(other)) {
                     if (current != null && !current.isEmpty() && !player.getInventory().insertStack(current.copy())) {
                        player.dropItem(current.copy(), false);
                     }

                     rs.equippedArmor[armorIndex] = toEquip.copy();
                     player.getInventory().removeStack(invSlot);
                     rd.markDirty();
                     sendRecruitInfo(player);
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(RECRUIT_UNEQUIP_ARMOR, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         int armorIndex = buf.readInt();
         server.execute(() -> {
            if (armorIndex >= 0 && armorIndex <= 1) {
               RecruitData rd = RecruitData.get(server);
               RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
               RecruitData.RecruitState rs;
               if (recruitOrd >= 1000) {
                  int ci = recruitOrd - 1000;
                  if (ci < 0 || ci >= pr.customRecruits.size()) {
                     return;
                  }

                  rs = pr.customRecruits.get(ci).state;
               } else {
                  if (recruitOrd < 0 || recruitOrd >= pr.recruits.length) {
                     return;
                  }

                  rs = pr.recruits[recruitOrd];
               }

               ItemStack current = rs.equippedArmor[armorIndex];
               if (current != null && !current.isEmpty()) {
                  if (!player.getInventory().insertStack(current.copy())) {
                     player.dropItem(current.copy(), false);
                  }

                  rs.equippedArmor[armorIndex] = ItemStack.EMPTY;
                  rd.markDirty();
                  sendRecruitInfo(player);
               }
            }
         });
      });
      Net121.registerServerReceiver(ARMOR_ROLL_REQUEST, (server, player, handler, buf, sender) -> {
         int slot = buf.readInt();
         server.execute(() -> {
            ItemStack s = player.getInventory().getStack(slot);
            if (ArmorStats.isArmorItem(s)) {
               if (ArmorStats.fromStack(s) == null) {
                  ArmorStats.rollStats(s, new Random());
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  sendArmorInfo(player, prog);
               }
            }
         });
      });
      Net121.registerServerReceiver(
         EQUIP_WEAPON,
         (server, player, handler, buf, sender) -> {
            int slotIndex = buf.readInt();
            boolean isParty = buf.readBoolean();
            server.execute(
               () -> {
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  ItemStack toEquip = player.getInventory().getStack(slotIndex);
                  if (!toEquip.isEmpty() && hasAttackDamage(toEquip)) {
                     WeaponStats newWeaponStats = WeaponStats.fromStack(toEquip);
                     if (newWeaponStats != null) {
                        if (isParty) {
                           if (prog.partyEquippedWeapon != null
                              && !prog.partyEquippedWeapon.isEmpty()
                              && !player.getInventory().insertStack(prog.partyEquippedWeapon.copy())) {
                              player.dropItem(prog.partyEquippedWeapon.copy(), false);
                           }

                           prog.partyEquippedWeapon = toEquip.copy();
                        } else {
                           if (prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty()) {
                              MobLevelManager.removeWeaponStats(player);
                              if (!player.getInventory().insertStack(prog.equippedWeapon.copy())) {
                                 player.dropItem(prog.equippedWeapon.copy(), false);
                              }
                           }

                           prog.equippedWeapon = toEquip.copy();
                           MobLevelManager.applyWeaponStats(player, newWeaponStats);
                        }

                        player.getInventory().removeStack(slotIndex);
                        data.markDirty();
                        sendEquipInfo(player, prog);
                        rebroadcastEquipIfInBattle(player);
                     }
                  }
               }
            );
         }
      );
      Net121.registerServerReceiver(UNEQUIP_WEAPON, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            if (prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty()) {
               MobLevelManager.removeWeaponStats(player);
               if (!player.getInventory().insertStack(prog.equippedWeapon.copy())) {
                  player.dropItem(prog.equippedWeapon.copy(), false);
               }

               prog.equippedWeapon = null;
               data.markDirty();
               sendEquipInfo(player, prog);
               rebroadcastEquipIfInBattle(player);
            }
         }));
      Net121.registerServerReceiver(PARTY_UNEQUIP_WEAPON, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            if (prog.partyEquippedWeapon != null && !prog.partyEquippedWeapon.isEmpty()) {
               if (!player.getInventory().insertStack(prog.partyEquippedWeapon.copy())) {
                  player.dropItem(prog.partyEquippedWeapon.copy(), false);
               }

               prog.partyEquippedWeapon = null;
               data.markDirty();
               sendEquipInfo(player, prog);
               rebroadcastEquipIfInBattle(player);
            }
         }));
      Net121.registerServerReceiver(RECRUIT_EQUIP_WEAPON, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         int slotIndex = buf.readInt();
         server.execute(() -> {
            ItemStack toEquip = player.getInventory().getStack(slotIndex);
            if (!toEquip.isEmpty()) {
               RecruitData rd = RecruitData.get(server);
               RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
               if (recruitOrd >= 1000) {
                  int ci = recruitOrd - 1000;
                  if (ci >= 0 && ci < pr.customRecruits.size()) {
                     RecruitData.RecruitState rs = pr.customRecruits.get(ci).state;
                     if (rs.equippedWeapon != null && !rs.equippedWeapon.isEmpty() && !player.getInventory().insertStack(rs.equippedWeapon.copy())) {
                        player.dropItem(rs.equippedWeapon.copy(), false);
                     }

                     rs.equippedWeapon = toEquip.copy();
                     player.getInventory().removeStack(slotIndex);
                     rd.markDirty();
                     sendRecruitInfo(player);
                     rebroadcastEquipIfInBattle(player);
                  }
               } else if (recruitOrd >= 0 && recruitOrd < Recruit.values().length) {
                  Recruit recruit = Recruit.values()[recruitOrd];
                  RecruitData.RecruitState rs = pr.get(recruit);
                  if (rs.equippedWeapon != null && !rs.equippedWeapon.isEmpty() && !player.getInventory().insertStack(rs.equippedWeapon.copy())) {
                     player.dropItem(rs.equippedWeapon.copy(), false);
                  }

                  rs.equippedWeapon = toEquip.copy();
                  player.getInventory().removeStack(slotIndex);
                  rd.markDirty();
                  sendRecruitInfo(player);
                  rebroadcastEquipIfInBattle(player);
               }
            }
         });
      });
      Net121.registerServerReceiver(RECRUIT_UNEQUIP_WEAPON, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            if (recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               if (ci >= 0 && ci < pr.customRecruits.size()) {
                  RecruitData.RecruitState rs = pr.customRecruits.get(ci).state;
                  if (rs.equippedWeapon != null && !rs.equippedWeapon.isEmpty()) {
                     if (!player.getInventory().insertStack(rs.equippedWeapon.copy())) {
                        player.dropItem(rs.equippedWeapon.copy(), false);
                     }

                     rs.equippedWeapon = null;
                     rd.markDirty();
                     sendRecruitInfo(player);
                     rebroadcastEquipIfInBattle(player);
                  }
               }
            } else if (recruitOrd >= 0 && recruitOrd < Recruit.values().length) {
               Recruit recruit = Recruit.values()[recruitOrd];
               RecruitData.RecruitState rs = pr.get(recruit);
               if (rs.equippedWeapon != null && !rs.equippedWeapon.isEmpty()) {
                  if (!player.getInventory().insertStack(rs.equippedWeapon.copy())) {
                     player.dropItem(rs.equippedWeapon.copy(), false);
                  }

                  rs.equippedWeapon = null;
                  rd.markDirty();
                  sendRecruitInfo(player);
                  rebroadcastEquipIfInBattle(player);
               }
            }
         });
      });
      Net121.registerServerReceiver(ACCESSORY_INFO_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            sendAccessoryInfo(player, prog);
         }));
      Net121.registerServerReceiver(EQUIP_ACCESSORY, (server, player, handler, buf, sender) -> {
         int slotIndex = buf.readInt();
         server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            ItemStack toEquip = player.getInventory().getStack(slotIndex);
            if (!toEquip.isEmpty() && OffhandGimmick.isAccessoryItem(toEquip)) {
               if (prog.equippedAccessory != null && !prog.equippedAccessory.isEmpty() && !player.getInventory().insertStack(prog.equippedAccessory.copy())) {
                  player.dropItem(prog.equippedAccessory.copy(), false);
               }

               prog.equippedAccessory = toEquip.copy();
               player.getInventory().removeStack(slotIndex);
               data.markDirty();
               sendAccessoryInfo(player, prog);
            }
         });
      });
      Net121.registerServerReceiver(UNEQUIP_ACCESSORY, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            if (prog.equippedAccessory != null && !prog.equippedAccessory.isEmpty()) {
               if (!player.getInventory().insertStack(prog.equippedAccessory.copy())) {
                  player.dropItem(prog.equippedAccessory.copy(), false);
               }

               prog.equippedAccessory = null;
               data.markDirty();
               sendAccessoryInfo(player, prog);
            }
         }));
      Net121.registerServerReceiver(RECRUIT_EQUIP_ACCESSORY, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         int slotIndex = buf.readInt();
         server.execute(() -> {
            ItemStack toEquip = player.getInventory().getStack(slotIndex);
            if (!toEquip.isEmpty() && OffhandGimmick.isAccessoryItem(toEquip)) {
               RecruitData rd = RecruitData.get(server);
               RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
               RecruitData.RecruitState rs;
               if (recruitOrd >= 1000) {
                  int ci = recruitOrd - 1000;
                  if (ci < 0 || ci >= pr.customRecruits.size()) {
                     return;
                  }

                  rs = pr.customRecruits.get(ci).state;
               } else {
                  if (recruitOrd < 0 || recruitOrd >= pr.recruits.length) {
                     return;
                  }

                  rs = pr.recruits[recruitOrd];
               }

               if (rs.equippedAccessory != null && !rs.equippedAccessory.isEmpty() && !player.getInventory().insertStack(rs.equippedAccessory.copy())) {
                  player.dropItem(rs.equippedAccessory.copy(), false);
               }

               rs.equippedAccessory = toEquip.copy();
               player.getInventory().removeStack(slotIndex);
               rd.markDirty();
               sendRecruitInfo(player);
            }
         });
      });
      Net121.registerServerReceiver(RECRUIT_UNEQUIP_ACCESSORY, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            RecruitData.RecruitState rs;
            if (recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               if (ci < 0 || ci >= pr.customRecruits.size()) {
                  return;
               }

               rs = pr.customRecruits.get(ci).state;
            } else {
               if (recruitOrd < 0 || recruitOrd >= pr.recruits.length) {
                  return;
               }

               rs = pr.recruits[recruitOrd];
            }

            if (rs.equippedAccessory != null && !rs.equippedAccessory.isEmpty()) {
               if (!player.getInventory().insertStack(rs.equippedAccessory.copy())) {
                  player.dropItem(rs.equippedAccessory.copy(), false);
               }

               rs.equippedAccessory = null;
               rd.markDirty();
               sendRecruitInfo(player);
            }
         });
      });
      Net121.registerServerReceiver(BESTIARY_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            BestiaryData bestiary = BestiaryData.get(server);
            Set defeated = bestiary.getDefeatedTypes(player.getUuid());
            PacketByteBuf responseBuf = PacketByteBufs.create();
            responseBuf.writeInt(defeated.size());

            for (String typeId : defeated) {
               responseBuf.writeString(typeId);
            }

            Net121.sendToClient(player, BESTIARY_DATA, responseBuf);
         }));
      Net121.registerServerReceiver(FAVORITE_TOGGLE, (server, player, handler, buf, sender) -> {
         String itemId = buf.readString(128);
         server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            if (!prog.favoriteFood.remove(itemId)) {
               prog.favoriteFood.clear();
               prog.favoriteFood.add(itemId);
            }

            data.markDirty();
            PacketByteBuf sb = PacketByteBufs.create();
            sb.writeInt(prog.favoriteFood.size());

            for (String f : prog.favoriteFood) {
               sb.writeString(f, 128);
            }

            Net121.sendToClient(player, FAVORITE_SYNC, sb);
         });
      });
      Net121.registerServerReceiver(ITEM_FEED, (server, player, handler, buf, sender) -> {
         int invSlot = buf.readInt();
         int targetCode = buf.readInt();
         server.execute(() -> {
            ItemStack stack = player.getInventory().getStack(invSlot);
            if (!stack.isEmpty()) {
               FoodComponent food = (FoodComponent)stack.get(DataComponentTypes.FOOD);
               if (food != null) {
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                  float heal = ((float)food.nutrition() * 2.0F + food.saturation() * 2.0F) * 2.0F;
                  if (prog.soulType == SoulType.KINDNESS) {
                     heal *= 1.25F;
                  }

                  if (targetCode == 0) {
                     if (prog.favoriteFood.contains(itemId)) {
                        heal *= 2.0F;
                     }

                     float newHp = Math.min(player.getMaxHealth(), player.getHealth() + heal);
                     BattleManager.IS_ROLLING = true;

                     try {
                        player.setHealth(newHp);
                     } finally {
                        BattleManager.IS_ROLLING = false;
                     }

                     sendPartyInfo(player);
                  } else {
                     RecruitData rd = RecruitData.get(server);
                     RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
                     int slotCode = targetCode == 1 ? pr.activeRecruit : pr.secondActiveRecruit;
                     if (slotCode < 0) {
                        return;
                     }

                     if (slotCode >= 1000) {
                        int ci = slotCode - 1000;
                        if (ci >= pr.customRecruits.size()) {
                           return;
                        }

                        RecruitData.CustomRecruitState cr = pr.customRecruits.get(ci);
                        float maxHp = RecruitData.maxHp(cr.state);
                        cr.state.hp = Math.min(maxHp, Math.max(0.0F, cr.state.hp) + heal);
                        rd.markDirty();
                     } else if (slotCode < pr.recruits.length) {
                        RecruitData.RecruitState rs = pr.recruits[slotCode];
                        float maxHp = RecruitData.maxHp(rs);
                        rs.hp = Math.min(maxHp, Math.max(0.0F, rs.hp) + heal);
                        rd.markDirty();
                     }

                     sendRecruitInfo(player);
                  }

                  stack.decrement(1);
               }
            }
         });
      });
      Net121.registerServerReceiver(SOUL_ROLL_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            boolean isFree = !prog.freeRollUsed;
            if (!isFree) {
               int diamondCount = 0;

               for (int i = 0; i < player.getInventory().size(); i++) {
                  ItemStack stack = player.getInventory().getStack(i);
                  if (stack.getItem() == Items.DIAMOND) {
                     diamondCount += stack.getCount();
                  }
               }

               if (diamondCount < 10) {
                  player.sendMessage(Text.literal("Not enough diamonds! Need 10 diamonds to reroll.").formatted(Formatting.RED), false);
                  sendSoulInfo(player, prog);
                  return;
               }

               int toRemove = 10;

               for (int ix = 0; ix < player.getInventory().size() && toRemove > 0; ix++) {
                  ItemStack stack = player.getInventory().getStack(ix);
                  if (stack.getItem() == Items.DIAMOND) {
                     int remove = Math.min(toRemove, stack.getCount());
                     stack.decrement(remove);
                     toRemove -= remove;
                  }
               }
            }

            SoulType[] types = Arrays.stream(SoulType.values()).filter(s -> s != SoulType.ORI).toArray(SoulType[]::new);
            SoulType oldSoul = prog.soulType;
            prog.soulType = types[RNG.nextInt(types.length)];
            prog.determinationUsed = false;
            prog.freeRollUsed = true;
            data.markDirty();
            if (isFree) {
               player.sendMessage(Text.literal("Your soul is " + prog.soulType.getDisplayName() + "!").formatted(Formatting.GOLD), false);
            } else {
               player.sendMessage(Text.literal("Rerolled! Your soul is now " + prog.soulType.getDisplayName() + ".").formatted(Formatting.GOLD), false);
            }

            sendPartyInfo(player);
         }));
      Net121.registerServerReceiver(INTRO_DONE, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            prog.introSeen = true;
            prog.worldTypeSeen = true;
            data.markDirty();
            player.setInvulnerable(false);
         }));
      Net121.registerServerReceiver(
         WORLD_TYPE_CHOSEN,
         (server, player, handler, buf, sender) -> {
            boolean roguelite = buf.readBoolean();
            server.execute(
               () -> {
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  prog.worldTypeSeen = true;
                  data.markDirty();
                  RogueliteData rogueDataGlobal = RogueliteData.get(server);
                  if (!rogueDataGlobal.worldTypeChosen) {
                     rogueDataGlobal.worldTypeChosen = true;
                     rogueDataGlobal.markDirty();
                  }

                  if (roguelite) {
                     boolean isOwner = server.isHost(player.getGameProfile()) || player.hasPermissionLevel(2);
                     if (!isOwner) {
                        RogueliteManager.sendRogueInfo(player);
                        return;
                     }

                     RogueliteData rogueData = RogueliteData.get(server);
                     if (!rogueData.rogueliteEnabled) {
                        rogueData.rogueliteEnabled = true;
                        rogueData.markDirty();
                        server.getPlayerManager()
                           .broadcast(Text.literal("[Roguelite] Roguelite mode has been enabled on this world!").formatted(Formatting.LIGHT_PURPLE), false);
                     }
                  }

                  RogueliteManager.sendRogueInfo(player);
                  if (RogueliteData.get(server).rogueliteEnabled) {
                     player.setInvulnerable(false);
                     RogueliteManager.sendRogueliteOpen(player, true);
                  } else if (data.needsClass(player.getUuid())) {
                     player.setInvulnerable(true);
                     Net121.sendToClient(player, CLASS_SELECT, PacketByteBufs.empty());
                  }
               }
            );
         }
      );
      Net121.registerServerReceiver(FORCE_END, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleState state = BattleManager.activeBattles.get(player.getUuid());
            if (state != null) {
               BattleManager.endBattle(state, server, "disconnect");
            }
         }));
      Net121.registerServerReceiver(PARTY_INVITE_TOGGLE, (server, player, handler, buf, sender) -> {
         boolean on = buf.readBoolean();
         server.execute(() -> PlayerPartyManager.setPublicInvites(player.getUuid(), on));
      });
      Net121.registerServerReceiver(PARTY_INVITE, (server, player, handler, buf, sender) -> {
         UUID targetUuid = buf.readUuid();
         server.execute(() -> {
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
            if (target != null) {
               PlayerPartyManager.sendInvite(player, target);
            } else {
               player.sendMessage(Text.literal("That player is not online."), false);
            }
         });
      });
      Net121.registerServerReceiver(PARTY_LEAVE, (server, player, handler, buf, sender) -> server.execute(() -> {
            if (PlayerPartyManager.isInParty(player.getUuid())) {
               PlayerPartyManager.leaveParty(server, player.getUuid(), true);
               PlayerPartyManager.pushPartyState(server, player.getUuid());
               player.sendMessage(Text.literal("You left the party."), false);
            }
         }));
      Net121.registerServerReceiver(PARTY_INVITE_ACCEPT, (server, player, handler, buf, sender) -> {
         String inviterName = buf.readString();
         server.execute(() -> PlayerPartyManager.acceptInvite(player, inviterName));
      });
      Net121.registerServerReceiver(PARTY_INVITE_DECLINE, (server, player, handler, buf, sender) -> server.execute(() -> {
            UUID inviterUuid = PlayerPartyManager.removePendingInvite(player.getUuid());
            if (inviterUuid != null) {
               ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(inviterUuid);
               if (inviter != null) {
                  inviter.sendMessage(Text.literal(player.getName().getString() + " declined your party invite."), false);
               }
            }
         }));
      Net121.registerServerReceiver(BATTLE_CHAT, (server, player, handler, buf, sender) -> {
         String msg = buf.readString(256);
         server.execute(() -> {
            BattleState state = BattleManager.activeBattles.get(player.getUuid());
            if (state != null) {
               if (!msg.isBlank()) {
                  for (UUID pid : state.participantOrder) {
                     if (!pid.equals(BattleState.PARTY_MEMBER_UUID)) {
                        ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pid);
                        if (pp != null) {
                           PacketByteBuf outBuf = PacketByteBufs.create();
                           outBuf.writeUuid(player.getUuid());
                           outBuf.writeString(BattleManager.getPlayerDisplayName(player), 64);
                           outBuf.writeString(msg, 256);
                           Net121.sendToClient(pp, BATTLE_CHAT_MSG, outBuf);
                        }
                     }
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(DD_EXIT, (server, player, handler, buf, sender) -> server.execute(() -> DepthsDescentManager.exitDepths(player)));
      Net121.registerServerReceiver(DD_PLAYER_MOVE, (server, player, handler, buf, sender) -> {
         float px = buf.readFloat();
         float py = buf.readFloat();
         int dir = buf.readInt();
         server.execute(() -> DepthsDescentManager.onPlayerMove(player, px, py, dir));
      });
      Net121.registerServerReceiver(DD_CHEST_OPEN, (server, player, handler, buf, sender) -> {
         int idx = buf.readInt();
         server.execute(() -> DepthsDescentManager.onChestOpen(player, idx));
      });
      Net121.registerServerReceiver(
         DD_STAIRS_INTERACT, (server, player, handler, buf, sender) -> server.execute(() -> DepthsDescentManager.onStairsInteract(player))
      );
      Net121.registerServerReceiver(DD_STAIRS_VOTE, (server, player, handler, buf, sender) -> {
         boolean seal = buf.readBoolean();
         server.execute(() -> DepthsDescentManager.onStairsVote(player, seal));
      });
      UseEntityCallback.EVENT.register((UseEntityCallback)(player, world, hand, entity, hitResult) -> {
         if (world.isClient()) {
            return ActionResult.PASS;
         } else if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
         } else if (!(entity instanceof VillagerEntity)) {
            return ActionResult.PASS;
         } else {
            return player instanceof ServerPlayerEntity sp ? ActionResult.PASS : ActionResult.PASS;
         }
      });
      ServerPlayConnectionEvents.JOIN
         .register(
            (Join)(handler, sender, server) -> {
               ServerPlayerEntity player = handler.getPlayer();
               server.execute(
                  () -> {
                     SoulWalk.onJoin(player);
                     sendBattlePrefs(player);
                     sendStatsExtra(player);
                     LiveCombat.onJoin(player);
                     DarkFountainData dfJoinData = DarkFountainData.get(server);
                     BlockPos exilePos = dfJoinData.pendingExile.remove(player.getUuid());
                     String exileDim = dfJoinData.pendingExileDim.remove(player.getUuid());
                     if (dfJoinData.offlineIn.remove(player.getUuid()) != null) {
                        dfJoinData.markDirty();
                     }

                     if (exilePos != null) {
                        dfJoinData.markDirty();
                        ServerWorld exileWorld = server.getOverworld();
                        Identifier exileId = exileDim != null ? Identifier.tryParse(exileDim) : null;
                        if (exileId != null) {
                           ServerWorld w = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, exileId));
                           if (w != null) {
                              exileWorld = w;
                           }
                        }

                        player.teleport(
                           exileWorld,
                           (double)exilePos.getX() + 0.5,
                           (double)exilePos.getY(),
                           (double)exilePos.getZ() + 0.5,
                           player.getYaw(),
                           player.getPitch()
                        );
                     }

                     BattleData data = BattleData.get(server);
                     BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                     MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
                     DarkFountainData.get(server).getOrRollPlayerTint(player.getUuid());
                     if (prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty()) {
                        WeaponStats ws = WeaponStats.fromStack(prog.equippedWeapon);
                        if (ws != null) {
                           MobLevelManager.applyWeaponStats(player, ws);
                        }
                     }

                     if (prog.equippedArmor != null) {
                        for (int ai = 0; ai < prog.equippedArmor.length; ai++) {
                           ItemStack ap = prog.equippedArmor[ai];
                           if (ap != null && !ap.isEmpty()) {
                              ArmorStats as = ArmorStats.fromStack(ap);
                              if (as != null) {
                                 MobLevelManager.applyArmorStats(player, as, ai);
                              }
                           }
                        }
                     }

                     RogueliteManager.onPlayerJoin(player);
                     DarkWorldSkins.onJoin(player);
                     MoralityManager.syncToPlayer(player);
                     BattleData.PlayerProgress favProg = BattleData.get(server).getOrCreate(player.getUuid());
                     PacketByteBuf favBuf = PacketByteBufs.create();
                     favBuf.writeInt(favProg.favoriteFood.size());

                     for (String f : favProg.favoriteFood) {
                        favBuf.writeString(f, 128);
                     }

                     Net121.sendToClient(player, FAVORITE_SYNC, favBuf);
                     BattleData.PlayerProgress stProg = BattleData.get(server).getOrCreate(player.getUuid());
                     StoryManager.sendState(player, stProg);
                     sendAccessoryInfo(player, prog);
                     sendEquipInfo(player, prog);
                     RogueliteManager.sendRogueInfo(player);
                     PlayerPartyManager.pushPartyState(server, player.getUuid());
                     PacketByteBuf lvBuf = PacketByteBufs.create();
                     lvBuf.writeInt(prog.level);
                     lvBuf.writeInt(prog.exp);
                     lvBuf.writeInt(BattleData.expNeeded(prog.level));
                     lvBuf.writeBoolean(false);
                     Net121.sendToClient(player, LEVEL_UPDATE, lvBuf);
                     RogueliteData rogueData = RogueliteData.get(server);
                     RogueliteData.PlayerRogueData rd = rogueData.players.get(player.getUuid());
                     boolean inRogueliteRun = rd != null && (rd.inRun || rd.deadThisRun);
                     boolean rogueliteWorld = rogueData.rogueliteEnabled;
                     if (!inRogueliteRun && !rogueliteWorld) {
                        if (data.needsClass(player.getUuid())) {
                           player.setInvulnerable(true);
                           Net121.sendToClient(player, CLASS_SELECT, PacketByteBufs.empty());
                        } else if (data.needsIntro(player.getUuid())) {
                           if (prog.soulType == null) {
                              SoulType[] introTypes = Arrays.stream(SoulType.values()).filter(s -> s != SoulType.ORI).toArray(SoulType[]::new);
                              prog.soulType = introTypes[RNG.nextInt(introTypes.length)];
                              prog.freeRollUsed = true;
                              data.markDirty();
                           }

                           sendSoulInfo(player, prog);
                           if (prog.soulRemoved) {
                              Net121.sendToClient(player, SOUL_REMOVED, PacketByteBufs.empty());
                           }

                           PacketByteBuf _ib = PacketByteBufs.create();
                           _ib.writeBoolean(TurnBattleConfig.get().storyMode);
                           Net121.sendToClient(player, INTRO_START, _ib);
                        } else {
                           sendPartyInfo(player);
                        }
                     }
                  }
               );
            }
         );
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         ServerPlayerEntity player = handler.getPlayer();
         SoulWalk.onDisconnect(player);
         DarkFountainData dfLeave = DarkFountainData.get(server);
         DarkFountainData.DarkWorldInstance leftIn = dfLeave.getInstanceByWorldKey(player.getWorld().getRegistryKey());
         if (leftIn != null) {
            dfLeave.offlineIn.put(player.getUuid(), leftIn.id);
            dfLeave.markDirty();
         }

         DarkWorldExit.onDisconnect(player);
         LiveCombat.onLeave(player);
         proximityChatDisabled.remove(player.getUuid());
         BlockPos lightPos = soulRevealingPlayers.remove(player.getUuid());
         if (lightPos != null) {
            removeSoulLightAt(player.getServerWorld(), lightPos);
         }

         BattleState state = BattleManager.activeBattles.get(player.getUuid());
         if (state != null) {
            BattleManager.endBattle(player, state, "disconnect");
         }

         BattleManager.battleCooldowns.remove(player.getUuid());
         if (PlayerPartyManager.isInParty(player.getUuid())) {
            PlayerPartyManager.leaveParty(server, player.getUuid(), true);
         }

         DepthsDescentManager.onPlayerDisconnect(player.getUuid(), server);
         WallClimbManager.forceRelease(player);
      });
      ServerLivingEntityEvents.ALLOW_DEATH.register((AllowDeath)(entity, source, amount) -> true);
      ServerLivingEntityEvents.AFTER_DEATH.register((AfterDeath)(entity, source) -> {
         if (entity instanceof ServerPlayerEntity deadPlayer) {
            BattleState state = BattleManager.activeBattles.get(deadPlayer.getUuid());
            if (state != null) {
               BattleManager.activeBattles.remove(deadPlayer.getUuid());
               BattleManager.endBattle(state, deadPlayer.getServer(), "lose");
            }

            RogueliteManager.onPlayerDeath(deadPlayer);
         }
      });
      ServerPlayerEvents.COPY_FROM.register((CopyFrom)(oldP, newP, alive) -> {
         if (!alive) {
            if (DarkFountainManager.isDarkWorld(oldP.getWorld().getRegistryKey())) {
               if (!newP.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                  newP.getInventory().clone(oldP.getInventory());
                  newP.experienceLevel = oldP.experienceLevel;
                  newP.totalExperience = oldP.totalExperience;
                  newP.experienceProgress = oldP.experienceProgress;
                  newP.setScore(oldP.getScore());
               }
            }
         }
      });
      ServerPlayerEvents.AFTER_RESPAWN.register((AfterRespawn)(oldP, newP, alive) -> {
         SoulWalk.onRespawn(newP);
         BattleManager.activeBattles.remove(newP.getUuid());
         BattleManager.battleCooldowns.remove(newP.getUuid());
         BattleManager.frozenEntityIds.remove(newP.getId());
         WallClimbManager.forceRelease(newP);
         DarkFountainData dfData = DarkFountainData.get(newP.getServer());

         for (DarkFountainData.DarkWorldInstance dfInst : dfData.instances.values()) {
            if (dfInst.playersInside.contains(newP.getUuid()) && !dfInst.getWorldKey().equals(newP.getWorld().getRegistryKey())) {
               dfInst.playersInside.remove(newP.getUuid());
               dfData.markDirty();
            }
         }

         if (!oldP.getWorld().getRegistryKey().equals(newP.getWorld().getRegistryKey()) && PlayerPartyManager.isInParty(newP.getUuid())) {
            PlayerPartyManager.leaveParty(newP.getServer(), newP.getUuid(), true);
            newP.sendMessage(Text.literal("You left your party by changing dimensions."), false);
         }

         BattleData data = BattleData.get(newP.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(newP.getUuid());
         MobLevelManager.applyPlayerStats(newP, prog.level, prog.playerStats);
         if (prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty()) {
            WeaponStats ws = WeaponStats.fromStack(prog.equippedWeapon);
            if (ws != null) {
               MobLevelManager.applyWeaponStats(newP, ws);
            }
         }

         for (int s = 0; s < 2; s++) {
            if (prog.equippedArmor[s] != null && !prog.equippedArmor[s].isEmpty()) {
               ArmorStats as = ArmorStats.fromStack(prog.equippedArmor[s]);
               if (as != null) {
                  MobLevelManager.applyArmorStats(newP, as, s);
               }
            }
         }

         if (prog.soulType == SoulType.DETERMINATION) {
            prog.determinationUsed = false;
            data.markDirty();
         }

         RogueliteManager.onRespawn(newP);
      });
      Net121.registerServerReceiver(
         ROGUE_ENABLE,
         (server, player, handler, buf, sender) -> server.execute(
               () -> {
                  if (!player.hasPermissionLevel(2) && !server.isHost(player.getGameProfile())) {
                     player.sendMessage(Text.literal("[Roguelite] Only the world's host or an operator can turn this on.").formatted(Formatting.RED), false);
                  } else {
                     RogueliteData data = RogueliteData.get(server);
                     if (!data.rogueliteEnabled) {
                        data.rogueliteEnabled = true;
                        data.markDirty();
                        server.getPlayerManager()
                           .broadcast(Text.literal("[Roguelite] Roguelite mode has been enabled on this world!").formatted(Formatting.LIGHT_PURPLE), false);
                     }

                     for (ServerPlayerEntity op : server.getPlayerManager().getPlayerList()) {
                        RogueliteManager.sendRogueInfo(op);
                     }
                  }
               }
            )
      );
      Net121.registerServerReceiver(ROGUE_INFO_REQUEST, (server, player, handler, buf, sender) -> server.execute(() -> RogueliteManager.sendRogueInfo(player)));
      Net121.registerServerReceiver(ROGUE_INVITE, (server, player, handler, buf, sender) -> {
         UUID inviteeUuid = buf.readUuid();
         server.execute(() -> {
            ServerPlayerEntity invitee = server.getPlayerManager().getPlayer(inviteeUuid);
            if (invitee != null) {
               RogueliteManager.sendInvite(player, invitee);
            }
         });
      });
      Net121.registerServerReceiver(ROGUE_BEGIN_RUN, (server, player, handler, buf, sender) -> server.execute(() -> {
            RogueliteManager.beginRun(server, player);
            RogueliteManager.sendRogueInfo(player);
         }));
      Net121.registerServerReceiver(ROGUE_UPGRADE, (server, player, handler, buf, sender) -> {
         int upgradeType = buf.readByte();
         String arg = buf.readString(64);
         server.execute(() -> {
            if (upgradeType == 0) {
               RogueliteManager.buyUpgrade(player, arg);
            } else if (upgradeType == 1) {
               RogueliteManager.chooseSoul(player, arg);
            }

            RogueliteManager.sendRogueInfo(player);
         });
      });
      Net121.registerServerReceiver(ROGUE_ABANDON, (server, player, handler, buf, sender) -> server.execute(() -> RogueliteManager.abandon(player)));
      Net121.registerServerReceiver(ROGUE_LEAVE_PARTY, (server, player, handler, buf, sender) -> server.execute(() -> RogueliteManager.leaveParty(player)));
      Net121.registerServerReceiver(SKILL_UNLOCK, (server, player, handler, buf, sender) -> {
         String skillName = buf.readString(64);
         server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            if (prog.skillPoints > 0) {
               if (SkillTreeDefs.canUnlockUnified(skillName, prog)) {
                  SoulMageSkill sms = SkillTreeDefs.soulMageSkillByName(skillName);
                  if (sms != null && sms.getSoul() != null && sms.getSoul() != prog.soulType) {
                     player.sendMessage(Text.literal("Only unlockable while your current soul trait is " + sms.getSoul().getDisplayName() + "."), true);
                  } else {
                     if (prog.playerRace == CharacterRace.MONSTER) {
                        MonsterSkill ms = SkillTreeDefs.monsterSkillByName(skillName);
                        if (ms != null && ms.soulsRequired() > 0 && prog.absorbedSouls.size() < ms.soulsRequired()) {
                           player.sendMessage(Text.literal("Requires " + ms.soulsRequired() + " absorbed souls to unlock."), true);
                           return;
                        }
                     }

                     if (SkillTreeDefs.prereqsMetUnified(skillName, prog.unlockedSkills, prog)) {
                        int needLv = SkillTreeDefs.levelRequiredFor(skillName);
                        if (prog.level < needLv) {
                           player.sendMessage(Text.literal("Reach LV " + needLv + " to learn that."), true);
                        } else if (!prog.unlockedSkills.contains(skillName)) {
                           int cost = SkillTreeDefs.nodeCostUnified(skillName, prog);
                           if (prog.skillPoints >= cost) {
                              prog.skillPoints -= cost;
                              prog.unlockedSkills.add(skillName);
                              if (!hasSkillSlotRoom(prog, skillName)) {
                                 prog.unequippedSkills.add(skillName);
                              }

                              data.markDirty();
                              if (SkillTreeDefs.isStatNode(skillName)) {
                                 sendPartyInfo(player);
                              } else {
                                 sendSkillInfo(player, prog);
                              }
                           }
                        }
                     }
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(SKILL_EQUIP_TOGGLE, (server, player, handler, buf, sender) -> {
         int slotCode = buf.readInt();
         String skillName = buf.readString(64);
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            if (slotCode >= 1000) {
               int ci = slotCode - 1000;
               if (ci >= 0 && ci < pr.customRecruits.size()) {
                  RecruitData.CustomRecruitState cr = pr.customRecruits.get(ci);
                  RecruitData.RecruitState rs = cr.state;
                  if (rs.unlockedSkills.contains(skillName)) {
                     if (rs.unequippedSkills.contains(skillName)) {
                        if (!hasSkillSlotRoom(rs, skillName, cr.playerClass, cr.soulType)) {
                           return;
                        }

                        rs.unequippedSkills.remove(skillName);
                     } else {
                        rs.unequippedSkills.add(skillName);
                     }

                     rd.markDirty();
                     sendRecruitInfo(player);
                  }
               }
            } else {
               BattleData data = BattleData.get(server);
               BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
               if (prog.unlockedSkills.contains(skillName)) {
                  if (prog.unequippedSkills.contains(skillName)) {
                     if (!hasSkillSlotRoom(prog, skillName)) {
                        return;
                     }

                     prog.unequippedSkills.remove(skillName);
                  } else {
                     prog.unequippedSkills.add(skillName);
                  }

                  data.markDirty();
                  sendSkillInfo(player, prog);
               } else {
                  for (Recruit r : Recruit.values()) {
                     RecruitData.RecruitState rs = pr.get(r);
                     if (rs != null && rs.unlockedSkills.contains(skillName)) {
                        if (rs.unequippedSkills.contains(skillName)) {
                           if (!hasSkillSlotRoom(rs, skillName, r.playerClass, null)) {
                              return;
                           }

                           rs.unequippedSkills.remove(skillName);
                        } else {
                           rs.unequippedSkills.add(skillName);
                        }

                        rd.markDirty();
                        sendRecruitInfo(player);
                        return;
                     }
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(PARTY_SKILL_UNLOCK, (server, player, handler, buf, sender) -> {
         int memberOrd = buf.readByte();
         String skillName = buf.readString(64);
         server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            PartyMember[] members = PartyMember.values();
            if (memberOrd >= 0 && memberOrd < members.length) {
               PartyMember member = members[memberOrd];
               if (member != PartyMember.NONE && member.playerClass != null) {
                  if (prog.partySkillPoints[memberOrd] > 0) {
                     if (SkillTreeDefs.prereqsMet(skillName, prog.partyUnlockedSkills[memberOrd], member.playerClass)) {
                        if (!prog.partyUnlockedSkills[memberOrd].contains(skillName)) {
                           int cost = SkillTreeDefs.nodeCost(skillName, member.playerClass);
                           if (prog.partySkillPoints[memberOrd] >= cost) {
                              if (SkillTreeDefs.isValidForClass(skillName, member.playerClass)) {
                                 prog.partySkillPoints[memberOrd] = prog.partySkillPoints[memberOrd] - cost;
                                 prog.partyUnlockedSkills[memberOrd].add(skillName);
                                 data.markDirty();
                                 sendSkillInfo(player, prog);
                              }
                           }
                        }
                     }
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(ROGUE_CARD_CHOSEN, (server, player, handler, buf, sender) -> {
         int index = buf.readVarInt();
         server.execute(() -> {
            if (!DepthsRun.onCardChoice(player, index)) {
               RogueliteManager.onCardChoice(player, index);
            }
         });
      });
      Net121.registerServerReceiver(BOSS_BEACON_SET_DIFFICULTY, (server, player, handler, buf, sender) -> {
         int bx = buf.readInt();
         int by = buf.readInt();
         int bz = buf.readInt();
         int diff = buf.readInt();
         server.execute(() -> BossBeaconManager.onSetDifficulty(player, new BlockPos(bx, by, bz), diff));
      });
      Net121.registerServerReceiver(BOSS_BEACON_BEGIN, (server, player, handler, buf, sender) -> {
         int bx = buf.readInt();
         int by = buf.readInt();
         int bz = buf.readInt();
         server.execute(() -> BossBeaconManager.onBeginBattle(player, new BlockPos(bx, by, bz)));
      });
      Net121.registerServerReceiver(BOSS_BEACON_LEAVE, (server, player, handler, buf, sender) -> {
         int bx = buf.readInt();
         int by = buf.readInt();
         int bz = buf.readInt();
         server.execute(() -> BossBeaconManager.onPlayerLeave(player, new BlockPos(bx, by, bz)));
      });
      Net121.registerServerReceiver(DARK_FOUNTAIN_RESET, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            prog.darkFountainSeals = 0;
            data.markDirty();
            DepthsDescentData ddData = DepthsDescentData.get(server);
            ddData.globalSealedCount = 0;

            for (DepthsDescentData.Instance inst : ddData.instances.values()) {
               inst.chestOpenedByPlayers.clear();
            }

            ddData.markDirty();
            PacketByteBuf rb = PacketByteBufs.create();
            rb.writeInt(0);
            Net121.sendToClient(player, DARK_FOUNTAIN_SEALS, rb);
         }));
      Net121.registerServerReceiver(QUESTLINE_RETRY, (server, player, handler, buf, sender) -> {
         String questlineId = buf.readString(32);
         server.execute(() -> QuestlineManager.retryQuestlineBattle(player));
      });
      Net121.registerServerReceiver(DARK_WORLD_CREATE_CONFIRM, (server, player, handler, buf, sender) -> {
         boolean uniqueColor = buf.readBoolean();
         String shape = buf.readableBytes() > 0 ? buf.readString(256) : "minecraft:overworld";
         List groups = new ArrayList<>();
         int n = buf.readableBytes() > 0 ? Math.min(256, buf.readVarInt()) : 0;

         for (int i = 0; i < n; i++) {
            groups.add(buf.readString(4096));
         }

         server.execute(() -> DarkFountainManager.onCreateConfirm(player, uniqueColor, shape, groups));
      });
      Net121.registerServerReceiver(DARK_FOUNTAIN_CANCEL, (server, player, handler, buf, sender) -> server.execute(() -> DarkFountainManager.onCancel(player)));
      Net121.registerServerReceiver(
         DARK_FOUNTAIN_SEAL_CONFIRM, (server, player, handler, buf, sender) -> server.execute(() -> DarkFountainManager.onSealConfirm(player))
      );
      Net121.registerServerReceiver(RECRUIT_SET_ACTIVE, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            if (recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               if (ci < pr.customRecruits.size()) {
                  pr.activeRecruit = recruitOrd;
                  if (pr.secondActiveRecruit == recruitOrd) {
                     pr.secondActiveRecruit = -1;
                  }
               }
            } else if (recruitOrd >= 0 && recruitOrd < Recruit.values().length) {
               RecruitData.RecruitState rs = pr.recruits[recruitOrd];
               if (rs.unlocked) {
                  pr.activeRecruit = recruitOrd;
                  if (pr.secondActiveRecruit == recruitOrd) {
                     pr.secondActiveRecruit = -1;
                  }
               }
            } else {
               pr.activeRecruit = -1;
            }

            rd.markDirty();
            sendRecruitInfo(player);
         });
      });
      Net121.registerServerReceiver(RECRUIT_STAT_SPEND, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         int statIdx = buf.readInt();
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            if (recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               if (ci >= 0 && ci < pr.customRecruits.size()) {
                  CharStats s = pr.customRecruits.get(ci).state.stats;
                  if (s.unspent > 0) {
                     boolean spent = switch (statIdx) {
                        case 0 -> {
                           s.offense++;
                           s.unspent--;
                           yield true;
                        }
                        case 1 -> {
                           s.defense++;
                           s.unspent--;
                           yield true;
                        }
                        case 2 -> {
                           s.magic++;
                           s.unspent--;
                           yield true;
                        }
                        case 3 -> {
                           s.iq++;
                           s.unspent--;
                           yield true;
                        }
                        case 4 -> {
                           s.vitality++;
                           s.unspent--;
                           yield true;
                        }
                        case 5 -> {
                           if (!s.isMaxed(5)) {
                              s.luck++;
                              s.unspent--;
                              yield true;
                           } else {
                              yield false;
                           }
                        }
                        default -> false;
                     };
                     if (spent) {
                        rd.markDirty();
                        sendRecruitInfo(player);
                     }
                  }
               }
            } else if (recruitOrd >= 0 && recruitOrd < Recruit.values().length) {
               RecruitData.RecruitState rs = pr.recruits[recruitOrd];
               if (rs.unlocked && rs.stats.unspent > 0) {
                  CharStats s = rs.stats;

                  boolean spent = switch (statIdx) {
                     case 0 -> {
                        s.offense++;
                        s.unspent--;
                        yield true;
                     }
                     case 1 -> {
                        s.defense++;
                        s.unspent--;
                        yield true;
                     }
                     case 2 -> {
                        s.magic++;
                        s.unspent--;
                        yield true;
                     }
                     case 3 -> {
                        s.iq++;
                        s.unspent--;
                        yield true;
                     }
                     case 4 -> {
                        s.vitality++;
                        s.unspent--;
                        yield true;
                     }
                     case 5 -> {
                        if (!s.isMaxed(5)) {
                           s.luck++;
                           s.unspent--;
                           yield true;
                        } else {
                           yield false;
                        }
                     }
                     default -> false;
                  };
                  if (spent) {
                     rd.markDirty();
                     sendRecruitInfo(player);
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(RECRUIT_SKILL_UNLOCK, (server, player, handler, buf, sender) -> {
         int recruitOrd = buf.readInt();
         String skillName = buf.readString(64);
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            if (recruitOrd >= 1000) {
               int ci = recruitOrd - 1000;
               if (ci >= 0 && ci < pr.customRecruits.size()) {
                  RecruitData.CustomRecruitState cr = pr.customRecruits.get(ci);
                  RecruitData.RecruitState rs = cr.state;
                  PlayerClass cls = cr.playerClass != null ? cr.playerClass : PlayerClass.HUMAN_MAGE;
                  List tree = SkillTreeDefs.getTree(cls, cr.soulType);
                  SkillTreeDefs.Node node = tree == null ? null : tree.stream().filter(n -> n.skill().equals(skillName)).findFirst().orElse(null);
                  if (node == null) {
                     node = SkillTreeDefs.findHumanNode(skillName);
                  }

                  if (node != null) {
                     SoulMageSkill traitSkill = SkillTreeDefs.soulMageSkillByName(skillName);
                     if (traitSkill == null || traitSkill.getSoul() == null || traitSkill.getSoul() == cr.soulType) {
                        if (!rs.unlockedSkills.contains(skillName)) {
                           if (rs.level >= SkillTreeDefs.levelRequiredFor(skillName)) {
                              if (rs.skillPoints >= node.cost()) {
                                 for (String pre : node.prereqs()) {
                                    if (!rs.unlockedSkills.contains(pre)) {
                                       return;
                                    }
                                 }

                                 rs.skillPoints = rs.skillPoints - node.cost();
                                 rs.unlockedSkills.add(skillName);
                                 cr.playerClass = SkillTreeDefs.leaningClass(rs.unlockedSkills, cr.playerClass);
                                 if (!hasSkillSlotRoom(rs, skillName, cr.playerClass, cr.soulType)) {
                                    rs.unequippedSkills.add(skillName);
                                 }

                                 if (SkillTreeDefs.isStatNode(skillName)) {
                                    int statIdx = SkillTreeDefs.statNodeStatIndex(skillName);
                                    int amount = SkillTreeDefs.statNodeAmount(skillName);
                                    if (statIdx >= 0) {
                                       switch (statIdx) {
                                          case 0:
                                             rs.stats.offense += amount;
                                             break;
                                          case 1:
                                             rs.stats.defense += amount;
                                             break;
                                          case 2:
                                             rs.stats.magic += amount;
                                             break;
                                          case 3:
                                             rs.stats.iq += amount;
                                             break;
                                          case 4:
                                             rs.stats.vitality += amount;
                                             break;
                                          case 5:
                                             rs.stats.luck += amount;
                                       }
                                    }
                                 }

                                 rd.markDirty();
                                 sendRecruitInfo(player);
                              }
                           }
                        }
                     }
                  }
               }
            } else if (recruitOrd >= 0 && recruitOrd < Recruit.values().length) {
               Recruit recruit = Recruit.values()[recruitOrd];
               RecruitData.RecruitState rsx = pr.recruits[recruitOrd];
               if (rsx.unlocked) {
                  List treex = SkillTreeDefs.getRecruitTree(recruit);
                  SkillTreeDefs.Node nodex = treex.stream().filter(n -> n.skill().equals(skillName)).findFirst().orElse(null);
                  if (nodex != null) {
                     if (!rsx.unlockedSkills.contains(skillName)) {
                        if (rsx.level >= SkillTreeDefs.levelRequiredFor(skillName)) {
                           if (rsx.skillPoints >= nodex.cost()) {
                              for (String prex : nodex.prereqs()) {
                                 if (!rsx.unlockedSkills.contains(prex)) {
                                    return;
                                 }
                              }

                              rsx.skillPoints = rsx.skillPoints - nodex.cost();
                              rsx.unlockedSkills.add(skillName);
                              if (!hasSkillSlotRoom(rsx, skillName, recruit.playerClass, null)) {
                                 rsx.unequippedSkills.add(skillName);
                              }

                              if (SkillTreeDefs.isStatNode(skillName)) {
                                 int statIdx = SkillTreeDefs.statNodeStatIndex(skillName);
                                 int amount = SkillTreeDefs.statNodeAmount(skillName);
                                 if (statIdx >= 0) {
                                    switch (statIdx) {
                                       case 0:
                                          rsx.stats.offense += amount;
                                          break;
                                       case 1:
                                          rsx.stats.defense += amount;
                                          break;
                                       case 2:
                                          rsx.stats.magic += amount;
                                          break;
                                       case 3:
                                          rsx.stats.iq += amount;
                                          break;
                                       case 4:
                                          rsx.stats.vitality += amount;
                                          break;
                                       case 5:
                                          rsx.stats.luck += amount;
                                    }
                                 }
                              }

                              rd.markDirty();
                              sendRecruitInfo(player);
                           }
                        }
                     }
                  }
               }
            }
         });
      });
      Net121.registerServerReceiver(STORY_CHOICE, (server, player, handler, buf, sender) -> {
         int choiceIdx = buf.readInt();
         server.execute(() -> {
            if (!QuestlineManager.isQuestlineBattle(player.getUuid()) && BattleData.get(server).getOrCreate(player.getUuid()).activeQuestlineId == null) {
               StoryManager.onStoryChoice(player, choiceIdx);
            } else {
               QuestlineManager.onQuestlineChoice(player, choiceIdx);
            }
         });
      });
      Net121.registerServerReceiver(VESSEL_SET, (server, player, handler, buf, sender) -> {
         int choice = buf.readInt();
         server.execute(() -> {
            BattleData data = BattleData.get(server);
            BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
            prog.vesselChoice = Math.max(0, Math.min(1, choice));
            data.markDirty();
            PacketByteBuf vbuf = PacketByteBufs.create();
            vbuf.writeInt(prog.vesselChoice);
            Net121.sendToClient(player, VESSEL_INFO, vbuf);
         });
      });
      Net121.registerServerReceiver(STORY_INSPECT, (server, player, handler, buf, sender) -> {
         String blockId = buf.readString(64);
         server.execute(() -> {
            if ("diamond_ore".equals(blockId) || "deepslate_diamond_ore".equals(blockId)) {
               StoryManager.onDiamondOreInspect(player);
            }
         });
      });
      Net121.registerServerReceiver(STORY_DIALOGUE_DONE, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleManager.playersInStoryDialogue.remove(player.getUuid());
            BattleManager.storyDialogueEnteredTick.remove(player.getUuid());
            player.setInvulnerable(false);
            BattleData qlData = BattleData.get(server);
            BattleData.PlayerProgress qlProg = qlData.getOrCreate(player.getUuid());
            if (qlProg.activeQuestlineId != null) {
               QuestlineManager.onDialogueDoneForQuestline(player);
            } else {
               if (StoryManager.pendingKrisAmbushArm.remove(player.getUuid())) {
                  StoryManager.armKrisAmbush(player);
               } else if (StoryManager.pendingKrisBattle.remove(player.getUuid())) {
                  StoryManager.spawnKrisAndBattle(player);
               }
            }
         }));
      Net121.registerServerReceiver(STORY_CONTINUE, (server, player, handler, buf, sender) -> server.execute(() -> {
            BattleManager.playersInStoryDialogue.remove(player.getUuid());
            BattleManager.storyDialogueEnteredTick.remove(player.getUuid());
            player.setInvulnerable(false);
            BattleData data2 = BattleData.get(server);
            BattleData.PlayerProgress prog2 = data2.getOrCreate(player.getUuid());
            if (prog2.storyUnlockedChapter == 1 && prog2.storyQuestComplete) {
               prog2.storyQuestIndex++;
               prog2.storyQuestStarted = true;
               prog2.storyQuestComplete = false;
               data2.markDirty();
               StoryManager.sendState(player, prog2);
            }
         }));
      Net121.registerServerReceiver(RECRUIT_ACTIVATE, (server, player, handler, buf, sender) -> {
         int idx = buf.readInt();
         server.execute(() -> {
            RecruitData rd = RecruitData.get(server);
            RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
            if (idx >= 0 && idx < Recruit.values().length && pr.recruits[idx].unlocked) {
               pr.activeRecruit = idx;
               if (pr.secondActiveRecruit == idx) {
                  pr.secondActiveRecruit = -1;
               }
            } else {
               pr.activeRecruit = -1;
            }

            rd.markDirty();
            sendRecruitInfo(player);
         });
      });
      Net121.registerServerReceiver(
         QUESTLINE_START,
         (server, player, handler, buf, sender) -> {
            String questlineId = buf.readString(64);
            server.execute(
               () -> {
                  BattleData data = BattleData.get(server);
                  BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                  if (prog.activeQuestlineId == null) {
                     if (!prog.completedQuestlines.contains(questlineId)) {
                        QuestlineManager.QuestlineMeta meta = QuestlineManager.ALL_QUESTLINES
                           .stream()
                           .filter(q -> q.id().equals(questlineId))
                           .findFirst()
                           .orElse(null);
                        if (meta == null || prog.level >= meta.levelRequired()) {
                           if (meta != null) {
                              for (String pre : meta.prereqs()) {
                                 if (!prog.completedQuestlines.contains(pre)) {
                                    return;
                                 }
                              }
                           }

                           prog.activeQuestlineId = questlineId;
                           prog.questlineNodeIndex = 0;
                           prog.questlineNodeDone = false;
                           data.markDirty();
                           QuestlineManager.onQuestlineStart(player, questlineId);
                           sendQuestlineState(player, prog);
                        }
                     }
                  }
               }
            );
         }
      );
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> SoulWalk.tick(server));
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> Shopkeeper.tick(server));
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> DarknerBosses.tick(server));
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         if (server.getTicks() % 20 == 7) {
            LiveCombat.healNaN(server);
         }
      });
      ServerTickEvents.END_SERVER_TICK
         .register(
            (EndTick)server -> {
               BattleManager.tickRollingHealth(server);
               BattleManager.tickEnemyResultTimeout(server);
               BattleManager.tickQueuedTurns(server);
               BattleManager.tickBattleTicks(server);
               BossBeaconManager.tick();
               if (!eliteTeamsInitialized) {
                  EliteMobManager.ensureEliteTeams(server);
                  eliteTeamsInitialized = true;
               }

               if (!soulRevealingPlayers.isEmpty()) {
                  Iterator it2 = soulRevealingPlayers.entrySet().iterator();

                  while (it2.hasNext()) {
                     java.util.Map.Entry e = it2.next();
                     ServerPlayerEntity sp = server.getPlayerManager().getPlayer(e.getKey());
                     if (sp == null) {
                        it2.remove();
                     } else {
                        BlockPos newPos = sp.getBlockPos();
                        if (!newPos.equals(e.getValue())) {
                           removeSoulLightAt(sp.getServerWorld(), e.getValue());
                           removeSoulLightAt(sp.getServerWorld(), newPos);
                           e.setValue(newPos);
                           placeSoulLight(sp);
                        }
                     }
                  }
               }

               DarkWorldExit.tick(server);
               LiveCombat.tick(server);
               if (server.getOverworld().getTime() % 20L == 0L) {
                  RogueliteManager.tick(server);
                  ((BooleanRule)server.getGameRules().get(GameRules.NATURAL_REGENERATION)).set(false, server);

                  for (ServerWorld w : server.getWorlds()) {
                     BooleanRule regen = (BooleanRule)w.getGameRules().get(GameRules.NATURAL_REGENERATION);
                     if (regen.get()) {
                        regen.set(false, server);
                     }
                  }
               }

               if (server.getOverworld().getTime() % 600L == 0L) {
                  ServerWorld overworld = server.getOverworld();
                  Random rng = new Random();

                  for (ServerPlayerEntity sp2 : overworld.getPlayers()) {
                     if (rng.nextInt(5) == 0) {
                        int angle = rng.nextInt(360);
                        int tx = sp2.getBlockX() + (int)(Math.cos(Math.toRadians((double)angle)) * 20.0);
                        int tz = sp2.getBlockZ() + (int)(Math.sin(Math.toRadians((double)angle)) * 20.0);
                        ChunkPos cp = new ChunkPos(tx >> 4, tz >> 4);
                        if (overworld.isChunkLoaded(cp.x, cp.z)) {
                           BlockPos scanPos = new BlockPos(tx, sp2.getBlockY(), tz);

                           for (int scanY = sp2.getBlockY(); scanY > -64; scanY--) {
                              if (!overworld.getBlockState(new BlockPos(tx, scanY - 1, tz)).isAir()
                                 && overworld.getBlockState(new BlockPos(tx, scanY, tz)).isAir()) {
                                 scanPos = new BlockPos(tx, scanY, tz);
                                 break;
                              }
                           }

                           if (overworld.getBlockState(scanPos).isAir()
                              && overworld.getEntitiesByClass(SavePointEntity.class, new Box(scanPos).expand(30.0), e -> true).isEmpty()) {
                              SavePointEntity sp3 = new SavePointEntity(SAVE_POINT_ENTITY_TYPE, overworld);
                              sp3.refreshPositionAndAngles((double)scanPos.getX() + 0.5, (double)scanPos.getY(), (double)scanPos.getZ() + 0.5, 0.0F, 0.0F);
                              overworld.spawnEntity(sp3);
                           }
                        }
                     }
                  }
               }

               for (ServerPlayerEntity hp : server.getPlayerManager().getPlayerList()) {
                  if (hp.getHungerManager().getFoodLevel() < 20) {
                     hp.getHungerManager().setFoodLevel(20);
                  }
               }

               long now = server.getOverworld().getTime();
               Iterator it = BattleManager.runInvincibleUntil.entrySet().iterator();

               while (it.hasNext()) {
                  java.util.Map.Entry entry = it.next();
                  if (now >= entry.getValue()) {
                     ServerPlayerEntity p = server.getPlayerManager().getPlayer(entry.getKey());
                     if (p != null) {
                        p.setInvulnerable(false);
                     }

                     it.remove();
                  }
               }

               long STORY_DIALOGUE_TIMEOUT_TICKS = 2400L;
               Iterator storyIt = BattleManager.storyDialogueEnteredTick.entrySet().iterator();

               while (storyIt.hasNext()) {
                  java.util.Map.Entry entry = storyIt.next();
                  if (now - entry.getValue() >= 2400L) {
                     BattleManager.playersInStoryDialogue.remove(entry.getKey());
                     ServerPlayerEntity p = server.getPlayerManager().getPlayer(entry.getKey());
                     if (p != null) {
                        p.setInvulnerable(false);
                     }

                     storyIt.remove();
                  }
               }

               DarkFountainManager.tick(server);
               WallClimbManager.tick(server);
               DarkFountainManager.tickScheduled(server);
               DepthsDescentManager.tick(server);
               StoryManager.tick(server);
               if (now % 6L == 0L) {
                  for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                     UUID sid = sp.getUuid();
                     if (BattleManager.isInBattle(sid)) {
                        if (nearHazardPlayers.remove(sid)) {
                           setSoulGlow(server, sp, null, false);
                        }

                        lastPlayerHp.put(sid, sp.getHealth());
                     } else {
                        BattleData.PlayerProgress prg = BattleData.get(server).getOrCreate(sid);
                        if (!prg.soulRemoved && !sealingPlayers.contains(sid) && !SoulWalk.isOut(sid)) {
                           boolean near = isNearHazard(sp);
                           boolean was = nearHazardPlayers.contains(sid);
                           if (near != was) {
                              if (near) {
                                 nearHazardPlayers.add(sid);
                              } else {
                                 nearHazardPlayers.remove(sid);
                              }

                              setSoulGlow(server, sp, prg.soulType, near);
                           }
                        } else if (nearHazardPlayers.remove(sid)) {
                           setSoulGlow(server, sp, null, false);
                        }

                        float prev = lastPlayerHp.getOrDefault(sid, sp.getHealth());
                        float curr = sp.getHealth();
                        if (curr < prev - 0.05F) {
                           BattleData.PlayerProgress dmgPrg = BattleData.get(server).getOrCreate(sid);
                           float dmg = prev - curr;
                           PacketByteBuf hb = PacketByteBufs.create();
                           hb.writeFloat(curr);
                           hb.writeFloat(sp.getMaxHealth());
                           hb.writeFloat(dmg);
                           hb.writeString(dmgPrg.vesselName != null && !dmgPrg.vesselName.isEmpty() ? dmgPrg.vesselName : sp.getName().getString());
                           Net121.sendToClient(sp, WORLD_DAMAGE_HUD, hb);
                           PlayerPartyManager.broadcastPartyDamage(server, sid, curr, sp.getMaxHealth(), dmg);
                           PlayerPartyManager.pushPartyState(server, sid);
                        } else if (curr > prev + 0.05F) {
                           BattleData.PlayerProgress healPrg = BattleData.get(server).getOrCreate(sid);
                           float amt = curr - prev;
                           String healName = healPrg.vesselName != null && !healPrg.vesselName.isEmpty() ? healPrg.vesselName : sp.getName().getString();
                           PacketByteBuf hb = PacketByteBufs.create();
                           hb.writeFloat(curr);
                           hb.writeFloat(sp.getMaxHealth());
                           hb.writeFloat(amt);
                           hb.writeString(healName);
                           hb.writeBoolean(false);
                           Net121.sendToClient(sp, OVERWORLD_HEAL, hb);
                           PlayerPartyManager.broadcastPartyHeal(server, sid, curr, sp.getMaxHealth(), amt);
                        }

                        lastPlayerHp.put(sid, curr);
                     }
                  }
               }

               if (now % 6L == 0L) {
                  for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                     if (BattleManager.isInBattle(player.getUuid())) {
                        if (nearHazardPlayers.remove(player.getUuid())) {
                           setSoulGlow(server, player, null, false);
                        }
                     } else {
                        BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(player.getUuid());
                        if (!prog.soulRemoved && !sealingPlayers.contains(player.getUuid()) && !SoulWalk.isOut(player.getUuid())) {
                           boolean near = isNearHazard(player);
                           boolean was = nearHazardPlayers.contains(player.getUuid());
                           if (near != was) {
                              if (near) {
                                 nearHazardPlayers.add(player.getUuid());
                              } else {
                                 nearHazardPlayers.remove(player.getUuid());
                              }

                              setSoulGlow(server, player, prog.soulType, near);
                           }
                        } else if (nearHazardPlayers.remove(player.getUuid())) {
                           setSoulGlow(server, player, null, false);
                        }

                        float prev = lastPlayerHp.getOrDefault(player.getUuid(), player.getHealth());
                        float curr = player.getHealth();
                        if (curr < prev - 0.1F) {
                           float dmg = prev - curr;
                           PacketByteBuf hb = PacketByteBufs.create();
                           hb.writeFloat(curr);
                           hb.writeFloat(player.getMaxHealth());
                           hb.writeFloat(dmg);
                           BattleData.PlayerProgress dmgProg = BattleData.get(server).getOrCreate(player.getUuid());
                           hb.writeString(dmgProg.vesselName != null && !dmgProg.vesselName.isEmpty() ? dmgProg.vesselName : player.getName().getString());
                           Net121.sendToClient(player, WORLD_DAMAGE_HUD, hb);
                        } else if (curr > prev + 0.1F) {
                           float amt = curr - prev;
                           BattleData.PlayerProgress healProg = BattleData.get(server).getOrCreate(player.getUuid());
                           String healName = healProg.vesselName != null && !healProg.vesselName.isEmpty() ? healProg.vesselName : player.getName().getString();
                           PacketByteBuf hb = PacketByteBufs.create();
                           hb.writeFloat(curr);
                           hb.writeFloat(player.getMaxHealth());
                           hb.writeFloat(amt);
                           hb.writeString(healName);
                           hb.writeBoolean(false);
                           Net121.sendToClient(player, OVERWORLD_HEAL, hb);
                           PlayerPartyManager.broadcastPartyHeal(server, player.getUuid(), curr, player.getMaxHealth(), amt);
                        }

                        lastPlayerHp.put(player.getUuid(), curr);
                     }
                  }
               }
            }
         );
      ServerEntityEvents.ENTITY_LOAD.register((Load)(entity, world) -> {
         if (entity instanceof MobEntity mob && !world.isClient()) {
            if (Darkners.onEntityLoad(entity, world)) {
               return;
            }

            MobLevelManager.initMob(mob);
            if (mob instanceof DarknerBossEntity boss) {
               DarknerBosses.onLoad(boss, world);
            }
         }
      });
      TurnBattleStructures.register();
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, registryAccess, env) -> {
               LiteralArgumentBuilder spawnBossCmd = (LiteralArgumentBuilder<ServerCommandSource>)((LiteralArgumentBuilder)CommandManager.literal("spawnboss")
                     .requires(src -> src.hasPermissionLevel(2)))
                  .executes(ctx -> spawnBossOnPlayer((ServerCommandSource)ctx.getSource(), -1));

               for (int t = 0; t < EliteMobManager.TIER_NAMES.length; t++) {
                  int tier = t;
                  spawnBossCmd.then(
                     CommandManager.literal(EliteMobManager.TIER_NAMES[tier].toLowerCase(Locale.ROOT))
                        .executes(ctx -> spawnBossOnPlayer((ServerCommandSource)ctx.getSource(), tier))
                  );
               }

               dispatcher.register(spawnBossCmd);
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("savepoint").requires(src -> src.hasPermissionLevel(2)))
                     .executes(ctx -> {
                        ServerCommandSource source = (ServerCommandSource)ctx.getSource();
                        ServerPlayerEntity op = source.getPlayer();
                        if (op == null) {
                           return 0;
                        } else {
                           SavePointEntity sp = new SavePointEntity(SAVE_POINT_ENTITY_TYPE, op.getWorld());
                           sp.refreshPositionAndAngles(op.getX(), op.getY(), op.getZ(), 0.0F, 0.0F);
                           op.getWorld().spawnEntity(sp);
                           source.sendFeedback(() -> Text.literal("Save point placed."), false);
                           return 1;
                        }
                     })
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("textbox").requires(src -> src.hasPermissionLevel(2)))
                     .then(
                        CommandManager.argument("player", EntityArgumentType.players())
                           .then(
                              CommandManager.argument("withSound", BoolArgumentType.bool())
                                 .then(CommandManager.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                                    Collection targets = EntityArgumentType.getPlayers(ctx, "player");
                                    boolean withSound = BoolArgumentType.getBool(ctx, "withSound");
                                    String raw = StringArgumentType.getString(ctx, "text");
                                    List pages = parseTextboxPages(raw);

                                    for (ServerPlayerEntity target : targets) {
                                       PacketByteBuf buf = PacketByteBufs.create();
                                       buf.writeInt(pages.size());

                                       for (String page : pages) {
                                          buf.writeString(page, 4096);
                                       }

                                       buf.writeBoolean(withSound);
                                       Net121.sendToClient(target, TEXTBOX_SHOW, buf);
                                    }

                                    return targets.size();
                                 }))
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("soulboundcharacter")
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("remake").requires(src -> src.hasPermissionLevel(2)))
                              .executes(ctx -> {
                                 ServerPlayerEntity self = ((ServerCommandSource)ctx.getSource()).getPlayer();
                                 if (self == null) {
                                    return 0;
                                 } else {
                                    self.setInvulnerable(true);
                                    Net121.sendToClient(self, CLASS_SELECT, PacketByteBufs.empty());
                                    return 1;
                                 }
                              }))
                           .then(CommandManager.argument("player", EntityArgumentType.player()).executes(ctx -> {
                              ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                              target.setInvulnerable(true);
                              Net121.sendToClient(target, CLASS_SELECT, PacketByteBufs.empty());
                              ((ServerCommandSource)ctx.getSource()).sendMessage(Text.literal("Opened character creation for " + target.getName().getString()));
                              return 1;
                           }))
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundskillreset")
                        .executes(
                           ctx -> {
                              ServerPlayerEntity self = ((ServerCommandSource)ctx.getSource()).getPlayer();
                              if (self == null) {
                                 ((ServerCommandSource)ctx.getSource())
                                    .sendError(Text.literal("Only players can reset their own skills - name a player to reset someone else's."));
                                 return 0;
                              } else {
                                 return resetOwnSkills(self);
                              }
                           }
                        ))
                     .then(
                        ((RequiredArgumentBuilder)CommandManager.argument("player", EntityArgumentType.player()).requires(src -> src.hasPermissionLevel(2)))
                           .executes(
                              ctx -> {
                                 ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                 String name = target.getName().getString();
                                 if (BattleManager.isInBattle(target.getUuid())) {
                                    ((ServerCommandSource)ctx.getSource()).sendError(Text.literal(name + " is in a battle - try again once it's over."));
                                    return 0;
                                 } else {
                                    int r = resetOwnSkills(target);
                                    if (r > 0 && ((ServerCommandSource)ctx.getSource()).getPlayer() != target) {
                                       ((ServerCommandSource)ctx.getSource())
                                          .sendFeedback(() -> Text.literal("Reset " + name + "'s skill tree (every skill point refunded)."), true);
                                    }

                                    return r;
                                 }
                              }
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("soulboundbattleview")
                     .then(
                        CommandManager.argument("view", StringArgumentType.word())
                           .suggests((c, b) -> {
                              b.suggest("3d");
                              b.suggest("traditional");
                              return b.buildFuture();
                           })
                           .executes(
                              ctx -> {
                                 ServerPlayerEntity self = ((ServerCommandSource)ctx.getSource()).getPlayer();
                                 if (self == null) {
                                    return 0;
                                 } else {
                                    String arg = StringArgumentType.getString(ctx, "view").toLowerCase(Locale.ROOT);

                                    BattleView view = switch (arg) {
                                       case "3d", "overhaul" -> BattleView.OVERHAUL;
                                       case "traditional" -> BattleView.TRADITIONAL;
                                       default -> null;
                                    };
                                    if (view == null) {
                                       self.sendMessage(Text.literal("Usage: /soulboundbattleview <3d|traditional>"), false);
                                       return 0;
                                    } else {
                                       BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());
                                       data.getOrCreate(self.getUuid()).battleView = view;
                                       data.markDirty();
                                       sendBattlePrefs(self);
                                       self.sendMessage(
                                          Text.literal(
                                             "Bullet Hell and Direct Damage battles will now use the " + view.displayName + " view (from your next battle)."
                                          ),
                                          false
                                       );
                                       return 1;
                                    }
                                 }
                              }
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("soulboundbattlestyle")
                     .then(CommandManager.argument("style", StringArgumentType.word()).suggests((c, b) -> {
                        b.suggest("bullethell");
                        b.suggest("direct");
                        b.suggest("live");
                        return b.buildFuture();
                     }).executes(ctx -> {
                        ServerPlayerEntity self = ((ServerCommandSource)ctx.getSource()).getPlayer();
                        if (self == null) {
                           return 0;
                        } else {
                           String arg = StringArgumentType.getString(ctx, "style").toLowerCase(Locale.ROOT);

                           BattleStyle style = switch (arg) {
                              case "bullethell", "bullet_hell", "bullet" -> BattleStyle.BULLET_HELL;
                              case "direct", "directdamage", "direct_damage" -> BattleStyle.DIRECT_DAMAGE;
                              case "live", "livecombat", "live_combat" -> BattleStyle.LIVE_COMBAT;
                              default -> null;
                           };
                           if (style == null) {
                              self.sendMessage(Text.literal("Usage: /soulboundbattlestyle <bullethell|direct|live>"), false);
                              return 0;
                           } else if (BattleManager.isInBattle(self.getUuid())) {
                              self.sendMessage(Text.literal("Finish this battle first."), false);
                              return 0;
                           } else {
                              BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());
                              data.getOrCreate(self.getUuid()).battleStyle = style;
                              data.markDirty();
                              LiveCombat.sendState(self);
                              sendBattlePrefs(self);
                              self.sendMessage(Text.literal("Battle style: " + style.displayName + ". " + style.description), false);
                              return 1;
                           }
                        }
                     }))
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("party").then(CommandManager.argument("member", StringArgumentType.word()).executes(ctx -> {
                     String arg = StringArgumentType.getString(ctx, "member").toLowerCase(Locale.ROOT);
                     ServerPlayerEntity src = ((ServerCommandSource)ctx.getSource()).getPlayer();
                     if (src == null) {
                        return 0;
                     } else {
                        PartyMember chosen;
                        switch (arg) {
                           case "ness":
                              chosen = PartyMember.NESS;
                              break;
                           case "lightner":
                              chosen = PartyMember.LIGHTNER;
                              break;
                           case "frisk":
                              chosen = PartyMember.FRISK;
                              break;
                           case "none":
                              chosen = PartyMember.NONE;
                              break;
                           default:
                              src.sendMessage(Text.literal("Usage: /party <none|ness|lightner|frisk>"), false);
                              return 0;
                        }

                        BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());
                        BattleData.PlayerProgress prog = data.getOrCreate(src.getUuid());
                        prog.partyMember = chosen;
                        data.markDirty();
                        String msg = chosen == PartyMember.NONE ? "Party member removed." : chosen.displayName + " will join your next battle!";
                        src.sendMessage(Text.literal(msg), false);
                        return 1;
                     }
                  }))
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("sbpartyrogue")
                     .then(CommandManager.literal("accept").then(CommandManager.argument("inviter", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayerEntity src = ((ServerCommandSource)ctx.getSource()).getPlayer();
                        if (src == null) {
                           return 0;
                        } else {
                           String inviterName = StringArgumentType.getString(ctx, "inviter");
                           boolean ok = RogueliteManager.acceptInvite(src, inviterName);
                           if (!ok) {
                              src.sendMessage(Text.literal("No pending invite from that player.").formatted(Formatting.RED), false);
                           }

                           return ok ? 1 : 0;
                        }
                     })))
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("soulboundlevel")
                     .then(
                        ((LiteralArgumentBuilder)CommandManager.literal("set").requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.argument("player", EntityArgumentType.player())
                                 .then(
                                    ((RequiredArgumentBuilder)CommandManager.argument("level", IntegerArgumentType.integer(1, 99))
                                          .executes(
                                             ctx -> applyPlayerLevel(
                                                   ctx,
                                                   EntityArgumentType.getPlayer(ctx, "player"),
                                                   IntegerArgumentType.getInteger(ctx, "level"),
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1
                                                )
                                          ))
                                       .then(
                                          CommandManager.argument("statPts", IntegerArgumentType.integer(0))
                                             .then(
                                                CommandManager.argument("skillPts", IntegerArgumentType.integer(0))
                                                   .then(
                                                      CommandManager.argument("offense", IntegerArgumentType.integer(0))
                                                         .then(
                                                            CommandManager.argument("defense", IntegerArgumentType.integer(0))
                                                               .then(
                                                                  CommandManager.argument("speed", IntegerArgumentType.integer(0))
                                                                     .then(
                                                                        CommandManager.argument("iq", IntegerArgumentType.integer(0))
                                                                           .then(
                                                                              CommandManager.argument("vitality", IntegerArgumentType.integer(0))
                                                                                 .then(
                                                                                    CommandManager.argument("luck", IntegerArgumentType.integer(0))
                                                                                       .executes(
                                                                                          ctx -> applyPlayerLevel(
                                                                                                ctx,
                                                                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                                                                IntegerArgumentType.getInteger(ctx, "statPts"),
                                                                                                IntegerArgumentType.getInteger(ctx, "skillPts"),
                                                                                                IntegerArgumentType.getInteger(ctx, "offense"),
                                                                                                IntegerArgumentType.getInteger(ctx, "defense"),
                                                                                                IntegerArgumentType.getInteger(ctx, "speed"),
                                                                                                IntegerArgumentType.getInteger(ctx, "iq"),
                                                                                                IntegerArgumentType.getInteger(ctx, "vitality"),
                                                                                                IntegerArgumentType.getInteger(ctx, "luck")
                                                                                             )
                                                                                       )
                                                                                 )
                                                                           )
                                                                     )
                                                               )
                                                         )
                                                   )
                                             )
                                       )
                                 )
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("soulboundclass")
                     .then(
                        ((LiteralArgumentBuilder)CommandManager.literal("set").requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.argument("player", EntityArgumentType.player())
                                 .then(CommandManager.argument("class", StringArgumentType.word()).suggests((ctx, builder) -> {
                                    for (PlayerClass c : PlayerClass.values()) {
                                       builder.suggest(c.name().toLowerCase(Locale.ROOT));
                                    }

                                    return builder.buildFuture();
                                 }).executes(
                                    ctx -> applyPlayerClass(ctx, EntityArgumentType.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "class"))
                                 ))
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("soulboundwill")
                     .then(
                        ((LiteralArgumentBuilder)CommandManager.literal("reroll").requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.argument("user", EntityArgumentType.player())
                                 .executes(ctx -> rerollPlayerWill(ctx, EntityArgumentType.getPlayer(ctx, "user")))
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("giveweapon").requires(src -> src.hasPermissionLevel(2)))
                     .then(
                        CommandManager.argument("user", EntityArgumentType.player())
                           .then(
                              CommandManager.argument("weapon", StringArgumentType.string())
                                 .then(
                                    CommandManager.argument("stat", StringArgumentType.word())
                                       .suggests((c, builder) -> {
                                          for (String s : WEAPON_STAT_NAMES) {
                                             builder.suggest(s);
                                          }

                                          return builder.buildFuture();
                                       })
                                       .then(
                                          CommandManager.argument("bonusDamage", DoubleArgumentType.doubleArg())
                                             .then(
                                                CommandManager.argument("statIncrease", DoubleArgumentType.doubleArg())
                                                   .executes(
                                                      ctx -> giveWeapon(
                                                            ctx,
                                                            EntityArgumentType.getPlayer(ctx, "user"),
                                                            StringArgumentType.getString(ctx, "weapon"),
                                                            StringArgumentType.getString(ctx, "stat"),
                                                            DoubleArgumentType.getDouble(ctx, "bonusDamage"),
                                                            DoubleArgumentType.getDouble(ctx, "statIncrease")
                                                         )
                                                   )
                                             )
                                       )
                                 )
                           )
                     )
               );
               SuggestionProvider storyRecruits = (c, builder) -> {
                  for (Recruit r : Recruit.values()) {
                     if (r.isReady()) {
                        builder.suggest(r.name().toLowerCase(Locale.ROOT));
                     }
                  }

                  return builder.buildFuture();
               };
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundrecruit")
                              .requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.literal("give")
                                 .then(
                                    ((RequiredArgumentBuilder)CommandManager.argument("recruit", StringArgumentType.word())
                                          .suggests(storyRecruits)
                                          .executes(
                                             ctx -> giveStoryRecruit(
                                                   ctx,
                                                   StringArgumentType.getString(ctx, "recruit"),
                                                   ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow(),
                                                   -1
                                                )
                                          ))
                                       .then(
                                          ((RequiredArgumentBuilder)CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(
                                                   ctx -> giveStoryRecruit(
                                                         ctx, StringArgumentType.getString(ctx, "recruit"), EntityArgumentType.getPlayer(ctx, "player"), -1
                                                      )
                                                ))
                                             .then(
                                                CommandManager.argument("level", IntegerArgumentType.integer(1, 99))
                                                   .executes(
                                                      ctx -> giveStoryRecruit(
                                                            ctx,
                                                            StringArgumentType.getString(ctx, "recruit"),
                                                            EntityArgumentType.getPlayer(ctx, "player"),
                                                            IntegerArgumentType.getInteger(ctx, "level")
                                                         )
                                                   )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           CommandManager.literal("remove")
                              .then(
                                 ((RequiredArgumentBuilder)CommandManager.argument("recruit", StringArgumentType.word())
                                       .suggests(storyRecruits)
                                       .executes(
                                          ctx -> removeStoryRecruit(
                                                ctx, StringArgumentType.getString(ctx, "recruit"), ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow()
                                             )
                                       ))
                                    .then(
                                       CommandManager.argument("player", EntityArgumentType.player())
                                          .executes(
                                             ctx -> removeStoryRecruit(
                                                   ctx, StringArgumentType.getString(ctx, "recruit"), EntityArgumentType.getPlayer(ctx, "player")
                                                )
                                          )
                                    )
                              )
                        ))
                     .then(
                        CommandManager.literal("level")
                           .then(
                              CommandManager.argument("recruit", StringArgumentType.word())
                                 .suggests(storyRecruits)
                                 .then(
                                    ((RequiredArgumentBuilder)CommandManager.argument("level", IntegerArgumentType.integer(1, 99))
                                          .executes(
                                             ctx -> applyRecruitLevel(
                                                   ctx,
                                                   StringArgumentType.getString(ctx, "recruit"),
                                                   IntegerArgumentType.getInteger(ctx, "level"),
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1,
                                                   -1
                                                )
                                          ))
                                       .then(
                                          CommandManager.argument("statPts", IntegerArgumentType.integer(0))
                                             .then(
                                                CommandManager.argument("skillPts", IntegerArgumentType.integer(0))
                                                   .then(
                                                      CommandManager.argument("offense", IntegerArgumentType.integer(0))
                                                         .then(
                                                            CommandManager.argument("defense", IntegerArgumentType.integer(0))
                                                               .then(
                                                                  CommandManager.argument("magic", IntegerArgumentType.integer(0))
                                                                     .then(
                                                                        CommandManager.argument("iq", IntegerArgumentType.integer(0))
                                                                           .then(
                                                                              CommandManager.argument("vitality", IntegerArgumentType.integer(0))
                                                                                 .then(
                                                                                    CommandManager.argument("luck", IntegerArgumentType.integer(0))
                                                                                       .executes(
                                                                                          ctx -> applyRecruitLevel(
                                                                                                ctx,
                                                                                                StringArgumentType.getString(ctx, "recruit"),
                                                                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                                                                IntegerArgumentType.getInteger(ctx, "statPts"),
                                                                                                IntegerArgumentType.getInteger(ctx, "skillPts"),
                                                                                                IntegerArgumentType.getInteger(ctx, "offense"),
                                                                                                IntegerArgumentType.getInteger(ctx, "defense"),
                                                                                                IntegerArgumentType.getInteger(ctx, "magic"),
                                                                                                IntegerArgumentType.getInteger(ctx, "iq"),
                                                                                                IntegerArgumentType.getInteger(ctx, "vitality"),
                                                                                                IntegerArgumentType.getInteger(ctx, "luck")
                                                                                             )
                                                                                       )
                                                                                 )
                                                                           )
                                                                     )
                                                               )
                                                         )
                                                   )
                                             )
                                       )
                                 )
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("vessel")
                     .then(
                        ((LiteralArgumentBuilder)CommandManager.literal("rename").requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.argument("player", EntityArgumentType.player())
                                 .then(
                                    CommandManager.argument("name", StringArgumentType.greedyString())
                                       .executes(
                                          ctx -> {
                                             ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                             String newName = StringArgumentType.getString(ctx, "name").trim();
                                             if (newName.isEmpty()) {
                                                ((ServerCommandSource)ctx.getSource()).sendMessage(Text.literal("Name cannot be empty."));
                                                return 0;
                                             } else {
                                                BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());
                                                BattleData.PlayerProgress prog = data.getOrCreate(target.getUuid());
                                                prog.vesselName = newName;
                                                data.markDirty();
                                                PacketByteBuf vbuf = PacketByteBufs.create();
                                                vbuf.writeInt(prog.vesselChoice);
                                                vbuf.writeString(newName);
                                                Net121.sendToClient(target, VESSEL_INFO, vbuf);
                                                ((ServerCommandSource)ctx.getSource())
                                                   .sendMessage(Text.literal("Set " + target.getName().getString() + "'s vessel name to \"" + newName + "\"."));
                                                target.sendMessage(Text.literal("Your vessel has been renamed to \"" + newName + "\"."), false);
                                                return 1;
                                             }
                                          }
                                       )
                                 )
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)CommandManager.literal("soulboundtrait")
                     .then(
                        ((LiteralArgumentBuilder)CommandManager.literal("set").requires(src -> src.hasPermissionLevel(2)))
                           .then(
                              CommandManager.argument("player", EntityArgumentType.player())
                                 .then(
                                    CommandManager.argument("soul", StringArgumentType.word())
                                       .suggests((ctx, builder) -> {
                                          for (SoulType st : SoulType.values()) {
                                             builder.suggest(st.name().toLowerCase(Locale.ROOT));
                                          }

                                          return builder.buildFuture();
                                       })
                                       .executes(
                                          ctx -> {
                                             ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                             String soulArg = StringArgumentType.getString(ctx, "soul").toUpperCase(Locale.ROOT);

                                             SoulType chosen;
                                             try {
                                                chosen = SoulType.valueOf(soulArg);
                                             } catch (IllegalArgumentException var6x) {
                                                ((ServerCommandSource)ctx.getSource())
                                                   .sendMessage(
                                                      Text.literal(
                                                         "Unknown soul type. Valid: "
                                                            + Arrays.stream(SoulType.values())
                                                               .map(s -> s.name().toLowerCase(Locale.ROOT))
                                                               .collect(Collectors.joining(", "))
                                                      )
                                                   );
                                                return 0;
                                             }

                                             BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());
                                             BattleData.PlayerProgress prog = data.getOrCreate(target.getUuid());
                                             prog.soulType = chosen;
                                             prog.freeRollUsed = true;
                                             data.markDirty();
                                             sendSoulInfo(target, prog);
                                             ((ServerCommandSource)ctx.getSource())
                                                .sendMessage(Text.literal("Set " + target.getName().getString() + "'s soul trait to " + chosen.name()));
                                             target.sendMessage(Text.literal("Your soul trait has been set to " + chosen.name() + "."), false);
                                             return 1;
                                          }
                                       )
                                 )
                           )
                     )
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("party")
                           .then(CommandManager.literal("invite").then(CommandManager.argument("player", EntityArgumentType.player()).executes(ctx -> {
                              ServerPlayerEntity src = ((ServerCommandSource)ctx.getSource()).getPlayer();
                              ServerPlayerEntity tgt = EntityArgumentType.getPlayer(ctx, "player");
                              if (src == null) {
                                 return 0;
                              } else {
                                 PlayerPartyManager.sendInvite(src, tgt);
                                 return 1;
                              }
                           }))))
                        .then(CommandManager.literal("accept").then(CommandManager.argument("player", EntityArgumentType.player()).executes(ctx -> {
                           ServerPlayerEntity src = ((ServerCommandSource)ctx.getSource()).getPlayer();
                           if (src == null) {
                              return 0;
                           } else {
                              ServerPlayerEntity inv = EntityArgumentType.getPlayer(ctx, "player");
                              return PlayerPartyManager.acceptInvite(src, inv.getName().getString()) ? 1 : 0;
                           }
                        }))))
                     .then(CommandManager.literal("leave").executes(ctx -> {
                        ServerPlayerEntity src = ((ServerCommandSource)ctx.getSource()).getPlayer();
                        if (src == null) {
                           return 0;
                        } else if (!PlayerPartyManager.isInParty(src.getUuid())) {
                           src.sendMessage(Text.literal("You are not in a party."), false);
                           return 0;
                        } else {
                           PlayerPartyManager.leaveParty(((ServerCommandSource)ctx.getSource()).getServer(), src.getUuid(), true);
                           src.sendMessage(Text.literal("You left the party."), false);
                           PlayerPartyManager.pushPartyState(((ServerCommandSource)ctx.getSource()).getServer(), src.getUuid());
                           return 1;
                        }
                     }))
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("soulboundworld").requires(src -> src.hasPermissionLevel(2)))
                     .executes(ctx -> {
                        ServerCommandSource _src = (ServerCommandSource)ctx.getSource();
                        ServerPlayerEntity admin = _src.getPlayer() instanceof ServerPlayerEntity ? _src.getPlayer() : null;
                        if (admin != null) {
                           Net121.sendToClient(admin, WORLD_TYPE_OPEN, PacketByteBufs.empty());
                        }

                        return 1;
                     })
               );
            }
         );
   }

   private static String formatBiomeName(String raw) {
      String[] parts = raw.split("_");
      StringBuilder sb = new StringBuilder();

      for (String p : parts) {
         if (!p.isEmpty()) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
         }
      }

      return sb.toString().trim();
   }

   public static void broadcastEquipInfo(BattleState state, MinecraftServer server) {
      BattleData data = BattleData.get(server);
      List uuids = new ArrayList<>();
      List weapons = new ArrayList<>();

      for (UUID pid : state.participantOrder) {
         if (!pid.equals(BattleState.PARTY_MEMBER_UUID) && !pid.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            BattleData.PlayerProgress prog = data.getOrCreate(pid);
            uuids.add(pid);
            weapons.add(prog.equippedWeapon != null ? prog.equippedWeapon : ItemStack.EMPTY);
         }
      }

      List recruitNames = new ArrayList<>();
      List recruitWeapons = new ArrayList<>();

      for (UUID pidx : state.participantOrder) {
         if (pidx.equals(BattleState.PARTY_MEMBER_UUID) || pidx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            recruitNames.add(state.participantNames.getOrDefault(pidx, ""));
            ItemStack rw = recruitSlotWeapon(state, server, pidx);
            recruitWeapons.add(rw != null ? rw : ItemStack.EMPTY);
         }
      }

      for (UUID pidxx : state.participantOrder) {
         if (!pidxx.equals(BattleState.PARTY_MEMBER_UUID) && !pidxx.equals(BattleState.PARTY_MEMBER_UUID_2)) {
            ServerPlayerEntity pp = server.getPlayerManager().getPlayer(pidxx);
            if (pp != null) {
               PacketByteBuf buf = PacketByteBufs.create();
               buf.writeInt(uuids.size());

               for (int i = 0; i < uuids.size(); i++) {
                  buf.writeUuid(uuids.get(i));
                  ItemStack w = weapons.get(i);
                  boolean has = w != null && !w.isEmpty();
                  buf.writeBoolean(has);
                  if (has) {
                     buf.writeNbt((NbtCompound)w.encode(server.getRegistryManager()));
                  }
               }

               buf.writeInt(recruitNames.size());

               for (int ix = 0; ix < recruitNames.size(); ix++) {
                  buf.writeString(recruitNames.get(ix));
                  ItemStack w = recruitWeapons.get(ix);
                  boolean has = w != null && !w.isEmpty();
                  buf.writeBoolean(has);
                  if (has) {
                     buf.writeNbt((NbtCompound)w.encode(server.getRegistryManager()));
                  }
               }

               Net121.sendToClient(pp, PLAYER_EQUIP_BROADCAST, buf);
            }
         }
      }
   }

   private static ItemStack recruitSlotWeapon(BattleState state, MinecraftServer server, UUID slot) {
      RecruitData.PlayerRecruits pr = RecruitData.get(server).getOrCreate(state.playerUuid);
      boolean second = slot.equals(BattleState.PARTY_MEMBER_UUID_2);
      int slotCode = second ? pr.secondActiveRecruit : pr.activeRecruit;
      if (slotCode >= 1000) {
         int ci = slotCode - 1000;
         if (ci >= 0 && ci < pr.customRecruits.size()) {
            return pr.customRecruits.get(ci).state.equippedWeapon;
         }
      } else if (slotCode >= 0 && slotCode < Recruit.values().length) {
         return pr.get(Recruit.values()[slotCode]).equippedWeapon;
      }

      return second ? null : BattleData.get(server).getOrCreate(state.playerUuid).partyEquippedWeapon;
   }

   static void rebroadcastEquipIfInBattle(ServerPlayerEntity player) {
      BattleState st = BattleManager.activeBattles.get(player.getUuid());
      if (st != null && player.getServer() != null) {
         broadcastEquipInfo(st, player.getServer());
      }
   }

   static void setSoulGlow(MinecraftServer server, ServerPlayerEntity player, SoulType soul, boolean on) {
      Scoreboard sb = server.getScoreboard();
      String teamName = "sb_soul_" + (soul != null ? soul.name().toLowerCase(Locale.ROOT) : "none");
      if (on) {
         Team team = sb.getTeam(teamName);
         if (team == null) {
            team = sb.addTeam(teamName);
            Formatting var10000;
            if (soul == null) {
               var10000 = Formatting.WHITE;
            } else {
               switch (soul) {
                  case DETERMINATION:
                     var10000 = Formatting.RED;
                     break;
                  case PERSEVERANCE:
                     var10000 = Formatting.LIGHT_PURPLE;
                     break;
                  case INTEGRITY:
                     var10000 = Formatting.BLUE;
                     break;
                  case PATIENCE:
                     var10000 = Formatting.AQUA;
                     break;
                  case JUSTICE:
                     var10000 = Formatting.YELLOW;
                     break;
                  case BRAVERY:
                     var10000 = Formatting.GOLD;
                     break;
                  case KINDNESS:
                     var10000 = Formatting.GREEN;
                     break;
                  case ORI:
                     var10000 = Formatting.WHITE;
                     break;
                  case MONSTER:
                     var10000 = Formatting.WHITE;
                     break;
                  default:
                     throw new MatchException(null, null);
               }
            }

            Formatting color = var10000;
            team.setColor(color);
         }

         Team cur = sb.getScoreHolderTeam(player.getName().getString());
         if (cur != null && !cur.getName().equals(teamName)) {
            sb.clearTeam(player.getName().getString());
         }

         if (!team.getPlayerList().contains(player.getName().getString())) {
            sb.addScoreHolderToTeam(player.getName().getString(), team);
         }
      } else {
         Team curx = sb.getScoreHolderTeam(player.getName().getString());
         if (curx != null && curx.getName().startsWith("sb_soul_")) {
            sb.clearTeam(player.getName().getString());
         }
      }

      player.setGlowing(on);
      PacketByteBuf gb = PacketByteBufs.create();
      gb.writeUuid(player.getUuid());
      gb.writeBoolean(soul != null);
      gb.writeInt(soul != null ? soul.ordinal() : -1);
      gb.writeBoolean(on);

      for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
         Net121.sendToClient(other, SOUL_GLOW, gb);
      }
   }

   static boolean isNearHazard(ServerPlayerEntity player) {
      World world = player.getWorld();
      BlockPos origin = player.getBlockPos();
      Mutable pos = new Mutable();

      for (int dx = -5; dx <= 5; dx++) {
         for (int dy = -5; dy <= 5; dy++) {
            for (int dz = -5; dz <= 5; dz++) {
               if (dx * dx + dy * dy + dz * dz <= 25) {
                  pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                  BlockState bs = world.getBlockState(pos);
                  Block b = bs.getBlock();
                  if (b == Blocks.LAVA
                     || b == Blocks.FIRE
                     || b == Blocks.SOUL_FIRE
                     || b == Blocks.MAGMA_BLOCK
                     || b == Blocks.CACTUS
                     || b == Blocks.POWDER_SNOW
                     || b == Blocks.POINTED_DRIPSTONE) {
                     return true;
                  }

                  if ((b == Blocks.CAMPFIRE || b == Blocks.SOUL_CAMPFIRE) && (Boolean)bs.get(CampfireBlock.LIT)) {
                     return true;
                  }

                  if (b == Blocks.SWEET_BERRY_BUSH && (Integer)bs.get(SweetBerryBushBlock.AGE) >= 1) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   static void sendSoulInfo(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBoolean(prog.soulType != null);
      if (prog.soulType != null) {
         buf.writeInt(prog.soulType.ordinal());
      }

      buf.writeBoolean(prog.freeRollUsed);
      Net121.sendToClient(player, SOUL_INFO, buf);
      PacketByteBuf vbuf2 = PacketByteBufs.create();
      vbuf2.writeInt(prog.vesselChoice);
      vbuf2.writeString(prog.vesselName != null ? prog.vesselName : "");
      Net121.sendToClient(player, VESSEL_INFO, vbuf2);
   }

   private static boolean hasSkillSlotRoom(BattleData.PlayerProgress prog, String skillNameToEquip) {
      if (prog.rogueRun) {
         return true;
      } else {
         int maxSlots = SkillTreeDefs.maxSkillSlots(prog.level);
         int used = SkillTreeDefs.skillSlotCostUnified(skillNameToEquip, prog);

         for (String s : prog.unlockedSkills) {
            if (!s.equals(skillNameToEquip) && !prog.unequippedSkills.contains(s)) {
               used += SkillTreeDefs.skillSlotCostUnified(s, prog);
            }
         }

         return used <= maxSlots;
      }
   }

   private static boolean hasSkillSlotRoom(RecruitData.RecruitState rs, String skillNameToEquip, PlayerClass cls, SoulType soul) {
      int maxSlots = SkillTreeDefs.maxSkillSlots(rs.level);
      int used = SkillTreeDefs.recruitSlotCost(skillNameToEquip, cls, soul);

      for (String s : rs.unlockedSkills) {
         if (!s.equals(skillNameToEquip) && !rs.unequippedSkills.contains(s)) {
            used += SkillTreeDefs.recruitSlotCost(s, cls, soul);
         }
      }

      return used <= maxSlots;
   }

   public static void sendRecruitInfo(ServerPlayerEntity player) {
      RecruitData rd = RecruitData.get(player.getServer());
      RecruitData.PlayerRecruits pr = rd.getOrCreate(player.getUuid());
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(pr.activeRecruit);
      buf.writeInt(pr.secondActiveRecruit);
      buf.writeInt(pr.customRecruits.size());

      for (RecruitData.CustomRecruitState cr : pr.customRecruits) {
         buf.writeString(cr.name, 32);
         buf.writeInt(cr.playerClass != null ? cr.playerClass.ordinal() : 0);
         buf.writeInt(cr.soulType != null ? cr.soulType.ordinal() : -1);
         buf.writeInt(cr.state.level);
         buf.writeFloat(RecruitData.maxHp(cr.state) > 0.0F ? rd.currentHp(cr.state) : cr.state.hp);
         buf.writeFloat(RecruitData.maxHp(cr.state));
         buf.writeInt(cr.state.skillPoints);
         buf.writeInt(cr.state.unlockedSkills.size());

         for (String s : cr.state.unlockedSkills) {
            buf.writeString(s, 64);
         }

         buf.writeInt(cr.state.unequippedSkills.size());

         for (String s : cr.state.unequippedSkills) {
            buf.writeString(s, 64);
         }

         writeCharStats(buf, cr.state.stats);
         boolean crHasWeapon = cr.state.equippedWeapon != null && !cr.state.equippedWeapon.isEmpty();
         buf.writeBoolean(crHasWeapon);
         if (crHasWeapon) {
            buf.writeNbt((NbtCompound)cr.state.equippedWeapon.encode(player.getServer().getRegistryManager()));
         }

         boolean crHasAcc = cr.state.equippedAccessory != null && !cr.state.equippedAccessory.isEmpty();
         buf.writeBoolean(crHasAcc);
         if (crHasAcc) {
            buf.writeNbt((NbtCompound)cr.state.equippedAccessory.encode(player.getServer().getRegistryManager()));
         }

         for (int ai = 0; ai < cr.state.equippedArmor.length; ai++) {
            ItemStack piece = cr.state.equippedArmor[ai];
            boolean crHasArmor = piece != null && !piece.isEmpty();
            buf.writeBoolean(crHasArmor);
            if (crHasArmor) {
               buf.writeNbt((NbtCompound)piece.encode(player.getServer().getRegistryManager()));
            }
         }
      }

      Recruit[] all = Recruit.values();
      buf.writeInt(all.length);

      for (Recruit r : all) {
         RecruitData.RecruitState rs = pr.recruits[r.ordinal()];
         buf.writeBoolean(rs.unlocked);
         buf.writeInt(rs.level);
         buf.writeFloat(RecruitData.maxHp(rs) > 0.0F ? rd.currentHp(rs) : rs.hp);
         buf.writeFloat(RecruitData.maxHp(rs));
         buf.writeInt(rs.skillPoints);
         writeCharStats(buf, rs.stats);
         buf.writeInt(rs.unlockedSkills.size());

         for (String s : rs.unlockedSkills) {
            buf.writeString(s, 64);
         }

         buf.writeInt(rs.unequippedSkills.size());

         for (String s : rs.unequippedSkills) {
            buf.writeString(s, 64);
         }

         boolean hasWeapon = rs.equippedWeapon != null && !rs.equippedWeapon.isEmpty();
         buf.writeBoolean(hasWeapon);
         if (hasWeapon) {
            buf.writeNbt((NbtCompound)rs.equippedWeapon.encode(player.getServer().getRegistryManager()));
         }

         boolean hasAcc = rs.equippedAccessory != null && !rs.equippedAccessory.isEmpty();
         buf.writeBoolean(hasAcc);
         if (hasAcc) {
            buf.writeNbt((NbtCompound)rs.equippedAccessory.encode(player.getServer().getRegistryManager()));
         }

         for (int aix = 0; aix < rs.equippedArmor.length; aix++) {
            ItemStack piece = rs.equippedArmor[aix];
            boolean hasArmorPiece = piece != null && !piece.isEmpty();
            buf.writeBoolean(hasArmorPiece);
            if (hasArmorPiece) {
               buf.writeNbt((NbtCompound)piece.encode(player.getServer().getRegistryManager()));
            }
         }
      }

      Net121.sendToClient(player, RECRUIT_INFO, buf);
      BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(player.getUuid());
      sendQuestlineState(player, prog);
   }

   public static void sendQuestlineState(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeString(prog.activeQuestlineId != null ? prog.activeQuestlineId : "", 32);
      buf.writeInt(prog.questlineNodeIndex);
      buf.writeBoolean(prog.questlineNodeDone);
      buf.writeInt(prog.completedQuestlines.size());

      for (String q : prog.completedQuestlines) {
         buf.writeString(q, 32);
      }

      Net121.sendToClient(player, QUESTLINE_STATE, buf);
   }

   public static void sendBattlePrefs(ServerPlayerEntity player) {
      BattleData.PlayerProgress prog = BattleData.get(player.getServer()).getOrCreate(player.getUuid());
      PacketByteBuf b = PacketByteBufs.create();
      b.writeInt(prog.battleStyle != null ? prog.battleStyle.ordinal() : 0);
      b.writeInt(prog.battleView != null ? prog.battleView.ordinal() : BattleView.TRADITIONAL.ordinal());
      Net121.sendToClient(player, BATTLE_PREFS, b);
   }

   public static void sendPartyInfo(ServerPlayerEntity player) {
      BattleData data = BattleData.get(player.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
      int mi = prog.partyMember.ordinal();
      int partyLevel = prog.partyLevels[mi];
      CharStats partyStats = prog.partyStats[mi];
      float partyMax = BattleManager.computePartyMemberMaxHpPublic(partyLevel, partyStats);
      float partyHp = prog.partyMemberHp >= 0.0F ? Math.min(prog.partyMemberHp, partyMax) : partyMax;
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(prog.level);
      buf.writeFloat(player.getHealth());
      buf.writeFloat(player.getMaxHealth());
      buf.writeInt(prog.playerClass == null ? 0 : prog.playerClass.ordinal());
      buf.writeInt(prog.partyMember.ordinal());
      buf.writeFloat(partyHp);
      buf.writeFloat(partyMax);
      buf.writeInt(prog.playerRace == null ? 0 : prog.playerRace.ordinal());
      buf.writeBoolean(prog.soulRemoved);
      buf.writeInt(prog.absorbedSouls.size());

      for (String s : prog.absorbedSouls) {
         buf.writeString(s, 32);
      }

      writeCharStats(buf, prog.playerStats);

      for (PartyMember m : PartyMember.values()) {
         buf.writeInt(prog.partyLevels[m.ordinal()]);
         writeCharStats(buf, prog.partyStats[m.ordinal()]);
      }

      Net121.sendToClient(player, PARTY_INFO, buf);
      sendSoulInfo(player, prog);
      sendSkillInfo(player, prog);
      sendRecruitInfo(player);
   }

   private static void refundSoulMageSkillsForOldSoul(BattleData.PlayerProgress prog, SoulType oldSoul, ServerPlayerEntity player) {
      if (prog.playerClass == PlayerClass.SOUL_MAGE && oldSoul != null) {
         Set toRefund = new HashSet<>();

         for (String s : prog.unlockedSkills) {
            SoulMageSkill sms = SkillTreeDefs.soulMageSkillByName(s);
            if (sms != null && sms.getSoul() == oldSoul) {
               toRefund.add(s);
            }

            if (SkillTreeDefs.isStatNode(s)) {
               toRefund.add(s);
            }
         }

         for (String s : toRefund) {
            prog.skillPoints = prog.skillPoints + SkillTreeDefs.nodeCost(s, PlayerClass.SOUL_MAGE);
         }

         prog.unlockedSkills.removeAll(toRefund);
         prog.playerStats.offense = prog.playerStats.offense - prog.treeStatBonuses[0];
         prog.playerStats.defense = prog.playerStats.defense - prog.treeStatBonuses[1];
         prog.playerStats.magic = prog.playerStats.magic - prog.treeStatBonuses[2];
         prog.playerStats.iq = prog.playerStats.iq - prog.treeStatBonuses[3];
         prog.playerStats.vitality = prog.playerStats.vitality - prog.treeStatBonuses[4];
         prog.playerStats.luck = prog.playerStats.luck - prog.treeStatBonuses[5];
         prog.playerStats.offense = Math.max(0, prog.playerStats.offense);
         prog.playerStats.defense = Math.max(0, prog.playerStats.defense);
         prog.playerStats.magic = Math.max(0, prog.playerStats.magic);
         prog.playerStats.iq = Math.max(0, prog.playerStats.iq);
         prog.playerStats.vitality = Math.max(0, prog.playerStats.vitality);
         prog.playerStats.luck = Math.max(0, prog.playerStats.luck);
         prog.treeStatBonuses = new int[6];
         MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
      }
   }

   private static void sendUpdatedPartyInfoAfterStatReset(MinecraftServer server, ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      int mi2 = prog.partyMember.ordinal();
      float partyMax2 = BattleManager.computePartyMemberMaxHpPublic(prog.partyLevels[mi2], prog.partyStats[mi2]);
      float partyHp2 = prog.partyMemberHp >= 0.0F ? Math.min(prog.partyMemberHp, partyMax2) : partyMax2;
      PacketByteBuf resp2 = PacketByteBufs.create();
      resp2.writeInt(prog.level);
      resp2.writeFloat(player.getHealth());
      resp2.writeFloat(player.getMaxHealth());
      resp2.writeInt(prog.playerClass == null ? 0 : prog.playerClass.ordinal());
      resp2.writeInt(prog.partyMember.ordinal());
      resp2.writeFloat(partyHp2);
      resp2.writeFloat(partyMax2);
      resp2.writeInt(prog.playerRace == null ? 0 : prog.playerRace.ordinal());
      resp2.writeBoolean(prog.soulRemoved);
      resp2.writeInt(prog.absorbedSouls.size());

      for (String s : prog.absorbedSouls) {
         resp2.writeString(s, 32);
      }

      writeCharStats(resp2, prog.playerStats);

      for (PartyMember m : PartyMember.values()) {
         resp2.writeInt(prog.partyLevels[m.ordinal()]);
         writeCharStats(resp2, prog.partyStats[m.ordinal()]);
      }

      Net121.sendToClient(player, PARTY_INFO, resp2);
   }

   static List<String> parseTextboxPages(String raw) {
      List pages = new ArrayList<>();
      StringBuilder cur = new StringBuilder();
      String validCodes = "0123456789abcdefklmnorABCDEFKLMNOR";

      for (int i = 0; i < raw.length(); i++) {
         char c = raw.charAt(i);
         if (c == '&') {
            cur.append('\n');
         } else if (c == ';') {
            if (i + 1 < raw.length() && validCodes.indexOf(raw.charAt(i + 1)) >= 0) {
               cur.append('\u00a7').append(raw.charAt(i + 1));
               i++;
            } else {
               cur.append(';');
            }
         } else if (c == '%') {
            pages.add(cur.toString());
            cur = new StringBuilder();
         } else {
            cur.append(c);
         }
      }

      pages.add(cur.toString());
      return pages;
   }

   private static int applyPlayerLevel(
      CommandContext<ServerCommandSource> ctx,
      ServerPlayerEntity target,
      int newLevel,
      int statPts,
      int skillPts,
      int off,
      int def,
      int spd,
      int iq,
      int vit,
      int lck
   ) {
      BattleData data = BattleData.get(((ServerCommandSource)ctx.getSource()).getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(target.getUuid());
      prog.level = newLevel;
      prog.exp = 0;
      prog.playerStats = new CharStats();
      prog.playerStats.pointValue = prog.magicant ? 2 : 1;
      if (statPts >= 0) {
         prog.playerStats.unspent = statPts;
         prog.playerStats.offense = off >= 0 ? off : 0;
         prog.playerStats.defense = def >= 0 ? def : 0;
         prog.playerStats.magic = spd >= 0 ? spd : 0;
         prog.playerStats.iq = iq >= 0 ? iq : 0;
         prog.playerStats.vitality = vit >= 0 ? vit : 0;
         prog.playerStats.luck = lck >= 0 ? lck : 0;
      } else {
         prog.playerStats.unspent = (newLevel - 1) * 3;
      }

      if (skillPts >= 0) {
         prog.skillPoints = skillPts;
      } else {
         int spent = 0;

         for (String s : prog.unlockedSkills) {
            spent += SkillTreeDefs.nodeCostUnified(s, prog);
         }

         prog.skillPoints = Math.max(0, (newLevel - 1) * 3 - spent);
      }

      for (PartyMember m : PartyMember.values()) {
         int mi = m.ordinal();
         prog.partyLevels[mi] = Math.min(newLevel, 99);
         prog.partyStats[mi] = new CharStats();
         prog.partyStats[mi].unspent = (newLevel - 1) * 3;
         prog.partyUnlockedSkills[mi].clear();
         prog.partySkillPoints[mi] = (newLevel - 1) * 3;
      }

      MobLevelManager.applyPlayerStats(target, newLevel, prog.playerStats);
      data.markDirty();
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(prog.level);
      buf.writeInt(prog.exp);
      buf.writeInt(BattleData.expNeeded(prog.level));
      buf.writeBoolean(false);
      Net121.sendToClient(target, LEVEL_UPDATE, buf);
      target.sendMessage(Text.literal("Level set to " + newLevel + "!"), false);
      ((ServerCommandSource)ctx.getSource()).sendMessage(Text.literal("Set " + target.getName().getString() + "'s level to " + newLevel));
      sendSkillInfo(target, prog);
      return 1;
   }

   static int resetOwnSkills(ServerPlayerEntity player) {
      if (BattleManager.isInBattle(player.getUuid())) {
         player.sendMessage(Text.literal("You can't reset your skills during a battle.").formatted(Formatting.RED), false);
         return 0;
      } else {
         BattleData data = BattleData.get(player.getServer());
         BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
         int refunded = 0;

         for (String s : prog.unlockedSkills) {
            refunded += SkillTreeDefs.nodeCostUnified(s, prog);
         }

         int skillsRemoved = 0;

         for (String s : prog.unlockedSkills) {
            if (!SkillTreeDefs.isStatNode(s)) {
               skillsRemoved++;
            }
         }

         prog.playerStats.offense = Math.max(0, prog.playerStats.offense - prog.treeStatBonuses[0]);
         prog.playerStats.defense = Math.max(0, prog.playerStats.defense - prog.treeStatBonuses[1]);
         prog.playerStats.magic = Math.max(0, prog.playerStats.magic - prog.treeStatBonuses[2]);
         prog.playerStats.iq = Math.max(0, prog.playerStats.iq - prog.treeStatBonuses[3]);
         prog.playerStats.vitality = Math.max(0, prog.playerStats.vitality - prog.treeStatBonuses[4]);
         prog.playerStats.luck = Math.max(0, prog.playerStats.luck - prog.treeStatBonuses[5]);
         prog.treeStatBonuses = new int[6];
         prog.unlockedSkills.clear();
         prog.unequippedSkills.clear();
         prog.skillPoints = Math.max(0, prog.skillPoints + refunded);
         MobLevelManager.applyPlayerStats(player, prog.level, prog.playerStats);
         data.markDirty();
         sendPartyInfo(player);
         sendSkillInfo(player, prog);
         player.sendMessage(
            Text.literal(
                  "Your skills have been reset. "
                     + skillsRemoved
                     + " skill"
                     + (skillsRemoved == 1 ? "" : "s")
                     + " removed, "
                     + refunded
                     + " skill point"
                     + (refunded == 1 ? "" : "s")
                     + " refunded ("
                     + prog.skillPoints
                     + " available)."
               )
               .formatted(Formatting.YELLOW),
            false
         );
         return 1;
      }
   }

   private static int applyPlayerClass(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, String classArg) {
      ServerCommandSource src = (ServerCommandSource)ctx.getSource();

      PlayerClass cls;
      try {
         cls = PlayerClass.valueOf(classArg.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var7) {
         src.sendError(Text.literal("Unknown class: " + classArg + ". Valid: " + Arrays.toString((Object[])PlayerClass.values())));
         return 0;
      }

      BattleData data = BattleData.get(src.getServer());
      BattleData.PlayerProgress prog = data.getOrCreate(target.getUuid());
      prog.playerClass = cls;
      prog.unlockedSkills.clear();
      prog.skillPoints = Math.max(0, (prog.level - 1) * 3);
      data.markDirty();
      sendSkillInfo(target, prog);
      target.sendMessage(Text.literal("Your class was set to " + cls.displayName + ". Skill points reset to " + prog.skillPoints + "."), false);
      src.sendMessage(
         Text.literal(
            "Set "
               + target.getName().getString()
               + "'s class to "
               + cls.displayName
               + " (skill points reset to "
               + prog.skillPoints
               + ", unlocked skills cleared)."
         )
      );
      return 1;
   }

   private static int rerollPlayerWill(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
      ServerCommandSource src = (ServerCommandSource)ctx.getSource();
      DarkFountainData data = DarkFountainData.get(src.getServer());
      int newTint = data.rerollPlayerTint(target.getUuid());
      String hex = String.format("#%06X", newTint);
      target.sendMessage(Text.literal("Your Dark World tint was rerolled."), false);
      src.sendMessage(
         Text.literal("Rerolled " + target.getName().getString() + "'s Dark World tint to " + hex + " (applies to Dark Worlds they create from now on).")
      );
      return 1;
   }

   private static RegistryEntry<EntityAttribute> resolveWeaponStat(String name) {
      String var1 = name.toLowerCase(Locale.ROOT);

      return switch (var1) {
         case "attack_damage" -> EntityAttributes.GENERIC_ATTACK_DAMAGE;
         case "attack_speed" -> EntityAttributes.GENERIC_ATTACK_SPEED;
         case "max_health" -> EntityAttributes.GENERIC_MAX_HEALTH;
         case "armor" -> EntityAttributes.GENERIC_ARMOR;
         case "armor_toughness" -> EntityAttributes.GENERIC_ARMOR_TOUGHNESS;
         case "movement_speed" -> EntityAttributes.GENERIC_MOVEMENT_SPEED;
         case "knockback_resistance" -> EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE;
         case "luck" -> EntityAttributes.GENERIC_LUCK;
         default -> null;
      };
   }

   private static int giveWeapon(
      CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, String weaponId, String statName, double bonusDamage, double statIncrease
   ) {
      ServerCommandSource src = (ServerCommandSource)ctx.getSource();
      Identifier itemIdentifier = Identifier.of(weaponId);
      Item item = (Item)Registries.ITEM.get(itemIdentifier);
      if (item == Items.AIR) {
         src.sendError(Text.literal("Unknown item: " + weaponId));
         return 0;
      } else {
         RegistryEntry statAttribute = resolveWeaponStat(statName);
         if (statAttribute == null) {
            src.sendError(Text.literal("Unknown stat: " + statName + ". Valid options: " + String.join(", ", WEAPON_STAT_NAMES)));
            return 0;
         } else {
            ItemStack stack = new ItemStack(item);
            AttributeModifiersComponent existing = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            List entries = new ArrayList<>();
            if (existing != null) {
               entries.addAll(existing.modifiers());
            }

            entries.add(
               new Entry(
                  EntityAttributes.GENERIC_ATTACK_DAMAGE,
                  new EntityAttributeModifier(Identifier.of("turnbattle", "giveweapon_bonus_damage"), bonusDamage, Operation.ADD_VALUE),
                  AttributeModifierSlot.MAINHAND
               )
            );
            entries.add(
               new Entry(
                  statAttribute,
                  new EntityAttributeModifier(Identifier.of("turnbattle", "giveweapon_stat_increase"), statIncrease, Operation.ADD_VALUE),
                  AttributeModifierSlot.MAINHAND
               )
            );
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(entries, true));
            target.giveItemStack(stack);
            src.sendMessage(
               Text.literal(
                  String.format(
                     "Gave %s a %s with +%s bonus attack damage and +%s %s.",
                     target.getName().getString(),
                     itemIdentifier.getPath(),
                     trimTrailingZeros(bonusDamage),
                     trimTrailingZeros(statIncrease),
                     statName
                  )
               )
            );
            return 1;
         }
      }
   }

   private static String trimTrailingZeros(double value) {
      return value == Math.floor(value) && !Double.isInfinite(value) ? String.format("%,.0f", value) : String.format("%,.2f", value);
   }

   private static Recruit storyRecruitNamed(ServerCommandSource src, String name) {
      for (Recruit r : Recruit.values()) {
         if (r.name().equalsIgnoreCase(name) || r.displayName.equalsIgnoreCase(name)) {
            return r;
         }
      }

      src.sendError(
         Text.literal(
            "Unknown recruit: "
               + name
               + ". Story recruits: "
               + Arrays.stream(Recruit.values()).filter(Recruit::isReady).map(rx -> rx.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", "))
         )
      );
      return null;
   }

   private static int giveStoryRecruit(CommandContext<ServerCommandSource> ctx, String recruitName, ServerPlayerEntity target, int level) {
      ServerCommandSource src = (ServerCommandSource)ctx.getSource();
      Recruit recruit = storyRecruitNamed(src, recruitName);
      if (recruit == null) {
         return 0;
      } else if (!recruit.isReady()) {
         src.sendError(Text.literal(recruit.displayName + " isn't ready yet - they have no skills to fight with."));
         return 0;
      } else {
         String who = target.getName().getString();
         RecruitData rd = RecruitData.get(src.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(target.getUuid());
         RecruitData.RecruitState rs = pr.get(recruit);
         if (rs.unlocked) {
            src.sendError(Text.literal(who + " already has " + recruit.displayName + "."));
            return 0;
         } else {
            rs.unlocked = true;
            rs.hp = -1.0F;
            if (level > 0) {
               rs.level = level;
               rs.stats.unspent = (level - 1) * 3;
               rs.skillPoints = (level - 1) * 3;
            } else {
               rs.level = recruit.startLevel;
               rs.stats.unspent = recruit.startLevel * 3;
               rs.skillPoints = recruit.startLevel * 3;
            }

            int ord = recruit.ordinal();
            String placed;
            if (pr.activeRecruit < 0) {
               pr.activeRecruit = ord;
               placed = " and joined the party";
            } else if (pr.secondActiveRecruit < 0) {
               pr.secondActiveRecruit = ord;
               placed = " and joined the party";
            } else {
               placed = " (the party's full - swap them in from the Party menu)";
            }

            rd.markDirty();
            sendRecruitInfo(target);
            src.sendFeedback(() -> Text.literal("Gave " + who + " " + recruit.displayName + " at LV " + rs.level + placed + "."), true);
            if (target != src.getPlayer()) {
               target.sendMessage(Text.literal(recruit.displayName + " has joined you!").formatted(Formatting.YELLOW), false);
            }

            return 1;
         }
      }
   }

   private static int removeStoryRecruit(CommandContext<ServerCommandSource> ctx, String recruitName, ServerPlayerEntity target) {
      ServerCommandSource src = (ServerCommandSource)ctx.getSource();
      Recruit recruit = storyRecruitNamed(src, recruitName);
      if (recruit == null) {
         return 0;
      } else {
         String who = target.getName().getString();
         RecruitData rd = RecruitData.get(src.getServer());
         RecruitData.PlayerRecruits pr = rd.getOrCreate(target.getUuid());
         int ord = recruit.ordinal();
         RecruitData.RecruitState rs = pr.recruits[ord];
         if (!rs.unlocked) {
            src.sendError(Text.literal(who + " doesn't have " + recruit.displayName + "."));
            return 0;
         } else if (BattleManager.isInBattle(target.getUuid())) {
            src.sendError(Text.literal("Not while " + who + " is in a battle."));
            return 0;
         } else {
            List gear = new ArrayList<>();
            gear.add(rs.equippedWeapon);
            gear.add(rs.equippedAccessory);

            for (ItemStack a : rs.equippedArmor) {
               gear.add(a);
            }

            int returned = 0;

            for (ItemStack g : gear) {
               if (g != null && !g.isEmpty()) {
                  target.getInventory().offerOrDrop(g.copy());
                  returned++;
               }
            }

            pr.recruits[ord] = new RecruitData.RecruitState();
            if (pr.activeRecruit == ord) {
               pr.activeRecruit = pr.secondActiveRecruit;
               pr.secondActiveRecruit = -1;
            }

            if (pr.secondActiveRecruit == ord) {
               pr.secondActiveRecruit = -1;
            }

            rd.markDirty();
            sendRecruitInfo(target);
            int back = returned;
            src.sendFeedback(
               () -> Text.literal(
                     "Removed " + recruit.displayName + " from " + who + (back > 0 ? " (" + back + " of their items went back to " + who + ")" : "") + "."
                  ),
               true
            );
            if (target != src.getPlayer()) {
               target.sendMessage(Text.literal(recruit.displayName + " has left you.").formatted(Formatting.GRAY), false);
            }

            return 1;
         }
      }
   }

   private static int applyRecruitLevel(
      CommandContext<ServerCommandSource> ctx, String recruitName, int newLevel, int statPts, int skillPts, int off, int def, int spd, int iq, int vit, int lck
   ) {
      ServerPlayerEntity src = ((ServerCommandSource)ctx.getSource()).getPlayer();
      if (src == null) {
         ((ServerCommandSource)ctx.getSource()).sendMessage(Text.literal("Must be run by a player."));
         return 0;
      } else {
         Recruit recruit = null;

         for (Recruit r : Recruit.values()) {
            if (r.name().equalsIgnoreCase(recruitName)) {
               recruit = r;
               break;
            }
         }

         if (recruit == null) {
            ((ServerCommandSource)ctx.getSource())
               .sendMessage(
                  Text.literal(
                     "Unknown recruit: "
                        + recruitName
                        + ". Options: "
                        + Arrays.stream(Recruit.values()).map(rx -> rx.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", "))
                  )
               );
            return 0;
         } else {
            RecruitData rd = RecruitData.get(((ServerCommandSource)ctx.getSource()).getServer());
            RecruitData.PlayerRecruits pr = rd.getOrCreate(src.getUuid());
            RecruitData.RecruitState rs = pr.get(recruit);
            rs.level = newLevel;
            if (statPts >= 0) {
               rs.stats.unspent = statPts;
               rs.stats.offense = off >= 0 ? off : 0;
               rs.stats.defense = def >= 0 ? def : 0;
               rs.stats.magic = spd >= 0 ? spd : 0;
               rs.stats.iq = iq >= 0 ? iq : 0;
               rs.stats.vitality = vit >= 0 ? vit : 0;
               rs.stats.luck = lck >= 0 ? lck : 0;
            } else {
               rs.stats.offense = 0;
               rs.stats.defense = 0;
               rs.stats.magic = 0;
               rs.stats.iq = 0;
               rs.stats.vitality = 0;
               rs.stats.luck = 0;
               rs.stats.unspent = (newLevel - 1) * 3;
            }

            rs.unlockedSkills.clear();
            if (skillPts >= 0) {
               rs.skillPoints = skillPts;
            } else {
               rs.skillPoints = (newLevel - 1) * 3;
            }

            rd.markDirty();
            sendRecruitInfo(src);
            ((ServerCommandSource)ctx.getSource())
               .sendMessage(Text.literal("Set " + recruit.name().toLowerCase(Locale.ROOT) + "'s level to " + newLevel + " for " + src.getName().getString()));
            return 1;
         }
      }
   }

   public static void sendSkillInfo(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(prog.skillPoints);
      buf.writeInt(prog.unlockedSkills.size());

      for (String s : prog.unlockedSkills) {
         buf.writeString(s, 64);
      }

      buf.writeInt(prog.unequippedSkills.size());

      for (String s : prog.unequippedSkills) {
         buf.writeString(s, 64);
      }

      for (PartyMember m : PartyMember.values()) {
         int mi = m.ordinal();
         buf.writeInt(prog.partySkillPoints[mi]);
         buf.writeInt(prog.partyUnlockedSkills[mi].size());

         for (String s : prog.partyUnlockedSkills[mi]) {
            buf.writeString(s, 64);
         }
      }

      Net121.sendToClient(player, SKILL_INFO, buf);
   }

   private static void writeCharStats(PacketByteBuf buf, CharStats s) {
      buf.writeInt(s.offense);
      buf.writeInt(s.defense);
      buf.writeInt(s.magic);
      buf.writeInt(s.iq);
      buf.writeInt(s.vitality);
      buf.writeInt(s.luck);
      buf.writeInt(s.unspent);
   }

   static void sendEquipInfo(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf buf = PacketByteBufs.create();
      boolean hasEquipped = prog.equippedWeapon != null && !prog.equippedWeapon.isEmpty();
      buf.writeBoolean(hasEquipped);
      if (hasEquipped) {
         buf.writeNbt((NbtCompound)prog.equippedWeapon.encode(player.getServer().getRegistryManager()));
      }

      boolean hasPartyEquipped = prog.partyEquippedWeapon != null && !prog.partyEquippedWeapon.isEmpty();
      buf.writeBoolean(hasPartyEquipped);
      if (hasPartyEquipped) {
         buf.writeNbt((NbtCompound)prog.partyEquippedWeapon.encode(player.getServer().getRegistryManager()));
      }

      List slots = new ArrayList<>();
      List stacks = new ArrayList<>();

      for (int i = 0; i < player.getInventory().size(); i++) {
         ItemStack s = player.getInventory().getStack(i);
         if (!s.isEmpty() && hasAttackDamage(s)) {
            slots.add(new int[]{i});
            stacks.add(s);
         }
      }

      buf.writeInt(stacks.size());

      for (int ix = 0; ix < stacks.size(); ix++) {
         buf.writeInt(slots.get(ix)[0]);
         new NbtCompound();
         buf.writeNbt((NbtCompound)stacks.get(ix).encode(player.getServer().getRegistryManager()));
      }

      Net121.sendToClient(player, EQUIP_INFO, buf);
   }

   static void sendAccessoryInfo(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf buf = PacketByteBufs.create();
      boolean hasEquipped = prog.equippedAccessory != null && !prog.equippedAccessory.isEmpty();
      buf.writeBoolean(hasEquipped);
      if (hasEquipped) {
         buf.writeNbt((NbtCompound)prog.equippedAccessory.encode(player.getServer().getRegistryManager()));
      }

      List slots = new ArrayList<>();
      List stacks = new ArrayList<>();

      for (int i = 0; i < player.getInventory().size(); i++) {
         ItemStack s = player.getInventory().getStack(i);
         if (!s.isEmpty() && OffhandGimmick.isAccessoryItem(s)) {
            slots.add(i);
            stacks.add(s);
         }
      }

      buf.writeInt(stacks.size());

      for (int ix = 0; ix < stacks.size(); ix++) {
         buf.writeInt(slots.get(ix));
         new NbtCompound();
         buf.writeNbt((NbtCompound)stacks.get(ix).encode(player.getServer().getRegistryManager()));
      }

      Net121.sendToClient(player, ACCESSORY_INFO, buf);
   }

   private static boolean hasAttackDamage(ItemStack stack) {
      AttributeModifiersComponent attrComp = (AttributeModifiersComponent)stack.getItem().getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      return attrComp == null ? false : !attrComp.modifiers().isEmpty();
   }

   private static int spawnBossOnPlayer(ServerCommandSource src, int tier) {
      try {
         ServerPlayerEntity player = src.getPlayer();
         if (player == null) {
            return 0;
         } else {
            ServerWorld world = (ServerWorld)player.getWorld();
            EntityType[] hostileTypes = new EntityType[]{
               EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.WITCH, EntityType.HUSK, EntityType.STRAY
            };
            Random rng = new Random();
            EntityType type = hostileTypes[rng.nextInt(hostileTypes.length)];
            if (type.create(world) instanceof MobEntity mob) {
               mob.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
               mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.COMMAND, null);
               world.spawnEntity(mob);
               int level = BattleData.get(world.getServer()).getOrCreate(player.getUuid()).level;
               MobLevelManager.restoreLevel(mob, Math.max(1, level));
               int finalTier = tier >= 0 && tier < EliteMobManager.TIER_NAMES.length ? tier : EliteMobManager.rollRandomTier();
               EliteMobManager.makeElite(mob, finalTier);
               src.sendFeedback(
                  () -> Text.literal("Spawned [" + EliteMobManager.TIER_NAMES[finalTier] + "] elite " + mob.getType().getName().getString() + "!")
                        .formatted(EliteMobManager.TIER_COLORS[finalTier]),
                  true
               );
               return 1;
            } else {
               return 0;
            }
         }
      } catch (Exception var11) {
         LOGGER.warn("[TurnBattle] /spawnboss failed: " + var11.getMessage());
         return 0;
      }
   }

   static void sendArmorInfo(ServerPlayerEntity player, BattleData.PlayerProgress prog) {
      PacketByteBuf buf = PacketByteBufs.create();

      for (int i = 0; i < 2; i++) {
         ItemStack piece = prog.equippedArmor[i];
         boolean has = piece != null && !piece.isEmpty();
         buf.writeBoolean(has);
         if (has) {
            new NbtCompound();
            buf.writeNbt((NbtCompound)piece.encode(player.getServer().getRegistryManager()));
         }
      }

      List slots = new ArrayList<>();
      List stacks = new ArrayList<>();

      for (int ix = 0; ix < player.getInventory().size(); ix++) {
         ItemStack s = player.getInventory().getStack(ix);
         if (ArmorStats.isArmorItem(s)) {
            slots.add(ix);
            stacks.add(s);
         }
      }

      buf.writeInt(stacks.size());

      for (int ixx = 0; ixx < stacks.size(); ixx++) {
         buf.writeInt(slots.get(ixx));
         new NbtCompound();
         buf.writeNbt((NbtCompound)stacks.get(ixx).encode(player.getServer().getRegistryManager()));
      }

      Net121.sendToClient(player, ARMOR_INFO, buf);
   }

   public static void sendStatsExtra(ServerPlayerEntity player) {
      if (player != null && player.getServer() != null) {
         PacketByteBuf b = PacketByteBufs.create();
         b.writeVarInt(BattleData.get(player.getServer()).getOrCreate(player.getUuid()).darkDollars);
         b.writeFloat(TurnBattleConfig.get().distanceLevelMultiplier);
         Net121.sendToClient(player, STATS_EXTRA, b);
      }
   }
}
