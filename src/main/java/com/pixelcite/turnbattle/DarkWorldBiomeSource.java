package com.pixelcite.turnbattle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

public class DarkWorldBiomeSource extends BiomeSource {
   public static final MapCodec<DarkWorldBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
               Biome.REGISTRY_CODEC.listOf().listOf().fieldOf("groups").forGetter(s -> s.groups),
               Biome.REGISTRY_CODEC.listOf().fieldOf("caves").forGetter(s -> s.caves),
               Codec.LONG.fieldOf("seed").forGetter(s -> s.seed),
               Codec.INT.fieldOf("cave_below").forGetter(s -> s.caveBelowBlockY)
            )
            .apply(i, DarkWorldBiomeSource::new)
   );
   private static final double REGION = 44.0;
   private static final double PATCH = 9.0;
   private static final double CAVE_REGION = 30.0;
   private final List<List<RegistryEntry<Biome>>> groups;
   private final List<RegistryEntry<Biome>> caves;
   private final long seed;
   private final int caveBelowBlockY;

   public DarkWorldBiomeSource(List<List<RegistryEntry<Biome>>> groups, List<RegistryEntry<Biome>> caves, long seed, int caveBelowBlockY) {
      this.groups = List.copyOf(groups);
      this.caves = List.copyOf(caves);
      this.seed = seed;
      this.caveBelowBlockY = caveBelowBlockY;
      if (this.groups.isEmpty() && this.caves.isEmpty()) {
         throw new IllegalArgumentException("A Dark World needs at least one biome");
      }
   }

   protected MapCodec<? extends BiomeSource> getCodec() {
      return CODEC;
   }

   protected Stream<RegistryEntry<Biome>> biomeStream() {
      Set all = new LinkedHashSet<>();

      for (List g : this.groups) {
         all.addAll(g);
      }

      all.addAll(this.caves);
      return new ArrayList<>(all).stream();
   }

   public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler noise) {
      boolean deep = !this.caves.isEmpty() && (this.groups.isEmpty() || y << 2 < this.caveBelowBlockY);
      if (deep) {
         int yo = Math.floorDiv(y, 6);
         long cell = region(x + yo * 7, z - yo * 5, 30.0, this.seed ^ 25214903917L);
         return this.caves.get(pick(cell, this.caves.size()));
      } else {
         long cell = region(x, z, 44.0, this.seed);
         List g = this.groups.get(pick(cell, this.groups.size()));
         if (g.size() == 1) {
            return g.get(0);
         } else {
            long patch = region(x, z, 9.0, this.seed ^ cell * -7046029254386353131L);
            return g.get(pick(patch, g.size()));
         }
      }
   }

   private static long region(int qx, int qz, double size, long seed) {
      double f = 1.0 / (size * 1.6);
      double wx = (double)qx
         + size * 0.55 * (valueNoise((double)qx * f, (double)qz * f, seed + 11L) - 0.5) * 2.0
         + size * 0.2 * (valueNoise((double)qx * f * 3.0, (double)qz * f * 3.0, seed + 13L) - 0.5) * 2.0;
      double wz = (double)qz
         + size * 0.55 * (valueNoise((double)qx * f, (double)qz * f, seed + 17L) - 0.5) * 2.0
         + size * 0.2 * (valueNoise((double)qx * f * 3.0, (double)qz * f * 3.0, seed + 19L) - 0.5) * 2.0;
      int cx = (int)Math.floor(wx / size);
      int cz = (int)Math.floor(wz / size);
      double best = Double.MAX_VALUE;
      long bestCell = 0L;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            int ix = cx + dx;
            int iz = cz + dz;
            long h = hash(ix, iz, seed);
            double px = ((double)ix + 0.15 + 0.7 * unit(h)) * size;
            double pz = ((double)iz + 0.15 + 0.7 * unit(h >>> 21)) * size;
            double d = (px - wx) * (px - wx) + (pz - wz) * (pz - wz);
            if (d < best) {
               best = d;
               bestCell = h;
            }
         }
      }

      return bestCell;
   }

   private static int pick(long cellHash, int n) {
      return (int)Math.floorMod(mix(cellHash ^ 7146057691288625177L), (long)n);
   }

   private static double valueNoise(double x, double z, long seed) {
      int x0 = (int)Math.floor(x);
      int z0 = (int)Math.floor(z);
      double fx = x - (double)x0;
      double fz = z - (double)z0;
      double sx = fx * fx * (3.0 - 2.0 * fx);
      double sz = fz * fz * (3.0 - 2.0 * fz);
      double a = unit(hash(x0, z0, seed));
      double b = unit(hash(x0 + 1, z0, seed));
      double c = unit(hash(x0, z0 + 1, seed));
      double d = unit(hash(x0 + 1, z0 + 1, seed));
      return a + (b - a) * sx + (c + (d - c) * sx - (a + (b - a) * sx)) * sz;
   }

   private static long hash(int x, int z, long seed) {
      return mix(seed ^ (long)x * -7046029254386353131L ^ (long)z * -4417276706812531889L);
   }

   private static long mix(long h) {
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      h *= -4265267296055464877L;
      return h ^ h >>> 33;
   }

   private static double unit(long h) {
      return (double)(h >>> 11) * 1.110223E-16F;
   }
}
