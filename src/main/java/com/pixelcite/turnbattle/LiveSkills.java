package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class LiveSkills {
   private LiveSkills() {
   }

   public static int encode(BattleSkill s) {
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

   public static BattleSkill decode(int code) {
      try {
         if (code >= 5000 && code < 6000) {
            return OriSkill.values()[code - 5000];
         }

         if (code >= 4000) {
            return code < 5000 ? SoulMageSkill.values()[code - 4000] : null;
         }

         if (code >= 3000) {
            return LightnerSkill.values()[code - 3000];
         }

         if (code >= 2000) {
            return MonsterSkill.values()[code - 2000];
         }

         if (code >= 1000) {
            return HumanMageSkill.values()[code - 1000];
         }

         if (code >= 0) {
            return PsiSkill.values()[code];
         }
      } catch (ArrayIndexOutOfBoundsException var2) {
      }

      return null;
   }

   public static String nameOf(BattleSkill s) {
      return s instanceof Enum<?> e ? e.name() : "";
   }

   public static List<BattleSkill> available(CharacterRace race, Collection<String> unlocked, Collection<String> unequipped) {
      List out = new ArrayList<>();

      for (PsiSkill s : PsiSkill.values()) {
         add(out, s, unlocked, unequipped);
      }

      for (LightnerSkill s : LightnerSkill.values()) {
         add(out, s, unlocked, unequipped);
      }

      if (race == CharacterRace.MONSTER) {
         for (MonsterSkill s : MonsterSkill.values()) {
            add(out, s, unlocked, unequipped);
         }
      } else {
         for (HumanMageSkill s : HumanMageSkill.values()) {
            add(out, s, unlocked, unequipped);
         }

         for (SoulMageSkill s : SoulMageSkill.values()) {
            if (s.getSoul() != null) {
               add(out, s, unlocked, unequipped);
            }
         }
      }

      return out;
   }

   public static List<BattleSkill> fromCards(Collection<String> cards) {
      List out = new ArrayList<>();

      for (String c : cards) {
         BattleSkill s = RogueCards.skillFor(c);
         if (s != null && !out.contains(s)) {
            out.add(s);
         }
      }

      return out;
   }

   public static List<BattleSkill> forPlayer(MinecraftServer server, UUID uuid, BattleData.PlayerProgress prog) {
      RogueliteData.PlayerRogueData rd = RogueliteData.get(server).players.get(uuid);
      return rd != null && rd.inRun ? fromCards(rd.drawnSkills) : available(prog.playerRace, prog.unlockedSkills, prog.unequippedSkills);
   }

   private static void add(List<BattleSkill> out, BattleSkill s, Collection<String> unlocked, Collection<String> unequipped) {
      String n = nameOf(s);
      if (unlocked.contains(n) && (unequipped == null || !unequipped.contains(n))) {
         out.add(s);
      }
   }

   public static boolean isSupport(BattleSkill s) {
      return switch (s.getType()) {
         case HEAL, SHIELD, BUFF -> true;
         default -> false;
      };
   }
}
