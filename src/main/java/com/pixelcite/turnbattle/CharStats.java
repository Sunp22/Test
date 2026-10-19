package com.pixelcite.turnbattle;

import net.minecraft.nbt.NbtCompound;

public class CharStats {
   public int offense = 0;
   public int defense = 0;
   public int magic = 0;
   public int iq = 0;
   public int vitality = 0;
   public int luck = 0;
   public int unspent = 0;
   public int pointValue = 1;
   public static final int POINTS_PER_LEVEL = 3;
   public static final int MAX_OFFENSE = 50;
   public static final int MAX_DEFENSE = 50;
   public static final int MAX_MAGIC = 50;
   public static final int MAX_IQ = 20;
   public static final int MAX_VITALITY = 50;
   public static final int MAX_LUCK = 15;
   public int absorbed = 0;

   public static int maxFor(int statIndex) {
      return switch (statIndex) {
         case 0 -> 50;
         case 1 -> 50;
         case 2 -> 50;
         case 3 -> 20;
         case 4 -> 50;
         case 5 -> 15;
         default -> Integer.MAX_VALUE;
      };
   }

   public int valueFor(int statIndex) {
      return switch (statIndex) {
         case 0 -> this.offense;
         case 1 -> this.defense;
         case 2 -> this.magic;
         case 3 -> this.iq;
         case 4 -> this.vitality;
         case 5 -> this.luck;
         default -> 0;
      };
   }

   public int capFor(int statIndex) {
      int m = maxFor(statIndex);
      if (m == Integer.MAX_VALUE) {
         return m;
      } else {
         float mult = Math.max((float)Math.max(1, this.pointValue), 1.0F + (float)Math.min(7, Math.max(0, this.absorbed)) / 7.0F);
         return Math.round((float)m * mult);
      }
   }

   public boolean isMaxed(int statIndex) {
      return this.valueFor(statIndex) >= this.capFor(statIndex);
   }

   public boolean spendPoint(int statIndex) {
      if (this.unspent > 0 && statIndex >= 0 && statIndex <= 5) {
         int v = Math.max(1, this.pointValue);
         switch (statIndex) {
            case 0:
               this.offense += v;
               break;
            case 1:
               this.defense += v;
               break;
            case 2:
               this.magic += v;
               break;
            case 3:
               this.iq += v;
               break;
            case 4:
               this.vitality += v;
               break;
            default:
               this.luck += v;
         }

         this.unspent--;
         return true;
      } else {
         return false;
      }
   }

   private static float softCap(int points, int cap) {
      int over = Math.max(0, points - cap);
      return (float)Math.min(points, cap) + (float)over * 0.25F;
   }

   public float offenseBonus() {
      return softCap(this.offense, this.capFor(0)) * 0.18F;
   }

   public float defenseReduction() {
      return defenseFraction(softCap(this.defense, this.capFor(1)));
   }

   public static float defenseFraction(float points) {
      float d = Math.max(0.0F, points);
      return 0.8F * d / (d + 50.0F);
   }

   public float afterDefense(float hit) {
      return hit <= 0.0F ? 0.0F : hit * (1.0F - this.defenseReduction());
   }

   public float dodgeChance() {
      return Math.min(softCap(this.luck, this.capFor(5)) * 0.01F, 0.2F);
   }

   public float maxTp() {
      return 100.0F;
   }

   public float grazeTP() {
      return 1.0F + softCap(this.iq, this.capFor(3)) * 0.125F;
   }

   public float vitalityHp() {
      return softCap(this.vitality, this.capFor(4)) * 10.0F;
   }

   public float maxHp() {
      return 20.0F + this.vitalityHp();
   }

   public float critChance() {
      return Math.min(softCap(this.luck, this.capFor(5)) * 0.01F, 0.15F);
   }

   public float magicBonus() {
      return softCap(this.magic, this.capFor(2)) * 0.017F;
   }

   public int totalSpent() {
      return this.offense + this.defense + this.magic + this.iq + this.vitality + this.luck;
   }

   public NbtCompound toNbt() {
      NbtCompound t = new NbtCompound();
      t.putInt("offense", this.offense);
      t.putInt("defense", this.defense);
      t.putInt("speed", this.magic);
      t.putInt("iq", this.iq);
      t.putInt("vitality", this.vitality);
      t.putInt("luck", this.luck);
      t.putInt("unspent", this.unspent);
      if (this.pointValue != 1) {
         t.putInt("pointValue", this.pointValue);
      }

      if (this.absorbed != 0) {
         t.putInt("absorbed", this.absorbed);
      }

      return t;
   }

   public static CharStats fromNbt(NbtCompound t) {
      CharStats s = new CharStats();
      s.offense = t.getInt("offense");
      s.defense = t.getInt("defense");
      s.magic = t.getInt("speed");
      s.iq = t.getInt("iq");
      s.vitality = t.getInt("vitality");
      s.luck = t.getInt("luck");
      s.unspent = t.getInt("unspent");
      s.pointValue = t.contains("pointValue") ? Math.max(1, t.getInt("pointValue")) : 1;
      s.absorbed = t.getInt("absorbed");
      return s;
   }

   public void reset() {
      int pv = Math.max(1, this.pointValue);
      this.unspent = this.unspent + this.totalSpent() / pv;
      this.offense = this.defense = this.magic = this.iq = this.vitality = this.luck = 0;
   }

   public CharStats copy() {
      CharStats c = new CharStats();
      c.offense = this.offense;
      c.defense = this.defense;
      c.magic = this.magic;
      c.iq = this.iq;
      c.vitality = this.vitality;
      c.luck = this.luck;
      c.unspent = this.unspent;
      c.pointValue = this.pointValue;
      c.absorbed = this.absorbed;
      return c;
   }
}
