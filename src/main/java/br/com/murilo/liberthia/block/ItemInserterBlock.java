package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * Pipe Inseridor — bloco simplificado que SÓ insere no inventário vizinho.
 *
 * <p>Recebe items via rede de pipes (faces DEFAULT/transporte) e empurra pra
 * dentro do inventário tocando a face INSERT.
 *
 * <ul>
 *   <li>Ao colocar encostado num inventário, a face que toca já vira INSERT
 *       automaticamente.</li>
 *   <li>SHIFT + right-click numa face: define ESSA face como a face de
 *       inserção.</li>
 *   <li>Right-click com item: adiciona filtro na face INSERT atual (whitelist).</li>
 * </ul>
 */
public class ItemInserterBlock extends ItemPipeBlock {

    public ItemInserterBlock(Properties props) { super(props); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) return;

        Direction insertFace = null;
        for (Direction d : Direction.values()) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(d));
            if (nbe == null) continue;
            if (nbe instanceof ItemPipeBlockEntity) continue;
            if (nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).isPresent()) {
                insertFace = d;
                break;
            }
        }
        if (insertFace != null) {
            pipe.setMode(insertFace, ItemPipeBlockEntity.Mode.INSERT);
            if (placer instanceof Player p) {
                p.displayClientMessage(Component.literal(
                        "→ Inseridor empurrando pra " + insertFace.getName().toUpperCase())
                        .withStyle(ChatFormatting.GREEN), false);
            }
        } else if (placer instanceof Player p) {
            p.displayClientMessage(Component.literal(
                    "⚠ Inseridor colocado sem inventário vizinho — coloque encostado num baú/máquina")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe))
            return InteractionResult.PASS;

        Direction face = hit.getDirection();
        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && held.isEmpty()) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(face));
            if (nbe == null || !nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, face.getOpposite()).isPresent()) {
                player.displayClientMessage(Component.literal(
                        "⚠ Face " + face.getName() + " não tem inventário vizinho")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            for (Direction d : Direction.values()) {
                if (pipe.getMode(d) == ItemPipeBlockEntity.Mode.INSERT) {
                    pipe.setMode(d, ItemPipeBlockEntity.Mode.DEFAULT);
                }
            }
            pipe.setMode(face, ItemPipeBlockEntity.Mode.INSERT);
            player.displayClientMessage(Component.literal(
                    "→ Inserção agora em " + face.getName().toUpperCase())
                    .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown() && !held.isEmpty()) {
            ItemPipeBlockEntity.Speed s = pipe.cycleSpeed();
            player.displayClientMessage(Component.literal("Velocidade: " + s.name())
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        if (!held.isEmpty()) {
            Direction insertFace = null;
            for (Direction d : Direction.values()) {
                if (pipe.getMode(d) == ItemPipeBlockEntity.Mode.INSERT) {
                    insertFace = d;
                    break;
                }
            }
            if (insertFace == null) {
                player.displayClientMessage(Component.literal(
                        "⚠ Sem face INSERT configurada — SHIFT-click a face que toca o baú primeiro")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            boolean added = pipe.addFilter(insertFace, held);
            player.displayClientMessage(Component.literal(added
                    ? "Filtro + " + held.getHoverName().getString()
                    : "Filtro cheio")
                    .withStyle(added ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        Direction insertFace = null;
        for (Direction d : Direction.values()) {
            if (pipe.getMode(d) == ItemPipeBlockEntity.Mode.INSERT) {
                insertFace = d;
                break;
            }
        }
        if (insertFace == null) {
            player.displayClientMessage(Component.literal(
                    "Inseridor: §csem face configurada§r — SHIFT-click face que toca o baú")
                    .withStyle(ChatFormatting.YELLOW), true);
        } else {
            ItemStack[] f = pipe.getFilter(insertFace);
            int count = 0;
            for (ItemStack fs : f) if (!fs.isEmpty()) count++;
            player.displayClientMessage(Component.literal("Inserindo em " + insertFace.getName().toUpperCase()
                    + " | filtro: " + (count == 0 ? "vazio (passa-tudo)" : count + " items"))
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.CONSUME;
    }
}
