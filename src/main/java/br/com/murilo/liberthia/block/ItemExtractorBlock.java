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
 * Pipe Extrator — bloco simplificado que SÓ extrai do inventário vizinho.
 *
 * <p>Compartilha {@link ItemPipeBlockEntity} com o pipe transporte normal,
 * mas com regras específicas:
 * <ul>
 *   <li>Ao colocar encostado num inventário, a face que toca já vira EXTRACT
 *       automaticamente.</li>
 *   <li>Outras faces ficam DEFAULT (servem como saída/transporte).</li>
 *   <li>SHIFT + right-click (mão vazia) numa face: define ESSA face como a
 *       nova face de extração (limpa qualquer EXTRACT anterior).</li>
 *   <li>Right-click com item: adiciona filtro na face EXTRACT atual.</li>
 *   <li>Não permite ciclar pra INSERT — esse é o trabalho do Inserter.</li>
 * </ul>
 */
public class ItemExtractorBlock extends ItemPipeBlock {

    public ItemExtractorBlock(Properties props) { super(props); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        // Pula o auto-config genérico do pai e faz nosso próprio: SÓ vira
        // EXTRACT a face que toca um inventário (não-pipe).
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) return;

        Direction extractFace = null;
        for (Direction d : Direction.values()) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(d));
            if (nbe == null) continue;
            if (nbe instanceof ItemPipeBlockEntity) continue;
            if (nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).isPresent()) {
                extractFace = d;
                break;
            }
        }
        if (extractFace != null) {
            pipe.setMode(extractFace, ItemPipeBlockEntity.Mode.EXTRACT);
            if (placer instanceof Player p) {
                p.displayClientMessage(Component.literal(
                        "→ Extrator pulando do inventário em " + extractFace.getName().toUpperCase())
                        .withStyle(ChatFormatting.GOLD), false);
            }
        } else if (placer instanceof Player p) {
            p.displayClientMessage(Component.literal(
                    "⚠ Extrator colocado sem inventário vizinho — coloque encostado num baú/máquina")
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

        // SHIFT + mão vazia → muda a face de extração pra essa.
        if (player.isShiftKeyDown() && held.isEmpty()) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(face));
            if (nbe == null || !nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, face.getOpposite()).isPresent()) {
                player.displayClientMessage(Component.literal(
                        "⚠ Face " + face.getName() + " não tem inventário vizinho")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            // Limpa qualquer EXTRACT anterior em outras faces
            for (Direction d : Direction.values()) {
                if (pipe.getMode(d) == ItemPipeBlockEntity.Mode.EXTRACT) {
                    pipe.setMode(d, ItemPipeBlockEntity.Mode.DEFAULT);
                }
            }
            pipe.setMode(face, ItemPipeBlockEntity.Mode.EXTRACT);
            player.displayClientMessage(Component.literal(
                    "→ Extração agora em " + face.getName().toUpperCase())
                    .withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }

        // SHIFT + item → ciclar velocidade (herdado)
        if (player.isShiftKeyDown() && !held.isEmpty()) {
            ItemPipeBlockEntity.Speed s = pipe.cycleSpeed();
            player.displayClientMessage(Component.literal("Velocidade: " + s.name())
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        // Item na mão → adiciona ao filtro DA FACE DE EXTRACT (não da face clicada)
        if (!held.isEmpty()) {
            Direction extractFace = null;
            for (Direction d : Direction.values()) {
                if (pipe.getMode(d) == ItemPipeBlockEntity.Mode.EXTRACT) {
                    extractFace = d;
                    break;
                }
            }
            if (extractFace == null) {
                player.displayClientMessage(Component.literal(
                        "⚠ Sem face EXTRACT configurada — SHIFT-click a face que toca o baú primeiro")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            boolean added = pipe.addFilter(extractFace, held);
            player.displayClientMessage(Component.literal(added
                    ? "Filtro + " + held.getHoverName().getString()
                    : "Filtro cheio")
                    .withStyle(added ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        // Mão vazia → mostra qual face tá extraindo e o filtro
        Direction extractFace = null;
        for (Direction d : Direction.values()) {
            if (pipe.getMode(d) == ItemPipeBlockEntity.Mode.EXTRACT) {
                extractFace = d;
                break;
            }
        }
        if (extractFace == null) {
            player.displayClientMessage(Component.literal(
                    "Extrator: §csem face configurada§r — SHIFT-click face que toca o baú")
                    .withStyle(ChatFormatting.YELLOW), true);
        } else {
            ItemStack[] f = pipe.getFilter(extractFace);
            int count = 0;
            for (ItemStack fs : f) if (!fs.isEmpty()) count++;
            player.displayClientMessage(Component.literal("Extraindo de " + extractFace.getName().toUpperCase()
                    + " | filtro: " + (count == 0 ? "vazio (passa-tudo)" : count + " items"))
                    .withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.CONSUME;
    }
}
