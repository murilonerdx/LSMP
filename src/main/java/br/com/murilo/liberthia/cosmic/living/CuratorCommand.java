package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r47: <b>Curator Commands</b> — admin manipula o Living Server.
 *
 * <h2>Comandos</h2>
 * <pre>
 * /liberthia curator inject_memory &lt;type&gt; [count]
 *   Adiciona memória fake no chunk atual (death/pvp/chat/etc).
 *
 * /liberthia curator chunk_info
 *   Info da memória do chunk atual.
 *
 * /liberthia curator clear_chunk
 *   Limpa memória do chunk atual.
 *
 * /liberthia curator still_hour
 *   Força The Still Hour AGORA.
 *
 * /liberthia curator second_sky &lt;player&gt; &lt;0-100&gt;
 *   Seta exposure de Second Sky no player.
 *
 * /liberthia curator wrong_player &lt;player&gt; assign|clear
 *   Atribui/remove a "wrong player identity" do player.
 *
 * /liberthia curator pressure &lt;value&gt;
 *   Seta observation pressure do chunk atual.
 *
 * /liberthia curator walker &lt;player&gt;
 *   Spawn 1 walker silhouette atrás do player target.
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CuratorCommand {

    private CuratorCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("liberthia")
                .then(Commands.literal("curator")
                        .requires(s -> s.hasPermission(2))

                        // inject_memory
                        .then(Commands.literal("inject_memory")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(ctx -> injectMemory(ctx, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> injectMemory(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "count"))))))

                        // chunk_info
                        .then(Commands.literal("chunk_info")
                                .executes(CuratorCommand::chunkInfo))

                        // clear_chunk
                        .then(Commands.literal("clear_chunk")
                                .executes(CuratorCommand::clearChunk))

                        // still_hour
                        .then(Commands.literal("still_hour")
                                .executes(ctx -> {
                                    StillHourManager.forceStart(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§4§l✦ The Still Hour iniciado."), true);
                                    return 1;
                                }))

                        // second_sky <player> <value>
                        .then(Commands.literal("second_sky")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                .executes(CuratorCommand::secondSky))))

                        // wrong_player <player> assign|clear
                        .then(Commands.literal("wrong_player")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("assign")
                                                .executes(ctx -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                                    String name = WrongPlayerManager.assign(p);
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "§a✓ Wrong identity '" + name + "' atribuída"), true);
                                                    return 1;
                                                }))
                                        .then(Commands.literal("clear")
                                                .executes(ctx -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                                    WrongPlayerManager.clear(p);
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "§a✓ Wrong identity removida"), true);
                                                    return 1;
                                                }))))

                        // pressure <value>
                        .then(Commands.literal("pressure")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            ChunkPos cp = new ChunkPos(
                                                    ctx.getSource().getPlayerOrException().blockPosition());
                                            ObservationPressure.set(cp,
                                                    IntegerArgumentType.getInteger(ctx, "value"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a✓ Pressure setado em chunk " + cp), true);
                                            return 1;
                                        })))

                        // walker <player>
                        .then(Commands.literal("walker")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            // Force exposure to trigger walker
                                            int prevInsanity =
                                                br.com.murilo.liberthia.cosmic.insanity.InsanityData.getInsanity(p);
                                            br.com.murilo.liberthia.cosmic.insanity.InsanityData.setInsanity(p, 60);
                                            // Trigger walker spawn manualmente via reflection seria complexo
                                            // Em vez disso usa hallucination direto
                                            br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager.force(
                                                    p, br.com.murilo.liberthia.cosmic.hallucination
                                                            .HallucinationType.FAKE_ENTITY_PERIPHERAL, 1.0F, 200, "");
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a✓ Walker spawn forçado pra " + p.getName().getString()), true);
                                            return 1;
                                        })))));
    }

    private static int injectMemory(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                     int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = ctx.getSource().getPlayerOrException();
        String type = StringArgumentType.getString(ctx, "type").toLowerCase();
        ChunkPos cp = new ChunkPos(sp.blockPosition());
        ChunkMemory cm = ChunkMemoryStorage.get(sp.serverLevel()).get(cp);
        long now = sp.level().getGameTime();

        for (int i = 0; i < count; i++) {
            switch (type) {
                case "death" -> cm.recordDeath(now);
                case "pvp" -> cm.recordPvpKill(now);
                case "chat" -> cm.recordChat(now, "...", sp.getUUID());
                case "ritual" -> cm.recordRitual(now);
                case "break" -> cm.recordBlockBreak(now);
                case "place" -> cm.recordBlockPlace(now);
                default -> {
                    ctx.getSource().sendFailure(Component.literal(
                            "§cTipo inválido. Opções: death|pvp|chat|ritual|break|place"));
                    return 0;
                }
            }
        }
        ChunkMemoryStorage.get(sp.serverLevel()).setDirty();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Injetado " + count + " " + type + " no chunk " + cp
                        + " (idx agora: " + cm.emotionalIndex() + ")"), true);
        return 1;
    }

    private static int chunkInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = ctx.getSource().getPlayerOrException();
        ChunkPos cp = new ChunkPos(sp.blockPosition());
        ChunkMemory cm = ChunkMemoryStorage.get(sp.serverLevel()).peek(cp);
        if (cm == null) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§7Chunk " + cp + " — §l(memória vazia)"), false);
            return 1;
        }
        int pressure = ObservationPressure.get(cp);
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("§7Chunk §e%s§7:\n"
                        + "  §fEmotional: §e%d§7/100\n"
                        + "  §fDeaths: §c%d§7 (PvP: §c%d§7)\n"
                        + "  §fChat: §b%d§7 | Builds: §a%d§7/§c%d§7\n"
                        + "  §fVisits: §6%d§7t | Rituals: §d%d§7\n"
                        + "  §fObs Pressure: §6%d§7\n"
                        + "  §fEchoes: §e%d",
                        cp, cm.emotionalIndex(), cm.deathCount, cm.pvpKills,
                        cm.chatActivity, cm.blocksPlaced, cm.blocksBroken,
                        cm.visitTicks, cm.ritualsPerformed,
                        pressure, cm.echoes.size())), false);
        return 1;
    }

    private static int clearChunk(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = ctx.getSource().getPlayerOrException();
        ChunkPos cp = new ChunkPos(sp.blockPosition());
        ChunkMemoryStorage.get(sp.serverLevel()).clearChunk(cp);
        ObservationPressure.set(cp, 0);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Memória do chunk " + cp + " apagada"), true);
        return 1;
    }

    private static int secondSky(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int val = IntegerArgumentType.getInteger(ctx, "value");
        SecondSkyManager.setExposure(target, val);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Second Sky exposure de " + target.getName().getString() + " = " + val), true);
        return 1;
    }
}
