package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class ProtectionUtils {

    private ProtectionUtils() {}

    public static boolean isClearMatterBlock(BlockState state) {
        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
            return true;
        }
        return hasRegistryPath(state, "clear_matter")
                || hasRegistryPath(state, "white_matter")
                || hasRegistryPath(state, "materia_branca");
    }

    public static boolean isYellowMatterBlock(BlockState state) {
        return hasRegistryPath(state, "yellow_matter")
                || hasRegistryPath(state, "yellowmatter")
                || hasRegistryPath(state, "materia_amarela");
    }

    public static boolean hasRegistryPath(BlockState state, String expectedFragment) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) {
            return false;
        }
        return key.getPath().contains(expectedFragment);
    }

    /**
     * Clear matter protection: 7x3x7 area (radius 3 XZ, 1 Y).
     * Increased from original 3x3x3 to give clear matter a meaningful purification zone.
     */
    public static boolean hasClearMatterProtection(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
            if (isClearMatterBlock(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Yellow matter protection: 5x3x5 area (radius 2 XZ, 1 Y).
     * Hard stop — infection cannot advance at all within this radius.
     */
    public static boolean hasYellowMatterProtection(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (isYellowMatterBlock(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSpreadBlockedByProtectiveBlocks(Level level, BlockPos center) {
        if (hasYellowMatterProtection(level, center)) {
            return true;
        }
        if (hasClearMatterProtection(level, center)) {
            return true;
        }
        return hasQuarantineWardProtection(level, center);
    }

    /**
     * F3: Quarantine Ward protection: 9x5x9 area (radius 4 XZ, 2 Y).
     */
    public static boolean hasQuarantineWardProtection(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            if (level.getBlockState(pos).is(ModBlocks.QUARANTINE_WARD.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * F9: Replace infection blocks with scarred variants.
     * CORRUPTED_SOIL → SCARRED_EARTH, DARK_MATTER/CORRUPTED_STONE → SCARRED_STONE,
     * INFECTION_GROWTH/SPORE_BLOOM/DARK_MATTER_FLUID → AIR
     */
    public static void replaceInfectionWithScar(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.CORRUPTED_SOIL.get())) {
            level.setBlockAndUpdate(pos, ModBlocks.SCARRED_EARTH.get().defaultBlockState());
        } else if (state.is(ModBlocks.DARK_MATTER_BLOCK.get()) || state.is(ModBlocks.CORRUPTED_STONE.get())) {
            level.setBlockAndUpdate(pos, ModBlocks.SCARRED_STONE.get().defaultBlockState());
        } else if (state.is(ModBlocks.INFECTION_GROWTH.get()) || state.is(ModBlocks.SPORE_BLOOM.get())
                || state.is(ModBlocks.INFECTION_VEIN.get())) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        } else if (state.is(ModBlocks.DARK_MATTER_FLUID_BLOCK.get())) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        } else if (state.is(ModBlocks.CORRUPTED_LOG.get())) {
            level.setBlockAndUpdate(pos, ModBlocks.SCARRED_STONE.get().defaultBlockState());
        } else if (state.is(ModBlocks.GLITCH_BLOCK.get())) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        }
    }

    /**
     * Check if a position is blocked by non-dark-matter fluid (water, lava, etc.).
     * Infection cannot cross water/ocean/fluid barriers.
     */
    public static boolean isWaterBarrier(Level level, BlockPos pos) {
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            FluidState fluid = level.getFluidState(scan);
            if (!fluid.isEmpty()
                    && !fluid.getType().isSame(ModFluids.DARK_MATTER.get())
                    && !fluid.getType().isSame(ModFluids.FLOWING_DARK_MATTER.get())) {
                if (fluid.is(FluidTags.WATER) || fluid.is(FluidTags.LAVA)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if any InfectionGrowth block exists within the given minimum spacing.
     * Scans a (2*spacing+1) x 7 x (2*spacing+1) volume with early exit for performance.
     */
    public static boolean hasGrowthTooClose(ServerLevel level, BlockPos pos, int minSpacing) {
        int verticalRange = Math.min(minSpacing / 2, 3);
        for (BlockPos scan : BlockPos.betweenClosed(
                pos.offset(-minSpacing, -verticalRange, -minSpacing),
                pos.offset(minSpacing, verticalRange, minSpacing))) {
            if (scan.equals(pos)) {
                continue;
            }
            if (level.getBlockState(scan).is(ModBlocks.INFECTION_GROWTH.get())) {
                return true;
            }
        }
        return false;
    }
}
