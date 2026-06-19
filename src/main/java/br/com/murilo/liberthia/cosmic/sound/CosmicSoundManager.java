package br.com.murilo.liberthia.cosmic.sound;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * v0.1.22 r52: <b>Cosmic Sound Manager</b> — API per-player pra tocar
 * sons de horror sem broadcastar pra todos.
 *
 * <h2>Filosofia</h2>
 * Cada método envia um {@link ClientboundSoundPacket} APENAS pro
 * {@code sp.connection} do target — outros players próximos NÃO ouvem.
 * Isso permite que cada player tenha experiência sonora única.
 *
 * <h2>API</h2>
 * <ul>
 *   <li>{@link #playDistantWhispers(ServerPlayer)}</li>
 *   <li>{@link #playEyePulse(ServerPlayer)}</li>
 *   <li>{@link #playFalseFootsteps(ServerPlayer, boolean)}</li>
 *   <li>{@link #playVoidBreathing(ServerPlayer)}</li>
 *   <li>{@link #playRadioBroadcast(ServerPlayer)}</li>
 *   <li>{@link #playSkyHum(ServerPlayer)}</li>
 *   <li>{@link #playTendrilMovement(ServerPlayer)}</li>
 *   <li>{@link #playRealityDistortion(ServerPlayer)}</li>
 *   <li>{@link #playDistantScream(ServerPlayer)}</li>
 *   <li>{@link #playAudiencePresence(ServerPlayer)}</li>
 * </ul>
 *
 * <h2>Per-player cooldown</h2>
 * Cada player tem 1 cooldown por sound type pra não spammar.
 */
public final class CosmicSoundManager {

    /** Cooldowns per-player per-sound (UUID + sound key → next allowed tick). */
    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();

    /** Default cooldown em ticks. */
    public static final int DEFAULT_CD = 60; // 3s

    private CosmicSoundManager() {}

    // ──────────── Public API: 10 horror sounds ────────────

    /** Whispers atrás do player (distance 2-4 blocos). */
    public static void playDistantWhispers(ServerPlayer sp) {
        if (!canPlay(sp, "whispers", 80)) return;
        Vec3 behind = sp.position().subtract(sp.getLookAngle().scale(2 + Math.random() * 2));
        playPositional(sp, ModSounds.COSMIC_DISTANT_WHISPERS.get(),
                behind.x, behind.y + 1.5, behind.z,
                0.4F + (float)(Math.random() * 0.2F),
                0.7F + (float)(Math.random() * 0.3F));
    }

    /** Eye pulse — wet organic bass perto do player. */
    public static void playEyePulse(ServerPlayer sp) {
        if (!canPlay(sp, "eye_pulse", 100)) return;
        playPositional(sp, ModSounds.COSMIC_EYE_PULSE.get(),
                sp.getX(), sp.getY() + 1, sp.getZ(),
                0.6F, 0.5F + (float)(Math.random() * 0.2F));
    }

    /** Footsteps fake — behind=true coloca atrás, false=lateral random. */
    public static void playFalseFootsteps(ServerPlayer sp, boolean behind) {
        if (!canPlay(sp, "footsteps", 40)) return;
        Vec3 pos;
        if (behind) {
            pos = sp.position().subtract(sp.getLookAngle().scale(2 + Math.random() * 1.5));
        } else {
            double a = Math.random() * Math.PI * 2;
            double r = 2 + Math.random() * 3;
            pos = sp.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
        }
        playPositional(sp, ModSounds.COSMIC_FALSE_FOOTSTEPS.get(),
                pos.x, pos.y, pos.z,
                0.8F + (float)(Math.random() * 0.3F),
                0.85F + (float)(Math.random() * 0.3F));
    }

    /** Void breathing — exhale glacial. */
    public static void playVoidBreathing(ServerPlayer sp) {
        if (!canPlay(sp, "breathing", 200)) return;
        Vec3 behind = sp.position().subtract(sp.getLookAngle().scale(1.5));
        playPositional(sp, ModSounds.COSMIC_VOID_BREATHING.get(),
                behind.x, behind.y + 1.5, behind.z,
                0.5F, 0.7F);
    }

    /** Radio broadcast distorcido. */
    public static void playRadioBroadcast(ServerPlayer sp) {
        if (!canPlay(sp, "radio", 100)) return;
        playPositional(sp, ModSounds.COSMIC_RADIO_BROADCAST.get(),
                sp.getX(), sp.getY() + 1, sp.getZ(),
                1.0F, 0.9F + (float)(Math.random() * 0.2F));
    }

    /** Sky hum — drone atmosférico massivo (volume alto, pitch low). */
    public static void playSkyHum(ServerPlayer sp) {
        if (!canPlay(sp, "sky_hum", 600)) return;
        playPositional(sp, ModSounds.COSMIC_SKY_HUM.get(),
                sp.getX(), sp.getY() + 50, sp.getZ(),
                2.0F, 0.3F);
    }

    /** Tendril movement — wet flesh stretch. */
    public static void playTendrilMovement(ServerPlayer sp) {
        if (!canPlay(sp, "tendril", 60)) return;
        double a = Math.random() * Math.PI * 2;
        double r = 1 + Math.random() * 3;
        playPositional(sp, ModSounds.COSMIC_TENDRIL_MOVEMENT.get(),
                sp.getX() + Math.cos(a) * r,
                sp.getY() + Math.random() * 2,
                sp.getZ() + Math.sin(a) * r,
                0.7F, 0.8F + (float)(Math.random() * 0.4F));
    }

    /** Reality distortion — digital tear/drone. */
    public static void playRealityDistortion(ServerPlayer sp) {
        if (!canPlay(sp, "distortion", 80)) return;
        playPositional(sp, ModSounds.COSMIC_REALITY_DISTORTION.get(),
                sp.getX(), sp.getY() + 1, sp.getZ(),
                0.8F, 0.6F + (float)(Math.random() * 0.3F));
    }

    /** Distant scream — humano-like, distância 30-60 blocos. */
    public static void playDistantScream(ServerPlayer sp) {
        if (!canPlay(sp, "scream", 1200)) return; // raro
        double a = Math.random() * Math.PI * 2;
        double r = 30 + Math.random() * 30;
        playPositional(sp, ModSounds.COSMIC_DISTANT_SCREAM.get(),
                sp.getX() + Math.cos(a) * r,
                sp.getY() + Math.random() * 8,
                sp.getZ() + Math.sin(a) * r,
                2.5F, 0.7F);
    }

    /** Audience presence — low frequency pressure (something massive watching). */
    public static void playAudiencePresence(ServerPlayer sp) {
        if (!canPlay(sp, "audience", 800)) return;
        playPositional(sp, ModSounds.COSMIC_AUDIENCE_PRESENCE.get(),
                sp.getX(), sp.getY() + 20, sp.getZ(),
                1.8F, 0.2F);
    }

    // ──────────── Lower-level helpers ────────────

    /**
     * Toca som POSITIONAL exclusivo pro player via ClientboundSoundPacket.
     * Pode ser chamado por outros sistemas pra sons custom.
     */
    public static void playPositional(ServerPlayer sp, SoundEvent sound,
                                       double x, double y, double z,
                                       float volume, float pitch) {
        if (sp == null || sound == null) return;
        try {
            sp.connection.send(new ClientboundSoundPacket(
                    Holder.direct(sound),
                    SoundSource.AMBIENT,
                    x, y, z,
                    volume, pitch,
                    sp.level().random.nextLong()));
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[CosmicSound] play error: {}", t.toString());
        }
    }

    /** Para som específico (não permite tocar de novo até cooldown). */
    public static boolean canPlay(ServerPlayer sp, String soundKey, int cdTicks) {
        long now = sp.level().getGameTime();
        String key = sp.getUUID() + ":" + soundKey;
        Long lastAllowed = COOLDOWNS.get(key);
        if (lastAllowed != null && now < lastAllowed) return false;
        COOLDOWNS.put(key, now + cdTicks);
        return true;
    }

    /** Cleanup quando player desloga. */
    public static void cleanup(UUID playerId) {
        String prefix = playerId.toString() + ":";
        COOLDOWNS.keySet().removeIf(k -> k.startsWith(prefix));
    }

    // ──────────── Auto-hook ────────────

    /**
     * Toca o som apropriado pra um tipo de Hallucination (auto-played pelo
     * HallucinationManager quando dispara um effect).
     */
    public static void playForHallucination(ServerPlayer sp,
            br.com.murilo.liberthia.cosmic.hallucination.HallucinationType type) {
        if (sp == null || type == null) return;
        switch (type) {
            case FAKE_WHISPER, NAME_WHISPER -> playDistantWhispers(sp);
            case FAKE_FOOTSTEP -> playFalseFootsteps(sp, true);
            case HEARTBEAT_PULSE -> playEyePulse(sp);
            case FAKE_ENTITY_PERIPHERAL, TEMPORAL_GHOST -> playVoidBreathing(sp);
            case DISTORTED_AUDIO -> playRealityDistortion(sp);
            case REVERSE_AUDIO_PULSE -> playRadioBroadcast(sp);
            case IMPOSSIBLE_MOON -> playSkyHum(sp);
            case SHADOW_MOVEMENT -> playTendrilMovement(sp);
            case REALITY_SHAKE, SCREEN_GLITCH_BURST -> playRealityDistortion(sp);
            case FAKE_DEATH_FLASH -> playDistantScream(sp);
            default -> {}
        }
    }
}
