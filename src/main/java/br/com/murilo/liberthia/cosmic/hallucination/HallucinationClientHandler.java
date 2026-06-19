package br.com.murilo.liberthia.cosmic.hallucination;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedList;
import java.util.Queue;

/**
 * v0.1.22 r40: <b>Client-side dispatcher</b> de hallucinations. Recebe
 * pacotes do server e aplica efeitos VISUAIS/SONOROS LOCAIS — outros players
 * não vêem nada.
 *
 * <h2>Estado client-side</h2>
 * Algumas hallucinations são instantâneas (sound, chat msg). Outras
 * persistem por X ticks (fake entity, glitch burst, fake death flash).
 * Estado persistente fica no static state — limpa ao desconectar.
 *
 * <h2>Hooks</h2>
 * Eventos client-tick atualizam timers das hallucinations ativas.
 * RenderGui overlay desenha em cima da tela.
 */
@OnlyIn(Dist.CLIENT)
public final class HallucinationClientHandler {

    /** Estado de uma hallucination ativa (com timer). */
    public static class ActiveHallucination {
        public HallucinationType type;
        public float x, y, z;
        public int remainingTicks;
        public float intensity;
        public int variant;
        public String aux;
        public long startTick;

        public ActiveHallucination(HallucinationS2CPacket p, long now) {
            this.type = p.type;
            this.x = p.x; this.y = p.y; this.z = p.z;
            this.remainingTicks = p.duration;
            this.intensity = p.intensity;
            this.variant = p.variant;
            this.aux = p.aux;
            this.startTick = now;
        }
    }

    /** Fila de hallucinations ativas (rendered each frame). */
    public static final Queue<ActiveHallucination> ACTIVE = new LinkedList<>();

    /** Última batida de coração tocada (pra HEARTBEAT_PULSE rate-limit). */
    public static long lastHeartbeatTick = 0;

    /** Glitch burst alpha atual (0-1) — usado pelo CosmicScreenOverlay. */
    public static volatile float glitchBurstAlpha = 0F;

    /** Fake death flash alpha (0-1). */
    public static volatile float deathFlashAlpha = 0F;

    /** Reality shake intensity (0-1) — usado por CosmicCameraShake. */
    public static volatile float shakeIntensity = 0F;

    private HallucinationClientHandler() {}

    /** Chamado pelo packet handler. */
    public static void dispatch(HallucinationS2CPacket pkt) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        Player p = mc.player;

        switch (pkt.type) {
            case FAKE_WHISPER, NAME_WHISPER -> playPrivateSound(SoundEvents.AMBIENT_CAVE.value(),
                    pkt.intensity, 0.5F + (float)(Math.random() * 0.3F), pkt.x, pkt.y, pkt.z);
            case FAKE_FOOTSTEP -> playPrivateSound(SoundEvents.STONE_HIT,
                    pkt.intensity * 1.5F, 0.6F + (float)(Math.random() * 0.3F),
                    pkt.x, pkt.y, pkt.z);
            case FAKE_CHAT_MESSAGE -> {
                if (!pkt.aux.isEmpty()) {
                    p.displayClientMessage(Component.literal(pkt.aux), false);
                }
            }
            case FAKE_DAMAGE_INDICATOR -> {
                // Triggers vanilla "hurt" overlay sem dano real
                p.hurtTime = 10;
                p.hurtDuration = 10;
                playPrivateSound(SoundEvents.PLAYER_HURT,
                        pkt.intensity * 0.7F, 1F, p.getX(), p.getY(), p.getZ());
            }
            case FAKE_DEATH_FLASH -> {
                deathFlashAlpha = Math.min(1F, pkt.intensity);
                ACTIVE.add(new ActiveHallucination(pkt, now));
            }
            case SCREEN_GLITCH_BURST -> {
                glitchBurstAlpha = Math.min(1F, pkt.intensity);
                ACTIVE.add(new ActiveHallucination(pkt, now));
            }
            case REVERSE_AUDIO_PULSE -> playPrivateSound(SoundEvents.PORTAL_AMBIENT,
                    pkt.intensity, 0.3F, p.getX(), p.getY(), p.getZ());
            case DISTORTED_AUDIO -> playPrivateSound(SoundEvents.WARDEN_AMBIENT,
                    pkt.intensity * 0.5F, 0.4F + (float)(Math.random() * 0.2F),
                    p.getX(), p.getY(), p.getZ());
            case HEARTBEAT_PULSE -> {
                if (now - lastHeartbeatTick > 8) {
                    playPrivateSound(SoundEvents.WARDEN_HEARTBEAT,
                            pkt.intensity, 1F + (float)(Math.random() * 0.3F),
                            p.getX(), p.getY(), p.getZ());
                    lastHeartbeatTick = now;
                }
            }
            case REALITY_SHAKE -> {
                shakeIntensity = Math.min(1F, pkt.intensity);
                ACTIVE.add(new ActiveHallucination(pkt, now));
            }
            case FAKE_ENTITY_PERIPHERAL -> {
                // Spawn cluster de partículas em volta — silhueta fake
                if (mc.level != null) {
                    spawnFakeSilhouetteParticles(p.getX() + pkt.x, p.getY() + pkt.y, p.getZ() + pkt.z);
                }
                ACTIVE.add(new ActiveHallucination(pkt, now));
            }
            case TEMPORAL_GHOST -> {
                // Ghost = SMOKE cluster com nametag fake
                if (mc.level != null) {
                    for (int i = 0; i < 20; i++) {
                        mc.level.addParticle(ParticleTypes.LARGE_SMOKE,
                                p.getX() + pkt.x + (Math.random() - 0.5) * 0.5,
                                p.getY() + pkt.y + Math.random() * 1.8,
                                p.getZ() + pkt.z + (Math.random() - 0.5) * 0.5,
                                0, 0.02, 0);
                    }
                }
            }
            case FAKE_BLOCK_FLASH -> {
                if (mc.level != null) {
                    // SQUID_INK flash no bloco indicado
                    mc.level.addParticle(ParticleTypes.SQUID_INK,
                            p.getX() + pkt.x, p.getY() + pkt.y, p.getZ() + pkt.z,
                            0, 0, 0);
                }
            }
            case FALSE_LIGHT -> {
                // END_ROD cluster simulando luz de "lugar errado"
                if (mc.level != null) {
                    for (int i = 0; i < 8; i++) {
                        mc.level.addParticle(ParticleTypes.END_ROD,
                                p.getX() + pkt.x + (Math.random() - 0.5),
                                p.getY() + pkt.y + Math.random() * 0.5,
                                p.getZ() + pkt.z + (Math.random() - 0.5),
                                0, 0, 0);
                    }
                }
            }
            case IMPOSSIBLE_MOON -> {
                // Trigger sky renderer
                ACTIVE.add(new ActiveHallucination(pkt, now));
            }
            case SHADOW_MOVEMENT -> {
                // SMOKE pulse rápido no canto da tela (cluster gentle)
                if (mc.level != null) {
                    var look = p.getLookAngle();
                    // Side vector
                    double sx = -look.z, sz = look.x;
                    for (int i = 0; i < 6; i++) {
                        mc.level.addParticle(ParticleTypes.SMOKE,
                                p.getX() + sx * (4 + Math.random()),
                                p.getY() + 1.5 + Math.random(),
                                p.getZ() + sz * (4 + Math.random()),
                                0, 0.05, 0);
                    }
                }
            }
            case FAKE_INVENTORY_ITEM -> {
                // Visual blip — só um som de pickup pra desorientar
                playPrivateSound(SoundEvents.ITEM_PICKUP,
                        pkt.intensity * 0.5F, 0.5F,
                        p.getX(), p.getY(), p.getZ());
            }
            default -> {}
        }

        if (ACTIVE.size() > 16) {
            ACTIVE.poll(); // limita memory
        }
    }

    /** Chamado a cada client tick — atualiza timers + decai alphas. */
    public static void clientTick() {
        var it = ACTIVE.iterator();
        while (it.hasNext()) {
            ActiveHallucination h = it.next();
            h.remainingTicks--;
            if (h.remainingTicks <= 0) {
                it.remove();
                // Reset alphas se era esse tipo
                if (h.type == HallucinationType.SCREEN_GLITCH_BURST) glitchBurstAlpha = 0F;
                if (h.type == HallucinationType.FAKE_DEATH_FLASH) deathFlashAlpha = 0F;
                if (h.type == HallucinationType.REALITY_SHAKE) shakeIntensity = 0F;
            }
        }
        // Decay continuous alphas
        if (glitchBurstAlpha > 0) glitchBurstAlpha = Math.max(0, glitchBurstAlpha - 0.05F);
        if (deathFlashAlpha > 0) deathFlashAlpha = Math.max(0, deathFlashAlpha - 0.03F);
        if (shakeIntensity > 0) shakeIntensity = Math.max(0, shakeIntensity - 0.04F);
    }

    /** Limpa estado (desconexão). */
    public static void clear() {
        ACTIVE.clear();
        glitchBurstAlpha = 0F;
        deathFlashAlpha = 0F;
        shakeIntensity = 0F;
    }

    // ────────── helpers ──────────

    private static void playPrivateSound(SoundEvent se, float vol, float pitch,
                                          double x, double y, double z) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        try {
            mc.level.playLocalSound(x, y, z, se,
                    net.minecraft.sounds.SoundSource.AMBIENT, vol, pitch, false);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[Hallucination] sound error: {}", t.toString());
        }
    }

    private static void spawnFakeSilhouetteParticles(double cx, double cy, double cz) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // Cabeça
        for (int i = 0; i < 6; i++) {
            mc.level.addParticle(ParticleTypes.LARGE_SMOKE,
                    cx + (Math.random() - 0.5) * 0.3,
                    cy + 1.8 + Math.random() * 0.2,
                    cz + (Math.random() - 0.5) * 0.3,
                    0, 0, 0);
        }
        // Torso
        for (int i = 0; i < 10; i++) {
            mc.level.addParticle(ParticleTypes.SMOKE,
                    cx + (Math.random() - 0.5) * 0.5,
                    cy + 0.8 + Math.random() * 0.8,
                    cz + (Math.random() - 0.5) * 0.5,
                    0, 0, 0);
        }
        // Olhos
        for (int i = 0; i < 2; i++) {
            mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    cx + (i == 0 ? -0.1 : 0.1),
                    cy + 1.85, cz,
                    0, 0, 0);
        }
    }
}
