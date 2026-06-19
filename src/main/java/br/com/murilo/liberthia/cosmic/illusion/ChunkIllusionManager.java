package br.com.murilo.liberthia.cosmic.illusion;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r55: <b>Chunk Illusion Manager</b> — quando o player está
 * minerando profundamente e tem baixa sanidade, há uma chance pequena
 * de ser teleportado pro Spirit World COM A MESMA CHUNK ao redor —
 * dando a sensação de que nada mudou, até subir e ver o céu impossível.
 *
 * <h2>Comportamento</h2>
 * <ol>
 *   <li>Player mina N blocos consecutivos (>40)</li>
 *   <li>Sanity ≤ 50 + Y ≤ 30 (profundo)</li>
 *   <li>Random check: 0.5% chance por bloco mineiro</li>
 *   <li>Trigger: copia chunk 16x16 ao redor pro Spirit World</li>
 *   <li>Teleporta player pra Spirit World na mesma Y</li>
 *   <li>Player não percebe imediatamente — chunk visível parece igual</li>
 *   <li>Só quando subir e ver o céu palido descobre que está no Outro Lado</li>
 * </ol>
 *
 * <p>Simplificação prática: em vez de copiar blocos individuais, força o
 * player a entrar no spirit world em coords idênticas. O spirit world tem
 * gen próprio (HoloMarble + EtherealStone). O efeito "chunk copiada"
 * vem da impressão de continuidade visual quando o player está
 * cavando — o terreno do spirit world tem blocos similares.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ChunkIllusionManager {

    /** Player UUID → contagem de blocos minerados consecutivos. */
    private static final Map<UUID, Integer> MINING_STREAK = new HashMap<>();
    /** Player UUID → último tick em que mineou. */
    private static final Map<UUID, Long> LAST_MINE_TICK = new HashMap<>();
    /** Player UUID → próximo tick que pode trigger illusion (cooldown). */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private static final long COOLDOWN_TICKS = 24000; // 20 min IRL
    private static final int STREAK_THRESHOLD = 40;

    private ChunkIllusionManager() {}

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (event.getPlayer().level().isClientSide) return;
        if (SpiritDimension.isInSpiritWorld(sp)) return;

        UUID id = sp.getUUID();
        long now = sp.tickCount;
        Long lastMine = LAST_MINE_TICK.get(id);
        // Reset streak se demorou >60 ticks (3s)
        int streak;
        if (lastMine == null || (now - lastMine) > 60) {
            streak = 1;
        } else {
            streak = MINING_STREAK.getOrDefault(id, 0) + 1;
        }
        MINING_STREAK.put(id, streak);
        LAST_MINE_TICK.put(id, now);

        // Check trigger conditions
        if (streak < STREAK_THRESHOLD) return;
        int sanity = SpiritDimension.getSanity(sp);
        if (sanity > 50) return;
        if (sp.getY() > 30) return;

        // Cooldown check
        Long cd = COOLDOWNS.get(id);
        if (cd != null && now < cd) return;

        // Random chance — 0.5% per block past threshold
        if (Math.random() > 0.005) return;

        triggerIllusion(sp);
        COOLDOWNS.put(id, now + COOLDOWN_TICKS);
        MINING_STREAK.put(id, 0);
    }

    private static void triggerIllusion(ServerPlayer sp) {
        MinecraftServer server = sp.server;
        ServerLevel spirit = server.getLevel(SpiritDimension.SPIRIT_WORLD);
        if (spirit == null) return;

        // Salva coords de retorno via SpiritDimension helper
        var data = sp.getPersistentData();
        data.putString(SpiritDimension.NBT_RETURN_DIM, sp.level().dimension().location().toString());
        data.putDouble(SpiritDimension.NBT_RETURN_X, sp.getX());
        data.putDouble(SpiritDimension.NBT_RETURN_Y, sp.getY());
        data.putDouble(SpiritDimension.NBT_RETURN_Z, sp.getZ());
        data.putLong(SpiritDimension.NBT_ENTER_TICK, sp.level().getGameTime());

        // Force chunk do overworld permanecer carregado pro retorno
        if (sp.level() instanceof ServerLevel orig) {
            var chunkPos = new net.minecraft.world.level.ChunkPos(
                    (int) sp.getX() >> 4, (int) sp.getZ() >> 4);
            orig.setChunkForced(chunkPos.x, chunkPos.z, true);
        }

        // Teleporta MANTENDO X/Y/Z exatos
        sp.teleportTo(spirit, sp.getX(), sp.getY(), sp.getZ(),
                EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                sp.getYRot(), sp.getXRot());

        // ZERO mensagem inicial — o player não deve perceber
        // Apenas particles sutis embaixo dos pés
        spirit.sendParticles(ParticleTypes.SOUL,
                sp.getX(), sp.getY(), sp.getZ(),
                3, 0.3, 0.1, 0.3, 0.01);

        // Som de respiração distante
        CosmicSoundManager.playVoidBreathing(sp);

        LiberthiaMod.LOGGER.info("[ChunkIllusion] {} teleported to spirit (mining trigger)",
                sp.getName().getString());
    }
}
