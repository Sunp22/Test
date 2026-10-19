package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Map;

public class InspectRegistry {
   private static final Map<String, InspectRegistry.Entry> BLOCKS = new HashMap<>();
   private static final Map<String, InspectRegistry.Entry> ENTITIES = new HashMap<>();
   private static final Map<String, InspectRegistry.Entry> BIOMES = new HashMap<>();
   private static final Map<String, InspectRegistry.Entry> SPECIAL = new HashMap<>();
   private static final InspectRegistry.Entry DEFAULT = new InspectRegistry.Entry("", new String[]{"* Nothing to see here."});

   private static void reg(Map<String, InspectRegistry.Entry> map, String key, String... lines) {
      map.put(key, new InspectRegistry.Entry("", lines));
   }

   private static void regS(Map<String, InspectRegistry.Entry> map, String key, String speaker, String... lines) {
      map.put(key, new InspectRegistry.Entry(speaker, lines));
   }

   public static InspectRegistry.Entry forBlock(String blockPath) {
      return BLOCKS.getOrDefault(blockPath, null);
   }

   public static InspectRegistry.Entry forEntity(String entityPath) {
      return ENTITIES.getOrDefault(entityPath, null);
   }

   public static InspectRegistry.Entry forBiome(String biomePath) {
      if (BIOMES.containsKey(biomePath)) {
         return BIOMES.get(biomePath);
      } else {
         for (Map.Entry e : BIOMES.entrySet()) {
            if (biomePath.contains(e.getKey())) {
               return e.getValue();
            }
         }

         return null;
      }
   }

   public static InspectRegistry.Entry forSpecial(String key) {
      return SPECIAL.getOrDefault(key, null);
   }

   public static InspectRegistry.Entry getDefault() {
      return DEFAULT;
   }

   static {
      reg(ENTITIES, "cow", "* You stare at the cow. The cow stares back.", "* Neither of you blink for an uncomfortable amount of time. You look away first.");
      reg(ENTITIES, "sheep", "* It's a sheep. Fluffy.", "* You resist the urge to touch it. You don't resist very long.");
      reg(ENTITIES, "pig", "* A pig. Surprisingly clean, all things considered.", "* You give it a little snort. It snorts back. Meaningful.");
      reg(
         ENTITIES,
         "chicken",
         "* The chicken regards you with one eye, then the other.",
         "* You're not sure what it's thinking. You're not sure it's thinking anything."
      );
      reg(
         ENTITIES,
         "cat",
         "* The cat looks at you with complete indifference.",
         "* You hold out your hand. It sniffs once and turns its head away. You feel judged."
      );
      reg(ENTITIES, "villager", "* They look like they want to say something important.", "* They do not.");
      reg(ENTITIES, "horse", "* A strong, noble creature. You rest a hand on its side.", "* It doesn't pull away. That feels like trust, even if it isn't.");
      reg(ENTITIES, "donkey", "* It looks tired. Honestly, relatable.");
      reg(ENTITIES, "rabbit", "* It freezes completely still, staring up at you.", "* You stay very still too. A moment passes. Then another.");
      reg(ENTITIES, "fox", "* The fox freezes, staring at you.", "* You stare back.");
      reg(ENTITIES, "bat", "* It hangs upside down, wings folded tight.", "* You wonder if it's aware of you at all.");
      reg(
         ENTITIES,
         "squid",
         "* It floats in place, propelling itself in slow, gentle pulses.",
         "* You get the sense it isn't going anywhere in particular. You respect that."
      );
      reg(
         ENTITIES,
         "glow_squid",
         "* It floats in place, propelling itself in slow, gentle pulses.",
         "* You get the sense it isn't going anywhere in particular. You respect that."
      );
      reg(ENTITIES, "axolotl", "* Oh. Oh it's adorable.", "* You consider abandoning all responsibilities and staying here forever.");
      reg(ENTITIES, "panda", "* It sits there, chewing on bamboo with complete focus.", "* You could learn something from that level of commitment.");
      reg(ENTITIES, "turtle", "* It moves very slowly.", "* That's okay. You're not in a rush either.");
      reg(ENTITIES, "snow_golem", "* The little snowman seems happy to exist.", "* You get the feeling it doesn't have long. You don't say anything.");
      reg(ENTITIES, "wolf", "* Dog. You pet it. It barks happily. :)");
      reg(
         ENTITIES,
         "iron_golem",
         "* The iron figure towers above you. You can't tell if its stare towards you is a kind one or a dangerous one. Intimidating..."
      );
      reg(BLOCKS, "dirt", "* Just dirt. Good, honest dirt.", "* Nothing remarkable about it. Sometimes that's enough.");
      reg(BLOCKS, "gravel", "* You kick it lightly. A few pieces scatter.", "* One of them reveals a flint underneath. A small thing. But noted.");
      reg(BLOCKS, "clay", "* The clay is soft and cool to the touch.");
      reg(
         BLOCKS,
         "sand",
         "* It shifts under your feet a little.",
         "* You think about how every grain used to be something else. Rock, maybe. Coral. A shoreline that doesn't exist anymore."
      );
      reg(
         BLOCKS,
         "red_sand",
         "* It shifts under your feet a little.",
         "* You think about how every grain used to be something else. Rock, maybe. Coral. A shoreline that doesn't exist anymore."
      );
      reg(BLOCKS, "cactus", "* You go to touch it and stop yourself just in time.", "* Maybe interacting with sharp pointy objects is a bad idea.");
      reg(BLOCKS, "dead_bush", "* It's a dead bush.", "* And yet it's still standing. There's something almost admirable about that.");
      reg(BLOCKS, "brown_mushroom", "* A mushroom. Big one.", "* You're not sure if it's edible. You decide you'd rather not find out the hard way.");
      reg(BLOCKS, "red_mushroom", "* A mushroom. Big one.", "* You're not sure if it's edible. You decide you'd rather not find out the hard way.");
      reg(BLOCKS, "hay_block", "* You fall backwards onto it.", "* It's softer than expected. You lie there for a moment. Nobody's watching.");
      reg(BLOCKS, "crafting_table", "* A crafting table. Nice and reliable. No need to question how you carry one in your pocket for most of your adventure.");
      reg(BLOCKS, "furnace", "* A furnace. You consider burning coal in it just for the warmth.");
      reg(
         BLOCKS,
         "chest",
         "* The sight of the chest makes you start wondering how so many of them got around the world. Whose loot is that? Who knows. Yours now."
      );
      reg(
         BLOCKS,
         "trapped_chest",
         "* The sight of the chest makes you start wondering how so many of them got around the world. Whose loot is that? Who knows. Yours now."
      );
      reg(BLOCKS, "white_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "orange_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "magenta_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(
         BLOCKS, "light_blue_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point."
      );
      reg(BLOCKS, "yellow_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "lime_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "pink_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "gray_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(
         BLOCKS, "light_gray_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point."
      );
      reg(BLOCKS, "cyan_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "purple_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "blue_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "brown_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "green_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "red_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(BLOCKS, "black_bed", "* You sit on the edge of it.", "* Wherever you are, there's a bed. That means someone made this place home, at some point.");
      reg(
         BLOCKS,
         "torch",
         "* A small flame, holding back the dark.",
         "* It could've been burning for years, or placed here moments ago. Either way, it's doing its job."
      );
      reg(
         BLOCKS,
         "wall_torch",
         "* A small flame, holding back the dark.",
         "* It could've been burning for years, or placed here moments ago. Either way, it's doing its job."
      );
      reg(BLOCKS, "soul_torch", "* A pale blue flame flickers quietly.", "* It's colder than a regular torch. Somehow that feels appropriate down here.");
      reg(BLOCKS, "soul_wall_torch", "* A pale blue flame flickers quietly.", "* It's colder than a regular torch. Somehow that feels appropriate down here.");
      reg(BLOCKS, "painting", "* You study it closely.", "* You're not sure what it's supposed to be. You're not sure the painter did either.");
      reg(BLOCKS, "beehive", "* The hive hums faintly.", "* You don't bother them. They don't bother you. Ideal.");
      reg(BLOCKS, "bee_nest", "* The hive hums faintly.", "* You don't bother them. They don't bother you. Ideal.");
      reg(BLOCKS, "pumpkin", "* Just sitting here in the field, completely unbothered.", "* You knock on it. You're not sure why. You do it again.");
      reg(BLOCKS, "carved_pumpkin", "* Just sitting here in the field, completely unbothered.", "* You knock on it. You're not sure why. You do it again.");
      reg(BLOCKS, "melon", "* You tap the side of it.", "* Perfectly ripe. You feel a small, simple happiness about this.");
      reg(BLOCKS, "moss_block", "* Soft and surprisingly damp.", "* It's been here a long time. You can tell.");
      reg(BLOCKS, "cobweb", "* Something lives here. Probably. You don't look too hard.");
      reg(
         BLOCKS,
         "ladder",
         "* You place your hand on the first rung and test to make sure it's sturdy. Yup. Doesn't seem like it'll break. Probably. Hopefully."
      );
      reg(BLOCKS, "lever", "* You look at it for a moment.", "* You don't know what it does. Best to leave it alone.");
      reg(BLOCKS, "note_block", "* You give it a tap.", "*plink.*", "* That was enough.");
      reg(
         BLOCKS,
         "bookshelf",
         "* There's a lot of books on the bookshelf. You pull one out and read a quick page.",
         "* ... You can't make out any of the text. You feel dejected as you put the book back into its rightful place on the shelf. Maybe some day, but not today."
      );
      reg(BLOCKS, "copper_ore", "* It's Copper Ore.", "* You wonder if it'll ever be mined.");
      reg(BLOCKS, "deepslate_copper_ore", "* It's Copper Ore.", "* You wonder if it'll ever be mined.");
      reg(BLOCKS, "iron_ore", "* It's Iron Ore. How ironic.", "* That joke didn't even land in your head. How lame.");
      reg(BLOCKS, "deepslate_iron_ore", "* It's Iron Ore. How ironic.", "* That joke didn't even land in your head. How lame.");
      reg(BLOCKS, "gold_ore", "* It's Gold Ore.", "* You wonder if somebody replaced all the gold for paper, since it's just as fragile.");
      reg(BLOCKS, "deepslate_gold_ore", "* It's Gold Ore.", "* You wonder if somebody replaced all the gold for paper, since it's just as fragile.");
      reg(
         BLOCKS,
         "diamond_ore",
         "* It's Diamond Ore. How pretty.",
         "* You pretend your party member has a physical form, so you can appreciate its beauty together."
      );
      reg(
         BLOCKS,
         "deepslate_diamond_ore",
         "* It's Diamond Ore. How pretty.",
         "* You pretend your party member has a physical form, so you can appreciate its beauty together."
      );
      reg(
         BLOCKS,
         "ancient_debris",
         "* You inspect the oddity embedded in the hot stone. Knocking on the side of it proves it to be sturdy.",
         "* You might be able to create ingots out of it."
      );
      reg(BLOCKS, "soul_sand", "* You could've sworn you heard a faint shriek come from the sand. It sends a chill down your spine.");
      reg(BLOCKS, "soul_soil", "* You could've sworn you heard a faint shriek come from the sand. It sends a chill down your spine.");
      reg(BLOCKS, "nether_bricks", "* The structure is sturdy. You wonder who could have built it.");
      reg(BLOCKS, "nether_brick_fence", "* The structure is sturdy. You wonder who could have built it.");
      reg(BLOCKS, "nether_brick_stairs", "* The structure is sturdy. You wonder who could have built it.");
      reg(BLOCKS, "nether_brick_slab", "* The structure is sturdy. You wonder who could have built it.");
      reg(
         SPECIAL,
         "deep_underground",
         "* You're very deep underground...",
         "* Somehow, it doesn't feel any lonelier than it is on the surface.",
         "* Unless you're playing Multiplayer, then this text just looks stupid."
      );
      reg(SPECIAL, "deep_dark", "* A shiver runs down your spine, followed by the feeling of impending doom.");
      reg(BIOMES, "village", "* You see townsfolk walking around. They speak in a language you don't understand. The language of hrrm.");
   }

   public static record Entry(String speaker, String[] lines) {
   }
}
