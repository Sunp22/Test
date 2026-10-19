package com.pixelcite.turnbattle;

public enum Element {
   FIRE(-48128, "Fire"),
   ICE(-12268289, "Ice"),
   NATURE(-12255420, "Nature"),
   DARK(-7846657, "Dark"),
   LIGHT(-188, "Light"),
   NONE(-1, "None");

   private final int color;
   private final String displayName;

   private Element(int color, String displayName) {
      this.color = color;
      this.displayName = displayName;
   }

   public int getColor() {
      return this.color;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public int getBulletColor() {
      return switch (this) {
         case FIRE -> -39373;
         case ICE -> -10035713;
         case NATURE -> -10027162;
         case DARK -> -5609729;
         case LIGHT -> -120;
         case NONE -> -1;
      };
   }
}
