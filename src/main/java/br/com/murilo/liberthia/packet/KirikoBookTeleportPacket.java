package br.com.murilo.liberthia.packet;

import br.com.murilo.liberthia.item.BookRedKirikoItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KirikoBookTeleportPacket {

    public enum TargetType {
        DIMENSION,
        PLAYER
    }

    private static final String ALLOWED_PLAYER = "Kiriko";

    private final TargetType type;
    private final String target;
    private final double x;
    private final double y;
    private final double z;

    public KirikoBookTeleportPacket(TargetType type, String target, double x, double y, double z) {
        this.type = type;
        this.target = target;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(KirikoBookTeleportPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
        buf.writeUtf(packet.target);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
    }

    public static KirikoBookTeleportPacket decode(FriendlyByteBuf buf) {
        TargetType type = buf.readEnum(TargetType.class);
        String target = buf.readUtf();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        return new KirikoBookTeleportPacket(type, target, x, y, z);
    }

    public static void handle(KirikoBookTeleportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            if (!canUseBook(player)) {
                player.sendSystemMessage(
                        Component.literal("O Livro Vermelho recusou sua passagem.")
                                .withStyle(ChatFormatting.DARK_RED)
                );
                return;
            }

            if (packet.type == TargetType.PLAYER) {
                teleportToPlayer(player, packet.target);
            } else {
                teleportToDimension(player, packet.target, packet.x, packet.y, packet.z);
            }
        });

        context.setPacketHandled(true);
    }

    private static void teleportToPlayer(ServerPlayer player, String targetName) {
        if (startsWithLNPC(targetName)) {
            player.sendSystemMessage(
                    Component.literal("Esse alvo não pode ser localizado pelo livro.")
                            .withStyle(ChatFormatting.DARK_RED)
            );
            return;
        }

        ServerPlayer target = player.server.getPlayerList().getPlayerByName(targetName);

        if (target == null) {
            player.sendSystemMessage(
                    Component.literal("Player não encontrado: " + targetName)
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        if (startsWithLNPC(target.getGameProfile().getName())) {
            player.sendSystemMessage(
                    Component.literal("Esse alvo não pode ser localizado pelo livro.")
                            .withStyle(ChatFormatting.DARK_RED)
            );
            return;
        }

        ServerLevel targetLevel = target.serverLevel();

        player.teleportTo(
                targetLevel,
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getYRot(),
                target.getXRot()
        );

        BookRedKirikoItem.applyTravelProtection(player);

        player.sendSystemMessage(
                Component.literal("Kiriko atravessou o vermelho até " + target.getGameProfile().getName() + ".")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
        );
    }

    private static void teleportToDimension(
            ServerPlayer player,
            String dimensionId,
            double wantedX,
            double wantedY,
            double wantedZ
    ) {
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);

        if (location == null) {
            player.sendSystemMessage(
                    Component.literal("Dimensão inválida: " + dimensionId)
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        ServerLevel targetLevel = null;

        for (ServerLevel level : player.server.getAllLevels()) {
            if (level.dimension().location().equals(location)) {
                targetLevel = level;
                break;
            }
        }

        if (targetLevel == null) {
            player.sendSystemMessage(
                    Component.literal("Dimensão não encontrada ou não carregada: " + dimensionId)
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        OpenKirikoBookScreenPacket.TeleportPos safePos = BookRedKirikoItem.findSafeTeleportPosition(
                targetLevel,
                wantedX,
                wantedY,
                wantedZ
        );

        player.teleportTo(
                targetLevel,
                safePos.x(),
                safePos.y(),
                safePos.z(),
                player.getYRot(),
                player.getXRot()
        );

        BookRedKirikoItem.applyTravelProtection(player);

        player.sendSystemMessage(
                Component.literal(
                                "Kiriko atravessou o vazio até "
                                        + dimensionId
                                        + " em "
                                        + (int) safePos.x()
                                        + ", "
                                        + (int) safePos.y()
                                        + ", "
                                        + (int) safePos.z()
                                        + "."
                        )
                        .withStyle(ChatFormatting.AQUA)
        );
    }

    private static boolean canUseBook(ServerPlayer player) {
        if (!player.getGameProfile().getName().equalsIgnoreCase(ALLOWED_PLAYER)
                && !player.server.getPlayerList().isOp(player.getGameProfile())) {
            return false;
        }

        return hasBook(player);
    }

    private static boolean hasBook(ServerPlayer player) {
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            if (stack.is(ModItems.RED_KIRIKO_BOOK.get())) {
                return true;
            }
        }

        return false;
    }

    private static boolean startsWithLNPC(String name) {
        return name != null && name.toUpperCase().startsWith("LNPC");
    }
}