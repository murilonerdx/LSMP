package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class RadiationGuideOverlay implements IGuiOverlay {

    public static final RadiationGuideOverlay INSTANCE = new RadiationGuideOverlay();

    // Cached values to avoid scanning every frame
    private int cachedParticles;
    private float cachedDensity;
    private int cachedBestDir = -1;
    private long lastScanTick;

    private RadiationGuideOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean holdingGeiger = main.is(ModItems.GEIGER_COUNTER.get()) || off.is(ModItems.GEIGER_COUNTER.get());
        if (!holdingGeiger) return;

        Level level = player.level();
        BlockPos pos = player.blockPosition();

        // Scan every 10 ticks to save performance
        long tick = player.tickCount;
        if (tick - lastScanTick >= 10) {
            lastScanTick = tick;
            cachedParticles = countInfectionBlocks(level, pos, 16, 6);
            cachedDensity = calculateDensityClient(level, pos);
            cachedBestDir = findHighestDensityDirection(level, pos);
        }

        int radiation = cachedParticles > 0 ? Math.min(20, Math.max(1, cachedParticles / 40)) : 0;
        boolean blackHoleWarning = cachedParticles > 1500 || cachedDensity > 0.80f;

        // Panel position (bottom-right)
        int panelW = 90;
        int panelH = blackHoleWarning ? 52 : 42;
        int px = screenWidth - panelW - 6;
        int py = screenHeight - panelH - 40;

        // Background
        guiGraphics.fill(px - 2, py - 2, px + panelW + 2, py + panelH + 2, 0xCC0A0A0A);

        // Title + direction
        int barColor = getRadColor(cachedDensity);
        guiGraphics.drawString(mc.font, "Geiger", px + 2, py + 2, barColor, true);

        String[] arrows = {"^", ">", "v", "<"};
        if (cachedBestDir >= 0) {
            guiGraphics.drawString(mc.font, arrows[cachedBestDir], px + panelW - 12, py + 2, 0xFFFF6600, true);
        }

        // Radiation bar
        int barY = py + 13;
        int barW = panelW - 4;
        guiGraphics.fill(px + 2, barY, px + 2 + barW, barY + 4, 0xFF2D2D2D);
        int filled = (int) (barW * Math.min(1.0f, (float) radiation / 20));
        guiGraphics.fill(px + 2, barY, px + 2 + filled, barY + 4, barColor | 0xFF000000);

        // Stats line (small)
        String stats = radiation + "/20 | " + cachedParticles + "p | " + String.format("%.0f%%", cachedDensity * 100);
        float scale = 0.75f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(mc.font, stats, (int) ((px + 2) / scale), (int) ((barY + 6) / scale), 0xFFCCCCCC, false);
        guiGraphics.pose().popPose();

        // Black hole warning
        if (blackHoleWarning) {
            boolean flash = (tick % 20) < 10;
            if (flash) {
                guiGraphics.drawString(mc.font, "[BH WARNING]", px + 4, py + panelH - 10, 0xFFFF2222, true);
            }
        }
    }

    private static int countInfectionBlocks(Level level, BlockPos center, int radius, int vertical) {
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-radius, -vertical, -radius),
                center.offset(radius, vertical, radius))) {
            BlockState state = level.getBlockState(p);
            if (state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                    || state.is(ModBlocks.CORRUPTED_SOIL.get())
                    || state.is(ModBlocks.INFECTION_GROWTH.get())
                    || state.is(ModBlocks.CORRUPTED_STONE.get())
                    || state.is(ModBlocks.INFECTION_VEIN.get())) {
                count++;
            }
        }
        return count;
    }

    private static float calculateDensityClient(Level level, BlockPos center) {
        int total = 0;
        int infected = 0;
        int r = 8;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -4, -r), center.offset(r, 4, r))) {
            if (!level.getBlockState(p).isAir()) {
                total++;
                BlockState state = level.getBlockState(p);
                if (state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                        || state.is(ModBlocks.CORRUPTED_SOIL.get())
                        || state.is(ModBlocks.INFECTION_GROWTH.get())
                        || state.is(ModBlocks.CORRUPTED_STONE.get())
                        || state.is(ModBlocks.INFECTION_VEIN.get())) {
                    infected++;
                }
            }
        }
        return total > 0 ? (float) infected / total : 0.0f;
    }

    private static int findHighestDensityDirection(Level level, BlockPos center) {
        int[][] dirs = {{0, -8}, {8, 0}, {0, 8}, {-8, 0}};
        int bestDir = -1;
        int bestCount = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos check = center.offset(dirs[i][0], 0, dirs[i][1]);
            int count = countInfectionBlocks(level, check, 4, 3);
            if (count > bestCount) {
                bestCount = count;
                bestDir = i;
            }
        }
        return bestDir;
    }

    private int getRadColor(float density) {
        if (density > 0.80f) return 0xFF8B00FF;
        if (density > 0.60f) return 0xFFFF2222;
        if (density > 0.35f) return 0xFFFFAA00;
        return 0xFF44DD44;
    }
}
