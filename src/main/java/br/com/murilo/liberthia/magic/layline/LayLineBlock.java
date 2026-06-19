package br.com.murilo.liberthia.magic.layline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.162 r138: <b>Lay Line</b> — bloco natural worldgen-spawned que gera
 * Source ambiente pra players em raio.
 *
 * <p>Inspirado em AN's "Source Nodes" mas simplificado:
 * <ul>
 *   <li>Sem inventory, sem GUI, sem fuel</li>
 *   <li>A cada {@link Tile#TICK_INTERVAL} ticks, distribui {@link Tile#SOURCE_PER_PLAYER}
 *       Source pra cada player em raio {@link Tile#RADIUS}</li>
 *   <li>VFX contínuo (~1 partícula END_ROD/tick)</li>
 *   <li>Indestrutível (hardness 50, blast resistance 1200) — patrimônio do mundo</li>
 * </ul>
 *
 * <p>Spawn natural: vai entrar em data/forge/biome_modifier ou via /place feature.
 */
public class LayLineBlock extends Block implements EntityBlock {

    public LayLineBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof Tile t) Tile.tick(lvl, pos, st, t);
        };
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        // Sinal forte — útil pra detectar com comparator e ativar mecanismo perto
        return 15;
    }

    public static class Tile extends BlockEntity {
        public static final int TICK_INTERVAL = 40;       // 2s
        public static final int SOURCE_PER_PLAYER = 3;    // 1.5 src/s por player
        public static final double RADIUS = 12.0;

        public Tile(BlockPos pos, BlockState state) {
            super(br.com.murilo.liberthia.registry.ModBlockEntities.LAY_LINE.get(), pos, state);
        }

        public static void tick(Level level, BlockPos pos, BlockState state, Tile be) {
            if (!(level instanceof ServerLevel sl)) {
                // Client VFX — anel sutil de partículas
                if (level.random.nextFloat() < 0.4F) {
                    double ang = level.random.nextDouble() * Math.PI * 2;
                    double r = 0.6 + level.random.nextDouble() * 0.4;
                    level.addParticle(ParticleTypes.END_ROD,
                            pos.getX() + 0.5 + Math.cos(ang) * r,
                            pos.getY() + 0.4 + level.random.nextDouble() * 0.6,
                            pos.getZ() + 0.5 + Math.sin(ang) * r,
                            -Math.cos(ang) * 0.02, 0.02, -Math.sin(ang) * 0.02);
                }
                return;
            }

            if (sl.getGameTime() % TICK_INTERVAL != 0) return;

            AABB box = new AABB(pos).inflate(RADIUS);
            for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                int cur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
                int max = br.com.murilo.liberthia.observation.source.SourceData.getMax(sp);
                if (cur >= max) continue;
                int give = Math.min(SOURCE_PER_PLAYER, max - cur);
                br.com.murilo.liberthia.observation.source.SourceData.add(sp, give);
            }

            // VFX: beam vertical sutil pra fora do bloco indicando ativo
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    3, 0.1, 0.5, 0.1, 0.05);
        }
    }
}
