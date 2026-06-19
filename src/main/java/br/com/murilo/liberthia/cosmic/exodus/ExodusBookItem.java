package br.com.murilo.liberthia.cosmic.exodus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r56: <b>Exodus Book</b> — item lendário, único caminho de saída
 * pra players exilados pelo Forbidden Tome.
 *
 * <h2>Obtenção</h2>
 * SOMENTE como drop de entidades sumonadas via ritual. NÃO pode ser crafted.
 *
 * <h2>Uso</h2>
 * Right-click no Spirit World: quebra a tome curse, teleporta de volta, dá
 * regeneração e satisfação.
 *
 * <h2>Tooltip filosofia</h2>
 * NÃO explica mecânica diretamente — só dá pistas poéticas. Player descobre.
 */
public class ExodusBookItem extends Item {

    public ExodusBookItem(Properties p) {
        super(p.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        boolean inSpirit = br.com.murilo.liberthia.dimension.SpiritDimension.isInSpiritWorld(sp);
        if (!inSpirit) {
            sp.displayClientMessage(Component.literal(
                    "§5§oA página está vazia. Você não pertence a este lado."), true);
            return InteractionResultHolder.fail(stack);
        }

        // VFX dramatic
        ServerLevel sl = (ServerLevel) level;
        for (int i = 0; i < 80; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 4;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    sp.getX() + Math.cos(a) * r,
                    sp.getY() + Math.random() * 3,
                    sp.getZ() + Math.sin(a) * r,
                    1, 0.05, 0.05, 0.05, 0.02);
        }
        sl.playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.5F);

        // Return via prayer (também quebra Tome Curse)
        boolean ok = br.com.murilo.liberthia.dimension.SpiritDimension.returnViaPrayer(sp);
        if (ok) {
            // Item CONSUME — exodus é one-time-use
            stack.shrink(1);
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
            sp.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 1));
            sp.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 6000, 0));
            sp.displayClientMessage(Component.literal(
                    "§6§l✦ §rO livro se desfaz em luz. Você §lvolta§r§r§6."), false);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§e§oLivro do Êxodo").withStyle(ChatFormatting.ITALIC));
        t.add(Component.empty());
        t.add(Component.literal("§7§o\"A primeira página é vazia."));
        t.add(Component.literal("§7§o A segunda também."));
        t.add(Component.literal("§7§o A décima diz uma palavra."));
        t.add(Component.literal("§7§o As outras você queima ao ler.\""));
        t.add(Component.empty());
        t.add(Component.literal("§8§o— Anônimo, escrito do outro lado"));
    }
}
