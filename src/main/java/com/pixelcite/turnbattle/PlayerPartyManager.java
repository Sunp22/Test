package com.pixelcite.turnbattle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PlayerPartyManager {
   private static final Map<UUID, Set<UUID>> parties = new ConcurrentHashMap<>();
   private static final Map<UUID, UUID> playerParty = new ConcurrentHashMap<>();
   private static final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, Long>> inviteCooldowns = new ConcurrentHashMap<>();
   private static final long INVITE_COOLDOWN_MS = 10000L;
   private static final Set<UUID> invitesDenied = ConcurrentHashMap.newKeySet();

   private static boolean isOnCooldown(UUID inviter, UUID invitee) {
      Map cd = inviteCooldowns.get(inviter);
      if (cd == null) {
         return false;
      } else {
         Long expiry = cd.get(invitee);
         return expiry != null && System.currentTimeMillis() < expiry;
      }
   }

   private static void startCooldown(UUID inviter, UUID invitee) {
      inviteCooldowns.computeIfAbsent(inviter, k -> new ConcurrentHashMap<>()).put(invitee, System.currentTimeMillis() + 10000L);
   }

   public static boolean hasPublicInvites(UUID player) {
      return !invitesDenied.contains(player);
   }

   public static void setPublicInvites(UUID player, boolean on) {
      if (on) {
         invitesDenied.remove(player);
      } else {
         invitesDenied.add(player);
      }
   }

   public static boolean isInParty(UUID player) {
      return playerParty.containsKey(player);
   }

   public static UUID getPartyId(UUID player) {
      return playerParty.get(player);
   }

   public static List<UUID> getPartyMembers(UUID player) {
      UUID pid = playerParty.get(player);
      if (pid == null) {
         return Collections.singletonList(player);
      } else {
         Set members = parties.get(pid);
         return (List<UUID>)(members == null ? Collections.singletonList(player) : new ArrayList<>(members));
      }
   }

   public static boolean inSameParty(UUID a, UUID b) {
      UUID pa = playerParty.get(a);
      return pa == null ? false : pa.equals(playerParty.get(b));
   }

   public static void sendInvite(ServerPlayerEntity inviter, ServerPlayerEntity invitee) {
      if (inviter.getUuid().equals(invitee.getUuid())) {
         inviter.sendMessage(Text.literal("You can't invite yourself."), false);
      } else if (MagicantManager.isBusy(invitee.getUuid())) {
         inviter.sendMessage(Text.literal(invitee.getName().getString() + " can't be reached right now."), false);
      } else if (inSameParty(inviter.getUuid(), invitee.getUuid())) {
         inviter.sendMessage(Text.literal(invitee.getName().getString() + " is already in your party."), false);
      } else if (!hasPublicInvites(invitee.getUuid())) {
         inviter.sendMessage(Text.literal(invitee.getName().getString() + " has disabled party invites."), false);
      } else if (isOnCooldown(inviter.getUuid(), invitee.getUuid())) {
         inviter.sendMessage(Text.literal("Wait before inviting " + invitee.getName().getString() + " again."), false);
      } else if (!inviter.getWorld().getRegistryKey().equals(invitee.getWorld().getRegistryKey())) {
         inviter.sendMessage(Text.literal(invitee.getName().getString() + " is in a different dimension."), false);
      } else {
         startCooldown(inviter.getUuid(), invitee.getUuid());
         pendingInvites.put(invitee.getUuid(), inviter.getUuid());
         inviter.sendMessage(Text.literal("Party invite sent to " + invitee.getName().getString() + "."), false);
         PacketByteBuf buf = PacketByteBufs.create();
         buf.writeString(inviter.getName().getString());
         buf.writeUuid(inviter.getUuid());
         Net121.sendToClient(invitee, TurnBattleMod.PARTY_INVITE_NOTIFY, buf);
      }
   }

   public static UUID removePendingInvite(UUID invitee) {
      return pendingInvites.remove(invitee);
   }

   public static boolean acceptInvite(ServerPlayerEntity acceptor, String inviterName) {
      UUID inviterUuid = pendingInvites.remove(acceptor.getUuid());
      if (inviterUuid == null) {
         acceptor.sendMessage(Text.literal("No pending party invite."), false);
         return false;
      } else {
         UUID existingPartyId = playerParty.get(inviterUuid);
         if (existingPartyId != null) {
            Set existingMembers = parties.get(existingPartyId);
            if (existingMembers != null && existingMembers.size() >= 8) {
               acceptor.sendMessage(Text.literal("That party is full (max 8 players)."), false);
               ServerPlayerEntity inviter = acceptor.getServer().getPlayerManager().getPlayer(inviterUuid);
               if (inviter != null) {
                  inviter.sendMessage(Text.literal("Party is full \u2014 " + acceptor.getName().getString() + " could not join."), false);
               }

               return false;
            }
         }

         UUID partyId = playerParty.computeIfAbsent(inviterUuid, k -> {
            UUID newId = UUID.randomUUID();
            Set members = ConcurrentHashMap.newKeySet();
            members.add(inviterUuid);
            parties.put(newId, members);
            return newId;
         });
         playerParty.putIfAbsent(inviterUuid, partyId);
         leaveParty(acceptor.getServer(), acceptor.getUuid(), false);
         parties.get(partyId).add(acceptor.getUuid());
         playerParty.put(acceptor.getUuid(), partyId);
         broadcastToParty(acceptor.getServer(), partyId, acceptor.getName().getString() + " joined the party!", acceptor.getUuid());
         acceptor.sendMessage(Text.literal("You joined " + inviterName + "'s party!"), false);
         pushPartyStateById(acceptor.getServer(), partyId);
         return true;
      }
   }

   public static void leaveParty(MinecraftServer server, UUID leaver, boolean notify) {
      UUID partyId = playerParty.remove(leaver);
      if (partyId != null) {
         Set members = parties.get(partyId);
         if (members != null) {
            members.remove(leaver);
            if (members.isEmpty()) {
               parties.remove(partyId);
            } else {
               if (notify) {
                  broadcastToParty(server, partyId, getPlayerName(server, leaver) + " left the party.", leaver);
               }

               pushPartyStateById(server, partyId);
            }
         }
      }
   }

   public static void onMemberEnteredBattle(MinecraftServer server, UUID enteringPlayer, BattleState state) {
      if (!state.magicant) {
         UUID partyId = playerParty.get(enteringPlayer);
         if (partyId != null) {
            Set members = parties.get(partyId);
            if (members != null) {
               ServerPlayerEntity initiator = server.getPlayerManager().getPlayer(enteringPlayer);

               for (UUID memberId : new ArrayList<>(members)) {
                  if (!memberId.equals(enteringPlayer) && !BattleManager.isInBattle(memberId) && !MagicantManager.isBusy(memberId)) {
                     ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                     if (member != null && member.isAlive() && !member.isDead()) {
                        if (initiator != null) {
                           boolean sameWorld = member.getWorld().getRegistryKey().equals(initiator.getWorld().getRegistryKey());
                           if (!sameWorld || member.squaredDistanceTo(initiator) > 225.0) {
                              continue;
                           }
                        }

                        if (!LiveCombat.isLive(member)) {
                           BattleManager.addPlayerToExistingBattle(member, state);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void pushPartyState(MinecraftServer server, UUID player) {
      UUID partyId = playerParty.get(player);
      if (partyId == null) {
         ServerPlayerEntity sp = server.getPlayerManager().getPlayer(player);
         if (sp != null) {
            sendPartyStatePacket(server, sp, Collections.emptyList());
         }
      } else {
         pushPartyStateById(server, partyId);
      }
   }

   private static void pushPartyStateById(MinecraftServer server, UUID partyId) {
      Set members = parties.get(partyId);
      if (members != null) {
         List memberList = new ArrayList<>(members);

         for (UUID m : memberList) {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(m);
            if (sp != null) {
               sendPartyStatePacket(server, sp, memberList);
            }
         }
      }
   }

   private static void sendPartyStatePacket(MinecraftServer server, ServerPlayerEntity player, List<UUID> members) {
      PacketByteBuf buf = PacketByteBufs.create();
      List others = new ArrayList<>();

      for (UUID m : members) {
         if (!m.equals(player.getUuid())) {
            others.add(m);
         }
      }

      buf.writeInt(others.size());

      for (UUID mx : others) {
         buf.writeUuid(mx);
         ServerPlayerEntity mp = server.getPlayerManager().getPlayer(mx);
         String name = mp != null ? mp.getName().getString() : mx.toString();
         buf.writeString(name);
         BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(mx);
         buf.writeString(prog.vesselName != null ? prog.vesselName : name);
         buf.writeInt(prog.soulType != null ? prog.soulType.ordinal() : -1);
         float hp = mp != null ? mp.getHealth() : 0.0F;
         float maxHp = mp != null ? mp.getMaxHealth() : 20.0F;
         buf.writeFloat(hp);
         buf.writeFloat(maxHp);
      }

      Net121.sendToClient(player, TurnBattleMod.PLAYER_PARTY_STATE, buf);
   }

   public static void broadcastPartyHeal(MinecraftServer server, UUID healedPlayer, float hp, float maxHp, float amount) {
      UUID partyId = playerParty.get(healedPlayer);
      if (partyId != null) {
         Set members = parties.get(partyId);
         if (members != null) {
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(healedPlayer);
            String vesselName = prog.vesselName != null && !prog.vesselName.isEmpty()
               ? prog.vesselName
               : (server.getPlayerManager().getPlayer(healedPlayer) != null ? server.getPlayerManager().getPlayer(healedPlayer).getName().getString() : "?");
            int soulOrd = prog.soulType != null ? prog.soulType.ordinal() : -1;

            for (UUID m : new ArrayList<>(members)) {
               if (!m.equals(healedPlayer)) {
                  ServerPlayerEntity sp = server.getPlayerManager().getPlayer(m);
                  if (sp != null) {
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeFloat(hp);
                     buf.writeFloat(maxHp);
                     buf.writeFloat(-amount);
                     buf.writeString(vesselName);
                     buf.writeUuid(healedPlayer);
                     buf.writeInt(soulOrd);
                     Net121.sendToClient(sp, TurnBattleMod.PARTY_MEMBER_DAMAGE, buf);
                  }
               }
            }
         }
      }
   }

   public static void broadcastPartyDamage(MinecraftServer server, UUID damagedPlayer, float hp, float maxHp, float dmg) {
      UUID partyId = playerParty.get(damagedPlayer);
      if (partyId != null) {
         Set members = parties.get(partyId);
         if (members != null) {
            BattleData.PlayerProgress prog = BattleData.get(server).getOrCreate(damagedPlayer);
            String vesselName = prog.vesselName != null && !prog.vesselName.isEmpty()
               ? prog.vesselName
               : (server.getPlayerManager().getPlayer(damagedPlayer) != null ? server.getPlayerManager().getPlayer(damagedPlayer).getName().getString() : "?");
            int soulOrd = prog.soulType != null ? prog.soulType.ordinal() : -1;

            for (UUID m : new ArrayList<>(members)) {
               if (!m.equals(damagedPlayer)) {
                  ServerPlayerEntity sp = server.getPlayerManager().getPlayer(m);
                  if (sp != null) {
                     PacketByteBuf buf = PacketByteBufs.create();
                     buf.writeFloat(hp);
                     buf.writeFloat(maxHp);
                     buf.writeFloat(dmg);
                     buf.writeString(vesselName);
                     buf.writeUuid(damagedPlayer);
                     buf.writeInt(soulOrd);
                     Net121.sendToClient(sp, TurnBattleMod.PARTY_MEMBER_DAMAGE, buf);
                  }
               }
            }
         }
      }
   }

   private static void broadcastToParty(MinecraftServer server, UUID partyId, String msg, UUID except) {
      Set members = parties.get(partyId);
      if (members != null) {
         for (UUID m : new ArrayList<>(members)) {
            if (!m.equals(except)) {
               ServerPlayerEntity sp = server.getPlayerManager().getPlayer(m);
               if (sp != null) {
                  sp.sendMessage(Text.literal("[Party] " + msg), false);
               }
            }
         }
      }
   }

   private static String getPlayerName(MinecraftServer server, UUID uuid) {
      ServerPlayerEntity sp = server.getPlayerManager().getPlayer(uuid);
      return sp != null ? sp.getName().getString() : uuid.toString();
   }
}
