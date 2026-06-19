package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.voice.VoicePlaybackManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * {@code /liberthia play <link>} — toca áudio online (via Simple Voice Chat) pra
 * todos os players. {@code /liberthia play stop} — para a reprodução.
 *
 * <p>OP (permission 2), silencioso (só o admin vê o status). Precisa de ffmpeg
 * no servidor pra formatos comprimidos (mp3/ogg); links .wav diretos funcionam
 * sem ffmpeg.
 */
public final class PlayUrlCommand {

    private PlayUrlCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("play")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("stop")
                                .executes(ctx -> stop(ctx.getSource())))
                        .then(Commands.argument("link", StringArgumentType.greedyString())
                                .executes(ctx -> play(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "link"))))));
    }

    private static int play(CommandSourceStack src, String link) {
        UUID feedback = null;
        try {
            ServerPlayer p = src.getPlayerOrException();
            feedback = p.getUUID();
        } catch (Exception ignored) {
            // console — feedback vai pro próprio source via sendSuccess
        }
        final String url = link.trim();
        src.sendSuccess(() -> Component.literal("§5Liberthia: §finiciando reprodução de §a" + url), false);
        VoicePlaybackManager.playUrlForAll(url, 1.0f, feedback);
        return 1;
    }

    private static int stop(CommandSourceStack src) {
        VoicePlaybackManager.stopAll();
        src.sendSuccess(() -> Component.literal("§5Liberthia: §fparei toda reprodução de áudio."), false);
        return 1;
    }
}
