package br.com.murilo.liberthia.world;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.data.ChunkInfectionData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.S2CAstaronSyncPacket;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

/**
 * r187 — motor do caos de Astaron. A cada 5s: conta cósmicos, gera spawns extras conforme a
 * radiação (≥20), espalha infecção em radiação alta (≥70) e sincroniza o estado pros clientes.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AstaronTickHandler {
    private AstaronTickHandler() {}

    private static int ticks = 0;
    private static final int INTERVAL = 100;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++ticks < INTERVAL) return;
        ticks = 0;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        AstaronData data = AstaronData.get(server);
        int radiation = data.getRadiation();
        int cosmicCount = data.countCosmicEntities(server);

        if (radiation >= 20) driveSpawns(server, data, radiation, cosmicCount);
        if (radiation >= 70) driveInfection(server, radiation);

        ModNetwork.sendToAll(server, new S2CAstaronSyncPacket(radiation, cosmicCount, data.getSkyColor()));
    }

    private static final List<java.util.function.Supplier<EntityType<? extends Mob>>> POOL = List.of(
            () -> ModEntities.PARASITIC_EYE.get(), () -> ModEntities.CINDER_PARASITE.get(),
            () -> ModEntities.GAZE_LEECH.get(), () -> ModEntities.ROT_EYE.get(),
            () -> ModEntities.GLOOM_MOTH.get(), () -> ModEntities.VOID_TICK.get(),
            () -> ModEntities.DREAD_ORB.get());

    private static void driveSpawns(MinecraftServer server, AstaronData data, int radiation, int liveCount) {
        int target = data.getChaosTarget();
        if (liveCount >= target + 20) return;
        float chance = (radiation - 20) / 80.0f;
        if (liveCount < target) chance = Math.min(1f, chance + 0.3f);
        for (ServerLevel level : server.getAllLevels()) {
            if (!isOverworldLike(level)) continue;
            for (ServerPlayer p : level.players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                if (level.random.nextFloat() > chance) continue;
                spawnCosmicNear(level, p);
            }
        }
    }

    private static void spawnCosmicNear(ServerLevel sl, ServerPlayer p) {
        EntityType<? extends Mob> type = POOL.get(sl.random.nextInt(POOL.size())).get();
        Mob mob = type.create(sl);
        if (mob == null) return;
        double ang = sl.random.nextDouble() * Math.PI * 2, dist = 14 + sl.random.nextInt(8);
        int x = (int) (p.getX() + Math.cos(ang) * dist), z = (int) (p.getZ() + Math.sin(ang) * dist);
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!sl.hasChunkAt(pos)) return;
        mob.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360F, 0F);
        mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        sl.addFreshEntity(mob);
    }

    private static void driveInfection(MinecraftServer server, int radiation) {
        float intensity = (radiation - 70) / 30.0f;
        for (ServerLevel level : server.getAllLevels()) {
            if (!isOverworldLike(level)) continue;
            for (ServerPlayer p : level.players()) {
                if (level.random.nextFloat() > intensity * 0.5f) continue;
                ChunkPos cp = new ChunkPos(p.blockPosition());
                ChunkInfectionData d = ChunkInfectionData.get(level);
                d.setContamination(cp, Math.min(100, d.getContamination(cp) + 10 + (int) (intensity * 20)));
            }
        }
    }

    private static boolean isOverworldLike(ServerLevel level) {
        // só superfícies com céu (overworld + dims customizadas tipo-overworld); exclui Nether/End
        return level.dimensionType().hasSkyLight() && !level.dimensionType().ultraWarm();
    }

    // r187 fix: reseta o contador a cada ciclo de servidor (evita disparo fora de hora em singleplayer)
    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent e) { ticks = 0; }
}
