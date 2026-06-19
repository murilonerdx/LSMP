package br.com.murilo.liberthia.magic.spell.voidspell;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.152 r120: <b>VoidLaserTracker</b> — gerencia o estado do feitiço
 * canalizado "void_laser".
 *
 * <p>Cada cast adiciona +250 ao stack do player. Stack reseta após {@code RESET_TICKS}
 * sem cast (decai). Stack cap em {@link #MAX_STACK} = 5000.
 *
 * <p>Server-side singleton (Map UUID → Stack).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class VoidLaserTracker {

    /** Damage adicionado por cada cast individual do laser. */
    public static final int DAMAGE_PER_TICK = 250;
    /** Máximo absoluto do stack do laser (cap declarado pelo usuário). */
    public static final int MAX_STACK = 5000;
    /** Quantos ticks sem castar antes de reset. */
    public static final int RESET_TICKS = 10;

    private static final Map<UUID, Stack> STACKS = new HashMap<>();

    private VoidLaserTracker() {}

    public static final class Stack {
        public int damage;
        public long lastCastTick;
        Stack(int dmg, long t) { this.damage = dmg; this.lastCastTick = t; }
    }

    /** Adiciona um stack. Retorna o stack atual (cap aplicado). */
    public static int addStack(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Stack s = STACKS.computeIfAbsent(player.getUUID(), k -> new Stack(0, now));
        // Se decay passou, reseta
        if (now - s.lastCastTick > RESET_TICKS) {
            s.damage = 0;
        }
        s.damage = Math.min(MAX_STACK, s.damage + DAMAGE_PER_TICK);
        s.lastCastTick = now;
        return s.damage;
    }

    public static int getStack(ServerPlayer player) {
        Stack s = STACKS.get(player.getUUID());
        if (s == null) return 0;
        long now = player.serverLevel().getGameTime();
        if (now - s.lastCastTick > RESET_TICKS) return 0;
        return s.damage;
    }

    public static void clear(ServerPlayer player) {
        STACKS.remove(player.getUUID());
    }

    /** Limpa stacks expirados a cada N ticks pra evitar memory leak. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (e.getServer().getTickCount() % 60 != 0) return;
        long now = e.getServer().overworld().getGameTime();
        STACKS.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().lastCastTick;
            return age > 200; // 10 segundos de inatividade
        });
    }

    /**
     * r137 fix #12: limpa o stack quando player morre/respawna/desloga.
     * Sem isso, player com 5000 damage stack persistia entre morte/respawn
     * e podia continuar a one-shotar bosses logo apos voltar.
     */
    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            clear(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            clear(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            clear(sp);
        }
    }
}
