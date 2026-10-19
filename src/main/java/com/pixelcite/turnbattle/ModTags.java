package com.pixelcite.turnbattle;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModTags {
   public static final TagKey<Item> WEAPONS = TagKey.of(RegistryKeys.ITEM, Identifier.of("turnbattle", "weapons"));

   private ModTags() {
   }
}
