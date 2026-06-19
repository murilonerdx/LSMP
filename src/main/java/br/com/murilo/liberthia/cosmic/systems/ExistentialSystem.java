package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * r81 #3/18 — <b>Existential Horror</b>.
 *
 * <p>Player começa a sentir <b>déjà vu</b>. Ao caminhar por áreas onde já passou,
 * tem um instante de "memória de futuro". Posições visitadas são rastreadas;
 * quando o player revisita um chunk após X minutos, há chance de teleport
 * micro-rollback (1 bloco pra trás) ou som duplicado de passos.
 *
 * <p>Em estado de horror EXISTENTIAL alto, eventos antes ocorridos se repetem —
 * mesmo som de mob exato, mesma sequência de partículas.
 */
public final class ExistentialSystem implements HorrorSystem {

    private final Map<UUID, Map<Long, Long>> chunkVisits = new HashMap<>();
    /** Posição "memória" do último som ouvido. */
    private final Map<UUID, Vec3> lastSoundPos = new HashMap<>();

    @Override
    public HorrorType type() {
        return HorrorType.EXISTENTIAL;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 100 != 0) return;
        UUID id = sp.getUUID();

        // Rastreia chunks visitados
        long chunkKey = ((long)(sp.chunkPosition().x) << 32) | (sp.chunkPosition().z & 0xFFFFFFFFL);
        Map<Long, Long> visits = chunkVisits.computeIfAbsent(id, k -> new HashMap<>());
        Long lastVisit = visits.get(chunkKey);

        if (lastVisit != null && (gameTime - lastVisit) > 6000) {
            // Revisita após 5+ min — déjà vu
            state.addExposure(HorrorType.EXISTENTIAL, 2.0F);
            if (state.getExposure(HorrorType.EXISTENTIAL) > 40 && Math.random() < 0.4) {
                // Micro rollback — empurra player 1 bloco contra direção atual
                Vec3 forward = sp.getLookAngle();
                sp.teleportTo(sp.getX() - forward.x * 0.5, sp.getY(), sp.getZ() - forward.z * 0.5);
                // Echo particle no spot de origem
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        sp.getX(), sp.getY() + 1, sp.getZ(),
                        15, 0.5, 0.8, 0.5, 0.05);
            }
        }
        visits.put(chunkKey, gameTime);

        // Trim antigo (evita map crescer indefinidamente)
        if (visits.size() > 200) {
            long cutoff = gameTime - 24000;
            visits.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        // Decay
        if (isSafe(sp, level)) {
            state.decayExposure(HorrorType.EXISTENTIAL, baseDecayRate());
        }
    }

    private boolean isSafe(ServerPlayer sp, ServerLevel level) {
        return level.dimension().toString().contains("overworld") && level.isDay();
    }
}
