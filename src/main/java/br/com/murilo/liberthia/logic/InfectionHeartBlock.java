package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.block.entity.InfectionHeartBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Coração da Infecção — bloco boss que pulsa, spawna mobs, e limpa infecção ao ser destruído.
 */
public class InfectionHeartBlock extends BaseEntityBlock {

    public InfectionHeartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfectionHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.INFECTION_HEART.get(), InfectionHeartBlockEntity::tick);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            float density = InfectionLogic.getChunkInfectionDensity(serverLevel, pos);
            int pct = (int) (density * 100);
            player.displayClientMessage(Component.literal("")
                    .append(Component.literal("\u2764 ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.translatable("block.liberthia.infection_heart").withStyle(ChatFormatting.LIGHT_PURPLE))
                    , false);
            player.displayClientMessage(Component.literal("  \u25B6 ")
                    .append(Component.translatable("chat.liberthia.heart_density", pct + "%").withStyle(ChatFormatting.RED))
                    , false);
            String bar = "\u2593".repeat(Math.max(0, pct / 10)) + "\u2591".repeat(Math.max(0, 10 - pct / 10));
            player.displayClientMessage(Component.literal("  [" + bar + "]").withStyle(ChatFormatting.DARK_RED), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Heart was destroyed — purge infection area
            if (level instanceof ServerLevel serverLevel) {
                InfectionHeartBlockEntity.purgeArea(serverLevel, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        // Pulsing particles
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.5,
                    pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 1.5,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.5,
                    0, 0.05, 0);
        }
        if (random.nextFloat() < 0.5f) {
            level.addParticle(ParticleTypes.SCULK_CHARGE_POP,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.3, 0.1, (random.nextDouble() - 0.5) * 0.3);
        }
    }
}
