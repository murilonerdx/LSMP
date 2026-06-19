package br.com.murilo.liberthia.cutscene;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

/**
 * r190 — /liberthia cutscene — admin reproduz um vídeo (YouTube) na tela de players-alvo
 * (ou todos), com stop/restart/link/reset.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CutsceneCommand {
    private CutsceneCommand() {}

    // r196: padrão = só cinematográfico DENTRO do jogo (sem abrir navegador). Use "browser" p/ forçar o link.
    private static final byte DEFAULT_MODE = (byte) (CutsceneS2CPacket.MODE_CINEMATIC | CutsceneS2CPacket.MODE_LOCK_ESC);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("cutscene")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("play")
                                .then(Commands.argument("url", StringArgumentType.string())
                                        .executes(c -> doPlay(c, self(c), url(c), DEFAULT_MODE))
                                        .then(Commands.argument("alvos", EntityArgument.players())
                                                .executes(c -> doPlay(c, players(c), url(c), DEFAULT_MODE))
                                                .then(Commands.literal("browser").executes(c -> doPlay(c, players(c), url(c), (byte) (CutsceneS2CPacket.MODE_BROWSER | CutsceneS2CPacket.MODE_LOCK_ESC))))
                                                .then(Commands.literal("cinematic").executes(c -> doPlay(c, players(c), url(c), (byte) (CutsceneS2CPacket.MODE_CINEMATIC | CutsceneS2CPacket.MODE_LOCK_ESC))))
                                                .then(Commands.literal("both").executes(c -> doPlay(c, players(c), url(c), DEFAULT_MODE))))))
                        .then(Commands.literal("stop")
                                .executes(c -> doAction(c, self(c), CutsceneAction.STOP, ""))
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> doAction(c, players(c), CutsceneAction.STOP, ""))))
                        .then(Commands.literal("restart")
                                .executes(c -> doAction(c, self(c), CutsceneAction.RESTART, ""))
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> doAction(c, players(c), CutsceneAction.RESTART, ""))))
                        .then(Commands.literal("link")
                                .then(Commands.argument("url", StringArgumentType.string())
                                        .executes(c -> doAction(c, self(c), CutsceneAction.LINK, url(c)))
                                        .then(Commands.argument("alvos", EntityArgument.players())
                                                .executes(c -> doAction(c, players(c), CutsceneAction.LINK, url(c))))))
                        .then(Commands.literal("reset")
                                .executes(c -> doAction(c, self(c), CutsceneAction.RESET, ""))
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> doAction(c, players(c), CutsceneAction.RESET, ""))))));
    }

    private static Collection<ServerPlayer> players(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return EntityArgument.getPlayers(c, "alvos");
    }
    private static Collection<ServerPlayer> self(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return List.of(c.getSource().getPlayerOrException());
    }
    private static String url(CommandContext<CommandSourceStack> c) {
        return StringArgumentType.getString(c, "url");
    }

    private static int doPlay(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets, String url, byte mode) {
        if (url == null || url.isBlank()) {
            c.getSource().sendFailure(Component.literal("§cURL não pode ser vazia."));
            return 0;
        }
        CutsceneS2CPacket pkt = new CutsceneS2CPacket(CutsceneAction.PLAY, url.trim(), mode);
        for (ServerPlayer p : targets) ModNetwork.sendToPlayer(p, pkt);
        int n = targets.size();
        c.getSource().sendSuccess(() -> Component.literal("§5Cutscene: §freproduzindo §a" + url + " §7→ §f" + n + " §7player(s)"), true);
        return n;
    }

    private static int doAction(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets, CutsceneAction action, String url) {
        CutsceneS2CPacket pkt = new CutsceneS2CPacket(action, url, DEFAULT_MODE);
        for (ServerPlayer p : targets) ModNetwork.sendToPlayer(p, pkt);
        int n = targets.size();
        c.getSource().sendSuccess(() -> Component.literal("§5Cutscene: §f" + action.name().toLowerCase() + " §7→ §f" + n + " §7player(s)"), true);
        return n;
    }
}
