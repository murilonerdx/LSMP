package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v0.1.24 r92: <b>Mob Jar Block</b>.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Vazio + right-click em mob próximo → captura</li>
 *   <li>Cheio + right-click vazio → libera mob</li>
 *   <li>Shape pequeno (8x8x12) — parece um pote</li>
 * </ul>
 */
public class MobJarBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.2, 0.0, 0.2, 0.8, 0.75, 0.8);

    public MobJarBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter g,
                                BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobJarBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof MobJarBlockEntity be)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        if (be.hasMob() && held.isEmpty()) {
            Entity released = be.releaseMob();
            if (released != null) {
                player.displayClientMessage(Component.literal(
                        "§5✦ §r§5" + released.getName().getString() + " §5libertado."), true);
            }
            return InteractionResult.CONSUME;
        }

        if (!be.hasMob() && held.isEmpty()) {
            // Procura LivingEntity num raio de 3
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(3));
            for (LivingEntity e : nearby) {
                if (e == player) continue;
                if (e instanceof Player) continue;
                if (e.getMaxHealth() > 200) {
                    player.displayClientMessage(Component.literal(
                            "§4§o✦ Criatura grande demais pra este jar."), true);
                    return InteractionResult.FAIL;
                }
                if (be.captureMob(e)) {
                    e.discard();
                    player.displayClientMessage(Component.literal(
                            "§5§l✦ §r§5" + e.getName().getString() + " §5capturado."), true);
                    return InteractionResult.CONSUME;
                }
                break;
            }
        }

        return InteractionResult.PASS;
    }
}
