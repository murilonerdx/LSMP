package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180: <b>Amostra de Matéria</b> — coletada de uma {@code CobaiaEntity} com um Frasco
 * de Vidro. Guarda na NBT o tipo e o nível de matéria daquele sujeito, pra estudo
 * (qual matéria domina, quão avançada está a infecção, e os efeitos esperados).
 */
public class MatterSampleItem extends Item {

    public MatterSampleItem(Properties props) {
        super(props.stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        String type = tag != null ? tag.getString("MatterType") : "none";
        int lvl = tag != null ? tag.getInt("MatterLevel") : 0;
        tip.add(Component.literal("§7Tipo: " + colored(type)));
        tip.add(Component.literal("§7Infecção: §f" + lvl + "%"));
        tip.add(Component.empty());
        switch (type) {
            case "dark" -> tip.add(Component.literal("§5Anti-criação: hostil, corrói o entorno.").withStyle(ChatFormatting.DARK_PURPLE));
            case "clear" -> tip.add(Component.literal("§bReações imediatas, energia pura.").withStyle(ChatFormatting.AQUA));
            case "yellow" -> tip.add(Component.literal("§eInstável, muta rápido e errático.").withStyle(ChatFormatting.YELLOW));
            case "mixed" -> tip.add(Component.literal("§cMistura volátil — evolução imprevisível.").withStyle(ChatFormatting.RED));
            default -> tip.add(Component.literal("§8Amostra vazia (sujeito não exposto).").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (lvl >= 100) tip.add(Component.literal("§4§l⚠ Saturação total — corpo perdido.").withStyle(ChatFormatting.DARK_RED));
    }

    private static String colored(String type) {
        return switch (type) {
            case "dark" -> "§5Escura";
            case "clear" -> "§bClara";
            case "yellow" -> "§eAmarela";
            case "mixed" -> "§cMista";
            default -> "§8—";
        };
    }
}
