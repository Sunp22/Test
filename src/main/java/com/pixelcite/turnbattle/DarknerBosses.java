package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap.Type;

public final class DarknerBosses {
   public static EntityType<DarknerBossEntity> TYPE;
   private static final Random RNG = new Random();
   public static final Identifier BOSS_TALK = Identifier.of("turnbattle", "darkner_boss_talk");
   public static final Identifier BOSS_TALK_REQ = Identifier.of("turnbattle", "darkner_boss_talk_req");
   public static final Identifier BOSS_CHALLENGE = Identifier.of("turnbattle", "darkner_boss_challenge");
   public static final int PERSONALITIES = 8;
   public static final String[] PERSONALITY_NAMES = new String[]{
      "A noble", "A gentle", "A nervous", "A boastful", "A cheerful", "A gloomy", "A theatrical", "A stern"
   };
   private static final String[] TITLES = new String[]{"Steadfast", "Unyielding", "Dreamer", "Grand", "Keeper", "Last Guard", "Radiant", "Unforgotten"};
   static final String[] DREAMS = new String[]{
      "to be held by someone again, like I was before I was packed away",
      "to see what lies past the edge of this world",
      "to be useful again, instead of gathering dust in a box",
      "to build a tower so tall the Light World can see it",
      "to have a name that people remember",
      "to find the rest of my stack, because they're out here somewhere, I know it",
      "to turn this place into a real kingdom, banners and all",
      "to sleep somewhere warm, without a lid closing over me"
   };
   static final String[] REASONS = new String[]{
      "If it's sealed, this whole world goes dark, and everyone in it goes with it.",
      "Before the Fountain, I was just a thing in a chest. Now I can move. I can think.",
      "Everyone here has only just started living. It isn't fair to end it now.",
      "Whoever made it opened it for a reason. It isn't ours to undo.",
      "I haven't finished my dream yet. I just need more time.",
      "Your world forgot about me. This one didn't.",
      "Closing it now would be like shutting a book halfway through.",
      "It's the only light in this place. Without it, there's nothing left to guard."
   };
   private static final String[] GREET = new String[]{
      "Halt, traveller. I am %s, sworn guardian of this Fountain.",
      "Oh! Hello there. I'm %s. I look after the Fountain... I hope that's alright.",
      "Ah- AH! A visitor! I-I'm %s! I guard the Fountain! Officially! Mostly!",
      "Behold! It is I, %s! The greatest guardian any Fountain ever had!",
      "Hiii! I'm %s! Welcome welcome WELCOME to the Fountain!",
      "...Oh. Someone came. I'm %s. I guard the Fountain. Not that anyone asked.",
      "At last, the curtain rises! Enter: %s, keeper of the Fountain!",
      "Stop there. %s. Fountain guard. State your business."
   };
   private static final String[] DREAM_LINE = new String[]{
      "Do you know what I fight for? I have a dream, %s. Without the Fountain, it dies with me.",
      "I have a dream, you see. It's small, maybe. I want %s. Please don't take that away.",
      "Because I have a dream, okay?! It's... it's %s! It's silly, I know! But it's mine!",
      "You know what greatness needs? A dream. Mine is %s. And I will achieve it, magnificently!",
      "'Cause I've got a dream! It's %s! Isn't that great?! I just need a little more time!",
      "I had a dream once. Still do. I wanted %s. Stupid, isn't it?",
      "Behold my dream, my grand ambition: %s! What is a story without a dream?",
      "I have one goal: %s. I will not abandon it."
   };
   private static final String[] REASON_LINE = new String[]{
      "And it is not only mine to lose. %s",
      "And it's not just about me. %s",
      "And- and that's not even all! %s",
      "But a dream needs a stage. %s",
      "Plus, plus, plus! %s So you see? It has to stay!",
      "And if the Fountain goes... %s",
      "And should the curtain fall... %s Is that the ending you'd write?",
      "This is not about me alone. %s"
   };
   private static final String[][] MAKER_ABSENT = new String[][]{
      {
            "The one who opened this Fountain gave us all of it. I never saw their face... but I know their heart was great.",
            "I swore to guard what they made. A knight does not break an oath."
      },
      {
            "Whoever opened the Fountain... I never got to see them. I don't even know their face. But they gave me a chance to be something.",
            "I think they must be very kind. I only want to keep what they made safe for them. Is that so wrong?"
      },
      {
            "The one who made the Fountain... I've never seen them. Not even once. I don't know what they look like!",
            "But they must be so brave. Way braver than me. I can't just let their world disappear!"
      },
      {
            "The Fountain's maker? Never seen them. I bet they were nearly as amazing as me.",
            "Anyone who can create a whole world deserves a guardian worthy of it. Lucky for them, they got me!"
      },
      {
            "I've never even met whoever made the Fountain! I don't know what they look like at all!",
            "But I just know they're super nice! When they come back, I want them to find everything still here!"
      },
      {
            "The one who made this place never came back. I don't even know what they look like.",
            "...But they made something beautiful. I'm the only one left who seems to care."
      },
      {
            "The Fountain's maker! Our playwright! Their face unseen, their name unknown to me!",
            "Yet what a masterpiece they left us! I will not let their work close in the first act!"
      },
      {"I never saw the one who opened the Fountain. I don't need to.", "They built this world. I keep it standing. That is my post."}
   };
   private static final String[][] MAKER_PRESENT = new String[][]{
      {
            "The one who opened this Fountain gave us all of it. I never saw their face, but I remember what the dark showed me. %s",
            "%s A guardian remembers such things. I owe them everything, and I will not dishonour them."
      },
      {
            "Whoever opened the Fountain... I never got to see their face. I only caught little things, in the dark. %s",
            "%s That's all I know. But it's enough to know they cared about this place."
      },
      {
            "The one who made the Fountain... I never saw their face! But I- I remember bits! %s",
            "%s Th-that's all I've got! But it's enough! They made this, and I'm keeping it safe!"
      },
      {
            "The Fountain's maker? Never saw their face. But someone as perceptive as me notices things. %s",
            "%s See? Nothing gets past me. Which is exactly why nothing gets past me now, either!"
      },
      {
            "I never saw the Fountain-maker's face! But I remember stuff, from when the world woke up! %s",
            "%s Cool, right?! I hope they come back someday. I want them to see everything's still here!"
      },
      {"The one who made this place... I never saw their face. All I have are scraps. %s", "%s ...That's all I have of them. I'm not giving up the rest."},
      {
            "The Fountain's maker! Their face unseen... yet clues whispered to me from the dark! %s",
            "%s Such is the portrait of our playwright! And I, their humble stagehand, shall guard their work!"
      },
      {"Never saw the maker's face. I know a few facts. %s", "%s That is all I know. It is enough to stand here for."}
   };
   private static final String[] PROMPT = new String[]{
      "I will not let you seal this Fountain. If you would try, you must first face me.",
      "I'm sorry, but I can't let you seal the Fountain. If you insist... we'll have to fight.",
      "Y-you can't seal the Fountain! I won't let you! ...Unless you beat me. Please don't.",
      "Seal the Fountain? Not while I'm here! Get past me first. Ha! Good luck.",
      "Oh, but you can't seal the Fountain! Nope! Not unless you beat me in a fight!",
      "You want to seal it. I won't let you. Challenge me, or don't.",
      "Seal the Fountain? Never! Not while this guardian still treads the stage!",
      "The Fountain stays open. Seal it and you go through me. Your call."
   };
   private static final String[] BATTLE_OPEN = new String[]{
      "You will not win this, traveller. This Fountain is all that holds my world together, and I will not yield it.",
      "I'm sorry... I really am. But I can't let you win this. I need the Fountain. We all do.",
      "O-okay! Listen! You're NOT winning this! I- I need the Fountain! Everyone does!",
      "Ha! Did you really think you could win? Against ME? This Fountain is mine to protect, and I never lose!",
      "Okay! Here's the thing! You're not gonna win, 'cause I really, REALLY need the Fountain!",
      "...You won't win. I know how this goes. But I need the Fountain more than you need it gone.",
      "Hear me! In this tale, the hero does NOT win! The Fountain is the heart of our stage, and I shall not let it go dark!",
      "Listen well. You will not win. The Fountain stays open."
   };
   private static final String[][] FILLER = new String[][]{
      {
            "So I beg of you, as one who also fights for something: lower your weapon.",
            "And if you will not... then I will stand here until the very end. You will not pass."
      },
      {"Please. I don't want to hurt you. I just want to keep my home.", "But if you keep going... I'll keep standing. I have to. I won't let you win."},
      {"Please, please just stop! I'm scared, okay?! I'm really scared!", "B-but I'm not moving! I'm not! You... you won't win! You can't!"},
      {
            "...Fine! Maybe I need this more than I let on. Maybe I'm asking you. Just this once.",
            "But don't think for a second that means you'll win! I'm the greatest! I'll prove it!"
      },
      {
            "C'mon, let's stop! We could be friends! We could have so much fun together!",
            "But if you won't stop... then I won't either! Smile, okay? 'Cause you're not winning this!"
      },
      {"So just... leave it. Please. Let one thing stay the way it is.", "You won't win. Not because I'm strong. Because I have nothing else to lose."},
      {
            "So I implore you, from the depths of my soul! Lay down your arms! Let the show go on!",
            "And if you will not... then this is my finest scene! You will NOT win!"
      },
      {"Stand down. That is the only request I will make.", "Refuse, and I hold this line until I fall. You will not win."}
   };
   private static final String[] LOW_HP = new String[]{
      "I... still stand. A guardian does not fall so easily.",
      "Ow... I'm okay. I'm okay...",
      "Th-that really hurt! Is this the end?!",
      "A scratch! Just a scratch! ...A big scratch.",
      "Hehe... ow. That one stung!",
      "Maybe this is how it ends. Figures.",
      "Is this... my final scene?",
      "Still standing."
   };
   private static final String[] MERCY_READY = new String[]{
      "...You've made your point. I yield.",
      "I don't want to fight anymore. Do you?",
      "C-can we stop now? Please?",
      "Fine! I'll let you win. This time.",
      "Let's call it a tie! Friends?",
      "...I'm tired. You can go.",
      "And so the hero earns their victory! Curtain call!",
      "Enough. You've earned it."
   };
   private static final String[] SPARED = new String[]{
      "Go, then. The Fountain is yours to decide.",
      "Please... be kind to this world, whatever you choose.",
      "Okay. I'll just... stand over here. Out of the way.",
      "I let you win! Remember that!",
      "That was fun! Come visit, okay?",
      "Do what you want. I'll watch.",
      "The hero passes! Exit the guardian, stage left!",
      "Pass."
   };
   private static final String[] DEFEATED = new String[]{
      "My watch... ends here.",
      "I'm sorry... I tried...",
      "I knew it... I knew I couldn't...",
      "Impossible... I was... the greatest...",
      "Aw... I wanted... to play more...",
      "...Figures.",
      "And so... the curtain... falls...",
      "...Dismissed."
   };
   private static final String[][][] ACTS = new String[][][]{
      {
            {"Bow", "You bow deeply. The guardian returns the gesture.", "30", "A fine bow! You have manners, at least."},
            {"Swear Oath", "You swear to respect this world, whatever happens.", "40", "An oath... I will hold you to that."},
            {"Duel Pose", "You strike a dramatic duelling pose.", "20", "Ha! A worthy stance!"}
      },
      {
            {"Reassure", "You tell the guardian everything will be okay.", "35", "You really think so? ...Thank you."},
            {"Share Snack", "You offer the guardian a snack. It nibbles politely.", "30", "Oh! That's so sweet of you."},
            {"Hum", "You hum a soft tune.", "25", "That's lovely... I almost forgot we were fighting."}
      },
      {
            {"Calm Down", "You tell the guardian to breathe. It tries its best.", "35", "In... out... okay. Okay. I'm okay."},
            {"Compliment", "You tell the guardian it's doing great.", "30", "R-really? Me? You mean it?!"},
            {"Boo!", "You jump out and yell BOO!", "15", "AAAH! ...Please don't do that again."}
      },
      {
            {"Applaud", "You applaud the guardian's every move.", "35", "Finally, someone who appreciates talent!"},
            {"Autograph", "You ask the guardian for an autograph.", "30", "Of course! Here, I signed it twice!"},
            {"Yawn", "You let out a long, bored yawn.", "20", "B-bored?! Of ME?! ...Watch THIS then!"}
      },
      {
            {"High Five", "You give the guardian a high five.", "35", "Yeah!! High five!!"},
            {"Dance", "You do a little dance. The guardian joins in.", "30", "Wheee! Dance party!"},
            {"Tell Joke", "You tell a joke. The guardian laughs way too hard.", "25", "HAHAHA! Oh, that's a good one!"}
      },
      {
            {"Sit With", "You sit down quietly next to the guardian.", "35", "...You don't have to stay. But... thanks."},
            {"Cheer Up", "You try to cheer the guardian up.", "25", "...That almost worked."},
            {"Listen", "You listen to the guardian's worries.", "30", "No one's ever listened before."}
      },
      {
            {"Encore", "You shout for an encore!", "35", "An encore?! For ME?! Oh, my heart!"},
            {"Throw Rose", "You toss an imaginary rose at the guardian's feet.", "30", "A rose! I shall treasure it forever!"},
            {"Critique", "You offer some constructive criticism.", "20", "Criticism?! ...Hm. You may have a point."}
      },
      {
            {"Salute", "You salute. The guardian salutes back.", "35", "Good form."},
            {"Stand Firm", "You stand perfectly still, unflinching.", "30", "Discipline. I respect that."},
            {"Report", "You give the guardian a short report on the world outside.", "25", "...Understood. Thank you."}
      }
   };
   private static final Map<BattleState, DarknerBosses.Talk> TALKS = new WeakHashMap<>();

   private DarknerBosses() {
   }

   static void register() {
      TYPE = (EntityType<DarknerBossEntity>)Registry.register(
         Registries.ENTITY_TYPE,
         Identifier.of("turnbattle", "darkner_boss"),
         Builder.create(DarknerBossEntity::new, SpawnGroup.MISC).dimensions(0.8F, 2.5F).eyeHeight(2.2F).maxTrackingRange(10).build()
      );
      FabricDefaultAttributeRegistry.register(TYPE, DarknerBossEntity.createAttributes());
      Net121.registerS2CChannel(BOSS_TALK);
      Net121.registerC2SChannel(BOSS_TALK_REQ);
      Net121.registerC2SChannel(BOSS_CHALLENGE);
      Net121.registerServerReceiver(BOSS_TALK_REQ, (server, player, handler, buf, sender) -> {
         int id = buf.readVarInt();
         server.execute(() -> {
            if (player.getWorld().getEntityById(id) instanceof DarknerBossEntity b && b.squaredDistanceTo(player) < 64.0) {
               talk(player, b);
            }
         });
      });
      Net121.registerServerReceiver(
         BOSS_CHALLENGE,
         (server, player, handler, buf, sender) -> {
            int id = buf.readVarInt();
            server.execute(
               () -> {
                  if (player.getWorld().getEntityById(id) instanceof DarknerBossEntity b
                     && b.isAlive()
                     && b.squaredDistanceTo(player) < 100.0
                     && !BattleManager.isInBattle(player.getUuid())) {
                     BattleManager.tryStart(player, b);
                  }
               }
            );
         }
      );

      for (int p = 0; p < 8; p++) {
         ActDefs.register("darkner_boss_p" + p, actsFor(p));
      }
   }

   static String fullName(DarknerBossEntity b) {
      return Darkners.nameFor(b.item(), b.seed()) + " " + TITLES[Math.floorMod(b.seed() >>> 3, TITLES.length)];
   }

   static String actKey(DarknerBossEntity b) {
      return "turnbattle:darkner_boss_p" + b.personality();
   }

   private static List<ActDefs.ActEntry> actsFor(int p) {
      List out = new ArrayList<>();
      out.add(ActDefs.CHECK);

      for (String[] a : ACTS[p]) {
         out.add(new ActDefs.ActEntry(a[0], a[0], 1, Integer.parseInt(a[2]), a[1]));
      }

      return out;
   }

   static void recordCreator(DarkFountainData.DarkWorldInstance inst, BattleData.PlayerProgress prog) {
      inst.creatorNameLen = prog.vesselName != null && !prog.vesselName.isBlank() ? prog.vesselName.trim().length() : -1;
      ItemStack w = prog.equippedWeapon;
      if (w != null && !w.isEmpty()) {
         inst.creatorWeapon = Registries.ITEM.getId(w.getItem()).toString();
         WeaponStats ws = WeaponStats.fromStack(w);
         inst.creatorWeaponMagic = ws != null && ws.magic;
      } else {
         inst.creatorWeapon = "";
         inst.creatorWeaponMagic = false;
      }

      inst.creatorArmor.clear();
      if (prog.equippedArmor != null) {
         for (ItemStack a : prog.equippedArmor) {
            if (a != null && !a.isEmpty()) {
               inst.creatorArmor.add(Registries.ITEM.getId(a.getItem()).toString());
            }
         }
      }
   }

   private static String itemName(String id) {
      Identifier i = Identifier.tryParse(id);
      Item it = i == null ? Items.AIR : (Item)Registries.ITEM.get(i);
      return it == Items.AIR ? "something" : it.getName().getString();
   }

   private static String a(String noun) {
      return ("AEIOUaeiou".indexOf(noun.isEmpty() ? 120 : noun.charAt(0)) >= 0 ? "an " : "a ") + noun;
   }

   private static String makerHintsA(DarkFountainData.DarkWorldInstance inst) {
      String name = inst.creatorNameLen < 0
         ? "I never caught their name."
         : (
            inst.creatorNameLen <= 4
               ? "Their name was a short one, just a few letters."
               : (inst.creatorNameLen <= 7 ? "Their name wasn't long or short." : "Their name was a long one.")
         );
      String weapon = inst.creatorWeapon.isEmpty()
         ? "They carried no weapon at all."
         : "They carried " + a(itemName(inst.creatorWeapon)) + (inst.creatorWeaponMagic ? ", humming with magic." : ", a weapon built for hitting things.");
      return name + " " + weapon;
   }

   private static String makerHintsB(DarkFountainData.DarkWorldInstance inst) {
      if (inst.creatorArmor.isEmpty()) {
         return "They wore no armor.";
      } else {
         return inst.creatorArmor.size() == 1
            ? "They wore " + itemName(inst.creatorArmor.get(0)) + "."
            : "They wore " + itemName(inst.creatorArmor.get(0)) + " and " + itemName(inst.creatorArmor.get(1)) + ".";
      }
   }

   private static String[] makerLines(DarknerBossEntity b, DarkFountainData.DarkWorldInstance inst, boolean makerHere) {
      int p = b.personality();
      return makerHere && inst != null
         ? new String[]{String.format(MAKER_PRESENT[p][0], makerHintsA(inst)), String.format(MAKER_PRESENT[p][1], makerHintsB(inst))}
         : (String[])MAKER_ABSENT[p].clone();
   }

   static void talk(ServerPlayerEntity player, DarknerBossEntity b) {
      if (!BattleManager.isInBattle(player.getUuid())) {
         int p = b.personality();
         List pages = new ArrayList<>();
         pages.add(String.format(GREET[p], fullName(b)));
         pages.add(PROMPT[p]);
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeVarInt(b.getId());
         buf.writeVarInt(pages.size());

         for (String s : pages) {
            buf.writeString(s);
         }

         Net121.sendToClient(player, BOSS_TALK, buf);
      }
   }

   static String battleLine(BattleState state, ServerWorld world) {
      if (state != null && world != null && !state.isPvp) {
         if (!(world.getEntityById(state.primaryEnemyId) instanceof DarknerBossEntity b)) {
            return "";
         } else {
            int var11 = b.getId();
            if (state.enemyHpMap.getOrDefault(var11, 1.0F) <= 0.0F) {
               return "";
            } else {
               DarknerBosses.Talk t = TALKS.computeIfAbsent(state, k -> new DarknerBosses.Talk());
               int p = b.personality();
               if (t.reaction != null) {
                  String r = t.reaction;
                  t.reaction = null;
                  return r;
               } else if (!t.mercySaid && state.isMercyEligibleById(var11)) {
                  t.mercySaid = true;
                  return MERCY_READY[p];
               } else {
                  float max = state.enemyMaxHpMap.getOrDefault(var11, 1.0F);
                  if (!t.lowSaid && max > 0.0F && state.enemyHpMap.getOrDefault(var11, max) / max < 0.35F) {
                     t.lowSaid = true;
                     return LOW_HP[p];
                  } else {
                     DarkFountainData.DarkWorldInstance inst = instanceOf(world);
                     boolean makerHere = inst != null && inst.creatorUuid != null && state.participantOrder.contains(inst.creatorUuid);
                     String[] maker = makerLines(b, inst, makerHere);
                     String[] script = new String[]{
                        BATTLE_OPEN[p],
                        String.format(DREAM_LINE[p], DREAMS[b.dream()]),
                        String.format(REASON_LINE[p], REASONS[b.reason()]),
                        maker[0],
                        maker[1],
                        FILLER[p][0],
                        FILLER[p][1]
                     };
                     return t.idx < script.length ? script[t.idx++] : "";
                  }
               }
            }
         }
      } else {
         return "";
      }
   }

   static void onAct(BattleState state, Entity target, String actName) {
      if (target instanceof DarknerBossEntity b) {
         for (String[] a : ACTS[b.personality()]) {
            if (a[0].equals(actName)) {
               TALKS.computeIfAbsent(state, k -> new DarknerBosses.Talk()).reaction = a[3];
               return;
            }
         }
      }
   }

   static int[] patternsFor(DarknerBossEntity b) {
      int[] all = IntStream.range(420, 440).filter(id -> id != 433).toArray();
      Random r = new Random((long)b.seed() * 977L + 13L);

      for (int i = all.length - 1; i > 0; i--) {
         int j = r.nextInt(i + 1);
         int t = all[i];
         all[i] = all[j];
         all[j] = t;
      }

      int[] out = new int[6];
      int tier = b.tier();

      for (int i = 0; i < out.length; i++) {
         out[i] = all[i] + 1000 * tier;
      }

      return out;
   }

   static DarkFountainData.DarkWorldInstance instanceOf(ServerWorld w) {
      return !DarkFountainManager.isDarkWorld(w.getRegistryKey()) ? null : DarkFountainData.get(w.getServer()).getInstanceByWorldKey(w.getRegistryKey());
   }

   private static boolean choose(ServerWorld w, DarkFountainData.DarkWorldInstance inst) {
      if (inst.bossState == 1 && !Darkners.isBlock(inst.bossItem)) {
         if (inst.bossUuid != null && w.getEntity(inst.bossUuid) instanceof DarknerBossEntity old) {
            if (BattleManager.frozenEntityIds.contains(old.getId())) {
               return true;
            }

            old.discard();
         }

         inst.bossState = 0;
         inst.bossUuid = null;
         DarkFountainData.get(w.getServer()).markDirty();
      }

      if (inst.bossState != 0) {
         return inst.bossState == 1;
      } else {
         List items = Darkners.itemsFor(w);
         if (items.isEmpty()) {
            return false;
         } else {
            inst.bossItem = items.get(RNG.nextInt(items.size()));
            inst.bossSeed = RNG.nextInt();
            int roll = RNG.nextInt(100);
            inst.bossTier = roll < 50 ? 1 : (roll < 85 ? 2 : 3);
            inst.bossState = 1;
            DarkFountainData.get(w.getServer()).markDirty();
            return true;
         }
      }
   }

   public static void tick(MinecraftServer server) {
      if (server.getTicks() % 40 == 17) {
         for (ServerWorld w : server.getWorlds()) {
            if (DarkFountainManager.isDarkWorld(w.getRegistryKey()) && !w.getPlayers().isEmpty()) {
               try {
                  tickWorld(w);
               } catch (Exception var4) {
                  TurnBattleMod.LOGGER.warn("[DarknerBosses] couldn't look after {}'s guardian", w.getRegistryKey().getValue(), var4);
               }
            }
         }
      }
   }

   private static void tickWorld(ServerWorld w) {
      DarkFountainData.DarkWorldInstance inst = instanceOf(w);
      if (inst != null && choose(w, inst)) {
         BlockPos spot = guardSpot(w, inst);
         boolean near = false;

         for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.getBlockPos().getSquaredDistance(spot) < 16384.0) {
               near = true;
               break;
            }
         }

         if (near && w.shouldTickEntity(spot)) {
            if (inst.bossUuid == null || !(w.getEntity(inst.bossUuid) instanceof DarknerBossEntity)) {
               List there = w.getEntitiesByClass(DarknerBossEntity.class, new Box(spot).expand(48.0), e -> true);
               if (!there.isEmpty()) {
                  inst.bossUuid = there.get(0).getUuid();
                  DarkFountainData.get(w.getServer()).markDirty();
               } else {
                  Identifier id = Identifier.tryParse(inst.bossItem);
                  Item item = id == null ? Items.AIR : (Item)Registries.ITEM.get(id);
                  if (item != Items.AIR) {
                     DarknerBossEntity b = (DarknerBossEntity)TYPE.create(w);
                     if (b != null) {
                        float yaw = (float)Math.toDegrees(Math.atan2((double)(spot.getZ() - inst.fountainZ), (double)(spot.getX() - inst.fountainX))) - 90.0F;
                        b.refreshPositionAndAngles((double)spot.getX() + 0.5, (double)spot.getY(), (double)spot.getZ() + 0.5, yaw, 0.0F);
                        b.setHeadYaw(yaw);
                        b.setBodyYaw(yaw);
                        b.initialize(w, w.getLocalDifficulty(spot), SpawnReason.EVENT, null);
                        b.setMakeup(new ItemStack(item), inst.bossSeed);
                        b.setTier(inst.bossTier);
                        if (w.spawnEntity(b)) {
                           inst.bossUuid = b.getUuid();
                           DarkFountainData.get(w.getServer()).markDirty();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static BlockPos guardSpot(ServerWorld w, DarkFountainData.DarkWorldInstance inst) {
      double dx = (double)(50 - inst.fountainX);
      double dz = (double)(-inst.fountainZ);
      double l = Math.sqrt(dx * dx + dz * dz);
      if (l < 1.0) {
         dx = 1.0;
         dz = 0.0;
         l = 1.0;
      }

      int x = (int)Math.floor((double)inst.fountainX + dx / l * 9.0);
      int z = (int)Math.floor((double)inst.fountainZ + dz / l * 9.0);
      int y = w.isChunkLoaded(x >> 4, z >> 4) ? w.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, x, z) : 64;
      return new BlockPos(x, y, z);
   }

   static void onLoad(DarknerBossEntity b, ServerWorld w) {
      DarkFountainData.DarkWorldInstance inst = instanceOf(w);
      if (inst == null || inst.bossState != 2 && (inst.bossUuid == null || inst.bossUuid.equals(b.getUuid()))) {
         w.getServer().execute(() -> {
            if (!b.isRemoved()) {
               int level = b.bossLevel > 0 ? b.bossLevel : MobLevelManager.getMobLevel(b);
               MobLevelManager.restoreLevel(b, Math.max(1, level));
               EliteMobManager.makeElite(b, b.tier());
               b.bossLevel = MobLevelManager.getMobLevel(b);
               b.setHealth(b.getMaxHealth());
               b.setPersistent();
            }
         });
      } else {
         w.getServer().execute(b::discard);
      }
   }

   static void checkStillGuardian(ServerWorld w, DarknerBossEntity b) {
      DarkFountainData.DarkWorldInstance inst = instanceOf(w);
      if (inst != null) {
         if ((inst.bossState == 2 || inst.bossUuid != null && !inst.bossUuid.equals(b.getUuid())) && !BattleManager.frozenEntityIds.contains(b.getId())) {
            b.discard();
         }
      }
   }

   static boolean blocksSeal(ServerPlayerEntity player) {
      ServerWorld w = player.getServerWorld();
      DarkFountainData.DarkWorldInstance inst = instanceOf(w);
      if (inst == null || inst.bossState == 2) {
         return false;
      } else if (!choose(w, inst)) {
         return false;
      } else {
         String who = "its guardian";
         if (inst.bossUuid != null && w.getEntity(inst.bossUuid) instanceof DarknerBossEntity b) {
            who = fullName(b);
         }

         textbox(player, who + " stands guard over the Fountain. It can't be sealed while they do.");
         return true;
      }
   }

   static void textbox(ServerPlayerEntity p, String... pages) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(pages.length);

      for (String s : pages) {
         buf.writeString(s, 4096);
      }

      buf.writeBoolean(true);
      buf.writeBoolean(true);
      Net121.sendToClient(p, TurnBattleMod.TEXTBOX_SHOW, buf);
   }

   static void onBeaten(Entity e, ServerWorld w, boolean killed) {
      if (e instanceof DarknerBossEntity b) {
         DarkFountainData.DarkWorldInstance inst = instanceOf(w);
         if (inst != null && inst.bossState != 2) {
            finish(w, inst, b.personality(), b.item(), b.getX(), b.getY(), b.getZ(), killed);
         }
      }
   }

   static void onBattleWon(BattleState state, ServerWorld w, boolean killed) {
      if (state != null && w != null && !state.isPvp) {
         DarkFountainData.DarkWorldInstance inst = instanceOf(w);
         if (inst != null && inst.bossState == 1 && inst.bossUuid != null && inst.bossUuid.equals(state.entityUuid)) {
            if (w.getEntity(inst.bossUuid) instanceof DarknerBossEntity b) {
               onBeaten(b, w, killed);
            } else {
               ServerPlayerEntity near = w.getServer().getPlayerManager().getPlayer(state.playerUuid);
               double x = near != null ? near.getX() : (double)inst.fountainX;
               double y = near != null ? near.getY() : 64.0;
               double z = near != null ? near.getZ() : (double)inst.fountainZ;
               Identifier id = Identifier.tryParse(inst.bossItem);
               Item it = id == null ? Items.AIR : (Item)Registries.ITEM.get(id);
               finish(w, inst, DarknerBossEntity.personalityOf(inst.bossSeed), it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it), x, y, z, killed);
            }
         }
      }
   }

   private static void finish(
      ServerWorld w, DarkFountainData.DarkWorldInstance inst, int personality, ItemStack item, double x, double y, double z, boolean killed
   ) {
      inst.bossState = 2;
      DarkFountainData.get(w.getServer()).markDirty();
      String said = killed ? DEFEATED[personality] : SPARED[personality];

      for (ServerPlayerEntity p : w.getPlayers()) {
         if (!(p.squaredDistanceTo(x, y, z) > 4096.0)) {
            textbox(p, said, "(The Fountain is unguarded. It can be sealed now.)");
         }
      }

      if (!item.isEmpty()) {
         ItemEntity ie = new ItemEntity(w, x, y + 0.5, z, new ItemStack(item.getItem()));
         ie.setPickupDelay(10);
         w.spawnEntity(ie);
      }
   }

   private static final class Talk {
      int idx;
      String reaction;
      boolean lowSaid;
      boolean mercySaid;
   }
}
