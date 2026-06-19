package br.com.murilo.liberthia.dimension.proc;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.LiminalDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r58: <b>PROCEDURAL STRANGENESS ENGINE</b> — gera arquitetura
 * liminal dinâmica nas 3 dimensões a partir da posição do player.
 *
 * <h2>Filosofia</h2>
 * Não pré-gera o mundo todo. Quando o player se move em uma das dims liminais,
 * o engine constrói rooms 16x6x16 ao redor dele on-the-fly, escolhendo um
 * template baseado em weighted random + player NBT seed.
 *
 * <h2>5 Templates (esboço inicial — só 2 implementados nesta rev)</h2>
 * <ol>
 *   <li>YELLOW_COMPLEX — paredes amarelas, carpete verde-escuro, salas vazias</li>
 *   <li>FLOODED_CONCRETE — concreto cinza, water nivel 1, tubulações</li>
 *   <li>IMPOSSIBLE_HOTEL — paredes vermelhas, tapete, portas (futuro)</li>
 *   <li>INFINITE_STAIRWELL — escada infinita (futuro)</li>
 *   <li>FALSE_SUBURB — gramado pálido, casas idênticas (futuro)</li>
 * </ol>
 *
 * <h2>Performance</h2>
 * Rooms são geradas em chunks 16×16. Cada chunk só é gerado UMA vez por
 * dimensão por sessão (rastreado via Set GENERATED_CHUNKS).
 *
 * <h2>Seed determinístico</h2>
 * Cada chunk usa hash(chunkX, chunkZ, dim) como seed — mesma posição sempre
 * gera o mesmo room. Mas a CONEXÃO entre chunks pode mudar com o tempo
 * (doors podem teleportar pra lugares random).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ProceduralStrangeness {

    /** Chunks já gerados — dim → set de (chunkX, chunkZ). */
    private static final Map<String, Set<Long>> GENERATED_CHUNKS = new ConcurrentHashMap<>();

    /** Player UUID → último chunk pos checado (pra evitar over-generation). */
    private static final Map<UUID, Long> LAST_CHECKED = new HashMap<>();

    /** Tamanho de cada room. */
    public static final int ROOM_SIZE = 16;
    public static final int ROOM_HEIGHT = 6;

    private ProceduralStrangeness() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!LiminalDimensions.isInLiminal(sp)) return;
        if (sp.tickCount % 20 != 0) return; // 1Hz

        UUID id = sp.getUUID();
        long chunkKey = (((long)(sp.getBlockX() >> 4)) << 32) | ((long)(sp.getBlockZ() >> 4) & 0xFFFFFFFFL);
        Long last = LAST_CHECKED.get(id);
        if (last != null && last == chunkKey) return;
        LAST_CHECKED.put(id, chunkKey);

        // Generate current chunk + 8 neighbors
        int cx = sp.getBlockX() >> 4;
        int cz = sp.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ensureChunk(sp, cx + dx, cz + dz);
            }
        }
    }

    private static void ensureChunk(ServerPlayer sp, int chunkX, int chunkZ) {
        if (!(sp.level() instanceof ServerLevel level)) return;
        String dimKey = level.dimension().location().toString();
        Set<Long> done = GENERATED_CHUNKS.computeIfAbsent(dimKey, k -> ConcurrentHashMap.newKeySet());
        long key = (((long)chunkX) << 32) | ((long)chunkZ & 0xFFFFFFFFL);
        if (done.contains(key)) return;
        done.add(key);

        // Pick template based on dim + chunk hash
        long seed = (long)dimKey.hashCode() * 31L + chunkX * 73L + chunkZ * 37L;
        Random rng = new Random(seed);

        // Pick template
        int template;
        if (LiminalDimensions.isUpsideSea(sp)) {
            template = rng.nextInt(2); // 0=hotel, 1=flooded
        } else if (LiminalDimensions.isFoldedCity(sp)) {
            template = rng.nextInt(2) + 1; // 1=flooded, 2=yellow
        } else {
            template = 2; // wooden -> yellow as base
        }

        int worldY;
        try {
            // Use a level Y as floor
            BlockPos centerXZ = new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8);
            worldY = (int) Math.max(60, sp.getY()) - 1; // floor just below player Y
        } catch (Throwable ignored) {
            worldY = 64;
        }

        generateRoom(level, chunkX, chunkZ, worldY, template, rng);
    }

    /** Gera uma sala 16×6×16. */
    private static void generateRoom(ServerLevel level, int chunkX, int chunkZ,
                                      int floorY, int template, Random rng) {
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        BlockState floor, wall, ceiling, light;
        boolean addWater = false;

        switch (template) {
            case 0 -> { // YELLOW_COMPLEX
                floor = Blocks.GREEN_WOOL.defaultBlockState();        // damp carpet
                wall = Blocks.YELLOW_TERRACOTTA.defaultBlockState();  // yellow walls
                ceiling = Blocks.WHITE_CONCRETE.defaultBlockState();
                light = Blocks.WHITE_CONCRETE.defaultBlockState();
            }
            case 1 -> { // FLOODED_CONCRETE
                floor = Blocks.SMOOTH_STONE.defaultBlockState();
                wall = Blocks.STONE_BRICKS.defaultBlockState();
                ceiling = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                light = Blocks.SEA_LANTERN.defaultBlockState();
                addWater = true;
            }
            case 2 -> { // IMPOSSIBLE_HOTEL
                floor = Blocks.RED_WOOL.defaultBlockState();
                wall = Blocks.SMOOTH_QUARTZ.defaultBlockState();
                ceiling = Blocks.OAK_PLANKS.defaultBlockState();
                light = Blocks.SHROOMLIGHT.defaultBlockState();
            }
            default -> {
                floor = Blocks.STONE.defaultBlockState();
                wall = Blocks.COBBLESTONE.defaultBlockState();
                ceiling = Blocks.STONE.defaultBlockState();
                light = Blocks.GLOWSTONE.defaultBlockState();
            }
        }

        // Build the room
        for (int x = 0; x < ROOM_SIZE; x++) {
            for (int z = 0; z < ROOM_SIZE; z++) {
                BlockPos floorPos = new BlockPos(baseX + x, floorY, baseZ + z);
                BlockPos ceilPos = new BlockPos(baseX + x, floorY + ROOM_HEIGHT, baseZ + z);
                level.setBlock(floorPos, floor, 2);
                level.setBlock(ceilPos, ceiling, 2);
                // Walls — outer ring
                if (x == 0 || x == ROOM_SIZE - 1 || z == 0 || z == ROOM_SIZE - 1) {
                    for (int y = 1; y < ROOM_HEIGHT; y++) {
                        level.setBlock(new BlockPos(baseX + x, floorY + y, baseZ + z), wall, 2);
                    }
                } else {
                    // Air inside
                    for (int y = 1; y < ROOM_HEIGHT; y++) {
                        level.setBlock(new BlockPos(baseX + x, floorY + y, baseZ + z),
                                Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Doors — random gaps in walls (1-3 doorways)
        int doors = 1 + rng.nextInt(3);
        for (int d = 0; d < doors; d++) {
            int side = rng.nextInt(4); // 0=N, 1=S, 2=W, 3=E
            int pos = 4 + rng.nextInt(8); // middle of wall
            for (int dy = 1; dy < 3; dy++) {
                int dx, dz;
                switch (side) {
                    case 0 -> { dx = pos; dz = 0; }
                    case 1 -> { dx = pos; dz = ROOM_SIZE - 1; }
                    case 2 -> { dx = 0; dz = pos; }
                    default -> { dx = ROOM_SIZE - 1; dz = pos; }
                }
                level.setBlock(new BlockPos(baseX + dx, floorY + dy, baseZ + dz),
                        Blocks.AIR.defaultBlockState(), 2);
            }
        }

        // Lights on ceiling
        int lights = 2 + rng.nextInt(3);
        for (int l = 0; l < lights; l++) {
            int lx = 2 + rng.nextInt(ROOM_SIZE - 4);
            int lz = 2 + rng.nextInt(ROOM_SIZE - 4);
            level.setBlock(new BlockPos(baseX + lx, floorY + ROOM_HEIGHT - 1, baseZ + lz),
                    light, 2);
        }

        // Add water if flooded template
        if (addWater) {
            for (int x = 1; x < ROOM_SIZE - 1; x++) {
                for (int z = 1; z < ROOM_SIZE - 1; z++) {
                    if (rng.nextInt(100) < 60) {
                        level.setBlock(new BlockPos(baseX + x, floorY + 1, baseZ + z),
                                Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Occasional debris/random props (5% per tile)
        for (int x = 2; x < ROOM_SIZE - 2; x++) {
            for (int z = 2; z < ROOM_SIZE - 2; z++) {
                if (rng.nextInt(100) < 2) {
                    BlockState prop = pickProp(template, rng);
                    if (prop != null) {
                        level.setBlock(new BlockPos(baseX + x, floorY + 1, baseZ + z), prop, 2);
                    }
                }
            }
        }

        LiberthiaMod.LOGGER.debug("[ProceduralStrangeness] Generated room ({},{}) template={}",
                chunkX, chunkZ, template);
    }

    private static BlockState pickProp(int template, Random rng) {
        return switch (template) {
            case 0 -> rng.nextBoolean()
                    ? Blocks.YELLOW_CARPET.defaultBlockState()
                    : Blocks.SOUL_LANTERN.defaultBlockState();
            case 1 -> rng.nextBoolean()
                    ? Blocks.RAIL.defaultBlockState()
                    : Blocks.IRON_BARS.defaultBlockState();
            case 2 -> rng.nextBoolean()
                    ? Blocks.OAK_DOOR.defaultBlockState()
                    : Blocks.LECTERN.defaultBlockState();
            default -> null;
        };
    }

    public static void cleanup(UUID id) {
        LAST_CHECKED.remove(id);
    }
}
