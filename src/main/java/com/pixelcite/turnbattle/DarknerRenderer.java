package com.pixelcite.turnbattle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class DarknerRenderer extends BipedEntityRenderer<DarknerEntity, DarknerRenderer.Model> {
   private static ItemStack bulletItem = ItemStack.EMPTY;

   public DarknerRenderer(Context ctx) {
      super(ctx, new DarknerRenderer.Model(ctx.getPart(EntityModelLayers.PLAYER)), 0.5F);
      this.addFeature(new DarknerRenderer.ItemFeature(this, ctx.getItemRenderer()));
   }

   static int tierOf(DarknerEntity e) {
      return e instanceof DarknerBossEntity b ? b.tier() : 0;
   }

   static DarknerRenderer.Look lookOf(DarknerEntity e) {
      return tierOf(e) > 0 ? DarknerRenderer.Look.boss(e.seed()) : DarknerRenderer.Look.of(e.seed());
   }

   public Identifier getTexture(DarknerEntity e) {
      return DarknerRenderer.Skins.texture(e.item(), e.seed(), tierOf(e));
   }

   protected void scale(DarknerEntity e, MatrixStack m, float amount) {
      float s = 0.9375F * lookOf(e).scale() * (tierOf(e) > 0 ? 1.4F : 1.0F);
      m.scale(s, s, s);
   }

   public void render(DarknerEntity e, float yaw, float tickDelta, MatrixStack m, VertexConsumerProvider vcp, int light) {
      DarknerRenderer.Look look = lookOf(e);
      DarknerRenderer.Model md = (DarknerRenderer.Model)this.model;
      md.look = look;
      md.setVisible(true);
      md.head.visible = false;
      md.hat.visible = false;
      md.jacket.visible = false;
      md.leftSleeve.visible = false;
      md.rightSleeve.visible = false;
      md.leftPants.visible = false;
      md.rightPants.visible = false;
      md.body.visible = look.form() == 0;
      super.render(e, yaw, tickDelta, m, vcp, light);
   }

   static void setBulletItem(String id) {
      Identifier i = id != null && !id.isEmpty() ? Identifier.tryParse(id) : null;
      Item it = i == null ? Items.AIR : (Item)Registries.ITEM.get(i);
      bulletItem = it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
   }

   static void renderBullets(DrawContext ctx, float delta, int bx, int by, int bw, int bh, List<Bullet> bullets) {
      boolean any = false;

      for (Bullet b : bullets) {
         if (b.active && b.kind == 41) {
            any = true;
            break;
         }
      }

      if (any) {
         ItemStack st = bulletItem.isEmpty() ? new ItemStack(Items.AMETHYST_SHARD) : bulletItem;
         ctx.enableScissor(bx - 14, by - 14, bx + bw + 14, by + bh + 14);
         MatrixStack m = ctx.getMatrices();

         for (Bullet bxx : bullets) {
            if (bxx.active && bxx.kind == 41) {
               float x = bxx.prevX + (bxx.x - bxx.prevX) * delta;
               float y = bxx.prevY + (bxx.y - bxx.prevY) * delta;
               int h = System.identityHashCode(bxx);
               float age = (float)bxx.age + delta;
               float spin = bxx.behavior == 0 && bxx.p1 != 0.0F ? bxx.p1 : ((h & 1) == 0 ? 0.07F : -0.07F);
               float px = bxx.size * 2.6F;
               if (bxx.behavior == 4) {
                  float f = 1.0F - MathHelper.clamp((float)bxx.timer / 38.0F, 0.0F, 1.0F);
                  px *= 1.0F + 0.35F * f + (bxx.timer < 10 && bxx.timer / 2 % 2 == 0 ? 0.1F : 0.0F);
               }

               m.push();
               m.translate(x, y, 0.0F);
               m.multiply(RotationAxis.POSITIVE_Z.rotation(age * spin + (float)(h % 628) / 100.0F));
               float k = px / 16.0F;
               m.scale(k, k, 1.0F);
               ctx.drawItem(st, -8, -8);
               m.pop();
            }
         }

         ctx.disableScissor();
      }
   }

   static final class ItemFeature extends FeatureRenderer<DarknerEntity, DarknerRenderer.Model> {
      private final ItemRenderer items;

      ItemFeature(FeatureRendererContext<DarknerEntity, DarknerRenderer.Model> ctx, ItemRenderer items) {
         super(ctx);
         this.items = items;
      }

      public void render(
         MatrixStack m,
         VertexConsumerProvider vcp,
         int light,
         DarknerEntity e,
         float limbAngle,
         float limbDistance,
         float tickDelta,
         float age,
         float headYaw,
         float headPitch
      ) {
         ItemStack st = e.item();
         if (!st.isEmpty()) {
            DarknerRenderer.Look l = DarknerRenderer.lookOf(e);
            int tier = DarknerRenderer.tierOf(e);
            if (tier > 0) {
               this.bossExtras(m, vcp, light, e, l, tier, limbDistance, age, st);
            }

            boolean deep;
            try {
               deep = this.items.getModel(st, e.getWorld(), null, 0).hasDepth();
            } catch (Throwable var18) {
               deep = false;
            }

            float s = l.itemScale();
            m.push();
            if (l.form() == 0) {
               ((DarknerRenderer.Model)this.getContextModel()).head.rotate(m);
               m.translate(0.0F, -0.25F * s, 0.0F);
            } else {
               ((DarknerRenderer.Model)this.getContextModel()).body.rotate(m);
               m.translate(0.0F, 0.36F, 0.0F);
               m.multiply(RotationAxis.POSITIVE_Z.rotation(MathHelper.sin(limbAngle * 0.6662F) * 0.12F * limbDistance));
            }

            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l.tilt()));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l.spin()));
            float front = deep ? 0.25F * s + 0.012F : 0.05F * s;
            eyes(m, vcp, light, e, s, front, tier > 0);
            int[] traits = DarknerRenderer.Traits.of(e.seed(), tier);
            headTraits(m, vcp, light, e, s, front, age, traits);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            m.scale(s, -s, -s);
            this.items.renderItem(st, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, m, vcp, e.getWorld(), e.getId());
            m.pop();
            this.bodyTraits(m, vcp, light, e, age, traits, st);
         }
      }

      private static void headTraits(MatrixStack m, VertexConsumerProvider vcp, int light, DarknerEntity e, float s, float front, float age, int[] traits) {
         Identifier tex = DarknerRenderer.Skins.texture(e.item(), e.seed(), DarknerRenderer.tierOf(e));
         VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
         net.minecraft.client.util.math.MatrixStack.Entry en = m.peek();
         float h = 0.25F * s;
         float top = -h;

         for (int t : traits) {
            switch (t) {
               case 0:
                  for (int side = -1; side <= 1; side += 2) {
                     for (int i = 0; i < 4; i++) {
                        float sz = 0.075F - (float)i * 0.013F;
                        float cx = (float)side * (0.5F * h + (float)i * 0.045F);
                        float cy = top - 0.04F - (float)i * 0.075F;
                        cube(
                           en, vc, cx - sz / 2.0F, cy - sz / 2.0F, -sz / 2.0F, cx + sz / 2.0F, cy + sz / 2.0F, sz / 2.0F, DarknerRenderer.Skins.SW_IVORY, light
                        );
                     }
                  }
                  break;
               case 1:
                  float w = 0.62F * h;
                  cube(en, vc, -w, top - 0.05F, -0.12F * h - 0.02F, w, top + 0.005F, 0.12F * h + 0.02F, DarknerRenderer.Skins.SW_GOLD, light);

                  for (int i = 0; i < 5; i++) {
                     float cx = -w + 0.03F + (float)i * (2.0F * w - 0.06F) / 4.0F;
                     cube(
                        en,
                        vc,
                        cx - 0.022F,
                        top - 0.05F - (i % 2 == 0 ? 0.1F : 0.065F),
                        -0.022F,
                        cx + 0.022F,
                        top - 0.05F,
                        0.022F,
                        DarknerRenderer.Skins.SW_GOLD,
                        light
                     );
                  }
                  break;
               case 2:
                  for (int side = -1; side <= 1; side += 2) {
                     float sway = MathHelper.sin(age * 0.12F + (float)side) * 0.03F;
                     float x = (float)side * 0.35F * h;
                     cube(en, vc, x - 0.013F, top - 0.28F, -0.013F, x + 0.013F, top, 0.013F, DarknerRenderer.Skins.SW_DARK, light);
                     cube(en, vc, x + sway - 0.04F, top - 0.34F, -0.04F, x + sway + 0.04F, top - 0.26F, 0.04F, DarknerRenderer.Skins.SW_TRIM, light);
                  }
                  break;
               case 3:
                  float bw = 0.72F * h;
                  float cw = 0.42F * h;
                  cube(en, vc, -bw, top - 0.03F, -0.55F * h, bw, top + 0.005F, 0.55F * h, DarknerRenderer.Skins.SW_DARK, light);
                  cube(en, vc, -cw, top - 0.36F, -0.36F * h, cw, top - 0.03F, 0.36F * h, DarknerRenderer.Skins.SW_DARK, light);
                  cube(
                     en, vc, -cw - 0.004F, top - 0.1F, -0.36F * h - 0.004F, cw + 0.004F, top - 0.06F, 0.36F * h + 0.004F, DarknerRenderer.Skins.SW_TRIM, light
                  );
                  break;
               case 4:
                  VertexConsumer glow = vcp.getBuffer(RenderLayer.getEyes(tex));
                  float y = -0.05F * s;
                  quad(
                     en,
                     glow,
                     -0.17F * s,
                     y - 0.028F * s,
                     0.17F * s,
                     y + 0.028F * s,
                     -front - 0.006F,
                     DarknerRenderer.Skins.SW_GLOW[0],
                     DarknerRenderer.Skins.SW_GLOW[1],
                     DarknerRenderer.Skins.SW_GLOW[0],
                     DarknerRenderer.Skins.SW_GLOW[1],
                     15728880
                  );
            }
         }
      }

      private void bodyTraits(MatrixStack m, VertexConsumerProvider vcp, int light, DarknerEntity e, float age, int[] traits, ItemStack st) {
         Identifier tex = DarknerRenderer.Skins.texture(e.item(), e.seed(), DarknerRenderer.tierOf(e));

         for (int t : traits) {
            m.push();
            ((DarknerRenderer.Model)this.getContextModel()).body.rotate(m);
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            switch (t) {
               case 5:
                  float flap = 0.45F + MathHelper.sin(age * 0.16F) * 0.3F;

                  for (int side = -1; side <= 1; side += 2) {
                     m.push();
                     m.translate((float)side * 0.1F, 0.05F, 0.14F);
                     m.multiply(RotationAxis.POSITIVE_Y.rotation((float)side * -flap));
                     net.minecraft.client.util.math.MatrixStack.Entry enx = m.peek();
                     float x0 = 0.0F;
                     float x1 = (float)side * 0.55F;
                     quad(
                        enx,
                        vc,
                        Math.min(x0, x1),
                        -0.2F,
                        Math.max(x0, x1),
                        0.25F,
                        0.0F,
                        DarknerRenderer.Skins.SW_DARK[0],
                        DarknerRenderer.Skins.SW_DARK[1],
                        DarknerRenderer.Skins.SW_DARK[0],
                        DarknerRenderer.Skins.SW_DARK[1],
                        light
                     );
                     quad(
                        enx,
                        vc,
                        Math.min(x0, x1 * 0.8F),
                        0.25F,
                        Math.max(x0, x1 * 0.8F),
                        0.45F,
                        0.0F,
                        DarknerRenderer.Skins.SW_COAT[0],
                        DarknerRenderer.Skins.SW_COAT[1],
                        DarknerRenderer.Skins.SW_COAT[0],
                        DarknerRenderer.Skins.SW_COAT[1],
                        light
                     );
                     m.pop();
                  }
                  break;
               case 6: {
                  net.minecraft.client.util.math.MatrixStack.Entry en = m.peek();

                  for (int i = 0; i < 6; i++) {
                     float sz = 0.1F - (float)i * 0.012F;
                     float x = MathHelper.sin(age * 0.11F + (float)i * 0.7F) * 0.03F * (float)i;
                     float y = 0.68F + (float)i * 0.06F;
                     float z = 0.14F + (float)i * 0.085F;
                     cube(
                        en,
                        vc,
                        x - sz / 2.0F,
                        y - sz / 2.0F,
                        z - sz / 2.0F,
                        x + sz / 2.0F,
                        y + sz / 2.0F,
                        z + sz / 2.0F,
                        i == 5 ? DarknerRenderer.Skins.SW_TRIM : DarknerRenderer.Skins.SW_COAT,
                        light
                     );
                  }
                  break;
               }
               case 7: {
                  net.minecraft.client.util.math.MatrixStack.Entry en = m.peek();
                  cube(en, vc, -0.27F, -0.02F, -0.15F, 0.27F, 0.08F, 0.15F, DarknerRenderer.Skins.SW_TRIM, light);
                  float sway = MathHelper.sin(age * 0.09F) * 0.04F;
                  cube(en, vc, 0.05F + sway, 0.08F, 0.13F, 0.16F + sway, 0.5F, 0.17F, DarknerRenderer.Skins.SW_TRIM, light);
                  break;
               }
               case 8: {
                  net.minecraft.client.util.math.MatrixStack.Entry en = m.peek();

                  for (int side = -1; side <= 1; side += 2) {
                     for (int i = 0; i < 3; i++) {
                        float x = (float)side * (0.27F + (float)i * 0.06F);
                        float hgt = 0.14F - (float)Math.abs(i - 1) * 0.04F;
                        cube(en, vc, x - 0.025F, -0.02F - hgt, -0.025F, x + 0.025F, -0.02F, 0.025F, DarknerRenderer.Skins.SW_IVORY, light);
                     }
                  }
                  break;
               }
               case 9:
                  m.translate(0.0F, 0.32F, 0.2F);
                  m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(35.0F));
                  m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                  m.scale(0.62F, -0.62F, -0.62F);
                  this.items.renderItem(st, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, m, vcp, e.getWorld(), e.getId() + 3);
            }

            m.pop();
         }
      }

      private static void cube(
         net.minecraft.client.util.math.MatrixStack.Entry en,
         VertexConsumer vc,
         float x0,
         float y0,
         float z0,
         float x1,
         float y1,
         float z1,
         float[] uv,
         int light
      ) {
         float u = uv[0];
         float v = uv[1];
         face(en, vc, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, u, v, light, 0.0F, 0.0F, -1.0F);
         face(en, vc, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, u, v, light, 0.0F, 0.0F, 1.0F);
         face(en, vc, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, u, v, light, -1.0F, 0.0F, 0.0F);
         face(en, vc, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, u, v, light, 1.0F, 0.0F, 0.0F);
         face(en, vc, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, u, v, light, 0.0F, -1.0F, 0.0F);
         face(en, vc, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, u, v, light, 0.0F, 1.0F, 0.0F);
      }

      private static void face(
         net.minecraft.client.util.math.MatrixStack.Entry en,
         VertexConsumer vc,
         float ax,
         float ay,
         float az,
         float bx,
         float by,
         float bz,
         float cx,
         float cy,
         float cz,
         float dx,
         float dy,
         float dz,
         float u,
         float v,
         int light,
         float nx,
         float ny,
         float nz
      ) {
         vc.vertex(en.getPositionMatrix(), ax, ay, az)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, nx, ny, nz);
         vc.vertex(en.getPositionMatrix(), bx, by, bz)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, nx, ny, nz);
         vc.vertex(en.getPositionMatrix(), cx, cy, cz)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, nx, ny, nz);
         vc.vertex(en.getPositionMatrix(), dx, dy, dz)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, nx, ny, nz);
      }

      private static void eyes(MatrixStack m, VertexConsumerProvider vcp, int light, DarknerEntity e, float s, float front, boolean boss) {
         Identifier tex = DarknerRenderer.Skins.texture(e.item(), e.seed(), DarknerRenderer.tierOf(e));
         VertexConsumer vc = vcp.getBuffer(boss ? RenderLayer.getEyes(tex) : RenderLayer.getEntityCutoutNoCull(tex));
         net.minecraft.client.util.math.MatrixStack.Entry en = m.peek();
         float w = (boss ? 0.1F : 0.08F) * s;
         float ex = 0.075F * s;
         float ey = -0.05F * s;
         float z = -front;
         float v0 = boss ? 0.875F : 0.75F;
         float v1 = boss ? 1.0F : 0.875F;
         int lt = boss ? 15728880 : light;
         quad(en, vc, -ex - w / 2.0F, ey - w / 2.0F, -ex + w / 2.0F, ey + w / 2.0F, z, 0.75F, v0, 0.875F, v1, lt);
         quad(en, vc, ex - w / 2.0F, ey - w / 2.0F, ex + w / 2.0F, ey + w / 2.0F, z, 0.875F, v0, 1.0F, v1, lt);
      }

      private void bossExtras(
         MatrixStack m, VertexConsumerProvider vcp, int light, DarknerEntity e, DarknerRenderer.Look l, int tier, float limbDistance, float age, ItemStack st
      ) {
         Identifier tex = DarknerRenderer.Skins.texture(e.item(), e.seed(), tier);
         m.push();
         ((DarknerRenderer.Model)this.getContextModel()).body.rotate(m);
         m.translate(0.0F, 0.02F, 0.16F);
         m.multiply(RotationAxis.POSITIVE_X.rotation(0.12F + Math.min(0.5F, limbDistance * 0.6F) + MathHelper.sin(age * 0.07F) * 0.04F));
         VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
         net.minecraft.client.util.math.MatrixStack.Entry en = m.peek();
         quadN(en, vc, -0.36F, 0.0F, 0.36F, 1.25F, 0.0F, 0.0F, 0.5F, 0.1875F, 0.75F, light, 1.0F);
         m.pop();

         for (int side = -1; side <= 1; side += 2) {
            m.push();
            ((DarknerRenderer.Model)this.getContextModel()).body.rotate(m);
            m.translate((float)side * 0.34F, -0.02F, 0.0F);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)side * 28.0F));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            m.scale(0.42F, -0.42F, -0.42F);
            this.items.renderItem(st, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, m, vcp, e.getWorld(), e.getId() + side);
            m.pop();
         }
      }

      private static void quadN(
         net.minecraft.client.util.math.MatrixStack.Entry en,
         VertexConsumer vc,
         float x0,
         float y0,
         float x1,
         float y1,
         float z,
         float u0,
         float v0,
         float u1,
         float v1,
         int light,
         float nz
      ) {
         vc.vertex(en.getPositionMatrix(), x0, y0, z)
            .color(255, 255, 255, 255)
            .texture(u0, v0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, 0.0F, 0.0F, nz);
         vc.vertex(en.getPositionMatrix(), x0, y1, z)
            .color(255, 255, 255, 255)
            .texture(u0, v1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, 0.0F, 0.0F, nz);
         vc.vertex(en.getPositionMatrix(), x1, y1, z)
            .color(255, 255, 255, 255)
            .texture(u1, v1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, 0.0F, 0.0F, nz);
         vc.vertex(en.getPositionMatrix(), x1, y0, z)
            .color(255, 255, 255, 255)
            .texture(u1, v0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, 0.0F, 0.0F, nz);
      }

      private static void quad(
         net.minecraft.client.util.math.MatrixStack.Entry en,
         VertexConsumer vc,
         float x0,
         float y0,
         float x1,
         float y1,
         float z,
         float u0,
         float v0,
         float u1,
         float v1,
         int light
      ) {
         vtx(en, vc, x0, y0, z, u0, v0, light);
         vtx(en, vc, x0, y1, z, u0, v1, light);
         vtx(en, vc, x1, y1, z, u1, v1, light);
         vtx(en, vc, x1, y0, z, u1, v0, light);
      }

      private static void vtx(net.minecraft.client.util.math.MatrixStack.Entry en, VertexConsumer vc, float x, float y, float z, float u, float v, int light) {
         vc.vertex(en.getPositionMatrix(), x, y, z)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(en, 0.0F, 0.0F, -1.0F);
      }
   }

   static record Look(int form, float scale, float itemScale, float tilt, float spin, float armSpread, boolean armUp, float bob, float phase, int eyes) {
      private static final Map<Integer, DarknerRenderer.Look> CACHE = new HashMap<>();
      private static final Map<Integer, DarknerRenderer.Look> BOSS_CACHE = new HashMap<>();

      static DarknerRenderer.Look of(int seed) {
         DarknerRenderer.Look l = CACHE.get(seed);
         if (l != null) {
            return l;
         } else {
            Random r = new Random((long)seed * 625341585L + 3L);
            int form = r.nextInt(2);
            l = new DarknerRenderer.Look(
               form,
               0.85F + r.nextFloat() * 0.35F,
               form == 0 ? 1.45F + r.nextFloat() * 0.6F : 1.6F + r.nextFloat() * 0.5F,
               (r.nextFloat() - 0.5F) * 30.0F,
               (r.nextFloat() - 0.5F) * 36.0F,
               r.nextFloat() * 0.35F,
               r.nextInt(5) == 0,
               0.3F + r.nextFloat() * 0.9F,
               r.nextFloat() * (float) (Math.PI * 2),
               r.nextInt(4)
            );
            if (CACHE.size() > 512) {
               CACHE.clear();
            }

            CACHE.put(seed, l);
            return l;
         }
      }

      static DarknerRenderer.Look boss(int seed) {
         DarknerRenderer.Look l = BOSS_CACHE.get(seed);
         if (l != null) {
            return l;
         } else {
            DarknerRenderer.Look b = of(seed);
            l = new DarknerRenderer.Look(
               b.form(), 1.0F, b.itemScale() * 1.12F, b.tilt() * 0.35F, b.spin() * 0.5F, 0.18F + b.armSpread(), false, b.bob() * 0.6F, b.phase(), b.eyes()
            );
            if (BOSS_CACHE.size() > 128) {
               BOSS_CACHE.clear();
            }

            BOSS_CACHE.put(seed, l);
            return l;
         }
      }
   }

   static final class Model extends PlayerEntityModel<DarknerEntity> {
      DarknerRenderer.Look look = DarknerRenderer.Look.of(0);

      Model(ModelPart root) {
         super(root, false);
      }

      public void setAngles(DarknerEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
         super.setAngles(e, limbAngle, limbDistance, age, headYaw, headPitch);
         DarknerRenderer.Look l = this.look;
         this.rightArm.roll = this.rightArm.roll + l.armSpread();
         this.leftArm.roll = this.leftArm.roll - l.armSpread();
         float bob = MathHelper.sin(age * 0.12F + l.phase()) * l.bob();
         this.body.pivotY += bob;
         this.head.pivotY += bob;
         this.rightArm.pivotY += bob;
         this.leftArm.pivotY += bob;
      }
   }

   static final class Skins {
      static final float[] SW_DARK = sw(56, 16);
      static final float[] SW_COAT = sw(58, 16);
      static final float[] SW_TRIM = sw(60, 16);
      static final float[] SW_LIGHT = sw(62, 16);
      static final float[] SW_GLOW = sw(56, 18);
      static final float[] SW_IVORY = sw(58, 18);
      static final float[] SW_GOLD = sw(60, 18);
      private static final Map<String, Identifier> CACHE = new LinkedHashMap<String, Identifier>(64, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<String, Identifier> eldest) {
            if (this.size() <= 160) {
               return false;
            } else {
               MinecraftClient.getInstance().getTextureManager().destroyTexture(eldest.getValue());
               return true;
            }
         }
      };
      private static final int[] TIER_GLOW = new int[]{-1, -11141291, -11141121, -22016};
      private static final Map<Item, int[]> PALETTES = new HashMap<>();

      private static float[] sw(int x, int y) {
         return new float[]{(float)(x + 1) / 64.0F, (float)(y + 1) / 64.0F};
      }

      static Identifier texture(ItemStack st, int seed) {
         return texture(st, seed, 0);
      }

      static Identifier texture(ItemStack st, int seed, int tier) {
         String item = st.isEmpty() ? "none" : Registries.ITEM.getId(st.getItem()).toString();
         String key = item + "#" + seed + "#" + tier;
         Identifier id = CACHE.get(key);
         if (id != null) {
            return id;
         } else {
            NativeImage img = paint(st, seed, tier);
            String path = "darkner/"
               + item.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_")
               + "_"
               + Integer.toHexString(seed)
               + (tier > 0 ? "_b" + tier : "");
            id = Identifier.of("turnbattle", path);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
            CACHE.put(key, id);
            return id;
         }
      }

      private static NativeImage paint(ItemStack st, int seed, int tier) {
         Random r = new Random((long)seed ^ 1374496523L);
         int[] pal = palette(st);
         if (pal == null) {
            float h = r.nextFloat();
            pal = new int[]{hsv(h, 0.55F, 0.75F), hsv(h + 0.45F, 0.6F, 0.85F), hsv(h, 0.5F, 0.25F), hsv(h, 0.15F, 0.95F)};
         }

         float shift = (r.nextFloat() - 0.5F) * 0.09F;
         int coat = hueShift(saturate(pal[0], 1.15F), shift);
         int trim = hueShift(pal[1], shift);
         if (r.nextInt(4) == 0) {
            int t = coat;
            coat = trim;
            trim = t;
         }

         if (tier > 0) {
            coat = mix(coat, -16777216, 0.45F);
            trim = saturate(shade(trim, 1.15F), 1.2F);
         }

         int pants = mix(pal[2], coat, 0.25F);
         int shoes = mix(pants, -16777216, 0.5F);

         int hands = switch (r.nextInt(3)) {
            case 0 -> -855310;
            case 1 -> trim;
            default -> mix(pal[2], -16777216, 0.3F);
         };
         int light = pal[3];
         NativeImage img = new NativeImage(64, 64, true);

         for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
               img.setColor(x, y, 0);
            }
         }

         box(img, r, 16, 16, coat);
         int sleeve = r.nextBoolean() ? coat : trim;
         limb(img, r, 40, 16, sleeve, hands, trim, true);
         limb(img, r, 32, 48, sleeve, hands, trim, true);
         limb(img, r, 0, 16, pants, shoes, pants, false);
         limb(img, r, 16, 48, pants, shoes, pants, false);
         front(img, r, coat, trim, light, pants);
         eyes(img, r.nextInt(4));
         if (tier > 0) {
            bossDetails(img, r, coat, trim, light, tier);
         }

         fillRaw(img, 56, 16, 58, 18, mix(pal[2], -16777216, 0.35F));
         fillRaw(img, 58, 16, 60, 18, coat);
         fillRaw(img, 60, 16, 62, 18, trim);
         fillRaw(img, 62, 16, 64, 18, light);
         fillRaw(img, 56, 18, 58, 20, tier > 0 ? TIER_GLOW[Math.min(3, tier)] : saturate(trim, 1.3F));
         fillRaw(img, 58, 18, 60, 20, -1186094);
         fillRaw(img, 60, 18, 62, 20, -1523648);
         return img;
      }

      private static void box(NativeImage img, Random r, int u, int v, int c) {
         fill(img, r, u + 4, v, u + 12, v + 4, shade(c, 1.08F));
         fill(img, r, u + 12, v, u + 20, v + 4, shade(c, 0.75F));
         fill(img, r, u, v + 4, u + 4, v + 16, shade(c, 0.86F));
         fill(img, r, u + 4, v + 4, u + 12, v + 16, c);
         fill(img, r, u + 12, v + 4, u + 16, v + 16, shade(c, 0.86F));
         fill(img, r, u + 16, v + 4, u + 24, v + 16, shade(c, 0.92F));
      }

      private static void limb(NativeImage img, Random r, int u, int v, int c, int end, int cuff, boolean arm) {
         fill(img, r, u + 4, v, u + 8, v + 4, shade(c, 1.08F));
         fill(img, r, u + 8, v, u + 12, v + 4, end);

         for (int i = 0; i < 4; i++) {
            float sh = i == 1 ? 1.0F : (i == 3 ? 0.92F : 0.86F);
            fill(img, r, u + i * 4, v + 4, u + i * 4 + 4, v + 16, shade(c, sh));
            int endRows = arm ? 3 : 2;
            fill(img, r, u + i * 4, v + 16 - endRows, u + i * 4 + 4, v + 16, shade(end, sh));
            if (arm && cuff != c) {
               fill(img, r, u + i * 4, v + 16 - endRows - 1, u + i * 4 + 4, v + 16 - endRows, shade(cuff, sh));
            }
         }
      }

      private static void front(NativeImage img, Random r, int coat, int trim, int light, int dark) {
         switch (r.nextInt(8)) {
            case 0:
               px(img, 23, 22, light);
               px(img, 24, 22, light);
               px(img, 23, 26, light);
               px(img, 24, 26, light);
               break;
            case 1:
               fillRaw(img, 23, 20, 25, 32, trim);
               break;
            case 2:
               fillRaw(img, 16, 28, 40, 30, dark);
               fillRaw(img, 23, 28, 25, 30, light);
               break;
            case 3:
               fillRaw(img, 21, 20, 23, 22, trim);
               fillRaw(img, 25, 20, 27, 22, trim);
               fillRaw(img, 23, 20, 25, 22, mix(trim, -16777216, 0.3F));
               break;
            case 4:
               for (int i = 0; i < 12; i++) {
                  int x = 20 + Math.min(7, i * 8 / 12);
                  fillRaw(img, x, 20 + i, x + 2, 21 + i, trim);
               }
               break;
            case 5:
               px(img, 20, 20, light);
               px(img, 21, 21, light);
               px(img, 22, 22, light);
               px(img, 27, 20, light);
               px(img, 26, 21, light);
               px(img, 25, 22, light);
               break;
            case 6:
               fillRaw(img, 20, 26, 28, 32, trim);
               break;
            default:
               fillRaw(img, 23, 23, 25, 27, light);
               px(img, 22, 25, light);
               px(img, 25, 25, light);
               px(img, 22, 24, light);
               px(img, 25, 24, light);
         }
      }

      private static void bossDetails(NativeImage img, Random r, int coat, int trim, int light, int tier) {
         fillRaw(img, 21, 21, 27, 27, trim);
         fillRaw(img, 22, 27, 26, 28, trim);
         px(img, 21, 21, light);
         px(img, 26, 21, light);
         px(img, 21, 26, light);
         px(img, 26, 26, light);
         fillRaw(img, 23, 22, 25, 26, mix(trim, -16777216, 0.35F));
         fillRaw(img, 44, 16, 48, 20, trim);
         fillRaw(img, 36, 48, 40, 52, trim);
         fillRaw(img, 40, 20, 56, 21, trim);
         fillRaw(img, 32, 52, 48, 53, trim);

         for (int y = 32; y < 48; y++) {
            for (int x = 0; x < 12; x++) {
               int c = mix(coat, -16777216, 0.25F + 0.2F * (float)(y - 32) / 16.0F);
               if (y >= 46) {
                  c = trim;
               }

               if (x == 0 || x == 11) {
                  c = mix(c, -16777216, 0.3F);
               }

               img.setColor(x, y, abgr(shade(c, 0.96F + r.nextFloat() * 0.06F)));
            }
         }

         int glow = TIER_GLOW[Math.max(0, Math.min(3, tier))];

         for (int side = 0; side < 2; side++) {
            int ox = 48 + side * 8;
            int oy = 56;

            for (int y = 0; y < 8; y++) {
               for (int x = 0; x < 8; x++) {
                  img.setColor(ox + x, oy + y, 0);
               }
            }

            for (int x = 0; x < 7; x++) {
               int xx = side == 0 ? x : 7 - x;
               int y = 3 + (x >= 4 ? 1 : 0);
               img.setColor(ox + xx, oy + y, abgr(glow));
               if (x >= 2 && x <= 4) {
                  img.setColor(ox + xx, oy + y + 1, abgr(mix(glow, -1, 0.6F)));
               }
            }
         }
      }

      private static void eyes(NativeImage img, int style) {
         for (int side = 0; side < 2; side++) {
            int ox = 48 + side * 8;
            int oy = 48;

            for (int y = 0; y < 8; y++) {
               for (int x = 0; x < 8; x++) {
                  float dx = (float)x - 3.5F;
                  float dy = (float)y - 3.5F;
                  int c = 0;

                  img.setColor(ox + x, oy + y, abgr(switch (style) {
                     case 0 -> {
                        float d = dx * dx + dy * dy;
                        if (d < 12.5F) {
                           yield d > 8.0F ? -16777216 : -1;
                        }

                        if (Math.abs(dx) < 1.1F && dy > -1.2F && dy < 2.2F) {
                           yield -16777216;
                        }
                     }
                     case 1 -> {
                        if (dx * dx / 4.0F + dy * dy / 10.0F < 1.0F) {
                           yield -16777216;
                        }

                        if (x == 3 && y == 2) {
                           yield -1;
                        }
                     }
                     case 2 -> {
                        float d = dx * dx + dy * dy;
                        if (d < 12.5F && dy > (side == 0 ? dx : -dx) * 0.6F - 1.2F) {
                           yield d > 8.0F ? -16777216 : -1;
                        }

                        if (Math.abs(dx) < 1.1F && dy > 0.0F && dy < 2.5F) {
                           yield -16777216;
                        }
                     }
                     default -> {
                        float d = (float)Math.sqrt((double)(dx * dx + (dy - 1.5F) * (dy - 1.5F)));
                        if (d > 2.2F && d < 3.6F && dy < 1.5F) {
                           yield -16777216;
                        }
                     }
                  }));
               }
            }
         }
      }

      static int[] palette(ItemStack st) {
         if (st.isEmpty()) {
            return null;
         } else if (PALETTES.containsKey(st.getItem())) {
            return PALETTES.get(st.getItem());
         } else {
            int[] out = null;

            try {
               MinecraftClient mc = MinecraftClient.getInstance();
               Sprite sprite = mc.getItemRenderer().getModel(st, null, null, 0).getParticleSprite();
               Identifier sid = sprite.getContents().getId();
               Identifier file = Identifier.of(sid.getNamespace(), "textures/" + sid.getPath() + ".png");
               Optional res = mc.getResourceManager().getResource(file);
               if (res.isPresent()) {
                  try (InputStream in = res.get().getInputStream()) {
                     NativeImage tex = NativeImage.read(in);

                     try {
                        out = extract(tex);
                     } catch (Throwable var13) {
                        if (tex != null) {
                           try {
                              tex.close();
                           } catch (Throwable var12) {
                              var13.addSuppressed(var12);
                           }
                        }

                        throw var13;
                     }

                     if (tex != null) {
                        tex.close();
                     }
                  }
               }
            } catch (Throwable var15) {
            }

            PALETTES.put(st.getItem(), out);
            return out;
         }
      }

      private static int[] extract(NativeImage tex) {
         int w = tex.getWidth();
         int h = Math.min(tex.getHeight(), w);
         Map buckets = new HashMap<>();
         int darkest = -1;
         int lightest = -1;
         float dl = 9.0F;
         float ll = -1.0F;
         List all = new ArrayList<>();

         for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
               int c = tex.getColor(x, y);
               int a = c >>> 24 & 0xFF;
               if (a >= 128) {
                  int rr = c & 0xFF;
                  int gg = c >> 8 & 0xFF;
                  int bb = c >> 16 & 0xFF;
                  int argb = 0xFF000000 | rr << 16 | gg << 8 | bb;
                  all.add(argb);
                  int key = rr >> 5 << 6 | gg >> 5 << 3 | bb >> 5;
                  int[] bk = buckets.computeIfAbsent(key, k -> new int[4]);
                  bk[0]++;
                  bk[1] += rr;
                  bk[2] += gg;
                  bk[3] += bb;
                  float lum = lum(argb);
                  if (lum < dl) {
                     dl = lum;
                     darkest = argb;
                  }

                  if (lum > ll) {
                     ll = lum;
                     lightest = argb;
                  }
               }
            }
         }

         if (all.size() < 4) {
            return null;
         } else {
            List sorted = new ArrayList<>(buckets.values());
            sorted.sort((p, q) -> q[0] - p[0]);
            int base = avg(sorted.get(0));
            int accent = base;

            for (int[] bkx : sorted) {
               int c = avg(bkx);
               if (dist(c, base) > 70) {
                  accent = c;
                  break;
               }
            }

            if (accent == base) {
               accent = hueShift(base, 0.5F);
            }

            if (lum(base) < 0.12F) {
               base = mix(base, accent, 0.35F);
            }

            return new int[]{base, accent, darkest, lightest};
         }
      }

      private static int avg(int[] bk) {
         int n = Math.max(1, bk[0]);
         return 0xFF000000 | bk[1] / n << 16 | bk[2] / n << 8 | bk[3] / n;
      }

      private static int abgr(int argb) {
         int a = argb >>> 24 & 0xFF;
         int rr = argb >> 16 & 0xFF;
         int gg = argb >> 8 & 0xFF;
         int bb = argb & 0xFF;
         return a << 24 | bb << 16 | gg << 8 | rr;
      }

      private static float lum(int c) {
         return (0.299F * (float)(c >> 16 & 0xFF) + 0.587F * (float)(c >> 8 & 0xFF) + 0.114F * (float)(c & 0xFF)) / 255.0F;
      }

      private static int dist(int a, int b) {
         return Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF)) + Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF)) + Math.abs((a & 0xFF) - (b & 0xFF));
      }

      static int mix(int a, int b, float t) {
         int rr = (int)((float)(a >> 16 & 0xFF) * (1.0F - t) + (float)(b >> 16 & 0xFF) * t);
         int gg = (int)((float)(a >> 8 & 0xFF) * (1.0F - t) + (float)(b >> 8 & 0xFF) * t);
         int bb = (int)((float)(a & 0xFF) * (1.0F - t) + (float)(b & 0xFF) * t);
         return 0xFF000000 | rr << 16 | gg << 8 | bb;
      }

      private static int shade(int c, float k) {
         int rr = Math.min(255, (int)((float)(c >> 16 & 0xFF) * k));
         int gg = Math.min(255, (int)((float)(c >> 8 & 0xFF) * k));
         int bb = Math.min(255, (int)((float)(c & 0xFF) * k));
         return 0xFF000000 | rr << 16 | gg << 8 | bb;
      }

      private static float[] toHsv(int c) {
         float rr = (float)(c >> 16 & 0xFF) / 255.0F;
         float gg = (float)(c >> 8 & 0xFF) / 255.0F;
         float bb = (float)(c & 0xFF) / 255.0F;
         float max = Math.max(rr, Math.max(gg, bb));
         float min = Math.min(rr, Math.min(gg, bb));
         float d = max - min;
         float hh = 0.0F;
         if (d > 1.0E-5F) {
            if (max == rr) {
               hh = (gg - bb) / d % 6.0F;
            } else if (max == gg) {
               hh = (bb - rr) / d + 2.0F;
            } else {
               hh = (rr - gg) / d + 4.0F;
            }

            hh /= 6.0F;
            if (hh < 0.0F) {
               hh++;
            }
         }

         return new float[]{hh, max <= 0.0F ? 0.0F : d / max, max};
      }

      private static int hsv(float hh, float ss, float vv) {
         hh = (hh % 1.0F + 1.0F) % 1.0F;
         ss = MathHelper.clamp(ss, 0.0F, 1.0F);
         vv = MathHelper.clamp(vv, 0.0F, 1.0F);
         return 0xFF000000 | MathHelper.hsvToRgb(hh, ss, vv) & 16777215;
      }

      private static int hueShift(int c, float by) {
         float[] f = toHsv(c);
         return hsv(f[0] + by, f[1], f[2]);
      }

      private static int saturate(int c, float k) {
         float[] f = toHsv(c);
         return hsv(f[0], f[1] * k, f[2]);
      }

      private static void px(NativeImage img, int x, int y, int argb) {
         img.setColor(x, y, abgr(argb));
      }

      private static void fillRaw(NativeImage img, int x0, int y0, int x1, int y1, int argb) {
         for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
               img.setColor(x, y, abgr(argb));
            }
         }
      }

      private static void fill(NativeImage img, Random r, int x0, int y0, int x1, int y1, int argb) {
         for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
               img.setColor(x, y, abgr(shade(argb, 0.96F + r.nextFloat() * 0.06F)));
            }
         }
      }
   }

   static final class Traits {
      static final int HORNS = 0;
      static final int CROWN = 1;
      static final int ANTENNAE = 2;
      static final int TOP_HAT = 3;
      static final int VISOR = 4;
      static final int WINGS = 5;
      static final int TAIL = 6;
      static final int SCARF = 7;
      static final int SHOULDER_SPIKES = 8;
      static final int BACK_ITEM = 9;
      static final int COUNT = 10;
      private static final Map<Long, int[]> CACHE = new HashMap<>();

      static int[] of(int seed, int tier) {
         long key = (long)seed << 3 | (long)tier;
         int[] t = CACHE.get(key);
         if (t != null) {
            return t;
         } else {
            Random r = new Random((long)seed * -7046029254386353131L + 21L);
            int want = tier > 0 ? 2 : 1;
            List pool = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
               if (tier <= 0 || i != 4) {
                  pool.add(i);
               }
            }

            t = new int[want];
            boolean head = false;

            for (int ix = 0; ix < want; ix++) {
               int pick;
               do {
                  pick = pool.remove(r.nextInt(pool.size()));
               } while (head && pick <= 4 && !pool.isEmpty());

               if (pick <= 4) {
                  head = true;
               }

               t[ix] = pick;
            }

            if (CACHE.size() > 1024) {
               CACHE.clear();
            }

            CACHE.put(key, t);
            return t;
         }
      }
   }
}
