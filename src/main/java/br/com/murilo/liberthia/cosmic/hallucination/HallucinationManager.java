package br.com.murilo.liberthia.cosmic.hallucination;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

/**
 * v0.1.22 r40: <b>Server-side ParanoiaManager</b> — analisa contexto do player
 * a cada N ticks e decide se/qual hallucination injetar.
 *
 * <h2>Context analyzed</h2>
 * <ul>
 *   <li><b>aggregate horror</b> (de InsanityData) — pressão psicológica acumulada</li>
 *   <li><b>darkness exposure</b> — light level local</li>
 *   <li><b>isolation</b> — distância de outros players</li>
 *   <li><b>time underground</b> — Y muito baixo</li>
 *   <li><b>nearby hostiles</b> — quantos mobs hostis perto</li>
 *   <li><b>dimension instability</b> — em Loom/Spirit?</li>
 *   <li><b>recent damage</b> — tomou dano recentemente?</li>
 * </ul>
 *
 * <h2>Strategy</h2>
 * Weighted random:
 * <ul>
 *   <li>Baixo horror (&lt;25): hallucinations leves (whisper, footstep) raras</li>
 *   <li>Médio (25-50): adiciona shadow_movement, fake_chat, fake_block_flash</li>
 *   <li>Alto (50-75): adiciona fake_entity_peripheral, distorted_audio, screen_glitch_burst</li>
 *   <li>Breaking (75+): adiciona fake_death_flash, reality_shake, impossible_moon, name_whisper</li>
 * </ul>
 */
public final class HallucinationManager {

    /** Intervalo entre análises (3s) — não roda EVERY tick pra economizar. */
    public static final int ANALYSIS_INTERVAL_TICKS = 60;

    /** Chance base por tick de paranoia event (varia com horror level). */
    public static final float BASE_CHANCE_PER_ANALYSIS = 0.15F;

    private static final Random RNG = new Random();

    /** Map de UUID → último tick que ParanoiaManager analisou (rate-limit). */
    private static final java.util.Map<java.util.UUID, Long> LAST_ANALYSIS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Map de UUID → último tick que algum player não-self foi visto (isolação). */
    private static final java.util.Map<java.util.UUID, Long> LAST_PLAYER_SIGHTING =
            new java.util.concurrent.ConcurrentHashMap<>();

    private HallucinationManager() {}

    /**
     * Chamado a cada server tick — escolhe players elegíveis e roda análise
     * pra cada um.
     */
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % ANALYSIS_INTERVAL_TICKS != 0) return;

        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (sp.isCreative() || sp.isSpectator()) continue;

            // Update isolation tracker
            updateIsolationTracking(sp, now);

            // Analyze + maybe inject hallucination
            analyzePlayer(sp, now);
        }
    }

    private static void updateIsolationTracking(ServerPlayer sp, long now) {
        ServerLevel level = sp.serverLevel();
        // Acha outro player vivo num raio 64
        boolean anyOther = false;
        for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
            if (other == sp) continue;
            if (other.serverLevel() != level) continue;
            if (other.distanceToSqr(sp) < 64 * 64) {
                anyOther = true;
                break;
            }
        }
        if (anyOther) {
            LAST_PLAYER_SIGHTING.put(sp.getUUID(), now);
        }
    }

    /** Computa "isolação" em ticks desde último avistamento. */
    public static long getIsolationTicks(ServerPlayer sp, long now) {
        Long last = LAST_PLAYER_SIGHTING.get(sp.getUUID());
        if (last == null) return 0;
        return now - last;
    }

    private static void analyzePlayer(ServerPlayer sp, long now) {
        ServerLevel level = sp.serverLevel();
        // r183: alucinações só com sanidade < 40% (Spirit World é exceção — sempre assombra)
        if (br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp) >= 40
                && !br.com.murilo.liberthia.dimension.SpiritDimension.isInSpiritWorld(sp)) return;

        int horror = InsanityData.aggregateHorror(sp);
        int insanity = InsanityData.getInsanity(sp);
        int paranoiaStat = InsanityData.getParanoia(sp);

        // Context factors
        BlockPos pos = sp.blockPosition();
        int lightLevel = level.getMaxLocalRawBrightness(pos);
        boolean isDark = lightLevel < 4;
        boolean isVeryDark = lightLevel < 1;
        boolean isUnderground = pos.getY() < 50;
        boolean isVeryDeep = pos.getY() < 0;
        long isolation = getIsolationTicks(sp, now);
        boolean isAlone = isolation > 600; // 30s sem ver outro player
        boolean isVeryAlone = isolation > 2400; // 2min

        // Detect nearby hostiles
        int hostileCount = (int) level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(24),
                e -> e instanceof net.minecraft.world.entity.monster.Monster).size();

        // Special dimensions
        boolean inDangerDim = level.dimension().location().getPath().contains("loom")
                || level.dimension().location().getPath().contains("spirit");

        // Calcula peso de chance — multiplicadores aditivos
        float chance = BASE_CHANCE_PER_ANALYSIS;
        chance += horror / 200.0F;          // +0.5 max em horror 100
        chance += paranoiaStat / 250.0F;    // +0.4 max em paranoia 100
        if (isDark) chance += 0.10F;
        if (isVeryDark) chance += 0.15F;
        if (isUnderground) chance += 0.05F;
        if (isVeryDeep) chance += 0.10F;
        if (isAlone) chance += 0.10F;
        if (isVeryAlone) chance += 0.20F;
        if (hostileCount > 3) chance += 0.10F;
        if (inDangerDim) chance += 0.25F;

        chance = Math.min(0.95F, chance);

        if (RNG.nextFloat() > chance) return; // Não rolou

        // Escolhe TIPO de hallucination com base no horror level
        HallucinationType type = pickType(horror, isDark, isAlone, inDangerDim);
        injectHallucination(sp, type, horror);

        // Acumula paranoia leve (escalada natural)
        if (RNG.nextFloat() < 0.3F) {
            InsanityData.addParanoia(sp, 1);
        }
    }

    /**
     * Pondera HallucinationType por horror level — tiers vão escalando do
     * leve pro extremo.
     */
    private static HallucinationType pickType(int horror, boolean dark, boolean alone, boolean dangerDim) {
        // Tiers: cada tier tem opções; horror desbloqueia tiers maiores
        HallucinationType[] tier1 = {
                HallucinationType.FAKE_WHISPER,
                HallucinationType.FAKE_FOOTSTEP,
                HallucinationType.SHADOW_MOVEMENT
        };
        HallucinationType[] tier2 = {
                HallucinationType.FAKE_CHAT_MESSAGE,
                HallucinationType.FAKE_BLOCK_FLASH,
                HallucinationType.DISTORTED_AUDIO,
                HallucinationType.HEARTBEAT_PULSE
        };
        HallucinationType[] tier3 = {
                HallucinationType.FAKE_ENTITY_PERIPHERAL,
                HallucinationType.SCREEN_GLITCH_BURST,
                HallucinationType.TEMPORAL_GHOST,
                HallucinationType.REVERSE_AUDIO_PULSE,
                HallucinationType.FALSE_LIGHT,
                HallucinationType.NAME_WHISPER
        };
        HallucinationType[] tier4 = {
                HallucinationType.FAKE_DEATH_FLASH,
                HallucinationType.FAKE_DAMAGE_INDICATOR,
                HallucinationType.REALITY_SHAKE,
                HallucinationType.IMPOSSIBLE_MOON,
                HallucinationType.FAKE_INVENTORY_ITEM
        };

        if (horror < InsanityData.THRESHOLD_LOW) {
            return tier1[RNG.nextInt(tier1.length)];
        }
        if (horror < InsanityData.THRESHOLD_MED) {
            return RNG.nextFloat() < 0.6F ? tier1[RNG.nextInt(tier1.length)]
                    : tier2[RNG.nextInt(tier2.length)];
        }
        if (horror < InsanityData.THRESHOLD_HIGH) {
            float r = RNG.nextFloat();
            if (r < 0.3F) return tier1[RNG.nextInt(tier1.length)];
            if (r < 0.7F) return tier2[RNG.nextInt(tier2.length)];
            return tier3[RNG.nextInt(tier3.length)];
        }
        if (horror < InsanityData.THRESHOLD_BREAKING) {
            float r = RNG.nextFloat();
            if (r < 0.2F) return tier2[RNG.nextInt(tier2.length)];
            if (r < 0.55F) return tier3[RNG.nextInt(tier3.length)];
            return tier4[RNG.nextInt(tier4.length)];
        }
        // Breaking — tier3 e tier4 dominam
        float r = RNG.nextFloat();
        if (r < 0.4F) return tier3[RNG.nextInt(tier3.length)];
        return tier4[RNG.nextInt(tier4.length)];
    }

    /** Constrói packet com params apropriados e envia. */
    public static void injectHallucination(ServerPlayer sp, HallucinationType type, int horror) {
        float intensity = 0.3F + (horror / 100.0F) * 0.7F; // 0.3 → 1.0
        int duration = 60 + RNG.nextInt(60); // 3-6 segundos
        String aux = "";
        int variant = 0;

        // Position offset relative to player
        float rx, ry, rz;
        switch (type) {
            case FAKE_FOOTSTEP -> {
                // Atrás do player
                var look = sp.getLookAngle();
                rx = (float)(-look.x * 2.5);
                ry = 0;
                rz = (float)(-look.z * 2.5);
            }
            case FAKE_ENTITY_PERIPHERAL, TEMPORAL_GHOST -> {
                // 8-16 blocos em volta
                double angle = RNG.nextDouble() * Math.PI * 2;
                double dist = 8 + RNG.nextDouble() * 8;
                rx = (float)(Math.cos(angle) * dist);
                ry = 0;
                rz = (float)(Math.sin(angle) * dist);
            }
            case SHADOW_MOVEMENT -> {
                // Lateral próximo
                var look = sp.getLookAngle();
                rx = (float)(-look.z * (3 + RNG.nextDouble()));
                ry = 1F;
                rz = (float)(look.x * (3 + RNG.nextDouble()));
            }
            case FAKE_CHAT_MESSAGE -> {
                rx = ry = rz = 0;
                aux = pickFakeChatMessage(sp, horror);
            }
            case NAME_WHISPER -> {
                rx = ry = rz = 0;
                aux = sp.getName().getString();
            }
            case FAKE_BLOCK_FLASH, FALSE_LIGHT -> {
                // Block próximo
                double angle = RNG.nextDouble() * Math.PI * 2;
                double dist = 2 + RNG.nextDouble() * 5;
                rx = (float)(Math.cos(angle) * dist);
                ry = 0;
                rz = (float)(Math.sin(angle) * dist);
            }
            default -> {
                rx = ry = rz = 0;
            }
        }

        HallucinationS2CPacket pkt = new HallucinationS2CPacket(
                type, rx, ry, rz, duration, intensity, variant, aux);
        ModNetwork.sendToPlayer(sp, pkt);

        LiberthiaMod.LOGGER.debug("[Hallucination] inject {} → {} (horror={}, intensity={})",
                type, sp.getName().getString(), horror, intensity);
    }

    /** Lista de mensagens fake de chat (para FAKE_CHAT_MESSAGE). */
    private static final String[] FAKE_CHAT = {
            "§7[Steve] §fAlguém aí?",
            "§7[???] §fpor que você está aqui?",
            "§7[<§4??§7>] §4eles te seguem",
            "§7[Sistema] §cConexão instável...",
            "§7[Sistema] §c[1.20.1] §cdesync detected",
            "§7[<§4??§7>] §4look behind you",
            "§7* §o§7você ouve uma voz familiar*",
            "§7[Server] §7" + "salvando...",
            "§7[???] §fnão olhe pra cima",
            "§7[<§4??§7>] §4volte",
            "§7* §oseu nome foi mencionado em outro lugar*",
            "§7[Sistema] §7§oentidade próxima",
            "§7[???] §fele está no chunk",
            "§7[Sistema] §7§o[chunk corrupted]"
    };

    private static String pickFakeChatMessage(ServerPlayer sp, int horror) {
        String chosen = FAKE_CHAT[RNG.nextInt(FAKE_CHAT.length)];
        if (horror > InsanityData.THRESHOLD_HIGH && RNG.nextFloat() < 0.3F) {
            // Insert player's own name pra desorientar
            chosen = chosen.replace("???", sp.getName().getString());
        }
        return chosen;
    }

    // ────────── API ──────────

    /**
     * Força uma hallucination específica num player (usado por items, admin
     * commands, etc).
     */
    public static void force(ServerPlayer sp, HallucinationType type,
                              float intensity, int duration, String aux) {
        HallucinationS2CPacket pkt = new HallucinationS2CPacket(
                type, 0, 0, 0, duration, intensity, 0, aux);
        ModNetwork.sendToPlayer(sp, pkt);
        // r52: auto-play sound apropriado pra cada hallucination type
        // (per-player via ClientboundSoundPacket — outros não ouvem)
        try {
            br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager.playForHallucination(sp, type);
        } catch (Throwable ignored) {}
    }

    /** Variante com offset. */
    public static void forceAt(ServerPlayer sp, HallucinationType type,
                                float intensity, int duration,
                                float x, float y, float z, String aux) {
        HallucinationS2CPacket pkt = new HallucinationS2CPacket(
                type, x, y, z, duration, intensity, 0, aux);
        ModNetwork.sendToPlayer(sp, pkt);
    }

    /** Limpa tracking de um player (logout). */
    public static void cleanup(java.util.UUID playerId) {
        LAST_ANALYSIS.remove(playerId);
        LAST_PLAYER_SIGHTING.remove(playerId);
    }
}
