package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * r81 #10/18 — <b>Religious / Angelic Horror</b>.
 *
 * <p>Anjos biblicamente incompreensíveis. Quando o player está em céu aberto
 * (sky exposed) por muito tempo durante a noite, particles END_ROD começam
 * a girar acima dele. Em exposure alta, ouve coral distante e tem blindness
 * brief.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Céu noturno aberto (canSeeSky + !isDay): +0.2 exposure/tick</li>
 *   <li>Em 40+: anel de particles END_ROD acima do player</li>
 *   <li>Em 70+: blindness + chime sound</li>
 * </ul>
 */
public final class ReligiousSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.RELIGIOUS;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        boolean openSky = level.canSeeSky(sp.blockPosition());
        boolean night = !level.isDay();

        if (openSky && night && gameTime % 20 == 0) {
            state.addExposure(HorrorType.RELIGIOUS, 0.2F);
        }

        float exp = state.getExposure(HorrorType.RELIGIOUS);

        // Anel de olhos angelicais — END_ROD orbitando
        if (exp >= 40 && gameTime % 10 == 0) {
            double angle = (gameTime / 5.0) % (Math.PI * 2);
            for (int i = 0; i < 6; i++) {
                double a = angle + (i / 6.0) * Math.PI * 2;
                double rx = sp.getX() + Math.cos(a) * 3;
                double ry = sp.getY() + 4 + Math.sin(gameTime * 0.05) * 0.3;
                double rz = sp.getZ() + Math.sin(a) * 3;
                level.sendParticles(ParticleTypes.END_ROD, rx, ry, rz, 1, 0, 0, 0, 0);
            }
        }

        // Coral + blindness em 70+
        if (exp >= 70 && state.canFireEvent("angel_chime", gameTime, 1200)) {
            sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(),
                    net.minecraft.sounds.SoundSource.AMBIENT, 1.2F, 0.3F);
            sp.displayClientMessage(Component.literal(
                    "§e§o✦ um coral incompreensível ressoa..."), true);
            state.markEventFired("angel_chime", gameTime);
        }

        // Decay
        if (level.isDay() && gameTime % 100 == 0) {
            state.decayExposure(HorrorType.RELIGIOUS, baseDecayRate());
        }
    }
}
