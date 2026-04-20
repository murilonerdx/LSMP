package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.util.TeleportAnchor;
import br.com.murilo.liberthia.util.TeleportToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class TeleportExecutorActionC2SPacket {
    public enum Action {
        TELEPORT_ONE,
        TELEPORT_ALL,
        UNMARK_ONE,
        CLEAR_ALL
    }

    private final Action action;
    private final UUID targetUuid;

    public TeleportExecutorActionC2SPacket(Action action, UUID targetUuid) {
        this.action = action;
        this.targetUuid = targetUuid;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(targetUuid != null);
        if (targetUuid != null) {
            buf.writeUUID(targetUuid);
        }
    }

    public static TeleportExecutorActionC2SPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        UUID uuid = buf.readBoolean() ? buf.readUUID() : null;
        return new TeleportExecutorActionC2SPacket(action, uuid);
    }

    public static void handle(TeleportExecutorActionC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }

            switch (msg.action) {
                case UNMARK_ONE -> {
                    if (msg.targetUuid != null) {
                        TeleportToolData.removeMarkedPlayer(sender, msg.targetUuid);
                        sender.displayClientMessage(Component.literal("Player desmarcado.").withStyle(ChatFormatting.YELLOW), true);
                    }
                }
                case CLEAR_ALL -> {
                    TeleportToolData.clearMarkedPlayers(sender);
                    sender.displayClientMessage(Component.literal("Todos os players marcados foram limpos.").withStyle(ChatFormatting.YELLOW), true);
                }
                case TELEPORT_ONE -> {
                    if (msg.targetUuid != null) {
                        teleportPlayers(sender, List.of(msg.targetUuid));
                    }
                }
                case TELEPORT_ALL -> teleportPlayers(sender, TeleportToolData.getMarkedPlayers(sender));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void teleportPlayers(ServerPlayer sender, List<UUID> targetUuids) {
        Optional<TeleportAnchor> anchorOptional = TeleportToolData.getAnchor(sender);
        if (anchorOptional.isEmpty()) {
            sender.displayClientMessage(Component.literal("Nenhum local de teleporte foi marcado.").withStyle(ChatFormatting.RED), true);
            return;
        }

        TeleportAnchor anchor = anchorOptional.get();
        ServerLevel targetLevel = sender.server.getLevel(anchor.dimension());
        if (targetLevel == null) {
            sender.displayClientMessage(Component.literal("A dimensão do teleporte não existe mais.").withStyle(ChatFormatting.RED), true);
            return;
        }

        int teleported = 0;
        for (UUID uuid : targetUuids) {
            ServerPlayer target = sender.server.getPlayerList().getPlayer(uuid);
            if (target != null) {
                target.teleportTo(targetLevel, anchor.x(), anchor.y(), anchor.z(), target.getYRot(), target.getXRot());
                teleported++;
            }
        }

        if (teleported > 0) {
            sender.displayClientMessage(Component.literal("Teleportados: " + teleported).withStyle(ChatFormatting.GREEN), true);
        } else {
            sender.displayClientMessage(Component.literal("Nenhum player marcado estava online.").withStyle(ChatFormatting.RED), true);
        }
    }
}
