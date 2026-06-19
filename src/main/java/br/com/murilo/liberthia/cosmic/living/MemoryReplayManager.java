package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r47: <b>Memory Replay Manager</b> — server tick que faz chunks
 * com alta {@code emotionalIndex()} REPLICAR echoes incorretamente.
 *
 * <h2>Por player tracking</h2>
 * Cada player ouve REPLAYS DIFERENTES. Mesmo chunk = experiências
 * únicas. Isso cria desconfiança social ("eu ouvi A, ele ouviu B").
 *
 * <h2>Thresholds</h2>
 * <ul>
 *   <li>Emotional Index ≥ 30: baixa chance (5%/check) de replay sutil</li>
 *   <li>≥ 60: chance moderada (15%) + echoes mais frequentes</li>
 *   <li>≥ 80: chance alta (35%) + chat fake + hallucinations</li>
 * </ul>
 *
 * <h2>Per-player uniqueness</h2>
 * O server escolhe um random seed POR (player, chunk, tick / 200) então
 * 2 players no mesmo chunk veem coisas distintas mas consistentes pra eles
 * por blocos de tempo.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MemoryReplayManager {

    /** Cooldown entre replays per-player em ticks. */
    public static final int REPLAY_COOLDOWN = 100; // 5s
    private static final java.util.Map<java.util.UUID, Long> LAST_REPLAY =
            new java.util.concurrent.ConcurrentHashMap<>();

    private MemoryReplayManager() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = event.getServer().overworld().getGameTime();
        if (now % 60 != 0) return; // checa cada 3s

        for (ServerPlayer sp : event.getServer().getPlayerList().getPlayers()) {
            try {
                tickPlayer(sp, now);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[ChunkMemory] replay error: {}", t.toString());
            }
        }
    }

    private static void tickPlayer(ServerPlayer sp, long now) {
        // Per-player cooldown
        Long last = LAST_REPLAY.get(sp.getUUID());
        if (last != null && now - last < REPLAY_COOLDOWN) return;

        if (sp.isCreative() || sp.isSpectator()) return;

        ServerLevel level = sp.serverLevel();
        ChunkPos cp = new ChunkPos(sp.blockPosition());
        ChunkMemory cm = ChunkMemoryStorage.get(level).peek(cp);
        if (cm == null) return;

        int idx = cm.emotionalIndex();
        if (idx < 30) return;

        // Per-player random seed (chunk + player + time block) — consistent per 10s
        long seed = ((long) cp.x * 31 + cp.z) ^ sp.getUUID().getMostSignificantBits()
                ^ (now / 200);
        java.util.Random rng = new java.util.Random(seed);

        // Chance baseada em emotional index
        float chance;
        if (idx >= 80) chance = 0.35F;
        else if (idx >= 60) chance = 0.15F;
        else chance = 0.05F;
        if (rng.nextFloat() > chance) return;

        // Escolhe um echo random (se houver) ou gera default
        ChunkMemory.Echo echo = cm.echoes.isEmpty() ? defaultEcho(rng) : cm.echoes.get(rng.nextInt(cm.echoes.size()));
        playEcho(sp, level, echo, idx, rng);

        LAST_REPLAY.put(sp.getUUID(), now);
    }

    private static ChunkMemory.Echo defaultEcho(java.util.Random rng) {
        ChunkMemory.Echo.Type[] types = ChunkMemory.Echo.Type.values();
        return new ChunkMemory.Echo(types[rng.nextInt(types.length)], "", null, 0);
    }

    /**
     * Toca um echo específico pro player. Som é POSITIONAL mas só ele ouve
     * (via ClientboundSoundPacket direto pra connection dele).
     */
    private static void playEcho(ServerPlayer sp, ServerLevel level,
                                  ChunkMemory.Echo echo, int emotionalIndex,
                                  java.util.Random rng) {
        // Position random offset 4-12 blocos do player
        double angle = rng.nextDouble() * Math.PI * 2;
        double dist = 4 + rng.nextDouble() * 8;
        double ex = sp.getX() + Math.cos(angle) * dist;
        double ez = sp.getZ() + Math.sin(angle) * dist;
        double ey = sp.getY() + (rng.nextDouble() - 0.5) * 4;

        SoundEvent sound;
        float vol = 0.6F + emotionalIndex / 200.0F;
        float pitch = 0.5F + rng.nextFloat() * 0.5F;

        switch (echo.type) {
            case FOOTSTEP -> {
                sound = SoundEvents.STONE_HIT;
                vol *= 1.2F;
            }
            case MINING -> {
                sound = SoundEvents.STONE_BREAK;
                pitch = 0.7F + rng.nextFloat() * 0.3F;
            }
            case DOOR_OPEN -> {
                sound = SoundEvents.IRON_DOOR_OPEN;
            }
            case CHAT -> {
                // Replay chat — manda fake message só pra esse player
                if (!echo.data.isEmpty()) {
                    String fake = "§7§o<§o" +
                            (echo.origin != null ? echo.origin.toString().substring(0, 5) : "???") +
                            "§7§o> §o" + echo.data;
                    HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE,
                            1.0F, 1, fake);
                    return;
                }
                sound = SoundEvents.AMBIENT_CAVE.value();
            }
            case DEATH_CRY -> {
                sound = SoundEvents.PLAYER_HURT;
                vol *= 1.5F;
                // Bonus: hallucination de damage indicator
                if (rng.nextFloat() < 0.3F) {
                    HallucinationManager.force(sp, HallucinationType.FAKE_DAMAGE_INDICATOR,
                            0.7F, 30, "");
                }
            }
            case BLOCK_BREAK -> {
                sound = SoundEvents.STONE_BREAK;
                pitch = 0.6F + rng.nextFloat() * 0.3F;
            }
            case CONVERSATION -> {
                sound = SoundEvents.AMBIENT_CAVE.value();
                // Chat fragment hallucination
                String[] fragments = {
                        "...você ouve...",
                        "...alguém ali...",
                        "...não consigo mais...",
                        "...eles voltaram...",
                        "...não fale do nome..."
                };
                HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE,
                        1.0F, 1, "§8§o" + fragments[rng.nextInt(fragments.length)]);
            }
            default -> sound = SoundEvents.AMBIENT_CAVE.value();
        }

        // Toca som POSITIONAL exclusivo pro player
        try {
            sp.connection.send(new ClientboundSoundPacket(
                    Holder.direct(sound), SoundSource.AMBIENT,
                    ex, ey, ez, vol, pitch, level.random.nextLong()));
        } catch (Throwable ignored) {}

        // Hallucinations extras pra echoes mais sinistros
        if (emotionalIndex >= 80 && rng.nextFloat() < 0.4F) {
            HallucinationManager.force(sp, HallucinationType.SHADOW_MOVEMENT,
                    0.6F, 30, "");
        }

        LiberthiaMod.LOGGER.debug("[ChunkMemory] {} echo {} at idx={}",
                sp.getName().getString(), echo.type, emotionalIndex);
    }

    public static void cleanupPlayer(java.util.UUID uuid) {
        LAST_REPLAY.remove(uuid);
    }
}
