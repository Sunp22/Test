package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public final class DarkFountainSites {
   static volatile List<DarkFountainSites.Site> sites = List.of();
   static volatile Map<String, int[]> fountains = Map.of();

   private DarkFountainSites() {
   }

   static int[] fountainOf(String dim) {
      return fountains.get(dim);
   }

   static void register() {
      Net121.registerClientReceiver(TurnBattleMod.DARK_FOUNTAIN_SITES, (client, handler, buf, sender) -> {
         List list = new ArrayList<>();
         int n = buf.readVarInt();

         for (int i = 0; i < n; i++) {
            String dim = buf.readString();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            int nd = buf.readVarInt();
            List doors = new ArrayList<>();

            for (int j = 0; j < nd; j++) {
               BlockPos p = BlockPos.fromLong(buf.readLong());
               Direction d = Direction.byId(buf.readByte());
               if (d.getAxis().isHorizontal()) {
                  doors.add(new DarkFountainSites.Door(p, d));
               }
            }

            list.add(new DarkFountainSites.Site(dim, (double)x + 0.5, (double)y, (double)z + 0.5, List.copyOf(doors)));
         }

         Map fs = new HashMap<>();
         if (buf.readableBytes() > 0) {
            int m = buf.readVarInt();

            for (int i = 0; i < m; i++) {
               String dim = buf.readString();
               fs.put(dim, new int[]{buf.readInt(), buf.readInt()});
            }
         }

         client.execute(() -> {
            sites = List.copyOf(list);
            Map merged = new HashMap<>(fountains);
            merged.putAll(fs);
            fountains = Map.copyOf(merged);
         });
      });
   }

   public static record Door(BlockPos pos, Direction out) {
   }

   public static record Site(String dim, double x, double y, double z, List<DarkFountainSites.Door> doors) {
   }
}
