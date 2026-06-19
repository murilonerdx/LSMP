package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.41: gerencia "boss mode" pra players via comando
 * {@code /liberthia boss on|off}. Quando ativo, mostra uma BossBar global pra
 * todos os players online conectados que reflete a vida do player-boss.
 *
 * <p>Mecânica:
 * <ul>
 *   <li>Ao ativar: cria {@link ServerBossEvent}, adiciona TODOS players online
 *       como viewers (exceto o próprio boss — ele já vê o HUD normal).</li>
 *   <li>Tick periodic (a cada 5 ticks): atualiza progress = currentHealth / maxHealth.</li>
 *   <li>Ao desativar (ou player morre, ou logout): remove a bossbar.</li>
 *   <li>Boss bar colorida em vermelho (BossEvent.BossBarColor.RED) +
 *       progress NOTCHED_10 (10 segmentos pra leitura clara).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PlayerBossFightManager {

    /** Player UUID → ServerBossEvent. */
    private static final Map<UUID, ServerBossEvent> ACTIVE = new HashMap<>();

    private PlayerBossFightManager() {}

    /** Inicia o modo boss pro player. Retorna false se já estava ativo. */
    public static boolean start(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return false;
        Component name = Component.literal("☠ " + player.getName().getString() + " ☠")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        ServerBossEvent bar = new ServerBossEvent(name,
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setProgress(player.getHealth() / player.getMaxHealth());
        // Adiciona TODOS online como viewers
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            bar.addPlayer(p);
        }
        ACTIVE.put(player.getUUID(), bar);
        return true;
    }

    /** Para o modo boss. Idempotente. */
    public static boolean stop(MinecraftServer server, UUID playerUuid) {
        ServerBossEvent bar = ACTIVE.remove(playerUuid);
        if (bar == null) return false;
        bar.removeAllPlayers();
        return true;
    }

    public static boolean isBoss(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }

    /** Tick: atualiza progress de cada boss ativo. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        if (server.getTickCount() % 5 != 0) return;

        Iterator<Map.Entry<UUID, ServerBossEvent>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ServerBossEvent> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) {
                // Player saiu ou morreu — limpa bar
                e.getValue().removeAllPlayers();
                it.remove();
                continue;
            }
            // Atualiza progresso baseado em HP
            float progress = p.getHealth() / p.getMaxHealth();
            e.getValue().setProgress(Math.max(0f, Math.min(1f, progress)));
            // Garante que players que entraram depois vejam a bar
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (!e.getValue().getPlayers().contains(other)) {
                    e.getValue().addPlayer(other);
                }
            }
        }
    }

    /** Death do boss → limpa. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!ACTIVE.containsKey(sp.getUUID())) return;
        stop(sp.server, sp.getUUID());
        sp.displayClientMessage(Component.literal("Boss mode encerrado (morte)")
                .withStyle(ChatFormatting.GRAY), false);
    }

    /** Logout do boss → limpa. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (ACTIVE.containsKey(sp.getUUID())) {
            stop(sp.server, sp.getUUID());
        }
    }
}
