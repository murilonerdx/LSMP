package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import br.com.murilo.liberthia.registry.ModFluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class WhiteMatterBombBlock extends Block {
    public static final BooleanProperty PRIMED = BooleanProperty.create("primed");

    public WhiteMatterBombBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PRIMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRIMED);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(pos) && !state.getValue(PRIMED)) {
            prime(level, pos, state);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(PRIMED) && (player.getItemInHand(hand).getItem() instanceof FlintAndSteelItem || player.getItemInHand(hand).is(Items.FIRE_CHARGE))) {
            prime(level, pos, state);
            player.getItemInHand(hand).hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    private void prime(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(PRIMED, true), 3);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.scheduleTick(pos, this, 80); // 4 seconds
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRIMED)) {
            detonate(level, pos);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRIMED)) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0.0, 0.1, 0.0);
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5), pos.getY() + 0.5, pos.getZ() + 0.5 + (random.nextDouble() - 0.5), 0.0, 0.0, 0.0);
        }
    }

    private void detonate(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 6.0f, false, Level.ExplosionInteraction.NONE); // Safe visual explosion
        level.playSound(null, pos, ModSounds.CLEAR_HUM.get(), net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.5F);

        int radius = 16;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
            if (p.distSqr(pos) <= radius * radius) {
                BlockState pState = level.getBlockState(p);
                FluidState fState = level.getFluidState(p);

                boolean isDark = pState.is(ModBlocks.DARK_MATTER_BLOCK.get()) || 
                                 pState.is(ModBlocks.INFECTION_GROWTH.get()) || 
                                 pState.is(ModBlocks.CORRUPTED_SOIL.get()) ||
                                 pState.is(ModBlocks.DARK_MATTER_ORE.get()) ||
                                 pState.is(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get()) ||
                                 pState.is(ModBlocks.DARK_MATTER_FLUID_BLOCK.get()) ||
                                 pState.getBlock() instanceof br.com.murilo.liberthia.logic.InfectionGrowthBlock ||
                                 !fState.isEmpty() && (fState.getType().isSame(ModFluids.DARK_MATTER.get()) || fState.getType().isSame(ModFluids.FLOWING_DARK_MATTER.get()));

                if (isDark) {
                    // ATOMIC SWEEP: Deep cleanse block and fluid state (Flag 3 | 16 ensures propagation)
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 19); 
                    
                    if (level.random.nextFloat() < 0.15f && level.getBlockState(p.below()).isSolidRender(level, p.below())) {
                        Block flower = level.random.nextBoolean() ? Blocks.DANDELION : Blocks.POPPY;
                        level.setBlockAndUpdate(p, flower.defaultBlockState());
                        level.setBlockAndUpdate(p.below(), Blocks.GRASS_BLOCK.defaultBlockState());
                    }
                }
            }
        }

        level.setDayTime(6000);
        if (level.isRaining() || level.isThundering()) {
            level.setWeatherParameters(6000, 0, false, false);
        }

        // Heal entities in range
        AABB healRadius = new AABB(pos).inflate(16);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, healRadius)) {
            entity.getCapability(br.com.murilo.liberthia.registry.ModCapabilities.INFECTION).ifPresent(data -> {
                data.setInfection(0);
                data.setMutations("");
                data.setDirty(true);
            });
            entity.removeAllEffects(); // Clears all debuffs
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(br.com.murilo.liberthia.registry.ModEffects.CLEAR_SHIELD.get(), 6000));
        }
    }
}
