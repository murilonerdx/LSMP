package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * <b>Loucura = Poder.</b> Quanto MENOR a sanidade do atacante, mais forte —
 * e mais INSTÁVEL — fica o golpe dele:
 *
 * <ul>
 *   <li>Abaixo de 50% de sanidade, o dano que ele causa escala (até +150%).</li>
 *   <li>Na loucura profunda, o golpe vira uma EXPLOSÃO de instabilidade: respinga
 *       dano em TODOS ao redor da vítima (inclusive aliados — "mata muita gente
 *       em área").</li>
 *   <li>No auge (loucura &gt; 80%), há chance de <b>backfire</b>: o próprio
 *       louco se fere.</li>
 * </ul>
 *
 * <p>Sem recursão: o dano em área usa fonte mágica genérica (sem entidade), então
 * não re-dispara a amplificação. Event-driven e raro (só com sanidade baixa) →
 * custo desprezível.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MadnessPowerHandler {

    private MadnessPowerHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        var src = event.getSource();
        if (src == null) return;
        if (!(src.getEntity() instanceof ServerPlayer attacker)) return;

        int sanity = SpiritDimension.getSanity(attacker);
        int max = SpiritDimension.MAX_SANITY;
        float half = max * 0.5f;
        if (sanity >= half) return; // só com loucura instalada

        // 0 (no limiar de 50%) → 1 (sanidade zero)
        float madness = Mth.clamp((half - sanity) / half, 0f, 1f);

        // 1) dano amplificado (até 2.5x no fundo do poço)
        float amplified = event.getAmount() * (1.0f + madness * 1.5f);
        event.setAmount(amplified);

        // 2) instabilidade: AoE a partir de ~40% de loucura
        LivingEntity victim = event.getEntity();
        if (madness < 0.4f || !(victim.level() instanceof ServerLevel sl)) return;

        double radius = 2.0 + madness * 4.0;                 // até ~6 blocos
        float splash = amplified * (0.30f + madness * 0.40f); // fração do golpe
        for (LivingEntity near : sl.getEntitiesOfClass(LivingEntity.class,
                victim.getBoundingBox().inflate(radius))) {
            if (near == victim) continue;
            // fonte mágica SEM entidade → não re-amplifica (sem recursão)
            near.hurt(near.damageSources().magic(), splash);
        }
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                victim.getX(), victim.getY() + 1.0, victim.getZ(),
                (int) (10 + madness * 25), radius * 0.3, 0.5, radius * 0.3, 0.05);

        // 3) backfire no auge da loucura
        if (madness > 0.8f && attacker.getRandom().nextFloat() < 0.25f) {
            attacker.hurt(attacker.damageSources().magic(), splash * 0.5f);
        }
    }
}
