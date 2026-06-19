package br.com.murilo.liberthia.magic.spell.voidspell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * v0.1.152 r120: <b>VoidInfectionEffect</b> — efeito de infecção do Vazio.
 *
 * <p>Mecânicas:
 * <ul>
 *   <li>Drena 1 HP por segundo (20 ticks)</li>
 *   <li>Emite partículas roxas continuamente do alvo</li>
 *   <li>A cada 60 ticks (3s), spawna uma larva voidica que ataca o alvo</li>
 *   <li>Tela do afetado fica com efeito roxo (gerenciado via HUD overlay client)</li>
 *   <li>Durante 10 segundos (200 ticks por default)</li>
 * </ul>
 *
 * <p>Quem está infectado é marcado via persistent data {@code liberthia.void_infected_until}
 * pra que o HUD client e larva AI possam consultar sem depender só do MobEffectInstance.
 */
public class VoidInfectionEffect extends MobEffect {

    public static final String NBT_INFECTED_UNTIL = "liberthia.void_infected_until";
    public static final int DRAIN_INTERVAL_TICKS = 20;     // 1 hp/s
    public static final int LARVA_SPAWN_INTERVAL = 60;     // 1 larva a cada 3s
    public static final float TICK_DAMAGE = 1.0F;

    public VoidInfectionEffect() {
        super(MobEffectCategory.HARMFUL, 0x8800CC); // roxo intenso
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // executa applyEffectTick em todo tick (precisamos pra particles smoothly)
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity living, int amplifier) {
        if (living.level().isClientSide) return;
        if (!(living.level() instanceof ServerLevel sl)) return;

        // Marca tempo de expiração via NBT (HUD client lê isso)
        var inst = living.getEffect(this);
        if (inst == null) return;  // r137: defensive — efeito pode ter saido
        long endTime = sl.getGameTime() + inst.getDuration();
        living.getPersistentData().putLong(NBT_INFECTED_UNTIL, endTime);

        // r137 fix #5: usa duration restante (relativo ao efeito) ao invés de gameTime absoluto.
        // Antes podia nunca disparar nos primeiros 20 ticks do mundo OU pular timings.
        int duration = inst.getDuration();

        // Damage tick — a cada 20 ticks da duracao
        if (duration % DRAIN_INTERVAL_TICKS == 0) {
            DamageSource ds = living.damageSources().magic();
            living.hurt(ds, TICK_DAMAGE * (amplifier + 1));
        }

        // Particles — sempre que tick. ~3 partículas/tick
        for (int i = 0; i < 3; i++) {
            double ox = (living.getRandom().nextDouble() - 0.5) * living.getBbWidth();
            double oy = living.getRandom().nextDouble() * living.getBbHeight();
            double oz = (living.getRandom().nextDouble() - 0.5) * living.getBbWidth();
            sl.sendParticles(
                    br.com.murilo.liberthia.registry.ModParticles.VOID_INFECTION.get(),
                    living.getX() + ox,
                    living.getY() + oy,
                    living.getZ() + oz,
                    1, 0.02, 0.05, 0.02, 0.02);
        }

        // Larva spawn — a cada 60 ticks da duração
        // (guard duration > 5 evita spawn imediato no momento de aplicacao)
        if (duration % LARVA_SPAWN_INTERVAL == 0 && duration < (200 - 5)) {
            spawnLarva(sl, living);
        }
    }

    private void spawnLarva(ServerLevel sl, LivingEntity target) {
        try {
            VoidLarvaEntity larva = br.com.murilo.liberthia.registry.ModEntities.VOID_LARVA.get().create(sl);
            if (larva == null) return;
            // Spawn 2-3 blocos atrás do alvo
            double angle = sl.random.nextDouble() * Math.PI * 2;
            double dist = 2.5;
            larva.moveTo(target.getX() + Math.cos(angle) * dist,
                         target.getY(),
                         target.getZ() + Math.sin(angle) * dist,
                         sl.random.nextFloat() * 360F, 0F);
            larva.setTargetUUID(target.getUUID());
            larva.setLifespan(200); // 10 segundos
            sl.addFreshEntity(larva);
            // VFX de spawn
            sl.sendParticles(
                    br.com.murilo.liberthia.registry.ModParticles.VOID_INFECTION.get(),
                    larva.getX(), larva.getY() + 0.3, larva.getZ(),
                    25, 0.4, 0.3, 0.4, 0.2);
        } catch (Throwable t) {
            // soft fail se entity não registrada
        }
    }

    /** Helper: check se um LivingEntity está infectado AGORA (consulta NBT, mais barato). */
    public static boolean isInfected(LivingEntity le) {
        long until = le.getPersistentData().getLong(NBT_INFECTED_UNTIL);
        if (until <= 0) return false;
        return le.level().getGameTime() < until;
    }
}
