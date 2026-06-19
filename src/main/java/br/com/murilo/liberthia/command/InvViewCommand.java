package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.item.WorkerInventoryViewerItem;
import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Backing command pros botões clicáveis do {@link WorkerInventoryViewerItem}.
 * O viewer manda mensagens em chat com {ClickEvent.RUN_COMMAND} apontando aqui.
 *
 * <p>Acesso protegido: requer player ter o item no inventário (ou ser OP).
 */
public final class InvViewCommand {

    private InvViewCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("invview")
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .executes(ctx -> openInv(ctx.getSource(), StringArgumentType.getString(ctx, "uuid"))))));
    }

    private static int openInv(CommandSourceStack src, String uuidStr) {
        if (!(src.getEntity() instanceof ServerPlayer caller)) return 0;

        boolean hasItem = false;
        for (int i = 0; i < caller.getInventory().getContainerSize(); i++) {
            ItemStack s = caller.getInventory().getItem(i);
            if (s.getItem() == ModItems.WORKER_INVENTORY_VIEWER.get()) { hasItem = true; break; }
        }
        if (!hasItem && !caller.hasPermissions(2)) {
            caller.sendSystemMessage(Component.literal("§cVocê precisa do Worker Inventory Viewer no inventário.").withStyle(ChatFormatting.RED));
            return 0;
        }

        UUID uuid;
        try { uuid = UUID.fromString(uuidStr); }
        catch (Exception e) {
            caller.sendSystemMessage(Component.literal("§cUUID inválido.").withStyle(ChatFormatting.RED));
            return 0;
        }
        ServerPlayer target = src.getServer().getPlayerList().getPlayer(uuid);
        if (target == null) {
            caller.sendSystemMessage(Component.literal("§cJogador offline.").withStyle(ChatFormatting.RED));
            return 0;
        }

        WorkerInventoryViewerItem.openInventoryFor(caller, target);
        return 1;
    }
}
