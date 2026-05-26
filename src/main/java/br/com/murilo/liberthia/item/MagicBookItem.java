package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r58: <b>Magic Book</b> — antigo grimoire de invocação. Right-click
 * mostra fragmentos crípticos de saber dos rituais.
 *
 * <p>Item lore/utility — mostra mensagem random sobre rituais quando lido.
 * Pode ser usado pra dar pistas pros players novos.
 */
public class MagicBookItem extends Item {

    private static final String[] FRAGMENTS = {
            "§5§oA primeira invocação requer §dgiz branco§r§5§o, §dvelas§r§5§o, e §lsangue§r§5§o.",
            "§5§oNomes têm peso. §dNão pronuncie em voz alta.§r",
            "§5§oA porta abre para quem fica de costas.",
            "§5§oCada §dsigilo§r §5§oé um contrato. Cumpra.",
            "§5§oO Tomo Proibido §lnão deve§r §5§oser segurado por mais de 4 minutos.",
            "§5§oQuem desenha em sangue precisa de §dQuartzo Fantasma§r §5§opra terminar.",
            "§5§oOs §dInvocados§r §5§odeixam um livro quando se vão. Procure entre as cinzas.",
            "§5§oO §dLivro do Êxodo§r §5§ofunciona somente do §lOutro Lado§r§5§o.",
            "§5§oNão olhe pra mesma sombra duas vezes.",
            "§5§oO §dCristal do Rifte§r §5§oarde frio.",
            "§5§oCada nome riscado neste livro foi cobrado de §lalguém§r§5§o."
    };

    public MagicBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // Pick random fragment
        String fragment = FRAGMENTS[(int)(Math.random() * FRAGMENTS.length)];
        sp.displayClientMessage(Component.literal(fragment), false);
        sp.displayClientMessage(Component.literal("§8§o[Livro Mágico]"), true);

        // Particle effect
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    sp.getX(), sp.getY() + 1.5, sp.getZ(),
                    20, 0.4, 0.6, 0.4, 0.1);
        }

        sp.getCooldowns().addCooldown(this, 60); // 3s cooldown
        return InteractionResultHolder.consume(stack);
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§5§oLivro Mágico").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Fragmentos de §dconhecimento ritualístico§r§7."));
        t.add(Component.literal("§7Right-click pra ler uma página random."));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"A página seguinte sempre está em branco.\""));
    }
}
