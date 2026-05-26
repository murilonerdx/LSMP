package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.matter.MatterContentRegistry;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sample Vial — frasco de coleta. Right-click num bloco copia o conteúdo de
 * matéria daquele bloco/item pro frasco (NBT). Também faz uma varredura de
 * "aura" de matéria escura num raio de 5 blocos em volta — qualquer
 * {@code CORRUPTED_SOIL}, {@code SCARRED_EARTH}, {@code SCARRED_STONE} ou
 * {@code CRYSTALLIZER} adjacente soma contaminação no frasco. Depois você
 * coloca o frasco no Matter Analyzer pra ver os detalhes.
 *
 * <p>Funciona pra qualquer bloco registrado no {@link MatterContentRegistry}
 * E pra qualquer bloco comum (terra, pedra, etc) — o analisador de aura
 * detecta matéria escura ambiente em qualquer lugar.
 */
public class SampleVialItem extends Item {

    public static final String TAG_DM = "dm";
    public static final String TAG_WM = "wm";
    public static final String TAG_YM = "ym";
    public static final String TAG_SOURCE = "src";

    /** Raio de scan da aura de contaminação ambiente. */
    private static final int AURA_RADIUS = 5;

    public SampleVialItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack blockStack = new ItemStack(state.getBlock().asItem());

        // 1) Matéria do bloco direto (registry)
        MatterContent direct = MatterContentRegistry.of(blockStack);

        // 2) Varredura de aura — soma contaminação dos blocos de infecção em volta
        float auraDm = scanInfectionAura(level, pos);

        float dm = direct.dark() + auraDm;
        float wm = direct.white();
        float ym = direct.yellow();
        float total = dm + wm + ym;

        if (total <= 0) {
            player.displayClientMessage(
                    Component.literal("Solo limpo. Sem traços de matéria detectados.")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        ItemStack inHand = ctx.getItemInHand();
        // Se tem mais de 1 frasco, tira 1 e devolve no inventário separado
        ItemStack vial = inHand.copy();
        vial.setCount(1);
        var tag = vial.getOrCreateTag();
        tag.putFloat(TAG_DM, dm);
        tag.putFloat(TAG_WM, wm);
        tag.putFloat(TAG_YM, ym);

        // Source label — se o bloco está registrado dá o nome dele, senão chama de "amostra de solo"
        String sourceLabel;
        if (direct.total() > 0) {
            sourceLabel = blockStack.getHoverName().getString();
            if (auraDm > 0) sourceLabel += " (+aura)";
        } else if (auraDm > 0) {
            sourceLabel = "Solo contaminado (aura)";
        } else {
            sourceLabel = blockStack.getHoverName().getString();
        }
        tag.putString(TAG_SOURCE, sourceLabel);

        if (inHand.getCount() > 1) {
            inHand.shrink(1);
            if (!player.getInventory().add(vial)) player.drop(vial, false);
        } else {
            // Substitui o item na mão
            player.setItemInHand(ctx.getHand(), vial);
        }

        // Feedback diferenciado se foi via aura
        String msg;
        ChatFormatting color;
        if (direct.total() > 0 && auraDm > 0) {
            msg = String.format("Amostra coletada: %s [DM:%.0f WM:%.0f YM:%.0f]",
                    blockStack.getHoverName().getString(), dm, wm, ym);
            color = ChatFormatting.LIGHT_PURPLE;
        } else if (auraDm > 0) {
            msg = String.format("Aura de matéria escura detectada (DM:%.0f)", auraDm);
            color = ChatFormatting.DARK_PURPLE;
        } else {
            msg = "Amostra coletada: " + blockStack.getHoverName().getString();
            color = ChatFormatting.LIGHT_PURPLE;
        }
        player.displayClientMessage(Component.literal(msg).withStyle(color), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Varre um cubo de raio {@link #AURA_RADIUS} ao redor de {@code center}
     * procurando blocos de infecção. Retorna a contaminação DM ponderada.
     *
     * <p>Cada infecção contribui com um peso diferente:
     * <ul>
     *   <li>CORRUPTED_SOIL: 1.0</li>
     *   <li>SCARRED_EARTH: 1.5</li>
     *   <li>SCARRED_STONE: 2.0</li>
     *   <li>CRYSTALLIZER: 4.0 (fonte principal)</li>
     * </ul>
     * A distância também atenua — blocos próximos contam mais.
     */
    private static float scanInfectionAura(Level level, BlockPos center) {
        Block corrupted = ModBlocks.CORRUPTED_SOIL.get();
        Block scarredE = ModBlocks.SCARRED_EARTH.get();
        Block scarredS = ModBlocks.SCARRED_STONE.get();
        Block crystallizer = ModBlocks.CRYSTALLIZER.get();

        float total = 0f;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -AURA_RADIUS; dx <= AURA_RADIUS; dx++) {
            for (int dy = -AURA_RADIUS; dy <= AURA_RADIUS; dy++) {
                for (int dz = -AURA_RADIUS; dz <= AURA_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    Block b = level.getBlockState(cursor).getBlock();

                    float weight;
                    if (b == corrupted) weight = 1.0f;
                    else if (b == scarredE) weight = 1.5f;
                    else if (b == scarredS) weight = 2.0f;
                    else if (b == crystallizer) weight = 4.0f;
                    else continue;

                    // Atenuação por distância (Chebyshev) — bloco adjacente vale 1.0,
                    // longe vale menos.
                    int dist = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                    float falloff = 1.0f - ((float) (dist - 1) / (float) AURA_RADIUS);
                    if (falloff < 0.1f) falloff = 0.1f;

                    total += weight * falloff;
                }
            }
        }
        return total;
    }

    /** Lê o conteúdo do frasco — usado pelo Analyzer. */
    public static MatterContent contentOf(ItemStack vialStack) {
        if (!vialStack.hasTag()) return MatterContent.EMPTY;
        var tag = vialStack.getTag();
        return new MatterContent(tag.getFloat(TAG_DM), tag.getFloat(TAG_WM), tag.getFloat(TAG_YM));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Tooltip MINIMALISTA — info detalhada (DM/WM/YM/mutação) SÓ aparece quando
        // o frasco é colocado dentro do Matter Analyzer. Sem isso, jogador descobre
        // tudo só hover-eando no inventário, quebrando o gameplay loop "coletar →
        // levar pro analyzer → descobrir composição".
        if (stack.hasTag() && stack.getTag().contains(TAG_SOURCE)) {
            tooltip.add(Component.literal("Amostra coletada")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Use o Matter Analyzer para identificar a composição")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.literal("Right-click num bloco pra coletar amostra")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Detecta aura de matéria escura em raio de 5 blocos")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_SOURCE);
    }
}
