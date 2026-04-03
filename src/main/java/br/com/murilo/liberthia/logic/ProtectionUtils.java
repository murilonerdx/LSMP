package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
        return hasClearMatterProtection(level, center);
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
