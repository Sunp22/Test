package com.pixelcite.turnbattle;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;

public class ElementRegistry {
   private static final Map<EntityType<?>, Element> ELEMENT_MAP = new HashMap<>();

   public static Element getElement(LivingEntity entity) {
      if (entity == null) {
         return Element.NONE;
      } else if (entity instanceof HumanEnemyEntity h) {
         return h.trait().element;
      } else if (entity instanceof MirrorCloneEntity) {
         return Element.NONE;
      } else {
         Element mapped = ELEMENT_MAP.get(entity.getType());
         return mapped != null ? mapped : randomElement(entity.getUuid().getLeastSignificantBits());
      }
   }

   public static Element getElement(EntityType<?> type) {
      if (type == null) {
         return Element.NONE;
      } else if (type == MagicantManager.MIRROR_CLONE) {
         return Element.NONE;
      } else {
         HumanEnemies.Trait human = HumanEnemies.traitOf(type);
         if (human != null) {
            return human.element;
         } else {
            Element mapped = ELEMENT_MAP.get(type);
            return mapped != null ? mapped : randomElement((long)EntityType.getId(type).toString().hashCode());
         }
      }
   }

   private static Element randomElement(long seed) {
      Element[] all = Element.values();
      int range = all.length - 1;
      int idx = (int)(Math.abs(seed) % (long)range);
      return all[idx];
   }

   static {
      ELEMENT_MAP.put(EntityType.BLAZE, Element.FIRE);
      ELEMENT_MAP.put(EntityType.MAGMA_CUBE, Element.FIRE);
      ELEMENT_MAP.put(EntityType.STRAY, Element.ICE);
      ELEMENT_MAP.put(EntityType.POLAR_BEAR, Element.ICE);
      ELEMENT_MAP.put(EntityType.ZOMBIE, Element.NATURE);
      ELEMENT_MAP.put(EntityType.HUSK, Element.NATURE);
      ELEMENT_MAP.put(EntityType.DROWNED, Element.ICE);
      ELEMENT_MAP.put(EntityType.SKELETON, Element.DARK);
      ELEMENT_MAP.put(EntityType.WITHER_SKELETON, Element.DARK);
      ELEMENT_MAP.put(EntityType.WITCH, Element.DARK);
      ELEMENT_MAP.put(EntityType.ENDERMAN, Element.DARK);
      ELEMENT_MAP.put(EntityType.PHANTOM, Element.DARK);
      ELEMENT_MAP.put(EntityType.ZOMBIE_VILLAGER, Element.LIGHT);
      ELEMENT_MAP.put(EntityType.PILLAGER, Element.LIGHT);
      ELEMENT_MAP.put(EntityType.VINDICATOR, Element.LIGHT);
      ELEMENT_MAP.put(EntityType.CREEPER, Element.FIRE);
      ELEMENT_MAP.put(EntityType.SPIDER, Element.NATURE);
      ELEMENT_MAP.put(EntityType.CAVE_SPIDER, Element.DARK);
      ELEMENT_MAP.put(EntityType.SILVERFISH, Element.DARK);
      ELEMENT_MAP.put(EntityType.ENDERMITE, Element.DARK);
      ELEMENT_MAP.put(EntityType.SLIME, Element.NATURE);
      ELEMENT_MAP.put(EntityType.GHAST, Element.FIRE);
      ELEMENT_MAP.put(EntityType.ZOMBIFIED_PIGLIN, Element.FIRE);
      ELEMENT_MAP.put(EntityType.PIGLIN, Element.FIRE);
      ELEMENT_MAP.put(EntityType.PIGLIN_BRUTE, Element.FIRE);
      ELEMENT_MAP.put(EntityType.HOGLIN, Element.FIRE);
      ELEMENT_MAP.put(EntityType.ZOGLIN, Element.FIRE);
      ELEMENT_MAP.put(EntityType.GUARDIAN, Element.ICE);
      ELEMENT_MAP.put(EntityType.ELDER_GUARDIAN, Element.ICE);
      ELEMENT_MAP.put(EntityType.SHULKER, Element.LIGHT);
      ELEMENT_MAP.put(EntityType.VEX, Element.LIGHT);
      ELEMENT_MAP.put(EntityType.EVOKER, Element.DARK);
      ELEMENT_MAP.put(EntityType.RAVAGER, Element.NATURE);
      ELEMENT_MAP.put(EntityType.WARDEN, Element.DARK);
   }
}
