package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActDefs {
   public static final ActDefs.ActEntry CHECK = new ActDefs.ActEntry("Check", "Check", 1, 0, "__CHECK__");
   public static final ActDefs.ActEntry SHINE = new ActDefs.ActEntry("Shine", "Shine", 1, 0, "__SHINE__");
   public static final List<ActDefs.ActEntry> DEFAULT = List.of(
      CHECK,
      new ActDefs.ActEntry("Talk", "Talk", 1, 20, "You talk to the enemy, trying to strike up conversation. They're not sure what to think."),
      ActDefs.ActEntry.confused("Plead", "Plead", 1, 10, "You plead with the enemy not to hurt you. They're not sure what to think.", 2),
      new ActDefs.ActEntry(
         "Convince (P2 + P3)", "Convince", 3, 55, "You and your allies strike up a genuine argument over why you should live. They ponder it."
      )
   );
   private static final Map<String, List<ActDefs.ActEntry>> MOB_ACTS = new LinkedHashMap<>();

   public static void register(String key, List<ActDefs.ActEntry> acts) {
      MOB_ACTS.put(key, new ArrayList<>(acts));
   }

   public static List<ActDefs.ActEntry> getActs(String typeKey) {
      if (typeKey == null) {
         return DEFAULT;
      } else {
         String key = typeKey.contains(":") ? typeKey.substring(typeKey.indexOf(58) + 1) : typeKey;
         return MOB_ACTS.getOrDefault(key, DEFAULT);
      }
   }

   static {
      List zombie = new ArrayList<>(
         List.of(
            CHECK,
            new ActDefs.ActEntry(
               "Mimic", "Mimic", 1, 25, "You raise your arms and make a groaning noise. The zombie is confused but feels weirdly appreciated."
            ),
            new ActDefs.ActEntry(
               "Thriller (P2 + P3)", "Thriller", 3, 80, "You and your allies dance in perfect unison. The zombie cannot help but be impressed."
            )
         )
      );

      for (String k : new String[]{"zombie", "drowned", "husk", "zombie_villager", "zombified_piglin", "zombie_horse"}) {
         MOB_ACTS.put(k, zombie);
      }

      List skeleton = new ArrayList<>(
         List.of(
            CHECK,
            new ActDefs.ActEntry("Practice", "Practice", 1, 35, "You make a bow firing motion with your hands. The skeleton admires your form."),
            new ActDefs.ActEntry(
               "Joke",
               "Joke",
               1,
               10,
               "You ask, \"What is a skeleton's favorite snack?\" The skeleton stares at you. Wordlessly. Because it can't talk back. Duh."
            ),
            new ActDefs.ActEntry(
               "Shake",
               "Shake",
               1,
               0,
               "You raise your arms and wiggle. The skeleton wonders how you can do that so perfectly with a skeleton inside of you.",
               3,
               false,
               "",
               0,
               0,
               0
            )
         )
      );

      for (String k : new String[]{"skeleton", "stray", "wither_skeleton", "bogged"}) {
         MOB_ACTS.put(k, skeleton);
      }

      MOB_ACTS.put(
         "creeper",
         new ArrayList<>(
            List.of(CHECK, new ActDefs.ActEntry("Hiss", "Hiss", 1, 0, "You hiss at the creeper. It hisses back... you're better off running, at this point."))
         )
      );
      MOB_ACTS.put(
         "enderman",
         new ArrayList<>(
            List.of(
               CHECK,
               ActDefs.ActEntry.withBuff("Stare", "Stare", 1, 0, "You make eye contact with the Enderman. It gets angry!", -10, 15),
               new ActDefs.ActEntry(
                  "Avoid", "Avoid", 1, 20, "You avoid making eye contact. It respects you for having basic decency in respecting its boundaries."
               ),
               new ActDefs.ActEntry(
                  "Gift (P2)",
                  "Gift",
                  2,
                  45,
                  "You and your ally give the Enderman a dirt block. It has never received a gift before. It could cry, if its tears wouldn't hurt it."
               )
            )
         )
      );
      MOB_ACTS.put(
         "pillager",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry("Discuss", "Discuss", 1, 20, "You talk about how much you want to pillage Villages. The pillager nods in agreement.")
            )
         )
      );
      MOB_ACTS.put(
         "vindicator",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry(
                  "Discuss",
                  "Discuss",
                  1,
                  0,
                  "You talk about how much you hate Villagers, but it just swings its axe at you. Guess talking isn't an option, here."
               )
            )
         )
      );
      List illager = new ArrayList<>(
         List.of(CHECK, new ActDefs.ActEntry("Discuss", "Discuss", 1, 20, "You talk about how much you hate Villagers. The Illager nods in agreement."))
      );

      for (String k : new String[]{"evoker", "ravager", "illusioner"}) {
         MOB_ACTS.put(k, illager);
      }

      MOB_ACTS.put(
         "warden",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry("Apologize", "Apologize", 1, 0, "You try to apologize to the Warden. It does not flinch."),
               ActDefs.ActEntry.sleepGated(
                  "Hide",
                  "Hide",
                  1,
                  7,
                  "You hide from the Warden. It loses sight of you for a moment.",
                  "You try to hide from the Warden. But it wasn't Tired enough to lose sight of you."
               ),
               ActDefs.ActEntry.sleepGated(
                  "Trick (P2)",
                  "Trick",
                  2,
                  18,
                  "You and your ally toss a rock to grab its attention. It spins around and roars, but finds nothing.",
                  "You and your ally toss a rock to grab its attention. Or, you try, but it wasn't Tired enough to fall for it."
               )
            )
         )
      );
      MOB_ACTS.put(
         "wither",
         new ArrayList<>(List.of(CHECK, ActDefs.ActEntry.withShield("Prepare", "Prepare", 1, 0, "You prepare yourself for the Wither's attacks.", 3)))
      );
      MOB_ACTS.put(
         "ender_dragon",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry(
                  "Calm",
                  "Calm",
                  1,
                  0,
                  "You try to calm her down. She hesitates for a moment. Maybe there was originally some kind of calm consciousness in her. If this dragon was sane once, she's not anymore. She will only attack."
               )
            )
         )
      );
      MOB_ACTS.put(
         "piglin",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry(
                  "Barter", "Barter", 1, 30, "You pick up some gold nuggets from nearby and show it to the Piglin. It hesitates, eyes sparking."
               ),
               new ActDefs.ActEntry(
                  "DoubleBarter (P2)",
                  "DoubleBarter",
                  2,
                  50,
                  "You and your ally scoop up some gold and bring it to the Piglin. Its eyes begin to shine with excitement."
               )
            )
         )
      );
      MOB_ACTS.put(
         "piglin_brute",
         new ArrayList<>(
            List.of(
               CHECK,
               ActDefs.ActEntry.sleepGated(
                  "Barter",
                  "Barter",
                  1,
                  35,
                  "You show the Piglin Brute some gold you found. It relaxes a bit, tired enough to forget about its job.",
                  "You show the Piglin Brute some gold you found. But it was not Tired enough to accept your offer."
               )
            )
         )
      );
      MOB_ACTS.put(
         "blaze",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry("FireUp", "FireUp", 1, 18, "You fire yourself up. The Blaze fires itself up too."),
               new ActDefs.ActEntry("CoolDown (P2 + P3)", "CoolDown", 3, 40, "You cool the Blaze down. It's nice to chill out sometimes.")
            )
         )
      );
      MOB_ACTS.put(
         "phantom",
         new ArrayList<>(
            List.of(CHECK, new ActDefs.ActEntry("SleepWalk", "SleepWalk", 1, 100, "You pretend to fall asleep where you stand. Good enough for the Phantom."))
         )
      );
      MOB_ACTS.put(
         "silverfish",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry(
                  "Wiggle", "Wiggle", 1, 100, "You wiggle your arms and legs to the rhythm of the silverfish. You enjoy each other's company."
               )
            )
         )
      );
      MOB_ACTS.put(
         "slime",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry("Slime", "Slime", 1, 100, "You put your SOUL inside the Slime. The slime really enjoys cleaning it up. You feel purified.")
            )
         )
      );
      MOB_ACTS.put(
         "magma_cube",
         new ArrayList<>(
            List.of(
               CHECK,
               new ActDefs.ActEntry(
                  "Slime", "Slime", 1, 15, "You put your SOUL inside the Slime. Ouch. Ouch OUCH you pull the SOUL back out. The Magma Cube feels bad."
               ),
               new ActDefs.ActEntry("Clean (P2)", "Clean", 2, 85, "You try to clean the molten material out of the Magma Cube. It feels relieved.")
            )
         )
      );
   }

   public static record ActEntry(
      String name,
      String logName,
      int participants,
      int mercyPct,
      String logText,
      int confuseTurns,
      boolean sleepCheck,
      String sleepLog,
      int buffAtk,
      int buffDef,
      int shieldTurns
   ) {
      public ActEntry(String name, String logName, int participants, int mercyPct, String logText) {
         this(name, logName, participants, mercyPct, logText, 0, false, "", 0, 0, 0);
      }

      public static ActDefs.ActEntry confused(String name, String logName, int participants, int mercyPct, String logText, int confuseTurns) {
         return new ActDefs.ActEntry(name, logName, participants, mercyPct, logText, confuseTurns, false, "", 0, 0, 0);
      }

      public static ActDefs.ActEntry sleepGated(String name, String logName, int participants, int mercyPct, String logText, String sleepLog) {
         return new ActDefs.ActEntry(name, logName, participants, mercyPct, logText, 0, true, sleepLog, 0, 0, 0);
      }

      public static ActDefs.ActEntry withBuff(String name, String logName, int participants, int mercyPct, String logText, int buffAtk, int buffDef) {
         return new ActDefs.ActEntry(name, logName, participants, mercyPct, logText, 0, false, "", buffAtk, buffDef, 0);
      }

      public static ActDefs.ActEntry withShield(String name, String logName, int participants, int mercyPct, String logText, int shieldTurns) {
         return new ActDefs.ActEntry(name, logName, participants, mercyPct, logText, 0, false, "", 0, 0, shieldTurns);
      }
   }
}
