package br.com.murilo.liberthia.cosmic.hallucination;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r55: <b>Low Sanity Hallucination Driver</b> — orquestra
 * efeitos paranóicos quando sanity baixa.
 *
 * <h2>Triggers por threshold</h2>
 * <ul>
 *   <li><b>Sanity ≤ 70</b>: footstep sounds atrás random (5min interval)</li>
 *   <li><b>Sanity ≤ 50</b>: vultos via {@link br.com.murilo.liberthia.cosmic.silhouette.PlayerSilhouetteManager}</li>
 *   <li><b>Sanity ≤ 40</b>: shadow stalker via {@link br.com.murilo.liberthia.cosmic.stalker.ShadowStalkerManager}</li>
 *   <li><b>Sanity ≤ 30</b>: whispers + breathing + particles estranhas</li>
 *   <li><b>Sanity ≤ 20</b>: radio + tendril + reality distortion frequentes</li>
 *   <li><b>Sanity ≤ 10</b>: scream + audience presence constantemente</li>
 * </ul>
 *
 * <p>Intervalos randomizados pra não ficar previsível/chato.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LowSanityHallucinationDriver {

    /** Player UUID → próximo tick em que pode disparar algum efeito. */
    private static final Map<UUID, Long> NEXT_TRIGGER = new HashMap<>();

    private LowSanityHallucinationDriver() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // r112: kill switch — efeitos paranóicos OFF por default
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicHorrorEnabled.get()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return; // 1s rate

        int sanity = SpiritDimension.getSanity(sp);
        boolean inSpirit = SpiritDimension.isInSpiritWorld(sp);

        // r179: criaturas/mobs ENCARANDO FIXAMENTE o player com sanidade baixa (≤40)
        if (sanity <= 40 || inSpirit) {
            makeNearbyMobsStare(sp);
        }

        UUID id = sp.getUUID();
        Long next = NEXT_TRIGGER.get(id);
        if (next != null && sp.tickCount < next) return;

        if (sanity >= 40 && !inSpirit) return; // r183: alucinações só com sanidade < 40%

        // Calcula intervalo entre triggers baseado em sanity
        int baseInterval;
        if (sanity <= 10) baseInterval = 200;       // 10s
        else if (sanity <= 20) baseInterval = 400;  // 20s
        else if (sanity <= 30) baseInterval = 600;  // 30s
        else if (sanity <= 50) baseInterval = 900;  // 45s
        else baseInterval = 1500;                    // 75s
        // Jitter ±50%
        int interval = baseInterval + (int)((Math.random() - 0.5) * baseInterval);

        // Pick effect baseado em sanity
        triggerRandomEffect(sp, sanity, inSpirit);
        NEXT_TRIGGER.put(id, (long)sp.tickCount + interval);
    }

    private static void triggerRandomEffect(ServerPlayer sp, int sanity, boolean inSpirit) {
        try {
            ServerLevel level = (ServerLevel) sp.level();
            int pick = (int)(Math.random() * 100);

            if (sanity <= 10) {
                // Sanity crítica: scream + audience + reality distortion
                if (pick < 25) CosmicSoundManager.playDistantScream(sp);
                else if (pick < 50) CosmicSoundManager.playAudiencePresence(sp);
                else if (pick < 75) CosmicSoundManager.playRealityDistortion(sp);
                else spawnFakeParticles(level, sp, 30);
            } else if (sanity <= 20) {
                if (pick < 30) CosmicSoundManager.playRadioBroadcast(sp);
                else if (pick < 60) CosmicSoundManager.playTendrilMovement(sp);
                else if (pick < 85) CosmicSoundManager.playRealityDistortion(sp);
                else spawnFakeParticles(level, sp, 20);
            } else if (sanity <= 30) {
                if (pick < 30) CosmicSoundManager.playDistantWhispers(sp);
                else if (pick < 55) CosmicSoundManager.playVoidBreathing(sp);
                else if (pick < 80) CosmicSoundManager.playEyePulse(sp);
                else spawnFakeParticles(level, sp, 10);
            } else if (sanity <= 50) {
                if (pick < 50) CosmicSoundManager.playFalseFootsteps(sp, Math.random() < 0.5);
                else CosmicSoundManager.playDistantWhispers(sp);
            } else {
                // 50-70: só footsteps raros
                if (pick < 70) CosmicSoundManager.playFalseFootsteps(sp, true);
            }
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[Hallucination] driver error: {}", t.toString());
        }
    }

    /**
     * r179: faz os mobs num raio de 20b ENCARAREM o player (cabeça virada pra ele).
     * O clássico "tudo está te olhando" da insanidade. Roda 1×/s com sanidade ≤ 40.
     */
    private static void makeNearbyMobsStare(ServerPlayer sp) {
        var box = sp.getBoundingBox().inflate(20.0);
        for (net.minecraft.world.entity.Mob mob : sp.level().getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class, box,
                m -> m.isAlive() && m.distanceToSqr(sp) > 1.0)) {
            mob.getLookControl().setLookAt(sp, 60F, 60F);
            double dx = sp.getX() - mob.getX();
            double dz = sp.getZ() - mob.getZ();
            float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
        }
    }

    private static void spawnFakeParticles(ServerLevel level, ServerPlayer sp, int count) {
        // Spawn particles random a 3-8 blocos em volta do player (não muito perto)
        for (int i = 0; i < count; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 3 + Math.random() * 5;
            double dx = Math.cos(a) * r;
            double dz = Math.sin(a) * r;
            double dy = Math.random() * 2;
            // Tipo random
            int type = (int)(Math.random() * 3);
            switch (type) {
                case 0 -> level.sendParticles(ParticleTypes.SOUL,
                        sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz, 1, 0, 0, 0, 0);
                case 1 -> level.sendParticles(ParticleTypes.SMOKE,
                        sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz, 1, 0, 0, 0, 0);
                case 2 -> level.sendParticles(ParticleTypes.SQUID_INK,
                        sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz, 1, 0, 0, 0, 0);
            }
        }
    }
}
