package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
 * v0.1.22 r61: <b>Source Gem</b> — item consumível que restaura Source.
 *
 * <p>Inspired by AN's mana jar concept, mas single-use por gem. Right-click
 * pra absorver: +25 Source. Particles soul fire.
 */
public class SourceGemItem extends Item {

    public SourceGemItem(Properties p) {
        super(p.stacksTo(16).rarity(Rarity.RARE));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        int current = SourceData.get(sp);
        int max = SourceData.getMax(sp);
        if (current >= max) {
            sp.displayClientMessage(Component.literal(
                "§7Sua Source já está cheia."), true);
            return InteractionResultHolder.fail(stack);
        }

        SourceData.add(sp, 25);
        stack.shrink(1);

        // VFX
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                sp.getX(), sp.getY() + 1, sp.getZ(),
                20, 0.4, 0.5, 0.4, 0.05);
        }
        sp.displayClientMessage(Component.literal(
            "§5§l+25 Source§r §7(§e" + SourceData.get(sp) + "§7/§e" + max + "§7)"), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§5§oGema de Source").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Right-click pra absorver §e+25 Source§7."));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"Fragmento condensado da atenção.\""));
    }
}
