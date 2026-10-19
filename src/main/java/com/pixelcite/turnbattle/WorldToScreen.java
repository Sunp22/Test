package com.pixelcite.turnbattle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class WorldToScreen {
   public static WorldToScreen.ScreenPos project(WorldRenderContext ctx, double relX, double relY, double relZ, int screenWidth, int screenHeight) {
      Vector3f rel = new Vector3f((float)relX, (float)relY, (float)relZ);
      Quaternionf viewRot = new Quaternionf(ctx.camera().getRotation()).conjugate();
      rel.rotate(viewRot);
      Vector4f clip = new Vector4f(rel.x, rel.y, rel.z, 1.0F);
      Matrix4f proj = ctx.projectionMatrix();
      proj.transform(clip);
      if (clip.w <= 0.01F) {
         return null;
      } else {
         float ndcX = clip.x / clip.w;
         float ndcY = clip.y / clip.w;
         int screenX = Math.round((ndcX * 0.5F + 0.5F) * (float)screenWidth);
         int screenY = Math.round((1.0F - (ndcY * 0.5F + 0.5F)) * (float)screenHeight);
         return new WorldToScreen.ScreenPos(screenX, screenY);
      }
   }

   public static record ScreenPos(int x, int y) {
   }
}
