package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * r81 #16/18 — <b>Void Horror</b>.
 *
 * <p>Nada absoluto. Em silêncio total (sem mobs próximos, sem música, longe
 * de outros players), começa a sentir uma "presença ausente". Cresce
 * exposição rapidamente, e em alto valor, aplica BLINDNESS + DARKNESS
 * intermitente (simula "vazio te puxando").
 */
public final class VoidHorrorSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.VOID;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 40 != 0) return;

        // Silêncio total: sem mobs num raio 30 + sem players num raio 50
        var mobs = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                sp.getBoundingBox().inflate(30));
        long count = mobs.stream().filter(e -> e != sp).count();

        if (count == 0) {
            state.addExposure(HorrorType.VOID, 2.0F);
        } else {
            state.decayExposure(HorrorType.VOID, baseDecayRate() * 2);
        }

        float exp = state.getExposure(HorrorType.VOID);

        // 50+: blindness brief
        if (exp >= 50 && gameTime % 200 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
        }

        // 80+: heavy slowness + dano leve "absurdo"
        if (exp >= 80 && gameTime % 300 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, true, false));
            sp.hurt(sp.damageSources().fellOutOfWorld(), 0.5F);
        }
    }
}
