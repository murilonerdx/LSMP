package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * r81 #17/18 — <b>Parasitic Horror</b>.
 *
 * <p>Infecção inteligente. Quando o player tem >=60 de exposure PARASITIC,
 * sente "raízes" — slowness aplicada SE o player ficar parado por mais
 * de 5 segundos. Tem que continuar se movendo pra escapar.
 *
 * <p>Trigger: estar perto de mobs infected (zombies, spiders, husks),
 * comer carne crua, ou em biomas swamp/mushroom.
 */
public final class ParasiticSystem implements HorrorSystem {

    private final java.util.Map<java.util.UUID, Integer> stillTicks = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> lastPos = new java.util.HashMap<>();

    @Override
    public HorrorType type() {
        return HorrorType.PARASITIC;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        java.util.UUID id = sp.getUUID();
        var pos = sp.position();

        // Tracking de quietude
        var prev = lastPos.get(id);
        if (prev != null && prev.distanceTo(pos) < 0.1) {
            int t = stillTicks.getOrDefault(id, 0) + 1;
            stillTicks.put(id, t);
        } else {
            stillTicks.put(id, 0);
        }
        lastPos.put(id, pos);

        // Detecta mobs infect próximos (zombies, husks, spider)
        if (gameTime % 60 == 0) {
            var infMobs = level.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class,
                    sp.getBoundingBox().inflate(8));
            int infected = 0;
            for (var e : infMobs) {
                String name = e.getType().toString().toLowerCase();
                if (name.contains("zombie") || name.contains("husk")
                        || name.contains("spider") || name.contains("infected")) {
                    infected++;
                }
            }
            if (infected > 0) {
                state.addExposure(HorrorType.PARASITIC, infected * 0.5F);
            }
        }

        float exp = state.getExposure(HorrorType.PARASITIC);
        int still = stillTicks.getOrDefault(id, 0);

        // 60+: parado mais de 100 ticks ⇒ slowness "raízes"
        if (exp >= 60 && still >= 100 && gameTime % 40 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, true, false));
            // Particle "raiz" subindo pelo player
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.MYCELIUM,
                    sp.getX(), sp.getY(), sp.getZ(),
                    8, 0.3, 0.5, 0.3, 0.02);
        }

        // Decay (se mexendo + não infectado próximo)
        if (still < 20 && gameTime % 100 == 0) {
            state.decayExposure(HorrorType.PARASITIC, baseDecayRate());
        }
    }
}
