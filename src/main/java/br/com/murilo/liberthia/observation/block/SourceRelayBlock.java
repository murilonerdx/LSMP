package br.com.murilo.liberthia.observation.block;

import br.com.murilo.liberthia.observation.source.SourceData;
import br.com.murilo.liberthia.observation.source.SourceJarTile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r74: <b>Source Relay Block</b> — transfere Source de Source Jar
 * próximos pra players num raio maior (8b vs jar's 4b).
 *
 * <p>Pattern AN's SourceRelay: relay block extende o range de jars + transfere
 * automaticamente entre jars conectados.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Tick a cada 40t (2s)</li>
 *   <li>Procura Source Jars em raio 4b — se algum tem Source &gt; 100, transfere 5 pra player próximo (raio 8b) que tem Source &lt; max</li>
 *   <li>Transfere entre 2 jars (equaliza levels)</li>
 * </ul>
 */
public class SourceRelayBlock extends Block implements EntityBlock {

    public SourceRelayBlock(Properties p) {
        super(p);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SourceRelayTile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, p, st, be) -> {
            if (be instanceof SourceRelayTile relay) relay.serverTick();
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        // Mostra info
        if (level.getBlockEntity(pos) instanceof SourceRelayTile relay) {
            int connected = relay.countConnectedJars();
            sp.displayClientMessage(Component.literal(
                "§5§l✦ §rSource Relay §7— jars conectados: §e" + connected
                + " §7| raio: §e8 blocos"), true);
        }
        return InteractionResult.CONSUME;
    }

    /** Tile entity simples — só faz tick. */
    public static class SourceRelayTile extends BlockEntity {
        public SourceRelayTile(BlockPos pos, BlockState state) {
            super(br.com.murilo.liberthia.registry.ModBlockEntities.SOURCE_RELAY.get(), pos, state);
        }

        public int countConnectedJars() {
            if (level == null) return 0;
            int count = 0;
            for (BlockPos p : BlockPos.betweenClosed(
                worldPosition.offset(-4, -2, -4),
                worldPosition.offset(4, 2, 4))) {
                if (level.getBlockEntity(p) instanceof SourceJarTile) count++;
            }
            return count;
        }

        public void serverTick() {
            if (level == null || level.isClientSide) return;
            if (level.getGameTime() % 40 != 0) return; // 2s

            // 1) Coleta jars próximos
            java.util.List<SourceJarTile> jars = new java.util.ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(
                worldPosition.offset(-4, -2, -4),
                worldPosition.offset(4, 2, 4))) {
                if (level.getBlockEntity(p) instanceof SourceJarTile j) jars.add(j);
            }
            if (jars.isEmpty()) return;

            // 2) Transfer Source pra players em raio 8b
            for (var player : level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                    new net.minecraft.world.phys.AABB(worldPosition).inflate(8))) {
                if (player instanceof ServerPlayer sp) {
                    int cur = SourceData.get(sp);
                    int max = SourceData.getMax(sp);
                    if (cur >= max) continue;
                    int need = Math.min(5, max - cur);
                    // Procura jar com source suficiente
                    for (var jar : jars) {
                        if (jar.getStored() >= need) {
                            if (jar.tryExtract(need)) {
                                SourceData.add(sp, need);
                                break;
                            }
                        }
                    }
                }
            }

            // 3) Equalize entre jars próximos
            if (jars.size() >= 2) {
                jars.sort((a, b) -> Integer.compare(b.getStored(), a.getStored()));
                var richest = jars.get(0);
                var poorest = jars.get(jars.size() - 1);
                int diff = richest.getStored() - poorest.getStored();
                if (diff > 50) {
                    int transfer = Math.min(10, diff / 4);
                    if (richest.tryExtract(transfer)) {
                        poorest.tryReceive(transfer);
                    }
                }
            }
        }
    }
}
