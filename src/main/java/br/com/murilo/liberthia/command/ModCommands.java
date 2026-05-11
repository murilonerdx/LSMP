package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.admin.api.AdminHttpServer;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.registry.ModCapabilities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("voice")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    String dump = br.com.murilo.liberthia.voice.LiberthiaVoicePlugin.statusDump();
                                    for (String line : dump.split("\n")) {
                                        context.getSource().sendSuccess(() -> Component.literal(line), false);
                                    }
                                    return 1;
                                }))
                )
                .then(Commands.literal("immune")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");

                                    player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                                        data.setImmune(enabled);
                                        if (enabled) {
                                            data.setInfection(0);
                                            data.setPermanentHealthPenalty(0);
                                            InfectionLogic.applyDerivedEffects(player, data);
                                        }
                                        InfectionLogic.sync(player, data);
                                    });

                                    context.getSource().sendSuccess(() -> Component.literal("§dLiberthia: §fImunidade definida para: " + (enabled ? "§aLigado" : "§cDesligado")), true);
                                    return 1;
                                }))
                )
                // NOTA: subcomando "admin" propositadamente NÃO registrado no dispatcher.
                // Some do autocomplete completamente. É interceptado em
                // AdminCommandInterceptor.onCommand() via CommandEvent (Forge).
                // Player digita manualmente "/liberthia admin link" e o handler roda.
        );
    }

    /**
     * Roteador secreto chamado por AdminCommandInterceptor quando alguém digita
     * /liberthia admin <sub>. Não passa pelo Brigadier porque "admin" não é
     * subcomando registrado — é truque pra manter fora do tab-complete.
     */
    public static int handleSecretAdmin(CommandSourceStack source, String sub) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.literal("§cVocê não tem permissão"));
            return 0;
        }
        String s = sub == null ? "" : sub.trim().toLowerCase();
        return switch (s) {
            case "", "link" -> printAdminLink(source);
            case "token" -> printAdminToken(source);
            case "status" -> printAdminStatus(source);
            case "test" -> testConnectivity(source);
            case "enable" -> setEnabled(source, true);
            case "disable" -> setEnabled(source, false);
            case "restart" -> restartApi(source);
            default -> {
                source.sendFailure(Component.literal("§cSubcomando admin desconhecido: §f" + s));
                yield 0;
            }
        };
    }

    /** Print clickable link with mod URL + token + frontend URL. */
    private static int printAdminLink(CommandSourceStack source) {
        String token = AdminHttpServer.getActiveToken();
        if (token == null) {
            source.sendFailure(Component.literal("§cAdmin API desligada. Liga em liberthia-server.toml: admin_api.enabled = true"));
            return 0;
        }
        int port = LiberthiaConfig.SERVER.adminApiPort.get();
        String localIp = detectLocalIp();
        String publicHint = localIp;
        try {
            String ip = source.getServer().getLocalIp();
            if (ip != null && !ip.isBlank()) publicHint = ip;
        } catch (Throwable ignored) {}

        String modUrlLocal = "http://" + localIp + ":" + port;
        String modUrlPublic = "http://" + publicHint + ":" + port;

        source.sendSuccess(() -> Component.literal("§5§l=== Liberthia Admin Panel ===").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Mod URL (local)
        source.sendSuccess(() -> linkLine("§7Mod URL (local):  ", modUrlLocal), false);
        // Mod URL (público/hostname)
        if (!localIp.equals(publicHint)) {
            source.sendSuccess(() -> linkLine("§7Mod URL (público): ", modUrlPublic), false);
        }
        // Token
        source.sendSuccess(() -> Component.literal("§7Token: §f" + token).withStyle(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, token))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§eClick pra copiar")))), false);

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7Cole essas infos no §fbackend/src/main/resources/application.yml§7:"), false);
        source.sendSuccess(() -> Component.literal("§8  mod.url:   §f" + modUrlLocal), false);
        source.sendSuccess(() -> Component.literal("§8  mod.token: §f" + token), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Frontend dev URL
        String frontUrl = "http://" + publicHint + ":5173";
        source.sendSuccess(() -> linkLine("§a§l→ Painel (dev): ", frontUrl), false);
        source.sendSuccess(() -> Component.literal("§8  (rodando " + "§7npm run dev" + " §8em frontend/)"), false);
        return 1;
    }

    private static int printAdminToken(CommandSourceStack source) {
        String token = AdminHttpServer.getActiveToken();
        if (token == null) {
            source.sendFailure(Component.literal("§cAdmin API desligada"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§7Token: §f" + token).withStyle(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, token))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§eClick pra copiar")))), false);
        return 1;
    }

    private static int printAdminStatus(CommandSourceStack source) {
        boolean enabled = LiberthiaConfig.SERVER.adminApiEnabled.get();
        int port = LiberthiaConfig.SERVER.adminApiPort.get();
        String bind = LiberthiaConfig.SERVER.adminApiBindAddress.get();
        boolean running = AdminHttpServer.isRunning();
        source.sendSuccess(() -> Component.literal(
                "§5Admin API: " + (running ? "§aativo" : "§coff") +
                " §7| port=§f" + port + " §7bind=§f" + bind + " §7configEnabled=§f" + enabled), false);
        return 1;
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        LiberthiaConfig.SERVER.adminApiEnabled.set(enabled);
        LiberthiaConfig.SERVER.adminApiEnabled.save();
        if (enabled) {
            boolean ok = AdminHttpServer.startNow();
            source.sendSuccess(() -> Component.literal(ok
                    ? "§aAdmin API ATIVADA. Use /liberthia admin link pra ver URL."
                    : "§cFalhou ao iniciar (porta em uso? veja log)"), true);
        } else {
            AdminHttpServer.stopNow();
            source.sendSuccess(() -> Component.literal("§cAdmin API DESATIVADA. Backend não vai mais conseguir conectar."), true);
        }
        return 1;
    }

    private static int restartApi(CommandSourceStack source) {
        AdminHttpServer.stopNow();
        boolean ok = AdminHttpServer.startNow();
        source.sendSuccess(() -> Component.literal(ok
                ? "§aAdmin API reiniciada"
                : "§cFalhou ao reiniciar"), true);
        return 1;
    }

    /**
     * Faz HTTP self-check em /health usando 127.0.0.1, IP da LAN e bind address.
     * Reporta tempo de resposta e status. Roda em thread separada pra não travar tick.
     */
    private static int testConnectivity(CommandSourceStack source) {
        if (!AdminHttpServer.isRunning()) {
            source.sendFailure(Component.literal("§cAdmin API desligada — use §f/liberthia admin enable§c primeiro"));
            return 0;
        }
        int port = LiberthiaConfig.SERVER.adminApiPort.get();
        String bind = LiberthiaConfig.SERVER.adminApiBindAddress.get();
        String lanIp = detectLocalIp();

        source.sendSuccess(() -> Component.literal("§5§l=== Liberthia Connectivity Test ===").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.literal("§7Testando em background..."), false);

        new Thread(() -> {
            probeHost(source, "127.0.0.1", port);
            probeHost(source, lanIp, port);
            if (!"0.0.0.0".equals(bind) && !bind.equals(lanIp) && !bind.equals("127.0.0.1")) {
                probeHost(source, bind, port);
            }
            // Testa também o backend (se configurado)
            String backendUrl = LiberthiaConfig.SERVER.adminBackendUrl.get();
            if (backendUrl != null && !backendUrl.isBlank()) {
                probeBackend(source, backendUrl);
            } else {
                source.sendSuccess(() -> Component.literal("§8(backend_url não configurado em liberthia-server.toml)"), false);
            }
        }, "Liberthia-Connectivity-Test").start();
        return 1;
    }

    private static void probeHost(CommandSourceStack source, String host, int port) {
        String url = "http://" + host + ":" + port + "/health";
        long t0 = System.currentTimeMillis();
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            long ms = System.currentTimeMillis() - t0;
            String tag = (code == 200) ? "§a✓" : "§e?";
            source.sendSuccess(() -> Component.literal(tag + " §7" + url + " §f" + code + " §8(" + ms + "ms)"), false);
            conn.disconnect();
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            source.sendSuccess(() -> Component.literal("§c✗ §7" + url + " §c" + e.getClass().getSimpleName() + " §8(" + ms + "ms)"), false);
        }
    }

    private static void probeBackend(CommandSourceStack source, String backendUrl) {
        String url = backendUrl.replaceAll("/+$", "") + "/api/health";
        long t0 = System.currentTimeMillis();
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            long ms = System.currentTimeMillis() - t0;
            String tag = (code == 200) ? "§a✓ backend" : "§e? backend";
            source.sendSuccess(() -> Component.literal(tag + " §7" + url + " §f" + code + " §8(" + ms + "ms)"), false);
            conn.disconnect();
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            source.sendSuccess(() -> Component.literal("§c✗ backend §7" + url + " §c" + e.getClass().getSimpleName() + " §8(" + ms + "ms)"), false);
        }
    }

    private static MutableComponent linkLine(String prefix, String url) {
        Style linkStyle = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§eClick pra abrir")));
        return Component.literal(prefix).append(Component.literal(url).withStyle(linkStyle));
    }

    /** Encontra IP local do servidor (não 127.0.0.1). */
    private static String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(ifs)) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    if (addr.getHostAddress().contains(":")) continue; // skip IPv6
                    return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }
}
