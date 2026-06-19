package br.com.murilo.liberthia.loom;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.1.22 r33: hook em FLINT_AND_STEEL no DARK_MATTER_BLOCK.
 *
 * <p>Detecta frame 4×5 (5 alto × 4 largo) feito de DARK_MATTER_BLOCK, abre
 * portal de 2×3 dentro com {@link LoomPortalBlock}.
 *
 * <h2>Padrão de frame (vista lateral, X axis):</h2>
 * <pre>
 *  XXXX        (top — 4 horizontais)
 *  X..X        (3 fileiras de portal)
 *  X..X
 *  X..X
 *  XXXX        (bottom)
 * </pre>
 * X = DARK_MATTER_BLOCK, . = AIR (vira PORTAL)
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LoomPortalIgniter {

    private LoomPortalIgniter() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getItemStack().getItem() != Items.FLINT_AND_STEEL) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        // Só ignita em DARK_MATTER_BLOCK
        if (!state.is(ModBlocks.LOOM_STONE.get())) return;

        // Tenta as 2 axes — X e Z
        if (tryIgnitePortal(level, pos, Direction.Axis.X)
                || tryIgnitePortal(level, pos, Direction.Axis.Z)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.getItemStack().hurtAndBreak(1, event.getEntity(),
                    p -> p.broadcastBreakEvent(event.getHand()));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Tenta detectar e ignitar portal numa axis específica.
     * Procura por frame DARK_MATTER 4×5 começando do clicked pos.
     */
    private static boolean tryIgnitePortal(Level level, BlockPos clicked, Direction.Axis axis) {
        // Direção lateral (perpendicular ao axis)
        Direction sideDir = (axis == Direction.Axis.X) ? Direction.EAST : Direction.SOUTH;

        // Acha base do frame — sobe enquanto for dark_matter
        BlockPos basePos = clicked;
        // Vai pra baixo até achar bloco que NÃO seja dark_matter
        for (int i = 0; i < 10; i++) {
            if (!level.getBlockState(basePos.below()).is(ModBlocks.LOOM_STONE.get())) break;
            basePos = basePos.below();
        }
        // basePos agora é o canto inferior do frame possível.
        // Verifica frame: bottom row 4 dark_matter, sides 3 alturas, top 4 dark_matter.
        // Padrão: 4 wide × 5 tall, interior 2×3.

        // Tenta começar do canto esquerdo: varre pra esquerda enquanto for dark_matter
        BlockPos leftBase = basePos;
        for (int i = 0; i < 5; i++) {
            BlockPos test = leftBase.relative(sideDir.getOpposite());
            if (!level.getBlockState(test).is(ModBlocks.LOOM_STONE.get())) break;
            leftBase = test;
        }

        // Verifica frame completo a partir de leftBase
        return validateAndCreatePortal(level, leftBase, sideDir, axis);
    }

    private static boolean validateAndCreatePortal(Level level, BlockPos leftBase,
                                                     Direction sideDir, Direction.Axis axis) {
        // Bottom row: 4 dark_matter horizontais
        for (int i = 0; i < 4; i++) {
            if (!level.getBlockState(leftBase.relative(sideDir, i)).is(ModBlocks.LOOM_STONE.get())) {
                return false;
            }
        }
        // Top row (4 alto): também 4 dark_matter
        for (int i = 0; i < 4; i++) {
            if (!level.getBlockState(leftBase.above(4).relative(sideDir, i)).is(ModBlocks.LOOM_STONE.get())) {
                return false;
            }
        }
        // Left + Right columns (entre top e bottom): dark_matter
        for (int y = 1; y <= 3; y++) {
            if (!level.getBlockState(leftBase.above(y)).is(ModBlocks.LOOM_STONE.get())) return false;
            if (!level.getBlockState(leftBase.above(y).relative(sideDir, 3)).is(ModBlocks.LOOM_STONE.get())) return false;
        }
        // Interior 2×3 deve ser AIR ou FIRE (acabou de spawnar pelo flint+steel)
        List<BlockPos> interior = new ArrayList<>();
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 2; x++) {
                BlockPos p = leftBase.above(y).relative(sideDir, x);
                BlockState s = level.getBlockState(p);
                if (!s.isAir() && !s.is(net.minecraft.world.level.block.Blocks.FIRE)) {
                    return false;
                }
                interior.add(p);
            }
        }
        // Tudo válido — coloca portal nos 6 blocos interiores
        for (BlockPos p : interior) {
            level.setBlock(p, ModBlocks.LOOM_PORTAL.get().defaultBlockState()
                    .setValue(LoomPortalBlock.AXIS_PROP, axis), 3);
        }
        level.playSound(null, leftBase, net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }
}
