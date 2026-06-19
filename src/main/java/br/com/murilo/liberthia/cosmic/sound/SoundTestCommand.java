package br.com.murilo.liberthia.cosmic.sound;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r53: comando admin pra testar cada cosmic sound direto.
 *
 * <pre>
 * /liberthia sound test &lt;type&gt; [player]
 *   types: whispers, eye_pulse, footsteps, breathing, radio,
 *          sky_hum, tendril, distortion, scream, audience, all
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SoundTestCommand {

    private SoundTestCommand() {}

    private static final String[] TYPES = {
            "whispers", "eye_pulse", "footsteps", "breathing", "radio",
            "sky_hum", "tendril", "distortion", "scream", "audience", "all"
    };

    private static final SuggestionProvider<CommandSourceStack> TYPE_SUGGEST =
            (ctx, b) -> { for (String t : TYPES) b.suggest(t); return b.buildFuture(); };

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("liberthia")
                .then(Commands.literal("sound")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("test")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPE_SUGGEST)
                                        .executes(ctx -> playTest(ctx,
                                                ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> playTest(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"))))))));
    }

    private static int playTest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                 ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String type = StringArgumentType.getString(ctx, "type").toLowerCase();
        switch (type) {
            case "whispers" -> CosmicSoundManager.playDistantWhispers(target);
            case "eye_pulse" -> CosmicSoundManager.playEyePulse(target);
            case "footsteps" -> CosmicSoundManager.playFalseFootsteps(target, true);
            case "breathing" -> CosmicSoundManager.playVoidBreathing(target);
            case "radio" -> CosmicSoundManager.playRadioBroadcast(target);
            case "sky_hum" -> CosmicSoundManager.playSkyHum(target);
            case "tendril" -> CosmicSoundManager.playTendrilMovement(target);
            case "distortion" -> CosmicSoundManager.playRealityDistortion(target);
            case "scream" -> CosmicSoundManager.playDistantScream(target);
            case "audience" -> CosmicSoundManager.playAudiencePresence(target);
            case "all" -> {
                // Toca todos com 1s delay entre cada (server scheduler simples via ticks)
                java.util.concurrent.atomic.AtomicInteger step = new java.util.concurrent.atomic.AtomicInteger(0);
                String[] sequence = {"whispers", "eye_pulse", "footsteps", "breathing", "radio",
                        "sky_hum", "tendril", "distortion", "scream", "audience"};
                // Para cada som, scheduler dispara em delay
                final var server = ctx.getSource().getServer();
                for (int i = 0; i < sequence.length; i++) {
                    final String t = sequence[i];
                    final int delay = i * 20; // 1s entre cada
                    server.tell(new net.minecraft.server.TickTask(
                            server.getTickCount() + delay,
                            () -> playSingle(t, target)));
                }
                target.displayClientMessage(Component.literal(
                        "§5§l✦ Testando §6todos§5 os 10 sons em sequência (10s total)..."), false);
                return 1;
            }
            default -> {
                ctx.getSource().sendFailure(Component.literal(
                        "§cTipos: " + String.join(", ", TYPES)));
                return 0;
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Tocou §6" + type + "§a em §e" + target.getName().getString()), false);
        return 1;
    }

    private static void playSingle(String type, ServerPlayer target) {
        switch (type) {
            case "whispers" -> CosmicSoundManager.playDistantWhispers(target);
            case "eye_pulse" -> CosmicSoundManager.playEyePulse(target);
            case "footsteps" -> CosmicSoundManager.playFalseFootsteps(target, true);
            case "breathing" -> CosmicSoundManager.playVoidBreathing(target);
            case "radio" -> CosmicSoundManager.playRadioBroadcast(target);
            case "sky_hum" -> CosmicSoundManager.playSkyHum(target);
            case "tendril" -> CosmicSoundManager.playTendrilMovement(target);
            case "distortion" -> CosmicSoundManager.playRealityDistortion(target);
            case "scream" -> CosmicSoundManager.playDistantScream(target);
            case "audience" -> CosmicSoundManager.playAudiencePresence(target);
        }
        target.displayClientMessage(Component.literal(
                "§7§o→ " + type), true);
    }
}
