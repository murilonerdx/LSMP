package br.com.murilo.liberthia.dimension;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r57: Itens de entrada para as 3 dimensões liminais.
 *
 * <h2>Itens</h2>
 * <ol>
 *   <li><b>Drowned Compass</b> — entra no Upside Sea</li>
 *   <li><b>Folded Address</b> — entra no Folded City</li>
 *   <li><b>Bark Token</b> — entra no Wooden Place</li>
 * </ol>
 *
 * <p>Tooltips são vagas. Right-click usa o item — consome 1 dos 3 charges
 * antes de quebrar.
 */
public final class LiminalEntryItems {

    private LiminalEntryItems() {}

    static abstract class LiminalKeyItem extends Item {
        private final ResourceKey<Level> destination;
        private final String[] lore;

        LiminalKeyItem(Properties p, ResourceKey<Level> destination, String... lore) {
            super(p.stacksTo(1).durability(3).rarity(Rarity.EPIC));
            this.destination = destination;
            this.lore = lore;
        }

        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

            // Já está na liminal? Tenta voltar
            if (LiminalDimensions.isInLiminal(sp)) {
                boolean ok = LiminalDimensions.returnToOrigin(sp);
                if (ok) {
                    stack.hurt(1, sp.getRandom(), sp);
                    return InteractionResultHolder.consume(stack);
                }
                return InteractionResultHolder.fail(stack);
            }

            // VFX dramatic antes do tp
            ServerLevel sl = (ServerLevel) level;
            sl.sendParticles(ParticleTypes.PORTAL,
                    sp.getX(), sp.getY() + 1, sp.getZ(), 60, 1, 1.5, 1, 0.1);

            boolean ok = LiminalDimensions.enterLiminal(sp, destination);
            if (ok) {
                stack.hurt(1, sp.getRandom(), sp);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.fail(stack);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            for (String line : lore) {
                t.add(Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }

        @Override public boolean isEnchantable(ItemStack s) { return false; }
    }

    /** Entra no Upside Sea. */
    public static class DrownedCompassItem extends LiminalKeyItem {
        public DrownedCompassItem(Properties p) {
            super(p, LiminalDimensions.UPSIDE_SEA,
                "§5§oUma bússola que aponta pra cima.",
                "§8§oA agulha está molhada.",
                "§8§o3 usos. Talvez menos.");
        }
    }

    /** Entra no Folded City. */
    public static class FoldedAddressItem extends LiminalKeyItem {
        public FoldedAddressItem(Properties p) {
            super(p, LiminalDimensions.FOLDED_CITY,
                "§5§oUm papel dobrado oito vezes.",
                "§8§oO endereço se repete em todas as línguas.",
                "§8§o\"Vire à esquerda. Sempre.\"");
        }
    }

    /** Entra no Wooden Place. */
    public static class BarkTokenItem extends LiminalKeyItem {
        public BarkTokenItem(Properties p) {
            super(p, LiminalDimensions.WOODEN_PLACE,
                "§5§oUm pedaço de casca quente.",
                "§8§oEle se mexe quando você não olha.",
                "§8§oNão olhe.");
        }
    }
}
