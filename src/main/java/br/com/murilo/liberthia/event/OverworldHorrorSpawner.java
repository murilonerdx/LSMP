package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.ICosmicHorror;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * r183 — <b>Spawner dirigido de horror</b>: aproxima as 14 criaturas cósmicas de players
 * com SANIDADE &lt; 40% à NOITE e no ESCURO (overworld e dims normais). Spawna atrás do
 * player; frequência cresce quanto MENOR a sanidade. Não spawna de dia/iluminado/sanidade
 * alta. As entidades de ambush são efêmeras (somem após um tempo); só as de ovo ficam.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class OverworldHorrorSpawner {
    private OverworldHorrorSpawner() {}

    private static List<EntityType<?>> pool;

    @SuppressWarnings("unchecked")
    private static List<EntityType<?>> pool() {
        if (pool == null) {
            pool = new ArrayList<>();
            // comuns (peso maior)
            EntityType<?>[] common = {
                ModEntities.CINDER_PARASITE.get(), ModEntities.WHISPER_MITE.get(), ModEntities.GLOOM_MOTH.get(),
                ModEntities.VOID_TICK.get(), ModEntities.ROT_EYE.get(), ModEntities.SCREAM_LARVA.get(),
                ModEntities.GAZE_LEECH.get(), ModEntities.MAW_CRAWLER.get(), ModEntities.MIRROR_SPAWN.get()
            };
            EntityType<?>[] rare = {
                ModEntities.BLIND_WEAVER.get(), ModEntities.FLESH_WATCHER.get(), ModEntities.DREAD_ORB.get(),
                ModEntities.PARASITE_HOST.get()
            };
            for (int i = 0; i < 3; i++) for (EntityType<?> e : common) pool.add(e);
            for (EntityType<?> e : rare) pool.add(e);
            pool.add(ModEntities.COLOSSAL_EYE.get()); // muito raro (1)
        }
        return pool;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null || server.getTickCount() % 100 != 0) return;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (SpiritDimension.isInSpiritWorld(p)) continue; // spirit tem spawner próprio
            int sanity = SpiritDimension.getSanity(p);
            if (sanity >= 40) continue;
            ServerLevel sl = p.serverLevel();
            if (sl.isDay()) continue;
            BlockPos at = p.blockPosition();
            if (sl.getMaxLocalRawBrightness(at) > 7) continue;

            float factor = (40 - sanity) / 40.0F;            // 0..1 (mais baixo = mais)
            if (sl.random.nextFloat() > 0.18F * factor) continue;
            if (countHorrorNear(sl, p) >= 4) continue;

            spawnBehind(sl, p);
        }
    }

    private static int countHorrorNear(ServerLevel sl, ServerPlayer p) {
        int n = 0;
        for (Entity e : sl.getEntities(p, p.getBoundingBox().inflate(48))) if (e instanceof ICosmicHorror) n++;
        return n;
    }

    private static void spawnBehind(ServerLevel sl, ServerPlayer p) {
        var back = p.getViewVector(1f).scale(-1);
        for (int tries = 0; tries < 6; tries++) {
            double dist = 16 + sl.random.nextInt(11);
            double ox = back.x * dist + (sl.random.nextDouble() - 0.5) * 8;
            double oz = back.z * dist + (sl.random.nextDouble() - 0.5) * 8;
            int x = (int) (p.getX() + ox), z = (int) (p.getZ() + oz);
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (sl.getMaxLocalRawBrightness(pos) > 7) continue;
            EntityType<?> type = pool().get(sl.random.nextInt(pool().size()));
            Entity ent = type.create(sl);
            if (!(ent instanceof Mob mob)) { if (ent != null) ent.discard(); return; }
            mob.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360F, 0);
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            sl.addFreshEntity(mob);
            return;
        }
    }
}
