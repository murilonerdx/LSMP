package br.com.murilo.liberthia.client.hud.unified;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r164: Comandos de gerenciamento de HUD + sync no login.
 *
 * <pre>
 * /liberthia hud editor     → abre o editor drag-and-drop
 * /liberthia hud reset      → reseta posições pros defaults
 * /liberthia hud sync       → força resync (debug)
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HudConfigCommand {

    private HudConfigCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("liberthia")
                .then(Commands.literal("hud")
                        .then(Commands.literal("editor").executes(HudConfigCommand::openEditor))
                        .then(Commands.literal("reset").executes(HudConfigCommand::resetAll))
                        .then(Commands.literal("sync").executes(HudConfigCommand::sync))));
    }

    private static int openEditor(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer sp;
        try { sp = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cSó player pode abrir o editor"));
            return 0;
        }
        // Sync antes pra cliente ter dados frescos
        ModNetwork.sendToPlayer(sp, HudPositionsSyncS2CPacket.forPlayer(sp));
        ModNetwork.sendToPlayer(sp, new OpenHudEditorS2CPacket());
        return 1;
    }

    private static int resetAll(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer sp;
        try { sp = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cSó player"));
            return 0;
        }
        HudPositionsData.resetAll(sp);
        ModNetwork.sendToPlayer(sp, HudPositionsSyncS2CPacket.forPlayer(sp));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Todos HUDs resetados pros defaults"), false);
        return 1;
    }

    private static int sync(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer sp;
        try { sp = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) { return 0; }
        ModNetwork.sendToPlayer(sp, HudPositionsSyncS2CPacket.forPlayer(sp));
        ctx.getSource().sendSuccess(() -> Component.literal("§a✓ HUDs sincronizados"), false);
        return 1;
    }

    /** Auto-sync no login pra carregar posições salvas. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            ModNetwork.sendToPlayer(sp, HudPositionsSyncS2CPacket.forPlayer(sp));
        }
    }
}
