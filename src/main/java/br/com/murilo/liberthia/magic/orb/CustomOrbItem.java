package br.com.murilo.liberthia.magic.orb;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r174: <b>Orb Customizado</b> — forjado no Infusor de Orbs a partir de itens
 * raros. Guarda no NBT a lista de efeitos resultantes e:
 * <ul>
 *   <li><b>Ao usar</b> (clique-direito): aplica TODOS os efeitos por 30s ao
 *       portador (real). Cooldown de 30s, não consome.</li>
 *   <li>Documenta cada efeito no tooltip (não fictício).</li>
 *   <li>É aceito como <b>Orb</b> no Arcane Workbench (compõe feitiços).</li>
 * </ul>
 */
public class CustomOrbItem extends Item {

    public static final String NBT_FX = OrbIngredients.NBT_FX;
    private static final int BUFF_DURATION = 600; // 30s
    private static final int COOLDOWN = 600;       // 30s

    public CustomOrbItem(Properties p) {
        super(p.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static ItemStack create(ListTag fx) {
        ItemStack s = new ItemStack(ModItems.CUSTOM_ORB.get());
        if (fx != null && !fx.isEmpty()) s.getOrCreateTag().put(NBT_FX, fx.copy());
        return s;
    }

    public static ListTag fx(ItemStack s) {
        return (s.hasTag() && s.getTag().contains(NBT_FX))
                ? s.getTag().getList(NBT_FX, Tag.TAG_COMPOUND) : new ListTag();
    }

    @Override public boolean isFoil(ItemStack s) { return !fx(s).isEmpty(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ListTag fx = fx(stack);
        if (fx.isEmpty()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            OrbIngredients.apply(player, fx, BUFF_DURATION);
            player.getCooldowns().addCooldown(this, COOLDOWN);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        ListTag fx = fx(stack);
        if (fx.isEmpty()) {
            tip.add(Component.literal("§8§oOrb em branco — forje no Infusor de Orbs."));
            return;
        }
        tip.add(Component.literal("§b§lEfeitos §r§7(use pra ativar 30s):"));
        for (int i = 0; i < fx.size(); i++) {
            String label = fx.getCompound(i).getString("label");
            if (label.isEmpty()) label = fx.getCompound(i).getString("id");
            tip.add(Component.literal("§3  ✦ " + label));
        }
        tip.add(Component.literal("§8§oTambém usável como Orb no Arcane Workbench."));
    }
}
