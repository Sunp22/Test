package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleData;
import com.pixelcite.turnbattle.BattleManager;
import com.pixelcite.turnbattle.LiveCombat;
import com.pixelcite.turnbattle.RogueliteManager;
import com.pixelcite.turnbattle.SoulBodyEntity;
import com.pixelcite.turnbattle.TurnBattleConfig;
import com.pixelcite.turnbattle.TurnBattleMod;
import java.util.UUID;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {LivingEntity.class},
   priority = 900
)
public abstract class LivingEntityMixin {
   @Inject(
      method = {"damage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnBattle$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      LivingEntity self = (LivingEntity)this;
      if (!self.getWorld().isClient()) {
         if (self instanceof SoulBodyEntity && !source.getTypeRegistryEntry().matchesKey(DamageTypes.OUT_OF_WORLD)) {
            cir.setReturnValue(false);
         } else if (!LiveCombat.APPLYING) {
            if (self instanceof ServerPlayerEntity player) {
               if (TurnBattleMod.isUntouchable(player.getUuid()) && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                  cir.setReturnValue(false);
               } else {
                  float voidHit = player.getMaxHealth() * 0.25F;
                  if (amount < voidHit && source.getTypeRegistryEntry().matchesKey(DamageTypes.OUT_OF_WORLD)) {
                     cir.setReturnValue(player.damage(source, voidHit));
                  } else if (!BattleManager.isApplyingBattleDamage()) {
                     if (!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && RogueliteManager.isPickingCard(player)) {
                        cir.setReturnValue(false);
                     } else if (BattleManager.activeBattles.containsKey(player.getUuid())) {
                        cir.setReturnValue(false);
                     } else if (LiveCombat.isLive(player)) {
                        if (!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                           if (source.getAttacker() instanceof ServerPlayerEntity turnAttacker
                              && turnAttacker != self
                              && !LiveCombat.isLive(turnAttacker)
                              && !isExcluded(source)
                              && !LiveCombat.sameParty(player.getServer(), turnAttacker.getUuid(), player.getUuid())) {
                              BattleManager.tryStartPvp(turnAttacker, player);
                              cir.setReturnValue(false);
                              return;
                           }

                           Boolean live = LiveCombat.onPlayerDamaged(player, source, amount);
                           if (live != null) {
                              cir.setReturnValue(live);
                           }
                        }
                     } else {
                        if (source.getAttacker() instanceof ServerPlayerEntity liveAttacker && liveAttacker != self && LiveCombat.isLive(liveAttacker)) {
                           if (isExcluded(source)) {
                              return;
                           }

                           if (!LiveCombat.sameParty(player.getServer(), liveAttacker.getUuid(), player.getUuid())) {
                              BattleManager.tryStartPvp(liveAttacker, player);
                           }

                           cir.setReturnValue(false);
                           return;
                        }

                        if (!isExcluded(source)) {
                           if (source.getAttacker() != self) {
                              if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
                                 BattleManager.tryStartPvp(attacker, player);
                                 cir.setReturnValue(false);
                              } else if (source.getAttacker() instanceof LivingEntity && !(source.getAttacker() instanceof ServerPlayerEntity)) {
                                 BattleData data = BattleData.get(player.getServer());
                                 BattleData.PlayerProgress prog = data.getOrCreate(player.getUuid());
                                 if (!prog.soulRemoved) {
                                    BattleManager.tryStartMobInitiated(player, (LivingEntity)source.getAttacker());
                                    cir.setReturnValue(false);
                                 }
                              } else {
                                 cir.setReturnValue(false);
                              }
                           }
                        }
                     }
                  }
               }
            } else if (!BattleManager.IS_ROLLING) {
               if (BattleManager.frozenEntityIds.contains(self.getId())) {
                  if (source.getAttacker() instanceof ServerPlayerEntity playerx
                     && !LiveCombat.isLive(playerx)
                     && !BattleManager.activeBattles.containsKey(playerx.getUuid())) {
                     BattleManager.tryStart(playerx, self);
                  }

                  cir.setReturnValue(false);
               } else {
                  if (source.getAttacker() instanceof ServerPlayerEntity playerx) {
                     if (LiveCombat.isLive(playerx)) {
                        LiveCombat.onVanillaHit(playerx, self);
                        return;
                     }

                     if (!BattleManager.activeBattles.containsKey(playerx.getUuid())) {
                        BattleManager.tryStartPlayerStruck(playerx, self);
                     }
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"setHealth"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void turnBattle$setHealth(float newHealth, CallbackInfo ci) {
      if (!BattleManager.IS_ROLLING) {
         LivingEntity self = (LivingEntity)this;
         if (self instanceof ServerPlayerEntity player) {
            if (!player.getWorld().isClient()) {
               UUID uuid = player.getUuid();
               if (BattleManager.activeBattles.containsKey(uuid)) {
                  if (TurnBattleConfig.get().rollingHealth) {
                     float current = player.getHealth();
                     if (!(newHealth <= current)) {
                        if (BattleManager.isPlayerDowned(uuid)) {
                           ci.cancel();
                        } else {
                           BattleManager.TARGET_HEALTH.put(uuid, Math.min(player.getMaxHealth(), newHealth));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isExcluded(DamageSource source) {
      return source.isIn(DamageTypeTags.IS_FALL)
         || source.isIn(DamageTypeTags.IS_EXPLOSION)
         || source.isIn(DamageTypeTags.IS_FIRE)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.IN_WALL)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.DROWN)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.CACTUS)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.SWEET_BERRY_BUSH)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.FALLING_BLOCK)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.FALLING_ANVIL)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.FALLING_STALACTITE)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.GENERIC_KILL)
         || source.getTypeRegistryEntry().matchesKey(DamageTypes.OUT_OF_WORLD)
         || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.getAttacker() instanceof FallingBlockEntity;
   }
}
