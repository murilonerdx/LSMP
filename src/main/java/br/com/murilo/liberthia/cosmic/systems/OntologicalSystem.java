package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * r81 #4/18 — <b>Ontological Horror</b>.
 *
 * <p>A realidade física começa a falhar. Blocos atrás do player se movem
 * sutilmente (particles deles mas não a fisica). Player ouve sons de bloco
 * quebrando sem haver quebra. Sem dano real — só visual/audio overlap.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>A cada 80 ticks (4s), 20% de chance de spawnar block-break particles
 *       num bloco aleatório atrás do player</li>
 *   <li>Sem dano, sem mudança de bloco — só ILUSÃO de instabilidade</li>
 *   <li>Adiciona exposição lenta enquanto o player está em estruturas
 *       (cave, edifícios)</li>
 * </ul>
 */
public final class OntologicalSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.ONTOLOGICAL;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 80 != 0) return;

        // Detecta se está em estrutura confinada (cave/edifício) — checa luz baixa
        BlockPos pos = sp.blockPosition();
        int light = level.getMaxLocalRawBrightness(pos);
        boolean confined = light < 7;

        if (confined) {
            state.addExposure(HorrorType.ONTOLOGICAL, 1.0F);
        }

        // Block phantom flicker — particle de bloco quebrando sem quebrar
        if (state.getExposure(HorrorType.ONTOLOGICAL) > 25 && Math.random() < 0.20) {
            phantomBlockBreak(sp, level);
        }

        // Decay
        if (level.isDay() && !confined) {
            state.decayExposure(HorrorType.ONTOLOGICAL, baseDecayRate());
        }
    }

    private void phantomBlockBreak(ServerPlayer sp, ServerLevel level) {
        // Atrás/lateral do player, 5-10 blocos
        double a = Math.random() * Math.PI * 2;
        double r = 5 + Math.random() * 5;
        int bx = (int)(sp.getX() + Math.cos(a) * r);
        int by = (int)sp.getY();
        int bz = (int)(sp.getZ() + Math.sin(a) * r);

        BlockPos pos = new BlockPos(bx, by, bz);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        // Spawna particles como se fosse uma quebra de bloco — mas NÃO quebra
        BlockParticleOption opt = new BlockParticleOption(ParticleTypes.BLOCK, state);
        level.sendParticles(opt, bx + 0.5, by + 0.5, bz + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
    }
}
