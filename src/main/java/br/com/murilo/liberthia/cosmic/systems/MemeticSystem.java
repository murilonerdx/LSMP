package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorFramework;
import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * r81 #6/18 — <b>Memetic Horror</b> (SCP-style).
 *
 * <p>"The Remembered" — uma entidade que ganha existência conforme players
 * a mencionam em chat. Cada vez que uma das palavras-gatilho aparece em
 * chat (em qualquer player), incrementa um contador global. Quando passar
 * de threshold, todos players ganham exposição MEMETIC e o entity tem
 * chance de "manifestar" (particle silhouette distante).
 *
 * <p>Gatilhos: "remembered", "watcher", "they see", "eyes", "wake up",
 * "static", "signal" (case-insensitive).
 */
public final class MemeticSystem implements HorrorSystem {

    /** Contador global. Sobe quando players mencionam triggers. */
    private static final AtomicInteger GLOBAL_MENTIONS = new AtomicInteger(0);
    /** Limite pra entidade manifestar visualmente. */
    private static final int MANIFEST_THRESHOLD = 5;
    /** Players que já viram a manifestação atual. */
    private static final Set<java.util.UUID> WITNESSED = new HashSet<>();

    private static final String[] TRIGGERS = {
            "remembered", "watcher", "they see", "they watch",
            "eyes", "wake up", "static", "signal", "the entity"
    };

    @Override
    public HorrorType type() {
        return HorrorType.MEMETIC;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 200 != 0) return;
        int count = GLOBAL_MENTIONS.get();

        if (count >= MANIFEST_THRESHOLD) {
            // Player ainda não testemunhou — manifesta agora
            if (!WITNESSED.contains(sp.getUUID())) {
                manifest(sp, level, count);
                WITNESSED.add(sp.getUUID());
                state.addExposure(HorrorType.MEMETIC, 30.0F);
            } else {
                state.addExposure(HorrorType.MEMETIC, 2.0F);
            }
        }
    }

    @Override
    public void onChat(ServerPlayer sp, String message, HorrorState state) {
        String lower = message.toLowerCase();
        for (String trigger : TRIGGERS) {
            if (lower.contains(trigger)) {
                int n = GLOBAL_MENTIONS.incrementAndGet();
                state.addExposure(HorrorType.MEMETIC, 5.0F);
                if (n >= MANIFEST_THRESHOLD - 1) {
                    // Próximo tick, todos podem ver
                    WITNESSED.clear();
                }
                break;
            }
        }
    }

    private void manifest(ServerPlayer sp, ServerLevel level, int strength) {
        // Spawna silhueta de soul particles a ~25 blocos do player
        double a = Math.random() * Math.PI * 2;
        double r = 25;
        double cx = sp.getX() + Math.cos(a) * r;
        double cy = sp.getY() + 1;
        double cz = sp.getZ() + Math.sin(a) * r;
        // Figura humanóide — 8 particles verticais
        for (int i = 0; i < 8; i++) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                    cx, cy + i * 0.3, cz,
                    1, 0.05, 0.05, 0.05, 0);
        }
        // Olho central
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                cx, cy + 1.5, cz, 2, 0.1, 0.1, 0.1, 0);
    }

    @Override
    public void onDeath(ServerPlayer sp, HorrorState state) {
        // Reset witness — pode ver de novo
        WITNESSED.remove(sp.getUUID());
    }

    /** Decai contador global ao longo do tempo (a ser chamado externamente). */
    public static void decayGlobal() {
        if (GLOBAL_MENTIONS.get() > 0) {
            GLOBAL_MENTIONS.decrementAndGet();
        }
    }
}
