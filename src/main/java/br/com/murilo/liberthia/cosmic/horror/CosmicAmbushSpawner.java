package br.com.murilo.liberthia.cosmic.horror;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * r164: Spawn "do nada" para os cosmic horrors (Observer, Empty Man, Absence,
 * Remembered) — eles aparecem perto do player em escuridão, NÃO via spawn
 * natural normal (que daria mob clusters indesejados).
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Verifica players a cada 5 segundos</li>
 *   <li>Player no escuro (light &lt; 4) em Spirit World/Loom/Folded City</li>
 *   <li>~1.5% chance por verificação → spawn 1 cosmic horror random a 15-25 blocos atrás do player</li>
 *   <li>Limite: máx 3 cosmic horrors num raio de 64 blocos do player</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CosmicAmbushSpawner {

    private static final Random RNG = new Random();
    private static final int CHECK_INTERVAL = 100; // 5s
    private static final ResourceLocation SPIRIT_WORLD =
            new ResourceLocation(LiberthiaMod.MODID, "spirit_world");
    private static final ResourceLocation LOOM =
            new ResourceLocation(LiberthiaMod.MODID, "loom_world");
    private static final ResourceLocation FOLDED_CITY =
            new ResourceLocation(LiberthiaMod.MODID, "folded_city");
    private static int tickCounter = 0;

    private CosmicAmbushSpawner() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation dim = level.dimension().location();
            if (!dim.equals(SPIRIT_WORLD) && !dim.equals(LOOM) && !dim.equals(FOLDED_CITY)) {
                continue;
            }
            for (ServerPlayer p : level.players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                tryAmbush(level, p);
            }
        }
    }

    private static void tryAmbush(ServerLevel sl, ServerPlayer p) {
        // Only in dark areas
        int light = sl.getBrightness(LightLayer.BLOCK, p.blockPosition());
        if (light > 4) return;

        // 1.5% chance per check (5s) — about 1-2 ambushes per minute on average
        if (RNG.nextFloat() > 0.015F) return;

        // Limit: count existing cosmic horrors within 64 blocks
        long nearby = sl.getEntities(null, p.getBoundingBox().inflate(64))
                .stream()
                .filter(e -> {
                    var type = e.getType();
                    return type == ModEntities.OBSERVER.get()
                            || type == ModEntities.EMPTY_MAN.get()
                            || type == ModEntities.ABSENCE.get()
                            || type == ModEntities.REMEMBERED.get();
                })
                .count();
        if (nearby >= 3) return;

        // Pick random cosmic horror with weights
        int roll = RNG.nextInt(100);
        EntityType<?> type;
        if (roll < 50)      type = ModEntities.EMPTY_MAN.get();
        else if (roll < 75) type = ModEntities.OBSERVER.get();   // raro mas presente
        else if (roll < 90) type = ModEntities.ABSENCE.get();
        else                type = ModEntities.REMEMBERED.get();

        // Spawn 15-25 blocos ATRÁS do player, longe da visão
        float yaw = p.getYRot() + 180F;  // direção oposta
        double dist = 15 + RNG.nextDouble() * 10;
        double angRad = Math.toRadians(yaw);
        double dx = -Math.sin(angRad) * dist;
        double dz =  Math.cos(angRad) * dist;
        BlockPos target = p.blockPosition().offset((int)dx, 0, (int)dz);
        // Achar Y válido (chão)
        int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
        target = new BlockPos(target.getX(), y, target.getZ());

        if (!sl.isLoaded(target)) return;
        if (!SpawnPlacements.checkSpawnRules(type, sl, MobSpawnType.NATURAL,
                target, sl.random)) {
            // não pode spawnar lá — desiste
            return;
        }
        var mob = type.spawn(sl, target, MobSpawnType.NATURAL);
        if (mob != null) {
            LiberthiaMod.LOGGER.debug("[CosmicAmbush] {} spawned for player {} at {} (light {})",
                    type.getDescriptionId(), p.getName().getString(), target, light);
        }
    }
}
