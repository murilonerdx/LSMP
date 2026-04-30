package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BookRedKirikoCommand {

    private static final String ALLOWED_PLAYER = "Kiriko";

    private BookRedKirikoCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("kiriko_book_tp")
                        .then(Commands.literal("player")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .executes(context -> teleportToPlayer(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "target")
                                        ))
                                )
                        )
                        .then(Commands.literal("dimension")
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> teleportToDimension(
                                                context.getSource(),
                                                DimensionArgument.getDimension(context, "dimension")
                                        ))
                                )
                        )
        );
    }

    private static int teleportToPlayer(CommandSourceStack source, String targetName) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Esse comando precisa ser usado por um player."));
            return 0;
        }

        if (!canUseBook(player)) {
            source.sendFailure(
                    Component.literal("O Livro Vermelho não permite sua passagem.")
                            .withStyle(ChatFormatting.DARK_RED)
            );
            return 0;
        }

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);

        if (target == null) {
            source.sendFailure(
                    Component.literal("Player não encontrado: " + targetName)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        if (target.getGameProfile().getName().toUpperCase().contains("LNPC")) {
            source.sendFailure(
                    Component.literal("Esse alvo está bloqueado pelo livro.")
                            .withStyle(ChatFormatting.DARK_RED)
            );
            return 0;
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

        player.sendSystemMessage(
                Component.literal("Kiriko atravessou o vermelho até " + target.getGameProfile().getName() + ".")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
        );

        return 1;
    }

    private static int teleportToDimension(CommandSourceStack source, ServerLevel targetLevel) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Esse comando precisa ser usado por um player."));
            return 0;
        }

        if (!canUseBook(player)) {
            source.sendFailure(
                    Component.literal("O Livro Vermelho não permite sua passagem.")
                            .withStyle(ChatFormatting.DARK_RED)
            );
            return 0;
        }

        ResourceKey<Level> dimension = targetLevel.dimension();
        BlockPos spawn = targetLevel.getSharedSpawnPos();

        double x = spawn.getX() + 0.5;
        double y = spawn.getY() + 1.0;
        double z = spawn.getZ() + 0.5;

        player.teleportTo(
                targetLevel,
                x,
                y,
                z,
                player.getYRot(),
                player.getXRot()
        );

        player.sendSystemMessage(
                Component.literal("Kiriko atravessou o vazio até " + dimension.location() + ".")
                        .withStyle(ChatFormatting.AQUA)
        );

        return 1;
    }

    private static boolean canUseBook(ServerPlayer player) {
        if (!player.getGameProfile().getName().equalsIgnoreCase(ALLOWED_PLAYER)) {
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
}