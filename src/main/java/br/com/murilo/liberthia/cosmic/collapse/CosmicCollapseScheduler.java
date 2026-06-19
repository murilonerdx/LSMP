package br.com.murilo.liberthia.cosmic.collapse;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicHorrorManager;
import br.com.murilo.liberthia.cosmic.CosmicHorrorPhase;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r41: <b>Cosmic Collapse Scheduler</b> — executor server-side da
 * skill "Cosmic Collapse" do SKILL.md. 6 phases cinematográficas ao longo
 * de ~14 segundos.
 *
 * <h2>Phase timeline (ticks)</h2>
 * <pre>
 * Phase 1 — PRE-SUMMONING       (0-40t, 2s)  caster levita, glyphs orbitam
 * Phase 2 — REALITY INSTABILITY (40-100t, 3s) chromatic + cracks aparecem
 * Phase 3 — DIMENSIONAL RUPTURE (100-180t, 4s) céu rasga, tentacles, ash
 * Phase 4 — ENTITY MANIFESTATION (180-240t, 3s) entidade gigante
 * Phase 5 — COSMIC COLLAPSE     (240t, instant) implosão + damage
 * Phase 6 — POST CORRUPTION     (240-400t, 8s) corruption fica no terreno
 * </pre>
 *
 * <h2>Arquitetura</h2>
 * Por player caster, mantém uma {@link CollapseSession} com center coords,
 * tickStart, e current phase. Server tick avança o timer e executa eventos
 * específicos de cada phase via {@link #tickSession}.
 *
 * <h2>Particle/Sound choreography</h2>
 * Cada phase emite combos de partículas específicas + sons cuidadosamente
 * timed. Hooks na rede pra trigger client-side overlays (shake, glitch).
 *
 * <h2>Damage</h2>
 * Damage aplicado APENAS na Phase 5 (collapse explosion). Todos os entities
 * num raio 12 sofrem dano cósmico + sanity loss + hallucinations.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CosmicCollapseScheduler {

    /** Marcos das phases em ticks. */
    public static final int PHASE_1_END = 40;    // 2s pre-summoning
    public static final int PHASE_2_END = 100;   // 3s reality instability
    public static final int PHASE_3_END = 180;   // 4s dimensional rupture
    public static final int PHASE_4_END = 240;   // 3s entity manifestation
    public static final int PHASE_5_END = 244;   // instant explosion
    public static final int PHASE_6_END = 400;   // 8s post corruption
    public static final int TOTAL_TICKS = PHASE_6_END;

    /** Damage da implosion (phase 5). */
    public static final float COLLAPSE_DAMAGE = 24.0F;
    /** Raio de damage da implosion. */
    public static final float COLLAPSE_RADIUS = 12.0F;

    /** Sessions ativas — UUID do caster → estado. */
    private static final java.util.Map<UUID, CollapseSession> SESSIONS =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static class CollapseSession {
        public final UUID casterId;
        public final Vec3 center;
        public final long startTick;
        public int currentPhase = 1;
        /** Cores derivadas de InsanityData — quanto mais cosmic influence, mais "vermelho-pálido". */
        public final Vector3f color;
        /** Tilt aleatório do plano orbital pra randomizar a aparência. */
        public final float orbitTiltX;
        public final float orbitTiltZ;

        public CollapseSession(UUID casterId, Vec3 center, long now, Vector3f color) {
            this.casterId = casterId;
            this.center = center;
            this.startTick = now;
            this.color = color;
            this.orbitTiltX = (float)(Math.random() - 0.5) * 0.4F;
            this.orbitTiltZ = (float)(Math.random() - 0.5) * 0.4F;
        }
    }

    private CosmicCollapseScheduler() {}

    // ────────── API ──────────

    /**
     * Inicia o ritual de Cosmic Collapse no centro especificado.
     * Cria session, agenda phase 1.
     */
    public static void startRitual(ServerPlayer caster, Vec3 center) {
        ServerLevel level = caster.serverLevel();
        long now = level.getGameTime();

        // Cor base — purple-violet shifting com cosmic influence
        float cosmicBoost = InsanityData.getCosmicInfluence(caster) / 100.0F;
        Vector3f color = new Vector3f(
                0.45F + cosmicBoost * 0.25F,  // mais vermelho com cosmic
                0.10F + cosmicBoost * 0.10F,
                0.85F - cosmicBoost * 0.20F);

        CollapseSession session = new CollapseSession(caster.getUUID(), center, now, color);
        SESSIONS.put(caster.getUUID(), session);

        // Cosmic horror também sincroniza no caster (shaders + camera shake)
        CosmicHorrorManager.setPhase(caster, CosmicHorrorPhase.SUBTLE_PRESENCE);

        // Initial cast feedback
        caster.displayClientMessage(Component.literal(
                "§5§l✦ §r§5§oVocê chama o que não devia. A realidade começa a ceder..."), false);
        level.playSound(null, BlockPos.containing(center),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.5F, 0.3F);

        // Knowledge cost
        InsanityData.addForbiddenKnowledge(caster, 10);
        InsanityData.addObsession(caster, 8);
        InsanityData.addInsanity(caster, 15);

        LiberthiaMod.LOGGER.info("[CosmicCollapse] {} initiated ritual at {}",
                caster.getName().getString(), center);
    }

    // ────────── Tick hook ──────────

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (SESSIONS.isEmpty()) return;

        var iter = SESSIONS.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            UUID uuid = entry.getKey();
            CollapseSession s = entry.getValue();

            ServerPlayer caster = event.getServer().getPlayerList().getPlayer(uuid);
            if (caster == null) { iter.remove(); continue; }
            if (!(caster.level() instanceof ServerLevel)) { iter.remove(); continue; }

            ServerLevel level = caster.serverLevel();
            long elapsed = level.getGameTime() - s.startTick;

            try {
                tickSession(caster, level, s, elapsed);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[CosmicCollapse] session error: {}", t.toString());
            }

            if (elapsed >= TOTAL_TICKS) {
                iter.remove();
                CosmicHorrorManager.reset(caster);
                caster.displayClientMessage(Component.literal(
                        "§7§o*o silêncio retorna*"), true);
            }
        }
    }

    // ────────── PHASE EXECUTION ──────────

    private static void tickSession(ServerPlayer caster, ServerLevel level,
                                     CollapseSession s, long elapsed) {
        // Switch phase based on elapsed
        int phase = phaseFromElapsed(elapsed);
        if (phase != s.currentPhase) {
            s.currentPhase = phase;
            onPhaseEnter(caster, level, s, phase);
        }

        // Per-phase per-tick effects
        switch (phase) {
            case 1 -> tickPhase1(caster, level, s, elapsed);
            case 2 -> tickPhase2(caster, level, s, elapsed);
            case 3 -> tickPhase3(caster, level, s, elapsed);
            case 4 -> tickPhase4(caster, level, s, elapsed);
            case 5 -> tickPhase5(caster, level, s, elapsed);
            case 6 -> tickPhase6(caster, level, s, elapsed);
        }
    }

    private static int phaseFromElapsed(long t) {
        if (t < PHASE_1_END) return 1;
        if (t < PHASE_2_END) return 2;
        if (t < PHASE_3_END) return 3;
        if (t < PHASE_4_END) return 4;
        if (t < PHASE_5_END) return 5;
        return 6;
    }

    private static void onPhaseEnter(ServerPlayer caster, ServerLevel level,
                                      CollapseSession s, int phase) {
        switch (phase) {
            case 2 -> {
                CosmicHorrorManager.setPhase(caster, CosmicHorrorPhase.DIMENSIONAL_CORRUPTION);
                level.playSound(null, BlockPos.containing(s.center),
                        SoundEvents.WARDEN_AMBIENT, SoundSource.AMBIENT, 2.5F, 0.4F);
                HallucinationManager.force(caster, HallucinationType.SCREEN_GLITCH_BURST,
                        0.5F, 30, "");
            }
            case 3 -> {
                CosmicHorrorManager.setPhase(caster, CosmicHorrorPhase.REALITY_RUPTURE);
                caster.displayClientMessage(Component.literal(
                        "§4§l§o✦ O VÉU SE RASGA ✦"), false);
                level.playSound(null, BlockPos.containing(s.center),
                        SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 3.5F, 0.5F);
                // Lightning visual at center
                spawnLightning(level, s.center);
            }
            case 4 -> {
                CosmicHorrorManager.setPhase(caster, CosmicHorrorPhase.ENTITY_MANIFESTATION);
                caster.displayClientMessage(Component.literal(
                        "§4§l§o✦ ELE OLHA DE VOLTA ✦"), false);
                level.playSound(null, BlockPos.containing(s.center),
                        SoundEvents.WARDEN_ROAR, SoundSource.AMBIENT, 4.0F, 0.4F);
                // Hallucination to all nearby players (not just caster)
                for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class,
                        new net.minecraft.world.phys.AABB(s.center, s.center).inflate(32))) {
                    HallucinationManager.force(near, HallucinationType.FAKE_ENTITY_PERIPHERAL,
                            1.0F, 80, "");
                    HallucinationManager.force(near, HallucinationType.REVERSE_AUDIO_PULSE,
                            0.8F, 60, "");
                }
            }
            case 5 -> {
                // SILENCE pre-explosion
                level.playSound(null, BlockPos.containing(s.center),
                        SoundEvents.PORTAL_TRIGGER, SoundSource.AMBIENT, 0.4F, 0.1F);
                triggerCollapseExplosion(caster, level, s);
            }
            case 6 -> {
                level.playSound(null, BlockPos.containing(s.center),
                        SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.5F, 0.5F);
            }
        }
    }

    // ────────── PHASE 1 — PRE-SUMMONING (2s) ──────────

    private static void tickPhase1(ServerPlayer caster, ServerLevel level,
                                    CollapseSession s, long t) {
        // Caster levita
        if (t == 0) {
            caster.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, false));
            caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 4, false, false));
        }

        // Glyphs orbiting around caster
        if (t % 3 == 0) {
            for (int i = 0; i < 4; i++) {
                double angle = (t * 0.15 + i * Math.PI / 2);
                double radius = 1.8;
                double gx = caster.getX() + Math.cos(angle) * radius;
                double gy = caster.getY() + 0.5 + Math.sin(t * 0.1 + i) * 0.3;
                double gz = caster.getZ() + Math.sin(angle) * radius;
                // Orbital debris particle
                level.sendParticles(ModParticles.ORBITAL_DEBRIS.get(), gx, gy, gz, 1,
                        2.0, 1.5, 0.3, 0);
            }
        }

        // Ambient darkness pulse (camera shake light)
        if (t % 10 == 0) {
            DustParticleOptions dust = new DustParticleOptions(s.color, 1.4F);
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = 1 + Math.random() * 2;
                level.sendParticles(dust,
                        caster.getX() + Math.cos(a) * d,
                        caster.getY() + Math.random() * 2.5,
                        caster.getZ() + Math.sin(a) * d,
                        2, 0.1, 0.1, 0.1, 0);
            }
        }

        // Floating debris on ground (gravitational influence visible)
        if (t == 20) {
            for (int i = 0; i < 16; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = 2 + Math.random() * 3;
                level.sendParticles(ParticleTypes.POOF,
                        s.center.x + Math.cos(a) * d,
                        s.center.y - 0.5,
                        s.center.z + Math.sin(a) * d,
                        1, 0, 0.05, 0, 0.02);
            }
        }

        // Low frequency cosmic drone every 1s
        if (t % 20 == 0) {
            level.playSound(null, caster.blockPosition(),
                    SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.8F, 0.3F);
        }
    }

    // ────────── PHASE 2 — REALITY INSTABILITY (3s) ──────────

    private static void tickPhase2(ServerPlayer caster, ServerLevel level,
                                    CollapseSession s, long t) {
        long phaseT = t - PHASE_1_END;

        // Dimensional cracks spawning in air around center
        if (phaseT % 8 == 0) {
            for (int i = 0; i < 2; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = 3 + Math.random() * 5;
                double cx = s.center.x + Math.cos(a) * d;
                double cy = s.center.y + 1 + Math.random() * 4;
                double cz = s.center.z + Math.sin(a) * d;
                // vy = max scale 1.5-3.0
                level.sendParticles(ModParticles.DIMENSIONAL_CRACK.get(),
                        cx, cy, cz, 1,
                        0, 1.5 + Math.random() * 1.5, 0, 0);
            }
        }

        // Floating eyes (Soul Fire Flame clusters at random angles)
        if (phaseT % 25 == 0) {
            double a = Math.random() * Math.PI * 2;
            double d = 4 + Math.random() * 3;
            double ex = s.center.x + Math.cos(a) * d;
            double ey = s.center.y + 1.5 + Math.random() * 2;
            double ez = s.center.z + Math.sin(a) * d;
            // 2 pixels eyes
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ex - 0.05, ey, ez, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ex + 0.05, ey, ez, 1, 0, 0, 0, 0);
        }

        // Velocity-distorted dust
        if (phaseT % 4 == 0) {
            DustParticleOptions dust = new DustParticleOptions(s.color, 1.6F);
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = 5 + Math.random() * 3;
                level.sendParticles(dust,
                        s.center.x + Math.cos(a) * d,
                        s.center.y + 0.5 + Math.random() * 5,
                        s.center.z + Math.sin(a) * d,
                        1, 0.05, 0.05, 0.05, 0);
            }
        }

        // Hallucination tickling — phase glitch micros pro caster + nearby
        if (phaseT % 30 == 0) {
            HallucinationManager.force(caster, HallucinationType.SHADOW_MOVEMENT, 0.6F, 20, "");
        }
    }

    // ────────── PHASE 3 — DIMENSIONAL RUPTURE (4s) ──────────

    private static void tickPhase3(ServerPlayer caster, ServerLevel level,
                                    CollapseSession s, long t) {
        long phaseT = t - PHASE_2_END;

        // Massive rupture in sky directly above center
        double skyY = s.center.y + 30;
        // Cosmic orbit ring at sky
        if (phaseT % 3 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = (phaseT * 0.05 + i * Math.PI / 4);
                double r = 4 + Math.sin(phaseT * 0.05) * 2;
                level.sendParticles(ModParticles.COSMIC_ORBIT.get(),
                        s.center.x + Math.cos(a) * r,
                        skyY + Math.sin(phaseT * 0.08 + i) * 1.5,
                        s.center.z + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }
        }

        // Inverse gravity debris (sucked upward toward rupture)
        if (phaseT % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = 1 + Math.random() * 6;
                double px = s.center.x + Math.cos(a) * d;
                double pz = s.center.z + Math.sin(a) * d;
                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                        px, s.center.y + Math.random() * 3, pz,
                        1, 0, 0.3, 0, 0.08);
            }
        }

        // Tentacle silhouettes — VOID_LEAK particles falling from rupture
        if (phaseT % 8 == 0) {
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = Math.random() * 5;
                double px = s.center.x + Math.cos(a) * d;
                double pz = s.center.z + Math.sin(a) * d;
                level.sendParticles(ModParticles.VOID_LEAK.get(),
                        px, skyY - Math.random() * 10, pz,
                        1, 0, -0.2, 0, 0.05);
            }
        }

        // Red lightning arcs
        if (phaseT % 25 == 0) {
            spawnLightning(level, s.center.add(
                    (Math.random() - 0.5) * 10,
                    Math.random() * 3,
                    (Math.random() - 0.5) * 10));
        }

        // Orbital debris growing — bigger orbits
        if (phaseT % 6 == 0) {
            for (int i = 0; i < 4; i++) {
                double angle = Math.random() * Math.PI * 2;
                double semi = 5 + Math.random() * 3;
                level.sendParticles(ModParticles.ORBITAL_DEBRIS.get(),
                        s.center.x, s.center.y + 1 + Math.random() * 3, s.center.z,
                        1,
                        semi,
                        semi * 0.6,
                        Math.random() * 0.6 - 0.3,
                        0);
            }
        }

        // Sound — sustained roar
        if (phaseT == 0 || phaseT % 40 == 0) {
            level.playSound(null, BlockPos.containing(s.center),
                    SoundEvents.WITHER_AMBIENT, SoundSource.AMBIENT, 2.5F, 0.4F);
        }
    }

    // ────────── PHASE 4 — ENTITY MANIFESTATION (3s) ──────────

    private static void tickPhase4(ServerPlayer caster, ServerLevel level,
                                    CollapseSession s, long t) {
        long phaseT = t - PHASE_3_END;

        // Massive entity at sky — multiple "eyes" SOUL_FIRE_FLAME clusters
        if (phaseT % 4 == 0) {
            double skyY = s.center.y + 25;
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 2 + Math.random() * 4;
                double ex = s.center.x + Math.cos(a) * r;
                double ey = skyY + Math.random() * 4 - 2;
                double ez = s.center.z + Math.sin(a) * r;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ex, ey, ez, 1, 0, 0, 0, 0);
            }
        }

        // Gravity singularity particles spawning around player — being drawn IN
        if (phaseT % 4 == 0) {
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * Math.PI * 2;
                double radius = 3 + Math.random() * 3;
                level.sendParticles(ModParticles.GRAVITY_SINGULARITY.get(),
                        s.center.x, s.center.y + 1.5, s.center.z, 1,
                        s.orbitTiltX, radius, s.orbitTiltZ, 0);
            }
        }

        // Heartbeat hallucinations to nearby players
        if (phaseT % 20 == 0) {
            for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class,
                    new net.minecraft.world.phys.AABB(s.center, s.center).inflate(24))) {
                HallucinationManager.force(near, HallucinationType.HEARTBEAT_PULSE, 1.0F, 20, "");
                InsanityData.addInsanity(near, 1);
                InsanityData.addCosmicInfluence(near, 1);
            }
        }

        // Void smoke trails — large smoke clusters falling
        if (phaseT % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = Math.random() * 5;
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        s.center.x + Math.cos(a) * d,
                        s.center.y + 15 - Math.random() * 10,
                        s.center.z + Math.sin(a) * d,
                        1, 0, -0.05, 0, 0.02);
            }
        }

        // Shadow pulse — darkness on all entities in radius
        if (phaseT == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(s.center, s.center).inflate(16))) {
                if (e == caster) continue;
                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
            }
        }
    }

    // ────────── PHASE 5 — COSMIC COLLAPSE EXPLOSION (instant) ──────────

    private static void tickPhase5(ServerPlayer caster, ServerLevel level,
                                    CollapseSession s, long t) {
        // Maintained for 4 ticks — explosion expanding particles continue
        long phaseT = t - PHASE_4_END;
        if (phaseT < 4) {
            // Expanding shockwave particles
            for (int i = 0; i < 12; i++) {
                double a = i * (Math.PI * 2 / 12);
                double r = phaseT * 3.0 + 1.0;
                level.sendParticles(ModParticles.COLLAPSE_SHOCKWAVE.get(),
                        s.center.x + Math.cos(a) * r,
                        s.center.y + 1,
                        s.center.z + Math.sin(a) * r,
                        1, 0, 3.5, 0, 0);
            }
        }
    }

    /**
     * Trigger ONE-TIME no início da phase 5 — damage + main visual implosion.
     * Faz a "implosão de partículas pra dentro" usando reversed velocity
     * Dragon Breath, depois um Explosion vanilla pra impacto.
     */
    private static void triggerCollapseExplosion(ServerPlayer caster, ServerLevel level,
                                                   CollapseSession s) {
        BlockPos centerBp = BlockPos.containing(s.center);

        // Implosion: particles flying INWARD (use reversed dragon breath positions)
        for (int i = 0; i < 60; i++) {
            double a = Math.random() * Math.PI * 2;
            double d = 4 + Math.random() * 8;
            double px = s.center.x + Math.cos(a) * d;
            double py = s.center.y + Math.random() * 4;
            double pz = s.center.z + Math.sin(a) * d;
            // Spawn at outer ring, "moves" toward center via dragon breath particle
            level.sendParticles(ParticleTypes.DRAGON_BREATH, px, py, pz, 1,
                    -Math.cos(a) * 0.3, 0, -Math.sin(a) * 0.3, 0.1);
        }

        // Then expand: shockwave
        for (int i = 0; i < 24; i++) {
            double a = i * (Math.PI * 2 / 24);
            level.sendParticles(ModParticles.COLLAPSE_SHOCKWAVE.get(),
                    s.center.x + Math.cos(a) * 0.5,
                    s.center.y + 1,
                    s.center.z + Math.sin(a) * 0.5,
                    1, 0, 4.0, 0, 0);
        }
        // Vertical pillar of cosmic ash
        for (int y = 0; y < 25; y++) {
            level.sendParticles(ModParticles.VOID_LEAK.get(),
                    s.center.x, s.center.y + y, s.center.z,
                    3, 0.3, 0.0, 0.3, 0.1);
        }
        // Bright flash
        level.sendParticles(ParticleTypes.FLASH,
                s.center.x, s.center.y + 1, s.center.z, 1, 0, 0, 0, 0);

        // Vanilla explosion FOR SOUND ONLY (no terrain damage)
        level.explode(caster, s.center.x, s.center.y, s.center.z,
                3.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);

        // DAMAGE all entities in radius
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(s.center, s.center).inflate(COLLAPSE_RADIUS))) {
            if (e == caster) continue;
            // Damage scaled by distance — closer = more dmg
            double dist = e.position().distanceTo(s.center);
            float dmgMul = (float) Math.max(0.3, 1.0 - dist / COLLAPSE_RADIUS);
            float dmg = COLLAPSE_DAMAGE * dmgMul;
            e.hurt(caster.damageSources().magic(), dmg);

            // Knockback INWARD (gravitational, not outward)
            Vec3 toward = s.center.subtract(e.position()).normalize().scale(0.6 * dmgMul);
            e.setDeltaMovement(toward.x, toward.y + 0.2, toward.z);
            e.hurtMarked = true;

            // Sanity loss + hallucination trigger on players
            if (e instanceof ServerPlayer sp && sp != caster) {
                InsanityData.addInsanity(sp, 8);
                InsanityData.addCosmicInfluence(sp, 5);
                InsanityData.addParanoia(sp, 5);
                HallucinationManager.force(sp, HallucinationType.FAKE_DEATH_FLASH, 1.0F, 40, "");
                HallucinationManager.force(sp, HallucinationType.REALITY_SHAKE, 1.0F, 60, "");
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, false));
            }
        }

        // Audio: delayed sound wave (silence then BOOM)
        level.playSound(null, centerBp, SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.AMBIENT, 4.0F, 0.4F);
        level.playSound(null, centerBp, SoundEvents.GENERIC_EXPLODE,
                SoundSource.AMBIENT, 2.5F, 0.3F);

        // Sanity reset on caster — feedback
        caster.displayClientMessage(Component.literal(
                "§4§l§o*o som chega DEPOIS*"), false);
    }

    // ────────── PHASE 6 — POST CORRUPTION (8s) ──────────

    private static void tickPhase6(ServerPlayer caster, ServerLevel level,
                                    CollapseSession s, long t) {
        long phaseT = t - PHASE_5_END;

        // Black fog persists in radius
        if (phaseT % 5 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = Math.random() * 8;
                level.sendParticles(ParticleTypes.SMOKE,
                        s.center.x + Math.cos(a) * d,
                        s.center.y + Math.random() * 0.3,
                        s.center.z + Math.sin(a) * d,
                        2, 0.1, 0.05, 0.1, 0.01);
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        s.center.x + Math.cos(a) * d,
                        s.center.y + 0.5 + Math.random() * 2,
                        s.center.z + Math.sin(a) * d,
                        1, 0.1, 0.05, 0.1, 0.01);
            }
        }

        // Eye growths — soul fire flames on ground
        if (phaseT % 15 == 0) {
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * Math.PI * 2;
                double d = Math.random() * 6;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        s.center.x + Math.cos(a) * d,
                        s.center.y + 0.1,
                        s.center.z + Math.sin(a) * d,
                        1, 0, 0.02, 0, 0);
            }
        }

        // Whispers to nearby players
        if (phaseT % 40 == 0) {
            for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class,
                    new net.minecraft.world.phys.AABB(s.center, s.center).inflate(20))) {
                if (Math.random() < 0.5) {
                    HallucinationManager.force(near, HallucinationType.FAKE_WHISPER, 0.6F, 30, "");
                }
                InsanityData.addCorruption(near, 1);
            }
        }

        // Reality instability zones — random cracks
        if (phaseT % 10 == 0) {
            double a = Math.random() * Math.PI * 2;
            double d = Math.random() * 7;
            level.sendParticles(ModParticles.DIMENSIONAL_CRACK.get(),
                    s.center.x + Math.cos(a) * d,
                    s.center.y + 0.5 + Math.random() * 2,
                    s.center.z + Math.sin(a) * d,
                    1, 0, 1.2 + Math.random(), 0, 0);
        }
    }

    // ────────── helpers ──────────

    private static void spawnLightning(ServerLevel level, Vec3 pos) {
        var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    /** True se o player tem ritual ativo. */
    public static boolean isActive(Player p) {
        return SESSIONS.containsKey(p.getUUID());
    }
}
