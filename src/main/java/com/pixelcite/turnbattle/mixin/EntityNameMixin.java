package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleData;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityNameMixin {
   @Inject(
      method = {"getName()Lnet/minecraft/text/Text;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void turnBattle$getName(CallbackInfoReturnable<Text> cir) {
      if (this instanceof ServerPlayerEntity player) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            BattleData.PlayerProgress prog = BattleData.get(server).players().get(player.getUuid());
            if (prog != null && prog.vesselName != null && !prog.vesselName.isEmpty()) {
               cir.setReturnValue(Text.literal(prog.vesselName));
            }
         }
      }
   }

   @Inject(
      method = {"getDisplayName()Lnet/minecraft/text/Text;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void turnBattle$getDisplayName(CallbackInfoReturnable<Text> cir) {
      if (this instanceof ServerPlayerEntity player) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            BattleData.PlayerProgress prog = BattleData.get(server).players().get(player.getUuid());
            if (prog != null && prog.vesselName != null && !prog.vesselName.isEmpty()) {
               cir.setReturnValue(Text.literal(prog.vesselName));
            }
         }
      }
   }
}
