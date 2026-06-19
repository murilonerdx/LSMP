package br.com.murilo.liberthia.dimension;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * v0.1.22 r73 (r80 REWORK): <b>SpiritSanitySpawner</b> — spawna mobs hostis
 * no Spirit World conforme a sanidade do player CAI.
 *
 * <p>Pattern: <b>quanto menor a sanidade, mais frequente e mais perigoso</b>.
 *
 * <h2>Sanity tiers (r80: Loom Trio)</h2>
 * <ul>
 *   <li><b>Sanity ≥ 60</b>: spawn raro — Loom Peripheral (mais gentil)</li>
 *   <li><b>Sanity 40-60</b>: Peripheral (60%) + Screamer (40%)</li>
 *   <li><b>Sanity 20-40</b>: Peripheral (35%) + Screamer (35%) + Watcher (30%)</li>
 *   <li><b>Sanity &lt; 20</b>: HORROR — todos + Dark Consciousness</li>
 * </ul>
 *
 * <h2>r80 fixes</h2>
 * <ul>
 *   <li>Removido <b>WEAVING_SHADE</b> — substituído por Loom Peripheral/Screamer/Watcher</li>
 *   <li>Removido <b>whisper chat spam</b> — som direto sem mensagem visual</li>
 * </ul>
 *
 * <p>Spawn interval: tick a cada {@code TICK_INTERVAL} ticks por player.
 * Cooldown global de {@code SPAWN_COOLDOWN_TICKS} após cada spawn pra não floodar.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpiritSanitySpawner {

    private static final int TICK_INTERVAL = 200;    // verifica a cada 10s
    private static final int SPAWN_COOLDOWN_TICKS = 600;  // 30s entre spawns por player
    private static final int MIN_SPAWN_DIST = 12;
    private static final int MAX_SPAWN_DIST = 24;
    private static final Random RNG = new Random();

    /** UUID → next allowed spawn tick. */
    private static final java.util.Map<java.util.UUID, Long> nextSpawnTick = new java.util.HashMap<>();

    private SpiritSanitySpawner() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (!SpiritDimension.isInSpiritWorld(sp)) {
            nextSpawnTick.remove(sp.getUUID());
            return;
        }
        long now = level.getGameTime();
        if (now % TICK_INTERVAL != 0) return;

        Long nextOk = nextSpawnTick.get(sp.getUUID());
        if (nextOk != null && now < nextOk) return;

        int sanity = SpiritDimension.getSanity(sp);
        if (sanity > 75) return; // sanity alta = sem spawn extra

        // Determina entity por tier
        EntityType<?> type = pickEntityByTier(sanity);
        if (type == null) return;

        // Determine spawn position
        Vec3 spawnPos = pickSpawnPosition(sp, level);
        if (spawnPos == null) return;

        // Spawn
        var entity = type.create(level);
        if (entity == null) return;
        entity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, RNG.nextFloat() * 360f, 0);
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.NATURAL, null, null);
        }
        level.addFreshEntity(entity);

        // Set cooldown
        long cooldown = SPAWN_COOLDOWN_TICKS - (75 - sanity) * 4L; // sanity baixa = cooldown menor
        nextSpawnTick.put(sp.getUUID(), now + Math.max(100, cooldown));

        // r80: whisper chat spam REMOVIDO. Player sente o spawn pelo som ambiente
        // e pela própria presença da criatura — não precisa de mensagem no chat
        // a cada spawn. Antes: spam infinito de "...alguém te observa..." etc.
    }

    private static EntityType<?> pickEntityByTier(int sanity) {
        int roll = RNG.nextInt(100);
        // r80: substituiu WEAVING_SHADE pela trinca Loom (Watcher/Peripheral/Screamer)
        if (sanity >= 60) {
            // Tier tranquilo — só Peripheral (sente algo na borda da visão)
            return ModEntities.LOOM_PERIPHERAL.get();
        } else if (sanity >= 40) {
            // Tier moderado — Peripheral + Screamer (já começa a se aproximar)
            if (roll < 60) return ModEntities.LOOM_PERIPHERAL.get();
            return ModEntities.LOOM_SCREAMER.get();
        } else if (sanity >= 20) {
            // Tier perigoso — adiciona Watcher (a verdadeira ameaça)
            if (roll < 35) return ModEntities.LOOM_PERIPHERAL.get();
            if (roll < 70) return ModEntities.LOOM_SCREAMER.get();
            return ModEntities.LOOM_WATCHER.get();
        } else {
            // Tier horror — tudo + Dark Consciousness raro
            if (roll < 30) return ModEntities.LOOM_PERIPHERAL.get();
            if (roll < 55) return ModEntities.LOOM_SCREAMER.get();
            if (roll < 85) return ModEntities.LOOM_WATCHER.get();
            return ModEntities.DARK_CONSCIOUSNESS.get();
        }
    }

    private static Vec3 pickSpawnPosition(ServerPlayer player, ServerLevel level) {
        // Random direction + distance, behind player preferably
        for (int attempts = 0; attempts < 8; attempts++) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist = MIN_SPAWN_DIST + RNG.nextDouble() * (MAX_SPAWN_DIST - MIN_SPAWN_DIST);
            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;
            // Find ground
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                (int)x, (int)player.getY(), (int)z);
            int maxY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            net.minecraft.core.BlockPos surfacePos = new net.minecraft.core.BlockPos(pos.getX(), maxY, pos.getZ());
            if (level.getBlockState(surfacePos.below()).isSolid()
                && level.getBlockState(surfacePos).isAir()
                && level.getBlockState(surfacePos.above()).isAir()) {
                return new Vec3(x, maxY, z);
            }
        }
        return null;
    }
}
