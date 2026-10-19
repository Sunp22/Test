package com.pixelcite.turnbattle;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Net121 {
   private static final Set<Identifier> registeredS2C = new HashSet<>();
   private static final Set<Identifier> registeredC2S = new HashSet<>();

   public static void registerS2CChannel(Identifier id) {
      if (registeredS2C.add(id)) {
         Id pid = new Id(id);
         PacketCodec codec = PacketCodec.of((payload, buf) -> {
            PacketByteBuf d = payload.data();
            buf.writeBytes(d, d.readerIndex(), d.readableBytes());
         }, buf -> new Net121.BufPayload(pid, new PacketByteBuf(buf.readBytes(buf.readableBytes()))));
         PayloadTypeRegistry.playS2C().register(pid, codec);
      }
   }

   public static void registerC2SChannel(Identifier id) {
      if (registeredC2S.add(id)) {
         Id pid = new Id(id);
         PacketCodec codec = PacketCodec.of((payload, buf) -> {
            PacketByteBuf d = payload.data();
            buf.writeBytes(d, d.readerIndex(), d.readableBytes());
         }, buf -> new Net121.BufPayload(pid, new PacketByteBuf(buf.readBytes(buf.readableBytes()))));
         PayloadTypeRegistry.playC2S().register(pid, codec);
      }
   }

   public static void sendToClient(ServerPlayerEntity player, Identifier channel, PacketByteBuf buf) {
      Id id = new Id(channel);
      ServerPlayNetworking.send(player, new Net121.BufPayload(id, buf));
   }

   public static void registerServerReceiver(Identifier channel, Net121.ServerReceiver handler) {
      ServerPlayNetworking.registerGlobalReceiver(
         new Id(channel), (payload, context) -> handler.receive(context.server(), context.player(), context.player().networkHandler, payload.data(), null)
      );
   }

   public static void sendToServer(Identifier channel, PacketByteBuf buf) {
      Id id = new Id(channel);
      ClientPlayNetworking.send(new Net121.BufPayload(id, buf));
   }

   public static void registerClientReceiver(Identifier channel, Net121.ClientReceiver handler) {
      ClientPlayNetworking.registerGlobalReceiver(
         new Id(channel), (payload, context) -> handler.receive(context.client(), context.player().networkHandler, payload.data(), null)
      );
   }

   public static record BufPayload(Id<Net121.BufPayload> id, PacketByteBuf data) implements CustomPayload {
      public Id<Net121.BufPayload> getId() {
         return this.id;
      }
   }

   @FunctionalInterface
   public interface ClientReceiver {
      void receive(MinecraftClient var1, ClientPlayNetworkHandler var2, PacketByteBuf var3, Object var4);
   }

   @FunctionalInterface
   public interface ServerReceiver {
      void receive(MinecraftServer var1, ServerPlayerEntity var2, ServerPlayNetworkHandler var3, PacketByteBuf var4, Object var5);
   }
}
