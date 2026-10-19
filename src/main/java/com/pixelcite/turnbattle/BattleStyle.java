package com.pixelcite.turnbattle;

public enum BattleStyle {
   BULLET_HELL("Bullet Hell", "Evade bullets with your SOUL."),
   DIRECT_DAMAGE("Direct Damage", "Enemies use SKILLS to directly attack the player."),
   LIVE_COMBAT("Live Combat", "Fight in real time where you stand: lock on, strike, block and dash. No turns.");

   public final String displayName;
   public final String description;

   public boolean hasViewChoice() {
      return this != LIVE_COMBAT;
   }

   public boolean isLive() {
      return this == LIVE_COMBAT;
   }

   private BattleStyle(String displayName, String description) {
      this.displayName = displayName;
      this.description = description;
   }
}
