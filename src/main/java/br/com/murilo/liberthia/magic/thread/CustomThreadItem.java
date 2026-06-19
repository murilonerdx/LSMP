package br.com.murilo.liberthia.magic.thread;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r172: <b>Thread Customizada</b> — montada no Tear de Threads a partir de itens
 * vanilla. Guarda no NBT a lista de efeitos resultantes e:
 * <ul>
 *   <li>Aplica os efeitos PASSIVAMENTE enquanto está no inventário (real).</li>
 *   <li>Documenta cada efeito no tooltip (não fictício).</li>
 *   <li>É aceita como Thread no Arcane Workbench (compõe feitiços).</li>
 * </ul>
 */
public class CustomThreadItem extends Item {

    public static final String NBT_FX = ThreadIngredients.NBT_FX;

    public CustomThreadItem(Properties p) {
        super(p.stacksTo(16).rarity(Rarity.RARE));
    }

    /** Cria uma thread com a lista de efeitos dada. */
    public static ItemStack create(ListTag fx) {
        ItemStack s = new ItemStack(ModItems.CUSTOM_THREAD.get());
        if (fx != null && !fx.isEmpty()) s.getOrCreateTag().put(NBT_FX, fx.copy());
        return s;
    }

    public static ListTag fx(ItemStack s) {
        return (s.hasTag() && s.getTag().contains(NBT_FX))
                ? s.getTag().getList(NBT_FX, Tag.TAG_COMPOUND) : new ListTag();
    }

    @Override public boolean isFoil(ItemStack s) { return !fx(s).isEmpty(); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player p)) return;
        if (level.getGameTime() % 40 != 0) return;            // re-aplica a cada 2s
        ListTag fx = fx(stack);
        if (!fx.isEmpty()) ThreadIngredients.apply(p, fx, 60); // 3s, refrescado
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        ListTag fx = fx(stack);
        if (fx.isEmpty()) {
            tip.add(Component.literal("§8§oThread em branco — monte no Tear de Threads."));
            return;
        }
        tip.add(Component.literal("§d§lEfeitos ativos §r§7(enquanto no inventário):"));
        for (int i = 0; i < fx.size(); i++) {
            String label = fx.getCompound(i).getString("label");
            if (label.isEmpty()) label = fx.getCompound(i).getString("id");
            tip.add(Component.literal("§a  ✦ " + label));
        }
        tip.add(Component.literal("§8§oTambém usável como Thread no Arcane Workbench."));
    }
}
