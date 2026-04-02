package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModFluids;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class PurgeInfectionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("purge")
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                    .executes(context -> purge(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
                )
                .executes(context -> purge(context.getSource(), 64))
            )
        );
    }

    private static int purge(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int count = 0;

        source.sendSuccess(() -> Component.literal("§d[Liberthia] Iniciando expurgo de infecção..."), true);

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);

            boolean isInfected = state.is(ModBlocks.DARK_MATTER_BLOCK.get()) || 
                                 state.is(ModBlocks.CORRUPTED_SOIL.get()) || 
                                 state.is(ModBlocks.INFECTION_GROWTH.get()) ||
                                 state.is(ModBlocks.DARK_MATTER_ORE.get()) ||
                                 state.is(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get()) ||
                                 state.is(ModBlocks.DARK_MATTER_FLUID_BLOCK.get()) ||
                                 (!fluid.isEmpty() && (fluid.getType().isSame(ModFluids.DARK_MATTER.get()) || fluid.getType().isSame(ModFluids.FLOWING_DARK_MATTER.get())));

            if (isInfected) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                count++;
            }
        }

        int totalCleared = count;
        source.sendSuccess(() -> Component.literal("§a[Liberthia] Expurgo concluído! " + totalCleared + " pontos de infecção removidos."), true);
        return count;
    }
}
