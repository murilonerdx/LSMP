package br.com.murilo.liberthia.observation.source;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r62: <b>Source Jar Block</b> — storage block pra Source.
 *
 * <p>Inspired by AN's SourceJarTile: armazena até 10000 Source, fill state
 * 0-11 visível externamente. Players podem deposit/extract right-clicking
 * com Source Gem.
 *
 * <h2>Block state</h2>
 * Property {@code FILL} 0-11 — controla model variant via blockstate JSON
 * (a textura muda baseado em quanto Source tem).
 */
public class SourceJarBlock extends Block implements EntityBlock {

    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 11);

    public SourceJarBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(2.5F, 6.0F)
            .lightLevel(state -> 5 + state.getValue(FILL))  // brilha mais se cheio
            .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) {
        b.add(FILL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SourceJarTile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof SourceJarTile jar) jar.serverTick();
        };
    }

    /** Right-click — mostra source atual e tenta receber/extract via Source Gem. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SourceJarTile jar)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Player segurando Source Gem → deposita gem na jar (+25 Source)
        if (held.is(br.com.murilo.liberthia.registry.ModItems.SOURCE_GEM.get())) {
            if (jar.tryReceive(25)) {
                held.shrink(1);
                sp.displayClientMessage(Component.literal(
                    "§5§l+25 Source §r§7→ §eJar§r §7(" + jar.getStored() + "/" + jar.getCapacity() + ")"), true);
                return InteractionResult.CONSUME;
            } else {
                sp.displayClientMessage(Component.literal("§c⚠ Jar cheia."), true);
                return InteractionResult.FAIL;
            }
        }

        // Player sem item — extrai 25 Source pro player se ele tiver Source < max
        int playerCurrent = SourceData.get(sp);
        int playerMax = SourceData.getMax(sp);
        if (playerCurrent < playerMax && jar.getStored() >= 25) {
            int gained = Math.min(25, playerMax - playerCurrent);
            if (jar.tryExtract(gained)) {
                SourceData.add(sp, gained);
                sp.displayClientMessage(Component.literal(
                    "§e§l✦ §r§5+" + gained + " Source§r §7→ §eVocê§r §7(" + SourceData.get(sp) + "/" + playerMax + ")"), true);
                return InteractionResult.CONSUME;
            }
        }

        // Só mostra info
        sp.displayClientMessage(Component.literal(
            "§eJar§r §7| §5" + jar.getStored() + "§7/§5" + jar.getCapacity()), true);
        return InteractionResult.SUCCESS;
    }
}
