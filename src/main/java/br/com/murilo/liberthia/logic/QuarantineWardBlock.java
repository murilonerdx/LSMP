package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Zona de Quarentena — bloco de proteção anti-infecção.
 * AGE 0-3: corrosão progressiva quando cercado por infecção.
 * AGE 3 + condições = bloco quebra.
 */
public class QuarantineWardBlock extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public QuarantineWardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            int age = state.getValue(AGE);
            int integrity = 100 - (age * 25);
            String bar = "\u2593".repeat(4 - age) + "\u2591".repeat(age);
            ChatFormatting color = age == 0 ? ChatFormatting.GREEN : age <= 2 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            player.displayClientMessage(Component.literal("\u26A1 ")
                    .append(Component.translatable("block.liberthia.quarantine_ward").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" [" + bar + "] " + integrity + "%").withStyle(color))
                    , false);
            if (age >= 3) {
                player.displayClientMessage(Component.literal("  \u26A0 ")
                        .append(Component.translatable("chat.liberthia.ward_compromised").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        , false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;

        int age = state.getValue(AGE);

        // Count infection blocks in 5x3x5 area
        int infectionCount = 0;
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            BlockState s = level.getBlockState(scan);
            if (isInfectionBlock(s)) {
                infectionCount++;
            }
        }

        // Corrode when surrounded by infection
        if (infectionCount > 4 && age < 3 && random.nextFloat() < 0.15f) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
        }

        // At max corrosion: 10% chance to break
        if (age >= 3 && random.nextFloat() < 0.10f) {
            level.destroyBlock(pos, true);
            return;
        }

        // Protective particle effect
        if (random.nextFloat() < 0.3f) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    3 - age, 0.5, 0.3, 0.5, 0.02
            );
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        int age = state.getValue(AGE);
        // Healthy ward: bright particles. Corroded: dim/red particles
        if (random.nextFloat() < 0.4f - age * 0.1f) {
            level.addParticle(
                    age < 2 ? ParticleTypes.END_ROD : ParticleTypes.SMOKE,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.0 + random.nextDouble() * 0.5,
                    pos.getZ() + random.nextDouble(),
                    0, 0.02, 0
            );
        }
    }

    private static boolean isInfectionBlock(BlockState s) {
        return s.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || s.is(ModBlocks.CORRUPTED_SOIL.get())
                || s.is(ModBlocks.INFECTION_GROWTH.get())
                || s.is(ModBlocks.INFECTION_VEIN.get())
                || s.is(ModBlocks.CORRUPTED_STONE.get())
                || s.is(ModBlocks.DARK_MATTER_FLUID_BLOCK.get())
                || s.is(ModBlocks.SPORE_BLOOM.get());
    }
}
