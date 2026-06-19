package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * r81 #13/18 — <b>Astronomical Horror</b>.
 *
 * <p>O cosmos está vivo. À noite, em céu aberto, há chance pequena de
 * gatilhar "DEAD MOON" — todos mobs visíveis pelo player <i>congelam</i>
 * por 5 segundos e viram pro player, simultaneamente. Reload + barra
 * de título "...a lua olha".
 */
public final class AstronomicalSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.ASTRONOMICAL;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 60 != 0) return;

        boolean nightOpenSky = !level.isDay() && level.canSeeSky(sp.blockPosition());
        if (!nightOpenSky) return;

        state.addExposure(HorrorType.ASTRONOMICAL, 0.5F);

        float exp = state.getExposure(HorrorType.ASTRONOMICAL);
        if (exp >= 50
                && state.canFireEvent("dead_moon", gameTime, 12000)  // 10 min cd
                && Math.random() < 0.05) {
            triggerDeadMoon(sp, level);
            state.markEventFired("dead_moon", gameTime);
        }
    }

    private void triggerDeadMoon(ServerPlayer sp, ServerLevel level) {
        // Title
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.literal("§5§l...")));
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                Component.literal("§7§oa lua olha")));
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                10, 100, 20));

        // Mobs viraram pro player + freeze 100t
        var mobs = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                sp.getBoundingBox().inflate(30));
        for (var e : mobs) {
            if (e == sp) continue;
            double dx = sp.getX() - e.getX();
            double dz = sp.getZ() - e.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            e.setYHeadRot(yaw);
            e.setYBodyRot(yaw);
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 250, true, false));
        }

        // Particles: aro de soul fire em volta do player
        for (int i = 0; i < 30; i++) {
            double a = (i / 30.0) * Math.PI * 2;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    sp.getX() + Math.cos(a) * 4,
                    sp.getY() + 8,
                    sp.getZ() + Math.sin(a) * 4,
                    1, 0, 0, 0, 0);
        }
    }
}
