package com.pixelcite.turnbattle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

/**
 * Thin reflection bridge to Infiniverse's API (net.commoble.infiniverse.api.InfiniverseAPI).
 *
 * Infiniverse is a NeoForge (Mojang-mapped) mod while this mod is Fabric (Yarn-mapped); going through reflection
 * means no Mojang-mapped jar has to be on the Loom compile classpath. Connector remaps the Minecraft types in this
 * class at runtime, and the Infiniverse method names are not obfuscated, so the lookups below are stable.
 */
public final class InfiniverseBridge {
   private static final String API_CLASS = "net.commoble.infiniverse.api.InfiniverseAPI";
   private static Object api;
   private static Method getOrCreateLevel;
   private static Method markForUnregistration;

   private InfiniverseBridge() {
   }

   private static synchronized void init() {
      if (api == null) {
         try {
            Class<?> cls = Class.forName(API_CLASS);
            api = cls.getMethod("get").invoke(null);

            for (Method m : cls.getMethods()) {
               if (m.getName().equals("getOrCreateLevel") && m.getParameterCount() == 3) {
                  getOrCreateLevel = m;
               } else if (m.getName().equals("markDimensionForUnregistration") && m.getParameterCount() == 2) {
                  markForUnregistration = m;
               }
            }

            if (getOrCreateLevel == null || markForUnregistration == null) {
               throw new NoSuchMethodException("Infiniverse API has an unexpected shape (wrong version?)");
            }
         } catch (ReflectiveOperationException | LinkageError var6) {
            api = null;
            throw new IllegalStateException("Infiniverse is required for Dark Worlds but could not be loaded (is it installed?)", var6);
         }
      }
   }

   public static ServerWorld getOrCreateWorld(MinecraftServer server, RegistryKey<World> key, Supplier<DimensionOptions> options) {
      init();

      try {
         return (ServerWorld)getOrCreateLevel.invoke(api, server, key, options);
      } catch (InvocationTargetException var5) {
         throw var5.getCause() instanceof RuntimeException re ? re : new IllegalStateException(var5.getCause());
      } catch (IllegalAccessException var6) {
         throw new IllegalStateException(var6);
      }
   }

   public static void unregisterWorld(MinecraftServer server, RegistryKey<World> key) {
      init();

      try {
         markForUnregistration.invoke(api, server, key);
      } catch (InvocationTargetException var4) {
         throw var4.getCause() instanceof RuntimeException re ? re : new IllegalStateException(var4.getCause());
      } catch (IllegalAccessException var5) {
         throw new IllegalStateException(var5);
      }
   }
}
