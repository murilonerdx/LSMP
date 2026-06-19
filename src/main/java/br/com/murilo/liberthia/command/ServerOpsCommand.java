package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.maintenance.ChunkBackupManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * {@code /liberthia server ...} — operações de manutenção do servidor (backup
 * de chunks, desligamento gracioso, restauração de regions corrompidas).
 * Ver {@link ChunkBackupManager}.
 *
 * <ul>
 *   <li>{@code /liberthia server backup} (op4) — salva + copia todos os .mca pra pasta datada.</li>
 *   <li>{@code /liberthia server shutdown [motivo]} (op4) — salva → backup → avisa → kicka todos → 10s → PARA o servidor.</li>
 *   <li>{@code /liberthia server backups} (op2) — lista os backups.</li>
 *   <li>{@code /liberthia server regions <backup>} (op2) — lista os .mca de um backup.</li>
 *   <li>{@code /liberthia server restore <backup> <region>} (op4) — AGENDA restaurar uma region (aplica no próximo boot).</li>
 *   <li>{@code /liberthia server restores} (op2) — lista as restaurações agendadas.</li>
 *   <li>{@code /liberthia server restore-cancel} (op4) — limpa as restaurações agendadas.</li>
 * </ul>
 */
public final class ServerOpsCommand {

    private ServerOpsCommand() {}

    private static final SuggestionProvider<CommandSourceStack> BACKUPS = (ctx, b) -> {
        MinecraftServer s = ctx.getSource().getServer();
        return SharedSuggestionProvider.suggest(ChunkBackupManager.listBackups(s), b);
    };

    private static final SuggestionProvider<CommandSourceStack> REGIONS = (ctx, b) -> {
        MinecraftServer s = ctx.getSource().getServer();
        String backup;
        try { backup = StringArgumentType.getString(ctx, "backup"); }
        catch (IllegalArgumentException e) { return b.buildFuture(); }
        return SharedSuggestionProvider.suggest(ChunkBackupManager.listRegions(s, backup), b);
    };

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("liberthia").then(Commands.literal("server")
                .then(Commands.literal("backup")
                        .requires(s -> s.hasPermission(4))
                        .executes(ServerOpsCommand::doBackup))
                .then(Commands.literal("shutdown")
                        .requires(s -> s.hasPermission(4))
                        .executes(ctx -> doShutdown(ctx, "§cServidor em manutenção. Volte em alguns minutos."))
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                .executes(ctx -> doShutdown(ctx, StringArgumentType.getString(ctx, "motivo")))))
                .then(Commands.literal("backups")
                        .requires(s -> s.hasPermission(2))
                        .executes(ServerOpsCommand::doListBackups))
                .then(Commands.literal("regions")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("backup", StringArgumentType.string()).suggests(BACKUPS)
                                .executes(ServerOpsCommand::doListRegions)))
                .then(Commands.literal("restore")
                        .requires(s -> s.hasPermission(4))
                        .then(Commands.argument("backup", StringArgumentType.string()).suggests(BACKUPS)
                                .then(Commands.argument("region", StringArgumentType.greedyString()).suggests(REGIONS)
                                        .executes(ServerOpsCommand::doRestore))))
                .then(Commands.literal("restores")
                        .requires(s -> s.hasPermission(2))
                        .executes(ServerOpsCommand::doListPending))
                .then(Commands.literal("restore-cancel")
                        .requires(s -> s.hasPermission(4))
                        .executes(ServerOpsCommand::doCancel))
        ));
    }

    private static int doBackup(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        MinecraftServer server = src.getServer();
        src.sendSuccess(() -> Component.literal("§6[Backup] §esalvando o mundo e copiando chunks..."), true);
        // Save na main thread + cópia em background pra não travar o tick.
        Thread t = new Thread(() -> {
            ChunkBackupManager.runOnMainBlocking(server, () -> server.saveEverything(false, true, true), 120);
            ChunkBackupManager.BackupResult res = ChunkBackupManager.createBackup(server);
            server.execute(() -> {
                if (res.ok()) {
                    src.sendSuccess(() -> Component.literal(
                            "§a[Backup] criado: §f" + res.name() + " §7(" + res.files() + " arquivos .mca)"), true);
                } else {
                    src.sendFailure(Component.literal("§c[Backup] falhou — veja o log do servidor."));
                }
            });
        }, "Liberthia-Backup");
        t.setDaemon(true);
        t.start();
        return 1;
    }

    private static int doShutdown(CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSourceStack src = ctx.getSource();
        boolean started = ChunkBackupManager.startGracefulShutdown(src.getServer(), reason);
        if (started) {
            src.sendSuccess(() -> Component.literal(
                    "§6[Manutenção] §esequência iniciada: salvar → backup → avisar → kick → parar."), true);
        } else {
            src.sendFailure(Component.literal("§c[Manutenção] já existe uma sequência de desligamento em andamento."));
        }
        return started ? 1 : 0;
    }

    private static int doListBackups(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<String> backups = ChunkBackupManager.listBackups(src.getServer());
        if (backups.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7Nenhum backup ainda. Use §f/liberthia server backup§7."), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("§6Backups disponíveis (§e" + backups.size() + "§6):"), false);
        for (String b : backups) {
            src.sendSuccess(() -> Component.literal("§7• §f" + b), false);
        }
        return backups.size();
    }

    private static int doListRegions(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String backup = StringArgumentType.getString(ctx, "backup");
        List<String> regions = ChunkBackupManager.listRegions(src.getServer(), backup);
        if (regions.isEmpty()) {
            src.sendFailure(Component.literal("§cBackup '" + backup + "' não encontrado ou sem .mca."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("§6Regions em §f" + backup + " §6(§e" + regions.size() + "§6):"), false);
        int shown = 0;
        for (String r : regions) {
            if (shown++ >= 60) {
                src.sendSuccess(() -> Component.literal("§8... (truncado — veja a pasta do backup)"), false);
                break;
            }
            src.sendSuccess(() -> Component.literal("§7• §f" + r), false);
        }
        return regions.size();
    }

    private static int doRestore(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String backup = StringArgumentType.getString(ctx, "backup");
        String region = StringArgumentType.getString(ctx, "region").trim();
        boolean ok = ChunkBackupManager.scheduleRestore(src.getServer(), backup, region);
        if (ok) {
            src.sendSuccess(() -> Component.literal(
                    "§a[Restore] agendado: §f" + region + " §7(do backup §f" + backup + "§7)."), true);
            src.sendSuccess(() -> Component.literal(
                    "§eAplica no PRÓXIMO boot. Use §f/liberthia server shutdown§e pra parar, depois suba o servidor."), false);
        } else {
            src.sendFailure(Component.literal(
                    "§c[Restore] '" + region + "' não existe no backup '" + backup + "'. Veja §f/liberthia server regions " + backup));
        }
        return ok ? 1 : 0;
    }

    private static int doListPending(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<String> pending = ChunkBackupManager.readPendingLines(src.getServer());
        if (pending.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7Nenhuma restauração agendada."), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("§6Restaurações agendadas (aplicam no próximo boot):"), false);
        for (String line : pending) {
            int sep = line.indexOf('|');
            String b = sep < 0 ? "?" : line.substring(0, sep);
            String r = sep < 0 ? line : line.substring(sep + 1);
            src.sendSuccess(() -> Component.literal("§7• §f" + r + " §8← §7" + b), false);
        }
        return pending.size();
    }

    private static int doCancel(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        boolean had = ChunkBackupManager.clearPending(src.getServer());
        src.sendSuccess(() -> Component.literal(had
                ? "§a[Restore] restaurações agendadas canceladas."
                : "§7Não havia restaurações agendadas."), true);
        return 1;
    }
}
