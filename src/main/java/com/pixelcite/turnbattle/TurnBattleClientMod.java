package com.pixelcite.turnbattle;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.AllowChat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterEntities;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.Last;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.Start;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BeforeInit;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;

@Environment(EnvType.CLIENT)
public class TurnBattleClientMod implements ClientModInitializer {
   public static int clientDarkDollars = 0;
   static final List<Object[]> pendingTextboxes = new ArrayList<>();
   public static float clientDistanceLevelMult = 1.0F;
   public static int clientPlayerLevel = 1;
   public static String clientVesselName = "";
   public static int clientLevel = 1;
   public static long clientPlaytimeSec = 0L;
   public static int clientExp = 0;
   public static int clientExpNeeded = 30;
   public static PlayerClass clientPlayerClass = PlayerClass.PSI_USER;
   public static CharacterRace clientPlayerRace = CharacterRace.HUMAN;
   public static UUID clientPvpOpponentUuid = null;
   public static CharStats clientPlayerStats = new CharStats();
   public static ItemStack clientEquippedWeapon = ItemStack.EMPTY;
   public static ItemStack[] clientEquippedArmor = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
   private static final Map<UUID, ItemStack> remoteEquippedWeapons = new HashMap<>();
   private static final Map<String, ItemStack> remoteRecruitWeapons = new HashMap<>();
   public static ItemStack clientEquippedAccessory = ItemStack.EMPTY;
   public static SoulType clientSoul = null;
   public static boolean clientSoulRemoved = false;
   public static boolean isWallClimbing = false;
   public static boolean isClimbStepping = false;
   private static boolean lastRevealHeld = false;
   public static Set<String> clientFavoriteFood = new HashSet<>();
   public static List<String> clientAbsorbedSouls = new ArrayList<>();
   public static boolean clientMagicant = false;
   public static BattleStyle clientBattleStyle = BattleStyle.BULLET_HELL;
   public static BattleView clientBattleView = BattleView.TRADITIONAL;
   public static int establishingConnectionTicks = 0;
   private static int soulExtractHoldTicks = 0;
   private static SoundInstance removeSoulSoundInstance = null;
   private static final int SOUL_EXTRACT_TICKS = 60;
   public static SoulType pvpOpponentSoul = null;
   public static int clientSkillPoints = 0;
   public static Set<String> clientUnlockedSkills = new HashSet<>();
   public static Set<String> clientUnequippedSkills = new HashSet<>();
   public static Set<String> clientRecruitUnequippedSkills = new HashSet<>();
   public static volatile boolean pendingCodexOpen = false;
   public static int clientActiveRecruit = -1;
   public static int clientSecondRecruit = -1;
   public static String[] customRecruitNames = new String[0];
   public static int[] customRecruitClasses = new int[0];
   public static int[] customRecruitSouls = new int[0];
   public static int[] customRecruitLevels = new int[0];
   public static float[] customRecruitHp = new float[0];
   public static float[] customRecruitMaxHp = new float[0];
   public static int[] customRecruitSkillPoints = new int[0];
   public static Set<String>[] customRecruitSkills = new HashSet[0];
   public static Set<String>[] customRecruitUnequippedSkills = new HashSet[0];
   public static CharStats[] customRecruitStats = new CharStats[0];
   public static ItemStack[] customRecruitWeapons = new ItemStack[0];
   public static ItemStack[] customRecruitAccessories = new ItemStack[0];
   public static ItemStack[][] customRecruitArmor = new ItemStack[0][2];
   public static boolean[] clientRecruitUnlocked;
   public static int[] clientRecruitLevels;
   public static float[] clientRecruitHp;
   public static float[] clientRecruitMaxHp;
   public static int[] clientRecruitSkillPoints;
   public static CharStats[] clientRecruitStats;
   public static Set<String>[] clientRecruitSkills;
   public static ItemStack[] clientRecruitWeapons;
   public static ItemStack[] clientRecruitAccessories;
   public static ItemStack[][] clientRecruitArmor;
   public static ItemStack clientPartyEquippedWeapon = ItemStack.EMPTY;
   public static String clientActiveQuestlineId;
   public static int clientQuestlineNodeIndex;
   public static boolean clientQuestlineNodeDone;
   public static final Set<String> clientCompletedQuestlines;
   public static int clientPartyMember;
   public static int[] clientPartySkillPoints;
   public static Set<String>[] clientPartyUnlockedSkills;
   public static long rogueElapsedTicks;
   public static boolean rogueliteInRun;
   public static boolean isRogueliteWorld;
   public static volatile int darkFountainBattlesRemaining;
   public static volatile int darkFountainBattlesRequired;
   public static volatile boolean isRuthless;
   public static volatile int vesselChoice;
   public static volatile boolean storyModeEnabled;
   public static final List<TurnBattleClientMod.PartyMemberInfo> clientPartyPlayers;
   public static boolean publicInvites;
   public static boolean lightWorldUi;
   public static BattleUi battleUi;
   public static boolean showBattleDebug;
   private static final File CLIENT_PREFS_FILE;
   public static DepthsDescentScreen pendingDepthsScreen;
   public static final List<TurnBattleClientMod.OnlinePlayerEntry> onlinePlayers;
   public static List<String> clientRogueSkills;
   public static List<String> clientUniqueCards;
   public static int[] clientCardStatBonuses;
   private static KeyBinding partyKey;
   static KeyBinding inspectKey;
   private static KeyBinding summonSoulKey;
   private static KeyBinding removeSoulKey;
   private static KeyBinding proximityChatToggleKey;
   public static boolean proximityChatEnabled;
   public static final Map<KeyBinding, Integer> defaultBattleKeys;
   public static KeyBinding battleUp;
   public static KeyBinding battleDown;
   public static KeyBinding battleLeft;
   public static KeyBinding battleRight;
   public static KeyBinding battleConfirm;
   public static KeyBinding battleBack;
   public static KeyBinding battleScrollPartyUp;
   public static KeyBinding battleScrollPartyDown;
   private static boolean revealSnapWait;
   private static boolean soulRipShown;
   private static boolean soulRipWaitRelease;

   private static ItemStack decodeStack(MinecraftClient client, NbtCompound nbt) {
      return nbt != null && client.world != null ? ItemStack.fromNbt(client.world.getRegistryManager(), nbt).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
   }

   public static ItemStack getRemoteEquippedWeapon(UUID uuid) {
      return remoteEquippedWeapons.getOrDefault(uuid, ItemStack.EMPTY);
   }

   public static ItemStack getRemoteRecruitWeapon(String recruitName) {
      return remoteRecruitWeapons.get(recruitName);
   }

   public static void saveClientPrefs() {
      try {
         JsonObject obj = new JsonObject();
         obj.addProperty("lightWorldUi", lightWorldUi);
         obj.addProperty("battleUi", battleUi.name());
         obj.addProperty("showBattleDebug", showBattleDebug);
         obj.addProperty("allowGuiScale4", GuiScaleGuard.allowed);

         try (FileWriter fw = new FileWriter(CLIENT_PREFS_FILE)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, fw);
         }
      } catch (Exception var6) {
      }
   }

   public static void loadClientPrefs() {
      if (CLIENT_PREFS_FILE.exists()) {
         try (FileReader fr = new FileReader(CLIENT_PREFS_FILE)) {
            JsonObject obj = JsonParser.parseReader(fr).getAsJsonObject();
            if (obj.has("lightWorldUi")) {
               lightWorldUi = obj.get("lightWorldUi").getAsBoolean();
            }

            if (obj.has("battleUi")) {
               battleUi = BattleUi.byName(obj.get("battleUi").getAsString());
            }

            if (obj.has("showBattleDebug")) {
               showBattleDebug = obj.get("showBattleDebug").getAsBoolean();
            }

            if (obj.has("allowGuiScale4")) {
               GuiScaleGuard.allowed = obj.get("allowGuiScale4").getAsBoolean();
            }
         } catch (Exception var5) {
         }
      }
   }

   public static boolean isPartyKey(int keyCode, int scanCode) {
      return partyKey != null && partyKey.matchesKey(keyCode, scanCode);
   }

   private static KeyBinding registerBattleKey(String name, int defaultGlfw) {
      KeyBinding kb = KeyBindingHelper.registerKeyBinding(
         new KeyBinding("key.turnbattle." + name, InputUtil.UNKNOWN_KEY.getCode(), "category.turnbattle.battle")
      );
      defaultBattleKeys.put(kb, defaultGlfw);
      return kb;
   }

   public static Key boundKey(KeyBinding kb) {
      if (kb == null) {
         return InputUtil.UNKNOWN_KEY;
      } else {
         Key bound;
         try {
            bound = KeyBindingHelper.getBoundKeyOf(kb);
         } catch (Exception var3) {
            bound = kb.getDefaultKey();
         }

         if (bound.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
            Integer def = defaultBattleKeys.get(kb);
            if (def != null) {
               return Type.KEYSYM.createFromCode(def);
            }
         }

         return bound;
      }
   }

   private static boolean onBind(KeyBinding kb, int glfwKey) {
      Key k = boundKey(kb);
      return k.getCategory() == Type.KEYSYM && k.getCode() == glfwKey && glfwKey != InputUtil.UNKNOWN_KEY.getCode();
   }

   public static boolean isMenuConfirm(int key) {
      return onBind(battleConfirm, key) || key == 90 || key == 257 || key == 335;
   }

   public static boolean isMenuBack(int key) {
      return onBind(battleBack, key) || key == 88 || key == 259;
   }

   public static String keyName(KeyBinding kb) {
      String s = boundKey(kb).getLocalizedText().getString();
      return s.replace("Left ", "L ").replace("Right ", "R ");
   }

   public void onInitializeClient() {
      BlockEntityRendererFactories.register(TurnBattleMod.DARK_FOUNTAIN_BLOCK_ENTITY_TYPE, DarkFountainBlockEntityRenderer::new);
      ScreenEvents.BEFORE_INIT.register((BeforeInit)(client, screen, sw, sh) -> {
         if (screen instanceof DeathScreen) {
            client.setScreen(new GameOverScreen());
         }
      });
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register(
                  (LiteralArgumentBuilder)ClientCommandManager.literal("soulboundguiscale").then(ClientCommandManager.literal("allow").executes(ctx -> {
                     GuiScaleGuard.allow(MinecraftClient.getInstance());
                     return 1;
                  }))
               )
         );
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register(
                  (LiteralArgumentBuilder)ClientCommandManager.literal("soulboundskin")
                     .then(ClientCommandManager.argument("recruitname", StringArgumentType.greedyString()).executes(ctx -> {
                        String query = StringArgumentType.getString(ctx, "recruitname");
                        MinecraftClient mc = MinecraftClient.getInstance();
                        String matched = null;

                        for (Recruit r : Recruit.values()) {
                           int ri = r.ordinal();
                           if (ri < clientRecruitUnlocked.length && clientRecruitUnlocked[ri] && r.displayName.equalsIgnoreCase(query)) {
                              matched = r.displayName;
                              break;
                           }
                        }

                        if (matched == null && customRecruitNames != null) {
                           for (String cn : customRecruitNames) {
                              if (cn != null && cn.equalsIgnoreCase(query)) {
                                 matched = cn;
                                 break;
                              }
                           }
                        }

                        if (matched == null) {
                           if (mc.player != null) {
                              mc.player.sendMessage(Text.literal("No recruit named '" + query + "' found.").formatted(Formatting.RED), false);
                           }

                           return 0;
                        } else if (matched.equals(Recruit.ORI.displayName)) {
                           if (mc.player != null) {
                              mc.player.sendMessage(Text.literal("Ori's appearance can't be changed.").formatted(Formatting.RED), false);
                           }

                           return 0;
                        } else {
                           String finalMatched = matched;
                           RecruitSkinManager.pickSkinFile(finalMatched, (success, error) -> {
                              if (mc.player != null) {
                                 if (success) {
                                    mc.player.sendMessage(Text.literal("Updated skin for " + finalMatched + ".").formatted(Formatting.GREEN), false);
                                 } else if (error != null) {
                                    mc.player.sendMessage(Text.literal("Skin picker error: " + error).formatted(Formatting.RED), false);
                                 } else {
                                    mc.player.sendMessage(Text.literal("Skin selection cancelled.").formatted(Formatting.GRAY), false);
                                 }
                              }
                           });
                           return 1;
                        }
                     }))
               )
         );
      EntityRendererRegistry.register(TurnBattleMod.SAVE_POINT_ENTITY_TYPE, ctx -> new EntityRenderer<SavePointEntity>(ctx) {
            public Identifier getTexture(SavePointEntity e) {
               return Identifier.of("turnbattle", "textures/savepoint/savepoint1.png");
            }

            public void render(SavePointEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
            }
         });
      EntityRendererRegistry.register(TurnBattleMod.BATTLE_RECRUIT_ENTITY_TYPE, BattleRecruitRenderer::new);
      EntityRendererRegistry.register(Shopkeeper.TYPE, ShopkeeperRenderer::new);
      EntityRendererRegistry.register(Darkners.TYPE, DarknerRenderer::new);
      EntityRendererRegistry.register(DarknerBosses.TYPE, DarknerRenderer::new);
      Net121.registerClientReceiver(DarknerBosses.BOSS_TALK, (client, handler, buf, sender) -> {
         int bossId = buf.readVarInt();
         int n = buf.readVarInt();
         String[] pages = new String[n];

         for (int i = 0; i < n; i++) {
            pages[i] = buf.readString();
         }

         client.execute(() -> client.setScreen(new TextboxScreen(pages, true, new String[]{"Challenge", "Bye"}, choice -> {
               if (choice == 0) {
                  PacketByteBuf cb = PacketByteBufs.create();
                  cb.writeVarInt(bossId);
                  Net121.sendToServer(DarknerBosses.BOSS_CHALLENGE, cb);
               }
            })));
      });
      ShopScreen.registerClient();
      HumanEnemyRenderer.register();
      MagicantClient.register();
      EntityRendererRegistry.register(SoulWalk.SOUL_BODY, SoulBodyRenderer::new);
      SoulWalkClient.register();
      ClientTickEvents.START_CLIENT_TICK.register(SoulWalkClient::startTick);
      ClientTickEvents.START_CLIENT_TICK.register((StartTick)c -> HandSwaps.restoreAll());
      WorldRenderEvents.START.register((Start)c -> HandSwaps.restoreAll());
      ClientTickEvents.END_CLIENT_TICK.register(SoulWalkClient::tick);
      DarkFountainSites.register();
      ClimbAnimClient.register();
      DarkWorldSkinsClient.register();
      RecruitSkinManager.ensureDir();
      UseItemCallback.EVENT.register((UseItemCallback)(player, world, hand) -> {
         if (!world.isClient()) {
            return TypedActionResult.pass(player.getStackInHand(hand));
         } else {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof SavePointItem) {
               MinecraftClient.getInstance().setScreen(new SavePointNameScreen(hand));
               return TypedActionResult.success(stack, true);
            } else {
               return TypedActionResult.pass(stack);
            }
         }
      });
      loadClientPrefs();

      for (int i = 0; i < clientPartyUnlockedSkills.length; i++) {
         clientPartyUnlockedSkills[i] = new HashSet<>();
      }

      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
         rogueliteInRun = false;
         isRogueliteWorld = false;
         rogueElapsedTicks = -1L;
         clientRogueSkills = new ArrayList<>();
         clientUniqueCards = new ArrayList<>();
         clientCardStatBonuses = new int[6];
         clientPartyPlayers.clear();
         onlinePlayers.clear();
         clientSoulRemoved = false;
         isWallClimbing = false;
         isClimbStepping = false;
         WallClimbTelegraphRenderer.clear();
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         clientPartyPlayers.clear();
         onlinePlayers.clear();
         isRuthless = false;
         MobFates.clearAll();
         RecruitSkinManager.clearCache();
         BattleEntityRenderer.clearAll();
         isWallClimbing = false;
         isClimbStepping = false;
         WallClimbTelegraphRenderer.clear();
      });
      partyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.turnbattle.menu", Type.KEYSYM, 77, "category.turnbattle"));
      inspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.turnbattle.inspect", Type.KEYSYM, 71, "category.turnbattle"));
      summonSoulKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.turnbattle.summon_soul", Type.KEYSYM, 82, "category.turnbattle"));
      removeSoulKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.turnbattle.remove_soul", Type.KEYSYM, 342, "category.turnbattle"));
      proximityChatToggleKey = KeyBindingHelper.registerKeyBinding(
         new KeyBinding("key.turnbattle.toggle_proximity_chat", Type.KEYSYM, 89, "category.turnbattle")
      );
      battleUp = registerBattleKey("battle_up", 87);
      battleDown = registerBattleKey("battle_down", 83);
      battleLeft = registerBattleKey("battle_left", 65);
      battleRight = registerBattleKey("battle_right", 68);
      battleConfirm = registerBattleKey("battle_confirm", 32);
      battleBack = registerBattleKey("battle_back", 340);
      battleScrollPartyUp = registerBattleKey("scroll_party_up", 87);
      battleScrollPartyDown = registerBattleKey("scroll_party_down", 83);
      ClientTickEvents.START_CLIENT_TICK.register(LiveCamera::tick);
      ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> {
         if (isWallClimbing && client.player != null) {
            byte mask = 0;
            if (client.options.forwardKey.isPressed()) {
               mask = (byte)(mask | 1);
            }

            if (client.options.backKey.isPressed()) {
               mask = (byte)(mask | 2);
            }

            if (client.options.leftKey.isPressed()) {
               mask = (byte)(mask | 4);
            }

            if (client.options.rightKey.isPressed()) {
               mask = (byte)(mask | 8);
            }

            if (client.options.sprintKey.isPressed()) {
               mask = (byte)(mask | 16);
            }

            if (client.options.sneakKey.isPressed()) {
               mask = (byte)(mask | 32);
            }

            if (client.options.jumpKey.isPressed()) {
               mask = (byte)(mask | 64);
            }

            PacketByteBuf climbBuf = PacketByteBufs.create();
            climbBuf.writeByte(mask);
            Net121.sendToServer(TurnBattleMod.WALL_CLIMB_INPUT, climbBuf);
            client.player.input.movementForward = 0.0F;
            client.player.input.movementSideways = 0.0F;
            client.player.input.jumping = false;
            if (!isClimbStepping) {
               client.player.setVelocity(Vec3d.ZERO);
            }
         }
      });
      ClientTickEvents.END_CLIENT_TICK
         .register(
            (EndTick)client -> {
               if (client.world != null) {
                  TitleMenuScreen.classic = false;
               }

               if (client.world != null && client.currentScreen == null) {
                  Key boundKey = InputUtil.fromKeyCode(summonSoulKey.getDefaultKey().getCode(), 0);

                  try {
                     boundKey = KeyBindingHelper.getBoundKeyOf(summonSoulKey);
                  } catch (Exception var10) {
                  }

                  boolean held = boundKey.getCategory() == Type.KEYSYM
                     && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), boundKey.getCode());
                  if (!SoulWalk.localOut) {
                     if (revealSnapWait) {
                        if (held) {
                           held = false;
                        } else {
                           revealSnapWait = false;
                        }
                     }
                  } else {
                     if (held && !revealSnapWait) {
                        revealSnapWait = true;
                        PacketByteBuf sb = PacketByteBufs.create();
                        sb.writeBoolean(true);
                        Net121.sendToServer(TurnBattleMod.SOUL_REVEAL, sb);
                     } else if (!held) {
                        revealSnapWait = false;
                     }

                     held = false;
                  }

                  if (clientSoulRemoved) {
                     held = false;
                  }

                  if (DarkFountainSealEffect.isActive()) {
                     held = false;
                  }

                  if (held && !lastRevealHeld) {
                     LiveCombatClient.onRevealPressed(client);
                  }

                  SoulGlowRenderer.setManualSummon(held);
                  WorldDamageHud.setRHeld(held && !LiveCombatClient.hudShown());
                  if (held != lastRevealHeld) {
                     lastRevealHeld = held;
                     PacketByteBuf rb = PacketByteBufs.create();
                     rb.writeBoolean(held);
                     Net121.sendToServer(TurnBattleMod.SOUL_REVEAL, rb);
                  }

                  if (!LiveCombatClient.menuOpen() && !LiveCombatClient.lockedOn()) {
                     handleSoulExtractionTick(client);
                  } else {
                     cancelSoulExtraction(client);
                  }
               } else {
                  SoulGlowRenderer.setManualSummon(false);
                  WorldDamageHud.setRHeld(false);
                  if (lastRevealHeld) {
                     lastRevealHeld = false;
                     PacketByteBuf rb2 = PacketByteBufs.create();
                     rb2.writeBoolean(false);
                     Net121.sendToServer(TurnBattleMod.SOUL_REVEAL, rb2);
                  }

                  cancelSoulExtraction(client);
               }

               while (partyKey.wasPressed() && !clientSoulRemoved) {
                  if (!DepthsClient.blocksMenu()) {
                     if (client.currentScreen == null) {
                        client.setScreen(new StatsScreen(null));
                     } else if (client.currentScreen instanceof PartyScreen || client.currentScreen instanceof VoidMenuScreen) {
                        client.setScreen(null);
                     }
                  }
               }

               while (proximityChatToggleKey.wasPressed()) {
                  proximityChatEnabled = !proximityChatEnabled;
                  PacketByteBuf toggleBuf = PacketByteBufs.create();
                  toggleBuf.writeBoolean(proximityChatEnabled);
                  Net121.sendToServer(TurnBattleMod.PROXIMITY_CHAT_TOGGLE, toggleBuf);
                  if (client.player != null) {
                     client.player.sendMessage(Text.literal("Proximity Chat: " + (proximityChatEnabled ? "ON" : "OFF")), true);
                  }
               }

               while (inspectKey.wasPressed()) {
                  if (client.currentScreen == null
                     && client.player != null
                     && client.world != null
                     && !DepthsClient.onInspect(client)
                     && !LightPillarClient.tryUse(client)) {
                     SavePointEntity nearbySp = null;
                     double closest = 9.0;

                     for (Entity ent : client.world.getEntities()) {
                        if (ent instanceof SavePointEntity) {
                           SavePointEntity sp = (SavePointEntity)ent;
                           double dist = client.player.squaredDistanceTo(sp);
                           if (dist < closest) {
                              closest = dist;
                              nearbySp = sp;
                           }
                        }
                     }

                     if (nearbySp != null) {
                        boolean holdingSavePoint = client.player.getMainHandStack().getItem() instanceof SavePointItem
                           || client.player.getOffHandStack().getItem() instanceof SavePointItem;
                        if (holdingSavePoint) {
                           PacketByteBuf delBuf = PacketByteBufs.create();
                           delBuf.writeInt(nearbySp.getId());
                           Net121.sendToServer(TurnBattleMod.SAVE_POINT_DELETE, delBuf);
                        } else {
                           client.setScreen(new SavePointScreen(nearbySp));
                        }
                     } else {
                        DarknerBossEntity boss = null;
                        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof DarknerBossEntity b) {
                           boss = b;
                        }

                        if (boss == null) {
                           double best = 16.0;

                           for (Entity entx : client.world.getEntities()) {
                              if (entx instanceof DarknerBossEntity) {
                                 DarknerBossEntity b = (DarknerBossEntity)entx;
                                 if (client.player.squaredDistanceTo(b) < best) {
                                    best = client.player.squaredDistanceTo(b);
                                    boss = b;
                                 }
                              }
                           }
                        }

                        if (boss != null) {
                           PacketByteBuf tb = PacketByteBufs.create();
                           tb.writeVarInt(boss.getId());
                           Net121.sendToServer(DarknerBosses.BOSS_TALK_REQ, tb);
                        } else if (!LiveCombatClient.onInspect(client)) {
                           if (client.crosshairTarget instanceof BlockHitResult bhr) {
                              BlockState bs = client.world.getBlockState(bhr.getBlockPos());
                              String bid = Registries.BLOCK.getId(bs.getBlock()).getPath();
                              if ((bid.equals("diamond_ore") || bid.equals("deepslate_diamond_ore"))
                                 && client.world.getRegistryKey().getValue().toString().startsWith("turnbattle:dark_world_v")) {
                                 PacketByteBuf sbuf = PacketByteBufs.create();
                                 sbuf.writeString(bid, 64);
                                 Net121.sendToServer(TurnBattleMod.STORY_INSPECT, sbuf);
                              }
                           }

                           InspectRegistry.Entry entry = resolveInspectEntry(client);
                           client.setScreen(new InspectScreen(entry));
                        }
                     }
                  }
               }

               DarkFountainClient.gameTick();
               DarkFountainClient.ensureTint(client);
            }
         );
      Net121.registerClientReceiver(TurnBattleMod.CLASS_SELECT, (client, handler, buf, sender) -> client.execute(() -> {
            if (!(client.currentScreen instanceof CharacterCreationScreen)) {
               client.setScreen(new CharacterCreationScreen());
            }
         }));
      Net121.registerClientReceiver(
         TurnBattleMod.BATTLE_START,
         (client, handler, buf, sender) -> {
            int entityId = buf.readInt();
            String name = buf.readString();
            String typeKey = buf.readString();
            float eMaxHp = buf.readFloat();
            float eHp = buf.readFloat();
            float pMaxHp = buf.readFloat();
            float pHp = buf.readFloat();
            float tp = buf.readFloat();
            int level = buf.readInt();
            int exp = buf.readInt();
            int expNeeded = buf.readInt();
            int classOrd = buf.readInt();
            List participants = new ArrayList<>();
            int pCount = buf.readInt();

            for (int i = 0; i < pCount; i++) {
               String pName = buf.readString();
               UUID pUuid = buf.readUuid();
               float pHpVal = buf.readFloat();
               float pMaxHpV = buf.readFloat();
               float pTp = buf.readFloat();
               boolean isCur = buf.readBoolean();
               boolean isMe = buf.readBoolean();
               int sOrd = buf.readInt();
               SoulType pSoul = sOrd >= 0 && sOrd < SoulType.values().length ? SoulType.values()[sOrd] : null;
               int shieldTurns = buf.readInt();
               int attackBoostTurns = buf.readInt();
               boolean isFrozen = buf.readBoolean();
               boolean isBurned = buf.readBoolean();
               boolean isAsleep = buf.readBoolean();
               boolean isConfused = buf.readBoolean();
               int pVessel = buf.readInt();
               participants.add(
                  new BattleScreen.ParticipantInfo(
                     pName, pHpVal, pMaxHpV, pTp, isCur, isMe, pSoul, shieldTurns, attackBoostTurns, pVessel, pUuid, isFrozen, isBurned, isAsleep, isConfused
                  )
               );
            }

            String passive = buf.readString();
            boolean darknessConstricting = buf.readBoolean();
            List extras = new ArrayList<>();
            int extraCount = buf.readInt();

            for (int i = 0; i < extraCount; i++) {
               int eid = buf.readInt();
               String ename = buf.readString();
               String etypeKey = buf.readString();
               float ehp = buf.readFloat();
               float emaxhp = buf.readFloat();
               int eFreeze = buf.readInt();
               int eBurn = buf.readInt();
               int eSleep = buf.readInt();
               int eConf = buf.readInt();
               int eDown = buf.readInt();
               int eSturdy = buf.readInt();
               float eSpare = buf.readFloat();
               extras.add(new BattleScreen.EnemyInfo(eid, ename, etypeKey, ehp, emaxhp, eFreeze, eBurn, eSleep, eConf, eDown, eSturdy, eSpare));
            }

            List enemyRoster = new ArrayList<>();
            int rosterCount = buf.readInt();

            for (int i = 0; i < rosterCount; i++) {
               int reid = buf.readInt();
               String rname = buf.readString();
               String rtype = buf.readString();
               float rhp = buf.readFloat();
               float rmaxhp = buf.readFloat();
               enemyRoster.add(new BattleScreen.EnemyInfo(reid, rname, rtype, rhp, rmaxhp));
            }

            int currentPrimaryId = buf.readInt();
            String logHistory = buf.readString();
            int elementOrd = buf.readInt();
            Element battleElement = elementOrd >= 0 && elementOrd < Element.values().length ? Element.values()[elementOrd] : Element.NONE;
            boolean hasSoul = buf.readBoolean();
            SoulType soul = null;
            if (hasSoul) {
               int soulOrd = buf.readInt();
               if (soulOrd >= 0 && soulOrd < SoulType.values().length) {
                  soul = SoulType.values()[soulOrd];
               }
            }

            int bossLivesRemaining = buf.readInt();
            int bossMaxLives = buf.readInt();
            buf.readString();
            int bsOrd2 = buf.readInt();
            BattleStyle[] bsArr = BattleStyle.values();
            BattleStyle fBattleStyle = bsOrd2 >= 0 && bsOrd2 < bsArr.length ? bsArr[bsOrd2] : BattleStyle.BULLET_HELL;
            SoulType fSoul = soul;
            boolean fOverhaul = buf.readableBytes() > 0 && buf.readBoolean();
            client.execute(() -> {
               clientPlayerLevel = level;
               PlayerClass[] classes = PlayerClass.values();
               clientPlayerClass = classOrd >= 0 && classOrd < classes.length ? classes[classOrd] : PlayerClass.PSI_USER;
               BattleScreen screen;
               if (client.currentScreen instanceof BattleScreen bs) {
                  screen = bs;
               } else {
                  screen = new BattleScreen();
                  client.setScreen(screen);
               }

               screen.onBattleStart(entityId, name, typeKey, eMaxHp, eHp, pMaxHp, pHp, tp, level, exp, expNeeded, classOrd);
               screen.updateParticipants(participants);
               screen.setEntityPassive(passive);
               screen.setDarknessConstricting(darknessConstricting);
               screen.setExtraEnemies(extras);
               screen.setEnemyRoster(enemyRoster);
               screen.setCurrentPrimaryId(currentPrimaryId);
               screen.setLogHistory(logHistory);
               screen.setBattleElement(battleElement);
               screen.setPlayerSoul(fSoul);
               screen.setBossLives(bossLivesRemaining, bossMaxLives);
               clientBattleStyle = fBattleStyle;
               screen.setBattleStyle(fBattleStyle);
               screen.setOverhaul(fOverhaul);
            });
         }
      );
      Net121.registerClientReceiver(ServerSettings.SYNC, (client, handler, buf, sender) -> {
         SettingsScreen.Sync sync = SettingsScreen.readSync(buf);
         client.execute(() -> SettingsScreen.onSync(sync));
      });
      Net121.registerClientReceiver(TurnBattleMod.BATTLE_PREFS, (client, handler, buf, sender) -> {
         int so = buf.readInt();
         int vo = buf.readInt();
         client.execute(() -> {
            BattleStyle[] st = BattleStyle.values();
            if (so >= 0 && so < st.length) {
               clientBattleStyle = st[so];
            }

            clientBattleView = BattleView.fromOrdinal(vo);
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.WORLD_BATTLE_EVENT, (client, handler, buf, sender) -> {
         int kind = buf.readByte();
         int actorId = buf.readInt();
         int targetId = buf.readInt();
         int skillOrd = buf.readInt();
         int classOrd = buf.readInt();
         int color = buf.readInt();
         client.execute(() -> OverhaulView.onWorldBattleEvent(kind, actorId, targetId, skillOrd, classOrd, color));
      });
      Net121.registerClientReceiver(
         TurnBattleMod.PVP_BATTLE_START,
         (client, handler, buf, sender) -> {
            int entityId = buf.readInt();
            UUID pvpOpponentUuid = buf.readUuid();
            clientPvpOpponentUuid = pvpOpponentUuid;
            String name = buf.readString();
            String typeKey = buf.readString();
            float eMaxHp = buf.readFloat();
            float eHp = buf.readFloat();
            float pMaxHp = buf.readFloat();
            float pHp = buf.readFloat();
            float tp = buf.readFloat();
            int level = buf.readInt();
            int exp = buf.readInt();
            int expNeeded = buf.readInt();
            int classOrd = buf.readInt();
            List participants = new ArrayList<>();
            int pCount = buf.readInt();

            for (int i = 0; i < pCount; i++) {
               String pName = buf.readString();
               UUID pUuid = buf.readUuid();
               float pHpVal = buf.readFloat();
               float pMaxHpV = buf.readFloat();
               float pTp = buf.readFloat();
               boolean isCur = buf.readBoolean();
               boolean isMe = buf.readBoolean();
               int sOrd = buf.readInt();
               SoulType pSoul = sOrd >= 0 && sOrd < SoulType.values().length ? SoulType.values()[sOrd] : null;
               int shieldTurns = buf.readInt();
               int attackBoostTurns = buf.readInt();
               boolean isFrozen = buf.readBoolean();
               boolean isBurned = buf.readBoolean();
               boolean isAsleep = buf.readBoolean();
               boolean isConfused = buf.readBoolean();
               int pVessel = buf.readInt();
               participants.add(
                  new BattleScreen.ParticipantInfo(
                     pName, pHpVal, pMaxHpV, pTp, isCur, isMe, pSoul, shieldTurns, attackBoostTurns, pVessel, pUuid, isFrozen, isBurned, isAsleep, isConfused
                  )
               );
            }

            List pvpOpponents = new ArrayList<>();
            int oCount = buf.readInt();

            for (int i = 0; i < oCount; i++) {
               String oName = buf.readString();
               UUID oUuid = buf.readUuid();
               float oHp = buf.readFloat();
               float oMaxHp = buf.readFloat();
               boolean oDowned = buf.readBoolean();
               pvpOpponents.add(new BattleScreen.OpponentInfo(oName, oUuid, oHp, oMaxHp, oDowned));
            }

            String passive = buf.readString();
            boolean darknessConstricting = buf.readBoolean();
            int extraCount = buf.readInt();

            for (int i = 0; i < extraCount; i++) {
               buf.readInt();
               buf.readString();
               buf.readString();
               buf.readFloat();
               buf.readFloat();
               buf.readInt();
               buf.readInt();
               buf.readInt();
               buf.readInt();
               buf.readInt();
               buf.readInt();
               buf.readFloat();
            }

            int rosterCount = buf.readInt();

            for (int i = 0; i < rosterCount; i++) {
               buf.readInt();
               buf.readString();
               buf.readString();
               buf.readFloat();
               buf.readFloat();
            }

            buf.readInt();
            String logHistory = buf.readString();
            int elementOrd = buf.readInt();
            boolean hasSoul = buf.readBoolean();
            SoulType soul = null;
            if (hasSoul) {
               int sOrd = buf.readInt();
               if (sOrd >= 0 && sOrd < SoulType.values().length) {
                  soul = SoulType.values()[sOrd];
               }
            }

            buf.readInt();
            buf.readInt();
            boolean hasOpSoul = buf.readBoolean();
            SoulType opSoul = null;
            if (hasOpSoul) {
               int sOrd = buf.readInt();
               if (sOrd >= 0 && sOrd < SoulType.values().length) {
                  opSoul = SoulType.values()[sOrd];
               }
            }

            float oppShield0 = buf.readFloat();
            float oppShieldMax0 = buf.readFloat();
            boolean oppShieldBroken0 = buf.readBoolean();
            float myShield0 = buf.readFloat();
            float myShieldMax0 = buf.readFloat();
            boolean myShieldBroken0 = buf.readBoolean();
            SoulType fSoul = soul;
            SoulType fOpSoul = opSoul;
            client.execute(() -> {
               clientPlayerLevel = level;
               PlayerClass[] classes = PlayerClass.values();
               clientPlayerClass = classOrd >= 0 && classOrd < classes.length ? classes[classOrd] : PlayerClass.PSI_USER;
               pvpOpponentSoul = fOpSoul;
               BattleScreen screen;
               if (client.currentScreen instanceof BattleScreen bs) {
                  screen = bs;
               } else {
                  screen = new BattleScreen();
                  client.setScreen(screen);
               }

               screen.setPvpMode(fOpSoul);
               screen.onBattleStart(entityId, name, typeKey, eMaxHp, eHp, pMaxHp, pHp, tp, level, exp, expNeeded, classOrd);
               screen.updateParticipants(participants);
               screen.updateOpponents(pvpOpponents);
               screen.setEntityPassive(passive);
               screen.setDarknessConstricting(darknessConstricting);
               screen.setExtraEnemies(new ArrayList<>());
               screen.setEnemyRoster(new ArrayList<>());
               screen.setLogHistory(logHistory);
               screen.setBattleElement(Element.NONE);
               screen.setPlayerSoul(fSoul);
               screen.setBossLives(0, 0);
               screen.setOverhaul(false);
               screen.setBattleStyle(BattleStyle.BULLET_HELL);
               screen.setPvpShield(oppShield0, oppShieldMax0, oppShieldBroken0);
               screen.setMyPvpShield(myShield0, myShieldMax0, myShieldBroken0);
            });
         }
      );
      Net121.registerClientReceiver(
         TurnBattleMod.BATTLE_STATE,
         (client, handler, buf, sender) -> {
            float eHp = buf.readFloat();
            float eMaxHp = buf.readFloat();
            String eName = buf.readString();
            String eTypeKey = buf.readString();
            float pHp = buf.readFloat();
            float pMaxHp = buf.readFloat();
            float tp = buf.readFloat();
            String msg = buf.readString();
            int hitCountFromServer = buf.readInt();
            boolean isPlayer = buf.readBoolean();
            boolean isEnemyTurn = buf.readBoolean();
            float shieldR = buf.readFloat();
            int shieldT = buf.readInt();
            int sleepT = buf.readInt();
            int confusedT = buf.readInt();
            int freezeT = buf.readInt();
            int burnT = buf.readInt();
            int downT = buf.readInt();
            int sturdyT = buf.readInt();
            boolean spare = buf.readBoolean();
            List participants2 = new ArrayList<>();
            int pCount2 = buf.readInt();

            for (int i = 0; i < pCount2; i++) {
               String pName = buf.readString();
               UUID pUuid2 = buf.readUuid();
               float pHpVal = buf.readFloat();
               float pMaxHpV = buf.readFloat();
               float pTp = buf.readFloat();
               boolean isCur = buf.readBoolean();
               boolean isMe = buf.readBoolean();
               int sOrd2 = buf.readInt();
               SoulType pSoul2 = sOrd2 >= 0 && sOrd2 < SoulType.values().length ? SoulType.values()[sOrd2] : null;
               int shieldTurns2 = buf.readInt();
               int attackBoostTurns2 = buf.readInt();
               boolean isFrozen2 = buf.readBoolean();
               boolean isBurned2 = buf.readBoolean();
               boolean isAsleep2 = buf.readBoolean();
               boolean isConfused2 = buf.readBoolean();
               int pVessel2 = buf.readInt();
               participants2.add(
                  new BattleScreen.ParticipantInfo(
                     pName,
                     pHpVal,
                     pMaxHpV,
                     pTp,
                     isCur,
                     isMe,
                     pSoul2,
                     shieldTurns2,
                     attackBoostTurns2,
                     pVessel2,
                     pUuid2,
                     isFrozen2,
                     isBurned2,
                     isAsleep2,
                     isConfused2
                  )
               );
            }

            List pvpOpponents2 = new ArrayList<>();
            int oCount2 = buf.readInt();

            for (int i = 0; i < oCount2; i++) {
               String oName2 = buf.readString();
               UUID oUuid2 = buf.readUuid();
               float oHp2 = buf.readFloat();
               float oMaxHp2 = buf.readFloat();
               boolean oDowned2 = buf.readBoolean();
               pvpOpponents2.add(new BattleScreen.OpponentInfo(oName2, oUuid2, oHp2, oMaxHp2, oDowned2));
            }

            String passive2 = buf.readString();
            boolean darknessConstricting2 = buf.readBoolean();
            List extras2 = new ArrayList<>();
            int extraCount2 = buf.readInt();

            for (int i = 0; i < extraCount2; i++) {
               int eid2 = buf.readInt();
               String ename2 = buf.readString();
               String etypeKey2 = buf.readString();
               float ehp2 = buf.readFloat();
               float emaxhp2 = buf.readFloat();
               int eFreeze2 = buf.readInt();
               int eBurn2 = buf.readInt();
               int eSleep2 = buf.readInt();
               int eConf2 = buf.readInt();
               int eDown2 = buf.readInt();
               int eSturdy2 = buf.readInt();
               float eSpare2 = buf.readFloat();
               extras2.add(new BattleScreen.EnemyInfo(eid2, ename2, etypeKey2, ehp2, emaxhp2, eFreeze2, eBurn2, eSleep2, eConf2, eDown2, eSturdy2, eSpare2));
            }

            List enemyRoster2 = new ArrayList<>();
            int rosterCount2 = buf.readInt();

            for (int i = 0; i < rosterCount2; i++) {
               int reid2 = buf.readInt();
               String rname2 = buf.readString();
               String rtype2 = buf.readString();
               float rhp2 = buf.readFloat();
               float rmaxhp2 = buf.readFloat();
               enemyRoster2.add(new BattleScreen.EnemyInfo(reid2, rname2, rtype2, rhp2, rmaxhp2));
            }

            int currentPrimaryId2 = buf.readInt();
            int bossLivesRemaining2 = buf.readInt();
            int bossMaxLives2 = buf.readInt();
            boolean rollingHealth2 = buf.readBoolean();
            buf.readString();
            float spareThreshold2 = buf.readFloat();
            int bsOrd3 = buf.readInt();
            float oppShield2 = buf.readFloat();
            float oppShieldMax2 = buf.readFloat();
            boolean oppShieldBroken2 = buf.readBoolean();
            float myShield2 = buf.readFloat();
            float myShieldMax2 = buf.readFloat();
            boolean myShieldBroken2 = buf.readBoolean();
            BattleStyle[] bsArr3 = BattleStyle.values();
            boolean isDirectDamage = bsOrd3 >= 0 && bsOrd3 < bsArr3.length && bsArr3[bsOrd3] == BattleStyle.DIRECT_DAMAGE;
            boolean fIsEnemyTurn = isEnemyTurn && !isDirectDamage;
            boolean fIsDirectWait = isEnemyTurn && isDirectDamage;
            client.execute(
               () -> {
                  if (client.currentScreen instanceof BattleScreen bs) {
                     bs.setRollingHealth(rollingHealth2);
                     bs.setEnemyStatusTurns(freezeT, burnT);
                     bs.setEnemyDownSturdy(downT, sturdyT);
                     bs.setDirectDamageWaiting(fIsDirectWait);
                     bs.onBattleState(
                        eHp,
                        eMaxHp,
                        eName,
                        eTypeKey,
                        pHp,
                        pMaxHp,
                        tp,
                        msg,
                        isPlayer,
                        fIsEnemyTurn,
                        shieldR,
                        shieldT,
                        sleepT,
                        confusedT,
                        spare,
                        hitCountFromServer
                     );
                     bs.updateParticipants(participants2);
                     bs.updateOpponents(pvpOpponents2);
                     bs.setEntityPassive(passive2);
                     bs.setDarknessConstricting(darknessConstricting2);
                     bs.checkExtraDamage(extras2);
                     bs.setExtraEnemies(extras2);
                     bs.setEnemyRoster(enemyRoster2);
                     bs.setCurrentPrimaryId(currentPrimaryId2);
                     bs.setBossLives(bossLivesRemaining2, bossMaxLives2);
                     bs.setSpareThreshold(spareThreshold2);
                     bs.setPvpShield(oppShield2, oppShieldMax2, oppShieldBroken2);
                     bs.setMyPvpShield(myShield2, myShieldMax2, myShieldBroken2);
                  }
               }
            );
         }
      );
      Net121.registerClientReceiver(TurnBattleMod.ENEMY_TURN, (client, handler, buf, sender) -> {
         float dmg = buf.readFloat();
         int pattern = buf.readInt();
         float bulletSpeedMult = buf.readFloat();
         int elementOrd2 = buf.readInt();
         long bulletSeed = buf.readLong();
         float turnLength = buf.isReadable() ? buf.readFloat() : 1.0F;
         String bulletItem = buf.isReadable() ? buf.readString() : "";
         String bossLine = buf.isReadable() ? buf.readString() : "";
         client.execute(() -> {
            DarknerRenderer.setBulletItem(bulletItem);
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.pendingBossLine = bossLine;
               bs.turnLengthMult = turnLength;
               bs.onEnemyTurn(dmg, pattern, bulletSpeedMult, elementOrd2, bulletSeed);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.SOUL_POS_SYNC, (client, handler, buf, sender) -> {
         UUID peer = buf.readUuid();
         float relX = buf.readFloat();
         float relY = buf.readFloat();
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onPeerSoulPos(peer, relX, relY);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.HP_SYNC, (client, handler, buf, sender) -> {
         float hp = buf.readFloat();
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onLiveHpSync(hp);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.FAVORITE_SYNC, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         Set favs = new HashSet<>();

         for (int i = 0; i < count; i++) {
            favs.add(buf.readString(128));
         }

         client.execute(() -> clientFavoriteFood = favs);
      });
      Net121.registerClientReceiver(
         TurnBattleMod.MERCY_SFX,
         (client, handler, buf, sender) -> client.execute(() -> TurnBattleSounds.playClient(TurnBattleSounds.SND_MERCYADD, 1.0F, 1.0F))
      );
      Net121.registerClientReceiver(TurnBattleMod.SPARE_THRESHOLD_UPDATE, (client, handler, buf, sender) -> {
         float thresh = buf.readFloat();
         int enemyId = buf.readableBytes() >= 4 ? buf.readInt() : Integer.MIN_VALUE;
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onSpareThresholdUpdate(thresh, enemyId);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.HEAL_PLAY, (client, handler, buf, sender) -> {
         int pidx = buf.readInt();
         float amount = buf.readFloat();
         boolean sfx = buf.readBoolean();
         boolean gold = buf.readBoolean();
         client.execute(() -> {
            if (sfx) {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_POWER, 1.0F, 1.0F);
            }

            if (client.currentScreen instanceof BattleScreen bs) {
               bs.spawnHealNumber(pidx, amount, gold);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.OVERWORLD_HEAL, (client, handler, buf, sender) -> {
         float hp = buf.readFloat();
         float maxHp = buf.readFloat();
         float amount = buf.readFloat();
         String name = buf.readString();
         boolean hasUuid = buf.readBoolean();
         UUID uuid = hasUuid ? buf.readUuid() : null;
         client.execute(() -> {
            TurnBattleSounds.playClient(TurnBattleSounds.SND_POWER, 1.0F, 1.0F);
            WorldDamageHud.onHeal(hp, maxHp, amount, name, uuid);
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.TP_SYNC, (client, handler, buf, sender) -> {
         float tp = buf.readFloat();
         float tpMax = buf.isReadable() ? buf.readFloat() : 100.0F;
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onLiveTpSync(tp, tpMax);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DIRECT_ENEMY_TURN, (client, handler, buf, sender) -> {
         float dmg = buf.readFloat();
         String actorName = buf.readString();
         String attackName = buf.readString();
         boolean isTarget = buf.readBoolean();
         int visualOrd = buf.readInt();
         int colorOverride = buf.readInt();
         int targetCount = buf.readInt();
         List targetUuids = new ArrayList<>(targetCount);

         for (int t = 0; t < targetCount; t++) {
            targetUuids.add(buf.readUuid());
         }

         MobAttack.Visual[] visVals = MobAttack.Visual.values();
         MobAttack.Visual visual = visualOrd >= 0 && visualOrd < visVals.length ? visVals[visualOrd] : MobAttack.Visual.IMPACT;
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onDirectEnemyTurn(dmg, actorName, attackName, isTarget, visual, colorOverride, targetUuids);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.BATTLE_END, (client, handler, buf, sender) -> {
         String outcome = buf.readString();
         int exp = buf.readInt();
         int dd = buf.isReadable() ? buf.readInt() : 0;
         client.execute(() -> {
            BattleEntityRenderer.clearAll();
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onBattleEnd(outcome, exp, dd);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.STATS_EXTRA, (client, handler, buf, sender) -> {
         int dd = buf.readVarInt();
         float mult = buf.readFloat();
         client.execute(() -> {
            clientDarkDollars = dd;
            clientDistanceLevelMult = mult;
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.BATON_PASS_START, (client, handler, buf, sender) -> {
         int cnt = buf.readInt();
         String[] names = new String[cnt];
         String[] uuids = new String[cnt];
         int[] souls = new int[cnt];

         for (int i = 0; i < cnt; i++) {
            names[i] = buf.readString();
            uuids[i] = buf.readString();
            souls[i] = buf.readInt();
         }

         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onBatonPassStart(names, uuids, souls);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.ALL_OUT_START, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         String[] names = new String[count];
         int[] souls = new int[count];

         for (int i = 0; i < count; i++) {
            names[i] = buf.readString();
            souls[i] = buf.readInt();
         }

         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onAllOutStart(names, souls);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.ALL_OUT_UPDATE, (client, handler, buf, sender) -> {
         int cnt = buf.readInt();
         LinkedHashMap votes = new LinkedHashMap<>();

         for (int i = 0; i < cnt; i++) {
            votes.put(buf.readString(), Integer.valueOf(buf.readInt()));
         }

         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onAllOutUpdate(votes);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.ALL_OUT_EXEC, (client, handler, buf, sender) -> {
         float totalDmg = buf.readFloat();
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onAllOutExec(totalDmg);
            }
         });
      });
      Net121.registerClientReceiver(
         TurnBattleMod.PARTY_MEMBER_TURN,
         (client, handler, buf, sender) -> {
            String memberName = buf.readString();
            int classOrd = buf.readInt();
            float partyMemberTp = buf.readFloat();
            int actingSlotCode = buf.readInt();
            boolean hasRecruitSoul = buf.readBoolean();
            SoulType recruitSoul = null;
            if (hasRecruitSoul) {
               int sOrdR = buf.readInt();
               if (sOrdR >= 0 && sOrdR < SoulType.values().length) {
                  recruitSoul = SoulType.values()[sOrdR];
               }
            }

            List plist = new ArrayList<>();
            int pc = buf.readInt();

            for (int i = 0; i < pc; i++) {
               String pName = buf.readString();
               UUID pUuid3 = buf.readUuid();
               float pHpV = buf.readFloat();
               float pMaxHpV = buf.readFloat();
               float pTp = buf.readFloat();
               boolean isCur = buf.readBoolean();
               boolean isMe = buf.readBoolean();
               int sOrd3 = buf.readInt();
               SoulType pSoul3 = sOrd3 >= 0 && sOrd3 < SoulType.values().length ? SoulType.values()[sOrd3] : null;
               int shieldTurns3 = buf.readInt();
               int attackBoostTurns3 = buf.readInt();
               boolean isFrozen3 = buf.readBoolean();
               boolean isBurned3 = buf.readBoolean();
               boolean isAsleep3 = buf.readBoolean();
               boolean isConfused3 = buf.readBoolean();
               int pVessel3 = buf.readInt();
               plist.add(
                  new BattleScreen.ParticipantInfo(
                     pName,
                     pHpV,
                     pMaxHpV,
                     pTp,
                     isCur,
                     isMe,
                     pSoul3,
                     shieldTurns3,
                     attackBoostTurns3,
                     pVessel3,
                     pUuid3,
                     isFrozen3,
                     isBurned3,
                     isAsleep3,
                     isConfused3
                  )
               );
            }

            SoulType fRecruitSoul = recruitSoul;
            client.execute(() -> {
               clientActiveRecruit = actingSlotCode;
               if (client.currentScreen instanceof BattleScreen bs) {
                  bs.onPartyMemberTurn(memberName, classOrd, partyMemberTp, plist, fRecruitSoul);
               }
            });
         }
      );
      Net121.registerClientReceiver(
         TurnBattleMod.PARTICIPANT_UPDATE,
         (client, handler, buf, sender) -> {
            List plist = new ArrayList<>();
            int pc = buf.readInt();

            for (int i = 0; i < pc; i++) {
               String pName = buf.readString();
               UUID pUuid4 = buf.readUuid();
               float pHpV = buf.readFloat();
               float pMaxHpV = buf.readFloat();
               float pTp = buf.readFloat();
               boolean isCur = buf.readBoolean();
               boolean isMe = buf.readBoolean();
               int sOrd4 = buf.readInt();
               SoulType pSoul4 = sOrd4 >= 0 && sOrd4 < SoulType.values().length ? SoulType.values()[sOrd4] : null;
               int shieldTurns4 = buf.readInt();
               int attackBoostTurns4 = buf.readInt();
               boolean isFrozen4 = buf.readBoolean();
               boolean isBurned4 = buf.readBoolean();
               boolean isAsleep4 = buf.readBoolean();
               boolean isConfused4 = buf.readBoolean();
               int pVessel4 = buf.readInt();
               plist.add(
                  new BattleScreen.ParticipantInfo(
                     pName,
                     pHpV,
                     pMaxHpV,
                     pTp,
                     isCur,
                     isMe,
                     pSoul4,
                     shieldTurns4,
                     attackBoostTurns4,
                     pVessel4,
                     pUuid4,
                     isFrozen4,
                     isBurned4,
                     isAsleep4,
                     isConfused4
                  )
               );
            }

            List pvpOpponents4 = new ArrayList<>();
            int oCount4 = buf.readInt();

            for (int i = 0; i < oCount4; i++) {
               String oName4 = buf.readString();
               UUID oUuid4 = buf.readUuid();
               float oHp4 = buf.readFloat();
               float oMaxHp4 = buf.readFloat();
               boolean oDowned4 = buf.readBoolean();
               pvpOpponents4.add(new BattleScreen.OpponentInfo(oName4, oUuid4, oHp4, oMaxHp4, oDowned4));
            }

            client.execute(() -> {
               if (client.currentScreen instanceof BattleScreen bs) {
                  bs.updateParticipants(plist);
                  bs.updateOpponents(pvpOpponents4);
               }
            });
         }
      );
      Net121.registerClientReceiver(TurnBattleMod.INTRO_START, (client, handler, buf, sender) -> {
         boolean storyOn = buf.readBoolean();
         client.execute(() -> {
            storyModeEnabled = storyOn;
            if (!(client.currentScreen instanceof DialogueScreen)) {
               client.setScreen(new DialogueScreen());
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.VESSEL_NAME, (client, handler, buf, sender) -> {
         String name = buf.readString(32);
         client.execute(() -> {
            if (client.currentScreen instanceof DialogueScreen ds) {
               ds.onNameConfirmed(name);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.GASTER_TRIGGER, (client, handler, buf, sender) -> client.execute(() -> {
            if (client.world != null) {
               client.world.disconnect();
            }

            client.disconnect();
            client.setScreen(new TitleScreen());
         }));
      Net121.registerClientReceiver(TurnBattleMod.PARTY_INFO, (client, handler, buf, sender) -> {
         int level = buf.readInt();
         float hp = buf.readFloat();
         float maxHp = buf.readFloat();
         int classOrd = buf.readInt();
         int memberOrd = buf.readInt();
         float partyHp = buf.readFloat();
         float partyMaxHp = buf.readFloat();
         int raceOrd = buf.readInt();
         boolean soulRemovedVal = buf.readBoolean();
         int absorbCount = buf.readInt();
         List absorbedList = new ArrayList<>();

         for (int i = 0; i < absorbCount; i++) {
            absorbedList.add(buf.readString(32));
         }

         CharStats pStats = readCharStats(buf);
         int numMembers = PartyMember.values().length;
         CharStats[] mStats = new CharStats[numMembers];
         int[] mLevels = new int[numMembers];

         for (int i = 0; i < numMembers; i++) {
            mLevels[i] = buf.readInt();
            mStats[i] = readCharStats(buf);
         }

         client.execute(() -> {
            pStats.pointValue = clientMagicant ? 2 : 1;
            clientPlayerStats = pStats;
            clientPartyMember = memberOrd;
            clientPlayerLevel = level;
            PlayerClass[] classes = PlayerClass.values();
            clientPlayerClass = classOrd >= 0 && classOrd < classes.length ? classes[classOrd] : clientPlayerClass;
            CharacterRace[] races = CharacterRace.values();
            clientPlayerRace = raceOrd >= 0 && raceOrd < races.length ? races[raceOrd] : CharacterRace.HUMAN;
            clientSoulRemoved = soulRemovedVal;
            clientAbsorbedSouls = absorbedList;
            MonsterSkill.viewerSoulCount = clientPlayerRace == CharacterRace.MONSTER ? absorbedList.size() : 0;
            if (clientSoulRemoved) {
               SoulGlowRenderer.setManualSummon(false);
               if (lastRevealHeld) {
                  lastRevealHeld = false;
                  PacketByteBuf rb = PacketByteBufs.create();
                  rb.writeBoolean(false);
                  Net121.sendToServer(TurnBattleMod.SOUL_REVEAL, rb);
               }
            }

            if (client.currentScreen instanceof PartyScreen ps) {
               ps.onPartyInfo(level, hp, maxHp, classOrd, memberOrd, partyHp, partyMaxHp, pStats, mStats, mLevels);
            }
         });
      });
      Net121.registerClientReceiver(
         TurnBattleMod.EQUIP_INFO,
         (client, handler, buf, sender) -> {
            boolean hasEquipped = buf.readBoolean();
            ItemStack equipped = ItemStack.EMPTY;
            if (hasEquipped) {
               NbtCompound w = buf.readNbt();
               if (w != null) {
                  equipped = ItemStack.fromNbt(client.world != null ? client.world.getRegistryManager() : null, w).orElse(ItemStack.EMPTY);
               }
            }

            boolean hasPartyEquipped = buf.readBoolean();
            ItemStack partyEquipped = ItemStack.EMPTY;
            if (hasPartyEquipped) {
               NbtCompound pw = buf.readNbt();
               if (pw != null) {
                  partyEquipped = ItemStack.fromNbt(client.world != null ? client.world.getRegistryManager() : null, pw).orElse(ItemStack.EMPTY);
               }
            }

            int count = buf.readInt();
            int[] invSlots = new int[count];
            ItemStack[] invStacks = new ItemStack[count];

            for (int i = 0; i < count; i++) {
               invSlots[i] = buf.readInt();
               NbtCompound sNbt = buf.readNbt();
               invStacks[i] = sNbt != null
                  ? ItemStack.fromNbt(client.world != null ? client.world.getRegistryManager() : null, sNbt).orElse(ItemStack.EMPTY)
                  : ItemStack.EMPTY;
            }

            ItemStack fEquipped = equipped;
            ItemStack fPartyEquipped = partyEquipped;
            client.execute(() -> {
               clientEquippedWeapon = fEquipped;
               clientPartyEquippedWeapon = fPartyEquipped;
               if (client.currentScreen instanceof PartyScreen ps) {
                  ps.onEquipInfo(fEquipped, fPartyEquipped, invSlots, invStacks);
               } else if (client.currentScreen instanceof EquipScreen es) {
                  es.onEquipInfo(invSlots, invStacks);
               }
            });
         }
      );
      Net121.registerClientReceiver(
         TurnBattleMod.ACCESSORY_INFO,
         (client, handler, buf, sender) -> {
            boolean hasEquipped = buf.readBoolean();
            ItemStack equipped = ItemStack.EMPTY;
            if (hasEquipped) {
               NbtCompound t = buf.readNbt();
               if (t != null) {
                  equipped = ItemStack.fromNbt(client.world != null ? client.world.getRegistryManager() : null, t).orElse(ItemStack.EMPTY);
               }
            }

            int count = buf.readInt();
            int[] invSlots = new int[count];
            ItemStack[] invStacks = new ItemStack[count];

            for (int i = 0; i < count; i++) {
               invSlots[i] = buf.readInt();
               NbtCompound sNbt = buf.readNbt();
               invStacks[i] = sNbt != null
                  ? ItemStack.fromNbt(client.world != null ? client.world.getRegistryManager() : null, sNbt).orElse(ItemStack.EMPTY)
                  : ItemStack.EMPTY;
            }

            ItemStack fEquipped = equipped;
            client.execute(() -> {
               clientEquippedAccessory = fEquipped;
               if (client.currentScreen instanceof PartyScreen ps) {
                  ps.onAccessoryInfo(fEquipped, invSlots, invStacks);
               } else if (client.currentScreen instanceof EquipScreen es) {
                  es.onAccessoryInfo(invSlots, invStacks);
               }
            });
         }
      );
      Net121.registerClientReceiver(
         TurnBattleMod.ARMOR_INFO,
         (client, handler, buf, sender) -> {
            ItemStack[] equipped = new ItemStack[2];

            for (int i = 0; i < 2; i++) {
               boolean has = buf.readBoolean();
               if (has) {
                  NbtCompound t = buf.readNbt();
                  equipped[i] = t != null
                     ? ItemStack.fromNbt(client.world != null ? client.world.getRegistryManager() : null, t).orElse(ItemStack.EMPTY)
                     : ItemStack.EMPTY;
               } else {
                  equipped[i] = ItemStack.EMPTY;
               }
            }

            int count = buf.readInt();
            int[] invSlots = new int[count];
            ItemStack[] invStacks = new ItemStack[count];

            for (int ix = 0; ix < count; ix++) {
               invSlots[ix] = buf.readInt();
               NbtCompound sNbt = buf.readNbt();
               invStacks[ix] = sNbt != null
                  ? ItemStack.fromNbt(client.world != null ? client.world.getRegistryManager() : null, sNbt).orElse(ItemStack.EMPTY)
                  : ItemStack.EMPTY;
            }

            client.execute(() -> {
               clientEquippedArmor = equipped;
               if (client.currentScreen instanceof PartyScreen ps) {
                  ps.onArmorInfo(equipped, invSlots, invStacks);
               } else if (client.currentScreen instanceof EquipScreen es) {
                  es.onArmorInfo(invSlots, invStacks);
               }
            });
         }
      );
      Net121.registerClientReceiver(TurnBattleMod.BESTIARY_DATA, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         Set defeated = new HashSet<>();

         for (int i = 0; i < count; i++) {
            defeated.add(buf.readString());
         }

         client.execute(() -> {
            if (client.player != null) {
               client.setScreen(new BestiaryScreen(defeated));
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.LEVEL_UPDATE, (client, handler, buf, sender) -> {
         int newLevel = buf.readInt();
         int exp = buf.readInt();
         int needed = buf.readInt();
         boolean leveledUp = buf.readBoolean();
         client.execute(() -> {
            clientPlayerLevel = newLevel;
            clientLevel = newLevel;
            clientExp = exp;
            clientExpNeeded = needed;
            if (leveledUp) {
               TurnBattleSounds.playClient(TurnBattleSounds.SND_LEVELUPMIX, 1.0F, 1.0F);
            }

            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onLevelUpdate(newLevel, exp, needed, leveledUp);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.VESSEL_INFO, (client, handler, buf, sender) -> {
         int choice = buf.readInt();
         String vname = buf.isReadable() ? buf.readString() : "";
         client.execute(() -> {
            vesselChoice = choice;
            if (!vname.isEmpty()) {
               clientVesselName = vname;
            }

            if (client.currentScreen instanceof PartyScreen ps) {
               ps.onVesselInfo(choice);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.SOUL_INFO, (client, handler, buf, sender) -> {
         boolean hasSoul = buf.readBoolean();
         SoulType soul = null;
         if (hasSoul) {
            int ord = buf.readInt();
            if (ord >= 0 && ord < SoulType.values().length) {
               soul = SoulType.values()[ord];
            }
         }

         boolean freeRollUsed = buf.readBoolean();
         SoulType fSoul = soul;
         client.execute(() -> {
            clientSoul = fSoul;
            if (client.currentScreen instanceof PartyScreen ps) {
               ps.onSoulInfo(fSoul, freeRollUsed);
            }

            if (fSoul != null && client.currentScreen instanceof DialogueScreen ds) {
               ds.onSoulRevealReady(fSoul);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.SKILL_INFO, (client, handler, buf, sender) -> {
         int pts = buf.readInt();
         int count = buf.readInt();
         Set skills = new HashSet<>();

         for (int i = 0; i < count; i++) {
            skills.add(buf.readString(64));
         }

         int ucount = buf.readInt();
         Set uneq = new HashSet<>();

         for (int i = 0; i < ucount; i++) {
            uneq.add(buf.readString(64));
         }

         int numMembers = PartyMember.values().length;
         int[] partyPts = new int[numMembers];
         Set[] partySkills = new HashSet[numMembers];

         for (int mi = 0; mi < numMembers; mi++) {
            partyPts[mi] = buf.readInt();
            int pc = buf.readInt();
            partySkills[mi] = new HashSet<>();

            for (int j = 0; j < pc; j++) {
               partySkills[mi].add(buf.readString(64));
            }
         }

         client.execute(() -> {
            clientSkillPoints = pts;
            clientUnlockedSkills = skills;
            clientUnequippedSkills = uneq;
            clientPartySkillPoints = partyPts;

            for (int mi = 0; mi < numMembers; mi++) {
               clientPartyUnlockedSkills[mi] = partySkills[mi];
            }

            if (pendingCodexOpen) {
               pendingCodexOpen = false;
               client.setScreen(new SkillTreeScreen(client.currentScreen));
            } else if (client.currentScreen instanceof SkillCodexScreen scs) {
               scs.refreshData(clientSkillPoints, clientUnlockedSkills);
            }
         });
      });
      RogueliteClient.register();
      DepthsClient.init();
      LiveCombatClient.register();
      MobFates.register();
      WorldSetup.register();
      Net121.registerClientReceiver(TurnBattleMod.ROGUE_SKILLS, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         List skills = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            skills.add(buf.readString(256));
         }

         int uCount = buf.readInt();
         List uniques = new ArrayList<>();

         for (int i = 0; i < uCount; i++) {
            uniques.add(buf.readString(64));
         }

         int[] statBonuses = new int[6];

         for (int i = 0; i < 6; i++) {
            statBonuses[i] = buf.readInt();
         }

         clientRogueSkills = skills;
         clientUniqueCards = uniques;
         clientCardStatBonuses = statBonuses;
      });
      Net121.registerClientReceiver(TurnBattleMod.SKILL_PLAY, (client, handler, buf, sender) -> {
         int skillOrdinal = buf.readInt();
         int clsOrd = buf.readInt();
         int targetIndex = buf.readInt();
         boolean hasTargetUuid = buf.readBoolean();
         UUID targetUuid = hasTargetUuid ? buf.readUuid() : null;
         UUID actorUuid = buf.readableBytes() >= 16 ? buf.readUuid() : null;
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               PlayerClass var8x = PlayerClass.values()[Math.min(clsOrd, PlayerClass.values().length - 1)];
               bs.playRemoteSkillEffect(skillOrdinal, var8x, targetIndex, targetUuid);
               bs.onCloneRemoteSkill(actorUuid, skillOrdinal);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.BASH_PLAY, (client, handler, buf, sender) -> {
         int soulColorRgb = buf.readInt();
         int targetIndex = buf.readInt();
         boolean hasTargetUuid = buf.readBoolean();
         UUID targetUuid = hasTargetUuid ? buf.readUuid() : null;
         UUID actorUuid = buf.readableBytes() >= 16 ? buf.readUuid() : null;
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.playRemoteBashEffect(soulColorRgb, targetIndex, targetUuid);
               bs.onCloneRemoteAction(actorUuid, CloneAnimator.Act.BASH, soulColorRgb, null);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.BASH_QTE_START, (client, handler, buf, sender) -> {
         String actorUuidStr = buf.readString();
         int soulColorRgb = buf.readInt();
         int targetIndex = buf.readInt();
         boolean crit = buf.readBoolean();
         float critZoneCenter = buf.readFloat();
         boolean isMe = buf.readBoolean();
         boolean hasTargetUuid = buf.readBoolean();
         UUID targetUuid = hasTargetUuid ? buf.readUuid() : null;
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onBashQteStart(soulColorRgb, targetIndex, crit, critZoneCenter, isMe, targetUuid);
               if (!crit && !isMe) {
                  try {
                     bs.onCloneBashWindup(UUID.fromString(actorUuidStr), soulColorRgb);
                  } catch (IllegalArgumentException var10x) {
                  }
               }
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.SHIELD_PARRY_RESULT, (client, handler, buf, sender) -> {
         boolean success = buf.readBoolean();
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onShieldParryResult(success);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.VESSEL_ANIM, (client, handler, buf, sender) -> {
         int participantIndex = buf.readInt();
         int animType = buf.readInt();
         UUID actorUuid = buf.readableBytes() > 0 && buf.readBoolean() ? buf.readUuid() : null;
         String itemId = buf.readableBytes() > 0 ? buf.readString() : "";
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onRemoteVesselAnim(participantIndex, animType, actorUuid, itemId);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.BATTLE_LINE, (client, handler, buf, sender) -> {
         int seq = buf.readInt();
         String text = buf.readString();
         int mode = buf.readByte();
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onBattleLine(seq, text, mode);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.BATTLE_LINE_SKIP, (client, handler, buf, sender) -> {
         int seq = buf.readInt();
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onBattleLineSkip(seq);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.MULTI_BASH_START, (client, handler, buf, sender) -> {
         int n = buf.readInt();
         List bars = new ArrayList<>();

         for (int i = 0; i < n; i++) {
            UUID actor = buf.readUuid();
            UUID owner = buf.readUuid();
            int color = buf.readInt();
            boolean crit = buf.readBoolean();
            int delay = buf.readInt();
            int pIdx = buf.readInt();
            String name = buf.readString();
            bars.add(new Object[]{actor, owner, color, crit, delay, pIdx, name});
         }

         if (buf.isReadable()) {
            buf.readFloat();
         }

         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onMultiBashStart(bars);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.MULTI_BASH_BAR, (client, handler, buf, sender) -> {
         UUID actor = buf.readUuid();
         float pos = buf.readFloat();
         boolean crit = buf.readBoolean();
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onMultiBashBar(actor, pos, crit);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.CLONE_POSE, (client, handler, buf, sender) -> {
         UUID actorUuid = buf.readUuid();
         int code = buf.readByte();
         String recruitName = buf.readableBytes() > 0 ? buf.readString() : "";
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.onRemoteClonePose(actorUuid, code, recruitName);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.WORLD_TYPE_OPEN, (client, handler, buf, sender) -> client.execute(() -> {
            if (!(client.currentScreen instanceof WorldTypeSelectScreen)) {
               client.setScreen(new WorldTypeSelectScreen());
            }
         }));
      Net121.registerClientReceiver(TurnBattleMod.BOSS_BEACON_SYNC, (client, handler, buf, sender) -> {
         boolean forceClose = buf.readBoolean();
         int bx = buf.readInt();
         int by = buf.readInt();
         int bz = buf.readInt();
         boolean isHost = buf.readBoolean();
         int diff = buf.readInt();
         int count = buf.readInt();
         List players = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            String pName = buf.readString(64);
            boolean pHost = buf.readBoolean();
            players.add(new BossBeaconScreen.PlayerInfo(pName, pHost));
         }

         BlockPos pos = new BlockPos(bx, by, bz);
         client.execute(() -> {
            if (forceClose) {
               if (client.currentScreen instanceof BossBeaconScreen) {
                  client.setScreen(null);
               }
            } else {
               if (!(client.currentScreen instanceof BossBeaconScreen)) {
                  client.setScreen(new BossBeaconScreen());
               }

               ((BossBeaconScreen)client.currentScreen).onSync(pos, isHost, diff, players);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DARK_FOUNTAIN_PROMPT, (client, handler, buf, sender) -> {
         int avgLevel = buf.readInt();
         float diffMult = buf.readFloat();
         String oreType = buf.readString(32);
         int sealCount = buf.readInt();
         client.execute(() -> client.setScreen(new DarkFountainScreen(avgLevel, diffMult, oreType, sealCount)));
      });
      Net121.registerClientReceiver(TurnBattleMod.DARK_FOUNTAIN_SEALS, (client, handler, buf, sender) -> {
         int seals = buf.readInt();
         client.execute(() -> {
            if (client.currentScreen instanceof DarkFountainScreen dfs) {
               dfs.updateSealCount(seals);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DARK_FOUNTAIN_ENCOUNTER, (client, handler, buf, sender) -> {
         int required = buf.readInt();
         int completed = buf.readInt();
         darkFountainBattlesRequired = required;
         darkFountainBattlesRemaining = required - completed;
      });
      Net121.registerClientReceiver(
         TurnBattleMod.DARK_FOUNTAIN_FADE,
         (client, handler, buf, sender) -> {
            boolean entering = buf.readBoolean();
            int tintRgb = buf.readInt();
            int slotId = buf.readInt();
            boolean refreshOnly = buf.readBoolean();
            client.execute(
               () -> {
                  if (!refreshOnly
                     && (
                        client.currentScreen instanceof DarkFountainScreen
                           || client.currentScreen instanceof DarkWorldCreationScreen
                           || client.currentScreen instanceof DarkFountainSealScreen
                     )) {
                     client.setScreen(null);
                  }

                  DarkFountainClient.startFade(entering, tintRgb, slotId, refreshOnly, client);
               }
            );
         }
      );
      Net121.registerClientReceiver(TurnBattleMod.DARK_FOUNTAIN_SEAL_START, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         List souls = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUuid();
            int soulOrd = buf.readInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            SoulType soul = soulOrd >= 0 && soulOrd < SoulType.values().length ? SoulType.values()[soulOrd] : null;
            souls.add(new DarkFountainSealEffect.DepartingSoul(uuid, soul, x, y, z));
         }

         double ffx = buf.readableBytes() >= 16 ? buf.readDouble() : Double.NaN;
         double ffz = buf.readableBytes() >= 8 ? buf.readDouble() : Double.NaN;
         client.execute(() -> {
            if (client.currentScreen instanceof DarkFountainSealScreen) {
               client.setScreen(null);
            }

            SoulGlowRenderer.setManualSummon(false);
            WorldDamageHud.setRHeld(false);
            if (lastRevealHeld) {
               lastRevealHeld = false;
               PacketByteBuf rb = PacketByteBufs.create();
               rb.writeBoolean(false);
               Net121.sendToServer(TurnBattleMod.SOUL_REVEAL, rb);
            }

            DarkFountainSealEffect.start(souls, ffx, ffz);
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DARK_FOUNTAIN_CREATE_SPARKLE, (client, handler, buf, sender) -> {
         double leftX = buf.readDouble();
         double leftZ = buf.readDouble();
         double rightX = buf.readDouble();
         double rightZ = buf.readDouble();
         double baseY = buf.readDouble();
         client.execute(() -> DarkFountainCreateSparkleEffect.start(leftX, leftZ, rightX, rightZ, baseY));
      });
      Net121.registerClientReceiver(TurnBattleMod.PLAYER_ANIM, (client, handler, buf, sender) -> {
         int kind = buf.readByte();
         UUID who = buf.readUuid();
         float yaw = buf.readFloat();
         double bx = 0.0;
         double by = 0.0;
         double bz = 0.0;
         int soulOrd = -1;
         if (kind == 0 || kind == 8) {
            bx = buf.readDouble();
            by = buf.readDouble();
            bz = buf.readDouble();
         }

         if (kind == 3) {
            soulOrd = buf.readInt();
         }

         double fbx = bx;
         double fby = by;
         double fbz = bz;
         int fSoul = soulOrd;
         client.execute(() -> PlayerActions.onPacket(kind, who, yaw, fbx, fby, fbz, fSoul));
      });
      Net121.registerClientReceiver(TurnBattleMod.DARK_WORLD_CREATE_MENU, (client, handler, buf, sender) -> {
         DarkWorldCreationScreen.Catalogue cat;
         try {
            cat = buf.readableBytes() > 0 ? DarkWorldCreationScreen.Catalogue.read(buf) : null;
         } catch (Exception var6) {
            cat = null;
         }

         DarkWorldCreationScreen.Catalogue fCat = cat;
         client.execute(() -> client.setScreen(fCat != null ? new DarkWorldCreationScreen(fCat) : new DarkWorldCreationScreen()));
      });
      Net121.registerClientReceiver(
         TurnBattleMod.DARK_FOUNTAIN_SEAL_PROMPT, (client, handler, buf, sender) -> client.execute(() -> client.setScreen(new DarkFountainSealScreen()))
      );
      Net121.registerClientReceiver(
         TurnBattleMod.RECRUIT_INFO,
         (client, handler, buf, sender) -> {
            int active = buf.readInt();
            int secondActive = buf.readInt();
            int customCount = buf.readInt();
            String[] cNames = new String[customCount];
            int[] cClasses = new int[customCount];
            int[] cSouls = new int[customCount];
            int[] cLevels = new int[customCount];
            float[] cHp = new float[customCount];
            float[] cMaxHp = new float[customCount];
            int[] cSp = new int[customCount];
            Set[] cSkills = new HashSet[customCount];
            Set[] cUneq = new HashSet[customCount];
            CharStats[] cStats = new CharStats[customCount];
            ItemStack[] cWeapons = new ItemStack[customCount];
            ItemStack[] cAccessories = new ItemStack[customCount];
            ItemStack[][] cArmor = new ItemStack[customCount][2];

            for (int ci = 0; ci < customCount; ci++) {
               cNames[ci] = buf.readString(32);
               cClasses[ci] = buf.readInt();
               cSouls[ci] = buf.readInt();
               cLevels[ci] = buf.readInt();
               cHp[ci] = buf.readFloat();
               cMaxHp[ci] = buf.readFloat();
               cSp[ci] = buf.readInt();
               int sc = buf.readInt();
               cSkills[ci] = new HashSet<>();

               for (int j = 0; j < sc; j++) {
                  cSkills[ci].add(buf.readString(64));
               }

               int uc = buf.readInt();
               cUneq[ci] = new HashSet<>();

               for (int j = 0; j < uc; j++) {
                  cUneq[ci].add(buf.readString(64));
               }

               cStats[ci] = new CharStats();
               cStats[ci].offense = buf.readInt();
               cStats[ci].defense = buf.readInt();
               cStats[ci].magic = buf.readInt();
               cStats[ci].iq = buf.readInt();
               cStats[ci].vitality = buf.readInt();
               cStats[ci].luck = buf.readInt();
               cStats[ci].unspent = buf.readInt();
               boolean crHasWeapon = buf.readBoolean();
               if (crHasWeapon) {
                  NbtCompound wNbt = buf.readNbt();
                  cWeapons[ci] = wNbt != null
                     ? ItemStack.fromNbt((WrapperLookup)(client.world != null ? client.world.getRegistryManager() : DynamicRegistryManager.EMPTY), wNbt)
                        .orElse(ItemStack.EMPTY)
                     : ItemStack.EMPTY;
               } else {
                  cWeapons[ci] = ItemStack.EMPTY;
               }

               boolean crHasAcc = buf.readBoolean();
               if (crHasAcc) {
                  NbtCompound aNbt = buf.readNbt();
                  cAccessories[ci] = aNbt != null
                     ? ItemStack.fromNbt((WrapperLookup)(client.world != null ? client.world.getRegistryManager() : DynamicRegistryManager.EMPTY), aNbt)
                        .orElse(ItemStack.EMPTY)
                     : ItemStack.EMPTY;
               } else {
                  cAccessories[ci] = ItemStack.EMPTY;
               }

               for (int ai = 0; ai < 2; ai++) {
                  boolean crHasArmor = buf.readBoolean();
                  if (crHasArmor) {
                     NbtCompound armNbt = buf.readNbt();
                     cArmor[ci][ai] = armNbt != null
                        ? ItemStack.fromNbt((WrapperLookup)(client.world != null ? client.world.getRegistryManager() : DynamicRegistryManager.EMPTY), armNbt)
                           .orElse(ItemStack.EMPTY)
                        : ItemStack.EMPTY;
                  } else {
                     cArmor[ci][ai] = ItemStack.EMPTY;
                  }
               }
            }

            int count = buf.readInt();
            boolean[] unlocked = new boolean[count];
            int[] levels = new int[count];
            float[] hp = new float[count];
            float[] maxHp = new float[count];
            int[] sp = new int[count];
            CharStats[] stats = new CharStats[count];
            Set[] skills = new HashSet[count];
            Set recruitUneq = new HashSet<>();
            ItemStack[] weapons = new ItemStack[count];
            ItemStack[] accessories = new ItemStack[count];
            ItemStack[][] armorArr = new ItemStack[count][2];

            for (int i = 0; i < count; i++) {
               unlocked[i] = buf.readBoolean();
               levels[i] = buf.readInt();
               hp[i] = buf.readFloat();
               maxHp[i] = buf.readFloat();
               sp[i] = buf.readInt();
               stats[i] = new CharStats();
               stats[i].offense = buf.readInt();
               stats[i].defense = buf.readInt();
               stats[i].magic = buf.readInt();
               stats[i].iq = buf.readInt();
               stats[i].vitality = buf.readInt();
               stats[i].luck = buf.readInt();
               stats[i].unspent = buf.readInt();
               int sc = buf.readInt();
               skills[i] = new HashSet<>();

               for (int j = 0; j < sc; j++) {
                  skills[i].add(buf.readString(64));
               }

               int uc = buf.readInt();

               for (int j = 0; j < uc; j++) {
                  recruitUneq.add(buf.readString(64));
               }

               boolean hasW = buf.readBoolean();
               if (hasW) {
                  NbtCompound wNbt2 = buf.readNbt();
                  weapons[i] = wNbt2 != null
                     ? ItemStack.fromNbt((WrapperLookup)(client.world != null ? client.world.getRegistryManager() : DynamicRegistryManager.EMPTY), wNbt2)
                        .orElse(ItemStack.EMPTY)
                     : ItemStack.EMPTY;
               } else {
                  weapons[i] = ItemStack.EMPTY;
               }

               boolean hasA = buf.readBoolean();
               if (hasA) {
                  NbtCompound aNbt2 = buf.readNbt();
                  accessories[i] = aNbt2 != null
                     ? ItemStack.fromNbt((WrapperLookup)(client.world != null ? client.world.getRegistryManager() : DynamicRegistryManager.EMPTY), aNbt2)
                        .orElse(ItemStack.EMPTY)
                     : ItemStack.EMPTY;
               } else {
                  accessories[i] = ItemStack.EMPTY;
               }

               for (int aix = 0; aix < 2; aix++) {
                  boolean hasArmorPiece = buf.readBoolean();
                  if (hasArmorPiece) {
                     NbtCompound armNbt2 = buf.readNbt();
                     armorArr[i][aix] = armNbt2 != null
                        ? ItemStack.fromNbt((WrapperLookup)(client.world != null ? client.world.getRegistryManager() : DynamicRegistryManager.EMPTY), armNbt2)
                           .orElse(ItemStack.EMPTY)
                        : ItemStack.EMPTY;
                  } else {
                     armorArr[i][aix] = ItemStack.EMPTY;
                  }
               }
            }

            client.execute(() -> {
               clientActiveRecruit = active;
               clientSecondRecruit = secondActive;
               customRecruitNames = cNames;
               customRecruitClasses = cClasses;
               customRecruitSouls = cSouls;
               customRecruitLevels = cLevels;
               customRecruitHp = cHp;
               customRecruitMaxHp = cMaxHp;
               customRecruitSkillPoints = cSp;
               customRecruitSkills = cSkills;
               customRecruitUnequippedSkills = cUneq;
               customRecruitStats = cStats;
               customRecruitWeapons = cWeapons;
               customRecruitAccessories = cAccessories;
               customRecruitArmor = cArmor;
               clientRecruitUnlocked = unlocked;
               clientRecruitLevels = levels;
               clientRecruitHp = hp;
               clientRecruitMaxHp = maxHp;
               clientRecruitSkillPoints = sp;
               clientRecruitStats = stats;
               clientRecruitSkills = skills;
               clientRecruitWeapons = weapons;
               clientRecruitAccessories = accessories;
               clientRecruitArmor = armorArr;
               clientRecruitUnequippedSkills = recruitUneq;
            });
         }
      );
      Net121.registerClientReceiver(TurnBattleMod.STORY_STATE, (client, handler, buf, sender) -> {
         int unlocked = buf.readInt();
         int questIdx = buf.readInt();
         boolean started = buf.readBoolean();
         boolean complete = buf.readBoolean();
         boolean[] done = new boolean[5];

         for (int i = 0; i < 5; i++) {
            done[i] = buf.readBoolean();
         }

         boolean storyOn = buf.readBoolean();
         client.execute(() -> {
            storyModeEnabled = storyOn;
            StoryChapterScreen.unlockedChapter = unlocked;
            StoryChapterScreen.questIndex = questIdx;
            StoryChapterScreen.questStarted = started;
            StoryChapterScreen.questComplete = complete;

            for (int i = 0; i < 5; i++) {
               StoryChapterScreen.chapterDone[i] = done[i];
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.STORY_DIALOGUE, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         String[] beats = new String[count];

         for (int i = 0; i < count; i++) {
            beats[i] = buf.readString(4096);
         }

         boolean spawnSheepy = buf.readBoolean();
         client.execute(() -> {
            if (client.currentScreen != null && !(client.currentScreen instanceof StoryDialogueScreen)) {
               client.setScreen(null);
            }

            client.setScreen(new StoryDialogueScreen(beats, spawnSheepy));
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.TEXTBOX_SHOW, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         String[] pages = new String[count];

         for (int i = 0; i < count; i++) {
            pages[i] = buf.readString(4096);
         }

         boolean withSound = buf.readBoolean();
         boolean whenFree = buf.isReadable() && buf.readBoolean();
         client.execute(() -> {
            if (whenFree && client.currentScreen != null) {
               pendingTextboxes.add(new Object[]{pages, withSound});
            } else {
               client.setScreen(new TextboxScreen(pages, withSound));
            }
         });
      });
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (!pendingTextboxes.isEmpty() && client.currentScreen == null && client.player != null) {
            Object[] t = pendingTextboxes.remove(0);
            client.setScreen(new TextboxScreen((String[])t[0], (Boolean)t[1]));
         }
      });
      Net121.registerClientReceiver(TurnBattleMod.PROXIMITY_CHAT_SHOW, (client, handler, buf, sender) -> {
         UUID speakerId = buf.readUuid();
         int count = buf.readInt();
         String[] pages = new String[count];

         for (int i = 0; i < count; i++) {
            pages[i] = buf.readString(4096);
         }

         client.execute(() -> ProximityChatRenderer.show(speakerId, pages));
      });
      Net121.registerClientReceiver(TurnBattleMod.STORY_DIALOGUE_DONE, (client, handler, buf, sender) -> {
      });
      Net121.registerClientReceiver(TurnBattleMod.WALL_CLIMB_STATE, (client, handler, buf, sender) -> {
         boolean climbingNow = buf.readBoolean();
         client.execute(() -> {
            isWallClimbing = climbingNow;
            if (!climbingNow) {
               isClimbStepping = false;
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.WALL_CLIMB_STEPPING, (client, handler, buf, sender) -> {
         boolean stepping = buf.readBoolean();
         client.execute(() -> isClimbStepping = stepping);
      });
      Net121.registerClientReceiver(TurnBattleMod.WALL_CLIMB_TELEGRAPH, (client, handler, buf, sender) -> {
         boolean active = buf.readBoolean();
         if (!active) {
            client.execute(WallClimbTelegraphRenderer::clear);
         } else {
            BlockPos target = buf.readBlockPos();
            Direction wallOut = Direction.values()[buf.readInt()];
            boolean valid = buf.readBoolean();
            client.execute(() -> WallClimbTelegraphRenderer.set(target, wallOut, valid));
         }
      });
      Net121.registerClientReceiver(
         TurnBattleMod.MORALITY_EVENT,
         (client, handler, buf, sender) -> {
            String type = buf.readString(32);
            boolean ruthless = buf.readBoolean();
            client.execute(
               () -> {
                  isRuthless = ruthless;
                  switch (type) {
                     case "KILL_WARNING": {
                        String[] lines = new String[]{
                           "* You feel a little less like yourself. Your hand trembles as you grip your weapon. You think, \"Maybe we should try hitting a little softer.\""
                        };
                        showAfterBattle(client, lines, null);
                        break;
                     }
                     case "RUTHLESS": {
                        String[] lines = new String[]{"* You start to feel a little more like yourself."};
                        Runnable playSound = () -> client.player
                              .playSound((SoundEvent)Registries.SOUND_EVENT.get(Identifier.of("turnbattle", "snd_ominous")), 1.0F, 1.0F);
                        showAfterBattle(client, lines, playSound);
                        break;
                     }
                     case "SPARE_CANCEL":
                        playAfterBattle(client, "turnbattle:snd_ominous_cancel");
                     case "SYNC":
                  }
               }
            );
         }
      );
      ClientSendMessageEvents.ALLOW_CHAT.register((AllowChat)message -> {
         if (!proximityChatEnabled) {
            return true;
         } else {
            PacketByteBuf sendBuf = PacketByteBufs.create();
            sendBuf.writeString(message, 256);
            Net121.sendToServer(TurnBattleMod.PROXIMITY_CHAT_SEND, sendBuf);
            return false;
         }
      });
      HudRenderCallback.EVENT
         .register(
            (HudRenderCallback)(drawContext, tickDelta) -> {
               MinecraftClient mc = MinecraftClient.getInstance();
               DarkFountainClient.renderOverlay(drawContext, mc);
               SoulGlowRenderer.renderHud(drawContext);
               MobPlateRenderer.renderHud(drawContext);
               SoulWalkClient.renderHud(drawContext);
               if (mc.currentScreen == null || !mc.currentScreen.shouldPause()) {
                  ProximityChatRenderer.renderHud(drawContext);
               }

               DarkFountainSealEffect.renderSoulsHud(drawContext);
               DarkFountainSealEffect.renderFlash(drawContext, mc);
               if (mc.currentScreen == null && mc.world != null && !LiveCombatClient.hudShown()) {
                  WorldDamageHud.render(drawContext);
               }

               if (mc.currentScreen == null && mc.world != null) {
                  OverhaulView.renderWatching(drawContext, tickDelta.getTickDelta(false));
               }

               LiveCombatClient.render(drawContext, tickDelta.getTickDelta(false));
               MobFates.renderHud(drawContext);
               PlayerActions.renderHud(drawContext, tickDelta.getTickDelta(false));
               RogueliteClient.renderHud(drawContext, mc);
               LightPillarClient.renderHud(drawContext);
               DarkWorldFall.renderHud(drawContext);
               if (establishingConnectionTicks > 0) {
                  establishingConnectionTicks--;
                  int w = mc.getWindow().getScaledWidth();
                  int h = mc.getWindow().getScaledHeight();
                  float fadeOut = establishingConnectionTicks < 30 ? (float)establishingConnectionTicks / 30.0F : 1.0F;
                  float pulse = (float)(0.75 + 0.25 * Math.sin((double)establishingConnectionTicks * 0.133));
                  float finalAlpha = pulse * fadeOut;
                  int alpha = (int)(finalAlpha * 255.0F);
                  drawContext.fill(0, 0, w, h, (int)(finalAlpha * 0.6F * 255.0F) << 24);
                  String msg = "Establishing Connection";
                  int tw = mc.textRenderer.getWidth(msg);
                  RenderSystem.enableBlend();
                  drawContext.drawTextWithShadow(
                     mc.textRenderer, Text.literal(msg).formatted(Formatting.WHITE), w / 2 - tw / 2, h / 2 - 9 / 2, alpha << 24 | 16777215
                  );
               }
            }
         );
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
         WorldDamageHud.tick();
         SoulGlowRenderer.tick();
         SavePointRenderer.tick();
         BattleEntityRenderer.tick();
         ProximityChatRenderer.tick();
         OverhaulView.tickWatching();
         PlayerActions.tick(mc);
         DarkWorldFall.tick(mc);
         LightPillarClient.tick(mc, inspectKey);
         GuiScaleGuard.tick(mc);
         RogueliteClient.tick(mc);
         LiveCombatClient.tick(mc);
         MobFates.tick();
         if (OverhaulView.isActive() && !(mc.currentScreen instanceof BattleScreen)) {
            OverhaulView.end();
         }
      });
      WorldRenderEvents.AFTER_ENTITIES.register((AfterEntities)ctx -> {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            SoulGlowRenderer.computeScreenPositions(ctx);
            SoulWalkClient.compute(ctx);
            MobPlateRenderer.compute(ctx);
            DarkFountainSealEffect.renderWorld(ctx);
            DarkFountainCreateSparkleEffect.render(ctx);
            DarkFountainPillarRenderer.render(ctx);
            DarkFountainGeyserRenderer.render(ctx);
            LightPillarClient.render(ctx);
            DarkFountainDoorGlow.render(ctx);
            WallClimbTelegraphRenderer.render(ctx);
            SavePointRenderer.render(ctx);
            ProximityChatRenderer.render(ctx);
            PlayerActions.renderWorld(ctx);
            MobFates.renderWorld(ctx);
         }
      });
      WorldRenderEvents.LAST.register((Last)ctx -> {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null) {
            DarkFountainSealEffect.computeSoulScreenPositions(ctx);
            OverhaulView.captureWorld(ctx);
         }
      });
      Net121.registerClientReceiver(TurnBattleMod.SAVE_POINT_OPEN, (client, handler, buf, sender) -> {
         int entityId = buf.readInt();
         client.execute(() -> {
            if (client.world != null) {
               Entity e = client.world.getEntityById(entityId);
               SavePointEntity sp = e instanceof SavePointEntity ? (SavePointEntity)e : null;
               client.setScreen(new SavePointScreen(sp));
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.PLAYER_EQUIP_BROADCAST, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         Map players = new LinkedHashMap<>();

         for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUuid();
            players.put(uuid, buf.readBoolean() ? buf.readNbt() : null);
         }

         Map recruits = new LinkedHashMap<>();
         boolean hasRecruitSection = buf.readableBytes() > 0;
         if (hasRecruitSection) {
            int rc = buf.readInt();

            for (int i = 0; i < rc; i++) {
               String name = buf.readString();
               recruits.put(name, buf.readBoolean() ? buf.readNbt() : null);
            }
         }

         client.execute(() -> {
            for (Entry e : players.entrySet()) {
               remoteEquippedWeapons.put(e.getKey(), decodeStack(client, e.getValue()));
            }

            if (hasRecruitSection) {
               remoteRecruitWeapons.clear();

               for (Entry e : recruits.entrySet()) {
                  remoteRecruitWeapons.put(e.getKey(), decodeStack(client, e.getValue()));
               }
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.SAVE_POINT_ACK, (client, handler, buf, sender) -> {
         long ptSec = buf.readLong();
         int svLv = buf.readInt();
         client.execute(() -> {
            clientPlaytimeSec = ptSec;
            clientLevel = svLv;
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.SOUL_GLOW, (client, handler, buf, sender) -> {
         UUID uuid = buf.readUuid();
         boolean hasSoul = buf.readBoolean();
         int soulOrd = buf.readInt();
         boolean on = buf.readBoolean();
         SoulType soul = hasSoul && soulOrd >= 0 && soulOrd < SoulType.values().length ? SoulType.values()[soulOrd] : null;
         client.execute(() -> SoulGlowRenderer.setGlowing(uuid, soul, on));
      });
      Net121.registerClientReceiver(TurnBattleMod.SOUL_REMOVED, (client, handler, buf, sender) -> client.execute(() -> {
            clientSoulRemoved = true;
            SoulGlowRenderer.setManualSummon(false);
            WorldDamageHud.setRHeld(false);
            if (lastRevealHeld) {
               lastRevealHeld = false;
               PacketByteBuf rb = PacketByteBufs.create();
               rb.writeBoolean(false);
               Net121.sendToServer(TurnBattleMod.SOUL_REVEAL, rb);
            }
         }));
      Net121.registerClientReceiver(TurnBattleMod.SOUL_RESTORED, (client, handler, buf, sender) -> client.execute(() -> clientSoulRemoved = false));
      Net121.registerClientReceiver(
         TurnBattleMod.ESTABLISHING_CONNECTION, (client, handler, buf, sender) -> client.execute(() -> establishingConnectionTicks = 120)
      );
      Net121.registerClientReceiver(TurnBattleMod.WORLD_DAMAGE_HUD, (client, handler, buf, sender) -> {
         float currHp = buf.readFloat();
         float maxHp = buf.readFloat();
         float dmg = buf.readFloat();
         String pname = buf.readString();
         client.execute(() -> WorldDamageHud.onDamage(currHp, maxHp, dmg, pname, null));
      });
      Net121.registerClientReceiver(TurnBattleMod.PLAYER_PARTY_STATE, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         List members = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString();
            String vesselName = buf.readString();
            int soulOrd = buf.readInt();
            float hp = buf.readFloat();
            float maxHp = buf.readFloat();
            SoulType soul = soulOrd >= 0 && soulOrd < SoulType.values().length ? SoulType.values()[soulOrd] : null;
            members.add(new TurnBattleClientMod.PartyMemberInfo(uuid, name, vesselName, soul, hp, maxHp));
         }

         client.execute(() -> {
            clientPartyPlayers.clear();
            clientPartyPlayers.addAll(members);
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.PARTY_MEMBER_DAMAGE, (client, handler, buf, sender) -> {
         float hp = buf.readFloat();
         float maxHp = buf.readFloat();
         float dmg = buf.readFloat();
         String name = buf.readString();
         UUID uuid = buf.readUuid();
         int soulOrd = buf.readInt();
         if (soulOrd >= 0 && soulOrd < SoulType.values().length) {
            SoulType var11 = SoulType.values()[soulOrd];
         } else {
            Object var10000 = null;
         }

         client.execute(() -> {
            for (int i = 0; i < clientPartyPlayers.size(); i++) {
               if (clientPartyPlayers.get(i).uuid().equals(uuid)) {
                  TurnBattleClientMod.PartyMemberInfo old = clientPartyPlayers.get(i);
                  clientPartyPlayers.set(i, new TurnBattleClientMod.PartyMemberInfo(uuid, old.name(), old.vesselName(), old.soul(), hp, maxHp));
                  break;
               }
            }

            if (dmg < 0.0F) {
               WorldDamageHud.onHeal(hp, maxHp, -dmg, name, uuid);
            } else {
               WorldDamageHud.onDamage(hp, maxHp, dmg, name, uuid);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.PARTY_INVITE_NOTIFY, (client, handler, buf, sender) -> {
         String inviterName = buf.readString();
         UUID inviterUuid = buf.readUuid();
         client.execute(() -> {
            if (!(client.currentScreen instanceof BattleScreen)) {
               client.setScreen(new PartyInviteScreen(inviterName, inviterUuid, client.currentScreen));
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.BATTLE_CHAT_MSG, (client, handler, buf, sender) -> {
         UUID senderUuid = buf.readUuid();
         String senderName = buf.readString(64);
         String msg = buf.readString(256);
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen bs) {
               bs.receiveBattleChat(senderUuid, senderName, msg);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_ENTER, (client, handler, buf, sender) -> {
         String key = buf.readString(128);
         int floor = buf.readInt();
         boolean boss = buf.readBoolean();
         float stx = buf.readFloat();
         float sty = buf.readFloat();
         boolean hasSoul = buf.readBoolean();
         SoulType soul = null;
         if (hasSoul) {
            int sOrd = buf.readInt();
            if (sOrd >= 0 && sOrd < SoulType.values().length) {
               soul = SoulType.values()[sOrd];
            }
         }

         SoulType fSoul = soul;
         client.execute(() -> {
            if (client.currentScreen instanceof BattleScreen) {
               if (pendingDepthsScreen != null) {
                  pendingDepthsScreen.currentFloor = floor;
                  pendingDepthsScreen.bossFloor = boss;
                  pendingDepthsScreen.fountainKey = key;
                  pendingDepthsScreen.mySoul = fSoul;
               }
            } else {
               DepthsDescentScreen screen = pendingDepthsScreen;
               if (screen == null) {
                  if (client.currentScreen instanceof DepthsDescentScreen dds) {
                     screen = dds;
                  } else {
                     screen = new DepthsDescentScreen();
                  }
               }

               pendingDepthsScreen = null;
               screen.posX = stx;
               screen.posY = sty;
               screen.currentFloor = floor;
               screen.bossFloor = boss;
               screen.fountainKey = key;
               screen.mySoul = fSoul;
               if (!(client.currentScreen instanceof DepthsDescentScreen)) {
                  client.setScreen(screen);
               }
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_ROOM_DATA, (client, handler, buf, sender) -> {
         int[][] tiles = new int[56][80];
         int[][] ftex = new int[56][80];

         for (int y = 0; y < 56; y++) {
            for (int x = 0; x < 80; x++) {
               tiles[y][x] = buf.readByte();
               ftex[y][x] = buf.readByte();
            }
         }

         int chestCount = buf.readInt();
         List chests = new ArrayList<>();

         for (int i = 0; i < chestCount; i++) {
            chests.add(new int[]{buf.readInt(), buf.readInt(), buf.readInt()});
         }

         int stx = buf.readInt();
         int sty = buf.readInt();
         client.execute(() -> {
            if (client.currentScreen instanceof DepthsDescentScreen dds) {
               dds.tiles = tiles;
               dds.floorTexture = ftex;
               dds.chestPositions.clear();
               dds.chestPositions.addAll(chests);
               dds.stairsTx = stx;
               dds.stairsTy = sty;
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_EXIT, (client, handler, buf, sender) -> client.execute(() -> {
            if (client.currentScreen instanceof DepthsDescentScreen) {
               client.setScreen(null);
            }
         }));
      Net121.registerClientReceiver(TurnBattleMod.DD_ENCOUNTER, (client, handler, buf, sender) -> {
         boolean active = buf.readBoolean();
         client.execute(() -> {
            if (active) {
               if (client.currentScreen instanceof DepthsDescentScreen dds) {
                  dds.setEncounter(true);
                  dds.enemyPositions.clear();
                  dds.bossDefeated = true;
                  pendingDepthsScreen = dds;
               }
            } else if (client.currentScreen instanceof DepthsDescentScreen dds) {
               dds.encounterActive = false;
            } else if (pendingDepthsScreen != null) {
               pendingDepthsScreen.encounterActive = false;
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_CHEST_RESULT, (client, handler, buf, sender) -> {
         int idx = buf.readInt();
         boolean hasRoom = buf.readBoolean();
         String itemName = hasRoom ? buf.readString(64) : "";
         int count = hasRoom ? buf.readInt() : 0;
         client.execute(() -> {
            if (client.currentScreen instanceof DepthsDescentScreen dds) {
               dds.showChestResult(idx, hasRoom, itemName, count);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_STAIRS_PROMPT, (client, handler, buf, sender) -> {
         int floor = buf.readInt();
         boolean boss = buf.readBoolean();
         boolean exhausted = buf.readBoolean();
         client.execute(() -> {
            if (client.currentScreen instanceof DepthsDescentScreen dds) {
               dds.showStairsVote(floor, boss, exhausted);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_NEXT_FLOOR, (client, handler, buf, sender) -> {
         int floor = buf.readInt();
         float spawnPx = buf.readFloat();
         float spawnPy = buf.readFloat();
         client.execute(() -> {
            if (client.currentScreen instanceof DepthsDescentScreen dds) {
               dds.advanceToFloor(floor, spawnPx, spawnPy);
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_ENEMIES_SYNC, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         float[][] positions = new float[count][2];

         for (int i = 0; i < count; i++) {
            positions[i][0] = buf.readFloat();
            positions[i][1] = buf.readFloat();
         }

         client.execute(() -> {
            DepthsDescentScreen dds = null;
            if (client.currentScreen instanceof DepthsDescentScreen d) {
               dds = d;
            } else if (pendingDepthsScreen != null) {
               dds = pendingDepthsScreen;
            }

            if (dds != null) {
               dds.enemyPositions.clear();

               for (float[] p : positions) {
                  dds.enemyPositions.add(p);
               }
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_TIMER_SYNC, (client, handler, buf, sender) -> {
         int seconds = buf.readInt();
         client.execute(() -> {
            if (client.currentScreen instanceof DepthsDescentScreen dds) {
               dds.encounterTimerSeconds = seconds;
            }

            if (pendingDepthsScreen != null) {
               pendingDepthsScreen.encounterTimerSeconds = seconds;
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.DD_PLAYERS_SYNC, (client, handler, buf, sender) -> {
         int count = buf.readInt();
         List list = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            UUID uid = buf.readUuid();
            float tx = buf.readFloat();
            float ty = buf.readFloat();
            int dir = buf.readInt();
            boolean enc = buf.readBoolean();
            String name = buf.readString(32);
            boolean hasSoul = buf.readBoolean();
            SoulType soul = null;
            if (hasSoul) {
               int sOrd = buf.readInt();
               if (sOrd >= 0 && sOrd < SoulType.values().length) {
                  soul = SoulType.values()[sOrd];
               }
            }

            list.add(new DepthsDescentScreen.OtherPlayer(uid, tx, ty, dir, enc, name, soul));
         }

         client.execute(() -> {
            if (client.currentScreen instanceof DepthsDescentScreen dds) {
               dds.updateOtherPlayers(list);
            }

            if (client.player != null) {
               UUID me = client.player.getUuid();

               for (DepthsDescentScreen.OtherPlayer op : list) {
                  if (op.uuid().equals(me) && client.currentScreen instanceof DepthsDescentScreen dds) {
                     break;
                  }
               }
            }
         });
      });
      Net121.registerClientReceiver(TurnBattleMod.QUESTLINE_STATE, (client, handler, buf, sender) -> {
         String activeId = buf.readString(32);
         int nodeIndex = buf.readInt();
         boolean nodeDone = buf.readBoolean();
         int completed = buf.readInt();
         Set completedSet = new HashSet<>();

         for (int i = 0; i < completed; i++) {
            completedSet.add(buf.readString(32));
         }

         client.execute(() -> {
            clientActiveQuestlineId = activeId;
            clientQuestlineNodeIndex = nodeIndex;
            clientQuestlineNodeDone = nodeDone;
            clientCompletedQuestlines.clear();
            clientCompletedQuestlines.addAll(completedSet);
            if (client.currentScreen instanceof PartyScreen ps) {
               ps.onQuestlineState();
            }
         });
      });
   }

   private static void showAfterBattle(MinecraftClient client, String[] lines, Runnable onClose) {
      if (!(client.currentScreen instanceof BattleScreen)) {
         client.setScreen(new InspectScreen(lines, onClose));
      } else {
         boolean[] fired = new boolean[]{false};
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)c -> {
            if (!fired[0]) {
               if (!(c.currentScreen instanceof BattleScreen)) {
                  fired[0] = true;
                  c.setScreen(new InspectScreen(lines, onClose));
               }
            }
         });
      }
   }

   private static void playAfterBattle(MinecraftClient client, String soundId) {
      if (!(client.currentScreen instanceof BattleScreen)) {
         if (client.player != null) {
            client.player.playSound((SoundEvent)Registries.SOUND_EVENT.get(Identifier.tryParse(soundId)), 1.0F, 1.0F);
         }
      } else {
         boolean[] fired = new boolean[]{false};
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)c -> {
            if (!fired[0]) {
               if (!(c.currentScreen instanceof BattleScreen)) {
                  fired[0] = true;
                  if (c.player != null) {
                     c.player.playSound((SoundEvent)Registries.SOUND_EVENT.get(Identifier.tryParse(soundId)), 1.0F, 1.0F);
                  }
               }
            }
         });
      }
   }

   private static InspectRegistry.Entry resolveInspectEntry(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         HitResult hit = client.crosshairTarget;
         if (hit != null && hit.getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
            if (hit instanceof EntityHitResult ehr) {
               String eid = Registries.ENTITY_TYPE.getId(ehr.getEntity().getType()).getPath();
               InspectRegistry.Entry e = InspectRegistry.forEntity(eid);
               if (e != null) {
                  return e;
               }
            }

            if (hit instanceof BlockHitResult bhr) {
               BlockPos pos = bhr.getBlockPos();
               BlockState bs = client.world.getBlockState(pos);
               String bid = Registries.BLOCK.getId(bs.getBlock()).getPath();
               InspectRegistry.Entry e = InspectRegistry.forBlock(bid);
               if (e != null) {
                  return e;
               }
            }
         }

         int y = (int)client.player.getY();
         RegistryEntry biomeEntry = client.world.getBiome(client.player.getBlockPos());
         String biomePath = biomeEntry.getKey().map(k -> k.getValue().getPath()).orElse("");
         if (biomePath.contains("deep_dark")) {
            InspectRegistry.Entry e = InspectRegistry.forSpecial("deep_dark");
            if (e != null) {
               return e;
            }
         }

         if (y < -10) {
            InspectRegistry.Entry e = InspectRegistry.forSpecial("deep_underground");
            if (e != null) {
               return e;
            }
         }

         if (!biomePath.isEmpty()) {
            InspectRegistry.Entry e = InspectRegistry.forBiome(biomePath);
            if (e != null) {
               return e;
            }
         }

         return InspectRegistry.getDefault();
      } else {
         return InspectRegistry.getDefault();
      }
   }

   private static CharStats readCharStats(PacketByteBuf buf) {
      CharStats s = new CharStats();
      s.offense = buf.readInt();
      s.defense = buf.readInt();
      s.magic = buf.readInt();
      s.iq = buf.readInt();
      s.vitality = buf.readInt();
      s.luck = buf.readInt();
      s.unspent = buf.readInt();
      return s;
   }

   @Environment(EnvType.CLIENT)
   private static void handleSoulExtractionTick(MinecraftClient client) {
      if (client.player != null && !clientSoulRemoved && clientSoul != null && !SoulWalk.localOut) {
         Key rkb = InputUtil.fromKeyCode(removeSoulKey.getDefaultKey().getCode(), 0);

         try {
            rkb = KeyBindingHelper.getBoundKeyOf(removeSoulKey);
         } catch (Exception var4) {
         }

         boolean removeHeld = rkb.getCategory() == Type.KEYSYM && InputUtil.isKeyPressed(client.getWindow().getHandle(), rkb.getCode());
         if (clientPlayerRace != CharacterRace.MONSTER && !DepthsClient.inRun()) {
            if (!removeHeld) {
               soulRipWaitRelease = false;
            }

            if (!removeHeld || !soulRipWaitRelease) {
               if (removeHeld) {
                  if (soulExtractHoldTicks == 0) {
                     if (PlayerActions.isActing(client.player.getUuid())) {
                        return;
                     }

                     PlayerActions.startLocalSoulRip(clientSoul);
                     sendSoulRipState(true);
                     SoundEvent se = (SoundEvent)Registries.SOUND_EVENT.get(Identifier.of("turnbattle", "snd_removesoul"));
                     if (se != null) {
                        removeSoulSoundInstance = PositionedSoundInstance.master(se, 1.0F, 1.0F);
                        client.getSoundManager().play(removeSoulSoundInstance);
                     }
                  }

                  soulExtractHoldTicks++;
                  if (soulExtractHoldTicks >= 60) {
                     PlayerActions.finishLocalSoulRip();
                     soulRipShown = false;
                     soulRipWaitRelease = true;
                     Net121.sendToServer(TurnBattleMod.SOUL_REMOVE_REQUEST, PacketByteBufs.empty());
                     soulExtractHoldTicks = 0;
                     removeSoulSoundInstance = null;
                  }
               } else if (soulExtractHoldTicks > 0) {
                  cancelSoulExtraction(client);
               }
            }
         } else {
            cancelSoulExtraction(client);
         }
      } else {
         cancelSoulExtraction(client);
      }
   }

   private static void sendSoulRipState(boolean on) {
      soulRipShown = on;
      PacketByteBuf b = PacketByteBufs.create();
      b.writeBoolean(on);
      Net121.sendToServer(TurnBattleMod.SOUL_RIP_STATE, b);
   }

   private static void cancelSoulExtraction(MinecraftClient client) {
      if (soulRipShown) {
         PlayerActions.cancelLocalSoulRip();
         sendSoulRipState(false);
      }

      soulExtractHoldTicks = 0;
      if (removeSoulSoundInstance != null) {
         client.getSoundManager().stop(removeSoulSoundInstance);
         removeSoulSoundInstance = null;
      }
   }

   static {
      int n = Recruit.values().length;
      clientRecruitUnlocked = new boolean[n];
      clientRecruitLevels = new int[n];
      clientRecruitHp = new float[n];
      clientRecruitMaxHp = new float[n];
      clientRecruitSkillPoints = new int[n];
      clientRecruitStats = new CharStats[n];
      clientRecruitSkills = new HashSet[n];
      clientRecruitWeapons = new ItemStack[n];

      for (int i = 0; i < n; i++) {
         clientRecruitStats[i] = new CharStats();
         clientRecruitSkills[i] = new HashSet<>();
         clientRecruitWeapons[i] = ItemStack.EMPTY;
      }

      clientActiveQuestlineId = "";
      clientQuestlineNodeIndex = 0;
      clientQuestlineNodeDone = false;
      clientCompletedQuestlines = new HashSet<>();
      clientPartyMember = 0;
      clientPartySkillPoints = new int[PartyMember.values().length];
      clientPartyUnlockedSkills = new HashSet[PartyMember.values().length];
      rogueElapsedTicks = -1L;
      rogueliteInRun = false;
      isRogueliteWorld = false;
      darkFountainBattlesRemaining = 0;
      darkFountainBattlesRequired = 0;
      isRuthless = false;
      vesselChoice = 0;
      storyModeEnabled = false;
      clientPartyPlayers = new CopyOnWriteArrayList<>();
      publicInvites = true;
      lightWorldUi = false;
      battleUi = BattleUi.DELTARUNE;
      showBattleDebug = true;
      CLIENT_PREFS_FILE = new File("config/turnbattle_client.json");
      pendingDepthsScreen = null;
      onlinePlayers = new CopyOnWriteArrayList<>();
      clientRogueSkills = new ArrayList<>();
      clientUniqueCards = new ArrayList<>();
      clientCardStatBonuses = new int[6];
      proximityChatEnabled = true;
      defaultBattleKeys = new IdentityHashMap<>();
      revealSnapWait = false;
      soulRipShown = false;
      soulRipWaitRelease = false;
   }

   public static record OnlinePlayerEntry(String name, UUID uuid) {
   }

   public static record PartyMemberInfo(UUID uuid, String name, String vesselName, SoulType soul, float hp, float maxHp) {
   }
}
