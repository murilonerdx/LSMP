package br.com.murilo.liberthia.cosmic;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.registry.ModParticles;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * v0.1.22 r38: <b>Procedural Event Scheduler</b> — gera eventos ambientais
 * por phase baseado em probabilidade. Server-side.
 *
 * <h2>r38 NEW PARANOIA EVENTS</h2>
 * <ul>
 *   <li><b>Mobs staring:</b> mobs/players próximos viram a cabeça pro player</li>
 *   <li><b>Footsteps behind:</b> som de passos atrás do player (positional)</li>
 *   <li><b>Distant grunts:</b> rosnados distantes (16-32 blocos)</li>
 *   <li><b>Mouse spin:</b> server manda packet pra rotar câmera do client</li>
 *   <li><b>Fake monsters:</b> partículas em volta simulando silhuetas</li>
 *   <li><b>Chill messages:</b> chat "frio na espinha", "alguém atrás de você"</li>
 * </ul>
 */
public final class CosmicHorrorEvents {

    private static final Random RNG = new Random();

    private static final String[] CHILL_MSGS = {
        "§8§o*você sente um frio na espinha*",
        "§8§o*alguém te observa de longe*",
        "§8§o*há respiração atrás de você*",
        "§8§o*os sons não combinam com o que você vê*",
        "§8§o*as sombras se moveram quando você desviou*",
        "§8§o*você tem certeza que estava sozinho?*",
        "§8§o*algo sussurra seu nome muito baixo*",
        "§8§o*você sente um cheiro de carne queimada*",
        "§5§o*o ar pesa mais agora*",
        "§5§o*alguma coisa caminha em você através do tempo*"
    };

    private CosmicHorrorEvents() {}

    /** Chamado quando player ENTRA numa nova phase (single shot). */
    public static void onPhaseEnter(ServerPlayer sp, CosmicHorrorPhase phase) {
        switch (phase) {
            case SUBTLE_PRESENCE -> {
                sp.displayClientMessage(Component.literal(
                        "§8§o*algo te observa*"), true);
                playPositional(sp, safeSound(ModSounds.COSMIC_WHISPER_1.get(),
                        SoundEvents.AMBIENT_CAVE.value()), 0.3F, 0.6F);
            }
            case DIMENSIONAL_CORRUPTION -> {
                sp.displayClientMessage(Component.literal(
                        "§5§oA realidade começa a falhar..."), false);
                playPositional(sp, safeSound(ModSounds.COSMIC_AZATHOTH_EYE.get(),
                        SoundEvents.PORTAL_AMBIENT), 1.2F, 0.4F);
            }
            case REALITY_RUPTURE -> {
                sp.displayClientMessage(Component.literal(
                        "§4§l§o✦ O VÉU SE RASGA ✦"), false);
                playPositional(sp, safeSound(ModSounds.COSMIC_DIMENSIONAL_TEAR.get(),
                        SoundEvents.WITHER_SPAWN), 2.5F, 0.5F);
                // Lightning vermelho aleatório
                spawnLightningVisualOnly(sp);
            }
            case ENTITY_MANIFESTATION -> {
                sp.displayClientMessage(Component.literal(
                        "§4§l§o✦ ELE TE VÊ ✦"), false);
                playPositional(sp, safeSound(ModSounds.SCREAMER_SCREAM.get(),
                        SoundEvents.WARDEN_ROAR), 4.0F, 0.3F);
                sp.displayClientMessage(Component.literal(
                        "§4§l§o⚠ §r§4§oO Tomo te puxa pra Outro Lugar em breve..."), false);
                // Spawn da entidade colossal acima do player
                spawnHerald(sp);
            }
            default -> {}
        }
    }

    /** Tick periódico durante phase ativa — eventos procedurais. */
    public static void tickPhase(ServerPlayer sp, CosmicHorrorManager.Session s) {
        ServerLevel level = (ServerLevel) sp.level();
        long tick = level.getGameTime();
        CosmicHorrorPhase p = s.phase;
        // r183: eventos de fase (vozes/partículas/paranoia) só com sanidade < 40%
        if (br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp) >= 40) return;

        // r40: durante phase ativa, acumula insanity gradualmente (1/sec)
        if (tick % 20 == 0 && p != CosmicHorrorPhase.DORMANT) {
            br.com.murilo.liberthia.cosmic.insanity.InsanityData.addInsanity(sp, 1);
            if (p.ordinal() >= CosmicHorrorPhase.REALITY_RUPTURE.ordinal()) {
                br.com.murilo.liberthia.cosmic.insanity.InsanityData.addParanoia(sp, 1);
            }
            if (p == CosmicHorrorPhase.ENTITY_MANIFESTATION) {
                br.com.murilo.liberthia.cosmic.insanity.InsanityData.addCosmicInfluence(sp, 1);
            }
        }

        // Particles em volta do player (rate por phase)
        if (p.particleRate > 0 && tick % Math.max(1, (int)(20 / p.particleRate)) == 0) {
            spawnAmbientParticles(level, sp, p);
        }

        // Whispers aleatórios
        if (p.whisperChance > 0 && RNG.nextFloat() < p.whisperChance / 20.0F) {
            playRandomWhisper(sp, p);
        }

        // r79 FIX: chill messages causavam spam infinito no chat (user reportou
        // "fica aparecendo mensagens de player no chat toda hora").
        // DESABILITADO. Mensagens narrativas só rolam em onPhaseEnter (1 vez por phase).
        // Pra reativar: descomenta + diminui whisperChance.

        // r38: PARANOIA EVENT — Mobs/players staring at player
        if (p.starePulseChance > 0 && RNG.nextFloat() < p.starePulseChance / 20.0F) {
            triggerStare(sp, level);
        }

        // r38: PARANOIA EVENT — Footsteps behind player
        if (p.footstepChance > 0 && RNG.nextFloat() < p.footstepChance / 20.0F) {
            playFootstepsBehind(sp);
        }

        // r38: PARANOIA EVENT — Mouse spin (random rotation force)
        if (p.mouseSpinChance > 0 && RNG.nextFloat() < p.mouseSpinChance / 20.0F) {
            forceRotation(sp, p);
        }

        // r38: PARANOIA EVENT — Fake monster silhouettes (particles cluster)
        if (p.paranoiaChance > 0 && RNG.nextFloat() < p.paranoiaChance / 20.0F) {
            spawnFakeMonsterSilhouette(sp, level);
        }

        // r38: Distant grunts (positional sound 16-32 blocks away)
        if (p.whisperChance > 0 && RNG.nextFloat() < p.whisperChance / 25.0F) {
            playDistantGrunt(sp, p);
        }

        // Phase-specific events (original logic mantida)
        switch (p) {
            case SUBTLE_PRESENCE -> {
                if (tick % 100 == 0 && RNG.nextFloat() < 0.3F) {
                    Vec3 behind = sp.position().subtract(sp.getLookAngle().scale(8));
                    level.sendParticles(ParticleTypes.SMOKE,
                            behind.x, behind.y + 1.5, behind.z, 3, 0.2, 0.2, 0.2, 0.01);
                }
                if (tick % 200 == 0) {
                    sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
                }
            }
            case DIMENSIONAL_CORRUPTION -> {
                if (tick % 10 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = RNG.nextDouble() * Math.PI * 2;
                        double r = 5 + RNG.nextDouble() * 20;
                        level.sendParticles(ParticleTypes.SQUID_INK,
                                sp.getX() + Math.cos(a) * r,
                                sp.getY() + 25 + RNG.nextDouble() * 10,
                                sp.getZ() + Math.sin(a) * r,
                                1, 0, -0.3, 0, 0.05);
                    }
                }
                if (tick % 80 == 0) {
                    sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
                }
            }
            case REALITY_RUPTURE -> {
                if (tick % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = RNG.nextDouble() * Math.PI * 2;
                        double r = RNG.nextDouble() * 6;
                        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                                sp.getX() + Math.cos(a) * r,
                                sp.getY() + 30 + RNG.nextDouble() * 8,
                                sp.getZ() + Math.sin(a) * r,
                                1, 0, -0.2, 0, 0.05);
                    }
                }
                if (tick % 60 == 0 && RNG.nextFloat() < 0.5F) {
                    spawnLightningVisualOnly(sp);
                }
            }
            case ENTITY_MANIFESTATION -> {
                if (tick % 3 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double a = (tick * 0.05 + i * 24) % 360;
                        double rad = Math.toRadians(a);
                        double r = 8 + Math.sin(tick * 0.02) * 3;
                        level.sendParticles(ParticleTypes.PORTAL,
                                sp.getX() + Math.cos(rad) * r,
                                sp.getY() + 2 + Math.sin(tick * 0.01 + i) * 1.5,
                                sp.getZ() + Math.sin(rad) * r,
                                1, 0.05, 0.05, 0.05, 0.02);
                    }
                }
                if (tick % 40 == 0) {
                    sp.hurt(sp.damageSources().wither(), 1.0F);
                    sp.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, true, false));
                }
            }
            default -> {}
        }
    }

    // ─────────────────────── r38: NEW PARANOIA EVENTS ───────────────────────

    /** Faz mobs/players próximos virarem cabeça pro player (efeito staring). */
    private static void triggerStare(ServerPlayer sp, ServerLevel level) {
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(20));
        for (LivingEntity e : nearby) {
            if (e == sp) continue;
            // Calcula yaw/pitch pra olhar pro player
            Vec3 toPlayer = sp.position().subtract(e.position());
            double dx = toPlayer.x;
            double dz = toPlayer.z;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
            float pitch = (float) (-Math.toDegrees(Math.atan2(toPlayer.y - 1, horizDist)));

            e.setYRot(yaw);
            e.setXRot(pitch);
            e.setYHeadRot(yaw);
            e.setYBodyRot(yaw);

            // Adiciona um effect leve só pra estatísticas: glowing brief
            if (e instanceof Mob mob) {
                mob.getLookControl().setLookAt(sp.getX(), sp.getY() + 1.5, sp.getZ(),
                        180F, 180F);
            }
        }
        // r79 FIX: removido chat narrativo "*todos estão te encarando*" — era spam.
        // O efeito visual (mobs rotacionando pra olhar pro player) já é suficiente.
    }

    /** Som de passos atrás do player (positional, distância ~2-3 blocos). */
    private static void playFootstepsBehind(ServerPlayer sp) {
        Vec3 behind = sp.position().subtract(sp.getLookAngle().scale(2.5));
        // Toca 2-4 passos seguidos
        int steps = 2 + RNG.nextInt(3);
        for (int i = 0; i < steps; i++) {
            final int delay = i * 6; // 0.3s entre passos — agendado por TickTask (antes tocavam todos juntos)
            sp.server.tell(new net.minecraft.server.TickTask(sp.server.getTickCount() + delay, () -> {
                if (sp.isRemoved()) return;
                sp.connection.send(new ClientboundSoundPacket(
                        Holder.direct(SoundEvents.STONE_HIT),
                        SoundSource.HOSTILE,
                        behind.x, behind.y, behind.z,
                        1.5F, 0.7F + RNG.nextFloat() * 0.3F,
                        sp.level().random.nextLong()));
            }));
        }
    }

    /** Server manda packet pro client rotar a câmera N graus (mouse spinning). */
    private static void forceRotation(ServerPlayer sp, CosmicHorrorPhase p) {
        // Magnitude do spin escala com a phase
        float baseMag = 15.0F + p.ordinal() * 10.0F; // 25 / 35 / 45 / 55
        float yawDelta = (RNG.nextFloat() - 0.5F) * 2 * baseMag;
        float pitchDelta = (RNG.nextFloat() - 0.5F) * 2 * (baseMag * 0.3F);
        ModNetwork.sendToPlayer(sp, new CosmicForceRotationS2CPacket(yawDelta, pitchDelta));
    }

    /** Spawna nuvem de partículas escuras como silhueta de monstro na periferia. */
    private static void spawnFakeMonsterSilhouette(ServerPlayer sp, ServerLevel level) {
        // Posição aleatória 8-16 blocos em volta do player
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist = 8 + RNG.nextDouble() * 8;
        double cx = sp.getX() + Math.cos(angle) * dist;
        double cz = sp.getZ() + Math.sin(angle) * dist;
        double cy = sp.getY();

        // Desenha silhueta humanoide com SMOKE — cabeça, torso, pernas
        // Cabeça (cluster 0.5x0.5 em y=2)
        for (int i = 0; i < 8; i++) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    cx + (RNG.nextDouble() - 0.5) * 0.4,
                    cy + 1.8 + RNG.nextDouble() * 0.3,
                    cz + (RNG.nextDouble() - 0.5) * 0.4,
                    1, 0, 0, 0, 0);
        }
        // Torso (cluster 0.6x1 em y=1)
        for (int i = 0; i < 12; i++) {
            level.sendParticles(ParticleTypes.SMOKE,
                    cx + (RNG.nextDouble() - 0.5) * 0.6,
                    cy + 0.8 + RNG.nextDouble() * 0.8,
                    cz + (RNG.nextDouble() - 0.5) * 0.6,
                    1, 0, 0, 0, 0);
        }
        // Pernas (cluster 0.3x0.8 em y=0.4)
        for (int i = 0; i < 8; i++) {
            level.sendParticles(ParticleTypes.SMOKE,
                    cx + (RNG.nextDouble() - 0.5) * 0.4,
                    cy + RNG.nextDouble() * 0.8,
                    cz + (RNG.nextDouble() - 0.5) * 0.4,
                    1, 0, 0, 0, 0);
        }
        // Olhos brilhantes — 2 SOUL_FIRE_FLAME pixels onde seriam os olhos
        for (int i = 0; i < 2; i++) {
            double eyeX = cx + (i == 0 ? -0.1 : 0.1);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    eyeX, cy + 1.85, cz,
                    1, 0, 0, 0, 0);
        }
    }

    /** Som de "rosnado" distante (16-32 blocos do player). */
    private static void playDistantGrunt(ServerPlayer sp, CosmicHorrorPhase p) {
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist = 16 + RNG.nextDouble() * 16;
        double gx = sp.getX() + Math.cos(angle) * dist;
        double gz = sp.getZ() + Math.sin(angle) * dist;
        double gy = sp.getY() + RNG.nextDouble() * 5 - 2;

        SoundEvent[] grunts = new SoundEvent[]{
                SoundEvents.ZOMBIE_AMBIENT,
                SoundEvents.SKELETON_AMBIENT,
                SoundEvents.HUSK_AMBIENT,
                SoundEvents.PIGLIN_AMBIENT,
                SoundEvents.PIGLIN_ANGRY,
                SoundEvents.WITHER_AMBIENT,
                SoundEvents.WARDEN_AMBIENT
        };
        SoundEvent chosen = grunts[RNG.nextInt(grunts.length)];
        float vol = 0.6F + p.ordinal() * 0.3F;
        float pitch = 0.5F + RNG.nextFloat() * 0.4F;

        sp.connection.send(new ClientboundSoundPacket(
                Holder.direct(chosen), SoundSource.HOSTILE,
                gx, gy, gz, vol, pitch, sp.level().random.nextLong()));
    }

    // ─────────────────────── HELPERS ───────────────────────

    private static SoundEvent safeSound(SoundEvent custom, SoundEvent fallback) {
        return custom != null ? custom : fallback;
    }

    private static void spawnAmbientParticles(ServerLevel level, ServerPlayer sp,
                                               CosmicHorrorPhase phase) {
        double radius = 4 + phase.ordinal() * 3;
        double a = RNG.nextDouble() * Math.PI * 2;
        double y = sp.getY() + RNG.nextDouble() * 4 - 1;
        try {
            level.sendParticles(ModParticles.COSMIC_ORBIT.get(),
                    sp.getX() + Math.cos(a) * radius,
                    y,
                    sp.getZ() + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        } catch (Throwable t) {
            level.sendParticles(ParticleTypes.PORTAL,
                    sp.getX() + Math.cos(a) * radius,
                    y,
                    sp.getZ() + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    private static void playRandomWhisper(ServerPlayer sp, CosmicHorrorPhase phase) {
        var sounds = new SoundEvent[]{
                safeSound(ModSounds.COSMIC_WHISPER_1.get(), SoundEvents.AMBIENT_CAVE.value()),
                safeSound(ModSounds.COSMIC_WHISPER_2.get(), SoundEvents.AMBIENT_CAVE.value()),
                safeSound(ModSounds.COSMIC_WHISPER_3.get(), SoundEvents.AMBIENT_CAVE.value())
        };
        var chosen = sounds[RNG.nextInt(sounds.length)];
        double vol = 0.3F + phase.ordinal() * 0.2F;
        double pitch = 0.4F + RNG.nextFloat() * 0.4F;
        playPositional(sp, chosen, (float) vol, (float) pitch);
    }

    private static void playPositional(ServerPlayer sp, SoundEvent se,
                                       float vol, float pitch) {
        sp.connection.send(new ClientboundSoundPacket(
                Holder.direct(se), SoundSource.AMBIENT,
                sp.getX(), sp.getY(), sp.getZ(), vol, pitch, sp.level().random.nextLong()));
    }

    private static void spawnLightningVisualOnly(ServerPlayer sp) {
        ServerLevel level = (ServerLevel) sp.level();
        double a = RNG.nextDouble() * Math.PI * 2;
        double r = 8 + RNG.nextDouble() * 16;
        var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(sp.getX() + Math.cos(a) * r, sp.getY(), sp.getZ() + Math.sin(a) * r);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private static void spawnHerald(ServerPlayer sp) {
        ServerLevel level = (ServerLevel) sp.level();
        try {
            Entity herald = net.minecraft.world.entity.EntityType.WARDEN.create(level);
            if (herald != null) {
                herald.moveTo(sp.getX(), sp.getY() + 50, sp.getZ(),
                        sp.getYRot(), 0);
                herald.setCustomName(Component.literal("§4§lO QUE NÃO DEVE SER NOMEADO"));
                herald.setCustomNameVisible(false);
                herald.setNoGravity(false);
                herald.getPersistentData().putBoolean("liberthia.cosmic_herald", true);
                level.addFreshEntity(herald);
            }
        } catch (Throwable ignored) {}
    }
}
