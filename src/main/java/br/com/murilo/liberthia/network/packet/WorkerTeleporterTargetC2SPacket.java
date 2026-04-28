package br.com.murilo.liberthia.network.packet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S from {@code WorkerTeleporterScreen} carrying the UUID of the chosen
 * target. The server teleports the sender (who holds the Worker Teleporter
 * stone) to the target's current position in the target's dimension.
 */
public class WorkerTeleporterTargetC2SPacket {
    private final UUID targetUuid;

    public WorkerTeleporterTargetC2SPacket(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetUuid);
    }

    public static WorkerTeleporterTargetC2SPacket decode(FriendlyByteBuf buf) {
        return new WorkerTeleporterTargetC2SPacket(buf.readUUID());
    }

    public static void handle(WorkerTeleporterTargetC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || msg.targetUuid == null) {
                return;
            }
            ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.targetUuid);
            if (target == null) {
                sender.displayClientMessage(
                        Component.literal("Jogador não está mais online.").withStyle(ChatFormatting.RED),
                        true);
                return;
            }
            if (target == sender) return;

            ServerLevel targetLevel = (ServerLevel) target.level();
            if (sender.level() != targetLevel) {
                sender.teleportTo(targetLevel, target.getX(), target.getY(), target.getZ(),
                        sender.getYRot(), sender.getXRot());
            } else {
                sender.teleportTo(target.getX(), target.getY(), target.getZ());
            }
            sender.level().playSound(null, sender.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            sender.displayClientMessage(
                    Component.literal("Teleportado até " + target.getGameProfile().getName())
                            .withStyle(ChatFormatting.GREEN), true);
            sender.getCooldowns().addCooldown(
                    sender.getMainHandItem().getItem(), 20 * 20);
        });
        ctx.get().setPacketHandled(true);
    }
}
