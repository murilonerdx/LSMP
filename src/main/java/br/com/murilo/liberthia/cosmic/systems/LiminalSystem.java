package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * r81 #12/18 — <b>Liminal Horror</b>.
 *
 * <p>Fear of empty/transitional spaces. Quando o player está num ambiente
 * "longo, vazio, com luz fluorescente" (corredor sem mob por mais de 30s),
 * a exposição cresce. Ouve sons de passos não-existentes, ambiência seca.
 *
 * <h2>Detecção de "liminal space"</h2>
 * <ul>
 *   <li>Player está dentro de um tubo (paredes ao norte+sul ou leste+oeste)</li>
 *   <li>Sem mobs num raio de 12</li>
 *   <li>Luz baixa-mediana (3-12)</li>
 *   <li>Tempo: streak de 200+ ticks</li>
 * </ul>
 */
public final class LiminalSystem implements HorrorSystem {

    private final java.util.Map<java.util.UUID, Integer> liminalStreak = new java.util.HashMap<>();

    @Override
    public HorrorType type() {
        return HorrorType.LIMINAL;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 20 != 0) return;

        boolean liminal = isLiminalSpace(sp, level);
        int streak;
        if (liminal) {
            streak = liminalStreak.getOrDefault(sp.getUUID(), 0) + 1;
            liminalStreak.put(sp.getUUID(), streak);
        } else {
            liminalStreak.remove(sp.getUUID());
            streak = 0;
        }

        if (streak >= 10) {  // 200+ ticks
            state.addExposure(HorrorType.LIMINAL, 1.0F);

            // Som de passos não-existentes
            if (streak % 5 == 0 && Math.random() < 0.3) {
                var back = sp.position().subtract(sp.getLookAngle().scale(3));
                sp.level().playSound(null,
                        net.minecraft.core.BlockPos.containing(back),
                        SoundEvents.STONE_STEP, SoundSource.HOSTILE,
                        0.6F, 0.8F + (float)Math.random() * 0.4F);
            }
        } else {
            state.decayExposure(HorrorType.LIMINAL, baseDecayRate() * 2);
        }
    }

    private boolean isLiminalSpace(ServerPlayer sp, ServerLevel level) {
        var pos = sp.blockPosition();
        int light = level.getMaxLocalRawBrightness(pos);
        // Luz baixa-média
        if (light < 3 || light > 12) return false;

        // Verifica se há paredes em pelo menos um eixo (norte+sul ou leste+oeste)
        boolean nsWall = !level.getBlockState(pos.north(2)).isAir()
                && !level.getBlockState(pos.south(2)).isAir();
        boolean ewWall = !level.getBlockState(pos.east(2)).isAir()
                && !level.getBlockState(pos.west(2)).isAir();
        if (!nsWall && !ewWall) return false;

        // Sem mobs próximos
        var mobs = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                sp.getBoundingBox().inflate(12));
        long count = mobs.stream().filter(e -> e != sp).count();
        return count == 0;
    }
}
