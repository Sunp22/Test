package com.pixelcite.turnbattle;

import net.minecraft.util.Identifier;

public enum SoulType {
   DETERMINATION("Determination", -65536, "redsoulsimple.png", "Last Stand: When falling to 3 HP or less, heal to full. (Once per life)"),
   PERSEVERANCE("Perseverance", -5635841, "purplesoulsimple.png", "Endurance: 50% chance to survive fatal hits at 3 HP or less, heal to 10 HP."),
   INTEGRITY("Integrity", -16750849, "bluesoulsimple.png", "Resolve: Take 15% less damage from all sources."),
   PATIENCE("Patience", -16720385, "cyansoulsimple.png", "Tranquility: Heal 5% of your max HP every time you guard."),
   JUSTICE("Justice", -8960, "yellowsoulsimple.png", "Retaliation: Deal 20% more damage when below 50% HP."),
   BRAVERY("Bravery", -30720, "orangesoulsimple.png", "Courage: Start battles with +25 TP."),
   KINDNESS("Kindness", -16711936, "greensoulsimple.png", "Compassion: Healing effects are 25% more effective."),
   ORI("Traitless", -1, "orisoulsimple.png", "No special passive. A soul stripped of its defining trait."),
   MONSTER("Monster Soul", -1, "monstersoulsimple.png", "Exclusive to Monsters. Absorb other soul traits for permanent stat bonuses.");

   private final String displayName;
   private final int color;
   private final String texturePath;
   private final String description;

   private SoulType(String displayName, int color, String texturePath, String description) {
      this.displayName = displayName;
      this.color = color;
      this.texturePath = texturePath;
      this.description = description;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public int getColor() {
      return this.color;
   }

   public String getDescription() {
      return this.description;
   }

   public Identifier getTexture() {
      return Identifier.of("turnbattle", "textures/souls/" + this.texturePath);
   }
}
