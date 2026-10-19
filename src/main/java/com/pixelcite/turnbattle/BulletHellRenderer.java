package com.pixelcite.turnbattle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

final class BulletHellRenderer {
   private final BattleScreen screen;
   final GimmickModes modes;
   final WebShooterModes webShooter;
   private static final Identifier BULLET = Identifier.of("turnbattle", "textures/gui/bullet.png");
   private static final float GRAZE_R = 14.4F;
   private static final long PEER_SOUL_INTERVAL_NANOS = 100000000L;
   private static final int SLASH_THICK = 16;
   private static final int WARN_TICKS = 20;
   private int bulletTick;
   private int bulletDuration = 210;
   private static final float TIME_POINTS = 3.0F;
   private static final int HOLD_TP_TICKS = 15;
   private float timeCut;
   private int holdTicks;
   private float holdTp;
   private boolean grazeHeld;
   private float holdAlpha;
   private float holdAlphaPrev;
   private int gimmickLineIndex = 1;
   private int soulsHit = 0;
   private int grazeCount = 0;
   private boolean prevKeyW;
   private float prevSoulX;
   private float prevSoulY;
   private int soulPosSendCooldown = 0;
   private boolean lastSoulMoving;
   private static final int FEATHER_READY_TICKS = 10;
   private int featherReadyTick = -1;

   BulletHellRenderer(BattleScreen screen) {
      this.screen = screen;
      this.modes = new GimmickModes(this, screen);
      this.webShooter = new WebShooterModes(this, screen);
   }

   void hitSoul() {
      this.screen.iframeTicks = 22;
      this.soulsHit++;
      if (this.screen.partyMemberLabel == null) {
         this.screen.setVesselAnim(this.screen.localParticipantIndex(), 6);
      }

      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeFloat(this.screen.enemyDmgPerHit);
      Net121.sendToServer(TurnBattleMod.SOUL_HIT, buf);
   }

   void awardGraze(boolean playSound) {
      this.grazeCount++;
      int baseIQ = TurnBattleClientMod.clientPlayerStats.iq;
      int weaponIQBonus = 0;
      if (TurnBattleClientMod.clientEquippedWeapon != null && !TurnBattleClientMod.clientEquippedWeapon.isEmpty()) {
         WeaponStats ws = WeaponStats.fromStack(TurnBattleClientMod.clientEquippedWeapon);
         if (ws != null) {
            weaponIQBonus = ws.netStatBonus(3);
         }
      }

      if (TurnBattleClientMod.clientEquippedArmor != null) {
         for (ItemStack piece : TurnBattleClientMod.clientEquippedArmor) {
            if (piece != null && !piece.isEmpty()) {
               ArmorStats as = ArmorStats.fromStack(piece);
               if (as != null && as.bonusStat == 3) {
                  weaponIQBonus += as.bonusAmt;
               }
            }
         }
      }

      float grazeTP = 1.0F + (float)(baseIQ + weaponIQBonus) * 0.125F;
      if (!this.screen.darknessConstricting) {
         this.screen.playerTp = Math.min(this.screen.playerTpMax, this.screen.playerTp + grazeTP);
      }

      this.screen.grazeRingTick = 5;
      if (playSound) {
         MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TurnBattleSounds.GRAZE, 1.0F, 1.0F));
      }
   }

   private static float timePoints(Bullet b) {
      return b.kind == 0 ? 3.0F : 3.0F * Math.max(1.0F, Math.min(2.5F, MobPatterns.hitRadius(b) / 5.0F));
   }

   private void cutForGraze(Bullet b) {
      this.timeCut = this.timeCut + timePoints(b) * 0.6666667F;
   }

   private void cutForHold(Bullet b) {
      this.timeCut = this.timeCut + timePoints(b) / 30.0F;
   }

   void tickEnemy() {
      this.prevSoulX = this.screen.soulX;
      this.prevSoulY = this.screen.soulY;

      for (Bullet b : this.screen.bullets) {
         b.prevX = b.x;
         b.prevY = b.y;
      }

      for (Bullet b : this.screen.playerBullets) {
         b.prevX = b.x;
         b.prevY = b.y;
      }

      if (this.screen.drSpinTick < 10) {
         int cx = this.screen.getWidth() / 2;
         int colsBottom = this.screen.arenaBottom();
         int cy = (this.screen.arenaTop() + colsBottom) / 2 + 5;
         this.screen.soulX = (float)cx - 9.0F / 2.0F;
         this.screen.soulY = (float)cy - 9.0F / 2.0F;
      } else if (this.screen.currentBoxWidth == this.screen.targetBoxWidth && this.screen.currentBoxHeight == this.screen.targetBoxHeight) {
         if ((float)(++this.bulletTick) + this.timeCut >= (float)this.bulletDuration) {
            this.endEnemyTurn();
         } else {
            switch (this.screen.activeGimmick) {
               case ICE_PLATFORMER:
                  this.modes.tickBlueWorld(this.bulletTick);
                  this.modes.tickBlueSoul();
                  break;
               case NATURE_SHIELD:
                  this.modes.tickGreen(this.bulletTick);
                  break;
               case DARK_LINES:
                  this.tickDarkLines();
                  this.webShooter.tickPurple(this.bulletTick);
                  break;
               case LIGHT_SHOOTER:
                  this.tickLightShooter();
                  this.webShooter.tickYellow(this.bulletTick, this.screen.spacePressed);
                  break;
               default:
                  if (this.screen.featherDashIframes <= 0) {
                     if (this.screen.keyW) {
                        this.screen.soulY = this.screen.soulY - this.screen.effectiveSoulSpeed();
                     }

                     if (this.screen.keyS) {
                        this.screen.soulY = this.screen.soulY + this.screen.effectiveSoulSpeed();
                     }

                     if (this.screen.keyA) {
                        this.screen.soulX = this.screen.soulX - this.screen.effectiveSoulSpeed();
                     }

                     if (this.screen.keyD) {
                        this.screen.soulX = this.screen.soulX + this.screen.effectiveSoulSpeed();
                     }

                     this.screen.soulX = BattleScreen.clamp(
                        this.screen.soulX, (float)this.screen.boxX, (float)(this.screen.boxX + this.screen.targetBoxWidth - 9)
                     );
                     this.screen.soulY = BattleScreen.clamp(
                        this.screen.soulY, (float)this.screen.boxY, (float)(this.screen.boxY + this.screen.targetBoxHeight - 9)
                     );
                  }
            }

            this.prevKeyW = this.screen.keyW;
            if (this.screen.activeGimmick == BattleScreen.GimmickType.NONE) {
               int beforeCount = this.screen.bullets.size();
               BulletPatterns.spawn(
                  this.screen.patternIndex,
                  this.bulletTick,
                  this.screen.boxX,
                  this.screen.boxY,
                  this.screen.targetBoxWidth,
                  this.screen.targetBoxHeight,
                  this.screen.soulX,
                  this.screen.soulY,
                  this.screen.bullets,
                  this.screen.bulletSpeedMult
               );
               float INSIDE_MARGIN = 6.0F;

               for (int i = beforeCount; i < this.screen.bullets.size(); i++) {
                  Bullet b = this.screen.bullets.get(i);
                  if (!(b.size >= 990.0F) && !b.noWarn && b.pendingTicks <= 0) {
                     boolean inside = b.x > (float)this.screen.boxX + 6.0F
                        && b.x < (float)(this.screen.boxX + this.screen.targetBoxWidth) - 6.0F
                        && b.y > (float)this.screen.boxY + 6.0F
                        && b.y < (float)(this.screen.boxY + this.screen.targetBoxHeight) - 6.0F;
                     if (inside) {
                        b.pendingTicks = 20;
                        b.active = false;
                        b.prevX = b.x;
                        b.prevY = b.y;
                     }
                  }
               }
            }

            for (Bullet b : this.screen.bullets) {
               if (b.pendingTicks > 0) {
                  b.pendingTicks--;
                  if (b.pendingTicks == 0) {
                     b.active = true;
                     b.prevX = b.x;
                     b.prevY = b.y;
                  }
               }
            }

            boolean mobPattern = this.isMobPattern();
            float bscx = this.screen.soulX + 9.0F / 2.0F;
            float bscy = this.screen.soulY + 9.0F / 2.0F;
            List mobSpawned = mobPattern ? new ArrayList<>() : null;

            for (Bullet bx : this.screen.bullets) {
               if (bx.active) {
                  if (mobPattern && bx.kind != 0) {
                     MobPatterns.update(bx, bscx, bscy, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight, mobSpawned);
                     if (!bx.active) {
                        continue;
                     }
                  }

                  bx.vx = bx.vx + bx.ax;
                  bx.vy = bx.vy + bx.ay;
                  bx.x = bx.x + bx.vx;
                  bx.y = bx.y + bx.vy;
               }
            }

            if (mobPattern) {
               this.screen.bullets.addAll(mobSpawned);
               MobPatterns.tickHazards(this.bulletTick, bscx, bscy);
            }

            for (Bullet pb : this.screen.playerBullets) {
               if (pb.active) {
                  pb.x = pb.x + pb.vx;
                  pb.y = pb.y + pb.vy;
               }
            }

            for (Bullet bxx : this.screen.bullets) {
               if (bxx.active && bxx.size == 990.0F) {
                  this.screen.krisSlashes.add(new BattleScreen.KrisSlash((int)bxx.vx, bxx.vy));
                  bxx.active = false;
               }
            }

            this.screen.krisSlashes.forEach(s -> {
               int prev = s.tick++;
               if (s.tick >= 21) {
                  s.active = false;
               }

               if (prev < 15 && s.tick >= 15 && this.screen.getClient() != null && this.screen.getClient().player != null) {
                  this.screen.getClient().player.playSound((SoundEvent)Registries.SOUND_EVENT.get(Identifier.of("turnbattle", "slash")), 1.0F, 1.0F);
               }
            });
            this.screen.krisSlashes.removeIf(s -> !s.active);
            this.screen
               .bullets
               .removeIf(
                  bxxx -> !bxxx.active && bxxx.pendingTicks <= 0
                        || bxxx.x < (float)(this.screen.boxX - 30)
                        || bxxx.x > (float)(this.screen.boxX + 160 + 30)
                        || bxxx.y < (float)(this.screen.boxY - 30)
                        || bxxx.y > (float)(this.screen.boxY + 120 + 30)
               );
            this.screen
               .playerBullets
               .removeIf(
                  bxxx -> !bxxx.active
                        || bxxx.x < (float)(this.screen.boxX - 30)
                        || bxxx.x > (float)(this.screen.boxX + 160 + 30)
                        || bxxx.y < (float)(this.screen.boxY - 30)
                        || bxxx.y > (float)(this.screen.boxY + 120 + 30)
               );
            float scx = this.screen.soulX + 9.0F / 2.0F;
            float scy = this.screen.soulY + 9.0F / 2.0F;
            boolean soulMoving = Math.abs(this.screen.soulX - this.prevSoulX) + Math.abs(this.screen.soulY - this.prevSoulY) > 0.01F
               || this.screen.featherDashIframes > 0;
            this.lastSoulMoving = soulMoving;
            boolean held = false;
            Iterator var9 = this.screen.bullets.iterator();

            while (true) {
               Bullet bxxx;
               while (true) {
                  if (!var9.hasNext()) {
                     this.tickHold(held);
                     if (this.screen.activeGimmick == BattleScreen.GimmickType.ICE_PLATFORMER) {
                        this.modes.collideBlue();
                     }

                     if (mobPattern) {
                        if (this.screen.iframeTicks <= 0 && MobPatterns.hazardHit(scx, scy, soulMoving)) {
                           this.hitSoul();
                        } else if (MobPatterns.hazardGraze(scx, scy, 14.4F, soulMoving)) {
                           this.awardGraze(true);
                        }
                     }

                     if (this.screen.iframeTicks > 0) {
                        this.screen.iframeTicks--;
                     }

                     if (this.screen.featherDashCooldown > 0 && --this.screen.featherDashCooldown == 0 && this.screen.offhandGimmick == OffhandGimmick.FEATHER) {
                        this.featherReady();
                     }

                     if (this.featherReadyTick >= 0 && ++this.featherReadyTick > 10) {
                        this.featherReadyTick = -1;
                     }

                     if (this.screen.featherDashIframes > 0) {
                        this.screen.featherDashIframes--;
                        this.screen.soulX = BattleScreen.clamp(
                           this.screen.soulX + this.screen.featherDashVX, (float)this.screen.boxX, (float)(this.screen.boxX + this.screen.targetBoxWidth - 9)
                        );
                        this.screen.soulY = BattleScreen.clamp(
                           this.screen.soulY + this.screen.featherDashVY, (float)this.screen.boxY, (float)(this.screen.boxY + this.screen.targetBoxHeight - 9)
                        );
                        this.screen.featherDashVX *= 0.7F;
                        this.screen.featherDashVY *= 0.7F;
                     }

                     for (float[] ring : this.screen.shieldRings) {
                        ring[2] += 2.0F;
                        ring[3] -= 0.08F;
                     }

                     this.screen.shieldRings.removeIf(r -> r[3] <= 0.0F);
                     if (this.screen.getClient() != null && this.screen.getClient().getNetworkHandler() != null) {
                        boolean hasOtherRealPlayer = false;

                        for (int ix = 0; ix < this.screen.participants.size(); ix++) {
                           if (!this.screen.participants.get(ix).isMe()
                              && ix < this.screen.participantUuids.size()
                              && this.screen.participantUuids.get(ix) != null) {
                              hasOtherRealPlayer = true;
                              break;
                           }
                        }

                        if (hasOtherRealPlayer && --this.soulPosSendCooldown <= 0) {
                           this.soulPosSendCooldown = 2;
                           float relX = this.screen.targetBoxWidth > 0
                              ? (this.screen.soulX - (float)this.screen.boxX) / (float)this.screen.targetBoxWidth
                              : 0.5F;
                           float relY = this.screen.targetBoxHeight > 0
                              ? (this.screen.soulY - (float)this.screen.boxY) / (float)this.screen.targetBoxHeight
                              : 0.5F;
                           PacketByteBuf posBuf = PacketByteBufs.create();
                           posBuf.writeFloat(relX);
                           posBuf.writeFloat(relY);
                           Net121.sendToServer(TurnBattleMod.SOUL_POS_SYNC, posBuf);
                        }
                     }

                     return;
                  }

                  bxxx = (Bullet)var9.next();
                  if (bxxx.active) {
                     if (this.screen.activeGimmick != BattleScreen.GimmickType.LIGHT_SHOOTER) {
                        break;
                     }

                     for (Bullet pbx : this.screen.playerBullets) {
                        if (pbx.active) {
                           float dx = pbx.x - bxxx.x;
                           float dy = pbx.y - bxxx.y;
                           if (Math.sqrt((double)(dx * dx + dy * dy)) < (double)(pbx.size + bxxx.size)) {
                              bxxx.active = false;
                              pbx.active = false;
                              break;
                           }
                        }
                     }

                     if (bxxx.active) {
                        break;
                     }
                  }
               }

               float dx = bxxx.x - scx;
               float dy = bxxx.y - scy;
               if (!(bxxx.size >= 990.0F)) {
                  boolean destroyedByRing = false;

                  for (float[] ring : this.screen.shieldRings) {
                     float rdx = bxxx.x - ring[0];
                     float rdy = bxxx.y - ring[1];
                     if (Math.sqrt((double)(rdx * rdx + rdy * rdy)) <= (double)ring[2]) {
                        bxxx.active = false;
                        destroyedByRing = true;
                        break;
                     }
                  }

                  if (!destroyedByRing) {
                     if (bxxx.kind != 0) {
                        if (!MobPatterns.hueHarmless(bxxx.hue, soulMoving)) {
                           float hr = MobPatterns.hitRadius(bxxx) + 9.0F * 0.2F;
                           if (this.screen.iframeTicks <= 0 && dx * dx + dy * dy < hr * hr) {
                              if (!MobPatterns.persists(bxxx)) {
                                 bxxx.active = false;
                              }

                              this.hitSoul();
                           } else if (Math.sqrt((double)(dx * dx + dy * dy)) < (double)(14.4F + MobPatterns.hitRadius(bxxx) - 3.0F)) {
                              if (!bxxx.grazed) {
                                 bxxx.grazed = true;
                                 this.awardGraze(true);
                                 this.cutForGraze(bxxx);
                              } else {
                                 this.cutForHold(bxxx);
                              }

                              held = true;
                           }
                        }
                     } else {
                        if (this.screen.iframeTicks <= 0) {
                           float hitInset = 9.0F * 0.3F;
                           float hx1 = this.screen.soulX + hitInset;
                           float hy1 = this.screen.soulY + hitInset;
                           float hx2 = this.screen.soulX + 9.0F - hitInset;
                           float hy2 = this.screen.soulY + 9.0F - hitInset;
                           if (hx1 < bxxx.x + bxxx.size && hx2 > bxxx.x - bxxx.size && hy1 < bxxx.y + bxxx.size && hy2 > bxxx.y - bxxx.size) {
                              bxxx.active = false;
                              this.hitSoul();
                              continue;
                           }
                        }

                        if (Math.sqrt((double)(dx * dx + dy * dy)) < 14.4F) {
                           if (!bxxx.grazed) {
                              bxxx.grazed = true;
                              this.awardGraze(true);
                              this.cutForGraze(bxxx);
                           } else {
                              this.cutForHold(bxxx);
                           }

                           held = true;
                        }
                     }
                  }
               }
            }
         }
      } else {
         int centerX = this.screen.getWidth() / 2;
         int colsBottom = this.screen.arenaBottom();
         int centerY = (this.screen.arenaTop() + colsBottom) / 2 + 5;
         int boxDrawX = centerX - this.screen.currentBoxWidth / 2;
         int boxDrawY = centerY - this.screen.currentBoxHeight / 2;
         this.screen.boxX = boxDrawX;
         this.screen.boxY = boxDrawY;
         this.screen.soulX = (float)this.screen.boxX + (float)(this.screen.currentBoxWidth - 9) / 2.0F;
         this.screen.soulY = (float)this.screen.boxY + (float)(this.screen.currentBoxHeight - 9) / 2.0F;
      }
   }

   private void featherReady() {
      this.featherReadyTick = 0;
      SoundEvent snd = (SoundEvent)Registries.SOUND_EVENT.get(Identifier.of("minecraft", "block.note_block.bell"));
      if (snd != null) {
         MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(snd, 1.7F, 0.9F));
      }
   }

   private void renderFeatherReady(DrawContext ctx, float rSoulX, float rSoulY, Identifier soulTexture) {
      if (this.featherReadyTick >= 0) {
         float t = Math.min(1.0F, ((float)this.featherReadyTick + this.screen.lastRenderDelta) / 10.0F);
         float scale = 1.0F + 1.3F * (1.0F - (1.0F - t) * (1.0F - t));
         float alpha = 0.55F * (1.0F - t);
         if (!(alpha <= 0.01F)) {
            float cx = rSoulX + 9.0F / 2.0F;
            float cy = rSoulY + 9.0F / 2.0F;
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(cx, cy, 0.0F);
            m.scale(scale, scale, 1.0F);
            m.translate(-4.5F, -4.5F, 0.0F);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            if (soulTexture != null) {
               ctx.drawTexture(soulTexture, 0, 0, 0.0F, 0.0F, 9, 9, 9, 9);
            } else {
               ctx.drawTexture(BattleScreen.ICONS, 0, 0, 52, 0, 9, 9);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            m.pop();
         }
      }
   }

   private void tickHold(boolean held) {
      this.grazeHeld = held;
      this.holdAlphaPrev = this.holdAlpha;
      if (held) {
         this.holdAlpha = 1.0F;
         if (++this.holdTicks % 15 == 0) {
            this.holdTp++;
            if (!this.screen.darknessConstricting) {
               this.screen.playerTp = Math.min(this.screen.playerTpMax, this.screen.playerTp + 1.0F);
            }
         }
      } else {
         this.holdTicks = 0;
         this.holdAlpha = Math.max(0.0F, this.holdAlpha - 1.0F / 5.0F);
      }
   }

   void tickDarkLines() {
      if (this.screen.keyW && this.gimmickLineIndex > 0) {
         this.gimmickLineIndex--;
         this.screen.keyW = false;
      }

      if (this.screen.keyS && this.gimmickLineIndex < 2) {
         this.gimmickLineIndex++;
         this.screen.keyS = false;
      }

      this.screen.soulY = (float)(this.screen.boxY + this.gimmickLineIndex * this.screen.targetBoxHeight / 3 + this.screen.targetBoxHeight / 6 - 9 / 2);
      if (this.screen.keyA) {
         this.screen.soulX = this.screen.soulX - this.screen.effectiveSoulSpeed();
      }

      if (this.screen.keyD) {
         this.screen.soulX = this.screen.soulX + this.screen.effectiveSoulSpeed();
      }

      this.screen.soulX = BattleScreen.clamp(this.screen.soulX, (float)this.screen.boxX, (float)(this.screen.boxX + this.screen.targetBoxWidth - 9));
   }

   void tickLightShooter() {
      if (this.screen.keyW) {
         this.screen.soulY = this.screen.soulY - this.screen.effectiveSoulSpeed();
      }

      if (this.screen.keyS) {
         this.screen.soulY = this.screen.soulY + this.screen.effectiveSoulSpeed();
      }

      if (this.screen.keyA) {
         this.screen.soulX = this.screen.soulX - this.screen.effectiveSoulSpeed();
      }

      if (this.screen.keyD) {
         this.screen.soulX = this.screen.soulX + this.screen.effectiveSoulSpeed();
      }

      this.screen.soulX = BattleScreen.clamp(this.screen.soulX, (float)this.screen.boxX, (float)(this.screen.boxX + this.screen.targetBoxWidth - 9));
      this.screen.soulY = BattleScreen.clamp(this.screen.soulY, (float)this.screen.boxY, (float)(this.screen.boxY + this.screen.targetBoxHeight - 9));
      if (!this.screen.shootHeld()) {
         this.screen.spacePressed = false;
      }
   }

   void renderGrazeRing(DrawContext ctx, float rSoulX, float rSoulY) {
      float cx = rSoulX + 9.0F / 2.0F;
      float cy = rSoulY + 9.0F / 2.0F;
      boolean popping = this.screen.grazeRingTick > 0;
      float smoothTick = (float)this.screen.grazeRingTickPrev
         + (float)(this.screen.grazeRingTick - this.screen.grazeRingTickPrev) * this.screen.lastRenderDelta;
      float progress = popping ? 1.0F - smoothTick / 5.0F : 1.0F;
      float hold = this.holdAlphaPrev + (this.holdAlpha - this.holdAlphaPrev) * this.screen.lastRenderDelta;
      if (hold <= 0.01F) {
         if (popping) {
            this.screen.renderGrazeRingAt(ctx, cx, cy, progress, 0.8F);
         }
      } else {
         float scale = 1.0F + 0.25F * (this.grazeHeld ? progress : 1.0F);
         this.screen.renderGrazeRingScaled(ctx, cx, cy, scale, Math.max(hold, popping ? 1.0F - progress : 0.0F), 0.8F);
      }
   }

   void renderEnemyPhase(DrawContext ctx, float delta) {
      float rSoulX = this.prevSoulX + (this.screen.soulX - this.prevSoulX) * delta;
      float rSoulY = this.prevSoulY + (this.screen.soulY - this.prevSoulY) * delta;
      if (this.screen.activeGimmick == BattleScreen.GimmickType.DARK_LINES) {
         this.webShooter.renderPurple(ctx, delta);
      }

      if (this.screen.activeGimmick == BattleScreen.GimmickType.LIGHT_SHOOTER) {
         this.webShooter.renderYellow(ctx, delta);
      }

      if (this.screen.activeGimmick == BattleScreen.GimmickType.ICE_PLATFORMER) {
         this.modes.renderBlue(ctx, delta);
      }

      boolean mobPattern = this.isMobPattern();
      if (mobPattern) {
         MobPatternsRender.renderBack(
            ctx, delta, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight, this.screen.bullets, this.bulletTick
         );
      }

      int bulletColor = this.screen.battleElement.getBulletColor();
      float r = (float)(bulletColor >> 16 & 0xFF) / 255.0F;
      float g = (float)(bulletColor >> 8 & 0xFF) / 255.0F;
      float blu = (float)(bulletColor & 0xFF) / 255.0F;
      RenderSystem.setShaderColor(r, g, blu, 1.0F);

      for (Bullet bullet : this.screen.bullets) {
         if (bullet.active && bullet.kind == 0) {
            float rx = bullet.prevX + (bullet.x - bullet.prevX) * delta;
            float ry = bullet.prevY + (bullet.y - bullet.prevY) * delta;
            int bx2 = (int)(rx - bullet.size);
            int by2 = (int)(ry - bullet.size);
            int bs = (int)(bullet.size * 2.0F);
            ctx.drawTexture(BULLET, bx2, by2, bs, bs, 0.0F, 0.0F, 16, 16, 16, 16);
         }
      }

      if (this.screen.activeGimmick == BattleScreen.GimmickType.LIGHT_SHOOTER) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 0.0F, 1.0F);

         for (Bullet pb : this.screen.playerBullets) {
            if (pb.active) {
               float rx = pb.prevX + (pb.x - pb.prevX) * delta;
               float ry = pb.prevY + (pb.y - pb.prevY) * delta;
               int bx2 = (int)(rx - pb.size);
               int by2 = (int)(ry - pb.size);
               int bs = (int)(pb.size * 2.0F);
               ctx.drawTexture(BULLET, bx2, by2, bs, bs, 0.0F, 0.0F, 16, 16, 16, 16);
            }
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (mobPattern) {
         MobPatternsRender.renderBullets(
            ctx, delta, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight, this.screen.bullets, this.bulletTick
         );
         DarknerRenderer.renderBullets(
            ctx, delta, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight, this.screen.bullets
         );
         MobPatternsRender.renderDarkness(
            ctx, delta, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight, rSoulX + 9.0F / 2.0F, rSoulY + 9.0F / 2.0F
         );
         MobPatternsRender.renderHazards(
            ctx, delta, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight, this.bulletTick
         );
         this.renderHueHint(ctx);
      }

      if (!this.screen.krisSlashes.isEmpty()) {
         ctx.enableScissor(this.screen.boxX, this.screen.boxY, this.screen.boxX + this.screen.targetBoxWidth, this.screen.boxY + this.screen.targetBoxHeight);

         for (BattleScreen.KrisSlash ks : this.screen.krisSlashes) {
            this.renderKrisSlash(ctx, ks, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight);
            if (ks.tick >= 15
               && this.screen.iframeTicks <= 0
               && this.krisSlashHits(
                  ks, this.screen.soulX, this.screen.soulY, this.screen.boxX, this.screen.boxY, this.screen.targetBoxWidth, this.screen.targetBoxHeight
               )) {
               this.screen.iframeTicks = 22;
               this.soulsHit++;
               if (this.screen.partyMemberLabel == null) {
                  this.screen.setVesselAnim(this.screen.localParticipantIndex(), 6);
               }

               PacketByteBuf hbuf = PacketByteBufs.create();
               hbuf.writeFloat(this.screen.enemyDmgPerHit);
               Net121.sendToServer(TurnBattleMod.SOUL_HIT, hbuf);
            }
         }

         ctx.disableScissor();
      }

      if (this.screen.activeGimmick == BattleScreen.GimmickType.NATURE_SHIELD && !this.screen.soulLeaving) {
         this.modes.renderGreen(ctx, delta, rSoulX, rSoulY, this.bulletTick);
      }

      boolean showSoul = (this.screen.iframeTicks == 0 || this.screen.iframeTicks / 3 % 2 == 0) && !this.screen.soulLeaving;
      if (showSoul) {
         Identifier soulTexture = null;

         soulTexture = switch (this.screen.activeGimmick) {
            case ICE_PLATFORMER -> Identifier.of("turnbattle", "textures/souls/bluesoulsimple.png");
            case NATURE_SHIELD -> Identifier.of("turnbattle", "textures/souls/greensoulsimple.png");
            case DARK_LINES -> Identifier.of("turnbattle", "textures/souls/purplesoulsimple.png");
            case LIGHT_SHOOTER -> Identifier.of("turnbattle", "textures/souls/yellowsoulupsidedownsimple.png");
            default -> {
               if (this.screen.playerSoul != null) {
                  yield this.screen.playerSoul.getTexture();
               }
            }
         };

         if (soulTexture != null) {
            ctx.drawTexture(soulTexture, (int)rSoulX, (int)rSoulY, 0.0F, 0.0F, 9, 9, 9, 9);
         } else {
            ctx.drawTexture(BattleScreen.ICONS, (int)rSoulX, (int)rSoulY, 52, 0, 9, 9);
         }

         this.renderFeatherReady(ctx, rSoulX, rSoulY, soulTexture);
         if (this.screen.activeGimmick == BattleScreen.GimmickType.LIGHT_SHOOTER) {
            this.webShooter.renderCharge(ctx, delta, rSoulX, rSoulY, soulTexture);
         }

         if (!this.screen.peerSoulPos.isEmpty()) {
            long nowNanos = System.nanoTime();

            for (Entry entry : this.screen.peerSoulPos.entrySet()) {
               UUID peerId = entry.getKey();
               int peerIdx = this.screen.participantUuids.indexOf(peerId);
               if (peerIdx >= 0 && peerIdx < this.screen.participants.size()) {
                  SoulType peerSoulType = this.screen.participants.get(peerIdx).soul();
                  Identifier peerTex = peerSoulType != null ? peerSoulType.getTexture() : Identifier.of("turnbattle", "textures/souls/redsoulsimple.png");
                  BattleScreen.PeerSoulSample s = entry.getValue();
                  float peerT = Math.min(1.0F, (float)(nowNanos - s.receivedAtNanos) / 1.0E8F);
                  float relX = s.prevX + (s.curX - s.prevX) * peerT;
                  float relY = s.prevY + (s.curY - s.prevY) * peerT;
                  float peerX = (float)this.screen.boxX + relX * (float)this.screen.targetBoxWidth;
                  float peerY = (float)this.screen.boxY + relY * (float)this.screen.targetBoxHeight;
                  RenderSystem.enableBlend();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.6F);
                  ctx.drawTexture(peerTex, (int)peerX, (int)peerY, 0.0F, 0.0F, 9, 9, 9, 9);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               }
            }
         }
      }

      for (Bullet b : this.screen.bullets) {
         if (b.pendingTicks > 0 && b.kind == 0) {
            boolean flash = b.pendingTicks / 3 % 2 == 0;
            if (flash) {
               float fade = Math.min(1.0F, (float)(20 - b.pendingTicks) / 5.0F + 0.4F);
               int alpha = (int)(255.0F * fade);
               int col = alpha << 24 | 16724787;
               int mx = (int)Math.max((float)(this.screen.boxX + 2), Math.min((float)(this.screen.boxX + this.screen.targetBoxWidth - 6), b.x));
               int my = (int)Math.max((float)(this.screen.boxY + 2), Math.min((float)(this.screen.boxY + this.screen.targetBoxHeight - 6), b.y));
               ctx.drawTextWithShadow(this.screen.getTextRenderer(), Text.literal("!").formatted(Formatting.RED), mx, my - 4, col);
            }
         }
      }

      if (showSoul && (this.screen.grazeRingTick > 0 || this.holdAlpha > 0.0F || this.holdAlphaPrev > 0.0F)) {
         this.renderGrazeRing(ctx, rSoulX, rSoulY);
      }

      if (this.screen.iframeTicks > 18) {
         ctx.fill(this.screen.boxX, this.screen.boxY, this.screen.boxX + 160, this.screen.boxY + 120, 1157562368);
      }

      if (this.screen.featherDashIframes > 0) {
         ctx.fill(this.screen.boxX, this.screen.boxY, this.screen.boxX + this.screen.targetBoxWidth, this.screen.boxY + this.screen.targetBoxHeight, 855681791);
      }

      for (float[] ring : this.screen.shieldRings) {
         int a = (int)(ring[3] * 220.0F);
         if (a > 0) {
            int col = a << 24 | 16777215;
            int cx2 = (int)ring[0];
            int cy2 = (int)ring[1];
            int ringR = (int)ring[2];
            int thickness = 3;

            for (int seg = 0; seg < 16; seg++) {
               double angle = (double)seg * Math.PI / 8.0;

               for (int ri = ringR - thickness; ri <= ringR; ri++) {
                  if (ri >= 0) {
                     int px = cx2 + (int)(Math.cos(angle) * (double)ri);
                     int py = cy2 + (int)(Math.sin(angle) * (double)ri);
                     ctx.fill(px - 1, py - 1, px + 1, py + 1, col);
                  }
               }
            }
         }
      }

      if (this.screen.offhandGimmick != OffhandGimmick.NONE && this.screen.pvpProtectTriggered) {
         ctx.drawCenteredTextWithShadow(
            this.screen.getTextRenderer(), Text.literal("PROTECTED!"), this.screen.getWidth() / 2, this.screen.boxY - 3 - 40, -7811841
         );
      }
   }

   void shootPlayerBullet() {
      this.webShooter.shoot();
   }

   static Element gimmickElementFor(int patternIndex, Element element) {
      if (patternIndex >= 100) {
         int base = patternIndex % 1000;
         int variant = patternIndex / 1000;
         Element human = MobPatterns.humanGimmickElement(base);
         if (human != null) {
            return human;
         }

         boolean zombie = (base == 100 || base == 101) && (variant == 0 || variant == 1);
         boolean skeleton = (base == 110 || base == 111) && variant == 0;
         if (zombie) {
            return Element.DARK;
         }

         if (skeleton) {
            return Element.NATURE;
         }
      }

      return element;
   }

   private void renderHueHint(DrawContext ctx) {
      int hue = MobPatterns.hueInUse();
      if (hue != 0 && this.bulletTick > 0) {
         TextRenderer tr = MinecraftClient.getInstance().textRenderer;
         boolean cyan = hue == 1;
         String msg = cyan ? "CYAN attacks: stay still" : "ORANGE attacks: keep moving";
         int rgb = cyan ? 2803967 : 16752680;
         boolean warn = cyan ? this.lastSoulMoving : !this.lastSoulMoving;
         int a = Math.min(255, Math.min(this.bulletTick * 12, warn ? 255 : 170));
         int x = this.screen.boxX + (this.screen.targetBoxWidth - tr.getWidth(msg)) / 2;
         int y = this.screen.boxY + this.screen.targetBoxHeight + 3 + 3;
         ctx.drawTextWithShadow(tr, msg, x, y, a << 24 | rgb);
      }
   }

   boolean isMobPattern() {
      return this.screen.patternIndex >= 100 && this.screen.activeGimmick == BattleScreen.GimmickType.NONE;
   }

   void startEnemyPhase() {
      this.bulletTick = 0;
      this.soulsHit = 0;
      this.grazeCount = 0;
      this.screen.iframeTicks = 0;
      this.timeCut = 0.0F;
      this.holdTicks = 0;
      this.holdTp = 0.0F;
      this.grazeHeld = false;
      this.holdAlpha = this.holdAlphaPrev = 0.0F;
      this.bulletDuration = Math.max(40, Math.round(210.0F * Math.max(0.1F, Math.min(1.0F, this.screen.turnLengthMult))));
      this.screen.bullets.clear();
      MobPatterns.reset();
      this.screen.playerBullets.clear();
      this.screen.shieldRingUsed = false;
      this.screen.shieldRings.clear();
      this.screen.featherDashIframes = 0;
      this.screen.featherDashCooldown = 0;
      this.featherReadyTick = -1;
      this.screen.featherDashVX = 0.0F;
      this.screen.featherDashVY = 0.0F;
      this.screen.pvpProtectTriggered = false;
      this.screen.soulX = (float)this.screen.boxX + (float)this.screen.targetBoxWidth / 2.0F - 9.0F / 2.0F;
      this.screen.soulY = (float)this.screen.boxY + (float)this.screen.targetBoxHeight / 2.0F - 9.0F / 2.0F;
      this.screen.keyW = this.screen.keyA = this.screen.keyS = this.screen.keyD = false;
      this.screen.keySneak = false;
      this.screen.spacePressed = false;
      this.screen.activeGimmick = BattleScreen.GimmickType.NONE;
      Element gimmickElement = gimmickElementFor(this.screen.patternIndex, this.screen.battleElement);
      boolean storyPattern = this.screen.patternIndex >= 32 && this.screen.patternIndex <= 41;
      if (!storyPattern && gimmickElement != Element.NONE && gimmickElement != Element.FIRE && BulletPatterns.RNG.nextFloat() < 0.3F) {
         this.screen.activeGimmick = switch (gimmickElement) {
            case ICE -> BattleScreen.GimmickType.ICE_PLATFORMER;
            case NATURE -> BattleScreen.GimmickType.NATURE_SHIELD;
            case DARK -> BattleScreen.GimmickType.DARK_LINES;
            case LIGHT -> BattleScreen.GimmickType.LIGHT_SHOOTER;
            default -> BattleScreen.GimmickType.NONE;
         };
      }

      switch (this.screen.activeGimmick) {
         case ICE_PLATFORMER:
            this.modes.startBlue(this.bulletDuration);
            break;
         case NATURE_SHIELD:
            this.modes.startGreen(this.bulletDuration);
            break;
         case DARK_LINES:
            this.gimmickLineIndex = 1;
            this.screen.soulY = (float)this.screen.boxY + (float)this.screen.targetBoxHeight / 2.0F - 9.0F / 2.0F;
            this.webShooter.startPurple();
            break;
         case LIGHT_SHOOTER:
            this.webShooter.startYellow();
      }

      this.screen.currentBoxWidth = 6;
      this.screen.currentBoxHeight = 6;
      this.screen.drSpinTick = 0;
      this.screen.drSpinTickPrev = 0;
      float[] anchor = this.screen.playerSoulAnchor();
      this.screen.soulLeaveStartX = anchor[0];
      this.screen.soulLeaveStartY = anchor[1];
      this.screen.soulLeaveTargetX = (float)this.screen.getWidth() / 2.0F - 9.0F / 2.0F;
      this.screen.soulLeaveTargetY = (float)(this.screen.arenaTop() + this.screen.arenaBottom()) / 2.0F + 5.0F - 9.0F / 2.0F;
      this.screen.soulLeaving = true;
      this.screen.soulLeaveTick = 0;
      this.screen.triggerPlayerGrazePulses();
      this.screen.setPhase(BattleScreen.Phase.ENEMY_TURN);
   }

   void endEnemyTurn() {
      this.screen.peerSoulPos.clear();
      MobPatterns.reset();
      this.screen.drSpinOutTick = 0;
      this.screen.drSpinOutTickPrev = 0;
      this.screen.boxShrinkPlaying = true;
      this.screen.boxRetreatFx = false;
      this.screen.soulReturnStartX = this.screen.soulX;
      this.screen.soulReturnStartY = this.screen.soulY;
      float[] anchor = this.screen.playerSoulAnchor();
      this.screen.soulReturnTargetX = anchor[0];
      this.screen.soulReturnTargetY = anchor[1];
      this.screen.soulReturning = true;
      this.screen.soulReturnTick = 0;
      if (this.screen.anchors3D()) {
         for (UUID u : this.screen.participantUuids) {
            if (u != null && BattleEntityRenderer.getAnim(u) == BattleEntityRenderer.BattleAnim.DEFENDING) {
               BattleEntityRenderer.setAnim(u, BattleEntityRenderer.BattleAnim.IDLE);
            }
         }

         for (BattleScreen.ParticipantInfo p : this.screen.participants) {
            if (BattleEntityRenderer.getRecruitAnim(p.name()) == BattleEntityRenderer.BattleAnim.DEFENDING) {
               BattleEntityRenderer.setRecruitAnim(p.name(), BattleEntityRenderer.BattleAnim.IDLE);
            }
         }
      }

      this.screen.setPhase(BattleScreen.Phase.ENEMY_TURN_END);
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeInt(this.soulsHit);
      buf.writeInt(this.grazeCount);
      buf.writeFloat(this.holdTp);
      this.grazeHeld = false;
      this.holdAlpha = this.holdAlphaPrev = 0.0F;
      Net121.sendToServer(TurnBattleMod.ENEMY_TURN_RESULT, buf);
   }

   void renderKrisSlash(DrawContext ctx, BattleScreen.KrisSlash ks, int bx, int by, int bw, int bh) {
      int t = ks.tick;
      boolean warn = t < 15;
      float frac = 0.0F;
      if (!warn) {
         if (t < 18) {
            frac = (float)(t - 15) / 3.0F;
         } else {
            frac = 1.0F - (float)(t - 18) / 3.0F;
         }

         frac = Math.max(0.0F, Math.min(1.0F, frac));
      }

      int alpha = warn ? 80 + (int)(50.0 * Math.sin((double)t * 0.5)) : 255;
      int col = warn ? alpha << 24 | 13369344 : -1;
      int ht = 16;
      if (ks.type == 1) {
         int cy = (int)ks.pos;
         int x2 = warn ? bx + bw : bx + (int)((float)bw * frac);
         ctx.fill(bx, cy - ht, x2, cy + ht, col);
      } else if (ks.type == 2) {
         int cx = (int)ks.pos;
         int y2 = warn ? by + bh : by + (int)((float)bh * frac);
         ctx.fill(cx - ht, by, cx + ht, y2, col);
      } else if (ks.type == 3 || ks.type == 4) {
         boolean lr = ks.type == 3;
         int maxY = warn ? by + bh : by + (int)((float)bh * frac);

         for (int py = by; py < maxY; py++) {
            float progress = (float)(py - by) / (float)bh;
            int cx = lr ? bx + (int)((float)bw * progress) : bx + bw - (int)((float)bw * progress);
            ctx.fill(cx - ht, py, cx + ht, py + 1, col);
         }
      }
   }

   boolean krisSlashHits(BattleScreen.KrisSlash ks, float soulX, float soulY, int bx, int by, int bw, int bh) {
      float cx = soulX + 9.0F / 2.0F;
      float cy = soulY + 9.0F / 2.0F;
      int ht = 16;
      if (ks.type == 1) {
         return Math.abs(cy - ks.pos) < (float)ht;
      } else if (ks.type == 2) {
         return Math.abs(cx - ks.pos) < (float)ht;
      } else if (ks.type != 3 && ks.type != 4) {
         return false;
      } else {
         float progress = (cy - (float)by) / (float)bh;
         float lineCx = ks.type == 3 ? (float)bx + (float)bw * progress : (float)(bx + bw) - (float)bw * progress;
         return Math.abs(cx - lineCx) < (float)ht;
      }
   }
}
