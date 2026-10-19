package com.pixelcite.turnbattle.mixin;

import com.pixelcite.turnbattle.DarkFountainSealEffect;
import com.pixelcite.turnbattle.DepthsClient;
import com.pixelcite.turnbattle.LiveCamera;
import com.pixelcite.turnbattle.OverhaulView;
import com.pixelcite.turnbattle.PlayerActions;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
public abstract class CameraOverhaulMixin {
   @Shadow
   private boolean field_18719;

   @Shadow
   protected abstract void method_19327(double var1, double var3, double var5);

   @Shadow
   protected abstract void method_19325(float var1, float var2);

   @Inject(
      method = {"update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"},
      at = {@At("TAIL")}
   )
   private void turnbattle$overhaulCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      if (DarkFountainSealEffect.cameraActive()) {
         DarkFountainSealEffect.updateCamera();
         Vec3d p = DarkFountainSealEffect.cameraPos();
         this.method_19325(DarkFountainSealEffect.cameraYaw(), DarkFountainSealEffect.cameraPitch());
         this.method_19327(p.x, p.y, p.z);
         this.field_18719 = true;
      } else if (OverhaulView.cameraActive()) {
         Vec3d p = OverhaulView.cameraPos(tickDelta);
         this.method_19325(OverhaulView.cameraYaw(tickDelta), OverhaulView.cameraPitch(tickDelta));
         this.method_19327(p.x, p.y, p.z);
         this.field_18719 = true;
      } else if (PlayerActions.cameraActive()) {
         Vec3d p = PlayerActions.cameraPos(tickDelta);
         this.method_19325(PlayerActions.cameraYaw(tickDelta), PlayerActions.cameraPitch(tickDelta));
         this.method_19327(p.x, p.y, p.z);
         this.field_18719 = true;
      } else if (DepthsClient.cameraActive()) {
         DepthsClient.updateCamera(tickDelta);
         Vec3d p = DepthsClient.cameraPos();
         this.method_19325(DepthsClient.cameraYaw(), DepthsClient.cameraPitch());
         this.method_19327(p.x, p.y, p.z);
         this.field_18719 = true;
      } else if (LiveCamera.active()) {
         LiveCamera.update(tickDelta);
         Vec3d p = LiveCamera.pos();
         this.method_19325(LiveCamera.yaw(), LiveCamera.pitch());
         this.method_19327(p.x, p.y, p.z);
         this.field_18719 = true;
      }
   }
}
