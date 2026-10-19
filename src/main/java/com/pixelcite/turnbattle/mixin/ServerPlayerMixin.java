package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.BattleManager;
import com.pixelcite.turnbattle.WallClimbManager;
import java.util.UUID;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayNetworkHandler.class})
public abstract class ServerPlayerMixin {
   @Shadow
   public ServerPlayerEntity field_14140;

   @Inject(
      method = {"onPlayerMove"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void freezeDuringBattleOrClimb(PlayerMoveC2SPacket packet, CallbackInfo ci) {
      if (this.field_14140 != null) {
         if (BattleManager.isInBattle(this.field_14140.getUuid())) {
            ci.cancel();
         } else {
            UUID uuid = this.field_14140.getUuid();
            if (WallClimbManager.isClimbing(uuid) && !WallClimbManager.isMoving(uuid)) {
               this.field_14140.setYaw(packet.getYaw(this.field_14140.getYaw()));
               this.field_14140.setPitch(packet.getPitch(this.field_14140.getPitch()));
               ci.cancel();
            }
         }
      }
   }
}
