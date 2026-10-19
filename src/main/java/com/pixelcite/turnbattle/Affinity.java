package com.pixelcite.turnbattle;

import java.util.EnumSet;
import java.util.Set;

public enum Affinity {
   FIRE("Fire", -48128),
   ELECTRIC("Electric", -4608),
   ICE("Ice", -7803137),
   PSI("PSI", -3390209),
   SLASH("Slash", -3355444),
   LIGHT("Light", -86),
   DARK("Dark", -14540220),
   IMPACT("Impact", -30652);

   public final String displayName;
   public final int color;

   private Affinity(String displayName, int color) {
      this.displayName = displayName;
      this.color = color;
   }

   public Affinity weakTo() {
      return switch (this) {
         case FIRE -> PSI;
         case ELECTRIC -> ICE;
         case ICE -> FIRE;
         case PSI -> DARK;
         case SLASH -> ELECTRIC;
         case LIGHT -> IMPACT;
         case DARK -> LIGHT;
         case IMPACT -> SLASH;
      };
   }

   public static Set<Affinity> weaknessesForWeapon(Affinity[] held) {
      if (held != null && held.length != 0) {
         Set heldSet = EnumSet.noneOf(Affinity.class);

         for (Affinity a : held) {
            if (a != null) {
               heldSet.add(a);
            }
         }

         if (heldSet.isEmpty()) {
            return EnumSet.of(PSI);
         } else {
            Set weaknesses = EnumSet.noneOf(Affinity.class);

            for (Affinity ax : heldSet) {
               weaknesses.add(ax.weakTo());
            }

            weaknesses.removeAll(heldSet);
            return weaknesses;
         }
      } else {
         return EnumSet.of(PSI);
      }
   }
}
