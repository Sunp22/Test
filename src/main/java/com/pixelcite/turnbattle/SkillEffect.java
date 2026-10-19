package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

@Environment(EnvType.CLIENT)
public class SkillEffect {
   private SkillEffect.Phase phase = SkillEffect.Phase.STARTUP;
   private int tick = 0;
   private final int startupDuration;
   private final int hitDuration;
   private final SkillType type;
   private int cx;
   private int cy;
   public float scale = 1.0F;
   public float volume = 1.0F;
   private final SkillVfx.Spec spec;
   private boolean swingSoundPlayed = false;
   private boolean damageSoundPlayed = false;

   private SkillEffect(SkillType type, int cx, int cy, int startupTicks, int hitTicks, SkillVfx.Spec spec) {
      this.type = type;
      this.cx = cx;
      this.cy = cy;
      this.startupDuration = startupTicks;
      this.hitDuration = hitTicks;
      this.spec = spec;
   }

   public static SkillEffect forSkill(BattleSkill skill, int cx, int cy) {
      return forSkill(skill, cx, cy, 0);
   }

   public static SkillEffect forMobAttack(SkillType type, int cx, int cy, int colorOverride) {
      return new SkillEffect(type, cx, cy, SkillTiming.mobStartup(type), SkillTiming.hit(type), SkillVfx.specForMob(type, colorOverride));
   }

   public static SkillEffect forSkill(BattleSkill skill, int cx, int cy, int colorOverride) {
      return new SkillEffect(skill.getType(), cx, cy, SkillTiming.startup(skill), SkillTiming.hit(skill), SkillVfx.specFor(skill, colorOverride));
   }

   public void tick() {
      if (this.phase != SkillEffect.Phase.DONE) {
         this.tick++;
         if (!this.swingSoundPlayed) {
            this.swingSoundPlayed = true;
            this.playSnd(TurnBattleSounds.RUDEBUSTER_SWING, 1.0F);
         }

         if (this.phase == SkillEffect.Phase.STARTUP && this.tick >= this.startupDuration) {
            this.phase = SkillEffect.Phase.HIT;
            this.tick = 0;
            if (!this.damageSoundPlayed && this.isDamaging()) {
               this.damageSoundPlayed = true;
               this.playSnd(TurnBattleSounds.DAMAGE, 1.0F);
            }
         } else if (this.phase == SkillEffect.Phase.HIT && this.tick >= this.hitDuration) {
            this.phase = SkillEffect.Phase.DONE;
         }
      }
   }

   public boolean isDone() {
      return this.phase == SkillEffect.Phase.DONE;
   }

   public boolean isAtHitFrame() {
      return this.phase == SkillEffect.Phase.HIT && this.tick == 0;
   }

   public void setCenter(int x, int y) {
      this.cx = x;
      this.cy = y;
   }

   public void render(DrawContext ctx, float delta) {
      if (this.phase != SkillEffect.Phase.DONE) {
         if (this.scale != 1.0F) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate((float)this.cx, (float)this.cy, 0.0F);
            ctx.getMatrices().scale(this.scale, this.scale, 1.0F);
            ctx.getMatrices().translate((float)(-this.cx), (float)(-this.cy), 0.0F);

            try {
               this.renderAt(ctx, delta);
            } finally {
               ctx.getMatrices().pop();
            }
         } else {
            this.renderAt(ctx, delta);
         }
      }
   }

   private void renderAt(DrawContext ctx, float delta) {
      float t = (float)(this.phase == SkillEffect.Phase.STARTUP ? this.tick : this.startupDuration + this.tick) + delta;
      MinecraftClient mc = MinecraftClient.getInstance();
      float sw = (float)mc.getWindow().getScaledWidth();
      float sh = (float)mc.getWindow().getScaledHeight();
      SkillVfx.render(ctx, this.spec, (float)this.cx, (float)this.cy, t, this.startupDuration, this.hitDuration, sw, sh);
   }

   private void playSnd(SoundEvent snd, float pitch) {
      if (!(this.volume <= 0.01F)) {
         MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(snd, pitch, this.volume));
      }
   }

   private boolean isDamaging() {
      return switch (this.type) {
         case DAMAGE, BYPASS, FREEZE, BURN, MULTI, AOE -> true;
         default -> false;
      };
   }

   public static enum Phase {
      STARTUP,
      HIT,
      DONE;
   }
}
