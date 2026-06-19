package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.ScribesTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r69: <b>Scribes Table</b> — bloco de craft mesa do escriba.
 *
 * <p>Right-click pra abrir GUI. Coloque um Spell Parchment vazio + glyphs nos
 * slots de 1-8 e o output será populado automaticamente. Take output consume
 * inputs.
 */
public class ScribesTableBlock extends BaseEntityBlock {

    /** Shape achatado tipo mesa (~6/16 alto). */
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(0, 0, 0, 16, 12, 16),  // base
        Block.box(2, 12, 2, 14, 14, 14)  // tampo
    );

    public ScribesTableBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScribesTableBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        // r116: SNEAK + right-click = tenta craftar glyph com reagents do inventário
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
            var recipe = br.com.murilo.liberthia.magic.glyph.SpiritGlyphRecipes.findMatching(player);
            if (recipe != null) {
                net.minecraft.world.item.ItemStack output =
                        br.com.murilo.liberthia.magic.glyph.SpiritGlyphRecipes.tryConsume(player, recipe);
                if (output != null) {
                    if (!player.getInventory().add(output)) {
                        player.drop(output, false);
                    }
                    // VFX de cast bem-sucedido em cima da mesa
                    if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                        br.com.murilo.liberthia.magic.spell.vfx.HelixSpawner.spawnRing(
                                sl, new net.minecraft.world.phys.Vec3(
                                        pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5),
                                br.com.murilo.liberthia.magic.school.SpellSchool.HOLY,
                                0.6, 16, 0);
                    }
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§a✓ Forjado: §d" + recipe.displayName), true);
                    return InteractionResult.CONSUME;
                }
            }
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§7Sem reagentes suficientes — caçe no Spirit World"), true);
            return InteractionResult.CONSUME;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ScribesTableBlockEntity scribes && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, scribes, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ScribesTableBlockEntity scribes) {
                scribes.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
